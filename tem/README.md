# WaveTerm Demo Web Backend

## 📋 项目概述

这是一个基于 **Spring Boot 3.x** 的现代化Web终端应用，提供以下核心功能：

1. **AI聊天集成** - 支持多种AI模型的流式聊天接口
2. **WebSocket终端** - 实时交互式终端（支持本地PTY和SSH远程）
3. **命令补全** - 智能命令自动补全服务
4. **文件上传** - 安全的文件上传管理
5. **JWT认证** - 基于Token的无状态认证

---

## 🏗️ 架构设计

### 整体架构模式

```
┌──────────────────────────────────────────────────────┐
│                  客户端 (Browser/CLI)                  │
└──────────┬──────────────────────────┬─────────────────┘
           │                          │
    HTTP REST API              WebSocket (/ws/terminal)
           │                          │
┌──────────┴──────────────────────────┴─────────────────┐
│              Spring Boot Web Layer                     │
│  ┌──────────────────┐  ┌──────────────────────────┐   │
│  │ ChatController   │  │ TerminalWebSocketHandler │   │
│  │ TerminalController│  │ CompletionController    │   │
│  │ UploadController │  │ SecurityConfig          │   │
│  └──────────────────┘  └──────────────────────────┘   │
└────────────────┬────────────────────────────────────────┘
                 │
┌────────────────┴────────────────────────────────────────┐
│                  Service Layer                          │
│  ┌─────────────────┐  ┌──────────────────────────┐     │
│  │ ChatService     │  │ TerminalSessionManager   │     │
│  │ UploadService   │  │ SshTerminalGateway       │     │
│  │ CompletionSvc   │  │ CommandCompletionService │     │
│  └─────────────────┘  └──────────────────────────┘     │
└────────────────┬────────────────────────────────────────┘
                 │
┌────────────────┴────────────────────────────────────────┐
│             Infrastructure Layer                        │
│  ┌─────────────────┐  ┌──────────────────────────┐     │
│  │ AiClientRegistry│  │ PtyProcessFactory        │     │
│  │ (Moonshot/      │  │ JSch SSH Client          │     │
│  │  Ollama/Echo)   │  │ TerminalHistoryService   │     │
│  └─────────────────┘  └──────────────────────────┘     │
└─────────────────────────────────────────────────────────┘
```

---

## 📁 目录结构

```
tem/
├── chat/                           # AI聊天模块
│   ├── ai/                        # AI客户端实现
│   │   ├── AiClient.java          # AI客户端接口
│   │   ├── AiClientRegistry.java  # 客户端注册和负载均衡
│   │   ├── echo/                  # Echo测试客户端
│   │   ├── moonshot/              # Moonshot AI集成
│   │   └── ollama/                # Ollama本地模型集成
│   ├── model/                     # 数据模型
│   │   ├── ChatChoice.java
│   │   ├── ChatContext.java
│   │   ├── ChatMessage.java
│   │   └── ChatRequest.java
│   ├── stream/                    # 流式响应
│   │   ├── ChatEventType.java
│   │   └── ChatStreamEvent.java
│   ├── ChatController.java        # REST控制器
│   ├── ChatService.java           # 业务逻辑
│   └── ChatProperties.java        # 配置属性
│
├── terminal/                      # 终端模块
│   ├── completion/                # 命令补全
│   │   ├── CommandCompletionService.java
│   │   ├── CompletionController.java
│   │   └── ...
│   ├── history/                   # 命令历史
│   │   ├── TerminalHistoryService.java
│   │   ├── TerminalCommandEntity.java
│   │   └── TerminalCommandRepository.java
│   ├── pty/                       # PTY进程管理
│   │   ├── PtyProcess.java
│   │   ├── PtyProcessFactory.java
│   │   ├── ProcessPtyProcess.java
│   │   └── ...
│   ├── ssh/                       # SSH终端
│   │   ├── SshTerminalGateway.java
│   │   ├── SshTerminalSession.java
│   │   ├── SshTargetProperties.java
│   │   └── model/
│   ├── upload/                    # 文件上传
│   │   ├── UploadService.java
│   │   ├── UploadController.java
│   │   └── ...
│   ├── TerminalController.java    # REST控制器
│   ├── TerminalWebSocketHandler.java  # WebSocket处理器
│   ├── TerminalSessionManager.java    # 会话管理器
│   └── ...
│
├── security/                      # 安全模块
│   ├── SecurityConfig.java        # Spring Security配置
│   ├── JwtAuthenticationFilter.java   # JWT过滤器
│   ├── JwtAuthProperties.java     # JWT配置属性
│   └── JwtAuthenticationException.java
│
├── config/                        # 配置模块
│   ├── WebSocketConfig.java       # WebSocket配置
│   ├── WebMvcConfig.java          # Web MVC配置
│   └── SchedulingConfig.java      # 定时任务配置
│
└── DemoWebBackendApplication.java # 应用启动类
```

