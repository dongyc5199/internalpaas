import { render } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { LayoutSelector } from '../../../react-app/components/LayoutSelector';
import { LayoutContext } from '../../../react-app/contexts/layoutContext';
import type { LayoutContextValue } from '../../../react-app/types/navigation';

// Mock layout components
vi.mock('../../../react-app/layout/ShellLayout', () => ({
  ShellLayout: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="shell-layout">{children}</div>
  )
}));

vi.mock('../../../react-app/components/ContentOnlyLayout', () => ({
  ContentOnlyLayout: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="content-only-layout">{children}</div>
  )
}));

describe('LayoutSelector', () => {
  let consoleSpy: any;

  beforeEach(() => {
    consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  /**
   * Helper function to render LayoutSelector with mocked context
   */
  function renderWithContext(contextValue: LayoutContextValue) {
    return render(
      <LayoutContext.Provider value={contextValue}>
        <LayoutSelector>
          <div>Test Content</div>
        </LayoutSelector>
      </LayoutContext.Provider>
    );
  }

  describe('Layout Component Selection', () => {
    it('should render ShellLayout when mode is "shell"', () => {
      const contextValue: LayoutContextValue = {
        mode: 'shell',
        setMode: vi.fn(),
        isEmbedded: false
      };

      const { getByTestId, getByText } = renderWithContext(contextValue);

      expect(getByTestId('shell-layout')).toBeInTheDocument();
      expect(getByText('Test Content')).toBeInTheDocument();
    });

    it('should render ContentOnlyLayout when mode is "content-only"', () => {
      const contextValue: LayoutContextValue = {
        mode: 'content-only',
        setMode: vi.fn(),
        isEmbedded: true
      };

      const { getByTestId, getByText } = renderWithContext(contextValue);

      expect(getByTestId('content-only-layout')).toBeInTheDocument();
      expect(getByText('Test Content')).toBeInTheDocument();
    });

    it('should render ContentOnlyLayout when mode is "minimal"', () => {
      const contextValue: LayoutContextValue = {
        mode: 'minimal',
        setMode: vi.fn(),
        isEmbedded: false
      };

      const { getByTestId, getByText } = renderWithContext(contextValue);

      expect(getByTestId('content-only-layout')).toBeInTheDocument();
      expect(getByText('Test Content')).toBeInTheDocument();
    });
  });

  describe('Unknown Mode Handling', () => {
    it('should fallback to ContentOnlyLayout for unknown mode', () => {
      const contextValue: LayoutContextValue = {
        mode: 'unknown-mode' as any, // 强制类型转换模拟未知模式
        setMode: vi.fn(),
        isEmbedded: false
      };

      const { getByTestId } = renderWithContext(contextValue);

      expect(getByTestId('content-only-layout')).toBeInTheDocument();
    });

    it('should warn when encountering unknown mode', () => {
      const contextValue: LayoutContextValue = {
        mode: 'invalid-mode' as any,
        setMode: vi.fn(),
        isEmbedded: false
      };

      renderWithContext(contextValue);

      expect(consoleSpy).toHaveBeenCalledWith(
        '[LayoutSelector] Unknown mode: invalid-mode, falling back to content-only'
      );
    });
  });

  describe('Children Rendering', () => {
    it('should pass children to selected layout component', () => {
      const contextValue: LayoutContextValue = {
        mode: 'shell',
        setMode: vi.fn(),
        isEmbedded: false
      };

      const { getByText } = render(
        <LayoutContext.Provider value={contextValue}>
          <LayoutSelector>
            <div>Child Component 1</div>
            <div>Child Component 2</div>
          </LayoutSelector>
        </LayoutContext.Provider>
      );

      expect(getByText('Child Component 1')).toBeInTheDocument();
      expect(getByText('Child Component 2')).toBeInTheDocument();
    });

    it('should render complex children structure', () => {
      const contextValue: LayoutContextValue = {
        mode: 'content-only',
        setMode: vi.fn(),
        isEmbedded: true
      };

      const ComplexChild = () => (
        <div>
          <header>Header</header>
          <main>Main Content</main>
          <footer>Footer</footer>
        </div>
      );

      const { getByText } = render(
        <LayoutContext.Provider value={contextValue}>
          <LayoutSelector>
            <ComplexChild />
          </LayoutSelector>
        </LayoutContext.Provider>
      );

      expect(getByText('Header')).toBeInTheDocument();
      expect(getByText('Main Content')).toBeInTheDocument();
      expect(getByText('Footer')).toBeInTheDocument();
    });
  });

  describe('Component Memoization', () => {
    it('should memoize layout component selection', () => {
      const contextValue: LayoutContextValue = {
        mode: 'shell',
        setMode: vi.fn(),
        isEmbedded: false
      };

      const { rerender, getByTestId } = renderWithContext(contextValue);

      const firstRender = getByTestId('shell-layout');

      // 强制重渲染,但mode不变
      rerender(
        <LayoutContext.Provider value={contextValue}>
          <LayoutSelector>
            <div>Test Content</div>
          </LayoutSelector>
        </LayoutContext.Provider>
      );

      const secondRender = getByTestId('shell-layout');

      // 组件应该是相同的实例(useMemo工作)
      expect(firstRender).toBe(secondRender);
    });

    it('should update layout component when mode changes', () => {
      const contextValue1: LayoutContextValue = {
        mode: 'shell',
        setMode: vi.fn(),
        isEmbedded: false
      };

      const { rerender, getByTestId, queryByTestId } = renderWithContext(contextValue1);

      expect(getByTestId('shell-layout')).toBeInTheDocument();

      // 模拟mode切换
      const contextValue2: LayoutContextValue = {
        mode: 'content-only',
        setMode: vi.fn(),
        isEmbedded: true
      };

      rerender(
        <LayoutContext.Provider value={contextValue2}>
          <LayoutSelector>
            <div>Test Content</div>
          </LayoutSelector>
        </LayoutContext.Provider>
      );

      expect(queryByTestId('shell-layout')).not.toBeInTheDocument();
      expect(getByTestId('content-only-layout')).toBeInTheDocument();
    });
  });

  describe('Error Handling', () => {
    it('should throw error when used outside LayoutProvider', () => {
      // 禁用错误边界的console.error输出
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      expect(() => {
        render(
          <LayoutSelector>
            <div>Test Content</div>
          </LayoutSelector>
        );
      }).toThrow('useLayout must be used within LayoutProvider');

      consoleErrorSpy.mockRestore();
    });
  });

  describe('Mode Transition Scenarios', () => {
    it('should handle shell → content-only transition', () => {
      const contextValue: LayoutContextValue = {
        mode: 'shell',
        setMode: vi.fn(),
        isEmbedded: false
      };

      const { rerender, getByTestId, queryByTestId } = renderWithContext(contextValue);

      expect(getByTestId('shell-layout')).toBeInTheDocument();

      // 切换到content-only
      rerender(
        <LayoutContext.Provider value={{ ...contextValue, mode: 'content-only', isEmbedded: true }}>
          <LayoutSelector>
            <div>Test Content</div>
          </LayoutSelector>
        </LayoutContext.Provider>
      );

      expect(queryByTestId('shell-layout')).not.toBeInTheDocument();
      expect(getByTestId('content-only-layout')).toBeInTheDocument();
    });

    it('should handle content-only → shell transition', () => {
      const contextValue: LayoutContextValue = {
        mode: 'content-only',
        setMode: vi.fn(),
        isEmbedded: true
      };

      const { rerender, getByTestId, queryByTestId } = renderWithContext(contextValue);

      expect(getByTestId('content-only-layout')).toBeInTheDocument();

      // 切换到shell
      rerender(
        <LayoutContext.Provider value={{ ...contextValue, mode: 'shell', isEmbedded: false }}>
          <LayoutSelector>
            <div>Test Content</div>
          </LayoutSelector>
        </LayoutContext.Provider>
      );

      expect(queryByTestId('content-only-layout')).not.toBeInTheDocument();
      expect(getByTestId('shell-layout')).toBeInTheDocument();
    });

    it('should handle minimal → shell transition', () => {
      const contextValue: LayoutContextValue = {
        mode: 'minimal',
        setMode: vi.fn(),
        isEmbedded: false
      };

      const { rerender, getByTestId, queryByTestId } = renderWithContext(contextValue);

      expect(getByTestId('content-only-layout')).toBeInTheDocument();

      // 切换到shell
      rerender(
        <LayoutContext.Provider value={{ ...contextValue, mode: 'shell' }}>
          <LayoutSelector>
            <div>Test Content</div>
          </LayoutSelector>
        </LayoutContext.Provider>
      );

      expect(queryByTestId('content-only-layout')).not.toBeInTheDocument();
      expect(getByTestId('shell-layout')).toBeInTheDocument();
    });
  });
});
