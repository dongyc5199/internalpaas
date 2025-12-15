import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { buildService } from '@/services/build';
import { useWebSocket } from '@/hooks/useWebSocket';
import {
  ArrowLeft,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Activity,
  Download,
  RotateCcw,
  StopCircle,
} from 'lucide-react';
import { format } from 'date-fns';
import { BuildStatus, WebSocketMessage } from '@/types';
import { useState, useEffect, useRef } from 'react';

const statusConfig: Record<BuildStatus, { icon: any; color: string; text: string }> = {
  pending: { icon: Clock, color: 'text-gray-500', text: '等待中' },
  running: { icon: Activity, color: 'text-blue-500 animate-pulse', text: '运行中' },
  success: { icon: CheckCircle, color: 'text-green-500', text: '成功' },
  failure: { icon: XCircle, color: 'text-red-500', text: '失败' },
  killed: { icon: AlertCircle, color: 'text-orange-500', text: '已取消' },
  error: { icon: XCircle, color: 'text-red-500', text: '错误' },
};

export default function BuildDetailPage() {
  const { id } = useParams<{ id: string }>();
  const buildId = parseInt(id || '0');
  const [logs, setLogs] = useState<string[]>([]);
  const logsEndRef = useRef<HTMLDivElement>(null);
  const [autoScroll, setAutoScroll] = useState(true);

  // 获取构建详情
  const { data: build, isLoading, refetch } = useQuery({
    queryKey: ['build', buildId],
    queryFn: () => buildService.get(buildId),
    enabled: !!buildId,
    refetchInterval: (query) => {
      // 如果构建正在运行，每5秒刷新一次
      return query.state.data?.status === 'running' ? 5000 : false;
    },
  });

  // WebSocket连接用于实时日志
  const { isConnected } = useWebSocket({
    buildId,
    onMessage: (message: WebSocketMessage) => {
      if (message.type === 'build_log' && message.data.line) {
        setLogs((prev) => [...prev, message.data.line as string]);
      } else if (message.type === 'build_status') {
        // 构建状态更新，刷新构建详情
        refetch();
      }
    },
  });

  // 获取历史日志
  useEffect(() => {
    if (buildId) {
      buildService.getLogs(buildId).then((logText) => {
        if (logText) {
          setLogs(logText.split('\n'));
        }
      }).catch((error) => {
        console.error('Failed to load logs:', error);
      });
    }
  }, [buildId]);

  // 自动滚动到底部
  useEffect(() => {
    if (autoScroll && logsEndRef.current) {
      logsEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [logs, autoScroll]);

  if (isLoading) {
    return (
      <div className="text-center py-12">
        <div className="inline-block w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full animate-spin"></div>
        <p className="text-gray-600 mt-4">加载中...</p>
      </div>
    );
  }

  if (!build) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-600">构建记录不存在</p>
      </div>
    );
  }

  const StatusIcon = statusConfig[build.status].icon;
  const duration = build.duration
    ? `${Math.floor(build.duration / 60)}分${build.duration % 60}秒`
    : '-';

  return (
    <div>
      {/* Header */}
      <div className="mb-6">
        <Link
          to={`/repositories/${build.repository_id}`}
          className="inline-flex items-center text-gray-600 hover:text-gray-900 mb-4"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          返回仓库
        </Link>

        <div className="flex items-start justify-between">
          <div className="flex items-center">
            <StatusIcon className={`w-8 h-8 mr-3 ${statusConfig[build.status].color}`} />
            <div>
              <h1 className="text-3xl font-bold text-gray-900">
                构建 #{build.drone_build_number || build.id}
              </h1>
              <p className="text-gray-600 mt-1">{build.commit_message || build.commit}</p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            {build.status === 'running' && (
              <button className="btn btn-danger flex items-center" title="取消构建">
                <StopCircle className="w-4 h-4 mr-2" />
                取消
              </button>
            )}
            {(build.status === 'success' || build.status === 'failure') && (
              <button className="btn btn-secondary flex items-center" title="重新构建">
                <RotateCcw className="w-4 h-4 mr-2" />
                重新构建
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Build Info Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
        <div className="card">
          <div className="text-sm text-gray-600 mb-1">状态</div>
          <div className="flex items-center">
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

        <div className="card">
          <div className="text-sm text-gray-600 mb-1">分支</div>
          <div className="text-lg font-semibold text-gray-900">{build.branch}</div>
        </div>

        <div className="card">
          <div className="text-sm text-gray-600 mb-1">触发方式</div>
          <div className="text-lg font-semibold text-gray-900">{build.trigger}</div>
        </div>

        <div className="card">
          <div className="text-sm text-gray-600 mb-1">耗时</div>
          <div className="text-lg font-semibold text-gray-900">{duration}</div>
        </div>
      </div>

      {/* Build Details */}
      <div className="card mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">构建信息</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div>
            <span className="text-gray-600">提交:</span>
            <span className="ml-2 font-mono text-gray-900">{build.commit?.substring(0, 7)}</span>
          </div>
          {build.author_name && (
            <div>
              <span className="text-gray-600">作者:</span>
              <span className="ml-2 text-gray-900">{build.author_name}</span>
            </div>
          )}
          {build.started_at && (
            <div>
              <span className="text-gray-600">开始时间:</span>
              <span className="ml-2 text-gray-900">
                {format(new Date(build.started_at), 'yyyy-MM-dd HH:mm:ss')}
              </span>
            </div>
          )}
          {build.finished_at && (
            <div>
              <span className="text-gray-600">结束时间:</span>
              <span className="ml-2 text-gray-900">
                {format(new Date(build.finished_at), 'yyyy-MM-dd HH:mm:ss')}
              </span>
            </div>
          )}
          {build.drone_link && (
            <div className="col-span-2">
              <span className="text-gray-600">Drone链接:</span>
              <a
                href={build.drone_link}
                target="_blank"
                rel="noopener noreferrer"
                className="ml-2 text-primary-600 hover:text-primary-700"
              >
                {build.drone_link}
              </a>
            </div>
          )}
        </div>
      </div>

      {/* Build Logs */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center">
            <h2 className="text-lg font-semibold text-gray-900">构建日志</h2>
            {isConnected && build.status === 'running' && (
              <span className="ml-3 flex items-center text-sm text-green-600">
                <span className="w-2 h-2 bg-green-500 rounded-full mr-2 animate-pulse"></span>
                实时连接
              </span>
            )}
          </div>
          <div className="flex items-center space-x-2">
            <label className="flex items-center text-sm text-gray-600">
              <input
                type="checkbox"
                checked={autoScroll}
                onChange={(e) => setAutoScroll(e.target.checked)}
                className="mr-2"
              />
              自动滚动
            </label>
            <button className="btn btn-secondary btn-sm flex items-center" title="下载日志">
              <Download className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="bg-gray-900 rounded-lg p-4 font-mono text-sm text-gray-100 h-[600px] overflow-y-auto">
          {logs.length > 0 ? (
            <>
              {logs.map((line, index) => (
                <div key={index} className="hover:bg-gray-800">
                  <span className="text-gray-500 select-none mr-4">{index + 1}</span>
                  <span className="whitespace-pre-wrap">{line}</span>
                </div>
              ))}
              <div ref={logsEndRef} />
            </>
          ) : (
            <div className="text-center text-gray-400 py-12">
              {build.status === 'pending' ? '等待构建开始...' : '暂无日志'}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
