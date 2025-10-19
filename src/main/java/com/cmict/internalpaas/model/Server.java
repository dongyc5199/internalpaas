package com.cmict.internalpaas.model;

import com.cmict.internalpaas.service.PasswordEncryptionService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "servers")
public class Server {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String hostname;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column
    private String description;
    
    @Column(nullable = false)
    private String baseWorkDirectory;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    // SSH连接配置
    @Column
    private Integer sshPort = 22;
    
    @Column
    private String sshUsername;
    
    @Column(name = "ssh_password")
    @JsonIgnore  // 防止密码在JSON序列化时暴露
    private String sshPasswordEncrypted;
    
    @Column
    private String sshKeyPath;
    
    @Column(name = "ssh_key_passphrase")
    @JsonIgnore  // 防止密钥密码在JSON序列化时暴露
    private String sshKeyPassphraseEncrypted;
    
    // 连接状态缓存
    @Column
    @Enumerated(EnumType.STRING)
    private ConnectionStatus connectionStatus = ConnectionStatus.UNKNOWN;
    
    @Column
    private LocalDateTime lastConnectionCheck;
    
    @Column
    private LocalDateTime lastMetricsUpdate;
    
    @Column
    private Boolean autoMonitorEnabled = true;
    
    @Column
    private Integer monitorIntervalSeconds = 60;
    
