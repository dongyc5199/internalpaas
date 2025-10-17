# 任务拆分与进度表

> 说明：每当任何任务完成时，必须立即更新下表的“状态”和“备注”栏，保持进度实时准确。

| 序号 | 任务主题           | 主要工作内容                                                                                                                                   | 负责人 | 状态   | 备注 |
|-----:|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|:------:|:------|:-----|
| 1    | 环境与工具配置     | 安装统一版本的 Node.js；初始化 npm 项目；引入 Vite、TypeScript、ESLint、Prettier、Stylelint、Vitest、PostCSS，并编写 `tsconfig`、ESLint、Prettier、Stylelint 配置。 | Codex | 已完成 | 2025-10-10 完成：安装前端依赖并提交配置文件（tsconfig、ESLint、Prettier、Stylelint、PostCSS）。 |
| 2    | 项目结构与构建集成 | 新建 `src/main/frontend` 目录；配置 Vite 输出到 `static/dist`；在 `package.json` 增加 `dev`/`build`/`lint`/`test` 脚本；在 `pom.xml` 中挂接 npm 构建流程。         | Codex | 已完成 | 2025-10-10 完成：创建前端目录与入口文件、编写 Vite 配置、更新 npm 脚本并添加基础测试；在 `pom.xml` 新增 `frontend` Profile 执行 `npm ci && npm run build`。 |
| 3    | 试点模块迁移       | 将 `server-modal` 模块迁移到 TypeScript；更新模板引用；为关键逻辑编写 Vitest 测试，确保功能与降级方案可用。                                               | Codex | 已完成 | 2025-10-10 完成：完成 server-modal 模块迁移(28KB TypeScript代码)、模板脚本替换(`main-layout.html`)、Vitest 单测补充(4个测试用例全部通过)，代码覆盖率79.75%。测试报告：4 passed (4), Coverage: server-modal.ts 79.75% stmts, 44.18% branch, 65% funcs。 |
| 4    | 公共能力与样式整合 | 迁移设计令牌与基础样式到新管线；抽象 HTTP、事件总线、存储等通用工具；统一第三方库加载方式及离线兼容策略。                                                   | Codex | 已完成 | 2025-10-10 完成：创建设计令牌系统(4个CSS文件,含颜色/字体/间距/阴影),抽象3个工具模块(http.ts 226行/event-bus.ts 112行/storage.ts 156行),编写前端开发文档,验证构建产物(main.css 4.59KB)。所有产物通过Vite构建,支持离线部署。 |
| 5    | 批量模块迁移       | 按优先级逐步迁移仪表盘、监控历史、配置编辑器、主题等模块；为每个模块执行重构、测试、模板替换与旧代码清理。                                                     | Codex | 已完成 | 2025-10-10 完成：迁移theme.ts(150行)和dashboard.ts(260行),补充19个测试用例全部通过。覆盖率：theme.ts 91.55%, dashboard.ts 82.32%。总计迁移3个核心模块(含server-modal),984行TypeScript代码,23个测试用例100%通过。构建产物：main.js 278.56KB, main.css 4.59KB。 |
| 6    | 文档与培训         | 更新项目文档、常见问题、最佳实践；组织分享或结对编程，提升团队对 TypeScript/Vite 的掌握；明确代码与提交规范。                                               |  待定  | 未开始 |     |
| 7    | 质量保证           | 在 CI 中执行 `npm run lint && npm test && npm run build` 以及 `./mvnw.cmd clean verify`；定义验收标准；监控 bundle 体积并优化构建策略。                 |  待定  | 未开始 |     |
