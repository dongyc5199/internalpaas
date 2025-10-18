# SSH配置导入并映射服务器 - 设计方案

> **文档版本**: v1.0  
> **最后更新**: 2025年10月18日  
> **设计目标**: 通过扫描客户端SSH配置文件，自动导入服务器信息并映射到本系统

---

## 📋 目录

- [业务背景](#业务背景)
- [功能概述](#功能概述)
- [核心流程](#核心流程)
- [技术设计](#技术设计)
- [数据模型](#数据模型)
- [接口设计](#接口设计)
- [前端设计](#前端设计)
- [映射规则](#映射规则)
- [安全考虑](#安全考虑)
- [实施计划](#实施计划)

---

## 业务背景

### 痛点分析

1. **手动添加服务器效率低**: 用户在本地已有完整的SSH配置，但需要在系统中重复录入
2. **配置信息不一致**: 手动录入容易出错，导致连接失败
3. **批量导入需求**: 运维人员管理数十台服务器，逐个添加工作量大
4. **配置同步困难**: 本地SSH配置更新后，系统配置需要同步

### 业务价值

- **提升效率**: 一键导入多台服务器，节省90%的录入时间
- **减少错误**: 自动解析配置，避免手动输入错误
- **快速上手**: 新用户可快速导入现有服务器配置
- **配置同步**: 支持定期同步本地SSH配置变更

---

## 功能概述

### 核心功能

1. **SSH配置文件扫描**
   - 支持Linux/Mac的`~/.ssh/config`
   - 支持Windows的`%USERPROFILE%\.ssh\config`
   - 支持上传自定义配置文件

2. **配置解析与映射**
   - 解析SSH配置中的Host、HostName、Port、User等字段
   - 智能映射到系统Server模型
   - 支持配置预览和编辑

3. **批量导入**
   - 勾选需要导入的服务器
   - 批量创建Server实体
   - 自动触发连接测试

4. **增量同步**
   - 检测配置文件变更
   - 新增配置自动导入
   - 已存在配置支持更新或跳过

### 功能边界

**包含**:
- ✅ SSH配置文件解析
- ✅ 服务器信息映射
- ✅ 批量导入入库
- ✅ 导入结果反馈
- ✅ 连接测试验证

**不包含**:
- ❌ SSH私钥文件导入（安全考虑，仅记录路径）
- ❌ 自动获取SSH密码（需用户手动输入）
- ❌ 修改本地SSH配置文件
- ❌ 实时监听配置文件变化

---

## 核心流程

### 整体流程图

```
┌──────────────────────────────────────────────────────────────┐
│ 1. 配置文件获取                                               │
│    ├─ 自动扫描本地 ~/.ssh/config                              │
│    ├─ 手动上传配置文件                                        │
│    └─ 输入配置文件路径                                        │
└────────────────┬─────────────────────────────────────────────┘
                 ↓
┌──────────────────────────────────────────────────────────────┐
│ 2. SSH配置解析                                                │
│    ├─ 解析Host块                                              │
│    ├─ 提取HostName、Port、User、IdentityFile等               │
│    ├─ 处理Include指令                                         │
│    └─ 处理通配符和正则                                        │
└────────────────┬─────────────────────────────────────────────┘
                 ↓
┌──────────────────────────────────────────────────────────────┐
│ 3. 数据映射与验证                                             │
│    ├─ SSH配置 → Server模型映射                                │
│    ├─ 字段完整性检查                                          │
│    ├─ 去重检查（Hostname+Port）                               │
│    └─ 生成预览数据                                            │
└────────────────┬─────────────────────────────────────────────┘
                 ↓
┌──────────────────────────────────────────────────────────────┐
│ 4. 用户确认与补充                                             │
│    ├─ 展示解析结果表格                                        │
│    ├─ 勾选需要导入的服务器                                    │
│    ├─ 补充缺失字段（密码、工作目录等）                        │
│    └─ 编辑映射结果                                            │
└────────────────┬─────────────────────────────────────────────┘
                 ↓
┌──────────────────────────────────────────────────────────────┐
│ 5. 批量导入执行                                               │
│    ├─ 遍历勾选的服务器                                        │
│    ├─ 创建Server实体                                          │
│    ├─ 密码加密存储                                            │
│    ├─ 保存到数据库                                            │
│    └─ 触发异步连接测试                                        │
└────────────────┬─────────────────────────────────────────────┘
                 ↓
┌──────────────────────────────────────────────────────────────┐
│ 6. 结果反馈                                                   │
│    ├─ 导入成功数量                                            │
│    ├─ 失败详情（重复、验证失败）                              │
│    ├─ 连接测试状态                                            │
│    └─ 跳转到服务器列表                                        │
└──────────────────────────────────────────────────────────────┘
```

### 详细步骤说明

#### 步骤1: 配置文件获取

**方式A - 自动扫描**:
```java
// 后端自动检测当前用户的SSH配置文件
String userHome = System.getProperty("user.home");
Path sshConfigPath = Paths.get(userHome, ".ssh", "config");
if (Files.exists(sshConfigPath)) {
    String configContent = Files.readString(sshConfigPath);
    // 解析配置...
}
```

**方式B - 文件上传**:
```html
<input type="file" accept=".config,.txt" @change="handleFileUpload">
```

**方式C - 路径输入**:
```html
<input type="text" placeholder="例如: /home/user/.ssh/config" v-model="configPath">
```

#### 步骤2: SSH配置解析

**SSH配置文件示例**:
```ssh-config
# Development Servers
Host dev-server-01
    HostName 192.168.1.100
    Port 22
    User devuser
    IdentityFile ~/.ssh/id_rsa_dev

Host prod-server-*
    HostName 10.0.%h.10
    Port 2222
    User produser
    IdentityFile ~/.ssh/id_rsa_prod
    
Host staging
    HostName staging.example.com
    User admin
    ProxyJump bastion.example.com
```

**解析逻辑**:
```java
public class SSHConfigParser {
    
    public List<SSHHostConfig> parseConfig(String configContent) {
        List<SSHHostConfig> hosts = new ArrayList<>();
        SSHHostConfig currentHost = null;
        
        for (String line : configContent.split("\\n")) {
            line = line.trim();
            
            // 跳过注释和空行
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // 解析Host指令
            if (line.startsWith("Host ")) {
                if (currentHost != null) {
                    hosts.add(currentHost);
                }
                String hostPattern = line.substring(5).trim();
                currentHost = new SSHHostConfig(hostPattern);
            }
            // 解析配置项
            else if (currentHost != null) {
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    String key = parts[0];
                    String value = parts[1];
                    
                    switch (key) {
                        case "HostName":
                            currentHost.setHostname(value);
                            break;
                        case "Port":
                            currentHost.setPort(Integer.parseInt(value));
                            break;
                        case "User":
                            currentHost.setUser(value);
                            break;
                        case "IdentityFile":
                            currentHost.setIdentityFile(expandPath(value));
                            break;
                        // 其他配置项...
                    }
                }
            }
        }
        
        // 添加最后一个Host
        if (currentHost != null) {
            hosts.add(currentHost);
        }
        
        return hosts;
    }
    
    private String expandPath(String path) {
        if (path.startsWith("~/")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }
}
```

#### 步骤3: 数据映射与验证

**映射规则**:
```java
public class SSHConfigMapper {
    
    public ServerImportDto mapToServer(SSHHostConfig sshConfig) {
        ServerImportDto dto = new ServerImportDto();
        
        // 基本信息映射
        dto.setName(sshConfig.getHostPattern());  // Host名称作为服务器名
        dto.setHostname(sshConfig.getHostname()); // HostName作为主机名
        dto.setSshPort(sshConfig.getPort() != null ? sshConfig.getPort() : 22);
        dto.setSshUsername(sshConfig.getUser());
        dto.setSshKeyPath(sshConfig.getIdentityFile());
        
        // 生成默认值
        dto.setPort(8080); // 应用端口默认值
        dto.setBaseWorkDirectory("/home/" + sshConfig.getUser());
        dto.setDescription("从SSH配置导入: " + sshConfig.getHostPattern());
        dto.setServerType(Server.ServerType.DEVELOPMENT); // 默认开发服务器
        
        // 标记字段完整性
        dto.setValid(isValid(dto));
        dto.setMissingFields(getMissingFields(dto));
        
        return dto;
    }
    
    private boolean isValid(ServerImportDto dto) {
        return dto.getName() != null && 
               dto.getHostname() != null && 
               dto.getSshUsername() != null;
    }
    
    private List<String> getMissingFields(ServerImportDto dto) {
        List<String> missing = new ArrayList<>();
        if (dto.getSshKeyPath() == null) {
            missing.add("SSH私钥路径");
        }
        // 可以添加更多必填字段检查
        return missing;
    }
}
```

#### 步骤4: 用户确认与补充

**预览表格**:
```typescript
interface ServerImportPreview {
  id: string;                    // 临时ID（前端生成）
  selected: boolean;             // 是否勾选导入
  name: string;                  // 服务器名称
  hostname: string;              // 主机名/IP
  sshPort: number;               // SSH端口
  sshUsername: string;           // SSH用户名
  sshKeyPath?: string;           // SSH私钥路径
  sshPassword?: string;          // SSH密码（需补充）
  port: number;                  // 应用端口
  baseWorkDirectory: string;     // 工作目录
  serverType: string;            // 服务器类型
  description: string;           // 描述
  valid: boolean;                // 是否有效
  missingFields: string[];       // 缺失字段
  duplicate: boolean;            // 是否重复
  duplicateWith?: string;        // 重复的服务器名
}
```

**前端交互**:
```vue
<template>
  <div class="ssh-import-wizard">
    <!-- 步骤1: 上传配置 -->
    <div v-if="step === 1">
      <h3>上传SSH配置文件</h3>
      <file-upload @upload="handleConfigUpload" />
    </div>
    
    <!-- 步骤2: 预览与编辑 -->
    <div v-if="step === 2">
      <h3>选择要导入的服务器</h3>
      <el-table :data="servers" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="hostname" label="主机名" />
        <el-table-column prop="sshPort" label="SSH端口" />
        <el-table-column prop="sshUsername" label="用户名" />
        <el-table-column label="密码">
          <template #default="{ row }">
            <el-input 
              type="password" 
              v-model="row.sshPassword" 
              placeholder="请输入SSH密码"
              show-password
            />
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag v-if="row.duplicate" type="danger">重复</el-tag>
            <el-tag v-else-if="!row.valid" type="warning">缺失字段</el-tag>
            <el-tag v-else type="success">就绪</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" @click="editServer(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="actions">
        <el-button @click="step = 1">上一步</el-button>
        <el-button 
          type="primary" 
          @click="importSelected"
          :disabled="selectedServers.length === 0"
        >
          导入选中的服务器 ({{ selectedServers.length }})
        </el-button>
      </div>
    </div>
    
    <!-- 步骤3: 导入结果 -->
    <div v-if="step === 3">
      <h3>导入结果</h3>
      <el-result 
        :icon="importResult.success ? 'success' : 'warning'"
        :title="`成功导入 ${importResult.successCount} 台服务器`"
      >
        <template #subTitle>
          <div v-if="importResult.failedCount > 0">
            失败 {{ importResult.failedCount }} 台
          </div>
        </template>
        <template #extra>
          <el-button type="primary" @click="goToServerList">
            查看服务器列表
          </el-button>
        </template>
      </el-result>
      
      <!-- 失败详情 -->
      <div v-if="importResult.failures.length > 0">
        <h4>失败详情</h4>
        <el-table :data="importResult.failures">
          <el-table-column prop="name" label="服务器名称" />
          <el-table-column prop="reason" label="失败原因" />
        </el-table>
      </div>
    </div>
  </div>
</template>
```

#### 步骤5: 批量导入执行

**Service层实现**:
```java
@Service
@Transactional
public class SSHConfigImportService {
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private ServerRepository serverRepository;
    
    @Autowired
    private PasswordEncryptionService passwordEncryptionService;
    
    /**
     * 批量导入服务器
     */
    public ServerImportResult batchImport(List<ServerImportDto> servers) {
        ServerImportResult result = new ServerImportResult();
        
        for (ServerImportDto dto : servers) {
            try {
                // 1. 去重检查
                if (isDuplicate(dto)) {
                    result.addFailure(dto.getName(), "服务器已存在（Hostname+Port重复）");
                    continue;
                }
                
                // 2. 字段验证
                List<String> errors = validate(dto);
                if (!errors.isEmpty()) {
                    result.addFailure(dto.getName(), "验证失败: " + String.join(", ", errors));
                    continue;
                }
                
                // 3. 创建Server实体
                Server server = convertToServer(dto);
                
                // 4. 设置密码（自动加密）
                if (dto.getSshPassword() != null && !dto.getSshPassword().isEmpty()) {
                    server = serverService.createServerWithPassword(server, dto.getSshPassword());
                } else {
                    server = serverService.saveServer(server);
                }
                
                // 5. 异步触发连接测试
                triggerConnectionTest(server.getId());
                
                result.addSuccess(server);
                
            } catch (Exception e) {
                logger.error("导入服务器失败: {}", dto.getName(), e);
                result.addFailure(dto.getName(), e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * 检查是否重复
     */
    private boolean isDuplicate(ServerImportDto dto) {
        return serverRepository.existsByHostnameAndSshPort(
            dto.getHostname(), 
            dto.getSshPort()
        );
    }
    
    /**
     * 字段验证
     */
    private List<String> validate(ServerImportDto dto) {
        List<String> errors = new ArrayList<>();
        
        if (dto.getName() == null || dto.getName().isEmpty()) {
            errors.add("服务器名称不能为空");
        }
        if (dto.getHostname() == null || dto.getHostname().isEmpty()) {
            errors.add("主机名不能为空");
        }
        if (dto.getSshUsername() == null || dto.getSshUsername().isEmpty()) {
            errors.add("SSH用户名不能为空");
        }
        if (dto.getSshPassword() == null && dto.getSshKeyPath() == null) {
            errors.add("SSH密码和私钥路径至少需要一个");
        }
        
        return errors;
    }
    
    /**
     * DTO转换为Server实体
     */
    private Server convertToServer(ServerImportDto dto) {
        Server server = new Server();
        server.setName(dto.getName());
        server.setHostname(dto.getHostname());
        server.setSshPort(dto.getSshPort());
        server.setSshUsername(dto.getSshUsername());
        server.setSshKeyPath(dto.getSshKeyPath());
        server.setPort(dto.getPort());
        server.setBaseWorkDirectory(dto.getBaseWorkDirectory());
        server.setDescription(dto.getDescription());
        server.setServerType(dto.getServerType());
        server.setActive(true);
        server.setAutoMonitorEnabled(true);
        server.setConnectionStatus(Server.ConnectionStatus.UNKNOWN);
        
        return server;
    }
    
    /**
     * 触发异步连接测试
     */
    private void triggerConnectionTest(Long serverId) {
        CompletableFuture.runAsync(() -> {
            try {
                serverService.checkServerConnectionAndMetrics(serverId);
            } catch (Exception e) {
                logger.error("服务器连接测试失败: {}", serverId, e);
            }
        });
    }
}
```

#### 步骤6: 结果反馈

**导入结果DTO**:
```java
@Data
public class ServerImportResult {
    private int successCount = 0;
    private int failedCount = 0;
    private List<Server> successServers = new ArrayList<>();
    private List<ImportFailure> failures = new ArrayList<>();
    
    public void addSuccess(Server server) {
        successCount++;
        successServers.add(server);
    }
    
    public void addFailure(String serverName, String reason) {
        failedCount++;
        failures.add(new ImportFailure(serverName, reason));
    }
    
    @Data
    @AllArgsConstructor
    public static class ImportFailure {
        private String serverName;
        private String reason;
    }
}
```

---

## 技术设计

### 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                         前端层                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 文件上传组件 │  │ 预览表格组件 │  │ 结果展示组件 │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                 │                 │               │
│         └─────────────────┼─────────────────┘               │
└───────────────────────────┼─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      Controller层                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ SSHConfigImportController                            │   │
│  │  - uploadConfig()      上传配置文件                  │   │
│  │  - parseConfig()       解析配置                      │   │
│  │  - previewServers()    预览服务器列表                │   │
│  │  - batchImport()       批量导入                      │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────────┼─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                       Service层                              │
│  ┌──────────────────┐  ┌──────────────────┐                 │
│  │SSHConfigParser   │  │SSHConfigImport   │                 │
│  │                  │  │Service           │                 │
│  │- parseConfig()   │  │- batchImport()   │                 │
│  │- parseHostBlock()│  │- validate()      │                 │
│  │- expandPath()    │  │- convertToServer()                │
│  └──────────────────┘  └────────┬─────────┘                 │
│  ┌──────────────────┐            │                          │
│  │SSHConfigMapper   │←───────────┘                          │
│  │- mapToServer()   │                                       │
│  │- isValid()       │                                       │
│  └──────────────────┘                                       │
└───────────────────────────┼─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      持久化层                                │
│  ┌──────────────────┐  ┌──────────────────┐                 │
│  │ServerRepository  │  │PasswordEncryption│                 │
│  │                  │  │Service           │                 │
│  │- save()          │  │- encryptPassword()                │
│  │- existsByHostname│  └──────────────────┘                 │
│  │  AndSshPort()    │                                       │
│  └──────────────────┘                                       │
└─────────────────────────────────────────────────────────────┘
```

### 技术选型

| 层次 | 技术 | 说明 |
|------|------|------|
| **前端** | Vue 3 + TypeScript | 响应式UI，类型安全 |
| **UI组件** | Element Plus | 表格、文件上传、对话框 |
| **HTTP客户端** | Axios | RESTful API调用 |
| **后端框架** | Spring Boot 3.x | Java企业级应用框架 |
| **配置解析** | 自定义解析器 | 轻量级SSH配置解析 |
| **文件上传** | MultipartFile | Spring标准文件上传 |
| **密码加密** | PasswordEncryptionService | AES加密存储 |
| **异步处理** | CompletableFuture | 异步连接测试 |
| **数据库** | JPA + MySQL | 服务器信息持久化 |

---

## 数据模型

### DTO定义

```java
/**
 * SSH配置Host块
 */
@Data
public class SSHHostConfig {
    private String hostPattern;        // Host名称（可能包含通配符）
    private String hostname;           // HostName
    private Integer port;              // Port
    private String user;               // User
    private String identityFile;       // IdentityFile路径
    private String proxyJump;          // ProxyJump（跳板机）
    private Map<String, String> extraOptions; // 其他配置项
    
    public SSHHostConfig(String hostPattern) {
        this.hostPattern = hostPattern;
        this.extraOptions = new HashMap<>();
    }
}

/**
 * 服务器导入DTO
 */
@Data
public class ServerImportDto {
    // 从SSH配置映射的字段
    private String name;                    // 服务器名称（来自Host）
    private String hostname;                // 主机名（来自HostName）
    private Integer sshPort;                // SSH端口（来自Port）
    private String sshUsername;             // SSH用户名（来自User）
    private String sshKeyPath;              // SSH私钥路径（来自IdentityFile）
    
    // 需要用户补充的字段
    private String sshPassword;             // SSH密码（需手动输入）
    private Integer port;                   // 应用端口（默认8080）
    private String baseWorkDirectory;       // 工作目录（默认/home/user）
    private String description;             // 描述
    private Server.ServerType serverType;   // 服务器类型（默认DEVELOPMENT）
    
    // 验证相关字段
    private boolean valid;                  // 是否通过验证
    private List<String> missingFields;     // 缺失的字段
    private boolean duplicate;              // 是否重复
    private String duplicateWith;           // 与哪个服务器重复
}

/**
 * 解析结果
 */
@Data
public class SSHConfigParseResult {
    private int totalHosts;                        // 解析到的Host总数
    private List<ServerImportDto> servers;         // 可导入的服务器列表
    private List<String> warnings;                 // 警告信息
    private List<String> errors;                   // 错误信息
}

/**
 * 导入结果
 */
@Data
public class ServerImportResult {
    private int successCount;
    private int failedCount;
    private List<Server> successServers;
    private List<ImportFailure> failures;
    
    @Data
    @AllArgsConstructor
    public static class ImportFailure {
        private String serverName;
        private String reason;
    }
}
```

### Repository扩展

```java
public interface ServerRepository extends JpaRepository<Server, Long> {
    
    // 现有方法...
    
    /**
     * 检查服务器是否已存在（根据主机名和SSH端口）
     */
    boolean existsByHostnameAndSshPort(String hostname, Integer sshPort);
    
    /**
     * 根据主机名和SSH端口查找服务器
     */
    Optional<Server> findByHostnameAndSshPort(String hostname, Integer sshPort);
    
    /**
     * 查找所有主机名
     */
    @Query("SELECT s.hostname FROM Server s")
    List<String> findAllHostnames();
}
```

---

## 接口设计

### REST API定义

```java
@RestController
@RequestMapping("/api/ssh-config-import")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class SSHConfigImportController {
    
    @Autowired
    private SSHConfigImportService importService;
    
    /**
     * 上传并解析SSH配置文件
     * 
     * POST /api/ssh-config-import/upload
     * Content-Type: multipart/form-data
     * 
     * @param file SSH配置文件
     * @return 解析结果
     */
    @PostMapping("/upload")
    public ResponseEntity<SSHConfigParseResult> uploadConfig(
            @RequestParam("file") MultipartFile file) {
        try {
            String configContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            SSHConfigParseResult result = importService.parseConfig(configContent);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("上传配置文件失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 解析本地SSH配置文件
     * 
     * POST /api/ssh-config-import/parse-local
     * 
     * @param path 配置文件路径（可选，默认~/.ssh/config）
     * @return 解析结果
     */
    @PostMapping("/parse-local")
    public ResponseEntity<SSHConfigParseResult> parseLocalConfig(
            @RequestParam(required = false) String path) {
        try {
            SSHConfigParseResult result = importService.parseLocalConfig(path);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("解析本地配置文件失败", e);
            return ResponseEntity.badRequest()
                .body(SSHConfigParseResult.error("解析失败: " + e.getMessage()));
        }
    }
    
    /**
     * 预览导入（去重检查）
     * 
     * POST /api/ssh-config-import/preview
     * Content-Type: application/json
     * 
     * @param servers 待导入的服务器列表
     * @return 预览结果（标记重复项）
     */
    @PostMapping("/preview")
    public ResponseEntity<List<ServerImportDto>> previewImport(
            @RequestBody List<ServerImportDto> servers) {
        List<ServerImportDto> preview = importService.previewImport(servers);
        return ResponseEntity.ok(preview);
    }
    
    /**
     * 批量导入服务器
     * 
     * POST /api/ssh-config-import/batch
     * Content-Type: application/json
     * 
     * @param servers 待导入的服务器列表
     * @return 导入结果
     */
    @PostMapping("/batch")
    public ResponseEntity<ServerImportResult> batchImport(
            @RequestBody List<ServerImportDto> servers) {
        try {
            ServerImportResult result = importService.batchImport(servers);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("批量导入服务器失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取默认配置文件路径
     * 
     * GET /api/ssh-config-import/default-path
     * 
     * @return 默认SSH配置文件路径
     */
    @GetMapping("/default-path")
    public ResponseEntity<Map<String, String>> getDefaultConfigPath() {
        String userHome = System.getProperty("user.home");
        String defaultPath = Paths.get(userHome, ".ssh", "config").toString();
        boolean exists = Files.exists(Paths.get(defaultPath));
        
        Map<String, String> response = new HashMap<>();
        response.put("path", defaultPath);
        response.put("exists", String.valueOf(exists));
        
        return ResponseEntity.ok(response);
    }
}
```

### API请求/响应示例

**1. 上传配置文件**

```http
POST /api/ssh-config-import/upload
Content-Type: multipart/form-data

--boundary
Content-Disposition: form-data; name="file"; filename="config"
Content-Type: text/plain

Host dev-server
    HostName 192.168.1.100
    Port 22
    User devuser
--boundary--
```

响应:
```json
{
  "totalHosts": 1,
  "servers": [
    {
      "name": "dev-server",
      "hostname": "192.168.1.100",
      "sshPort": 22,
      "sshUsername": "devuser",
      "port": 8080,
      "baseWorkDirectory": "/home/devuser",
      "serverType": "DEVELOPMENT",
      "valid": false,
      "missingFields": ["SSH密码或私钥路径"],
      "duplicate": false
    }
  ],
  "warnings": [],
  "errors": []
}
```

**2. 批量导入**

```http
POST /api/ssh-config-import/batch
Content-Type: application/json

[
  {
    "name": "dev-server",
    "hostname": "192.168.1.100",
    "sshPort": 22,
    "sshUsername": "devuser",
    "sshPassword": "password123",
    "port": 8080,
    "baseWorkDirectory": "/home/devuser",
    "serverType": "DEVELOPMENT"
  }
]
```

响应:
```json
{
  "successCount": 1,
  "failedCount": 0,
  "successServers": [
    {
      "id": 123,
      "name": "dev-server",
      "hostname": "192.168.1.100",
      "connectionStatus": "UNKNOWN"
    }
  ],
  "failures": []
}
```

---

## 前端设计

### 页面入口

在服务器管理页面添加"导入SSH配置"按钮：

```html
<!-- src/main/resources/templates/admin/servers-page.html -->
<div class="page-header">
  <h1>服务器管理</h1>
  <div class="actions">
    <button class="btn-primary" @click="openImportWizard">
      <i class="icon-upload"></i> 导入SSH配置
    </button>
    <button class="btn-primary" @click="openAddServerDrawer">
      <i class="icon-plus"></i> 添加服务器
    </button>
  </div>
</div>
```

### 导入向导组件

**文件**: `src/main/frontend/modules/SSHConfigImportWizard.ts`

```typescript
export class SSHConfigImportWizard {
  private step: number = 1;
  private servers: ServerImportDto[] = [];
  private selectedServers: ServerImportDto[] = [];
  
  /**
   * 打开导入向导
   */
  public open(): void {
    this.step = 1;
    this.servers = [];
    this.selectedServers = [];
    this.showModal();
  }
  
  /**
   * 处理文件上传
   */
  public async handleFileUpload(file: File): Promise<void> {
    const formData = new FormData();
    formData.append('file', file);
    
    try {
      const response = await axios.post<SSHConfigParseResult>(
        '/api/ssh-config-import/upload',
        formData
      );
      
      this.servers = response.data.servers;
      this.step = 2; // 进入预览步骤
      
    } catch (error) {
      console.error('上传失败', error);
      this.showError('配置文件上传失败');
    }
  }
  
  /**
   * 扫描本地配置
   */
  public async scanLocalConfig(): Promise<void> {
    try {
      const response = await axios.post<SSHConfigParseResult>(
        '/api/ssh-config-import/parse-local'
      );
      
      this.servers = response.data.servers;
      this.step = 2;
      
    } catch (error) {
      console.error('扫描失败', error);
      this.showError('扫描本地配置失败');
    }
  }
  
  /**
   * 批量导入选中的服务器
   */
  public async importSelected(): Promise<void> {
    if (this.selectedServers.length === 0) {
      this.showWarning('请至少选择一台服务器');
      return;
    }
    
    try {
      const response = await axios.post<ServerImportResult>(
        '/api/ssh-config-import/batch',
        this.selectedServers
      );
      
      this.showImportResult(response.data);
      this.step = 3;
      
    } catch (error) {
      console.error('导入失败', error);
      this.showError('批量导入失败');
    }
  }
}
```

### UI组件设计

**步骤1 - 配置文件选择**:
```vue
<div class="step step-1">
  <h3>选择SSH配置来源</h3>
  
  <div class="options">
    <!-- 选项1: 扫描本地 -->
    <div class="option-card" @click="scanLocalConfig">
      <i class="icon-scan"></i>
      <h4>扫描本地配置</h4>
      <p>自动读取 ~/.ssh/config</p>
    </div>
    
    <!-- 选项2: 上传文件 -->
    <div class="option-card">
      <i class="icon-upload"></i>
      <h4>上传配置文件</h4>
      <el-upload
        drag
        action="#"
        :auto-upload="false"
        :on-change="handleFileUpload"
        accept=".config,.txt"
      >
        <p>点击或拖拽文件到此处</p>
      </el-upload>
    </div>
    
    <!-- 选项3: 输入路径 -->
    <div class="option-card">
      <i class="icon-folder"></i>
      <h4>指定配置路径</h4>
      <el-input 
        v-model="customPath" 
        placeholder="/path/to/config"
      />
      <el-button @click="parseCustomPath">解析</el-button>
    </div>
  </div>
</div>
```

**步骤2 - 预览与编辑**:
```vue
<div class="step step-2">
  <h3>选择要导入的服务器 ({{ selectedServers.length }}/{{ servers.length }})</h3>
  
  <!-- 批量操作 -->
  <div class="batch-actions">
    <el-button size="small" @click="selectAll">全选</el-button>
    <el-button size="small" @click="selectValid">选择有效</el-button>
    <el-button size="small" @click="deselectAll">取消全选</el-button>
  </div>
  
  <!-- 服务器列表 -->
  <el-table 
    :data="servers" 
    @selection-change="handleSelectionChange"
    :row-class-name="getRowClassName"
  >
    <el-table-column type="selection" width="55" />
    
    <el-table-column prop="name" label="名称" width="150">
      <template #default="{ row }">
        <el-input v-model="row.name" size="small" />
      </template>
    </el-table-column>
    
    <el-table-column prop="hostname" label="主机名" width="150" />
    
    <el-table-column prop="sshPort" label="SSH端口" width="100" />
    
    <el-table-column prop="sshUsername" label="用户名" width="120" />
    
    <el-table-column label="密码" width="150">
      <template #default="{ row }">
        <el-input 
          v-model="row.sshPassword" 
          type="password" 
          placeholder="请输入"
          show-password
          size="small"
        />
      </template>
    </el-table-column>
    
    <el-table-column prop="serverType" label="类型" width="120">
      <template #default="{ row }">
        <el-select v-model="row.serverType" size="small">
          <el-option label="开发" value="DEVELOPMENT" />
          <el-option label="测试" value="TESTING" />
          <el-option label="预生产" value="STAGING" />
          <el-option label="生产" value="PRODUCTION" />
        </el-select>
      </template>
    </el-table-column>
    
    <el-table-column label="状态" width="120">
      <template #default="{ row }">
        <el-tag v-if="row.duplicate" type="danger" size="small">
          重复
        </el-tag>
        <el-tag v-else-if="!row.valid" type="warning" size="small">
          缺失字段
        </el-tag>
        <el-tag v-else type="success" size="small">
          就绪
        </el-tag>
      </template>
    </el-table-column>
    
    <el-table-column label="操作" width="150">
      <template #default="{ row }">
        <el-button size="small" @click="editServer(row)">
          编辑详情
        </el-button>
      </template>
    </el-table-column>
  </el-table>
  
  <!-- 底部操作 -->
  <div class="footer-actions">
    <el-button @click="step = 1">上一步</el-button>
    <el-button 
      type="primary" 
      @click="importSelected"
      :disabled="selectedServers.length === 0"
    >
      导入 {{ selectedServers.length }} 台服务器
    </el-button>
  </div>
</div>
```

**步骤3 - 导入结果**:
```vue
<div class="step step-3">
  <el-result 
    :icon="result.successCount > 0 ? 'success' : 'error'"
    :title="`成功导入 ${result.successCount} 台服务器`"
  >
    <template #subTitle>
      <div v-if="result.failedCount > 0" class="warning">
        失败 {{ result.failedCount }} 台，请查看详情
      </div>
    </template>
    
    <template #extra>
      <el-button type="primary" @click="goToServerList">
        查看服务器列表
      </el-button>
      <el-button @click="close">关闭</el-button>
    </template>
  </el-result>
  
  <!-- 失败详情 -->
  <div v-if="result.failures.length > 0" class="failures">
    <h4>失败详情</h4>
    <el-table :data="result.failures" border>
      <el-table-column prop="serverName" label="服务器名称" />
      <el-table-column prop="reason" label="失败原因" />
    </el-table>
  </div>
  
  <!-- 成功详情 -->
  <div v-if="result.successServers.length > 0" class="successes">
    <h4>导入成功的服务器</h4>
    <el-table :data="result.successServers" border>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="hostname" label="主机名" />
      <el-table-column label="连接状态">
        <template #default="{ row }">
          <server-status-tag :status="row.connectionStatus" />
        </template>
      </el-table-column>
    </el-table>
  </div>
</div>
```

---

## 映射规则

### SSH配置 → Server模型映射表

| SSH配置项 | Server字段 | 映射规则 | 默认值 |
|-----------|-----------|----------|--------|
| `Host` | `name` | 直接映射 | - |
| `HostName` | `hostname` | 直接映射 | - |
| `Port` | `sshPort` | 直接映射 | 22 |
| `User` | `sshUsername` | 直接映射 | - |
| `IdentityFile` | `sshKeyPath` | 展开`~`为用户目录 | null |
| - | `sshPassword` | **需用户手动输入** | null |
| - | `port` | 应用端口 | 8080 |
| - | `baseWorkDirectory` | 根据`User`生成 | `/home/{user}` |
| - | `description` | 自动生成 | "从SSH配置导入: {Host}" |
| - | `serverType` | 默认开发服务器 | `DEVELOPMENT` |
| - | `active` | 默认激活 | `true` |
| - | `autoMonitorEnabled` | 默认启用监控 | `true` |
| - | `connectionStatus` | 初始状态 | `UNKNOWN` |

### 特殊处理规则

1. **通配符Host处理**:
   ```ssh-config
   Host prod-*
       HostName 10.0.%h.10
   ```
   - 跳过通配符Host（无法确定具体主机）
   - 在警告中提示用户手动添加

2. **ProxyJump处理**:
   ```ssh-config
   Host app-server
       HostName 10.0.1.100
       ProxyJump bastion.example.com
   ```
   - 暂不支持跳板机配置
   - 在描述中记录ProxyJump信息
   - 警告用户需要手动配置跳板机

3. **IdentityFile路径展开**:
   ```java
   private String expandPath(String path) {
       if (path.startsWith("~/")) {
           return System.getProperty("user.home") + path.substring(1);
       }
       if (path.startsWith("$HOME/")) {
           return System.getProperty("user.home") + path.substring(5);
       }
       return path;
   }
   ```

4. **重复检查**:
   - 检查依据: `hostname` + `sshPort`
   - 如果已存在，标记为重复，默认不勾选
   - 允许用户强制导入（会创建新记录）

---

## 安全考虑

### 1. SSH私钥安全

**策略**: **不导入私钥内容，仅记录路径**

**原因**:
- SSH私钥是敏感凭证，不应在数据库中存储
- 私钥文件可能很大，存储成本高
- 私钥应由用户自行管理，保持在安全位置

**实现**:
```java
// ❌ 不这样做
String keyContent = Files.readString(Paths.get(identityFile));
server.setSshKeyContent(keyContent);

// ✅ 应该这样做
server.setSshKeyPath(identityFile);
```

### 2. 密码安全

**策略**: **强制使用加密存储**

**实现**:
```java
// 使用现有的PasswordEncryptionService
server.setPasswordEncryptionService(passwordEncryptionService);
server.setSshPassword(plainPassword); // 自动加密

// 数据库中存储加密后的密码
String encrypted = server.getSshPasswordEncrypted();
```

**加密格式**: `salt:encryptedPassword`

### 3. 文件上传安全

**限制**:
- 文件大小限制: 1MB
- 文件类型限制: 仅允许文本文件
- 内容验证: 检查是否为合法SSH配置格式

**实现**:
```java
@PostMapping("/upload")
public ResponseEntity<?> uploadConfig(@RequestParam("file") MultipartFile file) {
    // 1. 大小限制
    if (file.getSize() > 1024 * 1024) {
        return ResponseEntity.badRequest().body("文件大小超过1MB");
    }
    
    // 2. 类型检查
    String contentType = file.getContentType();
    if (!contentType.equals("text/plain")) {
        return ResponseEntity.badRequest().body("仅支持文本文件");
    }
    
    // 3. 内容验证
    String content = new String(file.getBytes(), StandardCharsets.UTF_8);
    if (!isValidSSHConfig(content)) {
        return ResponseEntity.badRequest().body("不是有效的SSH配置文件");
    }
    
    // 4. 解析处理...
}
```

### 4. 权限控制

**访问限制**: 仅管理员可使用导入功能

```java
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class SSHConfigImportController {
    // ...
}
```

### 5. 审计日志

**记录导入操作**:
```java
@Service
public class SSHConfigImportService {
    
    @Autowired
    private AuditLogService auditLogService;
    
    public ServerImportResult batchImport(List<ServerImportDto> servers, User user) {
        // 记录导入操作
        auditLogService.log(
            AuditAction.SSH_CONFIG_IMPORT,
            user.getUsername(),
            String.format("导入%d台服务器", servers.size())
        );
        
        // 执行导入...
    }
}
```

---

## 实施计划

### Phase 1: 后端核心功能 (3天)

**Day 1 - 配置解析器**
- [ ] 创建`SSHConfigParser`类
- [ ] 实现`parseConfig()`方法
- [ ] 支持Host、HostName、Port、User、IdentityFile解析
- [ ] 单元测试（覆盖率80%+）

**Day 2 - 数据映射与验证**
- [ ] 创建`SSHConfigMapper`类
- [ ] 实现DTO定义（SSHHostConfig、ServerImportDto）
- [ ] 实现`mapToServer()`方法
- [ ] 实现去重检查逻辑
- [ ] 单元测试

**Day 3 - 批量导入Service**
- [ ] 创建`SSHConfigImportService`
- [ ] 实现`batchImport()`方法
- [ ] 集成`ServerService.createServerWithPassword()`
- [ ] 异步连接测试
- [ ] 集成测试

### Phase 2: REST API开发 (2天)

**Day 4 - Controller实现**
- [ ] 创建`SSHConfigImportController`
- [ ] 实现`uploadConfig()`接口
- [ ] 实现`parseLocalConfig()`接口
- [ ] 实现`batchImport()`接口
- [ ] API文档（Swagger注解）

**Day 5 - 文件上传与安全**
- [ ] 配置MultipartFile上传
- [ ] 文件大小和类型限制
- [ ] 安全验证
- [ ] 错误处理
- [ ] API测试（Postman）

### Phase 3: 前端UI开发 (4天)

**Day 6 - 导入向导组件**
- [ ] 创建`SSHConfigImportWizard.ts`
- [ ] 实现三步向导逻辑
- [ ] API调用封装

**Day 7 - 步骤1界面**
- [ ] 文件上传组件
- [ ] 本地扫描按钮
- [ ] 路径输入框
- [ ] 样式优化

**Day 8 - 步骤2界面**
- [ ] 预览表格
- [ ] 行内编辑
- [ ] 勾选逻辑
- [ ] 批量操作按钮

**Day 9 - 步骤3界面**
- [ ] 结果展示组件
- [ ] 成功/失败列表
- [ ] 跳转逻辑

### Phase 4: 测试与优化 (2天)

**Day 10 - 集成测试**
- [ ] 端到端测试（上传→解析→导入→验证）
- [ ] 边界条件测试（空文件、格式错误、重复导入）
- [ ] 性能测试（大文件、大批量）

**Day 11 - 文档与发布**
- [ ] 用户手册
- [ ] API文档
- [ ] 代码注释
- [ ] 提交代码审查
- [ ] 发布到测试环境

---

## 附录

### A. SSH配置文件示例

```ssh-config
# 开发服务器
Host dev-server-01
    HostName 192.168.1.100
    Port 22
    User devuser
    IdentityFile ~/.ssh/id_rsa_dev

Host dev-server-02
    HostName 192.168.1.101
    Port 2222
    User root
    IdentityFile ~/.ssh/id_rsa_dev

# 生产服务器（通过跳板机）
Host prod-server-*
    HostName 10.0.%h.10
    Port 22
    User produser
    IdentityFile ~/.ssh/id_rsa_prod
    ProxyJump bastion.example.com

# 测试服务器
Host test-ubuntu
    HostName test.example.com
    User testuser
    IdentityFile ~/.ssh/id_rsa_test
    
# WSL本地
Host wsl
    HostName localhost
    Port 22
    User wsluser
```

### B. 解析结果示例

```json
{
  "totalHosts": 4,
  "servers": [
    {
      "name": "dev-server-01",
      "hostname": "192.168.1.100",
      "sshPort": 22,
      "sshUsername": "devuser",
      "sshKeyPath": "/home/user/.ssh/id_rsa_dev",
      "port": 8080,
      "baseWorkDirectory": "/home/devuser",
      "serverType": "DEVELOPMENT",
      "valid": true,
      "missingFields": [],
      "duplicate": false
    },
    {
      "name": "dev-server-02",
      "hostname": "192.168.1.101",
      "sshPort": 2222,
      "sshUsername": "root",
      "sshKeyPath": "/home/user/.ssh/id_rsa_dev",
      "port": 8080,
      "baseWorkDirectory": "/root",
      "serverType": "DEVELOPMENT",
      "valid": true,
      "missingFields": [],
      "duplicate": false
    }
  ],
  "warnings": [
    "跳过通配符Host: prod-server-* (需要手动添加)",
    "Host 'prod-server-*' 包含ProxyJump配置，暂不支持"
  ],
  "errors": []
}
```

### C. 相关文件清单

**后端**:
```
src/main/java/com/cmict/internalpaas/
├── controller/
│   └── SSHConfigImportController.java
├── service/
│   ├── SSHConfigImportService.java
│   ├── SSHConfigParser.java
│   └── SSHConfigMapper.java
├── dto/
│   ├── SSHHostConfig.java
│   ├── ServerImportDto.java
│   ├── SSHConfigParseResult.java
│   └── ServerImportResult.java
└── repository/
    └── ServerRepository.java (扩展方法)
```

**前端**:
```
src/main/frontend/
├── modules/
│   └── SSHConfigImportWizard.ts
└── components/
    ├── SSHConfigUpload.vue
    ├── ServerImportPreview.vue
    └── ImportResult.vue
```

**测试**:
```
src/test/java/com/cmict/internalpaas/
├── service/
│   ├── SSHConfigParserTest.java
│   └── SSHConfigImportServiceTest.java
└── controller/
    └── SSHConfigImportControllerTest.java
```

---

**文档维护者**: GitHub Copilot  
**审核状态**: 待审核  
**下次更新**: 功能实现完成后更新实施进度
