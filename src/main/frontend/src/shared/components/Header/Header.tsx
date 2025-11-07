import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore, useUser, useUserPreferences } from '../../stores/authStore';
import { ROUTES } from '../../constants';
import styles from './Header.module.css';

interface HeaderProps {
  onMenuToggle?: () => void;
  showMenuButton?: boolean;
}

/**
 * Header 组件
 *
 * 顶部导航栏,包含:
 * - 移动端菜单切换按钮
 * - 主题切换
 * - 用户菜单(个人资料/退出登录)
 * - 通知中心(可选)
 *
 * @example
 * ```tsx
 * <Header onMenuToggle={toggleSidebar} showMenuButton={isMobile} />
 * ```
 */
export function Header({ onMenuToggle, showMenuButton = false }: HeaderProps): React.JSX.Element {
  const navigate = useNavigate();
  const user = useUser();
  const preferences = useUserPreferences();
  const { logout, updatePreferences } = useAuthStore();

  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);

  /**
   * 切换主题
   */
  const toggleTheme = (): void => {
    const newTheme = preferences.theme === 'light' ? 'dark' : 'light';
    updatePreferences({ theme: newTheme });

    // 更新文档根元素的主题属性
    document.documentElement.setAttribute('data-theme', newTheme);
  };

  /**
   * 处理退出登录
   */
  const handleLogout = async (): Promise<void> => {
    try {
      await logout();
      navigate(ROUTES.LOGIN);
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  /**
   * 关闭用户菜单(点击外部)
   */
  useEffect((): (() => void) | void => {
    const handleClickOutside = (event: MouseEvent): void => {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
        setUserMenuOpen(false);
      }
    };

    if (userMenuOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return (): void => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [userMenuOpen]);

  /**
   * ESC键关闭菜单
   */
  useEffect((): (() => void) | void => {
    const handleEscape = (event: KeyboardEvent): void => {
      if (event.key === 'Escape') {
        setUserMenuOpen(false);
      }
    };

    if (userMenuOpen) {
      document.addEventListener('keydown', handleEscape);
    }

    return (): void => {
      document.removeEventListener('keydown', handleEscape);
    };
  }, [userMenuOpen]);

  return (
    <header className={styles.header}>
      <div className={styles.headerContent}>
        {/* 左侧: 移动端菜单按钮 */}
        <div className={styles.headerLeft}>
          {showMenuButton && (
            <button
              className={styles.menuButton}
              onClick={onMenuToggle}
              aria-label="切换菜单"
            >
              <i data-lucide="menu" />
            </button>
          )}
        </div>

        {/* 右侧: 主题切换 + 用户菜单 */}
        <div className={styles.headerRight}>
          {/* 主题切换按钮 */}
          <button
            className={styles.iconButton}
            onClick={toggleTheme}
            aria-label={preferences.theme === 'light' ? '切换到暗黑模式' : '切换到明亮模式'}
          >
            <i data-lucide={preferences.theme === 'light' ? 'moon' : 'sun'} />
          </button>

          {/* 用户菜单 */}
          <div className={styles.userMenu} ref={userMenuRef}>
            <button
              className={styles.userMenuButton}
              onClick={() => { setUserMenuOpen(!userMenuOpen); }}
              aria-expanded={userMenuOpen}
              aria-haspopup="true"
            >
              <div className={styles.avatar}>
                {user?.displayName?.charAt(0) ?? user?.username?.charAt(0) ?? 'U'}
              </div>
              <span className={styles.userName}>
                {user?.displayName ?? user?.username ?? '用户'}
              </span>
              <i data-lucide="chevron-down" className={styles.chevron} />
            </button>

            {userMenuOpen && (
              <div className={styles.userDropdown}>
                <div className={styles.userDropdownHeader}>
                  <div className={styles.userInfo}>
                    <div className={styles.userName}>{user?.displayName ?? user?.username ?? '用户'}</div>
                    <div className={styles.userEmail}>{user?.email ?? ''}</div>
                  </div>
                </div>

                <div className={styles.userDropdownDivider} />

                <div className={styles.userDropdownMenu}>
                  <button
                    className={styles.menuItem}
                    onClick={() => {
                      setUserMenuOpen(false);
                      navigate(ROUTES.PROFILE);
                    }}
                  >
                    <i data-lucide="user" />
                    <span>个人资料</span>
                  </button>

                  <button
                    className={styles.menuItem}
                    onClick={() => {
                      setUserMenuOpen(false);
                      // TODO: 打开设置页面
                    }}
                  >
                    <i data-lucide="settings" />
                    <span>设置</span>
                  </button>
                </div>

                <div className={styles.userDropdownDivider} />

                <div className={styles.userDropdownMenu}>
                  <button
                    className={styles.menuItem}
                    onClick={() => {
                      void handleLogout();
                    }}
                  >
                    <i data-lucide="log-out" />
                    <span>退出登录</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
