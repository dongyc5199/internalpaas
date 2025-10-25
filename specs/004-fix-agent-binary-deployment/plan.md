# Implementation Plan: Agent Binary Deployment Enhancement

**Branch**: `004-fix-agent-binary-deployment` | **Date**: 2025-10-25 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-fix-agent-binary-deployment/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

修复Agent二进制文件缺失导致部署失败的问题，实现多层次的二进制文件获取策略：
1. 优先使用应用资源中打包的二进制文件
2. 从配置的HTTP/HTTPS URL下载作为备选方案
3. 失败时提供清晰、可操作的错误诊断

技术方案：增强现有的 `AgentDeployService.java`，添加HTTP客户端下载能力，实现二进制文件验证机制，优化部署日志的可读性和诊断价值。

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.2.0, Java HTTP Client (java.net.http), JSch (SSH file transfer)
**Storage**: 文件系统（临时缓存下载的二进制文件）
**Testing**: JUnit 5, Mockito, Spring Boot Test
**Target Platform**: Linux servers (部署目标) + 应用服务器（运行平台）
**Project Type**: Web application (Spring Boot单体应用)
**Performance Goals**: 40MB文件在10分钟内完成下载和部署；支持10个并发部署
**Constraints**: HTTP下载5分钟超时；二进制文件大小<100MB；需要网络连接到下载URL
**Scale/Scope**: 单个部署操作；影响现有AgentDeployService（约700行代码）

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**注意**: 项目宪法文件（`.specify/memory/constitution.md`）为模板格式，尚未定制化。因此本次检查使用Spring Boot最佳实践和项目现有架构原则进行评估。

### 评估标准（基于现有代码库架构）

1. ✅ **分层架构原则**: 保持Service层职责单一，新功能集成到现有AgentDeployService
2. ✅ **依赖注入**: 使用Spring的@Value注解进行配置管理，HttpClient通过final字段初始化
3. ✅ **错误处理**: 统一异常处理，详细日志记录，用户友好的错误消息
4. ✅ **测试覆盖**: 单元测试覆盖新增的二进制获取逻辑，集成测试验证端到端流程
5. ✅ **向后兼容**: 保持现有API接口不变，仅增强内部实现

### 潜在违规点（无）

无违规。本功能为现有服务的增强，不引入新的架构层次或外部依赖库。

## Project Structure

### Documentation (this feature)

```text
specs/004-fix-agent-binary-deployment/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/main/java/com/cmict/internalpaas/
├── service/
│   ├── AgentDeployService.java           # 核心修改：增强二进制获取逻辑
│   └── SshFileTransferService.java       # 现有：已支持字节数组上传
├── dto/agent/
│   ├── DeployResult.java                 # 现有：部署结果DTO
│   └── PreCheckResult.java               # 现有：预检查结果DTO
└── config/
    └── (application.properties)          # 配置：agent.binary.download-url

src/main/resources/
└── agent/
    ├── otelcol-linux-amd64.tar.gz       # 可选：打包的二进制文件（当前缺失）
    ├── otelcol.yaml.tmpl                # 现有：配置模板
    └── bootstrap.sh                      # 现有：安装脚本

src/test/java/com/cmict/internalpaas/service/
├── AgentDeployServiceTest.java          # 增强：新增下载逻辑测试
└── AgentDeployServiceIntegrationTest.java # 新增：端到端测试
```

**Structure Decision**:
项目采用标准的Spring Boot Web应用结构。本功能主要修改 `AgentDeployService.java`，不引入新的服务层或控制器。测试文件遵循Maven标准布局（src/test/java）。配置文件使用Spring Boot的application.properties进行管理。

**关键文件说明**:
- **AgentDeployService.java**: 已实现 `resolveAgentBinary()` 基础框架，需增强HTTP下载和缓存能力
- **SshFileTransferService.java**: 已支持 `uploadFileBytes()`，无需修改
- **application.properties**: 新增 `agent.binary.download-url` 配置项

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

无违规项需要记录。

## 研究需求识别

基于技术上下文，以下领域需要在Phase 0进行研究：

1. **HTTP下载最佳实践**
   - Java HTTP Client的超时配置和重试机制
   - 支持HTTP重定向的实现方式
   - 大文件下载的内存优化策略

2. **文件验证策略**
   - tar.gz文件格式的签名识别
   - 文件损坏检测方法（魔数校验、最小文件大小）
   - 错误文件（如HTML错误页）的识别

3. **并发部署场景**
   - 多服务器同时部署时的下载去重机制
   - 本地缓存策略（内存 vs 磁盘）
   - 缓存失效和清理策略

4. **错误处理模式**
   - HTTP状态码到用户友好消息的映射
   - 网络超时、连接失败、DNS解析失败的区分
   - 部署日志的结构化格式

5. **配置管理**
   - 生产环境动态更新配置的方案
   - 敏感URL（包含认证信息）的安全存储
   - 配置验证（URL格式、协议限制）

## 下一步行动

Phase 0 研究任务将在 `research.md` 中详细展开，解决上述所有 "NEEDS CLARIFICATION" 项。
