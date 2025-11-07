# 现有React应用架构分析

**参考项目**: `src/main/frontend/react-app` (部署平台模块)
**分析日期**: 2025-01-04
**目的**: 从现有React代码中提取最佳实践，应用到新的React迁移项目

---

## 📁 项目结构分析

### 现有结构 (`react-app`)

```
react-app/
├── api/                    # API客户端层
│   ├── client.ts          # 基础HTTP客户端
│   ├── policyApi.ts       # 策略API
│   ├── releaseApi.ts      # 发布API
│   └── tokenApi.ts        # Token API
├── components/             # 通用组件
│   ├── Button/
│   ├── Input/
│   ├── Modal/
│   ├── Select/
│   ├── Table/
│   ├── LanguageSwitcher/
│   ├── ContentOnlyLayout.tsx
│   └── LayoutSelector.tsx
├── config/                 # 配置文件
│   ├── queryClient.ts     # React Query配置
│   └── constants.ts       # 全局常量
├── contexts/               # React Context
├── hooks/                  # 自定义Hooks
│   ├── useNavSync.ts      # 导航同步
│   ├── useTokenRefresh.ts # Token刷新
│   └── useEmbedMode.ts    # 嵌入模式检测
├── i18n/                   # 国际化
│   ├── i18n.ts
│   └── locales/
│       ├── zh-CN.json
│       └── en-US.json
├── layout/                 # 布局组件
├── pages/                  # 页面组件
│   ├── OverviewPage.tsx
│   ├── ReleasesPage.tsx
│   ├── ReleaseDetailsPage.tsx
│   └── PoliciesPage.tsx
├── providers/              # Provider包装器
│   └── LayoutProvider.tsx
├── stores/                 # 状态管理
├── types/                  # TypeScript类型定义
│   └── navigation.ts
├── App.tsx                 # 根组件
├── main.tsx                # 应用入口
└── token-bridge.ts         # Token桥接机制
```

---

## 🏗️ 核心架构模式

### 1. API客户端模式 ⭐⭐⭐

**文件**: `api/client.ts`

```typescript
// 关键设计点:
export type Fetcher = <T>(
    input: RequestInfo | URL,
    init?: RequestInit
) => Promise<T>;

// 基于Session的认证 (无需手动处理Authorization header)
export const defaultFetcher: Fetcher = async <T>(
    input: RequestInfo | URL,
    init?: RequestInit
): Promise<T> => {
    const response = await fetch(input, {
        ...init,
        credentials: 'same-origin', // 自动发送JSESSIONID cookie
        headers: {
            'Content-Type': 'application/json',
            ...(init?.headers ?? {})
        }
    });

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || `HTTP ${response.status}`);
    }

    return (await response.json()) as T;
};

// 工厂函数创建API客户端
export const createApiClient = (fetcher: Fetcher = defaultFetcher): ApiClient => ({
    get: async <T>(url: string) => fetcher<T>(url),
    post: async <T>(url: string, body?: unknown) =>
        fetcher<T>(url, {
            method: "POST",
            body: body ? JSON.stringify(body) : undefined
        })
});
```

**优点**:
- ✅ Session-based认证，无需手动管理JWT token
- ✅ 类型安全的泛型返回值
- ✅ 统一错误处理
- ✅ 可测试性强（可注入mock fetcher）

**应用到新项目**:
```typescript
// src/shared/api/client.ts
import { createApiClient } from './fetcher';

export const apiClient = createApiClient();

// 使用示例
const servers = await apiClient.get<Server[]>('/api/servers');
```

---

### 2. React Query配置 ⭐⭐⭐

**文件**: `config/queryClient.ts`

```typescript
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,        // 5分钟内数据保持新鲜
      gcTime: 10 * 60 * 1000,          // 缓存保留10分钟
      retry: 1,                         // 失败重试1次
      refetchOnWindowFocus: process.env.NODE_ENV === 'production',
      refetchOnReconnect: false,
    },
    mutations: {
      retry: 0,  // 用户操作不自动重试
    },
  },
});
```

**关键配置说明**:
- `staleTime: 5min` - 数据新鲜期，期间不会自动refetch
- `gcTime: 10min` - 缓存保留时间（v5改名自cacheTime）
- `retry: 1` - 避免认证失败时过度重试
- `refetchOnWindowFocus: prod only` - 开发模式不自动刷新，提升DX

**应用到新项目**:
直接复用此配置，已经过优化和实战验证。

---

### 3. 导航同步机制 ⭐⭐⭐⭐⭐

