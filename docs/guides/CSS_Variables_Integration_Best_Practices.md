# React应用CSS变量继承最佳实践指南

## 执行摘要

本文档研究了如何在嵌入式React应用中高效继承主应用的CSS自定义属性，同时保持主题切换同步和样式隔离。基于项目现状分析，提供了推荐的集成方案、实现模式和故障排查指南。

**关键发现：**
- CSS变量自动继承于DOM树，无需显式配置
- React应用需要建立主题同步机制而非重复定义
- `data-theme`属性是更可靠的主题标记方式
- 样式隔离与共享可通过CSS Modules + CSS变量混合实现

---

## 1. CSS变量继承机制详解

### 1.1 自动继承原理

CSS自定义属性（CSS Variables）遵循标准CSS继承规则：

```css
/* 主应用 (main-layout.html) */
:root {
  --color-primary: #6366f1;
  --bg-primary: #ffffff;
  --text-primary: #1e293b;
}

[data-theme="dark"] {
  --bg-primary: #0f172a;
  --text-primary: #f1f5f9;
}
```

```html
<!-- React应用容器继承所有:root和[data-theme]中定义的变量 -->
<div id="deploy-platform-root">
  <!-- React应用内部所有元素自动继承这些变量 -->
</div>
```

**关键机制：**
- 变量定义在`:root`上 → 所有子元素继承
- 变量定义在`[data-theme]`选择器上 → 受主题状态影响的变量继承
- 没有显式指定时，子元素自动使用父元素的变量值

### 1.2 继承查找链

```
组件的CSS变量查找顺序：
1. 组件自身的CSS (如果有局部定义)
2. 父组件的CSS
3. html/body元素
4. :root选择器
5. 浏览器默认值

示例：
.react-component {
  color: var(--text-primary);  /* 查找链：本地→父→html→:root→#1e293b */
}
```

**继承工作示例：**

```tsx
// src/main/frontend/react-app/components/Button.tsx
export function Button() {
  return (
    <button className="btn">
      Click Me
    </button>
  );
}
```

```css
/* React应用的CSS (index.css) */
.btn {
  color: var(--text-primary);  /* 自动继承主应用的--text-primary */
  background: var(--bg-secondary);  /* 自动继承 */
  border: 1px solid var(--border-primary);  /* 自动继承 */
}
```

当主应用切换主题 `[data-theme="dark"]` 时，所有引用这些变量的样式自动更新。

---

## 2. React组件访问CSS变量的方式

### 2.1 在样式表中直接使用

**推荐方式：** 直接在CSS中引用，无需JavaScript干预

```css
/* src/main/frontend/react-app/index.css */

.deploy-platform-app {
  /* 直接使用继承的变量 */
  color: var(--text-primary);
  background: var(--bg-primary);
  font-family: var(--font-family-sans);
}

.dp-btn {
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--text-white);
}
```

**优点：**
- 性能最佳（零JavaScript开销）
- 变量变化自动应用（浏览器原生支持）
- 与CSS预处理器兼容（Sass/LESS降级）

### 2.2 在JavaScript中读取CSS变量

**场景：** 当React组件需要基于当前主题做逻辑决策时

```typescript
// src/main/frontend/react-app/hooks/useThemeVariable.ts
import { useEffect, useState } from 'react';

/**
 * Hook: 读取CSS变量的当前值
 * @param variableName - CSS变量名（含--前缀）
 * @returns 变量的计算值
 */
export function useThemeVariable(variableName: string): string {
  const [value, setValue] = useState<string>('');

  useEffect(() => {
    // 获取root元素的计算样式
    const computedStyle = getComputedStyle(document.documentElement);

    // 读取变量值（自动处理继承）
    const variableValue = computedStyle.getPropertyValue(variableName).trim();
    setValue(variableValue);
  }, [variableName]);

  return value;
}

// 使用示例
export function ThemeAwareComponent() {
  const primaryColor = useThemeVariable('--color-primary');
  const textColor = useThemeVariable('--text-primary');

  return (
    <div style={{
      color: textColor,
      borderColor: primaryColor
    }}>
      Theme-aware content
    </div>
  );
}
```

### 2.3 计算派生值

**场景：** 需要基于CSS变量计算新值时

