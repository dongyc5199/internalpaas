# 🚀 快速启动指南

## ⚡ 一键启动（推荐）

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
