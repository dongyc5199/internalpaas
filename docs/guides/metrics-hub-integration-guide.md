# Metrics Hub 集成指南 - 现有系统改造方案

## 📋 一、现状分析

### 当前监控流程
```
前端 → MonitoringController → MonitoringService → SSH命令 → 服务器
                                     ↓
                              ServerMetrics (返回)
```

**核心文件**:
- `MonitoringController.java` - API入口 (L72: `getServerMetrics()`)
- `MonitoringService.java` - SSH执行逻辑
- `ServerMetrics.java` - 数据模型

---

## 🎯 二、集成策略（渐进式，零破坏）

### 集成后的流程
```
前端 (不变)
  ↓
MonitoringController (小幅改造)
  ↓
  ├─→ metricsHubEnabled=true  → MetricsHubClient → Metrics Hub
  │                                                    ├─→ Redis
  │                                                    └─→ TSDB
  └─→ metricsHubEnabled=false → MonitoringService (SSH) ← 降级
```

**关键原则**:
1. ✅ **前端零改动** - API接口保持兼容
2. ✅ **功能开关控制** - 配置文件一键开关
3. ✅ **自动降级** - Hub故障自动切SSH
4. ✅ **逐步迁移** - 可以只启用部分服务器

---

## 🔧 三、具体改造步骤

### Step 1: 添加配置（application.properties）

在 `src/main/resources/application.properties` 末尾添加：

```properties
# ==================== Metrics Hub 集成配置 ====================

# Metrics Hub 功能总开关 (默认关闭，不影响现有系统)
metrics.hub.enabled=false

# Metrics Hub 服务地址 (独立部署的 Metrics Hub 服务)
metrics.hub.base-url=http://localhost:8081

# 降级开关 (Hub不可用时自动切换到SSH)
metrics.hub.fallback-to-ssh=true

# 连接超时配置
metrics.hub.timeout.connect-ms=5000
metrics.hub.timeout.read-ms=10000

# Agent 自动部署配置 (可选，Phase 2启用)
metrics.agent.auto-deploy=false
metrics.agent.otlp-endpoint=http://localhost:4318/v1/metrics

# ==================== 结束 ====================
```

**说明**: 初始状态 `metrics.hub.enabled=false`，现有系统继续使用SSH，互不影响。

---

### Step 2: 创建 MetricsHubClient 类

新建文件: `src/main/java/com/cmict/internalpaas/client/MetricsHubClient.java`

```java
package com.cmict.internalpaas.client;

import com.cmict.internalpaas.model.ServerMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * Metrics Hub 客户端
 * 负责与独立部署的 Metrics Hub 服务通信
 */
@Component
public class MetricsHubClient {

    private static final Logger logger = LoggerFactory.getLogger(MetricsHubClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public MetricsHubClient(
            RestTemplateBuilder builder,
            @Value("${metrics.hub.base-url}") String baseUrl,
            @Value("${metrics.hub.timeout.connect-ms:5000}") int connectTimeout,
            @Value("${metrics.hub.timeout.read-ms:10000}") int readTimeout) {

        this.baseUrl = baseUrl;
        this.restTemplate = builder
            .setConnectTimeout(Duration.ofMillis(connectTimeout))
            .setReadTimeout(Duration.ofMillis(readTimeout))
            .build();

        logger.info("Metrics Hub Client initialized: {}", baseUrl);
    }

    /**
     * 获取服务器最新指标
     * 对应 API: GET /api/v1/metrics/server/{id}/latest
     */
    public ServerMetrics getLatestMetrics(Long serverId) {
        try {
            String url = String.format("%s/api/v1/metrics/server/%d/latest", baseUrl, serverId);
            logger.debug("Fetching metrics from Hub: {}", url);

            ServerMetrics metrics = restTemplate.getForObject(url, ServerMetrics.class);

            logger.debug("Successfully fetched metrics for server {}", serverId);
            return metrics;

        } catch (Exception e) {
            logger.error("Failed to fetch metrics from Hub for server {}: {}", serverId, e.getMessage());
            throw new MetricsHubException("Metrics Hub unavailable", e);
        }
    }

    /**
     * 获取历史指标
     * 对应 API: GET /api/v1/metrics/server/{id}/range?from=xxx&to=xxx
     */
    public ServerMetrics getRangeMetrics(Long serverId, Long from, Long to) {
        try {
            String url = String.format(
                "%s/api/v1/metrics/server/%d/range?from=%d&to=%d",
                baseUrl, serverId, from, to
            );

            logger.debug("Fetching range metrics from Hub: {}", url);
            return restTemplate.getForObject(url, ServerMetrics.class);

        } catch (Exception e) {
            logger.error("Failed to fetch range metrics from Hub: {}", e.getMessage());
            throw new MetricsHubException("Metrics Hub unavailable", e);
        }
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        try {
            String url = baseUrl + "/actuator/health";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return "UP".equals(response.get("status"));
        } catch (Exception e) {
            logger.warn("Metrics Hub health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Metrics Hub 异常
     */
    public static class MetricsHubException extends RuntimeException {
        public MetricsHubException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

---

### Step 3: 改造 MonitoringController（最小改动）

修改 `src/main/java/com/cmict/internalpaas/controller/MonitoringController.java`

**在类开头添加**:

```java
@Autowired(required = false)  // 可选依赖，不影响现有功能
private MetricsHubClient metricsHubClient;

