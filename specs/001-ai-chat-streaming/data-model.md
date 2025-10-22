# Phase 1: Data Model - AI Chat Streaming Output

**Feature**: AI Chat Streaming Output
**Date**: 2025-10-21
**Status**: Design Complete

## Overview

本功能主要在前端实现流式渲染逻辑，后端数据模型变更极小。复用现有的用户偏好设置系统 (`UserPreferencesDto`) 存储流式输出相关配置。

## Entity Relationships

```
┌─────────────────────┐
│      User           │
│  (existing entity)  │
└──────────┬──────────┘
           │ 1
           │
           │ 1
           ▼
┌─────────────────────┐
│ UserPreferencesDto  │ ◄── [扩展] 新增字段: streamingEnabled, streamingSpeed
│  (existing DTO)     │
└─────────────────────┘
```

## Entities

### UserPreferencesDto (扩展现有)

**Purpose**: 存储用户的全局偏好设置，包括流式输出相关配置

**Location**: `src/main/java/com/cmict/internalpaas/dto/UserPreferencesDto.java`

**Modifications Required**:

```java
// 新增字段
private Boolean aiStreamingEnabled = true;  // 默认启用流式输出
private String aiStreamingSpeed = "normal";  // slow, normal, fast
```

**Existing Fields** (保持不变):
- `String theme`
- `Boolean emailNotifications`
- `String terminalTheme`
- `Integer terminalFontSize`
- ... (其他现有字段)

**Validation Rules**:
- `aiStreamingEnabled`: 必须是 `true` 或 `false`，默认 `true`
- `aiStreamingSpeed`: 必须是 `"slow"`, `"normal"`, 或 `"fast"` 之一，默认 `"normal"`
- 无效值时回退到默认值而非抛出异常

**Field Mapping**:

| 字段 | 类型 | 默认值 | 描述 | 前端对应 |
|------|------|--------|------|---------|
| `aiStreamingEnabled` | `Boolean` | `true` | 是否启用打字机效果 | `localStorage.streamingEnabled` |
| `aiStreamingSpeed` | `String` | `"normal"` | 流式输出速度 | `localStorage.streamingSpeed` |

**Speed Mapping** (前端实现):

| 配置值 | 字符/秒 | 描述 | 适用场景 |
|--------|---------|------|---------|
| `slow` | 30 | 慢速 | 喜欢逐字阅读的用户 |
| `normal` | 50 | 正常 | 默认设置，平衡速度和可读性 |
| `fast` | 80 | 快速 | 快速阅读或跳过动画 |

### 前端状态管理 (TypewriterRenderer)

**Purpose**: 管理单个 AI 消息的流式渲染状态

**Location**: `src/main/resources/static/js/ai-panel.js` (新增类)

**Structure**:

```javascript
class TypewriterRenderer {
  // 状态字段
  target: HTMLElement;          // 渲染目标 DOM 元素
  text: string;                 // 完整文本内容
  position: number;             // 当前渲染位置 (字符索引)
  charsPerSecond: number;       // 渲染速度 (字符/秒)
  lastFrameTime: number;        // 上一帧时间戳 (performance.now())
  isPaused: boolean;            // 是否暂停
  isComplete: boolean;          // 是否完成
  onComplete: Function;         // 完成回调函数

  // 方法
  start(fullText: string): void;       // 开始流式渲染
  pause(): void;                       // 暂停渲染
  resume(): void;                      // 恢复渲染
  skipToEnd(): void;                   // 跳到结尾立即显示完整内容
  _renderFrame(): void;                // 内部: 渲染单帧
}
```

**State Transitions**:

```
[Initial]
   │
   ├─ start() ──> [Rendering] ◄─┐
   │                   │         │
   │                   ├─ pause() ──> [Paused] ─ resume()
   │                   │
   │                   ├─ skipToEnd() ──┐
   │                   │                │
   │                   ├─ (text end) ───┤
   │                   │                │
   │                   ▼                ▼
   └──────────────> [Complete]
```

**Lifecycle Example**:

```javascript
// 1. 创建实例
const renderer = new TypewriterRenderer(targetElement, {
  speed: 50,  // chars/sec
  onComplete: () => applyFormatting()
});

// 2. 开始渲染
renderer.start("这是 AI 的回复内容...");

// 3. 用户交互
renderer.pause();           // 暂停 (可选)
renderer.resume();          // 恢复 (可选)
renderer.skipToEnd();       // 立即显示全部 (FR-005)

// 4. 自动完成
// 渲染到文本末尾时自动调用 onComplete()
```

## Data Storage

### LocalStorage (前端)

**Purpose**: 快速访问的客户端偏好缓存

**Keys**:

| Key | Type | Example | Description |
|-----|------|---------|-------------|
| `ai.streaming.enabled` | `string` (boolean) | `"true"` | 是否启用流式输出 |
| `ai.streaming.speed` | `string` | `"normal"` | 流式速度设置 |

**Sync Strategy**:
1. **读取优先级**: LocalStorage → API (`/api/profile/preferences`) → 默认值
2. **写入策略**: 立即写入 LocalStorage + 异步同步到后端 API
3. **同步时机**: 用户修改设置时立即触发

```javascript
// 读取偏好 (带回退)
function getStreamingEnabled() {
  const local = localStorage.getItem('ai.streaming.enabled');
  if (local !== null) return local === 'true';

  // 从后端加载 (异步)
  fetchUserPreferences().then(prefs => {
    localStorage.setItem('ai.streaming.enabled', String(prefs.aiStreamingEnabled));
  });

  return true;  // 默认值
}

// 保存偏好 (双写)
function setStreamingEnabled(enabled) {
  localStorage.setItem('ai.streaming.enabled', String(enabled));

  // 异步同步到后端
  fetch('/profile/preferences', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ aiStreamingEnabled: enabled })
  });
}
```

### 数据库 (后端)

**Table**: `users` (现有表)

**Column**: `preferences` (JSON 类型, 现有字段)

**JSON Structure** (扩展):

```json
{
  "theme": "light",
  "terminalTheme": "dark",
  "terminalFontSize": 14,
  // ... 其他现有字段 ...

  // [新增] 流式输出配置
  "aiStreamingEnabled": true,
  "aiStreamingSpeed": "normal"
}
```

**Migration**: 无需数据库迁移，JSON 字段自动扩展

**Default Behavior**:
- 新用户: 自动使用 `UserPreferencesDto` 的默认值 (`aiStreamingEnabled=true`, `aiStreamingSpeed="normal"`)
- 现有用户: 字段缺失时使用默认值 (优雅降级)

## Validation Rules

### 后端验证 (UserPreferencesDto)

```java
public void setAiStreamingSpeed(String speed) {
    // 验证速度值
    if ("slow".equals(speed) || "normal".equals(speed) || "fast".equals(speed)) {
        this.aiStreamingSpeed = speed;
    } else {
        // 无效值时使用默认值
        this.aiStreamingSpeed = "normal";
    }
}
```

### 前端验证 (JavaScript)

```javascript
const SPEED_MAP = {
  'slow': 30,
  'normal': 50,
  'fast': 80
};

function getCharsPerSecond(speedSetting) {
  return SPEED_MAP[speedSetting] || SPEED_MAP['normal'];  // 回退到默认值
}
```

## Data Flow

### 用户修改偏好设置流程

```
┌─────────────────┐
│  User Action    │
│ (设置UI点击)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ JavaScript      │
│ 更新 LocalStorage│
└────────┬────────┘
         │
         ├───────────────┐
         │ (立即)        │ (异步)
         ▼               ▼
┌─────────────────┐  ┌──────────────────────┐
│ UI 立即响应     │  │ POST /profile/preferences│
│ (下一条消息生效)│  │ 同步到数据库          │
└─────────────────┘  └──────────────────────┘
```

### AI 响应流式渲染流程

