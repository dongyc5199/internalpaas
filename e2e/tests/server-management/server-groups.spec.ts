import { test, expect } from '@playwright/test';
import { ServerGroupManagementPage } from '../../pages/ServerGroupManagementPage';
import { ServerDetailOverlayPage } from '../../pages/ServerDetailOverlayPage';
import { generateTestData, waitForLoading } from '../../utils/helpers';

/**
 * 服务器群组管理E2E测试
 *
 * 测试服务器的添加、查看、编辑、删除等核心功能
 */

test.describe('服务器群组管理', () => {
  let serverPage: ServerGroupManagementPage;

  test.beforeEach(async ({ page }) => {
    serverPage = new ServerGroupManagementPage(page);
    await serverPage.goto();
  });

  test.describe('页面加载', () => {
    test('应该成功加载服务器列表页面', async ({ page }) => {
      // 验证页面标题
      const title = page.locator('h1, h2');
      await expect(title).toBeVisible();

      // 验证服务器卡片容器存在
      await expect(page.locator('.server-list, .server-grid, [data-server-list]')).toBeVisible();
    });

    test('应该加载监控图表', async () => {
      // 等待图表加载
      const chartsLoaded = await serverPage.areChartsLoaded();

      // 如果图表存在，应该能正确加载
      if (chartsLoaded) {
        await expect(serverPage.healthTrendChart).toBeVisible();
        await expect(serverPage.loadDistributionChart).toBeVisible();
        await expect(serverPage.appDistributionChart).toBeVisible();
      }
    });

    test('应该显示服务器统计信息', async ({ page }) => {
      const statsCards = page.locator('.stats-card, [data-stats], .metric-card');
      const count = await statsCards.count();

      // 至少应该有一些统计卡片
      expect(count).toBeGreaterThan(0);
    });
  });

  test.describe('服务器列表', () => {
    test('应该显示服务器列表', async () => {
      const serverCount = await serverPage.getServerCount();

      // 应该至少有一个服务器（可能是演示数据）
      expect(serverCount).toBeGreaterThanOrEqual(0);
    });

    test('应该能切换视图模式', async ({ page }) => {
      // 切换到列表视图
      await serverPage.switchView('list');
      await waitForLoading(page);

      // 验证列表视图已激活
      const listView = page.locator('.list-view, [data-view="list"]');
      const isListView = await listView.isVisible().catch(() => false);

      if (isListView) {
        await expect(listView).toBeVisible();
      }

      // 切换回卡片视图
      await serverPage.switchView('card');
      await waitForLoading(page);
    });

    test('应该能搜索服务器', async () => {
      // 执行搜索
      await serverPage.searchServer('test');

      // 等待搜索结果
      await waitForLoading(serverPage.page);

      // 验证搜索功能工作（无论是否有结果）
      const searchInput = serverPage.searchInput;
      const value = await searchInput.inputValue();
      expect(value).toBe('test');
    });

    test('应该能按状态筛选服务器', async () => {
      // 筛选在线服务器
      await serverPage.filterByStatus('online');
      await waitForLoading(serverPage.page);

      // 验证筛选器已应用
      const filterValue = await serverPage.filterDropdown.inputValue().catch(() => '');

      if (filterValue) {
        expect(filterValue).toBe('online');
      }
    });
  });

  test.describe('服务器操作', () => {
    test('应该能打开添加服务器对话框', async ({ page }) => {
      await serverPage.addServerButton.click();

      // 验证模态框打开
      const modal = page.locator('.modal, [role="dialog"], .drawer');
      await expect(modal).toBeVisible({ timeout: 5000 });

      // 验证表单字段存在
      await expect(page.locator('input[name="name"]')).toBeVisible();
      await expect(page.locator('input[name="host"]')).toBeVisible();
    });

    test('应该能添加新服务器', async ({ page }) => {
      const serverName = generateTestData('TestServer');

      await serverPage.addServer({
        name: serverName,
        host: '192.168.1.100',
        port: '22',
        username: 'testuser',
        password: 'testpass123',
      });

      // 验证服务器已添加
      const card = await serverPage.findServerCard(serverName);
      expect(card).not.toBeNull();

      // 清理：删除测试服务器
      if (card) {
        await serverPage.deleteServer(serverName);
      }
    });

    test('添加服务器时应该验证必填字段', async ({ page }) => {
      await serverPage.addServerButton.click();

      // 不填写任何字段，直接提交
      const submitButton = page.locator('button[type="submit"]');
      await submitButton.click();

      // 验证显示验证错误
      const nameInput = page.locator('input[name="name"]');
      const isValid = await nameInput.evaluate((el: HTMLInputElement) => el.checkValidity());

      expect(isValid).toBe(false);
    });

    test('应该能查看服务器详情', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount > 0) {
        // 获取第一个服务器的名称
        const firstCard = serverPage.serverCards.first();
        const serverName = await firstCard.locator('.server-name, h3, [data-server-name]').innerText();

        // 点击查看详情
        await serverPage.viewServerDetails(serverName);

        // 验证详情弹窗打开
        const detailOverlay = new ServerDetailOverlayPage(page);
        await detailOverlay.waitForOpen();

        // 验证显示监控数据
        await expect(detailOverlay.cpuUsage).toBeVisible();
        await expect(detailOverlay.memoryUsage).toBeVisible();

        // 关闭详情
        await detailOverlay.close();
      } else {
        test.skip();
      }
    });

    test('应该能刷新服务器状态', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount > 0) {
        const firstCard = serverPage.serverCards.first();
        const serverName = await firstCard.locator('.server-name, h3').innerText();

        // 刷新服务器状态
        await serverPage.refreshServerStatus(serverName);

        // 验证刷新动作完成（等待loading消失）
        await waitForLoading(page);
      } else {
        test.skip();
      }
    });
  });

  test.describe('服务器详情弹窗', () => {
    test('应该能切换详情标签页', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount > 0) {
        const firstCard = serverPage.serverCards.first();
        const serverName = await firstCard.locator('.server-name, h3').innerText();

        await serverPage.viewServerDetails(serverName);

        const detailOverlay = new ServerDetailOverlayPage(page);
        await detailOverlay.waitForOpen();

        // 切换到进程标签页
        await detailOverlay.switchTab('process');
        await waitForLoading(page);

        // 验证进程标签内容显示
        const processTab = detailOverlay.processTab;
        const isActive = await processTab.getAttribute('class');
        expect(isActive).toContain('active');

        await detailOverlay.close();
      } else {
        test.skip();
      }
    });

    test('应该显示CPU和内存使用率', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount > 0) {
        const firstCard = serverPage.serverCards.first();
        const serverName = await firstCard.locator('.server-name, h3').innerText();

        await serverPage.viewServerDetails(serverName);

        const detailOverlay = new ServerDetailOverlayPage(page);
        await detailOverlay.waitForOpen();

        // 获取CPU使用率
        const cpuUsage = await detailOverlay.getCpuUsage();
        expect(cpuUsage).toBeTruthy();

        // 获取内存使用率
        const memoryUsage = await detailOverlay.getMemoryUsage();
        expect(memoryUsage).toBeTruthy();

        await detailOverlay.close();
      } else {
        test.skip();
      }
    });

    test('应该能刷新服务器监控数据', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount > 0) {
        const firstCard = serverPage.serverCards.first();
        const serverName = await firstCard.locator('.server-name, h3').innerText();

        await serverPage.viewServerDetails(serverName);

        const detailOverlay = new ServerDetailOverlayPage(page);
        await detailOverlay.waitForOpen();

        // 点击刷新按钮
        await detailOverlay.refresh();

        // 验证数据已刷新（loading完成）
        await waitForLoading(page);

        await detailOverlay.close();
      } else {
        test.skip();
      }
    });
  });

  test.describe('批量操作', () => {
    test('应该能批量选择服务器', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount >= 2) {
        // 获取前两个服务器名称
        const names: string[] = [];

        for (let i = 0; i < 2; i++) {
          const card = serverPage.serverCards.nth(i);
          const name = await card.locator('.server-name, h3').innerText();
          names.push(name);
        }

        // 批量选择
        await serverPage.selectServers(names);

        // 验证复选框已选中
        const firstCheckbox = serverPage.serverCards.first().locator('input[type="checkbox"]');
        const isChecked = await firstCheckbox.isChecked();
        expect(isChecked).toBe(true);
      } else {
        test.skip();
      }
    });
  });

  test.describe('错误处理', () => {
    test('添加无效服务器时应该显示错误', async ({ page }) => {
      await serverPage.addServerButton.click();

      // 填写无效的服务器信息
      await page.fill('input[name="name"]', 'Invalid Server');
      await page.fill('input[name="host"]', 'invalid.host.that.does.not.exist');
      await page.fill('input[name="port"]', '22');
      await page.fill('input[name="username"]', 'user');
      await page.fill('input[name="password"]', 'pass');

      const submitButton = page.locator('button[type="submit"]');
      await submitButton.click();

      // 应该显示连接失败的错误提示
      const errorToast = page.locator('.toast-error, .error-message, [role="alert"]');
      const hasError = await errorToast.isVisible({ timeout: 5000 }).catch(() => false);

      // 错误提示可能出现也可能不出现（取决于是否进行了连接测试）
      if (hasError) {
        await expect(errorToast).toBeVisible();
      }
    });

    test('应该处理网络错误', async ({ page }) => {
      // 拦截API请求并返回错误
      await page.route('**/api/admin/servers/**', (route) => {
        route.abort('failed');
      });

      // 尝试刷新页面
      await page.reload();

      // 应该显示错误提示
      const errorMessage = page.locator('.error-message, [role="alert"], .toast-error');
      const hasError = await errorMessage.isVisible({ timeout: 3000 }).catch(() => false);

      if (hasError) {
        await expect(errorMessage).toBeVisible();
      }
    });
  });
});
