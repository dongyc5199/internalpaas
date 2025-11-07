import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi, type UserListParams } from '../../../shared/api/userApi';
import { Table } from '../../../shared/components/Table';
import { Button } from '../../../shared/components/Button';
import { Input } from '../../../shared/components/Input';
import { Modal } from '../../../shared/components/Modal';
import { QUERY_KEYS } from '../../../shared/constants';
import { UserRole, type User } from '../../../shared/types/user';
import { UserForm } from '../components/UserForm';
import styles from './UsersPage.module.css';

/**
 * UsersPage 组件
 *
 * 用户管理页面
 *
 * 功能:
 * - 用户列表展示
 * - 搜索和筛选
 * - 新增/编辑/删除用户
 * - 批量操作
 * - 角色管理
 */
export function UsersPage(): React.JSX.Element {
  const queryClient = useQueryClient();

  // 查询参数状态
  const [params, setParams] = useState<UserListParams>({
    page: 0,
    size: 20,
    search: '',
  });

  // UI 状态
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  /**
   * 获取用户列表
   */
  const { data, isLoading, error } = useQuery({
    queryKey: [QUERY_KEYS.USERS, params],
    queryFn: () => userApi.getUsers(params),
  });

  /**
   * 删除用户
   */
  const deleteMutation = useMutation({
    mutationFn: (id: number) => userApi.deleteUser(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.USERS] });
      setShowDeleteConfirm(false);
      setDeletingId(null);
    },
  });

  /**
   * 批量删除用户
   */
  const batchDeleteMutation = useMutation({
    mutationFn: (ids: number[]) => userApi.batchDeleteUsers(ids),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.USERS] });
      setSelectedIds(new Set());
    },
  });

  /**
   * 切换管理员角色
   */
  const toggleAdminMutation = useMutation({
    mutationFn: (id: number) => userApi.toggleAdmin(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.USERS] });
    },
  });

  /**
   * 处理搜索
   */
  const handleSearch = (value: string): void => {
    setParams((prev) => ({ ...prev, search: value, page: 0 }));
  };

  /**
   * 处理新增用户
   */
  const handleCreate = (): void => {
    setEditingUser(null);
    setShowCreateModal(true);
  };

  /**
   * 处理编辑用户
   */
  const handleEdit = (user: User): void => {
    setEditingUser(user);
    setShowCreateModal(true);
  };

  /**
   * 处理删除用户
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
   * 处理切换管理员
   */
  const handleToggleAdmin = (id: number): void => {
    toggleAdminMutation.mutate(id);
  };

  /**
   * 获取角色徽章
   */
  const getRoleBadge = (role: UserRole): React.JSX.Element => {
    const roleMap = {
      [UserRole.SUPER_ADMIN]: { label: '超级管理员', className: styles.roleSuperAdmin },
      [UserRole.ADMIN]: { label: '管理员', className: styles.roleAdmin },
      [UserRole.DEVELOPER]: { label: '开发者', className: styles.roleDeveloper },
    };

    const { label, className } = roleMap[role];
    return <span className={`${styles.roleBadge} ${className}`}>{label}</span>;
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
      key: 'username',
      title: '用户名',
      render: (_value: unknown, record: Record<string, unknown>) => {
        const user = record as unknown as User;
        return <span className={styles.username}>{user.username}</span>;
      },
    },
    {
      key: 'displayName',
      title: '显示名称',
      render: (_value: unknown, record: Record<string, unknown>) => {
        const user = record as unknown as User;
        return user.displayName || '-';
      },
    },
    {
      key: 'email',
      title: '邮箱',
      render: (_value: unknown, record: Record<string, unknown>) => {
        const user = record as unknown as User;
        return user.email;
      },
    },
    {
      key: 'role',
      title: '角色',
      render: (_value: unknown, record: Record<string, unknown>) => {
        const user = record as unknown as User;
        return getRoleBadge(user.role);
      },
    },
    {
      key: 'createdAt',
      title: '创建时间',
      render: (_value: unknown, record: Record<string, unknown>) => {
        const user = record as unknown as User;
        return new Date(user.createdAt).toLocaleDateString('zh-CN');
      },
    },
    {
      key: 'actions',
      title: '操作',
      render: (_value: unknown, record: Record<string, unknown>) => {
        const user = record as unknown as User;
        return (
          <div className={styles.actions}>
            <Button variant="ghost" size="sm" onClick={() => handleEdit(user)}>
              编辑
            </Button>
            {user.role !== UserRole.SUPER_ADMIN && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => handleToggleAdmin(user.id)}
                loading={toggleAdminMutation.isPending}
              >
                {user.role === UserRole.ADMIN ? '降为开发者' : '升为管理员'}
              </Button>
            )}
            <Button variant="ghost" size="sm" onClick={() => handleDelete(user.id)}>
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
          <h1 className={styles.title}>用户管理</h1>
          <p className={styles.subtitle}>管理系统用户和角色权限</p>
        </div>
        <div className={styles.headerRight}>
          <Button variant="primary" onClick={handleCreate}>
            <i data-lucide="plus" />
            新增用户
          </Button>
        </div>
      </div>

      {/* 工具栏 */}
      <div className={styles.toolbar}>
        <div className={styles.toolbarLeft}>
          <Input
            placeholder="搜索用户名或邮箱..."
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

      {/* 用户列表 */}
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
            rowKey={(record) => String((record as unknown as User).id)}
            pagination={true}
            pageSize={params.size ?? 20}
          />
        )}
      </div>

      {/* 创建/编辑模态框 */}
      <Modal
        open={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        title={editingUser ? '编辑用户' : '新增用户'}
        size="lg"
      >
        <UserForm
          user={editingUser ?? undefined}
          onSuccess={() => {
            setShowCreateModal(false);
            void queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.USERS] });
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
          <p>确定要删除这个用户吗？此操作不可撤销。</p>
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
