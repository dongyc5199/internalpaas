/**
 * AI Assistant Panel
 * AI助手面板组件
 *
 * 移植自: src/main/resources/static/js/ai-assistant.js
 * 核心功能: AI聊天、流式输出、设置管理
 */

import { useState, useRef, useEffect, useCallback } from 'react';
import { useTypewriter, useChatHistory } from '../hooks';
import { Markdown } from '../../../shared/components';
import type { AIMessage, AISettings, AIChatSession } from '../types/ai';
import { DEFAULT_AI_SETTINGS, STREAMING_SPEED_MAP } from '../types/ai';
import { streamAIChatSSE, getAvailableModels } from '../services/aiApi';
import type { AIChatRequest } from '../services/aiApi';
import { handleCommand } from '../utils/commandHandler';
import styles from './AIAssistant.module.css';

/**
 * 生成Markdown格式的导出内容
 */
function generateMarkdownExport(session: AIChatSession): string {
  const header = `# AI聊天记录\n\n**会话ID**: ${session.id}\n**标题**: ${session.title}\n**创建时间**: ${new Date(session.createdAt).toLocaleString('zh-CN')}\n**更新时间**: ${new Date(session.updatedAt).toLocaleString('zh-CN')}\n**消息数量**: ${session.messages.length}\n\n---\n\n`;

  const messages = session.messages.map((msg) => {
    const role = msg.role === 'user' ? '👤 用户' : '🤖 AI助手';
    const time = new Date(msg.timestamp).toLocaleTimeString('zh-CN');
    return `## ${role} (${time})\n\n${msg.content}\n\n---\n`;
  }).join('\n');

  return header + messages;
}

/**
 * 生成JSON格式的导出内容
 */
function generateJsonExport(session: AIChatSession): string {
  return JSON.stringify(session, null, 2);
}

/**
 * 生成纯文本格式的导出内容
 */
function generateTextExport(session: AIChatSession): string {
  const header = `AI聊天记录\n${'='.repeat(50)}\n\n会话ID: ${session.id}\n标题: ${session.title}\n创建时间: ${new Date(session.createdAt).toLocaleString('zh-CN')}\n更新时间: ${new Date(session.updatedAt).toLocaleString('zh-CN')}\n消息数量: ${session.messages.length}\n\n${'='.repeat(50)}\n\n`;

  const messages = session.messages.map((msg) => {
    const role = msg.role === 'user' ? '[用户]' : '[AI助手]';
    const time = new Date(msg.timestamp).toLocaleTimeString('zh-CN');
    return `${role} ${time}\n${'-'.repeat(50)}\n${msg.content}\n\n`;
  }).join('\n');

  return header + messages;
}

/**
 * AI助手组件属性
 */
export interface AIAssistantProps {
  /** 是否打开 */
  isOpen: boolean;
  /** 关闭回调 */
  onClose: () => void;
  /** 终端上下文 (可选) */
  terminalContext?: {
    terminalId: string;
    terminalName: string;
  };
  /** 获取终端上下文回调 */
  onGetTerminalContext?: (terminalId: string) => Promise<string>;
}

/**
 * AI Assistant Component
 */
