import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { buildService } from '@/services/build';
import { repositoryService } from '@/services/repository';
import { useWebSocket } from '@/hooks/useWebSocket';
import {
  Activity,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Search,
  Filter,
  Calendar,
  GitBranch,
  Play,
  RefreshCcw,
} from 'lucide-react';
import { format } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import { BuildStatus, WebSocketMessage } from '@/types';

const statusConfig: Record<BuildStatus, { icon: any; color: string; text: string; bgColor: string }> = {
  pending: { icon: Clock, color: 'text-gray-500', text: '等待中', bgColor: 'bg-gray-100 text-gray-700' },
  running: { icon: Activity, color: 'text-blue-500 animate-pulse', text: '运行中', bgColor: 'bg-blue-100 text-blue-700' },
  success: { icon: CheckCircle, color: 'text-green-500', text: '成功', bgColor: 'bg-green-100 text-green-700' },
  failure: { icon: XCircle, color: 'text-red-500', text: '失败', bgColor: 'bg-red-100 text-red-700' },
  killed: { icon: AlertCircle, color: 'text-orange-500', text: '已取消', bgColor: 'bg-orange-100 text-orange-700' },
  error: { icon: XCircle, color: 'text-red-500', text: '错误', bgColor: 'bg-red-100 text-red-700' },
};

