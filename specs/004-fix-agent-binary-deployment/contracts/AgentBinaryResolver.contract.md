# Contract: AgentBinaryResolver

**Purpose**: 定义Agent二进制文件获取的行为规范

**实现类**: `AgentDeployService` (增强现有方法)

---

## 方法签名

```java
/**
 * 解析并获取Agent二进制文件（带缓存支持）
 *
 * @param deployment 部署上下文，用于记录日志
 * @return 二进制文件字节数组，失败时返回null
 */
private byte[] resolveAgentBinary(AgentDeployment deployment)
```

---

## 行为契约

### 1. 优先级顺序

**必须按以下顺序尝试获取**:
1. 检查内存缓存（如果启用且URL匹配）
2. 从classpath资源加载（`agent.binary.path`）
3. HTTP/HTTPS下载（`agent.binary.download-url`）
4. 所有方法失败后返回null

### 2. 缓存行为

**前置条件**: `agent.binary.cache-enabled=true` 且 `downloadUrl` 非空

**行为**:
```
Given: 缓存中已存在URL对应的数据
When: 调用 resolveAgentBinary()
Then:
  - 直接返回缓存的字节数组
  - 记录日志 "[上传] 使用已缓存的Agent二进制"
  - 不执行HTTP下载
```

**并发安全**:
```
Given: 两个线程同时调用 resolveAgentBinary() 且缓存为空
When: 第一个线程开始下载
Then:
  - 第二个线程等待第一个线程完成下载
  - 第二个线程从缓存中获取第一个线程下载的数据
  - 仅执行一次HTTP下载
```

### 3. 资源文件加载

**前置条件**: `agent.binary.path` 配置的文件存在于classpath

**行为**:
```
Given: resources/agent/otelcol-linux-amd64.tar.gz 文件存在
When: 调用 resolveAgentBinary()
Then:
  - 读取文件内容到字节数组
  - 记录日志 "[上传] 使用内置Agent包 ({path}, {size} MB)"
  - size格式化为两位小数（例如：42.35 MB）
  - 返回字节数组
```

**失败场景**:
```
Given: 资源文件不存在或读取失败
When: loadBinaryFromResource() 执行
Then:
  - 记录ERROR日志（包含路径和异常信息）
  - 返回null（不抛出异常）
  - 继续尝试下一个方法（HTTP下载）
```

### 4. HTTP下载

**前置条件**: `agent.binary.download-url` 配置非空且格式正确

**成功场景**:
```
Given: URL可访问且返回200-299状态码
When: 调用 downloadAgentBinary()
Then:
  - 记录日志 "[上传] 通过下载地址获取Agent二进制: {url}"
  - 返回HTTP响应体字节数组
  - 记录日志 "[上传] ✅ 下载Agent二进制成功 ({size} MB)"
  - 验证文件（调用validateBinary）
  - 如果验证通过，写入缓存（如果启用）
```

**超时场景**:
```
Given: HTTP请求超过5分钟未完成
When: 下载执行
Then:
  - 抛出 IOException("下载超时（5分钟限制），文件可能过大或网络过慢")
  - 记录ERROR日志（包含URL和异常）
  - 调用方捕获后返回null
```

**HTTP错误场景**:
```
Given: 服务器返回4xx状态码
When: 下载执行
Then:
  - 抛出 IOException("下载失败（文件不存在或无权限），HTTP状态码：{code}")
  - 记录ERROR日志

Given: 服务器返回5xx状态码
When: 下载执行
Then:
  - 抛出 IOException("下载服务器错误，HTTP状态码：{code}，请稍后重试")
  - 记录ERROR日志
```

**连接失败场景**:
```
Given: 无法连接到服务器（ConnectException）
When: 下载执行
Then:
  - 抛出 IOException("无法连接到下载服务器，请检查网络或URL配置")
  - 记录ERROR日志（包含原始异常堆栈）
```

### 5. 返回值约定

**成功返回**:
- `byte[]` - 有效的二进制文件数据
- 数据已通过 `validateBinary()` 验证
- 大小在1MB到100MB之间

**失败返回**:
- `null` - 所有获取方法均失败
- 调用方必须检查null并记录错误日志
- 部署流程应中止

---

## 错误处理契约

### 异常类型

