/**
 * 主题切换模块
 * 提供亮色/暗色主题切换功能
 */

import { localStorageManager, eventBus } from "@/utils";

type Theme = "light" | "dark";

class ThemeManager {
    private readonly STORAGE_KEY = "theme";
    private readonly DARK_CLASS = "dark-theme";
    private readonly HTML_DARK_CLASS = "theme-dark";
    private readonly HTML_LIGHT_CLASS = "theme-light";

    constructor() {
        this.init();
    }

    /**
     * 初始化主题管理器
     */
    private init(): void {
        this.applySavedTheme();
        this.bindGlobalToggle();
    }

    /**
     * 应用保存的主题
     */
    private applySavedTheme(): void {
        const savedTheme = localStorageManager.get<Theme>(this.STORAGE_KEY, "light");

        if (savedTheme === "dark") {
            this.applyDarkTheme(false);
        } else {
            this.applyLightTheme(false);
        }

        // 重新启用过渡效果
        setTimeout(() => {
            const elements = document.querySelectorAll<HTMLElement>("*");
            elements.forEach((el) => {
                el.style.transition = "";
            });
        }, 100);
    }

    /**
     * 应用暗色主题
     */
    private applyDarkTheme(animate = true): void {
        if (animate) {
            document.body.classList.add(this.DARK_CLASS);
        } else {
            document.body.classList.add(this.DARK_CLASS);
        }

        document.documentElement.classList.remove(this.HTML_LIGHT_CLASS);
        document.documentElement.classList.add(this.HTML_DARK_CLASS);
    }

    /**
     * 应用亮色主题
     */
    private applyLightTheme(animate = true): void {
        if (animate) {
            document.body.classList.remove(this.DARK_CLASS);
        } else {
            document.body.classList.remove(this.DARK_CLASS);
        }

        document.documentElement.classList.remove(this.HTML_DARK_CLASS);
        document.documentElement.classList.add(this.HTML_LIGHT_CLASS);
    }

    /**
     * 切换主题
     */
    toggle(): void {
        const isDark = document.body.classList.toggle(this.DARK_CLASS);

        if (isDark) {
            this.applyDarkTheme(false);
        } else {
            this.applyLightTheme(false);
        }

        // 保存主题设置
        const theme: Theme = isDark ? "dark" : "light";
        localStorageManager.set(this.STORAGE_KEY, theme);

        // 触发主题变化事件
        eventBus.emit("theme:changed", { theme });

        // 兼容旧的CustomEvent
        const themeEvent = new CustomEvent("themeChanged", {
            detail: { theme }
        });
        document.dispatchEvent(themeEvent);
    }

    /**
     * 获取当前主题
     */
    getCurrentTheme(): Theme {
        return document.body.classList.contains(this.DARK_CLASS) ? "dark" : "light";
    }

    /**
     * 设置主题
     */
    setTheme(theme: Theme): void {
        if (theme === this.getCurrentTheme()) {
            return;
        }
        this.toggle();
    }

    /**
     * 绑定全局切换函数
     */
    private bindGlobalToggle(): void {
        window.toggleTheme = () => this.toggle();
    }
}

// 全局类型声明
declare global {
    interface Window {
        toggleTheme?: () => void;
        themeManager?: ThemeManager;
    }
}

// 初始化主题管理器
function initThemeManager(): ThemeManager {
    if (!window.themeManager) {
        window.themeManager = new ThemeManager();
        console.info("[ThemeManager] 已完成初始化");
    }
    return window.themeManager;
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
        initThemeManager();
    });
} else {
    initThemeManager();
}

export { ThemeManager, initThemeManager };
export type { Theme };
