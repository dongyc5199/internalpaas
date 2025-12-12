import { useState } from 'react';
import styles from './Table.module.css';

export interface Column<T> {
  /** 列键 */
  key: string;
  /** 列标题 */
  title: string;
  /** 渲染函数 */
  render?: (value: unknown, record: T, index: number) => React.ReactNode;
  /** 是否可排序 */
  sortable?: boolean;
  /** 宽度 */
  width?: string | number;
}

export interface RowSelection<T> {
  /** 已选中的行键列表 */
  selectedRowKeys: (string | number)[];
  /** 选中状态改变的回调 */
  onChange: (selectedRowKeys: (string | number)[], selectedRows: T[]) => void;
  /** 选择类型 */
  type?: 'checkbox' | 'radio';
}

export interface TableProps<T> {
  /** 数据源 */
  data?: T[];
  /** 列配置 */
  columns: Column<T>[];
  /** 行键提取函数 */
  rowKey?: (record: T) => string | number;
  /** 是否显示分页 */
  pagination?: boolean;
  /** 每页条数 */
  pageSize?: number;
  /** 空数据提示 */
  emptyText?: string;
  /** 加载状态 */
  loading?: boolean;
  /** 自定义类名 */
  className?: string;
  /** 行选择配置 */
  rowSelection?: RowSelection<T>;
}

type SortOrder = 'asc' | 'desc' | null;

/**
 * Table 组件
 *
 * 通用表格组件,支持排序和分页
 *
 * @example
 * ```tsx
 * interface User {
 *   id: number;
 *   name: string;
 *   email: string;
 * }
 *
 * const columns: Column<User>[] = [
 *   { key: 'id', title: 'ID', sortable: true },
 *   { key: 'name', title: '姓名' },
 *   {
 *     key: 'email',
 *     title: '邮箱',
 *     render: (value) => <a href={`mailto:${value}`}>{value}</a>
 *   },
 * ];
 *
 * <Table data={users} columns={columns} rowKey={(user) => user.id} />
 * ```
 */