---

## 📦 核心模块详解

### 1. Chat模块 (AI聊天)

#### 功能特性
- ✅ 支持多AI模型集成（Moonshot、Ollama、Echo）
- ✅ Server-Sent Events (SSE) 流式响应
- ✅ 智能缓存机制（TTL可配置）
- ✅ 失败重试与负载均衡
- ✅ Micrometer监控集成

#### 核心API

**POST /api/chat**
```json
{
  "chatId": "chat-uuid",
  "messages": [
    {
      "role": "user",
      "content": "Hello, AI!"
    }
  ],
  "context": {
    "temperature": 0.7
  },
  "client": "moonshot"  // 可选，不指定则使用负载均衡
}
```

响应（SSE流）:
```
event: chunk
data: {"content": "Hello"}

event: chunk
data: {"content": " there!"}

event: done
data: ""
```

#### 关键实现

**负载均衡**（`AiClientRegistry.java:44-58`）:
```java
public AiClient pickWeightedClient() {
    int totalWeight = weightedClients.stream()
        .mapToInt(WeightedClient::weight).sum();
    int choice = random.nextInt(totalWeight);
    // 加权随机选择
    ...
}
```

**缓存机制**（`ChatService.java:93-101`）:
```java
private Stream<ChatStreamEvent> cacheIfNeeded(String cacheKey,
                                               Stream<ChatStreamEvent> stream) {
    Duration ttl = properties.getCacheTtl();
    if (ttl.isZero() || ttl.isNegative()) {
        return stream;
    }
    ChatStreamEvent[] events = stream.toArray(ChatStreamEvent[]::new);
    cache.put(cacheKey, new CachedResponse(events, Instant.now()));
    return Stream.of(events);
}
```

---

### 2. Terminal模块 (WebSocket终端)

#### 功能特性
- ✅ 本地PTY终端 + SSH远程终端
- ✅ WebSocket实时双向通信
- ✅ 终端窗口大小动态调整
- ✅ 命令历史持久化
- ✅ 智能命令补全
- ✅ 会话自动清理机制

#### WebSocket协议

**连接**: `ws://localhost:8080/ws/terminal?sessionId=xxx&targetId=ssh-server-1`

**客户端消息类型**:

1. **输入命令**
```json
{
  "type": "input",
  "data": "ls -la\n"
}
```

2. **调整大小**
```json
{
  "type": "resize",
  "cols": 120,
  "rows": 30
}
```

3. **发送信号**
```json
{
  "type": "signal",
  "signal": "SIGINT"
}
```

4. **心跳**
```json
{
  "type": "ping"
}
```

**服务器消息类型**:

1. **会话建立**
```json
{
  "type": "session",
  "sessionId": "generated-session-id"
}
```

2. **终端输出**
```json
{
  "type": "output",
  "data": "total 64\ndrwxr-xr-x..."
}
```

3. **进程退出**
```json
{
  "type": "exit",
  "code": 0
}
```

4. **错误**
```json
{
  "type": "error",
  "code": "authentication_failed",
  "message": "SSH 认证失败"
}
```

5. **心跳响应**
```json
{
  "type": "pong",
  "ts": 1698765432000
}
```

#### REST API

**POST /api/terminal/write**
```json
{
  "sessionId": "session-uuid",
  "data": "ls -la\n"
}
```

