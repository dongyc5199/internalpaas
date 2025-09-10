package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.RemoteCommandService;
import com.cmict.internalpaas.service.RemoteCommandService.CommandResult;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 远程命令执行控制器
 * 提供REST API用于在远程服务器上执行命令
 */
@RestController
@RequestMapping("/api/command")
public class RemoteCommandController {

    @Autowired
    private RemoteCommandService remoteCommandService;

    @Autowired
    private ServerService serverService;

    @Autowired
    private UserService userService;

    /**
     * 在指定服务器上执行命令
     */
    @PostMapping("/execute/{serverId}")
    public ResponseEntity<CommandResponse> executeCommand(
            @PathVariable Long serverId,
            @RequestBody CommandRequest request) {
        
        try {
            // 获取当前用户
            User currentUser = getCurrentUser();
            
            Server server = serverService.getServerById(serverId)
                    .orElseThrow(() -> new RuntimeException("服务器不存在"));
            
            // 使用带安全验证的命令执行
            CommandResult result = remoteCommandService.executeCommand(server, request.getCommand(), currentUser);
            
            CommandResponse response = new CommandResponse(
                    result.isSuccess(),
                    result.getExitCode(),
                    result.getOutput(),
                    result.getError(),
                    result.getExecutionTime()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new CommandResponse(false, -1, "", e.getMessage(), 0));
        }
    }

    /**
     * 批量执行命令
     */
    @PostMapping("/execute/batch")
    public ResponseEntity<Map<String, CommandResponse>> executeBatchCommands(
            @RequestBody BatchCommandRequest request) {
        
        Map<String, CommandResponse> results = new java.util.HashMap<>();
        
        for (Long serverId : request.getServerIds()) {
            try {
                Server server = serverService.getServerById(serverId)
                        .orElseThrow(() -> new RuntimeException("服务器不存在"));
                
                User currentUser = getCurrentUser();
                CommandResult result = remoteCommandService.executeCommand(server, request.getCommand(), currentUser);
                
                results.put(server.getName(), new CommandResponse(
                        result.isSuccess(),
                        result.getExitCode(),
                        result.getOutput(),
                        result.getError(),
                        result.getExecutionTime()
                ));
                
            } catch (Exception e) {
                results.put("server-" + serverId, new CommandResponse(false, -1, "", e.getMessage(), 0));
            }
        }
        
        return ResponseEntity.ok(results);
    }

    /**
     * 读取远程文件
     */
    @GetMapping("/file/{serverId}")
    public ResponseEntity<CommandResponse> readFile(
            @PathVariable Long serverId,
            @RequestParam String path) {
        
        try {
            Server server = serverService.getServerById(serverId)
                    .orElseThrow(() -> new RuntimeException("服务器不存在"));
            
            CommandResult result = remoteCommandService.readFile(server, path);
            
            CommandResponse response = new CommandResponse(
                    result.isSuccess(),
                    result.getExitCode(),
                    result.getOutput(),
                    result.getError(),
                    result.getExecutionTime()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new CommandResponse(false, -1, "", e.getMessage(), 0));
        }
    }

    /**
     * 写入远程文件
     */
    @PostMapping("/file/{serverId}")
    public ResponseEntity<CommandResponse> writeFile(
            @PathVariable Long serverId,
            @RequestBody WriteFileRequest request) {
        
        try {
            Server server = serverService.getServerById(serverId)
                    .orElseThrow(() -> new RuntimeException("服务器不存在"));
            
            CommandResult result = remoteCommandService.writeFile(server, request.getPath(), request.getContent());
            
            CommandResponse response = new CommandResponse(
                    result.isSuccess(),
                    result.getExitCode(),
                    result.getOutput(),
                    result.getError(),
                    result.getExecutionTime()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new CommandResponse(false, -1, "", e.getMessage(), 0));
        }
    }

    /**
     * 获取常用命令列表
     */
    @GetMapping("/common-commands")
    public ResponseEntity<Map<String, String>> getCommonCommands() {
        Map<String, String> commands = new java.util.HashMap<>();
        commands.put("查看系统信息", "uname -a");
        commands.put("查看磁盘空间", "df -h");
        commands.put("查看内存", "free -h");
        commands.put("查看进程", "ps aux | head -20");
        commands.put("查看端口占用", "netstat -tulpn");
        commands.put("查看系统负载", "uptime");
        commands.put("查看Java进程", "ps aux | grep java");
        commands.put("查看日志", "tail -n 100 /var/log/syslog");
        commands.put("查看网络", "ip addr show");
        commands.put("查看时间", "date");
        
        return ResponseEntity.ok(commands);
    }

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                return userService.findByUsername(username).orElse(null);
            }
        } catch (Exception e) {
            // 记录错误但不中断流程
        }
        return null; // 系统内部调用时可能没有用户上下文
    }

    // 请求和响应类
    public static class CommandRequest {
        private String command;
        private Long timeout = 30000L;

        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public Long getTimeout() { return timeout; }
        public void setTimeout(Long timeout) { this.timeout = timeout; }
    }

    public static class BatchCommandRequest {
        private java.util.List<Long> serverIds;
        private String command;

        public java.util.List<Long> getServerIds() { return serverIds; }
        public void setServerIds(java.util.List<Long> serverIds) { this.serverIds = serverIds; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
    }

    public static class WriteFileRequest {
        private String path;
        private String content;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class CommandResponse {
        private boolean success;
        private int exitCode;
        private String output;
        private String error;
        private long executionTime;

        public CommandResponse(boolean success, int exitCode, String output, String error, long executionTime) {
            this.success = success;
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
            this.executionTime = executionTime;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public int getExitCode() { return exitCode; }
        public String getOutput() { return output; }
        public String getError() { return error; }
        public long getExecutionTime() { return executionTime; }
    }
}