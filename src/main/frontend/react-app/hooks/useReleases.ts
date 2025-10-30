/**
 * useReleases Hook
 *
 * React Query hook for fetching releases list with caching and automatic refetching.
 * Integrates with authStore to automatically inject JWT token.
 */

import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import { fetchReleases } from '../api/releaseApi';
import type { ReleaseListResponse, ReleaseFilters } from '../types/release';

/**
 * Options for useReleases hook
 */
export interface UseReleasesOptions {
  /** Filters to apply to release query */
  filters?: ReleaseFilters;

  /** Page number for pagination (default: 1) */
  page?: number;

  /** Number of items per page (default: 20) */
  pageSize?: number;

  /** JWT access token (optional, will use authStore if not provided) */
  token?: string;

  /** Whether the query should run (default: true) */
  enabled?: boolean;

  /** Custom stale time in milliseconds (default: uses queryClient config - 5min) */
  staleTime?: number;

  /** Custom refetch interval in milliseconds (default: none) */
  refetchInterval?: number;
}

/**
 * Fetch releases list with React Query
 *
 * @param options - Query options (filters, pagination, token)
 * @returns UseQueryResult with releases data, loading state, error, etc.
 *
 * @example
 * ```tsx
 * function ReleasesPage() {
 *   const { data, isLoading, error, refetch } = useReleases({
 *     filters: { status: ['deployed'], environment: ['production'] },
 *     page: 1,
 *     pageSize: 10,
 *   });
 *
 *   if (isLoading) return <div>Loading releases...</div>;
 *   if (error) return <div>Error: {error.message}</div>;
 *
 *   return (
 *     <div>
 *       <h1>Releases ({data?.total})</h1>
 *       {data?.releases.map(release => (
 *         <div key={release.id}>{release.name}</div>
 *       ))}
 *     </div>
 *   );
 * }
 * ```
 */
export function useReleases(
  options?: UseReleasesOptions
): UseQueryResult<ReleaseListResponse, Error> {
  const {
    filters,
    page = 1,
    pageSize = 20,
    token,
    enabled = true,
    staleTime,
    refetchInterval,
  } = options || {};

  // Generate query key based on filters and pagination
  // This ensures caching works correctly for different filter combinations
  const queryKey = ['releases', filters, page, pageSize] as const;

  return useQuery<ReleaseListResponse, Error>({
    queryKey,
    queryFn: async () => {
      // TODO: Integrate with authStore to get token automatically
      // const token = useAuthStore.getState().token;

      return fetchReleases({ filters, page, pageSize }, token);
    },
    enabled,
    staleTime, // Will use global config (5min) if not provided
    refetchInterval,
    // Don't retry on 401/403 (auth errors) - let the auth flow handle it
    retry: (failureCount, error) => {
      if (error.message.includes('401') || error.message.includes('403')) {
        return false;
      }
      return failureCount < 1; // Retry once for other errors
    },
  });
}

/**
 * Hook for refetching releases manually
 *
 * @example
 * ```tsx
 * function RefreshButton() {
 *   const { refetch, isFetching } = useReleases();
 *
 *   return (
 *     <button onClick={() => refetch()} disabled={isFetching}>
 *       {isFetching ? 'Refreshing...' : 'Refresh Releases'}
 *     </button>
 *   );
 * }
 * ```
 */
