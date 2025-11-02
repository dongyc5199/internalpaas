# CSS变量集成实施指南

## 快速开始

本指南提供了立即可用的代码示例和实施步骤，用于优化React应用的CSS变量继承。

---

## Part 1: 核心Hook实现

### useThemeSync Hook

```typescript
// src/main/frontend/react-app/hooks/useThemeSync.ts

import { useEffect, useState, useCallback } from 'react';

export type Theme = 'light' | 'dark';

interface ThemeSyncOptions {
  // 定期检查属性变化的间隔（毫秒）
  pollInterval?: number;
  // 初始化时是否强制应用保存的主题
  applyStoredTheme?: boolean;
  // 存储主题的localStorage键名
  storageKey?: string;
}

/**
 * Hook: 与主应用主题系统同步
 *
 * 提供3层同步机制：
 * 1. MutationObserver - 监听data-theme属性变化
 * 2. CustomEvent - 监听主应用发出的事件
 * 3. EventBus - 监听全局事件总线
 *
 * @param onThemeChange - 主题变化回调
 * @param options - 配置选项
 * @returns 当前主题
 *
 * @example
 * const theme = useThemeSync((newTheme) => {
 *   console.log('Theme changed to:', newTheme);
 * });
 */
export function useThemeSync(
  onThemeChange?: (theme: Theme) => void,
  options: ThemeSyncOptions = {}
): Theme {
  const {
    pollInterval = 0,
    applyStoredTheme = false,
    storageKey = 'app-theme'
  } = options;

  const [theme, setTheme] = useState<Theme>(() => {
    // 优先级：data-theme属性 > localStorage > 默认light
    const dataTheme = document.documentElement.getAttribute('data-theme') as Theme;
    if (dataTheme) return dataTheme;

    if (applyStoredTheme) {
      const stored = localStorage.getItem(storageKey) as Theme;
      if (stored) return stored;
    }

    return 'light';
  });

  useEffect(() => {
    const handleThemeChange = useCallback((newTheme: Theme) => {
      setTheme(newTheme);
      if (applyStoredTheme) {
        localStorage.setItem(storageKey, newTheme);
      }
      onThemeChange?.(newTheme);
    }, [onThemeChange, applyStoredTheme, storageKey]);

    // 方案A: MutationObserver - 最可靠
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.attributeName === 'data-theme') {
          const newTheme = (document.documentElement.getAttribute('data-theme') as Theme) || 'light';
          handleThemeChange(newTheme);
        }
      });
    });

    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['data-theme'],
      attributeOldValue: true
    });

    // 方案B: CustomEvent - 主应用发出的事件
    const handleCustomEvent = (event: Event) => {
      const customEvent = event as CustomEvent<{ theme: Theme }>;
      const newTheme = customEvent.detail?.theme;
      if (newTheme && newTheme !== theme) {
        handleThemeChange(newTheme);
      }
    };

    document.addEventListener('themeChanged', handleCustomEvent);

    // 方案C: EventBus - 如果使用了事件总线
    let unsubscribeEventBus: (() => void) | null = null;
    if ((window as any).eventBus?.on) {
      const handleEventBusTheme = ({ theme: newTheme }: { theme: Theme }) => {
        if (newTheme !== theme) {
          handleThemeChange(newTheme);
        }
      };

      (window as any).eventBus.on('theme:changed', handleEventBusTheme);

      unsubscribeEventBus = () => {
        (window as any).eventBus?.off('theme:changed', handleEventBusTheme);
      };
    }

    // 方案D: 定期检查 - 保险方案
    let pollIntervalId: NodeJS.Timeout | null = null;
    if (pollInterval > 0) {
      pollIntervalId = setInterval(() => {
        const currentTheme = (document.documentElement.getAttribute('data-theme') as Theme) || 'light';
        if (currentTheme !== theme) {
          handleThemeChange(currentTheme);
        }
      }, pollInterval);
    }

    // 清理函数
    return () => {
      observer.disconnect();
      document.removeEventListener('themeChanged', handleCustomEvent);
      if (unsubscribeEventBus) {
        unsubscribeEventBus();
      }
      if (pollIntervalId) {
        clearInterval(pollIntervalId);
      }
    };
  }, [theme, onThemeChange, applyStoredTheme, storageKey, pollInterval]);

  return theme;
}
```

### useThemeVariable Hook

