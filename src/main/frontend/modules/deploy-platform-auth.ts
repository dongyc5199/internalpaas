import { http, HttpError } from "@/utils/http";

type TokenApiResponse = {
    accessToken: string;
    expiresAt?: number;
    expiresIn?: number;
    issuer?: string;
    audience?: string;
    signature?: string;
};

const AUTH_REQUEST_EVENT = "deploy-platform-auth-request";
const AUTH_RESPONSE_EVENT = "deploy-platform-auth";
const AUTH_ERROR_EVENT = "deploy-platform-auth-error";
const DEFAULT_ENDPOINT = "/api/deploy-platform/token";

let listenerRegistered = false;

const resolveEndpoint = (): string => {
    const container = document.getElementById("deploy-platform-root");
    if (container && container instanceof HTMLElement) {
        const attr = container.dataset.authEndpoint;
        if (attr && attr.trim().length > 0) {
            return attr;
        }
    }
    return DEFAULT_ENDPOINT;
};

const postAuthMessage = (payload: Record<string, unknown>, type = AUTH_RESPONSE_EVENT): void => {
    window.postMessage(
        {
            type,
            payload
        },
        window.location.origin
    );
};

const handleAuthRequest = async (event: MessageEvent): Promise<void> => {
    if (!event.data || typeof event.data !== "object") {
        return;
    }
    const { type, nonce } = event.data as { type?: string; nonce?: string };
    if (type !== AUTH_REQUEST_EVENT || typeof nonce !== "string") {
        return;
    }

    try {
        const endpoint = resolveEndpoint();
        const response = await http.post<TokenApiResponse>(endpoint, { nonce });
        const body = response.data;
        const expiresAt =
            typeof body.expiresAt === "number"
                ? body.expiresAt
                : Date.now() + ((body.expiresIn ?? 1800) * 1000);

        postAuthMessage({
            accessToken: body.accessToken,
            expiresAt,
            issuer: body.issuer ?? window.location.origin,
            audience: body.audience ?? "deploy-platform",
            nonce,
            signature: body.signature
        });
    } catch (error) {
        let reason = "fetch_error";
        if (error instanceof HttpError) {
            reason = error.status === 0 ? "network_error" : `http_${error.status}`;
        } else if (error instanceof Error) {
            reason = error.name;
        }

        postAuthMessage({ nonce, reason }, AUTH_ERROR_EVENT);
    }
};

export const registerDeployPlatformAuthBridge = (): void => {
    if (listenerRegistered) {
        return;
    }
    window.addEventListener("message", (event: MessageEvent) => {
        if (event.origin && event.origin !== "null" && event.origin !== window.location.origin) {
            return;
        }
        void handleAuthRequest(event);
    });
    listenerRegistered = true;
};

registerDeployPlatformAuthBridge();
