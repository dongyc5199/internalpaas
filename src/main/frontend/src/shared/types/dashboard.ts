/**
 * Dashboard 相关类型定义
 */

/**
 * 统计卡片数据
 */
export interface StatCardData {
  /** 标题 */
  title: string;
  /** 数值 */
  value: number | string;
  /** 变化百分比 */
  change?: number;
  /** 变化趋势 */
  trend?: 'up' | 'down' | 'neutral';
  /** 图标名称 */
  icon?: string;
  /** 描述 */
  description?: string;
}

/**
 * 系统概览数据
 */
export interface SystemOverview {
  /** 服务器总数 */
  totalServers: number;
  /** 在线服务器数 */
  onlineServers: number;
  /** 应用总数 */
  totalApplications: number;
  /** 运行中的应用数 */
  runningApplications: number;
  /** 用户总数 */
  totalUsers: number;
  /** 活跃用户数 */
  activeUsers: number;
  /** 今日部署次数 */
  todayDeployments: number;
}

/**
 * 服务器状态分布
 */
export interface ServerStatusDistribution {
  /** 在线数量 */
  online: number;
  /** 离线数量 */
  offline: number;
  /** 维护中数量 */
  maintenance: number;
  /** 错误数量 */
  error: number;
  /** 未知数量 */
  unknown: number;
}

/**
 * 最近活动
 */
export interface RecentActivity {
  /** 活动ID */
  id: number;
  /** 活动类型 */
  type: 'deployment' | 'server_added' | 'user_login' | 'application_started' | 'application_stopped';
  /** 活动描述 */
  description: string;
  /** 用户名 */
  username?: string;
  /** 时间戳 */
  timestamp: string;
  /** 状态 */
  status: 'success' | 'warning' | 'error';
}

/**
 * 资源使用趋势数据点
 */
export interface ResourceTrendDataPoint {
  /** 时间戳 */
  timestamp: string;
  /** CPU使用率 */
  cpuUsage?: number;
  /** 内存使用率 */
  memoryUsage?: number;
  /** 磁盘使用率 */
  diskUsage?: number;
}

/**
 * Dashboard 数据
 */
export interface DashboardData {
  /** 系统概览 */
  overview: SystemOverview;
  /** 服务器状态分布 */
  serverDistribution: ServerStatusDistribution;
  /** 最近活动列表 */
  recentActivities: RecentActivity[];
  /** 资源使用趋势 */
  resourceTrends: ResourceTrendDataPoint[];
}
