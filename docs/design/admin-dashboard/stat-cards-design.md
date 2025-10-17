# 管理员仪表盘统计卡片设计规范

**版本**: v0.2
**更新时间**: 2025-09-29
**适用范围**: Admin Dashboard 首页核心统计卡片（在线服务器、活跃用户、未处理告警、系统健康度）

## 1. 设计目标
- 通过 3 秒即可感知的视觉层级呈现关键指标，支持管理员快速判断。
- 统一 Workspace UI 的尺寸、圆角、阴影与交互反馈，确保体验一致。
- 为正常、预警、严重、离线，以及加载、空数据、错误等数据态提供一致反馈。
- ≥1280px 桌面端采用四列排布，窄屏环境保持信息完整与可用。

## 2. 组件结构
```
.stats-card
 ├─ .stats-card__header
 │   ├─ .stats-card__icon      // 24x24 token icon / 状态色背景
 │   └─ .stats-card__label     // 指标名称 + 二级描述（可选）
 ├─ .stats-card__value-row
 │   ├─ .stats-card__value     // 主数值，支持单位修饰
 │   └─ .stats-card__delta     // 趋势/同比信息（可选）
 └─ .stats-card__footer
     ├─ .stats-card__meta      // 辅助信息，如更新时间、在线率
     └─ .stats-card__actions   // 快捷操作按钮组（最多 2 个，16px icon 按钮）
```

## 3. 尺寸与排布
- 卡片默认尺寸 240px (W) × 160px (H)，最小宽度 208px，可随父容器自适应。
- 栅格间距 `clamp(16px, 2vw, 24px)`；内部间距使用 `--spacing-5` / `--spacing-6`。
- 圆角 `--radius-2xl`（16px）；阴影默认 `--shadow-sm`，Hover 升级为 `--shadow-lg`。
- 布局：≥1600px 四列等宽；1280-1599px 自动 3 列；1024-1279px 2 列；≤680px 单列。

## 4. 视觉规范
| 元素         | Token / 样式说明 |
|--------------|------------------|
| 背景         | `--bg-primary`；状态色额外叠加 `rgba(state, 0.08)` |
| 边框         | 默认 `1px solid var(--border-primary)`；状态强调使用 3px 左侧色条 |
| 主值字体     | `var(--font-size-4xl)` + `var(--font-weight-extrabold)` |
| 标签字体     | `var(--font-size-sm)` + `var(--font-weight-medium)` |
| 趋势文本     | `var(--font-size-base)` + 状态色；向上箭头 `▲` 用成功色，向下用错误色 |
| 元数据       | `var(--text-secondary)`，字号 `var(--font-size-sm)` |
| 图标容器     | 32x32，圆角 12px，背景为状态色 16% 透明度 |

## 5. 状态定义
| 状态       | 触发条件示例                                   | 配色要点 |
|------------|-----------------------------------------------|----------|
| Default    | 指标在阈值内（如在线服务器 ≥95 台、告警 ≤5 条） | 品牌蓝；标准边框 |
| Success    | 超过目标（如活跃用户超标或环比提升显著）       | `--color-success` 左侧强调条与图标 |
| Warning    | 接近风险阈值（如在线率 <90%）                  | `--color-warning` 渐变背景提示风险 |
| Critical   | 超出阈值或阻断性告警（未处理告警 >10 条）      | `--color-error`，主值文本用深红 |
| Offline    | 数据源不可用或连接失败                        | `--text-muted`，背景用 `--surface-muted` |

## 6. 数据态
- **加载中**： `.stats-card--skeleton` 骨架屏，隐藏交互按钮。
- **空数据**：展示“暂无数据”与 16px 图标，保持整体尺寸不变。
- **错误态**：突出错误信息并提供重试按钮，背景使用 `--color-error-bg`。
- **可点击态**：卡片添加 `button` 语义，Hover 增强阴影，Focus 出现 2px 内描边 `--focus-ring`。

