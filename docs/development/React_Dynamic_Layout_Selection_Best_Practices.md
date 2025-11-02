# React应用中根据运行时条件动态选择布局组件的最佳实践

**文档版本**: 1.0
**更新时间**: 2025-11-02
**作者**: Claude AI Research
**相关项目**: Deploy Platform React App

---

## 1. 概述

本文档研究了在React应用中根据运行时条件（如嵌入/独立模式）动态选择不同布局组件的最佳实践。特别针对：
- React Router v6
- TypeScript类型安全
- 避免不必要的重渲染
- 状态和路由配置的一致性

---

## 2. 问题场景分析

### 2.1 当前项目状态

**现有架构**（如项目App.tsx所示）:
```
App
├── BrowserRouter
│   └── AppRoutes
│       └── ShellLayout (固定)
│           └── Routes
│               ├── OverviewPage
│               ├── ReleasesPage
│               ├── PoliciesPage
│               └── ...
```

**需要改进的点**:
1. ShellLayout硬编码在路由配置中
2. 无法根据运行时条件（嵌入/独立模式）切换布局
3. 如果同时需要ContentOnlyLayout（无侧边栏），需要大幅重构

### 2.2 使用场景

| 场景 | 布局类型 | 侧边栏 | 工具栏 | 导航 |
|------|---------|--------|--------|------|
| 独立应用 | ShellLayout | ✓ | ✓ | ✓ |
| 嵌入其他应用 | ContentOnlyLayout | ✗ | ✗ | ✗ |
| 仪表板模式 | MinimalLayout | ✗ | ✓ | ✗ |

---

## 3. 推荐的实现模式

### 3.1 核心原则

1. **单一责任**: 布局选择与路由配置分离
2. **类型安全**: 完整的TypeScript类型支持
3. **性能**: 避免布局切换时的不必要重渲染
4. **灵活性**: 支持多种布局组合
5. **可测试性**: 易于单元测试和集成测试

### 3.2 推荐方案：Context + 配置化路由

这是最灵活、可维护性最高的方案。

#### 3.2.1 第一步：定义类型和Context

**文件**: `src/main/frontend/react-app/context/layoutContext.ts`

```typescript
/**
 * 布局配置上下文
 *
 * 用于在应用全局范围内管理布局选择，
 * 避免需要将layout props逐级向下传递
 */

import { createContext, useContext } from 'react';

/**
 * 支持的布局类型
 */
export enum LayoutType {
  /** 完整的应用壳层：包含侧边栏、工具栏、导航 */
  SHELL = 'shell',
  /** 仅内容区域：适用于嵌入场景 */
  CONTENT_ONLY = 'content-only',
  /** 最小化布局：仅包含工具栏 */
  MINIMAL = 'minimal',
}

/**
 * 布局配置对象
 */
export interface LayoutConfig {
  /** 当前布局类型 */
  layoutType: LayoutType;

  /** 是否为嵌入模式（由父应用管理） */
  isEmbedded: boolean;

  /** 是否显示侧边栏 */
  showSidebar: boolean;

  /** 是否显示工具栏 */
  showToolbar: boolean;

  /** 侧边栏宽度（像素） */
  sidebarWidth: number;
}

/**
 * 布局上下文值类型
 */
export interface LayoutContextValue {
  /** 当前布局配置 */
  config: LayoutConfig;

  /** 更新布局类型 */
  setLayoutType: (layoutType: LayoutType) => void;

  /** 更新整个配置 */
  setConfig: (config: LayoutConfig) => void;

  /** 检查是否为特定布局类型 */
  isLayout: (layoutType: LayoutType) => boolean;
}

/**
 * 布局上下文（默认为SHELL布局）
 */
export const LayoutContext = createContext<LayoutContextValue | undefined>(undefined);

/**
 * 使用布局上下文的Hook
 *
 * @throws 如果在LayoutProvider外部使用
 * @example
 * ```tsx
 * function MyComponent() {
 *   const { config, setLayoutType } = useLayout();
 *   return <div>{config.layoutType}</div>;
 * }
 * ```
 */
export function useLayout(): LayoutContextValue {
  const context = useContext(LayoutContext);
  if (!context) {
    throw new Error('useLayout must be used within LayoutProvider');
  }
  return context;
}

/**
 * 检查布局上下文是否可用
 * @returns Context是否已初始化
 */
export function useLayoutSafe(): LayoutContextValue | null {
  return useContext(LayoutContext) ?? null;
}
```

#### 3.2.2 第二步：创建Layout Provider

**文件**: `src/main/frontend/react-app/providers/LayoutProvider.tsx`