    // 服务器类型和权限管理相关字段
    @Enumerated(EnumType.STRING)
    @Column(name = "server_type")
    private ServerType serverType = ServerType.DEVELOPMENT;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "os_type")
    private OsType osType = OsType.LINUX;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "privilege_level")
    private PrivilegeLevel privilegeLevel = PrivilegeLevel.UNKNOWN;
    
    @Column(name = "last_privilege_check")
    private LocalDateTime lastPrivilegeCheck;
    
    @Column(name = "privilege_check_details", columnDefinition = "TEXT")
    private String privilegeCheckDetails;
    
    @Column(name = "default_user_group_id")
    private Long defaultUserGroupId;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
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

    // 连接状态枚举
    public enum ConnectionStatus {
        UNKNOWN("未知"),
        CONNECTED("连接成功"), 
        FAILED("连接失败"),
        TIMEOUT("连接超时"),
        AUTH_FAILED("认证失败"),
        MONITORING("监控中");
        
        private final String description;
        
        ConnectionStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 服务器类型枚举
    public enum ServerType {
        DEVELOPMENT("开发服务器"),
        TESTING("测试服务器"),
        STAGING("预生产服务器"),
        PRODUCTION("生产服务器");
        
        private final String description;
        
        ServerType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 操作系统类型枚举
    public enum OsType {
        LINUX("Linux"),
        WINDOWS("Windows"),
        MACOS("macOS"),
        UNIX("Unix"),
        BSD("BSD"),
        OTHER("其他");
        
        private final String description;
        
        OsType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 服务器权限级别枚举
    public enum PrivilegeLevel {
        ROOT_ACCESS("完整root权限"),
        SUDO_FULL("完整sudo权限"),
        SUDO_LIMITED("受限sudo权限"),
        USER_ONLY("仅普通用户权限"),
        NO_ACCESS("无权限"),
        UNKNOWN("未检查");
        
        private final String description;
        
        PrivilegeLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getBaseWorkDirectory() { return baseWorkDirectory; }
    public void setBaseWorkDirectory(String baseWorkDirectory) { this.baseWorkDirectory = baseWorkDirectory; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public Integer getSshPort() { return sshPort; }
    public void setSshPort(Integer sshPort) { this.sshPort = sshPort; }
    
    public String getSshUsername() { return sshUsername; }
    public void setSshUsername(String sshUsername) { this.sshUsername = sshUsername; }
    
    /**
     * 获取SSH加密密码（内部使用，已加密）
     */
    public String getSshPasswordEncrypted() { 
        return sshPasswordEncrypted; 
    }
    
    /**
     * 设置SSH加密密码（内部使用，已加密）
     */
    public void setSshPasswordEncrypted(String sshPasswordEncrypted) { 
        this.sshPasswordEncrypted = sshPasswordEncrypted; 
    }
    
    /**
     * 获取SSH明文密码（业务逻辑使用）
     * 注意：此方法需要依赖注入PasswordEncryptionService
     */
    @Transient
    @JsonIgnore  // 防止JSON序列化时调用此字段
    private PasswordEncryptionService passwordEncryptionService;
    
    @JsonIgnore  // 防止JSON序列化调用此方法
    public String getSshPassword() {
        if (passwordEncryptionService == null) {
            throw new IllegalStateException("密码加密服务未初始化，请在Service层调用setPasswordEncryptionService()");
        }
        
        if (sshPasswordEncrypted == null || sshPasswordEncrypted.isEmpty()) {
            return null;
        }
        
        try {
            return passwordEncryptionService.decryptPassword(sshPasswordEncrypted);
        } catch (Exception e) {
            throw new RuntimeException("SSH密码解密失败", e);
        }
    }
    
    /**
     * 设置SSH明文密码（业务逻辑使用）
     */
    public void setSshPassword(String plainPassword) {
        if (passwordEncryptionService == null) {
            // 如果服务未注入，直接存储（向后兼容）
            this.sshPasswordEncrypted = plainPassword;
            return;
        }
        
        try {
            this.sshPasswordEncrypted = passwordEncryptionService.encryptPassword(plainPassword);
        } catch (Exception e) {
            throw new RuntimeException("SSH密码加密失败", e);
        }
    }
    
    public String getSshKeyPath() { return sshKeyPath; }
    public void setSshKeyPath(String sshKeyPath) { this.sshKeyPath = sshKeyPath; }
    
    /**
     * 获取SSH密钥加密密码（内部使用，已加密）
     */
    public String getSshKeyPassphraseEncrypted() { 
        return sshKeyPassphraseEncrypted; 
    }
    
    /**
     * 设置SSH密钥加密密码（内部使用，已加密）
     */
    public void setSshKeyPassphraseEncrypted(String sshKeyPassphraseEncrypted) { 
        this.sshKeyPassphraseEncrypted = sshKeyPassphraseEncrypted; 
    }
    
    /**
     * 获取SSH密钥明文密码（业务逻辑使用）
     */
    @JsonIgnore  // 防止JSON序列化调用此方法
    public String getSshKeyPassphrase() {
        if (passwordEncryptionService == null) {
            throw new IllegalStateException("密码加密服务未初始化，请在Service层调用setPasswordEncryptionService()");
        }
        
        if (sshKeyPassphraseEncrypted == null || sshKeyPassphraseEncrypted.isEmpty()) {
            return null;
        }
        
        try {
            return passwordEncryptionService.decryptPassword(sshKeyPassphraseEncrypted);
        } catch (Exception e) {
            throw new RuntimeException("SSH密钥密码解密失败", e);
        }
    }
    
    /**
     * 设置SSH密钥明文密码（业务逻辑使用）
     */
    public void setSshKeyPassphrase(String plainPassphrase) {
        if (passwordEncryptionService == null) {
            this.sshKeyPassphraseEncrypted = plainPassphrase;
            return;
        }
        
        try {
            this.sshKeyPassphraseEncrypted = passwordEncryptionService.encryptPassword(plainPassphrase);
        } catch (Exception e) {
            throw new RuntimeException("SSH密钥密码加密失败", e);
        }
    }
    
    public ConnectionStatus getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(ConnectionStatus connectionStatus) { this.connectionStatus = connectionStatus; }
    
    public LocalDateTime getLastConnectionCheck() { return lastConnectionCheck; }
    public void setLastConnectionCheck(LocalDateTime lastConnectionCheck) { this.lastConnectionCheck = lastConnectionCheck; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastMetricsUpdate() { return lastMetricsUpdate; }
    public void setLastMetricsUpdate(LocalDateTime lastMetricsUpdate) { this.lastMetricsUpdate = lastMetricsUpdate; }
    
    public Boolean getAutoMonitorEnabled() { return autoMonitorEnabled; }
    public void setAutoMonitorEnabled(Boolean autoMonitorEnabled) { this.autoMonitorEnabled = autoMonitorEnabled; }
    
    public Integer getMonitorIntervalSeconds() { return monitorIntervalSeconds; }
    public void setMonitorIntervalSeconds(Integer monitorIntervalSeconds) { this.monitorIntervalSeconds = monitorIntervalSeconds; }
    
    public ServerType getServerType() { return serverType; }
    public void setServerType(ServerType serverType) { this.serverType = serverType; }
    
    public OsType getOsType() { return osType; }
    public void setOsType(OsType osType) { this.osType = osType; }
    
    public PrivilegeLevel getPrivilegeLevel() { return privilegeLevel; }
    public void setPrivilegeLevel(PrivilegeLevel privilegeLevel) { this.privilegeLevel = privilegeLevel; }
    
    public LocalDateTime getLastPrivilegeCheck() { return lastPrivilegeCheck; }
    public void setLastPrivilegeCheck(LocalDateTime lastPrivilegeCheck) { this.lastPrivilegeCheck = lastPrivilegeCheck; }
    
    public String getPrivilegeCheckDetails() { return privilegeCheckDetails; }
    public void setPrivilegeCheckDetails(String privilegeCheckDetails) { this.privilegeCheckDetails = privilegeCheckDetails; }
    
    public Long getDefaultUserGroupId() { return defaultUserGroupId; }
    public void setDefaultUserGroupId(Long defaultUserGroupId) { this.defaultUserGroupId = defaultUserGroupId; }
    
    /**
     * 设置密码加密服务（用于依赖注入）
     * 此方法应在Service层中调用，确保密码加密解密功能正常工作
     */
    public void setPasswordEncryptionService(PasswordEncryptionService passwordEncryptionService) {
        this.passwordEncryptionService = passwordEncryptionService;
    }
    
    /**
     * 检查密码是否已加密
     */
    public boolean isPasswordEncrypted() {
        return passwordEncryptionService != null && 
               passwordEncryptionService.isPasswordEncrypted(sshPasswordEncrypted);
    }
    
    /**
     * 安全地获取SSH明文密码（仅供Service层使用）
     * 返回可选值，避免抛出异常
     */
    @JsonIgnore
    public String getSshPasswordSafely() {
        if (passwordEncryptionService == null) {
            return null; // 服务未初始化，返回null
        }
        
        if (sshPasswordEncrypted == null || sshPasswordEncrypted.isEmpty()) {
            return null;
        }
        
        try {
            return passwordEncryptionService.decryptPassword(sshPasswordEncrypted);
        } catch (Exception e) {
            // 日志记录错误但不抛出异常
            return null;
        }
    }
    
    /**
     * 安全地获取SSH密钥明文密码（仅供Service层使用）
     */
    @JsonIgnore
    public String getSshKeyPassphraseSafely() {
        if (passwordEncryptionService == null) {
            return null;
        }
        
        if (sshKeyPassphraseEncrypted == null || sshKeyPassphraseEncrypted.isEmpty()) {
            return null;
        }
        
        try {
            return passwordEncryptionService.decryptPassword(sshKeyPassphraseEncrypted);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取用于JSON序列化的脱敏密码信息
     */
    public String getPasswordMasked() {
        return (sshPasswordEncrypted != null && !sshPasswordEncrypted.isEmpty()) ? "***已设置***" : null;
    }
    
    /**
     * 获取用于JSON序列化的脱敏密钥密码信息
     */
    public String getKeyPassphraseMasked() {
        return (sshKeyPassphraseEncrypted != null && !sshKeyPassphraseEncrypted.isEmpty()) ? "***已设置***" : null;
    }
}