# 管理员工作台重新设计实施计划

**文档版本**: v1.1
**创建时间**: 2025-01-27
**负责人**: Claude AI
**项目**: Dev Debug Platform 管理员控制台

> 本修订版将管理员工作区概览限定为三大核心内容：统计概览、服务器监控面板、用户监控面板。此前规划的分析面板、系统工具、快捷操作等拓展模块已被裁剪。

## 📋 项目概述

### 设计目标
- 构建清晰的管理员工作区概览，突出平台运行态势的 key 指标。
- 通过实时化的服务器监控面板，帮助管理员掌握节点负载与健康状况。
- 打造用户监控面板，洞察用户登录、访问与关键行为。
- 保持内容页与外层框架解耦，使统计区域具备独立滚动与自适应能力。

### 核心需求
- **统计概览**：聚合展示全局关键指标（服务器总数、健康度、今日告警、活跃用户等）。
- **服务器监控面板**：提供节点图块/列表切换、核心性能指标、快速跳转行为。
- **用户监控面板**：呈现近况概览、活跃趋势、关键用户事件流。

## 🎯 设计方案

### 1. 整体布局结构

```
┌───────────────────────────────────────────────┐
│ 顶部导航栏（与框架保持一致）                  │
├───────────────────────────────────────────────┤
│ 统计概览（横向卡片）                          │
├───────────────────────────────────────────────┤
│ 服务器监控面板（左） │ 用户监控面板（右）       │
│            可自适应列宽 / 垂直滚动              │
└───────────────────────────────────────────────┘
```

- 统计概览占据顶部一整行，展示 3-5 个核心卡片。
- 中间区域采用两列布局：左侧为服务器监控，右侧为用户监控，支持在响应式场景下降级为上下堆叠。
- 所有内容区只在 `main` 内滚动，标题栏与侧边栏保持固定。

### 2. 核心功能模块

#### 2.1 统计概览
- **服务器指标**：总数、在线/离线/告警数量，最新健康评分。
- **告警摘要**：今日新增告警、待处理告警数量。
- **用户活跃**：今日活跃用户、趋势箭头、登录成功率。
- **可选 KPI**：任务队列长度、平均响应时间等，可通过配置开关控制。

#### 2.2 服务器监控面板
- **视图切换**：网格视图与列表视图，通过按钮或快捷键切换。
- **节点卡片**：显示 CPU/内存/磁盘占用、在线状态、所属机房。
- **交互入口**：查看详情、远程终端、刷新数据；保持组件 ID 与现有 JS 逻辑兼容。
- **滚动载入**：支持分页或懒加载，避免一次性渲染过多节点。

#### 2.3 用户监控面板
- **概览卡片**：今日登录数、失败率、关键角色占比。
- **活跃趋势图**：最近 7/30 天的登录趋势（折线或柱状图）。
- **事件流**：展示最新的用户关键操作（登录、敏感资源访问、异常行为）。
- **筛选条件**：按时间范围、角色、区域过滤；默认提供“过去24小时/7天/30天”。

## 🛠️ 技术实现方案

### 3.1 后端架构调整

- **Controller**：扩展 `AdminDashboardController`，提供以下端点：
  - `GET /admin/dashboard/overview` → `AdminDashboardDto`（仅填充统计概览字段）。
  - `GET /admin/dashboard/servers` → `ServerDashboardDto`（含节点列表 + 聚合数据）。
  - `GET /admin/dashboard/users` → `UserDashboardDto`（含趋势 + 事件流）。
- **Service**：复用 `DashboardService`/`MonitoringService`/`UserActivityService`，新增组装逻辑：
  - `DashboardService#getOverviewMetrics()` → 统计概览数据。
  - `DashboardService#getServerPanel()` → 服务器面板数据结构。
  - `DashboardService#getUserPanel()` → 用户面板数据结构。
- **DTO 精简**：
  - `AdminDashboardDto`：保留 `serverStats`、`alertStats`、`userStats` 字段；移除与分析面板相关的属性。
  - `ServerDashboardDto`：提供 `viewMode`、`cards`（节点列表）、`summary`（聚合指标）。
  - `UserDashboardDto`：提供 `activeSummary`、`trend`、`events`；支持分页 token。

