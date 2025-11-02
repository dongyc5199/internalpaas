# React嵌入/独立模式检测研究报告

## 1. 执行摘要

本文档研究如何检测React应用是否以**嵌入模式**（嵌入到Spring Boot Thymeleaf主应用的main-layout.html中）还是**独立模式**（直接通过URL访问）运行。

### 当前项目实现现状

**好消息**：项目已经具备了完整的嵌入模式基础设施：
- React应用通过Vite构建为MFE（Module Federation Entry）
- 通过`loadDeployPlatformMfe()`函数动态加载
- 已有数据属性机制传递配置信息
- Spring Boot模板已支持控制React挂载

**需要改进的地方**：
- React组件树无法感知当前运行模式
- `ShellLayout`在两种模式下都被强制渲染
- 无法根据运行模式动态选择layout

---

## 2. 当前架构分析

### 2.1 嵌入模式流程

```
Spring Boot (main-layout.html)
    ↓
main.ts (JavaScript主文件)
    ↓
loadDeployPlatformMfe()
    ↓
动态导入 /dist/assets/deploy-platform.js
    ↓
React App 挂载到 #deploy-platform-root
    ↓
App.tsx → BrowserRouter → AppRoutes
    ↓
ShellLayout (强制渲染)
```

### 2.2 关键代码文件位置

| 文件 | 用途 | 位置 |
|------|------|------|
| main-layout.html | Spring Boot主模板 | `src/main/resources/templates/main-layout.html` |
| deploy-platform-content.html | React应用容器片段 | `src/main/resources/templates/admin/deploy-platform-content.html` |
| main.tsx | React应用入口 | `src/main/frontend/react-app/main.tsx` |
| App.tsx | React应用根组件 | `src/main/frontend/react-app/App.tsx` |
| ShellLayout.tsx | 导航框架组件 | `src/main/frontend/react-app/layout/ShellLayout.tsx` |
| main.ts | 前端入口脚本 | `src/main/frontend/main.ts` |
| deploy-platform-loader.ts | MFE加载器 | `src/main/frontend/mfe/deploy-platform-loader.ts` |

### 2.3 当前挂载点配置

**在deploy-platform-content.html中**：
```html
<div id="deploy-platform-root"
     class="deploy-platform-host"
     data-auth-endpoint="/api/deploy-platform/token"
     data-summary-endpoint="/api/deploy-platform/dashboard/summary"
     data-i18n-zh="正在加载部署管理面板…"
     data-i18n-en="Loading deployment console...">
    Loading deployment console...
</div>
```

**可用的配置机制**：
- `dataset.*` 属性可传递任意配置
- 父容器的CSS类可指示环境
- `data-react-mounted` 属性用于追踪挂载状态

---

## 3. 推荐的检测方法

### 3.1 最佳实践：多层验证策略

使用**组合检测法**，结合多个信号进行可靠判断：

```typescript
// src/main/frontend/react-app/hooks/useEmbedMode.ts
import { useMemo } from 'react';

/**
 * 检测React应用运行模式
 * @returns true 表示嵌入模式，false 表示独立模式
 */
export function useIsEmbeddedMode(): boolean {
    return useMemo(() => {
        return detectEmbedMode();
    }, []);
}

function detectEmbedMode(): boolean {
    // 信号1：检查挂载点容器
    const container = document.getElementById('deploy-platform-root');
    if (!container) {
        return false; // 容器不存在 → 独立模式
    }

    // 信号2：检查父级结构（Spring Boot主模板的标志）
    const isInSpringBootTemplate = !!document.querySelector('[data-dev-shell-message]') ||
                                   !!document.querySelector('.content-framework');

    // 信号3：检查数据属性（可选的嵌入标记）
    const hasEmbedMarker = container.hasAttribute('data-embed-mode') &&
                          container.getAttribute('data-embed-mode') === 'true';

    // 信号4：检查自定义window属性（由Spring Boot模板设置）
    const hasExplicitFlag = (window as any).__DEPLOY_PLATFORM_EMBEDDED === true;

    // 综合判断：容器存在 + (Spring Boot模板标志 或 显式标记)
    return container && (isInSpringBootTemplate || hasEmbedMarker || hasExplicitFlag);
}
```

### 3.2 选择该方法的理由

