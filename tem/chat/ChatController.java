package com.waveterm.demo.chat;

import com.waveterm.demo.chat.model.ChatRequest;
import com.waveterm.demo.chat.stream.ChatEventType;
import com.waveterm.demo.chat.stream.ChatStreamEvent;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
@Validated
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        new Thread(() -> {
            try {
                for (ChatStreamEvent event : (Iterable<ChatStreamEvent>) chatService.stream(request)::iterator) {
                    emitter.send(SseEmitter.event().name(event.type().name()).data(event.data()));
                }
                emitter.send(SseEmitter.event().name(ChatEventType.done.name()).data(""));
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name(ChatEventType.error.name()).data(ex.getMessage() == null ? "error" : ex.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(ex);
            }
        }, "chat-stream-emitter").start();
        return emitter;
    }
}
