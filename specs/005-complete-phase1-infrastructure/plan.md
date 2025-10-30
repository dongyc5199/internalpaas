# Implementation Plan: 完成React前端迁移阶段1基础设施

**Branch**: `005-complete-phase1-infrastructure` | **Date**: 2025-10-29 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-complete-phase1-infrastructure/spec.md`

## Summary

本阶段完成React前端迁移阶段1的剩余20%基础设施工作，包括：
1. **修复测试失败** - 解决WebSocket测试6/29失败问题，确保CI流程畅通
2. **实现Token自动刷新机制** - 建立过期前5分钟自动刷新、BroadcastChannel跨标签页同步、重试和降级机制
3. **建立基础UI组件库** - 创建Button、Input、Modal、Table等核心组件，集成Storybook文档
4. **集成状态管理** - 配置React Query数据获取缓存和Zustand全局状态管理
5. **配置国际化支持** - 集成react-i18next，实现中英文切换和翻译字典加载

**技术方法**:
- 使用Vitest + fake-timers修复WebSocket时序测试
- 基于现有token-bridge扩展自动刷新逻辑，使用BroadcastChannel API跨标签页通信
- 在`src/main/frontend/react-app/components/`创建组件库，使用CSS Modules隔离样式
- 在App.tsx中配置QueryClientProvider和i18n Provider
- 创建示例hooks（useReleases）和stores（authStore）作为开发参考

## Technical Context

**Language/Version**: TypeScript 5.9.3, React 19.2.0, Node.js 18+
**Primary Dependencies**:
- 测试框架: Vitest 1.6.1, @testing-library/react 16.3.0, Playwright 1.56.0
- 状态管理: @tanstack/react-query 5.90.5, zustand 5.0.8
- 国际化: react-i18next 16.2.1, i18next 25.6.0
- UI文档: storybook 8.x (待安装)
- 构建工具: Vite 5.4.20, @originjs/vite-plugin-federation 1.4.1

**Storage**:
- 内存: Token、认证状态
- localStorage: 语言偏好
- BroadcastChannel: 跨标签页Token同步

**Testing**:
- 单元测试: Vitest + @testing-library/react
- E2E测试: Playwright
- 组件测试: Storybook Interactions (可选)

**Target Platform**: 现代浏览器 (Chrome 90+, Firefox 88+, Safari 14+)

**Project Type**: Web应用 - 微前端子应用（React）嵌入主应用（Thymeleaf）

**Performance Goals**:
- Token刷新响应时间 < 500ms
- 组件首次渲染 < 100ms
- Storybook构建时间 < 30秒
- 测试套件运行时间 < 2分钟

**Constraints**:
- 测试失败率 < 1%
- 核心模块测试覆盖率 ≥ 80%
- UI组件必须与主应用Bootstrap样式兼容（通过CSS Modules隔离）
- BroadcastChannel不支持旧浏览器需提供降级方案

**Scale/Scope**:
- 组件库: 5个核心组件（Button, Input, Select, Modal, Table）
- 测试用例: 修复6个失败的WebSocket测试
- 状态管理: 2个示例hooks + 1个全局store
- 国际化: 2种语言（zh-CN, en-US），约50-100个翻译键

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### 项目Constitution原则

由于Constitution文件为模板，基于项目实际情况和前端最佳实践，本功能遵循以下原则：

#### I. 测试优先（NON-NEGOTIABLE）
- ✅ **合规**: 所有修复的WebSocket测试必须先运行失败，然后修复通过
- ✅ **合规**: 新增的Token刷新、组件库功能必须有对应的单元测试
- ✅ **合规**: Storybook stories作为组件的"可视化测试"

#### II. 模块化与复用
- ✅ **合规**: UI组件库独立于页面，可被任何React页面引用
- ✅ **合规**: Token刷新逻辑封装在`useTokenRefresh` hook中
- ✅ **合规**: 状态管理（React Query/Zustand）提供统一的数据获取和状态访问模式

#### III. 渐进增强
- ✅ **合规**: 组件库从5个核心组件开始，后续按需扩展
- ✅ **合规**: 国际化先支持中英文，未来可添加更多语言
- ✅ **合规**: BroadcastChannel提供降级方案（localStorage + storage事件）

#### IV. 文档化
- ✅ **合规**: 每个组件在Storybook中至少有2-3个使用示例
- ✅ **合规**: hooks和stores提供JSDoc注释
- ✅ **合规**: 更新《前端迁移React技术方案与实施计划.md》的进度章节

#### V. 性能与质量
- ✅ **合规**: 测试覆盖率目标80%（核心模块）
- ✅ **合规**: CI环境测试失败率<1%
- ✅ **合规**: 组件使用React.memo优化渲染（按需）

### 复杂度评估

**无Constitution违规需要豁免**。本功能完全符合前端工程最佳实践：
- 测试覆盖充分
- 模块化设计
- 性能目标明确
- 文档齐全

## Project Structure

### Documentation (this feature)

```text
specs/005-complete-phase1-infrastructure/
├── plan.md              # 本文件 (/speckit.plan 输出)
├── spec.md              # 功能规格说明
├── research.md          # Phase 0 技术研究
├── data-model.md        # Phase 1 数据模型（Token、AuthState、TranslationDictionary）
├── quickstart.md        # Phase 1 快速开始指南
├── contracts/           # Phase 1 API契约
│   ├── token-refresh-api.yaml          # Token刷新接口OpenAPI规范
│   └── component-props-schema.json     # UI组件Props类型定义
└── checklists/
    └── requirements.md  # 规格质量检查清单（已完成）
