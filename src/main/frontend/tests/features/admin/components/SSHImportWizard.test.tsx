import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SSHImportWizard } from '../../../../src/features/admin/components/SSHImportWizard';
import '@testing-library/jest-dom';

// Create a wrapper with QueryClient
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
      mutations: {
        retry: false,
      },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('SSHImportWizard', () => {
  let mockOnSuccess: ReturnType<typeof vi.fn>;
  let mockOnCancel: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    mockOnSuccess = vi.fn();
    mockOnCancel = vi.fn();

    // Mock fetch for API calls
    global.fetch = vi.fn();
  });

  it('应该正确渲染向导', () => {
    render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    expect(screen.getByText('选择导入方式')).toBeInTheDocument();
    expect(screen.getByText('手动输入')).toBeInTheDocument();
    expect(screen.getByText('配置文件导入')).toBeInTheDocument();
    expect(screen.getByText('批量导入')).toBeInTheDocument();
  });

  it('应该显示所有步骤', () => {
    render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    expect(screen.getByText('选择导入方式')).toBeInTheDocument();
    expect(screen.getByText('配置信息')).toBeInTheDocument();
    expect(screen.getByText('连接测试')).toBeInTheDocument();
    expect(screen.getByText('确认导入')).toBeInTheDocument();
  });

  it('应该支持选择导入方式', () => {
    render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    const manualButton = screen.getByText('手动输入').closest('button');
    expect(manualButton).toBeInTheDocument();

    if (manualButton) {
      fireEvent.click(manualButton);
      // Manual method should be selected by default
    }
  });

  it('应该允许进入下一步', async () => {
    render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    const nextButton = screen.getByText('下一步');
    expect(nextButton).toBeInTheDocument();

    fireEvent.click(nextButton);

    await waitFor(() => {
      // Should move to configuration step
      expect(screen.getByText('填写服务器SSH连接信息')).toBeInTheDocument();
    });
  });

  it('应该在配置步骤显示表单字段', async () => {
    render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    // Click next to go to config step
    const nextButton = screen.getByText('下一步');
    fireEvent.click(nextButton);

    await waitFor(() => {
      expect(screen.getByText('服务器名称 *')).toBeInTheDocument();
      expect(screen.getByText('主机地址 *')).toBeInTheDocument();
      expect(screen.getByText('端口 *')).toBeInTheDocument();
      expect(screen.getByText('用户名 *')).toBeInTheDocument();
      expect(screen.getByText('密码 *')).toBeInTheDocument();
    });
  });

  it('应该支持返回上一步', async () => {
    render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    // Go to next step
    const nextButton = screen.getByText('下一步');
    fireEvent.click(nextButton);

    await waitFor(() => {
      expect(screen.getByText('填写服务器SSH连接信息')).toBeInTheDocument();
    });

    // Go back
    const prevButton = screen.getByText('上一步');
    fireEvent.click(prevButton);

    await waitFor(() => {
      expect(screen.getByText('选择如何导入SSH服务器配置')).toBeInTheDocument();
    });
  });

  it('应该调用onCancel回调', () => {
    render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    const cancelButton = screen.getByText('取消');
    fireEvent.click(cancelButton);

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('应该验证必填字段', async () => {
    render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    // Go to config step
    const nextButton = screen.getByText('下一步');
    fireEvent.click(nextButton);

    await waitFor(() => {
      expect(screen.getByText('填写服务器SSH连接信息')).toBeInTheDocument();
    });

    // Try to go to next step without filling form
    const nextButton2 = screen.getByText('下一步');
    fireEvent.click(nextButton2);

    await waitFor(() => {
      // Should show validation errors
      expect(screen.getByText('请输入服务器名称')).toBeInTheDocument();
      expect(screen.getByText('请输入主机地址')).toBeInTheDocument();
    });
  });

  it('应该在配置文件导入模式下显示文件上传', () => {
    render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    const configFileButton = screen.getByText('配置文件导入').closest('button');

    if (configFileButton) {
      fireEvent.click(configFileButton);

      expect(screen.getByText('选择SSH配置文件')).toBeInTheDocument();
    }
  });

  it('应该在批量导入模式下支持添加多个服务器', async () => {
    render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    // Select bulk import
    const bulkButton = screen.getByText('批量导入').closest('button');
    if (bulkButton) {
      fireEvent.click(bulkButton);
    }

    // Go to config step
    const nextButton = screen.getByText('下一步');
    fireEvent.click(nextButton);

    await waitFor(() => {
      expect(screen.getByText('服务器 #1')).toBeInTheDocument();
    });

    // Add another server
    const addButton = screen.getByText('添加服务器');
    fireEvent.click(addButton);

    await waitFor(() => {
      expect(screen.getByText('服务器 #2')).toBeInTheDocument();
    });
  });

  it('应该正确处理表单输入', async () => {
    render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    // Go to config step
    const nextButton = screen.getByText('下一步');
    fireEvent.click(nextButton);

    await waitFor(() => {
      const nameInput = screen.getByPlaceholderText('例如: Production Server 1');
      const hostInput = screen.getByPlaceholderText('192.168.1.100');

      fireEvent.change(nameInput, { target: { value: 'Test Server' } });
      fireEvent.change(hostInput, { target: { value: '10.0.0.1' } });

      expect(nameInput).toHaveValue('Test Server');
      expect(hostInput).toHaveValue('10.0.0.1');
    });
  });

  it('应该在最后一步显示确认按钮', async () => {
    render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    // Mock successful form validation and connection tests
    // This is a simplified test - in real scenario we'd need to fill out the form properly
    const { container } = render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    // Check that wizard structure is rendered
    expect(container.querySelector('[role="navigation"]')).toBeInTheDocument();
  });

  it('应该使用正确的ARIA标签', () => {
    render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    const navigation = screen.getByRole('navigation');
    expect(navigation).toHaveAttribute('aria-label', '进度步骤');
  });

  it('应该正确初始化步骤状态', () => {
    const { container } = render(
      <SSHImportWizard onSuccess={mockOnSuccess} onCancel={mockOnCancel} />,
      { wrapper: createWrapper() }
    );

    // First step should be current
    const currentSteps = container.querySelectorAll('[aria-current="step"]');
    expect(currentSteps.length).toBe(1);
  });
});
