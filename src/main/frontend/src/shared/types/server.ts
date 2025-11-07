// Server type definitions
export interface Server {
  id: number;
  name: string;
  host: string;
  port: number;
  username: string;
  status: ServerStatus;
  statusMessage?: string;
  lastHealthCheck?: string;
  tags: string[];
  groupId?: number;
  metrics?: ServerMetrics;
  createdAt: string;
  updatedAt: string;
}

export enum ServerStatus {
  ONLINE = 'ONLINE',
  OFFLINE = 'OFFLINE',
  MAINTENANCE = 'MAINTENANCE',
  ERROR = 'ERROR',
  UNKNOWN = 'UNKNOWN',
}

export interface ServerMetrics {
  cpuUsage: number;
  memoryUsage: number;
  diskUsage: number;
  networkIn: number;
  networkOut: number;
  timestamp: string;
}
