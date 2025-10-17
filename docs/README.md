# 项目文档中心

**欢迎来到 Internal PaaS Platform 文档中心!**

本文档中心提供项目的完整文档,包括用户指南、架构设计、开发文档等。

---

## 📚 目录结构

```
docs/
├── README.md (本文件 - 文档导航中心)
├── guides/ (用户指南)
└── archives/ (历史文档归档)
    ├── 2024-phase4/ (阶段4历史文档)
    └── frontend-upgrade/ (前端升级历史文档)
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
- [E2E测试](guides/e2e-quickstart.md)

### 2. 架构设计 (暂存于 /doc)
系统架构和设计文档
- [项目概览](../doc/project-overview.md)
- [前端架构](../doc/frontend-architecture.md)
- [性能优化](../doc/performance-optimization.md)

### 3. 技术文档 (暂存于 /doc 和 /hub/docs)
技术实现和最佳实践
- [数据库策略优化](../doc/轻量级平台数据库策略优化方案.md)
- [Hub模块安全指南](../hub/docs/Security.md)
- [Hub模块性能优化](../hub/docs/Perf.md)

### 4. Hub模块文档 (/hub)
Metrics Hub模块相关文档
- [Hub模块文档](../hub/README.md)
- [任务规范](../issues/) (T1-T8任务)

### 5. 历史归档 (archives/)
已完成阶段的历史文档

#### 5.1 阶段4文档 (2024-phase4/)
- [阶段4工作总结](archives/2024-phase4/阶段4-工作总结-2025-10-17.md)
- 13个阶段4相关文档

#### 5.2 前端升级文档 (frontend-upgrade/)
- [TASK4完成报告](archives/frontend-upgrade/TASK4_COMPLETION_REPORT.md)
- 24个前端升级任务文档

---

## 🔍 按主题查找

### 部署相关
- [快速开始](guides/quick-start.md) - 最快部署方式
- [部署指南](guides/deployment-guide.md) - 完整部署流程
- [离线部署](guides/offline-deployment-guide.md) - 离线环境方案

### 运维相关
- [运维手册](guides/operations-guide.md) - 日常运维
- [故障排查](guides/e2e-troubleshooting.md) - 问题诊断

### 开发相关
- [前端架构](../doc/frontend-architecture.md)
- [开发路线图](../doc/development-roadmap.md)
- [Lombok IDE配置](../doc/lombok-ide-setup.md)

### Hub模块相关
- [Hub集成指南](../doc/METRICS_HUB_INTEGRATION_GUIDE.md)
- [安全加固](../hub/docs/Security.md)
- [性能优化](../hub/docs/Perf.md)

---

## 📝 文档状态

| 状态 | 说明 | 文档数量 |
|-----|------|---------|
| ✅ 已整理 | 已迁移到docs/guides/ | 7个 |
| 📦 已归档 | 历史文档已归档 | 37个 |
| ⏳ 待整理 | 仍在原doc/目录 | ~50个 |
| 📍 位置稳定 | Hub/issues目录 | ~20个 |

---

## 🎯 文档规范

### 命名规范
- 使用英文命名,kebab-case格式
- 有意义的前缀: `guide-`, `design-`, `report-`
- 示例: `guide-quick-start.md`, `report-phase4-completion.md`

### 目录组织
- **guides/** - 用户指南
- **archives/** - 历史归档
- **/doc** - 临时位置(逐步迁移中)
- **/hub/docs** - Hub模块技术文档

---

## 📅 更新记录

| 日期 | 变更说明 |
|------|---------|
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
