import { useQuery } from '@tanstack/react-query';
import { useState, useMemo } from 'react';
import { serverApi } from '../../../shared/api/serverApi';
import { QUERY_KEYS } from '../../../shared/constants';
import { Button, Loading } from '../../../shared/components';
import type { Server, ServerMetrics } from '../../../shared/types';
import styles from './PerformanceMetrics.module.css';

/**
 * 时间范围类型
 */
type TimeRange = '1h' | '6h' | '24h' | '7d';

/**
 * 服务器性能数据
 */
interface ServerPerformance {
  server: Server;
  metrics: ServerMetrics;
  score: number; // 性能评分 0-100
}

/**
 * PerformanceMetrics 组件
 *
 * 性能指标仪表板 - 展示所有服务器的性能概览和对比
 */
export function PerformanceMetrics(): React.JSX.Element {
  const [timeRange, setTimeRange] = useState<TimeRange>('24h');
  const [sortBy, setSortBy] = useState<'name' | 'score' | 'cpu' | 'memory' | 'disk'>('score');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');

  // 获取所有服务器
  const { data: serversData, isLoading: serversLoading } = useQuery({
    queryKey: [QUERY_KEYS.SERVERS],
    queryFn: () => serverApi.getServers({ page: 0, size: 100 }),
  });

  // 获取每个服务器的最新指标（模拟数据 - 实际应从 API 批量获取）
  const { data: performanceData, isLoading: metricsLoading } = useQuery({
    queryKey: [QUERY_KEYS.MONITORING, 'performance', timeRange],
    queryFn: async (): Promise<ServerPerformance[]> => {
      const servers = serversData?.content ?? [];
      // 模拟性能数据
      return servers.map((server) => {
        const cpuUsage = Math.random() * 100;
        const memoryUsage = Math.random() * 100;
        const diskUsage = Math.random() * 100;
        const networkIn = Math.random() * 1000;
        const networkOut = Math.random() * 1000;

        // 计算性能评分（使用率越低分数越高）
        const score = Math.round(
          100 -
            (cpuUsage * 0.3 + memoryUsage * 0.3 + diskUsage * 0.2 + Math.min((networkIn + networkOut) / 20, 100) * 0.2)
        );

        return {
          server,
          metrics: {
            id: server.id,
            serverId: server.id,
            cpuUsage,
            memoryUsage,
            diskUsage,
            networkIn,
            networkOut,
            timestamp: new Date().toISOString(),
            serverName: server.name,
            status: 'ONLINE' as const,
            createdAt: new Date().toISOString(),
          },
          score,
        };
      });
    },
    enabled: !!serversData,
  });

  // 排序后的性能数据
  const sortedData = useMemo(() => {
    if (!performanceData) return [];

    return [...performanceData].sort((a, b) => {
      let compareValue = 0;

      switch (sortBy) {
        case 'name':
          compareValue = a.server.name.localeCompare(b.server.name);
          break;
        case 'score':
          compareValue = a.score - b.score;
          break;
        case 'cpu':
          compareValue = a.metrics.cpuUsage - b.metrics.cpuUsage;
          break;
        case 'memory':
          compareValue = a.metrics.memoryUsage - b.metrics.memoryUsage;
          break;
        case 'disk':
          compareValue = a.metrics.diskUsage - b.metrics.diskUsage;
          break;
      }

      return sortOrder === 'asc' ? compareValue : -compareValue;
    });
  }, [performanceData, sortBy, sortOrder]);

  // 计算统计数据
  const stats = useMemo(() => {
    if (!performanceData || performanceData.length === 0) {
      return {
        avgScore: 0,
        avgCpu: 0,
        avgMemory: 0,
        avgDisk: 0,
        healthyServers: 0,
        warningServers: 0,
        criticalServers: 0,
      };
    }

    const total = performanceData.length;
    const avgScore = performanceData.reduce((sum, item) => sum + item.score, 0) / total;
    const avgCpu = performanceData.reduce((sum, item) => sum + item.metrics.cpuUsage, 0) / total;
    const avgMemory = performanceData.reduce((sum, item) => sum + item.metrics.memoryUsage, 0) / total;
    const avgDisk = performanceData.reduce((sum, item) => sum + item.metrics.diskUsage, 0) / total;

    const healthyServers = performanceData.filter((item) => item.score >= 70).length;
    const warningServers = performanceData.filter((item) => item.score >= 40 && item.score < 70).length;
    const criticalServers = performanceData.filter((item) => item.score < 40).length;

    return {
      avgScore: Math.round(avgScore),
      avgCpu: avgCpu.toFixed(1),
      avgMemory: avgMemory.toFixed(1),
      avgDisk: avgDisk.toFixed(1),
      healthyServers,
      warningServers,
      criticalServers,
    };
  }, [performanceData]);

  // 切换排序
  const handleSort = (field: typeof sortBy): void => {
    if (sortBy === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(field);
      setSortOrder('desc');
    }
  };

  // 获取性能评分颜色类
  const getScoreClass = (score: number): string => {
    if (score >= 70) return styles.scoreHigh ?? '';
    if (score >= 40) return styles.scoreMedium ?? '';
    return styles.scoreLow ?? '';
  };

  // 获取使用率颜色类
  const getUsageClass = (usage: number): string => {
    if (usage >= 90) return styles.usageCritical ?? '';
    if (usage >= 70) return styles.usageWarning ?? '';
    return styles.usageNormal ?? '';
  };

  // 格式化网络流量
  const formatNetwork = (bytes: number): string => {
    if (bytes < 1024) return `${bytes.toFixed(1)} MB/s`;
    return `${(bytes / 1024).toFixed(2)} GB/s`;
  };

  const isLoading = serversLoading || metricsLoading;

  if (isLoading) {
    return <Loading text="加载性能数据..." fullScreen />;
  }

  return (
    <div className={styles.container}>
      {/* 页面头部 */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleGroup}>
            <h1 className={styles.title}>性能指标仪表板</h1>
            <p className={styles.subtitle}>实时监控所有服务器的性能状态</p>
          </div>
          <div className={styles.headerActions}>
            <select
              className={styles.timeSelect}
              value={timeRange}
              onChange={(e) => setTimeRange(e.target.value as TimeRange)}
            >
              <option value="1h">最近 1 小时</option>
              <option value="6h">最近 6 小时</option>
              <option value="24h">最近 24 小时</option>
              <option value="7d">最近 7 天</option>
            </select>
          </div>
        </div>
      </header>

      {/* 统计卡片 */}
      <div className={styles.statsGrid}>
        <div className={styles.statCard}>
          <div className={styles.statIcon}>
            <i data-lucide="gauge" />
          </div>
          <div className={styles.statContent}>
            <div className={styles.statLabel}>平均性能评分</div>
            <div className={`${styles.statValue} ${getScoreClass(stats.avgScore)}`}>{stats.avgScore}</div>
          </div>
        </div>

        <div className={styles.statCard}>
          <div className={styles.statIcon}>
            <i data-lucide="cpu" />
          </div>
          <div className={styles.statContent}>
            <div className={styles.statLabel}>平均 CPU 使用率</div>
            <div className={styles.statValue}>{stats.avgCpu}%</div>
          </div>
        </div>

        <div className={styles.statCard}>
          <div className={styles.statIcon}>
            <i data-lucide="memory-stick" />
          </div>
          <div className={styles.statContent}>
            <div className={styles.statLabel}>平均内存使用率</div>
            <div className={styles.statValue}>{stats.avgMemory}%</div>
          </div>
        </div>

        <div className={styles.statCard}>
          <div className={styles.statIcon}>
            <i data-lucide="hard-drive" />
          </div>
          <div className={styles.statContent}>
            <div className={styles.statLabel}>平均磁盘使用率</div>
            <div className={styles.statValue}>{stats.avgDisk}%</div>
          </div>
        </div>
      </div>

      {/* 服务器健康状态 */}
      <div className={styles.healthSection}>
        <h2 className={styles.sectionTitle}>服务器健康状态</h2>
        <div className={styles.healthGrid}>
          <div className={styles.healthCard}>
            <div className={`${styles.healthIcon} ${styles.healthIconGood}`}>
              <i data-lucide="check-circle" />
            </div>
            <div className={styles.healthContent}>
              <div className={styles.healthLabel}>健康</div>
              <div className={styles.healthValue}>{stats.healthyServers}</div>
              <div className={styles.healthDesc}>性能评分 ≥ 70</div>
            </div>
          </div>

          <div className={styles.healthCard}>
            <div className={`${styles.healthIcon} ${styles.healthIconWarning}`}>
              <i data-lucide="alert-triangle" />
            </div>
            <div className={styles.healthContent}>
              <div className={styles.healthLabel}>警告</div>
              <div className={styles.healthValue}>{stats.warningServers}</div>
              <div className={styles.healthDesc}>性能评分 40-69</div>
            </div>
          </div>

          <div className={styles.healthCard}>
            <div className={`${styles.healthIcon} ${styles.healthIconCritical}`}>
              <i data-lucide="x-circle" />
            </div>
            <div className={styles.healthContent}>
              <div className={styles.healthLabel}>严重</div>
              <div className={styles.healthValue}>{stats.criticalServers}</div>
              <div className={styles.healthDesc}>性能评分 &lt; 40</div>
            </div>
          </div>
        </div>
      </div>

      {/* 服务器性能列表 */}
      <div className={styles.performanceSection}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>服务器性能详情</h2>
          <div className={styles.sortControls}>
            <span className={styles.sortLabel}>排序：</span>
            <Button
              size="sm"
              variant={sortBy === 'score' ? 'primary' : 'secondary'}
              onClick={() => handleSort('score')}
            >
              性能评分
              {sortBy === 'score' && <i data-lucide={sortOrder === 'asc' ? 'arrow-up' : 'arrow-down'} />}
            </Button>
            <Button size="sm" variant={sortBy === 'cpu' ? 'primary' : 'secondary'} onClick={() => handleSort('cpu')}>
              CPU
              {sortBy === 'cpu' && <i data-lucide={sortOrder === 'asc' ? 'arrow-up' : 'arrow-down'} />}
            </Button>
            <Button
              size="sm"
              variant={sortBy === 'memory' ? 'primary' : 'secondary'}
              onClick={() => handleSort('memory')}
            >
              内存
              {sortBy === 'memory' && <i data-lucide={sortOrder === 'asc' ? 'arrow-up' : 'arrow-down'} />}
            </Button>
            <Button size="sm" variant={sortBy === 'disk' ? 'primary' : 'secondary'} onClick={() => handleSort('disk')}>
              磁盘
              {sortBy === 'disk' && <i data-lucide={sortOrder === 'asc' ? 'arrow-up' : 'arrow-down'} />}
            </Button>
          </div>
        </div>

        <div className={styles.performanceList}>
          {sortedData.map((item) => (
            <div key={item.server.id} className={styles.performanceCard}>
              <div className={styles.performanceHeader}>
                <div className={styles.serverInfo}>
                  <h3 className={styles.serverName}>{item.server.name}</h3>
                  <p className={styles.serverHost}>
                    {item.server.host}:{item.server.port}
                  </p>
                </div>
                <div className={`${styles.performanceScore} ${getScoreClass(item.score)}`}>
                  <div className={styles.scoreValue}>{item.score}</div>
                  <div className={styles.scoreLabel}>性能评分</div>
                </div>
              </div>

              <div className={styles.metricsGrid}>
                <div className={styles.metricItem}>
                  <div className={styles.metricHeader}>
                    <i data-lucide="cpu" />
                    <span className={styles.metricLabel}>CPU</span>
                  </div>
                  <div className={styles.metricBar}>
                    <div
                      className={`${styles.metricFill} ${getUsageClass(item.metrics.cpuUsage)}`}
                      style={{ width: `${item.metrics.cpuUsage}%` }}
                    />
                  </div>
                  <div className={styles.metricValue}>{item.metrics.cpuUsage.toFixed(1)}%</div>
                </div>

                <div className={styles.metricItem}>
                  <div className={styles.metricHeader}>
                    <i data-lucide="memory-stick" />
                    <span className={styles.metricLabel}>内存</span>
                  </div>
                  <div className={styles.metricBar}>
                    <div
                      className={`${styles.metricFill} ${getUsageClass(item.metrics.memoryUsage)}`}
                      style={{ width: `${item.metrics.memoryUsage}%` }}
                    />
                  </div>
                  <div className={styles.metricValue}>{item.metrics.memoryUsage.toFixed(1)}%</div>
                </div>

                <div className={styles.metricItem}>
                  <div className={styles.metricHeader}>
                    <i data-lucide="hard-drive" />
                    <span className={styles.metricLabel}>磁盘</span>
                  </div>
                  <div className={styles.metricBar}>
                    <div
                      className={`${styles.metricFill} ${getUsageClass(item.metrics.diskUsage)}`}
                      style={{ width: `${item.metrics.diskUsage}%` }}
                    />
                  </div>
                  <div className={styles.metricValue}>{item.metrics.diskUsage.toFixed(1)}%</div>
                </div>

                <div className={styles.metricItem}>
                  <div className={styles.metricHeader}>
                    <i data-lucide="network" />
                    <span className={styles.metricLabel}>网络</span>
                  </div>
                  <div className={styles.networkInfo}>
                    <div className={styles.networkFlow}>
                      <i data-lucide="arrow-down" />
                      {formatNetwork(item.metrics.networkIn ?? 0)}
                    </div>
                    <div className={styles.networkFlow}>
                      <i data-lucide="arrow-up" />
                      {formatNetwork(item.metrics.networkOut ?? 0)}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        {sortedData.length === 0 && (
          <div className={styles.emptyState}>
            <i data-lucide="inbox" />
            <p>暂无服务器性能数据</p>
          </div>
        )}
      </div>
    </div>
  );
}
