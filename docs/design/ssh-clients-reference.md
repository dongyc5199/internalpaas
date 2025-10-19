# SSH客户端配置导入 - 快速参考

**版本**: 1.0  
**日期**: 2025-10-18  
**用途**: 开发人员快速参考手册

---

## 🎯 支持的SSH客户端总览

| 软件名称 | 优先级 | 配置格式 | 扫描难度 | 实现状态 |
|---------|-------|---------|---------|---------|
| SecureCRT | P0 | INI | ⭐⭐ 中等 | 📋 待实现 |
| Xshell | P1 | XML | ⭐ 简单 | 📋 待实现 |
| Tabby | P1 | YAML | ⭐ 简单 | 📋 待实现 |
| MobaXterm | P2 | INI | ⭐⭐ 中等 | 📋 待实现 |
| PuTTY | P3 | Registry | ⭐⭐⭐ 困难 | 📋 待实现 |

---

## 📊 各客户端详细信息

### 1. SecureCRT

#### 基本信息
- **开发商**: VanDyke Software
- **支持平台**: Windows, macOS, Linux
- **配置格式**: INI
- **文件扩展名**: `.ini`

#### 默认路径（Windows）

```
安装路径:
  C:\Program Files\VanDyke Software\SecureCRT\
  C:\Program Files (x86)\VanDyke Software\SecureCRT\

配置路径:
  %APPDATA%\VanDyke\Config\Sessions\
  实际: C:\Users\{用户名}\AppData\Roaming\VanDyke\Config\Sessions\

注册表位置:
  HKEY_CURRENT_USER\Software\VanDyke\SecureCRT\Config Path
```

#### 默认路径（macOS）

```
安装路径:
  /Applications/SecureCRT.app/

配置路径:
  ~/Library/Application Support/VanDyke/SecureCRT/Config/Sessions/
```

#### 默认路径（Linux）

```
安装路径:
  /usr/local/bin/SecureCRT
  ~/securecrt/

配置路径:
  ~/.vandyke/SecureCRT/Config/Sessions/
```

#### 配置文件示例

```ini
# WebServer.ini
S:"Protocol Name"=SSH2
S:"Hostname"=192.168.1.100
D:"[SSH2] Port"=00000016
S:"Username"=admin
S:"Password"=u02:v1:encrypted_password_here
S:"Description"=Production Web Server
S:"Folder"=/Production/Web
```

#### 字段映射

| SecureCRT字段 | 类型 | Internal PaaS字段 | 转换规则 |
|--------------|------|------------------|---------|
| `S:"Session Name"` | String | `Server.name` | 直接 |
| `S:"Hostname"` | String | `Server.hostname` | 直接 |
| `D:"[SSH2] Port"` | Hex | `Server.sshPort` | Hex→Dec |
| `S:"Username"` | String | `Server.sshUsername` | 直接 |
| `S:"Password"` | Encrypted | - | 重新输入 |
| `S:"Description"` | String | `Server.description` | 直接 |
| `S:"Folder"` | String | `ServerGroup.name` | 路径映射 |
| `S:"Identity Filename"` | Path | `Server.sshKeyPath` | 直接 |

#### 扫描策略

```
1. 读取注册表 (Windows)
   → HKEY_CURRENT_USER\Software\VanDyke\SecureCRT\Config Path

2. 扫描默认路径
   → %APPDATA%\VanDyke\Config\Sessions\
   → %USERPROFILE%\Documents\VanDyke\Config\Sessions\

3. 从安装目录推断
   → 检测版本号 → 构造配置路径

4. 检查历史路径
   → 数据库中记录的上次成功路径
```

---

### 2. Xshell

#### 基本信息
- **开发商**: NetSarang
- **支持平台**: Windows
- **配置格式**: XML
- **文件扩展名**: `.xsh`

#### 默认路径（Windows）

```
安装路径:
  C:\Program Files\NetSarang\Xshell 7\
  C:\Program Files (x86)\NetSarang\Xshell 7\

配置路径:
  %USERPROFILE%\Documents\NetSarang Computer\7\Xshell\Sessions\
  实际: C:\Users\{用户名}\Documents\NetSarang Computer\7\Xshell\Sessions\

注册表位置:
  HKEY_CURRENT_USER\Software\NetSarang\Xshell\7\
```

