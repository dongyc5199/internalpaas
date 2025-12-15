# CodeHub - 轻量级CI/CD平台

> 基于开源组件集成的企业级代码管理与持续集成平台

## 📋 项目概述

CodeHub是一个面向局域网环境的轻量级CI/CD平台,集成了代码管理、持续集成、代码质量扫描和制品库管理功能。采用��流开源技术栈,避免重复造轮子,专注于业务集成与用户体验。

### 核心特性

- **🔐 代码管理**: 基于Gitea的轻量级Git托管服务
- **🚀 CI/CD流水线**: 基于Drone CI的云原生构建引擎
- **📊 代码质量**: 集成SonarQube的静态代码分析
- **📦 制品管理**: 基于Nexus的Maven/Docker/npm仓库
- **💾 对象存储**: 基于MinIO的构建产物存储
- **🎯 统一门户**: 单点登录与统一工作台

### 系统规模

- **用户数**: 100人
- **代码仓库**: 50个
- **日构建量**: 100+
- **并发构建**: 10个

## 🏗️ 技术架构

### 技术栈

| 组件 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| **后端语言** | Go | 1.22.0 | 性能与并发优势 |
| **Web框架** | Gin | 1.10.0 | 轻量级HTTP框架 |
| **代码管理** | Gitea | 1.21 | 轻量级Git托管 |
| **CI/CD引擎** | Drone CI | 2.20 | 云原生构建引擎 |
| **代码质量** | SonarQube | 10.3 Community | 静态代码分析 |
| **制品仓库** | Nexus OSS | 3.63 | 多格式制品管理 |
| **数据库** | PostgreSQL | 15 | 关系型数据库 |
| **缓存** | Redis | 7 | 高性能缓存 |
| **对象存储** | MinIO | Latest | S3兼容存储 |

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      CodeHub统一门户                         │
│                    (Go Backend + React)                      │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐     ┌──────────────┐
│   Gitea      │    │  Drone CI    │     │  SonarQube   │
│  代码管理     │◄───┤  持续集成     │────►│  代码质量     │
└──────────────┘    └──────────────┘     └──────────────┘
        │                     │                     │
        │                     ▼                     │
        │            ┌──────────────┐              │
        └───────────►│    Nexus     │◄─────────────┘
                     │   制品仓库    │
                     └──────────────┘
                              │
                              ▼
                     ┌──────────────┐
                     │    MinIO     │
                     │   对象存储    │
                     └──────────────┘
```

## 🚀 快速开始

### 前置要求

- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **Go**: 1.22.0 (本地开发)
- **Git**: 2.30+

### 服务器配置建议

- **CPU**: 8核心
- **内存**: 16GB
- **磁盘**: 500GB SSD
- **网络**: 局域网

### 安装步骤

#### 1. 克隆项目

```bash
git clone <repository-url>
cd internalpaas
```

#### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量（重要！）
vim .env
```

**必须修改的配置**:
- `DB_PASSWORD`: 数据库密码
- `REDIS_PASSWORD`: Redis密码
- `DRONE_RPC_SECRET`: Drone RPC密钥（建议使用 `openssl rand -hex 16`）
- `JWT_SECRET`: JWT签名密钥（建议使用 `openssl rand -base64 32`）

#### 3. 启动所有服务

```bash
# 启动所有容器
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

#### 4. 首次配置

**4.1 访问Gitea (http://localhost:3000)**

1. 使用admin账户登录（首次需要注册）
2. 创建组织和仓库
3. 生成访问令牌: 设置 → 应用 → 生成新令牌
4. 将令牌保存到`.env`的`GITEA_ADMIN_TOKEN`

**4.2 配置Gitea OAuth (用于Drone登录)**

1. Gitea管理后台 → 应用 → 添加OAuth应用
   - 应用名称: Drone CI
   - 重定向URL: `http://localhost:8000/login`
2. 复制Client ID和Client Secret到`.env`

