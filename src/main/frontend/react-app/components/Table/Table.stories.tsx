/**
 * Table Component Stories
 */

import React from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { Table } from './Table';
import { Button } from '../Button/Button';

const meta = {
  title: 'Components/Table',
  component: Table,
  parameters: {
    layout: 'padded',
  },
  tags: ['autodocs'],
  argTypes: {
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg'],
      description: 'Table size',
    },
    variant: {
      control: 'select',
      options: ['default', 'striped', 'bordered'],
      description: 'Table visual style',
    },
    hoverable: {
      control: 'boolean',
      description: 'Whether rows have hover effect',
    },
    loading: {
      control: 'boolean',
      description: 'Loading state',
    },
    stickyHeader: {
      control: 'boolean',
      description: 'Whether header sticks to top on scroll',
    },
  },
} satisfies Meta<typeof Table>;

export default meta;
type Story = StoryObj<typeof meta>;

// Sample data types
interface User {
  id: number;
  name: string;
  email: string;
  role: string;
  status: 'active' | 'inactive' | 'pending';
  createdAt: string;
}

interface Server {
  id: number;
  name: string;
  ip: string;
  cpu: number;
  memory: number;
  disk: number;
  status: 'online' | 'offline' | 'maintenance';
}

// Sample data
const sampleUsers: User[] = [
  { id: 1, name: '张三', email: 'zhangsan@example.com', role: '管理员', status: 'active', createdAt: '2025-01-15' },
  { id: 2, name: '李四', email: 'lisi@example.com', role: '开发者', status: 'active', createdAt: '2025-02-20' },
  { id: 3, name: '王五', email: 'wangwu@example.com', role: '查看者', status: 'inactive', createdAt: '2025-03-10' },
  { id: 4, name: 'John Doe', email: 'john@example.com', role: '开发者', status: 'pending', createdAt: '2025-04-05' },
  { id: 5, name: 'Jane Smith', email: 'jane@example.com', role: '管理员', status: 'active', createdAt: '2025-05-12' },
];

const sampleServers: Server[] = [
  { id: 1, name: 'web-server-01', ip: '192.168.1.10', cpu: 45, memory: 68, disk: 72, status: 'online' },
  { id: 2, name: 'api-server-01', ip: '192.168.1.11', cpu: 82, memory: 91, disk: 55, status: 'online' },
  { id: 3, name: 'db-server-01', ip: '192.168.1.12', cpu: 35, memory: 88, disk: 93, status: 'maintenance' },
  { id: 4, name: 'cache-server-01', ip: '192.168.1.13', cpu: 12, memory: 45, disk: 28, status: 'online' },
  { id: 5, name: 'worker-server-01', ip: '192.168.1.14', cpu: 95, memory: 78, disk: 65, status: 'offline' },
];

// Helper components
const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const colors: Record<string, string> = {
    active: '#22c55e',
    inactive: '#6b7280',
    pending: '#f59e0b',
    online: '#22c55e',
    offline: '#ef4444',
    maintenance: '#f59e0b',
  };

  return (
    <span
      style={{
        display: 'inline-block',
        padding: '0.25rem 0.75rem',
        borderRadius: '9999px',
        fontSize: '0.75rem',
        fontWeight: 500,
        backgroundColor: `${colors[status]}20`,
        color: colors[status],
      }}
    >
      {status}
    </span>
  );
};

const ProgressBar: React.FC<{ value: number }> = ({ value }) => {
  const color = value > 80 ? '#ef4444' : value > 60 ? '#f59e0b' : '#22c55e';

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
      <div
        style={{
          flex: 1,
          height: '8px',
          backgroundColor: '#e5e7eb',
          borderRadius: '4px',
          overflow: 'hidden',
        }}
      >
        <div
          style={{
            width: `${value}%`,
            height: '100%',
            backgroundColor: color,
            transition: 'width 0.3s',
          }}
        />
      </div>
      <span style={{ fontSize: '0.875rem', color: '#6b7280', minWidth: '3rem' }}>{value}%</span>
    </div>
  );
};

