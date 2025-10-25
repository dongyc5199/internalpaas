---
description: "Task list for Agent Binary Deployment Enhancement implementation"
---

# Tasks: Agent Binary Deployment Enhancement

**Input**: Design documents from `/specs/004-fix-agent-binary-deployment/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: 本功能规格中未明确要求TDD，但单元测试被列为验收标准。测试任务将包含在实现任务之后。

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

项目使用标准Spring Boot结构：
- **Source**: `src/main/java/com/cmict/internalpaas/`
- **Resources**: `src/main/resources/`
- **Tests**: `src/test/java/com/cmict/internalpaas/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 配置管理和开发环境准备

- [ ] T001 添加配置属性 agent.binary.download-url 到 src/main/resources/application.properties
- [ ] T002 添加配置属性 agent.binary.download-timeout-minutes 到 src/main/resources/application.properties（默认值5）
- [ ] T003 添加配置属性 agent.binary.cache-enabled 到 src/main/resources/application.properties（默认值true）

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 核心基础设施，所有用户故事依赖的共享组件

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 在 AgentDeployService.java 中添加 ConcurrentHashMap<String, byte[]> binaryCache 字段用于缓存下载的二进制文件
- [ ] T005 在 AgentDeployService.java 中添加 Object downloadLock 字段用于同步下载控制
- [ ] T006 [P] 在 AgentDeployService.java 中实现 validateBinary(byte[] data) 方法（检查文件大小、Gzip魔数、上限）
- [ ] T007 [P] 在 AgentDeployService.java 中实现 sanitizeUrl(String url) 方法（URL脱敏，移除认证信息）
- [ ] T008 在 AgentDeployService.java 中实现 downloadAgentBinary() 方法（HTTP下载逻辑，包含超时、重定向、错误处理）

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Automated Agent Deployment with Download Fallback (Priority: P1) 🎯 MVP

**Goal**: 实现三层二进制获取策略（缓存 → 资源 → HTTP下载），确保Agent部署成功

**Independent Test**: 添加一个新服务器，验证Agent成功部署（无论二进制来源是打包文件还是HTTP下载），服务器监控指标在仪表板可见

### Implementation for User Story 1

- [ ] T009 [US1] 增强 loadBinaryFromResource(AgentDeployment deployment) 方法，添加详细日志（使用内置Agent包，显示路径和文件大小）在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T010 [US1] 实现 resolveAgentBinaryWithCache(AgentDeployment deployment) 方法（优先检查缓存，缓存未命中时调用loadBinaryFromResource或downloadAgentBinary）在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T011 [US1] 在 resolveAgentBinaryWithCache 方法中实现并发下载控制（synchronized块 + 双重检查模式）在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T012 [US1] 在 resolveAgentBinaryWithCache 方法中添加缓存写入逻辑（下载成功且验证通过后）在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T013 [US1] 修改 uploadFiles 方法，调用 resolveAgentBinaryWithCache 替代原有的 resolveAgentBinary，处理null返回值在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T014 [US1] 添加缓存命中日志 "[上传] 使用已缓存的Agent二进制" 在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T015 [US1] 添加资源加载日志 "[上传] 使用内置Agent包 ({path}, {size} MB)" 在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T016 [US1] 添加下载开始日志 "[上传] 通过下载地址获取Agent二进制: {url}"（使用sanitizeUrl脱敏）在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T017 [US1] 添加下载成功日志 "[上传] ✅ 下载Agent二进制成功 ({size} MB)"（两位小数）在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T018 [US1] 添加文件验证通过日志 "[上传] ✅ 文件验证通过" 在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java

### Testing for User Story 1

