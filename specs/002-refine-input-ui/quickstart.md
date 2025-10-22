# Quick Start Guide: AI助手输入框UI优化

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Research**: [research.md](./research.md)
**Branch**: `002-refine-input-ui` | **Date**: 2025-10-22

---

## 目标概述

优化AI助手输入框的用户界面，通过添加独立的上下文标签区域、增强占位符提示、图标化工具栏按钮、优化下拉选择器，提升用户体验和视觉专业度。

**预计时间**: 2-3小时开发 + 1小时测试

---

## 开发环境设置

### 1. 克隆并切换到功能分支

```bash
# 确保在dev分支上
git checkout dev
git pull origin dev

# 切换到功能分支
git checkout 002-refine-input-ui

# 查看分支状态
git status
```

### 2. 启动应用

```bash
# 方法1: 使用Maven Wrapper (推荐)
./mvnw.cmd spring-boot:run

# 方法2: 如果已有运行实例，无需重启
# (因为我们只修改前端CSS/JS，浏览器刷新即可)
```

### 3. 打开开发页面

- **URL**: `http://localhost:8080/terminal/manager`
- **浏览器**: Chrome/Edge (推荐，开发者工具更强大)
- **开发者工具**: F12 → 选择"Elements"和"Console"标签

### 4. 定位目标文件

需要修改的文件:
```
E:\work\code\internalpaas\src\main\resources\
├── static\
│   ├── css\
│   │   └── ai-assistant.css       ← 主要修改
│   └── js\
│       └── ai-assistant.js         ← 主要修改
└── templates\
    └── terminal\
        └── manager.html            ← 无需修改(仅引用)
```

---

## 实施步骤概览

### 阶段1: CSS变量系统 (15分钟)
**目标**: 在`ai-assistant.css`顶部添加CSS变量，统一管理颜色/间距

**步骤**:
1. 打开`ai-assistant.css`
2. 在文件顶部(第1行)添加`:root`变量块(参考research.md第4节)
3. 保存文件
4. 刷新浏览器验证无报错

**验证**:
- 打开开发者工具 → Console → 无CSS错误
- 在Elements标签中检查`:root`样式已加载

---

### 阶段2: HTML结构重构 (30分钟)
**目标**: 在`ai-assistant.js`中修改`createInputArea()`函数，分离标签栏和工具栏

#### 2.1 定位修改位置
在`ai-assistant.js`中找到`createInputArea()`函数(约第XXX行)

#### 2.2 修改HTML结构
**原结构** (简化):
```html
<div class="ai-input-container">
  <textarea placeholder="请输入问题..."></textarea>
  <button class="send-btn">发送</button>
</div>
```

**新结构**:
```html
<div class="ai-input-container">
  <!-- 新增: 上下文标签区域 -->
  <div class="context-chips-container" id="contextChipsContainer">
    <!-- 动态插入标签 -->
  </div>

  <!-- 输入区域 -->
  <div class="input-wrapper">
    <textarea
      id="aiInputArea"
      placeholder="添加上下文(#)、扩展(@)、命令(/)"
      maxlength="500"
    ></textarea>
  </div>

  <!-- 底部工具栏 -->
  <div class="ai-toolbar">
    <div class="toolbar-left">
      <!-- 模式选择器 -->
      <div class="mode-selector">
        <select id="modeSelect">
          <option value="ask">Ask</option>
          <option value="agent" selected>Agent</option>
        </select>
      </div>

      <!-- 模型选择器 -->
      <div class="model-selector">
        <select id="modelSelect">
          <option value="kimi-k2" selected>Kimi K2</option>
        </select>
      </div>
    </div>

    <div class="toolbar-right">
      <!-- 设置按钮 -->
      <button class="toolbar-btn settings-btn" title="设置">⚙</button>

      <!-- 发送按钮 -->
      <button class="toolbar-btn primary send-btn" id="sendBtn" title="发送 (Ctrl+Enter)">▶</button>

      <!-- 更多选项按钮 -->
      <button class="toolbar-btn more-options" title="更多选项">⋮</button>
    </div>
  </div>
</div>
```

