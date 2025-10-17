# Agent 自动部署架构设计 (阶段2)

**设计日期**: 2025-10-16
**版本**: 1.0
**状态**: 设计中

---

## 一、设计目标

实现服务器创建后**自动部署OpenTelemetry Collector Agent**，实现：
- ✅ 零人工介入，自动化部署
- ✅ 预检验证，确保部署条件满足
- ✅ 实时进度反馈（WebSocket推送）
- ✅ 失败重试与回滚机制
- ✅ 健康检查与监控

---

## 二、整体架构

### 2.1 部署流程

```
┌─────────────────────────────────────────────────────────────────┐
│                   管理员添加服务器                               │
│               (POST /admin/servers)                              │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ↓
        ┌───────────────────────────┐
        │   ServerService.save()    │
        │   发布 ServerCreatedEvent │
        └───────────┬───────────────┘
                    │
                    ↓
        ┌───────────────────────────────────┐
        │   ServerCreatedListener (异步)    │
        │   监听事件 → 触发部署             │
        └───────────┬───────────────────────┘
                    │
                    ↓
┌───────────────────────────────────────────────────────────────┐
│                    AgentDeployService                          │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ Step 1: 预检查 (Pre-check)                              │  │
│  │  ├─ SSH连接测试                                          │  │
│  │  ├─ 权限检查 (sudo权限)                                  │  │
│  │  ├─ 磁盘空间检查 (>=100MB)                               │  │
│  │  └─ 端口占用检查 (4317/4318)                             │  │
│  │     ✅ 通过 → Step 2                                      │  │
│  │     ❌ 失败 → 记录错误 + WebSocket通知                   │  │
│  └─────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ Step 2: 文件上传 (Upload)                               │  │
│  │  ├─ 上传 agent.tar.gz (OTel Collector二进制)            │  │
│  │  ├─ 渲染并上传 otelcol.yaml (配置文件)                   │  │
│  │  └─ 上传 bootstrap.sh (安装脚本)                         │  │
│  │     ✅ 成功 → Step 3                                      │  │
│  │     ❌ 失败 → 清理 + 重试                                 │  │
│  └─────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ Step 3: 执行安装 (Execute)                              │  │
│  │  ├─ 解压: tar -xzf agent.tar.gz -C /opt/metrics-agent   │  │
│  │  ├─ 设置权限: chmod +x bootstrap.sh                      │  │
│  │  ├─ 执行脚本: sh bootstrap.sh install                    │  │
│  │  └─ 启动服务: systemctl enable --now otelcol             │  │
│  │     ✅ 成功 → Step 4                                      │  │
│  │     ❌ 失败 → 回滚 + 重试                                 │  │
│  └─────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ Step 4: 健康检查 (Health Check)                         │  │
│  │  ├─ systemctl status otelcol                             │  │
│  │  ├─ 检查进程: ps aux | grep otelcol                      │  │
│  │  ├─ 检查端口: netstat -tuln | grep 4317                  │  │
│  │  └─ 等待Hub收到首个指标 (30秒超时)                       │  │
│  │     ✅ 成功 → 部署完成                                    │  │
│  │     ❌ 失败 → 标记异常 + 告警                             │  │
│  └─────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────┐
│   WebSocket 实时进度推送        │
│   /topic/agent-deploy/{serverId}│
│   - 预检中 (10%)                │
│   - 上传中 (30%)                │
│   - 安装中 (60%)                │
│   - 健康检查中 (90%)            │
│   - 部署完成 (100%)             │
└─────────────────────────────────┘
```

### 2.2 重试与回滚策略

```
部署失败 → 重试策略 (指数退避)
  │
  ├─ 第1次失败: 等待 1分钟 → 重试
  ├─ 第2次失败: 等待 5分钟 → 重试
  └─ 第3次失败: 等待 15分钟 → 重试
      │
      └─ 仍失败 → 标记为 "部署失败"，停止重试
                   管理员可手动触发重试或回滚

回滚操作:
  1. systemctl stop otelcol
  2. systemctl disable otelcol
  3. rm -rf /opt/metrics-agent
  4. 清理日志和临时文件
```

