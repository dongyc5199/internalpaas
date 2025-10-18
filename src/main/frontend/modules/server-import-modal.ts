/**
 * Server Import Modal Module - 3 Tab Version
 * 服务器配置导入弹窗模块 - 三标签页版本
 * 支持：自动扫描、手动指定、文件上传
 */

import { safeShowToast, showSuccess, showError } from "@/utils";
import { eventBus } from "@/utils/event-bus";

// SSH客户端配置映射
const CLIENT_CONFIGS = {
    securecrt: {
        name: "SecureCRT",
        icon: "🔐",
        desc: "强大的终端模拟器",
        defaultPaths: {
            windows: "C:\\Users\\{username}\\AppData\\Roaming\\VanDyke\\Config\\Sessions",
            mac: "~/Library/Application Support/VanDyke/SecureCRT/Config/Sessions",
            linux: "~/.vandyke/SecureCRT/Config/Sessions"
        },
        configFile: "Sessions"
    },
    xshell: {
        name: "Xshell",
        icon: "📡",
        desc: "NetSarang终端工具",
        defaultPaths: {
            windows: "C:\\Users\\{username}\\Documents\\NetSarang Computer\\7\\Xshell\\Sessions",
            mac: null,
            linux: null
        },
        configFile: "*.xsh"
    },
    tabby: {
        name: "Tabby",
        icon: "⚡",
        desc: "现代跨平台终端",
        defaultPaths: {
            windows: "C:\\Users\\{username}\\AppData\\Roaming\\tabby\\config.yaml",
            mac: "~/Library/Application Support/tabby/config.yaml",
            linux: "~/.config/tabby/config.yaml"
        },
        configFile: "config.yaml"
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
    private currentTab: string = "auto-scan"; // auto-scan | manual | upload
    private selectedClient: string = "";
    private manualStep: number = 1; // Manual tab的步骤：1=选择客户端, 2=配置路径
    private configPath: string = "";
    private uploadedFile: File | null = null;
    private scanResults: any = null;
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
        
        console.log("Initializing Server Import Modal (3-Tab Version)...");
        
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
        console.log("Server Import Modal initialized (3-Tab Version)");
    }

    private attachEvents() {
        if (!this.modal) return;

        // 关闭按钮事件
        const closeBtn = this.modal.querySelector('[data-action="modal-close"]');
        closeBtn?.addEventListener("click", () => this.close());

        // Overlay点击关闭
        this.modal.addEventListener("click", (e) => {
            if (e.target === this.modal) {
                this.close();
            }
        });

        // ESC键关闭
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape" && this.isOpen()) {
                this.close();
            }
        });

        // 标签页切换
        const tabs = this.modal.querySelectorAll(".import-tab");
        tabs.forEach(tab => {
            tab.addEventListener("click", () => {
                const tabName = tab.getAttribute("data-tab");
                if (tabName) {
                    this.switchTab(tabName);
                }
            });
        });

        // === Auto Scan Tab Events ===
        const startScanBtn = this.modal.querySelector("#startScanBtn");
        startScanBtn?.addEventListener("click", () => this.startAutoScan());

        // === Manual Tab Events ===
        const clientCards = this.modal.querySelectorAll(".client-card");
        clientCards.forEach(card => {
            card.addEventListener("click", () => {
                const client = card.getAttribute("data-client");
                if (client) {
                    this.selectClient(client);
                }
            });
        });

        const nextStepBtn = this.modal.querySelector("#nextStepBtn");
        nextStepBtn?.addEventListener("click", () => this.manualNextStep());

        const changeClientBtn = this.modal.querySelector("#changeClientBtn");
        changeClientBtn?.addEventListener("click", () => this.manualPrevStep());

        const configPathInput = this.modal.querySelector("#manualConfigPath") as HTMLInputElement;
        configPathInput?.addEventListener("input", (e) => {
            this.configPath = (e.target as HTMLInputElement).value;
        });

        const browseBtn = this.modal.querySelector("#browsePathBtn");
        browseBtn?.addEventListener("click", () => this.browsePath());

        // 路径选项单选
        const configRadio = this.modal.querySelector("#pathConfig") as HTMLInputElement;
        const installRadio = this.modal.querySelector("#pathInstall") as HTMLInputElement;
        
        configRadio?.addEventListener("change", () => {
            if (configRadio.checked) {
                this.loadDefaultPath("config");
            }
        });

        installRadio?.addEventListener("change", () => {
            if (installRadio.checked) {
                this.loadDefaultPath("install");
            }
        });

        // === Upload Tab Events ===
        const uploadArea = this.modal.querySelector("#uploadArea");
        const fileInput = this.modal.querySelector("#fileInput") as HTMLInputElement;

        uploadArea?.addEventListener("click", () => {
            fileInput?.click();
        });

        uploadArea?.addEventListener("dragover", (e) => {
            e.preventDefault();
            uploadArea.classList.add("dragover");
        });

        uploadArea?.addEventListener("dragleave", () => {
            uploadArea.classList.remove("dragover");
        });

        uploadArea?.addEventListener("drop", (e) => {
            e.preventDefault();
            uploadArea.classList.remove("dragover");
            const files = (e as DragEvent).dataTransfer?.files;
            if (files && files.length > 0) {
                this.handleFileUpload(files[0]);
            }
        });

        fileInput?.addEventListener("change", (e) => {
            const files = (e.target as HTMLInputElement).files;
            if (files && files.length > 0) {
                this.handleFileUpload(files[0]);
            }
        });

        // === Footer Buttons ===
        const cancelBtn = this.modal.querySelector("#cancelBtn");
        cancelBtn?.addEventListener("click", () => this.close());

        const confirmImportBtn = this.modal.querySelector("#confirmImportBtn");
        confirmImportBtn?.addEventListener("click", () => this.confirmImport());
    }

    public open() {
        console.log("ServerImportModal.open() called");
        console.log("this.modal:", this.modal);
        
        if (!this.modal) {
            console.error("Modal element not found! Cannot open modal.");
            return;
        }
        
        console.log("Calling reset()...");
        this.reset();
        
        console.log("Adding 'active' class to modal overlay...");
        this.modal.classList.add("active");
        console.log("Modal classes after adding 'active':", this.modal.className);
        
        // 阻止背景滚动
        document.body.style.overflow = "hidden";
        console.log("Modal should now be visible");
    }

    public close() {
        if (!this.modal) return;
        
        this.modal.classList.remove("active");
        document.body.style.overflow = "";
    }

    public isOpen(): boolean {
        return this.modal?.classList.contains("active") || false;
    }

    private reset() {
        this.currentTab = "auto-scan";
        this.manualStep = 1;
        this.selectedClient = "";
        this.configPath = "";
        this.uploadedFile = null;
        this.scanResults = null;
        
        this.switchTab("auto-scan");
        this.resetAutoScan();
        this.resetManual();
        this.resetUpload();
        this.updateConfirmButton();
    }

    // ===== Tab Switching =====
    private switchTab(tabName: string) {
        if (!this.modal) return;

        this.currentTab = tabName;

        // 更新标签按钮状态
        const tabs = this.modal.querySelectorAll(".import-tab");
        tabs.forEach(tab => {
            if (tab.getAttribute("data-tab") === tabName) {
                tab.classList.add("active");
            } else {
                tab.classList.remove("active");
            }
        });

        // 更新内容显示
        const contents = this.modal.querySelectorAll(".tab-content");
        contents.forEach(content => {
            if (content.id === tabName) {
                content.classList.add("active");
            } else {
                content.classList.remove("active");
            }
        });

        this.updateConfirmButton();
    }

    // ===== Auto Scan Tab Logic =====
    private async startAutoScan() {
        if (!this.modal) return;

        const scanStatus = this.modal.querySelector("#scanningStatus") as HTMLElement;
        const scanResult = this.modal.querySelector("#scanResult") as HTMLElement;
        const progressFill = this.modal.querySelector("#progressFill") as HTMLElement;
        const step1 = this.modal.querySelector("#step1");
        const step2 = this.modal.querySelector("#step2");
        const step3 = this.modal.querySelector("#step3");

        // 显示扫描状态
        scanStatus.classList.add("active");
        scanResult.classList.remove("active");

        // 模拟扫描过程
        const steps = [
            { element: step1, label: "completed", progress: 33 },
            { element: step2, label: "completed", progress: 66 },
            { element: step3, label: "completed", progress: 100 }
        ];

        for (const { element, label, progress } of steps) {
            element?.classList.remove("active", "completed");
            element?.classList.add("active");
            progressFill.style.width = `${progress}%`;
            await new Promise(resolve => setTimeout(resolve, 800));
            element?.classList.remove("active");
            element?.classList.add(label);
        }

        // 模拟扫描结果
        await new Promise(resolve => setTimeout(resolve, 500));
        
        this.scanResults = {
            client: "SecureCRT",
            path: "C:\\Users\\Admin\\AppData\\Roaming\\VanDyke\\Config\\Sessions",
            count: 23
        };

        // 更新扫描结果显示
        const detectedClientName = this.modal.querySelector("#detectedClientName");
        const detectedConfigPath = this.modal.querySelector("#detectedConfigPath");
        const detectedServerCount = this.modal.querySelector("#detectedServerCount");

        if (detectedClientName) detectedClientName.textContent = this.scanResults.client;
        if (detectedConfigPath) detectedConfigPath.textContent = this.scanResults.path;
        if (detectedServerCount) detectedServerCount.textContent = `${this.scanResults.count} 个`;

        // 显示结果
        scanStatus.classList.remove("active");
        scanResult.classList.add("active");

        showSuccess(
            getCurrentLanguage() === "en"
                ? "Scan completed successfully"
                : "扫描完成"
        );

        this.updateConfirmButton();
    }

    private resetAutoScan() {
        if (!this.modal) return;

        const scanStatus = this.modal.querySelector("#scanningStatus") as HTMLElement;
        const scanResult = this.modal.querySelector("#scanResult") as HTMLElement;
        const progressFill = this.modal.querySelector("#progressFill") as HTMLElement;

        scanStatus.classList.remove("active");
        scanResult.classList.remove("active");
        progressFill.style.width = "0%";

        const steps = this.modal.querySelectorAll(".scan-step");
        steps.forEach(step => {
            step.classList.remove("active", "completed");
        });

        this.scanResults = null;
    }

    // ===== Manual Tab Logic =====
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
        const nextBtn = this.modal.querySelector("#nextStepBtn") as HTMLButtonElement;
        if (nextBtn) {
            nextBtn.disabled = false;
        }
    }

    private manualNextStep() {
        if (!this.selectedClient) {
            showError(
                getCurrentLanguage() === "en"
                    ? "Please select an SSH client"
                    : "请选择一个SSH客户端"
            );
            return;
        }

        if (this.manualStep < 2) {
            this.manualStep = 2;
            this.updateManualStepDisplay();
            this.updateSelectedClientDisplay();
            this.loadDefaultPath("config");
        }
    }

    private manualPrevStep() {
        if (this.manualStep > 1) {
            this.manualStep = 1;
            this.updateManualStepDisplay();
        }
    }

    private updateManualStepDisplay() {
        if (!this.modal) return;

        const step1 = this.modal.querySelector("#manualStep1");
        const step2 = this.modal.querySelector("#manualStep2");

        if (this.manualStep === 1) {
            step1?.classList.add("active");
            step2?.classList.remove("active");
        } else {
            step1?.classList.remove("active");
            step2?.classList.add("active");
        }

        this.updateConfirmButton();
    }

    private updateSelectedClientDisplay() {
        if (!this.modal || !this.selectedClient) return;

        const clientConfig = CLIENT_CONFIGS[this.selectedClient as keyof typeof CLIENT_CONFIGS];
        if (!clientConfig) return;

        const iconElement = this.modal.querySelector("#selectedClientIcon");
        const nameElement = this.modal.querySelector("#selectedClientName");

        if (iconElement) iconElement.textContent = clientConfig.icon;
        if (nameElement) nameElement.textContent = clientConfig.name;
    }

    private loadDefaultPath(type: "config" | "install") {
        if (!this.modal || !this.selectedClient) return;

        const clientConfig = CLIENT_CONFIGS[this.selectedClient as keyof typeof CLIENT_CONFIGS];
        if (!clientConfig) return;

        const osType = getOSType();
        const defaultPath = clientConfig.defaultPaths[osType];

        if (!defaultPath) {
            showError(
                getCurrentLanguage() === "en"
                    ? "Not supported on this platform"
                    : "此平台不支持此客户端"
            );
            return;
        }

        let resolvedPath = replacePath(defaultPath);

        // 如果选择的是安装目录，调整路径
        if (type === "install") {
            // 简化路径到可能的安装目录
            if (osType === "windows") {
                resolvedPath = "C:\\Program Files\\SecureCRT"; // 示例
            }
        }

        const pathInput = this.modal.querySelector("#manualConfigPath") as HTMLInputElement;
        if (pathInput) {
            pathInput.value = resolvedPath;
            this.configPath = resolvedPath;
        }
    }

    private browsePath() {
        showError(
            getCurrentLanguage() === "en"
                ? "File browser requires backend support"
                : "文件浏览器需要后端支持"
        );
    }

    private resetManual() {
        if (!this.modal) return;

        this.manualStep = 1;
        this.selectedClient = "";
        this.configPath = "";

        const cards = this.modal.querySelectorAll(".client-card");
        cards.forEach(card => card.classList.remove("selected"));

        const nextBtn = this.modal.querySelector("#nextStepBtn") as HTMLButtonElement;
        if (nextBtn) nextBtn.disabled = true;

        const pathInput = this.modal.querySelector("#manualConfigPath") as HTMLInputElement;
        if (pathInput) pathInput.value = "";

        this.updateManualStepDisplay();
    }

    // ===== Upload Tab Logic =====
    private handleFileUpload(file: File) {
        if (!this.modal) return;

        // 验证文件类型
        const validExtensions = [".ini", ".zip", ".xsh", ".yaml", ".json"];
        const fileExtension = file.name.substring(file.name.lastIndexOf(".")).toLowerCase();

        if (!validExtensions.includes(fileExtension)) {
            showError(
                getCurrentLanguage() === "en"
                    ? "Invalid file format. Please upload .ini, .zip, .xsh, .yaml, or .json files"
                    : "文件格式不支持，请上传 .ini、.zip、.xsh、.yaml 或 .json 文件"
            );
            return;
        }

        this.uploadedFile = file;

        // 显示上传结果
        const uploadResult = this.modal.querySelector("#uploadResult") as HTMLElement;
        const uploadFileName = this.modal.querySelector("#uploadFileName");
        const uploadFileSize = this.modal.querySelector("#uploadFileSize");

        if (uploadFileName) uploadFileName.textContent = file.name;
        if (uploadFileSize) uploadFileSize.textContent = this.formatFileSize(file.size);

        uploadResult.classList.add("active");

        showSuccess(
            getCurrentLanguage() === "en"
                ? "File uploaded successfully"
                : "文件上传成功"
        );

        this.updateConfirmButton();
    }

    private formatFileSize(bytes: number): string {
        if (bytes < 1024) return `${bytes} B`;
        if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
        return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    }

    private resetUpload() {
        if (!this.modal) return;

        this.uploadedFile = null;
        
        const uploadResult = this.modal.querySelector("#uploadResult") as HTMLElement;
        uploadResult.classList.remove("active");

        const fileInput = this.modal.querySelector("#fileInput") as HTMLInputElement;
        if (fileInput) fileInput.value = "";
    }

    // ===== Import Confirmation =====
    private updateConfirmButton() {
        if (!this.modal) return;

        const confirmBtn = this.modal.querySelector("#confirmImportBtn") as HTMLButtonElement;
        if (!confirmBtn) return;

        let canImport = false;

        switch (this.currentTab) {
            case "auto-scan":
                canImport = this.scanResults !== null;
                break;
            case "manual":
                canImport = this.manualStep === 2 && this.configPath.trim() !== "";
                break;
            case "upload":
                canImport = this.uploadedFile !== null;
                break;
        }

        confirmBtn.disabled = !canImport;
    }

    private async confirmImport() {
        try {
            let importData: any = null;

            switch (this.currentTab) {
                case "auto-scan":
                    importData = {
                        method: "auto-scan",
                        data: this.scanResults
                    };
                    break;
                case "manual":
                    importData = {
                        method: "manual",
                        client: this.selectedClient,
                        configPath: this.configPath
                    };
                    break;
                case "upload":
                    importData = {
                        method: "upload",
                        file: this.uploadedFile
                    };
                    break;
            }

            console.log("Starting import:", importData);

            // 这里应该调用后端API
            // const response = await fetch("/admin/server-groups/api/import", {
            //     method: "POST",
            //     headers: { "Content-Type": "application/json" },
            //     body: JSON.stringify(importData)
            // });

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
            this.updateSelectedClientDisplay();
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
        console.log("Creating ServerImportModal instance (3-Tab Version)...");
        serverImportModalInstance = new ServerImportModal();
    }
    return serverImportModalInstance;
}

// 全局函数供HTML调用
(window as any).openServerImportModal = function() {
    console.log("Global openServerImportModal called (3-Tab Version)");
    const modal = getServerImportModal();
    modal.open();
};

// 页面加载时初始化
console.log("server-import-modal.ts loaded (3-Tab Version)");

export {};
