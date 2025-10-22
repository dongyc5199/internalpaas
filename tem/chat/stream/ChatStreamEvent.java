package com.waveterm.demo.chat.stream;

public record ChatStreamEvent(ChatEventType type, String data) {
}
