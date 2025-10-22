# 前端工程化目录说明

## 目录结构

```
src/main/frontend/
├── assets/          # 静态资源 (图标、字体等)
├── modules/         # 功能模块
│   ├── server-modal.ts
│   └── server-group-management.ts
├── styles/          # 样式文件
│   ├── tokens/      # 设计令牌系统
│   │   ├── colors.css
│   │   ├── typography.css
│   │   ├── spacing.css
│   │   ├── shadows.css
│   │   └── index.css
│   └── main.css     # 主样式入口
├── utils/           # 工具模块
│   ├── http.ts      # HTTP客户端
│   ├── event-bus.ts # 事件总线
│   ├── storage.ts   # 存储封装
│   └── index.ts     # 统一导出
├── tests/           # 测试用例
│   ├── bootstrap.test.ts
│   └── server-modal.test.ts
└── main.ts          # 主入口文件
```

## 第三方库管理

### 已安装的npm包

- **chart.js**: 图表库 (v4.5.0)

### 使用方式

```typescript
// 通过npm安装并导入
import { Chart } from "chart.js";
```

### 添加新的第三方库

```bash
# 安装为生产依赖
npm install <package-name>

# 安装为开发依赖
npm install -D <package-name>
```

## 设计令牌系统

项目使用CSS变量实现设计令牌系统,所有样式应使用令牌而非硬编码值:

```css
/* ✅ 推荐 */
.button {
    color: var(--color-primary);
    padding: var(--spacing-4);
    border-radius: var(--radius-md);
}

/* ❌ 不推荐 */
.button {
    color: #6366f1;
    padding: 16px;
    border-radius: 8px;
}
```

## 工具模块使用

### HTTP客户端

```typescript
import { http } from "@/utils";

// GET请求
const { data } = await http.get("/api/servers");

// POST请求
await http.post("/api/servers", { name: "server-1" });
```

### 事件总线

```typescript
import { eventBus } from "@/utils";

// 订阅事件
const unsubscribe = eventBus.on("server:created", (data) => {
    console.log("Server created:", data);
});

// 发布事件
eventBus.emit("server:created", { id: "123" });

// 取消订阅
unsubscribe();
```

### 存储管理

```typescript
import { localStorage, sessionStorage } from "@/utils";

// 设置(带过期时间)
localStorage.set("token", "abc123", { expires: 3600000 }); // 1小时后过期

// 获取
const token = localStorage.get<string>("token");

// 移除
localStorage.remove("token");
```

### 格式化工具

```typescript
import {
    formatBytes,
    formatUptime,
    formatPercentage,
    formatTimestamp,
    formatNumber
} from "@/utils";

// 格式化字节
formatBytes(1024); // "1 KB"
formatBytes(1048576); // "1 MB"

// 格式化运行时间
formatUptime(3600); // "1小时 0分钟"
formatUptime(90000); // "1天 1小时"

// 格式化百分比
formatPercentage(85.678); // "85.7%"

// 格式化时间戳
formatTimestamp(Date.now()); // "2025-10-11 12:34:56"

// 数字千分位格式化
formatNumber(1234567.89, 2); // "1,234,567.89"
```

### 国际化工具

```typescript
import { setLocalizedText, updateMetricSummaryText, refreshContainerI18n } from "@/utils";

// 设置元素本地化文本
const element = document.getElementById("title");
setLocalizedText(element, "服务器", "Server");

// 更新指标摘要文本
const container = document.getElementById("metrics");
updateMetricSummaryText(container, ".cpu-usage", "CPU: 75%", "CPU: 75%");

// 刷新容器内所有i18n元素
refreshContainerI18n(document.body, "en");
```

### 通知工具

```typescript
import { safeShowToast, showSuccess, showError, showProgressNotification } from "@/utils";

// 显示通知
safeShowToast("操作成功", "success");

// 快捷方法
showSuccess("保存成功");
showError("操作失败");

// 进度通知
showProgressNotification("上传中", 7, 10, "info"); // "上传中 (7/10 - 70%)"
```

### 图表工具

```typescript
import { getChartColor, createTimeSeriesChartConfig, createPieChartConfig } from "@/utils";
import Chart from "chart.js/auto";

// 获取图表颜色
const color = getChartColor(0); // "rgba(99, 102, 241, 1)"
const transparentColor = getChartColor(0, 0.5); // "rgba(99, 102, 241, 0.5)"

// 创建时间序列图表
const config = createTimeSeriesChartConfig(
    ["1月", "2月", "3月"],
    [{ label: "销售额", data: [100, 200, 300] }]
);
const chart = new Chart(ctx, config);

// 创建饼图
const pieConfig = createPieChartConfig(["A", "B", "C"], [10, 20, 30]);
const pieChart = new Chart(ctx, pieConfig);
```

## 开发规范

### TypeScript

- 所有新代码必须使用TypeScript
- 禁止使用`any`类型,使用`unknown`替代
- 导出的函数/类必须添加JSDoc注释

### 样式

- 使用设计令牌系统
- 避免内联样式
- CSS类名使用kebab-case

### 测试

- 每个模块都应有对应的测试文件
- 测试覆盖率目标: 70%+
- 运行测试: `npm test`

## 构建命令

```bash
# 开发模式 (HMR)
npm run dev

# 生产构建
npm run build

# 代码检查
npm run lint
npm run lint:style

# 代码格式化
npm run format

# 测试
npm test
npm run test:run  # 带覆盖率
```
