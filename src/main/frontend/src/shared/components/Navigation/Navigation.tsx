import { useState, useMemo } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { UserRole } from '../../types/user';
import { useUser } from '../../stores/authStore';
import { ROUTES } from '../../constants';
import { Tooltip } from '../Tooltip';
import { Badge } from '../Badge';
import styles from './Navigation.module.css';

/**
 * 导航菜单项配置
 */
export interface NavItem {
  id: string;
  label: string;
  icon?: string;
  path?: string;
  roles?: UserRole[];
  children?: NavItem[];
  badge?: number; // 徽章数字
  badgeVariant?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
}

/**
 * 导航菜单配置
 * 根据用户角色显示不同的菜单项
 */
const NAV_ITEMS: NavItem[] = [
  {
    id: 'dashboard',
    label: '仪表板',
    icon: 'layout-dashboard',
    path: ROUTES.HOME,
    roles: [UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.DEVELOPER],
  },
  {
    id: 'applications',
    label: '应用管理',
    icon: 'package',
    path: ROUTES.APPLICATIONS.BASE,
    roles: [UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.DEVELOPER],
  },
  {
    id: 'terminal',
    label: 'SSH终端',
    icon: 'terminal',
    path: ROUTES.TERMINAL.BASE,
    roles: [UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.DEVELOPER],
  },
  {
    id: 'monitoring',
    label: '监控中心',
    icon: 'activity',
    roles: [UserRole.SUPER_ADMIN, UserRole.ADMIN],
    children: [
      {
        id: 'monitoring-overview',
        label: '监控概览',
        path: ROUTES.MONITORING.BASE,
      },
      {
        id: 'monitoring-history',
        label: '历史数据',
        path: ROUTES.MONITORING.HISTORY,
      },
      {
        id: 'monitoring-thresholds',
        label: '告警阈值',
        path: ROUTES.MONITORING.THRESHOLDS,
      },
    ],
  },
  {
    id: 'admin',
    label: '系统管理',
    icon: 'settings',
    roles: [UserRole.SUPER_ADMIN, UserRole.ADMIN],
    children: [
      {
        id: 'admin-servers',
        label: '服务器管理',
        path: ROUTES.ADMIN.SERVERS,
      },
      {
        id: 'admin-users',
        label: '用户管理',
        path: ROUTES.ADMIN.USERS,
        roles: [UserRole.SUPER_ADMIN],
      },
    ],
  },
];

interface NavigationProps {
  collapsed?: boolean;
  onCollapse?: (collapsed: boolean) => void;
}

/**
 * Navigation 组件
 *
 * 侧边栏导航组件,支持:
 * - 基于角色的菜单显示
 * - 二级菜单展开/收起
 * - 收起/展开侧边栏
 * - 活动项高亮
 *
 * @example
 * ```tsx
 * <Navigation collapsed={false} onCollapse={setCollapsed} />
 * ```
 */
