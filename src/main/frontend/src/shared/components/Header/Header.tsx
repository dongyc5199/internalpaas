import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore, useUser, useUserPreferences } from '../../stores/authStore';
import { ROUTES } from '../../constants';
import { Badge } from '../Badge';
import { Tooltip } from '../Tooltip';
import styles from './Header.module.css';

interface HeaderProps {
  onMenuToggle?: () => void;
  showMenuButton?: boolean;
  notificationCount?: number;
  onSearch?: (query: string) => void;
}

/**
 * 通知数据类型
 */
interface Notification {
  id: string;
  type: 'info' | 'success' | 'warning' | 'error';
  title: string;
  message: string;
  time: string;
  read: boolean;
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
export function Header({
  onMenuToggle,
  showMenuButton = false,
  notificationCount: _notificationCount = 0,
  onSearch,
}: HeaderProps): React.JSX.Element {
  const navigate = useNavigate();
  const user = useUser();
  const preferences = useUserPreferences();
  const { logout, updatePreferences } = useAuthStore();

  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [notificationMenuOpen, setNotificationMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const userMenuRef = useRef<HTMLDivElement>(null);
  const notificationMenuRef = useRef<HTMLDivElement>(null);
  const searchRef = useRef<HTMLDivElement>(null);

  // 模拟通知数据(后续可以从API获取)
  const [notifications] = useState<Notification[]>([
    {
      id: '1',
      type: 'warning',
      title: '服务器告警',
      message: 'Server-01 CPU使用率超过90%',
      time: '5分钟前',
      read: false,
    },
    {
      id: '2',
      type: 'info',
      title: '系统更新',
      message: '新版本已发布,请及时更新',
      time: '1小时前',
      read: false,
    },
    {
      id: '3',
      type: 'success',
      title: '部署成功',
      message: 'App-Demo 已成功部署到生产环境',
      time: '2小时前',
      read: true,
    },
  ]);

  // 未读通知数量
  const unreadCount = notifications.filter(n => !n.read).length;

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
   * 处理搜索
   */
  const handleSearch = (): void => {
    if (searchQuery.trim() && onSearch) {
      onSearch(searchQuery);
    }
  };

  /**
   * 获取通知图标
   */
  const getNotificationIcon = (type: Notification['type']): string => {
    switch (type) {
      case 'success':
        return 'check-circle';
      case 'warning':
        return 'alert-triangle';
      case 'error':
        return 'x-circle';
      default:
        return 'info';
    }
  };

  /**
   * 获取通知颜色类
   */
  const getNotificationClass = (type: Notification['type']): string => {
    return styles[`notification${type.charAt(0).toUpperCase() + type.slice(1)}`] || '';
  };

  /**
   * 关闭所有菜单(点击外部)
   */
  useEffect((): (() => void) | void => {
    const handleClickOutside = (event: MouseEvent): void => {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
        setUserMenuOpen(false);
      }
      if (
        notificationMenuRef.current &&
        !notificationMenuRef.current.contains(event.target as Node)
      ) {
        setNotificationMenuOpen(false);
      }
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setSearchOpen(false);
      }
    };

    if (userMenuOpen || notificationMenuOpen || searchOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return (): void => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [userMenuOpen, notificationMenuOpen, searchOpen]);

  /**
   * 键盘快捷键
   */
  useEffect((): (() => void) | void => {
    const handleKeyboard = (event: KeyboardEvent): void => {
      // ESC键关闭所有菜单
      if (event.key === 'Escape') {
        setUserMenuOpen(false);
        setNotificationMenuOpen(false);
        setSearchOpen(false);
      }

      // Ctrl+K 打开搜索
      if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
        event.preventDefault();
        setSearchOpen(true);
      }

      // Enter键执行搜索
      if (event.key === 'Enter' && searchOpen) {
        handleSearch();
      }
    };

    document.addEventListener('keydown', handleKeyboard);

    return (): void => {
      document.removeEventListener('keydown', handleKeyboard);
    };
  }, [userMenuOpen, notificationMenuOpen, searchOpen, searchQuery]);

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

        {/* 右侧: 搜索 + 通知 + 主题切换 + 用户菜单 */}
        <div className={styles.headerRight}>
          {/* 搜索按钮 */}
          <Tooltip content="搜索 (Ctrl+K)" placement="bottom">
            <button
              className={styles.iconButton}
              onClick={() => { setSearchOpen(!searchOpen); }}
              aria-label="打开搜索"
            >
              <i data-lucide="search" />
            </button>
          </Tooltip>

          {/* 搜索弹窗 */}
          {searchOpen && (
            <div className={styles.searchModal} ref={searchRef}>
              <div className={styles.searchModalContent}>
                <div className={styles.searchInputWrapper}>
                  <i data-lucide="search" className={styles.searchIcon} />
                  <input
                    type="text"
                    className={styles.searchInput}
                    placeholder="搜索..."
                    value={searchQuery}
                    onChange={(e) => { setSearchQuery(e.target.value); }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        handleSearch();
                      }
                    }}
                    autoFocus
                  />
                  {searchQuery && (
                    <button
                      className={styles.searchClear}
                      onClick={() => { setSearchQuery(''); }}
                    >
                      <i data-lucide="x" />
                    </button>
                  )}
                </div>
                <div className={styles.searchHint}>
                  <span>按 Enter 搜索</span>
                  <span>ESC 关闭</span>
                </div>
              </div>
            </div>
          )}

          {/* 通知按钮 */}
          <div className={styles.notificationMenu} ref={notificationMenuRef}>
            <Tooltip content="通知" placement="bottom">
              <button
                className={styles.iconButton}
                onClick={() => { setNotificationMenuOpen(!notificationMenuOpen); }}
                aria-label="打开通知"
                aria-expanded={notificationMenuOpen}
              >
                <Badge count={unreadCount} variant="danger">
                  <i data-lucide="bell" />
                </Badge>
              </button>
            </Tooltip>

            {/* 通知下拉菜单 */}
            {notificationMenuOpen && (
              <div className={styles.notificationDropdown}>
                <div className={styles.notificationHeader}>
                  <h3>通知</h3>
                  {unreadCount > 0 && (
                    <button className={styles.markAllRead}>全部标为已读</button>
                  )}
                </div>

                <div className={styles.notificationList}>
                  {notifications.length === 0 ? (
                    <div className={styles.emptyNotifications}>
                      <i data-lucide="inbox" />
                      <span>暂无通知</span>
                    </div>
                  ) : (
                    notifications.map((notification) => (
                      <div
                        key={notification.id}
                        className={`${styles.notificationItem} ${
                          !notification.read ? styles.unread : ''
                        } ${getNotificationClass(notification.type)}`}
                      >
                        <div className={styles.notificationIcon}>
                          <i data-lucide={getNotificationIcon(notification.type)} />
                        </div>
                        <div className={styles.notificationContent}>
                          <div className={styles.notificationTitle}>{notification.title}</div>
                          <div className={styles.notificationMessage}>{notification.message}</div>
                          <div className={styles.notificationTime}>{notification.time}</div>
                        </div>
                        {!notification.read && <div className={styles.unreadDot} />}
                      </div>
                    ))
                  )}
                </div>

                <div className={styles.notificationFooter}>
                  <button
                    onClick={() => {
                      setNotificationMenuOpen(false);
                      // 跳转到通知页面
                    }}
                  >
                    查看全部通知
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* 主题切换按钮 */}
          <Tooltip
            content={preferences.theme === 'light' ? '切换到暗黑模式' : '切换到明亮模式'}
            placement="bottom"
          >
            <button className={styles.iconButton} onClick={toggleTheme} aria-label="切换主题">
              <i data-lucide={preferences.theme === 'light' ? 'moon' : 'sun'} />
            </button>
          </Tooltip>

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