```typescript
// src/main/frontend/react-app/hooks/useThemeVariable.ts

import { useEffect, useState, useCallback } from 'react';

/**
 * Hook: 读取CSS变量的当前值
 *
 * 场景：需要在JavaScript中访问CSS变量值，例如：
 * - 传递给图表库的颜色
 * - 条件判断
 * - DOM操作
 *
 * @param variableName - CSS变量名（含--前缀）
 * @returns 变量的计算值，初始为空字符串
 *
 * @example
 * const primaryColor = useThemeVariable('--color-primary');
 * return <svg fill={primaryColor} />;
 */
export function useThemeVariable(variableName: string): string {
  const [value, setValue] = useState<string>('');
  const [isReady, setIsReady] = useState(false);

  const readVariable = useCallback(() => {
    try {
      const computed = getComputedStyle(document.documentElement);
      const variableValue = computed.getPropertyValue(variableName).trim();

      if (!variableValue && import.meta.env.DEV) {
        console.warn(
          `[useThemeVariable] CSS variable not found: ${variableName}. ` +
          `Available variables: ${Array.from(computed).filter(p => p.startsWith('--')).join(', ')}`
        );
      }

      setValue(variableValue);
      setIsReady(true);
    } catch (error) {
      console.error(`[useThemeVariable] Error reading ${variableName}:`, error);
      setIsReady(true);
    }
  }, [variableName]);

  useEffect(() => {
    // 等待DOM完全加载
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', readVariable);
      return () => {
        document.removeEventListener('DOMContentLoaded', readVariable);
      };
    }

    // 立即读取
    readVariable();

    // 监听主题变化事件重新读取
    const handleThemeChange = () => {
      readVariable();
    };

    document.addEventListener('themeChanged', handleThemeChange);

    // 使用MutationObserver也能捕捉变化
    const observer = new MutationObserver(() => {
      readVariable();
    });

    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['data-theme']
    });

    return () => {
      document.removeEventListener('themeChanged', handleThemeChange);
      observer.disconnect();
    };
  }, [readVariable, variableName]);

  return value;
}
```

### useDerivedThemeValue Hook

```typescript
// src/main/frontend/react-app/hooks/useDerivedThemeValue.ts

import { useEffect, useState } from 'react';

/**
 * 主题变量集合
 */
export interface ThemeVariables {
  [key: string]: string;
}

/**
 * Hook: 基于CSS变量计算派生值
 *
 * 场景：需要基于多个CSS变量计算新值，例如：
 * - 根据背景色自动判断文本颜色
 * - 生成CSS变量的RGB值
 * - 计算颜色透明度
 *
 * @param deriveFn - 派生函数，接收所有变量并返回计算结果
 * @param variableNames - 需要监听的变量名数组（优化性能）
 * @returns 派生值
 *
 * @example
 * const textColor = useDerivedThemeValue((vars) => {
 *   // 如果背景是浅色，返回深色文本
 *   return vars['--bg-primary'].includes('ff') ? '#1e293b' : '#f1f5f9';
 * }, ['--bg-primary']);
 */
export function useDerivedThemeValue(
  deriveFn: (variables: ThemeVariables) => string,
  variableNames?: string[]
): string {
  const [value, setValue] = useState<string>('');

  useEffect(() => {
    const calculateValue = () => {
      try {
        const computed = getComputedStyle(document.documentElement);

        // 如果指定了变量名，只读取这些；否则读取所有
        let variables: ThemeVariables = {};

        if (variableNames && variableNames.length > 0) {
          variableNames.forEach(name => {
            variables[name] = computed.getPropertyValue(name).trim();
          });
        } else {
          // 读取所有CSS变量
          for (let i = 0; i < computed.length; i++) {
            const prop = computed[i];
            if (prop.startsWith('--')) {
              variables[prop] = computed.getPropertyValue(prop).trim();
            }
          }
        }

        const derived = deriveFn(variables);
        setValue(derived);
      } catch (error) {
        console.error('[useDerivedThemeValue] Error:', error);
      }
    };

    // 初始计算
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', calculateValue);
      return () => {
        document.removeEventListener('DOMContentLoaded', calculateValue);
      };
    }

    calculateValue();

    // 监听主题变化
    const handleChange = () => {
      calculateValue();
    };

    document.addEventListener('themeChanged', handleChange);

    const observer = new MutationObserver(() => {
      calculateValue();
    });

    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['data-theme']
    });

    return () => {
      document.removeEventListener('themeChanged', handleChange);
      observer.disconnect();
    };
  }, [deriveFn, variableNames?.join(',')]); // 依赖项需要稳定

  return value;
}
```