#### 配置文件示例

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

#### 字段映射

| Xshell字段 | Internal PaaS字段 | 转换规则 |
|-----------|------------------|---------|
| `<title>` | `Server.name` | 直接 |
| `<host>` | `Server.hostname` | 直接 |
| `<port>` | `Server.sshPort` | 直接 |
| `<username>` | `Server.sshUsername` | 直接 |
| `<password>` | - | 重新输入 |
| `<description>` | `Server.description` | 直接 |
| `<folder>` | `ServerGroup.name` | 直接 |

#### 扫描策略

```
1. 读取注册表
   → HKEY_CURRENT_USER\Software\NetSarang\Xshell\{版本}\

2. 扫描默认路径
   → %USERPROFILE%\Documents\NetSarang Computer\{版本}\Xshell\Sessions\

3. 检查多个版本
   → Xshell 7, Xshell 6, Xshell 5

4. 从安装目录推断
   → 检测版本 → 构造配置路径
```

---

### 3. Tabby

#### 基本信息
- **开发商**: Tabby (开源)
- **支持平台**: Windows, macOS, Linux
- **配置格式**: YAML
- **文件扩展名**: `config.yaml`

#### 默认路径（Windows）

```
安装路径:
  %LOCALAPPDATA%\Programs\Tabby\
  实际: C:\Users\{用户名}\AppData\Local\Programs\Tabby\

配置文件:
  %APPDATA%\tabby\config.yaml
  实际: C:\Users\{用户名}\AppData\Roaming\tabby\config.yaml
```

#### 默认路径（macOS）

```
安装路径:
  /Applications/Tabby.app/

配置文件:
  ~/Library/Application Support/tabby/config.yaml
```

#### 默认路径（Linux）

```
安装路径:
  /usr/bin/tabby
  ~/.local/share/applications/

配置文件:
  ~/.config/tabby/config.yaml
```

#### 配置文件示例

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
      description: Production Server
```

#### 字段映射

| Tabby字段 | Internal PaaS字段 | 转换规则 |
|----------|------------------|---------|
| `name` | `Server.name` | 直接 |
| `host` | `Server.hostname` | 直接 |
| `port` | `Server.sshPort` | 直接 |
| `user` | `Server.sshUsername` | 直接 |
| `password` | - | 重新输入 |
| `description` | `Server.description` | 直接 |
| `group` | `ServerGroup.name` | 直接 |
| `privateKey` | `Server.sshKeyPath` | 直接 |

#### 扫描策略

```
1. 扫描配置文件
   → %APPDATA%\tabby\config.yaml (Windows)
   → ~/Library/Application Support/tabby/config.yaml (macOS)
   → ~/.config/tabby/config.yaml (Linux)

2. 检查安装目录
   → 便携版可能在安装目录下有config.yaml
```

---

### 4. MobaXterm

#### 基本信息
- **开发商**: Mobatek
- **支持平台**: Windows
- **配置格式**: INI
- **文件扩展名**: `.ini`, `.mxtsessions`

#### 默认路径（Windows）

```
安装路径（安装版）:
  C:\Program Files\Mobatek\MobaXterm\

安装路径（便携版）:
  {用户自定义目录}\MobaXterm_Personal\

配置文件（安装版）:
  %USERPROFILE%\Documents\MobaXterm\MobaXterm.ini

配置文件（便携版）:
  {安装目录}\MobaXterm.ini
```

#### 配置文件示例

```ini
[Bookmarks]
SubRep=MyServer
ImgNum=41
Session=Ssh://admin@192.168.1.100:22
```

#### 字段映射

| MobaXterm字段 | Internal PaaS字段 | 转换规则 |
|--------------|------------------|---------|
| `SubRep` | `Server.name` | 直接 |
| `Session` (URL) | 多个字段 | 解析URL |

#### 扫描策略

```
1. 扫描文档目录
   → %USERPROFILE%\Documents\MobaXterm\MobaXterm.ini

2. 查找安装目录
   → C:\Program Files\Mobatek\MobaXterm\

3. 检查便携版
   → 常见目录如 C:\MobaXterm\, D:\Tools\MobaXterm\
