import "@testing-library/jest-dom";
import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";

import { OverviewPage } from "../../react-app/pages/OverviewPage";
import * as hookModule from "../../react-app/hooks/useDeploymentSummary";

const successState: hookModule.SummaryState = {
    status: "success",
    data: {
        metrics: [
            { id: "pending", title: "待审批发布", value: 3, description: "等待审批" },
            { id: "canary", title: "金丝雀", value: 1, description: "运行中金丝雀" }
        ],
        highlights: [
            {
                application: "payment-gateway",
                environment: "prod",
                status: "warning",
                pendingReleases: 2,
                runningCanary: 1,
                lastUpdated: "2025-10-28T06:40:00Z"
            }
        ],
        pollingIntervalSeconds: 60
    }
};

describe("OverviewPage", () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it("renders loading state", () => {
        vi.spyOn(hookModule, "useDeploymentSummary").mockReturnValue({ status: "loading" });
        render(<OverviewPage />);

        expect(screen.getByTestId("overview-loading")).toHaveTextContent("正在加载");
    });

    it("renders error state", () => {
        vi.spyOn(hookModule, "useDeploymentSummary").mockReturnValue({
            status: "error",
            error: new Error("failed")
        });
        render(<OverviewPage />);

        expect(screen.getByTestId("overview-error")).toHaveTextContent("failed");
    });

    it("renders data when success", () => {
        vi.spyOn(hookModule, "useDeploymentSummary").mockReturnValue(successState);
        render(<OverviewPage />);

        expect(screen.getByTestId("metric-pending")).toBeInTheDocument();
        expect(screen.getByText("payment-gateway · prod")).toBeInTheDocument();
    });
});
