import { forwardRef } from 'react';
import styles from './Button.module.css';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  /** 按钮变体 */
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'outline';
  /** 按钮尺寸 */
  size?: 'sm' | 'md' | 'lg';
  /** 是否全宽 */
  fullWidth?: boolean;
  /** 加载状态 */
  loading?: boolean;
  /** 左侧图标 */
  leftIcon?: React.ReactNode;
  /** 右侧图标 */
  rightIcon?: React.ReactNode;
  /** 子内容 */
  children?: React.ReactNode;
}

/**
 * Button 组件
 *
 * 基础按钮组件,支持多种变体和尺寸
 *
 * @example
 * ```tsx
 * // 基础用法
 * <Button variant="primary">确定</Button>
 *
 * // 加载状态
 * <Button loading>提交中...</Button>
 *
 * // 带图标
 * <Button leftIcon={<PlusIcon />}>新建</Button>
 * ```
 */
export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = 'primary',
      size = 'md',
      fullWidth = false,
      loading = false,
      leftIcon,
      rightIcon,
      children,
      className,
      disabled,
      ...props
    },
    ref
  ) => {
    const classNames = [
      styles.button,
      styles[variant],
      styles[size],
      fullWidth && styles.fullWidth,
      loading && styles.loading,
      className,
    ]
      .filter(Boolean)
      .join(' ');

    return (
      <button
        ref={ref}
        className={classNames}
        disabled={disabled || loading}
        aria-busy={loading}
        {...props}
      >
        {loading && <span className={styles.spinner} aria-hidden="true" />}
        
        {!loading && leftIcon && (
          <span className={styles.leftIcon} aria-hidden="true">
            {leftIcon}
          </span>
        )}

        {children && <span className={styles.content}>{children}</span>}

        {!loading && rightIcon && (
          <span className={styles.rightIcon} aria-hidden="true">
            {rightIcon}
          </span>
        )}
      </button>
    );
  }
);

Button.displayName = 'Button';
