# Thymeleaf + Vanilla JS 与 React Router 双向导航同步最佳实践

## 执行摘要

本文档研究了在单页应用(SPA)架构中实现主应用(Thymeleaf + vanilla JS)与嵌入式React应用(React Router v6)之间双向导航同步的最佳实践。

**关键发现：**
- 推荐使用基于发布-订阅模式的CustomEvent机制
- 避免循环触发需要引入"正在同步"标志和事件源追踪
- React Router的useLocation Hook与window.postMessage结合最稳定
- 浏览器历史管理需要特殊处理，避免重复pushState

---

## 1. 当前架构分析

### 1.1 项目结构现状

**主应用层(Thymeleaf + Vanilla JS)**
```
src/main/resources/templates/main-layout.html
├── HTML框架 (grid布局)
├── 侧边栏导航 (data-role="sidebar")
├── 内容区域 (data-role="spa-outlet")
└── JavaScript事件系统 (emit/on事件循环)
```

**React应用层**
```
src/main/frontend/react-app/
├── App.tsx (主应用入口)
├── App Router (BrowserRouter with Routes)
├── layout/ShellLayout.tsx (导航UI)
└── pages/* (各页面组件)
```

### 1.2 现有通信机制

主应用已实现了基于发布-订阅的简单事件系统：

```javascript
// 现有的事件循环机制 (来自main-layout.html)
on('spa:navigate', ({ route }) => {
    outlet.dataset.route = route;
    emit('spa:loading', { route });
    loadContent(route).then(() => {
        // 初始化页面
    });
});
```

**问题：** 该系统仅用于主应用内部路由，与React Router无关联。

### 1.3 React应用当前状态

```typescript
// src/main/frontend/react-app/App.tsx
export function App({ basename }: AppProps = {}): JSX.Element {
    const base = basename ?? resolveBasename();

    return (
        <BrowserRouter basename={base}>
            <AppRoutes />
        </BrowserRouter>
    );
}

// src/main/frontend/react-app/layout/ShellLayout.tsx
export function ShellLayout({ children }: PropsWithChildren): JSX.Element {
    const { t } = useTranslation();
    const navItems = [
        { path: "/overview", labelKey: "navigation.overviewLabel", ... },
        { path: "/releases", labelKey: "navigation.releasesLabel", ... },
        { path: "/settings/policies", labelKey: "navigation.policiesLabel", ... }
    ];

    return (
        <aside className="dp-shell__sidebar">
            <nav className="dp-shell__nav">
                {navItems.map(item => (
                    <NavLink to={item.path} ...>
                        {/* 导航项 */}
                    </NavLink>
                ))}
            </nav>
        </aside>
    );
}
```

**问题：** React应用拥有自己独立的导航系统，与主应用侧边栏完全隔离。

---

## 2. 推荐的通信架构

### 2.1 架构设计原则

```
┌─────────────────────────────────────┐
│     Main App (Thymeleaf + JS)       │
│  ┌─────────────────────────────┐    │
│  │   Sidebar Navigation        │    │
│  │  (data-role="sidebar")      │    │
│  └──────────────┬──────────────┘    │
│                 │                    │
│         Navigation Bridge            │
│    (CustomEvent + postMessage)       │
│                 │                    │
│  ┌──────────────▼──────────────┐    │
│  │   Deploy Platform Content   │    │
│  │  ┌─────────────────────┐    │    │
│  │  │   React App Area    │    │    │
│  │  │  (deploy-platform-  │    │    │
│  │  │   root)             │    │    │
│  │  │ ┌────────────────┐  │    │    │
│  │  │ │ React Router   │  │    │    │
│  │  │ │ + NavLinks     │  │    │    │
│  │  │ └────────────────┘  │    │    │
│  │  └─────────────────────┘    │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

### 2.2 推荐方案：CustomEvent + 发布-订阅

基于项目现有的事件系统扩展，同时利用React Router的能力。

#### 2.2.1 事件定义

```typescript
// src/main/frontend/react-app/bridge/navigationBridge.ts

// 事件类型定义
interface NavigationEvent {
    type: 'app' | 'react' | 'system'; // 事件源
    path: string;
    timestamp: number;
    metadata?: Record<string, unknown>;
}

interface NavigationBridgeState {
    isSyncing: boolean;
    lastEventSource: 'app' | 'react' | null;
    currentPath: string;
}

// 全局状态 (防止循环触发)
const bridgeState: NavigationBridgeState = {
    isSyncing: false,
    lastEventSource: null,
    currentPath: '/'
};

// 事件监听器容器
const navigationListeners: Set<(event: NavigationEvent) => void> = new Set();
```

#### 2.2.2 主应用桥接器 (Vanilla JS)

```javascript
// 在main-layout.html中添加

