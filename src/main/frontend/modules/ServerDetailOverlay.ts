/**
 * ServerDetailOverlay - 服务器详情弹窗模块
 *
 * 职责：
 * - 显示服务器详细信息弹窗
 * - 实时监控数据展示（CPU、内存、磁盘、网络）
 * - 进程、应用、用户列表管理
 * - 图表可视化
 * - 导航和滚动交互
 */

import type {
    ServerDetail,
    ServerDetailDependencies,
    ServerDetailOverlayAPI
} from "../types/server-management";
import type { Chart } from "chart.js";

export class ServerDetailOverlay implements ServerDetailOverlayAPI {
    // 私有属性 - DOM元素
    private overlay: HTMLElement | null = null;
    private shell: HTMLElement | null = null;
    private body: HTMLElement | null = null;
    private navLinks: HTMLElement[] = [];
    private observer: IntersectionObserver | null = null;
    private toggleRefreshBtn: HTMLElement | null = null;
    private toggleChartsBtn: HTMLElement | null = null;
    private monitorGrid: HTMLElement | null = null;
    private toolbar: HTMLElement | null = null;
    private toolbarCompact: HTMLElement | null = null;
    private toolbarName: HTMLElement | null = null;
    private toolbarAddress: HTMLElement | null = null;
    private scrollHandler: (() => void) | null = null;

    // 私有属性 - 图表相关
    private charts: Array<Chart | null> = [];
    private chartTimer: number | null = null;
    private chartsPaused: boolean = false;
    private timeFormatter: Intl.DateTimeFormat | null = null;

    // 私有属性 - 数据和状态
    private currentServer: ServerDetail | null = null;
    private previousFocus: HTMLElement | null = null;
    private bodyOverflowBackup: string = "";
    private currentDetail: ServerDetail | null = null;
    private chartSeriesData: Array<{ time: string; cpu: number; memory: number }> = [];
    private activeSection: string = "";
    private processSort: string = "cpu";
    private processSortButtons: HTMLElement[] = [];
    private toolbarCompactActive: boolean = false;

    // 依赖
    private deps: ServerDetailDependencies;

    /**
     * 构造函数
     */
    constructor(deps: ServerDetailDependencies) {
        this.deps = deps;
    }

    /**
     * 初始化
     */
    init(): void {
        this.overlay = document.getElementById("serverDetailOverlay");
        if (!this.overlay) {
            console.warn("ServerDetailOverlay element not found");
            return;
        }

        this.shell = this.overlay.querySelector(".overlay-shell");
        this.body = this.overlay.querySelector(".overlay-body");
        this.toolbar = this.overlay.querySelector(".server-detail-toolbar");
        this.toolbarCompact = this.overlay.querySelector(".server-detail-toolbar-compact");
        this.toolbarName = this.toolbarCompact?.querySelector(".server-name") || null;
        this.toolbarAddress = this.toolbarCompact?.querySelector(".server-address") || null;
        this.toggleRefreshBtn = this.overlay.querySelector("#serverDetailToggleRefresh");
        this.toggleChartsBtn = this.overlay.querySelector("#serverDetailToggleCharts");
        this.monitorGrid = this.overlay.querySelector(".monitor-grid");

        // 设置关闭按钮事件
        const closeBtn = this.overlay.querySelector(".close-overlay");
        if (closeBtn) {
            closeBtn.addEventListener("click", () => this.close());
        }

        // 设置刷新按钮事件
        if (this.toggleRefreshBtn) {
            this.toggleRefreshBtn.addEventListener("click", () => {
                this.chartsPaused = !this.chartsPaused;
                this.updateRefreshButton();
                if (this.chartsPaused) {
                    this.pauseCharts();
                } else {
                    this.resumeCharts();
                }
            });
        }

        // 设置图表折叠按钮事件
        if (this.toggleChartsBtn) {
            this.toggleChartsBtn.addEventListener("click", () => {
                const collapsed = this.monitorGrid?.classList.toggle("collapsed") || false;
                this.updateChartsButton(collapsed);
            });
        }

        console.log("[ServerDetailOverlay] Initialized");
    }

