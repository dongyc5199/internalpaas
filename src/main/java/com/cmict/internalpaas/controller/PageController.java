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
        return "login";
    }

    @GetMapping("/")
    public String index(Authentication authentication, Model model) {
        if (isAuthenticated(authentication)) {
            if (hasAnyAuthority(authentication, "ROLE_SUPER_ADMIN", "ROLE_ADMIN")) {
                return "redirect:/admin/workspace#dashboard";
            }
            if (hasAnyAuthority(authentication, "ROLE_DEVELOPER", "ROLE_USER")) {
                return "redirect:/developer/workspace";
            }
            model.addAttribute("username", authentication.getName());
        }
        return "index";
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