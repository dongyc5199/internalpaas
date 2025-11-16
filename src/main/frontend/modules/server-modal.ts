interface CsrfToken {
    token: string;
    header: string;
}

interface ServerCreateResponse {
    serverId?: string;
    success?: boolean;
    message?: string;
}

interface ServerPayload {
    name: string;
    type: string;
    description: string | null;
    hostname: string;
    port: number;
    username: string;
    password: string;
    tags: string[];
    enableMonitoring: boolean;
    autoRestart: boolean;
    timeout: number;
    retryCount: number;
    envVars: string | null;
    autoInitGroups: boolean;
    enableMetrics: boolean;
}

type ToastType = "success" | "error" | "warning" | "info";

class ServerModal {
    private overlay: HTMLDivElement;
    private container: HTMLDivElement;
    private isOpen = false;
    private tags: string[] = [];

    constructor() {
        this.injectModal();
        this.overlay = this.requireElement<HTMLDivElement>("serverModalOverlay");
        this.container = this.requireElement<HTMLDivElement>("serverModalContainer");
        this.bindEvents();
    }

    private getCsrfToken(): CsrfToken {
        const tokenMeta = document.querySelector<HTMLMetaElement>('meta[name="_csrf"]');
        const headerMeta = document.querySelector<HTMLMetaElement>('meta[name="_csrf_header"]');

        return {
            token: tokenMeta?.content ?? "",
            header: headerMeta?.content ?? "X-CSRF-TOKEN"
        };
    }

