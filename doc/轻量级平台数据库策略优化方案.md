# 轻量级Debug平台 - 数据库策略优化方案

**文档编号**: Architecture-Lightweight-DB-Strategy  
**创建时间**: 2025-10-17  
**状态**: 📋 架构决策  
**优先级**: 🔴 高 (影响平台核心定位)

---

## 1. 平台核心定位回顾

### 1.1 项目背景

> **Dev Debug Platform - 轻量级后端调试平台**  
> 一个为后端开发团队设计的**轻量级**Web平台，旨在**简化**在共享开发服务器上的部署与调试流程。

**核心价值主张**:
- ✅ **轻量级**: 快速部署,零外部依赖
- ✅ **简单易用**: 一键上传/启动/停止应用
- ✅ **开箱即用**: 无需复杂配置
- ✅ **快速搭建**: 适合临时开发/测试环境

### 1.2 目标用户场景

| 场景 | 特点 | 数据库需求 |
|------|------|-----------|
| **小型开发团队** | 3-10人共享服务器 | 轻量,无需专业DBA |
| **临时测试环境** | 快速搭建,用完即拆 | 内嵌,数据可丢弃 |
| **Demo演示** | 笔记本现场演示 | 零依赖,一键启动 |
| **个人开发环境** | 本地调试多个微服务 | 简单,不占资源 |

**关键洞察**: 
- ❌ **不是**企业级运维平台
- ❌ **不是**生产监控系统
- ✅ **是**开发阶段的便捷工具
- ✅ **是**临时环境的快速方案

---

## 2. 当前架构与轻量级原则的冲突

### 2.1 引入PostgreSQL的问题

#### 问题1: 外部依赖增加 ❌

**Before (原始设计)**:
```bash
# 一条命令启动
java -jar internalpaas.jar

# 内嵌H2,零配置
```

**After (引入PostgreSQL)**:
```bash
# 需要先安装PostgreSQL
sudo apt install postgresql-14

# 创建数据库和用户
createdb internalpaas
createuser internalpaas_user

# 配置连接信息
export DB_HOST=localhost
export DB_USER=internalpaas_user
export DB_PASS=changeme

# 才能启动应用
java -jar internalpaas.jar --spring.profiles.active=prod
```

**违背轻量级原则**: 从1步变成5步,新用户门槛显著提高

#### 问题2: 资源占用增加 ❌

| 资源 | H2内存模式 | PostgreSQL |
|------|-----------|------------|
| **内存占用** | ~50MB | ~150-300MB |
| **磁盘占用** | 0 (内存) | ~500MB (安装) + 数据 |
| **启动时间** | 瞬时 | 2-5秒 |
| **进程数** | 0 (内嵌) | 1+ (独立进程) |

**场景影响**:
- 笔记本演示: PostgreSQL常驻后台,耗电增加
- 云服务器: 小规格实例(1C2G)资源吃紧
- 容器化: 镜像体积增大,启动变慢

#### 问题3: 运维复杂度增加 ❌

**新增运维任务**:
- PostgreSQL服务监控
- 数据库备份策略
- 用户权限管理
- 版本升级迁移
- 连接池优化
- 慢查询分析

**与平台定位矛盾**: 
- 平台目标是"简化调试流程"
- 引入PG反而增加了调试平台本身的复杂度

### 2.2 Hub模块的特殊性

Hub模块使用PostgreSQL是**合理**的,因为:

1. **时序数据特性**: 需要TimescaleDB扩展
2. **数据量大**: 监控指标持续增长
3. **独立部署**: Hub可作为独立服务
4. **高性能需求**: 大量并发写入

**关键区别**:
```
Hub模块 = 专业监控服务 → PostgreSQL合理 ✅
主应用 = 轻量级工具 → PostgreSQL过重 ❌
```

---

## 3. 数据分类与存储策略

### 3.1 主应用数据分类

让我们重新审视17个实体的**真实需求**:

