import Chart from "chart.js/auto";
import {
    formatBytes,
    formatUptime,
    formatPercentage,
    formatTimestamp,
    formatNumber,
    getUsageClass,
    setLocalizedText as utilsSetLocalizedText,
    updateMetricSummaryText as utilsUpdateMetricSummaryText,
    safeShowToast,
    showSuccess,
    showError,
    getChartColor,
    generateChartColors,
    destroyChart,
    destroyCharts,
    updateChartData,
    createTimeSeriesChartConfig,
    createBarChartConfig
} from "@/utils";
import { eventBus } from "@/utils/event-bus";
import { ServerListManager } from "./ServerListManager";
import { ServerDetailOverlay } from "./ServerDetailOverlay";
import { getServerImportModal } from "./server-import-modal";

// 声明全局变量和函数，带默认值
declare let currentLanguage: string;
declare function t(key: string, category?: string): string;
declare function applyLanguage(language: string, persist?: boolean, emitEvent?: boolean): void;
declare function showSuccessMessage(message: string): void;
declare function showErrorMessage(message: string): void;
declare function showWarningMessage(message: string): void;

// 确保currentLanguage有默认值
if (typeof currentLanguage === "undefined") {
    (window as any).currentLanguage = "zh";
}

// 安全的国际化函数包装器
function safeT(key: string, category?: string): string {
    if (typeof t === "function") {
        try {
            return t(key, category);
        } catch (error) {
            console.warn(`Translation function error for key: ${key}`, error);
        }
    }

    // 如果t函数不可用，返回默认文本
    const defaultTexts: Record<string, Record<string, string>> = {
        healthScore: { zh: "健康度", en: "Health Score" },
        time: { zh: "时间", en: "Time" },
        usagePercent: { zh: "使用率(%)", en: "Usage (%)" },
        applicationCount: { zh: "应用数量", en: "Application Count" }
    };

    const lang = typeof currentLanguage !== "undefined" ? currentLanguage : "zh";
    return defaultTexts[key]?.[lang] || key;
}

// 安全的消息显示函数包装器
function safeShowMessage(message: string, type: "success" | "error" | "warning" = "success"): void {
    try {
        if (type === "success") {
            if (typeof showSuccessMessage === "function") {
                showSuccessMessage(message);
            } else {
                showSuccess(message);
            }
        } else if (type === "error") {
            if (typeof showErrorMessage === "function") {
                showErrorMessage(message);
            } else {
                showError(message);
            }
        } else if (type === "warning") {
            if (typeof showWarningMessage === "function") {
                showWarningMessage(message);
            } else {
                // 警告消息降级为普通toast
                safeShowToast(message, "warning");
            }
        }
    } catch (error) {
        console.warn(`Failed to show message: ${message}`, error);
        // 最后的后备方案
        safeShowToast(message, type);
    }
}

declare global {
    interface Window {
        showToast?: (message: string, type?: string) => void;
        ServerGroupModule?: {
            init: () => void;
            cleanup: () => void;
            onLanguageChange: () => void;
            viewServerDetails: (serverId: number | string) => void;
            refreshServer: (serverId: number | string) => void;
            isDetailOpen: () => boolean;
        };
    }
}

