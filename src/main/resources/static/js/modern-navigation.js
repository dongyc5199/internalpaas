/*
===============================================
🎯 MODERN NAVIGATION CONTROLLER
===============================================
现代化导航栏JavaScript控制器 - Dev Debug Platform
特性: 响应式设计、主题切换、流畅动画、无障碍支持
版本: 1.0.0
更新: 2025-09-17
===============================================
*/

class ModernNavigationController {
    constructor(options = {}) {
        // 配置选项
        this.config = {
            // 🎯 选择器配置
            selectors: {
                sidebar: '.modern-sidebar',
                toggle: '.modern-sidebar-toggle',
                contentArea: '.main-content',
                themeToggle: '.theme-toggle-btn',
                navLinks: '.modern-nav-link',
                body: 'body',
                html: 'html',
                overlay: '.mobile-nav-overlay'
            },
            
            // 📱 响应式断点
            breakpoints: {
                mobile: 768,
                tablet: 1024
            },
            
            // 💾 存储键名
            storageKeys: {
                sidebarState: 'modern-nav-sidebar-state',
                theme: 'modern-nav-theme',
                activeItem: 'modern-nav-active-item'
            },
            
            // ⏱️ 动画延迟
            delays: {
                transition: 350,
                themeSwitch: 200,
                debounce: 100
            },
            
            // 🎨 主题配置
            themes: {
                light: 'theme-light',
                dark: 'theme-dark'
            },
            
            // 合并用户选项
            ...options
        };

        // 🔄 状态管理
        this.state = {
            sidebarCollapsed: false,
            currentTheme: 'light',
            isMobile: false,
            isTablet: false,
            activeNavItem: null,
            isTransitioning: false,
            touchStartX: 0,
            touchStartY: 0
        };

        // 📱 设备检测
        this.deviceDetection = {
            isTouchDevice: 'ontouchstart' in window || navigator.maxTouchPoints > 0,
            isIOS: /iPad|iPhone|iPod/.test(navigator.userAgent),
            isAndroid: /Android/.test(navigator.userAgent),
            supportsPassive: this.checkPassiveSupport()
        };

        // 初始化
        this.init();
    }

    /**
     * 🚀 初始化导航控制器
     */
    init() {
        try {
            //console.log('🚀 正在初始化现代化导航系统...');
            
            this.bindElements();
            this.loadStoredStates();
            this.setupEventListeners();
            this.updateResponsiveState();
            this.initializeTheme();
            this.setupAccessibility();
            this.performanceOptimization();
            
            //console.log('✅ 现代化导航系统初始化完成');
            
            // 触发初始化完成事件
            this.dispatchCustomEvent('navigationInitialized', {
                state: this.state,
                config: this.config
            });
            
        } catch (error) {
            console.error('❌ 导航系统初始化失败:', error);
            this.handleError(error, 'init');
        }
    }

    /**
     * 🎯 绑定DOM元素
     */
    bindElements() {
        this.elements = {};
        
        Object.entries(this.config.selectors).forEach(([key, selector]) => {
            const element = document.querySelector(selector);
            if (key === 'navLinks') {
                this.elements[key] = document.querySelectorAll(selector);
            } else {
                this.elements[key] = element;
                if (!element && key !== 'overlay') {
                    console.warn(`⚠️  未找到元素: ${selector}`);
                }
            }
        });
    }

    /**
     * 💾 加载存储的状态
     */
    loadStoredStates() {
        try {
            // 加载侧边栏状态
            const storedSidebarState = localStorage.getItem(this.config.storageKeys.sidebarState);
            if (storedSidebarState !== null) {
                this.state.sidebarCollapsed = JSON.parse(storedSidebarState);
            }

            // 加载主题状态
            const storedTheme = localStorage.getItem(this.config.storageKeys.theme);
            if (storedTheme && this.config.themes[storedTheme]) {
                this.state.currentTheme = storedTheme;
            }

            // 加载活跃导航项
            const storedActiveItem = localStorage.getItem(this.config.storageKeys.activeItem);
            if (storedActiveItem) {
                this.state.activeNavItem = storedActiveItem;
            }

            //console.log('💾 已加载存储状态:', this.state);
        } catch (error) {
            console.warn('⚠️  加载存储状态失败:', error);
        }
    }

