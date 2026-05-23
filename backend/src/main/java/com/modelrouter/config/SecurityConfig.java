package com.modelrouter.config;

import com.modelrouter.security.SsoJwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SsoJwtAuthFilter ssoJwtAuthFilter;

    public SecurityConfig(SsoJwtAuthFilter ssoJwtAuthFilter) {
        this.ssoJwtAuthFilter = ssoJwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/h2-console/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(ssoJwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
