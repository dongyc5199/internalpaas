import styles from './Loading.module.css';

/**
 * Loading Props
 */
export interface LoadingProps {
  /** 加载文本 */
  text?: string;
  /** 是否全屏 */
  fullScreen?: boolean;
}

/**
 * Loading 组件
 *
 * 通用加载指示器
 *
 * @example
 * ```tsx
 * <Loading text="加载中..." />
 * <Loading fullScreen />
 * ```
 */
export function Loading({ text = '加载中...', fullScreen = false }: LoadingProps): React.JSX.Element {
  return (
    <div className={`${styles.loading} ${fullScreen ? styles.fullScreen : ''}`}>
      <div className={styles.spinner} />
      <p className={styles.text}>{text}</p>
    </div>
  );
}