// ==================== 导航同步桥接器 ====================
const NavigationBridge = (() => {
    const EVENT_NAMESPACE = 'deploy-platform:navigation';
    const REACT_READY_EVENT = 'deploy-platform:ready';
    let isReactReady = false;
    let isSyncing = false;

    // 监听React应用就绪
    window.addEventListener('message', (event) => {
        if (event.data?.type === REACT_READY_EVENT) {
            isReactReady = true;
            console.log('[Navigation Bridge] React app ready for navigation sync');
        }
    });

    return {
        // 当主应用侧边栏被点击时触发
        notifyReactNavigation(path) {
            if (isSyncing) {
                console.log('[Navigation Bridge] Sync in progress, ignoring event');
                return;
            }

            isSyncing = true;
            console.log('[Navigation Bridge] Main app -> React navigation:', path);

            if (isReactReady) {
                // 发送postMessage给React应用
                const reactContainer = document.getElementById('deploy-platform-root');
                if (reactContainer?.contentWindow || window.ReactAppBridge) {
                    const navEvent = {
                        type: 'navigation:from-main-app',
                        path: path,
                        timestamp: Date.now()
                    };

                    // 方式1: postMessage (如果React是iframe)
                    if (reactContainer?.contentWindow) {
                        reactContainer.contentWindow.postMessage(navEvent, window.location.origin);
                    }

                    // 方式2: 直接调用 (React在同一文档)
                    if (window.ReactAppBridge?.navigate) {
                        window.ReactAppBridge.navigate(path);
                    }
                }
            }

            setTimeout(() => { isSyncing = false; }, 100);
        },

        // React应用调用此方法来更新主应用侧边栏
        updateMainAppNavigation(path) {
            if (isSyncing) {
                console.log('[Navigation Bridge] Sync in progress, ignoring event');
                return;
            }

            isSyncing = true;
            console.log('[Navigation Bridge] React -> Main app navigation:', path);

            // 触发主应用的路由事件
            emit('spa:navigate', { route: pathToRoute(path) });

            setTimeout(() => { isSyncing = false; }, 100);
        },

        // 路径映射 (React路径 <-> 主应用路由)
        pathToRoute(path) {
            const pathMap = {
                '/overview': 'overview',
                '/releases': 'releases',
                '/settings/policies': 'policies'
            };
            return pathMap[path] || path;
        },

        routeToPath(route) {
            const routeMap = {
                'overview': '/overview',
                'releases': '/releases',
                'policies': '/settings/policies'
            };
            return routeMap[route] || '/' + route;
        }
    };
})();

// 将侧边栏导航连接到桥接器
function initNavigationBridge() {
    const sidebarLinks = document.querySelectorAll('[data-role="sidebar"] a[data-route]');

    sidebarLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            const route = link.dataset.route;
            const path = NavigationBridge.routeToPath(route);
            NavigationBridge.notifyReactNavigation(path);
        });
    });
}

// 在主应用初始化时调用
document.addEventListener('DOMContentLoaded', initNavigationBridge);
```

#### 2.2.3 React应用桥接器

```typescript
// src/main/frontend/react-app/bridge/useNavigationBridge.ts

import { useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

export function useNavigationBridge() {
    const navigate = useNavigate();
    const location = useLocation();
    const isSyncingRef = useRef(false);
    const lastPathRef = useRef<string>('');

    // 将React应用的导航能力暴露给主应用
    useEffect(() => {
        // 定义全局导航接口
        const reactAppBridge = {
            navigate: (path: string) => {
                if (isSyncingRef.current) {
                    console.log('[React Navigation Bridge] Sync in progress, ignoring');
                    return;
                }

                if (path === lastPathRef.current) {
                    console.log('[React Navigation Bridge] Same path, ignoring');
                    return;
                }

                isSyncingRef.current = true;
                console.log('[React Navigation Bridge] Navigating to:', path);

                navigate(path, { replace: false });
                lastPathRef.current = path;

                setTimeout(() => {
                    isSyncingRef.current = false;
                }, 100);
            }
        };

        // 暴露到window对象
        (window as any).ReactAppBridge = reactAppBridge;

        // 通知主应用React应用已就绪
        window.parent?.postMessage(
            { type: 'deploy-platform:ready' },
            window.location.origin
        );

        return () => {
            delete (window as any).ReactAppBridge;
        };
    }, [navigate]);

    // 当React路由改变时，通知主应用
    useEffect(() => {
        if (isSyncingRef.current || location.pathname === lastPathRef.current) {
            return;
        }

        console.log('[React Navigation Bridge] Location changed to:', location.pathname);

        // 通知主应用
        if (window.parent !== window) {
            // React应用在iframe中
            window.parent?.postMessage(
                {
                    type: 'navigation:from-react',
                    path: location.pathname,
                    timestamp: Date.now()
                },
                window.location.origin
            );
        } else {
            // React应用在同一文档中，调用主应用的更新方法
            if ((window as any).NavigationBridge?.updateMainAppNavigation) {
                (window as any).NavigationBridge.updateMainAppNavigation(
                    location.pathname
                );
            }
        }

        lastPathRef.current = location.pathname;
    }, [location.pathname]);
}
```

#### 2.2.4 在React应用中使用

```typescript
// src/main/frontend/react-app/layout/ShellLayout.tsx

import { useNavigationBridge } from '../bridge/useNavigationBridge';

export function ShellLayout({ children }: PropsWithChildren): JSX.Element {
    const { t } = useTranslation();

    // 初始化导航桥接
    useNavigationBridge();

    const navItems: NavItem[] = [
        {
            path: "/overview",
            labelKey: "navigation.overviewLabel",
            descriptionKey: "navigation.overviewDesc",
            icon: "📊",
            exact: true
        },
        // ... 其他导航项
    ];

    return (
        <div className="dp-shell" data-testid="deploy-platform-shell">
            <aside className="dp-shell__sidebar">
                <nav className="dp-shell__nav" aria-label={t('shell.navLabel')}>
                    {navItems.map((item) => (
                        <NavLink
                            key={item.path}
                            to={item.path}
                            className={({ isActive }) =>
                                [
                                    "dp-shell__nav-item",
                                    isActive ? "dp-shell__nav-item--active" : "",
                                ]
                                    .filter(Boolean)
                                    .join(" ")
                            }
                            end={item.exact}
                        >
                            <span className="dp-shell__nav-icon" aria-hidden="true">
                                {item.icon}
                            </span>
                            <span className="dp-shell__nav-body">
                                <span className="dp-shell__nav-label">
                                    {t(item.labelKey)}
                                </span>
                                <span className="dp-shell__nav-description">
                                    {t(item.descriptionKey)}
                                </span>
                            </span>
                        </NavLink>
                    ))}
                </nav>
            </aside>

            <section className="dp-shell__content">
                {/* ... 其他内容 */}
                <main className="dp-shell__main">
                    {children}
                </main>
            </section>
        </div>
    );
}
```

---

## 3. 防止循环触发的策略

### 3.1 问题分析

**循环触发场景：**
```
用户点击侧边栏
    ↓
主应用发送导航事件给React
    ↓
React应用路由改变
    ↓
React应用通知主应用更新侧边栏
    ↓
主应用发送导航事件给React (循环！)
```

### 3.2 解决方案：同步标志 + 事件源追踪

```typescript
// 核心防循环机制

class NavigationSyncManager {
    private isSyncing = false;
    private lastEventSource: 'main' | 'react' | null = null;
    private lastPath = '';
    private syncTimeout: ReturnType<typeof setTimeout> | null = null;

    /**
     * 检查是否应该处理事件
     * @param eventSource 事件源 ('main' | 'react')
     * @param path 导航路径
     * @returns 是否应该处理
     */
    shouldProcess(eventSource: 'main' | 'react', path: string): boolean {
        // 1. 如果正在同步，忽略
        if (this.isSyncing) {
            console.log('[Sync Manager] Currently syncing, ignore event');
            return false;
        }

        // 2. 如果路径相同，忽略
        if (path === this.lastPath) {
            console.log('[Sync Manager] Same path, ignore event');
            return false;
        }

        // 3. 如果事件源相同，忽略 (例如React->Main->React)
        if (this.lastEventSource === eventSource) {
            console.log('[Sync Manager] Same event source, ignore event');
            return false;
        }

        return true;
    }

    /**
     * 标记同步开始
     */
    beginSync(eventSource: 'main' | 'react', path: string) {
        this.isSyncing = true;
        this.lastEventSource = eventSource;
        this.lastPath = path;

        // 清理之前的超时
        if (this.syncTimeout) {
            clearTimeout(this.syncTimeout);
        }

        // 设置同步超时 (防止永远卡在同步状态)
        this.syncTimeout = setTimeout(() => {
            this.isSyncing = false;
            this.lastEventSource = null;
            console.log('[Sync Manager] Sync timeout, reset state');
        }, 300); // 300ms是合理的同步窗口
    }

    /**
     * 标记同步结束
     */
    endSync() {
        if (this.syncTimeout) {
            clearTimeout(this.syncTimeout);
        }
        this.isSyncing = false;
    }
}

// 使用示例
const syncManager = new NavigationSyncManager();

// 在主应用中
NavigationBridge.notifyReactNavigation = (path) => {
    if (!syncManager.shouldProcess('main', path)) {
        return;
    }

    syncManager.beginSync('main', path);
    window.ReactAppBridge?.navigate(path);
    syncManager.endSync();
};

// 在React应用中
useEffect(() => {
    if (!syncManager.shouldProcess('react', location.pathname)) {
        return;
    }

    syncManager.beginSync('react', location.pathname);
    window.parent?.postMessage(
        {
            type: 'navigation:from-react',
            path: location.pathname,
            timestamp: Date.now()
        },
        window.location.origin
    );
    syncManager.endSync();
}, [location.pathname]);
```

### 3.3 事件流程图

```
时间轴
│
├─ T0: 用户点击侧边栏 /releases
│  ├─ 主应用: shouldProcess('main', '/releases') → true
│  ├─ 主应用: isSyncing = true, lastSource = 'main'
│  ├─ 主应用: 调用 React.navigate('/releases')
│  └─ React开始更新
│
├─ T1: React完成路由更新，location改变为 /releases
│  ├─ React: shouldProcess('react', '/releases') → false
│  │         (因为 lastSource === 'main', 且path相同)
│  └─ React: 忽略发送回主应用
│
├─ T2: 主应用同步超时 300ms
│  ├─ 主应用: isSyncing = false, lastSource = null
│  └─ 状态复位
│
└─ T3: 用户再次点击导航
   └─ 正常处理
```

---

## 4. 时序问题处理

### 4.1 React未初始化问题

**场景：** 主应用在React应用挂载前就尝试导航

```typescript
// 解决方案1: 延迟导航请求队列

class NavigationQueue {
    private queue: Array<{ path: string; timestamp: number }> = [];
    private isReactReady = false;

    init() {
        // 监听React就绪信号
        window.addEventListener('message', (event) => {
            if (event.data?.type === 'deploy-platform:ready') {
                this.isReactReady = true;
                this.processQueue();
            }
        });
    }

    enqueue(path: string) {
        if (this.isReactReady) {
            // React已就绪，直接执行
            window.ReactAppBridge?.navigate(path);
        } else {
            // React未就绪，加入队列
            this.queue.push({ path, timestamp: Date.now() });
            console.log(`[Navigation Queue] Queued navigation to ${path}, pending React ready`);
        }
    }

    private processQueue() {
        console.log(`[Navigation Queue] Processing ${this.queue.length} queued navigations`);

        while (this.queue.length > 0) {
            const { path } = this.queue.shift()!;
            window.ReactAppBridge?.navigate(path);
        }
    }
}

const navQueue = new NavigationQueue();
navQueue.init();

// 使用
NavigationBridge.notifyReactNavigation = (path) => {
    navQueue.enqueue(path);
};
```

```typescript
// 解决方案2: 在React应用中添加就绪检查

// src/main/frontend/react-app/bridge/useNavigationBridge.ts

export function useNavigationBridge() {
    const navigate = useNavigate();

    useEffect(() => {
        const reactAppBridge = {
            navigate: (path: string) => {
                console.log('[React] Navigating to:', path);
                navigate(path);
            }
        };

        (window as any).ReactAppBridge = reactAppBridge;

        // 通知主应用React已就绪
        // 使用多种方式确保通知被收到
        const notifyReady = () => {
            window.parent?.postMessage(
                { type: 'deploy-platform:ready' },
                window.location.origin
            );
        };

        // 立即通知
        notifyReady();

        // 100ms后再通知一次 (确保主应用已注册监听器)
        const timer = setTimeout(notifyReady, 100);

        return () => {
            clearTimeout(timer);
            delete (window as any).ReactAppBridge;
        };
    }, [navigate]);
}
```

### 4.2 竞态条件处理

```typescript
// 处理导航和路由更新的竞态条件

class NavigationRaceConditionHandler {
    private pendingNavigations: Map<string, number> = new Map();
    private navigationInFlight = false;

    async navigateWithWait(path: string, timeout = 2000): Promise<boolean> {
        if (this.navigationInFlight) {
            // 已有导航进行中，等待
            return new Promise((resolve) => {
                const checkInterval = setInterval(() => {
                    if (!this.navigationInFlight) {
                        clearInterval(checkInterval);
                        resolve(true);
                    }
                }, 50);

                setTimeout(() => {
                    clearInterval(checkInterval);
                    resolve(false);
                }, timeout);
            });
        }

        this.navigationInFlight = true;
        this.pendingNavigations.set(path, Date.now());

        try {
            // 执行导航
            await new Promise(resolve => setTimeout(resolve, 0));

            return true;
        } finally {
            this.navigationInFlight = false;
            this.pendingNavigations.delete(path);
        }
    }
}
```

---

## 5. 浏览器历史记录管理

### 5.1 问题分析

**问题场景：**
```
1. 用户点击侧边栏 → 导航到 /releases
   历史: [/, /releases]

2. React路由更新，反向通知主应用
   主应用: emit('spa:navigate', ...)

3. 主应用的 loadContent() 也会执行，可能导致pushState两次
   历史: [/, /releases, /releases, /releases]
```

### 5.2 解决方案：统一历史管理

```typescript
// React应用不直接更改浏览器历史，由主应用统一管理

// src/main/frontend/react-app/bridge/useHistoryBridge.ts

export function useHistoryBridge() {
    const location = useLocation();
    const navigate = useNavigate();
    const lastPathRef = useRef(location.pathname);

    useEffect(() => {
        if (location.pathname === lastPathRef.current) {
            return;
        }

        const currentPath = location.pathname;
        const previousPath = lastPathRef.current;

        console.log('[History Bridge] React location changed from', previousPath, 'to', currentPath);

        // 不直接修改历史，而是通知主应用
        // 让主应用统一管理 history.pushState
        window.parent?.postMessage(
            {
                type: 'navigation:history-update',
                current: currentPath,
                previous: previousPath,
                timestamp: Date.now()
            },
            window.location.origin
        );

        lastPathRef.current = currentPath;
    }, [location.pathname]);
}

// 主应用侧处理历史更新
const historyBridgeHandler = (event) => {
    if (event.data?.type === 'navigation:history-update') {
        const { current, previous } = event.data;

        // 主应用统一处理历史
        if (history.state?.path !== current) {
            history.pushState({ path: current }, '', current);
            console.log('[Main App] Pushed history:', current);
        }
    }
};

window.addEventListener('message', historyBridgeHandler);
```

### 5.3 back/forward按钮处理

```typescript
// React应用监听popstate事件

export function usePopStateHandler() {
    const navigate = useNavigate();

    useEffect(() => {
        const handlePopState = (event: PopStateEvent) => {
            const targetPath = event.state?.path || '/';

            console.log('[React] Popstate detected, navigating to:', targetPath);

            // 使用 replace 避免再次推入历史
            navigate(targetPath, { replace: true });
        };

        window.addEventListener('popstate', handlePopState);

        return () => {
            window.removeEventListener('popstate', handlePopState);
        };
    }, [navigate]);
}
```

完整的历史管理流程：

```
User clicks back button
    ↓
Browser popstate event
    ↓
React: usePopStateHandler catches it
    ↓
React: navigate(targetPath, { replace: true })
    ↓
React: location.pathname changes
    ↓
React: useHistoryBridge sends 'navigation:history-update'
    ↓
Main App: receives message
    ↓
Main App: 检查 history.state 是否已更新
    ↓
Main App: 如果未更新，执行 history.replaceState (确保状态一致)
```

---

## 6. 考虑过的替代方案与缺点比较

### 6.1 方案对比表

| 方案 | 优点 | 缺点 | 适用场景 |
|-----|------|------|---------|
| **CustomEvent** | 事件驱动，易于扩展 | 需要手动管理事件监听器 | 同一文档中的通信 |
| **window.postMessage** | 支持跨域跨iframe | 消息序列化开销，难以调试 | iframe通信 |
| **全局函数** | 最简单，直接调用 | 污染全局作用域，难以管理 | 快速原型开发 |
| **URL Query参数** | 自动与浏览器历史同步 | 代码复杂，导航延迟 | 需要深层链接支持 |
| **LocalStorage** | 易于多页签通信 | 异步，事件不可靠 | 跨标签页同步 |
| **发布-订阅模式** | 解耦，扩展性强，结构清晰 | 需要额外的中介层 | 本推荐方案 |

### 6.2 详细方案分析

#### 6.2.1 CustomEvent方案

```javascript
// 实现
const navEvent = new CustomEvent('deploy-platform:navigate', {
    detail: { path: '/releases' }
});
window.dispatchEvent(navEvent);

// 监听
window.addEventListener('deploy-platform:navigate', (e) => {
    console.log('Navigate to:', e.detail.path);
});
```

**缺点:**
- 仅支持同一文档 (不支持iframe)
- 事件名称容易冲突
- 难以实现请求-响应模式
- 无法传递函数类型数据

#### 6.2.2 window.postMessage方案

```javascript
// 主应用 → React (iframe)
reactFrame.contentWindow.postMessage(
    { type: 'navigate', path: '/releases' },
    targetOrigin
);

// React → 主应用
window.parent.postMessage(
    { type: 'navigated', path: '/releases' },
    window.location.origin
);
```

**缺点:**
- 需要处理序列化/反序列化
- 跨域安全考虑复杂
- 调试困难 (消息可能被拦截)
- 不支持双向通信的事务性 (A→B, B→A可能不同步)

#### 6.2.3 全局函数方案

```typescript
// 主应用
(window as any).reactNavigate = (path: string) => {
    window.ReactAppBridge?.navigate(path);
};

// React应用
(window as any).mainAppUpdateNavigation = (path: string) => {
    NavigationBridge.updateMainAppNavigation(path);
};
```

**缺点:**
- 全局命名空间污染
- 没有结构化的事件管理
- 难以追踪事件流
- 容易产生循环调用

#### 6.2.4 URL Query参数方案

```typescript
// 导航时：/deploy-platform?route=releases&sync=true
// React监听query参数变化，自动导航
// 浏览器历史自动同步

export function useQueryParamNavigation() {
    const [params] = useSearchParams();
    const navigate = useNavigate();

    useEffect(() => {
        const route = params.get('route');
        if (route) {
            navigate(`/${route}`);
        }
    }, [params, navigate]);
}
```

**缺点:**
- 导航路径变复杂
- 主应用需要不断修改URL
- React路由改变时，URL会反复闪烁
- 深层链接难以维护

#### 6.2.5 LocalStorage方案

```typescript
// 主应用
localStorage.setItem('deploy-platform:pending-nav', JSON.stringify({
    path: '/releases',
    timestamp: Date.now()
}));

// React应用监听
window.addEventListener('storage', (e) => {
    if (e.key === 'deploy-platform:pending-nav') {
        const data = JSON.parse(e.newValue || '{}');
        navigate(data.path);
    }
});
```

**缺点:**
- 仅支持同源跨标签页通信
- 事件可靠性低 (部分浏览器可能不触发)
- 不适合高频导航
- 需要额外的清理逻辑

### 6.3 方案选择理由

**我们推荐发布-订阅 + CustomEvent + postMessage混合方案的原因：**

1. **结构清晰**：所有通信通过统一的通道
2. **易于调试**：可以在 `shouldProcess` 处设置断点
3. **防循环触发**：内置的 `isSyncing` 和事件源追踪
4. **支持多种部署场景**：
   - React在同一文档：使用 `window.ReactAppBridge.navigate()`
   - React在iframe：使用 `postMessage()`
5. **扩展性强**：可以轻松添加新的导航事件类型
6. **与现有代码兼容**：项目已有类似的事件系统

---

## 7. 完整实现指南

### 7.1 文件结构

```
src/main/frontend/react-app/
├── bridge/
│   ├── navigationBridge.ts        # React侧桥接器
│   ├── useNavigationBridge.ts     # React Hook
│   ├── useHistoryBridge.ts        # 历史管理Hook
│   └── usePopStateHandler.ts      # Back按钮处理
├── types/
│   └── navigation.ts               # 类型定义
└── ...

src/main/resources/templates/
├── main-layout.html               # 主应用 (已有)
└── fragments/
    └── navigation-bridge.html      # 导航桥接脚本片段
```

### 7.2 实现步骤

#### 步骤1: 创建React侧桥接器

```typescript
// src/main/frontend/react-app/bridge/types.ts

export interface NavigationEvent {
    type: 'app' | 'react' | 'system';
    path: string;
    timestamp: number;
    metadata?: Record<string, unknown>;
}

export interface NavigationBridgeState {
    isSyncing: boolean;
    lastEventSource: 'app' | 'react' | null;
    currentPath: string;
}

export type NavigationListener = (event: NavigationEvent) => void;
```

```typescript
// src/main/frontend/react-app/bridge/navigationBridge.ts

import type { NavigationEvent, NavigationBridgeState, NavigationListener } from './types';

export class NavigationBridge {
    private state: NavigationBridgeState = {
        isSyncing: false,
        lastEventSource: null,
        currentPath: '/'
    };

    private listeners: Set<NavigationListener> = new Set();
    private syncTimeout: ReturnType<typeof setTimeout> | null = null;

    /**
     * 检查是否应该处理事件
     */
    shouldProcess(eventSource: 'app' | 'react', path: string): boolean {
        if (this.state.isSyncing) {
            console.log('[NavigationBridge] Sync in progress, ignoring event');
            return false;
        }

        if (path === this.state.currentPath) {
            console.log('[NavigationBridge] Same path, ignoring event');
            return false;
        }

        if (this.state.lastEventSource === eventSource) {
            console.log('[NavigationBridge] Same event source, ignoring event');
            return false;
        }

        return true;
    }

    /**
     * 标记同步开始
     */
    beginSync(eventSource: 'app' | 'react', path: string): void {
        this.state.isSyncing = true;
        this.state.lastEventSource = eventSource;
        this.state.currentPath = path;

        if (this.syncTimeout) {
            clearTimeout(this.syncTimeout);
        }

        this.syncTimeout = setTimeout(() => {
            this.state.isSyncing = false;
            this.state.lastEventSource = null;
            console.log('[NavigationBridge] Sync timeout, reset state');
        }, 300);
    }

    /**
     * 标记同步结束
     */
    endSync(): void {
        if (this.syncTimeout) {
            clearTimeout(this.syncTimeout);
        }
        this.state.isSyncing = false;
    }

    /**
     * 添加事件监听器
     */
    subscribe(listener: NavigationListener): () => void {
        this.listeners.add(listener);

        return () => {
            this.listeners.delete(listener);
        };
    }

    /**
     * 通知所有监听器
     */
    private notify(event: NavigationEvent): void {
        this.listeners.forEach(listener => {
            try {
                listener(event);
            } catch (error) {
                console.error('[NavigationBridge] Listener error:', error);
            }
        });
    }

    /**
     * 从主应用发起导航
     */
    navigateFromApp(path: string): void {
        if (!this.shouldProcess('app', path)) {
            return;
        }

        this.beginSync('app', path);

        const event: NavigationEvent = {
            type: 'app',
            path,
            timestamp: Date.now()
        };

        this.notify(event);

        // 不要立即调用endSync，让Hook来处理
    }

    /**
     * 从React应用发起导航
     */
    navigateFromReact(path: string): void {
        if (!this.shouldProcess('react', path)) {
            return;
        }

        this.beginSync('react', path);

        const event: NavigationEvent = {
            type: 'react',
            path,
            timestamp: Date.now()
        };

        this.notify(event);
    }

    /**
     * 获取当前状态
     */
    getState(): Readonly<NavigationBridgeState> {
        return { ...this.state };
    }
}

export const navigationBridge = new NavigationBridge();
```

#### 步骤2: 创建React Hook

```typescript
// src/main/frontend/react-app/bridge/useNavigationBridge.ts

import { useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { navigationBridge } from './navigationBridge';

export function useNavigationBridge() {
    const navigate = useNavigate();
    const location = useLocation();
    const isSyncingRef = useRef(false);
    const lastPathRef = useRef(location.pathname);

    // 暴露导航接口给主应用
    useEffect(() => {
        const reactAppBridge = {
            navigate: (path: string) => {
                console.log('[React Navigation] Navigating to:', path);
                isSyncingRef.current = true;
                navigate(path, { replace: false });
            }
        };

        (window as any).ReactAppBridge = reactAppBridge;

        // 通知主应用React已就绪
        const notifyReady = () => {
            window.parent?.postMessage(
                { type: 'deploy-platform:ready' },
                window.location.origin
            );
        };

        notifyReady();
        const timer = setTimeout(notifyReady, 100);

        return () => {
            clearTimeout(timer);
            delete (window as any).ReactAppBridge;
        };
    }, [navigate]);

    // 监听导航事件
    useEffect(() => {
        const unsubscribe = navigationBridge.subscribe((event) => {
            if (event.type === 'app') {
                console.log('[React] Received navigation event from app:', event.path);
                isSyncingRef.current = true;
                navigate(event.path, { replace: false });
            }
        });

        return unsubscribe;
    }, [navigate]);

    // 监听位置变化
    useEffect(() => {
        if (isSyncingRef.current) {
            // 这次变化是由React Bridge触发的，标记为完成
            isSyncingRef.current = false;
            navigationBridge.endSync();
            lastPathRef.current = location.pathname;
            return;
        }

        if (location.pathname === lastPathRef.current) {
            return;
        }

        console.log('[React] Location changed to:', location.pathname);

        navigationBridge.navigateFromReact(location.pathname);

        // 通知主应用
        window.parent?.postMessage(
            {
                type: 'navigation:from-react',
                path: location.pathname,
                timestamp: Date.now()
            },
            window.location.origin
        );

        lastPathRef.current = location.pathname;
    }, [location.pathname]);
}
```

#### 步骤3: 在ShellLayout中使用

```typescript
// src/main/frontend/react-app/layout/ShellLayout.tsx

import { useNavigationBridge } from '../bridge/useNavigationBridge';
import { usePopStateHandler } from '../bridge/usePopStateHandler';

export function ShellLayout({ children }: PropsWithChildren): JSX.Element {
    const { t } = useTranslation();

    // 初始化导航桥接
    useNavigationBridge();
    usePopStateHandler();

    // ... 其余代码
}
```

#### 步骤4: 更新主应用

```html
<!-- src/main/resources/templates/main-layout.html -->

<script>
    // ==================== 导航桥接器 ====================
    const NavigationBridge = (() => {
        const REACT_READY_EVENT = 'deploy-platform:ready';
        const SYNC_TIMEOUT = 300;

        let isReactReady = false;
        let isSyncing = false;
        let syncTimer = null;
        let lastPath = '';
        let lastEventSource = null;

        // 监听React就绪
        window.addEventListener('message', (event) => {
            if (event.data?.type === REACT_READY_EVENT) {
                isReactReady = true;
                console.log('[Navigation Bridge] React app ready');
                // 处理待处理的导航
                if (pendingNavigationPath) {
                    NavigationBridge.notifyReactNavigation(pendingNavigationPath);
                    pendingNavigationPath = null;
                }
            }
        });

        // 监听来自React的导航事件
        window.addEventListener('message', (event) => {
            if (event.data?.type === 'navigation:from-react') {
                const { path } = event.data;

                if (isSyncing) {
                    console.log('[Navigation Bridge] Sync in progress, ignoring React nav');
                    return;
                }

                if (path === lastPath && lastEventSource === 'app') {
                    console.log('[Navigation Bridge] Same path from app, ignoring');
                    return;
                }

                console.log('[Navigation Bridge] React -> Main app navigation:', path);
                // React应用已更新，不需要再次加载内容
                // 只需更新侧边栏状态
                updateSidebarNavigation(path);
            }
        });

        return {
            notifyReactNavigation(path) {
                if (!isReactReady) {
                    console.log('[Navigation Bridge] React not ready, queueing navigation to:', path);
                    pendingNavigationPath = path;
                    return;
                }

                if (isSyncing) {
                    console.log('[Navigation Bridge] Sync in progress, ignoring');
                    return;
                }

                if (path === lastPath && lastEventSource === 'react') {
                    console.log('[Navigation Bridge] Same path from React, ignoring');
                    return;
                }

                isSyncing = true;
                lastPath = path;
                lastEventSource = 'app';

                console.log('[Navigation Bridge] Main app -> React navigation:', path);

                if (window.ReactAppBridge?.navigate) {
                    window.ReactAppBridge.navigate(path);
                }

                if (syncTimer) {
                    clearTimeout(syncTimer);
                }
                syncTimer = setTimeout(() => {
                    isSyncing = false;
                    console.log('[Navigation Bridge] Sync timeout, reset');
                }, SYNC_TIMEOUT);
            }
        };
    })();

    let pendingNavigationPath = null;

    function updateSidebarNavigation(path) {
        const links = document.querySelectorAll('[data-role="sidebar"] a[href]');
        links.forEach(link => {
            const href = link.getAttribute('href');
            link.classList.toggle('active', href === path);
        });
    }

    // 初始化导航桥接
    function initNavigationBridge() {
        const sidebarLinks = document.querySelectorAll('[data-role="sidebar"] a[data-route]');

        sidebarLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                // 注意：不要阻止默认行为，让React Router处理
                const route = link.dataset.route;

                // 路径映射
                const pathMap = {
                    'overview': '/overview',
                    'releases': '/releases',
                    'policies': '/settings/policies'
                };

                const path = pathMap[route] || '/' + route;
                NavigationBridge.notifyReactNavigation(path);
            });
        });
    }

    document.addEventListener('DOMContentLoaded', initNavigationBridge);
