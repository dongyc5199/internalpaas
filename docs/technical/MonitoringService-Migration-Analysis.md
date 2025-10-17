# MonitoringService 迁移分析报告

**日期**: 2025-10-17  
**阶段**: Phase 4 - 数据存储迁移  
**状态**: 🔄 进行中

---

## 📋 执行摘要

`MonitoringService` 是旧架构中负责通过 SSH 收集服务器监控数据并存储到 H2 数据库的核心服务。随着 Phase 4 迁移到 Hub + TSDB 架构，该服务的大部分功能已废弃，但仍有 **5 个类** 在使用其功能。

### 迁移优先级
- 🔴 **高优先级**: `ServerResourceMonitoringService` - 资源监控告警核心
- 🟡 **中优先级**: `ServerService`, `SystemHealthService`, `ServerStatusTagService`
- 🟢 **低优先级**: `ServerStatusTestController` - 测试控制器

---

## 🔍 当前使用情况分析

### 1. ServerResourceMonitoringService.java
**路径**: `src/main/java/com/cmict/internalpaas/service/ServerResourceMonitoringService.java`  
**状态**: ❌ **使用废弃方法**

#### 使用场景
| 行号 | 方法调用 | 用途 | 废弃状态 |
|-----|---------|------|---------|
| 45 | `monitoringService.getLatestMetrics(serverId)` | 检查资源状态 | ❌ 已废弃 |
| 82 | `monitoringService.getLatestMetrics(serverId)` | 检查内存使用率 | ❌ 已废弃 |
| 153 | `monitoringService.getLatestMetrics(serverId)` | 检查CPU使用率 | ❌ 已废弃 |
| 174 | `monitoringService.getLatestMetrics(serverId)` | 检查磁盘使用率 | ❌ 已废弃 |

#### 迁移方案
```java
// ❌ 旧代码
ServerMetrics latestMetrics = monitoringService.getLatestMetrics(serverId);

// ✅ 新代码
ServerMetrics latestMetrics = metricsHubClient.getLatestMetrics(serverId);
```

#### 影响分析
- **功能**: 资源监控告警系统（80%内存阈值告警等）
- **调用方**: `ServerStatusScheduler` 定时任务
- **风险**: 高 - 影响实时告警功能
- **建议**: **立即迁移**

---

### 2. SystemHealthService.java
**路径**: `src/main/java/com/cmict/internalpaas/service/SystemHealthService.java`  
**状态**: ❌ **使用废弃方法**

#### 使用场景
| 行号 | 方法调用 | 用途 | 废弃状态 |
|-----|---------|------|---------|
| 96 | `monitoringService.getServerMetrics(server)` | 收集服务器指标 | ❌ 已废弃 |
| 228 | `monitoringService.getServerMetrics(server)` | 计算资源健康度 | ❌ 已废弃 |
| 332 | `monitoringService.getLatestMetrics(serverId)` | 聚合指标统计 | ❌ 已废弃 |

#### 迁移方案
```java
// ❌ 旧代码 - SSH实时收集
ServerMetrics metrics = monitoringService.getServerMetrics(server);

// ✅ 新代码 - 从Hub获取
ServerMetrics metrics = metricsHubClient.getLatestMetrics(server.getId());
```

#### 影响分析
- **功能**: 系统健康度评分计算
- **调用方**: 可能的 Dashboard API
- **风险**: 中 - 影响健康度展示
- **建议**: 尽快迁移

---

### 3. ServerStatusTagService.java
**路径**: `src/main/java/com/cmict/internalpaas/service/ServerStatusTagService.java`  
**状态**: ❌ **使用废弃方法**

#### 使用场景
| 行号 | 方法调用 | 用途 | 废弃状态 |
|-----|---------|------|---------|
| 353 | `monitoringService.getLatestMetrics(serverId)` | 获取监控数据生成标签 | ❌ 已废弃 |

#### 迁移方案
```java
// ❌ 旧代码
return monitoringService.getLatestMetrics(serverId);

// ✅ 新代码
return metricsHubClient.getLatestMetrics(serverId);
```

#### 影响分析
- **功能**: 服务器状态标签生成
- **调用方**: 标签系统
- **风险**: 中 - 影响状态标签展示
- **建议**: 配合资源监控服务一起迁移

---

### 4. ServerService.java
**路径**: `src/main/java/com/cmict/internalpaas/service/ServerService.java`  
**状态**: ⚠️ **部分使用废弃方法**

#### 使用场景
| 行号 | 方法调用 | 用途 | 废弃状态 |
|-----|---------|------|---------|
| 136 | `monitoringService.saveUserActivities(server)` | 保存用户活动 | ✅ **保留** |
| 280 | `monitoringService.saveUserActivities(server)` | 保存用户活动 | ✅ **保留** |
| 502 | `monitoringService.getLatestMetrics(serverId)` | 获取最新指标 | ❌ 已废弃 |
| 515 | `monitoringService.saveServerMetrics(server)` | 保存服务器指标 | ❌ 已废弃 |
| 522 | `monitoringService.saveUserActivities(server)` | 保存用户活动 | ✅ **保留** |

