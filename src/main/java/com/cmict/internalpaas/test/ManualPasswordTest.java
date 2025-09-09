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

/**
 * 手动密码加密测试
 * 创建测试服务器并验证密码是否正确加密存储
 */
@Component
@ConditionalOnProperty(name = "app.manual-test.enabled", havingValue = "true")
public class ManualPasswordTest implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ManualPasswordTest.class);

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

    @Autowired
    private ServerService serverService;

    @Autowired
    private SshConnectionService sshConnectionService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("=== 开始手动密码加密测试 ===");
        
        // 测试基础加密功能
        testBasicEncryption();
        
        // 创建测试服务器并验证
        testServerPasswordEncryption();
        
        logger.info("=== 手动密码加密测试完成 ===");
    }

    private void testBasicEncryption() {
        logger.info("1. 测试基础密码加密功能...");
        
        String testPassword = "my_test_password_123!@#";
        
        try {
            // 加密密码
            String encrypted = passwordEncryptionService.encryptPassword(testPassword);
            logger.info("   原始密码: {}", testPassword);
            logger.info("   加密结果: {}", encrypted);
            
            // 验证是否识别为加密密码
            boolean isEncrypted = passwordEncryptionService.isPasswordEncrypted(encrypted);
            logger.info("   加密识别: {}", isEncrypted ? "通过" : "失败");
            
            // 解密验证
            String decrypted = passwordEncryptionService.decryptPassword(encrypted);
            logger.info("   解密结果: {}", decrypted);
            
            boolean isValid = testPassword.equals(decrypted);
            logger.info("   验证结果: {}", isValid ? "✅ 通过" : "❌ 失败");
            
        } catch (Exception e) {
            logger.error("基础加密测试失败", e);
        }
    }

    private void testServerPasswordEncryption() {
        logger.info("2. 测试服务器密码加密存储...");
        
        try {
            // 创建测试服务器
            Server testServer = new Server();
            testServer.setName("Password Test Server");
            testServer.setHostname("test.example.com");
            testServer.setPort(8080);
            testServer.setBaseWorkDirectory("/tmp/test");
            testServer.setSshPort(22);
            testServer.setSshUsername("testuser");
            
            String plainPassword = "server_password_456";
            
            // 设置密码（应该自动加密）
            testServer.setPasswordEncryptionService(passwordEncryptionService);
            testServer.setSshPassword(plainPassword);
            
            // 验证加密存储
            String storedPassword = testServer.getSshPasswordEncrypted();
            logger.info("   明文密码: {}", plainPassword);
            logger.info("   存储密码: {}", storedPassword);
            
            // 检查是否已加密
            boolean isEncrypted = passwordEncryptionService.isPasswordEncrypted(storedPassword);
            logger.info("   存储状态: {}", isEncrypted ? "已加密" : "明文");
            
            // 验证可以正确获取明文密码
            String retrievedPassword = testServer.getSshPassword();
            logger.info("   获取密码: {}", retrievedPassword);
            
            boolean passwordMatch = plainPassword.equals(retrievedPassword);
            logger.info("   密码匹配: {}", passwordMatch ? "✅ 通过" : "❌ 失败");
            
            // 测试脱敏显示
            String maskedPassword = testServer.getPasswordMasked();
            logger.info("   脱敏显示: {}", maskedPassword);
            
            // 保存服务器到数据库
            logger.info("3. 保存服务器到数据库...");
            Server savedServer = serverService.saveServer(testServer);
            logger.info("   服务器ID: {}", savedServer.getId());
            logger.info("   服务器名称: {}", savedServer.getName());
            
            // 从数据库重新读取验证
            logger.info("4. 从数据库重新读取验证...");
            Server reloadedServer = serverService.getServerById(savedServer.getId()).orElse(null);
            if (reloadedServer != null) {
                String dbStoredPassword = reloadedServer.getSshPasswordEncrypted();
                String dbRetrievedPassword = reloadedServer.getSshPassword();
                
                logger.info("   数据库存储密码: {}", dbStoredPassword);
                logger.info("   数据库获取密码: {}", dbRetrievedPassword);
                
                boolean dbEncrypted = passwordEncryptionService.isPasswordEncrypted(dbStoredPassword);
                boolean dbPasswordMatch = plainPassword.equals(dbRetrievedPassword);
                
                logger.info("   数据库加密状态: {}", dbEncrypted ? "已加密" : "明文");
                logger.info("   数据库密码匹配: {}", dbPasswordMatch ? "✅ 通过" : "❌ 失败");
                
                // 测试SSH连接服务验证
                logger.info("5. 测试SSH连接服务密码验证...");
                boolean sshValidation = sshConnectionService.validateServerPassword(reloadedServer);
                logger.info("   SSH服务验证: {}", sshValidation ? "✅ 通过" : "❌ 失败");
                
                // 最终结果
                if (dbEncrypted && dbPasswordMatch && sshValidation) {
                    logger.info("🎉 所有测试通过！密码已正确加密存储并可以正常使用");
                } else {
                    logger.error("❌ 测试失败！存在密码加密或验证问题");
                }
            } else {
                logger.error("❌ 无法从数据库重新读取服务器");
            }
            
        } catch (Exception e) {
            logger.error("服务器密码测试失败", e);
        }
    }
}