# Implementation Tasks: AI Chat Streaming Output

**Feature Branch**: `001-ai-chat-streaming`
**Created**: 2025-10-21
**Status**: Ready for Implementation

**Related Documents**:
- [Feature Specification](spec.md) - 用户需求和验收标准
- [Implementation Plan](plan.md) - 技术方案和架构
- [Technical Research](research.md) - 技术决策详解
- [Data Model](data-model.md) - 数据结构设计
- [API Contract](contracts/user-preferences-api.md) - API 规范
- [Quickstart Guide](quickstart.md) - 实现指南

## Summary

本任务清单基于 3 个用户故事（P1-P3）生成，按优先级组织任务以支持增量交付。每个用户故事都是独立可测试的功能增量。

**总任务数**: 22 个任务
**并行机会**: 8 个任务可并行执行
**预计时间**: 3-4 小时（包含测试）

## Implementation Strategy

### MVP 优先策略
- **MVP 范围**: User Story 1 (P1) - 核心打字机效果
- **增量交付**: P1 → P2 → P3，每个故事独立可部署
- **独立测试**: 每个故事完成后立即可测试验证

### 依赖关系
```
Phase 1: Setup (无依赖)
    ↓
Phase 2: Foundational (依赖 Phase 1)
    ↓
Phase 3: User Story 1 - P1 (依赖 Phase 2) ← MVP
    ↓
Phase 4: User Story 2 - P2 (依赖 Phase 3)
    ↓
Phase 5: User Story 3 - P3 (依赖 Phase 3，不依赖 Phase 4)
    ↓
Phase 6: Polish & Integration (依赖所有 User Stories)
```

### 并行执行机会

**Phase 1 - Setup 阶段**:
```bash
# 后端和前端任务可并行
T001 (后端 DTO 扩展) || T002 (前端 TypewriterRenderer 类)
```

**Phase 3 - User Story 1 阶段**:
```bash
# 独立文件修改可并行
T006 (startAiStream 修改) || T007 (CSS 样式) || T008 (设置 UI)
```

**Phase 4 - User Story 2 阶段**:
```bash
# CSS 和逻辑可并行
T013 (视觉指示器 CSS) || T014 (视觉指示器逻辑)
```

**Phase 5 - User Story 3 阶段**:
```bash
# 三个控制功能可并行实现
T017 (点击跳过) || T018 (快捷键跳过) || T019 (偏好设置禁用)
```

---

## Phase 1: Setup (项目初始化)

**目标**: 准备后端和前端基础结构

**任务清单**:

- [ ] T001 [P] 扩展 UserPreferencesDto 添加流式输出字段 (`src/main/java/com/cmict/internalpaas/dto/UserPreferencesDto.java`)
  - 添加 `private Boolean aiStreamingEnabled = true;`
  - 添加 `private String aiStreamingSpeed = "normal";`
  - 添加 Getters/Setters，在 `setAiStreamingSpeed` 中验证值（slow/normal/fast）

- [ ] T002 [P] 实现 TypewriterRenderer 类 (`src/main/resources/static/js/ai-panel.js`)
  - 在文件开头创建 `TypewriterRenderer` 类
  - 实现构造函数、start、pause、resume、skipToEnd、_renderFrame 方法
  - 使用 requestAnimationFrame 实现平滑渲染

- [ ] T003 添加辅助工具函数 (`src/main/resources/static/js/ai-panel.js`)
  - 实现 `stripHtmlTags(html)` - 去除 HTML 标签
  - 实现 `getUserStreamingSpeed()` - 获取用户配置速度
  - 实现 `isStreamingEnabled()` - 检查是否启用流式输出

- [ ] T004 验证后端编译通过
  - 运行 `mvn clean compile`
  - 确认无编译错误

---

## Phase 2: Foundational (基础组件)

**目标**: 实现核心格式化渲染逻辑，为所有用户故事奠定基础

**任务清单**:

- [ ] T005 创建 applyFormatting 函数 (`src/main/resources/static/js/ai-panel.js`)
  - 提取现有格式化代码（postEnhanceTables, Prism, addCodeActions）到独立函数
  - 接受参数: aiEl, body, htmlContent
  - 添加淡入动画类 `format-applied`

---

## Phase 3: User Story 1 - Real-time AI Response Display (P1 - MVP)

**Story Goal**: 实现核心打字机效果，AI 回复逐字符流式显示

**Independent Test**: 发送任意消息到 AI 聊天面板，观察 AI 回复逐字符显示（而非一次性显示），流式完成后内容格式化渲染（Markdown、代码高亮）正常工作。

