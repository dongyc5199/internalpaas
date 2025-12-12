import "@testing-library/jest-dom";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { I18nextProvider } from "react-i18next";

import { AppRoutes } from "../../react-app/App";
import { LayoutProvider } from "../../react-app/providers/LayoutProvider";
import i18n from "../../react-app/i18n/i18n";

describe("React 微前端 App", () => {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    });

    const renderWithProviders = (ui: React.ReactElement, initialEntries: string[] = ["/"]) => {
        return render(
            <QueryClientProvider client={queryClient}>
                <I18nextProvider i18n={i18n}>
                    <LayoutProvider>
                        <MemoryRouter initialEntries={initialEntries}>
                            {ui}
                        </MemoryRouter>
                    </LayoutProvider>
                </I18nextProvider>
            </QueryClientProvider>
        );
    };

    it("renders shell layout and overview page", () => {
        renderWithProviders(<AppRoutes />, ["/overview"]);

        expect(screen.getByTestId("deploy-platform-shell")).toBeInTheDocument();
        expect(screen.getByRole("navigation", { name: "部署管理导航" })).toBeInTheDocument();
        expect(screen.getByTestId("overview-page")).toHaveTextContent("发布实时总览");
    });

    it("renders release details when navigated to release route", () => {
        renderWithProviders(<AppRoutes />, ["/releases/release-123"]);

        expect(screen.getByTestId("release-details-page")).toHaveTextContent("release-123");
    });
});
