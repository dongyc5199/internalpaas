import { useQuery } from '@tanstack/react-query';
import { useState, useMemo } from 'react';
import { serverApi } from '../../../shared/api/serverApi';
import { QUERY_KEYS } from '../../../shared/constants';
import { Button, Loading } from '../../../shared/components';
import type { Server } from '../../../shared/types';
import styles from './UserActivityMonitor.module.css';

/**
 * 用户活动记录
 */
interface UserActivity {
  id: number;
  userId: number;
  username: string;
  userRole: string;
  serverId: number;
  serverName: string;
  activityType: 'LOGIN' | 'LOGOUT' | 'COMMAND' | 'FILE_UPLOAD' | 'FILE_DOWNLOAD' | 'CONFIG_CHANGE' | 'APP_DEPLOY';
  description: string;
  timestamp: string;
  ipAddress: string;
  duration?: number; // 会话持续时间（秒）
  success: boolean;
}

/**
 * 用户活动统计
 */
interface UserActivityStats {
  username: string;
  userId: number;
  userRole: string;
  totalActivities: number;
  loginCount: number;
  commandCount: number;
  lastActivity: string;
  activeServers: number;
  averageSessionDuration: number;
  mostUsedServer: string;
}

/**
 * 服务器活动统计
 */
interface ServerActivityStats {
  serverId: number;
  serverName: string;
  totalActivities: number;
  activeUsers: number;
  topUser: string;
  lastActivity: string;
}

/**
 * 时间范围类型
 */
type TimeRange = '1h' | '6h' | '24h' | '7d' | '30d';

/**
 * UserActivityMonitor 组件
 *
 * 用户活动监控页面 - 展示用户在系统中的活动记录和统计
 */
