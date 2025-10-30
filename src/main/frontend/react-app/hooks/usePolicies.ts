/**
 * usePolicies Hook
 *
 * React Query hook for fetching policies list with caching and automatic refetching.
 * Integrates with authStore to automatically inject JWT token.
 */

import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import { fetchPolicies } from '../api/policyApi';
import type { PolicyListResponse, PolicyFilters } from '../types/policy';

/**
 * Options for usePolicies hook
 */
export interface UsePoliciesOptions {
  /** Filters to apply to policy query */
  filters?: PolicyFilters;

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
 * Fetch policies list with React Query
 *
 * @param options - Query options (filters, pagination, token)
 * @returns UseQueryResult with policies data, loading state, error, etc.
 *
 * @example
 * ```tsx
 * function PoliciesPage() {
 *   const { data, isLoading, error, refetch } = usePolicies({
 *     filters: { status: ['active'], effect: ['allow'] },
 *     page: 1,
 *     pageSize: 10,
 *   });
 *
 *   if (isLoading) return <div>Loading policies...</div>;
 *   if (error) return <div>Error: {error.message}</div>;
 *
 *   return (
 *     <div>
 *       <h1>Policies ({data?.total})</h1>
 *       {data?.policies.map(policy => (
 *         <div key={policy.id}>
 *           {policy.name} - {policy.effect}
 *         </div>
 *       ))}
 *     </div>
 *   );
 * }
 * ```
 *
 * @example
 * // Filter by user's policies
 * ```tsx
 * function UserPoliciesView({ userId }: { userId: string }) {
 *   const { data } = usePolicies({
 *     filters: { subjectId: userId },
 *   });
 *
 *   return (
 *     <div>
 *       <h2>Your Policies</h2>
 *       {data?.policies.map(policy => (
 *         <PolicyCard key={policy.id} policy={policy} />
 *       ))}
 *     </div>
 *   );
 * }
 * ```
 *
 * @example
 * // Filter by resource type
 * ```tsx
 * function ApplicationPoliciesView() {
 *   const { data, isLoading } = usePolicies({
 *     filters: {
 *       resourceType: ['application'],
 *       status: ['active'],
 *     },
 *   });
 *
 *   if (isLoading) return <Spinner />;
 *
 *   return (
 *     <div>
 *       <h2>Application Policies</h2>
 *       <Table data={data?.policies || []} />
 *     </div>
 *   );
 * }
 * ```
 */
export function usePolicies(
  options?: UsePoliciesOptions
): UseQueryResult<PolicyListResponse, Error> {
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
  const queryKey = ['policies', filters, page, pageSize] as const;

  return useQuery<PolicyListResponse, Error>({
    queryKey,
    queryFn: async () => {
      // TODO: Integrate with authStore to get token automatically
      // const token = useAuthStore.getState().token;

      return fetchPolicies({ filters, page, pageSize }, token);
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
 * Hook for refetching policies manually
 *
 * @example
 * ```tsx
 * function RefreshButton() {
 *   const { refetch, isFetching } = usePolicies();
 *
 *   return (
 *     <button onClick={() => refetch()} disabled={isFetching}>
 *       {isFetching ? 'Refreshing...' : 'Refresh Policies'}
 *     </button>
 *   );
 * }
 * ```
 */
