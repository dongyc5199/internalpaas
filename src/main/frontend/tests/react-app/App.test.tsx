import "@testing-library/jest-dom";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";

import { AppRoutes } from "../../react-app/App";

describe("React 微前端 App", () => {
    it("renders shell layout and overview page", () => {
        render(
            <MemoryRouter initialEntries={["/overview"]}>
                <AppRoutes />
            </MemoryRouter>
        );

        expect(screen.getByTestId("deploy-platform-shell")).toBeInTheDocument();
        expect(screen.getByRole("navigation", { name: "部署管理导航" })).toBeInTheDocument();
        expect(screen.getByTestId("overview-page")).toHaveTextContent("发布实时总览");
    });

    it("renders release details when navigated to release route", () => {
        render(
            <MemoryRouter initialEntries={["/releases/release-123"]}>
                <AppRoutes />
            </MemoryRouter>
        );

        expect(screen.getByTestId("release-details-page")).toHaveTextContent("release-123");
    });
});
