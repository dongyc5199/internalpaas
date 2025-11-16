/**
 * AI Chat API Service
 * 提供AI聊天API调用（Server-Sent Events 流式输出）
 */

/**
 * AI模型配置
 */
export interface AIModelConfig {
  /** 提供商 */
  provider: string;
  /** 模型ID */
  model: string;
  /** 模型显示名称 */
  displayName?: string;
  /** 是否可用 */
  available?: boolean;
}

export interface AIChatRequest {
  /** 聊天ID */
  chatId?: string;
  /** 会话ID */
  sessionId?: string;
  /** 模型名称 */
  model?: string;
  /** 用户消息 */
  message: string;
  /** 终端上下文（最后N行输出） */
  terminalTail?: string;
}

export interface AIChatStreamEvent {
  /** 事件类型 */
  type: 'start' | 'chunk' | 'done' | 'error';
  /** 事件数据 */
  data: string;
}

/**
 * AI聊天流式API调用
 *
 * @param request - 聊天请求
 * @param onEvent - 事件回调
 * @returns 取消函数
 */
export function streamAIChat(
  request: AIChatRequest,
  onEvent: (event: AIChatStreamEvent) => void
): () => void {
  const eventSource = new EventSource(buildStreamUrl(request));

  let isCancelled = false;

  // 监听不同类型的事件
  eventSource.addEventListener('start', (e: MessageEvent) => {
    if (isCancelled) return;
    onEvent({ type: 'start', data: e.data });
  });

  eventSource.addEventListener('chunk', (e: MessageEvent) => {
    if (isCancelled) return;
    onEvent({ type: 'chunk', data: e.data });
  });

  eventSource.addEventListener('done', (e: MessageEvent) => {
    if (isCancelled) return;
    onEvent({ type: 'done', data: e.data });
    eventSource.close();
  });

  eventSource.addEventListener('error', (e: MessageEvent) => {
    if (isCancelled) return;
    const errorMsg = e.data || 'AI聊天出错';
    onEvent({ type: 'error', data: errorMsg });
    eventSource.close();
  });

  // 连接错误处理
  eventSource.onerror = (error) => {
    if (isCancelled) return;
    console.error('AI聊天流式连接错误:', error);
    onEvent({
      type: 'error',
      data: '连接AI服务失败，请检查网络或稍后重试'
    });
    eventSource.close();
  };

  // 返回取消函数
  return () => {
    isCancelled = true;
    eventSource.close();
  };
}

/**
 * 构建流式请求URL（使用POST body需要不同的方式）
 * 注意：EventSource只支持GET，所以我们需要改用fetch + ReadableStream
 *
 * @deprecated 此函数已废弃，请使用 streamAIChatSSE
 */
function buildStreamUrl(_request: AIChatRequest): string {
  // 这个函数实际上不会被使用，因为EventSource不支持POST
  // 我们需要使用fetch API代替
  return '/ai/chat/stream';
}

/**
 * 使用fetch实现流式聊天（支持POST）
 */
export async function streamAIChatWithFetch(
  request: AIChatRequest,
  onEvent: (event: AIChatStreamEvent) => void,
  signal?: AbortSignal
): Promise<void> {
  try {
    const response = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
      },
      body: JSON.stringify(request),
      signal,
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    if (!response.body) {
      throw new Error('响应body为空');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();

      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');

      // 保留最后一个不完整的行
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (line.startsWith('event:')) {
          // 跳过，下一行是data
          continue;
        }

        if (line.startsWith('data:')) {
          const data = line.slice(5).trim();

          // 解析事件类型（从之前的event:行）
          // 这里简化处理，直接判断数据内容
          if (data === '[STREAM_START]') {
            onEvent({ type: 'start', data: '' });
          } else if (data === '[STREAM_END]') {
            onEvent({ type: 'done', data: '' });
          } else if (data.startsWith('[ERROR]')) {
            onEvent({ type: 'error', data: data.slice(7) });
          } else {
            onEvent({ type: 'chunk', data });
          }
        }
      }
    }
  } catch (error) {
    if (error instanceof Error) {
      if (error.name === 'AbortError') {
        console.log('AI聊天已取消');
        return;
      }
      onEvent({ type: 'error', data: error.message });
    } else {
      onEvent({ type: 'error', data: '未知错误' });
    }
  }
}

/**
 * 获取可用的AI模型列表
 */
export async function getAvailableModels(): Promise<string[]> {
  try {
    const response = await fetch('/api/ai/models');
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('获取AI模型列表失败:', error);
    return ['echo']; // 返回默认模型
  }
}

/**
 * 获取详细的AI模型列表
 */
export async function getDetailedModels(): Promise<AIModelConfig[]> {
  try {
    const response = await fetch('/api/ai/models/detailed');
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('获取详细AI模型列表失败:', error);
    return [];
  }
}

/**
 * 更好的SSE解析实现
 */
export async function streamAIChatSSE(
  request: AIChatRequest,
  onEvent: (event: AIChatStreamEvent) => void,
  signal?: AbortSignal
): Promise<void> {
  try {
    const response = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
      },
      body: JSON.stringify(request),
      signal,
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    if (!response.body) {
      throw new Error('响应body为空');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let currentEventType: string | null = null;

    while (true) {
      const { done, value } = await reader.read();

      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');

      // 保留最后一个可能不完整的行
      buffer = lines.pop() || '';

      for (const line of lines) {
        const trimmedLine = line.trim();

        if (trimmedLine === '') {
          // 空行表示事件结束
          currentEventType = null;
          continue;
        }

        if (trimmedLine.startsWith('event:')) {
          currentEventType = trimmedLine.slice(6).trim();
        } else if (trimmedLine.startsWith('data:')) {
          const data = trimmedLine.slice(5).trim();

          // 根据事件类型分发
          if (currentEventType === 'start') {
            onEvent({ type: 'start', data });
          } else if (currentEventType === 'chunk') {
            onEvent({ type: 'chunk', data });
          } else if (currentEventType === 'done') {
            onEvent({ type: 'done', data });
          } else if (currentEventType === 'error') {
            onEvent({ type: 'error', data });
          } else {
            // 默认当作chunk
            onEvent({ type: 'chunk', data });
          }
        }
      }
    }
  } catch (error) {
    if (error instanceof Error) {
      if (error.name === 'AbortError') {
        console.log('AI聊天已取消');
        return;
      }
      onEvent({ type: 'error', data: error.message });
    } else {
      onEvent({ type: 'error', data: '未知错误' });
    }
  }
}
