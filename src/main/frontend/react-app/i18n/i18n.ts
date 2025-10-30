/**
 * i18next Configuration
 *
 * Internationalization setup for React application using react-i18next.
 * Supports dynamic language switching with localStorage persistence.
 */

import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { LANGUAGE_STORAGE_KEY, DEFAULT_LANGUAGE } from '../config/constants';

// Import translation resources
import zhCN from './locales/zh-CN.json';
import enUS from './locales/en-US.json';

// Initialize i18next
i18n
  .use(initReactI18next) // Connect with React
  .init({
    // Translation resources
    resources: {
      'zh-CN': { translation: zhCN },
      'en-US': { translation: enUS },
    },

    // Language configuration
    lng: typeof window !== 'undefined'
      ? localStorage.getItem(LANGUAGE_STORAGE_KEY) || DEFAULT_LANGUAGE
      : DEFAULT_LANGUAGE,
    fallbackLng: DEFAULT_LANGUAGE,

    // Namespace configuration (using default 'translation')
    defaultNS: 'translation',

    // Interpolation settings
    interpolation: {
      escapeValue: false, // React already escapes values
    },

    // Development settings
    debug: process.env.NODE_ENV === 'development',

    // React-specific options
    react: {
      useSuspense: false, // Avoid suspense for SSR compatibility
    },
  });

// Save language preference to localStorage when changed
i18n.on('languageChanged', (lng) => {
  if (typeof window !== 'undefined') {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, lng);
  }
});

export default i18n;
