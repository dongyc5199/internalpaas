# Quick Start: Agent Binary Deployment Enhancement

**Feature**: 004-fix-agent-binary-deployment
**Audience**: 开发人员、运维人员
**Estimated Reading Time**: 10分钟

---

## 📋 功能概述

本功能解决了Agent部署失败的问题（由于二进制文件 `agent/otelcol-linux-amd64.tar.gz` 缺失），并引入了灵活的多层次获取策略：

1. ✅ **内置包优先**: 使用应用资源中打包的二进制文件
2. ✅ **HTTP下载备选**: 从配置的URL下载二进制文件
3. ✅ **智能缓存**: 自动缓存下载的文件，避免重复下载
4. ✅ **详细日志**: 提供清晰的部署进度和错误诊断信息

---

## 🚀 快速开始（5分钟）

### 方案A：使用下载URL（推荐）

**适用场景**: 不想在应用中打包大文件，或需要灵活切换Agent版本

**步骤**:

1. **配置下载地址** (application.properties):
   ```properties
   # OpenTelemetry Collector官方发布
   agent.binary.download-url=https://github.com/open-telemetry/opentelemetry-collector-releases/releases/download/v0.91.0/otelcol-contrib_0.91.0_linux_amd64.tar.gz
   ```

2. **启动应用**:
   ```bash
   ./mvnw.cmd spring-boot:run
   ```

3. **添加服务器测试**:
   - 访问管理员界面，添加一台测试服务器
   - 观察部署日志，应看到：
     ```
     [上传] 通过下载地址获取Agent二进制: https://github.com/...
     [上传] ✅ 下载Agent二进制成功 (42.35 MB)
     [上传] ✅ 文件验证通过
     [上传] ✅ Agent二进制文件上传成功
     ```

**优势**:
- ✅ 无需重新打包应用
- ✅ 可随时更换Agent版本
- ✅ jar包体积小

---

### 方案B：打包二进制文件

**适用场景**: 离线环境或希望完全自包含部署

**步骤**:

1. **下载二进制文件**:
   ```bash
   cd src/main/resources/agent
   wget https://github.com/open-telemetry/opentelemetry-collector-releases/releases/download/v0.91.0/otelcol-contrib_0.91.0_linux_amd64.tar.gz
   ```

2. **验证文件**:
   ```bash
   ls -lh otelcol-contrib_0.91.0_linux_amd64.tar.gz
   # 应显示约40-50MB的文件
   ```

3. **重命名文件** (如需要):
   ```bash
   mv otelcol-contrib_0.91.0_linux_amd64.tar.gz otelcol-linux-amd64.tar.gz
   ```
   或者修改配置：
   ```properties
   agent.binary.path=agent/otelcol-contrib_0.91.0_linux_amd64.tar.gz
   ```

4. **重新打包应用**:
   ```bash
   ./mvnw.cmd clean package -DskipTests
   ```

5. **部署测试**: 添加服务器，日志应显示：
   ```
   [上传] 使用内置Agent包 (agent/otelcol-linux-amd64.tar.gz, 42.35 MB)
   [上传] ✅ Agent二进制文件上传成功
   ```

**优势**:
- ✅ 离线环境可用
- ✅ 无需外部网络依赖

---

## 🔧 配置选项详解

### 完整配置示例 (application.properties)

```properties
# ============================================================================
# Agent二进制文件配置
# ============================================================================

# 方案1：使用下载URL（优先级低于打包文件）
agent.binary.download-url=https://github.com/open-telemetry/opentelemetry-collector-releases/releases/download/v0.91.0/otelcol-contrib_0.91.0_linux_amd64.tar.gz

# 方案2：打包文件路径（相对于src/main/resources）
agent.binary.path=agent/otelcol-linux-amd64.tar.gz

# 下载超时时间（分钟）
agent.binary.download-timeout-minutes=5

# 启用下载缓存（减少重复下载）
agent.binary.cache-enabled=true
```

### 配置优先级

```
资源文件 (agent.binary.path) > HTTP下载 (agent.binary.download-url)
```

**示例场景**:
- **同时配置**: 优先使用资源文件，仅在资源不存在时下载
- **仅配置URL**: 每次启动后第一次部署下载，后续使用缓存
- **都不配置**: 部署失败，提示"未能获取Agent二进制文件"

