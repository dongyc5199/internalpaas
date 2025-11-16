/**
 * Terminal API Client
 * SSH终端API客户端
 *
 * 提供SSH终端相关的API调用函数
 */

import { apiClient } from './client';
import type {
  Server,
  SSHSession,
  SSHSessionStatus,
  SessionStats,
} from '../types';

/**
 * Terminal API 对象
 */
export const terminalApi = {
  /**
   * 获取可用服务器列表
   */
  getServers: async (): Promise<Server[]> => {
    return await apiClient.get<Server[]>('/terminal/api/servers');
  },

  /**
   * 获取当前用户的活跃SSH会话
   */
  getActiveSessions: async (): Promise<SSHSession[]> => {
    return await apiClient.get<SSHSession[]>('/terminal/api/sessions');
  },

  /**
   * 获取SSH会话状态
   * @param sessionId 会话ID
   */
  getSessionStatus: async (sessionId: string): Promise<SSHSessionStatus> => {
    return await apiClient.get<SSHSessionStatus>(
      `/terminal/api/session/${sessionId}/status`
    );
  },

  /**
   * 关闭SSH会话
   * @param sessionId 会话ID
   */
  closeSession: async (sessionId: string): Promise<{ status: string; message: string }> => {
    return await apiClient.delete<{ status: string; message: string }>(
      `/terminal/api/session/${sessionId}`
    );
  },

  /**
   * 获取会话统计信息
   */
  getSessionStats: async (): Promise<SessionStats> => {
    return await apiClient.get<SessionStats>('/terminal/api/stats');
  },

  /**
   * 测试服务器连接
   * @param serverId 服务器ID
   */
  testConnection: async (
    serverId: number
  ): Promise<{ success: boolean; message: string; responseTime?: number }> => {
    return await apiClient.post<{
      success: boolean;
      message: string;
      responseTime?: number;
    }>(`/terminal/api/test-connection/${serverId}`);
  },
};

/**
 * WebSocket URL 生成器
 * @param sessionId 会话ID (可选，连接时可能还没有)
 */
export const getWebSocketUrl = (sessionId?: string): string => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const host = window.location.host;
  const path = '/ws/terminal';

  let url = `${protocol}//${host}${path}`;
  if (sessionId) {
    url += `?sessionId=${sessionId}`;
  }

  return url;
};
