/**
 * Modal Component
 *
 * A flexible modal dialog component with backdrop, animations, and accessibility support.
 * Supports different sizes, close mechanisms, and portal rendering.
 */

import React, { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import styles from './Modal.module.css';

export type ModalSize = 'sm' | 'md' | 'lg' | 'xl' | 'full';

export interface ModalProps {
  /**
   * Whether the modal is open
   */
  open: boolean;

  /**
   * Callback fired when the modal requests to be closed
   */
  onClose: () => void;

  /**
   * Modal size
   * @default 'md'
   */
  size?: ModalSize;

  /**
   * Modal title
   */
  title?: React.ReactNode;

  /**
   * Modal content
   */
  children: React.ReactNode;

  /**
   * Modal footer content (typically action buttons)
   */
  footer?: React.ReactNode;

  /**
   * Whether clicking the backdrop closes the modal
   * @default true
   */
  closeOnBackdropClick?: boolean;

  /**
   * Whether pressing Escape closes the modal
   * @default true
   */
  closeOnEscape?: boolean;

  /**
   * Whether to show the close button
   * @default true
   */
  showCloseButton?: boolean;

  /**
   * Custom className for the modal content
   */
  className?: string;

  /**
   * Custom className for the modal header
   */
  headerClassName?: string;

  /**
   * Custom className for the modal body
   */
  bodyClassName?: string;

  /**
   * Custom className for the modal footer
   */
  footerClassName?: string;

  /**
   * Portal container element
   * @default document.body
   */
  container?: Element | null;

  /**
   * Whether to prevent body scroll when modal is open
   * @default true
   */
  preventScroll?: boolean;

  /**
   * Initial focus element selector
   */
  initialFocus?: string;

  /**
   * Aria label for the modal
   */
  'aria-label'?: string;

  /**
   * Aria labelledby for the modal
   */
  'aria-labelledby'?: string;

  /**
   * Aria describedby for the modal
   */
  'aria-describedby'?: string;
}

/**
 * Modal component
 *
 * @example
 * ```tsx
 * // Basic modal
 * <Modal open={isOpen} onClose={() => setIsOpen(false)} title="Confirm Action">
 *   Are you sure you want to proceed?
 * </Modal>
 *
 * // Modal with footer
 * <Modal
 *   open={isOpen}
 *   onClose={() => setIsOpen(false)}
 *   title="Delete Item"
 *   footer={
 *     <>
 *       <Button variant="ghost" onClick={() => setIsOpen(false)}>Cancel</Button>
 *       <Button variant="danger" onClick={handleDelete}>Delete</Button>
 *     </>
 *   }
 * >
 *   This action cannot be undone.
 * </Modal>
 *
 * // Large modal without backdrop close
 * <Modal
 *   open={isOpen}
 *   onClose={() => setIsOpen(false)}
 *   size="lg"
 *   closeOnBackdropClick={false}
 *   title="Settings"
 * >
 *   <SettingsForm />
 * </Modal>
 * ```
 */
export const Modal: React.FC<ModalProps> = ({
  open,
  onClose,
  size = 'md',
  title,
  children,
  footer,
  closeOnBackdropClick = true,
  closeOnEscape = true,
  showCloseButton = true,
  className,
  headerClassName,
  bodyClassName,
  footerClassName,
  container,
  preventScroll = true,
  initialFocus,
  'aria-label': ariaLabel,
  'aria-labelledby': ariaLabelledby,
  'aria-describedby': ariaDescribedby,
}) => {
  const modalRef = useRef<HTMLDivElement>(null);
  const previousActiveElement = useRef<HTMLElement | null>(null);

  // Generate unique IDs for ARIA
  const modalId = useRef(`modal-${Math.random().toString(36).substr(2, 9)}`).current;
  const titleId = `${modalId}-title`;
  const descriptionId = `${modalId}-description`;

  // Handle escape key
  useEffect(() => {
    if (!open || !closeOnEscape) return;

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [open, closeOnEscape, onClose]);

  // Handle body scroll prevention
  useEffect(() => {
    if (!open || !preventScroll) return;

    const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
    document.body.style.overflow = 'hidden';
    document.body.style.paddingRight = `${scrollbarWidth}px`;

    return () => {
      document.body.style.overflow = '';
      document.body.style.paddingRight = '';
    };
  }, [open, preventScroll]);

  // Handle focus management
  useEffect(() => {
    if (!open) return;

    // Store the currently focused element
    previousActiveElement.current = document.activeElement as HTMLElement;

    // Focus the modal or initial focus element
    const focusElement = () => {
      if (initialFocus) {
        const element = modalRef.current?.querySelector(initialFocus) as HTMLElement;
        if (element) {
          element.focus();
          return;
        }
      }
      modalRef.current?.focus();
    };

    // Use setTimeout to ensure DOM is ready
    const timeoutId = setTimeout(focusElement, 100);

    return () => {
      clearTimeout(timeoutId);
      // Restore focus to the previously focused element
      if (previousActiveElement.current) {
        previousActiveElement.current.focus();
      }
    };
  }, [open, initialFocus]);

  // Handle backdrop click
  const handleBackdropClick = (event: React.MouseEvent<HTMLDivElement>) => {
    if (closeOnBackdropClick && event.target === event.currentTarget) {
      onClose();
    }
  };

  // Don't render if not open
  if (!open) return null;

  // Build class names
  const modalClasses = [
    styles.modal,
    styles[`size-${size}`],
    className,
  ]
    .filter(Boolean)
    .join(' ');

  const headerClasses = [styles.header, headerClassName].filter(Boolean).join(' ');
  const bodyClasses = [styles.body, bodyClassName].filter(Boolean).join(' ');
  const footerClasses = [styles.footer, footerClassName].filter(Boolean).join(' ');

  const modalContent = (
    <div className={styles.backdrop} onClick={handleBackdropClick} data-testid="modal-backdrop">
      <div
        ref={modalRef}
        className={modalClasses}
        role="dialog"
        aria-modal="true"
        aria-label={ariaLabel}
        aria-labelledby={title ? (ariaLabelledby || titleId) : ariaLabelledby}
        aria-describedby={ariaDescribedby || descriptionId}
        tabIndex={-1}
      >
        {(title || showCloseButton) && (
          <div className={headerClasses}>
            {title && (
              <h2 id={titleId} className={styles.title}>
                {title}
              </h2>
            )}
            {showCloseButton && (
              <button
                type="button"
                className={styles.closeButton}
                onClick={onClose}
                aria-label="Close modal"
              >
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 20 20"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    d="M15 5L5 15M5 5L15 15"
                    stroke="currentColor"
                    strokeWidth="1.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </button>
            )}
          </div>
        )}

        <div id={descriptionId} className={bodyClasses}>
          {children}
        </div>

        {footer && <div className={footerClasses}>{footer}</div>}
      </div>
    </div>
  );

  // Render to portal
  return createPortal(
    modalContent,
    container || document.body
  );
};

Modal.displayName = 'Modal';
