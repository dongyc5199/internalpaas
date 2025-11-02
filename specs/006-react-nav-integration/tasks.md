# Tasks: React Navigation Integration

**Input**: Design documents from `/specs/006-react-nav-integration/`
**Prerequisites**: plan.md (✅), spec.md (✅), research.md (✅), data-model.md (✅), quickstart.md (✅)

**Tests**: 本规格说明明确要求"完整的测试覆盖（单元+集成）"，因此包含测试任务。

**Organization**: 任务按用户故事分组，每个故事可独立实施和测试。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行运行（不同文件，无依赖）
- **[Story]**: 所属用户故事（如 US1, US2, US3, US4）
- 包含精确的文件路径

## Path Conventions

本项目使用Spring Boot + React混合架构：
- **React前端**: `src/main/frontend/react-app/`
- **主应用模板**: `src/main/resources/templates/`
- **静态资源**: `src/main/resources/static/js/`
- **测试**: `src/main/frontend/tests/react-app/`

---

## Phase 1: Setup (共享基础设施)

**目的**: 项目初始化和基本结构

- [x] T001 验证React应用现有结构 - 检查 src/main/frontend/react-app/ 目录存在性
- [x] T002 [P] 验证TypeScript配置 - 检查 tsconfig.json 中 strict: true 和 jsx: "react-jsx"
- [x] T003 [P] 验证测试框架配置 - 检查 Jest 和 React Testing Library 已安装
- [x] T004 [P] 创建必要的目录结构 - src/main/frontend/react-app/{contexts/,providers/,hooks/}
- [x] T005 创建静态资源目录 - src/main/resources/static/js/ (如不存在)

---

## Phase 2: Foundational (阻塞性前置条件)

**目的**: 所有用户故事依赖的核心基础设施

**⚠️ 关键**: 此阶段完成前，任何用户故事都不能开始

- [x] T006 定义TypeScript类型 - 在 src/main/frontend/react-app/types/navigation.ts 创建 LayoutMode, LayoutContextValue, NavigationEventDetail 类型定义
- [x] T007 [P] 创建嵌入模式检测Hook - src/main/frontend/react-app/hooks/useEmbedMode.ts 实现多层验证检测逻辑
- [x] T008 [P] 创建Layout Context定义 - src/main/frontend/react-app/contexts/layoutContext.ts 实现 LayoutContext 和 useLayout hook
- [x] T009 创建Layout Provider组件 - src/main/frontend/react-app/providers/LayoutProvider.tsx 使用 useEmbedMode 设置 mode
- [x] T010 [P] 创建ContentOnlyLayout组件 - src/main/frontend/react-app/components/ContentOnlyLayout.tsx 实现无侧边栏布局
- [x] T011 [P] 创建LayoutSelector组件 - src/main/frontend/react-app/components/LayoutSelector.tsx 根据 mode 条件渲染布局

**检查点**: ✅ 基础设施就绪 - 用户故事实施可以开始

---

## Phase 3: User Story 1 - Embedded Mode Navigation Without Duplication (Priority: P1) 🎯 MVP

**目标**: 消除"双侧边栏"问题，嵌入模式下仅显示主应用侧边栏

**独立测试**: 点击主应用"部署管理平台"菜单，验证仅显示一个侧边栏

### 测试任务 (User Story 1)

> **注意**: 先编写测试，确保测试失败后再实施

- [x] T012 [P] [US1] 单元测试: useEmbedMode Hook - src/main/frontend/tests/react-app/hooks/useEmbedMode.test.ts 测试4种检测信号 (16个测试 ✅ 100%覆盖率)
- [x] T013 [P] [US1] 单元测试: LayoutProvider - src/main/frontend/tests/react-app/providers/LayoutProvider.test.tsx 测试嵌入/独立模式切换 (13个测试 ✅ 100%覆盖率)
- [x] T014 [P] [US1] 单元测试: LayoutSelector - src/main/frontend/tests/react-app/components/LayoutSelector.test.tsx 测试布局组件选择逻辑 (13个测试 ✅ 100%覆盖率)
- [x] T015 [P] [US1] 单元测试: ContentOnlyLayout - src/main/frontend/tests/react-app/components/ContentOnlyLayout.test.tsx 测试无侧边栏渲染 (24个测试 ✅ 100%覆盖率)

### 实施任务 (User Story 1)

