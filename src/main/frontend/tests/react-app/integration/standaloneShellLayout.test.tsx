import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { I18nextProvider } from "react-i18next";

import { LayoutProvider } from "../../../react-app/providers/LayoutProvider";
import { LayoutSelector } from "../../../react-app/components/LayoutSelector";
import i18n from "../../../react-app/i18n/i18n";

describe("Integration: Shell layout rendering in standalone mode", () => {
    beforeEach(() => {
        const container = document.createElement("div");
        container.id = "deploy-platform-root";
        container.setAttribute("data-spring-context", "false");
        document.body.appendChild(container);
    });

    afterEach(() => {
        cleanup();
        const container = document.getElementById("deploy-platform-root");
        container?.remove();
    });

    it("renders ShellLayout wrapper when not embedded", () => {
        render(
            <I18nextProvider i18n={i18n}>
                <LayoutProvider>
                    <MemoryRouter initialEntries={["/overview"]}>
                        <LayoutSelector>
                            <div data-testid="standalone-content">Standalone content</div>
                        </LayoutSelector>
                    </MemoryRouter>
                </LayoutProvider>
            </I18nextProvider>
        );

        expect(screen.getByTestId("deploy-platform-shell")).toBeInTheDocument();
        expect(screen.getByTestId("standalone-content")).toBeInTheDocument();
    });
});
