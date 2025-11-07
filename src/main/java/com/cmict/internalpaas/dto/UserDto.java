package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 用户信息 DTO
 *
 * 用于React前端显示的用户信息(不包含敏感信息如密码)
 */
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String role; // 主要角色,对应前端的 UserRole 枚举
    private String displayName;
    private String avatar;
    private String createdAt; // ISO 8601 格式字符串
    private String lastLogin; // ISO 8601 格式字符串

    public UserDto() {
    }

    /**
     * 从User实体创建UserDto
     */
    public static UserDto fromUser(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());

        // 获取最高权限角色作为主要角色
        dto.setRole(getPrimaryRole(user.getRoles()));

        // displayName 默认使用 username
        dto.setDisplayName(user.getUsername());

        // 格式化时间为 ISO 8601 字符串
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        if (user.getCreatedAt() != null) {
            dto.setCreatedAt(user.getCreatedAt().format(formatter));
        }
        if (user.getLastLoginTime() != null) {
            dto.setLastLogin(user.getLastLoginTime().format(formatter));
        }

        return dto;
    }

    /**
     * 获取用户的主要角色(最高权限)
     */
    private static String getPrimaryRole(Set<User.Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return "DEVELOPER";
        }

        // 按权限优先级: SUPER_ADMIN > ADMIN > DEVELOPER
        if (roles.contains(User.Role.SUPER_ADMIN)) {
            return "SUPER_ADMIN";
        } else if (roles.contains(User.Role.ADMIN)) {
            return "ADMIN";
        } else {
            return "DEVELOPER";
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }
}
