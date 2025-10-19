/**
 * SSH Config Import Wizard Module
 * SSH配置文件导入向导模块
 *
 * 功能：
 * - 步骤1：选择配置文件（扫描本地/上传文件/输入路径）
 * - 步骤2：预览服务器列表（去重检查、字段验证、行内编辑）
 * - 步骤3：显示导入结果（成功/失败详情）
 *
 * @author Generated AI Assistant
 * @since 2025-10-18
 */

import { safeShowToast, showSuccess, showError } from "@/utils";
import { eventBus } from "@/utils/event-bus";

// ==================== TypeScript接口定义 ====================

/**
 * SSH主机配置（对应Java: SSHHostConfig）
 */
export interface SSHHostConfig {
    hostPattern: string;
    hostname: string | null;
    port: number | null;
    user: string | null;
    identityFile: string | null;
    proxyJump: string | null;
    extraOptions: Map<string, string>;
}

/**
 * 服务器导入预览DTO（对应Java: ServerImportDto）
 */
export interface ServerImportPreview {
    // 必填字段
    name: string;
    hostname: string;
    sshPort: number;
    sshUsername: string;

    // 认证凭证（二选一）
    sshPassword?: string | null;
    sshKeyPath?: string | null;

    // 可选字段
    port?: number;                    // 应用端口，默认8080
    baseWorkDirectory?: string | null;
    description?: string | null;
    serverType?: "DEVELOPMENT" | "TESTING" | "STAGING" | "PRODUCTION" | null;

    // 验证状态
    valid: boolean;                   // 是否有效（必填字段齐全）
    duplicate: boolean;               // 是否重复（与现有服务器冲突）
    missingFields: string[];          // 缺失字段列表

    // 前端扩展字段
    selected?: boolean;               // 是否勾选
    editing?: boolean;                // 是否正在编辑
}

/**
 * SSH配置解析结果（对应Java: SSHConfigParseResult）
 */
export interface SSHConfigParseResult {
    totalHosts: number;               // 解析到的Host数量
    servers: ServerImportPreview[];   // 转换后的服务器列表
    warnings: string[];               // 警告信息（如ProxyJump、Include等）
    errors: string[];                 // 错误信息
}

/**
 * 导入失败详情
 */
export interface ImportFailure {
    serverName: string;
    hostname: string;
    reason: string;
}

/**
 * 服务器导入结果（对应Java: ServerImportResult）
 */
export interface ServerImportResult {
    successCount: number;             // 成功数量
    failedCount: number;              // 失败数量
    successServers: {                 // 成功服务器列表
        id: number;
        name: string;
        hostname: string;
    }[];
    failures: ImportFailure[];        // 失败详情
}

/**
 * 默认配置路径信息
 */
export interface DefaultPathInfo {
    path: string;
    exists: boolean;
    message: string;
}

// ==================== SSH配置导入向导类 ====================

export class SSHConfigImportWizard {
    // 私有属性
    private modal: HTMLElement | null = null;
    private currentTab: 'auto-scan' | 'upload' | 'manual' = 'auto-scan'; // 当前激活的Tab
    private servers: ServerImportPreview[] = [];         // 解析出的服务器列表
    private selectedServers: ServerImportPreview[] = []; // 勾选的服务器列表
    private importResult: ServerImportResult | null = null;

    // 步骤1 - 文件选择相关
    private uploadedFile: File | null = null;
    private customPath: string = "";
    private defaultPathInfo: DefaultPathInfo | null = null;

    // 事件订阅清理函数
    private unsubscribeLanguageChange: (() => void) | null = null;
    private initialized: boolean = false;
    private globalEventsAttached: boolean = false;

    // ==================== 构造函数与初始化 ====================

    constructor() {
        // 延迟初始化，等待DOM加载完成
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", () => this.init());
        } else {
            this.init();
        }

