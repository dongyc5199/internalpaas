# CodeHub 快速启动指南

本指南将帮助你在5分钟内启动整个CodeHub CI/CD平台。

## 📋 前置条件检查

在开始之前，请确保已安装：

```bash
# 检查Docker版本
docker --version
# 需要: Docker version 20.10+

# 检查Docker Compose版本
docker-compose --version
# 需要: Docker Compose version 2.0+

# 检查Go版本（仅本地开发）
go version
# 需要: go version go1.22.0
```

## 🚀 快速启动（Docker Compose）

### 步骤1: 克隆项目并配置环境变量

```bash
# 进入项目目录
cd internalpaas

# 复制环境变量模板
cp .env.example .env

# 编辑环境变量（必须！）
# Windows: notepad .env
# Linux/Mac: vim .env
```

**必须修改的关键配置**：
```bash
# 数据库密码
DB_PASSWORD=your_secure_password_here

# Redis密码
REDIS_PASSWORD=your_redis_password_here

# JWT密钥（使用以下命令生成）
# openssl rand -base64 32
JWT_SECRET=your_jwt_secret_key_here

# Drone RPC密钥（使用以下命令生成）
# openssl rand -hex 16
DRONE_RPC_SECRET=your_drone_rpc_secret_here
```

### 步骤2: 启动所有服务

```bash
# 启动所有容器
docker-compose up -d

# 查看启动状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

**预计启动时间**: 2-5分钟（首次启动需下载镜像）

### 步骤3: 等待服务就绪

```bash
# 检查服务健康状态
docker-compose ps

# 所有服务应显示为 "healthy" 或 "running"
```

**各服务启动顺序**:
1. PostgreSQL (10-20秒)
2. Redis (5-10秒)
3. Gitea (30-60秒)
4. Drone Server (20-30秒)
5. Drone Runner (10秒)
6. SonarQube (60-120秒) - 最慢
7. Nexus (60-90秒)
8. MinIO (5-10秒)
9. CodeHub Server (5-10秒)

### 步骤4: 首次配置

#### 4.1 配置Gitea

1. 访问 http://localhost:3000
2. 如果是首次启动，会自动跳过安装向导（已预配置）
3. 注册管理员账户:
   - 用户名: `admin`
   - 邮箱: `admin@codehub.local`
   - 密码: `Admin123!`

4. 生成访问令牌:
   - 进入: 设置 → 应用 → 生成新令牌
   - 名称: `CodeHub API Token`
   - 权限: 选择所有权限
   - 复制生成的token

5. 将token保存到`.env`:
   ```bash
   GITEA_ADMIN_TOKEN=你的token
   ```

#### 4.2 配置Drone OAuth

1. 在Gitea中创建OAuth应用:
   - 进入: 管��后台 → 应用 → 添加OAuth应用
   - 应用名称: `Drone CI`
   - 重定向URL: `http://localhost:8000/login`
   - 点击创建

2. 复制Client ID和Client Secret到`.env`:
   ```bash
   GITEA_OAUTH_CLIENT_ID=你的client_id
   GITEA_OAUTH_CLIENT_SECRET=你的client_secret
   ```

#### 4.3 配置SonarQube

1. 访问 http://localhost:9000
2. 默认登录:
   - 用户名: `admin`
   - 密码: `admin`
3. 首次登录会要求修改密码
4. 生成token:
   - 进入: 我的账户 → 安全 → 生成令牌
   - 名称: `CodeHub`
   - 复制token到`.env`:
     ```bash
     SONARQUBE_ADMIN_TOKEN=你的token
     ```

#### 4.4 配置Nexus

1. 访问 http://localhost:8081
2. 获取初始密码:
   ```bash
   docker exec codehub-nexus cat /nexus-data/admin.password
   ```
3. 使用admin和初始密码登录
4. 完成设置向导并修改密码
5. 将新密码保存到`.env`:
   ```bash
   NEXUS_ADMIN_PASSWORD=你的新密码
   ```

#### 4.5 配置MinIO

1. 访问 http://localhost:9001
2. 使用`.env`中配置的用户名密码登录:
   - 用户名: `admin`
   - 密码: `minio123456`
3. 创建bucket:
   - 点击 "Create Bucket"
   - 名称: `build-artifacts`
   - 访问策略: `Private`

#### 4.6 重启CodeHub服务

```bash
# 应用新的环境变量
docker-compose restart codehub-server

# 查看日志确认启动成功
docker-compose logs -f codehub-server
```

