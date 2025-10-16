# Hub 模块集成方案与时机分析

**文档日期**: 2025-10-16
**分析目的**: 确定Hub模块集成到主应用的最佳时机和实施路径

---

## 一、当前状态评估

### 1.1 任务完成情况

| 任务 | 名称 | 状态 | 说明 |
|------|------|------|------|
| T1 | OTLP接收器 | ✅ 完成 | gRPC(4317) + HTTP(4318) |
| T2 | 归一化器 | ✅ 完成 | OTLP → MetricSample |
| T3 | 双写存储 | ✅ 完成 | Redis + TSDB |
| T4 | 读取API | ✅ 完成 | 查询接口 + 数据源选择 |
| T5 | 聚合保留 | ⏳ 待实施 | 降采样 + 数据保留 |
| T6 | Agent部署 | ⏳ 待实施 | 自动化部署 |
| T7 | 安全加固 | ⏳ 待实施 | TLS + 认证 |
| T8 | 性能优化 | ⏳ 待实施 | 规模化优化 |

### 1.2 核心功能链路状态

```
数据流完整性检查:
┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
│ T1 接收 │──>│ T2 归一化│──>│ T3 存储 │──>│ T4 读取 │
│   ✅    │   │   ✅    │   │   ✅    │   │   ✅    │
└─────────┘   └─────────┘   └─────────┘   └─────────┘

结论: ✅ 数据流完整，可以开始集成
```

### 1.3 系统架构对比

#### 现有系统 (主应用)
```
监控方式: SSH轮询 → 远程命令 → 解析结果
数据存储: H2数据库 (server_metrics表)
数据模型: ServerMetrics (传统监控指标)
查询方式: JPA Repository
实时性: 定时轮询 (1-5分钟)
```

#### Hub模块 (指标中心)
```
监控方式: OTLP推送 → 接收 → 归一化
数据存储: Redis (热) + PostgreSQL/Timescale (冷)
数据模型: MetricSample (OTLP标准)
查询方式: 智能路由 (Redis/TSDB)
实时性: 准实时 (10-15秒)
```

---

## 二、集成阶段划分

### ✅ **阶段0**: 当前状态（已完成）
**完成时间**: 2025-10-16
**状态**: ✅ 就绪

**特点**:
- Hub模块独立运行
- 主应用独立运行
- 两者完全隔离
- T1-T4功能完整

**适用场景**:
- ✅ Hub模块独立开发和测试
- ✅ API规范验证
- ✅ 性能基准测试

---

### 🎯 **阶段1**: API集成（推荐立即开始）
**预计耗时**: 1-2天
**复杂度**: ⭐⭐ (中低)
**风险**: 低

#### 2.1.1 集成目标
- 主应用通过HTTP调用Hub的读取API
- 保留现有SSH监控作为降级方案
- 前端无需改动

#### 2.1.2 实施步骤

##### Step 1: 创建MetricsHubClient（适配器）
**新建文件**: `src/main/java/com/cmict/internalpaas/client/MetricsHubClient.java`

```java
@Component
@ConfigurationProperties(prefix = "metrics-hub")
public class MetricsHubClient {

    private String baseUrl = "http://localhost:8081"; // Hub服务地址
    private RestTemplate restTemplate;

    /**
     * 查询服务器指标（调用Hub API）
     */
    public List<MetricSample> queryMetrics(
            String serverId, Long from, Long to,
            Integer step, String fields) {

        String url = String.format(
            "%s/monitoring/server/%s/metrics?from=%d&to=%d&step=%d&fields=%s",
            baseUrl, serverId, from, to, step, fields);

        // 调用Hub API
        ResponseEntity<MetricsResponse> response =
            restTemplate.getForEntity(url, MetricsResponse.class);

        return response.getBody().getData();
    }

    /**
     * 转换MetricSample → ServerMetrics
     */
    public ServerMetrics convertToServerMetrics(List<MetricSample> samples) {
        // 数据模型转换逻辑
        ServerMetrics metrics = new ServerMetrics();

        for (MetricSample sample : samples) {
            switch (sample.getName()) {
                case "system.cpu.usage":
                    metrics.setCpuUsage(sample.getValue());
                    break;
                case "system.memory.usage":
                    metrics.setMemoryUsage(sample.getValue());
                    break;
                // ... 其他字段映射
            }
        }

        return metrics;
    }
}
```

##### Step 2: 扩展MonitoringController（智能路由）
**修改文件**: `MonitoringController.java`

