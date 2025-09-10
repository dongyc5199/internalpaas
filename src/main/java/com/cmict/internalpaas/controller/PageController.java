package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    
    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String login(Authentication authentication) {
        // 如果用户已登录，重定向到主页
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/")
    public String index(Authentication authentication, Model model) {
        logger.info("用户访问首页，认证状态: {}", authentication != null ? authentication.isAuthenticated() : "未认证");
        
        if (authentication != null) {
            logger.info("认证用户: {}", authentication.getName());
            model.addAttribute("username", authentication.getName());
            
            // 根据用户角色重定向到不同的工作台
            User user = userService.findByUsername(authentication.getName()).orElse(null);
            if (user != null) {
                logger.info("用户角色: {}", user.getRoles());
                if (userService.hasRole(user, User.Role.SUPER_ADMIN) || userService.hasRole(user, User.Role.ADMIN)) {
                    // 管理员角色重定向到管理员工作空间
                    logger.info("重定向到管理员工作空间");
                    return "redirect:/admin/workspace";
                } else {
                    // 研发人员角色重定向到研发工作空间
                    logger.info("重定向到研发工作空间");
                    return "redirect:/developer/workspace";
                }
            }
        }
        logger.info("返回默认首页");
        return "index";
    }
}

