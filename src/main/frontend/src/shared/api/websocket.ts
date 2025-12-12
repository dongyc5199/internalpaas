import { Client, type Frame, type IMessage, type StompSubscription } from '@stomp/stompjs';
import { env } from '../../config/env';

export type WebSocketConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'reconnecting';

export interface SubscribeOptions<T> {
  topic: string;
  handler: (payload: T, message: IMessage) => void;
}

interface SubscriptionInfo {
  topic: string;
  subscription: StompSubscription;
  handler: (message: IMessage) => void;
}

/**
 * STOMP WebSocket 封装，提供连接、订阅和发送能力，并带指数退避重连。
 */
class StompWebSocketClient {
  private client: Client | null = null;
  private status: WebSocketConnectionStatus = 'disconnected';
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 5;
  private readonly subscriptions: Map<string, SubscriptionInfo> = new Map();
  private readonly statusListeners: Set<(status: WebSocketConnectionStatus) => void> = new Set();

  public getStatus(): WebSocketConnectionStatus {
    return this.status;
  }

  public getReconnectAttempts(): number {
    return this.reconnectAttempts;
  }

  private setStatus(next: WebSocketConnectionStatus): void {
    this.status = next;
    this.statusListeners.forEach((listener) => {
      listener(next);
    });
  }

  private buildClient(): Client {
    const brokerURL = env.wsBaseUrl;

    const client = new Client({
      brokerURL,
      reconnectDelay: 0, // 我们手动控制重连
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: (msg: string) => {
        if (import.meta.env.DEV) {
          console.debug('[STOMP]', msg);
        }
      },
      onConnect: this.handleConnect,
      onStompError: this.handleError,
      onWebSocketClose: () => {
        this.setStatus('disconnected');
        this.scheduleReconnect();
      },
      onWebSocketError: () => {
        this.setStatus('disconnected');
        this.scheduleReconnect();
      },
    });

    return client;
  }

  private handleConnect = (_frame?: Frame): void => {
    this.setStatus('connected');
    this.reconnectAttempts = 0;

    // 重放订阅
    for (const [key, info] of this.subscriptions) {
      const sub = this.client?.subscribe(info.topic, (message: IMessage) => {
        info.handler(message);
      });
      if (sub) {
        this.subscriptions.set(key, { topic: info.topic, subscription: sub, handler: info.handler });
      }
    }
  };

  private handleError = (): void => {
    this.setStatus('disconnected');
    this.scheduleReconnect();
  };

  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      return;
    }
    this.reconnectAttempts += 1;
    const delay = Math.min(1000 * 2 ** (this.reconnectAttempts - 1), 15000);
    this.setStatus('reconnecting');
    setTimeout(() => {
      void this.connect();
    }, delay);
  }

  public async connect(): Promise<void> {
    if (this.client && this.status === 'connected') {
      return;
    }

    this.client = this.buildClient();
    this.setStatus('connecting');
    this.client.activate();
  }

  public disconnect(): void {
    this.client?.deactivate();
    this.client = null;
    this.setStatus('disconnected');
    this.subscriptions.clear();
  }

  public subscribe<T>(options: SubscribeOptions<T>): () => void {
    const { topic, handler } = options;
    const id = `${topic}-${Date.now()}-${Math.random()}`;

    if (!this.client || this.status === 'disconnected') {
      void this.connect();
    }

    const subscription = this.client?.subscribe(topic, (message: IMessage) => {
      try {
        const parsed = JSON.parse(message.body) as T;
        handler(parsed, message);
      } catch (error) {
        console.error('[WebSocket] Failed to parse message', error);
      }
    });

    if (subscription) {
      this.subscriptions.set(id, {
        topic,
        subscription,
        handler: (message: IMessage) => {
          // 这里仅用于重放订阅
          const parsed = JSON.parse(message.body) as T;
          handler(parsed, message);
        },
      });
    }

    return () => {
      const info = this.subscriptions.get(id);
      info?.subscription.unsubscribe();
      this.subscriptions.delete(id);
    };
  }

  public send(destination: string, body: unknown): void {
    if (!this.client || this.status !== 'connected') {
      console.warn('[WebSocket] Not connected, message dropped');
      return;
    }

    this.client.publish({
      destination,
      body: typeof body === 'string' ? body : JSON.stringify(body),
    });
  }

  public onStatusChange(listener: (status: WebSocketConnectionStatus) => void): () => void {
    this.statusListeners.add(listener);
    return () => {
      this.statusListeners.delete(listener);
    };
  }
}

export const websocketClient = new StompWebSocketClient();
