# Data Model: Agent Binary Deployment Enhancement

**Feature**: 004-fix-agent-binary-deployment
**Date**: 2025-10-25
**Source**: 从spec.md的功能需求和research.md的技术决策中提取

---

## 概述

本功能主要涉及**配置数据**和**运行时状态数据**，不引入新的持久化实体。数据流动如下：

```
配置文件 → AgentBinaryConfig → AgentDeployService → 下载/缓存 → 部署日志
```

---

## 1. 配置实体

### 1.1 AgentBinaryConfig（配置类）

**用途**: 封装Agent二进制文件获取相关的配置参数

**属性**:

| 字段名 | 类型 | 必需 | 默认值 | 描述 | 验证规则 |
|-------|------|------|--------|------|----------|
| `downloadUrl` | String | 否 | `""` | HTTP/HTTPS下载地址 | 正则：`^(https?://.*\|)$` |
| `downloadTimeoutMinutes` | int | 是 | `5` | 下载超时时间（分钟） | 范围：1-30 |
| `cacheEnabled` | boolean | 是 | `true` | 是否启用内存缓存 | N/A |
| `binaryPath` | String | 是 | `agent/otelcol-linux-amd64.tar.gz` | 资源路径 | 非空 |

**关系**:
- 被 `AgentDeployService` 依赖注入使用
- 通过 `application.properties` 的 `agent.binary.*` 前缀配置

**状态转换**: 无（只读配置）

**验证规则**:
- `downloadUrl` 为空时允许（回退到打包文件方案）
- `downloadUrl` 非空时必须是合法的HTTP/HTTPS URL
- `downloadTimeoutMinutes` 必须在合理范围内，防止过长或过短

**示例数据**:
```java
AgentBinaryConfig {
    downloadUrl = "https://github.com/open-telemetry/opentelemetry-collector-releases/releases/download/v0.91.0/otelcol-contrib_0.91.0_linux_amd64.tar.gz",
    downloadTimeoutMinutes = 5,
    cacheEnabled = true,
    binaryPath = "agent/otelcol-linux-amd64.tar.gz"
}
```

---

## 2. 运行时数据结构

### 2.1 BinaryCache（内存缓存）

**用途**: 在JVM运行期缓存已下载的二进制文件，避免重复下载

**数据结构**:
```java
ConcurrentHashMap<String, byte[]>
  Key: downloadUrl (String)
  Value: binaryData (byte[])
```

**特性**:
- **线程安全**: 使用 `ConcurrentHashMap` 支持并发读取
- **生命周期**: 与JVM进程绑定，重启后清空
- **内存占用**: 约40MB/条目（典型Agent二进制大小）
- **失效策略**: 无TTL，通过JVM重启清理

**操作**:
- **读取**: `cache.get(url)` - 无锁并发读
- **写入**: `cache.put(url, data)` - 在synchronized块内执行
- **清空**: JVM重启自动清空

**并发控制**:
- 使用 `synchronized(downloadLock)` 确保同一URL仅下载一次
- 双重检查模式避免重复下载

---

## 3. 现有实体增强

### 3.1 AgentDeployment（现有实体，增强字段）

**用途**: 记录Agent部署过程的日志和状态

**新增日志格式**:
```
[上传] 通过下载地址获取Agent二进制: {url}
[上传] ✅ 下载Agent二进制成功 ({size} MB)
[上传] ✅ 文件验证通过
[上传] ❌ 下载Agent二进制失败: {errorMessage}
[上传] 使用已缓存的Agent二进制
[上传] 使用内置Agent包 ({path}, {size} MB)
```

**无需修改数据库Schema**: 日志通过 `appendLog(String message)` 方法追加到现有的日志字段

---

## 4. 错误响应数据结构

### 4.1 下载错误类型

**枚举**: `BinaryAcquisitionError`（逻辑层枚举，非持久化）

| 错误码 | 英文名称 | 用户消息 | HTTP状态码关联 |
|-------|---------|---------|--------------|
| `NOT_CONFIGURED` | Not Configured | Agent二进制未打包且未配置下载地址 | N/A |
| `INVALID_URL` | Invalid URL | 下载地址格式错误，仅支持HTTP/HTTPS | N/A |
| `CONNECTION_TIMEOUT` | Connection Timeout | 无法连接到下载服务器 | N/A (ConnectException) |
| `REQUEST_TIMEOUT` | Request Timeout | 下载超时（5分钟限制） | N/A (HttpTimeoutException) |
| `HTTP_CLIENT_ERROR` | HTTP 4xx | 文件不存在或无权限 | 400-499 |
| `HTTP_SERVER_ERROR` | HTTP 5xx | 下载服务器错误 | 500-599 |
| `VALIDATION_FAILED` | Validation Failed | 下载的文件无效（损坏或错误格式） | N/A |
| `UNKNOWN_ERROR` | Unknown Error | 未知下载错误 | N/A |

