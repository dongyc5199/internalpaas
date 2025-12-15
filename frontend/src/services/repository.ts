import api from './api';
import { Repository, CreateRepositoryRequest, PaginatedResponse } from '@/types';

export const repositoryService = {
  // 获取仓库列表
  async list(page = 1, pageSize = 20, projectId?: number): Promise<PaginatedResponse<Repository>> {
    const params: any = { page, page_size: pageSize };
    if (projectId) {
      params.project_id = projectId;
    }
    const response = await api.get<PaginatedResponse<Repository>>('/repositories', { params });
    return response.data;
  },

  // 获取仓库详情
  async get(id: number): Promise<Repository> {
    const response = await api.get<Repository>(`/repositories/${id}`);
    return response.data;
  },

  // 创建仓库
  async create(projectId: number, data: CreateRepositoryRequest): Promise<Repository> {
    const response = await api.post<Repository>(`/projects/${projectId}/repositories`, data);
    return response.data;
  },

  // 启用/禁用CI
  async updateCI(id: number, enable: boolean): Promise<void> {
    await api.put(`/repositories/${id}/ci`, { enable });
  },

  // 获取分支列���
  async getBranches(id: number): Promise<string[]> {
    const response = await api.get<{ branches: string[] }>(`/repositories/${id}/branches`);
    return response.data.branches;
  },

  // 获取提交列表
  async getCommits(id: number, branch: string, page = 1, pageSize = 20): Promise<any[]> {
    const response = await api.get<{ commits: any[] }>(`/repositories/${id}/commits`, {
      params: { branch, page, page_size: pageSize },
    });
    return response.data.commits;
  },
};
