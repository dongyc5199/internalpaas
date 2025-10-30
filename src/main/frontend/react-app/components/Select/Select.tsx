/**
 * Select Component
 *
 * A flexible select dropdown component with validation states and icon support.
 * Follows modern design principles with accessibility support.
 */

import React, { forwardRef } from 'react';
import styles from './Select.module.css';

export type SelectSize = 'sm' | 'md' | 'lg';
export type SelectVariant = 'default' | 'error' | 'success' | 'warning';

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

export interface SelectProps extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'size'> {
  /**
   * Select size
   * @default 'md'
   */
  size?: SelectSize;

  /**
   * Select variant (validation state)
   * @default 'default'
   */
  variant?: SelectVariant;

  /**
   * Whether select takes full width of container
   * @default false
   */
  fullWidth?: boolean;

  /**
   * Icon to display before the select
   */
  leftIcon?: React.ReactNode;

  /**
   * Label text
   */
  label?: string;

  /**
   * Helper text to display below select
   */
  helperText?: string;

  /**
   * Error message (sets variant to 'error')
   */
  error?: string;

  /**
   * Options for the select dropdown
   */
  options?: SelectOption[];

  /**
   * Placeholder text (shown as disabled first option)
   */
  placeholder?: string;

  /**
   * Select container className
   */
  containerClassName?: string;

  /**
   * Label className
   */
  labelClassName?: string;
}

/**
 * Select component
 *
 * @example
 * ```tsx
 * // Basic select
 * <Select options={[
 *   { value: '1', label: 'Option 1' },
 *   { value: '2', label: 'Option 2' }
 * ]} />
 *
 * // Select with label
 * <Select
 *   label="Country"
 *   placeholder="Select country"
 *   options={countries}
 * />
 *
 * // Select with validation
 * <Select
 *   label="Status"
 *   error="Please select a status"
 *   options={statusOptions}
 * />
 *
 * // Select with icon
 * <Select
 *   leftIcon={<LocationIcon />}
 *   placeholder="Select location"
 *   options={locations}
 * />
 *
 * // Disabled select
 * <Select label="Disabled" disabled options={options} />
 * ```
 */
export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  (
    {
      size = 'md',
      variant = 'default',
      fullWidth = false,
      leftIcon,
      label,
      helperText,
      error,
      options = [],
      placeholder,
      containerClassName,
      labelClassName,
      className,
      disabled,
      id,
      children,
      ...rest
    },
    ref
  ) => {
    // Generate unique ID if not provided
    const selectId = id || `select-${Math.random().toString(36).substr(2, 9)}`;

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

    // Build select wrapper class names
    const wrapperClasses = [
      styles.selectWrapper,
      styles[`size-${size}`],
      styles[`variant-${actualVariant}`],
      disabled && styles.disabled,
      leftIcon && styles.hasLeftIcon,
    ]
      .filter(Boolean)
      .join(' ');

    // Build select class names
    const selectClasses = [styles.select, className].filter(Boolean).join(' ');

    // Build label class names
    const labelClasses = [styles.label, disabled && styles.labelDisabled, labelClassName]
      .filter(Boolean)
      .join(' ');

    return (
      <div className={containerClasses}>
        {label && (
          <label htmlFor={selectId} className={labelClasses}>
            {label}
          </label>
        )}

        <div className={wrapperClasses}>
          {leftIcon && (
            <span className={styles.leftIcon} aria-hidden="true">
              {leftIcon}
            </span>
          )}

          <select
            ref={ref}
            id={selectId}
            className={selectClasses}
            disabled={disabled}
            aria-invalid={actualVariant === 'error'}
            aria-describedby={
              error
                ? `${selectId}-error`
                : helperText
                ? `${selectId}-helper`
                : undefined
            }
            {...rest}
          >
            {placeholder && (
              <option value="" disabled>
                {placeholder}
              </option>
            )}
            {options.map((option) => (
              <option
                key={option.value}
                value={option.value}
                disabled={option.disabled}
              >
                {option.label}
              </option>
            ))}
            {children}
          </select>

          <span className={styles.chevronIcon} aria-hidden="true">
            <svg
              width="20"
              height="20"
              viewBox="0 0 20 20"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M5 7.5L10 12.5L15 7.5"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </span>
        </div>

        {error && (
          <p id={`${selectId}-error`} className={styles.errorText} role="alert">
            {error}
          </p>
        )}

        {!error && helperText && (
          <p id={`${selectId}-helper`} className={styles.helperText}>
            {helperText}
          </p>
        )}
      </div>
    );
  }
);

Select.displayName = 'Select';
