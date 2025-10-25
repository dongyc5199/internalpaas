# Research: Agent Binary Deployment Enhancement

**Feature**: 004-fix-agent-binary-deployment
**Date**: 2025-10-25
**Purpose**: 解决技术实现中的未明确点，为Phase 1设计提供决策依据

---

## 1. HTTP下载最佳实践

### 决策：使用Java 11+ HttpClient with Configuration

**选择的方案**:
```java
HttpClient httpClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)  // 最多5次重定向
    .connectTimeout(Duration.ofSeconds(30))        // 连接超时30秒
    .build();

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(downloadUrl))
    .timeout(Duration.ofMinutes(5))                // 请求超时5分钟
    .GET()
    .build();

HttpResponse<byte[]> response = httpClient.send(
    request,
    HttpResponse.BodyHandlers.ofByteArray()
);
```

**理由**:
1. **自Java 11内置**：无需额外依赖，已在项目中可用（Java 17）
2. **自动重定向支持**：`NORMAL`模式支持HTTP 301/302/303/307/308，最多5次重定向
3. **超时控制**：分离连接超时（30秒）和整体请求超时（5分钟），满足规格要求
4. **内存友好**：`ofByteArray()` 适用于<100MB文件，一次性加载到内存

**考虑的替代方案**:
- **Apache HttpClient**: 更成熟但引入额外依赖，与项目"轻量级"原则不符
- **OkHttp**: 优秀的库但非标准库，增加维护成本
- **RestTemplate**: Spring提供但已被WebClient取代，且不如HttpClient灵活

**实施要点**:
- 使用 `Duration` API清晰表达超时配置
- 捕获 `HttpTimeoutException` 单独处理超时场景
- 检查响应状态码，2xx范围视为成功

---

## 2. 文件验证策略

### 决策：多层次验证（文件大小 + 魔数 + 基本格式检查）

**验证流程**:
```java
private boolean validateBinary(byte[] data) {
    // 1. 最小文件大小检查（至少1MB，避免错误页）
    if (data.length < 1_048_576) {
        logger.error("二进制文件过小 ({} bytes)，可能不是有效的tar.gz文件", data.length);
        return false;
    }

    // 2. Gzip魔数检查（前两个字节：0x1f, 0x8b）
    if (data.length < 2 || data[0] != 0x1f || (data[1] & 0xff) != 0x8b) {
        logger.error("文件签名不匹配，不是有效的gzip文件");
        return false;
    }

    // 3. 文件大小上限检查（防止异常大文件）
    if (data.length > 104_857_600) { // 100 MB
        logger.error("文件过大 ({} MB)，超过100MB限制", data.length / 1_048_576.0);
        return false;
    }

    return true;
}
```

**理由**:
1. **文件大小过滤**：HTML错误页通常<1MB，OpenTelemetry Collector约20-50MB
2. **魔数校验**：Gzip文件固定以`0x1f 0x8b`开头，可快速识别格式
3. **上限保护**：防止恶意文件或配置错误导致内存溢出

**考虑的替代方案**:
- **SHA256校验和**: 需要预先知道正确的哈希值，不适用于动态URL
- **完整tar.gz解压验证**: 耗时且复杂，不必要（安装脚本会最终验证）
- **仅依赖文件扩展名**: 极不可靠，容易被误导

**边缘情况处理**:
- **HTML错误页（404/500）**: 被最小大小检查拦截
- **部分下载**：被魔数检查拦截（不完整gzip头）
- **错误格式压缩包**: 基本验证通过，由安装脚本的tar命令最终拒绝

---

## 3. 并发部署场景

### 决策：内存缓存 + 同步控制（单JVM实例）

**缓存实现**:
```java
private final Map<String, byte[]> binaryCache = new ConcurrentHashMap<>();
private final Object downloadLock = new Object();

private byte[] resolveAgentBinaryWithCache(AgentDeployment deployment) {
    String cacheKey = agentBinaryDownloadUrl;

    // 1. 检查缓存
    byte[] cached = binaryCache.get(cacheKey);
    if (cached != null) {
        deployment.appendLog("[上传] 使用已缓存的Agent二进制");
        return cached;
    }

    // 2. 同步下载（避免并发重复下载）
    synchronized (downloadLock) {
        // 双重检查（可能其他线程已下载）
        cached = binaryCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 3. 执行下载
        byte[] downloaded = downloadAgentBinary();
        if (downloaded != null && validateBinary(downloaded)) {
            binaryCache.put(cacheKey, downloaded);
        }
        return downloaded;
    }
}
```