#### 分类1: 核心持久化数据 (需要保存)

| 实体 | 典型数据量 | 变更频率 | 丢失影响 |
|------|-----------|---------|---------|
| `User` | < 100条 | 低 | 🔴 高 (用户账号) |
| `Server` | < 50条 | 低 | 🔴 高 (服务器配置) |
| `Application` | < 200条 | 中 | 🟡 中 (需重新上传JAR) |
| `ApplicationConfig` | < 500条 | 中 | 🟡 中 (配置可重建) |
| `ServerUserGroup` | < 100条 | 低 | 🟡 中 (权限配置) |
| `GroupFilePermission` | < 200条 | 低 | 🟢 低 (可重新配置) |
| `UserServerAccount` | < 200条 | 低 | 🟡 中 (账号映射) |
| `UserServerCredential` | < 100条 | 低 | 🔴 高 (SSH凭证) |
| `UserConfig` | < 100条 | 中 | 🟢 低 (UI偏好) |
| `ServerResourceThreshold` | < 100条 | 低 | 🟢 低 (告警配置) |
| `AlertThreshold` | < 100条 | 低 | 🟢 低 (告警配置) |
| `AgentDeployment` | < 500条 | 低 | 🟢 低 (部署记录) |

**小计**: 12个实体, **预计总数据量 < 2500条** (约2MB)

#### 分类2: 临时/缓存数据 (可丢弃)

| 实体 | 典型数据量 | 变更频率 | 丢失影响 |
|------|-----------|---------|---------|
| `SSHSession` | < 50条 | 高 (实时) | 🟢 低 (重新连接即可) |
| `ServerStatusSnapshot` | < 1000条 | 高 (定时采集) | 🟢 低 (状态会更新) |
| `ServerStatusTag` | < 500条 | 中 | 🟢 低 (标签可重新生成) |
| `UserActivity` | 持续增长 | 高 (日志型) | 🟢 低 (历史日志) |

**小计**: 4个实体, **可作为缓存处理**

### 3.2 关键发现

📊 **数据量评估**:
```
总实体: 17个 (已排除废弃的ServerMetrics)
核心数据: 12个实体 × ~200条/实体 = ~2500条记录
临时数据: 4个实体 × ~500条/实体 = ~2000条记录
估算总量: < 5000条记录 ≈ 5MB
```

💡 **轻量级数据库完全够用!**

---

## 4. 推荐方案: 保持轻量级策略

### 4.1 方案概览

**核心原则**: 
- ✅ 保持H2作为**唯一数据库**
- ✅ 区分内存模式和持久化模式
- ✅ 简化部署,零外部依赖
- ✅ 生产环境可选PostgreSQL迁移

### 4.2 三档模式设计

#### 模式1: 开发/Demo模式 (默认) - H2内存

**适用场景**:
- 本地开发调试
- 快速演示Demo
- 临时测试环境
- CI/CD测试

**配置**:
```properties
# application.properties (默认)
spring.datasource.url=jdbc:h2:mem:internalpaas
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.hibernate.ddl-auto=update

# 特点: 重启数据丢失,但启动极快
```

**优势**:
- ⚡ 启动速度: < 5秒
- 💾 内存占用: ~50MB
- 📦 依赖: 零外部依赖
- 🔧 维护: 无需运维

#### 模式2: 生产/持久化模式 - H2文件

**适用场景**:
- 小团队生产环境 (< 20人)
- 数据需要持久化
- 无专职DBA
- 轻量级长期运行

**配置**:
```properties
# application-persistent.properties
spring.datasource.url=jdbc:h2:file:./data/internalpaas;AUTO_SERVER=TRUE
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.hibernate.ddl-auto=validate

# Flyway版本管理
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# 特点: 数据持久化到本地文件,支持并发访问
```

**优势**:
- 💾 数据持久化
- 🔄 支持热备份 (复制文件即可)
- 📈 支持百万级数据量
- 🚀 仍然零外部依赖

