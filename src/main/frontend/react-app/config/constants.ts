/**
 * Application Constants
 *
 * Centralized configuration values used across the React application.
 */

/**
 * Token refresh timing (in milliseconds)
 *
 * Token refresh is triggered when token expiration is within this window.
 * Default: 5 minutes (300,000 ms)
 *
 * Example:
 * - If token expires at 12:00 PM
 * - Refresh will be triggered at 11:55 AM
 */
export const TOKEN_REFRESH_BEFORE_MS = 5 * 60 * 1000; // 5 minutes

/**
 * BroadcastChannel name for cross-tab token synchronization
 *
 * All tabs listening to this channel will receive token refresh events.
 */
export const AUTH_CHANNEL_NAME = 'auth-token-refresh';

/**
 * LocalStorage key for storing language preference
 */
export const LANGUAGE_STORAGE_KEY = 'language';

/**
 * Default language fallback
 */
export const DEFAULT_LANGUAGE = 'zh-CN';

/**
 * Supported languages
 */
export const SUPPORTED_LANGUAGES = ['zh-CN', 'en-US'] as const;

/**
 * API base URL (derived from current origin in production)
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

/**
 * Token API endpoints
 */
export const TOKEN_REFRESH_ENDPOINT = '/api/deploy-platform/token/refresh';

/**
 * Component default sizes
 */
export const DEFAULT_COMPONENT_SIZE = 'medium' as const;

/**
 * Storybook build output directory
 */
export const STORYBOOK_OUTPUT_DIR = 'storybook-static';
