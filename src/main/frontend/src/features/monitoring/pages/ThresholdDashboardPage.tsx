import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { alertThresholdApi } from '../../../shared/api/alertThresholdApi';
import { serverApi } from '../../../shared/api/serverApi';
import styles from './ThresholdDashboardPage.module.css';

/**
 * 告警阈值管理仪表板 - MVP版本
 *
 * 功能：
 * - 显示阈值统计信息
 * - 显示服务器列表
 * - 快速导航到服务器阈值配置和历史数据
 *
 * 注：简化版本，聚焦核心功能
 */
export function ThresholdDashboardPage(): React.JSX.Element {
  const navigate = useNavigate();

  // 获取服务器列表
  const { data: serversResponse, isLoading: serversLoading } = useQuery({
    queryKey: ['servers'],
    queryFn: () => serverApi.getServers({ size: 100 }),
  });

  const servers = serversResponse?.content || [];

  // 获取阈值统计信息
  const { data: statistics, isLoading: statsLoading } = useQuery({
    queryKey: ['thresholdStatistics'],
    queryFn: () => alertThresholdApi.getStatistics(),
  });

  const isLoading = serversLoading || statsLoading;

  /**
   * 获取服务器状态的样式类
   */
  const getServerStatusClass = (status?: string): string => {
    if (!status) {
      return styles.statusUnknown || '';
    }

    const statusUpper = status.toUpperCase();
    switch (statusUpper) {
      case 'ONLINE':
      case 'CONNECTED':
        return styles.statusConnected || '';
      case 'MONITORING':
        return styles.statusMonitoring || '';
      case 'OFFLINE':
      case 'FAILED':
      case 'ERROR':
        return styles.statusFailed || '';
      default:
        return styles.statusUnknown || '';
    }
  };

  /**
   * 获取服务器状态的显示文本
   */
  const getServerStatusText = (status?: string): string => {
    if (!status) {
      return '未知';
    }

    const statusMap: Record<string, string> = {
      'ONLINE': '在线',
      'OFFLINE': '离线',
      'CONNECTED': '已连接',
      'MONITORING': '监控中',
      'MAINTENANCE': '维护中',
      'FAILED': '连接失败',
      'ERROR': '错误',
      'DISCONNECTED': '未连接',
      'UNKNOWN': '未知',
    };

    return statusMap[status.toUpperCase()] || status;
  };

  /**
   * 导航到服务器阈值配置页面
   */
  const handleConfigureThresholds = (serverId: number): void => {
    navigate(`/monitoring/servers/${serverId}/thresholds`);
  };

  /**
   * 导航到历史数据页面
   */
  const handleViewHistory = (serverId: number): void => {
    navigate(`/monitoring/history/dashboard?server=${serverId}`);
  };

  return (
    <div className={styles.container}>
      {/* 页面头部 */}
      <div className={styles.header}>
        <h1>
          <i className="fas fa-bell"></i> 告警阈值管理
        </h1>
      </div>

      {/* 统计信息卡片 */}
      {isLoading ? (
        <div className={styles.loading}>
          <i className="fas fa-spinner fa-spin"></i> 加载中...
        </div>
      ) : statistics ? (
        <div className={styles.statsGrid}>
          <div className={styles.statCard}>
            <div className={styles.statBody}>
              <h5>{statistics.totalThresholds || 0}</h5>
              <small>总阈值配置</small>
            </div>
          </div>
          <div className={styles.statCard}>
            <div className={styles.statBody}>
              <h5>{statistics.enabledThresholds || 0}</h5>
              <small>已启用</small>
            </div>
          </div>
          <div className={styles.statCard}>
            <div className={styles.statBody}>
              <h5>{statistics.notificationEnabledThresholds || 0}</h5>
              <small>通知已启用</small>
            </div>
          </div>
          <div className={styles.statCard}>
            <div className={styles.statBody}>
              <h5>{servers.length}</h5>
              <small>管理的服务器</small>
            </div>
          </div>
        </div>
      ) : null}

      {/* 服务器列表 */}
      <div className={styles.serversSection}>
        <div className={styles.sectionHeader}>
          <h5>
            <i className="fas fa-server"></i> 服务器阈值配置
          </h5>
        </div>
        <div className={styles.sectionBody}>
          {servers.length === 0 ? (
            <div className={styles.emptyState}>
              <i className="fas fa-server fa-3x"></i>
              <p>暂无服务器数据</p>
            </div>
          ) : (
            <div className={styles.serverGrid}>
              {servers.map((server) => (
                <div key={server.id} className={styles.serverCard}>
                  <div className={styles.serverCardHeader}>
                    <h6>{server.name}</h6>
                    <span
                      className={`${styles.statusBadge} ${getServerStatusClass(
                        server.status
                      )}`}
                    >
                      {getServerStatusText(server.status)}
                    </span>
                  </div>
                  <p className={styles.serverHostname}>
                    <i className="fas fa-network-wired"></i> {server.host}:{server.port}
                  </p>
                  <div className={styles.serverCardActions}>
                    <button
                      className={styles.btnPrimary}
                      onClick={() => handleConfigureThresholds(server.id)}
                    >
                      <i className="fas fa-cog"></i> 配置阈值
                    </button>
                    <button
                      className={styles.btnSecondary}
                      onClick={() => handleViewHistory(server.id)}
                    >
                      <i className="fas fa-chart-line"></i> 历史数据
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* 功能说明 */}
      <div className={styles.featuresSection}>
        <div className={styles.sectionHeader}>
          <h5>
            <i className="fas fa-info-circle"></i> 功能说明
          </h5>
        </div>
        <div className={styles.sectionBody}>
          <div className={styles.featuresGrid}>
            <div className={styles.featureItem}>
              <h6>
                <i className="fas fa-bell"></i> 告警阈值
              </h6>
              <p>为每个服务器的各项监控指标设置警告和严重阈值，超过阈值时系统将自动触发告警。</p>
            </div>
            <div className={styles.featureItem}>
              <h6>
                <i className="fas fa-envelope"></i> 通知设置
              </h6>
              <p>配置告警通知方式和频率，避免重复通知，确保及时响应重要告警。</p>
            </div>
            <div className={styles.featureItem}>
              <h6>
                <i className="fas fa-copy"></i> 批量管理
              </h6>
              <p>支持复制阈值配置到多个服务器，快速完成批量配置，提高管理效率。</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ThresholdDashboardPage;
