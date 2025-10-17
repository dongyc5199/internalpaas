/**
 * WebSocket 连接管理工具
 * 提供自动重连、心跳检测、事件处理等功能
 */

export interface WebSocketConfig {
    /** WebSocket 连接 URL */
    url: string;
    /** 自动重连延迟(毫秒), 默认 5000 */
    reconnectDelay?: number;
    /** 最大重连次数, 0表示无限重连, 默认 0 */
    maxReconnectAttempts?: number;
    /** 心跳间隔(毫秒), 0表示禁用心跳, 默认 30000 */
    heartbeatInterval?: number;
    /** 心跳超时(毫秒), 默认 10000 */
    heartbeatTimeout?: number;
    /** 心跳消息, 默认 "ping" */
    heartbeatMessage?: string;
    /** 连接超时(毫秒), 默认 10000 */
    connectionTimeout?: number;
    /** 调试模式 */
    debug?: boolean;
}

export interface WebSocketMessage<T = unknown> {
    type: string;
    data: T;
    timestamp?: number;
}

export type WebSocketEventType =
    | "open"
    | "close"
    | "error"
    | "message"
    | "reconnecting"
    | "reconnected"
    | "heartbeat"
    | "heartbeat-timeout";

export type WebSocketEventHandler<T = unknown> = (data?: T) => void;

/**
 * WebSocket 管理器类
 */
export class WebSocketManager {
    private config: Required<WebSocketConfig>;
    private ws: WebSocket | null = null;
    private reconnectTimer: number | null = null;
    private heartbeatTimer: number | null = null;
    private heartbeatTimeoutTimer: number | null = null;
    private reconnectAttempts: number = 0;
    private isManualClose: boolean = false;
    private connectionTimeoutTimer: number | null = null;
    private listeners: Map<WebSocketEventType, Set<WebSocketEventHandler>> = new Map();
    private isConnecting: boolean = false;

    constructor(config: WebSocketConfig) {
        this.config = {
            url: config.url,
            reconnectDelay: config.reconnectDelay ?? 5000,
            maxReconnectAttempts: config.maxReconnectAttempts ?? 0,
            heartbeatInterval: config.heartbeatInterval ?? 30000,
            heartbeatTimeout: config.heartbeatTimeout ?? 10000,
            heartbeatMessage: config.heartbeatMessage ?? "ping",
            connectionTimeout: config.connectionTimeout ?? 10000,
            debug: config.debug ?? false
        };

        this.log("WebSocketManager created with config:", this.config);
    }

    /**
     * 连接 WebSocket
     */
    connect(): void {
        if (this.isConnecting) {
            this.log("Connection already in progress, skipping...");
            return;
        }

        if (this.ws?.readyState === WebSocket.OPEN) {
            this.log("WebSocket already connected");
            return;
        }

        this.isConnecting = true;
        this.isManualClose = false;
        this.log(`Connecting to ${this.config.url}...`);

        try {
            this.ws = new WebSocket(this.config.url);
            this.setupWebSocketHandlers();
            this.setupConnectionTimeout();
        } catch (error) {
            this.isConnecting = false;
            this.log("Failed to create WebSocket:", error);
            this.emit("error", error);
            this.scheduleReconnect();
        }
    }

    /**
     * 设置 WebSocket 事件处理器
     */
    private setupWebSocketHandlers(): void {
        if (!this.ws) return;

        this.ws.onopen = this.handleOpen.bind(this);
        this.ws.onclose = this.handleClose.bind(this);
        this.ws.onerror = this.handleError.bind(this);
        this.ws.onmessage = this.handleMessage.bind(this);
    }

    /**
     * 设置连接超时
     */
    private setupConnectionTimeout(): void {
        this.clearConnectionTimeout();

        this.connectionTimeoutTimer = window.setTimeout(() => {
            if (this.ws?.readyState !== WebSocket.OPEN) {
                this.log("Connection timeout");
                this.ws?.close();
                this.scheduleReconnect();
            }
        }, this.config.connectionTimeout);
    }

    /**
     * 清除连接超时
     */
    private clearConnectionTimeout(): void {
        if (this.connectionTimeoutTimer !== null) {
            clearTimeout(this.connectionTimeoutTimer);
            this.connectionTimeoutTimer = null;
        }
    }

    /**
     * 处理连接打开
     */
    private handleOpen(event: Event): void {
        this.isConnecting = false;
        this.clearConnectionTimeout();
        this.reconnectAttempts = 0;

        this.log("WebSocket connected");
        this.emit("open", event);

        // 如果这是重连成功
        if (this.reconnectTimer !== null) {
            this.emit("reconnected");
        }

        // 启动心跳
        this.startHeartbeat();
    }

    /**
     * 处理连接关闭
     */
    private handleClose(event: CloseEvent): void {
        this.isConnecting = false;
        this.clearConnectionTimeout();
        this.stopHeartbeat();

        this.log(
            `WebSocket closed: code=${event.code}, reason=${event.reason}, clean=${event.wasClean}`
        );
        this.emit("close", event);

        // 如果不是手动关闭，则尝试重连
        if (!this.isManualClose) {
            this.scheduleReconnect();
        }
    }

    /**
     * 处理连接错误
     */
    private handleError(event: Event): void {
        this.log("WebSocket error:", event);
        this.emit("error", event);
    }

    /**
     * 处理接收到的消息
     */
    private handleMessage(event: MessageEvent): void {
        try {
            // 尝试解析 JSON
            let message: unknown;
            try {
                message = JSON.parse(event.data);
            } catch {
                // 如果不是 JSON，直接使用原始数据
                message = event.data;
            }

            // 检查是否是心跳响应
            if (
                message === "pong" ||
                (typeof message === "object" &&
                    message !== null &&
                    "type" in message &&
                    message.type === "pong")
            ) {
                this.handleHeartbeatResponse();
                return;
            }

            this.log("Message received:", message);
            this.emit("message", message);
        } catch (error) {
            this.log("Failed to parse message:", error);
        }
    }

