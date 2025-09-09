package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.DeveloperDashboardDto;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/developer/dashboard")
public class DeveloperDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    public String developerDashboard(Authentication authentication, Model model) {
        // 重定向到新的研发工作空间
        return "redirect:/developer/workspace";
    }
}