```typescript
// src/main/frontend/react-app/hooks/useDerivedThemeValue.ts

/**
 * 基于CSS变量计算派生值
 * 例如：根据背景色自动计算文本颜色
 */
export function useDerivedThemeValue(fn: (variables: ThemeVariables) => string) {
  const [value, setValue] = useState<string>('');

  useEffect(() => {
    const style = getComputedStyle(document.documentElement);

    const variables: ThemeVariables = {
      bgPrimary: style.getPropertyValue('--bg-primary').trim(),
      textPrimary: style.getPropertyValue('--text-primary').trim(),
      colorPrimary: style.getPropertyValue('--color-primary').trim(),
      // ... 更多变量
    };

    const derived = fn(variables);
    setValue(derived);
  }, [fn]);

  return value;
}

// 使用
export function SmartContainer() {
  const textColor = useDerivedThemeValue(vars => {
    // 如果背景是浅色，返回深色文本
    const isBgLight = !vars.bgPrimary.includes('0f');
    return isBgLight ? '#1e293b' : '#f1f5f9';
  });
}
```

### 2.4 与第三方UI库集成

```typescript
// src/main/frontend/react-app/config/themeConfig.ts

/**
 * 从CSS变量生成UI库主题配置
 */
export function generateThemeConfig() {
  const style = getComputedStyle(document.documentElement);

  return {
    colors: {
      primary: style.getPropertyValue('--color-primary').trim(),
      background: style.getPropertyValue('--bg-primary').trim(),
      text: style.getPropertyValue('--text-primary').trim(),
    },
    spacing: {
      sm: style.getPropertyValue('--spacing-2').trim(),
      md: style.getPropertyValue('--spacing-4').trim(),
      lg: style.getPropertyValue('--spacing-8').trim(),
    }
  };
}
```

---

## 3. 主题切换时的同步策略

### 3.1 主题变化监听机制

**当前项目情况：**
```typescript
// src/main/frontend/modules/theme.ts (主应用)

class ThemeManager {
  toggle(): void {
    const isDark = document.body.classList.toggle(this.DARK_CLASS);

    if (isDark) {
      document.documentElement.classList.add('theme-dark');
      document.documentElement.setAttribute('data-theme', 'dark');
    } else {
      document.documentElement.classList.remove('theme-dark');
      document.documentElement.setAttribute('data-theme', 'light');
    }

    // 触发全局事件通知所有应用
    eventBus.emit("theme:changed", { theme: isDark ? 'dark' : 'light' });

    const themeEvent = new CustomEvent("themeChanged", {
      detail: { theme: isDark ? 'dark' : 'light' }
    });
    document.dispatchEvent(themeEvent);
  }
}
```

**推荐改进方案：**

```typescript
// src/main/frontend/react-app/hooks/useThemeSync.ts

import { useEffect } from 'react';

/**
 * Hook: 监听主应用主题变化并同步React应用
 *
 * 同步策略：
 * 1. 监听data-theme属性变化（标准方式）
 * 2. 监听CustomEvent（向后兼容）
 * 3. 定期检查属性变化（备用方案）
 */
export function useThemeSync(onThemeChange?: (theme: 'light' | 'dark') => void) {
  useEffect(() => {
    // 方案A: MutationObserver监听属性变化
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.attributeName === 'data-theme') {
          const newTheme = document.documentElement.getAttribute('data-theme') as 'light' | 'dark';
          onThemeChange?.(newTheme);
        }
      });
    });

    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['data-theme']
    });

    // 方案B: 监听CustomEvent（主应用发出）
    const handleThemeChange = (event: CustomEvent) => {
      const theme = event.detail?.theme as 'light' | 'dark';
      onThemeChange?.(theme);
    };

    document.addEventListener('themeChanged', handleThemeChange as EventListener);

    // 方案C: 监听事件总线（如果使用EventBus）
    const handleEventBusTheme = ({ theme }: { theme: 'light' | 'dark' }) => {
      onThemeChange?.(theme);
    };

    if (window.eventBus?.on) {
      window.eventBus.on('theme:changed', handleEventBusTheme);
    }

    return () => {
      observer.disconnect();
      document.removeEventListener('themeChanged', handleThemeChange as EventListener);
      if (window.eventBus?.off) {
        window.eventBus.off('theme:changed', handleEventBusTheme);
      }
    };
  }, [onThemeChange]);
}

// 使用示例
export function App() {
  useThemeSync((theme) => {
    console.log(`主题已切换为: ${theme}`);
    // React应用自动继承新的CSS变量值
    // 无需手动更新状态（CSS变量的改变会自动触发浏览器重排）
  });

  return <AppContent />;
}
```

