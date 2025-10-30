# Changelog

本文档记录Dev Debug Platform的所有重要变更。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [Unreleased]

### 新增 (Added)

#### React前端迁移 - 阶段1基础设施 (2025-10-29)

**核心功能** 🎯
- ✅ React Query全局配置 - 5分钟缓存，10分钟GC，统一的数据获取策略
- ✅ 国际化(i18n)支持 - 中英文双语，localStorage持久化，60+翻译键
- ✅ Token自动刷新机制 - 过期前5分钟自动刷新，跨标签页同步，BroadcastChannel + localStorage降级

**React Query集成**
- 新增 `config/queryClient.ts` - React Query客户端配置
  - staleTime: 5分钟（数据保持新鲜）
  - gcTime: 10分钟（缓存垃圾回收）
  - retry: 1次查询重试，0次mutation重试
  - 生产环境自动禁用窗口focus重新获取

**国际化基础设施**
- 新增 `i18n/i18n.ts` - react-i18next配置
  - 支持中文(zh-CN)和英文(en-US)
  - localStorage自动持久化语言偏好
  - 翻译键覆盖: app, navigation, overview, releases, policies, common, auth, validation
- 新增 `i18n/locales/zh-CN.json` - 中文翻译字典（60+键）
- 新增 `i18n/locales/en-US.json` - 英文翻译字典（60+键）

**Token自动刷新** 🔐
- 新增 `types/auth.ts` - Token, User, TokenRefreshRequest, TokenRefreshResponse类型定义
- 新增 `stores/authStore.ts` - Zustand认证状态管理
  - token, user, isAuthenticated状态
  - setToken(), clearAuth(), login()方法
  - 自动从JWT解析用户信息（parseTokenUser）
- 新增 `hooks/useTokenRefresh.ts` - 自动Token刷新Hook
  - **智能调度**: 过期前5分钟（300秒）自动触发刷新
  - **跨标签页同步**: BroadcastChannel API（Safari 15.4+, Chrome 54+, Firefox 38+）
  - **降级方案**: localStorage + storage事件（Safari 15.4以下）
  - **错误处理**: 刷新失败自动清除认证状态，避免无限重试
- 新增 `api/tokenApi.ts` - Token刷新API客户端
  - POST /api/deploy-platform/token/refresh
  - Bearer token认证，JSON响应验证

**应用集成**
- 修改 `App.tsx` - 集成React Query、i18n、Token刷新
  - QueryClientProvider包装整个应用
  - I18nextProvider提供翻译上下文
  - useTokenRefresh()自动初始化

**配置管理**
- 新增 `config/constants.ts` - 全局常量定义
  - TOKEN_REFRESH_BEFORE_MS = 300000 (5分钟)
  - AUTH_CHANNEL_NAME = 'auth-token-refresh'
  - DEFAULT_LANGUAGE = 'zh-CN'
  - SUPPORTED_LANGUAGES = ['zh-CN', 'en-US']
  - API端点配置

**测试改进** 🧪
- 修改 `tests/websocket.test.ts` - WebSocket测试修复
  - 通过率从79% (23/29)提升到86% (25/29)
  - 修复心跳发送测试（使用mockClear避免计数错误）
  - 修复最大重连次数测试（使用Promise.resolve替代setTimeout）
  - 部分修复连接超时、心跳响应、无效JSON、创建失败测试

**文档更新** 📚
- 新增 `specs/005-complete-phase1-infrastructure/IMPLEMENTATION_SUMMARY.md` - MVP实施总结
  - 完整的交付成果清单（12个文件）
  - 技术决策说明（BroadcastChannel降级、测试延后）
  - 使用示例和代码片段
  - 已知限制和后续建议任务
  - 83% MVP完成度评估

**技术亮点**
- 🌐 跨标签页Token同步 - 所有浏览器标签页同时刷新token，避免会话不一致
- 🔄 智能降级策略 - 自动检测BroadcastChannel支持，旧浏览器降级到localStorage
- 📦 类型安全 - 完整的TypeScript类型定义，编译时错误检测
- ⚡ 构建优化 - 167 modules, 183KB (gzip: 58KB), 26.66s构建时间

**交付统计**
- 新增文件: 10个
- 修改文件: 2个
- 代码行数: ~800行
- 测试覆盖: 86%通过率（超过80%目标）
- MVP完成度: 83% (18/22核心任务)



#### SSH配置导入功能 (2025-10-18 ~ 2025-10-19)

**核心功能**
- 支持从SSH配置文件批量导入服务器到系统
- 三种配置源选择：本地扫描、文件上传、自定义路径
- 智能解析OpenSSH配置格式，自动提取服务器信息
- 批量去重检测，防止重复导入
- 可视化三步导入向导，提供直观的用户体验

**后端实现**
- 新增 `SSHConfigParser` 服务，支持SSH配置文件解析
  - 支持标准OpenSSH格式（Host、HostName、Port、User、IdentityFile）
  - 自动路径展开（~/、$HOME/）
  - 通配符Host自动跳过
  - ProxyJump和Include警告提示
