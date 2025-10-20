package com.cmict.internalpaas.ai.model;

public record ChatMessage(Role role, String content) {

    public enum Role {
        system,
        user,
        assistant
    }
}
