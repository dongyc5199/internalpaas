import { ReactNode } from 'react';

interface ContentOnlyLayoutProps {
  children: ReactNode;
}

/**
 * ContentOnlyLayout组件
 *
 * 无侧边栏的简化布局，用于嵌入模式
 * 当React应用嵌入到主应用时使用此布局，让主应用的侧边栏处理导航
 *
 * @param {ContentOnlyLayoutProps} props - 组件属性
 * @param {ReactNode} props.children - 子组件（通常是页面内容）
 *
 * @example
 * ```tsx
 * <ContentOnlyLayout>
 *   <OverviewPage />
 * </ContentOnlyLayout>
 * ```
 */
export function ContentOnlyLayout({ children }: ContentOnlyLayoutProps): JSX.Element {
  return (
    <div className="content-only-layout" style={{
      width: '100%',
      height: '100%',
      display: 'flex',
      flexDirection: 'column'
    }}>
      <main className="dp-main-content" style={{
        flex: 1,
        overflow: 'auto',
        padding: '0',
        margin: '0'
      }}>
        {children}
      </main>
    </div>
  );
}
