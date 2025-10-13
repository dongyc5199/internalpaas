# 前端升级技术方案（方案一）

## 目标
- 在保留 Thymeleaf 服务端渲染的前提下，引入 Vite + TypeScript 等现代工程化能力。
- 将现有零散的 JavaScript 与 CSS 脚本模块化，提升可维护性与测试覆盖。
- 保证离线部署与 Spring Boot 构建流程兼容、稳定。

## 栈选型
- **构建工具**：Vite 5、Node.js 20+、npm。
- **语言**：TypeScript 输出 ES Module。
- **样式**：PostCSS + Autoprefixer，必要时引入 CSS Modules 管理组件样式。
- **规范**：ESLint（TypeScript + 浏览器规则）、Prettier、Stylelint。
- **测试**：Vitest 为单元测试基础，后续可扩展 Playwright 进行端到端验证。
- **集成方式**：Vite 产物输出至 `src/main/resources/static/dist` 并在 Thymeleaf 模板中引用。

## 架构原则
1. 新建 `src/main/frontend` 作为前端源码目录，建议结构：
   - `assets/` 静态资源（图标、字体等）
   - `styles/` 全局样式与设计令牌
   - `modules/` 功能脚本（示例：`server-modal`、`dashboard`）
   - `tests/` Vitest 测试用例
2. 配置 Vite 多入口，逐步替换模板中的旧 `<script>` 引用。
3. 通过 TypeScript 声明共享 DTO/接口，减小前后端数据差异。
4. 保留现有 DOM `data-*` 属性，确保升级过程兼容老页面。
5. 使用工厂函数或轻量依赖注入，避免全球变量污染。

## 第三方资源策略
- 优先使用 npm 安装第三方库（Chart.js、Flatpickr、Xterm.js 等），利用 Tree Shaking 降低体积。
- 对必须离线的库，可在 Vite `resolve.alias` 中指向 `static/vendor` 目录，兼容现有方案。
- 离线模式继续由 `OfflineDeploymentService` 控制，逐步适配 Vite 产物。

## 构建与部署流程
1. 本地开发：`npm run dev`（Vite HMR）与 `./mvnw.cmd spring-boot:run` 并行。
2. 生产构建：`npm run build` 输出静态资源至 `static/dist`，Spring Boot 直接提供。
3. Maven 集成：在 `pom.xml` 新增前端 profile，执行 `npm ci && npm run build`。
4. CI/流水线：增加前端阶段执行 `npm run lint && npm test && npm run build`，缓存 `node_modules` 加速。

## 推进策略
- 选取服务器模态框等模块做试点，验证 TypeScript + Vite 的集成。
- 将成功经验整理为迁移手册（入口、事件绑定、国际化等）。
- 工具链稳定后，按优先级迁移仪表盘、监控历史等大模块，每次迁移附带测试与文档。