    /**
     * 🎛️ 设置事件监听器
     */
    setupEventListeners() {
        // 侧边栏切换
        if (this.elements.toggle) {
            this.elements.toggle.addEventListener('click', (e) => {
                e.preventDefault();
                this.toggleSidebar();
            });
        }

        // 主题切换
        if (this.elements.themeToggle) {
            this.elements.themeToggle.addEventListener('click', (e) => {
                e.preventDefault();
                this.toggleTheme();
            });
        }

        // 导航链接点击
        this.elements.navLinks?.forEach(link => {
            link.addEventListener('click', (e) => {
                this.handleNavItemClick(e, link);
            });
        });

        // 响应式监听
        window.addEventListener('resize', this.debounce(() => {
            this.updateResponsiveState();
        }, this.config.delays.debounce));

        // 移动端触摸手势
        if (this.deviceDetection.isTouchDevice) {
            this.setupTouchGestures();
        }

        // 键盘导航
        this.setupKeyboardNavigation();

        // 窗口焦点事件
        window.addEventListener('focus', () => {
            this.handleWindowFocus();
        });

        // 页面可见性变化
        document.addEventListener('visibilitychange', () => {
            this.handleVisibilityChange();
        });
    }

    /**
     * 🔄 切换侧边栏状态
     */
    toggleSidebar(force = null) {
        if (this.state.isTransitioning) {
            return;
        }

        const newState = force !== null ? force : !this.state.sidebarCollapsed;
        
        if (newState === this.state.sidebarCollapsed) {
            return;
        }

        this.state.isTransitioning = true;
        this.state.sidebarCollapsed = newState;

        // 更新UI状态
        this.updateSidebarUI();
        
        // 保存状态
        this.saveState('sidebarState', newState);

        // 触发自定义事件
        this.dispatchCustomEvent('sidebarToggle', {
            collapsed: newState,
            isAnimation: true
        });

        // 动画结束后重置状态
        setTimeout(() => {
            this.state.isTransitioning = false;
            this.dispatchCustomEvent('sidebarToggleComplete', {
                collapsed: newState
            });
        }, this.config.delays.transition);

        //console.log(`🔄 侧边栏${newState ? '收起' : '展开'}`);
    }

    /**
     * 🎨 切换主题
     */
    toggleTheme(targetTheme = null) {
        const currentTheme = this.state.currentTheme;
        const newTheme = targetTheme || (currentTheme === 'light' ? 'dark' : 'light');
        
        if (newTheme === currentTheme) {
            return;
        }

        // 添加切换类防止闪烁
        this.elements.body?.classList.add('theme-switching');
        
        // 执行主题切换
        setTimeout(() => {
            this.applyTheme(newTheme);
            this.state.currentTheme = newTheme;
            
            // 保存主题状态
            this.saveState('theme', newTheme);
            
            // 移除切换类
            setTimeout(() => {
                this.elements.body?.classList.remove('theme-switching');
            }, this.config.delays.themeSwitch);
            
            // 触发主题切换事件
            this.dispatchCustomEvent('themeChanged', {
                from: currentTheme,
                to: newTheme
            });
            
            //console.log(`🎨 主题已切换: ${currentTheme} → ${newTheme}`);
            
        }, 50);
    }

    /**
     * 🎨 应用主题
     */
    applyTheme(theme) {
        if (!this.elements.html) return;

        // 移除所有主题类
        Object.values(this.config.themes).forEach(themeClass => {
            this.elements.html.classList.remove(themeClass);
        });

        // 添加新主题类
        if (this.config.themes[theme]) {
            this.elements.html.classList.add(this.config.themes[theme]);
        }

        // 更新主题图标
        this.updateThemeIcon(theme);
    }

