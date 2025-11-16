/**
 * Terminal Manager Page
 * SSH终端管理器 - 多标签页支持
 *
 * 移植自: src/main/resources/templates/terminal/manager.html
 * 核心功能: 多终端标签管理、服务器选择、会话管理
 */

import { useState, useRef, useCallback, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Terminal, TerminalHandle, AIAssistant } from '../components';
import { useTerminal } from '../hooks/useTerminal';
import { terminalApi } from '../../../shared/api/terminalApi';
import styles from './TerminalManager.module.css';

/**
 * 终端标签页数据
 */
interface TerminalTab {
  id: string;
  serverId: number;
  serverName: string;
  status: 'disconnected' | 'connecting' | 'connected' | 'error';
  createdAt: number;
}

/**
 * 终端会话数据
 */
interface TerminalSessionData {
  terminalRef: React.RefObject<TerminalHandle>;
  useTerminalHook: ReturnType<typeof useTerminal>;
}

/**
 * Terminal Manager Component
 */
export function TerminalManager(): JSX.Element {
  // 服务器列表
  const { data: servers = [], isLoading: isLoadingServers } = useQuery({
    queryKey: ['terminal-servers'],
    queryFn: terminalApi.getServers,
  });

  // 状态管理
  const [tabs, setTabs] = useState<TerminalTab[]>([]);
  const [activeTabId, setActiveTabId] = useState<string | null>(null);
  const [selectedServerId, setSelectedServerId] = useState<number>(0);
  const [isAIAssistantOpen, setIsAIAssistantOpen] = useState(false);

  // 终端会话管理 (类似manager.html中的terminals Map)
  const sessionsRef = useRef<Map<string, TerminalSessionData>>(new Map());
  const nextTerminalIdRef = useRef(1);

  /**
   * 生成唯一终端ID
   */
  const generateTerminalId = useCallback((): string => {
    const id = `terminal-${nextTerminalIdRef.current}`;
    nextTerminalIdRef.current += 1;
    return id;
  }, []);

  /**
   * 创建新终端 (移植自manager.html line 591-703)
   */
  const createNewTerminal = useCallback((): void => {
    if (!selectedServerId) {
      alert('请先选择一个服务器');
      return;
    }

    const server = servers.find((s) => s.id === selectedServerId);
    if (!server) {
      alert('服务器不存在');
      return;
    }

    // 生成终端ID
    const terminalId = generateTerminalId();

    // 创建标签页
    const newTab: TerminalTab = {
      id: terminalId,
      serverId: server.id,
      serverName: server.name,
      status: 'connecting',
      createdAt: Date.now(),
    };

    setTabs((prev) => [...prev, newTab]);
    setActiveTabId(terminalId);

    console.log(`Created terminal ${terminalId} for server ${server.name}`);
  }, [selectedServerId, servers, generateTerminalId]);

  /**
   * 切换终端标签 (移植自manager.html line 829-873)
   */
  const switchToTerminal = useCallback((terminalId: string): void => {
    setActiveTabId(terminalId);

    // 延迟聚焦终端 (line 846-871)
    setTimeout(() => {
      const session = sessionsRef.current.get(terminalId);
      if (session?.terminalRef.current) {
        session.terminalRef.current.fit();
        session.terminalRef.current.focus();
        console.log('Terminal focused:', terminalId);
      }
    }, 100);
  }, []);

  /**
   * 关闭终端 (移植自manager.html line 878-917)
   */
  const closeTerminal = useCallback(
    (terminalId: string): void => {
      const session = sessionsRef.current.get(terminalId);
      if (session) {
        // 断开WebSocket连接
        session.useTerminalHook.disconnect();
        // 移除会话
        sessionsRef.current.delete(terminalId);
      }

      // 移除标签页
      setTabs((prev) => prev.filter((tab) => tab.id !== terminalId));

      // 如果关闭的是当前活跃终端，切换到其他终端
      if (activeTabId === terminalId) {
        setTabs((currentTabs) => {
          const remainingTabs = currentTabs.filter(
            (tab) => tab.id !== terminalId
          );
          const firstTab = remainingTabs[0];
          if (firstTab) {
            setActiveTabId(firstTab.id);
          } else {
            setActiveTabId(null);
          }
          return currentTabs;
        });
      }

      console.log(`Closed terminal ${terminalId}`);
    },
    [activeTabId]
  );

  /**
   * 复制当前会话 (移植自manager.html line 953-961)
   */
  const duplicateCurrentSession = useCallback((): void => {
    if (!activeTabId) return;

    const activeTab = tabs.find((tab) => tab.id === activeTabId);
    if (activeTab) {
      setSelectedServerId(activeTab.serverId);
      setTimeout(() => createNewTerminal(), 100);
    }
  }, [activeTabId, tabs, createNewTerminal]);

  /**
   * 重新连接当前会话 (移植自manager.html line 963-979)
   */
  const reconnectCurrentSession = useCallback((): void => {
    if (!activeTabId) return;

    const session = sessionsRef.current.get(activeTabId);
    if (session) {
      // 更新标签状态
      setTabs((prev) =>
        prev.map((tab) =>
          tab.id === activeTabId ? { ...tab, status: 'connecting' } : tab
        )
      );

      // 调用重连
      session.useTerminalHook.reconnect();
      console.log(`Reconnecting terminal ${activeTabId}`);
    }
  }, [activeTabId]);

  /**
   * 更新标签状态
   */
  const updateTabStatus = useCallback(
    (
      terminalId: string,
      status: 'disconnected' | 'connecting' | 'connected' | 'error'
    ): void => {
      setTabs((prev) =>
        prev.map((tab) => (tab.id === terminalId ? { ...tab, status } : tab))
      );
    },
    []
  );

  /**
   * 切换AI助手 (移植自ai-assistant.js - 快捷键 Ctrl+Shift+A)
   */
  const toggleAIAssistant = useCallback((): void => {
    setIsAIAssistantOpen((prev) => !prev);
  }, []);

  /**
   * 获取终端上下文（最后N行输出）
   */
  const getTerminalContext = useCallback(async (terminalId: string): Promise<string> => {
    const session = sessionsRef.current.get(terminalId);
    if (!session || !session.terminalRef.current) {
      console.warn(`无法获取终端${terminalId}的上下文: 会话不存在或终端未初始化`);
      return '';
    }

    const xterm = session.terminalRef.current.getXTerm();
    if (!xterm) {
      console.warn(`无法获取终端${terminalId}的xterm实例`);
      return '';
    }

    // 获取终端缓冲区
    const buffer = xterm.buffer.active;
    const totalLines = buffer.length;
    const contextLines = 100; // 获取最后100行
    const startLine = Math.max(0, totalLines - contextLines);

    const lines: string[] = [];
    for (let i = startLine; i < totalLines; i++) {
      const line = buffer.getLine(i);
      if (line) {
        lines.push(line.translateToString(true)); // true = trim trailing whitespace
      }
    }

    const context = lines.join('\n');
    console.log(`获取终端${terminalId}上下文: ${lines.length}行, ${context.length}字符`);
    return context;
  }, []);

  /**
   * 全局快捷键处理
   */
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent): void => {
      // Ctrl+Shift+A - 切换AI助手
      if (e.ctrlKey && e.shiftKey && e.key === 'A') {
        e.preventDefault();
        toggleAIAssistant();
      }
      // Ctrl+Shift+T - 新建终端
      else if (e.ctrlKey && e.shiftKey && e.key === 'T') {
        e.preventDefault();
        if (selectedServerId) {
          createNewTerminal();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [toggleAIAssistant, selectedServerId, createNewTerminal]);

  /**
   * 渲染欢迎消息
   */
  const renderWelcome = (): JSX.Element => {
    return (
      <div className={styles.welcomeMessage}>
        <div className={styles.welcomeIcon}>🖥️</div>
        <h2>SSH 终端管理器</h2>
        <p>选择一个服务器并点击"新建终端"开始</p>
        <div className={styles.welcomeStats}>
          <div className={styles.statItem}>
            <span className={styles.statLabel}>可用服务器</span>
            <span className={styles.statValue}>{servers.length}</span>
          </div>
          <div className={styles.statItem}>
            <span className={styles.statLabel}>活跃会话</span>
            <span className={styles.statValue}>{tabs.length}</span>
          </div>
        </div>
      </div>
    );
  };

  /**
   * 渲染单个终端标签内容
   */
  const renderTerminalContent = (tab: TerminalTab): JSX.Element => {
    return (
      <TerminalContent
        key={tab.id}
        tab={tab}
        isActive={activeTabId === tab.id}
        onStatusChange={(status) => updateTabStatus(tab.id, status)}
        sessionsRef={sessionsRef}
      />
    );
  };

  return (
    <div className={styles.terminalManager}>
      {/* 顶部工具栏 */}
      <div className={styles.toolbar}>
        <div className={styles.toolbarLeft}>
          <select
            className={styles.serverSelector}
            value={selectedServerId}
            onChange={(e) => setSelectedServerId(Number(e.target.value))}
            disabled={isLoadingServers}
          >
            <option value={0}>选择服务器...</option>
            {servers.map((server) => (
              <option key={server.id} value={server.id}>
                {server.name} ({server.host})
              </option>
            ))}
          </select>

          <button
            className={styles.toolbarBtn}
            onClick={createNewTerminal}
            disabled={!selectedServerId}
            title="新建终端 (Ctrl+Shift+T)"
          >
            <i className="fas fa-plus"></i> 新建终端
          </button>

          <button
            className={styles.toolbarBtn}
            onClick={duplicateCurrentSession}
            disabled={!activeTabId}
            title="复制会话"
          >
            <i className="fas fa-clone"></i> 复制
          </button>

          <button
            className={styles.toolbarBtn}
            onClick={reconnectCurrentSession}
            disabled={!activeTabId}
            title="重新连接"
          >
            <i className="fas fa-sync-alt"></i> 重连
          </button>
        </div>

        <div className={styles.toolbarRight}>
          <button
            className={`${styles.toolbarBtn} ${styles.aiBtn}`}
            onClick={toggleAIAssistant}
            title="AI助手 (Ctrl+Shift+A)"
          >
            🤖 AI助手
          </button>
          <span className={styles.sessionCount}>
            活跃会话: <strong>{tabs.length}</strong>
          </span>
        </div>
      </div>

      {/* 标签栏 */}
      {tabs.length > 0 && (
        <div className={styles.terminalTabs}>
          {tabs.map((tab) => (
            <div
              key={tab.id}
              className={`${styles.terminalTab} ${
                activeTabId === tab.id ? styles.active : ''
              }`}
              onClick={() => switchToTerminal(tab.id)}
            >
              <span
                className={`${styles.statusIndicator} ${styles[`status-${tab.status}`]}`}
              ></span>
              <span className={styles.tabTitle}>{tab.serverName}</span>
              <button
                className={styles.tabCloseBtn}
                onClick={(e) => {
                  e.stopPropagation();
                  closeTerminal(tab.id);
                }}
                title="关闭"
              >
                <i className="fas fa-times"></i>
              </button>
            </div>
          ))}
        </div>
      )}

      {/* 终端内容区域 */}
      <div className={styles.terminalContent}>
        {tabs.length === 0 ? renderWelcome() : tabs.map(renderTerminalContent)}
      </div>

      {/* AI助手面板 */}
      <AIAssistant
        isOpen={isAIAssistantOpen}
        onClose={() => setIsAIAssistantOpen(false)}
        terminalContext={
          activeTabId
            ? {
                terminalId: activeTabId,
                terminalName:
                  tabs.find((t) => t.id === activeTabId)?.serverName || '',
              }
            : undefined
        }
        onGetTerminalContext={getTerminalContext}
      />
    </div>
  );
}

/**
 * 单个终端标签内容组件
 */
interface TerminalContentProps {
  tab: TerminalTab;
  isActive: boolean;
  onStatusChange: (
    status: 'disconnected' | 'connecting' | 'connected' | 'error'
  ) => void;
  sessionsRef: React.MutableRefObject<Map<string, TerminalSessionData>>;
}

function TerminalContent({
  tab,
  isActive,
  onStatusChange,
  sessionsRef,
}: TerminalContentProps): JSX.Element {
  const terminalRef = useRef<TerminalHandle>(null);

  // 使用useTerminal hook
  const useTerminalHook = useTerminal({
    onOutput: (data) => {
      terminalRef.current?.write(data);
    },
    onConnected: () => {
      onStatusChange('connected');
      terminalRef.current?.write('\r\nTerminal ready.\r\n');
    },
    onDisconnected: () => {
      onStatusChange('disconnected');
    },
    onError: (error) => {
      onStatusChange('error');
      terminalRef.current?.write(`\r\n\x1b[31mError: ${error}\x1b[0m\r\n`);
    },
  });

  // 存储会话引用
  useEffect(() => {
    sessionsRef.current.set(tab.id, {
      terminalRef,
      useTerminalHook,
    });

    return () => {
      sessionsRef.current.delete(tab.id);
    };
  }, [tab.id, useTerminalHook, sessionsRef]);

  // 初始连接
  useEffect(() => {
    if (terminalRef.current) {
      // 延迟连接，等待终端就绪
      setTimeout(() => {
        const size = terminalRef.current?.getSize() || { cols: 80, rows: 24 };
        useTerminalHook.connect(tab.serverId, size.cols, size.rows);
      }, 500);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab.serverId]);

  return (
    <div
      className={styles.terminalPane}
      style={{ display: isActive ? 'block' : 'none' }}
    >
      <Terminal
        ref={terminalRef}
        onData={(data) => useTerminalHook.sendInput(data)}
        onResize={(cols, rows) => useTerminalHook.resize(cols, rows)}
        autoFocus={isActive}
      />
    </div>
  );
}
