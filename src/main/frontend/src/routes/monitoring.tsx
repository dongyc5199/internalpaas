import { lazy } from 'react';
import { ROUTES } from '../shared/constants';
import { UserRole } from '../shared/types/user';
import type { AppRoute } from './types';

const MonitoringDashboard = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.MonitoringDashboard }))
);
const ServerMonitoringDetail = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.ServerMonitoringDetail }))
);
const AlertsPage = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.AlertsPage }))
);
const AlertThresholdConfig = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.AlertThresholdConfig }))
);
const MonitoringHistory = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.MonitoringHistory }))
);
const MonitoringHistoryPage = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.MonitoringHistoryPage }))
);
const ThresholdDashboardPage = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.ThresholdDashboardPage }))
);
const AlertDetailPage = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.AlertDetailPage }))
);
const AlertNotificationSettings = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.AlertNotificationSettings }))
);
const PerformanceMetrics = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.PerformanceMetrics }))
);
const ResourceTrends = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.ResourceTrends }))
);
const ServerComparison = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.ServerComparison }))
);
const ServerHealthCheck = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.ServerHealthCheck }))
);
const UserActivityMonitor = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.UserActivityMonitor }))
);
const RealtimeMetricsDashboard = lazy(() =>
  import('../features/monitoring').then((module) => ({ default: module.RealtimeMetricsDashboard }))
);

export const monitoringRoutes: AppRoute[] = [
  {
    path: ROUTES.MONITORING.BASE,
    component: MonitoringDashboard,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载监控中心...',
  },
  {
    path: '/monitoring/servers/:serverId',
    component: ServerMonitoringDetail,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载服务器监控详情...',
  },
  {
    path: '/monitoring/alerts',
    component: AlertsPage,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载告警列表...',
  },
  {
    path: '/monitoring/servers/:serverId/thresholds',
    component: AlertThresholdConfig,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载阈值配置...',
  },
  {
    path: '/monitoring/history',
    component: MonitoringHistory,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载监控历史...',
  },
  {
    path: '/monitoring/history/dashboard',
    component: MonitoringHistoryPage,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载历史监控数据分析...',
  },
  {
    path: '/monitoring/thresholds/dashboard',
    component: ThresholdDashboardPage,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载告警阈值管理...',
  },
  {
    path: '/monitoring/alerts/:alertId',
    component: AlertDetailPage,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载告警详情...',
  },
  {
    path: '/monitoring/notifications',
    component: AlertNotificationSettings,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载通知设置...',
  },
  {
    path: '/monitoring/performance',
    component: PerformanceMetrics,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载性能指标...',
  },
  {
    path: '/monitoring/trends',
    component: ResourceTrends,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载趋势分析...',
  },
  {
    path: '/monitoring/comparison',
    component: ServerComparison,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载服务器对比...',
  },
  {
    path: '/monitoring/health',
    component: ServerHealthCheck,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载健康检查...',
  },
  {
    path: '/monitoring/user-activity',
    component: UserActivityMonitor,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载用户活动...',
  },
  {
    path: '/monitoring/realtime',
    component: RealtimeMetricsDashboard,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载实时指标...',
  },
];