| 异常类型 | 抛出场景 | 处理方式 |
|---------|---------|---------|
| `IOException` | HTTP下载失败 | 捕获后返回null |
| `InterruptedException` | HTTP客户端中断 | 捕获后返回null |
| `IllegalStateException` | 配置验证失败（启动时） | 不捕获，阻止应用启动 |

### 日志级别

| 场景 | 日志级别 | 消息格式 |
|-----|---------|---------|
| 缓存命中 | INFO | `使用已缓存的Agent二进制` |
| 使用资源文件 | INFO | `使用内置Agent包 ({path}, {size} MB)` |
| 开始下载 | INFO | `通过下载地址获取Agent二进制: {url}` |
| 下载成功 | INFO | `✅ 下载Agent二进制成功 ({size} MB)` |
| 下载失败 | ERROR | `下载Agent二进制失败: {errorMessage}` |
| 验证失败 | ERROR | `文件验证失败，大小：{size} bytes` |

---

## 测试用例要求

### 单元测试（必需）

1. **test_resolveFromCache_whenCacheHit_returnsData**
   - 模拟缓存已存在数据
   - 验证直接返回缓存，不调用下载方法

2. **test_resolveFromResource_whenFileExists_returnsData**
   - 模拟资源文件存在
   - 验证读取正确，日志格式正确

3. **test_downloadFromUrl_whenHttp200_returnsData**
   - Mock HttpClient返回200状态码
   - 验证下载成功，数据正确

4. **test_downloadTimeout_whenExceed5Minutes_returnsNull**
   - Mock HttpClient抛出HttpTimeoutException
   - 验证返回null，错误日志包含超时消息

5. **test_downloadHttp404_whenNotFound_returnsNull**
   - Mock返回404状态码
   - 验证错误消息包含"文件不存在或无权限"

6. **test_concurrentDownload_whenMultipleThreads_downloadOnce**
   - 多线程并发调用
   - 验证仅执行一次HTTP下载，其他线程从缓存获取

### 集成测试（可选）

7. **integration_downloadRealUrl_whenGitHubRelease_success**
   - 使用真实的GitHub Release URL
   - 验证端到端下载流程

---

## 依赖合约

### 调用方依赖

**AgentDeployService.uploadFiles()**:
```java
byte[] agentBinary = resolveAgentBinary(deployment);
if (agentBinary == null) {
    logger.error("未能获取Agent二进制文件 - serverId: {}", server.getId());
    deployment.appendLog("[上传] ❌ 未能获取Agent二进制文件，请检查配置");
    return false;
}
// 继续上传流程...
```

### 被调用方依赖

- **BinaryValidator.validateBinary(byte[])** - 文件验证
- **SshFileTransferService.uploadFileBytes()** - 文件上传（无变更）
- **HttpClient** - HTTP下载（java.net.http）

---

## 配置依赖

**必需配置**:
- `agent.binary.path` - 资源文件路径（默认值已提供）

**可选配置**:
- `agent.binary.download-url` - HTTP下载地址（为空时仅使用资源文件）
- `agent.binary.cache-enabled` - 缓存开关（默认true）
- `agent.binary.download-timeout-minutes` - 超时时间（默认5分钟）

**配置验证** (启动时):
- URL格式必须符合正则 `^(https?://.*|)$`
- 超时时间必须在1-30分钟范围内

---

## 性能要求

- **缓存命中**: < 10ms
- **资源文件读取**: < 500ms（40MB文件）
- **HTTP下载**: < 10分钟（40MB文件，取决于网络速度）
- **并发下载**: 10个线程仅执行1次下载，其余从缓存获取

---

## 向后兼容性

**现有方法保持不变**:
- `deployAgent(Server server)` - 公共接口无变更
- `uploadFiles(Server server, AgentDeployment deployment)` - 仅内部逻辑增强

**配置向后兼容**:
- 未配置 `download-url` 时，行为与之前相同（仅使用资源文件）
- 新增配置项有默认值，不影响现有部署

---

## 实现检查清单

- [ ] 实现三层获取逻辑（缓存 → 资源 → 下载）
- [ ] 实现并发下载同步控制
- [ ] 实现文件验证调用
- [ ] 实现分层错误处理
- [ ] 实现详细日志记录（包含文件大小格式化）
- [ ] 单元测试覆盖率 > 80%
- [ ] 集成测试验证真实下载场景
- [ ] 配置验证在启动时执行
- [ ] 日志输出符合格式规范
