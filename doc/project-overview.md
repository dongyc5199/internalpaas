# Dev Debug Platform - 轻量级后端调试平台

一个为后端开发团队设计的轻量级Web平台，旨在简化在共享开发服务器上的部署与调试流程。告别命令行与端口冲突，拥抱一键式可视化操作。

---

## 🎯 项目背景：解决了什么痛点？

在许多开发团队中，多名后端开发者需要共用一台或几台开发/测试服务器。这常常导致以下问题：
*   **端口冲突**：开发者A启动了服务占用了8080端口，开发者B必须等待或修改配置。
*   **重复操作**：频繁地 `scp` 上传JAR包，`kill` 旧进程，`nohup java -jar ...` 启动新进程，过程繁琐且易出错。
*   **环境不一致**：手动执行命令可能导致每个人启动服务的方式不完全一致。
*   **信息不透明**：无法直观地看到谁的服务正在运行，占用了什么资源，日志输出是什么。

本平台将这些繁琐的命令行操作封装成一个简洁的Web界面，为每个开发者提供隔离的、可一键操作的调试环境。

## ✨ 核心功能

*   👤 **用户与权限管理**:
    *   **独立用户系统**: 支持用户注册和登录，保障操作安全。
    *   **个性化工作区**: 用户首次登录需配置自己在服务器上的工作目录，所有资源（JAR包、日志）完全隔离。

*   🚀 **应用生命周期管理**:
    *   **一键上传**: 通过Web界面直接上传本地打包好的`*.jar`文件。
    *   **一键启动/停止/重启**: 可视化按钮控制应用的完整生命周期。
    *   **动态端口分配**: 系统自动从预设的端口池中为应用和远程调试（JDWP）分配可用端口，从根源上解决端口冲突。

*   📊 **状态与日志监控**:
    *   **实时状态面板**: 清晰展示当前应用的运行状态（运行中/已停止）、进程ID (PID)、应用端口和调试端口。
    *   **实时日志流**: 通过WebSocket实现 `tail -f` 的效果，在Web页面实时查看应用输出的日志。

*   🛠️ **调试友好**:
    *   自动开启远程调试端口，方便开发者使用IDE（如IntelliJ IDEA）连接进行断点调试。
    *   一键复制生成的SSH隧道命令，简化本地调试环境配置。

## 🛠️ 技术栈

*   **后端**: Spring Boot 3.x
*   **Web/API**: Spring Web
*   **安全**: Spring Security (用户认证与授权)
*   **数据库**: Spring Data JPA + H2 (内嵌数据库，易于部署) / SQLite
*   **前端模板**: Thymeleaf
*   **实时通信**: Spring for WebSocket
*   **构建工具**: Maven

## 🚀 快速开始

### 1. 环境要求
*   Java 17 或更高版本
*   Apache Maven 3.6+
*   Git

### 2. 下载与安装
```bash
# 1. 克隆项目到本地
git clone [Your Repository URL Here]
cd dev-debug-platform

# 2. 使用Maven构建项目
# 这会下载所有依赖并打包成一个可执行的JAR文件
mvn clean install
```

### 3. 运行平台
构建成功后，在 `target/` 目录下会生成一个 `dev-debug-platform-x.x.x.jar` 文件。
通过以下命令启动平台：
```bash
java -jar target/dev-debug-platform-*.jar
```
平台默认将在 `http://localhost:8080` 启动。

### 4. 配置文件
主要的配置在 `src/main/resources/application.properties` 文件中。
```properties
# 服务器端口
server.port=8080

# H2数据库配置
# 数据文件将存储在运行目录下的 ./data/ 文件夹中
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.datasource.url=jdbc:h2:file:./data/devplatformdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password

# JPA配置
# "update"模式会在启动时根据实体类自动更新数据库表结构，非常适合开发阶段
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## 📖 使用流程

1.  **注册与登录**:
    *   首次使用，请访问 `http://<服务器IP>:8080/register` 注册一个账户。
    *   然后通过 `http://<服务器IP>:8080/login` 登录。

2.  **首次配置**:
    *   首次登录后，系统会引导你进入配置页面。
    *   你需要提供一个**在服务器上已存在的、且平台运行用户有读写权限的绝对路径**作为你的工作目录（例如: `/home/your_username/debug_space`）。
    *   保存后，你将被引导至主控制台。

3.  **调试循环**:
    *   在主控制台，点击**上传JAR包**，选择你本地的`*.jar`文件。
    *   上传成功后，点击**启动服务**。
    *   平台的**状态面板**会立刻更新，显示"运行中"，并为你分配好**应用端口**和**调试端口**。
    *   切换到**日志**标签页，可以实时看到你的应用启动日志。
    *   使用你喜欢的IDE和新分配的调试端口，开始你的远程Debug之旅！
    *   调试完成后，点击**停止服务**释放资源。

## 🗺️ 未来路线图 (Roadmap)

- [ ] **历史版本管理**: 支持保留最近几个JAR包版本，并可选择回滚启动。
- [ ] **资源监控**: 在面板上显示CPU和内存占用率。
- [ ] **高级日志功能**: 支持日志搜索、过滤和下载。
- [ ] **多项目支持**: 允许一个用户管理多个不同的微服务调试实例。
- [ ] **Nginx/网关集成**: 自动更新反向代理配置，提供如 `your-domain.com/debug/username/` 的固定访问地址。

## 🤝 贡献

欢迎提交 Issues 和 Pull Requests 来帮助改进这个项目！

## 📄 许可证

本项目采用 [MIT License](LICENSE) 授权。