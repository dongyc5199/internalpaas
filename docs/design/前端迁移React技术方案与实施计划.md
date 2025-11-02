# 前端迁移 React 技术方案与实施计划

## 1. 背景与目标
- 现有主应用前端基于 Thymeleaf + 原生 TypeScript/DOM 脚本，随着部署管理等复杂模块引入，单页交互、状态管理与组件复用面临维护瓶颈。
- 目标是在保持主应用导航、鉴权、审计、运维一体化的前提下，引入 React 微前端架构，支撑复杂交互、模块化开发与长期可扩展性。
- 最终形态：主应用作为壳体，React 子应用（部署管理、监控、运维等）通过模块联邦或 iframe 嵌入；后端由 BFF 管理 token，确保安全可控。

## 2. 现状评估
- **技术栈**：Vite 构建、纯 TS/DOM 文件 `src/main/frontend/modules/*`、全局事件总线与工具函数；CSS 依赖 Bootstrap 与自定义样式。
- **会话与安全**：Spring Security 提供 Session + CSRF Meta；未使用前端框架的 token/状态管理。
- **问题摘要**
  - 复杂交互（多步骤向导、实时看板）使用原生 DOM 操作难以维护。
  - 组件复用、单元测试、主题切换缺乏统一规范。
  - 引入第三方 UI/图表库成本高，缺少组件封装层。
  - 跨团队协作与代码治理难度增长。

### 2.1 现有前端资产梳理
- **模块脚本**（`src/main/frontend/modules`）：
  - `server-group-management.ts` / `ServerListManager.ts` / `ServerDetailOverlay.ts`：管理服务器列表、详情和图表，依赖 Chart.js、事件总线、全局提示函数。
  - `server-modal.ts`、`SSHConfigImportWizard.ts`：模态框与导入向导，使用原生 DOM + FormData 处理复杂表单。
  - `dashboard.ts`、`theme.ts`、`tab-version-methods.ts`：主页统计、主题切换、Tab 行为等增强脚本。
- **工具库**（`src/main/frontend/utils`）：
  - `event-bus.ts` 简单发布订阅；`http.ts` 封装 fetch + CSRF 头；`format.ts`、`chart.ts`、`notification.ts` 提供格式化、图表配置、消息提示等。
  - `i18n.ts` 读取全局 `t()`；`storage.ts`、`websocket.ts` 管理本地存储与实时连接。
- **样式**（`src/main/frontend/styles`）：
  - `main.css`、`ssh-config-import-wizard-v2.css` 等传统全局样式；`styles/tokens` 目录维护颜色、间距、排版变量。
- **模板注入**：
  - Thymeleaf 模板（如 `main-layout.html`、`applications.html`）负责引入 `/dist/assets/main.js` 并渲染 `_csrf` 元数据，页面内含大量 `data-*` 钩子与内联 JSON。
- **构建与测试**：
  - 使用 Vite + TypeScript；测试脚本基于 Vitest、Playwright，但覆盖度低，多数页面缺少自动化用例。

## 3. 目标架构
- **宿主层（主应用）**
  - 保持 Thymeleaf 输出基础布局、导航、角色校验。
  - 在指定区域（如管理员控制台）挂载 React 微前端，使用 module federation 或 iframe 引入。
  - 通过 BFF 管理 Access/Refresh Token，与前端子应用以短期 token 交互。
- **React 子应用**
  - React 18 + TypeScript，使用 React Router、React Query（或 Redux Toolkit）管理路由/数据。
  - CSS Modules + 主题变量，部分页面可采用 Tailwind 或 Emotion（与宿主约定命名空间）。
  - 公共组件库（Button、Modal、Table、Form、Chart）沉淀在 `@internalpaas/ui`。
  - 集成 Storybook、Jest/React Testing Library、Playwright。
- **通信与安全**
  - 主应用通过 postMessage + nonce 或共享 store 传递短期 token/BFF 会话信息，BroadcastChannel 同步续期/登出。
  - 前端仅持有内存 token，关键操作需回调主应用确认。
  - 宿主提供统一的主题、i18n、路由同步接口。

