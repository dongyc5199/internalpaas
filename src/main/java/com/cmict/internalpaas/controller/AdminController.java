package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
}