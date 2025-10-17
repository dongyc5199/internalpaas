# 轻量级平台 - 数据库优化执行清单

**执行日期**: 2025-10-17  
**预计耗时**: 2-3小时  
**执行人**: 开发团队  
**目标**: 保持H2轻量级定位,清理架构混乱

---

## 📋 执行概览

```
阶段1: 代码清理 (30分钟) ✅ 必须执行
阶段2: H2配置优化 (60分钟) ✅ 必须执行  
阶段3: 文档更新 (30分钟) ✅ 必须执行
阶段4: 验证测试 (30分钟) ✅ 必须执行
```

---

## 🎯 阶段1: 清理废弃代码 (30分钟)

### 步骤1.1: 删除ServerMetrics实体 ⏱️ 5分钟

**理由**: 监控数据已迁移到Hub,此实体已废弃

**操作**:
```bash
# 1. 确认文件存在
ls src/main/java/com/cmict/internalpaas/model/ServerMetrics.java

# 2. 删除实体类
git rm src/main/java/com/cmict/internalpaas/model/ServerMetrics.java

# 3. 提交
git commit -m "refactor(phase4): remove deprecated ServerMetrics entity"
```

**验证**:
- [ ] 文件已删除
- [ ] Git提交成功

---

### 步骤1.2: 删除ServerMetricsRepository ⏱️ 5分钟

**理由**: 实体已删除,仓库接口无用

**操作**:
```bash
# 1. 删除仓库接口
git rm src/main/java/com/cmict/internalpaas/repository/ServerMetricsRepository.java

# 2. 提交
git commit -m "refactor(phase4): remove deprecated ServerMetricsRepository"
```

**验证**:
- [ ] 文件已删除
- [ ] Git提交成功

---

### 步骤1.3: 检查残留引用 ⏱️ 10分钟

**操作**:
```bash
# 1. 全局搜索ServerMetrics引用
grep -r "ServerMetrics" src/main/java/ --include="*.java"

# 2. 全局搜索ServerMetricsRepository引用
grep -r "ServerMetricsRepository" src/main/java/ --include="*.java"
```

**预期结果**: 应该没有任何引用 (除了注释)

**如果有引用**:
- 检查是否为注释 (可保留)
- 如果是代码引用,需要删除或重构

**验证**:
- [ ] 无残留代码引用
- [ ] 仅注释中提及 (可接受)

---

### 步骤1.4: 编译验证 ⏱️ 10分钟

**操作**:
```bash
# 清理编译
./mvnw.cmd clean

# 重新编译
./mvnw.cmd compile

# 查看编译结果
echo $LASTEXITCODE  # 应该为 0
```

**验证**:
- [ ] 编译成功 (EXIT CODE = 0)
- [ ] 无错误信息
- [ ] 无警告信息

---

## 🔧 阶段2: H2配置优化 (60分钟)

### 步骤2.1: 优化默认H2配置 ⏱️ 20分钟

**目标**: 保持内存模式作为默认,优化性能

**文件**: `src/main/resources/application.properties`

**修改内容**:
```properties
# ========================================
# 数据库配置 - H2内存模式 (默认)
# ========================================
# 定位: 轻量级开发/测试环境
# 特点: 快速启动,零依赖,重启数据丢失
# 适用: 本地开发、Demo演示、CI/CD测试

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.datasource.url=jdbc:h2:mem:internalpaas;DB_CLOSE_DELAY=-1;MODE=MySQL
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# HikariCP连接池
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
```

**验证**:
- [ ] 文件修改完成
- [ ] 注释清晰说明用途

---

### 步骤2.2: 创建持久化模式配置 ⏱️ 20分钟

**目标**: 支持小团队生产环境

**文件**: `src/main/resources/application-persistent.properties`