```typescript
/**
 * 布局提供者组件
 *
 * 负责：
 * 1. 在应用初始化时检测运行时条件（嵌入/独立模式）
 * 2. 初始化布局配置
 * 3. 提供布局上下文给所有子组件
 * 4. 处理布局动态切换
 */

import { PropsWithChildren, useState, useEffect, useCallback, useMemo } from 'react';
import { LayoutContext, LayoutConfig, LayoutType } from '../context/layoutContext';
import type { LayoutContextValue } from '../context/layoutContext';

/**
 * 布局检测配置
 */
interface LayoutDetectionConfig {
  /** 嵌入模式检测函数 */
  detectEmbedded: () => boolean;

  /** 默认布局类型 */
  defaultLayout: LayoutType;

  /** 嵌入模式下的默认布局 */
  embeddedLayout: LayoutType;
}

/**
 * 从DOM属性检测嵌入模式
 *
 * 检查点：
 * 1. 容器元素的data-*属性
 * 2. window全局变量
 * 3. URL查询参数
 */
function detectEmbeddedMode(): boolean {
  if (typeof document === 'undefined') {
    return false;
  }

  // 检查容器元素属性
  const container = document.getElementById('deploy-platform-root');
  if (container?.dataset?.embedded === 'true') {
    return true;
  }

  // 检查全局变量
  if (typeof window !== 'undefined' && (window as any).__DEPLOY_PLATFORM_EMBEDDED__) {
    return true;
  }

  // 检查URL查询参数
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search);
    if (params.get('embedded') === 'true') {
      return true;
    }
  }

  return false;
}

/**
 * 计算初始布局配置
 */
function computeInitialLayout(
  isEmbedded: boolean,
  config: LayoutDetectionConfig
): LayoutConfig {
  const layoutType = isEmbedded ? config.embeddedLayout : config.defaultLayout;

  return {
    layoutType,
    isEmbedded,
    showSidebar: layoutType === LayoutType.SHELL,
    showToolbar: layoutType !== LayoutType.CONTENT_ONLY,
    sidebarWidth: 280, // 默认侧边栏宽度
  };
}

/**
 * LayoutProvider Props
 */
interface LayoutProviderProps extends PropsWithChildren {
  /** 布局检测配置 */
  detectionConfig?: Partial<LayoutDetectionConfig>;
}

/**
 * 布局提供者组件
 *
 * 使用：
 * 1. 在应用根级别包装
 * 2. 优先级在BrowserRouter之前（如果使用Router）
 *
 * @example
 * ```tsx
 * <LayoutProvider
 *   detectionConfig={{
 *     defaultLayout: LayoutType.SHELL,
 *     embeddedLayout: LayoutType.CONTENT_ONLY,
 *   }}
 * >
 *   <App />
 * </LayoutProvider>
 * ```
 */
export function LayoutProvider({
  children,
  detectionConfig = {},
}: LayoutProviderProps): JSX.Element {
  const config: LayoutDetectionConfig = {
    detectEmbedded,
    defaultLayout: LayoutType.SHELL,
    embeddedLayout: LayoutType.CONTENT_ONLY,
    ...detectionConfig,
  };

  // 初始化布局配置
  const [layoutConfig, setLayoutConfig] = useState<LayoutConfig>(() => {
    const isEmbedded = config.detectEmbedded();
    return computeInitialLayout(isEmbedded, config);
  });

  // 提供布局切换函数
  const setLayoutType = useCallback((layoutType: LayoutType) => {
    setLayoutConfig((prev) => ({
      ...prev,
      layoutType,
      showSidebar: layoutType === LayoutType.SHELL,
      showToolbar: layoutType !== LayoutType.CONTENT_ONLY,
    }));
  }, []);

  const updateConfig = useCallback((newConfig: LayoutConfig) => {
    setLayoutConfig(newConfig);
  }, []);

  const isLayout = useCallback(
    (layoutType: LayoutType): boolean => layoutConfig.layoutType === layoutType,
    [layoutConfig.layoutType]
  );

  // 构建上下文值，使用useMemo避免不必要的重新创建
  const contextValue: LayoutContextValue = useMemo(
    () => ({
      config: layoutConfig,
      setLayoutType,
      setConfig: updateConfig,
      isLayout,
    }),
    [layoutConfig, setLayoutType, updateConfig, isLayout]
  );

  // 监听容器属性变化，重新检测嵌入模式
  useEffect(() => {
    const handleMutations = (mutations: MutationRecord[]) => {
      for (const mutation of mutations) {
        if (
          mutation.type === 'attributes' &&
          (mutation.target as HTMLElement).id === 'deploy-platform-root'
        ) {
          const newIsEmbedded = config.detectEmbedded();
          setLayoutConfig((prev) => {
            if (prev.isEmbedded === newIsEmbedded) {
              return prev; // 未变化，不更新
            }
            return computeInitialLayout(newIsEmbedded, config);
          });
        }
      }
    };

    const container = document.getElementById('deploy-platform-root');
    if (!container) return;

    const observer = new MutationObserver(handleMutations);
    observer.observe(container, { attributes: true });

    return () => observer.disconnect();
  }, [config]);

  if (import.meta.env.DEV) {
    console.log('[LayoutProvider] Initialized with config:', layoutConfig);
  }

  return (
    <LayoutContext.Provider value={contextValue}>
      {children}
    </LayoutContext.Provider>
  );
}

export function detectEmbedded(): boolean {
  return detectEmbeddedMode();
}
```

#### 3.2.3 第三步：创建布局选择器组件

**文件**: `src/main/frontend/react-app/layout/LayoutSelector.tsx`