```

### Source Code (repository root)

```text
# 现有结构（保持不变）
src/main/frontend/
├── main.ts                              # 主入口（加载模块联邦）
├── modules/                             # 现有模块（原生TS）
│   ├── deploy-platform-auth.ts          # 现有: Token桥接
│   └── ...
├── mfe/
│   └── deploy-platform-loader.ts        # 现有: 微前端加载器
├── utils/                               # 现有工具库
│   ├── http.ts                          # HTTP客户端
│   ├── event-bus.ts                     # 事件总线
│   └── ...
└── tests/                               # 现有测试
    ├── websocket.test.ts                # 需修复的WebSocket测试
    ├── modules/deploy-platform-auth.test.ts
    └── ...

# 新增/修改结构
src/main/frontend/react-app/
├── App.tsx                              # [修改] 添加QueryClientProvider、i18n Provider
├── main.tsx                             # [修改] 初始化i18n
├── components/                          # [新增] UI组件库
│   ├── Button/
│   │   ├── Button.tsx
│   │   ├── Button.module.css
│   │   ├── Button.stories.tsx           # Storybook story
│   │   └── Button.test.tsx
│   ├── Input/
│   │   ├── Input.tsx
│   │   ├── Input.module.css
│   │   ├── Input.stories.tsx
│   │   └── Input.test.tsx
│   ├── Select/
│   │   └── ...
│   ├── Modal/
│   │   └── ...
│   ├── Table/
│   │   └── ...
│   └── index.ts                         # 统一导出
├── hooks/                               # [新增] 自定义Hooks
│   ├── useTokenRefresh.ts               # Token自动刷新hook
│   ├── useReleases.ts                   # 示例: 发布列表数据获取
│   └── useDeploymentSummary.ts          # 现有hook（已存在）
├── stores/                              # [新增] Zustand状态管理
│   └── authStore.ts                     # 认证状态store
├── i18n/                                # [新增] 国际化配置
│   ├── i18n.ts                          # i18next初始化
│   ├── locales/
│   │   ├── zh-CN.json                   # 中文翻译
│   │   └── en-US.json                   # 英文翻译
│   └── types.ts                         # i18n类型定义
├── config/                              # [新增] 配置文件
│   ├── queryClient.ts                   # React Query配置
│   └── constants.ts                     # 常量（如Token刷新时间）
└── tests/                               # [新增] React组件测试
    ├── components/
    │   ├── Button.test.tsx
    │   └── ...
    └── hooks/
        └── useTokenRefresh.test.tsx

