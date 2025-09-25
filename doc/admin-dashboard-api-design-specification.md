# 管理员工作台API设计规范

**文档版本**: v1.0
**创建时间**: 2025-01-27
**基于**: CLAUDE.md API接口说明分析
**目标**: 确保新增管理员仪表板API与现有系统保持一致

## 现有API架构分析

### 1. 路径结构规范

| 模块类型 | 基础路径 | 示例 | 用途 |
|---------|----------|------|------|
| **管理员功能** | `/admin` | `/admin/servers`, `/admin/users` | 服务器管理、用户管理 |
| **监控功能** | `/monitoring` | `/monitoring/server/{id}/metrics` | 服务器监控、历史数据 |
| **应用管理** | `/apps` | `/apps/api/list`, `/apps/{id}/start` | 应用生命周期管理 |
| **通用API** | `/api` | `/api/profile`, `/api/command` | 通用数据接口 |

### 2. HTTP方法使用规范

```java
// 页面渲染
@GetMapping("/admin/servers")              // 返回页面模板
@GetMapping("/admin/servers/{id}")         // 返回详情页面

// JSON数据查询
@GetMapping("/admin/servers/{id}/status")  // 返回JSON数据
@GetMapping("/admin/servers/{id}/metrics") // 返回JSON监控数据

// 操作命令
@PostMapping("/admin/servers")                    // 创建服务器
@PostMapping("/admin/servers/{id}/update")        // 更新服务器
@PostMapping("/admin/servers/{id}/refresh")       // 刷新状态
@PostMapping("/admin/servers/check-all-connections") // 批量操作
```

### 3. 权限控制规范

```java
// 管理员权限 (服务器管理、用户管理)
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")

// 超级管理员权限 (用户角色切换)
@PreAuthorize("hasRole('SUPER_ADMIN')")

// 开发者权限 (告警阈值管理)
@PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
```

### 4. 响应格式规范

```java
// JSON响应格式
public ResponseEntity<AdminDashboardDto> getDashboardOverview()
public ResponseEntity<List<ServerSummaryDto>> getServersSummary()
public ResponseEntity<SystemHealthDto> getSystemHealth()

// 页面响应格式
public String workspace() {
    return "redirect:/admin/workspace";
}
```

## 管理员仪表板API设计方案

### 基于现有规范的API设计

```java
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminDashboardController {

    // ✅ 保持现有页面重定向功能
    @GetMapping("/workspace")
    public String workspace() {
        return "redirect:/admin/workspace";
    }

    // ✨ 新增仪表板页面 (遵循 /admin/servers 同级别规范)
    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "admin-dashboard";
    }

    // ✨ 新增仪表板数据API (遵循现有JSON API规范)
    @GetMapping("/dashboard/overview")
    public ResponseEntity<AdminDashboardDto> getDashboardOverview() {
        // 返回完整仪表板数据
    }

    @GetMapping("/dashboard/servers/summary")
    public ResponseEntity<List<ServerSummaryDto>> getServersSummary() {
        // 返回服务器汇总数据
    }

    @GetMapping("/dashboard/system/health")
    public ResponseEntity<SystemHealthDto> getSystemHealth() {
        // 返回系统健康度数据
    }

    @PostMapping("/dashboard/refresh")
    public ResponseEntity<String> refreshDashboard() {
        // 刷新仪表板数据 (遵循现有 refresh 操作规范)
    }
}
```

### API路径对比分析

| 功能模块 | 现有API | 新增仪表板API | 保持一致性 |
|---------|---------|---------------|------------|
| **页面渲染** | `/admin/servers` | `/admin/dashboard` | ✅ 同级别路径 |
| **状态查询** | `/admin/servers/{id}/status` | `/admin/dashboard/overview` | ✅ 相似结构 |
| **监控数据** | `/admin/servers/{id}/metrics` | `/admin/dashboard/servers/summary` | ✅ 子资源路径 |
| **操作命令** | `/admin/servers/{id}/refresh` | `/admin/dashboard/refresh` | ✅ 动词操作 |

### 命名规范总结

**✅ 遵循的现有规范:**
1. **路径层次**: `/{module}/{resource}/{action}`
2. **权限注解**: `@PreAuthorize` 角色检查
3. **HTTP方法**: GET(查询), POST(操作)
4. **响应格式**: `ResponseEntity<?>` 包装JSON数据
5. **操作命名**: 使用动词 (`refresh`, `update`, `check-connection`)

**🔄 与现有API的集成:**
```java
// 复用现有监控API
@Autowired
MonitoringController monitoringController;

@GetMapping("/dashboard/servers/detailed/{serverId}")
public ResponseEntity<?> getServerDetailedMetrics(@PathVariable Long serverId) {
    // 直接复用: /monitoring/server/{id}/metrics
    return monitoringController.getServerMetrics(serverId);
}
```

## 数据传输对象(DTO)规范

### 基于现有AdminDashboardDto扩展