- [ ] T019 [P] [US1] 编写单元测试 test_resolveFromCache_whenCacheHit_returnsData 在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T020 [P] [US1] 编写单元测试 test_resolveFromResource_whenFileExists_returnsData 在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T021 [P] [US1] 编写单元测试 test_downloadFromUrl_whenHttp200_returnsData（Mock HttpClient）在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T022 [P] [US1] 编写单元测试 test_concurrentDownload_whenMultipleThreads_downloadOnce 验证仅执行一次下载在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T023 [P] [US1] 编写单元测试 test_validate_whenValidGzipFile_returnsTrue 在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T024 [P] [US1] 编写单元测试 test_validate_whenFileTooSmall_returnsFalse 在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T025 [P] [US1] 编写单元测试 test_validate_whenWrongMagicNumber_returnsFalse 在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently. Run `./mvnw.cmd test -Dtest=AgentDeployServiceTest` to verify.

---

## Phase 4: User Story 2 - Clear Deployment Failure Diagnostics (Priority: P2)

**Goal**: 提供清晰、可操作的错误消息，帮助管理员快速定位问题

**Independent Test**: 故意配置错误（无效URL、缺失文件），验证错误消息准确识别根本原因并提供修复建议

### Implementation for User Story 2

- [ ] T026 [US2] 在 downloadAgentBinary 方法中添加 HttpTimeoutException 捕获，抛出 IOException 并附带用户友好消息 "下载超时（5分钟限制），文件可能过大或网络过慢" 在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T027 [US2] 在 downloadAgentBinary 方法中添加 ConnectException 捕获，抛出 IOException 并附带消息 "无法连接到下载服务器，请检查网络或URL配置" 在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T028 [US2] 在 downloadAgentBinary 方法中添加 HTTP 4xx 状态码处理，抛出 IOException 并附带消息 "下载失败（文件不存在或无权限），HTTP状态码：{code}" 在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T029 [US2] 在 downloadAgentBinary 方法中添加 HTTP 5xx 状态码处理，抛出 IOException 并附带消息 "下载服务器错误，HTTP状态码：{code}，请稍后重试" 在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T030 [US2] 在 resolveAgentBinaryWithCache 方法中添加异常捕获，记录错误日志 "[上传] ❌ 下载Agent二进制失败: {errorMessage}" 在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T031 [US2] 在 uploadFiles 方法中添加二进制获取失败处理，记录日志 "[上传] ❌ 未能获取Agent二进制文件，请检查配置" 在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T032 [US2] 在 validateBinary 方法失败时，添加详细错误日志 "[上传] ❌ 文件验证失败，大小：{size} bytes" 在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T033 [US2] 在 loadBinaryFromResource 方法失败时，添加错误日志（包含路径和异常信息）在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java

### Testing for User Story 2

- [ ] T034 [P] [US2] 编写单元测试 test_downloadTimeout_whenExceed5Minutes_returnsNull（Mock HttpClient抛出HttpTimeoutException）在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T035 [P] [US2] 编写单元测试 test_downloadHttp404_whenNotFound_returnsNull 验证错误消息包含 "文件不存在或无权限" 在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T036 [P] [US2] 编写单元测试 test_downloadHttp500_whenServerError_returnsNull 验证错误消息包含 "请稍后重试" 在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T037 [P] [US2] 编写单元测试 test_downloadConnectionFailed_returnsNull 验证错误消息包含 "无法连接到下载服务器" 在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T038 [P] [US2] 编写单元测试 test_resolveAgentBinary_whenAllMethodsFail_returnsNullWithLog 验证完整错误日志在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently. Test error scenarios with invalid configurations.

---

## Phase 5: User Story 3 - Deployment Progress Visibility (Priority: P3)

**Goal**: 提供详细的实时部署进度，包括下载状态、文件大小、上传进度

**Independent Test**: 发起Agent部署，观察部署日志显示每个步骤（下载开始、文件大小、上传进度、成功指示器）

### Implementation for User Story 3