---

## Part 2: CSS样式文件

### 全局样式（index.css）

```css
/* src/main/frontend/react-app/index.css */

/**
 * React应用全局样式
 * 直接使用主应用的CSS变量，无需重复定义
 */

/* ================================================================
 * 基础样式
 * ================================================================ */

:root {
  /* React应用扩展变量 - 避免与主应用冲突 */
  --react-sidebar-width: 320px;
  --react-content-max-width: 1200px;
  --react-card-elevation: 0 2px 8px rgba(0, 0, 0, 0.08);
  --react-transition-duration: var(--transition-normal, 300ms);
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

*::before,
*::after {
  box-sizing: border-box;
}

html {
  /* 确保变量在任何地方都可访问 */
  scroll-behavior: smooth;
}

body {
  /* 继承主应用变量 */
  background-color: var(--bg-primary, #ffffff);
  color: var(--text-primary, #1e293b);
  font-family: var(--font-family-sans, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif);
  font-size: var(--font-size-base, 1rem);
  line-height: 1.5;

  /* 平滑的主题切换 */
  transition: background-color var(--react-transition-duration),
              color var(--react-transition-duration);
}

/* ================================================================
 * 应用容器
 * ================================================================ */

.deploy-platform-app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;

  /* 继承变量 */
  background: var(--bg-primary);
  color: var(--text-primary);
}

.dp-shell {
  display: grid;
  grid-template-columns: var(--react-sidebar-width) 1fr;
  gap: var(--spacing-6, 1.5rem);
  height: 100%;
  padding: var(--spacing-6, 1.5rem);
}

@media (max-width: 1024px) {
  .dp-shell {
    grid-template-columns: 1fr;
  }
}

/* ================================================================
 * 侧边栏
 * ================================================================ */

.dp-shell__sidebar {
  /* 使用主应用的深色背景 */
  background: linear-gradient(
    180deg,
    rgba(15, 23, 42, 0.92),
    rgba(30, 64, 175, 0.82)
  );
  color: var(--text-white, #f1f5f9);
  border-radius: var(--radius-xl, 0.75rem);
  padding: var(--spacing-6, 1.5rem);

  /* 主题感知背景 */
  transition: background var(--react-transition-duration);
}

[data-theme="dark"] .dp-shell__sidebar {
  /* 暗主题自动处理 - CSS变量会自动更新 */
}

/* ================================================================
 * 按钮
 * ================================================================ */

.dp-btn {
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  border: none;
  cursor: pointer;

  transition: all var(--react-transition-duration);
  outline-offset: 2px;
}

.dp-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.dp-btn:active:not(:disabled) {
  transform: translateY(0);
}

.dp-btn:disabled {
  opacity: var(--state-disabled-opacity, 0.5);
  cursor: not-allowed;
}

.dp-btn--primary {
  background: var(--color-primary);
  color: var(--text-white);
}

.dp-btn--primary:hover:not(:disabled) {
  background: var(--color-primary-dark);
}

.dp-btn--secondary {
  background: var(--bg-secondary);
  color: var(--text-primary);
  border: 1px solid var(--border-primary);
}

.dp-btn--secondary:hover:not(:disabled) {
  background: var(--bg-tertiary);
}

/* ================================================================
 * 卡片
 * ================================================================ */

.dp-card {
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-sm);

  transition: all var(--react-transition-duration);
}

.dp-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--border-secondary);
}

/* ================================================================
 * 响应式设计
 * ================================================================ */

@media (max-width: 768px) {
  .dp-shell {
    gap: var(--spacing-4);
    padding: var(--spacing-4);
  }

  .dp-card {
    padding: var(--spacing-4);
  }
}

/* ================================================================
 * 打印样式
 * ================================================================ */

@media print {
  .dp-shell__sidebar,
  .dp-btn--secondary {
    display: none;
  }

  .deploy-platform-app {
    background: white;
    color: black;
  }
}
```

### 共享组件样式（shared.css）

