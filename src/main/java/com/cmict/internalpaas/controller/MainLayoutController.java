package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.AdminDashboardDto;
import com.cmict.internalpaas.dto.DeveloperDashboardDto;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.service.DashboardService;
import com.cmict.internalpaas.service.UserService;
import com.cmict.internalpaas.service.ServerService;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MainLayoutController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private ServerService serverService;

    /**
     * 主框架页面 - 管理员工作空间
     */
    @GetMapping("/admin/workspace")
    public String adminWorkspace(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("username", authentication.getName());
        model.addAttribute("userType", "admin");
        
        // 获取用户信息用于权限检查
        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null) {
            model.addAttribute("isAdmin", userService.hasRole(user, User.Role.ADMIN) || userService.hasRole(user, User.Role.SUPER_ADMIN));
            model.addAttribute("isSuperAdmin", userService.hasRole(user, User.Role.SUPER_ADMIN));
        }
        
        return "main-layout"; // 使用新的主框架页面
    }
    
    /**
     * 主框架页面 - 开发者工作空间
     */
    @GetMapping("/developer/workspace")
    public String developerWorkspace(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("username", authentication.getName());
        model.addAttribute("userType", "developer");
        
        // 获取用户信息用于权限检查
        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null) {
            model.addAttribute("isAdmin", userService.hasRole(user, User.Role.ADMIN) || userService.hasRole(user, User.Role.SUPER_ADMIN));
            model.addAttribute("isSuperAdmin", userService.hasRole(user, User.Role.SUPER_ADMIN));
        }
        
        return "main-layout"; // 使用同一个主框架页面
    }
    
    /**
     * 主框架页面 - 通用工作空间
     */
    @GetMapping("/workspace")
    public String workspace(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("username", authentication.getName());
        model.addAttribute("userType", "user");
        
        // 获取用户信息用于权限检查
        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null) {
            model.addAttribute("isAdmin", userService.hasRole(user, User.Role.ADMIN) || userService.hasRole(user, User.Role.SUPER_ADMIN));
            model.addAttribute("isSuperAdmin", userService.hasRole(user, User.Role.SUPER_ADMIN));
        }
        
        return "main-layout";
    }
    
    // API端点用于AJAX内容加载
    
    /**
     * 获取管理员工作台内容
     */
    @GetMapping("/admin/api/workspace-dashboard-content")
    @ResponseBody
    public ResponseEntity<String> getAdminDashboardContent(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("<div class=\"alert alert-warning\">请先登录</div>");
        }
        
        try {
            AdminDashboardDto adminData = dashboardService.getAdminDashboardData();
            List<Server> servers = serverService.getAllServers();
            
            StringBuilder htmlBuilder = new StringBuilder();
            
            // 构建完整的仪表板内容，完全参考admin-dashboard.html的结构和样式
            htmlBuilder.append("<div class=\"content-header\">");
            htmlBuilder.append("  <div class=\"page-title-group\">");
            htmlBuilder.append("    <h1 class=\"page-title\">管理员工作台</h1>");
            htmlBuilder.append("  </div>");
            htmlBuilder.append("</div>");
            
            // 现代化统计面板 - 采用与开发者工作台一致的样式
            htmlBuilder.append("<section class=\"modern-stats-container\">");
            htmlBuilder.append("<div class=\"stats-grid\">");
            
            // 服务器统计卡片
            htmlBuilder.append("<div class=\"modern-stats-card running\" onclick=\"loadContent('servers')\">");
            htmlBuilder.append("<div class=\"stats-card-content\">");
            htmlBuilder.append("<div class=\"stats-info\">");
            htmlBuilder.append("<div class=\"stats-label\">服务器总数</div>");
            htmlBuilder.append("<div class=\"stats-value\">");
            htmlBuilder.append("<span class=\"stats-value-main\">").append(adminData.getTotalServers()).append("</span>");
            htmlBuilder.append("<span class=\"stats-value-unit\">台</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-trend\"><span class=\"trend-icon trend-up\">↗</span>活跃: ").append(adminData.getActiveServers()).append("台</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-icon-container\">");
            htmlBuilder.append("<i class=\"stats-icon fas fa-server icon-pulse\"></i>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            
            // 用户统计卡片  
            htmlBuilder.append("<div class=\"modern-stats-card total\" onclick=\"loadContent('users')\">");
            htmlBuilder.append("<div class=\"stats-card-content\">");
            htmlBuilder.append("<div class=\"stats-info\">");
            htmlBuilder.append("<div class=\"stats-label\">用户总数</div>");
            htmlBuilder.append("<div class=\"stats-value\">");
            htmlBuilder.append("<span class=\"stats-value-main\">").append(adminData.getTotalUsers()).append("</span>");
            htmlBuilder.append("<span class=\"stats-value-unit\">人</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-trend\"><span class=\"trend-icon trend-stable\">~</span>管理员: ").append(adminData.getAdminUsers()).append("人</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-icon-container\">");
            htmlBuilder.append("<i class=\"stats-icon fas fa-users\"></i>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            
            // 系统资源统计卡片
            htmlBuilder.append("<div class=\"modern-stats-card starting\">");
            htmlBuilder.append("<div class=\"stats-card-content\">");
            htmlBuilder.append("<div class=\"stats-info\">");
            htmlBuilder.append("<div class=\"stats-label\">CPU使用率</div>");
            htmlBuilder.append("<div class=\"stats-value\">");
            htmlBuilder.append("<span class=\"stats-value-main\">").append(String.format("%.1f", adminData.getCpuUsage())).append("</span>");
            htmlBuilder.append("<span class=\"stats-value-unit\">%</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-trend\"><span class=\"trend-icon trend-stable\">~</span>内存: ").append(String.format("%.1f", adminData.getMemoryUsage())).append("%</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-icon-container\">");
            htmlBuilder.append("<i class=\"stats-icon fas fa-microchip icon-pulse\"></i>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            
            // 系统活跃度统计卡片
            htmlBuilder.append("<div class=\"modern-stats-card stopped\">");
            htmlBuilder.append("<div class=\"stats-card-content\">");
            htmlBuilder.append("<div class=\"stats-info\">");
            htmlBuilder.append("<div class=\"stats-label\">系统活跃度</div>");
            htmlBuilder.append("<div class=\"stats-value\">");
            htmlBuilder.append("<span class=\"stats-value-main\">").append(adminData.getTotalUsers() > 0 ? adminData.getTotalUsers() : 0).append("</span>");
            htmlBuilder.append("<span class=\"stats-value-unit\">分</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-trend\"><span class=\"trend-icon trend-up\">↗</span>今日活跃用户</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-icon-container\">");
            htmlBuilder.append("<i class=\"stats-icon fas fa-chart-line icon-bounce\"></i>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            
            htmlBuilder.append("</div>");
            htmlBuilder.append("</section>");
            
            // 现代化快捷操作区域
            htmlBuilder.append("<section class=\"modern-apps-section\">");
            htmlBuilder.append("<div class=\"section-header\">");
            htmlBuilder.append("<h2 class=\"section-title\">管理功能</h2>");
            htmlBuilder.append("<div class=\"section-actions\">");
            htmlBuilder.append("<button class=\"action-btn secondary\" onclick=\"refreshAllData()\">");
            htmlBuilder.append("<i class=\"fas fa-sync-alt\"></i>");
            htmlBuilder.append("<span>刷新数据</span>");
            htmlBuilder.append("</button>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            
            // 管理功能卡片网格
            htmlBuilder.append("<div class=\"modern-apps-grid\">");
            
            // 服务器管理卡片
            htmlBuilder.append("<div class=\"modern-app-card running\" onclick=\"loadContent('servers')\">");
            htmlBuilder.append("<div class=\"app-header\">");
            htmlBuilder.append("<div class=\"app-info\">");
            htmlBuilder.append("<div class=\"app-name\">服务器管理</div>");
            htmlBuilder.append("<div class=\"app-meta\">");
            htmlBuilder.append("<span class=\"app-id\">管理和监控服务器</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"status-indicator running\">");
            htmlBuilder.append("<i class=\"fas fa-server\"></i>");
            htmlBuilder.append("<span>活跃</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"app-metrics\">");
            htmlBuilder.append("<div class=\"metrics-grid\">");
            htmlBuilder.append("<div class=\"metric-item\">");
            htmlBuilder.append("<div class=\"metric-icon\"><i class=\"fas fa-server\"></i></div>");
            htmlBuilder.append("<div class=\"metric-info\">");
            htmlBuilder.append("<div class=\"metric-label\">服务器</div>");
            htmlBuilder.append("<div class=\"metric-value\">").append(adminData.getTotalServers()).append("台</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"metric-item\">");
            htmlBuilder.append("<div class=\"metric-icon\"><i class=\"fas fa-check-circle\"></i></div>");
            htmlBuilder.append("<div class=\"metric-info\">");
            htmlBuilder.append("<div class=\"metric-label\">在线</div>");
            htmlBuilder.append("<div class=\"metric-value\">").append(adminData.getActiveServers()).append("台</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"app-actions\">");
            htmlBuilder.append("<button class=\"app-action-btn primary\" onclick=\"loadContent('servers')\">");
            htmlBuilder.append("<i class=\"fas fa-cog\"></i>");
            htmlBuilder.append("<span>管理</span>");
            htmlBuilder.append("</button>");
            htmlBuilder.append("<button class=\"app-action-btn secondary\" onclick=\"addServer()\">");
            htmlBuilder.append("<i class=\"fas fa-plus\"></i>");
            htmlBuilder.append("<span>添加</span>");
            htmlBuilder.append("</button>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            
            // 用户管理卡片
            htmlBuilder.append("<div class=\"modern-app-card total\" onclick=\"loadContent('users')\">");
            htmlBuilder.append("<div class=\"app-header\">");
            htmlBuilder.append("<div class=\"app-info\">");
            htmlBuilder.append("<div class=\"app-name\">用户管理</div>");
            htmlBuilder.append("<div class=\"app-meta\">");
            htmlBuilder.append("<span class=\"app-id\">用户权限管理</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"status-indicator total\">");
            htmlBuilder.append("<i class=\"fas fa-users\"></i>");
            htmlBuilder.append("<span>管理中</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"app-metrics\">");
            htmlBuilder.append("<div class=\"metrics-grid\">");
            htmlBuilder.append("<div class=\"metric-item\">");
            htmlBuilder.append("<div class=\"metric-icon\"><i class=\"fas fa-users\"></i></div>");
            htmlBuilder.append("<div class=\"metric-info\">");
            htmlBuilder.append("<div class=\"metric-label\">用户</div>");
            htmlBuilder.append("<div class=\"metric-value\">").append(adminData.getTotalUsers()).append("人</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"metric-item\">");
            htmlBuilder.append("<div class=\"metric-icon\"><i class=\"fas fa-user-shield\"></i></div>");
            htmlBuilder.append("<div class=\"metric-info\">");
            htmlBuilder.append("<div class=\"metric-label\">管理员</div>");
            htmlBuilder.append("<div class=\"metric-value\">").append(adminData.getAdminUsers()).append("人</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"app-actions\">");
            htmlBuilder.append("<button class=\"app-action-btn primary\" onclick=\"loadContent('users')\">");
            htmlBuilder.append("<i class=\"fas fa-cog\"></i>");
            htmlBuilder.append("<span>管理</span>");
            htmlBuilder.append("</button>");
            htmlBuilder.append("<button class=\"app-action-btn secondary\" onclick=\"addUser()\">");
            htmlBuilder.append("<i class=\"fas fa-user-plus\"></i>");
            htmlBuilder.append("<span>添加</span>");
            htmlBuilder.append("</button>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            
            // 系统监控卡片
            htmlBuilder.append("<div class=\"modern-app-card starting\">");
            htmlBuilder.append("<div class=\"app-header\">");
            htmlBuilder.append("<div class=\"app-info\">");
            htmlBuilder.append("<div class=\"app-name\">系统监控</div>");
            htmlBuilder.append("<div class=\"app-meta\">");
            htmlBuilder.append("<span class=\"app-id\">实时系统状态</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"status-indicator starting\">");
            htmlBuilder.append("<i class=\"fas fa-chart-line\"></i>");
            htmlBuilder.append("<span>监控中</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"app-metrics\">");
            htmlBuilder.append("<div class=\"metrics-grid\">");
            htmlBuilder.append("<div class=\"metric-item\">");
            htmlBuilder.append("<div class=\"metric-icon\"><i class=\"fas fa-microchip\"></i></div>");
            htmlBuilder.append("<div class=\"metric-info\">");
            htmlBuilder.append("<div class=\"metric-label\">CPU</div>");
            htmlBuilder.append("<div class=\"metric-value\">").append(String.format("%.1f", adminData.getCpuUsage())).append("%</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"metric-item\">");
            htmlBuilder.append("<div class=\"metric-icon\"><i class=\"fas fa-memory\"></i></div>");
            htmlBuilder.append("<div class=\"metric-info\">");
            htmlBuilder.append("<div class=\"metric-label\">内存</div>");
            htmlBuilder.append("<div class=\"metric-value\">").append(String.format("%.1f", adminData.getMemoryUsage())).append("%</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"app-actions\">");
            htmlBuilder.append("<button class=\"app-action-btn primary\" onclick=\"alert('监控功能开发中...')\">");
            htmlBuilder.append("<i class=\"fas fa-chart-area\"></i>");
            htmlBuilder.append("<span>详情</span>");
            htmlBuilder.append("</button>");
            htmlBuilder.append("<button class=\"app-action-btn secondary\" onclick=\"refreshAllData()\">");
            htmlBuilder.append("<i class=\"fas fa-sync-alt\"></i>");
            htmlBuilder.append("<span>刷新</span>");
            htmlBuilder.append("</button>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            
            htmlBuilder.append("</div>");
            htmlBuilder.append("</section>");
            
            return ResponseEntity.ok(htmlBuilder.toString());
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("<div class=\"alert alert-danger\">加载工作台内容失败: " + e.getMessage() + "</div>");
        }
    }
    
    /**
     * 获取开发者工作台内容
     */
    @GetMapping("/developer/api/workspace-dashboard-content")
    @ResponseBody
    public ResponseEntity<String> getDeveloperDashboardContent(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("<div class=\"alert alert-warning\">请先登录</div>");
        }
        
        try {
            DeveloperDashboardDto developerData = dashboardService.getDeveloperDashboardData(authentication.getName());
            
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<div class=\"content-header\">");
            htmlBuilder.append("<div class=\"page-title-group\">");
            htmlBuilder.append("<h1 class=\"page-title\">开发者工作台</h1>");
            htmlBuilder.append("</div></div>");
            
            // 现代化统计面板
            htmlBuilder.append("<section class=\"modern-stats-container\">");
            htmlBuilder.append("<div class=\"stats-grid\">");
            
            // 我的应用卡片
            htmlBuilder.append("<div class=\"modern-stats-card total\" onclick=\"filterApplications('all')\">");
            htmlBuilder.append("<div class=\"stats-card-content\">");
            htmlBuilder.append("<div class=\"stats-info\">");
            htmlBuilder.append("<div class=\"stats-label\">我的应用</div>");
            htmlBuilder.append("<div class=\"stats-value\">");
            htmlBuilder.append("<span class=\"stats-value-main count-up\">").append(developerData.getTotalApplications()).append("</span>");
            htmlBuilder.append("<span class=\"stats-value-unit\">个</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-trend trend-up\">");
            htmlBuilder.append("<i class=\"fas fa-chart-line trend-icon\"></i>");
            htmlBuilder.append("<span>持续增长</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-icon-container\">");
            htmlBuilder.append("<i class=\"fas fa-rocket stats-icon icon-bounce\"></i>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-tooltip\">点击查看所有应用概览</div>");
            htmlBuilder.append("</div>");
            
            // 运行中应用卡片
            htmlBuilder.append("<div class=\"modern-stats-card running\" onclick=\"filterApplications('running')\">");
            htmlBuilder.append("<div class=\"stats-card-content\">");
            htmlBuilder.append("<div class=\"stats-info\">");
            htmlBuilder.append("<div class=\"stats-label\">运行中应用</div>");
            htmlBuilder.append("<div class=\"stats-value\">");
            htmlBuilder.append("<span class=\"stats-value-main count-up\">").append(developerData.getRunningApplications()).append("</span>");
            htmlBuilder.append("<span class=\"stats-value-unit\">个</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-trend trend-up\">");
            htmlBuilder.append("<i class=\"fas fa-arrow-up trend-icon\"></i>");
            htmlBuilder.append("<span>活跃运行</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-icon-container\">");
            htmlBuilder.append("<i class=\"fas fa-play-circle stats-icon icon-pulse\"></i>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-tooltip\">点击查看运行中的应用详情</div>");
            htmlBuilder.append("</div>");
            
            // 调试会话卡片
            htmlBuilder.append("<div class=\"modern-stats-card starting\" onclick=\"showDebugSessions()\">");
            htmlBuilder.append("<div class=\"stats-card-content\">");
            htmlBuilder.append("<div class=\"stats-info\">");
            htmlBuilder.append("<div class=\"stats-label\">调试会话</div>");
            htmlBuilder.append("<div class=\"stats-value\">");
            htmlBuilder.append("<span class=\"stats-value-main count-up\">").append(developerData.getActiveDebugSessions()).append("</span>");
            htmlBuilder.append("<span class=\"stats-value-unit\">个</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-trend trend-up\">");
            htmlBuilder.append("<i class=\"fas fa-arrow-up trend-icon\"></i>");
            htmlBuilder.append("<span>启动进行中</span>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-icon-container\">");
            htmlBuilder.append("<i class=\"fas fa-bug stats-icon icon-spin\"></i>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("</div>");
            htmlBuilder.append("<div class=\"stats-tooltip\">点击管理调试会话</div>");
            htmlBuilder.append("</div>");
            
            htmlBuilder.append("</div>"); // 关闭 stats-grid
            htmlBuilder.append("</section>"); // 关闭 modern-stats-container
            
            return ResponseEntity.ok(htmlBuilder.toString());
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("<div class=\"alert alert-danger\">加载工作台内容失败: " + e.getMessage() + "</div>");
        }
    }
    
    
    /**
     * 开发者仪表板实时数据API
     */
    @GetMapping("/api/developer/dashboard/stats")
    @ResponseBody
    public ResponseEntity<DeveloperDashboardDto> getDeveloperDashboardStats(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            DeveloperDashboardDto data = dashboardService.getDeveloperDashboardData(authentication.getName());
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}