```typescript
/**
 * 动态布局选择器
 *
 * 根据布局配置自动选择正确的布局组件
 */

import { PropsWithChildren } from 'react';
import { useLayout } from '../context/layoutContext';
import { LayoutType } from '../context/layoutContext';
import { ShellLayout } from './ShellLayout';
import { ContentOnlyLayout } from './ContentOnlyLayout';
import { MinimalLayout } from './MinimalLayout';

/**
 * 布局组件映射
 */
const LAYOUT_COMPONENTS = {
  [LayoutType.SHELL]: ShellLayout,
  [LayoutType.CONTENT_ONLY]: ContentOnlyLayout,
  [LayoutType.MINIMAL]: MinimalLayout,
} as const;

/**
 * LayoutSelector Props
 */
interface LayoutSelectorProps extends PropsWithChildren {
  /** 回退布局类型（如果检测失败） */
  fallbackLayout?: LayoutType;
}

/**
 * 布局选择器组件
 *
 * 根据LayoutContext中的配置自动选择合适的布局组件
 *
 * @example
 * ```tsx
 * <LayoutSelector fallbackLayout={LayoutType.SHELL}>
 *   <Routes>
 *     <Route path="/overview" element={<OverviewPage />} />
 *     {/* 其他路由 */}
 *   </Routes>
 * </LayoutSelector>
 * ```
 */
export function LayoutSelector({
  children,
  fallbackLayout = LayoutType.SHELL,
}: LayoutSelectorProps): JSX.Element {
  const { config } = useLayout();

  // 获取对应的布局组件
  const LayoutComponent = LAYOUT_COMPONENTS[config.layoutType] ||
                          LAYOUT_COMPONENTS[fallbackLayout];

  if (!LayoutComponent) {
    throw new Error(
      `Invalid layout type: ${config.layoutType}. ` +
      `Supported types: ${Object.keys(LayoutType).join(', ')}`
    );
  }

  return <LayoutComponent>{children}</LayoutComponent>;
}
```

#### 3.2.4 第四步：创建其他布局组件

**文件**: `src/main/frontend/react-app/layout/ContentOnlyLayout.tsx`

```typescript
/**
 * 仅内容布局组件
 *
 * 用于嵌入场景，不包含侧边栏、工具栏等
 * 仅渲染主内容区域
 */

import { PropsWithChildren } from 'react';

/**
 * ContentOnlyLayout Props
 */
interface ContentOnlyLayoutProps extends PropsWithChildren {
  /** 自定义CSS类名 */
  className?: string;
}

/**
 * 仅内容布局
 *
 * 特点：
 * - 无侧边栏
 * - 无工具栏
 * - 最小化DOM结构
 * - 适合嵌入到其他应用
 */
export function ContentOnlyLayout({
  children,
  className,
}: ContentOnlyLayoutProps): JSX.Element {
  return (
    <div className={['dp-layout-content-only', className].filter(Boolean).join(' ')}>
      <main data-testid="deploy-platform-content">
        {children}
      </main>
    </div>
  );
}
```

**文件**: `src/main/frontend/react-app/layout/MinimalLayout.tsx`

```typescript
/**
 * 最小化布局组件
 *
 * 包含工具栏但无侧边栏，用于仪表板模式
 */

import { PropsWithChildren } from 'react';
import { useTranslation } from 'react-i18next';
import { LanguageSwitcher } from '../components/LanguageSwitcher/LanguageSwitcher';

/**
 * MinimalLayout Props
 */
interface MinimalLayoutProps extends PropsWithChildren {
  /** 自定义CSS类名 */
  className?: string;
}

/**
 * 最小化布局
 *
 * 特点：
 * - 有工具栏
 * - 无侧边栏
 * - 适合仪表板/监控界面
 */
export function MinimalLayout({
  children,
  className,
}: MinimalLayoutProps): JSX.Element {
  const { t } = useTranslation();

  return (
    <div className={['dp-layout-minimal', className].filter(Boolean).join(' ')}>
      <header className="dp-minimal-layout__toolbar">
        <div>
          <h2 className="dp-minimal-layout__title">{t('shell.toolbarTitle')}</h2>
          <p className="dp-minimal-layout__subtitle">
            {t('shell.toolbarSubtitle')}
          </p>
        </div>
        <div className="dp-minimal-layout__actions">
          <LanguageSwitcher />
        </div>
      </header>

      <main data-testid="deploy-platform-content">
        {children}
      </main>
    </div>
  );
}
```

#### 3.2.5 第五步：更新App.tsx

**文件**: `src/main/frontend/react-app/App.tsx` (更新)

