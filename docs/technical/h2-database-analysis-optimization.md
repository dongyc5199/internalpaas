# H2数据库使用分析与优化建议

**文档编号**: DB-Analysis-H2-PostgreSQL  
**创建时间**: 2025-10-17  
**状态**: 📋 待决策  
**优先级**: 🔴 高 (影响架构一致性)

---

## 1. 问题概述

### 1.1 当前问题
在**阶段4**数据存储迁移过程中,H2数据库的使用出现了**不一致性**:

1. **监控数据已迁移**: `ServerMetrics` 实体的数据采集和存储已完全迁移到 **Metrics Hub (PostgreSQL/TimescaleDB)**
2. **H2仍在使用**: 主应用中仍有多个非监控实体依赖H2数据库
3. **架构分裂**: Hub模块使用PostgreSQL,主应用使用H2,存在双数据库架构
4. **配置不明确**: H2的定位从"主数据库"变为"临时开发数据库",但文档和配置不清晰

### 1.2 影响范围
- ❌ **启动失败**: H2配置不当导致应用启动异常
- ❌ **架构混乱**: 同一项目使用两套数据库方案
- ❌ **运维复杂**: 生产环境需要额外迁移H2到PostgreSQL
- ❌ **开发困惑**: 开发人员不清楚应该使用哪个数据库

---

## 2. 当前数据库使用情况

### 2.1 主应用 (internalpaas) - H2数据库

#### 2.1.1 依赖配置
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>  <!-- 开发/测试环境可用 -->
</dependency>
```

#### 2.1.2 数据库配置
```properties
# application.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

**特点**:
- ✅ 内存数据库,启动快
- ✅ 零配置,开发便捷
- ❌ 重启数据丢失
- ❌ 生产不可用

#### 2.1.3 实体清单 (19个JPA实体)

| 类别 | 实体名称 | 表名 | 用途 | 数据特点 |
|------|---------|------|------|---------|
| **核心业务** | `User` | `users` | 用户账户 | 持久化,低频变更 |
| | `Server` | `servers` | 服务器配置 | 持久化,中频变更 |
| | `Application` | `applications` | 应用程序 | 持久化,中频变更 |
| | `ApplicationConfig` | `application_configs` | 应用配置 | 持久化,中频变更 |
| **权限管理** | `ServerUserGroup` | `server_user_groups` | 用户组 | 持久化,低频变更 |
| | `GroupFilePermission` | `group_file_permissions` | 文件权限 | 持久化,低频变更 |
| | `UserServerAccount` | `user_server_accounts` | 用户账号映射 | 持久化,低频变更 |
| | `UserServerCredential` | `user_server_credentials` | 用户凭证 | 持久化,低频变更 |
| **配置&状态** | `UserConfig` | `user_configs` | 用户配置 | 持久化,中频变更 |
| | `ServerStatusTag` | `server_status_tags` | 服务器标签 | 持久化,高频变更 |
| | `ServerStatusSnapshot` | `server_status_snapshots` | 状态快照 | 临时性,高频写入 |
| | `ServerResourceThreshold` | `server_resource_thresholds` | 资源阈值 | 持久化,低频变更 |
| | `AlertThreshold` | `alert_thresholds` | 告警阈值 | 持久化,低频变更 |
| **会话&活动** | `SSHSession` | `ssh_sessions` | SSH会话 | 临时性,高频写入 |
| | `UserActivity` | `user_activities` | 用户活动日志 | 日志型,高频写入 |
| **部署管理** | `AgentDeployment` | `agent_deployments` | Agent部署记录 | 持久化,低频变更 |
| **⚠️已废弃** | `ServerMetrics` | `server_metrics` | **监控指标(已迁移)** | ~~H2存储~~ → **Hub存储** |

**⚠️ 关键发现**:
1. **17个有效实体**仍依赖H2数据库 (已排除废弃的`ServerMetrics`)
2. **3类数据特征**:
   - 🔵 **持久化数据** (12实体): User, Server, Application等核心配置
   - 🟡 **临时性数据** (2实体): SSHSession, ServerStatusSnapshot
   - 🟠 **日志型数据** (3实体): UserActivity, ServerStatusTag

### 2.2 Hub模块 (metrics-hub) - PostgreSQL/TimescaleDB

#### 2.2.1 依赖配置
```xml
<!-- hub/pom.xml -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- H2仅用于测试 -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

#### 2.2.2 数据库配置
```yaml
# hub/src/main/resources/application.yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/metrics_hub
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # 使用Flyway管理schema

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

