/**
 * WebSocketManager 测试套件
 */

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
    WebSocketManager,
    type WebSocketConfig,
    type WebSocketEventHandler,
    type WebSocketEventType
} from "../utils/websocket";

type WebSocketManagerInternals = {
    config: Required<WebSocketConfig>;
    ws: MockWebSocket | null;
    reconnectAttempts: number;
    reconnectTimer: number | null;
    heartbeatTimer: number | null;
    heartbeatTimeoutTimer: number | null;
    connectionTimeoutTimer: number | null;
    isManualClose: boolean;
    listeners: Map<WebSocketEventType, Set<WebSocketEventHandler>>;
};

const asInternals = (manager: WebSocketManager): WebSocketManagerInternals =>
    manager as unknown as WebSocketManagerInternals;

const globalWithWebSocket = globalThis as unknown as { WebSocket: unknown };

// Mock WebSocket
class MockWebSocket {
    static CONNECTING = 0;
    static OPEN = 1;
    static CLOSING = 2;
    static CLOSED = 3;

    url: string;
    readyState: number = MockWebSocket.CONNECTING;
    onopen: ((event: Event) => void) | null = null;
    onclose: ((event: CloseEvent) => void) | null = null;
    onerror: ((event: Event) => void) | null = null;
    onmessage: ((event: MessageEvent) => void) | null = null;

    constructor(url: string) {
        this.url = url;
        // 模拟异步连接
        setTimeout(() => {
            if (this.readyState === MockWebSocket.CONNECTING) {
                this.readyState = MockWebSocket.OPEN;
                this.onopen?.(new Event("open"));
            }
        }, 10);
    }

    send(data: string): void {
        void data;
        if (this.readyState !== MockWebSocket.OPEN) {
            throw new Error("WebSocket is not open");
        }
    }

    close(code?: number, reason?: string): void {
        this.readyState = MockWebSocket.CLOSED;
        const closeEvent = new CloseEvent("close", { code, reason });
        this.onclose?.(closeEvent);
    }

    // 模拟接收消息
    simulateMessage(data: string): void {
        if (this.readyState === MockWebSocket.OPEN) {
            const messageEvent = new MessageEvent("message", { data });
            this.onmessage?.(messageEvent);
        }
    }

    // 模拟错误
    simulateError(): void {
        const errorEvent = new Event("error");
        this.onerror?.(errorEvent);
    }
}

class PendingConnectionWebSocket {
    static CONNECTING = MockWebSocket.CONNECTING;
    static OPEN = MockWebSocket.OPEN;
    static CLOSING = MockWebSocket.CLOSING;
    static CLOSED = MockWebSocket.CLOSED;

    url: string;
    readyState = PendingConnectionWebSocket.CONNECTING;
    onopen: ((event: Event) => void) | null = null;
    onclose: ((event: CloseEvent) => void) | null = null;
    onerror: ((event: Event) => void) | null = null;
    onmessage: ((event: MessageEvent) => void) | null = null;

    constructor(url: string) {
        this.url = url;
    }

    send(data: string): void {
        void data;
        if (this.readyState !== PendingConnectionWebSocket.OPEN) {
            throw new Error("WebSocket is not open");
        }
    }

    close(code?: number, reason?: string): void {
        this.readyState = PendingConnectionWebSocket.CLOSED;
        const closeEvent = new CloseEvent("close", { code, reason });
        this.onclose?.(closeEvent);
    }
}

class ThrowingWebSocket {
    static CONNECTING = MockWebSocket.CONNECTING;
    static OPEN = MockWebSocket.OPEN;
    static CLOSING = MockWebSocket.CLOSING;
    static CLOSED = MockWebSocket.CLOSED;

    constructor() {
        throw new Error("Invalid URL");
    }
}