## 4. 技术选型
- **构建工具**：Vite 5（主应用继续使用），React 子应用使用 Vite + module federation 插件（`@originjs/vite-plugin-federation`），保持与现有打包体系一致。
- **状态管理**：React Query（数据获取）、Zustand/Redux（全局状态）、Context（鉴权、主题）。
- **样式**：CSS Modules + PostCSS；复用主应用 design tokens；支持 Tailwind（可选）。
- **表单**：React Hook Form + Zod 校验。
- **图表**：Recharts 或 Chart.js React 封装。
- **国际化**：react-i18next，与主应用字典同步。
- **测试**：React Testing Library、Vitest/Jest、Playwright；CI 中执行 `npm run test`, `npm run lint`, `npm run build`, `npm run test:e2e`。

### 4.1 关键依赖版本建议
- `react` / `react-dom`: 18.3.x（与 Suspense/Concurrent 特性兼容）。
- `react-router-dom`: 6.28.x（支持数据路由与懒加载）。
- `@tanstack/react-query`: 5.x（统一数据获取与缓存）。
- `zustand`: 4.x（轻量全局状态管理，交给复杂场景再接入 Redux Toolkit）。
- `react-hook-form`: 7.x + `@hookform/resolvers` 搭配 `zod` 3.x。
- `react-i18next`: 13.x；结合 `i18next-http-backend` 读取主应用字典。
- `@originjs/vite-plugin-federation`: 1.5.x；远程模块暴露和依赖共享。
- `vitest`: 1.6.x、`@testing-library/react`: 14.x、`playwright`: 1.56.x 与现有脚手架一致。
- `storybook`: 8.x（后续建设 UI Catalog 时接入）。

## 5. 微前端整合策略
- **模块联邦优先**：主应用作为 host，React 子应用作为 remote。保证构建产物在 `/dist/react/` 目录，宿主按需加载，支持缓存 busting。
- **iframe 备选**：若模块联邦受限，可在短期内使用 iframe，后续迁移至模块联邦；需处理跨窗口通信与样式隔离。
- **共享库**：React、React DOM、design tokens、API SDK 在 host 中暴露，remote 避免重复打包。
- **路由同步**：主应用导航跳转时通过事件通知 React Router；React 子应用路由变化发送历史记录事件给宿主，保持浏览器地址一致性。
- **降级策略**：未加载或加载失败时回退到旧页面或显示提示，支持 feature flag 控制。
### 5.1 实施进展
- `vite.config.ts` 已集成 `@originjs/vite-plugin-federation`，暴露 `./App` 与 `./bootstrap` 供宿主按需加载；构建产物包含 `deploy-platform-remote.js`。
- 调试页面 `/debug/react-mfe` 注入模块联邦脚本并模拟 token 回传，便于在本地验证 React 子应用挂载与通信流程。
- 后续计划：主应用 host 配置 `remotes["deploy-platform"]`，通过 `await import("deploy-platform/bootstrap")` 完成懒加载；生产环境结合 manifest 控制缓存。

## 6. Token 与安全方案
- **BFF 模式**：主应用后端持有 Refresh Token，前端子应用通过短期 Access Token（<1h）；BFF 提供 `/api/auth/token` 刷新接口，前端只持 session ID/nonce。
- **token 传递流程**
  1. 主应用登录成功后，在内存中存储 Access Token 与过期时间。
  2. 加载 React 子应用时，通过 postMessage（包含 issuer、audience、nonce、过期时间、签名）发送 token；子应用校验 `origin === expectedOrigin` 且 nonce 匹配后方可写入内存。调试页使用内嵌脚本模拟该流程，便于端到端验证。
  3. token 将过期前 5 分钟，宿主广播刷新事件，BFF 交换新 token 并下发；子应用收到后更新内存。
  4. 登出时宿主广播 logout，子应用清理状态并跳转登录页。
- **CSRF/权限**：React API 调用统一走 BFF Proxy，BFF 注入 CSRF 头并进行“应用 × 环境 × 动作”校验。
- **超时与异常处理**：React 子应用需监听 `token-error` 事件（如刷新失败、签名校验不通过），立即切换到只读或登录提示；所有 token 事件均写入审计日志。

