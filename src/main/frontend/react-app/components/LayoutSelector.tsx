import { ReactNode, useMemo } from 'react';
import { useLayout } from '../contexts/layoutContext';
import { ShellLayout } from '../layout/ShellLayout';
import { ContentOnlyLayout } from './ContentOnlyLayout';

interface LayoutSelectorProps {
  children: ReactNode;
}

/**
 * LayoutSelector组件
 *
 * 根据布局模式动态选择并渲染对应的布局组件
 * - 'shell'模式：渲染ShellLayout（带侧边栏）
 * - 'content-only'模式：渲染ContentOnlyLayout（无侧边栏）
 * - 'minimal'模式：渲染ContentOnlyLayout（可扩展支持MinimalLayout）
 *
 * @param {LayoutSelectorProps} props - 组件属性
 * @param {ReactNode} props.children - 子组件（路由内容）
 *
 * @example
 * ```tsx
 * <LayoutProvider>
 *   <LayoutSelector>
 *     <BrowserRouter>
 *       <Routes>
 *         <Route path="/overview" element={<OverviewPage />} />
 *       </Routes>
 *     </BrowserRouter>
 *   </LayoutSelector>
 * </LayoutProvider>
 * ```
 */
export function LayoutSelector({ children }: LayoutSelectorProps): JSX.Element {
  const { mode } = useLayout();

  // 使用useMemo缓存布局组件选择，避免不必要的重渲染
  const LayoutComponent = useMemo(() => {
    switch (mode) {
      case 'shell':
        return ShellLayout;
      case 'content-only':
        return ContentOnlyLayout;
      case 'minimal':
        // 当前minimal模式使用ContentOnlyLayout
        // 未来可以创建专门的MinimalLayout组件
        return ContentOnlyLayout;
      default:
        // 默认使用ContentOnlyLayout作为安全回退
        console.warn(`[LayoutSelector] Unknown mode: ${mode}, falling back to content-only`);
        return ContentOnlyLayout;
    }
  }, [mode]);

  return <LayoutComponent>{children}</LayoutComponent>;
}
