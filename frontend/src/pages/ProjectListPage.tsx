import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { projectService } from '@/services/project';
import { Plus, FolderGit2 } from 'lucide-react';

export default function ProjectListPage() {
  const [page, setPage] = useState(1);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // 获取项目列表
  const { data, isLoading } = useQuery({
    queryKey: ['projects', page],
    queryFn: () => projectService.list(page, 20),
  });

  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">项目列表</h1>
          <p className="text-gray-600 mt-1">管理您的所有项目</p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="btn btn-primary flex items-center"
        >
          <Plus className="w-4 h-4 mr-2" />
          创建项目
        </button>
      </div>

      {/* Projects grid */}
      {isLoading ? (
        <div className="text-center py-12">
          <div className="inline-block w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-gray-600 mt-4">加载中...</p>
        </div>
      ) : data?.items && data.items.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {data.items.map((project) => (
            <Link
              key={project.id}
              to={`/projects/${project.id}`}
              className="card hover:shadow-md transition-shadow cursor-pointer"
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center mb-2">
                    <FolderGit2 className="w-5 h-5 text-primary-600 mr-2" />
                    <h3 className="text-lg font-semibold text-gray-900">
                      {project.display_name}
                    </h3>
                  </div>
                  <p className="text-sm text-gray-600 mb-4">{project.description || '暂无描述'}</p>
                  <div className="flex items-center space-x-4 text-sm text-gray-500">
                    <span>{project.repository_count} 个仓库</span>
                    <span>{project.member_count} 个成员</span>
                    <span>{project.build_count} 次构建</span>
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      ) : (
        <div className="text-center py-12">
          <FolderGit2 className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-600">暂无项目，点击上方按钮创建第一个项目</p>
        </div>
      )}

      {/* Pagination */}
      {data && data.total > 20 && (
        <div className="flex items-center justify-center mt-8 space-x-2">
          <button
            onClick={() => setPage(p => Math.max(1, p - 1))}
            disabled={page === 1}
            className="btn btn-secondary disabled:opacity-50"
          >
            上一页
          </button>
          <span className="text-gray-600">
            第 {page} 页，共 {Math.ceil(data.total / 20)} 页
          </span>
          <button
            onClick={() => setPage(p => p + 1)}
            disabled={page >= Math.ceil(data.total / 20)}
            className="btn btn-secondary disabled:opacity-50"
          >
            下一页
          </button>
        </div>
      )}

      {/* Create Modal - placeholder */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full">
            <h3 className="text-lg font-semibold mb-4">创建项目</h3>
            <p className="text-gray-600">功能开发中...</p>
            <button
              onClick={() => setShowCreateModal(false)}
              className="btn btn-secondary mt-4 w-full"
            >
              关闭
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
