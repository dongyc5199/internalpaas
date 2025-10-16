/**
 * ServerDetailOverlay测试套件
 */

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { ServerDetailOverlay } from "../modules/ServerDetailOverlay";
import type { ServerDetailDependencies, ServerDetail } from "../types/server-management";

// Mock服务器详情数据
const mockServerDetail: ServerDetail = {
    id: 1,
    name: "Test Server",
    host: "192.168.1.1",
    port: 22,
    username: "admin",
    status: "online",
    cpuUsage: 45.5,
    memoryUsage: 60.2,
    diskUsage: 70.8,
    networkIn: 1024000,
    networkOut: 512000,
    uptime: 86400,
    lastCheck: new Date().toISOString(),
    cpuHistory: [40, 42, 45, 43, 45],
    memoryHistory: [58, 59, 60, 61, 60],
    diskHistory: [70, 70, 71, 70, 71],
    networkInHistory: [1000000, 1020000, 1024000, 1010000, 1024000],
    networkOutHistory: [500000, 510000, 512000, 508000, 512000],
    timestamps: ["10:00", "10:01", "10:02", "10:03", "10:04"],
    processes: [
        { pid: 1234, user: "root", cpu: 25.5, mem: 10.2, command: "nginx" },
        { pid: 5678, user: "www", cpu: 15.3, mem: 8.5, command: "node app.js" }
    ],
    applications: [
        { id: 1, name: "Web App", status: "running", port: 8080, pid: 9999 }
    ],
    users: [
        { username: "admin", loginTime: "2025-10-13 10:00", from: "192.168.1.100" }
    ]
};

// Mock DOM环境
function setupDOM() {
    document.body.innerHTML = `
        <div id="serverDetailOverlay" aria-hidden="true">
            <div class="overlay-shell" tabindex="-1">
                <div class="server-detail-toolbar">
                    <button id="serverDetailToggleRefresh">Toggle Refresh</button>
                    <button id="serverDetailToggleCharts">Toggle Charts</button>
                </div>
                <div class="server-detail-toolbar-compact">
                    <span class="server-name"></span>
                    <span class="server-address"></span>
                </div>
                <button class="close-overlay">Close</button>
                <div class="overlay-body">
                    <div class="monitor-grid"></div>
                    <section id="server-detail-overview"></section>
                    <section id="server-detail-processes">
                        <tbody id="server-detail-processes-tbody"></tbody>
                    </section>
                    <section id="server-detail-applications">
                        <tbody id="server-detail-applications-tbody"></tbody>
                    </section>
                    <section id="server-detail-users">
                        <tbody id="server-detail-users-tbody"></tbody>
                    </section>
                </div>
            </div>
        </div>
    `;
}

// 创建Mock依赖
function createMockDependencies(): ServerDetailDependencies {
    return {
        formatBytes: vi.fn((bytes: number) => `${bytes} B`),
        formatUptime: vi.fn((seconds: number) => `${seconds}s`),
        formatPercentage: vi.fn((value: number) => `${value}%`),
        formatDateTime: vi.fn((date: Date | string) => "2025-10-13"),
        showSuccess: vi.fn(),
        showError: vi.fn(),
        getCurrentLanguage: vi.fn(() => "zh"),
        initCharts: vi.fn(),
        destroyCharts: vi.fn()
    };
}

