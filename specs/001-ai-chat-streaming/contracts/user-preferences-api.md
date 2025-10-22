# API Contract: User Preferences (扩展)

**Feature**: AI Chat Streaming Output
**Endpoint**: `POST /profile/preferences` (existing)
**Purpose**: 更新用户偏好设置，包括流式输出相关配置

## Endpoint Details

**Method**: `POST`
**Path**: `/profile/preferences`
**Authentication**: Required (Session-based, Spring Security)
**Content-Type**: `application/json`

## Request

### Headers

| Header | Required | Value | Description |
|--------|----------|-------|-------------|
| `Content-Type` | ✅ Yes | `application/json` | JSON 格式请求体 |
| `X-CSRF-TOKEN` | ✅ Yes | `{csrf_token}` | CSRF 保护 token |
| `Cookie` | ✅ Yes | `JSESSIONID=...` | Session cookie |

### Body Schema

**Type**: `application/json`

```json
{
  // 现有字段 (可选，部分更新)
  "theme": "light",
  "emailNotifications": true,
  "systemNotifications": true,
  "terminalTheme": "dark",
  "terminalFontSize": 14,
  "terminalFontFamily": "Monaco",
  "language": "zh_CN",
  "timeZone": "Asia/Shanghai",

  // [新增] 流式输出配置字段
  "aiStreamingEnabled": true,       // ✅ 本功能新增
  "aiStreamingSpeed": "normal"      // ✅ 本功能新增
}
```

### Field Definitions

| Field | Type | Required | Default | Valid Values | Description |
|-------|------|----------|---------|--------------|-------------|
| `aiStreamingEnabled` | `boolean` | ❌ No | `true` | `true`, `false` | 是否启用 AI 回复的打字机效果 |
| `aiStreamingSpeed` | `string` | ❌ No | `"normal"` | `"slow"`, `"normal"`, `"fast"` | 打字机效果速度 |

**Notes**:
- 所有字段均为可选 (部分更新模式)
- 仅更新请求体中提供的字段，未提供的字段保持不变
- 无效值自动回退到默认值，不返回错误

### Request Examples

#### 示例 1: 仅更新流式输出设置

```http
POST /profile/preferences HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-CSRF-TOKEN: a8f3e9b4-c1d2-4e5f-6a7b-8c9d0e1f2a3b
Cookie: JSESSIONID=A1B2C3D4E5F6G7H8I9J0

{
  "aiStreamingEnabled": true,
  "aiStreamingSpeed": "fast"
}
```

#### 示例 2: 禁用流式输出

```http
POST /profile/preferences HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-CSRF-TOKEN: a8f3e9b4-c1d2-4e5f-6a7b-8c9d0e1f2a3b
Cookie: JSESSIONID=A1B2C3D4E5F6G7H8I9J0

{
  "aiStreamingEnabled": false
}
```

#### 示例 3: 同时更新多个偏好

```http
POST /profile/preferences HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-CSRF-TOKEN: a8f3e9b4-c1d2-4e5f-6a7b-8c9d0e1f2a3b
Cookie: JSESSIONID=A1B2C3D4E5F6G7H8I9J0

{
  "theme": "dark",
  "aiStreamingEnabled": true,
  "aiStreamingSpeed": "normal",
  "terminalFontSize": 16
}
```

## Response

### Success Response (200 OK)

**Status**: `200 OK`
**Content-Type**: `application/json`

```json
{
  "success": true,
  "message": "偏好设置已更新",
  "preferences": {
    "theme": "light",
    "emailNotifications": true,
    "systemNotifications": true,
    "applicationStatusNotifications": true,
    "securityNotifications": true,
    "dashboardLayout": "default",
    "showWelcomeMessage": true,
    "showQuickActions": true,
    "showRecentActivity": true,
    "terminalTheme": "dark",
    "terminalFontSize": 14,
    "terminalFontFamily": "Monaco",
    "language": "zh_CN",
    "timeZone": "Asia/Shanghai",
    "aiStreamingEnabled": true,      // ✅ 返回更新后的值
    "aiStreamingSpeed": "normal"     // ✅ 返回更新后的值
  }
}
```

### Error Responses

#### 401 Unauthorized - 未登录

```json
{
  "success": false,
  "message": "未登录，请先登录"
}
```

#### 403 Forbidden - CSRF Token 无效

```json
{
  "success": false,
  "message": "CSRF token 验证失败"
}
```

#### 400 Bad Request - 无效的 JSON

```json
{
  "success": false,
  "message": "请求格式错误"
}
```

**Note**: 无效的字段值（如 `aiStreamingSpeed: "invalid"`）不会返回错误，而是自动回退到默认值。

## Read Preferences API

### GET /api/profile/preferences

**Purpose**: 读取当前用户的偏好设置

#### Request

```http
GET /api/profile/preferences HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=A1B2C3D4E5F6G7H8I9J0
```

#### Response (200 OK)

