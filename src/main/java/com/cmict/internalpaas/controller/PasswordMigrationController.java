package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.service.PasswordMigrationService;
import com.cmict.internalpaas.service.PasswordEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 密码迁移管理控制器
 * 提供SSH密码加密迁移的管理接口
 */
@Controller
@RequestMapping("/admin/password-migration")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class PasswordMigrationController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordMigrationController.class);

    @Autowired
    private PasswordMigrationService passwordMigrationService;

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

    /**
     * 密码迁移管理页面
     */
    @GetMapping
    public String migrationPage(Model model) {
        try {
            // 获取当前密码状态验证结果
            PasswordMigrationService.ValidationResult validation = passwordMigrationService.validateAllPasswords();
            model.addAttribute("validation", validation);
            
            // 计算迁移进度
            int total = validation.getTotalCount();
            int encrypted = validation.getValidEncryptedCount();
            double progress = total > 0 ? (double) encrypted / total * 100 : 100;
            model.addAttribute("progress", Math.round(progress));
            
            return "admin/password-migration";
        } catch (Exception e) {
            logger.error("加载密码迁移页面失败", e);
            model.addAttribute("error", "加载密码迁移页面失败: " + e.getMessage());
            return "error";
        }
    }

    /**
     * 获取密码验证状态 (Ajax API)
     */
    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPasswordStatus() {
        try {
            PasswordMigrationService.ValidationResult validation = passwordMigrationService.validateAllPasswords();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("totalCount", validation.getTotalCount());
            result.put("validEncryptedCount", validation.getValidEncryptedCount());
            result.put("invalidEncryptedCount", validation.getInvalidEncryptedCount());
            result.put("plaintextCount", validation.getPlaintextCount());
            result.put("noPasswordCount", validation.getNoPasswordCount());
            result.put("timestamp", validation.getTimestamp());
            
            // 计算进度百分比
            int total = validation.getTotalCount();
            int encrypted = validation.getValidEncryptedCount();
            double progress = total > 0 ? (double) encrypted / total * 100 : 100;
            result.put("progress", Math.round(progress));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("获取密码状态失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "获取密码状态失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 执行密码迁移 (Ajax API)
     */
    @PostMapping("/api/migrate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> migratePasswords() {
        try {
            logger.info("开始执行SSH密码迁移...");
            PasswordMigrationService.MigrationResult result = passwordMigrationService.migrateAllPasswords();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCount", result.getTotalCount());
            response.put("successCount", result.getSuccessCount());
            response.put("alreadyEncryptedCount", result.getAlreadyEncryptedCount());
            response.put("noPasswordCount", result.getNoPasswordCount());
            response.put("failureCount", result.getFailureCount());
            response.put("timestamp", result.getTimestamp());
            
            String message = String.format("密码迁移完成 - 总数: %d, 成功: %d, 已加密: %d, 无密码: %d, 失败: %d",
                result.getTotalCount(), result.getSuccessCount(), result.getAlreadyEncryptedCount(),
                result.getNoPasswordCount(), result.getFailureCount());
            response.put("message", message);
            
            logger.info("SSH密码迁移完成: {}", message);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("执行密码迁移失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "密码迁移失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 迁移指定服务器的密码 (Ajax API)
     */
    @PostMapping("/api/migrate/{serverId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> migrateServerPassword(@PathVariable Long serverId) {
        try {
            logger.info("开始迁移服务器ID {} 的SSH密码", serverId);
            PasswordMigrationService.MigrationStatus status = passwordMigrationService.migrateServerPassword(serverId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("serverId", serverId);
            response.put("status", status.name());
            
            String message;
            switch (status) {
                case SUCCESS:
                    message = "密码迁移成功";
                    break;
                case ALREADY_ENCRYPTED:
                    message = "密码已经加密";
                    break;
                case NO_PASSWORD:
                    message = "未设置密码";
                    break;
                case FAILED:
                    message = "密码迁移失败";
                    break;
                default:
                    message = "未知状态";
                    break;
            }
            response.put("message", message);
            
            logger.info("服务器ID {} 的密码迁移完成: {}", serverId, message);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("迁移服务器ID {} 的密码失败", serverId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("serverId", serverId);
            error.put("error", "密码迁移失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 重新加密所有密码 (Ajax API)
     * 用于密钥轮换等场景
     */
    @PostMapping("/api/reencrypt")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reencryptPasswords() {
        try {
            logger.info("开始重新加密所有SSH密码...");
            PasswordMigrationService.MigrationResult result = passwordMigrationService.reencryptAllPasswords();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCount", result.getTotalCount());
            response.put("successCount", result.getSuccessCount());
            response.put("noPasswordCount", result.getNoPasswordCount());
            response.put("failureCount", result.getFailureCount());
            response.put("timestamp", result.getTimestamp());
            
            String message = String.format("密码重新加密完成 - 总数: %d, 成功: %d, 无密码: %d, 失败: %d",
                result.getTotalCount(), result.getSuccessCount(), result.getNoPasswordCount(), result.getFailureCount());
            response.put("message", message);
            
            logger.info("SSH密码重新加密完成: {}", message);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("重新加密密码失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "重新加密失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 测试密码加密解密功能 (Ajax API)
     */
    @PostMapping("/api/test")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testEncryptionDecryption(@RequestParam String testPassword) {
        try {
            boolean isValid = passwordEncryptionService.validateEncryptionDecryption(testPassword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("valid", isValid);
            response.put("message", isValid ? "加密解密测试通过" : "加密解密测试失败");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("测试密码加密解密功能失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("valid", false);
            error.put("error", "测试失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 验证所有密码状态 (Ajax API)
     */
    @PostMapping("/api/validate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateAllPasswords() {
        try {
            PasswordMigrationService.ValidationResult result = passwordMigrationService.validateAllPasswords();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result);
            response.put("message", "密码验证完成");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("验证所有密码失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "验证失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}