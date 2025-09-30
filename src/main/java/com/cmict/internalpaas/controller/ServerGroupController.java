package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.*;
import com.cmict.internalpaas.service.ServerGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/server-groups")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class ServerGroupController {

    private static final Logger logger = LoggerFactory.getLogger(ServerGroupController.class);

    @Autowired
    private ServerGroupService serverGroupService;

    /**
     * Return HTML fragment page
     */
    @GetMapping("/content")
    public String content() {
        logger.info("Loading server group content page");
        return "admin/server-group-content";
    }

    /**
     * Get enhanced server list with monitoring data
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<ServerGroupViewDto>> getList() {
        logger.info("API: Getting server group list");
        try {
            List<ServerGroupViewDto> servers = serverGroupService.getServerGroupView();
            return ResponseEntity.ok(servers);
        } catch (Exception e) {
            logger.error("Failed to get server group list", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get 24-hour health trend
     */
    @GetMapping("/api/health-trend")
    @ResponseBody
    public ResponseEntity<HealthTrendDto> getHealthTrend(
            @RequestParam(defaultValue = "24") int hours) {
        logger.info("API: Getting health trend for last {} hours", hours);
        try {
            HealthTrendDto trend = serverGroupService.getHealthTrend(hours);
            return ResponseEntity.ok(trend);
        } catch (Exception e) {
            logger.error("Failed to get health trend", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get load distribution analysis
     */
    @GetMapping("/api/load-distribution")
    @ResponseBody
    public ResponseEntity<LoadDistributionDto> getLoadDistribution() {
        logger.info("API: Getting load distribution");
        try {
            LoadDistributionDto distribution = serverGroupService.getLoadDistribution();
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            logger.error("Failed to get load distribution", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get application deployment distribution
     */
    @GetMapping("/api/app-distribution")
    @ResponseBody
    public ResponseEntity<AppDistributionDto> getAppDistribution() {
        logger.info("API: Getting application distribution");
        try {
            AppDistributionDto distribution = serverGroupService.getAppDistribution();
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            logger.error("Failed to get application distribution", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Batch refresh servers
     */
    @PostMapping("/api/batch-refresh")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> batchRefresh(@RequestBody BatchActionRequest request) {
        logger.info("API: Batch refreshing {} servers", request.getServerIds().size());
        Map<String, Object> response = new HashMap<>();

        try {
            serverGroupService.batchRefreshServers(request.getServerIds());
            response.put("success", true);
            response.put("message", "Batch refresh completed");
            response.put("count", request.getServerIds().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to batch refresh servers", e);
            response.put("success", false);
            response.put("message", "Batch refresh failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
