/**
 * Tests for useTokenRefresh Hook
 *
 * Test Coverage:
 * 1. Initialization and cleanup
 * 2. BroadcastChannel vs localStorage fallback detection
 * 3. Token scheduling logic (via unit tests of internal behavior)
 * 4. Error handling
 *
 * Note: Full integration tests with timers are complex due to React hook lifecycle.
 * These tests focus on verifying the hook initializes correctly and responds to external events.
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useTokenRefresh } from '../../react-app/hooks/useTokenRefresh';
import { useAuthStore } from '../../react-app/stores/authStore';
import type { Token } from '../../react-app/types/auth';

// Mock dependencies
vi.mock('../../react-app/api/tokenApi');
vi.mock('../../react-app/stores/authStore');

// Mock BroadcastChannel
class MockBroadcastChannel {
  name: string;
  listeners: Array<(event: MessageEvent) => void> = [];

  constructor(name: string) {
    this.name = name;
  }

  postMessage(data: unknown): void {
    // Simulate message broadcast
    const event = new MessageEvent('message', { data });
    this.listeners.forEach(listener => listener(event));
  }

  addEventListener(type: string, listener: (event: MessageEvent) => void): void {
    if (type === 'message') {
      this.listeners.push(listener);
    }
  }

  removeEventListener(type: string, listener: (event: MessageEvent) => void): void {
    if (type === 'message') {
      const index = this.listeners.indexOf(listener);
      if (index > -1) {
        this.listeners.splice(index, 1);
      }
    }
  }

  close(): void {
    this.listeners = [];
  }
}

describe('useTokenRefresh', () => {
  let mockSetToken: ReturnType<typeof vi.fn>;
  let mockClearAuth: ReturnType<typeof vi.fn>;
  let originalBroadcastChannel: typeof BroadcastChannel | undefined;

  const createToken = (expiresIn: number): Token => ({
    accessToken: 'test-token',
    expiresAt: Date.now() + expiresIn,
    issuer: 'test-issuer',
    audience: 'test-audience',
  });

  beforeEach(() => {
    vi.clearAllMocks();

    mockSetToken = vi.fn();
    mockClearAuth = vi.fn();

    vi.mocked(useAuthStore).mockReturnValue({
      token: null,
      user: null,
      isAuthenticated: false,
      setToken: mockSetToken,
      setUser: vi.fn(),
      clearAuth: mockClearAuth,
      login: vi.fn(),
    });

    // Mock BroadcastChannel
    originalBroadcastChannel = global.BroadcastChannel;
    global.BroadcastChannel = MockBroadcastChannel as any;

    // Mock localStorage
    Object.defineProperty(window, 'localStorage', {
      value: {
        getItem: vi.fn(),
        setItem: vi.fn(),
        removeItem: vi.fn(),
        clear: vi.fn(),
      },
      writable: true,
      configurable: true,
    });

    // Mock console
    vi.spyOn(console, 'log').mockImplementation(() => {});
    vi.spyOn(console, 'warn').mockImplementation(() => {});
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
    if (originalBroadcastChannel !== undefined) {
      global.BroadcastChannel = originalBroadcastChannel;
    }
  });

  describe('Initialization', () => {
    it('应该在没有token时不报错', () => {
      vi.mocked(useAuthStore).mockReturnValue({
        token: null,
        user: null,
        isAuthenticated: false,
        setToken: mockSetToken,
        setUser: vi.fn(),
        clearAuth: mockClearAuth,
        login: vi.fn(),
      });

      expect(() => renderHook(() => useTokenRefresh())).not.toThrow();
    });

    it('应该在有token时初始化并调度刷新', () => {
      const token = createToken(10 * 60 * 1000); // 10 minutes

      vi.mocked(useAuthStore).mockReturnValue({
        token,
        user: null,
        isAuthenticated: true,
        setToken: mockSetToken,
        setUser: vi.fn(),
        clearAuth: mockClearAuth,
        login: vi.fn(),
      });

      renderHook(() => useTokenRefresh());

      // Should log scheduling message
      expect(console.log).toHaveBeenCalledWith(
        '[useTokenRefresh] Scheduling refresh',
        expect.any(Object)
      );
    });

    it('应该在BroadcastChannel可用时使用它', () => {
      const token = createToken(10 * 60 * 1000);

      vi.mocked(useAuthStore).mockReturnValue({
        token,
        user: null,
        isAuthenticated: true,
        setToken: mockSetToken,
        setUser: vi.fn(),
        clearAuth: mockClearAuth,
        login: vi.fn(),
      });

      renderHook(() => useTokenRefresh());

      expect(console.log).toHaveBeenCalledWith('[useTokenRefresh] BroadcastChannel initialized');
    });
  });

  describe('BroadcastChannel Support Detection', () => {
    it('应该在BroadcastChannel不可用时使用localStorage', () => {
      // Remove BroadcastChannel
      delete (global as any).BroadcastChannel;

      const token = createToken(10 * 60 * 1000);

      vi.mocked(useAuthStore).mockReturnValue({
        token,
        user: null,
        isAuthenticated: true,
        setToken: mockSetToken,
        setUser: vi.fn(),
        clearAuth: mockClearAuth,
        login: vi.fn(),
      });

      renderHook(() => useTokenRefresh());

      expect(console.log).toHaveBeenCalledWith(
        '[useTokenRefresh] Using localStorage fallback (BroadcastChannel not supported)'
      );

      // Restore
      if (originalBroadcastChannel !== undefined) {
        global.BroadcastChannel = originalBroadcastChannel;
      }
    });
  });

  describe('Cleanup', () => {
    it('应该在卸载时清理资源', () => {
      const token = createToken(10 * 60 * 1000);

      vi.mocked(useAuthStore).mockReturnValue({
        token,
        user: null,
        isAuthenticated: true,
        setToken: mockSetToken,
        setUser: vi.fn(),
        clearAuth: mockClearAuth,
        login: vi.fn(),
      });

      const closeSpy = vi.spyOn(MockBroadcastChannel.prototype, 'close');

      const { unmount } = renderHook(() => useTokenRefresh());

      unmount();

      // Should close BroadcastChannel
      expect(closeSpy).toHaveBeenCalled();
    });

    it('应该在BroadcastChannel不可用时移除storage监听器', () => {
      // Remove BroadcastChannel
      delete (global as any).BroadcastChannel;

      const token = createToken(10 * 60 * 1000);

      vi.mocked(useAuthStore).mockReturnValue({
        token,
        user: null,
        isAuthenticated: true,
        setToken: mockSetToken,
        setUser: vi.fn(),
        clearAuth: mockClearAuth,
        login: vi.fn(),
      });

      const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener');

      const { unmount } = renderHook(() => useTokenRefresh());

      unmount();

      expect(removeEventListenerSpy).toHaveBeenCalledWith('storage', expect.any(Function));

      // Restore
      if (originalBroadcastChannel !== undefined) {
        global.BroadcastChannel = originalBroadcastChannel;
      }
    });
  });

  describe('Token Expiry Detection', () => {
    it('应该在token即将过期时立即触发刷新警告', () => {
      const token = createToken(2 * 60 * 1000); // 2 minutes (less than 5min refresh threshold)

      vi.mocked(useAuthStore).mockReturnValue({
        token,
        user: null,
        isAuthenticated: true,
        setToken: mockSetToken,
        setUser: vi.fn(),
        clearAuth: mockClearAuth,
        login: vi.fn(),
      });

      renderHook(() => useTokenRefresh());

      // Should warn about immediate refresh
      expect(console.warn).toHaveBeenCalledWith(
        '[useTokenRefresh] Token expires soon, refreshing immediately'
      );
    });

    it('应该在token有足够时间时正常调度', () => {
      const token = createToken(10 * 60 * 1000); // 10 minutes

      vi.mocked(useAuthStore).mockReturnValue({
        token,
        user: null,
        isAuthenticated: true,
        setToken: mockSetToken,
        setUser: vi.fn(),
        clearAuth: mockClearAuth,
        login: vi.fn(),
      });

      renderHook(() => useTokenRefresh());

      // Should schedule, not warn
      expect(console.warn).not.toHaveBeenCalled();
      expect(console.log).toHaveBeenCalledWith(
        '[useTokenRefresh] Scheduling refresh',
        expect.objectContaining({
          refreshIn: expect.stringContaining('s'),
        })
      );
    });
  });

  describe('Re-renders and Token Changes', () => {
    it('应该在token变化时重新调度', () => {
      const initialToken = createToken(10 * 60 * 1000);

      const { rerender } = renderHook(
        ({ token }) => {
          vi.mocked(useAuthStore).mockReturnValue({
            token,
            user: null,
            isAuthenticated: !!token,
            setToken: mockSetToken,
            setUser: vi.fn(),
            clearAuth: mockClearAuth,
            login: vi.fn(),
          });
          useTokenRefresh();
        },
        { initialProps: { token: initialToken } }
      );

      const callCountAfterFirst = (console.log as any).mock.calls.filter((call: any) =>
        call[0].includes('Scheduling refresh')
      ).length;

      // Change token
      const newToken = createToken(15 * 60 * 1000);
      rerender({ token: newToken });

      const callCountAfterSecond = (console.log as any).mock.calls.filter((call: any) =>
        call[0].includes('Scheduling refresh')
      ).length;

      // Should have scheduled again
      expect(callCountAfterSecond).toBeGreaterThan(callCountAfterFirst);
    });
  });

  describe('Edge Cases', () => {
    it('应该处理token为null的情况', () => {
      vi.mocked(useAuthStore).mockReturnValue({
        token: null,
        user: null,
        isAuthenticated: false,
        setToken: mockSetToken,
        setUser: vi.fn(),
        clearAuth: mockClearAuth,
        login: vi.fn(),
      });

      expect(() => renderHook(() => useTokenRefresh())).not.toThrow();

      // Should not schedule
      expect(console.log).not.toHaveBeenCalledWith(
        '[useTokenRefresh] Scheduling refresh',
        expect.any(Object)
      );
    });

    it('应该处理token对象格式正确但expiresAt为负数', () => {
      const invalidToken: Token = {
        accessToken: 'test',
        expiresAt: -1000, // Negative expiry
        issuer: 'test',
        audience: 'test',
      };

      vi.mocked(useAuthStore).mockReturnValue({
        token: invalidToken,
        user: null,
        isAuthenticated: true,
        setToken: mockSetToken,
        setUser: vi.fn(),
        clearAuth: mockClearAuth,
        login: vi.fn(),
      });

      renderHook(() => useTokenRefresh());

      // Should trigger immediate refresh warning
      expect(console.warn).toHaveBeenCalled();
    });
  });
});