**理由**:
1. **内存缓存**: 40MB文件×10并发=400MB内存占用，可接受（规格要求200MB可用）
2. **ConcurrentHashMap**: 线程安全，支持并发读取缓存
3. **同步下载**: 使用锁确保URL只下载一次，避免网络资源浪费
4. **双重检查**: 减少锁竞争，第二线程可直接使用第一线程的下载结果

**考虑的替代方案**:
- **磁盘缓存**: 增加I/O开销，需管理文件清理，复杂度高
- **分布式缓存（Redis）**: 过度设计，单JVM场景下不必要
- **无缓存**: 每次部署都下载，浪费带宽和时间

**缓存失效策略**:
- **JVM重启清空**: 简单有效，下次启动重新下载（配置可能已更新）
- **无TTL**: 运行期间永久缓存（URL变化会自动更新）
- **手动清理**: 管理员可通过重启应用清除缓存

**并发安全性**:
- **读多写少**: ConcurrentHashMap优化了读性能
- **synchronized块**: 仅在缓存未命中时使用，影响有限
- **原子性**: 下载+验证+缓存是一个原子操作

---

## 4. 错误处理模式

### 决策：分层错误消息（技术层 + 用户层）

**错误分类与消息映射**:

| 错误类型 | HTTP状态码/异常 | 用户友好消息 | 技术日志 |
|---------|----------------|------------|---------|
| 未配置URL | N/A | "Agent二进制未打包且未配置下载地址，请设置 agent.binary.download-url" | WARN: agentBinaryDownloadUrl is empty |
| 无效URL格式 | N/A | "下载地址格式错误：[URL]，仅支持HTTP/HTTPS协议" | ERROR: Invalid URL format |
| 连接超时 | ConnectException | "无法连接到下载服务器，请检查网络或URL配置" | ERROR: Connection timeout after 30s |
| 请求超时 | HttpTimeoutException | "下载超时（5分钟限制），文件可能过大或网络过慢" | ERROR: Request timeout after 300s |
| HTTP 4xx | 400-499 | "下载失败（文件不存在或无权限），HTTP状态码：[code]" | ERROR: HTTP client error [code] |
| HTTP 5xx | 500-599 | "下载服务器错误，HTTP状态码：[code]，请稍后重试" | ERROR: HTTP server error [code] |
| 文件验证失败 | N/A | "下载的文件无效（可能是错误页或损坏文件），大小：[size]" | ERROR: Binary validation failed, magic number mismatch |
| 未知错误 | Exception | "下载失败：[异常消息]" | ERROR: Unexpected error during download [stacktrace] |

**实现示例**:
```java
private byte[] downloadAgentBinary() throws IOException, InterruptedException {
    try {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(agentBinaryDownloadUrl))
            .timeout(Duration.ofMinutes(5))
            .GET()
            .build();

        HttpResponse<byte[]> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofByteArray());

        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response.body();
        } else if (status >= 400 && status < 500) {
            throw new IOException("下载失败（文件不存在或无权限），HTTP状态码：" + status);
        } else {
            throw new IOException("下载服务器错误，HTTP状态码：" + status + "，请稍后重试");
        }

    } catch (HttpTimeoutException e) {
        logger.error("Agent二进制下载超时 - url: {}", agentBinaryDownloadUrl, e);
        throw new IOException("下载超时（5分钟限制），文件可能过大或网络过慢", e);
    } catch (ConnectException e) {
        logger.error("无法连接到下载服务器 - url: {}", agentBinaryDownloadUrl, e);
        throw new IOException("无法连接到下载服务器，请检查网络或URL配置", e);
    }
}
```

**理由**:
1. **用户友好**: 非技术人员能理解问题和可能的解决方案
2. **技术日志**: 开发人员有足够信息调试（包含URL、状态码、异常堆栈）
3. **可操作性**: 消息指向具体的修复动作（检查网络、修改配置、稍后重试）

**部署日志格式**:
```
[上传] 通过下载地址获取Agent二进制: https://example.com/otelcol.tar.gz
[上传] ✅ 下载Agent二进制成功 (42.35 MB)
[上传] ✅ 文件验证通过
[上传] 上传Agent二进制文件 (42.35 MB)...
[上传] ✅ Agent二进制文件上传成功
```

**错误日志格式**:
```
[上传] 通过下载地址获取Agent二进制: https://example.com/otelcol.tar.gz
[上传] ❌ 下载Agent二进制失败: 下载超时（5分钟限制），文件可能过大或网络过慢
[上传] ❌ 未能获取Agent二进制文件，请检查配置
```

