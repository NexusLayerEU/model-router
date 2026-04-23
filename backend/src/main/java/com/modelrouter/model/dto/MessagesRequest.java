package com.modelrouter.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages API request format.
 * All clients (products) must send requests in this format to the router.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessagesRequest {

    /** Model identifier — the router ignores this and uses the active provider's model. */
    private String model;

    private List<Message> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("top_k")
    private Integer topK;

    /** Optional system prompt. */
    private String system;

    /** Whether to stream the response. */
    private Boolean stream;

    private List<Map<String, Object>> tools;

    @JsonProperty("tool_choice")
    private Map<String, Object> toolChoice;

    @JsonProperty("stop_sequences")
    private List<String> stopSequences;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;
        private Object content; // String or List<ContentBlock>
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentBlock {
        private String type;
        private String text;
    }
}
