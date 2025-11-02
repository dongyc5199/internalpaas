# 导航同步实现清单与快速参考

## 1. 快速开始 (15分钟集成)

### 1.1 最小化实现

如果你需要快速实现而不需要所有高级特性，使用这个最小化版本：

```typescript
// src/main/frontend/react-app/bridge/navigationBridge.min.ts

// React侧最小实现
export function useNavigationBridgeMin() {
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        // 暴露导航接口给主应用
        (window as any).ReactAppBridge = {
            navigate: (path: string) => navigate(path)
        };

        // 通知主应用就绪
        window.parent?.postMessage(
            { type: 'deploy-platform:ready' },
            window.location.origin
        );

        return () => {
            delete (window as any).ReactAppBridge;
        };
    }, [navigate]);

    // React导航时通知主应用
    useEffect(() => {
        window.parent?.postMessage(
            {
                type: 'navigation:from-react',
                path: location.pathname
            },
            window.location.origin
        );
    }, [location.pathname]);
}
```

```javascript
// src/main/resources/templates/main-layout.html中添加

// Main App侧最小实现
let isReactReady = false;

window.addEventListener('message', (e) => {
    if (e.data?.type === 'deploy-platform:ready') {
        isReactReady = true;
    }
    if (e.data?.type === 'navigation:from-react') {
        // React已处理导航，仅更新侧边栏
        updateSidebarActive(e.data.path);
    }
});

// 侧边栏点击处理
document.addEventListener('click', (e) => {
    const link = e.target.closest('[data-route]');
    if (link && isReactReady) {
        const route = link.dataset.route;
        const path = routeToPath(route);
        window.ReactAppBridge?.navigate(path);
    }
});

function routeToPath(route) {
    return {
        'overview': '/overview',
        'releases': '/releases',
        'policies': '/settings/policies'
    }[route] || '/' + route;
}

function updateSidebarActive(path) {
    document.querySelectorAll('[data-route]').forEach(link => {
        const route = link.dataset.route;
        const isActive = routeToPath(route) === path;
        link.classList.toggle('active', isActive);
    });
}
```

---

## 2. 完整实现检查清单

### 阶段1：准备工作 (1小时)

- [ ] **2.1 创建文件结构**
  ```bash
  mkdir -p src/main/frontend/react-app/bridge
  mkdir -p docs/guides/navigation
  ```

- [ ] **2.2 创建TypeScript类型文件**
  - [ ] `src/main/frontend/react-app/bridge/types.ts`
  - [ ] 定义 `NavigationEvent` 接口
  - [ ] 定义 `NavigationBridgeState` 接口
  - [ ] 定义 `NavigationListener` 类型

- [ ] **2.3 验证环境**
  - [ ] 检查 React 18+ (for useCallback, useRef)
  - [ ] 检查 React Router v6+ (for useLocation, useNavigate)
  - [ ] 检查 TypeScript 4.5+ (for as const assertions)

### 阶段2：React侧实现 (2小时)

- [ ] **2.4 创建NavigationBridge类**
  - [ ] `src/main/frontend/react-app/bridge/navigationBridge.ts`
  - 功能:
    - [ ] `shouldProcess()` - 检查是否应处理
    - [ ] `beginSync()` - 开始同步
    - [ ] `endSync()` - 结束同步
    - [ ] `subscribe()` - 添加监听器
    - [ ] `navigateFromApp()` - App触发的导航
    - [ ] `navigateFromReact()` - React触发的导航
    - [ ] `getState()` - 获取当前状态

- [ ] **2.5 创建useNavigationBridge Hook**
  - [ ] `src/main/frontend/react-app/bridge/useNavigationBridge.ts`
  - [ ] 暴露 `ReactAppBridge` 到 window
  - [ ] 发送 'deploy-platform:ready' 消息
  - [ ] 监听来自App的导航事件
  - [ ] 通知App路由改变

- [ ] **2.6 创建usePopStateHandler Hook**
  - [ ] `src/main/frontend/react-app/bridge/usePopStateHandler.ts`
  - [ ] 监听 popstate 事件
  - [ ] 使用 replace: true 避免重复历史

- [ ] **2.7 创建useHistoryBridge Hook**
  - [ ] `src/main/frontend/react-app/bridge/useHistoryBridge.ts`
  - [ ] 追踪location变化
  - [ ] 发送历史更新通知

