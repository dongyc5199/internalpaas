import { useState, FormEvent } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../../shared/stores/authStore';
import { ROUTES } from '../../../shared/constants';
import './login.css';

interface LocationState {
  from?: {
    pathname: string;
  };
}

/**
 * LoginPage 组件
 *
 * 用户登录页面
 *
 * 特性:
 * - 表单验证
 * - 错误提示
 * - 加载状态
 * - 登录后自动跳转到之前的页面
 *
 * @example
 * ```tsx
 * <Route path="/login" element={<LoginPage />} />
 * ```
 */
export function LoginPage(): React.JSX.Element {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuthStore();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // 获取登录前的路径
  const from = (location.state as LocationState)?.from?.pathname ?? ROUTES.HOME;

  /**
   * 处理表单提交
   */
  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setError('');

    // 基础验证
    if (!username.trim()) {
      setError('请输入用户名');
      return;
    }

    if (!password) {
      setError('请输入密码');
      return;
    }

    if (password.length < 6) {
      setError('密码至少6个字符');
      return;
    }

    setIsLoading(true);

    try {
      await login({ username: username.trim(), password });

      // 登录成功,跳转到之前的页面
      navigate(from, { replace: true });
    } catch (err) {
      console.error('Login failed:', err);
      setError(err instanceof Error ? err.message : '登录失败,请检查用户名和密码');
    } finally {
      setIsLoading(false);
    }
  };

  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);

  return (
    <div className="auth-page login-view">
      <div className="login-background auth-background"></div>

      <div className="login-shell auth-shell">
        {/* 左侧边栏 */}
        <aside className="login-sidebar auth-sidebar">
          <div className="sidebar-inner">
            {/* 品牌标识 */}
            <div className="brand-lockup login-brand">
              <div className="brand-icon-container brand-icon-xxl">DD</div>
              <div className="brand-copy">
                <h1 className="brand-title brand-text-main">Dev Debug Platform</h1>
                <p className="brand-subtitle brand-text-desc">统一的内部运维与开发调试中枢</p>
              </div>
            </div>

            <p className="sidebar-tagline">通过集中式工具链和实时洞察，提高团队交付效率与环境稳定性。</p>

            <dl className="sidebar-highlights">
              <div className="highlight-item">
                <dt>环境态势一览</dt>
                <dd>实时掌握服务器、任务与告警状态，支持按业务域快速定位。</dd>
              </div>
              <div className="highlight-item">
                <dt>自动化协同</dt>
                <dd>内置批量操作、远程终端与发布流程，轻松协同多角色团队。</dd>
              </div>
              <div className="highlight-item">
                <dt>合规与审计</dt>
                <dd>关键操作全链路留痕，可视化追踪审计，守护平台安全。</dd>
              </div>
            </dl>

            <div className="sidebar-divider" aria-hidden="true"></div>

            <div className="sidebar-meta">
              <div className="meta-block">
                <span className="meta-label">访问策略</span>
                <span className="meta-value">仅限公司内部网络</span>
              </div>
              <div className="meta-block">
                <span className="meta-label">支持时间</span>
                <span className="meta-value">07:00 - 23:00 工作时段</span>
              </div>
              <div className="meta-block status-block" role="status">
                <span className="meta-label">平台状态</span>
                <span className="meta-value status-value">服务正常</span>
              </div>
            </div>
          </div>
        </aside>

        {/* 右侧登录面板 */}
        <main className="login-panel auth-panel" aria-labelledby="loginHeading">
          <header className="panel-header">
            <h2 className="panel-title" id="loginHeading">账号登录</h2>
            <p className="panel-caption">使用统一账号登录 Dev Debug Platform</p>
            <div className="panel-badges" aria-hidden="true">
              <span className="panel-badge">SAML 集成</span>
              <span className="panel-badge">LDAP 同步</span>
              <span className="panel-badge">双因子支持</span>
            </div>
          </header>

          <section className="feedback-area" aria-live="polite">
            {error && (
              <div className="alert error" role="alert">
                <div className="alert-indicator" aria-hidden="true">!</div>
                <div className="alert-content">
                  <strong>登录失败</strong>
                  <p>{error}</p>
                </div>
              </div>
            )}
          </section>

          <form
            className="login-form auth-form"
            onSubmit={(e) => {
              void handleSubmit(e);
            }}
            noValidate
          >
            <div className="form-field">
              <label className="field-label" htmlFor="username">
                用户名
              </label>
              <div className="field-control">
                <input
                  type="text"
                  id="username"
                  name="username"
                  placeholder="请输入用户名"
                  autoComplete="username"
                  inputMode="text"
                  required
                  value={username}
                  onChange={(e) => { setUsername(e.target.value); }}
                  disabled={isLoading}
                  autoFocus
                />
              </div>
              <p className="field-hint">可使用企业账号或 SSO 账号登录</p>
            </div>

            <div className="form-field">
              <label className="field-label" htmlFor="password">
                密码
              </label>
              <div className="field-control">
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="password"
                  name="password"
                  placeholder="请输入密码"
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(e) => { setPassword(e.target.value); }}
                  disabled={isLoading}
                />
                <button
                  type="button"
                  className="control-addon"
                  aria-controls="password"
                  aria-label="显示或隐藏密码"
                  onClick={() => { setShowPassword(!showPassword); }}
                >
                  {showPassword ? '隐藏' : '显示'}
                </button>
              </div>
              <p className="field-hint" hidden>
                已启用大写锁定，注意密码大小写。
              </p>
            </div>

            <div className="form-assist">
              <label className="remember-option">
                <input
                  type="checkbox"
                  name="remember-me"
                  checked={rememberMe}
                  onChange={(e) => { setRememberMe(e.target.checked); }}
                />
                <span className="checkbox-box" aria-hidden="true"></span>
                <span className="checkbox-label">记住我</span>
              </label>
              <div className="assist-links">
                <a href="#" className="assist-link" onClick={(e) => { e.preventDefault(); }}>
                  忘记密码？
                </a>
                <a href="#" className="assist-link" onClick={(e) => { e.preventDefault(); }}>
                  查看系统公告
                </a>
              </div>
            </div>

            <button type="submit" className="submit-btn" disabled={isLoading}>
              <span className="submit-text">{isLoading ? '登录中...' : '登录'}</span>
              <span className="submit-icon" aria-hidden="true">
                →
              </span>
              <span className="submit-progress" aria-hidden="true"></span>
            </button>

            <div className="form-meta" aria-live="polite">
              <div className="meta-item">
                <span className="meta-icon" aria-hidden="true">
                  🔐
                </span>
                <span className="meta-text">平台已启用 TLS 1.3 与操作审计</span>
              </div>
              <div className="meta-item">
                <span className="meta-icon" aria-hidden="true">
                  🛡️
                </span>
                <span className="meta-text">密码最短 12 位，建议开启 MFA</span>
              </div>
            </div>
          </form>

          <footer className="panel-footer">
            <p className="register-tip">
              还没有账号？{' '}
              <a href={ROUTES.REGISTER} className="register-link">
                申请开通
              </a>
            </p>
            <div className="footer-links">
              <a href="#" className="footer-link" onClick={(e) => { e.preventDefault(); }}>
                平台公告
              </a>
              <a href="#" className="footer-link" onClick={(e) => { e.preventDefault(); }}>
                帮助中心
              </a>
              <a href="#" className="footer-link" onClick={(e) => { e.preventDefault(); }}>
                隐私政策
              </a>
            </div>
          </footer>
        </main>
      </div>
    </div>
  );
}
