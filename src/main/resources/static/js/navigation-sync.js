/**
 * 主应用导航同步模块
 *
 * 职责:
 * 1. 监听主应用侧边栏点击 → 发送 main-nav-change 事件通知React应用
 * 2. 监听React应用导航变化 (react-nav-change事件) → 更新主应用侧边栏高亮状态
 *
 * 工作原理:
 * - 使用CustomEvent API实现跨应用通信
 * - 使用isSyncing标志 + 300ms超时防止循环触发
 * - 自动初始化,无需手动调用
 *
 * 调试:
 * window.__DEPLOY_PLATFORM_DEBUG__ = true; // 启用调试日志
 */

(function() {
  'use strict';

  /**
   * 导航同步管理器
   */
  class NavigationSyncManager {
    constructor() {
      // 防循环触发标志
      this.isSyncing = false;
      this.syncTimeout = null;

      // 路由映射: 主应用菜单项ID → React路由路径
      this.routeMap = {
        'deploy-platform-overview': '/overview',
        'deploy-platform-releases': '/releases',
        'deploy-platform-policies': '/settings/policies',
        // 未来可扩展更多路由
      };

      // 反向映射: React路由路径 → 主应用菜单项ID
      this.reverseRouteMap = {};
      Object.keys(this.routeMap).forEach(menuId => {
        this.reverseRouteMap[this.routeMap[menuId]] = menuId;
      });

      // 当前激活的路由 (用于去重)
      this.currentRoute = null;

      // 绑定方法上下文
      this.handleSidebarClick = this.handleSidebarClick.bind(this);
      this.handleReactNavChange = this.handleReactNavChange.bind(this);
    }

    /**
     * 设置同步标志并在300ms后重置
     * @param {boolean} value - 标志值
     */
    setSyncFlag(value) {
      this.isSyncing = value;

      if (value) {
        // 清除之前的超时
        if (this.syncTimeout) {
          clearTimeout(this.syncTimeout);
        }

        // 300ms后重置标志
        this.syncTimeout = setTimeout(() => {
          this.isSyncing = false;
          this.syncTimeout = null;

          if (window.__DEPLOY_PLATFORM_DEBUG__) {
            console.log('[NavigationSync] Sync flag reset');
          }
        }, 300);
      }
    }

    /**
     * 处理侧边栏菜单项点击
     * @param {Event} event - 点击事件
     */
    handleSidebarClick(event) {
      // 防循环: 如果正在同步中,忽略
      if (this.isSyncing) {
        if (window.__DEPLOY_PLATFORM_DEBUG__) {
          console.log('[NavigationSync] Ignoring sidebar click (syncing in progress)');
        }
        return;
      }

      // 查找被点击的菜单项 (向上遍历DOM树)
      let target = event.target;
      while (target && !target.dataset.route) {
        target = target.parentElement;
      }

      if (!target || !target.dataset.route) {
        return; // 不是有效的导航菜单项
      }

      const menuId = target.dataset.route;
      const route = this.routeMap[menuId];

      if (!route) {
        if (window.__DEPLOY_PLATFORM_DEBUG__) {
          console.warn('[NavigationSync] Unknown menu ID:', menuId);
        }
        return;
      }

      // 去重: 如果目标路由与当前路由相同,忽略
      if (route === this.currentRoute) {
        if (window.__DEPLOY_PLATFORM_DEBUG__) {
          console.log('[NavigationSync] Ignoring sidebar click (same route):', route);
        }
        return;
      }

      // 调试日志
      if (window.__DEPLOY_PLATFORM_DEBUG__) {
        console.log('[NavigationSync] Sidebar clicked:', {
          menuId,
          route,
          currentRoute: this.currentRoute
        });
      }

      // 设置同步标志
      this.setSyncFlag(true);

      // 发送导航事件到React应用
      const navEvent = new CustomEvent('main-nav-change', {
        detail: {
          route: route,
          source: 'main-app',
          timestamp: Date.now()
        }
      });

      window.dispatchEvent(navEvent);

      // 更新当前路由
      this.currentRoute = route;

      // 更新侧边栏高亮 (在同步标志下)
      this.updateSidebarHighlight(menuId);

      if (window.__DEPLOY_PLATFORM_DEBUG__) {
        console.log('[NavigationSync] Dispatched main-nav-change:', navEvent.detail);
      }
    }

    /**
     * 处理React应用导航变化
     * @param {CustomEvent} event - react-nav-change事件
     */
    handleReactNavChange(event) {
      // 验证事件数据
      if (!event.detail || !event.detail.route) {
        if (window.__DEPLOY_PLATFORM_DEBUG__) {
          console.warn('[NavigationSync] Invalid react-nav-change event:', event);
        }
        return;
      }

      const { route, source, timestamp } = event.detail;

      // 防循环: 如果正在同步中,忽略
      if (this.isSyncing) {
        if (window.__DEPLOY_PLATFORM_DEBUG__) {
          console.log('[NavigationSync] Ignoring react-nav-change (syncing in progress)');
        }
        return;
      }

      // 去重: 如果目标路由与当前路由相同,忽略
      if (route === this.currentRoute) {
        if (window.__DEPLOY_PLATFORM_DEBUG__) {
          console.log('[NavigationSync] Ignoring react-nav-change (same route):', route);
        }
        return;
      }

      // 调试日志
      if (window.__DEPLOY_PLATFORM_DEBUG__) {
        console.log('[NavigationSync] Received react-nav-change:', {
          route,
          source,
          timestamp,
          currentRoute: this.currentRoute
        });
      }

      // 查找对应的菜单项ID
      const menuId = this.reverseRouteMap[route];

      if (!menuId) {
        if (window.__DEPLOY_PLATFORM_DEBUG__) {
          console.warn('[NavigationSync] Unknown route:', route);
        }
        return;
      }

      // 设置同步标志
      this.setSyncFlag(true);

      // 更新当前路由
      this.currentRoute = route;

      // 更新侧边栏高亮
      this.updateSidebarHighlight(menuId);

      if (window.__DEPLOY_PLATFORM_DEBUG__) {
        console.log('[NavigationSync] Updated sidebar highlight:', menuId);
      }
    }

    /**
     * 更新侧边栏高亮状态
     * @param {string} activeMenuId - 要激活的菜单项ID
     */
    updateSidebarHighlight(activeMenuId) {
      // 移除所有菜单项的激活状态
      document.querySelectorAll('[data-route]').forEach(item => {
        item.classList.remove('active', 'bg-blue-50', 'text-blue-600');
        item.classList.add('text-gray-700');
      });

      // 激活指定菜单项
      const activeItem = document.querySelector(`[data-route="${activeMenuId}"]`);
      if (activeItem) {
        activeItem.classList.remove('text-gray-700');
        activeItem.classList.add('active', 'bg-blue-50', 'text-blue-600');
      }
    }

    /**
     * 初始化导航同步
     */
    init() {
      // 注册侧边栏点击事件监听器 (事件委托到document)
      document.addEventListener('click', this.handleSidebarClick);

      // 注册React导航变化监听器
      window.addEventListener('react-nav-change', this.handleReactNavChange);

      if (window.__DEPLOY_PLATFORM_DEBUG__) {
        console.log('[NavigationSync] Initialized');
      }
    }

    /**
     * 清理导航同步 (用于测试或卸载)
     */
    destroy() {
      document.removeEventListener('click', this.handleSidebarClick);
      window.removeEventListener('react-nav-change', this.handleReactNavChange);

      if (this.syncTimeout) {
        clearTimeout(this.syncTimeout);
      }

      if (window.__DEPLOY_PLATFORM_DEBUG__) {
        console.log('[NavigationSync] Destroyed');
      }
    }
  }

  // 自动初始化 (DOM加载完成后)
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      window.__navigationSyncManager = new NavigationSyncManager();
      window.__navigationSyncManager.init();
    });
  } else {
    // DOM已加载,立即初始化
    window.__navigationSyncManager = new NavigationSyncManager();
    window.__navigationSyncManager.init();
  }

  // 暴露到全局 (用于调试和测试)
  window.NavigationSyncManager = NavigationSyncManager;

})();