**Acceptance Criteria**:
1. ✅ 用户发送消息，AI 响应逐字符显示
2. ✅ 字符以一致可读速度显示（不快不慢）
3. ✅ 流式输出跨句子边界平滑连续

**任务清单**:

- [ ] T006 [P] 修改 startAiStream 函数集成打字机渲染 (`src/main/resources/static/js/ai-panel.js`)
  - 在 `event === 'done'` 分支中：
    - 检查 `isStreamingEnabled()`
    - 如果启用：创建 `.streaming-text` 容器，启动 TypewriterRenderer
    - 如果禁用：直接调用 `applyFormatting`
  - 设置 `onComplete` 回调为 `applyFormatting`

- [ ] T007 [P] 添加流式输出 CSS 样式 (`src/main/resources/static/css/ai-panel.css`)
  - 添加 `.streaming-text` 样式（字体、行高、光标指示）
  - 添加 `::after` 伪元素实现闪烁光标动画
  - 添加 `.format-applied` 淡入动画

- [ ] T008 [P] 在 AI 面板 HTML 添加设置 UI (`src/main/resources/templates/terminal/ai-panel.html`)
  - 在设置抽屉中添加"AI 聊天设置"区域
  - 添加"启用打字机效果"复选框 (`#aiStreamingEnabled`)
  - 添加"流式速度"下拉选择 (`#aiStreamingSpeed`，选项: slow/normal/fast)

- [ ] T009 实现设置初始化和保存逻辑 (`src/main/resources/static/js/ai-panel.js`)
  - 实现 `initStreamingPreferences()` 函数
  - 从 LocalStorage 加载设置到 UI
  - 监听设置变化，更新 LocalStorage
  - 实现 `syncPreferencesToBackend()` 异步同步到 `/profile/preferences` API

- [ ] T010 在页面加载时初始化流式偏好 (`src/main/resources/static/js/ai-panel.js`)
  - 在 DOMContentLoaded 事件中调用 `initStreamingPreferences()`

- [ ] T011 [US1] 手动测试 User Story 1 功能
  - 启动应用 (`mvn spring-boot:run`)
  - 访问 AI 聊天面板，发送消息
  - 验证：AI 回复逐字符显示
  - 验证：流式完成后格式化渲染正常（Markdown、代码高亮）
  - 验证：闪烁光标在流式输出末尾显示

---

## Phase 4: User Story 2 - Visual Feedback During Streaming (P2)

**Story Goal**: 增强用户信心，提供明确的流式进度视觉反馈

**Independent Test**: 发送生成长响应的消息，验证在流式开始时显示加载指示器，流式进行中光标闪烁，流式完成后指示器消失。

**Acceptance Criteria**:
1. ✅ AI 开始响应前显示 typing indicator
2. ✅ 流式进行中末尾显示闪烁光标
3. ✅ 流式完成后所有指示器消失

**Dependencies**: 依赖 Phase 3 (US1) - 核心流式渲染必须先实现

**任务清单**:

- [ ] T012 [US2] 在 startAiStream 中添加初始 typing indicator (`src/main/resources/static/js/ai-panel.js`)
  - 在 AI 消息创建后，流式开始前显示"AI 正在思考..."提示
  - 流式开始时移除 typing indicator

- [ ] T013 [P] [US2] 添加 typing indicator CSS 样式 (`src/main/resources/static/css/ai-panel.css`)
  - 添加 `.typing-indicator` 样式
  - 添加脉动/闪烁动画

- [ ] T014 [P] [US2] 在 TypewriterRenderer 完成时清理指示器 (`src/main/resources/static/js/ai-panel.js`)
  - 在 `_onComplete` 方法中移除 `.streaming-text::after` 光标
  - 确保格式化完成后无残留指示器

- [ ] T015 [US2] 手动测试 User Story 2 功能
  - 发送消息，验证流式开始前显示 typing indicator
  - 验证流式进行中光标闪烁
  - 验证流式完成后所有指示器消失

---

## Phase 5: User Story 3 - User Control Over Streaming (P3)

**Story Goal**: 赋予用户控制流式行为的能力（跳过、速度调整、全局开关）

**Independent Test**: 触发长 AI 响应，测试：(1) 点击流式消息立即显示完整内容，(2) 按 ESC/Space 立即显示，(3) 在设置中禁用流式输出后新消息立即显示。

