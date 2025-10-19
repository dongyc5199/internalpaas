# WaveTerm AI 能力集成方案

## 1. 背景与目标
- 充分利用 WaveTerm Demo Web Backend 中的 AI 聊天、命令补全能力，为现有内部平台的终端场景提供智能助手与自动化支持。
- 与现有终端会话、服务器管理模块（`/terminal`、`/terminal/api/**`、`/ws/ssh-terminal`）解耦集成，避免影响 SSH 会话与现有监控能力。
- 建立一套可扩展、可配置的 AI 服务接入框架，为后续引入更多模型与辅助功能（如工单总结、风险扫描）奠定基础。

## 2. 现状概览
### 2.1 当前平台
- 终端入口：`terminal/index.html`、`terminal/manager.html`，使用 Spring Security Session、CSRF 保护和自定义 WebSocket `/ws/ssh-terminal`。
- 会话管理：`SSHTerminalService` + `SSHSessionRepository` 双层维护；服务器信息由 `ServerService` 负责。
- 安全体系：以 Session/角色为主，WebSocket 握手通过 `SecurityWebSocketHandshakeInterceptor` 传递认证。
- 缺口：无 AI 聊天、命令补全或自动建议能力。

### 2.2 WaveTerm AI 模块
- 架构：`chat/ai` 客户端注册表 + `ChatService` + `ChatController`，支持多模型流式响应。
- 扩展：`CommandCompletionService`、`TerminalHistoryService` 等围绕终端的辅助功能。
- 安全：默认 JWT；配置项集中在 `application.yml` 下的 `ai.providers.*`、`chat.*` 等节点。

## 3. 集成范围
- **纳入范围**
  - 引入 AI 客户端接口、注册表、服务层和 REST/流式 API（聊天、命令补全、上下文管理）。
  - 将配置项映射到现有 `application-*.properties`/`yml`，改造为可按环境覆盖。
  - 调整安全配置以允许新 API 调用，复用现有 Session 体系或按需补充 JWT 适配器。
  - **集成原则**：坚持“主应用终端 WebSocket 实现 + WaveTerm AI 能力”的组合策略，复用 `/ws/ssh-terminal` 负责终端会话，AI 通过 REST/SSE 或消息桥接提供辅助结果，避免改动底层终端通信。
  - 在终端管理页添加 AI 面板入口及基础交互（初期可放置在独立页或 Drawer）。
- **非纳入范围**
  - 直接替换现有终端 WebSocket 实现、服务器会话模型。
  - 引入 WaveTerm 的上传、监控等非 AI 功能。
  - 生产级别的模型接入（如细粒度计费、审计）超出本轮集成，后续迭代推进。

## 4. 技术架构调整
1. **模块划分**
   - 在 `com.cmict.internalpaas` 下新增 `ai`（或 `chat`）包，保留 WaveTerm 的 `ai`、`model`、`stream` 结构，统一命名风格。
   - 为命令补全、聊天上下文分别创建子模块，避免与现有 DTO、事件冲突。
2. **接口设计**
   - REST：`/ai/chat`、`/ai/chat/stream`、`/ai/completion` 等端点统一归档在 `AiController` 中，初期提供 JSON + SSE/NDJSON 两种模式。
   - 内部交互：`SSHTerminalService` 引入可选的 `AiSuggestionService` 钩子，用于在 session 事件中触发推荐。
3. **安全鉴权**
   - 基于角色控制访问 (`hasRole('ADMIN')`/`hasRole('DEVELOPER')`)；如需支持外部调用，可配置短期 JWT 签发器，兼容 WaveTerm 客户端。
4. **配置加载**
   - 新建 `application-ai.yml`（可在 `config` profile 下导入）或在现有配置中加入 `ai.providers.*`、`ai.chat.*` 节点。
   - 密钥统一放入环境变量或 Vault，避免硬编码。
