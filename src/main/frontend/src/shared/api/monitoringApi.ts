import { apiClient } from './client';
import type {
  ServerMonitoringData,
  ServerMetricsHistory,
  TimeRange,
  Alert,
  AlertThreshold,
  MonitoringOverview,
  MonitoringConfig,
  AlertStatus,
} from '../types/monitoring';
import type { PageResponse } from '../types/common';

/**
 * 监控 API 客户端
 *
 * 提供服务器监控、告警管理等功能的API接口
 */
export const monitoringApi = {
  /**
   * 获取监控概览数据
   */
  getOverview: async (): Promise<MonitoringOverview> => {
    const response = await apiClient.get<MonitoringOverview>('/api/monitoring/overview');
    return response;
  },

  /**
   * 获取服务器实时监控指标
   */
  getServerMetrics: async (serverId: number): Promise<ServerMonitoringData> => {
    const response = await apiClient.get<ServerMonitoringData>(`/api/monitoring/servers/${serverId}/metrics`);
    return response;
  },

  /**
   * 获取所有服务器的实时监控指标
   */
  getAllServerMetrics: async (): Promise<ServerMonitoringData[]> => {
    const response = await apiClient.get<ServerMonitoringData[]>('/api/monitoring/servers/metrics');
    return response;
  },

  /**
   * 获取服务器历史监控数据
   */
  getServerMetricsHistory: async (
    serverId: number,
    timeRange: TimeRange = '24h'
  ): Promise<ServerMetricsHistory> => {
    const response = await apiClient.get<ServerMetricsHistory>(
      `/api/monitoring/servers/${serverId}/history?timeRange=${timeRange}`
    );
    return response;
  },

  /**
   * 刷新服务器监控数据
   */
  refreshServerMetrics: async (serverId: number): Promise<ServerMonitoringData> => {
    const response = await apiClient.post<ServerMonitoringData>(
      `/api/monitoring/servers/${serverId}/refresh`
    );
    return response;
  },

  /**
   * 获取告警列表
   */
  getAlerts: async (params?: {
    serverId?: number;
    status?: AlertStatus;
    page?: number;
    size?: number;
  }): Promise<PageResponse<Alert>> => {
    const queryString = new URLSearchParams(
      Object.entries(params || {}).reduce((acc, [key, value]) => {
        if (value !== undefined) acc[key] = String(value);
        return acc;
      }, {} as Record<string, string>)
    ).toString();
    const response = await apiClient.get<PageResponse<Alert>>(
      `/api/monitoring/alerts${queryString ? `?${queryString}` : ''}`
    );
    return response;
  },

  /**
   * 获取活跃告警
   */
  getActiveAlerts: async (serverId?: number): Promise<Alert[]> => {
    const queryString = serverId ? `?serverId=${serverId}` : '';
    const response = await apiClient.get<Alert[]>(`/api/monitoring/alerts/active${queryString}`);
    return response;
  },

  /**
   * 获取单个告警详情
   */
  getAlert: async (alertId: number): Promise<Alert> => {
    const response = await apiClient.get<Alert>(`/api/monitoring/alerts/${alertId}`);
    return response;
  },

  /**
   * 确认告警
   */
  acknowledgeAlert: async (alertId: number): Promise<Alert> => {
    const response = await apiClient.post<Alert>(`/api/monitoring/alerts/${alertId}/acknowledge`);
    return response;
  },

  /**
   * 解决告警
   */
  resolveAlert: async (alertId: number): Promise<Alert> => {
    const response = await apiClient.post<Alert>(`/api/monitoring/alerts/${alertId}/resolve`);
    return response;
  },

  /**
   * 批量确认告警
   */
  batchAcknowledgeAlerts: async (alertIds: number[]): Promise<void> => {
    await apiClient.post('/api/monitoring/alerts/batch/acknowledge', { alertIds });
  },

  /**
   * 批量解决告警
   */
  batchResolveAlerts: async (alertIds: number[]): Promise<void> => {
    await apiClient.post('/api/monitoring/alerts/batch/resolve', { alertIds });
  },

  /**
   * 获取告警阈值配置
   */
  getAlertThresholds: async (serverId: number): Promise<AlertThreshold[]> => {
    const response = await apiClient.get<AlertThreshold[]>(
      `/api/monitoring/servers/${serverId}/thresholds`
    );
    return response;
  },

  /**
   * 保存告警阈值配置
   */
  saveAlertThreshold: async (threshold: AlertThreshold): Promise<AlertThreshold> => {
    const response = await apiClient.post<AlertThreshold>(
      '/api/monitoring/thresholds',
      threshold
    );
    return response;
  },

  /**
   * 更新告警阈值配置
   */
  updateAlertThreshold: async (
    thresholdId: number,
    threshold: Partial<AlertThreshold>
  ): Promise<AlertThreshold> => {
    const response = await apiClient.put<AlertThreshold>(
      `/api/monitoring/thresholds/${thresholdId}`,
      threshold
    );
    return response;
  },

  /**
   * 删除告警阈值配置
   */
  deleteAlertThreshold: async (thresholdId: number): Promise<void> => {
    await apiClient.delete(`/api/monitoring/thresholds/${thresholdId}`);
  },

  /**
   * 获取监控配置
   */
  getMonitoringConfig: async (serverId: number): Promise<MonitoringConfig> => {
    const response = await apiClient.get<MonitoringConfig>(
      `/api/monitoring/servers/${serverId}/config`
    );
    return response;
  },

  /**
   * 更新监控配置
   */
  updateMonitoringConfig: async (
    serverId: number,
    config: Partial<MonitoringConfig>
  ): Promise<MonitoringConfig> => {
    const response = await apiClient.put<MonitoringConfig>(
      `/api/monitoring/servers/${serverId}/config`,
      config
    );
    return response;
  },

  /**
   * 导出监控数据
   */
  exportMetrics: async (serverId: number, timeRange: TimeRange): Promise<Blob> => {
    const response = await apiClient.get<Blob>(
      `/api/monitoring/servers/${serverId}/export?timeRange=${timeRange}`
    );
    return response;
  },
};
