import { useMemo } from 'react';
import type { WebSocketConnectionStatus } from '../api/websocket';
import styles from './WebSocketStatus.module.css';

interface WebSocketStatusProps {
  status: WebSocketConnectionStatus;
  lastConnectedAt?: number | Date | null;
  reconnectCount?: number;
}

const STATUS_LABEL: Record<WebSocketConnectionStatus, string> = {
  connected: '已连接',
  connecting: '连接中',
  disconnected: '未连接',
  reconnecting: '重连中',
};

export function WebSocketStatus({
  status,
  lastConnectedAt,
  reconnectCount = 0,
}: WebSocketStatusProps): React.JSX.Element {
  const className = useMemo(() => {
    return `${styles.status} ${styles[status] ?? ''}`;
  }, [status]);

  const lastConnectedText = useMemo(() => {
    if (!lastConnectedAt) return null;
    const date = typeof lastConnectedAt === 'number' ? new Date(lastConnectedAt) : lastConnectedAt;
    return date.toLocaleString();
  }, [lastConnectedAt]);

  return (
    <div className={className} title={lastConnectedText ? `上次连接：${lastConnectedText}` : undefined}>
      <span className={styles.dot} aria-hidden />
      <span className={styles.label}>{STATUS_LABEL[status]}</span>
      {reconnectCount > 0 && <span className={styles.timestamp}>重连 {reconnectCount} 次</span>}
      {lastConnectedText && <span className={styles.timestamp}>{lastConnectedText}</span>}
    </div>
  );
}
