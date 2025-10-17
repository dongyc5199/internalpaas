# 🚀 快速启动指南

## 📊 数据库模式选择 (重要)

平台提供**三种数据库模式**,默认使用H2内存模式(零配置):

| 模式 | 命令 | 数据持久化 | 适用场景 |
|------|------|-----------|---------|
| **H2内存** (默认) | `mvn spring-boot:run` | ❌ 重启丢失 | 开发/Demo |
| **H2文件** (推荐生产) | `-Dspring-boot.run.profiles=persistent` | ✅ 文件存储 | 小团队 |
| **PostgreSQL** (可选) | `-Dspring.profiles.active=enterprise` | ✅ 数据库 | 大型团队 |

**快速选择**:
- 🔵 **快速体验/开发**: 继续往下看,使用默认模式
- 🟢 **小团队生产**: 跳到 [H2持久化模式](#h2持久化模式生产推荐)
- 🟡 **大型团队**: 查看 [部署文档](doc/deployment-guide.md)

---

## ⚡ 一键启动（推荐 - H2内存模式）

```bash
# 在项目根目录执行
start-demo.cmd
```

启动脚本会自动：
1. 构建前端资源 (`npm run build`)
2. 启动Spring Boot应用
3. 15秒后自动打开浏览器

---

## 📍 正确的访问地址

**应用端口**: `9090` ⚠️ 注意不是8080！

### 🌟 工具函数演示页面（推荐）
```
http://localhost:9090/demo/utils
```

### 📊 管理页面
```
http://localhost:9090/admin/servers         # 服务器管理
http://localhost:9090/admin/dashboard       # 管理员仪表板
http://localhost:9090/login                 # 登录页面
```

---

## 🔧 手动启动

### 方式1: 分步执行
```bash
# 1. 构建前端
npm run build

# 2. 启动应用
mvn spring-boot:run

# 3. 访问页面（15秒后）
# 浏览器打开: http://localhost:9090/demo/utils
```

### 方式2: 一行命令
```bash
npm run build && mvn spring-boot:run
```

---

## ✅ 验证启动成功

### 检查端口
```bash
# Windows
netstat -ano | findstr 9090

# 预期输出应包含:
# TCP  0.0.0.0:9090  0.0.0.0:0  LISTENING  <PID>
```

### 检查日志
应用启动成功会看到：
```
Started InternalpaasApplication in X.XXX seconds
```

### 访问健康检查
```
http://localhost:9090/actuator/health
```

---

## 🎯 演示页面功能

访问 `http://localhost:9090/demo/utils` 可以看到：

1. **顶部统计** 📊
   - 7个工具模块
   - 33个可复用函数
   - 121个测试用例
   - 93.1%覆盖率

2. **格式化工具** 📝
   - formatBytes: 文件大小格式化
   - formatUptime: 运行时间格式化
   - formatPercentage: 百分比格式化
   - formatTimestamp: 时间戳格式化
   - formatNumber: 数字千分位格式化
   - getUsageClass: 使用率样式

3. **国际化工具** 🌍
   - 中英文切换
   - 实时文本更新

4. **通知工具** 🔔
   - 4种通知类型按钮
   - 实时交互演示

5. **图表工具** 📈
   - Chart.js折线图
   - 动态数据展示

---

## ❌ 常见问题

### 问题1: 端口被占用
**错误**: `Port 9090 is already in use`

**解决**:
```bash
# 查找占用端口的进程
netstat -ano | findstr 9090

# 终止进程
taskkill /F /PID <进程ID>
```

### 问题2: 前端资源404
**错误**: `GET /dist/assets/main.js 404`

**解决**:
```bash
# 重新构建前端
npm run build

# 验证构建产物
ls src/main/resources/static/dist/assets/main.js
```

### 问题3: 访问8080端口无响应
**原因**: 端口配置错误，实际端口是9090

**解决**: 使用正确的地址
```
http://localhost:9090/demo/utils  ✅ 正确
http://localhost:8080/demo/utils  ❌ 错误
```

### 问题4: Maven构建失败
**解决**:
```bash
# 清理Maven缓存
mvn clean

# 重新构建
mvn clean install -DskipTests
```

---

## 📚 更多文档

- [完整演示指南](./DEMO_PAGES.md) - 所有可访问页面
- [详细启动说明](./upgrade/doc/demo-guide.md) - 深入的启动步骤
- [优化总结报告](./upgrade/doc/optimization-summary.md) - 重构成果
- [部署文档](./doc/deployment-guide.md) - 完整部署指南

---

## 🟢 H2持久化模式 (生产推荐)

### 特点
- ✅ 数据持久化,重启保留
- ✅ 零外部依赖 (轻量级)
- ✅ 适合小团队生产 (5-20人)
- 📁 数据保存在 `./data/` 目录

### 启动命令

```bash
# Maven方式 (推荐)
mvn spring-boot:run -Dspring-boot.run.profiles=persistent

# JAR方式
java -jar -Dspring.profiles.active=persistent target/internalpaas-0.0.1-SNAPSHOT.jar

# 环境变量方式
set SPRING_PROFILES_ACTIVE=persistent
java -jar target/internalpaas-0.0.1-SNAPSHOT.jar
```

### 首次启动验证

```bash
# 1. 启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=persistent

# 2. 确认数据目录创建
dir data\
# 应该看到: internalpaas.mv.db

# 3. 访问应用
# http://localhost:9090
```

### 数据备份

```bash
# 备份数据目录
tar -czf backup-$(date +%Y%m%d).tar.gz data/

# Windows
7z a backup.zip data\
```

### 数据恢复

```bash
# 1. 停止应用
# 2. 解压备份文件到data目录
tar -xzf backup-20251017.tar.gz

# 3. 重启应用
mvn spring-boot:run -Dspring-boot.run.profiles=persistent
```

---

## 🎉 快速测试

启动成功后，依次测试：

1. ✅ 访问演示页面: `http://localhost:9090/demo/utils`
2. ✅ 点击通知按钮，查看4种通知效果
3. ✅ 点击语言切换，查看中英文切换
4. ✅ 查看底部图表，验证Chart.js渲染
5. ✅ 访问服务器管理: `http://localhost:9090/admin/servers`

全部通过说明启动成功！🎊

---

**端口提醒**: 应用运行在 **9090** 端口，不是 8080！

**快速访问**: http://localhost:9090/demo/utils 🌟
