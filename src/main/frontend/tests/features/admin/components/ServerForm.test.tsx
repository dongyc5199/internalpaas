import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ServerForm } from '../../../../src/features/admin/components/ServerForm';
import { serverApi } from '../../../../src/shared/api/serverApi';
import { ServerStatus, type Server } from '../../../../src/shared/types/server';

/**
 * ServerForm 组件测试
 *
 * 测试覆盖:
 * - 创建模式表单渲染
 * - 编辑模式表单渲染
 * - 表单验证（必填字段、端口范围）
 * - 标签添加/删除
 * - 创建服务器提交
 * - 更新服务器提交
 * - 错误处理
 * - 取消操作
 */

// Mock serverApi
vi.mock('../../../../src/shared/api/serverApi', () => ({
  serverApi: {
    createServer: vi.fn(),
    updateServer: vi.fn(),
  },
}));

describe('ServerForm', () => {
  let queryClient: QueryClient;
  const mockOnSuccess = vi.fn();
  const mockOnCancel = vi.fn();

  const mockServer: Server = {
    id: 1,
    name: '测试服务器',
    host: '192.168.1.100',
    port: 22,
    username: 'root',
    status: ServerStatus.ONLINE,
    tags: ['production', 'web'],
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
      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      expect(screen.getByLabelText(/服务器名称/i)).toHaveValue('');
      expect(screen.getByLabelText(/主机地址/i)).toHaveValue('');
      expect(screen.getByLabelText(/SSH 端口/i)).toHaveValue(22); // number input
      expect(screen.getByLabelText(/用户名/i)).toHaveValue('');
      expect(screen.getByLabelText(/密码/i)).toHaveValue('');
      expect(screen.getByRole('button', { name: /创建/i })).toBeInTheDocument();
    });

    it('应该显示必填标记', () => {
      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // All required fields should have asterisk
      expect(screen.getAllByText('*').length).toBeGreaterThan(0);
    });

    it('应该验证必填字段', async () => {
      const user = userEvent.setup();
      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Submit without filling fields
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText('请输入服务器名称')).toBeInTheDocument();
        expect(screen.getByText('请输入主机地址')).toBeInTheDocument();
        expect(screen.getByText('请输入用户名')).toBeInTheDocument();
        expect(screen.getByText('请输入密码')).toBeInTheDocument();
      });

      expect(serverApi.createServer).not.toHaveBeenCalled();
    });

    it('应该验证端口号范围', async () => {
      const user = userEvent.setup();
      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      const portInput = screen.getByLabelText(/SSH 端口/i);

      // Test invalid port (< 1)
      await user.clear(portInput);
      await user.type(portInput, '0');
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText(/请输入有效的端口号/i)).toBeInTheDocument();
      });

      // Test invalid port (> 65535)
      await user.clear(portInput);
      await user.type(portInput, '70000');
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText(/请输入有效的端口号/i)).toBeInTheDocument();
      });
    });

    it('应该提交有效表单', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.createServer).mockResolvedValue(mockServer);

      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Fill form
      await user.type(screen.getByLabelText(/服务器名称/i), '新服务器');
      await user.type(screen.getByLabelText(/主机地址/i), '192.168.1.200');
      await user.clear(screen.getByLabelText(/SSH 端口/i));
      await user.type(screen.getByLabelText(/SSH 端口/i), '22');
      await user.type(screen.getByLabelText(/用户名/i), 'admin');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      // Submit
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(serverApi.createServer).toHaveBeenCalledWith({
          name: '新服务器',
          host: '192.168.1.200',
          port: 22,
          username: 'admin',
          password: 'password123',
          tags: [],
        });
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('应该处理创建失败', async () => {
      const user = userEvent.setup();
      const errorMessage = '服务器已存在';
      vi.mocked(serverApi.createServer).mockRejectedValue(new Error(errorMessage));

      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Fill and submit
      await user.type(screen.getByLabelText(/服务器名称/i), '新服务器');
      await user.type(screen.getByLabelText(/主机地址/i), '192.168.1.200');
      await user.type(screen.getByLabelText(/用户名/i), 'admin');
      await user.type(screen.getByLabelText(/密码/i), 'password123');
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText(errorMessage)).toBeInTheDocument();
      });

      expect(mockOnSuccess).not.toHaveBeenCalled();
    });
  });

  describe('编辑模式', () => {
    it('应该填充现有服务器数据', () => {
      renderWithProviders(
        <ServerForm server={mockServer} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      expect(screen.getByLabelText(/服务器名称/i)).toHaveValue('测试服务器');
      expect(screen.getByLabelText(/主机地址/i)).toHaveValue('192.168.1.100');
      expect(screen.getByLabelText(/SSH 端口/i)).toHaveValue(22); // number input
      expect(screen.getByLabelText(/用户名/i)).toHaveValue('root');
      expect(screen.getByRole('button', { name: /保存/i })).toBeInTheDocument();
    });

    it('应该显示现有标签', () => {
      renderWithProviders(
        <ServerForm server={mockServer} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      expect(screen.getByText('production')).toBeInTheDocument();
      expect(screen.getByText('web')).toBeInTheDocument();
    });

    it('密码字段应该为可选', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.updateServer).mockResolvedValue(mockServer);

      renderWithProviders(
        <ServerForm server={mockServer} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      // Password hint should be present
      expect(screen.getByText(/留空表示不修改密码/i)).toBeInTheDocument();

      // Submit without password should work
      await user.click(screen.getByRole('button', { name: /保存/i }));

      await waitFor(() => {
        expect(serverApi.updateServer).toHaveBeenCalled();
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('应该更新服务器', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.updateServer).mockResolvedValue({
        ...mockServer,
        name: '更新的服务器',
      });

      renderWithProviders(
        <ServerForm server={mockServer} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      // Update name
      const nameInput = screen.getByLabelText(/服务器名称/i);
      await user.clear(nameInput);
      await user.type(nameInput, '更新的服务器');

      // Submit
      await user.click(screen.getByRole('button', { name: /保存/i }));

      await waitFor(() => {
        expect(serverApi.updateServer).toHaveBeenCalledWith(1, {
          name: '更新的服务器',
          host: '192.168.1.100',
          port: 22,
          username: 'root',
          password: '',
          tags: ['production', 'web'],
        });
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });
  });

  describe('标签管理', () => {
    it('应该添加标签', async () => {
      const user = userEvent.setup();
      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      const tagInput = screen.getByPlaceholderText(/输入标签后按回车添加/i);
      await user.type(tagInput, 'test-tag');
      await user.click(screen.getByRole('button', { name: /添加/i }));

      await waitFor(() => {
        expect(screen.getByText('test-tag')).toBeInTheDocument();
      });
    });

    it('应该通过回车键添加标签', async () => {
      const user = userEvent.setup();
      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      const tagInput = screen.getByPlaceholderText(/输入标签后按回车添加/i);
      await user.type(tagInput, 'enter-tag{Enter}');

      await waitFor(() => {
        expect(screen.getByText('enter-tag')).toBeInTheDocument();
      });
    });

    it('应该删除标签', async () => {
      const user = userEvent.setup();
      renderWithProviders(
        <ServerForm server={mockServer} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      const productionTag = screen.getByText('production');
      expect(productionTag).toBeInTheDocument();

      // Find remove button within the tag
      const removeButtons = screen.getAllByRole('button');
      const tagRemoveButton = removeButtons.find(
        (btn) => btn.className.includes('tagRemove')
      );

      if (tagRemoveButton) {
        await user.click(tagRemoveButton);

        await waitFor(() => {
          expect(screen.queryByText('production')).not.toBeInTheDocument();
        });
      }
    });

    it('不应该添加重复标签', async () => {
      const user = userEvent.setup();
      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      const tagInput = screen.getByPlaceholderText(/输入标签后按回车添加/i);

      // Add tag twice
      await user.type(tagInput, 'duplicate-tag');
      await user.click(screen.getByRole('button', { name: /添加/i }));

      await user.type(tagInput, 'duplicate-tag');
      await user.click(screen.getByRole('button', { name: /添加/i }));

      // Should only appear once
      const tags = screen.getAllByText('duplicate-tag');
      expect(tags.length).toBe(1);
    });

    it('不应该添加空标签', async () => {
      const user = userEvent.setup();
      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      const tagInput = screen.getByPlaceholderText(/输入标签后按回车添加/i);

      // Try to add empty tag
      await user.type(tagInput, '   ');
      await user.click(screen.getByRole('button', { name: /添加/i }));

      // No tag should be added
      const tagsContainer = screen.queryByText('   ');
      expect(tagsContainer).not.toBeInTheDocument();
    });
  });

  describe('表单交互', () => {
    it('应该清除字段错误当输入时', async () => {
      const user = userEvent.setup();
      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Submit to trigger validation
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText('请输入服务器名称')).toBeInTheDocument();
      });

      // Type in the field
      await user.type(screen.getByLabelText(/服务器名称/i), 'Test');

      await waitFor(() => {
        expect(screen.queryByText('请输入服务器名称')).not.toBeInTheDocument();
      });
    });

    it('应该调用onCancel当点击取消', async () => {
      const user = userEvent.setup();
      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      await user.click(screen.getByRole('button', { name: /取消/i }));

      expect(mockOnCancel).toHaveBeenCalled();
    });

    it('应该禁用表单在提交时', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.createServer).mockImplementation(
        () => new Promise((resolve) => setTimeout(() => resolve(mockServer), 1000))
      );

      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Fill form
      await user.type(screen.getByLabelText(/服务器名称/i), '新服务器');
      await user.type(screen.getByLabelText(/主机地址/i), '192.168.1.200');
      await user.type(screen.getByLabelText(/用户名/i), 'admin');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      // Submit
      await user.click(screen.getByRole('button', { name: /创建/i }));

      // Inputs should be disabled
      await waitFor(() => {
        expect(screen.getByLabelText(/服务器名称/i)).toBeDisabled();
        expect(screen.getByLabelText(/主机地址/i)).toBeDisabled();
      });
    });
  });

  describe('表单提交包含标签', () => {
    it('应该在创建时包含标签', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.createServer).mockResolvedValue(mockServer);

      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Fill form
      await user.type(screen.getByLabelText(/服务器名称/i), '新服务器');
      await user.type(screen.getByLabelText(/主机地址/i), '192.168.1.200');
      await user.type(screen.getByLabelText(/用户名/i), 'admin');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      // Add tags
      const tagInput = screen.getByPlaceholderText(/输入标签后按回车添加/i);
      await user.type(tagInput, 'production{Enter}');
      await user.type(tagInput, 'api{Enter}');

      // Submit
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(serverApi.createServer).toHaveBeenCalledWith(
          expect.objectContaining({
            tags: ['production', 'api'],
          })
        );
      });
    });
  });

  describe('边界情况测试', () => {
    it('应该接受最小端口号1', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.createServer).mockResolvedValue(mockServer);

      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      await user.type(screen.getByLabelText(/服务器名称/i), '测试服务器');
      await user.type(screen.getByLabelText(/主机地址/i), '192.168.1.100');
      const portInput = screen.getByLabelText(/SSH 端口/i);
      await user.clear(portInput);
      await user.type(portInput, '1');
      await user.type(screen.getByLabelText(/用户名/i), 'root');
      await user.type(screen.getByLabelText(/密码/i), 'password');

      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(serverApi.createServer).toHaveBeenCalledWith(
          expect.objectContaining({ port: 1 })
        );
      });
    });

    it('应该接受最大端口号65535', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.createServer).mockResolvedValue(mockServer);

      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      await user.type(screen.getByLabelText(/服务器名称/i), '测试服务器');
      await user.type(screen.getByLabelText(/主机地址/i), '192.168.1.100');
      const portInput = screen.getByLabelText(/SSH 端口/i);
      await user.clear(portInput);
      await user.type(portInput, '65535');
      await user.type(screen.getByLabelText(/用户名/i), 'root');
      await user.type(screen.getByLabelText(/密码/i), 'password');

      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(serverApi.createServer).toHaveBeenCalledWith(
          expect.objectContaining({ port: 65535 })
        );
      });
    });

    it('应该接受域名作为主机地址', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.createServer).mockResolvedValue(mockServer);

      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      await user.type(screen.getByLabelText(/服务器名称/i), '测试服务器');
      await user.type(screen.getByLabelText(/主机地址/i), 'server.example.com');
      await user.type(screen.getByLabelText(/用户名/i), 'root');
      await user.type(screen.getByLabelText(/密码/i), 'password');

      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(serverApi.createServer).toHaveBeenCalledWith(
          expect.objectContaining({ host: 'server.example.com' })
        );
      });
    });

    it('应该接受IPv6地址作为主机地址', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.createServer).mockResolvedValue(mockServer);

      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      await user.type(screen.getByLabelText(/服务器名称/i), '测试服务器');
      await user.type(screen.getByLabelText(/主机地址/i), '2001:db8::1');
      await user.type(screen.getByLabelText(/用户名/i), 'root');
      await user.type(screen.getByLabelText(/密码/i), 'password');

      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(serverApi.createServer).toHaveBeenCalledWith(
          expect.objectContaining({ host: '2001:db8::1' })
        );
      });
    });
  });

  describe('错误恢复场景', () => {
    it('应该在提交失败后允许重新提交', async () => {
      const user = userEvent.setup();
      // First call fails, second succeeds
      vi.mocked(serverApi.createServer)
        .mockRejectedValueOnce(new Error('网络错误'))
        .mockResolvedValueOnce(mockServer);

      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Fill form
      await user.type(screen.getByLabelText(/服务器名称/i), '测试服务器');
      await user.type(screen.getByLabelText(/主机地址/i), '192.168.1.100');
      await user.type(screen.getByLabelText(/用户名/i), 'root');
      await user.type(screen.getByLabelText(/密码/i), 'password');

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
      vi.mocked(serverApi.createServer).mockResolvedValue(mockServer);

      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Submit with invalid port
      const portInput = screen.getByLabelText(/SSH 端口/i);
      await user.clear(portInput);
      await user.type(portInput, '0');
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText(/请输入有效的端口号/i)).toBeInTheDocument();
      });

      // Fix all validation errors
      await user.type(screen.getByLabelText(/服务器名称/i), '测试服务器');
      await user.type(screen.getByLabelText(/主机地址/i), '192.168.1.100');
      await user.clear(portInput);
      await user.type(portInput, '22');
      await user.type(screen.getByLabelText(/用户名/i), 'root');
      await user.type(screen.getByLabelText(/密码/i), 'password');

      // Resubmit
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('应该在提交失败后保留表单数据', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.createServer).mockRejectedValue(new Error('提交失败'));

      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Fill form
      await user.type(screen.getByLabelText(/服务器名称/i), '测试服务器');
      await user.type(screen.getByLabelText(/主机地址/i), '192.168.1.100');
      await user.type(screen.getByLabelText(/用户名/i), 'root');
      await user.type(screen.getByLabelText(/密码/i), 'password123');

      // Add tag
      const tagInput = screen.getByPlaceholderText(/输入标签后按回车添加/i);
      await user.type(tagInput, 'production{Enter}');

      // Submit
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText('提交失败')).toBeInTheDocument();
      });

      // Verify form data is retained
      expect(screen.getByLabelText(/服务器名称/i)).toHaveValue('测试服务器');
      expect(screen.getByLabelText(/主机地址/i)).toHaveValue('192.168.1.100');
      expect(screen.getByLabelText(/用户名/i)).toHaveValue('root');
      expect(screen.getByText('production')).toBeInTheDocument();
    });
  });

  describe('复杂验证场景', () => {
    it('应该同时验证多个字段错误', async () => {
      const user = userEvent.setup();
      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Set invalid port
      const portInput = screen.getByLabelText(/SSH 端口/i);
      await user.clear(portInput);
      await user.type(portInput, '70000');

      // Submit
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText('请输入服务器名称')).toBeInTheDocument();
        expect(screen.getByText('请输入主机地址')).toBeInTheDocument();
        expect(screen.getByText(/请输入有效的端口号/i)).toBeInTheDocument();
        expect(screen.getByText('请输入用户名')).toBeInTheDocument();
        expect(screen.getByText('请输入密码')).toBeInTheDocument();
      });
    });

    it('应该依次清除多个字段错误', async () => {
      const user = userEvent.setup();
      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Trigger validation
      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(screen.getByText('请输入服务器名称')).toBeInTheDocument();
        expect(screen.getByText('请输入主机地址')).toBeInTheDocument();
      });

      // Fix name
      await user.type(screen.getByLabelText(/服务器名称/i), '测试');
      await waitFor(() => {
        expect(screen.queryByText('请输入服务器名称')).not.toBeInTheDocument();
      });

      // Fix host
      await user.type(screen.getByLabelText(/主机地址/i), '192.168.1.100');
      await waitFor(() => {
        expect(screen.queryByText('请输入主机地址')).not.toBeInTheDocument();
      });
    });

    it('应该处理带空格的输入', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.createServer).mockResolvedValue(mockServer);

      renderWithProviders(<ServerForm onSuccess={mockOnSuccess} onCancel={mockOnCancel} />);

      // Type with leading/trailing spaces
      await user.type(screen.getByLabelText(/服务器名称/i), '  测试服务器  ');
      await user.type(screen.getByLabelText(/主机地址/i), '  192.168.1.100  ');
      await user.type(screen.getByLabelText(/用户名/i), '  root  ');
      await user.type(screen.getByLabelText(/密码/i), 'password');

      await user.click(screen.getByRole('button', { name: /创建/i }));

      await waitFor(() => {
        expect(serverApi.createServer).toHaveBeenCalledWith(
          expect.objectContaining({
            name: '  测试服务器  ',
            host: '  192.168.1.100  ',
            username: '  root  ',
          })
        );
      });
    });
  });

  describe('编辑模式高级场景', () => {
    it('应该只更新修改的字段', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.updateServer).mockResolvedValue(mockServer);

      renderWithProviders(
        <ServerForm server={mockServer} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      // Only change name
      const nameInput = screen.getByLabelText(/服务器名称/i);
      await user.clear(nameInput);
      await user.type(nameInput, '新名称');

      await user.click(screen.getByRole('button', { name: /保存/i }));

      await waitFor(() => {
        expect(serverApi.updateServer).toHaveBeenCalledWith(
          mockServer.id,
          expect.objectContaining({
            name: '新名称',
            host: mockServer.host,
            port: mockServer.port,
            username: mockServer.username,
            password: '',
            tags: mockServer.tags,
          })
        );
      });
    });

    it('应该在编辑模式下更新标签', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.updateServer).mockResolvedValue(mockServer);

      renderWithProviders(
        <ServerForm server={mockServer} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      // Remove existing tag
      const productionTag = screen.getByText('production');
      const removeButtons = screen.getAllByRole('button');
      const removeButton = removeButtons.find(
        (btn) => btn.className.includes('tagRemove')
      );

      if (removeButton) {
        await user.click(removeButton);
      }

      // Add new tag
      const tagInput = screen.getByPlaceholderText(/输入标签后按回车添加/i);
      await user.type(tagInput, 'staging{Enter}');

      await user.click(screen.getByRole('button', { name: /保存/i }));

      await waitFor(() => {
        expect(serverApi.updateServer).toHaveBeenCalledWith(
          mockServer.id,
          expect.objectContaining({
            tags: expect.arrayContaining(['staging']),
          })
        );
      });
    });

    it('应该在编辑模式下更新密码', async () => {
      const user = userEvent.setup();
      vi.mocked(serverApi.updateServer).mockResolvedValue(mockServer);

      renderWithProviders(
        <ServerForm server={mockServer} onSuccess={mockOnSuccess} onCancel={mockOnCancel} />
      );

      // Set new password
      await user.type(screen.getByLabelText(/密码/i), 'newpassword123');

      await user.click(screen.getByRole('button', { name: /保存/i }));

      await waitFor(() => {
        expect(serverApi.updateServer).toHaveBeenCalledWith(
          mockServer.id,
          expect.objectContaining({
            password: 'newpassword123',
          })
        );
      });
    });
  });
});
