package com.waveterm.demo.chat.model;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ChatRequest(String chatId,
                          @NotEmpty List<ChatMessage> messages,
                          ChatContext context,
                          String client) {
}
