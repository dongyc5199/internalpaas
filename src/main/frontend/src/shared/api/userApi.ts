import { apiClient } from './client';
import type { User, UserRole } from '../types/user';

/**
 * 用户创建/更新 DTO
 */
export interface UserFormData {
  username: string;
  email: string;
  password?: string;
  role: UserRole;
  displayName?: string;
}

/**
 * 用户列表查询参数
 */
export interface UserListParams {
  page?: number;
  size?: number;
  search?: string;
  role?: UserRole;
}

/**
 * 分页响应
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

/**
 * 用户 API 客户端
 *
 * 提供用户管理相关的 API 调用
 */
export const userApi = {
  /**
   * 获取用户列表
   *
   * @param params - 查询参数
   * @returns 分页的用户列表
   *
   * @example
   * ```ts
   * const users = await userApi.getUsers({
   *   page: 0,
   *   size: 20,
   *   role: UserRole.DEVELOPER
   * });
   * ```
   */
  getUsers: async (params?: UserListParams): Promise<PageResponse<User>> => {
    const queryParams = new URLSearchParams();

    if (params?.page !== undefined) queryParams.set('page', String(params.page));
    if (params?.size !== undefined) queryParams.set('size', String(params.size));
    if (params?.search) queryParams.set('search', params.search);
    if (params?.role) queryParams.set('role', params.role);

    const queryString = queryParams.toString();
    const url = `/admin/users${queryString ? `?${queryString}` : ''}`;

    return await apiClient.get<PageResponse<User>>(url);
  },

  /**
   * 获取用户详情
   *
   * @param id - 用户 ID
   * @returns 用户详情
   *
   * @example
   * ```ts
   * const user = await userApi.getUser(1);
   * ```
   */
  getUser: async (id: number): Promise<User> => {
    return await apiClient.get<User>(`/admin/users/${id}`);
  },

  /**
   * 创建用户
   *
   * @param data - 用户表单数据
   * @returns 创建的用户
   *
   * @example
   * ```ts
   * const newUser = await userApi.createUser({
   *   username: 'john',
   *   email: 'john@example.com',
   *   password: 'password123',
   *   role: UserRole.DEVELOPER,
   *   displayName: 'John Doe'
   * });
   * ```
   */
  createUser: async (data: UserFormData): Promise<User> => {
    return await apiClient.post<User>('/admin/users', data);
  },

  /**
   * 更新用户
   *
   * @param id - 用户 ID
   * @param data - 用户表单数据
   * @returns 更新后的用户
   *
   * @example
   * ```ts
   * const updatedUser = await userApi.updateUser(1, {
   *   displayName: 'John Smith',
   *   role: UserRole.ADMIN
   * });
   * ```
   */
  updateUser: async (id: number, data: Partial<UserFormData>): Promise<User> => {
    return await apiClient.put<User>(`/admin/users/${id}`, data);
  },

  /**
   * 删除用户
   *
   * @param id - 用户 ID
   *
   * @example
   * ```ts
   * await userApi.deleteUser(1);
   * ```
   */
  deleteUser: async (id: number): Promise<void> => {
    await apiClient.delete(`/admin/users/${id}`);
  },

  /**
   * 批量删除用户
   *
   * @param ids - 用户 ID 数组
   *
   * @example
   * ```ts
   * await userApi.batchDeleteUsers([1, 2, 3]);
   * ```
   */
  batchDeleteUsers: async (ids: number[]): Promise<void> => {
    await apiClient.post('/admin/users/batch-delete', { ids });
  },

  /**
   * 切换管理员角色
   *
   * @param id - 用户 ID
   * @returns 更新后的用户
   *
   * @example
   * ```ts
   * const user = await userApi.toggleAdmin(1);
   * ```
   */
  toggleAdmin: async (id: number): Promise<User> => {
    return await apiClient.post<User>(`/admin/users/${id}/toggle-admin`);
  },

  /**
   * 切换超级管理员角色
   *
   * @param id - 用户 ID
   * @returns 更新后的用户
   *
   * @example
   * ```ts
   * const user = await userApi.toggleSuperAdmin(1);
   * ```
   */
  toggleSuperAdmin: async (id: number): Promise<User> => {
    return await apiClient.post<User>(`/admin/users/${id}/toggle-super-admin`);
  },

  /**
   * 重置用户密码
   *
   * @param id - 用户 ID
   * @param newPassword - 新密码
   *
   * @example
   * ```ts
   * await userApi.resetPassword(1, 'newPassword123');
   * ```
   */
  resetPassword: async (id: number, newPassword: string): Promise<void> => {
    await apiClient.post(`/admin/users/${id}/reset-password`, { newPassword });
  },
};