### 3.2 API 设计规范

- 基础路径仍为 `/admin`，返回 JSON。
- 核心接口：

```java
@RestController
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminDashboardController {

    @GetMapping("/overview")
    public ResponseEntity<AdminDashboardDto> getOverview();

    @GetMapping("/servers")
    public ResponseEntity<ServerDashboardDto> getServerPanel();

    @GetMapping("/users")
    public ResponseEntity<UserDashboardDto> getUserPanel();
}
```

- 对接前端时保留原有 DOM ID，以便现有脚本继续工作。
- 考虑到用户事件可能较多，`/users` 支持 `?range=7d&pageToken=xxx`。

### 3.3 前端实现方案

- 页面骨架遵循内容框架规范：`stat-cards` + `main-content-section(two-column)`。
- JavaScript 入口函数 `initAdminDashboard()` 调整：
  - `loadDashboardOverview()`：填充统计卡片。
  - `loadServerPanel()`：渲染节点卡片，处理视图切换。
  - `loadUserPanel()`：渲染用户趋势与事件流。
  - 删除分析面板、系统工具相关的初始化。
- 主要 DOM 结构：
  - `#dashboardOverviewCards` → 统计概览容器。
  - `#serverStatusContainer`、`#serverViewToggle`。
  - `#userTrendChart`、`#userEventStream`。
- 样式：复用 `admin-dashboard.css`，去除未使用的 `.analysis-card`、`.system-tools` 等样式块。

### 3.4 数据刷新策略

- 统计概览：60 秒刷新一次，可手动刷新。
- 服务器面板：30 秒刷新节点状态，支持手动刷新按钮 `#refreshServers`。
- 用户面板：默认 5 分钟刷新，切换时间范围时立即重新请求。
- 使用现有缓存策略，避免对监控服务造成压力。

## 🧪 测试计划

### 后端
- `AdminDashboardControllerTest`：三条主要用例（overview/servers/users）。
- 验证权限、空数据场景、分页参数。
- `DashboardServiceTest`：确保剔除的字段不再返回。

### 前端
- 单元测试：Mock `fetch` 结果，断言三大区域渲染成功。
- 交互测试：
  - 视图切换（网格/列表）。
  - 时间范围筛选。
  - 手动刷新按钮。
- 视觉回归（可选）：确保裁剪组件后布局无错位。

## 📋 验收标准

| 验收ID | 类型 | 标准 | 状态 |
|--------|------|------|------|
| A1 | 功能 | 统计概览正确展示核心 KPI，刷新后数值更新 | ⏳ |
| A2 | 功能 | 服务器监控面板支持视图切换、节点详情入口 | ⏳ |
| A3 | 功能 | 用户监控面板展示趋势与事件流，支持筛选 | ⏳ |
| A4 | 功能 | 页面响应式：≤1280px 时自动堆叠列布局 | ⏳ |
| A5 | 性能 | 三个接口响应时间 < 200ms（缓存命中场景） | ⏳ |
| A6 | 安全 | 仅 ADMIN/SUPER_ADMIN 可访问内容页 | ⏳ |

## 🔄 维护与扩展

- **监控指标**：仅保留与三大模块直接相关的指标上报。
- **扩展建议**：
  - 统计概览可通过配置新增卡片，但默认不显示其他模块。
  - 用户面板未来可接入行为评分或风险等级。
  - 若需恢复系统工具/快捷操作，需另起子页面或弹窗。

## ✅ 任务进度概览

| 任务 | 状态 | 说明 |
|------|------|------|
| T1 概览指标梳理 | ✅ | 明确保留字段，删除分析相关字段 |
| T2 服务器面板裁剪 | ✅ | 移除工具卡片，只保留节点视图 |
| T3 用户面板新增 | ⏳ | 构建用户趋势与事件流输出 |
| T4 前端结构重构 | ⏳ | 调整 DOM 结构与样式，删除多余板块 |
| T5 接口与缓存策略 | ⏳ | 对接 `/overview` `/servers` `/users` |
| T6 测试回归 | ⏳ | 更新测试用例，覆盖三大模块 |

---

**文档状态**: 进行中（聚焦三大核心模块）

