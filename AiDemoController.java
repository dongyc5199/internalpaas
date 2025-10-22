package com.cmict.internalpaas.controller;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI Demo API 控制�? * 提供临时 Stub 接口支撑前端 Demo，后续会替换�?WaveTerm AI 服务实现�? */
@RestController
@RequestMapping("/ai")
public class AiDemoController {

    private static final Logger log = LoggerFactory.getLogger(AiDemoController.class);

    /**
     * 简单的命令补全 Stub，实现返回固定建议�?     */
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
     * AI 对话流式输出 Stub，通过 SseEmitter 模拟流式响应行为�?     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> payload) {
        var emitter = new SseEmitter(Duration.ofSeconds(30).toMillis());
        String model = payload != null ? (String) payload.getOrDefault("model", "local.echo") : "local.echo";
        String message = payload != null ? (String) payload.getOrDefault("message", "") : "";

        log.debug("收到 AI 对话请求 model={}, message={}", model, message);

        try {
            var intro = "已连接模�?" + model + "。\n";
            emitter.send(SseEmitter.event().name("message").data(intro));

            StringBuilder builder = new StringBuilder();
            builder.append("根据当前终端上下文，建议执行：\n");
            builder.append("1. journalctl -fu internal-paas\n");
            builder.append("2. tail -n 200 /var/log/internal-paas/app.log\n");
            builder.append("3. kubectl get pod -l app=internal-paas -A\n");

            if (!CollectionUtils.isEmpty(payload)) {
                builder.append("\n\n收到你的提问：\n");
                builder.append(message);
            }

            emitter.send(SseEmitter.event().name("message").data(builder.toString()));
            emitter.complete();
        } catch (IOException ex) {
            log.warn("发�?AI 流式消息失败", ex);
            emitter.completeWithError(ex);
        }

        return emitter;
    }
}

