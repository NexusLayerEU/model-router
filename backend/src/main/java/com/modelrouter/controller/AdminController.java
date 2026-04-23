package com.modelrouter.controller;

import com.modelrouter.model.dto.ProviderConfigDto;
import com.modelrouter.service.RouterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin REST API consumed by the Router UI.
 * Base path: /api/admin
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RouterService routerService;

    /** GET /api/admin/config — returns current config with masked keys. */
    @GetMapping("/config")
    public ResponseEntity<ProviderConfigDto> getConfig() {
        return ResponseEntity.ok(routerService.getConfig());
    }

    /**
     * PUT /api/admin/config — partial update: only supplied non-blank fields are written.
     * Clients may send only the fields they wish to update.
     */
    @PutMapping("/config")
    public ResponseEntity<ProviderConfigDto> updateConfig(@Valid @RequestBody ProviderConfigDto dto) {
        return ResponseEntity.ok(routerService.updateConfig(dto));
    }

    /**
     * POST /api/admin/provider/switch — switches the active provider.
     * Body: { "provider": "gemini" }
     */
    @PostMapping("/provider/switch")
    public ResponseEntity<ProviderConfigDto> switchProvider(@RequestBody Map<String, String> body) {
        String provider = body.get("provider");
        if (provider == null || provider.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(routerService.switchProvider(provider));
    }

    /** GET /api/admin/providers — returns list of all registered provider names. */
    @GetMapping("/providers")
    public ResponseEntity<List<String>> getProviders() {
        return ResponseEntity.ok(routerService.getAvailableProviders());
    }

    /**
     * GET /api/admin/ollama/models — returns locally installed Ollama model names
     * by querying the configured Ollama instance's /api/tags endpoint.
     * Returns an empty list (not an error) if Ollama is unreachable.
     */
    @GetMapping("/ollama/models")
    public ResponseEntity<List<String>> getOllamaModels() {
        return ResponseEntity.ok(routerService.getOllamaModels());
    }
}
