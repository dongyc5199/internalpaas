import { Page, Locator } from '@playwright/test';
import { waitForLoading, waitForModal, waitForToast } from '../utils/helpers';

/**
 * 用户管理页面对象模型
 */
export class UserManagementPage {
  readonly page: Page;

  // 页面元素
  readonly userTable: Locator;
  readonly userRows: Locator;
  readonly addUserButton: Locator;
  readonly searchInput: Locator;
  readonly filterDropdown: Locator;
  readonly pageTitle: Locator;

  constructor(page: Page) {
    this.page = page;

    // 初始化页面元素定位器
    this.pageTitle = page.locator('h1, h2').filter({ hasText: /用户|User/ });
    this.userTable = page.locator('table, .user-table, [data-testid="user-table"]');
    this.userRows = page.locator('tbody tr, .user-row, [data-testid="user-row"]');
    this.addUserButton = page.locator('button:has-text("添加用户"), button:has-text("Add User"), button#addUserBtn');
    this.searchInput = page.locator('input[type="search"], input[placeholder*="搜索"], input[name="search"]');
    this.filterDropdown = page.locator('select[name="filter"], select[name="role"], .filter-dropdown');
  }

  /**
   * 导航到用户管理页面
   */
  async goto() {
    await this.page.goto('/admin/users');
    await waitForLoading(this.page);
  }

  /**
   * 获取用户数量
   */
  async getUserCount(): Promise<number> {
    await this.userRows.first().waitFor({ timeout: 5000 }).catch(() => {});
    return await this.userRows.count();
  }

  /**
   * 根据用户名查找用户行
   */
  async findUserRow(username: string): Promise<Locator | null> {
    const rows = this.userRows;
    const count = await rows.count();

    for (let i = 0; i < count; i++) {
      const row = rows.nth(i);
      const text = await row.innerText();

      if (text.includes(username)) {
        return row;
      }
    }

    return null;
  }

  /**
   * 添加新用户
   */
  async addUser(userData: {
    username: string;
    email: string;
    password: string;
    role?: string;
  }) {
    // 点击添加按钮
    await this.addUserButton.click();
    await waitForModal(this.page, '添加用户');

    // 填写表单
    await this.page.fill('input[name="username"]', userData.username);
    await this.page.fill('input[name="email"]', userData.email);
    await this.page.fill('input[name="password"]', userData.password);

    // 如果有角色选择
    if (userData.role) {
      const roleSelect = this.page.locator('select[name="role"]');
      if (await roleSelect.isVisible().catch(() => false)) {
        await roleSelect.selectOption({ value: userData.role });
      }
    }

    // 提交表单
    const submitButton = this.page.locator('button[type="submit"], button:has-text("保存"), button:has-text("Save")');
    await submitButton.click();

    // 等待成功提示
    await waitForToast(this.page);
    await waitForLoading(this.page);
  }

  /**
   * 编辑用户
   */
  async editUser(username: string, newData: Partial<{ email: string; role: string }>) {
    const row = await this.findUserRow(username);

    if (!row) {
      throw new Error(`找不到用户: ${username}`);
    }

    // 点击编辑按钮
    const editButton = row.locator('button:has-text("编辑"), button:has-text("Edit"), button[data-action="edit"]');
    await editButton.click();
    await waitForModal(this.page);

    // 更新字段
    if (newData.email) {
      await this.page.fill('input[name="email"]', newData.email);
    }

    if (newData.role) {
      await this.page.selectOption('select[name="role"]', { value: newData.role });
    }

    // 提交表单
    const submitButton = this.page.locator('button[type="submit"], button:has-text("保存")');
    await submitButton.click();

    await waitForToast(this.page);
    await waitForLoading(this.page);
  }

