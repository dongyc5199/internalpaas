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
│   ├── ApplicationShutdownListener.java # 应用关闭监听器
│   ├── DataInitializer.java             # 数据初始化
│   ├── GlobalExceptionHandler.java      # 全局异常处理
│   ├── SecurityConfig.java              # 安全配置
│   ├── SecurityWebSocketHandshakeInterceptor.java # WebSocket安全拦截器
│   └── WebSocketConfig.java             # WebSocket配置
├── controller/                           # 控制器层
│   ├── AdminController.java             # 管理员功能
│   ├── AdminDashboardController.java    # 管理员仪表板
│   ├── AlertThresholdController.java    # 告警阈值控制器
│   ├── ApplicationConfigController.java # 应用配置控制器
│   ├── ApplicationController.java       # 应用管理
│   ├── DebugController.java             # 调试功能控制器
│   ├── DeveloperDashboardController.java # 开发者仪表板
│   ├── InitialConfigController.java     # 初始配置控制器
│   ├── LogController.java               # 日志控制器
│   ├── MonitoringController.java        # 监控功能
│   ├── MonitoringHistoryController.java # 监控历史控制器
│   ├── PageController.java              # 页面控制器
│   ├── RemoteCommandController.java     # 远程命令控制器
│   ├── SSHTerminalController.java       # SSH终端
│   ├── SSHTerminalWebSocketHandler.java # SSH WebSocket处理
│   ├── SSHTestController.java           # SSH测试控制器
│   ├── TestController.java              # 测试控制器
│   ├── UserController.java              # 用户管理
│   └── WebSocketController.java         # WebSocket控制器
├── dto/                                  # 数据传输对象
│   ├── AdminDashboardDto.java           # 管理员仪表板DTO
│   ├── AggregatedServerMetrics.java     # 聚合服务器指标DTO
│   ├── DeveloperDashboardDto.java       # 开发者仪表板DTO
│   ├── PasswordChangeDto.java           # 密码修改DTO
│   ├── UserPreferencesDto.java          # 用户偏好DTO
│   ├── UserProfileDto.java              # 用户档案DTO
│   └── UserRegistrationDto.java         # 用户注册DTO
├── event/                               # 事件类
│   └── ServerStatusUpdateEvent.java     # 服务器状态更新事件
├── model/                               # 实体类
│   ├── AlertThreshold.java              # 告警阈值实体
│   ├── Application.java                 # 应用实体
│   ├── ApplicationConfig.java           # 应用配置实体
│   ├── Server.java                      # 服务器实体
│   ├── ServerMetrics.java               # 服务器监控指标
│   ├── SSHSession.java                  # SSH会话实体
│   ├── User.java                        # 用户实体
│   ├── UserActivity.java                # 用户活动实体
│   └── UserConfig.java                  # 用户配置实体
├── repository/                          # 数据访问层
│   ├── AlertThresholdRepository.java    # 告警阈值数据访问
│   ├── ApplicationConfigRepository.java # 应用配置数据访问
│   ├── ApplicationRepository.java       # 应用数据访问
│   ├── ServerMetricsRepository.java     # 服务器指标数据访问
│   ├── ServerRepository.java            # 服务器数据访问
│   ├── SSHSessionRepository.java        # SSH会话数据访问
│   ├── UserActivityRepository.java      # 用户活动数据访问
│   ├── UserConfigRepository.java        # 用户配置数据访问
│   └── UserRepository.java              # 用户数据访问
├── service/                             # 业务逻辑层
│   ├── AlertThresholdService.java       # 告警阈值服务
│   ├── ApplicationConfigService.java    # 应用配置服务
│   ├── ApplicationService.java          # 应用服务
│   ├── BatchProcessingService.java      # 批处理服务
│   ├── DashboardService.java            # 仪表板服务
│   ├── Impl/                            # 服务实现
│   │   └── UserServiceImpl.java         # 用户服务实现
│   ├── LogMonitoringService.java        # 日志监控服务
│   ├── MonitoringHistoryService.java    # 监控历史服务
│   ├── MonitoringSchedulerService.java  # 监控调度服务
│   ├── MonitoringService.java           # 监控服务
│   ├── PerformanceMonitoringService.java # 性能监控服务
│   ├── PortManagerService.java          # 端口管理服务
│   ├── RemoteCommandService.java        # 远程命令服务
│   ├── ServerService.java               # 服务器服务
│   ├── SshConnectionService.java        # SSH连接服务
│   ├── SSHTerminalService.java          # SSH终端服务
│   ├── UserActivityService.java         # 用户活动服务
│   ├── UserService.java                 # 用户服务
│   └── UserSessionService.java          # 用户会话服务
└── test/                                # 测试工具
    ├── DirectSSHTest.java               # 直接SSH测试
    ├── ServerManagementTester.java      # 服务器管理测试器
    └── SSHConnectionTester.java         # SSH连接测试器