### 3.2 数据属性（data-theme）vs 类名（class）

**对比分析：**

| 方面 | data-theme 属性 | CSS类名 |
|------|----------------|---------|
| **可靠性** | ✅ 高 - 专用于主题标记 | ⚠️ 中 - 易与其他class冲突 |
| **性能** | ✅ 快 - 属性解析更高效 | ⚠️ 中 - 类名匹配较慢 |
| **可读性** | ✅ 好 - 语义清晰 | ⚠️ 差 - 需要约定 |
| **CSS规则** | `.selector[data-theme="dark"] {}` | `.selector.dark-theme {}` |
| **JavaScript访问** | `el.getAttribute('data-theme')` | `el.classList.contains('dark-theme')` |

**项目当前状况：**
```html
<!-- main-layout.html -->
<html data-theme="light">
  <!-- 同时使用data-theme和class（冗余） -->
</html>
```

**推荐改进：**
```html
<!-- 统一使用data-theme属性 -->
<html data-theme="light">
  <head>
    <link rel="stylesheet" href="/css/design-tokens.css">
  </head>
  <body>
    <div id="deploy-platform-root"></div>
  </body>
</html>
```

```css
/* design-tokens.css */
:root {
  --bg-primary: #ffffff;
  --text-primary: #1e293b;
}

/* 使用data-theme选择器 */
[data-theme="dark"] {
  --bg-primary: #0f172a;
  --text-primary: #f1f5f9;
}
```

### 3.3 初始主题加载

**优化方案：**

```typescript
// src/main/frontend/modules/theme-bootstrap.ts

/**
 * 页面加载时立即设置主题，避免FOUC（Flash of Unstyled Content）
 */
function initThemeBeforePaint() {
  // 在head中内联此脚本，在CSS加载前执行
  const savedTheme = localStorage.getItem('theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);

  // 立即应用CSS变量，不等待React挂载
  if (savedTheme === 'dark') {
    // CSS变量已在[data-theme="dark"]中定义
    // 浏览器会自动应用
  }
}

// 在main-layout.html的<head>中内联
// <script>initThemeBeforePaint();</script>
```

---

## 4. Shadow DOM的影响分析

### 4.1 CSS变量在Shadow DOM中的行为

**默认行为：CSS变量无法穿透Shadow DOM边界**

```typescript
// 这种方式会失败
export function ShadowComponentFailed() {
  return (
    <div id="shadow-host"></div>
  );
}

useEffect(() => {
  const host = document.getElementById('shadow-host');
  const shadow = host.attachShadow({ mode: 'open' });

  shadow.innerHTML = `
    <style>
      :host {
        color: var(--text-primary);  /* ❌ 不工作 - 变量未定义 */
      }
    </style>
  `;
}, []);
```

**原因：** Shadow DOM有自己的样式作用域，`var(--text-primary)`在Shadow DOM内部无法找到定义

### 4.2 解决方案

**方案A：在Shadow DOM宿主元素注入变量（推荐）**

```typescript
export function ShadowComponentFixed() {
  return (
    <div
      id="shadow-host"
      style={{
        // 在宿主元素上明确设置变量
        '--text-primary': 'var(--text-primary)',
        '--bg-primary': 'var(--bg-primary)',
      } as React.CSSProperties}
    >
      <ShadowContent />
    </div>
  );
}

function ShadowContent() {
  useEffect(() => {
    const host = document.getElementById('shadow-host');
    const shadow = host.attachShadow({ mode: 'open' });

    shadow.innerHTML = `
      <style>
        :host {
          color: var(--text-primary);  /* ✅ 现在工作了 */
          background: var(--bg-primary);
        }
      </style>
    `;
  }, []);

  return null;
}
```

**方案B：使用CSS-in-JS库（如Styled Components）**

```typescript
import styled from 'styled-components';

// Styled Components会自动处理变量继承
const StyledButton = styled.button`
  color: var(--text-primary);
  background: var(--bg-primary);
  padding: var(--spacing-4);
`;
```

**方案C：避免Shadow DOM，使用CSS Modules**

```typescript
// 推荐方案 - 不使用Shadow DOM，使用CSS Modules实现样式隔离
import styles from './Component.module.css';

export function Component() {
  return <div className={styles.container}>Content</div>;
}
```

```css
/* Component.module.css */
.container {
  color: var(--text-primary);  /* ✅ 自动继承 */
  background: var(--bg-primary);
}
```

---

