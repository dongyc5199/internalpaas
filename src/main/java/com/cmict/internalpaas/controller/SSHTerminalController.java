package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.SSHSession;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.repository.SSHSessionRepository;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.SSHTerminalService;
import com.cmict.internalpaas.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/terminal")
public class SSHTerminalController {

    private static final Logger logger = LoggerFactory.getLogger(SSHTerminalController.class);

    @Autowired
    private ServerService serverService;
    
    @Autowired
    private SSHTerminalService sshTerminalService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SSHSessionRepository sshSessionRepository;
    
    /**
     * SSH终端主页面 - 重定向到React应用
     */
    @GetMapping
    public String terminalIndex(Model model) {
        logger.info("重定向到React SSH终端页面");
        return "redirect:/app/terminal";
    }
    
    /**
     * 连接到指定服务器的终端页面 - 重定向到React应用
     */
    @GetMapping("/connect/{serverId}")
    public String connectToServer(@PathVariable Long serverId, Model model) {
        logger.info("重定向到React终端页面，服务器ID: {}", serverId);
        // React应用会自动处理serverId参数
        return "redirect:/app/terminal?serverId=" + serverId;
    }
    
    /**
     * 多标签页终端管理页面 - 重定向到React应用
     */
    @GetMapping("/manager")
    public String terminalManager(Model model) {
        logger.info("重定向到React终端管理器");
        return "redirect:/app/terminal";
    }
    
    /**
     * 终端 + AI 助手 Demo 页面 - 重定向到React应用（AI功能已集成）
     */
    @GetMapping("/ai-demo")
    public String aiAssistDemo(Model model) {
        logger.info("重定向到React终端（AI功能已集成）");
        return "redirect:/app/terminal";
    }

    /**
     * AI 面板原型页面 - 重定向到React应用（AI功能已集成）
     */
    @GetMapping("/ai-panel")
    public String aiPanel(Model model) {
        logger.info("重定向到React终端（AI功能已集成）");
        return "redirect:/app/terminal";
    }

    /**
     * AI 功能管理（模型配置）页面 - 重定向到React应用（AI功能已集成）
     */
    @GetMapping("/ai-models")
    public String aiModels(Model model) {
        logger.info("重定向到React终端（AI功能已集成）");
        return "redirect:/app/terminal";
    }
    
    /**
     * 获取服务器列表API
     */
    @GetMapping("/api/servers")
    @ResponseBody
    public ResponseEntity<List<Server>> getServers() {
        try {
            List<Server> servers = serverService.getActiveServers();
            return ResponseEntity.ok(servers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取当前用户的活跃SSH会话
     */
    @GetMapping("/api/sessions")
    @ResponseBody
    public ResponseEntity<List<SSHSession>> getActiveSessions() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            List<SSHSession> sessions = sshSessionRepository
                .findByUserIdAndIsActiveTrueOrderByStartTimeDesc(user.getId());
            
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 检查SSH会话状态
     */
    @GetMapping("/api/session/{sessionId}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSessionStatus(@PathVariable String sessionId) {
        try {
            boolean isActive = sshTerminalService.isSessionActive(sessionId);
            
            Map<String, Object> status = new HashMap<>();
            status.put("sessionId", sessionId);
            status.put("active", isActive);
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 关闭SSH会话
     */
    @DeleteMapping("/api/session/{sessionId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> closeSession(@PathVariable String sessionId) {
        try {
            sshTerminalService.closeSession(sessionId);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "会话已关闭");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取活跃会话统计
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSessionStats() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            long activeSessionCount = sshSessionRepository.countByUserIdAndIsActiveTrue(user.getId());
            int memorySessionCount = sshTerminalService.getActiveSessionCount();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("activeSessionCount", activeSessionCount);
            stats.put("memorySessionCount", memorySessionCount);
            stats.put("username", user.getUsername());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 测试服务器连接
     */
    @PostMapping("/api/test-connection/{serverId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long serverId) {
        try {
            Server server = serverService.findById(serverId)
                .orElseThrow(() -> new RuntimeException("服务器不存在"));
            
            // 检查连接状态
            server = serverService.checkServerConnectionAndMetrics(serverId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("serverId", serverId);
            result.put("serverName", server.getName());
            result.put("connectionStatus", server.getConnectionStatus());
            result.put("connectionDescription", server.getConnectionStatus().getDescription());
            result.put("lastCheck", server.getLastConnectionCheck());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
