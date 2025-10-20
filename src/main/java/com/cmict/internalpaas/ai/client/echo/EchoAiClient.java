package com.cmict.internalpaas.ai.client.echo;

import com.cmict.internalpaas.ai.client.AiClient;
import com.cmict.internalpaas.ai.model.ChatMessage;
import com.cmict.internalpaas.ai.model.ChatRequest;
import com.cmict.internalpaas.ai.stream.ChatEventType;
import com.cmict.internalpaas.ai.stream.ChatStreamEvent;
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