**特点**:
- ✅ 生产级数据库,高性能
- ✅ TimescaleDB时序优化
- ✅ 支持大规模数据
- ✅ Flyway版本管理
- ❌ 需要外部安装配置

#### 2.2.3 实体清单
- `ServerMetricsEntity` (时序数据)
- `MetricsRollup5m` (5分钟聚合)
- `MetricsRollup1h` (1小时聚合)

---

## 3. 架构问题分析

### 3.1 当前架构图

```
┌────────────────────────────────────────────────────────────┐
│                      InternalPaaS 主应用                    │
├────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────┐          ┌─────────────────┐         │
│  │  业务实体 (17个) │          │ ServerMetrics   │         │
│  │  - User         │          │  (已废弃)       │         │
│  │  - Server       │          └─────────────────┘         │
│  │  - Application  │                   │                   │
│  │  - ...          │                   │ (已迁移)         │
│  └─────────────────┘                   ▼                   │
│         │                    ┌─────────────────────┐       │
│         ├───────────────────▶│  H2 (mem:testdb)   │       │
│         │                    │  - 开发/测试环境    │       │
│         │                    │  - 重启数据丢失     │       │
│         │                    │  - 生产不推荐       │       │
│         │                    └─────────────────────┘       │
│         │                                                   │
│         │ HTTP调用                                         │
│         ▼                                                   │
│  ┌─────────────────────────────────────────┐              │
│  │      MetricsHubClient (HTTP Client)      │              │
│  │  - 读取监控数据                          │              │
│  │  - 代理Hub查询                           │              │
│  └─────────────────────────────────────────┘              │
│         │                                                   │
└─────────┼───────────────────────────────────────────────────┘
          │ HTTP (localhost:8080)
          ▼
┌────────────────────────────────────────────────────────────┐
│                      Metrics Hub 模块                       │
├────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────┐              │
│  │    OTLP 接收器 (gRPC/HTTP)              │              │
│  │  - 端口: 4317 (gRPC), 4318 (HTTP)       │              │
│  └─────────────────────────────────────────┘              │
│         │                                                   │
│         ▼                                                   │
│  ┌─────────────────────────────────────────┐              │
│  │         时序数据实体 (3个)               │              │
│  │  - ServerMetricsEntity (原始数据)       │              │
│  │  - MetricsRollup5m (5分钟聚合)          │              │
│  │  - MetricsRollup1h (1小时聚合)          │              │
│  └─────────────────────────────────────────┘              │
│         │                                                   │
│         ▼                                                   │
│  ┌─────────────────────────────────────────┐              │
│  │  PostgreSQL/TimescaleDB (metrics_hub)   │              │
│  │  - 生产级数据库                          │              │
│  │  - 时序优化                              │              │
│  │  - Flyway版本管理                        │              │
│  └─────────────────────────────────────────┘              │
│                                                              │
└────────────────────────────────────────────────────────────┘
```

### 3.2 问题识别

#### 🔴 问题1: 双数据库架构分裂
- **现象**: 主应用用H2,Hub模块用PostgreSQL
- **影响**: 
  - 运维复杂度增加
  - 开发环境需要同时启动PostgreSQL和H2
  - 生产环境需要额外数据迁移工作
- **根因**: 历史遗留,逐步迁移过程中的中间状态

#### 🔴 问题2: H2定位不清
- **现象**: H2既不是"主数据库"也不是"临时测试库"
- **影响**:
  - 开发者不知道何时用H2,何时用PostgreSQL
  - 配置文件注释矛盾("已移除"vs"恢复使用")
  - 生产部署指南缺失
- **根因**: 阶段4迁移未完成,缺少明确定位

#### 🟡 问题3: 数据持久化策略混乱
- **现象**: 核心业务数据存储在内存数据库(H2)中
- **影响**:
  - 重启后用户、服务器配置等数据丢失
  - 开发环境需要频繁重建测试数据
  - 与生产环境数据库类型不一致
- **根因**: 未制定明确的数据分类和存储策略

#### 🟡 问题4: ServerMetrics实体仍存在
- **现象**: 虽然已废弃,但`ServerMetrics.java`和`ServerMetricsRepository.java`仍在代码库
- **影响**:
  - H2仍会创建`server_metrics`表(浪费资源)
  - 代码库中存在"僵尸代码"
  - 可能误导开发者
