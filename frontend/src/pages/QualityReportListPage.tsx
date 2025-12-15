import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { qualityService } from '@/services/quality';
import { repositoryService } from '@/services/repository';
import {
  FileSearch,
  Search,
  Filter,
  Calendar,
  TrendingUp,
  TrendingDown,
  CheckCircle,
  XCircle,
  AlertTriangle,
  MinusCircle,
  GitBranch,
  Bug,
  Shield,
  Code,
  AlertCircle as AlertCircleIcon,
} from 'lucide-react';
import { format } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import { QualityGate, QualityReport } from '@/types';

const qualityGateConfig: Record<QualityGate, { icon: any; color: string; text: string; bgColor: string }> = {
  PASSED: { icon: CheckCircle, color: 'text-green-500', text: '通过', bgColor: 'bg-green-100 text-green-700' },
  FAILED: { icon: XCircle, color: 'text-red-500', text: '失败', bgColor: 'bg-red-100 text-red-700' },
  WARN: { icon: AlertTriangle, color: 'text-yellow-500', text: '警告', bgColor: 'bg-yellow-100 text-yellow-700' },
  NONE: { icon: MinusCircle, color: 'text-gray-400', text: '未设置', bgColor: 'bg-gray-100 text-gray-600' },
};

// Rating color helper
const getRatingColor = (rating?: string) => {
  if (!rating) return 'text-gray-400';
  const value = parseFloat(rating);
  if (value <= 2) return 'text-green-600';
  if (value <= 3) return 'text-yellow-600';
  return 'text-red-600';
};

