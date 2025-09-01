package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.ApplicationConfig;
import com.cmict.internalpaas.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationConfigRepository extends JpaRepository<ApplicationConfig, Long> {
    
    /**
     * Find the active configuration for an application
     */
    Optional<ApplicationConfig> findByApplicationAndIsActiveTrue(Application application);
    
    /**
     * Find all configurations for an application ordered by creation date
     */
    List<ApplicationConfig> findByApplicationOrderByCreatedAtDesc(Application application);
    
    /**
     * Find all template configurations
     */
    List<ApplicationConfig> findByIsTemplateTrueOrderByTemplateNameAsc();
    
    /**
     * Find template configuration by name
     */
    Optional<ApplicationConfig> findByTemplateNameAndIsTemplateTrue(String templateName);
    
    /**
     * Find configurations by type (ACTIVE, BACKUP, TEMPLATE)
     */
    List<ApplicationConfig> findByConfigTypeOrderByCreatedAtDesc(String configType);
    
    /**
     * Find configurations for an application by type
     */
    List<ApplicationConfig> findByApplicationAndConfigTypeOrderByCreatedAtDesc(
            Application application, String configType);
    
    /**
     * Find configurations created within a date range
     */
    List<ApplicationConfig> findByApplicationAndCreatedAtBetweenOrderByCreatedAtDesc(
            Application application, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find the latest N configurations for an application
     */
    @Query("SELECT ac FROM ApplicationConfig ac WHERE ac.application = :application " +
           "ORDER BY ac.createdAt DESC")
    List<ApplicationConfig> findLatestConfigurations(@Param("application") Application application);
    
    /**
     * Find configurations that need validation
     */
    List<ApplicationConfig> findByIsValidFalseOrderByCreatedAtDesc();
    
    /**
     * Count configurations for an application
     */
    long countByApplication(Application application);
    
    /**
     * Count template configurations
     */
    long countByIsTemplateTrue();
    
    /**
     * Find configurations by version
     */
    List<ApplicationConfig> findByApplicationAndConfigVersionOrderByCreatedAtDesc(
            Application application, String configVersion);
    
    /**
     * Find configurations applied by a specific user
     */
    @Query("SELECT ac FROM ApplicationConfig ac WHERE ac.appliedBy.id = :userId " +
           "ORDER BY ac.appliedAt DESC")
    List<ApplicationConfig> findConfigurationsAppliedByUser(@Param("userId") Long userId);
    
    /**
     * Find configurations created by a specific user
     */
    @Query("SELECT ac FROM ApplicationConfig ac WHERE ac.createdBy.id = :userId " +
           "ORDER BY ac.createdAt DESC")
    List<ApplicationConfig> findConfigurationsCreatedByUser(@Param("userId") Long userId);
    
    /**
     * Delete old backup configurations, keeping only the latest N
     */
    @Query("SELECT ac FROM ApplicationConfig ac WHERE ac.application = :application " +
           "AND ac.configType = 'BACKUP' AND ac.isActive = false " +
           "ORDER BY ac.createdAt DESC")
    List<ApplicationConfig> findBackupConfigurations(@Param("application") Application application);
    
    /**
     * Find configurations that can be hot-reloaded
     */
    @Query("SELECT ac FROM ApplicationConfig ac WHERE ac.application = :application " +
           "AND ac.isValid = true AND ac.isActive = false " +
           "ORDER BY ac.createdAt DESC")
    List<ApplicationConfig> findHotReloadableConfigurations(@Param("application") Application application);
    
    /**
     * Check if a template name already exists
     */
    boolean existsByTemplateNameAndIsTemplateTrue(String templateName);
    
    /**
     * Find configurations with validation errors
     */
    @Query("SELECT ac FROM ApplicationConfig ac WHERE ac.validationErrors IS NOT NULL " +
           "AND ac.validationErrors != '' ORDER BY ac.createdAt DESC")
    List<ApplicationConfig> findConfigurationsWithErrors();
}