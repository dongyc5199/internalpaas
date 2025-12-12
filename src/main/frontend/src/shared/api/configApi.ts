import { apiClient } from './client';

/**
 * 初始配置数据
 */
export interface InitialConfigData {
  workDirectory: string;
  enableNotifications?: boolean;
  autoBackup?: boolean;
  enableMetrics?: boolean;
}

/**
 * 配置响应
 */
export interface ConfigResponse {
  success: boolean;
  message: string;
}

/**
 * 用户配置信息
 */
export interface UserConfigInfo {
  username: string;
  isFirstTime: boolean;
  workDirectory: string | null;
  baseWorkDirectory: string;
}

/**
 * 配置 API 客户端
 *
 * 提供初始配置向导相关的 API 调用
 */
export const configApi = {
  /**
   * 获取用户配置信息
   *
   * @returns 用户配置信息
   *
   * @example
   * ```ts
   * const configInfo = await configApi.getUserConfigInfo();
   * ```
   */
  getUserConfigInfo: async (): Promise<UserConfigInfo> => {
    return await apiClient.get<UserConfigInfo>('/api/config/user-info');
  },

  /**
   * 保存初始配置
   *
   * @param data - 配置数据
   * @returns 配置响应
   *
   * @example
   * ```ts
   * await configApi.saveInitialConfig({
   *   workDirectory: './workspaces/john/',
   *   enableNotifications: true,
   *   autoBackup: true,
   *   enableMetrics: false
   * });
   * ```
   */
  saveInitialConfig: async (data: InitialConfigData): Promise<ConfigResponse> => {
    return await apiClient.post<ConfigResponse>('/initial-config', data);
  },
};