    /**
     * 清理资源
     */
    teardown(): void {
        this.close();
        this.destroyNavigation();
        this.destroyCharts();

        // 清理事件监听器
        if (this.toggleRefreshBtn) {
            this.toggleRefreshBtn.replaceWith(this.toggleRefreshBtn.cloneNode(true));
        }
        if (this.toggleChartsBtn) {
            this.toggleChartsBtn.replaceWith(this.toggleChartsBtn.cloneNode(true));
        }

        console.log("[ServerDetailOverlay] Torn down");
    }

    /**
     * 查看服务器详情
     */
    async view(serverId: number): Promise<void> {
        try {
            // 获取服务器详情数据
            const detail = await this.fetchDetail(serverId);
            if (!detail) {
                this.deps.showError(
                    this.deps.getCurrentLanguage() === "en"
                        ? "Failed to load server details"
                        : "加载服务器详情失败"
                );
                return;
            }

            this.currentServer = detail;
            this.currentDetail = detail;

            // 更新内容
            this.updateContent(detail);

            // 打开overlay
            this.open();

            // 初始化导航和图表
            this.initNavigation();
            this.initCharts();
        } catch (error) {
            console.error("Failed to view server details:", error);
            this.deps.showError(
                this.deps.getCurrentLanguage() === "en"
                    ? "Failed to load server details"
                    : "加载服务器详情失败"
            );
        }
    }

    /**
     * 关闭弹窗
     */
    close(options: { silent?: boolean } = {}): void {
        if (!this.overlay) return;

        // 停止图表更新
        this.pauseCharts();
        this.destroyCharts();
        this.destroyNavigation();

        // 隐藏overlay
        this.overlay.classList.remove("active");
        this.overlay.setAttribute("aria-hidden", "true");

        // 恢复body滚动
        if (this.bodyOverflowBackup) {
            document.body.style.overflow = this.bodyOverflowBackup;
            this.bodyOverflowBackup = "";
        }

        // 恢复焦点
        if (this.previousFocus instanceof HTMLElement) {
            this.previousFocus.focus();
            this.previousFocus = null;
        }

        // 重置状态
        this.currentServer = null;
        this.currentDetail = null;
        this.chartSeriesData = [];
        this.activeSection = "";
        this.chartsPaused = false;
        this.toolbarCompactActive = false;

        if (!options.silent) {
            console.log("[ServerDetailOverlay] Closed");
        }
    }

    /**
     * 检查overlay是否打开
     */
    isOpen(): boolean {
        return this.overlay?.classList.contains("active") || false;
    }

    /**
     * 刷新数据
     */
    async refresh(isAuto: boolean = true): Promise<void> {
        if (!this.currentServer) return;

        try {
            const detail = await this.fetchDetail(this.currentServer.id);
            if (!detail) return;

            this.currentServer = detail;
            this.currentDetail = detail;

            // 更新内容（不改变滚动位置）
            this.updateContent(detail);

            if (!isAuto) {
                this.deps.showSuccess(
                    this.deps.getCurrentLanguage() === "en"
                        ? "Server details refreshed"
                        : "服务器详情已刷新"
                );
            }
        } catch (error) {
            console.error("Failed to refresh server details:", error);
            if (!isAuto) {
                this.deps.showError(
                    this.deps.getCurrentLanguage() === "en" ? "Failed to refresh" : "刷新失败"
                );
            }
        }
    }

    /**
     * 暂停图表更新
     */
    pauseCharts(): void {
        this.chartsPaused = true;
        if (this.chartTimer) {
            clearInterval(this.chartTimer);
            this.chartTimer = null;
        }
        this.updateRefreshButton();
    }

    /**
     * 恢复图表更新
     */
    resumeCharts(): void {
        this.chartsPaused = false;
        this.startCharts();
        this.updateRefreshButton();
    }

    /**
     * 滚动到指定section
     */
    scrollToSection(sectionId: string): void {
        if (!this.body) return;

        const targetSection = this.body.querySelector(`#${sectionId}`);
        if (targetSection) {
            targetSection.scrollIntoView({ behavior: "smooth", block: "start" });
        }
    }