export function AIAssistant({
  isOpen,
  onClose,
  terminalContext,
  onGetTerminalContext,
}: AIAssistantProps): JSX.Element {
  // 会话历史管理
  const history = useChatHistory();

  // 状态管理
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [availableModels, setAvailableModels] = useState<string[]>(['echo']);
  const [settings, setSettings] = useState<AISettings>(() => {
    // 从localStorage加载设置
    const saved = localStorage.getItem('ai.settings');
    return saved ? JSON.parse(saved) : DEFAULT_AI_SETTINGS;
  });

  // Refs
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const streamingMessageIdRef = useRef<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  // 当前会话的消息
  const messages = history.currentSession?.messages || [];

  // 打字机效果
  const typewriter = useTypewriter({
    speed: STREAMING_SPEED_MAP[settings.streamingSpeed],
    onComplete: () => {
      // 打字完成后，更新消息状态
      if (streamingMessageIdRef.current && history.currentSessionId) {
        const updatedMessages = messages.map((msg) =>
          msg.id === streamingMessageIdRef.current
            ? { ...msg, isStreaming: false }
            : msg
        );
        history.updateMessages(history.currentSessionId, updatedMessages);
        streamingMessageIdRef.current = null;
      }
    },
  });

  /**
   * 保存设置到localStorage
   */
  useEffect(() => {
    localStorage.setItem('ai.settings', JSON.stringify(settings));
  }, [settings]);

  /**
   * 获取可用的AI模型列表
   */
  useEffect(() => {
    getAvailableModels().then((models) => {
      setAvailableModels(models);
      // 如果当前没有选中模型，或选中的模型不在列表中，使用第一个模型
      if (!settings.selectedModel || !models.includes(settings.selectedModel)) {
        setSettings((prev) => ({ ...prev, selectedModel: models[0] }));
      }
    });
  }, []);

  /**
   * 自动滚动到底部
   */
  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  /**
   * 获取终端上下文（最后N行输出）
   */
  const getTerminalContext = useCallback(async (terminalId: string): Promise<string> => {
    if (onGetTerminalContext) {
      return await onGetTerminalContext(terminalId);
    }
    console.log('未提供终端上下文获取函数:', terminalId);
    return '';
  }, [onGetTerminalContext]);

  /**
   * 监听打字机文本变化，更新显示
   */
  useEffect(() => {
    if (typewriter.displayedText && streamingMessageIdRef.current && history.currentSessionId) {
      const updatedMessages = messages.map((msg) =>
        msg.id === streamingMessageIdRef.current
          ? { ...msg, content: typewriter.displayedText }
          : msg
      );
      history.updateMessages(history.currentSessionId, updatedMessages);
      scrollToBottom();
    }
  }, [typewriter.displayedText, scrollToBottom, history, messages]);

  /**
   * 导出聊天记录
   */
  const exportChatHistory = useCallback((): void => {
    if (!history.currentSession || history.currentSession.messages.length === 0) {
      alert('当前会话无消息可导出');
      return;
    }

    // 询问导出格式
    const format = prompt('请选择导出格式:\n1. Markdown (.md)\n2. JSON (.json)\n3. 纯文本 (.txt)\n\n输入数字选择:', '1');

    if (!format) return; // 用户取消

    const session = history.currentSession;
    let content = '';
    let filename = '';
    let mimeType = '';

    switch (format) {
      case '1': // Markdown
        content = generateMarkdownExport(session);
        filename = `chat-${session.id}-${new Date().toISOString().slice(0, 10)}.md`;
        mimeType = 'text/markdown';
        break;

      case '2': // JSON
        content = generateJsonExport(session);
        filename = `chat-${session.id}-${new Date().toISOString().slice(0, 10)}.json`;
        mimeType = 'application/json';
        break;

      case '3': // 纯文本
        content = generateTextExport(session);
        filename = `chat-${session.id}-${new Date().toISOString().slice(0, 10)}.txt`;
        mimeType = 'text/plain';
        break;

      default:
        alert('无效的格式选择');
        return;
    }

    // 触发下载
    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }, [history]);

  /**
   * 发送消息
   */
  const sendMessage = useCallback(
    async (content: string): Promise<void> => {
      if (!content.trim() || isLoading) return;

      // 检查是否为快捷命令
      const commandResult = handleCommand(content);

      // 确保有活跃会话
      let sessionId = history.currentSessionId;
      if (!sessionId) {
        sessionId = history.createSession();
      }

      const userMessage: AIMessage = {
        id: `user-${Date.now()}`,
        role: 'user',
        content: content.trim(),
        timestamp: Date.now(),
      };

      history.updateMessages(sessionId, [...messages, userMessage]);
      setInputValue('');

      // 如果是命令，处理命令逻辑
      if (commandResult.isCommand) {
        // 执行命令副作用
        if (commandResult.action === 'clear') {
          // 清空当前会话
          history.createSession();
          return;
        } else if (commandResult.action === 'model-change') {
          // 切换模型
          const modelName = commandResult.payload;
          if (availableModels.includes(modelName)) {
            setSettings((prev) => ({ ...prev, selectedModel: modelName }));
          } else {
            const errorMsg: AIMessage = {
              id: `system-${Date.now()}`,
              role: 'assistant',
              content: `❌ 模型 **${modelName}** 不可用。\n\n可用模型: ${availableModels.join(', ')}`,
              timestamp: Date.now(),
            };
            history.updateMessages(sessionId, [...history.currentSession?.messages || [], errorMsg]);
            return;
          }
        } else if (commandResult.action === 'export') {
          // 导出聊天记录（T093中实现）
          exportChatHistory();
          return;
        } else if (commandResult.action === 'show-context') {
          // 显示终端上下文
          if (terminalContext?.terminalId) {
            const context = await getTerminalContext(terminalContext.terminalId);
            const contextMsg: AIMessage = {
              id: `system-${Date.now()}`,
              role: 'assistant',
              content: `## 📋 终端上下文 (最后100行)\n\n\`\`\`\n${context || '(无输出)'}\n\`\`\``,
              timestamp: Date.now(),
            };
            history.updateMessages(sessionId, [...history.currentSession?.messages || [], contextMsg]);
          } else {
            const errorMsg: AIMessage = {
              id: `system-${Date.now()}`,
              role: 'assistant',
              content: '❌ 当前没有活跃的终端会话',
              timestamp: Date.now(),
            };
            history.updateMessages(sessionId, [...history.currentSession?.messages || [], errorMsg]);
          }
          return;
        }

        // 显示命令响应
        if (commandResult.response) {
          const responseMsg: AIMessage = {
            id: `system-${Date.now()}`,
            role: 'assistant',
            content: commandResult.response,
            timestamp: Date.now(),
          };
          history.updateMessages(sessionId, [...history.currentSession?.messages || [], responseMsg]);
        }

        return;
      }

      setIsLoading(true);

      try {
        // 创建AI助手消息占位符
        const assistantMessage: AIMessage = {
          id: `assistant-${Date.now()}`,
          role: 'assistant',
          content: '',
          timestamp: Date.now(),
          isStreaming: true,
        };

        // 添加占位符到消息列表
        const currentMessages = history.currentSession?.messages || [];
        history.updateMessages(sessionId, [...currentMessages, assistantMessage]);
        streamingMessageIdRef.current = assistantMessage.id;

        // 创建AbortController用于取消请求
        abortControllerRef.current = new AbortController();

        // 构建API请求
        const chatRequest: AIChatRequest = {
          chatId: sessionId,
          sessionId: sessionId,
          message: content,
          model: settings.selectedModel,
          terminalTail: terminalContext?.terminalId
            ? await getTerminalContext(terminalContext.terminalId)
            : undefined,
        };

        // 累积的AI响应文本
        let accumulatedResponse = '';

        // 调用流式API
        await streamAIChatSSE(
          chatRequest,
          (event) => {
            if (event.type === 'chunk') {
              accumulatedResponse += event.data;

              // 根据设置决定是否使用打字机效果
              if (settings.streamingEnabled) {
                typewriter.start(accumulatedResponse);
              } else {
                // 直接更新内容，不使用打字机
                const updatedMessages = (history.currentSession?.messages || []).map((msg) =>
                  msg.id === assistantMessage.id
                    ? { ...msg, content: accumulatedResponse }
                    : msg
                );
                history.updateMessages(sessionId, updatedMessages);
              }
            } else if (event.type === 'done') {
              // 流式输出完成
              if (settings.streamingEnabled) {
                typewriter.start(accumulatedResponse);
              } else {
                const updatedMessages = (history.currentSession?.messages || []).map((msg) =>
                  msg.id === assistantMessage.id
                    ? { ...msg, content: accumulatedResponse, isStreaming: false }
                    : msg
                );
                history.updateMessages(sessionId, updatedMessages);
                streamingMessageIdRef.current = null;
              }
            } else if (event.type === 'error') {
              // 错误处理
              throw new Error(event.data || 'AI请求失败');
            }
          },
          abortControllerRef.current.signal
        );

        setIsLoading(false);
      } catch (error) {
        console.error('AI请求失败:', error);
        const errorMessage: AIMessage = {
          id: `error-${Date.now()}`,
          role: 'assistant',
          content: error instanceof Error ? `抱歉，发生了错误：${error.message}` : '抱歉，发生了错误。请稍后重试。',
          timestamp: Date.now(),
        };
        const currentMessages = history.currentSession?.messages || [];
        history.updateMessages(sessionId, [...currentMessages, errorMessage]);
        setIsLoading(false);
      }
    },
    [
      isLoading,
      settings.streamingEnabled,
      settings.selectedModel,
      typewriter,
      history,
      messages,
      terminalContext,
      getTerminalContext,
      availableModels,
      exportChatHistory,
    ]
  );

  /**
   * 处理表单提交
   */
  const handleSubmit = useCallback(
    (e: React.FormEvent): void => {
      e.preventDefault();
      sendMessage(inputValue);
    },
    [inputValue, sendMessage]
  );

  /**
   * 处理回车键
   */
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLTextAreaElement>): void => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage(inputValue);
      }
    },
    [inputValue, sendMessage]
  );

  /**
   * 新建会话
   */
  const handleNewSession = useCallback((): void => {
    // 取消当前的AI请求
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }

    history.createSession();
    streamingMessageIdRef.current = null;
    typewriter.skipToEnd(); // 停止当前打字机动画
    setIsLoading(false);
  }, [history, typewriter]);

  /**
   * 切换设置面板
   */
  const toggleSettings = useCallback((): void => {
    setShowSettings((prev) => !prev);
  }, []);

  /**
   * 复制消息内容
   */
  const handleCopyMessage = useCallback((content: string): void => {
    navigator.clipboard.writeText(content).then(() => {
      // 可以添加toast提示"已复制"
      console.log('消息已复制到剪贴板');
    }).catch((err) => {
      console.error('复制失败:', err);
      alert('复制失败,请手动复制');
    });
  }, []);

  /**
   * 重试发送消息
   */
  const handleRetryMessage = useCallback((content: string): void => {
    setInputValue(content);
    // 自动聚焦到输入框
    if (inputRef.current) {
      inputRef.current.focus();
    }
  }, []);

  // 面板打开时聚焦输入框
  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isOpen]);

  // 组件卸载时取消所有进行中的请求
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  if (!isOpen) return <></>;

  return (
    <div className={styles.aiAssistant}>
      {/* 顶部工具栏 */}
      <div className={styles.topbar}>
        <div className={styles.topbarLeft}>
          <span className={styles.icon}>🤖</span>
          <span className={styles.title}>AI助手</span>
          {settings.selectedModel && (
            <span className={styles.modelBadge} title="当前AI模型">
              {settings.selectedModel}
            </span>
          )}
          {terminalContext && (
            <span className={styles.context}>{terminalContext.terminalName}</span>
          )}
        </div>
        <div className={styles.topbarRight}>
          <button
            className={styles.iconBtn}
            onClick={handleNewSession}
            title="新建会话"
            disabled={messages.length === 0}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 5v14M5 12h14"/>
            </svg>
          </button>
          <button
            className={styles.iconBtn}
            onClick={() => setShowHistory((prev) => !prev)}
            title="会话历史"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
              <path d="M3 3v5h5"/>
              <path d="M12 7v5l4 2"/>
            </svg>
          </button>
          <button
            className={styles.iconBtn}
            onClick={toggleSettings}
            title="设置"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="3"/>
              <path d="M12 1v6m0 6v6M5.64 5.64l4.24 4.24m5.66 5.66l4.24 4.24M1 12h6m6 0h6M5.64 18.36l4.24-4.24m5.66-5.66l4.24-4.24"/>
            </svg>
          </button>
          <button
            className={styles.iconBtn}
            onClick={onClose}
            title="关闭"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>
      </div>

      {/* 消息区域 */}
      <div className={styles.messagesContainer}>
        {messages.length === 0 ? (
          <div className={styles.emptyState}>
            <div className={styles.emptyIcon}>🤖</div>
            <p>你好！我是AI助手。</p>
            <p>我可以帮你理解终端输出、解释错误、提供命令建议。</p>
          </div>
        ) : (
          <>
            {messages.map((message) => (
              <div
                key={message.id}
                className={`${styles.message} ${styles[`message-${message.role}`]}`}
              >
                <div className={styles.messageAvatar}>
                  {message.role === 'user' ? '👤' : '🤖'}
                </div>
                <div className={styles.messageContent}>
                  <div className={styles.messageText}>
                    {message.role === 'assistant' ? (
                      <>
                        <Markdown content={message.content} />
                        {message.isStreaming && <span className={styles.cursor}>▊</span>}
                      </>
                    ) : (
                      message.content
                    )}
                  </div>
                  <div className={styles.messageFooter}>
                    <div className={styles.messageTime}>
                      {new Date(message.timestamp).toLocaleTimeString()}
                    </div>
                    {!message.isStreaming && (
                      <div className={styles.messageActions}>
                        <button
                          className={styles.messageActionBtn}
                          onClick={(): void => handleCopyMessage(message.content)}
                          title="复制消息"
                        >
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
                          </svg>
                          复制
                        </button>
                        {message.role === 'user' && (
                          <button
                            className={styles.messageActionBtn}
                            onClick={(): void => handleRetryMessage(message.content)}
                            title="重新发送"
                          >
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                              <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/>
                              <path d="M21 3v5h-5"/>
                              <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/>
                              <path d="M3 21v-5h5"/>
                            </svg>
                            重试
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
            <div ref={messagesEndRef} />
          </>
        )}
      </div>

      {/* 会话历史抽屉 */}
      {showHistory && (
        <div className={styles.historyDrawer}>
          <div className={styles.historyHeader}>
            <h3 className={styles.historyTitle}>📜 会话历史</h3>
            <button
              className={styles.clearAllBtn}
              onClick={() => {
                if (window.confirm('确定要清空所有会话历史吗？')) {
                  history.clearAllSessions();
                }
              }}
              disabled={history.sessions.length === 0}
            >
              清空
            </button>
          </div>
          <div className={styles.historyList}>
            {history.sessions.length === 0 ? (
              <div className={styles.emptyHistory}>暂无历史会话</div>
            ) : (
              history.sessions.map((session) => (
                <div
                  key={session.id}
                  className={`${styles.historyItem} ${
                    session.id === history.currentSessionId ? styles.historyItemActive : ''
                  }`}
                  onClick={() => history.switchSession(session.id)}
                >
                  <div className={styles.historyItemContent}>
                    <div className={styles.historyItemTitle}>{session.title}</div>
                    <div className={styles.historyItemMeta}>
                      {new Date(session.updatedAt).toLocaleString('zh-CN', {
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                      })} · {session.messages.length} 条消息
                    </div>
                  </div>
                  <button
                    className={styles.deleteSessionBtn}
                    onClick={(e) => {
                      e.stopPropagation();
                      if (window.confirm('确定要删除此会话吗？')) {
                        history.deleteSession(session.id);
                      }
                    }}
                    title="删除会话"
                  >
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M18 6L6 18M6 6l12 12"/>
                    </svg>
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* 设置抽屉 */}
      {showSettings && (
        <div className={styles.settingsDrawer}>
          <h3 className={styles.settingsTitle}>⚙️ 聊天设置</h3>

          {/* AI模型选择 */}
          <div className={styles.settingItem}>
            <label htmlFor="aiModel" className={styles.settingLabelBlock}>
              AI 模型
            </label>
            <select
              id="aiModel"
              className={styles.settingSelect}
              value={settings.selectedModel || availableModels[0]}
              onChange={(e) =>
                setSettings({
                  ...settings,
                  selectedModel: e.target.value,
                })
              }
            >
              {availableModels.map((model) => (
                <option key={model} value={model}>
                  {model}
                </option>
              ))}
            </select>
            <p className={styles.settingDesc}>选择AI对话模型</p>
          </div>

          {/* 打字机效果 */}
          <div className={styles.settingItem}>
            <label className={styles.settingLabel}>
              <input
                type="checkbox"
                checked={settings.streamingEnabled}
                onChange={(e) =>
                  setSettings({ ...settings, streamingEnabled: e.target.checked })
                }
              />
              <span>启用打字机效果</span>
            </label>
            <p className={styles.settingDesc}>AI 回复将逐字符流式显示</p>
          </div>

          {/* 流式速度 */}
          <div className={styles.settingItem}>
            <label htmlFor="streamSpeed" className={styles.settingLabelBlock}>
              流式速度
            </label>
            <select
              id="streamSpeed"
              className={styles.settingSelect}
              value={settings.streamingSpeed}
              onChange={(e) =>
                setSettings({
                  ...settings,
                  streamingSpeed: e.target.value as AISettings['streamingSpeed'],
                })
              }
              disabled={!settings.streamingEnabled}
            >
              <option value="slow">慢速 (30 字符/秒)</option>
              <option value="normal">正常 (50 字符/秒)</option>
              <option value="fast">快速 (80 字符/秒)</option>
            </select>
          </div>
        </div>
      )}

      {/* 输入区域 */}
      <form className={styles.inputContainer} onSubmit={handleSubmit}>
        <div className={styles.inputWrapper}>
          <textarea
            ref={inputRef}
            className={styles.input}
            placeholder="输入消息或命令 (/ 开头为命令，输入 /help 查看帮助)"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            rows={2}
            disabled={isLoading}
          />
          {inputValue.startsWith('/') && (
            <div className={styles.commandHint}>
              💡 快捷命令模式 - 输入 <code>/help</code> 查看所有命令
            </div>
          )}
        </div>
        <button
          type="submit"
          className={styles.sendBtn}
          disabled={!inputValue.trim() || isLoading}
        >
          {isLoading ? '...' : '发送'}
        </button>
      </form>
    </div>
  );
}
