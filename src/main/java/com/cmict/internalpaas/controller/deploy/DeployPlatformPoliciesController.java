package com.cmict.internalpaas.controller.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 部署平台策略管理API控制器 (Mock实现用于测试)
 * 提供策略列表、更新等功能，用于测试React Query缓存和乐观更新
 */
@RestController
@RequestMapping("/api/deploy-platform/policies")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DEVELOPER', 'USER')")
public class DeployPlatformPoliciesController {

    private static final Logger log = LoggerFactory.getLogger(DeployPlatformPoliciesController.class);

    // 内存存储 - Mock数据
    private static final Map<Long, Policy> POLICY_STORE = new ConcurrentHashMap<>();
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    // 静态初始化Mock数据
    static {
        initializeMockData();
    }

    private static void initializeMockData() {
        createPolicy("生产环境部署审批", "所有生产环境部署必须经过至少2名审批者同意", "active", "production", 1);
        createPolicy("金丝雀发布策略", "金丝雀部署阶段必须持续至少30分钟并验证关键指标", "active", "all", 2);
        createPolicy("回滚审计要求", "所有回滚操作必须记录原因并通知相关团队", "active", "all", 3);
        createPolicy("自动化测试门禁", "部署前必须通过完整的自动化测试套件", "inactive", "staging", 4);
        createPolicy("变更窗口限制", "生产环境部署仅允许在工作日10:00-16:00进行", "active", "production", 5);
    }

    private static void createPolicy(String name, String description, String status, String environment, int order) {
        Long id = ID_GENERATOR.getAndIncrement();
        Policy policy = new Policy();
        policy.id = id;
        policy.name = name;
        policy.description = description;
        policy.status = status;
        policy.environment = environment;
        policy.order = order;
        policy.createdAt = "2025-10-20T10:00:00Z";
        policy.updatedAt = "2025-10-30T15:30:00Z";
        POLICY_STORE.put(id, policy);
    }

    /**
     * 获取策略列表（支持分页和筛选）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPolicies(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        log.info("Fetching policies: status={}, page={}, pageSize={}", status, page, pageSize);

        // 筛选
        List<Policy> filtered = POLICY_STORE.values().stream()
                .filter(p -> status == null || p.status.equals(status))
                .sorted(Comparator.comparingInt(p -> p.order))
                .toList();

        // 分页
        int total = filtered.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<Policy> paginated = start < total ? filtered.subList(start, end) : Collections.emptyList();

        Map<String, Object> response = Map.of(
                "data", paginated,
                "pagination", Map.of(
                        "page", page,
                        "pageSize", pageSize,
                        "total", total,
                        "totalPages", (int) Math.ceil((double) total / pageSize)
                )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 获取单个策略详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Policy> getPolicy(@PathVariable Long id) {
        log.info("Fetching policy: id={}", id);

        Policy policy = POLICY_STORE.get(id);
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(policy);
    }

    /**
     * 更新策略（用于测试乐观更新）
     */
    @PutMapping("/{id}")
    public ResponseEntity<Policy> updatePolicy(@PathVariable Long id, @RequestBody PolicyUpdateRequest request) {
        log.info("Updating policy: id={}, request={}", id, request);

        Policy policy = POLICY_STORE.get(id);
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }

        // 模拟网络延迟（用于观察乐观更新效果）
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 更新字段
        if (request.name != null) policy.name = request.name;
        if (request.description != null) policy.description = request.description;
        if (request.status != null) policy.status = request.status;
        if (request.environment != null) policy.environment = request.environment;
        if (request.order != null) policy.order = request.order;

        policy.updatedAt = new Date().toInstant().toString();

        log.info("Policy updated successfully: {}", policy);
        return ResponseEntity.ok(policy);
    }

    /**
     * 切换策略状态（快捷操作，用于测试乐观更新）
     */
    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<Policy> toggleStatus(@PathVariable Long id) {
        log.info("Toggling policy status: id={}", id);

        Policy policy = POLICY_STORE.get(id);
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }

        // 模拟网络延迟
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 切换状态
        policy.status = "active".equals(policy.status) ? "inactive" : "active";
        policy.updatedAt = new Date().toInstant().toString();

        log.info("Policy status toggled: {} -> {}", id, policy.status);
        return ResponseEntity.ok(policy);
    }

    // DTO类
    public static class Policy {
        public Long id;
        public String name;
        public String description;
        public String status;
        public String environment;
        public Integer order;
        public String createdAt;
        public String updatedAt;
    }

    public static class PolicyUpdateRequest {
        public String name;
        public String description;
        public String status;
        public String environment;
        public Integer order;
    }
}
