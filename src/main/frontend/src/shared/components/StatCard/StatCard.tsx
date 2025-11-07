import styles from './StatCard.module.css';

/**
 * StatCard 组件属性
 */
export interface StatCardProps {
  /** 卡片标题 */
  title: string;
  /** 主要数值 */
  value: number | string;
  /** 变化百分比 */
  change?: number;
  /** 变化趋势 */
  trend?: 'up' | 'down' | 'neutral';
  /** 图标 (lucide icon name) */
  icon?: string;
  /** 描述文本 */
  description?: string;
  /** 是否加载中 */
  loading?: boolean;
  /** 点击回调 */
  onClick?: () => void;
}

/**
 * StatCard 组件
 *
 * 统计卡片组件，用于显示关键指标
 *
 * 功能:
 * - 显示标题、数值、变化趋势
 * - 支持图标展示
 * - 加载状态
 * - 可点击交互
 *
 * @example
 * ```tsx
 * <StatCard
 *   title="服务器总数"
 *   value={42}
 *   change={12}
 *   trend="up"
 *   icon="server"
 *   description="较上月"
 * />
 * ```
 */
export function StatCard({
  title,
  value,
  change,
  trend = 'neutral',
  icon,
  description,
  loading = false,
  onClick,
}: StatCardProps): React.JSX.Element {
  const cardClassName = [
    styles.card,
    onClick ? styles.clickable : '',
    loading ? styles.loading : '',
  ]
    .filter(Boolean)
    .join(' ');

  const trendClassName = [
    styles.trend,
    trend === 'up' ? styles.trendUp : '',
    trend === 'down' ? styles.trendDown : '',
    trend === 'neutral' ? styles.trendNeutral : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={cardClassName} onClick={onClick} role={onClick ? 'button' : undefined}>
      {/* 卡片头部 */}
      <div className={styles.header}>
        <span className={styles.title}>{title}</span>
        {icon && (
          <div className={styles.iconWrapper}>
            <i data-lucide={icon} className={styles.icon} />
          </div>
        )}
      </div>

      {/* 主要数值 */}
      <div className={styles.body}>
        {loading ? (
          <div className={styles.skeleton}>
            <div className={styles.skeletonValue} />
            <div className={styles.skeletonChange} />
          </div>
        ) : (
          <>
            <div className={styles.value}>{value}</div>
            {change !== undefined && (
              <div className={trendClassName}>
                <i
                  data-lucide={
                    trend === 'up' ? 'trending-up' : trend === 'down' ? 'trending-down' : 'minus'
                  }
                  className={styles.trendIcon}
                />
                <span className={styles.changeValue}>
                  {change > 0 ? '+' : ''}
                  {change}%
                </span>
                {description && <span className={styles.description}>{description}</span>}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
