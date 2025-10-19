/**
 * Server Import Modal Module
 * 服务器配置导入弹窗模块
 */

import { safeShowToast, showSuccess, showError } from "@/utils";
import { eventBus } from "@/utils/event-bus";

// SSH客户端配置路径映射
const CLIENT_CONFIGS = {
    securecrt: {
        name: "SecureCRT",
        icon: "🔐",
        defaultPaths: {
            windows: "C:\\Users\\{username}\\AppData\\Roaming\\VanDyke\\Config",
            mac: "~/Library/Application Support/VanDyke/SecureCRT/Config",
            linux: "~/.vandyke/SecureCRT/Config"
        },
        configFile: "Sessions"
    },
    xshell: {
        name: "Xshell",
        icon: "⚡",
        defaultPaths: {
            windows: "C:\\Users\\{username}\\Documents\\NetSarang Computer\\7\\Xshell\\Sessions",
            mac: null,
            linux: null
        },
        configFile: "*.xsh"
    },
    mobaxterm: {
        name: "MobaXterm",
        icon: "🎯",
        defaultPaths: {
            windows: "C:\\Users\\{username}\\Documents\\MobaXterm\\sessions",
            mac: null,
            linux: null
        },
        configFile: "MobaXterm.ini"
    },
    tabby: {
        name: "Tabby",
        icon: "🦊",
        defaultPaths: {
            windows: "C:\\Users\\{username}\\AppData\\Roaming\\tabby\\config.yaml",
            mac: "~/Library/Application Support/tabby/config.yaml",
            linux: "~/.config/tabby/config.yaml"
        },
        configFile: "config.yaml"
    },
    finalshell: {
        name: "FinalShell",
        icon: "🐚",
        defaultPaths: {
            windows: "C:\\Users\\{username}\\.finalshell\\conn",
            mac: "~/.finalshell/conn",
            linux: "~/.finalshell/conn"
        },
        configFile: "*.json"
    }
};

// 声明全局函数
declare function t(key: string, category?: string): string;
declare let currentLanguage: string;

// 安全的国际化函数
function safeT(key: string, category?: string): string {
    if (typeof t === "function") {
        try {
            return t(key, category);
        } catch (error) {
            console.warn(`Translation error for key: ${key}`, error);
        }
    }
    return key;
}

// 获取当前语言
function getCurrentLanguage(): string {
    return typeof currentLanguage !== "undefined" ? currentLanguage : "zh";
}

// 获取操作系统类型
function getOSType(): "windows" | "mac" | "linux" {
    const userAgent = navigator.userAgent.toLowerCase();
    if (userAgent.indexOf("win") > -1) return "windows";
    if (userAgent.indexOf("mac") > -1) return "mac";
    return "linux";
}

// 替换路径中的用户名占位符
function replacePath(path: string): string {
    const username = "YourName"; // 默认占位符，实际应从系统获取
    return path.replace("{username}", username);
}

export class ServerImportModal {
    private modal: HTMLElement | null = null;
    private currentStep: number = 1;
    private selectedClient: string = "";
    private configPath: string = "";
    private unsubscribeLanguageChange: (() => void) | null = null;
    private initialized: boolean = false;

