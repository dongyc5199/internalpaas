import { useEffect } from 'react';
import { Toast } from './Toast';
import { useUIStore } from '../../stores/uiStore';
import styles from './Toast.module.css';

export type ToastPosition = 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left';

export interface ToastContainerProps {
  position?: ToastPosition;
  defaultDuration?: number;
}

export function ToastContainer({
  position = 'top-right',
  defaultDuration = 3500,
}: ToastContainerProps): React.JSX.Element | null {
  const { toasts, removeToast } = useUIStore((state) => ({
    toasts: state.toasts,
    removeToast: state.removeToast,
  }));

  useEffect(() => {
    const timers = toasts.map((toast) =>
      setTimeout(() => {
        removeToast(toast.id);
      }, toast.duration ?? defaultDuration)
    );

    return () => {
      timers.forEach((timer) => clearTimeout(timer));
    };
  }, [toasts, removeToast, defaultDuration]);

  if (!toasts.length) return null;

  const containerClass =
    position === 'top-right'
      ? `${styles.container} ${styles.topRight}`
      : position === 'top-left'
      ? `${styles.container} ${styles.topLeft}`
      : position === 'bottom-right'
      ? `${styles.container} ${styles.bottomRight}`
      : `${styles.container} ${styles.bottomLeft}`;

  return (
    <div className={containerClass}>
      {toasts.map((toast) => (
        <Toast
          key={toast.id}
          id={toast.id}
          message={toast.message}
          type={toast.type}
          onClose={removeToast}
        />
      ))}
    </div>
  );
}
