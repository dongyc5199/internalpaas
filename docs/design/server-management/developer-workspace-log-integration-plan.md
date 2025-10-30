# 开发者工作台日志/终端实时集成方案

**版本**: 0.1  
**日期**: 2025-10-27  
**作者**: AI Assistant  
**状态**: 草案（待评审）

---

## 1. 目标

- 在主应用中落地 `developer-workspace` 内容页，同时支持多实例服务的实时日志聚合与终端操作。
- 保障开发者能够在单页面内查看所有实例输出、执行命令，并具备可靠的断线重连、审计与扩缩容能力。
- 保持与现有“服务器群组”页面一致的集成方式（片段懒加载、独立静态资源、REST + 流式接口）。

---

## 2. 页面集成工作项

| 区域 | 待办事项 |
|------|----------|
| 模板 | 新增 `src/main/resources/templates/developer/developer-workspace-content.html`，定义 `th:fragment="developer-workspace-content"`。去除导航/标题，只保留内容区结构。 |
| 样式 | 将 demo 中的 `<style>` 拆分为 `static/css/developer-workspace.css`，统一前缀 `.developer-workspace` / `.workspace`，复用主站变量。 |
| 脚本 | 将交互逻辑抽成 `static/js/developer-workspace.js`，对外暴露 `DeveloperWorkspacePage.init(container)`。内部完成：服务下拉、指标卡刷新、日志/终端弹窗、运行洞察弹窗。 |
| 主布局 | 在 `main-layout.html` 导航映射中新增 `developer-workspace` 路由，点击后通过 `fetch('/developer/workspace/content')` 注入片段，并在完成后调用 `DeveloperWorkspacePage.init(...)`。 |
| i18n/导航 | 按照服务器群组页面方式设置 `data-i18n-en/zh` 属性，保证语言切换。 |
| 权限 | 仅对开发者角色展示菜单入口（参考角色控制逻辑）。 |

---

## 3. 后端接口规划

| 接口 | 说明 | 备注 |
|------|------|------|
| `GET /developer/workspace/content` | 返回上文 fragment | 供主应用加载 |
| `GET /developer/workspace/services` | 服务列表 + 环境信息 +默认服务 | 首屏请求 |
| `GET /developer/workspace/summary` | CPU/内存/硬盘/告警数据 | 支持多实例聚合 |
| `GET /developer/workspace/insights` | 配置文件、部署历史、审计摘录 | 弹窗内容 |
| `GET /developer/workspace/logs/history` | 最近 5~10 分钟日志快照 | 首屏填充 |
| `GET /developer/workspace/logs/stream` | SSE 输出实时日志 | 含实例标识、traceId、级别等字段 |
| `POST /developer/workspace/terminal/session` | 创建/恢复终端会话 | 返回 WebSocket 连接信息或 sessionId |
| `WS /developer/workspace/terminal/{sessionId}` | WebSocket 管道传输命令与回显 | 支持命令白名单、审计 |

---

## 4. 日志聚合方案（多实例）

1. **日志采集**  
   - 应用实例采用 Sidecar/Agent（Filebeat、Promtail、Fluent Bit）推送到集中式流式存储 Kafka 或 Redis Stream。
   - 按 `service + env + instance` 分区，保证顺序。

2. **聚合服务（workspace-log-aggregator）**  
   - 消费日志流，根据过滤条件（级别、关键字、traceId、实例）实时合并排序。
   - 暴露 SSE 接口 `/logs/stream`，支持 `Last-Event-ID`，断线后按 offset 续传。
   - 提供 `/logs/history` 查询近期窗口，初次加载或刷新时使用。
   - 具备背压控制、限速、告警。当客户端消费缓慢时返回节流提示。

3. **前端策略**  
   - 页面加载时调用 `/logs/history` → 渲染首屏，并标出实例来源。
   - 建立 SSE 连接追加新日志；UI 支持按照实例过滤/高亮。
   - 当 SSE 断线时，自动重试并带上 `Last-Event-ID`；回到页面时校同步步 offset。

4. **扩展**  
   - 聚合服务可水平扩展：按服务分区订阅 Kafka topic；部署在独立模块，利于后续复用。
   - 长周期日志仍由现有日志平台（ELK、Loki 等）负责；本聚合层专注实时 tail。

---

## 5. 终端实时方案

- **通道**：WebSocket（或接入 ttyd/Wetty 等终端代理）。  
- **工作流**：  
  1. 前端调用 `POST /terminal/session` 申请会话，后端创建 PTY、记录审计上下文。  
  2. 前端建立 `WS /terminal/{sessionId}`，发送命令；服务端按行回显。  
  3. 命令与输出可同步写入 Kafka/Redis，用于审计回放。  
- **安全与审计**：白名单/黑名单、超时断连、命令日志持久化。  
- **备选**：若需要 HTTP/2，考虑 gRPC-Web；若网络条件复杂，可引入 WebRTC + 数据通道，但成本较高。

---

## 6. 运行洞察弹窗