#### 2.3 更新JavaScript事件绑定
在`createInputArea()`函数末尾，添加新的事件监听器:

```javascript
// 模式选择器
document.getElementById('modeSelect').addEventListener('change', function(e) {
  console.log('Mode changed to:', e.target.value);
  // TODO: 实现模式切换逻辑
});

// 模型选择器
document.getElementById('modelSelect').addEventListener('change', function(e) {
  console.log('Model changed to:', e.target.value);
  // TODO: 实现模型切换逻辑
});

// 设置按钮
document.querySelector('.settings-btn').addEventListener('click', function() {
  console.log('Settings clicked');
  // TODO: 打开设置面板
});

// 更多选项按钮
document.querySelector('.more-options').addEventListener('click', function() {
  console.log('More options clicked');
  // TODO: 显示更多菜单
});
```

---

### 阶段3: 上下文标签样式 (20分钟)
**目标**: 在`ai-assistant.css`中添加标签相关样式

**步骤**:
1. 在`ai-assistant.css`末尾添加以下样式块:

```css
/* ============================================
   上下文标签区域
   ============================================ */
.context-chips-container {
  display: flex;
  gap: var(--ai-spacing-sm);
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: var(--ai-spacing-md);
  min-height: 0; /* 无标签时不占空间 */
}

.context-chips-container:empty {
  display: none; /* 无标签时隐藏 */
}

/* 自定义滚动条 */
.context-chips-container::-webkit-scrollbar {
  height: 6px;
}

.context-chips-container::-webkit-scrollbar-thumb {
  background: var(--ai-bg-tertiary);
  border-radius: 3px;
}

/* 上下文标签 */
.context-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--ai-spacing-sm);
  padding: 4px 8px;
  background: var(--ai-accent-blue-dark);
  color: var(--ai-text-primary);
  border-radius: var(--ai-radius-md);
  font-size: 13px;
  white-space: nowrap;
  flex-shrink: 0; /* 防止标签被压缩 */
  transition: background var(--ai-transition-fast);
}

.context-chip:hover {
  background: #1e40af; /* 稍亮的蓝色 */
}

.context-chip .chip-icon {
  font-size: 14px;
  opacity: 0.9;
}

.context-chip .chip-label {
  font-weight: 500;
}

.context-chip .close-btn {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 3px;
  cursor: pointer;
  font-size: 14px;
  transition: background var(--ai-transition-fast);
  margin-left: 2px;
}

.context-chip .close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}
```

2. 保存并刷新浏览器

**验证**:
- 在浏览器Console中手动添加测试标签:
```javascript
const container = document.getElementById('contextChipsContainer');
container.innerHTML = `
  <div class="context-chip">
    <span class="chip-icon">🖥️</span>
    <span class="chip-label">服务器-192.168.1.100</span>
    <span class="close-btn">×</span>
  </div>
`;
```
- 检查标签样式是否符合截图

---

### 阶段4: 输入框增强占位符 (10分钟)
**目标**: 更新占位符文本并添加对应样式

**步骤**:
1. 在`ai-assistant.js`的`createInputArea()`中，确认`<textarea>`的`placeholder`属性:
```html
<textarea placeholder="添加上下文(#)、扩展(@)、命令(/)"></textarea>
```

2. 在`ai-assistant.css`中添加占位符样式:
```css
.input-wrapper textarea::placeholder {
  color: var(--ai-text-secondary);
  font-size: 14px;
  opacity: 0.7;
}

.input-wrapper textarea:focus::placeholder {
  opacity: 0.5; /* 聚焦时淡化占位符 */
}
```

**验证**:
- 点击输入框，占位符应显示"添加上下文(#)、扩展(@)、命令(/)"
- 聚焦时占位符应变淡

---

### 阶段5: 工具栏图标按钮样式 (25分钟)
**目标**: 添加工具栏布局和按钮样式

**步骤**:
1. 在`ai-assistant.css`中添加工具栏样式:

