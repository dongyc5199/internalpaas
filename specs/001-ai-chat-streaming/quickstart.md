# Quickstart Guide: AI Chat Streaming Output

**Feature**: AI Chat Streaming Output (AI 聊天打字机效果)
**Target Audience**: 开发人员
**Estimated Time**: 30-45 minutes

## 概览

本功能为 SSH 终端的 AI 聊天面板增加打字机效果，AI 回复将逐字符流式显示而非一次性显示。本指南将引导你完成功能的实现和测试。

## 前置条件

### 环境要求
- ✅ Java 17+
- ✅ Maven 3.6+
- ✅ 现代浏览器 (Chrome/Firefox/Safari/Edge 最新版)
- ✅ 项目已正常运行 (`mvn spring-boot:run`)

### 必需知识
- 基础 JavaScript ES6+ (Promise, async/await, Class)
- 熟悉 Spring Boot 和 JPA
- 了解浏览器 `requestAnimationFrame` API

### 代码库检查

确认以下文件存在：
```bash
# 后端
src/main/java/com/cmict/internalpaas/dto/UserPreferencesDto.java
src/main/java/com/cmict/internalpaas/controller/UserController.java

# 前端
src/main/resources/static/js/ai-panel.js
src/main/resources/static/css/ai-panel.css
src/main/resources/templates/terminal/ai-panel.html
```

## 实现步骤

### 步骤 1: 扩展后端数据模型 (5 分钟)

#### 1.1 修改 UserPreferencesDto.java

**文件**: `src/main/java/com/cmict/internalpaas/dto/UserPreferencesDto.java`

在类中添加新字段（建议在终端设置区域之后）:

```java
// AI 聊天流式输出设置
private Boolean aiStreamingEnabled = true;  // 默认启用
private String aiStreamingSpeed = "normal";  // 默认正常速度

// Getters and Setters
public Boolean getAiStreamingEnabled() {
    return aiStreamingEnabled;
}

public void setAiStreamingEnabled(Boolean aiStreamingEnabled) {
    this.aiStreamingEnabled = aiStreamingEnabled;
}

public String getAiStreamingSpeed() {
    return aiStreamingSpeed;
}

public void setAiStreamingSpeed(String aiStreamingSpeed) {
    // 验证速度值
    if ("slow".equals(aiStreamingSpeed) ||
        "normal".equals(aiStreamingSpeed) ||
        "fast".equals(aiStreamingSpeed)) {
        this.aiStreamingSpeed = aiStreamingSpeed;
    } else {
        // 无效值回退到默认值
        this.aiStreamingSpeed = "normal";
    }
}
```

#### 1.2 验证后端修改

```bash
# 编译项目
mvn clean compile

# 确认无编译错误
```

### 步骤 2: 实现前端打字机渲染器 (15 分钟)

#### 2.1 创建 TypewriterRenderer 类

**文件**: `src/main/resources/static/js/ai-panel.js`

在文件开头（全局作用域或 IIFE 内部）添加以下类：

```javascript
/**
 * 打字机渲染器 - 实现字符级流式动画
 */
class TypewriterRenderer {
  constructor(targetElement, config = {}) {
    this.target = targetElement;
    this.charsPerSecond = config.speed || 50;  // 默认 50 字符/秒
    this.onComplete = config.onComplete || null;
    this.text = '';
    this.position = 0;
    this.isPaused = false;
    this.isComplete = false;
    this.lastFrameTime = 0;
  }

  start(fullText) {
    this.text = fullText;
    this.position = 0;
    this.isComplete = false;
    this.lastFrameTime = performance.now();
    this._renderFrame();
  }

  _renderFrame() {
    if (this.isPaused || this.isComplete) return;

    const now = performance.now();
    const deltaTime = (now - this.lastFrameTime) / 1000;  // 转换为秒
    const charsToAdd = Math.floor(deltaTime * this.charsPerSecond);

    if (charsToAdd > 0) {
      this.position = Math.min(this.position + charsToAdd, this.text.length);
      this.target.textContent = this.text.substring(0, this.position);
      this.lastFrameTime = now;

      // 自动滚动
      if (this.target.closest('.messages')) {
        this.target.closest('.messages').scrollTop = this.target.closest('.messages').scrollHeight;
      }
    }

    if (this.position < this.text.length) {
      requestAnimationFrame(() => this._renderFrame());
    } else {
      this.isComplete = true;
      if (this.onComplete) this.onComplete();
    }
  }

  pause() {
    this.isPaused = true;
  }

  resume() {
    if (this.isPaused && !this.isComplete) {
      this.isPaused = false;
      this.lastFrameTime = performance.now();
      this._renderFrame();
    }
  }

  skipToEnd() {
    this.position = this.text.length;
    this.target.textContent = this.text;
    this.isComplete = true;
    if (this.onComplete) this.onComplete();
  }
}
```