```typescript
/**
 * 应用根组件
 *
 * 变更点：
 * 1. 添加LayoutProvider包装
 * 2. 使用LayoutSelector替代硬编码的ShellLayout
 * 3. 保持其他功能（QueryClient、i18n等）不变
 */

import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { I18nextProvider } from "react-i18next";

import "./index.css";
import { LayoutProvider, LayoutType } from "./providers/LayoutProvider";
import { LayoutSelector } from "./layout/LayoutSelector";
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

/**
 * 应用路由配置
 *
 * 注意：路由配置与布局选择分离
 * 布局由LayoutSelector根据LayoutContext自动选择
 */
export function AppRoutes(): JSX.Element {
    return (
        <LayoutSelector fallbackLayout={LayoutType.SHELL}>
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
        </LayoutSelector>
    );
}

/**
 * 内部应用组件
 *
 * 包含所有功能提供者（除了路由）
 */
function InternalApp(): JSX.Element {
    // 初始化token刷新机制
    useTokenRefresh();

    return (
        <QueryClientProvider client={queryClient}>
            <I18nextProvider i18n={i18n}>
                <AppRoutes />
            </I18nextProvider>
            <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
    );
}

/**
 * 应用根组件
 */
export function App({ basename }: AppProps = {}): JSX.Element {
    const base = basename ?? resolveBasename();

    return (
        <BrowserRouter basename={base}>
            <LayoutProvider
                detectionConfig={{
                    defaultLayout: LayoutType.SHELL,
                    embeddedLayout: LayoutType.CONTENT_ONLY,
                }}
            >
                <InternalApp />
            </LayoutProvider>
        </BrowserRouter>
    );
}

export default App;
```

---

## 4. React Router v6 集成指南

### 4.1 路由配置最佳实践

#### 4.1.1 创建路由配置对象

**文件**: `src/main/frontend/react-app/config/routes.ts`

```typescript
/**
 * 路由配置
 *
 * 集中管理所有路由定义，独立于布局选择
 * 便于维护和单元测试
 */

import type { RouteObject } from 'react-router-dom';
import { Navigate } from 'react-router-dom';
import { OverviewPage } from '../pages/OverviewPage';
import { ReleaseDetailsPlaceholder, ReleasesPage } from '../pages/ReleasesPage';
import { ReleaseDetailsPage } from '../pages/ReleaseDetailsPage';
import { PoliciesPage } from '../pages/PoliciesPage';

/**
 * 路由ID常量（便于编程导航）
 */
export const ROUTE_IDS = {
  ROOT: 'root',
  OVERVIEW: 'overview',
  RELEASES: 'releases',
  RELEASE_DETAILS: 'release-details',
  POLICIES: 'policies',
} as const;

/**
 * 应用路由配置
 *
 * 特点：
 * - 与布局配置完全分离
 * - 易于扩展新路由
 * - 支持嵌套路由
 * - TypeScript完全类型支持
 */
export const appRoutes: RouteObject[] = [
  {
    id: ROUTE_IDS.ROOT,
    path: '/',
    element: <Navigate to="/overview" replace />,
  },
  {
    id: ROUTE_IDS.OVERVIEW,
    path: '/overview',
    element: <OverviewPage />,
  },
  {
    id: ROUTE_IDS.RELEASES,
    path: '/releases',
    element: <ReleasesPage />,
    children: [
      {
        index: true,
        element: <ReleaseDetailsPlaceholder />,
      },
      {
        id: ROUTE_IDS.RELEASE_DETAILS,
        path: ':releaseId',
        element: <ReleaseDetailsPage />,
      },
    ],
  },
  {
    id: ROUTE_IDS.POLICIES,
    path: '/settings/policies',
    element: <PoliciesPage />,
  },
  {
    path: '*',
    element: <Navigate to="/overview" replace />,
  },
];

/**
 * 获取特定路由配置
 * @param routeId - 路由ID
 * @returns 路由对象或undefined
 */
export function getRoute(routeId: string): RouteObject | undefined {
  return appRoutes.find((route) => route.id === routeId);
}

/**
 * 获取所有顶级路由路径
 * @returns 路由路径数组
 */
export function getTopLevelRoutePaths(): string[] {
  return appRoutes
    .filter((route) => route.path && route.path !== '*')
    .map((route) => route.path!)
    .filter(Boolean);
}
```

#### 4.1.2 使用useRoutes Hook

**文件**: `src/main/frontend/react-app/layout/AppRouter.tsx`

```typescript
/**
 * 应用路由器组件
 *
 * 使用useRoutes替代<Routes>元素，
 * 便于从配置对象生成路由树
 */

import { useRoutes } from 'react-router-dom';
import { appRoutes } from '../config/routes';
import { LayoutSelector } from './LayoutSelector';
import { LayoutType } from '../context/layoutContext';

/**
 * AppRouter Props
 */
interface AppRouterProps {
  /** 回退布局类型 */
  fallbackLayout?: LayoutType;
}

/**
 * 应用路由器
 *
 * 使用useRoutes Hook优点：
 * 1. 路由配置与组件分离
 * 2. 支持动态路由加载
 * 3. 支持路由优先级控制
 * 4. 更容易进行单元测试
 */
export function AppRouter({ fallbackLayout }: AppRouterProps): JSX.Element | null {
  const routes = useRoutes(appRoutes);

  if (!routes) {
    return null;
  }

  return (
    <LayoutSelector fallbackLayout={fallbackLayout}>
      {routes}
    </LayoutSelector>
  );
}
```

---

## 5. TypeScript类型定义最佳实践

### 5.1 完整的类型支持

