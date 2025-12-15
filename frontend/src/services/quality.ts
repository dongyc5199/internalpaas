import api from './api';
import { QualityReport, PaginatedResponse } from '@/types';

export const qualityService = {
  // 获取质量报告列表
  async list(repoId: number, page = 1, pageSize = 20): Promise<PaginatedResponse<QualityReport>> {
    const response = await api.get<PaginatedResponse<QualityReport>>(
      `/repositories/${repoId}/quality-reports`,
      { params: { page, page_size: pageSize } }
    );
    return response.data;
  },

  // 获取质量报告详情
  async get(id: number): Promise<QualityReport> {
    const response = await api.get<QualityReport>(`/quality-reports/${id}`);
    return response.data;
  },

  // 创建质量报告
  async create(data: { repository_id: number; build_id?: number }): Promise<QualityReport> {
    const response = await api.post<QualityReport>('/quality-reports', data);
    return response.data;
  },

  // 获取质量趋势
  async getTrend(repoId: number, days = 30): Promise<any> {
    const response = await api.get(`/repositories/${repoId}/quality-trend`, {
      params: { days },
    });
    return response.data;
  },

  // 获取质量统计
  async getStatistics(repoId: number): Promise<any> {
    const response = await api.get(`/repositories/${repoId}/quality-statistics`);
    return response.data;
  },
};
