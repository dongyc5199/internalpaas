# Research & Design Decisions: AI助手输入框UI优化

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)
**Date**: 2025-10-22 | **Phase**: 0 - 大纲与研究

---

## 1. 标签区域设计模式 (Context Chip Component Design)

### 研究对象
- **Material Design 3 Chips**: Input chips, filter chips, suggestion chips
- **Fluent UI 2 Tags**: Tag components with dismiss buttons
- **现有实现**: 截图中显示的蓝色标签样式

### 决策结果

#### ✅ 采用方案: Material Design Input Chips变体
- **背景色**: `#1e3a8a` (深蓝色) - 与截图一致
- **文字颜色**: `#ffffff` (白色) - 高对比度
- **圆角半径**: `6px` (中等圆角) - 平衡现代感与专业感
- **关闭按钮样式**:
  - 使用 `×` Unicode字符 (U+00D7)
  - 悬停时背景变为半透明白色 `rgba(255,255,255,0.2)`
  - 尺寸: 16x16px 可点击区域
- **间距**:
  - 标签内边距: `4px 8px` (紧凑型)
  - 标签间距: `6px` (横向间隔)
  - 标签栏下边距: `8px` (与输入框分隔)

#### 拒绝方案及理由
- ❌ **Fluent UI Tag样式**: 圆角过大(pill形状)，与截图不符
- ❌ **完全扁平设计**: 缺少深度感，不易识别为可交互元素
- ❌ **边框型标签**: 在深色主题中视觉层次不如填充型

### 最佳实践参考
- **Material Design 3 Guidelines**: Input chips应有明确的关闭按钮，支持键盘导航(Tab + Enter删除)
- **WCAG 2.1 可访问性**: 关闭按钮应有最小44x44px的触摸目标(移动端)或24x24px(桌面端)
- **用户体验**: 标签应支持横向滚动或自动换行，避免溢出

### CSS实现要点
```css
.context-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: #1e3a8a;
  color: #ffffff;
  border-radius: 6px;
  font-size: 13px;
  transition: background 0.2s ease;
}

.context-chip:hover {
  background: #1e40af; /* 稍亮的蓝色 */
}

.context-chip .close-btn {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 3px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.context-chip .close-btn:hover {
  background: rgba(255,255,255,0.2);
}
```

---

## 2. 工具栏图标化趋势 (Toolbar Iconography)

### 研究对象
- **VS Code**: 底部状态栏图标按钮
- **GitHub**: 评论框工具栏图标
- **Linear**: 问题编辑器工具栏
- **ChatGPT/Claude Web**: AI聊天界面图标按钮

### 决策结果

#### ✅ 采用方案: Unicode图标 + Hover提示
- **图标来源**: Unicode字符 (无需外部依赖)
  - 设置按钮: `⚙` (U+2699 GEAR)
  - 发送按钮: `▶` (U+25B6 BLACK RIGHT-POINTING TRIANGLE)
  - 更多选项: `⋮` (U+22EE VERTICAL ELLIPSIS)
  - 下拉箭头: `▼` (U+25BC BLACK DOWN-POINTING TRIANGLE)
- **图标尺寸**:
  - 主按钮(发送): 20px
  - 次要按钮(设置/更多): 18px
  - 下拉箭头: 12px
- **间距**:
  - 按钮之间: `8px`
  - 按钮内边距: `8px` (创建44x44px最小触摸目标)
- **悬停效果**:
  - 背景色变化: 透明 → `rgba(255,255,255,0.1)`
  - 圆角: `6px`
  - 过渡时间: `150ms ease`

#### 拒绝方案及理由
- ❌ **Font Awesome/Lucide图标库**: 增加外部依赖，违反约束条件
- ❌ **内联SVG**: 代码冗长，不易维护
- ❌ **纯文字按钮**: 占用空间大，不符合截图设计

### 最佳实践参考
- **VS Code设计系统**: 使用Codicon字体图标，悬停时有微妙的背景高亮
- **GitHub工具栏**: 图标按钮带有Tooltip提示，避免用户猜测功能
- **Linear UX**: 图标按钮尺寸统一，间距规律，视觉节奏感强