---

## 📊 使用场景示例

### 场景1：开发环境 - 使用本地缓存的下载文件

**配置**:
```properties
agent.binary.download-url=https://github.com/.../otelcol.tar.gz
agent.binary.cache-enabled=true
```

**行为**:
1. 第一次部署：下载文件（约30秒，取决于网络）
2. 后续部署：使用缓存（瞬间完成）
3. 重启应用：缓存清空，重新下载

**日志输出**:
```
# 第一次部署
[上传] 通过下载地址获取Agent二进制: https://github.com/...
[上传] ✅ 下载Agent二进制成功 (42.35 MB)

# 第二次部署（同一次运行）
[上传] 使用已缓存的Agent二进制
```

---

### 场景2：生产环境 - 使用内网镜像地址

**配置** (使用环境变量):
```bash
export AGENT_BINARY_URL=https://internal-repo.company.com/binaries/otelcol-v0.91.0.tar.gz
```

**application.properties**:
```properties
agent.binary.download-url=${AGENT_BINARY_URL}
```

**优势**:
- 敏感URL不硬编码
- 不同环境使用不同配置
- 内网下载速度快

---

### 场景3：离线部署 - 完全自包含

**准备步骤**:
```bash
# 1. 下载二进制到资源目录
wget -P src/main/resources/agent https://github.com/.../otelcol.tar.gz

# 2. 打包应用（包含二进制）
./mvnw.cmd clean package

# 3. 部署jar包到离线服务器
scp target/internalpaas-*.jar user@server:/opt/app/
```

**配置**:
```properties
# 无需配置download-url，仅依赖打包文件
agent.binary.path=agent/otelcol.tar.gz
```

---

## 🐛 故障排查

### 问题1：下载超时

**错误日志**:
```
[上传] ❌ 下载Agent二进制失败: 下载超时（5分钟限制），文件可能过大或网络过慢
```

**解决方案**:
1. **增加超时时间**:
   ```properties
   agent.binary.download-timeout-minutes=10
   ```

2. **检查网络连接**:
   ```bash
   curl -I https://github.com/.../otelcol.tar.gz
   # 应返回 HTTP/2 200
   ```

3. **使用更快的镜像**:
   ```properties
   agent.binary.download-url=https://cdn.example.com/otelcol.tar.gz
   ```

---

### 问题2：文件验证失败

**错误日志**:
```
[上传] ❌ 文件验证失败，大小：10240 bytes
```

**原因分析**:
- 下载的是HTML错误页（如404）
- 文件损坏或不完整
- URL指向了错误的文件类型

**解决方案**:
1. **手动验证URL**:
   ```bash
   wget https://your-url/file.tar.gz -O test.tar.gz
   file test.tar.gz
   # 应输出: test.tar.gz: gzip compressed data
   ```

2. **检查文件大小**:
   ```bash
   ls -lh test.tar.gz
   # 应该是40-50MB，不是几KB
   ```

3. **修正URL**:
   - 确认URL指向`.tar.gz`文件
   - 检查是否需要认证（使用 `https://user:pass@...`）

---

### 问题3：HTTP 404错误

**错误日志**:
```
[上传] ❌ 下载Agent二进制失败: 下载失败（文件不存在或无权限），HTTP状态码：404
```

**解决方案**:
1. **检查URL拼写**（常见错误）:
   ```
   ❌ ...releases/download/0.91.0/...  (缺少 'v' 前缀)
   ✅ ...releases/download/v0.91.0/... (正确)
   ```

2. **验证文件名**:
   ```bash
   # 访问GitHub Release页面确认文件名
   # https://github.com/open-telemetry/opentelemetry-collector-releases/releases
   ```

3. **检查版本号**: 确认版本存在且已发布

---

### 问题4：内存不足

**错误症状**: 应用崩溃，JVM OOM

**原因**: 多次并发下载导致内存占用过高

**解决方案**:
1. **禁用缓存**（临时方案）:
   ```properties
   agent.binary.cache-enabled=false
   ```

2. **增加JVM堆内存**:
   ```bash
   java -Xmx2g -jar internalpaas.jar
   ```

3. **使用打包文件方案** (推荐):
   - 避免运行时下载
   - 内存占用更可控

---

### 问题5：URL包含认证信息无法使用

**场景**: 需要从受保护的内部仓库下载

