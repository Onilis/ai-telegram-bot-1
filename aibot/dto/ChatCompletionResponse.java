package com.aibot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ChatCompletionResponse {

    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices; // ✅ Типизированный список
    private Usage usage;

    @Data
    @NoArgsConstructor
    public static class Choice {
        private Integer index;
        private ChatMessage message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @NoArgsConstructor
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}