```css
/* src/main/frontend/react-app/styles/shared.css */

/**
 * 所有组件共享的样式
 * 这些类使用CSS变量，可以被所有组件复用
 */

/* ================================================================
 * 颜色工具类
 * ================================================================ */

.text-primary {
  color: var(--text-primary) !important;
}

.text-secondary {
  color: var(--text-secondary) !important;
}

.text-tertiary {
  color: var(--text-tertiary) !important;
}

.text-muted {
  color: var(--text-muted) !important;
}

.bg-primary {
  background-color: var(--bg-primary) !important;
}

.bg-secondary {
  background-color: var(--bg-secondary) !important;
}

.border-primary {
  border-color: var(--border-primary) !important;
}

/* ================================================================
 * 间距工具类
 * ================================================================ */

.p-1 { padding: var(--spacing-1); }
.p-2 { padding: var(--spacing-2); }
.p-3 { padding: var(--spacing-3); }
.p-4 { padding: var(--spacing-4); }
.p-6 { padding: var(--spacing-6); }

.m-1 { margin: var(--spacing-1); }
.m-2 { margin: var(--spacing-2); }
.m-3 { margin: var(--spacing-3); }
.m-4 { padding: var(--spacing-4); }

.gap-2 { gap: var(--spacing-2); }
.gap-4 { gap: var(--spacing-4); }

/* ================================================================
 * 阴影工具类
 * ================================================================ */

.shadow-sm {
  box-shadow: var(--shadow-sm);
}

.shadow-md {
  box-shadow: var(--shadow-md);
}

.shadow-lg {
  box-shadow: var(--shadow-lg);
}

/* ================================================================
 * 圆角工具类
 * ================================================================ */

.rounded-sm {
  border-radius: var(--radius-sm);
}

.rounded-md {
  border-radius: var(--radius-md);
}

.rounded-lg {
  border-radius: var(--radius-lg);
}

.rounded-full {
  border-radius: var(--radius-full);
}

/* ================================================================
 * 排版工具类
 * ================================================================ */

.font-sans {
  font-family: var(--font-family-sans);
}

.font-mono {
  font-family: var(--font-family-mono);
}

.font-normal {
  font-weight: var(--font-weight-normal);
}

.font-semibold {
  font-weight: var(--font-weight-semibold);
}

.font-bold {
  font-weight: var(--font-weight-bold);
}

/* ================================================================
 * 布局工具类
 * ================================================================ */

.flex {
  display: flex;
}

.flex-col {
  flex-direction: column;
}

.items-center {
  align-items: center;
}

.justify-between {
  justify-content: space-between;
}

.w-full {
  width: 100%;
}

.h-full {
  height: 100%;
}

/* ================================================================
 * 过渡工具类
 * ================================================================ */

.transition-fast {
  transition: all var(--transition-fast);
}

.transition-normal {
  transition: all var(--transition-normal);
}

.transition-slow {
  transition: all var(--transition-slow);
}
```

### 组件样式示例（Button.module.css）

```css
/* src/main/frontend/react-app/components/Button/Button.module.css */

/**
 * 按钮组件CSS Module
 * 演示如何在CSS Modules中使用CSS变量
 */

.button {
  /* 基础样式 */
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2);

  /* 大小和间距 */
  padding: var(--spacing-3) var(--spacing-4);
  min-height: var(--btn-height-md, 40px);

  /* 边框和圆角 */
  border: none;
  border-radius: var(--radius-md);

  /* 文字 */
  font-family: var(--font-family-sans);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  line-height: 1;

  /* 前景 */
  background: var(--color-primary);
  color: var(--text-white);

  /* 交互 */
  cursor: pointer;
  user-select: none;

  /* 过渡 */
  transition: all var(--transition-fast);
  outline-offset: 2px;
}

/* 悬停状态 */
.button:hover:not(:disabled) {
  background: var(--color-primary-dark);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* 激活状态 */
.button:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
}

/* 禁用状态 */
.button:disabled {
  opacity: var(--state-disabled-opacity, 0.5);
  cursor: not-allowed;
  transform: none;
}

/* 焦点状态（可访问性） */
.button:focus-visible {
  outline: 2px solid var(--color-primary);
}

/* ================================================================
 * 变体：次按钮
 * ================================================================ */

.secondary {
  background: var(--bg-secondary);
  color: var(--text-primary);
  border: 1px solid var(--border-primary);
}

.secondary:hover:not(:disabled) {
  background: var(--bg-tertiary);
  border-color: var(--border-secondary);
}

/* ================================================================
 * 变体：危险按钮
 * ================================================================ */

.danger {
  background: var(--color-error);
  color: var(--text-white);
}

.danger:hover:not(:disabled) {
  background: var(--color-error-dark);
}

/* ================================================================
 * 变体：成功按钮
 * ================================================================ */

.success {
  background: var(--color-success);
  color: var(--text-white);
}

.success:hover:not(:disabled) {
  background: var(--color-success-dark);
}

/* ================================================================
 * 尺寸变体
 * ================================================================ */

.small {
  padding: var(--spacing-2) var(--spacing-3);
  font-size: var(--font-size-xs);
  min-height: var(--btn-height-sm, 32px);
}

.large {
  padding: var(--spacing-4) var(--spacing-6);
  font-size: var(--font-size-base);
  min-height: var(--btn-height-lg, 48px);
}

/* ================================================================
 * 加载状态
 * ================================================================ */

.loading {
  opacity: 0.7;
  pointer-events: none;
}

.loading::after {
  content: '';
  display: inline-block;
  width: 1em;
  height: 1em;
  margin-left: var(--spacing-2);
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ================================================================
 * 暗主题 - CSS变量自动处理
 * ================================================================ */

/* 无需重复定义 - [data-theme="dark"]中的CSS变量
 * 会自动更新以下样式 */

[data-theme="dark"] .button {
  /* background: var(--color-primary) 会自动使用暗主题的值 */
}
```

