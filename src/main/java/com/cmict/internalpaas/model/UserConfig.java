package com.cmict.internalpaas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_configs")
public class UserConfig {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = {})
    @MapsId // 将此实体的主键与User实体的主键关联
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String workDirectory;

    // 主题设置
    @Column(name = "theme", nullable = false)
    private String theme = "light"; // light, dark, auto

    // 通知设置
    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;

    @Column(name = "system_notifications")
    private Boolean systemNotifications = true;

    @Column(name = "application_status_notifications")
    private Boolean applicationStatusNotifications = true;

    @Column(name = "security_notifications")
    private Boolean securityNotifications = true;

    // 仪表板设置
    @Column(name = "dashboard_layout")
    private String dashboardLayout = "default"; // default, compact, detailed

    @Column(name = "show_welcome_message")
    private Boolean showWelcomeMessage = true;

    @Column(name = "show_quick_actions")
    private Boolean showQuickActions = true;

    @Column(name = "show_recent_activity")
    private Boolean showRecentActivity = true;

    // 终端设置
    @Column(name = "terminal_theme")
    private String terminalTheme = "dark"; // dark, light

    @Column(name = "terminal_font_size")
    private Integer terminalFontSize = 14;

    @Column(name = "terminal_font_family")
    private String terminalFontFamily = "Monaco";

    // 语言设置
    @Column(name = "language")
    private String language = "zh_CN"; // zh_CN, en_US

    @Column(name = "time_zone")
    private String timeZone = "Asia/Shanghai";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public UserConfig() {}

    public UserConfig(User user, String workDirectory) {
        this.user = user;
        this.workDirectory = workDirectory;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getWorkDirectory() { return workDirectory; }
    public void setWorkDirectory(String workDirectory) { this.workDirectory = workDirectory; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public Boolean getEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }

    public Boolean getSystemNotifications() { return systemNotifications; }
    public void setSystemNotifications(Boolean systemNotifications) { this.systemNotifications = systemNotifications; }

    public Boolean getApplicationStatusNotifications() { return applicationStatusNotifications; }
    public void setApplicationStatusNotifications(Boolean applicationStatusNotifications) { this.applicationStatusNotifications = applicationStatusNotifications; }

    public Boolean getSecurityNotifications() { return securityNotifications; }
    public void setSecurityNotifications(Boolean securityNotifications) { this.securityNotifications = securityNotifications; }

    public String getDashboardLayout() { return dashboardLayout; }
    public void setDashboardLayout(String dashboardLayout) { this.dashboardLayout = dashboardLayout; }

    public Boolean getShowWelcomeMessage() { return showWelcomeMessage; }
    public void setShowWelcomeMessage(Boolean showWelcomeMessage) { this.showWelcomeMessage = showWelcomeMessage; }

    public Boolean getShowQuickActions() { return showQuickActions; }
    public void setShowQuickActions(Boolean showQuickActions) { this.showQuickActions = showQuickActions; }

    public Boolean getShowRecentActivity() { return showRecentActivity; }
    public void setShowRecentActivity(Boolean showRecentActivity) { this.showRecentActivity = showRecentActivity; }

    public String getTerminalTheme() { return terminalTheme; }
    public void setTerminalTheme(String terminalTheme) { this.terminalTheme = terminalTheme; }

    public Integer getTerminalFontSize() { return terminalFontSize; }
    public void setTerminalFontSize(Integer terminalFontSize) { this.terminalFontSize = terminalFontSize; }

    public String getTerminalFontFamily() { return terminalFontFamily; }
    public void setTerminalFontFamily(String terminalFontFamily) { this.terminalFontFamily = terminalFontFamily; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

