# 布局系统迁移指南

**文档版本**: 1.0
**面向对象**: 开发人员
**预计完成时间**: 2-4天
**难度等级**: 中级

---

## 目录

1. [迁移前检查](#迁移前检查)
2. [逐步迁移流程](#逐步迁移流程)
3. [文件夹结构变更](#文件夹结构变更)
4. [代码示例](#代码示例)
5. [测试验证](#测试验证)
6. [回滚方案](#回滚方案)

---

## 迁移前检查

### 1. 依赖版本检查

```bash
# 验证React版本
npm ls react

# 验证React Router版本
npm ls react-router-dom

# 预期版本：
# react: ^18.2.0
# react-router-dom: ^6.0.0
```

### 2. 现状分析

```bash
# 检查现有的Layout使用
grep -r "ShellLayout" src/main/frontend/react-app --include="*.tsx" --include="*.ts"

# 检查是否有其他Layout组件
find src/main/frontend/react-app/layout -type f -name "*.tsx"

# 检查Routes配置
grep -r "Routes" src/main/frontend/react-app --include="*.tsx"
```

### 3. 备份当前状态

```bash
# 创建备份分支
git checkout -b backup/layout-migration-$(date +%Y%m%d)
git push origin backup/layout-migration-$(date +%Y%m%d)

# 或者在当前分支提交现有代码
git add -A
git commit -m "chore: backup before layout migration"
```

---

## 逐步迁移流程

### 步骤 1: 创建Context（第1天上午）

#### 1.1 创建目录结构

```bash
mkdir -p src/main/frontend/react-app/context
mkdir -p src/main/frontend/react-app/providers
mkdir -p src/main/frontend/react-app/config
```

#### 1.2 实现layoutContext.ts

**位置**: `src/main/frontend/react-app/context/layoutContext.ts`

```typescript
/**
 * 布局配置上下文
 */

import { createContext, useContext } from 'react';

export enum LayoutType {
  SHELL = 'shell',
  CONTENT_ONLY = 'content-only',
  MINIMAL = 'minimal',
}

export interface LayoutConfig {
  layoutType: LayoutType;
  isEmbedded: boolean;
  showSidebar: boolean;
  showToolbar: boolean;
  sidebarWidth: number;
}

export interface LayoutContextValue {
  config: LayoutConfig;
  setLayoutType: (layoutType: LayoutType) => void;
  setConfig: (config: LayoutConfig) => void;
  isLayout: (layoutType: LayoutType) => boolean;
}

export const LayoutContext = createContext<LayoutContextValue | undefined>(undefined);

export function useLayout(): LayoutContextValue {
  const context = useContext(LayoutContext);
  if (!context) {
    throw new Error('useLayout must be used within LayoutProvider');
  }
  return context;
}

export function useLayoutSafe(): LayoutContextValue | null {
  return useContext(LayoutContext) ?? null;
}
```

#### 1.3 编写单元测试

**位置**: `src/main/frontend/tests/react-app/context/layoutContext.test.ts`

```typescript
/**
 * Context和Hook的单元测试
 */

import { renderHook } from '@testing-library/react';
import { LayoutProvider } from '../../../react-app/providers/LayoutProvider';
import { useLayout, LayoutType } from '../../../react-app/context/layoutContext';

describe('layoutContext', () => {
  it('should throw error when useLayout is used outside provider', () => {
    expect(() => {
      renderHook(() => useLayout());
    }).toThrow('useLayout must be used within LayoutProvider');
  });

  it('should provide default SHELL layout', () => {
    const { result } = renderHook(() => useLayout(), {
      wrapper: ({ children }) => <LayoutProvider>{children}</LayoutProvider>,
    });

    expect(result.current.config.layoutType).toBe(LayoutType.SHELL);
    expect(result.current.config.showSidebar).toBe(true);
  });
});
```

**验证步骤**:
```bash
npm test -- layoutContext.test.ts
```

### 步骤 2: 创建Provider（第1天下午）

#### 2.1 实现LayoutProvider.tsx

**位置**: `src/main/frontend/react-app/providers/LayoutProvider.tsx`

```typescript
/**
 * 见本文档前面的完整实现
 * 或参考: React_Dynamic_Layout_Selection_Best_Practices.md
 */

import { PropsWithChildren, useState, useEffect, useCallback, useMemo } from 'react';
import { LayoutContext, LayoutConfig, LayoutType } from '../context/layoutContext';
import type { LayoutContextValue } from '../context/layoutContext';

function detectEmbeddedMode(): boolean {
  if (typeof document === 'undefined') {
    return false;
  }

  const container = document.getElementById('deploy-platform-root');
  if (container?.dataset?.embedded === 'true') {
    return true;
  }

  if (typeof window !== 'undefined' && (window as any).__DEPLOY_PLATFORM_EMBEDDED__) {
    return true;
  }

  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search);
    if (params.get('embedded') === 'true') {
      return true;
    }
  }

  return false;
}

function computeInitialLayout(
  isEmbedded: boolean,
  defaultLayout: LayoutType,
  embeddedLayout: LayoutType
): LayoutConfig {
  const layoutType = isEmbedded ? embeddedLayout : defaultLayout;

  return {
    layoutType,
    isEmbedded,
    showSidebar: layoutType === LayoutType.SHELL,
    showToolbar: layoutType !== LayoutType.CONTENT_ONLY,
    sidebarWidth: 280,
  };
}

interface LayoutProviderProps extends PropsWithChildren {
  detectionConfig?: Partial<{
    detectEmbedded: () => boolean;
    defaultLayout: LayoutType;
    embeddedLayout: LayoutType;
  }>;
}

export function LayoutProvider({
  children,
  detectionConfig = {},
}: LayoutProviderProps): JSX.Element {
  const config = {
    detectEmbedded,
    defaultLayout: LayoutType.SHELL,
    embeddedLayout: LayoutType.CONTENT_ONLY,
    ...detectionConfig,
  };

  const [layoutConfig, setLayoutConfig] = useState<LayoutConfig>(() => {
    const isEmbedded = config.detectEmbedded();
    return computeInitialLayout(isEmbedded, config.defaultLayout, config.embeddedLayout);
  });

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
```

#### 2.2 集成测试

**位置**: `src/main/frontend/tests/react-app/providers/LayoutProvider.test.tsx`

```typescript
/**
 * Provider集成测试
 */

import { render, screen } from '@testing-library/react';
import { LayoutProvider, LayoutType } from '../../../react-app/providers/LayoutProvider';
import { useLayout } from '../../../react-app/context/layoutContext';

function TestComponent() {
  const { config } = useLayout();
  return <div data-testid="layout-type">{config.layoutType}</div>;
}

describe('LayoutProvider', () => {
  it('should provide layout context', () => {
    render(
      <LayoutProvider>
        <TestComponent />
      </LayoutProvider>
    );

    expect(screen.getByTestId('layout-type')).toHaveTextContent(LayoutType.SHELL);
  });

  it('should initialize with embedded layout when detected', () => {
    render(
      <LayoutProvider
        detectionConfig={{
          detectEmbedded: () => true,
          defaultLayout: LayoutType.SHELL,
          embeddedLayout: LayoutType.CONTENT_ONLY,
        }}
      >
        <TestComponent />
      </LayoutProvider>
    );

    expect(screen.getByTestId('layout-type')).toHaveTextContent(
      LayoutType.CONTENT_ONLY
    );
  });
});
```

**验证步骤**:
```bash
npm test -- LayoutProvider.test.tsx
```

### 步骤 3: 创建新的Layout组件（第2天上午）

#### 3.1 创建ContentOnlyLayout.tsx

**位置**: `src/main/frontend/react-app/layout/ContentOnlyLayout.tsx`

```typescript
/**
 * 仅内容布局 - 用于嵌入场景
 */

import { PropsWithChildren } from 'react';

interface ContentOnlyLayoutProps extends PropsWithChildren {
  className?: string;
}

export function ContentOnlyLayout({
  children,
  className,
}: ContentOnlyLayoutProps): JSX.Element {
  return (
    <div
      className={['dp-layout-content-only', className].filter(Boolean).join(' ')}
      data-testid="deploy-platform-content-only"
    >
      <main data-testid="deploy-platform-content">
        {children}
      </main>
    </div>
  );
}
```

#### 3.2 创建MinimalLayout.tsx

**位置**: `src/main/frontend/react-app/layout/MinimalLayout.tsx`

```typescript
/**
 * 最小化布局 - 仅包含工具栏
 */

import { PropsWithChildren } from 'react';
import { useTranslation } from 'react-i18next';
import { LanguageSwitcher } from '../components/LanguageSwitcher/LanguageSwitcher';

interface MinimalLayoutProps extends PropsWithChildren {
  className?: string;
}

export function MinimalLayout({
  children,
  className,
}: MinimalLayoutProps): JSX.Element {
  const { t } = useTranslation();

  return (
    <div
      className={['dp-layout-minimal', className].filter(Boolean).join(' ')}
      data-testid="deploy-platform-minimal"
    >
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

#### 3.3 添加CSS样式

**位置**: `src/main/frontend/react-app/index.css` (追加)

```css
/**
 * 布局相关样式
 */

/* ContentOnlyLayout 样式 */
.dp-layout-content-only {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--background, #fff);
}

.dp-layout-content-only main {
  flex: 1;
  padding: 2rem;
  overflow-y: auto;
}

/* MinimalLayout 样式 */
.dp-layout-minimal {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--background, #fff);
}

.dp-minimal-layout__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem 2rem;
  border-bottom: 1px solid var(--border, #e5e7eb);
  background-color: var(--card, #fff);
}

.dp-minimal-layout__title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--foreground, #000);
}

.dp-minimal-layout__subtitle {
  margin: 0.25rem 0 0;
  font-size: 0.875rem;
  color: var(--muted-foreground, #6b7280);
}

.dp-minimal-layout__actions {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.dp-layout-minimal main {
  flex: 1;
  padding: 2rem;
  overflow-y: auto;
}

/* ShellLayout 样式（现有，保持不变） */
/* ... */
```

### 步骤 4: 创建LayoutSelector（第2天下午）

#### 4.1 实现LayoutSelector.tsx

**位置**: `src/main/frontend/react-app/layout/LayoutSelector.tsx`

```typescript
/**
 * 动态布局选择器
 */

import { PropsWithChildren } from 'react';
import { useLayout } from '../context/layoutContext';
import { LayoutType } from '../context/layoutContext';
import { ShellLayout } from './ShellLayout';
import { ContentOnlyLayout } from './ContentOnlyLayout';
import { MinimalLayout } from './MinimalLayout';

const LAYOUT_COMPONENTS = {
  [LayoutType.SHELL]: ShellLayout,
  [LayoutType.CONTENT_ONLY]: ContentOnlyLayout,
  [LayoutType.MINIMAL]: MinimalLayout,
} as const;

interface LayoutSelectorProps extends PropsWithChildren {
  fallbackLayout?: LayoutType;
}

export function LayoutSelector({
  children,
  fallbackLayout = LayoutType.SHELL,
}: LayoutSelectorProps): JSX.Element {
  const { config } = useLayout();

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

#### 4.2 单元测试

**位置**: `src/main/frontend/tests/react-app/layout/LayoutSelector.test.tsx`

```typescript
/**
 * LayoutSelector测试
 */

import { render, screen } from '@testing-library/react';
import { LayoutProvider, LayoutType } from '../../../react-app/providers/LayoutProvider';
import { LayoutSelector } from '../../../react-app/layout/LayoutSelector';

describe('LayoutSelector', () => {
  it('should render ShellLayout by default', () => {
    render(
      <LayoutProvider detectionConfig={{ detectEmbedded: () => false }}>
        <LayoutSelector>
          <div data-testid="test-content">Content</div>
        </LayoutSelector>
      </LayoutProvider>
    );

    expect(screen.getByTestId('deploy-platform-shell')).toBeInTheDocument();
    expect(screen.getByTestId('test-content')).toBeInTheDocument();
  });

  it('should render ContentOnlyLayout when embedded', () => {
    render(
      <LayoutProvider detectionConfig={{ detectEmbedded: () => true }}>
        <LayoutSelector>
          <div data-testid="test-content">Content</div>
        </LayoutSelector>
      </LayoutProvider>
    );

    expect(screen.getByTestId('deploy-platform-content-only')).toBeInTheDocument();
  });

  it('should throw error for invalid layout type', () => {
    // Mock useLayout to return invalid layout type
    expect(() => {
      render(
        <LayoutProvider>
          <LayoutSelector>Content</LayoutSelector>
        </LayoutProvider>
      );
    }).not.toThrow(); // 因为default会使用fallback
  });
});
```

### 步骤 5: 创建路由配置（第3天上午）

#### 5.1 实现routes.ts

**位置**: `src/main/frontend/react-app/config/routes.ts`

```typescript
/**
 * 应用路由配置
 * 与布局选择完全分离
 */

import type { RouteObject } from 'react-router-dom';
import { Navigate } from 'react-router-dom';
import { OverviewPage } from '../pages/OverviewPage';
import { ReleaseDetailsPlaceholder, ReleasesPage } from '../pages/ReleasesPage';
import { ReleaseDetailsPage } from '../pages/ReleaseDetailsPage';
import { PoliciesPage } from '../pages/PoliciesPage';

export const ROUTE_IDS = {
  ROOT: 'root',
  OVERVIEW: 'overview',
  RELEASES: 'releases',
  RELEASE_DETAILS: 'release-details',
  POLICIES: 'policies',
} as const;

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

export function getRoute(routeId: string): RouteObject | undefined {
  return appRoutes.find((route) => route.id === routeId);
}

export function getTopLevelRoutePaths(): string[] {
  return appRoutes
    .filter((route) => route.path && route.path !== '*')
    .map((route) => route.path!)
    .filter(Boolean);
}
```

#### 5.2 创建AppRouter.tsx（可选）

**位置**: `src/main/frontend/react-app/layout/AppRouter.tsx`

```typescript
/**
 * 应用路由器（可选的抽象）
 * 如果使用useRoutes替代<Routes>元素
 */

import { useRoutes } from 'react-router-dom';
import { appRoutes } from '../config/routes';
import { LayoutSelector } from './LayoutSelector';
import { LayoutType } from '../context/layoutContext';

interface AppRouterProps {
  fallbackLayout?: LayoutType;
}

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

### 步骤 6: 更新App.tsx（第3天下午）

#### 6.1 修改App.tsx

**位置**: `src/main/frontend/react-app/App.tsx`

```typescript
/**
 * 应用根组件 - 迁移版本
 *
 * 变更：
 * 1. 添加LayoutProvider包装
 * 2. 使用LayoutSelector替代ShellLayout
 * 3. 保持其他功能不变
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
 */
function InternalApp(): JSX.Element {
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

#### 6.2 验证现有App.test.tsx

```bash
npm test -- App.test.tsx
```

### 步骤 7: 全面测试和验证（第4天）

#### 7.1 运行所有测试

```bash
# 运行全部测试
npm test

# 运行特定测试文件
npm test -- layout

# 覆盖率报告
npm test -- --coverage --watchAll=false
```

#### 7.2 本地开发测试

```bash
# 启动开发服务器
npm run dev

# 在不同模式下测试
# 1. 标准模式（应显示完整Shell Layout）
open http://localhost:3000

# 2. 嵌入模式（应显示Content Only Layout）
open http://localhost:3000?embedded=true

# 3. 检查浏览器控制台是否有错误
```

#### 7.3 性能测试

```bash
# 使用React DevTools进行性能分析
# 1. 打开React DevTools
# 2. 切换到Profiler标签
# 3. 记录渲染过程
# 4. 验证没有不必要的重渲染

# 检查包体积
npm run build
du -sh dist/
```

---

## 文件夹结构变更

### 迁移前

```
src/main/frontend/react-app/
├── App.tsx
├── layout/
│   └── ShellLayout.tsx      (硬编码在App.tsx中)
├── pages/
│   ├── OverviewPage.tsx
│   ├── ReleasesPage.tsx
│   └── ...
└── ...
```

### 迁移后

```
src/main/frontend/react-app/
├── App.tsx                  (更新：添加LayoutProvider)
├── context/
│   └── layoutContext.ts     (新建)
├── providers/
│   └── LayoutProvider.tsx   (新建)
├── layout/
│   ├── ShellLayout.tsx      (现有，无改动)
│   ├── ContentOnlyLayout.tsx (新建)
│   ├── MinimalLayout.tsx    (新建)
│   ├── LayoutSelector.tsx   (新建)
│   └── AppRouter.tsx        (新建，可选)
├── config/
│   └── routes.ts            (新建)
├── pages/
│   ├── OverviewPage.tsx
│   ├── ReleasesPage.tsx
│   └── ...
└── ...
```

---

## 代码示例

### 在组件中使用Layout Context

```typescript
/**
 * 示例：检查当前布局类型
 */

import { useLayout, LayoutType } from '../context/layoutContext';

function MyComponent() {
  const { config, isLayout } = useLayout();

  return (
    <div>
      {/* 方式1：检查layoutType */}
      {config.layoutType === LayoutType.SHELL && (
        <div>显示在Shell Layout中</div>
      )}

      {/* 方式2：使用isLayout函数 */}
      {isLayout(LayoutType.CONTENT_ONLY) && (
        <div>显示在Content Only Layout中</div>
      )}

      {/* 方式3：检查布尔标志 */}
      {config.showSidebar && (
        <aside>侧边栏内容</aside>
      )}
    </div>
  );
}
```

### 动态切换布局

```typescript
/**
 * 示例：运行时切换布局
 */

import { useLayout, LayoutType } from '../context/layoutContext';

function LayoutSwitcher() {
  const { setLayoutType } = useLayout();

  return (
    <div>
      <button onClick={() => setLayoutType(LayoutType.SHELL)}>
        完整Layout
      </button>
      <button onClick={() => setLayoutType(LayoutType.CONTENT_ONLY)}>
        仅内容
      </button>
      <button onClick={() => setLayoutType(LayoutType.MINIMAL)}>
        最小化
      </button>
    </div>
  );
}
```

---

## 测试验证

### Checklist

- [ ] Context和Provider创建成功
- [ ] 布局组件创建成功
- [ ] LayoutSelector正常工作
- [ ] App.tsx成功集成LayoutProvider
- [ ] 所有路由仍然可访问
- [ ] 在标准模式下显示ShellLayout
- [ ] 在嵌入模式下显示ContentOnlyLayout
- [ ] 没有控制台错误或警告
- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 性能指标正常
- [ ] 浏览器兼容性测试通过

### 手动测试步骤

#### 标准模式测试

```bash
# 1. 启动应用
npm run dev

# 2. 访问所有页面
# - http://localhost:3000/overview
# - http://localhost:3000/releases
# - http://localhost:3000/settings/policies

# 3. 验证：
# ✓ 侧边栏显示
# ✓ 顶部工具栏显示
# ✓ 导航链接可点击
# ✓ 页面内容正确加载
```

#### 嵌入模式测试

```bash
# 1. 访问嵌入模式URL
# http://localhost:3000?embedded=true

# 2. 验证：
# ✓ 侧边栏不显示
# ✓ 工具栏不显示
# ✓ 仅显示主内容区域
# ✓ 页面仍然可以正常导航（如果有内部链接）
```

#### 容器属性检测测试

```html
<!-- 在HTML中设置嵌入属性 -->
<div id="deploy-platform-root" data-embedded="true"></div>
```

---

## 回滚方案

### 如果需要回滚

#### 快速回滚（完全回到之前的状态）

```bash
# 1. 回滚到备份分支
git checkout backup/layout-migration-<date>

# 2. 或使用git reset
git reset --hard <commit-hash-before-migration>

# 3. 重新安装依赖（如果有变化）
npm install

# 4. 验证应用
npm run dev
```

#### 部分回滚（保留部分新代码）

```bash
# 1. 恢复App.tsx到原始状态
git checkout HEAD~<number> -- src/main/frontend/react-app/App.tsx

# 2. 删除新建的文件
rm -rf src/main/frontend/react-app/context
rm -rf src/main/frontend/react-app/providers
rm -rf src/main/frontend/react-app/layout/ContentOnlyLayout.tsx
rm -rf src/main/frontend/react-app/layout/MinimalLayout.tsx
rm -rf src/main/frontend/react-app/layout/LayoutSelector.tsx

# 3. 验证应用
npm run dev
```

### 回滚后验证

```bash
# 清理依赖和缓存
rm -rf node_modules
npm install

# 重新构建
npm run build

# 运行测试
npm test

# 启动应用
npm run dev
```

---

## 预期时间表

| 阶段 | 任务 | 预计时间 | 负责人 |
|------|------|---------|--------|
| 1 | Context和Provider创建 | 1-2小时 | Developer |
| 2 | 新布局组件创建 | 2-3小时 | Developer |
| 3 | LayoutSelector实现 | 1小时 | Developer |
| 4 | 路由配置提取 | 1小时 | Developer |
| 5 | App.tsx更新 | 30分钟 | Developer |
| 6 | 测试和验证 | 2-3小时 | QA/Developer |
| 7 | 文档更新 | 1小时 | Developer |
| **总计** | | **2-4天** | |

---

## 常见问题解决

### Q: 迁移过程中应用无法启动

**A**: 检查以下几点：
```bash
# 1. 检查是否有TypeScript错误
npm run build

# 2. 检查是否缺少依赖
npm install

# 3. 检查console是否有运行时错误
npm run dev

# 4. 验证imports是否正确
grep -r "from '.*LayoutProvider" src/
```

### Q: LayoutProvider显示错误"useLayout must be used within LayoutProvider"

**A**: 确保LayoutProvider在Component之上：
```typescript
// ✓ 正确
<LayoutProvider>
  <YourComponent />
</LayoutProvider>

// ✗ 错误
<YourComponent />
<LayoutProvider>
  {/* ... */}
</LayoutProvider>
```

### Q: 布局不显示或样式错乱

**A**: 检查CSS是否加载：
```typescript
// 确保在App.tsx中导入样式
import "./index.css"; // 必须包含新的布局样式
```

---

## 后续优化建议

1. **代码分割**: 使用React.lazy进行布局组件的懒加载
2. **状态持久化**: 使用useLayoutEffect保存用户偏好的布局选择
3. **主题集成**: 将布局配置与主题系统集成
4. **动画过渡**: 添加布局切换时的过渡动画
5. **通知系统**: 在布局无法加载时显示用户友好的错误消息

---

**版本历史**:
- v1.0 (2025-11-02): 初始版本

**联系方式**:
- 技术支持: 查看项目文档
- 问题报告: 提交GitHub Issue
