import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { AuthStore, LoginCredentials } from '../types/auth';
import type { User, UserPreferences } from '../types/user';

/**
 * 默认用户偏好设置
 */
const defaultPreferences: UserPreferences = {
  theme: 'auto',
  language: 'zh',
  sidebarCollapsed: false,
};

/**
 * 认证 Store
 *
 * 使用 Zustand 管理全局认证状态
 * - 使用 persist 中间件持久化到 localStorage
 * - 自动处理登录/登出状态
 * - 管理用户偏好设置
 *
 * @example
 * ```tsx
 * function LoginButton() {
 *   const { login, isLoading } = useAuthStore();
 *
 *   const handleLogin = async () => {
 *     await login({ username: 'admin', password: 'password' });
 *   };
 *
 *   return <button onClick={handleLogin} disabled={isLoading}>登录</button>;
 * }
 * ```
 */
export const useAuthStore = create<AuthStore>()(
  persist(
    (set, get) => ({
      // State
      user: null,
      isAuthenticated: false,
      isLoading: false,
      preferences: defaultPreferences,
      lastRefreshAt: null,

      // Actions
      login: async (credentials: LoginCredentials) => {
        set({ isLoading: true });

        try {
          // 调用登录 API (将在 authApi.ts 中实现)
          const response = await fetch('/api/auth/login', {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(credentials),
          });

          if (!response.ok) {
            const error = await response.text();
            throw new Error(error || '登录失败');
          }

          const data = await response.json();

          set({
            user: data.user,
            isAuthenticated: true,
            isLoading: false,
            lastRefreshAt: Date.now(),
          });
        } catch (error) {
          set({ isLoading: false });
          throw error;
        }
      },

      logout: async () => {
        try {
          // 调用登出 API
          await fetch('/api/auth/logout', {
            method: 'POST',
            credentials: 'same-origin',
          });
        } catch (error) {
          console.error('Logout error:', error);
        } finally {
          // 无论是否成功,都清除本地状态
          set({
            user: null,
            isAuthenticated: false,
            lastRefreshAt: null,
            preferences: defaultPreferences,
          });
        }
      },

      refreshSession: async () => {
        try {
          const response = await fetch('/api/auth/current-user', {
            method: 'GET',
            credentials: 'same-origin',
          });

          if (!response.ok) {
            // 会话已过期
            get().reset();
            return;
          }

          const user = await response.json();

          set({
            user,
            isAuthenticated: true,
            lastRefreshAt: Date.now(),
          });
        } catch (error) {
          console.error('Session refresh error:', error);
          get().reset();
        }
      },

      updateUser: (userData: Partial<User>) => {
        const { user } = get();
        if (user) {
          set({ user: { ...user, ...userData } });
        }
      },

      updatePreferences: (prefs: Partial<UserPreferences>) => {
        set((state) => ({
          preferences: { ...state.preferences, ...prefs },
        }));
      },

      reset: () => {
        set({
          user: null,
          isAuthenticated: false,
          isLoading: false,
          lastRefreshAt: null,
          preferences: defaultPreferences,
        });
      },
    }),
    {
      name: 'auth-storage', // localStorage key
      storage: createJSONStorage(() => localStorage),
      // 只持久化这些字段
      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
        preferences: state.preferences,
        lastRefreshAt: state.lastRefreshAt,
      }),
    }
  )
);

/**
 * 选择器 Hooks - 优化性能,避免不必要的重渲染
 */
export const useUser = (): User | null => useAuthStore((state) => state.user);
export const useIsAuthenticated = (): boolean => useAuthStore((state) => state.isAuthenticated);
export const useUserPreferences = (): UserPreferences =>
  useAuthStore((state) => state.preferences);
