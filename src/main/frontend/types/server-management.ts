/**
 * 服务器管理相关类型定义
 */

/**
 * 服务器基本信息
 */
export interface Server {
    id: number;
    name: string;
    host: string;
    port: number;
    username: string;
    status: "online" | "offline" | "warning" | "unknown";
    healthScore?: number;
    cpuUsage?: number;
    memoryUsage?: number;
    diskUsage?: number;
    apps?: number;
    users?: number;
    networkIn?: number;
    networkOut?: number;
    uptime?: number;
    lastChecked?: string;
}

/**
 * 服务器列表加载选项
 */
export interface LoadOptions {
    silent?: boolean;
    applyFilter?: boolean;
}

/**
 * 过滤条件
 */
export interface FilterCriteria {
    status?: "all" | "online" | "offline" | "warning";
    search?: string;
}

/**
 * 视图模式
 */
export type ViewMode = "table" | "card";

/**
 * ServerListManager依赖注入接口
 */
export interface ServerListDependencies {
    // 工具函数
    formatBytes: (bytes: number) => string;
    formatUptime: (seconds: number) => string;
    formatPercentage: (value: number, decimals?: number) => string;
    showSuccess: (message: string) => void;
    showError: (message: string) => void;

    // 外部函数（可选）
    viewServerDetails?: (serverId: number) => void;
    connectToServer?: (serverId: number) => void;
    loadCharts?: () => void;

    // 国际化
    t: (key: string, namespace: string) => string;
    getCurrentLanguage: () => string;
}

/**
 * ServerListManager公共API
 */
export interface ServerListManagerAPI {
    // 生命周期
    init(): Promise<void>;
    cleanup(): void;

    // 数据操作
    load(options?: LoadOptions): Promise<void>;
    refresh(): Promise<void>;
    refreshServer(serverId: number): Promise<void>;

    // 视图控制
    switchView(mode: ViewMode): void;
    filter(criteria: FilterCriteria): void;

    // 批量操作
    selectAll(): void;
    deselectAll(): void;
    batchRefresh(): Promise<void>;
    getSelected(): number[];

    // 自动刷新
    startAutoRefresh(interval?: number): void;
    stopAutoRefresh(): void;
}

/**
 * 服务器详情数据
 */
export interface ServerDetail {
    id: number;
    name: string;
    host: string;
    port: number;
    username: string;
    status: "online" | "offline" | "warning" | "unknown";

    // 监控指标
    cpuUsage?: number;
    memoryUsage?: number;
    memoryTotal?: number;
    memoryUsed?: number;
    diskUsage?: number;
    diskTotal?: number;
    diskUsed?: number;
    networkIn?: number;
    networkOut?: number;
    uptime?: number;
    loadAverage?: number[];

    // 时间信息
    lastChecked?: string;

    // 详细信息
    processes?: ServerProcess[];
    applications?: ServerApplication[];
    users?: ServerUser[];

    // 历史数据
    cpuHistory?: number[];
    memoryHistory?: number[];
    diskHistory?: number[];
    networkInHistory?: number[];
    networkOutHistory?: number[];
    timestamps?: string[];
}

/**
 * 服务器进程信息
 */
export interface ServerProcess {
    pid: number;
    name: string;
    user: string;
    cpuUsage: number;
    memoryUsage: number;
    status: string;
    command?: string;
}

/**
 * 服务器应用信息
 */
export interface ServerApplication {
    id: number;
    name: string;
    port: number;
    status: "running" | "stopped" | "error";
    uptime?: number;
    memoryUsage?: number;
}

/**
 * 服务器用户信息
 */
export interface ServerUser {
    username: string;
    terminal: string;
    from: string;
    loginTime: string;
}

/**
 * ServerDetailOverlay依赖注入接口
 */
export interface ServerDetailDependencies {
    // 工具函数
    formatBytes: (bytes: number) => string;
    formatUptime: (seconds: number) => string;
    formatPercentage: (value: number, decimals?: number) => string;
    formatDateTime: (date: Date | string) => string;
    showSuccess: (message: string) => void;
    showError: (message: string) => void;

    // Chart.js
    Chart: any;

    // 外部函数
    connectToServer?: (serverId: number) => void;
    terminateProcess?: (serverId: number, pid: number) => Promise<void>;

    // 国际化
    t: (key: string, namespace: string) => string;
    getCurrentLanguage: () => string;
}

/**
 * ServerDetailOverlay公共API
 */
export interface ServerDetailOverlayAPI {
    // 生命周期
    init(): void;
    teardown(): void;

    // 核心功能
    view(serverId: number): Promise<void>;
    close(): void;
    isOpen(): boolean;

    // 数据操作
    refresh(isAuto?: boolean): Promise<void>;

    // 图表控制
    pauseCharts(): void;
    resumeCharts(): void;

    // 导航
    scrollToSection(sectionId: string): void;
}
