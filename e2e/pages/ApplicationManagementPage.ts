import { Page, Locator } from '@playwright/test';
import { waitForLoading, waitForModal, waitForToast } from '../utils/helpers';

/**
 * 应用管理页面对象模型
 */
export class ApplicationManagementPage {
  readonly page: Page;

  // 页面元素
  readonly applicationCards: Locator;
  readonly uploadButton: Locator;
  readonly refreshButton: Locator;
  readonly searchInput: Locator;
  readonly filterDropdown: Locator;

  constructor(page: Page) {
    this.page = page;

    this.applicationCards = page.locator('.application-card, [data-testid="app-card"]');
    this.uploadButton = page.locator('button:has-text("上传"), button:has-text("Upload")');
    this.refreshButton = page.locator('button:has-text("刷新"), button[data-action="refresh"]');
    this.searchInput = page.locator('input[type="search"], input[placeholder*="搜索"]');
    this.filterDropdown = page.locator('select[name="status"], .filter-dropdown');
  }

  /**
   * 导航到应用管理页面
   */
  async goto() {
    await this.page.goto('/apps');
    await waitForLoading(this.page);
  }

  /**
   * 获取应用数量
   */
  async getApplicationCount(): Promise<number> {
    await this.applicationCards.first().waitFor({ timeout: 5000 }).catch(() => {});
    return await this.applicationCards.count();
  }

  /**
   * 根据名称查找应用
   */
  async findApplicationCard(appName: string): Promise<Locator | null> {
    const cards = this.applicationCards;
    const count = await cards.count();

    for (let i = 0; i < count; i++) {
      const card = cards.nth(i);
      const text = await card.innerText();

      if (text.includes(appName)) {
        return card;
      }
    }

    return null;
  }

  /**
   * 上传JAR文件
   */
  async uploadApplication(filePath: string, appName: string, serverName?: string) {
    await this.uploadButton.click();
    await waitForModal(this.page, '上传应用');

    // 设置文件
    const fileInput = this.page.locator('input[type="file"]');
    await fileInput.setInputFiles(filePath);

    // 填写应用名称
    await this.page.fill('input[name="name"]', appName);

    // 选择服务器（如果提供）
    if (serverName) {
      await this.page.selectOption('select[name="serverId"]', { label: serverName });
    }

    // 提交
    const submitButton = this.page.locator('button[type="submit"]');
    await submitButton.click();

    await waitForToast(this.page);
    await waitForLoading(this.page);
  }

  /**
   * 启动应用
   */
  async startApplication(appName: string) {
    const card = await this.findApplicationCard(appName);

    if (!card) {
      throw new Error(`找不到应用: ${appName}`);
    }

    const startButton = card.locator('button:has-text("启动"), button[data-action="start"]');
    await startButton.click();
    await waitForToast(this.page);
    await waitForLoading(this.page);
  }

  /**
   * 停止应用
   */
  async stopApplication(appName: string) {
    const card = await this.findApplicationCard(appName);

    if (!card) {
      throw new Error(`找不到应用: ${appName}`);
    }

    const stopButton = card.locator('button:has-text("停止"), button[data-action="stop"]');
    await stopButton.click();
    await waitForToast(this.page);
    await waitForLoading(this.page);
  }

  /**
   * 重启应用
   */
  async restartApplication(appName: string) {
    const card = await this.findApplicationCard(appName);

    if (!card) {
      throw new Error(`找不到应用: ${appName}`);
    }

    const restartButton = card.locator('button:has-text("重启"), button[data-action="restart"]');
    await restartButton.click();
    await waitForToast(this.page);
    await waitForLoading(this.page);
  }

  /**
   * 删除应用
   */
  async deleteApplication(appName: string) {
    const card = await this.findApplicationCard(appName);

    if (!card) {
      throw new Error(`找不到应用: ${appName}`);
    }

    const deleteButton = card.locator('button:has-text("删除"), button[data-action="delete"]');
    await deleteButton.click();

    // 确认删除
    const confirmButton = this.page.locator('button:has-text("确认")');
    await confirmButton.click();

    await waitForToast(this.page);
    await waitForLoading(this.page);
  }

  /**
   * 查看应用详情
   */
  async viewApplicationDetails(appName: string) {
    const card = await this.findApplicationCard(appName);

    if (!card) {
      throw new Error(`找不到应用: ${appName}`);
    }

    const detailsButton = card.locator('button:has-text("详情"), a:has-text("详情")');
    await detailsButton.click();
    await this.page.waitForURL(/\/apps\/\d+/);
    await waitForLoading(this.page);
  }

  /**
   * 查看应用日志
   */
  async viewApplicationLogs(appName: string) {
    const card = await this.findApplicationCard(appName);

    if (!card) {
      throw new Error(`找不到应用: ${appName}`);
    }

    const logsButton = card.locator('button:has-text("日志"), button[data-action="logs"]');
    await logsButton.click();
    await waitForModal(this.page, '日志');
  }

  /**
   * 获取应用状态
   */
  async getApplicationStatus(appName: string): Promise<string> {
    const card = await this.findApplicationCard(appName);

    if (!card) {
      throw new Error(`找不到应用: ${appName}`);
    }

    const statusBadge = card.locator('.status, .badge, [data-status]');
    return await statusBadge.innerText();
  }

  /**
   * 搜索应用
   */
  async searchApplication(keyword: string) {
    await this.searchInput.fill(keyword);
    await waitForLoading(this.page);
  }

  /**
   * 按状态筛选
   */
  async filterByStatus(status: 'all' | 'running' | 'stopped' | 'error') {
    await this.filterDropdown.selectOption({ value: status });
    await waitForLoading(this.page);
  }

  /**
   * 刷新应用列表
   */
  async refresh() {
    await this.refreshButton.click();
    await waitForLoading(this.page);
  }
}
