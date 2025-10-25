# Contract: DeploymentLogger

**Purpose**: 定义Agent部署过程中的日志格式和记录规范

**实现方式**: 通过 `AgentDeployment.appendLog(String message)` 方法

---

## 日志格式规范

### 基本格式

```
[阶段] [状态标记] 消息内容 [(附加信息)]
```

**组成部分**:
- **阶段**: `[上传]`、`[安装]`、`[健康检查]` 等
- **状态标记**: `✅` (成功)、`❌` (失败)、`⚠️` (警告)、`ℹ️` (信息)、无标记 (进行中)
- **消息内容**: 描述当前操作
- **附加信息**: 可选，提供额外细节（文件大小、URL、错误原因等）

---

## 二进制获取阶段日志

### 1. 缓存命中

```
[上传] 使用已缓存的Agent二进制
```

**时机**: 从内存缓存中获取到数据时
**级别**: INFO
**必需**: 是

---

### 2. 资源文件加载

```
[上传] 使用内置Agent包 (agent/otelcol-linux-amd64.tar.gz, 42.35 MB)
```

**时机**: 从classpath资源成功读取文件后
**格式**:
- 路径: 相对于resources根目录的完整路径
- 大小: 保留两位小数的MB单位（使用 `String.format(Locale.ROOT, "%.2f", sizeMB)`）

**级别**: INFO
**必需**: 是

---

### 3. HTTP下载 - 开始

```
[上传] 通过下载地址获取Agent二进制: https://example.com/otelcol.tar.gz
```

**时机**: 开始执行HTTP下载前
**格式**:
- URL: 完整的下载地址（敏感认证信息应脱敏）
- 脱敏示例: `https://***:***@example.com/file.tar.gz`

**级别**: INFO
**必需**: 是

---

### 4. HTTP下载 - 成功

```
[上传] ✅ 下载Agent二进制成功 (42.35 MB)
```

**时机**: HTTP响应成功（2xx状态码）且数据接收完成后
**格式**:
- 大小: 保留两位小数的MB单位

**级别**: INFO
**必需**: 是

---

### 5. HTTP下载 - 失败

```
[上传] ❌ 下载Agent二进制失败: {具体错误原因}
```

**时机**: HTTP下载失败时（任何异常）
**错误原因示例**:
- `下载超时（5分钟限制），文件可能过大或网络过慢`
- `无法连接到下载服务器，请检查网络或URL配置`
- `下载失败（文件不存在或无权限），HTTP状态码：404`
- `下载服务器错误，HTTP状态码：500，请稍后重试`

**级别**: ERROR
**必需**: 是

---

### 6. 文件验证 - 通过

```
[上传] ✅ 文件验证通过
```

**时机**: `validateBinary()` 返回true后
**级别**: INFO
**必需**: 是

---

### 7. 文件验证 - 失败

```
[上传] ❌ 文件验证失败，大小：1048576 bytes
```

**时机**: `validateBinary()` 返回false后
**格式**:
- 大小: 字节单位（用于诊断）

**级别**: ERROR
**必需**: 是

---

### 8. 总体失败

```
[上传] ❌ 未能获取Agent二进制文件，请检查配置
```

**时机**: `resolveAgentBinary()` 返回null时
**级别**: ERROR
**必需**: 是

---

## 文件上传阶段日志

### 9. 上传开始

```
[上传] 上传Agent二进制文件 (42.35 MB)...
```

**时机**: 调用 `uploadFileBytes()` 前
**格式**:
- 大小: 保留两位小数的MB单位
- 省略号表示进行中

**级别**: INFO
**必需**: 是

---

### 10. 上传成功

```
[上传] ✅ Agent二进制文件上传成功
```

**时机**: `uploadFileBytes()` 返回true后
**级别**: INFO
**必需**: 是

---

### 11. 上传失败

```
[上传] ⚠️ Agent二进制文件上传失败，请检查网络或磁盘空间
```

**时机**: `uploadFileBytes()` 返回false后
**级别**: WARN
**必需**: 是

---

## 日志记录契约

### 调用方式

```java
deployment.appendLog("[上传] 通过下载地址获取Agent二进制: " + sanitizeUrl(downloadUrl));

double sizeMB = data.length / 1024.0 / 1024.0;
deployment.appendLog(String.format(Locale.ROOT,
    "[上传] ✅ 下载Agent二进制成功 (%.2f MB)", sizeMB));
```

### 格式化要求

