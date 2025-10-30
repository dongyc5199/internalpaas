/**
 * useTokenRefresh Hook
 *
 * Automatic token refresh mechanism that:
 * 1. Schedules refresh 5 minutes before token expires
 * 2. Synchronizes token across browser tabs using BroadcastChannel
 * 3. Falls back to localStorage events for Safari 15.4-
 */

import { useEffect, useRef, useCallback } from 'react';
import { useAuthStore } from '../stores/authStore';
import { refreshToken } from '../api/tokenApi';
import { TOKEN_REFRESH_BEFORE_MS, AUTH_CHANNEL_NAME } from '../config/constants';
import type { Token } from '../types/auth';

// Check if BroadcastChannel is supported (Safari 15.4+, Chrome 54+, Firefox 38+)
const HAS_BROADCAST_CHANNEL = typeof BroadcastChannel !== 'undefined';

/**
 * Message sent via BroadcastChannel when token is refreshed
 */
interface TokenRefreshMessage {
  type: 'TOKEN_REFRESHED';
  payload: Token;
}

/**
 * Hook to automatically refresh tokens before expiration
 *
 * @example
 * ```tsx
 * function App() {
 *   useTokenRefresh(); // Just call it once in your root component
 *   return <YourApp />;
 * }
 * ```
 */
export function useTokenRefresh(): void {
  const { token, setToken, clearAuth } = useAuthStore();
  const refreshTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const channelRef = useRef<BroadcastChannel | null>(null);

  /**
   * Schedule token refresh
   * Calculates time until token expires and sets timeout to refresh 5min before
   */
  const scheduleRefresh = useCallback((currentToken: Token) => {
    // Clear existing timeout
    if (refreshTimeoutRef.current) {
      clearTimeout(refreshTimeoutRef.current);
      refreshTimeoutRef.current = null;
    }

    const now = Date.now();
    const expiresAt = currentToken.expiresAt;
    const timeUntilExpiry = expiresAt - now;

    // Calculate when to trigger refresh (5min before expiry)
    const refreshTime = timeUntilExpiry - TOKEN_REFRESH_BEFORE_MS;

    console.log('[useTokenRefresh] Scheduling refresh', {
      expiresAt: new Date(expiresAt).toISOString(),
      timeUntilExpiry: `${Math.round(timeUntilExpiry / 1000)}s`,
      refreshIn: `${Math.round(refreshTime / 1000)}s`,
    });

    // If token expires in less than TOKEN_REFRESH_BEFORE_MS, refresh immediately
    if (refreshTime <= 0) {
      console.warn('[useTokenRefresh] Token expires soon, refreshing immediately');
      performRefresh(currentToken);
      return;
    }

    // Schedule refresh
    refreshTimeoutRef.current = setTimeout(() => {
      performRefresh(currentToken);
    }, refreshTime);
  }, []);

  /**
   * Perform the actual token refresh
   */
  const performRefresh = useCallback(async (currentToken: Token) => {
    try {
      console.log('[useTokenRefresh] Refreshing token...');
      const response = await refreshToken(currentToken.accessToken);

      const newToken: Token = {
        accessToken: response.accessToken,
        expiresAt: response.expiresAt,
        issuer: currentToken.issuer,
        audience: currentToken.audience,
      };

      // Update local state
      setToken(newToken);

      // Broadcast to other tabs
      broadcastTokenRefresh(newToken);

      // Schedule next refresh
      scheduleRefresh(newToken);

      console.log('[useTokenRefresh] Token refresh successful');
    } catch (error) {
      console.error('[useTokenRefresh] Token refresh failed:', error);
      // Clear auth state on refresh failure
      clearAuth();
      // Optionally redirect to login page or show notification
    }
  }, [setToken, clearAuth, scheduleRefresh]);

  /**
   * Broadcast token refresh to other tabs
   */
  const broadcastTokenRefresh = useCallback((newToken: Token) => {
    if (HAS_BROADCAST_CHANNEL && channelRef.current) {
      const message: TokenRefreshMessage = {
        type: 'TOKEN_REFRESHED',
        payload: newToken,
      };
      channelRef.current.postMessage(message);
      console.log('[useTokenRefresh] Broadcasted token refresh to other tabs');
    } else {
      // Fallback: Use localStorage for Safari 15.4-
      // Note: localStorage events only fire in OTHER tabs, not the current one
      localStorage.setItem('token-refresh', JSON.stringify({
        token: newToken,
        timestamp: Date.now(),
      }));
      console.log('[useTokenRefresh] Token refresh saved to localStorage (fallback)');
    }
  }, []);

  /**
   * Handle token refresh messages from other tabs
   */
  const handleTokenRefreshMessage = useCallback((event: MessageEvent<TokenRefreshMessage>) => {
    if (event.data.type === 'TOKEN_REFRESHED') {
      const newToken = event.data.payload;
      console.log('[useTokenRefresh] Received token refresh from another tab');
      setToken(newToken);
      scheduleRefresh(newToken);
    }
  }, [setToken, scheduleRefresh]);

  /**
   * Fallback: Handle localStorage events for Safari 15.4-
   */
  const handleStorageEvent = useCallback((event: StorageEvent) => {
    if (event.key === 'token-refresh' && event.newValue) {
      try {
        const data = JSON.parse(event.newValue);
        const newToken: Token = data.token;
        console.log('[useTokenRefresh] Received token refresh from localStorage (fallback)');
        setToken(newToken);
        scheduleRefresh(newToken);
      } catch (error) {
        console.error('[useTokenRefresh] Failed to parse localStorage token:', error);
      }
    }
  }, [setToken, scheduleRefresh]);

  /**
   * Effect: Initialize BroadcastChannel and schedule initial refresh
   */
  useEffect(() => {
    // Initialize BroadcastChannel if supported
    if (HAS_BROADCAST_CHANNEL) {
      channelRef.current = new BroadcastChannel(AUTH_CHANNEL_NAME);
      channelRef.current.addEventListener('message', handleTokenRefreshMessage);
      console.log('[useTokenRefresh] BroadcastChannel initialized');
    } else {
      // Fallback: Use localStorage events
      window.addEventListener('storage', handleStorageEvent);
      console.log('[useTokenRefresh] Using localStorage fallback (BroadcastChannel not supported)');
    }

    // Schedule refresh if token exists
    if (token) {
      scheduleRefresh(token);
    }

    // Cleanup
    return () => {
      if (refreshTimeoutRef.current) {
        clearTimeout(refreshTimeoutRef.current);
      }

      if (channelRef.current) {
        channelRef.current.removeEventListener('message', handleTokenRefreshMessage);
        channelRef.current.close();
      } else {
        window.removeEventListener('storage', handleStorageEvent);
      }
    };
  }, [token, scheduleRefresh, handleTokenRefreshMessage, handleStorageEvent]);
}