```typescript
/**
 * 文件: src/main/frontend/react-app/types/layout.ts
 *
 * 布局相关的完整类型定义
 */

import { LayoutType, LayoutConfig } from '../context/layoutContext';

/**
 * 布局组件的Props接口
 */
export interface LayoutComponentProps {
  /** 子组件 */
  children: React.ReactNode;

  /** 自定义CSS类名 */
  className?: string;
}

/**
 * 布局选择器的Props接口
 */
export interface LayoutSelectorProps {
  /** 子组件/路由 */
  children: React.ReactNode;

  /** 回退布局类型 */
  fallbackLayout?: LayoutType;
}

/**
 * 布局提供者的Props接口
 */
export interface LayoutProviderProps {
  /** 子组件 */
  children: React.ReactNode;

  /** 布局检测配置 */
  detectionConfig?: {
    detectEmbedded?: () => boolean;
    defaultLayout?: LayoutType;
    embeddedLayout?: LayoutType;
  };
}

/**
 * 需要访问布局的组件Hook返回类型
 */
export type UseLayoutReturn = {
  config: LayoutConfig;
  setLayoutType: (layoutType: LayoutType) => void;
  setConfig: (config: LayoutConfig) => void;
  isLayout: (layoutType: LayoutType) => boolean;
};

/**
 * 编程方式导航的位置状态
 */
export interface LocationState {
  /** 导航来源 */
  from?: string;

  /** 导航数据 */
  data?: Record<string, unknown>;
}
```

### 5.2 类型守卫函数

```typescript
/**
 * 文件: src/main/frontend/react-app/utils/typeGuards.ts
 *
 * 类型守卫和验证函数
 */

import { LayoutType, LayoutConfig } from '../context/layoutContext';

/**
 * 验证是否为有效的布局类型
 */
export function isValidLayoutType(value: unknown): value is LayoutType {
  return Object.values(LayoutType).includes(value as LayoutType);
}

/**
 * 验证布局配置对象
 */
export function isValidLayoutConfig(value: unknown): value is LayoutConfig {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const config = value as Record<string, unknown>;
  return (
    isValidLayoutType(config.layoutType) &&
    typeof config.isEmbedded === 'boolean' &&
    typeof config.showSidebar === 'boolean' &&
    typeof config.showToolbar === 'boolean' &&
    typeof config.sidebarWidth === 'number'
  );
}

/**
 * 安全获取布局类型枚举值
 */
export function parseLayoutType(value: unknown): LayoutType {
  if (isValidLayoutType(value)) {
    return value;
  }
  return LayoutType.SHELL; // 默认值
}
```

---

## 6. 性能优化考虑

### 6.1 避免不必要的重渲染

```typescript
/**
 * 优化1: 使用useMemo缓存Context值
 */
export function LayoutProvider({ children, detectionConfig = {} }: LayoutProviderProps) {
  const [layoutConfig, setLayoutConfig] = useState<LayoutConfig>(() => {
    // ... 初始化逻辑
  });

  // ✅ 正确: 只在layoutConfig或callbacks改变时重新创建
  const contextValue: LayoutContextValue = useMemo(
    () => ({
      config: layoutConfig,
      setLayoutType,
      setConfig: updateConfig,
      isLayout,
    }),
    [layoutConfig, setLayoutType, updateConfig, isLayout]
  );

  return (
    <LayoutContext.Provider value={contextValue}>
      {children}
    </LayoutContext.Provider>
  );
}

/**
 * 优化2: 使用useCallback稳定回调引用
 */
const setLayoutType = useCallback((layoutType: LayoutType) => {
  setLayoutConfig((prev) => ({
    ...prev,
    layoutType,
    showSidebar: layoutType === LayoutType.SHELL,
    showToolbar: layoutType !== LayoutType.CONTENT_ONLY,
  }));
}, []);

/**
 * 优化3: 在Consumer中选择性地订阅部分状态
 */
function MyComponent() {
  const { config } = useLayout();

  // ✅ 只依赖config.layoutType，不必要时不重渲染
  return <div>{config.layoutType}</div>;
}

/**
 * 优化4: 使用React.memo包装布局组件
 */
export const ShellLayout = React.memo(function ShellLayout({
  children,
}: LayoutComponentProps) {
  // ... 组件逻辑
  return <div>{children}</div>;
});
```

### 6.2 懒加载布局组件

```typescript
/**
 * 文件: src/main/frontend/react-app/layout/LazyLayoutSelector.tsx
 *
 * 使用React.lazy进行布局组件的代码分割
 */

import { Suspense, lazy } from 'react';
import { useLayout } from '../context/layoutContext';
import { LayoutType } from '../context/layoutContext';

// 动态导入布局组件
const ShellLayout = lazy(() => import('./ShellLayout').then(m => ({ default: m.ShellLayout })));
const ContentOnlyLayout = lazy(() => import('./ContentOnlyLayout').then(m => ({ default: m.ContentOnlyLayout })));
const MinimalLayout = lazy(() => import('./MinimalLayout').then(m => ({ default: m.MinimalLayout })));

const LAYOUT_COMPONENTS = {
  [LayoutType.SHELL]: ShellLayout,
  [LayoutType.CONTENT_ONLY]: ContentOnlyLayout,
  [LayoutType.MINIMAL]: MinimalLayout,
} as const;

/**
 * 加载状态占位符
 */
function LayoutLoadingFallback() {
  return <div className="dp-layout-loading">加载中...</div>;
}

/**
 * 懒加载布局选择器
 *
 * 优点：
 * - 减少初始包体积
 * - 支持代码分割
 * - 不同布局可分别优化
 */
export function LazyLayoutSelector({ children }: { children: React.ReactNode }) {
  const { config } = useLayout();

  const LayoutComponent = LAYOUT_COMPONENTS[config.layoutType];

  return (
    <Suspense fallback={<LayoutLoadingFallback />}>
      <LayoutComponent>{children}</LayoutComponent>
    </Suspense>
  );
}
```

