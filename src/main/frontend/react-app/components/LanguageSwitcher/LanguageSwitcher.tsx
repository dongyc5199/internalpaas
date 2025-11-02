/**
 * LanguageSwitcher Component
 *
 * Dropdown menu for switching between supported languages (zh-CN / en-US).
 * Persists language preference to localStorage.
 */

import { useTranslation } from 'react-i18next';
import { SUPPORTED_LANGUAGES } from '../../config/constants';
import styles from './LanguageSwitcher.module.css';

export interface LanguageSwitcherProps {
  className?: string;
}

export function LanguageSwitcher({ className }: LanguageSwitcherProps): JSX.Element {
  const { t, i18n } = useTranslation();

  const handleLanguageChange = (event: React.ChangeEvent<HTMLSelectElement>): void => {
    const newLanguage = event.target.value;
    i18n.changeLanguage(newLanguage);
  };

  return (
    <div className={`${styles.languageSwitcher} ${className ?? ''}`}>
      <label htmlFor="language-select" className={styles.label}>
        🌐
      </label>
      <select
        id="language-select"
        className={styles.select}
        value={i18n.language}
        onChange={handleLanguageChange}
        aria-label={t('language.switchLanguage', '切换语言')}
      >
        {SUPPORTED_LANGUAGES.map((lang) => (
          <option key={lang} value={lang}>
            {t(`language.${lang}`)}
          </option>
        ))}
      </select>
    </div>
  );
}
