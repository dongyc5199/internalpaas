/**
 * Terminal Feature Module
 * SSH终端功能模块
 */

// Components
export { Terminal, AIAssistant } from './components';
export type { TerminalProps, TerminalHandle, AIAssistantProps } from './components';

// Hooks
export { useTerminal, useTypewriter } from './hooks';
export type {
  UseTerminalReturn,
  UseTerminalOptions,
  TerminalSession,
  WSConnectionStatus,
  TypewriterConfig,
  TypewriterController,
} from './hooks';

// Types
export type { AIMode, AIMessageRole, AIMessage, AIChatSession, AISettings } from './types';
export { STREAMING_SPEED_MAP, DEFAULT_AI_SETTINGS } from './types';

// Pages
export { TerminalManager } from './pages';