</script>
```

### 7.3 测试检查清单

```markdown
## 导航同步测试清单

### 基础导航测试
- [ ] 点击主应用侧边栏，React应用正确导航
- [ ] React应用内导航，主应用侧边栏正确高亮
- [ ] 导航不会创建循环事件
- [ ] 导航不会出现重复的浏览器历史记录

### 时序测试
- [ ] 主应用先初始化，React后加载，导航正确
- [ ] React应用导航请求在React挂载前，自动排队
- [ ] 快速连续导航，不会出现乱序或丢失

### 浏览器控制测试
- [ ] 点击浏览器back按钮，应用正确返回上一页
- [ ] 点击浏览器forward按钮，应用正确前进
- [ ] 浏览器历史记录不包含重复的路径
- [ ] 刷新页面，路由状态正确保留

### 边界条件测试
- [ ] 相同路径导航多次，不会出现重复事件
- [ ] 快速从A → B → A导航，无循环触发
- [ ] React未加载时点击侧边栏，不会报错
- [ ] 网络延迟情况下，导航不会乱序

### 性能测试
- [ ] 导航延迟 < 100ms
- [ ] 连续导航10次，无内存泄漏
- [ ] 事件监听器正确清理，无重复绑定

### 浏览器兼容性测试
- [ ] Chrome/Edge 最新版本
- [ ] Firefox 最新版本
- [ ] Safari 最新版本
```

---

## 8. 监控与调试

### 8.1 添加日志记录

```typescript
// src/main/frontend/react-app/bridge/navigationBridge.ts

