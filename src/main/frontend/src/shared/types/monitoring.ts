/**
 * 监控相关类型定义
 */

/**
 * 服务器实时监控数据（详细版）
 */
export interface ServerMonitoringData {
  serverId: number;
  serverName: string;
  timestamp: string;
  cpuUsage: number;
  memoryUsage: number;
  diskUsage: number;
  networkIn: number;
  networkOut: number;
  processCount: number;
  uptime: number;
  status: 'ONLINE' | 'OFFLINE' | 'WARNING' | 'ERROR';
}

/**
 * 历史监控数据点
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
 * 服务器监控历史数据
 */
export interface ServerMetricsHistory {
  serverId: number;
  serverName: string;
  timeRange: TimeRange;
  dataPoints: MetricsDataPoint[];
  avgCpu: number;
  avgMemory: number;
  avgDisk: number;
  maxCpu: number;
  maxMemory: number;
  maxDisk: number;
}

/**
 * 时间范围
 */
export type TimeRange = '1h' | '6h' | '24h' | '7d' | '30d';

/**
 * 告警级别
 */
export type AlertLevel = 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL';

/**
 * 告警状态
 */
export type AlertStatus = 'ACTIVE' | 'RESOLVED' | 'ACKNOWLEDGED';

/**
 * 告警记录
 */
export interface Alert {
  id: number;
  serverId: number;
  serverName: string;
  level: AlertLevel;
  status: AlertStatus;
  type: string;
  message: string;
  value: number;
  threshold: number;
  createdAt: string;
  resolvedAt?: string;
  acknowledgedAt?: string;
  acknowledgedBy?: string;
}

/**
 * 告警阈值配置
 */
export interface AlertThreshold {
  id?: number;
  serverId: number;
  metricType: 'CPU' | 'MEMORY' | 'DISK' | 'NETWORK';
  warningThreshold: number;
  criticalThreshold: number;
  enabled: boolean;
  notifyEmail?: string;
}

/**
 * 监控概览
 */
export interface MonitoringOverview {
  totalServers: number;
  onlineServers: number;
  warningServers: number;
  errorServers: number;
  activeAlerts: number;
  resolvedAlertsToday: number;
  avgCpuUsage: number;
  avgMemoryUsage: number;
  avgDiskUsage: number;
}

/**
 * 实时监控数据 (WebSocket)
 */
export interface RealtimeMetrics {
  serverId: number;
  serverName: string;
  timestamp: string;
  metrics: {
    cpu: number;
    memory: number;
    disk: number;
    network: {
      in: number;
      out: number;
    };
  };
}

/**
 * 监控配置
 */
export interface MonitoringConfig {
  serverId: number;
  refreshInterval: number; // 刷新间隔(秒)
  retentionDays: number; // 数据保留天数
  enableAlerts: boolean;
  enableEmailNotify: boolean;
  notifyEmail?: string;
}