```json
{
  "theme": "light",
  "emailNotifications": true,
  "systemNotifications": true,
  "applicationStatusNotifications": true,
  "securityNotifications": true,
  "dashboardLayout": "default",
  "showWelcomeMessage": true,
  "showQuickActions": true,
  "showRecentActivity": true,
  "terminalTheme": "dark",
  "terminalFontSize": 14,
  "terminalFontFamily": "Monaco",
  "language": "zh_CN",
  "timeZone": "Asia/Shanghai",
  "aiStreamingEnabled": true,      // ✅ 新增字段
  "aiStreamingSpeed": "normal"     // ✅ 新增字段
}
```

**Note**: 如果用户从未设置过偏好，返回 `UserPreferencesDto` 的默认值。

## Contract Tests

### Test Cases

#### TC-001: 更新流式输出设置

**Given**: 用户已登录
**When**: POST `/profile/preferences` with `{"aiStreamingEnabled": false, "aiStreamingSpeed": "fast"}`
**Then**:
- 返回 200 OK
- 响应中 `preferences.aiStreamingEnabled` = `false`
- 响应中 `preferences.aiStreamingSpeed` = `"fast"`
- 数据库中 `users.preferences` JSON 字段已更新

#### TC-002: 无效速度值回退

**Given**: 用户已登录
**When**: POST `/profile/preferences` with `{"aiStreamingSpeed": "超快"}`
**Then**:
- 返回 200 OK (不报错)
- 响应中 `preferences.aiStreamingSpeed` = `"normal"` (默认值)

#### TC-003: 部分更新不影响其他字段

**Given**: 用户已登录，当前 `theme="dark"`
**When**: POST `/profile/preferences` with `{"aiStreamingEnabled": false}`
**Then**:
- 返回 200 OK
- 响应中 `preferences.theme` = `"dark"` (保持不变)
- 响应中 `preferences.aiStreamingEnabled` = `false` (已更新)

#### TC-004: 未登录返回 401

**Given**: 用户未登录 (无 session cookie)
**When**: POST `/profile/preferences` with any body
**Then**:
- 返回 401 Unauthorized
- 响应中 `success` = `false`

#### TC-005: 读取默认偏好

**Given**: 新用户，从未设置过偏好
**When**: GET `/api/profile/preferences`
**Then**:
- 返回 200 OK
- 响应中 `aiStreamingEnabled` = `true` (默认值)
- 响应中 `aiStreamingSpeed` = `"normal"` (默认值)

## Frontend Integration

### JavaScript API Call Example

```javascript
// 更新流式输出偏好
async function updateStreamingPreferences(enabled, speed) {
  const csrf = {
    token: document.querySelector('meta[name="_csrf"]')?.content,
    header: document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN'
  };

  const response = await fetch('/profile/preferences', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [csrf.header]: csrf.token
    },
    body: JSON.stringify({
      aiStreamingEnabled: enabled,
      aiStreamingSpeed: speed
    })
  });

  if (!response.ok) {
    throw new Error('Failed to update preferences');
  }

  const result = await response.json();

  // 更新 LocalStorage 缓存
  localStorage.setItem('ai.streaming.enabled', String(result.preferences.aiStreamingEnabled));
  localStorage.setItem('ai.streaming.speed', result.preferences.aiStreamingSpeed);

  return result.preferences;
}

// 读取偏好 (带 LocalStorage 缓存)
async function getStreamingPreferences() {
  // 优先从缓存读取
  const cachedEnabled = localStorage.getItem('ai.streaming.enabled');
  const cachedSpeed = localStorage.getItem('ai.streaming.speed');

  if (cachedEnabled !== null && cachedSpeed !== null) {
    return {
      enabled: cachedEnabled === 'true',
      speed: cachedSpeed
    };
  }

  // 缓存未命中，从 API 加载
  const response = await fetch('/api/profile/preferences');
  const prefs = await response.json();

  // 更新缓存
  localStorage.setItem('ai.streaming.enabled', String(prefs.aiStreamingEnabled));
  localStorage.setItem('ai.streaming.speed', prefs.aiStreamingSpeed);

  return {
    enabled: prefs.aiStreamingEnabled,
    speed: prefs.aiStreamingSpeed
  };
}
```

## Backend Implementation Notes

### UserController.java 修改要点

**现有方法** (复用，无需修改):
```java
@PostMapping("/profile/preferences")
public ResponseEntity<?> updatePreferences(@RequestBody UserPreferencesDto preferences) {
    // 现有实现已支持部分更新
    // 只需确保 UserPreferencesDto 包含新字段即可
    User user = getCurrentUser();
    userService.updatePreferences(user.getId(), preferences);
    return ResponseEntity.ok(/* ... */);
}
```

**Required Changes**: 无需修改控制器代码，仅需扩展 `UserPreferencesDto`

### UserPreferencesDto.java 修改

**新增字段**:
```java
// AI 流式输出设置
private Boolean aiStreamingEnabled = true;
private String aiStreamingSpeed = "normal";

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

## Summary

**API 契约复杂度**: 极低
- ✅ 复用现有 API 端点 (`POST /profile/preferences`)
- ✅ 向后兼容：新字段为可选，现有客户端不受影响
- ✅ 优雅降级：无效值自动回退，不报错
- ✅ 部分更新：仅更新提供的字段

**前后端协作**:
1. 前端优先使用 LocalStorage 缓存，提升响应速度
2. 后端提供权威数据源，确保跨设备一致性
3. 异步同步策略避免阻塞用户交互