- [ ] **2.8 在ShellLayout中集成**
  - [ ] 导入 `useNavigationBridge`
  - [ ] 导入 `usePopStateHandler`
  - [ ] 在组件中调用这些Hook
  - [ ] 验证侧边栏导航项正确使用 NavLink

### 阶段3：主应用侧实现 (2小时)

- [ ] **2.9 添加NavigationBridge脚本**
  - [ ] 在 `src/main/resources/templates/main-layout.html` 中添加
  - 功能:
    - [ ] `isReactReady` 状态管理
    - [ ] `isSyncing` 标志
    - [ ] `lastPath` 追踪
    - [ ] `lastEventSource` 追踪

- [ ] **2.10 实现事件监听**
  - [ ] 监听 'deploy-platform:ready' 消息
  - [ ] 监听 'navigation:from-react' 消息
  - [ ] 处理待处理的导航队列

- [ ] **2.11 实现侧边栏点击处理**
  - [ ] 为侧边栏链接添加 click 事件监听
  - [ ] 检查 isReactReady
  - [ ] 调用 `window.ReactAppBridge?.navigate()`
  - [ ] 处理未就绪的情况 (加入队列)

- [ ] **2.12 实现侧边栏高亮更新**
  - [ ] 创建 `updateSidebarNavigation(path)` 函数
  - [ ] 更新侧边栏链接的 active 类
  - [ ] 处理路径到路由的映射

### 阶段4：测试与调试 (2小时)

- [ ] **2.13 单元测试**
  - [ ] `NavigationBridge.shouldProcess()` 测试
  - [ ] `navigateFromApp()` 和 `navigateFromReact()` 测试
  - [ ] 事件循环防护测试
  - [ ] 同步超时测试

- [ ] **2.14 集成测试**
  - [ ] 侧边栏点击导航测试
  - [ ] React内导航反向同步测试
  - [ ] 快速连续导航测试
  - [ ] React未加载时的队列测试

- [ ] **2.15 浏览器测试**
  - [ ] Chrome/Edge 测试
  - [ ] Firefox 测试
  - [ ] Safari 测试
  - [ ] 移动浏览器 (如适用)

- [ ] **2.16 添加调试工具**
  - [ ] 创建 `__navigationBridgeDebug` 全局对象
  - [ ] `getLogs()` - 查看事件日志
  - [ ] `getState()` - 查看同步状态
  - [ ] `simulateMainAppNav()` - 模拟主应用导航
  - [ ] `simulateReactNav()` - 模拟React导航

### 阶段5：文档与维护 (1小时)

- [ ] **2.17 文档**
  - [ ] 更新项目README
  - [ ] 添加架构图
  - [ ] 添加故障排除指南
  - [ ] 记录已知限制

- [ ] **2.18 监控**
  - [ ] 添加性能指标收集
  - [ ] 添加错误日志
  - [ ] 添加用户行为追踪 (可选)

- [ ] **2.19 代码审查检查清单**
  - [ ] 没有全局作用域污染
  - [ ] 所有事件监听器都正确清理
  - [ ] TypeScript 类型正确性
  - [ ] 错误处理完整

---

## 3. 故障排除指南

### 问题3.1：侧边栏点击后React没有导航

**症状：** 点击侧边栏链接，URL不改变，React路由不更新

**诊断步骤：**
```javascript
// 在浏览器控制台执行
console.log('1. React App存在?', typeof window.ReactAppBridge)
console.log('2. React App ready?', window.ReactAppBridge?.navigate !== undefined)
console.log('3. Navigation Bridge state:', window.__navigationBridgeDebug?.getState())
console.log('4. Recent logs:', window.__navigationBridgeDebug?.getLogs()?.slice(-5))
```

**常见原因与解决：**

| 原因 | 症状 | 解决 |
|-----|------|------|
| React未就绪 | React 加载中时点击导航 | 检查useNavigationBridge Hook是否在ShellLayout中调用 |
| 侧边栏链接没有data-route | 点击无反应 | 添加 `data-route="overview"` 属性 |
| React Router未正确配置 | 导航触发但UI不更新 | 检查Route和路径是否匹配 |
| 防循环逻辑过严 | 导航直接被忽略 | 检查shouldProcess()中的条件 |

**修复清单：**
```typescript
// ✓ 确保ShellLayout中有这行
useNavigationBridge();

// ✓ 确保侧边栏链接正确
<a data-route="overview" href="/overview">Overview</a>

// ✓ 确保ReactAppBridge暴露
(window as any).ReactAppBridge = { navigate: ... }

// ✓ 确保ready消息发送
window.parent?.postMessage({ type: 'deploy-platform:ready' }, ...)
```

