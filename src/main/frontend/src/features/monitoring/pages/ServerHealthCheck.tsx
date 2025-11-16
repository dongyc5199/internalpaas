import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useMemo } from 'react';
import { serverApi } from '../../../shared/api/serverApi';
import { QUERY_KEYS } from '../../../shared/constants';
import { Button, Loading } from '../../../shared/components';
import type { Server } from '../../../shared/types';
import styles from './ServerHealthCheck.module.css';

/**
 * 健康检查结果
 */
interface HealthCheckResult {
  serverId: number;
  serverName: string;
  host: string;
  status: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY' | 'OFFLINE';
  healthScore: number; // 0-100
  lastCheckTime: string;
  responseTime: number; // ms
  issues: HealthIssue[];
}

/**
 * 健康问题
 */
interface HealthIssue {
  type: 'CPU' | 'MEMORY' | 'DISK' | 'NETWORK' | 'SERVICE';
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  message: string;
  value?: number;
  threshold?: number;
}

/**
 * 健康检查历史记录
 */
interface HealthCheckHistory {
  timestamp: string;
  status: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY' | 'OFFLINE';
  healthScore: number;
  responseTime: number;
}

/**
 * ServerHealthCheck 组件
 *
 * 服务器健康检查页面 - 展示所有服务器的健康状态总览
 */
