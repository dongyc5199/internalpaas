import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { UsersPage } from '../../../../src/features/admin/pages/UsersPage';
import { userApi } from '../../../../src/shared/api/userApi';
import { UserRole, type User } from '../../../../src/shared/types/user';

/**
 * UsersPage 组件测试
 *
 * 核心功能测试覆盖
 */

// Mock userApi
vi.mock('../../../../src/shared/api/userApi', () => ({
  userApi: {
    getUsers: vi.fn(),
    deleteUser: vi.fn(),
    batchDeleteUsers: vi.fn(),
    toggleAdmin: vi.fn(),
  },
}));

describe('UsersPage', () => {
  let queryClient: QueryClient;

  const mockUsers: User[] = [
    {
      id: 1,
      username: 'admin',
      displayName: '管理员',
      email: 'admin@example.com',
      role: UserRole.ADMIN,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
    {
      id: 2,
      username: 'developer',
      displayName: '开发者',
      email: 'dev@example.com',
      role: UserRole.DEVELOPER,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
  ];

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
    vi.clearAllMocks();

    // Default mock implementation
    vi.mocked(userApi.getUsers).mockResolvedValue({
      content: mockUsers,
      totalElements: 2,
      totalPages: 1,
      number: 0,
      size: 20,
    });
  });

  const renderWithProviders = (ui: React.ReactElement) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>{ui}</BrowserRouter>
      </QueryClientProvider>
    );
  };

  describe('渲染', () => {
    it('应该渲染页面标题', async () => {
      renderWithProviders(<UsersPage />);

      expect(screen.getByText('用户管理')).toBeInTheDocument();
      expect(screen.getByText('管理系统用户和角色权限')).toBeInTheDocument();
    });

    it('应该渲染搜索框', async () => {
      renderWithProviders(<UsersPage />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(/搜索用户/i)).toBeInTheDocument();
      });
    });

    it('应该渲染新增用户按钮', () => {
      renderWithProviders(<UsersPage />);

      expect(screen.getByRole('button', { name: /新增用户/i })).toBeInTheDocument();
    });

    it('应该渲染用户列表', async () => {
      renderWithProviders(<UsersPage />);

      await waitFor(() => {
        expect(screen.getByText('admin')).toBeInTheDocument();
        expect(screen.getByText('developer')).toBeInTheDocument();
      });
    });

    it('应该显示角色徽章', async () => {
      renderWithProviders(<UsersPage />);

      await waitFor(() => {
        // "管理员" and "开发者" appear multiple times (badge + button text), use getAllByText
        expect(screen.getAllByText('管理员').length).toBeGreaterThan(0);
        expect(screen.getAllByText('开发者').length).toBeGreaterThan(0);
      });
    });
  });

  describe('搜索', () => {
    it('应该在输入时更新搜索框', async () => {
      const user = userEvent.setup();
      renderWithProviders(<UsersPage />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(/搜索用户/i)).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText(/搜索用户/i);
      await user.type(searchInput, 'admin');

      expect(searchInput).toHaveValue('admin');
    });
  });

  // 批量操作功能暂未实现，测试跳过
  describe.skip('批量操作', () => {
    it('应该显示checkbox列', async () => {
      renderWithProviders(<UsersPage />);

      await waitFor(() => {
        expect(screen.getByText('admin')).toBeInTheDocument();
      });

      // Should have checkboxes (2 rows + 1 header = 3)
      const checkboxes = screen.getAllByRole('checkbox');
      expect(checkboxes.length).toBe(3);
    });

    it('应该选择单个用户', async () => {
      const user = userEvent.setup();
      renderWithProviders(<UsersPage />);

      await waitFor(() => {
        expect(screen.getByText('admin')).toBeInTheDocument();
      });

      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]); // First user checkbox

      await waitFor(() => {
        expect(screen.getByText(/已选择 1 项/i)).toBeInTheDocument();
      });
    });

    it('应该显示批量删除按钮当有选中项', async () => {
      const user = userEvent.setup();
      renderWithProviders(<UsersPage />);

      await waitFor(() => {
        expect(screen.getByText('admin')).toBeInTheDocument();
      });

      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]);

      await waitFor(() => {
        expect(screen.getByText('批量删除')).toBeInTheDocument();
      });
    });
  });

  describe('角色管理', () => {
    it('应该显示升为管理员按钮对开发者', async () => {
      renderWithProviders(<UsersPage />);

      await waitFor(() => {
        expect(screen.getByText('developer')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /升为管理员/i })).toBeInTheDocument();
    });

    it('应该显示降为开发者按钮对管理员', async () => {
      renderWithProviders(<UsersPage />);

      await waitFor(() => {
        expect(screen.getByText('admin')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /降为开发者/i })).toBeInTheDocument();
    });

    it('应该调用toggleAdmin API', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.toggleAdmin).mockResolvedValue({
        ...mockUsers[1],
        role: UserRole.ADMIN,
      });

      renderWithProviders(<UsersPage />);

      await waitFor(() => {
        expect(screen.getByText('developer')).toBeInTheDocument();
      });

      const toggleButton = screen.getByRole('button', { name: /升为管理员/i });
      await user.click(toggleButton);

      await waitFor(() => {
        expect(userApi.toggleAdmin).toHaveBeenCalledWith(2);
      });
    });
  });

  describe('错误处理', () => {
    it('应该显示错误状态', async () => {
      vi.mocked(userApi.getUsers).mockRejectedValue(new Error('加载失败'));

      renderWithProviders(<UsersPage />);

      await waitFor(() => {
        expect(screen.getByText(/加载失败/i)).toBeInTheDocument();
      });
    });

    it('应该显示空状态当没有用户', async () => {
      vi.mocked(userApi.getUsers).mockResolvedValue({
        content: [],
        totalElements: 0,
        totalPages: 0,
        number: 0,
        size: 20,
      });

      renderWithProviders(<UsersPage />);

      await waitFor(() => {
        // Table default empty text is "暂无数据"
        expect(screen.getByText('暂无数据')).toBeInTheDocument();
      });
    });
  });
});
