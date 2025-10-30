export type HttpMethod = "GET" | "POST";

export type Fetcher = <T>(
    input: RequestInfo | URL,
    init?: RequestInit
) => Promise<T>;

const JSON_HEADERS = {
    "Content-Type": "application/json"
};

/**
 * Default fetcher that relies on session-based authentication
 * Session cookies (JSESSIONID) are automatically sent by the browser
 * No need to manually add Authorization header
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
            ...(init?.headers ?? {})
        }
    });

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || `HTTP ${response.status}`);
    }

    return (await response.json()) as T;
};

export type ApiClient = {
    get<T>(url: string): Promise<T>;
    post<T>(url: string, body?: unknown): Promise<T>;
};

export const createApiClient = (fetcher: Fetcher = defaultFetcher): ApiClient => ({
    get: async <T>(url: string) => fetcher<T>(url),
    post: async <T>(url: string, body?: unknown) =>
        fetcher<T>(url, {
            method: "POST",
            body: body ? JSON.stringify(body) : undefined
        })
});

