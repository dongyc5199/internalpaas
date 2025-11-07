import { Skeleton } from '../../../shared/components';
import styles from '../pages/UserDetailPage.module.css';

/**
 * UserDetailSkeleton 组件
 *
 * 用户详情页加载骨架屏
 */
export function UserDetailSkeleton(): React.JSX.Element {
  return (
    <div className={styles.page}>
      {/* 面包屑骨架 */}
      <div style={{ marginBottom: '1.5rem' }}>
        <Skeleton width={300} height={20} />
      </div>

      {/* 页面头部骨架 */}
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <Skeleton width={80} height={36} borderRadius={8} />
          <div style={{ marginLeft: '1rem' }}>
            <Skeleton width={200} height={32} style={{ marginBottom: '0.5rem' }} />
            <Skeleton width={180} height={20} />
          </div>
        </div>
        <div className={styles.headerRight} style={{ gap: '0.75rem' }}>
          <Skeleton width={80} height={40} borderRadius={8} />
          <Skeleton width={80} height={40} borderRadius={8} />
        </div>
      </div>

      {/* 基本信息骨架 */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <Skeleton width={100} height={24} />
        </div>
        <div className={styles.infoGrid}>
          {Array.from({ length: 6 }).map((_, index) => (
            <div key={index} className={styles.infoItem}>
              <Skeleton width={80} height={14} style={{ marginBottom: '0.5rem' }} />
              <Skeleton width="100%" height={16} />
            </div>
          ))}
        </div>
      </div>

      {/* 权限管理骨架 */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <Skeleton width={100} height={24} />
        </div>
        <div className={styles.permissionsPanel}>
          {Array.from({ length: 2 }).map((_, index) => (
            <div key={index} className={styles.permissionItem}>
              <div>
                <Skeleton width={120} height={18} style={{ marginBottom: '0.5rem' }} />
                <Skeleton width={200} height={14} />
              </div>
              <Skeleton width={80} height={36} borderRadius={8} />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