---

## 三、核心组件设计

### 3.1 AgentDeployService

**职责**: Agent部署的核心业务逻辑

**主要方法**:
```java
public class AgentDeployService {

    /**
     * 部署Agent到指定服务器（异步）
     * @param server 目标服务器
     * @return CompletableFuture<DeployResult>
     */
    public CompletableFuture<DeployResult> deployAgent(Server server);

    /**
     * 预检查服务器环境
     * @param server 目标服务器
     * @return PreCheckResult (包含SSH、权限、磁盘、端口检查结果)
     */
    public PreCheckResult preCheck(Server server);

    /**
     * 上传Agent文件
     * @param server 目标服务器
     * @return UploadResult
     */
    public UploadResult uploadAgentFiles(Server server);

    /**
     * 执行安装脚本
     * @param server 目标服务器
     * @return ExecuteResult
     */
    public ExecuteResult executeInstallation(Server server);

    /**
     * 健康检查
     * @param server 目标服务器
     * @return HealthCheckResult
     */
    public HealthCheckResult performHealthCheck(Server server);

    /**
     * 回滚部署
     * @param server 目标服务器
     * @return RollbackResult
     */
    public RollbackResult rollback(Server server);

    /**
     * 重试部署
     * @param server 目标服务器
     * @param retryCount 当前重试次数
     * @return DeployResult
     */
    public DeployResult retryDeploy(Server server, int retryCount);
}
```

### 3.2 ServerCreatedListener

**职责**: 监听服务器创建事件，触发Agent部署

```java
@Component
public class ServerCreatedListener {

    @Autowired
    private AgentDeployService agentDeployService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 监听服务器创建事件
     */
    @EventListener
    @Async("agentDeployExecutor")
    public void onServerCreated(ServerCreatedEvent event) {
        Server server = event.getServer();

        // 检查是否启用自动部署
        if (!isAutoDeployEnabled()) {
            logger.info("Agent自动部署已禁用，跳过 - serverId: {}", server.getId());
            return;
        }

        logger.info("开始自动部署Agent - serverId: {}", server.getId());

        // 推送WebSocket通知
        sendProgress(server.getId(), "开始部署", 0);

        // 异步部署
        agentDeployService.deployAgent(server)
            .thenAccept(result -> {
                if (result.isSuccess()) {
                    logger.info("✅ Agent部署成功 - serverId: {}", server.getId());
                    sendProgress(server.getId(), "部署完成", 100);
                } else {
                    logger.error("❌ Agent部署失败 - serverId: {}, error: {}",
                                 server.getId(), result.getErrorMessage());
                    sendProgress(server.getId(), "部署失败: " + result.getErrorMessage(), -1);

                    // 触发重试
                    scheduleRetry(server, 1);
                }
            })
            .exceptionally(ex -> {
                logger.error("Agent部署异常 - serverId: {}", server.getId(), ex);
                sendProgress(server.getId(), "部署异常: " + ex.getMessage(), -1);
                return null;
            });
    }

    private void sendProgress(Long serverId, String message, int percentage) {
        Map<String, Object> progress = Map.of(
            "serverId", serverId,
            "message", message,
            "percentage", percentage,
            "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend("/topic/agent-deploy/" + serverId, progress);
    }
}
```

### 3.3 配置模板渲染

**OTel Collector配置模板** (`otelcol.yaml.tmpl`):
```yaml
# OpenTelemetry Collector Configuration
# Auto-generated by InternalPaaS Platform

receivers:
  hostmetrics:
    collection_interval: 10s
    scrapers:
      cpu:
        metrics:
          system.cpu.usage:
            enabled: true
      memory:
        metrics:
          system.memory.usage:
            enabled: true
          system.memory.total:
            enabled: true
      disk:
        metrics:
          system.disk.usage:
            enabled: true
      network:
        metrics:
          system.network.io.receive:
            enabled: true
          system.network.io.transmit:
            enabled: true

processors:
  batch:
    timeout: 10s
    send_batch_size: 100

  # 添加服务器标识标签
  resource:
    attributes:
      - key: server.id
        value: "${SERVER_ID}"
        action: upsert
      - key: server.name
        value: "${SERVER_NAME}"
        action: upsert
      - key: hostname
        value: "${HOSTNAME}"
        action: upsert

exporters:
  otlp:
    endpoint: "${OTLP_ENDPOINT}"  # 例如: http://hub.example.com:4317
    tls:
      insecure: true

  # 调试日志 (可选)
  logging:
    loglevel: info

service:
  pipelines:
    metrics:
      receivers: [hostmetrics]
      processors: [resource, batch]
      exporters: [otlp, logging]
```

