import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import styles from './Modal.module.css';

export interface ModalProps {
  /** 是否显示 */
  open: boolean;
  /** 关闭回调 */
  onClose: () => void;
  /** 标题 */
  title?: string;
  /** 子内容 */
  children: React.ReactNode;
  /** 底部内容 */
  footer?: React.ReactNode;
  /** 尺寸 */
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full';
  /** 点击背景是否关闭 */
  closeOnBackdrop?: boolean;
  /** 按 ESC 是否关闭 */
  closeOnEscape?: boolean;
  /** 自定义类名 */
  className?: string;
}

/**
 * Modal 组件
 *
 * 模态框组件,使用 React Portal 渲染
 *
 * @example
 * ```tsx
 * function Example() {
 *   const [open, setOpen] = useState(false);
 *
 *   return (
 *     <>
 *       <Button onClick={() => setOpen(true)}>打开模态框</Button>
 *       <Modal
 *         open={open}
 *         onClose={() => setOpen(false)}
 *         title="确认操作"
 *         footer={
 *           <>
 *             <Button variant="ghost" onClick={() => setOpen(false)}>取消</Button>
 *             <Button onClick={handleConfirm}>确定</Button>
 *           </>
 *         }
 *       >
 *         <p>确定要执行此操作吗?</p>
 *       </Modal>
 *     </>
 *   );
 * }
 * ```
 */
export function Modal({
  open,
  onClose,
  title,
  children,
  footer,
  size = 'md',
  closeOnBackdrop = true,
  closeOnEscape = true,
  className,
}: ModalProps): React.JSX.Element | null {
  const overlayRef = useRef<HTMLDivElement>(null);

  // ESC 键处理
  useEffect(() => {
    if (!open || !closeOnEscape) return;

    const handleEscape = (e: KeyboardEvent): void => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => { document.removeEventListener('keydown', handleEscape); };
  }, [open, closeOnEscape, onClose]);

  // 防止滚动穿透
  useEffect(() => {
    if (!open) return;

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, [open]);

  // 背景点击处理
  const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>): void => {
    if (closeOnBackdrop && e.target === overlayRef.current) {
      onClose();
    }
  };

  if (!open) return null;

  const modalClassName = [styles.modal, styles[size], className].filter(Boolean).join(' ');

  return createPortal(
    <div
      ref={overlayRef}
      className={styles.overlay}
      onClick={handleOverlayClick}
      role="dialog"
      aria-modal="true"
      aria-labelledby={title ? 'modal-title' : undefined}
    >
      <div className={modalClassName}>
        {title && (
          <div className={styles.header}>
            <h2 id="modal-title" className={styles.title}>
              {title}
            </h2>
            <button
              type="button"
              className={styles.closeButton}
              onClick={onClose}
              aria-label="关闭"
            >
              <svg width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M6 6l8 8M14 6l-8 8" />
              </svg>
            </button>
          </div>
        )}

        <div className={styles.body}>{children}</div>

        {footer && <div className={styles.footer}>{footer}</div>}
      </div>
    </div>,
    document.body
  );
}

Modal.displayName = 'Modal';
