import {
  LineChart,
  Line,
  BarChart,
  Bar,
  AreaChart,
  Area,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import styles from './Chart.module.css';

/**
 * 图表类型
 */
export type ChartType = 'line' | 'bar' | 'area' | 'pie';

/**
 * 图表数据点
 */
export interface ChartDataPoint {
  [key: string]: string | number;
}

/**
 * 图表配置项
 */
export interface ChartProps {
  /** 图表类型 */
  type: ChartType;
  /** 图表数据 */
  data: ChartDataPoint[];
  /** X轴数据键 */
  xKey?: string;
  /** Y轴数据键 */
  yKey: string | string[];
  /** 图表标题 */
  title?: string;
  /** 图表高度 (px) */
  height?: number;
  /** 是否显示网格 */
  showGrid?: boolean;
  /** 是否显示图例 */
  showLegend?: boolean;
  /** 是否显示工具提示 */
  showTooltip?: boolean;
  /** 颜色数组 (用于多数据或饼图) */
  colors?: string[];
  /** 加载状态 */
  loading?: boolean;
}

/**
 * 默认颜色方案
 */
const DEFAULT_COLORS = [
  'var(--shell-primary, #3b82f6)',
  'var(--color-success, #10b981)',
  'var(--color-warning, #f59e0b)',
  'var(--color-danger, #ef4444)',
  'var(--color-info, #06b6d4)',
];

/**
 * Chart 组件
 *
 * 通用图表组件，基于 Recharts 库
 * 支持折线图、柱状图、面积图和饼图
 *
 * @example
 * ```tsx
 * // 折线图
 * <Chart
 *   type="line"
 *   data={[
 *     { date: '2024-01', value: 100 },
 *     { date: '2024-02', value: 120 },
 *   ]}
 *   xKey="date"
 *   yKey="value"
 *   title="月度趋势"
 * />
 *
 * // 多数据折线图
 * <Chart
 *   type="line"
 *   data={data}
 *   xKey="date"
 *   yKey={['cpu', 'memory']}
 *   colors={['#3b82f6', '#10b981']}
 * />
 * ```
 */
export function Chart({
  type,
  data,
  xKey,
  yKey,
  title,
  height = 300,
  showGrid = true,
  showLegend = true,
  showTooltip = true,
  colors = DEFAULT_COLORS,
  loading = false,
}: ChartProps): React.JSX.Element {
  // 规范化 yKey 为数组
  const yKeys = Array.isArray(yKey) ? yKey : [yKey];

  // 加载骨架屏
  if (loading) {
    return (
      <div className={styles.chartContainer}>
        {title && <h3 className={styles.chartTitle}>{title}</h3>}
        <div className={styles.skeleton} style={{ height: `${height}px` }}>
          <div className={styles.skeletonPulse} />
        </div>
      </div>
    );
  }

  // 空数据提示
  if (!data || data.length === 0) {
    return (
      <div className={styles.chartContainer}>
        {title && <h3 className={styles.chartTitle}>{title}</h3>}
        <div className={styles.emptyState} style={{ height: `${height}px` }}>
          <i data-lucide="bar-chart-3" className={styles.emptyIcon} />
          <p>暂无数据</p>
        </div>
      </div>
    );
  }

  // 渲染折线图
  const renderLineChart = (): React.JSX.Element => (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={data}>
        {showGrid && <CartesianGrid strokeDasharray="3 3" className={styles.grid} />}
        {xKey && <XAxis dataKey={xKey} className={styles.axis} />}
        <YAxis className={styles.axis} />
        {showTooltip && <Tooltip contentStyle={{ backgroundColor: 'var(--shell-surface)' }} />}
        {showLegend && <Legend />}
        {yKeys.map((key, index) => (
          <Line
            key={key}
            type="monotone"
            dataKey={key}
            stroke={colors[index % colors.length]}
            strokeWidth={2}
            dot={{ fill: colors[index % colors.length] }}
          />
        ))}
      </LineChart>
    </ResponsiveContainer>
  );

  // 渲染柱状图
  const renderBarChart = (): React.JSX.Element => (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={data}>
        {showGrid && <CartesianGrid strokeDasharray="3 3" className={styles.grid} />}
        {xKey && <XAxis dataKey={xKey} className={styles.axis} />}
        <YAxis className={styles.axis} />
        {showTooltip && <Tooltip contentStyle={{ backgroundColor: 'var(--shell-surface)' }} />}
        {showLegend && <Legend />}
        {yKeys.map((key, index) => (
          <Bar key={key} dataKey={key} fill={colors[index % colors.length]} />
        ))}
      </BarChart>
    </ResponsiveContainer>
  );

  // 渲染面积图
  const renderAreaChart = (): React.JSX.Element => (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={data}>
        {showGrid && <CartesianGrid strokeDasharray="3 3" className={styles.grid} />}
        {xKey && <XAxis dataKey={xKey} className={styles.axis} />}
        <YAxis className={styles.axis} />
        {showTooltip && <Tooltip contentStyle={{ backgroundColor: 'var(--shell-surface)' }} />}
        {showLegend && <Legend />}
        {yKeys.map((key, index) => (
          <Area
            key={key}
            type="monotone"
            dataKey={key}
            stroke={colors[index % colors.length]}
            fill={colors[index % colors.length]}
            fillOpacity={0.3}
          />
        ))}
      </AreaChart>
    </ResponsiveContainer>
  );

  // 渲染饼图
  const renderPieChart = (): React.JSX.Element => {
    const pieData = data.map((item, index) => ({
      ...item,
      fill: colors[index % colors.length],
    }));

    return (
      <ResponsiveContainer width="100%" height={height}>
        <PieChart>
          <Pie
            data={pieData}
            dataKey={yKeys[0]}
            nameKey={xKey}
            cx="50%"
            cy="50%"
            outerRadius={80}
            label
          >
            {pieData.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.fill as string} />
            ))}
          </Pie>
          {showTooltip && <Tooltip />}
          {showLegend && <Legend />}
        </PieChart>
      </ResponsiveContainer>
    );
  };

  // 根据类型渲染对应图表
  const renderChart = (): React.JSX.Element => {
    switch (type) {
      case 'line':
        return renderLineChart();
      case 'bar':
        return renderBarChart();
      case 'area':
        return renderAreaChart();
      case 'pie':
        return renderPieChart();
      default:
        return renderLineChart();
    }
  };

  return (
    <div className={styles.chartContainer}>
      {title && <h3 className={styles.chartTitle}>{title}</h3>}
      <div className={styles.chartWrapper}>{renderChart()}</div>
    </div>
  );
}
