import { apiClient } from './client';
import type { Server, ServerStatus, ServerMetrics } from '../types/server';

/**
 * 服务器创建/更新 DTO
 */
export interface ServerFormData {
  name: string;
  host: string;
  port: number;
  username: string;
  password?: string;
  tags?: string[];
  groupId?: number;
}

/**
 * 服务器列表查询参数
 */
export interface ServerListParams {
  page?: number;
  size?: number;
  search?: string;
  status?: ServerStatus;
  groupId?: number;
  tags?: string[];
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
 * 服务器 API 客户端
 *
 * 提供服务器管理相关的 API 调用
 */
export const serverApi = {
  /**
   * 获取服务器列表
   *
   * @param params - 查询参数
   * @returns 分页的服务器列表
   *
   * @example
   * ```ts
   * const servers = await serverApi.getServers({
   *   page: 0,
   *   size: 20,
   *   search: 'prod',
   *   status: ServerStatus.ONLINE
   * });
   * ```
   */
  getServers: async (params?: ServerListParams): Promise<PageResponse<Server>> => {
    const queryParams = new URLSearchParams();

    if (params?.page !== undefined) queryParams.set('page', String(params.page));
    if (params?.size !== undefined) queryParams.set('size', String(params.size));
    if (params?.search) queryParams.set('search', params.search);
    if (params?.status) queryParams.set('status', params.status);
    if (params?.groupId !== undefined) queryParams.set('groupId', String(params.groupId));
    if (params?.tags && params.tags.length > 0) {
      params.tags.forEach((tag) => queryParams.append('tags', tag));
    }

    const queryString = queryParams.toString();
    const url = `/admin/servers${queryString ? `?${queryString}` : ''}`;

    return await apiClient.get<PageResponse<Server>>(url);
  },

  /**
   * 获取服务器详情
   *
   * @param id - 服务器 ID
   * @returns 服务器详情
   *
   * @example
   * ```ts
   * const server = await serverApi.getServer(1);
   * ```
   */
  getServer: async (id: number): Promise<Server> => {
    return await apiClient.get<Server>(`/admin/servers/${id}`);
  },

  /**
   * 创建服务器
   *
   * @param data - 服务器表单数据
   * @returns 创建的服务器
   *
   * @example
   * ```ts
   * const newServer = await serverApi.createServer({
   *   name: 'Production Server 1',
   *   host: '192.168.1.100',
   *   port: 22,
   *   username: 'admin',
   *   password: 'password123',
   *   tags: ['production', 'web']
   * });
   * ```
   */
  createServer: async (data: ServerFormData): Promise<Server> => {
    return await apiClient.post<Server>('/admin/servers', data);
  },

  /**
   * 更新服务器
   *
   * @param id - 服务器 ID
   * @param data - 服务器表单数据
   * @returns 更新后的服务器
   *
   * @example
   * ```ts
   * const updatedServer = await serverApi.updateServer(1, {
   *   name: 'Updated Server Name',
   *   port: 2222
   * });
   * ```
   */
  updateServer: async (id: number, data: Partial<ServerFormData>): Promise<Server> => {
    return await apiClient.put<Server>(`/admin/servers/${id}`, data);
  },

  /**
   * 删除服务器
   *
   * @param id - 服务器 ID
   *
   * @example
   * ```ts
   * await serverApi.deleteServer(1);
   * ```
   */
  deleteServer: async (id: number): Promise<void> => {
    await apiClient.delete(`/admin/servers/${id}`);
  },

  /**
   * 批量删除服务器
   *
   * @param ids - 服务器 ID 数组
   *
   * @example
   * ```ts
   * await serverApi.batchDeleteServers([1, 2, 3]);
   * ```
   */
  batchDeleteServers: async (ids: number[]): Promise<void> => {
    await apiClient.post('/admin/servers/batch-delete', { ids });
  },

  /**
   * 测试服务器连接
   *
   * @param id - 服务器 ID
   * @returns 连接测试结果
   *
   * @example
   * ```ts
   * const result = await serverApi.testConnection(1);
   * if (result.success) {
   *   console.log('Connection successful');
   * }
   * ```
   */
  testConnection: async (id: number): Promise<{ success: boolean; message: string }> => {
    return await apiClient.post<{ success: boolean; message: string }>(
      `/admin/servers/${id}/test-connection`
    );
  },

  /**
   * 刷新服务器状态
   *
   * @param id - 服务器 ID
   * @returns 更新后的服务器状态
   *
   * @example
   * ```ts
   * const status = await serverApi.refreshStatus(1);
   * ```
   */
  refreshStatus: async (id: number): Promise<Server> => {
    return await apiClient.post<Server>(`/admin/servers/${id}/refresh`);
  },

  /**
   * 获取服务器监控指标
   *
   * @param id - 服务器 ID
   * @returns 服务器监控指标
   *
   * @example
   * ```ts
   * const metrics = await serverApi.getMetrics(1);
   * console.log(`CPU Usage: ${metrics.cpuUsage}%`);
   * ```
   */
  getMetrics: async (id: number): Promise<ServerMetrics> => {
    return await apiClient.get<ServerMetrics>(`/monitoring/server/${id}/metrics`);
  },

  /**
   * 获取所有可用标签
   *
   * @returns 标签列表
   *
   * @example
   * ```ts
   * const tags = await serverApi.getAllTags();
   * ```
   */
  getAllTags: async (): Promise<string[]> => {
    return await apiClient.get<string[]>('/admin/servers/tags');
  },
};
