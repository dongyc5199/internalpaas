import { useState, useEffect, type ReactNode } from 'react';
import { Navigation } from '../Navigation/Navigation';
import { Header } from '../Header/Header';
import { useUIStore } from '../../stores/uiStore';
import styles from './MainLayout.module.css';

interface MainLayoutProps {
  children: ReactNode;
}

/**
 * MainLayout 组件
 *
 * 主应用布局组件,包含:
 * - 左侧导航栏 (Navigation)
 * - 顶部标题栏 (Header)
 * - 主内容区域
 *
 * 特性:
 * - 响应式设计(移动端自动折叠)
 * - 侧边栏状态持久化
 * - 主题支持
 *
 * @example
 * ```tsx
 * <MainLayout>
 *   <DashboardPage />
 * </MainLayout>
 * ```
 */
export function MainLayout({ children }: MainLayoutProps): React.JSX.Element {
  const sidebarCollapsed = useUIStore((state) => state.sidebarCollapsed);
  const setSidebarCollapsed = useUIStore((state) => state.setSidebarCollapsed);
  const [isMobile, setIsMobile] = useState(false);

  /**
   * 检测移动端设备
   */
  useEffect(() => {
    const checkMobile = (): void => {
      setIsMobile(window.innerWidth < 768);
    };

    checkMobile();
    window.addEventListener('resize', checkMobile);

    return () => {
      window.removeEventListener('resize', checkMobile);
    };
  }, []);

  /**
   * 初始化 Lucide 图标
   * 注意: 不依赖children,避免在重渲染时与React的DOM操作冲突
   */
  useEffect(() => {
    // 延迟初始化图标,确保DOM已渲染
    const timer = setTimeout(() => {
      if (window.lucide) {
        window.lucide.createIcons();
      }
    }, 0);

    return () => { clearTimeout(timer); };
  }, []); // 只在组件挂载时初始化一次

  /**
   * 切换侧边栏
   */
  const handleSidebarToggle = (collapsed: boolean): void => {
    setSidebarCollapsed(collapsed);
  };

  /**
   * 移动端菜单切换
   */
  const handleMenuToggle = (): void => {
    setSidebarCollapsed(!sidebarCollapsed);
  };

  return (
    <div className={styles.layout}>
      {/* 侧边栏 */}
      <aside
        className={`${styles.sidebar} ${sidebarCollapsed ? styles.collapsed : ''} ${
          isMobile ? styles.mobile : ''
        }`}
      >
        <Navigation collapsed={sidebarCollapsed} onCollapse={handleSidebarToggle} />
      </aside>

      {/* 移动端遮罩 */}
      {isMobile && !sidebarCollapsed && (
        <div className={styles.overlay} onClick={() => { setSidebarCollapsed(true); }} />
      )}

      {/* 主内容区域 */}
      <div className={styles.main}>
        {/* 顶部标题栏 */}
        <Header onMenuToggle={handleMenuToggle} showMenuButton={isMobile} />

        {/* 内容区域 */}
        <main className={styles.content}>{children}</main>
      </div>
    </div>
  );
}