```

├── static/                            # 静态资源
│   ├── css/                          # 样式文件
│   │   ├── application-detail.css    # 应用详情样式
│   │   ├── applications.css          # 应用列表样式
│   │   ├── common-dashboard.css      # 通用仪表板样式
│   │   ├── config-editor.css         # 配置编辑器样式
│   │   ├── dashboard-theme.css       # 主题样式
│   │   ├── drawer.css                # 抽屉组件样式
│   │   ├── fontawesome-local.css     # 本地Font Awesome图标
│   │   ├── index.css                 # 首页样式
│   │   ├── initial-config.css        # 初始配置样式
│   │   ├── login.css                 # 登录页面样式
│   │   ├── monitoring-history.css    # 监控历史样式
│   │   ├── profile.css               # 用户档案样式
│   │   ├── servers.css               # 服务器管理样式
│   │   ├── style.css                 # 通用样式
│   │   └── ux-enhancement.css        # UX增强样式
│   ├── js/                           # JavaScript文件
│   │   ├── application-detail.js     # 应用详情脚本
│   │   ├── config-editor.js          # 配置编辑器脚本
│   │   ├── dashboard.js              # 仪表板脚本
│   │   ├── drawer.js                 # 抽屉组件脚本
│   │   ├── monitoring-history.js     # 监控历史脚本
│   │   ├── profile.js                # 用户档案脚本
│   │   ├── server-detail.js          # 服务器详情脚本
│   │   ├── theme.js                  # 主题切换脚本
│   │   └── ux-enhancement.js         # UX增强脚本
│   └── vendor/                       # 第三方资源
└── templates/                         # Thymeleaf模板
    ├── admin-dashboard.html          # 管理员仪表板
    ├── admin/                        # 管理员页面
    │   ├── config-editor.html        # 配置编辑器
    │   ├── server-detail.html        # 服务器详情
    │   ├── server-form.html          # 服务器表单
    │   ├── servers.html              # 服务器列表
    │   ├── user-form.html            # 用户表单
    │   ├── users.html                # 用户列表
    │   └── users_old.html            # 用户列表(旧版)
    ├── application-detail.html       # 应用详情页面
    ├── applications.html             # 应用列表页面
    ├── developer-dashboard.html      # 开发者仪表板
    ├── index.html                    # 首页
    ├── initial-config.html           # 初始配置页面
    ├── login.html                    # 登录页面
    ├── monitoring/                   # 监控页面
    │   ├── history-dashboard.html    # 历史监控仪表板
    │   ├── server-details.html       # 服务器详情监控
    │   ├── threshold-dashboard.html  # 阈值监控仪表板
    │   └── user-activity.html        # 用户活动监控
    ├── register.html                 # 注册页面
    ├── server-management-test.html   # 服务器管理测试页面
    ├── ssh-test.html                 # SSH测试页面
    ├── terminal/                     # 终端页面
    │   ├── index.html                # 终端主页
    │   └── manager.html              # 终端管理器
    └── user-profile.html             # 用户档案页面
```

