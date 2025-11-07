# Data Model: Frontend React Migration

**Branch**: `007-frontend-react-migration`
**Date**: 2025-01-04
**Phase**: Phase 1 - Design & Contracts

## Overview

This document defines the data models, state structures, and component hierarchies for the React SPA. It maps domain entities to TypeScript interfaces and Zustand store schemas.

---

## Core Domain Entities

### 1. User

**Purpose**: Represents authenticated user with permissions

```typescript
interface User {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  displayName?: string;
  avatar?: string;
  preferences: UserPreferences;
  createdAt: string; // ISO 8601 timestamp
  lastLogin?: string;
}

enum UserRole {
  SUPER_ADMIN = 'SUPER_ADMIN',
  ADMIN = 'ADMIN',
  DEVELOPER = 'DEVELOPER',
}

interface UserPreferences {
  theme: 'light' | 'dark' | 'auto';
  language: 'en' | 'zh';
  sidebarCollapsed: boolean;
  defaultDashboard?: string; // route path
}
```

**Relationships**:
- User has many Applications (ownership)
- User has many SSH Sessions
- User has many User Activities (audit log)

**State Management**: `stores/authStore.ts`

```typescript
interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  permissions: Permission[];

  // Actions
  login: (credentials: Credentials) => Promise<void>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<void>;
  updatePreferences: (prefs: Partial<UserPreferences>) => Promise<void>;
}
```

---

### 2. Server

**Purpose**: Remote server managed by the platform

```typescript
interface Server {
  id: number;
  name: string;
  host: string;
  port: number;
  username: string;
  // password stored on backend only, never sent to frontend

  status: ServerStatus;
  statusMessage?: string;
  lastHealthCheck?: string;

  tags: string[];
  groupId?: number; // Optional server group

  metrics?: ServerMetrics;

  createdAt: string;
  updatedAt: string;
}

enum ServerStatus {
  ONLINE = 'ONLINE',
  OFFLINE = 'OFFLINE',
  MAINTENANCE = 'MAINTENANCE',
  ERROR = 'ERROR',
  UNKNOWN = 'UNKNOWN',
}

interface ServerMetrics {
  cpuUsage: number; // 0-100
  memoryUsage: number; // 0-100
  diskUsage: number; // 0-100
  networkIn: number; // bytes/sec
  networkOut: number; // bytes/sec
  timestamp: string;
}
```

**Relationships**:
- Server belongs to optional ServerGroup
- Server has many Applications deployed
- Server has many SSH Sessions
- Server has many ServerMetrics (time series)

**State Management**: React Query cache (no global store)

```typescript
// Cache keys
const serverKeys = {
  all: ['servers'] as const,
  lists: () => [...serverKeys.all, 'list'] as const,
  list: (filters: ServerFilters) => [...serverKeys.lists(), filters] as const,
  details: () => [...serverKeys.all, 'detail'] as const,
  detail: (id: number) => [...serverKeys.details(), id] as const,
  metrics: (id: number) => [...serverKeys.detail(id), 'metrics'] as const,
};
```

---

### 3. Application

**Purpose**: JAR application deployed on servers

```typescript
interface Application {
  id: number;
  name: string;
  description?: string;

  filePath: string; // Path on server
  fileName: string;
  fileSize: number; // bytes

  serverId: number;
  userId: number; // Owner

  status: ApplicationStatus;
  port?: number;
  debugPort?: number;
  pid?: number; // Process ID if running

  config?: ApplicationConfig;

  startedAt?: string;
  stoppedAt?: string;
  createdAt: string;
  updatedAt: string;
}

enum ApplicationStatus {
  STOPPED = 'STOPPED',
  STARTING = 'STARTING',
  RUNNING = 'RUNNING',
  STOPPING = 'STOPPING',
  ERROR = 'ERROR',
}

interface ApplicationConfig {
  id: number;
  applicationId: number;
  name: string;
  content: string; // Configuration file content (properties/YAML)
  isActive: boolean;
  version: number;
  createdAt: string;
}
```