#### 可靠性强 ✓
- **多信号交叉验证**：不依赖单一信号，降低误判概率
- **容器检查**：最基础的检查，可捕获100%的不存在情况
- **DOM结构验证**：检查Spring Boot主模板的特定DOM元素
- **显式标记兼容**：支持未来添加明确的嵌入模式标记

#### 扩展性好 ✓
- 可轻松添加新的检测信号
- 支持Spring Boot模板的配置变化
- 无需修改React应用核心逻辑

#### 性能高效 ✓
- 纯DOM查询，无网络请求
- 使用`useMemo`缓存计算结果
- 在应用初始化时执行一次

#### 与现有代码兼容 ✓
- 利用已有的`#deploy-platform-root`挂载点
- 兼容现有的数据属性机制
- 无需修改Spring Boot模板加载逻辑

---

## 4. 实现方案

### 4.1 第一步：创建检测Hook

**文件**：`src/main/frontend/react-app/hooks/useEmbedMode.ts`

```typescript
import { useMemo } from 'react';

/**
 * 检测React应用是否运行在嵌入模式（被Spring Boot主应用包含）
 *
 * 嵌入模式特征：
 * - 运行在主应用的内容片段中
 * - 由main.ts的loadDeployPlatformMfe()动态加载
 * - 共享Spring Boot提供的认证、i18n等服务
 *
 * 独立模式特征：
 * - 直接通过URL访问React应用
 * - 使用Vite dev server或独立的React构建
 * - 自管理所有应用状态
 */
export function useIsEmbeddedMode(): boolean {
    return useMemo(() => {
        if (typeof document === 'undefined') {
            return false;
        }

        // 信号1：容器存在检查（必要条件）
        const container = document.getElementById('deploy-platform-root');
        if (!container) {
            return false;
        }

        // 信号2：Spring Boot主模板标志
        // 检查主应用特有的DOM结构
        const hasMainLayoutMarker =
            !!document.querySelector('[data-dev-shell-message]') ||  // main.ts marker
            !!document.querySelector('.content-framework') ||         // admin template class
            !!document.querySelector('.sidebar') ||                   // Spring Boot sidebar
            !!document.querySelector('meta[name="_csrf"]');          // Spring Security marker

        // 信号3：显式嵌入标记（Spring Boot模板可设置）
        const hasExplicitEmbedFlag =
            container.hasAttribute('data-embed-mode') &&
            container.getAttribute('data-embed-mode') === 'true';

        // 信号4：window全局标记（可由Spring Boot模板注入）
        const hasWindowFlag = (window as any).__DEPLOY_PLATFORM_EMBEDDED === true;

        // 综合判断逻辑
        // 必须：容器存在
        // 且：至少满足以下之一
        //   - Spring Boot模板标志明确
        //   - 显式嵌入标记
        //   - Window全局标记
        const isEmbedded = container && (
            hasMainLayoutMarker ||
            hasExplicitEmbedFlag ||
            hasWindowFlag
        );

        // 开发调试信息
        if (import.meta.env.DEV) {
            console.log('[embed-mode-detector]', {
                containerExists: !!container,
                hasMainLayoutMarker,
                hasExplicitEmbedFlag,
                hasWindowFlag,
                isEmbedded
            });
        }

        return isEmbedded;
    }, []);
}

/**
 * 获取嵌入模式的上下文
 * 提供有用的信息用于条件渲染
 */
export function useEmbedModeContext() {
    const isEmbedded = useIsEmbeddedMode();
    const container = document.getElementById('deploy-platform-root');

    return useMemo(() => ({
        isEmbedded,
        container,
        // 从容器读取配置
        authEndpoint: container?.getAttribute('data-auth-endpoint'),
        summaryEndpoint: container?.getAttribute('data-summary-endpoint'),
        routerBase: container?.getAttribute('data-router-base'),
    }), [isEmbedded]);
}
```

### 4.2 第二步：创建条件Layout组件

**文件**：`src/main/frontend/react-app/layout/ConditionalLayout.tsx`

```typescript
import { PropsWithChildren } from 'react';
import { ShellLayout } from './ShellLayout';
import { useIsEmbeddedMode } from '../hooks/useEmbedMode';

/**
 * 条件式布局组件
 * - 嵌入模式：只渲染内容，不渲染导航框架
 * - 独立模式：渲染完整的ShellLayout（带侧边栏）
 */
export function ConditionalLayout({ children }: PropsWithChildren): JSX.Element {
    const isEmbedded = useIsEmbeddedMode();

    if (isEmbedded) {
        // 嵌入模式：Spring Boot主应用已提供导航框架
        // 只需渲染内容部分
        return <>{children}</>;
    }

    // 独立模式：需要完整的应用框架
    return <ShellLayout>{children}</ShellLayout>;
}
```

