/**
 * 格式化工具函数模块
 */

/**
 * 格式化字节为可读格式
 */
export function formatBytes(bytes: number, decimals = 2): string {
    if (bytes === 0) return "0 Bytes";

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ["Bytes", "KB", "MB", "GB", "TB", "PB"];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}

/**
 * 格式化运行时间(秒)
 */
export function formatUptime(seconds: number): string {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);

    if (days > 0) {
        return `${days}天 ${hours}小时`;
    } else if (hours > 0) {
        return `${hours}小时 ${minutes}分钟`;
    } else {
        return `${minutes}分钟`;
    }
}

/**
 * 格式化百分比
 */
export function formatPercentage(value: number, decimals = 1): string {
    return `${value.toFixed(decimals)}%`;
}

/**
 * 格式化时间戳为本地时间
 */
export function formatTimestamp(
    timestamp: number | string,
    format = "YYYY-MM-DD HH:mm:ss"
): string {
    const date = typeof timestamp === "string" ? new Date(timestamp) : new Date(timestamp);

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    const seconds = String(date.getSeconds()).padStart(2, "0");

    return format
        .replace("YYYY", String(year))
        .replace("MM", month)
        .replace("DD", day)
        .replace("HH", hours)
        .replace("mm", minutes)
        .replace("ss", seconds);
}

/**
 * 根据使用率获取CSS类名
 */
export function getUsageClass(value: number): string {
    if (value >= 90) return "danger";
    if (value >= 75) return "warning";
    return "normal";
}

/**
 * 数字千分位格式化
 */
export function formatNumber(num: number, decimals = 0): string {
    return num.toFixed(decimals).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}