**Relationships**:
- Application belongs to User (owner)
- Application belongs to Server
- Application has many ApplicationConfigs (versioned)

**State Management**: React Query cache

---

### 4. SSH Session

**Purpose**: Active WebSocket SSH terminal session

```typescript
interface SSHSession {
  id: string; // UUID
  serverId: number;
  userId: number;

  status: SessionStatus;
  terminalType: string; // e.g., 'xterm-256color'
  rows: number;
  cols: number;

  createdAt: string;
  lastActivity?: string;
}

enum SessionStatus {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  ERROR = 'ERROR',
}

interface TerminalMessage {
  sessionId: string;
  type: 'input' | 'output' | 'resize' | 'error';
  data: string | { rows: number; cols: number };
  timestamp: string;
}
```

**Relationships**:
- SSH Session belongs to User
- SSH Session belongs to Server

**State Management**: Component-local state + WebSocket hook

```typescript
interface TerminalState {
  sessions: Map<string, SSHSession>;
  activeSessionId: string | null;

  createSession: (serverId: number) => Promise<string>;
  closeSession: (sessionId: string) => void;
  setActiveSession: (sessionId: string) => void;
}
```

---

### 5. Server Group

**Purpose**: Logical grouping of servers for management

```typescript
interface ServerGroup {
  id: number;
  name: string;
  description?: string;
  color?: string; // Hex color for UI

  serverIds: number[];

  createdAt: string;
  updatedAt: string;
}

interface ServerGroupMetrics {
  groupId: number;
  totalServers: number;
  onlineServers: number;
  offlineServers: number;
  avgCpuUsage: number;
  avgMemoryUsage: number;
  avgDiskUsage: number;
  timestamp: string;
}
```

**Relationships**:
- Server Group has many Servers

**State Management**: React Query cache

---

### 6. Monitoring Data

**Purpose**: Time-series metrics for visualization

```typescript
interface MetricDataPoint {
  timestamp: string; // ISO 8601
  value: number;
}

interface ServerMetricHistory {
  serverId: number;
  metricType: MetricType;
  timeRange: TimeRange;
  dataPoints: MetricDataPoint[];
}

enum MetricType {
  CPU = 'CPU',
  MEMORY = 'MEMORY',
  DISK = 'DISK',
  NETWORK_IN = 'NETWORK_IN',
  NETWORK_OUT = 'NETWORK_OUT',
}

interface TimeRange {
  start: string; // ISO 8601
  end: string;
  granularity: 'minute' | 'hour' | 'day'; // Aggregation level
}

interface AlertThreshold {
  id: number;
  serverId: number;
  metricType: MetricType;
  warningThreshold: number;
  criticalThreshold: number;
  enabled: boolean;
}
```

**State Management**: React Query cache with aggressive caching

---

## Frontend-Specific Entities

### 7. UI State

**Purpose**: Global UI state (theme, navigation, modals)

```typescript
interface UIState {
  // Theme
  theme: 'light' | 'dark';
  systemTheme: 'light' | 'dark'; // OS preference

  // Navigation
  sidebarCollapsed: boolean;
  currentRoute: string;

  // Modals
  activeModal: ModalType | null;
  modalProps: Record<string, any>;

  // Notifications
  notifications: Notification[];

  // Actions
  setTheme: (theme: 'light' | 'dark' | 'auto') => void;
  toggleSidebar: () => void;
  openModal: (type: ModalType, props: any) => void;
  closeModal: () => void;
  addNotification: (notification: Omit<Notification, 'id'>) => void;
  dismissNotification: (id: string) => void;
}

enum ModalType {
  SERVER_CREATE = 'SERVER_CREATE',
  SERVER_EDIT = 'SERVER_EDIT',
  SERVER_DELETE = 'SERVER_DELETE',
  USER_CREATE = 'USER_CREATE',
  USER_EDIT = 'USER_EDIT',
  APPLICATION_UPLOAD = 'APPLICATION_UPLOAD',
  CONFIG_EDITOR = 'CONFIG_EDITOR',
  SSH_CONFIG_IMPORT = 'SSH_CONFIG_IMPORT',
}

interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number; // ms, auto-dismiss
  action?: {
    label: string;
    onClick: () => void;
  };
  createdAt: string;
}
```