#### 2.2 添加辅助函数

在 `ai-panel.js` 中添加以下辅助函数：

```javascript
/**
 * 去除 HTML 标签，返回纯文本
 */
function stripHtmlTags(html) {
  const temp = document.createElement('div');
  temp.innerHTML = html;
  return temp.textContent || temp.innerText || '';
}

/**
 * 获取用户偏好的流式速度（字符/秒）
 */
function getUserStreamingSpeed() {
  const SPEED_MAP = {
    'slow': 30,
    'normal': 50,
    'fast': 80
  };
  const speedSetting = localStorage.getItem('ai.streaming.speed') || 'normal';
  return SPEED_MAP[speedSetting] || SPEED_MAP['normal'];
}

/**
 * 检查是否启用流式输出
 */
function isStreamingEnabled() {
  const enabled = localStorage.getItem('ai.streaming.enabled');
  return enabled !== 'false';  // 默认启用
}
```

#### 2.3 修改 startAiStream 函数

**找到**: `async function startAiStream(text)` 函数（约在第 217 行）

**修改**: 在 `event === 'done'` 分支中集成打字机渲染：

```javascript
} else if (event === 'done') {
  // 移除流式加载指示器
  const streamingEl = aiEl.querySelector('.streaming');
  if (streamingEl) streamingEl.remove();

  // 检查是否启用流式输出
  if (isStreamingEnabled()) {
    // ✅ 启用打字机效果
    // 创建流式容器
    const streamingContainer = document.createElement('div');
    streamingContainer.className = 'streaming-text';
    body.appendChild(streamingContainer);

    // 启动打字机渲染（显示纯文本）
    const plainText = stripHtmlTags(acc);
    const typewriter = new TypewriterRenderer(streamingContainer, {
      speed: getUserStreamingSpeed(),
      onComplete: () => applyFormatting(aiEl, body, acc)
    });

    // 允许用户点击跳过
    streamingContainer.addEventListener('click', () => typewriter.skipToEnd());

    typewriter.start(plainText);
  } else {
    // ❌ 禁用流式输出，立即显示格式化内容
    applyFormatting(aiEl, body, acc);
  }

  persistCurrentSession();
}

// 新增：格式化渲染函数
function applyFormatting(aiEl, body, htmlContent) {
  // 清空 body，显示完整格式化内容
  body.innerHTML = htmlContent;

  // 应用现有的格式化增强
  try {
    postEnhanceTables(body);
    if (window.Prism) Prism.highlightAllUnder(aiEl);
    attachInteractiveListeners(aiEl);
  } catch (e) {
    console.warn('Enhanced content error:', e);
  }

  addCodeActions(aiEl);

  // 淡入动画
  body.classList.add('format-applied');
}
```

### 步骤 3: 添加 CSS 样式 (5 分钟)

**文件**: `src/main/resources/static/css/ai-panel.css`

在文件末尾添加以下样式：

```css
/* 流式文本容器 */
.streaming-text {
  font-family: inherit;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  cursor: pointer;  /* 提示可点击跳过 */
  position: relative;
}

/* 闪烁光标 */
.streaming-text::after {
  content: '|';
  display: inline-block;
  margin-left: 2px;
  animation: blink 1s step-end infinite;
  color: var(--primary-color, #007bff);
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  50.01%, 100% { opacity: 0; }
}

/* 格式化完成后淡入 */
.msg.ai .content .body {
  transition: opacity 0.3s ease-in;
}

.msg.ai .content .body.format-applied {
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from { opacity: 0.7; }
  to { opacity: 1; }
}

/* 流式文本提示 */
.streaming-text:hover::before {
  content: '点击跳过';
  position: absolute;
  top: -24px;
  right: 0;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  pointer-events: none;
}
```

