package com.modelrouter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sends LLM_CALL telemetry events to WatchGrid after every ModelRouter request.
 * Each /v1/messages call becomes one WatchGrid run: RUN_START → LLM_CALL → RUN_END.
 * Failures are swallowed and logged — telemetry must never break the main request path.
 *
 * Config (docker-compose env):
 *   WATCHGRID_URL     http://192.168.68.111:8085  (default)
 *   WATCHGRID_SDK_KEY wg_sdk_...                  (required to enable)
 */
@Slf4j
@Service
public class WatchGridService {

    /** Cost per million tokens (input, output) for each model. */
    private static final Map<String, double[]> PRICING = Map.of(
            "gemini-2.5-flash",             new double[]{0.15,  0.60},
            "gemini-2.0-flash",             new double[]{0.10,  0.40},
            "gemini-1.5-flash",             new double[]{0.075, 0.30},
            "claude-sonnet-4-20250514",     new double[]{3.00, 15.00},
            "claude-haiku-4-5-20251001",    new double[]{0.80,  4.00},
            "claude-3-5-sonnet-20241022",   new double[]{3.00, 15.00},
            "claude-3-5-haiku-20241022",    new double[]{0.80,  4.00},
            "llama3:latest",                new double[]{0.00,  0.00},
            "llama3.1:8b",                  new double[]{0.00,  0.00}
    );

    private final WebClient.Builder webClientBuilder;

    @Value("${watchgrid.url:http://192.168.68.111:8085}")
    private String watchgridUrl;

    @Value("${watchgrid.sdk-key:}")
    private String sdkKey;

    public WatchGridService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Logs a completed LLM call to WatchGrid asynchronously.
     * @param provider   active provider name ("gemini", "anthropic", "ollama")
     * @param model      model name returned in the response
     * @param inputTokens  prompt token count
     * @param outputTokens completion token count
     * @param latencyMs  wall-clock time for the LLM call
     */
    public void logLlmCall(String provider, String model,
                           int inputTokens, int outputTokens, long latencyMs) {
        if (sdkKey == null || sdkKey.isBlank()) return;

        String runId   = UUID.randomUUID().toString();
        String now     = Instant.now().toString();
        String taggedModel = provider + "/" + model;
        double costUsd = computeCost(model, inputTokens, outputTokens);

        List<Map<String, Object>> events = List.of(
                Map.of("type", "RUN_START", "runId", runId, "timestamp", now),
                Map.of("type",         "LLM_CALL",
                       "runId",        runId,
                       "model",        taggedModel,
                       "inputTokens",  inputTokens,
                       "outputTokens", outputTokens,
                       "costUsd",      costUsd,
                       "latencyMs",    (int) latencyMs,
                       "timestamp",    now),
                Map.of("type", "RUN_END", "runId", runId,
                       "status", "SUCCESS", "timestamp", now)
        );

        webClientBuilder.build()
                .post()
                .uri(watchgridUrl + "/ingest/events")
                .header("X-SDK-Key", sdkKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("events", events))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        r -> log.debug("[WatchGrid] Logged {} {} in={} out={} cost=${}", provider, model, inputTokens, outputTokens, costUsd),
                        e -> log.warn("[WatchGrid] Ingest failed (non-fatal): {}", e.getMessage())
                );
    }

    private double computeCost(String model, int inputTokens, int outputTokens) {
        double[] prices = PRICING.getOrDefault(model, new double[]{0.0, 0.0});
        return (inputTokens * prices[0] + outputTokens * prices[1]) / 1_000_000.0;
    }
}
