package com.waveterm.demo.chat.ai;

import com.waveterm.demo.chat.model.ChatRequest;
import com.waveterm.demo.chat.stream.ChatStreamEvent;

import java.time.Duration;
import java.util.stream.Stream;

public interface AiClient {

    String id();

    Stream<ChatStreamEvent> streamChat(ChatRequest request);

    default Duration timeoutHint() {
        return Duration.ofSeconds(30);
    }
}
