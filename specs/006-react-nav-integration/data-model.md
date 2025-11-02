# Data Model: React Navigation Integration

**Feature**: 006-react-nav-integration
**Date**: 2025-11-02

## Overview

本功能主要涉及UI集成和状态管理，无需数据库持久化。以下文档描述运行时状态模型和事件数据结构。

---

## State Models

### 1. LayoutMode (布局模式)

**类型**: 枚举 (TypeScript Union Type)

```typescript
type LayoutMode = 'shell' | 'content-only' | 'minimal';
```

**值域**:
- `'shell'`: 完整布局（包含侧边栏） - 用于独立模式
- `'content-only'`: 仅内容布局（无侧边栏） - 用于嵌入模式
- `'minimal'`: 最小化布局（可选，用于特殊场景如打印预览）

**用途**: 决定React应用渲染哪个Layout组件

**生命周期**: 应用初始化时确定，运行时不变（除非开发者手动切换）

---

### 2. LayoutContextValue (布局上下文值)

**类型**: TypeScript Interface

```typescript
interface LayoutContextValue {
  mode: LayoutMode;              // 当前布局模式
  setMode: (mode: LayoutMode) => void;  // 切换布局模式的方法
  isEmbedded: boolean;           // 是否在嵌入模式下运行
}
```

**字段说明**:

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `mode` | LayoutMode | ✅ | 'content-only' (嵌入) / 'shell' (独立) | 当前激活的布局模式 |
| `setMode` | Function | ✅ | - | 用于运行时切换布局的函数 |
| `isEmbedded` | boolean | ✅ | 根据环境检测 | 标识应用是否在主应用中嵌入运行 |

**校验规则**:
- `mode` 必须是 `LayoutMode` 的有效值之一
- `isEmbedded` 一旦初始化后不应更改

**状态转换**:
```
初始化 → 检测嵌入模式 → 设置 mode
  ↓
isEmbedded=true → mode='content-only'
isEmbedded=false → mode='shell'
```

---

### 3. NavigationEvent (导航事件)

**类型**: CustomEvent Detail数据结构

```typescript
interface NavigationEventDetail {
  route: string;         // 目标路由路径
  source: 'main-app' | 'react';  // 事件源
  timestamp?: number;    // 可选：事件时间戳
}
```

**字段说明**:

| 字段 | 类型 | 必填 | 示例值 | 说明 |
|------|------|------|--------|------|
| `route` | string | ✅ | '/deploy-platform/overview' | React Router路径 |
| `source` | 'main-app' \| 'react' | ✅ | 'main-app' | 标识事件来源，用于防止循环触发 |
| `timestamp` | number | ❌ | 1698765432000 | Unix时间戳，用于调试和监控 |

**路由格式约定**:
- 必须以 `/deploy-platform/` 开头
- 有效的子路由: `overview`, `releases`, `policies`
- 示例: `/deploy-platform/overview`, `/deploy-platform/releases`

**校验规则**:
- `route` 不能为空字符串
- `route` 必须符合路由格式约定
- `source` 必须是 `'main-app'` 或 `'react'` 之一

---

## Event Flow Models

### 主应用 → React导航事件

**Event Name**: `'main-nav-change'`

**触发时机**: 用户点击主应用侧边栏子菜单项

**Event Payload**:
```javascript
new CustomEvent('main-nav-change', {
  detail: {
    route: '/deploy-platform/releases',
    source: 'main-app',
    timestamp: Date.now()
  }
})
```

**监听方**: React应用 (useNavSync Hook)

**处理逻辑**:
1. 检查 `detail.source !== 'react'`（避免循环）
2. 调用 `navigate(detail.route)`
3. 不触发反向事件（单向同步）

---

### React → 主应用导航事件

**Event Name**: `'react-nav-change'`

**触发时机**: React Router location变化（用户在React内部导航）

**Event Payload**:
```javascript
new CustomEvent('react-nav-change', {
  detail: {
    route: location.pathname,
    source: 'react',
    timestamp: Date.now()
  }
})
```

**监听方**: 主应用侧边栏 (navigation-sync.js)

**处理逻辑**:
1. 检查 `detail.source !== 'main-app'`（避免循环）
2. 更新侧边栏菜单高亮状态
3. 不触发导航（仅UI更新）

---

## Runtime State Management

### 防循环触发机制

**状态变量**:
```javascript
// 主应用侧
let isSyncing = false;
let lastEventSource = null;

// React侧
let isSyncing = false;
let lastEventSource = null;
```

