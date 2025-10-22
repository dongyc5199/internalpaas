package com.waveterm.demo.chat.ai.moonshot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waveterm.demo.chat.ai.AiClient;
import com.waveterm.demo.chat.model.ChatMessage;
import com.waveterm.demo.chat.model.ChatRequest;
import com.waveterm.demo.chat.stream.ChatEventType;
import com.waveterm.demo.chat.stream.ChatStreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(prefix = "moonshot", name = "api-key")
public class MoonshotAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(MoonshotAiClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final MoonshotProperties properties;
    private final RestClient restClient;

    public MoonshotAiClient(MoonshotProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofMinutes(3).toMillis());
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public String id() {
        return "moonshot";
    }

    @Override
    public Stream<ChatStreamEvent> streamChat(ChatRequest request) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return Stream.of(new ChatStreamEvent(ChatEventType.error, "Moonshot API key missing"));
        }
        try {
            MoonshotChatRequest payload = buildPayload(request);
            List<ChatStreamEvent> events = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(payload)
                    .exchange((clientRequest, clientResponse) -> {
                        if (clientResponse.getStatusCode().isError()) {
                            throw new RestClientException("Moonshot request failed: " + clientResponse.getStatusCode());
                        }
                        final InputStream body;
                        try {
                            body = clientResponse.getBody();
                        } catch (IOException ex) {
                            throw new RestClientException("Moonshot stream access failed", ex);
                        }
                        if (body == null) {
                            return List.of(new ChatStreamEvent(ChatEventType.error, "Moonshot response empty"));
                        }
                        try {
                            return parseStream(body);
                        } catch (IOException ex) {
                            throw new RestClientException("Moonshot stream decoding failed", ex);
                        } finally {
                            try {
                                body.close();
                            } catch (IOException ignored) {
                            }
                        }
                    });
            return events.stream();
        } catch (RestClientException ex) {
            log.error("Moonshot request failed", ex);
            return Stream.of(
                    new ChatStreamEvent(ChatEventType.error, ex.getMessage() == null ? "Moonshot request failed" : ex.getMessage()),
                    new ChatStreamEvent(ChatEventType.done, "")
            );
        }
    }

    private MoonshotChatRequest buildPayload(ChatRequest request) {
        List<MoonshotChatRequest.Message> messages = new ArrayList<>();
        if (request.context() != null && request.context().terminalTail() != null && !request.context().terminalTail().isBlank()) {
            messages.add(new MoonshotChatRequest.Message("system", "Recent terminal context:\n" + request.context().terminalTail()));
        }
        for (ChatMessage message : request.messages()) {
            String role = switch (message.role()) {
                case system -> "system";
                case user -> "user";
                case assistant -> "assistant";
            };
            messages.add(new MoonshotChatRequest.Message(role, message.content()));
        }
        MoonshotChatRequest.Options options = new MoonshotChatRequest.Options(null, null);
        boolean stream = true;
        return new MoonshotChatRequest(messages, properties.getModel(), stream, options);
    }

    private List<ChatStreamEvent> parseStream(InputStream stream) throws IOException {
        List<ChatStreamEvent> events = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                JsonNode root = mapper.readTree(data);
                JsonNode error = root.path("error");
                if (!error.isMissingNode() && error.hasNonNull("message")) {
                    events.add(new ChatStreamEvent(ChatEventType.error, error.get("message").asText()));
                    break;
                }
                JsonNode choices = root.path("choices");
                boolean finished = false;
                if (choices.isArray()) {
                    for (JsonNode choice : choices) {
                        JsonNode delta = choice.path("delta");
                        JsonNode content = delta.path("content");
                        if (!content.isMissingNode() && !content.asText().isEmpty()) {
                            events.add(new ChatStreamEvent(ChatEventType.token, content.asText()));
                        }
                        String finish = choice.path("finish_reason").asText(null);
                        if ("stop".equalsIgnoreCase(finish)) {
                            finished = true;
                        }
                    }
                }
                if (finished) {
                    break;
                }
            }
        }
        events.add(new ChatStreamEvent(ChatEventType.done, ""));
        return events;
    }
}
