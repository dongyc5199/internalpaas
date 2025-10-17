# 🚀 Metrics Hub 集成快速开始

## 📌 目标

在**不影响现有系统**的前提下，为你的 Dev Debug Platform 集成 Metrics Hub 支持。

**预计时间**: 30分钟
**风险等级**: 零 (默认禁用，可随时回滚)

---

## ✅ 步骤 1: 查看现有文档

你已经有以下文档可供参考：

1. **集成指南** (详细方案)
   📄 `doc/METRICS_HUB_INTEGRATION_GUIDE.md`

2. **可视化对比** (前后对比)
   📄 `doc/integration-visual-comparison.md`

3. **完整技术方案** (2400行详解)
   📄 `issues/集成方案.md`

---

## ✅ 步骤 2: 准备工作（5分钟）

### 2.1 确认当前状态

```bash
# 1. 进入项目目录
cd /e/work/code/internalpaas

# 2. 检查是否已创建 MetricsHubClient
ls src/main/java/com/cmict/internalpaas/client/

# 应该看到: MetricsHubClient.java

# 3. 检查配置示例
ls src/main/resources/application-metrics-hub.properties

# 应该存在此文件
```

### 2.2 添加配置

打开 `src/main/resources/application.properties`，在**文件末尾**添加：

```properties
# ==================== Metrics Hub 集成配置 ====================
# 功能开关 (默认禁用，不影响现有功能)
metrics.hub.enabled=false
metrics.hub.base-url=http://localhost:8081
metrics.hub.fallback-to-ssh=true
metrics.hub.timeout.connect-ms=5000
metrics.hub.timeout.read-ms=10000
```

---

## ✅ 步骤 3: 修改代码（10分钟）

### 3.1 修改 MonitoringController

打开 `src/main/java/com/cmict/internalpaas/controller/MonitoringController.java`

#### **在类开头添加字段** (约 L22):

```java
@Autowired(required = false)  // 可选依赖
private MetricsHubClient metricsHubClient;

@Value("${metrics.hub.enabled:false}")
private boolean metricsHubEnabled;

@Value("${metrics.hub.fallback-to-ssh:true}")
private boolean fallbackEnabled;

private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);
```

#### **修改 `getServerMetrics()` 方法** (约 L72):

在现有代码的 **检查服务器存在性之后，SSH获取之前** 插入以下代码：

```java
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
            logger.info("✅ 成功从 Metrics Hub 获取数据 - 服务器ID: {}", id);
            return ResponseEntity.ok(metrics);
        }

    } catch (Exception e) {
        logger.error("❌ Metrics Hub 查询失败 - 服务器ID: {}, 错误: {}", id, e.getMessage());

        if (!fallbackEnabled) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Metrics Hub 不可用，且降级已禁用");
            return ResponseEntity.status(503).body(error);
        }

        logger.warn("⚠️ 降级到 SSH 轮询模式 - 服务器ID: {}", id);
        // 继续执行下面的 SSH 逻辑
    }
}
// ========== Metrics Hub 逻辑结束 ==========

// 原有 SSH 逻辑继续...
```

#### **添加健康检查端点** (在类末尾):

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

    health.put("sshPolling", "UP");

    return ResponseEntity.ok(health);
}
```

### 3.2 添加必要的 import

在 `MonitoringController.java` 顶部确保有以下 import:

```java
import com.cmict.internalpaas.client.MetricsHubClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
```

---

## ✅ 步骤 4: 编译测试（5分钟）

### 4.1 编译项目

```bash
# 停止正在运行的应用 (如果有)
# Ctrl+C 或关闭终端

# 清理编译
mvn clean compile

# 预期结果: BUILD SUCCESS
```

### 4.2 启动应用

```bash
# 启动应用
mvn spring-boot:run

# 观察日志，应该看到:
# "Metrics Hub Client 未启用" 或类似提示
```

### 4.3 测试现有功能

```bash
# 新开一个终端窗口

# 测试监控API (应该和之前一样工作)
curl http://localhost:9090/monitoring/server/1/metrics

# 测试健康检查 (新增端点)
curl http://localhost:9090/monitoring/health