```css
/* ============================================
   底部工具栏
   ============================================ */
.ai-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--ai-spacing-md) 0;
  border-top: 1px solid var(--ai-border-default);
  margin-top: var(--ai-spacing-md);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--ai-spacing-md);
}

/* 工具栏按钮 */
.toolbar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: var(--ai-radius-md);
  cursor: pointer;
  color: var(--ai-text-secondary);
  font-size: 18px;
  transition: all var(--ai-transition-fast);
}

.toolbar-btn:hover {
  background: var(--ai-bg-tertiary);
  color: var(--ai-text-primary);
}

.toolbar-btn.primary {
  color: var(--ai-accent-blue);
}

.toolbar-btn.primary:hover {
  background: rgba(59, 130, 246, 0.1);
  color: var(--ai-accent-blue);
}

.toolbar-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 发送按钮特殊样式 */
.send-btn {
  font-size: 20px;
}
```

**验证**:
- 悬停工具栏按钮应显示背景高亮
- 发送按钮(▶)应显示蓝色

---

### 阶段6: 下拉选择器样式 (20分钟)
**目标**: 自定义模式/模型选择器样式

**步骤**:
1. 在`ai-assistant.css`中添加选择器样式:

```css
/* ============================================
   下拉选择器
   ============================================ */
.mode-selector,
.model-selector {
  position: relative;
  display: inline-block;
}

.mode-selector select,
.model-selector select {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  padding: 6px 28px 6px 10px;
  background: var(--ai-bg-secondary);
  border: 1px solid var(--ai-border-default);
  border-radius: var(--ai-radius-md);
  color: var(--ai-text-primary);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--ai-transition-fast);
  min-width: 100px;
}

.mode-selector select:hover,
.model-selector select:hover {
  border-color: var(--ai-border-hover);
  background: var(--ai-bg-tertiary);
}

.mode-selector select:focus,
.model-selector select:focus {
  outline: none;
  border-color: var(--ai-accent-blue);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

/* 自定义下拉箭头 */
.mode-selector::after,
.model-selector::after {
  content: '▼';
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 10px;
  color: var(--ai-text-secondary);
  pointer-events: none;
}
```

**验证**:
- 选择器应显示自定义的下拉箭头(▼)
- 悬停时应有边框高亮效果

---

### 阶段7: 输入框自动高度调整 (15分钟)
**目标**: 实现textarea自动扩展高度

**步骤**:
1. 在`ai-assistant.js`中添加自动高度调整函数:

```javascript
// 自动调整输入框高度
function autoResizeTextarea(textarea) {
  textarea.style.height = 'auto';
  textarea.style.height = Math.min(textarea.scrollHeight, 150) + 'px';
}

// 在createInputArea()中绑定事件
const textarea = document.getElementById('aiInputArea');
textarea.addEventListener('input', function() {
  autoResizeTextarea(this);
});

// 初始化高度
autoResizeTextarea(textarea);
```

2. 在`ai-assistant.css`中添加输入框基础样式:

```css
.input-wrapper {
  position: relative;
}

.input-wrapper textarea {
  width: 100%;
  min-height: 60px;
  max-height: 150px;
  padding: 10px 12px;
  background: var(--ai-bg-secondary);
  border: 1px solid var(--ai-border-default);
  border-radius: var(--ai-radius-md);
  color: var(--ai-text-primary);
  font-size: 14px;
  font-family: inherit;
  resize: none; /* 禁用手动拖拽调整 */
  overflow-y: auto;
  transition: border-color var(--ai-transition-fast);
}

.input-wrapper textarea:focus {
  outline: none;
  border-color: var(--ai-accent-blue);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}
```

**验证**:
- 输入多行文本时，输入框应自动扩展
- 达到150px最大高度后应显示滚动条

---

### 阶段8: 响应式设计调整 (20分钟)
**目标**: 添加移动端适配样式

**步骤**:
在`ai-assistant.css`末尾添加媒体查询:

