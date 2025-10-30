/**
 * Policy API Client
 *
 * API functions for policy management operations (CRUD operations for access control policies).
 */

import type {
  Policy,
  PolicySummary,
  PolicyListResponse,
  CreatePolicyRequest,
  UpdatePolicyRequest,
  PolicyFilters,
  PolicyEvaluationRequest,
  PolicyEvaluationResult,
  UserPermissions,
} from '../types/policy';

/** Base API endpoint for policy operations */
const POLICY_API_BASE = '/api/deploy-platform/policies';

/**
 * Build query string from filter object
 */
function buildQueryString(filters?: PolicyFilters): string {
  if (!filters) return '';

  const params = new URLSearchParams();

  if (filters.status) {
    filters.status.forEach(s => params.append('status', s));
  }
  if (filters.effect) {
    filters.effect.forEach(e => params.append('effect', e));
  }
  if (filters.subjectType) {
    filters.subjectType.forEach(st => params.append('subjectType', st));
  }
  if (filters.subjectId) {
    params.append('subjectId', filters.subjectId);
  }
  if (filters.resourceType) {
    filters.resourceType.forEach(rt => params.append('resourceType', rt));
  }
  if (filters.resourceId) {
    params.append('resourceId', filters.resourceId);
  }
  if (filters.actions) {
    filters.actions.forEach(a => params.append('action', a));
  }
  if (filters.tags) {
    filters.tags.forEach(t => params.append('tag', t));
  }
  if (filters.includeExpired !== undefined) {
    params.append('includeExpired', String(filters.includeExpired));
  }

  const queryString = params.toString();
  return queryString ? `?${queryString}` : '';
}

/**
 * Fetch all policies with optional filters and pagination
 *
 * @param options - Query options (filters, pagination)
 * @param token - JWT access token for authorization
 * @returns Promise resolving to paginated policy list
 */
export async function fetchPolicies(
  options?: {
    filters?: PolicyFilters;
    page?: number;
    pageSize?: number;
  },
  token?: string
): Promise<PolicyListResponse> {
  const { filters, page = 1, pageSize = 20 } = options || {};

  const filterQuery = buildQueryString(filters);
  const paginationQuery = `${filterQuery ? '&' : '?'}page=${page}&pageSize=${pageSize}`;
  const url = `${POLICY_API_BASE}${filterQuery}${paginationQuery}`;

  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
      credentials: 'same-origin',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
      throw new Error(`Failed to fetch policies: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: PolicyListResponse = await response.json();

    // Validate response structure
    if (!Array.isArray(data.policies)) {
      throw new Error('Invalid policies response structure: missing policies array');
    }

    return data;
  } catch (error) {
    console.error('[policyApi] fetchPolicies error:', error);
    throw error;
  }
}

/**
 * Fetch a single policy by ID
 *
 * @param policyId - Policy unique identifier
 * @param token - JWT access token for authorization
 * @returns Promise resolving to policy details
 */
export async function fetchPolicyById(
  policyId: string,
  token?: string
): Promise<Policy> {
  const url = `${POLICY_API_BASE}/${policyId}`;

  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
      credentials: 'same-origin',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
      throw new Error(`Failed to fetch policy: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: Policy = await response.json();

    // Validate response structure
    if (!data.id || !data.name) {
      throw new Error('Invalid policy response structure');
    }

    return data;
  } catch (error) {
    console.error('[policyApi] fetchPolicyById error:', error);
    throw error;
  }
}

/**
 * Create a new policy
 *
 * @param request - Policy creation data
 * @param token - JWT access token for authorization
 * @returns Promise resolving to created policy
 */
