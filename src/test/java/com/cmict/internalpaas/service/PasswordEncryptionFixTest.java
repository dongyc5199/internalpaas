package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码加密解密修复验证测试
 * 验证Server实体JSON序列化不会触发密码解密异常
 */
@SpringBootTest
@ActiveProfiles("test")
public class PasswordEncryptionFixTest {

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 测试密码加密解密基本功能
     */
    @Test
    public void testPasswordEncryptionDecryption() throws Exception {
        String originalPassword = "test_password_123";
        
        // 测试加密
        String encrypted = passwordEncryptionService.encryptPassword(originalPassword);
        assertNotNull(encrypted, "加密后的密码不应为null");
        assertNotEquals(originalPassword, encrypted, "加密后密码应不同于原始密码");
        assertTrue(encrypted.contains(":"), "加密密码应包含分隔符");
        
        // 测试解密
        String decrypted = passwordEncryptionService.decryptPassword(encrypted);
        assertEquals(originalPassword, decrypted, "解密后应恢复原始密码");
        
        // 测试加密状态检查
        assertTrue(passwordEncryptionService.isPasswordEncrypted(encrypted), "应正确识别加密密码");
        assertFalse(passwordEncryptionService.isPasswordEncrypted(originalPassword), "应正确识别明文密码");
        
        System.out.println("✅ 密码加密解密基本功能测试通过");
    }

    /**
     * 测试Server实体JSON序列化不会触发密码解密异常
     */
    @Test
    public void testServerJsonSerializationDoesNotTriggerDecryption() throws Exception {
        // 创建服务器实体
        Server server = new Server();
        server.setId(1L);
        server.setName("test-server");
        server.setHostname("localhost");
        server.setPort(8080);
        server.setBaseWorkDirectory("/tmp");
        server.setSshPort(22);
        server.setSshUsername("root");
        
        // 设置加密密码（不注入passwordEncryptionService）
        String testPassword = "test_password_123";
        String encryptedPassword = passwordEncryptionService.encryptPassword(testPassword);
        server.setSshPasswordEncrypted(encryptedPassword);
        
        // 测试JSON序列化 - 这应该不会触发getSshPassword()方法
        String json = null;
        assertDoesNotThrow(() -> {
            String jsonResult = objectMapper.writeValueAsString(server);
            System.out.println("Server JSON: " + jsonResult);
        }, "Server JSON序列化不应抛出异常");
        
        System.out.println("✅ Server JSON序列化测试通过");
    }

    /**
     * 测试Server实体安全密码获取方法
     */
    @Test
    public void testServerSafePasswordRetrieval() throws Exception {
        Server server = new Server();
        server.setName("test-server");
        server.setHostname("localhost");
        server.setPort(8080);
        server.setBaseWorkDirectory("/tmp");
        
        String testPassword = "safe_password_test";
        String encryptedPassword = passwordEncryptionService.encryptPassword(testPassword);
        server.setSshPasswordEncrypted(encryptedPassword);
        
        // 测试无passwordEncryptionService时的安全获取
        String result1 = server.getSshPasswordSafely();
        assertNull(result1, "未注入加密服务时应返回null");
        
        // 注入加密服务后测试
        server.setPasswordEncryptionService(passwordEncryptionService);
        String result2 = server.getSshPasswordSafely();
        assertEquals(testPassword, result2, "注入加密服务后应正确解密密码");
        
        // 测试直接调用getSshPassword（应抛出异常）
        server.setPasswordEncryptionService(null); // 重置服务
        assertThrows(IllegalStateException.class, () -> {
            server.getSshPassword();
        }, "未注入服务时调用getSshPassword应抛出异常");
        
        System.out.println("✅ Server安全密码获取方法测试通过");
    }

    /**
     * 测试密码脱敏功能
     */
    @Test
    public void testPasswordMasking() throws Exception {
        Server server = new Server();
        server.setName("test-server");
        
        // 测试无密码时的脱敏
        String masked1 = server.getPasswordMasked();
        assertNull(masked1, "无密码时脱敏结果应为null");
        
        // 测试有密码时的脱敏
        String testPassword = "masked_password_test";
        String encryptedPassword = passwordEncryptionService.encryptPassword(testPassword);
        server.setSshPasswordEncrypted(encryptedPassword);
        
        String masked2 = server.getPasswordMasked();
        assertEquals("***已设置***", masked2, "有密码时应显示脱敏文本");
        
        System.out.println("✅ 密码脱敏功能测试通过");
    }

    /**
     * 测试加密服务的异常处理
     */
    @Test
    public void testEncryptionServiceExceptionHandling() {
        // 测试null密码处理
        assertDoesNotThrow(() -> {
            String result1 = passwordEncryptionService.encryptPassword(null);
            assertNull(result1, "null密码加密应返回null");
            
            String result2 = passwordEncryptionService.decryptPassword(null);
            assertNull(result2, "null密码解密应返回null");
        }, "null密码处理不应抛出异常");
        
        // 测试空字符串密码处理
        assertDoesNotThrow(() -> {
            String result1 = passwordEncryptionService.encryptPassword("");
            assertNull(result1, "空字符串密码加密应返回null");
            
            String result2 = passwordEncryptionService.decryptPassword("");
            assertNull(result2, "空字符串密码解密应返回null");
        }, "空字符串密码处理不应抛出异常");
        
        // 测试无效格式密码解密
        assertThrows(Exception.class, () -> {
            passwordEncryptionService.decryptPassword("invalid_format");
        }, "无效格式密码解密应抛出异常");
        
        System.out.println("✅ 加密服务异常处理测试通过");
    }

    /**
     * 测试加密验证功能
     */
    @Test
    public void testEncryptionValidation() throws Exception {
        String testPassword = "validation_test_password";
        
        // 测试加密解密验证
        boolean isValid = passwordEncryptionService.validateEncryptionDecryption(testPassword);
        assertTrue(isValid, "加密解密验证应通过");
        
        // 测试空密码验证
        boolean isValidNull = passwordEncryptionService.validateEncryptionDecryption(null);
        assertTrue(isValidNull, "null密码验证应通过（返回相同的null）");
        
        System.out.println("✅ 加密验证功能测试通过");
    }
}