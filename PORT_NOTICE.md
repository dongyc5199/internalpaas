# ⚠️ 端口配置说明

## 🔴 重要提醒

本应用运行在 **9090** 端口，不是 8080！

```
✅ 正确: http://localhost:9090
❌ 错误: http://localhost:8080
```

---

## 📍 配置位置

**文件**: `src/main/resources/application.properties`

```properties
server.port=9090
```

---

## 🌟 快速访问

### 演示页面
```
http://localhost:9090/demo/utils
```

### 管理页面
```
http://localhost:9090/admin/servers
http://localhost:9090/admin/dashboard
```

### 登录页面
```
http://localhost:9090/login
```

---

## 🚀 快速启动

```bash
# 一键启动
start-demo.cmd

# 或手动启动
npm run build && mvn spring-boot:run
```

15秒后访问: http://localhost:9090/demo/utils

---

## 📚 相关文档

- [快速启动指南](./QUICK_START.md)
- [完整演示说明](./DEMO_PAGES.md)