**Acceptance Criteria**:
1. ✅ 点击流式消息区域立即显示全部
2. ✅ 按 ESC/Space 键立即显示全部
3. ✅ 禁用流式输出时 AI 回复立即完整显示

**Dependencies**: 依赖 Phase 3 (US1) - 核心流式渲染必须先实现（不依赖 Phase 4）

**任务清单**:

- [ ] T016 [P] [US3] 为 .streaming-text 容器添加点击跳过监听器 (`src/main/resources/static/js/ai-panel.js`)
  - 在 startAiStream 中创建 typewriter 后绑定 click 事件
  - 调用 `typewriter.skipToEnd()`

- [ ] T017 [P] [US3] 添加全局键盘快捷键监听器 (`src/main/resources/static/js/ai-panel.js`)
  - 监听 ESC 和 Space 键
  - 如果当前有活跃的 typewriter，调用 `skipToEnd()`
  - 存储当前活跃 typewriter 引用（全局变量或模块变量）

- [ ] T018 [P] [US3] 实现偏好设置禁用流式输出逻辑 (`src/main/resources/static/js/ai-panel.js`)
  - 在 startAiStream 的 `event === 'done'` 分支中检查 `isStreamingEnabled()`
  - 如果禁用，直接调用 `applyFormatting()`，跳过 TypewriterRenderer

- [ ] T019 [US3] 添加流式跳过提示 hover 效果 CSS (`src/main/resources/static/css/ai-panel.css`)
  - 为 `.streaming-text:hover::before` 添加"点击跳过"提示
  - 设置半透明背景和适当定位

- [ ] T020 [US3] 手动测试 User Story 3 功能
  - 测试点击流式消息立即显示
  - 测试按 ESC 键立即显示
  - 测试按 Space 键立即显示
  - 测试禁用流式输出设置后新消息立即完整显示
  - 测试调整速度设置（slow/normal/fast）生效

---

## Phase 6: Polish & Integration (完善与集成)

**目标**: 边界情况处理、性能优化、最终验证

**任务清单**:

- [ ] T021 处理边界情况 (`src/main/resources/static/js/ai-panel.js`)
  - 用户发送新消息时，立即完成前一条流式输出（调用 `skipToEnd()`）
  - 处理特殊字符（emoji, Unicode）：使用 `Array.from(text)` 正确分割
  - 超长响应（>10,000 字符）自动提升速度或跳过流式

- [ ] T022 最终端到端测试
  - 运行完整测试清单（见 quickstart.md）
  - 验证所有 3 个用户故事的验收标准
  - 验证边界情况处理（并发、网络中断、特殊字符、超长响应）
  - 验证现有功能不受影响（Markdown、代码高亮、历史记录、表格）
  - 验证性能指标（首字符 <100ms，10K 字符无卡顿，中断 <200ms）

---

## Testing Checklist (手动测试清单)

### User Story 1 (P1) - 核心功能
- [ ] 发送消息，AI 回复逐字符显示
- [ ] 字符显示速度一致可读
- [ ] 流式完成后 Markdown 渲染正常
- [ ] 代码块高亮正常
- [ ] 表格格式正确
- [ ] 历史记录保存完整内容

### User Story 2 (P2) - 视觉反馈
- [ ] 流式开始前显示 typing indicator
- [ ] 流式进行中光标闪烁
- [ ] 流式完成后指示器消失

### User Story 3 (P3) - 用户控制
- [ ] 点击流式消息立即完整显示
- [ ] 按 ESC 键立即显示
- [ ] 按 Space 键立即显示
- [ ] 禁用流式输出设置生效
- [ ] 速度调整（slow/normal/fast）生效

### 边界情况
- [ ] 发送新消息时前一条流式立即完成
- [ ] 超长响应（5000+ 字符）性能良好
- [ ] 特殊字符（emoji）正确显示
- [ ] 网络中断时部分内容保留
- [ ] 用户滚动查看历史时流式不受影响
- [ ] 并发多条消息各自独立流式

### 兼容性
- [ ] 现有格式化功能（Markdown, Prism）正常
- [ ] 聊天历史加载正常
- [ ] 新建会话功能正常
- [ ] 其他 AI 面板功能不受影响

---

## Performance Benchmarks

验证以下性能指标：

| 指标 | 目标 | 验证方法 |
|------|------|---------|
| 首字符显示延迟 | <100ms | Chrome DevTools Performance 标签 |
| 10,000 字符流畅度 | 无卡顿 | 发送生成长响应的消息，观察 FPS |
| 中断响应时间 | <200ms | 点击跳过，测量到完整显示的时间 |
| 格式化渲染时间 | <500ms | 流式完成到格式化完成的时间 |
| 并发会话支持 | 100+ | 理论验证（无共享状态） |

