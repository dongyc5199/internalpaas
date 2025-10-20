package com.cmict.internalpaas.ai.client.moonshot;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MoonshotChatRequest(
        List<Message> messages,
        String model,
        Boolean stream,
        Options options
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Message(String role, String content) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Options(@JsonProperty("temperature") Double temperature, @JsonProperty("top_p") Double topP) {}
}