### 实现示例
```css
.toolbar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s ease;
  color: #94a3b8; /* 灰色默认 */
}

.toolbar-btn:hover {
  background: rgba(255,255,255,0.1);
  color: #ffffff;
}

.toolbar-btn.primary {
  color: #3b82f6; /* 蓝色强调 */
}

.toolbar-btn.primary:hover {
  background: rgba(59,130,246,0.1);
}
```

---

## 3. 下拉选择器UI模式 (Dropdown Selector Design)

### 研究对象
- **原生`<select>`元素**: 浏览器默认样式
- **自定义下拉组件**: Headless UI, Radix UI Dropdown
- **截图中的实现**: 带下拉箭头的模式/模型选择器

### 决策结果

#### ✅ 采用方案: 原生`<select>` + CSS自定义样式
- **理由**:
  - 无需JavaScript逻辑，保持简单
  - 原生可访问性支持(键盘导航、屏幕阅读器)
  - 移动端自动使用系统原生选择器
- **自定义样式要点**:
  - 隐藏原生下拉箭头: `appearance: none;`
  - 添加自定义箭头: `::after`伪元素 + Unicode ▼
  - 边框: `1px solid rgba(255,255,255,0.2)`
  - 内边距: `6px 28px 6px 10px` (右侧留空间放箭头)
  - 背景色: `rgba(255,255,255,0.05)` (半透明)
  - 悬停效果: 边框变亮 `rgba(255,255,255,0.3)`

#### 拒绝方案及理由
- ❌ **完全自定义下拉组件**: 需要大量JavaScript，违反"不引入新复杂性"约束
- ❌ **保留原生样式**: 在不同浏览器/操作系统下样式不一致，不专业

### 最佳实践参考
- **Tailwind UI Select模式**: 使用`appearance: none` + 自定义箭头图标
- **可访问性**: 确保`<label>`与`<select>`关联，支持键盘操作
- **响应式**: 移动端允许原生选择器覆盖(更好的触摸体验)

### CSS实现
```css
.mode-selector, .model-selector {
  position: relative;
  display: inline-block;
}

.mode-selector select, .model-selector select {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  padding: 6px 28px 6px 10px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 6px;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.mode-selector select:hover {
  border-color: rgba(255,255,255,0.3);
  background: rgba(255,255,255,0.08);
}

.mode-selector::after {
  content: '▼';
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 10px;
  color: #94a3b8;
  pointer-events: none;
}
```

---

## 4. CSS变量系统 (Theme Variable System)

### 研究对象
- **现有`ai-assistant.css`**: 检查是否已有颜色/间距系统
- **CSS Custom Properties标准**: 现代浏览器支持度
- **设计系统参考**: Tailwind色板, Material Design颜色系统

### 决策结果

#### ✅ 采用方案: 引入CSS变量统一管理主题色
- **理由**:
  - 提升维护性: 颜色修改只需改一处
  - 支持未来扩展: 易于添加明亮模式切换
  - 现代浏览器100%支持: IE11已退出历史舞台
- **变量定义位置**: `.ai-assistant-panel`根元素或`:root`
- **核心变量清单**:
```css
:root {
  /* 背景色 */
  --ai-bg-primary: #1e293b;      /* 主背景 */
  --ai-bg-secondary: #334155;    /* 次级背景 */
  --ai-bg-tertiary: #475569;     /* 三级背景(悬停) */

  /* 文字颜色 */
  --ai-text-primary: #ffffff;    /* 主文字 */
  --ai-text-secondary: #94a3b8;  /* 次要文字 */
  --ai-text-muted: #64748b;      /* 弱化文字 */

  /* 强调色 */
  --ai-accent-blue: #3b82f6;     /* 蓝色(主操作) */
  --ai-accent-blue-dark: #1e3a8a; /* 深蓝色(标签背景) */

  /* 边框 */
  --ai-border-default: rgba(255,255,255,0.2);
  --ai-border-hover: rgba(255,255,255,0.3);

  /* 间距 */
  --ai-spacing-xs: 4px;
  --ai-spacing-sm: 6px;
  --ai-spacing-md: 8px;
  --ai-spacing-lg: 12px;

  /* 圆角 */
  --ai-radius-sm: 4px;
  --ai-radius-md: 6px;
  --ai-radius-lg: 8px;

  /* 过渡 */
  --ai-transition-fast: 150ms ease;
  --ai-transition-normal: 200ms ease;
}
```