**状态转换图**:
```
[空闲] → 用户操作 → [设置isSyncing=true] → [发送事件]
  → [300ms超时] → [重置isSyncing=false] → [空闲]

[接收到事件] → [检查source] → (source===自己?)
  → YES: 忽略
  → NO: 处理事件
```

**超时重置**: 300ms自动重置，防止状态卡死

---

## Non-Persisted State

以下状态仅存在于JavaScript运行时内存中，不持久化：

| 状态 | 存储位置 | 生命周期 |
|------|---------|---------|
| `LayoutMode` | React Context | 应用初始化→卸载 |
| `isEmbedded` | React State | 应用初始化→卸载 |
| `isSyncing` | 闭包变量 | 应用初始化→卸载 |
| `lastEventSource` | 闭包变量 | 应用初始化→卸载 |

**刷新页面**: 所有状态重新计算/初始化

---

## Browser State

### LocalStorage (可选实现)

如果需要持久化用户偏好（Phase 2可选功能）：

```typescript
interface LayoutPreference {
  preferredMode?: LayoutMode;  // 用户偏好的布局模式
  lastRoute?: string;          // 最后访问的路由
}

// Key: 'deploy-platform:layout-preference'
```

**当前Phase 1实施**: 不使用LocalStorage，完全依赖运行时检测

---

## URL State

### Hash Fragment (可选实现)

用于深链接支持（Phase 2可选功能）：

```
示例URL: /admin/workspace#page=deploy-platform/releases
```

**解析逻辑**:
```typescript
const hash = window.location.hash;
const match = hash.match(/#page=(.+)/);
if (match) {
  const route = `/${match[1]}`;
  navigate(route);
}
```

**当前Phase 1实施**: 不使用Hash，简单的路由同步

---

## Validation Rules

### 路由验证

```typescript
function isValidRoute(route: string): boolean {
  const validRoutes = [
    '/deploy-platform/overview',
    '/deploy-platform/releases',
    '/deploy-platform/policies'
  ];
  return validRoutes.includes(route);
}
```

### 布局模式验证

```typescript
function isValidLayoutMode(mode: string): mode is LayoutMode {
  return mode === 'shell' || mode === 'content-only' || mode === 'minimal';
}
```

---

## Error States

### 无效路由

**场景**: 接收到无效的导航事件

**处理**:
```typescript
if (!isValidRoute(detail.route)) {
  console.warn(`Invalid route received: ${detail.route}`);
  navigate('/deploy-platform/overview'); // 回退到默认页面
}
```

### React未初始化

**场景**: 主应用发送导航事件，但React应用尚未加载完成

**处理**: 使用导航队列暂存事件
```javascript
const navQueue = [];
window.addEventListener('react-app-ready', () => {
  navQueue.forEach(event => window.dispatchEvent(event));
  navQueue.length = 0;
});
```

---

## Diagrams

### 状态机: 布局模式选择

```
┌─────────────┐
│ 应用启动     │
└──────┬──────┘
       │
       v
┌──────────────────────┐
│ useEmbedMode检测     │
│ - 检查容器元素        │
│ - 检查Spring标志      │
│ - 检查数据属性        │
└──────┬───────────────┘
       │
       ├─── isEmbedded=true ───> mode='content-only' ───> 渲染ContentOnlyLayout
       │
       └─── isEmbedded=false ──> mode='shell' ──────────> 渲染ShellLayout
```

### 事件流: 导航同步

```
用户点击侧边栏
    │
    v
main-layout.html
  handleSubmenuClick()
    │
    ├─ 设置 isSyncing=true
    ├─ 设置 lastEventSource='main-app'
    ├─ dispatchEvent('main-nav-change')
    └─ setTimeout(300ms) → isSyncing=false
    │
    v
window.dispatchEvent
    │
    v
React: useNavSync监听器
    │
    ├─ 检查 source !== 'react'? YES
    ├─ 调用 navigate(route)
    └─ location变化
    │
    v
React Router
    │
    v
页面组件重新渲染
```

---

## Summary

本数据模型文档定义了：
- ✅ 3个核心状态模型（LayoutMode, LayoutContextValue, NavigationEvent）
- ✅ 2个事件流模型（主应用→React, React→主应用）
- ✅ 运行时状态管理机制（防循环触发）
- ✅ 错误处理和边界情况
- ✅ 验证规则和状态转换

**无持久化数据**: 本功能纯前端UI集成，所有状态运行时计算。