export function Navigation({ collapsed = false, onCollapse }: NavigationProps): React.JSX.Element {
  const user = useUser();
  const location = useLocation();
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set());
  const [searchQuery, setSearchQuery] = useState('');
  const [_showSearch, setShowSearch] = useState(false);

  /**
   * 检查用户是否有权限查看菜单项
   */
  const hasPermission = (item: NavItem): boolean => {
    if (!item.roles) return true;
    if (!user) return false;
    return item.roles.includes(user.role);
  };

  /**
   * 过滤菜单项(根据权限)
   */
  const filterItems = (items: NavItem[]): NavItem[] => {
    return items
      .filter(hasPermission)
      .map((item) => ({
        ...item,
        children: item.children ? filterItems(item.children) : undefined,
      }))
      .filter((item) => !item.children || item.children.length > 0);
  };

  /**
   * 切换二级菜单展开状态
   */
  const toggleExpanded = (itemId: string): void => {
    setExpandedItems((prev) => {
      const next = new Set(prev);
      if (next.has(itemId)) {
        next.delete(itemId);
      } else {
        next.add(itemId);
      }
      return next;
    });
  };

  /**
   * 检查菜单项是否激活
   */
  const isActive = (item: NavItem): boolean => {
    if (item.path) {
      return location.pathname === item.path || location.pathname.startsWith(item.path + '/');
    }
    if (item.children) {
      return item.children.some(isActive);
    }
    return false;
  };

  /**
   * 搜索菜单项(递归)
   */
  const searchMenuItems = (items: NavItem[], query: string): NavItem[] => {
    if (!query.trim()) return items;

    const lowerQuery = query.toLowerCase();
    const results: NavItem[] = [];

    for (const item of items) {
      // 检查当前项是否匹配
      const matchesCurrent = item.label.toLowerCase().includes(lowerQuery);

      // 检查子项是否匹配
      let matchingChildren: NavItem[] | undefined;
      if (item.children) {
        matchingChildren = searchMenuItems(item.children, query);
      }

      // 如果当前项或子项匹配,添加到结果
      if (matchesCurrent || (matchingChildren && matchingChildren.length > 0)) {
        results.push({
          ...item,
          children: matchingChildren,
        });
      }
    }

    return results;
  };

  /**
   * 过滤后的菜单项
   */
  const filteredAndSearchedItems = useMemo((): NavItem[] => {
    const filtered = filterItems(NAV_ITEMS);
    return searchMenuItems(filtered, searchQuery);
  }, [searchQuery]);

  /**
   * 渲染菜单项图标
   */
  const renderIcon = (iconName?: string): React.JSX.Element | null => {
    if (!iconName) return null;
    return <i data-lucide={iconName} className={styles.icon} />;
  };

  /**
   * 渲染一级菜单项
   */
  const renderNavItem = (item: NavItem): React.JSX.Element => {
    const isExpanded = expandedItems.has(item.id);
    const active = isActive(item);
    const hasChildren = item.children && item.children.length > 0;
    const hasBadge = item.badge !== undefined && item.badge > 0;

    // 菜单内容
    const menuContent = (
      <>
        {renderIcon(item.icon)}
        {!collapsed && (
          <>
            <span className={styles.label}>{item.label}</span>
            {hasBadge && (
              <Badge count={item.badge} variant={item.badgeVariant || 'danger'} />
            )}
          </>
        )}
      </>
    );

    if (hasChildren) {
      return (
        <div key={item.id} className={styles.navGroup}>
          <Tooltip content={item.label} placement="right" disabled={!collapsed}>
            <button
              className={`${styles.navLink} ${active ? styles.active : ''} ${styles.expandable}`}
              onClick={() => { toggleExpanded(item.id); }}
              aria-expanded={isExpanded}
            >
              {menuContent}
              {!collapsed && (
                <i
                  data-lucide={isExpanded ? 'chevron-down' : 'chevron-right'}
                  className={styles.chevron}
                />
              )}
            </button>
          </Tooltip>
          {!collapsed && isExpanded && (
            <div className={styles.subMenu}>{item.children?.map(renderSubItem)}</div>
          )}
        </div>
      );
    }

    return (
      <Tooltip key={item.id} content={item.label} placement="right" disabled={!collapsed}>
        <NavLink
          to={item.path || '#'}
          className={({ isActive }) =>
            `${styles.navLink} ${isActive ? styles.active : ''}`
          }
        >
          {menuContent}
        </NavLink>
      </Tooltip>
    );
  };

  /**
   * 渲染二级菜单项
   */
  const renderSubItem = (item: NavItem): React.JSX.Element => {
    return (
      <NavLink
        key={item.id}
        to={item.path || '#'}
        className={({ isActive }) =>
          `${styles.subNavLink} ${isActive ? styles.active : ''}`
        }
      >
        <span className={styles.label}>{item.label}</span>
      </NavLink>
    );
  };

  return (
    <nav className={`${styles.navigation} ${collapsed ? styles.collapsed : ''}`}>
      <div className={styles.navHeader}>
        <div className={styles.logo}>
          {!collapsed && <span className={styles.logoText}>Dev Debug</span>}
          {collapsed && <span className={styles.logoIcon}>DD</span>}
        </div>
        <button
          className={styles.collapseBtn}
          onClick={() => onCollapse?.(!collapsed)}
          aria-label={collapsed ? '展开侧边栏' : '收起侧边栏'}
        >
          <i data-lucide={collapsed ? 'chevrons-right' : 'chevrons-left'} />
        </button>
      </div>

      {/* 搜索框 (展开状态显示) */}
      {!collapsed && (
        <div className={styles.searchBox}>
          <div className={styles.searchInputWrapper}>
            <i data-lucide="search" className={styles.searchIcon} />
            <input
              type="text"
              className={styles.searchInput}
              placeholder="搜索菜单... (Ctrl+K)"
              value={searchQuery}
              onChange={(e) => { setSearchQuery(e.target.value); }}
              onFocus={() => { setShowSearch(true); }}
              onBlur={() => {
                // 延迟关闭,允许点击搜索结果
                setTimeout(() => { setShowSearch(false); }, 200);
              }}
            />
            {searchQuery && (
              <button
                className={styles.searchClear}
                onClick={() => { setSearchQuery(''); }}
                aria-label="清除搜索"
              >
                <i data-lucide="x" />
              </button>
            )}
          </div>
        </div>
      )}

      <div className={styles.navContent}>
        <div className={styles.navItems}>
          {filteredAndSearchedItems.length > 0 ? (
            filteredAndSearchedItems.map(renderNavItem)
          ) : (
            <div className={styles.emptyState}>
              <i data-lucide="search-x" />
              <span>未找到匹配的菜单</span>
            </div>
          )}
        </div>
      </div>

      {!collapsed && (
        <div className={styles.navFooter}>
          <div className={styles.userInfo}>
            <div className={styles.avatar}>
              {user?.displayName?.charAt(0) ?? user?.username?.charAt(0) ?? 'U'}
            </div>
            <div className={styles.userDetails}>
              <div className={styles.userName}>{user?.displayName ?? user?.username ?? '用户'}</div>
              <div className={styles.userRole}>
                {user?.role === UserRole.SUPER_ADMIN
                  ? '超级管理员'
                  : user?.role === UserRole.ADMIN
                    ? '管理员'
                    : '开发者'}
              </div>
            </div>
          </div>
        </div>
      )}
    </nav>
  );
}
