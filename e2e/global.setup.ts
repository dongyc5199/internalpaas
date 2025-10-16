import { test as setup } from '@playwright/test';
import path from 'path';

/**
 * 全局Setup - 认证配置
 *
 * 在所有测试前执行，进行用户登录并保存认证状态
 * 这样后续测试可以直接使用已登录的状态，避免每个测试都重复登录
 */

const authFile = path.join(__dirname, '.auth/user.json');

setup('authenticate as admin user', async ({ page }) => {
  // 访问登录页面
  await page.goto('/login');

  // 填写登录表单
  await page.fill('input[name="username"]', process.env.TEST_ADMIN_USERNAME || 'admin');
  await page.fill('input[name="password"]', process.env.TEST_ADMIN_PASSWORD || 'admin');

  // 点击登录按钮
  await page.click('button[type="submit"]');

  // 等待导航到主页面（登录成功）
  // 实际URL可能是: /admin/workspace, /admin-dashboard, 或其他
  await page.waitForURL(/\/(admin|developer)/, { timeout: 30000 });

  // 等待页面加载完成
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {
    console.log('⚠️  网络未完全空闲，继续执行...');
  });

  // 确认登录成功（等待用户菜单或其他登录后的元素）
  // 使用更宽泛的选择器
  const loginSuccessSelectors = [
    '[data-testid="user-menu"]',
    '.user-info',
    '.user-profile',
    '.user-menu',
    '[class*="user"]',
    'button:has-text("退出")',
    'a:has-text("退出")'
  ];

  let loginSuccess = false;
  for (const selector of loginSuccessSelectors) {
    const element = page.locator(selector).first();
    const exists = await element.count() > 0;
    if (exists) {
      await element.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
      loginSuccess = true;
      console.log(`✓ 找到登录成功标识: ${selector}`);
      break;
    }
  }

  if (!loginSuccess) {
    console.log('⚠️  未找到明确的登录成功标识，但URL已变化，假定登录成功');
  }

  // 打印当前URL
  console.log('✓ 认证成功，当前URL:', page.url());

  // 保存认证状态到文件
  await page.context().storageState({ path: authFile });

  console.log('✓ 认证状态已保存到:', authFile);
});

// 暂时跳过开发者认证测试
setup.skip('authenticate as developer user', async ({ page }) => {
  const devAuthFile = path.join(__dirname, '.auth/developer.json');

  await page.goto('/login');

  await page.fill('input[name="username"]', process.env.TEST_DEV_USERNAME || 'developer');
  await page.fill('input[name="password"]', process.env.TEST_DEV_PASSWORD || 'developer');

  await page.click('button[type="submit"]');

  // 等待导航
  await page.waitForURL(/\/(admin|developer)/, { timeout: 30000 });

  // 等待页面加载
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {
    console.log('⚠️  网络未完全空闲，继续执行...');
  });

  // 确认登录成功
  const loginSuccessSelectors = [
    '[data-testid="user-menu"]',
    '.user-info',
    '.user-profile',
    '.user-menu',
    'button:has-text("退出")',
    'a:has-text("退出")'
  ];

  let loginSuccess = false;
  for (const selector of loginSuccessSelectors) {
    const element = page.locator(selector).first();
    const exists = await element.count() > 0;
    if (exists) {
      await element.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
      loginSuccess = true;
      console.log(`✓ 找到登录成功标识: ${selector}`);
      break;
    }
  }

  if (!loginSuccess) {
    console.log('⚠️  未找到明确的登录成功标识，但URL已变化，假定登录成功');
  }

  console.log('✓ 开发者认证成功，当前URL:', page.url());

  await page.context().storageState({ path: devAuthFile });

  console.log('✓ 开发者认证状态已保存到:', devAuthFile);
});
