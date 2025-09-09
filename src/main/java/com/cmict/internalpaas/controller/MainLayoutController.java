package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.AdminDashboardDto;
import com.cmict.internalpaas.dto.DeveloperDashboardDto;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.DashboardService;
import com.cmict.internalpaas.service.UserService;
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
            
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<div class=\"content-header\">");
            htmlBuilder.append("<div class=\"page-title-group\">");
            htmlBuilder.append("<h1 class=\"page-title\">管理员工作台</h1>");
            htmlBuilder.append("</div></div>");
            
            htmlBuilder.append("<div class=\"dashboard-stats\">");
            htmlBuilder.append("<div class=\"stat-item\">");
            htmlBuilder.append("<div class=\"stat-icon\"><i class=\"fas fa-server\"></i></div>");
            htmlBuilder.append("<div class=\"stat-content\">");
            htmlBuilder.append("<div class=\"stat-number\">").append(adminData.getTotalServers()).append("</div>");
            htmlBuilder.append("<div class=\"stat-label\">服务器总数</div>");
            htmlBuilder.append("</div></div>");
            
            htmlBuilder.append("<div class=\"stat-item\">");
            htmlBuilder.append("<div class=\"stat-icon\"><i class=\"fas fa-users\"></i></div>");
            htmlBuilder.append("<div class=\"stat-content\">");
            htmlBuilder.append("<div class=\"stat-number\">").append(adminData.getTotalUsers()).append("</div>");
            htmlBuilder.append("<div class=\"stat-label\">用户总数</div>");
            htmlBuilder.append("</div></div>");
            
            htmlBuilder.append("<div class=\"stat-item\">");
            htmlBuilder.append("<div class=\"stat-icon\"><i class=\"fas fa-check-circle\"></i></div>");
            htmlBuilder.append("<div class=\"stat-content\">");
            htmlBuilder.append("<div class=\"stat-number\">").append(adminData.getActiveServers()).append("</div>");
            htmlBuilder.append("<div class=\"stat-label\">活跃服务器</div>");
            htmlBuilder.append("</div></div>");
            
            htmlBuilder.append("<div class=\"stat-item\">");
            htmlBuilder.append("<div class=\"stat-icon\"><i class=\"fas fa-chart-line\"></i></div>");
            htmlBuilder.append("<div class=\"stat-content\">");
            htmlBuilder.append("<div class=\"stat-number\">").append(String.format("%.1f%%", adminData.getCpuUsage())).append("</div>");
            htmlBuilder.append("<div class=\"stat-label\">CPU使用率</div>");
            htmlBuilder.append("</div></div>");
            htmlBuilder.append("</div>");
            
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
            
            htmlBuilder.append("<div class=\"dashboard-stats\">");
            htmlBuilder.append("<div class=\"stat-item\">");
            htmlBuilder.append("<div class=\"stat-icon\"><i class=\"fas fa-rocket\"></i></div>");
            htmlBuilder.append("<div class=\"stat-content\">");
            htmlBuilder.append("<div class=\"stat-number\">").append(developerData.getTotalApplications()).append("</div>");
            htmlBuilder.append("<div class=\"stat-label\">我的应用</div>");
            htmlBuilder.append("</div></div>");
            
            htmlBuilder.append("<div class=\"stat-item\">");
            htmlBuilder.append("<div class=\"stat-icon\"><i class=\"fas fa-play\"></i></div>");
            htmlBuilder.append("<div class=\"stat-content\">");
            htmlBuilder.append("<div class=\"stat-number\">").append(developerData.getRunningApplications()).append("</div>");
            htmlBuilder.append("<div class=\"stat-label\">运行中</div>");
            htmlBuilder.append("</div></div>");
            
            htmlBuilder.append("<div class=\"stat-item\">");
            htmlBuilder.append("<div class=\"stat-icon\"><i class=\"fas fa-bug\"></i></div>");
            htmlBuilder.append("<div class=\"stat-content\">");
            htmlBuilder.append("<div class=\"stat-number\">").append(developerData.getActiveDebugSessions()).append("</div>");
            htmlBuilder.append("<div class=\"stat-label\">调试会话</div>");
            htmlBuilder.append("</div></div>");
            htmlBuilder.append("</div>");
            
            return ResponseEntity.ok(htmlBuilder.toString());
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("<div class=\"alert alert-danger\">加载工作台内容失败: " + e.getMessage() + "</div>");
        }
    }
}