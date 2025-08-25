package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.AdminDashboardDto;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    public String adminDashboard(Authentication authentication, Model model) {
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
            
            // 获取管理员工作台数据
            AdminDashboardDto adminData = dashboardService.getAdminDashboardData();
            model.addAttribute("adminData", adminData);
        }
        return "admin-dashboard";
    }
}