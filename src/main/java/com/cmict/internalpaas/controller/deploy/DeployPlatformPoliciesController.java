package com.cmict.internalpaas.controller.deploy;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Deploy platform policies REST controller.
 * Provides an in-memory mock implementation aligned with the frontend contract.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/deploy-platform/policies")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DEVELOPER', 'USER')")
public class DeployPlatformPoliciesController {

    private final Map<String, PolicyDetails> policies = new ConcurrentHashMap<>();

    @PostConstruct
    void seedPolicies() {
        if (!policies.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        PolicyDetails productionApproval = new PolicyDetails();
        productionApproval.setId("policy-prod-approval");
        productionApproval.setName("生产环境部署审批");
        productionApproval.setDescription("生产环境部署需要两名审批人同意");
        productionApproval.setStatus("active");
        productionApproval.setEffect("allow");
        productionApproval.setSubjectType("role");
        productionApproval.setSubjectIds(cloneList(List.of("release-manager", "platform-admin")));
        productionApproval.setSubjectNames(cloneList(List.of("发布经理", "平台管理员")));
        productionApproval.setResourceType("release");
        productionApproval.setResourceIds(cloneList(List.of("*")));
        productionApproval.setActions(cloneList(List.of("approve", "deploy")));
        productionApproval.setPriority(100);
        productionApproval.setCreatedBy("system");
        productionApproval.setCreatedByUsername("System");
        productionApproval.setCreatedAt(now.minusDays(10));
        productionApproval.setUpdatedAt(now.minusDays(2));
        productionApproval.setTags(cloneList(List.of("production", "approval")));
        productionApproval.setSystem(true);
        savePolicy(productionApproval);

        PolicyDetails canaryPolicy = new PolicyDetails();
        canaryPolicy.setId("policy-canary-guardrails");
        canaryPolicy.setName("金丝雀发布守护策略");
        canaryPolicy.setDescription("金丝雀发布阶段必须监控关键指标");
        canaryPolicy.setStatus("active");
        canaryPolicy.setEffect("allow");
        canaryPolicy.setSubjectType("group");
        canaryPolicy.setSubjectIds(cloneList(List.of("canary-ops")));
        canaryPolicy.setSubjectNames(cloneList(List.of("金丝雀运维组")));
        canaryPolicy.setResourceType("release");
        canaryPolicy.setResourceIds(cloneList(List.of("user-service", "payment-service")));
        canaryPolicy.setActions(cloneList(List.of("deploy")));
        canaryPolicy.setConditions(cloneMap(Map.of(
            "metrics", Map.of(
                "errorRate", Map.of("max", 1.5),
                "latency", Map.of("p95", 400)
            )
        )));
        canaryPolicy.setPriority(90);
        canaryPolicy.setCreatedBy("ops-admin");
        canaryPolicy.setCreatedByUsername("Ops Admin");
        canaryPolicy.setCreatedAt(now.minusDays(8));
        canaryPolicy.setUpdatedAt(now.minusDays(1));
        canaryPolicy.setTags(cloneList(List.of("canary", "observability")));
        savePolicy(canaryPolicy);

        PolicyDetails rollbackPolicy = new PolicyDetails();
        rollbackPolicy.setId("policy-rollback-audit");
        rollbackPolicy.setName("回滚审计要求");
        rollbackPolicy.setDescription("所有回滚必须记录原因");
        rollbackPolicy.setStatus("active");
        rollbackPolicy.setEffect("allow");
        rollbackPolicy.setSubjectType("role");
        rollbackPolicy.setSubjectIds(cloneList(List.of("release-engineer")));
        rollbackPolicy.setResourceType("release");
        rollbackPolicy.setResourceIds(cloneList(List.of("*")));
        rollbackPolicy.setActions(cloneList(List.of("deploy", "delete")));
        rollbackPolicy.setPriority(80);
        rollbackPolicy.setCreatedBy("qa-lead");
        rollbackPolicy.setCreatedByUsername("QA Lead");
        rollbackPolicy.setCreatedAt(now.minusDays(5));
        rollbackPolicy.setUpdatedAt(now.minusDays(3));
        rollbackPolicy.setTags(cloneList(List.of("audit")));
        savePolicy(rollbackPolicy);

        PolicyDetails stagingAccess = new PolicyDetails();
        stagingAccess.setId("policy-staging-access");
        stagingAccess.setName("预发环境访问");
        stagingAccess.setDescription("允许测试团队管理预发环境部署");
        stagingAccess.setStatus("inactive");
        stagingAccess.setEffect("allow");
        stagingAccess.setSubjectType("role");
        stagingAccess.setSubjectIds(cloneList(List.of("qa")));
        stagingAccess.setSubjectNames(cloneList(List.of("测试团队")));
        stagingAccess.setResourceType("release");
        stagingAccess.setResourceIds(cloneList(List.of("*")));
        stagingAccess.setActions(cloneList(List.of("deploy", "read")));
        stagingAccess.setPriority(60);
        stagingAccess.setCreatedBy("qa-lead");
        stagingAccess.setCreatedByUsername("QA Lead");
        stagingAccess.setCreatedAt(now.minusDays(4));
        stagingAccess.setUpdatedAt(now.minusDays(4));
        stagingAccess.setExpiresAt(now.plusDays(10));
        stagingAccess.setTags(cloneList(List.of("staging")));
        savePolicy(stagingAccess);
    }

    @GetMapping
    public ResponseEntity<PolicyListResponse> listPolicies(
        @RequestParam(value = "status", required = false) List<String> statuses,
        @RequestParam(value = "effect", required = false) List<String> effects,
        @RequestParam(value = "subjectType", required = false) List<String> subjectTypes,
        @RequestParam(value = "subjectId", required = false) String subjectId,
        @RequestParam(value = "resourceType", required = false) List<String> resourceTypes,
        @RequestParam(value = "resourceId", required = false) String resourceId,
        @RequestParam(value = "action", required = false) List<String> actions,
        @RequestParam(value = "tag", required = false) List<String> tags,
        @RequestParam(value = "includeExpired", required = false) Boolean includeExpired,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);

        List<PolicyDetails> filtered = policies.values().stream()
            .filter(policy -> matchesFilter(statuses, policy.getStatus()))
            .filter(policy -> matchesFilter(effects, policy.getEffect()))
            .filter(policy -> matchesFilter(subjectTypes, policy.getSubjectType()))
            .filter(policy -> subjectId == null || policy.getSubjectIds().stream()
                .anyMatch(id -> id.equalsIgnoreCase(subjectId) || "*".equals(id)))
            .filter(policy -> matchesFilter(resourceTypes, policy.getResourceType()))
            .filter(policy -> resourceId == null || policy.getResourceIds().isEmpty()
                || policy.getResourceIds().stream().anyMatch(id -> id.equalsIgnoreCase(resourceId) || "*".equals(id)))
            .filter(policy -> matchesAny(policy.getActions(), actions))
            .filter(policy -> matchesAny(policy.getTags(), tags))
            .filter(policy -> Boolean.TRUE.equals(includeExpired)
                || policy.getExpiresAt() == null
                || !policy.getExpiresAt().isBefore(now))
            .sorted(Comparator.comparingInt(PolicyDetails::getPriority).reversed()
                .thenComparing(PolicyDetails::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

        int fromIndex = Math.min((safePage - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());

        List<PolicySummary> summaries = filtered.subList(fromIndex, toIndex).stream()
            .map(PolicyDetails::toSummary)
            .toList();

        PolicyListResponse response = new PolicyListResponse(
            summaries,
            filtered.size(),
            safePage,
            safePageSize,
            toIndex < filtered.size()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{policyId}")
    public ResponseEntity<PolicyDetails> getPolicy(@PathVariable String policyId) {
        PolicyDetails policy = policies.get(policyId);
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(policy);
    }

    @PostMapping
    public ResponseEntity<PolicyDetails> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String id = UUID.randomUUID().toString();

        PolicyDetails policy = new PolicyDetails();
        policy.setId(id);
        policy.setName(request.getName());
        policy.setDescription(request.getDescription());
        policy.setStatus(Optional.ofNullable(request.getStatus()).orElse("inactive"));
        policy.setEffect(request.getEffect());
        policy.setSubjectType(request.getSubjectType());
        policy.setSubjectIds(cloneList(request.getSubjectIds()));
        policy.setSubjectNames(cloneList(request.getSubjectNames()));
        policy.setResourceType(request.getResourceType());
        policy.setResourceIds(cloneList(request.getResourceIds()));
        policy.setResourceNames(cloneList(request.getResourceNames()));
        policy.setActions(cloneList(request.getActions()));
        policy.setConditions(cloneMap(request.getConditions()));
        policy.setPriority(Optional.ofNullable(request.getPriority()).orElse(50));
        policy.setCreatedBy(Optional.ofNullable(request.getCreatedBy()).orElse("system"));
        policy.setCreatedByUsername(Optional.ofNullable(request.getCreatedByUsername()).orElse("System"));
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        policy.setExpiresAt(parseDate(request.getExpiresAt()));
        policy.setTags(cloneList(request.getTags()));
        policy.setSystem(Boolean.TRUE.equals(request.getSystem()));

        savePolicy(policy);
        log.info("Created policy {}", id);
        return ResponseEntity.status(HttpStatus.CREATED).body(policy);
    }

    @PutMapping("/{policyId}")
    public ResponseEntity<PolicyDetails> updatePolicy(
        @PathVariable String policyId,
        @Valid @RequestBody UpdatePolicyRequest request
    ) {
        PolicyDetails existing = policies.get(policyId);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (request.getName() != null) {
            existing.setName(request.getName());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (request.getEffect() != null) {
            existing.setEffect(request.getEffect());
        }
        if (request.getSubjectIds() != null) {
            existing.setSubjectIds(cloneList(request.getSubjectIds()));
        }
        if (request.getSubjectNames() != null) {
            existing.setSubjectNames(cloneList(request.getSubjectNames()));
        }
        if (request.getResourceIds() != null) {
            existing.setResourceIds(cloneList(request.getResourceIds()));
        }
        if (request.getResourceNames() != null) {
            existing.setResourceNames(cloneList(request.getResourceNames()));
        }
        if (request.getActions() != null) {
            existing.setActions(cloneList(request.getActions()));
        }
        if (request.getConditions() != null) {
            existing.setConditions(cloneMap(request.getConditions()));
        }
        if (request.getPriority() != null) {
            existing.setPriority(request.getPriority());
        }
        if (request.getExpiresAt() != null) {
            existing.setExpiresAt(parseDate(request.getExpiresAt()));
        }
        if (request.getTags() != null) {
            existing.setTags(cloneList(request.getTags()));
        }

        existing.setUpdatedAt(now);

        log.info("Updated policy {}", policyId);
        return ResponseEntity.ok(existing);
    }

    @DeleteMapping("/{policyId}")
    public ResponseEntity<Void> deletePolicy(@PathVariable String policyId) {
        if (policies.remove(policyId) == null) {
            return ResponseEntity.notFound().build();
        }
        log.info("Deleted policy {}", policyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{policyId}/activate")
    public ResponseEntity<PolicyDetails> activatePolicy(@PathVariable String policyId) {
        return updateStatus(policyId, "active");
    }

    @PostMapping("/{policyId}/deactivate")
    public ResponseEntity<PolicyDetails> deactivatePolicy(@PathVariable String policyId) {
        return updateStatus(policyId, "inactive");
    }

    @PostMapping("/evaluate")
    public ResponseEntity<PolicyEvaluationResult> evaluatePolicy(@Valid @RequestBody PolicyEvaluationRequest request) {
        List<PolicyDetails> applicable = policies.values().stream()
            .filter(policy -> "active".equalsIgnoreCase(policy.getStatus()))
            .filter(policy -> subjectsMatch(policy.getSubjectIds(), request.getUserId()))
            .filter(policy -> resourceMatches(policy, request.getResourceType(), request.getResourceId()))
            .toList();

        List<String> allowPolicies = applicable.stream()
            .filter(policy -> "allow".equalsIgnoreCase(policy.getEffect()))
            .filter(policy -> actionMatches(policy.getActions(), request.getAction()))
            .map(PolicyDetails::getId)
            .toList();

        List<String> denyPolicies = applicable.stream()
            .filter(policy -> "deny".equalsIgnoreCase(policy.getEffect()))
            .filter(policy -> actionMatches(policy.getActions(), request.getAction()))
            .map(PolicyDetails::getId)
            .toList();

        boolean allowed = !allowPolicies.isEmpty() && denyPolicies.isEmpty();

        Set<String> effectivePermissions = applicable.stream()
            .filter(policy -> "allow".equalsIgnoreCase(policy.getEffect()))
            .flatMap(policy -> policy.getActions().stream())
            .map(String::toLowerCase)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        PolicyEvaluationResult result = new PolicyEvaluationResult(
            allowed,
            allowed ? "Allowed by matching policy" : "Denied by policy configuration",
            allowPolicies,
            denyPolicies,
            new ArrayList<>(effectivePermissions)
        );

        log.info("Evaluated policy for user {} on {}:{} -> {}",
            request.getUserId(),
            request.getResourceType(),
            Optional.ofNullable(request.getResourceId()).orElse("*"),
            allowed ? "allowed" : "denied");

        return ResponseEntity.ok(result);
    }

    @GetMapping("/users/{userId}/permissions")
    public ResponseEntity<UserPermissions> getUserPermissions(@PathVariable String userId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        List<PolicyDetails> effectivePolicies = policies.values().stream()
            .filter(policy -> "active".equalsIgnoreCase(policy.getStatus()))
            .filter(policy -> subjectsMatch(policy.getSubjectIds(), userId))
            .toList();

        Map<String, List<String>> permissionsByResource = new LinkedHashMap<>();
        for (PolicyDetails policy : effectivePolicies) {
            List<String> resourceTargets = policy.getResourceIds().isEmpty()
                ? List.of(policy.getResourceType())
                : policy.getResourceIds();

            for (String resource : resourceTargets) {
                String key = "*".equals(resource) ? policy.getResourceType() : resource;
                permissionsByResource.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .addAll(policy.getActions());
            }
        }

        permissionsByResource.replaceAll((key, value) -> value.stream()
            .map(String::toLowerCase)
            .distinct()
            .toList());

        boolean isAdmin = effectivePolicies.stream()
            .anyMatch(policy -> policy.getActions().stream()
                .map(String::toLowerCase)
                .anyMatch(action -> action.equals("manage") || action.equals("*"))
                && ("*".equals(policy.getResourceType()) || policy.getResourceIds().contains("*")));

        UserPermissions permissions = new UserPermissions(
            userId,
            userId,
            effectivePolicies,
            permissionsByResource,
            isAdmin,
            now
        );

        return ResponseEntity.ok(permissions);
    }

    private ResponseEntity<PolicyDetails> updateStatus(String policyId, String status) {
        PolicyDetails policy = policies.get(policyId);
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }
        policy.setStatus(status);
        policy.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        log.info("Policy {} status changed to {}", policyId, status);
        return ResponseEntity.ok(policy);
    }

    private boolean matchesFilter(List<String> filters, String candidate) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        if (candidate == null) {
            return false;
        }
        String normalized = candidate.toLowerCase();
        return filters.stream()
            .map(String::toLowerCase)
            .anyMatch(value -> value.equals(normalized) || value.equals("*"));
    }

    private boolean matchesAny(List<String> values, List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        if (values == null || values.isEmpty()) {
            return false;
        }
        Set<String> normalizedValues = values.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        return filters.stream()
            .map(String::toLowerCase)
            .anyMatch(filter -> normalizedValues.contains(filter) || normalizedValues.contains("*"));
    }

    private boolean subjectsMatch(List<String> subjectIds, String userId) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return false;
        }
        String normalizedUser = userId.toLowerCase();
        return subjectIds.stream()
            .map(String::toLowerCase)
            .anyMatch(id -> id.equals("*") || id.equals(normalizedUser));
    }

    private boolean resourceMatches(PolicyDetails policy, String resourceType, String resourceId) {
        if (!matchesFilter(List.of(Optional.ofNullable(resourceType).orElse(policy.getResourceType())), policy.getResourceType())) {
            return false;
        }
        if (resourceId == null || policy.getResourceIds().isEmpty()) {
            return true;
        }
        return policy.getResourceIds().stream()
            .anyMatch(id -> id.equals("*") || id.equalsIgnoreCase(resourceId));
    }

    private boolean actionMatches(List<String> actions, String action) {
        if (action == null) {
            return false;
        }
        return actions.stream()
            .map(String::toLowerCase)
            .anyMatch(value -> value.equals("*") || value.equals(action.toLowerCase()));
    }

    private void savePolicy(PolicyDetails policy) {
        policies.put(policy.getId(), policy);
    }

    private static List<String> cloneList(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private static Map<String, Object> cloneMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private OffsetDateTime parseDate(String isoString) {
        if (isoString == null || isoString.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(isoString);
        } catch (Exception ex) {
            log.warn("Failed to parse ISO date '{}': {}", isoString, ex.getMessage());
            return null;
        }
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PolicyDetails {
        private String id;
        private String name;
        private String description;
        private String status;
        private String effect;
        private String subjectType;
        private List<String> subjectIds = new ArrayList<>();
        private List<String> subjectNames = new ArrayList<>();
        private String resourceType;
        private List<String> resourceIds = new ArrayList<>();
        private List<String> resourceNames = new ArrayList<>();
        private List<String> actions = new ArrayList<>();
        private Map<String, Object> conditions = new LinkedHashMap<>();
        private int priority;
        private String createdBy;
        private String createdByUsername;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private OffsetDateTime expiresAt;
        private List<String> tags = new ArrayList<>();
        private boolean system;

        PolicySummary toSummary() {
            return new PolicySummary(
                id,
                name,
                status,
                effect,
                subjectType,
                subjectIds.size(),
                resourceType,
                resourceIds.isEmpty() ? 0 : resourceIds.size(),
                createdAt,
                expiresAt
            );
        }
    }

    @Data
    @AllArgsConstructor
    public static class PolicySummary {
        private String id;
        private String name;
        private String status;
        private String effect;
        private String subjectType;
        private int subjectCount;
        private String resourceType;
        private int resourceCount;
        private OffsetDateTime createdAt;
        private OffsetDateTime expiresAt;
    }

    @Data
    @AllArgsConstructor
    public static class PolicyListResponse {
        private List<PolicySummary> policies;
        private long total;
        private int page;
        private int pageSize;
        private boolean hasMore;
    }

    @Data
    @AllArgsConstructor
    public static class PolicyEvaluationResult {
        private boolean allowed;
        private String reason;
        private List<String> matchedPolicies;
        private List<String> denyPolicies;
        private List<String> effectivePermissions;
    }

    @Data
    @AllArgsConstructor
    public static class UserPermissions {
        private String userId;
        private String username;
        private List<PolicyDetails> effectivePolicies;
        private Map<String, List<String>> permissionsByResource;
        private boolean isAdmin;
        private OffsetDateTime lastEvaluatedAt;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreatePolicyRequest {
        @NotBlank
        private String name;
        private String description;
        private String status;
        @NotBlank
        private String effect;
        @NotBlank
        private String subjectType;
        @NotEmpty
        private List<String> subjectIds;
        private List<String> subjectNames;
        @NotBlank
        private String resourceType;
        private List<String> resourceIds;
        private List<String> resourceNames;
        @NotEmpty
        private List<String> actions;
        private Map<String, Object> conditions;
        private Integer priority;
        private String createdBy;
        private String createdByUsername;
        private String expiresAt;
        private List<String> tags;
        private Boolean system;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UpdatePolicyRequest {
        private String name;
        private String description;
        private String status;
        private String effect;
        private List<String> subjectIds;
        private List<String> subjectNames;
        private List<String> resourceIds;
        private List<String> resourceNames;
        private List<String> actions;
        private Map<String, Object> conditions;
        private Integer priority;
        private String expiresAt;
        private List<String> tags;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PolicyEvaluationRequest {
        @NotBlank
        private String userId;
        @NotBlank
        private String action;
        @NotBlank
        private String resourceType;
        private String resourceId;
        private Map<String, Object> context;
    }
}
