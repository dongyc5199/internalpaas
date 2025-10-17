# Dev Debug Platform 部署文档

## 📋 目录
- [环境要求](#环境要求)
- [安装步骤](#安装步骤)
- [配置说明](#配置说明)
- [启动应用](#启动应用)
- [功能验证](#功能验证)
- [常见问题](#常见问题)
- [性能优化](#性能优化)
- [运维监控](#运维监控)

## 🎯 环境要求

### 基础环境
- **Java**: JDK 17 或更高版本
- **Maven**: 3.6.0 或更高版本
- **操作系统**: Windows 10/11, macOS 10.15+, Linux (Ubuntu 18.04+/CentOS 7+)
- **内存**: 最小 2GB RAM，推荐 4GB+ RAM
- **磁盘空间**: 最小 1GB 可用空间

### 网络要求
- **端口**: 默认使用 8080 端口（可配置）
- **网络**: 支持SSH连接到目标服务器（如需使用SSH功能）

### 浏览器要求
- **Chrome**: 90+ 版本
- **Firefox**: 88+ 版本
- **Safari**: 14+ 版本
- **Edge**: 90+ 版本

## 🚀 安装步骤

### 1. 环境准备

#### 1.1 安装Java JDK 17
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel

# macOS (使用 Homebrew)
brew install openjdk@17

# Windows
# 下载并安装 OpenJDK 17 from https://adoptium.net/
```

#### 1.2 验证Java安装
```bash
java -version
javac -version
```

#### 1.3 安装Maven
```bash
# Ubuntu/Debian
sudo apt install maven

# CentOS/RHEL
sudo yum install maven

# macOS (使用 Homebrew)
brew install maven

# Windows
# 下载并配置 Maven from https://maven.apache.org/
```

### 2. 源码获取

```bash
# 克隆仓库
git clone <repository-url>
cd internalpaas

# 或者下载源码包并解压
wget <source-package-url>
unzip internalpaas-source.zip
cd internalpaas
```

### 3. 构建应用

```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 打包应用
mvn clean package -DskipTests

# 验证构建结果
ls -la target/internalpaas-*.jar
```

## ⚙️ 配置说明

### 1. 数据库模式选择 ⭐ (重要)

平台提供三种数据库部署模式,请根据实际场景选择:

#### 📊 模式对比表

| 模式 | Profile | 数据持久化 | 外部依赖 | 适用场景 | 推荐度 |
|------|---------|-----------|---------|---------|--------|
| **H2内存模式** | 默认(无) | ❌ 重启丢失 | ✅ 零依赖 | 开发/Demo/快速体验 | ⭐⭐⭐⭐⭐ |
| **H2文件模式** | persistent | ✅ 文件存储 | ✅ 零依赖 | **小团队生产**(5-20人) | ⭐⭐⭐⭐⭐ |
| **PostgreSQL** | enterprise | ✅ 数据库 | ❌ 需要PG | 大型团队(50+人) | ⭐⭐⭐ |

#### 1️⃣ 模式1: H2内存模式 (默认 - 开发/Demo)

**特点**:
- ✅ 零配置,开箱即用
- ✅ 快速启动 (< 5秒)
- ✅ 自动清理,无残留
- ❌ 重启后数据丢失

**启动命令**:
```bash
# Maven方式
./mvnw.cmd spring-boot:run

# JAR方式
java -jar target/internalpaas-0.0.1-SNAPSHOT.jar
```

**适用场景**: 开发调试、功能演示、快速体验

---

#### 2️⃣ 模式2: H2文件持久化 (推荐 - 小团队生产)

**特点**:
- ✅ 数据持久化,重启保留
- ✅ 零外部依赖 (轻量级)
- ✅ 支持备份恢复
- ✅ 适合 5-20人团队
- ⚠️ 单机部署,数据量 < 10GB

**启动命令**:
```bash
# Maven方式
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=persistent

# JAR方式
java -jar -Dspring.profiles.active=persistent target/internalpaas-0.0.1-SNAPSHOT.jar

# 环境变量方式 (推荐)
set SPRING_PROFILES_ACTIVE=persistent
java -jar target/internalpaas-0.0.1-SNAPSHOT.jar
```

**数据文件位置**:
```
./data/internalpaas.mv.db    # 主数据文件
./data/internalpaas.trace.db # 追踪文件(仅错误时生成)
```

**备份与恢复**:
```bash
# 备份数据
tar -czf backup-$(date +%Y%m%d).tar.gz data/

# 恢复数据 (停止应用后)
tar -xzf backup-20251017.tar.gz
```

**适用场景**: 小团队生产环境、需要数据持久化、保持轻量级

---

#### 3️⃣ 模式3: PostgreSQL (可选 - 大型团队)

**特点**:
- ✅ 支持大数据量 (TB级)
- ✅ 高可用/主从复制
- ✅ 完整的企业特性
- ❌ 需要外部PostgreSQL数据库
- ❌ 部署复杂度增加

**前置条件**:
```bash
# 1. 安装PostgreSQL 12+
sudo apt install postgresql-12

# 2. 创建数据库
sudo -u postgres psql
CREATE DATABASE internalpaas;
CREATE USER internalpaas_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE internalpaas TO internalpaas_user;
\q

# 3. 配置环境变量
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=internalpaas
export DB_USER=internalpaas_user
export DB_PASSWORD=your_password
```

**启动命令**:
```bash
# 设置Profile和数据库连接
java -jar -Dspring.profiles.active=enterprise \
     -Ddb.host=localhost \
     -Ddb.port=5432 \
     target/internalpaas-0.0.1-SNAPSHOT.jar
```

**适用场景**: 大型团队(50+人)、高可用需求、数据量 > 10GB

---

### 2. 应用配置文件

#### 2.1 开发环境配置 (`application-dev.yml`)
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:./data/devplatformdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

logging:
  level:
    com.cmict.internalpaas: DEBUG
    org.springframework.security: INFO

app:
  port:
    range:
      start: 8000
      end: 9000
  debug:
    port:
      range:
        start: 5000
        end: 5999
```

#### 1.2 生产环境配置 (`application-prod.yml`)
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:/opt/devplatform/data/platformdb
    driver-class-name: org.h2.Driver
    username: sa
    password: ${DB_PASSWORD:}
  
  h2:
    console:
      enabled: false

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

logging:
  level:
    com.cmict.internalpaas: INFO
    root: WARN
  file:
    name: /opt/devplatform/logs/application.log

app:
  port:
    range:
      start: 8000
      end: 9000
  debug:
    port:
      range:
        start: 5000
        end: 5999
```

### 2. 环境变量配置

创建 `.env` 文件或设置系统环境变量：

```bash
# 数据库配置
DB_PASSWORD=your_secure_password

# 应用配置
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# JVM配置
JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"

# 日志配置
LOG_LEVEL=INFO
LOG_PATH=/opt/devplatform/logs
```

### 3. 系统配置

#### 3.1 创建应用目录
```bash
# Linux/macOS
sudo mkdir -p /opt/devplatform/{data,logs,uploads,workspaces}
sudo chown -R $USER:$USER /opt/devplatform

# Windows
mkdir C:\devplatform\data
mkdir C:\devplatform\logs
mkdir C:\devplatform\uploads
mkdir C:\devplatform\workspaces
```

#### 3.2 配置文件权限
```bash
# 确保配置文件安全
chmod 600 application-prod.yml
chmod 600 .env
```

## 🔧 启动应用

### 1. 快速启动 (H2内存模式 - 默认)

```bash
# Maven方式 (开发推荐)
./mvnw.cmd spring-boot:run

# JAR方式
mvn clean package -DskipTests
java -jar target/internalpaas-0.0.1-SNAPSHOT.jar
```

访问: http://localhost:9090

---

### 2. 生产模式启动 (H2文件持久化 - 推荐)

```bash
# 方式1: 启动参数
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=persistent

# 方式2: 环境变量 (推荐)
set SPRING_PROFILES_ACTIVE=persistent
java -jar target/internalpaas-0.0.1-SNAPSHOT.jar

# 方式3: JAR启动参数
java -jar -Dspring.profiles.active=persistent target/internalpaas-0.0.1-SNAPSHOT.jar
```

**首次启动检查**:
```bash
# 确认数据目录创建
ls -la data/

# 应该看到:
# data/internalpaas.mv.db (数据文件)
```

---

### 3. 企业级启动 (PostgreSQL)

```bash
# 1. 设置环境变量
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=internalpaas
export DB_USER=internalpaas_user
export DB_PASSWORD=your_secure_password
export SPRING_PROFILES_ACTIVE=enterprise

# 2. 启动应用
java -jar target/internalpaas-0.0.1-SNAPSHOT.jar
```

---

### 4. 后台运行 (Linux/macOS)

#### 使用systemd (推荐)

创建服务文件 `/etc/systemd/system/internalpaas.service`:

```ini
[Unit]
Description=Internal PaaS Platform
After=network.target

[Service]
Type=simple
User=internalpaas
WorkingDirectory=/opt/internalpaas
Environment="SPRING_PROFILES_ACTIVE=persistent"
ExecStart=/usr/bin/java -jar -Xms512m -Xmx2g /opt/internalpaas/internalpaas.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务:
```bash
sudo systemctl daemon-reload
sudo systemctl enable internalpaas
sudo systemctl start internalpaas
sudo systemctl status internalpaas
```

#### 使用nohup

```bash
# 后台启动
nohup java -jar -Dspring.profiles.active=persistent \
     target/internalpaas-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# 查看日志
tail -f app.log
```

---

### 5. Windows服务部署

使用 [WinSW](https://github.com/winsw/winsw) 将应用注册为Windows服务:

1. 下载 `WinSW.exe`
2. 创建 `internalpaas-service.xml`:

```xml
<service>
  <id>internalpaas</id>
  <name>Internal PaaS Platform</name>
  <description>Lightweight debug platform</description>
  <executable>java</executable>
  <arguments>-jar -Dspring.profiles.active=persistent internalpaas.jar</arguments>
  <workingdirectory>C:\internalpaas</workingdirectory>
  <log mode="roll"></log>
</service>
```

3. 安装并启动服务:
```cmd
WinSW.exe install internalpaas-service.xml
WinSW.exe start
```

---

### 6. Docker部署 (可选)

```bash
# 构建镜像
docker build -t internalpaas:latest .

# H2内存模式
docker run -d -p 9090:9090 --name internalpaas internalpaas:latest

# H2文件持久化模式 (挂载数据卷)
docker run -d -p 9090:9090 \
  -v $(pwd)/data:/app/data \
  -e SPRING_PROFILES_ACTIVE=persistent \
  --name internalpaas internalpaas:latest
```

---

### 7. 开发模式启动 (旧版)

```bash
# 使用Maven插件启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或者使用java命令
java -jar target/internalpaas-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 2. 生产模式启动

#### 2.1 直接启动
```bash
java -jar \
  -Xms512m -Xmx2g \
  -XX:+UseG1GC \
  -Dspring.profiles.active=prod \
  target/internalpaas-0.0.1-SNAPSHOT.jar
```

#### 2.2 使用脚本启动
创建 `start.sh` 脚本：
```bash
#!/bin/bash

APP_HOME="/opt/devplatform"
APP_JAR="$APP_HOME/internalpaas.jar"
PID_FILE="$APP_HOME/app.pid"
LOG_FILE="$APP_HOME/logs/startup.log"

JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -Dspring.profiles.active=prod"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "应用已在运行 (PID: $PID)"
        exit 1
    fi
fi

echo "启动 Dev Debug Platform..."
nohup java $JAVA_OPTS -jar "$APP_JAR" > "$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"

echo "应用启动完成，PID: $(cat $PID_FILE)"
echo "日志文件: $LOG_FILE"
```

#### 2.3 使用systemd服务（Linux）
创建 `/etc/systemd/system/devplatform.service`：
```ini
[Unit]
Description=Dev Debug Platform
After=network.target

[Service]
Type=simple
User=devplatform
Group=devplatform
ExecStart=/usr/bin/java -Xms512m -Xmx2g -XX:+UseG1GC -Dspring.profiles.active=prod -jar /opt/devplatform/internalpaas.jar
Restart=always
RestartSec=10
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=devplatform

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
sudo systemctl daemon-reload
sudo systemctl enable devplatform
sudo systemctl start devplatform
sudo systemctl status devplatform
```

## ✅ 功能验证

### 1. 基础功能验证

#### 1.1 访问应用
```bash
# 检查应用是否启动
curl -I http://localhost:8080

# 预期响应: HTTP/1.1 302
```

#### 1.2 登录验证
1. 访问 `http://localhost:8080`
2. 使用默认超级管理员账号登录：
   - 用户名: `admin`
   - 密码: `admin123`

#### 1.3 数据库验证
1. 访问 H2 控制台: `http://localhost:8080/h2-console`
2. 连接参数:
   - JDBC URL: `jdbc:h2:file:./data/devplatformdb`
   - 用户名: `sa`
   - 密码: (空)

### 2. 核心功能验证

#### 2.1 应用管理功能
1. 上传JAR文件
2. 启动/停止应用
3. 查看应用日志
4. 删除应用

#### 2.2 WebSocket功能
1. 实时日志推送
2. 应用状态更新
3. 服务器监控数据

#### 2.3 用户管理功能
1. 创建普通用户
2. 权限控制验证
3. 会话管理

## 🔧 常见问题

### 1. 启动问题

#### Q: 端口占用错误
```bash
Error: Port 8080 was already in use.
```
**解决方案:**
```bash
# 查找占用端口的进程
netstat -tulpn | grep 8080
# 或者
lsof -i :8080

# 杀死进程或更改端口
# 方法1: 杀死进程
kill -9 <PID>

# 方法2: 更改端口
java -jar app.jar --server.port=8081
```

#### Q: 内存不足错误
```bash
java.lang.OutOfMemoryError: Java heap space
```
**解决方案:**
```bash
# 增加堆内存
java -Xmx2g -jar app.jar

# 或者使用G1垃圾收集器
java -Xmx2g -XX:+UseG1GC -jar app.jar
```

### 2. 数据库问题

#### Q: 数据库文件权限错误
```bash
Caused by: org.h2.jdbc.JdbcSQLException: General error
```
**解决方案:**
```bash
# 检查并修复权限
chmod 755 data/
chmod 644 data/devplatformdb.mv.db

# 确保目录存在
mkdir -p data/
```

#### Q: 数据库连接失败
**解决方案:**
1. 检查数据库文件路径
2. 验证磁盘空间
3. 检查文件权限
4. 查看应用日志

### 3. SSH连接问题

#### Q: SSH认证失败
```bash
com.jcraft.jsch.JSchException: Auth fail
```
**解决方案:**
1. 验证SSH凭据
2. 检查目标服务器SSH服务状态
3. 确认网络连通性
4. 检查防火墙设置

#### Q: SSH连接超时
```bash
java.net.SocketTimeoutException: Read timed out
```
**解决方案:**
1. 增加连接超时时间
2. 检查网络延迟
3. 验证服务器负载

### 4. WebSocket问题

#### Q: WebSocket连接失败
**解决方案:**
1. 检查浏览器WebSocket支持
2. 验证网络代理设置
3. 检查防火墙配置
4. 查看浏览器控制台错误

## 🚀 性能优化

### 1. JVM参数优化

```bash
# 生产环境推荐配置
JAVA_OPTS="
  -Xms1g -Xmx4g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+UnlockExperimentalVMOptions
  -XX:+UseStringDeduplication
  -XX:+PrintGC
  -XX:+PrintGCDetails
  -XX:+PrintGCTimeStamps
  -Xloggc:/opt/devplatform/logs/gc.log
"
```

### 2. 应用配置优化

```yaml
# application-prod.yml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
        batch_versioned_data: true
    show-sql: false

server:
  tomcat:
    max-threads: 200
    min-spare-threads: 10
    max-connections: 8192

logging:
  level:
    org.springframework: WARN
    org.hibernate: WARN
```

### 3. 系统级优化

```bash
# 增加文件描述符限制
echo "* soft nofile 65536" >> /etc/security/limits.conf
echo "* hard nofile 65536" >> /etc/security/limits.conf

# 优化网络参数
echo "net.core.somaxconn = 32768" >> /etc/sysctl.conf
echo "net.core.netdev_max_backlog = 32768" >> /etc/sysctl.conf
sysctl -p
```

## 📊 运维监控

### 1. 日志监控

#### 1.1 应用日志位置
- 开发环境: `./logs/`
- 生产环境: `/opt/devplatform/logs/`

#### 1.2 重要日志文件
- `application.log`: 应用主日志
- `gc.log`: GC日志
- `startup.log`: 启动日志

#### 1.3 日志轮换配置
```yaml
# logback-spring.xml
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/opt/devplatform/logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/opt/devplatform/logs/application.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
</configuration>
```

### 2. 性能监控

#### 2.1 JVM监控
```bash
# 监控JVM状态
jstat -gc <PID> 5s

# 内存使用情况
jmap -histo <PID>

# 线程状态
jstack <PID>
```

#### 2.2 应用监控
```bash
# 检查应用状态
curl http://localhost:8080/actuator/health

# 查看应用指标
curl http://localhost:8080/actuator/metrics
```

### 3. 健康检查

#### 3.1 创建健康检查脚本
```bash
#!/bin/bash
# health-check.sh

HEALTH_URL="http://localhost:8080/actuator/health"
MAX_RETRIES=3
RETRY_INTERVAL=5

for i in $(seq 1 $MAX_RETRIES); do
    if curl -f -s "$HEALTH_URL" > /dev/null; then
        echo "应用健康检查通过"
        exit 0
    fi
    
    echo "健康检查失败，重试 $i/$MAX_RETRIES"
    sleep $RETRY_INTERVAL
done

echo "应用健康检查失败"
exit 1
```

#### 3.2 定期监控
```bash
# 添加到crontab
*/5 * * * * /opt/devplatform/scripts/health-check.sh
```

## 📞 技术支持

如遇到问题，请按以下顺序排查：

1. **查看应用日志**: 检查 `logs/application.log`
2. **检查系统资源**: CPU、内存、磁盘使用情况
3. **验证网络连接**: 端口访问、防火墙设置
4. **参考本文档**: 常见问题解决方案

**联系方式:**
- 邮箱: support@devdebug.com
- 热线: 400-123-4567
- 文档: https://docs.devdebug.com

---
*最后更新: 2025-08-24*