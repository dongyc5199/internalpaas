package com.cmict.internalpaas.test;

import com.cmict.internalpaas.service.PasswordEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SSH密码加密功能测试
 * 在应用启动时自动执行基础测试
 */
@Component
@ConditionalOnProperty(name = "app.test.password-encryption.enabled", havingValue = "true")
public class PasswordEncryptionTest implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncryptionTest.class);

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("开始SSH密码加密功能测试...");
        
        try {
            // 测试基本加密解密功能
            testBasicEncryptionDecryption();
            
            // 测试空密码处理
            testNullAndEmptyPassword();
            
            // 测试密码识别功能
            testPasswordIdentification();
            
            // 测试加密一致性
            testEncryptionConsistency();
            
            logger.info("SSH密码加密功能测试全部通过！");
            
        } catch (Exception e) {
            logger.error("SSH密码加密功能测试失败", e);
            throw e;
        }
    }

    /**
     * 测试基本加密解密功能
     */
    private void testBasicEncryptionDecryption() throws Exception {
        logger.info("测试基本加密解密功能...");
        
        String originalPassword = "test123!@#";
        
        // 加密密码
        String encryptedPassword = passwordEncryptionService.encryptPassword(originalPassword);
        logger.debug("原始密码: {} -> 加密密码: {}", originalPassword, encryptedPassword);
        
        // 验证加密后不等于原始密码
        if (originalPassword.equals(encryptedPassword)) {
            throw new RuntimeException("加密后密码不应与原始密码相同");
        }
        
        // 解密密码
        String decryptedPassword = passwordEncryptionService.decryptPassword(encryptedPassword);
        logger.debug("解密密码: {}", decryptedPassword);
        
        // 验证解密结果与原始密码一致
        if (!originalPassword.equals(decryptedPassword)) {
            throw new RuntimeException("解密后密码与原始密码不一致");
        }
        
        logger.info("✓ 基本加密解密功能测试通过");
    }

    /**
     * 测试空密码处理
     */
    private void testNullAndEmptyPassword() throws Exception {
        logger.info("测试空密码处理...");
        
        // 测试null密码
        String nullEncrypted = passwordEncryptionService.encryptPassword(null);
        if (nullEncrypted != null) {
            throw new RuntimeException("null密码加密后应返回null");
        }
        
        String nullDecrypted = passwordEncryptionService.decryptPassword(null);
        if (nullDecrypted != null) {
            throw new RuntimeException("null密码解密后应返回null");
        }
        
        // 测试空字符串密码
        String emptyEncrypted = passwordEncryptionService.encryptPassword("");
        if (emptyEncrypted != null) {
            throw new RuntimeException("空字符串密码加密后应返回null");
        }
        
        String emptyDecrypted = passwordEncryptionService.decryptPassword("");
        if (emptyDecrypted != null) {
            throw new RuntimeException("空字符串密码解密后应返回null");
        }
        
        logger.info("✓ 空密码处理测试通过");
    }

    /**
     * 测试密码识别功能
     */
    private void testPasswordIdentification() throws Exception {
        logger.info("测试密码识别功能...");
        
        String plainPassword = "plaintext123";
        String encryptedPassword = passwordEncryptionService.encryptPassword(plainPassword);
        
        // 测试明文密码识别
        if (passwordEncryptionService.isPasswordEncrypted(plainPassword)) {
            throw new RuntimeException("明文密码不应被识别为已加密");
        }
        
        // 测试加密密码识别
        if (!passwordEncryptionService.isPasswordEncrypted(encryptedPassword)) {
            throw new RuntimeException("加密密码应被识别为已加密");
        }
        
        // 测试null和空字符串
        if (passwordEncryptionService.isPasswordEncrypted(null)) {
            throw new RuntimeException("null不应被识别为已加密");
        }
        
        if (passwordEncryptionService.isPasswordEncrypted("")) {
            throw new RuntimeException("空字符串不应被识别为已加密");
        }
        
        logger.info("✓ 密码识别功能测试通过");
    }

    /**
     * 测试加密一致性
     */
    private void testEncryptionConsistency() throws Exception {
        logger.info("测试加密一致性...");
        
        String password = "consistency_test_password_123";
        
        // 对同一密码加密多次，应产生不同的加密结果（因为使用了随机IV）
        String encrypted1 = passwordEncryptionService.encryptPassword(password);
        String encrypted2 = passwordEncryptionService.encryptPassword(password);
        
        if (encrypted1.equals(encrypted2)) {
            throw new RuntimeException("同一密码的多次加密应产生不同结果（使用随机IV）");
        }
        
        // 但是解密结果应该相同
        String decrypted1 = passwordEncryptionService.decryptPassword(encrypted1);
        String decrypted2 = passwordEncryptionService.decryptPassword(encrypted2);
        
        if (!password.equals(decrypted1) || !password.equals(decrypted2)) {
            throw new RuntimeException("不同加密结果的解密应得到相同的原始密码");
        }
        
        if (!decrypted1.equals(decrypted2)) {
            throw new RuntimeException("不同加密结果的解密应得到相同结果");
        }
        
        logger.info("✓ 加密一致性测试通过");
    }

    /**
     * 测试特殊字符密码
     */
    public void testSpecialCharacterPasswords() throws Exception {
        logger.info("测试特殊字符密码...");
        
        String[] specialPasswords = {
            "password@123",
            "测试密码123",
            "пароль123",
            "パスワード123",
            "🔐secure_password_🔑",
            "line1\nline2\ttab",
            "quotes'and\"double",
            "spaces and more spaces"
        };
        
        for (String password : specialPasswords) {
            try {
                String encrypted = passwordEncryptionService.encryptPassword(password);
                String decrypted = passwordEncryptionService.decryptPassword(encrypted);
                
                if (!password.equals(decrypted)) {
                    throw new RuntimeException("特殊字符密码加解密失败: " + password);
                }
                
                logger.debug("特殊字符密码测试通过: {}", password);
            } catch (Exception e) {
                throw new RuntimeException("特殊字符密码测试失败: " + password, e);
            }
        }
        
        logger.info("✓ 特殊字符密码测试通过");
    }

    /**
     * 性能测试
     */
    public void testPerformance() throws Exception {
        logger.info("测试加密解密性能...");
        
        String password = "performance_test_password_12345";
        int testCount = 100;
        
        // 测试加密性能
        long startTime = System.currentTimeMillis();
        String[] encryptedPasswords = new String[testCount];
        
        for (int i = 0; i < testCount; i++) {
            encryptedPasswords[i] = passwordEncryptionService.encryptPassword(password);
        }
        
        long encryptTime = System.currentTimeMillis() - startTime;
        logger.info("加密 {} 次耗时: {} ms，平均每次: {} ms", testCount, encryptTime, (double)encryptTime / testCount);
        
        // 测试解密性能
        startTime = System.currentTimeMillis();
        
        for (String encrypted : encryptedPasswords) {
            String decrypted = passwordEncryptionService.decryptPassword(encrypted);
            if (!password.equals(decrypted)) {
                throw new RuntimeException("性能测试中解密结果不正确");
            }
        }
        
        long decryptTime = System.currentTimeMillis() - startTime;
        logger.info("解密 {} 次耗时: {} ms，平均每次: {} ms", testCount, decryptTime, (double)decryptTime / testCount);
        
        // 性能阈值检查（加密或解密单次不超过50ms）
        if (encryptTime / testCount > 50 || decryptTime / testCount > 50) {
            logger.warn("密码加解密性能较慢，建议优化");
        }
        
        logger.info("✓ 性能测试完成");
    }
}