**创建内容**:
```properties
# ========================================
# 数据库配置 - H2文件持久化模式
# ========================================
# 定位: 小团队生产环境 (< 50用户)
# 特点: 数据持久化,仍然零外部依赖
# 启动: java -jar app.jar --spring.profiles.active=persistent

# H2文件模式 + 性能优化
spring.datasource.url=jdbc:h2:file:./data/internalpaas;\
  AUTO_SERVER=TRUE;\
  DB_CLOSE_ON_EXIT=FALSE;\
  FILE_LOCK=FS;\
  CACHE_SIZE=65536;\
  PAGE_SIZE=8192;\
  LOCK_TIMEOUT=10000;\
  MODE=MySQL

spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# 生产环境JPA配置
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.open-in-view=false

# 连接池优化
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000

# H2控制台 (生产环境建议关闭)
spring.h2.console.enabled=false
```

**验证**:
- [ ] 文件创建成功
- [ ] 配置参数正确

---

### 步骤2.3: 创建PostgreSQL配置 (可选) ⏱️ 10分钟

**目标**: 为企业级用户提供迁移路径

**文件**: `src/main/resources/application-enterprise.properties`

**创建内容**:
```properties
# ========================================
# 数据库配置 - PostgreSQL企业级模式
# ========================================
# 定位: 大型团队/企业环境 (> 50用户)
# 特点: 专业数据库,需外部安装PostgreSQL
# 启动: java -jar app.jar --spring.profiles.active=enterprise

spring.datasource.url=jdbc:postgresql://localhost:5432/internalpaas
spring.datasource.username=${DB_USERNAME:internalpaas_user}
spring.datasource.password=${DB_PASSWORD:changeme}
spring.datasource.driver-class-name=org.postgresql.Driver

# PostgreSQL方言
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.open-in-view=false

# 连接池配置
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# Flyway数据库迁移 (推荐)
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
```

**验证**:
- [ ] 文件创建成功
- [ ] 配置参数正确

---

### 步骤2.4: 更新pom.xml依赖 ⏱️ 10分钟

**目标**: 明确H2为runtime依赖,PG为可选依赖

**文件**: `pom.xml`

**修改位置**: 约第104-115行

**修改前**:
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

**修改后**:
```xml
<!-- H2 Database - 轻量级内嵌数据库 (默认方案) -->
<!-- 定位: 开发/测试环境,小团队生产环境 -->
<!-- 优势: 零依赖,快速启动,开箱即用 -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- PostgreSQL - 企业级数据库 (可选方案) -->
<!-- 仅在大规模部署时使用,需外部安装PostgreSQL -->
<!-- 激活: --spring.profiles.active=enterprise -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>

<!-- Flyway - 数据库版本管理 (可选) -->
<!-- 建议在生产环境启用 -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <optional>true</optional>
</dependency>
```

**验证**:
- [ ] H2依赖保持runtime scope
- [ ] PostgreSQL添加为optional
- [ ] Flyway添加为optional

---

## 📝 阶段3: 文档更新 (30分钟)

### 步骤3.1: 更新项目概述文档 ⏱️ 10分钟

**文件**: `docs/architecture/overview.md`

**修改位置**: 第51-60行 (技术栈部分)

**修改前**:
```markdown
*   **数据库**: Spring Data JPA + H2 (内嵌数据库,易于部署) / SQLite
```

**修改后**:
```markdown
*   **数据库**: Spring Data JPA + H2 (默认) / PostgreSQL (可选)
    *   **H2内存模式** (默认): 零依赖,瞬时启动,适合开发/Demo
    *   **H2文件模式** (推荐): 数据持久化,适合小团队生产 (< 50用户)
    *   **PostgreSQL模式** (可选): 企业级方案,适合大规模部署
```

**验证**:
- [ ] 文档更新完成
- [ ] 说明清晰准确

---

### 步骤3.2: 创建数据库配置指南 ⏱️ 20分钟

**文件**: `doc/数据库配置指南.md`

