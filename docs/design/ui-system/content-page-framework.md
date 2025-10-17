# 内容页框架设计规范

## 📋 设计目标

创建一个通用的内容页框架，能够与标题栏和侧边导航栏完美集成，为所有后续页面开发提供统一的结构和样式基础。

## 🎨 框架架构

### 1. 总体布局结构

```
┌─────────────────────────────────────────────────────────────┐
│                    标题栏 (64px)                              │
├─────────┬───────────────────────────────────────────────────┤
│         │                 页面标题区                         │
│  侧边   │         [图标] [标题] [描述]  [操作按钮组]          │
│  导航   ├───────────────────────────────────────────────────┤
│  栏     │                 统计概览区                         │
│ (240px) │     [卡片1] [卡片2] [卡片3] [卡片4] [卡片5]        │
│         ├───────────────────────────────────────────────────┤
│         │                 主要内容区                         │
│         │  [过滤器/搜索]  [视图切换]  [操作工具]            │
│         │  ┌─────────────────────────────────────────────┐  │
│         │  │            内容展示区域                      │  │
│         │  │        (表格/卡片/图表/列表等)               │  │
│         │  └─────────────────────────────────────────────┘  │
│         ├───────────────────────────────────────────────────┤
│         │                 底部扩展区                         │
│         │   [详细分析]  [相关信息]  [历史记录]             │
│         └───────────────────────────────────────────────────┘
└─────────────────────────────────────────────────────────────┘
```

### 2. 框架组件体系

#### 2.1 内容容器框架 (.content-framework)
- **基础容器**: 统一内边距、背景色、与导航栏的间距
- **滚动管理**: 独立滚动区域，不影响导航和标题栏
- **响应式适配**: 与导航栏展开/收起状态联动

#### 2.2 页面头部区域 (.page-header)
- **标题组件**: 页面图标、主标题、描述文案
- **面包屑导航**: 层级导航路径指示
- **操作按钮组**: 页面级操作（新建、刷新、导出等）

#### 2.3 统计概览区域 (.stats-overview)
- **统计卡片**: 标准化的数据展示卡片
- **响应式网格**: 自适应卡片数量和布局
- **实时更新**: 支持数据刷新和状态指示

#### 2.4 内容工具栏 (.content-toolbar)
- **过滤器组件**: 搜索框、下拉选择、日期选择等
- **视图切换**: 列表/卡片/表格等视图模式切换
- **批量操作**: 选择、导出、删除等批量操作

#### 2.5 主内容展示区 (.main-content-area)
- **数据展示组件**: 支持表格、卡片、图表等多种展示形式
- **分页组件**: 统一的分页样式和交互
- **加载状态**: 统一的loading和empty状态

#### 2.6 底部扩展区域 (.content-footer)
- **详细信息**: 选中项的详细信息展示
- **相关数据**: 关联数据的展示
- **操作历史**: 最近操作记录

## 🎨 样式设计规范

### 3.1 布局尺寸规范

```css
:root {
  /* 框架尺寸变量 */
  --header-height: 64px;
  --sidebar-width: 240px;
  --sidebar-collapsed-width: 72px;

  /* 内容区域间距 */
  --content-padding: 24px 32px;
  --section-gap: 24px;
  --card-gap: 16px;

  /* 组件高度规范 */
  --page-header-height: auto;
  --stats-card-height: 120px;
  --toolbar-height: 64px;
}

/* 基础内容框架 */
.content-framework {
  padding: var(--content-padding);
  background: var(--content-bg);
  min-height: calc(100vh - var(--header-height));
  display: flex;
  flex-direction: column;
  gap: var(--section-gap);
}

/* 与导航栏联动的响应式 */
.sidebar-collapsed .content-framework {
  padding-left: calc(32px + var(--sidebar-collapsed-width) - var(--sidebar-width));
}
```

### 3.2 组件样式规范

#### 页面头部样式
```css
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 0;
  border-bottom: 1px solid var(--border-color);
}

.page-title-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--primary-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.page-description {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 4px 0 0;
}
```

#### 统计卡片样式
```css
.stats-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--card-gap);
  margin: var(--section-gap) 0;
}

.stats-card {
  background: var(--card-bg);
  border-radius: var(--border-radius-lg);
  padding: 20px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  transition: all 0.2s ease;
}

.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
```

### 3.3 主题适配规范

#### 管理员主题
```css
.admin-theme {
  --primary-gradient: linear-gradient(135deg, #2F9BFF, #3BC6B8);
  --content-bg: rgba(248, 250, 255, 0.72);
  --card-bg: rgba(255, 255, 255, 0.92);
  --primary-color: #2F9BFF;
  --success-color: #3BC6B8;
}
```

#### 开发者主题
```css
.developer-theme {
  --primary-gradient: linear-gradient(135deg, #6C63FF, #3D8BFF);
  --content-bg: rgba(240, 242, 255, 0.75);
  --card-bg: rgba(255, 255, 255, 0.94);
  --primary-color: #6C63FF;
  --success-color: #3D8BFF;
}
```

## 🔧 JavaScript交互框架

### 4.1 页面生命周期管理

