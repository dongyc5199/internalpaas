# Phase 0: Technical Research - AI Chat Streaming Output

**Feature**: AI Chat Streaming Output (打字机效果流式输出)
**Date**: 2025-10-21
**Status**: Complete

## Research Overview

本功能旨在为 AI 聊天面板实现字符级流式渲染（打字机效果）。当前系统已具备后端 SSE 流式响应能力，但前端在接收到完整消息后一次性显示。本研究探讨前端实现字符级动画渲染的最佳方案。

## Research Questions & Decisions

### RQ-001: 字符流式渲染技术方案

**研究问题**: 如何在前端实现平滑的字符级流式渲染，同时保持 Markdown 格式和代码高亮？

**决策**: **两阶段渲染方案**

**方案对比**:

| 方案 | 优点 | 缺点 | 决策 |
|------|------|------|------|
| **逐字符渲染原始文本** | 实现简单，性能好 | 无法显示 Markdown 格式，用户体验差 | ❌ 不采用 |
| **逐块渲染 HTML** | 保持格式，相对平滑 | Markdown 解析性能开销大，块粒度粗糙 | ❌ 不采用 |
| **两阶段渲染** | 流畅的打字机效果 + 完整的格式渲染 | 实现复杂度中等 | ✅ **采用** |

**采用方案详细说明** - 两阶段渲染:

1. **阶段 1 - 流式文本渲染** (用户输入过程):
   - 使用 `<div class="streaming">` 容器显示纯文本
   - 从 SSE 接收到的 HTML 内容中使用 `requestAnimationFrame` 逐字符渲染
   - 字符速度: 30-60 chars/sec (可调节)
   - 在末尾显示闪烁光标指示器 `<span class="cursor">|</span>`

2. **阶段 2 - 完整格式渲染** (流式完成后):
   - 替换 `.streaming` 容器为完整的 HTML 渲染内容
   - 应用 Markdown 渲染、代码高亮、表格增强等现有功能
   - 保持与非流式模式完全一致的最终显示效果

**技术关键点**:
- 使用 `requestAnimationFrame` 而非 `setTimeout/setInterval` 确保渲染帧率稳定
- HTML 转义处理：流式阶段显示纯文本（去除 HTML 标签），完成后显示完整 HTML
- 平滑过渡：使用 CSS `transition` 实现从流式文本到格式化内容的淡入效果

**Rationale**:
- 满足 P1 需求（流畅打字机效果）和 FR-003（保持格式）
- 性能优秀：流式阶段无需 Markdown 解析，完成后仅解析一次
- 用户体验佳：既有动态感又有最终的格式完整性

### RQ-002: 打字机动画性能优化

**研究问题**: 如何确保 10,000 字符响应的流式渲染不会卡顿或阻塞 UI？

**决策**: **requestAnimationFrame + 批量字符渲染**

**方案对比**:

| 方案 | 优点 | 缺点 | 决策 |
|------|------|------|------|
| `setTimeout` 逐字符 | 实现简单 | 性能差，大量定时器开销，帧率不稳定 | ❌ 不采用 |
| `setInterval` 逐字符 | 稍好于 setTimeout | 仍然有性能问题，不同步于浏览器刷新率 | ❌ 不采用 |
| **requestAnimationFrame** | 与浏览器刷新率同步 (60 FPS)，性能最优 | 需要手动计算每帧字符数 | ✅ **采用** |
| Web Worker 后台渲染 | 不阻塞主线程 | 无法直接操作 DOM，实现复杂 | ❌ 不采用 |

**采用方案详细说明**:

```javascript
class TypewriterRenderer {
  constructor(targetElement, config = {}) {
    this.target = targetElement;
    this.charsPerSecond = config.speed || 50;  // 默认 50 字符/秒
    this.text = '';
    this.position = 0;
    this.isPaused = false;
    this.isComplete = false;
  }

  start(fullText) {
    this.text = fullText;
    this.position = 0;
    this.lastFrameTime = performance.now();
    this._renderFrame();
  }

  _renderFrame() {
    if (this.isPaused || this.isComplete) return;

    const now = performance.now();
    const deltaTime = (now - this.lastFrameTime) / 1000;  // 秒
    const charsToAdd = Math.floor(deltaTime * this.charsPerSecond);

    if (charsToAdd > 0) {
      this.position = Math.min(this.position + charsToAdd, this.text.length);
      this.target.textContent = this.text.substring(0, this.position);
      this.lastFrameTime = now;

      // 自动滚动
      this.target.scrollIntoView({ behavior: 'smooth', block: 'end' });
    }

    if (this.position < this.text.length) {
      requestAnimationFrame(() => this._renderFrame());
    } else {
      this.isComplete = true;
      this._onComplete();
    }
  }

  pause() { this.isPaused = true; }
  resume() { this.isPaused = false; this._renderFrame(); }
  skipToEnd() {
    this.position = this.text.length;
    this.target.textContent = this.text;
    this.isComplete = true;
    this._onComplete();
  }

  _onComplete() {
    // 触发完成回调，执行格式化渲染
    if (this.onComplete) this.onComplete();
  }
}
```