    private injectModal(): void {
        if (document.getElementById("serverModalOverlay")) {
            return;
        }

        const modalHTML = `
            <div class="server-modal-overlay" id="serverModalOverlay">
                <div class="server-modal-container" id="serverModalContainer">
                    <div class="server-modal-header">
                        <h2 class="server-modal-title">
                            <span aria-hidden="true">🖥️</span>
                            添加新服务器
                        </h2>
                        <button class="server-modal-close" id="serverModalClose" aria-label="关闭">×</button>
                    </div>
                    <div class="server-modal-body">
                        <form id="serverAddForm">
                            <div class="server-modal-section">
                                <h3 class="server-modal-section-title">
                                    <i class="fas fa-info-circle"></i>
                                    基本信息
                                </h3>
                                <div class="server-modal-form-row">
                                    <div class="server-modal-form-group">
                                        <label class="server-modal-form-label">
                                            服务器名称 <span class="server-modal-required">*</span>
                                        </label>
                                        <input type="text" class="server-modal-form-control" name="name" id="serverName" placeholder="例: 生产服务器-01" required>
                                    </div>
                                    <div class="server-modal-form-group">
                                        <label class="server-modal-form-label">
                                            服务器类型 <span class="server-modal-required">*</span>
                                        </label>
                                        <select class="server-modal-form-control" name="type" id="serverType" required>
                                            <option value="">选择类型</option>
                                            <option value="production">生产环境</option>
                                            <option value="staging">预发布环境</option>
                                            <option value="development">开发环境</option>
                                            <option value="testing">测试环境</option>
                                        </select>
                                    </div>
                                    <div class="server-modal-form-group">
                                        <label class="server-modal-form-label">操作系统</label>
                                        <select class="server-modal-form-control" name="osType" id="serverOsType">
                                            <option value="LINUX" selected>Linux</option>
                                            <option value="WINDOWS">Windows</option>
                                            <option value="MACOS">macOS</option>
                                            <option value="UNIX">Unix</option>
                                            <option value="BSD">BSD</option>
                                            <option value="OTHER">其他</option>
                                        </select>
                                    </div>
                                </div>
                                <div class="server-modal-form-group">
                                    <label class="server-modal-form-label">描述</label>
                                    <textarea class="server-modal-form-control" name="description" id="serverDescription" rows="3" placeholder="输入服务器描述信息..."></textarea>
                                </div>
                            </div>
                            <div class="server-modal-section">
                                <h3 class="server-modal-section-title">
                                    <i class="fas fa-network-wired"></i>
                                    SSH连接配置
                                </h3>
                                <div class="server-modal-form-row">
                                    <div class="server-modal-form-group">
                                        <label class="server-modal-form-label">
                                            主机地址 <span class="server-modal-required">*</span>
                                        </label>
                                        <input type="text" class="server-modal-form-control" name="hostname" id="serverHostname" placeholder="例: 192.168.1.100 或 server.example.com" required>
                                    </div>
                                    <div class="server-modal-form-group">
                                        <label class="server-modal-form-label">
                                            SSH端口 <span class="server-modal-required">*</span>
                                        </label>
                                        <input type="number" class="server-modal-form-control" name="port" id="serverPort" value="22" min="1" max="65535" required>
                                    </div>
                                </div>
                                <div class="server-modal-form-row">
                                    <div class="server-modal-form-group">
                                        <label class="server-modal-form-label">
                                            SSH用户名 <span class="server-modal-required">*</span>
                                        </label>
                                        <input type="text" class="server-modal-form-control" name="username" id="serverUsername" placeholder="例: root" required>
                                    </div>
                                    <div class="server-modal-form-group">
                                        <label class="server-modal-form-label">
                                            SSH密码 <span class="server-modal-required">*</span>
                                        </label>
                                        <div class="server-modal-password-wrapper">
                                            <input type="password" class="server-modal-form-control" name="password" id="serverPassword" placeholder="输入SSH密码" required>
                                            <button type="button" class="server-modal-password-toggle" id="passwordToggle" aria-label="显示/隐藏密码">
                                                <i class="fas fa-eye"></i>
                                            </button>
                                        </div>
                                    </div>
                                </div>
                                <div class="server-modal-form-group">
                                    <button type="button" class="server-modal-test-btn" id="testConnectionBtn">
                                        <i class="fas fa-plug"></i>
                                        测试连接
                                    </button>
                                </div>
                            </div>
                            <div class="server-modal-section">
                                <h3 class="server-modal-section-title">
                                    <i class="fas fa-users"></i>
                                    用户组与监控
                                </h3>
                                <label class="server-modal-checkbox-tile">
                                    <input type="checkbox" name="autoInitGroups" id="autoInitGroups" checked>
                                    <div class="server-modal-checkbox-tile-content">
                                        <div class="server-modal-checkbox-tile-label">自动初始化默认用户组</div>
                                        <div class="server-modal-checkbox-tile-hint">创建管理员、开发者、访客等基础权限组，后续可在用户管理中调整。</div>
                                    </div>
                                </label>
                            </div>
                            <div class="server-modal-section">
                                <button type="button" class="server-modal-advanced-toggle" id="advancedToggle" aria-expanded="false">
                                    <i class="fas fa-cog"></i>
                                    高级选项
                                    <i class="fas fa-chevron-down server-modal-toggle-icon"></i>
                                </button>
                                <div class="server-modal-advanced-content" id="advancedContent">
                                    <div class="server-modal-form-group">
                                        <label class="server-modal-form-label">最大并发会话</label>
                                        <input type="number" class="server-modal-form-control" name="maxSessions" id="maxSessions" value="5" min="1" max="50">
                                        <small class="server-modal-hint">限制同一时间占用该服务器的工作空间数量。</small>
                                    </div>
                                    <div class="server-modal-form-group">
                                        <label class="server-modal-form-label">服务器标签</label>
                                        <div class="server-modal-tags-container" id="tagsContainer">
                                            <input type="text" class="server-modal-tags-input" id="tagsInput" placeholder="输入后回车添加，如 production">
                                        </div>
                                    </div>
                                    <label class="server-modal-checkbox-tile">
                                        <input type="checkbox" name="enableMetrics" id="enableMetrics" checked>
                                        <div class="server-modal-checkbox-tile-content">
                                            <div class="server-modal-checkbox-tile-label">启用实时指标采集</div>
                                            <div class="server-modal-checkbox-tile-hint">同步 CPU / 内存 / GPU 使用率，为容量规划提供依据。</div>
                                        </div>
                                    </label>
                                    <label class="server-modal-checkbox-tile">
                                        <input type="checkbox" name="enableMonitoring" id="enableMonitoring" checked>
                                        <div class="server-modal-checkbox-tile-content">
                                            <div class="server-modal-checkbox-tile-label">开启健康检查</div>
                                            <div class="server-modal-checkbox-tile-hint">定期调用健康检查接口，异常时触发告警。</div>
                                        </div>
                                    </label>
                                    <label class="server-modal-checkbox-tile">
                                        <input type="checkbox" name="autoRestart" id="autoRestart">
                                        <div class="server-modal-checkbox-tile-content">
                                            <div class="server-modal-checkbox-tile-label">启用自动重启</div>
                                            <div class="server-modal-checkbox-tile-hint">当探测失败达到阈值时尝试自动重启实例。</div>
                                        </div>
                                    </label>
                                    <div class="server-modal-form-group">
                                        <label class="server-modal-form-label">健康检查超时时间（秒）</label>
                                        <input type="number" class="server-modal-form-control" name="timeout" id="timeout" value="30" min="5" max="300">
                                        <small class="server-modal-hint">超过该时间即认为任务失败，触发重试与告警。</small>
                                    </div>
                                    <div class="server-modal-form-group">
                                        <label class="server-modal-form-label">最大重试次数</label>
                                        <input type="number" class="server-modal-form-control" name="retryCount" id="retryCount" value="3" min="0" max="10">
                                    </div>
                                    <div class="server-modal-form-group">
                                        <label class="server-modal-form-label">环境变量（JSON）</label>
                                        <textarea class="server-modal-form-control" name="envVars" id="envVars" rows="3" placeholder='例: {"ENV":"prod"}'></textarea>
                                        <small class="server-modal-hint">用于渲染部署模板，可在运行时注入私密凭据。</small>
                                    </div>
                                </div>
                            </div>
                        </form>
                    </div>
                    <div class="server-modal-footer">
                        <button type="button" class="server-modal-btn server-modal-btn-cancel" id="cancelBtn">
                            <i class="fas fa-times"></i>
                            取消
                        </button>
                        <button type="submit" class="server-modal-btn server-modal-btn-primary" id="submitBtn" form="serverAddForm">
                            <i class="fas fa-check"></i>
                            添加服务器
                        </button>
                    </div>
                </div>
            </div>
            <div class="server-modal-toast-container" id="toastContainer"></div>
        `;

        const modalElement = document.createElement("div");
        modalElement.innerHTML = modalHTML;
        document.body.appendChild(modalElement);
    }

