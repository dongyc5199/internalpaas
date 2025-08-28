# Dev Debug Platform - Claude AI 开发助手指南

## 项目概述

**Dev Debug Platform** 是一个轻量级的后端调试Web平台，专为后端开发团队设计，旨在简化在共享开发服务器上的部署与调试流程。项目使用Spring Boot 3.x构建，提供可视化的应用生命周期管理、实时监控和调试功能。

### 核心功能
- 👤 **用户与权限管理**: 独立用户系统，个性化工作区
- 🚀 **应用生命周期管理**: 一键上传、启动、停止、重启JAR应用
- 📊 **状态与日志监控**: 实时状态面板，WebSocket实时日志流
- 🛠️ **调试友好**: 自动远程调试端口分配，SSH隧道支持
- 🔒 **安全管理**: Spring Security认证授权，访问控制

## 技术栈

### 后端技术
- **框架**: Spring Boot 3.x
- **Web/API**: Spring Web, Spring WebSocket
- **安全**: Spring Security 
- **数据库**: Spring Data JPA + H2/SQLite
- **模板引擎**: Thymeleaf
- **构建工具**: Maven

### 关键依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

## 项目结构

```
src/main/java/com/cmict/internalpaas/
├── InternalpaasApplication.java          # 主应用入口
├── config/                               # 配置类
│   ├── DataInitializer.java             # 数据初始化
│   ├── GlobalExceptionHandler.java      # 全局异常处理
│   ├── SecurityConfig.java              # 安全配置
│   ├── SecurityWebSocketHandshakeInterceptor.java
│   └── WebSocketConfig.java             # WebSocket配置
├── controller/                           # 控制器层
│   ├── AdminController.java             # 管理员功能
│   ├── AdminDashboardController.java    # 管理员仪表板
│   ├── ApplicationController.java       # 应用管理
│   ├── DeveloperDashboardController.java # 开发者仪表板
│   ├── MonitoringController.java        # 监控功能
│   ├── SSHTerminalController.java       # SSH终端
│   ├── SSHTerminalWebSocketHandler.java # SSH WebSocket处理
│   ├── UserController.java              # 用户管理
│   └── ...其他控制器
├── dto/                                  # 数据传输对象
├── model/                               # 实体类
│   ├── Application.java                # 应用实体
│   ├── Server.java                     # 服务器实体
│   ├── ServerMetrics.java              # 监控指标
│   ├── SSHSession.java                 # SSH会话
│   ├── User.java                       # 用户实体
│   └── UserActivity.java               # 用户活动
├── repository/                          # 数据访问层
├── service/                            # 业务逻辑层
│   ├── ApplicationService.java         # 应用服务
│   ├── MonitoringService.java          # 监控服务
│   ├── SSHTerminalService.java         # SSH终端服务
│   ├── ServerService.java              # 服务器服务
│   ├── UserService.java                # 用户服务
│   └── ...其他服务类
└── test/                               # 测试工具
```

```
src/main/resources/
├── application.properties              # 主配置文件
├── static/                            # 静态资源
│   ├── css/                          # 样式文件
│   │   ├── common-dashboard.css      # 通用仪表板样式
│   │   ├── dashboard-theme.css       # 主题样式
│   │   └── ...其他样式文件
│   └── js/                           # JavaScript文件
│       ├── dashboard.js              # 仪表板脚本
│       ├── theme.js                  # 主题切换
│       └── ...其他脚本文件
└── templates/                         # Thymeleaf模板
    ├── admin-dashboard.html          # 管理员仪表板
    ├── developer-dashboard.html      # 开发者仪表板
    ├── admin/                        # 管理员页面
    ├── monitoring/                   # 监控页面
    └── terminal/                     # 终端页面
```

```
doc/                                   # 项目文档目录
├── README.md                         # 文档目录索引
├── project-overview.md               # 项目概述
├── deployment-guide.md               # 部署指南
├── operations-manual.md              # 运维手册
└── performance-optimization.md       # 性能优化指南
```

## 核心模块说明

### 1. 应用管理模块 (Application Management)
**位置**: `ApplicationController.java`, `ApplicationService.java`
- **功能**: JAR应用的上传、启动、停止、重启、删除
- **关键方法**:
  - `uploadApplication()`: 应用上传
  - `startApplication()`: 启动应用
  - `stopApplication()`: 停止应用
  - `getApplicationStatus()`: 获取应用状态

### 2. 用户管理模块 (User Management)
**位置**: `UserController.java`, `UserService.java`
- **功能**: 用户注册、登录、权限控制、会话管理
- **权限级别**: 
  - ADMIN: 管理员权限
  - DEVELOPER: 开发者权限