@Value("${metrics.hub.enabled:false}")
private boolean metricsHubEnabled;

@Value("${metrics.hub.fallback-to-ssh:true}")
private boolean fallbackEnabled;
```

**修改 `getServerMetrics()` 方法** (L72-114):

```java
/**
 * 获取服务器最新监控数据
 * 集成 Metrics Hub: 优先使用 Hub，失败则降级到 SSH
 */
@GetMapping("/server/{id}/metrics")
@ResponseBody
public ResponseEntity<?> getServerMetrics(
        @PathVariable Long id,
        @RequestParam(required = false) Long from,
        @RequestParam(required = false) Long to) {
    try {
        // 日志记录
        logger.info("请求监控数据 - 服务器ID: {}, Hub启用: {}", id, metricsHubEnabled);

        // 首先检查服务器是否存在
        if (!serverService.findById(id).isPresent()) {
            logger.warn("服务器不存在 - ID: {}", id);
            Map<String, String> error = new HashMap<>();
            error.put("error", "服务器不存在");
            error.put("serverId", String.valueOf(id));
            return ResponseEntity.status(404).body(error);
        }

        // ========== 新增: Metrics Hub 路由逻辑 ==========
        if (metricsHubEnabled && metricsHubClient != null) {
            try {
                logger.debug("尝试从 Metrics Hub 获取数据 - 服务器ID: {}", id);

                ServerMetrics metrics;
                if (from != null && to != null) {
                    // 历史范围查询
                    metrics = metricsHubClient.getRangeMetrics(id, from, to);
                } else {
                    // 最新数据查询
                    metrics = metricsHubClient.getLatestMetrics(id);
                }

                if (metrics != null) {
                    logger.info("成功从 Metrics Hub 获取数据 - 服务器ID: {}", id);
                    return ResponseEntity.ok(metrics);
                }

            } catch (Exception e) {
                logger.error("Metrics Hub 查询失败 - 服务器ID: {}, 错误: {}", id, e.getMessage());

                // 降级处理
                if (!fallbackEnabled) {
                    // 如果禁用降级，直接返回错误
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Metrics Hub 不可用，且降级已禁用");
                    error.put("serverId", String.valueOf(id));
                    return ResponseEntity.status(503).body(error);
                }

                logger.warn("降级到 SSH 轮询模式 - 服务器ID: {}", id);
                // 继续执行下面的 SSH 逻辑
            }
        }
        // ========== Metrics Hub 逻辑结束 ==========

        // 原有 SSH 逻辑 (保持不变，作为降级方案)
        ServerMetrics metrics = serverService.getServerLatestMetrics(id);
        logger.debug("SSH 获取监控数据结果 - metrics: {}", (metrics != null ? "有数据" : "无数据"));

        if (metrics == null) {
            // 如果没有数据，尝试刷新
            logger.debug("尝试刷新服务器监控数据 - ID: {}", id);
            metrics = serverService.refreshServerMetrics(id);
        }

        if (metrics == null) {
            logger.warn("监控数据为空 - ID: {}", id);
            Map<String, String> error = new HashMap<>();
            error.put("error", "监控数据不可用");
            error.put("serverId", String.valueOf(id));
            error.put("source", "ssh");  // 标识数据来源
            return ResponseEntity.status(404).body(error);
        }

        return ResponseEntity.ok(metrics);

    } catch (Exception e) {
        logger.error("获取监控数据异常 - ID: {}, 错误: {}", id, e.getMessage(), e);
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        error.put("serverId", String.valueOf(id));
        return ResponseEntity.internalServerError().body(error);
    }
}
```

**添加新的健康检查端点**:

```java
/**
 * 监控系统健康检查
 * 返回当前使用的数据源和状态
 */
@GetMapping("/health")
@ResponseBody
public ResponseEntity<Map<String, Object>> getMonitoringHealth() {
    Map<String, Object> health = new HashMap<>();

    health.put("metricsHubEnabled", metricsHubEnabled);
    health.put("fallbackEnabled", fallbackEnabled);

    if (metricsHubEnabled && metricsHubClient != null) {
        boolean hubHealthy = metricsHubClient.isHealthy();
        health.put("metricsHub", hubHealthy ? "UP" : "DOWN");
    } else {
        health.put("metricsHub", "DISABLED");
    }

    health.put("sshPolling", "UP");  // SSH 始终可用

    return ResponseEntity.ok(health);
}
```

---

### Step 4: 编译验证（不启动 Metrics Hub）

```bash
# 编译项目
mvn clean compile

