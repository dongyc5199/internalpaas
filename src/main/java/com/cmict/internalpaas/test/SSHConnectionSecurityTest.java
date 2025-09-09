package com.cmict.internalpaas.test;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.service.PasswordEncryptionService;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.SshConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SSH连接安全性测试
 * 验证加密密码在SSH连接中的使用
 */
@Component
@ConditionalOnProperty(name = "app.test.ssh-security.enabled", havingValue = "true")
public class SSHConnectionSecurityTest implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SSHConnectionSecurityTest.class);

    @Autowired
    private ServerService serverService;

    @Autowired
    private SshConnectionService sshConnectionService;

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("开始SSH连接安全性测试...");
        
        try {
            // 测试服务器密码设置
            testServerPasswordSetup();
            
            // 测试现有服务器的密码安全性
            testExistingServersPasswordSecurity();
            
            logger.info("SSH连接安全性测试完成");
            
        } catch (Exception e) {
            logger.error("SSH连接安全性测试失败", e);
            throw e;
        }
    }

    /**
     * 测试服务器密码设置
     */
    private void testServerPasswordSetup() throws Exception {
        logger.info("测试服务器密码设置功能...");
        
        // 创建测试服务器
        Server testServer = new Server();
        testServer.setName("Security Test Server");
        testServer.setHostname("test.example.com");
        testServer.setPort(8080);
        testServer.setBaseWorkDirectory("/tmp/test");
        testServer.setSshPort(22);
        testServer.setSshUsername("testuser");
        
        // 设置明文密码
        String plainPassword = "test_password_123";
        testServer.setPasswordEncryptionService(passwordEncryptionService);
        testServer.setSshPassword(plainPassword);
        
        // 验证密码已自动加密
        String storedPassword = testServer.getSshPasswordEncrypted();
        if (plainPassword.equals(storedPassword)) {
            throw new RuntimeException("密码未自动加密");
        }
        
        if (!passwordEncryptionService.isPasswordEncrypted(storedPassword)) {
            throw new RuntimeException("存储的密码未正确加密");
        }
        
        // 验证可以正确获取明文密码
        String retrievedPassword = testServer.getSshPassword();
        if (!plainPassword.equals(retrievedPassword)) {
            throw new RuntimeException("无法正确获取明文密码");
        }
        
        // 验证SSH连接服务能够验证密码
        boolean isValidPassword = sshConnectionService.validateServerPassword(testServer);
        if (!isValidPassword) {
            throw new RuntimeException("SSH连接服务无法验证加密密码");
        }
        
        logger.info("✓ 服务器密码设置功能测试通过");
    }

    /**
     * 测试现有服务器的密码安全性
     */
    private void testExistingServersPasswordSecurity() throws Exception {
        logger.info("检查现有服务器的密码安全性...");
        
        List<Server> allServers = serverService.getAllServers();
        
        int totalServers = allServers.size();
        int serversWithPassword = 0;
        int serversWithEncryptedPassword = 0;
        int serversWithPlaintextPassword = 0;
        
        for (Server server : allServers) {
            String password = server.getSshPasswordEncrypted();
            
            if (password != null && !password.isEmpty()) {
                serversWithPassword++;
                
                if (passwordEncryptionService.isPasswordEncrypted(password)) {
                    serversWithEncryptedPassword++;
                    logger.debug("服务器 {} 使用加密密码", server.getName());
                    
                    // 验证加密密码可以正确解密
                    try {
                        String decrypted = passwordEncryptionService.decryptPassword(password);
                        if (decrypted == null || decrypted.isEmpty()) {
                            logger.warn("服务器 {} 的加密密码解密后为空", server.getName());
                        }
                    } catch (Exception e) {
                        logger.error("服务器 {} 的加密密码解密失败", server.getName(), e);
                    }
                    
                } else {
                    serversWithPlaintextPassword++;
                    logger.warn("⚠️  服务器 {} 仍使用明文密码，存在安全风险", server.getName());
                }
            } else {
                logger.debug("服务器 {} 未设置SSH密码", server.getName());
            }
        }
        
        logger.info("密码安全性检查结果:");
        logger.info("  总服务器数: {}", totalServers);
        logger.info("  设置密码的服务器: {}", serversWithPassword);
        logger.info("  使用加密密码的服务器: {}", serversWithEncryptedPassword);
        logger.info("  仍使用明文密码的服务器: {}", serversWithPlaintextPassword);
        
        if (serversWithPlaintextPassword > 0) {
            logger.warn("⚠️  发现 {} 个服务器仍使用明文密码，建议执行密码迁移", serversWithPlaintextPassword);
        } else if (serversWithPassword > 0) {
            logger.info("✅ 所有设置密码的服务器都已使用加密存储");
        }
        
        // 计算密码安全比例
        if (serversWithPassword > 0) {
            double securityRatio = (double) serversWithEncryptedPassword / serversWithPassword * 100;
            logger.info("密码安全比例: {:.1f}%", securityRatio);
            
            if (securityRatio < 100.0) {
                logger.warn("密码安全比例未达到100%，建议执行完整的密码迁移");
            }
        }
    }

    /**
     * 模拟SSH连接测试（不实际连接）
     */
    public void simulateSSHConnectionTest() {
        logger.info("模拟SSH连接测试...");
        
        List<Server> activeServers = serverService.getActiveServers();
        
        for (Server server : activeServers) {
            try {
                // 验证服务器密码设置
                boolean hasValidPassword = sshConnectionService.validateServerPassword(server);
                
                if (hasValidPassword) {
                    logger.debug("服务器 {} 的密码设置有效", server.getName());
                    
                    // 模拟连接测试（检查网络可达性）
                    boolean isReachable = sshConnectionService.isReachable(server);
                    if (isReachable) {
                        logger.debug("服务器 {} 网络可达", server.getName());
                    } else {
                        logger.debug("服务器 {} 网络不可达（这在测试环境中是正常的）", server.getName());
                    }
                    
                } else {
                    logger.debug("服务器 {} 密码设置无效或未设置", server.getName());
                }
                
            } catch (Exception e) {
                logger.debug("模拟连接测试服务器 {} 时出现异常（这在测试环境中是正常的）: {}", 
                    server.getName(), e.getMessage());
            }
        }
        
        logger.info("✓ 模拟SSH连接测试完成");
    }

    /**
     * 验证密码脱敏功能
     */
    public void testPasswordMasking() {
        logger.info("测试密码脱敏功能...");
        
        List<Server> allServers = serverService.getAllServers();
        
        for (Server server : allServers) {
            // 测试脱敏方法
            String maskedPassword = server.getPasswordMasked();
            String maskedKeyPassphrase = server.getKeyPassphraseMasked();
            
            if (server.getSshPasswordEncrypted() != null && !server.getSshPasswordEncrypted().isEmpty()) {
                if (maskedPassword == null || !maskedPassword.contains("***")) {
                    logger.warn("服务器 {} 的密码脱敏功能可能不正常", server.getName());
                } else {
                    logger.debug("服务器 {} 密码脱敏正常: {}", server.getName(), maskedPassword);
                }
            }
            
            // 确保脱敏信息不包含实际密码
            if (maskedPassword != null && maskedPassword.length() > 10 && 
                !maskedPassword.contains("***")) {
                logger.warn("服务器 {} 的脱敏密码可能泄露了实际信息", server.getName());
            }
        }
        
        logger.info("✓ 密码脱敏功能测试完成");
    }
}