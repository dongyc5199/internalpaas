import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { useReleases } from '../hooks/useReleases';
import { Table } from '../components/Table/Table';
import { Button } from '../components/Button/Button';
import type { ReleaseSummary, ReleaseStatus, ReleaseEnvironment } from '../types/release';

/**
 * ReleasesPage Component
 *
 * Displays a list of releases with filtering capabilities.
 * Integrates useReleases hook for data fetching with React Query caching.
 */
export function ReleasesPage(): JSX.Element {
  // State for filters
  const [selectedStatus, setSelectedStatus] = useState<ReleaseStatus | 'all'>('all');
  const [selectedEnvironment, setSelectedEnvironment] = useState<ReleaseEnvironment | 'all'>('all');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  // Build filters object
  const filters =
    selectedStatus === 'all' && selectedEnvironment === 'all'
      ? undefined
      : {
          ...(selectedStatus !== 'all' && { status: [selectedStatus] }),
          ...(selectedEnvironment !== 'all' && { environment: [selectedEnvironment] }),
        };

  // Fetch releases with React Query
  const { data, isLoading, error, refetch, isFetching } = useReleases({
    filters,
    page: currentPage,
    pageSize,
  });

  // Status badge component
  const StatusBadge = ({ status }: { status: ReleaseStatus }) => {
    const colors: Record<ReleaseStatus, string> = {
      draft: '#6b7280',
      pending: '#f59e0b',
      approved: '#3b82f6',
      deploying: '#8b5cf6',
      deployed: '#22c55e',
      failed: '#ef4444',
      rolled_back: '#f97316',
      archived: '#9ca3af',
    };

    const labels: Record<ReleaseStatus, string> = {
      draft: '草稿',
      pending: '待审批',
      approved: '已批准',
      deploying: '部署中',
      deployed: '已部署',
      failed: '失败',
      rolled_back: '已回滚',
      archived: '已归档',
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

  // Format date
  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <section className="dp-page" data-testid="releases-page">
      <header className="dp-page__header">
        <div>
          <h3 className="dp-page__title">发布流水线</h3>
          <p className="dp-page__description">
            按应用与环境跟踪发布状态、审批进度与金丝雀阶段。
          </p>
        </div>
        <div className="dp-page__filters">
          <select
            aria-label="筛选状态"
            value={selectedStatus}
            onChange={(e) => {
              setSelectedStatus(e.target.value as ReleaseStatus | 'all');
              setCurrentPage(1); // Reset to first page
            }}
            style={{ marginRight: '0.5rem', padding: '0.5rem', borderRadius: '4px' }}
          >
            <option value="all">全部状态</option>
            <option value="draft">草稿</option>
            <option value="pending">待审批</option>
            <option value="approved">已批准</option>
            <option value="deploying">部署中</option>
            <option value="deployed">已部署</option>
            <option value="failed">失败</option>
            <option value="rolled_back">已回滚</option>
            <option value="archived">已归档</option>
          </select>
          <select
            aria-label="筛选环境"
            value={selectedEnvironment}
            onChange={(e) => {
              setSelectedEnvironment(e.target.value as ReleaseEnvironment | 'all');
              setCurrentPage(1);
            }}
            style={{ marginRight: '0.5rem', padding: '0.5rem', borderRadius: '4px' }}
          >
            <option value="all">全部环境</option>
            <option value="development">开发环境</option>
            <option value="testing">测试环境</option>
            <option value="staging">预发布环境</option>
            <option value="production">生产环境</option>
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
        <div className="dp-page__placeholder" data-testid="releases-loading">
          正在加载发布列表...
        </div>
      )}

      {error && (
        <div
          className="dp-page__placeholder dp-page__placeholder--error"
          data-testid="releases-error"
        >
          获取发布列表失败: {error.message}
          <br />
          <Button variant="primary" size="sm" onClick={() => refetch()} style={{ marginTop: '1rem' }}>
            重试
          </Button>
        </div>
      )}

      {!isLoading && !error && data && (
        <>
          <div style={{ marginBottom: '1rem', color: '#6b7280', fontSize: '0.875rem' }}>
            共 {data.total} 条发布记录 {isFetching && <span>（刷新中...）</span>}
          </div>

          {data.releases.length === 0 ? (
            <div className="dp-page__placeholder">暂无发布记录</div>
          ) : (
            <Table<ReleaseSummary>
              data={data.releases}
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
                  key: 'version',
                  label: '版本',
                  dataKey: 'version',
                  width: '100px',
                  sortable: true,
                },
                {
                  key: 'name',
                  label: '发布名称',
                  dataKey: 'name',
                  sortable: true,
                },
                {
                  key: 'applicationName',
                  label: '应用',
                  dataKey: 'applicationName',
                  sortable: true,
                },
                {
                  key: 'environment',
                  label: '环境',
                  dataKey: 'environment',
                  width: '100px',
                },
                {
                  key: 'status',
                  label: '状态',
                  width: '100px',
                  align: 'center',
                  render: (row) => <StatusBadge status={row.status} />,
                },
                {
                  key: 'createdAt',
                  label: '创建时间',
                  width: '160px',
                  sortable: true,
                  render: (row) => formatDate(row.createdAt),
                },
                {
                  key: 'deployedAt',
                  label: '部署时间',
                  width: '160px',
                  render: (row) => (row.deployedAt ? formatDate(row.deployedAt) : '-'),
                },
                {
                  key: 'actions',
                  label: '操作',
                  width: '120px',
                  align: 'center',
                  render: (row) => (
                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                      <Button size="sm" variant="outline">
                        查看
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

      <Outlet />
    </section>
  );
}

export function ReleaseDetailsPlaceholder(): JSX.Element {
  return (
    <section
      className="dp-page dp-page--inset"
      data-testid="release-details-placeholder"
      aria-label="发布详情占位"
    >
      <p>请选择一个发布记录以查看详细流水线与守门指标。</p>
    </section>
  );
}