#### 迁移方案
```java
// ✅ 保留 - 用户活动功能未废弃
monitoringService.saveUserActivities(server);

// ❌ 废弃 - 第502行
// 旧代码
return monitoringService.getLatestMetrics(serverId);

// ✅ 新代码
return metricsHubClient.getLatestMetrics(serverId);

// ❌ 废弃 - 第515行 (整个方法可能需要重构)
ServerMetrics metrics = monitoringService.saveServerMetrics(server);
// 此方法已不需要，Hub自动存储
```

#### 影响分析
- **功能**: 服务器核心管理服务
- **风险**: 低-中 - 部分功能已注释
- **建议**: 仅迁移 `getLatestMetrics()` 调用

---

### 5. ServerStatusTestController.java
**路径**: `src/main/java/com/cmict/internalpaas/controller/ServerStatusTestController.java`  
**状态**: ❌ **使用废弃方法**

#### 使用场景
| 行号 | 方法调用 | 用途 | 废弃状态 |
|-----|---------|------|---------|
| 69 | `monitoringService.getLatestMetrics(serverId)` | 测试页面获取指标 | ❌ 已废弃 |

#### 迁移方案
```java
// ❌ 旧代码
ServerMetrics metrics = monitoringService.getLatestMetrics(server.getId());

// ✅ 新代码
ServerMetrics metrics = metricsHubClient.getLatestMetrics(server.getId());
```

#### 影响分析
- **功能**: 测试控制器
- **风险**: 低 - 仅测试用途
- **建议**: 可以延后迁移

---

## 📊 迁移统计

### 废弃方法使用统计
| 废弃方法 | 调用次数 | 涉及文件数 | 迁移状态 |
|---------|---------|-----------|---------|
| `getLatestMetrics()` | 6次 | 4个文件 | ⏳ 待迁移 |
| `getServerMetrics()` | 2次 | 1个文件 | ⏳ 待迁移 |
| `saveServerMetrics()` | 1次 | 1个文件 | ⏳ 待迁移 |
| `saveUserActivities()` | 3次 | 1个文件 | ✅ **保留使用** |

### 保留功能
以下 `MonitoringService` 方法**不受影响**，继续使用：
- ✅ `saveUserActivities()` - SSH获取用户活跃信息（非监控数据）
- ✅ `getUserActivities()` - 获取用户活动列表
- ✅ `fetchTopProcesses()` - 获取进程列表（管理功能）
- ✅ `killProcess()` - 结束进程（管理功能）

---

## 🔧 迁移实施计划

### Phase 1: 核心服务迁移 (优先级: 🔴 高)
**时间**: 立即执行  
**影响**: 资源监控告警系统

#### 步骤 1.1: ServerResourceMonitoringService
```java
// 1. 添加依赖注入
@Autowired
private MetricsHubClient metricsHubClient;

// 2. 替换所有 getLatestMetrics() 调用 (4处)
// 第45行
ServerMetrics latestMetrics = metricsHubClient.getLatestMetrics(serverId);

// 第82行
ServerMetrics latestMetrics = metricsHubClient.getLatestMetrics(serverId);

// 第153行
ServerMetrics latestMetrics = metricsHubClient.getLatestMetrics(serverId);

// 第174行
ServerMetrics latestMetrics = metricsHubClient.getLatestMetrics(serverId);

// 3. 添加 Hub 可用性检查
if (metricsHubClient == null || !metricsHubClient.isAvailable()) {
    logger.warn("Hub服务不可用，无法获取监控数据");
    return null; // 或返回默认标签
}
```

#### 步骤 1.2: ServerStatusTagService
```java
// 1. 添加依赖注入
@Autowired
private MetricsHubClient metricsHubClient;

// 2. 修改 getLatestServerMetrics() 方法 (第353行)
private ServerMetrics getLatestServerMetrics(Long serverId) {
    if (metricsHubClient != null && metricsHubClient.isAvailable()) {
        return metricsHubClient.getLatestMetrics(serverId);
    }
    return null;
}
```

### Phase 2: 辅助服务迁移 (优先级: 🟡 中)
**时间**: Phase 1 完成后  
**影响**: 健康度计算、服务器管理

#### 步骤 2.1: SystemHealthService
```java
// 1. 添加依赖注入
@Autowired
private MetricsHubClient metricsHubClient;

// 2. 替换 getServerMetrics() 调用 (第96、228行)
// 旧: metrics = monitoringService.getServerMetrics(server);
// 新: metrics = metricsHubClient.getLatestMetrics(server.getId());

// 3. 替换 getLatestMetrics() 调用 (第332行)
ServerMetrics metrics = metricsHubClient.getLatestMetrics(server.getId());
```