    /**
     * 获取服务器详情数据（私有方法）
     */
    private async fetchDetail(serverId: number): Promise<ServerDetail | null> {
        try {
            const response = await fetch(
                `/admin/server-groups/api/${serverId}/detail?processSort=${this.processSort}`
            );
            if (!response.ok) throw new Error("Failed to fetch server detail");

            const data = await response.json();
            return data as ServerDetail;
        } catch (error) {
            console.error("Failed to fetch server detail:", error);
            return null;
        }
    }

    /**
     * 打开overlay（私有方法）
     */
    private open(): void {
        if (!this.overlay) return;

        // 保存当前焦点
        this.previousFocus = document.activeElement as HTMLElement;

        // 禁用body滚动
        this.bodyOverflowBackup = document.body.style.overflow;
        document.body.style.overflow = "hidden";

        // 显示overlay
        this.overlay.classList.add("active");
        this.overlay.setAttribute("aria-hidden", "false");

        // 聚焦到shell
        if (this.shell instanceof HTMLElement) {
            this.shell.focus();
        }

        console.log("[ServerDetailOverlay] Opened");
    }

    /**
     * 更新内容（私有方法）
     */
    private updateContent(detail: ServerDetail): void {
        if (!this.overlay) return;

        // 更新工具栏信息
        this.updateToolbarInfo(detail.name, `${detail.host}:${detail.port}`);

        // 更新基本信息
        this.updateBasicInfo(detail);

        // 更新监控指标
        this.updateMetrics(detail);

        // 更新进程列表
        this.renderProcesses(detail);

        // 更新应用列表
        this.renderApplications(detail);

        // 更新用户列表
        this.renderUsers(detail);

        // 存储图表数据
        if (detail.timestamps) {
            this.chartSeriesData = [
                { name: "CPU", data: detail.cpuHistory || [] },
                { name: "Memory", data: detail.memoryHistory || [] },
                { name: "Disk", data: detail.diskHistory || [] },
                { name: "Network In", data: detail.networkInHistory || [] },
                { name: "Network Out", data: detail.networkOutHistory || [] }
            ];
        }
    }

    /**
     * 更新工具栏信息（私有方法）
     */
    private updateToolbarInfo(name: string, address: string): void {
        if (this.toolbarName) {
            this.toolbarName.textContent = name;
        }
        if (this.toolbarAddress) {
            this.toolbarAddress.textContent = address;
        }
    }

    /**
     * 更新基本信息（私有方法）
     */
    private updateBasicInfo(detail: ServerDetail): void {
        // TODO: 实现基本信息更新逻辑
        // 更新服务器名称、地址、状态等
    }

    /**
     * 更新监控指标（私有方法）
     */
    private updateMetrics(detail: ServerDetail): void {
        // TODO: 实现监控指标更新逻辑
        // 更新CPU、内存、磁盘、网络等指标
    }

    /**
     * 渲染进程列表（私有方法）
     */
    private renderProcesses(detail: ServerDetail): void {
        const tbody = this.overlay?.querySelector("#server-detail-processes-tbody");
        if (!tbody) return;

        if (!detail.processes || detail.processes.length === 0) {
            this.renderEmptyState(
                tbody as HTMLElement,
                6,
                "暂无进程信息",
                "No process information available"
            );
            return;
        }

        // 排序进程
        const sorted = [...detail.processes].sort((a, b) => {
            if (this.processSort === "cpu") return b.cpuUsage - a.cpuUsage;
            if (this.processSort === "memory") return b.memoryUsage - a.memoryUsage;
            return 0;
        });

        const lang = this.deps.getCurrentLanguage();
        tbody.innerHTML = sorted
            .map(
                (proc) => `
            <tr>
                <td>${proc.pid}</td>
                <td class="process-name">${proc.name}</td>
                <td>${proc.user}</td>
                <td>${this.deps.formatPercentage(proc.cpuUsage)}</td>
                <td>${this.deps.formatPercentage(proc.memoryUsage)}</td>
                <td><span class="status-badge status-${proc.status.toLowerCase()}">${proc.status}</span></td>
            </tr>
        `
            )
            .join("");
    }

