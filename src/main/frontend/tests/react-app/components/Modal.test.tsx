/**
 * Modal Component Tests
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Modal } from '../../../react-app/components/Modal';
import styles from '../../../react-app/components/Modal/Modal.module.css';

describe('Modal', () => {
  let container: HTMLElement;

  beforeEach(() => {
    container = document.createElement('div');
    container.id = 'modal-root';
    document.body.appendChild(container);
  });

  afterEach(() => {
    document.body.removeChild(container);
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';
  });

  describe('Rendering', () => {
    it('应该在open为false时不渲染', () => {
      render(<Modal open={false} onClose={() => {}}>Content</Modal>);
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('应该在open为true时渲染', () => {
      render(<Modal open={true} onClose={() => {}}>Content</Modal>);
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('应该渲染children内容', () => {
      render(
        <Modal open={true} onClose={() => {}}>
          <div>Test Content</div>
        </Modal>
      );
      expect(screen.getByText('Test Content')).toBeInTheDocument();
    });

    it('应该渲染默认size为md', () => {
      render(<Modal open={true} onClose={() => {}}>Content</Modal>);
      const modal = screen.getByRole('dialog');
      expect(modal.className).toContain(styles['size-md']);
    });

    it('应该渲染backdrop', () => {
      render(<Modal open={true} onClose={() => {}}>Content</Modal>);
      expect(screen.getByTestId('modal-backdrop')).toBeInTheDocument();
    });
  });

  describe('Title', () => {
    it('应该渲染title', () => {
      render(
        <Modal open={true} onClose={() => {}} title="Test Modal">
          Content
        </Modal>
      );
      expect(screen.getByText('Test Modal')).toBeInTheDocument();
    });

    it('应该渲染title为heading', () => {
      render(
        <Modal open={true} onClose={() => {}} title="Test Modal">
          Content
        </Modal>
      );
      const title = screen.getByText('Test Modal');
      expect(title.tagName).toBe('H2');
    });

    it('应该支持ReactNode作为title', () => {
      render(
        <Modal open={true} onClose={() => {}} title={<span data-testid="custom-title">Custom</span>}>
          Content
        </Modal>
      );
      expect(screen.getByTestId('custom-title')).toBeInTheDocument();
    });
  });

  describe('Sizes', () => {
    it('应该渲染small size', () => {
      render(
        <Modal open={true} onClose={() => {}} size="sm">
          Content
        </Modal>
      );
      const modal = screen.getByRole('dialog');
      expect(modal.className).toContain(styles['size-sm']);
    });

    it('应该渲染medium size', () => {
      render(
        <Modal open={true} onClose={() => {}} size="md">
          Content
        </Modal>
      );
      const modal = screen.getByRole('dialog');
      expect(modal.className).toContain(styles['size-md']);
    });

    it('应该渲染large size', () => {
      render(
        <Modal open={true} onClose={() => {}} size="lg">
          Content
        </Modal>
      );
      const modal = screen.getByRole('dialog');
      expect(modal.className).toContain(styles['size-lg']);
    });

    it('应该渲染xl size', () => {
      render(
        <Modal open={true} onClose={() => {}} size="xl">
          Content
        </Modal>
      );
      const modal = screen.getByRole('dialog');
      expect(modal.className).toContain(styles['size-xl']);
    });

    it('应该渲染full size', () => {
      render(
        <Modal open={true} onClose={() => {}} size="full">
          Content
        </Modal>
      );
      const modal = screen.getByRole('dialog');
      expect(modal.className).toContain(styles['size-full']);
    });
  });

  describe('Close Button', () => {
    it('应该渲染关闭按钮', () => {
      render(<Modal open={true} onClose={() => {}}>Content</Modal>);
      expect(screen.getByLabelText('Close modal')).toBeInTheDocument();
    });

    it('应该在点击关闭按钮时调用onClose', async () => {
      const user = userEvent.setup();
      const handleClose = vi.fn();
      render(<Modal open={true} onClose={handleClose}>Content</Modal>);

      await user.click(screen.getByLabelText('Close modal'));
      expect(handleClose).toHaveBeenCalledTimes(1);
    });

    it('应该在showCloseButton为false时不渲染关闭按钮', () => {
      render(
        <Modal open={true} onClose={() => {}} showCloseButton={false}>
          Content
        </Modal>
      );
      expect(screen.queryByLabelText('Close modal')).not.toBeInTheDocument();
    });
  });

  describe('Footer', () => {
    it('应该渲染footer', () => {
      render(
        <Modal open={true} onClose={() => {}} footer={<div>Footer Content</div>}>
          Content
        </Modal>
      );
      expect(screen.getByText('Footer Content')).toBeInTheDocument();
    });

    it('应该不渲染footer当未提供', () => {
      const { container } = render(<Modal open={true} onClose={() => {}}>Content</Modal>);
      const footer = container.querySelector(`.${styles.footer}`);
      expect(footer).not.toBeInTheDocument();
    });
  });

  describe('Backdrop Click', () => {
    it('应该在点击backdrop时调用onClose', async () => {
      const user = userEvent.setup();
      const handleClose = vi.fn();
      render(<Modal open={true} onClose={handleClose}>Content</Modal>);

      await user.click(screen.getByTestId('modal-backdrop'));
      expect(handleClose).toHaveBeenCalledTimes(1);
    });

    it('应该在closeOnBackdropClick为false时不关闭', async () => {
      const user = userEvent.setup();
      const handleClose = vi.fn();
      render(
        <Modal open={true} onClose={handleClose} closeOnBackdropClick={false}>
          Content
        </Modal>
      );

      await user.click(screen.getByTestId('modal-backdrop'));
      expect(handleClose).not.toHaveBeenCalled();
    });

    it('应该在点击modal内容时不调用onClose', async () => {
      const user = userEvent.setup();
      const handleClose = vi.fn();
      render(<Modal open={true} onClose={handleClose}>Content</Modal>);

      await user.click(screen.getByRole('dialog'));
      expect(handleClose).not.toHaveBeenCalled();
    });
  });

  describe('Escape Key', () => {
    it('应该在按Escape时调用onClose', async () => {
      const user = userEvent.setup();
      const handleClose = vi.fn();
      render(<Modal open={true} onClose={handleClose}>Content</Modal>);

      await user.keyboard('{Escape}');
      expect(handleClose).toHaveBeenCalledTimes(1);
    });

    it('应该在closeOnEscape为false时不关闭', async () => {
      const user = userEvent.setup();
      const handleClose = vi.fn();
      render(
        <Modal open={true} onClose={handleClose} closeOnEscape={false}>
          Content
        </Modal>
      );

      await user.keyboard('{Escape}');
      expect(handleClose).not.toHaveBeenCalled();
    });
  });

  describe('Body Scroll Prevention', () => {
    it('应该在打开时阻止body滚动', () => {
      render(<Modal open={true} onClose={() => {}}>Content</Modal>);
      expect(document.body.style.overflow).toBe('hidden');
    });

    it('应该在关闭时恢复body滚动', () => {
      const { rerender } = render(<Modal open={true} onClose={() => {}}>Content</Modal>);
      expect(document.body.style.overflow).toBe('hidden');

      rerender(<Modal open={false} onClose={() => {}}>Content</Modal>);
      expect(document.body.style.overflow).toBe('');
    });

    it('应该在preventScroll为false时不阻止滚动', () => {
      render(
        <Modal open={true} onClose={() => {}} preventScroll={false}>
          Content
        </Modal>
      );
      expect(document.body.style.overflow).toBe('');
    });
  });

  describe('Focus Management', () => {
    it('应该在打开时聚焦modal', async () => {
      render(<Modal open={true} onClose={() => {}}>Content</Modal>);

      await waitFor(() => {
        const modal = screen.getByRole('dialog');
        expect(document.activeElement).toBe(modal);
      });
    });

    it('应该在关闭时恢复之前的焦点', async () => {
      const button = document.createElement('button');
      button.textContent = 'Trigger';
      document.body.appendChild(button);
      button.focus();

      const { rerender } = render(<Modal open={true} onClose={() => {}}>Content</Modal>);

      await waitFor(() => {
        expect(document.activeElement).toBe(screen.getByRole('dialog'));
      });

      rerender(<Modal open={false} onClose={() => {}}>Content</Modal>);

      await waitFor(() => {
        expect(document.activeElement).toBe(button);
      });

      document.body.removeChild(button);
    });
  });

  describe('Custom Props', () => {
    it('应该传递自定义className到modal', () => {
      render(
        <Modal open={true} onClose={() => {}} className="custom-modal">
          Content
        </Modal>
      );
      const modal = screen.getByRole('dialog');
      expect(modal.className).toContain('custom-modal');
    });

    it('应该传递自定义headerClassName', () => {
      render(
        <Modal open={true} onClose={() => {}} title="Title" headerClassName="custom-header">
          Content
        </Modal>
      );
      const header = document.body.querySelector(`.${styles.header}`) as HTMLElement;
      expect(header).toBeInTheDocument();
      expect(header.className).toContain('custom-header');
    });

    it('应该传递自定义bodyClassName', () => {
      render(
        <Modal open={true} onClose={() => {}} bodyClassName="custom-body">
          Content
        </Modal>
      );
      const body = document.body.querySelector(`.${styles.body}`) as HTMLElement;
      expect(body).toBeInTheDocument();
      expect(body.className).toContain('custom-body');
    });

    it('应该传递自定义footerClassName', () => {
      render(
        <Modal open={true} onClose={() => {}} footer="Footer" footerClassName="custom-footer">
          Content
        </Modal>
      );
      const footer = document.body.querySelector(`.${styles.footer}`) as HTMLElement;
      expect(footer).toBeInTheDocument();
      expect(footer.className).toContain('custom-footer');
    });
  });

  describe('Portal Rendering', () => {
    it('应该渲染到document.body', () => {
      render(<Modal open={true} onClose={() => {}}>Content</Modal>);
      expect(document.body.querySelector('[role="dialog"]')).toBeInTheDocument();
    });

    it('应该渲染到自定义容器', () => {
      const customContainer = document.createElement('div');
      document.body.appendChild(customContainer);

      render(
        <Modal open={true} onClose={() => {}} container={customContainer}>
          Content
        </Modal>
      );

      expect(customContainer.querySelector('[role="dialog"]')).toBeInTheDocument();
      document.body.removeChild(customContainer);
    });
  });

  describe('Accessibility', () => {
    it('应该有role为dialog', () => {
      render(<Modal open={true} onClose={() => {}}>Content</Modal>);
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('应该设置aria-modal为true', () => {
      render(<Modal open={true} onClose={() => {}}>Content</Modal>);
      expect(screen.getByRole('dialog')).toHaveAttribute('aria-modal', 'true');
    });

    it('应该关联title和aria-labelledby', () => {
      render(
        <Modal open={true} onClose={() => {}} title="Test Title">
          Content
        </Modal>
      );
      const modal = screen.getByRole('dialog');
      const ariaLabelledby = modal.getAttribute('aria-labelledby');
      expect(ariaLabelledby).toBeTruthy();

      const title = document.getElementById(ariaLabelledby!);
      expect(title?.textContent).toBe('Test Title');
    });

    it('应该支持自定义aria-label', () => {
      render(
        <Modal open={true} onClose={() => {}} aria-label="Custom Label">
          Content
        </Modal>
      );
      expect(screen.getByRole('dialog')).toHaveAttribute('aria-label', 'Custom Label');
    });

    it('应该支持自定义aria-labelledby', () => {
      render(
        <Modal open={true} onClose={() => {}} aria-labelledby="custom-id">
          Content
        </Modal>
      );
      expect(screen.getByRole('dialog')).toHaveAttribute('aria-labelledby', 'custom-id');
    });

    it('应该支持自定义aria-describedby', () => {
      render(
        <Modal open={true} onClose={() => {}} aria-describedby="custom-description">
          Content
        </Modal>
      );
      expect(screen.getByRole('dialog')).toHaveAttribute('aria-describedby', 'custom-description');
    });

    it('应该设置关闭按钮的aria-label', () => {
      render(<Modal open={true} onClose={() => {}}>Content</Modal>);
      const closeButton = screen.getByLabelText('Close modal');
      expect(closeButton).toBeInTheDocument();
    });

    it('应该设置modal为focusable', () => {
      render(<Modal open={true} onClose={() => {}}>Content</Modal>);
      expect(screen.getByRole('dialog')).toHaveAttribute('tabIndex', '-1');
    });
  });

  describe('State Changes', () => {
    it('应该在open状态变化时更新渲染', () => {
      const { rerender } = render(<Modal open={false} onClose={() => {}}>Content</Modal>);
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

      rerender(<Modal open={true} onClose={() => {}}>Content</Modal>);
      expect(screen.getByRole('dialog')).toBeInTheDocument();

      rerender(<Modal open={false} onClose={() => {}}>Content</Modal>);
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('应该在children更新时重新渲染', () => {
      const { rerender } = render(
        <Modal open={true} onClose={() => {}}>
          First Content
        </Modal>
      );
      expect(screen.getByText('First Content')).toBeInTheDocument();

      rerender(
        <Modal open={true} onClose={() => {}}>
          Second Content
        </Modal>
      );
      expect(screen.queryByText('First Content')).not.toBeInTheDocument();
      expect(screen.getByText('Second Content')).toBeInTheDocument();
    });
  });

  describe('Multiple Modals', () => {
    it('应该支持同时渲染多个modal', () => {
      render(
        <>
          <Modal open={true} onClose={() => {}} aria-label="First Modal">
            First
          </Modal>
          <Modal open={true} onClose={() => {}} aria-label="Second Modal">
            Second
          </Modal>
        </>
      );

      const modals = screen.getAllByRole('dialog');
      expect(modals).toHaveLength(2);
    });
  });

  describe('Complex Content', () => {
    it('应该渲染带表单的modal', () => {
      render(
        <Modal open={true} onClose={() => {}} title="Form Modal">
          <form>
            <input type="text" placeholder="Username" />
            <button type="submit">Submit</button>
          </form>
        </Modal>
      );

      expect(screen.getByPlaceholderText('Username')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Submit' })).toBeInTheDocument();
    });

    it('应该渲染带列表的modal', () => {
      render(
        <Modal open={true} onClose={() => {}} title="List Modal">
          <ul>
            <li>Item 1</li>
            <li>Item 2</li>
            <li>Item 3</li>
          </ul>
        </Modal>
      );

      expect(screen.getByText('Item 1')).toBeInTheDocument();
      expect(screen.getByText('Item 2')).toBeInTheDocument();
      expect(screen.getByText('Item 3')).toBeInTheDocument();
    });
  });
});
