import api from './api';
import { BuildRecord, TriggerBuildRequest, PaginatedResponse } from '@/types';

export const buildService = {
  // 获取所有构建列表（跨仓库）
  async listAll(
    page = 1,
    pageSize = 20,
    status?: string,
    repoId?: number
  ): Promise<PaginatedResponse<BuildRecord>> {
    const params: any = { page, page_size: pageSize };
    if (status) params.status = status;
    if (repoId) params.repository_id = repoId;

    const response = await api.get<PaginatedResponse<BuildRecord>>(
      '/builds',
      { params }
    );
    return response.data;
  },

  // 获取构建列表（特定仓库）
  async list(
    repoId: number,
    page = 1,
    pageSize = 20,
    status?: string,
    branch?: string
  ): Promise<PaginatedResponse<BuildRecord>> {
    const params: any = { page, page_size: pageSize };
    if (status) params.status = status;
    if (branch) params.branch = branch;

    const response = await api.get<PaginatedResponse<BuildRecord>>(
      `/repositories/${repoId}/builds`,
      { params }
    );
    return response.data;
  },

  // 获取构建详情
  async get(id: number): Promise<BuildRecord> {
    const response = await api.get<BuildRecord>(`/builds/${id}`);
    return response.data;
  },

  // 触发构建
  async trigger(repoId: number, data: TriggerBuildRequest): Promise<BuildRecord> {
    const response = await api.post<BuildRecord>(`/repositories/${repoId}/builds`, data);
    return response.data;
  },

  // 重启构建
  async restart(id: number): Promise<BuildRecord> {
    const response = await api.post<BuildRecord>(`/builds/${id}/restart`);
    return response.data;
  },

  // 取消构建
  async cancel(id: number): Promise<void> {
    await api.post(`/builds/${id}/cancel`);
  },

  // 获取构建日志
  async getLogs(id: number): Promise<string> {
    const response = await api.get<{ logs: string }>(`/builds/${id}/logs`);
    return response.data.logs;
  },

  // 获取构建统计
  async getStats(repoId: number): Promise<any> {
    const response = await api.get(`/repositories/${repoId}/builds/stats`);
    return response.data;
  },
};
