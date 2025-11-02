import { ReactNode, useMemo, useState, useCallback } from 'react';
import { LayoutContext } from '../contexts/layoutContext';
import { useEmbedMode } from '../hooks/useEmbedMode';
import type { LayoutMode } from '../types/navigation';

interface LayoutProviderProps {
  children: ReactNode;
}

/**
 * Layout Provider组件
 *
 * 提供布局模式状态管理的Context Provider
 * 自动检测嵌入/独立模式并设置对应的布局
 *
 * @param {LayoutProviderProps} props - 组件属性
 * @param {ReactNode} props.children - 子组件
 *
 * @example
 * ```tsx
 * function App() {
 *   return (
 *     <LayoutProvider>
 *       <LayoutSelector>
 *         <Router />
 *       </LayoutSelector>
 *     </LayoutProvider>
 *   );
 * }
 * ```
 */
export function LayoutProvider({ children }: LayoutProviderProps): JSX.Element {
  // 使用嵌入模式检测Hook
  const isEmbedded = useEmbedMode();

  // 根据嵌入模式设置初始布局模式
  const [mode, setModeState] = useState<LayoutMode>(
    isEmbedded ? 'content-only' : 'shell'
  );

  // 布局模式切换方法（使用useCallback稳定引用）
  const setMode = useCallback((newMode: LayoutMode) => {
    setModeState(newMode);

    // 调试日志
    if (window.__DEPLOY_PLATFORM_DEBUG__) {
      console.log('[LayoutProvider] Mode changed:', newMode);
    }
  }, []);

  // 使用useMemo缓存context value，避免不必要的重渲染
  const value = useMemo(
    () => ({
      mode,
      setMode,
      isEmbedded
    }),
    [mode, setMode, isEmbedded]
  );

  return (
    <LayoutContext.Provider value={value}>
      {children}
    </LayoutContext.Provider>
  );
}
