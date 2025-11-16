/**
 * AI Assistant Types
 * AI助手相关类型定义
 */

/**
 * AI模式
 */
export type AIMode = 'ask' | 'agent';

/**
 * AI消息角色
 */
export type AIMessageRole = 'user' | 'assistant' | 'system';

/**
 * AI消息
 */
export interface AIMessage {
  id: string;
  role: AIMessageRole;
  content: string;
  timestamp: number;
  isStreaming?: boolean;
}

/**
 * AI聊天会话
 */
export interface AIChatSession {
  id: string;
  title: string;
  messages: AIMessage[];
  createdAt: number;
  updatedAt: number;
}

/**
 * AI设置
 */
export interface AISettings {
  /** 是否启用流式输出 */
  streamingEnabled: boolean;
  /** 流式速度 (字符/秒) */
  streamingSpeed: 'slow' | 'normal' | 'fast';
  /** 选中的终端会话上下文 */
  contextSessions: string[];
  /** 选中的AI模型 */
  selectedModel?: string;
}

/**
 * 流式速度映射
 */
export const STREAMING_SPEED_MAP: Record<AISettings['streamingSpeed'], number> = {
  slow: 30,
  normal: 50,
  fast: 80,
};

/**
 * 默认AI设置
 */
export const DEFAULT_AI_SETTINGS: AISettings = {
  streamingEnabled: true,
  streamingSpeed: 'normal',
  contextSessions: [],
};
