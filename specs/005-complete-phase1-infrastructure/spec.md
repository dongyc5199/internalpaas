# Feature Specification: 完成React前端迁移阶段1基础设施

**Feature Branch**: `005-complete-phase1-infrastructure`
**Created**: 2025-10-29
**Status**: Draft
**Input**: User description: "完成阶段1的剩余20%工作量。"

## 背景

根据《前端迁移React技术方案与实施计划》，阶段1（基础设施）已完成约80%，包括：
- ✅ Vite + Module Federation 构建配置
- ✅ React 子项目结构与路由
- ✅ BFF Token 服务与安全机制
- ✅ 微前端加载器

剩余20%的关键基础设施缺失正在阻碍阶段2（业务功能开发）的推进。本规格涵盖：
1. 修复测试失败（WebSocket测试6/29失败）
2. 实现Token自动刷新机制
3. 建立基础UI组件库
4. 集成状态管理（React Query + Zustand）
5. 配置国际化支持

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 开发者运行测试套件并获得稳定结果 (Priority: P1)

作为前端开发者，我需要能够运行完整的测试套件并获得稳定的通过结果，这样我才能自信地提交代码和进行持续集成。

**Why this priority**: 测试失败会阻塞CI流程，导致无法合并代码。修复测试是启动任何新开发工作的前提条件。

**Independent Test**: 运行 `npm run test:run` 命令，所有测试用例应通过，覆盖率报告应生成无误。

**Acceptance Scenarios**:

1. **Given** 开发者在项目根目录，**When** 执行 `npm run test:run`，**Then** 所有测试用例通过，无失败或超时
2. **Given** WebSocket相关测试运行，**When** 测试连接管理、重连、心跳机制，**Then** 所有断言正确，无时序问题
3. **Given** 测试完成后，**When** 查看覆盖率报告，**Then** 核心模块（utils, mfe, modules）覆盖率达到80%以上

---

### User Story 2 - 前端应用自动刷新即将过期的认证令牌 (Priority: P1)

作为React子应用用户，当我长时间使用部署管理界面时，系统应在后台自动刷新即将过期的认证令牌，让我无需重新登录即可持续工作。

**Why this priority**: Token过期会导致API调用失败，破坏用户体验。这是微前端架构中的关键安全机制。

**Independent Test**: 用户停留在部署平台页面超过25分钟（假设token有效期30分钟），在第25分钟时系统应自动刷新token，用户的所有操作继续正常工作。

**Acceptance Scenarios**:

1. **Given** 用户已登录部署管理平台且token将在5分钟后过期，**When** 到达刷新时间点，**Then** 系统自动调用BFF刷新接口并更新内存中的token
2. **Given** Token刷新成功，**When** 多个浏览器标签页同时打开部署平台，**Then** 所有标签页通过BroadcastChannel同步接收新token
3. **Given** Token刷新失败（网络错误或会话过期），**When** 检测到刷新失败，**Then** 显示友好的"会话已过期，请重新登录"提示，并清除所有认证状态
4. **Given** 用户主动登出，**When** 登出事件广播，**Then** 所有标签页的React应用清理token并跳转到登录页

---

### User Story 3 - 开发者使用统一的UI组件库快速构建页面 (Priority: P2)

作为前端开发者，我需要一套预制的、符合设计规范的UI组件（按钮、表单、模态框、表格），这样我可以快速构建一致的用户界面，而无需每次都从头编写基础组件。

**Why this priority**: UI组件库是提升开发效率和保证界面一致性的基础，但可以在业务功能开发中逐步完善。

**Independent Test**: 开发者导入 `@internalpaas/ui` 组件库，使用 `<Button>`、`<Input>`、`<Modal>`、`<Table>` 组件构建一个简单的表单页面，所有组件按设计规范渲染且交互正常。

**Acceptance Scenarios**:

1. **Given** 开发者需要创建一个表单，**When** 导入 `Button`、`Input` 组件并传入必要props，**Then** 组件按主题样式渲染，支持禁用、加载、错误等状态
2. **Given** 开发者需要展示数据列表，**When** 使用 `Table` 组件并传入数据和列配置，**Then** 表格正确渲染，支持排序、分页（可选）
3. **Given** 开发者需要显示弹窗，**When** 使用 `Modal` 组件并控制显示状态，**Then** 模态框正确覆盖背景层，支持关闭、确认操作
4. **Given** 组件库已安装，**When** 开发者运行 Storybook（`npm run storybook`），**Then** 可以查看所有组件的交互式文档和示例

---

### User Story 4 - 应用使用状态管理获取和缓存服务器端数据 (Priority: P2)

作为React应用开发者，我需要使用统一的数据获取机制（React Query）来管理API调用、缓存和加载状态，这样我可以避免手动处理loading/error状态，提升代码质量。

**Why this priority**: 状态管理是构建复杂交互的基础，但可以在组件开发中逐步接入。

**Independent Test**: 开发者创建一个使用 `useQuery` 的组件（如 `useReleases` hook），该组件能正确显示加载状态、数据和错误信息，且数据会被自动缓存。

**Acceptance Scenarios**:

1. **Given** 开发者需要获取发布列表，**When** 使用 `useReleases()` hook，**Then** hook返回 `{data, isLoading, error}` 状态，组件根据状态渲染UI
2. **Given** API返回成功，**When** 数据被React Query缓存，**Then** 后续相同请求直接从缓存读取，不发起网络请求
3. **Given** 需要全局状态（如用户认证信息），**When** 使用Zustand store（`useAuthStore`），**Then** 所有组件可访问和更新认证状态
4. **Given** Token更新，**When** 在authStore中更新token，**Then** 所有使用该store的组件自动响应更新

---

### User Story 5 - 用户切换界面语言查看本地化内容 (Priority: P3)

作为部署平台用户，我希望能够在中英文之间切换界面语言，这样我可以使用我熟悉的语言进行操作。

**Why this priority**: 国际化是长期需求，但当前用户主要使用中文，可以先提供基础支持，后续扩展。

**Independent Test**: 用户点击语言切换按钮，界面上的所有文本（导航、按钮、提示信息）立即切换为选定语言。

**Acceptance Scenarios**:

1. **Given** React应用已加载，**When** 检测到用户的浏览器语言偏好，**Then** 自动加载对应语言包（默认中文）
2. **Given** 用户点击语言切换控件，**When** 选择"English"，**Then** 所有UI文本切换为英文，偏好保存到localStorage
3. **Given** 语言包缺失某些翻译键，**When** 尝试渲染该键，**Then** 显示键名而不是报错，并在控制台警告
4. **Given** 与主应用共享翻译字典，**When** 主应用更新翻译文件，**Then** React子应用通过HTTP backend自动加载最新翻译

---

### Edge Cases

- **测试环境不稳定**：WebSocket测试在CI环境中可能因网络延迟失败 → 增加合理的超时时间和重试机制
- **Token刷新时网络中断**：刷新请求失败时应重试最多3次，超过限制后提示用户重新登录
- **多标签页Token同步冲突**：一个标签页刷新token时，另一个标签页正在使用旧token → BroadcastChannel应立即广播，所有标签页在收到新token前的请求使用旧token，之后使用新token
- **组件库样式冲突**：UI组件与主应用的Bootstrap样式冲突 → 使用CSS Modules严格隔离作用域，或使用BEM命名约定加前缀
- **语言切换时表单数据丢失**：切换语言不应清空用户正在填写的表单 → 语言切换仅更新UI文本，不影响应用状态
- **Storybook与主应用依赖版本不一致**：Storybook可能需要特定版本的React → 使用与主应用相同的React版本，确保peerDependencies正确配置

## Requirements *(mandatory)*

### Functional Requirements

#### 测试稳定性（P1）

- **FR-001**: 测试套件必须在本地和CI环境中稳定通过，失败率低于1%
- **FR-002**: WebSocket测试必须正确模拟连接、断连、重连、心跳等场景，无时序竞争问题
- **FR-003**: 测试覆盖率报告必须自动生成，核心模块覆盖率达到80%以上
- **FR-004**: 每个测试用例必须具有清晰的描述和断言，失败时输出有意义的错误信息