**性能指标验证**:
- 60 FPS 下，每帧约 16.67ms
- 50 chars/sec ≈ 每帧 0.83 字符 → 批量渲染每 ~1.2 帧添加 1 字符
- 10,000 字符 @ 50 chars/sec = 200 秒 → 实际可调整到 100-150 chars/sec 缩短到 60-100 秒
- DOM 更新频率: 每帧最多 1 次 `textContent` 更新，性能开销极小

**Rationale**:
- `requestAnimationFrame` 是浏览器推荐的动画 API，与屏幕刷新率同步
- 避免 `setTimeout` 的累积误差和性能抖动
- 满足 SC-002 (10,000 字符无卡顿) 和 SC-001 (<100ms 延迟) 要求

### RQ-003: 用户控制功能实现

**研究问题**: 如何实现用户中断流式输出、调整速度、全局开关等控制功能？

**决策**: **偏好设置 + 交互事件监听**

**功能实现**:

1. **立即显示完整内容** (P3 需求, FR-005):
   - 点击流式消息区域 → `typewriter.skipToEnd()`
   - 快捷键 ESC/Space → `typewriter.skipToEnd()`
   - 实现: 为 `.streaming` 容器添加 `click` 事件监听器

2. **流式速度调整** (FR-010):
   - 用户偏好设置: `streamingSpeed: 'slow' | 'normal' | 'fast'`
   - 映射到字符速度: slow=30, normal=50, fast=80 chars/sec
   - 存储: LocalStorage (前端) + UserPreferences 表 (后端同步)

3. **全局开关** (FR-010):
   - 用户偏好设置: `streamingEnabled: boolean`
   - `false` 时直接跳过流式渲染，立即显示完整格式化内容
   - 默认值: `true` (启用流式输出)

4. **并发流式管理** (FR-006):
   - 每个 AI 消息独立管理 `TypewriterRenderer` 实例
   - 新消息到达时，不打断之前的流式输出（各自独立）
   - 用户发送新问题时，当前流式输出自动 `skipToEnd()`

**偏好设置 UI**:
```html
<!-- 在 ai-panel.html 的设置抽屉中添加 -->
<div class="setting-group">
  <h4>流式输出设置</h4>
  <label>
    <input type="checkbox" id="streamingEnabled" checked>
    启用打字机效果
  </label>
  <label>
    速度:
    <select id="streamingSpeed">
      <option value="slow">慢速 (30 字符/秒)</option>
      <option value="normal" selected>正常 (50 字符/秒)</option>
      <option value="fast">快速 (80 字符/秒)</option>
    </select>
  </label>
</div>
```

**Rationale**:
- 满足 P3 需求（用户控制）和 FR-005/FR-010 要求
- LocalStorage 确保前端即时响应，后端同步确保跨设备一致性
- 并发管理策略简单清晰，避免复杂的队列逻辑

### RQ-004: Markdown 和代码高亮兼容性

**研究问题**: 流式渲染如何与现有的 Markdown 渲染和 Prism.js 代码高亮协同工作？

**决策**: **延迟格式化 + 内容隔离**

**现有实现分析**:
```javascript
// 当前代码 (ai-panel.js:274-282)
if (event === 'done') {
  aiEl.setAttribute('data-original-text', acc);
  body.innerHTML = acc;  // 直接设置 HTML
  try {
    postEnhanceTables(body);  // 表格增强
    if (window.Prism) Prism.highlightAllUnder(aiEl);  // 代码高亮
    attachInteractiveListeners(aiEl);  // 交互监听器
  } catch(e) {
    console.warn('Enhanced content error:', e);
  }
  addCodeActions(aiEl);  // 代码块操作按钮
  persistCurrentSession();
}
```

**集成方案**:

**阶段 1 - 流式渲染** (新增):
```javascript
// 创建流式容器
const streamingContainer = document.createElement('div');
streamingContainer.className = 'streaming';
body.appendChild(streamingContainer);

// 启动打字机渲染纯文本（去除 HTML 标签）
const plainText = stripHtmlTags(acc);
const typewriter = new TypewriterRenderer(streamingContainer, {
  speed: getUserPreferredSpeed(),
  onComplete: () => applyFormatting()
});
typewriter.start(plainText);
```

**阶段 2 - 格式化渲染** (复用现有逻辑):
```javascript
function applyFormatting() {
  // 移除流式容器
  body.innerHTML = '';

  // 应用完整格式化（复用现有代码）
  body.innerHTML = acc;  // acc 是原始 HTML
  postEnhanceTables(body);
  if (window.Prism) Prism.highlightAllUnder(aiEl);
  attachInteractiveListeners(aiEl);
  addCodeActions(aiEl);

  // 淡入动画
  body.classList.add('format-applied');
}
```

**HTML 标签去除工具**:
```javascript
function stripHtmlTags(html) {
  const temp = document.createElement('div');
  temp.innerHTML = html;
  return temp.textContent || temp.innerText || '';
}
```

**Rationale**:
- 完全兼容现有格式化流程，无需修改 `postEnhanceTables`, `Prism`, 等现有函数
- 流式阶段显示纯文本避免不完整 HTML 导致的渲染错误
- 格式化阶段复用 100% 现有代码，确保行为一致性
- 满足 FR-003 (保持格式) 和 SC-002 (无卡顿)

### RQ-005: 边界情况处理策略

**研究问题**: 如何处理并发请求、网络中断、特殊字符等边界情况？

**决策**: **防御性编程 + 优雅降级**

**边界情况处理**:

| 场景 | 处理策略 | 实现 |
|------|---------|------|
| **用户发送新消息时上一条还在流式** | 立即完成上一条流式输出 | 发送前调用 `currentTypewriter?.skipToEnd()` |
| **网络中断导致流式未完成** | 保留已显示内容，标记为不完整 | `catch` 块中显示 `[流式中断]` 提示 |
| **超长响应 (>10,000 字符)** | 自动提升速度或跳过流式 | `if (text.length > 10000) speed *= 2` |
| **用户滚动查看历史消息** | 不影响流式输出 | 流式输出不强制滚动到底部，仅提示有新消息 |
| **特殊字符 (emoji, Unicode)** | 正确处理多字节字符 | 使用 `Array.from(text)` 而非 `text.split('')` |
| **HTML 实体 (如 `&lt;`)** | 流式阶段显示纯文本 | `stripHtmlTags` 自动解码实体 |
| **代码块中的换行** | 流式阶段保留换行 | `textContent` 保留所有空白符 |
| **快速连续点击中断** | 防抖处理 | 点击后 200ms 内忽略后续点击 |

**错误处理代码**:
```javascript
try {
  // SSE 流式接收逻辑
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    // ... 流式处理
  }
} catch (err) {
  // 网络错误或流式中断
  typewriter?.skipToEnd();  // 显示已接收内容
  body.innerHTML += `<div class="error-hint">[流式输出中断: ${err.message}]</div>`;
} finally {
  persistCurrentSession();  // 始终保存会话
}
```

**Rationale**:
- 满足功能规格中列出的 6 个边界情况要求
- 优雅降级确保即使出错也能显示部分内容，不影响用户使用
- 防御性编程避免未处理异常导致整个面板崩溃

## Technology Stack Confirmation

### Frontend
- **Core**: Vanilla JavaScript ES6+ (现有)
- **Animation**: `requestAnimationFrame` API (浏览器原生)
- **Storage**: LocalStorage (会话和偏好) + `UserPreferences` 表 (后端同步)
- **HTML Rendering**: 现有 HTML 内容直接渲染 (无 Markdown 库依赖流式阶段)
- **Syntax Highlighting**: Prism.js (现有, 仅在格式化阶段应用)

### Backend
- **Framework**: Spring Boot 3.2.0 (现有)
- **API**: 复用现有 `POST /profile/preferences` 端点
- **Storage**: 复用现有 `User.preferences` JSON 字段

### Dependencies
- ✅ **无新增外部依赖**
- ✅ 所有功能使用浏览器原生 API 和现有库实现

## Best Practices & Patterns

### 1. 性能优化模式
- ✅ 使用 `requestAnimationFrame` 替代定时器
- ✅ 批量 DOM 更新（每帧最多 1 次 `textContent` 更新）
- ✅ 避免流式阶段的复杂 HTML 解析
- ✅ CSS `transform` 和 `opacity` 动画使用 GPU 加速