    private bindEvents(): void {
        this.requireElement<HTMLButtonElement>("serverModalClose").addEventListener("click", () =>
            this.close()
        );
        this.requireElement<HTMLButtonElement>("cancelBtn").addEventListener("click", () =>
            this.close()
        );
        this.overlay.addEventListener("click", (event) => {
            if (event.target === this.overlay) {
                this.close();
            }
        });
        document.addEventListener("keydown", this.handleEscape);
        this.requireElement<HTMLButtonElement>("passwordToggle").addEventListener(
            "click",
            this.togglePassword
        );
        this.requireElement<HTMLInputElement>("tagsInput").addEventListener(
            "keydown",
            this.handleTagInput
        );
        this.requireElement<HTMLButtonElement>("advancedToggle").addEventListener(
            "click",
            this.toggleAdvanced
        );
        this.requireElement<HTMLButtonElement>("testConnectionBtn").addEventListener(
            "click",
            this.testConnection
        );
        this.requireElement<HTMLFormElement>("serverAddForm").addEventListener(
            "submit",
            this.handleSubmit
        );
    }

    private requireElement<T extends HTMLElement>(id: string): T {
        const element = document.getElementById(id);
        if (!element) {
            throw new Error(`[ServerModal] 未找到元素: ${id}`);
        }
        return element as T;
    }

    private handleEscape = (event: KeyboardEvent): void => {
        if (event.key === "Escape" && this.isOpen) {
            this.close();
        }
    };

    private togglePassword = (event: MouseEvent): void => {
        const button = event.currentTarget as HTMLButtonElement;
        const passwordInput = this.requireElement<HTMLInputElement>("serverPassword");
        const icon = button.querySelector("i");

        if (passwordInput.type === "password") {
            passwordInput.type = "text";
            icon?.classList.remove("fa-eye");
            icon?.classList.add("fa-eye-slash");
        } else {
            passwordInput.type = "password";
            icon?.classList.remove("fa-eye-slash");
            icon?.classList.add("fa-eye");
        }
    };

    private handleTagInput = (event: KeyboardEvent): void => {
        const input = event.currentTarget as HTMLInputElement;
        if (event.key === "Enter" && input.value.trim()) {
            event.preventDefault();
            const tagText = input.value.trim();
            if (!this.tags.includes(tagText)) {
                this.tags.push(tagText);
                const tagElement = this.createTagElement(tagText);
                input.before(tagElement);
                input.value = "";
            }
        } else if (event.key === "Backspace" && !input.value && this.tags.length > 0) {
            event.preventDefault();
            this.removeTag(this.tags.length - 1);
        }
    };