    /**
     * 🎯 更新主题图标
     */
    updateThemeIcon(theme) {
        const themeIcon = this.elements.themeToggle?.querySelector('.theme-icon');
        if (!themeIcon) return;

        const icons = {
            light: 'fas fa-moon',
            dark: 'fas fa-sun'
        };

        themeIcon.className = `theme-icon ${icons[theme] || icons.light}`;
    }

    /**
     * 📱 更新响应式状态
     */
    updateResponsiveState() {
        const width = window.innerWidth;
        const wasMobile = this.state.isMobile;
        const wasTablet = this.state.isTablet;

        this.state.isMobile = width < this.config.breakpoints.mobile;
        this.state.isTablet = width < this.config.breakpoints.tablet && !this.state.isMobile;

        // 移动端自动收起侧边栏
        if (this.state.isMobile && !wasMobile) {
            this.handleMobileTransition();
        }
        
        // 平板端自动收起
        if (this.state.isTablet && !wasTablet && !wasMobile) {
            this.toggleSidebar(true);
        }

        // 桌面端恢复状态
        if (!this.state.isMobile && !this.state.isTablet && (wasMobile || wasTablet)) {
            this.handleDesktopTransition();
        }

        // 更新CSS类
        this.updateResponsiveClasses();

        //console.log(`📱 响应式状态更新: Mobile(${this.state.isMobile}), Tablet(${this.state.isTablet})`);
    }

    /**
     * 📱 处理移动端转换
     */
    handleMobileTransition() {
        // 移动端隐藏侧边栏
        if (this.elements.sidebar) {
            this.elements.sidebar.classList.remove('show');
        }
        
        // 创建移动端遮罩
        this.createMobileOverlay();
    }

    /**
     * 💻 处理桌面端转换  
     */
    handleDesktopTransition() {
        // 恢复桌面端状态
        if (this.elements.sidebar) {
            this.elements.sidebar.classList.remove('show');
        }
        
        // 移除移动端遮罩
        this.removeMobileOverlay();
    }

    /**
     * 📱 创建移动端遮罩
     */
    createMobileOverlay() {
        if (document.querySelector('.mobile-nav-overlay')) return;

        const overlay = document.createElement('div');
        overlay.className = 'mobile-nav-overlay';
        overlay.addEventListener('click', () => {
            this.closeMobileSidebar();
        });

        document.body.appendChild(overlay);
    }

    /**
     * 📱 移除移动端遮罩
     */
    removeMobileOverlay() {
        const overlay = document.querySelector('.mobile-nav-overlay');
        if (overlay) {
            overlay.remove();
        }
    }

    /**
     * 📱 打开移动端侧边栏
     */
    openMobileSidebar() {
        if (!this.state.isMobile) return;

        if (this.elements.sidebar) {
            this.elements.sidebar.classList.add('show');
        }
        
        const overlay = document.querySelector('.mobile-nav-overlay');
        if (overlay) {
            overlay.classList.add('show');
        }

        // 防止背景滚动
        document.body.style.overflow = 'hidden';
    }

    /**
     * 📱 关闭移动端侧边栏
     */
    closeMobileSidebar() {
        if (this.elements.sidebar) {
            this.elements.sidebar.classList.remove('show');
        }
        
        const overlay = document.querySelector('.mobile-nav-overlay');
        if (overlay) {
            overlay.classList.remove('show');
        }

        // 恢复背景滚动
        document.body.style.overflow = '';
    }

    /**
     * 🎯 更新侧边栏UI
     */
    updateSidebarUI() {
        if (!this.elements.sidebar) return;

        // 更新侧边栏类
        if (this.state.sidebarCollapsed) {
            this.elements.sidebar.classList.add('collapsed');
            this.elements.body?.classList.add('sidebar-collapsed');
        } else {
            this.elements.sidebar.classList.remove('collapsed');
            this.elements.body?.classList.remove('sidebar-collapsed');
        }

        // 更新内容区域边距
        this.updateContentAreaMargin();

        // 更新切换按钮状态
        this.updateToggleButtonState();
    }

