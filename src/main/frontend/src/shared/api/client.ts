/**
 * API Client instance
 * Centralized HTTP client for all API requests
 */

import { createApiClient } from './fetcher';

export const apiClient = createApiClient();

// Re-export types
export type { ApiClient, Fetcher, HttpMethod } from './fetcher';
