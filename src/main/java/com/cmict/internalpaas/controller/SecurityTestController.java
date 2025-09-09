package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.service.PasswordEncryptionService;
import com.cmict.internalpaas.service.PasswordMigrationService;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.SshConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 安全测试控制器
 * 用于验证SSH密码加密和连接功能
 */
@Controller
@RequestMapping("/debug/security")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class SecurityTestController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityTestController.class);

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

    @Autowired
    private PasswordMigrationService passwordMigrationService;

    @Autowired
    private ServerService serverService;

    @Autowired
    private SshConnectionService sshConnectionService;

    /**
     * 安全测试页面
     */
    @GetMapping
    public String securityTestPage(Model model) {
        try {
            // 获取密码验证状态
            PasswordMigrationService.ValidationResult validation = passwordMigrationService.validateAllPasswords();
            model.addAttribute("validation", validation);
            
            // 获取服务器列表
            List<Server> servers = serverService.getAllServers();
            model.addAttribute("servers", servers);
            
            return "debug/security-test";
        } catch (Exception e) {
            logger.error("加载安全测试页面失败", e);
            model.addAttribute("error", "加载页面失败: " + e.getMessage());
            return "error";
        }
    }

    /**
     * 测试密码加密解密 (Ajax API)
     */
    @PostMapping("/api/test-encryption")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testEncryption(@RequestParam String testPassword) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 加密密码
            String encrypted = passwordEncryptionService.encryptPassword(testPassword);
            long encryptTime = System.currentTimeMillis() - startTime;
            
            startTime = System.currentTimeMillis();
            // 解密密码
            String decrypted = passwordEncryptionService.decryptPassword(encrypted);
            long decryptTime = System.currentTimeMillis() - startTime;
            
            // 验证结果
            boolean isValid = testPassword.equals(decrypted);
            boolean isEncrypted = passwordEncryptionService.isPasswordEncrypted(encrypted);
            
            result.put("success", true);
            result.put("originalPassword", testPassword);
            result.put("encryptedPassword", encrypted);
            result.put("decryptedPassword", decrypted);
            result.put("isValid", isValid);
            result.put("isEncrypted", isEncrypted);
            result.put("encryptTime", encryptTime);
            result.put("decryptTime", decryptTime);
            
            logger.info("密码加密解密测试: 有效={}, 加密耗时={}ms, 解密耗时={}ms", isValid, encryptTime, decryptTime);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("密码加密解密测试失败", e);
            result.put("success", false);
            result.put("error", "测试失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 测试服务器密码设置 (Ajax API)
     */
    @PostMapping("/api/test-server-password/{serverId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testServerPassword(@PathVariable Long serverId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Server server = serverService.getServerById(serverId).orElse(null);
            if (server == null) {
                result.put("success", false);
                result.put("error", "服务器不存在");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 检查密码设置
            String encryptedPassword = server.getSshPasswordEncrypted();
            boolean hasPassword = encryptedPassword != null && !encryptedPassword.isEmpty();
            boolean isEncrypted = hasPassword && passwordEncryptionService.isPasswordEncrypted(encryptedPassword);
            boolean canDecrypt = false;
            String decryptError = null;
            
            if (isEncrypted) {
                try {
                    String decrypted = passwordEncryptionService.decryptPassword(encryptedPassword);
                    canDecrypt = decrypted != null && !decrypted.isEmpty();
                } catch (Exception e) {
                    decryptError = e.getMessage();
                }
            }
            
            // 验证SSH服务密码设置
            boolean sshServiceValidation = sshConnectionService.validateServerPassword(server);
            
            result.put("success", true);
            result.put("serverId", serverId);
            result.put("serverName", server.getName());
            result.put("hasPassword", hasPassword);
            result.put("isEncrypted", isEncrypted);
            result.put("canDecrypt", canDecrypt);
            result.put("sshServiceValidation", sshServiceValidation);
            result.put("decryptError", decryptError);
            result.put("maskedPassword", server.getPasswordMasked());
            
            logger.info("服务器 {} 密码测试: 有密码={}, 已加密={}, 可解密={}, SSH服务验证={}", 
                server.getName(), hasPassword, isEncrypted, canDecrypt, sshServiceValidation);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("测试服务器密码失败", e);
            result.put("success", false);
            result.put("error", "测试失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 测试SSH连接（不实际连接）(Ajax API)
     */
    @PostMapping("/api/test-ssh-connection/{serverId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testSSHConnection(@PathVariable Long serverId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Server server = serverService.getServerById(serverId).orElse(null);
            if (server == null) {
                result.put("success", false);
                result.put("error", "服务器不存在");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 测试网络可达性
            boolean isReachable = false;
            try {
                isReachable = sshConnectionService.isReachable(server);
            } catch (Exception e) {
                logger.debug("网络可达性测试异常（在测试环境中这是正常的）: {}", e.getMessage());
            }
            
            // 检查连接参数
            boolean hasHostname = server.getHostname() != null && !server.getHostname().isEmpty();
            boolean hasUsername = server.getSshUsername() != null && !server.getSshUsername().isEmpty();
            boolean hasPassword = sshConnectionService.validateServerPassword(server);
            Integer sshPort = server.getSshPort();
            
            result.put("success", true);
            result.put("serverId", serverId);
            result.put("serverName", server.getName());
            result.put("hostname", server.getHostname());
            result.put("sshPort", sshPort != null ? sshPort : 22);
            result.put("sshUsername", server.getSshUsername());
            result.put("hasHostname", hasHostname);
            result.put("hasUsername", hasUsername);
            result.put("hasPassword", hasPassword);
            result.put("isReachable", isReachable);
            result.put("connectionStatus", server.getConnectionStatus());
            result.put("lastConnectionCheck", server.getLastConnectionCheck());
            
            logger.info("SSH连接测试 - 服务器: {}, 可达: {}, 参数完整: hostname={}, username={}, password={}", 
                server.getName(), isReachable, hasHostname, hasUsername, hasPassword);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("SSH连接测试失败", e);
            result.put("success", false);
            result.put("error", "测试失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 获取系统安全状态概览 (Ajax API)
     */
    @GetMapping("/api/security-overview")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSecurityOverview() {
        try {
            PasswordMigrationService.ValidationResult validation = passwordMigrationService.validateAllPasswords();
            List<Server> servers = serverService.getAllServers();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            // 密码安全统计
            Map<String, Object> passwordStats = new HashMap<>();
            passwordStats.put("totalServers", validation.getTotalCount());
            passwordStats.put("validEncrypted", validation.getValidEncryptedCount());
            passwordStats.put("invalidEncrypted", validation.getInvalidEncryptedCount());
            passwordStats.put("plaintext", validation.getPlaintextCount());
            passwordStats.put("noPassword", validation.getNoPasswordCount());
            
            double securityRatio = validation.getTotalCount() > 0 ? 
                (double) validation.getValidEncryptedCount() / validation.getTotalCount() * 100 : 100;
            passwordStats.put("securityRatio", Math.round(securityRatio * 10) / 10.0);
            
            result.put("passwordStats", passwordStats);
            
            // 连接状态统计
            Map<String, Integer> connectionStats = new HashMap<>();
            connectionStats.put("CONNECTED", 0);
            connectionStats.put("FAILED", 0);
            connectionStats.put("UNKNOWN", 0);
            connectionStats.put("TIMEOUT", 0);
            connectionStats.put("AUTH_FAILED", 0);
            connectionStats.put("MONITORING", 0);
            
            for (Server server : servers) {
                Server.ConnectionStatus status = server.getConnectionStatus();
                if (status != null) {
                    connectionStats.merge(status.name(), 1, Integer::sum);
                }
            }
            
            result.put("connectionStats", connectionStats);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("获取安全状态概览失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "获取概览失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}