### 3. 监控模块 (Monitoring)
**位置**: `MonitoringController.java`, `MonitoringService.java`
- **功能**: 服务器监控、应用状态监控、实时数据推送
- **监控指标**: CPU、内存、磁盘、网络、应用进程状态

### 4. SSH终端模块 (SSH Terminal)
**位置**: `SSHTerminalController.java`, `SSHTerminalService.java`
- **功能**: WebSocket SSH连接、远程命令执行、实时终端交互

### 5. WebSocket实时通信
**位置**: `WebSocketConfig.java`, 各种WebSocketHandler
- **功能**: 实时日志推送、状态更新、终端交互

## 数据库设计

### 核心表结构

#### users (用户表)
- `id`: 主键
- `username`: 用户名(唯一)
- `password`: 加密密码
- `email`: 邮箱
- `role`: 用户角色(ADMIN/DEVELOPER)
- `created_at`: 创建时间

#### servers (服务器表)
- `id`: 主键
- `name`: 服务器名称
- `host`: 服务器地址
- `port`: SSH端口
- `username`: SSH用户名
- `password`: SSH密码(加密)
- `status`: 服务器状态

#### applications (应用表)
- `id`: 主键
- `name`: 应用名称
- `file_path`: JAR文件路径
- `user_id`: 所属用户
- `server_id`: 所属服务器
- `status`: 应用状态
- `port`: 应用端口
- `debug_port`: 调试端口
- `created_at`: 创建时间

#### server_metrics (监控指标表)
- `id`: 主键
- `server_id`: 服务器ID
- `cpu_usage`: CPU使用率
- `memory_usage`: 内存使用率
- `disk_usage`: 磁盘使用率
- `timestamp`: 记录时间

## 配置说明

### 应用配置 (application.properties)
```properties
# 服务器配置
server.port=8080

# 数据库配置
spring.datasource.url=jdbc:h2:file:./data/devplatformdb
spring.datasource.username=sa
spring.datasource.password=password
spring.h2.console.enabled=true

# JPA配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# 端口配置
app.port.range.start=8000
app.port.range.end=9000
app.debug.port.range.start=5000
app.debug.port.range.end=5999
```

### 安全配置要点
- **认证**: 基于Session的认证机制
- **授权**: 基于角色的访问控制
- **CSRF**: 已启用CSRF保护
- **密码加密**: 使用BCrypt加密

## 开发指南

### 1. 环境要求
- **Java**: JDK 17或更高版本
- **Maven**: 3.6.0+
- **IDE**: IntelliJ IDEA/Eclipse (推荐IDEA)

### 2. 本地开发启动
```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 启动应用 (开发模式)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或者打包后运行
mvn clean package -DskipTests
java -jar target/internalpaas-*.jar --spring.profiles.active=dev
```

### 3. 开发注意事项
- **数据库**: 开发环境使用H2内存数据库，数据不持久化
- **端口**: 默认8080端口，确保端口未被占用
- **日志**: 开发环境日志级别为DEBUG
- **热重载**: 建议使用spring-boot-devtools实现热重载

### 4. 常用开发命令
```bash
# 查看应用状态
curl http://localhost:8080/actuator/health

# 访问H2控制台
http://localhost:8080/h2-console

# 查看API文档 (如果集成了Swagger)
http://localhost:8080/swagger-ui.html

# 测试构建
mvn clean package -DskipTests
```

### 5. 代码规范
- **命名**: 使用驼峰命名法，类名首字母大写
- **注释**: 所有公共方法必须有JavaDoc注释
- **异常**: 使用统一的异常处理机制
- **日志**: 使用SLF4J，合理设置日志级别
- **事务**: 需要事务的方法添加@Transactional注解

## 部署指南

### 1. 生产环境部署
```bash
# 构建生产包
mvn clean package -Pprod -DskipTests

# 启动应用
java -jar -Xms1g -Xmx2g -XX:+UseG1GC \
  -Dspring.profiles.active=prod \
  target/internalpaas-*.jar
```

### 2. Docker部署 (如果需要)
```dockerfile
FROM openjdk:17-jre-slim
COPY target/internalpaas-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

### 3. 系统服务配置 (systemd)
```ini
[Unit]
Description=Dev Debug Platform
After=network.target

[Service]
Type=simple
User=devplatform
Group=devplatform
ExecStart=/usr/bin/java -Xms1g -Xmx2g -jar /opt/devplatform/internalpaas.jar
Restart=always

[Install]
WantedBy=multi-user.target
```

## 运维指南

### 1. 日常检查脚本
```bash
#!/bin/bash
# 检查应用状态
curl -s http://localhost:8080/actuator/health