(() => {
    let unsubscribeLanguageChange: (() => void) | null = null;

    // Server Group Management Functions
    let serverGroupManagementInitialized = false;
    let serverGroupAutoRefreshTimer: ReturnType<typeof setInterval> | null = null;

    // ServerListManager实例
    let serverListManager: ServerListManager | null = null;

    // ServerDetailOverlay实例
    let serverDetailOverlayManager: ServerDetailOverlay | null = null;

    // safeShowToast 已从 @/utils 导入，不再需要本地实现

    // setLocalizedText保留，因为某些地方可能还在使用
    function setLocalizedText(element: HTMLElement | null, zh: string, en: string) {
        if (!element) {
            return;
        }
        const zhValue = zh || "";
        const enValue = en || "";
        element.setAttribute("data-i18n-zh", zhValue);
        element.setAttribute("data-i18n-en", enValue);
        const lang = getCurrentLanguage();
        element.textContent =
            lang === "en" ? enValue || zhValue || "--" : zhValue || enValue || "--";
    }

    function initServerGroupManagement() {
        if (!unsubscribeLanguageChange) {
            unsubscribeLanguageChange = eventBus.on("language:changed", () =>
                handleLanguageChange()
            );
        }
        if (!document.querySelector(".server-group-management")) {
            return;
        }

        // 防止重复初始化
        if (serverGroupManagementInitialized) {
            console.log("Server group management already initialized, skipping...");
            return;
        }

        console.log("Initializing server group management...");

        initServerGroupData();
        initServerGroupEvents();
        startServerGroupAutoRefresh();

        serverGroupManagementInitialized = true;
        console.log("Server group management initialized");
    }

    function cleanupServerGroupManagement() {
        if (unsubscribeLanguageChange) {
            unsubscribeLanguageChange();
            unsubscribeLanguageChange = null;
        }

        // 清理ServerListManager
        if (serverListManager) {
            serverListManager.cleanup();
            serverListManager = null;
            delete (window as any).serverListManager;
        }

        // 清理ServerDetailOverlay
        if (serverDetailOverlayManager) {
            serverDetailOverlayManager.teardown();
            serverDetailOverlayManager = null;
            delete (window as any).serverDetailOverlay;
        }

        stopServerGroupAutoRefresh();
        serverGroupManagementInitialized = false;
        console.log("Server group management cleaned up");
    }

    function initServerGroupData() {
        console.log("Loading server group data...");

        // 创建ServerListManager实例
        if (!serverListManager) {
            serverListManager = new ServerListManager({
                formatBytes,
                formatUptime,
                formatPercentage,
                showSuccess,
                showError,
                viewServerDetails: (id) => viewServerDetails(id),
                connectToServer: (id) => connectToServer(id),
                loadCharts: () => {
                    loadHealthTrendChart();
                    loadLoadDistributionChart();
                    loadAppDistributionChart();
                },
                t: (key, namespace) => safeT(key, namespace),
                getCurrentLanguage: () => getCurrentLanguage()
            });

            // 将实例暴露到window供HTML onclick调用
            (window as any).serverListManager = serverListManager;
        }

        // 初始化ServerListManager
        serverListManager.init();

        // 创建ServerDetailOverlay实例
        if (!serverDetailOverlayManager) {
            serverDetailOverlayManager = new ServerDetailOverlay({
                formatBytes,
                formatUptime,
                formatPercentage,
                formatDateTime: (date: string | Date) => formatTimestamp(String(date)),
                showSuccess,
                showError,
                Chart,
                connectToServer: (id) => connectToServer(id),
                terminateProcess: async (serverId, pid) => {
                    // TODO: 实现终止进程逻辑
                    console.log(`Terminate process ${pid} on server ${serverId}`);
                },
                t: (key, namespace) => safeT(key, namespace),
                getCurrentLanguage: () => getCurrentLanguage()
            });

            // 将实例暴露到window供HTML onclick调用
            (window as any).serverDetailOverlay = serverDetailOverlayManager;
        }

        // 初始化ServerDetailOverlay
        serverDetailOverlayManager.init();

        loadHealthTrendChart();
        loadLoadDistributionChart();
        loadAppDistributionChart();
        updateServerGroupLastUpdateTime();
    }

    function initServerGroupEvents() {
        const refreshBtn = document.getElementById("refreshServerGroupBtn");
        if (refreshBtn) {
            refreshBtn.addEventListener("click", () => {
                refreshAllServerGroupData();
            });
        }

        const exportBtn = document.getElementById("exportServerGroupBtn");
        if (exportBtn) {
            exportBtn.addEventListener("click", () => {
                exportServerGroupReport();
            });
        }

        const addServerBtn = document.getElementById("addServerBtn");
        if (addServerBtn) {
            addServerBtn.addEventListener("click", () => {
                addNewServer();
            });
        }

        const importServerBtn = document.getElementById("importServerBtn");
        if (importServerBtn) {
            importServerBtn.addEventListener("click", () => {
                openServerImportModal();
            });
        }

        const healthTrendRange = document.getElementById("healthTrendRange");
        if (healthTrendRange) {
            healthTrendRange.addEventListener("change", (e) => {
                const target = e.target as HTMLSelectElement;
                loadHealthTrendChart(parseInt(target.value));
            });
        }

        const searchInput = document.getElementById("serverSearchInput");
        if (searchInput) {
            let searchTimeout: ReturnType<typeof setTimeout> | null = null;
            searchInput.addEventListener("input", (e) => {
                if (searchTimeout) {
                    clearTimeout(searchTimeout);
                }
                searchTimeout = setTimeout(() => {
                    const search = (e.target as HTMLInputElement).value;
                    serverListManager?.filter({ search });
                }, 300);
            });
        }

        const statusFilter = document.getElementById("serverStatusFilter");
        if (statusFilter) {
            statusFilter.addEventListener("change", (e) => {
                const status = (e.target as HTMLSelectElement).value as
                    | "all"
                    | "online"
                    | "offline"
                    | "warning";
                serverListManager?.filter({ status });
            });
        }

        const healthFilter = document.getElementById("serverHealthFilter");
        if (healthFilter) {
            healthFilter.addEventListener("change", (e) => {
                const status = (e.target as HTMLSelectElement).value as
                    | "all"
                    | "online"
                    | "offline"
                    | "warning";
                serverListManager?.filter({ status });
            });
        }

        const tableViewBtn = document.getElementById("serverTableViewBtn");
        if (tableViewBtn) {
            tableViewBtn.addEventListener("click", () => {
                serverListManager?.switchView("table");
            });
        }

        const cardViewBtn = document.getElementById("serverCardViewBtn");
        if (cardViewBtn) {
            cardViewBtn.addEventListener("click", () => {
                serverListManager?.switchView("card");
            });
        }

        const selectAllCheckbox = document.getElementById("selectAllServers");
        if (selectAllCheckbox) {
            selectAllCheckbox.addEventListener("change", (e) => {
                const checked = (e.target as HTMLInputElement).checked;
                if (checked) {
                    serverListManager?.selectAll();
                } else {
                    serverListManager?.deselectAll();
                }
            });
        }

        const batchRefreshBtn = document.getElementById("batchRefreshBtn");
        if (batchRefreshBtn) {
            batchRefreshBtn.addEventListener("click", () => {
                serverListManager?.batchRefresh();
            });
        }

        const clearSelectionBtn = document.getElementById("clearSelectionBtn");
        if (clearSelectionBtn) {
            clearSelectionBtn.addEventListener("click", () => {
                serverListManager?.deselectAll();
            });
        }

        // Listen for language change events and reload charts
    }

    function handleLanguageChange() {
        if (!document.querySelector(".server-group-management")) {
            return;
        }
        const healthTrendRange = document.getElementById(
            "healthTrendRange"
        ) as HTMLSelectElement | null;
        const hours = healthTrendRange ? parseInt(healthTrendRange.value, 10) : 24;
        loadHealthTrendChart(hours);
        loadLoadDistributionChart();
        loadAppDistributionChart();
        serverListManager?.refresh();
        updateServerGroupLastUpdateTime();
        // ServerDetailOverlay会自动处理语言切换
    }

    const serverGroupListCache = [];
    let healthTrendChart: Chart | null = null;
    let loadDistributionChart: Chart | null = null;
    let appDistributionChart: Chart | null = null;
    let healthTrendLoading = false;

    async function loadHealthTrendChart(hours = 24) {
        const container = document.getElementById("healthTrendChartContainer");
        const placeholder = document.getElementById("healthTrendPlaceholder");
        const canvas = document.getElementById("healthTrendChart");

        if (!container || !canvas) return;

        // 防止并发调用
        if (healthTrendLoading) {
            console.log("Health trend chart is already loading, skipping...");
            return;
        }

        healthTrendLoading = true;

        try {
            console.log(`Loading health trend chart (${hours} hours)...`);
            const response = await fetch(`/admin/server-groups/api/health-trend?hours=${hours}`);
            if (!response.ok) throw new Error("Failed to load health trend");

            const data = await response.json();

            if (placeholder) placeholder.classList.add("hidden");
            container.classList.add("active");

            // 使用 utils/chart.ts 的 destroyChart 销毁旧图表
            destroyChart(healthTrendChart);

            const ctx = (canvas as HTMLCanvasElement).getContext("2d");
            if (!ctx) return;

            // 使用 utils/chart.ts 的 createTimeSeriesChartConfig
            const datasets = (data.servers || []).map((server: any, index: number) => ({
                label: server.serverName,
                data: server.healthScores,
                borderColor: getChartColor(index),
                backgroundColor: getChartColor(index, 0.1),
                fill: true
            }));

            const chartConfig: any = createTimeSeriesChartConfig(data.timePoints || [], datasets, {
                plugins: {
                    legend: {
                        display: true,
                        position: "bottom"
                    },
                    tooltip: {
                        mode: "index",
                        intersect: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100,
                        title: {
                            display: true,
                            text: safeT("healthScore", "chart")
                        }
                    },
                    x: {
                        title: {
                            display: true,
                            text: safeT("time", "chart")
                        }
                    }
                }
            });

            healthTrendChart = new Chart(ctx, chartConfig);
        } catch (error) {
            console.error("Failed to load health trend chart:", error);
            if (placeholder) {
                placeholder.textContent =
                    currentLanguage === "en" ? "Failed to load chart data" : "加载图表数据失败";
                placeholder.classList.remove("hidden");
            }
        } finally {
            healthTrendLoading = false;
        }
    }

    let loadDistributionLoading = false;

    async function loadLoadDistributionChart() {
        const container = document.getElementById("loadDistributionChartContainer");
        const placeholder = document.getElementById("loadDistributionPlaceholder");
        const canvas = document.getElementById("loadDistributionChart");

        if (!container || !canvas) return;

        // 防止并发调用
        if (loadDistributionLoading) {
            console.log("Load distribution chart is already loading, skipping...");
            return;
        }

        loadDistributionLoading = true;

        try {
            console.log("Loading load distribution chart...");
            const response = await fetch("/admin/server-groups/api/load-distribution");
            if (!response.ok) throw new Error("Failed to load distribution");

            const data = await response.json();

            updateLoadStatistics(data.statistics);

            if (placeholder) placeholder.classList.add("hidden");
            container.classList.add("active");

            // 使用 utils/chart.ts 的 destroyChart 销毁旧图表
            destroyChart(loadDistributionChart);

            const ctx = (canvas as HTMLCanvasElement).getContext("2d");
            if (!ctx) return;

            // 使用 utils/chart.ts 的 createBarChartConfig
            const datasets = (data.metrics || []).map((metric: any, index: number) => ({
                label: safeT(metric.label, "chart"), // 使用国际化函数翻译标签
                data: metric.servers.map((s: any) => s.value),
                backgroundColor: getChartColor(index, 0.8),
                borderColor: getChartColor(index)
            }));

            const labels = ((data.metrics && data.metrics[0]?.servers) || []).map((_: any, i: number) =>
                currentLanguage === "en" ? `Server ${i + 1}` : `服务器 ${i + 1}`
            );

            const chartConfig: any = createBarChartConfig(labels, datasets, {
                plugins: {
                    legend: {
                        display: true,
                        position: "bottom"
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100,
                        title: {
                            display: true,
                            text: safeT("usagePercent", "chart")
                        }
                    }
                }
            });

            loadDistributionChart = new Chart(ctx, chartConfig);
        } catch (error) {
            console.error("Failed to load load distribution chart:", error);
            if (placeholder) {
                placeholder.textContent =
                    currentLanguage === "en" ? "Failed to load chart data" : "加载图表数据失败";
                placeholder.classList.remove("hidden");
            }
        } finally {
            loadDistributionLoading = false;
        }
    }

    let appDistributionLoading = false;

    async function loadAppDistributionChart() {
        const container = document.getElementById("appDistributionChartContainer");
        const placeholder = document.getElementById("appDistributionPlaceholder");
        const canvas = document.getElementById("appDistributionChart");

        if (!container || !canvas) return;

        // 防止并发调用
        if (appDistributionLoading) {
            console.log("App distribution chart is already loading, skipping...");
            return;
        }

        appDistributionLoading = true;

        try {
            console.log("Loading app distribution chart...");
            const response = await fetch("/admin/server-groups/api/app-distribution");
            if (!response.ok) throw new Error("Failed to load app distribution");

            const data = await response.json();

            if (placeholder) placeholder.classList.add("hidden");
            container.classList.add("active");

            // 使用 utils/chart.ts 的 destroyChart 销毁旧图表
            destroyChart(appDistributionChart);

            const ctx = (canvas as HTMLCanvasElement).getContext("2d");
            if (!ctx) return;

            // 使用 utils/chart.ts 的 createBarChartConfig
            const datasets = (data.appTypes || []).map((appType: any) => ({
                label: safeT(appType.label, "chart"), // 使用国际化函数翻译标签
                data: appType.data,
                backgroundColor: appType.color || "#6366f1",
                borderColor: "#ffffff"
            }));

            const chartConfig: any = createBarChartConfig(data.servers || [], datasets, {
                plugins: {
                    legend: {
                        display: true,
                        position: "bottom"
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: safeT("applicationCount", "chart")
                        },
                        ticks: {
                            stepSize: 1
                        }
                    }
                }
            });

            appDistributionChart = new Chart(ctx, chartConfig);
        } catch (error) {
            console.error("Failed to load app distribution chart:", error);
            if (placeholder) {
                placeholder.textContent =
                    currentLanguage === "en" ? "Failed to load chart data" : "加载图表数据失败";
                placeholder.classList.remove("hidden");
            }
        } finally {
            appDistributionLoading = false;
        }
    }

    // loadServerGroupList 已被 ServerListManager.load() 替代
    // renderServerGroupList 已被 ServerListManager.render() 替代

    // 注意：这个函数返回CSS类名，与utils.getUsageClass不同
    function getUsageFillClass(value: number | undefined) {
        if (!value) return "usage-fill--low";
        if (value < 50) return "usage-fill--low";
        if (value < 70) return "usage-fill--medium";
        if (value < 85) return "usage-fill--high";
        return "usage-fill--critical";
    }

    // getChartColor 已从 @/utils 导入，不再需要本地实现

    // 辅助函数：更新DOM元素内容
    function updateElement(id: string, value: string | number) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = String(value);
        }
    }

    function updateLoadStatistics(stats: any) {
        if (!stats) return;
        updateElement("loadExcellent", stats.excellent || 0);
        updateElement("loadGood", stats.good || 0);
        updateElement("loadWarning", stats.warning || 0);
        updateElement("loadCritical", stats.critical || 0);
    }

    // filterServerList 已被 ServerListManager.filter() 替代
    // switchServerView 已被 ServerListManager.switchView() 替代
    // attachServerCheckboxEvents 已内置在 ServerListManager 中
    // toggleSelectAllServers 已被 ServerListManager.selectAll() 替代

    function updateBatchActionBar() {
        const checkboxes = document.querySelectorAll(".server-checkbox:checked");
        const count = checkboxes.length;
        const batchBar = document.getElementById("batchActionBar");
        const countElement = document.getElementById("selectedCount");

        if (batchBar) {
            batchBar.style.display = count > 0 ? "flex" : "none";
        }
        if (countElement) {
            countElement.textContent = String(count);
        }

        const selectAll = document.getElementById("selectAllServers") as HTMLInputElement | null;
        if (selectAll) {
            const allCheckboxes = document.querySelectorAll(".server-checkbox");
            selectAll.checked = count > 0 && count === allCheckboxes.length;
        }
    }

    // batchRefreshServers 已被 ServerListManager.batchRefresh() 替代
    // clearServerSelection 已被 ServerListManager.deselectAll() 替代

    async function refreshAllServerGroupData() {
        updateServerGroupLastUpdateTime();
        await Promise.all([
            loadHealthTrendChart(),
            loadLoadDistributionChart(),
            loadAppDistributionChart(),
            serverListManager?.load({ applyFilter: true })
        ]);
        safeShowMessage("Data refreshed successfully", "success");
    }

    // 自动刷新图表功能
    function startServerGroupAutoRefresh() {
        // 清除已存在的定时器
        stopServerGroupAutoRefresh();

        console.log("Starting server group auto-refresh (10s interval)...");

        serverGroupAutoRefreshTimer = setInterval(async () => {
            // 检查是否还在服务器群组管理页面
            if (!document.querySelector(".server-group-management")) {
                console.log("Not on server group page, stopping auto-refresh");
                stopServerGroupAutoRefresh();
                return;
            }

            console.log("Auto-refreshing server group data...");

            const healthTrendRange = document.getElementById("healthTrendRange") as HTMLSelectElement | null;
            const hours = healthTrendRange ? parseInt(healthTrendRange.value) : 24;

            const tasks = [
                {
                    name: "Server List",
                    promise:
                        serverListManager?.load({ silent: true, applyFilter: true }) ||
                        Promise.resolve()
                },
                { name: "Health Trend", promise: loadHealthTrendChart(hours) },
                { name: "Load Distribution", promise: loadLoadDistributionChart() },
                { name: "App Distribution", promise: loadAppDistributionChart() }
            ];

            const results = await Promise.allSettled(tasks.map((task) => task.promise));

            // 记录刷新结果
            const successCount = results.filter((r) => r.status === "fulfilled").length;
            const failedCount = results.filter((r) => r.status === "rejected").length;

            console.log(`Auto-refresh completed: ${successCount} succeeded, ${failedCount} failed`);

            if (failedCount > 0) {
                results.forEach((result, index) => {
                    if (result.status === "rejected") {
                        console.error(`Failed to refresh ${tasks[index].name}:`, result.reason);
                    }
                });
            }

            // 更新最后刷新时间
            updateServerGroupLastUpdateTime();
        }, 10000); // 10秒间隔
    }

    function stopServerGroupAutoRefresh() {
        if (serverGroupAutoRefreshTimer) {
            console.log("Stopping server group auto-refresh...");
            clearInterval(serverGroupAutoRefreshTimer);
            serverGroupAutoRefreshTimer = null;
        }
    }

    function updateServerGroupLastUpdateTime() {
        const timeElement = document.getElementById("serverGroupUpdateTime");
        if (timeElement) {
            const now = new Date();
            const timeString = now.toLocaleTimeString(
                currentLanguage === "en" ? "en-US" : "zh-CN",
                {
                    hour: "2-digit",
                    minute: "2-digit",
                    second: "2-digit"
                }
            );
            const label = currentLanguage === "en" ? "Last updated" : "最后更新";
            timeElement.textContent = `${label}: ${timeString}`;
        }
    }

    function exportServerGroupReport() {
        console.log("Exporting server group report...");
        safeShowMessage("Export feature coming soon", "success");
    }

    function addNewServer() {
        console.log("addNewServer called");
        console.log("window.openServerModal exists:", typeof window.openServerModal);
        console.log("window.serverModal exists:", !!window.serverModal);

        // Open the server modal to add a new server
        if (typeof window.openServerModal === "function") {
            console.log("Calling window.openServerModal()");
            window.openServerModal();
        } else {
            console.error("Server modal not available");
            console.error(
                "window object keys:",
                Object.keys(window).filter((k) => k.includes("server") || k.includes("modal"))
            );
            alert("无法打开服务器添加弹窗，请刷新页面后重试");
        }
    }

    function openServerImportModal() {
        console.log("openServerImportModal called");
        console.log("Getting server import modal instance...");
        const importModal = getServerImportModal();
        console.log("Modal instance obtained:", importModal);
        console.log("Calling modal.open()...");
        importModal.open();
        console.log("modal.open() called");
    }

    // Global function to refresh server list (called from server-modal.js)
    window.refreshServerList = async function () {
        if (serverListManager) {
            await serverListManager.load({ applyFilter: true });
            updateServerGroupLastUpdateTime();
            safeShowMessage(
                currentLanguage === "en" ? "Server list refreshed" : "服务器列表已刷新",
                "success"
            );
        }
    };

    // 轮询检查服务器指标是否收集完成
    const pollingIntervals = new Map();
    let isPolling = false; // 防止并发轮询

    window.startServerMetricsPolling = function (serverId) {
        console.log("Starting metrics polling for server:", serverId);

        // 如果已有该服务器的轮询，先清除
        if (pollingIntervals.has(serverId)) {
            clearInterval(pollingIntervals.get(serverId));
        }

        let attemptCount = 0;
        const maxAttempts = 15; // 最多轮询15次（约45秒）

        const intervalId = setInterval(async () => {
            // 防止并发请求
            if (isPolling) {
                console.log("Skipping poll - previous request still in progress");
                return;
            }

            attemptCount++;
            console.log(`Polling attempt ${attemptCount} for server ${serverId}`);
            isPolling = true;

            try {
                // 只获取服务器列表数据，不触发其他API调用
                const response = await fetch("/admin/server-groups/api/list");
                if (!response.ok) throw new Error("Failed to fetch server list");

                const data = await response.json();
                const servers = data.servers || [];

                // 静默更新列表（不显示加载状态）
                // renderServerGroupList已被ServerListManager.render()替代，这里直接更新缓存
                // ServerListManager会在下次refresh时自动更新

                // 检查该服务器的指标是否已有数据
                const server = servers.find((s: any) => s.id === serverId);
                if (
                    server &&
                    server.cpuUsage != null &&
                    server.memoryUsage != null &&
                    server.diskUsage != null
                ) {
                    console.log("Metrics collected for server:", serverId);
                    clearInterval(intervalId);
                    pollingIntervals.delete(serverId);
                    safeShowMessage(
                        currentLanguage === "en" ? "Server metrics loaded" : "服务器监控数据已加载",
                        "success"
                    );
                } else if (attemptCount >= maxAttempts) {
                    console.warn("Metrics polling timeout for server:", serverId);
                    clearInterval(intervalId);
                    pollingIntervals.delete(serverId);
                    safeShowMessage(
                        currentLanguage === "en"
                            ? "Metrics collection timeout"
                            : "监控数据收集超时",
                        "warning"
                    );
                }
            } catch (error) {
                console.error("Error during metrics polling:", error);
            } finally {
                isPolling = false;
            }
        }, 3000); // 每3秒轮询一次（降低频率）

        pollingIntervals.set(serverId, intervalId);
    };

    // 获取当前语言，带默认值
    function getCurrentLanguage(): string {
        return typeof currentLanguage !== "undefined" ? currentLanguage : "zh";
    }

    function isServerDetailOpen() {
        return serverDetailOverlayManager?.isOpen() || false;
    }

    async function viewServerDetails(serverId: number | string) {
        if (serverDetailOverlayManager) {
            await serverDetailOverlayManager.view(Number(serverId));
        } else {
            console.error("ServerDetailOverlay not initialized");
        }
    }

    async function connectToServer(serverId: number | string) {
        console.log(`Connecting to server ${serverId}...`);
        // TODO: Implement SSH terminal connection
        showSuccess(
            getCurrentLanguage() === "en"
                ? "SSH terminal feature coming soon"
                : "SSH终端功能即将推出"
        );
    }

    async function refreshServer(serverId: number | string) {
        try {
            const response = await fetch(`/admin/server-groups/api/batch-refresh`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ serverIds: [serverId] })
            });

            if (response.ok) {
                safeShowMessage("Server refreshed successfully", "success");
                await serverListManager?.load();
            } else {
                throw new Error("Refresh failed");
            }
        } catch (error) {
            console.error("Server refresh failed:", error);
            showErrorMessage("Refresh failed. Please try again.");
        }
    }

    // 曝露服务器操作函数供模板调用

    window.ServerGroupModule = {
        init: initServerGroupManagement,
        cleanup: cleanupServerGroupManagement,
        onLanguageChange: handleLanguageChange,
        viewServerDetails,
        refreshServer,
        isDetailOpen: isServerDetailOpen
    };
})();

export {};