**文件**: `hooks/useNavSync.ts`

**核心功能**: 实现主应用（Thymeleaf）与React应用的双向导航同步

```typescript
export function useNavSync(navigate: NavigateFunction, location: Location): void {
    const isSyncingRef = useRef(false);  // 防循环标志
    const prevPathnameRef = useRef(location.pathname);
    const isEmbedded = detectEmbedMode();

    // 方向1: 主应用 → React
    useEffect(() => {
        if (!isEmbedded) return;  // 独立模式跳过

        const handleMainNavChange = (event: Event): void => {
            const { route } = (event as CustomEvent<NavigationEventDetail>).detail;

            // 防循环 + 去重
            if (isSyncingRef.current || route === prevPathnameRef.current) {
                return;
            }

            isSyncingRef.current = true;
            navigate(route);

            // 300ms后重置标志
            setTimeout(() => { isSyncingRef.current = false; }, 300);
        };

        window.addEventListener("main-nav-change", handleMainNavChange);
        return () => window.removeEventListener("main-nav-change", handleMainNavChange);
    }, [navigate, isEmbedded]);

    // 方向2: React → 主应用
    useEffect(() => {
        if (!isEmbedded || location.pathname === prevPathnameRef.current) {
            return;
        }

        if (!isSyncingRef.current) {
            window.dispatchEvent(new CustomEvent("react-nav-change", {
                detail: { route: location.pathname, source: "react" }
            }));
        }

        prevPathnameRef.current = location.pathname;
    }, [location.pathname, isEmbedded]);
}
```

**关键设计点**:
1. **双向通信**: CustomEvent (`main-nav-change` ↔ `react-nav-change`)
2. **防循环**: `isSyncingRef` + 300ms超时
3. **去重**: 只在pathname实际变化时触发
4. **嵌入检测**: 独立模式自动跳过同步逻辑

**应用到新项目**:
这是**关键模式**，用于所有需要与主应用集成的React模块。

---

### 4. 组件设计模式 ⭐⭐⭐

**文件**: `components/Button/Button.tsx`

```typescript
export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'outline';
  size?: 'sm' | 'md' | 'lg';
  fullWidth?: boolean;
  loading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  children?: React.ReactNode;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', size = 'md', loading = false, ... }, ref) => {
    const classNames = [
      styles.button,
      styles[`variant-${variant}`],
      styles[`size-${size}`],
      fullWidth && styles.fullWidth,
      loading && styles.loading,
      className,
    ].filter(Boolean).join(' ');

    return (
      <button ref={ref} className={classNames} disabled={disabled || loading} aria-busy={loading}>
        {loading && <Spinner />}
        {!loading && leftIcon && <span className={styles.leftIcon}>{leftIcon}</span>}
        {children && <span className={styles.content}>{children}</span>}
        {!loading && rightIcon && <span className={styles.rightIcon}>{rightIcon}</span>}
      </button>
    );
  }
);

Button.displayName = 'Button';
```

**最佳实践**:
- ✅ `forwardRef` 支持ref转发
- ✅ `extends HTMLButtonElement` 继承原生属性
- ✅ JSDoc注释 + TypeScript类型
- ✅ CSS Modules作用域样式
- ✅ `aria-busy` 等可访问性属性
- ✅ 显式设置 `displayName` (调试友好)

---

### 5. i18n国际化模式 ⭐⭐⭐

**文件**: `i18n/i18n.ts`

```typescript
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import zhCN from './locales/zh-CN.json';
import enUS from './locales/en-US.json';

i18n
  .use(initReactI18next)
  .init({
    resources: {
      'zh-CN': { translation: zhCN },
      'en-US': { translation: enUS },
    },
    lng: typeof window !== 'undefined'
      ? localStorage.getItem('LANGUAGE') || 'zh-CN'
      : 'zh-CN',
    fallbackLng: 'zh-CN',
    interpolation: { escapeValue: false },
    react: { useSuspense: false },  // SSR兼容
  });

// 自动持久化语言选择
i18n.on('languageChanged', (lng) => {
  localStorage.setItem('LANGUAGE', lng);
});
```

**关键特性**:
- localStorage持久化语言偏好
- SSR兼容 (`typeof window !== 'undefined'`)
- 自动监听语言变化并保存

**使用示例**:
```typescript
import { useTranslation } from 'react-i18next';

function MyComponent() {
  const { t } = useTranslation();
  return <h1>{t('common.welcome')}</h1>;
}
```

---

### 6. 嵌入模式检测 ⭐⭐⭐⭐

