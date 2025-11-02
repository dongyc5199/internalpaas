import { useEffect, useRef } from 'react';
import type { NavigateFunction, Location } from 'react-router-dom';
import type { NavigationEventDetail } from '../types/navigation';

/**
 * 导航同步Hook
 *
 * 实现主应用与React应用之间的双向导航同步:
 * 1. 监听主应用发送的 main-nav-change 事件 → 触发React Router导航
 * 2. 监听React Router location变化 → 向主应用发送 react-nav-change 事件
 *
 * 关键功能:
 * - CustomEvent机制实现跨应用通信
 * - 防循环触发机制 (isSyncing标志 + 300ms超时)
 * - 智能去重 (仅在pathname变化时触发)
 *
 * @param navigate - React Router的navigate函数
 * @param location - React Router的location对象
 *
 * @example
 * ```tsx
 * function App() {
 *   const navigate = useNavigate();
 *   const location = useLocation();
 *
 *   // 启用导航同步
 *   useNavSync(navigate, location);
 *
 *   return <Routes>...</Routes>;
 * }
 * ```
 */
export function useNavSync(navigate: NavigateFunction, location: Location): void {
  // 使用ref而非state,避免触发重渲染
  const isSyncingRef = useRef(false);
  const prevPathnameRef = useRef(location.pathname);
  const syncTimeoutRef = useRef<number | null>(null);

  // 防循环触发: 设置同步标志并在300ms后重置
  const setSyncFlag = (value: boolean): void => {
    isSyncingRef.current = value;

    if (value) {
      // 清除之前的超时
      if (syncTimeoutRef.current !== null) {
        window.clearTimeout(syncTimeoutRef.current);
      }

      // 300ms后重置标志
      syncTimeoutRef.current = window.setTimeout(() => {
        isSyncingRef.current = false;
        syncTimeoutRef.current = null;

        if (window.__DEPLOY_PLATFORM_DEBUG__) {
          console.log('[useNavSync] Sync flag reset');
        }
      }, 300);
    }
  };

  // 效果1: 监听主应用导航事件 → 触发React导航
  useEffect(() => {
    const handleMainNavChange = (event: Event): void => {
      const customEvent = event as CustomEvent<NavigationEventDetail>;

      // 验证事件数据
      if (!customEvent.detail || !customEvent.detail.route) {
        if (window.__DEPLOY_PLATFORM_DEBUG__) {
          console.warn('[useNavSync] Invalid main-nav-change event:', customEvent);
        }
        return;
      }

      const { route, source, timestamp } = customEvent.detail;

      // 防循环: 如果正在同步中,忽略事件
      if (isSyncingRef.current) {
        if (window.__DEPLOY_PLATFORM_DEBUG__) {
          console.log('[useNavSync] Ignoring main-nav-change (syncing in progress)');
        }
        return;
      }

      // 去重: 如果目标路由与当前路由相同,忽略
      if (route === location.pathname) {
        if (window.__DEPLOY_PLATFORM_DEBUG__) {
          console.log('[useNavSync] Ignoring main-nav-change (same route):', route);
        }
        return;
      }

      // 调试日志
      if (window.__DEPLOY_PLATFORM_DEBUG__) {
        console.log('[useNavSync] Received main-nav-change:', {
          route,
          source,
          timestamp,
          currentPathname: location.pathname
        });
      }

      // 设置同步标志
      setSyncFlag(true);

      // 执行导航
      try {
        navigate(route);

        if (window.__DEPLOY_PLATFORM_DEBUG__) {
          console.log('[useNavSync] Navigated to:', route);
        }
      } catch (error) {
        console.error('[useNavSync] Navigation error:', error);
        // 导航失败时重置标志
        isSyncingRef.current = false;
      }
    };

    // 注册事件监听器
    window.addEventListener('main-nav-change', handleMainNavChange);

    if (window.__DEPLOY_PLATFORM_DEBUG__) {
      console.log('[useNavSync] Registered main-nav-change listener');
    }

    // 清理函数
    return () => {
      window.removeEventListener('main-nav-change', handleMainNavChange);

      // 清除超时
      if (syncTimeoutRef.current !== null) {
        window.clearTimeout(syncTimeoutRef.current);
      }

      if (window.__DEPLOY_PLATFORM_DEBUG__) {
        console.log('[useNavSync] Unregistered main-nav-change listener');
      }
    };
  }, [navigate, location.pathname]); // 依赖项: navigate和当前pathname

  // 效果2: React location变化 → 向主应用发送事件
  useEffect(() => {
    const currentPathname = location.pathname;

    // 去重: 仅在pathname变化时触发
    if (currentPathname === prevPathnameRef.current) {
      return;
    }

    // 防循环: 如果正在同步中,不发送事件(避免回环)
    if (isSyncingRef.current) {
      if (window.__DEPLOY_PLATFORM_DEBUG__) {
        console.log('[useNavSync] Skipping react-nav-change dispatch (syncing in progress)');
      }

      // 更新prev pathname但不发送事件
      prevPathnameRef.current = currentPathname;
      return;
    }

    // 调试日志
    if (window.__DEPLOY_PLATFORM_DEBUG__) {
      console.log('[useNavSync] Pathname changed:', {
        from: prevPathnameRef.current,
        to: currentPathname
      });
    }

    // 发送react-nav-change事件
    const event = new CustomEvent<NavigationEventDetail>('react-nav-change', {
      detail: {
        route: currentPathname,
        source: 'react',
        timestamp: Date.now()
      }
    });

    window.dispatchEvent(event);

    if (window.__DEPLOY_PLATFORM_DEBUG__) {
      console.log('[useNavSync] Dispatched react-nav-change:', event.detail);
    }

    // 更新prev pathname
    prevPathnameRef.current = currentPathname;
  }, [location.pathname]); // 依赖项: 仅pathname变化时触发
}