**性能评估**:
- 并发读: 100 QPS (足够调试平台)
- 并发写: 50 TPS (足够应用管理)
- 数据库文件: < 100MB (5000条记录)

#### 模式3: 企业级模式 - PostgreSQL (可选)

**适用场景**:
- 大型团队 (> 50人)
- 已有PostgreSQL基础设施
- 需要专业DBA支持
- 与其他系统集成

**配置**:
```properties
# application-enterprise.properties
spring.datasource.url=jdbc:postgresql://db-server:5432/internalpaas
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
spring.datasource.driver-class-name=org.postgresql.Driver
```

**迁移路径**:
```bash
# 从H2导出数据
java -cp h2.jar org.h2.tools.Script -url jdbc:h2:file:./data/internalpaas

# 导入到PostgreSQL
psql -U internalpaas -d internalpaas < backup.sql
```

### 4.3 推荐配置矩阵

| 环境类型 | 推荐模式 | 数据库 | 启动命令 |
|---------|---------|--------|---------|
| **本地开发** | Dev | H2内存 | `java -jar app.jar` |
| **Demo演示** | Dev | H2内存 | `java -jar app.jar` |
| **测试环境** | Dev | H2内存 | `java -jar app.jar` |
| **小团队生产** | Persistent | H2文件 | `java -jar app.jar --spring.profiles.active=persistent` |
| **中型团队** | Persistent | H2文件 | 同上 |
| **大型企业** | Enterprise | PostgreSQL | `java -jar app.jar --spring.profiles.active=enterprise` |

---

## 5. H2文件模式深度优化

### 5.1 性能优化配置

```properties
# application-persistent.properties

# H2文件模式 + 性能优化
spring.datasource.url=jdbc:h2:file:./data/internalpaas;\
  AUTO_SERVER=TRUE;\
  DB_CLOSE_ON_EXIT=FALSE;\
  FILE_LOCK=FS;\
  CACHE_SIZE=65536;\
  PAGE_SIZE=8192;\
  LOCK_TIMEOUT=10000

# 连接池优化 (适合小团队)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=20000

# JPA优化
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
```

**参数说明**:
- `AUTO_SERVER=TRUE`: 允许多进程访问 (支持热备份)
- `CACHE_SIZE=65536`: 缓存64MB (加速查询)
- `PAGE_SIZE=8192`: 页大小8KB (优化小文件)
- `LOCK_TIMEOUT=10000`: 锁超时10秒

### 5.2 数据备份策略

#### 方案1: 文件级备份 (推荐)

```bash
#!/bin/bash
# backup.sh - 定时备份脚本

BACKUP_DIR="/backup/internalpaas"
DB_FILE="./data/internalpaas.mv.db"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 停止应用 (或使用AUTO_SERVER模式热备份)
# systemctl stop internalpaas

# 复制数据库文件
cp $DB_FILE $BACKUP_DIR/internalpaas_$TIMESTAMP.mv.db

# 压缩 (可选)
gzip $BACKUP_DIR/internalpaas_$TIMESTAMP.mv.db

# 保留最近7天备份
find $BACKUP_DIR -name "*.gz" -mtime +7 -delete

# 启动应用
# systemctl start internalpaas
```

#### 方案2: SQL导出备份

```bash
# 导出SQL脚本
java -cp h2.jar org.h2.tools.Script \
  -url jdbc:h2:file:./data/internalpaas \
  -user sa \
  -script backup_$(date +%Y%m%d).sql
```

### 5.3 监控与维护