- [ ] T039 [US3] 在 uploadFiles 方法中添加上传开始日志 "[上传] 上传Agent二进制文件 ({size} MB)..."（文件大小两位小数）在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T040 [US3] 在 uploadFiles 方法中添加上传成功日志 "[上传] ✅ Agent二进制文件上传成功" 在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T041 [US3] 在 uploadFiles 方法中添加上传失败日志 "[上传] ⚠️ Agent二进制文件上传失败，请检查网络或磁盘空间" 在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T042 [US3] 确保所有日志使用 Locale.ROOT 格式化文件大小（避免国际化问题，小数点为 . 而非 ,）在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T043 [US3] 确保所有日志使用 String.format("%.2f", sizeMB) 格式化文件大小为两位小数在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java
- [ ] T044 [US3] 验证所有 Unicode 状态标记（✅ ❌ ⚠️）在日志中正确显示在 src/main/java/com/cmict/internalpaas/service/AgentDeployService.java

### Testing for User Story 3

- [ ] T045 [P] [US3] 编写单元测试 test_log_whenDownloadSuccess_formatCorrect 验证日志包含 "✅ 下载Agent二进制成功" 和两位小数文件大小在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T046 [P] [US3] 编写单元测试 test_log_whenUrlWithAuth_sanitized 验证日志显示 https://***:***@example.com 在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T047 [P] [US3] 编写单元测试 test_log_fileSizeFormat_usesDot 验证日志包含 "42.35" 而非 "42,35"（Locale.ROOT检查）在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T048 [P] [US3] 编写单元测试 test_log_whenCacheHit_formatCorrect 验证日志包含 "使用已缓存的Agent二进制" 在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java

**Checkpoint**: All user stories should now be independently functional. Verify complete deployment logs show all progress steps.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 改进和测试覆盖

- [ ] T049 [P] 编写单元测试 test_validate_whenFileTooLarge_returnsFalse（150MB文件）在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T050 [P] 编写单元测试 test_validate_whenHtmlErrorPage_returnsFalse（10KB HTML内容）在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T051 [P] 编写单元测试 test_validate_whenExactly1MB_returnsTrue（边界测试）在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T052 [P] 编写单元测试 test_validate_whenExactly100MB_returnsTrue（边界测试）在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceTest.java
- [ ] T053 [P] 编写集成测试 integration_downloadRealUrl_whenGitHubRelease_success 使用真实GitHub Release URL在 src/test/java/com/cmict/internalpaas/service/AgentDeployServiceIntegrationTest.java
- [ ] T054 代码审查：检查所有异常处理、日志记录、边界条件
- [ ] T055 运行完整测试套件：./mvnw.cmd clean test 确保所有测试通过
- [ ] T056 验证测试覆盖率 > 80%（使用 JaCoCo 报告）
- [ ] T057 使用 quickstart.md 中的场景进行手动验证（方案A：HTTP下载，方案B：打包文件）
- [ ] T058 [P] 更新 CLAUDE.md 中的功能描述（如需要）
- [ ] T059 提交代码并创建合并请求到主分支

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 - 立即开始
- **Foundational (Phase 2)**: 依赖 Setup 完成 - 阻塞所有用户故事
- **User Stories (Phase 3-5)**: 全部依赖 Foundational phase 完成
  - 用户故事可以并行进行（如果有足够人力）
  - 或按优先级顺序（P1 → P2 → P3）
- **Polish (Phase 6)**: 依赖所有需要的用户故事完成

### User Story Dependencies

- **User Story 1 (P1)**: Foundational完成后可开始 - 无其他故事依赖
- **User Story 2 (P2)**: Foundational完成后可开始 - 独立于US1，但共享US1的基础设施
- **User Story 3 (P3)**: Foundational完成后可开始 - 独立于US1/US2，增强日志可见性

**关键洞察**: US1、US2、US3都修改同一个文件（AgentDeployService.java），因此实际上必须按顺序实现，无法完全并行。但每个故事聚焦不同方面：
- US1: 核心获取逻辑
- US2: 错误处理
- US3: 日志格式化

### Within Each User Story

- 实现任务优先
- 测试任务在实现完成后（验证实现正确性）
- 日志任务与实现交织（实现一个方法就添加对应日志）

### Parallel Opportunities

