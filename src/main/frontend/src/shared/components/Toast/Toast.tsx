import styles from './Toast.module.css';

export type ToastType = 'info' | 'success' | 'warning' | 'error';

export interface ToastProps {
  id: string;
  message: string;
  type?: ToastType;
  onClose: (id: string) => void;
}

export function Toast({ id, message, type = 'info', onClose }: ToastProps): React.JSX.Element {
  return (
    <div className={`${styles.toast} ${styles[type]}`}>
      <div className={styles.icon} aria-hidden>
        {type === 'success' && <i data-lucide="check-circle" />}
        {type === 'warning' && <i data-lucide="alert-triangle" />}
        {type === 'error' && <i data-lucide="x-circle" />}
        {type === 'info' && <i data-lucide="info" />}
      </div>
      <div className={styles.message}>{message}</div>
      <button className={styles.close} onClick={() => onClose(id)} aria-label="关闭通知">
        <i data-lucide="x" />
      </button>
    </div>
  );
}
