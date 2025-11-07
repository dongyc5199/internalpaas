import { forwardRef, useId } from 'react';
import styles from './Input.module.css';

export interface InputProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size' | 'prefix'> {
  /** 标签文本 */
  label?: string;
  /** 是否必填 */
  required?: boolean;
  /** 错误信息 */
  error?: string;
  /** 帮助文本 */
  helperText?: string;
  /** 前缀图标 */
  prefixIcon?: React.ReactNode;
  /** 后缀图标 */
  suffixIcon?: React.ReactNode;
  /** 尺寸 */
  inputSize?: 'sm' | 'md' | 'lg';
}

/**
 * Input 组件
 *
 * 表单输入框组件,支持验证错误显示
 *
 * @example
 * ```tsx
 * // 基础用法
 * <Input
 *   label="用户名"
 *   placeholder="请输入用户名"
 *   required
 * />
 *
 * // 带错误提示
 * <Input
 *   label="邮箱"
 *   error="邮箱格式不正确"
 * />
 *
 * // 带图标
 * <Input
 *   label="搜索"
 *   prefix={<SearchIcon />}
 * />
 * ```
 */
export const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      label,
      required,
      error,
      helperText,
      prefixIcon,
      suffixIcon,
      inputSize = 'md',
      className,
      id,
      ...props
    },
    ref
  ) => {
    const generatedId = useId();
    const inputId = id || generatedId;
    const hasError = Boolean(error);

    const wrapperClassName = [styles['wrapper'], styles[inputSize], className]
      .filter(Boolean)
      .join(' ');

    const inputClassName = [
      styles['input'],
      hasError && styles['error'],
      prefixIcon && styles['withPrefix'],
      suffixIcon && styles['withSuffix'],
    ]
      .filter(Boolean)
      .join(' ');

    return (
      <div className={wrapperClassName}>
        {label && (
          <label htmlFor={inputId} className={styles['label']}>
            {label}
            {required && <span className={styles['required']}>*</span>}
          </label>
        )}

        <div className={styles['inputContainer']}>
          {prefixIcon && (
            <span className={styles['prefix']} aria-hidden="true">
              {prefixIcon}
            </span>
          )}

          <input
            ref={ref}
            id={inputId}
            className={inputClassName}
            aria-invalid={hasError}
            aria-describedby={
              hasError ? `${inputId}-error` : helperText ? `${inputId}-helper` : undefined
            }
            {...props}
          />

          {suffixIcon && (
            <span className={styles['suffix']} aria-hidden="true">
              {suffixIcon}
            </span>
          )}
        </div>

        {error && (
          <span id={`${inputId}-error`} className={styles['errorMessage']} role="alert">
            {error}
          </span>
        )}

        {!error && helperText && (
          <span id={`${inputId}-helper`} className={styles['helperText']}>
            {helperText}
          </span>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
