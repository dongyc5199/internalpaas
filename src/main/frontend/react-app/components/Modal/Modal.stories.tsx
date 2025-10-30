/**
 * Modal Component Stories
 */

import React, { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { Modal } from './Modal';
import { Button } from '../Button/Button';

const meta = {
  title: 'Components/Modal',
  component: Modal,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    open: {
      control: 'boolean',
      description: 'Whether the modal is open',
    },
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg', 'xl', 'full'],
      description: 'Modal size',
    },
    closeOnBackdropClick: {
      control: 'boolean',
      description: 'Close modal when clicking backdrop',
    },
    closeOnEscape: {
      control: 'boolean',
      description: 'Close modal when pressing Escape',
    },
    showCloseButton: {
      control: 'boolean',
      description: 'Show the close button',
    },
  },
} satisfies Meta<typeof Modal>;

export default meta;
type Story = StoryObj<typeof meta>;

// Wrapper component to handle modal state
const ModalWithTrigger: React.FC<Omit<React.ComponentProps<typeof Modal>, 'open' | 'onClose'>> = (props) => {
  const [open, setOpen] = useState(false);

  return (
    <div>
      <Button onClick={() => setOpen(true)}>打开弹窗</Button>
      <Modal {...props} open={open} onClose={() => setOpen(false)} />
    </div>
  );
};

// Default modal
export const Default: Story = {
  render: () => (
    <ModalWithTrigger
      title="默认弹窗"
      footer={
        <>
          <Button variant="outline">取消</Button>
          <Button variant="primary">确认</Button>
        </>
      }
    >
      <p>这是一个默认的弹窗内容示例。</p>
      <p>您可以在这里放置任何内容。</p>
    </ModalWithTrigger>
  ),
};

// Different sizes
export const Small: Story = {
  render: () => (
    <ModalWithTrigger
      title="小尺寸弹窗"
      size="sm"
      footer={<Button variant="primary" fullWidth>知道了</Button>}
    >
      <p>这是一个小尺寸的弹窗，适合显示简单的提示信息。</p>
    </ModalWithTrigger>
  ),
};

export const Medium: Story = {
  render: () => (
    <ModalWithTrigger
      title="中等尺寸弹窗"
      size="md"
      footer={
        <>
          <Button variant="outline">取消</Button>
          <Button variant="primary">确认</Button>
        </>
      }
    >
      <p>这是一个中等尺寸的弹窗，适合大多数场景。</p>
      <p>可以显示表单、列表等内容。</p>
    </ModalWithTrigger>
  ),
};

export const Large: Story = {
  render: () => (
    <ModalWithTrigger
      title="大尺寸弹窗"
      size="lg"
      footer={
        <>
          <Button variant="outline">取消</Button>
          <Button variant="primary">保存</Button>
        </>
      }
    >
      <div>
        <h3>详细表单示例</h3>
        <p>这是一个大尺寸的弹窗，适合显示复杂的内容。</p>
        <div style={{ marginTop: '1rem' }}>
          <p><strong>用户名:</strong> admin</p>
          <p><strong>邮箱:</strong> admin@example.com</p>
          <p><strong>角色:</strong> 管理员</p>
          <p><strong>创建时间:</strong> 2025-10-30</p>
        </div>
      </div>
    </ModalWithTrigger>
  ),
};

