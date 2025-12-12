import styles from './ApplicationsPage.module.css';

export function ApplicationsPage(): React.JSX.Element {
  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>应用管理</h1>
          <p className={styles.subtitle}>集中管理部署的应用，未来会替换旧版 Thymeleaf 页面</p>
        </div>
      </div>

      <div className={styles.placeholder}>
        <p>应用管理功能即将迁移，敬请期待。</p>
      </div>
    </div>
  );
}
