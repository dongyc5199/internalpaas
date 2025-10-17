package com.cmict.internalpaas.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "applications")
@Data
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name; // 应用名称

    @Column(nullable = false)
    private String jarFileName; // JAR文件名

    @Column(nullable = false)
    private String jarFilePath; // JAR文件完整路径

    private Integer port; // 应用端口
    
    private Integer debugPort; // 调试端口
    
    private String status = "STOPPED"; // 运行状态：RUNNING, STOPPED, ERROR
    
    private String processId; // 进程ID
    
    @Column(columnDefinition = "TEXT")
    private String logFilePath; // 日志文件路径
    
    // JVM参数配置
    @Column(columnDefinition = "TEXT")
    private String jvmOptions; // JVM参数，如 -Xmx512m -Xms256m
    
    @Column(columnDefinition = "TEXT")
    private String gcOptions; // GC参数，如 -XX:+UseG1GC
    
    @Column(columnDefinition = "TEXT")
    private String environmentVariables; // 环境变量，JSON格式存储
    
    private String javaVersion; // Java版本要求
    
    private String mainClass; // 主类名（如果不使用jar方式）
    
    private String programArguments; // 程序参数
    
    private Boolean enableJmx = false; // 是否启用JMX监控
    
    private Integer jmxPort; // JMX端口
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    private LocalDateTime lastStartedAt; // 上次启动时间
    
    // Configuration management relationships
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ApplicationConfig> configurations; // All configuration versions for this application
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_config_id")
    private ApplicationConfig activeConfiguration; // Currently active configuration
    
    // Configuration management helper methods
    public ApplicationConfig getActiveConfig() {
        return this.activeConfiguration;
    }
    
    public void setActiveConfig(ApplicationConfig config) {
        this.activeConfiguration = config;
        if (config != null) {
            config.setApplication(this);
            config.markAsActive();
        }
    }
    
    public boolean hasActiveConfiguration() {
        return this.activeConfiguration != null;
    }
    
    public int getConfigurationCount() {
        return this.configurations != null ? this.configurations.size() : 0;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}