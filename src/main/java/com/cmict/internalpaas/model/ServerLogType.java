package com.cmict.internalpaas.model;

/**
 * 服务器日志类型枚举
 * 定义系统中支持的各种日志类型和默认路径
 */
public enum ServerLogType {
    
    // 系统日志
    SYSTEM("/var/log/syslog", "系统日志", "系统总体运行日志，包含所有系统消息"),
    MESSAGES("/var/log/messages", "系统消息", "通用系统消息日志"),
    DMESG("/var/log/dmesg", "内核消息", "内核启动和硬件检测消息"),
    
    // 安全和认证日志
    AUTH("/var/log/auth.log", "认证日志", "用户认证、登录、sudo等安全相关日志"),
    SECURE("/var/log/secure", "安全日志", "安全相关事件日志（RedHat系）"),
    AUDIT("/var/log/audit/audit.log", "审计日志", "系统审计日志，详细记录系统调用"),
    
    // 内核和硬件日志
    KERN("/var/log/kern.log", "内核日志", "Linux内核消息和驱动程序日志"),
    HARDWARE("/var/log/hardware.log", "硬件日志", "硬件相关的错误和警告"),
    
    // 网络和邮件服务
    MAIL("/var/log/mail.log", "邮件日志", "邮件服务器日志（postfix、sendmail等）"),
    MAILLOG("/var/log/maillog", "邮件日志", "邮件系统日志（RedHat系）"),
    
    // Web服务器日志
    NGINX_ACCESS("/var/log/nginx/access.log", "Nginx访问日志", "Nginx Web服务器访问记录"),
    NGINX_ERROR("/var/log/nginx/error.log", "Nginx错误日志", "Nginx Web服务器错误日志"),
    APACHE_ACCESS("/var/log/apache2/access.log", "Apache访问日志", "Apache Web服务器访问记录"),
    APACHE_ERROR("/var/log/apache2/error.log", "Apache错误日志", "Apache Web服务器错误日志"),
    
    // 数据库日志
    MYSQL_ERROR("/var/log/mysql/error.log", "MySQL错误日志", "MySQL数据库错误日志"),
    MYSQL_SLOW("/var/log/mysql/mysql-slow.log", "MySQL慢查询", "MySQL慢查询日志"),
    POSTGRESQL("/var/log/postgresql/postgresql.log", "PostgreSQL日志", "PostgreSQL数据库日志"),
    
    // 系统服务日志
    CRON("/var/log/cron.log", "定时任务日志", "系统定时任务执行日志"),
    DAEMON("/var/log/daemon.log", "守护进程日志", "各种系统守护进程的日志"),
    USER("/var/log/user.log", "用户程序日志", "用户空间程序的日志消息"),
    
    // 应用程序日志
    APPLICATION("/var/log/application.log", "应用程序日志", "通用应用程序日志"),
    DEBUG("/var/log/debug.log", "调试日志", "详细的调试信息日志"),
    
    // 容器和虚拟化
    DOCKER("/var/log/docker.log", "Docker日志", "Docker容器运行时日志"),
    LIBVIRT("/var/log/libvirt/libvirtd.log", "虚拟化日志", "libvirt虚拟化管理日志"),
    
    // 自定义日志
    CUSTOM("", "自定义日志", "用户自定义的日志文件路径");
    
    private final String defaultPath;
    private final String displayName;
    private final String description;
    
    ServerLogType(String defaultPath, String displayName, String description) {
        this.defaultPath = defaultPath;
        this.displayName = displayName;
        this.description = description;
    }
    
    // Getters
    public String getDefaultPath() { return defaultPath; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    
    /**
     * 根据文件路径推断日志类型
     */
    public static ServerLogType inferFromPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return CUSTOM;
        }
        
        String lowerPath = path.toLowerCase();
        
        // 精确匹配
        for (ServerLogType type : values()) {
            if (type != CUSTOM && lowerPath.equals(type.defaultPath.toLowerCase())) {
                return type;
            }
        }
        
        // 模糊匹配
        if (lowerPath.contains("syslog")) return SYSTEM;
        if (lowerPath.contains("auth")) return AUTH;
        if (lowerPath.contains("secure")) return SECURE;
        if (lowerPath.contains("kern")) return KERN;
        if (lowerPath.contains("mail")) return MAIL;
        if (lowerPath.contains("nginx") && lowerPath.contains("access")) return NGINX_ACCESS;
        if (lowerPath.contains("nginx") && lowerPath.contains("error")) return NGINX_ERROR;
        if (lowerPath.contains("apache") && lowerPath.contains("access")) return APACHE_ACCESS;
        if (lowerPath.contains("apache") && lowerPath.contains("error")) return APACHE_ERROR;
        if (lowerPath.contains("mysql")) return MYSQL_ERROR;
        if (lowerPath.contains("cron")) return CRON;
        if (lowerPath.contains("daemon")) return DAEMON;
        if (lowerPath.contains("docker")) return DOCKER;
        
        return CUSTOM;
    }
    
    /**
     * 获取日志类型的分类
     */
    public String getCategory() {
        switch (this) {
            case SYSTEM:
            case MESSAGES:
            case DMESG:
                return "系统日志";
                
            case AUTH:
            case SECURE:
            case AUDIT:
                return "安全日志";
                
            case KERN:
            case HARDWARE:
                return "内核日志";
                
            case NGINX_ACCESS:
            case NGINX_ERROR:
            case APACHE_ACCESS:
            case APACHE_ERROR:
                return "Web服务";
                
            case MYSQL_ERROR:
            case MYSQL_SLOW:
            case POSTGRESQL:
                return "数据库";
                
            case MAIL:
            case MAILLOG:
                return "邮件服务";
                
            case CRON:
            case DAEMON:
            case USER:
                return "系统服务";
                
            case APPLICATION:
            case DEBUG:
                return "应用程序";
                
            case DOCKER:
            case LIBVIRT:
                return "容器化";
                
            default:
                return "自定义";
        }
    }
    
    /**
     * 判断是否为系统关键日志
     */
    public boolean isCriticalLog() {
        return this == SYSTEM || this == AUTH || this == SECURE || 
               this == KERN || this == AUDIT;
    }
    
    /**
     * 获取推荐的监控优先级
     */
    public int getMonitoringPriority() {
        if (isCriticalLog()) return 1; // 高优先级
        if (getCategory().equals("Web服务") || getCategory().equals("数据库")) return 2; // 中优先级
        return 3; // 低优先级
    }
}