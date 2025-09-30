package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.AdminDashboardDto;
import com.cmict.internalpaas.dto.DeveloperDashboardDto;
import com.cmict.internalpaas.dto.NavigationFooterLinkDto;
import com.cmict.internalpaas.dto.NavigationItemDto;
import com.cmict.internalpaas.dto.NavigationMetaEntryDto;
import com.cmict.internalpaas.dto.NavigationSectionDto;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.DashboardService;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.UserService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MainLayoutController {

    private enum WorkspaceMode {
        ADMIN,
        DEVELOPER,
        USER
    }

    @Autowired
    private UserService userService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ServerService serverService;

    /**
     * 管理员工作空间主框架。
     */
    @GetMapping("/admin/workspace")
    public String adminWorkspace(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        model.addAttribute("username", authentication.getName());
        model.addAttribute("userType", "admin");

        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null) {
            boolean isAdmin = userService.hasRole(user, User.Role.ADMIN) || userService.hasRole(user, User.Role.SUPER_ADMIN);
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isSuperAdmin", userService.hasRole(user, User.Role.SUPER_ADMIN));
        }

        configureNavigation(model, WorkspaceMode.ADMIN);
        return "main-layout";
    }

    /**
     * 开发者工作空间主框架。
     */
    @GetMapping("/developer/workspace")
    public String developerWorkspace(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        model.addAttribute("username", authentication.getName());
        model.addAttribute("userType", "developer");

        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null) {
            boolean isAdmin = userService.hasRole(user, User.Role.ADMIN) || userService.hasRole(user, User.Role.SUPER_ADMIN);
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isSuperAdmin", userService.hasRole(user, User.Role.SUPER_ADMIN));
        }

        configureNavigation(model, WorkspaceMode.DEVELOPER);
        return "main-layout";
    }

    /**
     * 通用工作空间入口，当前默认展示开发者布局。
     */
    @GetMapping("/workspace")
    public String workspace(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        model.addAttribute("username", authentication.getName());
        model.addAttribute("userType", "user");

        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null) {
            boolean isAdmin = userService.hasRole(user, User.Role.ADMIN) || userService.hasRole(user, User.Role.SUPER_ADMIN);
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isSuperAdmin", userService.hasRole(user, User.Role.SUPER_ADMIN));
        }

        configureNavigation(model, WorkspaceMode.USER);
        return "main-layout";
    }

    /**
     * 管理员仪表盘内容。
     */
    @GetMapping("/admin/api/workspace-dashboard-content")
    @ResponseBody
    public ResponseEntity<String> getAdminDashboardContent(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("<div class=\"alert alert-warning\">请先登录</div>");
        }

        try {
            AdminDashboardDto adminData = dashboardService.getAdminDashboardData();
            List<Server> servers = serverService.getAllServers();

            StringBuilder htmlBuilder = new StringBuilder();

            htmlBuilder.append("<section class='dashboard-summary'>");
            htmlBuilder.append("  <h2>平台概览</h2>");
            htmlBuilder.append("  <ul class='summary-cards'>");
            htmlBuilder.append("    <li><strong>")
                .append(adminData.getTotalServers())
                .append("</strong><span>服务器总数</span></li>");
            htmlBuilder.append("    <li><strong>")
                .append(adminData.getActiveServers())
                .append("</strong><span>活跃服务器</span></li>");
            htmlBuilder.append("    <li><strong>")
                .append(adminData.getMonitoringServers())
                .append("</strong><span>监控覆盖</span></li>");
            htmlBuilder.append("    <li><strong>")
                .append(adminData.getTotalUsers())
                .append("</strong><span>用户总数</span></li>");
            htmlBuilder.append("  </ul>");
            htmlBuilder.append("</section>");

            htmlBuilder.append("<section class='server-status-section'>");
            htmlBuilder.append("  <h2>服务器清单</h2>");
            htmlBuilder.append("  <ul class='server-simple-list'>");
            for (Server server : servers) {
                htmlBuilder.append("    <li><strong>")
                    .append(server.getName())
                    .append("</strong><span>")
                    .append(server.getHostname())
                    .append(":")
                    .append(server.getPort())
                    .append("</span></li>");
            }
            if (servers.isEmpty()) {
                htmlBuilder.append("    <li class='empty'>暂无服务器数据</li>");
            }
            htmlBuilder.append("  </ul>");
            htmlBuilder.append("</section>");

            return ResponseEntity.ok(htmlBuilder.toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("<div class=\"alert alert-danger\">加载工作台内容失败: " + e.getMessage() + "</div>");
        }
    }

    /**
     * 开发者仪表盘实时数据。
     */
    @GetMapping("/api/developer/dashboard/stats")
    @ResponseBody
    public ResponseEntity<DeveloperDashboardDto> getDeveloperDashboardStats(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            DeveloperDashboardDto data = dashboardService.getDeveloperDashboardData(authentication.getName());
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    private void configureNavigation(Model model, WorkspaceMode mode) {
        List<NavigationSectionDto> sections = new ArrayList<>();
        List<NavigationFooterLinkDto> footerLinks = new ArrayList<>();
        List<NavigationMetaEntryDto> footerMeta = new ArrayList<>();

        switch (mode) {
            case ADMIN:
                sections.add(
                    NavigationSectionDto.builder()
                        .title("工作空间")
                        .items(Arrays.asList(
                            createMenuItem("工作区概览", "overview", "overview", "OV", true),
                            createMenuItem("服务器群组", "servers", "servers", "SV", false),
                            createMenuItem("服务器群组管理", "server-groups", "server-groups", "SG", false),
                            createMenuItem("用户与权限", "users", "users", "US", false),
                            createMenuItem("系统设置", "settings", "settings", "SC", false)
                        ))
                        .build()
                );
                footerLinks.add(new NavigationFooterLinkDto("文档中心", "DC", "/docs"));
                footerLinks.add(new NavigationFooterLinkDto("支持服务", "SP", "/support"));
                footerMeta.add(new NavigationMetaEntryDto("当前环境", "DEV"));
                footerMeta.add(new NavigationMetaEntryDto("系统版本", "v2.1.0"));
                break;
            case DEVELOPER:
            case USER:
                sections.add(
                    NavigationSectionDto.builder()
                        .title("工作空间")
                        .items(Arrays.asList(
                            createMenuItem("应用总览", "applications", "applications", "AP", true),
                            createMenuItem("部署流水线", "deployments", "deployments", "DP", false),
                            createMenuItem("个人档案", "profile", "profile", "PF", false)
                        ))
                        .build()
                );
                sections.add(
                    NavigationSectionDto.builder()
                        .title("资源中心")
                        .items(Arrays.asList(
                            createMenuItem("日志中心", "logs", "logs", "LG", false),
                            createMenuItem("环境面板", "environments", "environments", "EV", false),
                            createMenuItem("集成管理", "integrations", "integrations", "IN", false)
                        ))
                        .build()
                );
                footerLinks.add(new NavigationFooterLinkDto("API 文档", "API", "/docs/api"));
                footerLinks.add(new NavigationFooterLinkDto("意见反馈", "FB", "/feedback"));
                footerLinks.add(new NavigationFooterLinkDto("CI 状态", "CI", "/ci-status"));
                break;
            default:
                break;
        }

        model.addAttribute("navSections", sections);
        model.addAttribute("sidebarFooterLinks", footerLinks.isEmpty() ? Collections.emptyList() : footerLinks);
        model.addAttribute("sidebarFooterMeta", footerMeta.isEmpty() ? Collections.emptyList() : footerMeta);
    }

    private NavigationItemDto createMenuItem(String label, String route, String page, String badge, boolean active) {
        return NavigationItemDto.builder()
            .label(label)
            .route(route)
            .page(page)
            .badge(badge)
            .active(active)
            .build();
    }
}