    constructor() {
        // 延迟初始化，等待DOM加载完成
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", () => this.init());
        } else {
            this.init();
        }
    }

    private init() {
        if (this.initialized) return;
        
        console.log("Initializing Server Import Modal...");
        
        // 监听语言切换事件
        if (!this.unsubscribeLanguageChange) {
            this.unsubscribeLanguageChange = eventBus.on("language:changed", () => {
                this.handleLanguageChange();
            });
        }

        // 查找模态框元素
        this.modal = document.getElementById("serverImportModal");
        
        if (!this.modal) {
            console.error("Server Import Modal element not found");
            return;
        }

        this.attachEvents();
        this.initialized = true;
        console.log("Server Import Modal initialized");
    }

    private attachEvents() {
        if (!this.modal) return;

        // 关闭按钮事件
        const closeButtons = this.modal.querySelectorAll('[data-action="modal-close"]');
        closeButtons.forEach(btn => {
            btn.addEventListener("click", () => this.close());
        });

        // ESC键关闭
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape" && this.isOpen()) {
                this.close();
            }
        });

        // 客户端卡片点击事件
        const clientCards = this.modal.querySelectorAll(".client-card");
        clientCards.forEach(card => {
            card.addEventListener("click", () => {
                const client = card.getAttribute("data-client");
                if (client) {
                    this.selectClient(client);
                }
            });
        });

        // 下一步按钮
        const nextBtn = this.modal.querySelector("#nextBtn");
        nextBtn?.addEventListener("click", () => this.nextStep());

        // 上一步按钮
        const backBtn = this.modal.querySelector("#backBtn");
        backBtn?.addEventListener("click", () => this.prevStep());

        // 更改客户端按钮
        const changeClientBtn = this.modal.querySelector("#changeClientBtn");
        changeClientBtn?.addEventListener("click", () => this.changeClient());

        // 自动检测按钮
        const autoDetectBtn = this.modal.querySelector("#autoDetectBtn");
        autoDetectBtn?.addEventListener("click", () => this.autoDetectPath());

        // 浏览按钮
        const browsePathBtn = this.modal.querySelector("#browsePathBtn");
        browsePathBtn?.addEventListener("click", () => this.browsePath());

        // 导入按钮
        const importBtn = this.modal.querySelector("#importBtn");
        importBtn?.addEventListener("click", () => this.startImport());

        // 配置路径输入框
        const configPathInput = this.modal.querySelector("#configPath") as HTMLInputElement;
        configPathInput?.addEventListener("input", (e) => {
            this.configPath = (e.target as HTMLInputElement).value;
        });
    }

    public open() {
        if (!this.modal) return;
        
        this.reset();
        this.modal.style.display = "flex";
        
        // 触发重绘以确保动画生效
        requestAnimationFrame(() => {
            this.modal?.classList.add("active");
        });

        // 阻止背景滚动
        document.body.style.overflow = "hidden";
    }

    public close() {
        if (!this.modal) return;
        
        this.modal.classList.remove("active");
        
        setTimeout(() => {
            if (this.modal) {
                this.modal.style.display = "none";
            }
            document.body.style.overflow = "";
        }, 300);
    }

    public isOpen(): boolean {
        return this.modal?.classList.contains("active") || false;
    }

    private reset() {
        this.currentStep = 1;
        this.selectedClient = "";
        this.configPath = "";
        this.updateStepDisplay();
        this.clearClientSelection();
        this.clearConfigPath();
    }

    private selectClient(client: string) {
        if (!this.modal) return;

        this.selectedClient = client;

        // 更新卡片选中状态
        const cards = this.modal.querySelectorAll(".client-card");
        cards.forEach(card => {
            if (card.getAttribute("data-client") === client) {
                card.classList.add("selected");
            } else {
                card.classList.remove("selected");
            }
        });

        // 启用下一步按钮
        const nextBtn = this.modal.querySelector("#nextBtn") as HTMLButtonElement;
        if (nextBtn) {
            nextBtn.disabled = false;
        }
    }

    private clearClientSelection() {
        if (!this.modal) return;

        const cards = this.modal.querySelectorAll(".client-card");
        cards.forEach(card => card.classList.remove("selected"));

        const nextBtn = this.modal.querySelector("#nextBtn") as HTMLButtonElement;
        if (nextBtn) {
            nextBtn.disabled = true;
        }
    }

    private clearConfigPath() {
        const configPathInput = this.modal?.querySelector("#configPath") as HTMLInputElement;
        if (configPathInput) {
            configPathInput.value = "";
        }
        this.configPath = "";

        const detectionResult = this.modal?.querySelector("#detectionResult") as HTMLElement;
        if (detectionResult) {
            detectionResult.style.display = "none";
        }
    }

    private nextStep() {
        if (this.currentStep === 1 && !this.selectedClient) {
            showError(
                getCurrentLanguage() === "en" 
                    ? "Please select an SSH client" 
                    : "请选择一个SSH客户端"
            );
            return;
        }

        if (this.currentStep < 2) {
            this.currentStep++;
            this.updateStepDisplay();
            this.updateClientDisplay();
            this.loadDefaultPath();
        }
    }

    private prevStep() {
        if (this.currentStep > 1) {
            this.currentStep--;
            this.updateStepDisplay();
        }
    }

    private changeClient() {
        this.prevStep();
    }

    private updateStepDisplay() {
        if (!this.modal) return;

        // 更新步骤指示器
        const stepItems = this.modal.querySelectorAll(".step-item");
        stepItems.forEach((item, index) => {
            const stepNum = index + 1;
            if (stepNum < this.currentStep) {
                item.classList.add("completed");
                item.classList.remove("active");
            } else if (stepNum === this.currentStep) {
                item.classList.add("active");
                item.classList.remove("completed");
            } else {
                item.classList.remove("active", "completed");
            }
        });

        // 更新步骤内容
        const stepContents = this.modal.querySelectorAll(".step-content");
        stepContents.forEach((content, index) => {
            if (index + 1 === this.currentStep) {
                content.classList.add("active");
            } else {
                content.classList.remove("active");
            }
        });

        // 更新按钮显示
        const backBtn = this.modal.querySelector("#backBtn") as HTMLElement;
        const nextBtn = this.modal.querySelector("#nextBtn") as HTMLElement;
        const importBtn = this.modal.querySelector("#importBtn") as HTMLElement;

        if (backBtn) {
            backBtn.style.display = this.currentStep > 1 ? "inline-flex" : "none";
        }

        if (nextBtn) {
            nextBtn.style.display = this.currentStep < 2 ? "inline-flex" : "none";
        }

        if (importBtn) {
            importBtn.style.display = this.currentStep === 2 ? "inline-flex" : "none";
        }
    }

    private updateClientDisplay() {
        if (!this.modal || !this.selectedClient) return;

        const clientConfig = CLIENT_CONFIGS[this.selectedClient as keyof typeof CLIENT_CONFIGS];
        if (!clientConfig) return;

        const iconElement = this.modal.querySelector("#selectedClientIcon");
        const nameElement = this.modal.querySelector("#selectedClientName");

        if (iconElement) {
            iconElement.textContent = clientConfig.icon;
        }

        if (nameElement) {
            nameElement.textContent = clientConfig.name;
        }
    }

    private loadDefaultPath() {
        if (!this.modal || !this.selectedClient) return;

        const clientConfig = CLIENT_CONFIGS[this.selectedClient as keyof typeof CLIENT_CONFIGS];
        if (!clientConfig) return;

        const osType = getOSType();
        const defaultPath = clientConfig.defaultPaths[osType];

        if (defaultPath) {
            const resolvedPath = replacePath(defaultPath);
            
            // 更新默认路径提示
            const pathHintCode = this.modal.querySelector("#defaultPathText");
            if (pathHintCode) {
                pathHintCode.textContent = resolvedPath;
            }

            // 更新输入框占位符
            const configPathInput = this.modal.querySelector("#configPath") as HTMLInputElement;
            if (configPathInput) {
                configPathInput.placeholder = resolvedPath;
            }
        }
    }

    private autoDetectPath() {
        if (!this.selectedClient) return;

        const clientConfig = CLIENT_CONFIGS[this.selectedClient as keyof typeof CLIENT_CONFIGS];
        if (!clientConfig) return;

        const osType = getOSType();
        const defaultPath = clientConfig.defaultPaths[osType];

        if (!defaultPath) {
            showError(
                getCurrentLanguage() === "en"
                    ? "Auto-detection not supported on this platform"
                    : "此平台不支持自动检测"
            );
            return;
        }

        // 模拟检测过程
        const detectionResult = this.modal?.querySelector("#detectionResult") as HTMLElement;
        const detectedPathElement = this.modal?.querySelector("#detectedPath") as HTMLElement;
        const configPathInput = this.modal?.querySelector("#configPath") as HTMLInputElement;

        const resolvedPath = replacePath(defaultPath);

        // 显示检测结果
        if (detectionResult && detectedPathElement && configPathInput) {
            detectedPathElement.textContent = resolvedPath;
            detectionResult.style.display = "flex";
            configPathInput.value = resolvedPath;
            this.configPath = resolvedPath;

            showSuccess(
                getCurrentLanguage() === "en"
                    ? "Configuration path detected successfully"
                    : "成功检测到配置路径"
            );
        }
    }

    private browsePath() {
        // 注意：浏览器中无法直接使用文件夹选择器
        // 这里需要后端支持或使用Electron等桌面框架
        showError(
            getCurrentLanguage() === "en"
                ? "File browser requires backend support"
                : "文件浏览器需要后端支持"
        );
    }

    private async startImport() {
        if (!this.configPath) {
            showError(
                getCurrentLanguage() === "en"
                    ? "Please specify the configuration path"
                    : "请指定配置文件路径"
            );
            return;
        }

        try {
            // 这里应该调用后端API进行实际导入
            // const response = await fetch("/admin/server-groups/api/import", {
            //     method: "POST",
            //     headers: { "Content-Type": "application/json" },
            //     body: JSON.stringify({
            //         client: this.selectedClient,
            //         configPath: this.configPath
            //     })
            // });

            // 模拟导入过程
            showSuccess(
                getCurrentLanguage() === "en"
                    ? "Import started successfully"
                    : "开始导入配置"
            );

            // 关闭弹窗
            setTimeout(() => {
                this.close();
                // 刷新服务器列表
                if (typeof (window as any).refreshServerList === "function") {
                    (window as any).refreshServerList();
                }
            }, 1000);

        } catch (error) {
            console.error("Import failed:", error);
            showError(
                getCurrentLanguage() === "en"
                    ? "Import failed. Please try again."
                    : "导入失败，请重试"
            );
        }
    }

    private handleLanguageChange() {
        // 语言切换时更新界面
        if (this.isOpen()) {
            this.updateClientDisplay();
        }
    }

    public cleanup() {
        if (this.unsubscribeLanguageChange) {
            this.unsubscribeLanguageChange();
            this.unsubscribeLanguageChange = null;
        }
    }
}

// 导出单例实例
let serverImportModalInstance: ServerImportModal | null = null;

export function getServerImportModal(): ServerImportModal {
    if (!serverImportModalInstance) {
        console.log("Creating ServerImportModal instance...");
        serverImportModalInstance = new ServerImportModal();
    }
    return serverImportModalInstance;
}

// 全局函数供HTML调用
(window as any).openServerImportModal = function() {
    console.log("Global openServerImportModal called");
    const modal = getServerImportModal();
    modal.open();
};

// 页面加载时初始化
console.log("server-import-modal.ts loaded");

export {};
