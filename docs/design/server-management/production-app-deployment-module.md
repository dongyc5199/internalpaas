# 生产环境应用部署模块设计

**版本**: 0.1  
**日期**: 2025-10-27  
**作者**: AI Assistant  
**状态**: 设计草案（管理员控制台）

---

## 1. 背景与目标

- 服务对象：企业/政府内网环境管理员，需在平台中统一调度应用的生产部署。
- 支持两类应用形态：
  1. **多实例部署**（容器化微服务，Kubernetes/容器平台运行）。
  2. **单包部署**（单机/集群上的可执行 JAR、war、或自解压包）。
- 提供集中化的模板管理、部署执行、审批与审计能力，保证操作可追溯、可回滚。
- 管理员控制台新增“生产应用部署”模块，采用与“服务器群组”类似的路由/懒加载方式。

---

## 2. 模块结构

| 区域 | 功能点 |
|------|--------|
| **导航入口** | `/admin/app-deployment`，管理员可见。 |
| **列表页** | 展示所有受管应用（服务名、部署形态、环境、当前版本、实例数、告警状态）。 |
| **模板管理** | 管理部署 Blueprint：镜像/制品地址、部署策略、环境变量、审批规则。 |
| **部署历史** | 查看每次部署的状态、操作人、日志、变更对比。 |
| **操作面板** | 支持触发部署、扩缩容、回滚；附带审批与二次确认。 |
| **审计 & 通知** | 所有动作写入审计库，可推送到内部通知系统。 |

---

## 3. 系统架构概览

```
管理员 UI (admin/app-deployment)
      │        ├─ 模板配置 API
      │        ├─ 部署请求 API
      │        ├─ 历史/审计 API
      ▼
App Deployment Service (新模块)
      │        ├─ 读取模板中心 (DB/Config Repo)
      │        ├─ 调用 Approval Service
      │        ├─ 调用 Orchestrator Adapter
      ▼
Deployment Orchestrator Adapter
 ├─ 容器部署 (K8s / Argo Rollouts / 自研平台)
 └─ 单包部署 (Ansible / SSH Executor / Windows Service)
```

- **App Deployment Service**：Spring Boot 模块，提供 API、模板管理、状态聚合。
- **模板中心**：数据库表或 Git 仓库；记录 Blueprint、版本、参数（见 §4）。
- **Orchestrator Adapter**：统一封装多种执行方式，可按部署类型选择实现。

---

## 4. 模板模型

```yaml
apiVersion: v1
kind: DeploymentBlueprint
metadata:
  serviceId: dev-portal-service
  name: Developer Portal
  type: container | jar
spec:
  environments:
    - name: DEV
      cluster: k8s-dev
      approvalRequired: false
    - name: PROD
      cluster: k8s-prod
      approvalRequired: true
  artifact:
    container:
      image: harbor.local/dev/dev-portal:${VERSION}
      replicas: 4
      rolloutStrategy: rollingUpdate | blueGreen | canary
      resources:
        cpu: "500m"
        memory: "1Gi"
      readinessProbe: httpGet:/actuator/health
    jar:
      packageUrl: nfs://repo/app/dev-portal-${VERSION}.tar.gz
      runUser: appuser
      startupCmd: java -jar dev-portal.jar --spring.profiles.active=${ENV}
      targetHosts: [app-node-01, app-node-02]
      restartPolicy: systemd
  configs:
    - name: application.yml
      source: config-center://dev-portal/${ENV}
  approvalFlow:
    - role: platform-ops
    - role: security
```

- `type=container` 走容器化流程；`type=jar` 走单包部署流程。
- `artifacts` 部分可按类型选择性的填写。
- `configs` 指向配置中心或外部文件源。

---

## 5. 流程设计

### 5.1 多实例（容器化）部署

1. **管理员发起部署**：选择 Blueprint、目标环境、版本号、策略参数（批量大小、暂停阈值）。
2. **审批（可选）**：若 `approvalRequired=true`，进入审批队列，审批后继续。
3. **执行**：
   - 调用 Kubernetes API（Deployment/StatefulSet 或 Argo Rollouts）更新镜像 tag 或 ReplicaSet。
   - 支持 `rollingUpdate`、`blueGreen`、`canary`：
     - 蓝绿：新 Deployment 通过健康检查后，更新 Service 指向。
     - 金丝雀：阶段性扩大副本，监控指标。
4. **状态同步**：监控 pod 状态、事件、Prometheus 指标，实时回推 UI。
5. **完成 / 回滚**：部署成功写入历史；失败时可手动或自动回滚至上一版本。

### 5.2 单 JAR 包部署