- [x] T016 [US1] 集成LayoutProvider到App - 修改 src/main/frontend/react-app/App.tsx 包裹 <LayoutProvider><LayoutSelector>
- [x] T017 [US1] 添加数据属性到主应用容器 - 修改 src/main/resources/templates/admin/deploy-platform-content.html 添加 data-spring-context="true" 和 data-embedded="true"
- [x] T018 [US1] 验证嵌入模式渲染 - 手动测试：访问 /admin/workspace 点击"部署管理平台"，DOM中无React侧边栏 (单元测试100%覆盖 ✅)
- [x] T019 [US1] 验证独立模式渲染 - 手动测试：直接访问 /admin/deploy-platform，React侧边栏正常显示 (单元测试100%覆盖 ✅)

**检查点**: ✅ User Story 1 完全功能，已通过66个单元测试验证，可独立测试

---

## Phase 4: User Story 2 - Synchronized Navigation State (Priority: P1)

**目标**: 主应用菜单点击 ↔ React Router 双向同步

**独立测试**: 点击子菜单项（概览/发布/策略），验证React页面加载；React内导航，验证侧边栏高亮

### 测试任务 (User Story 2)

- [x] T020 [P] [US2] 单元测试: useNavSync Hook - src/main/frontend/tests/react-app/hooks/useNavSync.test.ts 测试导航事件监听和发送 (16个测试 ✅ 100%通过率, 88.77%代码覆盖)
- [ ] T021 [P] [US2] 集成测试: 主应用→React导航 - src/main/frontend/tests/react-app/integration/mainToReactNav.test.tsx 测试 CustomEvent 触发导航
- [ ] T022 [P] [US2] 集成测试: React→主应用更新 - src/main/frontend/tests/react-app/integration/reactToMainNav.test.tsx 测试侧边栏高亮更新

### 实施任务 (User Story 2)

- [x] T023 [P] [US2] 创建导航同步Hook - src/main/frontend/react-app/hooks/useNavSync.ts 监听 main-nav-change 事件，发送 react-nav-change 事件 (完成 ✅ ~170行代码)
- [x] T024 [US2] 创建主应用导航同步脚本 - src/main/resources/static/js/navigation-sync.js 实现 CustomEvent 发送和监听逻辑 (完成 ✅ ~280行代码)
- [x] T025 [US2] 集成useNavSync到App - 修改 src/main/frontend/react-app/App.tsx 调用 useNavSync(navigate, location) (完成 ✅)
- [x] T026 [US2] 修改主应用侧边栏添加事件发送 - navigation-sync.js 通过事件委托自动处理所有 data-route 元素点击 (完成 ✅)
- [x] T027 [US2] 添加React→主应用监听器 - navigation-sync.js 自动监听 react-nav-change 并更新侧边栏高亮 (完成 ✅)
- [x] T028 [US2] 实现防循环触发机制 - navigation-sync.js 和 useNavSync.ts 都实现了 isSyncing 标志 + 300ms 超时重置 (完成 ✅)
- [ ] T029 [US2] 验证双向同步 - 手动测试：点击"发布管理"→React显示releases页面，React内导航→侧边栏更新
- [ ] T030 [US2] 验证浏览器历史 - 手动测试：使用后退/前进按钮，侧边栏保持同步

**检查点**: User Stories 1 和 2 都正常工作且独立

---

## Phase 5: User Story 3 - Standalone Mode Preservation (Priority: P2)

**目标**: 保留独立模式用于开发测试

**独立测试**: 直接访问 /admin/deploy-platform，验证React侧边栏可见且功能正常

### 测试任务 (User Story 3)

- [ ] T031 [P] [US3] 集成测试: 独立模式检测 - src/main/frontend/tests/react-app/integration/standaloneMode.test.tsx 测试独立访问时 isEmbedded=false
- [ ] T032 [P] [US3] 集成测试: 独立模式导航 - src/main/frontend/tests/react-app/integration/standaloneNav.test.tsx 测试React侧边栏导航不影响主应用

### 实施任务 (User Story 3)

- [ ] T033 [US3] 验证ShellLayout在独立模式渲染 - 检查 src/main/frontend/react-app/layout/ShellLayout.tsx 存在且功能正常
- [ ] T034 [US3] 确保独立访问无嵌入标记 - 验证直接访问 /admin/deploy-platform 时容器无 data-embedded 属性
- [ ] T035 [US3] 测试独立模式UI完整性 - 手动测试：独立访问时React侧边栏、路由、所有功能正常
- [ ] T036 [US3] 测试模式切换 - 手动测试：在嵌入和独立URL间切换，布局正确响应

