# P0 任务快速启动指南

## 🚀 立即开始

### ✅ 任务已启动!

**启动时间**: 2025年10月17日  
**当前任务**: P0-任务1 Day 1 - 迁移图表组件  
**状态**: 🟢 进行中

---

## 📋 第一步: 环境准备

### 1. 确认开发环境
```bash
# 检查 Node.js 版本
node --version  # 应该是 20.x

# 检查 npm 版本
npm --version

# 检查依赖安装
npm list chart.js vitest
```

### 2. 启动开发服务器
```bash
# 终端1: 启动前端开发服务器 (Vite HMR)
npm run dev

# 终端2: 启动后端服务器
./mvnw.cmd spring-boot:run
```

### 3. 运行测试
```bash
# 运行现有测试,确保环境正常
npm test
```

---

## 📂 第二步: 分析现有代码

### 现有仪表盘代码位置

1. **JavaScript 逻辑** (待迁移):
   - 📁 `src/main/resources/static/js/dashboard.js` (156行)
   - 包含: Dashboard 类、通知系统、进度条动画、工具函数

2. **HTML 模板**:
   - 📁 搜索结果显示在多个模板中使用
   - 主要文件待确认

3. **已有 TypeScript 模块**:
   - 📁 `src/main/frontend/modules/dashboard.ts` (基础框架)
   - 需要扩展和完善

---

## 🎯 第三步: 开始迁移

### Day 1 任务清单

#### ✅ 任务 1.1: 分析现有代码 (已完成)
- ✅ 找到 `dashboard.js` (156行)
- ✅ 识别核心功能:
  - Dashboard 类
  - 自动刷新 (30秒)
  - 统计数据更新
  - 数字动画效果
  - 通知系统
  - 进度条动画
  - 工具函数 (formatBytes, formatUptime)

#### 🔄 任务 1.2: 创建 TypeScript 架构 (进行中)

**创建文件**: `src/main/frontend/modules/dashboard.ts`

