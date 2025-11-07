import type { User, UserPreferences } from './user';

/**
 * 登录凭证
 */
export interface LoginCredentials {
  username: string;
  password: string;
  rememberMe?: boolean;
}

/**
 * 登录响应
 */
export interface LoginResponse {
  user: User;
  sessionId?: string;
  expiresAt?: number;
}

/**
 * 认证状态
 */
export interface AuthState {
  /** 当前登录用户 */
  user: User | null;
  /** 是否已认证 */
  isAuthenticated: boolean;
  /** 是否正在加载 */
  isLoading: boolean;
  /** 用户偏好设置 */
  preferences: UserPreferences;
  /** 上次会话刷新时间 */
  lastRefreshAt: number | null;
}

/**
 * 认证操作
 */
export interface AuthActions {
  /** 登录 */
  login: (credentials: LoginCredentials) => Promise<void>;
  /** 登出 */
  logout: () => Promise<void>;
  /** 刷新会话 */
  refreshSession: () => Promise<void>;
  /** 更新用户信息 */
  updateUser: (user: Partial<User>) => void;
  /** 更新偏好设置 */
  updatePreferences: (preferences: Partial<UserPreferences>) => void;
  /** 重置状态 */
  reset: () => void;
}

/**
 * 认证 Store 完整类型
 */
export type AuthStore = AuthState & AuthActions;
