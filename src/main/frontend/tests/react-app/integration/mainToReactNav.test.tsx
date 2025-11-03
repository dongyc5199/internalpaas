/**
 * Integration Test: Main App to React Navigation (T021)
 *
 * 目的: 测试主应用侧边栏点击触发的CustomEvent能够正确导航React应用
 *
 * 测试策略:
 * - 使用useNavSync hook的实际实现
 * - 模拟main-nav-change事件的dispatch
 * - 验证navigate函数被正确调用
 * - 测试防循环机制和错误处理
 */

import { renderHook, waitFor, act } from "@testing-library/react";
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { useNavSync } from "../../../react-app/hooks/useNavSync";
import type { NavigateFunction, Location } from "react-router-dom";

declare global {
    interface Window {
        __DEPLOY_PLATFORM_DEBUG__?: boolean;
    }
}

describe("Integration Test: Main App to React Navigation", () => {
    let navigate: NavigateFunction;
    let mockLocation: Location;

    beforeEach(() => {
        // Mock navigate function
        navigate = vi.fn() as unknown as NavigateFunction;

        // Mock location object
        mockLocation = {
            pathname: "/dashboard",
            search: "",
            hash: "",
            state: null,
            key: "default"
        } as Location;

        // Mock DOM environment
        const container = document.createElement("div");
        container.id = "deploy-platform-root";
        container.setAttribute("data-spring-context", "true");
        container.setAttribute("data-embedded", "true");
        document.body.appendChild(container);
    });

    afterEach(() => {
        // Clean up DOM
        const container = document.getElementById("deploy-platform-root");
        if (container) {
            document.body.removeChild(container);
        }

        vi.restoreAllMocks();
    });

    describe("Basic Navigation from Main App", () => {
        it("should navigate to /overview when main app sends overview route event", async () => {
            renderHook(() => useNavSync(navigate, mockLocation));

            const event = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/overview",
                    source: "main-app",
                    timestamp: Date.now()
                }
            });

            act(() => {
                window.dispatchEvent(event);
            });

            await waitFor(() => {
                expect(navigate).toHaveBeenCalledWith("/overview");
            });
        });

        it("should navigate to /releases when main app sends releases route event", async () => {
            renderHook(() => useNavSync(navigate, mockLocation));

            const event = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/releases",
                    source: "main-app",
                    timestamp: Date.now()
                }
            });

            act(() => {
                window.dispatchEvent(event);
            });

            await waitFor(() => {
                expect(navigate).toHaveBeenCalledWith("/releases");
            });
        });

        it("should navigate to /settings/policies when main app sends policies route event", async () => {
            renderHook(() => useNavSync(navigate, mockLocation));

            const event = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/settings/policies",
                    source: "main-app",
                    timestamp: Date.now()
                }
            });

            act(() => {
                window.dispatchEvent(event);
            });

            await waitFor(() => {
                expect(navigate).toHaveBeenCalledWith("/settings/policies");
            });
        });
    });

    describe("Event Payload Validation", () => {
        it("should handle event with all required fields", async () => {
            renderHook(() => useNavSync(navigate, mockLocation));

            const timestamp = Date.now();
            const event = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/releases",
                    source: "main-app",
                    timestamp: timestamp
                }
            });

            act(() => {
                window.dispatchEvent(event);
            });

            await waitFor(() => {
                expect(navigate).toHaveBeenCalledWith("/releases");
            });
        });

        it("should ignore event with missing route field", async () => {
            renderHook(() => useNavSync(navigate, mockLocation));

            const event = new CustomEvent("main-nav-change", {
                detail: {
                    source: "main-app",
                    timestamp: Date.now()
                    // route is missing
                }
            });

            act(() => {
                window.dispatchEvent(event);
            });

            // Wait a bit to ensure no navigation occurs
            await new Promise((resolve) => setTimeout(resolve, 200));

            expect(navigate).not.toHaveBeenCalled();
        });

        it("should handle event with null detail gracefully", async () => {
            renderHook(() => useNavSync(navigate, mockLocation));

            const event = new CustomEvent("main-nav-change", {
                detail: null
            });

            expect(() => {
                act(() => {
                    window.dispatchEvent(event);
                });
            }).not.toThrow();

            await new Promise((resolve) => setTimeout(resolve, 200));
            expect(navigate).not.toHaveBeenCalled();
        });
    });

    describe("Sequential Navigation", () => {
        it("should handle multiple navigation events in sequence after timeout", () => {
            vi.useFakeTimers();

            renderHook(() => useNavSync(navigate, mockLocation));

            const event1 = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/releases",
                    source: "main-app",
                    timestamp: Date.now()
                }
            });

            act(() => {
                window.dispatchEvent(event1);
            });

            expect(navigate).toHaveBeenNthCalledWith(1, "/releases");

            act(() => {
                vi.advanceTimersByTime(350);
            });

            const event2 = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/settings/policies",
                    source: "main-app",
                    timestamp: Date.now()
                }
            });

            act(() => {
                window.dispatchEvent(event2);
            });

            expect(navigate).toHaveBeenNthCalledWith(2, "/settings/policies");

            act(() => {
                vi.advanceTimersByTime(350);
            });

            const event3 = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/overview",
                    source: "main-app",
                    timestamp: Date.now()
                }
            });

            act(() => {
                window.dispatchEvent(event3);
            });

            expect(navigate).toHaveBeenNthCalledWith(3, "/overview");

            vi.useRealTimers();
        });
    });

    describe("Anti-Loop Mechanism", () => {
        it("should not navigate when isSyncing flag is active", async () => {
            renderHook(() => useNavSync(navigate, mockLocation));

            // First navigation event
            const event1 = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/releases",
                    source: "main-app",
                    timestamp: Date.now()
                }
            });

            act(() => {
                window.dispatchEvent(event1);
            });

            expect(navigate).toHaveBeenCalledTimes(1);

            // Immediately send another event (should be blocked by isSyncing)
            const event2 = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/overview",
                    source: "main-app",
                    timestamp: Date.now()
                }
            });

            act(() => {
                window.dispatchEvent(event2);
            });

            // Wait a bit
            await new Promise((resolve) => setTimeout(resolve, 100));

            expect(navigate).toHaveBeenCalledTimes(1);
        });

        it("should allow navigation after sync timeout (300ms)", async () => {
            vi.useFakeTimers();

            renderHook(() => useNavSync(navigate, mockLocation));

            // First navigation
            const event1 = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/releases",
                    source: "main-app"
                }
            });

            act(() => {
                window.dispatchEvent(event1);
            });

            expect(navigate).toHaveBeenCalledTimes(1);

            // Fast-forward past sync timeout (300ms)
            act(() => {
                vi.advanceTimersByTime(350);
            });

            // Second navigation (should work now)
            const event2 = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/settings/policies",
                    source: "main-app"
                }
            });

            act(() => {
                window.dispatchEvent(event2);
            });

            // Navigate should be called a second time
            expect(navigate).toHaveBeenCalledTimes(2);

            vi.useRealTimers();
        });
    });

    describe("Error Handling", () => {
        it("should not navigate when already on target route", async () => {
            const currentLocation = {
                ...mockLocation,
                pathname: "/overview"
            } as Location;

            renderHook(() => useNavSync(navigate, currentLocation));

            // Send event for current route
            const event = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/overview",
                    source: "main-app",
                    timestamp: Date.now()
                }
            });

            act(() => {
                window.dispatchEvent(event);
            });

            // Wait a bit
            await new Promise((resolve) => setTimeout(resolve, 200));

            // Should not navigate (already on this route)
            expect(navigate).not.toHaveBeenCalled();
        });

        it("should handle navigate errors gracefully", async () => {
            const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
            const errorNavigate = vi.fn(() => {
                throw new Error("Navigation failed");
            });

            renderHook(() => useNavSync(errorNavigate, mockLocation));

            const event = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/releases",
                    source: "main-app"
                }
            });

            act(() => {
                window.dispatchEvent(event);
            });

            // Navigate should have been called and error logged
            await waitFor(() => {
                expect(errorNavigate).toHaveBeenCalled();
                expect(consoleErrorSpy).toHaveBeenCalled();
            });

            consoleErrorSpy.mockRestore();
        });
    });

    describe("Debug Logging", () => {
        it("should log navigation events when debug flag is enabled", async () => {
            const consoleSpy = vi.spyOn(console, "log").mockImplementation(() => {});
            window.__DEPLOY_PLATFORM_DEBUG__ = true;

            renderHook(() => useNavSync(navigate, mockLocation));

            const event = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/releases",
                    source: "main-app"
                }
            });

            act(() => {
                window.dispatchEvent(event);
            });

            await waitFor(() => {
                const debugCalls = consoleSpy.mock.calls.filter(
                    (call) => typeof call[0] === "string" && call[0].includes("[useNavSync]")
                );
                expect(debugCalls.length).toBeGreaterThan(0);
            });

            delete window.__DEPLOY_PLATFORM_DEBUG__;
            consoleSpy.mockRestore();
        });

        it("should not log when debug flag is disabled", async () => {
            const consoleSpy = vi.spyOn(console, "log").mockImplementation(() => {});
            window.__DEPLOY_PLATFORM_DEBUG__ = false;

            renderHook(() => useNavSync(navigate, mockLocation));

            const event = new CustomEvent("main-nav-change", {
                detail: {
                    route: "/releases",
                    source: "main-app"
                }
            });

            act(() => {
                window.dispatchEvent(event);
            });

            await waitFor(() => {
                expect(navigate).toHaveBeenCalled();
            });

            const debugCalls = consoleSpy.mock.calls.filter(
                (call) => typeof call[0] === "string" && call[0].includes("[useNavSync]")
            );
            expect(debugCalls).toHaveLength(0);

            delete window.__DEPLOY_PLATFORM_DEBUG__;
            consoleSpy.mockRestore();
        });
    });
});
