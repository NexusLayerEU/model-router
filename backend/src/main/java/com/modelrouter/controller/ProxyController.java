package com.modelrouter.controller;

import com.modelrouter.model.dto.MessagesRequest;
import com.modelrouter.model.dto.MessagesResponse;
import com.modelrouter.service.RouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Claude-compatible proxy endpoint.
 *
 * All client products must target this controller at:
 *   POST http://<router-host>:8080/v1/messages
 *
 * The router accepts a standard Anthropic Messages API request and returns
 * a standard Anthropic Messages API response, regardless of which backend
 * provider is currently active.
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ProxyController {

    private final RouterService routerService;

    /**
     * Primary chat endpoint — mirrors the Anthropic POST /v1/messages contract.
     * Clients must send an x-api-key header (the value is validated locally; it is
     * NOT forwarded to the upstream provider — each provider uses its own key stored
     * in the router config).
     */
    @PostMapping(
            value = "/messages",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<MessagesResponse> messages(
            @RequestBody MessagesRequest request,
            @RequestHeader(value = "x-api-key", required = false) String clientApiKey
    ) {
        log.info("[ProxyController] Received /v1/messages request");
        MessagesResponse response = routerService.route(request);
        return ResponseEntity.ok(response);
    }

    /** Health check — useful for load-balancer or uptime monitoring. */
    @GetMapping("/health")
    public ResponseEntity<java.util.Map<String, String>> health() {
        return ResponseEntity.ok(java.util.Map.of("status", "ok", "service", "ModelRouter"));
    }
}
