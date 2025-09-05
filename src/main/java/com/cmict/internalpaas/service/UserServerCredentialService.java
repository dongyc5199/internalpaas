package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.model.UserServerCredential;
import com.cmict.internalpaas.repository.UserServerCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务器凭据管理服务
 * 负责管理用户在各个服务器上的SSH凭据，包括加密存储和解密使用
 */
@Service
@Transactional
public class UserServerCredentialService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserServerCredentialService.class);
    
    // 简单的密钥（实际生产环境中应该使用配置文件或密钥管理服务）
    private static final String ENCRYPTION_KEY = "MySecretKey12345"; // 16字节密钥
    private static final String ALGORITHM = "AES";
    
    @Autowired
    private UserServerCredentialRepository credentialRepository;
    
    /**
     * 保存或更新用户服务器凭据
     */
    public UserServerCredential saveCredential(User user, Server server, String sshUsername, String plainPassword) {
        try {
            // 加密密码
            String encryptedPassword = encryptPassword(plainPassword);
            
            // 查找是否已存在凭据
            Optional<UserServerCredential> existingCredential = credentialRepository.findByUserIdAndServerId(user.getId(), server.getId());
            
            UserServerCredential credential;
            if (existingCredential.isPresent()) {
                // 更新现有凭据
                credential = existingCredential.get();
                credential.setSshUsername(sshUsername);
                credential.setSshPassword(encryptedPassword);
                logger.info("更新用户 {} 在服务器 {} 上的SSH凭据", user.getUsername(), server.getName());
            } else {
                // 创建新凭据
                credential = new UserServerCredential(user, server, sshUsername, encryptedPassword);
                logger.info("创建用户 {} 在服务器 {} 上的SSH凭据", user.getUsername(), server.getName());
            }
            
            return credentialRepository.save(credential);
        } catch (Exception e) {
            logger.error("保存用户 {} 在服务器 {} 上的凭据失败: {}", user.getUsername(), server.getName(), e.getMessage(), e);
            throw new RuntimeException("保存用户凭据失败", e);
        }
    }
    
    /**
     * 获取用户在指定服务器上的凭据
     */
    public Optional<UserServerCredential> getCredential(Long userId, Long serverId) {
        return credentialRepository.findByUserIdAndServerId(userId, serverId);
    }
    
    /**
     * 获取用户在指定服务器上的凭据（根据用户名）
     */
    public Optional<UserServerCredential> getCredential(String username, Long serverId) {
        return credentialRepository.findByUsernameAndServerId(username, serverId);
    }
    
    /**
     * 获取用户在指定服务器上的明文密码
     */
    public Optional<String> getPlainPassword(Long userId, Long serverId) {
        Optional<UserServerCredential> credential = getCredential(userId, serverId);
        if (credential.isPresent()) {
            try {
                String decryptedPassword = decryptPassword(credential.get().getSshPassword());
                return Optional.of(decryptedPassword);
            } catch (Exception e) {
                logger.error("解密用户 {} 在服务器 {} 上的密码失败: {}", userId, serverId, e.getMessage(), e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
    
    /**
     * 获取用户在指定服务器上的明文密码（根据用户名）
     */
    public Optional<String> getPlainPassword(String username, Long serverId) {
        Optional<UserServerCredential> credential = getCredential(username, serverId);
        if (credential.isPresent()) {
            try {
                String decryptedPassword = decryptPassword(credential.get().getSshPassword());
                return Optional.of(decryptedPassword);
            } catch (Exception e) {
                logger.error("解密用户 {} 在服务器 {} 上的密码失败: {}", username, serverId, e.getMessage(), e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
    
    /**
     * 获取用户的所有服务器凭据
     */
    public List<UserServerCredential> getUserCredentials(Long userId) {
        return credentialRepository.findByUserId(userId);
    }
    
    /**
     * 删除用户在指定服务器上的凭据
     */
    public void deleteCredential(Long userId, Long serverId) {
        Optional<UserServerCredential> credential = getCredential(userId, serverId);
        if (credential.isPresent()) {
            credentialRepository.delete(credential.get());
            logger.info("删除用户 {} 在服务器 {} 上的凭据", userId, serverId);
        }
    }
    
    /**
     * 删除指定服务器的所有用户凭据
     */
    public void deleteServerCredentials(Long serverId) {
        credentialRepository.deleteByServerId(serverId);
        logger.info("删除服务器 {} 的所有用户凭据", serverId);
    }
    
    /**
     * 检查用户是否在指定服务器上有凭据
     */
    public boolean hasCredential(Long userId, Long serverId) {
        return credentialRepository.existsByUserIdAndServerId(userId, serverId);
    }
    
    /**
     * 加密密码
     */
    private String encryptPassword(String plainPassword) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        byte[] encryptedBytes = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    
    /**
     * 解密密码
     */
    private String decryptPassword(String encryptedPassword) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}