- 页面顶部提供“运行洞察”按钮。  
- 前端请求 `/insights` 后填充模板（配置文件、最近部署、审计摘录）。  
- 支持分页/更多链接跳转到现有页面（部署历史、配置管理等）。  
- 保留模版 `insightsTemplate` 以实现懒加载和多语言内容。

---

## 7. 测试与验收

| 领域 | 重点 |
|------|------|
| UI / 交互 | 导航切换、服务下拉、指标卡刷新、运行洞察、日志弹窗、终端弹窗 |
| 数据流 | SSE 重连、日志过滤、实例标识展示、终端命令回显 |
| 性能 | 日志聚合吞吐、SSE 客户端数量、终端并发会话（默认单用户 3 个） |
| 安全 | 命令审计、权限校验、日志脱敏、接口限流 |
| 回归 | 验证现有服务器群组等页面未受影响；`./mvnw.cmd clean verify` 通过 |

---

## 8. 开放问题

1. Kafka / Redis Stream 集群是否已有？若无，需要先落地日志中转基础设施。  
2. 聚合服务由哪个团队维护？需要 DevOps/平台协作。  
3. 终端代理是否直接与现有 SSH 基础对接，还是使用容器内 PTY？安全策略需提前评审。  
4. 日志、终端接口是否对外暴露？需要规划网关与权限。  
5. SSE 客户端断线后是否需要重新获取历史数据以补齐缺失？需要确定 offset 策略。

---

## 9. 多实例部署方案

### 9.1 架构拓扑

```
开发者浏览器
    │
    ├─HTTP(S) 请求 → 主应用 (Spring Boot, main-layout)
    │                   └─ 调用 DeveloperWorkspacePage（静态资源）
    │
    ├─SSE / WebSocket → Workspace Log Aggregator (多实例, K8s Deployment)
    │                       │
    │                       ├─ Kafka/Redis Stream（日志实时流，多分区）
    │                       └─ Audit/Terminal 消息队列
    │
    └─REST → 后端业务 API（服务列表、指标、洞察等）
```

### 9.2 组件部署策略

| 组件 | 部署方式 | 伸缩策略 | 高可用要点 |
|------|----------|----------|------------|
| 主应用 (UI) | 现有集群（k8s deployment），新增 `developer-workspace` 静态资源 | 与现有服务一致 | 无 |
| Workspace Log Aggregator | 新建 `Deployment`，初始 2 实例，横向拆分 Topic 分区 | HPA 基于消费延迟与 CPU 使用率 | 通过共享消费者组实现 failover |
| Kafka / Redis Stream | 复用现有流平台或专用集群 | 分区数≥服务实例数，允许动态扩展 | 开启持久化、跨可用区部署 |
| 终端代理 (WebSocket) | 使用内置实现或引入 `ttyd/wetty` Pod，前置 Nginx/Ingress | 会话数量作为伸缩指标 | Session 由 Redis 缓存持久化，支持粘性会话 |
| 日志采集 Sidecar | 每个业务 Pod 内置 Filebeat/Fluent Bit | 随业务扩缩容自动随增 | 统一配置管理 |

### 9.3 部署流程

1. **准备基础设施**  
   - 若无 Kafka/Redis Stream，优先上线（3 节点起步，开启跨 AZ 副本）。  
   - 选定终端实现（原生 WebSocket + PTY 或 ttyd/wetty），配好鉴权、审计。

2. **部署聚合服务**  
   - `workspace-log-aggregator` 使用 Spring Boot + Kafka client 或 Redis Stream client。  
   - 配置多消费者组，按 `serviceId` 分片；每个实例订阅一组 partition。  
   - 暴露 `/logs/history`、`/logs/stream`，对接主应用。  
   - 接入 Prometheus 指标（消费延迟、SSE 会话数、重连次数）。

3. **更新日志采集配置**  
   - 在服务部署模板里增加 Sidecar，输出格式统一（JSON 包含 service/env/instance/level/timestamp/message/traceId）。  
   - Kafka Topic 命名建议：`workspace.logs.{env}.{service}`，配好 ACL。

4. **主应用部署**  
   - 合并静态资源后发版；上线前配置新路由与 API 代理。  
   - 灰度：先在测试环境验证 SSE/WebSocket 行为，再逐步扩展到生产。

5. **扩容与容灾**  
   - 当日志量增大或实例增多时，扩容 Kafka partition & aggregator pod 数量。  
   - SSE 终端断线自动迁移：消费者使用 `sticky session` + `offset` 续传。  
   - 聚合服务故障时，主应用 fallback 至历史 API（显示“实时流暂不可用”）。

### 9.4 运维要求

- **监控**：Kafka Lag、Aggregator CPU/连接数、SSE 客户端数、终端会话数。  
- **告警**：消费滞后 > 30s、聚合服务不可用、WebSocket 会话大规模断开。  
- **日志留存**：实时流保留 10 分钟，历史日志依赖 ELK/集中日志系统。  
- **安全**：Aggregator 与终端服务均需开启认证（JWT/Session），防止未授权接入。

---

## 10. 多实例应用部署支持方案

