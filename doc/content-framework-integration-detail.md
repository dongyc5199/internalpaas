## 2.4 内容框架（`main.content-framework`）

### 2.4.1 语义与职责
- 语义：承载主要业务内容，保证与侧边栏、标题栏的视觉与交互一致性。
- 职责：隔离滚动容器；统一面包屑、页头、统计区、主内容区的组织方式；提供可扩展的模块插槽。

### 2.4.2 结构树
```
main.content-framework
└─ div.content-scrollable
   ├─ nav.breadcrumb
   ├─ div.page-header
   ├─ section.stats-overview
   ├─ section.main-content-section (内容工具栏 + 主面板)
   ├─ section.main-content-section (分析面板演示)
   ├─ section.main-content-section (系统工具)
   └─ div.footer-hint
```

### 2.4.3 样式与布局约束
- `content-framework`：背景 `--content-bg`、垂直布局、隐藏自身滚动。
- `content-scrollable`：`overflow-y: auto`，内边距 `24px 32px`，通过 `gap: 24px` 控制段落间距。
- 响应式：移动端（`max-width: 768px`）时调整为窄边距（建议 16px），并允许内容块自适应高度。
- 滚动条：可根据主题定制 `::-webkit-scrollbar`，保持与系统一致的滚动体验。

### 2.4.4 面包屑与页头
- `nav.breadcrumb`：
  - 数据结构：侧边导航当前选中项作为一级菜单节点，内容页内部跳转页作为二级菜单节点，可按需扩展更多层级。
  - 结构：多个 `div.breadcrumb-item`，中间 `span.breadcrumb-separator`。
  - 交互：首项可跳转至控制台主页，当前项禁用。
- `div.page-header`：
  - 左侧 `page-title-group` 包含图标（`div.page-icon`）与标题、描述。
  - 右侧 `page-actions`：主按钮 `btn btn-primary`、辅按钮 `btn btn-secondary`。按钮组可根据权限动态渲染。

### 2.4.5 状态管理
- 内容区滚动与侧边栏折叠互不影响。
- 通过事件总线或状态管理器（如 Vuex、Redux 等）向下游组件广播筛选条件（时间范围、搜索关键词）。
- 需提供 Loading/Empty/Error 三态占位模板，供 stats 或主内容区复用。

---

## 2.5 统计概览区（`section.stats-overview`）

### 2.5.1 设计目标
- 快速传达系统运行的核心指标。
- 通过卡片式布局提供扩展详情入口。
- 兼顾数据对比（历史 vs 当前）与告警状态。

### 2.5.2 布局与结构
- 容器：`display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 20px;`
- 卡片结构：
  ```
  div.stats-card
  ├─ div.card-header
  │  ├─ div.card-icon
  │  ├─ div.card-title
  │  └─ button.btn.btn-link
  └─ div.card-content
     ├─ div.main-stat
     │  ├─ span.stat-number
     │  └─ span.stat-label
     └─ div.sub-stats
        ├─ div.sub-stat / .warning / .offline / .critical
  ```

### 2.5.3 数据绑定规范
- 每个 `stats-card` 可定义 `data-card-id`（例如 `servers`, `users`, `apps`, `health`），用于脚本识别。
- `main-stat`：展示核心数值（数字、百分比或评分），建议支持单位（%、分等）。
- `sub-stats`：最多 4 组，分别包含指示器、标签和值。可根据状态添加额外类（`warning`, `offline`, `critical`）。
- 与后端接口的契约示例：
  ```json
  {
    "cardId": "servers",
    "main": { "value": 8, "label": "总计" },
    "subs": [
      { "label": "在线", "value": 6, "status": "normal" },
      { "label": "告警", "value": 1, "status": "warning" },
      { "label": "离线", "value": 1, "status": "offline" }
    ],
    "link": "/admin/servers"
  }
  ```

### 2.5.4 状态与交互
- Hover：卡片轻微提升（`transform: translateY(-2px)`），强调可点击性；标题旁 `btn-link` 支持跳转详情。
- 告警标识：
  - `div.sub-stat.warning` 使用橙色背景与边框。
  - `div.sub-stat.offline` 使用灰度背景。
  - `div.sub-stat.critical` 使用红色背景。
- Loading：用骨架屏或淡入动画，避免空白闪烁。
- Empty/Error：在 `card-content` 内显示占位提示与重试按钮。
- 可选功能：支持拖拽排序或用户自定义卡片显示顺序。

### 2.5.5 可观测性
- 在 `DOMContentLoaded` 或数据刷新后记录日志，便于排查更新失败问题。
- 建议曝光 `data-metric-name` 供埋点系统采集（如点击“查看详情”次数、卡片展示顺序等）。

---

## 2.6 内容工具栏与主内容区

### 2.6.1 内容工具栏（`div.content-toolbar`）

#### 结构与语义
```
div.content-toolbar
├─ div.toolbar-left
│  ├─ div.search-box
│  │  ├─ i.search-icon
│  │  └─ input.search-input
│  └─ select.filter-select
└─ div.toolbar-right
   ├─ button.btn.btn-secondary (视图切换)
   └─ button.btn.btn-primary (快速操作)
```

