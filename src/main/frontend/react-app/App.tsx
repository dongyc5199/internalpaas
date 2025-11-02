import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { I18nextProvider } from "react-i18next";

import "./index.css";
import { LayoutProvider } from "./providers/LayoutProvider";
import { LayoutSelector } from "./components/LayoutSelector";
import { OverviewPage } from "./pages/OverviewPage";
import { ReleaseDetailsPlaceholder, ReleasesPage } from "./pages/ReleasesPage";
import { ReleaseDetailsPage } from "./pages/ReleaseDetailsPage";
import { PoliciesPage } from "./pages/PoliciesPage";
import { queryClient } from "./config/queryClient";
import i18n from "./i18n/i18n";
import { useTokenRefresh } from "./hooks/useTokenRefresh";

type AppProps = {
    basename?: string;
};

const DEFAULT_BASENAME = "/";

const resolveBasename = (): string => {
    if (typeof document === "undefined") {
        return DEFAULT_BASENAME;
    }

    const container = document.getElementById("deploy-platform-root");
    const attributeBasename = container?.dataset?.routerBase;
    if (attributeBasename && attributeBasename.trim().length > 0) {
        return attributeBasename.trim();
    }

    return DEFAULT_BASENAME;
};

export function AppRoutes(): JSX.Element {
    return (
        <LayoutSelector>
            <Routes>
                <Route path="/" element={<Navigate to="/overview" replace />} />
                <Route path="/overview" element={<OverviewPage />} />
                <Route path="/releases" element={<ReleasesPage />}>
                    <Route index element={<ReleaseDetailsPlaceholder />} />
                    <Route path=":releaseId" element={<ReleaseDetailsPage />} />
                </Route>
                <Route path="/settings/policies" element={<PoliciesPage />} />
                <Route path="*" element={<Navigate to="/overview" replace />} />
            </Routes>
        </LayoutSelector>
    );
}

export function App({ basename }: AppProps = {}): JSX.Element {
    const base = basename ?? resolveBasename();

    // Initialize token refresh mechanism
    useTokenRefresh();

    return (
        <QueryClientProvider client={queryClient}>
            <I18nextProvider i18n={i18n}>
                <LayoutProvider>
                    <BrowserRouter basename={base}>
                        <AppRoutes />
                    </BrowserRouter>
                </LayoutProvider>
            </I18nextProvider>
            <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
    );
}

export default App;
