/**
 * Realtime Metrics Dashboard Page
 * 实时指标仪表盘页面
 *
 * 功能：
 * - WebSocket 实时数据推送
 * - 所有服务器实时指标展示
 * - 迷你卡片网格布局
 * - 实时折线图 (CPU/内存/磁盘)
 * - 告警闪烁提示
 * - 连接状态指示器
 */

import React, { useState, useMemo, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import styles from './RealtimeMetricsDashboard.module.css';

// Types
interface RealtimeServerMetrics {
  serverId: number;
  serverName: string;
  host: string;
  status: 'ONLINE' | 'OFFLINE' | 'WARNING';
  cpuUsage: number;
  memoryUsage: number;
  diskUsage: number;
  networkIn: number;
  networkOut: number;
  responseTime: number;
  uptime: number;
  lastUpdate: string;
  cpuHistory: number[]; // Last 30 data points
  memoryHistory: number[];
  diskHistory: number[];
}

interface WebSocketStatus {
  connected: boolean;
  reconnecting: boolean;
  lastMessage: string;
  messageCount: number;
}

/**
 * Realtime Metrics Dashboard Component
 */
export function RealtimeMetricsDashboard(): React.JSX.Element {
  // State
  const [selectedMetric, setSelectedMetric] = useState<'cpu' | 'memory' | 'disk'>('cpu');
  const [wsStatus, setWsStatus] = useState<WebSocketStatus>({
    connected: false,
    reconnecting: false,
    lastMessage: '',
    messageCount: 0,
  });
  const [realtimeData, setRealtimeData] = useState<RealtimeServerMetrics[]>([]);

  // Fetch initial data
  const { data: initialData, isLoading } = useQuery({
    queryKey: ['realtime-metrics-dashboard'],
    queryFn: async () => {
      // Mock initial data
      const mockServers: RealtimeServerMetrics[] = [];
      for (let i = 1; i <= 12; i++) {
        const cpuUsage = 20 + Math.random() * 60;
        const memoryUsage = 30 + Math.random() * 50;
        const diskUsage = 40 + Math.random() * 40;

        mockServers.push({
          serverId: i,
          serverName: `Server-${i.toString().padStart(2, '0')}`,
          host: `192.168.1.${100 + i}`,
          status: cpuUsage > 80 || memoryUsage > 80 ? 'WARNING' : 'ONLINE',
          cpuUsage,
          memoryUsage,
          diskUsage,
          networkIn: Math.random() * 100,
          networkOut: Math.random() * 100,
          responseTime: 10 + Math.random() * 50,
          uptime: Math.floor(Math.random() * 30 * 24 * 3600),
          lastUpdate: new Date().toISOString(),
          cpuHistory: Array.from({ length: 30 }, () => 20 + Math.random() * 60),
          memoryHistory: Array.from({ length: 30 }, () => 30 + Math.random() * 50),
          diskHistory: Array.from({ length: 30 }, () => 40 + Math.random() * 40),
        });
      }
      return mockServers;
    },
    refetchInterval: false, // Don't poll, use WebSocket instead
  });

  // Initialize realtime data with initial data
  useEffect(() => {
    if (initialData && realtimeData.length === 0) {
      setRealtimeData(initialData);
    }
  }, [initialData, realtimeData.length]);

  // Simulate WebSocket connection
  useEffect(() => {
    // Simulate connection
    setTimeout(() => {
      setWsStatus((prev) => ({
        ...prev,
        connected: true,
        lastMessage: new Date().toISOString(),
      }));
    }, 1000);

    // Simulate realtime updates
    const interval = setInterval(() => {
      setRealtimeData((prevData) => {
        if (prevData.length === 0) return prevData;

        return prevData.map((server) => {
          // Randomly update metrics (simulate small changes)
          const cpuDelta = (Math.random() - 0.5) * 5;
          const memoryDelta = (Math.random() - 0.5) * 3;
          const diskDelta = (Math.random() - 0.5) * 1;

          const newCpuUsage = Math.max(0, Math.min(100, server.cpuUsage + cpuDelta));
          const newMemoryUsage = Math.max(0, Math.min(100, server.memoryUsage + memoryDelta));
          const newDiskUsage = Math.max(0, Math.min(100, server.diskUsage + diskDelta));

          return {
            ...server,
            cpuUsage: newCpuUsage,
            memoryUsage: newMemoryUsage,
            diskUsage: newDiskUsage,
            networkIn: Math.random() * 100,
            networkOut: Math.random() * 100,
            responseTime: 10 + Math.random() * 50,
            status: newCpuUsage > 80 || newMemoryUsage > 80 ? 'WARNING' : 'ONLINE',
            lastUpdate: new Date().toISOString(),
            cpuHistory: [...server.cpuHistory.slice(1), newCpuUsage],
            memoryHistory: [...server.memoryHistory.slice(1), newMemoryUsage],
            diskHistory: [...server.diskHistory.slice(1), newDiskUsage],
          };
        });
      });

      setWsStatus((prev) => ({
        ...prev,
        lastMessage: new Date().toISOString(),
        messageCount: prev.messageCount + 1,
      }));
    }, 2000); // Update every 2 seconds

    return () => clearInterval(interval);
  }, []);

  // Statistics
  const stats = useMemo(() => {
    if (realtimeData.length === 0)
      return {
        totalServers: 0,
        onlineServers: 0,
        warningServers: 0,
        offlineServers: 0,
        avgCpuUsage: 0,
        avgMemoryUsage: 0,
        avgDiskUsage: 0,
      };

    const onlineServers = realtimeData.filter((s) => s.status === 'ONLINE').length;
    const warningServers = realtimeData.filter((s) => s.status === 'WARNING').length;
    const offlineServers = realtimeData.filter((s) => s.status === 'OFFLINE').length;
    const avgCpuUsage =
      realtimeData.reduce((sum, s) => sum + s.cpuUsage, 0) / realtimeData.length;
    const avgMemoryUsage =
      realtimeData.reduce((sum, s) => sum + s.memoryUsage, 0) / realtimeData.length;
    const avgDiskUsage =
      realtimeData.reduce((sum, s) => sum + s.diskUsage, 0) / realtimeData.length;

    return {
      totalServers: realtimeData.length,
      onlineServers,
      warningServers,
      offlineServers,
      avgCpuUsage,
      avgMemoryUsage,
      avgDiskUsage,
    };
  }, [realtimeData]);

  // Format uptime
  const formatUptime = (seconds: number): string => {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    return `${days}d ${hours}h ${minutes}m`;
  };

  // Format relative time
  const formatRelativeTime = (timestamp: string): string => {
    const now = new Date();
    const time = new Date(timestamp);
    const diff = Math.floor((now.getTime() - time.getTime()) / 1000);

    if (diff < 5) return '刚刚';
    if (diff < 60) return `${diff}秒前`;
    return `${Math.floor(diff / 60)}分钟前`;
  };

  // Loading state
  if (isLoading) {
    return (
      <div className={styles.container}>
        <div className={styles.loading}>
          <div className={styles.spinner} />
          <p>加载实时监控数据...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      {/* Header */}
      <div className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleGroup}>
            <h1 className={styles.title}>实时指标仪表盘</h1>
            <p className={styles.subtitle}>所有服务器实时监控数据流</p>
          </div>

          {/* WebSocket Status */}
          <div className={styles.wsStatus}>
            <div
              className={`${styles.wsIndicator} ${
                wsStatus.connected
                  ? styles.wsConnected
                  : wsStatus.reconnecting
                    ? styles.wsReconnecting
                    : styles.wsDisconnected
              }`}
            />
            <span className={styles.wsText}>
              {wsStatus.connected
                ? '实时连接'
                : wsStatus.reconnecting
                  ? '重新连接中...'
                  : '连接断开'}
            </span>
            {wsStatus.connected && (
              <span className={styles.wsInfo}>
                已接收 {wsStatus.messageCount} 条消息 · 最后更新:{' '}
                {formatRelativeTime(wsStatus.lastMessage)}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Statistics */}
      <div className={styles.statsSection}>
        <div className={styles.statsGrid}>
          <div className={styles.statCard}>
            <div className={styles.statIcon}>
              <i className="lucide-server" />
            </div>
            <div className={styles.statContent}>
              <div className={styles.statLabel}>总服务器</div>
              <div className={styles.statValue}>{stats.totalServers}</div>
            </div>
          </div>

          <div className={styles.statCard}>
            <div className={`${styles.statIcon} ${styles.iconSuccess}`}>
              <i className="lucide-check-circle" />
            </div>
            <div className={styles.statContent}>
              <div className={styles.statLabel}>在线</div>
              <div className={styles.statValue}>{stats.onlineServers}</div>
            </div>
          </div>

          <div className={styles.statCard}>
            <div className={`${styles.statIcon} ${styles.iconWarning}`}>
              <i className="lucide-alert-triangle" />
            </div>
            <div className={styles.statContent}>
              <div className={styles.statLabel}>警告</div>
              <div className={styles.statValue}>{stats.warningServers}</div>
            </div>
          </div>

          <div className={styles.statCard}>
            <div className={`${styles.statIcon} ${styles.iconDanger}`}>
              <i className="lucide-x-circle" />
            </div>
            <div className={styles.statContent}>
              <div className={styles.statLabel}>离线</div>
              <div className={styles.statValue}>{stats.offlineServers}</div>
            </div>
          </div>

          <div className={styles.statCard}>
            <div className={styles.statIcon}>
              <i className="lucide-cpu" />
            </div>
            <div className={styles.statContent}>
              <div className={styles.statLabel}>平均 CPU</div>
              <div className={styles.statValue}>{stats.avgCpuUsage.toFixed(1)}%</div>
            </div>
          </div>

          <div className={styles.statCard}>
            <div className={styles.statIcon}>
              <i className="lucide-memory-stick" />
            </div>
            <div className={styles.statContent}>
              <div className={styles.statLabel}>平均内存</div>
              <div className={styles.statValue}>{stats.avgMemoryUsage.toFixed(1)}%</div>
            </div>
          </div>

          <div className={styles.statCard}>
            <div className={styles.statIcon}>
              <i className="lucide-hard-drive" />
            </div>
            <div className={styles.statContent}>
              <div className={styles.statLabel}>平均磁盘</div>
              <div className={styles.statValue}>{stats.avgDiskUsage.toFixed(1)}%</div>
            </div>
          </div>
        </div>
      </div>

      {/* Metric Selector */}
      <div className={styles.metricSelector}>
        <button
          className={`${styles.metricButton} ${selectedMetric === 'cpu' ? styles.metricButtonActive : ''}`}
          onClick={() => setSelectedMetric('cpu')}
        >
          <i className="lucide-cpu" />
          <span>CPU 使用率</span>
        </button>
        <button
          className={`${styles.metricButton} ${selectedMetric === 'memory' ? styles.metricButtonActive : ''}`}
          onClick={() => setSelectedMetric('memory')}
        >
          <i className="lucide-memory-stick" />
          <span>内存使用率</span>
        </button>
        <button
          className={`${styles.metricButton} ${selectedMetric === 'disk' ? styles.metricButtonActive : ''}`}
          onClick={() => setSelectedMetric('disk')}
        >
          <i className="lucide-hard-drive" />
          <span>磁盘使用率</span>
        </button>
      </div>

      {/* Server Grid */}
      <div className={styles.serverGrid}>
        {realtimeData.map((server) => {
          const currentMetricValue =
            selectedMetric === 'cpu'
              ? server.cpuUsage
              : selectedMetric === 'memory'
                ? server.memoryUsage
                : server.diskUsage;

          const history =
            selectedMetric === 'cpu'
              ? server.cpuHistory
              : selectedMetric === 'memory'
                ? server.memoryHistory
                : server.diskHistory;

          const isWarning = currentMetricValue > 80;
          const isCritical = currentMetricValue > 90;

          return (
            <div
              key={server.serverId}
              className={`${styles.serverCard} ${
                isCritical
                  ? styles.serverCardCritical
                  : isWarning
                    ? styles.serverCardWarning
                    : ''
              }`}
            >
              {/* Card Header */}
              <div className={styles.cardHeader}>
                <div className={styles.serverInfo}>
                  <div className={styles.serverName}>{server.serverName}</div>
                  <div className={styles.serverHost}>{server.host}</div>
                </div>
                <div
                  className={`${styles.statusBadge} ${
                    server.status === 'ONLINE'
                      ? styles.statusOnline
                      : server.status === 'WARNING'
                        ? styles.statusWarning
                        : styles.statusOffline
                  }`}
                >
                  {server.status === 'ONLINE'
                    ? '在线'
                    : server.status === 'WARNING'
                      ? '警告'
                      : '离线'}
                </div>
              </div>

              {/* Mini Chart */}
              <div className={styles.miniChart}>
                <svg viewBox="0 0 120 40" className={styles.chartSvg}>
                  {/* Background grid */}
                  <line x1="0" y1="10" x2="120" y2="10" stroke="var(--border)" strokeWidth="0.5" />
                  <line x1="0" y1="20" x2="120" y2="20" stroke="var(--border)" strokeWidth="0.5" />
                  <line x1="0" y1="30" x2="120" y2="30" stroke="var(--border)" strokeWidth="0.5" />

                  {/* Line path */}
                  <path
                    d={history
                      .map((value, index) => {
                        const x = (index / (history.length - 1)) * 120;
                        const y = 40 - (value / 100) * 40;
                        return index === 0 ? `M ${x} ${y}` : `L ${x} ${y}`;
                      })
                      .join(' ')}
                    fill="none"
                    stroke={isCritical ? '#ef4444' : isWarning ? '#f59e0b' : '#3b82f6'}
                    strokeWidth="2"
                  />

                  {/* Area fill */}
                  <path
                    d={`${history
                      .map((value, index) => {
                        const x = (index / (history.length - 1)) * 120;
                        const y = 40 - (value / 100) * 40;
                        return index === 0 ? `M ${x} ${y}` : `L ${x} ${y}`;
                      })
                      .join(' ')} L 120 40 L 0 40 Z`}
                    fill={isCritical ? 'rgba(239, 68, 68, 0.1)' : isWarning ? 'rgba(245, 158, 11, 0.1)' : 'rgba(59, 130, 246, 0.1)'}
                  />
                </svg>
              </div>

              {/* Current Value */}
              <div className={styles.currentValue}>
                <span className={styles.valueLabel}>
                  {selectedMetric === 'cpu'
                    ? 'CPU'
                    : selectedMetric === 'memory'
                      ? '内存'
                      : '磁盘'}
                </span>
                <span
                  className={`${styles.valueNumber} ${
                    isCritical
                      ? styles.valueCritical
                      : isWarning
                        ? styles.valueWarning
                        : ''
                  }`}
                >
                  {currentMetricValue.toFixed(1)}%
                </span>
              </div>

              {/* Additional Metrics */}
              <div className={styles.additionalMetrics}>
                <div className={styles.metricItem}>
                  <i className="lucide-activity" />
                  <span>{server.responseTime.toFixed(0)}ms</span>
                </div>
                <div className={styles.metricItem}>
                  <i className="lucide-clock" />
                  <span>{formatUptime(server.uptime)}</span>
                </div>
              </div>

              {/* Last Update */}
              <div className={styles.lastUpdate}>
                最后更新: {formatRelativeTime(server.lastUpdate)}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