- **根因**: 阶段4 Step 3未彻底清理

---

## 4. PostgreSQL复用可行性分析

### 4.1 技术可行性: ✅ 完全可行

#### 方案1: 主应用直接复用Hub的PostgreSQL实例

```yaml
# 主应用 application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/internalpaas
spring.datasource.username=internalpaas_user
spring.datasource.password=strong_password
spring.datasource.driver-class-name=org.postgresql.Driver

# 与Hub共享PostgreSQL服务器,但使用不同数据库
# Hub使用: metrics_hub
# 主应用使用: internalpaas
```

**优点**:
- ✅ 只需一个PostgreSQL实例
- ✅ 统一数据库技术栈
- ✅ 生产环境与开发环境一致
- ✅ 支持数据持久化

**缺点**:
- ⚠️ 需要安装配置PostgreSQL
- ⚠️ 开发环境启动复杂度略增

#### 方案2: 使用Profile区分环境

```yaml
# application.properties (默认H2开发环境)
spring.profiles.active=dev
spring.datasource.url=jdbc:h2:mem:testdb

---
# application-prod.properties (生产环境PostgreSQL)
spring.datasource.url=jdbc:postgresql://prod-db:5432/internalpaas
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
spring.datasource.driver-class-name=org.postgresql.Driver
```

**优点**:
- ✅ 开发环境简单(H2)
- ✅ 生产环境可靠(PostgreSQL)
- ✅ Profile切换灵活

**缺点**:
- ⚠️ 开发与生产数据库不一致
- ⚠️ 可能出现兼容性问题(H2 vs PostgreSQL SQL方言)

### 4.2 架构优势分析

| 对比项 | 当前架构(H2+PG) | 统一PostgreSQL |
|--------|----------------|----------------|
| **数据库实例数** | 2个 (H2内存+PG) | 1个 (PG) |
| **驱动依赖** | h2.jar + postgresql.jar | postgresql.jar |
| **配置复杂度** | 高 (双配置) | 低 (单配置) |
| **数据持久化** | ❌ H2重启丢失 | ✅ PG持久化 |
| **生产一致性** | ❌ 开发H2,生产PG | ✅ 开发=生产 |
| **SQL兼容性** | ⚠️ 需测试兼容 | ✅ 完全一致 |
| **性能** | H2快但功能弱 | PG强大且稳定 |
| **开发便捷性** | ✅ H2零配置 | ⚠️ 需安装PG |
| **运维成本** | 高 (双数据库) | 低 (单数据库) |

### 4.3 数据隔离策略

**推荐方案**: 使用**不同数据库名**实现逻辑隔离

```
PostgreSQL服务器 (localhost:5432)
├── metrics_hub (Hub模块专用)
│   ├── server_metrics_entity
│   ├── metrics_rollup_5m
│   └── metrics_rollup_1h
│
└── internalpaas (主应用专用)
    ├── users
    ├── servers
    ├── applications
    ├── ssh_sessions
    └── ... (17个实体表)
```

**好处**:
- ✅ 逻辑隔离,互不干扰
- ✅ 权限可分离管理
- ✅ 备份恢复可独立进行
- ✅ 未来可拆分到不同PG实例

---

## 5. 优化建议方案

### 🎯 推荐方案: 渐进式统一到PostgreSQL

#### 阶段1: 立即执行 (1-2小时)

**目标**: 清理混乱状态,明确H2定位

##### 步骤1.1: 添加PostgreSQL依赖
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- H2改为仅测试环境 -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>  <!-- 仅测试使用 -->
</dependency>
```

##### 步骤1.2: 创建Profile配置

**application.properties** (开发环境默认H2)
```properties
# ========================================
# 数据库配置 (开发环境 - H2内存数据库)
# ========================================
# 说明: 默认使用H2快速开发,生产环境使用PostgreSQL
# 切换生产环境: --spring.profiles.active=prod

spring.datasource.url=jdbc:h2:mem:internalpaas
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# H2控制台 (开发调试)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

