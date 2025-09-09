# 服务器状态标签系统设计文档

## 文档信息
- **创建时间**: 2025-09-08
- **版本**: v1.0
- **作者**: Dev Debug Platform Team
- **状态**: 设计阶段

---

## 📋 目录
1. [概述](#概述)
2. [需求分析](#需求分析) 
3. [系统架构](#系统架构)
4. [数据模型设计](#数据模型设计)
5. [服务层设计](#服务层设计)
6. [API接口设计](#api接口设计)
7. [资源监控告警](#资源监控告警)
8. [前端展示规范](#前端展示规范)
9. [实施计划](#实施计划)

---

## 概述

### 项目背景
在Dev Debug Platform中，服务器管理是核心功能之一。目前服务器卡片展示的信息有限，管理员难以快速了解服务器的完整状态。为提升用户体验和运维效率，需要设计一套丰富的服务器状态标签系统。

### 设计目标
1. **直观展示** - 通过状态标签快速了解服务器关键信息
2. **实时监控** - 实时更新服务器各项状态指标
3. **告警提醒** - 及时发现并提醒资源使用异常
4. **可扩展性** - 支持后续新增更多状态类型
5. **用户友好** - 清晰的颜色编码和优先级排序

---

## 需求分析

### 功能需求

#### 核心状态标签类型
| 标签类型 | 说明 | 优先级 |
|---------|------|--------|
| 连接状态 | 服务器在线/离线状态 | 高 |
| 工作目录 | 工作目录创建和权限状态 | 中 |
| 活跃用户 | 当前活跃用户数量 | 中 |
| 监控状态 | 用户行为监控服务状态 | 中 |
| 资源状态 | CPU/内存/磁盘使用率 | 高 |
| 权限状态 | 服务器权限级别 | 低 |

#### 特殊需求
- **内存告警**: 内存使用率超过80%时显示告警标签
- **实时更新**: 状态标签需要实时或准实时更新
- **历史记录**: 保留状态变化历史记录用于分析

### 非功能需求
- **性能**: 状态检查不影响系统正常运行
- **可靠性**: 状态检查失败不影响其他功能
- **扩展性**: 易于添加新的状态类型和检查逻辑

---

## 系统架构

### 整体架构图
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   前端组件      │    │   控制器层       │    │   服务层        │
│                 │    │                  │    │                 │
│ ServerCard      │◄──►│ ServerStatus     │◄──►│ StatusTag       │
│ StatusTag       │    │ Controller       │    │ Service         │
│ Component       │    │                  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         │              ┌──────────────────┐             │
         │              │   WebSocket      │             │
         └──────────────►│   实时通知       │◄────────────┘
                        └──────────────────┘
                                 │
                        ┌──────────────────┐    ┌─────────────────┐
                        │   调度任务       │    │   数据层        │
                        │                  │    │                 │
                        │ Status           │◄──►│ ServerStatusTag │
                        │ Scheduler        │    │ Entity          │
                        │                  │    │                 │
                        └──────────────────┘    └─────────────────┘
```

### 核心组件说明
- **ServerStatusTagService**: 状态标签管理核心服务
- **ServerStatusAggregatorService**: 状态聚合服务  
- **ServerResourceMonitoringService**: 资源监控服务
- **ResourceAlertService**: 资源告警服务
- **ServerStatusScheduler**: 状态刷新调度器

---

## 数据模型设计

### 1. 服务器状态标签实体 `ServerStatusTag`

```java
@Entity
@Table(name = "server_status_tags")
public class ServerStatusTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "server_id", nullable = false)
    private Long serverId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type", nullable = false)
    private TagType tagType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false) 
    private TagStatus status;
    
    @Column(name = "display_text")
    private String displayText;
    
    @Column(name = "value")
    private String value; // 用于存储数值类信息
    
    @Column(name = "color_scheme")
    private String colorScheme; // success, warning, danger, info
    
    @Column(name = "priority")
    private Integer priority = 0; // 显示优先级
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "details", length = 500)
    private String details; // 详细信息
    
    // 标签类型枚举
    public enum TagType {
        CONNECTION("连接状态"),
        WORK_DIRECTORY("工作目录"), 
        ACTIVE_USERS("活跃用户"),
        MONITORING("监控状态"),
        PERMISSION("权限状态"),
        MEMORY_USAGE("内存使用"),
        CPU_USAGE("CPU使用"), 
        DISK_USAGE("磁盘使用"),
        SYSTEM_LOAD("系统负载"),
        SECURITY("安全状态"),
        MAINTENANCE("维护状态");
    }
    
    // 标签状态枚举
    public enum TagStatus {
        ACTIVE("激活"),
        INACTIVE("未激活"),
        WARNING("警告"),          // 资源使用70-80%
        CRITICAL("严重"),         // 资源使用80-90%  
        DANGER("危险"),           // 资源使用>90%
        ERROR("错误"),
        UNKNOWN("未知"),
        MAINTENANCE("维护中");
    }
}
```

### 2. 服务器状态快照实体 `ServerStatusSnapshot`

```java
@Entity
@Table(name = "server_status_snapshots")
public class ServerStatusSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "server_id", nullable = false)
    private Long serverId;
    
    // 连接状态相关
    @Column(name = "is_online")
    private Boolean isOnline;
    
    @Column(name = "connection_response_time")
    private Integer connectionResponseTime; // ms
    
    // 工作目录状态
    @Column(name = "work_directory_exists")
    private Boolean workDirectoryExists;
    
    @Column(name = "work_directory_writable")
    private Boolean workDirectoryWritable;
    
    // 用户活动状态
    @Column(name = "active_user_count")
    private Integer activeUserCount;
    
    @Column(name = "active_session_count")
    private Integer activeSessionCount;
    
    // 监控状态
    @Column(name = "monitoring_enabled")
    private Boolean monitoringEnabled;
    
    @Column(name = "last_metrics_update")
    private LocalDateTime lastMetricsUpdate;
    
    // 资源状态
    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;
    
    @Column(name = "memory_usage_percent")
    private Double memoryUsagePercent;
    
    @Column(name = "disk_usage_percent")
    private Double diskUsagePercent;
    
    @Column(name = "snapshot_time")
    private LocalDateTime snapshotTime;
    
    @PrePersist
    protected void onCreate() {
        snapshotTime = LocalDateTime.now();
    }
}
```

### 3. 服务器资源阈值配置实体 `ServerResourceThreshold`

```java
@Entity
@Table(name = "server_resource_thresholds")
public class ServerResourceThreshold {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "server_id", nullable = false)
    private Long serverId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;
    
    @Column(name = "warning_threshold")
    private Double warningThreshold; // 警告阈值，如 70.0
    
    @Column(name = "critical_threshold") 
    private Double criticalThreshold; // 严重阈值，如 80.0
    
    @Column(name = "danger_threshold")
    private Double dangerThreshold; // 危险阈值，如 90.0
    
    @Column(name = "enabled")
    private Boolean enabled = true;
    
    // 资源类型枚举
    public enum ResourceType {
        MEMORY("内存使用率", "%"),
        CPU("CPU使用率", "%"),
        DISK("磁盘使用率", "%"),
        LOAD_AVERAGE("系统负载", ""),
        SWAP("交换空间使用率", "%");
        
        private final String displayName;
        private final String unit;
        
        ResourceType(String displayName, String unit) {
            this.displayName = displayName;
            this.unit = unit;
        }
    }
    
    // 判断当前值的严重程度
    public ThresholdLevel evaluateLevel(Double currentValue) {
        if (currentValue >= dangerThreshold) return ThresholdLevel.DANGER;
        if (currentValue >= criticalThreshold) return ThresholdLevel.CRITICAL;
        if (currentValue >= warningThreshold) return ThresholdLevel.WARNING;
        return ThresholdLevel.NORMAL;
    }
    
    public enum ThresholdLevel {
        NORMAL("正常", "success"),
        WARNING("警告", "warning"), 
        CRITICAL("严重", "danger"),
        DANGER("危险", "danger");
        
        private final String displayName;
        private final String colorScheme;
        
        ThresholdLevel(String displayName, String colorScheme) {
            this.displayName = displayName;
            this.colorScheme = colorScheme;
        }
    }
}
```

---

## 服务层设计

### 1. 核心服务接口

#### A. 服务器状态标签服务 `ServerStatusTagService`

```java
@Service
public class ServerStatusTagService {
    
    // 核心方法
    public List<ServerStatusTag> getServerStatusTags(Long serverId);
    public ServerStatusTag createOrUpdateTag(Long serverId, TagType tagType, TagStatus status, String details);
    public void refreshAllServerTags(Long serverId);
    public void deleteObsoleteTags(Long serverId);
    
    // 状态检查方法
    private ServerStatusTag checkConnectionStatus(Long serverId);
    private ServerStatusTag checkWorkDirectoryStatus(Long serverId);
    private ServerStatusTag checkActiveUsersStatus(Long serverId);
    private ServerStatusTag checkMonitoringStatus(Long serverId);
    private ServerStatusTag checkResourceStatus(Long serverId);
}
```

#### B. 服务器资源监控服务 `ServerResourceMonitoringService`

```java
@Service
public class ServerResourceMonitoringService {
    
    /**
     * 检查服务器资源状态并生成相应的状态标签
     */
    public List<ServerStatusTag> checkResourceStatuses(Long serverId);
    
    /**
     * 专门检查内存使用率
     */
    public ServerStatusTag checkMemoryUsage(Long serverId);
    
    /**
     * 评估内存状态 - 重点实现80%阈值告警
     */
    private ServerStatusTag evaluateMemoryStatus(Long serverId, double memoryUsage, 
                                               ServerResourceThreshold threshold) {
        
        ThresholdLevel level = threshold.evaluateLevel(memoryUsage);
        String formattedUsage = String.format("%.1f%%", memoryUsage);
        
        switch (level) {
            case DANGER:
                return createTag(serverId, TagType.MEMORY_USAGE, TagStatus.DANGER,
                    "内存 " + formattedUsage, "danger", formattedUsage,
                    "内存使用率超过" + threshold.getDangerThreshold() + "%，系统面临崩溃风险");
                    
            case CRITICAL:
                return createTag(serverId, TagType.MEMORY_USAGE, TagStatus.CRITICAL,
                    "内存 " + formattedUsage, "danger", formattedUsage,
                    "内存使用率超过" + threshold.getCriticalThreshold() + "%，需要立即处理");
                    
            case WARNING:
                return createTag(serverId, TagType.MEMORY_USAGE, TagStatus.WARNING,
                    "内存 " + formattedUsage, "warning", formattedUsage,
                    "内存使用率超过" + threshold.getWarningThreshold() + "%，建议关注");
                    
            case NORMAL:
                return null; // 正常状态下不显示内存标签
                
            default:
                return createTag(serverId, TagType.MEMORY_USAGE, TagStatus.UNKNOWN,
                    "内存状态未知", "secondary", null, "无法评估内存状态");
        }
    }
    
    /**
     * 获取或创建默认的内存阈值配置
     */
    private ServerResourceThreshold getOrCreateMemoryThreshold(Long serverId) {
        return thresholdRepository.findByServerIdAndResourceType(serverId, ResourceType.MEMORY)
            .orElseGet(() -> {
                ServerResourceThreshold defaultThreshold = new ServerResourceThreshold();
                defaultThreshold.setServerId(serverId);
                defaultThreshold.setResourceType(ResourceType.MEMORY);
                defaultThreshold.setWarningThreshold(70.0);   // 70%警告
                defaultThreshold.setCriticalThreshold(80.0);  // 80%严重 ⭐
                defaultThreshold.setDangerThreshold(90.0);    // 90%危险
                defaultThreshold.setEnabled(true);
                return thresholdRepository.save(defaultThreshold);
            });
    }
}
```

#### C. 资源告警服务 `ResourceAlertService`

```java
@Service
public class ResourceAlertService {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    /**
     * 内存使用率专门检查 - 80%阈值告警
     */
    @EventListener
    @Async
    public void handleMemoryAlert(ServerMetricsUpdatedEvent event) {
        ServerMetrics metrics = event.getMetrics();
        double memoryUsage = metrics.getMemoryUsagePercent();
        
        // 检查80%阈值 ⭐
        if (memoryUsage >= 80.0) {
            logger.warn("服务器 {} 内存使用率达到 {}%，超过80%阈值", 
                       event.getServerId(), memoryUsage);
            
            // 发送WebSocket通知到前端
            sendMemoryAlertNotification(event.getServerId(), memoryUsage);
        }
    }
    
    private void sendMemoryAlertNotification(Long serverId, double memoryUsage) {
        Map<String, Object> alertData = new HashMap<>();
        alertData.put("type", "memory_alert");
        alertData.put("serverId", serverId);
        alertData.put("memoryUsage", memoryUsage);
        alertData.put("message", String.format("服务器内存使用率 %.1f%% 超过80%%阈值", memoryUsage));
        alertData.put("timestamp", System.currentTimeMillis());
        alertData.put("severity", memoryUsage >= 90 ? "danger" : "warning");
        
        // 通过WebSocket发送实时通知
        messagingTemplate.convertAndSend("/topic/alerts", alertData);
    }
}
```

### 2. 状态检测逻辑实现

#### A. 连接状态检测
```java
private ServerStatusTag checkConnectionStatus(Long serverId) {
    Server server = serverService.findById(serverId);
    boolean isConnected = sshConnectionService.testConnection(server);
    
    TagStatus status = isConnected ? TagStatus.ACTIVE : TagStatus.ERROR;
    String displayText = isConnected ? "在线" : "离线";
    String colorScheme = isConnected ? "success" : "danger";
    
    return createTag(serverId, TagType.CONNECTION, status, displayText, colorScheme);
}
```

#### B. 工作目录状态检测
```java
private ServerStatusTag checkWorkDirectoryStatus(Long serverId) {
    Server server = serverService.findById(serverId);
    
    try {
        boolean exists = remoteCommandService.executeCommand(serverId, 
            "test -d " + server.getBaseWorkDirectory()).getExitCode() == 0;
        boolean writable = remoteCommandService.executeCommand(serverId,
            "test -w " + server.getBaseWorkDirectory()).getExitCode() == 0;
            
        if (exists && writable) {
            return createTag(serverId, TagType.WORK_DIRECTORY, TagStatus.ACTIVE, 
                "目录可用", "success");
        } else if (exists) {
            return createTag(serverId, TagType.WORK_DIRECTORY, TagStatus.WARNING,
                "目录只读", "warning");  
        } else {
            return createTag(serverId, TagType.WORK_DIRECTORY, TagStatus.ERROR,
                "目录不存在", "danger");
        }
    } catch (Exception e) {
        return createTag(serverId, TagType.WORK_DIRECTORY, TagStatus.UNKNOWN,
            "检查失败", "secondary");
    }
}
```

#### C. 活跃用户状态检测
```java
private ServerStatusTag checkActiveUsersStatus(Long serverId) {
    int activeUserCount = userActivityService.getActiveUserCount(serverId);
    int activeSessionCount = userActivityService.getActiveSessionCount(serverId);
    
    String displayText = activeUserCount > 0 ? 
        activeUserCount + "个活跃用户" : "无活跃用户";
    String colorScheme = activeUserCount > 0 ? "info" : "secondary";
    TagStatus status = activeUserCount > 0 ? TagStatus.ACTIVE : TagStatus.INACTIVE;
    
    return createTag(serverId, TagType.ACTIVE_USERS, status, displayText, colorScheme)
        .setValue(String.valueOf(activeUserCount))
        .setDetails("活跃会话: " + activeSessionCount);
}
```

---

## API接口设计

### 1. 服务器状态API控制器

```java
@RestController
@RequestMapping("/api/servers/{serverId}/status")
public class ServerStatusController {
    
    @GetMapping("/tags")
    public ResponseEntity<List<ServerStatusTagDto>> getServerStatusTags(@PathVariable Long serverId);
    
    @GetMapping("/aggregation") 
    public ResponseEntity<ServerStatusAggregationDto> getServerStatusAggregation(@PathVariable Long serverId);
    
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshServerStatus(@PathVariable Long serverId);
    
    @GetMapping("/snapshot")
    public ResponseEntity<ServerStatusSnapshotDto> getLatestSnapshot(@PathVariable Long serverId);
    
    @GetMapping("/history")
    public ResponseEntity<List<ServerStatusSnapshotDto>> getStatusHistory(
        @PathVariable Long serverId,
        @RequestParam(defaultValue = "24") Integer hours);
}
```

### 2. 资源监控API

```java
@RestController
@RequestMapping("/api/servers/{serverId}/resources")
public class ServerResourceController {
    
    @GetMapping("/thresholds")
    public ResponseEntity<List<ServerResourceThresholdDto>> getResourceThresholds(@PathVariable Long serverId);
    
    @PostMapping("/thresholds")
    public ResponseEntity<Void> updateResourceThresholds(@PathVariable Long serverId,
                                                        @RequestBody List<ServerResourceThresholdDto> thresholds);
    
    @GetMapping("/alerts/current")
    public ResponseEntity<List<ResourceAlertDto>> getCurrentAlerts(@PathVariable Long serverId);
    
    @PostMapping("/check-memory")
    public ResponseEntity<ServerStatusTagDto> checkMemoryUsage(@PathVariable Long serverId);
}
```

### 3. DTO设计

#### A. 服务器状态聚合DTO
```java
public class ServerStatusAggregationDto {
    private Long serverId;
    private String serverName;
    private Server.ConnectionStatus connectionStatus;
    private List<ServerStatusTagDto> statusTags;
    private ServerStatusSnapshotDto currentSnapshot;
    private LocalDateTime lastUpdated;
    
    // 快捷状态方法
    public boolean isOnline();
    public boolean hasActiveUsers();
    public boolean isMonitoringActive();
    public boolean hasResourceWarnings();
    public boolean hasMemoryAlert(); // 新增：内存告警检查
}
```

#### B. 状态标签DTO
```java
public class ServerStatusTagDto {
    private TagType tagType;
    private TagStatus status;
    private String displayText;
    private String value;
    private String colorScheme;
    private Integer priority;
    private String details;
    private LocalDateTime lastUpdated;
    
    // 用于前端展示的方法
    public String getCssClass();
    public String getIconClass();
    public boolean isWarning();
    public boolean isError();
    public boolean isCritical(); // 新增：严重状态判断
}
```

---

## 资源监控告警

### 内存使用率80%阈值告警设计

#### 1. 阈值配置
| 级别 | 阈值 | 标签状态 | 颜色方案 | 说明 |
|------|------|----------|----------|------|
| 正常 | < 70% | - | - | 不显示标签 |
| 警告 | 70-80% | WARNING | warning | 🟡 `内存 72.5%` |
| **严重** | **80-90%** | **CRITICAL** | **danger** | **🟠 `内存 85.3%`** ⭐ |
| 危险 | > 90% | DANGER | danger | 🔴 `内存 92.1%` |

#### 2. 告警触发机制
```java
// 监控服务定时检查
@Scheduled(fixedDelay = 30000) // 30秒检查一次
public void checkMemoryUsage() {
    List<Server> servers = serverService.findAllActive();
    
    for (Server server : servers) {
        ServerMetrics metrics = monitoringService.getLatestMetrics(server.getId());
        if (metrics != null && metrics.getMemoryUsagePercent() >= 80.0) {
            // 触发内存告警
            resourceAlertService.handleMemoryAlert(
                new ServerMetricsUpdatedEvent(server.getId(), metrics)
            );
        }
    }
}
```

#### 3. 实时通知
```javascript
// 前端WebSocket监听
stompClient.subscribe('/topic/alerts', function(message) {
    const alert = JSON.parse(message.body);
    if (alert.type === 'memory_alert') {
        showMemoryAlertNotification(alert);
        updateServerCardStatus(alert.serverId, alert);
    }
});

function showMemoryAlertNotification(alert) {
    // 显示浮动通知
    showNotification({
        type: alert.severity,
        title: '内存使用率告警',
        message: alert.message,
        autoClose: false // 重要告警不自动关闭
    });
}
```

### 定时任务调度设计

```java
@Component
public class ServerStatusScheduler {
    
    @Scheduled(fixedDelay = 30000) // 30秒检查一次关键状态
    public void refreshCriticalStatuses() {
        List<Server> servers = serverService.findAllActive();
        servers.parallelStream().forEach(server -> {
            // 异步刷新服务器状态，重点关注资源使用率
            serverStatusAggregatorService.refreshServerStatusAsync(server.getId());
        });
    }
    
    @Scheduled(cron = "0 */5 * * * *") // 每5分钟全面检查
    public void refreshAllStatuses() {
        serverStatusAggregatorService.refreshAllServerStatuses();
    }
    
    @Scheduled(cron = "0 0 2 * * *") // 每天凌晨2点清理
    public void cleanupOldData() {
        serverStatusSnapshotService.cleanupOldSnapshots(7); // 保留7天
    }
}
```

---

## 前端展示规范

### 1. 状态标签展示效果

#### 服务器卡片示例
```
┌─────────────────────────────────────────┐
│  🖥️ 开发服务器 - dev-server-01           │
│  192.168.1.100:22                      │
│                                         │
│  🔴 内存 85.3%  🟢 在线  🔵 3个活跃用户   │
│  🟡 目录只读    🟢 监控正常               │
│                                         │
│  最后更新: 2分钟前                       │
└─────────────────────────────────────────┘
```

#### 标签优先级排序（高到低）
1. 🔴 `内存 92.1%` - 危险（优先级10）
2. 🔴 `连接失败` - 连接错误（优先级9）  
3. 🟠 `内存 85.3%` - 严重（优先级8） ⭐
4. 🟡 `CPU 75%` - 警告（优先级6）
5. 🔵 `3个活跃用户` - 信息（优先级2）
6. 🟢 `监控正常` - 正常（优先级1）

### 2. 颜色方案标准

| 状态 | 颜色代码 | Bootstrap类 | 说明 |
|------|----------|-------------|------|
| 成功/正常 | #28a745 | badge-success | 🟢 |
| 信息 | #17a2b8 | badge-info | 🔵 |
| 警告 | #ffc107 | badge-warning | 🟡 |
| 危险/严重 | #dc3545 | badge-danger | 🔴/🟠 |
| 次要 | #6c757d | badge-secondary | ⚫ |

### 3. 响应式设计
- **桌面端**: 显示所有优先级>=1的标签
- **平板端**: 显示所有优先级>=3的标签  
- **移动端**: 只显示优先级>=6的标签

---

## 实施计划

### 阶段1：基础架构（1-2周）
- [ ] 创建数据库表结构
- [ ] 实现基础实体类和Repository
- [ ] 实现核心服务接口
- [ ] 创建基础API接口

### 阶段2：状态检测逻辑（2-3周）  
- [ ] 实现连接状态检测
- [ ] 实现工作目录状态检测
- [ ] 实现活跃用户状态检测
- [ ] **实现内存使用率80%阈值告警** ⭐
- [ ] 实现其他资源状态检测

### 阶段3：告警系统（1-2周）
- [ ] 实现资源告警服务
- [ ] 集成WebSocket实时通知
- [ ] 实现定时任务调度器
- [ ] 添加告警历史记录

### 阶段4：前端集成（2-3周）
- [ ] 设计状态标签前端组件
- [ ] 集成服务器卡片展示
- [ ] 实现实时状态更新
- [ ] 添加告警通知UI
- [ ] 移动端适配

### 阶段5：测试与优化（1周）
- [ ] 单元测试覆盖
- [ ] 集成测试
- [ ] 性能测试与优化
- [ ] 用户体验测试

### 关键里程碑
- **Week 2**: 完成基础架构，可以创建和查询状态标签
- **Week 4**: 完成内存80%阈值告警功能 ⭐  
- **Week 6**: 完成所有状态检测逻辑
- **Week 8**: 完成前端集成，功能可用
- **Week 9**: 系统测试完成，准备上线

---

## 风险与应对

### 技术风险
| 风险 | 影响 | 应对措施 |
|------|------|----------|
| 状态检查影响性能 | 高 | 异步处理、合理的检查频率 |
| SSH连接超时 | 中 | 设置合理超时时间、错误处理 |
| 数据库存储压力 | 中 | 定期清理历史数据、分页查询 |

### 业务风险  
| 风险 | 影响 | 应对措施 |
|------|------|----------|
| 误报告警过多 | 中 | 调整阈值配置、增加确认机制 |
| 用户界面信息过载 | 低 | 优先级排序、响应式展示 |

---

## 总结

本设计文档详细描述了服务器状态标签系统的完整方案，特别突出了**内存使用率超过80%的阈值告警功能**。该系统将大幅提升Dev Debug Platform的服务器管理体验，帮助管理员快速了解服务器状态并及时处理异常情况。

### 核心价值
✅ **直观展示** - 通过丰富的状态标签快速了解服务器状态  
✅ **实时告警** - 内存使用率超过80%时立即通知  
✅ **可扩展性** - 易于添加新的监控指标和告警规则  
✅ **用户友好** - 清晰的优先级排序和颜色编码

---

*文档结束*