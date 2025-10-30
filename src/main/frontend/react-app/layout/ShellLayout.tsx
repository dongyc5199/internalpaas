import { PropsWithChildren, ReactNode } from "react";
import { NavLink } from "react-router-dom";

type NavItem = {
    path: string;
    label: string;
    description: string;
    icon: ReactNode;
    exact?: boolean;
};

const navItems: NavItem[] = [
    {
        path: "/overview",
        label: "总览",
        description: "实时掌握部署健康度与待办",
        icon: "📊",
        exact: true
    },
    {
        path: "/releases",
        label: "发布",
        description: "查看发布流水线与审批进度",
        icon: "🚀"
    },
    {
        path: "/settings/policies",
        label: "策略",
        description: "配置金丝雀、审批与门禁策略",
        icon: "🛡️"
    }
];

export function ShellLayout({ children }: PropsWithChildren): JSX.Element {
    return (
        <div className="dp-shell" data-testid="deploy-platform-shell">
            <aside className="dp-shell__sidebar">
                <header className="dp-shell__brand">
                    <span className="dp-shell__brand-icon" aria-hidden="true">
                        🚀
                    </span>
                    <div className="dp-shell__brand-text">
                        <h1>部署管理平台</h1>
                        <p>统一发布 · 灰度 · 审批 · 审计</p>
                    </div>
                </header>

                <nav className="dp-shell__nav" aria-label="部署管理导航">
                    {navItems.map((item) => (
                        <NavLink
                            key={item.path}
                            to={item.path}
                            className={({ isActive }) =>
                                [
                                    "dp-shell__nav-item",
                                    isActive ? "dp-shell__nav-item--active" : "",
                                    item.exact ? "dp-shell__nav-item--exact" : ""
                                ]
                                    .filter(Boolean)
                                    .join(" ")
                            }
                            end={item.exact}
                        >
                            <span className="dp-shell__nav-icon" aria-hidden="true">
                                {item.icon}
                            </span>
                            <span className="dp-shell__nav-body">
                                <span className="dp-shell__nav-label">{item.label}</span>
                                <span className="dp-shell__nav-description">{item.description}</span>
                            </span>
                        </NavLink>
                    ))}
                </nav>
            </aside>

            <section className="dp-shell__content">
                <header className="dp-shell__toolbar">
                    <div>
                        <h2 className="dp-shell__toolbar-title">部署活动总览</h2>
                        <p className="dp-shell__toolbar-subtitle">
                            聚合部署、灰度、审批与数据作业的实时态势
                        </p>
                    </div>
                    <div className="dp-shell__toolbar-actions">
                        <button type="button" className="dp-btn dp-btn--secondary">
                            刷新数据
                        </button>
                        <button type="button" className="dp-btn dp-btn--primary">
                            新建发布
                        </button>
                    </div>
                </header>

                <main className="dp-shell__main" data-testid="deploy-platform-content">
                    {children}
                </main>
            </section>
        </div>
    );
}