#### 步骤 2.2: ServerService
```java
// 1. 添加依赖注入
@Autowired
private MetricsHubClient metricsHubClient;

// 2. 修改 getServerLatestMetrics() 方法 (第502行)
public ServerMetrics getServerLatestMetrics(Long serverId) {
    if (metricsHubClient != null && metricsHubClient.isAvailable()) {
        return metricsHubClient.getLatestMetrics(serverId);
    }
    return null;
}

// 3. 重构或移除 refreshServerMetrics() 方法 (第515行)
// Hub自动收集数据，不需要手动刷新
// 可以改为仅刷新用户活动
public ServerMetrics refreshServerMetrics(Long serverId) {
    Server server = findById(serverId).orElse(null);
    if (server != null) {
        monitoringService.saveUserActivities(server); // 保留
        return metricsHubClient.getLatestMetrics(serverId); // 从Hub读取
    }
    return null;
}
```

### Phase 3: 测试控制器迁移 (优先级: 🟢 低)
**时间**: Phase 2 完成后

#### 步骤 3.1: ServerStatusTestController
```java
// 1. 添加依赖注入
@Autowired
private MetricsHubClient metricsHubClient;

// 2. 替换 getLatestMetrics() 调用 (第69行)
ServerMetrics metrics = metricsHubClient.getLatestMetrics(server.getId());
```

---

## ⚠️ 注意事项

### 1. Hub 可用性检查
所有迁移代码都应包含 Hub 可用性检查：
```java
if (metricsHubClient == null || !metricsHubClient.isAvailable()) {
    logger.warn("Hub服务不可用 - serverId: {}", serverId);
    return null; // 或返回降级数据
}
```

### 2. 空值处理
Hub 可能返回 `null`（服务器无数据时），需要妥善处理：
```java
ServerMetrics metrics = metricsHubClient.getLatestMetrics(serverId);
if (metrics == null) {
    logger.warn("Hub返回空数据 - serverId: {}", serverId);
    return createUnknownTag(serverId); // 返回未知状态标签
}
```

### 3. 保留的功能
**不要**移除或修改以下方法：
- `MonitoringService.saveUserActivities()`
- `MonitoringService.getUserActivities()`
- `MonitoringService.fetchTopProcesses()`
- `MonitoringService.killProcess()`

### 4. 依赖注入
`MetricsHubClient` 已配置为可选依赖：
```java
@Autowired(required = false)
private MetricsHubClient metricsHubClient;
```

---

## 🧪 测试验证

### 单元测试
每个迁移的服务都需要：
1. 测试 Hub 可用时的正常流程
2. 测试 Hub 不可用时的降级处理
3. 测试 Hub 返回空数据时的处理

### 集成测试
1. 启动 Hub 模块
2. 验证资源监控告警功能
3. 验证服务器状态标签生成
4. 验证系统健康度计算

### 回归测试
确保以下功能不受影响：
- ✅ 用户活动监控
- ✅ 进程管理（查看、结束）
- ✅ 服务器 SSH 连接管理

---

## 📈 迁移后收益

### 性能提升
- ❌ 旧: SSH轮询(60s) → 解析 → H2存储 → 查询
- ✅ 新: OTLP Agent(10s) → Hub → TSDB → 直接查询
- **数据实时性**: 60秒 → 10秒 ⚡
- **查询延迟**: 显著降低

### 架构优化
- 移除对废弃 H2 数据库的依赖
- 统一到 Hub + TSDB 架构
- 降低主应用复杂度

### 可维护性
- 监控数据收集逻辑集中在 Hub
- 主应用只负责业务逻辑
- 清晰的职责划分

---

## 📝 后续工作

### 迁移完成后
1. ✅ 移除 `MonitoringService` 中所有废弃方法
2. ✅ 移除 H2 数据库相关代码
3. ✅ 更新文档和架构图
4. ✅ 清理未使用的依赖

### 监控优化
1. 调整 OTLP Agent 采样间隔（如需要）
2. 优化 Hub 查询性能
3. 配置 TSDB 保留策略

---

## 🎯 总结

### 当前状态
- ✅ `MonitoringController` 已完成迁移
- ⏳ 5个服务类待迁移
- ⏳ 8个废弃方法调用需替换

### 迁移路径
```
Phase 1 (立即) → Phase 2 (本周) → Phase 3 (下周) → 清理工作
```

### 风险评估
- **技术风险**: 低 - 替换逻辑简单
- **业务风险**: 中 - 影响实时告警
- **回滚方案**: 保留 `MonitoringService` 代码直到验证完成

---

**报告生成时间**: 2025-10-17  
**下次更新**: 完成 Phase 1 迁移后
