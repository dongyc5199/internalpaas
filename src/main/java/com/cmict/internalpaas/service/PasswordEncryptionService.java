package com.cmict.internalpaas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SSH密码加密解密服务
 * 使用AES-256-CBC算法对SSH密码进行加密存储
 */
@Service
public class PasswordEncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncryptionService.class);

    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int IV_LENGTH = 16;

    // 密钥分隔符
    private static final String SEPARATOR = ":";

    // 从配置文件读取主密钥，如果没有配置则使用默认值（生产环境必须配置）
    @Value("${app.security.master-key:DEFAULT_MASTER_KEY_PLEASE_CHANGE_IN_PRODUCTION}")
    private String masterKey;

    /**
     * 加密SSH密码
     * 
     * @param plainPassword 明文密码
     * @return 加密后的密码字符串，格式为 base64(iv):base64(encryptedData)
     * @throws Exception 加密过程中的异常
     */
    public String encryptPassword(String plainPassword) throws Exception {
        if (plainPassword == null || plainPassword.isEmpty()) {
            logger.debug("密码为空，无需加密");
            return null;
        }

        try {
            // 生成密钥
            SecretKey secretKey = generateSecretKey();
            
            // 生成随机IV
            byte[] iv = generateRandomIV();
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // 初始化加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            
            // 执行加密
            byte[] encryptedData = cipher.doFinal(plainPassword.getBytes("UTF-8"));
            
            // 将IV和加密数据编码为Base64并组合
            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedData);
            String result = ivBase64 + SEPARATOR + encryptedBase64;
            
            logger.debug("密码加密成功，输出长度: {}", result.length());
            return result;
            
        } catch (Exception e) {
            logger.error("密码加密失败", e);
            throw new PasswordEncryptionException("密码加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解密SSH密码
     * 
     * @param encryptedPassword 加密的密码字符串，格式为 base64(iv):base64(encryptedData)
     * @return 解密后的明文密码
     * @throws Exception 解密过程中的异常
     */
    public String decryptPassword(String encryptedPassword) throws Exception {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            logger.debug("加密密码为空，无需解密");
            return null;
        }

        // 检查是否为旧的明文密码（兼容性处理）
        if (!encryptedPassword.contains(SEPARATOR)) {
            logger.error("检测到无效的加密格式，无法解密");
            throw new PasswordDecryptionException("加密密码格式无效", null);
        }

        try {
            // 分离IV和加密数据
            String[] parts = encryptedPassword.split(SEPARATOR);
            if (parts.length != 2) {
                throw new IllegalArgumentException("加密密码格式无效");
            }
            
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encryptedData = Base64.getDecoder().decode(parts[1]);
            
            // 生成密钥
            SecretKey secretKey = generateSecretKey();
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // 初始化解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            
            // 执行解密
            byte[] decryptedData = cipher.doFinal(encryptedData);
            String result = new String(decryptedData, "UTF-8");
            
            logger.debug("密码解密成功");
            return result;
            
        } catch (Exception e) {
            logger.error("密码解密失败", e);
            throw new PasswordDecryptionException("密码解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查密码是否已加密
     * 
     * @param password 待检查的密码
     * @return true表示已加密，false表示明文
     */
    public boolean isPasswordEncrypted(String password) {
        return password != null && password.contains(SEPARATOR) && password.split(SEPARATOR).length == 2;
    }

    /**
     * 生成密钥
     * 基于主密钥生成AES密钥
     */
    private SecretKey generateSecretKey() throws Exception {
        // 使用主密钥生成固定的AES密钥
        // 注意：生产环境中应该使用更安全的密钥派生函数(PBKDF2)
        byte[] keyBytes = masterKey.getBytes("UTF-8");
        
        // 如果密钥长度不足32字节，则填充到32字节
        byte[] key = new byte[32]; // 256位密钥
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, key.length));
        
        // 如果原密钥长度不足，用固定字符填充（生产环境应使用更安全的方法）
        for (int i = keyBytes.length; i < key.length; i++) {
            key[i] = (byte) ('A' + (i % 26));
        }
        
        return new SecretKeySpec(key, KEY_ALGORITHM);
    }

    /**
     * 生成随机IV
     */
    private byte[] generateRandomIV() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * 验证密码加密解密的正确性
     * 
     * @param originalPassword 原始密码
     * @return true表示加密解密正常
     */
    public boolean validateEncryptionDecryption(String originalPassword) {
        try {
            String encrypted = encryptPassword(originalPassword);
            String decrypted = decryptPassword(encrypted);
            if (originalPassword == null || originalPassword.isEmpty()) {
                return decrypted == null;
            }
            return originalPassword.equals(decrypted);
        } catch (Exception e) {
            logger.error("密码加密解密验证失败", e);
            return false;
        }
    }

    /**
     * 密码加密异常
     */
    public static class PasswordEncryptionException extends RuntimeException {
        public PasswordEncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 密码解密异常
     */
    public static class PasswordDecryptionException extends RuntimeException {
        public PasswordDecryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}