**使用方式**:
```java
try {
    byte[] binary = downloadAgentBinary();
} catch (IOException e) {
    deployment.appendLog("[上传] ❌ 下载失败: " + e.getMessage());
    logger.error("二进制下载失败 - url: {}", downloadUrl, e);
    return null;
}
```

---

## 5. 数据流图

```
┌─────────────────────────────────────────────────────────────┐
│ application.properties                                      │
│  - agent.binary.download-url                                │
│  - agent.binary.download-timeout-minutes                    │
│  - agent.binary.cache-enabled                               │
└───────────────────────┬─────────────────────────────────────┘
                        │ 启动时加载
                        ▼
              ┌──────────────────────┐
              │ AgentBinaryConfig    │
              │  (Spring Bean)       │
              └──────────┬───────────┘
                         │ 依赖注入
                         ▼
              ┌──────────────────────┐
              │ AgentDeployService   │
              └──────────┬───────────┘
                         │ 部署触发
                         ▼
       ┌─────────────────────────────────────┐
       │ resolveAgentBinaryWithCache()       │
       └──────┬──────────────────────────────┘
              │
    ┌─────────┴──────────┐
    │                    │
    ▼                    ▼
┌────────────┐    ┌──────────────┐
│ 内存缓存   │    │ 资源文件读取 │
│ (命中)     │    │ (打包二进制) │
└────────────┘    └──────┬───────┘
    │                    │ 不存在
    │                    ▼
    │             ┌──────────────┐
    │             │ HTTP下载     │
    │             └──────┬───────┘
    │                    │
    │                    ▼
    │             ┌──────────────┐
    │             │ 文件验证     │
    │             │ (魔数/大小)  │
    │             └──────┬───────┘
    │                    │ 成功
    │                    ▼
    │             ┌──────────────┐
    │             │ 写入缓存     │
    │             └──────┬───────┘
    └─────────────────────┘
              │
              ▼
    ┌──────────────────┐
    │ uploadFileBytes()│
    │ (SSH传输到服务器) │
    └──────────────────┘
              │
              ▼
    ┌──────────────────┐
    │ AgentDeployment  │
    │ .appendLog(...)  │
    └──────────────────┘
```

---

## 6. 存储需求

### 6.1 内存存储

**缓存容量估算**:
- 单个二进制文件: 40 MB（典型值）
- 最大并发部署: 10个服务器
- 缓存去重后: 1个条目（相同URL共享缓存）
- **总内存需求**: ~40 MB（单JVM实例）

**内存安全检查**:
- 规格要求: 200 MB可用空间
- 实际占用: 40 MB
- **安全边际**: 5倍余量 ✅

### 6.2 磁盘存储

**不需要额外磁盘存储**:
- 下载的二进制直接保存在内存（byte数组）
- 通过SSH上传到远程服务器后，本地内存数据不持久化
- 缓存在JVM堆内，无磁盘文件

---

## 7. 数据完整性与安全

### 7.1 验证规则

**启动时验证** (AgentBinaryConfig):
```java
@PostConstruct
public void validateConfig() {
    if (StringUtils.hasText(downloadUrl)) {
        if (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://")) {
            throw new IllegalStateException("仅支持HTTP/HTTPS协议");
        }
        try {
            URI.create(downloadUrl); // 格式验证
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("URL格式无效", e);
        }
    }
}
```

**运行时验证** (下载后):
```java
private boolean validateBinary(byte[] data) {
    if (data.length < 1_048_576) return false;          // 最小1MB
    if (data[0] != 0x1f || (data[1] & 0xff) != 0x8b) return false; // Gzip魔数
    if (data.length > 104_857_600) return false;       // 最大100MB
    return true;
}
```

### 7.2 敏感数据处理

**URL中的认证信息**:
- **日志输出**: 脱敏处理（隐藏用户名/密码）
- **示例**: `https://***:***@example.com/file.tar.gz`
- **配置管理**: 建议使用环境变量，避免硬编码

```java
private String sanitizeUrl(String url) {
    return url.replaceAll("://([^:]+):([^@]+)@", "://***:***@");
}
```

---

## 8. 数据模型总结

### 新增实体
- **AgentBinaryConfig**: 配置类（非持久化）
- **BinaryCache**: 内存缓存（ConcurrentHashMap）

### 修改实体
- **AgentDeployment**: 增强日志格式（无Schema变更）

### 无需持久化的临时数据
- HTTP响应体（byte[]）
- 错误枚举（BinaryAcquisitionError）
- 同步锁对象（downloadLock）

### 内存占用
- 配置对象: <1 KB
- 缓存数据: ~40 MB（单URL）
- **总计**: ~40 MB（远低于200MB限制）

### 外部依赖
- **配置源**: application.properties 或环境变量
- **网络资源**: HTTP/HTTPS下载服务器
- **远程存储**: 目标Linux服务器文件系统（/tmp目录）

---

## 下一步

数据模型设计完成，接下来生成API合约（contracts/）定义内部服务接口。
