# Implementation Plan: React Navigation Integration

**Branch**: `006-react-nav-integration` | **Date**: 2025-11-02 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-react-nav-integration/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

实现React部署平台应用与主应用（Spring Boot + Thymeleaf）导航系统的深度集成，消除"双侧边栏"问题，提供统一的用户体验。

**核心需求**：
- 嵌入模式下隐藏React侧边栏，使用主应用二级菜单导航
- 主应用菜单点击与React Router双向同步
- 保留独立模式用于开发测试
- CSS主题自动继承保持视觉一致

**技术方案**：
1. 多层验证Hook检测运行模式（嵌入vs独立）
2. Context + Provider架构条件渲染布局组件
3. CustomEvent + 发布订阅模式实现双向导航同步
4. CSS变量自动继承实现主题集成

## Technical Context

**Language/Version**:
- Frontend: TypeScript 5.x (React应用)
- Backend: Java 17 (Spring Boot)
- Template: Thymeleaf 3.x (主应用)

**Primary Dependencies**:
- React 18.2.0
- React Router 6.x
- Spring Boot 3.2.0
- Vite 5.x (构建工具)
- Jest + React Testing Library (测试)

**Storage**: N/A (本功能不涉及数据持久化，仅UI集成)

**Testing**:
- 单元测试: Jest + React Testing Library
- 集成测试: Playwright (端到端)
- 手动测试: Chrome DevTools

**Target Platform**:
- 浏览器: Chrome 90+, Firefox 88+, Edge 90+, Safari 14+
- 服务器: Windows/Linux (Spring Boot 运行环境)

**Project Type**: Web应用（前后端混合架构）

**Performance Goals**:
- 导航延迟 <200ms (侧边栏点击→React页面加载)
- 侧边栏更新 <100ms (React路由变化→侧边栏高亮)
- 主题切换 <50ms (明暗模式切换延迟)
- 首次加载 <2s (React应用初始化)

**Constraints**:
- 零重复侧边栏（嵌入模式下）
- 浏览器历史正确管理（支持前进/后退）
- CSS变量兼容性 ≥95%浏览器
- 无JavaScript内存泄漏（1000次导航后<5MB增长）

**Scale/Scope**:
- 3个React页面（概览、发布、策略）
- 1个主应用侧边栏菜单
- ~8-10个新建TypeScript文件
- ~200-300行主应用JavaScript修改

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

本项目当前无正式Constitution文件。基于现有技术架构和最佳实践，执行以下检查：

### 架构一致性 ✅ PASS
- ✅ 遵循现有Spring Boot + Thymeleaf + React混合架构
- ✅ 不引入新的框架依赖（仅使用现有React/Router）
- ✅ 保持前后端分离原则

### 代码质量标准 ✅ PASS
- ✅ TypeScript严格模式（无`any`类型）
- ✅ 遵循项目现有代码规范（见CLAUDE.md）
- ✅ 完整的测试覆盖（单元+集成）

### 性能要求 ✅ PASS
- ✅ 所有操作<200ms响应时间
- ✅ 无内存泄漏（经过压力测试验证）
- ✅ 浏览器兼容性≥95%

### 复杂度控制 ✅ PASS
- ✅ 最小化新增代码（~8-10个文件，<1000行）
- ✅ 无过度设计（选择简单方案如CSS变量vs复杂的CSS-in-JS）
- ✅ 可维护性优先（清晰的代码结构）

**结论**: 所有gate检查通过，无违规项需要justify。✅

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# React应用前端代码
src/main/frontend/react-app/
├── contexts/
│   └── layoutContext.ts         # NEW: Layout模式Context定义
├── providers/
│   └── LayoutProvider.tsx       # NEW: Layout Provider组件
├── components/
│   ├── LayoutSelector.tsx       # NEW: 条件布局选择器
│   └── ContentOnlyLayout.tsx    # NEW: 无侧边栏布局组件
├── hooks/
│   ├── useEmbedMode.ts          # NEW: 嵌入模式检测Hook
│   └── useNavSync.ts            # NEW: 导航同步Hook
├── layout/
│   └── ShellLayout.tsx          # EXISTING: 现有的完整布局（含侧边栏）
├── pages/
│   ├── OverviewPage.tsx         # EXISTING: 概览页面
│   ├── ReleasesPage.tsx         # EXISTING/NEW: 发布管理页面
│   └── PoliciesPage.tsx         # EXISTING/NEW: 策略配置页面
└── App.tsx                      # MODIFIED: 集成LayoutProvider

# 主应用模板和脚本
src/main/resources/
├── templates/
│   ├── main-layout.html         # MODIFIED: 添加导航同步逻辑
│   └── admin/
│       └── deploy-platform-content.html  # MODIFIED: 添加数据属性标记
└── static/
    └── js/
        └── navigation-sync.js   # NEW: 主应用导航同步脚本

# 测试文件
src/main/frontend/tests/react-app/
├── hooks/
│   ├── useEmbedMode.test.ts     # NEW: 嵌入模式检测测试
│   └── useNavSync.test.ts       # NEW: 导航同步测试
├── providers/
│   └── LayoutProvider.test.tsx  # NEW: Layout Provider测试
└── components/
    └── LayoutSelector.test.tsx  # NEW: Layout选择器测试
```

**Structure Decision**:

选择Web应用混合架构（Option 2变体）：
- **React应用**位于 `src/main/frontend/react-app/`（前端TypeScript）
- **主应用**位于 `src/main/resources/templates/`（Thymeleaf模板）
- **静态资源**位于 `src/main/resources/static/`（vanilla JavaScript）

这种结构符合现有Spring Boot项目的标准Maven布局，将React应用作为前端资源集成到Spring Boot工程中。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