export default function QualityReportListPage() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [gateFilter, setGateFilter] = useState<QualityGate | null>(null);
  const [repoFilter, setRepoFilter] = useState<number | null>(null);
  const pageSize = 20;

  // Fetch repositories for filter
  const { data: reposData } = useQuery({
    queryKey: ['repositories-list'],
    queryFn: () => repositoryService.list(1, 100),
  });

  // Fetch quality reports (need to implement listAll in service)
  const { data: reportsData, isLoading } = useQuery({
    queryKey: ['quality-reports-all', page, gateFilter, repoFilter],
    queryFn: async () => {
      // For now, we'll fetch from the first repo (this should be listAll in production)
      if (reposData?.items?.length) {
        const repoId = repoFilter || reposData.items[0].id;
        return qualityService.list(repoId, page, pageSize);
      }
      return { items: [], total: 0, page, page_size: pageSize };
    },
    enabled: !!reposData?.items?.length,
  });

  // Filter reports client-side
  const filteredReports = reportsData?.items?.filter((report) => {
    if (gateFilter && report.quality_gate_status !== gateFilter) return false;
    if (repoFilter && report.repository_id !== repoFilter) return false;
    if (search) {
      const searchLower = search.toLowerCase();
      const matchesBranch = report.branch?.toLowerCase().includes(searchLower);
      const matchesCommit = report.commit?.toLowerCase().includes(searchLower);
      if (!matchesBranch && !matchesCommit) return false;
    }
    return true;
  });

  // Calculate statistics
  const stats = filteredReports?.reduce(
    (acc, report) => {
      acc[report.quality_gate_status] = (acc[report.quality_gate_status] || 0) + 1;
      acc.totalBugs += report.bugs || 0;
      acc.totalVulnerabilities += report.vulnerabilities || 0;
      acc.totalCodeSmells += report.code_smells || 0;
      acc.totalCoverage += report.coverage || 0;
      acc.count += 1;
      return acc;
    },
    {
      PASSED: 0,
      FAILED: 0,
      WARN: 0,
      NONE: 0,
      totalBugs: 0,
      totalVulnerabilities: 0,
      totalCodeSmells: 0,
      totalCoverage: 0,
      count: 0,
    }
  );

  const avgCoverage = stats && stats.count > 0 ? (stats.totalCoverage / stats.count).toFixed(1) : '0.0';

  const totalPages = Math.ceil((reportsData?.total || 0) / pageSize);

  return (
    <div>
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">质量报告</h1>
        <p className="text-gray-600 mt-2">查看代码质量分析报告和趋势</p>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        {/* Quality Gate Stats */}
        {Object.entries(qualityGateConfig).map(([gate, config]) => {
          const count = stats?.[gate as QualityGate] || 0;
          const GateIcon = config.icon;
          return (
            <button
              key={gate}
              onClick={() => setGateFilter(gateFilter === gate ? null : (gate as QualityGate))}
              className={`card text-center transition-all ${
                gateFilter === gate ? 'ring-2 ring-primary-500 shadow-lg' : 'hover:shadow-md'
              }`}
            >
              <GateIcon className={`w-6 h-6 mx-auto mb-2 ${config.color}`} />
              <div className="text-2xl font-bold text-gray-900">{count}</div>
              <div className="text-xs text-gray-600 mt-1">{config.text}</div>
            </button>
          );
        })}
      </div>

      {/* Issue Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm text-gray-600 mb-1">总缺陷</div>
              <div className="text-2xl font-bold text-red-600">{stats?.totalBugs || 0}</div>
            </div>
            <Bug className="w-10 h-10 text-red-300" />
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm text-gray-600 mb-1">安全漏洞</div>
              <div className="text-2xl font-bold text-red-600">{stats?.totalVulnerabilities || 0}</div>
            </div>
            <Shield className="w-10 h-10 text-red-300" />
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm text-gray-600 mb-1">代码异味</div>
              <div className="text-2xl font-bold text-yellow-600">{stats?.totalCodeSmells || 0}</div>
            </div>
            <Code className="w-10 h-10 text-yellow-300" />
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm text-gray-600 mb-1">平均覆盖率</div>
              <div className="text-2xl font-bold text-green-600">{avgCoverage}%</div>
            </div>
            <CheckCircle className="w-10 h-10 text-green-300" />
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="card mb-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <input
              type="text"
              placeholder="搜索分支、提交..."
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

          {/* Quality Gate Filter */}
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <select
              value={gateFilter || ''}
              onChange={(e) => setGateFilter(e.target.value ? (e.target.value as QualityGate) : null)}
              className="input pl-10 w-full"
            >
              <option value="">所有质量门</option>
              {Object.entries(qualityGateConfig).map(([gate, config]) => (
                <option key={gate} value={gate}>
                  {config.text}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Active Filters Display */}
        {(search || gateFilter || repoFilter) && (
          <div className="mt-4 flex items-center gap-2">
            <span className="text-sm text-gray-600">已应用过滤:</span>
            {search && (
              <span className="px-2 py-1 bg-gray-100 text-gray-700 text-sm rounded">
                搜索: {search}
              </span>
            )}
            {gateFilter && (
              <span className="px-2 py-1 bg-gray-100 text-gray-700 text-sm rounded">
                质量门: {qualityGateConfig[gateFilter].text}
              </span>
            )}
            {repoFilter && (
              <span className="px-2 py-1 bg-gray-100 text-gray-700 text-sm rounded">
                仓库: {reposData?.items?.find((r) => r.id === repoFilter)?.name}
              </span>
            )}
            <button
              onClick={() => {
                setSearch('');
                setGateFilter(null);
                setRepoFilter(null);
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

      {/* Reports List */}
      {!isLoading && filteredReports && filteredReports.length > 0 ? (
        <>
          <div className="space-y-4">
            {filteredReports.map((report) => {
              const GateIcon = qualityGateConfig[report.quality_gate_status].icon;
              const repo = reposData?.items?.find((r) => r.id === report.repository_id);

              return (
                <Link
                  key={report.id}
                  to={`/quality-reports/${report.id}`}
                  className="card hover:shadow-lg transition-shadow"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-start flex-1">
                      <GateIcon
                        className={`w-6 h-6 mr-3 flex-shrink-0 mt-1 ${
                          qualityGateConfig[report.quality_gate_status].color
                        }`}
                      />

                      <div className="flex-1 min-w-0">
                        {/* Header */}
                        <div className="flex items-center mb-2">
                          <h3 className="text-lg font-semibold text-gray-900 mr-3">
                            {repo?.name || `Repository ${report.repository_id}`}
                          </h3>
                          <span className="px-2 py-1 text-xs bg-blue-100 text-blue-700 rounded">
                            {report.branch}
                          </span>
                          <span
                            className={`ml-2 px-3 py-1 text-sm font-medium rounded-full ${
                              qualityGateConfig[report.quality_gate_status].bgColor
                            }`}
                          >
                            {qualityGateConfig[report.quality_gate_status].text}
                          </span>
                        </div>

                        {/* Metrics Grid */}
                        <div className="grid grid-cols-2 md:grid-cols-6 gap-4 mt-3">
                          <div>
                            <div className="text-xs text-gray-500">缺陷</div>
                            <div className={`text-lg font-semibold ${report.bugs > 0 ? 'text-red-600' : 'text-gray-900'}`}>
                              {report.bugs}
                            </div>
                          </div>

                          <div>
                            <div className="text-xs text-gray-500">漏洞</div>
                            <div className={`text-lg font-semibold ${report.vulnerabilities > 0 ? 'text-red-600' : 'text-gray-900'}`}>
                              {report.vulnerabilities}
                            </div>
                          </div>

                          <div>
                            <div className="text-xs text-gray-500">代码异味</div>
                            <div className={`text-lg font-semibold ${report.code_smells > 0 ? 'text-yellow-600' : 'text-gray-900'}`}>
                              {report.code_smells}
                            </div>
                          </div>

                          <div>
                            <div className="text-xs text-gray-500">覆盖率</div>
                            <div className={`text-lg font-semibold ${report.coverage >= 80 ? 'text-green-600' : report.coverage >= 50 ? 'text-yellow-600' : 'text-red-600'}`}>
                              {report.coverage.toFixed(1)}%
                            </div>
                          </div>

                          <div>
                            <div className="text-xs text-gray-500">重复率</div>
                            <div className="text-lg font-semibold text-gray-900">
                              {report.duplications.toFixed(1)}%
                            </div>
                          </div>

                          <div>
                            <div className="text-xs text-gray-500">技术债</div>
                            <div className="text-lg font-semibold text-gray-900">
                              {Math.floor(report.technical_debt / 60)}h
                            </div>
                          </div>
                        </div>

                        {/* Ratings */}
                        <div className="flex items-center mt-3 space-x-4 text-sm">
                          {report.reliability_rating && (
                            <span className="flex items-center">
                              <span className="text-gray-600 mr-1">可靠性:</span>
                              <span className={`font-semibold ${getRatingColor(report.reliability_rating)}`}>
                                {report.reliability_rating}
                              </span>
                            </span>
                          )}
                          {report.security_rating && (
                            <span className="flex items-center">
                              <span className="text-gray-600 mr-1">安全性:</span>
                              <span className={`font-semibold ${getRatingColor(report.security_rating)}`}>
                                {report.security_rating}
                              </span>
                            </span>
                          )}
                          {report.maintainability_rating && (
                            <span className="flex items-center">
                              <span className="text-gray-600 mr-1">可维护性:</span>
                              <span className={`font-semibold ${getRatingColor(report.maintainability_rating)}`}>
                                {report.maintainability_rating}
                              </span>
                            </span>
                          )}
                        </div>

                        {/* Footer */}
                        <div className="flex items-center mt-3 text-xs text-gray-500 space-x-4">
                          <span>提交: {report.commit.substring(0, 7)}</span>
                          {report.analyzed_at && (
                            <span>
                              分析时间: {format(new Date(report.analyzed_at), 'yyyy-MM-dd HH:mm', { locale: zhCN })}
                            </span>
                          )}
                          <span>代码行数: {report.lines.toLocaleString()}</span>
                        </div>
                      </div>
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
            显示 {filteredReports.length} 条报告，共 {reportsData?.total || 0} 条
          </p>
        </>
      ) : (
        !isLoading && (
          <div className="text-center py-12">
            <FileSearch className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">暂无质量报告</h3>
            <p className="text-gray-600">
              {search || gateFilter || repoFilter
                ? '没有找到符合条件的质量报告'
                : '还没有任何质量报告，等待构建完成后自动生成'}
            </p>
          </div>
        )
      )}
    </div>
  );
}
