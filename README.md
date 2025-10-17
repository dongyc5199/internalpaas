# Internal PaaS Platform

**轻量级内部 PaaS 平台 - 企业级服务器管理与监控解决方案**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Internal-blue.svg)]()

---

## 🎯 项目简介

Internal PaaS Platform 是一个轻量级的企业内部 PaaS 管理平台,提供:
- 🖥️ **服务器管理** - 统一的服务器资源管理和监控
- 📊 **Metrics Hub** - 企业级指标收集、聚合与查询
- 🤖 **Agent自动部署** - 自动化的监控Agent部署
- 📈 **实时监控** - 服务器状态实时监控和告警
- 🎨 **现代化UI** - 基于Thymeleaf + TypeScript的响应式界面

---

## 📚 文档导航

### 快速开始
- **[5分钟快速开始](docs/guides/quick-start.md)** ⭐ - 最快部署方式
- [完整部署指南](docs/guides/deployment-guide.md) - 详细部署流程
- [运维手册](docs/guides/operations-guide.md) - 日常运维操作

### 技术文档
- [项目架构概览](docs/architecture/overview.md)
- [前端架构设计](docs/architecture/frontend-architecture.md)
- [数据库策略优化](doc/轻量级平台数据库策略优化方案.md)

### Hub模块
- [Metrics Hub集成指南](docs/guides/metrics-hub-integration-guide.md)
- [安全加固文档](hub/docs/Security.md) - TLS/mTLS, JWT/JWKS
- [性能优化文档](hub/docs/Perf.md) - 负载测试与调优

### 开发文档
- [AI Agent协作指南](AGENTS.md)
- [开发路线图](docs/development/roadmap.md)
- [Lombok IDE配置](docs/development/lombok-ide-setup.md)

### 完整文档索引
📖 **[文档中心](docs/README.md)** - 浏览所有文档

---

## 🚀 快速开始

### 前置要求
- Java 17+
- Maven 3.8+
- 浏览器支持: Chrome/Firefox/Edge (最新版)

### 启动应用

```bash
# 开发模式(默认H2数据库)
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

# 离线模式(无需网络)
./mvnw.cmd -Poffline spring-boot:run

# 生产模式(PostgreSQL)
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod
```

访问应用: http://localhost:8080

**默认登录:**
- 用户名: `admin`
- 密码: `admin123`

---

## 📦 主要功能

### 1. 服务器管理
- ✅ 服务器注册与分组管理
- ✅ 实时状态监控
- ✅ SSH连接管理
- ✅ 批量操作支持

### 2. Metrics Hub (指标中心)
- ✅ OTLP协议接收器 (T1)
- ✅ 数据标准化与验证 (T2)
- ✅ 双写管道 (ClickHouse + H2/PostgreSQL) (T3)
- ✅ 统一查询API (T4)
- ✅ 数据聚合与保留策略 (T5)
- ✅ 安全加固 (TLS/mTLS, JWT) (T7)
- ✅ 性能优化 (负载测试工具) (T8)
- 🔄 Agent自动部署 (T6, 进行中)

### 3. 监控与告警
- ✅ 实时指标采集
- ✅ 自定义告警规则
- ✅ 多渠道通知

### 4. 现代化UI
- ✅ 响应式设计
- ✅ 深色模式支持
- ✅ 流畅的用户体验
- ✅ TypeScript模块化架构

---

## 🏗️ 技术栈

### 后端
- **框架:** Spring Boot 3.x, Spring MVC, Spring Data JPA
- **数据库:** H2 (dev/offline), PostgreSQL (production), ClickHouse (时序数据)
- **安全:** Spring Security, JWT/JWKS, TLS/mTLS
- **监控:** Micrometer, OpenTelemetry (OTLP)
- **构建工具:** Maven 3.8+

### 前端
- **模板引擎:** Thymeleaf
- **脚本语言:** TypeScript
- **构建工具:** Vite
- **样式:** CSS3 (现代化设计系统)
- **测试:** Playwright (E2E)

### DevOps
- **容器化:** Docker (可选)
- **CI/CD:** GitHub Actions (规划中)
- **版本控制:** Git

---

## 📊 项目结构

```
E:\work\code\internalpaas\
├── src/
│   ├── main/
│   │   ├── java/com/cmict/internalpaas/  # Java源码
│   │   ├── frontend/                      # TypeScript前端
│   │   └── resources/
│   │       ├── templates/                 # Thymeleaf模板
│   │       ├── static/                    # 静态资源
│   │       └── db/migration/              # 数据库迁移
│   └── test/                              # 单元测试
├── hub/                                   # Metrics Hub模块
├── docs/                                  # 📚 文档中心
│   ├── guides/                            # 用户指南
│   └── archives/                          # 历史归档
├── doc/                                   # 设计与技术文档
├── issues/                                # Hub任务规范
├── e2e/                                   # E2E测试
├── scripts/                               # 运维脚本
└── data/                                  # 示例数据
```

---

## 🔧 开发指南

### 构建项目

```bash
# 清理并编译
./mvnw.cmd clean compile

# 运行测试
./mvnw.cmd test

# 打包
./mvnw.cmd package
```

### 数据库模式

**开发环境 (默认):**
- 使用H2内存数据库
- 自动建表,无需配置
- 数据在重启后清空

**离线模式:**
- 使用H2文件数据库
- 数据持久化到 `data/internalpaas.mv.db`
- 无需网络连接

**生产环境:**
- 使用PostgreSQL
- 需要配置 `application-prod.properties`

### 前端开发

```bash
# 安装依赖
npm install

# 启动Vite开发服务器
npm run dev

# TypeScript编译
npm run build
```

---

## 🧪 测试

### E2E测试

```bash
# 快速开始
cd e2e
npm install
npm run test:e2e

# 查看测试报告
npm run test:report
```

详见: [E2E测试快速开始](docs/guides/e2e-quickstart.md)

---

## 📈 路线图

### 已完成 ✅
- ✅ 基础服务器管理功能
- ✅ Metrics Hub核心功能 (T1-T5)
- ✅ 安全加固 (T7)
- ✅ 性能优化 (T8)
- ✅ 现代化UI重构
- ✅ E2E测试框架

### 进行中 🔄
- 🔄 Agent自动部署 (T6)
- 🔄 文档体系完善
- 🔄 CI/CD流程建设

### 规划中 📋
- 📋 多租户支持
- 📋 更多集成方式
- 📋 国际化支持

---

## 🤝 贡献指南

### 代码规范
- Java: 遵循[AGENTS.md](AGENTS.md)中的规范
- TypeScript: 4空格缩进,使用严格模式
- 提交信息: 遵循Conventional Commits

### 提交流程
1. Fork项目
2. 创建特性分支 (`git checkout -b feat/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add amazing feature'`)
4. 推送分支 (`git push origin feat/amazing-feature`)
5. 创建Pull Request

详见: [贡献者指南](CONTRIBUTING.md) (待创建)

---

## 📄 许可证

本项目为企业内部使用项目,版权归公司所有。

---

## 📞 联系方式

- **项目负责人:** [待填写]
- **技术支持:** [待填写]
- **问题反馈:** 使用GitHub Issues

---

## 🙏 致谢

感谢所有为本项目做出贡献的开发者!

---

**最后更新:** 2025-10-17  
**版本:** 1.0.0-SNAPSHOT
