# CSS变量实现实践指南

**文档版本**: v1.0
**创建日期**: 2025-11-02
**目标**: 为Deploy Platform项目提供CSS变量继承的具体实现步骤

---

## 目录

1. [当前状态评估](#当前状态评估)
2. [快速开始](#快速开始)
3. [分步实现](#分步实现)
4. [代码示例](#代码示例)
5. [验证清单](#验证清单)
6. [常见问题](#常见问题)

---

## 当前状态评估

### 已有资源分析

#### 主应用(Shell) - Thymeleaf
✓ **优势**:
- 已在`main-layout.html`定义了Shell系统变量（`:root`中的`--shell-*`）
- 已实现基础的`data-theme`属性主题切换机制
- 有设计令牌系统（`/css/design-tokens.css`）

**问题**:
- CSS变量定义分散在多个文件中
- 缺乏统一的变量命名约定
- 主题切换的一致性需要改进

#### React应用
✓ **优势**:
- 已有独立的令牌系统（`src/main/frontend/styles/tokens/`）
- 已实现ThemeManager（`modules/theme.ts`）
- 使用CSS Modules进行样式隔离

**问题**:
- React应用的主题管理器与主应用不同步
- CSS变量未充分利用
- 缺乏明确的变量继承策略

### 改进机会

| 项目 | 当前状态 | 目标状态 | 优先级 |
|------|---------|---------|--------|
| CSS变量统一定义 | 分散 | 集中管理 | 高 |
| 命名约定 | 无 | 清晰约定 | 高 |
| 主题同步 | 不完整 | 完全同步 | 高 |
| 文档化 | 无 | 完整文档 | 中 |

---

## 快速开始

### 5分钟快速实现

如果你只有5分钟，按以下步骤操作：

#### 步骤1: 验证CSS变量是否生效

```typescript
// 在浏览器F12控制台运行
getComputedStyle(document.documentElement)
    .getPropertyValue('--color-primary');
// 应该返回类似 "#6366f1" 的值
```

#### 步骤2: 在React组件中使用

```css
/* react-app/components/Button/Button.module.css */
.button {
    background: var(--color-primary);  /* ✓ 直接使用 */
    padding: var(--spacing-2) var(--spacing-4);
    border-radius: var(--radius-md);
}
```

#### 步骤3: 验证生效

```typescript
// React组件
import styles from './Button.module.css';

export function Button({ children }: PropsWithChildren) {
    return <button className={styles.button}>{children}</button>;
}
```

**完成**！如果CSS加载正确，按钮现在会使用主应用的颜色。

---

## 分步实现

### 第一步: 整合CSS变量定义

#### 1.1 统一变量位置

**当前问题**: CSS变量定义在多处：
- `main-layout.html` 内的 `<style>` 标签
- `/css/design-tokens.css`
- `src/main/frontend/styles/tokens/` 各文件

**目标**: 统一定义，避免重复。

#### 1.2 创建统一的变量定义文件

```bash
# 位置：src/main/resources/static/css/
# 文件：design-tokens-unified.css
```

**文件内容**:

```css
/**
 * Design Tokens - 统一的CSS变量定义
 * 版本: v2.0 (Unified)
 * 说明: 所有应用共用的设计令牌系统
 */

:root {
    /* ============================================
       第1层: Shell系统变量（主应用级）
       用于顶级布局、主题管理等
       ============================================ */

    /* 页面布局 */
    --shell-header-height: 64px;
    --shell-sidebar-width: 240px;
    --shell-sidebar-width-collapsed: 72px;

    /* 背景与表面色 */
    --shell-bg: #ffffff;
    --shell-surface: rgba(255, 255, 255, 0.92);
    --shell-glass: rgba(255, 255, 255, 0.78);
    --shell-overlay: rgba(15, 23, 42, 0.8);

    /* 文字色 */
    --shell-text-primary: #0f172a;
    --shell-text-secondary: rgba(15, 23, 42, 0.8);
    --shell-text-tertiary: rgba(15, 23, 42, 0.6);

    /* 边框色 */
    --shell-border: rgba(255, 255, 255, 0.6);
    --shell-border-dark: rgba(15, 23, 42, 0.18);

    /* 强调色 */
    --shell-accent: linear-gradient(135deg, #2f9bff, #3bc6b8);
    --shell-accent-dev: linear-gradient(135deg, #6c63ff, #3d8bff);
    --shell-scrollbar: rgba(15, 23, 42, 0.18);

    /* ============================================
       第2层: 设计令牌系统（全应用级）
       可被所有应用继承和使用
       ============================================ */

    /* 颜色系统 */
    --color-primary: #6366f1;
    --color-primary-light: #818cf8;
    --color-primary-dark: #4f46e5;
    --color-primary-ultra-light: rgba(99, 102, 241, 0.1);

    --color-success: #10b981;
    --color-success-light: #34d399;
    --color-success-dark: #059669;
    --color-success-bg: rgba(16, 185, 129, 0.1);

    --color-warning: #f59e0b;
    --color-warning-light: #fbbf24;
    --color-warning-dark: #d97706;
    --color-warning-bg: rgba(245, 158, 11, 0.1);

    --color-error: #ef4444;
    --color-error-light: #f87171;
    --color-error-dark: #dc2626;
    --color-error-bg: rgba(239, 68, 68, 0.1);

    --color-info: #3b82f6;
    --color-info-light: #60a5fa;
    --color-info-dark: #2563eb;
    --color-info-bg: rgba(59, 130, 246, 0.1);

    /* 背景色 */
    --bg-primary: #ffffff;
    --bg-secondary: #f8fafc;
    --bg-tertiary: #f1f5f9;
    --bg-quaternary: #e2e8f0;
    --bg-overlay: rgba(15, 23, 42, 0.8);

    /* 文字色 */
    --text-primary: #1e293b;
    --text-secondary: #475569;
    --text-tertiary: #64748b;
    --text-quaternary: #94a3b8;
    --text-white: #ffffff;

    /* 边框色 */
    --border-primary: #e2e8f0;
    --border-secondary: #cbd5e1;
    --border-light: #f1f5f9;
    --border-focus: var(--color-primary);

    /* 间距令牌 */
    --spacing-1: 0.25rem;  /* 4px */
    --spacing-2: 0.5rem;   /* 8px */
    --spacing-3: 0.75rem;  /* 12px */
    --spacing-4: 1rem;     /* 16px */
    --spacing-6: 1.5rem;   /* 24px */
    --spacing-8: 2rem;     /* 32px */
    --spacing-10: 2.5rem;  /* 40px */
    --spacing-12: 3rem;    /* 48px */

    /* 圆角令牌 */
    --radius-sm: 4px;
    --radius-md: 8px;
    --radius-lg: 12px;
    --radius-xl: 16px;
    --radius-full: 9999px;

    /* 字体令牌 */
    --font-family-sans: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    --font-family-mono: "JetBrains Mono", "Fira Code", Consolas, "Liberation Mono", monospace;

    --font-size-xs: 0.75rem;    /* 12px */
    --font-size-sm: 0.875rem;   /* 14px */
    --font-size-base: 1rem;     /* 16px */
    --font-size-lg: 1.125rem;   /* 18px */
    --font-size-xl: 1.25rem;    /* 20px */
    --font-size-2xl: 1.5rem;    /* 24px */
    --font-size-3xl: 1.875rem;  /* 30px */

    --font-weight-light: 300;
    --font-weight-normal: 400;
    --font-weight-medium: 500;
    --font-weight-semibold: 600;
    --font-weight-bold: 700;

    --line-height-tight: 1.25;
    --line-height-snug: 1.375;
    --line-height-normal: 1.5;
    --line-height-relaxed: 1.625;
    --line-height-loose: 2;

    /* 阴影令牌 */
    --shadow-xs: 0 1px 3px rgba(0, 0, 0, 0.1);
    --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.06);
    --shadow-md: 0 4px 6px rgba(0, 0, 0, 0.1), 0 2px 4px rgba(0, 0, 0, 0.06);
    --shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.1), 0 4px 6px rgba(0, 0, 0, 0.05);
    --shadow-xl: 0 20px 25px rgba(0, 0, 0, 0.1), 0 10px 10px rgba(0, 0, 0, 0.04);

    /* 过渡令牌 */
    --duration-fast: 150ms;
    --duration-normal: 300ms;
    --duration-slow: 500ms;

    --easing-ease: cubic-bezier(0.4, 0, 0.2, 1);
    --easing-ease-out: cubic-bezier(0, 0, 0.2, 1);
    --easing-ease-in: cubic-bezier(0.4, 0, 1, 1);
}

/* 深色主题 */
[data-theme="dark"] {
    /* Shell系统变量 */
    --shell-bg: #111827;
    --shell-surface: rgba(30, 41, 59, 0.92);
    --shell-glass: rgba(30, 41, 59, 0.82);
    --shell-border: rgba(148, 163, 184, 0.32);
    --shell-border-dark: rgba(15, 23, 42, 0.36);
    --shell-text-primary: #f9fafb;
    --shell-text-secondary: rgba(226, 232, 240, 0.72);
    --shell-text-tertiary: rgba(226, 232, 240, 0.56);
    --shell-scrollbar: rgba(148, 163, 184, 0.35);

    /* 设计令牌 */
    --bg-primary: #1f2937;
    --bg-secondary: #111827;
    --bg-tertiary: #0f172a;
    --text-primary: #f9fafb;
    --text-secondary: rgba(226, 232, 240, 0.8);
    --text-tertiary: rgba(226, 232, 240, 0.6);
    --border-primary: rgba(148, 163, 184, 0.32);
}

/* 高对比度模式（可选） */
@media (prefers-contrast: more) {
    :root {
        --color-primary: #4338ca;
        --text-primary: #000000;
        --bg-primary: #ffffff;
    }
}

/* 减少动画模式 */
@media (prefers-reduced-motion: reduce) {
    :root {
        --duration-fast: 0ms;
        --duration-normal: 0ms;
        --duration-slow: 0ms;
    }
}
```

#### 1.3 更新HTML引用

```html
<!-- main-layout.html -->
<head>
    <!-- 替换旧的分散引用 -->
    <link rel="stylesheet" href="/css/design-tokens-unified.css">

    <!-- 其他样式 -->
    <link rel="stylesheet" href="/css/base.css">
    <link rel="stylesheet" href="/css/main.css">
</head>
```

### 第二步: 同步React应用主题管理

#### 2.1 更新ThemeManager

```typescript
// src/main/frontend/modules/theme.ts
/**
 * 统一的主题管理器
 * 确保React应用与主应用主题同步
 */

import { localStorageManager, eventBus } from "@/utils";

export type Theme = "light" | "dark";

class UnifiedThemeManager {
    private readonly STORAGE_KEY = "app-theme";
    private readonly THEME_ATTR = "data-theme";  // 使用属性而非类

    constructor() {
        this.init();
    }

    private init(): void {
        // 从主应用读取当前主题
        const shellTheme = this.getShellTheme();
        if (shellTheme) {
            // 确保一致性
            return;
        }

        // 如果主应用未设置，使用保存的主题
        const savedTheme = this.getSavedTheme();
        if (savedTheme) {
            this.applyTheme(savedTheme);
        }

        // 监听主应用主题变化
        this.watchShellTheme();
    }

    /**
     * 从localStorage读取保存的主题
     */
    private getSavedTheme(): Theme | null {
        const saved = localStorageManager.get<string>(
            this.STORAGE_KEY,
            null
        );
        return saved === "dark" || saved === "light" ? (saved as Theme) : null;
    }

    /**
     * 从主应用读取当前主题
     */
    private getShellTheme(): Theme | null {
        const theme = document.documentElement.getAttribute(this.THEME_ATTR);
        return theme === "dark" || theme === "light" ? (theme as Theme) : null;
    }

    /**
     * 监听主应用主题变化
     */
    private watchShellTheme(): void {
        const observer = new MutationObserver(() => {
            const shellTheme = this.getShellTheme();
            if (shellTheme) {
                // 触发React应用的主题变化事件
                eventBus.emit("theme:changed", { theme: shellTheme });
                window.dispatchEvent(
                    new CustomEvent("theme-changed", { detail: { theme: shellTheme } })
                );
            }
        });

        observer.observe(document.documentElement, {
            attributes: true,
            attributeFilter: [this.THEME_ATTR]
        });
    }

    /**
     * 应用主题
     */
    applyTheme(theme: Theme): void {
        document.documentElement.setAttribute(this.THEME_ATTR, theme);
        localStorageManager.set(this.STORAGE_KEY, theme);
        eventBus.emit("theme:changed", { theme });

        window.dispatchEvent(
            new CustomEvent("theme-changed", { detail: { theme } })
        );
    }

    /**
     * 切换主题
     */
    toggle(): void {
        const current = this.getShellTheme() || "light";
        const next = current === "light" ? "dark" : "light";
        this.applyTheme(next);
    }

    /**
     * 获取当前主题
     */
    getCurrentTheme(): Theme {
        return this.getShellTheme() || "light";
    }

    /**
     * 获取CSS变量值
     */
    getVariable(name: string): string {
        return getComputedStyle(document.documentElement)
            .getPropertyValue(name)
            .trim();
    }
}

// 全局实例
let instance: UnifiedThemeManager | null = null;

export function getThemeManager(): UnifiedThemeManager {
    if (!instance) {
        instance = new UnifiedThemeManager();
    }
    return instance;
}

export const themeManager = getThemeManager();

// 全局绑定（保持向后兼容）
declare global {
    interface Window {
        toggleTheme?: () => void;
        themeManager?: UnifiedThemeManager;
    }
}

if (typeof window !== "undefined") {
    window.toggleTheme = () => themeManager.toggle();
    window.themeManager = themeManager;
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
        console.info("[ThemeManager] 已完成初始化");
    });
} else {
    console.info("[ThemeManager] 已完成初始化");
}
```

#### 2.2 创建React ThemeContext

```typescript
// react-app/context/ThemeContext.tsx
import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { themeManager, Theme } from '../../../modules/theme';

interface ThemeContextValue {
    theme: Theme;
    toggleTheme: () => void;
    getVariable: (name: string) => string;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }): JSX.Element {
    const [theme, setTheme] = useState<Theme>(themeManager.getCurrentTheme());

    useEffect(() => {
        // 监听主题变化事件
        const handleThemeChange = (event: CustomEvent<{ theme: Theme }>) => {
            setTheme(event.detail.theme);
        };

        window.addEventListener('theme-changed', handleThemeChange as EventListener);

        return () => {
            window.removeEventListener('theme-changed', handleThemeChange as EventListener);
        };
    }, []);

    return (
        <ThemeContext.Provider
            value={{
                theme,
                toggleTheme: () => themeManager.toggle(),
                getVariable: (name: string) => themeManager.getVariable(name)
            }}
        >
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

#### 2.3 更新App.tsx

```typescript
// react-app/App.tsx
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { I18nextProvider } from "react-i18next";

import "./index.css";
import { ThemeProvider } from "./context/ThemeContext";  // 添加
import { ShellLayout } from "./layout/ShellLayout";
import { OverviewPage } from "./pages/OverviewPage";
// ... 其他导入

export function AppRoutes(): JSX.Element {
    return (
        <ShellLayout>
            <Routes>
                {/* 路由配置 */}
            </Routes>
        </ShellLayout>
    );
}

export function App({ basename }: { basename?: string }): JSX.Element {
    const base = basename ?? resolveBasename();

    useTokenRefresh();

    return (
        <QueryClientProvider client={queryClient}>
            <I18nextProvider i18n={i18n}>
                <ThemeProvider>  {/* 添加 */}
                    <BrowserRouter basename={base}>
                        <AppRoutes />
                    </BrowserRouter>
                </ThemeProvider>
            </I18nextProvider>
            <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
    );
}

export default App;
```

### 第三步: 更新React组件CSS

#### 3.1 更新index.css

```css
/* react-app/index.css */
.deploy-platform-app {
    /* 自动继承主应用CSS变量 */
    background: var(--bg-primary);
    color: var(--text-primary);
    font-family: var(--font-family-sans);
    font-size: var(--font-size-base);
    line-height: var(--line-height-normal);

    /* 主题切换过渡效果 */
    transition:
        background-color var(--duration-normal) var(--easing-ease),
        color var(--duration-normal) var(--easing-ease);
}

/* 确保所有子元素继承 */
.deploy-platform-app * {
    box-sizing: border-box;
}
```

#### 3.2 创建全局样式辅助类

```css
/* react-app/styles/utilities.css (可选) */

/* 背景颜色快捷方式 */
.bg-primary { background: var(--bg-primary); }
.bg-secondary { background: var(--bg-secondary); }
.bg-success { background: var(--color-success-bg); }
.bg-warning { background: var(--color-warning-bg); }
.bg-error { background: var(--color-error-bg); }

/* 文字颜色快捷方式 */
.text-primary { color: var(--text-primary); }
.text-secondary { color: var(--text-secondary); }
.text-tertiary { color: var(--text-tertiary); }

/* 间距快捷方式 */
.p-2 { padding: var(--spacing-2); }
.p-4 { padding: var(--spacing-4); }
.m-2 { margin: var(--spacing-2); }
.m-4 { margin: var(--spacing-4); }

/* 圆角快捷方式 */
.rounded-md { border-radius: var(--radius-md); }
.rounded-lg { border-radius: var(--radius-lg); }

/* 阴影快捷方式 */
.shadow-md { box-shadow: var(--shadow-md); }
.shadow-lg { box-shadow: var(--shadow-lg); }
```

#### 3.3 更新组件CSS模块

示例: Button组件

```css
/* react-app/components/Button/Button.module.css */
.button {
    /* 使用CSS变量替代硬编码的颜色 */
    background: var(--color-primary);
    color: white;
    border: none;

    /* 使用间距令牌 */
    padding: var(--spacing-2) var(--spacing-4);
    border-radius: var(--radius-md);

    /* 使用字体令牌 */
    font-family: var(--font-family-sans);
    font-weight: var(--font-weight-semibold);
    font-size: var(--font-size-sm);

    /* 使用过渡令牌 */
    transition:
        background var(--duration-fast) var(--easing-ease),
        transform var(--duration-fast) var(--easing-ease);

    cursor: pointer;
}

.button:hover {
    background: var(--color-primary-light);
    transform: translateY(-1px);
}

.button:active {
    transform: translateY(0);
}

.button--secondary {
    background: var(--bg-secondary);
    color: var(--text-primary);
    border: 1px solid var(--border-primary);
}

.button--secondary:hover {
    background: var(--bg-tertiary);
}

.button--success {
    background: var(--color-success);
}

.button--success:hover {
    background: var(--color-success-light);
}

.button--warning {
    background: var(--color-warning);
}

.button--error {
    background: var(--color-error);
}

.button--error:hover {
    background: var(--color-error-light);
}

/* 禁用状态 */
.button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}
```

---

## 代码示例

### 示例1: 完整的按钮组件

```typescript
// react-app/components/Button/Button.tsx
import { ButtonHTMLAttributes, ReactNode } from 'react';
import styles from './Button.module.css';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: 'primary' | 'secondary' | 'success' | 'warning' | 'error';
    size?: 'sm' | 'md' | 'lg';
    loading?: boolean;
}

export function Button({
    variant = 'primary',
    size = 'md',
    loading = false,
    disabled = false,
    children,
    className,
    ...props
}: ButtonProps & { children: ReactNode }): JSX.Element {
    const variantClass = styles[`button--${variant}`];
    const sizeClass = styles[`button--${size}`];

    return (
        <button
            className={[
                styles.button,
                variantClass,
                sizeClass,
                className
            ].filter(Boolean).join(' ')}
            disabled={disabled || loading}
            {...props}
        >
            {loading ? '加载中...' : children}
        </button>
    );
}
```

### 示例2: 使用ThemeContext的组件

```typescript
// react-app/components/ThemeSwitcher/ThemeSwitcher.tsx
import { useTheme } from '../../context/ThemeContext';
import styles from './ThemeSwitcher.module.css';

export function ThemeSwitcher(): JSX.Element {
    const { theme, toggleTheme } = useTheme();

    return (
        <button
            className={styles.switcher}
            onClick={toggleTheme}
            aria-label={`切换至${theme === 'light' ? '深' : '浅'}色主题`}
            title={`当前主题：${theme === 'light' ? '浅' : '深'}色`}
        >
            {theme === 'light' ? '🌙' : '☀️'}
        </button>
    );
}
```

### 示例3: 获取CSS变量值的Hook

```typescript
// react-app/hooks/useThemeVariable.ts
import { useState, useEffect } from 'react';

/**
 * Hook: 获取并订阅CSS变量值变化
 * 用于在JavaScript中使用主应用定义的CSS变量
 */
export function useThemeVariable(varName: string): string {
    const [value, setValue] = useState<string>(() => {
        return getComputedStyle(document.documentElement)
            .getPropertyValue(varName)
            .trim();
    });

    useEffect(() => {
        // 当主题变化时更新值
        const handleThemeChange = () => {
            const newValue = getComputedStyle(document.documentElement)
                .getPropertyValue(varName)
                .trim();
            setValue(newValue);
        };

        window.addEventListener('theme-changed', handleThemeChange as EventListener);

        // 也监听DOM属性变化
        const observer = new MutationObserver(handleThemeChange);
        observer.observe(document.documentElement, {
            attributes: true,
            attributeFilter: ['data-theme']
        });

        return () => {
            window.removeEventListener('theme-changed', handleThemeChange as EventListener);
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
                        labels: { color: textColor }
                    }
                }
            }}
            data={{
                datasets: [{
                    borderColor: primaryColor,
                    backgroundColor: `${primaryColor}20`
                }]
            }}
        />
    );
}
```

---

## 验证清单

### 部署前检查清单

- [ ] **CSS变量定义**
  - [ ] 所有变量在`:root`定义
  - [ ] `data-theme="dark"`包含所有必要变量
  - [ ] 无变量名拼写错误

- [ ] **HTML集成**
  - [ ] `main-layout.html`引入了CSS文件
  - [ ] HTML元素有`data-theme`属性初始值
  - [ ] React应用容器在主应用样式加载后

- [ ] **React应用**
  - [ ] 更新了`ThemeManager`
  - [ ] 创建了`ThemeContext`
  - [ ] `App.tsx`包装了`ThemeProvider`
  - [ ] 组件CSS使用CSS变量而非硬编码

- [ ] **功能测试**
  - [ ] CSS变量在浏览器控制台可访问
  - [ ] 主题切换时CSS变量值正确更新
  - [ ] React组件颜色随主题变化
  - [ ] localStorage保存主题选择

- [ ] **浏览器兼容性**
  - [ ] Chrome/Edge 49+
  - [ ] Firefox 31+
  - [ ] Safari 9.1+
  - [ ] 移动浏览器

### 验证脚本

```typescript
// 在浏览器F12控制台运行验证

// 1. 检查基础变量
console.log('主色:', getComputedStyle(document.documentElement).getPropertyValue('--color-primary'));
console.log('背景色:', getComputedStyle(document.documentElement).getPropertyValue('--bg-primary'));

// 2. 检查主题切换
console.log('当前主题:', document.documentElement.getAttribute('data-theme'));
document.documentElement.setAttribute('data-theme', 'dark');
console.log('深色背景:', getComputedStyle(document.documentElement).getPropertyValue('--bg-primary'));

// 3. 检查组件样式
const button = document.querySelector('button');
console.log('按钮计算样式:', getComputedStyle(button));

// 4. 检查事件
window.addEventListener('theme-changed', (e) => {
    console.log('主题变化事件:', e);
});
```

---

## 常见问题

### Q1: CSS变量在组件中不显示？

**A**: 检查CSS文件加载顺序。主应用CSS文件必须在React应用CSS之前加载。

```html
<!-- ✓ 正确的顺序 -->
<link rel="stylesheet" href="/css/design-tokens-unified.css">  <!-- 主应用变量 -->
<link rel="stylesheet" href="/dist/assets/app.css">           <!-- React应用 -->
```

### Q2: 主题切换时没有过渡效果？

**A**: 添加transition到涉及颜色的元素：

```css
.element {
    background: var(--bg-primary);
    color: var(--text-primary);
    transition: background var(--duration-normal), color var(--duration-normal);
}
```

### Q3: CSS Modules中如何使用CSS变量？

**A**: 直接在CSS中使用，不需要特殊配置：

```css
/* Button.module.css */
.button {
    background: var(--color-primary);  /* ✓ 直接使用 */
}
```

### Q4: 如何在JavaScript中获取CSS变量？

**A**: 使用提供的`useThemeVariable` Hook或直接使用getComputedStyle：

```typescript
// 方案1: 使用Hook（推荐）
const primaryColor = useThemeVariable('--color-primary');

// 方案2: 直接获取
const color = getComputedStyle(document.documentElement)
    .getPropertyValue('--color-primary')
    .trim();
```

### Q5: Web Component中无法访问CSS变量？

**A**: CSS变量会自动穿透Shadow DOM，检查变量是否在`:root`定义：

```typescript
class MyComponent extends HTMLElement {
    connectedCallback() {
        const shadow = this.attachShadow({ mode: 'open' });
        shadow.innerHTML = `
            <style>
                :host { color: var(--text-primary); }  /* ✓ 自动继承 */
            </style>
        `;
    }
}
```

---

## 实施时间表

```
第一周:
  - 整合CSS变量定义（第一步）
  - 更新HTML引用
  - 本地验证

第二周:
  - 同步React主题管理（第二步）
  - 创建ThemeContext
  - 集成测试

第三周:
  - 更新组件CSS（第三步）
  - 逐个更新组件
  - 全面测试

第四周:
  - 用户验收测试
  - 性能优化
  - 文档完善
```

---

**文档版本**: v1.0
**最后更新**: 2025-11-02

