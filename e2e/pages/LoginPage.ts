import { Page, Locator } from '@playwright/test';
import { waitForLoading, waitForToast } from '../utils/helpers';

/**
 * 登录页面对象模型
 */
export class LoginPage {
  readonly page: Page;
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly errorMessage: Locator;
  readonly registerLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.usernameInput = page.locator('input[name="username"]');
    this.passwordInput = page.locator('input[name="password"]');
    this.loginButton = page.locator('button[type="submit"]');
    this.errorMessage = page.locator('.error-message, .alert-danger');
    this.registerLink = page.locator('a[href*="register"]');
  }

  /**
   * 导航到登录页面
   */
  async goto() {
    await this.page.goto('/login');
    await waitForLoading(this.page);
  }

  /**
   * 执行登录操作
   */
  async login(username: string, password: string) {
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }

  /**
   * 登录并等待跳转
   */
  async loginAndWait(username: string, password: string) {
    await this.login(username, password);
    // 等待URL变化（不再固定期望特定URL模式）
    await this.page.waitForURL(/\/(admin|developer)/, { timeout: 30000 });
    await waitForLoading(this.page);
  }

  /**
   * 获取错误消息文本
   */
  async getErrorMessage(): Promise<string> {
    await this.errorMessage.waitFor({ state: 'visible', timeout: 3000 });
    return await this.errorMessage.innerText();
  }

  /**
   * 检查是否显示错误
   */
  async hasError(): Promise<boolean> {
    try {
      await this.errorMessage.waitFor({ state: 'visible', timeout: 2000 });
      return true;
    } catch {
      return false;
    }
  }

  /**
   * 点击注册链接
   */
  async clickRegister() {
    await this.registerLink.click();
    await this.page.waitForURL(/register/);
  }
}
