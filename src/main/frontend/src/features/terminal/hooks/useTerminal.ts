/**
 * useTerminal Hook
 * SSH终端WebSocket管理Hook
 *
 * 移植自: src/main/resources/templates/terminal/manager.html
 * 核心功能: WebSocket连接、消息处理、会话管理
 */

import { useRef, useCallback, useEffect, useState } from 'react';
import { getWebSocketUrl } from '../../../shared/api/terminalApi';
import type {
  TerminalMessage,
  ConnectPayload,
} from '../../../shared/types';

/**
 * WebSocket连接状态
 */
export type WSConnectionStatus =
  | 'disconnected'
  | 'connecting'
  | 'connected'
  | 'error';

/**
 * 终端会话信息
 */
export interface TerminalSession {
  sessionId?: string;
  serverId: number;
  serverName?: string;
  status: WSConnectionStatus;
  connected: boolean;
  lastError?: string;
}

/**
 * useTerminal Hook返回值
 */
export interface UseTerminalReturn {
  /** 当前会话信息 */
  session: TerminalSession;
  /** 发送输入数据 */
  sendInput: (data: string) => void;
  /** 连接到服务器 */
  connect: (serverId: number, cols?: number, rows?: number) => void;
  /** 断开连接 */
  disconnect: () => void;
  /** 重新连接 */
  reconnect: () => void;
  /** 发送终端大小变化 */
  resize: (cols: number, rows: number) => void;
}

/**
 * useTerminal Hook配置
 */
export interface UseTerminalOptions {
  /** 服务器输出回调 */
  onOutput?: (data: string) => void;
  /** 连接成功回调 */
  onConnected?: (sessionId?: string) => void;
  /** 连接断开回调 */
  onDisconnected?: () => void;
  /** 错误回调 */
  onError?: (error: string) => void;
  /** 自动重连 */
  autoReconnect?: boolean;
  /** 重连间隔(ms) */
  reconnectInterval?: number;
}

/**
 * useTerminal Hook
 *
 * 使用示例:
 * ```tsx
 * const { session, sendInput, connect, disconnect } = useTerminal({
 *   onOutput: (data) => terminalRef.current?.write(data),
 *   onConnected: () => console.log('Connected!'),
 *   onError: (error) => console.error(error),
 * });
 *
 * // 连接到服务器
 * connect(serverId, 80, 24);
 *
 * // 发送输入
 * sendInput('ls -la\n');
 * ```
 */
