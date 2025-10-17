package com.cmict.internalpaas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import com.cmict.internalpaas.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import com.cmict.internalpaas.dto.*;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

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
    
    // === 用户档案管理端点 ===
    
    /**
     * 显示用户档案页面
     */
    @GetMapping("/profile")
    public String showProfile(Authentication authentication, Model model) {
        String username = authentication.getName();
        
        // 获取用户档案信息
        UserProfileDto profile = userService.getUserProfile(username);
        UserPreferencesDto preferences = userService.getUserPreferences(username);
        
        model.addAttribute("profile", profile);
        model.addAttribute("preferences", preferences);
        model.addAttribute("passwordChangeDto", new PasswordChangeDto());
        
        return "user-profile";
    }
    
    /**
     * 更新用户个人信息
     */
    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("profile") UserProfileDto profileDto,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            userService.updateUserProfile(username, profileDto);
            redirectAttributes.addFlashAttribute("successMessage", "个人信息更新成功！");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "更新失败：" + e.getMessage());
        }
        return "redirect:/profile";
    }
    
    /**
     * 修改密码
     */
    @PostMapping("/profile/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordChangeDto") PasswordChangeDto passwordChangeDto,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            userService.changePassword(username, passwordChangeDto);
            redirectAttributes.addFlashAttribute("successMessage", "密码修改成功！");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "密码修改失败：" + e.getMessage());
        }
        return "redirect:/profile";
    }
    
    /**
     * 更新用户偏好设置
     */
    @PostMapping("/profile/preferences")
    @ResponseBody
    public ResponseEntity<String> updatePreferences(@Valid @ModelAttribute("preferences") UserPreferencesDto preferencesDto,
                                                   Authentication authentication) {
        try {
            String username = authentication.getName();
            userService.updateUserPreferences(username, preferencesDto);
            return ResponseEntity.ok("偏好设置更新成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新失败：" + e.getMessage());
        }
    }
    
    /**
     * API端点：获取用户档案信息（用于AJAX调用）
     */
    @GetMapping("/api/profile")
    @ResponseBody
    public ResponseEntity<UserProfileDto> getProfileAPI(Authentication authentication) {
        try {
            String username = authentication.getName();
            UserProfileDto profile = userService.getUserProfile(username);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * API端点：获取用户偏好设置（用于AJAX调用）
     */
    @GetMapping("/api/profile/preferences")
    @ResponseBody
    public ResponseEntity<UserPreferencesDto> getPreferencesAPI(Authentication authentication) {
        try {
            String username = authentication.getName();
            UserPreferencesDto preferences = userService.getUserPreferences(username);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
