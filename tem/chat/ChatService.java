package com.waveterm.demo.chat;

import com.waveterm.demo.chat.ai.AiClient;
import com.waveterm.demo.chat.ai.AiClientRegistry;
import com.waveterm.demo.chat.model.ChatRequest;
import com.waveterm.demo.chat.stream.ChatEventType;
import com.waveterm.demo.chat.stream.ChatStreamEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public ChatService(AiClientRegistry clientRegistry, ChatProperties properties, MeterRegistry meterRegistry) {
        this.clientRegistry = clientRegistry;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    public Stream<ChatStreamEvent> stream(ChatRequest request) {
        String cacheKey = cacheKey(request);
        Optional<Stream<ChatStreamEvent>> cached = lookupCache(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        AiClient client = resolveClient(request.client());
        int maxRetries = Math.max(properties.getMaxRetries(), 0);
        RuntimeException lastError = null;
        Timer.Sample sample = Timer.start(meterRegistry);
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                Stream<ChatStreamEvent> stream = client.streamChat(request);
                stream = cacheIfNeeded(cacheKey, stream);
                sample.stop(timer(client.id(), "success"));
                return stream;
            } catch (RuntimeException ex) {
                lastError = ex;
                log.warn("AI client {} failed attempt {}/{} for chatId={}", client.id(), attempt + 1, maxRetries + 1, request.chatId(), ex);
            }
        }
        RuntimeException error = lastError != null ? lastError : new RuntimeException("unknown AI error");
        log.error("AI client {} failed for chatId={}", client.id(), request.chatId(), error);
        sample.stop(timer(client.id(), "failure"));
        return Stream.of(
                new ChatStreamEvent(ChatEventType.error, error.getMessage() != null ? error.getMessage() : "unknown error"),
                new ChatStreamEvent(ChatEventType.done, "")
        );
    }

    private AiClient resolveClient(String requestedClient) {
        if (requestedClient == null || requestedClient.isBlank()) {
            return clientRegistry.pickWeightedClient();
        }
        return clientRegistry.get(requestedClient).orElseGet(() -> {
            log.warn("requested AI client {} not found, using default", requestedClient);
            return clientRegistry.pickWeightedClient();
        });
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

    private Timer timer(String clientId, String outcome) {
        return Timer.builder("chat.client.duration")
                .tag("client", clientId)
                .tag("outcome", outcome)
                .register(meterRegistry);
    }
}