### 4.3 第三步：修改App.tsx使用条件Layout

**文件**：`src/main/frontend/react-app/App.tsx`

```typescript
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { I18nextProvider } from "react-i18next";

import "./index.css";
import { ConditionalLayout } from "./layout/ConditionalLayout";  // 新增
import { OverviewPage } from "./pages/OverviewPage";
import { ReleaseDetailsPlaceholder, ReleasesPage } from "./pages/ReleasesPage";
import { ReleaseDetailsPage } from "./pages/ReleaseDetailsPage";
import { PoliciesPage } from "./pages/PoliciesPage";
import { queryClient } from "./config/queryClient";
import i18n from "./i18n/i18n";
import { useTokenRefresh } from "./hooks/useTokenRefresh";

type AppProps = {
    basename?: string;
};

const DEFAULT_BASENAME = "/";

const resolveBasename = (): string => {
    if (typeof document === "undefined") {
        return DEFAULT_BASENAME;
    }

    const container = document.getElementById("deploy-platform-root");
    const attributeBasename = container?.dataset?.routerBase;
    if (attributeBasename && attributeBasename.trim().length > 0) {
        return attributeBasename.trim();
    }

    return DEFAULT_BASENAME;
};

export function AppRoutes(): JSX.Element {
    return (
        <ConditionalLayout>  {/* 替换ShellLayout */}
            <Routes>
                <Route path="/" element={<Navigate to="/overview" replace />} />
                <Route path="/overview" element={<OverviewPage />} />
                <Route path="/releases" element={<ReleasesPage />}>
                    <Route index element={<ReleaseDetailsPlaceholder />} />
                    <Route path=":releaseId" element={<ReleaseDetailsPage />} />
                </Route>
                <Route path="/settings/policies" element={<PoliciesPage />} />
                <Route path="*" element={<Navigate to="/overview" replace />} />
            </Routes>
        </ConditionalLayout>
    );
}

export function App({ basename }: AppProps = {}): JSX.Element {
    const base = basename ?? resolveBasename();

    // Initialize token refresh mechanism
    useTokenRefresh();

    return (
        <QueryClientProvider client={queryClient}>
            <I18nextProvider i18n={i18n}>
                <BrowserRouter basename={base}>
                    <AppRoutes />
                </BrowserRouter>
            </I18nextProvider>
            <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
    );
}

export default App;
```

### 4.4 第四步：可选 - 在Spring Boot模板中添加显式标记

**文件**：`src/main/resources/templates/admin/deploy-platform-content.html`（可选增强）

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="utf-8"/>
    <title>Deployment Platform</title>
</head>
<body>
<main class="content-framework admin-theme deploy-platform deploy-platform-page" role="main" th:fragment="deploy-platform-content">
    <div class="content-scrollable">
        <!-- React App Container (Full Height, No Padding) -->
        <div class="deploy-platform-react-container">
            <script>
                window.__DEPLOY_PLATFORM_DEFER_AUTO_MOUNT__ = false;
                // 显式标记应用运行在嵌入模式
                window.__DEPLOY_PLATFORM_EMBEDDED = true;
            </script>
            <div id="deploy-platform-root"
                 class="deploy-platform-host"
                 data-embed-mode="true"
                 data-auth-endpoint="/api/deploy-platform/token"
                 data-summary-endpoint="/api/deploy-platform/dashboard/summary"
                 data-i18n-zh="正在加载部署管理面板…"
                 data-i18n-en="Loading deployment console...">
                Loading deployment console...
            </div>
        </div>
    </div>
    <!-- ... rest of styles ... -->
</main>
</body>
</html>
```

### 4.5 第五步：为Vite Dev Server添加支持

**文件**：`src/main/frontend/react-app/index.html`（如果存在Vite dev入口）

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Deploy Platform - Development</title>
</head>
<body>
    <div id="deploy-platform-root"></div>

    <!-- 独立模式标记：模拟Spring Boot模板的某些元素，便于开发 -->
    <div data-dev-shell-message style="display: none;"></div>

    <script type="module" src="./main.tsx"></script>
</body>
</html>
```

