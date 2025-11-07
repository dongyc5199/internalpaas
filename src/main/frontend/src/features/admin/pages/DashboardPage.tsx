import { useQuery } from '@tanstack/react-query';
import { dashboardApi } from '../../../shared/api/dashboardApi';
import { QUERY_KEYS } from '../../../shared/constants';
import './admin-dashboard.css';

/**
 * DashboardPage 组件
 *
 * 管理员仪表板页面 - 完全匹配旧页面设计
 */
export function DashboardPage(): React.JSX.Element {
  /**
   * 获取 Dashboard 数据
   */
  const { data, isLoading, refetch } = useQuery({
    queryKey: [QUERY_KEYS.DASHBOARD],
    queryFn: () => dashboardApi.getDashboardData(),
    refetchInterval: 30000, // 每30秒自动刷新
  });

  /**
   * 手动刷新
   */
  const handleRefresh = (): void => {
    void refetch();
  };

  const overview = data?.overview;
  const serverDist = data?.serverDistribution;

  return (
    <main className="content-framework admin-theme admin-dashboard" role="main">
      <div className="content-scrollable">
        {/* 页面头部 */}
        <div className="page-header">
          <div className="page-title-group">
            <div aria-hidden="true" className="page-icon">
              <i>📊</i>
            </div>
            <div className="page-title-content">
              <h1 className="page-title">
                <span>管理员控制台</span>
              </h1>
              <p className="page-description">
                <span>实时监控系统状态，管理服务器资源和用户活动</span>
              </p>
            </div>
          </div>
          <div className="page-actions">
            <div className="last-update" id="globalLastUpdate">
              <span>最后更新：--</span>
            </div>
            <button className="btn btn-secondary" id="exportReportBtn" type="button">
              <i aria-hidden="true">📥</i>
              <span>导出报告</span>
            </button>
            <button
              aria-label="Refresh all data"
              className="btn btn-primary"
              onClick={handleRefresh}
              disabled={isLoading}
              title="Refresh all data"
              type="button"
            >
              <i aria-hidden="true">🔄</i>
              <span>刷新数据</span>
            </button>
          </div>
        </div>

        {/* 统计概览 */}
        <section aria-label="Key metrics" className="stats-overview">
          {/* 服务器集群 */}
          <article
            aria-describedby="statsServersMeta"
            aria-labelledby="statsServersTitle"
            className="stats-card stats-card--servers"
            role="group"
          >
            <header className="stats-card__header">
              <div className="stats-card__heading">
                <div aria-hidden="true" className="stats-card__icon">
                  🖥️
                </div>
                <div className="stats-card__label">
                  <span className="stats-card__title" id="statsServersTitle">
                    服务器集群
                  </span>
                  <span className="stats-card__subtitle">基础设施快照</span>
                </div>
              </div>
              <div className="stats-card__actions">
                <button className="btn btn-link" onClick={handleRefresh} type="button">
                  <span>刷新</span>
                </button>
              </div>
            </header>
            <div className="stats-card__body">
              <div className="stats-card__value-block">
                <div className="stats-card__value-row">
                  <span className="stats-card__value">{overview?.totalServers ?? 0}</span>
                  <span className="stats-card__unit">台</span>
                </div>
                <span className="stats-card__value-label">受管总数</span>
              </div>
              <ul className="stats-card__list" role="list">
                <li className="stats-card__list-item stats-card__list-item--online">
                  <span className="stats-card__indicator"></span>
                  <span className="stats-card__list-label">在线</span>
                  <span className="stats-card__list-value">{overview?.onlineServers ?? 0}</span>
                </li>
                <li className="stats-card__list-item stats-card__list-item--warning">
                  <span className="stats-card__indicator"></span>
                  <span className="stats-card__list-label">警告</span>
                  <span className="stats-card__list-value">{serverDist?.maintenance ?? 0}</span>
                </li>
                <li className="stats-card__list-item stats-card__list-item--offline">
                  <span className="stats-card__indicator"></span>
                  <span className="stats-card__list-label">离线</span>
                  <span className="stats-card__list-value">{serverDist?.offline ?? 0}</span>
                </li>
              </ul>
            </div>
          </article>

          {/* 未处理告警 */}
          <article
            aria-labelledby="statsUnresolvedTitle"
            className="stats-card stats-card--alerts stats-card--unresolved"
            role="group"
          >
            <header className="stats-card__header">
              <div className="stats-card__heading">
                <div aria-hidden="true" className="stats-card__icon">
                  🚨
                </div>
                <div className="stats-card__label">
                  <span className="stats-card__title" id="statsUnresolvedTitle">
                    未处理告警
                  </span>
                  <span className="stats-card__subtitle">待处理事件</span>
                </div>
              </div>
              <div className="stats-card__actions">
                <button className="btn btn-link" type="button">
                  <span>查看告警</span>
                </button>
              </div>
            </header>
            <div className="stats-card__body">
              <div className="stats-card__value-block">
                <div className="stats-card__value-row">
                  <span className="stats-card__value">0</span>
                  <span className="stats-card__delta stats-delta">--</span>
                </div>
                <span className="stats-card__value-label">未处理</span>
              </div>
              <ul className="stats-card__list" role="list">
                <li className="stats-card__list-item stats-card__list-item--critical">
                  <span className="stats-card__indicator"></span>
                  <span className="stats-card__list-label">严重</span>
                  <span className="stats-card__list-value">0</span>
                </li>
                <li className="stats-card__list-item stats-card__list-item--warning">
                  <span className="stats-card__indicator"></span>
                  <span className="stats-card__list-label">预警</span>
                  <span className="stats-card__list-value">0</span>
                </li>
                <li className="stats-card__list-item">
                  <span className="stats-card__indicator"></span>
                  <span className="stats-card__list-label">规则触发</span>
                  <span className="stats-card__list-value">0</span>
                </li>
              </ul>
            </div>
          </article>

          {/* 应用状态 */}
          <article
            aria-describedby="statsAlertsMeta"
            aria-labelledby="statsAlertsTitle"
            className="stats-card stats-card--alerts"
            role="group"
          >
            <header className="stats-card__header">
              <div className="stats-card__heading">
                <div aria-hidden="true" className="stats-card__icon">
                  📦
                </div>
                <div className="stats-card__label">
                  <span className="stats-card__title" id="statsAlertsTitle">
                    应用状态
                  </span>
                  <span className="stats-card__subtitle">部署健康概览</span>
                </div>
              </div>
            </header>
            <div className="stats-card__body">
              <div className="stats-card__value-block">
                <div className="stats-card__value-row">
                  <span className="stats-card__value">{overview?.totalApplications ?? 0}</span>
                  <span className="stats-card__delta stats-delta">--</span>
                </div>
                <span className="stats-card__value-label">总应用数</span>
              </div>
              <ul className="stats-card__list" role="list">
                <li className="stats-card__list-item stats-card__list-item--critical">
                  <span className="stats-card__indicator"></span>
                  <span className="stats-card__list-label">运行中</span>
                  <span className="stats-card__list-value">
                    {overview?.runningApplications ?? 0}
                  </span>
                </li>
                <li className="stats-card__list-item stats-card__list-item--warning">
                  <span className="stats-card__indicator"></span>
                  <span className="stats-card__list-label">已停止</span>
                  <span className="stats-card__list-value">0</span>
                </li>
                <li className="stats-card__list-item">
                  <span className="stats-card__indicator"></span>
                  <span className="stats-card__list-label">异常</span>
                  <span className="stats-card__list-value">0</span>
                </li>
              </ul>
            </div>
          </article>

          {/* 用户活跃度 */}
          <article
            aria-describedby="statsUsersMeta"
            aria-labelledby="statsUsersTitle"
            className="stats-card stats-card--users"
            role="group"
          >
            <header className="stats-card__header">
              <div className="stats-card__heading">
                <div aria-hidden="true" className="stats-card__icon">
                  👥
                </div>
                <div className="stats-card__label">
                  <span className="stats-card__title" id="statsUsersTitle">
                    用户活跃度
                  </span>
                  <span className="stats-card__subtitle">活跃趋势洞察</span>
                </div>
              </div>
            </header>
            <div className="stats-card__body">
              <div className="stats-card__value-block">
                <div className="stats-card__value-row">
                  <span className="stats-card__value">{overview?.activeUsers ?? 0}</span>
                  <span className="stats-card__delta stats-delta">--</span>
                </div>
                <span className="stats-card__value-label">今日活跃</span>
              </div>
              <ul className="stats-card__list" role="list">
                <li className="stats-card__list-item">
                  <span className="stats-card__indicator"></span>
                  <span className="stats-card__list-label">昨日</span>
                  <span className="stats-card__list-value">0</span>
                </li>
                <li className="stats-card__list-item">
                  <span className="stats-card__indicator"></span>
                  <span className="stats-card__list-label">前日</span>
                  <span className="stats-card__list-value">0</span>
                </li>
                <li className="stats-card__list-item">
                  <span className="stats-card__indicator"></span>
                  <span className="stats-card__list-label">总用户</span>
                  <span className="stats-card__list-value">{overview?.totalUsers ?? 0}</span>
                </li>
              </ul>
            </div>
          </article>
        </section>

        {/* 工作区主面板 */}
        <section className="main-content-section">
          {/* 图表可视化区域 */}
          <section aria-label="Dashboard charts" className="charts-section">
            <div className="charts-grid">
              {/* 服务器状态饼图 - 占位 */}
              <div className="chart-container">
                <div className="chart-header">
                  <h3 className="chart-title">
                    <i aria-hidden="true">📊</i>
                    <span>服务器状态分布</span>
                  </h3>
                </div>
                <div className="chart-body">
                  <div className="chart-placeholder">图表数据加载中...</div>
                </div>
              </div>

              {/* 资源使用率柱状图 - 占位 */}
              <div className="chart-container">
                <div className="chart-header">
                  <h3 className="chart-title">
                    <i aria-hidden="true">📈</i>
                    <span>系统资源使用率</span>
                  </h3>
                </div>
                <div className="chart-body">
                  <div className="chart-placeholder">图表数据加载中...</div>
                </div>
              </div>

              {/* 实时监控折线图 - 占位 */}
              <div className="chart-container chart-container--wide">
                <div className="chart-header">
                  <h3 className="chart-title">
                    <i aria-hidden="true">📉</i>
                    <span>实时资源监控</span>
                  </h3>
                  <div className="chart-legend">
                    <span className="legend-item">
                      <span className="legend-dot" style={{ backgroundColor: 'rgb(54, 162, 235)' }}></span>
                      <span>CPU使用率</span>
                    </span>
                    <span className="legend-item">
                      <span className="legend-dot" style={{ backgroundColor: 'rgb(255, 99, 132)' }}></span>
                      <span>内存使用率</span>
                    </span>
                  </div>
                </div>
                <div className="chart-body">
                  <div className="chart-placeholder">图表数据加载中...</div>
                </div>
              </div>
            </div>
          </section>

          {/* 服务器和用户监控面板 */}
          <div className="main-content-area main-content-area--two-columns">
            {/* 服务器监控面板 */}
            <section aria-label="Server monitoring" className="content-panel server-panel">
              <header className="section-header">
                <div>
                  <h2 className="section-title">
                    <i aria-hidden="true">🖥️</i>
                    <span>服务器监控</span>
                  </h2>
                  <p className="section-subtitle">
                    <span className="subtitle-text">实时掌握基础设施健康度与资源使用率</span>
                    <span className="hint-icon" aria-label="指标说明" role="img">ℹ️</span>
                  </p>
                </div>
                <div className="section-actions">
                  <button className="btn btn-secondary" onClick={handleRefresh} type="button">
                    <i aria-hidden="true">🔄</i>
                    <span>刷新</span>
                  </button>
                </div>
              </header>
              <div className="server-metrics">
                <div className="metric">
                  <div className="metric-label">平均 CPU</div>
                  <div className="metric-value"><span>0</span>%</div>
                  <div className="metric-progress">
                    <div className="metric-progress-bar" style={{ width: '0%' }}></div>
                  </div>
                </div>
                <div className="metric">
                  <div className="metric-label">平均内存</div>
                  <div className="metric-value"><span>0</span>%</div>
                  <div className="metric-progress">
                    <div className="metric-progress-bar" style={{ width: '0%' }}></div>
                  </div>
                </div>
                <div className="metric">
                  <div className="metric-label">平均磁盘</div>
                  <div className="metric-value"><span>0</span>%</div>
                  <div className="metric-progress">
                    <div className="metric-progress-bar" style={{ width: '0%' }}></div>
                  </div>
                </div>
              </div>
              <div className="server-view-toggle">
                <button className="btn btn-toggle active" type="button">网格</button>
                <button className="btn btn-toggle" type="button">列表</button>
                <span className="status-hint" aria-live="polite">
                  <span aria-hidden="true" className="status-hint__icon">❗</span>
                  <span>仅显示负载偏高或健康度偏低的服务器。</span>
                </span>
              </div>
              <div aria-live="polite" className="server-status">
                <div className="empty-state server-alert-placeholder">
                  当前所有监控服务器运行正常。
                </div>
              </div>
            </section>

            {/* 用户监控面板 */}
            <section aria-label="User monitoring" className="content-panel user-panel">
              <header className="section-header">
                <div>
                  <h2 className="section-title">
                    <i aria-hidden="true">👥</i>
                    <span>用户监控</span>
                  </h2>
                  <p className="section-subtitle">洞察活跃趋势与关键用户行为</p>
                </div>
                <div className="section-actions">
                  <select aria-label="User time range" className="form-select">
                    <option value="1d">过去24小时</option>
                    <option value="7d">过去7天</option>
                    <option value="30d">过去30天</option>
                  </select>
                  <button className="btn btn-secondary" onClick={handleRefresh} type="button">
                    <i aria-hidden="true">🔁</i>
                    <span>刷新</span>
                  </button>
                </div>
              </header>
              <div className="user-summary">
                <article className="summary-item">
                  <span className="summary-label">今日活跃</span>
                  <strong className="summary-value">{overview?.activeUsers ?? 0}</strong>
                </article>
                <article className="summary-item">
                  <span className="summary-label">昨日</span>
                  <strong className="summary-value">0</strong>
                </article>
                <article className="summary-item">
                  <span className="summary-label">转化率</span>
                  <strong className="summary-value">--</strong>
                </article>
              </div>
              <div className="user-trend">
                <h3 className="trend-title">活跃趋势</h3>
                <div className="chart-placeholder empty-state" aria-live="polite">
                  暂无活跃趋势数据，这里将展示活跃用户变化曲线
                </div>
              </div>
              <div className="user-events">
                <div className="user-events-header">
                  <h3>关键事件</h3>
                  <button className="btn btn-link" type="button">
                    <span>查看全部</span>
                  </button>
                </div>
                <ul aria-live="polite" className="item-list">
                  <li className="empty-state">暂无用户活动</li>
                </ul>
              </div>
            </section>
          </div>

          {/* 部署管理平台面板 */}
          <section className="content-panel deploy-platform-panel" aria-labelledby="deploy-platform-title">
            <header className="section-header">
              <div>
                <h2 className="section-title" id="deploy-platform-title">部署管理平台</h2>
                <p className="section-subtitle">统一查看发布、回滚、审批与审计状态</p>
              </div>
            </header>
            <div className="card deploy-platform-card">
              <div className="card-body">
                <div id="deploy-platform-root" className="deploy-platform-host">
                  正在加载部署管理面板…
                </div>
              </div>
            </div>
          </section>
        </section>
      </div>
    </main>
  );
}