**文件**: `hooks/useEmbedMode.ts`

```typescript
export const detectEmbedMode = (): boolean => {
  if (typeof window === 'undefined') return false;

  // 方法1: 检查URL参数
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('embed') === 'true') return true;

  // 方法2: 检查iframe
  if (window.self !== window.top) return true;

  // 方法3: 检查特定DOM容器
  const container = document.getElementById('deploy-platform-root');
  if (container?.dataset?.embed === 'true') return true;

  return false; // 默认独立模式
};
```

**应用场景**:
- 决定是否显示React自己的导航栏
- 决定是否启用与主应用的导航同步
- 根据模式调整布局（ContentOnlyLayout vs MainLayout）

---

## 🎯 推荐应用到新项目的模式

### 高优先级 (必须采用)

1. **API Client模式** ✅
   - 文件位置: `src/shared/api/client.ts`
   - Session-based认证
   - 统一错误处理

2. **React Query配置** ✅
   - 文件位置: `src/shared/config/queryClient.ts`
   - 直接复用现有配置

3. **导航同步机制** ✅
   - 文件位置: `src/shared/hooks/useNavSync.ts`
   - 支持与主应用集成

4. **组件设计模式** ✅
   - forwardRef + displayName
   - CSS Modules
   - 完整的TypeScript类型定义

### 中优先级 (推荐采用)

5. **i18n配置** ✅
   - react-i18next + localStorage持久化

6. **嵌入模式检测** ✅
   - 支持独立 + 嵌入双模式运行

---

## 📋 迁移清单

基于现有架构分析，新React应用需要添加/调整的文件：

### 立即创建

- [x] `src/shared/api/client.ts` - 基于react-app的API客户端 ✅
- [x] `src/shared/api/fetcher.ts` - HTTP客户端抽象层 ✅
- [x] `src/shared/config/queryClient.ts` - 复用React Query配置 ✅
- [x] `src/shared/hooks/useNavSync.ts` - 导航同步Hook ✅
- [x] `src/shared/hooks/useEmbedMode.ts` - 嵌入模式检测 ✅
- [x] `src/shared/types/navigation.ts` - 导航类型定义 ✅
- [ ] `src/i18n/i18n.ts` - i18next配置
- [ ] `src/i18n/locales/zh-CN.json` - 中文翻译
- [ ] `src/i18n/locales/en-US.json` - 英文翻译

### 调整现有文件

- [ ] `src/App.tsx` - 参考react-app的Provider嵌套顺序
- [ ] `src/shared/components/Button/` - 采用forwardRef模式
- [ ] `src/shared/components/` - 所有组件添加displayName

---

## 🔍 关键差异点

### react-app vs 新React应用

| 特性 | react-app (现有) | 新React应用 (计划) |
|------|------------------|-------------------|
| **路由策略** | 嵌入式（basename动态） | 完整SPA（替换Thymeleaf） |
| **认证机制** | Session + Token桥接 | 纯Session（简化） |
| **状态管理** | 轻量（Context + React Query） | Zustand + React Query |
| **组件库** | 自定义组件 | 扩展版组件库（更多组件） |
| **WebSocket** | 无 | STOMP.js集成 |
| **图表库** | 无 | Apache ECharts |

---

## 💡 经验教训

### ✅ 做得好的地方

1. **简洁的API抽象** - `createApiClient`模式易扩展
2. **细粒度的Hooks** - `useNavSync`, `useTokenRefresh`职责单一
3. **类型安全** - 全面的TypeScript类型定义
4. **可访问性** - 组件包含aria属性
5. **调试友好** - `window.__DEBUG__`标志 + displayName

### ⚠️ 需要改进的地方

1. **缺少全局错误边界** - 应在App级别添加
2. **测试覆盖率** - 需要补充单元测试
3. **文档注释** - 部分组件缺少使用示例

---

## 🚀 下一步行动

1. **立即迁移**:
   - 复制`api/client.ts`到新项目
   - 复制`config/queryClient.ts`
   - 复制`hooks/useNavSync.ts`和`hooks/useEmbedMode.ts`

2. **调整优化**:
   - 扩展API客户端支持DELETE、PUT、PATCH方法
   - 添加全局错误边界
   - 完善组件库（添加更多变体）

3. **保持一致**:
   - 组件命名遵循react-app风格
   - 文件组织遵循相同模式
   - TypeScript规范保持统一

---

**参考项目**: `src/main/frontend/react-app`
**文档版本**: v1.0
**最后更新**: 2025-01-04
