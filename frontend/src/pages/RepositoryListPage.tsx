import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { repositoryService } from '@/services/repository';
import {
  GitBranch,
  Search,
  Filter,
  Activity,
  Lock,
  Unlock,
  CheckCircle,
  XCircle,
  Star,
  GitFork,
  HardDrive,
} from 'lucide-react';

export default function RepositoryListPage() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [filterActive, setFilterActive] = useState<boolean | null>(null);
  const [filterPrivate, setFilterPrivate] = useState<boolean | null>(null);
  const pageSize = 12;

  // Fetch repositories with filters
  const { data: reposData, isLoading } = useQuery({
    queryKey: ['repositories', page, search, filterActive, filterPrivate],
    queryFn: () => repositoryService.list(page, pageSize),
  });

  // Apply client-side filtering (in production, this should be server-side)
  const filteredRepos = reposData?.items?.filter((repo) => {
    if (search && !repo.name.toLowerCase().includes(search.toLowerCase())) {
      return false;
    }
    if (filterActive !== null && repo.drone_active !== filterActive) {
      return false;
    }
    if (filterPrivate !== null && repo.is_private !== filterPrivate) {
      return false;
    }
    return true;
  });

  const totalPages = Math.ceil((reposData?.total || 0) / pageSize);

  return (
    <div>
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">仓库列表</h1>
        <p className="text-gray-600 mt-2">管理和查看所有代码仓库</p>
      </div>

      {/* Search and Filters */}
      <div className="card mb-6">
        <div className="flex flex-col md:flex-row gap-4">
          {/* Search Input */}
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <input
              type="text"
              placeholder="搜索仓库名称..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="input pl-10 w-full"
            />
          </div>

          {/* Filter Buttons */}
          <div className="flex items-center space-x-2">
            <Filter className="w-5 h-5 text-gray-400" />

            {/* CI Status Filter */}
            <button
              onClick={() => setFilterActive(filterActive === true ? null : true)}
              className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                filterActive === true
                  ? 'bg-green-100 text-green-700 border border-green-300'
                  : 'bg-white text-gray-600 border border-gray-300 hover:bg-gray-50'
              }`}
            >
              <CheckCircle className="w-4 h-4 inline mr-1" />
              CI已启用
            </button>

            <button
              onClick={() => setFilterActive(filterActive === false ? null : false)}
              className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                filterActive === false
                  ? 'bg-gray-100 text-gray-700 border border-gray-300'
                  : 'bg-white text-gray-600 border border-gray-300 hover:bg-gray-50'
              }`}
            >
              <XCircle className="w-4 h-4 inline mr-1" />
              CI未启用
            </button>

            {/* Privacy Filter */}
            <button
              onClick={() => setFilterPrivate(filterPrivate === true ? null : true)}
              className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                filterPrivate === true
                  ? 'bg-yellow-100 text-yellow-700 border border-yellow-300'
                  : 'bg-white text-gray-600 border border-gray-300 hover:bg-gray-50'
              }`}
            >
              <Lock className="w-4 h-4 inline mr-1" />
              私有
            </button>

            <button
              onClick={() => setFilterPrivate(filterPrivate === false ? null : false)}
              className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                filterPrivate === false
                  ? 'bg-blue-100 text-blue-700 border border-blue-300'
                  : 'bg-white text-gray-600 border border-gray-300 hover:bg-gray-50'
              }`}
            >
              <Unlock className="w-4 h-4 inline mr-1" />
              公开
            </button>
          </div>
        </div>

        {/* Active Filters Display */}
        {(search || filterActive !== null || filterPrivate !== null) && (
          <div className="mt-4 flex items-center gap-2">
            <span className="text-sm text-gray-600">已应用过滤:</span>
            {search && (
              <span className="px-2 py-1 bg-gray-100 text-gray-700 text-sm rounded">
                搜索: {search}
              </span>
            )}
            {filterActive !== null && (
              <span className="px-2 py-1 bg-gray-100 text-gray-700 text-sm rounded">
                CI: {filterActive ? '已启用' : '未启用'}
              </span>
            )}
            {filterPrivate !== null && (
              <span className="px-2 py-1 bg-gray-100 text-gray-700 text-sm rounded">
                {filterPrivate ? '私有' : '公开'}
              </span>
            )}
            <button
              onClick={() => {
                setSearch('');
                setFilterActive(null);
                setFilterPrivate(null);
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

      {/* Repository Grid */}
      {!isLoading && filteredRepos && filteredRepos.length > 0 ? (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredRepos.map((repo) => (
              <Link
                key={repo.id}
                to={`/repositories/${repo.id}`}
                className="card hover:shadow-lg transition-shadow duration-200"
              >
                {/* Repository Header */}
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center flex-1 min-w-0">
                    <GitBranch className="w-5 h-5 text-gray-400 mr-2 flex-shrink-0" />
                    <h3 className="text-lg font-semibold text-gray-900 truncate">
                      {repo.name}
                    </h3>
                  </div>
                  <div className="flex items-center space-x-2 ml-2">
                    {repo.is_private ? (
                      <Lock className="w-4 h-4 text-yellow-600" />
                    ) : (
                      <Unlock className="w-4 h-4 text-blue-600" />
                    )}
                    {repo.drone_active && (
                      <Activity className="w-4 h-4 text-green-600 animate-pulse" />
                    )}
                  </div>
                </div>

                {/* Repository Description */}
                <p className="text-sm text-gray-600 mb-4 line-clamp-2 min-h-[40px]">
                  {repo.description || '暂无描述'}
                </p>

                {/* Repository Stats */}
                <div className="flex items-center justify-between text-xs text-gray-500">
                  <div className="flex items-center space-x-3">
                    {repo.language && (
                      <span className="flex items-center">
                        <span className="w-2 h-2 rounded-full bg-primary-500 mr-1"></span>
                        {repo.language}
                      </span>
                    )}
                    <span className="flex items-center">
                      <Star className="w-3 h-3 mr-1" />
                      {repo.star_count}
                    </span>
                    <span className="flex items-center">
                      <GitFork className="w-3 h-3 mr-1" />
                      {repo.fork_count}
                    </span>
                  </div>
                  <span className="flex items-center">
                    <HardDrive className="w-3 h-3 mr-1" />
                    {(repo.size / 1024).toFixed(1)} MB
                  </span>
                </div>

                {/* Status Badges */}
                <div className="mt-3 flex flex-wrap gap-2">
                  {repo.drone_active ? (
                    <span className="px-2 py-1 text-xs bg-green-100 text-green-700 rounded-full">
                      CI已启用
                    </span>
                  ) : (
                    <span className="px-2 py-1 text-xs bg-gray-100 text-gray-600 rounded-full">
                      CI未启用
                    </span>
                  )}
                  {repo.is_private && (
                    <span className="px-2 py-1 text-xs bg-yellow-100 text-yellow-700 rounded-full">
                      私有
                    </span>
                  )}
                  {repo.default_branch && (
                    <span className="px-2 py-1 text-xs bg-blue-100 text-blue-700 rounded-full">
                      {repo.default_branch}
                    </span>
                  )}
                </div>
              </Link>
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-8 flex items-center justify-center space-x-2">
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
            显示 {filteredRepos.length} 个仓库，共 {reposData?.total || 0} 个
          </p>
        </>
      ) : (
        !isLoading && (
          <div className="text-center py-12">
            <GitBranch className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">暂无仓库</h3>
            <p className="text-gray-600">
              {search || filterActive !== null || filterPrivate !== null
                ? '没有找到符合条件的仓库'
                : '还没有任何仓库，从项目页面创建第一个仓库'}
            </p>
          </div>
        )
      )}
    </div>
  );
}