**创建内容**:
```markdown
# 数据库配置指南

## 快速选择

| 场景 | 推荐模式 | 启动命令 | 数据持久化 |
|------|---------|---------|-----------|
| 本地开发 | H2内存 (默认) | `java -jar app.jar` | ❌ 重启丢失 |
| Demo演示 | H2内存 (默认) | `java -jar app.jar` | ❌ 重启丢失 |
| 小团队生产 | H2文件 | `java -jar app.jar --spring.profiles.active=persistent` | ✅ 持久化 |
| 大型企业 | PostgreSQL | `java -jar app.jar --spring.profiles.active=enterprise` | ✅ 持久化 |

## 模式1: H2内存模式 (默认)

### 适用场景
- 本地开发调试
- 快速Demo演示
- CI/CD自动化测试
- 临时测试环境

### 特点
- ⚡ 启动速度: < 5秒
- 💾 内存占用: ~200MB
- 📦 外部依赖: 无
- 🔄 数据持久化: 否 (重启丢失)

### 启动命令
```bash
java -jar internalpaas.jar
```

### 访问H2控制台
- URL: http://localhost:9090/h2-console
- JDBC URL: `jdbc:h2:mem:internalpaas`
- Username: `sa`
- Password: (留空)

---

## 模式2: H2文件模式 (推荐生产)

### 适用场景
- 小团队生产环境 (< 50用户)
- 数据需要持久化
- 无专职DBA团队
- 希望保持轻量级部署

### 特点
- ⚡ 启动速度: < 8秒
- 💾 内存占用: ~220MB
- 📦 外部依赖: 无
- 🔄 数据持久化: 是
- 📊 数据容量: 支持到10GB

### 启动命令
```bash
java -jar internalpaas.jar --spring.profiles.active=persistent
```

### 数据文件位置
- 路径: `./data/internalpaas.mv.db`
- 备份: 直接复制此文件即可

### 备份脚本
```bash
#!/bin/bash
# backup.sh
cp ./data/internalpaas.mv.db ./backup/internalpaas_$(date +%Y%m%d).mv.db
```

---

## 模式3: PostgreSQL模式 (企业级)

### 适用场景
- 大型团队 (> 50用户)
- 数据量 > 10GB
- 需要专业DBA支持
- 已有PostgreSQL基础设施

### 前置条件
1. 安装PostgreSQL 12+
2. 创建数据库和用户

```sql
CREATE DATABASE internalpaas;
CREATE USER internalpaas_user WITH PASSWORD 'strong_password';
GRANT ALL PRIVILEGES ON DATABASE internalpaas TO internalpaas_user;
```

### 启动命令
```bash
# 方式1: 命令行参数
java -jar internalpaas.jar \
  --spring.profiles.active=enterprise \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/internalpaas \
  --spring.datasource.username=internalpaas_user \
  --spring.datasource.password=strong_password

# 方式2: 环境变量
export DB_USERNAME=internalpaas_user
export DB_PASSWORD=strong_password
java -jar internalpaas.jar --spring.profiles.active=enterprise
```

---

## 模式切换与迁移

### H2内存 → H2文件

**无需迁移**: 直接使用persistent模式启动即可,数据从零开始

### H2文件 → PostgreSQL

**使用迁移工具**:
```bash
# 1. 导出H2数据
java -cp h2.jar org.h2.tools.Script \
  -url jdbc:h2:file:./data/internalpaas \
  -user sa \
  -script backup.sql

# 2. 转换SQL方言 (可能需要手动调整)
sed -i 's/AUTO_INCREMENT/SERIAL/g' backup.sql

# 3. 导入PostgreSQL
psql -U internalpaas_user -d internalpaas -f backup.sql
```

---

## 常见问题

### Q: 生产环境必须用PostgreSQL吗?
A: 不必须。对于< 50用户的小团队,H2文件模式完全够用。

### Q: H2文件模式性能如何?
A: 支持100并发读/50并发写,足够轻量级调试平台使用。

### Q: 数据库文件会无限增长吗?
A: 不会。可设置定期清理策略,如自动删除30天前的日志数据。

### Q: 能同时运行多个实例吗?
A: H2文件模式设置了AUTO_SERVER=TRUE,支持多实例只读访问。
```

