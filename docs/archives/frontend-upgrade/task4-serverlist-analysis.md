# ServerListManager 模块分析

**分析日期**: 2025-10-11
**目标**: 提取ServerListManager为独立模块

---

## 📊 代码规模统计

### 核心函数统计（约20个）

#### 数据加载和渲染 (3个)
1. `loadServerGroupList()` - 加载服务器列表
2. `renderServerGroupList()` - 渲染服务器列表
3. `filterServerList()` - 过滤服务器列表

#### 视图切换 (2个)
4. `switchServerView()` - 切换视图模式（表格/卡片）
5. 视图模式state存储

#### 批量操作 (4个)
6. `attachServerCheckboxEvents()` - 绑定复选框事件
7. `toggleSelectAllServers()` - 全选/取消全选
8. `batchRefreshServers()` - 批量刷新
9. `clearServerSelection()` - 清除选择

#### 自动刷新 (4个)
10. `refreshAllServerGroupData()` - 刷新所有数据
11. `startServerGroupAutoRefresh()` - 启动自动刷新
12. `stopServerGroupAutoRefresh()` - 停止自动刷新
13. `updateServerGroupLastUpdateTime()` - 更新最后更新时间

#### 其他功能 (7个)
14. `exportServerGroupReport()` - 导出报告
15. `addNewServer()` - 添加新服务器
16. `refreshServer()` - 刷新单个服务器
17. `initServerGroupManagement()` - 初始化
18. `cleanupServerGroupManagement()` - 清理
19. `initServerGroupData()` - 初始化数据
20. `initServerGroupEvents()` - 初始化事件

---

## 🔗 依赖关系分析

### 外部依赖

#### 1. DOM元素
```typescript
// 主容器
const tableBody = document.getElementById("serverTableBody");
const viewToggleBtn = document.getElementById("viewToggle");
const selectAllCheckbox = document.getElementById("selectAllServers");
const batchRefreshBtn = document.querySelector('[data-action="batch-refresh"]');
// ... 等等
```

#### 2. 调用外部函数
```typescript
// 图表加载函数（来自同一模块）
loadHealthTrendChart();
loadLoadDistributionChart();
loadAppDistributionChart();

// 服务器详情（来自ServerDetailOverlay）
viewServerDetails(serverId);

// SSH连接
connectToServer(serverId);
```

#### 3. API调用
```typescript
// 服务器列表API
fetch("/admin/server-groups/api/list");

// 批量刷新API
fetch("/admin/server-groups/api/batch-refresh", {
    method: "POST",
    body: JSON.stringify({ serverIds })
});

// 单个服务器刷新API
fetch(`/admin/server-groups/api/batch-refresh`, {
    method: "POST",
    body: JSON.stringify({ serverIds: [serverId] })
});
```

### 状态管理

#### 当前状态变量
```typescript
// 视图模式
let currentViewMode = 'table'; // 或 'card'

// 过滤条件
let currentFilter = {
    status: 'all',
    search: ''
};

// 自动刷新
let autoRefreshTimer = null;
let autoRefreshInterval = 30000; // 30秒

// 选中的服务器
let selectedServerIds = new Set();
```

---

## 📐 模块接口设计

### 公共API

```typescript
export interface ServerListManagerAPI {
    // 初始化和清理
    init(): void;
    cleanup(): void;

    // 数据加载
    load(options?: LoadOptions): Promise<void>;
    refresh(): Promise<void>;
    refreshServer(serverId: number): Promise<void>;

    // 视图控制
    switchView(mode: 'table' | 'card'): void;
    filter(criteria: FilterCriteria): void;

    // 批量操作
    selectAll(): void;
    deselectAll(): void;
    batchRefresh(): Promise<void>;
    getSelected(): number[];

    // 自动刷新
    startAutoRefresh(interval?: number): void;
    stopAutoRefresh(): void;

    // 其他
    export(): void;
    addNew(): void;
}

export interface LoadOptions {
    silent?: boolean;
    applyFilter?: boolean;
}

export interface FilterCriteria {
    status?: 'all' | 'online' | 'offline' | 'warning';
    search?: string;
}
```