    /**
     * 调度重连
     */
    private scheduleReconnect(): void {
        // 检查是否达到最大重连次数
        if (
            this.config.maxReconnectAttempts > 0 &&
            this.reconnectAttempts >= this.config.maxReconnectAttempts
        ) {
            this.log("Max reconnect attempts reached, giving up");
            return;
        }

        // 清除现有的重连定时器
        if (this.reconnectTimer !== null) {
            clearTimeout(this.reconnectTimer);
        }

        this.reconnectAttempts++;
        const delay = this.config.reconnectDelay * Math.min(this.reconnectAttempts, 5); // 指数退避，最多5倍

        this.log(`Scheduling reconnect attempt ${this.reconnectAttempts} in ${delay}ms`);
        this.emit("reconnecting", { attempt: this.reconnectAttempts, delay });

        this.reconnectTimer = window.setTimeout(() => {
            this.reconnectTimer = null;
            this.connect();
        }, delay);
    }

    /**
     * 启动心跳
     */
    private startHeartbeat(): void {
        // 如果心跳间隔为0，则不启动心跳
        if (this.config.heartbeatInterval === 0) {
            return;
        }

        this.stopHeartbeat();

        this.log("Starting heartbeat");
        this.heartbeatTimer = window.setInterval(() => {
            this.sendHeartbeat();
        }, this.config.heartbeatInterval);

        // 立即发送一次心跳
        this.sendHeartbeat();
    }

    /**
     * 停止心跳
     */
    private stopHeartbeat(): void {
        if (this.heartbeatTimer !== null) {
            clearInterval(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }

        if (this.heartbeatTimeoutTimer !== null) {
            clearTimeout(this.heartbeatTimeoutTimer);
            this.heartbeatTimeoutTimer = null;
        }
    }

    /**
     * 发送心跳
     */
    private sendHeartbeat(): void {
        if (this.ws?.readyState !== WebSocket.OPEN) {
            return;
        }

        try {
            this.log("Sending heartbeat");
            this.ws.send(this.config.heartbeatMessage);
            this.emit("heartbeat");

            // 设置心跳超时
            this.heartbeatTimeoutTimer = window.setTimeout(() => {
                this.log("Heartbeat timeout");
                this.emit("heartbeat-timeout");
                this.ws?.close();
            }, this.config.heartbeatTimeout);
        } catch (error) {
            this.log("Failed to send heartbeat:", error);
        }
    }

    /**
     * 处理心跳响应
     */
    private handleHeartbeatResponse(): void {
        this.log("Heartbeat response received");

        // 清除心跳超时定时器
        if (this.heartbeatTimeoutTimer !== null) {
            clearTimeout(this.heartbeatTimeoutTimer);
            this.heartbeatTimeoutTimer = null;
        }
    }

    /**
     * 发送消息
     */
    send(message: unknown): boolean {
        if (this.ws?.readyState !== WebSocket.OPEN) {
            this.log("Cannot send message: WebSocket not open");
            return false;
        }

        try {
            const data = typeof message === "string" ? message : JSON.stringify(message);
            this.ws.send(data);
            this.log("Message sent:", message);
            return true;
        } catch (error) {
            this.log("Failed to send message:", error);
            return false;
        }
    }

    /**
     * 关闭连接
     */
    close(code?: number, reason?: string): void {
        this.isManualClose = true;
        this.stopHeartbeat();

        if (this.reconnectTimer !== null) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }

        if (this.ws) {
            this.log(`Closing WebSocket: code=${code}, reason=${reason}`);
            this.ws.close(code, reason);
            this.ws = null;
        }
    }

    /**
     * 监听事件
     */
    on<T = unknown>(event: WebSocketEventType, handler: WebSocketEventHandler<T>): () => void {
        if (!this.listeners.has(event)) {
            this.listeners.set(event, new Set());
        }

        this.listeners.get(event)!.add(handler as WebSocketEventHandler);

        // 返回取消监听函数
        return () => this.off(event, handler);
    }

    /**
     * 取消监听事件
     */
    off<T = unknown>(event: WebSocketEventType, handler: WebSocketEventHandler<T>): void {
        const handlers = this.listeners.get(event);
        if (handlers) {
            handlers.delete(handler as WebSocketEventHandler);
        }
    }

    /**
     * 触发事件
     */
    private emit(event: WebSocketEventType, data?: unknown): void {
        const handlers = this.listeners.get(event);
        if (handlers) {
            handlers.forEach((handler) => {
                try {
                    handler(data);
                } catch (error) {
                    this.log(`Error in ${event} handler:`, error);
                }
            });
        }
    }

    /**
     * 获取连接状态
     */
    getReadyState(): number {
        return this.ws?.readyState ?? WebSocket.CLOSED;
    }

    /**
     * 检查是否已连接
     */
    isConnected(): boolean {
        return this.ws?.readyState === WebSocket.OPEN;
    }

    /**
     * 获取重连次数
     */
    getReconnectAttempts(): number {
        return this.reconnectAttempts;
    }

    /**
     * 日志输出
     */
    private log(...args: unknown[]): void {
        if (this.config.debug) {
            console.log("[WebSocketManager]", ...args);
        }
    }

    /**
     * 销毁实例
     */
    destroy(): void {
        this.close();
        this.listeners.clear();
        this.log("WebSocketManager destroyed");
    }
}

/**
 * 创建 WebSocket 管理器实例
 */
export function createWebSocket(config: WebSocketConfig): WebSocketManager {
    return new WebSocketManager(config);
}