  /**
   * 删除用户
   */
  async deleteUser(username: string) {
    const row = await this.findUserRow(username);

    if (!row) {
      throw new Error(`找不到用户: ${username}`);
    }

    // 点击删除按钮
    const deleteButton = row.locator('button:has-text("删除"), button:has-text("Delete"), button[data-action="delete"]');
    await deleteButton.click();

    // 确认删除
    const confirmButton = this.page.locator('button:has-text("确认"), button:has-text("Confirm")');
    await confirmButton.click();

    await waitForToast(this.page);
    await waitForLoading(this.page);
  }

  /**
   * 切换用户角色
   */
  async toggleUserRole(username: string, role: 'ADMIN' | 'DEVELOPER') {
    const row = await this.findUserRow(username);

    if (!row) {
      throw new Error(`找不到用户: ${username}`);
    }

    // 查找角色切换按钮
    const toggleButton = row.locator(`button:has-text("${role}"), button[data-action="toggle-role"]`);
    await toggleButton.click();

    await waitForToast(this.page);
    await waitForLoading(this.page);
  }

  /**
   * 搜索用户
   */
  async searchUser(keyword: string) {
    await this.searchInput.fill(keyword);
    await waitForLoading(this.page);
  }

  /**
   * 按角色筛选
   */
  async filterByRole(role: 'all' | 'ADMIN' | 'DEVELOPER') {
    const filterSelect = this.filterDropdown;
    if (await filterSelect.isVisible().catch(() => false)) {
      await filterSelect.selectOption({ value: role });
      await waitForLoading(this.page);
    }
  }

  /**
   * 获取用户详细信息
   */
  async getUserInfo(username: string): Promise<{
    username: string;
    email: string;
    role: string;
  } | null> {
    const row = await this.findUserRow(username);

    if (!row) {
      return null;
    }

    const text = await row.innerText();
    const parts = text.split('\n').map((s) => s.trim());

    return {
      username: parts.find((p) => p.includes('@') === false) || username,
      email: parts.find((p) => p.includes('@')) || '',
      role: parts.find((p) => p.includes('ADMIN') || p.includes('DEVELOPER')) || '',
    };
  }

  /**
   * 重置用户密码
   */
  async resetPassword(username: string, newPassword: string) {
    const row = await this.findUserRow(username);

    if (!row) {
      throw new Error(`找不到用户: ${username}`);
    }

    // 点击重置密码按钮
    const resetButton = row.locator('button:has-text("重置密码"), button:has-text("Reset Password")');
    await resetButton.click();
    await waitForModal(this.page);

    // 输入新密码
    await this.page.fill('input[name="password"], input[type="password"]', newPassword);

    // 确认
    const confirmButton = this.page.locator('button[type="submit"], button:has-text("确认")');
    await confirmButton.click();

    await waitForToast(this.page);
    await waitForLoading(this.page);
  }

  /**
   * 查看用户详情
   */
  async viewUserDetails(username: string) {
    const row = await this.findUserRow(username);

    if (!row) {
      throw new Error(`找不到用户: ${username}`);
    }

    // 点击详情按钮或用户名
    const detailsButton = row.locator('button:has-text("详情"), button:has-text("Details"), a');

    if (await detailsButton.count() > 0) {
      await detailsButton.first().click();
    } else {
      await row.click();
    }

    await waitForModal(this.page);
  }

  /**
   * 批量删除用户
   */
  async batchDeleteUsers(usernames: string[]) {
    // 选中所有目标用户
    for (const username of usernames) {
      const row = await this.findUserRow(username);
      if (row) {
        const checkbox = row.locator('input[type="checkbox"]');
        await checkbox.check();
      }
    }

    // 点击批量删除按钮
    const batchDeleteButton = this.page.locator('button:has-text("批量删除"), button[data-action="batch-delete"]');
    await batchDeleteButton.click();

    // 确认删除
    const confirmButton = this.page.locator('button:has-text("确认")');
    await confirmButton.click();

    await waitForToast(this.page);
    await waitForLoading(this.page);
  }
}
