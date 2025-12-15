import api from './api';
import { Project, CreateProjectRequest, PaginatedResponse } from '@/types';

export const projectService = {
  // 获取项目列表
  async list(page = 1, pageSize = 20): Promise<PaginatedResponse<Project>> {
    const response = await api.get<PaginatedResponse<Project>>('/projects', {
      params: { page, page_size: pageSize },
    });
    return response.data;
  },

  // 获取项目详情
  async get(id: number): Promise<Project> {
    const response = await api.get<Project>(`/projects/${id}`);
    return response.data;
  },

  // 创建项目
  async create(data: CreateProjectRequest): Promise<Project> {
    const response = await api.post<Project>('/projects', data);
    return response.data;
  },

  // 更新项目
  async update(id: number, data: Partial<CreateProjectRequest>): Promise<Project> {
    const response = await api.put<Project>(`/projects/${id}`, data);
    return response.data;
  },

  // 删除项目
  async delete(id: number): Promise<void> {
    await api.delete(`/projects/${id}`);
  },
};
