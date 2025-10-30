/**
 * Input Component Stories
 */

import type { Meta, StoryObj } from '@storybook/react';
import { Input } from './Input';

const meta = {
  title: 'Components/Input',
  component: Input,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['default', 'error', 'success', 'warning'],
      description: 'Input validation state variant',
    },
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg'],
      description: 'Input size',
    },
    type: {
      control: 'select',
      options: ['text', 'email', 'password', 'number', 'tel', 'url'],
      description: 'Input type',
    },
    fullWidth: {
      control: 'boolean',
      description: 'Whether input takes full width',
    },
    disabled: {
      control: 'boolean',
      description: 'Disabled state',
    },
    onChange: { action: 'changed' },
  },
} satisfies Meta<typeof Input>;

export default meta;
type Story = StoryObj<typeof meta>;

// Default input
export const Default: Story = {
  args: {
    placeholder: 'Enter text...',
  },
};

// With label
export const WithLabel: Story = {
  args: {
    label: 'Username',
    placeholder: 'Enter your username',
  },
};

// With helper text
export const WithHelperText: Story = {
  args: {
    label: 'Email',
    placeholder: 'user@example.com',
    helperText: 'We will never share your email with anyone else.',
  },
};

// Error state
export const WithError: Story = {
  args: {
    label: 'Password',
    type: 'password',
    placeholder: 'Enter password',
    error: 'Password must be at least 8 characters long',
  },
};

// Success state
export const Success: Story = {
  args: {
    label: 'Email',
    value: 'user@example.com',
    variant: 'success',
    helperText: 'Email is valid',
  },
};

// Warning state
export const Warning: Story = {
  args: {
    label: 'Username',
    value: 'user123',
    variant: 'warning',
    helperText: 'Username should not contain numbers',
  },
};

// Sizes
export const Small: Story = {
  args: {
    label: 'Small Input',
    placeholder: 'Small size',
    size: 'sm',
  },
};

export const Medium: Story = {
  args: {
    label: 'Medium Input',
    placeholder: 'Medium size',
    size: 'md',
  },
};

export const Large: Story = {
  args: {
    label: 'Large Input',
    placeholder: 'Large size',
    size: 'lg',
  },
};

// Types
export const Email: Story = {
  args: {
    label: 'Email',
    type: 'email',
    placeholder: 'user@example.com',
  },
};

export const Password: Story = {
  args: {
    label: 'Password',
    type: 'password',
    placeholder: '********',
  },
};

export const Number: Story = {
  args: {
    label: 'Age',
    type: 'number',
    placeholder: '0',
  },
};

// Disabled
export const Disabled: Story = {
  args: {
    label: 'Disabled Input',
    placeholder: 'Cannot edit',
    disabled: true,
  },
};

// With left icon
export const WithLeftIcon: Story = {
  args: {
    label: 'Search',
    placeholder: 'Search...',
    leftIcon: (
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
        <circle cx="8" cy="8" r="6" stroke="currentColor" strokeWidth="2" />
        <path d="M12.5 12.5L17 17" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      </svg>
    ),
  },
};

// With right icon
export const WithRightIcon: Story = {
  args: {
    label: 'Website',
    placeholder: 'https://example.com',
    rightIcon: (
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
        <path
          d="M7 10L9 12L13 8"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    ),
    variant: 'success',
  },
};

// With both icons
export const WithBothIcons: Story = {
  args: {
    label: 'Amount',
    placeholder: '0.00',
    type: 'number',
    leftIcon: (
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
        <text x="5" y="15" fontSize="14" fill="currentColor">$</text>
      </svg>
    ),
    rightIcon: (
      <span style={{ fontSize: '12px', color: 'currentColor' }}>USD</span>
    ),
  },
};

// Full width
export const FullWidth: Story = {
  args: {
    label: 'Full Width Input',
    placeholder: 'This input takes full width',
    fullWidth: true,
  },
  parameters: {
    layout: 'padded',
  },
};

// All variants showcase
export const AllVariants: Story = {
  render: () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', width: '400px' }}>
      <Input label="Default" placeholder="Default variant" variant="default" />
      <Input label="Error" placeholder="Error variant" variant="error" error="This field is required" />
      <Input label="Success" placeholder="Success variant" variant="success" helperText="Looks good!" />
      <Input label="Warning" placeholder="Warning variant" variant="warning" helperText="Please review" />
    </div>
  ),
  parameters: {
    layout: 'padded',
  },
};

// All sizes showcase
export const AllSizes: Story = {
  render: () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', width: '400px' }}>
      <Input label="Small" placeholder="Small size" size="sm" />
      <Input label="Medium" placeholder="Medium size" size="md" />
      <Input label="Large" placeholder="Large size" size="lg" />
    </div>
  ),
  parameters: {
    layout: 'padded',
  },
};

// Form example
export const FormExample: Story = {
  render: () => (
    <form style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', width: '400px' }}>
      <Input
        label="Full Name"
        placeholder="John Doe"
        required
      />
      <Input
        label="Email"
        type="email"
        placeholder="john@example.com"
        helperText="We'll never share your email"
        required
      />
      <Input
        label="Password"
        type="password"
        placeholder="********"
        helperText="Must be at least 8 characters"
        required
      />
      <Input
        label="Phone"
        type="tel"
        placeholder="+1 (555) 000-0000"
        leftIcon={
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <path
              d="M2 3C2 2.44772 2.44772 2 3 2H5.15287C5.64171 2 6.0589 2.35341 6.13927 2.8356L6.87858 7.27147C6.95075 7.70451 6.73206 8.13397 6.3394 8.3303L4.79126 9.10437C5.90756 11.8783 8.12168 14.0924 10.8956 15.2087L11.6697 13.6606C11.866 13.2679 12.2955 13.0492 12.7285 13.1214L17.1644 13.8607C17.6466 13.9411 18 14.3583 18 14.8471V17C18 17.5523 17.5523 18 17 18H15C7.8203 18 2 12.1797 2 5V3Z"
              fill="currentColor"
            />
          </svg>
        }
      />
    </form>
  ),
  parameters: {
    layout: 'padded',
  },
};

// Interactive example
export const Interactive: Story = {
  args: {
    label: 'Interactive Input',
    placeholder: 'Try typing...',
    helperText: 'This input is fully interactive',
  },
  parameters: {
    docs: {
      description: {
        story: 'Try typing in the input and changing its props using the controls below.',
      },
    },
  },
};
