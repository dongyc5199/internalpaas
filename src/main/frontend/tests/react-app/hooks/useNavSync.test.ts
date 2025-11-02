import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { useNavSync } from '../../../react-app/hooks/useNavSync';
import type { NavigateFunction } from 'react-router-dom';
import type { Location } from 'react-router-dom';

describe('useNavSync', () => {
  let navigate: NavigateFunction;
  let mockLocation: Location;
  let eventListeners: Map<string, EventListener[]>;

  beforeEach(() => {
    // Mock navigate function
    navigate = vi.fn();

    // Mock location object
    mockLocation = {
      pathname: '/overview',
      search: '',
      hash: '',
      state: null,
      key: 'default'
    } as Location;

    // Track event listeners
    eventListeners = new Map();

    // Mock addEventListener
    const originalAddEventListener = window.addEventListener;
    window.addEventListener = vi.fn((event: string, handler: any) => {
      if (!eventListeners.has(event)) {
        eventListeners.set(event, []);
      }
      eventListeners.get(event)!.push(handler);
      originalAddEventListener.call(window, event, handler);
    });

    // Mock removeEventListener
    const originalRemoveEventListener = window.removeEventListener;
    window.removeEventListener = vi.fn((event: string, handler: any) => {
      const handlers = eventListeners.get(event);
      if (handlers) {
        const index = handlers.indexOf(handler);
        if (index > -1) {
          handlers.splice(index, 1);
        }
      }
      originalRemoveEventListener.call(window, event, handler);
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
    eventListeners.clear();
  });

  describe('Event Listening', () => {
    it('should register main-nav-change event listener on mount', () => {
      renderHook(() => useNavSync(navigate, mockLocation));

      expect(window.addEventListener).toHaveBeenCalledWith(
        'main-nav-change',
        expect.any(Function)
      );
    });

    it('should clean up event listener on unmount', () => {
      const { unmount } = renderHook(() => useNavSync(navigate, mockLocation));

      const handler = eventListeners.get('main-nav-change')?.[0];
      expect(handler).toBeDefined();

      unmount();

      expect(window.removeEventListener).toHaveBeenCalledWith(
        'main-nav-change',
        handler
      );
    });

    it('should handle main-nav-change event and navigate', async () => {
      renderHook(() => useNavSync(navigate, mockLocation));

      const event = new CustomEvent('main-nav-change', {
        detail: {
          route: '/releases',
          source: 'main-app',
          timestamp: Date.now()
        }
      });

      window.dispatchEvent(event);

      await waitFor(() => {
        expect(navigate).toHaveBeenCalledWith('/releases');
      });
    });

    it('should not navigate if route is the same as current location', async () => {
      renderHook(() => useNavSync(navigate, mockLocation));

      const event = new CustomEvent('main-nav-change', {
        detail: {
          route: '/overview', // Same as mockLocation.pathname
          source: 'main-app'
        }
      });

      window.dispatchEvent(event);

      await waitFor(() => {
        expect(navigate).not.toHaveBeenCalled();
      });
    });
  });

  describe('Event Dispatching', () => {
    it('should dispatch react-nav-change event when location changes', () => {
      const dispatchSpy = vi.spyOn(window, 'dispatchEvent');

      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      // Change location
      const newLocation: Location = {
        ...mockLocation,
        pathname: '/releases'
      };

      rerender({ location: newLocation });

      expect(dispatchSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'react-nav-change',
          detail: expect.objectContaining({
            route: '/releases',
            source: 'react'
          })
        })
      );
    });

    it('should not dispatch event if pathname has not changed', () => {
      const dispatchSpy = vi.spyOn(window, 'dispatchEvent');

      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      // Change search but not pathname
      const newLocation: Location = {
        ...mockLocation,
        search: '?tab=details'
      };

      rerender({ location: newLocation });

      expect(dispatchSpy).not.toHaveBeenCalled();
    });

    it('should include timestamp in dispatched event', () => {
      const dispatchSpy = vi.spyOn(window, 'dispatchEvent');
      const beforeTime = Date.now();

      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      const newLocation: Location = {
        ...mockLocation,
        pathname: '/settings/policies'
      };

      rerender({ location: newLocation });

      const afterTime = Date.now();

      expect(dispatchSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          detail: expect.objectContaining({
            timestamp: expect.any(Number)
          })
        })
      );

      const call = dispatchSpy.mock.calls[0][0] as CustomEvent;
      const timestamp = call.detail.timestamp;

      expect(timestamp).toBeGreaterThanOrEqual(beforeTime);
      expect(timestamp).toBeLessThanOrEqual(afterTime);
    });
  });

  describe('Anti-Loop Mechanism', () => {
    it('should prevent navigation loop using isSyncing flag', async () => {
      renderHook(() => useNavSync(navigate, mockLocation));

      // Simulate main app navigation
      const event1 = new CustomEvent('main-nav-change', {
        detail: {
          route: '/releases',
          source: 'main-app',
          timestamp: Date.now()
        }
      });

      window.dispatchEvent(event1);

      await waitFor(() => {
        expect(navigate).toHaveBeenCalledTimes(1);
      });

      // Immediately dispatch another event (should be ignored due to isSyncing)
      const event2 = new CustomEvent('main-nav-change', {
        detail: {
          route: '/settings/policies',
          source: 'main-app',
          timestamp: Date.now()
        }
      });

      window.dispatchEvent(event2);

      // Navigate should still be called only once
      expect(navigate).toHaveBeenCalledTimes(1);
    });

    it('should reset isSyncing flag after timeout', () => {
      vi.useFakeTimers();

      renderHook(() => useNavSync(navigate, mockLocation));

      // First navigation
      const event1 = new CustomEvent('main-nav-change', {
        detail: {
          route: '/releases',
          source: 'main-app'
        }
      });

      window.dispatchEvent(event1);

      // Navigate should be called once
      expect(navigate).toHaveBeenCalledTimes(1);

      // Fast-forward past sync timeout (300ms)
      vi.advanceTimersByTime(350);

      // Second navigation (should work now)
      const event2 = new CustomEvent('main-nav-change', {
        detail: {
          route: '/settings/policies',
          source: 'main-app'
        }
      });

      window.dispatchEvent(event2);

      // Navigate should be called a second time after timeout reset
      expect(navigate).toHaveBeenCalledTimes(2);

      vi.useRealTimers();
    });

    it('should not dispatch react-nav-change during incoming sync', () => {
      const dispatchSpy = vi.spyOn(window, 'dispatchEvent');

      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      // Simulate incoming navigation from main app
      const mainNavEvent = new CustomEvent('main-nav-change', {
        detail: {
          route: '/releases',
          source: 'main-app'
        }
      });

      window.dispatchEvent(mainNavEvent);

      // Change location (as a result of navigate() call)
      const newLocation: Location = {
        ...mockLocation,
        pathname: '/releases'
      };

      rerender({ location: newLocation });

      // Should not dispatch react-nav-change (isSyncing=true)
      const reactNavCalls = dispatchSpy.mock.calls.filter(
        (call) => (call[0] as CustomEvent).type === 'react-nav-change'
      );

      expect(reactNavCalls).toHaveLength(0);
    });
  });

  describe('Edge Cases', () => {
    it('should handle malformed event detail gracefully', () => {
      renderHook(() => useNavSync(navigate, mockLocation));

      const badEvent = new CustomEvent('main-nav-change', {
        detail: null // Invalid detail
      });

      expect(() => {
        window.dispatchEvent(badEvent);
      }).not.toThrow();

      expect(navigate).not.toHaveBeenCalled();
    });

    it('should handle missing route in event detail', () => {
      renderHook(() => useNavSync(navigate, mockLocation));

      const event = new CustomEvent('main-nav-change', {
        detail: {
          source: 'main-app'
          // route is missing
        }
      });

      expect(() => {
        window.dispatchEvent(event);
      }).not.toThrow();

      expect(navigate).not.toHaveBeenCalled();
    });

    it('should handle navigate errors gracefully', () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      const errorNavigate = vi.fn(() => {
        throw new Error('Navigation failed');
      });

      renderHook(() => useNavSync(errorNavigate, mockLocation));

      const event = new CustomEvent('main-nav-change', {
        detail: {
          route: '/releases',
          source: 'main-app'
        }
      });

      window.dispatchEvent(event);

      // Navigate should have been called and error logged
      expect(errorNavigate).toHaveBeenCalled();
      expect(consoleErrorSpy).toHaveBeenCalled();

      consoleErrorSpy.mockRestore();
    });

    it('should handle rapid location changes', () => {
      const dispatchSpy = vi.spyOn(window, 'dispatchEvent');

      const { rerender } = renderHook(
        ({ location }) => useNavSync(navigate, location),
        {
          initialProps: { location: mockLocation }
        }
      );

      // Rapid changes
      const locations = ['/releases', '/settings/policies', '/overview'];

      locations.forEach((pathname) => {
        rerender({
          location: { ...mockLocation, pathname }
        });
      });

      // Should dispatch event for each distinct pathname
      const reactNavCalls = dispatchSpy.mock.calls.filter(
        (call) => (call[0] as CustomEvent).type === 'react-nav-change'
      );

      expect(reactNavCalls.length).toBeGreaterThan(0);
    });
  });

  describe('Debug Logging', () => {
    it('should log navigation events when __DEPLOY_PLATFORM_DEBUG__ is true', () => {
      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
      (window as any).__DEPLOY_PLATFORM_DEBUG__ = true;

      renderHook(() => useNavSync(navigate, mockLocation));

      const event = new CustomEvent('main-nav-change', {
        detail: {
          route: '/releases',
          source: 'main-app'
        }
      });

      window.dispatchEvent(event);

      // Should have logged the event
      expect(consoleSpy).toHaveBeenCalled();
      const calls = consoleSpy.mock.calls.filter(
        (call) => typeof call[0] === 'string' && call[0].includes('[useNavSync]')
      );
      expect(calls.length).toBeGreaterThan(0);

      delete (window as any).__DEPLOY_PLATFORM_DEBUG__;
      consoleSpy.mockRestore();
    });

    it('should not log when __DEPLOY_PLATFORM_DEBUG__ is false', () => {
      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
      (window as any).__DEPLOY_PLATFORM_DEBUG__ = false;

      renderHook(() => useNavSync(navigate, mockLocation));

      const event = new CustomEvent('main-nav-change', {
        detail: {
          route: '/releases',
          source: 'main-app'
        }
      });

      window.dispatchEvent(event);

      // Navigate should have been called
      expect(navigate).toHaveBeenCalled();

      // Should not have any debug logs
      const debugCalls = consoleSpy.mock.calls.filter(
        (call) => typeof call[0] === 'string' && call[0].includes('[useNavSync]')
      );
      expect(debugCalls).toHaveLength(0);

      delete (window as any).__DEPLOY_PLATFORM_DEBUG__;
      consoleSpy.mockRestore();
    });
  });
});
