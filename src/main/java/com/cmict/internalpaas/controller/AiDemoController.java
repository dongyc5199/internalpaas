package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.ai.ChatService;
import com.cmict.internalpaas.ai.dto.AiChatRequest;
import com.cmict.internalpaas.ai.model.ChatContext;
import com.cmict.internalpaas.ai.model.ChatMessage;
import com.cmict.internalpaas.ai.model.ChatRequest;
import com.cmict.internalpaas.ai.stream.ChatEventType;
import com.cmict.internalpaas.ai.stream.ChatStreamEvent;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * AI Demo API 控制器，提供命令补全示例与聊天流式接口。
 */
@RestController
@RequestMapping("/ai")
@Validated
public class AiDemoController {

    private static final Logger log = LoggerFactory.getLogger(AiDemoController.class);
    private final ChatService chatService;

    public AiDemoController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 命令补全示例（后续可以替换为真实实现）。
     */
    @PostMapping("/completion/suggest")
    public ResponseEntity<Map<String, Object>> suggest(@RequestBody Map<String, Object> request) {
        String prompt = request != null ? (String) request.getOrDefault("prompt", "") : "";
        log.debug("收到补全请求 prompt={}", prompt);

        List<Map<String, Object>> suggestions = List.of(
                Map.of("text", "ls -lah", "confidence", 0.82, "provider", "历史命令"),
                Map.of("text", "journalctl -fu internal-paas", "confidence", 0.78, "provider", "KimiK2"),
                Map.of("text", "systemctl restart internal-paas", "confidence", 0.71, "provider", "本地模型")
        );

        return ResponseEntity.ok(Map.of(
                "prompt", prompt,
                "suggestions", suggestions,
                "generatedAt", Instant.now().toString()
        ));
    }

    /**
     * AI 对话接口，使用 ChatService 输出流式消息。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody AiChatRequest payload) {
        String chatId = payload.getChatId();
        if (chatId == null || chatId.isBlank()) {
            chatId = "chat-" + UUID.randomUUID().toString().replace("-", "");
        }

        ChatRequest request = new ChatRequest(
                chatId,
                List.of(new ChatMessage(ChatMessage.Role.user, payload.getMessage())),
                new ChatContext(payload.getTerminalTail(), Collections.emptyList()),
                payload.getModel()
        );

        log.debug("收到 AI 对话请求 chatId={}, model={}, sessionId={}", chatId, payload.getModel(), payload.getSessionId());

        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(5).toMillis());

        CompletableFuture.runAsync(() -> {
            try (Stream<ChatStreamEvent> stream = chatService.stream(request)) {
                stream.forEach(event -> sendEvent(emitter, event));
                emitter.complete();
            } catch (RuntimeException ex) {
                handleStreamError(emitter, ex);
            }
        });

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, ChatStreamEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event.type().name())
                    .data(event.data() == null ? "" : event.data()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void handleStreamError(SseEmitter emitter, RuntimeException ex) {
        try {
            emitter.send(SseEmitter.event()
                    .name(ChatEventType.error.name())
                    .data(ex.getMessage() == null ? "AI chat error" : ex.getMessage()));
        } catch (IOException ignored) {
        }
        emitter.completeWithError(ex);
    }
}
