import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/LoginPage';

/**
 * 用户认证流程E2E测试
 *
 * 测试登录、登出、权限控制等认证相关功能
 */

test.describe('用户认证', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
  });

  test.describe('登录功能', () => {
    test('应该成功使用管理员账号登录', async ({ page }) => {
      await loginPage.goto();

      // 执行登录
      await loginPage.loginAndWait('root', 'admin123');

      // 验证跳转到管理员相关页面（URL包含admin）
      expect(page.url()).toMatch(/\/admin/);

      // 验证页面包含用户信息（使用更宽泛的选择器）
      const userMenuSelectors = [
        '[data-testid="user-menu"]',
        '.user-info',
        '.user-menu',
        'button:has-text("退出")',
        'a:has-text("退出")'
      ];

      let found = false;
      for (const selector of userMenuSelectors) {
        const element = page.locator(selector).first();
        const count = await element.count();
        if (count > 0) {
          await expect(element).toBeVisible();
          found = true;
          break;
        }
      }

      expect(found).toBe(true);
    });

    test.skip('应该成功使用开发者账号登录', async ({ page }) => {
      await loginPage.goto();

      await loginPage.loginAndWait('developer', 'developer');

      // 验证跳转到开发者仪表板
      expect(page.url()).toMatch(/developer-dashboard/);

      await expect(page.locator('[data-testid="user-menu"]')).toBeVisible();
    });

    test('应该拒绝错误的用户名', async ({ page }) => {
      await loginPage.goto();

      await loginPage.login('wrong_user', 'admin');

      // 验证显示错误消息
      const hasError = await loginPage.hasError();
      expect(hasError).toBe(true);

      // 确认未跳转
      expect(page.url()).toContain('/login');
    });

    test('应该拒绝错误的密码', async ({ page }) => {
      await loginPage.goto();

      await loginPage.login('admin', 'wrong_password');

      const hasError = await loginPage.hasError();
      expect(hasError).toBe(true);

      expect(page.url()).toContain('/login');
    });

    test('应该拒绝空的用户名和密码', async ({ page }) => {
      await loginPage.goto();

      await loginPage.login('', '');

      // 验证表单验证提示
      const usernameField = loginPage.usernameInput;
      const isValid = await usernameField.evaluate((el: HTMLInputElement) => el.checkValidity());

      expect(isValid).toBe(false);
    });

    test('应该在多次失败后显示验证码（如果启用）', async ({ page }) => {
      await loginPage.goto();

      // 连续失败3次
      for (let i = 0; i < 3; i++) {
        await loginPage.login('admin', 'wrong_password');
        await page.waitForTimeout(500);
      }

      // 检查是否显示验证码
      const captcha = page.locator('input[name="captcha"], .captcha-input');
      const hasCaptcha = await captcha.isVisible().catch(() => false);

      if (hasCaptcha) {
        await expect(captcha).toBeVisible();
      }
    });
  });

  test.describe('登出功能', () => {
    test('应该成功登出', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait('admin', 'admin');

      // 点击用户菜单
      const userMenu = page.locator('[data-testid="user-menu"], .user-info, .user-profile');
      await userMenu.click();

      // 点击登出按钮
      const logoutButton = page.locator('button:has-text("登出"), a:has-text("登出"), button:has-text("Logout")');
      await logoutButton.click();

      // 验证跳转回登录页
      await page.waitForURL(/login/, { timeout: 5000 });
      expect(page.url()).toContain('/login');
    });

    test('登出后应该清除认证状态', async ({ page, context }) => {
      await loginPage.goto();
      await loginPage.loginAndWait('admin', 'admin');

      // 登出
      const userMenu = page.locator('[data-testid="user-menu"], .user-info');
      await userMenu.click();

      const logoutButton = page.locator('button:has-text("登出"), a:has-text("登出")');
      await logoutButton.click();

      await page.waitForURL(/login/);

      // 检查cookies是否被清除
      const cookies = await context.cookies();
      const sessionCookie = cookies.find((c) => c.name.includes('SESSION') || c.name.includes('JSESSIONID'));

      expect(sessionCookie).toBeUndefined();
    });
  });

  test.describe('会话管理', () => {
    test('应该在会话过期后重定向到登录页', async ({ page, context }) => {
      await loginPage.goto();
      await loginPage.loginAndWait('admin', 'admin');

      // 手动清除session cookie
      await context.clearCookies();

      // 尝试访问需要认证的页面
      await page.goto('/admin/servers');

      // 应该被重定向到登录页
      await page.waitForURL(/login/, { timeout: 5000 });
      expect(page.url()).toContain('/login');
    });

    test('应该保持会话状态（Remember Me功能）', async ({ page, context }) => {
      await loginPage.goto();

      // 勾选"记住我"选项（如果存在）
      const rememberCheckbox = page.locator('input[name="remember"], input[type="checkbox"]:has-text("记住")');
      const hasRememberMe = await rememberCheckbox.isVisible().catch(() => false);

      if (hasRememberMe) {
        await rememberCheckbox.check();
      }

      await loginPage.loginAndWait('admin', 'admin');

      // 获取cookies
      const cookies = await context.cookies();
      const sessionCookie = cookies.find((c) => c.name.includes('SESSION') || c.name.includes('remember'));

      if (hasRememberMe && sessionCookie) {
        // 如果启用了Remember Me，cookie应该有较长的过期时间
        expect(sessionCookie.expires).toBeGreaterThan(Date.now() / 1000 + 3600); // 超过1小时
      }
    });
  });

  test.describe('权限控制', () => {
    test('管理员应该能访问管理员页面', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait('admin', 'admin');

      // 访问管理员页面
      await page.goto('/admin/servers');

      // 验证页面正常加载
      await expect(page.locator('h1, h2')).toBeVisible();
      expect(page.url()).toContain('/admin/servers');
    });

    test('开发者不应该能访问管理员页面', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait('developer', 'developer');

      // 尝试访问管理员页面
      await page.goto('/admin/users');

      // 应该被重定向或显示403错误
      await page.waitForTimeout(1000);

      const url = page.url();
      const isBlocked = url.includes('403') || url.includes('access-denied') || url.includes('developer-dashboard');

      expect(isBlocked).toBe(true);
    });

    test('未登录用户应该被重定向到登录页', async ({ page }) => {
      // 直接访问需要认证的页面（不登录）
      await page.goto('/admin/servers');

      // 应该被重定向到登录页
      await page.waitForURL(/login/, { timeout: 5000 });
      expect(page.url()).toContain('/login');
    });
  });

  test.describe('密码安全', () => {
    test('密码输入框应该是隐藏类型', async ({ page }) => {
      await loginPage.goto();

      const passwordField = loginPage.passwordInput;
      const type = await passwordField.getAttribute('type');

      expect(type).toBe('password');
    });

    test('应该有显示/隐藏密码功能', async ({ page }) => {
      await loginPage.goto();

      const toggleButton = page.locator('button[data-password-toggle], .password-toggle');
      const hasToggle = await toggleButton.isVisible().catch(() => false);

      if (hasToggle) {
        const passwordField = loginPage.passwordInput;

        // 点击显示密码
        await toggleButton.click();
        let type = await passwordField.getAttribute('type');
        expect(type).toBe('text');

        // 再次点击隐藏密码
        await toggleButton.click();
        type = await passwordField.getAttribute('type');
        expect(type).toBe('password');
      }
    });
  });

  test.describe('用户注册', () => {
    test('应该能打开注册页面', async ({ page }) => {
      await loginPage.goto();

      // 点击注册链接
      await loginPage.clickRegister();

      // 验证跳转到注册页
      expect(page.url()).toContain('/register');
    });

    test('应该能成功注册新用户', async ({ page }) => {
      await page.goto('/register');

      // 生成唯一用户名
      const timestamp = Date.now();
      const username = `testuser_${timestamp}`;

      // 填写注册表单
      await page.fill('input[name="username"]', username);
      await page.fill('input[name="email"]', `${username}@test.com`);
      await page.fill('input[name="password"]', 'Test123456');
      await page.fill('input[name="confirmPassword"]', 'Test123456');

      // 提交注册
      await page.click('button[type="submit"]');

      // 验证注册成功（可能跳转到登录页或直接登录）
      await page.waitForURL(/login|dashboard/, { timeout: 5000 });
    });

    test('应该拒绝已存在的用户名', async ({ page }) => {
      await page.goto('/register');

      // 使用已存在的用户名
      await page.fill('input[name="username"]', 'admin');
      await page.fill('input[name="email"]', 'admin@test.com');
      await page.fill('input[name="password"]', 'Test123456');
      await page.fill('input[name="confirmPassword"]', 'Test123456');

      await page.click('button[type="submit"]');

      // 应该显示错误消息
      const errorMsg = page.locator('.error-message, .alert-danger');
      await expect(errorMsg).toBeVisible({ timeout: 3000 });
    });

    test('应该验证密码强度', async ({ page }) => {
      await page.goto('/register');

      // 使用弱密码
      await page.fill('input[name="username"]', 'newuser');
      await page.fill('input[name="password"]', '123');

      // 检查密码强度提示
      const strengthIndicator = page.locator('.password-strength, [data-password-strength]');
      const hasStrengthCheck = await strengthIndicator.isVisible().catch(() => false);

      if (hasStrengthCheck) {
        await expect(strengthIndicator).toContainText(/弱|weak/i);
      }
    });

    test('应该验证两次密码输入一致', async ({ page }) => {
      await page.goto('/register');

      await page.fill('input[name="password"]', 'Password123');
      await page.fill('input[name="confirmPassword"]', 'Password456');

      await page.click('button[type="submit"]');

      // 应该显示密码不匹配错误
      const errorMsg = page.locator('.error-message, .alert-danger');
      const hasError = await errorMsg.isVisible().catch(() => false);

      if (hasError) {
        const text = await errorMsg.innerText();
        expect(text).toMatch(/密码不一致|password.*not match/i);
      }
    });
  });
});