- Phase 1的所有任务可并行（独立配置项）
- Phase 2中：
  - T006（validateBinary）和 T007（sanitizeUrl）可并行（不同方法）
  - T004、T005 必须先于 T008（downloadAgentBinary依赖这些字段）
- 每个用户故事内的测试任务标记 [P] 可并行运行
- Phase 6的测试任务可并行

---

## Parallel Example: User Story 1

```bash
# 实现任务按顺序执行（修改同一文件）
Task T009: 增强 loadBinaryFromResource
Task T010: 实现 resolveAgentBinaryWithCache
Task T011: 并发控制
# ...

# 测试任务可并行执行（不同测试方法）
Task T019: test_resolveFromCache_whenCacheHit_returnsData
Task T020: test_resolveFromResource_whenFileExists_returnsData
Task T021: test_downloadFromUrl_whenHttp200_returnsData
Task T022: test_concurrentDownload_whenMultipleThreads_downloadOnce
Task T023: test_validate_whenValidGzipFile_returnsTrue
Task T024: test_validate_whenFileTooSmall_returnsFalse
Task T025: test_validate_whenWrongMagicNumber_returnsFalse
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup（3个任务，约10分钟）
2. Complete Phase 2: Foundational（5个任务，约2-3小时）
3. Complete Phase 3: User Story 1（10个实现任务 + 7个测试任务，约4-6小时）
4. **STOP and VALIDATE**: 运行测试，手动添加服务器验证部署成功
5. 如果通过验证，可以先部署MVP

**MVP交付物**:
- ✅ 支持HTTP下载Agent二进制
- ✅ 支持打包文件回退方案
- ✅ 基本的文件验证
- ✅ 缓存机制避免重复下载
- ✅ 基础日志记录

### Incremental Delivery

1. **MVP (US1)**: 核心功能 → 测试 → 部署
2. **US2增强**: 错误处理 → 测试 → 部署（错误诊断能力大幅提升）
3. **US3增强**: 日志优化 → 测试 → 部署（用户体验改善）
4. **Polish**: 完整测试覆盖 → 代码审查 → 最终发布

### Sequential Strategy (单人开发)

由于所有任务都修改同一个文件（AgentDeployService.java），建议按Phase顺序执行：

1. Phase 1（配置）
2. Phase 2（基础设施）
3. Phase 3（US1核心功能 + 测试）
4. Phase 4（US2错误处理 + 测试）
5. Phase 5（US3日志优化 + 测试）
6. Phase 6（Polish + 集成测试）

**预计总耗时**: 12-16小时（包含测试和验证）

---

## Notes

- [P] 标记的任务可并行（不同文件或不同测试方法）
- [Story] 标签映射任务到具体用户故事，便于追溯
- 每个用户故事应独立可完成和测试
- 在每个 Checkpoint 停下来验证故事独立性
- 提交频率：每完成一个故事阶段提交一次
- 避免：模糊任务、同一文件冲突、破坏故事独立性的跨故事依赖

---

## Task Count Summary

- **Phase 1 (Setup)**: 3 tasks
- **Phase 2 (Foundational)**: 5 tasks
- **Phase 3 (US1)**: 10 implementation + 7 testing = 17 tasks
- **Phase 4 (US2)**: 8 implementation + 5 testing = 13 tasks
- **Phase 5 (US3)**: 6 implementation + 4 testing = 10 tasks
- **Phase 6 (Polish)**: 11 tasks

**Total**: 59 tasks

**Parallelizable**: 35 tasks marked [P] (主要是测试任务)

**Test Coverage**: 27 test tasks (46% of total)

**Independent Tests**:
- US1: 添加服务器 → Agent部署成功 → 监控数据可见
- US2: 配置错误 → 清晰错误消息 → 问题可定位
- US3: 查看部署日志 → 详细进度信息 → 每个步骤可见

**Suggested MVP Scope**: Phase 1 + Phase 2 + Phase 3 (US1) = 25 tasks
