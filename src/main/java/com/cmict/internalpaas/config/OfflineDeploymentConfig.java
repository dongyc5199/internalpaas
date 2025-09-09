package com.cmict.internalpaas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 离线部署配置
 * 支持内网环境下的资源本地化
 */
@Configuration
@ConfigurationProperties(prefix = "app.offline")
public class OfflineDeploymentConfig implements WebMvcConfigurer {
    
    /**
     * 是否启用离线模式
     */
    private boolean enabled = false;
    
    /**
     * 外部CDN资源是否可用
     */
    private boolean cdnAvailable = true;
    
    /**
     * 本地vendor资源路径
     */
    private String vendorPath = "/static/vendor/";
    
    /**
     * CDN连接测试URL
     */
    private String cdnTestUrl = "https://cdn.jsdelivr.net/";
    
    /**
     * 连接测试超时时间（毫秒）
     */
    private int connectionTimeout = 5000;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置vendor资源路径映射
        registry.addResourceHandler("/vendor/**")
                .addResourceLocations("classpath:/static/vendor/")
                .setCachePeriod(3600 * 24 * 30); // 缓存30天
    }
    
    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isCdnAvailable() {
        return cdnAvailable;
    }

    public void setCdnAvailable(boolean cdnAvailable) {
        this.cdnAvailable = cdnAvailable;
    }

    public String getVendorPath() {
        return vendorPath;
    }

    public void setVendorPath(String vendorPath) {
        this.vendorPath = vendorPath;
    }

    public String getCdnTestUrl() {
        return cdnTestUrl;
    }

    public void setCdnTestUrl(String cdnTestUrl) {
        this.cdnTestUrl = cdnTestUrl;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
}