```css
/* ============================================
   响应式设计
   ============================================ */

/* 平板端 (768px以下) */
@media (max-width: 768px) {
  .input-wrapper textarea {
    max-height: 120px;
  }

  .toolbar-btn {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }
}

/* 移动端 (480px以下) */
@media (max-width: 480px) {
  .context-chips-container {
    flex-wrap: wrap; /* 允许标签换行 */
  }

  .input-wrapper textarea {
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

  .ai-toolbar {
    flex-wrap: wrap;
  }

  .toolbar-left,
  .toolbar-right {
    gap: var(--ai-spacing-sm);
  }
}
```

**验证**:
- 打开Chrome DevTools → Toggle Device Toolbar (Ctrl+Shift+M)
- 测试不同屏幕尺寸(iPhone SE, iPad, Desktop)
- 确认标签在移动端换行显示

---

## 测试清单

### 功能测试

- [ ] **上下文标签**:
  - [ ] 输入`#`可以触发上下文选择菜单
  - [ ] 选择SSH会话后显示标签
  - [ ] 点击标签的关闭按钮(×)可以移除标签
  - [ ] 添加多个标签时可以横向滚动

- [ ] **占位符提示**:
  - [ ] 空输入框显示"添加上下文(#)、扩展(@)、命令(/)"
  - [ ] 输入文本后占位符消失
  - [ ] 清空文本后占位符重新显示

- [ ] **工具栏按钮**:
  - [ ] 点击设置按钮(⚙)触发事件(Console有日志)
  - [ ] 点击更多选项按钮(⋮)触发事件
  - [ ] 点击发送按钮(▶)可以发送消息
  - [ ] 发送按钮在输入为空时应禁用

- [ ] **下拉选择器**:
  - [ ] 点击模式选择器显示下拉列表
  - [ ] 选择"Ask"或"Agent"模式生效
  - [ ] 点击模型选择器显示"Kimi K2"选项
  - [ ] 选择器显示自定义下拉箭头(▼)

- [ ] **输入框自动高度**:
  - [ ] 输入单行文本时高度为60px
  - [ ] 输入多行文本时自动扩展
  - [ ] 达到150px最大高度后显示滚动条

### 视觉测试

