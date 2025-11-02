import { useMemo } from "react";
import { useTranslation } from "react-i18next";

import { useDeploymentSummary } from "../hooks/useDeploymentSummary";
import type { DeploymentSummaryItem } from "../overview/api";

const resolveSummaryEndpoint = (): string | undefined => {
    if (typeof document === "undefined") {
        return undefined;
    }
    const container = document.getElementById("deploy-platform-root");
    const endpoint = container?.dataset?.summaryEndpoint;
    return endpoint && endpoint.trim().length > 0 ? endpoint : undefined;
};

export function OverviewPage(): JSX.Element {
    const { t } = useTranslation();
    const summary = useDeploymentSummary({
        endpoint: resolveSummaryEndpoint()
    });

    const highlightColumns = useMemo(() => {
        if (summary.status !== "success") {
            return [];
        }
        return summary.data.highlights ?? [];
    }, [summary]);

    const getStatusLabel = (status: DeploymentSummaryItem["status"]): string => {
        const statusMap: Record<DeploymentSummaryItem["status"], string> = {
            healthy: t('overview.statusHealthy'),
            warning: t('overview.statusWarning'),
            failed: t('overview.statusFailed'),
            pending: t('overview.statusPending')
        };
        return statusMap[status];
    };

    return (
        <section className="dp-page" data-testid="overview-page">
            <header className="dp-page__header">
                <div>
                    <h3 className="dp-page__title">{t('overview.pageTitle')}</h3>
                    <p className="dp-page__description">
                        {t('overview.pageDescription')}
                    </p>
                </div>
                {summary.status === "success" && (
                    <span className="dp-overview__last-updated">
                        {t('overview.lastUpdated')}{summary.data.highlights?.[0]?.lastUpdated ?? t('overview.justNow')}
                    </span>
                )}
            </header>

            {summary.status === "loading" && (
                <div className="dp-page__placeholder" data-testid="overview-loading">
                    {t('overview.loading')}
                </div>
            )}

            {summary.status === "error" && (
                <div className="dp-page__placeholder dp-page__placeholder--error" data-testid="overview-error">
                    {t('overview.loadError')}{summary.error.message}
                </div>
            )}

            {summary.status === "success" && (
                <>
                    <section className="dp-overview__metrics" aria-label={t('overview.metricsSection')}>
                        {summary.data.metrics.map((metric) => (
                            <article
                                key={metric.id}
                                className="dp-overview__metric-card"
                                data-testid={`metric-${metric.id}`}
                            >
                                <span className="dp-overview__metric-title">{metric.title}</span>
                                <div className="dp-overview__metric-value">
                                    <strong>
                                        {metric.value}
                                        {metric.unit ?? ""}
                                    </strong>
                                    {metric.trend && (
                                        <span className={`dp-overview__metric-trend dp-overview__metric-trend--${metric.trend}`}>
                                            {metric.trend === "up" ? "↑" : metric.trend === "down" ? "↓" : "→"}
                                        </span>
                                    )}
                                </div>
                                <p className="dp-overview__metric-description">{metric.description}</p>
                            </article>
                        ))}
                    </section>

                    <section className="dp-overview__highlights" aria-label={t('overview.highlightsSection')}>
                        <header className="dp-section__title">{t('overview.highlightsSection')}</header>
                        {highlightColumns.length === 0 ? (
                            <p className="dp-page__placeholder">{t('overview.noHighlights')}</p>
                        ) : (
                            <ul className="dp-overview__highlight-list">
                                {highlightColumns.map((item, index) => (
                                    <li key={`${item.application}-${item.environment}-${index}`}>
                                        <span className="dp-overview__highlight-app">
                                            {item.application} · {item.environment}
                                        </span>
                                        <span className={`dp-overview__highlight-status dp-status--${item.status}`}>
                                            {getStatusLabel(item.status)}
                                        </span>
                                        <span className="dp-overview__highlight-meta">
                                            {t('overview.pendingReleasesCount', { count: item.pendingReleases })} · {t('overview.runningCanaryCount', { count: item.runningCanary })}
                                        </span>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </section>
                </>
            )}
        </section>
    );
}
