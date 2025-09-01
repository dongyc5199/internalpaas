package com.cmict.internalpaas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_configs")
@Data
@EqualsAndHashCode(exclude = {"application"})
public class ApplicationConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    @JsonIgnore
    private Application application;
    
    @Column(nullable = false, length = 50)
    private String configType; // ACTIVE, BACKUP, TEMPLATE
    
    @Column(length = 100)
    private String templateName; // Template name for predefined configurations
    
    @Column(length = 500)
    private String description; // Configuration description
    
    // JVM Configuration
    @Column(columnDefinition = "TEXT")
    private String jvmOptions; // JVM heap and other options
    
    @Column(columnDefinition = "TEXT")
    private String gcOptions; // Garbage collection options
    
    @Column(columnDefinition = "TEXT")
    private String systemProperties; // System properties (-D options)
    
    // Environment Configuration
    @Column(columnDefinition = "TEXT")
    private String environmentVariables; // Environment variables in JSON format
    
    // Application Configuration
    @Column(columnDefinition = "TEXT")
    private String programArguments; // Command line arguments
    
    @Column(columnDefinition = "TEXT")
    private String springProperties; // Spring Boot properties in YAML/Properties format
    
    // JMX Configuration
    private Boolean enableJmx = false;
    private Integer jmxPort;
    private String jmxAuthUser;
    
    // Debug Configuration
    private Boolean enableDebug = false;
    private Integer debugPort;
    private String debugOptions; // Remote debug options
    
    // Logging Configuration
    @Column(columnDefinition = "TEXT")
    private String loggingConfig; // Logging configuration
    
    // Profile Configuration
    private String activeProfiles; // Spring active profiles
    
    // Server Configuration
    private Integer serverPort;
    private String serverContextPath;
    
    // Health Check Configuration
    private String healthCheckPath;
    private Integer healthCheckTimeout;
    
    // Performance Configuration
    private Integer maxThreads;
    private Integer connectionTimeout;
    
    // Configuration Metadata
    private String configVersion; // Configuration version for tracking changes
    private Boolean isActive = false; // Is this the active configuration
    private Boolean isTemplate = false; // Is this a template configuration
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime appliedAt; // When this config was applied
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // User who created this configuration
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_by")
    private User appliedBy; // User who applied this configuration
    
    // Configuration validation status
    private Boolean isValid = true;
    
    @Column(columnDefinition = "TEXT")
    private String validationErrors; // Validation error messages
    
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.configVersion == null) {
            this.configVersion = "1.0.0";
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        // Auto-increment version for tracking changes
        if (this.configVersion != null) {
            String[] parts = this.configVersion.split("\\.");
            if (parts.length == 3) {
                int patch = Integer.parseInt(parts[2]) + 1;
                this.configVersion = parts[0] + "." + parts[1] + "." + patch;
            }
        }
    }
    
    // Helper methods for configuration management
    public boolean isActiveConfig() {
        return Boolean.TRUE.equals(this.isActive);
    }
    
    public boolean isTemplateConfig() {
        return Boolean.TRUE.equals(this.isTemplate);
    }
    
    public void markAsActive() {
        this.isActive = true;
        this.appliedAt = LocalDateTime.now();
    }
    
    public void markAsInactive() {
        this.isActive = false;
    }
}