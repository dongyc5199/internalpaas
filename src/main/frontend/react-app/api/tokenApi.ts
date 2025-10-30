/**
 * Token API Client
 *
 * API functions for token refresh and authentication operations.
 */

import type { TokenRefreshRequest, TokenRefreshResponse } from '../types/auth';
import { TOKEN_REFRESH_ENDPOINT } from '../config/constants';

/**
 * Refresh the current access token
 *
 * @param currentToken - The current access token to be refreshed
 * @returns Promise resolving to new token data
 * @throws Error if refresh fails (network error, auth error, etc.)
 */
export async function refreshToken(currentToken: string): Promise<TokenRefreshResponse> {
  const requestBody: TokenRefreshRequest = {
    currentToken,
  };

  try {
    const response = await fetch(TOKEN_REFRESH_ENDPOINT, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${currentToken}`,
      },
      body: JSON.stringify(requestBody),
      credentials: 'same-origin', // Include cookies for CSRF protection
    });

    if (!response.ok) {
      // Handle HTTP errors
      const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
      throw new Error(`Token refresh failed: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: TokenRefreshResponse = await response.json();

    // Validate response structure
    if (!data.accessToken || typeof data.expiresAt !== 'number') {
      throw new Error('Invalid token refresh response structure');
    }

    console.log('[tokenApi] Token refreshed successfully', {
      expiresAt: new Date(data.expiresAt).toISOString(),
    });

    return data;
  } catch (error) {
    console.error('[tokenApi] Token refresh error:', error);
    throw error;
  }
}