---

## Part 3: React组件实现

### 完整的按钮组件示例

```typescript
// src/main/frontend/react-app/components/Button/Button.tsx

import React, { ReactNode } from 'react';
import styles from './Button.module.css';

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'success';
export type ButtonSize = 'small' | 'medium' | 'large';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  // 按钮变体
  variant?: ButtonVariant;

  // 按钮大小
  size?: ButtonSize;

  // 是否加载中
  loading?: boolean;

  // 按钮内容
  children: ReactNode;

  // 图标
  icon?: ReactNode;

  // 图标位置
  iconPosition?: 'left' | 'right';
}

/**
 * 按钮组件
 *
 * 完全继承主应用的CSS变量，支持主题自动切换
 *
 * @example
 * <Button variant="primary" size="medium">
 *   Click me
 * </Button>
 */
export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = 'primary',
      size = 'medium',
      loading = false,
      children,
      icon,
      iconPosition = 'left',
      className,
      disabled,
      ...props
    },
    ref
  ) => {
    // 构建样式类名
    const classes = [
      styles.button,
      styles[variant],
      size !== 'medium' && styles[size],
      loading && styles.loading,
      className
    ]
      .filter(Boolean)
      .join(' ');

    return (
      <button
        ref={ref}
        className={classes}
        disabled={disabled || loading}
        aria-busy={loading}
        {...props}
      >
        {icon && iconPosition === 'left' && <span className={styles.icon}>{icon}</span>}
        <span>{children}</span>
        {icon && iconPosition === 'right' && <span className={styles.icon}>{icon}</span>}
      </button>
    );
  }
);

Button.displayName = 'Button';
```

### 使用主题变量的组件

```typescript
// src/main/frontend/react-app/components/Chart/Chart.tsx

import React, { useEffect, useRef } from 'react';
import { useThemeVariable } from '../../hooks/useThemeVariable';
import { Chart as ChartJS } from 'chart.js';

export interface ChartProps {
  type: 'line' | 'bar' | 'pie';
  data: any;
  options?: any;
}

/**
 * 图表组件
 *
 * 演示如何在JavaScript中读取CSS变量
 * 并传递给第三方库
 */
export function Chart({ type, data, options }: ChartProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const chartRef = useRef<ChartJS | null>(null);

  // 读取CSS变量
  const primaryColor = useThemeVariable('--color-primary');
  const textColor = useThemeVariable('--text-primary');
  const borderColor = useThemeVariable('--border-primary');
  const bgColor = useThemeVariable('--bg-secondary');

  useEffect(() => {
    if (!canvasRef.current || !primaryColor) return;

    // 销毁旧图表
    if (chartRef.current) {
      chartRef.current.destroy();
    }

    // 创建新图表，使用CSS变量颜色
    chartRef.current = new ChartJS(canvasRef.current, {
      type,
      data: {
        ...data,
        // 使用CSS变量颜色自定义数据集
      },
      options: {
        ...options,
        // 使用CSS变量颜色自定义选项
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
            },
            grid: {
              color: borderColor
            }
          },
          x: {
            ticks: {
              color: textColor
            },
            grid: {
              color: borderColor
            }
          }
        }
      }
    });

    return () => {
      if (chartRef.current) {
        chartRef.current.destroy();
      }
    };
  }, [type, data, options, primaryColor, textColor, borderColor]);

  return <canvas ref={canvasRef} />;
}
```

