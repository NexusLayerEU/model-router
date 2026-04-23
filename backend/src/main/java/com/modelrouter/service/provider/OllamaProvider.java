package com.modelrouter.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
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
 * Translates Anthropic Messages API requests to the Ollama /api/chat endpoint and back.
 *
 * Anthropic format  →  Ollama format  →  Anthropic format
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaProvider implements LLMProvider {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llama3";

    private final WebClient.Builder webClientBuilder;

    @Override
    public String getName() {
        return "ollama";
    }

    @Override
    public MessagesResponse chat(MessagesRequest request, RouterConfig config) {
        String baseUrl = config.getOllamaBaseUrl() != null ? config.getOllamaBaseUrl() : DEFAULT_BASE_URL;
        String model = config.getOllamaModel() != null ? config.getOllamaModel() : DEFAULT_MODEL;

        // Remove trailing slash
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        List<Map<String, String>> messages = buildOllamaMessages(request);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);

        // Options map
        Map<String, Object> options = new HashMap<>();
        if (request.getTemperature() != null) {
            options.put("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            options.put("top_p", request.getTopP());
        }
        if (request.getTopK() != null) {
            options.put("top_k", request.getTopK());
        }
        if (request.getMaxTokens() != null) {
            options.put("num_predict", request.getMaxTokens());
        }
        if (!options.isEmpty()) {
            body.put("options", options);
        }

        log.info("[OllamaProvider] Sending request to Ollama at {}, model={}", baseUrl, model);

        JsonNode raw = webClientBuilder.build()
                .post()
                .uri(baseUrl + "/api/chat")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(JsonNode.class)
                                .flatMap(errBody -> {
                                    String msg = errBody.path("error").asText("");
                                    if (msg.isBlank()) msg = "HTTP " + response.statusCode().value();
                                    return Mono.error(new RuntimeException("Ollama: " + msg));
                                })
                )
                .bodyToMono(JsonNode.class)
                .block();

        return parseOllamaResponse(raw, model);
    }

    private List<Map<String, String>> buildOllamaMessages(MessagesRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();

        // Prepend system message if present
        if (request.getSystem() != null && !request.getSystem().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.getSystem()));
        }

        for (MessagesRequest.Message msg : request.getMessages()) {
            String text = extractText(msg.getContent());
            messages.add(Map.of("role", msg.getRole(), "content", text));
        }

        return messages;
    }

    private String extractText(Object content) {
        if (content instanceof String s) return s;
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

    private MessagesResponse parseOllamaResponse(JsonNode node, String model) {
        if (node == null) {
            throw new RuntimeException("Empty response from Ollama");
        }

        if (node.has("error")) {
            throw new RuntimeException("Ollama error: " + node.get("error").asText());
        }

        String text = node.path("message").path("content").asText("");

        MessagesResponse.Usage usage = MessagesResponse.Usage.builder()
                .inputTokens(node.path("prompt_eval_count").asInt(0))
                .outputTokens(node.path("eval_count").asInt(0))
                .build();

        return MessagesResponse.builder()
                .id("ollama-" + UUID.randomUUID())
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
