/**
 * Select Component Tests
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Select } from '../../../react-app/components/Select';
import styles from '../../../react-app/components/Select/Select.module.css';

describe('Select', () => {
  const testOptions = [
    { value: '1', label: 'Option 1' },
    { value: '2', label: 'Option 2' },
    { value: '3', label: 'Option 3' },
  ];

  describe('Rendering', () => {
    it('应该渲染基本下拉框', () => {
      render(<Select options={testOptions} />);
      expect(screen.getByRole('combobox')).toBeInTheDocument();
    });

    it('应该渲染默认size为md', () => {
      const { container } = render(<Select options={testOptions} />);
      const wrapper = container.querySelector('[class*=\"selectWrapper\"]');
      expect(wrapper?.className).toMatch(/size-md/);
    });

    it('应该渲染默认variant为default', () => {
      const { container } = render(<Select options={testOptions} />);
      const wrapper = container.querySelector('[class*=\"selectWrapper\"]');
      expect(wrapper?.className).toMatch(/variant-default/);
    });

    it('应该渲染chevron图标', () => {
      const { container } = render(<Select options={testOptions} />);
      const chevron = container.querySelector('[class*=\"chevronIcon\"]');
      expect(chevron).toBeInTheDocument();
    });
  });

  describe('Label', () => {
    it('应该渲染label', () => {
      render(<Select label="Country" options={testOptions} />);
      expect(screen.getByLabelText('Country')).toBeInTheDocument();
    });

    it('应该关联label和select', () => {
      render(<Select label="Country" id="country-select" options={testOptions} />);
      const label = screen.getByText('Country');
      const select = screen.getByLabelText('Country');
      expect(label).toHaveAttribute('for', 'country-select');
      expect(select).toHaveAttribute('id', 'country-select');
    });

    it('应该生成唯一ID如果未提供', () => {
      const { container } = render(<Select label="Test" options={testOptions} />);
      const select = container.querySelector('select');
      expect(select).toHaveAttribute('id');
      expect(select?.id).toMatch(/^select-/);
    });
  });

  describe('Sizes', () => {
    it('应该渲染small size', () => {
      const { container } = render(<Select size="sm" options={testOptions} />);
      const wrapper = container.querySelector('[class*=\"selectWrapper\"]');
      expect(wrapper?.className).toMatch(/size-sm/);
    });

    it('应该渲染medium size', () => {
      const { container } = render(<Select size="md" options={testOptions} />);
      const wrapper = container.querySelector('[class*=\"selectWrapper\"]');
      expect(wrapper?.className).toMatch(/size-md/);
    });

    it('应该渲染large size', () => {
      const { container } = render(<Select size="lg" options={testOptions} />);
      const wrapper = container.querySelector('[class*=\"selectWrapper\"]');
      expect(wrapper?.className).toMatch(/size-lg/);
    });
  });

  describe('Variants', () => {
    it('应该渲染default variant', () => {
      const { container } = render(<Select variant="default" options={testOptions} />);
      const wrapper = container.querySelector('[class*=\"selectWrapper\"]');
      expect(wrapper?.className).toMatch(/variant-default/);
    });

    it('应该渲染error variant', () => {
      const { container } = render(<Select variant="error" options={testOptions} />);
      const wrapper = container.querySelector('[class*=\"selectWrapper\"]');
      expect(wrapper?.className).toMatch(/variant-error/);
    });

    it('应该渲染success variant', () => {
      const { container } = render(<Select variant="success" options={testOptions} />);
      const wrapper = container.querySelector('[class*=\"selectWrapper\"]');
      expect(wrapper?.className).toMatch(/variant-success/);
    });

    it('应该渲染warning variant', () => {
      const { container } = render(<Select variant="warning" options={testOptions} />);
      const wrapper = container.querySelector('[class*=\"selectWrapper\"]');
      expect(wrapper?.className).toMatch(/variant-warning/);
    });
  });

  describe('Full Width', () => {
    it('应该渲染fullWidth样式', () => {
      const { container } = render(<Select fullWidth options={testOptions} />);
      const containerElem = container.querySelector('[class*=\"container\"]');
      expect(containerElem?.className).toMatch(/fullWidth/);
    });

    it('应该不渲染fullWidth样式当prop为false', () => {
      const { container } = render(<Select fullWidth={false} options={testOptions} />);
      const containerElem = container.querySelector('[class*=\"container\"]');
      expect(containerElem?.className).not.toMatch(/fullWidth/);
    });
  });

  describe('Disabled State', () => {
    it('应该禁用下拉框', () => {
      render(<Select disabled options={testOptions} />);
      expect(screen.getByRole('combobox')).toBeDisabled();
    });

    it('应该不触发onChange当disabled', async () => {
      const user = userEvent.setup();
      const handleChange = vi.fn();
      render(<Select disabled onChange={handleChange} options={testOptions} />);

      const select = screen.getByRole('combobox');
      await user.selectOptions(select, '2');
      expect(handleChange).not.toHaveBeenCalled();
    });

    it('应该添加disabled样式到wrapper', () => {
      const { container } = render(<Select disabled options={testOptions} />);
      const wrapper = container.querySelector('[class*=\"selectWrapper\"]');
      expect(wrapper?.className).toMatch(/disabled/);
    });
  });

  describe('Icons', () => {
    it('应该渲染左图标', () => {
      const { container } = render(
        <Select leftIcon={<span data-testid="left-icon">📍</span>} options={testOptions} />
      );
      expect(screen.getByTestId('left-icon')).toBeInTheDocument();
      expect(container.querySelector('[class*=\"leftIcon\"]')).not.toBeNull();
    });

    it('应该在图标上设置aria-hidden', () => {
      const { container } = render(
        <Select leftIcon={<span data-testid="test-icon">Icon</span>} options={testOptions} />
      );
      // The icon wrapper span should have aria-hidden
      const iconWrapper = container.querySelector('span[aria-hidden="true"]');
      expect(iconWrapper).toBeInTheDocument();
      // Verify it contains the icon
      expect(iconWrapper?.textContent).toBe('Icon');
    });

    it('应该在chevron图标上设置aria-hidden', () => {
      const { container } = render(<Select options={testOptions} />);
      const chevron = container.querySelector('[class*=\"chevronIcon\"]') as HTMLElement;
      expect(chevron).toHaveAttribute('aria-hidden', 'true');
    });
  });

  describe('Options', () => {
    it('应该渲染所有选项', () => {
      render(<Select options={testOptions} />);
      expect(screen.getByRole('option', { name: 'Option 1' })).toBeInTheDocument();
      expect(screen.getByRole('option', { name: 'Option 2' })).toBeInTheDocument();
      expect(screen.getByRole('option', { name: 'Option 3' })).toBeInTheDocument();
    });

    it('应该渲染placeholder选项', () => {
      render(<Select placeholder="Select an option" options={testOptions} />);
      expect(screen.getByRole('option', { name: 'Select an option' })).toBeInTheDocument();
    });

    it('应该设置placeholder选项为disabled', () => {
      render(<Select placeholder="Select an option" options={testOptions} />);
      const placeholderOption = screen.getByRole('option', { name: 'Select an option' }) as HTMLOptionElement;
      expect(placeholderOption).toBeDisabled();
      expect(placeholderOption.value).toBe('');
    });

    it('应该渲染disabled选项', () => {
      const optionsWithDisabled = [
        { value: '1', label: 'Option 1' },
        { value: '2', label: 'Option 2', disabled: true },
        { value: '3', label: 'Option 3' },
      ];
      render(<Select options={optionsWithDisabled} />);
      const option2 = screen.getByRole('option', { name: 'Option 2' }) as HTMLOptionElement;
      expect(option2).toBeDisabled();
    });

    it('应该设置正确的option value', () => {
      render(<Select options={testOptions} />);
      const option1 = screen.getByRole('option', { name: 'Option 1' }) as HTMLOptionElement;
      expect(option1.value).toBe('1');
    });

    it('应该支持children选项', () => {
      render(
        <Select options={testOptions}>
          <option value="custom">Custom Option</option>
        </Select>
      );
      expect(screen.getByRole('option', { name: 'Custom Option' })).toBeInTheDocument();
    });
  });

  describe('Helper Text', () => {
    it('应该显示helper text', () => {
      render(<Select helperText="Choose your country" options={testOptions} />);
      expect(screen.getByText('Choose your country')).toBeInTheDocument();
    });

    it('应该关联helper text和select', () => {
      render(<Select helperText="Choose your country" id="country" options={testOptions} />);
      const select = screen.getByRole('combobox');
      expect(select).toHaveAttribute('aria-describedby', 'country-helper');
    });
  });

  describe('Error State', () => {
    it('应该显示error消息', () => {
      render(<Select error="This field is required" options={testOptions} />);
      expect(screen.getByText('This field is required')).toBeInTheDocument();
    });

    it('应该设置variant为error当有error prop', () => {
      const { container } = render(<Select variant="success" error="Error message" options={testOptions} />);
      const wrapper = container.querySelector('[class*=\"selectWrapper\"]');
      expect(wrapper?.className).toMatch(/variant-error/);
    });

    it('应该设置aria-invalid为true当有error', () => {
      render(<Select error="Error message" options={testOptions} />);
      expect(screen.getByRole('combobox')).toHaveAttribute('aria-invalid', 'true');
    });

    it('应该关联error消息和select', () => {
      render(<Select error="Error message" id="test-select" options={testOptions} />);
      const select = screen.getByRole('combobox');
      expect(select).toHaveAttribute('aria-describedby', 'test-select-error');
    });

    it('应该在有error时不显示helper text', () => {
      render(<Select helperText="Helper text" error="Error message" options={testOptions} />);
      expect(screen.queryByText('Helper text')).not.toBeInTheDocument();
      expect(screen.getByText('Error message')).toBeInTheDocument();
    });

    it('应该设置error text的role为alert', () => {
      render(<Select error="Error message" options={testOptions} />);
      const errorText = screen.getByRole('alert');
      expect(errorText).toHaveTextContent('Error message');
    });
  });

  describe('Interactions', () => {
    it('应该触发onChange事件', async () => {
      const user = userEvent.setup();
      const handleChange = vi.fn();
      render(<Select onChange={handleChange} options={testOptions} />);

      const select = screen.getByRole('combobox');
      await user.selectOptions(select, '2');
      expect(handleChange).toHaveBeenCalled();
    });

    it('应该更新value', async () => {
      const user = userEvent.setup();
      render(<Select options={testOptions} />);
      const select = screen.getByRole('combobox') as HTMLSelectElement;

      await user.selectOptions(select, '2');
      expect(select.value).toBe('2');
    });

    it('应该支持受控组件', () => {
      const { rerender } = render(<Select value="1" onChange={() => {}} options={testOptions} />);
      const select = screen.getByRole('combobox') as HTMLSelectElement;
      expect(select.value).toBe('1');

      rerender(<Select value="2" onChange={() => {}} options={testOptions} />);
      expect(select.value).toBe('2');
    });
  });

  describe('Custom Props', () => {
    it('应该传递自定义className到select', () => {
      const { container } = render(<Select className="custom-select" options={testOptions} />);
      const select = container.querySelector('select');
      expect(select?.className).toContain('custom-select');
    });

    it('应该传递自定义containerClassName', () => {
      const { container } = render(<Select containerClassName="custom-container" options={testOptions} />);
      const containerElem = container.querySelector('[class*=\"container\"]');
      expect(containerElem?.className).toContain('custom-container');
    });

    it('应该传递自定义labelClassName', () => {
      const { container } = render(<Select label="Test" labelClassName="custom-label" options={testOptions} />);
      const label = container.querySelector('label');
      expect(label?.className).toContain('custom-label');
    });

    it('应该传递name属性', () => {
      render(<Select name="country" options={testOptions} />);
      expect(screen.getByRole('combobox')).toHaveAttribute('name', 'country');
    });

    it('应该传递multiple属性', () => {
      render(<Select multiple options={testOptions} />);
      expect(screen.getByRole('listbox')).toHaveAttribute('multiple');
    });
  });

  describe('Ref Forwarding', () => {
    it('应该支持ref转发', () => {
      const ref = vi.fn();
      render(<Select ref={ref} options={testOptions} />);
      expect(ref).toHaveBeenCalledWith(expect.any(HTMLSelectElement));
    });

    it('应该允许通过ref访问select', () => {
      const ref = { current: null as HTMLSelectElement | null };
      render(<Select ref={ref} options={testOptions} />);
      expect(ref.current).toBeInstanceOf(HTMLSelectElement);
      expect(ref.current?.tagName).toBe('SELECT');
    });
  });

  describe('Accessibility', () => {
    it('应该有正确的role', () => {
      render(<Select options={testOptions} />);
      expect(screen.getByRole('combobox')).toBeInTheDocument();
    });

    it('应该在有label时关联label', () => {
      render(<Select label="Country" id="country" options={testOptions} />);
      const select = screen.getByLabelText('Country');
      expect(select).toHaveAttribute('id', 'country');
    });

    it('应该在有error时设置aria-invalid', () => {
      render(<Select error="Error" options={testOptions} />);
      expect(screen.getByRole('combobox')).toHaveAttribute('aria-invalid', 'true');
    });

    it('应该在没有error时不设置aria-invalid为true', () => {
      render(<Select options={testOptions} />);
      const select = screen.getByRole('combobox');
      expect(select.getAttribute('aria-invalid')).toBe('false');
    });

    it('应该在有helperText时设置aria-describedby', () => {
      render(<Select helperText="Helper" id="test" options={testOptions} />);
      expect(screen.getByRole('combobox')).toHaveAttribute('aria-describedby', 'test-helper');
    });

    it('应该在有error时设置aria-describedby指向error', () => {
      render(<Select error="Error" id="test" options={testOptions} />);
      expect(screen.getByRole('combobox')).toHaveAttribute('aria-describedby', 'test-error');
    });

    it('应该在装饰性图标上设置aria-hidden', () => {
      const { container } = render(<Select leftIcon={<span>Icon</span>} options={testOptions} />);
      // Find all spans with aria-hidden="true"
      const ariaHiddenSpans = container.querySelectorAll('span[aria-hidden="true"]');
      // Should have 2: leftIcon wrapper and chevron wrapper
      expect(ariaHiddenSpans.length).toBeGreaterThanOrEqual(2);
    });

    it('应该为disabled label设置正确的样式', () => {
      const { container } = render(<Select label="Test" disabled options={testOptions} />);
      const label = container.querySelector('label');
      expect(label?.className).toMatch(/labelDisabled/);
    });
  });
});