**错误示例**:
```properties
# ❌ 密码包含特殊字符导致URL解析失败
agent.binary.download-url=https://user:p@ssw0rd@repo.internal.com/file.tar.gz
```

**解决方案**:
1. **URL编码密码**:
   ```properties
   # 将 @ 替换为 %40, : 替换为 %3A 等
   agent.binary.download-url=https://user:p%40ssw0rd@repo.internal.com/file.tar.gz
   ```

2. **使用环境变量**:
   ```bash
   export AGENT_BINARY_URL="https://user:password@repo.internal.com/file.tar.gz"
   ```

3. **使用令牌认证** (如GitHub Personal Access Token):
   ```properties
   agent.binary.download-url=https://ghp_xxxxxxxxxxxx@github.com/private-repo/file.tar.gz
   ```

---

## 🧪 测试验证

### 单元测试运行

```bash
# 运行Agent部署相关测试
./mvnw.cmd test -Dtest=AgentDeployServiceTest

# 验证测试覆盖率
./mvnw.cmd test jacoco:report
# 查看 target/site/jacoco/index.html
```

**预期结果**:
- ✅ 所有测试通过
- ✅ 代码覆盖率 > 80%

---

### 集成测试

**手动测试步骤**:

1. **准备测试服务器** (需要SSH访问权限):
   ```bash
   # 确保服务器满足要求
   - SSH可访问
   - /opt目录至少100MB可用空间
   - 端口4317和4318未被占用
   ```

2. **配置测试URL**:
   ```properties
   agent.binary.download-url=https://github.com/.../otelcol-contrib_0.91.0_linux_amd64.tar.gz
   ```

3. **启动应用并添加服务器**:
   - 访问 `http://localhost:8080`
   - 进入管理员界面 → 服务器管理
   - 添加测试服务器

4. **观察部署日志**:
   - 应看到完整的下载、验证、上传流程
   - 最终状态为"部署成功"

5. **验证Agent运行**:
   ```bash
   # 登录测试服务器
   ssh user@test-server

   # 检查服务状态
   systemctl status otelcol

   # 检查端口监听
   ss -ltn | grep 4317
   ```

---

## 📈 性能基准

### 典型部署时间

| 场景 | 首次部署 | 后续部署（缓存命中） |
|-----|---------|-------------------|
| 使用打包文件 | 2-3分钟 | 2-3分钟 |
| HTTP下载（快速网络） | 3-4分钟 | 2-3分钟 |
| HTTP下载（慢速网络） | 5-10分钟 | 2-3分钟 |

**时间分解**:
- 预检查: 10-30秒
- 二进制获取:
  - 打包文件: < 1秒
  - HTTP下载: 30秒 - 5分钟（取决于网络）
  - 缓存命中: < 0.1秒
- 文件上传: 30-60秒
- 安装执行: 1-2分钟
- 健康检查: 30秒

---

## 🔒 安全建议

### 生产环境最佳实践

1. **使用HTTPS**: 确保下载URL使用HTTPS协议
   ```properties
   # ✅ 推荐
   agent.binary.download-url=https://...

   # ❌ 不推荐（HTTP明文传输）
   agent.binary.download-url=http://...
   ```

2. **敏感信息管理**:
   ```bash
   # 使用环境变量而非硬编码
   export AGENT_BINARY_URL="https://token@private-repo.com/file.tar.gz"
   ```

3. **日志脱敏**: 系统自动脱敏URL中的认证信息
   ```
   # 实际URL: https://user:password@example.com/file.tar.gz
   # 日志显示: https://***:***@example.com/file.tar.gz
   ```

4. **内网部署**: 优先使用内网镜像地址
   ```properties
   agent.binary.download-url=https://internal-artifactory.company.com/otelcol.tar.gz
   ```

---

## 📚 进一步阅读

- **详细设计文档**: [plan.md](./plan.md)
- **技术研究**: [research.md](./research.md)
- **数据模型**: [data-model.md](./data-model.md)
- **API合约**: [contracts/](./contracts/)

---

## 🆘 获取帮助

**遇到问题？**
1. 查看部署日志（AgentDeployment记录）
2. 检查应用日志（logs/application.log）
3. 参考故障排查章节
4. 提交Issue并附上完整日志

**联系方式**: 查看项目README
