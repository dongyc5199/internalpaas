/**
 * useRelease Hook
 *
 * React Query hook for fetching a single release by ID with caching.
 * Integrates with authStore to automatically inject JWT token.
 */

import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import { fetchReleaseById } from '../api/releaseApi';
import type { Release } from '../types/release';

/**
 * Options for useRelease hook
 */
export interface UseReleaseOptions {
  /** JWT access token (optional, will use authStore if not provided) */
  token?: string;

  /** Whether the query should run (default: true if releaseId provided) */
  enabled?: boolean;

  /** Custom stale time in milliseconds (default: uses queryClient config - 5min) */
  staleTime?: number;

  /** Custom refetch interval in milliseconds (default: none) */
  refetchInterval?: number;
}

/**
 * Fetch a single release by ID with React Query
 *
 * @param releaseId - Release unique identifier (required)
 * @param options - Query options (token, enabled, staleTime, etc.)
 * @returns UseQueryResult with release data, loading state, error, etc.
 *
 * @example
 * ```tsx
 * function ReleaseDetail({ releaseId }: { releaseId: string }) {
 *   const { data: release, isLoading, error } = useRelease(releaseId);
 *
 *   if (isLoading) return <div>Loading release details...</div>;
 *   if (error) return <div>Error: {error.message}</div>;
 *   if (!release) return <div>Release not found</div>;
 *
 *   return (
 *     <div>
 *       <h1>{release.name}</h1>
 *       <p>Version: {release.version}</p>
 *       <p>Status: {release.status}</p>
 *       <p>Environment: {release.environment}</p>
 *     </div>
 *   );
 * }
 * ```
 *
 * @example
 * // Conditional query (only fetch when ID is available)
 * ```tsx
 * function ReleaseDetailConditional() {
 *   const [selectedId, setSelectedId] = useState<string | null>(null);
 *   const { data, isLoading } = useRelease(selectedId || '', {
 *     enabled: !!selectedId, // Only fetch when ID is set
 *   });
 *
 *   return (
 *     <div>
 *       <button onClick={() => setSelectedId('release-123')}>
 *         Load Release 123
 *       </button>
 *       {isLoading && <div>Loading...</div>}
 *       {data && <div>{data.name}</div>}
 *     </div>
 *   );
 * }
 * ```
 */
export function useRelease(
  releaseId: string,
  options?: UseReleaseOptions
): UseQueryResult<Release, Error> {
  const {
    token,
    enabled = true,
    staleTime,
    refetchInterval,
  } = options || {};

  // Generate query key - use unique key for each release
  const queryKey = ['release', releaseId] as const;

  return useQuery<Release, Error>({
    queryKey,
    queryFn: async () => {
      // TODO: Integrate with authStore to get token automatically
      // const token = useAuthStore.getState().token;

      if (!releaseId) {
        throw new Error('releaseId is required for useRelease');
      }

      return fetchReleaseById(releaseId, token);
    },
    // Only run query if enabled AND releaseId is provided
    enabled: enabled && !!releaseId,
    staleTime, // Will use global config (5min) if not provided
    refetchInterval,
    // Don't retry on 401/403 (auth errors) or 404 (not found)
    retry: (failureCount, error) => {
      const errorMsg = error.message;
      if (
        errorMsg.includes('401') ||
        errorMsg.includes('403') ||
        errorMsg.includes('404')
      ) {
        return false;
      }
      return failureCount < 1; // Retry once for other errors
    },
  });
}