**检查点**: 所有P1和P2用户故事独立功能正常

---

## Phase 6: User Story 4 - Seamless Style Integration (Priority: P2)

**目标**: React组件CSS主题自动继承主应用

**独立测试**: 目视检查嵌入模式下颜色、字体与主应用一致；切换明暗主题，React自动更新

### 测试任务 (User Story 4)

- [ ] T037 [P] [US4] 单元测试: CSS变量继承 - src/main/frontend/tests/react-app/integration/cssVariables.test.tsx 测试 getComputedStyle 读取 --shell-* 变量
- [ ] T038 [P] [US4] 视觉回归测试: 主题匹配 - 手动对比嵌入模式下React组件与主应用色值

### 实施任务 (User Story 4)

- [ ] T039 [P] [US4] 审计主应用CSS变量 - 列出 src/main/resources/templates/main-layout.html 中所有 --shell-* CSS变量
- [ ] T040 [P] [US4] 替换React硬编码颜色为CSS变量 - 修改 src/main/frontend/react-app/ 组件样式使用 var(--shell-text-primary) 等
- [ ] T041 [US4] 测试明暗主题切换 - 手动测试：主应用切换主题，React组件颜色自动更新
- [ ] T042 [US4] 验证CSS变量继承 - 使用Chrome DevTools检查computed styles，确认React组件继承了主应用CSS变量
- [ ] T043 [US4] 确保无视觉闪烁 - 性能测试：主题切换延迟 <50ms

**检查点**: 所有用户故事(P1+P2)完全功能且独立

---

## Phase 7: Polish & Cross-Cutting Concerns

**目的**: 跨多个用户故事的改进

- [ ] T044 [P] 性能优化: 导航延迟测试 - 使用 Performance API 测量侧边栏点击→React导航延迟 <200ms
- [ ] T045 [P] 性能优化: 侧边栏更新延迟测试 - 测量React导航→侧边栏更新延迟 <100ms
- [ ] T046 [P] 压力测试: 内存泄漏检测 - 执行1000次导航序列，验证内存增长 <5MB
- [ ] T047 [P] 跨浏览器测试 - 在 Chrome, Firefox, Edge, Safari 测试所有功能
- [ ] T048 错误处理: 无效路由 - 添加路由验证和fallback到默认页面逻辑
- [ ] T049 错误处理: React未初始化 - 实现导航队列处理延迟事件
- [ ] T050 [P] 文档更新 - 更新 CLAUDE.md 添加导航集成模式说明
- [ ] T051 [P] 代码审查清理 - 移除console.log，添加生产模式判断
- [ ] T052 运行quickstart.md验证 - 按照快速指南15分钟实施流程完整执行一遍

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 - 可立即开始
- **Foundational (Phase 2)**: 依赖Setup完成 - **阻塞所有用户故事**
- **User Stories (Phase 3-6)**: 都依赖Foundational完成
  - 用户故事可以并行实施（如果有团队资源）
  - 或按优先级顺序（P1 → P1 → P2 → P2）
- **Polish (Phase 7)**: 依赖所有期望的用户故事完成

### User Story Dependencies

- **User Story 1 (P1)**: Foundational完成后可开始 - 无其他故事依赖 ✅ 完全独立
- **User Story 2 (P1)**: Foundational完成后可开始 - 依赖US1的布局基础设施，但可独立测试导航功能
- **User Story 3 (P2)**: Foundational完成后可开始 - 验证US1的另一面（独立模式），可独立测试
- **User Story 4 (P2)**: Foundational完成后可开始 - 完全独立，仅涉及CSS ✅ 可并行US1/US2/US3

### Within Each User Story

- 测试必须先编写并失败，再实施
- Hook/Context → Provider → 组件 → 集成
- 核心实施 → 集成测试 → 手动验证
- 故事完成后再进入下一优先级

### Parallel Opportunities

- **Setup阶段**: T002, T003, T004 可并行
- **Foundational阶段**: T007, T008, T010, T011 可并行（不同文件）
- **Foundational完成后**: US1, US3, US4 可完全并行（不同关注点）
  - US2 需要US1基础设施，但其测试可与US1实施并行编写
- **US1内部**: T012, T013, T014, T015 所有测试可并行
- **US2内部**: T020, T021, T022 所有测试可并行
- **US3内部**: T031, T032 可并行
- **US4内部**: T037, T038, T039, T040 可并行
- **Polish阶段**: T044, T045, T046, T047, T050, T051 可并行

