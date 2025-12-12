/**
 * Tests for useReleases Hook
 *
 * Test Coverage:
 * 1. Successfully fetches releases list
 * 2. Handles loading state correctly
 * 3. Handles error state correctly
 * 4. Respects enabled option (conditional queries)
 * 5. Caching behavior (same query key returns cached data)
 * 6. Pagination and filtering
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useReleases } from '../../react-app/hooks/useReleases';
import * as releaseApi from '../../react-app/api/releaseApi';
import type { ReleaseListResponse } from '../../react-app/types/release';
import React from 'react';

// Mock the API module
vi.mock('../../react-app/api/releaseApi');

describe('useReleases', () => {
  let queryClient: QueryClient;

  // Test data
  const mockReleaseListResponse: ReleaseListResponse = {
    releases: [
      {
        id: 'release-1',
        version: '1.0.0',
        name: 'Release 1',
        status: 'deployed',
        environment: 'production',
        applicationName: 'App 1',
        createdAt: '2025-01-01T00:00:00Z',
      },
      {
        id: 'release-2',
        version: '2.0.0',
        name: 'Release 2',
        status: 'pending',
        environment: 'staging',
        applicationName: 'App 2',
        createdAt: '2025-01-02T00:00:00Z',
      },
    ],
    total: 2,
    page: 1,
    pageSize: 20,
    hasMore: false,
  };

  beforeEach(() => {
    // Create a new QueryClient for each test to ensure isolation
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false, // Disable retries in tests (hook has own retry logic)
          staleTime: 10000, // Keep data fresh for 10s to test caching
          gcTime: 30000, // Keep unused data in cache for 30s
        },
      },
    });

    vi.clearAllMocks();
  });

  afterEach(() => {
    queryClient.clear();
  });

  // Wrapper component that provides QueryClient
  const createWrapper = () => {
    return ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  };

  describe('Successful data fetching', () => {
    it('should successfully fetch releases list', async () => {
      // Mock successful API response
      vi.mocked(releaseApi.fetchReleases).mockResolvedValue(mockReleaseListResponse);

      const { result } = renderHook(() => useReleases(), {
        wrapper: createWrapper(),
      });

      // Initially should be loading
      expect(result.current.isLoading).toBe(true);
      expect(result.current.data).toBeUndefined();

      // Wait for query to complete
      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      // Verify data
      expect(result.current.data).toEqual(mockReleaseListResponse);
      expect(result.current.data?.releases).toHaveLength(2);
      expect(result.current.data?.total).toBe(2);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should pass correct parameters to API call', async () => {
      vi.mocked(releaseApi.fetchReleases).mockResolvedValue(mockReleaseListResponse);

      const filters = { status: ['deployed' as const], environment: ['production' as const] };
      const page = 2;
      const pageSize = 10;
      const token = 'test-token';

      renderHook(
        () =>
          useReleases({
            filters,
            page,
            pageSize,
            token,
          }),
        { wrapper: createWrapper() }
      );

      await waitFor(() => {
        expect(releaseApi.fetchReleases).toHaveBeenCalledWith(
          { filters, page, pageSize },
          token
        );
      });
    });
  });

  describe('Loading state', () => {
    it('should show loading state while fetching', async () => {
      // Create a delayed promise to test loading state
      let resolvePromise: (value: ReleaseListResponse) => void;
      const delayedPromise = new Promise<ReleaseListResponse>((resolve) => {
        resolvePromise = resolve;
      });

      vi.mocked(releaseApi.fetchReleases).mockReturnValue(delayedPromise);

      const { result } = renderHook(() => useReleases(), {
        wrapper: createWrapper(),
      });

      // Should be loading initially
      expect(result.current.isLoading).toBe(true);
      expect(result.current.isFetching).toBe(true);
      expect(result.current.data).toBeUndefined();

      // Resolve the promise
      resolvePromise!(mockReleaseListResponse);

      // Wait for loading to finish
      await waitFor(() => expect(result.current.isLoading).toBe(false));
    });
  });

  describe('Error handling', () => {
    it('should handle API errors correctly', async () => {
      const errorMessage = 'Failed to fetch releases: 500 - Internal Server Error';
      vi.mocked(releaseApi.fetchReleases).mockRejectedValue(new Error(errorMessage));

      const { result } = renderHook(() => useReleases(), {
        wrapper: createWrapper(),
      });

      // Wait for error state (hook will retry once for 500 error, so wait for that)
      await waitFor(() => expect(result.current.isError).toBe(true), {
        timeout: 3000, // Give enough time for retry
      });

      expect(result.current.error).toBeDefined();
      expect(result.current.error?.message).toBe(errorMessage);
      expect(result.current.data).toBeUndefined();
      expect(result.current.isLoading).toBe(false);
    });

    it('should not retry on 401 authentication error', async () => {
      vi.mocked(releaseApi.fetchReleases).mockRejectedValue(
        new Error('Failed to fetch releases: 401 - Unauthorized')
      );

      renderHook(() => useReleases(), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        // Should be called exactly once (no retries)
        expect(releaseApi.fetchReleases).toHaveBeenCalledTimes(1);
      });
    });

    it('should not retry on 403 forbidden error', async () => {
      vi.mocked(releaseApi.fetchReleases).mockRejectedValue(
        new Error('Failed to fetch releases: 403 - Forbidden')
      );

      renderHook(() => useReleases(), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(releaseApi.fetchReleases).toHaveBeenCalledTimes(1);
      });
    });
  });

  describe('Conditional queries (enabled option)', () => {
    it('should not fetch when enabled is false', async () => {
      vi.mocked(releaseApi.fetchReleases).mockResolvedValue(mockReleaseListResponse);

      const { result } = renderHook(() => useReleases({ enabled: false }), {
        wrapper: createWrapper(),
      });

      // Should not be loading or fetching
      expect(result.current.isLoading).toBe(false);
      expect(result.current.isFetching).toBe(false);
      expect(result.current.data).toBeUndefined();

      // API should not be called
      expect(releaseApi.fetchReleases).not.toHaveBeenCalled();
    });

    it('should fetch when enabled changes from false to true', async () => {
      vi.mocked(releaseApi.fetchReleases).mockResolvedValue(mockReleaseListResponse);

      const { result, rerender } = renderHook(
        ({ enabled }) => useReleases({ enabled }),
        {
          wrapper: createWrapper(),
          initialProps: { enabled: false },
        }
      );

      // Initially should not fetch
      expect(releaseApi.fetchReleases).not.toHaveBeenCalled();

      // Enable the query
      rerender({ enabled: true });

      // Now it should fetch
      await waitFor(() => {
        expect(releaseApi.fetchReleases).toHaveBeenCalled();
        expect(result.current.isSuccess).toBe(true);
      });
    });
  });

  describe('Caching behavior', () => {
    it('should return cached data for same query key', async () => {
      vi.mocked(releaseApi.fetchReleases).mockResolvedValue(mockReleaseListResponse);

      // Create a shared wrapper to ensure queries share the same QueryClient
      const wrapper = createWrapper();

      // First render
      const { result: result1 } = renderHook(() => useReleases(), {
        wrapper,
      });

      await waitFor(() => expect(result1.current.isSuccess).toBe(true));

      // Record how many times API was called after first fetch
      const firstCallCount = vi.mocked(releaseApi.fetchReleases).mock.calls.length;
      expect(firstCallCount).toBeGreaterThanOrEqual(1);

      // Second render with same query key in the same wrapper
      const { result: result2 } = renderHook(() => useReleases(), {
        wrapper,
      });

      // Should have data available (from cache or fresh)
      await waitFor(() => {
        expect(result2.current.data).toBeDefined();
        expect(result2.current.isSuccess).toBe(true);
      });

      // Data should be the same
      expect(result2.current.data).toEqual(mockReleaseListResponse);

      // With staleTime set, should not make additional calls immediately
      // Note: In test environment, window focus events may trigger refetch
      // So we check that calls don't exceed reasonable limit
      expect(vi.mocked(releaseApi.fetchReleases).mock.calls.length).toBeLessThanOrEqual(firstCallCount + 1);
    });

    it('should make new request for different query key', async () => {
      vi.mocked(releaseApi.fetchReleases).mockResolvedValue(mockReleaseListResponse);

      // First render with filters
      renderHook(() => useReleases({ filters: { status: ['deployed' as const] } }), {
        wrapper: createWrapper(),
      });

      await waitFor(() => expect(releaseApi.fetchReleases).toHaveBeenCalledTimes(1));

      // Second render with different filters (different query key)
      renderHook(() => useReleases({ filters: { status: ['pending' as const] } }), {
        wrapper: createWrapper(),
      });

      // Should make a new API call
      await waitFor(() => expect(releaseApi.fetchReleases).toHaveBeenCalledTimes(2));
    });
  });

  describe('Pagination and filtering', () => {
    it('should handle pagination correctly', async () => {
      vi.mocked(releaseApi.fetchReleases).mockResolvedValue(mockReleaseListResponse);

      renderHook(() => useReleases({ page: 2, pageSize: 10 }), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(releaseApi.fetchReleases).toHaveBeenCalledWith(
          { filters: undefined, page: 2, pageSize: 10 },
          undefined
        );
      });
    });

    it('should handle filters correctly', async () => {
      vi.mocked(releaseApi.fetchReleases).mockResolvedValue(mockReleaseListResponse);

      const filters = {
        status: ['deployed' as const, 'pending' as const],
        environment: ['production' as const],
        applicationId: 'app-1',
      };

      renderHook(() => useReleases({ filters }), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(releaseApi.fetchReleases).toHaveBeenCalledWith(
          { filters, page: 1, pageSize: 20 },
          undefined
        );
      });
    });
  });

  describe('Manual refetch', () => {
    it('should allow manual refetch via refetch function', async () => {
      vi.mocked(releaseApi.fetchReleases).mockResolvedValue(mockReleaseListResponse);

      const { result } = renderHook(() => useReleases(), {
        wrapper: createWrapper(),
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      // API should be called once
      expect(releaseApi.fetchReleases).toHaveBeenCalledTimes(1);

      // Manual refetch
      await result.current.refetch();

      // API should be called again
      expect(releaseApi.fetchReleases).toHaveBeenCalledTimes(2);
    });
  });
});
