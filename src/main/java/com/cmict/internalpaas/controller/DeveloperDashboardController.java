package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.DeveloperDashboardDto;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.DashboardService;
import com.cmict.internalpaas.service.UserService;
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
    private UserService userService;
    
    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    public String developerDashboard(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("username", authentication.getName());
        
        // 获取用户信息和数据
        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null) {
            model.addAttribute("isAdmin", userService.hasRole(user, User.Role.ADMIN) || userService.hasRole(user, User.Role.SUPER_ADMIN));
            model.addAttribute("isSuperAdmin", userService.hasRole(user, User.Role.SUPER_ADMIN));
        }
        
        // 获取开发者数据
        DeveloperDashboardDto developerData = dashboardService.getDeveloperDashboardData(authentication.getName());
        model.addAttribute("developerData", developerData);
        
        return "developer-dashboard";
    }
}