## 5. 样式隔离 vs 样式共享的权衡

### 5.1 不同隔离方案对比

| 方案 | 隔离程度 | CSS变量继承 | 复杂度 | 推荐场景 |
|------|---------|-----------|-------|--------|
| **全局CSS** | 无 | ✅ 完全继承 | 低 | 样式共享，主题一致 |
| **CSS Modules** | ✅ 高 | ✅ 仍可继承变量 | 中 | React组件库 |
| **CSS-in-JS** | ✅ 高 | ✅ 支持变量 | 中 | 动态主题 |
| **Shadow DOM** | ✅ 完全 | ❌ 需手动处理 | 高 | 微前端隔离 |
| **iFrame** | ✅ 完全 | ❌ 需重复定义 | 高 | 完全独立应用 |

### 5.2 推荐架构：混合方案

```
项目结构：
src/main/frontend/react-app/
├── shared/              # 共享层 - 使用全局CSS变量
│   └── styles/
│       └── shared.css   # 继承主应用变量
├── components/          # 组件层 - CSS Modules + 变量
│   └── Button/
│       ├── Button.tsx
│       ├── Button.module.css  # color: var(--text-primary)
├── pages/              # 页面层 - 少量局部覆盖
│   └── Home/
│       ├── Home.tsx
│       └── Home.module.css
└── index.css           # 全局变量定义（备份）
```

**实现示例：**

```css
/* shared/styles/shared.css */
/* 直接使用继承的主应用变量 */

.text-primary {
  color: var(--text-primary);
}

.bg-primary {
  background: var(--bg-primary);
}

.btn-base {
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
  font-family: var(--font-family-sans);
}
```

```css
/* components/Button/Button.module.css */
.button {
  composes: btn-base from '../shared/styles/shared.css';

  /* 继承主应用变量 */
  background: var(--color-primary);
  color: var(--text-white);

  /* 局部覆盖 */
  border: none;
  cursor: pointer;
}

.button:hover {
  background: var(--color-primary-dark);
}

/* 支持Dark主题 */
[data-theme="dark"] .button {
  /* 变量自动更新，无需重复定义 */
}
```

```tsx
// components/Button/Button.tsx
import styles from './Button.module.css';

export function Button({ children, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button className={styles.button} {...props}>
      {children}
    </button>
  );
}
```

### 5.3 处理特殊场景

**场景1：React应用需要扩展主应用变量**

```css
/* src/main/frontend/react-app/index.css */

/* 继承主应用的基础变量 */
@import url('/css/design-tokens.css');

/* 扩展React特有的变量（不会污染主应用） */
:root {
  --react-app-sidebar-width: 320px;
  --react-app-card-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 当React应用在独立模式运行时也有完整的设计系统 */
@media (prefers-color-scheme: dark) {
  :root {
    --react-app-card-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }
}
```

**场景2：主应用变量版本不兼容**

```typescript
// src/main/frontend/react-app/hooks/useCompatibleTheme.ts

/**
 * 兼容性Hook - 当主应用变量缺失时使用备用值
 */
export function useCompatibleTheme(variableName: string, fallback: string) {
  const [value, setValue] = useState<string>(fallback);

  useEffect(() => {
    const computed = getComputedStyle(document.documentElement);
    const variable = computed.getPropertyValue(variableName).trim();

    // 如果变量未定义，使用备用值
    setValue(variable || fallback);
  }, [variableName, fallback]);

  return value;
}

// 使用
const primaryColor = useCompatibleTheme('--color-primary', '#6366f1');
```

---

## 6. 实现方案总结

### 6.1 推荐集成方案（Best Practice）

```
阶段1: 主应用配置
  ✅ 在:root定义全局CSS变量
  ✅ 在[data-theme="dark"]覆盖变量
  ✅ 触发主题变化事件
  ✅ 通过data-theme属性标记主题

阶段2: React应用接入
  ✅ 直接在CSS中使用var()引用变量
  ✅ 通过useThemeSync Hook监听主题变化
  ✅ 使用CSS Modules隔离组件样式
  ✅ 在需要时用useThemeVariable读取变量值

阶段3: 样式架构
  ✅ shared/styles/shared.css - 全局样式
  ✅ components/**/*.module.css - 组件样式
  ✅ 避免Shadow DOM，使用CSS Modules
  ✅ 保持变量命名一致性
```

### 6.2 代码检查清单