# Storybook配置（新增）
.storybook/
├── main.ts                              # Storybook主配置
├── preview.ts                           # 全局装饰器和参数
└── manager.ts                           # Storybook管理器配置
```

**Structure Decision**:
- 采用**Web应用 + 微前端**结构
- React子应用（`src/main/frontend/react-app/`）通过模块联邦嵌入主应用
- UI组件库采用**按功能模块组织**（每个组件独立目录）
- 测试文件与源文件同目录（`*.test.tsx`）或集中在`tests/`
- Storybook独立配置目录（`.storybook/`）

## Complexity Tracking

**无需填写** - 本功能无Constitution违规需要豁免。

---

## Phase 0: Outline & Research

### 研究任务

基于Technical Context中的技术栈和依赖，需要研究以下主题：

1. **WebSocket测试时序问题最佳实践**
   - 研究Vitest fake-timers的正确使用方法
   - 调查时序竞争问题的常见解决方案
   - 参考其他项目的WebSocket测试案例

2. **Token自动刷新实现模式**
   - 研究基于setTimeout的刷新调度机制
   - 调查BroadcastChannel API的兼容性和降级方案
   - 参考JWT refresh token最佳实践

3. **CSS Modules与主应用样式隔离**
   - 研究CSS Modules在Vite中的配置
   - 调查如何与现有Bootstrap样式共存
   - 评估scoped styles vs. BEM命名约定

4. **Storybook 8.x与React 19集成**
   - 研究Storybook 8最新配置方法
   - 调查与Vite Module Federation的兼容性
   - 参考React 19 + Storybook的示例项目

5. **React Query与Zustand集成模式**
   - 研究React Query的最佳缓存策略
   - 调查Zustand与React Query的配合使用
   - 评估是否需要Redux Toolkit（当前不需要）

### 研究输出

详细研究结果见 [research.md](./research.md)（由Phase 0生成）

---

## Phase 1: Design & Contracts

### 数据模型

详细数据模型设计见 [data-model.md](./data-model.md)（由Phase 1生成），包括：

#### 核心实体

1. **Token（认证令牌）**
   - `accessToken: string` - JWT字符串
   - `expiresAt: number` - 过期时间戳（毫秒）
   - `issuer: string` - 签发者（默认"internalpaas"）
   - `audience: string` - 受众（默认"deploy-platform"）

2. **AuthState（认证状态 - Zustand Store）**
   - `token: Token | null` - 当前token
   - `user: {username: string, role: string} | null` - 用户信息
   - `isAuthenticated: boolean` - 是否已认证
   - `setToken: (token: Token) => void` - 更新token
   - `clearAuth: () => void` - 清除认证状态

3. **UIComponentProps（UI组件属性）**
   - 通用Props: `variant`, `size`, `disabled`, `loading`, `className`
   - Button特有: `onClick`, `type`
   - Input特有: `value`, `onChange`, `error`, `placeholder`
   - Modal特有: `isOpen`, `onClose`, `title`, `children`
   - Table特有: `data`, `columns`, `onSort`

4. **TranslationDictionary（翻译字典）**
   - 嵌套JSON对象
   - 键路径: `common.buttons.submit`, `errors.network.timeout`
   - 语言: `zh-CN`, `en-US`

### API契约

详细API契约见 `contracts/` 目录（由Phase 1生成）：

#### 1. Token刷新接口 (`contracts/token-refresh-api.yaml`)

```yaml
openapi: 3.1.0
info:
  title: Token Refresh API
  version: 1.0.0
paths:
  /api/deploy-platform/token/refresh:
    post:
      summary: 刷新访问令牌
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                currentToken:
                  type: string
                  description: 当前token或session ID
      responses:
        '200':
          description: 刷新成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  accessToken:
                    type: string
                  expiresAt:
                    type: integer
                  issuer:
                    type: string
                  audience:
                    type: string
        '401':
          description: 会话已过期
        '500':
          description: 服务器错误
```

#### 2. BroadcastChannel消息契约

```typescript
// 跨标签页消息类型
type TokenRefreshMessage = {
  type: 'token-refresh';
  payload: {
    accessToken: string;
    expiresAt: number;
  };
};

type LogoutMessage = {
  type: 'logout';
  payload: {};
};

