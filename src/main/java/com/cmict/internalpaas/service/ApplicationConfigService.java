package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.model.ApplicationConfig;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.repository.ApplicationConfigRepository;
import com.cmict.internalpaas.repository.ApplicationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationConfigService {

    private final ApplicationConfigRepository configRepository;
    private final ApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper;
    private final Yaml yaml = new Yaml();

    // JVM option patterns for validation
    private static final Pattern JVM_HEAP_PATTERN = Pattern.compile("^-X(m[sx])\\d+[kmgKMG]?$");
    private static final Pattern JVM_GC_PATTERN = Pattern.compile("^-XX:[+\\-]?\\w+.*$");
    private static final Pattern SYSTEM_PROPERTY_PATTERN = Pattern.compile("^-D[\\w\\.]+=[^\\s]*$");
    
    // Port validation range
    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;

    /**
     * Get active configuration for an application
     */
    public Optional<ApplicationConfig> getActiveConfiguration(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .flatMap(configRepository::findByApplicationAndIsActiveTrue);
    }

    /**
     * Get all configurations for an application
     */
    public List<ApplicationConfig> getApplicationConfigurations(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .map(configRepository::findByApplicationOrderByCreatedAtDesc)
                .orElse(Collections.emptyList());
    }

    /**
     * Get configuration history with pagination
     */
    public List<ApplicationConfig> getConfigurationHistory(Long applicationId, int limit) {
        List<ApplicationConfig> configs = getApplicationConfigurations(applicationId);
        return configs.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Create new configuration
     */
    @Transactional
    public ApplicationConfig createConfiguration(Long applicationId, ApplicationConfig config, User user) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        config.setApplication(application);
        config.setCreatedBy(user);
        config.setConfigType("BACKUP");
        
        // Validate configuration
        ValidationResult validation = validateConfiguration(config);
        config.setIsValid(validation.isValid());
        config.setValidationErrors(validation.getErrorsAsString());

        return configRepository.save(config);
    }

    /**
     * Apply configuration to application
     */
    @Transactional
    public ApplicationConfig applyConfiguration(Long configId, User user) {
        ApplicationConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + configId));

        // Validate before applying
        ValidationResult validation = validateConfiguration(config);
        if (!validation.isValid()) {
            throw new IllegalStateException("Cannot apply invalid configuration: " + 
                    validation.getErrorsAsString());
        }

        Application application = config.getApplication();

        // Deactivate current active configuration
        configRepository.findByApplicationAndIsActiveTrue(application)
                .ifPresent(activeConfig -> {
                    activeConfig.markAsInactive();
                    configRepository.save(activeConfig);
                });

        // Apply new configuration
        config.markAsActive();
        config.setAppliedBy(user);
        config.setConfigType("ACTIVE");

        // Update application entity with new configuration
        updateApplicationFromConfig(application, config);

        return configRepository.save(config);
    }

    /**
     * Create backup of current configuration before making changes
     */
    @Transactional
    public ApplicationConfig createBackup(Long applicationId, String description, User user) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        Optional<ApplicationConfig> activeConfig = configRepository.findByApplicationAndIsActiveTrue(application);
        
        if (activeConfig.isEmpty()) {
            // Create backup from application entity
            ApplicationConfig backup = createConfigFromApplication(application);
            backup.setDescription(description != null ? description : "Auto-backup from application settings");
            backup.setCreatedBy(user);
            backup.setConfigType("BACKUP");
            return configRepository.save(backup);
        } else {
            // Clone active configuration as backup
            ApplicationConfig backup = cloneConfiguration(activeConfig.get());
            backup.setDescription(description != null ? description : "Backup before configuration change");
            backup.setCreatedBy(user);
            backup.setConfigType("BACKUP");
            backup.setIsActive(false);
            backup.setAppliedAt(null);
            backup.setAppliedBy(null);
            return configRepository.save(backup);
        }
    }

    /**
     * Get all template configurations
     */
    public List<ApplicationConfig> getTemplateConfigurations() {
        return configRepository.findByIsTemplateTrueOrderByTemplateNameAsc();
    }

    /**
     * Create configuration template
     */
    @Transactional
    public ApplicationConfig createTemplate(ApplicationConfig config, String templateName, 
                                          String description, User user) {
        // Check if template name already exists
        if (configRepository.existsByTemplateNameAndIsTemplateTrue(templateName)) {
            throw new IllegalArgumentException("Template name already exists: " + templateName);
        }

        // Validate configuration
        ValidationResult validation = validateConfiguration(config);
        config.setIsValid(validation.isValid());
        config.setValidationErrors(validation.getErrorsAsString());

        config.setTemplateName(templateName);
        config.setDescription(description);
        config.setIsTemplate(true);
        config.setConfigType("TEMPLATE");
        config.setCreatedBy(user);
        config.setApplication(null); // Templates are not tied to specific applications

        return configRepository.save(config);
    }

    /**
     * Apply template to application
     */
    @Transactional
    public ApplicationConfig applyTemplate(Long applicationId, String templateName, User user) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        ApplicationConfig template = configRepository.findByTemplateNameAndIsTemplateTrue(templateName)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateName));

        // Clone template for application
        ApplicationConfig newConfig = cloneConfiguration(template);
        newConfig.setApplication(application);
        newConfig.setIsTemplate(false);
        newConfig.setTemplateName(null);
        newConfig.setConfigType("ACTIVE");
        newConfig.setCreatedBy(user);
        newConfig.setAppliedBy(user);

        // Deactivate current configuration
        configRepository.findByApplicationAndIsActiveTrue(application)
                .ifPresent(activeConfig -> {
                    activeConfig.markAsInactive();
                    configRepository.save(activeConfig);
                });

        newConfig.markAsActive();
        updateApplicationFromConfig(application, newConfig);

        return configRepository.save(newConfig);
    }

    /**
     * Get predefined configuration templates
     */
    public Map<String, ApplicationConfig> getPredefinedTemplates() {
        Map<String, ApplicationConfig> templates = new HashMap<>();
        
        // Development template
        ApplicationConfig devTemplate = new ApplicationConfig();
        devTemplate.setTemplateName("Development");
        devTemplate.setDescription("Development environment configuration");
        devTemplate.setJvmOptions("-Xms256m -Xmx512m");
        devTemplate.setGcOptions("-XX:+UseG1GC");
        devTemplate.setSystemProperties("-Dspring.profiles.active=dev -Dlogging.level.root=DEBUG");
        devTemplate.setSpringProperties("server.port=8080\nmanagement.endpoints.web.exposure.include=*");
        devTemplate.setEnableDebug(true);
        devTemplate.setEnableJmx(true);
        devTemplate.setActiveProfiles("dev");
        templates.put("Development", devTemplate);

        // Production template
        ApplicationConfig prodTemplate = new ApplicationConfig();
        prodTemplate.setTemplateName("Production");
        prodTemplate.setDescription("Production environment configuration");
        prodTemplate.setJvmOptions("-Xms1g -Xmx2g");
        prodTemplate.setGcOptions("-XX:+UseG1GC -XX:MaxGCPauseMillis=200");
        prodTemplate.setSystemProperties("-Dspring.profiles.active=prod -Dfile.encoding=UTF-8");
        prodTemplate.setSpringProperties("server.port=8080\nlogging.level.root=WARN\nlogging.level.com.cmict=INFO");
        prodTemplate.setEnableDebug(false);
        prodTemplate.setEnableJmx(true);
        prodTemplate.setActiveProfiles("prod");
        templates.put("Production", prodTemplate);

        // Testing template
        ApplicationConfig testTemplate = new ApplicationConfig();
        testTemplate.setTemplateName("Testing");
        testTemplate.setDescription("Testing environment configuration");
        testTemplate.setJvmOptions("-Xms512m -Xmx1g");
        testTemplate.setGcOptions("-XX:+UseG1GC");
        testTemplate.setSystemProperties("-Dspring.profiles.active=test -Dspring.jpa.show-sql=true");
        testTemplate.setSpringProperties("server.port=8080\nlogging.level.org.springframework.web=DEBUG");
        testTemplate.setEnableDebug(true);
        testTemplate.setEnableJmx(true);
        testTemplate.setActiveProfiles("test");
        templates.put("Testing", testTemplate);

        return templates;
    }

    /**
     * Validate configuration
     */
    public ValidationResult validateConfiguration(ApplicationConfig config) {
        ValidationResult result = new ValidationResult();

        // Validate JVM options
        if (config.getJvmOptions() != null && !config.getJvmOptions().trim().isEmpty()) {
            validateJvmOptions(config.getJvmOptions(), result);
        }

        // Validate GC options
        if (config.getGcOptions() != null && !config.getGcOptions().trim().isEmpty()) {
            validateGcOptions(config.getGcOptions(), result);
        }

        // Validate system properties
        if (config.getSystemProperties() != null && !config.getSystemProperties().trim().isEmpty()) {
            validateSystemProperties(config.getSystemProperties(), result);
        }

        // Validate environment variables
        if (config.getEnvironmentVariables() != null && !config.getEnvironmentVariables().trim().isEmpty()) {
            validateEnvironmentVariables(config.getEnvironmentVariables(), result);
        }

        // Validate Spring properties
        if (config.getSpringProperties() != null && !config.getSpringProperties().trim().isEmpty()) {
            validateSpringProperties(config.getSpringProperties(), result);
        }

        // Validate ports
        validatePorts(config, result);

        return result;
    }

    /**
     * Hot reload configuration (for supported properties)
     */
    public boolean hotReloadConfiguration(Long applicationId, ApplicationConfig config) {
        try {
            // This would integrate with Spring Boot Actuator's refresh endpoint
            // or use Spring Cloud Config for hot reloading
            log.info("Hot reloading configuration for application: {}", applicationId);
            
            // Example: Update logging levels
            if (config.getSpringProperties() != null) {
                updateLoggingLevels(config.getSpringProperties());
            }
            
            return true;
        } catch (Exception e) {
            log.error("Failed to hot reload configuration for application: " + applicationId, e);
            return false;
        }
    }

    /**
     * Export configuration as JSON
     */
    public String exportConfigurationAsJson(Long configId) throws JsonProcessingException {
        ApplicationConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + configId));
        
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
    }

    /**
     * Export configuration as YAML
     */
    public String exportConfigurationAsYaml(Long configId) {
        ApplicationConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + configId));
        
        Map<String, Object> configMap = convertConfigToMap(config);
        return yaml.dump(configMap);
    }

    /**
     * Import configuration from JSON
     */
    public ApplicationConfig importConfigurationFromJson(String json, Long applicationId, User user) 
            throws JsonProcessingException {
        ApplicationConfig config = objectMapper.readValue(json, ApplicationConfig.class);
        config.setId(null); // Clear ID for new entity
        config.setCreatedAt(LocalDateTime.now());
        
        return createConfiguration(applicationId, config, user);
    }

    /**
     * Clean up old backup configurations, keeping only the latest N
     */
    @Transactional
    public void cleanupOldBackups(Long applicationId, int keepCount) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        List<ApplicationConfig> backups = configRepository.findBackupConfigurations(application);
        
        if (backups.size() > keepCount) {
            List<ApplicationConfig> toDelete = backups.subList(keepCount, backups.size());
            configRepository.deleteAll(toDelete);
            log.info("Cleaned up {} old backup configurations for application: {}", 
                    toDelete.size(), applicationId);
        }
    }

    // Private helper methods

    private void validateJvmOptions(String jvmOptions, ValidationResult result) {
        String[] options = jvmOptions.split("\\s+");
        for (String option : options) {
            if (option.trim().isEmpty()) continue;
            
            if (option.startsWith("-Xm") && !JVM_HEAP_PATTERN.matcher(option).matches()) {
                result.addError("Invalid JVM heap option: " + option);
            }
        }
    }

    private void validateGcOptions(String gcOptions, ValidationResult result) {
        String[] options = gcOptions.split("\\s+");
        for (String option : options) {
            if (option.trim().isEmpty()) continue;
            
            if (option.startsWith("-XX:") && !JVM_GC_PATTERN.matcher(option).matches()) {
                result.addError("Invalid GC option: " + option);
            }
        }
    }

    private void validateSystemProperties(String systemProperties, ValidationResult result) {
        String[] properties = systemProperties.split("\\s+");
        for (String property : properties) {
            if (property.trim().isEmpty()) continue;
            
            if (property.startsWith("-D") && !SYSTEM_PROPERTY_PATTERN.matcher(property).matches()) {
                result.addError("Invalid system property: " + property);
            }
        }
    }

    private void validateEnvironmentVariables(String envVars, ValidationResult result) {
        try {
            Map<String, String> vars = objectMapper.readValue(envVars, 
                    new TypeReference<Map<String, String>>() {});
            
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    result.addError("Environment variable key cannot be empty");
                }
            }
        } catch (JsonProcessingException e) {
            result.addError("Invalid JSON format for environment variables: " + e.getMessage());
        }
    }

    private void validateSpringProperties(String springProperties, ValidationResult result) {
        String[] lines = springProperties.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            
            if (!line.contains("=")) {
                result.addError("Invalid Spring property format: " + line);
            }
        }
    }

    private void validatePorts(ApplicationConfig config, ValidationResult result) {
        if (config.getServerPort() != null) {
            if (config.getServerPort() < MIN_PORT || config.getServerPort() > MAX_PORT) {
                result.addError("Server port must be between " + MIN_PORT + " and " + MAX_PORT);
            }
        }
        
        if (config.getDebugPort() != null) {
            if (config.getDebugPort() < MIN_PORT || config.getDebugPort() > MAX_PORT) {
                result.addError("Debug port must be between " + MIN_PORT + " and " + MAX_PORT);
            }
        }
        
        if (config.getJmxPort() != null) {
            if (config.getJmxPort() < MIN_PORT || config.getJmxPort() > MAX_PORT) {
                result.addError("JMX port must be between " + MIN_PORT + " and " + MAX_PORT);
            }
        }
    }

    private ApplicationConfig createConfigFromApplication(Application application) {
        ApplicationConfig config = new ApplicationConfig();
        config.setApplication(application);
        config.setJvmOptions(application.getJvmOptions());
        config.setGcOptions(application.getGcOptions());
        config.setEnvironmentVariables(application.getEnvironmentVariables());
        config.setProgramArguments(application.getProgramArguments());
        config.setEnableJmx(application.getEnableJmx());
        config.setJmxPort(application.getJmxPort());
        config.setDebugPort(application.getDebugPort());
        config.setServerPort(application.getPort());
        return config;
    }

    private void updateApplicationFromConfig(Application application, ApplicationConfig config) {
        application.setJvmOptions(config.getJvmOptions());
        application.setGcOptions(config.getGcOptions());
        application.setEnvironmentVariables(config.getEnvironmentVariables());
        application.setProgramArguments(config.getProgramArguments());
        application.setEnableJmx(config.getEnableJmx());
        application.setJmxPort(config.getJmxPort());
        application.setDebugPort(config.getDebugPort());
        application.setPort(config.getServerPort());
        applicationRepository.save(application);
    }

    private ApplicationConfig cloneConfiguration(ApplicationConfig source) {
        ApplicationConfig clone = new ApplicationConfig();
        clone.setJvmOptions(source.getJvmOptions());
        clone.setGcOptions(source.getGcOptions());
        clone.setSystemProperties(source.getSystemProperties());
        clone.setEnvironmentVariables(source.getEnvironmentVariables());
        clone.setProgramArguments(source.getProgramArguments());
        clone.setSpringProperties(source.getSpringProperties());
        clone.setEnableJmx(source.getEnableJmx());
        clone.setJmxPort(source.getJmxPort());
        clone.setJmxAuthUser(source.getJmxAuthUser());
        clone.setEnableDebug(source.getEnableDebug());
        clone.setDebugPort(source.getDebugPort());
        clone.setDebugOptions(source.getDebugOptions());
        clone.setLoggingConfig(source.getLoggingConfig());
        clone.setActiveProfiles(source.getActiveProfiles());
        clone.setServerPort(source.getServerPort());
        clone.setServerContextPath(source.getServerContextPath());
        clone.setHealthCheckPath(source.getHealthCheckPath());
        clone.setHealthCheckTimeout(source.getHealthCheckTimeout());
        clone.setMaxThreads(source.getMaxThreads());
        clone.setConnectionTimeout(source.getConnectionTimeout());
        return clone;
    }

    private Map<String, Object> convertConfigToMap(ApplicationConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("jvmOptions", config.getJvmOptions());
        map.put("gcOptions", config.getGcOptions());
        map.put("systemProperties", config.getSystemProperties());
        map.put("environmentVariables", config.getEnvironmentVariables());
        map.put("programArguments", config.getProgramArguments());
        map.put("springProperties", config.getSpringProperties());
        map.put("enableJmx", config.getEnableJmx());
        map.put("jmxPort", config.getJmxPort());
        map.put("enableDebug", config.getEnableDebug());
        map.put("debugPort", config.getDebugPort());
        map.put("activeProfiles", config.getActiveProfiles());
        map.put("serverPort", config.getServerPort());
        return map;
    }

    private void updateLoggingLevels(String springProperties) {
        // Implementation for hot-reloading logging levels
        // This would use Spring Boot Actuator endpoints
        log.debug("Updating logging levels from configuration");
    }

    // Validation result helper class
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public String getErrorsAsString() {
            return String.join("; ", errors);
        }
    }
}