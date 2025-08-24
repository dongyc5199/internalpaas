package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.repository.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class PortManagerService {

    private static final Logger logger = LoggerFactory.getLogger(PortManagerService.class);
    private static final int PORT_CHECK_TIMEOUT = 1000; // 1秒超时
    private static final int MAX_RETRY_ATTEMPTS = 5; // 最大重试次数

    @Value("${app.port.range.start:8000}")
    private int portRangeStart;
    
    @Value("${app.port.range.end:9000}")
    private int portRangeEnd;
    
    @Value("${app.debug.port.range.start:5000}")
    private int debugPortRangeStart;
    
    @Value("${app.debug.port.range.end:5999}")
    private int debugPortRangeEnd;
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    /**
     * 为用户分配可用端口
     */
    public PortAllocation allocatePorts(User user) {
        logger.info("开始为用户 {} 分配端口", user.getUsername());
        
        int appPort = findAvailablePortWithRetry(user, portRangeStart, portRangeEnd, false);
        int debugPort = findAvailablePortWithRetry(user, debugPortRangeStart, debugPortRangeEnd, true);
        
        logger.info("端口分配成功: 用户={}, 应用端口={}, 调试端口={}", 
            user.getUsername(), appPort, debugPort);
        
        return new PortAllocation(appPort, debugPort);
    }
    
    /**
     * 带重试机制的端口查找
     */
    private int findAvailablePortWithRetry(User user, int start, int end, boolean isDebug) {
        String portType = isDebug ? "调试端口" : "应用端口";
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            logger.debug("第 {} 次尝试分配{}: 用户={}, 范围={}-{}", 
                attempt, portType, user.getUsername(), start, end);
                
            try {
                int port = findAvailablePort(user, start, end, isDebug);
                
                // 验证端口是否真正可用
                if (isPortActuallyAvailable(port)) {
                    logger.info("{}分配成功: 端口={}, 用户={}, 尝试次数={}", 
                        portType, port, user.getUsername(), attempt);
                    return port;
                } else {
                    logger.warn("{}系统级占用: 端口={}, 用户={}, 尝试次数={}", 
                        portType, port, user.getUsername(), attempt);
                }
            } catch (RuntimeException e) {
                logger.warn("{}分配失败: 用户={}, 尝试次数={}, 错误: {}", 
                    portType, user.getUsername(), attempt, e.getMessage());
                    
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    throw e;
                }
            }
            
            // 等待一段时间再重试
            try {
                TimeUnit.MILLISECONDS.sleep(100 * attempt); // 递增延迟
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("端口分配被中断", ie);
            }
        }
        
        throw new RuntimeException(String.format(
            "%s分配失败: 用户=%s, 范围=%d-%d, 已重试%d次", 
            portType, user.getUsername(), start, end, MAX_RETRY_ATTEMPTS));
    }
    
    /**
     * 查找可用端口（仅检查数据库记录）
     */
    private int findAvailablePort(User user, int start, int end, boolean isDebug) {
        Set<Integer> usedPorts = getUsedPorts(user, isDebug);
        
        // 也需要检查其他用户的端口占用情况
        Set<Integer> allUsedPorts = getAllUsedPorts(isDebug);
        usedPorts.addAll(allUsedPorts);
        
        logger.debug("查找可用端口: 用户={}, 范围={}-{}, 已用端口数量={}, 类型={}", 
            user.getUsername(), start, end, usedPorts.size(), isDebug ? "调试" : "应用");
        
        for (int port = start; port <= end; port++) {
            if (!usedPorts.contains(port)) {
                logger.debug("找到潜在可用端口: {}", port);
                return port;
            }
        }
        
        throw new RuntimeException(String.format(
            "端口范围 %d-%d 中没有可用端口 (类型: %s, 已用: %d/%d)", 
            start, end, isDebug ? "调试" : "应用", usedPorts.size(), end - start + 1));
    }
    
    /**
     * 获取用户已使用的端口
     */
    private Set<Integer> getUsedPorts(User user, boolean isDebug) {
        Set<Integer> usedPorts = new HashSet<>();
        
        applicationRepository.findByUser(user).forEach(app -> {
            if (isDebug && app.getDebugPort() != null) {
                usedPorts.add(app.getDebugPort());
            } else if (!isDebug && app.getPort() != null) {
                usedPorts.add(app.getPort());
            }
        });
        
        return usedPorts;
    }
    
    /**
     * 获取所有用户已使用的端口（避免全局冲突）
     */
    private Set<Integer> getAllUsedPorts(boolean isDebug) {
        Set<Integer> usedPorts = new HashSet<>();
        
        applicationRepository.findAll().forEach(app -> {
            if (isDebug && app.getDebugPort() != null) {
                usedPorts.add(app.getDebugPort());
            } else if (!isDebug && app.getPort() != null) {
                usedPorts.add(app.getPort());
            }
        });
        
        return usedPorts;
    }
    
    /**
     * 检查端口是否真正可用（系统级检查）
     */
    private boolean isPortActuallyAvailable(int port) {
        // 检查端口是否被系统其他进程占用
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress("localhost", port));
            
            logger.debug("端口可用性检查通过: {}", port);
            return true;
            
        } catch (IOException e) {
            logger.debug("端口不可用: {} - {}", port, e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查端口是否在使用中（通过连接测试）
     */
    public boolean isPortInUse(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), PORT_CHECK_TIMEOUT);
            return true; // 连接成功，说明端口在使用中
        } catch (IOException e) {
            return false; // 连接失败，端口可能未使用
        }
    }
    
    /**
     * 获取端口使用统计信息
     */
    public PortUsageStats getPortUsageStats() {
        Set<Integer> usedAppPorts = getAllUsedPorts(false);
        Set<Integer> usedDebugPorts = getAllUsedPorts(true);
        
        int totalAppPorts = portRangeEnd - portRangeStart + 1;
        int totalDebugPorts = debugPortRangeEnd - debugPortRangeStart + 1;
        
        return new PortUsageStats(
            usedAppPorts.size(), totalAppPorts,
            usedDebugPorts.size(), totalDebugPorts,
            portRangeStart, portRangeEnd,
            debugPortRangeStart, debugPortRangeEnd
        );
    }
    
    public static class PortAllocation {
        private final int applicationPort;
        private final int debugPort;
        
        public PortAllocation(int applicationPort, int debugPort) {
            this.applicationPort = applicationPort;
            this.debugPort = debugPort;
        }
        
        public int getApplicationPort() { return applicationPort; }
        public int getDebugPort() { return debugPort; }
        
        @Override
        public String toString() {
            return String.format("PortAllocation{app=%d, debug=%d}", applicationPort, debugPort);
        }
    }
    
    public static class PortUsageStats {
        private final int usedAppPorts;
        private final int totalAppPorts;
        private final int usedDebugPorts;
        private final int totalDebugPorts;
        private final int appPortRangeStart;
        private final int appPortRangeEnd;
        private final int debugPortRangeStart;
        private final int debugPortRangeEnd;
        
        public PortUsageStats(int usedAppPorts, int totalAppPorts, 
                             int usedDebugPorts, int totalDebugPorts,
                             int appPortRangeStart, int appPortRangeEnd,
                             int debugPortRangeStart, int debugPortRangeEnd) {
            this.usedAppPorts = usedAppPorts;
            this.totalAppPorts = totalAppPorts;
            this.usedDebugPorts = usedDebugPorts;
            this.totalDebugPorts = totalDebugPorts;
            this.appPortRangeStart = appPortRangeStart;
            this.appPortRangeEnd = appPortRangeEnd;
            this.debugPortRangeStart = debugPortRangeStart;
            this.debugPortRangeEnd = debugPortRangeEnd;
        }
        
        // Getters
        public int getUsedAppPorts() { return usedAppPorts; }
        public int getTotalAppPorts() { return totalAppPorts; }
        public int getUsedDebugPorts() { return usedDebugPorts; }
        public int getTotalDebugPorts() { return totalDebugPorts; }
        public int getAppPortRangeStart() { return appPortRangeStart; }
        public int getAppPortRangeEnd() { return appPortRangeEnd; }
        public int getDebugPortRangeStart() { return debugPortRangeStart; }
        public int getDebugPortRangeEnd() { return debugPortRangeEnd; }
        
        public double getAppPortUsagePercentage() {
            return totalAppPorts > 0 ? (double) usedAppPorts / totalAppPorts * 100 : 0;
        }
        
        public double getDebugPortUsagePercentage() {
            return totalDebugPorts > 0 ? (double) usedDebugPorts / totalDebugPorts * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "PortUsageStats{appPorts=%d/%d(%.1f%%), debugPorts=%d/%d(%.1f%%)}",
                usedAppPorts, totalAppPorts, getAppPortUsagePercentage(),
                usedDebugPorts, totalDebugPorts, getDebugPortUsagePercentage()
            );
        }
    }
}