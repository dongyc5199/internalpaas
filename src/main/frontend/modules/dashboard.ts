/**
 * 仪表板模块
 * 提供通用仪表板功能:自动刷新、通知系统、进度条动画等
 */

import { http, formatBytes, formatUptime } from "@/utils";

interface StatsData {
    newValue?: number;
    [key: string]: unknown;
}

type NotificationType = "success" | "error" | "warning" | "info";

class Dashboard {
    private refreshInterval: number;
    private notificationContainer: HTMLDivElement | null = null;
    private refreshTimer: number | null = null;

    constructor(refreshInterval = 30000) {
        this.refreshInterval = refreshInterval;
        this.init();
    }

    /**
     * 初始化仪表板
     */
    private init(): void {
        this.setupNotifications();
        this.setupProgressBars();
        this.setupAutoRefresh();
    }

    /**
     * 设置自动刷新
     */
    private setupAutoRefresh(): void {
        this.refreshTimer = window.setInterval(() => {
            void this.updateStats();
        }, this.refreshInterval);
    }

    /**
     * 停止自动刷新
     */
    stopAutoRefresh(): void {
        if (this.refreshTimer !== null) {
            clearInterval(this.refreshTimer);
            this.refreshTimer = null;
        }
    }

    /**
     * 更新统计数据
     */
    async updateStats(): Promise<void> {
        try {
            // 检查当前页面是否需要stats API
            const isDashboardPage = document.querySelector('.dashboard-stats, [data-stats]');
            if (!isDashboardPage) {
                return; // 如果不是仪表板页面，跳过stats更新
            }

            const { data } = await http.get<StatsData>("/api/stats");
            this.updateStatCards(data);
        } catch (error) {
            // 静默处理404错误，因为不是所有页面都有stats API
            if (error instanceof Error && error.message.includes('404')) {
                // 404错误不记录，因为很多页面不需要这个API
                return;
            }
            console.error("[Dashboard] 更新统计数据失败:", error);
        }
    }

    /**
     * 更新统计卡片
     */
    private updateStatCards(data: StatsData): void {
        const cards = document.querySelectorAll<HTMLElement>(".stat-card");
        cards.forEach((card) => {
            const number = card.querySelector<HTMLElement>(".stat-number");
            if (number) {
                const start = parseInt(number.textContent || "0", 10);
                const end = data.newValue || 0;
                this.animateNumber(number, start, end);
            }
        });
    }

    /**
     * 数字动画
     */
    private animateNumber(element: HTMLElement, start: number, end: number): void {
        const duration = 1000;
        const startTime = performance.now();

        const animate = (currentTime: number): void => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);

            const current = Math.floor(start + (end - start) * progress);
            element.textContent = String(current);

            if (progress < 1) {
                requestAnimationFrame(animate);
            }
        };

        requestAnimationFrame(animate);
    }

    /**
     * 设置通知系统
     */
    private setupNotifications(): void {
        this.notificationContainer = document.createElement("div");
        this.notificationContainer.id = "notification-container";
        this.notificationContainer.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 9999;
            pointer-events: none;
        `;
        document.body.appendChild(this.notificationContainer);
    }

    /**
     * 显示通知
     */
    showNotification(message: string, type: NotificationType = "info", duration = 3000): void {
        if (!this.notificationContainer) {
            return;
        }

        const notification = document.createElement("div");
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        notification.style.cssText = `
            margin-bottom: 10px;
            padding: 12px 20px;
            border-radius: 4px;
            background: white;
            box-shadow: 0 2px 8px rgba(0,0,0,0.15);
            pointer-events: auto;
            animation: slideInRight 0.3s ease-out;
        `;

        this.notificationContainer.appendChild(notification);

        setTimeout(() => {
            notification.style.animation = "slideOutRight 0.3s ease-in";
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, duration);
    }

    /**
     * 设置进度条动画
     */
    private setupProgressBars(): void {
        const progressBars = document.querySelectorAll<HTMLElement>(".progress-bar-fill");
        progressBars.forEach((bar) => {
            const width = bar.style.width || "0%";
            bar.style.width = "0%";
            setTimeout(() => {
                bar.style.width = width;
            }, 100);
        });
    }

    /**
     * 销毁实例
     */
    destroy(): void {
        this.stopAutoRefresh();
        if (this.notificationContainer && this.notificationContainer.parentNode) {
            this.notificationContainer.parentNode.removeChild(this.notificationContainer);
        }
    }
}

/**
 * 工具函数: 显示加载动画
 */
export function showLoading(element: HTMLElement): void {
    const spinner = document.createElement("div");
    spinner.className = "loading-spinner";
    spinner.innerHTML = '<div class="spinner"></div>';
    element.appendChild(spinner);
}

/**
 * 工具函数: 隐藏加载动画
 */
export function hideLoading(element: HTMLElement): void {
    const spinner = element.querySelector(".loading-spinner");
    if (spinner) {
        spinner.remove();
    }
}

// 重新导出格式化工具函数 (保持向后兼容)
export { formatBytes, formatUptime };

// 全局类型声明
declare global {
    interface Window {
        dashboard?: Dashboard;
    }
}

// 初始化仪表板
function initDashboard(): Dashboard {
    if (!window.dashboard) {
        window.dashboard = new Dashboard();
        console.info("[Dashboard] 已完成初始化");
    }
    return window.dashboard;
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
        initDashboard();
    });
} else {
    initDashboard();
}

export { Dashboard, initDashboard };
export type { NotificationType, StatsData };
