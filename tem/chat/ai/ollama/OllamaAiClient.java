package com.waveterm.demo.chat.ai.ollama;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.waveterm.demo.chat.ai.AiClient;
import com.waveterm.demo.chat.model.ChatMessage;
import com.waveterm.demo.chat.model.ChatRequest;
import com.waveterm.demo.chat.stream.ChatEventType;
import com.waveterm.demo.chat.stream.ChatStreamEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(prefix = "ollama", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OllamaAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiClient.class);

    private final OllamaProperties properties;
    private final RestClient restClient;
    private final JsonFactory jsonFactory;

    public OllamaAiClient(OllamaProperties properties) {
        this.properties = properties;
        this.jsonFactory = new JsonFactory();
        Duration connectTimeout = properties.getConnectTimeout();
        Duration readTimeout = properties.getReadTimeout();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeout.toMillis());
        factory.setReadTimeout((int) readTimeout.toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(properties.getHost() + ":" + properties.getPort())
                .requestFactory(factory)
                .build();
    }

    @Override
    public String id() {
        return "ollama";
    }

    @Override
    public Stream<ChatStreamEvent> streamChat(ChatRequest request) {
        if (!properties.isEnabled()) {
            return Stream.of(new ChatStreamEvent(ChatEventType.error, "Ollama client disabled"));
        }
        try {
            OllamaChatRequest payload = buildPayload(request);
            InputStream responseStream = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(InputStream.class);
            if (responseStream == null) {
                return Stream.of(new ChatStreamEvent(ChatEventType.error, "Ollama response empty"));
            }
            List<ChatStreamEvent> events = parseStream(responseStream);
            return events.stream();
        } catch (RestClientException | IOException ex) {
            log.error("Ollama request failed", ex);
            return Stream.of(
                    new ChatStreamEvent(ChatEventType.error, ex.getMessage() == null ? "Ollama request failed" : ex.getMessage()),
                    new ChatStreamEvent(ChatEventType.done, "")
            );
        }
    }

    private OllamaChatRequest buildPayload(ChatRequest request) {
        List<OllamaChatRequest.Message> messages = new ArrayList<>();
        for (ChatMessage message : request.messages()) {
            String role = switch (message.role()) {
                case system -> "system";
                case user -> "user";
                case assistant -> "assistant";
            };
            messages.add(new OllamaChatRequest.Message(role, message.content()));
        }
        Map<String, Object> options = new HashMap<>();
        options.put("chat_id", request.chatId());
        if (request.context() != null && request.context().terminalTail() != null) {
            options.put("context", request.context().terminalTail());
        }
        return new OllamaChatRequest(properties.getModel(), messages, true, options);
    }

    @SuppressWarnings("unchecked")
    private List<ChatStreamEvent> parseStream(InputStream stream) throws IOException {
        List<ChatStreamEvent> events = new ArrayList<>();
        try (JsonParser parser = jsonFactory.createParser(stream)) {
            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == null) {
                    break;
                }
                if (token == JsonToken.START_OBJECT) {
                    Map<String, Object> node = parser.readValueAs(Map.class);
                    Object error = node.get("error");
                    if (error instanceof String err && !err.isBlank()) {
                        events.add(new ChatStreamEvent(ChatEventType.error, err));
                        break;
                    }
                    Object messageObj = node.get("message");
                    if (messageObj instanceof Map<?, ?> messageMap) {
                        Object content = messageMap.get("content");
                        if (content instanceof String text && !text.isBlank()) {
                            events.add(new ChatStreamEvent(ChatEventType.token, text));
                        }
                    }
                    Object done = node.get("done");
                    if (done instanceof Boolean finished && finished) {
                        break;
                    }
                }
            }
        }
        events.add(new ChatStreamEvent(ChatEventType.done, ""));
        return events;
    }
}