---

### 问题3.2：导航循环 (A→B→A→B...)

**症状：** 观察到重复的导航事件，浏览器历史重复

**诊断步骤：**
```javascript
// 获取最后的10个事件
const logs = __navigationBridgeDebug.getLogs().slice(-10)

// 查看事件源模式
logs.forEach(log => {
    console.log(`${log.timestamp}: ${log.type} - ${log.message}`)
})

// 查看同步状态
console.log(__navigationBridgeDebug.getState())
```

**原因分析：**

```
✗ 错误模式:
main → React
React → main (通知)
main → React (再次通知)  ← 循环！

✓ 正确模式:
main → React
React → main (仅通知，不导航)
React重置标志，等待下次用户操作
```

**修复：**
```typescript
// 确保useNavigationBridge中的这段代码
useEffect(() => {
    if (isSyncingRef.current) {
        // 这是由Bridge触发的，重置标志
        isSyncingRef.current = false;
        navigationBridge.endSync();
        return;  // ← 关键：不向主应用发送通知
    }
    // 只有用户直接操作才会到这里
    navigationBridge.navigateFromReact(location.pathname);
}, [location.pathname]);
```

---

### 问题3.3：浏览器Back按钮不工作

**症状：** 点击浏览器后退按钮，URL改变但React页面不更新

**诊断步骤：**
```javascript
// 检查popstate监听
window.addEventListener('popstate', (e) => {
    console.log('Popstate triggered:', e.state)
})

// 手动触发返回
history.back()

// 检查React是否响应
console.log('Current React path:', window.location.pathname)
```

**常见原因：**

| 原因 | 解决 |
|-----|------|
| usePopStateHandler未调用 | 在ShellLayout中添加 `usePopStateHandler()` |
| React Router未同步状态 | 确保useNavigate正确使用 |
| history.state设置错误 | 检查pushState和replaceState的state对象 |

**修复：**
```typescript
// 确保ShellLayout中有
usePopStateHandler();

// 确保usePopStateHandler实现正确
export function usePopStateHandler() {
    const navigate = useNavigate();

    useEffect(() => {
        const handlePopState = (event: PopStateEvent) => {
            const targetPath = event.state?.path || '/';
            navigate(targetPath, { replace: true });  // 使用replace避免重复历史
        };

        window.addEventListener('popstate', handlePopState);
        return () => window.removeEventListener('popstate', handlePopState);
    }, [navigate]);
}
```

---

### 问题3.4：React加载延迟导致导航丢失

**症状：** 在React完全加载前点击导航，导航被忽略，React加载后无法恢复到该页面

**诊断步骤：**
```javascript
// 检查待处理导航队列
console.log('Pending navigations:', window.__navigationBridgeDebug?.getLogs()
    .filter(l => l.message.includes('Queue')))

// 检查React是否收到了导航
console.log('React mounted?', document.getElementById('deploy-platform-root')?.hasAttribute('data-react-mounted'))
```

**解决方案：**

```javascript
// 在main-layout.html中实现导航队列
let navigationQueue = [];
let isReactReady = false;

window.addEventListener('message', (e) => {
    if (e.data?.type === 'deploy-platform:ready') {
        isReactReady = true;
        // 处理队列中的导航
        while (navigationQueue.length > 0) {
            const path = navigationQueue.shift();
            window.ReactAppBridge?.navigate(path);
        }
    }
});

function navigateWithQueue(path) {
    if (isReactReady) {
        window.ReactAppBridge?.navigate(path);
    } else {
        navigationQueue.push(path);
        console.log('React not ready, queued navigation to:', path);
    }
}
```

---

### 问题3.5：侧边栏高亮不同步

**症状：** React应用导航成功，URL改变，但侧边栏链接没有高亮正确的项目

**诊断步骤：**
```javascript
// 检查DOM中的侧边栏
const sidebarLinks = document.querySelectorAll('[data-role="sidebar"] a');
sidebarLinks.forEach(link => {
    console.log(`${link.textContent}: active=${link.classList.contains('active')}`)
})

// 检查当前URL
console.log('Current URL:', window.location.pathname)

// 手动更新看是否有效
updateSidebarNavigation('/overview')
```

**常见原因：**

