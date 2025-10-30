/**
 * Input Component
 *
 * A flexible input component with validation states, sizes, and icon support.
 * Follows modern design principles with accessibility support.
 */

import React, { forwardRef } from 'react';
import styles from './Input.module.css';

export type InputSize = 'sm' | 'md' | 'lg';
export type InputVariant = 'default' | 'error' | 'success' | 'warning';

export interface InputProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size'> {
  /**
   * Input size
   * @default 'md'
   */
  size?: InputSize;

  /**
   * Input variant (validation state)
   * @default 'default'
   */
  variant?: InputVariant;

  /**
   * Whether input takes full width of container
   * @default false
   */
  fullWidth?: boolean;

  /**
   * Icon to display before the input
   */
  leftIcon?: React.ReactNode;

  /**
   * Icon to display after the input
   */
  rightIcon?: React.ReactNode;

  /**
   * Label text
   */
  label?: string;

  /**
   * Helper text to display below input
   */
  helperText?: string;

  /**
   * Error message (sets variant to 'error')
   */
  error?: string;

  /**
   * Input container className
   */
  containerClassName?: string;

  /**
   * Label className
   */
  labelClassName?: string;
}

/**
 * Input component
 *
 * @example
 * ```tsx
 * // Basic input
 * <Input placeholder="Enter text" />
 *
 * // Input with label
 * <Input label="Username" placeholder="Enter username" />
 *
 * // Input with validation
 * <Input
 *   label="Email"
 *   type="email"
 *   error="Please enter a valid email"
 * />
 *
 * // Input with icons
 * <Input
 *   leftIcon={<SearchIcon />}
 *   placeholder="Search..."
 * />
 *
 * // Disabled input
 * <Input label="Disabled" disabled value="Cannot edit" />
 * ```
 */
export const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      size = 'md',
      variant = 'default',
      fullWidth = false,
      leftIcon,
      rightIcon,
      label,
      helperText,
      error,
      containerClassName,
      labelClassName,
      className,
      disabled,
      id,
      type = 'text',
      ...rest
    },
    ref
  ) => {
    // Generate unique ID if not provided
    const inputId = id || `input-${Math.random().toString(36).substr(2, 9)}`;

    // Determine actual variant (error overrides variant prop)
    const actualVariant = error ? 'error' : variant;

    // Build container class names
    const containerClasses = [
      styles.container,
      fullWidth && styles.fullWidth,
      containerClassName,
    ]
      .filter(Boolean)
      .join(' ');

    // Build input wrapper class names
    const wrapperClasses = [
      styles.inputWrapper,
      styles[`size-${size}`],
      styles[`variant-${actualVariant}`],
      disabled && styles.disabled,
      leftIcon && styles.hasLeftIcon,
      rightIcon && styles.hasRightIcon,
    ]
      .filter(Boolean)
      .join(' ');

    // Build input class names
    const inputClasses = [styles.input, className].filter(Boolean).join(' ');

    // Build label class names
    const labelClasses = [styles.label, disabled && styles.labelDisabled, labelClassName]
      .filter(Boolean)
      .join(' ');

    return (
      <div className={containerClasses}>
        {label && (
          <label htmlFor={inputId} className={labelClasses}>
            {label}
          </label>
        )}

        <div className={wrapperClasses}>
          {leftIcon && (
            <span className={styles.leftIcon} aria-hidden="true">
              {leftIcon}
            </span>
          )}

          <input
            ref={ref}
            id={inputId}
            type={type}
            className={inputClasses}
            disabled={disabled}
            aria-invalid={actualVariant === 'error'}
            aria-describedby={
              error
                ? `${inputId}-error`
                : helperText
                ? `${inputId}-helper`
                : undefined
            }
            {...rest}
          />

          {rightIcon && (
            <span className={styles.rightIcon} aria-hidden="true">
              {rightIcon}
            </span>
          )}
        </div>

        {error && (
          <p id={`${inputId}-error`} className={styles.errorText} role="alert">
            {error}
          </p>
        )}

        {!error && helperText && (
          <p id={`${inputId}-helper`} className={styles.helperText}>
            {helperText}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