    /**
     * 📐 更新内容区域边距
     */
    updateContentAreaMargin() {
        if (!this.elements.contentArea || this.state.isMobile) return;

        const marginLeft = this.state.sidebarCollapsed ? 
            this.config.selectors.sidebar.includes('collapsed') ? '68px' : '280px' :
            '280px';

        this.elements.contentArea.style.marginLeft = this.state.sidebarCollapsed ? '68px' : '280px';
    }

    /**
     * 🔘 更新切换按钮状态
     */
    updateToggleButtonState() {
        if (!this.elements.toggle) return;

        const icon = this.elements.toggle.querySelector('.hamburger-icon');
        if (icon) {
            if (this.state.sidebarCollapsed) {
                icon.classList.add('active');
            } else {
                icon.classList.remove('active');
            }
        }
    }

    /**
     * 🎯 处理导航项点击
     */
    handleNavItemClick(event, linkElement) {
        // 阻止默认链接行为
        event.preventDefault();

        // 移除所有活跃状态
        this.elements.navLinks?.forEach(link => {
            link.classList.remove('active');
        });

        // 添加活跃状态到当前项
        linkElement.classList.add('active');

        // 保存活跃项
        const itemId = linkElement.getAttribute('data-nav-id') || linkElement.getAttribute('href');
        if (itemId) {
            this.state.activeNavItem = itemId;
            this.saveState('activeItem', itemId);
        }

        // 移动端点击后自动关闭
        if (this.state.isMobile) {
            setTimeout(() => {
                this.closeMobileSidebar();
            }, 200);
        }

        // 触发导航事件
        this.dispatchCustomEvent('navigationItemClick', {
            element: linkElement,
            itemId: itemId
        });
    }

    /**
     * 👆 设置触摸手势
     */
    setupTouchGestures() {
        let startX = 0;
        let startY = 0;
        let startTime = 0;

        const handleTouchStart = (e) => {
            const touch = e.touches[0];
            startX = touch.clientX;
            startY = touch.clientY;
            startTime = Date.now();
        };

        const handleTouchEnd = (e) => {
            const touch = e.changedTouches[0];
            const endX = touch.clientX;
            const endY = touch.clientY;
            const deltaX = endX - startX;
            const deltaY = endY - startY;
            const deltaTime = Date.now() - startTime;

            // 检测滑动手势
            if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 50 && deltaTime < 300) {
                if (startX < 20 && deltaX > 0 && this.state.isMobile) {
                    // 从左边缘向右滑动 - 打开侧边栏
                    this.openMobileSidebar();
                } else if (startX > window.innerWidth - 20 && deltaX < 0 && this.state.isMobile) {
                    // 从右边缘向左滑动 - 关闭侧边栏
                    this.closeMobileSidebar();
                }
            }
        };

        document.addEventListener('touchstart', handleTouchStart, 
            this.deviceDetection.supportsPassive ? { passive: true } : false
        );
        