## 7. 交互规范
- Hover：`transform: translateY(-2px)` + `box-shadow: var(--shadow-lg)`。
- Active：`transform: translateY(0)`，阴影降为 `--shadow-xs`。
- Focus：`outline: 2px solid var(--focus-ring)`，`outline-offset: 2px`。
- 键盘操作：`Enter` / `Space` 触发主操作，`Tab` 可访问 footer 操作按钮。
- 趋势数值变化触发 320ms 渐入+上浮动画；尊重 `prefers-reduced-motion`。

## 8. 可访问性
- 文本与背景对比度 ≥4.5:1，状态色 Hover 后仍 ≥3:1。
- 卡片 `role="group"`，通过 `aria-labelledby`、`aria-describedby` 关联标题与元信息。
- 实时数据更新使用 `aria-live="polite"`，避免打断读屏器。
- 图标按钮补充 `aria-label`；禁用态 `aria-disabled="true"`。

## 9. 响应式与自适应
- <1024px：主值降为 `var(--font-size-3xl)`，footer 改为单列。
- <680px：header 改水平排布（图标左、文本右），趋势信息下移至 footer。
- 暗色模式：背景 `--bg-elevated-dark`，文本 `--text-primary-dark`，状态背景透明度增至 18%。
- 触控屏：整体 padding 使用 `--spacing-7`，操作按钮命中面积 ≥44px。

## 10. 数据绑定约定
```json
{
  "metric": "server_online_rate",
  "label": "在线服务器",
  "value": 89,
  "total": 100,
  "unit": "台",
  "delta": {
    "value": -4,
    "trend": "down",
    "period": "vs. 昨日"
  },
  "status": "warning",
  "updatedAt": "2025-09-29T08:30:00Z",
  "actions": [
    { "type": "link", "label": "查看详情", "href": "/admin/servers" },
    { "type": "command", "label": "刷新", "action": "refreshMetric" }
  ]
}
```
- 状态字段映射到 `.stats-card--{status}` 类；缺失字段需优雅降级（如无 `delta` 时隐藏趋势）。
- 四个指标标识固定为：`server_online_rate`、`active_users_today`、`alerts_unresolved`、`system_health_score`。

## 11. 开发指引
1. 将统计卡片样式抽离到 `src/main/resources/static/admin-dashboard/stat-cards.css`。
2. 在 Thymeleaf 中创建复用片段 `fragments/stats-card.html` 避免重复标记。
3. 对接 `/admin/dashboard/overview` 接口，校验数据与状态映射逻辑。
4. 编写组件级 Jest/Playwright 测试，覆盖状态色渲染与可访问性属性。

## 12. Demo 说明
- Demo 文件：`docs/design/prototypes/admin-stat-cards-demo.html`。
- 演示在线服务器、活跃用户、未处理告警、系统健康度四张卡片，覆盖 Warning、Success、Critical、Offline 等状态。集成时需将样式迁移至正式样式库，并替换为真实数据。

## 13. 任务列表
- [x] 整理并抽取统计卡片样式至 `src/main/resources/static/admin-dashboard/stat-cards.css`
- [x] 创建 Thymeleaf 片段 `fragments/stats-card.html` 并替换模板中重复结构
- [x] 对接 `/admin/dashboard/overview` 接口，校验四项指标字段及状态映射
- [x] 为统计卡片编写 Playwright/Jest 测试，覆盖可访问性与状态切换
- [x] 在开发环境验证暗色模式与移动端响应式表现




\n## 14. 验证记录\n- 2025-09-29 | 运行 Python 脚本 	ests/test_admin_dashboard_cards.py 校验统计卡片结构、数据字段及 ria/data-* 属性，确保自动化检测通过。\n- 2025-09-29 | 浏览器 DevTools 检查 prefers-color-scheme: dark 与 <768px 栅格断点，确认暗色模式与移动端布局变量生效，未出现溢出或对比度问题。\n