5. **依赖** 
   - 补充 HTTP 客户端（Spring WebFlux/WebClient 或 OkHttp）、JSON 处理（Jackson 已存在）。
   - 确认与现有 Spring Boot、Java 17 版本兼容，必要时升级 Maven 依赖。 

## 5. 实施步骤
| 阶段 | 工作项 | 产物 |
| --- | --- | --- |
| Phase 0 | 立项 & 环境准备：确认模型供应方、密钥、网络策略；梳理现有 `pom.xml` 与配置 | 集成评估结论、依赖清单 |
| Phase 1 | 代码引入：复制/改造 WaveTerm AI 包，完成包名、Bean 定义、配置绑定、单元测试 | 基础服务层代码、配置模板 |
| Phase 2 | 接口整合：实现 REST/SSE 控制器，补充 `SecurityConfig`、`WebMvc` 跨域/消息转换配置 | API 文档草案（`docs/api`）、Postman 集合 |
| Phase 3 | 前端适配：在 `terminal/manager.html` 或新模块中嵌入对话/补全入口，封装 Fetch/WebSocket 调用 | 前端分支、交互原型 |
| Phase 4 | 联调与回归：模拟会话场景、限流测试、错误处理、日志审计 | 测试报告（`docs/testing`）、风险清单更新 |
| Phase 5 | 上线准备：编写运维指南、配置部署脚本、监控指标 | 运维文档（`docs/operations` 或 `docs/development`）、发布核对表 |

## 6. 依赖与配置要求
- **模型密钥**：Moonshot/Ollama 等至少准备一个默认模型；无本地模型时可启用 Echo 模式做演示。
- **网络出口**：若调用外部模型，需要开放对应域名；离线环境需准备本地模型服务。
- **配置项**：
  ```yaml
  ai:
    providers:
      moonshot:
        enabled: true
        base-url: https://api.moonshot.cn
        api-key: ${MOONSHOT_API_KEY}
      ollama:
        enabled: false
        base-url: http://localhost:11434
    chat:
      default-provider: moonshot
      max-context-messages: 20
      streaming-enabled: true
  ```
- **日志/审计**：与 `UserActivityService` 对接，记录 AI 请求、模型、耗时等关键指标。

## 7. 风险与缓解
- **依赖冲突**：WaveTerm 采用 Spring Boot 3.x，需确认现有项目版本；如差异较大，可通过 shading 或独立 starter 方式隔离。
- **安全合规**：AI 调用可能涉及敏感命令/日志，需在 Web 层添加内容审查与脱敏逻辑。
- **性能压力**：流式响应对线程池、连接池提出要求，建议在 `application-ai` profile 中配置限流与超时。
- **前端体验**：终端页面已有大量脚本，新增 AI 面板可能影响加载时间，应分包或懒加载。
- **运维复杂度**：多模型支持需要额外监控，推荐引入健康检查与 fallback 机制。

## 8. 验收标准与测试思路
- REST 接口通过 `./mvnw.cmd clean verify` 期间的集成测试验证，包含：
  - 成功对话、超时、供应商不可用等分支。
  - 命令补全在无模型时自动降级。
- 前端交互：终端页 AI 面板可发起请求、收到流式输出，并能关联当前会话上下文。
- 安全性：未授权用户无法访问 `/ai/**`，日志中记录调用主体与耗时。
- 运维：提供完整配置清单、启动/切换模型的操作指引。

## 9. 时间与资源建议
- 开发：后端 1 人周、前端 0.5 人周；若需上线 Moonshot，需要额外 0.5 人周做对接。
- 测试：1 名 QA 完成接口与前端联调，预留 3~4 天。
- 运维：与安全团队确认密钥管理方案，预留 2 天培训。

## 10. 后续方向
- 与终端会话更深融合：在命令执行日志中自动建议下一步操作。
- 引入知识库/Agent：结合内部文档，实现上下文增强。
- 统一 AI 网关：若后续还要集成其他系统，可沉淀为共享的 AI 接入平台。