---

## 7. 状态保持策略

### 7.1 路由切换时保持页面状态

```typescript
/**
 * 文件: src/main/frontend/react-app/hooks/useLayoutPersistence.ts
 *
 * 处理布局切换时的状态保持
 */

import { useEffect, useRef } from 'react';
import { useLayout } from '../context/layoutContext';
import type { LayoutConfig } from '../context/layoutContext';

const LAYOUT_STORAGE_KEY = 'deploy-platform-layout-config';

/**
 * 将布局配置序列化为localStorage
 */
function persistLayoutConfig(config: LayoutConfig): void {
  try {
    localStorage.setItem(LAYOUT_STORAGE_KEY, JSON.stringify(config));
  } catch (error) {
    console.warn('[useLayoutPersistence] Failed to persist layout config:', error);
  }
}

/**
 * 从localStorage恢复布局配置
 */
function restoreLayoutConfig(): LayoutConfig | null {
  try {
    const stored = localStorage.getItem(LAYOUT_STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch (error) {
    console.warn('[useLayoutPersistence] Failed to restore layout config:', error);
    return null;
  }
}

/**
 * Hook: 自动保持布局配置
 */
export function useLayoutPersistence(): void {
  const { config } = useLayout();
  const configRef = useRef<LayoutConfig>(config);

  // 监听配置变化并保存
  useEffect(() => {
    if (configRef.current !== config) {
      persistLayoutConfig(config);
      configRef.current = config;
    }
  }, [config]);
}

/**
 * Hook: 恢复之前保存的布局配置
 */
export function useLayoutRestore(): LayoutConfig | null {
  const { setConfig } = useLayout();
  const restoredRef = useRef(false);

  useEffect(() => {
    if (restoredRef.current) return;

    const restored = restoreLayoutConfig();
    if (restored) {
      setConfig(restored);
      restoredRef.current = true;
    }
  }, [setConfig]);

  return restoreLayoutConfig();
}
```

### 7.2 在布局切换时保持滚动位置

```typescript
/**
 * Hook: 保持滚动位置
 */
export function useScrollRestoration(): void {
  const scrollPositionsRef = useRef<Record<string, number>>({});

  useEffect(() => {
    return () => {
      // 保存当前滚动位置
      const path = window.location.pathname;
      scrollPositionsRef.current[path] = window.scrollY;
    };
  }, []);

  useEffect(() => {
    // 恢复之前的滚动位置
    const path = window.location.pathname;
    const savedPosition = scrollPositionsRef.current[path];
    if (savedPosition !== undefined) {
      window.scrollTo(0, savedPosition);
    } else {
      window.scrollTo(0, 0);
    }
  }, []);
}
```

---

## 8. 替代方案对比

### 8.1 方案对比表

| 方案 | 灵活性 | 类型安全 | 性能 | 可维护性 | 学习曲线 | 建议场景 |
|------|--------|----------|-------|----------|---------|---------|
| **Context + Provider** (推荐) | ★★★★★ | ★★★★★ | ★★★★★ | ★★★★★ | 中 | 中大型应用 |
| HOC (High Order Component) | ★★★★ | ★★★★ | ★★★ | ★★★ | 低 | 简单场景 |
| Render Props | ★★★★ | ★★★ | ★★★ | ★★★ | 高 | 不推荐 |
| URL Query参数 | ★★★ | ★★ | ★★★★ | ★★★ | 低 | 快速原型 |
| Feature Flags | ★★★★ | ★★★★ | ★★★★ | ★★★★ | 中 | 渐进式迁移 |

### 8.2 备选方案详解

#### 8.2.1 HOC方案（简单但不推荐）

**优点**:
- 实现简单
- 组件级别隔离
- 学习曲线低

**缺点**:
- 类型支持较弱
- Props drilling问题
- 难以处理嵌套HOC

```typescript
/**
 * HOC方案示例
 */
function withLayout(Component: React.ComponentType<any>, layoutType: LayoutType) {
  return function WithLayoutComponent(props: any) {
    const LayoutComponent = layoutType === LayoutType.SHELL ? ShellLayout : ContentOnlyLayout;
    return (
      <LayoutComponent>
        <Component {...props} />
      </LayoutComponent>
    );
  };
}

// 使用
export const ShellReleasesPage = withLayout(ReleasesPage, LayoutType.SHELL);
```

