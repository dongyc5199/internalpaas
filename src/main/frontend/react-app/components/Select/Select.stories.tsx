/**
 * Select Component Stories
 */

import type { Meta, StoryObj } from '@storybook/react';
import { Select } from './Select';

const meta = {
  title: 'Components/Select',
  component: Select,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg'],
      description: 'Select size',
    },
    variant: {
      control: 'select',
      options: ['default', 'error', 'success', 'warning'],
      description: 'Select validation state',
    },
    fullWidth: {
      control: 'boolean',
      description: 'Whether select takes full width',
    },
    disabled: {
      control: 'boolean',
      description: 'Disabled state',
    },
    required: {
      control: 'boolean',
      description: 'Required field',
    },
  },
} satisfies Meta<typeof Select>;

export default meta;
type Story = StoryObj<typeof meta>;

// Sample options for stories
const sampleOptions = [
  { value: '', label: '请选择...', disabled: true },
  { value: 'option1', label: '选项 1' },
  { value: 'option2', label: '选项 2' },
  { value: 'option3', label: '选项 3' },
  { value: 'option4', label: '选项 4', disabled: true },
  { value: 'option5', label: '选项 5' },
];

const countryOptions = [
  { value: '', label: 'Select country...' },
  { value: 'cn', label: '🇨🇳 China' },
  { value: 'us', label: '🇺🇸 United States' },
  { value: 'jp', label: '🇯🇵 Japan' },
  { value: 'uk', label: '🇬🇧 United Kingdom' },
  { value: 'de', label: '🇩🇪 Germany' },
];

// Default select
export const Default: Story = {
  args: {
    label: '选择选项',
    options: sampleOptions,
    size: 'md',
  },
};

// Different sizes
export const Small: Story = {
  args: {
    label: 'Small Select',
    options: sampleOptions,
    size: 'sm',
  },
};

export const Medium: Story = {
  args: {
    label: 'Medium Select',
    options: sampleOptions,
    size: 'md',
  },
};

export const Large: Story = {
  args: {
    label: 'Large Select',
    options: sampleOptions,
    size: 'lg',
  },
};

// Variants / States
export const WithError: Story = {
  args: {
    label: '国家',
    options: countryOptions,
    error: '请选择一个有效的国家',
    variant: 'error',
  },
};

export const WithSuccess: Story = {
  args: {
    label: '国家',
    options: countryOptions,
    value: 'cn',
    variant: 'success',
    helperText: '选择成功',
  },
};

export const WithWarning: Story = {
  args: {
    label: '区域',
    options: sampleOptions,
    variant: 'warning',
    helperText: '此选项将在下个版本中移除',
  },
};

export const Disabled: Story = {
  args: {
    label: '禁用的选择框',
    options: sampleOptions,
    disabled: true,
    value: 'option2',
  },
};

export const Required: Story = {
  args: {
    label: '必填字段',
    options: sampleOptions,
    required: true,
    helperText: '此字段为必填项',
  },
};

// Full width
export const FullWidth: Story = {
  args: {
    label: '全宽选择框',
    options: countryOptions,
    fullWidth: true,
  },
  parameters: {
    layout: 'padded',
  },
};

// With helper text
export const WithHelperText: Story = {
  args: {
    label: '首选语言',
    options: [
      { value: '', label: 'Select language...' },
      { value: 'zh', label: '中文' },
      { value: 'en', label: 'English' },
      { value: 'ja', label: '日本語' },
      { value: 'ko', label: '한국어' },
    ],
    helperText: '选择您的首选显示语言',
  },
};

// With icon
export const WithIcon: Story = {
  args: {
    label: '位置',
    options: countryOptions,
    leftIcon: '📍',
  },
};

// Complex example with all features
export const ComplexExample: Story = {
  args: {
    label: '部署环境',
    options: [
      { value: '', label: '选择环境...' },
      { value: 'dev', label: '开发环境 (Development)' },
      { value: 'test', label: '测试环境 (Testing)' },
      { value: 'staging', label: '预发布环境 (Staging)' },
      { value: 'prod', label: '生产环境 (Production)' },
    ],
    required: true,
    helperText: '请选择应用的部署环境',
    size: 'lg',
    fullWidth: true,
  },
  parameters: {
    layout: 'padded',
  },
};

// With default value
export const WithDefaultValue: Story = {
  args: {
    label: '时区',
    options: [
      { value: 'utc', label: 'UTC (Universal Time)' },
      { value: 'cst', label: 'CST (China Standard Time)', disabled: false },
      { value: 'jst', label: 'JST (Japan Standard Time)' },
      { value: 'pst', label: 'PST (Pacific Standard Time)' },
      { value: 'est', label: 'EST (Eastern Standard Time)' },
    ],
    value: 'cst',
    helperText: '当前选择: 中国标准时间',
  },
};
