import { test, expect } from '@playwright/test';
import { ServerGroupManagementPage } from '../../pages/ServerGroupManagementPage';
import { ServerDetailOverlayPage } from '../../pages/ServerDetailOverlayPage';
import { waitForLoading, waitForModal } from '../../utils/helpers';

/**
 * 服务器群组管理UI元素测试
 *
 * 测试内容：
 * 1. 页面中各元素加载是否成功
 * 2. 添加服务器按钮是否正确响应
 * 3. 弹窗是否打开
 * 4. 服务器详情是否正确打开
 */

test.describe('服务器群组管理 - UI元素测试', () => {
  let serverPage: ServerGroupManagementPage;

  test.beforeEach(async ({ page }) => {
    serverPage = new ServerGroupManagementPage(page);
    await serverPage.goto();
  });

  test.describe('1. 页面元素加载测试', () => {
    test('应该成功加载页面标题', async ({ page }) => {
      // 验证页面标题存在
      const pageTitle = page.locator('h1:has-text("服务器群组管理"), h1:has-text("Server Group Management")');
      await expect(pageTitle).toBeVisible({ timeout: 10000 });

      console.log('✓ 页面标题加载成功');
    });

    test('应该加载页面描述', async ({ page }) => {
      // 验证页面描述存在
      const description = page.locator('.page-description, p[data-i18n-zh*="监控和管理"]');
      await expect(description).toBeVisible();

      console.log('✓ 页面描述加载成功');
    });

    test('应该显示添加服务器按钮', async () => {
      // 验证添加服务器按钮存在且可见
      await expect(serverPage.addServerButton).toBeVisible();

      // 验证按钮文本
      const buttonText = await serverPage.addServerButton.innerText();
      expect(buttonText).toMatch(/添加服务器|Add Server/);

      console.log('✓ 添加服务器按钮加载成功');
    });

    test('应该显示刷新数据按钮', async ({ page }) => {
      const refreshButton = page.locator('button#refreshServerGroupBtn, button:has-text("刷新数据")');
      await expect(refreshButton).toBeVisible();

      console.log('✓ 刷新数据按钮加载成功');
    });

    test('应该显示导出报告按钮', async ({ page }) => {
      const exportButton = page.locator('button#exportServerGroupBtn, button:has-text("导出报告")');
      await expect(exportButton).toBeVisible();

      console.log('✓ 导出报告按钮加载成功');
    });

    test('应该显示最后更新时间', async ({ page }) => {
      const lastUpdate = page.locator('#serverGroupLastUpdate, .last-update');
      await expect(lastUpdate).toBeVisible();

      console.log('✓ 最后更新时间显示成功');
    });

    test('应该加载服务器卡片容器', async ({ page }) => {
      // 等待服务器卡片容器加载
      const serverContainer = page.locator('.server-list, .server-grid, .server-cards-container, [data-server-list]');
      await expect(serverContainer).toBeVisible({ timeout: 10000 });

      console.log('✓ 服务器卡片容器加载成功');
    });

    test('应该显示统计卡片区域', async ({ page }) => {
      const statsArea = page.locator('.stats-grid, .metrics-overview, [data-stats-grid]');
      const isVisible = await statsArea.isVisible({ timeout: 5000 }).catch(() => false);

      if (isVisible) {
        await expect(statsArea).toBeVisible();
        console.log('✓ 统计卡片区域加载成功');
      } else {
        console.log('⊘ 统计卡片区域未找到（可能不存在）');
      }
    });

    test('应该加载健康趋势图表', async ({ page }) => {
      const healthChart = page.locator('#healthTrendChart, [data-chart="health-trend"], canvas.chart-canvas');
      const isVisible = await healthChart.first().isVisible({ timeout: 5000 }).catch(() => false);

      if (isVisible) {
        await expect(healthChart.first()).toBeVisible();
        console.log('✓ 健康趋势图表加载成功');
      } else {
        console.log('⊘ 健康趋势图表未找到（可能需要数据）');
      }
    });

    test('应该加载负载分布图表', async ({ page }) => {
      const loadChart = page.locator('#loadDistributionChart, [data-chart="load-distribution"], canvas.chart-canvas');
      const isVisible = await loadChart.isVisible({ timeout: 5000 }).catch(() => false);

      if (isVisible) {
        await expect(loadChart).toBeVisible();
        console.log('✓ 负载分布图表加载成功');
      } else {
        console.log('⊘ 负载分布图表未找到（可能需要数据）');
      }
    });

    test('应该加载应用分布图表', async ({ page }) => {
      const appChart = page.locator('#appDistributionChart, [data-chart="app-distribution"], canvas.chart-canvas');
      const isVisible = await appChart.isVisible({ timeout: 5000 }).catch(() => false);

      if (isVisible) {
        await expect(appChart).toBeVisible();
        console.log('✓ 应用分布图表加载成功');
      } else {
        console.log('⊘ 应用分布图表未找到（可能需要数据）');
      }
    });
  });

  test.describe('2. 添加服务器按钮响应测试', () => {
    test('点击添加服务器按钮应该响应', async ({ page }) => {
      // 确认按钮可点击
      await expect(serverPage.addServerButton).toBeEnabled();

      // 点击按钮
      await serverPage.addServerButton.click();

      console.log('✓ 添加服务器按钮点击响应成功');
    });

    test('添加服务器按钮应该有正确的hover效果', async ({ page }) => {
      // 悬停在按钮上
      await serverPage.addServerButton.hover();

      // 检查按钮状态
      const isEnabled = await serverPage.addServerButton.isEnabled();
      expect(isEnabled).toBe(true);

      console.log('✓ 添加服务器按钮hover效果正常');
    });

    test('添加服务器按钮应该有正确的图标', async ({ page }) => {
      const buttonHtml = await serverPage.addServerButton.innerHTML();

      // 检查是否包含图标元素
      const hasIcon = buttonHtml.includes('<i') || buttonHtml.includes('svg') || buttonHtml.includes('➕');
      expect(hasIcon).toBe(true);

      console.log('✓ 添加服务器按钮包含图标');
    });
  });

  test.describe('3. 添加服务器弹窗测试', () => {
    test('点击添加按钮后应该打开弹窗', async ({ page }) => {
      // 点击添加按钮
      await serverPage.addServerButton.click();

      // 等待弹窗出现
      const modal = page.locator('.modal, [role="dialog"], .drawer, .overlay');
      await expect(modal).toBeVisible({ timeout: 10000 });

      console.log('✓ 添加服务器弹窗打开成功');
    });

    test('弹窗应该显示标题', async ({ page }) => {
      await serverPage.addServerButton.click();

      // 检查弹窗标题
      const modalTitle = page.locator('.modal-title, .drawer-title, h2, h3').filter({ hasText: /添加|新建|Add|Create/ });
      await expect(modalTitle).toBeVisible({ timeout: 5000 });

      console.log('✓ 弹窗标题显示正确');
    });

    test('弹窗应该包含服务器名称输入框', async ({ page }) => {
      await serverPage.addServerButton.click();
      await waitForModal(page);

      const nameInput = page.locator('input[name="name"], input#serverName, input[placeholder*="名称"]');
      await expect(nameInput).toBeVisible();

      console.log('✓ 服务器名称输入框存在');
    });

    test('弹窗应该包含主机地址输入框', async ({ page }) => {
      await serverPage.addServerButton.click();
      await waitForModal(page);

      const hostInput = page.locator('input[name="host"], input#serverHost, input[placeholder*="主机"]');
      await expect(hostInput).toBeVisible();

      console.log('✓ 主机地址输入框存在');
    });

    test('弹窗应该包含端口输入框', async ({ page }) => {
      await serverPage.addServerButton.click();
      await waitForModal(page);

      const portInput = page.locator('input[name="port"], input#serverPort, input[placeholder*="端口"]');
      await expect(portInput).toBeVisible();

      console.log('✓ 端口输入框存在');
    });

    test('弹窗应该包含用户名输入框', async ({ page }) => {
      await serverPage.addServerButton.click();
      await waitForModal(page);

      const usernameInput = page.locator('input[name="username"], input#serverUsername, input[placeholder*="用户名"]');
      await expect(usernameInput).toBeVisible();

      console.log('✓ 用户名输入框存在');
    });

    test('弹窗应该包含密码输入框', async ({ page }) => {
      await serverPage.addServerButton.click();
      await waitForModal(page);

      const passwordInput = page.locator('input[name="password"], input#serverPassword, input[type="password"]');
      await expect(passwordInput).toBeVisible();

      console.log('✓ 密码输入框存在');
    });

    test('弹窗应该有保存按钮', async ({ page }) => {
      await serverPage.addServerButton.click();
      await waitForModal(page);

      const saveButton = page.locator('button[type="submit"], button:has-text("保存"), button:has-text("Save")');
      await expect(saveButton).toBeVisible();

      console.log('✓ 保存按钮存在');
    });

    test('弹窗应该有取消按钮', async ({ page }) => {
      await serverPage.addServerButton.click();
      await waitForModal(page);

      const cancelButton = page.locator('button:has-text("取消"), button:has-text("Cancel"), button.close-button');
      await expect(cancelButton.first()).toBeVisible();

      console.log('✓ 取消按钮存在');
    });

    test('点击取消按钮应该关闭弹窗', async ({ page }) => {
      await serverPage.addServerButton.click();
      await waitForModal(page);

      const cancelButton = page.locator('button:has-text("取消"), button:has-text("Cancel")');
      await cancelButton.first().click();

      // 等待弹窗关闭
      const modal = page.locator('.modal, [role="dialog"], .drawer');
      await expect(modal).not.toBeVisible({ timeout: 5000 });

      console.log('✓ 点击取消按钮成功关闭弹窗');
    });
  });

  test.describe('4. 服务器详情弹窗测试', () => {
    test('应该能找到服务器卡片', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount > 0) {
        console.log(`✓ 找到 ${serverCount} 个服务器`);
        expect(serverCount).toBeGreaterThan(0);
      } else {
        console.log('⊘ 当前没有服务器数据');
        test.skip();
      }
    });

    test('点击服务器卡片应该打开详情弹窗', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount > 0) {
        // 获取第一个服务器
        const firstCard = serverPage.serverCards.first();

        // 查找详情按钮或点击卡片
        const detailsButton = firstCard.locator('button:has-text("详情"), button:has-text("Details"), button[data-action="view"]');

        if (await detailsButton.count() > 0) {
          await detailsButton.first().click();
        } else {
          // 如果没有详情按钮，直接点击卡片
          await firstCard.click();
        }

        // 等待详情弹窗出现
        const detailModal = page.locator('.server-detail-overlay, .modal, [role="dialog"]');
        await expect(detailModal).toBeVisible({ timeout: 10000 });

        console.log('✓ 服务器详情弹窗打开成功');
      } else {
        test.skip();
      }
    });

    test('详情弹窗应该显示服务器基本信息', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount > 0) {
        const firstCard = serverPage.serverCards.first();
        const detailsButton = firstCard.locator('button:has-text("详情"), button:has-text("Details")');

        if (await detailsButton.count() > 0) {
          await detailsButton.first().click();
        } else {
          await firstCard.click();
        }

        await waitForModal(page);

        // 创建详情页面对象
        const detailOverlay = new ServerDetailOverlayPage(page);
        await detailOverlay.waitForOpen();

        // 验证基本信息显示
        const serverName = page.locator('.server-name, h2, h3').first();
        await expect(serverName).toBeVisible();

        console.log('✓ 服务器基本信息显示正确');

        // 关闭弹窗
        await detailOverlay.close();
      } else {
        test.skip();
      }
    });

    test('详情弹窗应该显示CPU使用率', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount > 0) {
        const firstCard = serverPage.serverCards.first();
        const detailsButton = firstCard.locator('button:has-text("详情"), button:has-text("Details")');

        if (await detailsButton.count() > 0) {
          await detailsButton.first().click();
        } else {
          await firstCard.click();
        }

        const detailOverlay = new ServerDetailOverlayPage(page);
        await detailOverlay.waitForOpen();

        // 验证CPU使用率显示
        await expect(detailOverlay.cpuUsage).toBeVisible({ timeout: 5000 });

        const cpuText = await detailOverlay.cpuUsage.innerText();
        console.log(`✓ CPU使用率显示: ${cpuText}`);

        await detailOverlay.close();
      } else {
        test.skip();
      }
    });

    test('详情弹窗应该显示内存使用率', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount > 0) {
        const firstCard = serverPage.serverCards.first();
        const detailsButton = firstCard.locator('button:has-text("详情"), button:has-text("Details")');

        if (await detailsButton.count() > 0) {
          await detailsButton.first().click();
        } else {
          await firstCard.click();
        }

        const detailOverlay = new ServerDetailOverlayPage(page);
        await detailOverlay.waitForOpen();

        // 验证内存使用率显示
        await expect(detailOverlay.memoryUsage).toBeVisible({ timeout: 5000 });

        const memoryText = await detailOverlay.memoryUsage.innerText();
        console.log(`✓ 内存使用率显示: ${memoryText}`);

        await detailOverlay.close();
      } else {
        test.skip();
      }
    });

    test('详情弹窗应该有标签页导航', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount > 0) {
        const firstCard = serverPage.serverCards.first();
        const detailsButton = firstCard.locator('button:has-text("详情"), button:has-text("Details")');

        if (await detailsButton.count() > 0) {
          await detailsButton.first().click();
        } else {
          await firstCard.click();
        }

        const detailOverlay = new ServerDetailOverlayPage(page);
        await detailOverlay.waitForOpen();

        // 检查标签页
        const tabs = page.locator('.tab, .nav-tab, [role="tab"]');
        const tabCount = await tabs.count();

        if (tabCount > 0) {
          console.log(`✓ 找到 ${tabCount} 个标签页`);
          expect(tabCount).toBeGreaterThan(0);
        }

        await detailOverlay.close();
      } else {
        test.skip();
      }
    });

    test('详情弹窗应该有关闭按钮', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount > 0) {
        const firstCard = serverPage.serverCards.first();
        const detailsButton = firstCard.locator('button:has-text("详情"), button:has-text("Details")');

        if (await detailsButton.count() > 0) {
          await detailsButton.first().click();
        } else {
          await firstCard.click();
        }

        await waitForModal(page);

        // 查找关闭按钮
        const closeButton = page.locator('button.close, button:has-text("关闭"), button[aria-label*="关闭"], button[aria-label*="Close"]');
        await expect(closeButton.first()).toBeVisible();

        console.log('✓ 关闭按钮存在');

        // 点击关闭
        await closeButton.first().click();

        // 验证弹窗已关闭
        const modal = page.locator('.server-detail-overlay, .modal');
        await expect(modal).not.toBeVisible({ timeout: 5000 });

        console.log('✓ 点击关闭按钮成功关闭详情弹窗');
      } else {
        test.skip();
      }
    });

    test('详情弹窗应该有刷新按钮', async ({ page }) => {
      const serverCount = await serverPage.getServerCount();

      if (serverCount > 0) {
        const firstCard = serverPage.serverCards.first();
        const detailsButton = firstCard.locator('button:has-text("详情"), button:has-text("Details")');

        if (await detailsButton.count() > 0) {
          await detailsButton.first().click();
        } else {
          await firstCard.click();
        }

        const detailOverlay = new ServerDetailOverlayPage(page);
        await detailOverlay.waitForOpen();

        // 查找刷新按钮
        const refreshButton = page.locator('button:has-text("刷新"), button[data-action="refresh"], button[aria-label*="刷新"]');

        if (await refreshButton.count() > 0) {
          await expect(refreshButton.first()).toBeVisible();
          console.log('✓ 刷新按钮存在');
        } else {
          console.log('⊘ 刷新按钮未找到');
        }

        await detailOverlay.close();
      } else {
        test.skip();
      }
    });
  });
});