# 启动应用（Metrics Hub 功能默认关闭）
mvn spring-boot:run

# 测试现有功能是否正常
curl http://localhost:9090/monitoring/server/1/metrics

# 检查监控健康状态
curl http://localhost:9090/monitoring/health
# 应该返回: {"metricsHubEnabled":false, "metricsHub":"DISABLED", "sshPolling":"UP"}
```

**预期结果**:
- ✅ 编译成功
- ✅ 现有SSH监控继续正常工作
- ✅ Metrics Hub 功能未启用，不影响任何现有功能

---

## 🚀 四、启用 Metrics Hub（可选，Phase 2）

### Step 1: 部署 Metrics Hub 服务

```bash
# 使用 Docker Compose 快速启动
cd metrics-hub
docker-compose up -d
```

### Step 2: 修改配置启用

```properties
# application.properties
metrics.hub.enabled=true           # 开启 Metrics Hub
metrics.hub.base-url=http://localhost:8081
metrics.hub.fallback-to-ssh=true  # 保持降级开关
```

### Step 3: 重启应用并测试

```bash
# 重启
mvn spring-boot:run

# 测试 (如果 Hub 可用，应该从 Hub 获取数据)
curl http://localhost:9090/monitoring/server/1/metrics

# 检查健康状态
curl http://localhost:9090/monitoring/health
# 应该返回: {"metricsHubEnabled":true, "metricsHub":"UP", "sshPolling":"UP"}
```

---

## 📊 五、测试场景

### 场景 1: Hub 正常工作
```bash
# 1. Hub 启动且健康
curl http://localhost:8081/actuator/health
# {"status":"UP"}

# 2. 请求监控数据（应该从 Hub 获取）
curl http://localhost:9090/monitoring/server/1/metrics

# 3. 查看日志
# 应该看到: "成功从 Metrics Hub 获取数据"
```

### 场景 2: Hub 故障，自动降级
```bash
# 1. 停止 Hub
docker-compose down

# 2. 请求监控数据（应该自动降级到 SSH）
curl http://localhost:9090/monitoring/server/1/metrics

# 3. 查看日志
# 应该看到: "Metrics Hub 查询失败" 和 "降级到 SSH 轮询模式"
```

### 场景 3: 禁用 Hub
```properties
# application.properties
metrics.hub.enabled=false
```

```bash
# 重启后，完全使用 SSH 模式（和现在一样）
curl http://localhost:9090/monitoring/server/1/metrics
```

---

## ✅ 六、集成检查清单

### 代码改动
- [ ] `application.properties` - 添加配置
- [ ] 创建 `MetricsHubClient.java`
- [ ] 修改 `MonitoringController.java` - 添加路由逻辑
- [ ] 编译通过，无错误

### 功能验证
- [ ] Hub 禁用时，SSH 监控正常工作
- [ ] Hub 启用且健康时，优先使用 Hub
- [ ] Hub 故障时，自动降级到 SSH
- [ ] 前端页面显示正常，无感知切换

### 性能测试
- [ ] Hub 查询延迟 < 300ms
- [ ] 降级切换时间 < 5s
- [ ] 无内存泄漏

---

## 🎯 七、阶段性目标

### Phase 1: 集成准备（当前）
- ✅ 添加配置和客户端类
- ✅ 改造 Controller 支持路由
- ✅ 验证不破坏现有功能

### Phase 2: Hub 部署（1-2周后）
- 部署 Metrics Hub 服务（独立）
- 启用配置 `metrics.hub.enabled=true`
- 小范围测试（3-5台服务器）

### Phase 3: 全面迁移（1个月后）
- 部署 Agent 到所有服务器
- 逐步切换数据源
- 监控性能和稳定性

### Phase 4: SSH 下线（3个月后）
- 确认 Hub 稳定运行
- 移除 SSH 轮询代码
- 完全切换到 OTLP 模式

---

## 📞 常见问题

### Q1: 改动后现有系统会受影响吗？
**A**: 不会。默认配置 `metrics.hub.enabled=false`，完全兼容现有系统。

### Q2: 如果 Metrics Hub 一直不部署呢？
**A**: 系统继续使用 SSH 模式，无任何影响。

### Q3: 可以只启用部分服务器吗？
**A**: 可以。在 Hub 中只部署特定服务器的 Agent，其他仍走 SSH。

### Q4: 出问题怎么快速回滚？
**A**: 修改配置 `metrics.hub.enabled=false`，重启即可。

---

## 🔗 相关文档

- [Metrics Hub 完整方案](./集成方案.md)
- [OTLP 协议说明](./T1-otlp-receiver.md)
- [Agent 部署指南](./T6-auto-deploy-agent.md)

---

**最后更新**: 2025-10-15
**文档版本**: v1.0
