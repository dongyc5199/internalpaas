# CSS变量集成 - 快速入门指南

**版本**: v1.0
**创建日期**: 2025-11-02
**目标受众**: 前端开发者（5分钟快速上手）

---

## 一句话总结

**在React应用中使用CSS变量，自动继承主应用的主题，零配置！**

```css
/* 主应用定义 */
:root { --color-primary: #6366f1; }

/* React应用直接用 */
.button { background: var(--color-primary); }  /* ✓ 完成！ */
```

---

## 3分钟验证

### 步骤1: 检查CSS变量是否存在

在浏览器F12控制台粘贴:

```javascript
// 检查是否有CSS变量
const value = getComputedStyle(document.documentElement)
    .getPropertyValue('--color-primary');
console.log('主色:', value);  // 应该显示 #6366f1
```

**如果显示值** → ✓ CSS变量已可用，继续步骤2
**如果显示空字符串** → ❌ CSS文件未加载，检查HTML中的`<link>`标签

### 步骤2: 在React组件中使用

创建或编辑 `Button.module.css`:

```css
.button {
    background: var(--color-primary);  /* 直接使用变量 */
    color: white;
    padding: 8px 16px;
    border-radius: 8px;
    border: none;
    cursor: pointer;
}
```

更新 `Button.tsx`:

```typescript
import styles from './Button.module.css';

export function Button({ children }: { children: React.ReactNode }) {
    return <button className={styles.button}>{children}</button>;
}
```

### 步骤3: 测试

```bash
# 启动应用
npm start

# 在浏览器中查看按钮
# 应该使用 --color-primary 的颜色（#6366f1 紫色）
```

**完成！** 你的组件现在使用主应用的主题颜色了。

---

## 常用的CSS变量

### 颜色变量

```css
/* 主要颜色 */
--color-primary: #6366f1          /* 品牌主色 */
--color-success: #10b981          /* 成功（绿色） */
--color-warning: #f59e0b          /* 警告（黄色） */
--color-error: #ef4444            /* 错误（红色） */

/* 背景色 */
--bg-primary: #ffffff             /* 主背景（浅色） */
--bg-secondary: #f8fafc           /* 卡片背景 */
--bg-tertiary: #f1f5f9            /* 悬停背景 */

/* 文字色 */
--text-primary: #1e293b           /* 主要文字 */
--text-secondary: #475569         /* 次要文字 */
--text-tertiary: #64748b          /* 提示文字 */

/* 边框色 */
--border-primary: #e2e8f0         /* 主要边框 */
```

### 间距/尺寸变量

```css
--spacing-1: 0.25rem    /* 4px */
--spacing-2: 0.5rem     /* 8px */
--spacing-4: 1rem       /* 16px */
--spacing-6: 1.5rem     /* 24px */

--radius-md: 8px        /* 中等圆角 */
--radius-lg: 12px       /* 大圆角 */
```

### 字体变量

```css
--font-family-sans: "Inter", sans-serif    /* 标准字体 */
--font-family-mono: "JetBrains Mono", monospace  /* 等宽字体 */

--font-size-sm: 0.875rem     /* 14px */
--font-size-base: 1rem       /* 16px */
--font-size-lg: 1.125rem     /* 18px */
```

---

## 实际例子

### 示例1: 按钮组件

```css
/* Button.module.css */
.button {
    background: var(--color-primary);
    color: white;
    padding: var(--spacing-2) var(--spacing-4);
    border-radius: var(--radius-md);
    border: none;
    cursor: pointer;
    font-weight: 600;
}

.button:hover {
    opacity: 0.9;
}

.button--secondary {
    background: var(--bg-secondary);
    color: var(--text-primary);
    border: 1px solid var(--border-primary);
}
```

### 示例2: 卡片组件

```css
/* Card.module.css */
.card {
    background: var(--bg-primary);
    border: 1px solid var(--border-primary);
    border-radius: var(--radius-lg);
    padding: var(--spacing-6);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.card__title {
    color: var(--text-primary);
    font-size: var(--font-size-lg);
    font-weight: 600;
    margin-bottom: var(--spacing-4);
}

.card__content {
    color: var(--text-secondary);
    font-size: var(--font-size-base);
}
```

