package com.cmict.internalpaas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import com.cmict.internalpaas.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model; // 添加缺失的导入语句
import com.cmict.internalpaas.dto.UserRegistrationDto;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {
    
    @Autowired
    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "register"; // 返回 register.html 模板
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("user") UserRegistrationDto registrationDto, RedirectAttributes redirectAttributes) {
        try {
            // 验证密码和确认密码是否匹配
            if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
                redirectAttributes.addFlashAttribute("errorMessage", "两次输入的密码不一致！");
                return "redirect:/register";
            }
            
            // 验证邮箱格式
            if (registrationDto.getEmail() == null || !registrationDto.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                redirectAttributes.addFlashAttribute("errorMessage", "请输入有效的邮箱地址！");
                return "redirect:/register";
            }
            
            userService.registerNewUser(registrationDto);
            redirectAttributes.addFlashAttribute("successMessage", "注册成功！请登录。");
            return "redirect:/login"; // 注册成功后重定向到登录页面
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register"; // 如果用户名已存在，重定向回注册页面并显示错误
        }
    }
}
