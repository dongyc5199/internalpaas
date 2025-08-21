package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class MonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);
    private static final int COMMAND_TIMEOUT = 10000; // 10秒命令超时
    
    /**
     * 获取服务器实时指标
     */
    public ServerMetrics getServerMetrics(Server server) {
        ServerMetrics metrics = new ServerMetrics(server.getName(), server.getHostname());
        
        try {
            return CompletableFuture.supplyAsync(() -> collectMetrics(server))
                    .get(COMMAND_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.warn("获取服务器指标超时: {}", server.getHostname(), e);
            metrics.setCpuUsage(0.0);
            metrics.setMemoryUsage(0.0);
            metrics.setDiskUsage(0.0);
            return metrics;
        }
    }
    
    /**
     * 收集服务器指标
     */
    private ServerMetrics collectMetrics(Server server) {
        ServerMetrics metrics = new ServerMetrics(server.getName(), server.getHostname());
        
        try {
            // 获取CPU使用率
            Double cpuUsage = getCpuUsage(server);
            metrics.setCpuUsage(cpuUsage);
            
            // 获取内存信息
            Long[] memoryInfo = getMemoryInfo(server);
            if (memoryInfo != null && memoryInfo.length >= 2) {
                metrics.setMemoryTotal(memoryInfo[0]);
                metrics.setMemoryUsed(memoryInfo[1]);
                if (memoryInfo[0] > 0) {
                    metrics.setMemoryUsage((double) memoryInfo[1] / memoryInfo[0] * 100);
                }
            }
            
            // 获取磁盘信息
            Long[] diskInfo = getDiskInfo(server);
            if (diskInfo != null && diskInfo.length >= 2) {
                metrics.setDiskTotal(diskInfo[0]);
                metrics.setDiskUsed(diskInfo[1]);
                if (diskInfo[0] > 0) {
                    metrics.setDiskUsage((double) diskInfo[1] / diskInfo[0] * 100);
                }
            }
            
        } catch (Exception e) {
            logger.error("收集服务器指标失败: {}", server.getHostname(), e);
        }
        
        return metrics;
    }
    
    /**
     * 获取CPU使用率
     */
    private Double getCpuUsage(Server server) throws Exception {
        String cpuCommand = "top -bn1 | grep 'Cpu(s)' | awk '{print $2}' | sed 's/%us,//'";
        String result = executeCommand(server, cpuCommand);
        
        try {
            return Double.parseDouble(result.trim());
        } catch (NumberFormatException e) {
            // 备用命令
            cpuCommand = "cat /proc/stat | head -1 | awk '{print ($2+$3+$4)*100/($2+$3+$4+$5+$6+$7+$8)}'";
            result = executeCommand(server, cpuCommand);
            try {
                return Double.parseDouble(result.trim());
            } catch (NumberFormatException ex) {
                return 0.0;
            }
        }
    }
    
    /**
     * 获取内存信息
     */
    private Long[] getMemoryInfo(Server server) throws Exception {
        String memCommand = "free | grep 'Mem:' | awk '{print $2, $3}'";
        String result = executeCommand(server, memCommand);
        
        String[] parts = result.trim().split("\\s+");
        if (parts.length >= 2) {
            return new Long[]{
                Long.parseLong(parts[0]) * 1024,  // KB to bytes
                Long.parseLong(parts[1]) * 1024   // KB to bytes
            };
        }
        return null;
    }
    
    /**
     * 获取磁盘信息
     */
    private Long[] getDiskInfo(Server server) throws Exception {
        String diskCommand = "df -B1 / | tail -1 | awk '{print $2, $3}'";
        String result = executeCommand(server, diskCommand);
        
        String[] parts = result.trim().split("\\s+");
        if (parts.length >= 2) {
            return new Long[]{
                Long.parseLong(parts[0]),
                Long.parseLong(parts[1])
            };
        }
        return null;
    }
    
    /**
     * 执行远程命令
     */
    private String executeCommand(Server server, String command) throws Exception {
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        BufferedReader reader = null;
        
        try {
            session = createSession(jsch, server);
            session.connect(5000);
            
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            
            channel.setInputStream(null);
            channel.setErrStream(System.err);
            
            reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect(1000);
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            return output.toString().trim();
            
        } finally {
            if (reader != null) try { reader.close(); } catch (Exception ignored) {}
            if (channel != null) try { channel.disconnect(); } catch (Exception ignored) {}
            if (session != null) try { session.disconnect(); } catch (Exception ignored) {}
        }
    }
    
    /**
     * 创建SSH会话
     */
    private Session createSession(JSch jsch, Server server) throws JSchException {
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        
        Session session = jsch.getSession(
            server.getSshUsername() != null ? server.getSshUsername() : "root",
            server.getHostname(),
            server.getSshPort() != null ? server.getSshPort() : 22
        );
        
        session.setConfig(config);
        session.setTimeout(5000);
        
        if (server.getSshPassword() != null && !server.getSshPassword().isEmpty()) {
            session.setPassword(server.getSshPassword());
        } else if (server.getSshKeyPath() != null && !server.getSshKeyPath().isEmpty()) {
            File keyFile = new java.io.File(server.getSshKeyPath());
            if (keyFile.exists()) {
                if (server.getSshKeyPassphrase() != null && !server.getSshKeyPassphrase().isEmpty()) {
                    jsch.addIdentity(server.getSshKeyPath(), server.getSshKeyPassphrase());
                } else {
                    jsch.addIdentity(server.getSshKeyPath());
                }
            }
        }
        
        return session;
    }
}