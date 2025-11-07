import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { queryClient } from './shared/config/queryClient';
import { MainLayout, ProtectedRoute } from './shared/components';
import { LoginPage } from './features/auth';
import {
  DashboardPage,
  ServersPage,
  ServerDetailPage,
  UsersPage,
  UserDetailPage,
} from './features/admin';
import { ROUTES } from './shared/constants';
import { UserRole } from './shared/types/user';

/**
 * 主应用组件
 *
 * Provider 层级结构:
 * 1. QueryClientProvider - React Query 数据获取
 * 2. BrowserRouter - 路由管理
 * 3. Routes - 路由配置
 *
 * 参考 react-app 的 Provider 嵌套顺序
 */
function App(): React.JSX.Element {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter basename="/app">
        <Routes>
          {/* 公开路由 - LoginPage 不使用 EmptyLayout (自带完整布局) */}
          <Route path={ROUTES.LOGIN} element={<LoginPage />} />

          {/* 受保护路由 - 使用 MainLayout */}
          <Route
            path={ROUTES.HOME}
            element={
              <ProtectedRoute>
                <MainLayout>
                  <DashboardPage />
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 应用管理 */}
          <Route
            path={ROUTES.APPLICATIONS.BASE}
            element={
              <ProtectedRoute>
                <MainLayout>
                  <ApplicationsPage />
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* SSH 终端 */}
          <Route
            path={ROUTES.TERMINAL.BASE}
            element={
              <ProtectedRoute>
                <MainLayout>
                  <TerminalPage />
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 监控中心 - 需要管理员权限 */}
          <Route
            path={ROUTES.MONITORING.BASE}
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <MonitoringPage />
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 系统管理 - 需要管理员权限 */}
          <Route
            path={ROUTES.ADMIN.SERVERS}
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <ServersPage />
                </MainLayout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/servers/:id"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <ServerDetailPage />
                </MainLayout>
              </ProtectedRoute>
            }
          />

          <Route
            path={ROUTES.ADMIN.USERS}
            element={
              <ProtectedRoute requiredRoles={[UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <UsersPage />
                </MainLayout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/users/:id"
            element={
              <ProtectedRoute requiredRoles={[UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <UserDetailPage />
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 个人资料 */}
          <Route
            path={ROUTES.PROFILE}
            element={
              <ProtectedRoute>
                <MainLayout>
                  <ProfilePage />
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 默认重定向 */}
          <Route path="*" element={<Navigate to={ROUTES.HOME} replace />} />
        </Routes>
      </BrowserRouter>

      {/* React Query Devtools - 仅开发环境显示 */}
      {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
    </QueryClientProvider>
  );
}

/**
 * 临时页面组件 - 后续在各自的 feature 模块中实现
 */
// DashboardPage, ServersPage 和 UsersPage 已从 admin feature 导入

function ApplicationsPage(): React.JSX.Element {
  return (
    <div style={{ padding: '2rem' }}>
      <h1>应用管理</h1>
      <p>管理您的应用程序</p>
    </div>
  );
}

function TerminalPage(): React.JSX.Element {
  return (
    <div style={{ padding: '2rem' }}>
      <h1>SSH 终端</h1>
      <p>连接到远程服务器</p>
    </div>
  );
}

function MonitoringPage(): React.JSX.Element {
  return (
    <div style={{ padding: '2rem' }}>
      <h1>监控中心</h1>
      <p>查看系统监控数据</p>
    </div>
  );
}

function ProfilePage(): React.JSX.Element {
  return (
    <div style={{ padding: '2rem' }}>
      <h1>个人资料</h1>
      <p>查看和编辑您的个人信息</p>
    </div>
  );
}

export default App;
