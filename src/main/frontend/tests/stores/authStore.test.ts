/**
 * Tests for authStore (Zustand Authentication Store)
 *
 * Test Coverage:
 * 1. Initial state
 * 2. setToken() - Updates token, parses user from JWT, sets isAuthenticated
 * 3. setUser() - Updates user without changing token
 * 4. clearAuth() - Clears all authentication state
 * 5. login() - Sets both token and user
 * 6. JWT parsing (valid, invalid, malformed)
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useAuthStore } from '../../react-app/stores/authStore';
import type { Token, User } from '../../react-app/types/auth';

describe('authStore', () => {
  beforeEach(() => {
    // Reset store state before each test
    const { result } = renderHook(() => useAuthStore());
    act(() => {
      result.current.clearAuth();
    });

    // Mock console.error
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // Helper to create JWT token
  const createJWT = (payload: Record<string, unknown>): string => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body = btoa(JSON.stringify(payload));
    const signature = 'fake-signature';
    return `${header}.${body}.${signature}`;
  };

  // Helper to create Token object
  const createToken = (payload: Record<string, unknown>): Token => ({
    accessToken: createJWT(payload),
    expiresAt: Date.now() + 10 * 60 * 1000, // 10 minutes
    issuer: 'test-issuer',
    audience: 'test-audience',
  });

  describe('Initial State', () => {
    it('应该有正确的初始状态', () => {
      const { result } = renderHook(() => useAuthStore());

      expect(result.current.token).toBeNull();
      expect(result.current.user).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
    });

    it('应该提供所有必需的方法', () => {
      const { result } = renderHook(() => useAuthStore());

      expect(typeof result.current.setToken).toBe('function');
      expect(typeof result.current.setUser).toBe('function');
      expect(typeof result.current.clearAuth).toBe('function');
      expect(typeof result.current.login).toBe('function');
    });
  });

  describe('setToken()', () => {
    it('应该更新token并从JWT解析用户信息', () => {
      const { result } = renderHook(() => useAuthStore());

      const token = createToken({
        sub: 'john.doe',
        role: 'ADMIN',
        exp: Math.floor(Date.now() / 1000) + 600,
      });

      act(() => {
        result.current.setToken(token);
      });

      expect(result.current.token).toEqual(token);
      expect(result.current.user).toEqual({
        username: 'john.doe',
        role: 'ADMIN',
      });
      expect(result.current.isAuthenticated).toBe(true);
    });

    it('应该正确解析包含sub的JWT', () => {
      const { result } = renderHook(() => useAuthStore());

      const token = createToken({
        sub: 'alice',
        role: 'DEVELOPER',
      });

      act(() => {
        result.current.setToken(token);
      });

      expect(result.current.user).toEqual({
        username: 'alice',
        role: 'DEVELOPER',
      });
    });

    it('应该正确解析包含role的JWT', () => {
      const { result } = renderHook(() => useAuthStore());

      const token = createToken({
        sub: 'admin-user',
        role: 'SUPER_ADMIN',
      });

      act(() => {
        result.current.setToken(token);
      });

      expect(result.current.user).toEqual({
        username: 'admin-user',
        role: 'SUPER_ADMIN',
      });
    });

    it('应该在缺少sub时使用默认用户名', () => {
      const { result } = renderHook(() => useAuthStore());

      const token = createToken({
        role: 'DEVELOPER',
        // sub missing
      });

      act(() => {
        result.current.setToken(token);
      });

      expect(result.current.user).toEqual({
        username: 'unknown',
        role: 'DEVELOPER',
      });
    });

    it('应该在缺少role时使用默认角色', () => {
      const { result } = renderHook(() => useAuthStore());

      const token = createToken({
        sub: 'test-user',
        // role missing
      });

      act(() => {
        result.current.setToken(token);
      });

      expect(result.current.user).toEqual({
        username: 'test-user',
        role: 'DEVELOPER',
      });
    });

    it('应该处理无效的JWT格式', () => {
      const { result } = renderHook(() => useAuthStore());

      const invalidToken: Token = {
        accessToken: 'not-a-valid-jwt',
        expiresAt: Date.now() + 10 * 60 * 1000,
        issuer: 'test-issuer',
        audience: 'test-audience',
      };

      act(() => {
        result.current.setToken(invalidToken);
      });

      // Should set token but fallback to default user
      expect(result.current.token).toEqual(invalidToken);
      expect(result.current.user).toEqual({
        username: 'unknown',
        role: 'DEVELOPER',
      });
      expect(result.current.isAuthenticated).toBe(true);
      expect(console.error).toHaveBeenCalled();
    });

    it('应该处理格式正确但payload无效的JWT', () => {
      const { result } = renderHook(() => useAuthStore());

      // Create JWT with invalid payload (not valid JSON)
      const header = btoa(JSON.stringify({ alg: 'HS256' }));
      const body = 'invalid-base64-{{';
      const signature = 'fake-signature';
      const malformedJWT = `${header}.${body}.${signature}`;

      const token: Token = {
        accessToken: malformedJWT,
        expiresAt: Date.now() + 10 * 60 * 1000,
        issuer: 'test-issuer',
        audience: 'test-audience',
      };

      act(() => {
        result.current.setToken(token);
      });

      expect(result.current.token).toEqual(token);
      expect(result.current.user).toEqual({
        username: 'unknown',
        role: 'DEVELOPER',
      });
      expect(console.error).toHaveBeenCalledWith(
        '[authStore] Failed to parse token:',
        expect.any(Error)
      );
    });
  });

  describe('setUser()', () => {
    it('应该只更新用户信息而不改变token', () => {
      const { result } = renderHook(() => useAuthStore());

      // First set a token
      const token = createToken({
        sub: 'original-user',
        role: 'DEVELOPER',
      });

      act(() => {
        result.current.setToken(token);
      });

      expect(result.current.user?.username).toBe('original-user');

      // Update user directly
      const newUser: User = {
        username: 'updated-user',
        role: 'ADMIN',
      };

      act(() => {
        result.current.setUser(newUser);
      });

      expect(result.current.user).toEqual(newUser);
      expect(result.current.token).toEqual(token); // Token unchanged
      expect(result.current.isAuthenticated).toBe(true); // Still authenticated
    });

    it('应该允许在没有token时更新用户', () => {
      const { result } = renderHook(() => useAuthStore());

      const user: User = {
        username: 'standalone-user',
        role: 'DEVELOPER',
      };

      act(() => {
        result.current.setUser(user);
      });

      expect(result.current.user).toEqual(user);
      expect(result.current.token).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
    });
  });

  describe('clearAuth()', () => {
    it('应该清除所有认证状态', () => {
      const { result } = renderHook(() => useAuthStore());

      // First set token
      const token = createToken({
        sub: 'test-user',
        role: 'ADMIN',
      });

      act(() => {
        result.current.setToken(token);
      });

      expect(result.current.isAuthenticated).toBe(true);

      // Clear auth
      act(() => {
        result.current.clearAuth();
      });

      expect(result.current.token).toBeNull();
      expect(result.current.user).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
    });

    it('应该在已清除状态下调用clearAuth时保持清除状态', () => {
      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.clearAuth();
      });

      expect(result.current.token).toBeNull();
      expect(result.current.user).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);

      // Call again
      act(() => {
        result.current.clearAuth();
      });

      expect(result.current.token).toBeNull();
      expect(result.current.user).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
    });
  });

  describe('login()', () => {
    it('应该同时设置token和user', () => {
      const { result } = renderHook(() => useAuthStore());

      const token = createToken({
        sub: 'jwt-user',
        role: 'DEVELOPER',
      });

      const user: User = {
        username: 'provided-user',
        role: 'ADMIN',
      };

      act(() => {
        result.current.login(token, user);
      });

      expect(result.current.token).toEqual(token);
      expect(result.current.user).toEqual(user); // Use provided user, not parsed
      expect(result.current.isAuthenticated).toBe(true);
    });

    it('应该允许用户信息与JWT中的不同', () => {
      const { result } = renderHook(() => useAuthStore());

      const token = createToken({
        sub: 'jwt-username',
        role: 'DEVELOPER',
      });

      const user: User = {
        username: 'custom-username', // Different from JWT
        role: 'SUPER_ADMIN', // Different from JWT
      };

      act(() => {
        result.current.login(token, user);
      });

      expect(result.current.user).toEqual(user);
      expect(result.current.user.username).not.toBe('jwt-username');
    });

    it('应该覆盖之前的认证状态', () => {
      const { result } = renderHook(() => useAuthStore());

      // First login
      const token1 = createToken({
        sub: 'user1',
        role: 'DEVELOPER',
      });

      const user1: User = {
        username: 'user1',
        role: 'DEVELOPER',
      };

      act(() => {
        result.current.login(token1, user1);
      });

      expect(result.current.user?.username).toBe('user1');

      // Second login
      const token2 = createToken({
        sub: 'user2',
        role: 'ADMIN',
      });

      const user2: User = {
        username: 'user2',
        role: 'ADMIN',
      };

      act(() => {
        result.current.login(token2, user2);
      });

      expect(result.current.token).toEqual(token2);
      expect(result.current.user).toEqual(user2);
      expect(result.current.isAuthenticated).toBe(true);
    });
  });

  describe('State Consistency', () => {
    it('应该在有token时isAuthenticated为true', () => {
      const { result } = renderHook(() => useAuthStore());

      const token = createToken({
        sub: 'test-user',
        role: 'DEVELOPER',
      });

      act(() => {
        result.current.setToken(token);
      });

      expect(result.current.isAuthenticated).toBe(true);
    });

    it('应该在没有token时isAuthenticated为false', () => {
      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.clearAuth();
      });

      expect(result.current.isAuthenticated).toBe(false);
    });

    it('应该在只设置user时isAuthenticated为false', () => {
      const { result } = renderHook(() => useAuthStore());

      const user: User = {
        username: 'standalone-user',
        role: 'DEVELOPER',
      };

      act(() => {
        result.current.setUser(user);
      });

      expect(result.current.user).not.toBeNull();
      expect(result.current.token).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
    });
  });

  describe('Multiple Store Instances', () => {
    it('应该在不同hook实例间共享状态', () => {
      const { result: result1 } = renderHook(() => useAuthStore());
      const { result: result2 } = renderHook(() => useAuthStore());

      const token = createToken({
        sub: 'shared-user',
        role: 'ADMIN',
      });

      // Update from first instance
      act(() => {
        result1.current.setToken(token);
      });

      // Both instances should reflect the change
      expect(result1.current.isAuthenticated).toBe(true);
      expect(result2.current.isAuthenticated).toBe(true);
      expect(result2.current.user?.username).toBe('shared-user');
    });

    it('应该在一个实例clearAuth后影响所有实例', () => {
      const { result: result1 } = renderHook(() => useAuthStore());
      const { result: result2 } = renderHook(() => useAuthStore());

      const token = createToken({
        sub: 'test-user',
        role: 'DEVELOPER',
      });

      act(() => {
        result1.current.setToken(token);
      });

      expect(result1.current.isAuthenticated).toBe(true);
      expect(result2.current.isAuthenticated).toBe(true);

      // Clear from second instance
      act(() => {
        result2.current.clearAuth();
      });

      // Both should be cleared
      expect(result1.current.isAuthenticated).toBe(false);
      expect(result2.current.isAuthenticated).toBe(false);
    });
  });
});
