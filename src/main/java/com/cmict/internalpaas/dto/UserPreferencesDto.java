package com.cmict.internalpaas.dto;

/**
 * 用户偏好设置DTO
 */
public class UserPreferencesDto {
    
    // 主题设置
    private String theme = "light"; // light, dark, auto
    
    // 通知设置
    private Boolean emailNotifications = true;
    private Boolean systemNotifications = true;
    private Boolean applicationStatusNotifications = true;
    private Boolean securityNotifications = true;
    
    // 仪表板设置
    private String dashboardLayout = "default"; // default, compact, detailed
    private Boolean showWelcomeMessage = true;
    private Boolean showQuickActions = true;
    private Boolean showRecentActivity = true;
    
    // 终端设置
    private String terminalTheme = "dark"; // dark, light
    private Integer terminalFontSize = 14;
    private String terminalFontFamily = "Monaco";

    // AI 聊天流式输出设置
    private Boolean aiStreamingEnabled = true; // 默认启用打字机效果
    private String aiStreamingSpeed = "normal"; // slow, normal, fast

    // 语言设置
    private String language = "zh_CN"; // zh_CN, en_US
    private String timeZone = "Asia/Shanghai";
    
    // 构造函数
    public UserPreferencesDto() {}
    
    // Getters and Setters
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

    public Boolean getAiStreamingEnabled() { return aiStreamingEnabled; }
    public void setAiStreamingEnabled(Boolean aiStreamingEnabled) { this.aiStreamingEnabled = aiStreamingEnabled; }

    public String getAiStreamingSpeed() { return aiStreamingSpeed; }
    public void setAiStreamingSpeed(String aiStreamingSpeed) {
        // 验证速度值，仅接受 slow, normal, fast
        if ("slow".equals(aiStreamingSpeed) ||
            "normal".equals(aiStreamingSpeed) ||
            "fast".equals(aiStreamingSpeed)) {
            this.aiStreamingSpeed = aiStreamingSpeed;
        } else {
            // 无效值回退到默认值
            this.aiStreamingSpeed = "normal";
        }
    }
}