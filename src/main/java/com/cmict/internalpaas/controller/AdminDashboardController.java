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