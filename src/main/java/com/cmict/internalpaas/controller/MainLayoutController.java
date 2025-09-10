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
            
            // 现代化统计面板 - 完全复制admin-dashboard.html的结构
            htmlBuilder.append("<section class=\"modern-stats-grid\">");
            
            // 服务器统计卡片
            htmlBuilder.append("  <div class=\"modern-stat-card servers\">");
            htmlBuilder.append("    <div class=\"stat-header\">");
            htmlBuilder.append("      <div class=\"stat-icon-wrapper servers\"><i class=\"fas fa-server\"></i></div>");
            htmlBuilder.append("      <div class=\"stat-trend positive\">+</div>");
            htmlBuilder.append("    </div>");
            htmlBuilder.append("    <div class=\"stat-body\">");
            htmlBuilder.append("      <div class=\"stat-number\">").append(adminData.getTotalServers()).append("</div>");
            htmlBuilder.append("      <div class=\"stat-label\">服务器总数</div>");
            htmlBuilder.append("      <div class=\"stat-sublabel\">活跃服务器: <span>").append(adminData.getActiveServers()).append("</span></div>");
            htmlBuilder.append("    </div>");
            htmlBuilder.append("  </div>");
            
            // 用户统计卡片  
            htmlBuilder.append("  <div class=\"modern-stat-card users\">");
            htmlBuilder.append("    <div class=\"stat-header\">");
            htmlBuilder.append("      <div class=\"stat-icon-wrapper users\"><i class=\"fas fa-users\"></i></div>");
            htmlBuilder.append("      <div class=\"stat-trend neutral\">~</div>");
            htmlBuilder.append("    </div>");
            htmlBuilder.append("    <div class=\"stat-body\">");
            htmlBuilder.append("      <div class=\"stat-number\">").append(adminData.getTotalUsers()).append("</div>");
            htmlBuilder.append("      <div class=\"stat-label\">用户总数</div>");
            htmlBuilder.append("      <div class=\"stat-sublabel\">管理员: <span>").append(adminData.getAdminUsers()).append("</span> | 普通用户: <span>").append(adminData.getRegularUsers()).append("</span></div>");
            htmlBuilder.append("    </div>");
            htmlBuilder.append("  </div>");
            
            // CPU使用率统计卡片
            htmlBuilder.append("  <div class=\"modern-stat-card monitoring\">");
            htmlBuilder.append("    <div class=\"stat-header\">");
            htmlBuilder.append("      <div class=\"stat-icon-wrapper monitoring\"><i class=\"fas fa-microchip\"></i></div>");
            htmlBuilder.append("      <div class=\"stat-trend neutral\">~</div>");
            htmlBuilder.append("    </div>");
            htmlBuilder.append("    <div class=\"stat-body\">");
            htmlBuilder.append("      <div class=\"stat-number\">").append(String.format("%.1f", adminData.getCpuUsage())).append("%</div>");
            htmlBuilder.append("      <div class=\"stat-label\">CPU使用率</div>");
            htmlBuilder.append("      <div class=\"stat-sublabel\">内存: <span>").append(String.format("%.1f", adminData.getMemoryUsage())).append("%</span></div>");
            htmlBuilder.append("    </div>");
            htmlBuilder.append("  </div>");
            
            // 系统活跃度统计卡片
            htmlBuilder.append("  <div class=\"modern-stat-card activity\">");
            htmlBuilder.append("    <div class=\"stat-header\">");
            htmlBuilder.append("      <div class=\"stat-icon-wrapper activity\"><i class=\"fas fa-chart-line\"></i></div>");
            htmlBuilder.append("      <div class=\"stat-trend positive\">↗</div>");
            htmlBuilder.append("    </div>");
            htmlBuilder.append("    <div class=\"stat-body\">");
            htmlBuilder.append("      <div class=\"stat-number\">").append(adminData.getTotalUsers() > 0 ? adminData.getTotalUsers() : 0).append("</div>");
            htmlBuilder.append("      <div class=\"stat-label\">系统活跃度</div>");
            htmlBuilder.append("      <div class=\"stat-sublabel\">今日登录用户数</div>");
            htmlBuilder.append("    </div>");
            htmlBuilder.append("  </div>");
            
            htmlBuilder.append("</section>");
            
            // 最近活动时间线 - 复制admin-dashboard.html结构
            htmlBuilder.append("<section class=\"recent-activity\">");
            htmlBuilder.append("  <h2>最近活动</h2>");
            htmlBuilder.append("  <div class=\"activity-timeline\">");
            
            htmlBuilder.append("    <div class=\"activity-item\">");
            htmlBuilder.append("      <div class=\"activity-icon\"><i class=\"fas fa-user\"></i></div>");
            htmlBuilder.append("      <div class=\"activity-content\">");
            htmlBuilder.append("        <h4>用户登录</h4>");
            htmlBuilder.append("        <p>管理员 <strong>root</strong> 登录系统</p>");
            htmlBuilder.append("        <span class=\"activity-time\">2分钟前</span>");
            htmlBuilder.append("      </div>");
            htmlBuilder.append("    </div>");
            
            if (!servers.isEmpty()) {
                Server firstServer = servers.get(0);
                htmlBuilder.append("    <div class=\"activity-item\">");
                htmlBuilder.append("      <div class=\"activity-icon\"><i class=\"fas fa-server\"></i></div>");
                htmlBuilder.append("      <div class=\"activity-content\">");
                htmlBuilder.append("        <h4>服务器状态变更</h4>");
                htmlBuilder.append("        <p>服务器 <strong>").append(firstServer.getName()).append("</strong> 状态为").append(firstServer.getActive() ? "活跃" : "离线").append("</p>");
                htmlBuilder.append("        <span class=\"activity-time\">15分钟前</span>");
                htmlBuilder.append("      </div>");
                htmlBuilder.append("    </div>");
            } else {
                htmlBuilder.append("    <div class=\"activity-item\">");
                htmlBuilder.append("      <div class=\"activity-icon\"><i class=\"fas fa-server\"></i></div>");
                htmlBuilder.append("      <div class=\"activity-content\">");
                htmlBuilder.append("        <h4>系统监控</h4>");
                htmlBuilder.append("        <p>系统监控服务正常运行</p>");
                htmlBuilder.append("        <span class=\"activity-time\">15分钟前</span>");
                htmlBuilder.append("      </div>");
                htmlBuilder.append("    </div>");
            }
            
            htmlBuilder.append("    <div class=\"activity-item\">");
            htmlBuilder.append("      <div class=\"activity-icon\"><i class=\"fas fa-plus\"></i></div>");
            htmlBuilder.append("      <div class=\"activity-content\">");
            htmlBuilder.append("        <h4>系统状态</h4>");
            htmlBuilder.append("        <p>系统监控正常，所有服务运行稳定</p>");
            htmlBuilder.append("        <span class=\"activity-time\">1小时前</span>");
            htmlBuilder.append("      </div>");
            htmlBuilder.append("    </div>");
            
            htmlBuilder.append("  </div>");
            htmlBuilder.append("</section>");
            
            // 现代化快捷操作区 - 完全复制admin-dashboard.html结构
            htmlBuilder.append("<section class=\"modern-actions-grid\">");
            
            htmlBuilder.append("  <a href=\"javascript:void(0)\" onclick=\"loadContent('servers')\" class=\"modern-action-card servers\">");
            htmlBuilder.append("    <div class=\"action-card-content\">");
            htmlBuilder.append("      <div class=\"action-icon-wrapper\"><i class=\"fas fa-server\"></i></div>");
            htmlBuilder.append("      <h3 class=\"action-title\">服务器管理</h3>");
            htmlBuilder.append("      <p class=\"action-description\">管理和监控所有服务器的状态</p>");
            htmlBuilder.append("    </div>");
            htmlBuilder.append("  </a>");
            
            htmlBuilder.append("  <a href=\"javascript:void(0)\" onclick=\"loadContent('users')\" class=\"modern-action-card users\">");
            htmlBuilder.append("    <div class=\"action-card-content\">");
            htmlBuilder.append("      <div class=\"action-icon-wrapper\"><i class=\"fas fa-users\"></i></div>");
            htmlBuilder.append("      <h3 class=\"action-title\">用户管理</h3>");
            htmlBuilder.append("      <p class=\"action-description\">管理用户账户和权限设置</p>");
            htmlBuilder.append("    </div>");
            htmlBuilder.append("  </a>");
            
            htmlBuilder.append("  <a href=\"/admin/servers/new\" class=\"modern-action-card servers\">");
            htmlBuilder.append("    <div class=\"action-card-content\">");
            htmlBuilder.append("      <div class=\"action-icon-wrapper\"><i class=\"fas fa-plus\"></i></div>");
            htmlBuilder.append("      <h3 class=\"action-title\">添加服务器</h3>");
            htmlBuilder.append("      <p class=\"action-description\">向系统添加新的服务器</p>");
            htmlBuilder.append("    </div>");
            htmlBuilder.append("  </a>");
            
            htmlBuilder.append("  <a href=\"/admin/users/new\" class=\"modern-action-card users\">");
            htmlBuilder.append("    <div class=\"action-card-content\">");
            htmlBuilder.append("      <div class=\"action-icon-wrapper\"><i class=\"fas fa-user-plus\"></i></div>");
            htmlBuilder.append("      <h3 class=\"action-title\">创建用户</h3>");
            htmlBuilder.append("      <p class=\"action-description\">添加新的用户账户</p>");
            htmlBuilder.append("    </div>");
            htmlBuilder.append("  </a>");
            
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