package com.waveterm.demo.chat.model;

public record ChatMessage(Role role, String content) {

    public enum Role {
        system,
        user,
        assistant
    }
}