**State Management**: `stores/uiStore.ts` (Zustand)

---

### 8. WebSocket Connection State

**Purpose**: Track real-time connection status

```typescript
interface WebSocketState {
  status: ConnectionStatus;
  subscriptions: Map<string, SubscriptionInfo>;
  reconnectAttempts: number;
  lastConnectedAt?: string;
  lastDisconnectedAt?: string;

  // Actions
  subscribe: (topic: string, handler: MessageHandler) => void;
  unsubscribe: (topic: string) => void;
  sendMessage: (destination: string, body: any) => void;
}

enum ConnectionStatus {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  RECONNECTING = 'RECONNECTING',
  DISCONNECTED = 'DISCONNECTED',
  ERROR = 'ERROR',
}

interface SubscriptionInfo {
  topic: string;
  subscribedAt: string;
  messageCount: number;
  lastMessageAt?: string;
}

type MessageHandler = (message: any) => void;
```

**State Management**: Custom hook `useWebSocket` with internal Zustand store

---

## Component Data Flow Patterns

### 1. Server List Page

**Data Flow**:
```
useServers() hook
  ↓ (React Query)
[Server List from API]
  ↓
ServerListTable component
  ↓
ServerCard components
  ↓
Status Badge, Actions Menu
```

**State Dependencies**:
- React Query cache: Server list data
- UI Store: Selected servers, filter state
- WebSocket: Real-time status updates

**Component Props**:
```typescript
interface ServerListTableProps {
  servers: Server[];
  isLoading: boolean;
  error: Error | null;
  onServerClick: (server: Server) => void;
  onServerDelete: (serverId: number) => void;
  onServerEdit: (server: Server) => void;
}

interface ServerCardProps {
  server: Server;
  onClick: () => void;
  onEdit: () => void;
  onDelete: () => void;
}
```

---

### 2. SSH Terminal Page

**Data Flow**:
```
useWebSocket(topic) hook
  ↓ (STOMP WebSocket)
[Terminal messages]
  ↓
Terminal component (xterm.js)
  ↓ (user input)
sendMessage() action
  ↓
Backend SSH handler
```

**State Dependencies**:
- Component local state: XTerm instance, session ID
- WebSocket hook: Message stream, connection status
- React Query: Server info for connection

**Component Props**:
```typescript
interface TerminalProps {
  serverId: number;
  sessionId: string;
  onClose: () => void;
}

interface TerminalControlsProps {
  sessionId: string;
  status: ConnectionStatus;
  onReconnect: () => void;
  onClear: () => void;
  onClose: () => void;
}
```

---

### 3. Monitoring Dashboard

**Data Flow**:
```
useServerMetrics(serverId, timeRange) hook
  ↓ (React Query + WebSocket for live updates)
[Metric history + real-time data]
  ↓
MetricsChart component (ECharts)
  ↓
Chart renders with zoom/pan controls
```

**State Dependencies**:
- React Query cache: Historical metrics
- WebSocket: Real-time metric updates
- UI Store: Selected time range, chart settings

**Component Props**:
```typescript
interface MetricsChartProps {
  serverId: number;
  metricType: MetricType;
  timeRange: TimeRange;
  data: MetricDataPoint[];
  realTimeUpdates: boolean;
  onExport: () => void;
}

interface TimeRangePickerProps {
  value: TimeRange;
  onChange: (range: TimeRange) => void;
  presets: TimeRangePreset[];
}

interface TimeRangePreset {
  label: string;
  range: TimeRange;
}
```

---

## Validation Rules

### Server Validation

