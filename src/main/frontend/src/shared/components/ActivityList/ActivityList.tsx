import type { RecentActivity } from '../../types/dashboard';
import styles from './ActivityList.module.css';

/**
 * ActivityList 组件属性
 */
export interface ActivityListProps {
  /** 活动列表 */
  activities: RecentActivity[];
  /** 是否加载中 */
  loading?: boolean;
  /** 最大显示数量 */
  maxItems?: number;
}

/**
 * 获取活动类型的显示信息
 */
function getActivityTypeInfo(type: RecentActivity['type']): {
  icon: string;
  label: string;
  color: string;
} {
  const typeMap = {
    deployment: { icon: 'upload', label: '部署', color: '#3b82f6' },
    server_added: { icon: 'server', label: '服务器', color: '#10b981' },
    user_login: { icon: 'log-in', label: '登录', color: '#6b7280' },
    application_started: { icon: 'play', label: '启动', color: '#10b981' },
    application_stopped: { icon: 'square', label: '停止', color: '#ef4444' },
  };

  return typeMap[type] || { icon: 'activity', label: '活动', color: '#6b7280' };
}

/**
 * 获取状态样式类名
 */
function getStatusClassName(status: RecentActivity['status']): string {
  const statusMap = {
    success: styles.statusSuccess,
    warning: styles.statusWarning,
    error: styles.statusError,
  };

  return statusMap[status] || '';
}

/**
 * 格式化时间显示
 */
function formatTimeAgo(timestamp: string): string {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;

  return date.toLocaleDateString('zh-CN');
}

/**
 * ActivityList 组件
 *
 * 最近活动列表组件
 *
 * 功能:
 * - 显示最近活动列表
 * - 活动类型图标和颜色
 * - 时间相对显示
 * - 状态指示器
 * - 加载骨架屏
 */
export function ActivityList({
  activities,
  loading = false,
  maxItems = 10,
}: ActivityListProps): React.JSX.Element {
  const displayActivities = activities.slice(0, maxItems);

  if (loading) {
    return (
      <div className={styles.container}>
        <div className={styles.header}>
          <h3 className={styles.title}>最近活动</h3>
        </div>
        <div className={styles.list}>
          {Array.from({ length: 5 }).map((_, index) => (
            <div key={index} className={styles.skeletonItem}>
              <div className={styles.skeletonIcon} />
              <div className={styles.skeletonContent}>
                <div className={styles.skeletonText} />
                <div className={styles.skeletonTime} />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (displayActivities.length === 0) {
    return (
      <div className={styles.container}>
        <div className={styles.header}>
          <h3 className={styles.title}>最近活动</h3>
        </div>
        <div className={styles.empty}>
          <i data-lucide="inbox" className={styles.emptyIcon} />
          <p className={styles.emptyText}>暂无活动记录</p>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h3 className={styles.title}>最近活动</h3>
        <span className={styles.count}>{activities.length} 条</span>
      </div>
      <div className={styles.list}>
        {displayActivities.map((activity) => {
          const typeInfo = getActivityTypeInfo(activity.type);
          const statusClassName = getStatusClassName(activity.status);

          return (
            <div key={activity.id} className={styles.item}>
              <div className={styles.iconWrapper} style={{ backgroundColor: `${typeInfo.color}15` }}>
                <i
                  data-lucide={typeInfo.icon}
                  className={styles.icon}
                  style={{ color: typeInfo.color }}
                />
              </div>
              <div className={styles.content}>
                <div className={styles.description}>
                  {activity.username && (
                    <span className={styles.username}>{activity.username}</span>
                  )}
                  {activity.description}
                </div>
                <div className={styles.meta}>
                  <span className={styles.time}>{formatTimeAgo(activity.timestamp)}</span>
                  <span className={`${styles.status} ${statusClassName}`}>
                    {activity.status === 'success'
                      ? '成功'
                      : activity.status === 'warning'
                        ? '警告'
                        : '失败'}
                  </span>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
