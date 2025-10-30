type TokenPayload = {
    accessToken: string;
    expiresAt: number; // epoch milliseconds
    issuer: string;
    audience: string;
    nonce: string;
    signature?: string;
};

type BridgeOptions = {
    expectedOrigin?: string;
    requestTimeoutMs?: number;
    onTokenUpdate?: (token: TokenPayload) => void;
    onTokenError?: (message: string) => void;
};

const DEFAULT_EXPECTED_ORIGIN =
    typeof window !== "undefined" && window.location ? window.location.origin : "*";
const DEFAULT_TIMEOUT = 10_000;

let currentToken: TokenPayload | null = null;
let pendingNonce: string | null = null;
let tokenTimer: number | null = null;
let messageHandler: ((event: MessageEvent) => void) | null = null;

const listeners: Array<(token: TokenPayload | null) => void> = [];

const notify = (token: TokenPayload | null): void => {
    listeners.forEach((listener) => {
        try {
            listener(token);
        } catch (err) {
            console.warn("[deploy-platform] token listener error", err);
        }
    });
};

const clearTimer = (): void => {
    if (tokenTimer) {
        window.clearTimeout(tokenTimer);
        tokenTimer = null;
    }
};

const scheduleRefreshWarning = (expiresAt: number, onError?: (message: string) => void): void => {
    clearTimer();
    const now = Date.now();
    const lead = Math.max(expiresAt - now - 60_000, 0); // 1 min earlier
    tokenTimer = window.setTimeout(() => {
        onError?.("token_expiring");
    }, lead);
};

const validatePayload = (payload: unknown, expectedOrigin: string): payload is TokenPayload => {
    if (typeof payload !== "object" || payload === null) {
        return false;
    }
    const p = payload as Record<string, unknown>;
    return (
        typeof p.accessToken === "string" &&
        typeof p.expiresAt === "number" &&
        typeof p.issuer === "string" &&
        typeof p.audience === "string" &&
        typeof p.nonce === "string" &&
        (typeof p.signature === "string" || p.signature === null || typeof p.signature === "undefined") &&
        expectedOrigin.length > 0
    );
};

export const tokenStore = {
    get(): TokenPayload | null {
        return currentToken;
    },
    subscribe(listener: (token: TokenPayload | null) => void): () => void {
        listeners.push(listener);
        return () => {
            const index = listeners.indexOf(listener);
            if (index >= 0) {
                listeners.splice(index, 1);
            }
        };
    }
};

export const requestToken = (nonce: string): void => {
    if (typeof window === "undefined") {
        return;
    }
    pendingNonce = nonce;
    window.parent?.postMessage(
        {
            type: "deploy-platform-auth-request",
            nonce
        },
        window.location.origin
    );
};

export const initTokenBridge = ({
    expectedOrigin = DEFAULT_EXPECTED_ORIGIN,
    requestTimeoutMs = DEFAULT_TIMEOUT,
    onTokenUpdate,
    onTokenError
}: BridgeOptions = {}): void => {
    if (typeof window === "undefined") {
        return;
    }

    if (messageHandler) {
        window.removeEventListener("message", messageHandler);
        messageHandler = null;
    }

    const handleMessage = (event: MessageEvent): void => {
        if (!event.data || typeof event.data !== "object") {
            return;
        }
        const { type, payload } = event.data as { type?: string; payload?: unknown };
        if (type === "deploy-platform-auth-error") {
            const reason =
                typeof payload === "object" && payload !== null && "reason" in payload
                    ? (payload as Record<string, unknown>).reason
                    : "unknown_error";
            onTokenError?.(String(reason));
            return;
        }
        if (type !== "deploy-platform-auth") {
            return;
        }

        if (expectedOrigin !== "*" && event.origin !== expectedOrigin) {
            console.warn("[deploy-platform] Unexpected token origin", event.origin);
            onTokenError?.("origin_mismatch");
            return;
        }

        if (!validatePayload(payload, expectedOrigin)) {
            console.warn("[deploy-platform] Received invalid token payload", payload);
            onTokenError?.("invalid_payload");
            return;
        }

        if (pendingNonce && payload.nonce !== pendingNonce) {
            console.warn("[deploy-platform] Nonce mismatch", payload.nonce);
            onTokenError?.("nonce_mismatch");
            return;
        }

        currentToken = payload;
        pendingNonce = null;
        onTokenUpdate?.(payload);
        scheduleRefreshWarning(payload.expiresAt, onTokenError);
        notify(payload);
    };

        window.addEventListener("message", handleMessage);
    messageHandler = handleMessage;

    const nonce = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).slice(2);
    requestToken(nonce);

    if (requestTimeoutMs > 0) {
        window.setTimeout(() => {
            if (!currentToken) {
                onTokenError?.("request_timeout");
            }
        }, requestTimeoutMs);
    }
};

export const getAccessToken = (): string | null => {
    if (!currentToken) {
        return null;
    }
    if (currentToken.expiresAt <= Date.now()) {
        return null;
    }
    return currentToken.accessToken;
};

export const __resetTokenBridgeForTest = (): void => {
    clearTimer();
    currentToken = null;
    pendingNonce = null;
    if (messageHandler) {
        window.removeEventListener("message", messageHandler);
        messageHandler = null;
    }
    listeners.splice(0, listeners.length);
};
