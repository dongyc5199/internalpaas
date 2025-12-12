import { useState, FormEvent, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { configApi } from '../../../shared/api/configApi';
import { ROUTES } from '../../../shared/constants';
import './initial-config.css';

/**
 * InitialConfigPage 组件
 *
 * 初始配置向导页面
 *
 * 特性:
 * - 工作目录配置
 * - 可选功能配置
 * - 配置预览
 * - 进度指示器
 *
 * @example
 * ```tsx
 * <Route path="/initial-config" element={<InitialConfigPage />} />
 * ```
 */
export function InitialConfigPage(): React.JSX.Element {
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [isFirstTime, setIsFirstTime] = useState(true);
  const [workDirectory, setWorkDirectory] = useState('');
  const [enableNotifications, setEnableNotifications] = useState(true);
  const [autoBackup, setAutoBackup] = useState(true);
  const [enableMetrics, setEnableMetrics] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [currentStep, setCurrentStep] = useState(1);
  const [showOptional, setShowOptional] = useState(false);

  /**
   * 加载用户配置信息
   */
  useEffect(() => {
    const loadConfigInfo = async (): Promise<void> => {
      try {
        const info = await configApi.getUserConfigInfo();
        setUsername(info.username);
        setIsFirstTime(info.isFirstTime);

        if (info.workDirectory) {
          // 如果已有工作目录，跳转到首页
          navigate(ROUTES.HOME);
        } else {
          setWorkDirectory(`./workspaces/${info.username}/`);
        }
      } catch (err) {
        console.error('Load config info failed:', err);
        setError('加载配置信息失败');
      }
    };

    void loadConfigInfo();
  }, [navigate]);

  /**
   * 处理表单提交
   */
  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!agreeTerms) {
      setError('请先同意用户协议和隐私政策');
      return;
    }

    setIsLoading(true);
    setCurrentStep(2);

    try {
      await configApi.saveInitialConfig({
        workDirectory,
        enableNotifications,
        autoBackup,
        enableMetrics,
      });

      setCurrentStep(3);
      setSuccess('配置成功！正在跳转...');

      setTimeout(() => {
        navigate(ROUTES.HOME);
      }, 2000);
    } catch (err) {
      console.error('Save config failed:', err);
      setError(err instanceof Error ? err.message : '配置保存失败，请稍后重试');
      setCurrentStep(1);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * 复制路径到剪贴板
   */
  const handleCopyPath = (): void => {
    navigator.clipboard
      .writeText(workDirectory)
      .then(() => {
        setSuccess('路径已复制到剪贴板');
        setTimeout(() => {
          setSuccess('');
        }, 2000);
      })
      .catch(() => {
        setError('复制失败，请手动复制');
      });
  };

  return (
    <div className="config-page">
      <div className="config-background">
        <div className="gradient-orb orb-1"></div>
        <div className="gradient-orb orb-2"></div>
        <div className="gradient-orb orb-3"></div>
      </div>

      <div className="config-container">
        {/* 页面头部 */}
        <header className="config-header">
          <div className="brand-section">
            <div className="brand-icon-container">DD</div>
            <div className="brand-info">
              <h1 className="brand-title">Dev Debug Platform</h1>
              <p className="brand-subtitle">开发调试平台配置向导</p>
            </div>
          </div>
        </header>

        {/* 配置向导 */}
        <div className="config-wizard">
          {/* 进度指示器 */}
          <div className="progress-section">
            <h2 className="progress-title">{isFirstTime ? '🎉 欢迎首次使用！' : '⚙️ 工作目录配置'}</h2>
            <div className="progress-indicator">
              <div className={`step-indicator ${currentStep >= 1 ? 'active' : ''}`}>
                <div className="step-number">1</div>
                <span className="step-label">配置说明</span>
              </div>
              <div className="progress-line"></div>
              <div className={`step-indicator ${currentStep >= 2 ? 'active' : ''}`}>
                <div className="step-number">2</div>
                <span className="step-label">确认配置</span>
              </div>
              <div className="progress-line"></div>
              <div className={`step-indicator ${currentStep >= 3 ? 'active' : ''}`}>
                <div className="step-number">3</div>
                <span className="step-label">开始使用</span>
              </div>
            </div>
          </div>

          {/* 配置内容 */}
          <div className="config-content">
            {/* 欢迎信息 */}
            {isFirstTime && (
              <div className="welcome-card">
                <div className="welcome-icon">👋</div>
                <div className="welcome-content">
                  <h3>欢迎使用 Dev Debug Platform！</h3>
                  <p>我们将为您快速配置个人开发环境，让您能够立即开始使用平台的所有功能。</p>
                  <div className="welcome-features">
                    <div className="feature-item">
                      <span className="feature-icon">📁</span>
                      <span className="feature-text">自动创建工作目录</span>
                    </div>
                    <div className="feature-item">
                      <span className="feature-icon">🔧</span>
                      <span className="feature-text">配置开发环境</span>
                    </div>
                    <div className="feature-item">
                      <span className="feature-icon">🚀</span>
                      <span className="feature-text">一键启动应用</span>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* 工作目录预览 */}
            <div className="directory-preview">
              <div className="preview-header">
                <div className="preview-icon">📁</div>
                <h3>工作目录预览</h3>
              </div>
              <div className="directory-info">
                <div className="info-item">
                  <div className="info-label">用户名</div>
                  <div className="info-value">{username}</div>
                </div>
                <div className="info-item">
                  <div className="info-label">工作目录</div>
                  <div className="info-value directory-path">
                    <span>{workDirectory}</span>
                    <button type="button" className="copy-btn" onClick={handleCopyPath}>
                      📋
                    </button>
                  </div>
                </div>
              </div>
            </div>

            {/* 反馈信息 */}
            {(error || success) && (
              <div className="feedback-area">
                {error && (
                  <div className="alert error">
                    <span className="alert-icon">❌</span>
                    <span className="alert-text">{error}</span>
                  </div>
                )}
                {success && (
                  <div className="alert success">
                    <span className="alert-icon">✅</span>
                    <span className="alert-text">{success}</span>
                  </div>
                )}
              </div>
            )}

            {/* 配置表单 */}
            <form className="config-form" onSubmit={(e) => { void handleSubmit(e); }}>
              {/* 可选配置 */}
              <div className="optional-config">
                <div className="optional-header">
                  <h4>🔧 可选配置</h4>
                  <button
                    type="button"
                    className="toggle-optional"
                    onClick={() => { setShowOptional(!showOptional); }}
                  >
                    {showOptional ? '收起选项 ▲' : '展开选项 ▼'}
                  </button>
                </div>
                {showOptional && (
                  <div className="optional-content">
                    <label className="option-label">
                      <input
                        type="checkbox"
                        checked={enableNotifications}
                        onChange={(e) => { setEnableNotifications(e.target.checked); }}
                      />
                      <span className="option-text">
                        <strong>启用通知提醒</strong>
                        <small>接收应用状态变更和系统消息通知</small>
                      </span>
                    </label>
                    <label className="option-label">
                      <input
                        type="checkbox"
                        checked={autoBackup}
                        onChange={(e) => { setAutoBackup(e.target.checked); }}
                      />
                      <span className="option-text">
                        <strong>自动备份配置</strong>
                        <small>定期备份重要的配置文件和应用数据</small>
                      </span>
                    </label>
                    <label className="option-label">
                      <input
                        type="checkbox"
                        checked={enableMetrics}
                        onChange={(e) => { setEnableMetrics(e.target.checked); }}
                      />
                      <span className="option-text">
                        <strong>性能监控</strong>
                        <small>收集应用性能指标用于优化建议</small>
                      </span>
                    </label>
                  </div>
                )}
              </div>

              {/* 用户协议 */}
              <div className="agreement-section">
                <label className="agreement-label">
                  <input
                    type="checkbox"
                    checked={agreeTerms}
                    onChange={(e) => { setAgreeTerms(e.target.checked); }}
                    required
                  />
                  <span className="agreement-text">
                    我已阅读并同意 <a href="#" onClick={(e) => { e.preventDefault(); }}>《用户协议》</a> 和{' '}
                    <a href="#" onClick={(e) => { e.preventDefault(); }}>《隐私政策》</a>
                  </span>
                </label>
              </div>

              {/* 提交按钮 */}
              <div className="form-actions">
                <button type="submit" className="submit-btn" disabled={isLoading}>
                  <span className="btn-icon">🚀</span>
                  <span className="btn-text">{isLoading ? '配置中...' : '确认并开始使用'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
