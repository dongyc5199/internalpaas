/**
 * 仪表板模块
 * 提供通用仪表板功能:自动刷新、通知系统、进度条动画、图表管理等
 */

import { http, formatBytes, formatUptime } from "@/utils";
import {
    createTimeSeriesChartConfig,
    createPieChartConfig,
    createBarChartConfig,
    getChartColor,
    destroyCharts
} from "@/utils/chart";
import { WebSocketManager, createWebSocket, WebSocketMessage } from "@/utils/websocket";
import Chart from "chart.js/auto";

interface StatsData {
    newValue?: number;
    serverCount?: number;
    activeServers?: number;
    stoppedServers?: number;
    errorServers?: number;
    cpuUsage?: number;
    memoryUsage?: number;
    diskUsage?: number;
    [key: string]: unknown;
}

interface ChartInstance {
    chart: Chart | null;
    canvas: HTMLCanvasElement | null;
}

type NotificationType = "success" | "error" | "warning" | "info";

class Dashboard {
    private refreshInterval: number;
    private notificationContainer: HTMLDivElement | null = null;
    private refreshTimer: number | null = null;
    private ws: WebSocketManager | null = null;
    private charts: Map<string, ChartInstance> = new Map();
    private realtimeDataBuffer: Array<{ timestamp: string; cpu: number; memory: number }> = [];
    private maxDataPoints = 20; // 最多保留20个数据点

    constructor(refreshInterval = 30000) {
        this.refreshInterval = refreshInterval;
        this.init();
    }

    /**
     * 初始化仪表板
     */
    private init(): void {
        this.setupNotifications();
        this.setupProgressBars();
        this.initializeCharts();
        this.setupAutoRefresh();
        this.setupWebSocket();
    }

    /**
     * 初始化所有图表
     */
    private initializeCharts(): void {
        // 检查是否在仪表盘页面
        if (!document.querySelector("[data-dashboard-charts]")) {
            return;
        }

        console.info("[Dashboard] 开始初始化图表...");

        // 初始化服务器状态图表
        this.createServerStatusChart();

        // 初始化资源使用率图表
        this.createResourceUsageChart();

        // 初始化实时监控图表
        this.createRealtimeMonitorChart();

        console.info("[Dashboard] 图表初始化完成");
    }

    /**
     * 创建服务器状态图表 (饼图)
     */
    private createServerStatusChart(): void {
        const canvas = document.getElementById("server-status-chart") as HTMLCanvasElement;
        if (!canvas) {
            console.warn("[Dashboard] 未找到服务器状态图表容器");
            return;
        }

        const ctx = canvas.getContext("2d");
        if (!ctx) {
            console.error("[Dashboard] 无法获取2D渲染上下文");
            return;
        }

        const config = createPieChartConfig(["运行中", "已停止", "错误"], [0, 0, 0], {
            plugins: {
                title: {
                    display: true,
                    text: "服务器状态分布",
                    font: {
                        size: 16,
                        weight: "bold"
                    }
                },
                legend: {
                    position: "bottom" as const
                }
            }
        });

        const chart = new Chart(ctx, config);
        this.charts.set("server-status", { chart, canvas });
        console.info("[Dashboard] 服务器状态图表已创建");
    }

    /**
     * 创建资源使用率图表 (柱状图)
     */
    private createResourceUsageChart(): void {
        const canvas = document.getElementById("resource-usage-chart") as HTMLCanvasElement;
        if (!canvas) {
            console.warn("[Dashboard] 未找到资源使用率图表容器");
            return;
        }

        const ctx = canvas.getContext("2d");
        if (!ctx) {
            console.error("[Dashboard] 无法获取2D渲染上下文");
            return;
        }

        const config = createBarChartConfig(
            ["CPU", "内存", "磁盘"],
            [
                {
                    label: "使用率 (%)",
                    data: [0, 0, 0],
                    backgroundColor: getChartColor(0, 0.6),
                    borderColor: getChartColor(0)
                }
            ],
            {
                plugins: {
                    title: {
                        display: true,
                        text: "系统资源使用率",
                        font: {
                            size: 16,
                            weight: "bold"
                        }
                    },
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100,
                        ticks: {
                            callback: (value: string | number) => `${value}%`
                        }
                    }
                }
            }
        );

