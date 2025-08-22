package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    @Autowired
    private ServerService serverService;

    @Autowired
    private UserService userService;

    @GetMapping("/servers")
    public String serverManagement(Model model) {
        List<Server> servers = serverService.getAllServers();
        model.addAttribute("servers", servers);
        return "admin/servers";
    }

    @GetMapping("/servers/new")
    public String newServerForm(Model model) {
        model.addAttribute("server", new Server());
        return "admin/server-form";
    }

    @GetMapping("/servers/{id}")
    public String serverDetail(@PathVariable Long id, Model model) {
        Server server = serverService.findById(id)
            .orElseThrow(() -> new RuntimeException("服务器未找到"));
        
        String connectionStatus = serverService.checkConnectionStatus(id);
        String connectionDescription = serverService.getConnectionStatusDescription(
            Server.ConnectionStatus.valueOf(connectionStatus));
        
        model.addAttribute("server", server);
        model.addAttribute("connectionStatus", connectionStatus);
        model.addAttribute("connectionDescription", connectionDescription);
        
        return "admin/server-detail";
    }

    @PostMapping("/servers")
    public String saveServer(@ModelAttribute Server server) {
        serverService.saveServer(server);
        return "redirect:/admin/servers";
    }

    @GetMapping("/servers/{id}/edit")
    public String editServerForm(@PathVariable Long id, Model model) {
        Server server = serverService.getServerById(id)
            .orElseThrow(() -> new RuntimeException("Server not found"));
        model.addAttribute("server", server);
        return "admin/server-form";
    }

    @PostMapping("/servers/{id}/update")
    public String updateServer(@PathVariable Long id, @ModelAttribute Server server) {
        serverService.updateServer(id, server);
        return "redirect:/admin/servers";
    }

    @PostMapping("/servers/{id}/delete")
    public String deleteServer(@PathVariable Long id) {
        serverService.deleteServer(id);
        return "redirect:/admin/servers";
    }
    
    @PostMapping("/servers/{id}/check-connection")
    public String checkServerConnection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Server server = serverService.checkServerConnection(id);
            String statusDesc = serverService.getConnectionStatusDescription(server.getConnectionStatus());
            redirectAttributes.addFlashAttribute("successMessage", 
                String.format("服务器 '%s' 连接状态: %s", server.getName(), statusDesc));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "检查连接失败: " + e.getMessage());
        }
        return "redirect:/admin/servers";
    }
    
    @PostMapping("/servers/check-all-connections")
    public String checkAllServerConnections(RedirectAttributes redirectAttributes) {
        try {
            serverService.checkAllServerConnections();
            redirectAttributes.addFlashAttribute("successMessage", "已启动所有服务器的连接检查");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "批量检查失败: " + e.getMessage());
        }
        return "redirect:/admin/servers";
    }

    @GetMapping("/users")
    public String userManagement(Model model) {
        List<User> users = userService.findAllUsers();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle-admin")
    public String toggleAdminRole(@PathVariable Long id) {
        userService.toggleAdminRole(id);
        return "redirect:/admin/users";
    }
    
    @PostMapping("/users/{id}/toggle-super-admin")
    public String toggleSuperAdminRole(@PathVariable Long id) {
        userService.toggleSuperAdminRole(id);
        return "redirect:/admin/users";
    }
    
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "用户删除成功");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}