export function useTerminal(options: UseTerminalOptions = {}): UseTerminalReturn {
  const {
    onOutput,
    onConnected,
    onDisconnected,
    onError,
    autoReconnect = false,
    reconnectInterval = 3000,
  } = options;

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<number | null>(null);
  const serverIdRef = useRef<number | null>(null);
  const terminalSizeRef = useRef<{ cols: number; rows: number }>({
    cols: 80,
    rows: 24,
  });

  const [session, setSession] = useState<TerminalSession>({
    serverId: 0,
    status: 'disconnected',
    connected: false,
  });

  /**
   * 发送WebSocket消息
   */
  const sendMessage = useCallback((message: TerminalMessage): void => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message));
    } else {
      console.warn('WebSocket not ready, message not sent:', message);
    }
  }, []);

  /**
   * 处理WebSocket消息 (移植自manager.html line 721-748, 788-826)
   */
  const handleMessage = useCallback(
    (event: MessageEvent): void => {
      try {
        const data = JSON.parse(event.data) as TerminalMessage;
        console.log('WebSocket received:', data);

        switch (data.type) {
          case 'output':
          case 'data':
            // 服务器输出 (line 730-732, 800-806)
            if (typeof data.data === 'string' && onOutput) {
              onOutput(data.data);
            }
            break;

          case 'connected':
            // 连接成功 (line 733-739, 793-799)
            setSession((prev) => ({
              ...prev,
              sessionId: data.sessionId,
              status: 'connected',
              connected: true,
              lastError: undefined,
            }));
            if (onConnected) {
              onConnected(data.sessionId);
            }
            break;

          case 'error':
            // 错误消息 (line 807-811)
            const errorMsg =
              typeof data.data === 'object' && 'message' in data.data
                ? (data.data as { message: string }).message
                : 'Unknown error';
            setSession((prev) => ({
              ...prev,
              status: 'error',
              connected: false,
              lastError: errorMsg,
            }));
            if (onError) {
              onError(errorMsg);
            }
            if (onOutput) {
              onOutput(`\r\n\x1b[31m错误: ${errorMsg}\x1b[0m\r\n`);
            }
            break;

          case 'pong':
            // 心跳响应 (line 813-815)
            console.log('Heartbeat pong received');
            break;

          default:
            // 兜底处理 (line 817-825)
            console.log('Unknown message type:', data.type, data);
            if (typeof data.data === 'string' && onOutput) {
              onOutput(data.data);
            }
            break;
        }
      } catch (e) {
        // 不是JSON格式，直接写入终端 (line 744-747)
        console.log('Writing raw data to terminal:', event.data);
        if (onOutput) {
          onOutput(event.data);
        }
      }
    },
    [onOutput, onConnected, onError]
  );

  /**
   * 创建WebSocket连接 (移植自manager.html line 705-761)
   */
  const createWebSocket = useCallback((): void => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      console.warn('WebSocket already connected');
      return;
    }

    console.log('Creating WebSocket connection');

    // 创建WebSocket (line 711)
    const ws = new WebSocket(getWebSocketUrl());
    wsRef.current = ws;

    // 连接打开 (line 714-719)
    ws.onopen = (): void => {
      console.log('WebSocket connected');
      setSession((prev) => ({
        ...prev,
        status: 'connected',
        connected: true,
      }));

      // 如果有serverId，立即发送连接请求
      if (serverIdRef.current) {
        const connectPayload: ConnectPayload = {
          serverId: serverIdRef.current,
          cols: terminalSizeRef.current.cols,
          rows: terminalSizeRef.current.rows,
        };

        const message: TerminalMessage = {
          type: 'connect',
          data: connectPayload,
          timestamp: Date.now(),
        };

        ws.send(JSON.stringify(message));
      }
    };

    // 接收消息 (line 721-748)
    ws.onmessage = handleMessage;

    // 连接错误 (line 751-754)
    ws.onerror = (error): void => {
      console.error('WebSocket error:', error);
      setSession((prev) => ({
        ...prev,
        status: 'error',
        connected: false,
        lastError: 'WebSocket connection error',
      }));
      if (onError) {
        onError('WebSocket connection error');
      }
    };

    // 连接关闭 (line 756-760)
    ws.onclose = (): void => {
      console.log('WebSocket closed');
      setSession((prev) => ({
        ...prev,
        status: 'disconnected',
        connected: false,
      }));
      wsRef.current = null;

      if (onDisconnected) {
        onDisconnected();
      }

      // 自动重连
      if (autoReconnect && serverIdRef.current) {
        console.log(`Auto reconnecting in ${reconnectInterval}ms...`);
        reconnectTimerRef.current = window.setTimeout(() => {
          createWebSocket();
        }, reconnectInterval);
      }
    };
  }, [
    handleMessage,
    onError,
    onDisconnected,
    autoReconnect,
    reconnectInterval,
  ]);

  /**
   * 连接到SSH服务器 (移植自manager.html line 764-786)
   */
  const connect = useCallback(
    (serverId: number, cols = 80, rows = 24): void => {
      serverIdRef.current = serverId;
      terminalSizeRef.current = { cols, rows };

      setSession({
        serverId,
        status: 'connecting',
        connected: false,
      });

      // 创建WebSocket连接
      createWebSocket();
    },
    [createWebSocket]
  );

  /**
   * 断开连接 (移植自manager.html line 878-897)
   */
  const disconnect = useCallback((): void => {
    // 清除重连定时器
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }

    // 关闭WebSocket
    if (wsRef.current) {
      if (wsRef.current.readyState === WebSocket.OPEN) {
        // 发送断开消息
        sendMessage({
          type: 'disconnect',
          timestamp: Date.now(),
        });

        wsRef.current.close();
      }
      wsRef.current = null;
    }

    serverIdRef.current = null;

    setSession((prev) => ({
      ...prev,
      status: 'disconnected',
      connected: false,
    }));
  }, [sendMessage]);

  /**
   * 重新连接 (移植自manager.html line 963-1013)
   */
  const reconnect = useCallback((): void => {
    if (!serverIdRef.current) {
      console.warn('No server to reconnect to');
      return;
    }

    console.log('Reconnecting...');

    // 关闭现有连接
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }

    // 更新状态
    setSession((prev) => ({
      ...prev,
      status: 'connecting',
      connected: false,
    }));

    // 延迟重连
    setTimeout(() => {
      createWebSocket();
    }, 1000);
  }, [createWebSocket]);

  /**
   * 发送输入数据 (移植自manager.html line 648-664)
   */
  const sendInput = useCallback(
    (data: string): void => {
      if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) {
        console.warn('WebSocket not connected, input not sent');
        return;
      }

      const message: TerminalMessage = {
        type: 'input',
        data,
        timestamp: Date.now(),
      };

      sendMessage(message);
    },
    [sendMessage]
  );

  /**
   * 发送终端大小变化
   */
  const resize = useCallback(
    (cols: number, rows: number): void => {
      terminalSizeRef.current = { cols, rows };

      if (wsRef.current?.readyState === WebSocket.OPEN) {
        const message: TerminalMessage = {
          type: 'resize',
          data: { cols, rows },
          timestamp: Date.now(),
        };

        sendMessage(message);
      }
    },
    [sendMessage]
  );

  // 组件卸载时清理
  useEffect(() => {
    return () => {
      disconnect();
    };
  }, [disconnect]);

  return {
    session,
    sendInput,
    connect,
    disconnect,
    reconnect,
    resize,
  };
}