    /**
     * 渲染应用列表（私有方法）
     */
    private renderApplications(detail: ServerDetail): void {
        const tbody = this.overlay?.querySelector("#server-detail-applications-tbody");
        if (!tbody) return;

        if (!detail.applications || detail.applications.length === 0) {
            this.renderEmptyState(
                tbody as HTMLElement,
                5,
                "暂无应用信息",
                "No application information available"
            );
            return;
        }

        tbody.innerHTML = detail.applications
            .map(
                (app) => `
            <tr>
                <td>${app.name}</td>
                <td>${app.port}</td>
                <td><span class="status-badge status-${app.status}">${app.status}</span></td>
                <td>${app.uptime ? this.deps.formatUptime(app.uptime) : "--"}</td>
                <td>${app.memoryUsage ? this.deps.formatBytes(app.memoryUsage) : "--"}</td>
            </tr>
        `
            )
            .join("");
    }

    /**
     * 渲染用户列表（私有方法）
     */
    private renderUsers(detail: ServerDetail): void {
        const tbody = this.overlay?.querySelector("#server-detail-users-tbody");
        if (!tbody) return;

        if (!detail.users || detail.users.length === 0) {
            this.renderEmptyState(
                tbody as HTMLElement,
                4,
                "暂无用户信息",
                "No user information available"
            );
            return;
        }

        tbody.innerHTML = detail.users
            .map(
                (user) => `
            <tr>
                <td>${user.username}</td>
                <td>${user.terminal}</td>
                <td>${user.from}</td>
                <td>${user.loginTime}</td>
            </tr>
        `
            )
            .join("");
    }

    /**
     * 渲染空状态（私有方法）
     */
    private renderEmptyState(
        tbody: HTMLElement,
        colspan: number,
        messageZh: string,
        messageEn: string
    ): void {
        const lang = this.deps.getCurrentLanguage();
        const message = lang === "en" ? messageEn : messageZh;
        tbody.innerHTML = `<tr><td colspan="${colspan}" class="empty-state">${message}</td></tr>`;
    }

    /**
     * 初始化导航（私有方法）
     */
    private initNavigation(): void {
        // TODO: 实现导航初始化逻辑
        // IntersectionObserver + 导航高亮
    }

    /**
     * 销毁导航（私有方法）
     */
    private destroyNavigation(): void {
        if (this.observer) {
            this.observer.disconnect();
            this.observer = null;
        }
        this.navLinks = [];
    }

    /**
     * 初始化图表（私有方法）
     */
    private initCharts(): void {
        // TODO: 实现图表初始化逻辑
        // Chart.js实时图表
    }

    /**
     * 销毁图表（私有方法）
     */
    private destroyCharts(): void {
        this.charts.forEach((chart) => {
            if (chart && typeof chart.destroy === "function") {
                chart.destroy();
            }
        });
        this.charts = [];

        if (this.chartTimer) {
            clearInterval(this.chartTimer);
            this.chartTimer = null;
        }
    }

    /**
     * 启动图表自动刷新（私有方法）
     */
    private startCharts(): void {
        if (this.chartTimer) {
            clearInterval(this.chartTimer);
        }

        this.chartTimer = window.setInterval(() => {
            if (!this.chartsPaused) {
                this.refresh(true);
            }
        }, 5000); // 5秒刷新一次
    }

    /**
     * 更新刷新按钮状态（私有方法）
     */
    private updateRefreshButton(): void {
        if (!this.toggleRefreshBtn) return;

        const lang = this.deps.getCurrentLanguage();
        if (this.chartsPaused) {
            this.toggleRefreshBtn.textContent = lang === "en" ? "Resume" : "恢复";
            this.toggleRefreshBtn.classList.add("paused");
        } else {
            this.toggleRefreshBtn.textContent = lang === "en" ? "Pause" : "暂停";
            this.toggleRefreshBtn.classList.remove("paused");
        }
    }

    /**
     * 更新图表按钮状态（私有方法）
     */
    private updateChartsButton(collapsed: boolean): void {
        if (!this.toggleChartsBtn) return;

        const lang = this.deps.getCurrentLanguage();
        this.toggleChartsBtn.textContent = collapsed
            ? lang === "en"
                ? "Show Charts"
                : "显示图表"
            : lang === "en"
              ? "Hide Charts"
              : "隐藏图表";
    }
}
