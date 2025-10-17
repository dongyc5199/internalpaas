# 项目文档中心

**欢迎来到 Internal PaaS Platform 文档中心!**

本文档中心提供项目的完整文档,包括用户指南、架构设计、开发文档等。

---

## 📚 目录结构

```
docs/
├── README.md (本文件 - 文档导航中心)
├── guides/ (用户指南)
├── architecture/ (架构设计)
├── design/ (功能设计)
│   ├── admin-dashboard/ (管理后台)
│   ├── server-management/ (服务器管理)
│   ├── agent-deployment/ (Agent部署)
│   ├── ui-system/ (UI系统)
│   └── prototypes/ (原型)
├── technical/ (技术文档)
├── development/ (开发文档)
└── archives/ (历史文档归档)
    ├── 2024-phase4/ (阶段4历史文档)
    ├── frontend-upgrade/ (前端升级历史文档)
    ├── legacy-reports/ (旧报告归档)
    └── troubleshooting/ (故障排查历史)
```

---

## 🚀 快速开始

### 用户指南

| 文档 | 描述 | 适用对象 |
|-----|------|---------|
| [快速开始指南](guides/quick-start.md) | 5分钟快速部署和运行 | 新用户 |
| [部署指南](guides/deployment-guide.md) | 完整的部署说明和配置 | 运维人员 |
| [运维手册](guides/operations-guide.md) | 日常运维操作和故障排查 | 运维人员 |
| [离线部署指南](guides/offline-deployment-guide.md) | 离线环境部署方案 | 特殊环境用户 |
| [Metrics Hub集成指南](guides/metrics-hub-integration-guide.md) | Hub模块集成完整指南 | 开发人员 |
| [Hub快速集成](guides/hub-quick-start-integration.md) | Hub模块快速集成 | 开发人员 |

### E2E测试指南

| 文档 | 描述 |
|-----|------|
| [E2E快速开始](guides/e2e-quickstart.md) | E2E测试快速入门 |
| [E2E运行指南](guides/e2e-running-guide.md) | E2E测试详细操作 |
| [E2E故障排查](guides/e2e-troubleshooting.md) | E2E测试常见问题 |

---

## 📖 完整文档导航

### 1. 用户指南 (guides/)
面向用户的快速开始、部署、运维文档
- [快速开始](guides/quick-start.md)
- [部署指南](guides/deployment-guide.md)
- [运维手册](guides/operations-guide.md)
- [离线部署](guides/offline-deployment-guide.md)
- [E2E测试快速开始](guides/e2e-quickstart.md)
- [E2E测试运行指南](guides/e2e-running-guide.md)
- [E2E故障排查](guides/e2e-troubleshooting.md)
- [Metrics Hub集成指南](guides/metrics-hub-integration-guide.md)
- [Hub快速集成](guides/hub-quick-start-integration.md)
- [Hub集成快速开始](guides/hub-integration-quick-start.md)

### 2. 架构设计 (architecture/)
系统架构和设计文档
- [项目概览](architecture/overview.md)
- [前端架构](architecture/frontend-architecture.md)
- [Agent自动部署架构](architecture/agent-auto-deployment-architecture.md)

### 3. 功能设计 (design/)
详细的功能设计文档

#### 3.1 管理后台 (design/admin-dashboard/)
- [API设计规范](design/admin-dashboard/api-design-specification.md)
- [重新设计计划](design/admin-dashboard/redesign-plan.md)
- [统计卡片设计](design/admin-dashboard/stat-cards-design.md)

#### 3.2 服务器管理 (design/server-management/)
- [详情页设计](design/server-management/detail-page-design.md)
- [详情集成计划](design/server-management/detail-integration-plan.md)
- [状态标签设计](design/server-management/status-tags-design.md)
- [用户账号同步](design/server-management/user-account-sync.md)

#### 3.3 Agent部署 (design/agent-deployment/)
- [测试指南](design/agent-deployment/testing-guide.md)
- [工作区UI重新设计](design/agent-deployment/workspace-ui-redesign.md)

#### 3.4 UI系统 (design/ui-system/)
- [登录页设计](design/ui-system/login-page-design.md)
- [图标资源规范](design/ui-system/icon-resource-specification.md)
- [内容页框架](design/ui-system/content-page-framework.md)
- [框架使用示例](design/ui-system/content-framework-usage-example.md)

#### 3.5 原型 (design/prototypes/)
- HTML原型和演示页面

### 4. 技术文档 (technical/)
技术实现和最佳实践