// Basic table
export const Default: Story = {
  args: {
    columns: [
      { key: 'id', label: 'ID', width: '80px' },
      { key: 'name', label: '姓名', dataKey: 'name' },
      { key: 'email', label: '邮箱', dataKey: 'email' },
      { key: 'role', label: '角色', dataKey: 'role' },
      {
        key: 'status',
        label: '状态',
        render: (row: User) => <StatusBadge status={row.status} />,
      },
      { key: 'createdAt', label: '创建时间', dataKey: 'createdAt' },
    ],
    data: sampleUsers,
  },
};

// Different sizes
export const Small: Story = {
  args: {
    columns: [
      { key: 'id', label: 'ID', dataKey: 'id' },
      { key: 'name', label: '名称', dataKey: 'name' },
      { key: 'status', label: '状态', render: (row: User) => <StatusBadge status={row.status} /> },
    ],
    data: sampleUsers.slice(0, 3),
    size: 'sm',
  },
};

export const Large: Story = {
  args: {
    columns: [
      { key: 'id', label: 'ID', width: '80px', dataKey: 'id' },
      { key: 'name', label: '姓名', dataKey: 'name' },
      { key: 'email', label: '邮箱', dataKey: 'email' },
      { key: 'role', label: '角色', dataKey: 'role' },
    ],
    data: sampleUsers,
    size: 'lg',
  },
};

// Variants
export const Striped: Story = {
  args: {
    columns: [
      { key: 'id', label: 'ID', dataKey: 'id' },
      { key: 'name', label: '姓名', dataKey: 'name' },
      { key: 'email', label: '邮箱', dataKey: 'email' },
    ],
    data: sampleUsers,
    variant: 'striped',
  },
};

export const Bordered: Story = {
  args: {
    columns: [
      { key: 'id', label: 'ID', dataKey: 'id' },
      { key: 'name', label: '姓名', dataKey: 'name' },
      { key: 'email', label: '邮箱', dataKey: 'email' },
    ],
    data: sampleUsers,
    variant: 'bordered',
  },
};

// With hover effect
export const Hoverable: Story = {
  args: {
    columns: [
      { key: 'id', label: 'ID', dataKey: 'id' },
      { key: 'name', label: '姓名', dataKey: 'name' },
      { key: 'email', label: '邮箱', dataKey: 'email' },
    ],
    data: sampleUsers,
    hoverable: true,
  },
};

// Loading state
export const Loading: Story = {
  args: {
    columns: [
      { key: 'id', label: 'ID', dataKey: 'id' },
      { key: 'name', label: '姓名', dataKey: 'name' },
      { key: 'email', label: '邮箱', dataKey: 'email' },
    ],
    data: [],
    loading: true,
  },
};

// Empty state
export const Empty: Story = {
  args: {
    columns: [
      { key: 'id', label: 'ID', dataKey: 'id' },
      { key: 'name', label: '姓名', dataKey: 'name' },
      { key: 'email', label: '邮箱', dataKey: 'email' },
    ],
    data: [],
    emptyText: '暂无数据',
  },
};

// With actions
export const WithActions: Story = {
  args: {
    columns: [
      { key: 'id', label: 'ID', width: '80px', dataKey: 'id' },
      { key: 'name', label: '姓名', dataKey: 'name' },
      { key: 'email', label: '邮箱', dataKey: 'email' },
      {
        key: 'status',
        label: '状态',
        render: (row: User) => <StatusBadge status={row.status} />,
      },
      {
        key: 'actions',
        label: '操作',
        width: '200px',
        render: () => (
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <Button size="sm" variant="outline">
              编辑
            </Button>
            <Button size="sm" variant="danger">
              删除
            </Button>
          </div>
        ),
      },
    ],
    data: sampleUsers,
  },
};