```javascript
// 内容页面基类
class ContentPageFramework {
  constructor(pageId, options = {}) {
    this.pageId = pageId;
    this.options = {
      autoRefresh: true,
      refreshInterval: 30000,
      enableKeyboardShortcuts: true,
      ...options
    };

    this.initialize();
  }

  // 页面初始化
  async initialize() {
    await this.loadInitialData();
    this.setupEventHandlers();
    this.setupRealTimeUpdates();
    this.setupKeyboardShortcuts();
  }

  // 数据加载框架
  async loadInitialData() {
    try {
      this.showLoadingState();
      const data = await this.fetchPageData();
      this.renderPageContent(data);
      this.hideLoadingState();
    } catch (error) {
      this.showErrorState(error);
    }
  }

  // 内容渲染框架
  renderPageContent(data) {
    this.renderStatsOverview(data.stats);
    this.renderMainContent(data.content);
    this.renderFooterContent(data.footer);
  }

  // 实时更新框架
  setupRealTimeUpdates() {
    if (this.options.autoRefresh) {
      this.refreshTimer = setInterval(() => {
        this.refreshPageData();
      }, this.options.refreshInterval);
    }
  }

  // 销毁页面
  destroy() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
    this.removeEventHandlers();
  }
}
```

### 4.2 统一的数据获取接口

```javascript
// 数据获取工具类
class ContentDataService {
  static async fetchWithAuth(url, options = {}) {
    const csrfToken = document.querySelector('input[name="_csrf"]')?.value;

    return fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': csrfToken,
        ...options.headers
      }
    });
  }

  static async handleResponse(response) {
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    return response.json();
  }
}
```

### 4.3 通用UI组件

```javascript
// 统计卡片组件
class StatsCard {
  static render(container, data) {
    container.innerHTML = `
      <div class="stats-card">
        <div class="card-header">
          <div class="card-icon">
            <i class="${data.icon}"></i>
          </div>
          <div class="card-title">${data.title}</div>
        </div>
        <div class="card-content">
          <div class="main-stat">
            <span class="stat-number">${data.mainValue}</span>
            <span class="stat-label">${data.mainLabel}</span>
          </div>
          ${data.subStats ? this.renderSubStats(data.subStats) : ''}
        </div>
      </div>
    `;
  }
}

// 加载状态组件
class LoadingState {
  static show(container, message = '加载中...') {
    container.innerHTML = `
      <div class="loading-placeholder">
        <div class="loading-spinner"></div>
        <p>${message}</p>
      </div>
    `;
  }

  static hide(container) {
    const loading = container.querySelector('.loading-placeholder');
    if (loading) loading.remove();
  }
}
```

## 📱 响应式设计规范

### 5.1 断点规范
```css
/* 响应式断点 */
:root {
  --breakpoint-mobile: 768px;
  --breakpoint-tablet: 1024px;
  --breakpoint-desktop: 1280px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .content-framework {
    padding: 16px;
    gap: 16px;
  }

  .stats-overview {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
```

### 5.2 导航栏响应式联动
```css
/* 导航栏收起时的内容区域调整 */
@media (min-width: 992px) {
  .layout.collapsed .content-framework {
    margin-left: calc(72px - 240px);
    transition: margin-left 0.3s ease;
  }
}

/* 移动端抽屉式导航时的遮罩 */
@media (max-width: 991px) {
  .mobile-nav-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 999;
  }
}
```

## 🛠️ 使用指南

### 6.1 创建新内容页面的步骤

1. **创建HTML片段文件**
```html
<!-- templates/admin/new-page-content.html -->
<div th:fragment="new-page-content" class="content-framework new-page">
  <!-- 页面头部 -->
  <div class="page-header">
    <div class="page-title-group">
      <div class="page-icon">
        <i class="fas fa-cog"></i>
      </div>
      <div>
        <h1 class="page-title">页面标题</h1>
        <p class="page-description">页面描述文字</p>
      </div>
    </div>
    <div class="page-actions">
      <button class="btn btn-primary">主要操作</button>
    </div>
  </div>

  <!-- 统计概览 -->
  <div class="stats-overview" id="statsOverview">
    <!-- 动态统计卡片 -->
  </div>

  <!-- 主要内容 -->
  <div class="main-content-section">
    <div class="content-toolbar">
      <!-- 工具栏 -->
    </div>
    <div class="main-content-area">
      <!-- 主要内容 -->
    </div>
  </div>
</div>
```

2. **在main-layout.html中添加JavaScript逻辑**
```javascript
// 在 main-layout.html 的 handleContentEvents 中添加
case 'new-page':
  initNewPage();
  break;

function initNewPage() {
  const newPage = new ContentPageFramework('new-page', {
    autoRefresh: true,
    refreshInterval: 30000
  });
}
```

3. **创建对应的CSS样式**
```css
/* 在对应的CSS文件中添加页面特有样式 */
.new-page {
  /* 页面特有样式 */
}
```

### 6.2 框架配置选项

```javascript
// ContentPageFramework 配置项
const pageOptions = {
  autoRefresh: true,           // 是否自动刷新
  refreshInterval: 30000,      // 刷新间隔(毫秒)
  enableKeyboardShortcuts: true, // 是否启用键盘快捷键
  showLoadingOnRefresh: true,  // 刷新时是否显示加载状态
  errorRetryCount: 3,         // 错误重试次数
  theme: 'admin'              // 主题名称
};
```

## 🎯 设计原则

1. **一致性**: 所有页面保持统一的视觉语言和交互模式
2. **响应式**: 完美适配各种屏幕尺寸和设备类型
3. **可扩展**: 框架易于扩展，支持自定义组件和样式
4. **性能优化**: 采用懒加载、缓存等策略提升性能
5. **可访问性**: 遵循WCAG指南，支持键盘导航和屏幕阅读器
6. **主题统一**: 与整体设计系统保持一致，支持角色主题切换

## 📚 示例页面类型

### 管理类页面
- 服务器管理
- 用户管理
- 权限配置
- 系统设置

### 监控类页面
- 服务器监控
- 应用监控
- 性能分析
- 告警管理

### 数据类页面
- 统计报表
- 历史数据
- 日志查看
- 审计记录

每种页面类型都可以基于此框架快速开发，保证UI一致性和用户体验的连贯性。