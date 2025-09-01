package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.MonitoringSchedulerService;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private MonitoringSchedulerService schedulerService;

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
        
        return "admin/servers";
    }

    @GetMapping("/servers/new")
    public String newServerForm(Model model) {
        model.addAttribute("server", new Server());
        return "admin/server-form";
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
            // 使用自动检测功能
            Server savedServer = serverService.saveServerWithAutoCheck(server);
            redirectAttributes.addFlashAttribute("successMessage", 
                String.format("服务器 '%s' 已保存，正在后台检测连接状态", savedServer.getName()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "保存失败: " + e.getMessage());
        }
        return "redirect:/admin/servers";
    }
    
    /**
     * 创建服务器API - 用于抽屉提交
     */
    @PostMapping("/api/servers")
    @ResponseBody
    public ResponseEntity<?> createServerApi(@ModelAttribute Server server) {
        try {
            Server savedServer = serverService.saveServerWithAutoCheck(server);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "服务器创建成功");
            response.put("server", savedServer);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/servers/{id}/edit")
    public String editServerForm(@PathVariable Long id, Model model) {
        Server server = serverService.getServerById(id)
            .orElseThrow(() -> new RuntimeException("Server not found"));
        model.addAttribute("server", server);
        return "admin/server-form";
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
    public String updateServer(@PathVariable Long id, @ModelAttribute Server server) {
        serverService.updateServer(id, server);
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
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "服务器更新成功");
            response.put("server", updatedServer);
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

    @GetMapping("/users")
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
    
    // 仅超级管理员可以创建新用户
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        return "admin/user-form";
    }
    
    // 仅超级管理员可以创建新用户
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/users")
    public String createUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        try {
            // 对密码进行加密处理
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            userService.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "用户创建成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "用户创建失败: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
    
    // 仅超级管理员可以编辑用户
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.findAllUsers().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("用户未找到"));
        model.addAttribute("user", user);
        return "admin/user-form";
    }
    
    // 仅超级管理员可以更新用户
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/users/{id}/update")
    public String updateUser(@PathVariable Long id, @ModelAttribute User user, RedirectAttributes redirectAttributes) {
        try {
            User existingUser = userService.findAllUsers().stream()
                    .filter(u -> u.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("用户未找到"));
            
            // 更新用户信息，但不更新密码（除非提供了新密码）
            existingUser.setEmail(user.getEmail());
            existingUser.setWorkDirectory(user.getWorkDirectory());
            existingUser.setRoles(user.getRoles());
            existingUser.setIsFirstLogin(user.getIsFirstLogin());
            
            // 如果提供了新密码，则更新密码
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            
            userService.save(existingUser);
            redirectAttributes.addFlashAttribute("successMessage", "用户更新成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "用户更新失败: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}