#### Token自动刷新（P1）

- **FR-005**: 系统必须在token过期前5分钟自动触发刷新请求
- **FR-006**: Token刷新必须调用BFF接口 `/api/deploy-platform/token/refresh`（需后端实现），传入当前token或session ID
- **FR-007**: Token刷新成功后必须更新内存中的token和过期时间，并通过BroadcastChannel通知所有标签页
- **FR-008**: Token刷新失败后必须重试最多3次，间隔5秒，超过限制后触发登出流程
- **FR-009**: 用户主动登出时必须广播登出事件，所有标签页清除认证状态并跳转到登录页
- **FR-010**: Token相关事件（刷新成功、刷新失败、登出）必须记录到前端日志（console.info/warn）

#### UI组件库（P2）

- **FR-011**: 组件库必须提供至少以下核心组件：Button、Input、Select、Modal、Table
- **FR-012**: 所有组件必须支持主题样式（通过CSS变量或设计tokens），与主应用保持一致
- **FR-013**: 组件必须支持常见状态：默认、悬停、激活、禁用、加载、错误
- **FR-014**: 组件必须通过props控制行为和样式，避免硬编码
- **FR-015**: 组件库必须集成Storybook，每个组件至少提供2-3个使用示例
- **FR-016**: 组件必须使用CSS Modules或scoped样式，避免全局样式污染

#### 状态管理（P2）

- **FR-017**: React应用必须集成React Query，提供QueryClient配置（缓存时间、重试策略等）
- **FR-018**: 必须提供至少2个示例数据获取hook（如 `useReleases`、`useDeploymentSummary`）
- **FR-019**: React应用必须集成Zustand，提供认证状态store（authStore），包含token、用户信息、登录/登出方法
- **FR-020**: 所有API调用必须自动注入当前有效的token到Authorization header
- **FR-021**: API调用失败（401 Unauthorized）时必须触发token刷新或登出流程

#### 国际化（P3）

- **FR-022**: React应用必须集成react-i18next，初始化i18n实例并配置中英文语言包
- **FR-023**: 系统必须提供语言切换功能，用户选择后保存偏好到localStorage
- **FR-024**: 翻译键缺失时必须显示键名并在控制台警告，不应导致应用崩溃
- **FR-025**: React应用必须能够从主应用的翻译字典路径（如 `/translations/{{lng}}.json`）加载翻译

### Key Entities

- **Token（认证令牌）**:
  - 属性：accessToken（JWT字符串）、expiresAt（过期时间戳）、issuer（签发者）、audience（受众）
  - 生命周期：由BFF签发 → 存储在内存 → 过期前自动刷新 → 登出时清除

- **AuthState（认证状态）**:
  - 属性：token（Token对象）、user（用户信息：username、role）、isAuthenticated（是否已认证）
  - 管理方式：存储在Zustand store，所有组件可访问

- **UIComponent（UI组件）**:
  - 通用属性：variant（变体）、size（尺寸）、disabled（禁用）、loading（加载中）
  - 专用属性：Button（onClick、type）、Input（value、onChange、error）、Modal（isOpen、onClose）

- **TranslationDictionary（翻译字典）**:
  - 结构：嵌套JSON对象，键路径如 `common.buttons.submit`
  - 语言：zh-CN（中文）、en-US（英文）

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 测试套件在CI环境中稳定通过，连续10次构建无测试失败
- **SC-002**: Token自动刷新机制投入使用后，用户长时间操作（超过30分钟）不再因token过期导致操作失败
- **SC-003**: 开发者使用UI组件库构建新页面的速度提升50%（对比从零编写基础组件）
- **SC-004**: 使用React Query后，数据获取相关的代码量减少30%（对比手动管理loading/error状态）
- **SC-005**: 国际化功能上线后，支持中英文切换，翻译覆盖率达到80%以上（核心UI文本）
- **SC-006**: Storybook文档站点可访问，所有核心组件有完整的使用示例