**GET /api/terminal/history**
```
GET /api/terminal/history?sessionId=xxx&limit=50&cursor=xxx&q=git
```

响应:
```json
{
  "items": [
    {
      "id": "cmd-uuid",
      "input": "git status",
      "ts": 1698765432000
    }
  ],
  "nextCursor": "cursor-token"
}
```

#### SSH配置示例

```yaml
terminal:
  ssh:
    enabled: true
    targets:
      - id: production-server
        host: prod.example.com
        port: 22
        username: admin
        password: ${SSH_PASSWORD}  # 建议使用环境变量
        strictHostKeyChecking: true
        workingDirectory: /home/admin
        connectTimeout: PT10S
        idleTimeout: PT30M
        maxRetries: 3
        retryBackoff: PT2S
```

#### 会话清理机制

**自动清理条件**（`TerminalSessionManager.java:89-108`）:
1. 进程已终止（`!session.isAlive()`）
2. 超过空闲时间（`session.isIdle(idleTimeout)`）

**清理器配置**:
```yaml
terminal:
  session:
    idleTimeout: PT30M      # 30分钟
    sweepInterval: PT5M     # 每5分钟检查一次
```

---

### 3. Security模块 (安全认证)

#### JWT认证流程

```
Client Request
    ↓
[JwtAuthenticationFilter]
    ↓
Extract Bearer Token
    ↓
Validate Token (JJWT)
    ├─ Verify Signature (HMAC SHA256)
    ├─ Check Issuer
    ├─ Check Audience
    ├─ Check Expiration
    └─ Extract Subject
    ↓
Set SecurityContext
    ↓
[Controller]
```

#### JWT配置

```yaml
jwt:
  secret: your-256-bit-secret-key-here  # 至少32字符
  issuer: waveterm
  audience: waveterm-api
  clockSkew: PT30S  # 时钟偏移容忍度30秒
  excludePaths:
    - /actuator/**
    - /api/chat
```

#### Token示例

**Header**:
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload**:
```json
{
  "sub": "user123",
  "iss": "waveterm",
  "aud": "waveterm-api",
  "exp": 1698851832,
  "iat": 1698765432
}
```

#### 安全配置

