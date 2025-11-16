import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { monitoringApi } from '../../../shared/api/monitoringApi';
import { QUERY_KEYS } from '../../../shared/constants';
import { Button, Loading } from '../../../shared/components';
import type { Alert, AlertStatus, AlertLevel } from '../../../shared/types';
import styles from './AlertDetailPage.module.css';

/**
 * 告警处理记录
 */
interface AlertAction {
  id: number;
  alertId: number;
  action: 'ACKNOWLEDGE' | 'RESOLVE' | 'COMMENT';
  performedBy: string;
  performedAt: string;
  comment?: string;
}

/**
 * AlertDetailPage 组件
 *
 * 告警详情页面 - 显示单个告警的完整信息和处理历史
 */
export function AlertDetailPage(): React.JSX.Element {
  const { alertId } = useParams<{ alertId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // 获取告警详情
  const { data: alert, isLoading } = useQuery<Alert>({
    queryKey: [QUERY_KEYS.ALERTS, alertId],
    queryFn: () => monitoringApi.getAlert(Number(alertId)),
    enabled: !!alertId,
  });

  // 确认告警
  const acknowledgeMutation = useMutation({
    mutationFn: () => monitoringApi.acknowledgeAlert(Number(alertId)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ALERTS, alertId] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ALERTS] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ACTIVE_ALERTS] });
    },
  });

  // 解决告警
  const resolveMutation = useMutation({
    mutationFn: () => monitoringApi.resolveAlert(Number(alertId)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ALERTS, alertId] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ALERTS] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ACTIVE_ALERTS] });
    },
  });

  // 处理确认
  const handleAcknowledge = async (): Promise<void> => {
    await acknowledgeMutation.mutateAsync();
  };

  // 处理解决
  const handleResolve = async (): Promise<void> => {
    await resolveMutation.mutateAsync();
  };

  // 获取告警级别颜色类
  const getAlertLevelClass = (level: AlertLevel): string => {
    switch (level) {
      case 'CRITICAL':
        return styles.levelCritical ?? '';
      case 'ERROR':
        return styles.levelError ?? '';
      case 'WARNING':
        return styles.levelWarning ?? '';
      default:
        return styles.levelInfo ?? '';
    }
  };

  // 获取告警状态颜色类
  const getAlertStatusClass = (status: AlertStatus): string => {
    switch (status) {
      case 'ACTIVE':
        return styles.statusActive ?? '';
      case 'ACKNOWLEDGED':
        return styles.statusAcknowledged ?? '';
      case 'RESOLVED':
        return styles.statusResolved ?? '';
      default:
        return '';
    }
  };

  // 格式化时间
  const formatTime = (timestamp?: string): string => {
    if (!timestamp) return '-';
    const date = new Date(timestamp);
    return date.toLocaleString('zh-CN');
  };

  // 计算持续时间
  const calculateDuration = (startTime?: string, endTime?: string): string => {
    if (!startTime) return '-';
    const start = new Date(startTime).getTime();
    const end = endTime ? new Date(endTime).getTime() : Date.now();
    const duration = end - start;

    const hours = Math.floor(duration / (1000 * 60 * 60));
    const minutes = Math.floor((duration % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((duration % (1000 * 60)) / 1000);

    if (hours > 0) return `${hours}小时 ${minutes}分钟`;
    if (minutes > 0) return `${minutes}分钟 ${seconds}秒`;
    return `${seconds}秒`;
  };

  // 模拟告警处理记录（实际应从API获取）
  const alertActions: AlertAction[] = [
    ...(alert?.acknowledgedAt
      ? [
          {
            id: 1,
            alertId: alert.id,
            action: 'ACKNOWLEDGE' as const,
            performedBy: '管理员',
            performedAt: alert.acknowledgedAt,
          },
        ]
      : []),
    ...(alert?.resolvedAt
      ? [
          {
            id: 2,
            alertId: alert.id,
            action: 'RESOLVE' as const,
            performedBy: '管理员',
            performedAt: alert.resolvedAt,
          },
        ]
      : []),
  ];

  if (isLoading) {
    return <Loading text="加载告警详情..." fullScreen />;
  }

  if (!alert) {
    return (
      <div className={styles.container}>
        <div className={styles.emptyState}>
          <i data-lucide="alert-circle" />
          <h3>未找到告警</h3>
          <p>该告警不存在或已被删除</p>
          <Button onClick={() => navigate('/monitoring/alerts')}>返回告警列表</Button>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      {/* 页面头部 */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleGroup}>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => navigate('/monitoring/alerts')}
              className={styles.backBtn}
            >
              <i data-lucide="arrow-left" />
              返回列表
            </Button>
            <div>
              <h1 className={styles.title}>告警详情</h1>
              <p className={styles.subtitle}>告警 ID: {alert.id}</p>
            </div>
          </div>
          <div className={styles.headerActions}>
            {alert.status === 'ACTIVE' && (
              <>
                <Button
                  variant="secondary"
                  onClick={handleAcknowledge}
                  disabled={acknowledgeMutation.isPending}
                >
                  <i data-lucide="check" />
                  确认告警
                </Button>
                <Button onClick={handleResolve} disabled={resolveMutation.isPending}>
                  <i data-lucide="check-check" />
                  解决告警
                </Button>
              </>
            )}
            {alert.status === 'ACKNOWLEDGED' && (
              <Button onClick={handleResolve} disabled={resolveMutation.isPending}>
                <i data-lucide="check-check" />
                解决告警
              </Button>
            )}
          </div>
        </div>
      </header>

      {/* 告警概览卡片 */}
      <div className={styles.overviewCard}>
        <div className={styles.overviewHeader}>
          <div className={styles.statusBadges}>
            <span className={`${styles.levelBadge} ${getAlertLevelClass(alert.level)}`}>
              {alert.level}
            </span>
            <span className={`${styles.statusBadge} ${getAlertStatusClass(alert.status)}`}>
              {alert.status}
            </span>
          </div>
          <div className={styles.duration}>
            <i data-lucide="clock" />
            <span>持续时间: {calculateDuration(alert.createdAt, alert.resolvedAt)}</span>
          </div>
        </div>

        <h2 className={styles.alertMessage}>{alert.message}</h2>

        <div className={styles.metricsGrid}>
          <div className={styles.metricItem}>
            <span className={styles.metricLabel}>告警类型</span>
            <span className={styles.metricValue}>{alert.type}</span>
          </div>
          <div className={styles.metricItem}>
            <span className={styles.metricLabel}>服务器</span>
            <span className={styles.metricValue}>{alert.serverName}</span>
          </div>
          <div className={styles.metricItem}>
            <span className={styles.metricLabel}>当前值</span>
            <span className={styles.metricValue}>{alert.value.toFixed(2)}</span>
          </div>
          <div className={styles.metricItem}>
            <span className={styles.metricLabel}>阈值</span>
            <span className={styles.metricValue}>{alert.threshold.toFixed(2)}</span>
          </div>
        </div>
      </div>

      {/* 时间线信息 */}
      <div className={styles.timelineCard}>
        <h3 className={styles.cardTitle}>
          <i data-lucide="calendar" />
          时间线
        </h3>
        <div className={styles.timeline}>
          <div className={styles.timelineItem}>
            <div className={styles.timelineIcon}>
              <i data-lucide="alert-triangle" />
            </div>
            <div className={styles.timelineContent}>
              <div className={styles.timelineTitle}>告警创建</div>
              <div className={styles.timelineTime}>{formatTime(alert.createdAt)}</div>
              <div className={styles.timelineDescription}>
                系统检测到 {alert.type} 指标异常
              </div>
            </div>
          </div>

          {alert.acknowledgedAt && (
            <div className={styles.timelineItem}>
              <div className={styles.timelineIcon}>
                <i data-lucide="user-check" />
              </div>
              <div className={styles.timelineContent}>
                <div className={styles.timelineTitle}>告警已确认</div>
                <div className={styles.timelineTime}>{formatTime(alert.acknowledgedAt)}</div>
                <div className={styles.timelineDescription}>管理员已确认此告警</div>
              </div>
            </div>
          )}

          {alert.resolvedAt && (
            <div className={styles.timelineItem}>
              <div className={styles.timelineIcon}>
                <i data-lucide="check-circle" />
              </div>
              <div className={styles.timelineContent}>
                <div className={styles.timelineTitle}>告警已解决</div>
                <div className={styles.timelineTime}>{formatTime(alert.resolvedAt)}</div>
                <div className={styles.timelineDescription}>问题已修复，告警已关闭</div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 处理记录 */}
      {alertActions.length > 0 && (
        <div className={styles.actionsCard}>
          <h3 className={styles.cardTitle}>
            <i data-lucide="history" />
            处理记录
          </h3>
          <div className={styles.actionsList}>
            {alertActions.map((action) => (
              <div key={action.id} className={styles.actionItem}>
                <div className={styles.actionIcon}>
                  {action.action === 'ACKNOWLEDGE' && <i data-lucide="check" />}
                  {action.action === 'RESOLVE' && <i data-lucide="check-check" />}
                  {action.action === 'COMMENT' && <i data-lucide="message-square" />}
                </div>
                <div className={styles.actionContent}>
                  <div className={styles.actionHeader}>
                    <span className={styles.actionType}>
                      {action.action === 'ACKNOWLEDGE' && '确认了告警'}
                      {action.action === 'RESOLVE' && '解决了告警'}
                      {action.action === 'COMMENT' && '添加了评论'}
                    </span>
                    <span className={styles.actionTime}>{formatTime(action.performedAt)}</span>
                  </div>
                  <div className={styles.actionUser}>操作人: {action.performedBy}</div>
                  {action.comment && <div className={styles.actionComment}>{action.comment}</div>}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 相关服务器信息 */}
      <div className={styles.serverCard}>
        <h3 className={styles.cardTitle}>
          <i data-lucide="server" />
          相关服务器
        </h3>
        <div className={styles.serverInfo}>
          <div className={styles.serverInfoRow}>
            <span className={styles.serverInfoLabel}>服务器名称:</span>
            <span className={styles.serverInfoValue}>{alert.serverName}</span>
          </div>
          <div className={styles.serverInfoRow}>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => navigate(`/monitoring/servers/${alert.serverId}`)}
            >
              <i data-lucide="external-link" />
              查看服务器详情
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
