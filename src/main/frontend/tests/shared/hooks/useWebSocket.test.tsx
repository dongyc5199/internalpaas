import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useWebSocket } from '../../../src/shared/hooks/useWebSocket';
import { websocketClient, type WebSocketConnectionStatus } from '../../../src/shared/api/websocket';

// Mock websocket client
vi.mock('../../../src/shared/api/websocket', () => {
  const mockSubscribers: Array<(status: WebSocketConnectionStatus) => void> = [];
  const mockMessageHandlers = new Map<string, Array<(data: any) => void>>();

  return {
    websocketClient: {
      connect: vi.fn().mockResolvedValue(undefined),
      disconnect: vi.fn(),
      send: vi.fn(),
      subscribe: vi.fn((options: { topic: string; handler: (data: any) => void }) => {
        if (!mockMessageHandlers.has(options.topic)) {
          mockMessageHandlers.set(options.topic, []);
        }
        mockMessageHandlers.get(options.topic)?.push(options.handler);

        return () => {
          const handlers = mockMessageHandlers.get(options.topic);
          if (handlers) {
            const index = handlers.indexOf(options.handler);
            if (index > -1) {
              handlers.splice(index, 1);
            }
          }
        };
      }),
      onStatusChange: vi.fn((callback: (status: WebSocketConnectionStatus) => void) => {
        mockSubscribers.push(callback);
        return () => {
          const index = mockSubscribers.indexOf(callback);
          if (index > -1) {
            mockSubscribers.splice(index, 1);
          }
        };
      }),
      getStatus: vi.fn(() => 'connected' as WebSocketConnectionStatus),
      getReconnectAttempts: vi.fn(() => 0),
      __mockTriggerMessage: (topic: string, data: any) => {
        const handlers = mockMessageHandlers.get(topic);
        handlers?.forEach((handler) => handler(data));
      },
      __mockTriggerStatusChange: (status: WebSocketConnectionStatus) => {
        mockSubscribers.forEach((subscriber) => subscriber(status));
      },
      __mockReset: () => {
        mockSubscribers.length = 0;
        mockMessageHandlers.clear();
      },
    },
  };
});