## ✅ 验证安装

### 1. 检查CodeHub API

```bash
# 健康检查
curl http://localhost:8880/health

# 应返回:
# {"status":"ok","version":"dev","build_time":"unknown"}

# Ping测试
curl http://localhost:8880/api/v1/ping

# 应返回:
# {"message":"pong","version":"dev"}
```

### 2. 注册第一个用户

```bash
curl -X POST http://localhost:8880/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer",
    "email": "developer@codehub.local",
    "password": "Dev123456",
    "full_name": "Developer"
  }'

# 应返回用户信息（不含密码）
```

### 3. 登录测试

```bash
curl -X POST http://localhost:8880/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer",
    "password": "Dev123456"
  }'

# 应返回JWT token
```

### 4. 访问各服务界面

| 服务 | URL | 默认账户 |
|------|-----|---------|
| CodeHub API | http://localhost:8880 | 自行注册 |
| Gitea | http://localhost:3000 | admin / Admin123! |
| Drone CI | http://localhost:8000 | 使用Gitea账户登录 |
| SonarQube | http://localhost:9000 | admin / (首次需改密) |
| Nexus | http://localhost:8081 | admin / (初始密码在容器内) |
| MinIO Console | http://localhost:9001 | admin / minio123456 |

## 🛠️ 本地开发模式

### 仅启动依赖服务

如果你想在本地开发CodeHub后端：

```bash
# 启动除codehub-server外的所有服务
docker-compose up -d postgres redis gitea drone-server drone-runner-docker sonarqube nexus minio

# 本地运行CodeHub
cd internalpaas
make run

# 或者
go run ./cmd/server
```

### 检查Go版本兼容性

```bash
# 项目强制使用Go 1.22.0
make check-go-version

# 如果版本不匹配，使用以下方式安装:
# 1. 使用gvm (推荐)
gvm install go1.22.0
gvm use go1.22.0

# 2. 或从官网下载
# https://golang.org/dl/go1.22.0
```

## 📝 后续步骤

1. **创建第一个项目**:
   ```bash
   curl -X POST http://localhost:8880/api/v1/projects \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "name": "demo-project",
       "display_name": "演示项目",
       "description": "我的第一个CI/CD项目"
     }'
   ```

2. **在Gitea中创建仓库**:
   - 访问 http://localhost:3000
   - 创建新仓库
   - 添加 `.drone.yml` 配置文件

3. **在Drone中启用CI**:
   - 访问 http://localhost:8000
   - 使用Gitea账户登录
   - 激活仓库
   - 推送代码触发构建

## ���� 常见问题

### 问题1: SonarQube启动失败

**症状**: 容器不断重启

**解决**:
```bash
# Linux需要调整vm.max_map_count
sudo sysctl -w vm.max_map_count=262144

# 永久生效
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
```

### 问题2: Drone无法连接Gitea

**症状**: Drone登录时提示连接错误

**解决**: 检查OAuth配置:
```bash
# 确认.env中的配置正确
cat .env | grep GITEA_OAUTH

# 重启Drone服务
docker-compose restart drone-server
```

### 问题3: CodeHub API连接数据库失败

**症状**: CodeHub日志显示数据库连接错误

**解决**:
```bash
# 检查PostgreSQL是否正常运行
docker-compose ps postgres

# 检查数据库密码是否正确
cat .env | grep DB_PASSWORD

# 重启服务
docker-compose restart codehub-server
```

### 问题4: 端口冲突

**症状**: 启动时提示端口已被占用

**解决**:
```bash
# Windows查看端口占用
netstat -ano | findstr "8880"

# Linux/Mac查看端口占用
lsof -i :8880

# 修改docker-compose.yml中的端口映射
# 例如将 "8880:8880" 改为 "8881:8880"
```

## 📚 下一步

- 阅读完整文档: [README.md](./README.md)
- API文档: [docs/api.md](./docs/api.md)
- 开发指南: [docs/development.md](./docs/development.md)
- 部署指南: [docs/deployment.md](./docs/deployment.md)

## 💡 提示

- 首次启动建议预留10-15分钟等待所有服务完全就绪
- 生产环境部署前务必修改所有默认密码
- 定期备份数据卷 `/var/lib/docker/volumes/internalpaas_*`
- 监控磁盘空间，特别是Nexus和Gitea的数据目录

---

**遇到问题？** 查看日志获取更多信息:
```bash
docker-compose logs -f [service_name]
```