export const ExtraLarge: Story = {
  render: () => (
    <ModalWithTrigger
      title="超大尺寸弹窗"
      size="xl"
      footer={
        <>
          <Button variant="outline">取消</Button>
          <Button variant="primary">应用</Button>
        </>
      }
    >
      <div>
        <h3>数据表格示例</h3>
        <p>超大尺寸弹窗可以显示表格、图表等复杂内容。</p>
        <table style={{ width: '100%', marginTop: '1rem', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #ddd' }}>
              <th style={{ padding: '0.5rem', textAlign: 'left' }}>ID</th>
              <th style={{ padding: '0.5rem', textAlign: 'left' }}>名称</th>
              <th style={{ padding: '0.5rem', textAlign: 'left' }}>状态</th>
              <th style={{ padding: '0.5rem', textAlign: 'left' }}>创建时间</th>
            </tr>
          </thead>
          <tbody>
            {[1, 2, 3, 4, 5].map((id) => (
              <tr key={id} style={{ borderBottom: '1px solid #eee' }}>
                <td style={{ padding: '0.5rem' }}>{id}</td>
                <td style={{ padding: '0.5rem' }}>项目 {id}</td>
                <td style={{ padding: '0.5rem' }}>激活</td>
                <td style={{ padding: '0.5rem' }}>2025-10-30</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </ModalWithTrigger>
  ),
};

export const FullScreen: Story = {
  render: () => (
    <ModalWithTrigger
      title="全屏弹窗"
      size="full"
      footer={
        <>
          <Button variant="outline">关闭</Button>
          <Button variant="primary">保存更改</Button>
        </>
      }
    >
      <div>
        <h3>全屏模式</h3>
        <p>全屏弹窗占据整个视口，适合复杂的编辑界面。</p>
        <div style={{ marginTop: '2rem' }}>
          <h4>功能特性:</h4>
          <ul>
            <li>完整的编辑空间</li>
            <li>适合表单、编辑器等复杂界面</li>
            <li>提供最大的内容展示区域</li>
          </ul>
        </div>
      </div>
    </ModalWithTrigger>
  ),
};

// Different behaviors
export const WithoutCloseButton: Story = {
  render: () => (
    <ModalWithTrigger
      title="无关闭按钮"
      showCloseButton={false}
      footer={
        <>
          <Button variant="outline">取消</Button>
          <Button variant="primary">确认</Button>
        </>
      }
    >
      <p>这个弹窗没有右上角的关闭按钮。</p>
      <p>必须通过底部按钮或按Esc键关闭。</p>
    </ModalWithTrigger>
  ),
};

export const PreventBackdropClose: Story = {
  render: () => (
    <ModalWithTrigger
      title="禁止背景关闭"
      closeOnBackdropClick={false}
      footer={<Button variant="primary">我知道了</Button>}
    >
      <p>点击背景遮罩层无法关闭此弹窗。</p>
      <p>必须点击按钮或关闭按钮。</p>
    </ModalWithTrigger>
  ),
};

export const PreventEscapeClose: Story = {
  render: () => (
    <ModalWithTrigger
      title="禁止Esc关闭"
      closeOnEscape={false}
      footer={<Button variant="primary">关闭</Button>}
    >
      <p>按下Esc键无法关闭此弹窗。</p>
      <p>必须点击关闭按钮。</p>
    </ModalWithTrigger>
  ),
};

// Without footer
export const WithoutFooter: Story = {
  render: () => (
    <ModalWithTrigger title="无底部操作栏">
      <p>这个弹窗没有底部操作栏。</p>
      <p>只能通过右上角的关闭按钮或按Esc键关闭。</p>
    </ModalWithTrigger>
  ),
};

// Without header
export const WithoutHeader: Story = {
  render: () => (
    <ModalWithTrigger
      footer={<Button variant="primary">关闭</Button>}
    >
      <p>这个弹窗没有标题栏。</p>
      <p>适合显示简单的内容。</p>
    </ModalWithTrigger>
  ),
};

// Confirmation dialog
export const ConfirmationDialog: Story = {
  render: () => (
    <ModalWithTrigger
      title="确认删除"
      size="sm"
      footer={
        <>
          <Button variant="outline">取消</Button>
          <Button variant="danger">删除</Button>
        </>
      }
    >
      <p>您确定要删除这个项目吗？</p>
      <p style={{ color: '#666', fontSize: '0.875rem' }}>此操作无法撤销。</p>
    </ModalWithTrigger>
  ),
};

// Form dialog
export const FormDialog: Story = {
  render: () => (
    <ModalWithTrigger
      title="创建新用户"
      size="md"
      footer={
        <>
          <Button variant="outline">取消</Button>
          <Button variant="primary">创建</Button>
        </>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <div>
          <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>
            用户名
          </label>
          <input
            type="text"
            placeholder="请输入用户名"
            style={{
              width: '100%',
              padding: '0.5rem',
              border: '1px solid #ddd',
              borderRadius: '4px',
            }}
          />
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>
            邮箱
          </label>
          <input
            type="email"
            placeholder="请输入邮箱"
            style={{
              width: '100%',
              padding: '0.5rem',
              border: '1px solid #ddd',
              borderRadius: '4px',
            }}
          />
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>
            角色
          </label>
          <select
            style={{
              width: '100%',
              padding: '0.5rem',
              border: '1px solid #ddd',
              borderRadius: '4px',
            }}
          >
            <option value="">请选择角色</option>
            <option value="admin">管理员</option>
            <option value="developer">开发者</option>
            <option value="viewer">查看者</option>
          </select>
        </div>
      </div>
    </ModalWithTrigger>
  ),
};

// Long content with scroll
export const LongContent: Story = {
  render: () => (
    <ModalWithTrigger
      title="服务条款"
      size="md"
      footer={
        <>
          <Button variant="outline">拒绝</Button>
          <Button variant="primary">接受</Button>
        </>
      }
    >
      <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
        <h3>1. 服务条款</h3>
        <p>欢迎使用我们的服务。通过访问或使用我们的服务，您同意受本服务条款的约束。</p>

        <h3>2. 使用许可</h3>
        <p>我们授予您有限的、非排他性的、不可转让的许可，以访问和使用我们的服务。</p>

        <h3>3. 用户责任</h3>
        <p>您有责任维护账户的安全性和保密性。您对在您的账户下发生的所有活动负责。</p>

        <h3>4. 隐私政策</h3>
        <p>我们重视您的隐私。我们的隐私政策说明了我们如何收集、使用和保护您的个人信息。</p>

        <h3>5. 知识产权</h3>
        <p>所有服务内容均受版权、商标和其他知识产权法律保护。</p>

        <h3>6. 免责声明</h3>
        <p>我们的服务按"原样"提供，不提供任何明示或暗示的保证。</p>

        <h3>7. 责任限制</h3>
        <p>在法律允许的最大范围内，我们不对任何间接、偶然、特殊或后果性损害承担责任。</p>

        <h3>8. 变更</h3>
        <p>我们保留随时修改这些条款的权利。继续使用服务即表示您接受修改后的条款。</p>
      </div>
    </ModalWithTrigger>
  ),
};