```java
@Autowired(required = false) // 可选依赖，Hub未启动时不报错
private MetricsHubClient metricsHubClient;

@GetMapping("/server/{id}/metrics")
public ResponseEntity<?> getServerMetrics(@PathVariable Long id) {
    // 1. 尝试从Hub读取
    if (metricsHubClient != null && metricsHubClient.isAvailable()) {
        try {
            List<MetricSample> samples = metricsHubClient.queryMetrics(
                String.valueOf(id), null, null, null, null);

            ServerMetrics metrics = metricsHubClient.convertToServerMetrics(samples);

            logger.info("✅ 数据来源: Metrics Hub");
            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            logger.warn("Hub查询失败，降级到SSH: {}", e.getMessage());
        }
    }

    // 2. 降级: SSH轮询（现有逻辑）
    logger.info("⚠️ 数据来源: SSH轮询（降级）");
    ServerMetrics metrics = serverService.getServerLatestMetrics(id);
    return ResponseEntity.ok(metrics);
}
```

##### Step 3: 配置文件
**修改文件**: `application.properties`

```properties
# Metrics Hub 配置
metrics-hub.base-url=http://localhost:8081
metrics-hub.enabled=true
metrics-hub.timeout=5000
metrics-hub.fallback-to-ssh=true
```

#### 2.1.3 验证测试

```bash
# 1. 启动Hub服务
cd hub && ./start.sh

# 2. 启动主应用
mvn spring-boot:run

# 3. 测试API（应该从Hub读取）
curl http://localhost:8080/monitoring/server/1/metrics

# 4. 停止Hub服务，测试降级
# 应该自动降级到SSH轮询
```

#### 2.1.4 优势
- ✅ **零破坏**: 现有功能完全保留
- ✅ **渐进式**: 可按服务器逐步切换
- ✅ **可回退**: 随时禁用Hub集成
- ✅ **快速**: 1-2天即可完成

---

### 🚀 **阶段2**: Agent部署集成（T6完成后）
**预计耗时**: 3-5天
**复杂度**: ⭐⭐⭐⭐ (高)
**风险**: 中

**前置条件**:
- ✅ T1-T4完成
- ⏳ T6 (Agent自动部署) 完成

**集成内容**:
1. 服务器创建时自动部署OpenTelemetry Collector
2. 配置渲染（otelcol.yaml.tmpl → otelcol.yaml）
3. systemd服务管理
4. 健康检查和重试

**关键代码**:
```java
@EventListener
public void onServerCreated(ServerCreatedEvent event) {
    Server server = event.getServer();

    // 1. 上传agent二进制
    agentDeployService.uploadCollector(server);

    // 2. 渲染配置文件
    String config = templateEngine.render("otelcol.yaml.tmpl", Map.of(
        "hubEndpoint", metricsHubUrl,
        "serverId", server.getId()
    ));

    // 3. 启动systemd服务
    sshService.executeCommand(server, "systemctl start otelcol");
}
```

---

### 🔄 **阶段3**: 数据双写过渡（可选）
**预计耗时**: 2-3天
**复杂度**: ⭐⭐⭐ (中高)
**风险**: 中

**目的**: 平滑迁移，新旧系统并行

**实施方案**:
```java
@Scheduled(fixedRate = 60000) // 1分钟轮询
public void syncMetrics() {
    // 1. 从Hub读取最新数据
    List<MetricSample> samples = metricsHubClient.queryMetrics(...);

    // 2. 转换并写入H2数据库（兼容旧系统）
    ServerMetrics metrics = converter.convert(samples);
    metricsRepository.save(metrics);

    // 3. 保证前端无感知
}
```

**优势**:
- 前端无需改动
- 新旧数据同时可用
- 逐步验证Hub稳定性

---

### 📊 **阶段4**: 数据存储迁移（生产就绪）
**预计耗时**: 1周
**复杂度**: ⭐⭐⭐⭐⭐ (最高)
**风险**: 高

**前置条件**:
- ✅ T1-T4完成
- ✅ T5 (聚合保留) 完成
- ✅ 阶段1-3验证通过

**迁移内容**:
1. 停用H2，切换到PostgreSQL/Timescale
2. 历史数据迁移
3. 前端改造（直接调用Hub API）
4. SSH监控完全移除

---

## 三、推荐集成时机矩阵

| 阶段 | 何时开始 | 前置条件 | 风险 | 收益 |
|------|---------|---------|------|------|
| **阶段1: API集成** | ✅ **立即** | T1-T4完成 | 低 | 中 - 准实时数据 |
| 阶段2: Agent部署 | T6完成后 | T1-T4+T6 | 中 | 高 - 自动化 |
| 阶段3: 数据双写 | 可选/并行 | T1-T4 | 中 | 中 - 平滑过渡 |
| 阶段4: 存储迁移 | 生产验证后 | T1-T5+稳定性 | 高 | 高 - 完全现代化 |