**模板渲染逻辑**:
```java
public String renderOtelConfig(Server server, String hubEndpoint) {
    String template = readTemplate("otelcol.yaml.tmpl");

    Map<String, String> variables = Map.of(
        "SERVER_ID", server.getId().toString(),
        "SERVER_NAME", server.getName(),
        "HOSTNAME", server.getHostname(),
        "OTLP_ENDPOINT", hubEndpoint
    );

    String config = template;
    for (Map.Entry<String, String> entry : variables.entrySet()) {
        config = config.replace("${" + entry.getKey() + "}", entry.getValue());
    }

    return config;
}
```

### 3.4 Bootstrap脚本

**安装脚本** (`bootstrap.sh`):
```bash
#!/bin/bash
# OpenTelemetry Collector Agent安装脚本
# 由 InternalPaaS Platform 自动生成

set -e

INSTALL_DIR="/opt/metrics-agent"
SERVICE_NAME="otelcol"
CONFIG_FILE="$INSTALL_DIR/otelcol.yaml"
BINARY_FILE="$INSTALL_DIR/otelcol"

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "====================================="
echo "OpenTelemetry Collector Agent 安装"
echo "====================================="

# 1. 创建安装目录
echo -e "${GREEN}[1/5]${NC} 创建安装目录..."
sudo mkdir -p $INSTALL_DIR
sudo chown $(whoami):$(whoami) $INSTALL_DIR

# 2. 解压Agent文件（假设已上传到 /tmp/agent.tar.gz）
echo -e "${GREEN}[2/5]${NC} 解压Agent文件..."
if [ -f /tmp/agent.tar.gz ]; then
    tar -xzf /tmp/agent.tar.gz -C $INSTALL_DIR
    chmod +x $BINARY_FILE
else
    echo -e "${RED}错误: Agent文件不存在${NC}"
    exit 1
fi

# 3. 复制配置文件（假设已上传到 /tmp/otelcol.yaml）
echo -e "${GREEN}[3/5]${NC} 配置Agent..."
if [ -f /tmp/otelcol.yaml ]; then
    cp /tmp/otelcol.yaml $CONFIG_FILE
else
    echo -e "${RED}错误: 配置文件不存在${NC}"
    exit 1
fi

# 4. 创建systemd服务
echo -e "${GREEN}[4/5]${NC} 创建systemd服务..."
sudo tee /etc/systemd/system/$SERVICE_NAME.service > /dev/null <<EOF
[Unit]
Description=OpenTelemetry Collector Agent
After=network.target

[Service]
Type=simple
User=$(whoami)
ExecStart=$BINARY_FILE --config=$CONFIG_FILE
Restart=on-failure
RestartSec=5s
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

# 5. 启动服务
echo -e "${GREEN}[5/5]${NC} 启动Agent服务..."
sudo systemctl daemon-reload
sudo systemctl enable $SERVICE_NAME
sudo systemctl start $SERVICE_NAME

# 验证服务状态
sleep 2
if systemctl is-active --quiet $SERVICE_NAME; then
    echo -e "${GREEN}✅ Agent安装成功！${NC}"
    systemctl status $SERVICE_NAME --no-pager
    exit 0
else
    echo -e "${RED}❌ Agent启动失败${NC}"
    systemctl status $SERVICE_NAME --no-pager
    exit 1
fi
```

---

## 四、数据模型

### 4.1 DeploymentRecord (部署记录)

