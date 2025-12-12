package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class InitialConfigController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private ServerService serverService;
    
    @GetMapping("/initial-config")
    public String initialConfigPage(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // 如果是超级管理员，重定向到服务器管理
        if (user.getRoles().contains(User.Role.SUPER_ADMIN)) {
            return "redirect:/admin/servers";
        }

        // 如果用户已有工作目录且不是首次登录，重定向到首页
        if (!user.getIsFirstLogin() && user.getWorkDirectory() != null) {
            return "redirect:/";
        }

        // 重定向到React初始配置页面
        return "redirect:/app/initial-config";
    }
    
    @PostMapping("/initial-config")
    public String saveInitialConfig(Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
            // 超级管理员不需要设置个人工作目录
            if (user.getRoles().contains(User.Role.SUPER_ADMIN)) {
                return "redirect:/admin/servers";
            }
                
            // 获取默认服务器的工作目录
            String baseWorkDir = serverService.getDefaultBaseWorkDirectory();
            String userWorkDir = Paths.get(baseWorkDir, user.getUsername()).toString();
            
            // 创建工作目录
            Path workDirPath = Paths.get(userWorkDir);
            if (!Files.exists(workDirPath)) {
                Files.createDirectories(workDirPath);
            }
            
            // 验证是否有写权限
            if (!Files.isWritable(workDirPath)) {
                redirectAttributes.addFlashAttribute("error", "没有工作目录的写权限");
                return "redirect:/initial-config";
            }
            
            user.setWorkDirectory(userWorkDir);
            user.setIsFirstLogin(false);
            userService.save(user);
            
            redirectAttributes.addFlashAttribute("success", "工作目录配置成功！");
            return "redirect:/";
            
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "创建工作目录失败: " + e.getMessage());
            return "redirect:/initial-config";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "配置保存失败: " + e.getMessage());
            return "redirect:/initial-config";
        }
    }
}