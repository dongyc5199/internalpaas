import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useMemo, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { monitoringApi } from '../../../shared/api/monitoringApi';
import { QUERY_KEYS } from '../../../shared/constants';
import { Chart, Loading } from '../../../shared/components';
import type { ChartDataPoint, TimeRange } from '../../../shared/types';
import styles from './MonitoringDashboard.module.css';

/**
 * MonitoringDashboard 组件
 *
 * 监控中心主页面 - 实时监控和告警管理
 */
export function MonitoringDashboard(): React.JSX.Element {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [timeRange, setTimeRange] = useState<TimeRange>('24h');
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [serverFilter, setServerFilter] = useState<'all' | 'online' | 'warning' | 'error'>('all');
  const [lastUpdateTime, setLastUpdateTime] = useState<string>(new Date().toISOString());

  // 获取监控概览
  const { data: overview, isLoading: overviewLoading } = useQuery({
    queryKey: [QUERY_KEYS.MONITORING_OVERVIEW],
    queryFn: () => monitoringApi.getOverview(),
    refetchInterval: autoRefresh ? 30000 : false,
  });

  // 获取所有服务器监控指标
  const { data: allMetrics, isLoading: metricsLoading } = useQuery({
    queryKey: [QUERY_KEYS.SERVER_METRICS],
    queryFn: () => monitoringApi.getAllServerMetrics(),
    refetchInterval: autoRefresh ? 10000 : false,
  });

  // 获取活跃告警
  const { data: activeAlerts, isLoading: alertsLoading } = useQuery({
    queryKey: [QUERY_KEYS.ACTIVE_ALERTS],
    queryFn: () => monitoringApi.getActiveAlerts(),
    refetchInterval: autoRefresh ? 30000 : false,
  });

  // Update last update time when data changes
  useEffect(() => {
    if (!overviewLoading && !metricsLoading && !alertsLoading) {
      setLastUpdateTime(new Date().toISOString());
    }
  }, [overviewLoading, metricsLoading, alertsLoading]);

  // Alert action mutations
  const acknowledgeAlertMutation = useMutation({
    mutationFn: async (_alertId: number) => {
      // Mock implementation
      await new Promise(resolve => setTimeout(resolve, 500));
      return { success: true };
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ACTIVE_ALERTS] });
    },
  });

  const resolveAlertMutation = useMutation({
    mutationFn: async (_alertId: number) => {
      // Mock implementation
      await new Promise(resolve => setTimeout(resolve, 500));
      return { success: true };
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ACTIVE_ALERTS] });
    },
  });

  // Filtered server list
  const filteredServers = useMemo(() => {
    if (!allMetrics) return [];

    switch (serverFilter) {
      case 'online':
        return allMetrics.filter(s => s.status === 'ONLINE');
      case 'warning':
        return allMetrics.filter(s => s.status === 'WARNING');
      case 'error':
        return allMetrics.filter(s => s.status === 'ERROR' || s.status === 'OFFLINE');
      default:
        return allMetrics;
    }
  }, [allMetrics, serverFilter]);

  // 准备图表数据 (使用 useMemo 优化)
  const serverStatusData = useMemo<ChartDataPoint[]>(() => [
    { name: '在线', value: overview?.onlineServers ?? 0 },
    { name: '警告', value: overview?.warningServers ?? 0 },
    { name: '错误', value: overview?.errorServers ?? 0 },
  ].filter((item) => item.value > 0), [overview]);

  const resourceUsageData = useMemo<ChartDataPoint[]>(() => [
    { resource: 'CPU', usage: overview?.avgCpuUsage ? overview.avgCpuUsage.toFixed(1) : '0' },
    { resource: '内存', usage: overview?.avgMemoryUsage ? overview.avgMemoryUsage.toFixed(1) : '0' },
    { resource: '磁盘', usage: overview?.avgDiskUsage ? overview.avgDiskUsage.toFixed(1) : '0' },
  ], [overview]);

  // 服务器列表数据
  const serverTrendData = useMemo<ChartDataPoint[]>(() =>
    allMetrics?.map((m) => ({
      server: m.serverName.length > 10 ? m.serverName.substring(0, 10) + '...' : m.serverName,
      cpu: m.cpuUsage,
      memory: m.memoryUsage,
    })) ?? [], [allMetrics]);

  // 切换自动刷新
  const toggleAutoRefresh = (): void => {
    setAutoRefresh((prev) => !prev);
  };

  // 获取状态颜色类
  const getStatusClass = (status: string): string => {
    switch (status) {
      case 'ONLINE':
        return styles.statusOnline ?? '';
      case 'WARNING':
        return styles.statusWarning ?? '';
      case 'ERROR':
      case 'OFFLINE':
        return styles.statusError ?? '';
      default:
        return '';
    }
  };

  // 获取告警级别颜色类
  const getAlertLevelClass = (level: string): string => {
    switch (level) {
      case 'CRITICAL':
        return styles.alertCritical ?? '';
      case 'ERROR':
        return styles.alertError ?? '';
      case 'WARNING':
        return styles.alertWarning ?? '';
      default:
        return styles.alertInfo ?? '';
    }
  };

  // 格式化时间
  const formatTime = (timestamp: string): string => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);

    if (diffMins < 1) return '刚刚';
    if (diffMins < 60) return `${diffMins}分钟前`;
    if (diffMins < 1440) return `${Math.floor(diffMins / 60)}小时前`;
    return `${Math.floor(diffMins / 1440)}天前`;
  };

  return (
    <div className={styles.container}>
      {/* 页面头部 */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleGroup}>
            <h1 className={styles.title}>监控中心</h1>
            <p className={styles.subtitle}>
              实时监控服务器状态和性能指标
              <span className={styles.lastUpdate}> · 最后更新: {formatTime(lastUpdateTime)}</span>
            </p>
          </div>
          <div className={styles.headerActions}>
            <button
              className={`${styles.btn} ${autoRefresh ? styles.btnActive : styles.btnSecondary}`}
              onClick={toggleAutoRefresh}
              title={autoRefresh ? '关闭自动刷新' : '开启自动刷新'}
            >
              <i data-lucide={autoRefresh ? 'pause' : 'play'} />
              <span>{autoRefresh ? '自动刷新' : '已暂停'}</span>
            </button>
            <select
              className={styles.select}
              value={timeRange}
              onChange={(e) => { setTimeRange(e.target.value as TimeRange); }}
            >
              <option value="1h">近1小时</option>
              <option value="6h">近6小时</option>
              <option value="24h">近24小时</option>
              <option value="7d">近7天</option>
              <option value="30d">近30天</option>
            </select>
          </div>
        </div>
      </header>

      {/* 快速操作面板 */}
      <section className={styles.quickActions}>
        <button
          className={styles.quickActionBtn}
          onClick={() => navigate('/monitoring/realtime')}
          title="实时指标仪表盘"
        >
          <i data-lucide="activity" />
          <span>实时监控</span>
        </button>
        <button
          className={styles.quickActionBtn}
          onClick={() => navigate('/monitoring/alerts')}
          title="告警列表"
        >
          <i data-lucide="bell" />
          <span>告警管理</span>
        </button>
        <button
          className={styles.quickActionBtn}
          onClick={() => navigate('/monitoring/performance')}
          title="性能指标"
        >
          <i data-lucide="trending-up" />
          <span>性能分析</span>
        </button>
        <button
          className={styles.quickActionBtn}
          onClick={() => navigate('/monitoring/health')}
          title="健康检查"
        >
          <i data-lucide="heart-pulse" />
          <span>健康检查</span>
        </button>
        <button
          className={styles.quickActionBtn}
          onClick={() => navigate('/monitoring/user-activity')}
          title="用户活动"
        >
          <i data-lucide="users" />
          <span>用户活动</span>
        </button>
      </section>

      {/* 统计概览卡片 */}
      <section className={styles.statsGrid}>
        <div className={styles.statCard}>
          <div className={styles.statIcon}>
            <i data-lucide="server" />
          </div>
          <div className={styles.statContent}>
            <div className={styles.statValue}>{overview?.totalServers ?? 0}</div>
            <div className={styles.statLabel}>总服务器数</div>
            <div className={styles.statDetail}>
              在线 {overview?.onlineServers ?? 0} / 离线{' '}
              {(overview?.totalServers ?? 0) - (overview?.onlineServers ?? 0)}
            </div>
          </div>
        </div>

        <div className={styles.statCard}>
          <div className={`${styles.statIcon} ${styles.iconWarning}`}>
            <i data-lucide="alert-triangle" />
          </div>
          <div className={styles.statContent}>
            <div className={styles.statValue}>{overview?.activeAlerts ?? 0}</div>
            <div className={styles.statLabel}>活跃告警</div>
            <div className={styles.statDetail}>今日已解决 {overview?.resolvedAlertsToday ?? 0}</div>
          </div>
        </div>

        <div className={styles.statCard}>
          <div className={`${styles.statIcon} ${styles.iconCpu}`}>
            <i data-lucide="cpu" />
          </div>
          <div className={styles.statContent}>
            <div className={styles.statValue}>{overview?.avgCpuUsage ? overview.avgCpuUsage.toFixed(1) : '0'}%</div>
            <div className={styles.statLabel}>平均CPU使用率</div>
            <div className={styles.statDetail}>
              内存 {overview?.avgMemoryUsage ? overview.avgMemoryUsage.toFixed(1) : '0'}%
            </div>
          </div>
        </div>

        <div className={styles.statCard}>
          <div className={`${styles.statIcon} ${styles.iconDisk}`}>
            <i data-lucide="hard-drive" />
          </div>
          <div className={styles.statContent}>
            <div className={styles.statValue}>{overview?.avgDiskUsage ? overview.avgDiskUsage.toFixed(1) : '0'}%</div>
            <div className={styles.statLabel}>平均磁盘使用率</div>
            <div className={styles.statDetail}>警告服务器 {overview?.warningServers ?? 0}</div>
          </div>
        </div>
      </section>

      {/* 图表区域 */}
      <section className={styles.chartsSection}>
        <div className={styles.chartsGrid}>
          {/* 服务器状态分布 */}
          <div className={styles.chartCard}>
            <Chart
              type="pie"
              data={serverStatusData}
              xKey="name"
              yKey="value"
              title="服务器状态分布"
              height={300}
              loading={overviewLoading}
              colors={['#10b981', '#f59e0b', '#ef4444']}
            />
          </div>

          {/* 平均资源使用率 */}
          <div className={styles.chartCard}>
            <Chart
              type="bar"
              data={resourceUsageData}
              xKey="resource"
              yKey="usage"
              title="平均资源使用率"
              height={300}
              loading={overviewLoading}
              colors={['#3b82f6']}
            />
          </div>

          {/* 服务器资源趋势 */}
          <div className={`${styles.chartCard} ${styles.chartWide}`}>
            <Chart
              type="bar"
              data={serverTrendData}
              xKey="server"
              yKey={['cpu', 'memory']}
              title="服务器资源使用情况"
              height={300}
              loading={metricsLoading}
              colors={['#3b82f6', '#ef4444']}
            />
          </div>
        </div>
      </section>

      {/* 服务器列表和告警 */}
      <section className={styles.contentGrid}>
        {/* 服务器监控列表 */}
        <div className={styles.panelCard}>
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>
              <i data-lucide="server" />
              <span>服务器监控</span>
            </h2>
            <div className={styles.panelActions}>
              <div className={styles.filterGroup}>
                <button
                  className={`${styles.filterBtn} ${serverFilter === 'all' ? styles.filterBtnActive : ''}`}
                  onClick={() => setServerFilter('all')}
                >
                  全部 ({allMetrics?.length ?? 0})
                </button>
                <button
                  className={`${styles.filterBtn} ${serverFilter === 'online' ? styles.filterBtnActive : ''}`}
                  onClick={() => setServerFilter('online')}
                >
                  在线 ({allMetrics?.filter(s => s.status === 'ONLINE').length ?? 0})
                </button>
                <button
                  className={`${styles.filterBtn} ${serverFilter === 'warning' ? styles.filterBtnActive : ''}`}
                  onClick={() => setServerFilter('warning')}
                >
                  警告 ({allMetrics?.filter(s => s.status === 'WARNING').length ?? 0})
                </button>
                <button
                  className={`${styles.filterBtn} ${serverFilter === 'error' ? styles.filterBtnActive : ''}`}
                  onClick={() => setServerFilter('error')}
                >
                  错误 ({allMetrics?.filter(s => s.status === 'ERROR' || s.status === 'OFFLINE').length ?? 0})
                </button>
              </div>
            </div>
          </div>
          <div className={styles.panelBody}>
            {metricsLoading && <Loading text="加载监控数据..." />}
            {!metricsLoading && filteredServers && filteredServers.length === 0 && (
              <div className={styles.emptyState}>
                <i data-lucide="server" />
                <p>暂无{serverFilter !== 'all' ? `${serverFilter}状态的` : ''}监控数据</p>
              </div>
            )}
            {!metricsLoading && filteredServers && filteredServers.length > 0 && (
              <div className={styles.serverList}>
                {filteredServers.map((server) => (
                  <div
                    key={server.serverId}
                    className={styles.serverItem}
                    onClick={() => { navigate(`/monitoring/servers/${server.serverId}`); }}
                  >
                    <div className={styles.serverInfo}>
                      <div className={styles.serverName}>{server.serverName}</div>
                      <div className={`${styles.serverStatus} ${getStatusClass(server.status)}`}>
                        {server.status}
                      </div>
                    </div>
                    <div className={styles.serverMetrics}>
                      <div className={styles.metric}>
                        <span className={styles.metricLabel}>CPU</span>
                        <span className={styles.metricValue}>{server.cpuUsage.toFixed(1)}%</span>
                      </div>
                      <div className={styles.metric}>
                        <span className={styles.metricLabel}>内存</span>
                        <span className={styles.metricValue}>{server.memoryUsage.toFixed(1)}%</span>
                      </div>
                      <div className={styles.metric}>
                        <span className={styles.metricLabel}>磁盘</span>
                        <span className={styles.metricValue}>{server.diskUsage.toFixed(1)}%</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* 活跃告警列表 */}
        <div className={styles.panelCard}>
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>
              <i data-lucide="alert-triangle" />
              <span>活跃告警</span>
            </h2>
            <span className={`${styles.badge} ${styles.badgeDanger}`}>
              {activeAlerts?.length ?? 0}
            </span>
          </div>
          <div className={styles.panelBody}>
            {alertsLoading && <Loading text="加载告警数据..." />}
            {!alertsLoading && activeAlerts && activeAlerts.length === 0 && (
              <div className={styles.emptyState}>
                <i data-lucide="check-circle" />
                <p>暂无活跃告警</p>
              </div>
            )}
            {!alertsLoading && activeAlerts && activeAlerts.length > 0 && (
              <div className={styles.alertList}>
                {activeAlerts.map((alert) => (
                  <div key={alert.id} className={styles.alertItem}>
                    <div className={`${styles.alertLevel} ${getAlertLevelClass(alert.level)}`}>
                      {alert.level}
                    </div>
                    <div className={styles.alertContent}>
                      <div className={styles.alertTitle}>
                        {alert.serverName} - {alert.type}
                      </div>
                      <div className={styles.alertMessage}>{alert.message}</div>
                      <div className={styles.alertTime}>{formatTime(alert.createdAt)}</div>
                    </div>
                    <div className={styles.alertActions}>
                      <button
                        className={styles.btnIconSmall}
                        title="确认告警"
                        onClick={(e) => {
                          e.stopPropagation();
                          acknowledgeAlertMutation.mutate(alert.id);
                        }}
                        disabled={acknowledgeAlertMutation.isPending}
                      >
                        <i data-lucide="check" />
                      </button>
                      <button
                        className={styles.btnIconSmall}
                        title="解决告警"
                        onClick={(e) => {
                          e.stopPropagation();
                          resolveAlertMutation.mutate(alert.id);
                        }}
                        disabled={resolveAlertMutation.isPending}
                      >
                        <i data-lucide="x" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
