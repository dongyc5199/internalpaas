/**
 * HTTP Fetcher abstraction
 * Based on react-app/api/client.ts
 */

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

export type Fetcher = <T>(
  input: RequestInfo | URL,
  init?: RequestInit
) => Promise<T>;

const JSON_HEADERS = {
  'Content-Type': 'application/json',
};

/**
 * Default fetcher using session-based authentication
 * Session cookies (JSESSIONID) are automatically sent by the browser
 */
export const defaultFetcher: Fetcher = async <T>(
  input: RequestInfo | URL,
  init?: RequestInit
): Promise<T> => {
  const response = await fetch(input, {
    ...init,
    credentials: 'same-origin', // Include cookies for session authentication
    headers: {
      ...JSON_HEADERS,
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `HTTP ${response.status}`);
  }

  return (await response.json()) as T;
};

export interface ApiClient {
  get<T>(url: string): Promise<T>;
  post<T>(url: string, body?: unknown): Promise<T>;
  put<T>(url: string, body?: unknown): Promise<T>;
  patch<T>(url: string, body?: unknown): Promise<T>;
  delete<T>(url: string): Promise<T>;
}

export const createApiClient = (fetcher: Fetcher = defaultFetcher): ApiClient => ({
  get: async <T>(url: string) => fetcher<T>(url),
  
  post: async <T>(url: string, body?: unknown) =>
    fetcher<T>(url, {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    }),
  
  put: async <T>(url: string, body?: unknown) =>
    fetcher<T>(url, {
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    }),
  
  patch: async <T>(url: string, body?: unknown) =>
    fetcher<T>(url, {
      method: 'PATCH',
      body: body ? JSON.stringify(body) : undefined,
    }),
  
  delete: async <T>(url: string) =>
    fetcher<T>(url, {
      method: 'DELETE',
    }),
});
