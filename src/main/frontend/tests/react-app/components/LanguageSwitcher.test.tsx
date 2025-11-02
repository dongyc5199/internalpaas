/**
 * LanguageSwitcher Component Tests
 *
 * Tests language switching functionality and localStorage persistence.
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { I18nextProvider } from 'react-i18next';
import i18n from '../../../react-app/i18n/i18n';
import { LanguageSwitcher } from '../../../react-app/components/LanguageSwitcher/LanguageSwitcher';

describe('LanguageSwitcher', () => {
  // Reset i18n and localStorage before each test
  beforeEach(() => {
    localStorage.clear();
    i18n.changeLanguage('zh-CN');
  });

  it('renders language switcher with current language', () => {
    render(
      <I18nextProvider i18n={i18n}>
        <LanguageSwitcher />
      </I18nextProvider>
    );

    const select = screen.getByRole('combobox');
    expect(select).toBeInTheDocument();
    expect(select).toHaveValue('zh-CN');
  });

  it('displays all supported languages in dropdown', () => {
    render(
      <I18nextProvider i18n={i18n}>
        <LanguageSwitcher />
      </I18nextProvider>
    );

    const options = screen.getAllByRole('option');
    expect(options).toHaveLength(2);
    expect(options[0]).toHaveValue('zh-CN');
    expect(options[1]).toHaveValue('en-US');
  });

  it('changes language when user selects a different option', async () => {
    render(
      <I18nextProvider i18n={i18n}>
        <LanguageSwitcher />
      </I18nextProvider>
    );

    const select = screen.getByRole('combobox');

    // Change to English
    fireEvent.change(select, { target: { value: 'en-US' } });

    await waitFor(() => {
      expect(i18n.language).toBe('en-US');
      expect(select).toHaveValue('en-US');
    });
  });

  it('persists language preference to localStorage', async () => {
    const setItemSpy = vi.spyOn(Storage.prototype, 'setItem');

    render(
      <I18nextProvider i18n={i18n}>
        <LanguageSwitcher />
      </I18nextProvider>
    );

    const select = screen.getByRole('combobox');

    // Change to English
    fireEvent.change(select, { target: { value: 'en-US' } });

    await waitFor(() => {
      expect(setItemSpy).toHaveBeenCalledWith('language', 'en-US');
    });

    setItemSpy.mockRestore();
  });

  it('loads language preference from localStorage on mount', () => {
    localStorage.setItem('language', 'en-US');

    // Re-initialize i18n to pick up localStorage value
    i18n.changeLanguage(localStorage.getItem('language') || 'zh-CN');

    render(
      <I18nextProvider i18n={i18n}>
        <LanguageSwitcher />
      </I18nextProvider>
    );

    const select = screen.getByRole('combobox');
    expect(select).toHaveValue('en-US');
  });

  it('renders with custom className', () => {
    const { container } = render(
      <I18nextProvider i18n={i18n}>
        <LanguageSwitcher className="custom-class" />
      </I18nextProvider>
    );

    const switcher = container.querySelector('.custom-class');
    expect(switcher).toBeInTheDocument();
  });

  it('updates UI when language changes externally', async () => {
    render(
      <I18nextProvider i18n={i18n}>
        <LanguageSwitcher />
      </I18nextProvider>
    );

    const select = screen.getByRole('combobox');
    expect(select).toHaveValue('zh-CN');

    // Change language programmatically (e.g., from another component)
    await i18n.changeLanguage('en-US');

    await waitFor(() => {
      expect(select).toHaveValue('en-US');
    });
  });

  it('has accessible label for screen readers', () => {
    render(
      <I18nextProvider i18n={i18n}>
        <LanguageSwitcher />
      </I18nextProvider>
    );

    const select = screen.getByRole('combobox');
    expect(select).toHaveAccessibleName();
  });
});
