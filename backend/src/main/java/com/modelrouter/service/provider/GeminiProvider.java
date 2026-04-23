package com.modelrouter.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.modelrouter.model.domain.RouterConfig;
import com.modelrouter.model.dto.MessagesRequest;
import com.modelrouter.model.dto.MessagesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Translates Anthropic Messages API requests to Google Gemini generateContent API and back.
 *
 * Anthropic format  →  Gemini format  →  Anthropic format
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiProvider implements LLMProvider {

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "gemini";
    }

    @Override
    public MessagesResponse chat(MessagesRequest request, RouterConfig config) {
        String apiKey = config.getGeminiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        String model = config.getGeminiModel() != null ? config.getGeminiModel() : DEFAULT_MODEL;
        String url = GEMINI_API_BASE + model + ":generateContent?key=" + apiKey;

        Map<String, Object> geminiBody = buildGeminiBody(request);

        log.info("[GeminiProvider] Sending request to Gemini, model={}", model);

        JsonNode raw = webClientBuilder.build()
                .post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(geminiBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(JsonNode.class)
                                .flatMap(body -> {
                                    String msg = body.path("error").path("message").asText("");
                                    if (msg.isBlank()) msg = "HTTP " + response.statusCode().value();
                                    return Mono.error(new RuntimeException("Gemini: " + msg));
                                })
                )
                .bodyToMono(JsonNode.class)
                .block();

        return parseGeminiResponse(raw, model);
    }

    private Map<String, Object> buildGeminiBody(MessagesRequest request) {
        List<Map<String, Object>> contents = new ArrayList<>();

        for (MessagesRequest.Message msg : request.getMessages()) {
            String geminiRole = "user".equals(msg.getRole()) ? "user" : "model";
            String text = extractText(msg.getContent());

            Map<String, Object> turn = new HashMap<>();
            turn.put("role", geminiRole);
            turn.put("parts", List.of(Map.of("text", text)));
            contents.add(turn);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);

        // Use Gemini's native systemInstruction field instead of prepending as user turn
        if (request.getSystem() != null && !request.getSystem().isBlank()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", request.getSystem()))));
        }

        // Generation config
        Map<String, Object> genConfig = new HashMap<>();
        if (request.getMaxTokens() != null) {
            genConfig.put("maxOutputTokens", request.getMaxTokens());
        }
        if (request.getTemperature() != null) {
            genConfig.put("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            genConfig.put("topP", request.getTopP());
        }
        if (request.getTopK() != null) {
            genConfig.put("topK", request.getTopK());
        }
        if (!genConfig.isEmpty()) {
            body.put("generationConfig", genConfig);
        }

        return body;
    }

    @SuppressWarnings("unchecked")
    private String extractText(Object content) {
        if (content instanceof String s) {
            return s;
        }
        if (content instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                if (item instanceof Map<?, ?> block) {
                    Object text = block.get("text");
                    if (text != null) sb.append(text);
                }
            }
            return sb.toString();
        }
        return content != null ? content.toString() : "";
    }

    private MessagesResponse parseGeminiResponse(JsonNode node, String model) {
        if (node == null) {
            throw new RuntimeException("Empty response from Gemini");
        }

        if (node.has("error")) {
            String errMsg = node.get("error").path("message").asText("Unknown Gemini error");
            throw new RuntimeException("Gemini API error: " + errMsg);
        }

        String text = "";
        JsonNode candidates = node.path("candidates");
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && parts.size() > 0) {
                text = parts.get(0).path("text").asText("");
            }
        }

        MessagesResponse.Usage usage = null;
        JsonNode usageNode = node.path("usageMetadata");
        if (!usageNode.isMissingNode()) {
            usage = MessagesResponse.Usage.builder()
                    .inputTokens(usageNode.path("promptTokenCount").asInt(0))
                    .outputTokens(usageNode.path("candidatesTokenCount").asInt(0))
                    .build();
        }

        return MessagesResponse.builder()
                .id("gemini-" + UUID.randomUUID())
                .type("message")
                .role("assistant")
                .content(List.of(MessagesResponse.ContentBlock.builder()
                        .type("text")
                        .text(text)
                        .build()))
                .model(model)
                .stopReason("end_turn")
                .usage(usage)
                .build();
    }
}
