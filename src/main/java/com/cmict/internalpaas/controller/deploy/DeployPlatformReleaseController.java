package com.cmict.internalpaas.controller.deploy;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Deploy Platform release operations REST controller.
 * Provides a temporary in-memory implementation so the React UI stops failing with 404s.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/deploy-platform/releases")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DEVELOPER', 'USER')")
public class DeployPlatformReleaseController {

    private final Map<String, ReleaseDetails> releases = new ConcurrentHashMap<>();

    @PostConstruct
    void seedSampleReleases() {
        if (!releases.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        ReleaseDetails releaseOne = new ReleaseDetails();
        releaseOne.setId("rel-2025-001");
        releaseOne.setVersion("1.4.0");
        releaseOne.setName("User Service Blue-Green");
        releaseOne.setDescription("Introduce blue-green rollout for user-service");
        releaseOne.setStatus("deployed");
        releaseOne.setPriority("high");
        releaseOne.setEnvironment("production");
        releaseOne.setApplicationId("user-service");
        releaseOne.setApplicationName("user-service");
        releaseOne.setCreatedBy("ops-admin");
        releaseOne.setCreatedByUsername("Ops Admin");
        releaseOne.setApprovedBy("lead-qa");
        releaseOne.setApprovedByUsername("Lead QA");
        releaseOne.setCreatedAt(now.minusDays(6));
        releaseOne.setUpdatedAt(now.minusDays(1));
        releaseOne.setDeployedAt(now.minusDays(1));
        releaseOne.setReleaseNotes("- rollout blue-green\n- add feature flags");
        releaseOne.setGitCommit("4c0ffeecafedeadbeef");
        releaseOne.setBuildNumber("build-1204");
        releaseOne.setArtifacts(List.of(
            new ReleaseArtifact("art-001", "docker", "user-service:1.4.0", "https://registry.example.com/user-service:1.4.0", 128_000_000L, "sha256:123", now.minusDays(2))
        ));
        releaseOne.setConfig(cloneMap(Map.of("canary", Map.of("percentage", 20)))); // minimal demo override
        releaseOne.setTags(cloneList(List.of("blue-green", "production")));

        ReleaseDetails releaseTwo = new ReleaseDetails();
        releaseTwo.setId("rel-2025-002");
        releaseTwo.setVersion("2.1.0-beta");
        releaseTwo.setName("Payment Service Beta");
        releaseTwo.setDescription("Beta rollout for payment-service refactor");
        releaseTwo.setStatus("pending");
        releaseTwo.setPriority("critical");
        releaseTwo.setEnvironment("staging");
        releaseTwo.setApplicationId("payment-service");
        releaseTwo.setApplicationName("payment-service");
        releaseTwo.setCreatedBy("product-owner");
        releaseTwo.setCreatedByUsername("Product Owner");
        releaseTwo.setCreatedAt(now.minusDays(3));
        releaseTwo.setUpdatedAt(now.minusDays(3));
    releaseTwo.setTags(cloneList(List.of("payment", "beta")));

        releases.put(releaseOne.getId(), releaseOne);
        releases.put(releaseTwo.getId(), releaseTwo);

        log.info("Seeded {} in-memory release entries for deploy platform demo", releases.size());
    }

    @GetMapping
    public ResponseEntity<ReleaseListResponse> listReleases(
        @RequestParam(value = "status", required = false) List<String> status,
        @RequestParam(value = "environment", required = false) List<String> environments,
        @RequestParam(value = "priority", required = false) List<String> priorities,
        @RequestParam(value = "applicationId", required = false) String applicationId,
        @RequestParam(value = "createdBy", required = false) String createdBy,
        @RequestParam(value = "tag", required = false) List<String> tags,
        @RequestParam(value = "fromDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
        @RequestParam(value = "toDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);

        List<ReleaseDetails> filtered = releases.values().stream()
            .filter(release -> matchesFilter(status, release.getStatus()))
            .filter(release -> matchesFilter(environments, release.getEnvironment()))
            .filter(release -> matchesFilter(priorities, release.getPriority()))
            .filter(release -> applicationId == null || applicationId.equalsIgnoreCase(release.getApplicationId()))
            .filter(release -> createdBy == null || createdBy.equalsIgnoreCase(release.getCreatedBy()))
            .filter(release -> tags == null || tags.isEmpty() || release.getTags().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet())
                .containsAll(tags.stream().map(String::toLowerCase).collect(Collectors.toSet())))
            .filter(release -> fromDate == null || !release.getCreatedAt().isBefore(fromDate))
            .filter(release -> toDate == null || !release.getCreatedAt().isAfter(toDate))
            .sorted(Comparator.comparing(ReleaseDetails::getCreatedAt).reversed())
            .toList();

        int fromIndex = Math.min((safePage - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());

        List<ReleaseSummary> summaries = filtered.subList(fromIndex, toIndex).stream()
            .map(ReleaseDetails::toSummary)
            .toList();

        ReleaseListResponse response = new ReleaseListResponse(
            summaries,
            filtered.size(),
            safePage,
            safePageSize,
            toIndex < filtered.size()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{releaseId}")
    public ResponseEntity<ReleaseDetails> getRelease(@PathVariable String releaseId) {
        ReleaseDetails release = releases.get(releaseId);
        if (release == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(release);
    }

    @PostMapping
    public ResponseEntity<ReleaseDetails> createRelease(@Valid @RequestBody CreateReleaseRequest request) {
        String id = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        ReleaseDetails release = new ReleaseDetails();
        release.setId(id);
        release.setVersion(request.getVersion());
        release.setName(request.getName());
        release.setDescription(request.getDescription());
        release.setStatus("pending");
        release.setPriority(request.getPriority());
        release.setEnvironment(request.getEnvironment());
        release.setApplicationId(request.getApplicationId());
        release.setApplicationName(request.getApplicationId());
        release.setCreatedBy(Optional.ofNullable(request.getCreatedBy()).orElse("system"));
        release.setCreatedByUsername(Optional.ofNullable(request.getCreatedByUsername()).orElse("System"));
        release.setCreatedAt(now);
        release.setUpdatedAt(now);
        release.setReleaseNotes(request.getReleaseNotes());
        release.setGitCommit(request.getGitCommit());
        release.setBuildNumber(request.getBuildNumber());
        release.setConfig(cloneMap(request.getConfig()));
        release.setTags(cloneList(request.getTags()));
        release.setArtifacts(new ArrayList<>());

        releases.put(id, release);
        log.info("Created release {} with version {}", id, release.getVersion());

        return ResponseEntity.status(HttpStatus.CREATED).body(release);
    }

    @PutMapping("/{releaseId}")
    public ResponseEntity<ReleaseDetails> updateRelease(
        @PathVariable String releaseId,
        @Valid @RequestBody UpdateReleaseRequest request
    ) {
        ReleaseDetails existing = releases.get(releaseId);
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
            if ("deployed".equalsIgnoreCase(request.getStatus())) {
                existing.setDeployedAt(now);
            }
        }
        if (request.getPriority() != null) {
            existing.setPriority(request.getPriority());
        }
        if (request.getReleaseNotes() != null) {
            existing.setReleaseNotes(request.getReleaseNotes());
        }
        if (request.getConfig() != null) {
            existing.setConfig(cloneMap(request.getConfig()));
        }
        if (request.getTags() != null) {
            existing.setTags(cloneList(request.getTags()));
        }

        existing.setUpdatedAt(now);

        log.info("Updated release {}", releaseId);
        return ResponseEntity.ok(existing);
    }

    @DeleteMapping("/{releaseId}")
    public ResponseEntity<Void> deleteRelease(@PathVariable String releaseId) {
        if (releases.remove(releaseId) == null) {
            return ResponseEntity.notFound().build();
        }

        log.info("Deleted release {}", releaseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{releaseId}/deploy")
    public ResponseEntity<ReleaseDeploymentResult> deployRelease(@PathVariable String releaseId) {
        ReleaseDetails release = releases.get(releaseId);
        if (release == null) {
            return ResponseEntity.notFound().build();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        release.setStatus("deployed");
        release.setDeployedAt(now);
        release.setUpdatedAt(now);

        ReleaseDeploymentResult result = new ReleaseDeploymentResult(
            releaseId,
            "success",
            "Deployment simulated successfully",
            now,
            List.of("Deployment workflow executed (simulated)")
        );

        log.info("Simulated deployment for release {}", releaseId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{releaseId}/approve")
    public ResponseEntity<ReleaseDetails> approveRelease(@PathVariable String releaseId) {
        ReleaseDetails release = releases.get(releaseId);
        if (release == null) {
            return ResponseEntity.notFound().build();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        release.setStatus("approved");
        release.setApprovedBy("system-approver");
        release.setApprovedByUsername("System Approver");
        release.setUpdatedAt(now);

        log.info("Approved release {}", releaseId);
        return ResponseEntity.ok(release);
    }

    private boolean matchesFilter(List<String> values, String candidate) {
        if (values == null || values.isEmpty()) {
            return true;
        }
        if (candidate == null) {
            return false;
        }
        String normalized = candidate.toLowerCase();
        return values.stream().map(String::toLowerCase).anyMatch(value -> value.equals(normalized));
    }

    private List<String> cloneList(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private Map<String, Object> cloneMap(Map<String, Object> source) {
        return source == null ? Map.of() : new LinkedHashMap<>(source);
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReleaseDetails {
        private String id;
        private String version;
        private String name;
        private String description;
        private String status;
        private String priority;
        private String environment;
        private String applicationId;
        private String applicationName;
        private String createdBy;
        private String createdByUsername;
        private String approvedBy;
        private String approvedByUsername;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private OffsetDateTime deployedAt;
        private String releaseNotes;
        private String gitCommit;
        private String buildNumber;
        private List<ReleaseArtifact> artifacts = new ArrayList<>();
        private Map<String, Object> config = Map.of();
        private List<String> tags = new ArrayList<>();

        ReleaseSummary toSummary() {
            return new ReleaseSummary(
                id,
                version,
                name,
                status,
                environment,
                Optional.ofNullable(applicationName).orElse(applicationId),
                createdAt,
                deployedAt
            );
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReleaseArtifact {
        private String id;
        private String type;
        private String filename;
        private String url;
        private Long size;
        private String checksum;
        private OffsetDateTime uploadedAt;
    }

    @Data
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReleaseSummary {
        private String id;
        private String version;
        private String name;
        private String status;
        private String environment;
        private String applicationName;
        private OffsetDateTime createdAt;
        private OffsetDateTime deployedAt;
    }

    @Data
    @AllArgsConstructor
    public static class ReleaseListResponse {
        private List<ReleaseSummary> releases;
        private long total;
        private int page;
        private int pageSize;
        private boolean hasMore;
    }

    @Data
    @AllArgsConstructor
    public static class ReleaseDeploymentResult {
        private String releaseId;
        private String status;
        private String message;
        private OffsetDateTime deployedAt;
        private List<String> logs;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreateReleaseRequest {
        @NotBlank
        private String version;
        @NotBlank
        private String name;
        private String description;
        @NotNull
        private String priority;
        @NotNull
        private String environment;
        @NotBlank
        private String applicationId;
        private String releaseNotes;
        private String gitCommit;
        private String buildNumber;
        private Map<String, Object> config;
        private List<String> tags;
        private String createdBy;
        private String createdByUsername;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UpdateReleaseRequest {
        private String name;
        private String description;
        private String status;
        private String priority;
        private String releaseNotes;
        private Map<String, Object> config;
        private List<String> tags;
    }
}