# E2E测试指南

## 📋 目录

- [简介](#简介)
- [测试框架](#测试框架)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [编写测试](#编写测试)
- [运行测试](#运行测试)
- [调试测试](#调试测试)
- [CI/CD集成](#cicd集成)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)

---

## 简介

本项目使用**Playwright**作为E2E（端到端）测试框架，用于测试Dev Debug Platform的完整用户流程。

### 测试覆盖范围

✅ **用户认证流程**
- 登录/登出
- 权限控制
- 会话管理
- 用户注册

✅ **服务器群组管理**
- 服务器列表展示
- 添加/编辑/删除服务器
- 服务器详情查看
- 监控数据展示

✅ **应用管理**
- 应用列表展示
- JAR文件上传
- 应用生命周期管理（启动/停止/重启）
- 应用日志查看

---

## 测试框架

### 为什么选择Playwright？

| 优势 | 说明 |
|-----|------|
| 🚀 **高性能** | 比Cypress更快的执行速度 |
| 🌐 **多浏览器** | 原生支持Chromium, Firefox, WebKit |
| 📝 **TypeScript** | 与项目技术栈完美集成 |
| 🔍 **强大调试** | 内置Inspector和Trace Viewer |
| 🎯 **可靠性** | 自动等待机制，减少flaky测试 |
| 📊 **丰富报告** | HTML报告、截图、视频录制 |

---

## 项目结构

```
e2e/
├── .auth/                      # 认证状态存储（gitignore）
│   ├── user.json              # 管理员认证
│   └── developer.json         # 开发者认证
├── fixtures/                   # 测试数据
│   └── test-app.jar           # 测试用JAR文件
├── pages/                      # Page Object Models
│   ├── LoginPage.ts           # 登录页面
│   ├── ServerGroupManagementPage.ts  # 服务器管理页面
│   ├── ServerDetailOverlayPage.ts    # 服务器详情弹窗
│   └── ApplicationManagementPage.ts  # 应用管理页面
├── tests/                      # 测试用例
│   ├── auth/
│   │   └── login.spec.ts      # 认证测试
│   ├── server-management/
│   │   └── server-groups.spec.ts  # 服务器管理测试
│   └── application-management/
│       └── applications.spec.ts   # 应用管理测试
├── utils/                      # 工具函数
│   └── helpers.ts             # 通用辅助函数
└── global.setup.ts            # 全局setup（登录认证）

playwright.config.ts           # Playwright配置
.env.e2e.example              # 环境变量示例
```

---

## 快速开始

### 1. 安装依赖

```bash
# 安装npm包
npm install

# 安装Playwright浏览器
npx playwright install chromium
```

### 2. 配置环境变量

复制环境变量模板并修改：

```bash
cp .env.e2e.example .env.e2e
```

编辑`.env.e2e`：

```env
BASE_URL=http://localhost:8080
TEST_ADMIN_USERNAME=admin
TEST_ADMIN_PASSWORD=admin
TEST_DEV_USERNAME=developer
TEST_DEV_PASSWORD=developer
```

### 3. 启动应用

确保Spring Boot应用正在运行：

```bash
# 方式1: Maven
mvn spring-boot:run

# 方式2: JAR包
java -jar target/internalpaas-*.jar

# 方式3: IDE运行
# 在IntelliJ IDEA中运行 InternalpaasApplication
```

### 4. 运行测试

```bash
# 运行所有E2E测试
npm run test:e2e

# 运行特定测试文件
npx playwright test e2e/tests/auth/login.spec.ts

# 使用UI模式运行
npm run test:e2e:ui
```

---

## 编写测试

### Page Object Model模式

我们使用**Page Object Model (POM)**模式来组织测试代码。

#### 示例：创建一个新的Page Object

```typescript
// e2e/pages/MyNewPage.ts
import { Page, Locator } from '@playwright/test';

export class MyNewPage {
  readonly page: Page;
  readonly myButton: Locator;
  readonly myInput: Locator;

  constructor(page: Page) {
    this.page = page;
    this.myButton = page.locator('button.my-button');
    this.myInput = page.locator('input[name="myInput"]');
  }

  async goto() {
    await this.page.goto('/my-page');
  }

  async fillForm(value: string) {
    await this.myInput.fill(value);
    await this.myButton.click();
  }
}
```

#### 示例：编写测试用例

```typescript
// e2e/tests/my-feature/my-test.spec.ts
import { test, expect } from '@playwright/test';
import { MyNewPage } from '../../pages/MyNewPage';

test.describe('我的功能测试', () => {
  let myPage: MyNewPage;

  test.beforeEach(async ({ page }) => {
    myPage = new MyNewPage(page);
    await myPage.goto();
  });

  test('应该能填写表单', async () => {
    await myPage.fillForm('测试数据');

    // 验证结果
    await expect(myPage.page.locator('.success-message')).toBeVisible();
  });
});
```

### 测试结构最佳实践

1. **使用describe分组相关测试**

```typescript
test.describe('用户登录', () => {
  test.describe('成功场景', () => {
    test('管理员登录', async () => { /* ... */ });
    test('开发者登录', async () => { /* ... */ });
  });

  test.describe('失败场景', () => {
    test('错误密码', async () => { /* ... */ });
    test('不存在的用户', async () => { /* ... */ });
  });
});
```

2. **使用beforeEach进行setup**

```typescript
test.beforeEach(async ({ page }) => {
  // 每个测试前执行
  await page.goto('/my-page');
});
```

3. **清理测试数据**

```typescript
test.afterEach(async () => {
  // 清理测试创建的数据
  if (testServerId) {
    await deleteTestServer(testServerId);
  }
});
```

---

## 运行测试

### 常用命令

```bash
# 运行所有测试（无头模式）
npm run test:e2e

# 运行所有测试（有头模式，可见浏览器）
npm run test:e2e:headed

# 使用UI模式（推荐用于开发）
npm run test:e2e:ui

# 调试模式（逐步执行）
npm run test:e2e:debug

# 运行特定测试文件
npx playwright test login.spec.ts

# 运行特定测试用例（使用grep）
npx playwright test --grep "应该成功登录"

# 查看HTML报告
npm run test:e2e:report
```

### 测试选项

```bash
# 指定浏览器
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit

# 并行运行（指定worker数）
npx playwright test --workers=4

# 重试失败的测试
npx playwright test --retries=2

# 更新快照
npx playwright test --update-snapshots

# 显示浏览器
npx playwright test --headed

# 慢速执行（每个操作延迟1秒）
npx playwright test --slow-mo=1000
```

---

## 调试测试

### 方法1：使用Playwright Inspector

```bash
# 调试模式运行
npm run test:e2e:debug

# 或使用PWDEBUG环境变量
PWDEBUG=1 npx playwright test
```

在Inspector中可以：
- 逐步执行测试
- 查看选择器
- 查看控制台日志
- 截图

### 方法2：使用page.pause()

在测试代码中添加暂停点：

```typescript
test('调试测试', async ({ page }) => {
  await page.goto('/login');

  await page.pause(); // 暂停，打开Inspector

  await page.fill('input[name="username"]', 'admin');
});
```

### 方法3：使用Trace Viewer

```typescript
// playwright.config.ts中已配置
use: {
  trace: 'retain-on-failure',  // 失败时保存trace
}
```

查看trace：

```bash
npx playwright show-trace trace.zip
```

### 方法4：使用VS Code扩展

安装"Playwright Test for VSCode"扩展：
- 在测试文件中点击播放按钮
- 设置断点
- 查看测试结果

---

## CI/CD集成

### GitHub Actions

项目已配置GitHub Actions workflow（`.github/workflows/e2e-tests.yml`）。

**触发条件**：
- Push到master或dev分支
- Pull Request到master或dev分支
- 手动触发

**工作流程**：
1. 设置Java和Node.js环境
2. 安装依赖和Playwright
3. 编译前端和后端
4. 启动Spring Boot应用
5. 运行E2E测试
6. 上传测试报告和截图

### 本地模拟CI环境

```bash
# 模拟CI环境运行测试
CI=true npm run test:e2e
```

---

## 最佳实践

### 1. 选择器策略

**优先级顺序**：

1️⃣ **测试ID** (最推荐)
```typescript
page.locator('[data-testid="login-button"]')
```

2️⃣ **角色和可访问性属性**
```typescript
page.getByRole('button', { name: '登录' })
page.getByLabel('用户名')
```

3️⃣ **语义化选择器**
```typescript
page.locator('button:has-text("登录")')
```

4️⃣ **CSS类和ID** (不推荐)
```typescript
page.locator('#login-btn')  // 可能改变
page.locator('.btn-primary')  // 样式类不稳定
```

### 2. 等待策略

**自动等待**（Playwright内置）：
```typescript
// Playwright自动等待元素可见、可用
await page.click('button');  // 自动等待
```

**显式等待**（特殊情况）：
```typescript
// 等待元素
await page.waitForSelector('.loading', { state: 'detached' });

// 等待URL变化
await page.waitForURL(/dashboard/);

// 等待网络空闲
await page.waitForLoadState('networkidle');

// 等待API响应
await page.waitForResponse(response =>
  response.url().includes('/api/servers')
);
```

### 3. 断言最佳实践

```typescript
// ✅ 推荐：使用expect with auto-waiting
await expect(page.locator('.message')).toBeVisible();
await expect(page.locator('.message')).toContainText('成功');

// ❌ 不推荐：手动获取再断言
const text = await page.locator('.message').innerText();
expect(text).toContain('成功');  // 没有自动等待
```

### 4. 测试独立性

```typescript
// ✅ 每个测试独立，不依赖其他测试
test('测试A', async () => {
  await setupTestData();
  // 测试逻辑
  await cleanupTestData();
});

test('测试B', async () => {
  await setupTestData();  // 独立setup
  // 测试逻辑
  await cleanupTestData();
});

// ❌ 测试依赖其他测试的数据
test('测试A创建数据', async () => { /* ... */ });
test('测试B使用测试A的数据', async () => { /* 依赖测试A */ });
```

### 5. 测试数据管理

```typescript
// ✅ 使用动态生成的测试数据
const serverName = generateTestData('TestServer');
await addServer({ name: serverName, /* ... */ });

// ❌ 使用固定的测试数据（可能冲突）
await addServer({ name: 'TestServer', /* ... */ });
```

### 6. 错误处理

```typescript
// ✅ 处理可能的错误情况
test('测试有条件的功能', async ({ page }) => {
  const hasFeature = await page.locator('.feature').isVisible().catch(() => false);

  if (hasFeature) {
    // 测试功能
  } else {
    test.skip();  // 跳过测试
  }
});
```

### 7. 避免硬编码延迟

```typescript
// ❌ 不推荐：硬编码延迟
await page.click('button');
await page.waitForTimeout(5000);  // 不可靠

// ✅ 推荐：等待特定条件
await page.click('button');
await page.waitForSelector('.success-message');
```

---

## 常见问题

### Q1: 测试超时怎么办？

**A:** 增加超时时间或检查应用是否正常运行：

```typescript
// 在配置中增加全局超时
// playwright.config.ts
export default defineConfig({
  timeout: 60 * 1000,  // 60秒
});

// 或为单个测试设置超时
test('慢速测试', async ({ page }) => {
  test.setTimeout(120000);  // 120秒
  // 测试逻辑
});
```

### Q2: 选择器找不到元素？

**A:** 使用Playwright Inspector查看元素：

```bash
npx playwright codegen http://localhost:8080
```

或在测试中添加：

```typescript
await page.pause();  // 打开Inspector
```

### Q3: 测试不稳定（flaky）怎么办？

**A:** 检查以下问题：

1. 是否使用了硬编码延迟？改用`waitFor*`方法
2. 是否等待元素可见？使用`await expect().toBeVisible()`
3. 是否有竞态条件？确保异步操作完成
4. 配置重试：`retries: 2`

### Q4: 如何在不同环境运行测试？

**A:** 使用环境变量：

```bash
# 开发环境
BASE_URL=http://localhost:8080 npm run test:e2e

# 测试环境
BASE_URL=https://test.example.com npm run test:e2e

# 生产环境（谨慎）
BASE_URL=https://prod.example.com npm run test:e2e
```

### Q5: 如何跳过特定测试？

**A:**  使用`.skip()`或条件跳过：

```typescript
// 跳过单个测试
test.skip('暂时跳过的测试', async () => { /* ... */ });

// 条件跳过
test('测试功能', async ({ page }) => {
  const hasFeature = await checkFeature();
  if (!hasFeature) {
    test.skip();
  }
  // 测试逻辑
});

// 跳过整个describe
test.describe.skip('暂时跳过的功能', () => {
  // 所有测试都被跳过
});
```

### Q6: 如何测试文件上传？

**A:** 使用`setInputFiles`：

```typescript
const fileInput = page.locator('input[type="file"]');
await fileInput.setInputFiles('path/to/test-file.jar');
```

### Q7: 如何模拟API响应？

**A:** 使用`page.route()`：

```typescript
await page.route('**/api/servers', route => {
  route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify([
      { id: 1, name: 'Mock Server' }
    ])
  });
});
```

### Q8: 如何清理测试数据？

**A:** 使用`afterEach`或`afterAll`：

```typescript
const createdServers: string[] = [];

test.afterEach(async () => {
  // 清理每个测试创建的数据
  for (const serverId of createdServers) {
    await deleteServer(serverId);
  }
  createdServers.length = 0;
});
```

---

## 参考资源

### 官方文档
- [Playwright官方文档](https://playwright.dev/)
- [Playwright API文档](https://playwright.dev/docs/api/class-playwright)
- [Playwright最佳实践](https://playwright.dev/docs/best-practices)

### 项目文档
- [前端架构指南](../doc/frontend-architecture.md)
- [任务5.1完成报告](../upgrade/doc/task5.1-module-migration-completion.md)
- [Claude AI开发助手指南](../CLAUDE.md)

### 常用链接
- [选择器生成器](https://playwright.dev/docs/codegen)
- [Trace Viewer](https://playwright.dev/docs/trace-viewer)
- [测试报告示例](https://playwright.dev/docs/test-reporters)

---

## 贡献指南

### 添加新测试

1. 确定测试类别（auth, server-management, application-management等）
2. 创建或使用现有Page Object
3. 编写测试用例
4. 运行并验证测试
5. 提交PR

### 代码风格

遵循项目的TypeScript和Prettier配置：

```bash
npm run format
npm run lint:fix
```

---

**最后更新**: 2025-10-13
**维护者**: Dev Debug Platform Team
**版本**: v1.0.0
