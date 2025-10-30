/**
 * Authentication State Store (Zustand)
 *
 * Global client-side authentication state management using Zustand.
 * Stores current token, user identity, and authentication status.
 */

import { create } from 'zustand';
import type { Token, User } from '../types/auth';

/**
 * Authentication state structure
 */
export interface AuthState {
  /**
   * Current access token (null if not authenticated)
   */
  token: Token | null;

  /**
   * Logged-in user information (null if not authenticated)
   */
  user: User | null;

  /**
   * Quick check for authentication status
   * @computed Derived from token existence
   */
  isAuthenticated: boolean;

  /**
   * Update token and parse user information
   * @param token - New token from BFF
   */
  setToken: (token: Token) => void;

  /**
   * Update only user information (without changing token)
   * @param user - User information
   */
  setUser: (user: User) => void;

  /**
   * Clear all authentication state (logout)
   */
  clearAuth: () => void;

  /**
   * Convenience method to set both token and user
   * @param token - New token from BFF
   * @param user - User information
   */
  login: (token: Token, user: User) => void;
}

/**
 * Parse user information from JWT payload
 * @internal Helper function for token parsing
 */
function parseTokenUser(accessToken: string): User {
  try {
    const payload = JSON.parse(atob(accessToken.split('.')[1]));
    return {
      username: payload.sub || 'unknown',
      role: payload.role || 'DEVELOPER',
    };
  } catch (error) {
    console.error('[authStore] Failed to parse token:', error);
    return { username: 'unknown', role: 'DEVELOPER' };
  }
}

/**
 * Create authentication store
 */
export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  user: null,
  isAuthenticated: false,

  setToken: (token) =>
    set({
      token,
      user: parseTokenUser(token.accessToken),
      isAuthenticated: true,
    }),

  setUser: (user) =>
    set({
      user,
    }),

  clearAuth: () =>
    set({
      token: null,
      user: null,
      isAuthenticated: false,
    }),

  login: (token, user) =>
    set({
      token,
      user,
      isAuthenticated: true,
    }),
}));
