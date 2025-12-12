import styles from './ProfilePage.module.css';

export function ProfilePage(): React.JSX.Element {
  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>个人资料</h1>
          <p className={styles.subtitle}>查看与编辑账户信息，后续将补充偏好与安全设置。</p>
        </div>
      </div>

      <div className={styles.placeholder}>
        <p>个人资料模块正在迁移中，稍后提供完整功能。</p>
      </div>
    </div>
  );
}
