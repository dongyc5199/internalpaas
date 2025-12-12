import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { monitoringHistoryApi } from '../../../shared/api/monitoringHistoryApi';
import { serverApi } from '../../../shared/api/serverApi';
import styles from './MonitoringHistoryPage.module.css';

type TimeRange = '1h' | '6h' | '24h' | '7d' | '30d';

/**
 * 监控历史页面 - 简化MVP版本
 *
 * 功能：
 * - 服务器选择
 * - 快速时间范围选择
 * - 基础数据展示（表格形式）
 * - 统计信息
 *
 * 注：完整版本应包含图表（recharts），异常事件等
 */
export function MonitoringHistoryPage(): React.JSX.Element {
  const [selectedServerId, setSelectedServerId] = useState<number | null>(null);
  const [timeRange, setTimeRange] = useState<TimeRange>('24h');

  // 获取服务器列表
  const { data: serversResponse } = useQuery({
    queryKey: ['servers'],
    queryFn: () => serverApi.getServers({ size: 100 }),
  });

  const servers = serversResponse?.content || [];

  // 自动选择第一个��务器
  useEffect(() => {
    if (servers.length > 0 && !selectedServerId && servers[0]) {
      setSelectedServerId(servers[0].id);
    }
  }, [servers, selectedServerId]);

  // 获取历史数据
  const { data: historyData, isLoading } = useQuery({
    queryKey: ['monitoringHistory', selectedServerId, timeRange],
    queryFn: () => {
      if (!selectedServerId) return null;
      return monitoringHistoryApi.getQuickRangeData(selectedServerId, timeRange);
    },
    enabled: !!selectedServerId,
  });

  // 获取统计信息
  const { data: statistics } = useQuery({
    queryKey: ['monitoringStatistics', selectedServerId, timeRange],
    queryFn: async () => {
      if (!selectedServerId) return null;
      const now = new Date();
      const ranges: Record<TimeRange, number> = {
        '1h': 1,
        '6h': 6,
        '24h': 24,
        '7d': 168,
        '30d': 720,
      };
      const hours = ranges[timeRange];
      const startTime = new Date(now.getTime() - hours * 60 * 60 * 1000).toISOString();
      const endTime = now.toISOString();
      return await monitoringHistoryApi.getStatistics(
        selectedServerId,
        startTime,
        endTime
      );
    },
    enabled: !!selectedServerId,
  });

  const handleExport = async (): Promise<void> => {
    if (!selectedServerId) return;

    try {
      const now = new Date();
      const ranges: Record<TimeRange, number> = {
        '1h': 1,
        '6h': 6,
        '24h': 24,
        '7d': 168,
        '30d': 720,
      };
      const hours = ranges[timeRange];
      const startTime = new Date(now.getTime() - hours * 60 * 60 * 1000).toISOString();
      const endTime = now.toISOString();

      const blob = await monitoringHistoryApi.exportData(
        selectedServerId,
        startTime,
        endTime
      );

      // 创建下载链接
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `monitoring-history-${selectedServerId}-${timeRange}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Export failed:', error);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1>
          <i className="fas fa-history"></i> 历史监控数据分析
        </h1>
        <div className={styles.headerActions}>
          <button className={styles.btnOutline} onClick={handleExport}>
            <i className="fas fa-download"></i> 导出数据
          </button>
        </div>
      </div>

      {/* 控制面板 */}
      <div className={styles.controlPanel}>
        <div className={styles.panelHeader}>
          <h3>
            <i className="fas fa-cogs"></i> 监控控制面板
          </h3>
        </div>
        <div className={styles.panelBody}>
          {/* 服务器选择 */}
          <div className={styles.formGroup}>
            <label>选择服务器</label>
            <select
              className={styles.select}
              value={selectedServerId || ''}
              onChange={(e) => setSelectedServerId(Number(e.target.value))}
            >
              <option value="">请选择服务器</option>
              {servers?.map((server) => (
                <option key={server.id} value={server.id}>
                  {server.name}
                </option>
              ))}
            </select>
          </div>

          {/* 时间范围选择 */}
          <div className={styles.formGroup}>
            <label>时间范围</label>
            <div className={styles.timeRangeButtons}>
              {(['1h', '6h', '24h', '7d', '30d'] as TimeRange[]).map((range) => (
                <button
                  key={range}
                  className={`${styles.timeRangeBtn} ${
                    timeRange === range ? styles.active : ''
                  }`}
                  onClick={() => setTimeRange(range)}
                >
                  {range === '1h' && '最近1小时'}
                  {range === '6h' && '最近6小时'}
                  {range === '24h' && '最近24小时'}
                  {range === '7d' && '最近7天'}
                  {range === '30d' && '最近30天'}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* 统计信息卡片 */}
      {statistics && (
        <div className={styles.statsGrid}>
          <div className={styles.statCard}>
            <div className={styles.statIcon} style={{ background: '#3b82f6' }}>
              <i className="fas fa-microchip"></i>
            </div>
            <div className={styles.statContent}>
              <div className={styles.statLabel}>CPU使用率</div>
              <div className={styles.statValue}>{statistics.avgCpu.toFixed(1)}%</div>
              <div className={styles.statDetail}>
                最大: {statistics.maxCpu.toFixed(1)}% | 最小: {statistics.minCpu.toFixed(1)}%
              </div>
            </div>
          </div>

          <div className={styles.statCard}>
            <div className={styles.statIcon} style={{ background: '#10b981' }}>
              <i className="fas fa-memory"></i>
            </div>
            <div className={styles.statContent}>
              <div className={styles.statLabel}>内存使用率</div>
              <div className={styles.statValue}>{statistics.avgMemory.toFixed(1)}%</div>
              <div className={styles.statDetail}>
                最大: {statistics.maxMemory.toFixed(1)}% | 最小: {statistics.minMemory.toFixed(1)}%
              </div>
            </div>
          </div>

          <div className={styles.statCard}>
            <div className={styles.statIcon} style={{ background: '#f59e0b' }}>
              <i className="fas fa-hdd"></i>
            </div>
            <div className={styles.statContent}>
              <div className={styles.statLabel}>磁盘使用率</div>
              <div className={styles.statValue}>{statistics.avgDisk.toFixed(1)}%</div>
              <div className={styles.statDetail}>
                最大: {statistics.maxDisk.toFixed(1)}% | 最小: {statistics.minDisk.toFixed(1)}%
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 数据表格 */}
      <div className={styles.dataSection}>
        <div className={styles.sectionHeader}>
          <h3>
            <i className="fas fa-table"></i> 历史数据记录
          </h3>
        </div>

        {isLoading && (
          <div className={styles.loading}>
            <i className="fas fa-spinner fa-spin"></i> 加载中...
          </div>
        )}

        {!isLoading && historyData && (
          <div className={styles.tableWrapper}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>时间</th>
                  <th>CPU (%)</th>
                  <th>内存 (%)</th>
                  <th>磁盘 (%)</th>
                  <th>网络入 (MB/s)</th>
                  <th>网络出 (MB/s)</th>
                </tr>
              </thead>
              <tbody>
                {historyData.dataPoints && historyData.dataPoints.length > 0 ? (
                  historyData.dataPoints.slice(0, 50).map((point, index) => (
                    <tr key={index}>
                      <td>{new Date(point.timestamp).toLocaleString()}</td>
                      <td>
                        <span
                          className={`${styles.badge} ${
                            point.cpuUsage > 80
                              ? styles.badgeDanger
                              : point.cpuUsage > 60
                              ? styles.badgeWarning
                              : styles.badgeSuccess
                          }`}
                        >
                          {point.cpuUsage.toFixed(1)}%
                        </span>
                      </td>
                      <td>
                        <span
                          className={`${styles.badge} ${
                            point.memoryUsage > 80
                              ? styles.badgeDanger
                              : point.memoryUsage > 60
                              ? styles.badgeWarning
                              : styles.badgeSuccess
                          }`}
                        >
                          {point.memoryUsage.toFixed(1)}%
                        </span>
                      </td>
                      <td>
                        <span
                          className={`${styles.badge} ${
                            point.diskUsage > 80
                              ? styles.badgeDanger
                              : point.diskUsage > 60
                              ? styles.badgeWarning
                              : styles.badgeSuccess
                          }`}
                        >
                          {point.diskUsage.toFixed(1)}%
                        </span>
                      </td>
                      <td>{point.networkIn?.toFixed(2) || '-'}</td>
                      <td>{point.networkOut?.toFixed(2) || '-'}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className={styles.noData}>
                      <i className="fas fa-info-circle"></i> 暂无数据
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default MonitoringHistoryPage;