export class NavigationBridgeWithLogging extends NavigationBridge {
    private logs: Array<{
        timestamp: number;
        type: string;
        message: string;
        data?: unknown;
    }> = [];

    private logEvent(type: string, message: string, data?: unknown) {
        const entry = {
            timestamp: Date.now(),
            type,
            message,
            data
        };

        this.logs.push(entry);

        // 保持日志不超过1000条
        if (this.logs.length > 1000) {
            this.logs.shift();
        }

        console.log(`[NavigationBridge ${type}] ${message}`, data);
    }

    shouldProcess(eventSource: 'app' | 'react', path: string): boolean {
        const result = super.shouldProcess(eventSource, path);

        if (!result) {
            this.logEvent('SKIP', `Skipped event from ${eventSource}`, { path, reason: this.getState() });
        }

        return result;
    }

    getLogs() {
        return [...this.logs];
    }

    exportLogsAsJson() {
        return JSON.stringify(this.logs, null, 2);
    }
}
```

### 8.2 浏览器DevTools集成

```typescript
// 在控制台中可用的调试工具
(window as any).__navigationBridgeDebug = {
    getLogs: () => navigationBridge.getLogs(),
    exportLogs: () => navigationBridge.exportLogsAsJson(),
    getState: () => navigationBridge.getState(),
    simulateMainAppNav: (path: string) => navigationBridge.navigateFromApp(path),
    simulateReactNav: (path: string) => navigationBridge.navigateFromReact(path)
};
```

使用：
```javascript
// 在浏览器控制台中
__navigationBridgeDebug.getLogs()        // 查看所有导航事件日志
__navigationBridgeDebug.getState()      // 查看当前同步状态
__navigationBridgeDebug.exportLogs()    // 导出日志用于分析
```

---

## 9. 常见问题与解决方案

### Q1: React路由更新时，侧边栏没有高亮

**原因：** useNavigationBridge Hook未在ShellLayout中调用

```typescript
// 检查 ShellLayout.tsx
export function ShellLayout({ children }: PropsWithChildren): JSX.Element {
    // 需要添加这行
    useNavigationBridge();

    // ...
}
```

### Q2: 导航很卡，延迟明显

**原因：** sync时间过长，或者有其他重操作

```typescript
// 优化：减少sync时间
beginSync(eventSource: 'app' | 'react', path: string) {
    // ...
    this.syncTimeout = setTimeout(() => {
        this.state.isSyncing = false;
        this.state.lastEventSource = null;
    }, 100); // 改为100ms而不是300ms
}
```

### Q3: 浏览器历史记录重复

**原因：** 主应用和React都在pushState

```typescript
// 解决：让React只负责记录location变化，由主应用统一管理历史
useEffect(() => {
    // React侧：仅通知，不修改历史
    if (location.pathname !== lastPathRef.current) {
        window.parent?.postMessage({
            type: 'navigation:notify',
            path: location.pathname
        }, window.location.origin);
    }
}, [location.pathname]);
```

### Q4: 无法在iframe中通信

**原因：** postMessage的targetOrigin设置错误

```typescript
// 正确的做法
window.parent?.postMessage(
    { type: 'deploy-platform:ready' },
    window.location.origin  // 这里应该是父窗口的origin
);

