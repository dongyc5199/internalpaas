/**
 * Release API Client
 *
 * API functions for release management operations (CRUD operations for releases).
 */

import type {
  Release,
  ReleaseSummary,
  ReleaseListResponse,
  CreateReleaseRequest,
  UpdateReleaseRequest,
  ReleaseFilters,
  ReleaseDeploymentResult,
} from '../types/release';

/** Base API endpoint for release operations */
const RELEASE_API_BASE = '/api/deploy-platform/releases';

/**
 * Build query string from filter object
 */
function buildQueryString(filters?: ReleaseFilters): string {
  if (!filters) return '';

  const params = new URLSearchParams();

  if (filters.status) {
    filters.status.forEach(s => params.append('status', s));
  }
  if (filters.environment) {
    filters.environment.forEach(e => params.append('environment', e));
  }
  if (filters.applicationId) {
    params.append('applicationId', filters.applicationId);
  }
  if (filters.createdBy) {
    params.append('createdBy', filters.createdBy);
  }
  if (filters.priority) {
    filters.priority.forEach(p => params.append('priority', p));
  }
  if (filters.tags) {
    filters.tags.forEach(t => params.append('tag', t));
  }
  if (filters.fromDate) {
    params.append('fromDate', filters.fromDate);
  }
  if (filters.toDate) {
    params.append('toDate', filters.toDate);
  }

  const queryString = params.toString();
  return queryString ? `?${queryString}` : '';
}

/**
 * Fetch all releases with optional filters and pagination
 *
 * @param options - Query options (filters, pagination)
 * @param token - JWT access token for authorization
 * @returns Promise resolving to paginated release list
 */
export async function fetchReleases(
  options?: {
    filters?: ReleaseFilters;
    page?: number;
    pageSize?: number;
  },
  token?: string
): Promise<ReleaseListResponse> {
  const { filters, page = 1, pageSize = 20 } = options || {};

  const filterQuery = buildQueryString(filters);
  const paginationQuery = `${filterQuery ? '&' : '?'}page=${page}&pageSize=${pageSize}`;
  const url = `${RELEASE_API_BASE}${filterQuery}${paginationQuery}`;

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
      throw new Error(`Failed to fetch releases: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: ReleaseListResponse = await response.json();

    // Validate response structure
    if (!Array.isArray(data.releases)) {
      throw new Error('Invalid releases response structure: missing releases array');
    }

    return data;
  } catch (error) {
    console.error('[releaseApi] fetchReleases error:', error);
    throw error;
  }
}

/**
 * Fetch a single release by ID
 *
 * @param releaseId - Release unique identifier
 * @param token - JWT access token for authorization
 * @returns Promise resolving to release details
 */
export async function fetchReleaseById(
  releaseId: string,
  token?: string
): Promise<Release> {
  const url = `${RELEASE_API_BASE}/${releaseId}`;

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
      throw new Error(`Failed to fetch release: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: Release = await response.json();

    // Validate response structure
    if (!data.id || !data.version) {
      throw new Error('Invalid release response structure');
    }

    return data;
  } catch (error) {
    console.error('[releaseApi] fetchReleaseById error:', error);
    throw error;
  }
}

/**
 * Create a new release
 *
 * @param request - Release creation data
 * @param token - JWT access token for authorization
 * @returns Promise resolving to created release
 */
export async function createRelease(
  request: CreateReleaseRequest,
  token?: string
): Promise<Release> {
  const url = RELEASE_API_BASE;

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
      throw new Error(`Failed to create release: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: Release = await response.json();

    console.log('[releaseApi] Release created successfully:', data.id);
    return data;
  } catch (error) {
    console.error('[releaseApi] createRelease error:', error);
    throw error;
  }
}

/**
 * Update an existing release
 *
 * @param releaseId - Release unique identifier
 * @param request - Release update data
 * @param token - JWT access token for authorization
 * @returns Promise resolving to updated release
 */
export async function updateRelease(
  releaseId: string,
  request: UpdateReleaseRequest,
  token?: string
): Promise<Release> {
  const url = `${RELEASE_API_BASE}/${releaseId}`;

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
      throw new Error(`Failed to update release: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: Release = await response.json();

    console.log('[releaseApi] Release updated successfully:', releaseId);
    return data;
  } catch (error) {
    console.error('[releaseApi] updateRelease error:', error);
    throw error;
  }
}

/**
 * Delete a release
 *
 * @param releaseId - Release unique identifier
 * @param token - JWT access token for authorization
 * @returns Promise resolving when deletion succeeds
 */
export async function deleteRelease(
  releaseId: string,
  token?: string
): Promise<void> {
  const url = `${RELEASE_API_BASE}/${releaseId}`;

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
      throw new Error(`Failed to delete release: ${response.status} - ${errorData.message || response.statusText}`);
    }

    console.log('[releaseApi] Release deleted successfully:', releaseId);
  } catch (error) {
    console.error('[releaseApi] deleteRelease error:', error);
    throw error;
  }
}

/**
 * Deploy a release
 *
 * @param releaseId - Release unique identifier
 * @param token - JWT access token for authorization
 * @returns Promise resolving to deployment result
 */
export async function deployRelease(
  releaseId: string,
  token?: string
): Promise<ReleaseDeploymentResult> {
  const url = `${RELEASE_API_BASE}/${releaseId}/deploy`;

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
      throw new Error(`Failed to deploy release: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: ReleaseDeploymentResult = await response.json();

    console.log('[releaseApi] Release deployment initiated:', releaseId);
    return data;
  } catch (error) {
    console.error('[releaseApi] deployRelease error:', error);
    throw error;
  }
}

/**
 * Approve a release (change status to 'approved')
 *
 * @param releaseId - Release unique identifier
 * @param token - JWT access token for authorization
 * @returns Promise resolving to updated release
 */
export async function approveRelease(
  releaseId: string,
  token?: string
): Promise<Release> {
  const url = `${RELEASE_API_BASE}/${releaseId}/approve`;

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
      throw new Error(`Failed to approve release: ${response.status} - ${errorData.message || response.statusText}`);
    }

    const data: Release = await response.json();

    console.log('[releaseApi] Release approved successfully:', releaseId);
    return data;
  } catch (error) {
    console.error('[releaseApi] approveRelease error:', error);
    throw error;
  }
}