---

## 5. 考虑过的替代方案

### 5.1 方案A：基于URL路径检测

```typescript
function isEmbeddedByURL(): boolean {
    return !window.location.pathname.startsWith('/standalone/');
}
```

**优点**：
- 实现简单
- 明确的URL约定

**缺点**：
- ❌ 需要修改Spring Boot路由
- ❌ URL改变时检测失效
- ❌ 与当前的URL路由架构不兼容
- ❌ 无法区分相同URL下的不同运行模式

**结论**：不适用于本项目的动态内容加载模式

---

### 5.2 方案B：基于window属性检测

```typescript
function isEmbeddedByWindowProp(): boolean {
    return window.__DEPLOY_PLATFORM_EMBEDDED === true;
}
```

**优点**：
- 简洁明确
- 可由Spring Boot模板显式设置

**缺点**：
- ❌ 依赖单一信号，容易误判
- ❌ 如果Spring Boot模板忘记设置则失效
- ❌ 开发过程中容易忘记配置

**推荐用途**：作为`useIsEmbeddedMode()`的**补充信号**而非唯一方案

---

### 5.3 方案C：基于特定CSS类检测

```typescript
function isEmbeddedByCSS(): boolean {
    return document.body.classList.contains('layout-shell') &&
           document.querySelector('.content-framework') !== null;
}
```

**优点**：
- 利用现有的CSS类
- 与视觉布局关联

**缺点**：
- ❌ 对CSS变化敏感
- ❌ 单一信号可靠性不足
- ❌ 重构CSS时容易破坏检测

**推荐用途**：作为`useIsEmbeddedMode()`的**备选信号**

---

### 5.4 方案D：基于API端点可达性检测

```typescript
async function isEmbeddedByAPI(): Promise<boolean> {
    try {
        const response = await fetch('/api/deploy-platform/token', {
            method: 'HEAD'
        });
        return response.ok;
    } catch {
        return false;
    }
}
```

**优点**：
- 运行时验证
- 真实检查API可用性

**缺点**：
- ❌ 异步操作，影响应用初始化
- ❌ 额外的网络请求
- ❌ 网络延迟可能导致延迟初始化
- ❌ 不适合同步检测

**推荐用途**：在应用初始化后的**运行时验证**，而非启动检测

---

### 5.5 方案E：基于容器属性检测

```typescript
function isEmbeddedByContainer(): boolean {
    const container = document.getElementById('deploy-platform-root');
    return container !== null &&
           container.parentElement?.classList.contains('content-scrollable');
}
```

**优点**：
- 检查具体的DOM结构
- 与挂载点直接关联

**缺点**：
- ❌ 单一信号，容易误判
- ❌ 对DOM结构变化敏感

**推荐用途**：作为`useIsEmbeddedMode()`的**基础信号**

---

## 6. 检测时机分析

### 6.1 最佳时机：应用初始化时（推荐）

```typescript
// 在App.tsx的root组件中进行
export function App(): JSX.Element {
    const isEmbedded = useIsEmbeddedMode();  // 应用启动时检测一次

    // ... rest of app initialization
}
```

**优点**：
- ✓ 及时反映
- ✓ 决定初始的UI结构
- ✓ 避免React重新渲染

**缺点**：
- 如果检测时机晚，可能导致布局闪现

---

### 6.2 备选时机：main.tsx中的bootstrap

```typescript
// src/main/frontend/react-app/main.tsx
const bootstrap = (): void => {
    initTokenBridge({
        onTokenError: (reason) => {
            if (import.meta.env.DEV) {
                console.warn("[deploy-platform] token bridge error", reason);
            }
        }
    });

    // 在这里检测嵌入模式，可以提前设置全局标记
    const isEmbedded = detectEmbedMode();
    window.__DETECTED_EMBED_MODE = isEmbedded;

    if (window.__DEPLOY_PLATFORM_DEFER_AUTO_MOUNT__) {
        return;
    }

    if (document.readyState === "loading") {
        document.addEventListener(
            "DOMContentLoaded",
            () => {
                mountDeployPlatform();
            },
            { once: true }
        );
    } else {
        mountDeployPlatform();
    }
};
```

**优点**：
- ✓ 最早阶段检测
- ✓ 可以预先设置全局标记
- ✓ 便于条件加载资源

