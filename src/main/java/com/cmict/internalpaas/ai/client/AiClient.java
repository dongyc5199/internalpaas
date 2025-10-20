package com.cmict.internalpaas.ai.client;

import com.cmict.internalpaas.ai.model.ChatRequest;
import com.cmict.internalpaas.ai.stream.ChatStreamEvent;

import java.time.Duration;
import java.util.stream.Stream;

public interface AiClient {

    String id();

    Stream<ChatStreamEvent> streamChat(ChatRequest request);

    default Duration timeoutHint() {
        return Duration.ofSeconds(30);
    }
}
