import { apiClient } from './client';
import type { DashboardData, SystemOverview, RecentActivity } from '../types/dashboard';

/**
 * Dashboard API 客户端
 *
 * 提供仪表板数据相关的 API 调用
 */
export const dashboardApi = {
  /**
   * 获取完整的 Dashboard 数据
   *
   * @returns Dashboard 数据
   *
   * @example
   * ```ts
   * const dashboardData = await dashboardApi.getDashboardData();
   * ```
   */
  getDashboardData: async (): Promise<DashboardData> => {
    return await apiClient.get<DashboardData>('/admin/dashboard/data');
  },

  /**
   * 获取系统概览数据
   *
   * @returns 系统概览数据
   *
   * @example
   * ```ts
   * const overview = await dashboardApi.getSystemOverview();
   * ```
   */
  getSystemOverview: async (): Promise<SystemOverview> => {
    return await apiClient.get<SystemOverview>('/admin/dashboard/overview');
  },

  /**
   * 获取最近活动列表
   *
   * @param limit - 返回数量限制
   * @returns 最近活动列表
   *
   * @example
   * ```ts
   * const activities = await dashboardApi.getRecentActivities(10);
   * ```
   */
  getRecentActivities: async (limit = 10): Promise<RecentActivity[]> => {
    return await apiClient.get<RecentActivity[]>(`/admin/dashboard/activities?limit=${limit}`);
  },

  /**
   * 刷新 Dashboard 数据
   *
   * @returns 刷新后的 Dashboard 数据
   *
   * @example
   * ```ts
   * const refreshedData = await dashboardApi.refreshDashboard();
   * ```
   */
  refreshDashboard: async (): Promise<DashboardData> => {
    return await apiClient.post<DashboardData>('/admin/dashboard/refresh');
  },
};