**application-prod.properties** (生产环境PostgreSQL)
```properties
# ========================================
# 数据库配置 (生产环境 - PostgreSQL)
# ========================================
# 与Hub模块共享PostgreSQL服务器,但使用独立数据库

spring.datasource.url=jdbc:postgresql://localhost:5432/internalpaas
spring.datasource.username=${DB_USERNAME:internalpaas_user}
spring.datasource.password=${DB_PASSWORD:changeme}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000

spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate  # 生产使用Flyway
spring.jpa.show-sql=false

# Flyway数据库迁移
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

##### 步骤1.3: 删除废弃的ServerMetrics相关代码

**待删除文件列表**:
1. `src/main/java/com/cmict/internalpaas/model/ServerMetrics.java` (已废弃实体)
2. `src/main/java/com/cmict/internalpaas/repository/ServerMetricsRepository.java` (已废弃仓库)

**理由**:
- 监控数据已完全迁移到Hub
- 保留代码会导致H2创建无用表
- 避免开发者误用

**操作**:
```bash
# 删除前确认没有残留引用
git rm src/main/java/com/cmict/internalpaas/model/ServerMetrics.java
git rm src/main/java/com/cmict/internalpaas/repository/ServerMetricsRepository.java
git commit -m "refactor(phase4): remove deprecated ServerMetrics entity and repository"
```

##### 步骤1.4: 更新文档

**文件**: `doc/阶段4-数据存储迁移实施方案.md`

添加章节:
```markdown
## 9. 数据库统一策略

### 9.1 当前状态 (2025-10-17)
- ✅ 监控数据: Hub模块 (PostgreSQL/TimescaleDB)
- 🔄 业务数据: 主应用 (H2开发环境 / PostgreSQL生产环境)

### 9.2 环境配置
| 环境 | 数据库 | 配置方式 |
|------|--------|---------|
| 开发/测试 | H2 (内存) | 默认 `application.properties` |
| 生产 | PostgreSQL | `--spring.profiles.active=prod` |

### 9.3 PostgreSQL安装指南
见: `doc/postgresql-setup-guide.md`
```

#### 阶段2: 中期执行 (1-2天)

**目标**: 引入Flyway,支持生产级部署

##### 步骤2.1: 添加Flyway依赖
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

##### 步骤2.2: 创建初始化迁移脚本

**文件**: `src/main/resources/db/migration/V1__init_schema.sql`

```sql
-- ============================================================
-- InternalPaaS 主应用数据库初始化脚本
-- 版本: V1
-- 创建时间: 2025-10-17
-- 说明: 从H2自动建表迁移到Flyway版本管理
-- ============================================================

-- 用户表
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    department VARCHAR(255),
    phone VARCHAR(255),
    avatar_url VARCHAR(255),
    work_directory VARCHAR(255) NOT NULL,
    is_first_login BOOLEAN NOT NULL DEFAULT true,
    is_email_verified BOOLEAN DEFAULT false,
    is_account_locked BOOLEAN DEFAULT false,
    failed_login_attempts INTEGER DEFAULT 0,
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(255),
    login_count INTEGER DEFAULT 0,
    default_server_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 服务器表
CREATE TABLE servers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    hostname VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    description VARCHAR(255),
    server_type VARCHAR(255),
    base_work_directory VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    connection_status VARCHAR(255),
    privilege_level VARCHAR(255),
    privilege_check_details TEXT,
    auto_monitor_enabled BOOLEAN DEFAULT true,
    monitor_interval_seconds INTEGER DEFAULT 60,
    ssh_username VARCHAR(255),
    ssh_password VARCHAR(255),
    ssh_port INTEGER,
    ssh_key_path VARCHAR(255),
    ssh_key_passphrase VARCHAR(255),
    default_user_group_id BIGINT,
    last_connection_check TIMESTAMP,
    last_privilege_check TIMESTAMP,
    last_metrics_update TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ... (其他表省略,参考Hibernate自动建表的SQL)

-- 创建索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_servers_hostname ON servers(hostname);
CREATE INDEX idx_servers_active ON servers(active);
-- ...

-- 初始化默认数据
INSERT INTO users (username, password, email, full_name, work_directory, is_first_login)
VALUES ('admin', '$2a$10$...', 'admin@example.com', 'Administrator', '/tmp', false);
```

##### 步骤2.3: 配置生产环境启动脚本

**文件**: `scripts/start-prod.sh`
```bash
#!/bin/bash
# 生产环境启动脚本

# 检查PostgreSQL连接
echo "Checking PostgreSQL connection..."
pg_isready -h localhost -p 5432 -U internalpaas_user

if [ $? -ne 0 ]; then
    echo "ERROR: PostgreSQL is not ready"
    exit 1
fi

# 启动应用 (使用生产Profile)
echo "Starting InternalPaaS with PostgreSQL..."
java -jar target/internalpaas-0.0.1-SNAPSHOT.jar \
    --spring.profiles.active=prod \
    --server.port=9090
