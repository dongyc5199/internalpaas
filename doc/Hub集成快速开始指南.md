# Metrics Hub 集成快速开始指南

**版本**: 1.0
**更新日期**: 2025-10-16

---

## 📋 前置条件

1. ✅ 主应用已编译成功 (`mvn clean compile`)
2. ✅ Hub服务已部署（可选，未部署则自动使用SSH监控）
3. ✅ Java 17+ 运行环境

---

## 🚀 快速开始

### 方案A: Hub启用模式（推荐）

```bash
# 1. 启动Hub服务（如果有）
cd hub && ./start.sh

# 2. 验证Hub健康
curl http://localhost:8081/actuator/health
# 预期输出: {"status":"UP"}

# 3. 启动主应用（Hub默认启用）
mvn spring-boot:run

# 4. 测试监控API
curl http://localhost:9090/monitoring/server/1/metrics

# 预期: dataSource 字段为 "hub"
```

### 方案B: SSH专用模式

```bash
# 1. 启动主应用（禁用Hub）
mvn spring-boot:run -Dspring-boot.run.arguments="--metrics.hub.enabled=false"

# 2. 测试监控API
curl http://localhost:9090/monitoring/server/1/metrics

# 预期: dataSource 字段为 "ssh"
```

---

## ⚙️ 配置说明

### 核心配置项 (application.properties)

```properties
# 启用/禁用 Hub 集成
metrics.hub.enabled=true

# Hub 服务地址
metrics.hub.base-url=http://localhost:8081

# 超时配置（毫秒）
metrics.hub.timeout.connect-ms=5000
metrics.hub.timeout.read-ms=10000
```

### 环境变量覆盖

```bash
# 动态修改Hub地址
export METRICS_HUB_BASE_URL=http://192.168.1.100:8081
java -jar app.jar

# 动态禁用Hub
export METRICS_HUB_ENABLED=false
java -jar app.jar
```

---

## 🧪 验证测试

### 测试1: Hub优先策略

```bash
# 确保Hub服务运行
curl http://localhost:8081/actuator/health

# 调用监控API
curl http://localhost:9090/monitoring/server/1/metrics | jq .

# 验证点:
# ✅ 响应中 "dataSource": "hub"
# ✅ 日志显示: ✅ 数据来源: Metrics Hub (准实时)
```

### 测试2: SSH降级策略

```bash
# 停止Hub服务（模拟故障）
# curl -X POST http://localhost:8081/actuator/shutdown

# 调用监控API
curl http://localhost:9090/monitoring/server/1/metrics | jq .

# 验证点:
# ✅ 响应中 "dataSource": "ssh"
# ✅ 日志显示: ⚠️ 数据来源: SSH轮询（降级）
```

### 测试3: 批量查询

```bash
curl http://localhost:9090/monitoring/servers/metrics | jq .

# 验证点:
# ✅ 返回 dataSourceStats 统计信息
# ✅ 不同服务器可能来自不同数据源
```

---

## 📊 数据源识别

所有监控API响应中都包含 `dataSource` 字段：

```json
{
  "dataSource": "hub",  // 或 "ssh"
  "metrics": {
    "serverId": 1,
    "cpuUsage": 45.2,
    "memoryUsage": 68.5,
    // ...
  },
  "timestamp": 1697452800000
}
```

| dataSource值 | 说明 | 延迟 |
|-------------|------|------|
| `"hub"` | 来自Metrics Hub | 10-15秒 |
| `"ssh"` | 来自SSH轮询 | 1-5分钟 |

---

## 🔧 故障排查

### 问题1: Hub连接失败

**症状**: 日志显示 `Hub查询失败，降级到SSH`

**检查步骤**:
```bash
# 1. 检查Hub服务状态
curl http://localhost:8081/actuator/health

# 2. 检查配置
cat application.properties | grep metrics.hub

# 3. 测试网络连通性
telnet localhost 8081
```

**解决方案**:
- Hub未启动 → 启动Hub服务
- 地址配置错误 → 修改 `metrics.hub.base-url`
- 防火墙阻止 → 开放8081端口

### 问题2: 数据始终来自SSH

**症状**: `dataSource` 始终为 `"ssh"`

**检查步骤**:
```bash
# 1. 确认Hub已启用
cat application.properties | grep "metrics.hub.enabled"
# 预期: metrics.hub.enabled=true

# 2. 查看启动日志
# 预期看到: ✅ Metrics Hub Client initialized (T4): http://localhost:8081

# 3. 手动测试Hub API
curl http://localhost:8081/monitoring/server/1/metrics/query
```

