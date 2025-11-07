import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '../../../shared/api/userApi';
import { Button, Modal, Breadcrumb } from '../../../shared/components';
import { QUERY_KEYS, ROUTES } from '../../../shared/constants';
import { UserRole } from '../../../shared/types/user';
import { UserForm } from '../components/UserForm';
import { UserDetailSkeleton } from '../components/UserDetailSkeleton';
import { useState } from 'react';
import styles from './UserDetailPage.module.css';

/**
 * UserDetailPage 组件
 *
 * 用户详情页面
 *
 * 功能:
 * - 显示用户基本信息
 * - 显示用户角色和权限
 * - 编辑用户
 * - 删除用户
 * - 重置密码
 * - 切换角色
 */
export function UserDetailPage(): React.JSX.Element {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const userId = Number(id);

  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showResetPasswordModal, setShowResetPasswordModal] = useState(false);
  const [newPassword, setNewPassword] = useState('');

  /**
   * 获取用户详情
   */
  const { data: user, isLoading, error } = useQuery({
    queryKey: [QUERY_KEYS.USERS, userId],
    queryFn: () => userApi.getUser(userId),
    enabled: !isNaN(userId),
  });

  /**
   * 删除用户
   */
  const deleteMutation = useMutation({
    mutationFn: () => userApi.deleteUser(userId),
    onSuccess: () => {
      navigate(ROUTES.ADMIN.USERS);
    },
  });

  /**
   * 重置密码
   */
  const resetPasswordMutation = useMutation({
    mutationFn: (password: string) => userApi.resetPassword(userId, password),
    onSuccess: () => {
      setShowResetPasswordModal(false);
      setNewPassword('');
    },
  });

  /**
   * 切换管理员
   */
  const toggleAdminMutation = useMutation({
    mutationFn: () => userApi.toggleAdmin(userId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.USERS, userId] });
    },
  });

  /**
   * 切换超级管理员
   */
  const toggleSuperAdminMutation = useMutation({
    mutationFn: () => userApi.toggleSuperAdmin(userId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.USERS, userId] });
    },
  });

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
   * 处理重置密码
   */
  const handleResetPassword = (): void => {
    setShowResetPasswordModal(true);
  };

  /**
   * 确认重置密码
   */
  const confirmResetPassword = (): void => {
    if (newPassword.length >= 6) {
      resetPasswordMutation.mutate(newPassword);
    }
  };

  /**
   * 处理切换管理员
   */
  const handleToggleAdmin = (): void => {
    toggleAdminMutation.mutate();
  };

  /**
   * 处理切换超级管理员
   */
  const handleToggleSuperAdmin = (): void => {
    toggleSuperAdminMutation.mutate();
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
   * 格式化日期
   */
  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleString('zh-CN');
  };

  if (isNaN(userId)) {
    return (
      <div className={styles.page}>
        <div className={styles.error}>
          <i data-lucide="alert-circle" />
          <p>无效的用户ID</p>
          <Button variant="primary" onClick={() => navigate(ROUTES.ADMIN.USERS)}>
            返回列表
          </Button>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return <UserDetailSkeleton />;
  }

  if (error) {
    return (
      <div className={styles.page}>
        <div className={styles.error}>
          <i data-lucide="alert-circle" />
          <p>加载失败: {error instanceof Error ? error.message : '未知错误'}</p>
          <Button variant="primary" onClick={() => navigate(ROUTES.ADMIN.USERS)}>
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
          { label: '用户管理', href: ROUTES.ADMIN.USERS },
          { label: user?.username || '用户详情' },
        ]}
      />

      {/* 页面头部 */}
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate(ROUTES.ADMIN.USERS)}
            className={styles.backButton}
          >
            <i data-lucide="arrow-left" />
            返回
          </Button>
          <div>
            <h1 className={styles.title}>
              {user?.username}
              {user && getRoleBadge(user.role)}
            </h1>
            <p className={styles.subtitle}>{user?.email}</p>
          </div>
        </div>
        <div className={styles.headerRight}>
          <Button variant="secondary" onClick={handleEdit}>
            <i data-lucide="edit" />
            编辑
          </Button>
          <Button variant="secondary" onClick={handleResetPassword}>
            <i data-lucide="key" />
            重置密码
          </Button>
          <Button variant="danger" onClick={handleDelete}>
            <i data-lucide="trash-2" />
            删除
          </Button>
        </div>
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
                navigator.clipboard.writeText(user?.email || '');
              }}
              title="复制邮箱地址"
            >
              <i data-lucide="copy" />
              复制邮箱
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                navigator.clipboard.writeText(user?.username || '');
              }}
              title="复制用户名"
            >
              <i data-lucide="user" />
              复制用户名
            </Button>
          </div>
        </div>
        <div className={styles.infoGrid}>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>用户名</span>
            <span className={styles.infoValue}>{user?.username}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>邮箱</span>
            <span className={styles.infoValue}>{user?.email}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>显示名称</span>
            <span className={styles.infoValue}>{user?.displayName || '未设置'}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>角色</span>
            <span className={styles.infoValue}>{user && getRoleBadge(user.role)}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>创建时间</span>
            <span className={styles.infoValue}>{user && formatDate(user.createdAt)}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>最后登录</span>
            <span className={styles.infoValue}>
              {user?.lastLogin ? formatDate(user.lastLogin) : '从未登录'}
            </span>
          </div>
        </div>
      </div>

      {/* 权限管理 */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>权限管理</h2>
        </div>
        <div className={styles.permissions}>
          <div className={styles.permissionItem}>
            <div>
              <h3 className={styles.permissionTitle}>管理员权限</h3>
              <p className={styles.permissionDesc}>
                拥有服务器和用户管理权限，可以执行大部分管理操作
              </p>
            </div>
            {user?.role !== UserRole.SUPER_ADMIN && (
              <Button
                variant={user?.role === UserRole.ADMIN ? 'danger' : 'primary'}
                onClick={handleToggleAdmin}
                loading={toggleAdminMutation.isPending}
              >
                {user?.role === UserRole.ADMIN ? '撤销管理员' : '授予管理员'}
              </Button>
            )}
          </div>
          <div className={styles.permissionItem}>
            <div>
              <h3 className={styles.permissionTitle}>超级管理员权限</h3>
              <p className={styles.permissionDesc}>
                拥有系统最高权限，可以管理所有用户和系统配置
              </p>
            </div>
            <Button
              variant={user?.role === UserRole.SUPER_ADMIN ? 'danger' : 'primary'}
              onClick={handleToggleSuperAdmin}
              loading={toggleSuperAdminMutation.isPending}
            >
              {user?.role === UserRole.SUPER_ADMIN ? '撤销超级管理员' : '授予超级管理员'}
            </Button>
          </div>
        </div>
      </div>

      {/* 编辑模态框 */}
      <Modal
        open={showEditModal}
        onClose={() => setShowEditModal(false)}
        title="编辑用户"
        size="lg"
      >
        <UserForm
          user={user}
          onSuccess={() => {
            setShowEditModal(false);
            void queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.USERS, userId] });
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
          <p>确定要删除用户 "{user?.username}" 吗？此操作不可撤销。</p>
          <div className={styles.deleteActions}>
            <Button variant="secondary" onClick={() => setShowDeleteConfirm(false)}>
              取消
            </Button>
            <Button variant="danger" onClick={confirmDelete} loading={deleteMutation.isPending}>
              确认删除
            </Button>
          </div>
        </div>
      </Modal>

      {/* 重置密码模态框 */}
      <Modal
        open={showResetPasswordModal}
        onClose={() => setShowResetPasswordModal(false)}
        title="重置密码"
        size="sm"
      >
        <div className={styles.resetPassword}>
          <div className={styles.formGroup}>
            <label htmlFor="newPassword" className={styles.label}>
              新密码
            </label>
            <input
              id="newPassword"
              type="password"
              className={styles.input}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="请输入新密码（至少6位）"
            />
          </div>
          <div className={styles.resetActions}>
            <Button
              variant="secondary"
              onClick={() => {
                setShowResetPasswordModal(false);
                setNewPassword('');
              }}
            >
              取消
            </Button>
            <Button
              variant="primary"
              onClick={confirmResetPassword}
              loading={resetPasswordMutation.isPending}
              disabled={newPassword.length < 6}
            >
              确认重置
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