### 步骤 4: 添加用户设置 UI (10 分钟)

#### 4.1 修改 HTML 模板

**文件**: `src/main/resources/templates/terminal/ai-panel.html`

找到设置抽屉（settings drawer），添加流式输出配置：

```html
<!-- 在设置抽屉中添加 -->
<div class="setting-section">
  <h4>AI 聊天设置</h4>

  <div class="setting-item">
    <label>
      <input type="checkbox" id="aiStreamingEnabled" checked>
      <span>启用打字机效果</span>
    </label>
    <p class="setting-hint">AI 回复将逐字符流式显示，提升互动体验</p>
  </div>

  <div class="setting-item">
    <label for="aiStreamingSpeed">流式速度</label>
    <select id="aiStreamingSpeed">
      <option value="slow">慢速 (30 字符/秒)</option>
      <option value="normal" selected>正常 (50 字符/秒)</option>
      <option value="fast">快速 (80 字符/秒)</option>
    </select>
    <p class="setting-hint">调整 AI 回复的显示速度</p>
  </div>
</div>
```

#### 4.2 添加设置保存逻辑

在 `ai-panel.js` 中添加设置保存函数：

```javascript
// 初始化：加载用户偏好设置
function initStreamingPreferences() {
  const enabledCheckbox = document.getElementById('aiStreamingEnabled');
  const speedSelect = document.getElementById('aiStreamingSpeed');

  if (enabledCheckbox) {
    // 从 LocalStorage 加载
    const enabled = localStorage.getItem('ai.streaming.enabled');
    if (enabled !== null) {
      enabledCheckbox.checked = enabled === 'true';
    }

    // 监听变化
    enabledCheckbox.addEventListener('change', (e) => {
      localStorage.setItem('ai.streaming.enabled', String(e.target.checked));
      syncPreferencesToBackend();
    });
  }

  if (speedSelect) {
    const speed = localStorage.getItem('ai.streaming.speed');
    if (speed) {
      speedSelect.value = speed;
    }

    speedSelect.addEventListener('change', (e) => {
      localStorage.setItem('ai.streaming.speed', e.target.value);
      syncPreferencesToBackend();
    });
  }
}

// 同步偏好设置到后端
async function syncPreferencesToBackend() {
  const enabled = localStorage.getItem('ai.streaming.enabled') !== 'false';
  const speed = localStorage.getItem('ai.streaming.speed') || 'normal';

  const csrf = getCsrf();  // 复用现有的 getCsrf() 函数

  try {
    await fetch('/profile/preferences', {
      method: 'POST',
      headers: Object.assign(
        { 'Content-Type': 'application/json' },
        csrf.token ? { [csrf.header]: csrf.token } : {}
      ),
      body: JSON.stringify({
        aiStreamingEnabled: enabled,
        aiStreamingSpeed: speed
      })
    });
  } catch (err) {
    console.warn('Failed to sync preferences:', err);
    // 失败不影响使用，LocalStorage 已保存
  }
}

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', () => {
  loadModels();  // 现有代码
  initStreamingPreferences();  // ✅ 新增
  // ... 其他初始化代码
});
```

### 步骤 5: 测试功能 (10 分钟)

#### 5.1 启动应用

```bash
# 重新编译并启动
mvn clean compile spring-boot:run
```

#### 5.2 手动测试清单

访问 `http://localhost:8080/terminal/manager`（或你的 AI 聊天面板页面）：

- [ ] **基础功能测试**
  - [ ] 发送消息，AI 回复逐字符显示（打字机效果）
  - [ ] 闪烁光标在流式输出末尾显示
  - [ ] 流式完成后，内容格式化渲染（Markdown、代码高亮等）

- [ ] **用户控制测试**
  - [ ] 点击流式输出区域，立即显示完整内容
  - [ ] 打开设置，切换"启用打字机效果"复选框，下一条消息生效
  - [ ] 调整流式速度（slow/normal/fast），下一条消息生效

- [ ] **边界情况测试**
  - [ ] 发送多条消息，各自独立流式渲染
  - [ ] 流式输出中途发送新消息，前一条立即完成
  - [ ] 刷新页面，设置保留

- [ ] **兼容性测试**
  - [ ] 禁用流式输出，AI 回复立即显示格式化内容
  - [ ] 现有功能（代码高亮、表格、历史记录）正常工作