#### 交互逻辑
- 搜索框：
  - `input.search-input` 使用 debounce（300ms）触发 `onSearch(keyword)`。
  - 支持快捷键 `Ctrl/Cmd + K` 聚焦输入。
  - 空值时恢复默认列表。
- 时间范围下拉：
  - `select` 通过 `change` 事件触发 `onRangeChange(range)`。
  - 需与统计概览、主内容区共享状态，可走全局 store 或自定义事件。
- 视图切换：
  - `btn-secondary` 在“网格/列表”等视图间切换，按钮应保持当前态反馈（高亮或图标变化）。
- 快速操作：
  - `btn-primary` 触发操作面板或模态（如新建工单、批量操作）。
  - 若操作需要权限，需提前校验；禁用时提供 Tooltip。

#### 响应式调整
- 小屏模式下 `content-toolbar` 变为纵向堆叠：
  - `flex-direction: column; align-items: stretch;`
  - 搜索框宽度 100%，下拉框与按钮组换行。

### 2.6.2 主内容区模板（`div.main-content-area`）

#### 通用结构
```
div.main-content-area
├─ div.section-header
│  ├─ h3.section-title
│  │  ├─ i (图标)
│  │  └─ 标题文本
│  └─ div.section-actions (可选)
└─ div.content-area
   └─ 模块主体（表格/图表/占位）
```

#### 状态设计
- `loading`：显示骨架屏或旋转动画。
- `empty`：提示空状态 + 二次操作按钮（如“新建任务”）。
- `error`：展示错误信息与“重试”入口。
- `ready`：渲染真实内容组件。

#### 模块实例
1. **服务器监控面板（默认）**
   - `section-title` 图标：🖥️。
   - `section-actions`：`管理服务器`（跳转管理页）、`刷新`（调用刷新 API）。
   - `content-area`：
     - 默认展示说明文字，可替换为监控总览图表（如 CPU、内存折线图）。
     - 组件建议拆分为 `ServerOverviewCard`、`ResourceChart` 等。

2. **分析面板演示**
   - 网格布局 `display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr))`。
   - 四个信息卡：
     - 实时监控（📈）
     - 活动日志（📋）
     - 告警中心（⚠️）
     - 快速操作（🚀）
   - 每个卡片可映射到功能子页链接。

3. **系统工具**
   - 垂直布局 `flex-direction: column; gap: 16px;`。
   - 列表项说明各工具功能：性能分析、安全检查、备份管理、系统配置。
   - 可扩展为可折叠面板或卡片点击打开的详情。

#### 数据与事件
- 所有主内容模块应接收统一的 `filters`（搜索词、时间范围、环境等）作为 props/input。
- 推荐采用事件驱动更新：
  - `onToolbarChange` -> `emit` 或调用 store -> 各模块响应。
  - 各模块完成数据加载后回报状态：`loaded`、`failed`，供页面显示全局提示。

### 2.6.3 扩展与复用建议
- 将 `section-header` 抽象为复用组件，支持传入标题、图标、按钮数组。
- `content-area` 可配合 Slot/Fragment，嵌入图表库（Chart.js/ECharts）、表格（SAP UI5 表格）、自定义 React/Vue 组件。
- 提供 `data-section-id` 属性，便于埋点与自动化测试定位。
- 预留 `aria` 属性和键盘导航支持，满足可访问性要求。
---

## 3. 任务拆解清单

| 序号 | 任务 | 描述 | 状态 |
| --- | --- | --- | --- |
| 1 | 内容框架结构实现 | 按 `main.content-framework` → `content-scrollable` → 面包屑/页头/统计区/主内容区的层级搭建 Thymeleaf 或前端组件骨架，确保滚动容器分离。 | 待开始 |
| 2 | 面包屑与页头开发 | 接入“一级菜单=侧边导航当前项、二级菜单=内容页内部跳转”数据结构，完成交互（首项可跳转、当前项禁用）与按钮权限控制。 | 待开始 |
| 3 | 统计概览区组件化 | 实现四类 `stats-card` 模板，支持 `data-card-id` 绑定、子指标状态样式（normal/warning/offline/critical）、骨架屏与空/错状态。 | 待开始 |
| 4 | 内容工具栏交互 | 完成搜索框（含 debounce 与快捷键）、时间范围下拉、视图切换、快速操作按钮的响应式布局与事件派发。 | 待开始 |
| 5 | 主内容区模块落地 | 依据模板结构接入服务器监控面板、分析面板、系统工具三段，统一 loading/empty/error 状态处理，并对接全局筛选条件。 | 待开始 |
| 6 | 状态管理与数据流 | 设计事件总线或 store，将工具栏筛选条件、统计卡片刷新、主内容区加载串联，记录关键日志与埋点。 | 待开始 |
| 7 | 响应式与可访问性 | 完善移动端布局调整、滚动条样式、ARIA 属性、键盘导航支持，确保跨设备体验一致。 | 待开始 |

