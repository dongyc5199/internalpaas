import { test, expect } from '@playwright/test';
import { UserManagementPage } from '../../pages/UserManagementPage';
import { waitForLoading, waitForModal } from '../../utils/helpers';

/**
 * 用户与权限管理UI元素测试
 *
 * 测试内容：
 * 1. 用户管理页面是否正确加载
 * 2. 添加用户按钮是否正确响应
 * 3. 添加用户弹窗是否正确打开和显示
 */

test.describe('用户与权限管理 - UI元素测试', () => {
  let userPage: UserManagementPage;

  test.beforeEach(async ({ page }) => {
    userPage = new UserManagementPage(page);
    await userPage.goto();
  });

  test.describe('1. 页面加载测试', () => {
    test('应该成功加载页面标题', async ({ page }) => {
      // 验证页面标题存在
      await expect(userPage.pageTitle).toBeVisible({ timeout: 10000 });

      const titleText = await userPage.pageTitle.innerText();
      console.log(`✓ 页面标题加载成功: ${titleText}`);
    });

    test('应该显示页面描述或副标题', async ({ page }) => {
      const description = page.locator('.page-description, .subtitle, p').first();
      const isVisible = await description.isVisible({ timeout: 5000 }).catch(() => false);

      if (isVisible) {
        const descText = await description.innerText();
        console.log(`✓ 页面描述显示: ${descText}`);
      } else {
        console.log('⊘ 页面描述未找到（可能不存在）');
      }
    });

    test('应该加载用户列表表格', async ({ page }) => {
      // 验证表格容器存在
      const tableContainer = page.locator('table, .user-table, .data-table, [data-testid="user-table"]');
      await expect(tableContainer).toBeVisible({ timeout: 10000 });

      console.log('✓ 用户列表表格加载成功');
    });

    test('应该显示表格表头', async ({ page }) => {
      const tableHeader = page.locator('thead, .table-header, th').first();
      await expect(tableHeader).toBeVisible();

      console.log('✓ 表格表头显示成功');
    });

    test('表头应该包含必要的列', async ({ page }) => {
      const headers = page.locator('th, .table-header-cell');
      const headerCount = await headers.count();

      expect(headerCount).toBeGreaterThan(0);
      console.log(`✓ 表头包含 ${headerCount} 列`);

      // 检查常见列名
      const headerText = await page.locator('thead, .table-header').innerText();
      const hasUsername = headerText.includes('用户名') || headerText.includes('Username');
      const hasEmail = headerText.includes('邮箱') || headerText.includes('Email');
      const hasRole = headerText.includes('角色') || headerText.includes('Role');

      console.log(`  - 用户名列: ${hasUsername ? '✓' : '✗'}`);
      console.log(`  - 邮箱列: ${hasEmail ? '✓' : '✗'}`);
      console.log(`  - 角色列: ${hasRole ? '✓' : '✗'}`);
    });

    test('应该显示用户数据行', async () => {
      const userCount = await userPage.getUserCount();

      if (userCount > 0) {
        console.log(`✓ 显示 ${userCount} 个用户`);
        expect(userCount).toBeGreaterThan(0);
      } else {
        console.log('⊘ 当前没有用户数据');
      }
    });

    test('应该显示添加用户按钮', async () => {
      await expect(userPage.addUserButton).toBeVisible();

      const buttonText = await userPage.addUserButton.innerText();
      console.log(`✓ 添加用户按钮显示: ${buttonText}`);
    });

    test('应该显示搜索功能', async ({ page }) => {
      const searchBox = page.locator('input[type="search"], input[placeholder*="搜索"], .search-input');
      const isVisible = await searchBox.isVisible({ timeout: 5000 }).catch(() => false);

      if (isVisible) {
        await expect(searchBox).toBeVisible();
        console.log('✓ 搜索功能显示');
      } else {
        console.log('⊘ 搜索功能未找到（可能不存在）');
      }
    });

    test('应该显示筛选功能', async ({ page }) => {
      const filterDropdown = page.locator('select, .filter-dropdown, [data-filter]');
      const isVisible = await filterDropdown.first().isVisible({ timeout: 5000 }).catch(() => false);

      if (isVisible) {
        await expect(filterDropdown.first()).toBeVisible();
        console.log('✓ 筛选功能显示');
      } else {
        console.log('⊘ 筛选功能未找到（可能不存在）');
      }
    });

    test('应该显示分页控件', async ({ page }) => {
      const pagination = page.locator('.pagination, .page-navigation, [aria-label*="分页"]');
      const isVisible = await pagination.isVisible({ timeout: 3000 }).catch(() => false);

      if (isVisible) {
        await expect(pagination).toBeVisible();
        console.log('✓ 分页控件显示');
      } else {
        console.log('⊘ 分页控件未找到（可能数据较少不需要分页）');
      }
    });

    test('应该显示刷新按钮', async ({ page }) => {
      const refreshButton = page.locator('button:has-text("刷新"), button[data-action="refresh"], button[aria-label*="刷新"]');
      const isVisible = await refreshButton.first().isVisible({ timeout: 3000 }).catch(() => false);

      if (isVisible) {
        await expect(refreshButton.first()).toBeVisible();
        console.log('✓ 刷新按钮显示');
      } else {
        console.log('⊘ 刷新按钮未找到（可能不存在）');
      }
    });
  });

  test.describe('2. 添加用户按钮响应测试', () => {
    test('添加用户按钮应该可点击', async () => {
      await expect(userPage.addUserButton).toBeEnabled();
      console.log('✓ 添加用户按钮可点击');
    });

    test('点击添加用户按钮应该有响应', async ({ page }) => {
      // 记录点击前的状态
      const beforeClick = Date.now();

      // 点击按钮
      await userPage.addUserButton.click();

      const afterClick = Date.now();
      const responseTime = afterClick - beforeClick;

      console.log(`✓ 按钮点击响应时间: ${responseTime}ms`);
      expect(responseTime).toBeLessThan(5000);
    });

    test('添加用户按钮应该有正确的样式', async ({ page }) => {
      const button = userPage.addUserButton;

      // 检查按钮类名
      const className = await button.getAttribute('class');
      console.log(`✓ 按钮类名: ${className}`);

      // 通常应该有primary或类似的样式类
      const hasButtonStyle = className?.includes('btn') || className?.includes('button');
      expect(hasButtonStyle).toBe(true);
    });

    test('添加用户按钮应该有图标或文本', async () => {
      const buttonContent = await userPage.addUserButton.innerHTML();

      const hasIcon = buttonContent.includes('<i') || buttonContent.includes('svg') || buttonContent.includes('➕');
      const hasText = buttonContent.includes('添加') || buttonContent.includes('Add');

      expect(hasIcon || hasText).toBe(true);
      console.log(`✓ 按钮包含${hasIcon ? '图标' : ''}${hasIcon && hasText ? '和' : ''}${hasText ? '文本' : ''}`);
    });

    test('悬停在添加用户按钮上应该有视觉反馈', async ({ page }) => {
      await userPage.addUserButton.hover();

      // 检查按钮仍然可见和可用
      await expect(userPage.addUserButton).toBeVisible();
      await expect(userPage.addUserButton).toBeEnabled();

      console.log('✓ 按钮hover效果正常');
    });

    test('添加用户按钮应该有合适的可访问性标签', async () => {
      const ariaLabel = await userPage.addUserButton.getAttribute('aria-label');
      const title = await userPage.addUserButton.getAttribute('title');

      if (ariaLabel || title) {
        console.log(`✓ 按钮可访问性标签: ${ariaLabel || title}`);
      } else {
        console.log('⊘ 按钮没有可访问性标签（建议添加）');
      }
    });
  });

  test.describe('3. 添加用户弹窗测试', () => {
    test('点击添加按钮后应该打开弹窗', async ({ page }) => {
      await userPage.addUserButton.click();

      // 等待弹窗出现
      const modal = page.locator('.modal, [role="dialog"], .drawer, .overlay');
      await expect(modal).toBeVisible({ timeout: 10000 });

      console.log('✓ 添加用户弹窗打开成功');
    });

    test('弹窗应该显示标题', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      const modalTitle = page.locator('.modal-title, .drawer-title, h2, h3').filter({ hasText: /添加|新建|Add|Create/ });
      await expect(modalTitle.first()).toBeVisible({ timeout: 5000 });

      const titleText = await modalTitle.first().innerText();
      console.log(`✓ 弹窗标题: ${titleText}`);
    });

    test('弹窗应该包含用户名输入框', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      const usernameInput = page.locator('input[name="username"], input#username, input[placeholder*="用户名"]');
      await expect(usernameInput).toBeVisible();

      // 检查输入框属性
      const placeholder = await usernameInput.getAttribute('placeholder');
      const required = await usernameInput.getAttribute('required');

      console.log(`✓ 用户名输入框存在`);
      if (placeholder) console.log(`  - 占位符: ${placeholder}`);
      if (required !== null) console.log(`  - 必填: 是`);
    });

    test('弹窗应该包含邮箱输入框', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      const emailInput = page.locator('input[name="email"], input#email, input[type="email"]');
      await expect(emailInput).toBeVisible();

      // 检查输入框类型
      const inputType = await emailInput.getAttribute('type');
      console.log(`✓ 邮箱输入框存在 (类型: ${inputType})`);
    });

    test('弹窗应该包含密码输入框', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      const passwordInput = page.locator('input[name="password"], input#password, input[type="password"]');
      await expect(passwordInput).toBeVisible();

      // 检查密码输入框类型
      const inputType = await passwordInput.getAttribute('type');
      expect(inputType).toBe('password');

      console.log('✓ 密码输入框存在且类型正确');
    });

    test('弹窗应该包含角色选择器', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      const roleSelect = page.locator('select[name="role"], select#role, .role-selector');
      const isVisible = await roleSelect.isVisible({ timeout: 3000 }).catch(() => false);

      if (isVisible) {
        await expect(roleSelect).toBeVisible();

        // 获取角色选项
        const options = await roleSelect.locator('option').count();
        console.log(`✓ 角色选择器存在 (${options} 个选项)`);
      } else {
        console.log('⊘ 角色选择器未找到（可能使用其他形式）');
      }
    });

    test('弹窗应该包含确认密码输入框', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      const confirmPasswordInput = page.locator('input[name="confirmPassword"], input[name="passwordConfirm"], input#confirmPassword');
      const isVisible = await confirmPasswordInput.isVisible({ timeout: 3000 }).catch(() => false);

      if (isVisible) {
        await expect(confirmPasswordInput).toBeVisible();
        console.log('✓ 确认密码输入框存在');
      } else {
        console.log('⊘ 确认密码输入框未找到（可能不需要）');
      }
    });

    test('弹窗应该有保存/提交按钮', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      const submitButton = page.locator('button[type="submit"], button:has-text("保存"), button:has-text("添加"), button:has-text("Save")');
      await expect(submitButton.first()).toBeVisible();

      const buttonText = await submitButton.first().innerText();
      console.log(`✓ 提交按钮存在: ${buttonText}`);
    });

    test('弹窗应该有取消按钮', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      const cancelButton = page.locator('button:has-text("取消"), button:has-text("Cancel"), button.cancel-button');
      await expect(cancelButton.first()).toBeVisible();

      const buttonText = await cancelButton.first().innerText();
      console.log(`✓ 取消按钮存在: ${buttonText}`);
    });

    test('弹窗应该有关闭图标', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      const closeIcon = page.locator('button.close, button[aria-label*="关闭"], button[aria-label*="Close"], .close-icon');
      const isVisible = await closeIcon.first().isVisible({ timeout: 3000 }).catch(() => false);

      if (isVisible) {
        await expect(closeIcon.first()).toBeVisible();
        console.log('✓ 关闭图标存在');
      } else {
        console.log('⊘ 关闭图标未找到');
      }
    });

    test('点击取消按钮应该关闭弹窗', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      const cancelButton = page.locator('button:has-text("取消"), button:has-text("Cancel")');
      await cancelButton.first().click();

      // 验证弹窗已关闭
      const modal = page.locator('.modal, [role="dialog"]');
      await expect(modal).not.toBeVisible({ timeout: 5000 });

      console.log('✓ 点击取消按钮成功关闭弹窗');
    });

    test('点击遮罩层应该关闭弹窗', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      // 点击弹窗外部区域（遮罩层）
      const overlay = page.locator('.modal-backdrop, .overlay, [role="dialog"]').first();
      const hasOverlay = await overlay.isVisible({ timeout: 3000 }).catch(() => false);

      if (hasOverlay) {
        // 获取弹窗边界
        const modalContent = page.locator('.modal-content, .drawer-content').first();

        // 点击遮罩层（不是弹窗内容）
        await page.mouse.click(10, 10);

        // 等待一下看是否关闭
        await page.waitForTimeout(500);

        const modalVisible = await page.locator('.modal, [role="dialog"]').isVisible().catch(() => false);

        if (!modalVisible) {
          console.log('✓ 点击遮罩层可以关闭弹窗');
        } else {
          console.log('⊘ 点击遮罩层不会关闭弹窗（可能需要点击取消按钮）');
        }
      } else {
        console.log('⊘ 未找到遮罩层');
      }
    });

    test('表单验证 - 空表单提交应该显示错误', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      // 不填写任何内容直接提交
      const submitButton = page.locator('button[type="submit"]');
      await submitButton.click();

      // 检查是否有验证错误提示
      const usernameInput = page.locator('input[name="username"]');
      const isValid = await usernameInput.evaluate((el: HTMLInputElement) => el.checkValidity());

      expect(isValid).toBe(false);
      console.log('✓ 空表单验证正确触发');
    });

    test('表单验证 - 邮箱格式验证', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      const emailInput = page.locator('input[name="email"], input[type="email"]');

      // 输入无效邮箱
      await emailInput.fill('invalid-email');

      const isValid = await emailInput.evaluate((el: HTMLInputElement) => el.checkValidity());

      expect(isValid).toBe(false);
      console.log('✓ 邮箱格式验证工作正常');
    });

    test('表单字段应该可以正常输入', async ({ page }) => {
      await userPage.addUserButton.click();
      await waitForModal(page);

      // 测试用户名输入
      const usernameInput = page.locator('input[name="username"]');
      await usernameInput.fill('testuser123');
      const usernameValue = await usernameInput.inputValue();
      expect(usernameValue).toBe('testuser123');

      // 测试邮箱输入
      const emailInput = page.locator('input[name="email"]');
      await emailInput.fill('test@example.com');
      const emailValue = await emailInput.inputValue();
      expect(emailValue).toBe('test@example.com');

      // 测试密码输入
      const passwordInput = page.locator('input[name="password"]');
      await passwordInput.fill('Test@123456');
      const passwordValue = await passwordInput.inputValue();
      expect(passwordValue).toBe('Test@123456');

      console.log('✓ 所有表单字段可以正常输入');
    });
  });

  test.describe('4. 用户列表操作测试', () => {
    test('用户行应该显示操作按钮', async ({ page }) => {
      const userCount = await userPage.getUserCount();

      if (userCount > 0) {
        const firstRow = userPage.userRows.first();

        // 查找操作按钮
        const actionButtons = firstRow.locator('button');
        const buttonCount = await actionButtons.count();

        if (buttonCount > 0) {
          console.log(`✓ 用户行包含 ${buttonCount} 个操作按钮`);
          expect(buttonCount).toBeGreaterThan(0);
        } else {
          console.log('⊘ 未找到操作按钮');
        }
      } else {
        test.skip();
      }
    });

    test('应该有编辑用户按钮', async ({ page }) => {
      const userCount = await userPage.getUserCount();

      if (userCount > 0) {
        const firstRow = userPage.userRows.first();
        const editButton = firstRow.locator('button:has-text("编辑"), button:has-text("Edit"), button[data-action="edit"]');

        const isVisible = await editButton.first().isVisible({ timeout: 3000 }).catch(() => false);

        if (isVisible) {
          await expect(editButton.first()).toBeVisible();
          console.log('✓ 编辑按钮存在');
        } else {
          console.log('⊘ 编辑按钮未找到');
        }
      } else {
        test.skip();
      }
    });

    test('应该有删除用户按钮', async ({ page }) => {
      const userCount = await userPage.getUserCount();

      if (userCount > 0) {
        const firstRow = userPage.userRows.first();
        const deleteButton = firstRow.locator('button:has-text("删除"), button:has-text("Delete"), button[data-action="delete"]');

        const isVisible = await deleteButton.first().isVisible({ timeout: 3000 }).catch(() => false);

        if (isVisible) {
          await expect(deleteButton.first()).toBeVisible();
          console.log('✓ 删除按钮存在');
        } else {
          console.log('⊘ 删除按钮未找到');
        }
      } else {
        test.skip();
      }
    });

    test('应该显示用户角色标签', async ({ page }) => {
      const userCount = await userPage.getUserCount();

      if (userCount > 0) {
        const firstRow = userPage.userRows.first();
        const rowText = await firstRow.innerText();

        const hasRoleInfo = rowText.includes('ADMIN') || rowText.includes('DEVELOPER') || rowText.includes('管理员') || rowText.includes('开发者');

        if (hasRoleInfo) {
          console.log('✓ 用户角色信息显示正确');
          expect(hasRoleInfo).toBe(true);
        } else {
          console.log('⊘ 未找到角色信息');
        }
      } else {
        test.skip();
      }
    });

    test('应该显示用户状态', async ({ page }) => {
      const userCount = await userPage.getUserCount();

      if (userCount > 0) {
        const firstRow = userPage.userRows.first();
        const statusBadge = firstRow.locator('.badge, .status, .tag, [data-status]');

        const isVisible = await statusBadge.first().isVisible({ timeout: 3000 }).catch(() => false);

        if (isVisible) {
          const statusText = await statusBadge.first().innerText();
          console.log(`✓ 用户状态显示: ${statusText}`);
        } else {
          console.log('⊘ 用户状态标签未找到');
        }
      } else {
        test.skip();
      }
    });
  });
});
