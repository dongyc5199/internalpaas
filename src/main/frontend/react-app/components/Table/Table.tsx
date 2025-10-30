/**
 * Table Component
 *
 * A flexible table component with sorting, selection, and customizable columns.
 * Supports responsive design, striped rows, and hover effects.
 */

import React from 'react';
import styles from './Table.module.css';

export type TableSize = 'sm' | 'md' | 'lg';
export type TableVariant = 'default' | 'striped' | 'bordered';

export interface TableColumn<T = unknown> {
  /**
   * Unique key for the column
   */
  key: string;

  /**
   * Column header label
   */
  label: React.ReactNode;

  /**
   * Function to render cell content
   */
  render?: (row: T, index: number) => React.ReactNode;

  /**
   * Data accessor key (used if render is not provided)
   */
  dataKey?: keyof T;

  /**
   * Whether the column is sortable
   * @default false
   */
  sortable?: boolean;

  /**
   * Column width (CSS width value)
   */
  width?: string;

  /**
   * Column alignment
   * @default 'left'
   */
  align?: 'left' | 'center' | 'right';

  /**
   * Custom header className
   */
  headerClassName?: string;

  /**
   * Custom cell className
   */
  cellClassName?: string;
}

export interface TableProps<T = unknown> {
  /**
   * Table columns configuration
   */
  columns: TableColumn<T>[];

  /**
   * Table data rows
   */
  data: T[];

  /**
   * Table size
   * @default 'md'
   */
  size?: TableSize;

  /**
   * Table variant
   * @default 'default'
   */
  variant?: TableVariant;

  /**
   * Whether to show hover effect on rows
   * @default true
   */
  hover?: boolean;

  /**
   * Whether rows are selectable
   * @default false
   */
  selectable?: boolean;

  /**
   * Selected row keys (for controlled selection)
   */
  selectedKeys?: string[];

  /**
   * Callback when row selection changes
   */
  onSelectionChange?: (selectedKeys: string[]) => void;

  /**
   * Row key extractor function
   */
  rowKey?: (row: T, index: number) => string;

  /**
   * Callback when row is clicked
   */
  onRowClick?: (row: T, index: number) => void;

  /**
   * Current sort configuration
   */
  sortConfig?: {
    key: string;
    direction: 'asc' | 'desc';
  };

  /**
   * Callback when sort changes
   */
  onSortChange?: (key: string, direction: 'asc' | 'desc') => void;

  /**
   * Whether table takes full width
   * @default true
   */
  fullWidth?: boolean;

  /**
   * Empty state message
   */
  emptyMessage?: React.ReactNode;

  /**
   * Loading state
   */
  loading?: boolean;

  /**
   * Custom className for table container
   */
  className?: string;

  /**
   * Custom className for table element
   */
  tableClassName?: string;

  /**
   * Caption for the table (accessibility)
   */
  caption?: string;

  /**
   * Sticky header
   * @default false
   */
  stickyHeader?: boolean;
}

/**
 * Table component
 *
 * @example
 * ```tsx
 * // Basic table
 * <Table
 *   columns={[
 *     { key: 'name', label: 'Name', dataKey: 'name' },
 *     { key: 'email', label: 'Email', dataKey: 'email' }
 *   ]}
 *   data={users}
 * />
 *
 * // Table with sorting
 * <Table
 *   columns={[
 *     { key: 'name', label: 'Name', dataKey: 'name', sortable: true },
 *     { key: 'age', label: 'Age', dataKey: 'age', sortable: true }
 *   ]}
 *   data={users}
 *   sortConfig={{ key: 'name', direction: 'asc' }}
 *   onSortChange={(key, direction) => handleSort(key, direction)}
 * />
 *
 * // Table with selection
 * <Table
 *   columns={columns}
 *   data={items}
 *   selectable
 *   selectedKeys={selectedKeys}
 *   onSelectionChange={setSelectedKeys}
 *   rowKey={(row) => row.id}
 * />
 *
 * // Table with custom render
 * <Table
 *   columns={[
 *     { key: 'name', label: 'Name', dataKey: 'name' },
 *     {
 *       key: 'actions',
 *       label: 'Actions',
 *       render: (row) => (
 *         <Button size="sm" onClick={() => handleEdit(row)}>Edit</Button>
 *       )
 *     }
 *   ]}
 *   data={items}
 * />
 * ```
 */