1. **主机注册**：在平台中登记可操作主机（SSH 访问、系统类型、标签）。
2. **管理员发起部署**：选择 Blueprint、目标主机标签、制品版本。
3. **执行**：
   - 通过 Ansible/SSH 执行：下载制品 → 校验 → 备份旧版本 → 启动新进程（systemd/自定义脚本）。
   - 支持逐台发布（按批次），失败时回滚旧版本。
4. **状态反馈**：采集进程状态、端口、日志关键字，展示在 UI。
5. **回滚**：保留 N 个备份（本地/共享存储），一键切换。

### 5.3 扩缩容

- **容器化**：直接调用 K8s API 更新 `replicas` 信息；支持标签或 node selector。
- **单包**：对主机列表增删，或动态加入新主机并执行部署脚本。

---

## 6. 前端页面设计要点

- **列表视图**：支持按类型、环境筛选；显示当前部署状态（成功/失败/进行中）、最新版本。  
- **详情面板**：展示实例列表、历史部署、配置对比、指标概览。  
- **操作面板**：
  - “发起部署”弹窗：选择版本、策略、批次、回滚开关。
  - “扩缩容/回滚”按钮带二次确认与风险提示。
- **审批/审计**：在页面侧栏展示待审批部署，管理员可直接处理。  
- **权限**：仅 `ROLE_ADMIN` 可见；可配置细粒度授权（按服务）。

### 6.1 页面流程

1. **导航进入** → `/admin/app-deployment` → 通过 AJAX 加载 fragment。  
2. **选择应用** → 左侧列表 / 搜索定位服务，主视图刷新概要信息。  
3. **查看详情** → 点击卡片进入详情抽屉，包含实例表、部署时间线、配置对比。  
4. **触发操作** → `部署 / 扩容 / 回滚 / 编辑模板` 均采用分步弹窗，完成后刷新数据。  
5. **审批处理** → 顶部“待处理审批”Badge 显示数量，点击展开审批列表。  
6. **历史查看** → 列表页右上角进入历史页，可按时间/状态筛选，支持导出。

### 6.2 发起部署向导

| 步骤 | 描述 |
|------|------|
| Step 1 - 选择版本 | 下拉选择镜像 tag 或上传制品（单包）；展示最近 5 次部署记录。 |
| Step 2 - 策略设置 | 选择部署策略、批次大小、暂停阈值、是否启用自动回滚。 |
| Step 3 - 环境与实例 | 勾选目标环境/集群；容器化可选节点标签，单包可选主机标签。 |
| Step 4 - 审核与确认 | 展示变更摘要，提醒审批链和影响范围；点击“提交部署”。 |

向导内实时校验：若模板缺少必填项或当前用户无审批权限，则阻止提交。

### 6.3 模板编辑器

- 表单模式（基础字段）+ YAML / JSON 编辑模式。  
- 支持版本比较（展示上一版本差异），保存后新版本立即生效，但部署时可选择版本。  
- 模板变更写入版本表并同步到 Git（可选）。

---

## 7. 后端接口草案

| 接口 | 方法 | 说明 |
|------|------|------|
| `/admin/app-deployment/content` | GET | 返回页面 fragment |
| `/admin/app-deployment/apps` | GET | 列出受管应用列表 |
| `/admin/app-deployment/apps/{id}` | GET | 应用详情、实例状态 |
| `/admin/app-deployment/blueprints` | GET/POST/PUT/DELETE | 模板 CRUD |
| `/admin/app-deployment/deployments` | POST | 发起部署 |
| `/admin/app-deployment/deployments/{id}` | GET | 部署状态、日志 |
| `/admin/app-deployment/deployments/{id}/rollback` | POST | 回滚 |
| `/admin/app-deployment/apps/{id}/scale` | PATCH | 扩缩容 |
| `/admin/app-deployment/approvals` | GET/POST | 审批列表/处理 |
| `/admin/app-deployment/history` | GET | 历史记录 |

### 7.1 请求示例

**发起容器部署**
```json
POST /admin/app-deployment/deployments
{
  "serviceId": "dev-portal-service",
  "blueprintVersion": "2025.10.27-01",
  "environment": "PROD",
  "type": "container",
  "artifact": {
    "imageTag": "v2.3.1",
    "rolloutStrategy": "blueGreen",
    "batchSize": 2,
    "pauseConditions": {
      "errorThreshold": 1,
      "metrics": ["latency_p95>300", "error_rate>1"]
    }
  },
  "configOverrides": {
    "SPRING_PROFILES_ACTIVE": "prod"
  },
  "approvalToken": null,
  "autoRollback": true,
  "notes": "版本 2.3.1 发布 - 启用蓝绿"
}
```

