import { apiClient } from './client';
import type { LoginCredentials, LoginResponse } from '../types/auth';
import type { User } from '../types/user';

/**
 * 认证 API 客户端
 *
 * 提供登录、登出、会话管理等认证相关的 API 调用
 */
export const authApi = {
  /**
   * 用户登录
   *
   * @param credentials - 登录凭证 (用户名 + 密码)
   * @returns 登录响应,包含用户信息
   *
   * @example
   * ```ts
   * const response = await authApi.login({
   *   username: 'admin',
   *   password: 'password123'
   * });
   * ```
   */
  login: async (credentials: LoginCredentials): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/login', credentials);
    return response;
  },

  /**
   * 用户登出
   *
   * @example
   * ```ts
   * await authApi.logout();
   * ```
   */
  logout: async (): Promise<void> => {
    await apiClient.post('/api/auth/logout');
  },

  /**
   * 获取当前登录用户信息
   *
   * @returns 当前用户信息
   *
   * @example
   * ```ts
   * const user = await authApi.getCurrentUser();
   * ```
   */
  getCurrentUser: async (): Promise<User> => {
    const response = await apiClient.get<User>('/api/auth/current-user');
    return response;
  },

  /**
   * 刷新会话
   * 用于检查会话是否有效,并更新会话时间
   *
   * @returns 更新后的用户信息
   *
   * @example
   * ```ts
   * const user = await authApi.refreshSession();
   * ```
   */
  refreshSession: async (): Promise<User> => {
    const response = await apiClient.get<User>('/api/auth/refresh');
    return response;
  },

  /**
   * 检查用户名是否可用
   *
   * @param username - 要检查的用户名
   * @returns 是否可用
   *
   * @example
   * ```ts
   * const isAvailable = await authApi.checkUsername('newuser');
   * ```
   */
  checkUsername: async (username: string): Promise<boolean> => {
    const response = await apiClient.get<{ available: boolean }>(
      `/api/auth/check-username?username=${encodeURIComponent(username)}`
    );
    return response.available;
  },
};
