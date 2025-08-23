package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.ApplicationService;
import com.cmict.internalpaas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/apps")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 显示应用管理页面
     */
    @GetMapping
    public String applicationsPage(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }
        
        User user = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        List<Application> applications = applicationService.getUserApplications(user);
        model.addAttribute("applications", applications);
        model.addAttribute("username", authentication.getName());
        
        // 获取当前用户的工作目录
        String workDirectory = applicationService.getUserWorkDirectory(user);
        model.addAttribute("userWorkDirectory", workDirectory);
        
        return "applications";
    }
    
    /**
     * 上传应用
     */
    @PostMapping("/upload")
    public String uploadApplication(@RequestParam("file") MultipartFile file,
                                   @RequestParam("appName") String appName,
                                   Authentication authentication,
                                   Model model) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
            if (user.getWorkDirectory() == null) {
                model.addAttribute("error", "请先配置工作目录");
                return "redirect:/initial-config";
            }
            
            applicationService.uploadApplication(user, file, appName);
            return "redirect:/apps";
            
        } catch (IOException e) {
            model.addAttribute("error", "文件上传失败: " + e.getMessage());
            return "applications";
        }
    }
    
    /**
     * 启动应用
     */
    @PostMapping("/{id}/start")
    public String startApplication(@PathVariable Long id, Authentication authentication) throws IOException {
        applicationService.startApplication(id);
        return "redirect:/apps";
    }
    
    /**
     * 停止应用
     */
    @PostMapping("/{id}/stop")
    public String stopApplication(@PathVariable Long id, Authentication authentication) {
        applicationService.stopApplication(id);
        return "redirect:/apps";
    }
    
    /**
     * 重启应用
     */
    @PostMapping("/{id}/restart")
    public String restartApplication(@PathVariable Long id, Authentication authentication) throws IOException {
        applicationService.restartApplication(id);
        return "redirect:/apps";
    }
    
    /**
     * 删除应用
     */
    @PostMapping("/{id}/delete")
    public String deleteApplication(@PathVariable Long id, Authentication authentication) throws IOException {
        applicationService.deleteApplication(id);
        return "redirect:/apps";
    }
    
    /**
     * REST API - 获取应用列表
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<Application>> getApplications(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        List<Application> applications = applicationService.getUserApplications(user);
        return ResponseEntity.ok(applications);
    }
}