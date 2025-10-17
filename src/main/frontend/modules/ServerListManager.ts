/**
 * ServerListManager - 服务器列表管理模块
 *
 * 职责：
 * - 加载和渲染服务器列表
 * - 视图模式切换（表格/卡片）
 * - 服务器过滤和搜索
 * - 批量操作（选择、刷新）
 * - 自动刷新
 */

import type {
    Server,
    LoadOptions,
    FilterCriteria,
    ViewMode,
    ServerListDependencies,
    ServerListManagerAPI
} from "../types/server-management";

export class ServerListManager implements ServerListManagerAPI {
    // 私有属性 - 数据
    private servers: Server[] = [];
    private filteredServers: Server[] = [];
    private selectedIds: Set<number> = new Set();

    // 私有属性 - 状态
    private viewMode: ViewMode = "table";
    private filterCriteria: FilterCriteria = { status: "all", search: "" };
    private autoRefreshTimer: number | null = null;
    private autoRefreshInterval: number = 30000; // 30秒

    // 依赖
    private deps: ServerListDependencies;

    /**
     * 构造函数
     */
    constructor(deps: ServerListDependencies) {
        this.deps = deps;
    }

    /**
     * 初始化
     */
    async init(): Promise<void> {
        await this.load();
    }

    /**
     * 清理资源
     */
    cleanup(): void {
        this.stopAutoRefresh();
        this.selectedIds.clear();
    }

    /**
     * 加载服务器列表
     */
    async load(options: LoadOptions = {}): Promise<void> {
        const { silent = false, applyFilter = false } = options;
        const tableBody = document.getElementById("serverTableBody");

        if (!tableBody) return;

        if (!silent) {
            const loadingText =
                this.deps.getCurrentLanguage() === "en"
                    ? "Loading servers..."
                    : "正在加载服务器...";
            tableBody.innerHTML = `<tr><td colspan="11" class="empty-state">${loadingText}</td></tr>`;
        }

        try {
            const response = await fetch("/admin/server-groups/api/list");
            if (!response.ok) throw new Error("Failed to load server list");

            const data = await response.json();
            // 后端直接返回数组，不是 { servers: [...] } 格式
            this.servers = Array.isArray(data) ? data : data.servers || [];

            if (applyFilter) {
                this.applyFilter();
            } else {
                this.filteredServers = [...this.servers];
            }

            this.render();

            // 加载完成后调用图表更新
            if (this.deps.loadCharts) {
                this.deps.loadCharts();
            }
        } catch (error) {
            console.error("Failed to load server list:", error);
            const errorText =
                this.deps.getCurrentLanguage() === "en"
                    ? "Failed to load server list"
                    : "加载服务器列表失败";
            tableBody.innerHTML = `<tr><td colspan="11" class="empty-state">${errorText}</td></tr>`;
        }
    }

    /**
     * 刷新列表
     */
    async refresh(): Promise<void> {
        await this.load({ silent: true, applyFilter: true });
    }

