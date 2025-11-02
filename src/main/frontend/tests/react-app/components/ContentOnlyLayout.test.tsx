import { render } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { ContentOnlyLayout } from '../../../react-app/components/ContentOnlyLayout';

describe('ContentOnlyLayout', () => {
  describe('Basic Rendering', () => {
    it('should render without crashing', () => {
      const { container } = render(
        <ContentOnlyLayout>
          <div>Test Content</div>
        </ContentOnlyLayout>
      );

      expect(container).toBeInTheDocument();
    });

    it('should render children content', () => {
      const { getByText } = render(
        <ContentOnlyLayout>
          <div>Test Child Component</div>
        </ContentOnlyLayout>
      );

      expect(getByText('Test Child Component')).toBeInTheDocument();
    });

    it('should render multiple children', () => {
      const { getByText } = render(
        <ContentOnlyLayout>
          <div>Child 1</div>
          <div>Child 2</div>
          <div>Child 3</div>
        </ContentOnlyLayout>
      );

      expect(getByText('Child 1')).toBeInTheDocument();
      expect(getByText('Child 2')).toBeInTheDocument();
      expect(getByText('Child 3')).toBeInTheDocument();
    });
  });

  describe('DOM Structure', () => {
    it('should render with correct root class name', () => {
      const { container } = render(
        <ContentOnlyLayout>
          <div>Content</div>
        </ContentOnlyLayout>
      );

      const rootElement = container.querySelector('.content-only-layout');
      expect(rootElement).toBeInTheDocument();
    });

    it('should render main content wrapper with correct class', () => {
      const { container } = render(
        <ContentOnlyLayout>
          <div>Content</div>
        </ContentOnlyLayout>
      );

      const mainElement = container.querySelector('.dp-main-content');
      expect(mainElement).toBeInTheDocument();
      expect(mainElement?.tagName).toBe('MAIN');
    });

    it('should have correct DOM hierarchy', () => {
      const { container } = render(
        <ContentOnlyLayout>
          <div data-testid="test-child">Content</div>
        </ContentOnlyLayout>
      );

      const rootElement = container.querySelector('.content-only-layout');
      const mainElement = rootElement?.querySelector('.dp-main-content');
      const childElement = mainElement?.querySelector('[data-testid="test-child"]');

      expect(rootElement).toBeInTheDocument();
      expect(mainElement).toBeInTheDocument();
      expect(childElement).toBeInTheDocument();
    });
  });

  describe('Inline Styles', () => {
    it('should apply correct styles to root element', () => {
      const { container } = render(
        <ContentOnlyLayout>
          <div>Content</div>
        </ContentOnlyLayout>
      );

      const rootElement = container.querySelector('.content-only-layout') as HTMLElement;

      expect(rootElement.style.width).toBe('100%');
      expect(rootElement.style.height).toBe('100%');
      expect(rootElement.style.display).toBe('flex');
      expect(rootElement.style.flexDirection).toBe('column');
    });

    it('should apply correct styles to main content element', () => {
      const { container } = render(
        <ContentOnlyLayout>
          <div>Content</div>
        </ContentOnlyLayout>
      );

      const mainElement = container.querySelector('.dp-main-content') as HTMLElement;

      // Browser expands 'flex: 1' to '1 1 0%'
      expect(mainElement.style.flex).toBe('1 1 0%');
      expect(mainElement.style.overflow).toBe('auto');
      expect(mainElement.style.padding).toBe('0px');
      expect(mainElement.style.margin).toBe('0px');
    });
  });

  describe('Content Rendering', () => {
    it('should render simple text content', () => {
      const { getByText } = render(
        <ContentOnlyLayout>
          Simple Text Content
        </ContentOnlyLayout>
      );

      expect(getByText('Simple Text Content')).toBeInTheDocument();
    });

    it('should render complex nested components', () => {
      const ComplexComponent = () => (
        <div>
          <header>Header Section</header>
          <nav>Navigation Section</nav>
          <article>Article Content</article>
          <footer>Footer Section</footer>
        </div>
      );

      const { getByText } = render(
        <ContentOnlyLayout>
          <ComplexComponent />
        </ContentOnlyLayout>
      );

      expect(getByText('Header Section')).toBeInTheDocument();
      expect(getByText('Navigation Section')).toBeInTheDocument();
      expect(getByText('Article Content')).toBeInTheDocument();
      expect(getByText('Footer Section')).toBeInTheDocument();
    });

    it('should render React components with props', () => {
      const TestComponent = ({ title, content }: { title: string; content: string }) => (
        <div>
          <h1>{title}</h1>
          <p>{content}</p>
        </div>
      );

      const { getByText } = render(
        <ContentOnlyLayout>
          <TestComponent title="Test Title" content="Test Content" />
        </ContentOnlyLayout>
      );

      expect(getByText('Test Title')).toBeInTheDocument();
      expect(getByText('Test Content')).toBeInTheDocument();
    });
  });

  describe('Layout Characteristics', () => {
    it('should have NO sidebar elements', () => {
      const { container } = render(
        <ContentOnlyLayout>
          <div>Content</div>
        </ContentOnlyLayout>
      );

      // 确认没有侧边栏相关元素
      expect(container.querySelector('aside')).not.toBeInTheDocument();
      expect(container.querySelector('[class*="sidebar"]')).not.toBeInTheDocument();
      expect(container.querySelector('nav[class*="side"]')).not.toBeInTheDocument();
    });

    it('should have NO header elements', () => {
      const { container } = render(
        <ContentOnlyLayout>
          <div>Content</div>
        </ContentOnlyLayout>
      );

      // 确认根层级没有header元素(children内部可以有)
      const rootElement = container.querySelector('.content-only-layout');
      const directHeader = rootElement?.querySelector(':scope > header');

      expect(directHeader).not.toBeInTheDocument();
    });

    it('should allow children to define their own structure', () => {
      const { getByTestId } = render(
        <ContentOnlyLayout>
          <div>
            <header data-testid="child-header">Child Header</header>
            <aside data-testid="child-sidebar">Child Sidebar</aside>
            <main data-testid="child-main">Child Main</main>
          </div>
        </ContentOnlyLayout>
      );

      // Children内部的结构应该保留
      expect(getByTestId('child-header')).toBeInTheDocument();
      expect(getByTestId('child-sidebar')).toBeInTheDocument();
      expect(getByTestId('child-main')).toBeInTheDocument();
    });
  });

  describe('Embedded Mode Integration', () => {
    it('should render content suitable for iframe embedding', () => {
      const { container } = render(
        <ContentOnlyLayout>
          <div data-testid="embedded-content">Embedded Content</div>
        </ContentOnlyLayout>
      );

      const rootElement = container.querySelector('.content-only-layout') as HTMLElement;

      // 验证100%宽高适合iframe嵌入
      expect(rootElement.style.width).toBe('100%');
      expect(rootElement.style.height).toBe('100%');

      // 验证内容可滚动
      const mainElement = container.querySelector('.dp-main-content') as HTMLElement;
      expect(mainElement.style.overflow).toBe('auto');
    });

    it('should have minimal padding/margin for seamless integration', () => {
      const { container } = render(
        <ContentOnlyLayout>
          <div>Content</div>
        </ContentOnlyLayout>
      );

      const mainElement = container.querySelector('.dp-main-content') as HTMLElement;

      // 验证没有padding/margin,实现无缝集成
      expect(mainElement.style.padding).toBe('0px');
      expect(mainElement.style.margin).toBe('0px');
    });
  });

  describe('Accessibility', () => {
    it('should use semantic HTML main element', () => {
      const { container } = render(
        <ContentOnlyLayout>
          <div>Content</div>
        </ContentOnlyLayout>
      );

      const mainElement = container.querySelector('main');
      expect(mainElement).toBeInTheDocument();
    });

    it('should preserve ARIA attributes from children', () => {
      const { getByRole } = render(
        <ContentOnlyLayout>
          <div role="region" aria-label="Test Region">
            Accessible Content
          </div>
        </ContentOnlyLayout>
      );

      const regionElement = getByRole('region');
      expect(regionElement).toHaveAttribute('aria-label', 'Test Region');
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty children gracefully', () => {
      const { container } = render(
        <ContentOnlyLayout>
          {null}
        </ContentOnlyLayout>
      );

      const mainElement = container.querySelector('.dp-main-content');
      expect(mainElement).toBeInTheDocument();
      expect(mainElement?.textContent).toBe('');
    });

    it('should handle undefined children gracefully', () => {
      const { container } = render(
        <ContentOnlyLayout>
          {undefined}
        </ContentOnlyLayout>
      );

      const mainElement = container.querySelector('.dp-main-content');
      expect(mainElement).toBeInTheDocument();
    });

    it('should handle boolean children gracefully', () => {
      const { container } = render(
        <ContentOnlyLayout>
          {false}
          {true}
        </ContentOnlyLayout>
      );

      const mainElement = container.querySelector('.dp-main-content');
      expect(mainElement).toBeInTheDocument();
    });

    it('should handle fragment children', () => {
      const { getByText } = render(
        <ContentOnlyLayout>
          <>
            <div>Fragment Child 1</div>
            <div>Fragment Child 2</div>
          </>
        </ContentOnlyLayout>
      );

      expect(getByText('Fragment Child 1')).toBeInTheDocument();
      expect(getByText('Fragment Child 2')).toBeInTheDocument();
    });
  });

  describe('Re-rendering Behavior', () => {
    it('should update when children change', () => {
      const { getByText, rerender } = render(
        <ContentOnlyLayout>
          <div>Initial Content</div>
        </ContentOnlyLayout>
      );

      expect(getByText('Initial Content')).toBeInTheDocument();

      rerender(
        <ContentOnlyLayout>
          <div>Updated Content</div>
        </ContentOnlyLayout>
      );

      expect(getByText('Updated Content')).toBeInTheDocument();
    });

    it('should maintain structure during re-renders', () => {
      const { container, rerender } = render(
        <ContentOnlyLayout>
          <div>Content 1</div>
        </ContentOnlyLayout>
      );

      const rootElement1 = container.querySelector('.content-only-layout');

      rerender(
        <ContentOnlyLayout>
          <div>Content 2</div>
        </ContentOnlyLayout>
      );

      const rootElement2 = container.querySelector('.content-only-layout');

      // 根元素结构应该保持一致
      expect(rootElement1?.className).toBe(rootElement2?.className);
    });
  });
});
