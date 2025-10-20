package com.cmict.internalpaas.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class AiChatRequest {

    private String chatId;
    private String sessionId;

    private String model;

    @NotBlank
    private String message;

    private String terminalTail;

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTerminalTail() {
        return terminalTail;
    }

    public void setTerminalTail(String terminalTail) {
        this.terminalTail = terminalTail;
    }
}