// 如果不知道父窗口origin，可以用'*'但这不安全
// 更好的做法是在parent中发送ready signal
const parent = window.parent;
if (parent !== window) {
    parent.postMessage({ type: 'child-ready' }, '*');
    // parent会回复：
    // window.addEventListener('message', (e) => {
    //     if (e.data.type === 'ready-ack') {
    //         // 现在知道了parent的origin，存储起来
    //     }
    // });
}
```

---

## 10. 性能优化建议

### 10.1 防止不必要的重新渲染

```typescript
// 使用useCallback避免回调函数重新创建
const handleNavigate = useCallback((path: string) => {
    navigationBridge.navigateFromApp(path);
}, []);
```

### 10.2 事件监听器清理

```typescript
// 确保在组件卸载时清理
useEffect(() => {
    const unsubscribe = navigationBridge.subscribe(listener);

    return () => {
        unsubscribe(); // 重要！
    };
}, []);
```

### 10.3 防止过度同步

```typescript
// 使用防抖处理快速连续导航
function useNavigationWithDebounce(delay = 100) {
    const debounceTimer = useRef<number | null>(null);

    const navigate = (path: string) => {
        if (debounceTimer.current) {
            clearTimeout(debounceTimer.current);
        }

        debounceTimer.current = window.setTimeout(() => {
            navigationBridge.navigateFromApp(path);
        }, delay);
    };

    useEffect(() => {
        return () => {
            if (debounceTimer.current) {
                clearTimeout(debounceTimer.current);
            }
        };
    }, []);

    return navigate;
}
```

---

## 总结

### 推荐的最终方案：

1. **通信机制**：发布-订阅模式 + CustomEvent + postMessage混合
2. **防循环触发**：`isSyncing`标志 + 事件源追踪 + 路径检查
3. **时序处理**：导航队列 + React就绪信号
4. **历史管理**：主应用统一管理，React仅通知
5. **监控调试**：结构化日志 + DevTools集成

### 关键文件清单：

- `src/main/frontend/react-app/bridge/navigationBridge.ts` - 核心桥接器
- `src/main/frontend/react-app/bridge/useNavigationBridge.ts` - React Hook
- `src/main/frontend/react-app/layout/ShellLayout.tsx` - 集成点
- `src/main/resources/templates/main-layout.html` - 主应用脚本

### 预期效果：

- ✅ 点击侧边栏 → React应用正确导航
- ✅ React导航 → 侧边栏自动高亮
- ✅ 无循环触发，无重复历史
- ✅ 浏览器back/forward正常工作
- ✅ 支持React延迟加载场景
