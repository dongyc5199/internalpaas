import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { serverApi } from '../../../shared/api/serverApi';
import { Button, Modal, StatCard, Breadcrumb } from '../../../shared/components';
import { QUERY_KEYS, ROUTES } from '../../../shared/constants';
import { ServerStatus } from '../../../shared/types/server';
import { ServerForm } from '../components/ServerForm';
import { ServerDetailSkeleton } from '../components/ServerDetailSkeleton';
import { useState } from 'react';
import styles from './ServerDetailPage.module.css';

/**
 * ServerDetailPage 组件
 *
 * 服务器详情页面
 *
 * 功能:
 * - 显示服务器基本信息
 * - 显示服务器监控指标
 * - 测试连接
 * - 刷新状态
 * - 编辑服务器
 * - 删除服务器
 */
export function ServerDetailPage(): React.JSX.Element {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const serverId = Number(id);

  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  /**
   * 获取服务器详情
   */
  const { data: server, isLoading, error } = useQuery({
    queryKey: [QUERY_KEYS.SERVERS, serverId],
    queryFn: () => serverApi.getServer(serverId),
    enabled: !isNaN(serverId),
  });

  /**
   * 获取服务器监控指标
   */
  const { data: metrics, isLoading: metricsLoading } = useQuery({
    queryKey: [QUERY_KEYS.SERVERS, serverId, 'metrics'],
    queryFn: () => serverApi.getMetrics(serverId),
    enabled: !isNaN(serverId),
    refetchInterval: 30000, // 每30秒刷新
  });

  /**
   * 测试连接
   */
  const testConnectionMutation = useMutation({
    mutationFn: () => serverApi.testConnection(serverId),
  });

  /**
   * 刷新状态
   */
  const refreshStatusMutation = useMutation({
    mutationFn: () => serverApi.refreshStatus(serverId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.SERVERS, serverId] });
    },
  });

  /**
   * 删除服务器
   */
  const deleteMutation = useMutation({
    mutationFn: () => serverApi.deleteServer(serverId),
    onSuccess: () => {
      navigate(ROUTES.ADMIN.SERVERS);
    },
  });

  /**
   * 处理测试连接
   */
  const handleTestConnection = (): void => {
    testConnectionMutation.mutate();
  };

  /**
   * 处理刷新状态
   */
  const handleRefreshStatus = (): void => {
    refreshStatusMutation.mutate();
  };

  /**
   * 处理编辑
   */
  const handleEdit = (): void => {
    setShowEditModal(true);
  };

  /**
   * 处理删除
   */
  const handleDelete = (): void => {
    setShowDeleteConfirm(true);
  };

  /**
   * 确认删除
   */
  const confirmDelete = (): void => {
    deleteMutation.mutate();
  };

  /**
   * 获取状态显示
   */
  const getStatusBadge = (status: ServerStatus): React.JSX.Element => {
    const statusMap = {
      [ServerStatus.ONLINE]: { label: '在线', className: styles.statusOnline },
      [ServerStatus.OFFLINE]: { label: '离线', className: styles.statusOffline },
      [ServerStatus.MAINTENANCE]: { label: '维护中', className: styles.statusMaintenance },
      [ServerStatus.ERROR]: { label: '错误', className: styles.statusError },
      [ServerStatus.UNKNOWN]: { label: '未知', className: styles.statusUnknown },
    };

    const { label, className } = statusMap[status];
    return <span className={`${styles.statusBadge} ${className}`}>{label}</span>;
  };

  if (isNaN(serverId)) {
    return (
      <div className={styles.page}>
        <div className={styles.error}>
          <i data-lucide="alert-circle" />
          <p>无效的服务器ID</p>
          <Button variant="primary" onClick={() => navigate(ROUTES.ADMIN.SERVERS)}>
            返回列表
          </Button>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return <ServerDetailSkeleton />;
  }

  if (error) {
    return (
      <div className={styles.page}>
        <div className={styles.error}>
          <i data-lucide="alert-circle" />
          <p>加载失败: {error instanceof Error ? error.message : '未知错误'}</p>
          <Button variant="primary" onClick={() => navigate(ROUTES.ADMIN.SERVERS)}>
            返回列表
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      {/* 面包屑导航 */}
      <Breadcrumb
        items={[
          { label: '首页', href: ROUTES.HOME },
          { label: '服务器管理', href: ROUTES.ADMIN.SERVERS },
          { label: server?.name || '服务器详情' },
        ]}
      />

      {/* 页面头部 */}
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate(ROUTES.ADMIN.SERVERS)}
            className={styles.backButton}
          >
            <i data-lucide="arrow-left" />
            返回
          </Button>
          <div>
            <h1 className={styles.title}>
              {server?.name}
              {server && getStatusBadge(server.status)}
            </h1>
            <p className={styles.subtitle}>
              {server?.host}:{server?.port}
            </p>
          </div>
        </div>
        <div className={styles.headerRight}>
          <Button
            variant="secondary"
            onClick={handleTestConnection}
            loading={testConnectionMutation.isPending}
          >
            <i data-lucide="wifi" />
            测试连接
          </Button>
          <Button
            variant="secondary"
            onClick={handleRefreshStatus}
            loading={refreshStatusMutation.isPending}
          >
            <i data-lucide="refresh-cw" />
            刷新状态
          </Button>
          <Button variant="secondary" onClick={handleEdit}>
            <i data-lucide="edit" />
            编辑
          </Button>
          <Button variant="danger" onClick={handleDelete}>
            <i data-lucide="trash-2" />
            删除
          </Button>
        </div>
      </div>

      {/* 连接测试结果 */}
      {testConnectionMutation.isSuccess && (
        <div
          className={`${styles.alert} ${
            testConnectionMutation.data.success ? styles.alertSuccess : styles.alertError
          }`}
        >
          <i data-lucide={testConnectionMutation.data.success ? 'check-circle' : 'x-circle'} />
          <p>{testConnectionMutation.data.message}</p>
        </div>
      )}

      {/* 监控指标卡片 */}
      <div className={styles.metricsGrid}>
        <StatCard
          title="CPU 使用率"
          value={metricsLoading ? '--' : `${metrics?.cpuUsage.toFixed(1)}%`}
          icon="cpu"
          loading={metricsLoading}
        />
        <StatCard
          title="内存使用率"
          value={metricsLoading ? '--' : `${metrics?.memoryUsage.toFixed(1)}%`}
          icon="database"
          loading={metricsLoading}
        />
        <StatCard
          title="磁盘使用率"
          value={metricsLoading ? '--' : `${metrics?.diskUsage.toFixed(1)}%`}
          icon="hard-drive"
          loading={metricsLoading}
        />
        <StatCard
          title="网络流量"
          value={metricsLoading ? '--' : `${(metrics?.networkIn ?? 0) + (metrics?.networkOut ?? 0)} MB/s`}
          icon="network"
          loading={metricsLoading}
        />
      </div>

      {/* 基本信息 */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>基本信息</h2>
          <div className={styles.quickActions}>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                navigator.clipboard.writeText(server?.host || '');
              }}
              title="复制主机地址"
            >
              <i data-lucide="copy" />
              复制地址
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                navigator.clipboard.writeText(`ssh ${server?.username}@${server?.host} -p ${server?.port}`);
              }}
              title="复制SSH连接命令"
            >
              <i data-lucide="terminal" />
              复制SSH命令
            </Button>
          </div>
        </div>
        <div className={styles.infoGrid}>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>服务器名称</span>
            <span className={styles.infoValue}>{server?.name}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>主机地址</span>
            <span className={styles.infoValue}>{server?.host}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>SSH 端口</span>
            <span className={styles.infoValue}>{server?.port}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>用户名</span>
            <span className={styles.infoValue}>{server?.username}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>所属群组</span>
            <span className={styles.infoValue}>
              {server?.groupId ? `群组 ${server.groupId}` : '无'}
            </span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>标签</span>
            <div className={styles.tags}>
              {server?.tags && server.tags.length > 0 ? (
                server.tags.map((tag) => (
                  <span key={tag} className={styles.tag}>
                    {tag}
                  </span>
                ))
              ) : (
                <span className={styles.infoValue}>无</span>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* 编辑模态框 */}
      <Modal
        open={showEditModal}
        onClose={() => setShowEditModal(false)}
        title="编辑服务器"
        size="lg"
      >
        <ServerForm
          server={server}
          onSuccess={() => {
            setShowEditModal(false);
            void queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.SERVERS, serverId] });
          }}
          onCancel={() => setShowEditModal(false)}
        />
      </Modal>

      {/* 删除确认模态框 */}
      <Modal
        open={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        title="确认删除"
        size="sm"
      >
        <div className={styles.deleteConfirm}>
          <p>确定要删除服务器 "{server?.name}" 吗？此操作不可撤销。</p>
          <div className={styles.deleteActions}>
            <Button variant="secondary" onClick={() => setShowDeleteConfirm(false)}>
              取消
            </Button>
            <Button
              variant="danger"
              onClick={confirmDelete}
              loading={deleteMutation.isPending}
            >
              确认删除
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