**缺点**：
- 需要在两个地方进行相同的检测逻辑

---

### 6.3 应避免的时机：延迟检测

❌ **不推荐**：在effect中延迟检测
```typescript
useEffect(() => {
    // 不好的做法：这会导致React先渲染完整layout，再切换到内容only
    const isEmbedded = detectEmbedMode();
    setIsEmbedded(isEmbedded);
}, []);
```

---

## 7. 将检测结果传递给React组件树

### 7.1 方案A：Context API（推荐）

```typescript
// src/main/frontend/react-app/context/EmbedModeContext.tsx
import React, { createContext, useContext, useMemo } from 'react';

interface EmbedModeContextType {
    isEmbedded: boolean;
    container: HTMLElement | null;
}

const EmbedModeContext = createContext<EmbedModeContextType | undefined>(undefined);

export function EmbedModeProvider({ children }: { children: React.ReactNode }) {
    const isEmbedded = useIsEmbeddedMode();
    const container = document.getElementById('deploy-platform-root');

    const value = useMemo<EmbedModeContextType>(() => ({
        isEmbedded,
        container,
    }), [isEmbedded]);

    return (
        <EmbedModeContext.Provider value={value}>
            {children}
        </EmbedModeContext.Provider>
    );
}

export function useEmbedMode(): EmbedModeContextType {
    const context = useContext(EmbedModeContext);
    if (!context) {
        throw new Error('useEmbedMode must be used within EmbedModeProvider');
    }
    return context;
}
```

**优点**：
- ✓ 符合React最佳实践
- ✓ 类型安全
- ✓ 任何深层组件都可访问
- ✓ 便于扩展（可添加更多上下文信息）

**缺点**：
- 需要在App.tsx中包装provider

**使用示例**：
```typescript
// 在App.tsx中
export function App(): JSX.Element {
    return (
        <EmbedModeProvider>
            <QueryClientProvider client={queryClient}>
                {/* ... rest of app ... */}
            </QueryClientProvider>
        </EmbedModeProvider>
    );
}

// 在任何组件中
function MyComponent() {
    const { isEmbedded } = useEmbedMode();
    return isEmbedded ? <p>嵌入模式</p> : <p>独立模式</p>;
}
```

---

### 7.2 方案B：全局window属性

```typescript
declare global {
    interface Window {
        __DEPLOY_PLATFORM_IS_EMBEDDED?: boolean;
    }
}

// 在main.tsx初始化时设置
window.__DEPLOY_PLATFORM_IS_EMBEDDED = detectEmbedMode();

// 组件中访问
function useIsEmbedded() {
    return window.__DEPLOY_PLATFORM_IS_EMBEDDED ?? false;
}
```

**优点**：
- ✓ 简单直接
- ✓ 无需provider包装
- ✓ 适合全局配置

**缺点**：
- ❌ 无类型安全（除非声明接口）
- ❌ 不符合React最佳实践
- ❌ 难以测试

**推荐用途**：仅作为备选方案

---

### 7.3 方案C：useState存储模式（不推荐）

❌ **不推荐**：
```typescript
function App() {
    const [isEmbedded, setIsEmbedded] = useState(false);

    useEffect(() => {
        setIsEmbedded(detectEmbedMode());
    }, []);

    // 这会导致初始渲染时isEmbedded为false，
    // 之后变为true时React会重新渲染整个树
}
```

---

## 8. 常见陷阱与边界情况

### 8.1 陷阱1：初始化时序问题

❌ **问题**：
```typescript
// 错误：DOM可能还未初始化
const isEmbedded = detectEmbedMode(); // 在顶级调用，可能太早

function MyComponent() {
    return <div>{/* 使用 isEmbedded ... */}</div>;
}
```

✅ **解决**：
```typescript
function MyComponent() {
    const isEmbedded = useIsEmbeddedMode(); // 在组件中调用，确保DOM已准备好
    return <div>{/* 使用 isEmbedded ... */}</div>;
}
```

---

### 8.2 陷阱2：SSR上下文（如果有的话）

❌ **问题**：
```typescript
function detectEmbedMode(): boolean {
    // 这会在SSR时崩溃
    return document.getElementById('deploy-platform-root') !== null;
}
```

✅ **解决**：
```typescript
function detectEmbedMode(): boolean {
    if (typeof document === 'undefined') {
        return false; // SSR环境
    }
    return document.getElementById('deploy-platform-root') !== null;
}
```

