import { useEffect, useRef, useCallback, useState } from 'react';

/**
 * WebSocket 连接状态
 */
export type WebSocketStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

/**
 * WebSocket Hook 配置
 */
export interface UseWebSocketOptions<T> {
  /**
   * WebSocket URL
   */
  url: string;

  /**
   * 是否自动连接
   * @default true
   */
  autoConnect?: boolean;

  /**
   * 重连延迟(毫秒)
   * @default 3000
   */
  reconnectDelay?: number;

  /**
   * 最大重连次数
   * @default 5
   */
  maxReconnectAttempts?: number;

  /**
   * 消息处理回调
   */
  onMessage?: (data: T) => void;

  /**
   * 连接成功回调
   */
  onOpen?: () => void;

  /**
   * 连接关闭回调
   */
  onClose?: () => void;

  /**
   * 错误回调
   */
  onError?: (error: Event) => void;
}

/**
 * WebSocket Hook 返回值
 */
export interface UseWebSocketReturn<T> {
  /**
   * 最新接收的消息
   */
  lastMessage: T | null;

  /**
   * 连接状态
   */
  status: WebSocketStatus;

  /**
   * 发送消息
   */
  sendMessage: (message: string | object) => void;

  /**
   * 手动连接
   */
  connect: () => void;

  /**
   * 手动断开
   */
  disconnect: () => void;

  /**
   * 重连次数
   */
  reconnectCount: number;
}

/**
 * WebSocket 自定义 Hook
 *
 * 提供 WebSocket 连接管理和消息处理
 *
 * @example
 * ```tsx
 * const { lastMessage, status, sendMessage } = useWebSocket<MetricsData>({
 *   url: 'ws://localhost:8080/ws/metrics',
 *   onMessage: (data) => console.log('Received:', data),
 * });
 * ```
 */
export function useWebSocket<T = unknown>(
  options: UseWebSocketOptions<T>
): UseWebSocketReturn<T> {
  const {
    url,
    autoConnect = true,
    reconnectDelay = 3000,
    maxReconnectAttempts = 5,
    onMessage,
    onOpen,
    onClose,
    onError,
  } = options;

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const [lastMessage, setLastMessage] = useState<T | null>(null);
  const [status, setStatus] = useState<WebSocketStatus>('disconnected');
  const [reconnectCount, setReconnectCount] = useState(0);

  /**
   * 连接 WebSocket
   */
  const connect = useCallback(() => {
    // 如果已连接,先断开
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      return;
    }

    try {
      setStatus('connecting');
      const ws = new WebSocket(url);

      ws.onopen = () => {
        setStatus('connected');
        setReconnectCount(0);
        onOpen?.();
      };

      ws.onmessage = (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data) as T;
          setLastMessage(data);
          onMessage?.(data);
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error);
        }
      };

      ws.onclose = () => {
        setStatus('disconnected');
        onClose?.();

        // 自动重连
        if (reconnectCount < maxReconnectAttempts) {
          reconnectTimeoutRef.current = setTimeout(() => {
            setReconnectCount((prev) => prev + 1);
            connect();
          }, reconnectDelay);
        }
      };

      ws.onerror = (error: Event) => {
        setStatus('error');
        onError?.(error);
        console.error('WebSocket error:', error);
      };

      wsRef.current = ws;
    } catch (error) {
      setStatus('error');
      console.error('Failed to create WebSocket connection:', error);
    }
  }, [url, reconnectCount, maxReconnectAttempts, reconnectDelay, onMessage, onOpen, onClose, onError]);

  /**
   * 断开 WebSocket
   */
  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }

    setStatus('disconnected');
    setReconnectCount(0);
  }, []);

  /**
   * 发送消息
   */
  const sendMessage = useCallback((message: string | object) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      const data = typeof message === 'string' ? message : JSON.stringify(message);
      wsRef.current.send(data);
    } else {
      console.warn('WebSocket is not connected. Cannot send message.');
    }
  }, []);

  /**
   * 初始化连接
   */
  useEffect(() => {
    if (autoConnect) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [autoConnect, connect, disconnect]);

  return {
    lastMessage,
    status,
    sendMessage,
    connect,
    disconnect,
    reconnectCount,
  };
}
