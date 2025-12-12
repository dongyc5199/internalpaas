import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { RegisterPage } from '../../../../src/features/auth/pages/RegisterPage';
import { userApi } from '../../../../src/shared/api/userApi';

/**
 * RegisterPage 组件测试
 *
 * 测试覆盖:
 * - 页面渲染
 * - 表单验证（用户名、邮箱、密码强度、密码匹配）
 * - 注册功能
 * - 错误处理
 * - 加载状态
 * - 密码强度指示器
 * - 实时用户名验证
 */

// Mock userApi
vi.mock('../../../../src/shared/api/userApi', () => ({
  userApi: {
    register: vi.fn(),
    checkUsernameAvailable: vi.fn(),
  },
}));

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderWithRouter = (ui: React.ReactElement): ReturnType<typeof render> => {
    return render(<BrowserRouter>{ui}</BrowserRouter>);
  };

  describe('渲染', () => {
    it('应该渲染注册表单', () => {
      renderWithRouter(<RegisterPage />);

      expect(screen.getByLabelText(/^用户名$/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/^企业邮箱$/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/^设置密码$/i)).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/请再次输入密码/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /注册/i })).toBeInTheDocument();
    });

    it('应该显示注册页面标题', () => {
      renderWithRouter(<RegisterPage />);

      expect(screen.getByText('账号注册')).toBeInTheDocument();
    });

    it('应该显示返回登录链接', () => {
      renderWithRouter(<RegisterPage />);

      expect(screen.getByText(/已有账号？/i)).toBeInTheDocument();
      expect(screen.getByText(/立即登录/i)).toBeInTheDocument();
    });
  });

  describe('表单验证', () => {
    it('应该在用户名为空时显示错误', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const submitButton = screen.getByRole('button', { name: /注册/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/请输入用户名/i)).toBeInTheDocument();
      });
    });

    it('应该在用户名少于3个字符时显示错误', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      await user.type(usernameInput, 'ab');

      const submitButton = screen.getByRole('button', { name: /注册/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/用户名至少3个字符/i)).toBeInTheDocument();
      });
    });

    it('应该在邮箱为空时显示错误', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      await user.type(usernameInput, 'testuser');

      const submitButton = screen.getByRole('button', { name: /注册/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/请输入邮箱地址/i)).toBeInTheDocument();
      });
    });

    it('应该在邮箱格式无效时显示错误', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      const emailInput = screen.getByLabelText(/企业邮箱/i);

      await user.type(usernameInput, 'testuser');
      await user.type(emailInput, 'invalid-email');

      const submitButton = screen.getByRole('button', { name: /注册/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/请输入有效的邮箱地址/i)).toBeInTheDocument();
      });
    });

    it('应该在密码为空时显示错误', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      const emailInput = screen.getByLabelText(/企业邮箱/i);

      await user.type(usernameInput, 'testuser');
      await user.type(emailInput, 'test@example.com');

      const submitButton = screen.getByRole('button', { name: /注册/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/请输入密码/i)).toBeInTheDocument();
      });
    });

    it('应该在密码少于12位时显示错误', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      const emailInput = screen.getByLabelText(/^企业邮箱$/i);
      const passwordInput = screen.getByLabelText(/^设置密码$/i);

      await user.type(usernameInput, 'testuser');
      await user.type(emailInput, 'test@example.com');
      await user.type(passwordInput, 'Short1!');

      const submitButton = screen.getByRole('button', { name: /注册/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/密码至少需要12个字符/i)).toBeInTheDocument();
      });
    });

    it('应该在密码强度不足时显示错误', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      const emailInput = screen.getByLabelText(/^企业邮箱$/i);
      const passwordInput = screen.getByLabelText(/^设置密码$/i);

      await user.type(usernameInput, 'testuser');
      await user.type(emailInput, 'test@example.com');
      await user.type(passwordInput, 'onlylowercase');

      const submitButton = screen.getByRole('button', { name: /注册/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(
          screen.getByText(/密码需要包含大写字母、小写字母、数字和特殊字符中的至少3种/i)
        ).toBeInTheDocument();
      });
    });

    it('应该在两次密码不一致时显示错误', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      const emailInput = screen.getByLabelText(/^企业邮箱$/i);
      const passwordInput = screen.getByLabelText(/^设置密码$/i);
      const confirmPasswordInput = screen.getByPlaceholderText(/请再次输入密码/i);

      await user.type(usernameInput, 'testuser');
      await user.type(emailInput, 'test@example.com');
      await user.type(passwordInput, 'StrongPass123!');
      await user.type(confirmPasswordInput, 'DifferentPass123!');

      const submitButton = screen.getByRole('button', { name: /注册/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/两次输入的密码不一致/i)).toBeInTheDocument();
      });
    });
  });

  describe('密码强度指示器', () => {
    it('应该在密码为空时不显示强度指示器', () => {
      renderWithRouter(<RegisterPage />);

      expect(screen.queryByText(/弱|中|强|非常强/)).not.toBeInTheDocument();
    });

    it('应该显示弱密码提示', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const passwordInput = screen.getByLabelText(/设置密码/i);
      await user.type(passwordInput, 'short');

      await waitFor(() => {
        expect(screen.getByText('弱')).toBeInTheDocument();
      });
    });

    it('应该显示中等强度密码提示', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const passwordInput = screen.getByLabelText(/设置密码/i);
      await user.type(passwordInput, 'Password123');

      await waitFor(() => {
        expect(screen.getByText('中')).toBeInTheDocument();
      });
    });

    it('应该显示强密码提示', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const passwordInput = screen.getByLabelText(/设置密码/i);
      await user.type(passwordInput, 'StrongPass123!');

      await waitFor(() => {
        expect(screen.getByText(/强|非常强/)).toBeInTheDocument();
      });
    });
  });

  describe('实时用户名验证', () => {
    it('应该在用户名可用时显示提示', async () => {
      vi.mocked(userApi.checkUsernameAvailable).mockResolvedValue(true);

      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      await user.type(usernameInput, 'available');

      // 等待防抖和API调用
      await waitFor(
        () => {
          expect(userApi.checkUsernameAvailable).toHaveBeenCalledWith('available');
      });

      await waitFor(() => {
        expect(screen.getByText(/✓ 可用/i)).toBeInTheDocument();
      });
    });

    it('应该在用户名已占用时显示错误', async () => {
      vi.mocked(userApi.checkUsernameAvailable).mockResolvedValue(false);

      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      await user.type(usernameInput, 'taken');

      // 等待防抖和API调用
      await waitFor(
        () => {
          expect(userApi.checkUsernameAvailable).toHaveBeenCalledWith('taken');
      });

      await waitFor(() => {
        expect(screen.getByText(/✗ 已占用/i)).toBeInTheDocument();
      });
    });

    it('应该在用户名少于3个字符时不进行检查', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      await user.type(usernameInput, 'ab');

      // 等待一段时间，确保防抖时间过去
      await new Promise((resolve) => setTimeout(resolve, 600));

      expect(userApi.checkUsernameAvailable).not.toHaveBeenCalled();
    });

    it('应该显示检查中状态', async () => {
      vi.mocked(userApi.checkUsernameAvailable).mockImplementation(
        () => new Promise((resolve) => setTimeout(() => { resolve(true); }, 1000))
      );

      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      await user.type(usernameInput, 'checking');

      // 等待防抖后显示检查中状态
      await waitFor(() => {
        expect(screen.getByText(/检查中/i)).toBeInTheDocument();
      }, { timeout: 2000 });
    });
  });

  describe('注册功能', () => {
    it('应该成功注册用户', async () => {
      vi.mocked(userApi.checkUsernameAvailable).mockResolvedValue(true);
      vi.mocked(userApi.register).mockResolvedValue({ message: '注册成功' });

      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      const emailInput = screen.getByLabelText(/^企业邮箱$/i);
      const passwordInput = screen.getByLabelText(/^设置密码$/i);
      const confirmPasswordInput = screen.getByPlaceholderText(/请再次输入密码/i);

      await user.type(usernameInput, 'newuser');
      await user.type(emailInput, 'new@example.com');
      await user.type(passwordInput, 'StrongPass123!');
      await user.type(confirmPasswordInput, 'StrongPass123!');

      // 等待用户名验证完成
      await waitFor(
        () => {
          expect(screen.getByText(/✓ 可用/i)).toBeInTheDocument();
        },
        { timeout: 2000 }
      );

      const submitButton = screen.getByRole('button', { name: /注册/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(userApi.register).toHaveBeenCalledWith({
          username: 'newuser',
          email: 'new@example.com',
          password: 'StrongPass123!',
          confirmPassword: 'StrongPass123!',
        });
      });

      await waitFor(() => {
        expect(screen.getByText(/注册成功/i)).toBeInTheDocument();
      });
    });

    it('应该在注册失败时显示错误', async () => {
      vi.mocked(userApi.checkUsernameAvailable).mockResolvedValue(true);
      vi.mocked(userApi.register).mockRejectedValue(new Error('邮箱已被注册'));

      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      const emailInput = screen.getByLabelText(/^企业邮箱$/i);
      const passwordInput = screen.getByLabelText(/^设置密码$/i);
      const confirmPasswordInput = screen.getByPlaceholderText(/请再次输入密码/i);

      await user.type(usernameInput, 'newuser');
      await user.type(emailInput, 'existing@example.com');
      await user.type(passwordInput, 'StrongPass123!');
      await user.type(confirmPasswordInput, 'StrongPass123!');

      // 等待用户名验证完成
      await waitFor(
        () => {
          expect(screen.getByText(/✓ 可用/i)).toBeInTheDocument();
        },
        { timeout: 2000 }
      );

      const submitButton = screen.getByRole('button', { name: /注册/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/邮箱已被注册/i)).toBeInTheDocument();
      });
    });

    it('应该在用户名已占用时阻止提交', async () => {
      vi.mocked(userApi.checkUsernameAvailable).mockResolvedValue(false);

      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      const emailInput = screen.getByLabelText(/^企业邮箱$/i);
      const passwordInput = screen.getByLabelText(/^设置密码$/i);
      const confirmPasswordInput = screen.getByPlaceholderText(/请再次输入密码/i);

      await user.type(usernameInput, 'takenuser');
      await user.type(emailInput, 'test@example.com');
      await user.type(passwordInput, 'StrongPass123!');
      await user.type(confirmPasswordInput, 'StrongPass123!');

      // 等待用户名验证完成
      await waitFor(
        () => {
          expect(screen.getByText(/✗ 已占用/i)).toBeInTheDocument();
        },
        { timeout: 2000 }
      );

      const submitButton = screen.getByRole('button', { name: /注册/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/用户名已被占用/i)).toBeInTheDocument();
      });

      expect(userApi.register).not.toHaveBeenCalled();
    });
  });

  describe('显示/隐藏密码', () => {
    it('应该切换密码可见性', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const passwordInput = screen.getByLabelText(/^设置密码$/i) as HTMLInputElement;
      expect(passwordInput.type).toBe('password');

      const toggleButton = screen.getAllByText(/显示/i)[0];
      await user.click(toggleButton);

      expect(passwordInput.type).toBe('text');

      await user.click(screen.getAllByText(/隐藏/i)[0]);
      expect(passwordInput.type).toBe('password');
    });

    it('应该切换确认密码可见性', async () => {
      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const confirmPasswordInput = screen.getByPlaceholderText(/请再次输入密码/i) as HTMLInputElement;
      expect(confirmPasswordInput.type).toBe('password');

      const toggleButtons = screen.getAllByText(/显示/i);
      const confirmToggleButton = toggleButtons[1];
      await user.click(confirmToggleButton);

      expect(confirmPasswordInput.type).toBe('text');

      const hideButtons = screen.getAllByText(/隐藏/i);
      await user.click(hideButtons[hideButtons.length - 1]);
      expect(confirmPasswordInput.type).toBe('password');
    });
  });

  describe('加载状态', () => {
    it('应该在提交时禁用表单', async () => {
      vi.mocked(userApi.checkUsernameAvailable).mockResolvedValue(true);
      vi.mocked(userApi.register).mockImplementation(
        () => new Promise((resolve) => setTimeout(() => { resolve({ message: '注册成功' }); }, 1000))
      );

      const user = userEvent.setup({ delay: null });
      renderWithRouter(<RegisterPage />);

      const usernameInput = screen.getByLabelText(/^用户名$/i);
      const emailInput = screen.getByLabelText(/^企业邮箱$/i);
      const passwordInput = screen.getByLabelText(/^设置密码$/i);
      const confirmPasswordInput = screen.getByPlaceholderText(/请再次输入密码/i);

      await user.type(usernameInput, 'newuser');
      await user.type(emailInput, 'new@example.com');
      await user.type(passwordInput, 'StrongPass123!');
      await user.type(confirmPasswordInput, 'StrongPass123!');

      // 等待用户名验证完成
      await waitFor(
        () => {
          expect(screen.getByText(/✓ 可用/i)).toBeInTheDocument();
        },
        { timeout: 2000 }
      );

      const submitButton = screen.getByRole('button', { name: /注册/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/注册中/i)).toBeInTheDocument();
        expect(submitButton).toBeDisabled();
      });
    });
  });
});