// Server monitoring table
export const ServerMonitoring: Story = {
  args: {
    columns: [
      { key: 'name', label: '服务器名称', dataKey: 'name', width: '180px' },
      { key: 'ip', label: 'IP地址', dataKey: 'ip', width: '140px' },
      {
        key: 'cpu',
        label: 'CPU使用率',
        render: (row: Server) => <ProgressBar value={row.cpu} />,
      },
      {
        key: 'memory',
        label: '内存使用率',
        render: (row: Server) => <ProgressBar value={row.memory} />,
      },
      {
        key: 'disk',
        label: '磁盘使用率',
        render: (row: Server) => <ProgressBar value={row.disk} />,
      },
      {
        key: 'status',
        label: '状态',
        width: '120px',
        render: (row: Server) => <StatusBadge status={row.status} />,
      },
    ],
    data: sampleServers,
    variant: 'striped',
    hoverable: true,
  },
};

// With sortable columns
export const Sortable: Story = {
  args: {
    columns: [
      { key: 'id', label: 'ID', dataKey: 'id', sortable: true, width: '80px' },
      { key: 'name', label: '姓名', dataKey: 'name', sortable: true },
      { key: 'email', label: '邮箱', dataKey: 'email', sortable: true },
      { key: 'createdAt', label: '创建时间', dataKey: 'createdAt', sortable: true },
    ],
    data: sampleUsers,
  },
};

// With custom alignment
export const CustomAlignment: Story = {
  args: {
    columns: [
      { key: 'id', label: 'ID', dataKey: 'id', align: 'center', width: '80px' },
      { key: 'name', label: '姓名', dataKey: 'name', align: 'left' },
      { key: 'email', label: '邮箱', dataKey: 'email', align: 'left' },
      {
        key: 'status',
        label: '状态',
        align: 'center',
        render: (row: User) => <StatusBadge status={row.status} />,
      },
      { key: 'createdAt', label: '创建时间', dataKey: 'createdAt', align: 'right' },
    ],
    data: sampleUsers,
  },
};

// Sticky header (with scrollable content)
export const StickyHeader: Story = {
  args: {
    columns: [
      { key: 'id', label: 'ID', dataKey: 'id', width: '80px' },
      { key: 'name', label: '姓名', dataKey: 'name' },
      { key: 'email', label: '邮箱', dataKey: 'email' },
      { key: 'role', label: '角色', dataKey: 'role' },
    ],
    data: [...sampleUsers, ...sampleUsers, ...sampleUsers], // Duplicate data for scrolling
    stickyHeader: true,
  },
  decorators: [
    (Story) => (
      <div style={{ maxHeight: '400px', overflow: 'auto' }}>
        <Story />
      </div>
    ),
  ],
};

// Complex example with all features
export const ComplexExample: Story = {
  args: {
    columns: [
      {
        key: 'select',
        label: (
          <input
            type="checkbox"
            style={{ cursor: 'pointer' }}
            aria-label="全选"
          />
        ),
        width: '50px',
        align: 'center',
        render: () => (
          <input
            type="checkbox"
            style={{ cursor: 'pointer' }}
            aria-label="选择"
          />
        ),
      },
      { key: 'id', label: 'ID', dataKey: 'id', sortable: true, width: '80px' },
      { key: 'name', label: '服务器名称', dataKey: 'name', sortable: true },
      { key: 'ip', label: 'IP地址', dataKey: 'ip' },
      {
        key: 'cpu',
        label: 'CPU',
        sortable: true,
        render: (row: Server) => <ProgressBar value={row.cpu} />,
      },
      {
        key: 'memory',
        label: '内存',
        sortable: true,
        render: (row: Server) => <ProgressBar value={row.memory} />,
      },
      {
        key: 'status',
        label: '状态',
        align: 'center',
        render: (row: Server) => <StatusBadge status={row.status} />,
      },
      {
        key: 'actions',
        label: '操作',
        width: '180px',
        align: 'center',
        render: () => (
          <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
            <Button size="sm" variant="outline">
              查看
            </Button>
            <Button size="sm" variant="primary">
              编辑
            </Button>
          </div>
        ),
      },
    ],
    data: sampleServers,
    variant: 'striped',
    hoverable: true,
    size: 'md',
  },
};