**验证**:
- [ ] 文档创建完成
- [ ] 内容清晰准确
- [ ] 示例可执行

---

## ✅ 阶段4: 验证测试 (30分钟)

### 步骤4.1: 测试默认H2内存模式 ⏱️ 10分钟

**操作**:
```bash
# 1. 清理编译
./mvnw.cmd clean compile

# 2. 启动应用 (默认H2内存)
./mvnw.cmd spring-boot:run

# 3. 等待启动完成 (观察日志)
# 应该看到: "Started InternalpaasApplication in X seconds"

# 4. 浏览器访问
# http://localhost:9090
# http://localhost:9090/h2-console

# 5. 停止应用 (Ctrl+C)
```

**验证清单**:
- [ ] 应用成功启动 (< 10秒)
- [ ] 无错误日志
- [ ] 主页可访问
- [ ] H2控制台可访问
- [ ] 可注册新用户
- [ ] 可登录系统

---

### 步骤4.2: 测试H2文件持久化模式 ⏱️ 10分钟

**操作**:
```bash
# 1. 创建数据目录
mkdir -p data

# 2. 启动应用 (H2文件模式)
./mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=persistent"

# 3. 注册一个测试账号
# http://localhost:9090/register

# 4. 停止应用

# 5. 检查数据文件
ls -lh data/
# 应该看到: internalpaas.mv.db

# 6. 重新启动
./mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=persistent"

# 7. 验证数据保留
# 尝试登录之前注册的账号
```

**验证清单**:
- [ ] 应用成功启动
- [ ] 数据文件已创建 (data/internalpaas.mv.db)
- [ ] 重启后数据保留
- [ ] 可正常登录之前的账号

---

### 步骤4.3: 运行单元测试 ⏱️ 10分钟

**操作**:
```bash
# 运行所有测试
./mvnw.cmd test

# 查看测试结果
# 应该全部通过
```

**验证清单**:
- [ ] 所有测试通过
- [ ] 无测试失败
- [ ] 无编译错误

---

## 📊 执行完成检查表

### 代码层面
- [ ] ServerMetrics.java 已删除
- [ ] ServerMetricsRepository.java 已删除
- [ ] 无残留代码引用
- [ ] 编译成功无错误
- [ ] 单元测试全部通过

### 配置层面
- [ ] application.properties 优化完成
- [ ] application-persistent.properties 创建完成
- [ ] application-enterprise.properties 创建完成
- [ ] pom.xml 依赖配置正确

### 文档层面
- [ ] project-overview.md 更新完成
- [ ] 数据库配置指南.md 创建完成
- [ ] 注释清晰说明用途

### 功能验证
- [ ] H2内存模式启动成功
- [ ] H2文件模式启动成功
- [ ] 数据持久化验证通过
- [ ] 用户注册登录正常

---

## 🎉 完成标志

当以上所有检查项都打勾后,数据库优化工作完成!

**效果**:
- ✅ 清理了废弃代码
- ✅ 保持了轻量级定位
- ✅ 提供了三种部署模式
- ✅ 文档清晰完善

**下一步建议**:
1. 更新部署文档
2. 通知团队新的配置方式
3. 准备发布新版本

---

## 📞 遇到问题?

如果执行过程中遇到问题:

1. **编译失败**: 检查ServerMetrics引用是否完全删除
2. **启动失败**: 检查H2依赖是否正确配置
3. **数据丢失**: 确认使用了persistent模式
4. **性能问题**: 检查HikariCP连接池配置

**联系人**: 架构组
**文档**: `doc/轻量级平台数据库策略优化方案.md`
