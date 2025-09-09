package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.repository.ServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 密码迁移服务
 * 负责将现有的明文密码迁移为加密存储
 */
@Service
public class PasswordMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordMigrationService.class);

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

    /**
     * 应用启动完成后自动执行密码迁移
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("应用启动完成，开始检查SSH密码迁移需求...");
        
        try {
            MigrationResult result = migrateAllPasswords();
            logger.info("SSH密码迁移检查完成 - 总数: {}, 已加密: {}, 新迁移: {}, 失败: {}", 
                result.getTotalCount(), result.getAlreadyEncryptedCount(), 
                result.getSuccessCount(), result.getFailureCount());
                
            if (result.getFailureCount() > 0) {
                logger.warn("存在 {} 个服务器的密码迁移失败，请检查日志", result.getFailureCount());
            }
        } catch (Exception e) {
            logger.error("SSH密码迁移过程中发生异常", e);
        }
    }

    /**
     * 迁移所有服务器的SSH密码
     * 
     * @return 迁移结果统计
     */
    @Transactional
    public MigrationResult migrateAllPasswords() {
        logger.info("开始SSH密码迁移任务...");
        
        MigrationResult result = new MigrationResult();
        List<Server> allServers = serverRepository.findAll();
        result.setTotalCount(allServers.size());
        
        for (Server server : allServers) {
            try {
                MigrationStatus status = migrateServerPassword(server);
                
                switch (status) {
                    case ALREADY_ENCRYPTED:
                        result.incrementAlreadyEncrypted();
                        logger.debug("服务器 {} 的密码已经加密", server.getHostname());
                        break;
                    case SUCCESS:
                        result.incrementSuccess();
                        logger.info("服务器 {} 的密码迁移成功", server.getHostname());
                        break;
                    case NO_PASSWORD:
                        result.incrementNoPassword();
                        logger.debug("服务器 {} 未设置密码", server.getHostname());
                        break;
                    case FAILED:
                        result.incrementFailure();
                        logger.error("服务器 {} 的密码迁移失败", server.getHostname());
                        break;
                }
                
            } catch (Exception e) {
                result.incrementFailure();
                logger.error("迁移服务器 {} 的密码时发生异常", server.getHostname(), e);
            }
        }
        
        logger.info("SSH密码迁移任务完成 - 成功: {}, 已加密: {}, 无密码: {}, 失败: {}", 
            result.getSuccessCount(), result.getAlreadyEncryptedCount(), 
            result.getNoPasswordCount(), result.getFailureCount());
            
        return result;
    }

    /**
     * 迁移单个服务器的SSH密码
     * 
     * @param server 服务器实体
     * @return 迁移状态
     */
    @Transactional
    public MigrationStatus migrateServerPassword(Server server) {
        if (server == null) {
            return MigrationStatus.FAILED;
        }
        
        String currentPassword = server.getSshPasswordEncrypted();
        
        // 检查是否有密码
        if (currentPassword == null || currentPassword.isEmpty()) {
            logger.debug("服务器 {} 未设置SSH密码，跳过迁移", server.getHostname());
            return MigrationStatus.NO_PASSWORD;
        }
        
        // 检查是否已经加密
        if (passwordEncryptionService.isPasswordEncrypted(currentPassword)) {
            logger.debug("服务器 {} 的密码已经加密，跳过迁移", server.getHostname());
            return MigrationStatus.ALREADY_ENCRYPTED;
        }
        
        try {
            // 加密明文密码
            String encryptedPassword = passwordEncryptionService.encryptPassword(currentPassword);
            
            // 验证加密解密是否正确
            String decryptedPassword = passwordEncryptionService.decryptPassword(encryptedPassword);
            if (!currentPassword.equals(decryptedPassword)) {
                logger.error("服务器 {} 的密码加密验证失败", server.getHostname());
                return MigrationStatus.FAILED;
            }
            
            // 更新数据库
            server.setSshPasswordEncrypted(encryptedPassword);
            serverRepository.save(server);
            
            logger.info("服务器 {} 的SSH密码加密迁移成功", server.getHostname());
            return MigrationStatus.SUCCESS;
            
        } catch (Exception e) {
            logger.error("服务器 {} 的密码迁移失败", server.getHostname(), e);
            return MigrationStatus.FAILED;
        }
    }

    /**
     * 手动迁移指定服务器的密码
     * 
     * @param serverId 服务器ID
     * @return 迁移状态
     */
    @Transactional
    public MigrationStatus migrateServerPassword(Long serverId) {
        try {
            Server server = serverRepository.findById(serverId).orElse(null);
            if (server == null) {
                logger.warn("未找到ID为 {} 的服务器", serverId);
                return MigrationStatus.FAILED;
            }
            
            return migrateServerPassword(server);
        } catch (Exception e) {
            logger.error("迁移服务器ID {} 的密码时发生异常", serverId, e);
            return MigrationStatus.FAILED;
        }
    }

    /**
     * 验证所有服务器密码的加密状态
     * 
     * @return 验证结果
     */
    public ValidationResult validateAllPasswords() {
        logger.info("开始验证所有服务器的密码加密状态...");
        
        ValidationResult result = new ValidationResult();
        List<Server> allServers = serverRepository.findAll();
        result.setTotalCount(allServers.size());
        
        for (Server server : allServers) {
            try {
                String password = server.getSshPasswordEncrypted();
                
                if (password == null || password.isEmpty()) {
                    result.incrementNoPassword();
                    continue;
                }
                
                if (passwordEncryptionService.isPasswordEncrypted(password)) {
                    // 验证加密密码能否正确解密
                    try {
                        String decrypted = passwordEncryptionService.decryptPassword(password);
                        if (decrypted != null && !decrypted.isEmpty()) {
                            result.incrementValidEncrypted();
                        } else {
                            result.incrementInvalidEncrypted();
                            logger.warn("服务器 {} 的加密密码解密后为空", server.getHostname());
                        }
                    } catch (Exception e) {
                        result.incrementInvalidEncrypted();
                        logger.error("服务器 {} 的加密密码解密失败", server.getHostname(), e);
                    }
                } else {
                    result.incrementPlaintext();
                    logger.warn("服务器 {} 仍使用明文密码", server.getHostname());
                }
                
            } catch (Exception e) {
                logger.error("验证服务器 {} 的密码状态时发生异常", server.getHostname(), e);
            }
        }
        
        logger.info("密码验证完成 - 总数: {}, 有效加密: {}, 无效加密: {}, 明文: {}, 无密码: {}", 
            result.getTotalCount(), result.getValidEncryptedCount(), 
            result.getInvalidEncryptedCount(), result.getPlaintextCount(), result.getNoPasswordCount());
            
        return result;
    }

    /**
     * 强制重新加密所有密码（用于密钥轮换）
     * 
     * @return 重新加密结果
     */
    @Transactional
    public MigrationResult reencryptAllPasswords() {
        logger.info("开始强制重新加密所有SSH密码...");
        
        MigrationResult result = new MigrationResult();
        List<Server> allServers = serverRepository.findAll();
        result.setTotalCount(allServers.size());
        
        for (Server server : allServers) {
            try {
                String currentPassword = server.getSshPasswordEncrypted();
                
                if (currentPassword == null || currentPassword.isEmpty()) {
                    result.incrementNoPassword();
                    continue;
                }
                
                // 解密当前密码
                String plainPassword;
                if (passwordEncryptionService.isPasswordEncrypted(currentPassword)) {
                    plainPassword = passwordEncryptionService.decryptPassword(currentPassword);
                } else {
                    plainPassword = currentPassword; // 明文密码
                }
                
                // 重新加密
                String newEncryptedPassword = passwordEncryptionService.encryptPassword(plainPassword);
                server.setSshPasswordEncrypted(newEncryptedPassword);
                serverRepository.save(server);
                
                result.incrementSuccess();
                logger.info("服务器 {} 的密码重新加密成功", server.getHostname());
                
            } catch (Exception e) {
                result.incrementFailure();
                logger.error("服务器 {} 的密码重新加密失败", server.getHostname(), e);
            }
        }
        
        logger.info("密码重新加密完成 - 成功: {}, 无密码: {}, 失败: {}", 
            result.getSuccessCount(), result.getNoPasswordCount(), result.getFailureCount());
            
        return result;
    }

    /**
     * 迁移状态枚举
     */
    public enum MigrationStatus {
        SUCCESS,           // 迁移成功
        ALREADY_ENCRYPTED, // 已经加密
        NO_PASSWORD,       // 无密码
        FAILED            // 迁移失败
    }

    /**
     * 迁移结果统计
     */
    public static class MigrationResult {
        private int totalCount = 0;
        private int successCount = 0;
        private int alreadyEncryptedCount = 0;
        private int noPasswordCount = 0;
        private int failureCount = 0;
        private LocalDateTime timestamp = LocalDateTime.now();

        // Getters and setters
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public void incrementSuccess() { this.successCount++; }

        public int getAlreadyEncryptedCount() { return alreadyEncryptedCount; }
        public void setAlreadyEncryptedCount(int alreadyEncryptedCount) { this.alreadyEncryptedCount = alreadyEncryptedCount; }
        public void incrementAlreadyEncrypted() { this.alreadyEncryptedCount++; }

        public int getNoPasswordCount() { return noPasswordCount; }
        public void setNoPasswordCount(int noPasswordCount) { this.noPasswordCount = noPasswordCount; }
        public void incrementNoPassword() { this.noPasswordCount++; }

        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public void incrementFailure() { this.failureCount++; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    /**
     * 验证结果统计
     */
    public static class ValidationResult {
        private int totalCount = 0;
        private int validEncryptedCount = 0;
        private int invalidEncryptedCount = 0;
        private int plaintextCount = 0;
        private int noPasswordCount = 0;
        private LocalDateTime timestamp = LocalDateTime.now();

        // Getters and setters
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

        public int getValidEncryptedCount() { return validEncryptedCount; }
        public void setValidEncryptedCount(int validEncryptedCount) { this.validEncryptedCount = validEncryptedCount; }
        public void incrementValidEncrypted() { this.validEncryptedCount++; }

        public int getInvalidEncryptedCount() { return invalidEncryptedCount; }
        public void setInvalidEncryptedCount(int invalidEncryptedCount) { this.invalidEncryptedCount = invalidEncryptedCount; }
        public void incrementInvalidEncrypted() { this.invalidEncryptedCount++; }

        public int getPlaintextCount() { return plaintextCount; }
        public void setPlaintextCount(int plaintextCount) { this.plaintextCount = plaintextCount; }
        public void incrementPlaintext() { this.plaintextCount++; }

        public int getNoPasswordCount() { return noPasswordCount; }
        public void setNoPasswordCount(int noPasswordCount) { this.noPasswordCount = noPasswordCount; }
        public void incrementNoPassword() { this.noPasswordCount++; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}