export default function BuildListPage() {
  const [page, setPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState<BuildStatus | null>(null);
  const [repoFilter, setRepoFilter] = useState<number | null>(null);
  const [search, setSearch] = useState('');
  const [dateRange, setDateRange] = useState<'all' | 'today' | 'week' | 'month'>('all');
  const pageSize = 20;

  // Fetch all builds
  const { data: buildsData, isLoading, refetch } = useQuery({
    queryKey: ['builds-all', page, statusFilter, repoFilter, dateRange],
    queryFn: () => buildService.listAll(page, pageSize),
    refetchInterval: 10000, // Refresh every 10 seconds
  });

  // Fetch repositories for filter dropdown
  const { data: reposData } = useQuery({
    queryKey: ['repositories-list'],
    queryFn: () => repositoryService.list(1, 100),
  });

  // WebSocket for real-time status updates
  const { isConnected } = useWebSocket({
    onMessage: (message: WebSocketMessage) => {
      if (message.type === 'build_status') {
        refetch();
      }
    },
  });

  // Filter builds client-side (in production, this should be server-side)
  const filteredBuilds = buildsData?.items?.filter((build) => {
    if (statusFilter && build.status !== statusFilter) return false;
    if (repoFilter && build.repository_id !== repoFilter) return false;
    if (search) {
      const searchLower = search.toLowerCase();
      const matchesCommit = build.commit?.toLowerCase().includes(searchLower);
      const matchesMessage = build.commit_message?.toLowerCase().includes(searchLower);
      const matchesBranch = build.branch?.toLowerCase().includes(searchLower);
      if (!matchesCommit && !matchesMessage && !matchesBranch) return false;
    }

    // Date range filter
    if (dateRange !== 'all' && build.created_at) {
      const buildDate = new Date(build.created_at);
      const now = new Date();
      const daysDiff = (now.getTime() - buildDate.getTime()) / (1000 * 60 * 60 * 24);

      if (dateRange === 'today' && daysDiff > 1) return false;
      if (dateRange === 'week' && daysDiff > 7) return false;
      if (dateRange === 'month' && daysDiff > 30) return false;
    }

    return true;
  });

  const totalPages = Math.ceil((buildsData?.total || 0) / pageSize);

  // Count builds by status
  const statusCounts = filteredBuilds?.reduce((acc, build) => {
    acc[build.status] = (acc[build.status] || 0) + 1;
    return acc;
  }, {} as Record<BuildStatus, number>);

  return (
    <div>
      {/* Header */}
      <div className="mb-6 flex items-start justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">构建记录</h1>
          <p className="text-gray-600 mt-2">查看所有构建记录和状态</p>
        </div>
        <div className="flex items-center space-x-3">
          {isConnected && (
            <span className="flex items-center text-sm text-green-600">
              <span className="w-2 h-2 bg-green-500 rounded-full mr-2 animate-pulse"></span>
              实时连接
            </span>
          )}
          <button onClick={() => refetch()} className="btn btn-secondary flex items-center">
            <RefreshCcw className="w-4 h-4 mr-2" />
            刷新
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 md:grid-cols-6 gap-4 mb-6">
        {Object.entries(statusConfig).map(([status, config]) => {
          const count = statusCounts?.[status as BuildStatus] || 0;
          const StatusIcon = config.icon;
          return (
            <button
              key={status}
              onClick={() => setStatusFilter(statusFilter === status ? null : (status as BuildStatus))}
              className={`card text-center transition-all ${
                statusFilter === status ? 'ring-2 ring-primary-500 shadow-lg' : 'hover:shadow-md'
              }`}
            >
              <StatusIcon className={`w-6 h-6 mx-auto mb-2 ${config.color}`} />
              <div className="text-2xl font-bold text-gray-900">{count}</div>
              <div className="text-xs text-gray-600 mt-1">{config.text}</div>
            </button>
          );
        })}
      </div>

      {/* Filters */}
      <div className="card mb-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <input
              type="text"
              placeholder="搜索提交信息、分支..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="input pl-10 w-full"
            />
          </div>

          {/* Repository Filter */}
          <div className="relative">
            <GitBranch className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <select
              value={repoFilter || ''}
              onChange={(e) => setRepoFilter(e.target.value ? parseInt(e.target.value) : null)}
              className="input pl-10 w-full"
            >
              <option value="">所有仓库</option>
              {reposData?.items?.map((repo) => (
                <option key={repo.id} value={repo.id}>
                  {repo.name}
                </option>
              ))}
            </select>
          </div>

          {/* Status Filter */}
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <select
              value={statusFilter || ''}
              onChange={(e) => setStatusFilter(e.target.value ? (e.target.value as BuildStatus) : null)}
              className="input pl-10 w-full"
            >
              <option value="">所有状态</option>
              {Object.entries(statusConfig).map(([status, config]) => (
                <option key={status} value={status}>
                  {config.text}
                </option>
              ))}
            </select>
          </div>

          {/* Date Range Filter */}
          <div className="relative">
            <Calendar className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <select
              value={dateRange}
              onChange={(e) => setDateRange(e.target.value as any)}
              className="input pl-10 w-full"
            >
              <option value="all">所有时间</option>
              <option value="today">今天</option>
              <option value="week">最近7天</option>
              <option value="month">最近30天</option>
            </select>
          </div>
        </div>

        {/* Active Filters Display */}
        {(search || statusFilter || repoFilter || dateRange !== 'all') && (
          <div className="mt-4 flex items-center gap-2">
            <span className="text-sm text-gray-600">已应用过滤:</span>
            {search && (
              <span className="px-2 py-1 bg-gray-100 text-gray-700 text-sm rounded">
                搜索: {search}
              </span>
            )}
            {statusFilter && (
              <span className="px-2 py-1 bg-gray-100 text-gray-700 text-sm rounded">
                状态: {statusConfig[statusFilter].text}
              </span>
            )}
            {repoFilter && (
              <span className="px-2 py-1 bg-gray-100 text-gray-700 text-sm rounded">
                仓库: {reposData?.items?.find((r) => r.id === repoFilter)?.name}
              </span>
            )}
            {dateRange !== 'all' && (
              <span className="px-2 py-1 bg-gray-100 text-gray-700 text-sm rounded">
                时间: {
                  dateRange === 'today' ? '今天' :
                  dateRange === 'week' ? '最近7天' :
                  dateRange === 'month' ? '最近30天' : '所有时间'
                }
              </span>
            )}
            <button
              onClick={() => {
                setSearch('');
                setStatusFilter(null);
                setRepoFilter(null);
                setDateRange('all');
              }}
              className="text-sm text-primary-600 hover:text-primary-700"
            >
              清除所有
            </button>
          </div>
        )}
      </div>

      {/* Loading State */}
      {isLoading && (
        <div className="text-center py-12">
          <div className="inline-block w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-gray-600 mt-4">加载中...</p>
        </div>
      )}

      {/* Build List */}
      {!isLoading && filteredBuilds && filteredBuilds.length > 0 ? (
        <>
          <div className="card divide-y divide-gray-200">
            {filteredBuilds.map((build) => {
              const StatusIcon = statusConfig[build.status].icon;
              const repo = reposData?.items?.find((r) => r.id === build.repository_id);

              return (
                <Link
                  key={build.id}
                  to={`/builds/${build.id}`}
                  className="block p-4 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center flex-1 min-w-0">
                      <StatusIcon className={`w-5 h-5 mr-3 flex-shrink-0 ${statusConfig[build.status].color}`} />

                      <div className="flex-1 min-w-0">
                        <div className="flex items-center mb-1">
                          <span className="font-medium text-gray-900 mr-2">
                            #{build.drone_build_number || build.id}
                          </span>
                          {repo && (
                            <>
                              <GitBranch className="w-3 h-3 text-gray-400 mr-1" />
                              <span className="text-sm text-gray-600 mr-3">{repo.name}</span>
                            </>
                          )}
                          <span className="px-2 py-0.5 text-xs bg-blue-100 text-blue-700 rounded">
                            {build.branch}
                          </span>
                        </div>

                        <div className="flex items-center text-sm text-gray-600 space-x-4">
                          <span className="truncate max-w-md">
                            {build.commit_message || build.commit?.substring(0, 7)}
                          </span>
                          {build.trigger && (
                            <span className="flex items-center flex-shrink-0">
                              <Play className="w-3 h-3 mr-1" />
                              {build.trigger}
                            </span>
                          )}
                          {build.duration && (
                            <span className="flex items-center flex-shrink-0">
                              <Clock className="w-3 h-3 mr-1" />
                              {Math.floor(build.duration / 60)}分{build.duration % 60}秒
                            </span>
                          )}
                        </div>

                        {build.author_name && (
                          <div className="text-xs text-gray-500 mt-1">
                            作者: {build.author_name}
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="ml-4 flex items-center space-x-4 flex-shrink-0">
                      {build.created_at && (
                        <span className="text-sm text-gray-500">
                          {format(new Date(build.created_at), 'MM-dd HH:mm', { locale: zhCN })}
                        </span>
                      )}
                      <span className={`px-3 py-1 text-sm font-medium rounded-full ${statusConfig[build.status].bgColor}`}>
                        {statusConfig[build.status].text}
                      </span>
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-6 flex items-center justify-center space-x-2">
              <button
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page === 1}
                className="btn btn-secondary"
              >
                上一页
              </button>

              <div className="flex items-center space-x-1">
                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  let pageNum;
                  if (totalPages <= 5) {
                    pageNum = i + 1;
                  } else if (page <= 3) {
                    pageNum = i + 1;
                  } else if (page >= totalPages - 2) {
                    pageNum = totalPages - 4 + i;
                  } else {
                    pageNum = page - 2 + i;
                  }

                  return (
                    <button
                      key={pageNum}
                      onClick={() => setPage(pageNum)}
                      className={`px-3 py-2 rounded ${
                        page === pageNum
                          ? 'bg-primary-600 text-white'
                          : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-300'
                      }`}
                    >
                      {pageNum}
                    </button>
                  );
                })}
              </div>

              <button
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={page === totalPages}
                className="btn btn-secondary"
              >
                下一页
              </button>
            </div>
          )}

          {/* Results Count */}
          <p className="text-center text-sm text-gray-600 mt-4">
            显示 {filteredBuilds.length} 条记录，共 {buildsData?.total || 0} 条
          </p>
        </>
      ) : (
        !isLoading && (
          <div className="text-center py-12">
            <Activity className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">暂无构建记录</h3>
            <p className="text-gray-600">
              {search || statusFilter || repoFilter || dateRange !== 'all'
                ? '没有找到符合条件的构建记录'
                : '还没有任何构建记录'}
            </p>
          </div>
        )
      )}
    </div>
  );
}