**解决方案**:
- 配置禁用 → 设置 `metrics.hub.enabled=true`
- Hub API异常 → 检查Hub服务日志

### 问题3: Hub响应慢

**症状**: API响应超过2秒

**解决方案**:
```properties
# 增加超时时间
metrics.hub.timeout.connect-ms=10000
metrics.hub.timeout.read-ms=20000

# 或禁用Hub，仅使用SSH
metrics.hub.enabled=false
```

---

## 🎯 最佳实践

### 1. 生产环境部署

```bash
# 使用 Hub 配置文件
java -jar app.jar --spring.profiles.active=hub

# 配置文件: application-hub.properties
metrics.hub.base-url=http://hub.production.com:8081
metrics.hub.timeout.connect-ms=3000
metrics.hub.timeout.read-ms=8000
```

### 2. 开发环境部署

```bash
# Hub启用模式（默认）
mvn spring-boot:run

# SSH专用模式（Hub未部署时）
mvn spring-boot:run -Dspring-boot.run.profiles=ssh
```

### 3. 灰度发布策略

```properties
# 阶段1: 仅部分服务器启用Hub（通过配置）
# 阶段2: 逐步增加Hub覆盖范围
# 阶段3: 全量切换到Hub

# 可通过配置开关快速回滚
metrics.hub.enabled=false
```

---

## 📈 性能优化建议

### 1. Hub服务优化

- 确保Redis内存充足 (推荐 >= 2GB)
- 使用TSDB (TimescaleDB) 存储历史数据
- 启用HTTP Keep-Alive连接复用

### 2. 主应用优化

```properties
# 健康检查缓存（避免频繁检查）
metrics.hub.health-check.interval-ms=60000

# 连接池优化
metrics.hub.timeout.connect-ms=3000
metrics.hub.timeout.read-ms=8000
```

### 3. 监控调优

```properties
# 减少日志输出（生产环境）
logging.level.com.cmict.internalpaas.client.MetricsHubClient=WARN
logging.level.com.cmict.internalpaas.controller.MonitoringController=INFO
```

---

## 📚 API参考

### GET /monitoring/server/{id}/metrics

获取单个服务器最新监控数据

**响应示例**:
```json
{
  "dataSource": "hub",
  "metrics": {
    "serverId": 1,
    "serverName": "app-server-01",
    "hostname": "192.168.1.100",
    "cpuUsage": 45.2,
    "memoryUsage": 68.5,
    "diskUsage": 55.8,
    "networkReceivedBytes": 12345678,
    "networkTransmittedBytes": 23456789,
    "timestamp": "2025-10-16T14:30:00"
  },
  "timestamp": 1697452800000
}
```

### GET /monitoring/servers/metrics

批量获取所有服务器监控数据

**响应示例**:
```json
{
  "metrics": {
    "server1": { ... },
    "server2": { ... }
  },
  "dataSourceStats": {
    "hub": 5,
    "ssh": 3,
    "total": 8
  },
  "timestamp": 1697452800000
}
```

---

## 🆘 获取帮助

### 查看日志

```bash
# 实时查看日志
tail -f logs/application.log

# 过滤Hub相关日志
tail -f logs/application.log | grep MetricsHubClient

# 过滤数据源日志
tail -f logs/application.log | grep "数据来源"
```

### 启用详细日志

```properties
# application.properties
logging.level.com.cmict.internalpaas.client.MetricsHubClient=DEBUG
logging.level.com.cmict.internalpaas.controller.MonitoringController=DEBUG
```

### 常用命令

```bash
# 检查Hub连接
curl -I http://localhost:8081/actuator/health

# 测试Hub API
curl http://localhost:8081/monitoring/server/1/metrics/query

# 查看主应用配置
curl http://localhost:9090/actuator/configprops | jq '.contexts.application.beans.metricsHubClient'

# 重启服务（快速禁用Hub）
kill -HUP <PID>
# 或
systemctl restart devplatform
```

---

## 📞 联系支持

- **文档**: [Hub模块集成完成报告](./Hub模块集成完成报告.md)
- **API文档**: [OpenAPI规范](../docs/openapi/openapi.yaml)
- **团队**: Dev Debug Platform Team

---

**总结**:
- ✅ Hub启用 = 准实时监控 (10-15秒延迟)
- ✅ Hub禁用 = SSH监控 (1-5分钟延迟)
- ✅ 自动降级 = 服务永不中断

**立即开始**: `mvn spring-boot:run` 🚀