        // 绑定全局事件（只执行一次）
        this.attachGlobalEvents();
    }

    /**
     * 初始化向导
     */
    private init() {
        if (this.initialized) return;

        console.log("Initializing SSH Config Import Wizard...");

        // 监听语言切换事件
        if (!this.unsubscribeLanguageChange) {
            this.unsubscribeLanguageChange = eventBus.on("language:changed", () => {
                this.handleLanguageChange();
            });
        }

        // 查找模态框元素
        this.modal = document.getElementById("sshConfigImportModal");

        if (!this.modal) {
            console.warn("SSH Config Import Modal element not found (will be created later)");
            // 不直接返回，允许后续动态创建
        } else {
            this.attachEvents();
        }

        this.initialized = true;
        console.log("SSH Config Import Wizard initialized");
    }

    /**
     * 绑定全局事件（使用事件委托，只绑定一次）
     */
    private attachGlobalEvents() {
        if (this.globalEventsAttached) return;

        console.log("Attaching global delegated events for SSH Config Import Wizard...");

        // ESC键关闭
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape" && this.isOpen()) {
                this.close();
            }
        });

        // 使用事件委托处理modal内的所有点击
        document.addEventListener("click", (e) => {
            const target = e.target as HTMLElement;

            // 关闭按钮
            if (target.closest('[data-action="ssh-config-modal-close"]')) {
                const modal = target.closest("#sshConfigImportModal");
                if (modal && this.isOpen()) {
                    e.preventDefault();
                    e.stopPropagation();
                    console.log("SSH Config Import Modal: Close button clicked");
                    this.close();
                }
                return;
            }

            // 点击遮罩层关闭
            if (target.id === "sshConfigImportModal" && target.classList.contains("modal-overlay")) {
                console.log("SSH Config Import Modal: Overlay clicked");
                this.close();
                return;
            }
        });

        this.globalEventsAttached = true;
        console.log("Global delegated events attached for SSH Config Import Wizard");
    }

    /**
     * 绑定模态框内事件
     */
    private attachEvents() {
        if (!this.modal) return;

        // 检查是否已经绑定过事件
        if (this.modal.hasAttribute("data-ssh-config-events-attached")) {
            console.log("Events already attached to SSH Config Import Modal");
            return;
        }

        console.log("Attaching events to SSH Config Import Modal (Tab version)...");

        // ===== Tab切换事件 =====
        // 支持两种命名：旧版使用 .tab，新版模板使用 .import-tab
        const tabSelectors = ['.tab', '.import-tab'];
        for (const sel of tabSelectors) {
            const tabs = Array.from(this.modal.querySelectorAll<HTMLButtonElement>(sel));
            if (tabs.length > 0) {
                tabs.forEach(tab => {
                    tab.addEventListener('click', () => {
                        const tabName = (tab.dataset.tab as 'auto-scan' | 'upload' | 'manual') || tab.getAttribute('data-tab');
                        if (tabName) {
                            this.switchTab(tabName as any);
                        }
                    });
                });
                break; // found matching selector, stop searching
            }
        }

        // ===== 自动扫描Tab事件 =====
        // 新版模板中按钮 id 为 #startScanBtn，旧版可能为 #btnStartScan，兼容两者
        const btnStartScan = this.modal.querySelector<HTMLButtonElement>('#startScanBtn') || this.modal.querySelector<HTMLButtonElement>('#btnStartScan');
        btnStartScan?.addEventListener('click', () => {
            this.startAutoScan();
        });

        // ===== 文件上传Tab事件 =====
        const uploadArea = this.modal.querySelector<HTMLDivElement>('#uploadArea');
        const fileInput = this.modal.querySelector<HTMLInputElement>('#sshConfigFileInput');

        // 点击上传区域触发文件选择
        uploadArea?.addEventListener('click', (e) => {
            if ((e.target as HTMLElement).tagName !== 'INPUT') {
                fileInput?.click();
            }
        });

        // 文件选择事件
        fileInput?.addEventListener('change', (e) => {
            const file = (e.target as HTMLInputElement).files?.[0];
            if (file) {
                this.handleFileUpload(file);
            }
        });

        // 拖拽事件
        uploadArea?.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadArea.classList.add('dragover');
        });

        uploadArea?.addEventListener('dragleave', () => {
            uploadArea.classList.remove('dragover');
        });

        uploadArea?.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadArea.classList.remove('dragover');
            const file = e.dataTransfer?.files[0];
            if (file) {
                this.handleFileUpload(file);
            }
        });

        // ===== 自定义路径Tab事件 =====
        // 路径示例点击事件
        this.modal.querySelectorAll<HTMLDivElement>('.path-example-item').forEach(item => {
            item.addEventListener('click', () => {
                const path = item.dataset.path;
                if (path) {
                    const pathInput = this.modal?.querySelector<HTMLInputElement>('#manualPathInput');
                    if (pathInput) {
                        pathInput.value = path;
                        pathInput.focus();
                    }
                }
            });
        });

        // 解析自定义路径按钮
        const btnParsePath = this.modal.querySelector<HTMLButtonElement>('#btnParsePath');
        btnParsePath?.addEventListener('click', () => {
            const pathInput = this.modal?.querySelector<HTMLInputElement>('#manualPathInput');
            if (pathInput?.value) {
                this.parseCustomPath(pathInput.value);
            } else {
                showError('请输入配置文件路径');
            }
        });

        // ===== 全局按钮事件 =====
        // 确认导入按钮
        const btnConfirmImport = this.modal.querySelector<HTMLButtonElement>('#btnConfirmImport');
        btnConfirmImport?.addEventListener('click', () => {
            this.confirmImport();
        });

        // 标记已绑定事件
        this.modal.setAttribute("data-ssh-config-events-attached", "true");
        console.log("Events attached to SSH Config Import Modal (Tab version)");
    }

    /**
     * 切换Tab
     */
    private switchTab(tabName: 'auto-scan' | 'upload' | 'manual') {
        console.log(`Switching to tab: ${tabName}`);
        this.currentTab = tabName;

        // 更新Tab按钮状态
        const tabs = this.modal?.querySelectorAll<HTMLButtonElement>('.tab');
        tabs?.forEach(tab => {
            if (tab.dataset.tab === tabName) {
                tab.classList.add('active');
            } else {
                tab.classList.remove('active');
            }
        });

        // 更新Tab内容显示
        const contents = this.modal?.querySelectorAll<HTMLDivElement>('.tab-content');
        contents?.forEach(content => {
            if (content.id === tabName) {
                content.classList.add('active');
            } else {
                content.classList.remove('active');
            }
        });
    }

    // ==================== 公共方法：打开/关闭 ====================

    /**
     * 打开导入向导模态框
     */
    public open() {
        console.log("SSHConfigImportWizard.open() called");

        // 每次打开时重新查找DOM元素，避免缓存过期的引用
        this.modal = document.getElementById("sshConfigImportModal");

        if (!this.modal) {
            console.error("SSH Config Import Modal element not found! Cannot open modal.");
            console.error("Attempting to reinitialize...");
            // 尝试重新初始化
            this.initialized = false;
            this.init();

            // 再次尝试获取
            this.modal = document.getElementById("sshConfigImportModal");
            if (!this.modal) {
                console.error("SSH Config Import Modal element still not found after reinit!");
                showError("无法打开SSH配置导入向导，请刷新页面后重试");
                return;
            }
        }

        // 重新绑定事件（因为DOM可能已被重新创建）
        console.log("Re-attaching events...");
        this.attachEvents();

        // 重置向导状态
        console.log("Resetting wizard state...");
        this.reset();

        // 显示模态框
        console.log("Adding 'active' class to modal overlay...");
        this.modal.classList.add("active");
        // 移除内联的 display: none 样式
        this.modal.style.display = "flex";

        // 阻止背景滚动
        document.body.style.overflow = "hidden";
        console.log("SSH Config Import Modal should now be visible");
    }

    /**
     * 关闭导入向导模态框
     */
    public close() {
        if (!this.modal) return;

        console.log("Closing SSH Config Import Modal");
        this.modal.classList.remove("active");
        // 恢复 display: none
        this.modal.style.display = "none";
        document.body.style.overflow = "";

        // 重置状态
        this.reset();
    }

    /**
     * 判断模态框是否打开
     */
    public isOpen(): boolean {
        return this.modal?.classList.contains("active") || false;
    }

    /**
     * 重置向导状态
     */
    private reset() {
        this.step = 1;
        this.servers = [];
        this.selectedServers = [];
        this.importResult = null;
        this.uploadedFile = null;
        this.customPath = "";
        this.defaultPathInfo = null;

        // Tab版本: 重置Tab状态
        this.currentTab = "auto-scan";
        this.switchTab("auto-scan");

        // 清空所有结果显示
        const scanResult = this.modal?.querySelector<HTMLDivElement>("#scanResult");
        const uploadResult = this.modal?.querySelector<HTMLDivElement>("#uploadResult");
        const parseResult = this.modal?.querySelector<HTMLDivElement>("#parseResult");

        scanResult?.classList.remove("active");
        uploadResult?.classList.remove("active");
        parseResult?.classList.remove("active");

        // 重置文件输入
        const fileInput = this.modal?.querySelector<HTMLInputElement>("#sshConfigFileInput");
        if (fileInput) {
            fileInput.value = "";
        }

        // 重置路径输入
        const pathInput = this.modal?.querySelector<HTMLInputElement>("#manualPathInput");
        if (pathInput) {
            pathInput.value = "";
        }

        console.log("Wizard reset to initial state (Tab version)");
    }

    // ==================== API调用方法（待实现） ====================

    /**
     * T6.2.1: 处理文件上传
     * 上传SSH配置文件到后端API并解析
     *
     * @param file 上传的文件
     */
    public async handleFileUpload(file: File): Promise<void> {
        console.log("handleFileUpload called:", file.name);

        try {
            // 1. 验证文件类型（text/plain, application/octet-stream, 或无类型）
            const validTypes = ["text/plain", "application/octet-stream", ""];
            if (file.type && !validTypes.includes(file.type)) {
                showError(`文件类型不支持：${file.type}。请上传SSH配置文件（通常为纯文本文件）`);
                return;
            }

            // 2. 验证文件大小（最大1MB）
            const MAX_FILE_SIZE = 1024 * 1024; // 1MB
            if (file.size > MAX_FILE_SIZE) {
                showError(`文件大小超过限制：${this.formatFileSize(file.size)}。最大允许 1 MB`);
                return;
            }

            // 保存文件引用
            this.uploadedFile = file;

            // 3. 创建FormData并上传
            const formData = new FormData();
            formData.append("file", file);

            // 显示加载状态
            console.log("Uploading file to /api/ssh-config-import/upload...");

            // 4. 调用 POST /api/ssh-config-import/upload
            const response = await fetch("/api/ssh-config-import/upload", {
                method: "POST",
                body: formData,
                // 不设置Content-Type，让浏览器自动设置multipart/form-data边界
            });

            if (!response.ok) {
                // 处理HTTP错误状态
                const errorData = await response.json().catch(() => null);
                const errorMessage = errorData?.message || `上传失败：HTTP ${response.status}`;
                throw new Error(errorMessage);
            }

            // 5. 解析响应，填充servers列表
            const result: SSHConfigParseResult = await response.json();

            console.log("Upload successful, parsed servers:", result);

            // 验证响应数据
            if (!result.servers || result.servers.length === 0) {
                showError("配置文件中未找到有效的SSH Host配置，请检查文件格式");
                return;
            }

            // 初始化服务器列表（添加前端扩展字段）
            this.servers = result.servers.map(server => ({
                ...server,
                selected: server.valid && !server.duplicate, // 默认选中有效且不重复的服务器
                editing: false
            }));

            // 更新选中列表
            this.updateSelectedServers();

            // 显示警告信息（如果有）
            if (result.warnings && result.warnings.length > 0) {
                console.warn("Parse warnings:", result.warnings);
                result.warnings.forEach(warning => {
                    safeShowToast(warning, "warning");
                });
            }

            // 显示成功消息
            showSuccess(`文件上传成功！解析到 ${result.totalHosts} 个Host，${this.servers.filter(s => s.valid && !s.duplicate).length} 个可导入`);

            // Tab版本:显示上传结果在当前Tab
            const uploadResult = this.modal?.querySelector<HTMLDivElement>("#uploadResult");
            if (uploadResult) {
                uploadResult.classList.add("active");
                uploadResult.innerHTML = `
                    <div class="success-box">
                        <div class="success-icon">✓</div>
                        <h3>上传成功</h3>
                        <div class="info-item">
                            <div class="info-label">解析到的Host数量</div>
                            <div class="info-value">${result.totalHosts}</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">可导入的服务器</div>
                            <div class="info-value">${this.servers.filter(s => s.valid && !s.duplicate).length}</div>
                        </div>
                    </div>
                `;
            }

        } catch (error) {
            console.error("File upload failed:", error);
            const errorMessage = error instanceof Error ? error.message : "文件上传失败，请重试";
            showError(errorMessage);
        }
    }

    /**
     * T6.2.2: 扫描本地SSH配置
     * 调用后端API扫描默认路径 ~/.ssh/config
     */
    public async scanLocalConfig(): Promise<void> {
        console.log("scanLocalConfig called");

        try {
            // 1. 显示加载状态
            console.log("Scanning local SSH config at default path...");
            // TODO: 显示加载动画

            // 2. 调用 POST /api/ssh-config-import/parse-local
            const response = await fetch("/api/ssh-config-import/parse-local", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                }
            });

            if (!response.ok) {
                // 处理HTTP错误状态
                const errorData = await response.json().catch(() => null);

                // 404表示配置文件不存在
                if (response.status === 404) {
                    showError("未找到本地SSH配置文件（~/.ssh/config），请选择其他导入方式");
                    return;
                }

                const errorMessage = errorData?.message || `扫描失败：HTTP ${response.status}`;
                throw new Error(errorMessage);
            }

            // 3. 解析响应，填充servers列表
            const result: SSHConfigParseResult = await response.json();

            console.log("Local scan successful, parsed servers:", result);

            // 检查是否有错误信息
            if (result.errors && result.errors.length > 0) {
                console.error("Parse errors:", result.errors);
                showError(`解析失败：${result.errors.join("; ")}`);
                return;
            }

            // 验证响应数据
            if (!result.servers || result.servers.length === 0) {
                showError("配置文件中未找到有效的SSH Host配置");
                return;
            }

            // 初始化服务器列表（添加前端扩展字段）
            this.servers = result.servers.map(server => ({
                ...server,
                selected: server.valid && !server.duplicate,
                editing: false
            }));

            // 更新选中列表
            this.updateSelectedServers();

            // 显示警告信息（如果有）
            if (result.warnings && result.warnings.length > 0) {
                console.warn("Parse warnings:", result.warnings);
                result.warnings.forEach(warning => {
                    safeShowToast(warning, "warning");
                });
            }

            // 显示成功消息
            showSuccess(`本地扫描成功！解析到 ${result.totalHosts} 个Host，${this.servers.filter(s => s.valid && !s.duplicate).length} 个可导入`);

            // 4. 进入步骤2（预览）
            this.step = 2;
            this.updateStepDisplay();

        } catch (error) {
            console.error("Local scan failed:", error);
            const errorMessage = error instanceof Error ? error.message : "本地配置扫描失败，请重试";
            showError(errorMessage);
        }
    }

    /**
     * ===== Tab版本专用方法 =====
     * 开始自动扫描 (Tab版本)
     * 带4步进度动画的自动扫描功能
     */
    private async startAutoScan() {
        console.log("Starting auto-scan with progress animation...");

        const scanningStatus = this.modal?.querySelector<HTMLDivElement>("#scanningStatus");
        const scanResult = this.modal?.querySelector<HTMLDivElement>("#scanResult");
        const progressFill = this.modal?.querySelector<HTMLDivElement>("#progressFill");

        if (!scanningStatus || !scanResult) {
            console.error("Scan status elements not found");
            return;
        }

        try {
            // 显示扫描状态
            scanningStatus.classList.add("active");
            scanResult.classList.remove("active");

            // 定义4步进度
            const steps = [
                { id: "step1", progress: 25, delay: 500 },
                { id: "step2", progress: 50, delay: 1000 },
                { id: "step3", progress: 75, delay: 1500 },
                { id: "step4", progress: 100, delay: 2000 }
            ];

            // 调用后端API开始扫描
            const response = await fetch("/api/ssh-config-import/scan-local", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                }
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => null);
                if (response.status === 404) {
                    throw new Error("未找到本地SSH配置文件");
                }
                throw new Error(errorData?.message || `扫描失败：HTTP ${response.status}`);
            }

            const result: SSHConfigParseResult = await response.json();

            // 执行进度动画
            for (let i = 0; i < steps.length; i++) {
                await new Promise(resolve => setTimeout(resolve, steps[i].delay));

                const stepEl = this.modal?.querySelector<HTMLDivElement>(`#${steps[i].id}`);
                if (stepEl) {
                    stepEl.classList.add("active");
                    if (progressFill) {
                        progressFill.style.width = `${steps[i].progress}%`;
                    }
                }

                // 标记前一步完成
                if (i > 0) {
                    const prevStep = this.modal?.querySelector<HTMLDivElement>(`#${steps[i - 1].id}`);
                    if (prevStep) {
                        prevStep.classList.remove("active");
                        prevStep.classList.add("completed");
                        const icon = prevStep.querySelector(".scan-step-icon");
                        if (icon) icon.textContent = "✓";
                    }
                }
            }

            // 标记最后一步完成
            const lastStep = this.modal?.querySelector<HTMLDivElement>("#step4");
            if (lastStep) {
                lastStep.classList.remove("active");
                lastStep.classList.add("completed");
                const icon = lastStep.querySelector(".scan-step-icon");
                if (icon) icon.textContent = "✓";
            }

            // 延迟后显示结果
            await new Promise(resolve => setTimeout(resolve, 500));
            scanningStatus.classList.remove("active");
            scanResult.classList.add("active");

            // 处理扫描结果
            if (result.servers && result.servers.length > 0) {
                this.servers = result.servers.map(server => ({
                    ...server,
                    selected: server.valid && !server.duplicate,
                    editing: false
                }));
                this.updateSelectedServers();
                this.displayScanResult(result);
                showSuccess(`扫描完成！发现 ${result.totalHosts} 个配置，${this.servers.filter(s => s.valid && !s.duplicate).length} 个可导入`);
            } else {
                showError("未发现SSH配置文件");
            }

        } catch (error) {
            console.error("Auto-scan failed:", error);
            scanningStatus.classList.remove("active");
            showError(error instanceof Error ? error.message : "自动扫描失败");
        }
    }

    /**
     * 显示扫描结果 (Tab版本)
     */
    private displayScanResult(result: SSHConfigParseResult) {
        const resultInfo = this.modal?.querySelector<HTMLDivElement>("#scanResultInfo");
        const subtitle = this.modal?.querySelector<HTMLParagraphElement>("#scanResultSubtitle");
        const tipContent = this.modal?.querySelector<HTMLSpanElement>("#scanTipContent");

        if (!resultInfo) return;

        // 更新副标题
        if (subtitle) {
            const clientCount = new Set(result.servers.map(s => this.guessClientName(s.hostname || s.name))).size;
            subtitle.textContent = `检测到 ${clientCount} 个已安装的SSH客户端`;
        }

        // 填充结果信息
        const clientSummary = this.groupByClient(result.servers);
        resultInfo.innerHTML = clientSummary.map(item => `
            <div class="info-item">
                <div class="info-label">
                    ${this.getClientIcon(item.clientName)} ${item.clientName} ${item.version}
                    <span class="badge badge-success">已安装</span>
                </div>
                <div class="info-value">
                    ${item.sessionCount} 个会话 • ${item.configPath}
                </div>
            </div>
        `).join("");

        // 更新提示
        if (tipContent) {
            const totalSessions = result.servers.length;
            tipContent.textContent = `共发现 ${totalSessions} 个会话配置（来自${clientSummary.length}个客户端），系统将自动去重和合并。预计导入时间 3-5 分钟。`;
        }
    }

    /**
     * 根据hostname猜测客户端名称
     */
    private guessClientName(hostname: string): string {
        if (!hostname) return "Unknown";
        const lower = hostname.toLowerCase();
        if (lower.includes("securecrt")) return "SecureCRT";
        if (lower.includes("xshell")) return "Xshell";
        if (lower.includes("tabby")) return "Tabby";
        if (lower.includes("mobaxterm")) return "MobaXterm";
        if (lower.includes("putty")) return "PuTTY";
        return "SSH Config";
    }

    /**
     * 获取客户端图标
     */
    private getClientIcon(clientName: string): string {
        const icons: Record<string, string> = {
            "SecureCRT": "🔐",
            "Xshell": "📡",
            "Tabby": "⚡",
            "MobaXterm": "🖥️",
            "PuTTY": "🔧",
            "SSH Config": "💻",
            "Unknown": "💻"
        };
        return icons[clientName] || "💻";
    }

    /**
     * 按客户端分组
     */
    private groupByClient(servers: ServerImportPreview[]): {
        clientName: string;
        version: string;
        sessionCount: number;
        configPath: string;
    }[] {
        const grouped = new Map<string, {
            clientName: string;
            version: string;
            sessionCount: number;
            configPath: string;
        }>();

        servers.forEach(server => {
            const clientName = this.guessClientName(server.hostname || server.name);
            if (!grouped.has(clientName)) {
                grouped.set(clientName, {
                    clientName,
                    version: "未知版本",
                    sessionCount: 0,
                    configPath: server.hostname || "默认路径"
                });
            }
            grouped.get(clientName)!.sessionCount++;
        });

        return Array.from(grouped.values());
    }

    /**
     * 确认导入 (Tab版本)
     */
    private async confirmImport() {
        if (this.selectedServers.length === 0) {
            showError("请先选择要导入的服务器");
            return;
        }

        console.log(`Confirming import of ${this.selectedServers.length} servers...`);

        try {
            const response = await fetch("/api/ssh-config-import/confirm", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ servers: this.selectedServers })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => null);
                throw new Error(errorData?.message || `导入失败：HTTP ${response.status}`);
            }

            const result: ServerImportResult = await response.json();
            this.importResult = result;

            showSuccess(`导入成功！成功 ${result.successCount} 个，失败 ${result.failedCount} 个`);

            // 关闭模态框并触发事件
            this.close();
            eventBus.emit("server:imported", result);

        } catch (error) {
            console.error("Import failed:", error);
            showError(error instanceof Error ? error.message : "导入失败");
        }
    }

    /**
     * T6.2.3: 解析自定义路径配置
     * 调用后端API解析指定路径的SSH配置文件
     *
     * @param path 配置文件路径
     */
    public async parseCustomPath(path: string): Promise<void> {
        console.log("parseCustomPath called:", path);

        try {
            // 1. 验证路径不为空
            if (!path || path.trim() === "") {
                showError("请输入配置文件路径");
                return;
            }

            const trimmedPath = path.trim();
            this.customPath = trimmedPath;

            // 2. 显示加载状态
            console.log(`Parsing custom path: ${trimmedPath}`);
            // TODO: 显示加载动画

            // 3. 调用 POST /api/ssh-config-import/parse-local?path={path}
            const response = await fetch(`/api/ssh-config-import/parse-local?path=${encodeURIComponent(trimmedPath)}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                }
            });

            if (!response.ok) {
                // 处理HTTP错误状态
                const errorData = await response.json().catch(() => null);

                // 404表示配置文件不存在
                if (response.status === 404) {
                    showError(`未找到配置文件：${trimmedPath}`);
                    return;
                }

                const errorMessage = errorData?.message || `解析失败：HTTP ${response.status}`;
                throw new Error(errorMessage);
            }

            // 4. 解析响应，填充servers列表
            const result: SSHConfigParseResult = await response.json();

            console.log("Custom path parse successful, parsed servers:", result);

            // 检查是否有错误信息
            if (result.errors && result.errors.length > 0) {
                console.error("Parse errors:", result.errors);
                showError(`解析失败：${result.errors.join("; ")}`);
                return;
            }

            // 验证响应数据
            if (!result.servers || result.servers.length === 0) {
                showError("配置文件中未找到有效的SSH Host配置");
                return;
            }

            // 初始化服务器列表（添加前端扩展字段）
            this.servers = result.servers.map(server => ({
                ...server,
                selected: server.valid && !server.duplicate,
                editing: false
            }));

            // 更新选中列表
            this.updateSelectedServers();

            // 显示警告信息（如果有）
            if (result.warnings && result.warnings.length > 0) {
                console.warn("Parse warnings:", result.warnings);
                result.warnings.forEach(warning => {
                    safeShowToast(warning, "warning");
                });
            }

            // 显示成功消息
            showSuccess(`解析成功！解析到 ${result.totalHosts} 个Host，${this.servers.filter(s => s.valid && !s.duplicate).length} 个可导入`);

            // 5. 进入步骤2（预览）
            this.step = 2;
            this.updateStepDisplay();

        } catch (error) {
            console.error("Custom path parse failed:", error);
            const errorMessage = error instanceof Error ? error.message : "配置文件解析失败，请重试";
            showError(errorMessage);
        }
    }

    /**
     * T6.2.4: 导入选中的服务器
     * 收集勾选的服务器并调用批量导入API
     */
    public async importSelected(): Promise<void> {
        console.log("importSelected called");

        try {
            // 1. 收集selectedServers
            this.updateSelectedServers();

            // 2. 验证至少选中一台服务器
            if (this.selectedServers.length === 0) {
                showError("请至少选择一台服务器进行导入");
                return;
            }

            // 3. 调用validateServers()验证必填字段
            const validation = this.validateServers();
            if (!validation.valid) {
                showError(`服务器验证失败：\n${validation.errors.join("\n")}`);
                return;
            }

            // 4. 显示加载状态
            console.log(`Importing ${this.selectedServers.length} servers...`);
            // TODO: 显示加载动画

            // 准备请求体（移除前端扩展字段）
            const serversToImport = this.selectedServers.map(({ selected, editing, ...server }) => server);

            // 5. 调用 POST /api/ssh-config-import/batch
            const response = await fetch("/api/ssh-config-import/batch", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(serversToImport)
            });

            // 6. 解析响应，保存到importResult
            const result: ServerImportResult = await response.json();

            console.log("Import result:", result);

            // 保存导入结果
            this.importResult = result;

            // 根据导入结果显示不同消息
            if (response.status === 200) {
                // 200: 全部成功
                showSuccess(`导入成功！成功导入 ${result.successCount} 台服务器`);
            } else if (response.status === 207) {
                // 207: 部分成功
                safeShowToast(
                    `部分导入成功：成功 ${result.successCount} 台，失败 ${result.failedCount} 台`,
                    "warning"
                );
            } else if (response.status === 400) {
                // 400: 全部失败
                showError(`导入失败：${result.failedCount} 台服务器全部失败`);
            } else {
                // 其他错误状态
                throw new Error(`导入失败：HTTP ${response.status}`);
            }

            // 7. 进入步骤3（结果）
            this.step = 3;
            this.updateStepDisplay();

            // 触发服务器列表刷新事件
            eventBus.emit("servers:refresh");

        } catch (error) {
            console.error("Import failed:", error);
            const errorMessage = error instanceof Error ? error.message : "批量导入失败，请重试";
            showError(errorMessage);
        }
    }

    // ==================== 工具方法（待实现） ====================

    /**
     * T6.3.1: 验证选中服务器
     * 验证选中服务器的必填字段是否齐全
     *
     * @returns 验证结果 { valid: boolean, errors: string[] }
     */
    public validateServers(): { valid: boolean; errors: string[] } {
        console.log("validateServers called");

        const errors: string[] = [];

        // 1. 遍历selectedServers
        for (const server of this.selectedServers) {
            const serverErrors: string[] = [];

            // 2. 检查必填字段：name, hostname, sshUsername
            if (!server.name || server.name.trim() === "") {
                serverErrors.push("服务器名称");
            }
            if (!server.hostname || server.hostname.trim() === "") {
                serverErrors.push("主机名");
            }
            if (!server.sshUsername || server.sshUsername.trim() === "") {
                serverErrors.push("SSH用户名");
            }

            // 3. 检查认证凭证：sshPassword 或 sshKeyPath 至少有一个
            const hasPassword = server.sshPassword && server.sshPassword.trim() !== "";
            const hasKeyPath = server.sshKeyPath && server.sshKeyPath.trim() !== "";

            if (!hasPassword && !hasKeyPath) {
                serverErrors.push("SSH认证凭证（密码或私钥路径）");
            }

            // 4. 收集错误信息
            if (serverErrors.length > 0) {
                errors.push(`服务器 "${server.name || server.hostname || '未知'}" 缺少必填字段：${serverErrors.join(", ")}`);
            }
        }

        // 5. 返回验证结果
        const valid = errors.length === 0;
        return { valid, errors };
    }

    /**
     * T6.3.2: 全选
     * 选中所有服务器
     */
    public selectAll(): void {
        console.log("selectAll called");

        // 1. 遍历servers，设置selected=true
        this.servers.forEach(server => {
            server.selected = true;
        });

        // 2. 更新selectedServers列表
        this.updateSelectedServers();

        // 3. 更新UI（勾选框状态、统计信息）
        this.updateSelectionUI();

        console.log(`Selected all ${this.selectedServers.length} servers`);
    }

    /**
     * T6.3.2: 选择有效服务器
     * 仅选中有效且不重复的服务器
     */
    public selectValid(): void {
        console.log("selectValid called");

        // 1. 遍历servers，过滤valid=true且duplicate=false的服务器
        this.servers.forEach(server => {
            if (server.valid && !server.duplicate) {
                server.selected = true;
            } else {
                server.selected = false;
            }
        });

        // 2. 更新selectedServers列表
        this.updateSelectedServers();

        // 3. 更新UI（勾选框状态、统计信息）
        this.updateSelectionUI();

        console.log(`Selected ${this.selectedServers.length} valid servers`);
    }

    /**
     * T6.3.2: 取消全选
     * 取消选中所有服务器
     */
    public deselectAll(): void {
        console.log("deselectAll called");

        // 1. 遍历servers，设置selected=false
        this.servers.forEach(server => {
            server.selected = false;
        });

        // 2. 清空selectedServers列表
        this.updateSelectedServers();

        // 3. 更新UI（勾选框状态、统计信息）
        this.updateSelectionUI();

        console.log("Deselected all servers");
    }

    // ==================== 辅助方法 ====================

    /**
     * 更新selectedServers列表
     * 根据servers数组中selected=true的项更新selectedServers
     */
    private updateSelectedServers(): void {
        this.selectedServers = this.servers.filter(server => server.selected === true);
        console.log(`Updated selected servers: ${this.selectedServers.length}/${this.servers.length}`);
    }

    /**
     * 更新选择UI
     * 更新勾选框状态、统计信息等
     */
    private updateSelectionUI(): void {
        if (!this.modal) return;

        // 1. 更新全选复选框状态
        const selectAllCheckbox = document.getElementById("selectAllCheckbox") as HTMLInputElement;
        if (selectAllCheckbox) {
            const allSelected = this.servers.length > 0 && this.servers.every(s => s.selected);
            selectAllCheckbox.checked = allSelected;
        }

        // 2. 更新统计信息
        const selectedCount = document.getElementById("selectedServerCount");
        const totalCount = document.getElementById("totalServerCount");
        if (selectedCount) selectedCount.textContent = this.selectedServers.length.toString();
        if (totalCount) totalCount.textContent = this.servers.length.toString();

        // 3. 更新导入按钮禁用状态
        const nextBtn = document.getElementById("wizardNextBtn");
        if (nextBtn && this.step === 2) {
            nextBtn.disabled = this.selectedServers.length === 0;
        }

        console.log("Selection UI updated");
    }

    /**
     * 渲染预览表格
     */
    private renderPreviewTable(): void {
        if (!this.modal) return;

        const tbody = document.getElementById("serverPreviewTableBody");
        if (!tbody) {
            console.error("Table body not found");
            return;
        }

        // 清空现有内容
        tbody.innerHTML = "";

        // 如果没有数据，显示空状态
        if (this.servers.length === 0) {
            tbody.innerHTML = `
                <tr class="empty-state-row">
                    <td colspan="9" class="empty-state">
                        <div class="empty-icon">📋</div>
                        <p>暂无数据</p>
                        <p class="empty-hint">请先选择配置文件并解析</p>
                    </td>
                </tr>
            `;
            return;
        }

        // 渲染每一行
        this.servers.forEach((server, index) => {
            const row = this.createTableRow(server, index);
            tbody.appendChild(row);
        });

        // 绑定事件
        this.attachTableEvents();

        // 更新统计信息
        this.updateSelectionUI();
    }

    /**
     * 创建表格行
     */
    private createTableRow(server: ServerImportPreview, index: number): HTMLTableRowElement {
        const row = document.createElement("tr");
        row.dataset.index = index.toString();

        // 勾选框
        const checkboxCell = document.createElement("td");
        checkboxCell.className = "col-checkbox";
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.checked = server.selected || false;
        checkbox.dataset.index = index.toString();
        checkboxCell.appendChild(checkbox);
        row.appendChild(checkboxCell);

        // 服务器名称
        const nameCell = document.createElement("td");
        nameCell.innerHTML = `<input type="text" class="editable-field" value="${this.escapeHtml(server.name)}" data-field="name" data-index="${index}">`;
        row.appendChild(nameCell);

        // 主机名
        const hostnameCell = document.createElement("td");
        hostnameCell.innerHTML = `<input type="text" class="editable-field" value="${this.escapeHtml(server.hostname)}" data-field="hostname" data-index="${index}">`;
        row.appendChild(hostnameCell);

        // SSH端口
        const portCell = document.createElement("td");
        portCell.innerHTML = `<input type="number" class="editable-field" value="${server.sshPort}" data-field="sshPort" data-index="${index}" style="width: 80px;">`;
        row.appendChild(portCell);

        // 用户名
        const usernameCell = document.createElement("td");
        usernameCell.innerHTML = `<input type="text" class="editable-field" value="${this.escapeHtml(server.sshUsername)}" data-field="sshUsername" data-index="${index}">`;
        row.appendChild(usernameCell);

        // 认证方式
        const authCell = document.createElement("td");
        const hasPassword = server.sshPassword && server.sshPassword.trim() !== "";
        const hasKeyPath = server.sshKeyPath && server.sshKeyPath.trim() !== "";
        let authDisplay = "";
        if (hasPassword) authDisplay = "🔑 密码";
        else if (hasKeyPath) authDisplay = "🔐 私钥";
        else authDisplay = "⚠ 未设置";
        authCell.textContent = authDisplay;
        row.appendChild(authCell);

        // 服务器类型
        const typeCell = document.createElement("td");
        typeCell.innerHTML = `
            <select class="editable-select" data-field="serverType" data-index="${index}">
                <option value="DEVELOPMENT" ${server.serverType === "DEVELOPMENT" ? "selected" : ""}>开发</option>
                <option value="TESTING" ${server.serverType === "TESTING" ? "selected" : ""}>测试</option>
                <option value="STAGING" ${server.serverType === "STAGING" ? "selected" : ""}>预发布</option>
                <option value="PRODUCTION" ${server.serverType === "PRODUCTION" ? "selected" : ""}>生产</option>
            </select>
        `;
        row.appendChild(typeCell);

        // 状态
        const statusCell = document.createElement("td");
        const statusBadge = this.createStatusBadge(server);
        statusCell.appendChild(statusBadge);
        row.appendChild(statusCell);

        // 操作
        const actionsCell = document.createElement("td");
        actionsCell.className = "col-actions";
        actionsCell.innerHTML = `
            <div class="row-actions">
                <button class="action-btn action-btn-delete" data-action="delete" data-index="${index}" title="删除">
                    🗑️
                </button>
            </div>
        `;
        row.appendChild(actionsCell);

        return row;
    }

    /**
     * 创建状态徽章
     */
    private createStatusBadge(server: ServerImportPreview): HTMLSpanElement {
        const badge = document.createElement("span");
        badge.className = "status-badge";

        if (server.duplicate) {
            badge.classList.add("status-duplicate");
            badge.textContent = "⚠ 重复";
        } else if (!server.valid) {
            badge.classList.add("status-invalid");
            badge.textContent = "✗ 缺失字段";
        } else {
            badge.classList.add("status-ready");
            badge.textContent = "✓ 就绪";
        }

        return badge;
    }

    /**
     * 绑定表格事件
     */
    private attachTableEvents(): void {
        if (!this.modal) return;

        // 使用事件委托处理表格内的所有事件
        const table = document.getElementById("serverPreviewTable");
        if (!table) return;

        // 处理复选框变化
        table.addEventListener("change", (e) => {
            const target = e.target as HTMLElement;

            // 全选复选框
            if (target.id === "selectAllCheckbox") {
                const checked = (target as HTMLInputElement).checked;
                if (checked) {
                    this.selectAll();
                } else {
                    this.deselectAll();
                }
                this.renderPreviewTable();
                return;
            }

            // 单个复选框
            if (target.tagName === "INPUT" && target.getAttribute("type") === "checkbox") {
                const index = parseInt(target.dataset.index || "-1");
                if (index >= 0 && index < this.servers.length) {
                    this.servers[index].selected = (target as HTMLInputElement).checked;
                    this.updateSelectedServers();
                    this.updateSelectionUI();
                }
                return;
            }

            // 可编辑字段
            if (target.classList.contains("editable-field") || target.classList.contains("editable-select")) {
                const index = parseInt(target.dataset.index || "-1");
                const field = target.dataset.field;
                if (index >= 0 && index < this.servers.length && field) {
                    const value = (target as HTMLInputElement | HTMLSelectElement).value;
                    (this.servers[index] as any)[field] = field === "sshPort" ? parseInt(value) : value;
                }
            }
        });

        // 处理删除按钮
        table.addEventListener("click", (e) => {
            const target = e.target as HTMLElement;
            const deleteBtn = target.closest('[data-action="delete"]');
            if (deleteBtn) {
                const index = parseInt((deleteBtn as HTMLElement).dataset.index || "-1");
                if (index >= 0 && index < this.servers.length) {
                    this.servers.splice(index, 1);
                    this.updateSelectedServers();
                    this.renderPreviewTable();
                }
            }
        });

        // 批量操作按钮
        const selectAllBtn = document.getElementById("selectAllServersBtn");
        const selectValidBtn = document.getElementById("selectValidServersBtn");
        const deselectAllBtn = document.getElementById("deselectAllServersBtn");

        if (selectAllBtn) {
            selectAllBtn.onclick = () => {
                this.selectAll();
                this.renderPreviewTable();
            };
        }
        if (selectValidBtn) {
            selectValidBtn.onclick = () => {
                this.selectValid();
                this.renderPreviewTable();
            };
        }
        if (deselectAllBtn) {
            deselectAllBtn.onclick = () => {
                this.deselectAll();
                this.renderPreviewTable();
            };
        }
    }

    /**
     * HTML转义
     */
    private escapeHtml(text: string): string {
        const div = document.createElement("div");
        div.textContent = text;
        return div.innerHTML;
    }

    // ==================== 步骤3：结果展示 ====================

    /**
     * 渲染导入结果
     * 根据importResult渲染成功/失败详情
     */
    private renderImportResult(): void {
        if (!this.modal || !this.importResult) return;

        console.log("Rendering import result:", this.importResult);

        // 隐藏进度容器
        const progressContainer = document.getElementById("importProgressContainer");
        if (progressContainer) {
            progressContainer.style.display = "none";
        }

        // 显示结果容器
        const resultContainer = document.getElementById("importResultContainer");
        if (resultContainer) {
            resultContainer.style.display = "block";
        }

        // 渲染摘要卡片
        this.renderResultSummary();

        // 渲染失败详情（如果有）
        if (this.importResult.failedCount > 0) {
            this.renderFailureSection();
        } else {
            // 隐藏失败详情区域
            const failureSection = document.getElementById("failureSection");
            if (failureSection) failureSection.style.display = "none";
        }

        // 渲染成功列表（如果有）
        if (this.importResult.successCount > 0) {
            this.renderSuccessSection();
        } else {
            // 隐藏成功详情区域
            const successSection = document.getElementById("successSection");
            if (successSection) successSection.style.display = "none";
        }

        // 绑定操作按钮
        this.attachResultActions();
    }

    /**
     * 渲染结果摘要卡片
     */
    private renderResultSummary(): void {
        if (!this.importResult) return;

        const summaryCard = document.getElementById("resultSummaryCard");
        if (!summaryCard) return;

        const { successCount, failedCount } = this.importResult;
        const total = successCount + failedCount;

        // 判断状态：全部成功 / 部分成功 / 全部失败
        let status: "success" | "partial-success" | "failure";
        let iconClass: string;
        let iconText: string;
        let titleText: string;
        let summaryText: string;

        if (failedCount === 0) {
            // 全部成功
            status = "success";
            iconClass = "icon-success";
            iconText = "✓";
            titleText = "导入成功！";
            summaryText = `成功导入 <strong>${successCount}</strong> 台服务器`;
        } else if (successCount === 0) {
            // 全部失败
            status = "failure";
            iconClass = "icon-error";
            iconText = "✗";
            titleText = "导入失败";
            summaryText = `<strong>${failedCount}</strong> 台服务器导入失败`;
        } else {
            // 部分成功
            status = "partial-success";
            iconClass = "icon-warning";
            iconText = "⚠";
            titleText = "部分导入成功";
            summaryText = `成功 <strong>${successCount}</strong> 台，失败 <strong>${failedCount}</strong> 台`;
        }

        // 更新摘要卡片样式
        summaryCard.className = "result-summary-card";
        if (status !== "success") {
            summaryCard.classList.add(status);
        }

        // 更新图标
        const summaryIcon = document.getElementById("summaryIcon");
        if (summaryIcon) {
            summaryIcon.innerHTML = `<span class="${iconClass}">${iconText}</span>`;
        }

        // 更新标题
        const summaryTitle = document.getElementById("summaryTitle");
        if (summaryTitle) {
            summaryTitle.textContent = titleText;
        }

        // 更新摘要文本
        const summaryTextEl = document.getElementById("summaryText");
        if (summaryTextEl) {
            summaryTextEl.innerHTML = summaryText;
        }

        // 更新成功计数（如果存在）
        const successCountEl = document.getElementById("successCount");
        if (successCountEl) {
            successCountEl.textContent = successCount.toString();
        }
    }

    /**
     * 渲染失败详情表格
     */
    private renderFailureSection(): void {
        if (!this.importResult || this.importResult.failedCount === 0) return;

        const failureSection = document.getElementById("failureSection");
        const failureTableBody = document.getElementById("failureTableBody");

        if (!failureSection || !failureTableBody) return;

        // 显示失败详情区域
        failureSection.style.display = "block";

        // 清空现有内容
        failureTableBody.innerHTML = "";

        // 渲染每一行失败记录
        this.importResult.failures.forEach(failure => {
            const row = document.createElement("tr");

            // 服务器名称
            const nameCell = document.createElement("td");
            nameCell.textContent = failure.serverName;
            row.appendChild(nameCell);

            // 主机名
            const hostnameCell = document.createElement("td");
            hostnameCell.innerHTML = `<code>${this.escapeHtml(failure.hostname)}</code>`;
            row.appendChild(hostnameCell);

            // 失败原因
            const reasonCell = document.createElement("td");
            reasonCell.textContent = failure.reason;
            row.appendChild(reasonCell);

            failureTableBody.appendChild(row);
        });
    }

    /**
     * 渲染成功服务器列表
     */
    private renderSuccessSection(): void {
        if (!this.importResult || this.importResult.successCount === 0) return;

        const successSection = document.getElementById("successSection");
        const successGrid = document.getElementById("successServerGrid");

        if (!successSection || !successGrid) return;

        // 显示成功详情区域
        successSection.style.display = "block";

        // 清空现有内容
        successGrid.innerHTML = "";

        // 渲染每个成功服务器的卡片
        this.importResult.successServers.forEach(server => {
            const card = document.createElement("div");
            card.className = "success-server-card";

            card.innerHTML = `
                <h5>${this.escapeHtml(server.name)}</h5>
                <div class="server-detail">
                    <span class="detail-label">主机名:</span>
                    <span class="detail-value">${this.escapeHtml(server.hostname)}</span>
                </div>
                <div class="server-detail">
                    <span class="detail-label">ID:</span>
                    <span class="detail-value">#${server.id}</span>
                </div>
            `;

            successGrid.appendChild(card);
        });
    }

    /**
     * 绑定结果页面的操作按钮
     */
    private attachResultActions(): void {
        if (!this.modal) return;

        // 重新导入按钮
        const restartBtn = document.getElementById("restartImportBtn");
        if (restartBtn) {
            restartBtn.onclick = () => {
                console.log("Restart import clicked");
                this.reset();
            };
        }

        // 查看服务器列表按钮
        const viewServersBtn = document.getElementById("viewServersBtn");
        if (viewServersBtn) {
            viewServersBtn.onclick = () => {
                console.log("View servers clicked");
                this.close();
                // 触发服务器列表刷新
                eventBus.emit("servers:refresh");
                // 如果页面有导航功能，可以在这里跳转到服务器列表页面
                // window.location.href = "/admin/servers";
            };
        }

        // 关闭按钮
        const closeBtn = document.getElementById("closeWizardBtn");
        if (closeBtn) {
            closeBtn.onclick = () => {
                console.log("Close wizard clicked");
                this.close();
            };
        }
    }

    /**
     * 显示导入进度
     * @param text 进度文本
     * @param progress 进度百分比 (0-100)
     */
    private showImportProgress(text: string, progress: number): void {
        if (!this.modal) return;

        const progressContainer = document.getElementById("importProgressContainer");
        const progressText = document.getElementById("importProgressText");
        const progressFill = document.getElementById("importProgressFill");

        if (progressContainer) {
            progressContainer.style.display = "flex";
        }

        if (progressText) {
            progressText.textContent = text;
        }

        if (progressFill) {
            progressFill.style.width = `${progress}%`;
        }
    }

    /**
     * 语言切换处理
     */
    private handleLanguageChange() {
        console.log("Language changed, updating SSH Config Import Wizard UI");
        // TODO: 更新界面文本
    }

    /**
     * 格式化文件大小
     */
    private formatFileSize(bytes: number): string {
        if (bytes < 1024) return `${bytes} B`;
        if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
        return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    }

    /**
     * 清理资源
     */
    public cleanup() {
        if (this.unsubscribeLanguageChange) {
            this.unsubscribeLanguageChange();
            this.unsubscribeLanguageChange = null;
        }
    }
}

// ==================== 导出单例实例 ====================

let sshConfigImportWizardInstance: SSHConfigImportWizard | null = null;

export function getSSHConfigImportWizard(): SSHConfigImportWizard {
    if (!sshConfigImportWizardInstance) {
        console.log("Creating SSHConfigImportWizard instance...");
        sshConfigImportWizardInstance = new SSHConfigImportWizard();
    }
    return sshConfigImportWizardInstance;
}

// 全局函数供HTML调用
(window as any).openSSHConfigImportWizard = function() {
    console.log("Global openSSHConfigImportWizard called");
    const wizard = getSSHConfigImportWizard();
    wizard.open();
};

// 暴露wizard实例给全局,供HTML关闭按钮调用
(window as any).sshConfigWizard = {
    close: () => {
        console.log("Global sshConfigWizard.close() called");
        getSSHConfigImportWizard().close();
    }
};

// 页面加载时初始化
console.log("SSHConfigImportWizard.ts loaded");

export {};