```typescript
// ✅ 推荐
.button {
  color: var(--text-primary);  /* 直接使用变量 */
  background: var(--color-primary);
}

// ❌ 避免
.button {
  color: #1e293b;  /* 硬编码颜色 */
  background: #6366f1;
}

// ✅ 推荐
@media (prefers-color-scheme: dark) {
  .button {
    /* CSS变量已在[data-theme="dark"]中更新 */
    /* 无需重复定义 */
  }
}

// ❌ 避免
.dark-theme .button {
  color: #f1f5f9;  /* 重复定义颜色 */
}

// ✅ 推荐
const theme = useThemeSync();  /* Hook监听变化 */

// ❌ 避免
const theme = localStorage.getItem('theme');  /* 手动管理状态 */
```

---

## 7. CSS变量命名约定

### 7.1 项目当前命名方案

```css
/* 基础颜色变量 */
--color-primary: #6366f1;        /* 主品牌色 */
--color-primary-light: #818cf8;  /* 浅色变体 */
--color-primary-dark: #4f46e5;   /* 深色变体 */

/* 语义化颜色变量 */
--color-success: #10b981;
--color-warning: #f59e0b;
--color-error: #ef4444;
--color-info: #3b82f6;

/* 背景颜色 */
--bg-primary: #ffffff;      /* 主背景 */
--bg-secondary: #f8fafc;    /* 次背景 */
--bg-overlay: rgba(...);    /* 叠加层 */

/* 文字颜色 */
--text-primary: #1e293b;    /* 主文本 */
--text-secondary: #475569;  /* 次文本 */
--text-muted: #6b7280;      /* 弱化文本 */

/* 间距系统 */
--spacing-1: 0.25rem;  /* 4px */
--spacing-2: 0.5rem;   /* 8px */
--spacing-4: 1rem;     /* 16px */

/* 排版系统 */
--font-family-sans: 'Inter', sans-serif;
--font-size-base: 1rem;
--font-weight-normal: 400;

/* 动画 */
--transition-fast: 150ms;
--transition-normal: 300ms;
```

### 7.2 React应用扩展变量

```css
/* React应用专有变量 - 避免与主应用冲突 */
:root {
  /* 前缀：--react- 表示React应用专用 */
  --react-sidebar-width: 320px;
  --react-content-max-width: 1200px;
  --react-card-shadow: var(--shadow-md);

  /* 复合变量 - 重用主应用变量 */
  --react-primary-light: var(--color-primary-light);
  --react-error-bg: var(--color-error-bg);
}
```

### 7.3 命名最佳实践

```
命名规则：
  --[domain]-[component]-[property]-[state]?

示例：
  ✅ --shell-nav-bg                    /* shell组件的nav背景 */
  ✅ --react-button-bg-hover           /* react按钮悬停状态 */
  ✅ --color-primary                   /* 全局主色 */
  ❌ --primary                         /* 太简洁，容易冲突 */
  ❌ --my-custom-color                 /* 个人化命名，不规范 */
```

---

## 8. 故障排查指南

### 8.1 CSS变量未生效的常见原因

| 问题 | 原因 | 解决方案 |
|------|------|--------|
| 变量为空值 | 变量定义在错误的选择器中 | 检查变量定义在`:root`或`[data-theme]` |
| Dark主题下变量不变 | 缺少`[data-theme="dark"]`选择器 | 在CSS中添加主题选择器覆盖 |
| Shadow DOM中无法使用变量 | Shadow DOM隔离 | 在宿主元素注入变量 |
| React刷新后变量复位 | 主题状态未同步 | 使用useThemeSync监听变化 |

### 8.2 调试工具使用

```javascript
// 在浏览器控制台调试CSS变量

// 1. 读取所有CSS变量
const style = getComputedStyle(document.documentElement);
const allVars = {};
for (let i = 0; i < style.length; i++) {
  const prop = style[i];
  if (prop.startsWith('--')) {
    allVars[prop] = style.getPropertyValue(prop);
  }
}
console.table(allVars);

// 2. 修改变量以测试
document.documentElement.style.setProperty('--color-primary', '#ff0000');

// 3. 查看元素的计算样式
const el = document.querySelector('.button');
console.log(getComputedStyle(el));

// 4. 监听变量变化
const observer = new MutationObserver(() => {
  console.log('CSS variables changed');
});
observer.observe(document.documentElement, { attributes: true });
```

### 8.3 性能检查

