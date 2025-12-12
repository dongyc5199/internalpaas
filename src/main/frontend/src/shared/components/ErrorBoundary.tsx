import { Component, type ErrorInfo, type ReactNode } from 'react';
import styles from './ErrorBoundary.module.css';

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error?: Error;
}

/**
 * 捕获子树运行时错误并提供兜底 UI。
 * 用于阻断整棵应用因单个组件崩溃而白屏。
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  public constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false };
  }

  // React 要求 static，不支持 override 关键字
  public static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  public override componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    // 开发环境打印，后续可接入后端日志
    console.error('ErrorBoundary 捕获错误:', error, errorInfo);
  }

  private handleRetry = (): void => {
    this.setState({ hasError: false, error: undefined });
  };

  private handleReload = (): void => {
    window.location.reload();
  };

  public override render(): ReactNode {
    if (this.state.hasError) {
      return (
        <div className={styles.container} role="alert">
          <h2 className={styles.title}>页面发生错误</h2>
          <p className={styles.message}>
            {this.state.error?.message ?? '未知错误，请稍后重试。'}
          </p>
          <div className={styles.actions}>
            <button className={`${styles.button} ${styles.primary}`} onClick={this.handleReload}>
              刷新页面
            </button>
            <button className={`${styles.button} ${styles.secondary}`} onClick={this.handleRetry}>
              返回继续尝试
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