| 原因 | 解决 |
|-----|------|
| CSS类名错误 | 检查 `.active` 类是否定义，以及路径映射是否正确 |
| React通知未到达主应用 | 检查 'navigation:from-react' 消息是否发送 |
| 路径映射不一致 | 确保路径映射在两边一致 |

**修复：**
```javascript
// 确保路径映射正确且一致
const pathToRouteMap = {
    '/overview': 'overview',
    '/releases': 'releases',
    '/settings/policies': 'policies'
};

const routeToPathMap = {
    'overview': '/overview',
    'releases': '/releases',
    'policies': '/settings/policies'
};

// 确保updateSidebarNavigation正确实现
function updateSidebarNavigation(currentPath) {
    document.querySelectorAll('[data-role="sidebar"] a[data-route]').forEach(link => {
        const route = link.dataset.route;
        const expectedPath = routeToPathMap[route];
        const isActive = expectedPath === currentPath;
        link.classList.toggle('active', isActive);
    });
}

// 并在接收'navigation:from-react'消息时调用
window.addEventListener('message', (e) => {
    if (e.data?.type === 'navigation:from-react') {
        updateSidebarNavigation(e.data.path);
    }
});
```

---

### 问题3.6：内存泄漏 (长时间使用后变慢)

**症状：** 导航多次后，浏览器变慢，内存占用不断增长

**诊断步骤：**
```javascript
// 检查事件监听器数量
console.log('Message listeners:',
    window.getEventListeners?.('message')?.length || 'unknown')

// 检查订阅者数量
console.log('Navigation subscribers:',
    __navigationBridgeDebug?.getState()?.subscriberCount || 'unknown')

// 在Chrome DevTools中：
// 1. 打开Memory标签
// 2. 进行若干导航操作
// 3. 拍摄堆快照
// 4. 比较两次快照，查看泄漏对象
```

**常见原因与修复：**

| 原因 | 修复 |
|-----|------|
| useEffect未清理 | 返回清理函数 `return () => { ... }` |
| 事件监听器未移除 | 在清理函数中调用 `removeEventListener` |
| 定时器未清除 | 在清理中 `clearTimeout` 或 `clearInterval` |
| 订阅未取消 | 保存unsubscribe函数并在清理中调用 |

**修复模板：**
```typescript
useEffect(() => {
    const unsubscribe = navigationBridge.subscribe(handler);

    const handleMessage = (e) => { /* ... */ };
    window.addEventListener('message', handleMessage);

    const timer = setTimeout(() => { /* ... */ }, 1000);

    // ✓ 清理函数必须清理所有资源
    return () => {
        unsubscribe();
        window.removeEventListener('message', handleMessage);
        clearTimeout(timer);
    };
}, []);
```

---

## 4. 性能优化检查清单

- [ ] **4.1 减少同步等待时间**
  - [ ] 将syncTimeout从300ms降至100ms (如果可靠)
  - [ ] 使用requestAnimationFrame加速UI更新

- [ ] **4.2 防止不必要的重新渲染**
  - [ ] 使用useCallback包装导航处理函数
  - [ ] 使用useMemo缓存navItems列表
  - [ ] 考虑使用React.memo包装NavLink

- [ ] **4.3 优化事件监听**
  - [ ] 使用事件委托而非为每个链接添加监听
  - [ ] 使用 { once: true } 对一次性事件
  - [ ] 及时清理所有监听器

- [ ] **4.4 监控关键指标**
  - [ ] 导航延迟 < 100ms
  - [ ] 内存增长 < 1MB/100次导航
  - [ ] CPU使用 < 10% (平均)

---

## 5. 代码质量检查清单

### TypeScript类型检查
- [ ] 没有使用 `any` 类型
- [ ] 所有函数都有明确的返回类型
- [ ] 接口定义完整且有文档注释
- [ ] 使用 `as const` 确保类型安全

### 错误处理
- [ ] 所有异步操作都有try/catch
- [ ] 消息事件验证origin和data
- [ ] 超时情况有降级处理
- [ ] 用户操作失败有错误提示

### 安全性
- [ ] postMessage验证origin
- [ ] 不传递敏感信息
- [ ] 验证message.data的结构
- [ ] 限制暴露给window的API

### 可维护性
- [ ] 代码有充分的注释
- [ ] 魔法数字提取为常量
- [ ] 函数单一职责
- [ ] 易于测试和扩展

---