1. **文件大小**: 始终使用 `Locale.ROOT` 避免国际化问题（确保小数点为`.`而非`,`）
2. **URL脱敏**: 使用 `sanitizeUrl()` 方法移除认证信息
3. **Unicode符号**: 直接使用 `✅`、`❌`、`⚠️`、`ℹ️`（Java String原生支持）
4. **换行**: 每条日志自动换行，`appendLog()` 方法内部处理

---

## 时间戳处理

**现有机制**: `AgentDeployment.appendLog()` 自动添加时间戳

**格式示例**:
```
2025-10-25 14:32:15 [上传] 通过下载地址获取Agent二进制: https://example.com/file.tar.gz
2025-10-25 14:32:45 [上传] ✅ 下载Agent二进制成功 (42.35 MB)
```

**无需手动添加**: 调用方只需提供消息内容

---

## 日志完整性要求

### 成功流程日志示例

```
[上传] 通过下载地址获取Agent二进制: https://github.com/.../otelcol.tar.gz
[上传] ✅ 下载Agent二进制成功 (42.35 MB)
[上传] ✅ 文件验证通过
[上传] 上传Agent二进制文件 (42.35 MB)...
[上传] ✅ Agent二进制文件上传成功
```

### 缓存命中流程日志示例

```
[上传] 使用已缓存的Agent二进制
[上传] 上传Agent二进制文件 (42.35 MB)...
[上传] ✅ Agent二进制文件上传成功
```

### 资源文件流程日志示例

```
[上传] 使用内置Agent包 (agent/otelcol-linux-amd64.tar.gz, 42.35 MB)
[上传] 上传Agent二进制文件 (42.35 MB)...
[上传] ✅ Agent二进制文件上传成功
```

### 失败流程日志示例

```
[上传] 通过下载地址获取Agent二进制: https://example.com/file.tar.gz
[上传] ❌ 下载Agent二进制失败: 下载超时（5分钟限制），文件可能过大或网络过慢
[上传] ❌ 未能获取Agent二进制文件，请检查配置
```

---

## 测试验证要点

### 单元测试

1. **test_log_whenCacheHit_formatCorrect**
   - 验证: 日志包含"使用已缓存"
   - 验证: 无文件大小信息（缓存命中不重复记录大小）

2. **test_log_whenDownloadSuccess_formatCorrect**
   - 验证: 日志包含"✅ 下载Agent二进制成功"
   - 验证: 大小格式为"XX.XX MB"（两位小数）

3. **test_log_whenDownloadFail_formatCorrect**
   - 验证: 日志包含"❌ 下载Agent二进制失败"
   - 验证: 包含具体错误原因

4. **test_log_whenUrlWithAuth_sanitized**
   - 输入: `https://user:pass@example.com/file.tar.gz`
   - 验证: 日志显示`https://***:***@example.com/file.tar.gz`

5. **test_log_fileSizeFormat_usesDot**
   - 输入: 44,370,176 bytes (42.35 MB)
   - 验证: 日志包含"42.35"而非"42,35"（确保使用Locale.ROOT）

---

## URL脱敏实现

```java
private String sanitizeUrl(String url) {
    if (url == null) return "";
    // 移除 ://user:password@ 格式的认证信息
    return url.replaceAll("://([^:]+):([^@]+)@", "://***:***@");
}
```

**测试用例**:
| 输入 | 输出 |
|------|------|
| `https://example.com/file.tar.gz` | `https://example.com/file.tar.gz` |
| `https://user:pass@example.com/file.tar.gz` | `https://***:***@example.com/file.tar.gz` |
| `http://admin:secret123@internal.corp/agent.tar.gz` | `http://***:***@internal.corp/agent.tar.gz` |

---

## 性能要求

- **日志记录时间**: < 5ms/条（包含时间戳生成和字符串拼接）
- **内存占用**: 日志字符串临时对象，GC自动回收
- **存储**: 日志存储在 `AgentDeployment.deploymentLog` 字段（数据库TEXT类型）

---

## 实现检查清单

- [ ] 所有11种日志场景都有对应的 `appendLog()` 调用
- [ ] 文件大小格式化使用 `Locale.ROOT` 和 `%.2f`
- [ ] URL脱敏方法实现并应用于所有URL日志
- [ ] Unicode状态标记正确显示（✅❌⚠️）
- [ ] 日志消息清晰、可操作、用户友好
- [ ] 单元测试验证所有日志格式
- [ ] 集成测试验证完整日志流程
