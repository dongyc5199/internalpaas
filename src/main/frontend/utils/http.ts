/**
 * HTTP客户端工具模块
 * 统一封装fetch API,提供CSRF、错误处理、拦截器等功能
 */

interface CsrfToken {
    token: string;
    header: string;
}

interface HttpRequestConfig extends RequestInit {
    params?: Record<string, string | number | boolean>;
    timeout?: number;
}

interface HttpResponse<T = unknown> {
    data: T;
    status: number;
    statusText: string;
    headers: Headers;
}

class HttpError extends Error {
    constructor(
        public status: number,
        public statusText: string,
        message?: string
    ) {
        super(message || `HTTP Error ${status}: ${statusText}`);
        this.name = "HttpError";
    }
}

/**
 * 获取CSRF令牌
 */
function getCsrfToken(): CsrfToken {
    const tokenMeta = document.querySelector<HTMLMetaElement>('meta[name="_csrf"]');
    const headerMeta = document.querySelector<HTMLMetaElement>('meta[name="_csrf_header"]');

    return {
        token: tokenMeta?.content ?? "",
        header: headerMeta?.content ?? "X-CSRF-TOKEN"
    };
}

/**
 * 构建URL查询参数
 */
function buildQueryString(params: Record<string, string | number | boolean>): string {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        searchParams.append(key, String(value));
    });
    return searchParams.toString();
}

/**
 * 请求超时控制
 */
function createTimeoutSignal(timeout: number): AbortSignal {
    const controller = new AbortController();
    setTimeout(() => controller.abort(), timeout);
    return controller.signal;
}

/**
 * 发送HTTP请求
 */
async function request<T = unknown>(
    url: string,
    config: HttpRequestConfig = {}
): Promise<HttpResponse<T>> {
    const { params, timeout = 30000, headers: customHeaders = {}, ...restConfig } = config;

    // 构建完整URL
    let fullUrl = url;
    if (params) {
        const queryString = buildQueryString(params);
        fullUrl = `${url}${url.includes("?") ? "&" : "?"}${queryString}`;
    }

    // 构建请求头
    const headers = new Headers(customHeaders as HeadersInit);

    // 自动添加CSRF令牌(POST/PUT/DELETE等请求)
    const method = (restConfig.method || "GET").toUpperCase();
    if (["POST", "PUT", "DELETE", "PATCH"].includes(method)) {
        const csrf = getCsrfToken();
        if (csrf.token) {
            headers.set(csrf.header, csrf.token);
        }
    }

    // 默认Content-Type
    if (!headers.has("Content-Type") && restConfig.body) {
        if (typeof restConfig.body === "string") {
            headers.set("Content-Type", "application/json");
        }
    }

    // 发送请求
    try {
        const response = await fetch(fullUrl, {
            ...restConfig,
            headers,
            signal: timeout > 0 ? createTimeoutSignal(timeout) : undefined
        });

        // 错误处理
        if (!response.ok) {
            throw new HttpError(response.status, response.statusText);
        }

        // 解析响应
        const contentType = response.headers.get("Content-Type") || "";
        let data: T;

        if (contentType.includes("application/json")) {
            data = (await response.json()) as T;
        } else if (contentType.includes("text/")) {
            data = (await response.text()) as T;
        } else {
            data = (await response.blob()) as T;
        }

        return {
            data,
            status: response.status,
            statusText: response.statusText,
            headers: response.headers
        };
    } catch (error) {
        if (error instanceof HttpError) {
            throw error;
        }
        if (error instanceof Error) {
            if (error.name === "AbortError") {
                throw new HttpError(408, "Request Timeout", "请求超时");
            }
            throw new HttpError(0, "Network Error", error.message);
        }
        throw error;
    }
}

/**
 * HTTP客户端API
 */
export const http = {
    /**
     * GET请求
     */
    get<T = unknown>(url: string, config?: HttpRequestConfig): Promise<HttpResponse<T>> {
        return request<T>(url, { ...config, method: "GET" });
    },

    /**
     * POST请求
     */
    post<T = unknown>(
        url: string,
        data?: unknown,
        config?: HttpRequestConfig
    ): Promise<HttpResponse<T>> {
        return request<T>(url, {
            ...config,
            method: "POST",
            body: data ? JSON.stringify(data) : undefined
        });
    },

    /**
     * PUT请求
     */
    put<T = unknown>(
        url: string,
        data?: unknown,
        config?: HttpRequestConfig
    ): Promise<HttpResponse<T>> {
        return request<T>(url, {
            ...config,
            method: "PUT",
            body: data ? JSON.stringify(data) : undefined
        });
    },

    /**
     * DELETE请求
     */
    delete<T = unknown>(url: string, config?: HttpRequestConfig): Promise<HttpResponse<T>> {
        return request<T>(url, { ...config, method: "DELETE" });
    },

    /**
     * PATCH请求
     */
    patch<T = unknown>(
        url: string,
        data?: unknown,
        config?: HttpRequestConfig
    ): Promise<HttpResponse<T>> {
        return request<T>(url, {
            ...config,
            method: "PATCH",
            body: data ? JSON.stringify(data) : undefined
        });
    }
};

export { HttpError, type HttpResponse, type HttpRequestConfig, type CsrfToken };