```typescript
/**
 * Dashboard 模块
 * 负责仪表盘的图表渲染和数据更新
 */

import { http } from '@/utils';
import { createTimeSeriesChartConfig, getChartColor } from '@/utils/chart';
import { safeShowToast } from '@/utils/notification';
import Chart from 'chart.js/auto';

interface DashboardStats {
    serverCount: number;
    activeServers: number;
    cpuUsage: number;
    memoryUsage: number;
    diskUsage: number;
}

interface ChartInstance {
    chart: Chart | null;
    canvas: HTMLCanvasElement | null;
}

export class DashboardManager {
    private refreshInterval: number = 30000; // 30秒
    private intervalId: number | null = null;
    private charts: Map<string, ChartInstance> = new Map();

    constructor() {
        this.init();
    }

    /**
     * 初始化仪表盘
     */
    private init(): void {
        this.initializeCharts();
        this.setupAutoRefresh();
        this.setupProgressBars();
    }

    /**
     * 初始化所有图表
     */
    private initializeCharts(): void {
        // 服务器状态图表
        this.createServerStatusChart();
        
        // 资源使用率图表
        this.createResourceUsageChart();
        
        // 实时监控图表
        this.createRealtimeMonitorChart();
    }

    /**
     * 创建服务器状态图表
     */
    private createServerStatusChart(): void {
        const canvas = document.getElementById('server-status-chart') as HTMLCanvasElement;
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        const config = {
            type: 'doughnut' as const,
            data: {
                labels: ['运行中', '已停止', '错误'],
                datasets: [{
                    data: [0, 0, 0],
                    backgroundColor: [
                        getChartColor(0),
                        getChartColor(1),
                        getChartColor(2)
                    ]
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        position: 'bottom' as const
                    },
                    title: {
                        display: true,
                        text: '服务器状态分布'
                    }
                }
            }
        };

        const chart = new Chart(ctx, config);
        this.charts.set('server-status', { chart, canvas });
    }

    /**
     * 创建资源使用率图表
     */
    private createResourceUsageChart(): void {
        const canvas = document.getElementById('resource-usage-chart') as HTMLCanvasElement;
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        const config = {
            type: 'bar' as const,
            data: {
                labels: ['CPU', '内存', '磁盘'],
                datasets: [{
                    label: '使用率 (%)',
                    data: [0, 0, 0],
                    backgroundColor: [
                        getChartColor(0, 0.5),
                        getChartColor(1, 0.5),
                        getChartColor(2, 0.5)
                    ],
                    borderColor: [
                        getChartColor(0),
                        getChartColor(1),
                        getChartColor(2)
                    ],
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100
                    }
                },
                plugins: {
                    title: {
                        display: true,
                        text: '资源使用率'
                    }
                }
            }
        };

        const chart = new Chart(ctx, config);
        this.charts.set('resource-usage', { chart, canvas });
    }

    /**
     * 创建实时监控图表
     */
    private createRealtimeMonitorChart(): void {
        const canvas = document.getElementById('realtime-monitor-chart') as HTMLCanvasElement;
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        const now = new Date();
        const labels = Array.from({ length: 10 }, (_, i) => {
            const time = new Date(now.getTime() - (9 - i) * 60000);
            return `${time.getHours()}:${time.getMinutes().toString().padStart(2, '0')}`;
        });

        const config = createTimeSeriesChartConfig(
            labels,
            [
                {
                    label: 'CPU (%)',
                    data: new Array(10).fill(0)
                },
                {
                    label: '内存 (%)',
                    data: new Array(10).fill(0)
                }
            ]
        );

        const chart = new Chart(ctx, config);
        this.charts.set('realtime-monitor', { chart, canvas });
    }

    /**
     * 设置自动刷新
     */
    private setupAutoRefresh(): void {
        this.updateStats();
        this.intervalId = window.setInterval(() => {
            this.updateStats();
        }, this.refreshInterval);
    }

    /**
     * 更新统计数据
     */
    private async updateStats(): Promise<void> {
        try {
            const { data } = await http.get<DashboardStats>('/api/stats');
            this.updateCharts(data);
            this.updateStatCards(data);
        } catch (error) {
            console.error('更新统计数据失败:', error);
            safeShowToast('更新数据失败', 'error');
        }
    }

    /**
     * 更新图表数据
     */
    private updateCharts(data: DashboardStats): void {
        // 更新服务器状态图表
        const serverStatusChart = this.charts.get('server-status')?.chart;
        if (serverStatusChart) {
            serverStatusChart.data.datasets[0].data = [
                data.activeServers,
                data.serverCount - data.activeServers,
                0 // 错误数量
            ];
            serverStatusChart.update();
        }

        // 更新资源使用率图表
        const resourceUsageChart = this.charts.get('resource-usage')?.chart;
        if (resourceUsageChart) {
            resourceUsageChart.data.datasets[0].data = [
                data.cpuUsage,
                data.memoryUsage,
                data.diskUsage
            ];
            resourceUsageChart.update();
        }

        // 更新实时监控图表 (滚动数据)
        this.updateRealtimeChart(data);
    }

    /**
     * 更新实时监控图表 (滚动数据)
     */
    private updateRealtimeChart(data: DashboardStats): void {
        const realtimeChart = this.charts.get('realtime-monitor')?.chart;
        if (!realtimeChart) return;

        // 移除第一个数据点
        realtimeChart.data.labels?.shift();
        realtimeChart.data.datasets.forEach(dataset => {
            dataset.data.shift();
        });

        // 添加新数据点
        const now = new Date();
        const timeLabel = `${now.getHours()}:${now.getMinutes().toString().padStart(2, '0')}`;
        realtimeChart.data.labels?.push(timeLabel);
        realtimeChart.data.datasets[0].data.push(data.cpuUsage);
        realtimeChart.data.datasets[1].data.push(data.memoryUsage);

        realtimeChart.update('none'); // 不使用动画,平滑滚动
    }

    /**
     * 更新统计卡片
     */
    private updateStatCards(data: DashboardStats): void {
        this.updateStatCard('server-count', data.serverCount);
        this.updateStatCard('active-servers', data.activeServers);
        this.updateStatCard('cpu-usage', data.cpuUsage, '%');
        this.updateStatCard('memory-usage', data.memoryUsage, '%');
    }

    /**
     * 更新单个统计卡片
     */
    private updateStatCard(id: string, value: number, suffix: string = ''): void {
        const element = document.getElementById(id);
        if (!element) return;

        const currentValue = parseInt(element.textContent || '0');
        this.animateNumber(element, currentValue, value, suffix);
    }

    /**
     * 数字动画
     */
    private animateNumber(
        element: HTMLElement,
        start: number,
        end: number,
        suffix: string = ''
    ): void {
        const duration = 1000;
        const startTime = performance.now();

        const animate = (currentTime: number) => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);

            const current = Math.floor(start + (end - start) * progress);
            element.textContent = current + suffix;

            if (progress < 1) {
                requestAnimationFrame(animate);
            }
        };

        requestAnimationFrame(animate);
    }

    /**
     * 设置进度条动画
     */
    private setupProgressBars(): void {
        const progressBars = document.querySelectorAll<HTMLElement>('.progress-bar-fill');
        progressBars.forEach(bar => {
            const width = bar.style.width || '0%';
            bar.style.width = '0%';
            setTimeout(() => {
                bar.style.width = width;
            }, 100);
        });
    }

    /**
     * 销毁仪表盘
     */
    destroy(): void {
        // 清除定时器
        if (this.intervalId !== null) {
            window.clearInterval(this.intervalId);
            this.intervalId = null;
        }

        // 销毁所有图表
        this.charts.forEach(({ chart }) => {
            chart?.destroy();
        });
        this.charts.clear();
    }
}

// 导出单例实例
let dashboardInstance: DashboardManager | null = null;

export function initDashboard(): DashboardManager {
    if (!dashboardInstance) {
        dashboardInstance = new DashboardManager();
    }
    return dashboardInstance;
}

export function destroyDashboard(): void {
    if (dashboardInstance) {
        dashboardInstance.destroy();
        dashboardInstance = null;
    }
}
```

