# Implementation Plan: AI Chat Streaming Output

**Branch**: `001-ai-chat-streaming` | **Date**: 2025-10-21 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-ai-chat-streaming/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

为 SSH 终端的 AI 聊天面板增加流式输出能力，实现打字机效果逐字符显示 AI 回复内容。当前系统已支持后端 SSE 流式响应，但前端一次性显示完整内容。本功能将在前端实现字符级流式渲染，提升用户体验和交互自然度。

## Technical Context

**Language/Version**: Java 17, JavaScript ES6+
**Primary Dependencies**:
- 后端: Spring Boot 3.2.0, Spring WebFlux (SSE 支持)
- 前端: Vanilla JavaScript, Markdown-it (Markdown 渲染), Prism.js (代码高亮)
**Storage**: H2/SQLite 数据库 (用户偏好设置存储), LocalStorage (会话持久化)
**Testing**: JUnit 5 (后端单元测试), 手动浏览器测试 (前端交互测试)
**Target Platform**: Web 应用 (现代浏览器 Chrome/Firefox/Safari/Edge)
**Project Type**: Web 应用 (后端 Spring Boot + 前端 JavaScript)
**Performance Goals**:
- 流式渲染延迟 < 100ms (从接收字符到显示)
- 支持 10,000 字符响应无卡顿
- 打字机动画帧率 ≥ 30 FPS
- 支持 100 并发聊天会话
**Constraints**:
- 必须兼容现有 SSE 流式架构 (/ai/chat/stream 端点)
- 必须保持 Markdown 渲染和代码高亮功能
- 用户偏好设置需持久化
- 不能影响现有聊天历史功能
**Scale/Scope**:
- 单个前端 JavaScript 模块增强 (ai-panel.js)
- 1 个新增后端配置端点 (用户偏好)
- 约 200-300 行新增前端代码
- 无需数据库迁移 (复用现有 User 表)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**状态**: ⚠️ 项目未定义 Constitution 文件

Constitution 模板文件 (`.specify/memory/constitution.md`) 仅包含占位符，未定义实际项目规范。由于这是现有项目的功能增强，应用以下通用最佳实践：

- ✅ **最小化变更**: 仅修改前端展示逻辑，复用现有 SSE 流式架构
- ✅ **向后兼容**: 通过用户偏好开关控制，默认启用但可关闭
- ✅ **测试覆盖**: 需要端到端浏览器测试验证流式动画效果
- ✅ **性能优先**: 使用 requestAnimationFrame 优化渲染性能

## Project Structure

### Documentation (this feature)

```
specs/001-ai-chat-streaming/
├── plan.md              # 本文件 (实现计划)
├── spec.md              # 功能规格说明
├── research.md          # Phase 0 输出 (技术研究)
├── data-model.md        # Phase 1 输出 (数据模型)
├── quickstart.md        # Phase 1 输出 (快速开始指南)
├── contracts/           # Phase 1 输出 (API 契约)
├── checklists/          # 质量检查清单
│   └── requirements.md  # 规格验证清单
└── tasks.md             # Phase 2 输出 (/speckit.tasks 命令 - 尚未创建)
```

### Source Code (repository root)

本功能基于现有 Web 应用架构，主要修改以下文件：

```
src/main/
├── java/com/cmict/internalpaas/
│   ├── controller/
│   │   └── UserController.java              # [修改] 增加用户偏好 API
│   ├── model/
│   │   └── User.java                        # [查看] 确认偏好字段
│   ├── service/
│   │   └── UserService.java                 # [修改] 用户偏好业务逻辑
│   └── dto/
│       └── UserPreferencesDto.java          # [已存在] 复用现有 DTO
│
└── resources/
    ├── static/
    │   ├── js/
    │   │   └── ai-panel.js                  # [主要修改] 实现流式渲染逻辑
    │   └── css/
    │       └── ai-panel.css                 # [修改] 流式动画样式
    └── templates/
        └── terminal/
            └── ai-panel.html                # [修改] 用户偏好设置 UI

tests/
└── [手动浏览器测试] 流式动画效果验证
```

**Structure Decision**: 采用 Web 应用结构 (Option 2)，前后端分离但在同一 Spring Boot 项目中。前端使用 Thymeleaf 模板 + Vanilla JavaScript，后端提供 RESTful API。本功能主要涉及前端 JavaScript 逻辑增强和少量后端 API 扩展。

### 关键文件说明

- **ai-panel.js**: 核心前端逻辑文件，包含现有的 SSE 流式接收代码 (`startAiStream` 函数)，需增加字符级流式渲染逻辑
- **UserPreferencesDto.java**: 已存在的用户偏好 DTO，包含各类偏好设置字段
- **UserController.java**: 已存在用户偏好更新端点 (`POST /profile/preferences`)

## Complexity Tracking

*无需填写 - 本功能未违反 Constitution 规范 (项目未定义 Constitution)*

本功能复杂度评估：
- **代码变更范围**: 小 (单个 JS 文件为主)
- **架构影响**: 无 (复用现有 SSE 流式架构)
- **风险等级**: 低 (前端展示层改动，可通过偏好开关回滚)

