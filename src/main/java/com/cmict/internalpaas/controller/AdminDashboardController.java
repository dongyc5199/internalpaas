package com.cmict.internalpaas.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    @GetMapping
    public String adminDashboard(Authentication authentication, Model model) {
        // 重定向到新的管理员工作空间
        return "redirect:/admin/workspace";
    }
}