```
┌──────────────────┐
│ Backend SSE      │
│ event: token     │
│ data: "你好"     │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ JavaScript       │
│ 累积文本到 acc   │
└────────┬─────────┘
         │
         ▼
  ┌─────────────────┐
  │ 检查偏好设置     │
  └────────┬─────────┘
           │
     ┌─────┴─────┐
     │ enabled?  │
     └─────┬─────┘
           │
    ┌──────┴──────┐
    │ YES         │ NO
    ▼             ▼
┌────────────┐  ┌──────────────┐
│TypewriterR.│  │立即显示完整   │
│start(acc)  │  │body.innerHTML │
└─────┬──────┘  └──────────────┘
      │
      ▼ (每帧)
┌────────────┐
│ 逐字符渲染  │
│ textContent │
└─────┬──────┘
      │
      ▼ (完成)
┌────────────┐
│ applyFormat│
│ 格式化渲染  │
└────────────┘
```

## Error Handling

### 数据缺失或损坏

| 场景 | 处理策略 | 用户体验 |
|------|---------|---------|
| LocalStorage 被清空 | 从后端加载，失败则用默认值 | 透明，不影响使用 |
| 后端 preferences 为 null | 使用 DTO 默认值 | 透明 |
| 无效的 speed 值 | 回退到 "normal" | 透明 |
| API 请求失败 | 保留 LocalStorage 值，后台重试 | 设置仍然生效 |

### 并发冲突

**场景**: 用户在多个标签页同时修改偏好

**处理**:
- LocalStorage 跨标签页共享，最后写入生效
- 后端 API 最后请求覆盖前一次请求
- 用户刷新页面后从后端重新加载，保证最终一致性

**不需要处理**: 流式渲染状态 (`TypewriterRenderer`) 是每个消息独立的，无跨标签页共享需求

## Testing Considerations

### 数据模型测试

1. **后端单元测试**:
   ```java
   @Test
   public void testStreamingPreferencesDefaults() {
       UserPreferencesDto dto = new UserPreferencesDto();
       assertEquals(true, dto.getAiStreamingEnabled());
       assertEquals("normal", dto.getAiStreamingSpeed());
   }

   @Test
   public void testInvalidSpeedFallback() {
       UserPreferencesDto dto = new UserPreferencesDto();
       dto.setAiStreamingSpeed("invalid");
       assertEquals("normal", dto.getAiStreamingSpeed());
   }
   ```

2. **前端手动测试**:
   - 清空 LocalStorage → 验证默认值生效
   - 设置 speed="invalid" → 验证回退到 "normal"
   - 网络断开情况下修改设置 → 验证 LocalStorage 仍然生效

3. **集成测试**:
   - 修改偏好 → 刷新页面 → 验证设置保留
   - 多标签页同时修改 → 验证最终一致性

## Migration & Compatibility

### 向后兼容性

✅ **100% 向后兼容**

- 现有用户: `preferences` JSON 字段缺失新字段时使用默认值
- 现有 API: `POST /profile/preferences` 接受部分字段更新
- 前端: 通过 `localStorage` 缓存避免频繁 API 调用

### 数据迁移

❌ **无需数据迁移**

- JSON 字段自动扩展，无需 ALTER TABLE
- 默认值在应用层处理，不需要 SQL UPDATE

### 回滚策略

如需回滚本功能：
1. 前端 JavaScript 代码回滚 (删除 `TypewriterRenderer` 类)
2. 后端保留 `aiStreamingEnabled` 和 `aiStreamingSpeed` 字段 (不影响其他功能)
3. 用户设置保留，未来重新启用功能时自动恢复

## Summary

**数据模型复杂度**: 极低
- 仅扩展 1 个现有 DTO (`UserPreferencesDto`)
- 新增 2 个字段 (`aiStreamingEnabled`, `aiStreamingSpeed`)
- 前端新增 1 个类 (`TypewriterRenderer`)
- 无数据库迁移
- 无新增 API 端点 (复用现有 `/profile/preferences`)

**关键设计决策**:
✅ 复用现有偏好设置系统，避免新增 API
✅ LocalStorage 缓存提升响应速度
✅ 优雅降级确保向后兼容
✅ 每消息独立状态避免并发问题