```java
@Entity
@Table(name = "agent_deployments")
public class AgentDeployment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "server_id")
    private Server server;

    private String agentVersion;  // Agent版本号

    @Enumerated(EnumType.STRING)
    private DeploymentStatus status;  // PENDING, IN_PROGRESS, SUCCESS, FAILED

    private Integer retryCount = 0;  // 重试次数

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Column(length = 2000)
    private String errorMessage;  // 错误信息

    @Column(length = 5000)
    private String deploymentLog;  // 部署日志

    // Getters and Setters
}

public enum DeploymentStatus {
    PENDING("待部署"),
    PRE_CHECK("预检中"),
    UPLOADING("上传中"),
    INSTALLING("安装中"),
    HEALTH_CHECK("健康检查中"),
    SUCCESS("部署成功"),
    FAILED("部署失败"),
    ROLLED_BACK("已回滚");

    private String description;

    DeploymentStatus(String description) {
        this.description = description;
    }
}
```

---

## 五、配置管理

### 5.1 Application配置

```properties
# ========================================
# Agent 自动部署配置
# ========================================

# 启用/禁用自动部署
agent.auto-deploy.enabled=true

# Agent版本
agent.version=0.91.0

# Agent二进制文件路径（相对于classpath）
agent.binary.path=agent/otelcol-linux-amd64.tar.gz

# 配置模板路径
agent.config.template=agent/otelcol.yaml.tmpl

# Bootstrap脚本路径
agent.bootstrap.script=agent/bootstrap.sh

# Hub OTLP端点
agent.otlp.endpoint=http://localhost:4317

# 部署超时（分钟）
agent.deploy.timeout-minutes=10

# 重试策略
agent.deploy.retry.max-attempts=3
agent.deploy.retry.delay-minutes=1,5,15

# 健康检查配置
agent.health-check.timeout-seconds=30
agent.health-check.interval-seconds=5

# 预检配置
agent.pre-check.min-disk-mb=100
agent.pre-check.required-ports=4317,4318
```

---

## 六、错误处理与监控

### 6.1 错误分类

| 错误类型 | 处理策略 | 通知 |
|---------|---------|------|
| **SSH连接失败** | 立即重试3次 → 标记失败 | WebSocket + 日志 |
| **权限不足** | 不重试，记录错误 | WebSocket + 告警 |
| **磁盘空间不足** | 不重试，记录错误 | WebSocket + 告警 |
| **上传失败** | 清理后重试 | WebSocket + 日志 |
| **安装脚本执行失败** | 回滚后重试 | WebSocket + 日志 |
| **健康检查失败** | 标记异常，人工介入 | WebSocket + 告警 |

### 6.2 监控指标

建议监控以下指标：
- Agent部署成功率
- 平均部署时长
- 重试次数分布
- 失败原因统计
- Agent在线率

---

## 七、安全考虑

1. **SSH密钥管理**: 使用已有的SSH密码加密机制
2. **文件权限**: 确保配置文件仅用户可读 (chmod 600)
3. **sudo权限**: Bootstrap脚本仅需要systemd管理权限
4. **网络隔离**: OTLP端点应使用内网地址或VPN
5. **日志脱敏**: 部署日志中不记录敏感信息

---

## 八、后续优化

1. **批量部署**: 支持一次部署多台服务器
2. **版本管理**: 支持Agent版本升级
3. **配置热更新**: 修改配置后自动推送到Agent
4. **监控告警**: Agent离线自动告警
5. **自动卸载**: 服务器删除时自动卸载Agent

---

## 九、实施计划

| 阶段 | 任务 | 预计耗时 |
|------|------|---------|
| 1 | 创建AgentDeployService基础框架 | 0.5天 |
| 2 | 实现预检查功能 | 0.5天 |
| 3 | 实现文件上传功能 | 0.5天 |
| 4 | 实现安装执行功能 | 0.5天 |
| 5 | 实现健康检查功能 | 0.5天 |
| 6 | 实现重试与回滚 | 0.5天 |
| 7 | 集成到Server创建流程 | 0.5天 |
| 8 | WebSocket进度推送 | 0.5天 |
| 9 | 测试与文档 | 1天 |
| **总计** | | **5天** |

---

**结论**: 架构设计完成，准备进入开发阶段。

