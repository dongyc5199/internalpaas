import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { STORAGE_KEYS } from '../constants';

export type ThemeMode = 'light' | 'dark' | 'auto';
export type LanguageCode = 'en' | 'zh';

export interface UIToast {
  id: string;
  message: string;
  type?: 'info' | 'success' | 'warning' | 'error';
  duration?: number;
}

export interface UIState {
  theme: ThemeMode;
  language: LanguageCode;
  sidebarCollapsed: boolean;
  activeModal: string | null;
  modalProps?: Record<string, unknown>;
  toasts: UIToast[];

  setTheme: (theme: ThemeMode) => void;
  setLanguage: (language: LanguageCode) => void;
  setSidebarCollapsed: (collapsed: boolean) => void;
  openModal: (id: string, props?: Record<string, unknown>) => void;
  closeModal: () => void;
  enqueueToast: (toast: UIToast | Omit<UIToast, 'id'>) => void;
  removeToast: (id: string) => void;
  reset: () => void;
}

const getInitialLanguage = (): LanguageCode => {
  if (typeof window !== 'undefined') {
    const stored = localStorage.getItem('i18nextLng');
    if (stored === 'en' || stored === 'zh') return stored;
  }
  return 'zh';
};

const defaultState: Pick<UIState, 'theme' | 'language' | 'sidebarCollapsed' | 'activeModal' | 'toasts'> =
  {
    theme: 'auto',
    language: getInitialLanguage(),
    sidebarCollapsed: false,
    activeModal: null,
    toasts: [],
  };

export const useUIStore = create<UIState>()(
  persist(
    (set) => ({
      ...defaultState,
      setTheme: (theme) => set({ theme }),
      setSidebarCollapsed: (collapsed) => set({ sidebarCollapsed: collapsed }),
      setLanguage: (language) => set({ language }),
      openModal: (id, props) => set({ activeModal: id, modalProps: props }),
      closeModal: () => set({ activeModal: null, modalProps: undefined }),
      enqueueToast: (toastInput) =>
        set((state) => {
          const toast =
            'id' in toastInput
              ? toastInput
              : { ...toastInput, id: `${Date.now()}-${Math.random()}` };
          return { toasts: [...state.toasts, toast] };
        }),
      removeToast: (id) =>
        set((state) => ({ toasts: state.toasts.filter((toast) => toast.id !== id) })),
      reset: () => set({ ...defaultState }),
    }),
    {
      name: STORAGE_KEYS.UI_STATE,
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        theme: state.theme,
        language: state.language,
        sidebarCollapsed: state.sidebarCollapsed,
      }),
    }
  )
);