### 依赖注入

```typescript
export interface ServerListDependencies {
    // DOM容器
    container: HTMLElement;

    // 工具函数
    utils: {
        formatBytes: Function;
        formatUptime: Function;
        formatPercentage: Function;
        showSuccess: Function;
        showError: Function;
        // ...
    };

    // 外部函数
    viewServerDetails?: (serverId: number) => void;
    connectToServer?: (serverId: number) => void;
    loadCharts?: () => void;

    // 国际化
    t: (key: string, namespace: string) => string;
}
```

### 事件系统

```typescript
export interface ServerListEvents {
    // 数据事件
    'data:loaded': (servers: Server[]) => void;
    'data:refreshed': () => void;
    'server:refreshed': (serverId: number) => void;

    // 选择事件
    'selection:changed': (selectedIds: number[]) => void;
    'selection:cleared': () => void;

    // 视图事件
    'view:changed': (mode: 'table' | 'card') => void;
    'filter:applied': (criteria: FilterCriteria) => void;
}
```

---

## 🏗️ 类设计

```typescript
export class ServerListManager {
    // 私有属性
    private container: HTMLElement;
    private servers: Server[] = [];
    private filteredServers: Server[] = [];
    private selectedIds: Set<number> = new Set();
    private viewMode: 'table' | 'card' = 'table';
    private filterCriteria: FilterCriteria = { status: 'all', search: '' };
    private autoRefreshTimer: number | null = null;
    private autoRefreshInterval: number = 30000;

    // 依赖
    private deps: ServerListDependencies;
    private eventBus: EventEmitter;

    // 构造函数
    constructor(deps: ServerListDependencies) {
        this.deps = deps;
        this.container = deps.container;
        this.eventBus = new EventEmitter();
    }

    // 公共方法
    async init(): Promise<void> {
        this.attachEvents();
        await this.load();
    }

    cleanup(): void {
        this.stopAutoRefresh();
        this.detachEvents();
    }

    async load(options: LoadOptions = {}): Promise<void> {
        const { silent = false, applyFilter = false } = options;

        if (!silent) {
            this.showLoading();
        }

        try {
            const response = await fetch("/admin/server-groups/api/list");
            const data = await response.json();

            this.servers = data.servers || [];

            if (applyFilter) {
                this.applyFilter();
            } else {
                this.filteredServers = [...this.servers];
            }

            this.render();
            this.eventBus.emit('data:loaded', this.servers);
        } catch (error) {
            this.showError(error);
        }
    }

    switchView(mode: 'table' | 'card'): void {
        if (this.viewMode === mode) return;

        this.viewMode = mode;
        this.render();
        this.eventBus.emit('view:changed', mode);
    }

    filter(criteria: FilterCriteria): void {
        this.filterCriteria = { ...this.filterCriteria, ...criteria };
        this.applyFilter();
        this.render();
        this.eventBus.emit('filter:applied', this.filterCriteria);
    }

    selectAll(): void {
        this.selectedIds.clear();
        this.filteredServers.forEach(server => {
            this.selectedIds.add(server.id);
        });
        this.updateCheckboxes();
        this.eventBus.emit('selection:changed', Array.from(this.selectedIds));
    }

    deselectAll(): void {
        this.selectedIds.clear();
        this.updateCheckboxes();
        this.eventBus.emit('selection:cleared');
    }

    async batchRefresh(): Promise<void> {
        const serverIds = Array.from(this.selectedIds);

        if (serverIds.length === 0) {
            this.deps.utils.showError("请先选择要刷新的服务器");
            return;
        }

        try {
            await fetch("/admin/server-groups/api/batch-refresh", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ serverIds })
            });

            this.deps.utils.showSuccess(`已刷新 ${serverIds.length} 台服务器`);
            await this.load({ silent: true, applyFilter: true });
        } catch (error) {
            this.deps.utils.showError("批量刷新失败");
        }
    }

    // 私有方法
    private applyFilter(): void {
        let filtered = [...this.servers];

        // 状态过滤
        if (this.filterCriteria.status && this.filterCriteria.status !== 'all') {
            filtered = filtered.filter(server =>
                server.status === this.filterCriteria.status
            );
        }

        // 搜索过滤
        if (this.filterCriteria.search) {
            const search = this.filterCriteria.search.toLowerCase();
            filtered = filtered.filter(server =>
                server.name.toLowerCase().includes(search) ||
                server.host.toLowerCase().includes(search)
            );
        }

        this.filteredServers = filtered;
    }

    private render(): void {
        if (this.viewMode === 'table') {
            this.renderTable();
        } else {
            this.renderCards();
        }
    }

    private renderTable(): void {
        // 渲染表格视图
    }

    private renderCards(): void {
        // 渲染卡片视图
    }

    // ... 其他私有方法
}
```

