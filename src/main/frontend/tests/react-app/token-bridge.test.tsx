import "@testing-library/jest-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
    __resetTokenBridgeForTest,
    getAccessToken,
    initTokenBridge,
    tokenStore,
    type TokenPayload
} from "../../react-app/token-bridge";

const mockPostMessage = (
    origin: string,
    payload: Omit<TokenPayload, "nonce"> & { nonce?: string }
): void => {
    const parentWindow: Window & typeof globalThis = window.parent || window;
    vi.spyOn(parentWindow, "postMessage").mockImplementation((message: any) => {
        if (!message || message.type !== "deploy-platform-auth-request") {
            return undefined;
        }
        const nonce = message.nonce ?? "test-nonce";
        setTimeout(() => {
            window.dispatchEvent(
                new MessageEvent("message", {
                    origin,
                    data: {
                        type: "deploy-platform-auth",
                        payload: {
                            ...payload,
                            nonce
                        }
                    }
                })
            );
        }, 0);
        return undefined;
    });
};

describe("token-bridge", () => {
    beforeEach(() => {
        vi.useFakeTimers();
        __resetTokenBridgeForTest();
        if (!("crypto" in globalThis)) {
            // @ts-expect-error jsdom fallback
            globalThis.crypto = {};
        }
        // @ts-expect-error jsdom fallback
        globalThis.crypto.randomUUID = () => "test-nonce";
    });

    it("stores token received via postMessage", async () => {
        const updateHandler = vi.fn();
        const errorHandler = vi.fn();
        const expectedOrigin = window.location.origin;

        mockPostMessage(expectedOrigin, {
            accessToken: "token-abc",
            expiresAt: Date.now() + 5 * 60 * 1000,
            issuer: expectedOrigin,
            audience: "deploy-platform"
        });

        let latest: TokenPayload | null = null;
        const unsubscribe = tokenStore.subscribe((token) => {
            latest = token;
        });

        initTokenBridge({
            expectedOrigin,
            onTokenUpdate: updateHandler,
            onTokenError: errorHandler,
            requestTimeoutMs: 1000
        });

        await vi.runAllTimersAsync();

        expect(updateHandler).toHaveBeenCalledTimes(1);
        expect(errorHandler).toHaveBeenCalledWith("token_expiring");
        expect(getAccessToken()).toBe("token-abc");

        unsubscribe();
        expect(latest?.accessToken).toBe("token-abc");
    });

    it("reports origin mismatch error", async () => {
        const errorHandler = vi.fn();
        const expectedOrigin = window.location.origin;

        mockPostMessage("https://evil.example.com", {
            accessToken: "bad-token",
            expiresAt: Date.now() + 5 * 60 * 1000,
            issuer: "https://evil.example.com",
            audience: "deploy-platform"
        });

        initTokenBridge({
            expectedOrigin,
            onTokenError: errorHandler,
            requestTimeoutMs: 1000
        });

        await vi.runAllTimersAsync();

        expect(errorHandler).toHaveBeenCalledWith("origin_mismatch");
        expect(getAccessToken()).toBeNull();
    });
});