#### 4.1 数据库相关
- [数据库策略优化](technical/database-strategy-optimization.md)
- [H2数据库分析优化](technical/h2-database-analysis-optimization.md)
- [H2数据库说明](technical/h2-database-clarification.md)
- [数据库优化清单](technical/database-optimization-checklist.md)
- [Phase4数据库优化验证](technical/phase4-database-optimization-validation.md)

#### 4.2 性能优化
- [性能优化指南](technical/performance-optimization.md)
- [N+1查询优化验证](technical/n1-query-optimization-verification.md)

#### 4.3 代码优化
- [代码优化报告](technical/code-optimization-report.md)
- [代码审查优化建议](technical/code-review-optimization-recommendations.md)

### 5. 开发文档 (development/)
开发指南和工具
- [开发路线图](development/roadmap.md)
- [Lombok IDE配置](development/lombok-ide-setup.md)
- [前端开发总结指南](development/frontend-dev-summary-guide.md)

### 6. 历史归档 (archives/)
已完成阶段的历史文档

#### 6.1 阶段4文档 (2024-phase4/)
- [阶段4工作总结](archives/2024-phase4/阶段4-工作总结-2025-10-17.md)
- 14个阶段4相关文档

#### 6.2 前端升级文档 (frontend-upgrade/)
- [TASK4完成报告](archives/frontend-upgrade/TASK4_COMPLETION_REPORT.md)
- 24个前端升级任务文档

#### 6.3 历史报告 (legacy-reports/)
- Agent部署报告 (2个)
- Hub集成报告 (2个)
- UI实现报告
- 集成分析报告 (4个)
- 滚动问题修复报告 (3个)
- 修复报告 (2个)
- 文档管理分析
- 旧doc目录README

#### 6.4 故障排查历史 (troubleshooting/)
- 历史故障排查指南

---

## 🔍 按主题查找

### 部署相关
- [快速开始](guides/quick-start.md) - 最快部署方式
- [部署指南](guides/deployment-guide.md) - 完整部署流程
- [离线部署](guides/offline-deployment-guide.md) - 离线环境方案

### 运维相关
- [运维手册](guides/operations-guide.md) - 日常运维
- [E2E故障排查](guides/e2e-troubleshooting.md) - 问题诊断

### 开发相关
- [前端架构](architecture/frontend-architecture.md) - 架构设计
- [开发路线图](development/roadmap.md) - 项目规划
- [Lombok IDE配置](development/lombok-ide-setup.md) - 工具配置
- [前端开发指南](development/frontend-dev-summary-guide.md) - 开发总结

### Hub模块相关
- [Metrics Hub集成指南](guides/metrics-hub-integration-guide.md) - 完整集成
- [Hub快速集成](guides/hub-quick-start-integration.md) - 快速开始
- [Hub模块安全指南](../hub/docs/Security.md) - 安全加固
- [Hub模块性能优化](../hub/docs/Perf.md) - 性能调优

### 技术实现
- [数据库策略优化](technical/database-strategy-optimization.md)
- [性能优化指南](technical/performance-optimization.md)
- [代码优化报告](technical/code-optimization-report.md)

---

## 📝 文档状态

| 状态 | 说明 | 文档数量 |
|-----|------|---------|
| ✅ 已整理 | 已迁移到docs/各子目录 | 50+个 |
| 📦 已归档 | 历史文档已归档 | 50+个 |
| 📍 位置稳定 | Hub/issues目录 | ~20个 |
| **总计** | **docs/目录** | **100个** |

---

## 🎯 文档规范

### 命名规范
- 使用英文命名,kebab-case格式
- 有意义的前缀: `guide-`, `design-`, `report-`
- 示例: `guide-quick-start.md`, `report-phase4-completion.md`

### 目录组织
- **guides/** - 用户指南
- **architecture/** - 架构设计
- **design/** - 功能设计
- **technical/** - 技术文档
- **development/** - 开发文档
- **archives/** - 历史归档

---

## 📅 更新记录

| 日期 | 变更说明 |
|------|---------|
| 2025-10-17 | 完整重组doc/目录,迁移50+个文档到分类目录 |
| 2025-10-17 | 创建文档中心,迁移用户指南,归档历史文档 |

---

## 💡 贡献指南

**添加新文档:**
1. 确定文档类型(指南/设计/技术)
2. 放入合适目录
3. 更新本索引文档
4. 遵循命名规范

**更新文档:**
- 保持文档时效性
- 更新相关链接
- 记录变更历史

---

**维护者:** 项目团队  
**最后更新:** 2025-10-17  
**下次审查:** 2025-11-17

---

**返回:** [项目主页](../README.md)
