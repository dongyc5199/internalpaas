// Application type definitions
export interface Application {
  id: number;
  name: string;
  description?: string;
  filePath: string;
  fileName: string;
  fileSize: number;
  serverId: number;
  userId: number;
  status: ApplicationStatus;
  port?: number;
  debugPort?: number;
  pid?: number;
  startedAt?: string;
  stoppedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export enum ApplicationStatus {
  STOPPED = 'STOPPED',
  STARTING = 'STARTING',
  RUNNING = 'RUNNING',
  STOPPING = 'STOPPING',
  ERROR = 'ERROR',
}