```

---

### 5. PuTTY

#### 基本信息
- **开发商**: Simon Tatham (开源)
- **支持平台**: Windows, Linux
- **配置格式**: Registry (Windows), 文件 (Linux)
- **文件扩展名**: `.reg` (导出)

#### 默认路径（Windows）

```
注册表位置:
  HKEY_CURRENT_USER\Software\SimonTatham\PuTTY\Sessions\

每个会话是一个注册表键:
  Sessions\
    ├── Default Settings
    ├── MyServer
    └── ProductionDB
```

#### 默认路径（Linux）

```
配置目录:
  ~/.putty/sessions/

每个文件是一个会话配置
```

#### 注册表示例

```
[HKEY_CURRENT_USER\Software\SimonTatham\PuTTY\Sessions\MyServer]
"HostName"="192.168.1.100"
"PortNumber"=dword:00000016
"UserName"="admin"
"Protocol"="ssh"
```

#### 字段映射

| PuTTY字段 | Internal PaaS字段 | 转换规则 |
|----------|------------------|---------|
| Session Name (键名) | `Server.name` | 直接 |
| `HostName` | `Server.hostname` | 直接 |
| `PortNumber` | `Server.sshPort` | DWORD→Int |
| `UserName` | `Server.sshUsername` | 直接 |
| `Protocol` | - | 验证为SSH |

#### 扫描策略

```
1. 读取注册表 (Windows)
   → HKEY_CURRENT_USER\Software\SimonTatham\PuTTY\Sessions\
   → 枚举所有子键

2. 扫描配置目录 (Linux)
   → ~/.putty/sessions/

注意: PuTTY不存储密码，总是需要用户输入
```

---

## 🛠️ 实现优先级建议

### Phase 1 (P0 - 必须)
- ✅ **SecureCRT**: 企业最常用，优先实现

### Phase 2 (P1 - 应该)
- ✅ **Xshell**: 国内用户多，XML格式简单
- ✅ **Tabby**: 开源工具，YAML格式简单

### Phase 3 (P2 - 可以)
- ⭕ **MobaXterm**: Windows工具，INI格式
- ⭕ **PuTTY**: 注册表读取复杂，但用户基数大

---

## 📝 开发检查清单

### 每个客户端都需要实现

- [ ] `ConfigScanner` 接口实现
- [ ] `ConfigParser` 接口实现
- [ ] 默认路径列表
- [ ] 注册表读取（如适用）
- [ ] 路径验证逻辑
- [ ] 配置文件解析
- [ ] 字段映射转换
- [ ] 密码处理策略
- [ ] 分组/文件夹映射
- [ ] 单元测试
- [ ] 集成测试
- [ ] 文档更新

---

## 🧪 测试用例模板

```java
@Test
void testAutoScan_Success() {
    // 准备: 创建模拟配置文件
    // 执行: 调用 scanner.autoScan()
    // 验证: 
    //   - scanStatus = SUCCESS
    //   - configPath 正确
    //   - sessionCount > 0
}

@Test
void testAutoScan_Failed() {
    // 准备: 删除所有配置文件
    // 执行: 调用 scanner.autoScan()
    // 验证: 
    //   - scanStatus = FAILED
    //   - scannedPaths 包含所有尝试路径
}

@Test
void testParseConfig_ValidFile() {
    // 准备: 准备合法的配置文件
    // 执行: 调用 parser.parseConfig()
    // 验证: 
    //   - 解析出正确数量的服务器
    //   - 字段映射正确
}

@Test
void testInferConfigPath() {
    // 准备: 提供安装目录
    // 执行: 调用 scanner.inferConfigPath()
    // 验证: 返回正确的配置路径
}
```

---

## 📚 参考资料

- **SecureCRT**: [配置文件文档](https://www.vandyke.com/support/securecrt/config_files.html)
- **Xshell**: [会话管理](https://www.netsarang.com/zh/xshell/)
- **Tabby**: [GitHub仓库](https://github.com/Eugeny/tabby)
- **MobaXterm**: [官方文档](https://mobaxterm.mobatek.net/documentation.html)
- **PuTTY**: [配置说明](https://www.chiark.greenend.org.uk/~sgtatham/putty/docs.html)

---

**最后更新**: 2025-10-18  
**维护者**: AI Assistant
