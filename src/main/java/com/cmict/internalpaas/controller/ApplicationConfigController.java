package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.ApplicationConfig;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.ApplicationConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/api/applications/{applicationId}/config")
@RequiredArgsConstructor
@Slf4j
public class ApplicationConfigController {

    private final ApplicationConfigService configService;

    /**
     * Get configuration editor page
     */
    @GetMapping("/editor")
    public String getConfigurationEditor(@PathVariable Long applicationId, Model model) {
        model.addAttribute("applicationId", applicationId);
        
        // Get active configuration
        configService.getActiveConfiguration(applicationId)
                .ifPresent(config -> model.addAttribute("activeConfig", config));
        
        // Get configuration templates
        model.addAttribute("templates", configService.getTemplateConfigurations());
        
        // Get predefined templates
        model.addAttribute("predefinedTemplates", configService.getPredefinedTemplates());
        
        // Get configuration history
        model.addAttribute("configHistory", configService.getConfigurationHistory(applicationId, 10));
        
        return "admin/config-editor";
    }

    /**
     * Get active configuration
     */
    @GetMapping("/active")
    @ResponseBody
    public ResponseEntity<ApplicationConfig> getActiveConfiguration(@PathVariable Long applicationId) {
        return configService.getActiveConfiguration(applicationId)
                .map(config -> ResponseEntity.ok(config))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all configurations for application
     */
    @GetMapping
    @ResponseBody
    public ResponseEntity<List<ApplicationConfig>> getConfigurations(@PathVariable Long applicationId) {
        List<ApplicationConfig> configs = configService.getApplicationConfigurations(applicationId);
        return ResponseEntity.ok(configs);
    }

    /**
     * Get configuration history
     */
    @GetMapping("/history")
    @ResponseBody
    public ResponseEntity<List<ApplicationConfig>> getConfigurationHistory(
            @PathVariable Long applicationId,
            @RequestParam(defaultValue = "20") int limit) {
        List<ApplicationConfig> history = configService.getConfigurationHistory(applicationId, limit);
        return ResponseEntity.ok(history);
    }

    /**
     * Create new configuration
     */
    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createConfiguration(
            @PathVariable Long applicationId,
            @RequestBody ApplicationConfig config,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ApplicationConfig savedConfig = configService.createConfiguration(applicationId, config, user);
            
            response.put("success", true);
            response.put("message", "Configuration created successfully");
            response.put("config", savedConfig);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create configuration for application: " + applicationId, e);
            
            response.put("success", false);
            response.put("message", "Failed to create configuration: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Apply configuration
     */
    @PostMapping("/{configId}/apply")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyConfiguration(
            @PathVariable Long applicationId,
            @PathVariable Long configId,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ApplicationConfig appliedConfig = configService.applyConfiguration(configId, user);
            
            response.put("success", true);
            response.put("message", "Configuration applied successfully");
            response.put("config", appliedConfig);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to apply configuration: " + configId, e);
            
            response.put("success", false);
            response.put("message", "Failed to apply configuration: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Validate configuration
     */
    @PostMapping("/validate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateConfiguration(@RequestBody ApplicationConfig config) {
        ApplicationConfigService.ValidationResult result = configService.validateConfiguration(config);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.isValid());
        response.put("errors", result.getErrors());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Create backup configuration
     */
    @PostMapping("/backup")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createBackup(
            @PathVariable Long applicationId,
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ApplicationConfig backup = configService.createBackup(applicationId, description, user);
            
            response.put("success", true);
            response.put("message", "Backup created successfully");
            response.put("backup", backup);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create backup for application: " + applicationId, e);
            
            response.put("success", false);
            response.put("message", "Failed to create backup: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get configuration templates
     */
    @GetMapping("/templates")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTemplates() {
        Map<String, Object> response = new HashMap<>();
        response.put("templates", configService.getTemplateConfigurations());
        response.put("predefined", configService.getPredefinedTemplates());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Create configuration template
     */
    @PostMapping("/templates")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createTemplate(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ApplicationConfig config = (ApplicationConfig) request.get("config");
            String templateName = (String) request.get("templateName");
            String description = (String) request.get("description");
            
            ApplicationConfig template = configService.createTemplate(config, templateName, description, user);
            
            response.put("success", true);
            response.put("message", "Template created successfully");
            response.put("template", template);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create template", e);
            
            response.put("success", false);
            response.put("message", "Failed to create template: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Apply template to application
     */
    @PostMapping("/templates/{templateName}/apply")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyTemplate(
            @PathVariable Long applicationId,
            @PathVariable String templateName,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ApplicationConfig appliedConfig = configService.applyTemplate(applicationId, templateName, user);
            
            response.put("success", true);
            response.put("message", "Template applied successfully");
            response.put("config", appliedConfig);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to apply template: " + templateName + " to application: " + applicationId, e);
            
            response.put("success", false);
            response.put("message", "Failed to apply template: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Hot reload configuration
     */
    @PostMapping("/{configId}/hot-reload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> hotReloadConfiguration(
            @PathVariable Long applicationId,
            @PathVariable Long configId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get configuration
            ApplicationConfig config = configService.getApplicationConfigurations(applicationId)
                    .stream()
                    .filter(c -> c.getId().equals(configId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));
            
            boolean success = configService.hotReloadConfiguration(applicationId, config);
            
            response.put("success", success);
            response.put("message", success ? "Configuration hot-reloaded successfully" : 
                    "Hot reload failed - restart required");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to hot reload configuration: " + configId, e);
            
            response.put("success", false);
            response.put("message", "Hot reload failed: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Export configuration as JSON
     */
    @GetMapping("/{configId}/export/json")
    public void exportConfigurationAsJson(
            @PathVariable Long applicationId,
            @PathVariable Long configId,
            HttpServletResponse response) throws IOException {
        
        try {
            String json = configService.exportConfigurationAsJson(configId);
            
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Content-Disposition", 
                    "attachment; filename=config-" + configId + ".json");
            response.getWriter().write(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to export configuration as JSON: " + configId, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("{\"error\": \"Export failed\"}");
        }
    }

    /**
     * Export configuration as YAML
     */
    @GetMapping("/{configId}/export/yaml")
    public void exportConfigurationAsYaml(
            @PathVariable Long applicationId,
            @PathVariable Long configId,
            HttpServletResponse response) throws IOException {
        
        try {
            String yaml = configService.exportConfigurationAsYaml(configId);
            
            response.setContentType("application/x-yaml");
            response.setHeader("Content-Disposition", 
                    "attachment; filename=config-" + configId + ".yml");
            response.getWriter().write(yaml);
        } catch (Exception e) {
            log.error("Failed to export configuration as YAML: " + configId, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("error: Export failed");
        }
    }

    /**
     * Import configuration from file
     */
    @PostMapping("/import")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> importConfiguration(
            @PathVariable Long applicationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("format") String format,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            ApplicationConfig config;
            
            if ("json".equalsIgnoreCase(format)) {
                config = configService.importConfigurationFromJson(content, applicationId, user);
            } else {
                throw new IllegalArgumentException("Unsupported format: " + format);
            }
            
            response.put("success", true);
            response.put("message", "Configuration imported successfully");
            response.put("config", config);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to import configuration for application: " + applicationId, e);
            
            response.put("success", false);
            response.put("message", "Failed to import configuration: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Compare two configurations
     */
    @GetMapping("/{configId1}/compare/{configId2}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> compareConfigurations(
            @PathVariable Long applicationId,
            @PathVariable Long configId1,
            @PathVariable Long configId2) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ApplicationConfig> configs = configService.getApplicationConfigurations(applicationId);
            
            ApplicationConfig config1 = configs.stream()
                    .filter(c -> c.getId().equals(configId1))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Configuration 1 not found"));
            
            ApplicationConfig config2 = configs.stream()
                    .filter(c -> c.getId().equals(configId2))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Configuration 2 not found"));
            
            Map<String, Object> differences = compareConfigObjects(config1, config2);
            
            response.put("success", true);
            response.put("config1", config1);
            response.put("config2", config2);
            response.put("differences", differences);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to compare configurations: " + configId1 + " vs " + configId2, e);
            
            response.put("success", false);
            response.put("message", "Failed to compare configurations: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Clean up old backup configurations
     */
    @DeleteMapping("/cleanup")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cleanupOldBackups(
            @PathVariable Long applicationId,
            @RequestParam(defaultValue = "10") int keepCount) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            configService.cleanupOldBackups(applicationId, keepCount);
            
            response.put("success", true);
            response.put("message", "Old backup configurations cleaned up successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to cleanup old backups for application: " + applicationId, e);
            
            response.put("success", false);
            response.put("message", "Failed to cleanup old backups: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Private helper methods

    private Map<String, Object> compareConfigObjects(ApplicationConfig config1, ApplicationConfig config2) {
        Map<String, Object> differences = new HashMap<>();
        
        addDifference(differences, "jvmOptions", config1.getJvmOptions(), config2.getJvmOptions());
        addDifference(differences, "gcOptions", config1.getGcOptions(), config2.getGcOptions());
        addDifference(differences, "systemProperties", config1.getSystemProperties(), config2.getSystemProperties());
        addDifference(differences, "environmentVariables", config1.getEnvironmentVariables(), config2.getEnvironmentVariables());
        addDifference(differences, "programArguments", config1.getProgramArguments(), config2.getProgramArguments());
        addDifference(differences, "springProperties", config1.getSpringProperties(), config2.getSpringProperties());
        addDifference(differences, "enableJmx", config1.getEnableJmx(), config2.getEnableJmx());
        addDifference(differences, "jmxPort", config1.getJmxPort(), config2.getJmxPort());
        addDifference(differences, "enableDebug", config1.getEnableDebug(), config2.getEnableDebug());
        addDifference(differences, "debugPort", config1.getDebugPort(), config2.getDebugPort());
        addDifference(differences, "activeProfiles", config1.getActiveProfiles(), config2.getActiveProfiles());
        addDifference(differences, "serverPort", config1.getServerPort(), config2.getServerPort());
        
        return differences;
    }

    private void addDifference(Map<String, Object> differences, String field, Object value1, Object value2) {
        if (!Objects.equals(value1, value2)) {
            Map<String, Object> diff = new HashMap<>();
            diff.put("old", value1);
            diff.put("new", value2);
            differences.put(field, diff);
        }
    }
}