        const chart = new Chart(ctx, config);
        this.charts.set("resource-usage", { chart, canvas });
        console.info("[Dashboard] 资源使用率图表已创建");
    }

    /**
     * 创建实时监控图表 (折线图)
     */
    private createRealtimeMonitorChart(): void {
        const canvas = document.getElementById("realtime-monitor-chart") as HTMLCanvasElement;
        if (!canvas) {
            console.warn("[Dashboard] 未找到实时监控图表容器");
            return;
        }

        const ctx = canvas.getContext("2d");
        if (!ctx) {
            console.error("[Dashboard] 无法获取2D渲染上下文");
            return;
        }

        // 初始化时间标签 (最近10个数据点)
        const now = new Date();
        const labels = Array.from({ length: 10 }, (_, i) => {
            const time = new Date(now.getTime() - (9 - i) * 60000);
            return `${time.getHours().toString().padStart(2, "0")}:${time
                .getMinutes()
                .toString()
                .padStart(2, "0")}`;
        });

        // 初始化数据缓冲区
        this.realtimeDataBuffer = labels.map((timestamp) => ({
            timestamp,
            cpu: 0,
            memory: 0
        }));

        const config = createTimeSeriesChartConfig(
            labels,
            [
                {
                    label: "CPU 使用率 (%)",
                    data: new Array(10).fill(0),
                    borderColor: getChartColor(0),
                    backgroundColor: getChartColor(0, 0.1),
                    fill: true
                },
                {
                    label: "内存使用率 (%)",
                    data: new Array(10).fill(0),
                    borderColor: getChartColor(1),
                    backgroundColor: getChartColor(1, 0.1),
                    fill: true
                }
            ],
            {
                plugins: {
                    title: {
                        display: true,
                        text: "实时资源监控",
                        font: {
                            size: 16,
                            weight: "bold"
                        }
                    },
                    legend: {
                        display: true,
                        position: "top" as const
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100,
                        ticks: {
                            callback: (value: string | number) => `${value}%`
                        }
                    }
                },
                animation: {
                    duration: 750
                }
            }
        );

        const chart = new Chart(ctx, config);
        this.charts.set("realtime-monitor", { chart, canvas });
        console.info("[Dashboard] 实时监控图表已创建");
    }

    /**
     * 设置自动刷新
     */
    private setupAutoRefresh(): void {
        this.refreshTimer = window.setInterval(() => {
            void this.updateStats();
        }, this.refreshInterval);
    }

    /**
     * 停止自动刷新
     */
    stopAutoRefresh(): void {
        if (this.refreshTimer !== null) {
            clearInterval(this.refreshTimer);
            this.refreshTimer = null;
        }
    }

    /**
     * 更新统计数据
     */
    async updateStats(): Promise<void> {
        try {
            // 检查当前页面是否需要stats API
            const isDashboardPage = document.querySelector(".dashboard-stats, [data-stats]");
            if (!isDashboardPage) {
                return; // 如果不是仪表板页面，跳过stats更新
            }

            const { data } = await http.get<StatsData>("/api/stats");
            this.updateStatCards(data);
            this.updateCharts(data);
        } catch (error) {
            // 静默处理404错误，因为不是所有页面都有stats API
            if (error instanceof Error && error.message.includes("404")) {
                // 404错误不记录，因为很多页面不需要这个API
                return;
            }
            console.error("[Dashboard] 更新统计数据失败:", error);
        }
    }

    /**
     * 更新所有图表数据
     */
    private updateCharts(data: StatsData): void {
        this.updateServerStatusChart(data);
        this.updateResourceUsageChart(data);
        this.updateRealtimeMonitorChart(data);
    }

    /**
     * 更新服务器状态图表
     */
    private updateServerStatusChart(data: StatsData): void {
        const chartInstance = this.charts.get("server-status");
        if (!chartInstance?.chart) {
            return;
        }

        const activeServers = data.activeServers || 0;
        const stoppedServers = data.stoppedServers || 0;
        const errorServers = data.errorServers || 0;

        chartInstance.chart.data.datasets[0].data = [activeServers, stoppedServers, errorServers];
        chartInstance.chart.update();

        console.debug("[Dashboard] 服务器状态图表已更新", {
            active: activeServers,
            stopped: stoppedServers,
            error: errorServers
        });
    }

    /**
     * 更新资源使用率图表
     */
    private updateResourceUsageChart(data: StatsData): void {
        const chartInstance = this.charts.get("resource-usage");
        if (!chartInstance?.chart) {
            return;
        }

        const cpuUsage = data.cpuUsage || 0;
        const memoryUsage = data.memoryUsage || 0;
        const diskUsage = data.diskUsage || 0;

        chartInstance.chart.data.datasets[0].data = [cpuUsage, memoryUsage, diskUsage];
        chartInstance.chart.update();

        console.debug("[Dashboard] 资源使用率图表已更新", {
            cpu: cpuUsage,
            memory: memoryUsage,
            disk: diskUsage
        });
    }

    /**
     * 更新实时监控图表 (滚动更新)
     */
    private updateRealtimeMonitorChart(data: StatsData): void {
        const chartInstance = this.charts.get("realtime-monitor");
        if (!chartInstance?.chart) {
            return;
        }

        const cpuUsage = data.cpuUsage || 0;
        const memoryUsage = data.memoryUsage || 0;

        // 生成当前时间标签
        const now = new Date();
        const timeLabel = `${now.getHours().toString().padStart(2, "0")}:${now
            .getMinutes()
            .toString()
            .padStart(2, "0")}`;

        // 添加新数据到缓冲区
        this.realtimeDataBuffer.push({
            timestamp: timeLabel,
            cpu: cpuUsage,
            memory: memoryUsage
        });

        // 保持最大数据点数量
        if (this.realtimeDataBuffer.length > this.maxDataPoints) {
            this.realtimeDataBuffer.shift();
        }

        // 更新图表
        const labels = this.realtimeDataBuffer.map((d) => d.timestamp);
        const cpuData = this.realtimeDataBuffer.map((d) => d.cpu);
        const memoryData = this.realtimeDataBuffer.map((d) => d.memory);

        chartInstance.chart.data.labels = labels;
        chartInstance.chart.data.datasets[0].data = cpuData;
        chartInstance.chart.data.datasets[1].data = memoryData;
        chartInstance.chart.update("none"); // 使用 'none' 模式减少动画,更流畅

        console.debug("[Dashboard] 实时监控图表已更新", {
            timestamp: timeLabel,
            cpu: cpuUsage,
            memory: memoryUsage,
            bufferSize: this.realtimeDataBuffer.length
        });
    }

    /**
     * 更新统计卡片
     */
    private updateStatCards(data: StatsData): void {
        const cards = document.querySelectorAll<HTMLElement>(".stat-card");
        cards.forEach((card) => {
            const number = card.querySelector<HTMLElement>(".stat-number");
            if (number) {
                const start = parseInt(number.textContent || "0", 10);
                const end = data.newValue || 0;
                this.animateNumber(number, start, end);
            }
        });
    }

    /**
     * 数字动画
     */
    private animateNumber(element: HTMLElement, start: number, end: number): void {
        const duration = 1000;
        const startTime = performance.now();

        const animate = (currentTime: number): void => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);

            const current = Math.floor(start + (end - start) * progress);
            element.textContent = String(current);

            if (progress < 1) {
                requestAnimationFrame(animate);
            }
        };

        requestAnimationFrame(animate);
    }

    /**
     * 设置通知系统
     */
    private setupNotifications(): void {
        this.notificationContainer = document.createElement("div");
        this.notificationContainer.id = "notification-container";
        this.notificationContainer.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 9999;
            pointer-events: none;
        `;
        document.body.appendChild(this.notificationContainer);
    }

    /**
     * 显示通知
     */
    showNotification(message: string, type: NotificationType = "info", duration = 3000): void {
        if (!this.notificationContainer) {
            return;
        }

        const notification = document.createElement("div");
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        notification.style.cssText = `
            margin-bottom: 10px;
            padding: 12px 20px;
            border-radius: 4px;
            background: white;
            box-shadow: 0 2px 8px rgba(0,0,0,0.15);
            pointer-events: auto;
            animation: slideInRight 0.3s ease-out;
        `;

        this.notificationContainer.appendChild(notification);

        setTimeout(() => {
            notification.style.animation = "slideOutRight 0.3s ease-in";
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, duration);
    }

    /**
     * 设置进度条动画
     */
    private setupProgressBars(): void {
        const progressBars = document.querySelectorAll<HTMLElement>(".progress-bar-fill");
        progressBars.forEach((bar) => {
            const width = bar.style.width || "0%";
            bar.style.width = "0%";
            setTimeout(() => {
                bar.style.width = width;
            }, 100);
        });
    }

    /**
     * 获取所有图表实例
     */
    getCharts(): Map<string, ChartInstance> {
        return this.charts;
    }

    /**
     * 设置WebSocket实时数据流
     */
    private setupWebSocket(): void {
        // 如果不在仪表盘页面，不启动WebSocket
        if (!document.querySelector("[data-dashboard-charts]")) {
            return;
        }

        // 获取WebSocket URL (从页面meta标签或默认配置)
        const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
        const host = window.location.host;
        const wsUrl = `${protocol}//${host}/ws/dashboard`;

        console.info("[Dashboard] 初始化WebSocket连接:", wsUrl);

        try {
            this.ws = createWebSocket({
                url: wsUrl,
                reconnectDelay: 5000,
                maxReconnectAttempts: 0, // 无限重连
                heartbeatInterval: 30000,
                debug: true
            });

            // 监听连接打开
            this.ws.on("open", () => {
                console.info("[Dashboard] WebSocket已连接");
                this.showNotification("实时数据连接已建立", "success");
            });

            // 监听接收消息
            this.ws.on<WebSocketMessage<StatsData>>("message", (message) => {
                if (message && typeof message === "object" && "type" in message) {
                    this.handleWebSocketMessage(message as WebSocketMessage<StatsData>);
                }
            });

            // 监听连接关闭
            this.ws.on("close", () => {
                console.warn("[Dashboard] WebSocket连接已关闭");
            });

            // 监听连接错误
            this.ws.on("error", (error) => {
                console.error("[Dashboard] WebSocket错误:", error);
            });

            // 监听重连中
            this.ws.on("reconnecting", (data) => {
                if (data && typeof data === "object" && "attempt" in data) {
                    console.info(`[Dashboard] 正在重连... (尝试 ${data.attempt})`);
                }
            });

            // 监听重连成功
            this.ws.on("reconnected", () => {
                console.info("[Dashboard] 重连成功");
                this.showNotification("实时数据连接已恢复", "success");
            });

            // 启动连接
            this.ws.connect();
        } catch (error) {
            console.error("[Dashboard] WebSocket初始化失败:", error);
        }
    }

    /**
     * 处理WebSocket消息
     */
    private handleWebSocketMessage(message: WebSocketMessage<StatsData>): void {
        console.debug("[Dashboard] 收到WebSocket消息:", message);

        switch (message.type) {
            case "stats_update":
                // 统计数据更新
                if (message.data) {
                    this.updateStatCards(message.data);
                    this.updateCharts(message.data);
                }
                break;

            case "realtime_metrics":
                // 实时指标数据
                if (message.data) {
                    this.updateRealtimeMonitorChart(message.data);
                }
                break;

            case "notification":
                // 通知消息
                if (message.data && "message" in message.data) {
                    const notifType = ("notificationType" in message.data ?
                        message.data.notificationType : "info") as NotificationType;
                    this.showNotification(String(message.data.message), notifType);
                }
                break;

            default:
                console.warn("[Dashboard] 未知的消息类型:", message.type);
        }
    }

    /**
     * 获取图表实例
     */
    getChart(name: string): Chart | null {
        return this.charts.get(name)?.chart || null;
    }

    /**
     * 销毁实例
     */
    destroy(): void {
        this.stopAutoRefresh();

        // 关闭WebSocket连接
        if (this.ws) {
            this.ws.destroy();
            this.ws = null;
        }

        // 销毁所有图表
        const chartInstances = Array.from(this.charts.values()).map((instance) => instance.chart);
        destroyCharts(chartInstances);
        this.charts.clear();

        // 清空数据缓冲区
        this.realtimeDataBuffer = [];

        // 移除通知容器
        if (this.notificationContainer && this.notificationContainer.parentNode) {
            this.notificationContainer.parentNode.removeChild(this.notificationContainer);
        }

        // 清理全局引用，允许重新初始化
        try {
            // 避免 TS 严格模式报错，直接赋 undefined
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (window as any).dashboard = undefined;
        } catch (e) {
            // 忽略任何异常，继续销毁
        }

        console.info("[Dashboard] 实例已销毁");
    }
}

/**
 * 工具函数: 显示加载动画
 */
export function showLoading(element: HTMLElement): void {
    const spinner = document.createElement("div");
    spinner.className = "loading-spinner";
    spinner.innerHTML = '<div class="spinner"></div>';
    element.appendChild(spinner);
}

/**
 * 工具函数: 隐藏加载动画
 */
export function hideLoading(element: HTMLElement): void {
    const spinner = element.querySelector(".loading-spinner");
    if (spinner) {
        spinner.remove();
    }
}

// 重新导出格式化工具函数 (保持向后兼容)
export { formatBytes, formatUptime };

// 全局类型声明
declare global {
    interface Window {
        dashboard?: Dashboard;
    }
}

// 初始化仪表板
function initDashboard(): Dashboard {
    if (!window.dashboard) {
        window.dashboard = new Dashboard();
        console.info("[Dashboard] 已完成初始化");
    }
    return window.dashboard;
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
        initDashboard();
    });
} else {
    initDashboard();
}

export { Dashboard, initDashboard };
export type { NotificationType, StatsData };
