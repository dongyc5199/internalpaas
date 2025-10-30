import "@testing-library/jest-dom";
import { waitFor } from "@testing-library/dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

const postMock = vi.fn();
const httpPostMock = vi.fn();

vi.mock("@/utils/http", () => {
    class MockHttpError extends Error {
        status: number;
        statusText: string;
        constructor(status: number, statusText: string, message?: string) {
            super(message);
            this.status = status;
            this.statusText = statusText;
            this.name = "HttpError";
        }
    }

    return {
        http: {
            post: httpPostMock
        },
        HttpError: MockHttpError
    };
});

describe("deploy-platform-auth module", () => {
    beforeEach(async () => {
        vi.resetModules();
        vi.clearAllMocks();
        httpPostMock.mockReset();
        postMock.mockReset();
        document.body.innerHTML = '<div id="deploy-platform-root" data-auth-endpoint="/api/mock-token"></div>';
        vi.spyOn(window, "postMessage").mockImplementation(postMock);
        await import("../../modules/deploy-platform-auth");
    });

    it("fetches token and posts response", async () => {
        httpPostMock.mockResolvedValue({
            data: {
                accessToken: "from-bff",
                expiresIn: 1200,
                issuer: "https://issuer",
                audience: "deploy-platform"
            }
        });

        window.dispatchEvent(
            new MessageEvent("message", {
                data: { type: "deploy-platform-auth-request", nonce: "abc" },
                origin: window.location.origin
            })
        );

        await waitFor(() => {
            expect(httpPostMock).toHaveBeenCalledWith("/api/mock-token", { nonce: "abc" });
            expect(postMock).toHaveBeenCalled();
        });

        expect(postMock).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "deploy-platform-auth",
                payload: expect.objectContaining({ accessToken: "from-bff", nonce: "abc" })
            }),
            window.location.origin
        );
    });

    it("emits error event when request fails", async () => {
        const { HttpError } = await import("@/utils/http");
        httpPostMock.mockRejectedValue(new HttpError(401, "Unauthorized", "Invalid"));

        window.dispatchEvent(
            new MessageEvent("message", {
                data: { type: "deploy-platform-auth-request", nonce: "xyz" },
                origin: window.location.origin
            })
        );

        await waitFor(() => {
            expect(postMock).toHaveBeenCalled();
        });

        expect(postMock).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "deploy-platform-auth-error",
                payload: expect.objectContaining({ nonce: "xyz", reason: "http_401" })
            }),
            window.location.origin
        );
    });
});