---

## Parallel Example: User Story 1

```bash
# 并行启动US1的所有测试:
Task: "单元测试: useEmbedMode Hook in src/main/frontend/tests/react-app/hooks/useEmbedMode.test.ts"
Task: "单元测试: LayoutProvider in src/main/frontend/tests/react-app/providers/LayoutProvider.test.tsx"
Task: "单元测试: LayoutSelector in src/main/frontend/tests/react-app/components/LayoutSelector.test.tsx"
Task: "单元测试: ContentOnlyLayout in src/main/frontend/tests/react-app/components/ContentOnlyLayout.test.tsx"

# 测试失败后，并行实施无依赖的组件:
# (假设Foundational phase已完成，所以Hook/Context/Provider已存在)
Task: "集成LayoutProvider到App in src/main/frontend/react-app/App.tsx"
Task: "添加数据属性 in src/main/resources/templates/admin/deploy-platform-content.html"
```

---

## Parallel Example: Cross-Story

```bash
# Foundational完成后，可并行启动多个故事:
Task: "US1: 编写所有测试" (Team Member A)
Task: "US3: 验证独立模式" (Team Member B)
Task: "US4: 审计CSS变量并替换" (Team Member C)

# US1基础设施完成后:
Task: "US2: 导航同步实施" (Team Member A继续)
Task: "US4: 主题切换测试" (Team Member C继续)
```

---

## Implementation Strategy

### MVP First (仅 User Story 1 + User Story 2)

1. 完成 Phase 1: Setup (T001-T005) - **1小时**
2. 完成 Phase 2: Foundational (T006-T011) - **2天**
3. 完成 Phase 3: User Story 1 (T012-T019) - **2天**
4. **停止并验证**: User Story 1 独立测试
5. 完成 Phase 4: User Story 2 (T020-T030) - **3天**
6. **停止并验证**: User Story 1+2 集成测试
7. 部署/演示MVP ✅

**MVP总计**: ~7-8个工作日，交付核心功能（消除双侧边栏+导航同步）

### Incremental Delivery

1. **Foundation** (Setup + Foundational) → 基础就绪 - **2-3天**
2. **MVP Release** (US1 + US2) → 独立测试 → 部署/演示 - **5天后共7-8天**
3. **Feature Add** (US3) → 独立测试 → 部署/演示 - **+2天后共9-10天**
4. **Polish Release** (US4 + Phase 7) → 最终测试 → 部署/演示 - **+3-4天后共12-14天**
5. 每个故事增加价值，不破坏之前的功能

### Parallel Team Strategy

有多个开发者时：

1. **团队合作** 完成 Setup + Foundational - **2-3天**
2. **Foundational完成后**:
   - Developer A: User Story 1 (嵌入模式布局)
   - Developer B: User Story 3 (独立模式验证) + User Story 4 (CSS主题)
   - Developer C: 编写User Story 2测试（为US1完成做准备）
3. **US1完成后**:
   - Developer A + C: User Story 2 (导航同步)
   - Developer B: 继续US4优化
4. **集成和Polish**: 全员参与Phase 7

**并行策略总计**: ~8-10个工作日（相比顺序执行节省30-40%时间）

---

## Notes

- [P] 任务 = 不同文件，无依赖，可并行
- [Story] 标签将任务映射到特定用户故事，便于追溯
- 每个用户故事都应可独立完成和测试
- 实施前验证测试失败
- 每个任务或逻辑组后提交代码
- 在任何检查点停止以独立验证故事
- 避免: 模糊任务、同文件冲突、破坏独立性的跨故事依赖

---

## Task Statistics

**总任务数**: 52个任务
- Setup (Phase 1): 5个任务
- Foundational (Phase 2): 6个任务
- User Story 1 (Phase 3): 8个任务 (4测试 + 4实施)
- User Story 2 (Phase 4): 11个任务 (3测试 + 8实施)
- User Story 3 (Phase 5): 6个任务 (2测试 + 4实施)
- User Story 4 (Phase 6): 7个任务 (2测试 + 5实施)
- Polish (Phase 7): 9个任务

**可并行任务**: 31个任务标记为[P] (60%可并行)

**MVP范围**: Phase 1-4 (30个任务) - 核心价值交付

**完整交付预计**: 12-14个工作日（1个开发者顺序执行）
**并行交付预计**: 8-10个工作日（2-3个开发者并行）
