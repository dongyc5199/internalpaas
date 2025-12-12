import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { monitoringApi } from '../../../shared/api/monitoringApi';
import { QUERY_KEYS } from '../../../shared/constants';
import { Chart, Button, Loading, WebSocketStatus as WebSocketIndicator } from '../../../shared/components';
import type { ChartDataPoint, TimeRange, MetricsDataPoint } from '../../../shared/types';
import { useRealtimeMetrics } from '../hooks/useRealtimeMetrics';
import styles from './ServerMonitoringDetail.module.css';

/**
 * ServerMonitoringDetail 组件
 *
 * 服务器监控详情页面 - 单服务器深度监控
 */
export function ServerMonitoringDetail(): React.JSX.Element {
  const { serverId } = useParams<{ serverId: string }>();
  const navigate = useNavigate();
  const [timeRange, setTimeRange] = useState<TimeRange>('24h');
  const [autoRefresh, setAutoRefresh] = useState(true);

  // WebSocket 实时监控数据
  const { status: wsStatus, reconnectCount } = useRealtimeMetrics({
    serverId: serverId ? Number(serverId) : undefined,
    enabled: autoRefresh,
  });

  // 获取服务器实时监控指标
  const { data: metrics, isLoading: metricsLoading } = useQuery({
    queryKey: [QUERY_KEYS.SERVER_METRICS, serverId],
    queryFn: () => monitoringApi.getServerMetrics(Number(serverId)),
    refetchInterval: autoRefresh ? 10000 : false,
    enabled: !!serverId,
  });

  // 获取服务器历史监控数据
  const { data: history, isLoading: historyLoading } = useQuery({
    queryKey: [QUERY_KEYS.SERVER_METRICS_HISTORY, serverId, timeRange],
    queryFn: () => monitoringApi.getServerMetricsHistory(Number(serverId), timeRange),
    refetchInterval: autoRefresh ? 30000 : false,
    enabled: !!serverId,
  });

  // 获取服务器活跃告警
  const { data: alerts, isLoading: alertsLoading } = useQuery({
    queryKey: [QUERY_KEYS.ACTIVE_ALERTS, serverId],
    queryFn: () => monitoringApi.getActiveAlerts(Number(serverId)),
    refetchInterval: autoRefresh ? 30000 : false,
    enabled: !!serverId,
  });

  // 刷新监控数据
  const handleRefresh = async (): Promise<void> => {
    if (!serverId) return;
    try {
      await monitoringApi.refreshServerMetrics(Number(serverId));
      // 刷新所有查询
      window.location.reload();
    } catch (error) {
      console.error('Failed to refresh metrics:', error);
    }
  };

  // 切换自动刷新
  const toggleAutoRefresh = (): void => {
    setAutoRefresh((prev) => !prev);
  };

  // 准备CPU历史数据
  const cpuHistoryData: ChartDataPoint[] =
    history?.dataPoints.map((point: MetricsDataPoint) => ({
      time: new Date(point.timestamp).toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
      }),
      value: point.cpuUsage,
    })) ?? [];

  // 准备内存历史数据
  const memoryHistoryData: ChartDataPoint[] =
    history?.dataPoints.map((point: MetricsDataPoint) => ({
      time: new Date(point.timestamp).toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
      }),
      value: point.memoryUsage,
    })) ?? [];

  // 准备磁盘历史数据
  const diskHistoryData: ChartDataPoint[] =
    history?.dataPoints.map((point: MetricsDataPoint) => ({
      time: new Date(point.timestamp).toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
      }),
      value: point.diskUsage,
    })) ?? [];

  // 准备网络历史数据
  const networkHistoryData: ChartDataPoint[] =
    history?.dataPoints.map((point: MetricsDataPoint) => ({
      time: new Date(point.timestamp).toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
      }),
      in: point.networkIn ?? 0,
      out: point.networkOut ?? 0,
    })) ?? [];

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

  // 格式化运行时间
  const formatUptime = (seconds: number): string => {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);

    if (days > 0) return `${days}天${hours}小时`;
    if (hours > 0) return `${hours}小时${minutes}分钟`;
    return `${minutes}分钟`;
  };

  if (metricsLoading || historyLoading) {
    return <Loading text="加载监控数据..." fullScreen />;
  }

  if (!metrics) {
    return (
      <div className={styles.error}>
        <i data-lucide="alert-triangle" />
        <p>无法加载服务器监控数据</p>
        <Button onClick={() => navigate('/monitoring')}>返回监控中心</Button>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      {/* 页面头部 */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleGroup}>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => navigate('/monitoring')}
              className={styles.backBtn}
            >
              <i data-lucide="arrow-left" />
              返回
            </Button>
            <div>
              <h1 className={styles.title}>{metrics.serverName}</h1>
              <div className={styles.subtitle}>
                <span className={`${styles.status} ${getStatusClass(metrics.status)}`}>
                  {metrics.status}
                </span>
                <span className={styles.separator}>·</span>
                <span>运行时间: {formatUptime(metrics.uptime)}</span>
                <span className={styles.separator}>·</span>
                <span>进程数: {metrics.processCount}</span>
              </div>
            </div>
          </div>
          <div className={styles.headerActions}>
            {/* WebSocket 状态指示器 */}
            {autoRefresh && (
              <WebSocketIndicator status={wsStatus} reconnectCount={reconnectCount} />
            )}
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
              onChange={(e) => {
                setTimeRange(e.target.value as TimeRange);
              }}
            >
              <option value="1h">近1小时</option>
              <option value="6h">近6小时</option>
              <option value="24h">近24小时</option>
              <option value="7d">近7天</option>
              <option value="30d">近30天</option>
            </select>
            <Button onClick={handleRefresh}>
              <i data-lucide="refresh-cw" />
              刷新
            </Button>
          </div>
        </div>
      </header>

      {/* 实时指标卡片 */}
      <section className={styles.metricsGrid}>
        <div className={styles.metricCard}>
          <div className={styles.metricHeader}>
            <i data-lucide="cpu" className={styles.iconCpu} />
            <span className={styles.metricLabel}>CPU使用率</span>
          </div>
          <div className={styles.metricValue}>{metrics.cpuUsage.toFixed(1)}%</div>
          <div className={styles.metricStats}>
            <span>平均: {history?.avgCpu ? history.avgCpu.toFixed(1) : '0'}%</span>
            <span>峰值: {history?.maxCpu ? history.maxCpu.toFixed(1) : '0'}%</span>
          </div>
        </div>

        <div className={styles.metricCard}>
          <div className={styles.metricHeader}>
            <i data-lucide="memory-stick" className={styles.iconMemory} />
            <span className={styles.metricLabel}>内存使用率</span>
          </div>
          <div className={styles.metricValue}>{metrics.memoryUsage.toFixed(1)}%</div>
          <div className={styles.metricStats}>
            <span>平均: {history?.avgMemory ? history.avgMemory.toFixed(1) : '0'}%</span>
            <span>峰值: {history?.maxMemory ? history.maxMemory.toFixed(1) : '0'}%</span>
          </div>
        </div>

        <div className={styles.metricCard}>
          <div className={styles.metricHeader}>
            <i data-lucide="hard-drive" className={styles.iconDisk} />
            <span className={styles.metricLabel}>磁盘使用率</span>
          </div>
          <div className={styles.metricValue}>{metrics.diskUsage.toFixed(1)}%</div>
          <div className={styles.metricStats}>
            <span>平均: {history?.avgDisk ? history.avgDisk.toFixed(1) : '0'}%</span>
            <span>峰值: {history?.maxDisk ? history.maxDisk.toFixed(1) : '0'}%</span>
          </div>
        </div>

        <div className={styles.metricCard}>
          <div className={styles.metricHeader}>
            <i data-lucide="network" className={styles.iconNetwork} />
            <span className={styles.metricLabel}>网络流量</span>
          </div>
          <div className={styles.metricValue}>
            <div className={styles.networkValue}>
              <span className={styles.networkIn}>↓ {(metrics.networkIn / 1024).toFixed(2)} MB/s</span>
              <span className={styles.networkOut}>↑ {(metrics.networkOut / 1024).toFixed(2)} MB/s</span>
            </div>
          </div>
        </div>
      </section>

      {/* 历史趋势图表 */}
      <section className={styles.chartsSection}>
        <div className={styles.chartsGrid}>
          {/* CPU趋势 */}
          <div className={styles.chartCard}>
            <Chart
              type="area"
              data={cpuHistoryData}
              xKey="time"
              yKey="value"
              title="CPU使用率趋势"
              height={280}
              loading={historyLoading}
              colors={['#3b82f6']}
            />
          </div>

          {/* 内存趋势 */}
          <div className={styles.chartCard}>
            <Chart
              type="area"
              data={memoryHistoryData}
              xKey="time"
              yKey="value"
              title="内存使用率趋势"
              height={280}
              loading={historyLoading}
              colors={['#10b981']}
            />
          </div>

          {/* 磁盘趋势 */}
          <div className={styles.chartCard}>
            <Chart
              type="area"
              data={diskHistoryData}
              xKey="time"
              yKey="value"
              title="磁盘使用率趋势"
              height={280}
              loading={historyLoading}
              colors={['#f59e0b']}
            />
          </div>

          {/* 网络流量趋势 */}
          <div className={styles.chartCard}>
            <Chart
              type="line"
              data={networkHistoryData}
              xKey="time"
              yKey={['in', 'out']}
              title="网络流量趋势"
              height={280}
              loading={historyLoading}
              colors={['#10b981', '#ef4444']}
            />
          </div>
        </div>
      </section>

      {/* 活跃告警 */}
      <section className={styles.alertsSection}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>
            <i data-lucide="alert-triangle" />
            <span>活跃告警</span>
          </h2>
          <span className={`${styles.badge} ${(alerts?.length ?? 0) > 0 ? styles.badgeDanger : ''}`}>
            {alerts?.length ?? 0}
          </span>
        </div>
        <div className={styles.alertsBody}>
          {alertsLoading && <Loading text="加载告警数据..." />}
          {!alertsLoading && (!alerts || alerts.length === 0) && (
            <div className={styles.emptyState}>
              <i data-lucide="check-circle" />
              <p>暂无活跃告警</p>
            </div>
          )}
          {!alertsLoading && alerts && alerts.length > 0 && (
            <div className={styles.alertList}>
              {alerts.map((alert) => (
                <div key={alert.id} className={styles.alertItem}>
                  <div className={`${styles.alertLevel} ${getAlertLevelClass(alert.level)}`}>
                    {alert.level}
                  </div>
                  <div className={styles.alertContent}>
                    <div className={styles.alertTitle}>{alert.type}</div>
                    <div className={styles.alertMessage}>{alert.message}</div>
                    <div className={styles.alertMeta}>
                      <span>当前值: {alert.value.toFixed(2)}</span>
                      <span className={styles.separator}>·</span>
                      <span>阈值: {alert.threshold.toFixed(2)}</span>
                      <span className={styles.separator}>·</span>
                      <span>{formatTime(alert.createdAt)}</span>
                    </div>
                  </div>
                  <div className={styles.alertActions}>
                    <button className={styles.btnIconSmall} title="确认">
                      <i data-lucide="check" />
                    </button>
                    <button className={styles.btnIconSmall} title="解决">
                      <i data-lucide="x" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
