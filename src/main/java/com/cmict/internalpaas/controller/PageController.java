package com.cmict.internalpaas.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        // 重定向到React登录页面
        return "redirect:/app/login";
    }

    @GetMapping("/")
    public String index(Authentication authentication, Model model) {
        if (isAuthenticated(authentication)) {
            // 重定向到React应用
            return "redirect:/app";
        }
        // 未登录用户显示登录页
        return "redirect:/login";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean hasAnyAuthority(Authentication authentication, String... roles) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            for (String role : roles) {
                if (authority.getAuthority().equals(role)) {
                    return true;
                }
            }
        }
        return false;
    }
}