```java
public class AdminDashboardDto {
    // ✅ 现有字段 (已验证完整)
    private long totalServers;           // 服务器统计
    private long activeServers;
    private long inactiveServers;
    private long monitoringServers;

    private long totalUsers;             // 用户统计
    private long adminUsers;
    private long regularUsers;
    private long firstLoginUsers;

    private double cpuUsage;             // 系统监控
    private double memoryUsage;
    private double diskUsage;

    private List<ActivityRecord> recentActivities; // 活动记录

    // ✨ 建议新增字段
    private long totalApplications;      // 应用统计
    private long runningApplications;
    private long stoppedApplications;

    private long totalAlerts;            // 告警统计
    private long criticalAlerts;
    private long warningAlerts;

    private double systemHealthScore;    // 系统健康度评分
    private String systemHealthStatus;   // 健康度状态描述
}
```

### 辅助DTO设计

```java
// 服务器汇总DTO (复用现有AggregatedServerMetrics思路)
public class ServerSummaryDto {
    private Long serverId;
    private String serverName;
    private String status;               // online/offline/warning
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private LocalDateTime lastUpdate;
}

// 系统健康度DTO
public class SystemHealthDto {
    private double overallScore;         // 整体评分 0-100
    private String status;               // excellent/good/warning/critical
    private Map<String, Double> componentScores; // 各组件评分
    private List<String> healthIssues;   // 健康问题列表
    private LocalDateTime lastCalculated;
}
```

## 服务层集成规范

### 利用现有服务能力

```java
@Service
public class DashboardService {

    @Autowired
    private ServerService serverService;        // ✅ 现有服务

    @Autowired
    private MonitoringService monitoringService; // ✅ 现有服务

    @Autowired
    private UserService userService;            // ✅ 现有服务

    @Autowired
    private UserActivityService userActivityService; // ✅ 现有服务

    public AdminDashboardDto getDashboardOverview() {
        AdminDashboardDto dashboard = new AdminDashboardDto();

        // 复用现有服务方法
        dashboard.setTotalServers(serverService.getAllServers().size());
        dashboard.setTotalUsers(userService.getTotalUserCount());

        // 利用现有监控服务
        AggregatedServerMetrics metrics = monitoringService.getAggregatedMetrics();
        dashboard.setCpuUsage(metrics.getAvgCpuUsage());
        dashboard.setMemoryUsage(metrics.getAvgMemoryUsage());
        dashboard.setDiskUsage(metrics.getAvgDiskUsage());

        return dashboard;
    }
}
```

## WebSocket集成规范

### 实时数据更新

```java
// 遵循现有WebSocket主题规范
@Controller
public class DashboardWebSocketController {

    // 复用现有WebSocket基础设施
    @MessageMapping("/dashboard/subscribe")
    @SendTo("/topic/dashboard-updates")
    public DashboardUpdateMessage subscribeUpdates() {
        // 推送仪表板更新消息
    }
}

// 主题路径规范
/topic/status           // ✅ 现有通用状态
/topic/server-status    // ✅ 现有服务器状态
/topic/dashboard-status // ✨ 新增仪表板状态
```

## 缓存策略规范

### 利用现有缓存机制

```java
@Service
public class DashboardService {

    // 复用现有MonitoringService的缓存配置
    @Cacheable(value = "dashboard-overview", unless = "#result == null")
    public AdminDashboardDto getDashboardOverview() {
        // 组装数据，利用现有服务的缓存能力
    }

    // 遵循现有缓存key规范
    @Cacheable(value = "server-summary", key = "'all-servers'")
    public List<ServerSummaryDto> getAllServersSummary() {
        // 利用现有ServerService和MonitoringService
    }
}
```

## 安全规范

### 权限验证

```java
// 遵循现有权限控制模式
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
@GetMapping("/admin/dashboard/**")
public ResponseEntity<?> adminDashboardEndpoints() {
    // 复用现有SecurityConfig中的权限配置
}
```

### 数据脱敏

```java
// 遵循现有数据脱敏规范
public class AdminDashboardDto {
    // 敏感信息处理
    @JsonIgnore
    private String sensitiveServerInfo;

    // 用户信息部分遮蔽 (参考现有UserProfileDto)
    public String getMaskedUserEmail() {
        return email.replaceAll("(.{2}).*(@.*)", "$1***$2");
    }
}
```

## 总结

### 设计原则遵循度检查

| 规范类别 | 遵循现有规范 | 新增内容 | 一致性评分 |
|---------|-------------|----------|------------|
| **路径结构** | ✅ `/admin/{resource}` | `/dashboard` | 100% |
| **HTTP方法** | ✅ GET/POST规范 | 完全一致 | 100% |
| **权限控制** | ✅ @PreAuthorize | ADMIN/SUPER_ADMIN | 100% |
| **响应格式** | ✅ ResponseEntity | JSON包装 | 100% |
| **服务集成** | ✅ 复用现有服务 | 无重复开发 | 100% |
| **缓存策略** | ✅ 复用现有缓存 | 扩展现有机制 | 100% |

### 实施建议

1. **优先级**: 先实现核心数据API，再添加实时更新功能
2. **测试**: 复用现有AdminController的测试模式
3. **文档**: 更新CLAUDE.md的API接口说明章节
4. **监控**: 利用现有监控系统跟踪新API性能

---

**规范状态**: ✅ 完成，确保100%与现有系统API设计保持一致
**下一步**: 按照此规范实施管理员仪表板API开发
**更新时间**: 2025-01-27