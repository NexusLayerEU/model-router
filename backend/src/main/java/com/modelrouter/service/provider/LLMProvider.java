package com.modelrouter.service.provider;

import com.modelrouter.model.domain.RouterConfig;
import com.modelrouter.model.dto.MessagesRequest;
import com.modelrouter.model.dto.MessagesResponse;

/**
 * Common contract for all LLM provider adapters.
 * Each implementation receives a Claude-format request and returns a Claude-format response.
 */
public interface LLMProvider {

    /**
     * Returns the unique provider identifier (e.g. "anthropic", "gemini", "ollama").
     */
    String getName();

    /**
     * Sends the request to the underlying LLM and returns a normalised Anthropic-format response.
     *
     * @param request   Anthropic Messages API request
     * @param config    Current router configuration (keys, URLs, model names)
     * @return          Anthropic Messages API response
     */
    MessagesResponse chat(MessagesRequest request, RouterConfig config);
}
