import { PropsWithChildren, ReactNode } from "react";
import { NavLink } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { LanguageSwitcher } from "../components/LanguageSwitcher/LanguageSwitcher";

type NavItem = {
    path: string;
    labelKey: string;
    descriptionKey: string;
    icon: ReactNode;
    exact?: boolean;
};

export function ShellLayout({ children }: PropsWithChildren): JSX.Element {
    const { t } = useTranslation();

    const navItems: NavItem[] = [
        {
            path: "/overview",
            labelKey: "navigation.overviewLabel",
            descriptionKey: "navigation.overviewDesc",
            icon: "📊",
            exact: true
        },
        {
            path: "/releases",
            labelKey: "navigation.releasesLabel",
            descriptionKey: "navigation.releasesDesc",
            icon: "🚀"
        },
        {
            path: "/settings/policies",
            labelKey: "navigation.policiesLabel",
            descriptionKey: "navigation.policiesDesc",
            icon: "🛡️"
        }
    ];

    return (
        <div className="dp-shell" data-testid="deploy-platform-shell">
            <aside className="dp-shell__sidebar">
                <header className="dp-shell__brand">
                    <span className="dp-shell__brand-icon" aria-hidden="true">
                        🚀
                    </span>
                    <div className="dp-shell__brand-text">
                        <h1>{t('shell.brandTitle')}</h1>
                        <p>{t('shell.brandSubtitle')}</p>
                    </div>
                </header>

                <nav className="dp-shell__nav" aria-label={t('shell.navLabel')}>
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
                                <span className="dp-shell__nav-label">{t(item.labelKey)}</span>
                                <span className="dp-shell__nav-description">{t(item.descriptionKey)}</span>
                            </span>
                        </NavLink>
                    ))}
                </nav>
            </aside>

            <section className="dp-shell__content">
                <header className="dp-shell__toolbar">
                    <div>
                        <h2 className="dp-shell__toolbar-title">{t('shell.toolbarTitle')}</h2>
                        <p className="dp-shell__toolbar-subtitle">
                            {t('shell.toolbarSubtitle')}
                        </p>
                    </div>
                    <div className="dp-shell__toolbar-actions">
                        <LanguageSwitcher />
                        <button type="button" className="dp-btn dp-btn--secondary">
                            {t('shell.refreshData')}
                        </button>
                        <button type="button" className="dp-btn dp-btn--primary">
                            {t('shell.newRelease')}
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