## Assumptions *(mandatory)*

1. **后端支持**：假设后端团队会实现Token刷新接口 `/api/deploy-platform/token/refresh`，该接口接受当前session或refreshToken，返回新的accessToken
2. **浏览器兼容性**：假设目标用户使用现代浏览器（Chrome 90+、Firefox 88+、Safari 14+），支持BroadcastChannel API
3. **设计规范**：假设存在统一的设计规范文档（颜色、字体、间距），UI组件库将遵循该规范
4. **翻译资源**：假设主应用已有部分中英文翻译文件，React子应用可以复用或扩展
5. **开发环境**：假设开发者本地已安装Node.js 18+、npm 9+，且项目依赖已正确安装
6. **测试环境**：假设CI环境（如GitHub Actions、Jenkins）支持运行Vitest和Playwright测试

## Dependencies *(mandatory)*

### Internal Dependencies
- **主应用（main-layout.html）**: Token桥接通信、BroadcastChannel事件监听
- **BFF服务（Spring Boot后端）**: Token刷新接口、认证状态管理
- **设计团队**: 提供UI组件设计规范和Figma原型（如有）
- **翻译团队**: 提供中英文翻译文件或协助翻译（如有专职团队）

### External Dependencies
- **npm包**: react-query@5.x、zustand@5.x、react-i18next@16.x、storybook@8.x
- **开发工具**: Vitest（测试运行器）、Playwright（E2E测试）、ESLint（代码检查）
- **浏览器API**: BroadcastChannel（跨标签页通信）、localStorage（持久化偏好设置）

## Out of Scope

以下内容**不属于**本阶段范围，将在后续阶段处理：

- ❌ **完整的部署管理业务功能**（发布向导、灰度监控、审计列表）→ 阶段2
- ❌ **服务器管理、监控仪表盘的React重写** → 阶段3
- ❌ **性能优化**（代码拆包、懒加载、bundle分析）→ 阶段3-4
- ❌ **完整的E2E测试覆盖**（端到端业务流程测试）→ 阶段2-3
- ❌ **生产环境部署和灰度发布** → 阶段4
- ❌ **高级UI组件**（DatePicker、Chart、Tree等）→ 按需在阶段2-3中添加
- ❌ **响应式布局优化**（移动端适配）→ 阶段3
- ❌ **无障碍访问（A11y）支持** → 阶段3-4
- ❌ **主题切换功能**（深色模式）→ 阶段3

## Risks

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 测试修复耗时超预期（WebSocket时序问题复杂） | 阻塞CI流程，延迟其他开发 | 先修复关键测试（P1），非关键测试可暂时skip，后续修复 |
| 后端Token刷新接口延期 | Token自动刷新功能无法完整实现 | 前端先实现客户端逻辑和mock接口，后端就绪后对接 |
| UI组件库设计规范不明确 | 组件样式不统一，需返工 | 先实现基础样式，与设计团队同步确认后迭代 |
| BroadcastChannel API在旧浏览器不支持 | 多标签页Token同步失败 | 提供polyfill或降级方案（localStorage + storage事件） |
| Storybook配置与主应用依赖冲突 | 无法启动Storybook | 隔离Storybook配置，使用独立的依赖版本 |
| 翻译资源不足或翻译质量差 | 国际化功能体验不佳 | 优先覆盖核心UI文本，使用机器翻译 + 人工校对 |

## Open Questions

当前无需用户澄清的问题。所有需求已基于现有技术方案和行业最佳实践做出合理假设。

## Notes

- 本规格基于《前端迁移React技术方案与实施计划.md》和项目现状分析报告
- 优先级（P1/P2/P3）已根据阻塞性和业务价值分配
- 估算工作量：P1任务约10-12人日，P2任务约8-10人日，P3任务约3-5人日，总计约21-27人日
- 建议3人团队并行开发：1人负责测试修复和Token机制，1人负责UI组件库，1人负责状态管理和国际化
- 所有功能完成后，应进行集成测试并更新《前端迁移React技术方案与实施计划.md》的进度章节