```typescript
// 检查CSS变量导致的重排/重绘

// ❌ 差的做法 - 频繁修改变量
setInterval(() => {
  document.documentElement.style.setProperty('--color-primary', newColor);
}, 100);  // 频繁触发重排

// ✅ 好的做法 - 批量修改变量
function updateTheme(newVars: Record<string, string>) {
  const root = document.documentElement.style;
  Object.entries(newVars).forEach(([key, value]) => {
    root.setProperty(key, value);
  });
}

// ✅ 最好的做法 - 使用data-theme属性（由CSS处理）
document.documentElement.setAttribute('data-theme', 'dark');
```

---

## 9. 替代方案对比与缺点分析

### 9.1 Sass变量 vs CSS变量

```scss
// Sass变量 - 编译时替换
$color-primary: #6366f1;

.button {
  background: $color-primary;  // 编译后变成 background: #6366f1;
}
```

**缺点：**
- 编译时确定，运行时无法改变
- 无法实现动态主题切换
- 每个主题需要单独编译

```css
/* CSS变量 - 运行时计算（推荐） */
:root {
  --color-primary: #6366f1;
}

[data-theme="dark"] {
  --color-primary: #4f46e5;
}

.button {
  background: var(--color-primary);  // 运行时读取，可动态改变
}
```

**优点：**
- 运行时动态改变
- 支持主题切换
- 一份CSS支持多套主题

### 9.2 CSS-in-JS库 vs CSS变量

```typescript
// styled-components（CSS-in-JS）
const StyledButton = styled.button`
  background: ${props => props.theme.colors.primary};
`;
```

**缺点：**
- 增加bundle体积
- 运行时性能开销（JS解析和生成CSS）
- 学习成本

```css
/* CSS变量（推荐） */
.button {
  background: var(--color-primary);
}
```

**优点：**
- 原生支持，零开销
- 浏览器优化良好
- 与任何框架兼容

### 9.3 Context API + 状态管理 vs CSS变量

```typescript
// 不推荐：使用React Context管理主题
const ThemeContext = createContext();

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState('light');
  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}
```

**缺点：**
- 不必要的React重新渲染
- 需要在每个组件中useContext
- 不适合嵌入式应用（与主应用脱离）

```typescript
// 推荐：仅用CSS变量处理主题
// 所有主题逻辑由[data-theme]属性处理
// React不需要管理主题状态
```

### 9.4 iFrame隔离 vs CSS变量

```html
<!-- iFrame - 完全隔离但复杂 -->
<iframe src="/react-app/index.html"></iframe>

<!-- 缺点：
  - CSS变量无法继承（需在iframe内重复定义）
  - 通信复杂（需postMessage API）
  - 加载时间长
  - 不适合嵌入式应用
-->
```

```html
<!-- CSS变量隔离 - 简单而优雅 -->
<div id="deploy-platform-root" data-router-base="/deploy-platform/"></div>
<script src="/react-app/dist/main.js"></script>

<!-- 优点：
  - CSS变量自动继承
  - 零通信开销
  - 可以共享DOM事件
  - 适合嵌入式应用
-->
```

---

## 10. 实施建议

### 10.1 短期改进（立即执行）

1. **统一主题标记方式**
   ```html
   <!-- 从class改为data-theme -->
   <html data-theme="light">
   ```

2. **React应用添加主题同步**
   ```typescript
   // useThemeSync Hook - 实现主题变化监听
   export function useThemeSync(callback) { ... }
   ```

3. **审查CSS文件**
   - 确保所有颜色使用CSS变量
   - 移除硬编码的颜色值
   - 统一变量命名

### 10.2 中期优化（1-2周）

1. **建立共享样式库**
   ```
   shared/styles/
   ├── shared.css          # 全局变量副本
   ├── variables.css       # 变量扩展
   └── utilities.css       # 工具类
   ```

2. **React应用CSS重构**
   - 所有组件迁移到CSS Modules
   - 使用CSS变量替代硬编码值

3. **文档更新**
   - 编写CSS变量使用指南
   - 记录主题定制流程

### 10.3 长期优化（1个月）

1. **自动化测试**
   - 主题切换测试
   - CSS变量完整性检查

2. **性能监控**
   - 追踪主题切换性能
   - 监测CSS变量导致的重排

3. **扩展性规划**
   - 多主题支持（品牌自定义）
   - 动态主题生成

---

## 11. 完整示例代码

### 11.1 项目文件结构