### 主题感知的容器组件

```typescript
// src/main/frontend/react-app/components/ThemeAwareContainer/ThemeAwareContainer.tsx

import React, { ReactNode } from 'react';
import { useThemeSync, Theme } from '../../hooks/useThemeSync';
import { useDerivedThemeValue } from '../../hooks/useDerivedThemeValue';
import styles from './ThemeAwareContainer.module.css';

export interface ThemeAwareContainerProps {
  children: ReactNode;
  onThemeChange?: (theme: Theme) => void;
}

/**
 * 主题感知容器
 *
 * 演示如何使用多个主题Hook
 */
export function ThemeAwareContainer({ children, onThemeChange }: ThemeAwareContainerProps) {
  // 监听主题变化
  const theme = useThemeSync((newTheme) => {
    console.log('Theme changed to:', newTheme);
    onThemeChange?.(newTheme);
  });

  // 计算派生值：基于背景色判断是否需要调整布局
  const shouldUseCompactLayout = useDerivedThemeValue((vars) => {
    const bgColor = vars['--bg-primary'];
    // 如果背景很深，使用紧凑布局
    return bgColor && bgColor.includes('0f') ? 'true' : 'false';
  }, ['--bg-primary']);

  // 计算派生值：文本阴影
  const textShadowColor = useDerivedThemeValue((vars) => {
    const bgColor = vars['--bg-primary'];
    // 浅色背景使用深色阴影，深色背景使用浅色阴影
    return bgColor && bgColor.includes('ff') ? 'rgba(0,0,0,0.2)' : 'rgba(255,255,255,0.1)';
  }, ['--bg-primary']);

  return (
    <div
      className={`${styles.container} ${shouldUseCompactLayout === 'true' ? styles.compact : ''}`}
      data-theme={theme}
      style={{
        textShadow: `0 1px 2px ${textShadowColor}`
      }}
    >
      <div className={styles.badge}>当前主题: {theme}</div>
      {children}
    </div>
  );
}
```

---

## Part 4: 应用集成

### App.tsx 整合

```typescript
// src/main/frontend/react-app/App.tsx

import React from 'react';
import { useThemeSync } from './hooks/useThemeSync';
import './index.css';

export function App() {
  // 监听主应用主题变化
  useThemeSync(
    (theme) => {
      console.log('[App] Theme synchronized:', theme);
      // React应用会自动继承新的CSS变量
      // 无需手动更新任何状态
    },
    {
      applyStoredTheme: false, // React应用跟随主应用主题
      pollInterval: 0          // 依赖事件，不需要定期检查
    }
  );

  return (
    <div className="deploy-platform-app">
      {/* 所有子组件都自动使用CSS变量 */}
      {/* 包括其他内容 */}
    </div>
  );
}
```

---

## Part 5: 测试和验证

### 主题同步测试

```typescript
// src/main/frontend/react-app/__tests__/ThemeSync.test.ts

import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useThemeSync } from '../hooks/useThemeSync';

describe('useThemeSync', () => {
  beforeEach(() => {
    // 重置主题
    document.documentElement.setAttribute('data-theme', 'light');
  });

  afterEach(() => {
    document.documentElement.removeAttribute('data-theme');
  });

  it('应该返回当前主题', () => {
    const { result } = renderHook(() => useThemeSync());
    expect(result.current).toBe('light');
  });

  it('应该监听主题属性变化', async () => {
    const callback = vi.fn();
    const { result } = renderHook(() => useThemeSync(callback));

    // 改变主题
    document.documentElement.setAttribute('data-theme', 'dark');

    await waitFor(() => {
      expect(result.current).toBe('dark');
      expect(callback).toHaveBeenCalledWith('dark');
    });
  });

  it('应该监听CustomEvent事件', async () => {
    const callback = vi.fn();
    renderHook(() => useThemeSync(callback));

    // 发出自定义事件
    const event = new CustomEvent('themeChanged', {
      detail: { theme: 'dark' }
    });
    document.dispatchEvent(event);

    await waitFor(() => {
      expect(callback).toHaveBeenCalledWith('dark');
    });
  });
});
```

