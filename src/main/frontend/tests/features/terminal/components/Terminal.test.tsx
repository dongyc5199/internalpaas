import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, waitFor } from '@testing-library/react';
import { Terminal, type TerminalHandle } from '../../../../src/features/terminal/components/Terminal';
import { createRef } from 'react';
import '@testing-library/jest-dom';

// Mock xterm.js
const mockWrite = vi.fn();
const mockClear = vi.fn();
const mockFocus = vi.fn();
const mockDispose = vi.fn();
const mockOpen = vi.fn();
const mockLoadAddon = vi.fn();
const mockOnData = vi.fn((callback) => ({
  dispose: vi.fn(),
}));
const mockOnResize = vi.fn((callback) => ({
  dispose: vi.fn(),
}));
const mockFit = vi.fn();

vi.mock('@xterm/xterm', () => ({
  Terminal: vi.fn().mockImplementation(() => ({
    write: mockWrite,
    clear: mockClear,
    focus: mockFocus,
    dispose: mockDispose,
    open: mockOpen,
    loadAddon: mockLoadAddon,
    onData: mockOnData,
    onResize: mockOnResize,
    cols: 80,
    rows: 24,
    options: {},
  })),
}));

vi.mock('@xterm/addon-fit', () => ({
  FitAddon: vi.fn().mockImplementation(() => ({
    fit: mockFit,
  })),
}));

// Mock ResizeObserver
global.ResizeObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  disconnect: vi.fn(),
  unobserve: vi.fn(),
}));