```

#### 阶段3: 长期优化 (按需执行)

**目标**: 完全统一到PostgreSQL,移除H2

##### 可选1: Docker Compose一键启动
```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: timescale/timescaledb:latest-pg14
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/01-init.sql

  internalpaas:
    build: .
    depends_on:
      - postgres
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_USERNAME: internalpaas_user
      DB_PASSWORD: strong_password
    ports:
      - "9090:9090"

  metrics-hub:
    build: ./hub
    depends_on:
      - postgres
    environment:
      DB_USERNAME: hub_user
      DB_PASSWORD: strong_password
    ports:
      - "8080:8080"
      - "4317:4317"
      - "4318:4318"

volumes:
  postgres-data:
```

##### 可选2: 开发环境也使用PostgreSQL
- 使用Testcontainers在单元测试时自动启动PostgreSQL容器
- 本地开发使用Docker Compose统一环境

---

## 6. 决策建议

### 6.1 立即执行 (必须)

✅ **1. 清理ServerMetrics废弃代码**
- **优先级**: 🔴 高
- **时间**: 30分钟
- **理由**: 避免混淆,减少H2表创建

✅ **2. 明确H2定位为"开发快速启动工具"**
- **优先级**: 🔴 高
- **时间**: 1小时
- **理由**: 统一认知,更新文档

✅ **3. 添加PostgreSQL作为生产数据库选项**
- **优先级**: 🔴 高
- **时间**: 2小时
- **理由**: 支持生产部署,与Hub统一

### 6.2 中期执行 (建议)

🟡 **4. 引入Flyway数据库版本管理**
- **优先级**: 🟡 中
- **时间**: 1-2天
- **理由**: 生产级schema管理

🟡 **5. 编写生产部署指南**
- **优先级**: 🟡 中
- **时间**: 4小时
- **理由**: 标准化运维流程

### 6.3 长期优化 (可选)

🟢 **6. 完全统一到PostgreSQL**
- **优先级**: 🟢 低
- **时间**: 1周
- **理由**: 架构一致性最优解

🟢 **7. Docker化部署方案**
- **优先级**: 🟢 低
- **时间**: 2-3天
- **理由**: 简化环境搭建

---

## 7. 实施时间表

```
Week 1:
├── Day 1-2: 清理ServerMetrics + 添加PostgreSQL支持 ✅ 必须
├── Day 3-4: 引入Flyway + 迁移脚本 🟡 建议
└── Day 5: 测试验证 + 文档更新 ✅ 必须

Week 2-4:
└── 可选: Docker化 + 完全统一PostgreSQL 🟢 可选
```

---

## 8. 风险评估

| 风险 | 等级 | 影响 | 应对措施 |
|------|------|------|---------|
| PostgreSQL配置复杂 | 🟡 中 | 开发环境搭建困难 | 保留H2开发环境 + Docker方案 |
| SQL兼容性问题 | 🟢 低 | JPA生成的SQL可能不兼容 | Profile隔离 + 充分测试 |
| 数据迁移失败 | 🟢 低 | H2数据无法迁移到PG | H2为内存库,数据可重建 |
| 运维成本增加 | 🟡 中 | 需要维护PostgreSQL | 统一技术栈,长期降低成本 |

---

## 9. 总结

### 当前问题
- ❌ H2和PostgreSQL并存,架构分裂
- ❌ ServerMetrics废弃但代码未删除
- ❌ 生产环境数据库策略不明确

### 优化方向
- ✅ **短期**: 明确H2为开发工具,PostgreSQL为生产方案
- ✅ **中期**: 引入Flyway版本管理,标准化部署
- ✅ **长期**: 统一到PostgreSQL,简化架构

### 核心收益
1. **架构统一**: 主应用与Hub共享PostgreSQL技术栈
2. **运维简化**: 单一数据库方案,降低复杂度
3. **生产就绪**: 数据持久化,支持大规模部署
4. **开发友好**: Profile隔离,H2快速启动

---

**建议下一步行动**:
1. 执行"阶段1: 立即执行"的4个步骤 (预计2-3小时)
2. 验证主应用在H2和PostgreSQL两种模式下均可正常启动
3. 更新部署文档,说明环境切换方法
4. 团队review此方案,确定中长期执行计划

**责任人**: 架构组  
**审核人**: 技术负责人  
**预期完成时间**: 2025-10-20
