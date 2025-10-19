# 🧪 P0-Day5 图表验证测试方案

本文档提供了 4 种测试方案来验证 Admin Dashboard 图表功能。

---

## 📋 方案概览

| 方案 | 类型 | 自动化程度 | 适用场景 |
|------|------|------------|---------|
| **方案 1** | Playwright E2E | ⭐⭐⭐⭐⭐ 全自动 | CI/CD 集成,回归测试 |
| **方案 2** | 浏览器控制台脚本 | ⭐⭐⭐⭐ 半自动 | 快速验证,本地开发 |
| **方案 3** | 手动检查清单 | ⭐⭐ 手动 | 首次验证,问题排查 |
| **方案 4** | Visual Regression | ⭐⭐⭐⭐ 自动 | UI 一致性测试 |

---

## 🚀 方案 1: Playwright E2E 测试 (推荐)

### 📝 说明
使用 Playwright 编写的端到端测试,自动化验证图表渲染、尺寸、日志等。

### ▶️ 运行步骤

```bash
# 1. 确保应用运行在 9091 端口
# (已运行,PID: 6360)

# 2. 运行特定测试文件
npx playwright test e2e/tests/admin-dashboard-charts.spec.ts

# 3. 运行测试并查看报告
npx playwright test e2e/tests/admin-dashboard-charts.spec.ts --reporter=html

# 4. 运行测试并生成截图 (失败时自动截图)
npx playwright test e2e/tests/admin-dashboard-charts.spec.ts --screenshot=on

# 5. 运行测试并打开浏览器 (调试模式)
npx playwright test e2e/tests/admin-dashboard-charts.spec.ts --headed --debug
```

### ✅ 测试内容

1. **容器存在性测试** - 验证 3 个 Canvas 元素存在
2. **尺寸验证测试** - 验证 Canvas 尺寸 > 0
3. **日志验证测试** - 捕获初始化日志
4. **实例验证测试** - 验证 `window.dashboard` 存在
5. **截图测试** - 自动保存图表截图到 `e2e/screenshots/`
6. **像素验证测试** - 检查 Canvas 是否真实渲染

### 📊 预期输出

```
Running 6 tests using 1 worker

  ✓ [chromium] › admin-dashboard-charts.spec.ts:20 应该成功加载 3 个图表容器 (2s)
  ✓ [chromium] › admin-dashboard-charts.spec.ts:35 Canvas 元素应该有正确的尺寸 (1s)
  ✓ [chromium] › admin-dashboard-charts.spec.ts:75 应该看到 Chart.js 初始化日志 (3s)
  ✓ [chromium] › admin-dashboard-charts.spec.ts:100 应该能够验证 Chart.js 实例存在 (1s)
  ✓ [chromium] › admin-dashboard-charts.spec.ts:115 应该能够截图保存图表效果 (2s)
  ✓ [chromium] › admin-dashboard-charts.spec.ts:138 Canvas 应该有可见的像素内容 (1s)

  6 passed (10s)
```

---

## 🔍 方案 2: 浏览器控制台验证脚本

### 📝 说明
在浏览器控制台直接运行的 JavaScript 脚本,快速验证图表状态。

### ▶️ 运行步骤

1. 打开浏览器访问 http://localhost:9091/admin/workspace
2. 按 **F12** 打开开发者工具
3. 切换到 **Console** 标签页
4. 复制 `scripts/verify-dashboard-charts.js` 的完整内容
5. 粘贴到控制台并按回车执行

### ✅ 测试内容

- ✅ Canvas 元素存在性检查
- ✅ Canvas 尺寸验证 (offsetWidth/Height 和 canvas.width/height)
- ✅ Dashboard 实例检查
- ✅ 全局函数检查
- ✅ Canvas 渲染内容检测 (像素分析)
- ✅ 图表区域可见性检查

### 📊 预期输出

```
🔍 开始验证 Dashboard 图表...

📋 测试 1: 检查 Canvas 元素...
  ✅ server-status-chart: 存在
  ✅ resource-usage-chart: 存在
  ✅ realtime-monitor-chart: 存在

📐 测试 2: 检查 Canvas 尺寸...
  📊 server-status-chart:
     - 显示尺寸: 320 x 240
     - Canvas尺寸: 320 x 240
  📊 resource-usage-chart:
     - 显示尺寸: 320 x 240
     - Canvas尺寸: 320 x 240
  📊 realtime-monitor-chart:
     - 显示尺寸: 640 x 240
     - Canvas尺寸: 640 x 240

🔧 测试 3: 检查 Dashboard 实例...
  ✅ window.dashboard: 存在
  📊 图表实例数量: 3

🌍 测试 4: 检查全局函数...
  ✅ window.initDashboardCharts: 存在

🎨 测试 5: 检查 Canvas 渲染内容...
  ✅ server-status-chart: 包含渲染内容
  ✅ resource-usage-chart: 包含渲染内容
  ✅ realtime-monitor-chart: 包含渲染内容

👁️ 测试 6: 检查图表区域可见性...
  ✅ 图表区域尺寸: 1200 x 600

============================================================
📊 测试报告
============================================================

✅ 通过测试 (9):
   ✅ 所有 3 个 Canvas 元素都存在
   ✅ 所有 Canvas 都有正确的尺寸
   ✅ window.dashboard 实例存在
   ✅ 3 个图表实例已创建
   ✅ window.initDashboardCharts 函数存在
   ✅ 至少一个图表包含渲染内容
   ✅ 图表区域可见

📈 测试通过率: 6/6 (100.0%)

🎉 所有测试通过! 图表渲染正常!
============================================================
```

