import { lazy } from 'react';
import { ROUTES } from '../shared/constants';
import { UserRole } from '../shared/types/user';
import type { AppRoute } from './types';

const DashboardPage = lazy(() =>
  import('../features/admin').then((module) => ({ default: module.DashboardPage }))
);

const ServersPage = lazy(() =>
  import('../features/admin').then((module) => ({ default: module.ServersPage }))
);

const ServerDetailPage = lazy(() =>
  import('../features/admin').then((module) => ({ default: module.ServerDetailPage }))
);

const UsersPage = lazy(() =>
  import('../features/admin').then((module) => ({ default: module.UsersPage }))
);

const UserDetailPage = lazy(() =>
  import('../features/admin').then((module) => ({ default: module.UserDetailPage }))
);

export const adminRoutes: AppRoute[] = [
  {
    path: ROUTES.HOME,
    component: DashboardPage,
    suspenseText: '加载仪表盘...',
  },
  {
    path: ROUTES.ADMIN.SERVERS,
    component: ServersPage,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载服务器列表...',
  },
  {
    path: '/admin/servers/:id',
    component: ServerDetailPage,
    requiredRoles: [UserRole.ADMIN, UserRole.SUPER_ADMIN],
    suspenseText: '加载服务器详情...',
  },
  {
    path: ROUTES.ADMIN.USERS,
    component: UsersPage,
    requiredRoles: [UserRole.SUPER_ADMIN],
    suspenseText: '加载用户列表...',
  },
  {
    path: '/admin/users/:id',
    component: UserDetailPage,
    requiredRoles: [UserRole.SUPER_ADMIN],
    suspenseText: '加载用户详情...',
  },
];