- [ ] **颜色与间距**:
  - [ ] 标签背景色为深蓝色 (#1e3a8a)
  - [ ] 按钮悬停时有背景高亮效果
  - [ ] 选择器边框在悬停时变亮
  - [ ] 所有元素间距统一(6px/8px)

- [ ] **图标显示**:
  - [ ] 设置按钮显示⚙图标
  - [ ] 发送按钮显示▶图标
  - [ ] 更多选项按钮显示⋮图标
  - [ ] 下拉选择器显示▼箭头

- [ ] **响应式布局**:
  - [ ] 桌面端(>768px): 所有按钮可见
  - [ ] 平板端(481-768px): 布局紧凑但功能完整
  - [ ] 移动端(≤480px): 标签换行，更多选项按钮隐藏

### 浏览器兼容性

- [ ] **Chrome** (最新版): 所有功能正常
- [ ] **Edge** (最新版): 所有功能正常
- [ ] **Firefox** (最新版): 所有功能正常
- [ ] **Safari** (最新版): 所有功能正常

### 性能测试

- [ ] **响应速度**:
  - [ ] 点击按钮后立即显示视觉反馈(<50ms)
  - [ ] 输入500字符无卡顿
  - [ ] CSS动画保持流畅(60 FPS)

- [ ] **资源加载**:
  - [ ] 无额外HTTP请求(无外部图标库)
  - [ ] CSS文件大小增加<10KB

---

## 故障排除

### 问题1: CSS变量不生效
**症状**: 样式显示为默认值，颜色不符合预期
**解决方案**:
1. 检查`:root`块是否在`ai-assistant.css`的最顶部
2. 清除浏览器缓存 (Ctrl+Shift+Delete)
3. 硬刷新页面 (Ctrl+F5)

### 问题2: Unicode图标显示为方块
**症状**: ⚙/▶/⋮图标显示为□
**解决方案**:
1. 确认浏览器字体支持Unicode字符
2. 在CSS中添加fallback字体:
```css
.toolbar-btn {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}
```

### 问题3: 下拉箭头不显示
**症状**: 选择器旁边没有▼箭头
**解决方案**:
1. 检查`.mode-selector::after`的`pointer-events: none;`是否存在
2. 确认父元素`.mode-selector`的`position: relative;`已设置

### 问题4: 输入框高度不自动调整
**症状**: 输入多行文本时高度不变
**解决方案**:
1. 确认`autoResizeTextarea()`函数已定义
2. 检查事件监听器是否正确绑定:
```javascript
textarea.addEventListener('input', function() {
  autoResizeTextarea(this);
});
```
3. 在Console中手动执行测试:
```javascript
autoResizeTextarea(document.getElementById('aiInputArea'));
```

### 问题5: 移动端样式未生效
**症状**: 在移动设备/模拟器上样式与桌面端相同
**解决方案**:
1. 确认HTML的`<meta>`标签包含viewport设置:
```html
<meta name="viewport" content="width=device-width, initial-scale=1.0">
```
2. 检查媒体查询语法是否正确(`@media (max-width: 480px)`)

---

## 回滚计划

如果发现严重问题需要回滚:

### 方案1: Git回退
```bash
# 查看当前修改
git status

# 撤销所有未提交的修改
git checkout -- src/main/resources/static/css/ai-assistant.css
git checkout -- src/main/resources/static/js/ai-assistant.js

# 或回退到合并前的commit
git log --oneline -5
git reset --hard <commit-hash>
```

### 方案2: 备份文件恢复
```bash
# 如果事先创建了备份
cp src/main/resources/static/css/ai-assistant.css.backup src/main/resources/static/css/ai-assistant.css
cp src/main/resources/static/js/ai-assistant.js.backup src/main/resources/static/js/ai-assistant.js
```

**回滚影响**:
- ✅ 无数据库迁移需要回滚
- ✅ 无后端API需要回滚
- ✅ 仅需恢复2个前端文件
- ✅ 刷新浏览器即可恢复原样式

---

## 提交与合并

### 提交代码

```bash
# 查看修改
git status
git diff src/main/resources/static/css/ai-assistant.css
git diff src/main/resources/static/js/ai-assistant.js

# 暂存修改
git add src/main/resources/static/css/ai-assistant.css
git add src/main/resources/static/js/ai-assistant.js

# 提交
git commit -m "feat(ui): 优化AI助手输入框界面

- 新增独立的上下文标签区域,支持显示和移除SSH会话上下文
- 增强输入框占位符,提示#/@//三种功能触发器
- 图标化工具栏按钮(⚙️设置、▶️发送、⋮更多选项)
- 优化下拉选择器样式,添加自定义▼箭头
- 实现输入框自动高度调整(最大150px)
- 添加CSS变量系统统一管理主题色
- 新增响应式设计支持移动端/平板端

Ref: specs/002-refine-input-ui/spec.md"
```

### 合并到dev分支

```bash
# 切换到dev分支
git checkout dev

# 合并功能分支
git merge 002-refine-input-ui

# 推送到远程
git push origin dev
```

---

## 后续优化建议

完成本次UI优化后,可以考虑以下增强:

1. **键盘快捷键支持**:
   - `Ctrl+Enter` 发送消息
   - `Ctrl+K` 打开设置
   - `Esc` 关闭设置面板

2. **动画效果增强**:
   - 标签添加/移除时的淡入淡出动画
   - 工具栏按钮点击时的波纹效果(Ripple)

3. **无障碍访问**:
   - 添加ARIA标签 (`aria-label`, `role="button"`)
   - 支持Tab键导航
   - 屏幕阅读器优化

4. **暗黑/明亮模式切换**:
   - 利用CSS变量轻松实现主题切换
   - 添加主题切换按钮

5. **@扩展和/命令功能实现**:
   - 当前占位符已提示,待后续开发

---

**快速开始指南状态**: ✅ 完成
**下一步**: 执行实施步骤,开始代码修改
