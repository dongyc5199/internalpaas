import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1]
content_path = root / 'src' / 'main' / 'resources' / 'templates' / 'admin' / 'admin-dashboard-content.html'
css_path = root / 'src' / 'main' / 'resources' / 'static' / 'admin-dashboard' / 'stat-cards.css'
fragment_path = root / 'src' / 'main' / 'resources' / 'templates' / 'fragments' / 'stats-card.html'

missing = []
if not content_path.exists():
    missing.append(f'Missing template: {content_path}')
if not css_path.exists():
    missing.append(f'Missing CSS: {css_path}')
if not fragment_path.exists():
    missing.append(f'Missing fragment: {fragment_path}')
if missing:
    raise SystemExit('\n'.join(missing))

content = content_path.read_text(encoding='utf-8')
required_ids = [
    'dashboardServersOnline',
    'dashboardServersTotal',
    'dashboardServersMaintaining',
    'dashboardServersOffline',
    'dashboardServersRate',
    'dashboardUsersActiveToday',
    'dashboardUsersDelta',
    'dashboardUsersTotal',
    'dashboardAlertsUnresolved',
    'dashboardAlertsDelta',
    'dashboardAlertsRules',
    'dashboardHealthScore',
    'dashboardHealthStatus',
    'dashboardHealthUpdatedAt'
]
for element_id in required_ids:
    if f'id="{element_id}"' not in content:
        raise AssertionError(f'Missing element id="{element_id}" in admin-dashboard-content.html')

# Accessibility checks
if 'aria-live="polite"' not in content:
    raise AssertionError('Expected aria-live="polite" for stat meta sections')
if 'data-suffix="%" id="dashboardUsersDelta"' not in content:
    raise AssertionError('dashboardUsersDelta should declare data-suffix for formatting')
if 'data-suffix="条" id="dashboardAlertsDelta"' not in content:
    raise AssertionError('dashboardAlertsDelta should declare data-suffix for alerts count')

css = css_path.read_text(encoding='utf-8')
required_selectors = [
    '.admin-enhanced .stats-card',
    '.admin-enhanced .stats-card .stats-delta',
    '.admin-enhanced .stats-card .stat-meta',
    '.admin-enhanced .stats-card .stat-annotation'
]
for selector in required_selectors:
    if selector not in css:
        raise AssertionError(f'Missing CSS selector "{selector}" in stat-cards.css')

fragment = fragment_path.read_text(encoding='utf-8')
if 'th:fragment="statsCard' not in fragment:
    raise AssertionError('stats-card fragment definition missing')
if 'th:insert="~{::body}"' not in fragment:
    raise AssertionError('stats-card fragment should expose body slot via th:insert="~{::body}"')

print('Admin dashboard stat card structure checks passed.')
