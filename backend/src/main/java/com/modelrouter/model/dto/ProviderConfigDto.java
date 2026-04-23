package com.modelrouter.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for provider configuration sent from the UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderConfigDto {

    private String provider; // "anthropic" | "gemini" | "ollama"

    /** Anthropic API key */
    private String anthropicApiKey;

    /** Anthropic model (e.g. claude-sonnet-4-20250514) */
    private String anthropicModel;

    /** Gemini API key */
    private String geminiApiKey;

    /** Gemini model override (default: gemini-2.0-flash) */
    private String geminiModel;

    /** Ollama base URL (e.g. http://localhost:11434) */
    private String ollamaBaseUrl;

    /** Ollama model name (e.g. llama3, mistral) */
    private String ollamaModel;

    /** Whether to mask sensitive values in the response */
    private Boolean masked;
}
