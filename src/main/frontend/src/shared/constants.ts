// Application-wide constants

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';
export const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8080/ws';

// Query keys for React Query
export const QUERY_KEYS = {
  SERVERS: 'servers',
  USERS: 'users',
  APPLICATIONS: 'applications',
  MONITORING: 'monitoring',
  SSH_SESSIONS: 'sshSessions',
  PROFILE: 'profile',
  DASHBOARD: 'dashboard',
} as const;

// WebSocket topics
export const WS_TOPICS = {
  SERVER_STATUS: (serverId: number): string => `/topic/server-status/${String(serverId)}`,
  APPLICATION_STATUS: (appId: number): string => `/topic/application-status/${String(appId)}`,
  SSH_OUTPUT: (sessionId: string): string => `/topic/ssh/${sessionId}/output`,
  SSH_INPUT: (sessionId: string): string => `/app/ssh/${sessionId}/input`,
  APPLICATION_LOGS: (appId: number): string => `/topic/application-logs/${String(appId)}`,
  SERVERS_GLOBAL: '/topic/servers',
  USER_NOTIFICATIONS: '/user/queue/notifications',
} as const;

// Local storage keys
export const STORAGE_KEYS = {
  AUTH: 'auth-storage',
  UI_STATE: 'ui-storage',
  USER_PREFERENCES: 'user-preferences',
  TERMINAL_SESSIONS: 'active-terminal-sessions',
} as const;

// Application routes
export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  ADMIN: {
    BASE: '/admin',
    SERVERS: '/admin/servers',
    USERS: '/admin/users',
    SERVER_DETAIL: (id: number): string => `/admin/servers/${String(id)}`,
  },
  TERMINAL: {
    BASE: '/terminal',
    CONNECT: (serverId: number): string => `/terminal/connect/${String(serverId)}`,
  },
  MONITORING: {
    BASE: '/monitoring',
    SERVER: (id: number): string => `/monitoring/server/${String(id)}`,
    HISTORY: '/monitoring/history',
    THRESHOLDS: '/monitoring/thresholds',
  },
  APPLICATIONS: {
    BASE: '/applications',
    DETAIL: (id: number): string => `/applications/${String(id)}`,
  },
  PROFILE: '/profile',
} as const;