```
src/main/frontend/react-app/
├── index.css
├── hooks/
│   ├── useThemeSync.ts        # 主题同步Hook
│   ├── useThemeVariable.ts    # 读取变量Hook
│   └── useDerivedThemeValue.ts # 派生值Hook
├── styles/
│   └── shared.css             # 继承变量的共享样式
├── components/
│   ├── Button/
│   │   ├── Button.tsx
│   │   └── Button.module.css
│   └── Card/
│       ├── Card.tsx
│       └── Card.module.css
└── App.tsx
```

### 11.2 完整Hook实现

```typescript
// src/main/frontend/react-app/hooks/useThemeSync.ts

import { useEffect, useState } from 'react';

export type Theme = 'light' | 'dark';

/**
 * Hook: 监听主应用主题变化
 *
 * 工作原理：
 * 1. 监听data-theme属性变化（主方式）
 * 2. 监听CustomEvent事件（备用方式）
 * 3. 定期检查属性变化（保险方式）
 */
export function useThemeSync(
  onThemeChange?: (theme: Theme) => void,
  options?: { pollInterval?: number }
): Theme {
  const [theme, setTheme] = useState<Theme>(() => {
    // 初始化时读取当前主题
    return (document.documentElement.getAttribute('data-theme') as Theme) || 'light';
  });

  useEffect(() => {
    const handleThemeChange = (newTheme: Theme) => {
      setTheme(newTheme);
      onThemeChange?.(newTheme);
    };

    // 方案A: MutationObserver监听属性变化
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
      attributeFilter: ['data-theme']
    });

    // 方案B: 监听CustomEvent
    const handleCustomEvent = (event: Event) => {
      const customEvent = event as CustomEvent<{ theme: Theme }>;
      const newTheme = customEvent.detail?.theme;
      if (newTheme) {
        handleThemeChange(newTheme);
      }
    };

    document.addEventListener('themeChanged', handleCustomEvent);

    // 方案C: 监听EventBus事件
    const handleEventBusTheme = ({ theme: newTheme }: { theme: Theme }) => {
      handleThemeChange(newTheme);
    };

    if ((window as any).eventBus?.on) {
      (window as any).eventBus.on('theme:changed', handleEventBusTheme);
    }

    // 方案D: 定期检查（保险方案）
    let pollInterval: NodeJS.Timeout | null = null;
    if (options?.pollInterval) {
      pollInterval = setInterval(() => {
        const currentTheme = (document.documentElement.getAttribute('data-theme') as Theme) || 'light';
        if (currentTheme !== theme) {
          handleThemeChange(currentTheme);
        }
      }, options.pollInterval);
    }

    return () => {
      observer.disconnect();
      document.removeEventListener('themeChanged', handleCustomEvent);
      if ((window as any).eventBus?.off) {
        (window as any).eventBus.off('theme:changed', handleEventBusTheme);
      }
      if (pollInterval) {
        clearInterval(pollInterval);
      }
    };
  }, [onThemeChange, options?.pollInterval]);

  return theme;
}

// 使用示例
export function App() {
  const theme = useThemeSync(
    (newTheme) => {
      console.log(`主题已切换为: ${newTheme}`);
    },
    { pollInterval: 5000 }  // 每5秒检查一次（可选）
  );

  return (
    <div className="app" data-current-theme={theme}>
      {/* React应用会自动继承CSS变量 */}
    </div>
  );
}
```

```typescript
// src/main/frontend/react-app/hooks/useThemeVariable.ts

import { useEffect, useState } from 'react';

/**
 * Hook: 读取当前CSS变量值
 *
 * 用途：
 * - 需要在JavaScript中访问CSS变量
 * - 需要基于变量值做条件判断
 * - 需要传递变量值给第三方库
 */
export function useThemeVariable(variableName: string): string {
  const [value, setValue] = useState<string>('');

  useEffect(() => {
    const updateValue = () => {
      const computed = getComputedStyle(document.documentElement);
      const variableValue = computed.getPropertyValue(variableName).trim();

      // 如果值为空，尝试从CSS中解析
      if (!variableValue) {
        console.warn(`CSS variable not found: ${variableName}`);
      }

      setValue(variableValue);
    };

    // 初始读取
    updateValue();

    // 监听主题变化后重新读取
    const handleThemeChange = () => {
      updateValue();
    };

    document.addEventListener('themeChanged', handleThemeChange);

    return () => {
      document.removeEventListener('themeChanged', handleThemeChange);
    };
  }, [variableName]);

  return value;
}

// 使用示例
export function ColorfulButton() {
  const primaryColor = useThemeVariable('--color-primary');
  const textColor = useThemeVariable('--text-primary');

  return (
    <button
      style={{
        backgroundColor: primaryColor,
        color: textColor
      }}
    >
      Click me
    </button>
  );
}
```

