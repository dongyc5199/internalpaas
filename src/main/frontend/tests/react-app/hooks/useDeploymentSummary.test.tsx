import { describe, expect, it, vi } from "vitest";

import { renderHook, waitFor } from "../utils/renderHook";
import { useDeploymentSummary } from "../../../react-app/hooks/useDeploymentSummary";
import type { OverviewApi } from "../../../react-app/overview/api";

const mockData = {
    metrics: [
        { id: "pending", title: "待审批发布", value: 2, description: "等待审批" },
        { id: "canary", title: "进行中的金丝雀", value: 1, description: "灰度执行中" }
    ],
    highlights: [
        {
            application: "billing-service",
            environment: "prod",
            status: "warning" as const,
            pendingReleases: 1,
            runningCanary: 1,
            lastUpdated: "2025-10-28T06:40:00Z"
        }
    ],
    pollingIntervalSeconds: 30
};

describe("useDeploymentSummary", () => {
    it("loads data successfully", async () => {
        const fetchSummary = vi.fn<OverviewApi["fetchSummary"]>().mockResolvedValue(mockData);
        const { result } = renderHook(() =>
            useDeploymentSummary({
                api: { fetchSummary }
            })
        );

        await waitFor(() => {
            expect(result.current.status).toBe("success");
        });

        if (result.current.status !== "success") {
            throw new Error("Expected success");
        }

        expect(result.current.data.metrics).toHaveLength(2);
        expect(fetchSummary).toHaveBeenCalledTimes(1);
    });

    it("handles error state", async () => {
        const fetchSummary = vi.fn<OverviewApi["fetchSummary"]>().mockRejectedValue(
            new Error("failed")
        );
        const { result } = renderHook(() =>
            useDeploymentSummary({
                api: { fetchSummary },
                pollingEnabled: false
            })
        );

        await waitFor(() => {
            expect(result.current.status).toBe("error");
        });

        if (result.current.status !== "error") {
            throw new Error("Expected error");
        }

        expect(result.current.error.message).toBe("failed");
    });
});
