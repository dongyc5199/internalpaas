import { useQuery } from '@tanstack/react-query';
import { useState, useMemo } from 'react';
import { serverApi } from '../../../shared/api/serverApi';
import { QUERY_KEYS } from '../../../shared/constants';
import { Button, Loading } from '../../../shared/components';
import type { Server, ServerMetrics } from '../../../shared/types';
import styles from './ServerComparison.module.css';

/**
 * 服务器对比数据
 */
interface ServerComparisonData {
  server: Server;
  metrics: ServerMetrics;
  rank: {
    cpu: number;
    memory: number;
    disk: number;
    overall: number;
  };
}

/**
 * 对比指标类型
 */
type ComparisonMetric = 'cpu' | 'memory' | 'disk' | 'network' | 'all';

/**
 * ServerComparison 组件
 *
 * 服务器对比视图 - 并排对比多个服务器的性能指标
 */
export function ServerComparison(): React.JSX.Element {
  const [selectedServerIds, setSelectedServerIds] = useState<number[]>([]);
  const [comparisonMetric, setComparisonMetric] = useState<ComparisonMetric>('all');
  const [sortBy, setSortBy] = useState<'name' | 'cpu' | 'memory' | 'disk' | 'overall'>('overall');

  // 获取所有服务器
  const { data: serversData, isLoading: serversLoading } = useQuery({
    queryKey: [QUERY_KEYS.SERVERS],
    queryFn: () => serverApi.getServers({ page: 0, size: 100 }),
  });

  const servers = (serversData?.content ?? []) as Server[];

  // 获取对比数据（模拟数据）
  const { data: comparisonData, isLoading: comparisonLoading } = useQuery({
    queryKey: [QUERY_KEYS.MONITORING, 'comparison', selectedServerIds],
    queryFn: async (): Promise<ServerComparisonData[]> => {
      const selectedServers = servers.filter((s) => selectedServerIds.includes(s.id));

      // 模拟性能数据
      const dataWithMetrics = selectedServers.map((server) => {
        const cpuUsage = Math.random() * 100;
        const memoryUsage = Math.random() * 100;
        const diskUsage = Math.random() * 100;
        const networkIn = Math.random() * 1000;
        const networkOut = Math.random() * 1000;

        return {
          server,
          metrics: {
            cpuUsage,
            memoryUsage,
            diskUsage,
            networkIn,
            networkOut,
            timestamp: new Date().toISOString(),
          } as ServerMetrics,
          rank: {
            cpu: 0,
            memory: 0,
            disk: 0,
            overall: 0,
          },
        };
      });

      // 计算排名
      const cpuRanked = [...dataWithMetrics].sort((a, b) => a.metrics.cpuUsage - b.metrics.cpuUsage);
      const memoryRanked = [...dataWithMetrics].sort((a, b) => a.metrics.memoryUsage - b.metrics.memoryUsage);
      const diskRanked = [...dataWithMetrics].sort((a, b) => a.metrics.diskUsage - b.metrics.diskUsage);

      dataWithMetrics.forEach((item) => {
        item.rank.cpu = cpuRanked.indexOf(item) + 1;
        item.rank.memory = memoryRanked.indexOf(item) + 1;
        item.rank.disk = diskRanked.indexOf(item) + 1;
        item.rank.overall = Math.round((item.rank.cpu + item.rank.memory + item.rank.disk) / 3);
      });

      return dataWithMetrics;
    },
    enabled: selectedServerIds.length > 0,
  });

  // 排序后的对比数据
  const sortedData = useMemo(() => {
    if (!comparisonData) return [];

    return [...comparisonData].sort((a, b) => {
      switch (sortBy) {
        case 'name':
          return a.server.name.localeCompare(b.server.name);
        case 'cpu':
          return a.metrics.cpuUsage - b.metrics.cpuUsage;
        case 'memory':
          return a.metrics.memoryUsage - b.metrics.memoryUsage;
        case 'disk':
          return a.metrics.diskUsage - b.metrics.diskUsage;
        case 'overall':
          return a.rank.overall - b.rank.overall;
        default:
          return 0;
      }
    });
  }, [comparisonData, sortBy]);

  // 计算对比统计
  const comparisonStats = useMemo(() => {
    if (!comparisonData || comparisonData.length === 0) return null;

    const cpuValues = comparisonData.map((d) => d.metrics.cpuUsage);
    const memoryValues = comparisonData.map((d) => d.metrics.memoryUsage);
    const diskValues = comparisonData.map((d) => d.metrics.diskUsage);

    return {
      cpu: {
        max: Math.max(...cpuValues),
        min: Math.min(...cpuValues),
        avg: cpuValues.reduce((sum, v) => sum + v, 0) / cpuValues.length,
        diff: Math.max(...cpuValues) - Math.min(...cpuValues),
      },
      memory: {
        max: Math.max(...memoryValues),
        min: Math.min(...memoryValues),
        avg: memoryValues.reduce((sum, v) => sum + v, 0) / memoryValues.length,
        diff: Math.max(...memoryValues) - Math.min(...memoryValues),
      },
      disk: {
        max: Math.max(...diskValues),
        min: Math.min(...diskValues),
        avg: diskValues.reduce((sum, v) => sum + v, 0) / diskValues.length,
        diff: Math.max(...diskValues) - Math.min(...diskValues),
      },
    };
  }, [comparisonData]);

  // 切换服务器选择
  const toggleServerSelection = (serverId: number): void => {
    if (selectedServerIds.includes(serverId)) {
      setSelectedServerIds(selectedServerIds.filter((id) => id !== serverId));
    } else {
      if (selectedServerIds.length < 6) {
        // 最多对比 6 个服务器
        setSelectedServerIds([...selectedServerIds, serverId]);
      }
    }
  };

  // 获取使用率颜色类
  const getUsageClass = (usage: number): string => {
    if (usage >= 90) return styles.usageCritical ?? '';
    if (usage >= 70) return styles.usageWarning ?? '';
    return styles.usageNormal ?? '';
  };

  // 获取排名徽章颜色
  const getRankBadgeClass = (rank: number): string => {
    if (rank === 1) return styles.rankFirst ?? '';
    if (rank === 2) return styles.rankSecond ?? '';
    if (rank === 3) return styles.rankThird ?? '';
    return styles.rankOther ?? '';
  };

  // 格式化网络流量
  const formatNetwork = (bytes: number): string => {
    if (bytes < 1024) return `${bytes.toFixed(1)} MB/s`;
    return `${(bytes / 1024).toFixed(2)} GB/s`;
  };

  const isLoading = serversLoading || comparisonLoading;

  return (
    <div className={styles.container}>
      {/* 页面头部 */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleGroup}>
            <h1 className={styles.title}>服务器对比视图</h1>
            <p className={styles.subtitle}>并排对比多个服务器的性能指标（最多 6 个）</p>
          </div>
        </div>
      </header>

      {/* 服务器选择区域 */}
      <div className={styles.selectionSection}>
        <h2 className={styles.sectionTitle}>
          选择服务器 ({selectedServerIds.length}/6)
        </h2>
        <div className={styles.serverGrid}>
          {servers.map((server) => (
            <div
              key={server.id}
              className={`${styles.serverCard} ${
                selectedServerIds.includes(server.id) ? styles.serverCardSelected : ''
              } ${selectedServerIds.length >= 6 && !selectedServerIds.includes(server.id) ? styles.serverCardDisabled : ''}`}
              onClick={() => toggleServerSelection(server.id)}
            >
              <div className={styles.serverCardHeader}>
                <input
                  type="checkbox"
                  checked={selectedServerIds.includes(server.id)}
                  onChange={() => {}}
                  className={styles.checkbox}
                />
                <h3 className={styles.serverCardTitle}>{server.name}</h3>
              </div>
              <p className={styles.serverCardHost}>
                {server.host}:{server.port}
              </p>
              <div className={styles.serverCardStatus}>
                <span className={`${styles.statusBadge} ${styles[`status${server.status}`]}`}>
                  {server.status}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {selectedServerIds.length === 0 ? (
        <div className={styles.emptyState}>
          <i data-lucide="server" />
          <h3>请选择服务器</h3>
          <p>选择至少 2 个服务器以进行性能对比</p>
        </div>
      ) : selectedServerIds.length === 1 ? (
        <div className={styles.emptyState}>
          <i data-lucide="git-compare" />
          <h3>需要更多服务器</h3>
          <p>请再选择至少 1 个服务器以进行对比</p>
        </div>
      ) : isLoading ? (
        <Loading text="加载对比数据..." />
      ) : (
        <>
          {/* 控制栏 */}
          <div className={styles.controls}>
            <div className={styles.controlGroup}>
              <label className={styles.controlLabel}>对比指标</label>
              <div className={styles.buttonGroup}>
                <Button
                  size="sm"
                  variant={comparisonMetric === 'all' ? 'primary' : 'secondary'}
                  onClick={() => setComparisonMetric('all')}
                >
                  全部
                </Button>
                <Button
                  size="sm"
                  variant={comparisonMetric === 'cpu' ? 'primary' : 'secondary'}
                  onClick={() => setComparisonMetric('cpu')}
                >
                  <i data-lucide="cpu" />
                  CPU
                </Button>
                <Button
                  size="sm"
                  variant={comparisonMetric === 'memory' ? 'primary' : 'secondary'}
                  onClick={() => setComparisonMetric('memory')}
                >
                  <i data-lucide="memory-stick" />
                  内存
                </Button>
                <Button
                  size="sm"
                  variant={comparisonMetric === 'disk' ? 'primary' : 'secondary'}
                  onClick={() => setComparisonMetric('disk')}
                >
                  <i data-lucide="hard-drive" />
                  磁盘
                </Button>
                <Button
                  size="sm"
                  variant={comparisonMetric === 'network' ? 'primary' : 'secondary'}
                  onClick={() => setComparisonMetric('network')}
                >
                  <i data-lucide="network" />
                  网络
                </Button>
              </div>
            </div>

            <div className={styles.controlGroup}>
              <label className={styles.controlLabel}>排序方式</label>
              <div className={styles.buttonGroup}>
                <Button
                  size="sm"
                  variant={sortBy === 'overall' ? 'primary' : 'secondary'}
                  onClick={() => setSortBy('overall')}
                >
                  综合排名
                </Button>
                <Button
                  size="sm"
                  variant={sortBy === 'cpu' ? 'primary' : 'secondary'}
                  onClick={() => setSortBy('cpu')}
                >
                  CPU
                </Button>
                <Button
                  size="sm"
                  variant={sortBy === 'memory' ? 'primary' : 'secondary'}
                  onClick={() => setSortBy('memory')}
                >
                  内存
                </Button>
                <Button
                  size="sm"
                  variant={sortBy === 'disk' ? 'primary' : 'secondary'}
                  onClick={() => setSortBy('disk')}
                >
                  磁盘
                </Button>
              </div>
            </div>
          </div>

          {/* 对比统计 */}
          {comparisonStats && (
            <div className={styles.statsSection}>
              <h2 className={styles.sectionTitle}>对比统计</h2>
              <div className={styles.statsGrid}>
                <div className={styles.statCard}>
                  <h3 className={styles.statCardTitle}>
                    <i data-lucide="cpu" />
                    CPU 使用率
                  </h3>
                  <div className={styles.statMetrics}>
                    <div className={styles.statMetricItem}>
                      <span className={styles.statMetricLabel}>最高</span>
                      <span className={styles.statMetricValue}>{comparisonStats.cpu.max.toFixed(1)}%</span>
                    </div>
                    <div className={styles.statMetricItem}>
                      <span className={styles.statMetricLabel}>最低</span>
                      <span className={styles.statMetricValue}>{comparisonStats.cpu.min.toFixed(1)}%</span>
                    </div>
                    <div className={styles.statMetricItem}>
                      <span className={styles.statMetricLabel}>平均</span>
                      <span className={styles.statMetricValue}>{comparisonStats.cpu.avg.toFixed(1)}%</span>
                    </div>
                    <div className={styles.statMetricItem}>
                      <span className={styles.statMetricLabel}>差异</span>
                      <span className={`${styles.statMetricValue} ${styles.statDiff}`}>
                        {comparisonStats.cpu.diff.toFixed(1)}%
                      </span>
                    </div>
                  </div>
                </div>

                <div className={styles.statCard}>
                  <h3 className={styles.statCardTitle}>
                    <i data-lucide="memory-stick" />
                    内存使用率
                  </h3>
                  <div className={styles.statMetrics}>
                    <div className={styles.statMetricItem}>
                      <span className={styles.statMetricLabel}>最高</span>
                      <span className={styles.statMetricValue}>{comparisonStats.memory.max.toFixed(1)}%</span>
                    </div>
                    <div className={styles.statMetricItem}>
                      <span className={styles.statMetricLabel}>最低</span>
                      <span className={styles.statMetricValue}>{comparisonStats.memory.min.toFixed(1)}%</span>
                    </div>
                    <div className={styles.statMetricItem}>
                      <span className={styles.statMetricLabel}>平均</span>
                      <span className={styles.statMetricValue}>{comparisonStats.memory.avg.toFixed(1)}%</span>
                    </div>
                    <div className={styles.statMetricItem}>
                      <span className={styles.statMetricLabel}>差异</span>
                      <span className={`${styles.statMetricValue} ${styles.statDiff}`}>
                        {comparisonStats.memory.diff.toFixed(1)}%
                      </span>
                    </div>
                  </div>
                </div>

                <div className={styles.statCard}>
                  <h3 className={styles.statCardTitle}>
                    <i data-lucide="hard-drive" />
                    磁盘使用率
                  </h3>
                  <div className={styles.statMetrics}>
                    <div className={styles.statMetricItem}>
                      <span className={styles.statMetricLabel}>最高</span>
                      <span className={styles.statMetricValue}>{comparisonStats.disk.max.toFixed(1)}%</span>
                    </div>
                    <div className={styles.statMetricItem}>
                      <span className={styles.statMetricLabel}>最低</span>
                      <span className={styles.statMetricValue}>{comparisonStats.disk.min.toFixed(1)}%</span>
                    </div>
                    <div className={styles.statMetricItem}>
                      <span className={styles.statMetricLabel}>平均</span>
                      <span className={styles.statMetricValue}>{comparisonStats.disk.avg.toFixed(1)}%</span>
                    </div>
                    <div className={styles.statMetricItem}>
                      <span className={styles.statMetricLabel}>差异</span>
                      <span className={`${styles.statMetricValue} ${styles.statDiff}`}>
                        {comparisonStats.disk.diff.toFixed(1)}%
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* 对比视图 */}
          <div className={styles.comparisonSection}>
            <h2 className={styles.sectionTitle}>详细对比</h2>
            <div className={styles.comparisonGrid}>
              {sortedData.map((item, index) => (
                <div key={item.server.id} className={styles.comparisonCard}>
                  <div className={styles.comparisonHeader}>
                    <div className={styles.rankBadge}>
                      <span className={getRankBadgeClass(index + 1)}>#{index + 1}</span>
                    </div>
                    <div className={styles.serverInfo}>
                      <h3 className={styles.comparisonServerName}>{item.server.name}</h3>
                      <p className={styles.comparisonServerHost}>
                        {item.server.host}:{item.server.port}
                      </p>
                    </div>
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => toggleServerSelection(item.server.id)}
                      className={styles.removeBtn}
                    >
                      <i data-lucide="x" />
                    </Button>
                  </div>

                  {(comparisonMetric === 'all' || comparisonMetric === 'cpu') && (
                    <div className={styles.metricSection}>
                      <div className={styles.metricHeader}>
                        <i data-lucide="cpu" />
                        <span className={styles.metricLabel}>CPU</span>
                        <span className={`${styles.rankTag} ${getRankBadgeClass(item.rank.cpu)}`}>
                          第 {item.rank.cpu} 名
                        </span>
                      </div>
                      <div className={styles.metricBar}>
                        <div
                          className={`${styles.metricFill} ${getUsageClass(item.metrics.cpuUsage)}`}
                          style={{ width: `${item.metrics.cpuUsage}%` }}
                        />
                      </div>
                      <div className={styles.metricValue}>{item.metrics.cpuUsage.toFixed(1)}%</div>
                    </div>
                  )}

                  {(comparisonMetric === 'all' || comparisonMetric === 'memory') && (
                    <div className={styles.metricSection}>
                      <div className={styles.metricHeader}>
                        <i data-lucide="memory-stick" />
                        <span className={styles.metricLabel}>内存</span>
                        <span className={`${styles.rankTag} ${getRankBadgeClass(item.rank.memory)}`}>
                          第 {item.rank.memory} 名
                        </span>
                      </div>
                      <div className={styles.metricBar}>
                        <div
                          className={`${styles.metricFill} ${getUsageClass(item.metrics.memoryUsage)}`}
                          style={{ width: `${item.metrics.memoryUsage}%` }}
                        />
                      </div>
                      <div className={styles.metricValue}>{item.metrics.memoryUsage.toFixed(1)}%</div>
                    </div>
                  )}

                  {(comparisonMetric === 'all' || comparisonMetric === 'disk') && (
                    <div className={styles.metricSection}>
                      <div className={styles.metricHeader}>
                        <i data-lucide="hard-drive" />
                        <span className={styles.metricLabel}>磁盘</span>
                        <span className={`${styles.rankTag} ${getRankBadgeClass(item.rank.disk)}`}>
                          第 {item.rank.disk} 名
                        </span>
                      </div>
                      <div className={styles.metricBar}>
                        <div
                          className={`${styles.metricFill} ${getUsageClass(item.metrics.diskUsage)}`}
                          style={{ width: `${item.metrics.diskUsage}%` }}
                        />
                      </div>
                      <div className={styles.metricValue}>{item.metrics.diskUsage.toFixed(1)}%</div>
                    </div>
                  )}

                  {(comparisonMetric === 'all' || comparisonMetric === 'network') && (
                    <div className={styles.metricSection}>
                      <div className={styles.metricHeader}>
                        <i data-lucide="network" />
                        <span className={styles.metricLabel}>网络</span>
                      </div>
                      <div className={styles.networkMetrics}>
                        <div className={styles.networkFlow}>
                          <i data-lucide="arrow-down" />
                          <span>{formatNetwork(item.metrics.networkIn ?? 0)}</span>
                        </div>
                        <div className={styles.networkFlow}>
                          <i data-lucide="arrow-up" />
                          <span>{formatNetwork(item.metrics.networkOut ?? 0)}</span>
                        </div>
                      </div>
                    </div>
                  )}

                  <div className={styles.overallRank}>
                    <span className={styles.overallRankLabel}>综合排名</span>
                    <span className={`${styles.overallRankValue} ${getRankBadgeClass(item.rank.overall)}`}>
                      第 {item.rank.overall} 名
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
