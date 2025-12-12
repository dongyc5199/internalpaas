/**
 * Input Component Tests (Fixed Version)
 * 专注于功能测试，避免CSS Module类名依赖
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Input } from '../../../react-app/components/Input';

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
      const wrapper = container.querySelector('[class*="inputWrapper"]');
      expect(wrapper?.className).toMatch(/size-md/);
    });

    it('应该渲染默认variant为default', () => {
      const { container } = render(<Input />);
      const wrapper = container.querySelector('[class*="inputWrapper"]');
      expect(wrapper?.className).toMatch(/variant-default/);
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
      const wrapper = container.querySelector('[class*="inputWrapper"]');
      expect(wrapper?.className).toMatch(/size-sm/);
    });

    it('应该渲染medium size', () => {
      const { container } = render(<Input size="md" />);
      const wrapper = container.querySelector('[class*="inputWrapper"]');
      expect(wrapper?.className).toMatch(/size-md/);
    });

    it('应该渲染large size', () => {
      const { container } = render(<Input size="lg" />);
      const wrapper = container.querySelector('[class*="inputWrapper"]');
      expect(wrapper?.className).toMatch(/size-lg/);
    });
  });

  describe('Variants', () => {
    it('应该渲染default variant', () => {
      const { container } = render(<Input variant="default" />);
      const wrapper = container.querySelector('[class*="inputWrapper"]');
      expect(wrapper?.className).toMatch(/variant-default/);
    });

    it('应该渲染error variant', () => {
      const { container } = render(<Input variant="error" />);
      const wrapper = container.querySelector('[class*="inputWrapper"]');
      expect(wrapper?.className).toMatch(/variant-error/);
    });

    it('应该渲染success variant', () => {
      const { container } = render(<Input variant="success" />);
      const wrapper = container.querySelector('[class*="inputWrapper"]');
      expect(wrapper?.className).toMatch(/variant-success/);
    });

    it('应该渲染warning variant', () => {
      const { container } = render(<Input variant="warning" />);
      const wrapper = container.querySelector('[class*="inputWrapper"]');
      expect(wrapper?.className).toMatch(/variant-warning/);
    });
  });

  describe('Full Width', () => {
    it('应该渲染fullWidth样式', () => {
      const { container } = render(<Input fullWidth />);
      const containerElem = container.querySelector('[class*="container"]');
      expect(containerElem?.className).toMatch(/fullWidth/);
    });

    it('应该不渲染fullWidth样式当prop为false', () => {
      const { container } = render(<Input fullWidth={false} />);
      const containerElem = container.querySelector('[class*="container"]');
      expect(containerElem?.className).not.toMatch(/fullWidth/);
    });
  });

  describe('Disabled State', () => {
    it('应该禁用输入框', () => {
      render(<Input disabled />);
      const input = screen.getByRole('textbox');
      expect(input).toBeDisabled();
    });

    it('应该渲染disabled样式', () => {
      const { container } = render(<Input disabled />);
      const wrapper = container.querySelector('[class*="inputWrapper"]');
      expect(wrapper?.className).toMatch(/disabled/);
    });

    it('应该不触发onChange当disabled', async () => {
      const handleChange = vi.fn();
      render(<Input disabled onChange={handleChange} />);
      const input = screen.getByRole('textbox');

      // 尝试输入不应触发onChange
      await userEvent.type(input, 'test');
      expect(handleChange).not.toHaveBeenCalled();
    });
  });

  describe('Icons', () => {
    it('应该渲染leftIcon', () => {
      const { container } = render(<Input leftIcon={<span>Icon</span>} />);
      expect(container.querySelector('[class*="leftIcon"]')).not.toBeNull();
    });

    it('应该渲染rightIcon', () => {
      const { container } = render(<Input rightIcon={<span>Icon</span>} />);
      expect(container.querySelector('[class*="rightIcon"]')).not.toBeNull();
    });
  });

  describe('Helper Text', () => {
    it('应该渲染helper text', () => {
      render(<Input helperText="This is helper text" />);
      expect(screen.getByText('This is helper text')).toBeInTheDocument();
    });

    it('应该关联helper text和input', () => {
      render(<Input helperText="Helper" id="test-input" />);
      const input = screen.getByRole('textbox');
      const helperTextId = input.getAttribute('aria-describedby');
      expect(helperTextId).toBeTruthy();
      expect(document.getElementById(helperTextId!)).toHaveTextContent('Helper');
    });
  });

  describe('Error State', () => {
    it('应该渲染error消息', () => {
      render(<Input error="This is an error" />);
      expect(screen.getByText('This is an error')).toBeInTheDocument();
    });

    it('应该设置variant为error当有error prop', () => {
      const { container } = render(<Input error="Error message" />);
      const wrapper = container.querySelector('[class*="inputWrapper"]');
      expect(wrapper?.className).toMatch(/variant-error/);
    });

    it('应该设置aria-invalid为true当有error', () => {
      render(<Input error="Error" />);
      const input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('aria-invalid', 'true');
    });

    it('应该关联error消息和input', () => {
      render(<Input error="Error message" id="test-input" />);
      const input = screen.getByRole('textbox');
      const errorTextId = input.getAttribute('aria-describedby');
      expect(errorTextId).toBeTruthy();
      expect(document.getElementById(errorTextId!)).toHaveTextContent('Error message');
    });
  });

  describe('Interactions', () => {
    it('应该触发onChange事件', async () => {
      const handleChange = vi.fn();
      render(<Input onChange={handleChange} />);
      const input = screen.getByRole('textbox');

      await userEvent.type(input, 'test');
      expect(handleChange).toHaveBeenCalled();
    });

    it('应该更新value', async () => {
      render(<Input />);
      const input = screen.getByRole('textbox') as HTMLInputElement;

      await userEvent.type(input, 'test value');
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
      let input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('type', 'email');

      rerender(<Input type="password" />);
      input = document.querySelector('input[type="password"]')!;
      expect(input).toHaveAttribute('type', 'password');
    });
  });

  describe('Custom Props', () => {
    it('应该传递className', () => {
      render(<Input className="custom-class" />);
      const input = screen.getByRole('textbox');
      expect(input.className).toContain('custom-class');
    });

    it('应该传递placeholder', () => {
      render(<Input placeholder="Enter something" />);
      expect(screen.getByPlaceholderText('Enter something')).toBeInTheDocument();
    });

    it('应该传递name属性', () => {
      render(<Input name="username" />);
      const input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('name', 'username');
    });
  });

  describe('Accessibility', () => {
    it('应该有正确的role', () => {
      render(<Input />);
      expect(screen.getByRole('textbox')).toBeInTheDocument();
    });

    it('应该在有error时设置aria-invalid', () => {
      render(<Input error="Error message" />);
      const input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('aria-invalid', 'true');
    });

    it('应该在没有error时不设置aria-invalid', () => {
      render(<Input />);
      const input = screen.getByRole('textbox');
      expect(input).not.toHaveAttribute('aria-invalid', 'true');
    });

    it('应该在有helperText时设置aria-describedby', () => {
      render(<Input helperText="Helper text" />);
      const input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('aria-describedby');
    });

    it('应该在有error时设置aria-describedby指向error', () => {
      render(<Input error="Error message" />);
      const input = screen.getByRole('textbox');
      const describedBy = input.getAttribute('aria-describedby');
      expect(describedBy).toBeTruthy();
      expect(document.getElementById(describedBy!)).toHaveTextContent('Error message');
    });
  });

  describe('Forwarded Ref', () => {
    it('应该将ref转发到input元素', () => {
      const ref = { current: null as HTMLInputElement | null };
      render(<Input ref={ref} />);
      expect(ref.current).toBeInstanceOf(HTMLInputElement);
    });

    it('应该允许通过ref focus输入框', () => {
      const ref = { current: null as HTMLInputElement | null };
      render(<Input ref={ref} />);
      ref.current?.focus();
      expect(document.activeElement).toBe(ref.current);
    });
  });
});
