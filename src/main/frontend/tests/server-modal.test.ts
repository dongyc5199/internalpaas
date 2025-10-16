import { beforeEach, describe, expect, it, vi, afterEach } from "vitest";
import { ServerModal } from "../modules/server-modal";

const originalFetch = global.fetch;

function prepareDocument(): void {
    document.body.innerHTML = `
        <meta name="_csrf" content="token-xyz" />
        <meta name="_csrf_header" content="X-CSRF-TOKEN" />
    `;
    document.body.style.overflow = "";
    window.serverModal = undefined;
    window.openServerModal = undefined;
    window.refreshServerList = undefined;
    window.startServerMetricsPolling = undefined;
}

describe("ServerModal", () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        vi.resetAllMocks();
        prepareDocument();
    });

    afterEach(() => {
        global.fetch = originalFetch;
        vi.useRealTimers();
    });

    it("open 时锁定背景滚动并聚焦服务器名称输入框", () => {
        vi.useFakeTimers();
        const modal = new ServerModal();
        const nameInput = document.getElementById("serverName") as HTMLInputElement;
        const focusSpy = vi.spyOn(nameInput, "focus");

        modal.open();
        expect(document.body.style.overflow).toBe("hidden");

        vi.runAllTimers();
        expect(focusSpy).toHaveBeenCalledTimes(1);
    });

    it("提交成功后触发列表刷新与指标轮询", async () => {
        vi.useFakeTimers();
        const modal = new ServerModal();
        const form = document.getElementById("serverAddForm") as HTMLFormElement;

        (document.getElementById("serverName") as HTMLInputElement).value = "demo-server";
        (document.getElementById("serverType") as HTMLSelectElement).value = "production";
        (document.getElementById("serverHostname") as HTMLInputElement).value = "127.0.0.1";
        (document.getElementById("serverPort") as HTMLInputElement).value = "22";
        (document.getElementById("serverUsername") as HTMLInputElement).value = "root";
        (document.getElementById("serverPassword") as HTMLInputElement).value = "secret";

        const refreshSpy = vi.fn().mockResolvedValue(undefined);
        const pollingSpy = vi.fn();
        window.refreshServerList = refreshSpy;
        window.startServerMetricsPolling = pollingSpy;

        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => ({ serverId: "abc-001" })
        }) as unknown as typeof fetch;

        const submitPromise = new Promise<void>((resolve) => {
            form.addEventListener(
                "submit",
                () => {
                    setTimeout(() => resolve(), 1000);
                },
                { once: true }
            );
        });

        form.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));

        await vi.runAllTimersAsync();
        await submitPromise;

        expect(refreshSpy).toHaveBeenCalledTimes(1);
        expect(pollingSpy).toHaveBeenCalledWith("abc-001");
    });

    it("测试连接失败时展示错误 Toast", async () => {
        const modal = new ServerModal();

        (document.getElementById("serverHostname") as HTMLInputElement).value = "1.1.1.1";
        (document.getElementById("serverPort") as HTMLInputElement).value = "22";
        (document.getElementById("serverUsername") as HTMLInputElement).value = "root";
        (document.getElementById("serverPassword") as HTMLInputElement).value = "secret";

        global.fetch = vi
            .fn()
            .mockRejectedValue(new Error("network down")) as unknown as typeof fetch;

        const testButton = document.getElementById("testConnectionBtn") as HTMLButtonElement;

        // 触发点击事件
        testButton.click();

        // 等待异步操作完成
        await vi.waitFor(
            () => {
                const toasts = document.querySelectorAll(".server-modal-toast");
                expect(toasts.length).toBeGreaterThan(0);
            },
            { timeout: 2000 }
        );

        const toasts = document.querySelectorAll(".server-modal-toast");
        expect(toasts[toasts.length - 1]?.textContent).toContain("连接测试失败");
    });
});
