import { apiClient } from './client';

/**
 * 服务器历史数据
 */
export interface ServerHistoryData {
  serverId: number;
  serverName: string;
  dataPoints: MetricsDataPoint[];
  statistics: ServerStatistics;
}

/**
 * 监控数据点
 */
export interface MetricsDataPoint {
  timestamp: string;
  cpuUsage: number;
  memoryUsage: number;
  diskUsage: number;
  networkIn?: number;
  networkOut?: number;
}

/**
 * 服务器统计信息
 */
export interface ServerStatistics {
  avgCpu: number;
  maxCpu: number;
  minCpu: number;
  avgMemory: number;
  maxMemory: number;
  minMemory: number;
  avgDisk: number;
  maxDisk: number;
  minDisk: number;
}

/**
 * 服务器对比数据
 */
export interface ServerCompareData {
  servers: ServerHistoryData[];
  timeRange: {
    startTime: string;
    endTime: string;
  };
}

/**
 * 异常事件
 */
export interface AnomalyEvent {
  id: string;
  serverId: number;
  serverName: string;
  timestamp: string;
  type: 'cpu' | 'memory' | 'disk' | 'network';
  severity: 'warning' | 'error' | 'critical';
  message: string;
  value: number;
  threshold: number;
}

/**
 * 服务器排名
 */
export interface ServerRanking {
  serverId: number;
  serverName: string;
  avgCpu: number;
  avgMemory: number;
  avgDisk: number;
  score: number;
}

/**
 * 监控历史 API 客户端
 */
export const monitoringHistoryApi = {
  /**
   * 获取单个服务器的历史数据
   */
  getServerHistoryData: async (
    serverId: number,
    startTime: string,
    endTime: string,
    aggregated = false,
    aggregationType = 'hour'
  ): Promise<ServerHistoryData> => {
    const params = new URLSearchParams({
      startTime,
      endTime,
      aggregated: String(aggregated),
      aggregationType,
    });
    return await apiClient.get<ServerHistoryData>(
      `/monitoring/history/api/server/${serverId}/data?${params}`
    );
  },

  /**
   * 对比多个服务器数据
   */
  compareServers: async (
    serverIds: number[],
    startTime: string,
    endTime: string
  ): Promise<ServerCompareData> => {
    const params = new URLSearchParams({
      serverIds: serverIds.join(','),
      startTime,
      endTime,
    });
    return await apiClient.get<ServerCompareData>(
      `/monitoring/history/api/servers/compare?${params}`
    );
  },

  /**
   * 获取服务器实时数据
   */
  getRealtimeData: async (serverId: number): Promise<MetricsDataPoint> => {
    return await apiClient.get<MetricsDataPoint>(
      `/monitoring/history/api/server/${serverId}/realtime`
    );
  },

  /**
   * 获取服务器统计信息
   */
  getStatistics: async (
    serverId: number,
    startTime: string,
    endTime: string
  ): Promise<ServerStatistics> => {
    const params = new URLSearchParams({ startTime, endTime });
    return await apiClient.get<ServerStatistics>(
      `/monitoring/history/api/server/${serverId}/statistics?${params}`
    );
  },

  /**
   * 获取异常事件列表
   */
  getAnomalies: async (
    serverId: number,
    startTime: string,
    endTime: string
  ): Promise<AnomalyEvent[]> => {
    const params = new URLSearchParams({ startTime, endTime });
    return await apiClient.get<AnomalyEvent[]>(
      `/monitoring/history/api/server/${serverId}/anomalies?${params}`
    );
  },

  /**
   * 获取服务器性能排名
   */
  getServerRanking: async (
    startTime: string,
    endTime: string
  ): Promise<ServerRanking[]> => {
    const params = new URLSearchParams({ startTime, endTime });
    return await apiClient.get<ServerRanking[]>(
      `/monitoring/history/api/servers/ranking?${params}`
    );
  },

  /**
   * 导出历史数据为CSV
   */
  exportData: async (
    serverId: number,
    startTime: string,
    endTime: string
  ): Promise<Blob> => {
    const params = new URLSearchParams({ startTime, endTime });
    const response = await fetch(
      `/monitoring/history/api/server/${serverId}/export?${params}`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'text/csv',
        },
      }
    );
    if (!response.ok) {
      throw new Error('Export failed');
    }
    return await response.blob();
  },

  /**
   * 获取快速时间范围数据
   */
  getQuickRangeData: async (
    serverId: number,
    range: '1h' | '6h' | '24h' | '7d' | '30d'
  ): Promise<ServerHistoryData> => {
    return await apiClient.get<ServerHistoryData>(
      `/monitoring/history/api/server/${serverId}/quick-range?range=${range}`
    );
  },
};
