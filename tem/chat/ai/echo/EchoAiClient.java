package com.waveterm.demo.chat.ai.echo;

import com.waveterm.demo.chat.ai.AiClient;
import com.waveterm.demo.chat.model.ChatMessage;
import com.waveterm.demo.chat.model.ChatRequest;
import com.waveterm.demo.chat.stream.ChatEventType;
import com.waveterm.demo.chat.stream.ChatStreamEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@Component
public class EchoAiClient implements AiClient {

    @Override
    public String id() {
        return "echo";
    }

    @Override
    public Stream<ChatStreamEvent> streamChat(ChatRequest request) {
        List<ChatMessage> messages = request.messages();
        String lastUser = messages.isEmpty() ? "" : messages.get(messages.size() - 1).content();
        return Stream.of(
                new ChatStreamEvent(ChatEventType.token, "Echo: " + lastUser),
                new ChatStreamEvent(ChatEventType.done, "")
        );
    }
}
