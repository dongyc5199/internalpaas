import { useEffect, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useWebSocket } from '../../../shared/hooks/useWebSocket';
import { QUERY_KEYS, WS_TOPICS } from '../../../shared/constants';
import type { RealtimeMetrics } from '../../../shared/types';

/**
 * 实时监控指标 Hook 配置
 */
export interface UseRealtimeMetricsOptions {
  /**
   * 服务器ID (可选,不指定则监听所有服务器)
   */
  serverId?: number;

  /**
   * 是否启用实时更新
   * @default true
   */
  enabled?: boolean;

}

/**
 * 实时监控指标 Hook
 *
 * 通过 WebSocket 接收实时监控数据并更新 React Query 缓存
 *
 * @example
 * ```tsx
 * const { status, reconnectCount } = useRealtimeMetrics({
 *   serverId: 1,
 *   enabled: autoRefresh,
 * });
 * ```
 */
export function useRealtimeMetrics(options: UseRealtimeMetricsOptions = {}) {
  const { serverId, enabled = true } = options;

  const queryClient = useQueryClient();

  /**
   * 处理实时监控数据
   */
  const handleRealtimeData = useCallback(
    (data: RealtimeMetrics) => {
      // 如果指定了serverId,只处理该服务器的数据
      if (serverId && data.serverId !== serverId) {
        return;
      }

      // 更新单个服务器的监控数据
      queryClient.setQueryData(
        [QUERY_KEYS.SERVER_METRICS, data.serverId],
        (old: unknown) => {
          // 合并新数据到现有数据
          const baseData = old && typeof old === 'object' ? old : {};
          return {
            ...baseData,
            serverId: data.serverId,
            serverName: data.serverName,
            timestamp: data.timestamp,
            cpuUsage: data.metrics.cpu,
            memoryUsage: data.metrics.memory,
            diskUsage: data.metrics.disk,
            networkIn: data.metrics.network.in,
            networkOut: data.metrics.network.out,
          };
        }
      );

      // 如果没有指定serverId,更新所有服务器列表
      if (!serverId) {
        queryClient.invalidateQueries({
          queryKey: [QUERY_KEYS.SERVER_METRICS],
        });
      }
    },
    [serverId, queryClient]
  );

  /**
   * WebSocket 连接
   */
  const topic = serverId ? WS_TOPICS.SERVER_STATUS(serverId) : WS_TOPICS.SERVERS_GLOBAL;
  const { status, reconnectCount, disconnect } = useWebSocket<RealtimeMetrics>({
    topic,
    autoConnect: enabled,
    onMessage: handleRealtimeData,
  });

  /**
   * 清理连接
   */
  useEffect(() => {
    return () => {
      disconnect();
    };
  }, [disconnect]);

  return {
    status,
    reconnectCount,
  };
}
