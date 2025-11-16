import { useQuery } from '@tanstack/react-query';
import { useState, useMemo } from 'react';
import { serverApi } from '../../../shared/api/serverApi';
import { QUERY_KEYS } from '../../../shared/constants';
import { Button, Loading } from '../../../shared/components';
import type { Server } from '../../../shared/types';
import styles from './ResourceTrends.module.css';

/**
 * 时间范围类型
 */
type TimeRange = '24h' | '7d' | '30d';

/**
 * 资源类型
 */
type ResourceType = 'cpu' | 'memory' | 'disk' | 'network';

/**
 * 趋势数据点
 */
interface TrendDataPoint {
  timestamp: string;
  value: number;
  label: string;
}

/**
 * 趋势统计
 */
interface TrendStats {
  current: number;
  avg: number;
  max: number;
  min: number;
  trend: 'up' | 'down' | 'stable'; // 趋势方向
  changePercent: number; // 变化百分比
  peakTime?: string; // 峰值时间
  valleyTime?: string; // 谷值时间
}

/**
 * ResourceTrends 组件
 *
 * 资源使用趋势分析页面 - 深入分析资源使用模式和趋势
 */
export function ResourceTrends(): React.JSX.Element {
  const [selectedServerId, setSelectedServerId] = useState<number | null>(null);
  const [timeRange, setTimeRange] = useState<TimeRange>('24h');
  const [resourceType, setResourceType] = useState<ResourceType>('cpu');
  const [showPrediction, setShowPrediction] = useState(false);

  // 获取所有服务器
  const { data: serversData, isLoading: serversLoading } = useQuery({
    queryKey: [QUERY_KEYS.SERVERS],
    queryFn: () => serverApi.getServers({ page: 0, size: 100 }),
  });

  const servers = (serversData?.content ?? []) as Server[];

  // 获取趋势数据（模拟数据）
  const { data: trendData, isLoading: trendLoading } = useQuery({
    queryKey: [QUERY_KEYS.MONITORING, 'trends', selectedServerId, timeRange, resourceType],
    queryFn: async (): Promise<TrendDataPoint[]> => {
      // 模拟趋势数据
      const dataPoints: TrendDataPoint[] = [];
      const now = Date.now();
      let interval: number;
      let count: number;

      switch (timeRange) {
        case '24h':
          interval = 60 * 60 * 1000; // 1小时
          count = 24;
          break;
        case '7d':
          interval = 24 * 60 * 60 * 1000; // 1天
          count = 7;
          break;
        case '30d':
          interval = 24 * 60 * 60 * 1000; // 1天
          count = 30;
          break;
      }

      for (let i = count - 1; i >= 0; i--) {
        const timestamp = new Date(now - i * interval);
        const baseValue = 40 + Math.random() * 30;
        const variation = Math.sin((i / count) * Math.PI * 2) * 15;
        const value = Math.max(0, Math.min(100, baseValue + variation));

        dataPoints.push({
          timestamp: timestamp.toISOString(),
          value,
          label: formatTimestamp(timestamp, timeRange),
        });
      }

      return dataPoints;
    },
    enabled: selectedServerId !== null,
  });

  // 计算趋势统计
  const trendStats = useMemo((): TrendStats | null => {
    if (!trendData || trendData.length === 0) return null;

    const values = trendData.map((d) => d.value);
    const current = values[values.length - 1] ?? 0;
    const avg = values.reduce((sum, v) => sum + v, 0) / values.length;
    const max = Math.max(...values);
    const min = Math.min(...values);

    const maxIndex = values.indexOf(max);
    const minIndex = values.indexOf(min);
    const peakTime = trendData[maxIndex]?.timestamp ?? '';
    const valleyTime = trendData[minIndex]?.timestamp ?? '';

    // 计算趋势（比较最近 1/3 和之前 1/3 的平均值）
    const thirdLength = Math.floor(values.length / 3);
    const recentAvg = values.slice(-thirdLength).reduce((sum, v) => sum + v, 0) / thirdLength;
    const previousAvg = values.slice(0, thirdLength).reduce((sum, v) => sum + v, 0) / thirdLength;
    const changePercent = ((recentAvg - previousAvg) / previousAvg) * 100;

    let trend: 'up' | 'down' | 'stable';
    if (Math.abs(changePercent) < 5) {
      trend = 'stable';
    } else if (changePercent > 0) {
      trend = 'up';
    } else {
      trend = 'down';
    }

    return {
      current,
      avg,
      max,
      min,
      trend,
      changePercent,
      peakTime,
      valleyTime,
    };
  }, [trendData]);

  // 生成预测数据（简单线性预测）
  const predictionData = useMemo((): TrendDataPoint[] => {
    if (!showPrediction || !trendData || trendData.length < 2) return [];

    const lastPoint = trendData[trendData.length - 1];
    const secondLastPoint = trendData[trendData.length - 2];

    if (!lastPoint || !secondLastPoint) return [];

    const trend = lastPoint.value - secondLastPoint.value;

    const predictions: TrendDataPoint[] = [];
    const interval = new Date(lastPoint.timestamp).getTime() - new Date(secondLastPoint.timestamp).getTime();

    for (let i = 1; i <= 5; i++) {
      const timestamp = new Date(new Date(lastPoint.timestamp).getTime() + i * interval);
      const predictedValue = Math.max(0, Math.min(100, lastPoint.value + trend * i));

      predictions.push({
        timestamp: timestamp.toISOString(),
        value: predictedValue,
        label: formatTimestamp(timestamp, timeRange),
      });
    }

    return predictions;
  }, [trendData, showPrediction, timeRange]);

  // 格式化时间戳
  function formatTimestamp(date: Date, range: TimeRange): string {
    if (range === '24h') {
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    }
    return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
  }

  // 格式化时间显示
  const formatTime = (timestamp?: string): string => {
    if (!timestamp) return '-';
    const date = new Date(timestamp);
    return date.toLocaleString('zh-CN');
  };

  // 获取趋势图标
  const getTrendIcon = (trend: 'up' | 'down' | 'stable'): string => {
    switch (trend) {
      case 'up':
        return 'trending-up';
      case 'down':
        return 'trending-down';
      default:
        return 'minus';
    }
  };

  // 获取趋势颜色类
  const getTrendClass = (trend: 'up' | 'down' | 'stable'): string => {
    switch (trend) {
      case 'up':
        return styles.trendUp ?? '';
      case 'down':
        return styles.trendDown ?? '';
      default:
        return styles.trendStable ?? '';
    }
  };

  // 渲染趋势图表
  const renderChart = (): React.JSX.Element => {
    if (!trendData || trendData.length === 0) {
      return (
        <div className={styles.emptyChart}>
          <i data-lucide="line-chart" />
          <p>暂无趋势数据</p>
        </div>
      );
    }

    const allData = [...trendData, ...predictionData];
    const maxValue = Math.max(...allData.map((d) => d.value));
    const chartWidth = 1000;
    const chartHeight = 400;
    const padding = { top: 20, right: 40, bottom: 60, left: 60 };

    const xScale = (chartWidth - padding.left - padding.right) / (allData.length - 1);
    const yScale = (chartHeight - padding.top - padding.bottom) / maxValue;

    // 生成路径数据
    const pathData = trendData
      .map((d, i) => {
        const x = padding.left + i * xScale;
        const y = chartHeight - padding.bottom - d.value * yScale;
        return `${i === 0 ? 'M' : 'L'} ${x.toFixed(2)},${y.toFixed(2)}`;
      })
      .join(' ');

    // 生成区域填充数据
    const areaData = `${pathData} L ${padding.left + (trendData.length - 1) * xScale},${chartHeight - padding.bottom} L ${padding.left},${chartHeight - padding.bottom} Z`;

    // 生成预测路径（虚线）
    const lastTrendPoint = trendData[trendData.length - 1];
    const predictionPathData =
      predictionData.length > 0 && lastTrendPoint
        ? [
            `M ${padding.left + (trendData.length - 1) * xScale},${chartHeight - padding.bottom - lastTrendPoint.value * yScale}`,
            ...predictionData.map((d, i) => {
              const x = padding.left + (trendData.length + i) * xScale;
              const y = chartHeight - padding.bottom - d.value * yScale;
              return `L ${x.toFixed(2)},${y.toFixed(2)}`;
            }),
          ].join(' ')
        : '';

    return (
      <svg viewBox={`0 0 ${chartWidth} ${chartHeight}`} className={styles.chart}>
        {/* 网格线 */}
        {[0, 25, 50, 75, 100].map((value) => {
          const y = chartHeight - padding.bottom - value * yScale;
          return (
            <g key={value}>
              <line
                x1={padding.left}
                y1={y}
                x2={chartWidth - padding.right}
                y2={y}
                stroke="var(--border-color, #e5e7eb)"
                strokeWidth="1"
                strokeDasharray="4 4"
              />
              <text x={padding.left - 10} y={y + 5} textAnchor="end" fill="var(--text-secondary, #6b7280)" fontSize="12">
                {value}%
              </text>
            </g>
          );
        })}

        {/* 区域填充 */}
        <path d={areaData} fill="url(#gradient)" opacity="0.3" />

        {/* 实际数据线 */}
        <path d={pathData} fill="none" stroke="var(--primary-color, #3b82f6)" strokeWidth="3" />

        {/* 预测数据线 */}
        {predictionPathData && (
          <path
            d={predictionPathData}
            fill="none"
            stroke="var(--primary-color, #3b82f6)"
            strokeWidth="3"
            strokeDasharray="8 4"
            opacity="0.6"
          />
        )}

        {/* 数据点 */}
        {trendData.map((d, i) => {
          const x = padding.left + i * xScale;
          const y = chartHeight - padding.bottom - d.value * yScale;
          return (
            <g key={i}>
              <circle cx={x} cy={y} r="5" fill="var(--primary-color, #3b82f6)" />
              <title>
                {d.label}: {d.value.toFixed(1)}%
              </title>
            </g>
          );
        })}

        {/* 预测数据点 */}
        {predictionData.map((d, i) => {
          const x = padding.left + (trendData.length + i) * xScale;
          const y = chartHeight - padding.bottom - d.value * yScale;
          return (
            <g key={`pred-${i}`}>
              <circle cx={x} cy={y} r="4" fill="var(--primary-color, #3b82f6)" opacity="0.6" />
              <title>
                预测 {d.label}: {d.value.toFixed(1)}%
              </title>
            </g>
          );
        })}

        {/* X轴标签 */}
        {trendData
          .filter((_, i) => i % Math.ceil(trendData.length / 10) === 0)
          .map((d, i) => {
            const originalIndex = trendData.indexOf(d);
            const x = padding.left + originalIndex * xScale;
            return (
              <text
                key={`label-${i}`}
                x={x}
                y={chartHeight - padding.bottom + 20}
                textAnchor="middle"
                fill="var(--text-secondary, #6b7280)"
                fontSize="12"
              >
                {d.label}
              </text>
            );
          })}

        {/* 渐变定义 */}
        <defs>
          <linearGradient id="gradient" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="var(--primary-color, #3b82f6)" stopOpacity="0.8" />
            <stop offset="100%" stopColor="var(--primary-color, #3b82f6)" stopOpacity="0" />
          </linearGradient>
        </defs>
      </svg>
    );
  };

  const isLoading = serversLoading || trendLoading;
  const selectedServer = servers.find((s) => s.id === selectedServerId);

  return (
    <div className={styles.container}>
      {/* 页面头部 */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleGroup}>
            <h1 className={styles.title}>资源使用趋势分析</h1>
            <p className={styles.subtitle}>深入分析资源使用模式和未来趋势</p>
          </div>
        </div>
      </header>

      {/* 控制面板 */}
      <div className={styles.controls}>
        <div className={styles.controlGroup}>
          <label className={styles.controlLabel}>选择服务器</label>
          <select
            className={styles.select}
            value={selectedServerId ?? ''}
            onChange={(e) => setSelectedServerId(e.target.value ? Number(e.target.value) : null)}
          >
            <option value="">-- 请选择服务器 --</option>
            {servers.map((server) => (
              <option key={server.id} value={server.id}>
                {server.name} ({server.host})
              </option>
            ))}
          </select>
        </div>

        <div className={styles.controlGroup}>
          <label className={styles.controlLabel}>时间范围</label>
          <div className={styles.buttonGroup}>
            <Button
              size="sm"
              variant={timeRange === '24h' ? 'primary' : 'secondary'}
              onClick={() => setTimeRange('24h')}
            >
              24 小时
            </Button>
            <Button
              size="sm"
              variant={timeRange === '7d' ? 'primary' : 'secondary'}
              onClick={() => setTimeRange('7d')}
            >
              7 天
            </Button>
            <Button
              size="sm"
              variant={timeRange === '30d' ? 'primary' : 'secondary'}
              onClick={() => setTimeRange('30d')}
            >
              30 天
            </Button>
          </div>
        </div>

        <div className={styles.controlGroup}>
          <label className={styles.controlLabel}>资源类型</label>
          <div className={styles.buttonGroup}>
            <Button
              size="sm"
              variant={resourceType === 'cpu' ? 'primary' : 'secondary'}
              onClick={() => setResourceType('cpu')}
            >
              <i data-lucide="cpu" />
              CPU
            </Button>
            <Button
              size="sm"
              variant={resourceType === 'memory' ? 'primary' : 'secondary'}
              onClick={() => setResourceType('memory')}
            >
              <i data-lucide="memory-stick" />
              内存
            </Button>
            <Button
              size="sm"
              variant={resourceType === 'disk' ? 'primary' : 'secondary'}
              onClick={() => setResourceType('disk')}
            >
              <i data-lucide="hard-drive" />
              磁盘
            </Button>
            <Button
              size="sm"
              variant={resourceType === 'network' ? 'primary' : 'secondary'}
              onClick={() => setResourceType('network')}
            >
              <i data-lucide="network" />
              网络
            </Button>
          </div>
        </div>

        <div className={styles.controlGroup}>
          <label className={styles.controlLabel}>
            <input
              type="checkbox"
              checked={showPrediction}
              onChange={(e) => setShowPrediction(e.target.checked)}
              className={styles.checkbox}
            />
            <span>显示趋势预测</span>
          </label>
        </div>
      </div>

      {!selectedServerId ? (
        <div className={styles.emptyState}>
          <i data-lucide="server" />
          <h3>请选择服务器</h3>
          <p>选择一个服务器以查看资源使用趋势分析</p>
        </div>
      ) : isLoading ? (
        <Loading text="加载趋势数据..." />
      ) : (
        <>
          {/* 统计卡片 */}
          {trendStats && (
            <div className={styles.statsGrid}>
              <div className={styles.statCard}>
                <div className={styles.statIcon}>
                  <i data-lucide="activity" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>当前值</div>
                  <div className={styles.statValue}>{trendStats.current.toFixed(1)}%</div>
                </div>
              </div>

              <div className={styles.statCard}>
                <div className={styles.statIcon}>
                  <i data-lucide="bar-chart-2" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>平均值</div>
                  <div className={styles.statValue}>{trendStats.avg.toFixed(1)}%</div>
                </div>
              </div>

              <div className={styles.statCard}>
                <div className={styles.statIcon}>
                  <i data-lucide="arrow-up" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>峰值</div>
                  <div className={styles.statValue}>{trendStats.max.toFixed(1)}%</div>
                  <div className={styles.statTime}>{formatTime(trendStats.peakTime)}</div>
                </div>
              </div>

              <div className={styles.statCard}>
                <div className={styles.statIcon}>
                  <i data-lucide="arrow-down" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>谷值</div>
                  <div className={styles.statValue}>{trendStats.min.toFixed(1)}%</div>
                  <div className={styles.statTime}>{formatTime(trendStats.valleyTime)}</div>
                </div>
              </div>

              <div className={`${styles.statCard} ${styles.trendCard}`}>
                <div className={`${styles.statIcon} ${getTrendClass(trendStats.trend)}`}>
                  <i data-lucide={getTrendIcon(trendStats.trend)} />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>趋势</div>
                  <div className={`${styles.statValue} ${getTrendClass(trendStats.trend)}`}>
                    {trendStats.trend === 'up' && '上升'}
                    {trendStats.trend === 'down' && '下降'}
                    {trendStats.trend === 'stable' && '稳定'}
                  </div>
                  <div className={styles.statChange}>
                    {trendStats.changePercent > 0 ? '+' : ''}
                    {trendStats.changePercent.toFixed(1)}%
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* 趋势图表 */}
          <div className={styles.chartSection}>
            <div className={styles.chartHeader}>
              <h2 className={styles.chartTitle}>
                {selectedServer?.name} - {resourceType.toUpperCase()} 使用趋势
              </h2>
              {showPrediction && <span className={styles.predictionBadge}>含预测数据</span>}
            </div>
            <div className={styles.chartContainer}>{renderChart()}</div>
          </div>

          {/* 分析建议 */}
          {trendStats && (
            <div className={styles.insights}>
              <h3 className={styles.insightsTitle}>
                <i data-lucide="lightbulb" />
                分析建议
              </h3>
              <div className={styles.insightsList}>
                {trendStats.trend === 'up' && (
                  <div className={`${styles.insightItem} ${styles.insightWarning}`}>
                    <i data-lucide="alert-triangle" />
                    <div>
                      <strong>资源使用率呈上升趋势</strong>
                      <p>
                        {resourceType.toUpperCase()} 使用率在所选时间范围内上升了{' '}
                        {Math.abs(trendStats.changePercent).toFixed(1)}%，建议关注资源容量规划。
                      </p>
                    </div>
                  </div>
                )}

                {trendStats.max > 90 && (
                  <div className={`${styles.insightItem} ${styles.insightDanger}`}>
                    <i data-lucide="alert-circle" />
                    <div>
                      <strong>检测到高峰值</strong>
                      <p>
                        峰值达到 {trendStats.max.toFixed(1)}%，发生在 {formatTime(trendStats.peakTime)}
                        。建议调查该时段的负载情况。
                      </p>
                    </div>
                  </div>
                )}

                {trendStats.trend === 'stable' && trendStats.avg < 50 && (
                  <div className={`${styles.insightItem} ${styles.insightSuccess}`}>
                    <i data-lucide="check-circle" />
                    <div>
                      <strong>资源使用稳定</strong>
                      <p>
                        {resourceType.toUpperCase()} 使用率保持稳定，平均值为 {trendStats.avg.toFixed(1)}%
                        ，系统运行良好。
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