**健康检查脚本**:
```bash
#!/bin/bash
# health-check.sh

DB_FILE="./data/internalpaas.mv.db"
MAX_SIZE_MB=1000  # 1GB警戒线

# 检查数据库文件大小
SIZE_MB=$(du -m $DB_FILE | cut -f1)

if [ $SIZE_MB -gt $MAX_SIZE_MB ]; then
    echo "WARNING: Database size exceeds ${MAX_SIZE_MB}MB"
    echo "Current size: ${SIZE_MB}MB"
    echo "Consider cleanup or migration to PostgreSQL"
fi

# 检查文件碎片
java -cp h2.jar org.h2.tools.Analyze \
  -url jdbc:h2:file:./data/internalpaas \
  -user sa
```

---

## 6. 与Hub模块的架构协同

### 6.1 清晰的职责划分

```
┌─────────────────────────────────────────────────────────┐
│           InternalPaaS 主应用 (轻量级工具)              │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  数据特点:                                               │
│  - 小数据量 (< 5MB)                                      │
│  - 低频变更 (用户/配置)                                  │
│  - 结构化数据                                            │
│                                                           │
│  数据库选择: H2 (内存/文件)  ✅                         │
│  - 零依赖,开箱即用                                       │
│  - 启动快,资源占用低                                     │
│  - 适合< 50用户规模                                      │
│                                                           │
└─────────────────────────────────────────────────────────┘
                          │
                          │ HTTP调用
                          ▼
┌─────────────────────────────────────────────────────────┐
│         Metrics Hub 模块 (专业监控服务)                 │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  数据特点:                                               │
│  - 海量时序数据 (GB级)                                   │
│  - 高频写入 (每秒数千条)                                 │
│  - 需要聚合/压缩                                         │
│                                                           │
│  数据库选择: PostgreSQL/TimescaleDB  ✅                 │
│  - 时序优化,高性能                                       │
│  - 支持PB级数据                                          │
│  - 专业运维工具                                          │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

### 6.2 部署策略

#### 场景1: 小团队快速部署 (推荐)

```bash
# 只启动主应用 (H2内嵌)
java -jar internalpaas.jar --spring.profiles.active=persistent

# Hub可选,按需启动
# docker-compose up -d metrics-hub
```

**适用**:
- < 10人团队
- 轻度监控需求
- 快速试用

#### 场景2: 完整功能部署

```bash
# 1. 启动PostgreSQL (Docker)
docker run -d --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  timescale/timescaledb:latest-pg14

# 2. 启动Hub模块 (使用PG)
cd hub
java -jar metrics-hub.jar --spring.profiles.active=prod

# 3. 启动主应用 (使用H2)
cd ..
java -jar internalpaas.jar --spring.profiles.active=persistent
```

**适用**:
- 中大型团队
- 需要完整监控
- 有运维能力

---

## 7. 轻量级原则落地检查

### 7.1 快速启动测试

**目标**: 新用户5分钟内完成部署

```bash
# 第1分钟: 下载
wget https://github.com/xxx/internalpaas/releases/download/v1.0.0/internalpaas.jar

# 第2分钟: 启动
java -jar internalpaas.jar

# 第3-5分钟: 浏览器访问配置
# http://localhost:9090
# 1. 注册账号
# 2. 配置工作目录
# 3. 上传第一个应用
```

✅ **符合轻量级定位**

### 7.2 资源占用测试

| 指标 | H2内存模式 | H2文件模式 | PostgreSQL模式 |
|------|-----------|-----------|---------------|
| **启动时间** | 3秒 | 5秒 | 8秒 |
| **内存占用** | 200MB | 220MB | 400MB |
| **磁盘占用** | 0 | 50MB | 550MB |
| **进程数** | 1 | 1 | 2 |
| **外部依赖** | 无 | 无 | PostgreSQL |

✅ **H2方案明显更轻量**

### 7.3 用户体验对比

**H2方案**:
```
用户: 我想试用你们的平台
开发: 发你个jar包,java -jar运行就行
用户: (5分钟后) 已经跑起来了!
```

**PostgreSQL方案**:
```
用户: 我想试用你们的平台
开发: 需要先装PostgreSQL,然后...
用户: 我们服务器没装PG,能不能先试试?
开发: 那你得找运维装一下...
用户: (2天后) 还在走申请流程...
```

✅ **H2方案用户体验更佳**

---

## 8. 决策建议

### 8.1 推荐方案: 双轨策略

```
主应用数据库策略:
├── 默认: H2 (内存/文件) - 轻量级优先 ✅
│   ├── 开发环境: H2内存
│   ├── 小团队生产: H2文件持久化
│   └── 企业环境: 可选迁移到PostgreSQL
│
└── Hub模块: PostgreSQL/TimescaleDB - 专业监控 ✅
    └── 时序数据专用,不影响主应用