---

## ✋ 方案 3: 手动检查清单

### 📝 说明
逐项检查的手动测试清单,适合首次验证或问题排查。

### ✅ 检查项

#### 1. 页面加载验证
- [ ] 访问 http://localhost:9091/admin/workspace
- [ ] 页面无 404/500 错误
- [ ] 无 JavaScript 致命错误

#### 2. 控制台日志验证
打开 F12 → Console,确认以下日志:
- [ ] `[SPA] Chart.js 图表初始化已触发`
- [ ] `[Dashboard] 开始初始化图表...`
- [ ] `[Dashboard] 服务器状态图表已创建`
- [ ] `[Dashboard] 资源使用率图表已创建`
- [ ] `[Dashboard] 实时监控图表已创建`
- [ ] `[Dashboard] 图表初始化完成`

#### 3. 视觉验证
滚动到图表区域,确认:
- [ ] 看到 3 个图表容器
- [ ] **服务器状态分布** (饼图) 显示在左侧
- [ ] **系统资源使用率** (柱状图) 显示在右侧
- [ ] **实时资源监控** (折线图) 显示在下方全宽
- [ ] 图表有颜色、图例、坐标轴
- [ ] 无空白占位符或错误提示

#### 4. 元素审查验证
F12 → Elements,选中 Canvas 元素:
- [ ] `<canvas id="server-status-chart">` 存在
- [ ] Canvas Computed 样式: `width > 0`, `height > 0`
- [ ] Canvas 属性: `width` 和 `height` 不为 0

#### 5. 网络验证
F12 → Network:
- [ ] `main.js` (278.31 kB) 加载成功 (200 状态)
- [ ] `main.css` 加载成功
- [ ] 无关键资源 404 错误

#### 6. 交互验证 (可选)
- [ ] 鼠标悬停图表显示 Tooltip
- [ ] 图表图例可点击切换数据系列
- [ ] WebSocket 连接尝试 (预期失败,后端未实现)

---

## 📸 方案 4: Visual Regression Testing

### 📝 说明
使用 Playwright 的截图对比功能,确保 UI 一致性。

### ▶️ 运行步骤

```bash
# 1. 首次运行生成基准截图
npx playwright test e2e/tests/admin-dashboard-charts.spec.ts --update-snapshots

# 2. 后续运行对比截图差异
npx playwright test e2e/tests/admin-dashboard-charts.spec.ts

# 3. 查看差异报告
npx playwright show-report
```

### 📂 截图保存位置

```
e2e/
├── screenshots/              # 手动截图
│   ├── dashboard-charts.png  # 整体图表区域
│   ├── chart-server-status.png
│   ├── chart-resource-usage.png
│   └── chart-realtime-monitor.png
└── tests/
    └── admin-dashboard-charts.spec.ts-snapshots/  # 基准截图
        └── chromium/
```

---

## 🎯 推荐使用顺序

### 首次验证 (Day 5)
1. ✅ **方案 3** (手动检查) - 快速确认图表可见
2. ✅ **方案 2** (控制台脚本) - 验证技术细节
3. ✅ **方案 1** (E2E 测试) - 生成自动化测试和截图
4. ✅ **方案 4** (Visual Regression) - 建立基准截图

### 后续回归测试
- 🔄 **方案 1** (E2E 测试) - 每次代码变更后自动运行
- 🔄 **方案 4** (Visual Regression) - 检测 UI 变化

### 问题排查
- 🔍 **方案 2** (控制台脚本) - 快速定位问题
- 🔍 **方案 3** (手动检查) - 逐步验证每个环节

---

## 📝 生成完成报告

所有测试通过后,运行以下命令生成报告:

```bash
# 生成 HTML 测试报告
npx playwright test --reporter=html

# 生成 JUnit XML 报告 (CI 集成)
npx playwright test --reporter=junit

# 生成 JSON 报告 (自定义处理)
npx playwright test --reporter=json
```

---

## 🐛 常见问题

### Q1: Playwright 测试超时
**原因:** 应用启动慢或图表初始化延迟  
**解决:** 增加 `waitForTimeout` 时间

### Q2: 截图全黑或空白
**原因:** Canvas 尚未渲染完成  
**解决:** 在截图前增加等待时间

### Q3: 控制台脚本报错
**原因:** 在错误的页面执行或图表未初始化  
**解决:** 确保在 `/admin/workspace` 页面执行,且等待加载完成

---

## 📚 相关文档

- [Playwright 官方文档](https://playwright.dev/)
- [Chart.js 文档](https://www.chartjs.org/)
- [项目 E2E 测试指南](../e2e/README.md)
- [前端开发指南](../docs/development/frontend-dev-summary-guide.md)

---

**创建日期:** 2025-01-17  
**作者:** GitHub Copilot  
**关联任务:** P0-Day5 Dashboard HTML 集成浏览器测试
