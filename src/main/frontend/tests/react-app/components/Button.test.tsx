/**
 * Button Component Tests
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Button } from '../../../react-app/components/Button';
import styles from '../../../react-app/components/Button/Button.module.css';

describe('Button', () => {
  describe('Rendering', () => {
    it('应该渲染按钮文本', () => {
      render(<Button>Click me</Button>);
      expect(screen.getByRole('button', { name: 'Click me' })).toBeInTheDocument();
    });

    it('应该渲染默认variant为primary', () => {
      const { container } = render(<Button>Primary</Button>);
      const button = container.querySelector('button');
      expect(button?.className).toContain('variant-primary');
    });

    it('应该渲染默认size为md', () => {
      const { container } = render(<Button>Medium</Button>);
      const button = container.querySelector('button');
      expect(button?.className).toContain('size-md');
    });

    it('应该渲染默认type为button', () => {
      render(<Button>Button</Button>);
      expect(screen.getByRole('button')).toHaveAttribute('type', 'button');
    });
  });

  describe('Variants', () => {
    it('应该渲染primary variant', () => {
      const { container } = render(<Button variant="primary">Primary</Button>);
      const button = container.querySelector('button');
      expect(button?.className).toContain('variant-primary');
    });

    it('应该渲染secondary variant', () => {
      const { container } = render(<Button variant="secondary">Secondary</Button>);
      const button = container.querySelector('button');
      expect(button?.className).toContain('variant-secondary');
    });

    it('应该渲染danger variant', () => {
      const { container } = render(<Button variant="danger">Danger</Button>);
      const button = container.querySelector('button');
      expect(button?.className).toContain('variant-danger');
    });

    it('应该渲染ghost variant', () => {
      const { container } = render(<Button variant="ghost">Ghost</Button>);
      const button = container.querySelector('button');
      expect(button?.className).toContain('variant-ghost');
    });

    it('应该渲染outline variant', () => {
      const { container } = render(<Button variant="outline">Outline</Button>);
      const button = container.querySelector('button');
      expect(button?.className).toContain('variant-outline');
    });
  });

  describe('Sizes', () => {
    it('应该渲染small size', () => {
      const { container } = render(<Button size="sm">Small</Button>);
      const button = container.querySelector('button');
      expect(button?.className).toContain('size-sm');
    });

    it('应该渲染medium size', () => {
      const { container } = render(<Button size="md">Medium</Button>);
      const button = container.querySelector('button');
      expect(button?.className).toContain('size-md');
    });

    it('应该渲染large size', () => {
      const { container } = render(<Button size="lg">Large</Button>);
      const button = container.querySelector('button');
      expect(button?.className).toContain('size-lg');
    });
  });

  describe('Full Width', () => {
    it('应该渲染fullWidth样式', () => {
      const { container } = render(<Button fullWidth>Full Width</Button>);
      const button = container.querySelector('button');
      expect(button?.className).toContain('fullWidth');
    });

    it('应该不渲染fullWidth样式当prop为false', () => {
      const { container } = render(<Button fullWidth={false}>Normal Width</Button>);
      const button = container.querySelector('button');
      expect(button?.className).not.toContain('fullWidth');
    });
  });

  describe('Loading State', () => {
    it('应该显示spinner当loading为true', () => {
      const { container } = render(<Button loading>Loading</Button>);
      const spinner = container.querySelector(`.${styles.spinner}`);
      expect(spinner).toBeInTheDocument();
    });

    it('应该禁用按钮当loading为true', () => {
      render(<Button loading>Loading</Button>);
      expect(screen.getByRole('button')).toBeDisabled();
    });

    it('应该设置aria-busy为true当loading', () => {
      render(<Button loading>Loading</Button>);
      expect(screen.getByRole('button')).toHaveAttribute('aria-busy', 'true');
    });

    it('应该隐藏内容当loading为true', () => {
      const { container } = render(<Button loading>Loading</Button>);
      const content = container.querySelector(`.${styles.content}`) as HTMLElement;
      expect(content).not.toBeNull();
      expect(content.className).toContain(styles.content);
    });

    it('应该隐藏左图标当loading为true', () => {
      const { container } = render(
        <Button loading leftIcon={<span>Icon</span>}>
          Loading
        </Button>
      );
      const leftIcon = container.querySelector(`.${styles.leftIcon}`);
      expect(leftIcon).toBeNull();
    });
  });

  describe('Disabled State', () => {
    it('应该禁用按钮', () => {
      render(<Button disabled>Disabled</Button>);
      expect(screen.getByRole('button')).toBeDisabled();
    });

    it('应该不触发onClick当disabled', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();
      render(
        <Button disabled onClick={handleClick}>
          Disabled
        </Button>
      );

      await user.click(screen.getByRole('button'));
      expect(handleClick).not.toHaveBeenCalled();
    });
  });

  describe('Icons', () => {
    it('应该渲染左图标', () => {
      const { container } = render(
        <Button leftIcon={<span data-testid="left-icon">←</span>}>With Left Icon</Button>
      );
      expect(screen.getByTestId('left-icon')).toBeInTheDocument();
      expect(container.querySelector(`.${styles.leftIcon}`)).not.toBeNull();
    });

    it('应该渲染右图标', () => {
      const { container } = render(
        <Button rightIcon={<span data-testid="right-icon">→</span>}>With Right Icon</Button>
      );
      expect(screen.getByTestId('right-icon')).toBeInTheDocument();
      expect(container.querySelector(`.${styles.rightIcon}`)).not.toBeNull();
    });

    it('应该同时渲染左右图标', () => {
      render(
        <Button
          leftIcon={<span data-testid="left-icon">←</span>}
          rightIcon={<span data-testid="right-icon">→</span>}
        >
          Both Icons
        </Button>
      );
      expect(screen.getByTestId('left-icon')).toBeInTheDocument();
      expect(screen.getByTestId('right-icon')).toBeInTheDocument();
    });
  });

  describe('Interactions', () => {
    it('应该触发onClick事件', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();
      render(<Button onClick={handleClick}>Click me</Button>);

      await user.click(screen.getByRole('button'));
      expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it('应该传递event对象给onClick', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();
      render(<Button onClick={handleClick}>Click me</Button>);

      await user.click(screen.getByRole('button'));
      expect(handleClick).toHaveBeenCalledWith(expect.any(Object));
    });

    it('应该支持submit type', () => {
      render(<Button type="submit">Submit</Button>);
      expect(screen.getByRole('button')).toHaveAttribute('type', 'submit');
    });

    it('应该支持reset type', () => {
      render(<Button type="reset">Reset</Button>);
      expect(screen.getByRole('button')).toHaveAttribute('type', 'reset');
    });
  });

  describe('Custom Props', () => {
    it('应该传递自定义className', () => {
      const { container } = render(<Button className="custom-class">Custom</Button>);
      const button = container.querySelector('button');
      expect(button?.className).toContain('custom-class');
    });

    it('应该传递data attributes', () => {
      render(<Button data-testid="custom-button">Custom</Button>);
      expect(screen.getByTestId('custom-button')).toBeInTheDocument();
    });

    it('应该传递aria attributes', () => {
      render(<Button aria-label="Custom Label">Custom</Button>);
      expect(screen.getByRole('button')).toHaveAttribute('aria-label', 'Custom Label');
    });
  });

  describe('Ref Forwarding', () => {
    it('应该支持ref转发', () => {
      const ref = vi.fn();
      render(<Button ref={ref}>With Ref</Button>);
      expect(ref).toHaveBeenCalledWith(expect.any(HTMLButtonElement));
    });
  });

  describe('Accessibility', () => {
    it('应该有正确的role', () => {
      render(<Button>Accessible</Button>);
      expect(screen.getByRole('button')).toBeInTheDocument();
    });

    it('应该有正确的disabled属性', () => {
      render(<Button disabled>Disabled</Button>);
      expect(screen.getByRole('button')).toHaveAttribute('disabled');
    });

    it('应该在loading时设置aria-busy', () => {
      render(<Button loading>Loading</Button>);
      expect(screen.getByRole('button')).toHaveAttribute('aria-busy', 'true');
    });

    it('应该在图标上设置aria-hidden', () => {
      const { container } = render(
        <Button leftIcon={<span>Icon</span>}>With Icon</Button>
      );
      const leftIcon = container.querySelector(`.${styles.leftIcon}`) as HTMLElement;
      expect(leftIcon).toHaveAttribute('aria-hidden', 'true');
    });
  });
});