---

## 📦 提取步骤

### 阶段1: 准备工作 ✅
1. ✅ 分析代码结构和依赖
2. ✅ 设计模块接口
3. ⏳ 创建类型定义文件

### 阶段2: 提取代码
1. ⏳ 创建 `ServerListManager.ts` 文件
2. ⏳ 迁移状态变量
3. ⏳ 迁移核心函数
4. ⏳ 实现类方法

### 阶段3: 集成测试
1. ⏳ 更新 `server-group-management.ts`
2. ⏳ 测试列表加载
3. ⏳ 测试视图切换
4. ⏳ 测试批量操作
5. ⏳ 测试自动刷新

### 阶段4: 优化清理
1. ⏳ 移除旧代码
2. ⏳ 添加单元测试
3. ⏳ 更新文档

---

## ⚠️ 注意事项

### 1. 与ServerDetailOverlay的交互
ServerListManager需要能够调用`viewServerDetails()`:
```typescript
// 在ServerListManager中
onViewDetails(serverId: number) {
    if (this.deps.viewServerDetails) {
        this.deps.viewServerDetails(serverId);
    }
}
```

### 2. 与图表模块的交互
列表刷新时需要更新图表:
```typescript
// 刷新后调用
if (this.deps.loadCharts) {
    this.deps.loadCharts();
}
```

### 3. 状态持久化
考虑将视图模式和过滤条件保存到localStorage:
```typescript
// 保存状态
localStorage.setItem('serverListViewMode', this.viewMode);
localStorage.setItem('serverListFilter', JSON.stringify(this.filterCriteria));

// 恢复状态
this.viewMode = localStorage.getItem('serverListViewMode') || 'table';
this.filterCriteria = JSON.parse(
    localStorage.getItem('serverListFilter') || '{"status":"all","search":""}'
);
```

### 4. 性能优化
对于大量服务器的情况:
- 实现虚拟滚动
- 分页加载
- 搜索防抖

---

## 🎯 预期效果

### 代码组织
- ✅ 从2474行减少约600行
- ✅ 列表管理逻辑独立
- ✅ 可独立测试
- ✅ 可复用

### 可维护性
- ✅ 单一职责原则
- ✅ 清晰的API接口
- ✅ 事件驱动架构
- ✅ 易于扩展

---

## 📊 与ServerDetailOverlay对比

| 特性 | ServerDetailOverlay | ServerListManager |
|-----|---------------------|-------------------|
| 代码规模 | ~800行 (35个函数) | ~600行 (20个函数) |
| DOM操作 | 重度（overlay） | 中度（列表渲染） |
| 状态复杂度 | 高（图表、导航、数据） | 中（过滤、选择、视图） |
| 外部依赖 | Chart.js、工具函数 | 主要是工具函数 |
| 提取难度 | 较高 | 中等 |

---

**分析完成**: 2025-10-11
**下一步**: 创建类型定义文件，然后开始提取