# 预期返回:
# {
#   "metricsHubEnabled": false,
#   "metricsHub": "DISABLED",
#   "sshPolling": "UP",
#   "fallbackEnabled": true
# }
```

**✅ 如果以上测试通过，说明集成成功！**

---

## ✅ 步骤 5: 验证回滚能力（2分钟）

### 5.1 测试配置切换

```bash
# 1. 停止应用

# 2. 修改 application.properties
# 将 metrics.hub.enabled=false 改为 true
metrics.hub.enabled=true

# 3. 重启应用
mvn spring-boot:run

# 4. 观察日志，应该看到:
# "✅ Metrics Hub Client initialized: http://localhost:8081"

# 5. 测试API (应该尝试连接 Hub，失败后降级到 SSH)
curl http://localhost:9090/monitoring/server/1/metrics

# 6. 观察日志，应该看到:
# "❌ Metrics Hub 查询失败"
# "⚠️ 降级到 SSH 轮询模式"

# 7. 改回禁用状态
metrics.hub.enabled=false

# 8. 重启验证
```

---

## ✅ 步骤 6: 下一步计划（根据需求）

### 选项 A: 暂时不部署 Metrics Hub（推荐）

```properties
# application.properties 保持
metrics.hub.enabled=false
```

**优势**:
- ✅ 代码已集成，随时可启用
- ✅ 完全不影响现有系统
- ✅ 为未来升级做好准备

### 选项 B: 部署 Metrics Hub 服务（Phase 2）

需要:
1. 部署 PostgreSQL/Timescale
2. 部署 Redis
3. 部署 Metrics Hub 服务
4. 部署 Agent 到目标服务器

参考文档:
- `issues/集成方案.md` (完整方案)
- `issues/T1-otlp-receiver.md` (OTLP接收器)
- `issues/T6-auto-deploy-agent.md` (Agent部署)

---

## 📊 集成检查清单

### 代码改动
- [x] `application.properties` - 添加配置 (6行)
- [x] 创建 `MetricsHubClient.java` (已自动创建)
- [ ] 修改 `MonitoringController.java` - 添加字段 (5行)
- [ ] 修改 `MonitoringController.java` - 修改方法 (30行)
- [ ] 修改 `MonitoringController.java` - 添加健康检查 (20行)

### 功能验证
- [ ] 编译通过
- [ ] Hub 禁用时，SSH 监控正常
- [ ] Hub 启用且不可用时，自动降级
- [ ] 健康检查端点可访问
- [ ] 前端页面显示正常

---

## 🆘 常见问题

### Q1: 编译报错 "找不到 MetricsHubClient"
**A**: 检查文件是否存在:
```bash
ls src/main/java/com/cmict/internalpaas/client/MetricsHubClient.java
```

### Q2: 启动报错 "Bean creation failed"
**A**: 检查配置:
```properties
# 确保有这行，且值为 false
metrics.hub.enabled=false
```

### Q3: 想完全移除集成代码
**A**: 删除以下内容即可:
- `MetricsHubClient.java`
- `application.properties` 中的 Metrics Hub 配置
- `MonitoringController.java` 中的新增代码

### Q4: 怎么知道是从 Hub 还是 SSH 获取的数据？
**A**: 查看日志:
- Hub: "✅ 成功从 Metrics Hub 获取数据"
- SSH: "SSH 获取监控数据结果"

---

## 📞 获取帮助

如果遇到问题:

1. **查看日志**:
   ```bash
   # 查看应用日志
   tail -f logs/application.log
   ```

2. **检查健康状态**:
   ```bash
   curl http://localhost:9090/monitoring/health
   ```

3. **参考完整文档**:
   - `doc/METRICS_HUB_INTEGRATION_GUIDE.md`
   - `doc/integration-visual-comparison.md`

---

## 🎯 成功标准

**集成成功的标志**:

✅ 编译无错误
✅ 启动无异常
✅ 现有监控功能正常
✅ 健康检查端点可访问
✅ 日志显示 Hub 状态

**恭喜你完成了 Metrics Hub 集成准备工作！**

---

**最后更新**: 2025-10-15
**版本**: v1.0
