import { Page, Locator } from '@playwright/test';

/**
 * 测试辅助工具函数
 */

/**
 * 等待并点击元素
 */
export async function clickAndWait(page: Page, selector: string, waitForNavigation = false) {
  await page.waitForSelector(selector, { state: 'visible' });
  const clickPromise = page.click(selector);

  if (waitForNavigation) {
    await Promise.all([page.waitForNavigation(), clickPromise]);
  } else {
    await clickPromise;
  }
}

/**
 * 填写表单字段
 */
export async function fillForm(page: Page, fields: Record<string, string>) {
  for (const [selector, value] of Object.entries(fields)) {
    await page.fill(selector, value);
  }
}

/**
 * 等待加载完成
 */
export async function waitForLoading(page: Page, timeout = 5000) {
  // 等待loading spinner消失
  await page.waitForSelector('.loading, .spinner, [data-loading="true"]', {
    state: 'detached',
    timeout,
  }).catch(() => {
    // 如果没有找到loading元素，说明已经加载完成
  });

  // 等待网络空闲
  await page.waitForLoadState('networkidle').catch(() => {
    // 网络可能不会完全空闲，继续执行
  });
}

/**
 * 等待Toast通知出现
 */
export async function waitForToast(page: Page, expectedText?: string) {
  const toast = page.locator('.toast, .notification, [role="alert"]').first();
  await toast.waitFor({ state: 'visible', timeout: 5000 });

  if (expectedText) {
    await toast.filter({ hasText: expectedText }).waitFor({ state: 'visible' });
  }

  return toast;
}

/**
 * 等待模态框出现
 */
export async function waitForModal(page: Page, title?: string) {
  const modal = page.locator('.modal, [role="dialog"], .drawer').first();
  await modal.waitFor({ state: 'visible', timeout: 5000 });

  if (title) {
    await modal.locator(`:has-text("${title}")`).first().waitFor({ state: 'visible' });
  }

  return modal;
}

/**
 * 关闭模态框
 */
export async function closeModal(page: Page) {
  // 尝试点击关闭按钮
  const closeButton = page.locator('.modal .close, [role="dialog"] .close, button[aria-label="Close"]').first();

  if (await closeButton.isVisible()) {
    await closeButton.click();
  } else {
    // 按ESC键关闭
    await page.keyboard.press('Escape');
  }

  // 等待模态框消失
  await page.locator('.modal, [role="dialog"], .drawer').first().waitFor({
    state: 'detached',
    timeout: 2000,
  }).catch(() => {
    // 可能已经关闭
  });
}

/**
 * 截图并保存（用于调试）
 */
export async function takeScreenshot(page: Page, name: string) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const filename = `screenshots/${name}-${timestamp}.png`;
  await page.screenshot({ path: filename, fullPage: true });
  console.log(`📸 截图已保存: ${filename}`);
  return filename;
}

/**
 * 等待API请求完成
 */
export async function waitForApiResponse(
  page: Page,
  urlPattern: string | RegExp,
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' = 'GET'
) {
  const response = await page.waitForResponse(
    (response) => {
      const url = response.url();
      const matchesUrl = typeof urlPattern === 'string' ? url.includes(urlPattern) : urlPattern.test(url);
      return matchesUrl && response.request().method() === method;
    },
    { timeout: 10000 }
  );

  return response;
}

/**
 * 模拟API响应
 */
export async function mockApiResponse(
  page: Page,
  urlPattern: string | RegExp,
  responseData: any,
  status = 200
) {
  await page.route(urlPattern, (route) => {
    route.fulfill({
      status,
      contentType: 'application/json',
      body: JSON.stringify(responseData),
    });
  });
}

/**
 * 检查元素是否存在
 */
export async function elementExists(page: Page, selector: string): Promise<boolean> {
  try {
    await page.waitForSelector(selector, { timeout: 1000 });
    return true;
  } catch {
    return false;
  }
}

/**
 * 获取表格行数据
 */
export async function getTableRowData(row: Locator): Promise<string[]> {
  const cells = row.locator('td');
  const count = await cells.count();
  const data: string[] = [];

  for (let i = 0; i < count; i++) {
    const text = await cells.nth(i).innerText();
    data.push(text.trim());
  }

  return data;
}

/**
 * 在表格中查找包含指定文本的行
 */
export async function findTableRow(page: Page, tableSelector: string, searchText: string): Promise<Locator | null> {
  const table = page.locator(tableSelector);
  const rows = table.locator('tbody tr');
  const count = await rows.count();

  for (let i = 0; i < count; i++) {
    const row = rows.nth(i);
    const text = await row.innerText();

    if (text.includes(searchText)) {
      return row;
    }
  }

  return null;
}

/**
 * 等待并验证URL变化
 */
export async function expectUrlChange(page: Page, expectedPattern: string | RegExp) {
  await page.waitForURL(expectedPattern, { timeout: 5000 });
}

/**
 * 刷新页面并等待加载
 */
export async function refreshPage(page: Page) {
  await page.reload();
  await waitForLoading(page);
}

/**
 * 随机生成测试数据
 */
export function generateTestData(prefix: string): string {
  const timestamp = Date.now();
  const random = Math.floor(Math.random() * 10000);
  return `${prefix}-${timestamp}-${random}`;
}

/**
 * 延迟执行（谨慎使用）
 */
export function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