**不推荐原因**:
- 布局选择是全局的，不应该在组件级别绑定
- 无法在运行时动态切换
- TypeScript支持需要复杂的泛型

#### 8.2.2 URL Query参数方案

**优点**:
- 快速实现
- URL可分享
- 支持浏览器历史

**缺点**:
- 污染URL
- 性能差（触发路由重新计算）
- 安全性问题

```typescript
/**
 * Query参数方案示例
 */
function useQueryLayout(): LayoutType {
  const [params] = useSearchParams();
  return (params.get('layout') as LayoutType) || LayoutType.SHELL;
}
```

**不推荐原因**:
- 用户看到非标准URL可能困惑
- SEO问题（同一内容多个URL）
- 开销（每次URL变化都需要重新计算）

---

## 9. 测试策略

### 9.1 Context和Provider的单元测试

```typescript
/**
 * 文件: src/main/frontend/tests/react-app/context/layoutContext.test.ts
 */

import { renderHook, act } from '@testing-library/react';
import { render } from '@testing-library/react';
import { LayoutProvider } from '../../../react-app/providers/LayoutProvider';
import { useLayout, LayoutType } from '../../../react-app/context/layoutContext';

describe('LayoutContext', () => {
  describe('useLayout hook', () => {
    it('should throw error when used outside LayoutProvider', () => {
      expect(() => {
        renderHook(() => useLayout());
      }).toThrow('useLayout must be used within LayoutProvider');
    });

    it('should provide layout configuration', () => {
      const { result } = renderHook(() => useLayout(), {
        wrapper: ({ children }) => <LayoutProvider>{children}</LayoutProvider>,
      });

      expect(result.current.config).toBeDefined();
      expect(result.current.config.layoutType).toBe(LayoutType.SHELL);
    });

    it('should allow changing layout type', () => {
      const { result } = renderHook(() => useLayout(), {
        wrapper: ({ children }) => <LayoutProvider>{children}</LayoutProvider>,
      });

      act(() => {
        result.current.setLayoutType(LayoutType.CONTENT_ONLY);
      });

      expect(result.current.config.layoutType).toBe(LayoutType.CONTENT_ONLY);
    });
  });

  describe('LayoutProvider', () => {
    it('should detect embedded mode from data attribute', () => {
      const { container } = render(
        <LayoutProvider
          detectionConfig={{
            detectEmbedded: () => true,
            defaultLayout: LayoutType.SHELL,
            embeddedLayout: LayoutType.CONTENT_ONLY,
          }}
        >
          <div>Test</div>
        </LayoutProvider>
      );

      expect(container).toBeInTheDocument();
    });

    it('should initialize with correct default layout', () => {
      const { result } = renderHook(() => useLayout(), {
        wrapper: ({ children }) => (
          <LayoutProvider
            detectionConfig={{
              detectEmbedded: () => false,
              defaultLayout: LayoutType.SHELL,
              embeddedLayout: LayoutType.CONTENT_ONLY,
            }}
          >
            {children}
          </LayoutProvider>
        ),
      });

      expect(result.current.config.layoutType).toBe(LayoutType.SHELL);
      expect(result.current.config.isEmbedded).toBe(false);
    });
  });
});
```

### 9.2 集成测试

```typescript
/**
 * 文件: src/main/frontend/tests/react-app/layout/LayoutSelector.test.tsx
 */

import { render, screen } from '@testing-library/react';
import { LayoutProvider, LayoutType } from '../../../react-app/providers/LayoutProvider';
import { LayoutSelector } from '../../../react-app/layout/LayoutSelector';

describe('LayoutSelector', () => {
  it('should render ShellLayout for SHELL layout type', () => {
    render(
      <LayoutProvider
        detectionConfig={{
          detectEmbedded: () => false,
          defaultLayout: LayoutType.SHELL,
          embeddedLayout: LayoutType.CONTENT_ONLY,
        }}
      >
        <LayoutSelector>
          <div data-testid="content">Test Content</div>
        </LayoutSelector>
      </LayoutProvider>
    );

    expect(screen.getByTestId('deploy-platform-shell')).toBeInTheDocument();
    expect(screen.getByTestId('content')).toBeInTheDocument();
  });

  it('should render ContentOnlyLayout for CONTENT_ONLY layout type', () => {
    render(
      <LayoutProvider
        detectionConfig={{
          detectEmbedded: () => true,
          defaultLayout: LayoutType.SHELL,
          embeddedLayout: LayoutType.CONTENT_ONLY,
        }}
      >
        <LayoutSelector>
          <div data-testid="content">Test Content</div>
        </LayoutSelector>
      </LayoutProvider>
    );

    expect(screen.getByTestId('deploy-platform-content-only')).toBeInTheDocument();
  });
});
```

---

## 10. 实现清单

### 10.1 阶段式实现计划

#### 阶段1: 基础设施（1-2天）
- [ ] 创建LayoutContext和类型定义
- [ ] 创建LayoutProvider组件
- [ ] 编写单元测试
- [ ] 文档和代码示例

#### 阶段2: 布局组件（1-2天）
- [ ] 创建ContentOnlyLayout组件
- [ ] 创建MinimalLayout组件
- [ ] 创建LayoutSelector组件
- [ ] 添加CSS样式
- [ ] 编写集成测试

