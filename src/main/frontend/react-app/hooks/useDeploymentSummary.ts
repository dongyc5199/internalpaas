import { useEffect, useRef, useState } from "react";

import {
    createOverviewApi,
    DeploymentSummaryResponse,
    OverviewApi
} from "../overview/api";

export type SummaryState =
    | { status: "idle" }
    | { status: "loading" }
    | { status: "success"; data: DeploymentSummaryResponse }
    | { status: "error"; error: Error };

type UseDeploymentSummaryOptions = {
    endpoint?: string;
    api?: OverviewApi;
    pollingEnabled?: boolean;
};

const DEFAULT_POLL_INTERVAL = 60_000;
const MIN_POLL_INTERVAL = 15_000;

export function useDeploymentSummary({
    endpoint,
    api,
    pollingEnabled = true
}: UseDeploymentSummaryOptions = {}): SummaryState {
    const [state, setState] = useState<SummaryState>({ status: "idle" });
    const timeoutRef = useRef<number | null>(null);

    // Create stable API instance - only create once, not on every render
    const apiRef = useRef<OverviewApi>(api ?? createOverviewApi());
    const stableApi = apiRef.current;

    useEffect(() => {
        let disposed = false;

        const scheduleReload = (intervalMs: number): void => {
            if (!pollingEnabled) {
                return;
            }
            timeoutRef.current = window.setTimeout(() => {
                void loadSummary();
            }, intervalMs);
        };

        const loadSummary = async (): Promise<void> => {
            setState((prev) => (prev.status === "success" ? prev : { status: "loading" }));
            try {
                const data = await stableApi.fetchSummary(endpoint);
                if (disposed) {
                    return;
                }
                setState({ status: "success", data });

                const pollSeconds = data.pollingIntervalSeconds ?? DEFAULT_POLL_INTERVAL / 1000;
                const pollInterval = Math.max(pollSeconds * 1000, MIN_POLL_INTERVAL);
                scheduleReload(pollInterval);
            } catch (error) {
                if (disposed) {
                    return;
                }
                const err = error instanceof Error ? error : new Error("Unknown error");
                setState({ status: "error", error: err });
                // Don't retry on error - only reload on success
                // This prevents infinite failed API calls
                console.error("[useDeploymentSummary] Failed to fetch summary:", err.message);
            }
        };

        void loadSummary();

        return () => {
            disposed = true;
            if (timeoutRef.current) {
                window.clearTimeout(timeoutRef.current);
                timeoutRef.current = null;
            }
        };
    }, [endpoint, stableApi, pollingEnabled]);

    return state;
}
