package com.cmict.internalpaas.service;

import com.cmict.internalpaas.config.OfflineDeploymentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 离线部署服务
 * 负责检测CDN可用性和管理资源路径
 */
@Service
public class OfflineDeploymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(OfflineDeploymentService.class);
    
    @Autowired
    private OfflineDeploymentConfig offlineConfig;
    
    private volatile boolean cdnCheckPerformed = false;
    private volatile boolean cdnAvailable = true;
    
    /**
     * 获取资源路径映射
     * 根据CDN可用性返回本地或远程路径
     */
    public Map<String, String> getResourcePaths() {
        Map<String, String> paths = new HashMap<>();
        
        boolean useCdn = shouldUseCdn();
        
        if (useCdn) {
            // 使用CDN路径
            paths.put("bootstrap-css", "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css");
            paths.put("bootstrap-js", "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js");
            paths.put("chartjs-css", "https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.min.css");
            paths.put("chartjs-js", "https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.js");
            paths.put("xterm-css", "https://cdn.jsdelivr.net/npm/xterm@5.3.0/css/xterm.css");
            paths.put("xterm-js", "https://cdn.jsdelivr.net/npm/xterm@5.3.0/lib/xterm.js");
            paths.put("prism-css", "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism.min.css");
            paths.put("prism-js", "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js");
            paths.put("flatpickr-css", "https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css");
            paths.put("flatpickr-js", "https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.js");
            paths.put("sockjs-js", "https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");
            paths.put("stomp-js", "https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js");
            paths.put("hammer-js", "https://cdn.jsdelivr.net/npm/hammerjs@2.0.8/hammer.min.js");
        } else {
            // 使用本地路径
            paths.put("bootstrap-css", "/vendor/bootstrap/bootstrap.min.css");
            paths.put("bootstrap-js", "/vendor/bootstrap/bootstrap.bundle.min.js");
            paths.put("chartjs-css", "/vendor/chartjs/chart.min.css");
            paths.put("chartjs-js", "/vendor/chartjs/chart.umd.js");
            paths.put("xterm-css", "/vendor/xterm/xterm.css");
            paths.put("xterm-js", "/vendor/xterm/xterm.js");
            paths.put("prism-css", "/vendor/prism/prism.min.css");
            paths.put("prism-js", "/vendor/prism/prism.min.js");
            paths.put("flatpickr-css", "/vendor/flatpickr/flatpickr.min.css");
            paths.put("flatpickr-js", "/vendor/flatpickr/flatpickr.min.js");
            paths.put("sockjs-js", "/vendor/sockjs/sockjs.min.js");
            paths.put("stomp-js", "/vendor/sockjs/stomp.min.js");
            paths.put("hammer-js", "/vendor/hammerjs/hammer.min.js");
        }
        
        return paths;
    }
    
    /**
     * 判断是否应该使用CDN
     */
    private boolean shouldUseCdn() {
        // 如果强制启用离线模式，直接返回false
        if (offlineConfig.isEnabled()) {
            return false;
        }
        
        // 如果还没有检查过CDN可用性，先检查
        if (!cdnCheckPerformed) {
            checkCdnAvailability();
        }
        
        return cdnAvailable;
    }
    
    /**
     * 异步检查CDN可用性
     */
    public void checkCdnAvailability() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return testConnection(offlineConfig.getCdnTestUrl());
            } catch (Exception e) {
                logger.warn("CDN连接测试失败: {}", e.getMessage());
                return false;
            }
        }).orTimeout(offlineConfig.getConnectionTimeout(), TimeUnit.MILLISECONDS)
        .thenAccept(available -> {
            cdnAvailable = available;
            cdnCheckPerformed = true;
            
            if (available) {
                logger.info("✅ CDN连接正常，使用在线模式");
            } else {
                logger.warn("⚠️ CDN连接不可用，切换到离线模式");
            }
        }).exceptionally(throwable -> {
            cdnAvailable = false;
            cdnCheckPerformed = true;
            logger.warn("❌ CDN连接测试超时，切换到离线模式");
            return null;
        });
    }
    
    /**
     * 测试URL连接
     */
    private boolean testConnection(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(offlineConfig.getConnectionTimeout());
            connection.setReadTimeout(offlineConfig.getConnectionTimeout());
            
            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 400;
            
        } catch (IOException e) {
            logger.debug("连接测试失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查是否为离线模式
     */
    public boolean isOfflineMode() {
        return offlineConfig.isEnabled() || !cdnAvailable;
    }
    
    /**
     * 获取适当的模板名称
     */
    public String getLayoutTemplate() {
        return isOfflineMode() ? "main-layout-offline" : "main-layout";
    }
    
    /**
     * 强制设置CDN可用性（用于测试）
     */
    public void setCdnAvailable(boolean available) {
        this.cdnAvailable = available;
        this.cdnCheckPerformed = true;
    }
}