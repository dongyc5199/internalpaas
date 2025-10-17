# E2E测试故障排除指南

## 问题：连接被拒绝 (ERR_CONNECTION_REFUSED)

### 错误信息
```
Error: page.goto: net::ERR_CONNECTION_REFUSED at http://localhost:8080/login
```

### 原因分析
1. ❌ Spring Boot应用未启动
2. ❌ 应用运行在不同的端口（如9090而非8080）
3. ❌ `.env.e2e` 文件未创建或配置错误

### 解决方案

#### 方案1：确认应用正在运行

```bash
# 检查端口占用
netstat -ano | findstr :9090

# 或者访问浏览器
http://localhost:9090
```

如果应用未运行，启动它：

```bash
# 方式1: Maven
mvn spring-boot:run

# 方式2: JAR包
java -jar target/internalpaas-*.jar

# 方式3: IDE运行
# 在IntelliJ IDEA中运行 InternalpaasApplication
```

#### 方案2：配置正确的BASE_URL

**步骤1**: 确认 `.env.e2e` 文件存在

```bash
# 如果不存在，复制示例文件
cp .env.e2e.example .env.e2e
```

**步骤2**: 编辑 `.env.e2e` 文件

```env
# 修改为你的应用实际端口
BASE_URL=http://localhost:9090

# 修改为你的测试账号
TEST_ADMIN_USERNAME=root
TEST_ADMIN_PASSWORD=admin123
```

**步骤3**: 确认配置已加载

```bash
# 重新运行测试
npm run test:e2e
```

#### 方案3：检查依赖安装

确保已安装 `dotenv` 包：

```bash
npm install -D dotenv dotenv-cli
```

### 验证步骤

1. **确认应用运行**
```bash
curl http://localhost:9090
# 应该返回HTML内容或重定向
```

2. **确认环境变量加载**
```bash
# 在playwright.config.ts中应该有这些行：
import * as dotenv from 'dotenv';
dotenv.config({ path: path.resolve(__dirname, '.env.e2e') });
```

3. **运行单个测试验证**
```bash
# 运行登录测试
npx playwright test e2e/tests/auth/login.spec.ts --headed

# 使用debug模式查看实际访问的URL
npx playwright test --debug
```

---

## 其他常见问题

### Q1: 浏览器未安装

**错误**: `Executable doesn't exist at ...`

**解决**:
```bash
npx playwright install chromium
```

### Q2: 测试超时

**错误**: `Test timeout of 30000ms exceeded`

**解决**: 在 `.env.e2e` 中增加超时：
```env
TIMEOUT=60000
NAVIGATION_TIMEOUT=60000
```

### Q3: 认证失败

**错误**: `应该拒绝错误的用户名`

**原因**: 测试账号不存在或密码错误

**解决**:
1. 检查 `.env.e2e` 中的账号配置
2. 确认数据库中存在这些账号
3. 或者在首次启动时创建测试账号

### Q4: 端口冲突

**错误**: 应用启动失败 `Port 9090 is already in use`

**解决**:
```bash
# Windows查找占用端口的进程
netstat -ano | findstr :9090
taskkill /PID <进程ID> /F

# 或修改应用配置使用其他端口
# application.properties: server.port=9091
```

---

## 快速诊断命令

```bash
# 1. 检查应用是否运行
curl http://localhost:9090

# 2. 检查.env.e2e文件
cat .env.e2e

# 3. 使用debug模式运行测试
npm run test:e2e:debug

# 4. 使用headed模式查看浏览器
npm run test:e2e:headed

# 5. 只运行setup测试
npx playwright test global.setup.ts --headed
```

---

## 完整检查清单

- [ ] Spring Boot应用正在运行
- [ ] 应用端口为9090（或你配置的端口）
- [ ] `.env.e2e` 文件存在
- [ ] `.env.e2e` 中 `BASE_URL=http://localhost:9090` 配置正确
- [ ] 测试账号密码正确
- [ ] Playwright浏览器已安装
- [ ] `dotenv` 包已安装
- [ ] `playwright.config.ts` 已导入并加载 `.env.e2e`

---

## 仍然无法解决？

1. 查看完整文档：[e2e/README.md](./e2e/README.md)
2. 查看常见问题：[e2e/README.md#常见问题](./e2e/README.md#常见问题)
3. 使用Inspector调试：`npx playwright test --debug`

---

**最后更新**: 2025-10-13
**相关文档**: [E2E快速上手](./E2E_QUICKSTART.md)
