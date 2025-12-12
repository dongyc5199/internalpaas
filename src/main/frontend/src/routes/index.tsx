import { lazy } from 'react';
import { ROUTES } from '../shared/constants';
import type { AppRoute } from './types';
import { adminRoutes } from './admin';
import { monitoringRoutes } from './monitoring';

const TerminalManager = lazy(() =>
  import('../features/terminal').then((module) => ({ default: module.TerminalManager }))
);

const ApplicationsPage = lazy(() =>
  import('../features/applications').then((module) => ({ default: module.ApplicationsPage }))
);

const ConfigEditorPage = lazy(() =>
  import('../features/applications').then((module) => ({ default: module.ConfigEditorPage }))
);

const ProfilePage = lazy(() =>
  import('../features/profile').then((module) => ({ default: module.ProfilePage }))
);

export const protectedRoutes: AppRoute[] = [
  ...adminRoutes,
  ...monitoringRoutes,
  {
    path: ROUTES.APPLICATIONS.BASE,
    component: ApplicationsPage,
    suspenseText: '加载应用管理...',
  },
  {
    path: '/applications/:applicationId/config',
    component: ConfigEditorPage,
    suspenseText: '加载配置编辑器...',
  },
  {
    path: ROUTES.TERMINAL.BASE,
    component: TerminalManager,
    suspenseText: '加载SSH终端...',
  },
  {
    path: ROUTES.PROFILE,
    component: ProfilePage,
    suspenseText: '加载个人资料...',
  },
];

export { adminRoutes, monitoringRoutes };