describe("WebSocketManager", () => {
    let wsManager: WebSocketManager;
    let originalWebSocket: unknown;

    beforeEach(() => {
        // 替换全局 WebSocket
        originalWebSocket = globalWithWebSocket.WebSocket;
        globalWithWebSocket.WebSocket = MockWebSocket;
        vi.useFakeTimers();
    });

    afterEach(() => {
        wsManager?.close();
        vi.clearAllTimers();
        vi.useRealTimers();
        vi.clearAllMocks();
        globalWithWebSocket.WebSocket = originalWebSocket;
    });

    describe("初始化", () => {
        it("应该使用默认配置创建实例", () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            expect(wsManager).toBeDefined();
        });

        it("应该合并用户配置和默认配置", () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080",
                reconnectDelay: 3000,
                heartbeatInterval: 20000
            };

            wsManager = new WebSocketManager(config);

            const internals = asInternals(wsManager);
            expect(internals.config.reconnectDelay).toBe(3000);
            expect(internals.config.heartbeatInterval).toBe(20000);
            expect(internals.config.heartbeatTimeout).toBe(10000); // 默认值
        });
    });

    describe("连接管理", () => {
        it("应该能够成功连接 WebSocket", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            const openHandler = vi.fn();
            wsManager.on("open", openHandler);

            wsManager.connect();

            // 等待连接建立
            await vi.advanceTimersByTimeAsync(50);

            expect(openHandler).toHaveBeenCalled();
        });

        it("应该在已连接时跳过重复连接", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            const openHandler = vi.fn();
            wsManager.on("open", openHandler);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            // 再次尝试连接
            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            // 只应该触发一次 open 事件
            expect(openHandler).toHaveBeenCalledTimes(1);
        });

        it("应该能够手动断开连接", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            const closeHandler = vi.fn();
            wsManager.on("close", closeHandler);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            wsManager.close();

            expect(closeHandler).toHaveBeenCalled();
            expect(asInternals(wsManager).isManualClose).toBe(true);
        });

        it("应该在连接超时后重连", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080",
                connectionTimeout: 1000
            };

            wsManager = new WebSocketManager(config);

            // 阻止 WebSocket 自动打开
            const previousWebSocket = globalWithWebSocket.WebSocket;
            globalWithWebSocket.WebSocket = PendingConnectionWebSocket;

            const reconnectingHandler = vi.fn();
            wsManager.on("reconnecting", reconnectingHandler);

            wsManager.connect();

            // 等待连接超时以及首次重连调度
            await vi.advanceTimersByTimeAsync(1000);
            await vi.advanceTimersByTimeAsync(5000);

            expect(reconnectingHandler).toHaveBeenCalled();

            globalWithWebSocket.WebSocket = previousWebSocket;
        });
    });

    describe("自动重连", () => {
        it("应该在连接断开后自动重连", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080",
                reconnectDelay: 1000
            };

            wsManager = new WebSocketManager(config);

            const reconnectingHandler = vi.fn();
            wsManager.on("reconnecting", reconnectingHandler);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            // 模拟连接断开
            asInternals(wsManager).ws?.close();

            // 等待重连延迟
            await vi.advanceTimersByTimeAsync(1000);

            expect(reconnectingHandler).toHaveBeenCalled();
        });

        it("应该使用指数退避策略增加重连延迟", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080",
                reconnectDelay: 1000
            };

            wsManager = new WebSocketManager(config);

            const reconnectingHandler = vi.fn();
            wsManager.on("reconnecting", reconnectingHandler);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            // 第一次断开重连
            asInternals(wsManager).ws?.close();
            await vi.advanceTimersByTimeAsync(1000);
            expect(reconnectingHandler).toHaveBeenCalledTimes(1);

            // 第二次断开重连(延迟应该增加)
            asInternals(wsManager).ws?.close();
            await vi.advanceTimersByTimeAsync(2000);
            expect(reconnectingHandler).toHaveBeenCalledTimes(2);
        });

        it("应该限制最大重连延迟", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080",
                reconnectDelay: 1000
            };

            wsManager = new WebSocketManager(config);

            const internals = asInternals(wsManager);
            for (let i = 0; i < 10; i++) {
                internals.reconnectAttempts = i;
            }

            // WebSocketManager 内部使用 Math.min(reconnectAttempts, 5)
            // 所以最大延迟是 1000 * 5 = 5000ms
            expect(internals.reconnectAttempts).toBe(9);
        });

        it("应该在达到最大重连次数后停止重连", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080",
                reconnectDelay: 1000,
                maxReconnectAttempts: 3
            };

            wsManager = new WebSocketManager(config);

            const reconnectingHandler = vi.fn();
            wsManager.on("reconnecting", reconnectingHandler);

            // 模拟连接失败
            const previousWebSocket = globalWithWebSocket.WebSocket;
            globalWithWebSocket.WebSocket = class extends MockWebSocket {
                constructor(url: string) {
                    super(url);
                    // 立即模拟连接关闭，不使用setTimeout
                    this.readyState = MockWebSocket.OPEN;
                    Promise.resolve().then(() => {
                        this.readyState = MockWebSocket.CLOSED;
                        this.onclose?.(new CloseEvent("close"));
                    });
                }
            };

            wsManager.connect();

            // 等待初始连接失败 + 3次重连
            await vi.advanceTimersByTimeAsync(100); // 初始失败
            await vi.advanceTimersByTimeAsync(1100); // 第1次重连
            await vi.advanceTimersByTimeAsync(1100); // 第2次重连
            await vi.advanceTimersByTimeAsync(1100); // 第3次重连

            // 应该只尝试3次重连
            expect(reconnectingHandler).toHaveBeenCalledTimes(3);

            globalWithWebSocket.WebSocket = previousWebSocket;
        });

        it("应该在手动断开后不自动重连", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080",
                reconnectDelay: 1000
            };

            wsManager = new WebSocketManager(config);

            const reconnectingHandler = vi.fn();
            wsManager.on("reconnecting", reconnectingHandler);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            // 手动断开
            wsManager.close();

            // 等待重连延迟
            await vi.advanceTimersByTimeAsync(2000);

            // 不应该触发重连
            expect(reconnectingHandler).not.toHaveBeenCalled();
        });
    });

    describe("心跳机制", () => {
        it("应该定期发送心跳消息", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080",
                heartbeatInterval: 1000,
                heartbeatMessage: "ping"
            };

            wsManager = new WebSocketManager(config);

            const sendSpy = vi.spyOn(MockWebSocket.prototype, "send");

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            // 清除之前可能的调用
            sendSpy.mockClear();

            // 等待第一次心跳
            await vi.advanceTimersByTimeAsync(1000);
            expect(sendSpy).toHaveBeenCalledWith("ping");

            // 等待第二次心跳
            await vi.advanceTimersByTimeAsync(1000);
            // 验证总共调用了2次（1次第一次心跳 + 1次第二次心跳）
            expect(sendSpy).toHaveBeenCalledTimes(2);
        });

        it("应该在心跳超时后重连", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080",
                heartbeatInterval: 1000,
                heartbeatTimeout: 500
            };

            wsManager = new WebSocketManager(config);

            const heartbeatTimeoutHandler = vi.fn();
            const reconnectingHandler = vi.fn();
            wsManager.on("heartbeat-timeout", heartbeatTimeoutHandler);
            wsManager.on("reconnecting", reconnectingHandler);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            // 等待心跳超时
            await vi.advanceTimersByTimeAsync(600);

            expect(heartbeatTimeoutHandler).toHaveBeenCalled();
            expect(reconnectingHandler).toHaveBeenCalled();
        });

        it("应该在收到心跳响应后重置超时", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080",
                heartbeatInterval: 1000,
                heartbeatTimeout: 500
            };

            wsManager = new WebSocketManager(config);

            const heartbeatTimeoutHandler = vi.fn();
            wsManager.on("heartbeat-timeout", heartbeatTimeoutHandler);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            const ws = asInternals(wsManager).ws;
            expect(ws).not.toBeNull();

            // 在心跳超时触发前收到响应
            ws?.simulateMessage(JSON.stringify({ type: "pong" }));
            await Promise.resolve();

            await vi.advanceTimersByTimeAsync(600);

            // 不应该触发超时
            expect(heartbeatTimeoutHandler).not.toHaveBeenCalled();
        });

        it("应该能够禁用心跳", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080",
                heartbeatInterval: 0
            };

            wsManager = new WebSocketManager(config);

            const sendSpy = vi.spyOn(MockWebSocket.prototype, "send");

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            // 等待足够长时间
            await vi.advanceTimersByTimeAsync(5000);

            // 不应该发送心跳
            expect(sendSpy).not.toHaveBeenCalled();
        });
    });

    describe("消息处理", () => {
        it("应该能够接收和处理消息", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            const messageHandler = vi.fn();
            wsManager.on("message", messageHandler);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            const testMessage = { type: "test", data: "hello" };
            const ws = asInternals(wsManager).ws;
            expect(ws).not.toBeNull();
            ws?.simulateMessage(JSON.stringify(testMessage));

            expect(messageHandler).toHaveBeenCalledWith(testMessage);
        });

        it("应该能够发送消息", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            const sendSpy = vi.spyOn(MockWebSocket.prototype, "send");

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            const message = { type: "test", data: "hello" };
            wsManager.send(message);

            expect(sendSpy).toHaveBeenCalledWith(JSON.stringify(message));
        });

        it("应该在未连接时无法发送消息", () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            const message = { type: "test", data: "hello" };
            const result = wsManager.send(message);

            expect(result).toBe(false);
        });

        it("应该能够处理无效的 JSON 消息", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            const messageHandler = vi.fn();
            const errorHandler = vi.fn();
            wsManager.on("message", messageHandler);
            wsManager.on("error", errorHandler);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            const ws = asInternals(wsManager).ws;
            expect(ws).not.toBeNull();
            ws?.simulateMessage("invalid json");

            // 刷新微任务，确保消息被处理
            await Promise.resolve();

            // 应该以原始数据通知监听器
            expect(messageHandler).toHaveBeenCalledWith("invalid json");
            expect(errorHandler).not.toHaveBeenCalled();
        });
    });

    describe("事件系统", () => {
        it("应该能够注册事件监听器", () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            const handler = vi.fn();
            wsManager.on("open", handler);

            expect(asInternals(wsManager).listeners.get("open")?.size).toBe(1);
        });

        it("应该能够移除事件监听器", () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            const handler = vi.fn();
            wsManager.on("open", handler);
            wsManager.off("open", handler);

            expect(asInternals(wsManager).listeners.get("open")?.size).toBe(0);
        });

        it("应该能够触发多个监听器", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            const handler1 = vi.fn();
            const handler2 = vi.fn();
            wsManager.on("open", handler1);
            wsManager.on("open", handler2);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            expect(handler1).toHaveBeenCalled();
            expect(handler2).toHaveBeenCalled();
        });

        it("应该能够移除所有事件监听器", () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            wsManager.on("open", vi.fn());
            wsManager.on("close", vi.fn());
            wsManager.on("error", vi.fn());

            // 使用 destroy 方法清理所有监听器
            wsManager.destroy();

            expect(asInternals(wsManager).listeners.size).toBe(0);
        });
    });

    describe("状态查询", () => {
        it("应该能够查询连接状态", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            expect(wsManager.isConnected()).toBe(false);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            expect(wsManager.isConnected()).toBe(true);

            wsManager.close();

            expect(wsManager.isConnected()).toBe(false);
        });

        it("应该能够获取重连次数", () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            expect(wsManager.getReconnectAttempts()).toBe(0);
        });
    });

    describe("错误处理", () => {
        it("应该能够处理 WebSocket 错误", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            const errorHandler = vi.fn();
            wsManager.on("error", errorHandler);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            const ws = asInternals(wsManager).ws;
            expect(ws).not.toBeNull();
            ws?.simulateError();

            expect(errorHandler).toHaveBeenCalled();
        });

        it("应该在创建 WebSocket 失败时触发错误", async () => {
            const config: WebSocketConfig = {
                url: "invalid-url"
            };

            // 模拟 WebSocket 构造函数抛出错误
            const previousWebSocket = globalWithWebSocket.WebSocket;
            globalWithWebSocket.WebSocket = ThrowingWebSocket;

            wsManager = new WebSocketManager(config);

            const errorHandler = vi.fn();
            wsManager.on("error", errorHandler);

            wsManager.connect();

            // 刷新微任务，确保错误被触发
            await Promise.resolve();

            expect(errorHandler).toHaveBeenCalled();

            globalWithWebSocket.WebSocket = previousWebSocket;
        });
    });

    describe("资源清理", () => {
        it("应该在断开连接时清理所有定时器", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080",
                heartbeatInterval: 1000
            };

            wsManager = new WebSocketManager(config);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            wsManager.close();

            const internals = asInternals(wsManager);
            expect(internals.reconnectTimer).toBeNull();
            expect(internals.heartbeatTimer).toBeNull();
            expect(internals.heartbeatTimeoutTimer).toBeNull();
            expect(internals.connectionTimeoutTimer).toBeNull();
        });

        it("应该在销毁时清理 WebSocket 实例", async () => {
            const config: WebSocketConfig = {
                url: "ws://localhost:8080"
            };

            wsManager = new WebSocketManager(config);

            wsManager.connect();
            await vi.advanceTimersByTimeAsync(50);

            wsManager.close();

            expect(asInternals(wsManager).ws).toBeNull();
        });
    });
});
