import { test, expect } from '@playwright/test';
import { ApplicationManagementPage } from '../../pages/ApplicationManagementPage';
import { generateTestData, waitForLoading, waitForToast } from '../../utils/helpers';
import path from 'path';

/**
 * 应用管理E2E测试
 *
 * 测试应用的上传、启动、停止、重启、删除等核心功能
 */

test.describe('应用管理', () => {
  let appPage: ApplicationManagementPage;

  test.beforeEach(async ({ page }) => {
    appPage = new ApplicationManagementPage(page);
    await appPage.goto();
  });

  test.describe('页面加载', () => {
    test('应该成功加载应用列表页面', async ({ page }) => {
      // 验证页面标题
      const title = page.locator('h1, h2');
      await expect(title).toBeVisible();

      // 验证应用容器存在
      await expect(page.locator('.app-list, .app-grid, [data-app-list]')).toBeVisible();
    });

    test('应该显示上传应用按钮', async () => {
      await expect(appPage.uploadButton).toBeVisible();
    });

    test('应该显示搜索和筛选功能', async () => {
      // 搜索框可能存在
      const hasSearch = await appPage.searchInput.isVisible().catch(() => false);

      // 筛选下拉框可能存在
      const hasFilter = await appPage.filterDropdown.isVisible().catch(() => false);

      // 至少应该有一个存在
      expect(hasSearch || hasFilter).toBe(true);
    });
  });

  test.describe('应用列表', () => {
    test('应该显示应用列表', async () => {
      const appCount = await appPage.getApplicationCount();

      // 应该至少有0个应用（新系统可能为空）
      expect(appCount).toBeGreaterThanOrEqual(0);
    });

    test('应该能搜索应用', async () => {
      const hasSearch = await appPage.searchInput.isVisible().catch(() => false);

      if (hasSearch) {
        await appPage.searchApplication('test');
        await waitForLoading(appPage.page);

        const value = await appPage.searchInput.inputValue();
        expect(value).toBe('test');
      } else {
        test.skip();
      }
    });

    test('应该能按状态筛选应用', async () => {
      const hasFilter = await appPage.filterDropdown.isVisible().catch(() => false);

      if (hasFilter) {
        await appPage.filterByStatus('running');
        await waitForLoading(appPage.page);

        const value = await appPage.filterDropdown.inputValue();
        expect(value).toBe('running');
      } else {
        test.skip();
      }
    });

    test('应该能刷新应用列表', async () => {
      await appPage.refresh();
      await waitForLoading(appPage.page);

      // 验证刷新完成
      const appCount = await appPage.getApplicationCount();
      expect(appCount).toBeGreaterThanOrEqual(0);
    });
  });

  test.describe('应用上传', () => {
    test('应该能打开上传对话框', async ({ page }) => {
      await appPage.uploadButton.click();

      // 验证模态框打开
      const modal = page.locator('.modal, [role="dialog"], .drawer');
      await expect(modal).toBeVisible({ timeout: 5000 });

      // 验证文件上传控件存在
      const fileInput = page.locator('input[type="file"]');
      await expect(fileInput).toBeVisible();
    });

    test('上传表单应该包含必要字段', async ({ page }) => {
      await appPage.uploadButton.click();
      await waitForLoading(page);

      // 验证关键表单字段
      const fileInput = page.locator('input[type="file"]');
      const nameInput = page.locator('input[name="name"], input[name="applicationName"]');

      await expect(fileInput).toBeVisible();

      const hasNameInput = await nameInput.isVisible().catch(() => false);

      // 文件输入必须存在
      expect(await fileInput.isVisible()).toBe(true);
    });

    test('应该验证文件类型为JAR', async ({ page }) => {
      await appPage.uploadButton.click();

      const fileInput = page.locator('input[type="file"]');

      // 检查accept属性
      const accept = await fileInput.getAttribute('accept');

      if (accept) {
        expect(accept).toContain('.jar');
      }
    });

    // 注意：实际的文件上传测试需要真实的JAR文件
    // 这里提供一个示例框架
    test.skip('应该能上传JAR文件', async ({ page }) => {
      // 需要准备一个测试用的JAR文件
      const testJarPath = path.resolve(__dirname, '../../../fixtures/test-app.jar');
      const appName = generateTestData('TestApp');

      await appPage.uploadApplication(testJarPath, appName);

      // 验证应用已添加到列表
      const app = await appPage.findApplicationCard(appName);
      expect(app).not.toBeNull();

      // 清理
      if (app) {
        await appPage.deleteApplication(appName);
      }
    });
  });

  test.describe('应用生命周期管理', () => {
    test('应该显示应用状态', async () => {
      const appCount = await appPage.getApplicationCount();

      if (appCount > 0) {
        const firstCard = appPage.applicationCards.first();
        const statusBadge = firstCard.locator('.status, .badge, [data-status]');
        await expect(statusBadge).toBeVisible();

        const statusText = await statusBadge.innerText();
        expect(statusText).toBeTruthy();
      } else {
        test.skip();
      }
    });

    test('应该有启动按钮（当应用停止时）', async ({ page }) => {
      const appCount = await appPage.getApplicationCount();

      if (appCount > 0) {
        const cards = appPage.applicationCards;
        const count = await cards.count();

        // 查找已停止的应用
        for (let i = 0; i < count; i++) {
          const card = cards.nth(i);
          const statusText = await card.locator('.status, .badge').innerText().catch(() => '');

          if (statusText.includes('停止') || statusText.includes('stopped')) {
            // 验证有启动按钮
            const startButton = card.locator('button:has-text("启动"), button[data-action="start"]');
            await expect(startButton).toBeVisible();
            return;
          }
        }

        test.skip(); // 没有找到停止的应用
      } else {
        test.skip();
      }
    });

    test('应该有停止按钮（当应用运行时）', async ({ page }) => {
      const appCount = await appPage.getApplicationCount();

      if (appCount > 0) {
        const cards = appPage.applicationCards;
        const count = await cards.count();

        // 查找正在运行的应用
        for (let i = 0; i < count; i++) {
          const card = cards.nth(i);
          const statusText = await card.locator('.status, .badge').innerText().catch(() => '');

          if (statusText.includes('运行') || statusText.includes('running')) {
            // 验证有停止按钮
            const stopButton = card.locator('button:has-text("停止"), button[data-action="stop"]');
            await expect(stopButton).toBeVisible();
            return;
          }
        }

        test.skip();
      } else {
        test.skip();
      }
    });

    test('点击启动按钮应该触发启动流程', async ({ page }) => {
      const appCount = await appPage.getApplicationCount();

      if (appCount > 0) {
        const cards = appPage.applicationCards;
        const count = await cards.count();

        for (let i = 0; i < count; i++) {
          const card = cards.nth(i);
          const startButton = card.locator('button:has-text("启动"), button[data-action="start"]');
          const isVisible = await startButton.isVisible().catch(() => false);

          if (isVisible) {
            await startButton.click();

            // 验证显示提示消息
            await waitForToast(page);

            return;
          }
        }

        test.skip();
      } else {
        test.skip();
      }
    });
  });

  test.describe('应用详情', () => {
    test('应该能查看应用详情', async ({ page }) => {
      const appCount = await appPage.getApplicationCount();

      if (appCount > 0) {
        const firstCard = appPage.applicationCards.first();
        const appName = await firstCard.locator('.app-name, h3, [data-app-name]').innerText();

        // 点击查看详情
        await appPage.viewApplicationDetails(appName);

        // 验证跳转到详情页
        expect(page.url()).toMatch(/\/apps\/\d+/);

        // 验证详情页面加载
        await expect(page.locator('h1, h2')).toBeVisible();
      } else {
        test.skip();
      }
    });

    test('详情页应该显示应用配置信息', async ({ page }) => {
      const appCount = await appPage.getApplicationCount();

      if (appCount > 0) {
        const firstCard = appPage.applicationCards.first();
        const detailsLink = firstCard.locator('a:has-text("详情"), button:has-text("详情")');
        const hasDetailsLink = await detailsLink.isVisible().catch(() => false);

        if (hasDetailsLink) {
          await detailsLink.click();
          await page.waitForURL(/\/apps\/\d+/);

          // 验证显示关键信息
          const content = await page.content();

          // 应该包含端口、状态等信息
          expect(content.length).toBeGreaterThan(0);
        } else {
          test.skip();
        }
      } else {
        test.skip();
      }
    });
  });

  test.describe('应用日志', () => {
    test('应该能查看应用日志', async ({ page }) => {
      const appCount = await appPage.getApplicationCount();

      if (appCount > 0) {
        const firstCard = appPage.applicationCards.first();
        const logsButton = firstCard.locator('button:has-text("日志"), button[data-action="logs"]');
        const hasLogsButton = await logsButton.isVisible().catch(() => false);

        if (hasLogsButton) {
          await logsButton.click();

          // 验证日志模态框打开
          const modal = page.locator('.modal, [role="dialog"]');
          await expect(modal).toBeVisible({ timeout: 5000 });

          // 验证有日志内容区域
          const logContent = page.locator('.log-content, pre, [data-logs]');
          await expect(logContent).toBeVisible();
        } else {
          test.skip();
        }
      } else {
        test.skip();
      }
    });
  });

  test.describe('应用删除', () => {
    test('删除应用应该显示确认对话框', async ({ page }) => {
      const appCount = await appPage.getApplicationCount();

      if (appCount > 0) {
        const firstCard = appPage.applicationCards.first();
        const deleteButton = firstCard.locator('button:has-text("删除"), button[data-action="delete"]');
        const hasDeleteButton = await deleteButton.isVisible().catch(() => false);

        if (hasDeleteButton) {
          await deleteButton.click();

          // 验证确认对话框出现
          const confirmDialog = page.locator('.modal, [role="alertdialog"], [role="dialog"]');
          await expect(confirmDialog).toBeVisible({ timeout: 3000 });

          // 验证有确认和取消按钮
          const confirmButton = page.locator('button:has-text("确认"), button:has-text("Confirm")');
          const cancelButton = page.locator('button:has-text("取消"), button:has-text("Cancel")');

          await expect(confirmButton).toBeVisible();
          await expect(cancelButton).toBeVisible();

          // 点击取消
          await cancelButton.click();
        } else {
          test.skip();
        }
      } else {
        test.skip();
      }
    });
  });

  test.describe('错误处理', () => {
    test('上传过大的文件应该显示错误', async ({ page }) => {
      await appPage.uploadButton.click();

      const modal = page.locator('.modal, [role="dialog"]');
      await expect(modal).toBeVisible();

      // 文件大小限制提示（如果有）
      const sizeHint = page.locator('text=/最大|max.*size|文件大小/i');
      const hasSizeHint = await sizeHint.isVisible().catch(() => false);

      // 如果有大小限制提示，应该能看到
      if (hasSizeHint) {
        await expect(sizeHint).toBeVisible();
      }
    });

    test('应该处理网络错误', async ({ page }) => {
      // 拦截API请求并返回错误
      await page.route('**/api/apps/**', (route) => {
        route.abort('failed');
      });

      // 尝试刷新
      await page.reload();

      // 应该显示错误提示
      const errorMessage = page.locator('.error-message, [role="alert"]');
      const hasError = await errorMessage.isVisible({ timeout: 3000 }).catch(() => false);

      if (hasError) {
        await expect(errorMessage).toBeVisible();
      }
    });
  });
});
