import { forwardRef } from 'react';
import styles from './Select.module.css';

/**
 * Select 组件属性
 */
export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  /** 是否全宽 */
  fullWidth?: boolean;
  /** 错误状态 */
  error?: boolean;
}

/**
 * Select 组件
 *
 * 可重用的下拉选择组件
 *
 * 功能:
 * - 支持原生 select 所有属性
 * - 可选全宽模式
 * - 错误状态样式
 * - 禁用状态样式
 */
export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ className = '', fullWidth = false, error = false, disabled = false, ...props }, ref) => {
    const selectClassName = [
      styles.select,
      fullWidth ? styles.fullWidth : '',
      error ? styles.error : '',
      disabled ? styles.disabled : '',
      className,
    ]
      .filter(Boolean)
      .join(' ');

    return <select ref={ref} className={selectClassName} disabled={disabled} {...props} />;
  }
);

Select.displayName = 'Select';
