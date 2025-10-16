import { beforeEach, describe, expect, it, vi, afterEach } from "vitest";
import {
    Dashboard,
    formatBytes,
    formatUptime,
    showLoading,
    hideLoading
} from "../modules/dashboard";

const originalFetch = global.fetch;

function prepareDocument(): void {
    document.body.innerHTML = "";
    window.dashboard = undefined;
}

describe("Dashboard", () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        prepareDocument();
    });

    afterEach(() => {
        global.fetch = originalFetch;
    });

    it("初始化时创建通知容器", () => {
        new Dashboard();

        const container = document.getElementById("notification-container");
        expect(container).not.toBeNull();
    });

    it("显示通知消息", () => {
        const dashboard = new Dashboard();

        dashboard.showNotification("测试消息", "success");

        const notifications = document.querySelectorAll(".notification");
        expect(notifications.length).toBeGreaterThan(0);
    });

    it("自动刷新统计数据", async () => {
        vi.useFakeTimers();

        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => ({ newValue: 100 })
        }) as unknown as typeof fetch;

        const dashboard = new Dashboard(1000);

        // 只推进一次计时器
        vi.advanceTimersByTime(1000);

        await Promise.resolve();

        expect(fetch).toHaveBeenCalled();

        dashboard.stopAutoRefresh();
        vi.useRealTimers();
    });

    it("停止自动刷新", () => {
        vi.useFakeTimers();

        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => ({})
        }) as unknown as typeof fetch;

        const dashboard = new Dashboard(1000);
        dashboard.stopAutoRefresh();

        vi.advanceTimersByTime(2000);

        // fetch在初始化时会被调用一次,停止后不应再被调用
        expect(fetch).toHaveBeenCalledTimes(0);

        vi.useRealTimers();
    });

    it("销毁实例", () => {
        const dashboard = new Dashboard();
        const container = document.getElementById("notification-container");

        dashboard.destroy();

        expect(document.getElementById("notification-container")).toBeNull();
        expect(container?.parentNode).toBeNull();
    });
});

describe("工具函数", () => {
    it("formatBytes - 格式化字节", () => {
        expect(formatBytes(0)).toBe("0 Bytes");
        expect(formatBytes(1024)).toBe("1 KB");
        expect(formatBytes(1048576)).toBe("1 MB");
        expect(formatBytes(1073741824)).toBe("1 GB");
    });

    it("formatUptime - 格式化运行时间", () => {
        expect(formatUptime(59)).toBe("0分钟");
        expect(formatUptime(3600)).toBe("1小时 0分钟");
        expect(formatUptime(86400)).toBe("1天 0小时");
        expect(formatUptime(90000)).toBe("1天 1小时");
    });

    it("showLoading - 显示加载动画", () => {
        const element = document.createElement("div");
        document.body.appendChild(element);

        showLoading(element);

        expect(element.querySelector(".loading-spinner")).not.toBeNull();
    });

    it("hideLoading - 隐藏加载动画", () => {
        const element = document.createElement("div");
        element.innerHTML = '<div class="loading-spinner"></div>';
        document.body.appendChild(element);

        hideLoading(element);

        expect(element.querySelector(".loading-spinner")).toBeNull();
    });
});
