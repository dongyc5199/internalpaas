import { Page, Locator } from '@playwright/test';
import { waitForLoading, waitForModal, waitForToast, closeModal } from '../utils/helpers';

/**
 * 服务器群组管理页面对象模型
 */
export class ServerGroupManagementPage {
  readonly page: Page;

  // 页面元素
  readonly serverCards: Locator;
  readonly addServerButton: Locator;
  readonly refreshButton: Locator;
  readonly viewToggleButtons: Locator;
  readonly searchInput: Locator;
  readonly filterDropdown: Locator;

  // 图表元素
  readonly healthTrendChart: Locator;
  readonly loadDistributionChart: Locator;
  readonly appDistributionChart: Locator;

  constructor(page: Page) {
    this.page = page;

    // 初始化页面元素定位器
    this.serverCards = page.locator('.server-card, [data-testid="server-card"]');
    this.addServerButton = page.locator('button:has-text("添加服务器"), button:has-text("Add Server")');
    this.refreshButton = page.locator('button:has-text("刷新"), button[data-action="refresh"]');
    this.viewToggleButtons = page.locator('.view-toggle button, [data-view-toggle]');
    this.searchInput = page.locator('input[type="search"], input[placeholder*="搜索"]');
    this.filterDropdown = page.locator('select[name="filter"], .filter-dropdown');

    // 图表元素
    this.healthTrendChart = page.locator('#healthTrendChart, [data-chart="health-trend"]');
    this.loadDistributionChart = page.locator('#loadDistributionChart, [data-chart="load-distribution"]');
    this.appDistributionChart = page.locator('#appDistributionChart, [data-chart="app-distribution"]');
  }

  /**
   * 导航到服务器群组管理页面
   */
  async goto() {
    await this.page.goto('/admin/servers');
    await waitForLoading(this.page);
  }

  /**
   * 获取服务器卡片数量
   */
  async getServerCount(): Promise<number> {
    await this.serverCards.first().waitFor({ timeout: 5000 }).catch(() => {});
    return await this.serverCards.count();
  }

  /**
   * 根据名称查找服务器卡片
   */
  async findServerCard(serverName: string): Promise<Locator | null> {
    const cards = this.serverCards;
    const count = await cards.count();

    for (let i = 0; i < count; i++) {
      const card = cards.nth(i);
      const text = await card.innerText();

      if (text.includes(serverName)) {
        return card;
      }
    }

    return null;
  }

  /**
   * 点击服务器卡片查看详情
   */
  async viewServerDetails(serverName: string) {
    const card = await this.findServerCard(serverName);

    if (!card) {
      throw new Error(`找不到服务器: ${serverName}`);
    }

    // 点击卡片或详情按钮
    const detailsButton = card.locator('button:has-text("详情"), button:has-text("Details")');

    if (await detailsButton.isVisible()) {
      await detailsButton.click();
    } else {
      await card.click();
    }

    await waitForModal(this.page);
  }

  /**
   * 添加新服务器
   */
  async addServer(serverData: {
    name: string;
    host: string;
    port: string;
    username: string;
    password: string;
  }) {
    // 点击添加按钮
    await this.addServerButton.click();
    await waitForModal(this.page, '添加服务器');

    // 填写表单
    await this.page.fill('input[name="name"]', serverData.name);
    await this.page.fill('input[name="host"]', serverData.host);
    await this.page.fill('input[name="port"]', serverData.port);
    await this.page.fill('input[name="username"]', serverData.username);
    await this.page.fill('input[name="password"]', serverData.password);

    // 提交表单
    const submitButton = this.page.locator('button[type="submit"], button:has-text("保存")');
    await submitButton.click();

    // 等待成功提示
    await waitForToast(this.page);
    await waitForLoading(this.page);
  }

  /**
   * 删除服务器
   */
  async deleteServer(serverName: string) {
    const card = await this.findServerCard(serverName);

    if (!card) {
      throw new Error(`找不到服务器: ${serverName}`);
    }

    // 点击删除按钮
    const deleteButton = card.locator('button:has-text("删除"), button[data-action="delete"]');
    await deleteButton.click();

    // 确认删除
    const confirmButton = this.page.locator('button:has-text("确认"), button:has-text("Confirm")');
    await confirmButton.click();

    await waitForToast(this.page);
    await waitForLoading(this.page);
  }

  /**
   * 刷新服务器状态
   */
  async refreshServerStatus(serverName: string) {
    const card = await this.findServerCard(serverName);

    if (!card) {
      throw new Error(`找不到服务器: ${serverName}`);
    }

    const refreshButton = card.locator('button:has-text("刷新"), button[data-action="refresh"]');
    await refreshButton.click();
    await waitForLoading(this.page);
  }

  /**
   * 搜索服务器
   */
  async searchServer(keyword: string) {
    await this.searchInput.fill(keyword);
    await waitForLoading(this.page);
  }

  /**
   * 切换视图模式（卡片/列表）
   */
  async switchView(viewType: 'card' | 'list') {
    const button = this.viewToggleButtons.filter({ hasText: viewType === 'card' ? '卡片' : '列表' });
    await button.click();
    await waitForLoading(this.page);
  }

  /**
   * 筛选服务器状态
   */
  async filterByStatus(status: 'all' | 'online' | 'offline' | 'warning') {
    await this.filterDropdown.selectOption({ value: status });
    await waitForLoading(this.page);
  }

  /**
   * 检查图表是否已加载
   */
  async areChartsLoaded(): Promise<boolean> {
    try {
      await this.healthTrendChart.waitFor({ state: 'visible', timeout: 5000 });
      await this.loadDistributionChart.waitFor({ state: 'visible', timeout: 5000 });
      await this.appDistributionChart.waitFor({ state: 'visible', timeout: 5000 });
      return true;
    } catch {
      return false;
    }
  }

  /**
   * 获取服务器状态统计
   */
  async getServerStatusStats(): Promise<{ online: number; offline: number; warning: number }> {
    const statsCard = this.page.locator('.stats-card, [data-stats]');

    const onlineText = await statsCard.filter({ hasText: '在线' }).innerText();
    const offlineText = await statsCard.filter({ hasText: '离线' }).innerText();
    const warningText = await statsCard.filter({ hasText: '警告' }).innerText();

    return {
      online: parseInt(onlineText.match(/\d+/)?.[0] || '0'),
      offline: parseInt(offlineText.match(/\d+/)?.[0] || '0'),
      warning: parseInt(warningText.match(/\d+/)?.[0] || '0'),
    };
  }

  /**
   * 批量选择服务器
   */
  async selectServers(serverNames: string[]) {
    for (const name of serverNames) {
      const card = await this.findServerCard(name);

      if (card) {
        const checkbox = card.locator('input[type="checkbox"]');
        await checkbox.check();
      }
    }
  }

  /**
   * 批量操作
   */
  async batchAction(action: 'start' | 'stop' | 'restart' | 'delete') {
    const batchButton = this.page.locator(`button[data-batch-action="${action}"]`);
    await batchButton.click();

    // 如果是删除操作，需要确认
    if (action === 'delete') {
      const confirmButton = this.page.locator('button:has-text("确认")');
      await confirmButton.click();
    }

    await waitForToast(this.page);
    await waitForLoading(this.page);
  }
}
