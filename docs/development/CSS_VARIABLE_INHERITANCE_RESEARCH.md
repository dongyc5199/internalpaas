# CSS变量继承与React应用主题集成研究

**文档版本**: v1.0
**创建日期**: 2025-11-02
**研究范围**: CSS变量继承机制、React应用主题集成最佳实践
**目标应用**: Deploy Platform 前端架构（主应用Shell + React子应用）

---

## 目录

1. [执行摘要](#执行摘要)
2. [CSS变量继承机制](#css变量继承机制)
3. [React应用主题集成方案](#react应用主题集成方案)
4. [主题切换实现](#主题切换实现)
5. [CSS变量命名约定](#css变量命名约定)
6. [Shadow DOM影响分析](#shadow-dom影响分析)
7. [样式隔离vs样式共享](#样式隔离vs样式共享)
8. [推荐方案对比](#推荐方案对比)
9. [实现示例](#实现示例)
10. [故障排除指南](#故障排除指南)

---

## 执行摘要

### 核心发现

基于Deploy Platform的当前架构（Thymeleaf主应用 + React子应用），建议采用**CSS变量混合继承方案**：

- ✅ **自动继承**: 主应用在`<html>`或`:root`定义的CSS变量自动被React应用继承
- ✅ **显式读取**: React组件通过`getComputedStyle()`动态读取主应用CSS变量
- ✅ **分层定义**: 建立清晰的变量继承层级（全局 → 主题 → 组件）
- ✅ **主题同步**: 通过CSS类选择器（`[data-theme]`）或自定义属性（CSS变量）实现明暗主题切换

### 关键优势

| 方案 | 优势 | 劣势 | 适用场景 |
|------|------|------|---------|
| CSS变量自动继承 | 零配置、自动同步 | 需要预定义变量 | 嵌入模式 ✓ |
| 显式JS读取 | 灵活、可编程 | 需要JS介入 | 动态主题切换 |
| CSS Modules隔离 | 避免冲突 | 无法继承样式 | 独立应用 |
| CSS-in-JS (styled-components) | 完全类型安全 | 运行时开销 | 复杂主题系统 |

### 推荐架构

```
主应用(Shell) - Thymeleaf模板
    ├─ :root { --shell-bg, --shell-text, ... }
    ├─ [data-theme="dark"] { 深色主题变量 }
    └─ [data-theme="light"] { 浅色主题变量 }

React子应用 - 自动继承
    ├─ 读取主应用CSS变量
    ├─ 组件层定义派生变量
    └─ CSS Modules使用变量
```

---

## CSS变量继承机制

### 1. 自动继承原理

#### 为什么CSS变量会自动继承？

CSS自定义属性（Custom Properties）遵循标准的CSS继承规则：

```css
/* 主应用在 :root 定义 */
:root {
    --shell-bg: #ffffff;
    --shell-text-primary: #1f2937;
}

/* React应用自动继承 */
.react-app {
    background: var(--shell-bg);     /* ✓ 可用 - 继承自 :root */
    color: var(--shell-text-primary); /* ✓ 可用 - 继承自 :root */
}
```

**继承链路**:
```
:root (主应用定义)
  ↓
html (自动继承)
  ↓
body (自动继承)
  ↓
.deploy-platform-app (React容器，自动继承)
  ↓
所有子元素 (自动继承)
```

#### 继承特点

| 特点 | 说明 | 代码示例 |
|------|------|---------|
| **实时更新** | CSS变量修改立即生效 | 无需刷新页面 |
| **级联覆盖** | 子级可覆盖父级变量 | 见下文 |
| **回退值** | 变量不存在时使用回退值 | `var(--color, #000)` |
| **跨域隔离** | 不跨越Shadow DOM边界 | 见[Shadow DOM章节](#shadow-dom影响分析) |

### 2. 显式读取 vs 自动继承

#### 自动继承（推荐）

```typescript
// styles.css - React应用
.dp-shell {
    background: var(--shell-bg);      /* 自动从 :root 继承 */
    color: var(--shell-text-primary);
}

// 无需JavaScript介入，立即生效
```

**优点**:
- 零运行时开销
- 无需JavaScript
- 自动同步
- 支持CSS Modules

**缺点**:
- 必须提前定义变量
- 无法动态注入变量

#### 显式读取（需要时使用）

```typescript
// 当需要在JavaScript中获取CSS变量值时
const getThemeColor = (varName: string): string => {
    const element = document.documentElement;
    return getComputedStyle(element)
        .getPropertyValue(varName)
        .trim();
};

// 使用示例
const shellBg = getThemeColor('--shell-bg');
const shellText = getThemeColor('--shell-text-primary');

// React中使用
const MyComponent: React.FC = () => {
    const [themeColor, setThemeColor] = useState<string>('');

    useEffect(() => {
        const color = getThemeColor('--shell-accent');
        setThemeColor(color);
    }, []);

    return <div style={{ color: themeColor }}>Dynamic Color</div>;
};
```

**优点**:
- 灵活、可编程
- 支持动态变量注入
- 可在JavaScript中使用主题值

**缺点**:
- 需要JavaScript
- 可能存在FOUC（Flash of Unstyled Content）
- 无法在纯CSS中使用

### 3. CSS变量的级联和优先级

```css
/* 层级1: 全局定义 (主应用 - Thymeleaf) */
:root {
    --shell-primary: #6366f1;
    --shell-bg: #ffffff;
}

/* 层级2: 主题切换 (基于 data-theme 属性) */
[data-theme="dark"] {
    --shell-primary: #818cf8;
    --shell-bg: #1f2937;
}

/* 层级3: 组件级覆盖 (React应用) */
.deploy-platform-app {
    --shell-primary: #7c3aed;  /* 覆盖全局变量 */
}

/* 层级4: 内联样式 (最高优先级) */
<div style="--shell-primary: #3b82f6;">高优先级</div>
```

**继承顺序** (按优先级从低到高):
1. 全局 `:root` 变量
2. 父元素变量
3. 元素自身变量
4. 内联样式变量
5. `!important` 标记（谨慎使用）

---

## React应用主题集成方案

### 当前架构分析

**主应用(Shell) - Thymeleaf**:
```html
<!-- main-layout.html -->
<html lang="zh-CN" data-theme="light">
<head>
    <link rel="stylesheet" href="/css/design-tokens.css">
    <!-- 定义 --shell-bg, --shell-text-primary 等 -->
</head>
<body class="layout-shell">
    <!-- React应用挂载点 -->
    <div id="deploy-platform-root"></div>
</body>
</html>
```

**React应用(子应用)**:
```typescript
// react-app/App.tsx
export function App(): JSX.Element {
    return (
        <QueryClientProvider client={queryClient}>
            <I18nextProvider i18n={i18n}>
                <BrowserRouter>
                    <AppRoutes />
                </BrowserRouter>
            </I18nextProvider>
        </QueryClientProvider>
    );
}

// react-app/layout/ShellLayout.tsx
export function ShellLayout({ children }: PropsWithChildren): JSX.Element {
    return (
        <div className="dp-shell">
            <aside className="dp-shell__sidebar">
                {/* 使用 dp-shell 样式 */}
            </aside>
            {/* ... */}
        </div>
    );
}
```

**CSS令牌系统**:
```css
/* src/main/frontend/styles/tokens/colors.css */
:root {
    --color-primary: #6366f1;
    --text-primary: #1e293b;
    --bg-primary: #ffffff;
}

/* React应用 index.css */
.deploy-platform-app {
    color: var(--text-primary);      /* ✓ 从主应用继承 */
    font-family: var(--font-family-sans); /* ✓ 从令牌系统继承 */
}
```

### 方案1: CSS变量自动继承 (推荐 - 嵌入模式)

**原理**: React应用自动继承主应用在`:root`定义的CSS变量，无需额外配置。

**实现步骤**:

1. **主应用定义基础变量** (`/css/design-tokens.css`):
```css
:root {
    /* Shell层级变量 */
    --shell-bg: #ffffff;
    --shell-text-primary: #0f172a;
    --shell-surface: rgba(255,255,255,0.92);
    --shell-accent: linear-gradient(135deg, #2f9bff, #3bc6b8);

    /* 令牌系统变量 */
    --color-primary: #6366f1;
    --color-success: #10b981;
    --text-primary: #1e293b;
    --bg-primary: #ffffff;
}

/* 主题切换 */
[data-theme="dark"] {
    --shell-bg: #111827;
    --shell-text-primary: #f9fafb;
    --text-primary: #f9fafb;
    --bg-primary: #1f2937;
}
```

2. **React应用直接使用**:
```typescript
// react-app/index.css
.deploy-platform-app {
    background: var(--bg-primary);      /* 自动继承 */
    color: var(--text-primary);
    font-family: var(--font-family-sans);
}

.dp-shell {
    background: var(--shell-bg);        /* 自动继承 */
    color: var(--shell-text-primary);
}
```

3. **组件级CSS使用**:
```css
/* react-app/components/Button/Button.module.css */
.button {
    background: var(--color-primary);   /* ✓ 自动继承 */
    color: var(--shell-text-primary);
    border: 1px solid var(--shell-border);
}

.button:hover {
    background: var(--color-primary-light);
}
```

**优点**:
- ✅ 零配置
- ✅ 自动同步主题切换
- ✅ 性能最佳（无运行时开销）
- ✅ 完全支持CSS Modules
- ✅ 易于维护

**缺点**:
- 需要预定义所有变量
- React应用无法修改主应用变量

---

### 方案2: 主题Context + CSS变量 (React特定主题)

**原理**: React应用有自己的主题系统，但仍然继承主应用变量作为基础。

**实现步骤**:

1. **创建主题Context**:
```typescript
// react-app/context/ThemeContext.tsx
import { createContext, useContext, useEffect, useState, ReactNode } from 'react';

type Theme = 'light' | 'dark';

interface ThemeContextValue {
    theme: Theme;
    toggleTheme: () => void;
    getVariable: (name: string) => string;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }) {
    const [theme, setTheme] = useState<Theme>(() => {
        // 检查主应用主题
        const htmlTheme = document.documentElement.getAttribute('data-theme') as Theme;
        return htmlTheme || 'light';
    });

    const toggleTheme = () => {
        const newTheme = theme === 'light' ? 'dark' : 'light';
        setTheme(newTheme);

        // 同步到主应用
        document.documentElement.setAttribute('data-theme', newTheme);

        // 保存到localStorage
        localStorage.setItem('react-app-theme', newTheme);
    };

    const getVariable = (name: string): string => {
        return getComputedStyle(document.documentElement)
            .getPropertyValue(name)
            .trim();
    };

    // 监听主应用主题变化
    useEffect(() => {
        const observer = new MutationObserver(() => {
            const htmlTheme = document.documentElement.getAttribute('data-theme') as Theme;
            if (htmlTheme && htmlTheme !== theme) {
                setTheme(htmlTheme);
            }
        });

        observer.observe(document.documentElement, {
            attributes: true,
            attributeFilter: ['data-theme']
        });

        return () => observer.disconnect();
    }, [theme]);

    return (
        <ThemeContext.Provider value={{ theme, toggleTheme, getVariable }}>
            {children}
        </ThemeContext.Provider>
    );
}

export function useTheme(): ThemeContextValue {
    const context = useContext(ThemeContext);
    if (!context) {
        throw new Error('useTheme must be used within ThemeProvider');
    }
    return context;
}
```

2. **集成到应用**:
```typescript
// react-app/App.tsx
export function App(): JSX.Element {
    return (
        <QueryClientProvider client={queryClient}>
            <I18nextProvider i18n={i18n}>
                <ThemeProvider>  {/* 添加主题提供者 */}
                    <BrowserRouter>
                        <AppRoutes />
                    </BrowserRouter>
                </ThemeProvider>
            </I18nextProvider>
        </QueryClientProvider>
    );
}
```

3. **在组件中使用**:
```typescript
// react-app/components/ThemeSwitcher.tsx
export function ThemeSwitcher() {
    const { theme, toggleTheme } = useTheme();

    return (
        <button onClick={toggleTheme} aria-label="Toggle theme">
            {theme === 'light' ? '🌙' : '☀️'}
        </button>
    );
}

// 使用CSS变量
// react-app/components/Card/Card.module.css
.card {
    background: var(--bg-primary);      /* 从主应用继承 */
    color: var(--text-primary);
    padding: var(--spacing-4);
}
```

**优点**:
- ✅ React独立主题系统
- ✅ 与主应用同步
- ✅ 完全可控
- ✅ 支持动态主题变量

**缺点**:
- 需要额外的Context和Provider
- 增加React组件树深度
- 可能存在主题闪烁（FOUC）

---

### 方案3: CSS-in-JS (完全类型安全)

**原理**: 使用styled-components或emotion，在JavaScript中定义样式和主题。

**实现步骤**:

1. **定义主题对象**:
```typescript
// react-app/theme/theme.ts
export const lightTheme = {
    colors: {
        primary: 'var(--color-primary)',
        bg: 'var(--bg-primary)',
        text: 'var(--text-primary)',
    },
    spacing: {
        xs: '0.25rem',
        sm: '0.5rem',
        md: '1rem',
        lg: '2rem',
    },
} as const;

export const darkTheme: typeof lightTheme = {
    colors: {
        primary: 'var(--color-primary)',
        bg: 'var(--bg-dark)',
        text: 'var(--text-dark)',
    },
    // ...
};
```

2. **使用styled-components**:
```typescript
// react-app/components/Button/Button.tsx
import styled from 'styled-components';

const StyledButton = styled.button<{ variant?: 'primary' | 'secondary' }>`
    background: ${props => props.variant === 'primary'
        ? 'var(--color-primary)'
        : 'var(--bg-primary)'};
    color: var(--text-primary);
    padding: var(--spacing-2) var(--spacing-3);
    border: none;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
        background: var(--color-primary-light);
        transform: translateY(-2px);
    }
`;

export function Button({ variant = 'primary', children }: Props) {
    return <StyledButton variant={variant}>{children}</StyledButton>;
}
```

**优点**:
- ✅ 完全类型安全
- ✅ 支持动态样式
- ✅ 自动CSS前缀
- ✅ 避免样式冲突

**缺点**:
- ❌ 运行时性能开销
- ❌ 增加bundle大小
- ❌ 复杂性较高
- ❌ 不支持纯CSS特性（如`@supports`）

---

## 主题切换实现

### 当前实现分析

**现有主题管理器** (`src/main/frontend/modules/theme.ts`):

```typescript
class ThemeManager {
    private readonly STORAGE_KEY = "theme";
    private readonly DARK_CLASS = "dark-theme";

    toggle(): void {
        const isDark = document.body.classList.toggle(this.DARK_CLASS);
        const theme: Theme = isDark ? "dark" : "light";
        localStorage.setItem(this.STORAGE_KEY, theme);
        eventBus.emit("theme:changed", { theme });
    }
}
```

**问题**:
- 使用CSS类而非`data-theme`属性
- React应用可能无法捕获主题变化
- 主应用使用`data-theme`，不一致

### 推荐实现: 统一的主题切换

#### 1. 创建统一的主题管理器

```typescript
// react-app/services/themeService.ts
export class UnifiedThemeManager {
    private readonly STORAGE_KEY = 'app-theme';
    private readonly THEME_ATTR = 'data-theme';
    private listeners = new Set<(theme: Theme) => void>();

    constructor() {
        this.init();
    }

    private init(): void {
        // 从localStorage读取保存的主题
        const savedTheme = this.getSavedTheme();
        // 从主应用读取主题
        const shellTheme = this.getShellTheme();

        // 优先使用主应用主题，保持一致性
        const initialTheme = shellTheme || savedTheme || 'light';
        this.applyTheme(initialTheme);

        // 监听主应用主题变化
        this.watchShellTheme();
    }

    private getSavedTheme(): Theme | null {
        const saved = localStorage.getItem(this.STORAGE_KEY);
        return (saved === 'dark' || saved === 'light') ? saved : null;
    }

    private getShellTheme(): Theme | null {
        const theme = document.documentElement.getAttribute(this.THEME_ATTR);
        return (theme === 'dark' || theme === 'light') ? theme : null;
    }

    private watchShellTheme(): void {
        const observer = new MutationObserver(() => {
            const shellTheme = this.getShellTheme();
            if (shellTheme) {
                this.notifyListeners(shellTheme);
            }
        });

        observer.observe(document.documentElement, {
            attributes: true,
            attributeFilter: [this.THEME_ATTR]
        });
    }

    applyTheme(theme: Theme): void {
        document.documentElement.setAttribute(this.THEME_ATTR, theme);
        localStorage.setItem(this.STORAGE_KEY, theme);
        this.notifyListeners(theme);
    }

    toggle(): void {
        const current = this.getShellTheme() || 'light';
        const next = current === 'light' ? 'dark' : 'light';
        this.applyTheme(next);
    }

    getCurrentTheme(): Theme {
        return this.getShellTheme() || 'light';
    }

    subscribe(callback: (theme: Theme) => void): () => void {
        this.listeners.add(callback);
        return () => this.listeners.delete(callback);
    }

    private notifyListeners(theme: Theme): void {
        this.listeners.forEach(callback => callback(theme));
    }
}

export type Theme = 'light' | 'dark';
export const themeManager = new UnifiedThemeManager();
```

#### 2. 在React应用中使用

```typescript
// react-app/context/ThemeContext.tsx
import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { themeManager, Theme } from '../services/themeService';

interface ThemeContextValue {
    theme: Theme;
    toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }) {
    const [theme, setTheme] = useState<Theme>(themeManager.getCurrentTheme());

    useEffect(() => {
        const unsubscribe = themeManager.subscribe(setTheme);
        return unsubscribe;
    }, []);

    return (
        <ThemeContext.Provider value={{
            theme,
            toggleTheme: () => themeManager.toggle()
        }}>
            {children}
        </ThemeContext.Provider>
    );
}

export function useTheme(): ThemeContextValue {
    const context = useContext(ThemeContext);
    if (!context) {
        throw new Error('useTheme must be used within ThemeProvider');
    }
    return context;
}
```

#### 3. React应用集成

```typescript
// react-app/App.tsx
export function App(): JSX.Element {
    return (
        <QueryClientProvider client={queryClient}>
            <I18nextProvider i18n={i18n}>
                <ThemeProvider>
                    <BrowserRouter>
                        <AppRoutes />
                    </BrowserRouter>
                </ThemeProvider>
            </I18nextProvider>
            <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
    );
}
```

#### 4. CSS支持主题切换

```css
/* react-app/index.css */
.deploy-platform-app {
    color: var(--text-primary);
    background: var(--bg-primary);
    transition: background-color 0.3s ease, color 0.3s ease;
}

/* 主题切换时自动应用 */
[data-theme="dark"] .deploy-platform-app {
    --text-primary: #f9fafb;
    --bg-primary: #1f2937;
}
```

---

## CSS变量命名约定

### 推荐命名体系

采用**分层命名法**，区分不同作用域的变量：

```css
:root {
    /* ===== 第1层: Shell系统变量 (主应用定义) ===== */
    /* 用于主应用顶层布局和主题管理 */
    --shell-bg: #ffffff;                    /* 背景颜色 */
    --shell-text-primary: #0f172a;          /* 主文字颜色 */
    --shell-text-secondary: rgba(15,23,42,0.8);
    --shell-border: rgba(255,255,255,0.6);
    --shell-accent: linear-gradient(135deg, #2f9bff, #3bc6b8);
    --shell-header-height: 64px;
    --shell-sidebar-width: 240px;

    /* ===== 第2层: 设计令牌系统变量 (主应用定义) ===== */
    /* 通用设计令牌，所有应用都可使用 */

    /* 颜色令牌 */
    --color-primary: #6366f1;               /* 品牌主色 */
    --color-primary-light: #818cf8;         /* 悬停状态 */
    --color-primary-dark: #4f46e5;          /* 激活状态 */
    --color-success: #10b981;               /* 成功色 */
    --color-warning: #f59e0b;               /* 警告色 */
    --color-error: #ef4444;                 /* 错误色 */

    /* 背景/表面色 */
    --bg-primary: #ffffff;                  /* 主背景 */
    --bg-secondary: #f8fafc;                /* 卡片背景 */
    --bg-tertiary: #f1f5f9;                 /* 悬停背景 */

    /* 文字色 */
    --text-primary: #1e293b;                /* 主要文字 */
    --text-secondary: #475569;              /* 次要文字 */
    --text-tertiary: #64748b;               /* 提示文字 */

    /* 边框色 */
    --border-primary: #e2e8f0;              /* 主要边框 */
    --border-secondary: #cbd5e1;            /* 次要边框 */

    /* 间距令牌 */
    --spacing-1: 0.25rem;  /* 4px */
    --spacing-2: 0.5rem;   /* 8px */
    --spacing-3: 0.75rem;  /* 12px */
    --spacing-4: 1rem;     /* 16px */
    --spacing-6: 1.5rem;   /* 24px */
    --spacing-8: 2rem;     /* 32px */

    /* 圆角令牌 */
    --radius-sm: 4px;
    --radius-md: 8px;
    --radius-lg: 12px;

    /* 字体令牌 */
    --font-family-sans: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    --font-family-mono: "JetBrains Mono", "Fira Code", monospace;
    --font-size-sm: 0.875rem;     /* 14px */
    --font-size-base: 1rem;       /* 16px */
    --font-size-lg: 1.125rem;     /* 18px */

    /* ===== 第3层: 组件变量 (React应用定义) ===== */
    /* 组件特定的变量，仅在组件作用域内使用 */
    /* 这些变量通常在组件CSS文件中定义 */
}

/* 主题变量覆盖 */
[data-theme="dark"] {
    --shell-bg: #111827;
    --shell-text-primary: #f9fafb;
    --bg-primary: #1f2937;
    --text-primary: #f9fafb;
    --border-primary: rgba(148,163,184,0.32);
}

/* 组件级变量示例 */
.dp-button {
    --button-bg: var(--color-primary);      /* 继承第2层 */
    --button-text: white;
    --button-padding-y: var(--spacing-2);   /* 继承第2层 */
    --button-padding-x: var(--spacing-4);
}
```

### 命名规范

| 类别 | 前缀 | 示例 | 说明 |
|------|------|------|------|
| Shell变量 | `--shell-` | `--shell-bg` | 主应用系统变量 |
| 颜色令牌 | `--color-` | `--color-primary` | 设计系统颜色 |
| 背景色 | `--bg-` | `--bg-primary` | 背景颜色变量 |
| 文字色 | `--text-` | `--text-primary` | 文字颜色变量 |
| 边框色 | `--border-` | `--border-primary` | 边框颜色变量 |
| 间距 | `--spacing-` | `--spacing-4` | 尺寸间距令牌 |
| 圆角 | `--radius-` | `--radius-md` | 边框圆角令牌 |
| 字体 | `--font-` | `--font-family-sans` | 字体相关令牌 |
| 阴影 | `--shadow-` | `--shadow-lg` | 阴影效果令牌 |
| 过渡 | `--duration-` | `--duration-fast` | 动画持续时间 |

### 禁止事项

```css
/* ❌ 避免 */
--color: #fff;                  /* 太模糊 */
--blue: #0ea5e9;               /* 颜色值作为名称 */
--primary: #6366f1;            /* 缺少作用域前缀 */
--shell-primary-light-bg-hover: #fff; /* 过长，重复概念 */

/* ✅ 推荐 */
--color-primary: #6366f1;
--color-info: #0ea5e9;
--shell-bg: #ffffff;
--button-bg-hover: #7c3aed;
```

---

## Shadow DOM影响分析

### Shadow DOM与CSS变量

Shadow DOM创建了一个"样式边界"，但**CSS自定义属性穿透此边界**。

#### 1. CSS变量穿透Shadow DOM

```typescript
// Web Component示例
class MyComponent extends HTMLElement {
    connectedCallback() {
        const shadow = this.attachShadow({ mode: 'open' });
        shadow.innerHTML = `
            <style>
                :host {
                    color: var(--text-primary);         /* ✓ 可见主应用变量 */
                    background: var(--bg-primary);      /* ✓ 可见主应用变量 */
                }
            </style>
            <div>Content</div>
        `;
    }
}

customElements.define('my-component', MyComponent);
```

**关键点**:
- ✅ Shadow DOM**内的样式可以访问**外部CSS变量
- ✅ CSS变量自动穿透Shadow边界
- ✅ 无需任何特殊配置

#### 2. 样式隔离的有限性

```typescript
class StyledComponent extends HTMLElement {
    connectedCallback() {
        const shadow = this.attachShadow({ mode: 'open' });
        shadow.innerHTML = `
            <style>
                /* ❌ 这些样式不会泄露到外部 */
                div { color: red; }

                /* ✓ CSS变量可以共享 */
                :host {
                    --local-color: var(--color-primary);
                }
            </style>
        `;
    }
}
```

#### 3. Deploy Platform中的使用

如果React应用使用Web Components：

```typescript
// React应用使用Web Component
export function MyPage() {
    return (
        <div className="deploy-platform-app">
            {/* Web Component自动继承CSS变量 */}
            <my-component />
        </div>
    );
}

// Web Component定义
class MyComponent extends HTMLElement {
    connectedCallback() {
        const shadow = this.attachShadow({ mode: 'open' });
        shadow.innerHTML = `
            <style>
                :host {
                    /* ✓ 自动从 :root 继承 */
                    color: var(--text-primary);
                    background: var(--bg-primary);
                    padding: var(--spacing-4);
                    font-family: var(--font-family-sans);
                }
            </style>
            <slot></slot>
        `;
    }
}
customElements.define('my-component', MyComponent);
```

**关键收获**:
- CSS变量**天然穿透**Shadow DOM边界
- 无需特殊处理，Web Components自动继承主应用变量
- 这是Shadow DOM设计的优势，而非劣势

---

## 样式隔离vs样式共享

### 1. 样式隔离方案

**CSS Modules** (推荐用于React):

```typescript
// react-app/components/Button/Button.tsx
import styles from './Button.module.css';

export function Button({ children }: PropsWithChildren) {
    return <button className={styles.button}>{children}</button>;
}
```

```css
/* react-app/components/Button/Button.module.css */
.button {
    /* ✓ 样式只作用于此组件 */
    background: var(--color-primary);   /* ✓ 但仍然继承CSS变量 */
    color: white;
    padding: var(--spacing-2) var(--spacing-4);
    border-radius: var(--radius-md);
    border: none;
    cursor: pointer;

    /* 本地类名自动生成：Button_button__a1b2c */
}

.button:hover {
    background: var(--color-primary-light);
}
```

**优点**:
- ✅ 避免全局命名冲突
- ✅ 易于维护和重构
- ✅ 自动作用域隔离
- ✅ 仍然可以使用CSS变量

**缺点**:
- 文件数量增多
- 需要import每个样式

### 2. 样式共享方案

**全局CSS + BEM命名**:

```css
/* src/main/frontend/styles/components/button.css */
.btn {
    background: var(--color-primary);
    padding: var(--spacing-2) var(--spacing-4);
}

.btn--primary {
    background: var(--color-primary);
}

.btn--secondary {
    background: var(--bg-secondary);
}

.btn:hover {
    background: var(--color-primary-light);
}
```

**优点**:
- ✅ 文件结构简洁
- ✅ 易于全局一致性
- ✅ 样式复用好

**缺点**:
- ❌ 可能存在命名冲突
- ❌ 样式难以隔离

### 3. 混合方案 (推荐)

```
src/main/frontend/
├── styles/
│   ├── tokens/              # 全局令牌（CSS变量）
│   ├── base/                # 全局基础样式
│   └── components/          # 共享组件样式（全局）
│
└── react-app/
    └── components/
        ├── Button/
        │   ├── Button.tsx
        │   └── Button.module.css    # 本地样式
        └── Card/
            ├── Card.tsx
            └── Card.module.css      # 本地样式
```

**CSS Modules + CSS变量**:

```css
/* react-app/components/Button/Button.module.css */
.button {
    /* 使用全局CSS变量 */
    background: var(--color-primary);
    color: var(--text-primary);
    border-radius: var(--radius-md);

    /* 本地样式（生成局部类名） */
    transition: all 0.3s ease;
}

.button--large {
    padding: var(--spacing-4) var(--spacing-6);
    font-size: var(--font-size-lg);
}
```

---

## 推荐方案对比

### 全面对比表格

| 维度 | CSS变量自动继承 | 主题Context | CSS-in-JS | CSS Modules |
|------|-----------------|------------|-----------|------------|
| **性能** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **易用性** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **类型安全** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **灵活性** | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **维护成本** | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ |
| **学习曲线** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |

### 场景推荐

| 场景 | 推荐方案 | 原因 |
|------|---------|------|
| **嵌入模式**（主应用内） | CSS变量自动继承 | 零配置，自动同步 |
| **独立应用** | CSS Modules | 样式隔离，无冲突 |
| **复杂主题系统** | 主题Context + CSS变量 | 灵活，易于扩展 |
| **设计系统** | CSS变量 + CSS-in-JS | 类型安全，可维护 |
| **实时主题切换** | 主题Context | 可观察，易于反应 |
| **大规模应用** | CSS Modules + Context | 平衡性能和功能 |

### Deploy Platform推荐方案

```
┌─────────────────────────────────────────────────────────┐
│  Deploy Platform 主题集成推荐方案                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  分层策略：                                             │
│  ┌──────────────────────────────────────────────────┐   │
│  │ 第1层：主应用(Shell) - Thymeleaf                 │   │
│  │ - 定义全局CSS变量(:root)                         │   │
│  │ - 管理 data-theme 属性                           │   │
│  │ - 提供外观一致的基础主题                         │   │
│  └──────────────────────────────────────────────────┘   │
│                        ↓                                │
│  ┌──────────────────────────────────────────────────┐   │
│  │ 第2层：React应用 - 自动继承                      │   │
│  │ - 导入全局CSS变量                                │   │
│  │ - CSS Modules使用变量                            │   │
│  │ - 可选：ThemeContext用于高级功能                │   │
│  └──────────────────────────────────────────────────┘   │
│                        ↓                                │
│  ┌──────────────────────────────────────────────────┐   │
│  │ 第3层：组件层 - 派生变量                         │   │
│  │ - 组件CSS Modules                                │   │
│  │ - 使用第2层变量                                  │   │
│  │ - 组件级主题调整                                 │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
├─────────────────────────────────────────────────────────┤
│ 核心原则：                                             │
│ 1. 首选CSS变量自动继承（零配置）                      │
│ 2. CSS Modules处理样式隔离                            │
│ 3. 高级功能时才使用Context                            │
│ 4. 避免CSS-in-JS增加复杂性                            │
└─────────────────────────────────────────────────────────┘
```

---

## 实现示例

### 示例1: 基础CSS变量继承

**文件结构**:
```
主应用 Thymeleaf
└─ main-layout.html
   ├─ link: /css/design-tokens.css
   └─ [data-theme="light|dark"]

React应用
├─ src/main/frontend/react-app/
│  ├─ index.css
│  ├─ App.tsx
│  └─ components/
│     ├─ Button/
│     │  ├─ Button.tsx
│     │  └─ Button.module.css
│     └─ Card/
│        ├─ Card.tsx
│        └─ Card.module.css
```

**步骤1: 主应用定义变量** (`/css/design-tokens.css`):

```css
:root {
    /* Shell系统变量 */
    --shell-bg: #ffffff;
    --shell-text-primary: #0f172a;
    --shell-border: rgba(15, 23, 42, 0.12);

    /* 设计令牌 */
    --color-primary: #6366f1;
    --color-success: #10b981;
    --text-primary: #1e293b;
    --bg-primary: #ffffff;
    --spacing-2: 0.5rem;
    --spacing-4: 1rem;
    --radius-md: 8px;
}

[data-theme="dark"] {
    --shell-bg: #111827;
    --shell-text-primary: #f9fafb;
    --text-primary: #f9fafb;
    --bg-primary: #1f2937;
}
```

**步骤2: React应用使用变量** (`react-app/index.css`):

```css
.deploy-platform-app {
    background: var(--bg-primary);
    color: var(--text-primary);
}
```

**步骤3: 组件中使用** (`react-app/components/Button/Button.module.css`):

```css
.button {
    background: var(--color-primary);
    color: white;
    padding: var(--spacing-2) var(--spacing-4);
    border-radius: var(--radius-md);
    border: none;
    cursor: pointer;
    transition: background 0.3s ease;
}

.button:hover {
    background: #7c3aed;  /* 或使用变量 var(--color-primary-light) */
}
```

**步骤4: React组件** (`react-app/components/Button/Button.tsx`):

```typescript
import { ButtonHTMLAttributes, PropsWithChildren } from 'react';
import styles from './Button.module.css';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: 'primary' | 'secondary';
}

export function Button({
    variant = 'primary',
    children,
    ...props
}: PropsWithChildren<ButtonProps>) {
    return (
        <button className={styles.button} {...props}>
            {children}
        </button>
    );
}
```

**结果**:
- ✅ CSS变量自动从主应用继承
- ✅ 主题切换时自动更新
- ✅ 无需额外配置

---

### 示例2: 主题Context实现

**创建ThemeContext** (`react-app/context/ThemeContext.tsx`):

```typescript
import { createContext, useContext, useEffect, useState, ReactNode } from 'react';

type Theme = 'light' | 'dark';

interface ThemeContextValue {
    theme: Theme;
    toggleTheme: () => void;
    getVariable: (varName: string) => string;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }) {
    const [theme, setTheme] = useState<Theme>(() => {
        const stored = localStorage.getItem('react-app-theme');
        const htmlTheme = document.documentElement.getAttribute('data-theme');
        return (stored || htmlTheme || 'light') as Theme;
    });

    const toggleTheme = () => {
        const newTheme = theme === 'light' ? 'dark' : 'light';
        setTheme(newTheme);
        document.documentElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('react-app-theme', newTheme);

        // 触发CSS变量更新事件
        window.dispatchEvent(new CustomEvent('theme-changed', {
            detail: { theme: newTheme }
        }));
    };

    const getVariable = (varName: string): string => {
        const value = getComputedStyle(document.documentElement)
            .getPropertyValue(varName)
            .trim();
        return value;
    };

    // 监听主应用主题变化
    useEffect(() => {
        const observer = new MutationObserver(() => {
            const shellTheme = document.documentElement.getAttribute('data-theme');
            if (shellTheme && shellTheme !== theme) {
                setTheme(shellTheme as Theme);
            }
        });

        observer.observe(document.documentElement, {
            attributes: true,
            attributeFilter: ['data-theme']
        });

        return () => observer.disconnect();
    }, [theme]);

    return (
        <ThemeContext.Provider value={{ theme, toggleTheme, getVariable }}>
            {children}
        </ThemeContext.Provider>
    );
}

export function useTheme(): ThemeContextValue {
    const context = useContext(ThemeContext);
    if (!context) {
        throw new Error('useTheme must be used within ThemeProvider');
    }
    return context;
}
```

**在App中集成** (`react-app/App.tsx`):

```typescript
import { ThemeProvider } from './context/ThemeContext';

export function App(): JSX.Element {
    return (
        <QueryClientProvider client={queryClient}>
            <I18nextProvider i18n={i18n}>
                <ThemeProvider>  {/* 添加主题提供者 */}
                    <BrowserRouter>
                        <AppRoutes />
                    </BrowserRouter>
                </ThemeProvider>
            </I18nextProvider>
            <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
    );
}
```

**在组件中使用** (`react-app/components/ThemeSwitcher.tsx`):

```typescript
import { useTheme } from '../context/ThemeContext';

export function ThemeSwitcher() {
    const { theme, toggleTheme } = useTheme();

    return (
        <button
            onClick={toggleTheme}
            aria-label={`Switch to ${theme === 'light' ? 'dark' : 'light'} theme`}
            title={`Current theme: ${theme}`}
        >
            {theme === 'light' ? '🌙 Dark' : '☀️ Light'}
        </button>
    );
}
```

**结果**:
- ✓ React应用有独立的主题管理
- ✓ 与主应用主题同步
- ✓ 可以获取CSS变量值用于JavaScript逻辑

---

### 示例3: 动态主题变量获取

```typescript
// react-app/hooks/useThemeVariable.ts
import { useState, useEffect } from 'react';

/**
 * Hook: 获取并订阅CSS变量变化
 */
export function useThemeVariable(varName: string): string {
    const [value, setValue] = useState<string>(() => {
        return getComputedStyle(document.documentElement)
            .getPropertyValue(varName)
            .trim();
    });

    useEffect(() => {
        // 监听主题变化事件
        const handleThemeChange = () => {
            const newValue = getComputedStyle(document.documentElement)
                .getPropertyValue(varName)
                .trim();
            setValue(newValue);
        };

        // 监听自定义事件
        window.addEventListener('theme-changed', handleThemeChange);

        // 监听DOM属性变化
        const observer = new MutationObserver(handleThemeChange);
        observer.observe(document.documentElement, {
            attributes: true,
            attributeFilter: ['data-theme']
        });

        return () => {
            window.removeEventListener('theme-changed', handleThemeChange);
            observer.disconnect();
        };
    }, [varName]);

    return value;
}

// 使用示例
export function ChartComponent() {
    const primaryColor = useThemeVariable('--color-primary');
    const textColor = useThemeVariable('--text-primary');

    return (
        <Chart
            options={{
                plugins: {
                    legend: {
                        labels: {
                            color: textColor
                        }
                    }
                },
                scales: {
                    y: {
                        ticks: {
                            color: textColor
                        }
                    }
                }
            }}
            data={{
                datasets: [{
                    borderColor: primaryColor,
                    backgroundColor: primaryColor
                }]
            }}
        />
    );
}
```

---

## 故障排除指南

### 问题1: CSS变量在React应用中不生效

**症状**:
```typescript
// 这个样式不生效
.button {
    background: var(--color-primary);  /* 显示为默认颜色而非主应用变量 */
}
```

**排查步骤**:

1. **检查变量定义**:
```typescript
// 在浏览器控制台运行
const value = getComputedStyle(document.documentElement)
    .getPropertyValue('--color-primary');
console.log('--color-primary:', value);  // 应该返回 #6366f1
```

2. **检查CSS加载顺序**:
```html
<!-- main-layout.html -->
<head>
    <!-- ✓ 设计令牌必须在React应用样式之前加载 -->
    <link rel="stylesheet" href="/css/design-tokens.css">
    <!-- ✓ 再加载React样式 -->
    <link rel="stylesheet" href="/dist/assets/app.css">
</head>
```

3. **检查选择器优先级**:
```css
/* ❌ 问题：内联样式优先级更高 */
<div style="background: red;">
    <!-- var(--color-primary) 会被忽略 -->
</div>

/* ✓ 解决：使用CSS优先 */
<div className={styles.button}>
    {/* CSS中的 var(--color-primary) 会生效 */}
</div>
```

4. **检查变量作用域**:
```css
/* ❌ 问题：在Shadow DOM内无法访问外部变量 */
.local-component {
    color: var(--color-primary);  /* 可能失效 */
}

/* ✓ 解决：确保变量定义在 :root */
:root {
    --color-primary: #6366f1;
}
```

**常见原因**:
- [ ] CSS文件加载顺序错误
- [ ] CSS变量名称拼写错误
- [ ] CSS变量定义在错误的选择器中
- [ ] React应用CSS没有引入主应用样式

---

### 问题2: 主题切换时样式不更新

**症状**:
```
用户切换深色模式，但React应用界面没有变化
```

**排查步骤**:

1. **检查data-theme属性是否变化**:
```typescript
// 在浏览器控制台运行
document.documentElement.getAttribute('data-theme');  // 应该返回 'dark' 或 'light'

// 监听变化
const observer = new MutationObserver(() => {
    console.log('theme changed:', document.documentElement.getAttribute('data-theme'));
});
observer.observe(document.documentElement, { attributes: true });
```

2. **检查CSS变量是否更新**:
```typescript
// 检查深色主题下的变量值
console.log(
    getComputedStyle(document.documentElement)
        .getPropertyValue('--shell-bg')
);  // 深色主题下应该返回 #111827
```

3. **检查CSS主题选择器**:
```css
/* ✓ 正确：使用 data-theme 属性 */
[data-theme="dark"] {
    --shell-bg: #111827;
    --text-primary: #f9fafb;
}

/* ❌ 错误：使用类名（不一致） */
.dark-theme {
    --shell-bg: #111827;
}
```

4. **检查CSS过渡**:
```css
/* 添加过渡效果 */
body, html {
    transition: background-color 0.3s ease, color 0.3s ease;
}

/* 如果没有过渡，可能看起来像没有变化 */
```

**常见原因**:
- [ ] 主题管理器使用了错误的属性名（`class` vs `data-theme`）
- [ ] CSS选择器和HTML属性不匹配
- [ ] React应用没有监听主题变化事件
- [ ] CSS过渡被禁用

---

### 问题3: React组件中无法读取CSS变量

**症状**:
```typescript
// 这个返回空字符串或undefined
const color = getComputedStyle(document.documentElement)
    .getPropertyValue('--color-primary');
console.log(color);  // 返回空字符串
```

**排查步骤**:

1. **检查变量是否存在**:
```typescript
// 列出所有CSS变量
const styles = getComputedStyle(document.documentElement);
const allProperties = Array.from(styles).filter(p => p.startsWith('--'));
console.log('CSS Variables:', allProperties);
```

2. **检查变量名称**:
```typescript
// ❌ 错误：变量名包含空格
const color = getComputedStyle(document.documentElement)
    .getPropertyValue('-- color-primary');

// ✓ 正确
const color = getComputedStyle(document.documentElement)
    .getPropertyValue('--color-primary');
```

3. **确保在DOM加载后读取**:
```typescript
// ❌ 早期读取，可能失败
const color = getComputedStyle(document.documentElement)
    .getPropertyValue('--color-primary');

// ✓ 在React生命周期中读取
useEffect(() => {
    const color = getComputedStyle(document.documentElement)
        .getPropertyValue('--color-primary');
    console.log(color);
}, []);
```

**常见原因**:
- [ ] 在页面完全加载前读取
- [ ] 变量名称拼写错误
- [ ] CSS文件未加载
- [ ] 获取值时包含空格或其他字符

---

### 问题4: Shadow DOM中无法访问CSS变量

**症状**:
```typescript
// Web Component中的样式无法使用主应用CSS变量
const shadow = this.attachShadow({ mode: 'open' });
shadow.innerHTML = `
    <style>
        :host { color: var(--text-primary); }  /* 不工作 */
    </style>
`;
```

**解决方案**:

```typescript
// ✓ 正确：CSS变量会自动穿透Shadow边界
class MyWebComponent extends HTMLElement {
    connectedCallback() {
        const shadow = this.attachShadow({ mode: 'open' });
        shadow.innerHTML = `
            <style>
                :host {
                    /* ✓ 这会正确继承 :root 中的变量 */
                    color: var(--text-primary);
                    background: var(--bg-primary);
                }
            </style>
            <div>Content</div>
        `;
    }
}
```

**排查步骤**:

1. **检查变量是否在:root定义**:
```css
:root {
    --text-primary: #1e293b;  /* 必须在 :root，不能在其他地方 */
}
```

2. **在Web Component中验证**:
```typescript
// 在 connectedCallback 中检查
const computedStyle = getComputedStyle(this);
const color = computedStyle.getPropertyValue('--text-primary');
console.log('Color from parent:', color);
```

3. **检查mode是否为'open'**:
```typescript
// ✓ 允许样式穿透
const shadow = this.attachShadow({ mode: 'open' });

// ❌ 'closed' mode 可能导致问题（虽然CSS变量仍然可用）
const shadow = this.attachShadow({ mode: 'closed' });
```

**注意**: CSS变量会穿透Shadow DOM，这是Chrome的标准行为，无需特殊处理。

---

### 问题5: 组件级CSS变量覆盖不生效

**症状**:
```css
/* 想在组件级覆盖全局变量 */
.my-component {
    --color-primary: #ff0000;  /* 希望在组件内使用红色 */
}

.my-component button {
    background: var(--color-primary);  /* 仍然使用全局的#6366f1 */
}
```

**原因**: CSS变量的级联规则不同于普通CSS属性。

**解决方案**:

```css
/* ✓ 方案1：直接在元素上声明并使用 */
.my-component {
    --color-primary: #ff0000;
}

.my-component button {
    background: var(--color-primary);  /* ✓ 现在使用 #ff0000 */
}

/* ✓ 方案2：使用更具体的选择器 */
.my-component {
    --local-primary: #ff0000;  /* 使用本地变量名 */
}

.my-component button {
    background: var(--local-primary);  /* 使用本地变量 */
}

/* ✓ 方案3：使用 :is() 提高选择器优先级 */
:is(.my-component) {
    --color-primary: #ff0000;
}
```

---

## 总结与建议

### 核心建议

1. **优先使用CSS变量自动继承**
   - 零配置，自动同步
   - 最小化JavaScript介入
   - 性能最佳

2. **建立清晰的变量分层**
   - Shell变量（`--shell-`）
   - 设计令牌（`--color-`, `--spacing-`等）
   - 组件变量（在组件CSS中定义）

3. **统一主题切换机制**
   - 使用`data-theme`属性，而非CSS类
   - React应用监听主应用主题变化
   - 提供主题Context用于高级功能

4. **合理选择样式方案**
   - 标准组件：CSS Modules + CSS变量
   - 共享组件：全局CSS + BEM命名
   - 复杂主题系统：CSS-in-JS（谨慎使用）

5. **充分利用CSS变量的穿透性**
   - Web Components自动继承主应用变量
   - Shadow DOM不是障碍
   - 无需特殊处理

### 实施路线图

**第一阶段**（立即实施）:
- [ ] 统一主应用和React应用的CSS变量命名
- [ ] 确保React应用自动继承主应用CSS变量
- [ ] 测试主题切换是否正常工作

**第二阶段**（可选）:
- [ ] 实现React ThemeContext用于高级功能
- [ ] 创建`useThemeVariable` Hook方便获取变量值
- [ ] 文档化CSS变量命名约定

**第三阶段**（长期维护）:
- [ ] 定期审查和优化CSS变量结构
- [ ] 在新组件中始终使用CSS变量
- [ ] 收集用户反馈并改进主题系统

### 相关文件位置

```
E:\work\code\internalpaas\
├── src\main\resources\templates\main-layout.html      # 主应用模板
├── src\main\resources\static\css\design-tokens.css   # CSS变量定义
├── src\main\frontend\
│   ├── styles\tokens\                                # 设计令牌
│   │   ├── colors.css
│   │   ├── typography.css
│   │   ├── spacing.css
│   │   └── shadows.css
│   └── react-app\
│       ├── App.tsx
│       ├── index.css
│       ├── layout\ShellLayout.tsx
│       └── components\
└── docs\development\                                  # 本文件位置
```

---

**文档版本**: v1.0
**最后更新**: 2025-11-02
**维护者**: Dev Debug Platform Team