### 2. 用户体验模式
- ✅ 提供立即中断选项（点击/快捷键）
- ✅ 闪烁光标视觉反馈
- ✅ 平滑过渡动画（流式 → 格式化）
- ✅ 可配置速度和全局开关

### 3. 兼容性模式
- ✅ 向后兼容：通过偏好开关可禁用流式
- ✅ 渐进增强：不支持 `requestAnimationFrame` 的浏览器回退到立即显示
- ✅ 现有功能保持不变：Markdown, 代码高亮, 表格增强, 历史记录

### 4. 错误处理模式
- ✅ Try-catch 包裹所有流式渲染逻辑
- ✅ 优雅降级：出错时显示已接收内容
- ✅ 用户友好错误提示

## Integration Points

### 与现有代码集成

#### 1. `startAiStream` 函数修改
**位置**: `ai-panel.js:217-292`
**修改**: 在 `event === 'token'` 分支中积累文本，在 `event === 'done'` 分支中启动打字机渲染

#### 2. 用户偏好 API
**位置**: `UserController.java`
**修改**: 确保 `UserPreferencesDto` 包含 `streamingEnabled` 和 `streamingSpeed` 字段

#### 3. CSS 样式
**位置**: `ai-panel.css`
**新增**: 打字机光标动画、格式化淡入动画

#### 4. HTML 模板
**位置**: `ai-panel.html`
**新增**: 设置抽屉中的流式输出配置 UI

## Alternatives Considered & Rejected

### ❌ Server-Side Rendering (SSR) 流式
- **为什么拒绝**: 后端已提供 SSE 流式响应，前端直接处理更简单高效
- **缺点**: 增加服务器负担，无法利用客户端 GPU 加速动画

### ❌ WebSocket 替代 SSE
- **为什么拒绝**: 现有 SSE 架构工作良好，无需迁移
- **缺点**: 增加架构复杂度，SSE 单向通信已满足需求

### ❌ Canvas/WebGL 渲染文本
- **为什么拒绝**: 过度工程化，无法利用浏览器原生文本渲染和选择功能
- **缺点**: 无障碍访问性差，无法复制文本

### ❌ 逐单词流式（而非逐字符）
- **为什么拒绝**: 不符合"打字机效果"的用户期望
- **缺点**: 视觉效果不够连贯流畅

## Performance Benchmarks (Expected)

| 指标 | 目标 | 实现方案确认 |
|------|------|-------------|
| 首字符显示延迟 | <100ms | ✅ `requestAnimationFrame` 首帧即显示 |
| 10,000 字符流畅度 | 无卡顿 | ✅ 每帧单次 `textContent` 更新，GPU 加速滚动 |
| 中断响应时间 | <200ms | ✅ `skipToEnd()` 立即更新 DOM，单次操作 |
| 格式化渲染时间 | <500ms | ✅ 复用现有代码，已验证性能 |
| 并发会话支持 | 100+ | ✅ 独立 `TypewriterRenderer` 实例，无共享状态 |

## Security Considerations

- ✅ **XSS 防护**: 流式阶段使用 `textContent` (自动转义)，格式化阶段使用现有的受信任 HTML
- ✅ **CSRF 保护**: 复用现有 CSRF token 机制
- ✅ **输入验证**: 速度配置限制在预定义选项 (slow/normal/fast)
- ✅ **无敏感信息泄露**: 偏好设置不包含敏感数据

## Open Questions & Follow-up

1. **实际用户测试**: 50 chars/sec 是否是最佳默认速度？
   - **跟进**: 实现后进行 A/B 测试，收集用户反馈调整

2. **移动端性能**: 低端 Android 设备性能是否足够？
   - **跟进**: 实现后在目标设备上测试，必要时针对移动端降低默认速度

3. **长文本策略**: >50,000 字符的响应是否需要特殊处理？
   - **跟进**: 先实现基础功能，后续根据实际使用情况优化

## Conclusion

所有技术研究已完成，无未解决的 NEEDS CLARIFICATION 项。技术方案清晰可行：

- ✅ **核心方案**: 两阶段渲染 (流式纯文本 + 延迟格式化)
- ✅ **性能方案**: `requestAnimationFrame` + 批量渲染
- ✅ **兼容方案**: 完全复用现有格式化流程
- ✅ **控制方案**: 偏好设置 + 交互事件监听
- ✅ **边界处理**: 防御性编程 + 优雅降级

可以进入 Phase 1 设计阶段。
