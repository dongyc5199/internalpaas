import { ReactNode } from 'react';
import styles from './EmptyLayout.module.css';

interface EmptyLayoutProps {
  children: ReactNode;
}

/**
 * EmptyLayout 组件
 *
 * 空白布局,用于登录、注册等不需要导航栏的页面
 *
 * @example
 * ```tsx
 * <EmptyLayout>
 *   <LoginPage />
 * </EmptyLayout>
 * ```
 */
export function EmptyLayout({ children }: EmptyLayoutProps): React.JSX.Element {
  return (
    <div className={styles.layout}>
      <main className={styles.content}>{children}</main>
    </div>
  );
}