```
项目根目录结构:
├── CLAUDE.md                         # Claude AI 开发助手指南
├── DRAWER_INTEGRATION.md             # 抽屉组件集成文档
├── data/                             # 数据文件目录
│   └── devplatformdb.mv.db           # H2数据库文件
├── doc/                              # 项目文档目录
│   ├── README.md                     # 文档目录索引
│   ├── deployment-guide.md           # 部署指南
│   ├── operations-manual.md          # 运维手册
│   ├── performance-optimization.md   # 性能优化指南
│   └── project-overview.md           # 项目概述
├── mvnw                              # Maven Wrapper (Unix)
├── mvnw.cmd                          # Maven Wrapper (Windows)
├── pom.xml                           # Maven项目配置
├── src/                              # 源代码目录
├── target/                           # 构建输出目录
└── workspaces/                       # 工作空间目录
    └── root/                         # 根工作空间
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

### 1. 应用管理API (`/apps`)
**基础路径**: `/apps`
**权限**: 用户只能访问自己的应用

#### 应用生命周期管理
- `GET /apps`: 应用管理页面
- `GET /apps/{id}`: 应用详情页面
- `POST /apps/upload`: 上传应用
- `POST /apps/{id}/start`: 启动应用
- `POST /apps/{id}/stop`: 停止应用
- `POST /apps/{id}/restart`: 重启应用
- `POST /apps/{id}/delete`: 删除应用

#### 应用配置管理
- `GET /apps/{id}/config/editor`: 配置编辑器页面
- `POST /apps/{id}/config`: 更新应用配置

#### 应用数据API
- `GET /apps/api/list`: 获取用户应用列表 (JSON)
- `GET /apps/{id}/metrics`: 获取应用监控指标 (JSON)
- `GET /apps/{id}/logs`: 获取应用日志 (JSON)
- `GET /apps/{id}/logs/download`: 下载应用日志文件
- `GET /apps/{id}/health`: 应用健康检查 (JSON)

### 2. 用户管理API
**权限**: 需要认证用户

#### 用户注册与档案
- `GET /register`: 用户注册页面
- `POST /register`: 处理用户注册
- `GET /profile`: 用户档案页面
- `POST /profile/update`: 更新用户档案
- `POST /profile/change-password`: 修改密码
- `POST /profile/preferences`: 更新用户偏好 (JSON)

#### 用户数据API
- `GET /api/profile`: 获取用户档案信息 (JSON)
- `GET /api/profile/preferences`: 获取用户偏好设置 (JSON)

### 3. 管理员API (`/admin`)
**基础路径**: `/admin`
**权限**: 需要ADMIN或SUPER_ADMIN角色

#### 服务器管理
- `GET /admin/servers`: 服务器管理页面
- `GET /admin/servers/{id}`: 服务器详情页面
- `POST /admin/servers`: 创建服务器
- `POST /admin/servers/{id}/update`: 更新服务器
- `POST /admin/servers/{id}/delete`: 删除服务器
- `POST /admin/servers/{id}/check-connection`: 检查服务器连接
- `POST /admin/servers/check-all-connections`: 检查所有服务器连接
- `GET /admin/servers/{id}/status`: 获取服务器状态 (JSON)
- `GET /admin/servers/{id}/metrics`: 获取服务器监控数据 (JSON)
- `POST /admin/servers/{id}/refresh`: 刷新服务器状态 (JSON)

#### 用户管理 (SUPER_ADMIN)
- `GET /admin/users`: 用户管理页面
- `POST /admin/users`: 创建用户
- `POST /admin/users/{id}/update`: 更新用户
- `POST /admin/users/{id}/delete`: 删除用户
- `POST /admin/users/{id}/toggle-admin`: 切换管理员角色
- `POST /admin/users/{id}/toggle-super-admin`: 切换超级管理员角色

### 4. 监控API (`/monitoring`)
**基础路径**: `/monitoring`

#### 监控页面
- `GET /monitoring/server/{id}`: 服务器详情监控页面
- `GET /monitoring/user-activity`: 用户活动监控页面

#### 监控数据API
- `GET /monitoring/server/{id}/metrics`: 获取服务器监控指标 (JSON)
- `POST /monitoring/server/{id}/refresh`: 刷新服务器监控数据 (JSON)
- `GET /monitoring/servers/metrics`: 获取所有服务器监控数据 (JSON)
- `GET /monitoring/server/{id}/health`: 服务器健康检查 (JSON)
- `GET /monitoring/server/{id}/users`: 获取服务器活跃用户 (JSON)
- `GET /monitoring/server/{id}/activity-summary`: 获取服务器活动摘要 (JSON)

### 5. 告警阈值API (`/monitoring/thresholds`)
**基础路径**: `/monitoring/thresholds`
**权限**: 需要ADMIN或DEVELOPER角色

#### 阈值管理页面
- `GET /monitoring/thresholds/dashboard`: 阈值管理仪表板
- `GET /monitoring/thresholds/server/{serverId}`: 服务器阈值配置页面

#### 阈值配置API
- `GET /monitoring/thresholds/api/server/{serverId}`: 获取服务器阈值配置 (JSON)
- `POST /monitoring/thresholds/api/save`: 保存阈值配置 (JSON)
- `POST /monitoring/thresholds/api/batch-save`: 批量保存阈值 (JSON)
- `DELETE /monitoring/thresholds/api/{thresholdId}`: 删除阈值配置 (JSON)
- `POST /monitoring/thresholds/api/server/{serverId}/reset`: 重置服务器阈值 (JSON)
- `POST /monitoring/thresholds/api/server/{sourceServerId}/copy`: 复制阈值配置 (JSON)
- `GET /monitoring/thresholds/api/statistics`: 获取阈值统计信息 (JSON)

### 6. 监控历史API (`/monitoring/history`)
**基础路径**: `/monitoring/history`

#### 历史数据API
- `GET /monitoring/history/api/server/{serverId}/data`: 获取服务器历史数据 (JSON)
- `GET /monitoring/history/api/servers/compare`: 比较服务器数据 (JSON)
- `GET /monitoring/history/api/server/{serverId}/realtime`: 获取实时数据 (JSON)
- `GET /monitoring/history/api/server/{serverId}/statistics`: 获取服务器统计信息 (JSON)
- `GET /monitoring/history/api/server/{serverId}/anomalies`: 获取异常事件 (JSON)
- `GET /monitoring/history/api/servers/ranking`: 获取服务器性能排名 (JSON)
- `GET /monitoring/history/api/server/{serverId}/export`: 导出历史数据为CSV
- `GET /monitoring/history/api/server/{serverId}/quick-range`: 获取快速时间范围数据 (JSON)

### 7. 应用配置API (`/api/applications/{applicationId}/config`)
**基础路径**: `/api/applications/{applicationId}/config`

#### 配置管理
- `GET /api/applications/{applicationId}/config/active`: 获取活跃配置 (JSON)
- `GET /api/applications/{applicationId}/config`: 获取所有配置 (JSON)
- `GET /api/applications/{applicationId}/config/history`: 获取配置历史 (JSON)
- `POST /api/applications/{applicationId}/config`: 创建配置 (JSON)
- `POST /api/applications/{applicationId}/config/{configId}/apply`: 应用配置 (JSON)
- `POST /api/applications/{applicationId}/config/validate`: 验证配置 (JSON)
- `POST /api/applications/{applicationId}/config/backup`: 创建配置备份 (JSON)
- `POST /api/applications/{applicationId}/config/{configId}/hot-reload`: 热重载配置 (JSON)

#### 配置模板与导入导出
- `GET /api/applications/{applicationId}/config/templates`: 获取配置模板 (JSON)
- `POST /api/applications/{applicationId}/config/templates`: 创建配置模板 (JSON)
- `POST /api/applications/{applicationId}/config/templates/{templateName}/apply`: 应用配置模板 (JSON)
- `GET /api/applications/{applicationId}/config/{configId}/export/json`: 导出配置为JSON
- `GET /api/applications/{applicationId}/config/{configId}/export/yaml`: 导出配置为YAML
- `GET /api/applications/{applicationId}/config/{configId1}/compare/{configId2}`: 比较配置 (JSON)

### 8. 日志管理API (`/api/logs`)
**基础路径**: `/api/logs`

#### 日志监控
- `POST /api/logs/{applicationId}/start-monitoring`: 开始日志监控 (JSON)
- `POST /api/logs/{applicationId}/stop-monitoring`: 停止日志监控 (JSON)
- `GET /api/logs/{applicationId}/tail`: 获取尾部日志 (JSON)
- `GET /api/logs/{applicationId}/info`: 获取日志信息 (JSON)

### 9. 远程命令API (`/api/command`)
**基础路径**: `/api/command`

#### 远程命令执行
- `POST /api/command/execute/{serverId}`: 在服务器上执行命令 (JSON)
- `POST /api/command/execute/batch`: 批量执行命令 (JSON)
- `GET /api/command/file/{serverId}`: 读取远程文件 (JSON)
- `POST /api/command/file/{serverId}`: 写入远程文件 (JSON)
- `GET /api/command/common-commands`: 获取常用命令列表 (JSON)

### 10. SSH终端API (`/terminal`)
**基础路径**: `/terminal`

#### 终端管理
- `GET /terminal`: 终端首页
- `GET /terminal/connect/{serverId}`: 连接服务器终端页面
- `GET /terminal/manager`: 终端管理器页面

#### 终端数据API
- `GET /terminal/api/servers`: 获取服务器列表 (JSON)
- `GET /terminal/api/sessions`: 获取活跃SSH会话 (JSON)
- `GET /terminal/api/session/{sessionId}/status`: 获取会话状态 (JSON)
- `DELETE /terminal/api/session/{sessionId}`: 关闭SSH会话 (JSON)
- `GET /terminal/api/stats`: 获取会话统计信息 (JSON)
- `POST /terminal/api/test-connection/{serverId}`: 测试服务器连接 (JSON)

### 11. WebSocket实时通信API
**协议**: STOMP over WebSocket
**端点**: `/ws`

#### 消息映射
- `@MessageMapping /connect` → `@SendTo /topic/status`: 处理客户端连接
- `@SubscribeMapping /logs/{applicationId}`: 处理日志订阅
- `@SubscribeMapping /status`: 处理状态订阅

#### 订阅主题
- `/topic/status`: 通用状态更新
- `/topic/app-status`: 应用状态更新
- `/topic/server-status`: 服务器状态更新
- `/logs/{applicationId}`: 应用特定日志流

### 12. 调试与测试API
**基础路径**: `/debug`, `/test`

#### 调试端点
- `GET /debug/check-root`: 检查root用户状态
- `GET /debug/test-aggregated-metrics`: 测试聚合监控指标
- `GET /debug/test-monitoring-service`: 测试监控服务
- `GET /debug/test-user-profile`: 测试用户档案功能
- `GET /debug/check-server-status`: 检查服务器状态信息

#### 测试端点
- `GET /test/ssh`: SSH测试页面
- `POST /test/ssh/connect`: 测试SSH连接 (JSON)
- `POST /test/ssh/execute`: 执行SSH命令测试 (JSON)
- `GET /test/ssh/servers`: 获取测试服务器列表 (JSON)
- `GET /test/server-management`: 测试服务器管理功能 (JSON)

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
**文档更新**: 2025-09-02

---

*此文档基于项目当前状态生成，随项目发展持续更新*