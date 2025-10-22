# Implementation Plan: AI助手输入框UI优化

**Branch**: `002-refine-input-ui` | **Date**: 2025-10-22 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-refine-input-ui/spec.md`

## Summary

本功能旨在参考提供的截图优化AI助手输入框的用户界面，重点改进视觉层次、用户引导和交互体验。主要改进包括：独立的上下文标签区域、增强的占位符提示、图标化的工具栏按钮、优化的下拉选择器。**技术方法**：纯前端CSS/HTML/JavaScript修改，无后端API或数据库变更。

## Technical Context

**Language/Version**: JavaScript (ES6+), HTML5, CSS3
**Primary Dependencies**:
  - 现有依赖: xterm.js (终端模拟), SockJS/STOMP (WebSocket通信)
  - 无需新增依赖 (使用内联SVG和Unicode图标)
**Storage**: N/A (纯UI优化，无数据存储需求)
**Testing**: 浏览器手动测试 + 视觉回归测试 (可选)
**Target Platform**: 现代Web浏览器 (Chrome, Firefox, Edge, Safari最新版本)
**Project Type**: Web应用 (Spring Boot后端 + Thymeleaf模板 + 静态资源前端)
**Performance Goals**:
  - 界面响应时间 < 50ms (点击到视觉反馈)
  - 支持500+字符输入无卡顿
  - CSS动画保持60 FPS
**Constraints**:
  - 仅修改前端文件 (`ai-assistant.js`, `ai-assistant.css`)
  - 保留所有现有功能 (流式输出、键盘快捷键、#上下文选择)
  - 不引入新的第三方库
**Scale/Scope**:
  - 修改2个文件（1个JS + 1个CSS）
  - 新增约200行CSS
  - 修改约50行HTML结构
  - 新增3个工具栏按钮（设置、发送、更多）

## Constitution Check

*宪法文件尚未初始化，跳过此检查*

**评估结果**: ✅ 无宪法违规
- 本项目为纯UI/CSS优化，不涉及架构、测试、库等宪法通常关注的方面
- 不引入新的复杂性或依赖
- 完全向后兼容，无破坏性变更

## Project Structure

### Documentation (this feature)

```
specs/002-refine-input-ui/
├── spec.md              # 功能规范
├── plan.md              # 本文件 - 实施计划
├── research.md          # Phase 0输出 - CSS设计模式研究
├── quickstart.md        # Phase 1输出 - 实施快速指南
└── checklists/
    └── requirements.md  # 规范质量检查清单
```

**说明**: 由于本项目是纯前端UI优化，不涉及数据模型或API契约，因此**不创建**以下文件：
- `data-model.md` (无数据模型)
- `contracts/` (无API契约)

### Source Code (repository root)

```
src/main/resources/
├── static/
│   ├── css/
│   │   └── ai-assistant.css       # 主要修改：新增约200行CSS
│   └── js/
│       └── ai-assistant.js         # 主要修改：重构HTML结构、新增事件处理
└── templates/
    └── terminal/
        └── manager.html            # 引用ai-assistant.js的主页面（无需修改）
```

**Structure Decision**: 本项目遵循Spring Boot Web应用的标准结构，所有前端静态资源位于`src/main/resources/static/`，使用Thymeleaf模板引擎。由于仅为UI优化，无需改动后端Java代码或模板文件，所有变更集中在2个前端文件中。

## Complexity Tracking

**无违规项** - 本项目不引入任何复杂性，仅优化现有UI元素的视觉呈现和布局。

---

## Phase 0: 大纲与研究

**输出**: `research.md`

### 研究任务

由于这是一个UI优化项目，研究重点在视觉设计模式和CSS最佳实践：

1. **标签区域设计模式**
   - 研究: Material Design / Fluent Design中的Chip组件设计模式
   - 决策点: 背景色、间距、圆角半径、关闭按钮样式

2. **工具栏图标化趋势**
   - 研究: 现代Web应用的工具栏设计（VS Code、GitHub、Linear等）
   - 决策点: 图标尺寸、间距、悬停效果、图标来源（SVG vs Unicode）

3. **下拉选择器UI模式**
   - 研究: 自定义select元素的最佳实践
   - 决策点: 是否使用原生`<select>`还是自定义组件、下拉箭头实现方式

4. **CSS变量系统**
   - 研究: 现有`ai-assistant.css`的颜色/间距系统
   - 决策点: 是否引入CSS变量统一管理主题色

5. **响应式设计**
   - 研究: 输入框在移动端的最佳实践
   - 决策点: 断点选择、移动端标签布局

**无需澄清项**: 所有技术细节基于截图参考和现有代码结构，无NEEDS CLARIFICATION标记。

---

## Phase 1: 设计与契约

**输出**: `quickstart.md`

### 数据模型

**N/A** - 本项目无数据模型变更，所有状态管理在JavaScript变量中（`contextSessions[]`, `isHashtagMenuOpen`等），与现有实现一致。

### API契约

**N/A** - 本项目不涉及任何后端API变更，所有交互纯前端。

### 快速开始指南

`quickstart.md`将包含：

1. **开发环境设置**
   - 克隆分支 `002-refine-input-ui`
   - 启动Spring Boot应用
   - 导航到`http://localhost:8080/terminal/manager`

2. **实施步骤概览**
   - 步骤1: HTML结构重构（标签栏、工具栏分离）
   - 步骤2: CSS样式新增（标签、图标按钮、下拉箭头）
   - 步骤3: JavaScript事件绑定更新
   - 步骤4: 视觉测试和微调

3. **测试清单**
   - 上下文标签显示/移除
   - 占位符文本提示
   - 工具栏按钮响应
   - 下拉选择器交互
   - 发送按钮状态变化

4. **回滚计划**
   - Git回退到合并前状态
   - 无数据库迁移或配置变更

---

## 后续阶段

**Phase 2 (任务分解)** 将由 `/speckit.tasks` 命令生成`tasks.md`，不在本计划范围内。

预期任务结构：
- T001: 重构HTML结构 - 分离标签栏和工具栏
- T002: 实现上下文标签样式
- T003: 实现占位符多功能提示
- T004: 添加工具栏图标按钮（设置、更多）
- T005: 优化下拉选择器样式
- T006: 改进发送按钮图标
- T007: 添加悬停和聚焦效果
- T008: 响应式设计调整
- T009: 浏览器兼容性测试
- T010: 视觉回归测试（对比截图）

---

**计划状态**: ✅ 完成
**准备就绪**: Phase 0研究 → Phase 1快速指南 → Phase 2任务分解(/speckit.tasks)