        document.addEventListener('touchend', handleTouchEnd, 
            this.deviceDetection.supportsPassive ? { passive: true } : false
        );
    }

    /**
     * ⌨️ 设置键盘导航
     */
    setupKeyboardNavigation() {
        document.addEventListener('keydown', (e) => {
            // ESC键关闭移动端侧边栏
            if (e.key === 'Escape' && this.state.isMobile) {
                this.closeMobileSidebar();
            }
            
            // Ctrl/Cmd + B 切换侧边栏
            if ((e.ctrlKey || e.metaKey) && e.key === 'b') {
                e.preventDefault();
                this.toggleSidebar();
            }
            
            // Ctrl/Cmd + Shift + T 切换主题
            if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key === 'T') {
                e.preventDefault();
                this.toggleTheme();
            }
        });
    }

    /**
     * ♿ 设置无障碍功能
     */
    setupAccessibility() {
        // 为切换按钮添加ARIA属性
        if (this.elements.toggle) {
            this.elements.toggle.setAttribute('aria-label', '切换侧边栏');
            this.elements.toggle.setAttribute('aria-expanded', !this.state.sidebarCollapsed);
        }

        // 为侧边栏添加ARIA属性和tabindex管理
        if (this.elements.sidebar) {
            const isHidden = this.state.sidebarCollapsed;

            // 管理侧边栏内链接的可访问性
            const sidebarLinks = this.elements.sidebar.querySelectorAll('a, button');
            sidebarLinks.forEach(link => {
                if (isHidden) {
                    // 如果元素有焦点，先移除焦点再设置aria-hidden
                    if (link === document.activeElement) {
                        link.blur();
                    }
                    link.setAttribute('tabindex', '-1');
                    link.setAttribute('aria-hidden', 'true');
                } else {
                    link.removeAttribute('tabindex');
                    link.removeAttribute('aria-hidden');
                }
            });

            // 设置侧边栏的aria-hidden属性
            this.elements.sidebar.setAttribute('aria-hidden', isHidden);
        }

        // 更新ARIA状态
        this.updateAriaStates();
    }

    /**
     * ♿ 更新ARIA状态
     */
    updateAriaStates() {
        if (this.elements.toggle) {
            this.elements.toggle.setAttribute('aria-expanded', !this.state.sidebarCollapsed);
        }
        
        if (this.elements.sidebar) {
            const isHidden = this.state.sidebarCollapsed;

            // 如果要隐藏侧边栏，先处理焦点管理
            if (isHidden) {
                const focusedElement = this.elements.sidebar.querySelector(':focus');
                if (focusedElement) {
                    focusedElement.blur();
                }
            }

            this.elements.sidebar.setAttribute('aria-hidden', isHidden);

            // 管理侧边栏内链接的可访问性
            const sidebarLinks = this.elements.sidebar.querySelectorAll('a, button');
            sidebarLinks.forEach(link => {
                if (isHidden) {
                    link.setAttribute('tabindex', '-1');
                    link.setAttribute('aria-hidden', 'true');
                } else {
                    link.removeAttribute('tabindex');
                    link.removeAttribute('aria-hidden');
                }
            });
        }
    }

    /**
     * ⚡ 性能优化
     */
    performanceOptimization() {
        // 启用GPU加速
        const acceleratedElements = [
            this.elements.sidebar,
            this.elements.toggle,
            ...Array.from(this.elements.navLinks || [])
        ];

        acceleratedElements.forEach(element => {
            if (element) {
                element.style.willChange = 'transform';
                element.style.transform = 'translateZ(0)';
            }
        });

        // 预加载关键动画
        this.preloadAnimations();
    }

    /**
     * 🎬 预加载动画
     */
    preloadAnimations() {
        // 创建隐藏元素触发CSS动画以预加载
        const preloadElement = document.createElement('div');
        preloadElement.style.cssText = `
            position: absolute;
            top: -9999px;
            left: -9999px;
            width: 1px;
            height: 1px;
            opacity: 0;
            pointer-events: none;
        `;
        preloadElement.className = 'modern-nav-link';
        document.body.appendChild(preloadElement);
        
        // 触发动画
        setTimeout(() => {
            preloadElement.style.transform = 'translateX(4px)';
            setTimeout(() => {
                document.body.removeChild(preloadElement);
            }, 100);
        }, 50);
    }

    /**
     * 🎨 初始化主题
     */
    initializeTheme() {
        // 应用存储的主题
        this.applyTheme(this.state.currentTheme);
        
        // 检测系统主题偏好
        if (window.matchMedia) {
            const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
            
            // 如果没有存储的主题偏好，使用系统主题
            if (!localStorage.getItem(this.config.storageKeys.theme)) {
                const systemTheme = mediaQuery.matches ? 'dark' : 'light';
                this.toggleTheme(systemTheme);
            }
            
            // 监听系统主题变化
            mediaQuery.addListener((e) => {
                if (!localStorage.getItem(this.config.storageKeys.theme)) {
                    const systemTheme = e.matches ? 'dark' : 'light';
                    this.toggleTheme(systemTheme);
                }
            });
        }
    }

    /**
     * 🔄 更新响应式类
     */
    updateResponsiveClasses() {
        if (!this.elements.body) return;

        // 移动端类
        if (this.state.isMobile) {
            this.elements.body.classList.add('is-mobile');
        } else {
            this.elements.body.classList.remove('is-mobile');
        }

        // 平板类
        if (this.state.isTablet) {
            this.elements.body.classList.add('is-tablet');
        } else {
            this.elements.body.classList.remove('is-tablet');
        }
    }

    /**
     * 💾 保存状态到本地存储
     */
    saveState(key, value) {
        try {
            const storageKey = this.config.storageKeys[key];
            if (storageKey) {
                localStorage.setItem(storageKey, JSON.stringify(value));
            }
        } catch (error) {
            console.warn('⚠️  保存状态失败:', error);
        }
    }

    /**
     * 🔄 防抖函数
     */
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    /**
     * 🎯 检查被动事件支持
     */
    checkPassiveSupport() {
        let supportsPassive = false;
        try {
            const opts = Object.defineProperty({}, 'passive', {
                get: function() {
                    supportsPassive = true;
                }
            });
            window.addEventListener('testPassive', null, opts);
            window.removeEventListener('testPassive', null, opts);
        } catch (e) {}
        return supportsPassive;
    }

    /**
     * 📡 分发自定义事件
     */
    dispatchCustomEvent(eventName, detail) {
        const event = new CustomEvent(`modernNav:${eventName}`, {
            detail: detail,
            bubbles: true,
            cancelable: true
        });
        document.dispatchEvent(event);
    }

    /**
     * 🔍 处理窗口焦点
     */
    handleWindowFocus() {
        // 窗口获得焦点时刷新状态
        this.updateResponsiveState();
    }

    /**
     * 👁️ 处理页面可见性变化
     */
    handleVisibilityChange() {
        if (document.visibilityState === 'visible') {
            // 页面变为可见时刷新状态
            this.updateResponsiveState();
        }
    }

    /**
     * ❌ 错误处理
     */
    handleError(error, context) {
        console.error(`❌ 导航系统错误 [${context}]:`, error);
        
        // 触发错误事件
        this.dispatchCustomEvent('navigationError', {
            error: error,
            context: context,
            state: this.state
        });
    }

    /**
     * 🔄 公共API - 切换侧边栏
     */
    toggle() {
        this.toggleSidebar();
    }

    /**
     * 📖 公共API - 展开侧边栏
     */
    expand() {
        this.toggleSidebar(false);
    }

    /**
     * 📁 公共API - 收起侧边栏
     */
    collapse() {
        this.toggleSidebar(true);
    }

    /**
     * 🎨 公共API - 设置主题
     */
    setTheme(theme) {
        this.toggleTheme(theme);
    }

    /**
     * 📊 公共API - 获取当前状态
     */
    getState() {
        return { ...this.state };
    }

    /**
     * 🔧 公共API - 更新配置
     */
    updateConfig(newConfig) {
        this.config = { ...this.config, ...newConfig };
        //console.log('🔧 配置已更新:', this.config);
    }

    /**
     * 🗑️ 公共API - 销毁实例
     */
    destroy() {
        // 清理事件监听器
        // 恢复原始状态
        // 清理DOM修改
        console.log('🗑️  导航控制器已销毁');
    }
}

// 将类添加到window对象以便后续检查
window.ModernNavigationController = ModernNavigationController;

// ===========================================
// 🚀 自动初始化
// ===========================================

// DOM加载完成后自动初始化
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeNavigation);
} else {
    initializeNavigation();
}

function initializeNavigation() {
    // 防止重复初始化
    if (window.modernNavController) {
        console.warn('⚠️ ModernNavigation已初始化，跳过重复初始化');
        return;
    }

    // 创建全局导航控制器实例
    window.modernNavController = new ModernNavigationController();

    // 绑定全局快捷方法
    window.toggleSidebar = () => window.modernNavController.toggle();
    window.toggleTheme = () => window.modernNavController.toggleTheme();

    console.log('🎯 现代化导航系统已就绪');
}

// ===========================================
// 🎯 导出模块 (如果支持)
// ===========================================
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ModernNavigationController;
}

