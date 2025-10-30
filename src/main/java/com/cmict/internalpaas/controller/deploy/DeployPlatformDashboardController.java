package com.cmict.internalpaas.controller.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 部署平台仪表板API控制器
 * 提供部署概览数据、指标统计等
 */
@RestController
@RequestMapping("/api/deploy-platform/dashboard")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DEVELOPER', 'USER')")
public class DeployPlatformDashboardController {

    private static final Logger log = LoggerFactory.getLogger(DeployPlatformDashboardController.class);

    /**
     * 获取部署概览摘要数据
     *
     * @return 仪表板摘要数据
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        log.info("Fetching deployment platform dashboard summary");

        // Mock数据 - 后续可以替换为真实的数据库查询
        Map<String, Object> response = Map.of(
            "metrics", List.of(
                Map.of(
                    "id", "total-releases",
                    "title", "总发布数",
                    "value", 42,
                    "unit", "个",
                    "trend", "up",
                    "description", "本月新增发布"
                ),
                Map.of(
                    "id", "active-canary",
                    "title", "活跃金丝雀",
                    "value", 5,
                    "unit", "个",
                    "trend", "flat",
                    "description", "正在进行的金丝雀部署"
                ),
                Map.of(
                    "id", "pending-approvals",
                    "title", "待审批",
                    "value", 3,
                    "unit", "个",
                    "trend", "down",
                    "description", "等待审批的发布请求"
                ),
                Map.of(
                    "id", "success-rate",
                    "title", "成功率",
                    "value", 98,
                    "unit", "%",
                    "trend", "up",
                    "description", "过去30天发布成功率"
                )
            ),
            "highlights", List.of(
                Map.of(
                    "application", "user-service",
                    "environment", "production",
                    "status", "healthy",
                    "pendingReleases", 0,
                    "runningCanary", 0,
                    "lastUpdated", "2025-10-30T15:30:00Z"
                ),
                Map.of(
                    "application", "order-service",
                    "environment", "staging",
                    "status", "warning",
                    "pendingReleases", 1,
                    "runningCanary", 1,
                    "lastUpdated", "2025-10-30T15:25:00Z"
                ),
                Map.of(
                    "application", "payment-service",
                    "environment", "production",
                    "status", "healthy",
                    "pendingReleases", 0,
                    "runningCanary", 0,
                    "lastUpdated", "2025-10-30T15:20:00Z"
                )
            ),
            "pollingIntervalSeconds", 30
        );

        return ResponseEntity.ok(response);
    }
}
