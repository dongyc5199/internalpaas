// User type definitions
export interface User {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  displayName?: string;
  avatar?: string;
  createdAt: string;
  lastLogin?: string;
}

export enum UserRole {
  SUPER_ADMIN = 'SUPER_ADMIN',
  ADMIN = 'ADMIN',
  DEVELOPER = 'DEVELOPER',
}

export interface UserPreferences {
  theme: 'light' | 'dark' | 'auto';
  language: 'en' | 'zh';
  sidebarCollapsed: boolean;
  defaultDashboard?: string;
}