### CSS变量可用性检查

```typescript
// src/main/frontend/react-app/__tests__/CSSVariables.test.ts

import { describe, it, expect } from 'vitest';

describe('CSS Variables', () => {
  // 需要的CSS变量列表
  const requiredVariables = [
    '--color-primary',
    '--bg-primary',
    '--text-primary',
    '--spacing-4',
    '--font-family-sans',
    '--transition-normal'
  ];

  it('应该定义所有必需的CSS变量', () => {
    const style = getComputedStyle(document.documentElement);

    requiredVariables.forEach(varName => {
      const value = style.getPropertyValue(varName).trim();
      expect(value).toBeTruthy(`CSS variable ${varName} should be defined`);
    });
  });

  it('应该在暗主题下更新变量', () => {
    // 设置暗主题
    document.documentElement.setAttribute('data-theme', 'dark');

    const style = getComputedStyle(document.documentElement);
    const bgColor = style.getPropertyValue('--bg-primary').trim();

    // 暗主题背景应该是深色
    expect(bgColor).toMatch(/0f|1e|2e/); // 深色的十六进制值
  });
});
```

---

## Part 6: 故障排查脚本

### 浏览器控制台调试脚本

```javascript
// 在浏览器控制台粘贴以下代码调试CSS变量

// 1. 检查所有CSS变量
function checkCSSVariables() {
  const style = getComputedStyle(document.documentElement);
  const variables = {};

  for (let i = 0; i < style.length; i++) {
    const prop = style[i];
    if (prop.startsWith('--')) {
      variables[prop] = style.getPropertyValue(prop).trim();
    }
  }

  console.table(variables);
  return variables;
}

// 2. 监听主题变化
function monitorThemeChanges() {
  let changeCount = 0;

  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.attributeName === 'data-theme') {
        changeCount++;
        const newTheme = document.documentElement.getAttribute('data-theme');
        console.log(`[Theme Change #${changeCount}]`, {
          timestamp: new Date().toLocaleTimeString(),
          newTheme,
          oldValue: mutation.oldValue
        });
      }
    });
  });

  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme'],
    attributeOldValue: true
  });

  document.addEventListener('themeChanged', (e) => {
    console.log('[Custom Event: themeChanged]', e.detail);
  });

  return observer;
}

// 3. 测试变量
function testVariable(varName) {
  const style = getComputedStyle(document.documentElement);
  const value = style.getPropertyValue(varName).trim();

  if (!value) {
    console.warn(`❌ Variable ${varName} is not defined`);
    console.log('Available variables:', checkCSSVariables());
  } else {
    console.log(`✅ ${varName} = ${value}`);
  }
}

// 4. 手动切换主题（测试）
function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'light';
  const next = current === 'light' ? 'dark' : 'light';
  document.documentElement.setAttribute('data-theme', next);
  console.log(`Theme changed from ${current} to ${next}`);
}

// 使用示例
checkCSSVariables();        // 列表所有变量
monitorThemeChanges();      // 开始监听
testVariable('--color-primary');  // 测试某个变量
toggleTheme();              // 切换主题
```

---

## Part 7: 部署检查清单

- [ ] **主应用配置**
  - [ ] CSS变量定义在`:root`
  - [ ] 使用`[data-theme]`标记主题
  - [ ] 触发`themeChanged` CustomEvent

- [ ] **React应用配置**
  - [ ] 导入useThemeSync Hook
  - [ ] CSS文件使用`var()`引用变量
  - [ ] 移除所有硬编码的颜色值

- [ ] **样式管理**
  - [ ] 建立shared.css共享样式
  - [ ] 组件使用CSS Modules
  - [ ] 变量命名遵循约定

- [ ] **测试验证**
  - [ ] 主题切换功能正常
  - [ ] 所有CSS变量可用
  - [ ] 没有控制台警告
  - [ ] 暗主题和亮主题都正常显示

- [ ] **文档更新**
  - [ ] 更新CSS变量文档
  - [ ] 添加组件集成示例
  - [ ] 记录常见问题

---

**版本：** v1.0
**最后更新：** 2025-11-02