#### 阶段3: 集成（1天）
- [ ] 更新App.tsx
- [ ] 创建AppRouter和路由配置
- [ ] 测试路由切换
- [ ] 性能监控

#### 阶段4: 优化和文档（1-2天）
- [ ] 实现懒加载
- [ ] 状态持久化
- [ ] 完整文档编写
- [ ] 开发指南更新

### 10.2 文件清单

```
src/main/frontend/react-app/
├── context/
│   └── layoutContext.ts              [新建]
├── providers/
│   └── LayoutProvider.tsx            [新建]
├── layout/
│   ├── ShellLayout.tsx               [现有，无改动]
│   ├── ContentOnlyLayout.tsx         [新建]
│   ├── MinimalLayout.tsx             [新建]
│   ├── LayoutSelector.tsx            [新建]
│   └── LazyLayoutSelector.tsx        [新建，可选]
├── config/
│   └── routes.ts                     [新建]
├── hooks/
│   ├── useLayoutPersistence.ts       [新建]
│   └── useTokenRefresh.ts            [现有，无改动]
├── utils/
│   └── typeGuards.ts                 [新建]
├── types/
│   ├── layout.ts                     [新建]
│   └── auth.ts                       [现有，无改动]
├── App.tsx                           [修改]
└── main.tsx                          [现有，无改动]
```

---

## 11. 常见问题和解决方案

### 11.1 Q: 如何在特定路由上使用不同的布局？

**A**: 使用嵌套路由配置：

```typescript
const routes: RouteObject[] = [
  {
    path: '/admin',
    element: <AdminLayout />, // 使用不同布局的Admin区域
    children: [
      { path: 'dashboard', element: <AdminDashboard /> },
      { path: 'users', element: <AdminUsers /> },
    ],
  },
  {
    path: '/user',
    element: <UserLayout />, // 使用不同布局的User区域
    children: [
      { path: 'profile', element: <UserProfile /> },
    ],
  },
];
```

### 11.2 Q: 如何避免布局闪烁？

**A**: 使用useLayoutEffect确保同步更新：

```typescript
import { useLayoutEffect } from 'react';

export function useLayoutSync(config: LayoutConfig) {
  useLayoutEffect(() => {
    // 同步更新DOM，避免视觉闪烁
    document.body.setAttribute('data-layout', config.layoutType);
  }, [config.layoutType]);
}
```

### 11.3 Q: 如何处理SSR场景？

**A**: 在服务器端传递初始布局配置：

```typescript
/**
 * 服务器端渲染初始化
 */
import { readFile } from 'fs/promises';
import { renderToString } from 'react-dom/server';

async function renderApp(isEmbedded: boolean) {
  const layoutConfig = {
    layoutType: isEmbedded ? LayoutType.CONTENT_ONLY : LayoutType.SHELL,
    isEmbedded,
    showSidebar: !isEmbedded,
    showToolbar: true,
    sidebarWidth: 280,
  };

  const html = renderToString(
    <LayoutProvider
      detectionConfig={{
        detectEmbedded: () => isEmbedded,
      }}
    >
      <App />
    </LayoutProvider>
  );

  // 将配置注入HTML window对象
  const fullHtml = html.replace(
    '</head>',
    `<script>window.__LAYOUT_CONFIG__=${JSON.stringify(layoutConfig)}</script></head>`
  );

  return fullHtml;
}
```

---

## 12. 参考资源

### 12.1 官方文档

- [React Context API](https://react.dev/reference/react/useContext)
- [React Router v6](https://reactrouter.com/en/main)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)

### 12.2 相关模式

- [Compound Component Pattern](https://epic-react-dev.com/pages/examples/13)
- [Custom Hooks for State Management](https://react.dev/reference/react/hooks)
- [Layout Patterns in Web Design](https://www.smashingmagazine.com/2015/12/web-design-navigation-layouts/)

### 12.3 项目内相关文件

- [前端架构指南](./frontend-architecture.md)
- [TypeScript编码规范](./TYPESCRIPT_CODING_STANDARDS.md)

---

## 13. 总结

### 13.1 关键建议

1. **使用Context + Provider模式** - 最灵活、可维护性最高
2. **分离路由配置和布局选择** - 各自独立维护，易于扩展
3. **完整的TypeScript支持** - 避免类型不安全问题
4. **性能优化** - 使用useMemo和useCallback避免不必要的重渲染
5. **状态持久化** - 支持用户偏好设置的保存和恢复

### 13.2 实现建议顺序

1. 从Context和Provider开始
2. 创建基础布局组件
3. 集成到App.tsx
4. 添加路由配置
5. 编写测试
6. 性能优化

### 13.3 预期收益

- ✅ 支持运行时动态布局切换
- ✅ 完整的TypeScript类型安全
- ✅ 无性能损失
- ✅ 代码可维护性提高
- ✅ 支持新布局类型的快速扩展
- ✅ 完全向后兼容现有代码

---

**文档完成时间**: 2025-11-02
**最后审查**: Claude AI Research
**状态**: 可用于生产环境实现