    private toggleAdvanced = (event: MouseEvent): void => {
        const toggle = event.currentTarget as HTMLButtonElement;
        const content = this.requireElement<HTMLDivElement>("advancedContent");
        const icon = toggle.querySelector(".server-modal-toggle-icon") as HTMLElement | null;
        const expanded = toggle.getAttribute("aria-expanded") === "true";

        if (expanded) {
            content.classList.remove("show");
            content.style.maxHeight = "0";
            toggle.setAttribute("aria-expanded", "false");
            if (icon) {
                icon.style.transform = "rotate(0deg)";
            }
        } else {
            content.classList.add("show");
            requestAnimationFrame(() => {
                content.style.maxHeight = `${content.scrollHeight}px`;
            });
            toggle.setAttribute("aria-expanded", "true");
            if (icon) {
                icon.style.transform = "rotate(180deg)";
            }
        }
    };

    private testConnection = async (): Promise<void> => {
        const hostname = this.requireElement<HTMLInputElement>("serverHostname").value.trim();
        const port = this.requireElement<HTMLInputElement>("serverPort").value;
        const username = this.requireElement<HTMLInputElement>("serverUsername").value.trim();
        const password = this.requireElement<HTMLInputElement>("serverPassword").value;
        const button = this.requireElement<HTMLButtonElement>("testConnectionBtn");

        if (!hostname || !port || !username || !password) {
            this.showToast("请填写完整的SSH连接信息", "warning");
            return;
        }

        button.disabled = true;
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 测试中...';

        try {
            const csrf = this.getCsrfToken();
            const headers: Record<string, string> = {
                "Content-Type": "application/json"
            };
            if (csrf.token) {
                headers[csrf.header] = csrf.token;
            }

            const response = await fetch("/admin/servers/test-connection", {
                method: "POST",
                headers,
                body: JSON.stringify({
                    hostname,
                    port: Number.parseInt(port, 10),
                    username,
                    password
                })
            });

            const result = (await response.json()) as ServerCreateResponse;
            if (response.ok && result.success) {
                this.showToast("连接测试成功！", "success");
            } else {
                this.showToast(result.message ?? "连接测试失败", "error");
            }
        } catch (error) {
            console.error("[ServerModal] Connection test error:", error);
            const message = error instanceof Error ? error.message : "未知错误";
            this.showToast(`连接测试失败: ${message}`, "error");
        } finally {
            button.disabled = false;
            button.innerHTML = '<i class="fas fa-plug"></i> 测试连接';
        }
    };