    /**
     * 刷新单个服务器
     */
    async refreshServer(serverId: number): Promise<void> {
        try {
            const response = await fetch(`/admin/server-groups/api/batch-refresh`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ serverIds: [serverId] })
            });

            if (response.ok) {
                this.deps.showSuccess("服务器刷新成功");
                await this.load();
            }
        } catch (error) {
            console.error("Failed to refresh server:", error);
            this.deps.showError("服务器刷新失败");
        }
    }

    /**
     * 切换视图模式
     */
    switchView(mode: ViewMode): void {
        if (this.viewMode === mode) return;
        this.viewMode = mode;
        this.render();
    }

    /**
     * 过滤服务器
     */
    filter(criteria: FilterCriteria): void {
        this.filterCriteria = { ...this.filterCriteria, ...criteria };
        this.applyFilter();
        this.render();
    }

    /**
     * 全选
     */
    selectAll(): void {
        this.selectedIds.clear();
        this.filteredServers.forEach((server) => {
            this.selectedIds.add(server.id);
        });
        this.updateCheckboxes();
    }

    /**
     * 取消全选
     */
    deselectAll(): void {
        this.selectedIds.clear();
        this.updateCheckboxes();
    }

    /**
     * 批量刷新
     */
    async batchRefresh(): Promise<void> {
        const serverIds = Array.from(this.selectedIds);

        if (serverIds.length === 0) {
            this.deps.showError(
                this.deps.getCurrentLanguage() === "en"
                    ? "Please select servers first"
                    : "请先选择要刷新的服务器"
            );
            return;
        }

        try {
            const response = await fetch("/admin/server-groups/api/batch-refresh", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ serverIds })
            });

            if (response.ok) {
                const message =
                    this.deps.getCurrentLanguage() === "en"
                        ? `Refreshed ${serverIds.length} servers`
                        : `已刷新 ${serverIds.length} 台服务器`;
                this.deps.showSuccess(message);
                await this.load({ silent: true, applyFilter: true });
            }
        } catch (error) {
            console.error("Batch refresh failed:", error);
            this.deps.showError(
                this.deps.getCurrentLanguage() === "en" ? "Batch refresh failed" : "批量刷新失败"
            );
        }
    }

    /**
     * 获取选中的服务器ID
     */
    getSelected(): number[] {
        return Array.from(this.selectedIds);
    }

    /**
     * 启动自动刷新
     */
    startAutoRefresh(interval?: number): void {
        if (interval) {
            this.autoRefreshInterval = interval;
        }

        this.stopAutoRefresh();
        this.autoRefreshTimer = window.setInterval(() => {
            this.refresh();
        }, this.autoRefreshInterval);
    }

    /**
     * 停止自动刷新
     */
    stopAutoRefresh(): void {
        if (this.autoRefreshTimer) {
            clearInterval(this.autoRefreshTimer);
            this.autoRefreshTimer = null;
        }
    }

    /**
     * 应用过滤条件（私有方法）
     */
    private applyFilter(): void {
        let filtered = [...this.servers];

        // 状态过滤
        if (this.filterCriteria.status && this.filterCriteria.status !== "all") {
            filtered = filtered.filter((server) => server.status === this.filterCriteria.status);
        }

        // 搜索过滤
        if (this.filterCriteria.search) {
            const search = this.filterCriteria.search.toLowerCase();
            filtered = filtered.filter(
                (server) =>
                    server.name.toLowerCase().includes(search) ||
                    server.host.toLowerCase().includes(search)
            );
        }

        this.filteredServers = filtered;
    }

    /**
     * 渲染列表（私有方法）
     */
    private render(): void {
        const tableBody = document.getElementById("serverTableBody");
        if (!tableBody) return;

        if (this.filteredServers.length === 0) {
            const emptyText =
                this.deps.getCurrentLanguage() === "en" ? "No servers found" : "未找到服务器";
            tableBody.innerHTML = `<tr><td colspan="11" class="empty-state">${emptyText}</td></tr>`;
            return;
        }

        // 渲染服务器行
        tableBody.innerHTML = this.filteredServers
            .map((server) => this.renderServerRow(server))
            .join("");

        // 附加复选框事件
        this.attachCheckboxEvents();
    }

    /**
     * 渲染单个服务器行（私有方法）
     */
    private renderServerRow(server: Server): string {
        const isChecked = this.selectedIds.has(server.id);
        const lang = this.deps.getCurrentLanguage();

        // 状态显示
        const statusMap: Record<string, { text: string; textEn: string; class: string }> = {
            online: { text: "在线", textEn: "Online", class: "status-online" },
            offline: { text: "离线", textEn: "Offline", class: "status-offline" },
            warning: { text: "警告", textEn: "Warning", class: "status-warning" },
            unknown: { text: "未知", textEn: "Unknown", class: "status-unknown" }
        };

        const statusInfo = statusMap[server.status] || statusMap.unknown;
        const statusText = lang === "en" ? statusInfo.textEn : statusInfo.text;

        // 操作按钮文本
        const viewDetailsText = lang === "en" ? "Details" : "详情";
        const connectText = lang === "en" ? "SSH" : "SSH";
        const refreshText = lang === "en" ? "Refresh" : "刷新";
        const viewDetailsTitle = lang === "en" ? "View Details" : "查看详情";
        const connectTitle = lang === "en" ? "SSH Connect" : "SSH连接";
        const refreshTitle = lang === "en" ? "Refresh" : "刷新";

        return `
            <tr data-server-id="${server.id}">
                <td>
                    <input type="checkbox"
                           class="server-checkbox"
                           data-server-id="${server.id}"
                           ${isChecked ? "checked" : ""}>
                </td>
                <td>${server.name || "--"}</td>
                <td><span class="status-badge ${statusInfo.class}">${statusText}</span></td>
                <td>${server.healthScore != null ? server.healthScore : "--"}</td>
                <td>${server.cpuUsage != null ? this.deps.formatPercentage(server.cpuUsage) : "--"}</td>
                <td>${server.memoryUsage != null ? this.deps.formatPercentage(server.memoryUsage) : "--"}</td>
                <td>${server.diskUsage != null ? this.deps.formatPercentage(server.diskUsage) : "--"}</td>
                <td>${server.apps != null ? server.apps : "--"}</td>
                <td>${server.users != null ? server.users : "--"}</td>
                <td>${server.uptime || "--"}</td>
                <td class="actions">
                    <button class="btn-text btn-primary" onclick="window.serverListManager.viewDetails(${server.id})" title="${viewDetailsTitle}">${viewDetailsText}</button>
                    <button class="btn-text btn-info" onclick="window.serverListManager.connect(${server.id})" title="${connectTitle}">${connectText}</button>
                    <button class="btn-text btn-secondary" onclick="window.serverListManager.refreshServer(${server.id})" title="${refreshTitle}">${refreshText}</button>
                </td>
            </tr>
        `;
    }

    /**
     * 附加复选框事件（私有方法）
     */
    private attachCheckboxEvents(): void {
        const checkboxes = document.querySelectorAll<HTMLInputElement>(".server-checkbox");
        checkboxes.forEach((checkbox) => {
            checkbox.addEventListener("change", (e) => {
                const target = e.target as HTMLInputElement;
                const serverId = parseInt(target.dataset.serverId || "0");

                if (target.checked) {
                    this.selectedIds.add(serverId);
                } else {
                    this.selectedIds.delete(serverId);
                }

                this.updateSelectAllCheckbox();
            });
        });
    }

    /**
     * 更新复选框状态（私有方法）
     */
    private updateCheckboxes(): void {
        const checkboxes = document.querySelectorAll<HTMLInputElement>(".server-checkbox");
        checkboxes.forEach((checkbox) => {
            const serverId = parseInt(checkbox.dataset.serverId || "0");
            checkbox.checked = this.selectedIds.has(serverId);
        });

        this.updateSelectAllCheckbox();
    }

    /**
     * 更新全选复选框状态（私有方法）
     */
    private updateSelectAllCheckbox(): void {
        const selectAllCheckbox = document.getElementById("selectAllServers") as HTMLInputElement;
        if (!selectAllCheckbox) return;

        const visibleServerIds = this.filteredServers.map((s) => s.id);
        const allSelected =
            visibleServerIds.length > 0 && visibleServerIds.every((id) => this.selectedIds.has(id));

        selectAllCheckbox.checked = allSelected;
    }

    /**
     * 查看服务器详情（公共方法，供window调用）
     */
    viewDetails(serverId: number): void {
        if (this.deps.viewServerDetails) {
            this.deps.viewServerDetails(serverId);
        }
    }

    /**
     * SSH连接（公共方法，供window调用）
     */
    connect(serverId: number): void {
        if (this.deps.connectToServer) {
            this.deps.connectToServer(serverId);
        }
    }
}
