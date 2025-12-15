// 用户相关类型
export interface User {
  id: number;
  username: string;
  email: string;
  is_active: boolean;
  is_admin: boolean;
  gitea_id?: number;
  created_at: string;
  updated_at: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

// 项目相关类型
export interface Project {
  id: number;
  name: string;
  display_name: string;
  description?: string;
  gitea_org_id?: number;
  gitea_org_name?: string;
  repository_count: number;
  member_count: number;
  build_count: number;
  created_at: string;
  updated_at: string;
}

export interface CreateProjectRequest {
  name: string;
  display_name: string;
  description?: string;
}

// 仓库相关类型
export interface Repository {
  id: number;
  project_id: number;
  owner_id: number;
  name: string;
  full_name: string;
  description?: string;
  language?: string;
  gitea_repo_id?: number;
  clone_url?: string;
  ssh_url?: string;
  drone_repo_id?: number;
  drone_active: boolean;
  size: number;
  star_count: number;
  fork_count: number;
  issue_count: number;
  default_branch?: string;
  is_private: boolean;
  created_at: string;
  updated_at: string;
}

export interface CreateRepositoryRequest {
  name: string;
  description?: string;
  is_private: boolean;
  auto_init: boolean;
  gitignore?: string;
  license?: string;
}

// 构建记录相关类型
export type BuildStatus = 'pending' | 'running' | 'success' | 'failure' | 'killed' | 'error';
export type BuildTrigger = 'push' | 'pull_request' | 'tag' | 'manual' | 'cron' | 'restart';

export interface BuildRecord {
  id: number;
  repository_id: number;
  trigger_user_id: number;
  drone_build_id?: number;
  drone_build_number?: number;
  drone_link?: string;
  status: BuildStatus;
  trigger: BuildTrigger;
  branch: string;
  commit: string;
  event?: string;
  commit_message?: string;
  author_name?: string;
  author_email?: string;
  started_at?: string;
  finished_at?: string;
  duration?: number;
  quality_report_id?: number;
  created_at: string;
  updated_at: string;
}

export interface TriggerBuildRequest {
  branch: string;
  commit?: string;
  params?: Record<string, string>;
}

// 质量报告相关类型
export type QualityGate = 'PASSED' | 'FAILED' | 'WARN' | 'NONE';

export interface QualityReport {
  id: number;
  repository_id: number;
  build_id?: number;
  sonar_project_key: string;
  sonar_task_id?: string;
  sonar_analysis_id?: string;
  sonar_link?: string;
  branch: string;
  commit: string;
  quality_gate_status: QualityGate;
  quality_gate_details?: Record<string, any>;
  lines: number;
  code_smells: number;
  bugs: number;
  vulnerabilities: number;
  security_hotspots: number;
  duplications: number;
  coverage: number;
  technical_debt: number;
  reliability_rating?: string;
  security_rating?: string;
  maintainability_rating?: string;
  security_review_rating?: string;
  analyzed_at?: string;
  analysis_duration?: number;
  status: string;
  metrics?: Record<string, any>;
  created_at: string;
  updated_at: string;
}

// WebSocket消息类型
export type MessageType = 'build_log' | 'build_status' | 'error' | 'info';

export interface WebSocketMessage {
  type: MessageType;
  build_id?: number;
  timestamp: string;
  data: Record<string, any>;
}

// API响应类型
export interface ApiResponse<T> {
  data?: T;
  error?: string;
  message?: string;
}

export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  page_size: number;
}