#### 拒绝方案及理由
- ❌ **硬编码颜色值**: 维护成本高，不易扩展
- ❌ **使用Sass/Less变量**: 需要构建步骤，违反"纯前端"约束

### 最佳实践参考
- **Material Design 3 Token系统**: 分层命名(primary/secondary/tertiary)
- **Tailwind设计系统**: 语义化变量名(text-primary vs text-muted)
- **GitHub Primer**: 使用CSS变量实现明暗模式切换

---

## 5. 响应式设计 (Responsive Design for Input Area)

### 研究对象
- **移动端输入框最佳实践**: iOS Safari, Android Chrome
- **断点选择**: 常见设备宽度分布
- **截图分析**: 桌面端布局特征

### 决策结果

#### ✅ 采用方案: 桌面优先 + 移动端适配
- **断点定义**:
  - 桌面端 (默认): `> 768px`
  - 平板端: `481px - 768px`
  - 移动端: `≤ 480px`
- **响应式策略**:

**桌面端 (默认)**:
- 标签区域横向排列，支持横向滚动
- 工具栏所有按钮可见
- 输入框最大高度: `150px`

**平板端 (768px以下)**:
- 标签区域保持横向排列
- 工具栏按钮图标尺寸不变
- 输入框最大高度: `120px`

**移动端 (480px以下)**:
- 标签区域自动换行 (flex-wrap: wrap)
- 隐藏"更多选项"按钮 (⋮)，功能合并到设置中
- 输入框最大高度: `100px`
- 发送按钮固定在输入框右下角(绝对定位)

#### CSS实现
```css
/* 桌面端默认 */
.context-chips-container {
  display: flex;
  gap: var(--ai-spacing-sm);
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: var(--ai-spacing-md);
}

.ai-input-area {
  max-height: 150px;
  overflow-y: auto;
}

/* 平板端 */
@media (max-width: 768px) {
  .ai-input-area {
    max-height: 120px;
  }
}

/* 移动端 */
@media (max-width: 480px) {
  .context-chips-container {
    flex-wrap: wrap; /* 允许换行 */
  }

  .ai-input-area {
    max-height: 100px;
    padding-right: 50px; /* 为发送按钮留空间 */
  }

  .toolbar-btn.more-options {
    display: none; /* 隐藏更多选项按钮 */
  }

  .send-btn {
    position: absolute;
    right: 8px;
    bottom: 8px;
  }
}
```

#### 拒绝方案及理由
- ❌ **移动优先设计**: 桌面端是主要使用场景(开发工具)
- ❌ **固定断点库(Bootstrap)**: 引入外部依赖，违反约束
- ❌ **完全禁用移动端**: 部分场景用户可能在移动设备上查看

### 最佳实践参考
- **iOS Safari输入框**: 聚焦时自动放大，需设置`font-size: 16px`避免缩放
- **Android Chrome**: 支持`:focus-visible`伪类，优化键盘导航体验
- **触摸目标尺寸**: 移动端按钮最小44x44px (Apple HIG标准)

---

## 总结与下一步

### 关键设计决策汇总
1. ✅ **标签样式**: Material Design Input Chips + 深蓝色背景
2. ✅ **图标来源**: Unicode字符 (⚙/▶/⋮/▼)
3. ✅ **下拉选择器**: 原生`<select>` + CSS自定义
4. ✅ **主题系统**: CSS变量统一管理颜色/间距
5. ✅ **响应式**: 桌面优先 + 三级断点适配

### 技术栈确认
- **纯CSS**: 无需Sass/Less/PostCSS预处理
- **无外部依赖**: 无需Font Awesome/Lucide图标库
- **浏览器兼容**: Chrome/Firefox/Edge/Safari最新版本

### Phase 1 准备就绪
所有设计决策已完成，可以开始编写 `quickstart.md` 实施指南。

---

**研究状态**: ✅ 完成
**下一步**: Phase 1 - 生成 `quickstart.md`
