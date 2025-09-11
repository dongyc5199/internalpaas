package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.model.ServerStatusTag;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.MonitoringSchedulerService;
import com.cmict.internalpaas.service.RemoteCommandService;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.ServerStatusTagService;
import com.cmict.internalpaas.service.SshConnectionService;
import com.cmict.internalpaas.service.UserServerCredentialService;
import com.cmict.internalpaas.service.UserService;
import com.cmict.internalpaas.repository.UserRepository;

import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")  // 允许超级管理员和管理员访问
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private ServerService serverService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MonitoringSchedulerService schedulerService;
    
    @Autowired
    private RemoteCommandService remoteCommandService;
    
    @Autowired
    private com.cmict.internalpaas.service.ServerUserGroupService serverUserGroupService;
    
    @Autowired
    private SshConnectionService sshConnectionService;
    
    @Autowired
    private UserServerCredentialService credentialService;
    
    @Autowired
    private com.cmict.internalpaas.service.UserServerAccountService userServerAccountService;
    
    @Autowired
    private ServerStatusTagService statusTagService;
    
    
    @Autowired
    private WebSocketController webSocketController;
    
    @Autowired
    private com.cmict.internalpaas.service.ServerStatusTagSchedulerService statusTagSchedulerService;

    @GetMapping("/servers")
    public String serverManagement(Model model) {
        List<Server> servers = serverService.getAllServers();
        model.addAttribute("servers", servers);
        
        // 计算统计数据 - 基于连接状态而不是active字段
        long totalServers = servers.size();
        long activeServers = servers.stream()
            .filter(server -> server.getConnectionStatus() == Server.ConnectionStatus.CONNECTED || 
                             server.getConnectionStatus() == Server.ConnectionStatus.MONITORING)
            .count();
        long inactiveServers = servers.stream()
            .filter(server -> server.getConnectionStatus() == Server.ConnectionStatus.FAILED ||
                             server.getConnectionStatus() == Server.ConnectionStatus.TIMEOUT ||
                             server.getConnectionStatus() == Server.ConnectionStatus.AUTH_FAILED ||
                             server.getConnectionStatus() == Server.ConnectionStatus.UNKNOWN)
            .count();
        long monitoringServers = servers.stream()
            .filter(server -> server.getConnectionStatus() == Server.ConnectionStatus.MONITORING)
            .count();
        
        model.addAttribute("totalServers", totalServers);
        model.addAttribute("activeServers", activeServers);
        model.addAttribute("inactiveServers", inactiveServers);
        model.addAttribute("monitoringServers", monitoringServers);
        
        // 添加监控相关功能的标志
        model.addAttribute("hasHistoryFeature", true);
        model.addAttribute("hasThresholdFeature", true);
        
        return "admin/servers-page";
    }


    @GetMapping("/servers/{id}")
    public String serverDetail(@PathVariable Long id, Model model) {
        Server server = serverService.findById(id)
            .orElseThrow(() -> new RuntimeException("服务器未找到"));
        
        // 使用服务器当前存储的连接状态，避免同步SSH检查导致页面卡住
        Server.ConnectionStatus currentStatus = server.getConnectionStatus() != null ? 
            server.getConnectionStatus() : Server.ConnectionStatus.UNKNOWN;
        String connectionDescription = serverService.getConnectionStatusDescription(currentStatus);
        
        model.addAttribute("server", server);
        model.addAttribute("connectionStatus", currentStatus.name());
        model.addAttribute("connectionDescription", connectionDescription);
        
        return "admin/server-detail";
    }

    @PostMapping("/servers")
    public String saveServer(@ModelAttribute Server server, RedirectAttributes redirectAttributes) {
        try {
            // 保存服务器，不进行自动检测
            Server savedServer = serverService.saveServer(server);
            
            // 异步刷新服务器状态和监控数据（提交到后台任务队列）
            CompletableFuture.runAsync(() -> {
                try {
                    logger.info("开始为新创建的服务器 {} 执行异步状态刷新", savedServer.getName());
                    Server refreshedServer = serverService.checkServerConnectionAndMetrics(savedServer.getId());
                    logger.info("新服务器 {} 异步状态刷新完成，连接状态: {}", 
                        refreshedServer.getName(), refreshedServer.getConnectionStatus());
                    
                    // 通知前端更新服务器状态
                    webSocketController.broadcast("/topic/server-status", Map.of(
                        "type", "SERVER_CREATED_STATUS_UPDATED",
                        "serverId", savedServer.getId(),
                        "serverName", savedServer.getName(),
                        "connectionStatus", refreshedServer.getConnectionStatus().name(),
                        "timestamp", System.currentTimeMillis()
                    ));
                    
                } catch (Exception e) {
                    logger.error("新服务器 {} 异步状态刷新失败: {}", savedServer.getName(), e.getMessage(), e);
                }
            });
            
            redirectAttributes.addFlashAttribute("successMessage", 
                String.format("服务器 '%s' 已保存，状态检查正在后台进行", savedServer.getName()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "保存失败: " + e.getMessage());
        }
        return "redirect:/admin/servers";
    }
    
    /**
     * 获取服务器列表API - 用于前端选择
     */
    @GetMapping("/api/servers")
    @ResponseBody
    public ResponseEntity<?> getServerListApi() {
        try {
            List<Server> servers = serverService.getAllServers();
            return ResponseEntity.ok(servers);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取可用服务器列表API - 用于用户管理中的服务器选择
     */
    @GetMapping("/api/servers/available")
    @ResponseBody
    public ResponseEntity<?> getAvailableServersApi() {
        try {
            // 首先尝试获取活跃服务器
            List<Server> activeServers = serverService.getActiveServers();
            
            // 如果没有活跃服务器，则获取所有服务器
            if (activeServers.isEmpty()) {
                activeServers = serverService.getAllServers();
                logger.warn("没有找到活跃服务器，返回所有服务器列表");
            }
            
            // 转换为前端需要的格式，包含抽屉组件需要的字段
            List<Map<String, Object>> responseData = activeServers.stream()
                .map(server -> {
                    Map<String, Object> serverData = new HashMap<>();
                    serverData.put("id", server.getId());
                    serverData.put("name", server.getName());
                    serverData.put("hostname", server.getHostname());
                    serverData.put("active", server.getActive() != null ? server.getActive() : true);
                    serverData.put("connectionStatus", server.getConnectionStatus() != null ? 
                        server.getConnectionStatus().name() : "UNKNOWN");
                    return serverData;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 创建服务器API - 用于抽屉提交
     */
    @PostMapping("/api/servers")
    @ResponseBody
    public ResponseEntity<?> createServerApi(@ModelAttribute Server server) {
        try {
            // 设置默认端口值（如果未提供）
            if (server.getPort() == null) {
                server.setPort(8080); // 默认应用端口
            }
            
            Server savedServer = serverService.saveServer(server);
            
            // 检查并创建基础工作目录
            Map<String, Object> directoryResult = ensureBaseWorkDirectory(savedServer);
            boolean directoryCreated = (Boolean) directoryResult.get("created");
            
            // 自动初始化默认用户组
            try {
                int initializedCount = serverUserGroupService.initializeDefaultUserGroups(savedServer);
                logger.info("为服务器 {} 成功初始化 {} 个默认用户组", savedServer.getName(), initializedCount);
            } catch (Exception e) {
                logger.error("为服务器 {} 初始化用户组失败: {}", savedServer.getName(), e.getMessage(), e);
                // 不中断服务器创建流程，只记录错误
            }
            
            // 异步刷新服务器状态和监控数据（提交到后台任务队列）
            CompletableFuture.runAsync(() -> {
                try {
                    logger.info("开始为新创建的服务器 {} 执行异步状态刷新", savedServer.getName());
                    Server refreshedServer = serverService.checkServerConnectionAndMetrics(savedServer.getId());
                    logger.info("新服务器 {} 异步状态刷新完成，连接状态: {}", 
                        refreshedServer.getName(), refreshedServer.getConnectionStatus());
                    
                    // 通知前端更新服务器状态
                    webSocketController.broadcast("/topic/server-status", Map.of(
                        "type", "SERVER_CREATED_STATUS_UPDATED",
                        "serverId", savedServer.getId(),
                        "serverName", savedServer.getName(),
                        "connectionStatus", refreshedServer.getConnectionStatus().name(),
                        "timestamp", System.currentTimeMillis()
                    ));
                    
                } catch (Exception e) {
                    logger.error("新服务器 {} 异步状态刷新失败: {}", savedServer.getName(), e.getMessage(), e);
                }
            });
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            
            // 构建成功消息
            StringBuilder message = new StringBuilder("服务器创建成功");
            if (directoryCreated) {
                message.append("并已创建基础工作目录");
            }
            message.append("并已初始化默认用户组");
            
            response.put("message", message.toString());
            response.put("server", savedServer);
            response.put("directoryInfo", directoryResult);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    
    /**
     * 获取服务器数据API - 用于抽屉编辑
     */
    @GetMapping("/servers/{id}/data")
    @ResponseBody
    public ResponseEntity<?> getServerData(@PathVariable Long id) {
        try {
            Server server = serverService.findById(id)
                .orElseThrow(() -> new RuntimeException("服务器未找到"));
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", server.getId());
            data.put("name", server.getName());
            data.put("description", server.getDescription());
            data.put("hostname", server.getHostname());
            data.put("port", server.getPort());
            data.put("baseWorkDirectory", server.getBaseWorkDirectory());
            data.put("sshPort", server.getSshPort());
            data.put("sshUsername", server.getSshUsername());
            data.put("sshPassword", server.getSshPassword());
            data.put("active", server.getActive());
            
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    @PostMapping("/servers/{id}/update")
    public String updateServer(@PathVariable Long id, @ModelAttribute Server server, RedirectAttributes redirectAttributes) {
        try {
            Server updatedServer = serverService.updateServer(id, server);
            
            // 异步刷新服务器状态和监控数据（提交到后台任务队列）
            CompletableFuture.runAsync(() -> {
                try {
                    logger.info("开始为更新后的服务器 {} 执行异步状态刷新", updatedServer.getName());
                    Server refreshedServer = serverService.checkServerConnectionAndMetrics(updatedServer.getId());
                    logger.info("更新后服务器 {} 异步状态刷新完成，连接状态: {}", 
                        refreshedServer.getName(), refreshedServer.getConnectionStatus());
                    
                    // 通知前端更新服务器状态
                    webSocketController.broadcast("/topic/server-status", Map.of(
                        "type", "SERVER_UPDATED_STATUS_UPDATED",
                        "serverId", updatedServer.getId(),
                        "serverName", updatedServer.getName(),
                        "connectionStatus", refreshedServer.getConnectionStatus().name(),
                        "timestamp", System.currentTimeMillis()
                    ));
                    
                } catch (Exception e) {
                    logger.error("更新后服务器 {} 异步状态刷新失败: {}", updatedServer.getName(), e.getMessage(), e);
                }
            });
            
            redirectAttributes.addFlashAttribute("successMessage", 
                String.format("服务器 '%s' 更新成功，状态检查正在后台进行", updatedServer.getName()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "更新失败: " + e.getMessage());
        }
        
        return "redirect:/admin/servers";
    }
    
    /**
     * 更新服务器API - 用于抽屉提交
     */
    @PostMapping("/api/servers/{id}/update")
    @ResponseBody
    public ResponseEntity<?> updateServerApi(@PathVariable Long id, @ModelAttribute Server server) {
        try {
            Server updatedServer = serverService.updateServer(id, server);
            
            // 检查并创建基础工作目录
            Map<String, Object> directoryResult = ensureBaseWorkDirectory(updatedServer);
            boolean directoryCreated = (Boolean) directoryResult.get("created");
            
            // 异步刷新服务器状态和监控数据（提交到后台任务队列）
            CompletableFuture.runAsync(() -> {
                try {
                    logger.info("开始为更新后的服务器 {} 执行异步状态刷新", updatedServer.getName());
                    Server refreshedServer = serverService.checkServerConnectionAndMetrics(updatedServer.getId());
                    logger.info("更新后服务器 {} 异步状态刷新完成，连接状态: {}", 
                        refreshedServer.getName(), refreshedServer.getConnectionStatus());
                    
                    // 通知前端更新服务器状态
                    webSocketController.broadcast("/topic/server-status", Map.of(
                        "type", "SERVER_UPDATED_STATUS_UPDATED",
                        "serverId", updatedServer.getId(),
                        "serverName", updatedServer.getName(),
                        "connectionStatus", refreshedServer.getConnectionStatus().name(),
                        "timestamp", System.currentTimeMillis()
                    ));
                    
                } catch (Exception e) {
                    logger.error("更新后服务器 {} 异步状态刷新失败: {}", updatedServer.getName(), e.getMessage(), e);
                }
            });
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            
            // 构建成功消息
            StringBuilder message = new StringBuilder("服务器更新成功");
            if (directoryCreated) {
                message.append("并已创建基础工作目录");
            }
            
            response.put("message", message.toString());
            response.put("server", updatedServer);
            response.put("directoryInfo", directoryResult);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/servers/{id}/delete")
    public String deleteServer(@PathVariable Long id) {
        serverService.deleteServer(id);
        return "redirect:/admin/servers";
    }
    
    @PostMapping("/servers/{id}/check-connection")
    public String checkServerConnection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Server server = serverService.checkServerConnectionAndMetrics(id);
            String statusDesc = serverService.getConnectionStatusDescription(server.getConnectionStatus());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                String.format("服务器 '%s' 连接状态: %s", server.getName(), statusDesc));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "检查连接失败: " + e.getMessage());
        }
        return "redirect:/admin/servers";
    }
    
    /**
     * 获取服务器监控数据API - 与监控控制器保持一致
     */
    @GetMapping("/servers/{id}/metrics")
    @ResponseBody
    public ResponseEntity<?> getServerMetrics(@PathVariable Long id) {
        try {
            // 首先检查服务器是否存在
            if (!serverService.findById(id).isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "服务器不存在");
                error.put("serverId", String.valueOf(id));
                return ResponseEntity.status(404).body(error);
            }
            
            ServerMetrics metrics = serverService.getServerLatestMetrics(id);
            
            if (metrics == null) {
                // 如果没有数据，尝试刷新
                metrics = serverService.refreshServerMetrics(id);
            }
            
            if (metrics == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "监控数据不可用");
                error.put("serverId", String.valueOf(id));
                return ResponseEntity.status(404).body(error);
            }
            
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("serverId", String.valueOf(id));
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 手动触发健康检查API
     */
    @PostMapping("/servers/trigger-health-check")
    @ResponseBody
    public ResponseEntity<Map<String, String>> triggerHealthCheck() {
        try {
            schedulerService.triggerFullHealthCheck();
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "健康检查已触发");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @PostMapping("/servers/check-all-connections")
    public String checkAllServerConnections(RedirectAttributes redirectAttributes) {
        try {
            serverService.checkAllServerConnectionsAndMetrics();
            redirectAttributes.addFlashAttribute("successMessage", "已启动所有服务器的连接检查和监控数据更新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "批量检查失败: " + e.getMessage());
        }
        return "redirect:/admin/servers";
    }
    
    @GetMapping("/servers/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getServerStatus(@PathVariable Long id) {
        try {
            Server server = serverService.findById(id)
                .orElseThrow(() -> new RuntimeException("服务器未找到"));
            
            Map<String, String> status = new HashMap<>();
            status.put("connectionStatus", server.getConnectionStatus() != null ? 
                server.getConnectionStatus().name() : "UNKNOWN");
            status.put("connectionDescription", serverService.getConnectionStatusDescription(
                server.getConnectionStatus() != null ? server.getConnectionStatus() : Server.ConnectionStatus.UNKNOWN));
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("获取服务器状态失败，ID: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取状态失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/servers/{id}/refresh")
    @ResponseBody
    public ResponseEntity<Map<String, String>> refreshServer(@PathVariable Long id) {
        try {
            // 增加超时保护和更详细的日志
            long startTime = System.currentTimeMillis();
            logger.info("开始刷新服务器状态，ID: {}", id);
            
            Server server = serverService.checkServerConnectionAndMetrics(id);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("服务器状态刷新完成，ID: {}, 耗时: {}ms, 状态: {}", 
                id, duration, server.getConnectionStatus().name());
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", String.format("服务器状态已刷新 (耗时: %dms)", duration));
            response.put("connectionStatus", server.getConnectionStatus().name());
            response.put("duration", String.valueOf(duration));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("刷新服务器状态失败，ID: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取服务器日志页面
     */
    @GetMapping("/servers/{id}/logs")
    public String serverLogs(@PathVariable Long id, Model model) {
        Optional<Server> serverOpt = serverService.getServerById(id);
        if (serverOpt.isEmpty()) {
            model.addAttribute("errorMessage", "服务器不存在");
            return "redirect:/admin/servers";
        }
        
        Server server = serverOpt.get();
        model.addAttribute("server", server);
        
        return "admin/server-logs";
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public String userManagement(Model model) {
        List<User> users = userService.findAllUsers();
        model.addAttribute("users", users);
        
        // 计算统计数据
        long totalUsers = users.size();
        long adminUsers = users.stream().filter(user -> 
            user.getRoles().contains(User.Role.ADMIN) || 
            user.getRoles().contains(User.Role.SUPER_ADMIN)).count();
        long regularUsers = users.stream().filter(user -> 
            user.getRoles().contains(User.Role.USER) && 
            !user.getRoles().contains(User.Role.ADMIN) && 
            !user.getRoles().contains(User.Role.SUPER_ADMIN)).count();
        long firstLoginUsers = users.stream().filter(User::getIsFirstLogin).count();
        // 计算近期活跃用户（7天内登录）
        long recentUsers = users.stream().filter(user -> 
            user.getLastLoginTime() != null && 
            user.getLastLoginTime().isAfter(java.time.LocalDateTime.now().minusDays(7))).count();
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("adminUsers", adminUsers);
        model.addAttribute("regularUsers", regularUsers);
        model.addAttribute("firstLoginUsers", firstLoginUsers);
        model.addAttribute("recentUsers", recentUsers);
        
        return "admin/users";
    }

    // 仅超级管理员可以切换角色
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/users/{id}/toggle-admin")
    public String toggleAdminRole(@PathVariable Long id) {
        userService.toggleAdminRole(id);
        return "redirect:/admin/users";
    }
    
    // 仅超级管理员可以切换角色
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/users/{id}/toggle-super-admin")
    public String toggleSuperAdminRole(@PathVariable Long id) {
        userService.toggleSuperAdminRole(id);
        return "redirect:/admin/users";
    }
    
    // 管理员和超级管理员都可以删除用户，但需要添加额外的检查
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "用户删除成功");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }
    
    /**
     * 删除用户API - AJAX调用
     */
    @DeleteMapping("/users/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteUserApi(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "用户删除成功");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 重置用户密码API - AJAX调用
     */
    @PostMapping("/users/{id}/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetUserPasswordApi(@PathVariable Long id) {
        try {
            // 生成随机密码
            String newPassword = generateRandomPassword();
            
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户未找到"));
            
            // 更新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setIsFirstLogin(true); // 强制首次登录修改密码
            userRepository.save(user);
            
            logger.info("管理员重置了用户 {} 的密码", user.getUsername());
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "密码重置成功，新密码：" + newPassword);
            response.put("newPassword", newPassword);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 生成随机密码
     */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
    
    /**
     * 检查并创建服务器基础工作目录
     * @param server 服务器信息
     * @return 检查和创建结果
     */
    private Map<String, Object> ensureBaseWorkDirectory(Server server) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("created", false);
        
        try {
            String baseWorkDir = server.getBaseWorkDirectory();
            if (baseWorkDir == null || baseWorkDir.trim().isEmpty()) {
                result.put("message", "基础工作目录路径为空，跳过检查");
                result.put("success", true);
                return result;
            }
            
            // 检查目录是否存在
            String checkCommand = String.format("[ -d '%s' ] && echo 'exists' || echo 'not_exists'", baseWorkDir);
            logger.info("检查服务器 {} 基础工作目录是否存在: {}", server.getName(), baseWorkDir);
            
            String checkResult = sshConnectionService.executeCommand(server, checkCommand, 10000);
            
            if ("exists".equals(checkResult.trim())) {
                result.put("success", true);
                result.put("message", "基础工作目录已存在: " + baseWorkDir);
                logger.info("服务器 {} 基础工作目录已存在: {}", server.getName(), baseWorkDir);
            } else {
                // 目录不存在，创建它
                logger.info("服务器 {} 基础工作目录不存在，开始创建: {}", server.getName(), baseWorkDir);
                String createCommand = String.format("mkdir -p '%s' && chmod 755 '%s'", baseWorkDir, baseWorkDir);
                
                sshConnectionService.executeCommand(server, createCommand, 15000);
                
                // 再次检查是否创建成功
                String verifyResult = sshConnectionService.executeCommand(server, checkCommand, 5000);
                if ("exists".equals(verifyResult.trim())) {
                    result.put("success", true);
                    result.put("created", true);
                    result.put("message", "基础工作目录创建成功: " + baseWorkDir);
                    logger.info("服务器 {} 基础工作目录创建成功: {}", server.getName(), baseWorkDir);
                } else {
                    result.put("message", "基础工作目录创建失败，验证时仍不存在");
                    logger.error("服务器 {} 基础工作目录创建失败: {}", server.getName(), baseWorkDir);
                }
            }
            
        } catch (Exception e) {
            result.put("message", "检查基础工作目录时发生错误: " + e.getMessage());
            logger.error("检查服务器 {} 基础工作目录时发生错误: {}", server.getName(), e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 获取用户数据API - 用于抽屉编辑
     */
    @GetMapping("/users/{id}/data")
    @ResponseBody
    @Transactional // 添加事务注解以解决懒加载问题
    public ResponseEntity<?> getUserData(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户未找到"));
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            data.put("workDirectory", user.getWorkDirectory());
            data.put("enabled", !user.getIsAccountLocked());
            
            // 转换角色为字符串数组
            List<String> roleNames = user.getRoles().stream()
                .map(role -> role.name())
                .collect(java.util.stream.Collectors.toList());
            data.put("roles", roleNames);
            
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }
    
    /**
     * 创建用户API - 用于抽屉提交
     */
    @PostMapping("/api/users")
    @ResponseBody
    public ResponseEntity<?> createUserApi(@RequestBody Map<String, Object> userData) {
        try {
            logger.info("收到创建用户请求，数据: {}", userData);
            String username = (String) userData.get("username");
            String email = (String) userData.get("email");
            
            // 检查用户名唯一性
            if (userRepository.findByUsername(username).isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "用户名已存在: " + username);
                return ResponseEntity.badRequest().body(error);
            }
            
            // 检查邮箱唯一性
            if (userRepository.findByEmail(email).isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "邮箱已存在: " + email);
                return ResponseEntity.badRequest().body(error);
            }
            
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setWorkDirectory((String) userData.get("workDirectory"));
            user.setIsAccountLocked(!((Boolean) userData.getOrDefault("enabled", true)));
            
            // 设置密码
            String password = (String) userData.get("password");
            if (password != null && !password.trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(password));
            }
            
            // 设置角色
            List<String> roleNames = (List<String>) userData.get("roles");
            logger.info("收到的角色数据: {}", roleNames);
            if (roleNames != null && !roleNames.isEmpty()) {
                Set<User.Role> roles = new HashSet<>();
                for (String roleName : roleNames) {
                    try {
                        User.Role role = User.Role.valueOf(roleName);
                        roles.add(role);
                        logger.info("添加角色: {}", role);
                    } catch (IllegalArgumentException e) {
                        logger.warn("无效角色: {}", roleName);
                    }
                }
                user.setRoles(roles);
                logger.info("用户设置的角色集合: {}", roles);
            } else {
                logger.warn("没有收到角色数据或角色数据为空");
            }
            
            // 处理服务器分配
            this.handleUserServerAssignment(user, userData);
            
            // 根据可用服务器生成工作目录路径
            this.generateUserWorkDirectory(user);
            
            User savedUser = userService.save(user);
            
            // 自动将用户加入服务器的默认用户组
            autoAssignUserToDefaultGroups(savedUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            
            // 构建详细的成功消息
            StringBuilder message = new StringBuilder("用户创建成功");
            if (savedUser.getAvailableServers() != null && !savedUser.getAvailableServers().isEmpty()) {
                message.append("，已在 ").append(savedUser.getAvailableServers().size()).append(" 台服务器上创建账户和工作目录");
                message.append("，已自动加入对应的默认用户组");
            }
            
            response.put("message", message.toString());
            response.put("user", savedUser);
            response.put("serverCount", savedUser.getAvailableServers() != null ? savedUser.getAvailableServers().size() : 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 更新用户API - 用于抽屉提交
     */
    @PostMapping("/api/users/{id}/update")
    @ResponseBody
    public ResponseEntity<?> updateUserApi(@PathVariable Long id, @RequestBody Map<String, Object> userData) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户未找到"));
            
            // 更新基本信息
            user.setUsername((String) userData.get("username"));
            user.setEmail((String) userData.get("email"));
            user.setIsAccountLocked(!((Boolean) userData.getOrDefault("enabled", true)));
            
            // 更新密码（如果提供）
            String password = (String) userData.get("password");
            if (password != null && !password.trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(password));
            }
            
            // 更新角色
            List<String> roleNames = (List<String>) userData.get("roles");
            if (roleNames != null && !roleNames.isEmpty()) {
                Set<User.Role> roles = new HashSet<>();
                for (String roleName : roleNames) {
                    try {
                        roles.add(User.Role.valueOf(roleName));
                    } catch (IllegalArgumentException e) {
                        // 忽略无效角色
                    }
                }
                user.setRoles(roles);
            }
            
            // 处理服务器分配
            this.handleUserServerAssignment(user, userData);
            
            // 根据可用服务器重新生成工作目录路径
            this.generateUserWorkDirectory(user);
            
            User updatedUser = userService.save(user);
            
            // 重新分配用户到服务器的默认用户组（如果服务器分配发生了变化）
            autoAssignUserToDefaultGroups(updatedUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            
            // 构建详细的成功消息
            StringBuilder message = new StringBuilder("用户更新成功");
            if (updatedUser.getAvailableServers() != null && !updatedUser.getAvailableServers().isEmpty()) {
                message.append("，已在 ").append(updatedUser.getAvailableServers().size()).append(" 台服务器上更新账户和工作目录");
            }
            
            response.put("message", message.toString());
            response.put("user", updatedUser);
            response.put("serverCount", updatedUser.getAvailableServers() != null ? updatedUser.getAvailableServers().size() : 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    
    
    /**
     * 检查用户名可用性API - 用于实时验证
     */
    @GetMapping("/api/users/check-username")
    @ResponseBody
    public ResponseEntity<?> checkUsernameAvailability(@RequestParam String username) {
        try {
            // 检查用户名是否已存在
            boolean exists = userRepository.findByUsername(username).isPresent();
            
            Map<String, Object> response = new HashMap<>();
            response.put("available", !exists);
            response.put("message", exists ? "用户名已存在" : "用户名可用");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 检查邮箱可用性API - 用于实时验证
     */
    @GetMapping("/api/users/check-email")
    @ResponseBody
    public ResponseEntity<?> checkEmailAvailability(@RequestParam String email) {
        try {
            // 检查邮箱是否已存在
            boolean exists = userRepository.findByEmail(email).isPresent();
            
            Map<String, Object> response = new HashMap<>();
            response.put("available", !exists);
            response.put("message", exists ? "邮箱已存在" : "邮箱可用");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取用户已分配的服务器列表API - 用于抽屉编辑
     */
    @GetMapping("/api/users/{id}/servers")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> getUserServersApi(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户未找到"));
            
            // 获取用户已分配的服务器
            Set<Server> userServers = user.getAvailableServers();
            
            // 转换为前端需要的格式
            List<Map<String, Object>> responseData = userServers.stream()
                .map(server -> {
                    Map<String, Object> serverData = new HashMap<>();
                    serverData.put("id", server.getId());
                    serverData.put("name", server.getName());
                    serverData.put("hostname", server.getHostname());
                    serverData.put("active", server.getActive() != null ? server.getActive() : true);
                    return serverData;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    /**
     * 处理用户服务器分配
     */
    private void handleUserServerAssignment(User user, Map<String, Object> userData) {
        // 处理服务器分配
        List<Integer> serverIds = (List<Integer>) userData.get("serverIds");
        logger.info("接收到的服务器ID列表: {}", serverIds);
        if (serverIds != null && !serverIds.isEmpty()) {
            Set<Server> assignedServers = new HashSet<>();
            for (Integer serverId : serverIds) {
                serverService.findById(serverId.longValue()).ifPresent(assignedServers::add);
            }
            user.setAvailableServers(assignedServers);
            
            // 如果没有默认服务器，设置第一个为默认服务器
            if (!assignedServers.isEmpty() && user.getDefaultServer() == null) {
                user.setDefaultServer(assignedServers.iterator().next());
            }
        }
        logger.info("用户 {} 信息更新完成，分配了 {} 个服务器", user.getUsername(), 
            user.getAvailableServers().size());
    }
    
    /**
     * 根据用户的可用服务器生成工作目录路径
     */
    private void generateUserWorkDirectory(User user) {
        String userWorkDirectory;
        
        if (user.getAvailableServers() != null && !user.getAvailableServers().isEmpty()) {
            // 获取第一个服务器的基础工作目录作为主要工作目录
            Server firstServer = user.getAvailableServers().iterator().next();
            String baseWorkDirectory = firstServer.getBaseWorkDirectory();
            
            // 在基础工作目录下创建用户名目录
            userWorkDirectory = baseWorkDirectory + "/" + user.getUsername();
            // 规范化路径，避免双斜杠
            userWorkDirectory = userWorkDirectory.replaceAll("/+", "/");
            
            logger.info("为用户 {} 生成工作目录: {}", user.getUsername(), userWorkDirectory);
        } else {
            // 如果没有可用服务器，使用默认工作目录
            userWorkDirectory = "./workspaces/" + user.getUsername();
            logger.warn("用户 {} 没有可用服务器，使用默认工作目录: {}", user.getUsername(), userWorkDirectory);
        }
        
        user.setWorkDirectory(userWorkDirectory);
        
        // 在所有选中的服务器上创建用户账户和工作目录
        createUserAccountsOnServers(user);
        
        // 本地创建备用工作目录
        createLocalWorkDirectory(user);
    }
    
    /**
     * 在所有选中的服务器上创建用户账户和工作目录
     */
    private void createUserAccountsOnServers(User user) {
        if (user.getAvailableServers() == null || user.getAvailableServers().isEmpty()) {
            logger.warn("用户 {} 没有可用服务器，跳过远程账户创建", user.getUsername());
            return;
        }
        
        for (Server server : user.getAvailableServers()) {
            try {
                logger.info("开始在服务器 {} 上为用户 {} 创建账户和工作目录", server.getName(), user.getUsername());
                
                String username = user.getUsername();
                String userWorkDir = server.getBaseWorkDirectory() + "/" + username;
                userWorkDir = userWorkDir.replaceAll("/+", "/");
                
                // 1. 检查用户是否已存在
                String checkUserCommand = String.format("id %s >/dev/null 2>&1 && echo 'exists' || echo 'not_exists'", username);
                String userCheckResult = sshConnectionService.executeCommand(server, checkUserCommand, 10000);
                
                if ("exists".equals(userCheckResult.trim())) {
                    logger.info("用户 {} 在服务器 {} 上已存在，跳过创建", username, server.getName());
                } else {
                    // 2. 创建用户账户
                    String createUserCommand = String.format("useradd -m -d %s -s /bin/bash %s", userWorkDir, username);
                    sshConnectionService.executeCommand(server, createUserCommand, 15000);
                    logger.info("在服务器 {} 上成功创建用户 {}", server.getName(), username);
                    
                    // 3. 生成高安全性随机密码
                    String randomPassword = generateSecurePassword();
                    String setPasswordCommand = String.format("echo '%s:%s' | chpasswd", username, randomPassword);
                    sshConnectionService.executeCommand(server, setPasswordCommand, 10000);
                    logger.info("为用户 {} 在服务器 {} 上设置了高安全性随机密码", username, server.getName());
                    
                    // 4. 将加密密码保存到数据库中，供SSH自动登录使用
                    credentialService.saveCredential(user, server, username, randomPassword);
                    logger.info("已将用户 {} 在服务器 {} 上的SSH凭据保存到数据库", username, server.getName());
                }
                
                // 5. 确保工作目录存在并设置正确权限
                String ensureDirCommand = String.format(
                    "mkdir -p %s && chown %s:%s %s && chmod 755 %s", 
                    userWorkDir, username, username, userWorkDir, userWorkDir);
                sshConnectionService.executeCommand(server, ensureDirCommand, 15000);
                
                // 6. 验证创建结果
                String verifyCommand = String.format("[ -d %s ] && [ \"$(stat -c %%U %s)\" = \"%s\" ] && echo 'success' || echo 'failed'", 
                    userWorkDir, userWorkDir, username);
                String verifyResult = sshConnectionService.executeCommand(server, verifyCommand, 10000);
                
                if ("success".equals(verifyResult.trim())) {
                    logger.info("在服务器 {} 上成功为用户 {} 创建工作目录: {}", server.getName(), username, userWorkDir);
                } else {
                    logger.error("在服务器 {} 上为用户 {} 创建工作目录失败: {}", server.getName(), username, userWorkDir);
                }
                
            } catch (Exception e) {
                logger.error("在服务器 {} 上为用户 {} 创建账户时发生错误: {}", server.getName(), user.getUsername(), e.getMessage(), e);
                // 继续处理下一个服务器，不中断整个流程
            }
        }
    }
    
    /**
     * 创建本地备用工作目录
     */
    private void createLocalWorkDirectory(User user) {
        String localWorkDir = "./workspaces/" + user.getUsername();
        try {
            java.nio.file.Path workPath = java.nio.file.Paths.get(localWorkDir);
            if (!java.nio.file.Files.exists(workPath)) {
                java.nio.file.Files.createDirectories(workPath);
                logger.info("成功创建本地备用工作目录: {}", localWorkDir);
            } else {
                logger.info("本地备用工作目录已存在: {}", localWorkDir);
            }
        } catch (Exception e) {
            logger.error("创建本地备用工作目录失败: {} - {}", localWorkDir, e.getMessage(), e);
        }
    }
    
    /**
     * 生成高安全性随机密码
     * 密码包含大写字母、小写字母、数字和特殊字符
     * 长度为16位，确保足够的安全性
     */
    private String generateSecurePassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specialChars = "!@#$%^&*()-_=+[]{}|;:,.<>?";
        
        String allChars = upperCase + lowerCase + digits + specialChars;
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder password = new StringBuilder(16);
        
        // 确保密码至少包含每种类型的字符
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));
        
        // 填充剩余的12个字符
        for (int i = 4; i < 16; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // 打乱字符顺序
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        
        return new String(chars);
    }
    
    /**
     * 自动将用户分配到服务器的默认用户组
     * 如果服务器没有默认用户组，将尝试创建一个简化的默认用户组
     */
    private void autoAssignUserToDefaultGroups(User user) {
        logger.info("开始为用户 {} 自动分配默认用户组", user.getUsername());
        
        if (user.getAvailableServers() == null || user.getAvailableServers().isEmpty()) {
            logger.info("用户 {} 没有可用的服务器，跳过用户组分配", user.getUsername());
            return;
        }
        
        for (Server server : user.getAvailableServers()) {
            try {
                logger.info("处理服务器 {} 的用户组分配", server.getName());
                
                // 检查服务器是否有默认用户组
                Optional<com.cmict.internalpaas.model.ServerUserGroup> defaultGroup = 
                    serverUserGroupService.getDefaultUserGroup(server.getId());
                    
                if (defaultGroup.isPresent()) {
                    logger.info("服务器 {} 已有默认用户组: {}", server.getName(), defaultGroup.get().getGroupName());
                    
                    // 创建用户服务器账户并分配到默认用户组
                    try {
                        userServerAccountService.createUserAccount(user, server, defaultGroup.get());
                        logger.info("成功为用户 {} 在服务器 {} 上创建账户并加入用户组 {}", 
                            user.getUsername(), server.getName(), defaultGroup.get().getGroupName());
                    } catch (Exception e) {
                        logger.warn("为用户 {} 在服务器 {} 上创建账户或分配用户组失败: {}", 
                            user.getUsername(), server.getName(), e.getMessage());
                    }
                } else {
                    logger.info("服务器 {} 没有默认用户组，尝试创建简化默认用户组", server.getName());
                    
                    // 检查服务器类型，如果为空则设置为开发环境
                    if (server.getServerType() == null) {
                        server.setServerType(Server.ServerType.DEVELOPMENT);
                        serverService.saveServer(server);
                        logger.info("服务器 {} 类型为空，已设置为开发环境", server.getName());
                    }
                    
                    // 尝试为服务器创建简化的默认用户组
                    try {
                        int createdCount = serverUserGroupService.initializeDefaultUserGroups(server);
                        logger.info("为服务器 {} 成功创建了 {} 个默认用户组", server.getName(), createdCount);
                        
                        // 重新获取默认用户组
                        Optional<com.cmict.internalpaas.model.ServerUserGroup> newDefaultGroup = 
                            serverUserGroupService.getDefaultUserGroup(server.getId());
                        if (newDefaultGroup.isPresent()) {
                            // 创建用户服务器账户并分配到新创建的默认用户组
                            try {
                                userServerAccountService.createUserAccount(user, server, newDefaultGroup.get());
                                logger.info("成功为用户 {} 在服务器 {} 上创建账户并加入新的默认用户组 {}", 
                                    user.getUsername(), server.getName(), newDefaultGroup.get().getGroupName());
                            } catch (Exception createAccountException) {
                                logger.warn("为用户 {} 在服务器 {} 上创建账户或分配用户组失败: {}", 
                                    user.getUsername(), server.getName(), createAccountException.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("为服务器 {} 创建默认用户组失败: {}", server.getName(), e.getMessage());
                    }
                }
                
            } catch (Exception e) {
                logger.error("为用户 {} 在服务器 {} 上分配用户组时出错: {}", 
                    user.getUsername(), server.getName(), e.getMessage(), e);
            }
        }
        
        logger.info("完成为用户 {} 的用户组分配处理", user.getUsername());
    }
    
    /**
     * 获取服务器状态标签API - 用于前端页面动态显示
     */
    @GetMapping("/api/servers/{serverId}/status-tags")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getServerStatusTags(@PathVariable Long serverId) {
        try {
            List<ServerStatusTag> tags = statusTagService.getServerStatusTags(serverId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("serverId", serverId);
            response.put("tags", tags.stream().map(this::convertTagToMap).toArray());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取服务器 {} 状态标签失败: {}", serverId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", "获取状态标签失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有服务器的状态标签API
     */
    @GetMapping("/api/servers/status-tags")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllServersStatusTags() {
        try {
            List<Server> servers = serverService.findAllActive();
            Map<String, Object> serverStatusMap = new HashMap<>();
            
            for (Server server : servers) {
                List<ServerStatusTag> tags = statusTagService.getServerStatusTags(server.getId());
                Map<String, Object> serverStatus = new HashMap<>();
                serverStatus.put("serverId", server.getId());
                serverStatus.put("serverName", server.getName());
                serverStatus.put("tags", tags.stream().map(this::convertTagToMap).toArray());
                serverStatusMap.put(String.valueOf(server.getId()), serverStatus);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("servers", serverStatusMap);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取所有服务器状态标签失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", "获取所有服务器状态标签失败: " + e.getMessage()));
        }
    }
    
    /**
     * 刷新服务器状态标签API
     */
    /**
     * 刷新指定服务器的状态标签（异步处理，立即返回）
     */
    @PostMapping("/api/servers/{serverId}/refresh-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> refreshServerStatus(@PathVariable Long serverId) {
        try {
            // 验证服务器是否存在
            Optional<Server> serverOpt = serverService.findById(serverId);
            if (!serverOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Server server = serverOpt.get();
            
            // 获取当前的状态标签（从数据库读取）
            List<ServerStatusTag> currentTags = statusTagService.getServerStatusTags(serverId);
            
            // 异步执行状态刷新（提交到后台任务队列）
            CompletableFuture.runAsync(() -> {
                try {
                    statusTagService.refreshAllServerTags(serverId);
                    logger.info("✅ 异步刷新服务器 {} 状态完成", server.getName());
                    
                    // 通知前端更新
                    webSocketController.broadcast("/topic/server-status", Map.of(
                        "type", "SERVER_STATUS_REFRESHED",
                        "serverId", serverId,
                        "serverName", server.getName(),
                        "timestamp", System.currentTimeMillis()
                    ));
                    
                } catch (Exception e) {
                    logger.error("❌ 异步刷新服务器 {} 状态失败: {}", server.getName(), e.getMessage(), e);
                }
            });
            
            // 立即返回当前状态，不等待刷新完成
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("serverId", serverId);
            response.put("serverName", server.getName());
            response.put("tags", currentTags.stream().map(this::convertTagToMap).toArray());
            response.put("message", "状态刷新已提交，将在后台异步执行");
            response.put("refreshType", "async");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("🚀 服务器 {} 状态刷新任务已提交到后台执行", server.getName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("提交服务器 {} 状态刷新任务失败: {}", serverId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", "提交刷新任务失败: " + e.getMessage()));
        }
    }

    /**
     * 获取服务器状态标签调度服务统计信息
     */
    @GetMapping("/api/servers/status-scheduler/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSchedulerStatistics() {
        try {
            Map<String, Object> statistics = statusTagSchedulerService.getSchedulerStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("scheduler", statistics);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取调度服务统计信息失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", "获取统计信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除所有监控异常标签
     */
    @PostMapping("/api/servers/cleanup-monitoring-tags")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cleanupMonitoringTags() {
        try {
            statusTagService.deleteAllMonitoringTags();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "监控异常标签已删除");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("删除监控异常标签失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", "删除监控异常标签失败: " + e.getMessage()));
        }
    }
    
    /**
     * AJAX API: 获取工作台内容片段
     */
    @GetMapping("/api/dashboard-content")
    @ResponseBody
    public ResponseEntity<String> getDashboardContent(Model model) {
        try {
            // 获取统计数据
            List<Server> servers = serverService.getAllServers();
            List<User> users = userService.findAllUsers();
            
            long totalServers = servers.size();
            long activeServers = servers.stream()
                .filter(server -> server.getConnectionStatus() == Server.ConnectionStatus.CONNECTED || 
                                 server.getConnectionStatus() == Server.ConnectionStatus.MONITORING)
                .count();
            long totalUsers = users.size();
            long adminUsers = users.stream()
                .filter(user -> user.getRoles() != null && 
                               user.getRoles().stream().anyMatch(role -> 
                                   role.name().contains("ADMIN") || role.name().contains("SUPER_ADMIN")))
                .count();
            long regularUsers = totalUsers - adminUsers;
            
            // 模拟系统监控数据
            double cpuUsage = Math.random() * 50 + 20; // 20-70%
            double memoryUsage = Math.random() * 40 + 30; // 30-70%
            
            StringBuilder content = new StringBuilder();
            content.append("<div class=\"content-header\">\n");
            content.append("    <div class=\"page-title-group\">\n");
            content.append("        <h1 class=\"page-title\"><i class=\"fas fa-tachometer-alt\"></i> 管理员工作台</h1>\n");
            content.append("    </div>\n");
            content.append("</div>\n\n");
            
            // 统计面板
            content.append("<section class=\"modern-stats-grid\">\n");
            content.append("    <div class=\"modern-stat-card servers\">\n");
            content.append("        <div class=\"stat-header\">\n");
            content.append("            <div class=\"stat-icon-wrapper servers\"><i class=\"fas fa-server\"></i></div>\n");
            content.append("            <div class=\"stat-trend positive\">+</div>\n");
            content.append("        </div>\n");
            content.append("        <div class=\"stat-body\">\n");
            content.append("            <div class=\"stat-number\">").append(totalServers).append("</div>\n");
            content.append("            <div class=\"stat-label\">服务器总数</div>\n");
            content.append("            <div class=\"stat-sublabel\">活跃服务器: <span>").append(activeServers).append("</span></div>\n");
            content.append("        </div>\n");
            content.append("    </div>\n");
            
            content.append("    <div class=\"modern-stat-card users\">\n");
            content.append("        <div class=\"stat-header\">\n");
            content.append("            <div class=\"stat-icon-wrapper users\"><i class=\"fas fa-users\"></i></div>\n");
            content.append("            <div class=\"stat-trend neutral\">~</div>\n");
            content.append("        </div>\n");
            content.append("        <div class=\"stat-body\">\n");
            content.append("            <div class=\"stat-number\">").append(totalUsers).append("</div>\n");
            content.append("            <div class=\"stat-label\">用户总数</div>\n");
            content.append("            <div class=\"stat-sublabel\">管理员: <span>").append(adminUsers)
                      .append("</span> | 普通用户: <span>").append(regularUsers).append("</span></div>\n");
            content.append("        </div>\n");
            content.append("    </div>\n");
            
            content.append("    <div class=\"modern-stat-card monitoring\">\n");
            content.append("        <div class=\"stat-header\">\n");
            content.append("            <div class=\"stat-icon-wrapper monitoring\"><i class=\"fas fa-microchip\"></i></div>\n");
            content.append("            <div class=\"stat-trend neutral\">~</div>\n");
            content.append("        </div>\n");
            content.append("        <div class=\"stat-body\">\n");
            content.append("            <div class=\"stat-number\">").append(String.format("%.1f", cpuUsage)).append("%</div>\n");
            content.append("            <div class=\"stat-label\">CPU使用率</div>\n");
            content.append("            <div class=\"stat-sublabel\">内存: <span>").append(String.format("%.1f", memoryUsage)).append("%</span></div>\n");
            content.append("        </div>\n");
            content.append("    </div>\n");
            
            content.append("    <div class=\"modern-stat-card activity\">\n");
            content.append("        <div class=\"stat-header\">\n");
            content.append("            <div class=\"stat-icon-wrapper activity\"><i class=\"fas fa-chart-line\"></i></div>\n");
            content.append("            <div class=\"stat-trend positive\">↗</div>\n");
            content.append("        </div>\n");
            content.append("        <div class=\"stat-body\">\n");
            content.append("            <div class=\"stat-number\">").append(totalUsers).append("</div>\n");
            content.append("            <div class=\"stat-label\">系统活跃度</div>\n");
            content.append("            <div class=\"stat-sublabel\">今日登录用户数</div>\n");
            content.append("        </div>\n");
            content.append("    </div>\n");
            content.append("</section>\n\n");
            
            // 快捷操作区
            content.append("<section class=\"modern-actions-grid\">\n");
            content.append("    <a href=\"javascript:void(0)\" onclick=\"loadContent('servers')\" class=\"modern-action-card servers\">\n");
            content.append("        <div class=\"action-card-content\">\n");
            content.append("            <div class=\"action-icon-wrapper\"><i class=\"fas fa-server\"></i></div>\n");
            content.append("            <h3 class=\"action-title\">服务器管理</h3>\n");
            content.append("            <p class=\"action-description\">管理和监控所有服务器的状态</p>\n");
            content.append("        </div>\n");
            content.append("    </a>\n");
            content.append("    <a href=\"javascript:void(0)\" onclick=\"loadContent('users')\" class=\"modern-action-card users\">\n");
            content.append("        <div class=\"action-card-content\">\n");
            content.append("            <div class=\"action-icon-wrapper\"><i class=\"fas fa-users\"></i></div>\n");
            content.append("            <h3 class=\"action-title\">用户管理</h3>\n");
            content.append("            <p class=\"action-description\">管理用户账户和权限设置</p>\n");
            content.append("        </div>\n");
            content.append("    </a>\n");
            content.append("    <a href=\"javascript:void(0)\" onclick=\"loadContent('applications')\" class=\"modern-action-card servers\">\n");
            content.append("        <div class=\"action-card-content\">\n");
            content.append("            <div class=\"action-icon-wrapper\"><i class=\"fas fa-rocket\"></i></div>\n");
            content.append("            <h3 class=\"action-title\">应用管理</h3>\n");
            content.append("            <p class=\"action-description\">管理和监控应用程序</p>\n");
            content.append("        </div>\n");
            content.append("    </a>\n");
            content.append("    <a href=\"javascript:void(0)\" onclick=\"loadContent('monitoring')\" class=\"modern-action-card users\">\n");
            content.append("        <div class=\"action-card-content\">\n");
            content.append("            <div class=\"action-icon-wrapper\"><i class=\"fas fa-chart-line\"></i></div>\n");
            content.append("            <h3 class=\"action-title\">系统监控</h3>\n");
            content.append("            <p class=\"action-description\">查看系统运行状态和性能指标</p>\n");
            content.append("        </div>\n");
            content.append("    </a>\n");
            content.append("</section>\n");
            
            return ResponseEntity.ok(content.toString());
            
        } catch (Exception e) {
            logger.error("获取工作台内容失败", e);
            String errorContent = "<div class=\"alert alert-danger\"><i class=\"fas fa-exclamation-triangle\"></i> 加载工作台内容失败: " + e.getMessage() + "</div>";
            return ResponseEntity.status(500).body(errorContent);
        }
    }
    

    /**
     * AJAX API: 获取服务器管理内容片段
     */
    @GetMapping("/api/servers-content")
    public String getServersContent(Model model) {
        List<Server> servers = serverService.getAllServers();
        model.addAttribute("servers", servers);
        
        // 统计活跃和非活跃服务器
        long activeCount = servers.stream()
            .filter(server -> server.getConnectionStatus() != null && 
                    (server.getConnectionStatus() == Server.ConnectionStatus.CONNECTED || 
                     server.getConnectionStatus() == Server.ConnectionStatus.MONITORING))
            .count();
        long inactiveCount = servers.size() - activeCount;
        
        model.addAttribute("totalServers", servers.size());
        model.addAttribute("activeServers", activeCount);
        model.addAttribute("inactiveServers", inactiveCount);
        model.addAttribute("monitoringServers", activeCount); // 简化处理
        
        // 返回服务器管理内容片段
        return "fragments/servers-fragment :: servers-content";
    }
    
    /**
     * AJAX API: 获取用户管理内容片段
     */
    @GetMapping("/api/users-content")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String getUsersContent(Model model) {
        List<User> users = userService.findAllUsers();
        model.addAttribute("users", users);
        
        // 计算统计数据
        long adminCount = users.stream().filter(u -> u.getRoles().contains(User.Role.ADMIN) || u.getRoles().contains(User.Role.SUPER_ADMIN)).count();
        long developerCount = users.stream().filter(u -> u.getRoles().contains(User.Role.USER)).count();
        long activeCount = users.size(); // 简化处理，所有用户都是活跃的
        
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("developerCount", developerCount);
        model.addAttribute("activeCount", activeCount);
        
        // 返回用户管理内容片段
        return "admin/users :: users-content";
    }

    /**
     * 转换标签为前端所需的Map格式
     */
    private Map<String, Object> convertTagToMap(ServerStatusTag tag) {
        Map<String, Object> tagMap = new HashMap<>();
        tagMap.put("id", tag.getId());
        tagMap.put("tagType", tag.getTagType());
        tagMap.put("status", tag.getStatus());
        tagMap.put("displayText", tag.getDisplayText());
        tagMap.put("value", tag.getValue());
        tagMap.put("colorScheme", tag.getColorScheme());
        tagMap.put("priority", tag.getPriority());
        tagMap.put("details", tag.getDetails());
        tagMap.put("isAlert", tag.isAlert());
        tagMap.put("lastUpdated", tag.getLastUpdated());
        return tagMap;
    }
}