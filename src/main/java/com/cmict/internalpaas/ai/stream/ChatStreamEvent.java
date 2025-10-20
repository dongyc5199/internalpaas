package com.cmict.internalpaas.ai.stream;

public record ChatStreamEvent(ChatEventType type, String data) {
}
