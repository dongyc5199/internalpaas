import { createContext, useContext } from 'react';
import type { LayoutContextValue } from '../types/navigation';

/**
 * Layout Context
 *
 * 提供布局模式状态和控制方法的React Context
 */
export const LayoutContext = createContext<LayoutContextValue | null>(null);

/**
 * useLayout Hook
 *
 * 访问Layout Context的自定义Hook
 *
 * @throws {Error} 如果在LayoutProvider之外使用将抛出错误
 * @returns {LayoutContextValue} 布局上下文值
 *
 * @example
 * ```tsx
 * function MyComponent() {
 *   const { mode, isEmbedded } = useLayout();
 *
 *   return (
 *     <div>
 *       当前模式: {mode}
 *       {isEmbedded && <p>运行在嵌入模式下</p>}
 *     </div>
 *   );
 * }
 * ```
 */
export function useLayout(): LayoutContextValue {
  const context = useContext(LayoutContext);

  if (!context) {
    throw new Error('useLayout must be used within LayoutProvider');
  }

  return context;
}
