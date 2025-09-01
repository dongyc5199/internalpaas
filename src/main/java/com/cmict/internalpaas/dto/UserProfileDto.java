package com.cmict.internalpaas.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户个人资料DTO
 */
public class UserProfileDto {
    private Long id;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "请输入有效的邮箱地址")
    private String email;
    
    @Size(max = 255, message = "工作目录路径不能超过255个字符")
    private String workDirectory;
    
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginTime;
    private Boolean isFirstLogin;
    
    // 扩展用户信息字段
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String fullName;
    
    @Size(max = 20, message = "电话号码长度不能超过20个字符")
    private String phone;
    
    @Size(max = 100, message = "部门名称长度不能超过100个字符")
    private String department;
    
    // 用户统计信息
    private Integer totalApplications;
    private Integer activeApplications;
    private Integer totalSessions;
    private Integer totalCommands;
    private LocalDateTime lastActivityTime;
    
    // 构造函数
    public UserProfileDto() {}
    
    public UserProfileDto(String username, String email, String workDirectory) {
        this.username = username;
        this.email = email;
        this.workDirectory = workDirectory;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getWorkDirectory() { return workDirectory; }
    public void setWorkDirectory(String workDirectory) { this.workDirectory = workDirectory; }
    
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    
    public Boolean getIsFirstLogin() { return isFirstLogin; }
    public void setIsFirstLogin(Boolean isFirstLogin) { this.isFirstLogin = isFirstLogin; }
    
    public Integer getTotalApplications() { return totalApplications; }
    public void setTotalApplications(Integer totalApplications) { this.totalApplications = totalApplications; }
    
    public Integer getActiveApplications() { return activeApplications; }
    public void setActiveApplications(Integer activeApplications) { this.activeApplications = activeApplications; }
    
    public Integer getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }
    
    public Integer getTotalCommands() { return totalCommands; }
    public void setTotalCommands(Integer totalCommands) { this.totalCommands = totalCommands; }
    
    public LocalDateTime getLastActivityTime() { return lastActivityTime; }
    public void setLastActivityTime(LocalDateTime lastActivityTime) { this.lastActivityTime = lastActivityTime; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}