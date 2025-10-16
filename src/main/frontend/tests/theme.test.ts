import { beforeEach, describe, expect, it, vi } from "vitest";
import { ThemeManager } from "../modules/theme";

function prepareDocument(): void {
    document.body.className = "";
    document.documentElement.className = "";
    window.themeManager = undefined;
    window.toggleTheme = undefined;
}

describe("ThemeManager", () => {
    beforeEach(() => {
        prepareDocument();
        vi.clearAllMocks();
        localStorage.clear();
    });

    it("应用保存的暗色主题", () => {
        localStorage.setItem("theme", JSON.stringify({ value: "dark" }));

        new ThemeManager();

        expect(document.body.classList.contains("dark-theme")).toBe(true);
        expect(document.documentElement.classList.contains("theme-dark")).toBe(true);
    });

    it("默认应用亮色主题", () => {
        new ThemeManager();

        expect(document.body.classList.contains("dark-theme")).toBe(false);
        expect(document.documentElement.classList.contains("theme-light")).toBe(true);
    });

    it("切换主题时更新类名", () => {
        const manager = new ThemeManager();

        manager.toggle();

        expect(document.body.classList.contains("dark-theme")).toBe(true);
        expect(document.documentElement.classList.contains("theme-dark")).toBe(true);
    });

    it("切换主题时保存到localStorage", () => {
        const manager = new ThemeManager();

        manager.toggle();

        const saved = localStorage.getItem("theme");
        expect(saved).toContain('"dark"');
    });

    it("触发主题变化事件", () => {
        const manager = new ThemeManager();
        const handler = vi.fn();

        document.addEventListener("themeChanged", handler);
        manager.toggle();

        expect(handler).toHaveBeenCalledTimes(1);
    });

    it("获取当前主题", () => {
        const manager = new ThemeManager();

        expect(manager.getCurrentTheme()).toBe("light");

        manager.toggle();

        expect(manager.getCurrentTheme()).toBe("dark");
    });

    it("设置指定主题", () => {
        const manager = new ThemeManager();

        manager.setTheme("dark");

        expect(manager.getCurrentTheme()).toBe("dark");
    });

    it("绑定全局toggleTheme函数", () => {
        new ThemeManager();

        expect(typeof window.toggleTheme).toBe("function");

        window.toggleTheme!();

        expect(document.body.classList.contains("dark-theme")).toBe(true);
    });
});
