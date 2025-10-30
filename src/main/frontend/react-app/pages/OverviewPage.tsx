import { useMemo } from "react";

import { useDeploymentSummary } from "../hooks/useDeploymentSummary";
import type { DeploymentSummaryItem } from "../overview/api";

const statusLabelMap: Record<DeploymentSummaryItem["status"], string> = {
    healthy: "健康",
    warning: "告警",
    failed: "失败",
    pending: "待处理"
};

const resolveSummaryEndpoint = (): string | undefined => {
    if (typeof document === "undefined") {
        return undefined;
    }
    const container = document.getElementById("deploy-platform-root");
    const endpoint = container?.dataset?.summaryEndpoint;
    return endpoint && endpoint.trim().length > 0 ? endpoint : undefined;
};

export function OverviewPage(): JSX.Element {
    const summary = useDeploymentSummary({
        endpoint: resolveSummaryEndpoint()
    });

    const highlightColumns = useMemo(() => {
        if (summary.status !== "success") {
            return [];
        }
        return summary.data.highlights ?? [];
    }, [summary]);

    return (
        <section className="dp-page" data-testid="overview-page">
            <header className="dp-page__header">
                <div>
                    <h3 className="dp-page__title">发布实时总览</h3>
                    <p className="dp-page__description">
                        查看关键指标、待处理发布与风险提示。
                    </p>
                </div>
                {summary.status === "success" && (
                    <span className="dp-overview__last-updated">
                        最近更新：{summary.data.highlights?.[0]?.lastUpdated ?? "刚刚"}
                    </span>
                )}
            </header>

            {summary.status === "loading" && (
                <div className="dp-page__placeholder" data-testid="overview-loading">
                    正在加载最新的部署指标与发布动态…
                </div>
            )}

            {summary.status === "error" && (
                <div className="dp-page__placeholder dp-page__placeholder--error" data-testid="overview-error">
                    获取部署总览失败：{summary.error.message}
                </div>
            )}

            {summary.status === "success" && (
                <>
                    <section className="dp-overview__metrics" aria-label="关键指标">
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

                    <section className="dp-overview__highlights" aria-label="重点关注">
                        <header className="dp-section__title">重点关注</header>
                        {highlightColumns.length === 0 ? (
                            <p className="dp-page__placeholder">暂无待处理的发布或风险。</p>
                        ) : (
                            <ul className="dp-overview__highlight-list">
                                {highlightColumns.map((item, index) => (
                                    <li key={`${item.application}-${item.environment}-${index}`}>
                                        <span className="dp-overview__highlight-app">
                                            {item.application} · {item.environment}
                                        </span>
                                        <span className={`dp-overview__highlight-status dp-status--${item.status}`}>
                                            {statusLabelMap[item.status]}
                                        </span>
                                        <span className="dp-overview__highlight-meta">
                                            待审批发布 {item.pendingReleases} 条 · 运行中金丝雀 {item.runningCanary} 条
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