# 检查进程
ps aux | grep internalpaas

# 检查端口
netstat -tuln | grep 8080

# 检查日志
tail -f logs/application.log
```

### 2. 备份策略
- **数据库备份**: 每日备份H2数据库文件
- **配置备份**: 定期备份配置文件
- **日志备份**: 定期轮换和备份日志文件

### 3. 监控指标
- **应用健康**: `/actuator/health`端点
- **系统指标**: CPU、内存、磁盘使用率
- **业务指标**: 活跃用户数、应用运行数量
- **错误监控**: 错误日志、异常统计

## 性能优化指南

### 1. 数据库优化
- **索引优化**: 为频繁查询字段添加索引
- **连接池**: 优化HikariCP连接池配置
- **查询优化**: 避免N+1查询，使用批量查询

### 2. 缓存策略
- **应用缓存**: 使用Spring Cache缓存频繁查询数据
- **Redis缓存**: 可集成Redis实现分布式缓存
- **静态资源**: 配置静态资源缓存策略

### 3. JVM优化
```bash
# 推荐JVM参数
-Xms2g -Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
```

## 故障排除

### 1. 常见问题

#### 应用启动失败
- **检查端口占用**: `netstat -tuln | grep 8080`
- **检查Java版本**: `java -version`
- **查看启动日志**: `tail -f logs/application.log`

#### 数据库连接失败
- **检查数据库文件**: `ls -la data/`
- **检查文件权限**: `chmod 644 data/*.db`
- **清理临时文件**: `rm -f data/*.lock`

#### WebSocket连接失败
- **检查浏览器支持**: 确认浏览器支持WebSocket
- **检查网络代理**: 确认代理配置正确
- **查看控制台错误**: 检查浏览器开发者工具

### 2. 日志分析
```bash
# 查找错误日志
grep -i "error" logs/application.log

# 统计错误数量
grep -c "ERROR" logs/application.log

# 实时监控错误
tail -f logs/application.log | grep -i "error"
```

### 3. 性能分析
```bash
# 查看JVM状态
jstat -gc <PID>

# 内存使用分析
jmap -histo <PID>

# 线程状态分析
jstack <PID>
```

## API接口说明

### 1. 应用管理API
- `POST /api/applications/upload`: 上传应用
- `POST /api/applications/{id}/start`: 启动应用
- `POST /api/applications/{id}/stop`: 停止应用
- `GET /api/applications/{id}/status`: 获取应用状态
- `GET /api/applications/{id}/logs`: 获取应用日志

### 2. 用户管理API
- `POST /api/users/register`: 用户注册
- `POST /api/users/login`: 用户登录
- `GET /api/users/profile`: 获取用户信息
- `PUT /api/users/profile`: 更新用户信息

### 3. 监控API
- `GET /api/monitoring/servers`: 获取服务器列表
- `GET /api/monitoring/servers/{id}/metrics`: 获取服务器监控数据
- `GET /api/monitoring/applications/{id}/metrics`: 获取应用监控数据

## 安全注意事项

### 1. 敏感信息保护
- **密码加密**: 所有密码使用BCrypt加密存储
- **配置文件**: 生产环境配置文件权限设置为600
- **日志脱敏**: 确保日志中不包含敏感信息

### 2. 访问控制
- **角色权限**: 严格按角色分配权限
- **会话管理**: 配置合理的会话超时时间
- **HTTPS**: 生产环境建议启用HTTPS

### 3. 安全加固
- **防火墙**: 只开放必要端口
- **更新**: 定期更新依赖包和系统补丁
- **监控**: 启用安全日志监控和告警

## 扩展开发指南

### 1. 添加新功能模块
1. 创建对应的Entity、Repository、Service、Controller
2. 配置必要的安全策略
3. 添加前端页面和JavaScript
4. 编写单元测试和集成测试
5. 更新相关文档

### 2. 集成第三方服务
- **消息队列**: 可集成RabbitMQ/Kafka实现异步处理
- **监控系统**: 可集成Prometheus+Grafana实现监控
- **日志系统**: 可集成ELK Stack实现日志分析

### 3. 微服务化改造
- **服务拆分**: 按业务领域拆分独立服务
- **注册中心**: 使用Eureka/Consul实现服务发现
- **网关**: 使用Spring Cloud Gateway实现API网关
- **配置中心**: 使用Spring Cloud Config实现配置管理

---

## 联系信息

**开发团队**: CMICT Internal PaaS Team
**技术支持**: 查看项目README或提交Issue
**文档更新**: 2025-08-26

---

*此文档基于项目当前状态生成，随项目发展持续更新*