### 示例3: 输入框组件

```css
/* Input.module.css */
.input {
    width: 100%;
    padding: var(--spacing-2) var(--spacing-3);
    border: 1px solid var(--border-primary);
    border-radius: var(--radius-md);
    background: var(--bg-primary);
    color: var(--text-primary);
    font-family: var(--font-family-sans);
    font-size: var(--font-size-base);
}

.input:focus {
    outline: none;
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.input:disabled {
    background: var(--bg-tertiary);
    color: var(--text-tertiary);
    cursor: not-allowed;
}
```

---

## 主题切换（深色/浅色）

### 工作原理

主应用会根据用户选择改变`data-theme`属性:

```html
<!-- 浅色主题 -->
<html data-theme="light">

<!-- 深色主题 -->
<html data-theme="dark">
```

当主题改变时，CSS变量的值会自动更新，你的组件会自动应用新的颜色！

### 在浏览器中测试

```javascript
// 切换到深色主题
document.documentElement.setAttribute('data-theme', 'dark');

// 切换回浅色主题
document.documentElement.setAttribute('data-theme', 'light');
```

---

## 故障排除

### 问题1: 变量不生效

```javascript
// 检查变量是否存在
const color = getComputedStyle(document.documentElement)
    .getPropertyValue('--color-primary');
console.log(color);  // 应该显示颜色值

// 如果是空字符串，检查:
// 1. CSS文件是否在HTML中加载?
// 2. 变量名称是否拼写正确?
// 3. 是否用在正确的选择器中?
```

### 问题2: 深色主题的颜色不对

```css
/* 确保在 [data-theme="dark"] 中定义了变量 */
[data-theme="dark"] {
    --color-primary: #818cf8;  /* 深色主题的浅紫 */
    --bg-primary: #1f2937;     /* 深色主题的暗色背景 */
}
```

### 问题3: 样式闪烁（FOUC）

```css
/* 添加过渡效果 */
body {
    transition: background-color 0.3s ease, color 0.3s ease;
}
```

---

## 快速参考表

### 颜色使用

| 用途 | 变量 | 使用场景 |
|------|------|---------|
| 主要按钮 | `--color-primary` | 按钮、链接 |
| 成功提示 | `--color-success` | 成功消息、绿色高亮 |
| 警告提示 | `--color-warning` | 警告信息、黄色高亮 |
| 错误提示 | `--color-error` | 错误信息、红色高亮 |
| 页面背景 | `--bg-primary` | 页面、卡片背景 |
| 主要文字 | `--text-primary` | 标题、主要内容 |
| 次要文字 | `--text-secondary` | 描述、辅助内容 |

### 间距使用

| 大小 | 变量 | 像素值 | 使用场景 |
|------|------|--------|---------|
| 小 | `--spacing-2` | 8px | 按钮内边距 |
| 中 | `--spacing-4` | 16px | 卡片内边距 |
| 大 | `--spacing-6` | 24px | 区间间距 |

---

## 完整检查清单

在提交代码前检查:

- [ ] CSS变量在浏览器控制台可用
- [ ] 所有硬编码颜色已替换为变量
- [ ] 浅色/深色主题都测试过
- [ ] 响应式设计适配正确
- [ ] 按钮/输入框等交互状态正确
- [ ] 无CSS拼写错误

---

## 相关文档

需要更多详细信息？查看:

1. **[研究文档](./CSS_VARIABLE_INHERITANCE_RESEARCH.md)** - 深入理论
2. **[实现指南](./CSS_VARIABLE_IMPLEMENTATION_GUIDE.md)** - 完整步骤
3. **[方案对比](./CSS_VARIABLE_ALTERNATIVES_ANALYSIS.md)** - 技术选型

---

## 一句话速记

> CSS变量是主应用和React应用之间的"翻译官"
> - 主应用定义变量(`--color-primary`)
> - React应用引用变量(`var(--color-primary)`)
> - 主题改变时自动更新，无需修改代码！

---

**版本**: v1.0
**维护**: Dev Debug Platform Team
**更新**: 2025-11-02