---

### 8.3 陷阱3：缓存失效

❌ **问题**：
```typescript
// 检测结果改变后，缓存不会更新
const isEmbedded = useIsEmbeddedMode();
// ... 之后手动设置 window.__DEPLOY_PLATFORM_EMBEDDED = false;
// 但 isEmbedded 仍然是旧值
```

✅ **解决**：
- 使用 `useMemo` 的依赖数组
- 如果检测条件可能改变，使用 `useEffect` 监听
- 或者声明检测条件是**一次性的，应用启动时不会改变**

---

### 8.4 边界情况1：容器被移除

❌ **问题**：
```typescript
// 如果容器在应用运行中被DOM移除
document.getElementById('deploy-platform-root').remove();

// 后续调用仍然会返回false，即使应该仍在嵌入模式
const isEmbedded = useIsEmbeddedMode();
```

✅ **解决**：
- 在初始化时存储检测结果
- 使用缓存的值而非每次重新检测
- 确保Spring Boot模板在应用生命周期中不移除容器

---

### 8.5 边界情况2：开发环境中的Vite Hot Module Reload

❌ **问题**：
```typescript
// 在HMR后，window属性可能被重置
window.__DEPLOY_PLATFORM_EMBEDDED = true;
// ... HMR发生 ...
// window.__DEPLOY_PLATFORM_EMBEDDED 可能不存在了
```

✅ **解决**：
- 使用持久化的检测逻辑（基于DOM结构）而非window属性
- 在useIsEmbeddedMode()中重新进行DOM检查
- 避免依赖可能在HMR时改变的全局变量

---

### 8.6 边界情况3：多个React应用共存

❌ **问题**：
```typescript
// 如果有多个React应用，都使用 #deploy-platform-root
const container = document.getElementById('deploy-platform-root');
// 无法区分是哪个应用的容器
```

✅ **解决**：
- 为每个MFE应用使用不同的容器ID
- 示例：`#deploy-platform-root`, `#other-app-root` 等
- 在Hook中参数化容器ID

---

### 8.7 边界情况4：网络隔离环境

❌ **问题**：
```typescript
// 如果API端点不可达，无法进行API检测
const isEmbedded = await checkAPIAvailable('/api/...');
```

✅ **解决**：
- 使用本地DOM检查，不依赖网络请求
- 如果需要网络检查，设置timeout和fallback

---

## 9. 完整的实现检查清单

- [ ] 创建 `src/main/frontend/react-app/hooks/useEmbedMode.ts`
- [ ] 创建 `src/main/frontend/react-app/layout/ConditionalLayout.tsx`
- [ ] 修改 `src/main/frontend/react-app/App.tsx` 使用 ConditionalLayout
- [ ] （可选）修改 `src/main/resources/templates/admin/deploy-platform-content.html` 添加显式标记
- [ ] 添加 TypeScript 类型定义到 `src/main/frontend/react-app/main.tsx`
- [ ] 编写单元测试：`src/main/frontend/tests/react-app/hooks/useEmbedMode.test.tsx`
- [ ] 编写集成测试：验证嵌入模式下ShellLayout不渲染，独立模式下渲染
- [ ] 更新开发文档记录检测机制
- [ ] 在Storybook中测试ConditionalLayout两种模式

---

## 10. 性能考虑

### 10.1 DOM查询成本

```typescript
// ❌ 低效：每次都查询DOM
const isEmbedded1 = () => document.getElementById('deploy-platform-root') !== null;
const isEmbedded2 = () => document.querySelector('[data-dev-shell-message]') !== null;

// ✅ 高效：使用useMemo缓存结果
const isEmbedded = useMemo(() => {
    return document.getElementById('deploy-platform-root') !== null;
}, []);
```

### 10.2 初始化顺序优化

```typescript
// ✅ 推荐的初始化顺序
1. main.tsx bootstrap阶段：设置window标记
2. App.tsx：使用useIsEmbeddedMode()进行第一次检测
3. ConditionalLayout：根据检测结果渲染不同的树
```

---

## 11. 测试策略

### 11.1 单元测试

