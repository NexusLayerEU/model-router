package com.modelrouter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.modelrouter.model.domain.RouterConfig;
import com.modelrouter.model.dto.MessagesRequest;
import com.modelrouter.model.dto.MessagesResponse;
import com.modelrouter.model.dto.ProviderConfigDto;
import com.modelrouter.repository.RouterConfigRepository;
import com.modelrouter.service.provider.LLMProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.Duration;
import java.time.Instant;

/**
 * Core routing service.
 * <ul>
 *   <li>Loads and persists router config in H2 (single-row, id=1).</li>
 *   <li>Dispatches incoming Claude-format requests to the active provider.</li>
 *   <li>Manages provider config updates from the Admin UI.</li>
 * </ul>
 */
@Slf4j
@Service
public class RouterService {

    private static final long CONFIG_ID = 1L;

    private final RouterConfigRepository configRepo;
    private final Map<String, LLMProvider> providers;
    private final WebClient.Builder webClientBuilder;
    private final WatchGridService watchGrid;

    public RouterService(RouterConfigRepository configRepo,
                         List<LLMProvider> providerList,
                         WebClient.Builder webClientBuilder,
                         WatchGridService watchGrid) {
        this.configRepo = configRepo;
        this.webClientBuilder = webClientBuilder;
        this.watchGrid = watchGrid;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(LLMProvider::getName, Function.identity()));
    }

    /** Ensures a default config row exists on startup. */
    @PostConstruct
    @Transactional
    public void init() {
        if (!configRepo.existsById(CONFIG_ID)) {
            RouterConfig defaults = RouterConfig.builder()
                    .id(CONFIG_ID)
                    .activeProvider("anthropic")
                    .anthropicModel("claude-sonnet-4-20250514")
                    .geminiModel("gemini-2.5-flash")
                    .ollamaBaseUrl("http://172.20.33.239:11434")
                    .ollamaModel("llama3:latest")
                    .build();
            configRepo.save(defaults);
            log.info("[RouterService] Initialised default config");
        }
    }

    /** Routes a Claude-format request to the currently active provider. */
    public MessagesResponse route(MessagesRequest request) {
        RouterConfig config = loadConfig();
        String active = config.getActiveProvider();
        LLMProvider provider = providers.get(active);

        if (provider == null) {
            throw new IllegalStateException("Unknown provider: " + active);
        }

        log.info("[RouterService] Routing request to provider={}", active);
        Instant start = Instant.now();
        MessagesResponse response = provider.chat(request, config);
        long latencyMs = Duration.between(start, Instant.now()).toMillis();

        // Log to WatchGrid asynchronously — never blocks the response
        if (response.getUsage() != null) {
            watchGrid.logLlmCall(
                    active,
                    response.getModel(),
                    response.getUsage().getInputTokens(),
                    response.getUsage().getOutputTokens(),
                    latencyMs
            );
        }

        return response;
    }

    /** Returns the current config (API keys masked). */
    public ProviderConfigDto getConfig() {
        RouterConfig cfg = loadConfig();
        return ProviderConfigDto.builder()
                .provider(cfg.getActiveProvider())
                .anthropicApiKey(mask(cfg.getAnthropicApiKey()))
                .anthropicModel(cfg.getAnthropicModel())
                .geminiApiKey(mask(cfg.getGeminiApiKey()))
                .geminiModel(cfg.getGeminiModel())
                .ollamaBaseUrl(cfg.getOllamaBaseUrl())
                .ollamaModel(cfg.getOllamaModel())
                .masked(true)
                .build();
    }

    /** Persists updated config. Only non-null fields in the DTO overwrite stored values. */
    @Transactional
    public ProviderConfigDto updateConfig(ProviderConfigDto dto) {
        RouterConfig cfg = loadConfig();

        if (dto.getProvider() != null) {
            if (!providers.containsKey(dto.getProvider())) {
                throw new IllegalArgumentException("Unknown provider: " + dto.getProvider());
            }
            cfg.setActiveProvider(dto.getProvider());
        }

        // Only update keys/URLs/models if explicitly provided (non-blank)
        if (isProvided(dto.getAnthropicApiKey())) {
            cfg.setAnthropicApiKey(dto.getAnthropicApiKey());
        }
        if (isProvided(dto.getAnthropicModel())) {
            cfg.setAnthropicModel(dto.getAnthropicModel());
        }
        if (isProvided(dto.getGeminiApiKey())) {
            cfg.setGeminiApiKey(dto.getGeminiApiKey());
        }
        if (isProvided(dto.getGeminiModel())) {
            cfg.setGeminiModel(dto.getGeminiModel());
        }
        if (isProvided(dto.getOllamaBaseUrl())) {
            cfg.setOllamaBaseUrl(dto.getOllamaBaseUrl());
        }
        if (isProvided(dto.getOllamaModel())) {
            cfg.setOllamaModel(dto.getOllamaModel());
        }

        configRepo.save(cfg);
        log.info("[RouterService] Config updated, active={}", cfg.getActiveProvider());
        return getConfig();
    }

    /** Switches the active provider only. */
    @Transactional
    public ProviderConfigDto switchProvider(String providerName) {
        if (!providers.containsKey(providerName)) {
            throw new IllegalArgumentException("Unknown provider: " + providerName);
        }
        RouterConfig cfg = loadConfig();
        cfg.setActiveProvider(providerName);
        configRepo.save(cfg);
        log.info("[RouterService] Switched active provider to {}", providerName);
        return getConfig();
    }

    /** Returns all registered provider names. */
    public List<String> getAvailableProviders() {
        return List.copyOf(providers.keySet());
    }

    /** Calls the configured Ollama instance's /api/tags endpoint and returns
     * a list of locally installed model names. Times out after 3 seconds.
     */
    public List<String> getOllamaModels() {
        RouterConfig cfg = loadConfig();
        String baseUrl = cfg.getOllamaBaseUrl() != null ? cfg.getOllamaBaseUrl() : "http://172.20.33.239:11434";
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

        try {
            JsonNode response = webClientBuilder.build()
                    .get()
                    .uri(baseUrl + "/api/tags")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(java.time.Duration.ofSeconds(3))
                    .block();

            if (response == null || !response.has("models")) return List.of();

            List<String> names = new java.util.ArrayList<>();
            for (JsonNode model : response.get("models")) {
                String name = model.path("name").asText("");
                if (!name.isBlank()) names.add(name);
            }
            return names;
        } catch (Exception e) {
            log.warn("[RouterService] Could not fetch Ollama models from {}: {}", baseUrl, e.getMessage());
            return List.of();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RouterConfig loadConfig() {
        return configRepo.findById(CONFIG_ID)
                .orElseThrow(() -> new IllegalStateException("Router config not initialised"));
    }

    private boolean isProvided(String value) {
        return value != null && !value.isBlank();
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() <= 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
