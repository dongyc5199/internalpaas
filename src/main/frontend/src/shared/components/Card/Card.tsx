import styles from './Card.module.css';

export interface CardProps {
  title?: string;
  description?: string;
  headerSlot?: React.ReactNode;
  children: React.ReactNode;
  footer?: React.ReactNode;
  variant?: 'default' | 'ghost' | 'elevated';
  className?: string;
}

/**
 * 统一的卡片容器，支持可选头/脚和阴影变体。
 */
export function Card({
  title,
  description,
  headerSlot,
  children,
  footer,
  variant = 'default',
  className,
}: CardProps): React.JSX.Element {
  const cardClass = [styles.card, styles[variant], className].filter(Boolean).join(' ');

  return (
    <div className={cardClass}>
      {(title || description || headerSlot) && (
        <div className={styles.header}>
          <div>
            {title && <h3 className={styles.title}>{title}</h3>}
            {description && <p className={styles.description}>{description}</p>}
          </div>
          {headerSlot && <div className={styles.headerSlot}>{headerSlot}</div>}
        </div>
      )}

      <div className={styles.body}>{children}</div>

      {footer && <div className={styles.footer}>{footer}</div>}
    </div>
  );
}