## 6. 快速参考：关键代码片段

### 6.1 检查React是否就绪

```typescript
// ✓ 推荐方式
const isReactReady = typeof window.ReactAppBridge !== 'undefined' &&
                    typeof window.ReactAppBridge.navigate === 'function';

// ✗ 避免
if (window.ReactAppBridge) { // 可能为undefined或null
```

### 6.2 安全的postMessage

```typescript
// ✓ 推荐
window.parent?.postMessage(
    { type: 'navigation:ready', payload: {} },
    window.location.origin  // 限制origin
);

// ✗ 避免
window.top.postMessage(message, '*');  // 安全风险
```

### 6.3 正确的事件清理

```typescript
// ✓ 推荐
useEffect(() => {
    const handler = (e) => { /* ... */ };
    window.addEventListener('message', handler);

    return () => {
        window.removeEventListener('message', handler);  // 必须清理
    };
}, []);

// ✗ 避免
window.addEventListener('message', () => { /* ... */ });  // 无法清理，内存泄漏
```

### 6.4 防止循环导航

```typescript
// ✓ 推荐：检查事件源
if (this.state.lastEventSource === eventSource) {
    return;  // 忽略相同源的事件
}

// ✗ 避免：无法检测循环
navigationBridge.navigateFromApp(path);
navigationBridge.navigateFromReact(path);  // 可能循环
```

### 6.5 处理未初始化的React

```typescript
// ✓ 推荐：使用队列
const queue = [];
if (isReactReady) {
    navigate(path);
} else {
    queue.push(path);
}

// ✗ 避免：假设React已就绪
window.ReactAppBridge.navigate(path);  // 可能报错
```

---

## 7. 部署检查清单

在部署到生产环境前：

- [ ] **功能测试**
  - [ ] 所有导航路径都正常工作
  - [ ] 浏览器back/forward正常
  - [ ] React热加载正常
  - [ ] 刷新页面状态保留

- [ ] **性能测试**
  - [ ] 导航延迟可接受
  - [ ] 内存使用正常
  - [ ] CPU占用合理
  - [ ] 网络请求优化

- [ ] **兼容性测试**
  - [ ] 支持的浏览器都正常
  - [ ] 响应式设计正常
  - [ ] 移动设备正常 (如适用)

- [ ] **安全测试**
  - [ ] CSP策略允许
  - [ ] 没有XSS漏洞
  - [ ] 没有泄露敏感信息

- [ ] **监控部署**
  - [ ] 错误日志已设置
  - [ ] 性能指标已收集
  - [ ] 用户反馈机制就绪

---

## 8. 常见配置问题

### 问题：CSP策略阻止postMessage

```html
<!-- 错误的CSP设置 -->
<meta http-equiv="Content-Security-Policy"
      content="default-src 'self'">

<!-- ✓ 正确的设置允许postMessage -->
<meta http-equiv="Content-Security-Policy"
      content="default-src 'self'; child-src 'self'">
```

### 问题：CORS限制导致导航失败

```javascript
// ✓ 如果React在iframe中，需要same-origin
// src/main/resources/templates/main-layout.html
<iframe src="/deploy-platform"
        sandbox="allow-same-origin allow-scripts">
</iframe>

// ✗ 避免限制过多
<iframe src="https://external-domain.com/app" ...></iframe>
```

### 问题：React basename配置错误

```typescript
// ✓ 正确配置
const base = container?.dataset?.routerBase || '/';
<BrowserRouter basename={base}>
    <Routes>
        <Route path="/" element={<Navigate to="/overview" />} />
        <Route path="/overview" element={<OverviewPage />} />
    </Routes>
</BrowserRouter>

// ✗ 不要这样做
<BrowserRouter basename="/deploy-platform">
    <Route path="/deploy-platform/overview" .../>  // 路径重复
</BrowserRouter>
```

---

## 总结：实现优先级

**最小可行版本 (MVP)：** 30分钟
- 仅 useNavigationBridge Hook (React侧)
- 简单的侧边栏点击处理 (Main App侧)
- 无高级特性，无防循环

**生产可用版本 (v1.0)：** 1天
- 完整的NavigationBridge类
- 防循环触发机制
- 浏览器历史管理
- 基本测试

**企业级版本 (v2.0)：** 3天
- 完整的错误恢复
- 详细的日志记录
- 性能监控
- 完整的测试套件

建议从**生产可用版本**开始，然后根据实际需求选择升级。

