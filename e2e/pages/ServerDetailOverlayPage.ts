import { Page, Locator } from '@playwright/test';
import { waitForLoading } from '../utils/helpers';

/**
 * 服务器详情弹窗/抽屉页面对象模型
 */
export class ServerDetailOverlayPage {
  readonly page: Page;
  readonly overlay: Locator;

  // 导航标签
  readonly overviewTab: Locator;
  readonly processTab: Locator;
  readonly diskTab: Locator;
  readonly networkTab: Locator;
  readonly logsTab: Locator;

  // 概览信息
  readonly cpuUsage: Locator;
  readonly memoryUsage: Locator;
  readonly diskUsage: Locator;
  readonly networkInfo: Locator;

  // 操作按钮
  readonly refreshButton: Locator;
  readonly closeButton: Locator;
  readonly connectButton: Locator;

  constructor(page: Page) {
    this.page = page;

    // 主容器（抽屉或模态框）
    this.overlay = page.locator('.server-detail-overlay, .drawer, [role="dialog"]').first();

    // 标签页
    this.overviewTab = this.overlay.locator('button:has-text("概览"), [data-tab="overview"]');
    this.processTab = this.overlay.locator('button:has-text("进程"), [data-tab="process"]');
    this.diskTab = this.overlay.locator('button:has-text("磁盘"), [data-tab="disk"]');
    this.networkTab = this.overlay.locator('button:has-text("网络"), [data-tab="network"]');
    this.logsTab = this.overlay.locator('button:has-text("日志"), [data-tab="logs"]');

    // 监控指标
    this.cpuUsage = this.overlay.locator('[data-metric="cpu"], .cpu-usage');
    this.memoryUsage = this.overlay.locator('[data-metric="memory"], .memory-usage');
    this.diskUsage = this.overlay.locator('[data-metric="disk"], .disk-usage');
    this.networkInfo = this.overlay.locator('[data-metric="network"], .network-info');

    // 操作按钮
    this.refreshButton = this.overlay.locator('button:has-text("刷新"), button[data-action="refresh"]');
    this.closeButton = this.overlay.locator('button.close, button[aria-label="Close"]');
    this.connectButton = this.overlay.locator('button:has-text("连接"), button[data-action="connect"]');
  }

  /**
   * 等待弹窗打开
   */
  async waitForOpen() {
    await this.overlay.waitFor({ state: 'visible', timeout: 5000 });
  }

  /**
   * 关闭弹窗
   */
  async close() {
    await this.closeButton.click();
    await this.overlay.waitFor({ state: 'detached', timeout: 3000 });
  }

  /**
   * 切换到指定标签页
   */
  async switchTab(tab: 'overview' | 'process' | 'disk' | 'network' | 'logs') {
    const tabMap = {
      overview: this.overviewTab,
      process: this.processTab,
      disk: this.diskTab,
      network: this.networkTab,
      logs: this.logsTab,
    };

    await tabMap[tab].click();
    await waitForLoading(this.page);
  }

  /**
   * 获取CPU使用率
   */
  async getCpuUsage(): Promise<string> {
    await this.cpuUsage.waitFor({ state: 'visible' });
    return await this.cpuUsage.innerText();
  }

  /**
   * 获取内存使用率
   */
  async getMemoryUsage(): Promise<string> {
    await this.memoryUsage.waitFor({ state: 'visible' });
    return await this.memoryUsage.innerText();
  }

  /**
   * 获取磁盘使用率
   */
  async getDiskUsage(): Promise<string> {
    await this.diskUsage.waitFor({ state: 'visible' });
    return await this.diskUsage.innerText();
  }

  /**
   * 刷新服务器数据
   */
  async refresh() {
    await this.refreshButton.click();
    await waitForLoading(this.page);
  }

  /**
   * 连接到SSH终端
   */
  async connectToTerminal() {
    await this.connectButton.click();
    await this.page.waitForURL(/terminal/);
  }

  /**
   * 获取进程列表（在进程标签页）
   */
  async getProcessList(): Promise<string[]> {
    await this.switchTab('process');

    const processRows = this.overlay.locator('tbody tr, .process-item');
    const count = await processRows.count();
    const processes: string[] = [];

    for (let i = 0; i < count; i++) {
      const text = await processRows.nth(i).innerText();
      processes.push(text);
    }

    return processes;
  }

  /**
   * 终止进程
   */
  async killProcess(processId: string) {
    await this.switchTab('process');

    const processRow = this.overlay.locator(`tr:has-text("${processId}"), [data-pid="${processId}"]`);
    const killButton = processRow.locator('button:has-text("终止"), button[data-action="kill"]');

    await killButton.click();

    // 确认终止
    const confirmButton = this.page.locator('button:has-text("确认")');
    await confirmButton.click();

    await waitForLoading(this.page);
  }

  /**
   * 获取磁盘分区信息
   */
  async getDiskPartitions(): Promise<Array<{ name: string; usage: string }>> {
    await this.switchTab('disk');

    const partitionCards = this.overlay.locator('.disk-partition, [data-partition]');
    const count = await partitionCards.count();
    const partitions: Array<{ name: string; usage: string }> = [];

    for (let i = 0; i < count; i++) {
      const card = partitionCards.nth(i);
      const name = await card.locator('.partition-name, [data-name]').innerText();
      const usage = await card.locator('.partition-usage, [data-usage]').innerText();

      partitions.push({ name, usage });
    }

    return partitions;
  }

  /**
   * 查看日志
   */
  async viewLogs(): Promise<string> {
    await this.switchTab('logs');

    const logContainer = this.overlay.locator('.log-container, pre, [data-logs]');
    await logContainer.waitFor({ state: 'visible' });

    return await logContainer.innerText();
  }
}
