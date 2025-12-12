import { useState, FormEvent, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../../../shared/api/userApi';
import { ROUTES } from '../../../shared/constants';
import './login.css';

/**
 * RegisterPage 组件
 *
 * 用户注册页面
 *
 * 特性:
 * - 表单验证（用户名、邮箱、密码强度、密码匹配）
 * - 实时密码强度检测
 * - 错误提示
 * - 加载状态
 * - 注册成功后自动跳转到登录页
 *
 * @example
 * ```tsx
 * <Route path="/register" element={<RegisterPage />} />
 * ```
 */
export function RegisterPage(): React.JSX.Element {
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // 用户名验证状态
  const [usernameChecking, setUsernameChecking] = useState(false);
  const [usernameAvailable, setUsernameAvailable] = useState<boolean | null>(null);
  const [usernameError, setUsernameError] = useState('');

  /**
   * 验证密码强度
   */
  const validatePasswordStrength = (pwd: string): string | null => {
    if (pwd.length < 12) {
      return '密码至少需要12个字符';
    }

    const hasUpperCase = /[A-Z]/.test(pwd);
    const hasLowerCase = /[a-z]/.test(pwd);
    const hasNumber = /\d/.test(pwd);
    const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(pwd);

    const strengthCount = [hasUpperCase, hasLowerCase, hasNumber, hasSpecialChar].filter(Boolean).length;

    if (strengthCount < 3) {
      return '密码需要包含大写字母、小写字母、数字和特殊字符中的至少3种';
    }

    return null;
  };

  /**
   * 获取密码强度等级
   */
  const getPasswordStrength = (pwd: string): { level: number; label: string; color: string } => {
    if (pwd.length === 0) {
      return { level: 0, label: '', color: '' };
    }

    if (pwd.length < 12) {
      return { level: 1, label: '弱', color: '#ef4444' };
    }

    const hasUpperCase = /[A-Z]/.test(pwd);
    const hasLowerCase = /[a-z]/.test(pwd);
    const hasNumber = /\d/.test(pwd);
    const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(pwd);

    const strengthCount = [hasUpperCase, hasLowerCase, hasNumber, hasSpecialChar].filter(Boolean).length;

    if (strengthCount <= 2) {
      return { level: 2, label: '中', color: '#f59e0b' };
    } else if (strengthCount === 3) {
      return { level: 3, label: '强', color: '#10b981' };
    } else {
      return { level: 4, label: '非常强', color: '#059669' };
    }
  };

  const passwordStrength = getPasswordStrength(password);

  /**
   * 检查用户名是否可用（防抖）
   */
  const checkUsernameAvailability = useCallback(async (usernameToCheck: string): Promise<void> => {
    if (!usernameToCheck.trim() || usernameToCheck.trim().length < 3) {
      setUsernameAvailable(null);
      setUsernameError('');
      return;
    }

    setUsernameChecking(true);
    setUsernameError('');

    try {
      const available = await userApi.checkUsernameAvailable(usernameToCheck.trim());
      setUsernameAvailable(available);
      if (!available) {
        setUsernameError('用户名已被占用');
      }
    } catch (err) {
      console.error('Check username failed:', err);
      setUsernameAvailable(null);
      setUsernameError('检查用户名失败，请稍后重试');
    } finally {
      setUsernameChecking(false);
    }
  }, []);

  /**
   * 用户名变化时进行防抖检查
   */
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      void checkUsernameAvailability(username);
    }, 500); // 500ms 防抖

    return () => {
      clearTimeout(timeoutId);
    };
  }, [username, checkUsernameAvailability]);

  /**
   * 处理表单提交
   */
  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setError('');
    setSuccess('');

    // 基础验证
    if (!username.trim()) {
      setError('请输入用户名');
      return;
    }

    if (username.trim().length < 3) {
      setError('用户名至少3个字符');
      return;
    }

    // 检查用户名是否可用
    if (usernameAvailable === false) {
      setError('用户名已被占用，请选择其他用户名');
      return;
    }

    if (!email.trim()) {
      setError('请输入邮箱地址');
      return;
    }

    const emailRegex = /^[A-Za-z0-9+_.-]+@(.+)$/;
    if (!emailRegex.test(email.trim())) {
      setError('请输入有效的邮箱地址');
      return;
    }

    if (!password) {
      setError('请输入密码');
      return;
    }

    // 密码强度验证
    const passwordError = validatePasswordStrength(password);
    if (passwordError) {
      setError(passwordError);
      return;
    }

    if (!confirmPassword) {
      setError('请确认密码');
      return;
    }

    if (password !== confirmPassword) {
      setError('两次输入的密码不一致');
      return;
    }

    setIsLoading(true);

    try {
      await userApi.register({
        username: username.trim(),
        email: email.trim(),
        password,
        confirmPassword,
      });

      setSuccess('注册成功！管理员将在1-2个工作日内审批您的账号。审批通过后，您将收到邮件通知。');

      // 3秒后跳转到登录页
      setTimeout(() => {
        navigate(ROUTES.LOGIN);
      }, 3000);
    } catch (err) {
      console.error('Registration failed:', err);
      setError(err instanceof Error ? err.message : '注册失败，请稍后重试');
    } finally {
      setIsLoading(false);
    }
  };

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
                <span className="meta-label">审批时效</span>
                <span className="meta-value">1-2个工作日</span>
              </div>
              <div className="meta-block">
                <span className="meta-label">账号类型</span>
                <span className="meta-value">企业统一账号</span>
              </div>
              <div className="meta-block status-block" role="status">
                <span className="meta-label">注册状态</span>
                <span className="meta-value status-value">开放申请</span>
              </div>
            </div>
          </div>
        </aside>

        {/* 右侧注册面板 */}
        <main className="login-panel auth-panel" aria-labelledby="registerHeading">
          <header className="panel-header">
            <h2 className="panel-title" id="registerHeading">账号注册</h2>
            <p className="panel-caption">申请开通 Dev Debug Platform 访问权限</p>
            <div className="panel-badges" aria-hidden="true">
              <span className="panel-badge">企业邮箱</span>
              <span className="panel-badge">审批制度</span>
              <span className="panel-badge">安全认证</span>
            </div>
          </header>

          <section className="feedback-area" aria-live="polite">
            {error && (
              <div className="alert error" role="alert">
                <div className="alert-indicator" aria-hidden="true">!</div>
                <div className="alert-content">
                  <strong>注册失败</strong>
                  <p>{error}</p>
                </div>
              </div>
            )}
            {success && (
              <div className="alert success" role="alert">
                <div className="alert-indicator" aria-hidden="true">✓</div>
                <div className="alert-content">
                  <strong>注册成功</strong>
                  <p>{success}</p>
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
                  minLength={3}
                  value={username}
                  onChange={(e) => {
                    setUsername(e.target.value);
                    setUsernameAvailable(null);
                    setUsernameError('');
                  }}
                  disabled={isLoading}
                  autoFocus
                  style={{
                    borderColor:
                      usernameAvailable === true
                        ? '#10b981'
                        : usernameAvailable === false || usernameError
                        ? '#ef4444'
                        : undefined,
                  }}
                />
                {usernameChecking && (
                  <span className="control-addon" style={{ pointerEvents: 'none' }}>
                    检查中...
                  </span>
                )}
                {!usernameChecking && usernameAvailable === true && username.trim().length >= 3 && (
                  <span className="control-addon" style={{ pointerEvents: 'none', color: '#10b981' }}>
                    ✓ 可用
                  </span>
                )}
                {!usernameChecking && usernameAvailable === false && (
                  <span className="control-addon" style={{ pointerEvents: 'none', color: '#ef4444' }}>
                    ✗ 已占用
                  </span>
                )}
              </div>
              {usernameError ? (
                <p className="field-hint" style={{ color: '#ef4444' }}>
                  {usernameError}
                </p>
              ) : (
                <p className="field-hint">建议使用企业花名或统一账号，至少3个字符</p>
              )}
            </div>

            <div className="form-field">
              <label className="field-label" htmlFor="email">
                企业邮箱
              </label>
              <div className="field-control">
                <input
                  type="email"
                  id="email"
                  name="email"
                  placeholder="请输入企业邮箱"
                  autoComplete="email"
                  inputMode="email"
                  required
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                  }}
                  disabled={isLoading}
                />
              </div>
              <p className="field-hint">用于接收审批通知和账号信息</p>
            </div>

            <div className="form-field">
              <label className="field-label" htmlFor="password">
                设置密码
              </label>
              <div className="field-control">
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="password"
                  name="password"
                  placeholder="请输入密码"
                  autoComplete="new-password"
                  required
                  minLength={12}
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                  }}
                  disabled={isLoading}
                />
                <button
                  type="button"
                  className="control-addon"
                  aria-controls="password"
                  aria-label="显示或隐藏密码"
                  onClick={() => {
                    setShowPassword(!showPassword);
                  }}
                >
                  {showPassword ? '隐藏' : '显示'}
                </button>
              </div>
              {password && (
                <div className="password-strength">
                  <div className="strength-bar">
                    <div
                      className="strength-fill"
                      style={{
                        width: `${(passwordStrength.level / 4) * 100}%`,
                        backgroundColor: passwordStrength.color,
                      }}
                    ></div>
                  </div>
                  <span className="strength-label" style={{ color: passwordStrength.color }}>
                    {passwordStrength.label}
                  </span>
                </div>
              )}
              <p className="field-hint">密码至少12位，包含大小写字母、数字和特殊字符</p>
            </div>

            <div className="form-field">
              <label className="field-label" htmlFor="confirmPassword">
                确认密码
              </label>
              <div className="field-control">
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  id="confirmPassword"
                  name="confirmPassword"
                  placeholder="请再次输入密码"
                  autoComplete="new-password"
                  required
                  minLength={12}
                  value={confirmPassword}
                  onChange={(e) => {
                    setConfirmPassword(e.target.value);
                  }}
                  disabled={isLoading}
                />
                <button
                  type="button"
                  className="control-addon"
                  aria-controls="confirmPassword"
                  aria-label="显示或隐藏确认密码"
                  onClick={() => {
                    setShowConfirmPassword(!showConfirmPassword);
                  }}
                >
                  {showConfirmPassword ? '隐藏' : '显示'}
                </button>
              </div>
              {confirmPassword && password !== confirmPassword && (
                <p className="field-hint" style={{ color: '#ef4444' }}>
                  两次输入的密码不一致
                </p>
              )}
            </div>

            <button type="submit" className="submit-btn" disabled={isLoading || !!success}>
              <span className="submit-text">{isLoading ? '注册中...' : success ? '注册成功' : '注册'}</span>
              <span className="submit-icon" aria-hidden="true">
                →
              </span>
              <span className="submit-progress" aria-hidden="true"></span>
            </button>

            <div className="form-meta" aria-live="polite">
              <div className="meta-item">
                <span className="meta-icon" aria-hidden="true">
                  📋
                </span>
                <span className="meta-text">注册后需管理员审批，1-2个工作日内完成</span>
              </div>
              <div className="meta-item">
                <span className="meta-icon" aria-hidden="true">
                  🔐
                </span>
                <span className="meta-text">密码最短 12 位，建议开启 MFA</span>
              </div>
            </div>
          </form>

          <footer className="panel-footer">
            <p className="register-tip">
              已有账号？{' '}
              <a href={ROUTES.LOGIN} className="register-link">
                立即登录
              </a>
            </p>
            <div className="footer-links">
              <a href="#" className="footer-link" onClick={(e) => { e.preventDefault(); }}>
                注册协议
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