```typescript
import { z } from 'zod';

const serverSchema = z.object({
  name: z.string()
    .min(3, 'Name must be at least 3 characters')
    .max(50, 'Name must be at most 50 characters')
    .regex(/^[a-zA-Z0-9-_]+$/, 'Name can only contain alphanumeric characters, hyphens, and underscores'),

  host: z.string()
    .min(1, 'Host is required')
    .regex(/^[a-zA-Z0-9.-]+$/, 'Invalid hostname format'),

  port: z.number()
    .int('Port must be an integer')
    .min(1, 'Port must be at least 1')
    .max(65535, 'Port must be at most 65535'),

  username: z.string()
    .min(1, 'Username is required')
    .max(32, 'Username must be at most 32 characters'),

  password: z.string()
    .min(8, 'Password must be at least 8 characters')
    .max(128, 'Password must be at most 128 characters'),

  tags: z.array(z.string()).optional(),
  groupId: z.number().optional(),
});

type ServerFormData = z.infer<typeof serverSchema>;
```

### Application Validation

```typescript
const applicationSchema = z.object({
  name: z.string()
    .min(3, 'Name must be at least 3 characters')
    .max(100, 'Name must be at most 100 characters'),

  description: z.string()
    .max(500, 'Description must be at most 500 characters')
    .optional(),

  file: z.instanceof(File)
    .refine((file) => file.size <= 100 * 1024 * 1024, 'File size must be under 100MB')
    .refine((file) => file.name.endsWith('.jar'), 'File must be a JAR file'),

  serverId: z.number()
    .int('Server ID must be an integer')
    .positive('Server ID must be positive'),

  port: z.number()
    .int('Port must be an integer')
    .min(1024, 'Port must be at least 1024')
    .max(65535, 'Port must be at most 65535')
    .optional(),

  debugPort: z.number()
    .int('Debug port must be an integer')
    .min(5000, 'Debug port must be at least 5000')
    .max(5999, 'Debug port must be at most 5999')
    .optional(),
});

type ApplicationFormData = z.infer<typeof applicationSchema>;
```

---

## State Transitions

### Server Status State Machine

```
UNKNOWN → [health check] → ONLINE | OFFLINE | ERROR
ONLINE → [manual] → MAINTENANCE
MAINTENANCE → [manual] → ONLINE
ONLINE → [health check fail] → OFFLINE
OFFLINE → [health check success] → ONLINE
* → [error condition] → ERROR
ERROR → [retry] → ONLINE | OFFLINE
```

### Application Status State Machine

```
STOPPED → [start command] → STARTING
STARTING → [process started] → RUNNING
STARTING → [start failed] → ERROR
RUNNING → [stop command] → STOPPING
STOPPING → [process stopped] → STOPPED
STOPPING → [stop failed] → ERROR
RUNNING → [crash detected] → ERROR
ERROR → [restart] → STARTING
```

### SSH Session Status State Machine

```
null → [create session] → CONNECTING
CONNECTING → [WebSocket connected] → CONNECTED
CONNECTING → [connection failed] → ERROR
CONNECTED → [network drop] → DISCONNECTED
DISCONNECTED → [reconnect] → CONNECTING
CONNECTED → [close session] → DISCONNECTED
ERROR → [retry] → CONNECTING
```

---

## Data Persistence

### Local Storage

```typescript
// Persisted in browser localStorage
interface PersistedState {
  'auth-storage': {
    user: User | null;
    isAuthenticated: boolean;
  };

  'ui-storage': {
    theme: 'light' | 'dark' | 'auto';
    sidebarCollapsed: boolean;
  };

  'user-preferences': UserPreferences;
}
```

### Session Storage

```typescript
// Cleared on tab close
interface SessionState {
  'active-terminal-sessions': string[]; // Session IDs
  'selected-servers': number[]; // For batch operations
}
```

### React Query Cache

- Default cache time: 5 minutes
- Stale time: 1 minute for list queries, 30 seconds for detail queries
- Automatic garbage collection after 5 minutes of inactivity

---

## Type Exports

All types should be exported from a central location:

```typescript
// types/index.ts
export * from './user';
export * from './server';
export * from './application';
export * from './session';
export * from './metrics';
export * from './ui';
export * from './websocket';

// Usage in components
import type { Server, ServerStatus, ServerMetrics } from '@/types';
```

---

**Phase 1 Data Model Complete**: All entities, relationships, validation rules, and state management patterns documented. Ready for API contract generation.