### 10.1 用户目标
- 开发者可在平台中为同一应用管理多个实例（跨环境/节点）。  
- 支持一次部署覆盖多实例，提供回滚与灰度能力。  
- 在“开发者工作台”内直接触发扩缩容、实例重启、版本回滚。

### 10.2 架构设计

| 组件 | 职责 | 说明 |
|------|------|------|
| 发布编排服务 (Release Orchestrator) | 接收 UI 发起的部署请求，封装策略，驱动后端部署引擎 | 可封装在平台后端，调用 Kubernetes / 自研调度器 |
| 模板中心 (Service Blueprint) | 存储应用部署描述（镜像/制品、Env、副本数、策略） | 支持版本化，开发者在 UI 编辑 |
| 部署引擎 | 实际执行多实例部署（如 Kubernetes Deployment、Nomad、Ansible） | 支持滚动/蓝绿/金丝雀 |
| 状态同步 | 汇总实例状态、事件、指标，回传工作台 | 与日志聚合共享通道 |

### 10.3 流程（UI → 后端）

1. **准备模板**  
   - 在“配置管理”或独立页面维护 `service.yaml`（定义镜像、资源、健康检查、初始副本数、环境变量）。  
   - 模板可关联多环境变量（DEV/STG/PROD）的差异化配置。

2. **触发部署**  
   - 开发者在工作台点击“重新部署”，选择：
     - 制品版本（容器镜像 tag、制品文件）
     - 目标实例/环境
     - 部署策略（滚动、蓝绿、分批）与批量大小
     - 并行度 & 健康阈值
   - 前端调用 `POST /developer/workspace/deployments`，提供上述参数。

3. **编排服务处理**  
   - 验证配置完整性 → 生成部署计划 → 调用部署引擎 API（例如 Kubernetes Deployment Patch、Argo Rollout、内部发布系统）。  
   - 为每次部署生成 ID，写入审计日志。

4. **执行与回调**  
   - 部署引擎按策略依次处理实例：  
     - 滚动更新：依次替换 Pod，健康检查通过后继续。  
     - 蓝绿：新环境拉起成功后切换流量。  
     - 金丝雀：部署部分实例，观测指标后继续。  
   - 过程中持续向平台回报：实例状态、事件、失败原因。

5. **结果呈现**  
   - 工作台部署面板实时展示进度条、每批次实例状态。  
   - 若失败，提供“回滚至上一版本”按钮，调用 `POST /developer/workspace/deployments/{id}/rollback`。

### 10.4 扩缩容与实例管理

- 扩容：UI 输入目标实例数 → `PATCH /developer/workspace/services/{id}/scale` → 调用部署引擎更新副本数。  
- 缩容：同上，但需确认最小副本与业务权重。  
- 单实例重启/进入维护：调用后端触发滚动重启/Drain 节点，记录审计。  
- 支持标记实例标签（az、节点类型），部署时可按标签分批。

### 10.5 状态与回滚保障

- 所有部署操作写入审计表（操作人、输入参数、实例列表、结果）。  
- 引擎需具备自动回滚策略：当失败率超过阈值时暂停并回退。  
- 保留最近 N 个部署版本（镜像 tag + 配置快照），用于一键回滚。

### 10.6 依赖清单

- 制品存储：镜像仓库、制品库（Nexus、Harbor）。  
- 调度执行：Kubernetes API / 内部部署系统。  
- 指标支撑：Prometheus / Kubernetes Metrics，供 UI 显示扩容建议。  
- 权限：开发者需被授权操作特定服务；审批流程（可选）在发起部署前执行。

### 10.7 开放问题

1. 当前平台使用的部署引擎？若无，需要选型（K8s 优先）。  
2. 配置差异管理方式：YAML 模板 + 参数化还是集中配置中心？  
3. 蓝绿/金丝雀所需的流量切换能力是否已有（Ingress、Service Mesh）？  
4. 审批流程需求（是否要接入现有审批系统）。  
5. 指标与告警来源是否充足，能否支持“观测窗口”自动判定。

### 10.8 角色分工与入口

- **管理员控制台**  
  - 集成部署编排 UI（建议在 `/admin/app-service-management` 下新增“部署编排”页）。  
  - 提供模板管理、部署请求发起、批量扩缩容、历史记录与审批。  
  - 仅管理员可访问；可参考“服务器群组”页面的路由加载方式。
- **开发者工作台**  
  - 只展示部署状态、运行指标、实时日志、终端能力；可以触发“重新部署”但实际调用管理员编排服务（若需审批则走管理员流程）。  
  - 无法直接编辑模板或修改多实例策略，以保证权限边界。
- **审批链路**  
  - 管理员可以配置审批节点（可选），开发者提交的部署请求在管理员控制台审核后执行。  
  - 审批结果、执行状态通过审计事件反馈到开发者工作台。

---

**下一步**  
1. 审核本方案，确认日志聚合与终端实现路线。  
2. 搭建最小可行版本：Mock API + SSE stub + WebSocket echo。  
3. 完成页面模块化、静态资源拆分与路由集成。  
4. 与后端/运维对齐中间件部署计划（Kafka/Redis Stream、终端代理）。  