---

## 5. 配置管理

### 决策：Spring配置属性 + 启动时验证

**配置定义**（application.properties）:
```properties
# Agent二进制下载配置
agent.binary.download-url=https://github.com/open-telemetry/opentelemetry-collector-releases/releases/download/v0.91.0/otelcol-contrib_0.91.0_linux_amd64.tar.gz
agent.binary.download-timeout-minutes=5
agent.binary.cache-enabled=true
```

**Java配置类**:
```java
@Component
@ConfigurationProperties(prefix = "agent.binary")
@Validated
public class AgentBinaryConfig {

    @Pattern(regexp = "^(https?://.*|)$", message = "仅支持HTTP/HTTPS协议")
    private String downloadUrl = "";

    @Min(1)
    @Max(30)
    private int downloadTimeoutMinutes = 5;

    private boolean cacheEnabled = true;

    @PostConstruct
    public void validateConfig() {
        if (StringUtils.hasText(downloadUrl)) {
            try {
                URI.create(downloadUrl); // 验证URL格式
                logger.info("Agent二进制下载已配置: {}", downloadUrl);
            } catch (IllegalArgumentException e) {
                logger.error("Agent二进制下载URL格式错误: {}", downloadUrl);
                throw new IllegalStateException("配置错误：agent.binary.download-url格式无效", e);
            }
        } else {
            logger.warn("Agent二进制下载URL未配置，将仅使用打包的二进制文件");
        }
    }
}
```

**理由**:
1. **Spring标准**: 使用@ConfigurationProperties，支持类型安全和验证
2. **启动时验证**: `@PostConstruct`确保应用启动时就发现配置错误
3. **正则验证**: `@Pattern`限制仅HTTP/HTTPS协议，阻止file://等不安全协议
4. **默认值**: 未配置时不报错，仅警告，回退到打包文件方案

**考虑的替代方案**:
- **@Value注解**: 简单但缺少类型安全和集中验证
- **Environment直接读取**: 灵活但无验证，易出错
- **外部配置中心（Nacos/Apollo）**: 过度设计，当前需求不需要动态刷新

**敏感信息处理**（含认证的URL）:
```properties
# 方案1：基本认证嵌入URL（不推荐生产环境）
agent.binary.download-url=https://user:password@internal.example.com/otelcol.tar.gz

# 方案2：使用环境变量（推荐）
agent.binary.download-url=${AGENT_BINARY_URL}

# 方案3：使用Spring加密属性（需jasypt依赖）
agent.binary.download-url=ENC(encrypted_url_here)
```

**生产环境建议**:
- 使用环境变量或Kubernetes Secret注入配置
- 避免将认证信息硬编码在application.properties
- 日志输出时脱敏URL中的认证部分

**配置热更新**（暂不实施）:
- 当前方案：需重启应用生效
- 未来增强：结合`@RefreshScope`实现无重启更新（需Spring Cloud Config）

---

## 研究总结

### 关键决策一览

| 研究领域 | 决策 | 主要理由 |
|---------|------|---------|
| HTTP下载 | Java HttpClient + 5分钟超时 + 自动重定向 | 内置库、零依赖、满足规格要求 |
| 文件验证 | 大小检查 + Gzip魔数 + 100MB上限 | 快速、可靠、覆盖主要错误场景 |
| 并发缓存 | ConcurrentHashMap内存缓存 + synchronized下载 | 简单、高效、适合单JVM场景 |
| 错误处理 | 分层消息（用户友好 + 技术日志） | 可操作性强、调试友好 |
| 配置管理 | @ConfigurationProperties + 启动验证 | Spring标准、类型安全、早期发现错误 |

### 未解决问题（标记为未来增强）

1. **断点续传**: 当前不支持，网络中断需重新下载（可通过HTTP Range请求实现）
2. **多镜像源**: 单一URL，无CDN或镜像源failover（可引入URL列表配置）
3. **签名验证**: 无加密签名校验，依赖HTTPS保证传输安全（可引入SHA256校验）
4. **配置热更新**: 需重启应用生效（可集成Spring Cloud Config实现）

### Phase 1准备就绪

所有技术未明确点已解决，现在可以进入Phase 1进行详细设计：
- ✅ HTTP下载实现方案明确
- ✅ 文件验证逻辑定义
- ✅ 并发安全策略确定
- ✅ 错误处理模式建立
- ✅ 配置管理方案清晰

下一步：生成数据模型（data-model.md）和API合约（contracts/）。
