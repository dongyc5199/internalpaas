/**
 * Integration Test: React to Main App Navigation Update (T022)
 *
 * 目的: 测试React应用内部导航能够正确通知主应用更新侧边栏高亮状态
 *
 * 测试策略:
 * - 使用useNavSync hook的实际实现
 * - 模拟React路由变化(location changes)
 * - 验证react-nav-change事件被正确dispatch
 * - 测试事件payload和防循环机制
 */

import { renderHook, waitFor, act } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { useNavSync } from '../../../react-app/hooks/useNavSync';
import type { NavigateFunction, Location } from 'react-router-dom';

describe('Integration Test: React to Main App Navigation Update', () => {
  let navigate: NavigateFunction;
  let mockLocation: Location;
  let eventSpy: ReturnType<typeof vi.spyOn>;
  let capturedEvents: CustomEvent[];

  beforeEach(() => {
    // Mock navigate function
    navigate = vi.fn();

    // Mock initial location
    mockLocation = {
      pathname: '/overview',
      search: '',
      hash: '',
      state: null,
      key: 'default'
    } as Location;

    // Mock DOM environment
    const container = document.createElement('div');
    container.id = 'deploy-platform-root';
    container.setAttribute('data-spring-context', 'true');
    container.setAttribute('data-embedded', 'true');
    document.body.appendChild(container);

    // Spy on window.dispatchEvent to capture react-nav-change events
    capturedEvents = [];
    eventSpy = vi.spyOn(window, 'dispatchEvent').mockImplementation((event: Event) => {
      if (event instanceof CustomEvent && event.type === 'react-nav-change') {
        capturedEvents.push(event);
      }
      // Call original for other events
      return true;
    });
  });

  afterEach(() => {
    // Clean up DOM
    const container = document.getElementById('deploy-platform-root');
    if (container) {
      document.body.removeChild(container);
    }

    eventSpy.mockRestore();
    capturedEvents = [];
    vi.restoreAllMocks();
  });

  describe('Event Dispatching on Route Change', () => {
    it('should dispatch react-nav-change event when navigating to /releases', async () => {
      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      // Change location to /releases
      const newLocation: Location = {
        ...mockLocation,
        pathname: '/releases',
        key: 'new-key-1'
      };

      act(() => {
        rerender({ location: newLocation });
      });

      await waitFor(() => {
        expect(capturedEvents.length).toBeGreaterThan(0);
      });

      const event = capturedEvents.find(e => e.detail?.route === '/releases');
      expect(event).toBeDefined();
      expect(event!.detail.source).toBe('react');
      expect(event!.detail.timestamp).toBeDefined();
      expect(typeof event!.detail.timestamp).toBe('number');
    });

    it('should dispatch react-nav-change event when navigating to /settings/policies', async () => {
      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      // Change location to /settings/policies
      const newLocation: Location = {
        ...mockLocation,
        pathname: '/settings/policies',
        key: 'new-key-2'
      };

      act(() => {
        rerender({ location: newLocation });
      });

      await waitFor(() => {
        expect(capturedEvents.length).toBeGreaterThan(0);
      });

      const event = capturedEvents.find(e => e.detail?.route === '/settings/policies');
      expect(event).toBeDefined();
      expect(event!.detail.source).toBe('react');
    });

    it('should not dispatch event when only search params change', async () => {
      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      // Change only search params, not pathname
      const newLocation: Location = {
        ...mockLocation,
        search: '?tab=details',
        key: 'new-key-3'
      };

      act(() => {
        rerender({ location: newLocation });
      });

      await new Promise(resolve => setTimeout(resolve, 200));

      // Should not have dispatched any events
      expect(capturedEvents).toHaveLength(0);
    });
  });

  describe('Event Payload Validation', () => {
    it('should include all required fields in event detail', async () => {
      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      const newLocation: Location = {
        ...mockLocation,
        pathname: '/releases',
        key: 'new-key-4'
      };

      act(() => {
        rerender({ location: newLocation });
      });

      await waitFor(() => {
        expect(capturedEvents.length).toBeGreaterThan(0);
      });

      const event = capturedEvents[0];
      expect(event.detail).toHaveProperty('route');
      expect(event.detail).toHaveProperty('source');
      expect(event.detail).toHaveProperty('timestamp');
      expect(event.detail.route).toBe('/releases');
      expect(event.detail.source).toBe('react');
    });

    it('should have correct event type', async () => {
      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      const newLocation: Location = {
        ...mockLocation,
        pathname: '/releases',
        key: 'new-key-5'
      };

      act(() => {
        rerender({ location: newLocation });
      });

      await waitFor(() => {
        expect(capturedEvents.length).toBeGreaterThan(0);
        expect(capturedEvents[0].type).toBe('react-nav-change');
      });
    });

    it('should have timestamp within reasonable range', async () => {
      const beforeTime = Date.now();

      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      const newLocation: Location = {
        ...mockLocation,
        pathname: '/releases',
        key: 'new-key-6'
      };

      act(() => {
        rerender({ location: newLocation });
      });

      await waitFor(() => {
        expect(capturedEvents.length).toBeGreaterThan(0);
      });

      const afterTime = Date.now();
      const event = capturedEvents[0];

      expect(event.detail.timestamp).toBeGreaterThanOrEqual(beforeTime);
      expect(event.detail.timestamp).toBeLessThanOrEqual(afterTime);
    });
  });

  describe('Sequential Route Changes', () => {
    it('should dispatch separate events for each route change', async () => {
      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      // First navigation
      const location1: Location = {
        ...mockLocation,
        pathname: '/releases',
        key: 'key-1'
      };

      act(() => {
        rerender({ location: location1 });
      });

      await waitFor(() => {
        expect(capturedEvents.length).toBeGreaterThanOrEqual(1);
      });

      // Second navigation
      const location2: Location = {
        ...mockLocation,
        pathname: '/settings/policies',
        key: 'key-2'
      };

      act(() => {
        rerender({ location: location2 });
      });

      await waitFor(() => {
        expect(capturedEvents.length).toBeGreaterThanOrEqual(2);
      });

      // Third navigation
      const location3: Location = {
        ...mockLocation,
        pathname: '/overview',
        key: 'key-3'
      };

      act(() => {
        rerender({ location: location3 });
      });

      await waitFor(() => {
        expect(capturedEvents.length).toBeGreaterThanOrEqual(3);
      });

      // Verify each route was dispatched
      const routes = capturedEvents.map(e => e.detail?.route);
      expect(routes).toContain('/releases');
      expect(routes).toContain('/settings/policies');
      expect(routes).toContain('/overview');
    });

    it('should dispatch events with increasing timestamps', async () => {
      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      // Navigate to releases
      const location1: Location = {
        ...mockLocation,
        pathname: '/releases',
        key: 'key-1'
      };

      act(() => {
        rerender({ location: location1 });
      });

      await waitFor(() => {
        expect(capturedEvents.length).toBeGreaterThanOrEqual(1);
      });

      // Small delay to ensure different timestamp
      await new Promise(resolve => setTimeout(resolve, 10));

      // Navigate to policies
      const location2: Location = {
        ...mockLocation,
        pathname: '/settings/policies',
        key: 'key-2'
      };

      act(() => {
        rerender({ location: location2 });
      });

      await waitFor(() => {
        expect(capturedEvents.length).toBeGreaterThanOrEqual(2);
      });

      // Verify timestamps are increasing
      const timestamps = capturedEvents.map(e => e.detail?.timestamp);
      for (let i = 1; i < timestamps.length; i++) {
        expect(timestamps[i]).toBeGreaterThanOrEqual(timestamps[i - 1]);
      }
    });
  });

  describe('Anti-Loop Mechanism', () => {
    it('should not dispatch event when navigation is triggered by main app', async () => {
      renderHook(() => useNavSync(navigate, mockLocation));

      // Clear any initial events
      capturedEvents = [];

      // Simulate main app navigation (this sets isSyncing=true)
      // First dispatch main-nav-change without the spy
      eventSpy.mockRestore();

      const mainNavEvent = new CustomEvent('main-nav-change', {
        detail: {
          route: '/releases',
          source: 'main-app',
          timestamp: Date.now()
        }
      });

      act(() => {
        window.dispatchEvent(mainNavEvent);
      });

      await waitFor(() => {
        expect(navigate).toHaveBeenCalledWith('/releases');
      });

      // Re-attach spy to capture react-nav-change events
      eventSpy = vi.spyOn(window, 'dispatchEvent').mockImplementation((event: Event) => {
        if (event instanceof CustomEvent && event.type === 'react-nav-change') {
          capturedEvents.push(event);
        }
        return true;
      });

      // Wait a bit
      await new Promise(resolve => setTimeout(resolve, 200));

      // Should NOT have dispatched react-nav-change (because isSyncing=true)
      const reactNavEvents = capturedEvents.filter(e => e.detail?.route === '/releases');
      expect(reactNavEvents).toHaveLength(0);
    });

    it('should dispatch event after sync timeout expires', async () => {
      vi.useFakeTimers();

      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      // Simulate main app navigation first
      eventSpy.mockRestore();

      const mainNavEvent = new CustomEvent('main-nav-change', {
        detail: {
          route: '/releases',
          source: 'main-app',
          timestamp: Date.now()
        }
      });

      act(() => {
        window.dispatchEvent(mainNavEvent);
      });

      await waitFor(() => {
        expect(navigate).toHaveBeenCalledWith('/releases');
      });

      // Advance time past sync timeout (300ms)
      act(() => {
        vi.advanceTimersByTime(350);
      });

      // Re-attach spy after timeout
      capturedEvents = [];
      eventSpy = vi.spyOn(window, 'dispatchEvent').mockImplementation((event: Event) => {
        if (event instanceof CustomEvent && event.type === 'react-nav-change') {
          capturedEvents.push(event);
        }
        return true;
      });

      // Now navigate via React (should dispatch event)
      const newLocation: Location = {
        ...mockLocation,
        pathname: '/overview',
        key: 'new-key'
      };

      act(() => {
        rerender({ location: newLocation });
      });

      // Should have dispatched react-nav-change now
      await waitFor(() => {
        expect(capturedEvents.length).toBeGreaterThan(0);
      });

      const event = capturedEvents.find(e => e.detail?.route === '/overview');
      expect(event).toBeDefined();

      vi.useRealTimers();
    });
  });

  describe('Main App Event Handler Simulation', () => {
    it('should allow main app to receive and process react-nav-change event', async () => {
      const mainAppHandlerSpy = vi.fn();

      // Simulate main app's event listener
      const mainAppHandler = (event: Event): void => {
        if (event instanceof CustomEvent && event.type === 'react-nav-change') {
          mainAppHandlerSpy(event.detail);

          // Simulate sidebar update logic
          const { route } = event.detail;
          const menuId = getMenuIdFromRoute(route);
          if (menuId) {
            updateSidebarHighlight(menuId);
          }
        }
      };

      // Helper function to map route to menu ID (like main app does)
      function getMenuIdFromRoute(route: string): string | null {
        const reverseMap: Record<string, string> = {
          '/overview': 'deploy-platform/overview',
          '/releases': 'deploy-platform/releases',
          '/settings/policies': 'deploy-platform/policies'
        };
        return reverseMap[route] || null;
      }

      // Mock sidebar update function
      const updateSidebarHighlight = vi.fn();

      // Restore original dispatchEvent and add our handler
      eventSpy.mockRestore();
      window.addEventListener('react-nav-change', mainAppHandler as EventListener);

      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      // Navigate to releases
      const newLocation: Location = {
        ...mockLocation,
        pathname: '/releases',
        key: 'new-key'
      };

      act(() => {
        rerender({ location: newLocation });
      });

      // Main app handler should have been called
      await waitFor(() => {
        expect(mainAppHandlerSpy).toHaveBeenCalled();
      });

      // Verify the event detail
      expect(mainAppHandlerSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          route: '/releases',
          source: 'react',
          timestamp: expect.any(Number)
        })
      );

      // Sidebar highlight should have been updated
      expect(updateSidebarHighlight).toHaveBeenCalledWith('deploy-platform/releases');

      // Clean up
      window.removeEventListener('react-nav-change', mainAppHandler as EventListener);
    });

    it('should handle all route mappings correctly', async () => {
      const updateSidebarHighlight = vi.fn();

      const mainAppHandler = (event: Event): void => {
        if (event instanceof CustomEvent && event.type === 'react-nav-change') {
          const reverseMap: Record<string, string> = {
            '/overview': 'deploy-platform/overview',
            '/releases': 'deploy-platform/releases',
            '/settings/policies': 'deploy-platform/policies'
          };
          const menuId = reverseMap[event.detail.route];
          if (menuId) {
            updateSidebarHighlight(menuId);
          }
        }
      };

      eventSpy.mockRestore();
      window.addEventListener('react-nav-change', mainAppHandler as EventListener);

      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      // Test all three routes
      const routes = [
        { path: '/releases', menuId: 'deploy-platform/releases' },
        { path: '/settings/policies', menuId: 'deploy-platform/policies' },
        { path: '/overview', menuId: 'deploy-platform/overview' }
      ];

      for (const { path, menuId } of routes) {
        const newLocation: Location = {
          ...mockLocation,
          pathname: path,
          key: `key-${path}`
        };

        act(() => {
          rerender({ location: newLocation });
        });

        await waitFor(() => {
          expect(updateSidebarHighlight).toHaveBeenCalledWith(menuId);
        });
      }

      window.removeEventListener('react-nav-change', mainAppHandler as EventListener);
    });
  });

  describe('Debug Logging', () => {
    it('should log events when debug flag is enabled', async () => {
      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
      (window as any).__DEPLOY_PLATFORM_DEBUG__ = true;

      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      const newLocation: Location = {
        ...mockLocation,
        pathname: '/releases',
        key: 'new-key'
      };

      act(() => {
        rerender({ location: newLocation });
      });

      await waitFor(() => {
        const debugCalls = consoleSpy.mock.calls.filter(
          call => typeof call[0] === 'string' && call[0].includes('[useNavSync]')
        );
        expect(debugCalls.length).toBeGreaterThan(0);
      });

      delete (window as any).__DEPLOY_PLATFORM_DEBUG__;
      consoleSpy.mockRestore();
    });

    it('should not log when debug flag is disabled', async () => {
      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
      (window as any).__DEPLOY_PLATFORM_DEBUG__ = false;

      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      const newLocation: Location = {
        ...mockLocation,
        pathname: '/releases',
        key: 'new-key'
      };

      act(() => {
        rerender({ location: newLocation });
      });

      await waitFor(() => {
        expect(capturedEvents.length).toBeGreaterThan(0);
      });

      const debugCalls = consoleSpy.mock.calls.filter(
        call => typeof call[0] === 'string' && call[0].includes('[useNavSync]')
      );
      expect(debugCalls).toHaveLength(0);

      delete (window as any).__DEPLOY_PLATFORM_DEBUG__;
      consoleSpy.mockRestore();
    });
  });
});
