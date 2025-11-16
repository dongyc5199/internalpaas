import { useState, useRef, useEffect } from 'react';
import styles from './Tooltip.module.css';

export interface TooltipProps {
  content: string;
  children: React.ReactNode;
  placement?: 'top' | 'right' | 'bottom' | 'left';
  disabled?: boolean;
  delay?: number;
}

/**
 * Tooltip 组件
 *
 * 悬停提示组件,支持四个方向和延迟显示
 *
 * @example
 * ```tsx
 * <Tooltip content="这是一个提示" placement="right">
 *   <button>悬停查看</button>
 * </Tooltip>
 * ```
 */
export function Tooltip({
  content,
  children,
  placement = 'right',
  disabled = false,
  delay = 300,
}: TooltipProps): React.JSX.Element {
  const [visible, setVisible] = useState(false);
  const timeoutRef = useRef<number>();
  const tooltipRef = useRef<HTMLDivElement>(null);

  /**
   * 显示提示(带延迟)
   */
  const showTooltip = (): void => {
    if (disabled) return;

    timeoutRef.current = window.setTimeout(() => {
      setVisible(true);
    }, delay);
  };

  /**
   * 隐藏提示(立即)
   */
  const hideTooltip = (): void => {
    if (timeoutRef.current) {
      window.clearTimeout(timeoutRef.current);
    }
    setVisible(false);
  };

  /**
   * 清理定时器
   */
  useEffect((): (() => void) => {
    return (): void => {
      if (timeoutRef.current) {
        window.clearTimeout(timeoutRef.current);
      }
    };
  }, []);

  return (
    <div
      className={styles.tooltipContainer}
      onMouseEnter={showTooltip}
      onMouseLeave={hideTooltip}
      onFocus={showTooltip}
      onBlur={hideTooltip}
    >
      {children}
      {visible && !disabled && (
        <div
          ref={tooltipRef}
          className={`${styles.tooltip} ${styles[placement]}`}
          role="tooltip"
        >
          {content}
          <div className={styles.arrow} />
        </div>
      )}
    </div>
  );
}
