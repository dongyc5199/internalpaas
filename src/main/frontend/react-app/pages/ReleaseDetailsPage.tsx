import { useParams } from "react-router-dom";

export function ReleaseDetailsPage(): JSX.Element {
    const { releaseId } = useParams<{ releaseId: string }>();

    return (
        <section className="dp-page dp-page--details" data-testid="release-details-page">
            <header className="dp-page__header">
                <h3 className="dp-page__title">发布详情</h3>
                <p className="dp-page__description">
                    发布 ID：<code>{releaseId}</code>，后续将展示阶段、守门指标与审批轨迹。
                </p>
            </header>

            <div className="dp-page__placeholder">
                发布详情内容将在后续任务中实现，包括时间轴、指标可视化与审批操作。
            </div>
        </section>
    );
}

