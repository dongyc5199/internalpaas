/**
 * notification工具模块测试
 */

import { describe, it, expect, beforeEach, vi } from "vitest";
import {
    safeShowToast,
    showSuccess,
    showError,
    showWarning,
    showInfo,
    showNotificationBatch,
    showProgressNotification
} from "../utils/notification";

describe("notification工具模块", () => {
    beforeEach(() => {
        // 清理全局对象
        delete (window as any).showToast;
        delete (window as any).AppShell;
        delete (window as any).dashboard;

        // Mock console.log
        vi.spyOn(console, "log").mockImplementation(() => {});
    });

    describe("safeShowToast", () => {
        it("应调用全局showToast函数", () => {
            const mockShowToast = vi.fn();
            (window as any).showToast = mockShowToast;

            safeShowToast("测试消息", "success");

            expect(mockShowToast).toHaveBeenCalledWith("测试消息", "success");
        });

        it("应调用AppShell.emit", () => {
            const mockEmit = vi.fn();
            (window as any).AppShell = { emit: mockEmit };

            safeShowToast("测试消息", "error");

            expect(mockEmit).toHaveBeenCalledWith("notification:show", {
                message: "测试消息",
                type: "error"
            });
        });

        it("应调用dashboard.showNotification", () => {
            const mockShowNotification = vi.fn();
            (window as any).dashboard = { showNotification: mockShowNotification };

            safeShowToast("测试消息", "warning");

            expect(mockShowNotification).toHaveBeenCalledWith("测试消息", "warning");
        });

        it("应降级到console.log", () => {
            safeShowToast("测试消息", "info");

            expect(console.log).toHaveBeenCalledWith("[Toast info] 测试消息");
        });

        it("应使用默认type为info", () => {
            safeShowToast("测试消息");

            expect(console.log).toHaveBeenCalledWith("[Toast info] 测试消息");
        });
    });

    describe("快捷通知函数", () => {
        it("showSuccess应调用safeShowToast", () => {
            const mockShowToast = vi.fn();
            (window as any).showToast = mockShowToast;

            showSuccess("成功消息");

            expect(mockShowToast).toHaveBeenCalledWith("成功消息", "success");
        });

        it("showError应调用safeShowToast", () => {
            const mockShowToast = vi.fn();
            (window as any).showToast = mockShowToast;

            showError("错误消息");

            expect(mockShowToast).toHaveBeenCalledWith("错误消息", "error");
        });

        it("showWarning应调用safeShowToast", () => {
            const mockShowToast = vi.fn();
            (window as any).showToast = mockShowToast;

            showWarning("警告消息");

            expect(mockShowToast).toHaveBeenCalledWith("警告消息", "warning");
        });

        it("showInfo应调用safeShowToast", () => {
            const mockShowToast = vi.fn();
            (window as any).showToast = mockShowToast;

            showInfo("信息消息");

            expect(mockShowToast).toHaveBeenCalledWith("信息消息", "info");
        });
    });

    describe("showNotificationBatch", () => {
        it("应批量显示通知", async () => {
            const mockShowToast = vi.fn();
            (window as any).showToast = mockShowToast;

            const messages = ["消息1", "消息2", "消息3"];

            await showNotificationBatch(messages, "success", 0);

            expect(mockShowToast).toHaveBeenCalledTimes(3);
            expect(mockShowToast).toHaveBeenNthCalledWith(1, "消息1", "success");
            expect(mockShowToast).toHaveBeenNthCalledWith(2, "消息2", "success");
            expect(mockShowToast).toHaveBeenNthCalledWith(3, "消息3", "success");
        });

        it("应在消息之间延迟", async () => {
            vi.useFakeTimers();
            const mockShowToast = vi.fn();
            (window as any).showToast = mockShowToast;

            const messages = ["消息1", "消息2"];
            const promise = showNotificationBatch(messages, "info", 100);

            // 推进所有计时器
            await vi.runAllTimersAsync();

            // 等待Promise完成
            await promise;

            // 两条消息都应该显示
            expect(mockShowToast).toHaveBeenCalledTimes(2);
            expect(mockShowToast).toHaveBeenNthCalledWith(1, "消息1", "info");
            expect(mockShowToast).toHaveBeenNthCalledWith(2, "消息2", "info");

            vi.useRealTimers();
        });

        it("应使用默认delay和type", async () => {
            const mockShowToast = vi.fn();
            (window as any).showToast = mockShowToast;

            await showNotificationBatch(["消息1"], undefined, 0);

            expect(mockShowToast).toHaveBeenCalledWith("消息1", "info");
        });
    });

    describe("showProgressNotification", () => {
        it("应显示带进度的通知", () => {
            const mockShowToast = vi.fn();
            (window as any).showToast = mockShowToast;

            showProgressNotification("处理中", 3, 10, "info");

            expect(mockShowToast).toHaveBeenCalledWith("处理中 (3/10 - 30%)", "info");
        });

        it("应正确计算百分比", () => {
            const mockShowToast = vi.fn();
            (window as any).showToast = mockShowToast;

            showProgressNotification("上传", 7, 10);

            expect(mockShowToast).toHaveBeenCalledWith("上传 (7/10 - 70%)", "info");
        });

        it("应处理完成状态", () => {
            const mockShowToast = vi.fn();
            (window as any).showToast = mockShowToast;

            showProgressNotification("完成", 10, 10, "success");

            expect(mockShowToast).toHaveBeenCalledWith("完成 (10/10 - 100%)", "success");
        });
    });
});
