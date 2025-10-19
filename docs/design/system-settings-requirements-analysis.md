# 系统设置页面需求分析文档

**文档版本:** 1.0  
**创建日期:** 2025-10-18  
**作者:** AI Assistant  
**状态:** 需求分析阶段

---

## 📋 目录

1. [需求概述](#需求概述)
2. [核心功能分析](#核心功能分析)
3. [SecureCRT配置导入详细分析](#securecrt配置导入详细分析)
4. [其他系统配置项](#其他系统配置项)
5. [技术实现方案](#技术实现方案)
6. [数据模型设计](#数据模型设计)
7. [用户界面设计](#用户界面设计)
8. [安全性考虑](#安全性考虑)
9. [开发优先级](#开发优先级)
10. [验收标准](#验收标准)

---

## 需求概述

### 背景
Internal PaaS Platform 作为企业级服务器管理与监控平台，当前缺少统一的系统配置管理界面。用户需要：
- 快速批量导入现有的服务器配置（如从SecureCRT）
- 集中管理各类系统级参数
- 个性化定制平台行为和外观

### 目标
设计并实现一个统一的"系统设置"页面，包含：
1. **服务器配置导入** - 支持从SecureCRT 7.0等SSH客户端批量导入服务器配置
2. **系统全局配置** - 平台级参数管理
3. **用户个性化设置** - 主题、通知、语言等用户偏好

### 用户角色
- **管理员 (Admin)**: 可访问全部系统设置
- **开发者 (Developer)**: 仅可访问个人设置

---

## 核心功能分析

### 功能1: 服务器配置导入

#### 1.1 功能描述
从外部SSH客户端（SecureCRT、Xshell、Tabby等）导入现有的服务器连接配置，避免手动重复录入。

#### 1.2 支持的导入源
| 工具名称 | 版本支持 | 配置文件格式 | 优先级 |
|---------|---------|------------|--------|
| SecureCRT | 7.0+ | `.ini` (Sessions目录) | P0 - 必须 |
| Xshell | 6.0+ | `.xsh` (XML格式) | P1 - 应该 |
| Tabby | 1.0+ | `config.yaml` | P2 - 可以 |
| MobaXterm | 20.0+ | `.mxtsessions` | P3 - 可选 |
| Putty | Any | `.reg` (注册表) | P3 - 可选 |

#### 1.3 导入模式
- **自动扫描导入**: 根据软件默认安装路径自动扫描配置文件（推荐）
- **手动指定导入**: 用户手动指定配置文件目录或安装路径
- **文件上传导入**: 上传配置文件，适用于无法访问本地文件系统的场景

#### 1.4 导入流程
```
┌─────────────┐
│ 选择导入源  │ → SecureCRT / Xshell / Tabby / 其他
└──────┬──────┘
       ↓
┌─────────────────────────────┐
│ 自动扫描 / 手动指定 / 上传   │
├─────────────────────────────┤
│ • 自动扫描: 检测默认安装路径  │
│ • 手动指定: 用户输入目录     │
│ • 文件上传: 上传配置文件     │
└──────┬──────────────────────┘
       ↓
┌─────────────┐
│ 扫描结果展示 │ → 显示找到的配置文件数量
└──────┬──────┘
       ↓
┌─────────────┐
│ 解析配置文件 │ → 提取服务器信息
└──────┬──────┘
       ↓
┌─────────────┐
│ 数据预览确认 │ → 展示解析结果，允许编辑
└──────┬──────┘
       ↓
┌─────────────┐
│ 冲突检测处理 │ → 检查重复服务器
└──────┬──────┘
       ↓
┌─────────────┐
│ 批量导入执行 │ → 写入数据库
└──────┬──────┘
       ↓
┌─────────────┐
│ 结果报告展示 │ → 成功/失败统计
└─────────────┘
```

---

## SecureCRT配置导入详细分析

### 2.1 SecureCRT配置文件结构

#### 默认安装路径和配置文件位置

**Windows系统**:
- **默认安装路径**: 
  - `C:\Program Files\VanDyke Software\SecureCRT\`
  - `C:\Program Files (x86)\VanDyke Software\SecureCRT\`
- **配置文件目录**: `%APPDATA%\VanDyke\Config\Sessions\`
  - 实际路径通常为: `C:\Users\{用户名}\AppData\Roaming\VanDyke\Config\Sessions\`

**macOS系统**:
- **默认安装路径**: `/Applications/SecureCRT.app/`
- **配置文件目录**: `~/Library/Application Support/VanDyke/SecureCRT/Config/Sessions/`

**Linux系统**:
- **默认安装路径**: `/usr/local/bin/SecureCRT` 或 `~/securecrt/`
- **配置文件目录**: `~/.vandyke/SecureCRT/Config/Sessions/`

#### 配置文件格式 (.ini)
SecureCRT使用INI格式存储每个会话的配置：

```ini
# 文件: __FolderData__.ini (文件夹信息)
S:"Session Name"=My Server

# 文件: MyServer.ini (会话配置)
[SessionConfiguration]
S:"Protocol Name"=SSH2
S:"Hostname"=192.168.1.100
D:"[SSH2] Port"=00000016  ; 十六进制，转换为22
S:"Username"=admin
S:"Password"=<encrypted>  ; 加密存储
S:"Description"=Production Server
S:"Firewall Name"=None
S:"Folder"=/Production

[SessionOptions]
D:"Color Scheme"=00000001
S:"Output Transformer Name"=UTF-8
D:"Use Word Delimiter Chars"=00000001

[Terminal]
D:"Rows"=00000018  ; 24行
D:"Cols"=00000050  ; 80列
```

### 2.2 需要提取的关键字段映射

| SecureCRT字段 | 数据类型 | Internal PaaS字段 | 转换规则 |
|--------------|---------|------------------|---------|
| `S:"Hostname"` | String | `Server.hostname` | 直接映射 |
| `D:"[SSH2] Port"` | Hex | `Server.sshPort` | 十六进制转十进制 |
| `S:"Username"` | String | `Server.sshUsername` | 直接映射 |
| `S:"Password"` | Encrypted | `Server.sshPasswordEncrypted` | 需要解密后重新加密 |
| `S:"Session Name"` | String | `Server.name` | 直接映射 |
| `S:"Description"` | String | `Server.description` | 直接映射 |
| `S:"Folder"` | String | `ServerGroup.name` | 映射到服务器分组 |
| `S:"Identity Filename"` | Path | `Server.sshKeyPath` | SSH密钥路径 |

### 2.3 密码处理策略

#### SecureCRT密码加密
SecureCRT使用Blowfish算法加密密码，存储格式：
```
S:"Password"=u02:v1:...encrypted_data...
```

#### 处理方案
由于SecureCRT使用专有加密算法，我们采用以下策略：

**方案1: 提示用户重新输入密码（推荐）**
- 解析时将密码字段标记为`***需要重新输入***`
- 导入预览时提示用户为每个服务器输入密码
- 使用系统自己的加密方式存储

**方案2: 尝试解密（可选，复杂度高）**
- 实现SecureCRT v2密码解密算法
- 需要用户提供主密码（如果设置了）
- 解密后使用系统加密方式重新加密

**推荐使用方案1**，理由：
- 更安全（避免破解专有加密）
- 实现简单
- 符合安全最佳实践

### 2.4 文件夹（分组）映射

SecureCRT使用文件夹组织会话：
```
Sessions/
├── __FolderData__.ini
├── Production/
│   ├── __FolderData__.ini
│   ├── WebServer1.ini
│   └── DBServer1.ini
└── Development/
    ├── __FolderData__.ini
    └── DevServer1.ini
```

映射到`ServerGroup`:
- 文件夹路径 → `ServerGroup.name`
- 文件夹结构支持嵌套（用`/`分隔）
- 导入时自动创建不存在的分组

### 2.5 自动扫描功能设计

#### 2.5.1 扫描策略

**扫描顺序**（按优先级）:
1. **环境变量检查**: 
   - Windows: 检查注册表 `HKEY_CURRENT_USER\Software\VanDyke\SecureCRT\`
   - 获取实际配置路径
   
2. **默认路径扫描**:
   ```
   Windows:
   - %APPDATA%\VanDyke\Config\Sessions\
   - %USERPROFILE%\Documents\VanDyke\Config\Sessions\
   
   macOS:
   - ~/Library/Application Support/VanDyke/SecureCRT/Config/Sessions/
   - ~/Documents/VanDyke/SecureCRT/Config/Sessions/
   
   Linux:
   - ~/.vandyke/SecureCRT/Config/Sessions/
   - ~/securecrt/Config/Sessions/
   ```

3. **安装目录推断**:
   - 如果找到安装目录，尝试从安装目录推断配置目录
   - Windows: `{安装目录}\..\Config\Sessions\`

4. **用户最近使用**:
   - 记录用户上次成功导入的路径
   - 优先扫描历史成功路径

#### 2.5.2 扫描结果返回

```json
{
  "scanStatus": "success",  // success, partial, failed
  "detectedSoftware": "SecureCRT 9.0",
  "configPath": "C:\\Users\\Admin\\AppData\\Roaming\\VanDyke\\Config\\Sessions",
  "sessionCount": 25,
  "folderCount": 5,
  "estimatedImportTime": "2分钟",
  "scanDetails": {
    "installPath": "C:\\Program Files\\VanDyke Software\\SecureCRT",
    "installVersion": "9.0.2",
    "configLastModified": "2025-10-15T10:30:00",
    "scanMethod": "registry"  // registry, default-path, user-history
  },
  "suggestions": [
    "发现25个会话配置，建议在低峰期导入",
    "检测到5个文件夹，将自动创建对应的服务器分组"
  ]
}
```

#### 2.5.3 扫描失败处理

当自动扫描失败时，提供智能提示：

```
❌ 自动扫描失败

未能在以下位置找到SecureCRT配置文件：
• C:\Users\Admin\AppData\Roaming\VanDyke\Config\Sessions
• C:\Users\Admin\Documents\VanDyke\Config\Sessions

可能的原因：
1. SecureCRT未安装或使用便携版
2. 配置文件位于非默认位置
3. 没有访问配置目录的权限

解决方案：
→ [手动指定目录]  手动输入配置文件目录
→ [选择安装目录]  指定SecureCRT安装目录，系统将自动推断配置路径
→ [上传配置文件]  直接上传.ini配置文件
```

---

## 其他SSH客户端配置导入支持

### 3.1 Xshell配置导入

#### 默认安装路径和配置文件位置

**Windows系统**:
- **默认安装路径**: 
  - `C:\Program Files\NetSarang\Xshell 7\`
  - `C:\Program Files (x86)\NetSarang\Xshell 7\`
- **配置文件目录**: `%USERPROFILE%\Documents\NetSarang Computer\7\Xshell\Sessions\`
  - 或注册表位置: `HKEY_CURRENT_USER\Software\NetSarang\Xshell\7\`

#### 配置文件格式
Xshell使用XML格式存储会话配置 (`.xsh`文件):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<session version="8.0">
    <title>MyServer</title>
    <host>192.168.1.100</host>
    <port>22</port>
    <protocol>SSH</protocol>
    <username>admin</username>
    <authentication>PASSWORD</authentication>
    <password encoding="base64">encrypted_password</password>
    <description>Production Server</description>
    <folder>Production</folder>
</session>
```

#### 自动扫描策略
1. 检查注册表获取实际配置路径
2. 扫描默认文档路径
3. 从安装目录推断配置路径

### 3.2 Tabby配置导入

#### 默认安装路径和配置文件位置

**Windows系统**:
- **默认安装路径**: `C:\Users\{用户名}\AppData\Local\Programs\Tabby\`
- **配置文件**: `%APPDATA%\tabby\config.yaml`

**macOS系统**:
- **默认安装路径**: `/Applications/Tabby.app/`
- **配置文件**: `~/Library/Application Support/tabby/config.yaml`

**Linux系统**:
- **默认安装路径**: `/usr/bin/tabby` 或 `~/.local/share/applications/`
- **配置文件**: `~/.config/tabby/config.yaml`

#### 配置文件格式
Tabby使用YAML格式存储配置:

```yaml
ssh:
  connections:
    - name: MyServer
      host: 192.168.1.100
      port: 22
      user: admin
      password: encrypted_password
      group: Production
      privateKey: /path/to/key
```

### 3.3 MobaXterm配置导入

#### 默认安装路径和配置文件位置

**Windows系统**:
- **默认安装路径**: 
  - `C:\Program Files\Mobatek\MobaXterm\`
  - 便携版: 用户自定义目录
- **配置文件**: `%USERPROFILE%\Documents\MobaXterm\MobaXterm.ini`
  - 或便携版: `{安装目录}\MobaXterm.ini`

#### 配置文件格式
MobaXterm使用INI格式，但结构不同于SecureCRT:

```ini
[Bookmarks]
SubRep=MyServer
ImgNum=41
Session=Ssh://admin@192.168.1.100:22
```

### 3.4 PuTTY配置导入

#### 默认配置位置

**Windows系统**:
- **配置存储**: Windows注册表
- **路径**: `HKEY_CURRENT_USER\Software\SimonTatham\PuTTY\Sessions\`

**Linux系统** (使用PuTTY for Linux):
- **配置文件**: `~/.putty/sessions/`

#### 配置格式
PuTTY在注册表中存储会话配置，每个会话是一个注册表项:

```
Sessions\MyServer\
├── HostName = "192.168.1.100"
├── PortNumber = 22
├── UserName = "admin"
└── Protocol = "ssh"
```

### 3.5 统一扫描接口设计

所有SSH客户端支持统一的扫描接口：

```java
public interface ConfigScanner {
    /**
     * 获取扫描优先级（数字越小优先级越高）
     */
    int getPriority();
    
    /**
     * 获取支持的软件名称
     */
    String getSoftwareName();
    
    /**
     * 获取默认扫描路径列表
     */
    List<String> getDefaultScanPaths();
    
    /**
     * 自动扫描配置文件
     */
    ScanResult autoScan();
    
    /**
     * 从指定路径扫描
     */
    ScanResult scanFromPath(String path);
    
    /**
     * 从安装目录推断配置路径
     */
    String inferConfigPath(String installPath);
    
    /**
     * 解析配置文件
     */
    List<ServerImportDto> parseConfig(String configPath);
}
```

---

## 其他系统配置项

### 3.1 全局系统配置（管理员可见）

#### A. 数据库配置模式切换
- **当前模式**: H2内存/H2文件/PostgreSQL
- **切换说明**: 展示当前使用的数据库类型
- **操作**: 提供配置向导，生成`application-*.properties`

#### B. Metrics Hub集成配置
```properties
# 从 application.properties 提取
metrics.hub.enabled=true
metrics.hub.base-url=http://localhost:8081
metrics.hub.timeout.connect-ms=5000
metrics.hub.timeout.read-ms=10000
metrics.hub.fallback.enabled=true
```

**界面呈现**:
- Hub服务地址配置
- 连接超时设置
- 降级策略开关
- 连接测试按钮

#### C. Agent自动部署配置
```properties
agent.auto-deploy.enabled=true
agent.version=0.91.0
agent.otlp.endpoint=http://localhost:4317
agent.deploy.timeout-minutes=10
```

**界面呈现**:
- Agent版本选择
- OTLP端点配置
- 部署超时设置
- Agent健康检查配置

#### D. SSH监控调度配置
```properties
monitoring.ssh.scheduler.enabled=false
```

**界面呈现**:
- SSH监控开关（已停用，显示说明）
- 监控间隔设置（如果启用）

#### E. 安全配置
```properties
app.security.master-key=***
app.security.password-encryption.enabled=true
server.servlet.session.timeout=30m
```

**界面呈现**:
- 主密钥管理（脱敏显示）
- 密码加密开关
- 会话超时配置
- Cookie安全设置

#### F. 日志级别配置
```properties
logging.level.com.cmict.internalpaas=ERROR
logging.level.root=ERROR
```

**界面呈现**:
- 全局日志级别
- 模块级别日志配置（下拉选择：ERROR/WARN/INFO/DEBUG）
- 实时日志查看器（可选）

#### G. 性能与资源限制
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.servlet.multipart.max-file-size=100MB
spring.mvc.async.request-timeout=120000
```

**界面呈现**:
- 数据库连接池大小
- 文件上传限制
- 请求超时配置
- JVM内存使用监控（只读）

#### H. 告警配置
- 告警阈值全局设置（参考`AlertThresholdRepository`）
- CPU使用率阈值
- 内存使用率阈值
- 磁盘使用率阈值
- 网络流量阈值
- 告警通知渠道（邮件/webhook）

#### I. 服务器分组管理
- 创建/编辑/删除服务器分组
- 分组成员管理
- 分组权限配置

#### J. 系统维护
- 数据备份配置（自动备份周期）
- 数据清理策略（日志保留天数）
- 系统诊断工具
- 数据库迁移工具

---

### 3.2 用户个人配置（所有用户可见）

基于现有`UserConfig`模型：

#### A. 外观与主题
```java
private String theme = "light"; // light, dark, auto
private String dashboardLayout = "default"; // default, compact, detailed
```

**界面呈现**:
- 主题选择器：浅色/深色/跟随系统
- 仪表板布局：默认/紧凑/详细
- 主题预览效果

#### B. 通知设置
```java
private Boolean emailNotifications = true;
private Boolean systemNotifications = true;
private Boolean applicationStatusNotifications = true;
private Boolean securityNotifications = true;
```

**界面呈现**:
- 邮件通知开关
- 系统通知开关
- 应用状态通知
- 安全通知

#### C. 终端设置
```java
private String terminalTheme = "dark";
private Integer terminalFontSize = 14;
private String terminalFontFamily = "Monaco";
```

**界面呈现**:
- 终端主题选择
- 字体大小滑块（10-24）
- 字体系列选择（Monaco/Consolas/Courier）
- 实时预览

#### D. 语言与时区
```java
private String language = "zh_CN"; // zh_CN, en_US
private String timeZone = "Asia/Shanghai";
```

**界面呈现**:
- 语言选择：中文/英文
- 时区选择器
- 日期时间格式预览

#### E. 仪表板定制
```java
private Boolean showWelcomeMessage = true;
private Boolean showQuickActions = true;
private Boolean showRecentActivity = true;
```

**界面呈现**:
- 显示欢迎消息开关
- 显示快捷操作开关
- 显示最近活动开关

#### F. 工作目录
```java
private String workDirectory;
```

**界面呈现**:
- 工作目录路径输入
- 浏览按钮
- 权限验证

---

## 技术实现方案

### 4.1 后端架构

#### 目录结构
```
src/main/java/com/cmict/internalpaas/
├── controller/
│   └── SystemSettingsController.java      # 系统设置控制器
├── service/
│   ├── ConfigImportService.java           # 配置导入服务（统一入口）
│   ├── scanner/
│   │   ├── ConfigScanner.java             # 扫描器接口
│   │   ├── SecureCRTScanner.java          # SecureCRT扫描器
│   │   ├── XshellScanner.java             # Xshell扫描器
│   │   ├── TabbyScanner.java              # Tabby扫描器
│   │   └── ScannerFactory.java            # 扫描器工厂
│   ├── parser/
│   │   ├── ConfigParser.java              # 解析器接口
│   │   ├── SecureCRTParser.java           # SecureCRT解析器
│   │   ├── XshellParser.java              # Xshell解析器
│   │   └── TabbyParser.java               # Tabby解析器
│   ├── SystemConfigService.java           # 系统配置服务
│   └── UserPreferenceService.java         # 用户偏好服务
├── model/
│   ├── SystemConfig.java                  # 系统配置实体
│   ├── ImportSession.java                 # 导入会话记录
│   ├── ServerImportDto.java               # 服务器导入DTO
│   └── ScanResult.java                    # 扫描结果DTO
└── repository/
    ├── SystemConfigRepository.java        # 系统配置仓库
    └── ImportSessionRepository.java       # 导入会话仓库
```

#### API设计

##### 配置导入API
```java
// 自动扫描配置文件
POST /api/settings/import/scan
Request:
{
  "importSource": "securecrt",  // securecrt, xshell, tabby
  "customPath": null  // 可选：用户指定的扫描路径
}
Response:
{
  "success": true,
  "scanStatus": "success",
  "detectedSoftware": "SecureCRT 9.0",
  "configPath": "C:\\Users\\Admin\\AppData\\Roaming\\VanDyke\\Config\\Sessions",
  "sessionCount": 25,
  "folderCount": 5,
  "estimatedImportTime": "2分钟",
  "scanMethod": "registry",
  "suggestions": [
    "发现25个会话配置，建议在低峰期导入"
  ]
}

// 解析配置文件
POST /api/settings/import/parse
Request:
{
  "importSource": "securecrt",  // securecrt, xshell, tabby
  "scanMode": "auto",  // auto(自动扫描), manual(手动指定), upload(上传文件)
  "configPath": "/path/to/sessions",  // scanMode=manual时必填
  "installPath": "/path/to/securecrt",  // 可选：安装目录，用于推断配置路径
  "fileContent": "base64_encoded_content"  // scanMode=upload时必填
}
Response:
{
  "success": true,
  "sessionId": "import-123456",
  "parsedServers": [
    {
      "name": "WebServer1",
      "hostname": "192.168.1.100",
      "port": 22,
      "username": "admin",
      "passwordStatus": "needs_input",  // encrypted, needs_input
      "group": "Production",
      "description": "Web Server",
      "conflict": false  // 是否与现有服务器冲突
    }
  ],
  "summary": {
    "total": 10,
    "conflicts": 2,
    "newGroups": ["Production", "Development"]
  }
}

// 执行导入
POST /api/settings/import/execute
Request:
{
  "sessionId": "import-123456",
  "selectedServers": [0, 1, 2],  // 索引
  "conflictResolution": "skip",  // skip, overwrite, rename
  "passwords": {
    "0": "encrypted_password_1",
    "1": "encrypted_password_2"
  }
}
Response:
{
  "success": true,
  "imported": 8,
  "skipped": 2,
  "failed": 0,
  "errors": []
}

// 取消导入
POST /api/settings/import/cancel
Request:
{
  "sessionId": "import-123456"
}
```

##### 系统配置API
```java
// 获取系统配置
GET /api/settings/system
Response:
{
  "metricsHub": {
    "enabled": true,
    "baseUrl": "http://localhost:8081",
    "timeout": {
      "connect": 5000,
      "read": 10000
    }
  },
  "agent": {
    "autoDeployEnabled": true,
    "version": "0.91.0",
    "otlpEndpoint": "http://localhost:4317"
  },
  "security": {
    "passwordEncryptionEnabled": true,
    "sessionTimeout": 1800
  },
  "logging": {
    "globalLevel": "ERROR",
    "modules": {
      "com.cmict.internalpaas": "ERROR"
    }
  }
}

// 更新系统配置
PUT /api/settings/system
Request: (同上结构)
Response:
{
  "success": true,
  "message": "配置已更新",
  "requiresRestart": true  // 是否需要重启
}

// 测试配置连接
POST /api/settings/system/test-connection
Request:
{
  "type": "metrics-hub",  // metrics-hub, database, smtp
  "config": { ... }
}
Response:
{
  "success": true,
  "latency": 50,
  "message": "连接成功"
}
```

##### 用户偏好API
```java
// 获取用户配置
GET /api/settings/user
Response:
{
  "theme": "dark",
  "language": "zh_CN",
  "notifications": {
    "email": true,
    "system": true
  },
  "terminal": {
    "theme": "dark",
    "fontSize": 14,
    "fontFamily": "Monaco"
  }
}

// 更新用户配置
PUT /api/settings/user
Request: (同上结构)
Response:
{
  "success": true,
  "message": "偏好设置已保存"
}
```

### 4.2 前端架构

#### 页面结构
```
src/main/resources/templates/admin/
└── system-settings.html

src/main/resources/static/
├── js/
│   └── system-settings.js
└── css/
    └── system-settings.css
```

#### 组件设计
```
┌────────────────────────────────────────────┐
│          系统设置 - System Settings         │
├────────────────────────────────────────────┤
│  ┌──────────┐  ┌─────────────────────────┐ │
│  │ 导航菜单  │  │       内容区域           │ │
│  │          │  │                         │ │
│  │ 服务器导入│  │  [动态内容区域]          │ │
│  │ 系统配置  │  │                         │ │
│  │ 用户偏好  │  │                         │ │
│  │ 告警配置  │  │                         │ │
│  │ 分组管理  │  │                         │ │
│  │ 系统维护  │  │                         │ │
│  └──────────┘  └─────────────────────────┘ │
└────────────────────────────────────────────┘
```

---

## 数据模型设计

### 5.1 系统配置实体

```java
@Entity
@Table(name = "system_configs")
public class SystemConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 配置分类
    @Column(nullable = false, unique = true, length = 50)
    private String category; // metrics_hub, agent, security, logging
    
    // 配置键
    @Column(nullable = false, length = 100)
    private String configKey;
    
    // 配置值（JSON格式）
    @Column(columnDefinition = "TEXT")
    private String configValue;
    
    // 配置数据类型
    @Column(length = 20)
    private String valueType; // string, number, boolean, json
    
    // 配置描述
    @Column(length = 500)
    private String description;
    
    // 是否需要重启
    @Column(nullable = false)
    private Boolean requiresRestart = false;
    
    // 更新记录
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @ManyToOne
    @JoinColumn(name = "updated_by")
    private User updatedBy;
    
    // ... getters and setters
}
```

### 5.2 导入会话实体

```java
@Entity
@Table(name = "import_sessions")
public class ImportSession {
    @Id
    private String sessionId; // UUID
    
    // 导入源
    @Column(nullable = false, length = 50)
    private String importSource; // securecrt, xshell, tabby
    
    // 导入状态
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status; // PARSING, READY, EXECUTING, COMPLETED, CANCELLED, FAILED
    
    // 解析结果（JSON格式）
    @Column(columnDefinition = "TEXT")
    private String parsedData;
    
    // 导入统计
    @Column
    private Integer totalServers;
    
    @Column
    private Integer importedServers;
    
    @Column
    private Integer skippedServers;
    
    @Column
    private Integer failedServers;
    
    // 创建信息
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @Column
    private LocalDateTime completedAt;
    
    // ... getters and setters
    
    public enum ImportStatus {
        PARSING("解析中"),
        READY("就绪"),
        EXECUTING("执行中"),
        COMPLETED("已完成"),
        CANCELLED("已取消"),
        FAILED("失败");
        
        private final String description;
        ImportStatus(String description) {
            this.description = description;
        }
    }
}
```

### 5.3 服务器导入DTO

```java
public class ServerImportDto {
    private String name;
    private String hostname;
    private Integer sshPort;
    private String sshUsername;
    private String sshPassword; // 明文或加密
    private String sshKeyPath;
    private String description;
    private String groupName;
    
    // 冲突检测
    private boolean hasConflict;
    private Long conflictingServerId;
    private String conflictReason;
    
    // 验证状态
    private boolean isValid;
    private List<String> validationErrors;
    
    // ... getters and setters
}
```

### 5.4 扫描结果DTO

```java
public class ScanResult {
    // 扫描状态
    private ScanStatus status; // SUCCESS, PARTIAL, FAILED
    
    // 检测到的软件信息
    private String detectedSoftware;
    private String installPath;
    private String installVersion;
    
    // 配置文件信息
    private String configPath;
    private Integer sessionCount;
    private Integer folderCount;
    private LocalDateTime configLastModified;
    
    // 扫描详情
    private String scanMethod; // registry, default-path, user-history, install-infer
    private List<String> scannedPaths;
    private String estimatedImportTime;
    
    // 建议和警告
    private List<String> suggestions;
    private List<String> warnings;
    
    public enum ScanStatus {
        SUCCESS("扫描成功"),
        PARTIAL("部分成功"),
        FAILED("扫描失败");
        
        private final String description;
        ScanStatus(String description) {
            this.description = description;
        }
    }
    
    // ... getters and setters
}
```

---

## 用户界面设计

### 6.1 页面布局

```html
<!-- 系统设置主页面 -->
<div class="settings-container">
    <!-- 左侧导航 -->
    <aside class="settings-sidebar">
        <nav class="settings-nav">
            <div class="settings-nav-section">
                <h3>数据管理</h3>
                <a href="#import-servers" class="settings-nav-item active">
                    <i class="fas fa-download"></i>
                    <span>服务器导入</span>
                </a>
            </div>
            
            <div class="settings-nav-section">
                <h3>系统配置</h3>
                <a href="#metrics-hub" class="settings-nav-item">
                    <i class="fas fa-chart-line"></i>
                    <span>Metrics Hub</span>
                </a>
                <a href="#agent-deploy" class="settings-nav-item">
                    <i class="fas fa-robot"></i>
                    <span>Agent部署</span>
                </a>
                <a href="#security" class="settings-nav-item">
                    <i class="fas fa-shield-alt"></i>
                    <span>安全配置</span>
                </a>
                <a href="#logging" class="settings-nav-item">
                    <i class="fas fa-file-alt"></i>
                    <span>日志配置</span>
                </a>
            </div>
            
            <div class="settings-nav-section">
                <h3>个人设置</h3>
                <a href="#appearance" class="settings-nav-item">
                    <i class="fas fa-palette"></i>
                    <span>外观</span>
                </a>
                <a href="#notifications" class="settings-nav-item">
                    <i class="fas fa-bell"></i>
                    <span>通知</span>
                </a>
                <a href="#terminal" class="settings-nav-item">
                    <i class="fas fa-terminal"></i>
                    <span>终端</span>
                </a>
            </div>
        </nav>
    </aside>
    
    <!-- 右侧内容区 -->
    <main class="settings-content">
        <div id="import-servers" class="settings-section">
            <!-- 服务器导入界面 -->
        </div>
        
        <div id="metrics-hub" class="settings-section hidden">
            <!-- Metrics Hub配置 -->
        </div>
        
        <!-- 其他配置区域 -->
    </main>
</div>
```

### 6.2 服务器导入向导界面

```html
<!-- 步骤1: 选择导入源 -->
<div class="import-wizard-step active" data-step="1">
    <h2>选择导入源</h2>
    <div class="import-source-cards">
        <div class="import-source-card" data-source="securecrt">
            <div class="source-icon">
                <i class="fas fa-terminal"></i>
            </div>
            <h3>SecureCRT</h3>
            <p>从SecureCRT 7.0+导入会话配置</p>
            <span class="badge badge-primary">推荐</span>
        </div>
        
        <div class="import-source-card" data-source="xshell">
            <div class="source-icon">
                <i class="fas fa-terminal"></i>
            </div>
            <h3>Xshell</h3>
            <p>从Xshell 6.0+导入会话配置</p>
        </div>
        
        <div class="import-source-card disabled" data-source="other">
            <div class="source-icon">
                <i class="fas fa-ellipsis-h"></i>
            </div>
            <h3>其他</h3>
            <p>Tabby、MobaXterm等（即将推出）</p>
        </div>
    </div>
</div>

<!-- 步骤2: 配置文件扫描 -->
<div class="import-wizard-step" data-step="2">
    <h2>配置文件扫描</h2>
    
    <div class="import-method-tabs">
        <button class="tab-btn active" data-method="auto">
            <i class="fas fa-magic"></i> 自动扫描
        </button>
        <button class="tab-btn" data-method="manual">
            <i class="fas fa-folder"></i> 手动指定
        </button>
        <button class="tab-btn" data-method="upload">
            <i class="fas fa-upload"></i> 上传文件
        </button>
    </div>
    
    <div class="import-method-content">
        <!-- 自动扫描 -->
        <div class="method-panel active" data-method="auto">
            <div class="scan-info-box">
                <i class="fas fa-info-circle"></i>
                <div>
                    <h4>自动扫描说明</h4>
                    <p>系统将自动检测SecureCRT的默认安装路径和配置文件位置</p>
                    <ul>
                        <li>检查注册表获取实际配置路径（Windows）</li>
                        <li>扫描常见默认路径</li>
                        <li>从安装目录推断配置位置</li>
                    </ul>
                </div>
            </div>
            
            <div class="scan-progress hidden" id="scanProgress">
                <div class="progress-header">
                    <i class="fas fa-spinner fa-spin"></i>
                    <span>正在扫描配置文件...</span>
                </div>
                <div class="progress-bar-container">
                    <div class="progress-bar" id="scanProgressBar"></div>
                </div>
                <div class="scan-steps" id="scanSteps">
                    <div class="scan-step">✓ 检查注册表</div>
                    <div class="scan-step active">→ 扫描默认路径</div>
                    <div class="scan-step">⋯ 推断配置位置</div>
                </div>
            </div>
            
            <div class="scan-result hidden" id="scanResult">
                <!-- 扫描成功 -->
                <div class="scan-success" id="scanSuccessResult">
                    <div class="result-icon success">
                        <i class="fas fa-check-circle"></i>
                    </div>
                    <h3>扫描成功！</h3>
                    <div class="scan-details">
                        <table class="table table-sm">
                            <tr>
                                <td><strong>检测到软件</strong></td>
                                <td id="detectedSoftware">SecureCRT 9.0</td>
                            </tr>
                            <tr>
                                <td><strong>配置路径</strong></td>
                                <td id="detectedConfigPath">C:\Users\...\VanDyke\Config\Sessions</td>
                            </tr>
                            <tr>
                                <td><strong>会话数量</strong></td>
                                <td id="detectedSessionCount">25 个</td>
                            </tr>
                            <tr>
                                <td><strong>文件夹数量</strong></td>
                                <td id="detectedFolderCount">5 个</td>
                            </tr>
                            <tr>
                                <td><strong>预计导入时间</strong></td>
                                <td id="estimatedTime">约 2 分钟</td>
                            </tr>
                        </table>
                    </div>
                    <div class="scan-suggestions" id="scanSuggestions">
                        <!-- 动态生成建议 -->
                    </div>
                </div>
                
                <!-- 扫描失败 -->
                <div class="scan-failed hidden" id="scanFailedResult">
                    <div class="result-icon failed">
                        <i class="fas fa-times-circle"></i>
                    </div>
                    <h3>未找到配置文件</h3>
                    <p>未能在以下位置找到SecureCRT配置文件：</p>
                    <ul class="scanned-paths" id="scannedPaths">
                        <!-- 动态生成已扫描路径 -->
                    </ul>
                    <div class="failure-suggestions">
                        <p><strong>可能的原因：</strong></p>
                        <ul>
                            <li>SecureCRT未安装或使用便携版</li>
                            <li>配置文件位于非默认位置</li>
                            <li>没有访问配置目录的权限</li>
                        </ul>
                        <p><strong>解决方案：</strong></p>
                        <div class="solution-buttons">
                            <button class="btn btn-primary" onclick="switchToMethod('manual')">
                                <i class="fas fa-folder"></i> 手动指定目录
                            </button>
                            <button class="btn btn-secondary" onclick="switchToMethod('upload')">
                                <i class="fas fa-upload"></i> 上传配置文件
                            </button>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="import-actions">
                <button class="btn btn-primary btn-lg" id="autoScanBtn">
                    <i class="fas fa-search"></i> 开始自动扫描
                </button>
            </div>
        </div>
        
        <!-- 手动指定 -->
        <div class="method-panel" data-method="manual">
            <div class="form-group">
                <label>
                    <i class="fas fa-folder"></i> 配置方式
                </label>
                <div class="config-mode-selector">
                    <label class="radio-card active">
                        <input type="radio" name="configMode" value="config-dir" checked>
                        <div class="radio-card-content">
                            <i class="fas fa-folder-open"></i>
                            <span>指定配置目录</span>
                            <small>直接指定Sessions配置目录</small>
                        </div>
                    </label>
                    <label class="radio-card">
                        <input type="radio" name="configMode" value="install-dir">
                        <div class="radio-card-content">
                            <i class="fas fa-hdd"></i>
                            <span>指定安装目录</span>
                            <small>系统将自动推断配置路径</small>
                        </div>
                    </label>
                </div>
            </div>
            
            <div id="configDirInput">
                <div class="form-group">
                    <label>SecureCRT配置目录 (Sessions目录)</label>
                    <div class="input-with-browse">
                        <input type="text" 
                               id="configPath" 
                               placeholder="例如: C:\Users\YourName\AppData\Roaming\VanDyke\Config\Sessions"
                               class="form-control">
                        <button class="btn btn-secondary" id="browseConfigBtn">
                            <i class="fas fa-folder-open"></i> 浏览
                        </button>
                    </div>
                    <small class="form-text">
                        <i class="fas fa-info-circle"></i>
                        Windows默认: <code>%APPDATA%\VanDyke\Config\Sessions\</code><br>
                        macOS默认: <code>~/Library/Application Support/VanDyke/SecureCRT/Config/Sessions/</code><br>
                        Linux默认: <code>~/.vandyke/SecureCRT/Config/Sessions/</code>
                    </small>
                </div>
            </div>
            
            <div id="installDirInput" class="hidden">
                <div class="form-group">
                    <label>SecureCRT安装目录</label>
                    <div class="input-with-browse">
                        <input type="text" 
                               id="installPath" 
                               placeholder="例如: C:\Program Files\VanDyke Software\SecureCRT"
                               class="form-control">
                        <button class="btn btn-secondary" id="browseInstallBtn">
                            <i class="fas fa-folder-open"></i> 浏览
                        </button>
                    </div>
                    <small class="form-text">
                        <i class="fas fa-info-circle"></i>
                        Windows默认: <code>C:\Program Files\VanDyke Software\SecureCRT\</code><br>
                        macOS默认: <code>/Applications/SecureCRT.app/</code><br>
                        Linux默认: <code>/usr/local/bin/SecureCRT</code>
                    </small>
                    <div class="inferred-path-preview hidden" id="inferredPathPreview">
                        <i class="fas fa-arrow-right"></i>
                        <span>推断的配置路径: <strong id="inferredPath"></strong></span>
                    </div>
                </div>
            </div>
            
            <div class="import-actions">
                <button class="btn btn-primary" id="validatePathBtn">
                    <i class="fas fa-check-circle"></i> 验证路径
                </button>
                <button class="btn btn-success hidden" id="parseManualBtn">
                    <i class="fas fa-cogs"></i> 解析配置
                </button>
            </div>
        </div>
        
        <!-- 上传文件 -->
        <div class="method-panel" data-method="upload">
            <div class="upload-zone" id="uploadZone">
                <i class="fas fa-cloud-upload-alt"></i>
                <p>拖拽配置文件到此处，或点击选择文件</p>
                <input type="file" id="fileInput" multiple accept=".ini,.xsh,.yaml">
                <small class="upload-hint">
                    支持格式: .ini (SecureCRT), .xsh (Xshell), .yaml (Tabby)
                </small>
            </div>
            <div class="uploaded-files hidden" id="uploadedFilesList">
                <!-- 已上传文件列表 -->
            </div>
            <div class="import-actions">
                <button class="btn btn-primary hidden" id="parseUploadBtn">
                    <i class="fas fa-cogs"></i> 解析文件
                </button>
            </div>
        </div>
    </div>
</div>

<!-- 步骤3: 预览与确认 -->
<div class="import-wizard-step" data-step="3">
    <h2>预览与确认</h2>
    
    <div class="import-summary">
        <div class="summary-card">
            <span class="summary-label">解析成功</span>
            <span class="summary-value" id="totalServers">0</span>
        </div>
        <div class="summary-card warning">
            <span class="summary-label">需要密码</span>
            <span class="summary-value" id="needPasswordCount">0</span>
        </div>
        <div class="summary-card danger">
            <span class="summary-label">存在冲突</span>
            <span class="summary-value" id="conflictCount">0</span>
        </div>
        <div class="summary-card info">
            <span class="summary-label">新分组</span>
            <span class="summary-value" id="newGroupCount">0</span>
        </div>
    </div>
    
    <div class="import-preview-table">
        <table class="table">
            <thead>
                <tr>
                    <th><input type="checkbox" id="selectAll"></th>
                    <th>服务器名称</th>
                    <th>主机地址</th>
                    <th>端口</th>
                    <th>用户名</th>
                    <th>密码</th>
                    <th>分组</th>
                    <th>状态</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody id="serverPreviewList">
                <!-- 动态生成 -->
                <tr>
                    <td><input type="checkbox" checked></td>
                    <td>WebServer1</td>
                    <td>192.168.1.100</td>
                    <td>22</td>
                    <td>admin</td>
                    <td>
                        <input type="password" 
                               placeholder="输入密码" 
                               class="form-control form-control-sm">
                    </td>
                    <td>Production</td>
                    <td>
                        <span class="badge badge-success">就绪</span>
                    </td>
                    <td>
                        <button class="btn btn-sm btn-link">编辑</button>
                    </td>
                </tr>
                <tr class="conflict-row">
                    <td><input type="checkbox" checked></td>
                    <td>DBServer1</td>
                    <td>192.168.1.101</td>
                    <td>22</td>
                    <td>admin</td>
                    <td><span class="badge badge-secondary">已存在</span></td>
                    <td>Production</td>
                    <td>
                        <span class="badge badge-warning">冲突</span>
                    </td>
                    <td>
                        <select class="form-control form-control-sm">
                            <option>跳过</option>
                            <option>覆盖</option>
                            <option>重命名</option>
                        </select>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
    
    <div class="import-actions">
        <button class="btn btn-secondary" id="backBtn">
            <i class="fas fa-arrow-left"></i> 返回
        </button>
        <button class="btn btn-success" id="importBtn">
            <i class="fas fa-check"></i> 开始导入
        </button>
    </div>
</div>

<!-- 步骤4: 导入结果 -->
<div class="import-wizard-step" data-step="4">
    <h2>导入完成</h2>
    
    <div class="import-result">
        <div class="result-icon success">
            <i class="fas fa-check-circle"></i>
        </div>
        <h3>导入成功！</h3>
        <p>已成功导入 <strong id="importedCount">8</strong> 台服务器</p>
        
        <div class="result-details">
            <table class="table table-sm">
                <tr>
                    <td>总数</td>
                    <td id="resultTotal">10</td>
                </tr>
                <tr class="success">
                    <td>成功</td>
                    <td id="resultSuccess">8</td>
                </tr>
                <tr class="warning">
                    <td>跳过</td>
                    <td id="resultSkipped">2</td>
                </tr>
                <tr class="danger">
                    <td>失败</td>
                    <td id="resultFailed">0</td>
                </tr>
            </table>
        </div>
        
        <div class="result-actions">
            <button class="btn btn-primary" onclick="location.href='#servers'">
                <i class="fas fa-server"></i> 查看服务器列表
            </button>
            <button class="btn btn-secondary" id="newImportBtn">
                <i class="fas fa-redo"></i> 再次导入
            </button>
        </div>
    </div>
</div>
```

### 6.3 系统配置界面示例

```html
<!-- Metrics Hub配置 -->
<div id="metrics-hub" class="settings-section">
    <div class="settings-header">
        <h2>Metrics Hub 配置</h2>
        <p>配置指标收集中心的连接参数</p>
    </div>
    
    <form id="metricsHubForm">
        <div class="settings-card">
            <h3>基本配置</h3>
            
            <div class="form-group">
                <label class="form-label">
                    <input type="checkbox" id="hubEnabled" checked>
                    启用 Metrics Hub 集成
                </label>
                <small class="form-text">禁用后将使用SSH直接采集监控数据</small>
            </div>
            
            <div class="form-group">
                <label>服务地址</label>
                <input type="text" 
                       id="hubBaseUrl" 
                       value="http://localhost:8081"
                       class="form-control">
            </div>
            
            <div class="form-row">
                <div class="form-group col-md-6">
                    <label>连接超时 (ms)</label>
                    <input type="number" 
                           id="hubConnectTimeout" 
                           value="5000"
                           class="form-control">
                </div>
                <div class="form-group col-md-6">
                    <label>读取超时 (ms)</label>
                    <input type="number" 
                           id="hubReadTimeout" 
                           value="10000"
                           class="form-control">
                </div>
            </div>
        </div>
        
        <div class="settings-card">
            <h3>降级策略</h3>
            
            <div class="form-group">
                <label class="form-label">
                    <input type="checkbox" id="fallbackEnabled" checked>
                    启用自动降级
                </label>
                <small class="form-text">Hub不可用时自动切换到SSH监控</small>
            </div>
        </div>
        
        <div class="settings-actions">
            <button type="button" class="btn btn-secondary" id="testConnectionBtn">
                <i class="fas fa-plug"></i> 测试连接
            </button>
            <button type="submit" class="btn btn-primary">
                <i class="fas fa-save"></i> 保存配置
            </button>
        </div>
    </form>
</div>
```

---

## 安全性考虑

### 7.1 权限控制

| 配置类型 | 管理员 | 开发者 |
|---------|-------|-------|
| 服务器导入 | ✅ 读写 | ❌ 无权限 |
| 系统全局配置 | ✅ 读写 | 👁️ 只读 |
| 用户个人配置 | ✅ 读写 | ✅ 读写 |
| 告警配置 | ✅ 读写 | 👁️ 只读 |
| 分组管理 | ✅ 读写 | 👁️ 只读 |

### 7.2 密码处理

- **导入时**: 提示用户重新输入密码，不直接使用SecureCRT加密密码
- **存储**: 使用系统统一的`PasswordEncryptionService`加密
- **传输**: HTTPS + 加密传输
- **展示**: 脱敏显示（`***`）

### 7.3 审计日志

所有系统配置变更需记录审计日志：
```java
// 使用现有的 UserActivity 模型
activity.setActivityType("SYSTEM_CONFIG_CHANGE");
activity.setDescription("更新Metrics Hub配置: baseUrl=...");
activity.setTargetType("SYSTEM_CONFIG");
```

### 7.4 敏感信息保护

- 主密钥不在界面显示，只允许修改
- 数据库密码脱敏显示
- SMTP密码脱敏显示
- 导出配置时排除敏感字段

---

## 开发优先级

### P0 - 必须实现（第一期）

1. **SecureCRT自动扫描与导入**
   - 自动扫描SecureCRT配置文件（注册表+默认路径）
   - 手动指定配置目录/安装目录
   - 文件上传支持
   - 解析SecureCRT .ini文件
   - 导入向导界面（4步）
   - 冲突检测与处理
   - 密码重新输入机制

2. **基础系统配置界面**
   - Metrics Hub配置
   - Agent部署配置
   - 日志级别配置

3. **用户个人配置**
   - 主题切换
   - 语言选择
   - 通知设置

### P1 - 应该实现（第二期）

4. **Xshell自动扫描与导入**
   - 自动扫描Xshell配置文件
   - 解析Xshell .xsh文件（XML格式）
   - 集成到导入向导

5. **Tabby自动扫描与导入**
   - 自动扫描Tabby配置文件
   - 解析YAML格式配置
   - 集成到导入向导

6. **高级系统配置**
   - 告警阈值配置
   - 性能参数调优
   - 数据备份策略

7. **配置导出功能**
   - 导出当前系统配置
   - 配置模板管理

### P2 - 可以实现（第三期）

8. **更多导入源支持**
   - MobaXterm配置导入
   - PuTTY配置导入（注册表读取）
   - FinalShell配置导入

9. **智能推荐功能**
   - 根据服务器特征自动分组
   - 重复服务器智能合并
   - 配置质量评分

10. **配置版本管理**
    - 配置变更历史
    - 配置回滚
    - 配置比较

11. **批量操作增强**
    - 批量测试连接
    - 批量修改配置
    - 批量导出

---

## 验收标准

### 功能验收

#### 自动扫描功能
- [ ] Windows系统能够从注册表读取SecureCRT配置路径
- [ ] 能够扫描所有默认路径并列出可能的配置目录
- [ ] 扫描进度实时反馈，包含扫描步骤说明
- [ ] 扫描成功显示详细信息（软件版本、会话数量、预计导入时间）
- [ ] 扫描失败提供清晰的失败原因和解决方案
- [ ] 从安装目录正确推断配置文件路径

#### SecureCRT导入
- [ ] 能够解析SecureCRT 7.0+的.ini配置文件
- [ ] 正确提取服务器名称、主机地址、端口、用户名
- [ ] 正确识别文件夹结构并映射到服务器分组
- [ ] 检测到重复服务器时显示冲突提示
- [ ] 允许用户为加密密码的服务器重新输入密码
- [ ] 支持三种导入方式：自动扫描/手动指定/文件上传
- [ ] 导入成功后能在服务器列表中看到新服务器
- [ ] 导入的服务器能够正常连接

#### 手动指定功能
- [ ] 支持指定配置目录或安装目录两种方式
- [ ] 路径验证功能正常工作
- [ ] 从安装目录推断配置路径并显示预览
- [ ] 路径不存在或无效时给出明确提示

#### 文件上传功能
- [ ] 支持拖拽上传配置文件
- [ ] 支持多文件同时上传
- [ ] 显示已上传文件列表
- [ ] 文件格式验证正确（.ini/.xsh/.yaml）

#### 系统配置
- [ ] 能够修改Metrics Hub配置并保存
- [ ] 测试连接功能正常工作
- [ ] 配置修改后正确提示是否需要重启
- [ ] 日志级别修改后立即生效（无需重启）

#### 用户配置
- [ ] 主题切换后界面立即更新
- [ ] 语言切换后界面文本正确翻译
- [ ] 通知设置保存后生效
- [ ] 终端设置在SSH终端中生效

### 性能验收

- [ ] 解析100个服务器配置在5秒内完成
- [ ] 导入100个服务器在10秒内完成
- [ ] 配置页面加载时间<2秒
- [ ] 配置保存响应时间<500ms

### 安全验收

- [ ] 开发者无法访问系统全局配置
- [ ] 密码在界面和API响应中脱敏
- [ ] 密码使用系统加密方式存储
- [ ] 配置变更记录到审计日志
- [ ] 敏感配置不出现在前端代码中

### 用户体验验收

- [ ] 导入向导流程清晰，步骤明确
- [ ] 错误提示信息清晰，指导用户如何修复
- [ ] 配置项有详细的说明文字
- [ ] 支持配置预览和测试
- [ ] 操作成功后有明确的反馈

---

## 附录

### A. SecureCRT配置文件示例

```ini
# WebServer.ini
S:"Protocol Name"=SSH2
S:"Hostname"=192.168.1.100
D:"[SSH2] Port"=00000016
S:"Username"=admin
S:"Password"=u02:v1:encrypted_password_here
S:"Description"=Production Web Server
S:"Folder"=/Production/Web
S:"Color Scheme"=Solarized Dark
D:"Rows"=00000018
D:"Cols"=00000050
```

### B. 参考资料

- [SecureCRT配置文件格式文档](https://www.vandyke.com/support/securecrt/config_files.html)
- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [Thymeleaf Template Engine](https://www.thymeleaf.org/documentation.html)

### C. 技术栈

- **后端**: Spring Boot 3.x, Spring MVC, Spring Data JPA
- **前端**: Thymeleaf, TypeScript, Bootstrap 5
- **安全**: Spring Security, 自定义密码加密
- **数据库**: H2/PostgreSQL (取决于部署模式)

---

**文档结束**

**下一步行动**:
1. 评审需求分析文档
2. 设计数据库表结构
3. 实现SecureCRT解析器
4. 开发导入向导前端
5. 集成测试