export function ServerHealthCheck(): React.JSX.Element {
  const [selectedServerId, setSelectedServerId] = useState<number | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const queryClient = useQueryClient();

  // 获取所有服务器
  const { data: serversData, isLoading: serversLoading } = useQuery({
    queryKey: [QUERY_KEYS.SERVERS],
    queryFn: () => serverApi.getServers({ page: 0, size: 100 }),
  });

  const servers = (serversData?.content ?? []) as Server[];

  // 获取健康检查结果（模拟数据）
  const { data: healthResults, isLoading: healthLoading } = useQuery({
    queryKey: [QUERY_KEYS.MONITORING, 'health-check'],
    queryFn: async (): Promise<HealthCheckResult[]> => {
      // 模拟健康检查数据
      return servers.map((server) => {
        const cpuUsage = Math.random() * 100;
        const memoryUsage = Math.random() * 100;
        const diskUsage = Math.random() * 100;
        const responseTime = Math.random() * 500 + 50;

        // 计算健康评分
        const healthScore = Math.round(
          100 - (cpuUsage * 0.3 + memoryUsage * 0.3 + diskUsage * 0.2 + Math.min(responseTime / 5, 100) * 0.2)
        );

        // 确定健康状态
        let status: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY' | 'OFFLINE';
        if (server.status === 'OFFLINE') {
          status = 'OFFLINE';
        } else if (healthScore >= 80) {
          status = 'HEALTHY';
        } else if (healthScore >= 60) {
          status = 'DEGRADED';
        } else {
          status = 'UNHEALTHY';
        }

        // 生成健康问题
        const issues: HealthIssue[] = [];
        if (cpuUsage > 80) {
          issues.push({
            type: 'CPU',
            severity: cpuUsage > 90 ? 'CRITICAL' : 'WARNING',
            message: `CPU使用率过高: ${cpuUsage.toFixed(1)}%`,
            value: cpuUsage,
            threshold: 80,
          });
        }
        if (memoryUsage > 80) {
          issues.push({
            type: 'MEMORY',
            severity: memoryUsage > 90 ? 'CRITICAL' : 'WARNING',
            message: `内存使用率过高: ${memoryUsage.toFixed(1)}%`,
            value: memoryUsage,
            threshold: 80,
          });
        }
        if (diskUsage > 85) {
          issues.push({
            type: 'DISK',
            severity: diskUsage > 95 ? 'CRITICAL' : 'WARNING',
            message: `磁盘使用率过高: ${diskUsage.toFixed(1)}%`,
            value: diskUsage,
            threshold: 85,
          });
        }
        if (responseTime > 300) {
          issues.push({
            type: 'NETWORK',
            severity: responseTime > 400 ? 'CRITICAL' : 'WARNING',
            message: `响应时间过长: ${responseTime.toFixed(0)}ms`,
            value: responseTime,
            threshold: 300,
          });
        }

        return {
          serverId: server.id,
          serverName: server.name,
          host: server.host,
          status,
          healthScore,
          lastCheckTime: new Date().toISOString(),
          responseTime,
          issues,
        };
      });
    },
    refetchInterval: autoRefresh ? 30000 : false, // 自动刷新：30秒
  });

  // 获取健康检查历史（模拟数据）
  const { data: healthHistory } = useQuery({
    queryKey: [QUERY_KEYS.MONITORING, 'health-history', selectedServerId],
    queryFn: async (): Promise<HealthCheckHistory[]> => {
      if (!selectedServerId) return [];

      // 模拟24小时历史数据（每小时一个点）
      const history: HealthCheckHistory[] = [];
      const now = Date.now();

      for (let i = 23; i >= 0; i--) {
        const timestamp = new Date(now - i * 60 * 60 * 1000);
        const baseScore = 70 + Math.random() * 25;
        const healthScore = Math.max(0, Math.min(100, baseScore + Math.sin((i / 24) * Math.PI * 2) * 10));

        let status: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY' | 'OFFLINE';
        if (healthScore >= 80) {
          status = 'HEALTHY';
        } else if (healthScore >= 60) {
          status = 'DEGRADED';
        } else if (healthScore >= 40) {
          status = 'UNHEALTHY';
        } else {
          status = 'OFFLINE';
        }

        history.push({
          timestamp: timestamp.toISOString(),
          status,
          healthScore,
          responseTime: Math.random() * 300 + 50,
        });
      }

      return history;
    },
    enabled: selectedServerId !== null,
  });

  // 手动触发健康检查
  const triggerHealthCheck = useMutation({
    mutationFn: async (_serverId?: number) => {
      // 实际应调用 API: await monitoringApi.triggerHealthCheck(_serverId)
      await new Promise((resolve) => setTimeout(resolve, 1000));
      return { success: true };
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.MONITORING, 'health-check'] });
    },
  });

  // 筛选后的结果
  const filteredResults = useMemo(() => {
    if (!healthResults) return [];
    if (filterStatus === 'ALL') return healthResults;
    return healthResults.filter((result) => result.status === filterStatus);
  }, [healthResults, filterStatus]);

  // 统计数据
  const stats = useMemo(() => {
    if (!healthResults) return null;

    const healthy = healthResults.filter((r) => r.status === 'HEALTHY').length;
    const degraded = healthResults.filter((r) => r.status === 'DEGRADED').length;
    const unhealthy = healthResults.filter((r) => r.status === 'UNHEALTHY').length;
    const offline = healthResults.filter((r) => r.status === 'OFFLINE').length;
    const avgHealthScore =
      healthResults.reduce((sum, r) => sum + r.healthScore, 0) / healthResults.length;
    const avgResponseTime =
      healthResults.reduce((sum, r) => sum + r.responseTime, 0) / healthResults.length;
    const totalIssues = healthResults.reduce((sum, r) => sum + r.issues.length, 0);

    return {
      healthy,
      degraded,
      unhealthy,
      offline,
      total: healthResults.length,
      avgHealthScore,
      avgResponseTime,
      totalIssues,
    };
  }, [healthResults]);

  // 获取状态图标
  const getStatusIcon = (status: string): string => {
    switch (status) {
      case 'HEALTHY':
        return 'check-circle';
      case 'DEGRADED':
        return 'alert-circle';
      case 'UNHEALTHY':
        return 'x-circle';
      case 'OFFLINE':
        return 'power-off';
      default:
        return 'help-circle';
    }
  };

  // 获取状态颜色类
  const getStatusClass = (status: string): string => {
    switch (status) {
      case 'HEALTHY':
        return styles.statusHealthy ?? '';
      case 'DEGRADED':
        return styles.statusDegraded ?? '';
      case 'UNHEALTHY':
        return styles.statusUnhealthy ?? '';
      case 'OFFLINE':
        return styles.statusOffline ?? '';
      default:
        return '';
    }
  };

  // 获取健康评分颜色类
  const getScoreClass = (score: number): string => {
    if (score >= 80) return styles.scoreHigh ?? '';
    if (score >= 60) return styles.scoreMedium ?? '';
    return styles.scoreLow ?? '';
  };

  // 获取问题严重性颜色类
  const getSeverityClass = (severity: string): string => {
    switch (severity) {
      case 'CRITICAL':
        return styles.severityCritical ?? '';
      case 'WARNING':
        return styles.severityWarning ?? '';
      case 'INFO':
        return styles.severityInfo ?? '';
      default:
        return '';
    }
  };

  // 格式化时间
  const formatTime = (timestamp: string): string => {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);

    if (minutes < 1) return '刚刚';
    if (minutes < 60) return `${minutes}分钟前`;
    if (minutes < 1440) return `${Math.floor(minutes / 60)}小时前`;
    return date.toLocaleString('zh-CN');
  };

  const isLoading = serversLoading || healthLoading;

  return (
    <div className={styles.container}>
      {/* 页面头部 */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleGroup}>
            <h1 className={styles.title}>服务器健康检查</h1>
            <p className={styles.subtitle}>实时监控所有服务器的健康状态</p>
          </div>
          <div className={styles.headerActions}>
            <label className={styles.autoRefreshToggle}>
              <input
                type="checkbox"
                checked={autoRefresh}
                onChange={(e) => setAutoRefresh(e.target.checked)}
                className={styles.checkbox}
              />
              <span>自动刷新 (30s)</span>
            </label>
            <Button
              variant="primary"
              onClick={() => triggerHealthCheck.mutate(undefined)}
              disabled={triggerHealthCheck.isPending}
            >
              <i data-lucide="refresh-cw" />
              {triggerHealthCheck.isPending ? '检查中...' : '全部检查'}
            </Button>
          </div>
        </div>
      </header>

      {isLoading ? (
        <Loading text="加载健康检查数据..." />
      ) : (
        <>
          {/* 统计卡片 */}
          {stats && (
            <div className={styles.statsGrid}>
              <div className={styles.statCard}>
                <div className={`${styles.statIcon} ${styles.statusHealthy}`}>
                  <i data-lucide="check-circle" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>健康</div>
                  <div className={styles.statValue}>{stats.healthy}</div>
                  <div className={styles.statPercent}>
                    {((stats.healthy / stats.total) * 100).toFixed(0)}%
                  </div>
                </div>
              </div>

              <div className={styles.statCard}>
                <div className={`${styles.statIcon} ${styles.statusDegraded}`}>
                  <i data-lucide="alert-circle" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>降级</div>
                  <div className={styles.statValue}>{stats.degraded}</div>
                  <div className={styles.statPercent}>
                    {((stats.degraded / stats.total) * 100).toFixed(0)}%
                  </div>
                </div>
              </div>

              <div className={styles.statCard}>
                <div className={`${styles.statIcon} ${styles.statusUnhealthy}`}>
                  <i data-lucide="x-circle" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>不健康</div>
                  <div className={styles.statValue}>{stats.unhealthy}</div>
                  <div className={styles.statPercent}>
                    {((stats.unhealthy / stats.total) * 100).toFixed(0)}%
                  </div>
                </div>
              </div>

              <div className={styles.statCard}>
                <div className={`${styles.statIcon} ${styles.statusOffline}`}>
                  <i data-lucide="power-off" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>离线</div>
                  <div className={styles.statValue}>{stats.offline}</div>
                  <div className={styles.statPercent}>
                    {((stats.offline / stats.total) * 100).toFixed(0)}%
                  </div>
                </div>
              </div>

              <div className={styles.statCard}>
                <div className={styles.statIcon}>
                  <i data-lucide="activity" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>平均健康评分</div>
                  <div className={`${styles.statValue} ${getScoreClass(stats.avgHealthScore)}`}>
                    {stats.avgHealthScore.toFixed(0)}
                  </div>
                </div>
              </div>

              <div className={styles.statCard}>
                <div className={styles.statIcon}>
                  <i data-lucide="clock" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>平均响应时间</div>
                  <div className={styles.statValue}>{stats.avgResponseTime.toFixed(0)}ms</div>
                </div>
              </div>

              <div className={styles.statCard}>
                <div className={styles.statIcon}>
                  <i data-lucide="alert-triangle" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>问题总数</div>
                  <div className={styles.statValue}>{stats.totalIssues}</div>
                </div>
              </div>
            </div>
          )}

          {/* 筛选控制 */}
          <div className={styles.filterSection}>
            <div className={styles.filterGroup}>
              <label className={styles.filterLabel}>筛选状态</label>
              <div className={styles.buttonGroup}>
                <Button
                  size="sm"
                  variant={filterStatus === 'ALL' ? 'primary' : 'secondary'}
                  onClick={() => setFilterStatus('ALL')}
                >
                  全部 ({healthResults?.length ?? 0})
                </Button>
                <Button
                  size="sm"
                  variant={filterStatus === 'HEALTHY' ? 'primary' : 'secondary'}
                  onClick={() => setFilterStatus('HEALTHY')}
                >
                  <i data-lucide="check-circle" />
                  健康 ({stats?.healthy ?? 0})
                </Button>
                <Button
                  size="sm"
                  variant={filterStatus === 'DEGRADED' ? 'primary' : 'secondary'}
                  onClick={() => setFilterStatus('DEGRADED')}
                >
                  <i data-lucide="alert-circle" />
                  降级 ({stats?.degraded ?? 0})
                </Button>
                <Button
                  size="sm"
                  variant={filterStatus === 'UNHEALTHY' ? 'primary' : 'secondary'}
                  onClick={() => setFilterStatus('UNHEALTHY')}
                >
                  <i data-lucide="x-circle" />
                  不健康 ({stats?.unhealthy ?? 0})
                </Button>
                <Button
                  size="sm"
                  variant={filterStatus === 'OFFLINE' ? 'primary' : 'secondary'}
                  onClick={() => setFilterStatus('OFFLINE')}
                >
                  <i data-lucide="power-off" />
                  离线 ({stats?.offline ?? 0})
                </Button>
              </div>
            </div>
          </div>

          {/* 健康检查列表 */}
          <div className={styles.healthList}>
            {filteredResults.map((result) => (
              <div
                key={result.serverId}
                className={`${styles.healthCard} ${selectedServerId === result.serverId ? styles.healthCardSelected : ''}`}
                onClick={() =>
                  setSelectedServerId(selectedServerId === result.serverId ? null : result.serverId)
                }
              >
                <div className={styles.healthCardHeader}>
                  <div className={styles.serverInfo}>
                    <div className={`${styles.statusIndicator} ${getStatusClass(result.status)}`}>
                      <i data-lucide={getStatusIcon(result.status)} />
                    </div>
                    <div>
                      <h3 className={styles.serverName}>{result.serverName}</h3>
                      <p className={styles.serverHost}>{result.host}</p>
                    </div>
                  </div>
                  <div className={styles.healthScoreBadge}>
                    <div className={`${styles.scoreCircle} ${getScoreClass(result.healthScore)}`}>
                      <span className={styles.scoreValue}>{result.healthScore}</span>
                      <span className={styles.scoreLabel}>分</span>
                    </div>
                  </div>
                </div>

                <div className={styles.healthCardBody}>
                  <div className={styles.healthMetrics}>
                    <div className={styles.metricItem}>
                      <i data-lucide="clock" />
                      <span>响应时间: {result.responseTime.toFixed(0)}ms</span>
                    </div>
                    <div className={styles.metricItem}>
                      <i data-lucide="calendar" />
                      <span>最后检查: {formatTime(result.lastCheckTime)}</span>
                    </div>
                    <div className={styles.metricItem}>
                      <i data-lucide="alert-triangle" />
                      <span>问题数: {result.issues.length}</span>
                    </div>
                  </div>

                  {result.issues.length > 0 && (
                    <div className={styles.issuesList}>
                      <div className={styles.issuesHeader}>
                        <i data-lucide="alert-triangle" />
                        <span>检测到的问题 ({result.issues.length})</span>
                      </div>
                      {result.issues.map((issue, index) => (
                        <div key={index} className={`${styles.issueItem} ${getSeverityClass(issue.severity)}`}>
                          <div className={styles.issueSeverity}>{issue.severity}</div>
                          <div className={styles.issueMessage}>{issue.message}</div>
                        </div>
                      ))}
                    </div>
                  )}

                  <div className={styles.healthCardActions}>
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={(e) => {
                        e.stopPropagation();
                        triggerHealthCheck.mutate(result.serverId);
                      }}
                      disabled={triggerHealthCheck.isPending}
                    >
                      <i data-lucide="refresh-cw" />
                      重新检查
                    </Button>
                  </div>
                </div>

                {/* 展开的历史趋势 */}
                {selectedServerId === result.serverId && healthHistory && healthHistory.length > 0 && (
                  <div className={styles.historySection}>
                    <h4 className={styles.historyTitle}>
                      <i data-lucide="trending-up" />
                      24小时健康趋势
                    </h4>
                    <div className={styles.historyChart}>
                      {healthHistory.map((record, index) => {
                        const height = `${record.healthScore}%`;
                        const time = new Date(record.timestamp);
                        const label = `${time.getHours()}:00`;

                        return (
                          <div key={index} className={styles.chartBar} title={`${label}: ${record.healthScore}分`}>
                            <div
                              className={`${styles.barFill} ${getStatusClass(record.status)}`}
                              style={{ height }}
                            />
                            {index % 4 === 0 && <div className={styles.barLabel}>{label}</div>}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>

          {filteredResults.length === 0 && (
            <div className={styles.emptyState}>
              <i data-lucide="search" />
              <h3>没有找到匹配的服务器</h3>
              <p>尝试调整筛选条件</p>
            </div>
          )}
        </>
      )}
    </div>
  );
}
