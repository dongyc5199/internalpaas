import { renderHook } from "@testing-library/react";
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import type { PropsWithChildren } from "react";

import { LayoutProvider } from "../../../react-app/providers/LayoutProvider";
import { useLayout } from "../../../react-app/contexts/layoutContext";

describe("Integration: Standalone Mode Behaviour", () => {
    let container: HTMLDivElement;

    beforeEach(() => {
        container = document.createElement("div");
        container.id = "deploy-platform-root";
        container.setAttribute("data-spring-context", "false");
        if (container.parentNode) {
            container.parentNode.removeChild(container);
        }
        document.body.appendChild(container);

        Reflect.deleteProperty(window, "__DEPLOY_PLATFORM_EMBEDDED__");
        Reflect.deleteProperty(window, "__DEPLOY_PLATFORM_DEBUG__");
    });

    afterEach(() => {
        vi.restoreAllMocks();

        const mountedContainer = document.getElementById("deploy-platform-root");
        if (mountedContainer) {
            mountedContainer.remove();
        }
    });

    it("should expose standalone layout context with shell mode", () => {
        const wrapper = ({ children }: PropsWithChildren): JSX.Element => (
            <LayoutProvider>{children}</LayoutProvider>
        );

        const { result } = renderHook(() => useLayout(), { wrapper });

        expect(result.current.isEmbedded).toBe(false);
        expect(result.current.mode).toBe("shell");
    });
});
