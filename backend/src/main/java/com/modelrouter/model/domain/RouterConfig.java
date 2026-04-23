package com.modelrouter.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity persisting router configuration to the H2 file-based database.
 * A single row (id=1) holds the current global config.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "router_config")
public class RouterConfig {

    @Id
    private Long id;

    /** Currently active provider: "anthropic" | "gemini" | "ollama" */
    @Column(nullable = false)
    private String activeProvider;

    // ── Anthropic ─────────────────────────────────────────────────────────────
    @Column(length = 512)
    private String anthropicApiKey;

    @Column(length = 128)
    private String anthropicModel;

    // ── Gemini ────────────────────────────────────────────────────────────────
    @Column(length = 512)
    private String geminiApiKey;

    @Column(length = 128)
    private String geminiModel;

    // ── Ollama ────────────────────────────────────────────────────────────────
    @Column(length = 512)
    private String ollamaBaseUrl;

    @Column(length = 128)
    private String ollamaModel;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
