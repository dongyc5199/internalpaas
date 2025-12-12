import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { UserForm } from '../../../../src/features/admin/components/UserForm';
import { userApi } from '../../../../src/shared/api/userApi';
import { UserRole, type User } from '../../../../src/shared/types/user';

/**
 * UserForm 组件测试
 *
 * 测试覆盖:
 * - 创建模式表单渲染
 * - 编辑模式表单渲染
 * - 表单验证（必填字段、格式验证）
 * - 用户名验证
 * - 邮箱验证
 * - 密码验证
 * - 角色选择
 * - 创建用户提交
 * - 更新用户提交
 * - 错误处理
 * - 取消操作
 */

// Mock userApi
vi.mock('../../../../src/shared/api/userApi', () => ({
  userApi: {
    createUser: vi.fn(),
    updateUser: vi.fn(),
  },
}));

describe('UserForm', () => {
  let queryClient: QueryClient;
  const mockOnSuccess = vi.fn();
  const mockOnCancel = vi.fn();

  const mockUser: User = {
    id: 1,
    username: 'testuser',
    displayName: '测试用户',
    email: 'test@example.com',
    role: UserRole.DEVELOPER,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
    vi.clearAllMocks();
  });

  const renderWithProviders = (ui: React.ReactElement) => {
    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
  };

  describe('创建模式', () => {
    it('应该渲染空表单', () => {
      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      expect(screen.getByLabelText(/用户名/i)).toHaveValue('');
      expect(screen.getByLabelText(/邮箱/i)).toHaveValue('');
      expect(screen.getByLabelText(/显示名称/i)).toHaveValue('');
      expect(screen.getByLabelText(/密码/i)).toHaveValue('');
      expect(screen.getByRole('button', { name: /创建/i })).toBeInTheDocument();
    });

    it('应该显示必填标记', () => {
      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Required fields should have asterisk
      expect(screen.getAllByText('*').length).toBeGreaterThan(0);
    });

    it('应该显示用户名和密码提示', () => {
      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      expect(screen.getByText(/用户名只能包含字母、数字、下划线和连字符/i)).toBeInTheDocument();
      expect(screen.getByText(/密码长度至少6位/i)).toBeInTheDocument();
    });

    it('应该验证必填字段', async () => {
      const user = userEvent.setup();
      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Submit without filling fields
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText('请输入用户名')).toBeInTheDocument();
        expect(screen.getByText('请输入邮箱')).toBeInTheDocument();
        expect(screen.getByText('请输入密码')).toBeInTheDocument();
      });

      expect(userApi.createUser).not.toHaveBeenCalled();
    });

    it('应该验证用户名格式', async () => {
      const user = userEvent.setup();
      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      const usernameInput = screen.getByLabelText(/用户名/i);

      // Test invalid username (too short)
      await user.type(usernameInput, 'ab');
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        // Error appears in both error message and hint, use getAllByText
        const errorTexts = screen.getAllByText(
          /用户名只能包含字母、数字、下划线和连字符，长度3-20位/i
        );
        expect(errorTexts.length).toBeGreaterThan(0);
      });

      // Test invalid username (invalid characters)
      await user.clear(usernameInput);
      await user.type(usernameInput, 'user@name');
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        // Error appears in both error message and hint, use getAllByText
        const errorTexts = screen.getAllByText(
          /用户名只能包含字母、数字、下划线和连字符，长度3-20位/i
        );
        expect(errorTexts.length).toBeGreaterThan(0);
      });
    });

    it('应该验证邮箱格式', async () => {
      const user = userEvent.setup();
      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Fill required fields
      await user.type(screen.getByLabelText(/用户名/i), 'testuser');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      const emailInput = screen.getByLabelText(/邮箱/i);

      // Test invalid email (missing domain extension)
      await user.type(emailInput, 'invalid@test');
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText('请输入有效的邮箱地址')).toBeInTheDocument();
      });
    });

    it('应该验证密码长度', async () => {
      const user = userEvent.setup();
      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      const passwordInput = screen.getByLabelText(/密码/i);

      // Test short password
      await user.type(passwordInput, '12345');
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        // "密码长度至少6位" appears in both error message and hint, use getAllByText
        const errorTexts = screen.getAllByText('密码长度至少6位');
        expect(errorTexts.length).toBeGreaterThan(0);
      });
    });

    it('应该提交有效表单', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.createUser).mockResolvedValue(mockUser);

      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Fill form
      await user.type(screen.getByLabelText(/用户名/i), 'newuser');
      await user.type(screen.getByLabelText(/邮箱/i), 'newuser@example.com');
      await user.type(screen.getByLabelText(/显示名称/i), '新用户');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      // Submit
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(userApi.createUser).toHaveBeenCalledWith({
          username: 'newuser',
          email: 'newuser@example.com',
          displayName: '新用户',
          password: 'password123',
          role: UserRole.DEVELOPER,
        });
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('应该处理创建失败', async () => {
      const user = userEvent.setup();
      const errorMessage = '用户名已存在';
      vi.mocked(userApi.createUser).mockRejectedValue(new Error(errorMessage));

      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Fill and submit
      await user.type(screen.getByLabelText(/用户名/i), 'existinguser');
      await user.type(screen.getByLabelText(/邮箱/i), 'test@example.com');
      await user.type(screen.getByLabelText(/密码/i), 'password123');
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText(errorMessage)).toBeInTheDocument();
      });

      expect(mockOnSuccess).not.toHaveBeenCalled();
    });
  });

  describe('编辑模式', () => {
    it('应该填充现有用户数据', () => {
      renderWithProviders(
        <UserForm user={mockUser} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      expect(screen.getByLabelText(/用户名/i)).toHaveValue('testuser');
      expect(screen.getByLabelText(/邮箱/i)).toHaveValue('test@example.com');
      expect(screen.getByLabelText(/显示名称/i)).toHaveValue('测试用户');
      expect(screen.getByRole('button', { name: /保存/i })).toBeInTheDocument();
    });

    it('用户名字段应该被禁用', () => {
      renderWithProviders(
        <UserForm user={mockUser} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      expect(screen.getByLabelText(/用户名/i)).toBeDisabled();
    });

    it('密码字段应该为可选', () => {
      renderWithProviders(
        <UserForm user={mockUser} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      // Password hint should indicate it's optional (appears in both placeholder and hint)
      const passwordHints = screen.getAllByText(/留空/i);
      expect(passwordHints.length).toBeGreaterThan(0);
      expect(screen.getByText(/如需修改密码，请输入新密码；否则留空/i)).toBeInTheDocument();
    });

    it('应该在不修改密码的情况下更新用户', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.updateUser).mockResolvedValue({
        ...mockUser,
        email: 'newemail@example.com',
      });

      renderWithProviders(
        <UserForm user={mockUser} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      // Update email
      const emailInput = screen.getByLabelText(/邮箱/i);
      await user.clear(emailInput);
      await user.type(emailInput, 'newemail@example.com');

      // Submit without password
      await user.click(screen.getByRole('button', { name: /保存/i }));

      await waitFor(() => {
        expect(userApi.updateUser).toHaveBeenCalledWith(1, {
          email: 'newemail@example.com',
          role: UserRole.DEVELOPER,
          displayName: '测试用户',
        });
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('应该更新包括密码在内的用户信息', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.updateUser).mockResolvedValue(mockUser);

      renderWithProviders(
        <UserForm user={mockUser} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      // Update email and password
      const emailInput = screen.getByLabelText(/邮箱/i);
      await user.clear(emailInput);
      await user.type(emailInput, 'newemail@example.com');

      const passwordInput = screen.getByLabelText(/密码/i);
      await user.type(passwordInput, 'newpassword123');

      // Submit
      await user.click(screen.getByRole('button', { name: /保存/i }));

      await waitFor(() => {
        expect(userApi.updateUser).toHaveBeenCalledWith(1, {
          email: 'newemail@example.com',
          password: 'newpassword123',
          role: UserRole.DEVELOPER,
          displayName: '测试用户',
        });
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('应该验证编辑模式下的密码长度', async () => {
      const user = userEvent.setup();
      renderWithProviders(
        <UserForm user={mockUser} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      // Try to set short password
      const passwordInput = screen.getByLabelText(/密码/i);
      await user.type(passwordInput, '12345');
      await user.click(screen.getByRole('button', { name: /保存/i }));

      await waitFor(() => {
        expect(screen.getByText('密码长度至少6位')).toBeInTheDocument();
      });
    });
  });

  describe('角色选择', () => {
    it('应该选择不同的角色', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.createUser).mockResolvedValue(mockUser);

      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Change role
      const roleSelect = screen.getByLabelText(/角色/i);
      await user.selectOptions(roleSelect, UserRole.ADMIN);

      // Fill other fields
      await user.type(screen.getByLabelText(/用户名/i), 'adminuser');
      await user.type(screen.getByLabelText(/邮箱/i), 'admin@example.com');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      // Submit
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(userApi.createUser).toHaveBeenCalledWith(
          expect.objectContaining({
            role: UserRole.ADMIN,
          })
        );
      });
    });

    it('应该显示所有角色选项', () => {
      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Get all options from the select element
      const roleSelect = screen.getByLabelText(/角色/i) as HTMLSelectElement;
      const options = Array.from(roleSelect.options).map((opt) => opt.text);

      expect(options).toContain('开发者');
      expect(options).toContain('管理员');
      expect(options).toContain('超级管理员');
    });
  });

  describe('表单交互', () => {
    it('应该清除字段错误当输入时', async () => {
      const user = userEvent.setup();
      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Submit to trigger validation
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText('请输入用户名')).toBeInTheDocument();
      });

      // Type in the field
      await user.type(screen.getByLabelText(/用户名/i), 'testuser');

      await waitFor(() => {
        expect(screen.queryByText('请输入用户名')).not.toBeInTheDocument();
      });
    });

    it('应该调用onCancel当点击取消', async () => {
      const user = userEvent.setup();
      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      await user.click(screen.getByRole('button', { name: /取消/i }));

      expect(mockOnCancel).toHaveBeenCalled();
    });

    it('应该禁用表单在提交时', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.createUser).mockImplementation(
        () => new Promise((resolve) => setTimeout(() => resolve(mockUser), 1000))
      );

      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Fill form
      await user.type(screen.getByLabelText(/用户名/i), 'newuser');
      await user.type(screen.getByLabelText(/邮箱/i), 'test@example.com');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      // Submit
      await user.click(screen.getByRole('button', { name: /创建/i }));

      // Inputs should be disabled except username (which is always disabled in edit mode)
      await waitFor(() => {
        expect(screen.getByLabelText(/邮箱/i)).toBeDisabled();
        expect(screen.getByLabelText(/密码/i)).toBeDisabled();
      });
    });
  });

  describe('显示名称', () => {
    it('显示名称应该是可选的', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.createUser).mockResolvedValue(mockUser);

      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Fill only required fields
      await user.type(screen.getByLabelText(/用户名/i), 'newuser');
      await user.type(screen.getByLabelText(/邮箱/i), 'test@example.com');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      // Submit without displayName
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(userApi.createUser).toHaveBeenCalledWith({
          username: 'newuser',
          email: 'test@example.com',
          displayName: '',
          password: 'password123',
          role: UserRole.DEVELOPER,
        });
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });
  });

  describe('边界情况测试', () => {
    it('应该接受最小长度用户名（3位）', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.createUser).mockResolvedValue(mockUser);

      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      await user.type(screen.getByLabelText(/用户名/i), 'abc');
      await user.type(screen.getByLabelText(/邮箱/i), 'test@example.com');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(userApi.createUser).toHaveBeenCalledWith(
          expect.objectContaining({ username: 'abc' })
        );
      });
    });

    it('应该接受最大长度用户名（20位）', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.createUser).mockResolvedValue(mockUser);

      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      const maxUsername = 'a'.repeat(20); // 20 characters
      await user.type(screen.getByLabelText(/用户名/i), maxUsername);
      await user.type(screen.getByLabelText(/邮箱/i), 'test@example.com');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(userApi.createUser).toHaveBeenCalledWith(
          expect.objectContaining({ username: maxUsername })
        );
      });
    });

    it('应该拒绝过长的用户名（超过20位）', async () => {
      const user = userEvent.setup();
      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      const longUsername = 'a'.repeat(21); // 21 characters
      await user.type(screen.getByLabelText(/用户名/i), longUsername);
      await user.type(screen.getByLabelText(/邮箱/i), 'test@example.com');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        const errorTexts = screen.getAllByText(
          /用户名只能包含字母、数字、下划线和连字符，长度3-20位/i
        );
        expect(errorTexts.length).toBeGreaterThan(0);
      });
    });

    it('应该接受最小长度密码（6位）', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.createUser).mockResolvedValue(mockUser);

      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      await user.type(screen.getByLabelText(/用户名/i), 'testuser');
      await user.type(screen.getByLabelText(/邮箱/i), 'test@example.com');
      await user.type(screen.getByLabelText(/密码/i), '123456'); // exactly 6 characters

      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(userApi.createUser).toHaveBeenCalled();
      });
    });

    it('应该接受各种有效邮箱格式', async () => {
      const validEmails = [
        'test@example.com',
        'user+tag@example.co.uk',
        'test.user@sub.example.com',
        'test_user@example.com',
      ];

      for (const email of validEmails) {
        const user = userEvent.setup();
        vi.mocked(userApi.createUser).mockResolvedValue(mockUser);

        const { unmount } = renderWithProviders(
          <UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
        );

        await user.type(screen.getByLabelText(/用户名/i), 'testuser');
        await user.type(screen.getByLabelText(/邮箱/i), email);
        await user.type(screen.getByLabelText(/密码/i), 'password123');

        await user.click(screen.getByRole('button', { name: /创建/i }));

        await waitFor(() => {
          expect(userApi.createUser).toHaveBeenCalledWith(
            expect.objectContaining({ email })
          );
        });

        unmount();
        vi.clearAllMocks();
      }
    });

  });

  describe('错误恢复场景', () => {
    it('应该在提交失败后允许重新提交', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.createUser)
        .mockRejectedValueOnce(new Error('网络错误'))
        .mockResolvedValueOnce(mockUser);

      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Fill and submit
      await user.type(screen.getByLabelText(/用户名/i), 'testuser');
      await user.type(screen.getByLabelText(/邮箱/i), 'test@example.com');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      // First submit fails
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText('网络错误')).toBeInTheDocument();
      });

      // Second submit succeeds
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('应该在验证失败后允许修复并提交', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.createUser).mockResolvedValue(mockUser);

      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Submit with invalid username only
      await user.type(screen.getByLabelText(/用户名/i), 'ab'); // too short
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getAllByText(/用户名只能包含字母、数字、下划线和连字符/i).length).toBeGreaterThan(0);
      });

      // Fix username
      const usernameInput = screen.getByLabelText(/用户名/i);
      await user.clear(usernameInput);
      await user.type(usernameInput, 'validuser');

      // Fill other required fields
      await user.type(screen.getByLabelText(/邮箱/i), 'valid@example.com');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      // Resubmit
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('应该在提交失败后保留表单数据', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.createUser).mockRejectedValue(new Error('提交失败'));

      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Fill form
      await user.type(screen.getByLabelText(/用户名/i), 'testuser');
      await user.type(screen.getByLabelText(/邮箱/i), 'test@example.com');
      await user.type(screen.getByLabelText(/显示名称/i), '测试用户');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      const roleSelect = screen.getByLabelText(/角色/i);
      await user.selectOptions(roleSelect, UserRole.ADMIN);

      // Submit
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText('提交失败')).toBeInTheDocument();
      });

      // Verify form data is retained
      expect(screen.getByLabelText(/用户名/i)).toHaveValue('testuser');
      expect(screen.getByLabelText(/邮箱/i)).toHaveValue('test@example.com');
      expect(screen.getByLabelText(/显示名称/i)).toHaveValue('测试用户');
      expect(roleSelect).toHaveValue(UserRole.ADMIN);
    });
  });

  describe('复杂验证场景', () => {
    it('应该同时验证多个字段错误', async () => {
      const user = userEvent.setup();
      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Submit empty form
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText('请输入用户名')).toBeInTheDocument();
        expect(screen.getByText('请输入邮箱')).toBeInTheDocument();
        expect(screen.getByText('请输入密码')).toBeInTheDocument();
      });
    });

    it('应该依次清除多个字段错误', async () => {
      const user = userEvent.setup();
      renderWithProviders(<UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Trigger validation
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText('请输入用户名')).toBeInTheDocument();
        expect(screen.getByText('请输入邮箱')).toBeInTheDocument();
      });

      // Fix username
      await user.type(screen.getByLabelText(/用户名/i), 'testuser');
      await waitFor(() => {
        expect(screen.queryByText('请输入用户名')).not.toBeInTheDocument();
      });

      // Fix email
      await user.type(screen.getByLabelText(/邮箱/i), 'test@example.com');
      await waitFor(() => {
        expect(screen.queryByText('请输入邮箱')).not.toBeInTheDocument();
      });
    });

    it('应该处理特殊字符的用户名', async () => {
      const specialUsernames = ['user_123', 'user-name', 'user_name-123'];

      for (const username of specialUsernames) {
        const user = userEvent.setup();
        vi.mocked(userApi.createUser).mockResolvedValue(mockUser);

        const { unmount } = renderWithProviders(
          <UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
        );

        await user.type(screen.getByLabelText(/用户名/i), username);
        await user.type(screen.getByLabelText(/邮箱/i), 'test@example.com');
        await user.type(screen.getByLabelText(/密码/i), 'password123');

        await user.click(screen.getByRole('button', { name: /创建/i }));

        await waitFor(() => {
          expect(userApi.createUser).toHaveBeenCalledWith(
            expect.objectContaining({ username })
          );
        });

        unmount();
        vi.clearAllMocks();
      }
    });

    it('应该处理不同角色的创建', async () => {
      const roles = [UserRole.DEVELOPER, UserRole.ADMIN, UserRole.SUPER_ADMIN];

      for (const role of roles) {
        const user = userEvent.setup();
        vi.mocked(userApi.createUser).mockResolvedValue(mockUser);

        const { unmount } = renderWithProviders(
          <UserForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
        );

        await user.type(screen.getByLabelText(/用户名/i), 'testuser');
        await user.type(screen.getByLabelText(/邮箱/i), 'test@example.com');
        await user.type(screen.getByLabelText(/密码/i), 'password123');

        const roleSelect = screen.getByLabelText(/角色/i);
        await user.selectOptions(roleSelect, role);

        await user.click(screen.getByRole('button', { name: /创建/i }));

        await waitFor(() => {
          expect(userApi.createUser).toHaveBeenCalledWith(
            expect.objectContaining({ role })
          );
        });

        unmount();
        vi.clearAllMocks();
      }
    });
  });

  describe('编辑模式高级场景', () => {
    it('应该只更新修改的字段', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.updateUser).mockResolvedValue(mockUser);

      renderWithProviders(
        <UserForm user={mockUser} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      // Only change email
      const emailInput = screen.getByLabelText(/邮箱/i);
      await user.clear(emailInput);
      await user.type(emailInput, 'newemail@example.com');

      await user.click(screen.getByRole('button', { name: /保存/i }));

      await waitFor(() => {
        expect(userApi.updateUser).toHaveBeenCalledWith(
          mockUser.id,
          expect.objectContaining({
            email: 'newemail@example.com',
            role: mockUser.role,
            displayName: mockUser.displayName,
          })
        );
      });
    });

    it('应该在编辑模式下更新角色', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.updateUser).mockResolvedValue(mockUser);

      renderWithProviders(
        <UserForm user={mockUser} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      const roleSelect = screen.getByLabelText(/角色/i);
      await user.selectOptions(roleSelect, UserRole.ADMIN);

      await user.click(screen.getByRole('button', { name: /保存/i }));

      await waitFor(() => {
        expect(userApi.updateUser).toHaveBeenCalledWith(
          mockUser.id,
          expect.objectContaining({ role: UserRole.ADMIN })
        );
      });
    });

    it('应该在编辑模式下更新显示名称', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.updateUser).mockResolvedValue(mockUser);

      renderWithProviders(
        <UserForm user={mockUser} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      const displayNameInput = screen.getByLabelText(/显示名称/i);
      await user.clear(displayNameInput);
      await user.type(displayNameInput, '新名称');

      await user.click(screen.getByRole('button', { name: /保存/i }));

      await waitFor(() => {
        expect(userApi.updateUser).toHaveBeenCalledWith(
          mockUser.id,
          expect.objectContaining({ displayName: '新名称' })
        );
      });
    });

    it('应该在编辑模式下同时更新多个字段', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.updateUser).mockResolvedValue(mockUser);

      renderWithProviders(
        <UserForm user={mockUser} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      // Update multiple fields
      const emailInput = screen.getByLabelText(/邮箱/i);
      await user.clear(emailInput);
      await user.type(emailInput, 'new@example.com');

      const displayNameInput = screen.getByLabelText(/显示名称/i);
      await user.clear(displayNameInput);
      await user.type(displayNameInput, '新用户');

      const roleSelect = screen.getByLabelText(/角色/i);
      await user.selectOptions(roleSelect, UserRole.ADMIN);

      await user.type(screen.getByLabelText(/密码/i), 'newpassword123');

      await user.click(screen.getByRole('button', { name: /保存/i }));

      await waitFor(() => {
        expect(userApi.updateUser).toHaveBeenCalledWith(mockUser.id, {
          email: 'new@example.com',
          displayName: '新用户',
          role: UserRole.ADMIN,
          password: 'newpassword123',
        });
      });
    });

    it('应该在编辑模式下处理空显示名称', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.updateUser).mockResolvedValue(mockUser);

      renderWithProviders(
        <UserForm user={mockUser} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      // Clear displayName
      const displayNameInput = screen.getByLabelText(/显示名称/i);
      await user.clear(displayNameInput);

      await user.click(screen.getByRole('button', { name: /保存/i }));

      await waitFor(() => {
        expect(userApi.updateUser).toHaveBeenCalledWith(
          mockUser.id,
          expect.objectContaining({
            displayName: undefined,
          })
        );
      });
    });
  });
});