```typescript
// src/main/frontend/tests/react-app/hooks/useEmbedMode.test.tsx
import { renderHook } from '@testing-library/react';
import { useIsEmbeddedMode } from '@/react-app/hooks/useEmbedMode';

describe('useIsEmbeddedMode', () => {
    it('should return false when container does not exist', () => {
        const { result } = renderHook(() => useIsEmbeddedMode());
        expect(result.current).toBe(false);
    });

    it('should return true when container exists with Spring Boot markers', () => {
        // Setup: 创建模拟的Spring Boot DOM结构
        const container = document.createElement('div');
        container.id = 'deploy-platform-root';
        document.body.appendChild(container);

        const marker = document.createElement('div');
        marker.setAttribute('data-dev-shell-message', '');
        document.body.appendChild(marker);

        const { result } = renderHook(() => useIsEmbeddedMode());
        expect(result.current).toBe(true);

        // Cleanup
        container.remove();
        marker.remove();
    });

    it('should return true with explicit embed flag', () => {
        const container = document.createElement('div');
        container.id = 'deploy-platform-root';
        container.setAttribute('data-embed-mode', 'true');
        document.body.appendChild(container);

        const { result } = renderHook(() => useIsEmbeddedMode());
        expect(result.current).toBe(true);

        container.remove();
    });
});
```

### 11.2 集成测试

```typescript
// 验证嵌入模式下ShellLayout不渲染
describe('ConditionalLayout - Embedded Mode', () => {
    it('should not render ShellLayout in embedded mode', () => {
        // Setup embedded environment
        const root = document.createElement('div');
        root.id = 'deploy-platform-root';
        document.body.appendChild(root);

        const { container } = render(
            <ConditionalLayout>
                <div data-testid="content">Content Only</div>
            </ConditionalLayout>
        );

        expect(screen.getByTestId('content')).toBeInTheDocument();
        expect(screen.queryByTestId('deploy-platform-shell')).not.toBeInTheDocument();
    });
});

// 验证独立模式下ShellLayout渲染
describe('ConditionalLayout - Standalone Mode', () => {
    it('should render ShellLayout in standalone mode', () => {
        const { container } = render(
            <ConditionalLayout>
                <div data-testid="content">Content</div>
            </ConditionalLayout>
        );

        expect(screen.getByTestId('content')).toBeInTheDocument();
        expect(screen.getByTestId('deploy-platform-shell')).toBeInTheDocument();
    });
});
```

---

## 12. 部署和生产考虑

### 12.1 构建时验证

```bash
# 在CI/CD中验证两种模式都能正常工作
npm run build
npm run build:standalone  # 如果有
npm run test
```

### 12.2 运行时监控

```typescript
if (import.meta.env.PROD) {
    console.log('[embed-mode] Running in mode:',
        window.__DEPLOY_PLATFORM_IS_EMBEDDED ? 'embedded' : 'standalone'
    );
}
```

### 12.3 错误处理

```typescript
// 如果检测失败，使用安全的默认值
const isEmbedded = detectEmbedMode() || false;
```

---

## 13. 总结表格

| 检测方法 | 可靠性 | 性能 | 扩展性 | 推荐度 |
|---------|-------|------|--------|--------|
| **多层验证策略** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **推荐** |
| 单一window属性 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | 补充 |
| URL路径 | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | 不适用 |
| 特定CSS类 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | 备选 |
| API可达性 | ⭐⭐⭐⭐ | ⭐ | ⭐⭐⭐⭐ | 运行时验证 |

---

## 14. 参考资源

### 当前项目文件
- 挂载点配置：`src/main/resources/templates/admin/deploy-platform-content.html`
- React入口：`src/main/frontend/react-app/main.tsx`
- MFE加载：`src/main/frontend/mfe/deploy-platform-loader.ts`
- 主应用：`src/main/resources/templates/main-layout.html`

### 相关技术文档
- React Context API: https://react.dev/reference/react/useContext
- Vite: https://vitejs.dev/
- Module Federation: https://webpack.js.org/concepts/module-federation/
- React 18 Suspense: https://react.dev/reference/react/Suspense

---

## 15. 后续改进建议

1. **监控和日志**：添加详细的初始化日志便于调试
2. **功能标志集成**：考虑集成Feature Flag系统
3. **性能分析**：添加性能监控指标
4. **E2E测试**：使用Cypress/Playwright进行端到端测试
5. **多MFE支持**：如果有多个Module Federation应用，扩展检测逻辑

---

**文档版本**：1.0
**最后更新**：2025-11-02
**维护者**：Development Team
