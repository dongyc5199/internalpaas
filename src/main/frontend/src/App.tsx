import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { lazy, Suspense } from 'react';
import { queryClient } from './shared/config/queryClient';
import { MainLayout, ProtectedRoute, Loading } from './shared/components';
import { LoginPage } from './features/auth';
import { ROUTES } from './shared/constants';
import { UserRole } from './shared/types/user';

// 懒加载 Admin 页面（代码分割优化）
const DashboardPage = lazy(() =>
  import('./features/admin').then((module) => ({ default: module.DashboardPage }))
);
const ServersPage = lazy(() =>
  import('./features/admin').then((module) => ({ default: module.ServersPage }))
);
const ServerDetailPage = lazy(() =>
  import('./features/admin').then((module) => ({ default: module.ServerDetailPage }))
);
const UsersPage = lazy(() =>
  import('./features/admin').then((module) => ({ default: module.UsersPage }))
);
const UserDetailPage = lazy(() =>
  import('./features/admin').then((module) => ({ default: module.UserDetailPage }))
);

// 懒加载 Monitoring 页面
const MonitoringDashboard = lazy(() =>
  import('./features/monitoring').then((module) => ({ default: module.MonitoringDashboard }))
);
const ServerMonitoringDetail = lazy(() =>
  import('./features/monitoring').then((module) => ({ default: module.ServerMonitoringDetail }))
);
const AlertsPage = lazy(() =>
  import('./features/monitoring').then((module) => ({ default: module.AlertsPage }))
);
const AlertThresholdConfig = lazy(() =>
  import('./features/monitoring').then((module) => ({ default: module.AlertThresholdConfig }))
);
const MonitoringHistory = lazy(() =>
  import('./features/monitoring').then((module) => ({ default: module.MonitoringHistory }))
);
const AlertDetailPage = lazy(() =>
  import('./features/monitoring').then((module) => ({ default: module.AlertDetailPage }))
);
const AlertNotificationSettings = lazy(() =>
  import('./features/monitoring').then((module) => ({ default: module.AlertNotificationSettings }))
);
const PerformanceMetrics = lazy(() =>
  import('./features/monitoring').then((module) => ({ default: module.PerformanceMetrics }))
);
const ResourceTrends = lazy(() =>
  import('./features/monitoring').then((module) => ({ default: module.ResourceTrends }))
);
const ServerComparison = lazy(() =>
  import('./features/monitoring').then((module) => ({ default: module.ServerComparison }))
);
const ServerHealthCheck = lazy(() =>
  import('./features/monitoring').then((module) => ({ default: module.ServerHealthCheck }))
);
const UserActivityMonitor = lazy(() =>
  import('./features/monitoring').then((module) => ({ default: module.UserActivityMonitor }))
);
const RealtimeMetricsDashboard = lazy(() =>
  import('./features/monitoring').then((module) => ({ default: module.RealtimeMetricsDashboard }))
);

// 懒加载 Terminal 页面
const TerminalManager = lazy(() =>
  import('./features/terminal').then((module) => ({ default: module.TerminalManager }))
);

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
                  <Suspense fallback={<Loading text="加载仪表板..." />}>
                    <DashboardPage />
                  </Suspense>
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
                  <Suspense fallback={<Loading text="加载SSH终端..." />}>
                    <TerminalManager />
                  </Suspense>
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
                  <Suspense fallback={<Loading text="加载监控中心..." />}>
                    <MonitoringDashboard />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 服务器监控详情 - 需要管理员权限 */}
          <Route
            path="/monitoring/servers/:serverId"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载服务器监控详情..." />}>
                    <ServerMonitoringDetail />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 告警列表 - 需要管理员权限 */}
          <Route
            path="/monitoring/alerts"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载告警列表..." />}>
                    <AlertsPage />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 告警阈值配置 - 需要管理员权限 */}
          <Route
            path="/monitoring/servers/:serverId/thresholds"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载阈值配置..." />}>
                    <AlertThresholdConfig />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 监控历史 - 需要管理员权限 */}
          <Route
            path="/monitoring/history"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载监控历史..." />}>
                    <MonitoringHistory />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 告警详情 - 需要管理员权限 */}
          <Route
            path="/monitoring/alerts/:alertId"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载告警详情..." />}>
                    <AlertDetailPage />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 告警通知设置 - 需要管理员权限 */}
          <Route
            path="/monitoring/notifications"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载通知设置..." />}>
                    <AlertNotificationSettings />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 性能指标仪表板 - 需要管理员权限 */}
          <Route
            path="/monitoring/performance"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载性能指标..." />}>
                    <PerformanceMetrics />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 资源使用趋势分析 - 需要管理员权限 */}
          <Route
            path="/monitoring/trends"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载趋势分析..." />}>
                    <ResourceTrends />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 服务器对比视图 - 需要管理员权限 */}
          <Route
            path="/monitoring/comparison"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载服务器对比..." />}>
                    <ServerComparison />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 服务器健康检查 - 需要管理员权限 */}
          <Route
            path="/monitoring/health"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载健康检查..." />}>
                    <ServerHealthCheck />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 用户活动监控 - 需要管理员权限 */}
          <Route
            path="/monitoring/user-activity"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载用户活动..." />}>
                    <UserActivityMonitor />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 实时指标仪表盘 - 需要管理员权限 */}
          <Route
            path="/monitoring/realtime"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载实时指标..." />}>
                    <RealtimeMetricsDashboard />
                  </Suspense>
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
                  <Suspense fallback={<Loading text="加载服务器列表..." />}>
                    <ServersPage />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/servers/:id"
            element={
              <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载服务器详情..." />}>
                    <ServerDetailPage />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          <Route
            path={ROUTES.ADMIN.USERS}
            element={
              <ProtectedRoute requiredRoles={[UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载用户列表..." />}>
                    <UsersPage />
                  </Suspense>
                </MainLayout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/users/:id"
            element={
              <ProtectedRoute requiredRoles={[UserRole.SUPER_ADMIN]}>
                <MainLayout>
                  <Suspense fallback={<Loading text="加载用户详情..." />}>
                    <UserDetailPage />
                  </Suspense>
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

function ProfilePage(): React.JSX.Element {
  return (
    <div style={{ padding: '2rem' }}>
      <h1>个人资料</h1>
      <p>查看和编辑您的个人信息</p>
    </div>
  );
}

export default App;