---

## 四、立即可行的行动计划

### 🎯 本周可完成（阶段1）

#### Day 1: 准备工作
- [ ] 确认Hub服务可启动 (`cd hub && ./start.sh`)
- [ ] 验证T1-T4功能正常
- [ ] 准备测试服务器

#### Day 2: 开发MetricsHubClient
- [ ] 创建`MetricsHubClient.java`
- [ ] 实现HTTP调用逻辑
- [ ] 实现数据模型转换器
- [ ] 单元测试

#### Day 3: 集成到Controller
- [ ] 修改`MonitoringController`添加智能路由
- [ ] 实现降级逻辑
- [ ] 添加配置开关
- [ ] 集成测试

#### Day 4: 测试和调优
- [ ] 端到端测试
- [ ] 性能对比（Hub vs SSH）
- [ ] 降级场景测试
- [ ] 文档更新

---

## 五、技术细节要点

### 5.1 数据模型映射

#### Hub → 主应用转换表

| Hub (MetricSample) | 主应用 (ServerMetrics) | 转换逻辑 |
|-------------------|----------------------|---------|
| `system.cpu.usage` | `cpuUsage` | 直接赋值 (%) |
| `system.memory.usage` | `memoryUsage` | 直接赋值 (%) |
| `system.memory.total` | `memoryTotal` | 直接赋值 (bytes) |
| `system.disk.usage` | `diskUsage` | 直接赋值 (%) |
| `system.network.io.receive` | `networkReceivedBytes` | 直接赋值 |
| `system.load.average` | `loadAverage` | 格式化字符串 |

### 5.2 错误处理策略

```java
// 三级降级策略
public ServerMetrics getMetrics(Long serverId) {
    // Level 1: Hub Redis (最快)
    try {
        return hubClient.queryFromRedis(serverId);
    } catch (Exception e1) {
        logger.warn("Redis查询失败: {}", e1.getMessage());

        // Level 2: Hub TSDB (较快)
        try {
            return hubClient.queryFromTsdb(serverId);
        } catch (Exception e2) {
            logger.warn("TSDB查询失败: {}", e2.getMessage());

            // Level 3: SSH轮询 (兜底)
            return sshMonitoringService.getMetrics(serverId);
        }
    }
}
```

### 5.3 配置管理

```yaml
# application.yml
metrics-hub:
  enabled: ${METRICS_HUB_ENABLED:true}
  base-url: ${METRICS_HUB_URL:http://localhost:8081}
  timeout:
    connect: 3000
    read: 5000
  fallback:
    enabled: true
    method: ssh
  retry:
    max-attempts: 3
    backoff: 1000
```

---

## 六、风险评估与应对

### 6.1 主要风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| Hub服务宕机 | 中 | 低 | 自动降级到SSH |
| 数据模型不兼容 | 高 | 中 | 完善转换器 + 测试 |
| 性能退化 | 中 | 低 | 性能测试 + 缓存 |
| 网络延迟 | 低 | 中 | 超时控制 + 降级 |

### 6.2 回滚方案

```properties
# 一键禁用Hub集成
metrics-hub.enabled=false

# 或通过环境变量
export METRICS_HUB_ENABLED=false
mvn spring-boot:run
```

---

## 七、总结与建议

### ✅ 立即可行
**阶段1 (API集成)** 是当前最佳选择：

1. **前置条件满足**: T1-T4已完成 ✅
2. **风险可控**: 有降级机制 ✅
3. **投入产出比高**: 2天开发 → 准实时监控 ✅
4. **无破坏性**: 完全向后兼容 ✅

### 🎯 实施建议

**本周行动**:
```bash
# 1. 验证Hub服务
cd hub && ./start.sh
curl http://localhost:8081/actuator/health

# 2. 创建集成分支
git checkout -b feat/integrate-metrics-hub

# 3. 实施阶段1集成
# ... (按上述步骤)

# 4. 测试验证
mvn test
```

### 📈 预期效果

集成完成后：
- ⏱️ **延迟**: 1-5分钟 → 10-15秒
- 📊 **实时性**: 轮询 → 准实时推送
- 💾 **数据保留**: 7天 → 30-90天
- 🔍 **查询能力**: 简单 → 时间范围+降采样+字段过滤

---

**结论**: ✅ **立即开始阶段1 (API集成)**，预计2-3天完成，风险低、收益高。

---

**文档维护**:
- 创建时间: 2025-10-16
- 最后更新: 2025-10-16
- 负责人: Dev Team
