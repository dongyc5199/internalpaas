import { render, renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { LayoutProvider } from '../../../react-app/providers/LayoutProvider';
import { useLayout } from '../../../react-app/contexts/layoutContext';
import { ReactNode } from 'react';

// Mock useEmbedMode hook
vi.mock('../../../react-app/hooks/useEmbedMode', () => ({
  useEmbedMode: vi.fn()
}));

import { useEmbedMode } from '../../../react-app/hooks/useEmbedMode';

describe('LayoutProvider', () => {
  beforeEach(() => {
    // 清理Window全局标志
    delete (window as any).__DEPLOY_PLATFORM_DEBUG__;

    // 默认返回非嵌入模式
    vi.mocked(useEmbedMode).mockReturnValue(false);

    // 模拟console.log
    vi.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Context Provision', () => {
    it('should provide layout context to children', () => {
      const { result } = renderHook(() => useLayout(), {
        wrapper: ({ children }: { children: ReactNode }) => (
          <LayoutProvider>{children}</LayoutProvider>
        )
      });

      expect(result.current).toBeDefined();
      expect(result.current.mode).toBeDefined();
      expect(result.current.setMode).toBeDefined();
      expect(result.current.isEmbedded).toBeDefined();
    });

    it('should throw error when useLayout is used outside LayoutProvider', () => {
      expect(() => {
        renderHook(() => useLayout());
      }).toThrow('useLayout must be used within LayoutProvider');
    });
  });

  describe('Initial Mode Detection', () => {
    it('should set mode to "shell" when NOT embedded (standalone mode)', () => {
      vi.mocked(useEmbedMode).mockReturnValue(false);

      const { result } = renderHook(() => useLayout(), {
        wrapper: ({ children }: { children: ReactNode }) => (
          <LayoutProvider>{children}</LayoutProvider>
        )
      });

      expect(result.current.mode).toBe('shell');
      expect(result.current.isEmbedded).toBe(false);
    });

    it('should set mode to "content-only" when embedded', () => {
      vi.mocked(useEmbedMode).mockReturnValue(true);

      const { result } = renderHook(() => useLayout(), {
        wrapper: ({ children }: { children: ReactNode }) => (
          <LayoutProvider>{children}</LayoutProvider>
        )
      });

      expect(result.current.mode).toBe('content-only');
      expect(result.current.isEmbedded).toBe(true);
    });
  });

  describe('Mode Switching', () => {
    it('should allow mode switching via setMode', async () => {
      vi.mocked(useEmbedMode).mockReturnValue(false);

      const { result } = renderHook(() => useLayout(), {
        wrapper: ({ children }: { children: ReactNode }) => (
          <LayoutProvider>{children}</LayoutProvider>
        )
      });

      expect(result.current.mode).toBe('shell');

      // 切换到content-only
      result.current.setMode('content-only');
      await waitFor(() => {
        expect(result.current.mode).toBe('content-only');
      });
    });

    it('should support all three layout modes', async () => {
      vi.mocked(useEmbedMode).mockReturnValue(false);

      const { result } = renderHook(() => useLayout(), {
        wrapper: ({ children }: { children: ReactNode }) => (
          <LayoutProvider>{children}</LayoutProvider>
        )
      });

      // 初始状态: shell
      expect(result.current.mode).toBe('shell');

      // 切换到content-only
      result.current.setMode('content-only');
      await waitFor(() => {
        expect(result.current.mode).toBe('content-only');
      });

      // 切换到minimal
      result.current.setMode('minimal');
      await waitFor(() => {
        expect(result.current.mode).toBe('minimal');
      });

      // 切换回shell
      result.current.setMode('shell');
      await waitFor(() => {
        expect(result.current.mode).toBe('shell');
      });
    });
  });

  describe('Debug Logging', () => {
    it('should log mode changes when __DEPLOY_PLATFORM_DEBUG__ is true', async () => {
      const consoleSpy = vi.spyOn(console, 'log');
      (window as any).__DEPLOY_PLATFORM_DEBUG__ = true;

      vi.mocked(useEmbedMode).mockReturnValue(false);

      const { result } = renderHook(() => useLayout(), {
        wrapper: ({ children }: { children: ReactNode }) => (
          <LayoutProvider>{children}</LayoutProvider>
        )
      });

      result.current.setMode('content-only');

      await waitFor(() => {
        expect(consoleSpy).toHaveBeenCalledWith(
          '[LayoutProvider] Mode changed:',
          'content-only'
        );
      });
    });

    it('should NOT log when __DEPLOY_PLATFORM_DEBUG__ is false', async () => {
      const consoleSpy = vi.spyOn(console, 'log');
      (window as any).__DEPLOY_PLATFORM_DEBUG__ = false;

      vi.mocked(useEmbedMode).mockReturnValue(false);

      const { result } = renderHook(() => useLayout(), {
        wrapper: ({ children }: { children: ReactNode }) => (
          <LayoutProvider>{children}</LayoutProvider>
        )
      });

      result.current.setMode('content-only');

      await waitFor(() => {
        expect(result.current.mode).toBe('content-only');
      });

      expect(consoleSpy).not.toHaveBeenCalled();
    });
  });

  describe('Context Value Stability', () => {
    it('should memoize context value to prevent unnecessary re-renders', () => {
      vi.mocked(useEmbedMode).mockReturnValue(false);

      const { result, rerender } = renderHook(() => useLayout(), {
        wrapper: ({ children }: { children: ReactNode }) => (
          <LayoutProvider>{children}</LayoutProvider>
        )
      });

      const firstValue = result.current;

      // 强制重渲染
      rerender();

      // Context value应该保持稳定(引用相等)
      expect(result.current).toBe(firstValue);
    });

    it('should update context value when mode changes', async () => {
      vi.mocked(useEmbedMode).mockReturnValue(false);

      const { result } = renderHook(() => useLayout(), {
        wrapper: ({ children }: { children: ReactNode }) => (
          <LayoutProvider>{children}</LayoutProvider>
        )
      });

      const initialValue = result.current;

      result.current.setMode('content-only');

      await waitFor(() => {
        expect(result.current.mode).toBe('content-only');
      });

      // Context value应该已更新
      expect(result.current).not.toBe(initialValue);
    });
  });

  describe('Integration with useEmbedMode', () => {
    it('should react to embed mode changes', () => {
      // 第一次渲染: 非嵌入模式
      vi.mocked(useEmbedMode).mockReturnValue(false);

      const { result, unmount } = renderHook(() => useLayout(), {
        wrapper: ({ children }: { children: ReactNode }) => (
          <LayoutProvider>{children}</LayoutProvider>
        )
      });

      expect(result.current.mode).toBe('shell');
      expect(result.current.isEmbedded).toBe(false);

      unmount();

      // 第二次渲染: 嵌入模式
      vi.mocked(useEmbedMode).mockReturnValue(true);

      const { result: result2 } = renderHook(() => useLayout(), {
        wrapper: ({ children }: { children: ReactNode }) => (
          <LayoutProvider>{children}</LayoutProvider>
        )
      });

      expect(result2.current.mode).toBe('content-only');
      expect(result2.current.isEmbedded).toBe(true);
    });
  });

  describe('Children Rendering', () => {
    it('should render children components', () => {
      vi.mocked(useEmbedMode).mockReturnValue(false);

      const { getByText } = render(
        <LayoutProvider>
          <div>Test Child Component</div>
        </LayoutProvider>
      );

      expect(getByText('Test Child Component')).toBeInTheDocument();
    });

    it('should allow nested children to access layout context', () => {
      vi.mocked(useEmbedMode).mockReturnValue(false);

      function NestedComponent() {
        const { mode } = useLayout();
        return <div>Current Mode: {mode}</div>;
      }

      const { getByText } = render(
        <LayoutProvider>
          <NestedComponent />
        </LayoutProvider>
      );

      expect(getByText('Current Mode: shell')).toBeInTheDocument();
    });
  });
});
