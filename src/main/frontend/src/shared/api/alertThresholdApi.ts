import { apiClient } from './client';
import type { AlertThreshold } from '../types';

/**
 * 阈值统计信息
 */
export interface ThresholdStatistics {
  totalThresholds: number;
  enabledThresholds: number;
  notificationEnabledThresholds: number;
}

/**
 * 复制阈值请求
 */
export interface CopyThresholdRequest {
  targetServerIds: number[];
}

/**
 * 测试阈值请求
 */
export interface TestThresholdRequest {
  serverId: number;
  metricType: string;
  testValue: number;
}

/**
 * 测试阈值响应
 */
export interface TestThresholdResponse {
  success: boolean;
  testValue: number;
  triggered: boolean;
  alertLevel?: string;
  message: string;
}

/**
 * 指标类型信息
 */
export interface MetricTypeInfo {
  name: string;
  displayName: string;
  unit: string;
}

/**
 * Alert Threshold API 客户端
 *
 * 提供告警阈值管理的完整API接口
 */
export const alertThresholdApi = {
  /**
   * 获取服务器的所有阈值配置
   */
  getServerThresholds: async (serverId: number): Promise<AlertThreshold[]> => {
    return await apiClient.get<AlertThreshold[]>(
      `/monitoring/thresholds/api/server/${serverId}`
    );
  },

  /**
   * 保存单个阈值配置
   */
  saveThreshold: async (threshold: AlertThreshold): Promise<{ success: boolean; threshold: AlertThreshold; message: string }> => {
    return await apiClient.post<{ success: boolean; threshold: AlertThreshold; message: string }>(
      '/monitoring/thresholds/api/save',
      threshold
    );
  },

  /**
   * 批量保存阈值配置
   */
  batchSaveThresholds: async (thresholds: AlertThreshold[]): Promise<{
    success: boolean;
    thresholds: AlertThreshold[];
    count: number;
    message: string;
  }> => {
    return await apiClient.post(
      '/monitoring/thresholds/api/batch-save',
      thresholds
    );
  },

  /**
   * 删除阈值配置
   */
  deleteThreshold: async (thresholdId: number): Promise<{ success: boolean; message: string }> => {
    return await apiClient.delete<{ success: boolean; message: string }>(
      `/monitoring/thresholds/api/${thresholdId}`
    );
  },

  /**
   * 重置服务器阈值为默认值
   */
  resetServerThresholds: async (serverId: number): Promise<{
    success: boolean;
    thresholds: AlertThreshold[];
    message: string;
  }> => {
    return await apiClient.post(
      `/monitoring/thresholds/api/server/${serverId}/reset`
    );
  },

  /**
   * 复制阈值配置到其他服务器
   */
  copyThresholds: async (
    sourceServerId: number,
    request: CopyThresholdRequest
  ): Promise<{
    success: boolean;
    thresholds: AlertThreshold[];
    message: string;
  }> => {
    return await apiClient.post(
      `/monitoring/thresholds/api/server/${sourceServerId}/copy`,
      request
    );
  },

  /**
   * 获取阈值统计信息
   */
  getStatistics: async (): Promise<ThresholdStatistics> => {
    return await apiClient.get<ThresholdStatistics>(
      '/monitoring/thresholds/api/statistics'
    );
  },

  /**
   * 创建服务器默认阈值配置
   */
  createDefaultThresholds: async (serverId: number): Promise<{
    success: boolean;
    thresholds: AlertThreshold[];
    message: string;
  }> => {
    return await apiClient.post(
      `/monitoring/thresholds/api/server/${serverId}/create-defaults`
    );
  },

  /**
   * 测试阈值配置
   */
  testThreshold: async (request: TestThresholdRequest): Promise<TestThresholdResponse> => {
    return await apiClient.post<TestThresholdResponse>(
      '/monitoring/thresholds/api/test',
      request
    );
  },

  /**
   * 获取指标类型信息
   */
  getMetricTypes: async (): Promise<{
    success: boolean;
    metricTypes: Record<string, MetricTypeInfo>;
  }> => {
    return await apiClient.get(
      '/monitoring/thresholds/api/metric-types'
    );
  },
};