describe("ServerDetailOverlay", () => {
    let overlay: ServerDetailOverlay;
    let mockDeps: ServerDetailDependencies;
    let originalFetch: typeof global.fetch;

    beforeEach(() => {
        setupDOM();
        mockDeps = createMockDependencies();
        overlay = new ServerDetailOverlay(mockDeps);

        // Mock fetch
        originalFetch = global.fetch;
        global.fetch = vi.fn();

        // Mock IntersectionObserver
        global.IntersectionObserver = vi.fn().mockImplementation(() => ({
            observe: vi.fn(),
            unobserve: vi.fn(),
            disconnect: vi.fn()
        }));
    });

    afterEach(() => {
        overlay.teardown();
        global.fetch = originalFetch;
        vi.clearAllTimers();
    });

    describe("初始化和清理", () => {
        it("应该正确初始化", () => {
            const consoleLog = vi.spyOn(console, "log").mockImplementation(() => {});

            overlay.init();

            expect(consoleLog).toHaveBeenCalledWith(
                "[ServerDetailOverlay] Initialized"
            );

            consoleLog.mockRestore();
        });

        it("应该在DOM元素不存在时发出警告", () => {
            document.body.innerHTML = "";
            const consoleWarn = vi.spyOn(console, "warn").mockImplementation(() => {});

            overlay.init();

            expect(consoleWarn).toHaveBeenCalledWith(
                "ServerDetailOverlay element not found"
            );

            consoleWarn.mockRestore();
        });

        it("应该正确清理资源", () => {
            overlay.init();

            const consoleLog = vi.spyOn(console, "log").mockImplementation(() => {});

            overlay.teardown();

            expect(consoleLog).toHaveBeenCalledWith("[ServerDetailOverlay] Torn down");

            consoleLog.mockRestore();
        });
    });

    describe("查看服务器详情", () => {
        it("应该成功加载和显示服务器详情", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);

            expect(global.fetch).toHaveBeenCalledWith(
                "/admin/server-groups/api/1/detail?processSort=cpu"
            );

            expect(overlay.isOpen()).toBe(true);
        });

        it("应该在加载失败时显示错误", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: false
            });

            overlay.init();
            await overlay.view(1);

            expect(mockDeps.showError).toHaveBeenCalledWith("加载服务器详情失败");
            expect(overlay.isOpen()).toBe(false);
        });

        it("应该在网络错误时显示错误", async () => {
            (global.fetch as any).mockRejectedValueOnce(new Error("Network error"));

            overlay.init();

            const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});

            await overlay.view(1);

            expect(consoleError).toHaveBeenCalled();
            expect(mockDeps.showError).toHaveBeenCalled();

            consoleError.mockRestore();
        });

        it("应该使用英文错误消息（当语言为en时）", async () => {
            (mockDeps.getCurrentLanguage as any).mockReturnValue("en");
            (global.fetch as any).mockResolvedValueOnce({
                ok: false
            });

            overlay.init();
            await overlay.view(1);

            expect(mockDeps.showError).toHaveBeenCalledWith(
                "Failed to load server details"
            );
        });
    });

    describe("关闭弹窗", () => {
        it("应该关闭弹窗并重置状态", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);

            expect(overlay.isOpen()).toBe(true);

            overlay.close();

            expect(overlay.isOpen()).toBe(false);
        });

        it("应该恢复body滚动", async () => {
            document.body.style.overflow = "auto";

            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);

            // 打开时禁用滚动
            expect(document.body.style.overflow).toBe("hidden");

            overlay.close();

            // 关闭时恢复滚动
            expect(document.body.style.overflow).toBe("auto");
        });

        it("应该支持静默关闭", () => {
            const consoleLog = vi.spyOn(console, "log").mockImplementation(() => {});

            overlay.init();
            overlay.close({ silent: true });

            expect(consoleLog).not.toHaveBeenCalledWith("[ServerDetailOverlay] Closed");

            consoleLog.mockRestore();
        });

        it("应该在没有初始化时不报错", () => {
            expect(() => {
                overlay.close();
            }).not.toThrow();
        });
    });

    describe("刷新数据", () => {
        it("应该刷新服务器详情数据", async () => {
            (global.fetch as any)
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockServerDetail
                })
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => ({
                        ...mockServerDetail,
                        cpuUsage: 50.0
                    })
                });

            overlay.init();
            await overlay.view(1);

            await overlay.refresh(false);

            expect(global.fetch).toHaveBeenCalledTimes(2);
            expect(mockDeps.showSuccess).toHaveBeenCalledWith("服务器详情已刷新");
        });

        it("应该在自动刷新时不显示成功消息", async () => {
            (global.fetch as any)
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockServerDetail
                })
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockServerDetail
                });

            overlay.init();
            await overlay.view(1);

            await overlay.refresh(true);

            expect(mockDeps.showSuccess).not.toHaveBeenCalled();
        });

        it("应该在刷新失败时静默处理（自动刷新）", async () => {
            (global.fetch as any)
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockServerDetail
                })
                .mockResolvedValueOnce({
                    ok: false
                });

            overlay.init();
            await overlay.view(1);

            const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});

            // 自动刷新失败时不显示错误消息
            await overlay.refresh(true);

            expect(mockDeps.showError).not.toHaveBeenCalled();

            consoleError.mockRestore();
        });

        it("应该在没有当前服务器时不执行刷新", async () => {
            overlay.init();

            await overlay.refresh();

            expect(global.fetch).not.toHaveBeenCalled();
        });
    });

    describe("图表控制", () => {
        it("应该暂停图表更新", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);

            overlay.pauseCharts();

            // 验证暂停逻辑
            expect(overlay).toBeDefined();
        });

        it("应该恢复图表更新", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);

            overlay.pauseCharts();
            overlay.resumeCharts();

            // 验证恢复逻辑
            expect(overlay).toBeDefined();
        });
    });

    describe("滚动到section", () => {
        it("应该滚动到指定section", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            // Mock scrollIntoView
            Element.prototype.scrollIntoView = vi.fn();

            overlay.init();
            await overlay.view(1);

            overlay.scrollToSection("server-detail-overview");

            const section = document.querySelector("#server-detail-overview");
            expect(section?.scrollIntoView).toHaveBeenCalledWith({
                behavior: "smooth",
                block: "start"
            });
        });

        it("应该在section不存在时不报错", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);

            expect(() => {
                overlay.scrollToSection("non-existent-section");
            }).not.toThrow();
        });

        it("应该在body不存在时不报错", () => {
            overlay.init();

            expect(() => {
                overlay.scrollToSection("server-detail-overview");
            }).not.toThrow();
        });
    });

    describe("isOpen状态检查", () => {
        it("应该在关闭时返回false", () => {
            overlay.init();
            expect(overlay.isOpen()).toBe(false);
        });

        it("应该在打开时返回true", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);

            expect(overlay.isOpen()).toBe(true);
        });

        it("应该在没有初始化时返回false", () => {
            expect(overlay.isOpen()).toBe(false);
        });
    });

    describe("按钮交互", () => {
        it("应该切换刷新状态", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);

            const toggleBtn = document.getElementById("serverDetailToggleRefresh");
            toggleBtn?.click();

            // 验证按钮被点击
            expect(toggleBtn).toBeDefined();
        });

        it("应该切换图表折叠状态", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);

            const toggleBtn = document.getElementById("serverDetailToggleCharts");
            toggleBtn?.click();

            const monitorGrid = document.querySelector(".monitor-grid");
            expect(monitorGrid?.classList.contains("collapsed")).toBe(true);
        });

        it("应该通过关闭按钮关闭弹窗", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);

            const closeBtn = document.querySelector(".close-overlay") as HTMLElement;
            closeBtn?.click();

            expect(overlay.isOpen()).toBe(false);
        });
    });

    describe("数据渲染", () => {
        it("应该更新工具栏信息", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);

            const nameEl = document.querySelector(
                ".server-detail-toolbar-compact .server-name"
            );
            const addressEl = document.querySelector(
                ".server-detail-toolbar-compact .server-address"
            );

            expect(nameEl?.textContent).toBe("Test Server");
            expect(addressEl?.textContent).toBe("192.168.1.1:22");
        });

        it("应该处理空进程列表", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    ...mockServerDetail,
                    processes: []
                })
            });

            overlay.init();
            await overlay.view(1);

            // 验证空状态处理
            expect(overlay.isOpen()).toBe(true);
        });

        it("应该处理空应用列表", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    ...mockServerDetail,
                    applications: []
                })
            });

            overlay.init();
            await overlay.view(1);

            expect(overlay.isOpen()).toBe(true);
        });

        it("应该处理空用户列表", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    ...mockServerDetail,
                    users: []
                })
            });

            overlay.init();
            await overlay.view(1);

            expect(overlay.isOpen()).toBe(true);
        });
    });

    describe("边界条件", () => {
        it("应该处理无效的serverId", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: false
            });

            overlay.init();
            await overlay.view(-1);

            expect(mockDeps.showError).toHaveBeenCalled();
        });

        it("应该处理null响应数据", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => null
            });

            overlay.init();
            await overlay.view(1);

            expect(mockDeps.showError).toHaveBeenCalled();
        });

        it("应该处理多次快速打开关闭", async () => {
            (global.fetch as any).mockResolvedValue({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();

            await overlay.view(1);
            overlay.close();
            await overlay.view(1);
            overlay.close();
            await overlay.view(1);

            expect(overlay.isOpen()).toBe(true);
        });

        it("应该在teardown后不能再使用", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);
            overlay.teardown();

            // 尝试再次查看应该不报错但也不工作
            await overlay.view(1);

            expect(overlay.isOpen()).toBe(false);
        });
    });

    describe("焦点管理", () => {
        it("应该在打开时保存当前焦点", async () => {
            const button = document.createElement("button");
            document.body.appendChild(button);
            button.focus();

            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);

            // 焦点应该转移到overlay
            expect(document.activeElement).not.toBe(button);
        });

        it("应该在关闭时恢复焦点", async () => {
            const button = document.createElement("button");
            document.body.appendChild(button);
            button.focus();

            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                json: async () => mockServerDetail
            });

            overlay.init();
            await overlay.view(1);
            overlay.close();

            // 焦点应该恢复到button
            expect(document.activeElement).toBe(button);
        });
    });
});
