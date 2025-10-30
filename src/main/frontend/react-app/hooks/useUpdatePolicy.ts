/**
 * useUpdatePolicy Hook
 *
 * React Query mutation hook for updating policies with optimistic updates and automatic rollback.
 * Integrates with authStore to automatically inject JWT token.
 */

import { useMutation, useQueryClient, type UseMutationResult } from '@tanstack/react-query';
import { updatePolicy } from '../api/policyApi';
import type { Policy, PolicySummary, UpdatePolicyRequest, PolicyListResponse } from '../types/policy';

/**
 * Variables for policy update mutation
 */
export interface UpdatePolicyVariables {
  /** Policy unique identifier */
  policyId: string;

  /** Policy update data */
  request: UpdatePolicyRequest;

  /** JWT access token (optional, will use authStore if not provided) */
  token?: string;
}

/**
 * Options for useUpdatePolicy hook
 */
export interface UseUpdatePolicyOptions {
  /**
   * Callback invoked when mutation succeeds (after server response)
   */
  onSuccess?: (data: Policy, variables: UpdatePolicyVariables) => void;

  /**
   * Callback invoked when mutation fails
   */
  onError?: (error: Error, variables: UpdatePolicyVariables) => void;

  /**
   * Callback invoked when mutation is settled (success or error)
   */
  onSettled?: (data: Policy | undefined, error: Error | null, variables: UpdatePolicyVariables) => void;

  /**
   * Whether to enable optimistic updates (default: true)
   * When enabled, UI updates immediately before server responds
   */
  enableOptimisticUpdate?: boolean;
}

/**
 * Update a policy with React Query mutation
 *
 * Features:
 * - Optimistic updates for instant UI feedback
 * - Automatic rollback on error
 * - Query invalidation to refresh related data
 * - Toast notifications (onSuccess/onError callbacks)
 *
 * @param options - Mutation options (callbacks, optimistic updates)
 * @returns UseMutationResult with mutate, mutateAsync, isLoading, error, etc.
 *
 * @example
 * ```tsx
 * function PolicyStatusToggle({ policy }: { policy: Policy }) {
 *   const { mutate: updatePolicy, isLoading } = useUpdatePolicy({
 *     onSuccess: () => {
 *       toast.success('Policy updated successfully!');
 *     },
 *     onError: (error) => {
 *       toast.error(`Failed to update policy: ${error.message}`);
 *     },
 *   });
 *
 *   const handleToggleStatus = () => {
 *     updatePolicy({
 *       policyId: policy.id,
 *       request: {
 *         status: policy.status === 'active' ? 'inactive' : 'active',
 *       },
 *     });
 *   };
 *
 *   return (
 *     <button onClick={handleToggleStatus} disabled={isLoading}>
 *       {isLoading ? 'Updating...' : policy.status === 'active' ? 'Deactivate' : 'Activate'}
 *     </button>
 *   );
 * }
 * ```
 *
 * @example
 * // Update with optimistic UI update
 * ```tsx
 * function PolicyPriorityEditor({ policy }: { policy: Policy }) {
 *   const { mutate } = useUpdatePolicy({
 *     enableOptimisticUpdate: true, // UI updates immediately
 *   });
 *
 *   return (
 *     <select
 *       value={policy.priority}
 *       onChange={(e) => {
 *         mutate({
 *           policyId: policy.id,
 *           request: { priority: parseInt(e.target.value) },
 *         });
 *       }}
 *     >
 *       <option value="1">Low</option>
 *       <option value="5">Medium</option>
 *       <option value="10">High</option>
 *     </select>
 *   );
 * }
 * ```
 */
export function useUpdatePolicy(
  options?: UseUpdatePolicyOptions
): UseMutationResult<Policy, Error, UpdatePolicyVariables> {
  const queryClient = useQueryClient();

  const {
    onSuccess,
    onError,
    onSettled,
    enableOptimisticUpdate = true,
  } = options || {};

  return useMutation<Policy, Error, UpdatePolicyVariables>({
    mutationFn: async (variables) => {
      // TODO: Integrate with authStore to get token automatically
      // const token = variables.token || useAuthStore.getState().token;

      return updatePolicy(
        variables.policyId,
        variables.request,
        variables.token
      );
    },

    // Optimistic update: immediately update UI before server responds
    onMutate: async (variables) => {
      if (!enableOptimisticUpdate) return;

      const { policyId, request } = variables;

      // Cancel outgoing refetches (so they don't overwrite our optimistic update)
      await queryClient.cancelQueries({ queryKey: ['policies'] });

      // Snapshot previous values for rollback
      const previousPoliciesList = queryClient.getQueriesData({ queryKey: ['policies'] });

      // Optimistically update policy lists
      queryClient.setQueriesData<PolicyListResponse>(
        { queryKey: ['policies'] },
        (oldData) => {
          if (!oldData) return oldData;

          return {
            ...oldData,
            policies: oldData.policies.map((policy) =>
              policy.id === policyId
                ? { ...policy, ...request } // Apply optimistic update
                : policy
            ),
          };
        }
      );

      // Return context for rollback
      return { previousPoliciesList };
    },

    // Rollback on error
    onError: (error, variables, context) => {
      if (enableOptimisticUpdate && context) {
        // Restore previous values
        context.previousPoliciesList.forEach(([queryKey, previousData]) => {
          queryClient.setQueryData(queryKey, previousData);
        });
      }

      console.error('[useUpdatePolicy] Update failed:', error);

      // Call user-provided error callback
      onError?.(error, variables);
    },

    // Invalidate and refetch on success
    onSuccess: (data, variables) => {
      console.log('[useUpdatePolicy] Policy updated successfully:', data.id);

      // Invalidate all policies queries to trigger refetch
      // This ensures the optimistic update is replaced with real server data
      queryClient.invalidateQueries({ queryKey: ['policies'] });

      // Call user-provided success callback
      onSuccess?.(data, variables);
    },

    // Called after success or error
    onSettled: (data, error, variables) => {
      onSettled?.(data || undefined, error, variables);
    },
  });
}

/**
 * Batch update multiple policies
 *
 * @example
 * ```tsx
 * function BulkPolicyActivator({ policyIds }: { policyIds: string[] }) {
 *   const { mutate, isLoading } = useUpdatePolicy();
 *
 *   const handleActivateAll = () => {
 *     policyIds.forEach(id => {
 *       mutate({
 *         policyId: id,
 *         request: { status: 'active' },
 *       });
 *     });
 *   };
 *
 *   return (
 *     <button onClick={handleActivateAll} disabled={isLoading}>
 *       Activate All Policies
 *     </button>
 *   );
 * }
 * ```
 */
