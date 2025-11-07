package com.cmict.internalpaas.dto;

/**
 * 登录请求 DTO
 *
 * 用于React前端的JSON API登录请求
 */
public class LoginRequest {
    private String username;
    private String password;
    private Boolean rememberMe;

    public LoginRequest() {
    }

    public LoginRequest(String username, String password, Boolean rememberMe) {
        this.username = username;
        this.password = password;
        this.rememberMe = rememberMe;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}