export function Table<T = unknown>({
  columns,
  data,
  size = 'md',
  variant = 'default',
  hover = true,
  selectable = false,
  selectedKeys = [],
  onSelectionChange,
  rowKey = (_, index) => String(index),
  onRowClick,
  sortConfig,
  onSortChange,
  fullWidth = true,
  emptyMessage = 'No data available',
  loading = false,
  className,
  tableClassName,
  caption,
  stickyHeader = false,
}: TableProps<T>) {
  // Handle row selection
  const handleRowSelect = (key: string, checked: boolean) => {
    if (!onSelectionChange) return;

    const newSelectedKeys = checked
      ? [...selectedKeys, key]
      : selectedKeys.filter((k) => k !== key);

    onSelectionChange(newSelectedKeys);
  };

  // Handle select all
  const handleSelectAll = (checked: boolean) => {
    if (!onSelectionChange) return;

    const newSelectedKeys = checked ? data.map((row, index) => rowKey(row, index)) : [];
    onSelectionChange(newSelectedKeys);
  };

  // Handle sort
  const handleSort = (columnKey: string) => {
    if (!onSortChange) return;

    const newDirection =
      sortConfig?.key === columnKey && sortConfig.direction === 'asc' ? 'desc' : 'asc';
    onSortChange(columnKey, newDirection);
  };

  // Check if all rows are selected
  const allSelected = data.length > 0 && selectedKeys.length === data.length;
  const someSelected = selectedKeys.length > 0 && selectedKeys.length < data.length;

  // Build class names
  const containerClasses = [
    styles.container,
    fullWidth && styles.fullWidth,
    className,
  ]
    .filter(Boolean)
    .join(' ');

  const tableClasses = [
    styles.table,
    styles[`size-${size}`],
    styles[`variant-${variant}`],
    hover && styles.hover,
    stickyHeader && styles.stickyHeader,
    tableClassName,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={containerClasses}>
      <table className={tableClasses}>
        {caption && <caption className={styles.caption}>{caption}</caption>}

        <thead className={styles.thead}>
          <tr>
            {selectable && (
              <th className={`${styles.th} ${styles.checkboxCell}`}>
                <input
                  type="checkbox"
                  checked={allSelected}
                  ref={(input) => {
                    if (input) {
                      input.indeterminate = someSelected;
                    }
                  }}
                  onChange={(e) => handleSelectAll(e.target.checked)}
                  aria-label="Select all rows"
                />
              </th>
            )}

            {columns.map((column) => {
              const thClasses = [
                styles.th,
                column.align && styles[`align-${column.align}`],
                column.sortable && styles.sortable,
                column.headerClassName,
              ]
                .filter(Boolean)
                .join(' ');

              return (
                <th
                  key={column.key}
                  className={thClasses}
                  style={{ width: column.width }}
                  onClick={() => column.sortable && handleSort(column.key)}
                >
                  <div className={styles.thContent}>
                    {column.label}
                    {column.sortable && (
                      <span className={styles.sortIcon}>
                        {sortConfig?.key === column.key ? (
                          sortConfig.direction === 'asc' ? (
                            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                              <path
                                d="M8 4L4 8H12L8 4Z"
                                fill="currentColor"
                              />
                            </svg>
                          ) : (
                            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                              <path
                                d="M8 12L4 8H12L8 12Z"
                                fill="currentColor"
                              />
                            </svg>
                          )
                        ) : (
                          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                            <path
                              d="M8 4L4 8H12L8 4Z"
                              fill="currentColor"
                              opacity="0.3"
                            />
                            <path
                              d="M8 12L4 8H12L8 12Z"
                              fill="currentColor"
                              opacity="0.3"
                            />
                          </svg>
                        )}
                      </span>
                    )}
                  </div>
                </th>
              );
            })}
          </tr>
        </thead>

        <tbody className={styles.tbody}>
          {loading ? (
            <tr>
              <td colSpan={columns.length + (selectable ? 1 : 0)} className={styles.loadingCell}>
                <div className={styles.loadingContainer}>
                  <div className={styles.spinner} />
                  <span>Loading...</span>
                </div>
              </td>
            </tr>
          ) : data.length === 0 ? (
            <tr>
              <td colSpan={columns.length + (selectable ? 1 : 0)} className={styles.emptyCell}>
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((row, rowIndex) => {
              const key = rowKey(row, rowIndex);
              const isSelected = selectedKeys.includes(key);

              const trClasses = [
                styles.tr,
                isSelected && styles.selected,
                onRowClick && styles.clickable,
              ]
                .filter(Boolean)
                .join(' ');

              return (
                <tr
                  key={key}
                  className={trClasses}
                  onClick={() => onRowClick && onRowClick(row, rowIndex)}
                >
                  {selectable && (
                    <td className={`${styles.td} ${styles.checkboxCell}`}>
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={(e) => {
                          e.stopPropagation();
                          handleRowSelect(key, e.target.checked);
                        }}
                        aria-label={`Select row ${rowIndex + 1}`}
                      />
                    </td>
                  )}

                  {columns.map((column) => {
                    const tdClasses = [
                      styles.td,
                      column.align && styles[`align-${column.align}`],
                      column.cellClassName,
                    ]
                      .filter(Boolean)
                      .join(' ');

                    const cellContent = column.render
                      ? column.render(row, rowIndex)
                      : column.dataKey
                      ? String((row as Record<string, unknown>)[column.dataKey] ?? '')
                      : '';

                    return (
                      <td key={column.key} className={tdClasses} style={{ width: column.width }}>
                        {cellContent}
                      </td>
                    );
                  })}
                </tr>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );
}

Table.displayName = 'Table';