#### ⚪ 任务 1.3: 在 main.ts 中注册 (待完成)

编辑 `src/main/frontend/main.ts`:
```typescript
import { initDashboard } from './modules/dashboard';

// 当页面是仪表盘时初始化
if (document.getElementById('dashboard-container')) {
    initDashboard();
}
```

#### ⚪ 任务 1.4: 更新 HTML 模板 (待完成)

在相关的 HTML 模板中:
1. 添加 canvas 元素用于渲染图表
2. 添加统计卡片元素
3. 引用新的构建产物

```html
<!-- 引入 Vite 构建的 JS -->
<script type="module" src="/dist/assets/main.js"></script>
<script nomodule src="/dist/assets/main-legacy.js"></script>

<!-- 图表容器 -->
<canvas id="server-status-chart"></canvas>
<canvas id="resource-usage-chart"></canvas>
<canvas id="realtime-monitor-chart"></canvas>

<!-- 统计卡片 -->
<div id="server-count">0</div>
<div id="active-servers">0</div>
<div id="cpu-usage">0%</div>
<div id="memory-usage">0%</div>
```

---

## 🧪 第四步: 测试

### 1. 类型检查
```bash
npm run type-check
```

### 2. 构建测试
```bash
npm run build
```

### 3. 功能测试
```bash
# 启动服务器
./mvnw.cmd spring-boot:run

# 访问仪表盘页面
# 检查图表是否正常渲染
# 检查数据是否自动更新
```

---

## ✅ 今日目标

- [x] 分析现有 dashboard.js 代码
- [ ] 创建完整的 DashboardManager 类
- [ ] 迁移三个图表组件
- [ ] 更新 HTML 模板
- [ ] 功能测试通过

---

## 📞 遇到问题?

- **TypeScript 错误**: 运行 `npm run type-check`
- **构建失败**: 检查 `vite.config.ts` 配置
- **图表不显示**: 检查 canvas 元素是否存在
- **数据不更新**: 检查后端 API `/api/stats` 是否正常

---

## 📚 参考资料

- [Chart.js 文档](https://www.chartjs.org/docs/latest/)
- [utils/chart.ts 封装](../../src/main/frontend/utils/chart.ts)
- [现有 dashboard.js](../../src/main/resources/static/js/dashboard.js)

---

**加油! 今天就开始第一个图表的迁移吧!** 💪🚀