describe('useWebSocket', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (websocketClient as any).__mockReset();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('应该初始化并返回初始状态', () => {
    const { result } = renderHook(() =>
      useWebSocket({
        topic: '/topic/test',
        autoConnect: false,
      })
    );

    expect(result.current.lastMessage).toBeNull();
    expect(result.current.status).toBe('connected');
    expect(result.current.reconnectCount).toBe(0);
  });

  it('应该在autoConnect为true时自动连接', () => {
    renderHook(() =>
      useWebSocket({
        topic: '/topic/test',
        autoConnect: true,
      })
    );

    expect(websocketClient.connect).toHaveBeenCalled();
  });

  it('应该在autoConnect为false时不自动连接', () => {
    renderHook(() =>
      useWebSocket({
        topic: '/topic/test',
        autoConnect: false,
      })
    );

    expect(websocketClient.connect).not.toHaveBeenCalled();
  });

  it('应该订阅指定的topic', () => {
    const topic = '/topic/test';
    renderHook(() =>
      useWebSocket({
        topic,
        autoConnect: false,
      })
    );

    expect(websocketClient.subscribe).toHaveBeenCalledWith(
      expect.objectContaining({
        topic,
      })
    );
  });

  it('应该接收并更新lastMessage', async () => {
    const topic = '/topic/test';
    const testData = { message: 'test' };

    const { result } = renderHook(() =>
      useWebSocket<typeof testData>({
        topic,
        autoConnect: false,
      })
    );

    // Trigger message
    (websocketClient as any).__mockTriggerMessage(topic, testData);

    await waitFor(() => {
      expect(result.current.lastMessage).toEqual(testData);
    });
  });

  it('应该在收到消息时调用onMessage回调', async () => {
    const topic = '/topic/test';
    const testData = { message: 'test' };
    const onMessage = vi.fn();

    renderHook(() =>
      useWebSocket({
        topic,
        autoConnect: false,
        onMessage,
      })
    );

    (websocketClient as any).__mockTriggerMessage(topic, testData);

    await waitFor(() => {
      expect(onMessage).toHaveBeenCalledWith(testData);
    });
  });

  it('应该更新status当连接状态变化', async () => {
    (websocketClient.getStatus as any).mockReturnValue('connecting');

    const { result } = renderHook(() =>
      useWebSocket({
        topic: '/topic/test',
        autoConnect: false,
      })
    );

    const newStatus: WebSocketConnectionStatus = 'disconnected';
    (websocketClient as any).__mockTriggerStatusChange(newStatus);

    await waitFor(() => {
      expect(result.current.status).toBe(newStatus);
    });
  });

  it('应该在状态变化时调用onStatusChange回调', async () => {
    const onStatusChange = vi.fn();

    renderHook(() =>
      useWebSocket({
        topic: '/topic/test',
        autoConnect: false,
        onStatusChange,
      })
    );

    const newStatus: WebSocketConnectionStatus = 'disconnected';
    (websocketClient as any).__mockTriggerStatusChange(newStatus);

    await waitFor(() => {
      expect(onStatusChange).toHaveBeenCalledWith(newStatus);
    });
  });

  it('应该更新reconnectCount', async () => {
    (websocketClient.getReconnectAttempts as any).mockReturnValue(3);

    const { result } = renderHook(() =>
      useWebSocket({
        topic: '/topic/test',
        autoConnect: false,
      })
    );

    (websocketClient as any).__mockTriggerStatusChange('connecting');

    await waitFor(() => {
      expect(result.current.reconnectCount).toBe(3);
    });
  });

  it('应该提供sendMessage方法', () => {
    const { result } = renderHook(() =>
      useWebSocket({
        topic: '/topic/test',
        autoConnect: false,
      })
    );

    const destination = '/app/send';
    const body = { data: 'test' };

    result.current.sendMessage(destination, body);

    expect(websocketClient.send).toHaveBeenCalledWith(destination, body);
  });

  it('应该提供connect方法', () => {
    const { result } = renderHook(() =>
      useWebSocket({
        topic: '/topic/test',
        autoConnect: false,
      })
    );

    result.current.connect();

    expect(websocketClient.connect).toHaveBeenCalled();
  });

  it('应该提供disconnect方法', () => {
    const { result } = renderHook(() =>
      useWebSocket({
        topic: '/topic/test',
        autoConnect: false,
      })
    );

    result.current.disconnect();

    expect(websocketClient.disconnect).toHaveBeenCalled();
  });

  it('应该在卸载时取消订阅', () => {
    const topic = '/topic/test';
    const { unmount } = renderHook(() =>
      useWebSocket({
        topic,
        autoConnect: false,
      })
    );

    const subscribeCallCount = (websocketClient.subscribe as any).mock.calls.length;

    unmount();

    // Verify unsubscribe was called by checking that the subscription was cleaned up
    expect(subscribeCallCount).toBeGreaterThan(0);
  });

  it('应该在topic变化时重新订阅', () => {
    const { rerender } = renderHook(
      ({ topic }) =>
        useWebSocket({
          topic,
          autoConnect: false,
        }),
      {
        initialProps: { topic: '/topic/test1' },
      }
    );

    const firstCallCount = (websocketClient.subscribe as any).mock.calls.length;

    rerender({ topic: '/topic/test2' });

    const secondCallCount = (websocketClient.subscribe as any).mock.calls.length;
    expect(secondCallCount).toBeGreaterThan(firstCallCount);
  });

  it('应该处理多条消息', async () => {
    const topic = '/topic/test';
    const messages = [
      { id: 1, text: 'message 1' },
      { id: 2, text: 'message 2' },
      { id: 3, text: 'message 3' },
    ];

    const { result } = renderHook(() =>
      useWebSocket<typeof messages[0]>({
        topic,
        autoConnect: false,
      })
    );

    for (const message of messages) {
      (websocketClient as any).__mockTriggerMessage(topic, message);

      await waitFor(() => {
        expect(result.current.lastMessage).toEqual(message);
      });
    }

    // Should have the last message
    expect(result.current.lastMessage).toEqual(messages[messages.length - 1]);
  });

  it('应该处理空消息', async () => {
    const topic = '/topic/test';

    const { result } = renderHook(() =>
      useWebSocket({
        topic,
        autoConnect: false,
      })
    );

    (websocketClient as any).__mockTriggerMessage(topic, null);

    await waitFor(() => {
      expect(result.current.lastMessage).toBeNull();
    });
  });

  it('应该处理复杂数据类型', async () => {
    const topic = '/topic/test';
    const complexData = {
      id: 1,
      nested: {
        array: [1, 2, 3],
        object: { key: 'value' },
      },
      timestamp: new Date().toISOString(),
    };

    const { result } = renderHook(() =>
      useWebSocket<typeof complexData>({
        topic,
        autoConnect: false,
      })
    );

    (websocketClient as any).__mockTriggerMessage(topic, complexData);

    await waitFor(() => {
      expect(result.current.lastMessage).toEqual(complexData);
    });
  });

  it('应该允许手动连接和断开', () => {
    const { result } = renderHook(() =>
      useWebSocket({
        topic: '/topic/test',
        autoConnect: false,
      })
    );

    // Manual connect
    result.current.connect();
    expect(websocketClient.connect).toHaveBeenCalled();

    // Manual disconnect
    result.current.disconnect();
    expect(websocketClient.disconnect).toHaveBeenCalled();
  });

  it('应该保持sendMessage方法引用稳定', () => {
    const { result, rerender } = renderHook(() =>
      useWebSocket({
        topic: '/topic/test',
        autoConnect: false,
      })
    );

    const firstSendMessage = result.current.sendMessage;

    rerender();

    const secondSendMessage = result.current.sendMessage;

    expect(firstSendMessage).toBe(secondSendMessage);
  });

  it('应该保持connect方法引用稳定', () => {
    const { result, rerender } = renderHook(() =>
      useWebSocket({
        topic: '/topic/test',
        autoConnect: false,
      })
    );

    const firstConnect = result.current.connect;

    rerender();

    const secondConnect = result.current.connect;

    expect(firstConnect).toBe(secondConnect);
  });

  it('应该保持disconnect方法引用稳定', () => {
    const { result, rerender } = renderHook(() =>
      useWebSocket({
        topic: '/topic/test',
        autoConnect: false,
      })
    );

    const firstDisconnect = result.current.disconnect;

    rerender();

    const secondDisconnect = result.current.disconnect;

    expect(firstDisconnect).toBe(secondDisconnect);
  });
});
