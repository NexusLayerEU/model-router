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

import java.util.List;
import java.util.UUID;

/**
 * Routes requests directly to the Anthropic Messages API.
 * The request is already in Anthropic format, so minimal transformation is needed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnthropicProvider implements LLMProvider {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final WebClient.Builder webClientBuilder;

    @Override
    public String getName() {
        return "anthropic";
    }

    @Override
    public MessagesResponse chat(MessagesRequest request, RouterConfig config) {
        String apiKey = config.getAnthropicApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Anthropic API key is not configured");
        }

        String model = config.getAnthropicModel() != null ? config.getAnthropicModel() : DEFAULT_MODEL;

        // Build a copy of the request with the router's configured model
        MessagesRequest outbound = MessagesRequest.builder()
                .model(model)
                .messages(request.getMessages())
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 1024)
                .temperature(request.getTemperature())
                .topP(request.getTopP())
                .topK(request.getTopK())
                .system(request.getSystem())
                .stream(false) // streaming handled separately if needed
                .stopSequences(request.getStopSequences())
                .build();

        log.info("[AnthropicProvider] Sending request to Anthropic, model={}", model);

        JsonNode raw = webClientBuilder.build()
                .post()
                .uri(ANTHROPIC_API_URL + "/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(outbound)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(JsonNode.class)
                                .flatMap(body -> {
                                    String msg = body.path("error").path("message").asText("");
                                    if (msg.isBlank()) msg = "HTTP " + response.statusCode().value();
                                    return Mono.error(new RuntimeException("Anthropic: " + msg));
                                })
                )
                .bodyToMono(JsonNode.class)
                .block();

        return parseAnthropicResponse(raw);
    }

    private MessagesResponse parseAnthropicResponse(JsonNode node) {
        if (node == null) {
            throw new RuntimeException("Empty response from Anthropic");
        }

        MessagesResponse.Usage usage = null;
        if (node.has("usage")) {
            JsonNode u = node.get("usage");
            usage = MessagesResponse.Usage.builder()
                    .inputTokens(u.path("input_tokens").asInt(0))
                    .outputTokens(u.path("output_tokens").asInt(0))
                    .build();
        }

        List<MessagesResponse.ContentBlock> content = List.of();
        if (node.has("content") && node.get("content").isArray()) {
            content = new java.util.ArrayList<>();
            for (JsonNode block : node.get("content")) {
                content.add(MessagesResponse.ContentBlock.builder()
                        .type(block.path("type").asText("text"))
                        .text(block.path("text").asText(""))
                        .build());
            }
        }

        return MessagesResponse.builder()
                .id(node.path("id").asText(UUID.randomUUID().toString()))
                .type("message")
                .role("assistant")
                .content(content)
                .model(node.path("model").asText())
                .stopReason(node.path("stop_reason").asText("end_turn"))
                .usage(usage)
                .build();
    }
}
