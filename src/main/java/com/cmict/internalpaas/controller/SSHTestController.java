package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.test.SSHConnectionTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * SSH测试控制器
 * 用于测试SSH连接功能
 */
@Controller
@RequestMapping("/test")
public class SSHTestController {

    @Autowired
    private SSHConnectionTester sshTester;

    /**
     * 显示SSH测试页面
     */
    @GetMapping("/ssh")
    public String sshTestPage() {
        return "ssh-test";
    }

    /**
     * 测试SSH连接
     */
    @PostMapping("/ssh/connect")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String host = (String) request.get("host");
            Integer port = (Integer) request.getOrDefault("port", 22);
            String username = (String) request.get("username");
            String password = (String) request.get("password");
            
            if (host == null || username == null || password == null) {
                response.put("success", false);
                response.put("message", "请提供完整的连接信息：host, username, password");
                return ResponseEntity.badRequest().body(response);
            }
            
            boolean connected = sshTester.testConnection(host, port, username, password);
            
            response.put("success", connected);
            response.put("message", connected ? "SSH连接测试成功" : "SSH连接测试失败");
            response.put("host", host);
            response.put("port", port);
            response.put("username", username);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "测试过程中发生错误: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 执行SSH命令
     */
    @PostMapping("/ssh/execute")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> executeCommand(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String host = (String) request.get("host");
            Integer port = (Integer) request.getOrDefault("port", 22);
            String username = (String) request.get("username");
            String password = (String) request.get("password");
            String command = (String) request.get("command");
            
            if (host == null || username == null || password == null || command == null) {
                response.put("success", false);
                response.put("message", "请提供完整的信息：host, username, password, command");
                return ResponseEntity.badRequest().body(response);
            }
            
            String result = sshTester.executeCommand(host, port, username, password, command);
            
            response.put("success", true);
            response.put("result", result);
            response.put("command", command);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "命令执行失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取预设的测试服务器列表
     */
    @GetMapping("/ssh/servers")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTestServers() {
        Map<String, Object> response = new HashMap<>();
        
        // 提供一些常见的测试服务器配置
        @SuppressWarnings("unchecked")
        Map<String, Object>[] servers = new Map[]{
            createServerConfig("Docker SSH服务器", "localhost", 2222, "testuser", "admin123"),
            createServerConfig("WSL Ubuntu", "localhost", 22, "testuser", "admin123"),
            createServerConfig("本地OpenSSH", "127.0.0.1", 22, "testuser", "admin123")
        };
        
        response.put("success", true);
        response.put("servers", servers);
        response.put("note", "这些是建议的测试服务器配置，请根据实际情况修改");
        
        return ResponseEntity.ok(response);
    }
    
    private Map<String, Object> createServerConfig(String name, String host, int port, String username, String password) {
        Map<String, Object> config = new HashMap<>();
        config.put("name", name);
        config.put("host", host);
        config.put("port", port);
        config.put("username", username);
        config.put("password", password);
        return config;
    }
}