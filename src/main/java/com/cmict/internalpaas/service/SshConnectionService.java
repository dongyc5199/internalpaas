package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class SshConnectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SshConnectionService.class);
    
    private static final int CONNECTION_TIMEOUT = 5000; // 5秒连接超时
    private static final int CHECK_TIMEOUT = 3000; // 3秒检测超时
    
    /**
     * 检查服务器SSH连接状态
     */
    public Server.ConnectionStatus checkConnection(Server server) {
        if (server == null || server.getHostname() == null) {
            return Server.ConnectionStatus.FAILED;
        }
        
        try {
            return CompletableFuture.supplyAsync(() -> performConnectionCheck(server))
                    .get(CHECK_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.warn("SSH连接检查超时: {}", server.getHostname(), e);
            return Server.ConnectionStatus.TIMEOUT;
        }
    }
    
    /**
     * 执行实际的SSH连接检查
     */
    private Server.ConnectionStatus performConnectionCheck(Server server) {
        JSch jsch = new JSch();
        Session session = null;
        
        try {
            // 设置主机密钥检查为不检查（生产环境应配置已知主机）
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            
            session = jsch.getSession(
                server.getSshUsername() != null ? server.getSshUsername() : "root",
                server.getHostname(),
                server.getSshPort() != null ? server.getSshPort() : 22
            );
            
            session.setConfig(config);
            session.setTimeout(CONNECTION_TIMEOUT);
            
            // 设置认证方式
            if (server.getSshPassword() != null && !server.getSshPassword().isEmpty()) {
                session.setPassword(server.getSshPassword());
            } else if (server.getSshKeyPath() != null && !server.getSshKeyPath().isEmpty()) {
                File keyFile = new File(server.getSshKeyPath());
                if (keyFile.exists()) {
                    if (server.getSshKeyPassphrase() != null && !server.getSshKeyPassphrase().isEmpty()) {
                        jsch.addIdentity(server.getSshKeyPath(), server.getSshKeyPassphrase());
                    } else {
                        jsch.addIdentity(server.getSshKeyPath());
                    }
                }
            }
            
            // 尝试连接
            session.connect(CONNECTION_TIMEOUT);
            
            // 测试执行一个简单的命令
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("echo 'connection_test'");
            channel.connect(1000);
            
            // 等待命令执行完成
            Thread.sleep(500);
            
            if (channel.isConnected()) {
                channel.disconnect();
                return Server.ConnectionStatus.CONNECTED;
            } else {
                return Server.ConnectionStatus.FAILED;
            }
            
        } catch (JSchException e) {
            if (e.getMessage() != null && e.getMessage().contains("Auth fail")) {
                logger.warn("SSH认证失败: {}", server.getHostname());
                return Server.ConnectionStatus.AUTH_FAILED;
            } else {
                logger.warn("SSH连接失败: {}", server.getHostname(), e);
                return Server.ConnectionStatus.FAILED;
            }
        } catch (Exception e) {
            logger.error("SSH连接检查异常: {}", server.getHostname(), e);
            return Server.ConnectionStatus.FAILED;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
    
    /**
     * 测试服务器是否可达（不尝试认证）
     */
    public boolean isReachable(Server server) {
        try {
            String host = server.getHostname();
            int port = server.getSshPort() != null ? server.getSshPort() : 22;
            
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), 2000);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取连接状态的描述信息
     */
    public String getConnectionStatusDescription(Server.ConnectionStatus status) {
        switch (status) {
            case CONNECTED:
                return "连接成功";
            case FAILED:
                return "连接失败";
            case TIMEOUT:
                return "连接超时";
            case AUTH_FAILED:
                return "认证失败";
            case UNKNOWN:
                return "未检查";
            default:
                return "未知";
        }
    }
}