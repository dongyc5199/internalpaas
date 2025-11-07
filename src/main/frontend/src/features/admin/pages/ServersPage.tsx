import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { serverApi, type ServerListParams } from '../../../shared/api/serverApi';
import { Table } from '../../../shared/components/Table';
import { Button } from '../../../shared/components/Button';
import { Input } from '../../../shared/components/Input';
import { Modal } from '../../../shared/components/Modal';
import { QUERY_KEYS, ROUTES } from '../../../shared/constants';
import { ServerStatus, type Server } from '../../../shared/types/server';
import { ServerForm } from '../components/ServerForm';
import styles from './ServersPage.module.css';

/**
 * ServersPage 组件
 *
 * 服务器管理页面
 *
 * 功能:
 * - 服务器列表展示
 * - 搜索和筛选
 * - 新增/编辑/删除服务器
 * - 批量操作
 * - 连接测试
 */
export function ServersPage(): React.JSX.Element {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // 查询参数状态
  const [params, setParams] = useState<ServerListParams>({
    page: 0,
    size: 20,
    search: '',
  });

  // UI 状态
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingServer, setEditingServer] = useState<Server | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  /**
   * 获取服务器列表
   */
  const { data, isLoading, error } = useQuery({
    queryKey: [QUERY_KEYS.SERVERS, params],
    queryFn: () => serverApi.getServers(params),
  });

  /**
   * 删除服务器
   */
  const deleteMutation = useMutation({
    mutationFn: (id: number) => serverApi.deleteServer(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.SERVERS] });
      setShowDeleteConfirm(false);
      setDeletingId(null);
    },
  });

  /**
   * 批量删除服务器
   */
  const batchDeleteMutation = useMutation({
    mutationFn: (ids: number[]) => serverApi.batchDeleteServers(ids),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.SERVERS] });
      setSelectedIds(new Set());
    },
  });

  /**
   * 测试连接
   */
  const testConnectionMutation = useMutation({
    mutationFn: (id: number) => serverApi.testConnection(id),
  });

  /**
   * 处理搜索
   */
  const handleSearch = (value: string): void => {
    setParams((prev) => ({ ...prev, search: value, page: 0 }));
  };

  /**
   * 处理新增服务器
   */
  const handleCreate = (): void => {
    setEditingServer(null);
    setShowCreateModal(true);
  };

  /**
   * 处理编辑服务器
   */
  const handleEdit = (server: Server): void => {
    setEditingServer(server);
    setShowCreateModal(true);
  };

  /**
   * 处理删除服务器
   */
  const handleDelete = (id: number): void => {
    setDeletingId(id);
    setShowDeleteConfirm(true);
  };

  /**
   * 确认删除
   */
  const confirmDelete = (): void => {
    if (deletingId) {
      deleteMutation.mutate(deletingId);
    }
  };

  /**
   * 处理批量删除
   */
  const handleBatchDelete = (): void => {
    if (selectedIds.size > 0) {
      batchDeleteMutation.mutate(Array.from(selectedIds));
    }
  };

  /**
   * 处理测试连接
   */
  const handleTestConnection = (id: number): void => {
    testConnectionMutation.mutate(id);
  };

  /**
   * 处理查看详情
   */
  const handleViewDetail = (id: number): void => {
    navigate(ROUTES.ADMIN.SERVER_DETAIL(id));
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

  /**
   * 表格列定义
   */
  const columns: Array<{
    key: string;
    title: string;
    render?: (_value: unknown, record: Record<string, unknown>, index: number) => React.ReactNode;
  }> = [
    {
      key: 'name',
      title: '服务器名称',
      render: (_value: unknown, record: Record<string, unknown>) => {
        const server = record as unknown as Server;
        return (
          <button className={styles.nameButton} onClick={() => handleViewDetail(server.id)}>
            {server.name}
          </button>
        );
      },
    },
    {
      key: 'host',
      title: '主机地址',
      render: (_value: unknown, record: Record<string, unknown>) => {
        const server = record as unknown as Server;
        return `${server.host}:${server.port}`;
      },
    },
    {
      key: 'status',
      title: '状态',
      render: (_value: unknown, record: Record<string, unknown>) => {
        const server = record as unknown as Server;
        return getStatusBadge(server.status);
      },
    },
    {
      key: 'tags',
      title: '标签',
      render: (_value: unknown, record: Record<string, unknown>) => {
        const server = record as unknown as Server;
        return (
          <div className={styles.tags}>
            {server.tags.map((tag) => (
              <span key={tag} className={styles.tag}>
                {tag}
              </span>
            ))}
          </div>
        );
      },
    },
    {
      key: 'actions',
      title: '操作',
      render: (_value: unknown, record: Record<string, unknown>) => {
        const server = record as unknown as Server;
        return (
          <div className={styles.actions}>
            <Button variant="ghost" size="sm" onClick={() => handleEdit(server)}>
              编辑
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handleTestConnection(server.id)}
              loading={testConnectionMutation.isPending}
            >
              测试连接
            </Button>
            <Button variant="ghost" size="sm" onClick={() => handleDelete(server.id)}>
              删除
            </Button>
          </div>
        );
      },
    },
  ];

  return (
    <div className={styles.page}>
      {/* 页面头部 */}
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <h1 className={styles.title}>服务器管理</h1>
          <p className={styles.subtitle}>管理所有服务器的配置和状态</p>
        </div>
        <div className={styles.headerRight}>
          <Button variant="primary" onClick={handleCreate}>
            <i data-lucide="plus" />
            新增服务器
          </Button>
        </div>
      </div>

      {/* 工具栏 */}
      <div className={styles.toolbar}>
        <div className={styles.toolbarLeft}>
          <Input
            placeholder="搜索服务器名称或地址..."
            value={params.search}
            onChange={(e) => handleSearch(e.target.value)}
          />
        </div>
        <div className={styles.toolbarRight}>
          {selectedIds.size > 0 && (
            <>
              <span className={styles.selectedCount}>已选择 {selectedIds.size} 项</span>
              <Button
                variant="danger"
                size="sm"
                onClick={handleBatchDelete}
                loading={batchDeleteMutation.isPending}
              >
                批量删除
              </Button>
            </>
          )}
        </div>
      </div>

      {/* 服务器列表 */}
      <div className={styles.content}>
        {error ? (
          <div className={styles.error}>
            <i data-lucide="alert-circle" />
            <p>加载失败: {error instanceof Error ? error.message : '未知错误'}</p>
          </div>
        ) : (
          <Table
            data={data?.content as unknown as Record<string, unknown>[]}
            columns={columns}
            loading={isLoading}
            rowKey={(record) => String((record as unknown as Server).id)}
            pagination={true}
            pageSize={params.size ?? 20}
          />
        )}
      </div>

      {/* 创建/编辑模态框 */}
      <Modal
        open={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        title={editingServer ? '编辑服务器' : '新增服务器'}
        size="lg"
      >
        <ServerForm
          server={editingServer ?? undefined}
          onSuccess={() => {
            setShowCreateModal(false);
            void queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.SERVERS] });
          }}
          onCancel={() => setShowCreateModal(false)}
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
          <p>确定要删除这个服务器吗？此操作不可撤销。</p>
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
