import { Link } from 'react-router-dom';
import styles from './Breadcrumb.module.css';

/**
 * 面包屑项
 */
export interface BreadcrumbItem {
  /** 显示文本 */
  label: string;
  /** 链接路径（可选，最后一项通常没有链接） */
  href?: string;
}

/**
 * Breadcrumb 组件属性
 */
export interface BreadcrumbProps {
  /** 面包屑项列表 */
  items: BreadcrumbItem[];
  /** 分隔符 */
  separator?: React.ReactNode;
}

/**
 * Breadcrumb 组件
 *
 * 面包屑导航组件
 *
 * 功能:
 * - 显示当前页面路径
 * - 支持点击返回上级
 * - 自定义分隔符
 *
 * @example
 * ```tsx
 * <Breadcrumb
 *   items={[
 *     { label: '首页', href: '/' },
 *     { label: '服务器管理', href: '/admin/servers' },
 *     { label: 'server-001' }
 *   ]}
 * />
 * ```
 */
export function Breadcrumb({
  items,
  separator = <i data-lucide="chevron-right" />,
}: BreadcrumbProps): React.JSX.Element {
  return (
    <nav className={styles.breadcrumb} aria-label="面包屑导航">
      <ol className={styles.list}>
        {items.map((item, index) => {
          const isLast = index === items.length - 1;

          return (
            <li key={index} className={styles.item}>
              {item.href && !isLast ? (
                <Link to={item.href} className={styles.link}>
                  {item.label}
                </Link>
              ) : (
                <span className={isLast ? styles.current : styles.text}>{item.label}</span>
              )}
              {!isLast && <span className={styles.separator}>{separator}</span>}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