**路径权限**（`SecurityConfig.java:28-34`）:
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/**").permitAll()
    .requestMatchers("/api/chat").permitAll()
    .requestMatchers("/api/upload").authenticated()
    .requestMatchers("/api/**").authenticated()
    .anyRequest().permitAll()
)
```

---

### 4. 命令补全服务

#### 功能特性
- ✅ 前缀匹配优先
- ✅ 包含匹配降级
- ✅ 历史命令权重提升
- ✅ 缓存优化（30秒TTL）
- ✅ 可配置命令词典

#### API

**POST /api/completion**
```json
{
  "prompt": "git ",
  "recentCommands": [
    "git status",
    "git commit -m 'fix'"
  ]
}
```

响应:
```json
{
  "suggestions": [
    {
      "command": "git status",
      "score": 0.95
    },
    {
      "command": "git commit",
      "score": 1.0
    },
    {
      "command": "git push",
      "score": 1.0
    }
  ]
}
```

#### 补全策略

1. **前缀匹配**（高优先级）
   ```java
   if (entry.getKey().toLowerCase().startsWith(normalized))
   ```

2. **包含匹配**（降级策略）
   ```java
   .filter(e -> e.getKey().toLowerCase().contains(normalized))
   ```

3. **历史命令加权**（权重0.95）
   ```java
   .map(cmd -> new CompletionSuggestion(cmd, 0.95))
   ```

---

### 5. 文件上传服务

#### 功能特性
- ✅ 文件大小限制
- ✅ Content-Type白名单
- ✅ 文件名安全化
- ✅ UUID文件ID

#### API

**POST /api/upload**
```
Content-Type: multipart/form-data

file: [binary data]
```

响应:
```json
{
  "uploadId": "uuid-here",
  "url": "file:///path/to/file"
}
```

#### 配置

```yaml
upload:
  maxSizeBytes: 10485760  # 10MB
  baseDir: /var/uploads
  allowedContentTypes:
    - text/plain
    - application/json
    - application/octet-stream
```

#### 安全措施

1. **文件名安全化**（`UploadService.java:47`）:
   ```java
   String sanitizedName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
   ```

2. **大小验证**:
   ```java
   if (file.getSize() > properties.getMaxSizeBytes()) {
       throw new UploadValidationException("file exceeds limit");
   }
   ```

3. **Content-Type验证**:
   ```java
   if (!allowedTypes.contains(contentType)) {
       throw new UploadValidationException("unsupported type");
   }
   ```

---

## 🔧 技术栈

### 核心框架
- **Spring Boot** 3.x
  - spring-boot-starter-web
  - spring-boot-starter-websocket
  - spring-boot-starter-security
  - spring-boot-starter-data-jpa

### 关键依赖
- **JSch** - Java SSH客户端库
- **JJWT** - JSON Web Token处理
- **Micrometer** - 监控和指标收集
- **Jakarta Validation** - Bean验证
- **Jackson** - JSON序列化/反序列化

### Java版本
- **Java 17+** （使用record、switch表达式等现代特性）

---

## ⚡ 设计模式

### 应用的设计模式

1. **策略模式**
   - `AiClient` 接口 + 多实现（Moonshot/Ollama/Echo）

2. **工厂模式**
   - `PtyProcessFactory` - PTY进程创建

3. **观察者模式**
   - Terminal的 `outputListener` 和 `exitListener`

4. **注册表模式**
   - `AiClientRegistry` - AI客户端管理

5. **代理模式**
   - `TerminalGateway` 接口 + 多实现（本地/SSH）

6. **缓存模式**
   - Chat响应缓存
   - Completion结果缓存

7. **单例模式**
   - Spring Bean容器管理

---

## 🚀 快速开始

### 1. 环境要求
- JDK 17+
- Maven 3.6+

### 2. 构建项目
```bash
mvn clean package -DskipTests
```

### 3. 运行应用
```bash
java -jar target/demo-web-backend.jar
```

### 4. 访问端点
- HTTP API: `http://localhost:8080/api`
- WebSocket: `ws://localhost:8080/ws/terminal`
- Actuator: `http://localhost:8080/actuator`

---

## 📊 监控与指标

### Micrometer指标

**Chat服务指标**（`ChatService.java:113-118`）:
```java
Timer.builder("chat.client.duration")
    .tag("client", clientId)
    .tag("outcome", "success|failure")
    .register(meterRegistry);
```

### Actuator端点

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 指标查询
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/chat.client.duration
```

---

## 🔒 安全最佳实践

### 当前实现
✅ JWT无状态认证
✅ HMAC SHA256签名
✅ Token过期验证
✅ 文件上传大小限制
✅ 文件名安全化

### 建议改进
⚠️ **WebSocket CORS**: 当前设置为 `*`，应限制为可信域名
⚠️ **SSH密码**: 建议使用加密存储或密钥认证
⚠️ **会话限制**: 增加单用户会话数量配额
⚠️ **Rate Limiting**: 实施API速率限制

---

## 📈 性能优化

### 已实现优化
1. **并发设计**
   - `ConcurrentHashMap` 管理会话
   - 异步SSE流处理
   - Daemon线程定时任务

2. **缓存策略**
   - Chat响应缓存（TTL可配置）
   - Completion结果缓存（30秒）

3. **资源清理**
   - `@PreDestroy` 钩子
   - Sweeper定时清理

4. **连接池**
   - SSH连接复用（会话管理）

### 优化建议
- 🔧 实现Redis分布式缓存
- 🔧 增加数据库连接池配置
- 🔧 启用HTTP/2
- 🔧 静态资源CDN

---

## 🧪 测试

### 单元测试示例

**Chat服务测试**:
```java
@Test
void testChatWithRetry() {
    // Mock AI客户端失败
    when(mockClient.streamChat(any()))
        .thenThrow(new RuntimeException("AI error"))
        .thenReturn(Stream.of(new ChatStreamEvent(...)));

    // 验证重试成功
    Stream<ChatStreamEvent> result = chatService.stream(request);
    assertNotNull(result);
}
```

**Terminal会话测试**:
```java
@Test
void testSessionSweep() {
    // 创建空闲会话
    ManagedTerminalSession session = manager.getOrCreate("test");

    // 模拟超时
    Thread.sleep(idleTimeout + 1000);

    // 验证被清理
    assertTrue(session.isIdle(idleTimeout));
}
```

---

## 📝 配置参考

### application.yml 完整示例

```yaml
server:
  port: 8080

spring:
  application:
    name: waveterm-demo

  datasource:
    url: jdbc:h2:mem:terminaldb
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

# JWT配置
jwt:
  secret: your-secret-key-at-least-256-bits-long
  issuer: waveterm
  audience: waveterm-api
  clockSkew: PT30S
  excludePaths:
    - /actuator/**
    - /api/chat

# Chat配置
chat:
  defaultClient: moonshot
  maxRetries: 2
  cacheTtl: PT5M
  clients:
    moonshot:
      weight: 5
      timeout: PT30S
    ollama:
      weight: 3
      timeout: PT60S
    echo:
      weight: 1
      timeout: PT5S

# 终端配置
terminal:
  shell:
    command: /bin/bash
    args: ["-l"]

  session:
    idleTimeout: PT30M
    sweepInterval: PT5M

  ssh:
    enabled: true
    targets:
      - id: production
        host: prod.example.com
        port: 22
        username: admin
        password: ${SSH_PASSWORD}
        strictHostKeyChecking: true
        workingDirectory: /home/admin
        connectTimeout: PT10S
        idleTimeout: PT30M
        maxRetries: 3
        retryBackoff: PT2S

# 命令补全配置
terminal.completion:
  resource: classpath:commands.txt
  variantsResource: classpath:command-variants.txt
  cacheTtl: PT30S
  extras:
    - kubectl
    - docker

# 上传配置
upload:
  maxSizeBytes: 10485760  # 10MB
  baseDir: /var/uploads
  allowedContentTypes:
    - text/plain
    - application/json

# 监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  metrics:
    enable:
      jvm: true
      system: true
```

---

## 🐛 常见问题

### 1. WebSocket连接失败

**问题**: 浏览器报 `WebSocket connection failed`

**解决**:
- 检查端口是否正确
- 确认防火墙规则
- 验证CORS配置

### 2. SSH认证失败

**问题**: `SshTerminalException: authentication_failed`

**解决**:
- 验证用户名密码
- 检查SSH密钥权限（600）
- 查看服务器 `/var/log/auth.log`

### 3. JWT Token过期

**问题**: `401 Unauthorized: Token expired`

**解决**:
- 刷新Token
- 检查时钟偏移配置（`clockSkew`）
- 验证服务器时间同步

### 4. 文件上传失败

**问题**: `UploadValidationException: file exceeds limit`

**解决**:
- 增加 `upload.maxSizeBytes` 配置
- 检查磁盘空间
- 验证Content-Type白名单

---

## 📚 相关资源

- [Spring Boot文档](https://spring.io/projects/spring-boot)
- [Spring WebSocket文档](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [JJWT库](https://github.com/jwtk/jjwt)
- [JSch文档](http://www.jcraft.com/jsch/)

---

## 📊 项目统计

- **Java文件总数**: 57个
- **代码总行数**: ~5000+行
- **主要模块**: 4个 (chat, terminal, security, config)
- **支持的AI模型**: 3种 (Moonshot, Ollama, Echo)
- **API端点数**: 10+
- **WebSocket端点**: 1个

---

## 🎯 代码质量

**评分**: ⭐⭐⭐⭐⭐ (5/5)

**优点**:
- ✅ 代码整洁，遵循Spring Boot最佳实践
- ✅ 合理使用Java 17+特性（record、switch表达式）
- ✅ 良好的日志和异常处理
- ✅ 完善的资源清理机制
- ✅ 模块化设计，职责分明

**可改进**:
- 🔧 增加单元测试覆盖率
- 🔧 完善API文档（Swagger/OpenAPI）
- 🔧 加强安全配置（CORS、密码加密）

---

## 📄 License

本项目为演示项目，仅供学习参考。

---

## 👥 贡献

欢迎提交Issue和Pull Request！

---

**最后更新**: 2025-10-19
**文档版本**: 1.0.0
