/**
 * useChatHistory Hook
 * AI聊天会话历史管理
 */

import { useState, useCallback, useEffect } from 'react';
import type { AIChatSession, AIMessage } from '../types/ai';

const STORAGE_KEY = 'ai.chat.history';
const MAX_SESSIONS = 50; // 最多保存50个会话

/**
 * useChatHistory Hook返回值
 */
export interface UseChatHistoryReturn {
  /** 所有会话 */
  sessions: AIChatSession[];
  /** 当前会话ID */
  currentSessionId: string | null;
  /** 当前会话 */
  currentSession: AIChatSession | null;
  /** 创建新会话 */
  createSession: () => string;
  /** 切换到会话 */
  switchSession: (sessionId: string) => void;
  /** 删除会话 */
  deleteSession: (sessionId: string) => void;
  /** 更新会话消息 */
  updateMessages: (sessionId: string, messages: AIMessage[]) => void;
  /** 更新会话标题 */
  updateTitle: (sessionId: string, title: string) => void;
  /** 清空所有会话 */
  clearAllSessions: () => void;
}

/**
 * 从localStorage加载会话
 */
function loadSessions(): AIChatSession[] {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      return JSON.parse(saved);
    }
  } catch (error) {
    console.error('Failed to load chat history:', error);
  }
  return [];
}

/**
 * 保存会话到localStorage
 */
function saveSessions(sessions: AIChatSession[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
  } catch (error) {
    console.error('Failed to save chat history:', error);
  }
}

/**
 * 生成会话标题
 */
function generateTitle(messages: AIMessage[]): string {
  const firstUserMessage = messages.find((m) => m.role === 'user');
  if (firstUserMessage) {
    // 取前30个字符作为标题
    const content = firstUserMessage.content.trim();
    return content.length > 30 ? content.substring(0, 30) + '...' : content;
  }
  return '新会话';
}

/**
 * useChatHistory Hook
 *
 * 使用示例:
 * ```tsx
 * const history = useChatHistory();
 *
 * // 创建新会话
 * const sessionId = history.createSession();
 *
 * // 更新消息
 * history.updateMessages(sessionId, [...messages, newMessage]);
 *
 * // 切换会话
 * history.switchSession(sessionId);
 * ```
 */
export function useChatHistory(): UseChatHistoryReturn {
  const [sessions, setSessions] = useState<AIChatSession[]>(() => loadSessions());
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);

  // 保存到localStorage
  useEffect(() => {
    saveSessions(sessions);
  }, [sessions]);

  /**
   * 创建新会话
   */
  const createSession = useCallback((): string => {
    const newSession: AIChatSession = {
      id: `session-${Date.now()}`,
      title: '新会话',
      messages: [],
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };

    setSessions((prev) => {
      const updated = [newSession, ...prev];
      // 限制最大会话数
      return updated.slice(0, MAX_SESSIONS);
    });

    setCurrentSessionId(newSession.id);
    return newSession.id;
  }, []);

  /**
   * 切换到会话
   */
  const switchSession = useCallback((sessionId: string): void => {
    setCurrentSessionId(sessionId);
  }, []);

  /**
   * 删除会话
   */
  const deleteSession = useCallback((sessionId: string): void => {
    setSessions((prev) => {
      const remaining = prev.filter((s) => s.id !== sessionId);

      // 如果删除的是当前会话，切换到第一个
      setCurrentSessionId((current) => {
        if (current === sessionId) {
          return remaining.length > 0 ? remaining[0]?.id ?? null : null;
        }
        return current;
      });

      return remaining;
    });
  }, []);

  /**
   * 更新会话消息
   */
  const updateMessages = useCallback(
    (sessionId: string, messages: AIMessage[]): void => {
      setSessions((prev) =>
        prev.map((session) => {
          if (session.id === sessionId) {
            return {
              ...session,
              messages,
              title: messages.length > 0 ? generateTitle(messages) : session.title,
              updatedAt: Date.now(),
            };
          }
          return session;
        })
      );
    },
    []
  );

  /**
   * 更新会话标题
   */
  const updateTitle = useCallback(
    (sessionId: string, title: string): void => {
      setSessions((prev) =>
        prev.map((session) =>
          session.id === sessionId ? { ...session, title, updatedAt: Date.now() } : session
        )
      );
    },
    []
  );

  /**
   * 清空所有会话
   */
  const clearAllSessions = useCallback((): void => {
    setSessions([]);
    setCurrentSessionId(null);
  }, []);

  // 获取当前会话
  const currentSession = currentSessionId
    ? sessions.find((s) => s.id === currentSessionId) || null
    : null;

  return {
    sessions,
    currentSessionId,
    currentSession,
    createSession,
    switchSession,
    deleteSession,
    updateMessages,
    updateTitle,
    clearAllSessions,
  };
}
