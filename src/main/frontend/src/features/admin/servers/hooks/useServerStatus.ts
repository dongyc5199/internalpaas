import { useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useWebSocket } from '../../../../shared/hooks/useWebSocket';
import { QUERY_KEYS, WS_TOPICS } from '../../../../shared/constants';
import type { Server, ServerMetrics, ServerStatus } from '../../../../shared/types/server';
import type { PageResponse } from '../../../../shared/api/serverApi';

export interface ServerStatusUpdate {
  serverId: number;
  status: ServerStatus;
  statusMessage?: string;
  lastHealthCheck?: string;
  metrics?: ServerMetrics;
}

/**
 * 订阅服务器状态更新，并同步 React Query 缓存。
 */
export function useServerStatus(serverId: number) {
  const queryClient = useQueryClient();
  const topic = WS_TOPICS.SERVER_STATUS(serverId);

  const { status, reconnectCount, disconnect } = useWebSocket<ServerStatusUpdate>({
    topic,
    onMessage: (payload) => {
      // 更新详情缓存
      queryClient.setQueryData([QUERY_KEYS.SERVERS, serverId], (prev: unknown) => {
        if (!prev || typeof prev !== 'object') {
          return prev;
        }
        return {
          ...(prev as Server),
          status: payload.status,
          statusMessage: payload.statusMessage ?? (prev as Server).statusMessage,
          lastHealthCheck: payload.lastHealthCheck ?? (prev as Server).lastHealthCheck,
          metrics: payload.metrics ?? (prev as Server).metrics,
        };
      });

      // 更新列表缓存
      queryClient.setQueriesData({ queryKey: [QUERY_KEYS.SERVERS] }, (prev: unknown) => {
        if (!prev || typeof prev !== 'object') {
          return prev;
        }
        const page = prev as PageResponse<Server>;
        const content = page.content?.map((server) =>
          server.id === payload.serverId
            ? {
                ...server,
                status: payload.status,
                statusMessage: payload.statusMessage ?? server.statusMessage,
                lastHealthCheck: payload.lastHealthCheck ?? server.lastHealthCheck,
                metrics: payload.metrics ?? server.metrics,
              }
            : server
        );
        return { ...page, content };
      });
    },
  });

  useEffect(() => disconnect, [disconnect]);

  return { status, reconnectCount };
}
