/**
 * i18n工具模块测试
 */

import { describe, it, expect, beforeEach, vi } from "vitest";
import {
    setLocalizedText,
    updateMetricSummaryText,
    setLocalizedTextBatch,
    getLocalizedText,
    addI18nAttributes,
    refreshLocalizedText,
    refreshContainerI18n
} from "../utils/i18n";

describe("i18n工具模块", () => {
    beforeEach(() => {
        document.body.innerHTML = "";
        (window as any).currentLanguage = "zh";
    });

    describe("setLocalizedText", () => {
        it("应设置中文文本并添加data属性", () => {
            const element = document.createElement("div");
            setLocalizedText(element, "中文", "English");

            expect(element.textContent).toBe("中文");
            expect(element.getAttribute("data-i18n-zh")).toBe("中文");
            expect(element.getAttribute("data-i18n-en")).toBe("English");
        });

        it("应根据当前语言设置文本", () => {
            const element = document.createElement("div");
            (window as any).currentLanguage = "en";

            setLocalizedText(element, "中文", "English");
            expect(element.textContent).toBe("English");
        });

        it("应处理null元素", () => {
            expect(() => setLocalizedText(null, "中文", "English")).not.toThrow();
        });

        it("应处理空文本并使用默认值", () => {
            const element = document.createElement("div");
            setLocalizedText(element, "", "");

            expect(element.textContent).toBe("--");
        });

        it("应在缺少英文翻译时使用中文", () => {
            const element = document.createElement("div");
            (window as any).currentLanguage = "en";

            setLocalizedText(element, "中文", "");
            expect(element.textContent).toBe("中文");
        });
    });

    describe("updateMetricSummaryText", () => {
        it("应更新容器内元素的文本", () => {
            const container = document.createElement("div");
            const target = document.createElement("span");
            target.className = "metric";
            container.appendChild(target);

            updateMetricSummaryText(container, ".metric", "测试", "Test");

            expect(target.textContent).toBe("测试");
            expect(target.getAttribute("data-i18n-zh")).toBe("测试");
        });

        it("应处理null容器", () => {
            expect(() => updateMetricSummaryText(null, ".metric", "测试", "Test")).not.toThrow();
        });

        it("应处理不存在的选择器", () => {
            const container = document.createElement("div");
            expect(() =>
                updateMetricSummaryText(container, ".nonexistent", "测试", "Test")
            ).not.toThrow();
        });

        it("应在文本为空时使用默认值", () => {
            const container = document.createElement("div");
            const target = document.createElement("span");
            target.className = "metric";
            container.appendChild(target);

            updateMetricSummaryText(container, ".metric", "", "");

            expect(target.textContent).toBe("暂无数据");
        });
    });

    describe("setLocalizedTextBatch", () => {
        it("应批量设置多个元素的本地化文本", () => {
            const elements = [
                document.createElement("div"),
                document.createElement("div"),
                document.createElement("div")
            ];

            const textMap = [
                { zh: "第一", en: "First" },
                { zh: "第二", en: "Second" },
                { zh: "第三", en: "Third" }
            ];

            setLocalizedTextBatch(elements, textMap);

            expect(elements[0].textContent).toBe("第一");
            expect(elements[1].textContent).toBe("第二");
            expect(elements[2].textContent).toBe("第三");
        });

        it("应在长度不匹配时显示警告", () => {
            const consoleSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
            const elements = [document.createElement("div")];
            const textMap = [
                { zh: "第一", en: "First" },
                { zh: "第二", en: "Second" }
            ];

            setLocalizedTextBatch(elements, textMap);

            expect(consoleSpy).toHaveBeenCalled();
            consoleSpy.mockRestore();
        });
    });

    describe("getLocalizedText", () => {
        it("应根据当前语言返回文本", () => {
            (window as any).currentLanguage = "zh";
            expect(getLocalizedText("中文", "English")).toBe("中文");

            (window as any).currentLanguage = "en";
            expect(getLocalizedText("中文", "English")).toBe("English");
        });

        it("应在缺少翻译时fallback到另一语言", () => {
            (window as any).currentLanguage = "en";
            expect(getLocalizedText("中文", "")).toBe("中文");
        });
    });

    describe("addI18nAttributes", () => {
        it("应添加i18n属性但不设置文本", () => {
            const element = document.createElement("div");
            element.textContent = "原始文本";

            addI18nAttributes(element, "中文", "English");

            expect(element.textContent).toBe("原始文本");
            expect(element.getAttribute("data-i18n-zh")).toBe("中文");
            expect(element.getAttribute("data-i18n-en")).toBe("English");
        });

        it("应处理null元素", () => {
            expect(() => addI18nAttributes(null, "中文", "English")).not.toThrow();
        });
    });

    describe("refreshLocalizedText", () => {
        it("应从data属性刷新文本", () => {
            const element = document.createElement("div");
            element.setAttribute("data-i18n-zh", "中文");
            element.setAttribute("data-i18n-en", "English");

            (window as any).currentLanguage = "zh";
            refreshLocalizedText(element);
            expect(element.textContent).toBe("中文");

            (window as any).currentLanguage = "en";
            refreshLocalizedText(element);
            expect(element.textContent).toBe("English");
        });

        it("应处理null元素", () => {
            expect(() => refreshLocalizedText(null)).not.toThrow();
        });
    });

    describe("refreshContainerI18n", () => {
        it("应刷新容器内所有带i18n属性的元素", () => {
            const container = document.createElement("div");

            const elem1 = document.createElement("div");
            elem1.setAttribute("data-i18n-zh", "第一");
            elem1.setAttribute("data-i18n-en", "First");

            const elem2 = document.createElement("div");
            elem2.setAttribute("data-i18n-zh", "第二");
            elem2.setAttribute("data-i18n-en", "Second");

            container.appendChild(elem1);
            container.appendChild(elem2);

            (window as any).currentLanguage = "en";
            refreshContainerI18n(container);

            expect(elem1.textContent).toBe("First");
            expect(elem2.textContent).toBe("Second");
        });

        it("应支持在document上刷新", () => {
            const elem = document.createElement("div");
            elem.setAttribute("data-i18n-zh", "测试");
            elem.setAttribute("data-i18n-en", "Test");
            document.body.appendChild(elem);

            (window as any).currentLanguage = "en";
            refreshContainerI18n(document);

            expect(elem.textContent).toBe("Test");
        });
    });
});