export function Table<T extends Record<string, unknown>>({
  data,
  columns,
  rowKey = (record) => String((record as Record<string, unknown>).id),
  pagination = true,
  pageSize = 10,
  emptyText = '暂无数据',
  loading = false,
  className,
  rowSelection,
}: TableProps<T>): JSX.Element {
  const [currentPage, setCurrentPage] = useState(1);
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortOrder, setSortOrder] = useState<SortOrder>(null);

  // 排序逻辑
  const handleSort = (key: string): void => {
    if (sortKey === key) {
      setSortOrder((prev) => {
        if (prev === 'asc') return 'desc';
        if (prev === 'desc') return null;
        return 'asc';
      });
    } else {
      setSortKey(key);
      setSortOrder('asc');
    }
  };

  // 应用排序 (防御性检查: 确保data不为undefined)
  const sortedData = [...(data ?? [])];
  if (sortKey && sortOrder) {
    sortedData.sort((a, b) => {
      const aValue = a[sortKey] as string | number;
      const bValue = b[sortKey] as string | number;

      if (aValue === bValue) return 0;
      const comparison = aValue < bValue ? -1 : 1;
      return sortOrder === 'asc' ? comparison : -comparison;
    });
  }

  // 应用分页
  const totalPages = pagination ? Math.ceil(sortedData.length / pageSize) : 1;
  const paginatedData = pagination
    ? sortedData.slice((currentPage - 1) * pageSize, currentPage * pageSize)
    : sortedData;

  // 分页控制
  const goToPage = (page: number): void => {
    setCurrentPage(Math.max(1, Math.min(page, totalPages)));
  };

  // 行选择逻辑
  const handleRowSelect = (record: T, checked: boolean): void => {
    if (!rowSelection) return;

    const key = rowKey(record);
    const newSelectedKeys = checked
      ? [...rowSelection.selectedRowKeys, key]
      : rowSelection.selectedRowKeys.filter((k) => k !== key);

    const selectedRows = sortedData.filter((row) =>
      newSelectedKeys.includes(rowKey(row))
    );

    rowSelection.onChange(newSelectedKeys, selectedRows);
  };

  const handleSelectAll = (checked: boolean): void => {
    if (!rowSelection) return;

    const newSelectedKeys = checked
      ? paginatedData.map((record) => rowKey(record))
      : [];

    const selectedRows = checked ? paginatedData : [];
    rowSelection.onChange(newSelectedKeys, selectedRows);
  };

  const isRowSelected = (record: T): boolean => {
    if (!rowSelection) return false;
    return rowSelection.selectedRowKeys.includes(rowKey(record));
  };

  const isAllSelected = (): boolean => {
    if (!rowSelection || paginatedData.length === 0) return false;
    return paginatedData.every((record) => isRowSelected(record));
  };

  const isIndeterminate = (): boolean => {
    if (!rowSelection || paginatedData.length === 0) return false;
    const selectedCount = paginatedData.filter((record) => isRowSelected(record)).length;
    return selectedCount > 0 && selectedCount < paginatedData.length;
  };

  const wrapperClassName = [styles.wrapper, className].filter(Boolean).join(' ');

  return (
    <div className={wrapperClassName}>
      <table className={styles.table}>
        <thead className={styles.thead}>
          <tr>
            {rowSelection && (
              <th className={`${styles.th} ${styles.checkboxColumn}`}>
                <input
                  type="checkbox"
                  checked={isAllSelected()}
                  ref={(input) => {
                    if (input) {
                      input.indeterminate = isIndeterminate();
                    }
                  }}
                  onChange={(e) => { handleSelectAll(e.target.checked); }}
                  className={styles.checkbox}
                  aria-label="全选"
                />
              </th>
            )}
            {columns.map((column) => (
              <th
                key={column.key}
                className={`${styles.th} ${column.sortable ? styles.sortable : ''}`}
                style={{ width: column.width }}
                onClick={column.sortable ? () => { handleSort(column.key); } : undefined}
              >
                {column.title}
                {column.sortable && sortKey === column.key && (
                  <span className={styles.sortIndicator}>
                    {sortOrder === 'asc' ? '↑' : sortOrder === 'desc' ? '↓' : ''}
                  </span>
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className={styles.tbody}>
          {loading ? (
            <tr>
              <td colSpan={columns.length + (rowSelection ? 1 : 0)} className={styles.empty}>
                加载中...
              </td>
            </tr>
          ) : paginatedData.length === 0 ? (
            <tr>
              <td colSpan={columns.length + (rowSelection ? 1 : 0)} className={styles.empty}>
                {emptyText}
              </td>
            </tr>
          ) : (
            paginatedData.map((record, index) => (
              <tr key={rowKey(record)} className={styles.tr}>
                {rowSelection && (
                  <td className={`${styles.td} ${styles.checkboxColumn}`}>
                    <input
                      type="checkbox"
                      checked={isRowSelected(record)}
                      onChange={(e) => { handleRowSelect(record, e.target.checked); }}
                      className={styles.checkbox}
                      aria-label={`选择第${index + 1}行`}
                    />
                  </td>
                )}
                {columns.map((column) => (
                  <td key={column.key} className={styles.td}>
                    {column.render
                      ? column.render(record[column.key], record, index)
                      : String(record[column.key] ?? '')}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>

      {pagination && totalPages > 1 && (
        <div className={styles.pagination}>
          <div className={styles.paginationInfo}>
            第 {(currentPage - 1) * pageSize + 1}-
            {Math.min(currentPage * pageSize, data.length)} 条，共 {data.length} 条
          </div>
          <div className={styles.paginationControls}>
            <button
              className={styles.pageButton}
              onClick={() => { goToPage(currentPage - 1); }}
              disabled={currentPage === 1}
              aria-label="上一页"
            >
              ‹
            </button>
            {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
              let pageNum: number;
              if (totalPages <= 5) {
                pageNum = i + 1;
              } else if (currentPage <= 3) {
                pageNum = i + 1;
              } else if (currentPage >= totalPages - 2) {
                pageNum = totalPages - 4 + i;
              } else {
                pageNum = currentPage - 2 + i;
              }

              return (
                <button
                  key={pageNum}
                  className={`${styles.pageButton} ${currentPage === pageNum ? styles.active : ''}`}
                  onClick={() => { goToPage(pageNum); }}
                >
                  {pageNum}
                </button>
              );
            })}
            <button
              className={styles.pageButton}
              onClick={() => { goToPage(currentPage + 1); }}
              disabled={currentPage === totalPages}
              aria-label="下一页"
            >
              ›
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

Table.displayName = 'Table';