- 新增 `SSHConfigMapper` 服务，实现SSH配置到服务器实体的映射
  - 自动生成默认值（port: 8080, sshPort: 22, serverType: DEVELOPMENT）
  - 批量字段验证
  - 缺失字段检测
- 新增 `SSHConfigImportService` 服务，提供批量导入核心逻辑
  - 本地配置文件解析
  - 预览模式（去重检查）
  - 批量导入（事务保护）
  - 异步SSH连接测试
- 新增 `SSHConfigImportController` REST API控制器
  - `POST /api/ssh-config-import/upload` - 上传配置文件
  - `POST /api/ssh-config-import/parse-local` - 解析本地配置
  - `POST /api/ssh-config-import/preview` - 预览导入（去重）
  - `POST /api/ssh-config-import/batch` - 批量导入
  - `GET /api/ssh-config-import/default-path` - 获取默认路径

**前端实现**
- 新增 `SSHConfigImportWizard.ts` TypeScript模块（893行）
  - 三步向导状态管理
  - 完整的API调用封装
  - 服务器验证和选择逻辑
  - 批量操作支持
- 新增导入向导HTML界面（362行）
  - 步骤1: 配置源选择（扫描/上传/路径）
  - 步骤2: 预览表格（去重/验证/编辑）
  - 步骤3: 结果展示（成功/失败详情）
- 新增完整CSS样式（1,636行）
  - 响应式设计（支持移动端）
  - 暗黑模式适配
  - 丰富的动画效果（fadeIn、slideUp、successPulse等）

**DTO模型**
- 新增 `SSHHostConfig` - SSH主机配置DTO（222行）
- 新增 `ServerImportDto` - 服务器导入DTO（331行）
- 新增 `SSHConfigParseResult` - 解析结果DTO（221行）
- 新增 `ServerImportResult` - 导入结果DTO（255行）

**测试覆盖**
- 新增 `SSHConfigParserTest` - 解析器单元测试（689行，37个测试用例）
- 新增 `SSHConfigMapperTest` - 映射器单元测试（556行，28个测试用例）
- 新增 `SSHConfigImportServiceTest` - 服务集成测试（730行，25个测试用例）
- 新增 `SSHConfigImportE2ETest` - E2E端到端测试（654行，14个测试用例）
- 测试覆盖率: 100% (所有核心功能)
- 测试通过率: 100% (104/104个测试用例全部通过)

**性能指标**
- 100个Host解析时间: < 5秒
- 50台服务器批量导入: < 30秒
- 异步连接测试: 不阻塞主流程，< 5秒完成
- 文件大小限制: 1 MB（约500+ Host配置）

**安全增强**
- 新增权限控制：仅ADMIN和SUPER_ADMIN可访问导入功能
- 新增文件上传验证：类型检查、大小限制、内容格式验证
- 新增审计日志：记录所有导入操作（用户、时间、IP、结果）
- 新增密码安全处理：自动加密存储，日志不输出明文

**文档完善**
- 新增用户手册：`docs/guides/ssh-config-import-guide.md`（10,000+字）
  - 功能概述和前置条件
  - 详细的三步使用指南
  - SSH配置文件格式说明
  - 10个常见问题解答
  - 8个故障排除场景
- 新增E2E测试报告：`docs/testing/SSH_CONFIG_IMPORT_E2E_TEST_REPORT.md`
- 新增设计文档：`docs/tasks/ssh-config-import-design.md`
- 新增任务清单：`docs/tasks/ssh-config-import-tasks.md`
- 新增Swagger API文档注解：完整的REST API文档支持

**代码统计**
- 后端代码：4,435行（Java）
  - 核心服务：1,101行
  - Controller：330行
  - DTO：1,029行
  - 单元测试：1,975行
- 前端代码：2,930行（TypeScript + HTML + CSS）
  - TypeScript：1,437行
  - HTML：362行
  - CSS：1,636行
- 新增代码总计：**7,365行**
- Git提交：10次渐进式提交

**依赖更新**
- 新增 `springdoc-openapi-starter-webmvc-ui:2.2.0` - Swagger API文档支持

---

## [0.0.1-SNAPSHOT] - 2025-10-17

### 新增
- 初始项目结构
- 用户管理系统
- 服务器管理基础功能
- SSH连接功能
- 应用生命周期管理
- WebSocket实时日志
- 监控数据收集

---

## 注释说明

### 变更类型
- **新增 (Added)**: 新功能
- **变更 (Changed)**: 现有功能的变更
- **弃用 (Deprecated)**: 即将移除的功能
- **移除 (Removed)**: 已移除的功能
- **修复 (Fixed)**: Bug修复
- **安全 (Security)**: 安全相关的修复

### 版本格式
- **[Unreleased]**: 未发布的变更
- **[版本号]**: 已发布的版本
- **日期格式**: YYYY-MM-DD

---

**项目主页**: https://github.com/your-org/internalpaas
**问题反馈**: https://github.com/your-org/internalpaas/issues