## 7. 实施计划
### 阶段 0：准备（第 1–2 周）
- 完成方案评审、列出可复用模块与风险点。
- 建立迭代看板，确定人力安排与时间窗口。

### 阶段 1：基础设施（第 3–4 周）
- 创建 React 子项目，配置 Vite + module federation。
- 打通 BFF token 接口、postMessage 通道、BroadcastChannel 同步。
- 初始化 UI 组件库、主题和 API SDK。
- 建置测试/CI 与代码规范（ESLint/Prettier/Husky）。

### 阶段 2：部署管理模块迁移（第 5–9 周）
- 按功能拆分（发布向导、灰度监控、策略配置、审计列表、数据作业）逐步重写为 React 组件。
- 与后端联调，完成 API、Socket/Webhook、Prometheus 数据展示。
- 编写单元测试与端到端测试，接入 CI。
- 在灰度环境与旧页面 A/B 对比，收集反馈。

### 阶段 3：扩展与优化（第 10–13 周）
- 迁移其他复杂模块或新增 React 页面（服务器管理、监控仪表盘等）。
- 完善 Storybook/UI Catalog，推广组件库使用。
- 更新开发指南、培训文档、脚手架脚本。
- 建立性能监控与错误上报。

### 阶段 4：收尾（第 14 周以后）
- 清理遗留 DOM 脚本、提供兼容层或迁移完毕后删除。
- 完善 BFF token 运维（吊销、日志、告警）。
- 评估是否进一步 SPA 化或迁移其他模块。

## 8. 工作量估算
- 阶段 0–1：约 10–15 人日（方案、脚手架、BFF 接入）。
- 阶段 2：约 40–50 人日（按功能模块并行开发）。
- 阶段 3：约 20–25 人日（扩展、组件库、培训）。
- 阶段 4：约 10–15 人日（清理、优化、监控）。
- 总体约 80–105 人日，可由 3–4 人团队 2–3 个月内完成（含灰度与优化）。

## 9. 风险与对策
- **React 经验不足**：安排培训、Pair Programming、引入 code review checklist。
- **与主应用冲突**：严格使用 CSS Modules/命名空间，建立共享 design tokens，逐步替换旧样式。
- **token 安全**：postMessage 加 nonce 校验，BFF 端记录日志并设限联络；遇异常立即触发 logout。
- **性能问题**：监控 bundle 体积与加载时间（Lighthouse、Webpack Bundle Analyzer），按需拆包/懒加载。
- **双轨维护成本**：迁移期间保留旧页面，通过 feature flag 控制入口；设定 sunset 时间表。

## 10. 成功指标
- 部署管理模块 React 版本上线后，用户操作平均响应时间下降/保持不变，错误率可控。
- 新增功能开发效率提升（需求到上线周期减少 20% 以上）。
- 前端代码覆盖率、Lint 通过率、性能指标达标；无重大安全事件。
- 开发团队对 React 栈熟练度提高，并形成可复用的组件与脚手架。

## 11. 后续工作
- 与主应用团队对齐 token/BFF 接口协议与审计要求。
- 设立迁移周会与看板，跟踪阶段成果与风险。
- 灰度上线前准备回滚方案，确保在紧急情况下可快速退回旧页面。
- 持续收集用户反馈，优化 UX 与性能，迭代组件库。

### Host Loader Notes
- Main shell now loads the remote via `src/main/frontend/mfe/deploy-platform-loader.ts`, which dynamically imports `deploy-platform/bootstrap` and invokes the exported `mount` method once the host placeholder exists.
- Loader exposes test hooks (`__setRemoteLoaderForTest`, `__resetDeployPlatformLoader`) ensuring Vitest coverage for remote integration logic.
- Legacy `main.ts` triggers the loader after DOM ready so existing Thymeleaf pages can opt-in by rendering `<div id="deploy-platform-root"></div>`.

- 宿主脚本 `src/main/frontend/modules/deploy-platform-auth.ts` 监听 `deploy-platform-auth-request`，通过 BFF 接口（默认 `/api/deploy-platform/token`，可由 `#deploy-platform-root[data-auth-endpoint]` 指定）获取短期访问令牌并回传 `deploy-platform-auth` 消息；失败时返回 `deploy-platform-auth-error` 供前端降级。