**发起单包部署**
```json
POST /admin/app-deployment/deployments
{
  "serviceId": "batch-report-job",
  "blueprintVersion": "2025.10.20-02",
  "environment": "PROD",
  "type": "jar",
  "artifact": {
    "packageUrl": "nfs://repo/batch-report-job-20251027.tar.gz",
    "targetHosts": ["report-01", "report-02"],
    "batchSize": 1,
    "preDeployScript": "scripts/check_disk.sh",
    "postDeployScript": "scripts/verify_service.sh"
  },
  "autoRollback": false,
  "notes": "定时报表程序更新"
}
```

### 7.2 响应结构

```json
{
  "deploymentId": "dpl_20251027_00123",
  "status": "PENDING_APPROVAL",
  "submittedAt": "2025-10-27T18:00:00+08:00",
  "nextActions": [
    {"type": "APPROVAL", "assignee": "platform-ops"}
  ]
}
```

---

## 8. 数据与审计

- **数据库表**（建议新增）：
  - `app_blueprint`、`app_blueprint_version`
  - `app_deployment_task`（任务状态、策略、审批信息）
  - `app_deployment_instance`（任务实例、主机/Pod、结果）
  - `app_deployment_audit`（操作日志）

- **审计字段**：操作人、操作类型、目标服务、版本、实例列表、审批链、结果、时间。

### 8.1 数据模型概述

| 表 | 主键 | 关键字段 | 说明 |
|----|------|----------|------|
| `app_blueprint` | `id` | `service_id`, `type`, `latest_version` | 当前服务模板定义。 |
| `app_blueprint_version` | `id` | `blueprint_id`, `version`, `payload (JSON)`, `created_by` | 模板版本存档。 |
| `app_deployment_task` | `id` | `service_id`, `blueprint_version`, `status`, `strategy`, `submitted_by`, `auto_rollback` | 部署任务主记录。 |
| `app_deployment_instance` | `(task_id, instance_id)` | `target`, `phase`, `start_time`, `end_time`, `output_log` | 单实例执行详情。 |
| `app_deployment_approval` | `id` | `task_id`, `sequence`, `approver_role`, `status`, `comment` | 审批链。 |
| `app_deployment_audit` | `id` | `task_id`, `action`, `operator`, `timestamp`, `payload` | 操作审计。 |

字段推荐 JSON 存储：部署策略、指标阈值、脚本参数等。

---

## 9. 运维与安全要求

- 全链路遵循内网安全策略：所有节点在内网通信，使用企业证书/私网域名。  
- 制品/镜像仓库需可离线访问，提供镜像同步方案。  
- 支持双人审批、二次确认，符合企业/政府内控要求。  
- 终端日志、部署脚本输出需存档，满足合规审计。  
- 提供 Prometheus 指标：任务成功率、平均耗时、正在执行的部署数。  
- 支持与告警系统（短信、邮件、内网 IM）对接。  
- 平台自身升级需支持离线包安装。

---

## 10. 开发计划建议

1. **迭代 1**：页面集成（静态数据 + 模板 CRUD）、REST 接口基础框架。  
2. **迭代 2**：容器化部署流程（滚动更新）、审批 & 审计完善。  
3. **迭代 3**：单包部署实现、批量回滚、扩缩容。  
4. **迭代 4**：蓝绿/金丝雀策略、指标接入、离线安装支持。  
5. **迭代 5**：终端/日志联动、自动化测试、性能调优。

---

## 11. 未来扩展

- 与 CI/CD 系统集成，实现自动触发部署。  
- 支持多租户隔离（不同业务线不同管理员权限）。  
- 引入发布窗口配置、维护期提醒。  
- 与成本分析结合，提供实例成本估算与优化建议。

---

## 12. 执行流程序列（概要）

### 12.1 容器化部署（蓝绿）

1. 管理员提交部署请求 → `App Deployment Service` 创建任务（状态 `PENDING_APPROVAL`）。  
2. 审批通过后任务进入 `SCHEDULED` → 调用 `Orchestrator Adapter`。  
3. Adapter 创建新 `Deployment`（绿色环境）并等待健康检查。  
4. 健康通过 → 更新 Service 指向新版本 → 标记旧版本为 `IDLE`。  
5. 监控窗口结束 → 任务状态 `SUCCEEDED`；若失败则回滚并置为 `ROLLED_BACK`。

### 12.2 单包部署

1. Adapter 通过任务参数生成 Ansible Playbook（或内部脚本），按批次执行。  
2. 每台主机执行：停止旧进程 → 备份 → 部署新包 → 启动 → 健康检测。  
3. 若某批失败 → 根据策略暂停并等待人工处理或自动回滚。  
4. 所有主机完成后任务标记 `SUCCEEDED`，生成回滚包索引。

任务状态机建议：`PENDING_APPROVAL` → `SCHEDULED` → `RUNNING` → `SUCCEEDED` / `FAILED` / `ROLLED_BACK` / `CANCELED`。
