import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { projectService } from '@/services/project';
import { repositoryService } from '@/services/repository';
import { ArrowLeft, GitBranch, Plus, Activity } from 'lucide-react';
import { useState } from 'react';

export default function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const projectId = parseInt(id || '0');
  const [showCreateRepo, setShowCreateRepo] = useState(false);

  // 获取项目详情
  const { data: project, isLoading: projectLoading } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectService.get(projectId),
    enabled: !!projectId,
  });

  // 获取项目下的仓库列表
  const { data: reposData, isLoading: reposLoading } = useQuery({
    queryKey: ['repositories', projectId],
    queryFn: () => repositoryService.list(1, 20, projectId),
    enabled: !!projectId,
  });

  if (projectLoading) {
    return (
      <div className="text-center py-12">
        <div className="inline-block w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full animate-spin"></div>
        <p className="text-gray-600 mt-4">加载中...</p>
      </div>
    );
  }

  if (!project) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-600">项目不存在</p>
        <Link to="/projects" className="text-primary-600 hover:text-primary-700 mt-4 inline-block">
          返回项目列表
        </Link>
      </div>
    );
  }

  return (
    <div>
      {/* Header */}
      <div className="mb-6">
        <Link
          to="/projects"
          className="inline-flex items-center text-gray-600 hover:text-gray-900 mb-4"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          返回项目列表
        </Link>

        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">{project.display_name}</h1>
            <p className="text-gray-600 mt-2">{project.description || '暂无描述'}</p>
            <div className="flex items-center space-x-6 mt-4 text-sm text-gray-500">
              <div className="flex items-center">
                <GitBranch className="w-4 h-4 mr-1" />
                {project.repository_count} 个仓库
              </div>
              <div className="flex items-center">
                <Activity className="w-4 h-4 mr-1" />
                {project.build_count} 次构建
              </div>
            </div>
          </div>

          <button
            onClick={() => setShowCreateRepo(true)}
            className="btn btn-primary flex items-center"
          >
            <Plus className="w-4 h-4 mr-2" />
            创建仓库
          </button>
        </div>
      </div>

      {/* Project Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="card">
          <div className="text-sm text-gray-600 mb-1">仓库数量</div>
          <div className="text-2xl font-bold text-gray-900">{project.repository_count}</div>
        </div>
        <div className="card">
          <div className="text-sm text-gray-600 mb-1">团队成员</div>
          <div className="text-2xl font-bold text-gray-900">{project.member_count}</div>
        </div>
        <div className="card">
          <div className="text-sm text-gray-600 mb-1">总构建次数</div>
          <div className="text-2xl font-bold text-gray-900">{project.build_count}</div>
        </div>
      </div>

      {/* Repositories */}
      <div className="card">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">仓库列表</h2>

        {reposLoading ? (
          <div className="text-center py-8">
            <div className="inline-block w-6 h-6 border-4 border-primary-600 border-t-transparent rounded-full animate-spin"></div>
          </div>
        ) : reposData?.items && reposData.items.length > 0 ? (
          <div className="space-y-3">
            {reposData.items.map((repo) => (
              <Link
                key={repo.id}
                to={`/repositories/${repo.id}`}
                className="block p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center mb-2">
                      <GitBranch className="w-4 h-4 text-gray-400 mr-2" />
                      <h3 className="text-lg font-medium text-gray-900">{repo.name}</h3>
                      {repo.is_private && (
                        <span className="ml-2 px-2 py-0.5 text-xs bg-gray-100 text-gray-600 rounded">
                          私有
                        </span>
                      )}
                      {repo.drone_active && (
                        <span className="ml-2 px-2 py-0.5 text-xs bg-green-100 text-green-700 rounded">
                          CI已启用
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-gray-600 mb-2">{repo.description || '暂无描述'}</p>
                    <div className="flex items-center space-x-4 text-xs text-gray-500">
                      {repo.language && <span>{repo.language}</span>}
                      <span>⭐ {repo.star_count}</span>
                      <span>🔱 {repo.fork_count}</span>
                      <span>{(repo.size / 1024).toFixed(1)} MB</span>
                    </div>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="text-center py-8">
            <GitBranch className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-600">暂无仓库</p>
            <button
              onClick={() => setShowCreateRepo(true)}
              className="btn btn-primary mt-4"
            >
              创建第一个仓库
            </button>
          </div>
        )}
      </div>

      {/* Create Repository Modal */}
      {showCreateRepo && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold mb-4">创建仓库</h3>
            <p className="text-gray-600 mb-4">
              仓库创建功能将在后续版本中完善，包括：
            </p>
            <ul className="text-sm text-gray-600 space-y-2 mb-4">
              <li>• 仓库名称和描述</li>
              <li>• 公开/私有选项</li>
              <li>• 初始化README</li>
              <li>• .gitignore模板</li>
              <li>• 许可证选择</li>
            </ul>
            <button
              onClick={() => setShowCreateRepo(false)}
              className="btn btn-secondary w-full"
            >
              关闭
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
