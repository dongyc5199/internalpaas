import { useCallback, useEffect, useState } from 'react';
import {
  websocketClient,
  type WebSocketConnectionStatus,
} from '../api/websocket';

export interface UseWebSocketOptions<T> {
  /**
   * STOMP topic，例如 /topic/server-status/{id}
   */
  topic: string;
  /**
   * 是否自动连接
   * @default true
   */
  autoConnect?: boolean;
  /**
   * 收到消息
   */
  onMessage?: (data: T) => void;
  /**
   * 状态变更回调
   */
  onStatusChange?: (status: WebSocketConnectionStatus) => void;
}

export interface UseWebSocketReturn<T> {
  lastMessage: T | null;
  status: WebSocketConnectionStatus;
  reconnectCount: number;
  sendMessage: (destination: string, body: unknown) => void;
  connect: () => void;
  disconnect: () => void;
}

export function useWebSocket<T>(options: UseWebSocketOptions<T>): UseWebSocketReturn<T> {
  const { topic, autoConnect = true, onMessage, onStatusChange } = options;
  const [lastMessage, setLastMessage] = useState<T | null>(null);
  const [status, setStatus] = useState<WebSocketConnectionStatus>(websocketClient.getStatus());
  const [reconnectCount, setReconnectCount] = useState<number>(
    websocketClient.getReconnectAttempts()
  );

  useEffect(() => {
    if (autoConnect) {
      void websocketClient.connect();
    }
  }, [autoConnect]);

  useEffect(() => {
    const unsubscribe = websocketClient.onStatusChange((next) => {
      setStatus(next);
      setReconnectCount(websocketClient.getReconnectAttempts());
      onStatusChange?.(next);
    });

    return unsubscribe;
  }, [onStatusChange]);

  useEffect(() => {
    const unsubscribe = websocketClient.subscribe<T>({
      topic,
      handler: (data) => {
        setLastMessage(data);
        onMessage?.(data);
      },
    });

    return unsubscribe;
  }, [topic, onMessage]);

  const sendMessage = useCallback((destination: string, body: unknown) => {
    websocketClient.send(destination, body);
  }, []);

  const connect = useCallback(() => {
    void websocketClient.connect();
  }, []);

  const disconnect = useCallback(() => {
    websocketClient.disconnect();
  }, []);

  return {
    lastMessage,
    status,
    reconnectCount,
    sendMessage,
    connect,
    disconnect,
  };
}
