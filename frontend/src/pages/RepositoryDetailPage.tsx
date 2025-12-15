import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { repositoryService } from '@/services/repository';
import { buildService } from '@/services/build';
import {
  ArrowLeft,
  GitBranch,
  Activity,
  Play,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Settings,
} from 'lucide-react';
import { format } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import { BuildStatus } from '@/types';
import { useState } from 'react';

const statusConfig: Record<BuildStatus, { icon: any; color: string; text: string }> = {
  pending: { icon: Clock, color: 'text-gray-500', text: '等待中' },
  running: { icon: Activity, color: 'text-blue-500 animate-pulse', text: '运行中' },
  success: { icon: CheckCircle, color: 'text-green-500', text: '成功' },
  failure: { icon: XCircle, color: 'text-red-500', text: '失败' },
  killed: { icon: AlertCircle, color: 'text-orange-500', text: '已取消' },
  error: { icon: XCircle, color: 'text-red-500', text: '错误' },
};

export default function RepositoryDetailPage() {
  const { id } = useParams<{ id: string }>();
  const repoId = parseInt(id || '0');
  const queryClient = useQueryClient();
  const [showTriggerBuild, setShowTriggerBuild] = useState(false);

  // 获取仓库详情
  const { data: repository, isLoading: repoLoading } = useQuery({
    queryKey: ['repository', repoId],
    queryFn: () => repositoryService.get(repoId),
    enabled: !!repoId,
  });

  // 获取构建列表
  const { data: buildsData, isLoading: buildsLoading } = useQuery({
    queryKey: ['builds', repoId],
    queryFn: () => buildService.list(repoId, 1, 10),
    enabled: !!repoId,
  });

  // 启用/禁用CI
  const toggleCIMutation = useMutation({
    mutationFn: (enable: boolean) => repositoryService.updateCI(repoId, enable),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repository', repoId] });
    },
  });

  if (repoLoading) {
    return (
      <div className="text-center py-12">
        <div className="inline-block w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full animate-spin"></div>
        <p className="text-gray-600 mt-4">加载中...</p>
      </div>
    );
  }

  if (!repository) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-600">仓库不存在</p>
        <Link to="/repositories" className="text-primary-600 hover:text-primary-700 mt-4 inline-block">
          返回仓库列表
        </Link>
      </div>
    );
  }

  return (
    <div>
      {/* Header */}
      <div className="mb-6">
        <Link
          to={`/projects/${repository.project_id}`}
          className="inline-flex items-center text-gray-600 hover:text-gray-900 mb-4"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          返回项目
        </Link>

        <div className="flex items-start justify-between">
          <div className="flex-1">
            <div className="flex items-center mb-2">
              <GitBranch className="w-6 h-6 text-gray-400 mr-3" />
              <h1 className="text-3xl font-bold text-gray-900">{repository.name}</h1>
              {repository.is_private && (
                <span className="ml-3 px-2 py-1 text-xs bg-gray-100 text-gray-600 rounded">
                  私有
                </span>
              )}
            </div>
            <p className="text-gray-600 mt-2">{repository.description || '暂无描述'}</p>

            <div className="flex items-center space-x-6 mt-4 text-sm text-gray-500">
              {repository.language && <span>语言: {repository.language}</span>}
              <span>⭐ {repository.star_count}</span>
              <span>🔱 {repository.fork_count}</span>
              <span>{(repository.size / 1024).toFixed(1)} MB</span>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={() => toggleCIMutation.mutate(!repository.drone_active)}
              className={`btn ${repository.drone_active ? 'btn-danger' : 'btn-primary'}`}
              disabled={toggleCIMutation.isPending}
            >
              <Settings className="w-4 h-4 mr-2" />
              {repository.drone_active ? '禁用CI' : '启用CI'}
            </button>

            {repository.drone_active && (
              <button
                onClick={() => setShowTriggerBuild(true)}
                className="btn btn-primary flex items-center"
              >
                <Play className="w-4 h-4 mr-2" />
                触发构建
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Repository Info Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="card">
          <div className="text-sm text-gray-600 mb-1">默认分支</div>
          <div className="text-lg font-semibold text-gray-900">
            {repository.default_branch || 'main'}
          </div>
        </div>
        <div className="card">
          <div className="text-sm text-gray-600 mb-1">CI状态</div>
          <div className="text-lg font-semibold">
            {repository.drone_active ? (
              <span className="text-green-600">已启用</span>
            ) : (
              <span className="text-gray-400">未启用</span>
            )}
          </div>
        </div>
        <div className="card">
          <div className="text-sm text-gray-600 mb-1">问题数</div>
          <div className="text-lg font-semibold text-gray-900">{repository.issue_count}</div>
        </div>
      </div>

      {/* Clone URLs */}
      <div className="card mb-8">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">克隆地址</h2>
        <div className="space-y-3">
          {repository.clone_url && (
            <div>
              <label className="block text-sm text-gray-600 mb-1">HTTPS</label>
              <input
                type="text"
                value={repository.clone_url}
                readOnly
                className="input font-mono text-sm"
                onClick={(e) => e.currentTarget.select()}
              />
            </div>
          )}
          {repository.ssh_url && (
            <div>
              <label className="block text-sm text-gray-600 mb-1">SSH</label>
              <input
                type="text"
                value={repository.ssh_url}
                readOnly
                className="input font-mono text-sm"
                onClick={(e) => e.currentTarget.select()}
              />
            </div>
          )}
        </div>
      </div>

      {/* Build Records */}
      <div className="card">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">构建记录</h2>

        {buildsLoading ? (
          <div className="text-center py-8">
            <div className="inline-block w-6 h-6 border-4 border-primary-600 border-t-transparent rounded-full animate-spin"></div>
          </div>
        ) : buildsData?.items && buildsData.items.length > 0 ? (
          <div className="space-y-3">
            {buildsData.items.map((build) => {
              const StatusIcon = statusConfig[build.status].icon;

              return (
                <Link
                  key={build.id}
                  to={`/builds/${build.id}`}
                  className="block p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center flex-1">
                      <StatusIcon className={`w-5 h-5 mr-3 ${statusConfig[build.status].color}`} />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center mb-1">
                          <span className="font-medium text-gray-900">
                            #{build.drone_build_number || build.id}
                          </span>
                          <span className="mx-2 text-gray-400">•</span>
                          <span className="text-sm text-gray-600 truncate">
                            {build.commit_message || build.commit?.substring(0, 7)}
                          </span>
                        </div>
                        <div className="flex items-center text-sm text-gray-500 space-x-3">
                          <span>{build.branch}</span>
                          <span>{build.trigger}</span>
                          {build.duration && (
                            <span>{Math.floor(build.duration / 60)}分{build.duration % 60}秒</span>
                          )}
                          {build.created_at && (
                            <span>
                              {format(new Date(build.created_at), 'yyyy-MM-dd HH:mm', {
                                locale: zhCN,
                              })}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="ml-4">
                      <span
                        className={`px-3 py-1 text-sm font-medium rounded-full ${
                          build.status === 'success'
                            ? 'bg-green-100 text-green-700'
                            : build.status === 'running'
                            ? 'bg-blue-100 text-blue-700'
                            : build.status === 'failure' || build.status === 'error'
                            ? 'bg-red-100 text-red-700'
                            : 'bg-gray-100 text-gray-700'
                        }`}
                      >
                        {statusConfig[build.status].text}
                      </span>
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        ) : (
          <div className="text-center py-8">
            <Activity className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-600">暂无构建记录</p>
            {repository.drone_active && (
              <button onClick={() => setShowTriggerBuild(true)} className="btn btn-primary mt-4">
                触发第一次构建
              </button>
            )}
          </div>
        )}

        {buildsData && buildsData.total > 10 && (
          <div className="mt-4 text-center">
            <Link
              to={`/repositories/${repoId}/builds`}
              className="text-primary-600 hover:text-primary-700 font-medium"
            >
              查看全部 {buildsData.total} 条记录 →
            </Link>
          </div>
        )}
      </div>

      {/* Trigger Build Modal */}
      {showTriggerBuild && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold mb-4">触发构建</h3>
            <p className="text-gray-600 mb-4">构建触发功能将在后续完善...</p>
            <button onClick={() => setShowTriggerBuild(false)} className="btn btn-secondary w-full">
              关闭
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
