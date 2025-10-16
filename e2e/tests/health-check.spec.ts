import { test, expect } from '@playwright/test';

/**
 * 健康检查测试 - 用于诊断E2E测试环境
 */

test.describe('健康检查', () => {
  test('应该能访问应用首页', async ({ page }) => {
    // 设置较长的超时时间
    test.setTimeout(60000);

    console.log('🔍 正在访问:', process.env.BASE_URL || 'http://localhost:8080');

    // 访问首页
    const response = await page.goto('/', {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    });

    console.log('✅ HTTP状态码:', response?.status());

    // 验证响应成功
    expect(response?.status()).toBe(200);

    // 截图保存
    await page.screenshot({ path: 'test-results/homepage.png', fullPage: true });
    console.log('📸 截图已保存: test-results/homepage.png');

    // 打印页面标题
    const title = await page.title();
    console.log('📄 页面标题:', title);
  });

  test('应该能访问登录页面', async ({ page }) => {
    test.setTimeout(60000);

    console.log('🔍 正在访问登录页面...');

    const response = await page.goto('/login', {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    });

    console.log('✅ HTTP状态码:', response?.status());

    // 验证响应成功
    expect(response?.status()).toBe(200);

    // 截图
    await page.screenshot({ path: 'test-results/login-page.png', fullPage: true });
    console.log('📸 截图已保存: test-results/login-page.png');

    // 打印页面内容（部分）
    const content = await page.content();
    console.log('📄 页面包含"登录"文本:', content.includes('登录') || content.includes('login'));
    console.log('📄 页面包含username输入框:', content.includes('username'));
    console.log('📄 页面包含password输入框:', content.includes('password'));

    // 查找关键元素
    const usernameInput = page.locator('input[name="username"], input[id="username"], input[type="text"]').first();
    const passwordInput = page.locator('input[name="password"], input[id="password"], input[type="password"]').first();

    const hasUsername = await usernameInput.count() > 0;
    const hasPassword = await passwordInput.count() > 0;

    console.log('🔍 找到用户名输入框:', hasUsername);
    console.log('🔍 找到密码输入框:', hasPassword);

    if (hasUsername) {
      const usernameSelector = await usernameInput.first().evaluate(el => {
        return `${el.tagName.toLowerCase()}${el.name ? `[name="${el.name}"]` : ''}${el.id ? `[id="${el.id}"]` : ''}`;
      });
      console.log('✅ 用户名输入框选择器:', usernameSelector);
    }

    if (hasPassword) {
      const passwordSelector = await passwordInput.first().evaluate(el => {
        return `${el.tagName.toLowerCase()}${el.name ? `[name="${el.name}"]` : ''}${el.id ? `[id="${el.id}"]` : ''}`;
      });
      console.log('✅ 密码输入框选择器:', passwordSelector);
    }

    // 查找提交按钮
    const submitButton = page.locator('button[type="submit"], input[type="submit"], button:has-text("登录")').first();
    const hasSubmit = await submitButton.count() > 0;
    console.log('🔍 找到提交按钮:', hasSubmit);
  });

  test('应该能填写登录表单（不提交）', async ({ page }) => {
    test.setTimeout(60000);

    await page.goto('/login', { waitUntil: 'domcontentloaded' });

    console.log('🔍 尝试填写表单...');

    // 使用更宽泛的选择器
    const usernameInput = page.locator('input[name="username"], input[id="username"]').first();
    const passwordInput = page.locator('input[name="password"], input[id="password"]').first();

    // 等待元素可见
    await usernameInput.waitFor({ state: 'visible', timeout: 10000 });
    await passwordInput.waitFor({ state: 'visible', timeout: 10000 });

    console.log('✅ 输入框已可见');

    // 填写表单
    await usernameInput.fill('root');
    await passwordInput.fill('admin123');

    console.log('✅ 表单填写成功');

    // 截图
    await page.screenshot({ path: 'test-results/login-form-filled.png', fullPage: true });
    console.log('📸 截图已保存: test-results/login-form-filled.png');

    // 验证值已填入
    const usernameValue = await usernameInput.inputValue();
    const passwordValue = await passwordInput.inputValue();

    console.log('📝 用户名值:', usernameValue);
    console.log('📝 密码值:', passwordValue ? '***' : '(空)');

    expect(usernameValue).toBe('root');
    expect(passwordValue).toBe('admin123');
  });
});
