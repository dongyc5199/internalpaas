/**
 * 国际化工具函数模块
 */

/**
 * 扩展Window接口以包含国际化配置
 */
interface WindowWithI18n extends Window {
    currentLanguage?: string;
}

/**
 * 设置元素的本地化文本
 * @param element - DOM元素
 * @param zh - 中文文本
 * @param en - 英文文本
 * @param currentLang - 当前语言 (默认从全局获取)
 */
export function setLocalizedText(
    element: HTMLElement | null,
    zh: string,
    en: string,
    currentLang?: string
): void {
    if (!element) {
        return;
    }

    const zhValue = zh || "";
    const enValue = en || "";

    element.setAttribute("data-i18n-zh", zhValue);
    element.setAttribute("data-i18n-en", enValue);

    // 如果未提供当前语言，尝试从全局变量获取
    const lang = currentLang || (window as unknown as WindowWithI18n).currentLanguage || "zh";

    element.textContent = lang === "en" ? enValue || zhValue || "--" : zhValue || enValue || "--";
}

/**
 * 更新指标摘要文本的本地化
 * @param container - 容器元素
 * @param selector - CSS选择器
 * @param zh - 中文文本
 * @param en - 英文文本
 */
export function updateMetricSummaryText(
    container: HTMLElement | null,
    selector: string,
    zh: string,
    en: string
): void {
    if (!container) {
        return;
    }

    const element = container.querySelector(selector) as HTMLElement;
    if (!element) {
        return;
    }

    const zhText = zh || "暂无数据";
    const enText = en || "No data";

    setLocalizedText(element, zhText, enText);
}

/**
 * 批量设置元素的本地化文本
 * @param elements - DOM元素数组
 * @param textMap - 文本映射表 [{zh: string, en: string}]
 */
export function setLocalizedTextBatch(
    elements: (HTMLElement | null)[],
    textMap: Array<{ zh: string; en: string }>
): void {
    if (elements.length !== textMap.length) {
        console.warn("[i18n] Elements and text map length mismatch");
        return;
    }

    elements.forEach((element, index) => {
        const { zh, en } = textMap[index];
        setLocalizedText(element, zh, en);
    });
}

/**
 * 根据当前语言获取文本
 * @param zh - 中文文本
 * @param en - 英文文本
 * @param currentLang - 当前语言
 */
export function getLocalizedText(zh: string, en: string, currentLang?: string): string {
    const lang = currentLang || (window as unknown as WindowWithI18n).currentLanguage || "zh";
    return lang === "en" ? en || zh : zh || en;
}

/**
 * 为元素添加国际化属性（不立即渲染）
 * @param element - DOM元素
 * @param zh - 中文文本
 * @param en - 英文文本
 */
export function addI18nAttributes(element: HTMLElement | null, zh: string, en: string): void {
    if (!element) {
        return;
    }

    element.setAttribute("data-i18n-zh", zh || "");
    element.setAttribute("data-i18n-en", en || "");
}

/**
 * 刷新元素的国际化文本（从data属性读取）
 * @param element - DOM元素
 * @param currentLang - 当前语言
 */
export function refreshLocalizedText(element: HTMLElement | null, currentLang?: string): void {
    if (!element) {
        return;
    }

    const zh = element.getAttribute("data-i18n-zh") || "";
    const en = element.getAttribute("data-i18n-en") || "";
    const lang = currentLang || (window as unknown as WindowWithI18n).currentLanguage || "zh";

    element.textContent = lang === "en" ? en || zh || "--" : zh || en || "--";
}

/**
 * 批量刷新容器内所有带i18n属性的元素
 * @param container - 容器元素
 * @param currentLang - 当前语言
 */
export function refreshContainerI18n(
    container: HTMLElement | Document,
    currentLang?: string
): void {
    const elements = container.querySelectorAll("[data-i18n-zh], [data-i18n-en]");

    elements.forEach((element) => {
        refreshLocalizedText(element as HTMLElement, currentLang);
    });
}
