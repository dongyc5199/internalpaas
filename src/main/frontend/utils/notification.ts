/**
 * 通知工具函数模块
 */

export type NotificationType = "success" | "error" | "warning" | "info";

/**
 * 扩展Window接口以包含通知系统
 */
interface WindowWithNotifications extends Window {
    showToast?: (message: string, type: NotificationType) => void;
    AppShell?: {
        emit?: (event: string, data: unknown) => void;
    };
    dashboard?: {
        showNotification?: (message: string, type: NotificationType) => void;
    };
}

/**
 * 安全地显示Toast通知
 * 兼容多种通知系统：showToast函数、AppShell事件、控制台输出
 */
export function safeShowToast(message: string, type: NotificationType = "info"): void {
    const win = window as unknown as WindowWithNotifications;

    // 尝试使用全局showToast函数
    if (typeof win.showToast === "function") {
        win.showToast(message, type);
        return;
    }

    // 尝试使用AppShell事件系统
    const appShell = win.AppShell;
    if (appShell && typeof appShell.emit === "function") {
        appShell.emit("notification:show", {
            message,
            type
        });
        return;
    }

    // 尝试使用dashboard通知系统
    const dashboard = win.dashboard;
    if (dashboard && typeof dashboard.showNotification === "function") {
        dashboard.showNotification(message, type);
        return;
    }

    // 降级到控制台输出
    console.log(`[Toast ${type}] ${message}`);
}

/**
 * 显示成功通知
 */
export function showSuccess(message: string): void {
    safeShowToast(message, "success");
}

/**
 * 显示错误通知
 */
export function showError(message: string): void {
    safeShowToast(message, "error");
}

/**
 * 显示警告通知
 */
export function showWarning(message: string): void {
    safeShowToast(message, "warning");
}

/**
 * 显示信息通知
 */
export function showInfo(message: string): void {
    safeShowToast(message, "info");
}

/**
 * 批量显示通知（带延迟）
 * @param messages - 消息数组
 * @param type - 通知类型
 * @param delay - 每条消息之间的延迟（毫秒）
 */
export async function showNotificationBatch(
    messages: string[],
    type: NotificationType = "info",
    delay = 500
): Promise<void> {
    for (const message of messages) {
        safeShowToast(message, type);
        if (delay > 0) {
            await new Promise((resolve) => setTimeout(resolve, delay));
        }
    }
}

/**
 * 显示带进度的通知
 * @param message - 消息内容
 * @param current - 当前进度
 * @param total - 总进度
 * @param type - 通知类型
 */
export function showProgressNotification(
    message: string,
    current: number,
    total: number,
    type: NotificationType = "info"
): void {
    const percent = Math.round((current / total) * 100);
    const progressMessage = `${message} (${current}/${total} - ${percent}%)`;
    safeShowToast(progressMessage, type);
}