#### 5.3 浏览器控制台检查

打开开发者工具 (F12)，确认：
- 无 JavaScript 错误
- 流式渲染时 FPS 稳定在 60 左右（Performance 标签）
- Network 标签中偏好设置 API 调用成功

### 步骤 6: 单元测试（可选，5 分钟）

#### 6.1 后端测试

创建测试文件：`src/test/java/com/cmict/internalpaas/dto/UserPreferencesDtoTest.java`

```java
package com.cmict.internalpaas.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserPreferencesDtoTest {

    @Test
    public void testDefaultStreamingPreferences() {
        UserPreferencesDto dto = new UserPreferencesDto();
        assertTrue(dto.getAiStreamingEnabled(), "默认应启用流式输出");
        assertEquals("normal", dto.getAiStreamingSpeed(), "默认速度应为 normal");
    }

    @Test
    public void testInvalidSpeedFallback() {
        UserPreferencesDto dto = new UserPreferencesDto();
        dto.setAiStreamingSpeed("invalid_value");
        assertEquals("normal", dto.getAiStreamingSpeed(), "无效速度应回退到 normal");
    }

    @Test
    public void testValidSpeedValues() {
        UserPreferencesDto dto = new UserPreferencesDto();

        dto.setAiStreamingSpeed("slow");
        assertEquals("slow", dto.getAiStreamingSpeed());

        dto.setAiStreamingSpeed("fast");
        assertEquals("fast", dto.getAiStreamingSpeed());
    }
}
```

运行测试：
```bash
mvn test -Dtest=UserPreferencesDtoTest
```

## 常见问题

### Q1: 流式输出看起来卡顿

**可能原因**: 浏览器性能不足或速度设置过快

**解决方案**:
1. 检查浏览器 Performance 标签，确认 FPS
2. 降低速度设置（设置为 "slow"）
3. 检查是否有其他 JavaScript 错误阻塞主线程

### Q2: 点击跳过没有反应

**可能原因**: 事件监听器未正确绑定

**解决方案**:
1. 检查浏览器控制台是否有错误
2. 确认 `.streaming-text` 元素的 `click` 监听器已添加
3. 验证 `typewriter.skipToEnd()` 函数被调用（添加 `console.log`）

### Q3: 设置不保存

**可能原因**: LocalStorage 被浏览器阻止或 API 调用失败

**解决方案**:
1. 检查浏览器是否启用了 LocalStorage（隐私模式可能禁用）
2. 查看 Network 标签，确认 `/profile/preferences` API 调用状态
3. 检查 CSRF token 是否正确获取

### Q4: 格式化渲染丢失（代码高亮不工作）

**可能原因**: `applyFormatting` 函数未正确调用

**解决方案**:
1. 确认 `onComplete` 回调正确传递给 `TypewriterRenderer`
2. 检查 `Prism.highlightAllUnder(aiEl)` 是否被调用
3. 确认 Prism.js 库已加载

## 下一步

功能实现完成后：

1. **性能优化**: 使用 Chrome DevTools Performance Profiler 分析渲染性能
2. **用户测试**: 收集真实用户反馈，调整默认速度
3. **国际化**: 为设置 UI 添加多语言支持
4. **辅助功能**: 添加 ARIA 属性支持屏幕阅读器

## 参考资料

- [requestAnimationFrame API 文档](https://developer.mozilla.org/en-US/docs/Web/API/window/requestAnimationFrame)
- [Spring Boot 偏好设置最佳实践](https://spring.io/guides/tutorials/spring-boot-web/)
- 项目文档:
  - [Technical Research](research.md) - 技术方案详解
  - [Data Model](data-model.md) - 数据结构设计
  - [API Contract](contracts/user-preferences-api.md) - API 规范

## 故障排除

遇到问题时，按以下顺序检查：

1. **编译错误** → 检查 Java 代码语法，确认 DTO 字段类型正确
2. **运行时错误** → 查看浏览器控制台和后端日志
3. **功能异常** → 逐步调试 JavaScript 代码（添加 `console.log`）
4. **性能问题** → 使用 Performance Profiler 定位瓶颈

需要帮助？查看 [常见问题](research.md#open-questions--follow-up) 或提交 Issue。
