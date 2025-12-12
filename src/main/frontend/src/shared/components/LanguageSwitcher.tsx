import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useUIStore, type LanguageCode } from '../stores/uiStore';

export interface LanguageSwitcherProps {
  className?: string;
}

const LANG_OPTIONS: { code: LanguageCode; label: string }[] = [
  { code: 'zh', label: '中文' },
  { code: 'en', label: 'English' },
];

/**
 * 语言切换组件，更新 i18n 语言并写入本地存储。
 */
export function LanguageSwitcher({ className }: LanguageSwitcherProps): React.JSX.Element {
  const { i18n } = useTranslation();
  const { language, setLanguage } = useUIStore((state) => ({
    language: state.language,
    setLanguage: state.setLanguage,
  }));

  useEffect(() => {
    void i18n.changeLanguage(language);
  }, [language, i18n]);

  const handleChange = (value: LanguageCode): void => {
    setLanguage(value);
    void i18n.changeLanguage(value);
    localStorage.setItem('i18nextLng', value);
  };

  return (
    <select
      className={className}
      value={language}
      onChange={(e) => handleChange(e.target.value as LanguageCode)}
      aria-label="切换语言"
    >
      {LANG_OPTIONS.map((opt) => (
        <option key={opt.code} value={opt.code}>
          {opt.label}
        </option>
      ))}
    </select>
  );
}
