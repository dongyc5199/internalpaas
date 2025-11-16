import styles from './Badge.module.css';

export interface BadgeProps {
  count?: number;
  max?: number;
  dot?: boolean;
  variant?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
  children?: React.ReactNode;
  showZero?: boolean;
}

/**
 * Badge 组件
 *
 * 徽章组件,用于显示数字、状态点或标签
 *
 * @example
 * ```tsx
 * <Badge count={5}>
 *   <button>消息</button>
 * </Badge>
 *
 * <Badge dot variant="danger">
 *   <i data-lucide="bell" />
 * </Badge>
 * ```
 */
export function Badge({
  count = 0,
  max = 99,
  dot = false,
  variant = 'danger',
  children,
  showZero = false,
}: BadgeProps): React.JSX.Element {
  /**
   * 格式化显示数字
   */
  const formatCount = (): string => {
    if (count > max) {
      return `${max}+`;
    }
    return String(count);
  };

  /**
   * 是否显示徽章
   */
  const shouldShow = (): boolean => {
    if (dot) return true;
    if (count === 0 && !showZero) return false;
    return true;
  };

  if (!shouldShow()) {
    return <>{children}</>;
  }

  // 独立徽章(无子元素)
  if (!children) {
    return (
      <span className={`${styles.badge} ${styles[variant]} ${styles.standalone}`}>
        {dot ? null : formatCount()}
      </span>
    );
  }

  // 带子元素的徽章
  return (
    <span className={styles.badgeWrapper}>
      {children}
      <span
        className={`${styles.badge} ${styles[variant]} ${dot ? styles.dot : ''}`}
        aria-label={dot ? '有新消息' : `${count} 条新消息`}
      >
        {dot ? null : formatCount()}
      </span>
    </span>
  );
}
