import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { monitoringApi } from '../../../shared/api/monitoringApi';
import { QUERY_KEYS } from '../../../shared/constants';
import { Button, Loading, Table } from '../../../shared/components';
import type { Alert, AlertLevel, AlertStatus } from '../../../shared/types';
import styles from './AlertsPage.module.css';

/**
 * AlertsPage 组件
 *
 * 告警管理页面 - 查看和管理所有系统告警
 */
export function AlertsPage(): React.JSX.Element {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [filterStatus, setFilterStatus] = useState<AlertStatus | ''>('');
  const [filterLevel, setFilterLevel] = useState<AlertLevel | ''>('');
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  // 获取告警列表
  const { data: alertsData, isLoading } = useQuery({
    queryKey: [QUERY_KEYS.ALERTS, page, pageSize, filterStatus, filterLevel],
    queryFn: () =>
      monitoringApi.getAlerts({
        page,
        size: pageSize,
        status: filterStatus || undefined,
      }),
  });

  // 确认告警
  const acknowledgeMutation = useMutation({
    mutationFn: monitoringApi.acknowledgeAlert,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ALERTS] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ACTIVE_ALERTS] });
    },
  });

  // 解决告警
  const resolveMutation = useMutation({
    mutationFn: monitoringApi.resolveAlert,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ALERTS] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ACTIVE_ALERTS] });
    },
  });

  // 批量确认告警
  const batchAcknowledgeMutation = useMutation({
    mutationFn: monitoringApi.batchAcknowledgeAlerts,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ALERTS] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ACTIVE_ALERTS] });
      setSelectedIds([]);
    },
  });

  // 批量解决告警
  const batchResolveMutation = useMutation({
    mutationFn: monitoringApi.batchResolveAlerts,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ALERTS] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ACTIVE_ALERTS] });
      setSelectedIds([]);
    },
  });

  // 处理确认告警
  const handleAcknowledge = async (id: number): Promise<void> => {
    await acknowledgeMutation.mutateAsync(id);
  };

  // 处理解决告警
  const handleResolve = async (id: number): Promise<void> => {
    await resolveMutation.mutateAsync(id);
  };

  // 处理批量确认
  const handleBatchAcknowledge = async (): Promise<void> => {
    if (selectedIds.length > 0) {
      await batchAcknowledgeMutation.mutateAsync(selectedIds);
    }
  };

  // 处理批量解决
  const handleBatchResolve = async (): Promise<void> => {
    if (selectedIds.length > 0) {
      await batchResolveMutation.mutateAsync(selectedIds);
    }
  };

  // 切换选择
  const toggleSelect = (id: number): void => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((selectedId) => selectedId !== id) : [...prev, id]
    );
  };

  // 全选/取消全选
  const toggleSelectAll = (): void => {
    if (selectedIds.length === (alertsData?.items.length ?? 0)) {
      setSelectedIds([]);
    } else {
      setSelectedIds(alertsData?.items.map((alert) => alert.id) ?? []);
    }
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

  // 表格列定义
  const columns = [
    {
      key: 'select',
      header: (
        <input
          type="checkbox"
          checked={selectedIds.length === (alertsData?.items.length ?? 0) && selectedIds.length > 0}
          onChange={toggleSelectAll}
        />
      ),
      render: (alert: Alert) => (
        <input
          type="checkbox"
          checked={selectedIds.includes(alert.id)}
          onChange={() => toggleSelect(alert.id)}
        />
      ),
    },
    {
      key: 'level',
      header: '级别',
      render: (alert: Alert) => (
        <span className={`${styles.levelBadge} ${getAlertLevelClass(alert.level)}`}>
          {alert.level}
        </span>
      ),
    },
    {
      key: 'server',
      header: '服务器',
      render: (alert: Alert) => <span className={styles.serverName}>{alert.serverName}</span>,
    },
    {
      key: 'type',
      header: '类型',
      render: (alert: Alert) => <span>{alert.type}</span>,
    },
    {
      key: 'message',
      header: '消息',
      render: (alert: Alert) => (
        <span
          className={styles.message}
          onClick={() => navigate(`/monitoring/alerts/${alert.id}`)}
          style={{ cursor: 'pointer', textDecoration: 'underline' }}
        >
          {alert.message}
        </span>
      ),
    },
    {
      key: 'value',
      header: '当前值/阈值',
      render: (alert: Alert) => (
        <span className={styles.metrics}>
          {alert.value.toFixed(2)} / {alert.threshold.toFixed(2)}
        </span>
      ),
    },
    {
      key: 'status',
      header: '状态',
      render: (alert: Alert) => (
        <span className={`${styles.statusBadge} ${getAlertStatusClass(alert.status)}`}>
          {alert.status}
        </span>
      ),
    },
    {
      key: 'createdAt',
      header: '创建时间',
      render: (alert: Alert) => <span className={styles.timeText}>{formatTime(alert.createdAt)}</span>,
    },
    {
      key: 'actions',
      header: '操作',
      render: (alert: Alert) => (
        <div className={styles.actions}>
          {alert.status === 'ACTIVE' && (
            <>
              <Button
                size="sm"
                variant="secondary"
                onClick={() => handleAcknowledge(alert.id)}
                disabled={acknowledgeMutation.isPending}
              >
                确认
              </Button>
              <Button
                size="sm"
                onClick={() => handleResolve(alert.id)}
                disabled={resolveMutation.isPending}
              >
                解决
              </Button>
            </>
          )}
          {alert.status === 'ACKNOWLEDGED' && (
            <Button
              size="sm"
              onClick={() => handleResolve(alert.id)}
              disabled={resolveMutation.isPending}
            >
              解决
            </Button>
          )}
          {alert.status === 'RESOLVED' && <span className={styles.resolvedText}>已解决</span>}
        </div>
      ),
    },
  ];

  return (
    <div className={styles.container}>
      {/* 页面头部 */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleGroup}>
            <h1 className={styles.title}>告警管理</h1>
            <p className={styles.subtitle}>查看和管理所有系统告警</p>
          </div>
        </div>
      </header>

      {/* 筛选和批量操作 */}
      <div className={styles.toolbar}>
        <div className={styles.filters}>
          <select
            className={styles.select}
            value={filterStatus}
            onChange={(e) => {
              setFilterStatus(e.target.value as AlertStatus | '');
              setPage(0);
            }}
          >
            <option value="">所有状态</option>
            <option value="ACTIVE">活跃</option>
            <option value="ACKNOWLEDGED">已确认</option>
            <option value="RESOLVED">已解决</option>
          </select>

          <select
            className={styles.select}
            value={filterLevel}
            onChange={(e) => {
              setFilterLevel(e.target.value as AlertLevel | '');
              setPage(0);
            }}
          >
            <option value="">所有级别</option>
            <option value="INFO">信息</option>
            <option value="WARNING">警告</option>
            <option value="ERROR">错误</option>
            <option value="CRITICAL">严重</option>
          </select>
        </div>

        {selectedIds.length > 0 && (
          <div className={styles.batchActions}>
            <span className={styles.selectedCount}>已选择 {selectedIds.length} 项</span>
            <Button
              size="sm"
              variant="secondary"
              onClick={handleBatchAcknowledge}
              disabled={batchAcknowledgeMutation.isPending}
            >
              批量确认
            </Button>
            <Button
              size="sm"
              onClick={handleBatchResolve}
              disabled={batchResolveMutation.isPending}
            >
              批量解决
            </Button>
          </div>
        )}
      </div>

      {/* 告警列表表格 */}
      <div className={styles.tableContainer}>
        {isLoading ? (
          <Loading text="加载告警数据..." />
        ) : alertsData && alertsData.items.length > 0 ? (
          <Table
            data={alertsData.items as unknown as Record<string, unknown>[]}
            columns={columns as never[]}
          />
        ) : (
          <div className={styles.emptyState}>
            <i data-lucide="inbox" />
            <p>暂无告警数据</p>
          </div>
        )}
      </div>

      {/* 分页 */}
      {alertsData && alertsData.totalPages > 1 && (
        <div className={styles.pagination}>
          <Button
            size="sm"
            variant="secondary"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
          >
            上一页
          </Button>
          <span className={styles.pageInfo}>
            第 {page + 1} 页 / 共 {alertsData.totalPages} 页
          </span>
          <Button
            size="sm"
            variant="secondary"
            onClick={() => setPage((p) => Math.min(alertsData.totalPages - 1, p + 1))}
            disabled={page >= alertsData.totalPages - 1}
          >
            下一页
          </Button>
        </div>
      )}
    </div>
  );
}
