package com.cmict.internalpaas.ai;

import com.cmict.internalpaas.ai.client.AiClient;
import com.cmict.internalpaas.ai.client.AiClientRegistry;
import com.cmict.internalpaas.ai.moonshot.MoonshotProperties;
import com.cmict.internalpaas.ai.ollama.OllamaProperties;
import com.cmict.internalpaas.ai.model.ChatRequest;
import com.cmict.internalpaas.ai.stream.ChatEventType;
import com.cmict.internalpaas.ai.stream.ChatStreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final AiClientRegistry clientRegistry;
    private final ChatProperties properties;
    private final MoonshotProperties moonshotProperties;
    private final OllamaProperties ollamaProperties;
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    public ChatService(AiClientRegistry clientRegistry,
                       ChatProperties properties,
                       MoonshotProperties moonshotProperties,
                       OllamaProperties ollamaProperties) {
        this.clientRegistry = clientRegistry;
        this.properties = properties;
        this.moonshotProperties = moonshotProperties;
        this.ollamaProperties = ollamaProperties;
    }

    public Stream<ChatStreamEvent> stream(ChatRequest request) {
        String cacheKey = cacheKey(request);
        Optional<Stream<ChatStreamEvent>> cached = lookupCache(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }
        AiClient client = resolveClient(request.client());
        log.info("AI stream start: chatId={}, requestedClient/model={}, resolvedClient={}",
                request.chatId(), request.client(), client.id());
        int maxRetries = Math.max(properties.getMaxRetries(), 0);
        RuntimeException lastError = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                Stream<ChatStreamEvent> stream = client.streamChat(request);
                stream = cacheIfNeeded(cacheKey, stream);
                return stream;
            } catch (RuntimeException ex) {
                lastError = ex;
                log.warn("AI client {} failed attempt {}/{} for chatId={}: {}", client.id(), attempt + 1, maxRetries + 1,
                        request.chatId(), ex.getMessage());
                sleepBackoff(attempt);
            }
        }
        RuntimeException error = lastError != null ? lastError : new RuntimeException("unknown AI error");
        String rootMessage = resolveRootMessage(error);
        log.error("AI client {} failed for chatId={}, retries={}, reason={}",
                client.id(), request.chatId(), maxRetries + 1, rootMessage, error);
        String guidance = """
AI服务调用失败（client=%s）。原因：%s
请检查网络连通性、API Key 或模型服务状态后重试。
""".formatted(client.id(), rootMessage);
        return Stream.of(
                new ChatStreamEvent(ChatEventType.error, guidance.strip()),
                new ChatStreamEvent(ChatEventType.done, "")
        );
    }

    private void sleepBackoff(int attempt) {
        long backoff = Math.min(2L << attempt, 5);
        try {
            TimeUnit.MILLISECONDS.sleep(backoff * 100L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private AiClient resolveClient(String requestedClient) {
        // requestedClient may be either a client id (e.g. 'moonshot','ollama','echo')
        // or a model id selected from the UI (e.g. 'kimi-k2-0905-preview' or 'gpt-oss:20b').
        if (requestedClient == null || requestedClient.isBlank()) {
            return clientRegistry.pickWeightedClient();
        }

        // direct client id match
        var direct = clientRegistry.get(requestedClient);
        if (direct.isPresent()) {
            return direct.get();
        }

        // try mapping model id to known providers
        try {
            if (moonshotProperties != null) {
                String m = moonshotProperties.getModel();
                if (m != null && m.equalsIgnoreCase(requestedClient)) {
                    return clientRegistry.get("moonshot").orElseGet(clientRegistry::pickWeightedClient);
                }
            }
        } catch (Exception ignored) {}

        try {
            if (ollamaProperties != null && ollamaProperties.isEnabled()) {
                String o = ollamaProperties.getModel();
                if (o != null && o.equalsIgnoreCase(requestedClient)) {
                    return clientRegistry.get("ollama").orElseGet(clientRegistry::pickWeightedClient);
                }
            }
        } catch (Exception ignored) {}

        log.warn("requested AI client/model '{}' not found, using default", requestedClient);
        return clientRegistry.pickWeightedClient();
    }

    private Optional<Stream<ChatStreamEvent>> lookupCache(String cacheKey) {
        Duration ttl = properties.getCacheTtl();
        if (ttl.isZero() || ttl.isNegative()) {
            return Optional.empty();
        }
        CachedResponse cached = cache.get(cacheKey);
        if (cached == null || cached.expired(ttl)) {
            cache.remove(cacheKey);
            return Optional.empty();
        }
        return Optional.of(Stream.of(cached.events()).flatMap(Stream::of));
    }

    private Stream<ChatStreamEvent> cacheIfNeeded(String cacheKey, Stream<ChatStreamEvent> stream) {
        Duration ttl = properties.getCacheTtl();
        if (ttl.isZero() || ttl.isNegative()) {
            return stream;
        }
        ChatStreamEvent[] events = stream.toArray(ChatStreamEvent[]::new);
        cache.put(cacheKey, new CachedResponse(events, Instant.now()));
        return Stream.of(events);
    }

    private String cacheKey(ChatRequest request) {
        return request.chatId() + "::" + request.messages().hashCode() + "::" + Objects.hashCode(request.context()) + "::" + Objects.hashCode(request.client());
    }

    private record CachedResponse(ChatStreamEvent[] events, Instant storedAt) {
        boolean expired(Duration ttl) {
            return storedAt.plus(ttl).isBefore(Instant.now());
        }
    }

    private String resolveRootMessage(Throwable throwable) {
        if (throwable == null) {
            return "未知错误";
        }
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            message = cause.getClass().getSimpleName();
        }
        return message;
    }
}
