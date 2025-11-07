package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.AdminDashboardDto;
import com.cmict.internalpaas.service.DashboardService;
import com.cmict.internalpaas.service.SystemHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 管理员工作台控制器
 * 提供管理员仪表板页面和数据API
 */
@Controller
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private SystemHealthService systemHealthService;

    /**
     * 管理员仪表板页面
     * 保持现有的页面重定向功能
     */
    @GetMapping
    public String adminDashboard(Authentication authentication, Model model) {
        // 重定向到新的管理员工作空间
        return "redirect:/admin/workspace";
    }

    /**
     * 返回管理员仪表板HTML内容片段
     * 用于SPA路由系统加载
     */
    @GetMapping("/content")
    public String adminDashboardContent() {
        return "admin/admin-dashboard-content :: admin-dashboard-content";
    }

    /**
     * 获取仪表板总览数据 (JSON)
     * 遵循现有API路径规范: /admin/dashboard/overview
     */
    @GetMapping("/overview")
    @ResponseBody
    public ResponseEntity<AdminDashboardDto> getDashboardOverview() {
        try {
            AdminDashboardDto dashboardData = dashboardService.getDashboardOverview();
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取仪表板数据 - React前端格式 (JSON)
     * 路径: /admin/dashboard/data
     * 返回符合React前端DashboardData类型的数据结构
     */
    @GetMapping("/data")
    @ResponseBody
    public ResponseEntity<?> getDashboardData() {
        try {
            AdminDashboardDto adminData = dashboardService.getDashboardOverview();

            // 转换为React前端期望的格式
            var response = new java.util.HashMap<String, Object>();

            // overview部分
            var overview = new java.util.HashMap<String, Object>();
            overview.put("totalServers", adminData.getTotalServers());
            overview.put("onlineServers", adminData.getActiveServers());
            overview.put("totalApplications", adminData.getTotalApplications());
            overview.put("runningApplications", adminData.getRunningApplications());
            overview.put("totalUsers", adminData.getTotalUsers());
            overview.put("activeUsers", adminData.getTodayActiveUsers());
            overview.put("todayDeployments", 0); // 暂时返回0,后续可添加统计逻辑
            response.put("overview", overview);

            // serverDistribution部分
            var serverDist = new java.util.HashMap<String, Object>();
            serverDist.put("online", adminData.getActiveServers());
            serverDist.put("offline", adminData.getInactiveServers());
            serverDist.put("maintenance", adminData.getMonitoringServers());
            serverDist.put("error", 0);
            serverDist.put("unknown", 0);
            response.put("serverDistribution", serverDist);

            // recentActivities部分 - 转换ActivityRecord为前端期望的格式
            var activities = new java.util.ArrayList<java.util.Map<String, Object>>();
            if (adminData.getRecentActivities() != null) {
                for (var activity : adminData.getRecentActivities()) {
                    var act = new java.util.HashMap<String, Object>();
                    act.put("id", activity.hashCode()); // 使用hashCode作为临时ID
                    act.put("type", activity.getType() != null ? activity.getType() : "deployment");
                    act.put("description", activity.getDescription());
                    act.put("username", activity.getUser());
                    act.put("timestamp", activity.getTimestamp());
                    act.put("status", "success"); // 默认状态
                    activities.add(act);
                }
            }
            response.put("recentActivities", activities);

            // resourceTrends部分 - 暂时返回空数组
            response.put("resourceTrends", new java.util.ArrayList<>());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取服务器摘要数据 (JSON)
     * 遵循现有API风格: /admin/dashboard/servers/summary
     */
    @GetMapping("/servers/summary")
    @ResponseBody
    public ResponseEntity<?> getServersSummary() {
        try {
            // 调用现有的服务器服务获取摘要数据
            var serversSummary = dashboardService.getServersSummary();
            return ResponseEntity.ok(serversSummary);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取系统健康度数据 (JSON)
     * 遵循现有监控API风格: /admin/dashboard/system/health
     */
    @GetMapping("/system/health")
    @ResponseBody
    public ResponseEntity<SystemHealthService.SystemHealthDetails> getSystemHealth() {
        try {
            SystemHealthService.SystemHealthDetails healthDetails = systemHealthService.getSystemHealthDetails();
            return ResponseEntity.ok(healthDetails);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 刷新仪表板数据 (JSON)
     * 遵循现有操作风格: /admin/dashboard/refresh
     */
    @PostMapping("/refresh")
    @ResponseBody
    public ResponseEntity<String> refreshDashboard() {
        try {
            // 触发数据刷新
            dashboardService.refreshDashboardData();
            return ResponseEntity.ok("{\"message\":\"仪表板数据刷新成功\"}");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"仪表板数据刷新失败: " + e.getMessage() + "\"}");
        }
    }
}