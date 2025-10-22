# SSH 客户端配置解析字段映射清单

**日期:** 2025-10-19  
**版本:** 1.0  
**状态:** ✅ 已完成

---

## 📋 目录

1. [通用数据模型](#通用数据模型)
2. [SecureCRT 字段映射](#securecrt-字段映射)
3. [Xshell 字段映射](#xshell-字段映射)
4. [Tabby 字段映射](#tabby-字段映射)
5. [映射到系统实体](#映射到系统实体)
6. [字段验证规则](#字段验证规则)
7. [已知问题和限制](#已知问题和限制)

---

## 🎯 通用数据模型

### SSHHostConfig（中间层 DTO）

所有 SSH 客户端配置首先解析为统一的 `SSHHostConfig` 对象：

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `hostPattern` | String | ✅ | - | 会话名称/别名 |
| `hostname` | String | ✅ | - | 实际主机地址（IP/域名） |
| `port` | Integer | ❌ | 22 | SSH 端口号 |
| `user` | String | ❌ | - | SSH 用户名 |
| `identityFile` | String | ❌ | - | SSH 私钥文件路径 |
| `proxyJump` | String | ❌ | - | 跳板机配置 |
| `description` | String | ❌ | - | 描述信息 |
| `group` | String | ❌ | - | 分组/文件夹路径 |
| `extraOptions` | Map<String, String> | ❌ | {} | 其他扩展选项 |

#### 辅助方法
```java
boolean isWildcardHost()      // 是否包含通配符 * 或 ?
boolean hasProxyJump()        // 是否配置跳板机
boolean hasIdentityFile()     // 是否配置私钥
int getEffectivePort()        // 获取有效端口（默认22）
```

---

## 🔐 SecureCRT 字段映射

### 文件格式
- **扩展名:** `.ini`
- **编码:** UTF-8 / ANSI
- **格式:** Windows INI 格式

### 字段类型前缀
- `S:"字段名"` - 字符串 (String)
- `D:"字段名"` - 十六进制整数 (Decimal Hex)
- `B:"字段名"` - 布尔值 (Boolean)

### 关键字段映射

| SecureCRT 字段 | 类型 | SSHHostConfig 字段 | 转换逻辑 | 示例 |
|----------------|------|-------------------|----------|------|
| `Protocol Name` | S: | - | 验证条件：必须为 `SSH2` 或 `SSH1` | `S:"Protocol Name"=SSH2` |
| `Hostname` | S: | `hostname` | 直接映射（必填） | `S:"Hostname"=192.168.1.100` |
| `[SSH2] Port` | D: | `port` | 十六进制转十进制 | `D:"[SSH2] Port"=00000016` → `22` |
| `Username` | S: | `user` | 直接映射 | `S:"Username"=admin` |
| `Identity Filename` | S: | `identityFile` | 直接映射 | `S:"Identity Filename"=C:\keys\id_rsa` |
| `Description` | S: | `description` | 直接映射 | `S:"Description"=Production DB` |
| `Folder` | S: | `group` | 直接映射，保留路径分隔符 | `S:"Folder"=Production/Databases` |
| `Firewall Name` | S: | ⚠️ 未映射 | 预留用于跳板机功能 | `S:"Firewall Name"=bastion01` |
| 文件名（去除.ini） | - | `hostPattern` | 使用会话文件名作为名称 | `prod-db-01.ini` → `prod-db-01` |

### 特殊转换规则

#### 1. 端口号转换
```java
// SecureCRT 使用 8 位十六进制表示端口
"00000016" → Integer.parseInt("00000016", 16) → 22
"00001770" → Integer.parseInt("00001770", 16) → 6000
```

#### 2. 值提取
```java
// 从引号中提取值
S:"Hostname"=192.168.1.100 → extractQuotedString() → "192.168.1.100"
```

### 示例配置文件

**prod-mysql-master.ini**
```ini
S:"Protocol Name"=SSH2
S:"Hostname"=10.0.1.50
D:"[SSH2] Port"=00000016
S:"Username"=root
S:"Identity Filename"=C:\Users\Admin\.ssh\prod_rsa
S:"Description"=生产环境MySQL主库
S:"Folder"=Production/Databases/MySQL
S:"Firewall Name"=
```

**解析结果:**
```json
{
  "hostPattern": "prod-mysql-master",
  "hostname": "10.0.1.50",
  "port": 22,
  "user": "root",
  "identityFile": "C:\\Users\\Admin\\.ssh\\prod_rsa",
  "description": "生产环境MySQL主库",
  "group": "Production/Databases/MySQL"
}
```

---

## 🖥️ Xshell 字段映射

### 文件格式
- **扩展名:** `.xsh`
- **编码:** UTF-8
- **格式:** XML

### 关键字段映射

| Xshell XML 标签 | SSHHostConfig 字段 | 转换逻辑 | 示例 |
|-----------------|-------------------|----------|------|
| `<title>` | `hostPattern` | 直接映射（会话名称） | `<title>生产服务器</title>` |
| `<host>` | `hostname` | 直接映射（必填） | `<host>192.168.1.100</host>` |
| `<port>` | `port` | 直接映射，默认 22 | `<port>22</port>` |
| `<protocol>` | - | 验证条件：必须为 `SSH` | `<protocol>SSH</protocol>` |
| `<username>` | `user` | 直接映射 | `<username>admin</username>` |
| `<userkey>` | `identityFile` | 直接映射（私钥路径） | `<userkey>/home/user/.ssh/id_rsa</userkey>` |
| `<description>` | `description` | 直接映射 | `<description>数据库服务器</description>` |
| `<folder>` | `group` | 直接映射 | `<folder>Production/Databases</folder>` |
| `<authentication>` | ⚠️ 未映射 | 仅用于验证认证方式 | `<authentication>PublicKey</authentication>` |

### 特殊处理规则

#### 1. 协议验证
```java
// 仅解析 SSH 协议会话
if (!session.protocol.toUpperCase().startsWith("SSH")) {
    return null; // 跳过非 SSH 会话（如 TELNET）
}
```

#### 2. 默认会话名称
```java
// 如果 title 为空，使用文件名（去除 .xsh）
if (title == null || title.isEmpty()) {
    title = fileName.replace(".xsh", "");
}
```

### 示例配置文件

**dev-server-01.xsh**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<session version="8.0">
    <title>开发环境-应用服务器01</title>
    <host>192.168.10.100</host>
    <port>22</port>
    <protocol>SSH</protocol>
    <username>developer</username>
    <authentication>PublicKey</authentication>
    <userkey>C:\Users\Dev\.ssh\dev_rsa</userkey>
    <description>开发环境的应用服务器，部署测试版本</description>
    <folder>Development/Servers</folder>
</session>
```

**解析结果:**
```json
{
  "hostPattern": "开发环境-应用服务器01",
  "hostname": "192.168.10.100",
  "port": 22,
  "user": "developer",
  "identityFile": "C:\\Users\\Dev\\.ssh\\dev_rsa",
  "description": "开发环境的应用服务器，部署测试版本",
  "group": "Development/Servers"
}
```

---

## 🎨 Tabby 字段映射

### 文件格式
- **文件名:** `config.yaml`
- **编码:** UTF-8
- **格式:** YAML

### 关键字段映射

| Tabby YAML 路径 | SSHHostConfig 字段 | 转换逻辑 | 示例 |
|-----------------|-------------------|----------|------|
| `profiles[].name` | `hostPattern` | 直接映射（会话名称） | `name: 生产服务器` |
| `profiles[].type` | - | 验证条件：必须为 `ssh` | `type: ssh` |
| `profiles[].options.host` | `hostname` | 直接映射（必填） | `host: 192.168.1.100` |
| `profiles[].options.port` | `port` | 直接映射，默认 22 | `port: 22` |
| `profiles[].options.user` | `user` | 直接映射 | `user: admin` |
| `profiles[].options.privateKey` | `identityFile` | 直接映射（私钥路径） | `privateKey: /path/to/key` |
| `profiles[].description` | `description` | 直接映射 | `description: 数据库服务器` |
| `profiles[].group` | `group` | 直接映射 | `group: Production/Databases` |

### 特殊处理规则

#### 1. 类型过滤
```java
// 仅解析 type=ssh 的 profile
if (!"ssh".equalsIgnoreCase(type)) {
    continue; // 跳过其他类型（如 serial, local）
}
```

#### 2. 嵌套 options 对象
```java
// host/port/user 等字段在 options 对象下
Map<String, Object> options = profile.get("options");
String host = options.get("host");
```

### 示例配置文件

**config.yaml**
```yaml
profiles:
  - type: ssh
    name: 生产数据库主库
    options:
      host: 10.0.1.50
      port: 22
      user: root
      privateKey: /home/admin/.ssh/prod_rsa
      forwardAgent: true
    description: 生产环境MySQL主库服务器
    group: Production/Databases/MySQL
  
  - type: ssh
    name: 测试环境Web服务器
    options:
      host: 192.168.20.10
      port: 22
      user: deploy
      privateKey: /home/admin/.ssh/test_rsa
    group: Testing/WebServers
```

**解析结果（第一个 profile）:**
```json
{
  "hostPattern": "生产数据库主库",
  "hostname": "10.0.1.50",
  "port": 22,
  "user": "root",
  "identityFile": "/home/admin/.ssh/prod_rsa",
  "description": "生产环境MySQL主库服务器",
  "group": "Production/Databases/MySQL"
}
```

---

## 🗄️ 映射到系统实体

### Server 实体（最终存储）

`SSHHostConfig` → `Server` 实体字段映射：

| SSHHostConfig 字段 | Server 实体字段 | 转换逻辑 | 备注 |
|-------------------|----------------|----------|------|
| `hostPattern` | `name` | 直接映射 | 服务器名称（唯一标识） |
| `hostname` | `hostname` | 直接映射 | 主机地址（必填） |
| `port` | `sshPort` | 直接映射 | SSH 端口，默认 22 |
| `user` | `sshUsername` | 直接映射 | SSH 用户名 |
| `identityFile` | `sshKeyPath` | 直接映射 | SSH 私钥文件路径 |
| `description` | `description` | 直接映射 | 服务器描述 |
| `group` | ⚠️ 未映射 | **需扩展** | 可用于服务器分组功能 |
| `proxyJump` | ⚠️ 未映射 | **需扩展** | 可用于跳板机配置 |
| - | `baseWorkDirectory` | **需补充** | 默认工作目录（如 `/home/user`） |
| - | `sshPasswordEncrypted` | **需补充** | 加密后的 SSH 密码 |
| - | `sshKeyPassphraseEncrypted` | **需补充** | 私钥密码短语（加密） |
| - | `serverType` | **需补充** | 服务器类型（DEVELOPMENT/PRODUCTION） |
| - | `privilegeLevel` | **需补充** | 权限级别（ROOT/SUDO/NORMAL） |
| - | `active` | **默认值** | 默认 `true` |
| - | `autoMonitorEnabled` | **默认值** | 默认 `true` |
| - | `monitorIntervalSeconds` | **默认值** | 默认 `60` |

### 系统补充字段

以下字段需在导入时由用户补充或系统自动填充：

#### 1. 必填字段
- ✅ **`baseWorkDirectory`** - 需用户输入或默认 `/home/{sshUsername}`
- ⚠️ **`sshPasswordEncrypted`** - 如果不使用密钥，需用户输入密码

#### 2. 可选字段
- ✅ **`serverType`** - 根据 `group` 推断（如包含 "prod" → PRODUCTION）
- ✅ **`privilegeLevel`** - 导入后首次连接时检测
- ✅ **`sshKeyPassphraseEncrypted`** - 如果私钥有密码，需用户输入

---

## ✅ 字段验证规则

### 必填字段验证

#### SSHHostConfig 层面
```java
// 1. hostname 必填
if (hostname == null || hostname.trim().isEmpty()) {
    throw new ValidationException("hostname is required");
}

// 2. hostPattern 必填
if (hostPattern == null || hostPattern.trim().isEmpty()) {
    throw new ValidationException("hostPattern is required");
}
```

#### Server 实体层面
```java
// 1. name 必填且唯一
@Column(nullable = false, unique = true)
private String name;

// 2. hostname 必填
@Column(nullable = false)
private String hostname;

// 3. port 必填，默认 22
@Column(nullable = false)
private Integer sshPort = 22;

// 4. baseWorkDirectory 必填
@Column(nullable = false)
private String baseWorkDirectory;
```

### 格式验证

#### 端口号
```java
// 端口范围：1-65535
if (port < 1 || port > 65535) {
    throw new ValidationException("Invalid port: " + port);
}
```

#### 主机名/IP
```java
// IP 地址或域名格式验证
if (!isValidHostname(hostname) && !isValidIPAddress(hostname)) {
    throw new ValidationException("Invalid hostname: " + hostname);
}
```

#### 私钥路径
```java
// 文件路径验证（仅格式检查，不验证存在性）
if (identityFile != null && identityFile.contains("..")) {
    throw new ValidationException("Invalid identity file path: " + identityFile);
}
```

---

## ⚠️ 已知问题和限制

### 1. 密码处理

| 客户端 | 密码存储方式 | 导入支持 | 解决方案 |
|--------|-------------|---------|----------|
| **SecureCRT** | 加密存储在 INI 文件中 | ❌ 无法解密 | 导入后要求用户重新输入 |
| **Xshell** | 存储在系统密钥链 | ❌ 无法访问 | 导入后要求用户重新输入 |
| **Tabby** | 存储在系统密钥链 | ❌ 无法访问 | 导入后要求用户重新输入 |

**统一策略:** 所有导入的服务器配置**不包含密码**，用户首次连接时需输入并保存。

### 2. 私钥密码短语

私钥文件路径可以导入，但如果私钥有密码保护：
- ❌ 无法从客户端配置中获取
- ✅ 导入时提示用户输入（可选）
- ✅ 首次连接时要求输入并缓存

### 3. 跳板机配置

| 客户端 | 跳板机支持 | 映射字段 | 当前状态 |
|--------|-----------|---------|---------|
| **SecureCRT** | ✅ Firewall Name | `proxyJump` | ⚠️ 已识别，未实现 |
| **Xshell** | ✅ Proxy Settings | - | ❌ 未解析 |
| **Tabby** | ✅ Jump Host | - | ❌ 未解析 |

**计划扩展:** Phase 3 支持跳板机配置解析和使用。

### 4. 服务器分组

| 客户端 | 分组字段 | SSHHostConfig | Server 实体 |
|--------|---------|--------------|------------|
| **SecureCRT** | `Folder` | ✅ `group` | ⚠️ 未映射 |
| **Xshell** | `folder` | ✅ `group` | ⚠️ 未映射 |
| **Tabby** | `group` | ✅ `group` | ⚠️ 未映射 |

**建议扩展:** 
- 选项 1: 扩展 `Server` 实体增加 `groupName` 字段
- 选项 2: 创建独立的 `ServerGroup` 表和关联关系

### 5. 特殊字符处理

```java
// 文件路径中的反斜杠转换
// Windows: C:\Users\Admin\.ssh\id_rsa
// 存储为: C:\\Users\\Admin\\.ssh\\id_rsa
identityFile = identityFile.replace("\\", "\\\\");
```

### 6. 重复服务器检测

导入时需检查：
- ✅ 按 `hostname` + `sshPort` 去重
- ✅ 按 `name` 唯一性约束
- ⚠️ 提示用户选择保留或覆盖

---

## 📊 字段映射完整性检查

### 核心字段映射率

| 字段类型 | SecureCRT | Xshell | Tabby | 映射到 Server |
|---------|-----------|--------|-------|--------------|
| 会话名称 | ✅ 100% | ✅ 100% | ✅ 100% | ✅ `name` |
| 主机地址 | ✅ 100% | ✅ 100% | ✅ 100% | ✅ `hostname` |
| 端口号 | ✅ 100% | ✅ 100% | ✅ 100% | ✅ `sshPort` |
| 用户名 | ✅ 100% | ✅ 100% | ✅ 100% | ✅ `sshUsername` |
| 私钥路径 | ✅ 100% | ✅ 100% | ✅ 100% | ✅ `sshKeyPath` |
| 描述信息 | ✅ 100% | ✅ 100% | ✅ 100% | ✅ `description` |
| 分组路径 | ✅ 100% | ✅ 100% | ✅ 100% | ⚠️ 未映射 |
| 跳板机 | ⚠️ 50% | ❌ 0% | ❌ 0% | ⚠️ 未映射 |
| 密码 | ❌ 0% | ❌ 0% | ❌ 0% | ⚠️ 需用户输入 |

### 建议优先级

**P0 - 必须实现:**
- ✅ 所有核心字段已完成

**P1 - 高优先级:**
- ⚠️ 服务器分组功能（`group` → `ServerGroup`）
- ⚠️ 密码输入提示（导入后首次连接）

**P2 - 中优先级:**
- ⚠️ 跳板机配置支持（`proxyJump`）
- ⚠️ 私钥密码短语输入

**P3 - 低优先级:**
- ⚠️ 自动检测服务器类型（根据 group/hostname 推断）
- ⚠️ 批量编辑导入配置

---

## 🔗 相关文件

### DTO 类
- `SSHHostConfig.java` - 通用配置 DTO
- `SSHConfigParseResult.java` - 解析结果封装

### Parser 实现
- `SecureCRTIniParser.java` - SecureCRT 解析器
- `XshellXmlParser.java` - Xshell 解析器
- `TabbyYamlParser.java` - Tabby 解析器
- `ConfigParser.java` - 解析器接口

### 实体类
- `Server.java` - 服务器实体（最终存储）

### 测试用例
- `SecureCRTIniParserTest.java`
- `XshellXmlParserTest.java`
- `TabbyYamlParserTest.java`

---

**文档版本:** 1.0  
**最后更新:** 2025-10-19  
**维护者:** InternalPaaS Team
