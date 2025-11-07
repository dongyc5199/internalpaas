import { ReactNode, useEffect } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore, useIsAuthenticated } from '../../stores/authStore';
import { UserRole } from '../../types/user';
import { ROUTES } from '../../constants';

interface ProtectedRouteProps {
  children: ReactNode;
  requiredRoles?: UserRole[];
  fallbackPath?: string;
}

/**
 * ProtectedRoute 组件
 *
 * 路由守卫组件,用于保护需要认证的路由
 *
 * 特性:
 * - 未登录用户重定向到登录页
 * - 支持基于角色的访问控制
 * - 自动刷新会话状态
 * - 保存跳转前的路径,登录后自动返回
 *
 * @example
 * ```tsx
 * // 需要登录
 * <ProtectedRoute>
 *   <DashboardPage />
 * </ProtectedRoute>
 *
 * // 需要管理员权限
 * <ProtectedRoute requiredRoles={[UserRole.ADMIN, UserRole.SUPER_ADMIN]}>
 *   <AdminPage />
 * </ProtectedRoute>
 * ```
 */
export function ProtectedRoute({
  children,
  requiredRoles,
  fallbackPath = ROUTES.HOME,
}: ProtectedRouteProps): React.JSX.Element {
  const isAuthenticated = useIsAuthenticated();
  const { user, refreshSession } = useAuthStore();
  const location = useLocation();

  /**
   * 刷新会话状态
   * 在组件挂载时检查会话是否有效
   */
  useEffect(() => {
    if (isAuthenticated) {
      refreshSession().catch((error) => {
        console.error('Failed to refresh session:', error);
      });
    }
  }, [isAuthenticated, refreshSession]);

  /**
   * 未登录,重定向到登录页
   */
  if (!isAuthenticated) {
    return <Navigate to={ROUTES.LOGIN} state={{ from: location }} replace />;
  }

  /**
   * 检查角色权限
   */
  if (requiredRoles && requiredRoles.length > 0 && user) {
    const hasRequiredRole = requiredRoles.includes(user.role);

    if (!hasRequiredRole) {
      // 权限不足,重定向到首页或指定页面
      return <Navigate to={fallbackPath} replace />;
    }
  }

  return <>{children}</>;
}