    private handleSubmit = async (event: SubmitEvent): Promise<void> => {
        event.preventDefault();

        const form = event.currentTarget as HTMLFormElement;
        const formData = new FormData(form);
        const payload: ServerPayload = {
            name: (formData.get("name") as string) ?? "",
            type: (formData.get("type") as string) ?? "",
            description: (formData.get("description") as string) ?? null,
            hostname: (formData.get("hostname") as string) ?? "",
            port: Number.parseInt((formData.get("port") as string) ?? "0", 10),
            username: (formData.get("username") as string) ?? "",
            password: (formData.get("password") as string) ?? "",
            tags: [...this.tags],
            enableMonitoring: formData.get("enableMonitoring") === "on",
            autoRestart: formData.get("autoRestart") === "on",
            timeout: Number.parseInt((formData.get("timeout") as string) ?? "30", 10) || 30,
            retryCount: Number.parseInt((formData.get("retryCount") as string) ?? "3", 10) || 3,
            envVars: (formData.get("envVars") as string) ?? null,
            autoInitGroups: formData.get("autoInitGroups") === "on",
            enableMetrics: formData.get("enableMetrics") === "on"
        };

        const submitButton = this.requireElement<HTMLButtonElement>("submitBtn");
        submitButton.disabled = true;
        submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 添加中...';

        try {
            const csrf = this.getCsrfToken();
            const headers: Record<string, string> = {
                "Content-Type": "application/json"
            };
            if (csrf.token) {
                headers[csrf.header] = csrf.token;
            }

            const response = await fetch("/admin/servers/api/create", {
                method: "POST",
                headers,
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                const result = (await response.json()) as ServerCreateResponse;
                this.showToast("服务器添加成功！正在收集监控数据...", "success");
                setTimeout(() => {
                    this.close();
                    if (typeof window.refreshServerList === "function") {
                        void window.refreshServerList();
                    }
                    if (result.serverId && typeof window.startServerMetricsPolling === "function") {
                        window.startServerMetricsPolling(result.serverId);
                    }
                }, 800);
            } else {
                const errorBody = (await response.json()) as ServerCreateResponse;
                this.showToast(errorBody.message ?? "添加服务器失败", "error");
                submitButton.disabled = false;
                submitButton.innerHTML = '<i class="fas fa-check"></i> 添加服务器';
            }
        } catch (error) {
            console.error("[ServerModal] Submit error:", error);
            const message = error instanceof Error ? error.message : "未知错误";
            this.showToast(`添加服务器失败: ${message}`, "error");
            submitButton.disabled = false;
            submitButton.innerHTML = '<i class="fas fa-check"></i> 添加服务器';
        }
    };

    private createTagElement(text: string): HTMLSpanElement {
        const tag = document.createElement("span");
        tag.className = "server-modal-tag";
        tag.innerHTML = `
            ${text}
            <button type="button" class="server-modal-tag-remove" aria-label="移除标签">
                <i class="fas fa-times"></i>
            </button>
        `;

        const removeButton = tag.querySelector<HTMLButtonElement>(".server-modal-tag-remove");
        removeButton?.addEventListener("click", () => {
            const index = this.tags.indexOf(text);
            this.removeTag(index);
        });

        return tag;
    }

    private removeTag(index: number): void {
        if (index < 0 || index >= this.tags.length) {
            return;
        }
        this.tags.splice(index, 1);
        const container = this.requireElement<HTMLDivElement>("tagsContainer");
        const tags = container.querySelectorAll<HTMLSpanElement>(".server-modal-tag");
        const target = tags[index];
        target?.remove();
    }

    private resetForm(): void {
        this.requireElement<HTMLFormElement>("serverAddForm").reset();
        this.tags = [];
        document.querySelectorAll(".server-modal-tag").forEach((tag) => tag.remove());

        const content = this.requireElement<HTMLDivElement>("advancedContent");
        const toggle = this.requireElement<HTMLButtonElement>("advancedToggle");
        const icon = toggle.querySelector<HTMLElement>(".server-modal-toggle-icon");
        content.classList.remove("show");
        content.style.maxHeight = "0";
        toggle.setAttribute("aria-expanded", "false");
        if (icon) {
            icon.style.transform = "rotate(0deg)";
        }

        const submitButton = this.requireElement<HTMLButtonElement>("submitBtn");
        submitButton.disabled = false;
        submitButton.innerHTML = '<i class="fas fa-check"></i> 添加服务器';
    }

    private showToast(message: string, type: ToastType = "info"): void {
        const container = this.requireElement<HTMLDivElement>("toastContainer");
        const toast = document.createElement("div");
        toast.className = `server-modal-toast ${type} show`;

        const iconMap: Record<ToastType, string> = {
            success: "fa-check-circle",
            error: "fa-exclamation-circle",
            warning: "fa-exclamation-triangle",
            info: "fa-info-circle"
        };

        toast.innerHTML = `
            <i class="fas ${iconMap[type]}"></i>
            <span>${message}</span>
        `;

        container.appendChild(toast);

        setTimeout(() => {
            toast.classList.remove("show");
            setTimeout(() => {
                // 添加DOM检查防止removeChild错误
                if (toast.parentNode && document.body.contains(toast)) {
                    try {
                        toast.remove();
                    } catch (err) {
                        console.warn('[ServerModal] Failed to remove toast:', err);
                    }
                }
            }, 300);
        }, 3000);
    }

    open(): void {
        this.overlay.classList.add("show");
        this.isOpen = true;
        document.body.style.overflow = "hidden";

        window.setTimeout(() => {
            this.requireElement<HTMLInputElement>("serverName").focus();
        }, 300);
    }

    close(): void {
        this.overlay.classList.remove("show");
        this.isOpen = false;
        document.body.style.overflow = "";

        window.setTimeout(() => {
            this.resetForm();
        }, 300);
    }
}

declare global {
    interface Window {
        serverModal?: ServerModal;
        openServerModal?: () => void;
        refreshServerList?: () => Promise<void>;
        startServerMetricsPolling?: (serverId: string) => void;
    }
}

function initServerModal(): ServerModal {
    if (!window.serverModal) {
        window.serverModal = new ServerModal();
    }
    return window.serverModal;
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
        initServerModal();
        console.info("[ServerModal] 已完成初始化");
    });
} else {
    initServerModal();
    console.info("[ServerModal] 已完成初始化");
}

window.openServerModal = () => {
    const modal = window.serverModal ?? initServerModal();
    modal.open();
    console.info("[ServerModal] openServerModal 已触发");
};

export { initServerModal, ServerModal };
