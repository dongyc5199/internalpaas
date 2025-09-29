(function() {
    class ModernNavigation {
        constructor() {
            this.sidebar = document.getElementById('modernSidebar');
            this.toggleButton = document.getElementById('sidebarToggle');
            this.themeToggle = document.querySelector('.theme-toggle-btn');
            this.navLinks = Array.from(document.querySelectorAll('.modern-nav-link'));
            this.userMenuToggle = document.getElementById('userMenuToggle');
            this.userDropdown = document.getElementById('userDropdown');
            this.searchInput = document.querySelector('.search-input');

            this.storageKeys = {
                sidebar: 'modern-nav:sidebar-collapsed',
                theme: 'modern-nav:theme',
                active: 'modern-nav:active-item'
            };

            this.state = {
                sidebarCollapsed: false,
                theme: document.documentElement.getAttribute('data-theme') || 'light',
                activeItem: null
            };

            this.init();
        }

        init() {
            this.restoreState();
            this.bindEvents();
            this.applySidebarState();
            this.highlightActiveLink();
            this.dispatch('navigationInitialized', {
                collapsed: this.state.sidebarCollapsed,
                activeItem: this.state.activeItem,
                theme: this.state.theme
            });
        }

        bindEvents() {
            if (this.toggleButton) {
                this.toggleButton.addEventListener('click', () => {
                    this.toggleSidebar();
                });
            }

            this.navLinks.forEach(link => {
                link.addEventListener('click', event => {
                    const target = event.currentTarget;
                    const navId = target.getAttribute('data-nav-id');
                    const page = target.getAttribute('data-page') || (navId ? navId : null);
                    const href = target.getAttribute('href');

                    if (href && href.startsWith('#')) {
                        event.preventDefault();
                    }

                    if (!page) {
                        return;
                    }

                    this.setActiveLink(target);
                    this.saveState('active', page);
                    this.dispatch('navigationItemClick', {
                        element: target,
                        itemId: page
                    });
                });
            });

            if (this.themeToggle) {
                this.themeToggle.addEventListener('click', () => {
                    this.toggleTheme();
                });
            }

            if (this.userMenuToggle && this.userDropdown) {
                this.userMenuToggle.addEventListener('click', event => {
                    event.stopPropagation();
                    const expanded = this.userMenuToggle.getAttribute('aria-expanded') === 'true';
                    this.setUserMenuState(!expanded);
                });

                document.addEventListener('click', event => {
                    if (this.userDropdown.contains(event.target)) {
                        return;
                    }
                    if (event.target === this.userMenuToggle) {
                        return;
                    }
                    this.setUserMenuState(false);
                });

                document.addEventListener('keydown', event => {
                    if (event.key === 'Escape') {
                        this.setUserMenuState(false);
                    }
                });
            }

            if (this.searchInput) {
                this.searchInput.addEventListener('keydown', event => {
                    if (event.key === 'Enter') {
                        const query = this.searchInput.value.trim();
                        if (query.length > 0) {
                            this.dispatch('searchSubmit', { query });
                        }
                    }
                });
            }

            window.addEventListener('modernNav:applyTheme', event => {
                const theme = event.detail && event.detail.theme;
                if (theme) {
                    this.setTheme(theme);
                }
            });
        }

        restoreState() {
            try {
                const collapsed = localStorage.getItem(this.storageKeys.sidebar);
                if (collapsed !== null) {
                    this.state.sidebarCollapsed = collapsed === 'true';
                }

                const activeItem = localStorage.getItem(this.storageKeys.active);
                if (activeItem) {
                    this.state.activeItem = activeItem;
                }

                const savedTheme = localStorage.getItem(this.storageKeys.theme);
                if (savedTheme) {
                    this.state.theme = savedTheme;
                    this.applyTheme(savedTheme);
                }
            } catch (error) {
                console.warn('modern-navigation: unable to restore state', error);
            }
        }

        saveState(key, value) {
            const storageKey = this.storageKeys[key];
            if (!storageKey) {
                return;
            }
            try {
                localStorage.setItem(storageKey, String(value));
            } catch (error) {
                console.warn('modern-navigation: unable to persist state', error);
            }
        }

        toggleSidebar(force) {
            if (!this.sidebar) {
                return;
            }

            if (typeof force === 'boolean') {
                this.state.sidebarCollapsed = force;
            } else {
                this.state.sidebarCollapsed = !this.state.sidebarCollapsed;
            }

            this.applySidebarState();
            this.saveState('sidebar', this.state.sidebarCollapsed);
            this.dispatch('sidebarToggle', { collapsed: this.state.sidebarCollapsed });
        }

        applySidebarState() {
            document.body.classList.toggle('sidebar-collapsed', this.state.sidebarCollapsed);
        }

        setActiveLink(linkElement) {
            this.navLinks.forEach(link => link.classList.remove('active'));
            if (linkElement) {
                linkElement.classList.add('active');
                const page = linkElement.getAttribute('data-page');
                if (page) {
                    this.state.activeItem = page;
                }
            }
        }

        highlightActiveLink() {
            if (!this.state.activeItem) {
                const preset = this.navLinks.find(link => link.classList.contains('active')) || null;
                if (preset) {
                    this.state.activeItem = preset.getAttribute('data-page') || preset.getAttribute('data-nav-id');
                } else {
                    return;
                }
            }
            const activeLink = this.navLinks.find(link => {
                return link.getAttribute('data-page') === this.state.activeItem;
            });
            if (activeLink) {
                this.navLinks.forEach(link => link.classList.remove('active'));
                activeLink.classList.add('active');
            }
        }

        toggleTheme() {
            const next = this.state.theme === 'dark' ? 'light' : 'dark';
            this.setTheme(next);
        }

        setTheme(theme) {
            this.state.theme = theme === 'dark' ? 'dark' : 'light';
            this.applyTheme(this.state.theme);
            this.saveState('theme', this.state.theme);
            this.dispatch('themeChanged', {
                theme: this.state.theme
            });
        }

        applyTheme(theme) {
            if (typeof window.__applyThemePreferences === 'function') {
                window.__applyThemePreferences(theme, true);
                return;
            }
            const root = document.documentElement;
            const modeClass = theme === 'dark' ? 'theme-dark' : 'theme-light';
            const shellClass = theme === 'dark' ? 'shell-theme-dark' : 'shell-theme-light';

            root.setAttribute('data-theme', theme);
            root.classList.remove('theme-light', 'theme-dark', 'shell-theme-light', 'shell-theme-dark');
            root.classList.add(modeClass, shellClass);

            if (document.body) {
                document.body.classList.toggle('dark-mode', theme === 'dark');
            }
        }

        setUserMenuState(open) {
            if (!this.userMenuToggle || !this.userDropdown) {
                return;
            }
            this.userMenuToggle.setAttribute('aria-expanded', open ? 'true' : 'false');
            this.userDropdown.classList.toggle('open', !!open);
        }

        dispatch(name, detail) {
            const event = new CustomEvent(`modernNav:${name}`, {
                detail: detail || {},
                bubbles: true,
                cancelable: true
            });
            document.dispatchEvent(event);
        }
    }

    function bootstrap() {
        if (window.modernNavController) {
            return;
        }
        const controller = new ModernNavigation();
        window.modernNavController = {
            toggle: () => controller.toggleSidebar(),
            collapse: () => controller.toggleSidebar(true),
            expand: () => controller.toggleSidebar(false),
            toggleTheme: () => controller.toggleTheme(),
            setTheme: theme => controller.setTheme(theme),
            getState: () => ({
                collapsed: controller.state.sidebarCollapsed,
                theme: controller.state.theme,
                activeItem: controller.state.activeItem
            })
        };
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', bootstrap);
    } else {
        bootstrap();
    }
})();

