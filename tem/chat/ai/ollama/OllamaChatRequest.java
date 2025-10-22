package com.waveterm.demo.chat.ai.ollama;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaChatRequest(String model,
                                List<Message> messages,
                                Boolean stream,
                                Map<String, Object> options) {

    public record Message(String role, String content) {}
}