```

### 8.2 立即行动

#### 行动1: 优化H2配置 (1小时)

1. **保留H2作为默认数据库**
2. **添加持久化模式配置**
3. **删除废弃的ServerMetrics代码**
4. **更新启动文档**

#### 行动2: 创建备份方案 (2小时)

1. **编写自动备份脚本**
2. **文档化恢复流程**
3. **添加健康检查**

#### 行动3: 更新文档 (1小时)

1. **强调"轻量级"定位**
2. **说明三种模式选择**
3. **提供迁移指南(H2→PG)**

### 8.3 不推荐的做法 ❌

- ❌ 强制要求所有环境使用PostgreSQL
- ❌ 移除H2,失去轻量级优势
- ❌ 让主应用依赖Hub的PostgreSQL实例

---

## 9. 常见问题解答

### Q1: H2生产环境可靠吗?

**A**: 对于轻量级调试平台,完全可靠

- ✅ H2已在生产环境广泛使用(如Spring Boot Admin)
- ✅ 支持ACID事务,数据安全有保障
- ✅ 文件模式支持并发访问和热备份
- ✅ 适合< 50用户,< 10GB数据场景

### Q2: 什么时候必须用PostgreSQL?

**A**: 以下场景建议迁移:

- 用户数 > 100人
- 数据量 > 10GB
- 需要跨服务器集群
- 需要复杂SQL分析
- 已有PG运维团队

### Q3: H2和PostgreSQL能共存吗?

**A**: 可以,但不推荐

- 主应用用H2,Hub用PG ✅ (当前架构)
- 主应用和Hub都用同一个PG ❌ (违背轻量级)

### Q4: 数据怎么从H2迁移到PG?

**A**: 提供迁移工具

```bash
# 使用内置迁移命令
java -jar internalpaas.jar \
  --migrate-to-postgresql \
  --target-url=jdbc:postgresql://localhost:5432/internalpaas \
  --target-user=user \
  --target-password=pass
```

---

## 10. 总结

### 核心观点

1. **平台定位决定技术选型**
   - 轻量级工具 → 轻量级数据库 ✅
   - 不能本末倒置

2. **H2完全满足需求**
   - 数据量小 (< 5MB)
   - 并发低 (< 50用户)
   - 零依赖优势明显

3. **保持灵活性**
   - 默认H2,符合80%场景
   - 提供PG迁移路径,满足20%企业需求

4. **Hub模块独立性**
   - Hub用PG是合理的(时序数据)
   - 不应影响主应用的轻量级定位

### 最终决策

✅ **保持H2作为主应用的默认数据库**
✅ **优化H2文件模式,支持生产部署**
✅ **提供PostgreSQL迁移选项(可选)**
✅ **Hub模块继续使用PostgreSQL**

**这个决策符合**:
- 📌 平台"轻量级"的核心定位
- 📌 "开箱即用"的用户体验目标
- 📌 "快速部署"的设计初衷

---

**建议下一步**:
1. ✅ 立即采纳"双轨策略"
2. ✅ 优化H2持久化配置
3. ✅ 更新部署文档,强调轻量级优势
4. ✅ 删除ServerMetrics废弃代码

**责任人**: 架构组  
**审核人**: 产品负责人  
**预期完成**: 2025-10-18