---

## Task Summary

### 总体统计
- **总任务数**: 22 个
- **Setup 阶段**: 4 个任务
- **Foundational 阶段**: 1 个任务
- **User Story 1 (P1)**: 6 个任务 ← MVP
- **User Story 2 (P2)**: 4 个任务
- **User Story 3 (P3)**: 5 个任务
- **Polish 阶段**: 2 个任务

### 并行机会
- **8 个任务可并行执行** (标记为 [P])
- Phase 1: 2 个并行任务（T001, T002）
- Phase 3: 3 个并行任务（T006, T007, T008）
- Phase 4: 2 个并行任务（T013, T014）
- Phase 5: 3 个并行任务（T016, T017, T018）

### 每个用户故事的任务数
- **US1 (P1)**: 6 个实现任务 + 1 个测试任务 = 7 个总任务
- **US2 (P2)**: 3 个实现任务 + 1 个测试任务 = 4 个总任务
- **US3 (P3)**: 4 个实现任务 + 1 个测试任务 = 5 个总任务

### 独立测试标准
每个用户故事都有明确的 **Independent Test** 标准，确保可独立验证：
- **US1**: 发送消息 → 观察逐字符显示 → 验证格式化正常
- **US2**: 发送长消息 → 观察指示器行为（出现 → 闪烁 → 消失）
- **US3**: 触发流式 → 测试 3 种控制方式（点击、快捷键、设置）

### 建议的 MVP 范围
- **推荐**: 仅实现 Phase 1 + Phase 2 + Phase 3 (User Story 1)
- **理由**: US1 包含核心打字机效果，已能显著提升用户体验
- **交付时间**: 约 2 小时（含测试）
- **后续迭代**: P2 和 P3 可在后续版本中增量添加

---

## Implementation Notes

### 关键文件清单
```
# 后端修改（1 个文件）
src/main/java/com/cmict/internalpaas/dto/UserPreferencesDto.java

# 前端修改（3 个文件）
src/main/resources/static/js/ai-panel.js        # 主要逻辑
src/main/resources/static/css/ai-panel.css      # 样式
src/main/resources/templates/terminal/ai-panel.html  # 设置 UI
```

### 代码变更统计（预估）
- **后端**: +20 行（DTO 字段和验证）
- **前端 JS**: +300 行（TypewriterRenderer 类 + 集成逻辑）
- **前端 CSS**: +50 行（动画样式）
- **前端 HTML**: +20 行（设置 UI）
- **总计**: ~390 行新增代码

### 风险与缓解
| 风险 | 可能性 | 影响 | 缓解措施 |
|------|-------|------|---------|
| 性能问题（长响应卡顿） | 低 | 中 | requestAnimationFrame 优化，超长响应自动加速 |
| 格式化渲染冲突 | 低 | 高 | 两阶段渲染隔离，流式阶段仅显示纯文本 |
| 用户偏好同步失败 | 中 | 低 | LocalStorage 优先，后端异步同步失败不影响使用 |
| 边界情况未覆盖 | 中 | 中 | 全面的手动测试清单，Phase 6 专门处理边界情况 |

### 回滚策略
如需回滚：
1. 前端 JavaScript 回滚（删除 TypewriterRenderer 类和调用）
2. 前端 CSS 和 HTML 回滚（删除流式相关样式和设置 UI）
3. 后端保留 DTO 字段（不影响其他功能，未来可重新启用）

---

## Next Steps

### 立即开始实现
```bash
# 1. 确认在正确的分支
git branch  # 应该在 001-ai-chat-streaming

# 2. 开始第一个任务
# 按顺序执行 tasks.md 中的任务
# 从 T001 开始，逐个完成

# 3. 每完成一个 User Story，立即测试
# 完成 Phase 3 (US1) 后立即测试核心功能
```

### 使用快速开始指南
参考 [quickstart.md](quickstart.md) 获取：
- 详细的代码示例
- 分步骤实现指导
- 常见问题解答
- 故障排查提示

### 需要帮助？
- **技术细节**: 参考 [research.md](research.md) 了解技术决策
- **数据结构**: 参考 [data-model.md](data-model.md) 了解数据流程
- **API 规范**: 参考 [contracts/user-preferences-api.md](contracts/user-preferences-api.md)