type BroadcastMessage = TokenRefreshMessage | LogoutMessage;
```

### 快速开始指南

详细指南见 [quickstart.md](./quickstart.md)（由Phase 1生成），包含：
- 环境准备（Node.js、npm、依赖安装）
- 运行测试（`npm run test:run`）
- 启动Storybook（`npm run storybook`）
- 使用组件库示例代码
- 使用状态管理示例代码
- 语言切换示例代码

---

## Phase 2: Implementation Tasks

**注意**: 任务清单（tasks.md）由 `/speckit.tasks` 命令生成，不属于本计划范围。

预期任务分类（仅供参考）：

### P1任务（阻塞性）

1. **测试修复**
   - 修复WebSocket连接超时测试
   - 修复WebSocket重连次数测试
   - 修复心跳发送测试
   - 修复心跳响应测试
   - 修复JSON解析错误测试
   - 修复WebSocket创建失败测试

2. **Token自动刷新**
   - 实现`useTokenRefresh` hook
   - 实现BroadcastChannel同步逻辑
   - 后端实现`/api/deploy-platform/token/refresh`接口
   - 添加Token刷新单元测试
   - 添加BroadcastChannel集成测试

### P2任务（高价值）

3. **UI组件库**
   - 创建Button组件+样式+story+测试
   - 创建Input组件+样式+story+测试
   - 创建Select组件+样式+story+测试
   - 创建Modal组件+样式+story+测试
   - 创建Table组件+样式+story+测试
   - 配置Storybook 8.x

4. **状态管理**
   - 配置React Query QueryClient
   - 创建authStore（Zustand）
   - 实现useReleases示例hook
   - 修改App.tsx添加Providers
   - 添加状态管理单元测试

### P3任务（增强）

5. **国际化**
   - 配置react-i18next
   - 创建中文翻译文件
   - 创建英文翻译文件
   - 实现语言切换组件
   - 添加i18n测试

---

## 验收标准

基于规格说明的成功标准（SC-001至SC-006），Phase 1完成后应满足：

1. ✅ **测试稳定性**:
   - 运行`npm run test:run`，所有测试通过
   - CI环境连续10次构建无测试失败
   - 核心模块覆盖率≥80%

2. ✅ **Token自动刷新**:
   - 用户在页面停留30分钟以上，无token过期导致的操作失败
   - 多标签页同步接收新token
   - 刷新失败后正确提示并登出

3. ✅ **UI组件库**:
   - 5个核心组件可用
   - Storybook可访问（`npm run storybook`）
   - 每个组件至少2-3个story

4. ✅ **状态管理**:
   - React Query缓存生效（检查Network面板，相同请求不重复发起）
   - authStore可被任意组件访问
   - 数据获取代码量减少（对比手动管理loading/error）

5. ✅ **国际化**:
   - 支持中英文切换
   - 翻译覆盖率≥80%（核心UI文本）
   - 缺失翻译键显示键名而不是报错

---

## 风险与缓解

| 风险 | 缓解措施 | 责任人 |
|------|---------|--------|
| WebSocket测试修复耗时超预期 | 先修复P1关键测试，非关键测试可暂时skip | 测试工程师 |
| 后端Token刷新接口延期 | 前端先实现逻辑并使用mock接口 | 前端开发者 |
| Storybook配置与依赖冲突 | 隔离Storybook配置，使用独立版本 | 前端开发者 |
| BroadcastChannel旧浏览器不支持 | 提供localStorage降级方案 | 前端开发者 |

---

## 估算工作量

基于规格说明的估算（21-27人日），按任务分解：

| 任务类别 | 估算人日 | 负责人 |
|---------|---------|--------|
| P1: 测试修复 | 2-3 | 开发者A |
| P1: Token刷新（前端） | 3-4 | 开发者A |
| P1: Token刷新（后端） | 2-3 | 后端开发者 |
| P2: UI组件库 | 4-5 | 开发者B |
| P2: 状态管理 | 4-5 | 开发者C |
| P3: 国际化 | 3-5 | 开发者C |
| **总计** | **18-25** | 3人团队 |

**建议并行开发**:
- 开发者A: P1任务（测试+Token，关键路径）
- 开发者B: P2 UI组件库（独立任务）
- 开发者C: P2状态管理 + P3国际化（相关任务）

---

## 交付物清单

- [x] 本文件 (plan.md)
- [ ] research.md（Phase 0研究文档）
- [ ] data-model.md（Phase 1数据模型）
- [ ] contracts/token-refresh-api.yaml
- [ ] contracts/component-props-schema.json
- [ ] quickstart.md（Phase 1快速开始指南）
- [ ] tasks.md（由`/speckit.tasks`生成，非本计划范围）

---

## 下一步

1. **Phase 0**: 运行研究任务，生成`research.md`
2. **Phase 1**: 根据研究结果生成数据模型、契约和快速开始指南
3. **Phase 2**: 运行`/speckit.tasks`命令生成详细任务清单
4. **实施**: 按优先级（P1→P2→P3）执行任务
5. **验收**: 运行测试套件、验证功能、更新进度文档

---

**计划创建时间**: 2025-10-29
**预计开始时间**: 待确认
**预计完成时间**: 开始后2-3周
