import { ApiClient, createApiClient } from "../api/client";

export type DeploymentSummaryItem = {
    application: string;
    environment: string;
    status: "healthy" | "warning" | "failed" | "pending";
    pendingReleases: number;
    runningCanary: number;
    lastUpdated: string;
};

export type DeploymentSummaryResponse = {
    metrics: Array<{
        id: string;
        title: string;
        value: number;
        unit?: string;
        trend?: "up" | "down" | "flat";
        description: string;
    }>;
    highlights: DeploymentSummaryItem[];
    pollingIntervalSeconds: number;
};

const DEFAULT_ENDPOINT = "/api/deploy-platform/dashboard/summary";

export const createOverviewApi = (client: ApiClient = createApiClient()) => ({
    async fetchSummary(endpoint: string = DEFAULT_ENDPOINT): Promise<DeploymentSummaryResponse> {
        return client.get<DeploymentSummaryResponse>(endpoint);
    }
});

export type OverviewApi = ReturnType<typeof createOverviewApi>;