### 11.3 完整样式文件示例

```css
/* src/main/frontend/react-app/index.css */

/* 导入主应用的设计系统（可选，作为备份） */
@import url('/css/design-tokens.css');

/* React应用全局样式 */
:root {
  /* 扩展React特有的变量 */
  --react-sidebar-width: 320px;
  --react-content-max-width: 1200px;
  --react-transition-duration: 300ms;
}

.deploy-platform-app {
  display: flex;
  flex-direction: column;

  /* 继承主应用变量 */
  color: var(--text-primary);
  background: var(--bg-primary);
  font-family: var(--font-family-sans);
  font-size: var(--font-size-base);

  /* 过渡 */
  transition: color var(--transition-normal),
              background var(--transition-normal);
}

/* 主题切换时自动应用 */
[data-theme="dark"] .deploy-platform-app {
  /* 无需重复定义 - CSS变量自动更新 */
}
```

```css
/* src/main/frontend/react-app/components/Button/Button.module.css */

.button {
  /* 继承主应用变量 */
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
  font-family: var(--font-family-sans);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);

  /* 主按钮样式 */
  background: var(--color-primary);
  color: var(--text-white);
  border: none;
  cursor: pointer;

  /* 过渡 */
  transition: all var(--transition-fast);

  /* 无障碍 */
  outline-offset: 2px;
}

.button:hover {
  background: var(--color-primary-dark);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.button:active {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
}

.button:disabled {
  opacity: var(--state-disabled-opacity);
  cursor: not-allowed;
  transform: none;
}

/* 主题变化自动适应 */
[data-theme="dark"] .button {
  /* CSS变量已在[data-theme="dark"]中更新 */
}

/* 按钮变体 */
.button.secondary {
  background: var(--bg-secondary);
  color: var(--text-primary);
  border: 1px solid var(--border-primary);
}

.button.secondary:hover {
  background: var(--bg-tertiary);
}
```

---

## 12. 参考资源

### 12.1 CSS变量相关文档
- [MDN: CSS Custom Properties](https://developer.mozilla.org/en-US/docs/Web/CSS/--*)
- [CSS Variables Specification](https://www.w3.org/TR/css-variables-1/)
- [CSS Variables Browser Support](https://caniuse.com/css-variables)

### 12.2 主题管理最佳实践
- [OpenProps - CSS变量框架](https://open-props.style/)
- [System UI](https://system-ui.com/)
- [Tailwind CSS - 主题定制](https://tailwindcss.com/docs/theme)

### 12.3 项目相关文件
- `/css/design-tokens.css` - 主应用设计系统
- `/src/main/frontend/modules/theme.ts` - 主应用主题管理
- `/src/main/frontend/react-app/index.css` - React应用样式

---

## 附录：快速参考表

### A1. 常用CSS变量列表

```css
/* 颜色系统 */
--color-primary         /* 主品牌色 */
--color-success         /* 成功绿 */
--color-warning         /* 警告黄 */
--color-error           /* 错误红 */
--bg-primary            /* 主背景 */
--text-primary          /* 主文本 */

/* 间距系统 */
--spacing-1 to --spacing-24    /* 4px to 96px */

/* 排版 */
--font-family-sans      /* 无衬线字体 */
--font-size-base        /* 基础字体 */
--font-weight-normal    /* 正常粗细 */

/* 动画 */
--transition-fast       /* 150ms */
--transition-normal     /* 300ms */
```

### A2. 故障排查流程图

```
CSS变量不生效？
│
├─ 变量值为空
│  └─ 检查变量定义位置（:root vs [data-theme="dark"]）
│
├─ 仅在某些元素不生效
│  └─ 检查CSS优先级（内联样式>ID>类）
│
├─ Dark主题下不变
│  └─ 确保[data-theme="dark"]选择器中有变量定义
│
├─ Shadow DOM中无效
│  └─ 在宿主元素上注入变量
│
└─ React中变量未更新
   └─ 添加useThemeSync Hook监听主题变化
```

---

**文档版本：** v1.0
**最后更新：** 2025-11-02
**作者：** Claude Code (AI Assistant)
**相关模块：** 前端架构、主题系统、样式管理