export function UserActivityMonitor(): React.JSX.Element {
  const [timeRange, setTimeRange] = useState<TimeRange>('24h');
  const [selectedUser, setSelectedUser] = useState<number | null>(null);
  const [selectedServer, setSelectedServer] = useState<number | null>(null);
  const [activityTypeFilter, setActivityTypeFilter] = useState<string>('ALL');
  const [autoRefresh, setAutoRefresh] = useState(true);

  // 获取所有服务器
  const { data: serversData } = useQuery({
    queryKey: [QUERY_KEYS.SERVERS],
    queryFn: () => serverApi.getServers({ page: 0, size: 100 }),
  });

  const servers = (serversData?.content ?? []) as Server[];

  // 获取用户活动记录（模拟数据）
  const { data: activities, isLoading: activitiesLoading } = useQuery({
    queryKey: [QUERY_KEYS.MONITORING, 'user-activities', timeRange, selectedUser, selectedServer, activityTypeFilter],
    queryFn: async (): Promise<UserActivity[]> => {
      // 模拟用户活动数据
      const activityTypes: UserActivity['activityType'][] = [
        'LOGIN',
        'LOGOUT',
        'COMMAND',
        'FILE_UPLOAD',
        'FILE_DOWNLOAD',
        'CONFIG_CHANGE',
        'APP_DEPLOY',
      ];

      const usernames = ['admin', 'developer1', 'developer2', 'ops_user', 'tester'];
      const roles = ['SUPER_ADMIN', 'ADMIN', 'DEVELOPER', 'DEVELOPER', 'DEVELOPER'];

      const now = Date.now();
      const activities: UserActivity[] = [];

      // 根据时间范围生成活动记录
      let count: number;
      let timeWindow: number;

      switch (timeRange) {
        case '1h':
          count = 20;
          timeWindow = 60 * 60 * 1000;
          break;
        case '6h':
          count = 50;
          timeWindow = 6 * 60 * 60 * 1000;
          break;
        case '24h':
          count = 100;
          timeWindow = 24 * 60 * 60 * 1000;
          break;
        case '7d':
          count = 200;
          timeWindow = 7 * 24 * 60 * 60 * 1000;
          break;
        case '30d':
          count = 500;
          timeWindow = 30 * 24 * 60 * 60 * 1000;
          break;
      }

      for (let i = 0; i < count; i++) {
        const userIndex = Math.floor(Math.random() * usernames.length);
        const server = servers[Math.floor(Math.random() * servers.length)];
        if (!server) continue;

        const activityType = activityTypes[Math.floor(Math.random() * activityTypes.length)] ?? 'COMMAND';
        const timestamp = new Date(now - Math.random() * timeWindow);

        // 应用筛选
        if (selectedUser !== null && userIndex !== selectedUser) continue;
        if (selectedServer !== null && server.id !== selectedServer) continue;
        if (activityTypeFilter !== 'ALL' && activityType !== activityTypeFilter) continue;

        const descriptions: Record<UserActivity['activityType'], string[]> = {
          LOGIN: ['SSH登录成功', 'Web界面登录', '终端连接建立'],
          LOGOUT: ['用户登出', 'SSH会话关闭', '连接超时断开'],
          COMMAND: [
            '执行系统命令: ls -la',
            '查看进程: ps aux',
            '编辑配置文件',
            '重启服务',
            '查看日志文件',
          ],
          FILE_UPLOAD: ['上传应用包 app.jar', '上传配置文件', '上传脚本文件'],
          FILE_DOWNLOAD: ['下载日志文件', '导出配置', '下载备份文件'],
          CONFIG_CHANGE: ['修改应用配置', '更新环境变量', '调整JVM参数'],
          APP_DEPLOY: ['部署新版本应用', '重启应用服务', '回滚应用版本'],
        };

        activities.push({
          id: i + 1,
          userId: userIndex,
          username: usernames[userIndex] ?? 'unknown',
          userRole: roles[userIndex] ?? 'DEVELOPER',
          serverId: server.id,
          serverName: server.name,
          activityType,
          description: descriptions[activityType]?.[Math.floor(Math.random() * 3)] ?? '未知操作',
          timestamp: timestamp.toISOString(),
          ipAddress: `192.168.1.${Math.floor(Math.random() * 255)}`,
          duration: activityType === 'LOGIN' ? Math.floor(Math.random() * 3600) : undefined,
          success: Math.random() > 0.1, // 90% 成功率
        });
      }

      // 按时间倒序排序
      return activities.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
    },
    refetchInterval: autoRefresh ? 30000 : false,
  });

  // 用户活动统计
  const userStats = useMemo((): UserActivityStats[] => {
    if (!activities) return [];

    const userMap = new Map<number, UserActivityStats>();

    activities.forEach((activity) => {
      if (!userMap.has(activity.userId)) {
        userMap.set(activity.userId, {
          username: activity.username,
          userId: activity.userId,
          userRole: activity.userRole,
          totalActivities: 0,
          loginCount: 0,
          commandCount: 0,
          lastActivity: activity.timestamp,
          activeServers: 0,
          averageSessionDuration: 0,
          mostUsedServer: '',
        });
      }

      const stats = userMap.get(activity.userId);
      if (!stats) return;

      stats.totalActivities++;
      if (activity.activityType === 'LOGIN') stats.loginCount++;
      if (activity.activityType === 'COMMAND') stats.commandCount++;

      // 更新最后活动时间
      if (new Date(activity.timestamp) > new Date(stats.lastActivity)) {
        stats.lastActivity = activity.timestamp;
      }
    });

    // 计算活跃服务器数和最常用服务器
    userMap.forEach((stats, userId) => {
      const userActivities = activities.filter((a) => a.userId === userId);
      const serverCounts = new Map<string, number>();

      userActivities.forEach((a) => {
        serverCounts.set(a.serverName, (serverCounts.get(a.serverName) ?? 0) + 1);
      });

      stats.activeServers = serverCounts.size;

      // 找出最常用的服务器
      let maxCount = 0;
      let mostUsed = '';
      serverCounts.forEach((count, serverName) => {
        if (count > maxCount) {
          maxCount = count;
          mostUsed = serverName;
        }
      });
      stats.mostUsedServer = mostUsed;

      // 计算平均会话时长
      const sessions = userActivities.filter((a) => a.duration !== undefined);
      if (sessions.length > 0) {
        const totalDuration = sessions.reduce((sum, a) => sum + (a.duration ?? 0), 0);
        stats.averageSessionDuration = totalDuration / sessions.length;
      }
    });

    return Array.from(userMap.values()).sort((a, b) => b.totalActivities - a.totalActivities);
  }, [activities]);

  // 服务器活动统计
  const serverStats = useMemo((): ServerActivityStats[] => {
    if (!activities) return [];

    const serverMap = new Map<number, ServerActivityStats>();

    activities.forEach((activity) => {
      if (!serverMap.has(activity.serverId)) {
        serverMap.set(activity.serverId, {
          serverId: activity.serverId,
          serverName: activity.serverName,
          totalActivities: 0,
          activeUsers: 0,
          topUser: '',
          lastActivity: activity.timestamp,
        });
      }

      const stats = serverMap.get(activity.serverId);
      if (!stats) return;

      stats.totalActivities++;

      if (new Date(activity.timestamp) > new Date(stats.lastActivity)) {
        stats.lastActivity = activity.timestamp;
      }
    });

    // 计算活跃用户数和最活跃用户
    serverMap.forEach((stats, serverId) => {
      const serverActivities = activities.filter((a) => a.serverId === serverId);
      const userCounts = new Map<string, number>();

      serverActivities.forEach((a) => {
        userCounts.set(a.username, (userCounts.get(a.username) ?? 0) + 1);
      });

      stats.activeUsers = userCounts.size;

      // 找出最活跃的用户
      let maxCount = 0;
      let topUser = '';
      userCounts.forEach((count, username) => {
        if (count > maxCount) {
          maxCount = count;
          topUser = username;
        }
      });
      stats.topUser = topUser;
    });

    return Array.from(serverMap.values()).sort((a, b) => b.totalActivities - a.totalActivities);
  }, [activities]);

  // 格式化时间
  const formatTime = (timestamp: string): string => {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);

    if (minutes < 1) return '刚刚';
    if (minutes < 60) return `${minutes}分钟前`;
    if (minutes < 1440) return `${Math.floor(minutes / 60)}小时前`;
    return date.toLocaleString('zh-CN');
  };

  // 格式化时长
  const formatDuration = (seconds: number): string => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0) return `${hours}小时${minutes}分钟`;
    if (minutes > 0) return `${minutes}分钟`;
    return `${seconds}秒`;
  };

  // 获取活动类型图标
  const getActivityIcon = (type: string): string => {
    switch (type) {
      case 'LOGIN':
        return 'log-in';
      case 'LOGOUT':
        return 'log-out';
      case 'COMMAND':
        return 'terminal';
      case 'FILE_UPLOAD':
        return 'upload';
      case 'FILE_DOWNLOAD':
        return 'download';
      case 'CONFIG_CHANGE':
        return 'settings';
      case 'APP_DEPLOY':
        return 'package';
      default:
        return 'activity';
    }
  };

  // 获取活动类型颜色类
  const getActivityTypeClass = (type: string): string => {
    switch (type) {
      case 'LOGIN':
        return styles.typeLogin ?? '';
      case 'LOGOUT':
        return styles.typeLogout ?? '';
      case 'COMMAND':
        return styles.typeCommand ?? '';
      case 'FILE_UPLOAD':
        return styles.typeFileUpload ?? '';
      case 'FILE_DOWNLOAD':
        return styles.typeFileDownload ?? '';
      case 'CONFIG_CHANGE':
        return styles.typeConfigChange ?? '';
      case 'APP_DEPLOY':
        return styles.typeAppDeploy ?? '';
      default:
        return '';
    }
  };

  const isLoading = activitiesLoading;

  return (
    <div className={styles.container}>
      {/* 页面头部 */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleGroup}>
            <h1 className={styles.title}>用户活动监控</h1>
            <p className={styles.subtitle}>实时追踪用户操作和系统活动</p>
          </div>
          <div className={styles.headerActions}>
            <label className={styles.autoRefreshToggle}>
              <input
                type="checkbox"
                checked={autoRefresh}
                onChange={(e) => setAutoRefresh(e.target.checked)}
                className={styles.checkbox}
              />
              <span>自动刷新 (30s)</span>
            </label>
          </div>
        </div>
      </header>

      {/* 控制栏 */}
      <div className={styles.controls}>
        <div className={styles.controlGroup}>
          <label className={styles.controlLabel}>时间范围</label>
          <div className={styles.buttonGroup}>
            <Button size="sm" variant={timeRange === '1h' ? 'primary' : 'secondary'} onClick={() => setTimeRange('1h')}>
              1小时
            </Button>
            <Button size="sm" variant={timeRange === '6h' ? 'primary' : 'secondary'} onClick={() => setTimeRange('6h')}>
              6小时
            </Button>
            <Button
              size="sm"
              variant={timeRange === '24h' ? 'primary' : 'secondary'}
              onClick={() => setTimeRange('24h')}
            >
              24小时
            </Button>
            <Button size="sm" variant={timeRange === '7d' ? 'primary' : 'secondary'} onClick={() => setTimeRange('7d')}>
              7天
            </Button>
            <Button
              size="sm"
              variant={timeRange === '30d' ? 'primary' : 'secondary'}
              onClick={() => setTimeRange('30d')}
            >
              30天
            </Button>
          </div>
        </div>

        <div className={styles.controlGroup}>
          <label className={styles.controlLabel}>活动类型</label>
          <div className={styles.buttonGroup}>
            <Button
              size="sm"
              variant={activityTypeFilter === 'ALL' ? 'primary' : 'secondary'}
              onClick={() => setActivityTypeFilter('ALL')}
            >
              全部
            </Button>
            <Button
              size="sm"
              variant={activityTypeFilter === 'LOGIN' ? 'primary' : 'secondary'}
              onClick={() => setActivityTypeFilter('LOGIN')}
            >
              <i data-lucide="log-in" />
              登录
            </Button>
            <Button
              size="sm"
              variant={activityTypeFilter === 'COMMAND' ? 'primary' : 'secondary'}
              onClick={() => setActivityTypeFilter('COMMAND')}
            >
              <i data-lucide="terminal" />
              命令
            </Button>
            <Button
              size="sm"
              variant={activityTypeFilter === 'FILE_UPLOAD' ? 'primary' : 'secondary'}
              onClick={() => setActivityTypeFilter('FILE_UPLOAD')}
            >
              <i data-lucide="upload" />
              上传
            </Button>
            <Button
              size="sm"
              variant={activityTypeFilter === 'APP_DEPLOY' ? 'primary' : 'secondary'}
              onClick={() => setActivityTypeFilter('APP_DEPLOY')}
            >
              <i data-lucide="package" />
              部署
            </Button>
          </div>
        </div>
      </div>

      {isLoading ? (
        <Loading text="加载用户活动数据..." />
      ) : (
        <>
          {/* 统计概览 */}
          <div className={styles.statsSection}>
            <div className={styles.statsGrid}>
              <div className={styles.statCard}>
                <div className={styles.statIcon}>
                  <i data-lucide="activity" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>总活动数</div>
                  <div className={styles.statValue}>{activities?.length ?? 0}</div>
                </div>
              </div>

              <div className={styles.statCard}>
                <div className={styles.statIcon}>
                  <i data-lucide="users" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>活跃用户</div>
                  <div className={styles.statValue}>{userStats.length}</div>
                </div>
              </div>

              <div className={styles.statCard}>
                <div className={styles.statIcon}>
                  <i data-lucide="server" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>活跃服务器</div>
                  <div className={styles.statValue}>{serverStats.length}</div>
                </div>
              </div>

              <div className={styles.statCard}>
                <div className={styles.statIcon}>
                  <i data-lucide="clock" />
                </div>
                <div className={styles.statContent}>
                  <div className={styles.statLabel}>时间范围</div>
                  <div className={styles.statValue}>
                    {timeRange === '1h' && '1小时'}
                    {timeRange === '6h' && '6小时'}
                    {timeRange === '24h' && '24小时'}
                    {timeRange === '7d' && '7天'}
                    {timeRange === '30d' && '30天'}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* 主内容区 */}
          <div className={styles.mainContent}>
            {/* 左侧：用户活动统计 */}
            <div className={styles.leftPanel}>
              <div className={styles.panelHeader}>
                <h2 className={styles.panelTitle}>
                  <i data-lucide="users" />
                  用户活动统计
                </h2>
              </div>
              <div className={styles.userStatsList}>
                {userStats.map((user) => (
                  <div
                    key={user.userId}
                    className={`${styles.userStatsCard} ${selectedUser === user.userId ? styles.userStatsCardSelected : ''}`}
                    onClick={() => setSelectedUser(selectedUser === user.userId ? null : user.userId)}
                  >
                    <div className={styles.userStatsHeader}>
                      <div className={styles.userAvatar}>
                        <i data-lucide="user" />
                      </div>
                      <div className={styles.userInfo}>
                        <div className={styles.username}>{user.username}</div>
                        <div className={styles.userRole}>{user.userRole}</div>
                      </div>
                    </div>
                    <div className={styles.userStatsBody}>
                      <div className={styles.statsRow}>
                        <span>总活动:</span>
                        <strong>{user.totalActivities}</strong>
                      </div>
                      <div className={styles.statsRow}>
                        <span>登录次数:</span>
                        <strong>{user.loginCount}</strong>
                      </div>
                      <div className={styles.statsRow}>
                        <span>命令执行:</span>
                        <strong>{user.commandCount}</strong>
                      </div>
                      <div className={styles.statsRow}>
                        <span>活跃服务器:</span>
                        <strong>{user.activeServers}</strong>
                      </div>
                      <div className={styles.statsRow}>
                        <span>最常用:</span>
                        <strong>{user.mostUsedServer || '-'}</strong>
                      </div>
                      <div className={styles.statsRow}>
                        <span>最后活动:</span>
                        <strong>{formatTime(user.lastActivity)}</strong>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              <div className={styles.panelHeader} style={{ marginTop: '2rem' }}>
                <h2 className={styles.panelTitle}>
                  <i data-lucide="server" />
                  服务器活动统计
                </h2>
              </div>
              <div className={styles.serverStatsList}>
                {serverStats.map((server) => (
                  <div
                    key={server.serverId}
                    className={`${styles.serverStatsCard} ${selectedServer === server.serverId ? styles.serverStatsCardSelected : ''}`}
                    onClick={() => setSelectedServer(selectedServer === server.serverId ? null : server.serverId)}
                  >
                    <div className={styles.serverStatsHeader}>
                      <div className={styles.serverIcon}>
                        <i data-lucide="server" />
                      </div>
                      <div className={styles.serverInfo}>
                        <div className={styles.serverName}>{server.serverName}</div>
                      </div>
                    </div>
                    <div className={styles.serverStatsBody}>
                      <div className={styles.statsRow}>
                        <span>总活动:</span>
                        <strong>{server.totalActivities}</strong>
                      </div>
                      <div className={styles.statsRow}>
                        <span>活跃用户:</span>
                        <strong>{server.activeUsers}</strong>
                      </div>
                      <div className={styles.statsRow}>
                        <span>最活跃:</span>
                        <strong>{server.topUser || '-'}</strong>
                      </div>
                      <div className={styles.statsRow}>
                        <span>最后活动:</span>
                        <strong>{formatTime(server.lastActivity)}</strong>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* 右侧：活动时间线 */}
            <div className={styles.rightPanel}>
              <div className={styles.panelHeader}>
                <h2 className={styles.panelTitle}>
                  <i data-lucide="list" />
                  活动时间线
                  {(selectedUser !== null || selectedServer !== null) && (
                    <span className={styles.filterBadge}>已筛选</span>
                  )}
                </h2>
                {(selectedUser !== null || selectedServer !== null) && (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => {
                      setSelectedUser(null);
                      setSelectedServer(null);
                    }}
                  >
                    <i data-lucide="x" />
                    清除筛选
                  </Button>
                )}
              </div>

              <div className={styles.timeline}>
                {activities && activities.length > 0 ? (
                  activities.slice(0, 50).map((activity) => (
                    <div key={activity.id} className={styles.timelineItem}>
                      <div className={`${styles.activityIcon} ${getActivityTypeClass(activity.activityType)}`}>
                        <i data-lucide={getActivityIcon(activity.activityType)} />
                      </div>
                      <div className={styles.activityContent}>
                        <div className={styles.activityHeader}>
                          <span className={styles.activityUser}>
                            <i data-lucide="user" />
                            {activity.username}
                          </span>
                          <span className={styles.activityType}>{activity.activityType}</span>
                          <span className={styles.activityTime}>{formatTime(activity.timestamp)}</span>
                        </div>
                        <div className={styles.activityDescription}>{activity.description}</div>
                        <div className={styles.activityMeta}>
                          <span>
                            <i data-lucide="server" />
                            {activity.serverName}
                          </span>
                          <span>
                            <i data-lucide="globe" />
                            {activity.ipAddress}
                          </span>
                          {activity.duration !== undefined && (
                            <span>
                              <i data-lucide="clock" />
                              {formatDuration(activity.duration)}
                            </span>
                          )}
                          <span
                            className={activity.success ? styles.statusSuccess : styles.statusFailed}
                          >
                            {activity.success ? '成功' : '失败'}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className={styles.emptyState}>
                    <i data-lucide="inbox" />
                    <h3>暂无活动记录</h3>
                    <p>所选时间范围内没有找到活动记录</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
