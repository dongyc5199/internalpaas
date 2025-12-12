import { useCallback, useEffect, useMemo } from 'react';
import { useUIStore, type ThemeMode } from '../stores/uiStore';

const DARK_QUERY = '(prefers-color-scheme: dark)';

export interface UseThemeResult {
  theme: ThemeMode;
  resolvedTheme: Exclude<ThemeMode, 'auto'>;
  setTheme: (theme: ThemeMode) => void;
}

/**
 * 提供主题相关的读取与切换，并在 DOM 节点写入 data-theme。
 */
export function useTheme(): UseThemeResult {
  const { theme, setTheme } = useUIStore((state) => ({
    theme: state.theme,
    setTheme: state.setTheme,
  }));

  const resolvedTheme = useMemo<Exclude<ThemeMode, 'auto'>>(() => {
    if (theme === 'auto') {
      return window.matchMedia(DARK_QUERY).matches ? 'dark' : 'light';
    }
    return theme;
  }, [theme]);

  const applyTheme = useCallback(
    (nextTheme: Exclude<ThemeMode, 'auto'>) => {
      document.documentElement.setAttribute('data-theme', nextTheme);
    },
    []
  );

  useEffect(() => {
    applyTheme(resolvedTheme);

    if (theme !== 'auto') {
      return;
    }

    const mediaQuery = window.matchMedia(DARK_QUERY);
    const handler = (event: MediaQueryListEvent): void => {
      applyTheme(event.matches ? 'dark' : 'light');
    };

    mediaQuery.addEventListener('change', handler);
    return () => {
      mediaQuery.removeEventListener('change', handler);
    };
  }, [applyTheme, theme, resolvedTheme]);

  return { theme, resolvedTheme, setTheme };
}