describe('Terminal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('应该正确渲染终端容器', () => {
    const { container } = render(<Terminal />);

    const terminalWrapper = container.querySelector('.terminalWrapper');
    expect(terminalWrapper).toBeInTheDocument();

    const terminalContainer = container.querySelector('.terminalContainer');
    expect(terminalContainer).toBeInTheDocument();
  });

  it('应该在挂载时创建xterm实例', async () => {
    render(<Terminal />);

    await waitFor(() => {
      expect(mockOpen).toHaveBeenCalled();
    });
  });

  it('应该加载FitAddon', async () => {
    render(<Terminal />);

    await waitFor(() => {
      expect(mockLoadAddon).toHaveBeenCalled();
    });
  });

  it('应该在初始化后自动调整大小', async () => {
    render(<Terminal />);

    // Fast-forward past setTimeout(200ms)
    vi.advanceTimersByTime(200);

    await waitFor(() => {
      expect(mockFit).toHaveBeenCalled();
    });
  });

  it('应该在autoFocus为true时自动聚焦', async () => {
    render(<Terminal autoFocus={true} />);

    vi.advanceTimersByTime(200);

    await waitFor(() => {
      expect(mockFocus).toHaveBeenCalled();
    });
  });

  it('应该在autoFocus为false时不自动聚焦', async () => {
    render(<Terminal autoFocus={false} />);

    vi.advanceTimersByTime(200);

    await waitFor(() => {
      expect(mockFocus).not.toHaveBeenCalled();
    });
  });

  it('应该触发onReady回调', async () => {
    const handleReady = vi.fn();
    render(<Terminal onReady={handleReady} />);

    vi.advanceTimersByTime(200);

    await waitFor(() => {
      expect(handleReady).toHaveBeenCalled();
    });
  });

  it('应该监听onData事件', async () => {
    const handleData = vi.fn();
    render(<Terminal onData={handleData} />);

    await waitFor(() => {
      expect(mockOnData).toHaveBeenCalled();
    });
  });

  it('应该监听onResize事件', async () => {
    const handleResize = vi.fn();
    render(<Terminal onResize={handleResize} />);

    await waitFor(() => {
      expect(mockOnResize).toHaveBeenCalled();
    });
  });

  it('应该通过ref暴露write方法', async () => {
    const ref = createRef<TerminalHandle>();
    render(<Terminal ref={ref} />);

    await waitFor(() => {
      expect(ref.current).not.toBeNull();
    });

    ref.current?.write('test data');
    expect(mockWrite).toHaveBeenCalledWith('test data');
  });

  it('应该通过ref暴露clear方法', async () => {
    const ref = createRef<TerminalHandle>();
    render(<Terminal ref={ref} />);

    await waitFor(() => {
      expect(ref.current).not.toBeNull();
    });

    ref.current?.clear();
    expect(mockClear).toHaveBeenCalled();
  });

  it('应该通过ref暴露focus方法', async () => {
    const ref = createRef<TerminalHandle>();
    render(<Terminal ref={ref} />);

    await waitFor(() => {
      expect(ref.current).not.toBeNull();
    });

    ref.current?.focus();
    expect(mockFocus).toHaveBeenCalled();
  });

  it('应该通过ref暴露fit方法', async () => {
    const ref = createRef<TerminalHandle>();
    render(<Terminal ref={ref} />);

    await waitFor(() => {
      expect(ref.current).not.toBeNull();
    });

    ref.current?.fit();
    expect(mockFit).toHaveBeenCalled();
  });

  it('应该通过ref暴露getSize方法', async () => {
    const ref = createRef<TerminalHandle>();
    render(<Terminal ref={ref} />);

    await waitFor(() => {
      expect(ref.current).not.toBeNull();
    });

    const size = ref.current?.getSize();
    expect(size).toEqual({ cols: 80, rows: 24 });
  });

  it('应该通过ref暴露getXTerm方法', async () => {
    const ref = createRef<TerminalHandle>();
    render(<Terminal ref={ref} />);

    await waitFor(() => {
      expect(ref.current).not.toBeNull();
    });

    const xterm = ref.current?.getXTerm();
    expect(xterm).not.toBeNull();
  });

  it('应该接受自定义配置', () => {
    const config = {
      fontSize: 16,
      fontFamily: 'Monaco',
      cursorBlink: false,
    };

    render(<Terminal config={config} />);

    // XTerm constructor should be called with merged config
    expect(mockOpen).toHaveBeenCalled();
  });

  it('应该接受自定义主题', () => {
    const theme = {
      foreground: '#ffffff',
      background: '#000000',
    };

    render(<Terminal theme={theme} />);

    expect(mockOpen).toHaveBeenCalled();
  });

  it('应该接受自定义className', () => {
    const { container } = render(<Terminal className="custom-class" />);

    const terminalWrapper = container.querySelector('.terminalWrapper');
    expect(terminalWrapper).toHaveClass('custom-class');
  });

  it('应该在卸载时清理资源', async () => {
    const { unmount } = render(<Terminal />);

    await waitFor(() => {
      expect(mockOpen).toHaveBeenCalled();
    });

    unmount();

    await waitFor(() => {
      expect(mockDispose).toHaveBeenCalled();
    });
  });

  it('应该处理fit错误', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    mockFit.mockImplementationOnce(() => {
      throw new Error('Fit error');
    });

    const ref = createRef<TerminalHandle>();
    render(<Terminal ref={ref} />);

    await waitFor(() => {
      expect(ref.current).not.toBeNull();
    });

    ref.current?.fit();

    expect(consoleErrorSpy).toHaveBeenCalled();
    consoleErrorSpy.mockRestore();
  });

  it('应该处理open错误', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    mockOpen.mockImplementationOnce(() => {
      throw new Error('Open error');
    });

    render(<Terminal />);

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Error opening terminal:',
        expect.any(Error)
      );
    });

    consoleErrorSpy.mockRestore();
  });

  it('应该在主题变化时更新', async () => {
    const { rerender } = render(<Terminal theme={{ foreground: '#ffffff' }} />);

    await waitFor(() => {
      expect(mockOpen).toHaveBeenCalled();
    });

    rerender(<Terminal theme={{ foreground: '#000000' }} />);

    // Theme should be updated
    await waitFor(() => {
      expect(mockOpen).toHaveBeenCalled();
    });
  });

  it('应该设置ResizeObserver', async () => {
    render(<Terminal />);

    await waitFor(() => {
      expect(ResizeObserver).toHaveBeenCalled();
    });
  });

  it('应该在resize时调用fit', async () => {
    const mockObserve = vi.fn();
    const mockDisconnect = vi.fn();
    let resizeCallback: (() => void) | null = null;

    (global.ResizeObserver as any) = vi.fn().mockImplementation((callback) => {
      resizeCallback = callback;
      return {
        observe: mockObserve,
        disconnect: mockDisconnect,
        unobserve: vi.fn(),
      };
    });

    render(<Terminal />);

    await waitFor(() => {
      expect(mockObserve).toHaveBeenCalled();
    });

    // Trigger resize
    if (resizeCallback) {
      resizeCallback();
    }

    expect(mockFit).toHaveBeenCalled();
  });

  it('应该在配置变化时重新创建终端', async () => {
    const { rerender } = render(<Terminal config={{ fontSize: 14 }} />);

    await waitFor(() => {
      expect(mockOpen).toHaveBeenCalledTimes(1);
    });

    rerender(<Terminal config={{ fontSize: 16 }} />);

    await waitFor(() => {
      expect(mockDispose).toHaveBeenCalled();
      expect(mockOpen).toHaveBeenCalledTimes(2);
    });
  });

  it('应该返回默认大小当xterm未初始化', () => {
    const ref = createRef<TerminalHandle>();
    render(<Terminal ref={ref} />);

    // Before xterm is initialized
    const size = ref.current?.getSize();
    expect(size).toEqual({ cols: 80, rows: 24 });
  });

  it('应该在write时处理null xterm', async () => {
    const ref = createRef<TerminalHandle>();
    const { unmount } = render(<Terminal ref={ref} />);

    await waitFor(() => {
      expect(ref.current).not.toBeNull();
    });

    unmount();

    // After unmount, xterm should be null
    ref.current?.write('test');
    // Should not throw error
  });
});
