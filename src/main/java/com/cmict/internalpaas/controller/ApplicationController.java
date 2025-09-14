package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.ApplicationService;
import com.cmict.internalpaas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/apps")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 显示应用管理页面
     */
    @GetMapping
    public String applicationsPage(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }
        
        User user = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        List<Application> applications = applicationService.getUserApplications(user);
        model.addAttribute("applications", applications);
        model.addAttribute("username", authentication.getName());
        
        // 获取当前用户的工作目录
        String workDirectory = applicationService.getUserWorkDirectory(user);
        model.addAttribute("userWorkDirectory", workDirectory);
        
        return "applications";
    }
    
    /**
     * 上传应用
     */
    @PostMapping("/upload")
    public String uploadApplication(@RequestParam("file") MultipartFile file,
                                   @RequestParam("appName") String appName,
                                   @RequestParam(value = "jvmOptions", required = false) String jvmOptions,
                                   @RequestParam(value = "programArgs", required = false) String programArgs,
                                   @RequestParam(value = "envVars", required = false) String envVars,
                                   @RequestParam(value = "enableJmx", required = false) Boolean enableJmx,
                                   @RequestParam(value = "jmxPort", required = false) Integer jmxPort,
                                   Authentication authentication,
                                   Model model) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
            if (user.getWorkDirectory() == null) {
                model.addAttribute("error", "请先配置工作目录");
                return "redirect:/initial-config";
            }
            
            // 上传应用并设置配置
            Application app = applicationService.uploadApplication(user, file, appName);
            
            // 设置额外的配置参数
            if (jvmOptions != null && !jvmOptions.trim().isEmpty()) {
                app.setJvmOptions(jvmOptions.trim());
            }
            if (programArgs != null && !programArgs.trim().isEmpty()) {
                app.setProgramArguments(programArgs.trim());
            }
            if (envVars != null && !envVars.trim().isEmpty()) {
                app.setEnvironmentVariables(envVars.trim());
            }
            if (enableJmx != null && enableJmx) {
                app.setEnableJmx(true);
                if (jmxPort != null) {
                    app.setJmxPort(jmxPort);
                }
            } else {
                app.setEnableJmx(false);
            }
            
            // 保存更新后的应用配置
            applicationService.saveApplication(app);
            
            return "redirect:/apps";
            
        } catch (IOException e) {
            model.addAttribute("error", "文件上传失败: " + e.getMessage());
            return "applications";
        } catch (Exception e) {
            model.addAttribute("error", "应用上传失败: " + e.getMessage());
            return "applications";
        }
    }
    
    /**
     * 启动应用
     */
    @PostMapping("/{id}/start")
    public String startApplication(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            applicationService.startApplication(id);
            model.addAttribute("success", "应用启动成功");
        } catch (Exception e) {
            model.addAttribute("error", "应用启动失败: " + e.getMessage());
        }
        return "redirect:/apps";
    }
    
    /**
     * 停止应用
     */
    @PostMapping("/{id}/stop")
    public String stopApplication(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            applicationService.stopApplication(id);
            model.addAttribute("success", "应用停止成功");
        } catch (Exception e) {
            model.addAttribute("error", "应用停止失败: " + e.getMessage());
        }
        return "redirect:/apps";
    }
    
    /**
     * 重启应用
     */
    @PostMapping("/{id}/restart")
    public String restartApplication(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户未找到"));
            
            // 检查应用是否存在
            Application app = applicationService.getApplicationById(id);
            if (app == null) {
                model.addAttribute("error", "应用不存在");
                return "redirect:/apps";
            }
            
            // 检查权限 - 确保用户只能重启自己的应用
            if (!app.getUser().getId().equals(user.getId())) {
                model.addAttribute("error", "权限不足：您只能重启自己的应用");
                return "redirect:/apps";
            }
            
            // 执行重启操作
            Application restartedApp = applicationService.restartApplication(id);
            
            if ("RUNNING".equals(restartedApp.getStatus())) {
                model.addAttribute("success", 
                    String.format("应用 '%s' 重启成功 (端口: %d, 调试端口: %d)", 
                        restartedApp.getName(), 
                        restartedApp.getPort(), 
                        restartedApp.getDebugPort()));
            } else {
                model.addAttribute("warning", 
                    String.format("应用 '%s' 重启完成，但状态为: %s", 
                        restartedApp.getName(), 
                        restartedApp.getStatus()));
            }
            
        } catch (IllegalStateException e) {
            model.addAttribute("error", "应用状态错误: " + e.getMessage());
        } catch (IOException e) {
            model.addAttribute("error", "重启过程中发生IO错误: " + e.getMessage());
        } catch (RuntimeException e) {
            model.addAttribute("error", "重启失败: " + e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "重启过程中发生未知错误: " + e.getMessage());
        }
        
        return "redirect:/apps";
    }
    
    /**
     * 删除应用
     */
    @PostMapping("/{id}/delete")
    public String deleteApplication(@PathVariable Long id, Authentication authentication) throws IOException {
        applicationService.deleteApplication(id);
        return "redirect:/apps";
    }
    
    /**
     * 显示应用详情页面
     */
    @GetMapping("/{id}")
    public String applicationDetailPage(@PathVariable Long id, Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }
        
        User user = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        Application application = applicationService.getApplicationById(id);
        if (application == null) {
            model.addAttribute("error", "应用不存在");
            return "redirect:/apps";
        }
        
        // 检查权限 - 确保用户只能查看自己的应用
        if (!application.getUser().getId().equals(user.getId())) {
            model.addAttribute("error", "权限不足：您只能查看自己的应用");
            return "redirect:/apps";
        }
        
        model.addAttribute("application", application);
        model.addAttribute("username", authentication.getName());
        
        return "application-detail";
    }
    
    /**
     * 更新应用配置
     */
    @PostMapping("/{id}/config")
    public ResponseEntity<String> updateApplicationConfig(@PathVariable Long id,
                                                        @RequestParam(value = "jvmOptions", required = false) String jvmOptions,
                                                        @RequestParam(value = "programArgs", required = false) String programArgs,
                                                        @RequestParam(value = "envVars", required = false) String envVars,
                                                        @RequestParam(value = "enableJmx", required = false) Boolean enableJmx,
                                                        Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
            Application application = applicationService.getApplicationById(id);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查权限
            if (!application.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body("权限不足");
            }
            
            // 更新配置
            if (jvmOptions != null) {
                application.setJvmOptions(jvmOptions.trim().isEmpty() ? null : jvmOptions.trim());
            }
            if (programArgs != null) {
                application.setProgramArguments(programArgs.trim().isEmpty() ? null : programArgs.trim());
            }
            if (envVars != null) {
                application.setEnvironmentVariables(envVars.trim().isEmpty() ? null : envVars.trim());
            }
            if (enableJmx != null) {
                application.setEnableJmx(enableJmx);
            }
            
            applicationService.saveApplication(application);
            return ResponseEntity.ok("配置更新成功");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("配置更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取应用监控指标
     */
    @GetMapping("/{id}/metrics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getApplicationMetrics(@PathVariable Long id, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
            Application application = applicationService.getApplicationById(id);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查权限
            if (!application.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
            
            Map<String, Object> metrics = new HashMap<>();
            
            if ("RUNNING".equals(application.getStatus()) && application.getProcessId() != null) {
                try {
                    // 这里可以集成实际的监控数据获取逻辑
                    // 暂时返回模拟数据
                    metrics.put("cpuUsage", Math.random() * 30 + 10); // 10-40% CPU
                    metrics.put("memoryUsage", Math.random() * 20 + 40); // 40-60% Memory
                    metrics.put("diskUsage", Math.random() * 10 + 20); // 20-30% Disk
                    
                    // 计算运行时间
                    if (application.getLastStartedAt() != null) {
                        long uptimeMinutes = java.time.Duration.between(application.getLastStartedAt(), LocalDateTime.now()).toMinutes();
                        metrics.put("uptime", formatUptime(uptimeMinutes));
                    }
                    
                } catch (Exception e) {
                    // 获取指标失败，返回默认值
                    metrics.put("cpuUsage", 0.0);
                    metrics.put("memoryUsage", 0.0);
                    metrics.put("diskUsage", 0.0);
                    metrics.put("uptime", "未知");
                }
            } else {
                metrics.put("cpuUsage", 0.0);
                metrics.put("memoryUsage", 0.0);
                metrics.put("diskUsage", 0.0);
                metrics.put("uptime", "应用未运行");
            }
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取应用日志
     */
    @GetMapping("/{id}/logs")
    @ResponseBody
    public ResponseEntity<String> getApplicationLogs(@PathVariable Long id,
                                                    @RequestParam(value = "lines", defaultValue = "100") int lines,
                                                    Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
            Application application = applicationService.getApplicationById(id);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查权限
            if (!application.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
            
            String logs = "";
            if (application.getLogFilePath() != null && Files.exists(Paths.get(application.getLogFilePath()))) {
                try {
                    List<String> logLines = Files.readAllLines(Paths.get(application.getLogFilePath()));
                    int startIndex = Math.max(0, logLines.size() - lines);
                    logs = String.join("\n", logLines.subList(startIndex, logLines.size()));
                } catch (Exception e) {
                    logs = "读取日志文件失败: " + e.getMessage();
                }
            } else {
                logs = "日志文件不存在或应用未启动";
            }
            
            return ResponseEntity.ok(logs);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("获取日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载应用日志
     */
    @GetMapping("/{id}/logs/download")
    public ResponseEntity<byte[]> downloadApplicationLogs(@PathVariable Long id, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
            Application application = applicationService.getApplicationById(id);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查权限
            if (!application.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
            
            if (application.getLogFilePath() != null && Files.exists(Paths.get(application.getLogFilePath()))) {
                byte[] logContent = Files.readAllBytes(Paths.get(application.getLogFilePath()));
                
                return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + application.getName() + "_" + application.getId() + ".log")
                    .header("Content-Type", "text/plain")
                    .body(logContent);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 显示配置编辑器页面
     */
    @GetMapping("/{id}/config/editor")
    public String showConfigEditor(@PathVariable Long id, Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }
        
        User user = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        Application application = applicationService.getApplicationById(id);
        if (application == null) {
            model.addAttribute("error", "应用不存在");
            return "redirect:/apps";
        }
        
        // 检查权限
        if (!application.getUser().getId().equals(user.getId())) {
            model.addAttribute("error", "权限不足：您只能配置自己的应用");
            return "redirect:/apps";
        }
        
        model.addAttribute("applicationId", id);
        model.addAttribute("application", application);
        model.addAttribute("username", authentication.getName());
        
        return "admin/config-editor";
    }
    
    /**
     * 应用健康检查
     */
    @GetMapping("/{id}/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getApplicationHealth(@PathVariable Long id, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
            Application application = applicationService.getApplicationById(id);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查权限
            if (!application.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
            
            Map<String, Object> health = new HashMap<>();
            health.put("applicationName", application.getName());
            health.put("status", application.getStatus());
            health.put("port", application.getPort());
            health.put("processId", application.getProcessId());
            
            if ("RUNNING".equals(application.getStatus()) && application.getProcessId() != null) {
                try {
                    // 检查进程是否存在
                    boolean isAlive = ProcessHandle.of(Long.parseLong(application.getProcessId()))
                        .map(ProcessHandle::isAlive)
                        .orElse(false);
                    
                    if (isAlive) {
                        health.put("healthStatus", "UP");
                        health.put("message", "应用运行正常");
                    } else {
                        health.put("healthStatus", "DOWN");
                        health.put("message", "进程不存在，但状态显示运行中");
                    }
                } catch (Exception e) {
                    health.put("healthStatus", "UNKNOWN");
                    health.put("message", "无法检查进程状态: " + e.getMessage());
                }
            } else {
                health.put("healthStatus", "DOWN");
                health.put("message", "应用未运行");
            }
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("healthStatus", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.ok(error);
        }
    }
    
    /**
     * 格式化运行时间
     */
    private String formatUptime(long minutes) {
        if (minutes < 60) {
            return minutes + "分钟";
        } else if (minutes < 1440) { // 60 * 24
            return (minutes / 60) + "小时" + (minutes % 60) + "分钟";
        } else {
            long days = minutes / 1440;
            long hours = (minutes % 1440) / 60;
            return days + "天" + hours + "小时";
        }
    }
    
    /**
     * REST API - 获取应用列表
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<Application>> getApplications(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        List<Application> applications = applicationService.getUserApplications(user);
        return ResponseEntity.ok(applications);
    }
    
    /**
     * AJAX API: 获取应用管理内容片段 - 现代化版本
     */
    @GetMapping("/api/content")
    @ResponseBody
    public ResponseEntity<String> getApplicationsContent(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body("<div class=\"alert alert-warning\">请先登录</div>");
            }
            
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
            List<Application> applications = applicationService.getUserApplications(user);
            
            StringBuilder htmlBuilder = new StringBuilder();
            
            // 页面标题
            htmlBuilder.append("<div class=\"content-header\">");
            htmlBuilder.append("<div class=\"page-title-group\">");
            htmlBuilder.append("<h1 class=\"page-title\">应用管理控制面板</h1>");
            htmlBuilder.append("</div></div>");
            
            // 现代化统计面板
            htmlBuilder.append("<section class=\"modern-stats-container\">");
            htmlBuilder.append("<div class=\"stats-grid\">");
            
            // 应用状态统计
            long runningCount = applications.stream().filter(app -> "RUNNING".equals(app.getStatus())).count();
            long stoppedCount = applications.stream().filter(app -> "STOPPED".equals(app.getStatus())).count();
            long startingCount = applications.stream().filter(app -> "STARTING".equals(app.getStatus())).count();
            long totalCount = applications.size();
            
            // 运行中应用卡片
            htmlBuilder.append("<div class=\"modern-stats-card running\" onclick=\"filterApplications('running')\">");
            htmlBuilder.append("<div class=\"stats-card-content\">");
            htmlBuilder.append("<div class=\"stats-info\">");
            htmlBuilder.append("<div class=\"stats-label\">运行中</div>");
            htmlBuilder.append("<div class=\"stats-value\">");
            htmlBuilder.append("<span class=\"stats-value-main\">").append(runningCount).append("</span>");
            htmlBuilder.append("<span class=\"stats-value-unit\">个</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-trend\"><span class=\"trend-icon trend-up\">↗</span>应用正常运行</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-icon-container\">");
            htmlBuilder.append("<i class=\"stats-icon fas fa-check-circle icon-pulse\"></i>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            
            // 已停止应用卡片  
            htmlBuilder.append("<div class=\"modern-stats-card stopped\" onclick=\"filterApplications('stopped')\">");
            htmlBuilder.append("<div class=\"stats-card-content\">");
            htmlBuilder.append("<div class=\"stats-info\">");
            htmlBuilder.append("<div class=\"stats-label\">已停止</div>");
            htmlBuilder.append("<div class=\"stats-value\">");
            htmlBuilder.append("<span class=\"stats-value-main\">").append(stoppedCount).append("</span>");
            htmlBuilder.append("<span class=\"stats-value-unit\">个</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-trend\"><span class=\"trend-icon trend-stable\">~</span>待启动</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-icon-container\">");
            htmlBuilder.append("<i class=\"stats-icon fas fa-times-circle\"></i>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            
            // 启动中应用卡片
            htmlBuilder.append("<div class=\"modern-stats-card starting\" onclick=\"filterApplications('starting')\">");
            htmlBuilder.append("<div class=\"stats-card-content\">");
            htmlBuilder.append("<div class=\"stats-info\">");
            htmlBuilder.append("<div class=\"stats-label\">启动中</div>");
            htmlBuilder.append("<div class=\"stats-value\">");
            htmlBuilder.append("<span class=\"stats-value-main\">").append(startingCount).append("</span>");
            htmlBuilder.append("<span class=\"stats-value-unit\">个</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-trend\"><span class=\"trend-icon trend-up\">↗</span>正在启动</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-icon-container\">");
            htmlBuilder.append("<i class=\"stats-icon fas fa-spinner icon-spin\"></i>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            
            // 总应用数卡片
            htmlBuilder.append("<div class=\"modern-stats-card total\" onclick=\"filterApplications('all')\">");
            htmlBuilder.append("<div class=\"stats-card-content\">");
            htmlBuilder.append("<div class=\"stats-info\">");
            htmlBuilder.append("<div class=\"stats-label\">总应用数</div>");
            htmlBuilder.append("<div class=\"stats-value\">");
            htmlBuilder.append("<span class=\"stats-value-main\">").append(totalCount).append("</span>");
            htmlBuilder.append("<span class=\"stats-value-unit\">个</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-trend\"><span class=\"trend-icon trend-stable\">~</span>应用概览</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-icon-container\">");
            htmlBuilder.append("<i class=\"stats-icon fas fa-rocket\"></i>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            
            htmlBuilder.append("</div>");
            htmlBuilder.append("</section>");
            
            // 应用卡片网格
            htmlBuilder.append("<section class=\"modern-apps-section\">");
            htmlBuilder.append("<div class=\"section-header\">");
            htmlBuilder.append("<h2 class=\"section-title\">我的应用</h2>");
            htmlBuilder.append("<div class=\"section-actions\">");
            htmlBuilder.append("<button class=\"action-btn secondary\" onclick=\"refreshApps()\">");
            htmlBuilder.append("<i class=\"fas fa-sync-alt\"></i>");
            htmlBuilder.append("<span>刷新数据</span>");
            htmlBuilder.append("</button>");
            htmlBuilder.append("<button class=\"action-btn primary\" onclick=\"uploadApp()\">");
            htmlBuilder.append("<i class=\"fas fa-plus\"></i>");
            htmlBuilder.append("<span>上传新应用</span>");
            htmlBuilder.append("</button>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            
            if (applications.isEmpty()) {
                // 空状态
                htmlBuilder.append("<div class=\"empty-state\">");
                htmlBuilder.append("<div class=\"empty-icon\">");
                htmlBuilder.append("<i class=\"fas fa-rocket\"></i>");
                htmlBuilder.append("</div>");
                htmlBuilder.append("<h3 class=\"empty-title\">还没有应用</h3>");
                htmlBuilder.append("<p class=\"empty-description\">上传您的第一个JAR应用开始使用管理功能</p>");
                htmlBuilder.append("<div class=\"empty-actions\">");
                htmlBuilder.append("<button class=\"modern-action-btn primary\" onclick=\"uploadApp()\">");
                htmlBuilder.append("<i class=\"fas fa-plus\"></i>");
                htmlBuilder.append("<span>上传应用</span>");
                htmlBuilder.append("</button>");
                htmlBuilder.append("</div>");
                htmlBuilder.append("</div>");
            } else {
                // 应用卡片网格
                htmlBuilder.append("<div class=\"modern-apps-grid\">");
                for (Application app : applications) {
                    String statusClass = getModernStatusClass(app.getStatus());
                    String statusText = getStatusText(app.getStatus());
                    String statusIcon = getStatusIcon(app.getStatus());
                    
                    htmlBuilder.append("<div class=\"modern-app-card ").append(statusClass).append("\">");
                    
                    // 卡片头部
                    htmlBuilder.append("<div class=\"app-header\">");
                    htmlBuilder.append("<div class=\"app-info\">");
                    htmlBuilder.append("<div class=\"app-name\">").append(escapeHtml(app.getName())).append("</div>");
                    htmlBuilder.append("<div class=\"app-meta\">");
                    htmlBuilder.append("<span class=\"app-id\">ID: ").append(app.getId()).append("</span>");
                    if (app.getLastStartedAt() != null) {
                        htmlBuilder.append("<span class=\"app-time\">").append(formatRelativeTime(app.getLastStartedAt())).append("</span>");
                    }
                    htmlBuilder.append("</div>");
                    htmlBuilder.append("</div>");
                    htmlBuilder.append("<div class=\"status-indicator ").append(statusClass).append("\">");
                    htmlBuilder.append("<i class=\"").append(statusIcon).append("\"></i>");
                    htmlBuilder.append("<span>").append(statusText).append("</span>");
                    htmlBuilder.append("</div>");
                    htmlBuilder.append("</div>");
                    
                    // 应用指标
                    htmlBuilder.append("<div class=\"app-metrics\">");
                    htmlBuilder.append("<div class=\"metrics-grid\">");
                    htmlBuilder.append("<div class=\"metric-item\">");
                    htmlBuilder.append("<div class=\"metric-icon\"><i class=\"fas fa-plug\"></i></div>");
                    htmlBuilder.append("<div class=\"metric-info\">");
                    htmlBuilder.append("<div class=\"metric-label\">端口</div>");
                    htmlBuilder.append("<div class=\"metric-value\">").append(app.getPort() != null ? app.getPort() : "N/A").append("</div>");
                    htmlBuilder.append("</div>");
                    htmlBuilder.append("</div>");
                    if (app.getDebugPort() != null) {
                        htmlBuilder.append("<div class=\"metric-item\">");
                        htmlBuilder.append("<div class=\"metric-icon\"><i class=\"fas fa-bug\"></i></div>");
                        htmlBuilder.append("<div class=\"metric-info\">");
                        htmlBuilder.append("<div class=\"metric-label\">调试</div>");
                        htmlBuilder.append("<div class=\"metric-value\">").append(app.getDebugPort()).append("</div>");
                        htmlBuilder.append("</div>");
                        htmlBuilder.append("</div>");
                    }
                    htmlBuilder.append("</div>");
                    htmlBuilder.append("</div>");
                    
                    // 操作按钮
                    htmlBuilder.append("<div class=\"app-actions\">");
                    if ("RUNNING".equals(app.getStatus())) {
                        htmlBuilder.append("<button class=\"app-action-btn secondary\" onclick=\"stopApplication('").append(app.getId()).append("')\">");
                        htmlBuilder.append("<i class=\"fas fa-stop\"></i>");
                        htmlBuilder.append("<span>停止</span>");
                        htmlBuilder.append("</button>");
                        htmlBuilder.append("<button class=\"app-action-btn primary\" onclick=\"restartApplication('").append(app.getId()).append("')\">");
                        htmlBuilder.append("<i class=\"fas fa-redo\"></i>");
                        htmlBuilder.append("<span>重启</span>");
                        htmlBuilder.append("</button>");
                    } else {
                        htmlBuilder.append("<button class=\"app-action-btn success\" onclick=\"startApplication('").append(app.getId()).append("')\">");
                        htmlBuilder.append("<i class=\"fas fa-play\"></i>");
                        htmlBuilder.append("<span>启动</span>");
                        htmlBuilder.append("</button>");
                    }
                    htmlBuilder.append("<button class=\"app-action-btn secondary\" onclick=\"viewApplicationDetails('").append(app.getId()).append("')\">");
                    htmlBuilder.append("<i class=\"fas fa-info-circle\"></i>");
                    htmlBuilder.append("<span>详情</span>");
                    htmlBuilder.append("</button>");
                    htmlBuilder.append("</div>");
                    
                    htmlBuilder.append("</div>");
                }
                htmlBuilder.append("</div>");
            }
            
            htmlBuilder.append("</section>");
            
            return ResponseEntity.ok(htmlBuilder.toString());
            
        } catch (Exception e) {
            String errorContent = "<div class=\"alert alert-danger\"><i class=\"fas fa-exclamation-triangle\"></i> 加载应用内容失败: " + e.getMessage() + "</div>";
            return ResponseEntity.status(500).body(errorContent);
        }
    }
    
    // 辅助方法 - 现代化版本
    private String getModernStatusClass(String status) {
        switch (status != null ? status.toUpperCase() : "UNKNOWN") {
            case "RUNNING": return "running";
            case "STOPPED": return "stopped";
            case "STARTING": return "starting";
            default: return "stopped";
        }
    }
    
    private String getStatusText(String status) {
        switch (status != null ? status.toUpperCase() : "UNKNOWN") {
            case "RUNNING": return "运行中";
            case "STOPPED": return "已停止";
            case "STARTING": return "启动中";
            default: return "未知";
        }
    }
    
    private String getStatusIcon(String status) {
        switch (status != null ? status.toUpperCase() : "UNKNOWN") {
            case "RUNNING": return "fas fa-check-circle";
            case "STOPPED": return "fas fa-times-circle";
            case "STARTING": return "fas fa-spinner fa-spin";
            default: return "fas fa-question-circle";
        }
    }
    
    private String formatRelativeTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        
        java.time.Duration duration = java.time.Duration.between(dateTime, java.time.LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();
        
        if (minutes < 60) {
            return minutes + "分钟前";
        } else if (hours < 24) {
            return hours + "小时前";
        } else {
            return days + "天前";
        }
    }
    
    // 保留原有方法以兼容其他地方的调用
    private String getStatusClass(String status) {
        return getModernStatusClass(status);
    }
    
    private String getBadgeClass(String status) {
        switch (status != null ? status.toUpperCase() : "UNKNOWN") {
            case "RUNNING": return "bg-success";
            case "STOPPED": return "bg-danger";
            case "STARTING": return "bg-primary";
            default: return "bg-secondary";
        }
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
    
    // ===============================
    // 批量操作API
    // ===============================
    
    /**
     * 批量启动应用
     */
    @PostMapping("/batch/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> batchStart(
            @RequestBody Map<String, Object> request, 
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
        }
        
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            @SuppressWarnings("unchecked")
            List<Long> applicationIds = (List<Long>) request.get("applicationIds");
            
            if (applicationIds == null || applicationIds.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("应用ID列表不能为空"));
            }
            
            Map<String, Object> result = applicationService.batchStart(applicationIds, user);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("批量启动失败: " + e.getMessage()));
        }
    }
    
    /**
     * 批量停止应用
     */
    @PostMapping("/batch/stop")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> batchStop(
            @RequestBody Map<String, Object> request, 
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
        }
        
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            @SuppressWarnings("unchecked")
            List<Long> applicationIds = (List<Long>) request.get("applicationIds");
            
            if (applicationIds == null || applicationIds.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("应用ID列表不能为空"));
            }
            
            Map<String, Object> result = applicationService.batchStop(applicationIds, user);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("批量停止失败: " + e.getMessage()));
        }
    }
    
    /**
     * 批量重启应用
     */
    @PostMapping("/batch/restart")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> batchRestart(
            @RequestBody Map<String, Object> request, 
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
        }
        
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            @SuppressWarnings("unchecked")
            List<Long> applicationIds = (List<Long>) request.get("applicationIds");
            
            if (applicationIds == null || applicationIds.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("应用ID列表不能为空"));
            }
            
            Map<String, Object> result = applicationService.batchRestart(applicationIds, user);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("批量重启失败: " + e.getMessage()));
        }
    }
    
    /**
     * 批量删除应用
     */
    @PostMapping("/batch/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> batchDelete(
            @RequestBody Map<String, Object> request, 
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
        }
        
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            @SuppressWarnings("unchecked")
            List<Long> applicationIds = (List<Long>) request.get("applicationIds");
            
            if (applicationIds == null || applicationIds.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("应用ID列表不能为空"));
            }
            
            Map<String, Object> result = applicationService.batchDelete(applicationIds, user);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("批量删除失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}