**4.3 访问Drone CI (http://localhost:8000)**

1. 使用Gitea账户登录（OAuth）
2. 激活需要CI的仓库
3. 生成令牌: 用户设置 → Tokens
4. 将令牌保存到`.env`的`DRONE_ADMIN_TOKEN`

**4.4 访问SonarQube (http://localhost:9000)**

1. 默认账户: admin / admin（首次需要修改密码）
2. 生成令牌: 用户 → 安全 → 生成令牌
3. 将令牌保存到`.env`的`SONARQUBE_ADMIN_TOKEN`

**4.5 访问Nexus (http://localhost:8081)**

1. 获取初始密码:
   ```bash
   docker exec codehub-nexus cat /nexus-data/admin.password
   ```
2. 登录并修改密码
3. 将新密码保存到`.env`的`NEXUS_ADMIN_PASSWORD`

**4.6 访问MinIO (http://localhost:9001)**

1. 使用`.env`中配置的`MINIO_ROOT_USER`和`MINIO_ROOT_PASSWORD`登录
2. 创建bucket: `build-artifacts`

#### 5. 重启CodeHub服务

```bash
# 配置完成后重启CodeHub后端
docker-compose restart codehub-server
```

### 服务端口列表

| 服务 | 端口 | 用途 |
|------|------|------|
| CodeHub API | 8880 | 统一后端服务 |
| Gitea HTTP | 3000 | Web界面 |
| Gitea SSH | 2222 | Git SSH访问 |
| Drone CI | 8000 | CI/CD界面 |
| SonarQube | 9000 | 代码质量平台 |
| Nexus HTTP | 8081 | 制品仓库界面 |
| Nexus Docker | 8082 | Docker Registry |
| MinIO API | 9000 | S3 API |
| MinIO Console | 9001 | 管理控制台 |
| PostgreSQL | 5432 | 数据库 |
| Redis | 6379 | 缓存 |

## 🛠️ 本地开发

### 环境准备

```bash
# 检查Go版本（必须是1.22.0）
make check-go-version

# 安装开发工具
make install-tools

# 下载依赖
go mod download
```

### 开发命令

```bash
# 运行开发服务器
make run

# 编译项目
make build

# 运行测试
make test

# 代码格式化
make fmt

# 代码检查
make lint

# 漏洞扫描
make vuln-check

# 查看覆盖率
make test-coverage
```

### 项目结构

```
.
├── cmd/
│   └── server/          # 应用入口
├── internal/
│   ├── api/v1/          # API处理器
│   ├── service/         # 业务逻辑
│   ├── model/           # 数据模型
│   ├── middleware/      # 中间件
│   └── pkg/             # 工具包
├── config/              # 配置文件
├── deployments/         # 部署配置
├── scripts/             # 辅助脚本
├── docs/                # 文档
├── docker-compose.yml   # Docker编排
├── Dockerfile           # 容器构建
├── Makefile             # 开发工具链
└── go.mod               # Go模块定义
```

## 📝 使用指南

### 创建第一个项目

1. **在Gitea中创建仓库**
   - 访问http://localhost:3000
   - 创建新仓库: `my-project`

2. **添加.drone.yml流水线配置**

```yaml
kind: pipeline
type: docker
name: default

steps:
  - name: build
    image: golang:1.22.0-alpine
    commands:
      - go build -o app ./cmd/server

  - name: test
    image: golang:1.22.0-alpine
    commands:
      - go test -v ./...

  - name: sonar-scan
    image: sonarsource/sonar-scanner-cli:latest
    environment:
      SONAR_HOST_URL: http://sonarqube:9000
      SONAR_TOKEN:
        from_secret: sonar_token
    commands:
      - sonar-scanner

  - name: publish
    image: plugins/docker
    settings:
      registry: localhost:8082
      repo: localhost:8082/my-project
      tags: latest
    when:
      branch: main
```

3. **推送代码触发构建**

```bash
git add .
git commit -m "Add CI pipeline"
git push origin main
```

4. **在Drone中查看构建状态**
   - 访问http://localhost:8000
   - 查看构建日志和结果

## 🔧 运维指南

### 备份策略

```bash
# 备份所有数据卷
docker-compose down
tar -czf codehub-backup-$(date +%Y%m%d).tar.gz \
  /var/lib/docker/volumes/internalpaas_*

# 恢复
tar -xzf codehub-backup-20231215.tar.gz -C /
docker-compose up -d
```

### 日志查看

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f codehub-server
docker-compose logs -f gitea
docker-compose logs -f drone-server
```

### 性能监控

```bash
# 查看容器资源使用
docker stats

# 查看服务健康状态
docker-compose ps
```

### 常见问题

**Q: Drone无法连接Gitea**
- 检查`.env`中的OAuth配置是否正确
- 确认Gitea和Drone容器在同一网络

**Q: SonarQube启动失败**
- 检查系统`vm.max_map_count`设置:
  ```bash
  sudo sysctl -w vm.max_map_count=262144
  ```

**Q: Nexus内存不足**
- 调整`docker-compose.yml`中的JVM参数:
  ```yaml
  environment:
    - INSTALL4J_ADD_VM_PARAMS=-Xms2g -Xmx4g
  ```

## 📚 文档

- [API文档](docs/api.md)
- [架构设计](docs/architecture.md)
- [开发指南](docs/development.md)
- [部署指南](docs/deployment.md)

## 🤝 贡献指南

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License

## 📧 联系方式

- 项目负责人: [Your Name]
- 邮箱: [your.email@example.com]
