import { renderHook } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { useEmbedMode } from '../../../react-app/hooks/useEmbedMode';

describe('useEmbedMode', () => {
  let container: HTMLElement | null = null;

  beforeEach(() => {
    // 清理DOM环境
    document.body.innerHTML = '';
    container = null;

    // 清理Window全局标志
    delete (window as any).__DEPLOY_PLATFORM_EMBEDDED__;
    delete (window as any).__DEPLOY_PLATFORM_DEBUG__;

    // 模拟console.log避免测试输出污染
    vi.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    if (container) {
      container.remove();
    }
    vi.restoreAllMocks();
  });

  describe('Signal 1: Container Element Detection', () => {
    it('should return false when container element does not exist', () => {
      const { result } = renderHook(() => useEmbedMode());
      expect(result.current).toBe(false);
    });

    it('should return false when container exists but has no embed signals', () => {
      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      document.body.appendChild(container);

      const { result } = renderHook(() => useEmbedMode());
      expect(result.current).toBe(false);
    });
  });

  describe('Signal 2: Spring Boot Context Flag (Primary Signal)', () => {
    it('should return true when data-spring-context="true"', () => {
      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      container.setAttribute('data-spring-context', 'true');
      document.body.appendChild(container);

      const { result } = renderHook(() => useEmbedMode());
      expect(result.current).toBe(true);
    });

    it('should return false when data-spring-context="false"', () => {
      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      container.setAttribute('data-spring-context', 'false');
      document.body.appendChild(container);

      const { result } = renderHook(() => useEmbedMode());
      expect(result.current).toBe(false);
    });
  });

  describe('Signal 3: Explicit Embed Flag (Enhanced Signal)', () => {
    it('should return true when data-embedded="true"', () => {
      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      container.setAttribute('data-embedded', 'true');
      document.body.appendChild(container);

      const { result } = renderHook(() => useEmbedMode());
      expect(result.current).toBe(true);
    });

    it('should return false when data-embedded="false"', () => {
      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      container.setAttribute('data-embedded', 'false');
      document.body.appendChild(container);

      const { result } = renderHook(() => useEmbedMode());
      expect(result.current).toBe(false);
    });
  });

  describe('Signal 4: Window Global Flag (Supplementary Signal)', () => {
    it('should return true when window.__DEPLOY_PLATFORM_EMBEDDED__ is true', () => {
      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      document.body.appendChild(container);

      (window as any).__DEPLOY_PLATFORM_EMBEDDED__ = true;

      const { result } = renderHook(() => useEmbedMode());
      expect(result.current).toBe(true);
    });

    it('should return false when window.__DEPLOY_PLATFORM_EMBEDDED__ is false', () => {
      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      document.body.appendChild(container);

      (window as any).__DEPLOY_PLATFORM_EMBEDDED__ = false;

      const { result } = renderHook(() => useEmbedMode());
      expect(result.current).toBe(false);
    });
  });

  describe('Multi-layer Validation Logic', () => {
    it('should return true when ANY signal is true (OR logic)', () => {
      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      container.setAttribute('data-spring-context', 'true');
      container.setAttribute('data-embedded', 'false');
      document.body.appendChild(container);

      (window as any).__DEPLOY_PLATFORM_EMBEDDED__ = false;

      const { result } = renderHook(() => useEmbedMode());
      expect(result.current).toBe(true); // data-spring-context is true
    });

    it('should return true when multiple signals are true', () => {
      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      container.setAttribute('data-spring-context', 'true');
      container.setAttribute('data-embedded', 'true');
      document.body.appendChild(container);

      (window as any).__DEPLOY_PLATFORM_EMBEDDED__ = true;

      const { result } = renderHook(() => useEmbedMode());
      expect(result.current).toBe(true);
    });

    it('should return false when all signals are false', () => {
      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      container.setAttribute('data-spring-context', 'false');
      container.setAttribute('data-embedded', 'false');
      document.body.appendChild(container);

      (window as any).__DEPLOY_PLATFORM_EMBEDDED__ = false;

      const { result } = renderHook(() => useEmbedMode());
      expect(result.current).toBe(false);
    });
  });

  describe('Debug Logging', () => {
    it('should log detection signals when __DEPLOY_PLATFORM_DEBUG__ is true', () => {
      const consoleSpy = vi.spyOn(console, 'log');

      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      container.setAttribute('data-spring-context', 'true');
      document.body.appendChild(container);

      (window as any).__DEPLOY_PLATFORM_DEBUG__ = true;

      renderHook(() => useEmbedMode());

      expect(consoleSpy).toHaveBeenCalledWith(
        '[useEmbedMode] Detection signals:',
        expect.objectContaining({
          hasSpringContext: true,
          hasEmbedFlag: false,
          hasWindowFlag: false,
          result: true
        })
      );
    });

    it('should NOT log when __DEPLOY_PLATFORM_DEBUG__ is false', () => {
      const consoleSpy = vi.spyOn(console, 'log');

      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      container.setAttribute('data-spring-context', 'true');
      document.body.appendChild(container);

      (window as any).__DEPLOY_PLATFORM_DEBUG__ = false;

      renderHook(() => useEmbedMode());

      expect(consoleSpy).not.toHaveBeenCalled();
    });
  });

  describe('Edge Cases', () => {
    it('should handle missing container gracefully', () => {
      expect(() => {
        renderHook(() => useEmbedMode());
      }).not.toThrow();
    });

    it('should handle undefined attribute values', () => {
      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      // 不设置任何属性
      document.body.appendChild(container);

      const { result } = renderHook(() => useEmbedMode());
      expect(result.current).toBe(false);
    });

    it('should handle partial attribute values', () => {
      container = document.createElement('div');
      container.id = 'deploy-platform-root';
      container.setAttribute('data-spring-context', ''); // 空字符串
      document.body.appendChild(container);

      const { result } = renderHook(() => useEmbedMode());
      expect(result.current).toBe(false); // 空字符串不等于"true"
    });
  });
});
