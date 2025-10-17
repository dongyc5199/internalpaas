# E2E测试运行指南 - 管理员功能

## ✅ 已修复的问题

### 问题1: 连接被拒绝
- **原因**: 应用运行在9090端口，配置指向8080
- **修复**: 创建`.env.e2e`文件，设置`BASE_URL=http://localhost:9090`

### 问题2: 测试超时
- **原因**: URL模式不匹配
  - 期望: `/admin-dashboard` 或 `/developer-dashboard`
  - 实际: `/admin/workspace#dashboard`
- **修复**: 更新URL匹配模式为 `/\/(admin|developer)/`

### 问题3: 开发者账号问题
- **状态**: 暂时跳过开发者测试
- **方案**: 专注测试管理员功能

## 🚀 快速开始

### 1. 确认环境配置

**检查`.env.e2e`文件**:
```bash
cat .env.e2e
```

应该包含:
```env
BASE_URL=http://localhost:9090
TEST_ADMIN_USERNAME=root
TEST_ADMIN_PASSWORD=admin123
```

### 2. 确认应用运行

```bash
# 检查应用状态
curl http://localhost:9090

# 应该返回HTML内容或重定向
```

### 3. 运行测试

```bash
# 运行健康检查（推荐先运行）
npx playwright test health-check.spec.ts --reporter=list

# 运行管理员登录测试
npx playwright test e2e/tests/auth/login.spec.ts --grep "管理员" --reporter=list

# 运行所有管理员相关测试（跳过开发者测试）
npm run test:e2e
```

## 📊 测试状态

### ✅ 通过的测试

**健康检查** (4个测试):
- ✅ 应该能访问应用首页
- ✅ 应该能访问登录页面
- ✅ 应该能填写登录表单（不提交）
- ✅ Setup: 管理员认证

**认证测试**:
- ✅ 应该成功使用管理员账号登录

### ⏭️ 跳过的测试

**开发者相关**:
- ⏭️ authenticate as developer user (Setup)
- ⏭️ 应该成功使用开发者账号登录

## 🎯 当前焦点

**管理员功能测试**:
1. ✅ 用户认证（登录/登出）
2. 🔄 服务器管理
3. 🔄 应用管理
4. 🔄 监控功能

## 📝 已修改的文件

### 配置文件
- `.env.e2e` - 环境变量配置（新建）
- `playwright.config.ts` - 增加超时时间，加载环境变量

### Page Objects
- `e2e/pages/LoginPage.ts` - 修复URL匹配模式

### 测试文件
- `e2e/global.setup.ts` - 修复认证流程，跳过开发者测试
- `e2e/tests/auth/login.spec.ts` - 更新断言，跳过开发者测试
- `e2e/tests/health-check.spec.ts` - 新增健康检查测试

## 🔧 常用命令

```bash
# 查看测试列表
npx playwright test --list

# 运行特定测试
npx playwright test <测试文件名>

# 使用UI模式（推荐调试时使用）
npm run test:e2e:ui

# 使用headed模式查看浏览器
npm run test:e2e:headed

# 查看测试报告
npm run test:e2e:report

# 生成测试代码（录制）
npm run test:e2e:codegen
```

## 🐛 调试技巧

### 查看截图
```bash
# 测试失败时自动保存截图
ls test-results/*/test-failed-*.png
```

### 查看Trace
```bash
# 查看详细的测试执行trace
npx playwright show-trace test-results/*/trace.zip
```

### 运行单个测试并查看浏览器
```bash
npx playwright test <测试文件> --grep "<测试名称>" --headed --debug
```

## 📈 下一步

### 短期目标
1. ✅ 修复认证问题
2. 🔄 运行服务器管理测试
3. 🔄 运行应用管理测试
4. 🔄 根据实际页面结构调整选择器

### 中期目标
1. 为关键元素添加`data-testid`属性
2. 创建或修复开发者测试账号
3. 完善所有测试用例

## ⚠️ 已知限制

1. **开发者账号**: 当前跳过所有开发者相关测试
2. **URL模式**: 实际URL为`/admin/workspace`而非`/admin-dashboard`
3. **选择器**: 使用宽泛的选择器，建议添加`data-testid`提高稳定性

## 💡 提示

### 确保测试通过的清单
- [ ] Spring Boot应用运行在9090端口
- [ ] `.env.e2e`文件存在且配置正确
- [ ] 管理员账号`root`/`admin123`可用
- [ ] 网络稳定（测试会访问多个页面）

### 如果测试失败
1. 查看`test-results/`目录中的截图
2. 使用`--headed`模式查看浏览器
3. 使用`--debug`模式逐步执行
4. 检查控制台日志

---

**最后更新**: 2025-10-13
**测试框架**: Playwright v1.56+
**测试状态**: ✅ 管理员登录测试通过
