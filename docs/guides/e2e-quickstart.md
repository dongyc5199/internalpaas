# E2E测试快速开始 🚀

## 一分钟快速启动

```bash
# 1. 安装依赖
npm install
npx playwright install chromium

# 2. 启动应用
mvn spring-boot:run
# 或者
java -jar target/internalpaas-*.jar

# 3. 运行测试
npm run test:e2e
```

## 常用命令

```bash
# 🎭 UI模式运行（推荐）
npm run test:e2e:ui

# 🐛 调试模式
npm run test:e2e:debug

# 👀 查看浏览器运行
npm run test:e2e:headed

# 📊 查看测试报告
npm run test:e2e:report

# 🎬 生成测试代码
npm run test:e2e:codegen
```

## 测试覆盖

✅ **用户认证** - 登录、登出、权限控制
✅ **服务器管理** - 添加、查看、编辑、删除服务器
✅ **应用管理** - 上传、启动、停止、重启应用

## 目录结构

```
e2e/
├── pages/          # Page Object Models
├── tests/          # 测试用例
│   ├── auth/                  # 认证测试
│   ├── server-management/     # 服务器管理测试
│   └── application-management/ # 应用管理测试
├── utils/          # 工具函数
└── README.md       # 完整文档
```

## 需要帮助？

查看完整文档：[e2e/README.md](./README.md)

## 快速示例

```typescript
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';

test('登录测试', async ({ page }) => {
  const loginPage = new LoginPage(page);

  await loginPage.goto();
  await loginPage.login('admin', 'admin');

  await expect(page).toHaveURL(/dashboard/);
});
```

---

**创建时间**: 2025-10-13
**框架**: Playwright v1.56+
**Node.js**: v18+
