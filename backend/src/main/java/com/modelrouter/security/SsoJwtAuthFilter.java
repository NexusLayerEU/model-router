package com.modelrouter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class SsoJwtAuthFilter extends OncePerRequestFilter {

    private final SecretKey key;
    private final String identityServerUrl;
    private final String productName;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SsoJwtAuthFilter(
            @Value("${jwt.secret}") String secret,
            @Value("${sso.identity-server-url}") String identityServerUrl,
            @Value("${sso.product-name:modelrouter}") String productName) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.identityServerUrl = identityServerUrl;
        this.productName = productName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(req);
        if (token == null) { chain.doFilter(req, res); return; }

        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) { chain.doFilter(req, res); return; }

        String tier = claims.get("tier", String.class);
        if (tier == null) tier = "FREE";
        Long planExpiresAt = claims.get("planExpiresAt", Long.class);
        Object trialObj = claims.get("trialStartedAt");
        long trialStartedAt = trialObj instanceof Number ? ((Number) trialObj).longValue() : 0L;

        SsoUserDetails userDetails = new SsoUserDetails(
                claims.getSubject(),
                claims.get("name", String.class),
                claims.get("role", String.class),
                tier, trialStartedAt, planExpiresAt);

        if (userDetails.needsUsageCheck() && isActionEndpoint(req)) {
            if (!consumeUsage(token, res)) return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
                userDetails.email(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + (userDetails.role() != null ? userDetails.role() : "USER"))));
        auth.setDetails(userDetails);
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(req, res);
    }

    protected boolean isActionEndpoint(HttpServletRequest req) {
        String method = req.getMethod();
        return "POST".equals(method) || "PUT".equals(method) ||
               "PATCH".equals(method) || "DELETE".equals(method);
    }

    private boolean consumeUsage(String token, HttpServletResponse res) throws IOException {
        try {
            String body = objectMapper.writeValueAsString(Map.of("product", productName));
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(identityServerUrl + "/api/v1/usage/consume"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> httpRes = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(httpRes.body(), Map.class);
            if (Boolean.FALSE.equals(result.get("allowed"))) {
                res.setStatus(402);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"Daily limit reached. Upgrade to Pro — contact admin@nexuslayer.eu\"}");
                return false;
            }
        } catch (Exception e) {
            // fail open
        }
        return true;
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        return null;
    }
}
