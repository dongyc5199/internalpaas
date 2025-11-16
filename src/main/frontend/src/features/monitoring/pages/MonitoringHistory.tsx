import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { monitoringApi } from '../../../shared/api/monitoringApi';
import { serverApi } from '../../../shared/api/serverApi';
import { QUERY_KEYS } from '../../../shared/constants';
import { Button, Loading } from '../../../shared/components';
import type { Server, MetricsDataPoint, TimeRange } from '../../../shared/types';
import styles from './MonitoringHistory.module.css';

/**
 * 图表数据点
 */
interface DataPoint {
  timestamp: string;
  value: number;
}


/**
 * MonitoringHistory 组件
 *
 * 监控历史数据图表页面 - 显示服务器历史监控数据的趋势图表
 */
export function MonitoringHistory(): React.JSX.Element {
  const [searchParams, setSearchParams] = useSearchParams();
  const serverIdParam = searchParams.get('serverId');
  const [selectedServerId, setSelectedServerId] = useState<number | null>(
    serverIdParam ? Number(serverIdParam) : null
  );
  const [timeRange, setTimeRange] = useState<TimeRange>('24h');
  const [selectedMetric, setSelectedMetric] = useState<'cpu' | 'memory' | 'disk' | 'network'>(
    'cpu'
  );

  // 获取所有服务器列表
  const { data: serversData } = useQuery({
    queryKey: [QUERY_KEYS.SERVERS],
    queryFn: () => serverApi.getServers(),
  });

  const servers = (serversData?.content ?? []) as Server[];

  // 获取历史数据
  const { data: historyData, isLoading } = useQuery({
    queryKey: [QUERY_KEYS.SERVER_METRICS_HISTORY, selectedServerId, timeRange],
    queryFn: () =>
      selectedServerId
        ? monitoringApi.getServerMetricsHistory(selectedServerId, timeRange)
        : Promise.resolve(null),
    enabled: !!selectedServerId,
  });

  // 处理服务器选择
  const handleServerChange = (serverId: number): void => {
    setSelectedServerId(serverId);
    setSearchParams({ serverId: String(serverId) });
  };

  // 处理时间范围变更
  const handleTimeRangeChange = (range: TimeRange): void => {
    setTimeRange(range);
  };

  // 获取当前选中的服务器
  const selectedServer = servers?.find((s: Server) => s.id === selectedServerId);

  // 时间范围选项
  const timeRanges: Array<{ value: TimeRange; label: string }> = [
    { value: '1h', label: '最近1小时' },
    { value: '6h', label: '最近6小时' },
    { value: '24h', label: '最近24小时' },
    { value: '7d', label: '最近7天' },
    { value: '30d', label: '最近30天' },
  ];

  // 指标选项
  const metrics = [
    { value: 'cpu', label: 'CPU使用率', unit: '%', color: '#3b82f6' },
    { value: 'memory', label: '内存使用率', unit: '%', color: '#10b981' },
    { value: 'disk', label: '磁盘使用率', unit: '%', color: '#f59e0b' },
    { value: 'network', label: '网络流量', unit: 'MB/s', color: '#8b5cf6' },
  ] as const;

  // 转换历史数据为图表数据格式
  const transformMetricsData = (
    dataPoints: MetricsDataPoint[] | undefined,
    metric: 'cpu' | 'memory' | 'disk' | 'network'
  ): DataPoint[] => {
    if (!dataPoints) return [];
    return dataPoints.map((point) => {
      let value = 0;
      if (metric === 'cpu') value = point.cpuUsage;
      else if (metric === 'memory') value = point.memoryUsage;
      else if (metric === 'disk') value = point.diskUsage;
      else if (metric === 'network')
        value = (point.networkIn ?? 0) + (point.networkOut ?? 0);
      return {
        timestamp: point.timestamp,
        value,
      };
    });
  };

  // 获取当前指标的数据
  const currentMetricData = transformMetricsData(historyData?.dataPoints, selectedMetric);
  const currentMetric = metrics.find((m) => m.value === selectedMetric);

  // 计算统计信息
  const calculateStats = (data: DataPoint[]): { avg: number; max: number; min: number } => {
    if (data.length === 0) return { avg: 0, max: 0, min: 0 };
    const values = data.map((d) => d.value);
    return {
      avg: values.reduce((a, b) => a + b, 0) / values.length,
      max: Math.max(...values),
      min: Math.min(...values),
    };
  };

  const stats = calculateStats(currentMetricData);

  // 格式化时间戳
  const formatTimestamp = (timestamp: string, range: TimeRange): string => {
    const date = new Date(timestamp);
    if (range === '1h' || range === '6h') {
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    } else if (range === '24h') {
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    } else {
      return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
    }
  };

  // 简化的SVG图表渲染
  const renderChart = (): React.JSX.Element => {
    if (!currentMetricData || currentMetricData.length === 0) {
      return (
        <div className={styles.emptyChart}>
          <i data-lucide="bar-chart-3" />
          <p>暂无历史数据</p>
        </div>
      );
    }

    const chartWidth = 800;
    const chartHeight = 300;
    const padding = { top: 20, right: 20, bottom: 40, left: 60 };
    const innerWidth = chartWidth - padding.left - padding.right;
    const innerHeight = chartHeight - padding.top - padding.bottom;

    // 计算比例
    const xScale = innerWidth / (currentMetricData.length - 1);
    const maxValue = Math.max(...currentMetricData.map((d) => d.value), 100);
    const yScale = innerHeight / maxValue;

    // 生成路径
    const pathData = currentMetricData
      .map((d: DataPoint, i: number) => {
        const x = padding.left + i * xScale;
        const y = chartHeight - padding.bottom - d.value * yScale;
        return `${i === 0 ? 'M' : 'L'} ${x.toFixed(2)},${y.toFixed(2)}`;
      })
      .join(' ');

    // 生成区域填充路径
    const areaData = `${pathData} L ${padding.left + (currentMetricData.length - 1) * xScale},${chartHeight - padding.bottom} L ${padding.left},${chartHeight - padding.bottom} Z`;

    return (
      <svg
        width="100%"
        height="100%"
        viewBox={`0 0 ${chartWidth} ${chartHeight}`}
        className={styles.chartSvg}
      >
        {/* 网格线 */}
        {[0, 25, 50, 75, 100].map((tick) => {
          const y = chartHeight - padding.bottom - (tick / 100) * maxValue * yScale;
          return (
            <g key={tick}>
              <line
                x1={padding.left}
                y1={y}
                x2={chartWidth - padding.right}
                y2={y}
                className={styles.gridLine}
              />
              <text x={padding.left - 10} y={y + 5} className={styles.axisLabel}>
                {tick}
              </text>
            </g>
          );
        })}

        {/* 区域填充 */}
        <path d={areaData} className={styles.chartArea} fill={currentMetric?.color} />

        {/* 线条 */}
        <path d={pathData} className={styles.chartLine} stroke={currentMetric?.color} />

        {/* 数据点 */}
        {currentMetricData.map((d: DataPoint, i: number) => {
          const x = padding.left + i * xScale;
          const y = chartHeight - padding.bottom - d.value * yScale;
          return (
            <circle
              key={i}
              cx={x}
              cy={y}
              r="3"
              className={styles.chartPoint}
              fill={currentMetric?.color}
            />
          );
        })}

        {/* X轴标签 */}
        {currentMetricData
          .filter((_: DataPoint, i: number) => i % Math.ceil(currentMetricData.length / 6) === 0)
          .map((d: DataPoint, idx: number) => {
            const originalIndex = idx * Math.ceil(currentMetricData.length / 6);
            const x = padding.left + originalIndex * xScale;
            return (
              <text
                key={idx}
                x={x}
                y={chartHeight - padding.bottom + 25}
                className={styles.axisLabel}
              >
                {formatTimestamp(d.timestamp, timeRange)}
              </text>
            );
          })}
      </svg>
    );
  };

  return (
    <div className={styles.container}>
      {/* 页面头部 */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleGroup}>
            <h1 className={styles.title}>监控历史</h1>
            <p className={styles.subtitle}>查看服务器历史监控数据趋势</p>
          </div>
        </div>
      </header>

      {/* 控制面板 */}
      <div className={styles.controlPanel}>
        {/* 服务器选择 */}
        <div className={styles.controlGroup}>
          <label className={styles.controlLabel}>选择服务器</label>
          <select
            className={styles.select}
            value={selectedServerId ?? ''}
            onChange={(e) => handleServerChange(Number(e.target.value))}
          >
            <option value="">请选择服务器</option>
            {servers?.map((server: Server) => (
              <option key={server.id} value={server.id}>
                {server.name} ({server.host})
              </option>
            ))}
          </select>
        </div>

        {/* 时间范围选择 */}
        {selectedServerId && (
          <>
            <div className={styles.controlGroup}>
              <label className={styles.controlLabel}>时间范围</label>
              <div className={styles.timeRangeButtons}>
                {timeRanges.map((range) => (
                  <Button
                    key={range.value}
                    size="sm"
                    variant={timeRange === range.value ? 'primary' : 'secondary'}
                    onClick={() => handleTimeRangeChange(range.value)}
                  >
                    {range.label}
                  </Button>
                ))}
              </div>
            </div>
          </>
        )}
      </div>

      {/* 主内容区 */}
      {!selectedServerId ? (
        <div className={styles.emptyState}>
          <i data-lucide="server" />
          <h3>请选择一个服务器</h3>
          <p>选择服务器以查看其历史监控数据</p>
        </div>
      ) : isLoading ? (
        <Loading text="加载历史数据..." />
      ) : (
        <>
          {/* 服务器信息卡片 */}
          <div className={styles.serverInfo}>
            <div className={styles.serverInfoItem}>
              <span className={styles.serverInfoLabel}>服务器名称</span>
              <span className={styles.serverInfoValue}>{selectedServer?.name}</span>
            </div>
            <div className={styles.serverInfoItem}>
              <span className={styles.serverInfoLabel}>服务器地址</span>
              <span className={styles.serverInfoValue}>{selectedServer?.host}</span>
            </div>
            <div className={styles.serverInfoItem}>
              <span className={styles.serverInfoLabel}>时间范围</span>
              <span className={styles.serverInfoValue}>
                {timeRanges.find((r) => r.value === timeRange)?.label}
              </span>
            </div>
          </div>

          {/* 指标选择 */}
          <div className={styles.metricsSelector}>
            {metrics.map((metric) => (
              <button
                key={metric.value}
                className={`${styles.metricButton} ${selectedMetric === metric.value ? styles.metricButtonActive : ''}`}
                onClick={() => setSelectedMetric(metric.value)}
                style={
                  {
                    '--metric-color': metric.color,
                  } as React.CSSProperties
                }
              >
                <span className={styles.metricButtonLabel}>{metric.label}</span>
                <span className={styles.metricButtonUnit}>{metric.unit}</span>
              </button>
            ))}
          </div>

          {/* 统计卡片 */}
          <div className={styles.statsGrid}>
            <div className={styles.statCard}>
              <div className={styles.statLabel}>平均值</div>
              <div className={styles.statValue}>
                {stats.avg.toFixed(2)} {currentMetric?.unit}
              </div>
            </div>
            <div className={styles.statCard}>
              <div className={styles.statLabel}>最大值</div>
              <div className={styles.statValue}>
                {stats.max.toFixed(2)} {currentMetric?.unit}
              </div>
            </div>
            <div className={styles.statCard}>
              <div className={styles.statLabel}>最小值</div>
              <div className={styles.statValue}>
                {stats.min.toFixed(2)} {currentMetric?.unit}
              </div>
            </div>
            <div className={styles.statCard}>
              <div className={styles.statLabel}>数据点</div>
              <div className={styles.statValue}>{currentMetricData.length}</div>
            </div>
          </div>

          {/* 图表容器 */}
          <div className={styles.chartContainer}>
            <div className={styles.chartHeader}>
              <h3 className={styles.chartTitle}>{currentMetric?.label}</h3>
            </div>
            <div className={styles.chartWrapper}>{renderChart()}</div>
          </div>
        </>
      )}
    </div>
  );
}