export async function createPolicy(
  request: CreatePolicyRequest,
  token?: string
): Promise<Policy> {
  const url = POLICY_API_BASE;

  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
      body: JSON.stringify(request),
      credentials: 'same-origin',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
      throw new Error(`Failed to create policy: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: Policy = await response.json();

    console.log('[policyApi] Policy created successfully:', data.id);
    return data;
  } catch (error) {
    console.error('[policyApi] createPolicy error:', error);
    throw error;
  }
}

/**
 * Update an existing policy
 *
 * @param policyId - Policy unique identifier
 * @param request - Policy update data
 * @param token - JWT access token for authorization
 * @returns Promise resolving to updated policy
 */
export async function updatePolicy(
  policyId: string,
  request: UpdatePolicyRequest,
  token?: string
): Promise<Policy> {
  const url = `${POLICY_API_BASE}/${policyId}`;

  try {
    const response = await fetch(url, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
      body: JSON.stringify(request),
      credentials: 'same-origin',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
      throw new Error(`Failed to update policy: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: Policy = await response.json();

    console.log('[policyApi] Policy updated successfully:', policyId);
    return data;
  } catch (error) {
    console.error('[policyApi] updatePolicy error:', error);
    throw error;
  }
}

/**
 * Delete a policy
 *
 * @param policyId - Policy unique identifier
 * @param token - JWT access token for authorization
 * @returns Promise resolving when deletion succeeds
 */
export async function deletePolicy(
  policyId: string,
  token?: string
): Promise<void> {
  const url = `${POLICY_API_BASE}/${policyId}`;

  try {
    const response = await fetch(url, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
      credentials: 'same-origin',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
      throw new Error(`Failed to delete policy: ${response.status} - ${errorData.message || response.statusText}`);
    }

    console.log('[policyApi] Policy deleted successfully:', policyId);
  } catch (error) {
    console.error('[policyApi] deletePolicy error:', error);
    throw error;
  }
}

/**
 * Evaluate a policy for a specific user and action
 *
 * @param request - Policy evaluation request
 * @param token - JWT access token for authorization
 * @returns Promise resolving to evaluation result (allowed/denied)
 */
export async function evaluatePolicy(
  request: PolicyEvaluationRequest,
  token?: string
): Promise<PolicyEvaluationResult> {
  const url = `${POLICY_API_BASE}/evaluate`;

  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
      body: JSON.stringify(request),
      credentials: 'same-origin',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
      throw new Error(`Failed to evaluate policy: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: PolicyEvaluationResult = await response.json();

    console.log('[policyApi] Policy evaluated:', {
      allowed: data.allowed,
      userId: request.userId,
      action: request.action,
    });

    return data;
  } catch (error) {
    console.error('[policyApi] evaluatePolicy error:', error);
    throw error;
  }
}

/**
 * Fetch effective permissions for a user
 *
 * @param userId - User unique identifier
 * @param token - JWT access token for authorization
 * @returns Promise resolving to user's effective permissions
 */
export async function fetchUserPermissions(
  userId: string,
  token?: string
): Promise<UserPermissions> {
  const url = `${POLICY_API_BASE}/users/${userId}/permissions`;

  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
      credentials: 'same-origin',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
      throw new Error(`Failed to fetch user permissions: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: UserPermissions = await response.json();

    console.log('[policyApi] User permissions fetched:', userId);
    return data;
  } catch (error) {
    console.error('[policyApi] fetchUserPermissions error:', error);
    throw error;
  }
}

/**
 * Activate a policy (change status to 'active')
 *
 * @param policyId - Policy unique identifier
 * @param token - JWT access token for authorization
 * @returns Promise resolving to updated policy
 */
export async function activatePolicy(
  policyId: string,
  token?: string
): Promise<Policy> {
  const url = `${POLICY_API_BASE}/${policyId}/activate`;

  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
      credentials: 'same-origin',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
      throw new Error(`Failed to activate policy: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: Policy = await response.json();

    console.log('[policyApi] Policy activated successfully:', policyId);
    return data;
  } catch (error) {
    console.error('[policyApi] activatePolicy error:', error);
    throw error;
  }
}

/**
 * Deactivate a policy (change status to 'inactive')
 *
 * @param policyId - Policy unique identifier
 * @param token - JWT access token for authorization
 * @returns Promise resolving to updated policy
 */
export async function deactivatePolicy(
  policyId: string,
  token?: string
): Promise<Policy> {
  const url = `${POLICY_API_BASE}/${policyId}/deactivate`;

  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
      credentials: 'same-origin',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
      throw new Error(`Failed to deactivate policy: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: Policy = await response.json();

    console.log('[policyApi] Policy deactivated successfully:', policyId);
    return data;
  } catch (error) {
    console.error('[policyApi] deactivatePolicy error:', error);
    throw error;
  }
}
