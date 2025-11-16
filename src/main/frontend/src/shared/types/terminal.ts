/**
 * SSH Terminal Types
 * SSH终端相关类型定义
 */

/**
 * SSH会话信息
 */
export interface SSHSession {
  id: string;
  serverId: number;
  serverName: string;
  serverHost: string;
  userId: number;
  username: string;
  isActive: boolean;
  startTime: string;
  endTime?: string;
  lastActivityTime: string;
}

/**
 * SSH会话状态
 */
export interface SSHSessionStatus {
  sessionId: string;
  active: boolean;
  timestamp: number;
}

/**
 * SSH终端消息类型
 */
export type TerminalMessageType =
  | 'connect'      // 连接请求
  | 'connected'    // 连接成功
  | 'input'        // 用户输入
  | 'output'       // 服务器输出
  | 'data'         // 数据输出 (与output等价)
  | 'resize'       // 终端大小调整
  | 'disconnect'   // 断开连接
  | 'error'        // 错误消息
  | 'ping'         // 心跳
  | 'pong';        // 心跳响应

/**
 * SSH终端消息
 */
export interface TerminalMessage {
  type: TerminalMessageType;
  sessionId?: string;
  data?: string | TerminalSize | ConnectPayload | ErrorPayload;
  timestamp: number;
}

/**
 * 连接请求负载
 */
export interface ConnectPayload {
  serverId: number;
  cols?: number;
  rows?: number;
}

/**
 * 错误负载
 */
export interface ErrorPayload {
  code: string;
  message: string;
  details?: string;
}

/**
 * 终端尺寸
 */
export interface TerminalSize {
  cols: number;
  rows: number;
}

/**
 * SSH终端配置
 */
export interface TerminalConfig {
  fontSize: number;
  fontFamily: string;
  cursorBlink: boolean;
  cursorStyle: 'block' | 'underline' | 'bar';
  theme: TerminalTheme;
  scrollback: number;
  bellStyle: 'none' | 'sound';
}

/**
 * 终端主题
 */
export interface TerminalTheme {
  foreground: string;
  background: string;
  cursor: string;
  cursorAccent: string;
  selection: string;
  black: string;
  red: string;
  green: string;
  yellow: string;
  blue: string;
  magenta: string;
  cyan: string;
  white: string;
  brightBlack: string;
  brightRed: string;
  brightGreen: string;
  brightYellow: string;
  brightBlue: string;
  brightMagenta: string;
  brightCyan: string;
  brightWhite: string;
}

/**
 * 默认终端主题 (暗色)
 */
export const defaultDarkTheme: TerminalTheme = {
  foreground: '#f8f8f2',
  background: '#282a36',
  cursor: '#f8f8f0',
  cursorAccent: '#282a36',
  selection: '#44475a',
  black: '#21222c',
  red: '#ff5555',
  green: '#50fa7b',
  yellow: '#f1fa8c',
  blue: '#bd93f9',
  magenta: '#ff79c6',
  cyan: '#8be9fd',
  white: '#f8f8f2',
  brightBlack: '#6272a4',
  brightRed: '#ff6e6e',
  brightGreen: '#69ff94',
  brightYellow: '#ffffa5',
  brightBlue: '#d6acff',
  brightMagenta: '#ff92df',
  brightCyan: '#a4ffff',
  brightWhite: '#ffffff',
};

/**
 * 默认终端主题 (亮色)
 */
export const defaultLightTheme: TerminalTheme = {
  foreground: '#24292e',
  background: '#ffffff',
  cursor: '#24292e',
  cursorAccent: '#ffffff',
  selection: '#c8c8fa',
  black: '#24292e',
  red: '#d73a49',
  green: '#22863a',
  yellow: '#b08800',
  blue: '#0366d6',
  magenta: '#e36209',
  cyan: '#1b7c83',
  white: '#6a737d',
  brightBlack: '#959da5',
  brightRed: '#cb2431',
  brightGreen: '#22863a',
  brightYellow: '#b08800',
  brightBlue: '#0366d6',
  brightMagenta: '#e36209',
  brightCyan: '#1b7c83',
  brightWhite: '#24292e',
};

/**
 * 默认终端配置
 */
export const defaultTerminalConfig: TerminalConfig = {
  fontSize: 14,
  fontFamily: '"Cascadia Code", "Fira Code", "Consolas", "Monaco", monospace',
  cursorBlink: true,
  cursorStyle: 'block',
  theme: defaultDarkTheme,
  scrollback: 1000,
  bellStyle: 'none',
};

/**
 * SSH会话统计
 */
export interface SessionStats {
  totalSessions: number;
  activeSessions: number;
  totalServers: number;
  avgSessionDuration: number; // seconds
}

/**
 * 终端标签页
 */
export interface TerminalTab {
  id: string;
  serverId: number;
  serverName: string;
  sessionId?: string;
  isActive: boolean;
  isConnected: boolean;
  isConnecting: boolean;
  error?: string;
  createdAt: number;
}
