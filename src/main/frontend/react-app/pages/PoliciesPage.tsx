import { useState } from 'react';
import { usePolicies } from '../hooks/usePolicies';
import { useUpdatePolicy } from '../hooks/useUpdatePolicy';
import { Table } from '../components/Table/Table';
import { Button } from '../components/Button/Button';
import { Modal } from '../components/Modal/Modal';
import type {
  PolicySummary,
  PolicyStatus,
  PolicyEffect,
  PolicyResourceType,
} from '../types/policy';

/**
 * PoliciesPage Component
 *
 * Displays a list of policies with filtering and edit capabilities.
 * Integrates usePolicies and useUpdatePolicy hooks for data fetching and mutation.
 * Demonstrates optimistic updates and automatic rollback on error.
 */
export function PoliciesPage(): JSX.Element {
  // State for filters
  const [selectedStatus, setSelectedStatus] = useState<PolicyStatus | 'all'>('all');
  const [selectedEffect, setSelectedEffect] = useState<PolicyEffect | 'all'>('all');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  // State for modal
  const [editingPolicy, setEditingPolicy] = useState<PolicySummary | null>(null);
  const [newStatus, setNewStatus] = useState<PolicyStatus>('active');

  // Build filters
  const filters =
    selectedStatus === 'all' && selectedEffect === 'all'
      ? undefined
      : {
          ...(selectedStatus !== 'all' && { status: [selectedStatus] }),
          ...(selectedEffect !== 'all' && { effect: [selectedEffect] }),
        };

  // Fetch policies with React Query
  const { data, isLoading, error, refetch, isFetching } = usePolicies({
    filters,
    page: currentPage,
    pageSize,
  });

  // Update policy mutation with optimistic updates
  const updatePolicyMutation = useUpdatePolicy({
    enableOptimisticUpdate: true, // Enable instant UI feedback
    onSuccess: (data) => {
      console.log('Policy updated successfully:', data.id);
      setEditingPolicy(null); // Close modal
      // Success feedback could be shown here (e.g., toast notification)
    },
    onError: (error) => {
      console.error('Failed to update policy:', error);
      // Error feedback could be shown here (e.g., toast notification)
      alert(`更新失败: ${error.message}`);
    },
  });

  // Handle status toggle
  const handleToggleStatus = (policy: PolicySummary) => {
    const newStatus: PolicyStatus = policy.status === 'active' ? 'inactive' : 'active';

    updatePolicyMutation.mutate({
      policyId: policy.id,
      request: { status: newStatus },
    });
  };

  // Handle edit (opens modal)
  const handleEdit = (policy: PolicySummary) => {
    setEditingPolicy(policy);
    setNewStatus(policy.status);
  };

  // Handle save from modal
  const handleSave = () => {
    if (!editingPolicy) return;

    updatePolicyMutation.mutate({
      policyId: editingPolicy.id,
      request: { status: newStatus },
    });
  };

  // Status badge component
  const StatusBadge = ({ status }: { status: PolicyStatus }) => {
    const colors: Record<PolicyStatus, string> = {
      active: '#22c55e',
      inactive: '#6b7280',
      expired: '#ef4444',
    };

    const labels: Record<PolicyStatus, string> = {
      active: '激活',
      inactive: '停用',
      expired: '已过期',
    };

    return (
      <span
        style={{
          display: 'inline-block',
          padding: '0.25rem 0.75rem',
          borderRadius: '9999px',
          fontSize: '0.75rem',
          fontWeight: 500,
          backgroundColor: `${colors[status]}20`,
          color: colors[status],
        }}
      >
        {labels[status]}
      </span>
    );
  };

  // Effect badge
  const EffectBadge = ({ effect }: { effect: PolicyEffect }) => {
    const color = effect === 'allow' ? '#3b82f6' : '#f97316';
    const label = effect === 'allow' ? '允许' : '拒绝';

    return (
      <span
        style={{
          display: 'inline-block',
          padding: '0.25rem 0.75rem',
          borderRadius: '9999px',
          fontSize: '0.75rem',
          fontWeight: 500,
          backgroundColor: `${color}20`,
          color,
        }}
      >
        {label}
      </span>
    );
  };

  return (
    <section className="dp-page" data-testid="policies-page">
      <header className="dp-page__header">
        <div>
          <h3 className="dp-page__title">策略配置中心</h3>
          <p className="dp-page__description">
            管理审批链路、金丝雀门禁与冻结窗口等策略。演示React Query乐观更新功能。
          </p>
        </div>
        <div className="dp-page__filters">
          <select
            aria-label="筛选状态"
            value={selectedStatus}
            onChange={(e) => {
              setSelectedStatus(e.target.value as PolicyStatus | 'all');
              setCurrentPage(1);
            }}
            style={{ marginRight: '0.5rem', padding: '0.5rem', borderRadius: '4px' }}
          >
            <option value="all">全部状态</option>
            <option value="active">激活</option>
            <option value="inactive">停用</option>
            <option value="expired">已过期</option>
          </select>
          <select
            aria-label="筛选效果"
            value={selectedEffect}
            onChange={(e) => {
              setSelectedEffect(e.target.value as PolicyEffect | 'all');
              setCurrentPage(1);
            }}
            style={{ marginRight: '0.5rem', padding: '0.5rem', borderRadius: '4px' }}
          >
            <option value="all">全部效果</option>
            <option value="allow">允许</option>
            <option value="deny">拒绝</option>
          </select>
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetch()}
            disabled={isFetching}
          >
            {isFetching ? '刷新中...' : '刷新'}
          </Button>
        </div>
      </header>

      {isLoading && (
        <div className="dp-page__placeholder" data-testid="policies-loading">
          正在加载策略列表...
        </div>
      )}

      {error && (
        <div
          className="dp-page__placeholder dp-page__placeholder--error"
          data-testid="policies-error"
        >
          获取策略列表失败: {error.message}
          <br />
          <Button variant="primary" size="sm" onClick={() => refetch()} style={{ marginTop: '1rem' }}>
            重试
          </Button>
        </div>
      )}

      {!isLoading && !error && data && (
        <>
          <div style={{ marginBottom: '1rem', color: '#6b7280', fontSize: '0.875rem' }}>
            共 {data.total} 条策略记录 {isFetching && <span>（刷新中...）</span>}
            {updatePolicyMutation.isLoading && <span style={{ color: '#f59e0b' }}> · 正在保存...</span>}
          </div>

          {data.policies.length === 0 ? (
            <div className="dp-page__placeholder">暂无策略记录</div>
          ) : (
            <Table<PolicySummary>
              data={data.policies}
              columns={[
                {
                  key: 'id',
                  label: 'ID',
                  dataKey: 'id',
                  width: '120px',
                  render: (row) => (
                    <span style={{ fontFamily: 'monospace', fontSize: '0.875rem' }}>
                      {row.id.slice(0, 8)}
                    </span>
                  ),
                },
                {
                  key: 'name',
                  label: '策略名称',
                  dataKey: 'name',
                  sortable: true,
                },
                {
                  key: 'effect',
                  label: '效果',
                  width: '80px',
                  align: 'center',
                  render: (row) => <EffectBadge effect={row.effect} />,
                },
                {
                  key: 'subjectType',
                  label: '主体类型',
                  dataKey: 'subjectType',
                  width: '100px',
                },
                {
                  key: 'subjectCount',
                  label: '主体数',
                  dataKey: 'subjectCount',
                  width: '80px',
                  align: 'center',
                },
                {
                  key: 'resourceType',
                  label: '资源类型',
                  dataKey: 'resourceType',
                  width: '120px',
                },
                {
                  key: 'resourceCount',
                  label: '资源数',
                  dataKey: 'resourceCount',
                  width: '80px',
                  align: 'center',
                },
                {
                  key: 'status',
                  label: '状态',
                  width: '100px',
                  align: 'center',
                  render: (row) => <StatusBadge status={row.status} />,
                },
                {
                  key: 'actions',
                  label: '操作',
                  width: '200px',
                  align: 'center',
                  render: (row) => (
                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleEdit(row)}
                        disabled={updatePolicyMutation.isLoading}
                      >
                        编辑
                      </Button>
                      <Button
                        size="sm"
                        variant={row.status === 'active' ? 'danger' : 'primary'}
                        onClick={() => handleToggleStatus(row)}
                        disabled={updatePolicyMutation.isLoading || row.status === 'expired'}
                      >
                        {row.status === 'active' ? '停用' : '激活'}
                      </Button>
                    </div>
                  ),
                },
              ]}
              variant="striped"
              hoverable
              size="md"
            />
          )}

          {/* Pagination */}
          {data.total > pageSize && (
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginTop: '1rem',
                padding: '1rem',
                borderTop: '1px solid #e5e7eb',
              }}
            >
              <div style={{ color: '#6b7280', fontSize: '0.875rem' }}>
                第 {currentPage} 页，共 {Math.ceil(data.total / pageSize)} 页
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                  disabled={currentPage === 1}
                >
                  上一页
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setCurrentPage((p) => p + 1)}
                  disabled={!data.hasMore}
                >
                  下一页
                </Button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Edit Modal */}
      <Modal
        open={editingPolicy !== null}
        onClose={() => setEditingPolicy(null)}
        title="编辑策略状态"
        size="sm"
        footer={
          <>
            <Button variant="outline" onClick={() => setEditingPolicy(null)}>
              取消
            </Button>
            <Button
              variant="primary"
              onClick={handleSave}
              disabled={updatePolicyMutation.isLoading}
            >
              {updatePolicyMutation.isLoading ? '保存中...' : '保存'}
            </Button>
          </>
        }
      >
        {editingPolicy && (
          <div style={{ padding: '1rem 0' }}>
            <p style={{ marginBottom: '1rem' }}>
              策略名称: <strong>{editingPolicy.name}</strong>
            </p>
            <div>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>
                状态
              </label>
              <select
                value={newStatus}
                onChange={(e) => setNewStatus(e.target.value as PolicyStatus)}
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                }}
              >
                <option value="active">激活</option>
                <option value="inactive">停用</option>
                <option value="expired">已过期</option>
              </select>
            </div>
            <p style={{ marginTop: '1rem', fontSize: '0.875rem', color: '#6b7280' }}>
              💡 提示: 点击"保存"后，表格会立即更新（乐观更新）。如果保存失败，将自动回滚到原始状态。
            </p>
          </div>
        )}
      </Modal>
    </section>
  );
}
