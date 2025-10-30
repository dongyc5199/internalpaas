/**
 * Input Component Tests
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Input } from '../../../react-app/components/Input';
import styles from '../../../react-app/components/Input/Input.module.css';

describe('Input', () => {
  describe('Rendering', () => {
    it('应该渲染基本输入框', () => {
      render(<Input placeholder="Enter text" />);
      expect(screen.getByPlaceholderText('Enter text')).toBeInTheDocument();
    });

    it('应该渲染默认type为text', () => {
      render(<Input />);
      const input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('type', 'text');
    });

    it('应该渲染默认size为md', () => {
      const { container } = render(<Input />);
      const wrapper = container.querySelector(`.${styles['inputWrapper']}`);
      expect(wrapper?.className).toContain(styles['size-md']);
    });

    it('应该渲染默认variant为default', () => {
      const { container } = render(<Input />);
      const wrapper = container.querySelector(`.${styles['inputWrapper']}`);
      expect(wrapper?.className).toContain(styles['variant-default']);
    });
  });

  describe('Label', () => {
    it('应该渲染label', () => {
      render(<Input label="Username" />);
      expect(screen.getByLabelText('Username')).toBeInTheDocument();
    });

    it('应该关联label和input', () => {
      render(<Input label="Email" id="email-input" />);
      const label = screen.getByText('Email');
      const input = screen.getByLabelText('Email');
      expect(label).toHaveAttribute('for', 'email-input');
      expect(input).toHaveAttribute('id', 'email-input');
    });

    it('应该生成唯一ID如果未提供', () => {
      const { container } = render(<Input label="Test" />);
      const input = container.querySelector('input');
      expect(input).toHaveAttribute('id');
      expect(input?.id).toMatch(/^input-/);
    });
  });

  describe('Sizes', () => {
    it('应该渲染small size', () => {
      const { container } = render(<Input size="sm" />);
      const wrapper = container.querySelector(`.${styles['inputWrapper']}`);
      expect(wrapper?.className).toContain(styles['size-sm']);
    });

    it('应该渲染medium size', () => {
      const { container } = render(<Input size="md" />);
      const wrapper = container.querySelector(`.${styles['inputWrapper']}`);
      expect(wrapper?.className).toContain(styles['size-md']);
    });

    it('应该渲染large size', () => {
      const { container } = render(<Input size="lg" />);
      const wrapper = container.querySelector(`.${styles['inputWrapper']}`);
      expect(wrapper?.className).toContain(styles['size-lg']);
    });
  });

  describe('Variants', () => {
    it('应该渲染default variant', () => {
      const { container } = render(<Input variant="default" />);
      const wrapper = container.querySelector(`.${styles['inputWrapper']}`);
      expect(wrapper?.className).toContain(styles['variant-default']);
    });

    it('应该渲染error variant', () => {
      const { container } = render(<Input variant="error" />);
      const wrapper = container.querySelector(`.${styles['inputWrapper']}`);
      expect(wrapper?.className).toContain(styles['variant-error']);
    });

    it('应该渲染success variant', () => {
      const { container } = render(<Input variant="success" />);
      const wrapper = container.querySelector(`.${styles['inputWrapper']}`);
      expect(wrapper?.className).toContain(styles['variant-success']);
    });

    it('应该渲染warning variant', () => {
      const { container } = render(<Input variant="warning" />);
      const wrapper = container.querySelector(`.${styles['inputWrapper']}`);
      expect(wrapper?.className).toContain(styles['variant-warning']);
    });
  });

  describe('Full Width', () => {
    it('应该渲染fullWidth样式', () => {
      const { container } = render(<Input fullWidth />);
      const containerElem = container.querySelector(`.${styles['container']}`);
      expect(containerElem?.className).toContain(styles['fullWidth']);
    });

    it('应该不渲染fullWidth样式当prop为false', () => {
      const { container } = render(<Input fullWidth={false} />);
      const containerElem = container.querySelector(`.${styles['container']}`);
      expect(containerElem?.className).not.toContain(styles['fullWidth']);
    });
  });

  describe('Disabled State', () => {
    it('应该禁用输入框', () => {
      render(<Input disabled />);
      expect(screen.getByRole('textbox')).toBeDisabled();
    });

    it('应该不触发onChange当disabled', async () => {
      const user = userEvent.setup();
      const handleChange = vi.fn();
      render(<Input disabled onChange={handleChange} />);

      await user.type(screen.getByRole('textbox'), 'test');
      expect(handleChange).not.toHaveBeenCalled();
    });

    it('应该添加disabled样式到wrapper', () => {
      const { container } = render(<Input disabled />);
      const wrapper = container.querySelector(`.${styles['inputWrapper']}`);
      expect(wrapper?.className).toContain(styles['disabled']);
    });
  });

  describe('Icons', () => {
    it('应该渲染左图标', () => {
      const { container } = render(
        <Input leftIcon={<span data-testid="left-icon">🔍</span>} />
      );
      expect(screen.getByTestId('left-icon')).toBeInTheDocument();
      expect(container.querySelector(`.${styles['leftIcon']}`)).not.toBeNull();
    });

    it('应该渲染右图标', () => {
      const { container } = render(
        <Input rightIcon={<span data-testid="right-icon">✓</span>} />
      );
      expect(screen.getByTestId('right-icon')).toBeInTheDocument();
      expect(container.querySelector(`.${styles['rightIcon']}`)).not.toBeNull();
    });

    it('应该同时渲染左右图标', () => {
      render(
        <Input
          leftIcon={<span data-testid="left-icon">🔍</span>}
          rightIcon={<span data-testid="right-icon">✓</span>}
        />
      );
      expect(screen.getByTestId('left-icon')).toBeInTheDocument();
      expect(screen.getByTestId('right-icon')).toBeInTheDocument();
    });

    it('应该在图标上设置aria-hidden', () => {
      const { container } = render(
        <Input leftIcon={<span>Icon</span>} />
      );
      const leftIcon = container.querySelector(`.${styles['leftIcon']}`) as HTMLElement;
      expect(leftIcon).toHaveAttribute('aria-hidden', 'true');
    });
  });

  describe('Helper Text', () => {
    it('应该显示helper text', () => {
      render(<Input helperText="Enter your username" />);
      expect(screen.getByText('Enter your username')).toBeInTheDocument();
    });

    it('应该关联helper text和input', () => {
      render(<Input helperText="Enter your username" id="username" />);
      const input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('aria-describedby', 'username-helper');
    });
  });

  describe('Error State', () => {
    it('应该显示error消息', () => {
      render(<Input error="This field is required" />);
      expect(screen.getByText('This field is required')).toBeInTheDocument();
    });

    it('应该设置variant为error当有error prop', () => {
      const { container } = render(<Input variant="success" error="Error message" />);
      const wrapper = container.querySelector(`.${styles['inputWrapper']}`);
      expect(wrapper?.className).toContain(styles['variant-error']);
    });

    it('应该设置aria-invalid为true当有error', () => {
      render(<Input error="Error message" />);
      expect(screen.getByRole('textbox')).toHaveAttribute('aria-invalid', 'true');
    });

    it('应该关联error消息和input', () => {
      render(<Input error="Error message" id="test-input" />);
      const input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('aria-describedby', 'test-input-error');
    });

    it('应该在有error时不显示helper text', () => {
      render(<Input helperText="Helper text" error="Error message" />);
      expect(screen.queryByText('Helper text')).not.toBeInTheDocument();
      expect(screen.getByText('Error message')).toBeInTheDocument();
    });

    it('应该设置error text的role为alert', () => {
      render(<Input error="Error message" />);
      const errorText = screen.getByRole('alert');
      expect(errorText).toHaveTextContent('Error message');
    });
  });

  describe('Interactions', () => {
    it('应该触发onChange事件', async () => {
      const user = userEvent.setup();
      const handleChange = vi.fn();
      render(<Input onChange={handleChange} />);

      await user.type(screen.getByRole('textbox'), 'test');
      expect(handleChange).toHaveBeenCalled();
    });

    it('应该更新value', async () => {
      const user = userEvent.setup();
      render(<Input />);
      const input = screen.getByRole('textbox') as HTMLInputElement;

      await user.type(input, 'test value');
      expect(input.value).toBe('test value');
    });

    it('应该支持受控组件', () => {
      const { rerender } = render(<Input value="initial" onChange={() => {}} />);
      const input = screen.getByRole('textbox') as HTMLInputElement;
      expect(input.value).toBe('initial');

      rerender(<Input value="updated" onChange={() => {}} />);
      expect(input.value).toBe('updated');
    });

    it('应该支持不同的input类型', () => {
      const { rerender } = render(<Input type="email" />);
      expect(screen.getByRole('textbox')).toHaveAttribute('type', 'email');

      rerender(<Input type="password" />);
      const input = document.querySelector('input[type="password"]');
      expect(input).toBeInTheDocument();
    });
  });

  describe('Custom Props', () => {
    it('应该传递自定义className到input', () => {
      const { container } = render(<Input className="custom-input" />);
      const input = container.querySelector('input');
      expect(input?.className).toContain('custom-input');
    });

    it('应该传递自定义containerClassName', () => {
      const { container } = render(<Input containerClassName="custom-container" />);
      const containerElem = container.querySelector(`.${styles['container']}`);
      expect(containerElem?.className).toContain('custom-container');
    });

    it('应该传递自定义labelClassName', () => {
      const { container } = render(<Input label="Test" labelClassName="custom-label" />);
      const label = container.querySelector('label');
      expect(label?.className).toContain('custom-label');
    });

    it('应该传递placeholder', () => {
      render(<Input placeholder="Enter text" />);
      expect(screen.getByPlaceholderText('Enter text')).toBeInTheDocument();
    });

    it('应该传递name属性', () => {
      render(<Input name="username" />);
      expect(screen.getByRole('textbox')).toHaveAttribute('name', 'username');
    });
  });

  describe('Ref Forwarding', () => {
    it('应该支持ref转发', () => {
      const ref = vi.fn();
      render(<Input ref={ref} />);
      expect(ref).toHaveBeenCalledWith(expect.any(HTMLInputElement));
    });

    it('应该允许通过ref访问input', () => {
      const ref = { current: null as HTMLInputElement | null };
      render(<Input ref={ref} />);
      expect(ref.current).toBeInstanceOf(HTMLInputElement);
      expect(ref.current?.tagName).toBe('INPUT');
    });
  });

  describe('Accessibility', () => {
    it('应该有正确的role', () => {
      render(<Input />);
      expect(screen.getByRole('textbox')).toBeInTheDocument();
    });

    it('应该在有label时关联label', () => {
      render(<Input label="Username" id="username" />);
      const input = screen.getByLabelText('Username');
      expect(input).toHaveAttribute('id', 'username');
    });

    it('应该在有error时设置aria-invalid', () => {
      render(<Input error="Error" />);
      expect(screen.getByRole('textbox')).toHaveAttribute('aria-invalid', 'true');
    });

    it('应该在没有error时不设置aria-invalid', () => {
      render(<Input />);
      expect(screen.getByRole('textbox')).not.toHaveAttribute('aria-invalid', 'true');
    });

    it('应该在有helperText时设置aria-describedby', () => {
      render(<Input helperText="Helper" id="test" />);
      expect(screen.getByRole('textbox')).toHaveAttribute('aria-describedby', 'test-helper');
    });

    it('应该在有error时设置aria-describedby指向error', () => {
      render(<Input error="Error" id="test" />);
      expect(screen.getByRole('textbox')).toHaveAttribute('aria-describedby', 'test-error');
    });
  });
});
