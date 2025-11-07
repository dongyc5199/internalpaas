package com.cmict.internalpaas.dto;

/**
 * 登录响应 DTO
 *
 * 返回给React前端的登录响应,包含用户信息
 */
public class LoginResponse {
    private UserDto user;
    private String sessionId;
    private Long expiresAt;

    public LoginResponse() {
    }

    public LoginResponse(UserDto user, String sessionId, Long expiresAt) {
        this.user = user;
        this.sessionId = sessionId;
        this.expiresAt = expiresAt;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }
}
