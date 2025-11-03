import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { renderHook, act } from "@testing-library/react";
import type { Location, NavigateFunction } from "react-router-dom";

import { useNavSync } from "../../../react-app/hooks/useNavSync";
import { detectEmbedMode } from "../../../react-app/hooks/useEmbedMode";

const createStandaloneRoot = (): HTMLDivElement => {
    const root = document.createElement("div");
    root.id = "deploy-platform-root";
    root.setAttribute("data-spring-context", "false");
    document.body.appendChild(root);
    return root;
};

describe("Integration: Standalone Navigation Isolation", () => {
    let container: HTMLDivElement;

    beforeEach(() => {
        container = createStandaloneRoot();
        Reflect.deleteProperty(window, "__DEPLOY_PLATFORM_EMBEDDED__");
    });

    afterEach(() => {
        container.remove();
        vi.restoreAllMocks();
    });

    it("should confirm detectEmbedMode reports standalone", () => {
        expect(detectEmbedMode()).toBe(false);
    });

    it("should ignore main-nav-change events when standalone", async () => {
        const navigate = vi.fn() as unknown as NavigateFunction;
        const mockLocation: Location = {
            pathname: "/overview",
            search: "",
            hash: "",
            state: null,
            key: "init"
        } as Location;

        renderHook(({ location }) => useNavSync(navigate, location), {
            initialProps: { location: mockLocation }
        });

        const mainEvent = new CustomEvent("main-nav-change", {
            detail: {
                route: "/releases",
                source: "main-app",
                timestamp: Date.now()
            }
        });

        act(() => {
            window.dispatchEvent(mainEvent);
        });

        await new Promise((resolve) => setTimeout(resolve, 20));
        expect(navigate).not.toHaveBeenCalled();
    });

    it("should not dispatch react-nav-change events when location changes", async () => {
        const navigate = vi.fn() as unknown as NavigateFunction;
        const mockLocation: Location = {
            pathname: "/overview",
            search: "",
            hash: "",
            state: null,
            key: "base"
        } as Location;

        const dispatchSpy = vi.spyOn(window, "dispatchEvent");

        const { rerender } = renderHook(({ location }) => useNavSync(navigate, location), {
            initialProps: { location: mockLocation }
        });

        const newLocation: Location = {
            ...mockLocation,
            pathname: "/releases",
            key: "standalone"
        } as Location;

        act(() => {
            rerender({ location: newLocation });
        });

        await new Promise((resolve) => setTimeout(resolve, 20));

        const emitted = dispatchSpy.mock.calls.filter(
            ([event]) => event instanceof CustomEvent && event.type === "react-nav-change"
        );

        expect(emitted).toHaveLength(0);
    });
});
