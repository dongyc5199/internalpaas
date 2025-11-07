import styles from './Skeleton.module.css';

/**
 * Skeleton 组件属性
 */
export interface SkeletonProps {
  /** 宽度 */
  width?: string | number;
  /** 高度 */
  height?: string | number;
  /** 圆角 */
  borderRadius?: string | number;
  /** 是否为圆形 */
  circle?: boolean;
  /** 自定义类名 */
  className?: string;
  /** 动画类型 */
  animation?: 'pulse' | 'wave' | 'none';
  /** 自定义样式 */
  style?: React.CSSProperties;
}

/**
 * Skeleton 组件
 *
 * 骨架屏占位组件，用于加载状态展示
 *
 * 功能:
 * - 支持自定义尺寸
 * - 支持圆形模式
 * - 支持多种动画效果
 *
 * @example
 * ```tsx
 * <Skeleton width="100%" height={40} />
 * <Skeleton circle width={60} height={60} />
 * <Skeleton animation="wave" />
 * ```
 */
export function Skeleton({
  width,
  height = 20,
  borderRadius = 4,
  circle = false,
  className = '',
  animation = 'pulse',
  style: customStyle,
}: SkeletonProps): React.JSX.Element {
  const style: React.CSSProperties = {
    width: typeof width === 'number' ? `${width}px` : width,
    height: typeof height === 'number' ? `${height}px` : height,
    borderRadius: circle
      ? '50%'
      : typeof borderRadius === 'number'
        ? `${borderRadius}px`
        : borderRadius,
    ...customStyle,
  };

  return (
    <div
      className={`${styles.skeleton} ${styles[animation]} ${className}`}
      style={style}
      aria-busy="true"
      aria-live="polite"
    />
  );
}

/**
 * SkeletonText 组件属性
 */
export interface SkeletonTextProps {
  /** 行数 */
  lines?: number;
  /** 最后一行宽度百分比 */
  lastLineWidth?: string;
  /** 行间距 */
  spacing?: number;
}

/**
 * SkeletonText 组件
 *
 * 多行文本骨架屏
 */
export function SkeletonText({
  lines = 3,
  lastLineWidth = '60%',
  spacing = 12,
}: SkeletonTextProps): React.JSX.Element {
  return (
    <div className={styles.skeletonText}>
      {Array.from({ length: lines }).map((_, index) => (
        <Skeleton
          key={index}
          width={index === lines - 1 ? lastLineWidth : '100%'}
          height={16}
          className={styles.textLine}
          style={{ marginBottom: index < lines - 1 ? `${spacing}px` : 0 }}
        />
      ))}
    </div>
  );
}
