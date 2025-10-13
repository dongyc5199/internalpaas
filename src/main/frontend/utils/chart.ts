/**
 * 图表工具函数模块
 */

/**
 * 预定义的图表颜色集合
 */
export const CHART_COLORS = {
    indigo: [99, 102, 241],
    green: [16, 185, 129],
    amber: [245, 158, 11],
    red: [239, 68, 68],
    blue: [59, 130, 246],
    purple: [139, 92, 246]
} as const;

/**
 * 默认颜色数组（用于循环）
 */
export const DEFAULT_COLORS = [
    CHART_COLORS.indigo,
    CHART_COLORS.green,
    CHART_COLORS.amber,
    CHART_COLORS.red,
    CHART_COLORS.blue,
    CHART_COLORS.purple
];

/**
 * 根据索引获取图表颜色
 * @param index - 颜色索引
 * @param alpha - 透明度 (0-1)
 * @returns RGBA颜色字符串
 */
export function getChartColor(index: number, alpha = 1): string {
    const color = DEFAULT_COLORS[index % DEFAULT_COLORS.length];
    return `rgba(${color[0]}, ${color[1]}, ${color[2]}, ${alpha})`;
}

/**
 * 根据名称获取图表颜色
 * @param name - 颜色名称
 * @param alpha - 透明度 (0-1)
 * @returns RGBA颜色字符串
 */
export function getChartColorByName(name: keyof typeof CHART_COLORS, alpha = 1): string {
    const color = CHART_COLORS[name];
    return `rgba(${color[0]}, ${color[1]}, ${color[2]}, ${alpha})`;
}

/**
 * 生成颜色数组
 * @param count - 颜色数量
 * @param alpha - 透明度
 * @returns RGBA颜色字符串数组
 */
export function generateChartColors(count: number, alpha = 1): string[] {
    return Array.from({ length: count }, (_, i) => getChartColor(i, alpha));
}

/**
 * 生成渐变色数组
 * @param startColor - 起始颜色 RGB数组
 * @param endColor - 结束颜色 RGB数组
 * @param count - 颜色数量
 * @param alpha - 透明度
 * @returns RGBA颜色字符串数组
 */
export function generateGradientColors(
    startColor: number[],
    endColor: number[],
    count: number,
    alpha = 1
): string[] {
    if (count <= 1) {
        return [`rgba(${startColor[0]}, ${startColor[1]}, ${startColor[2]}, ${alpha})`];
    }

    const colors: string[] = [];
    for (let i = 0; i < count; i++) {
        const ratio = i / (count - 1);
        const r = Math.round(startColor[0] + (endColor[0] - startColor[0]) * ratio);
        const g = Math.round(startColor[1] + (endColor[1] - startColor[1]) * ratio);
        const b = Math.round(startColor[2] + (endColor[2] - startColor[2]) * ratio);
        colors.push(`rgba(${r}, ${g}, ${b}, ${alpha})`);
    }

    return colors;
}

/**
 * 默认图表配置选项
 */
export const DEFAULT_CHART_OPTIONS = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
        legend: {
            display: true,
            position: "top" as const
        },
        tooltip: {
            enabled: true
        }
    }
};

/**
 * 创建时间序列图表配置
 * @param labels - 时间标签
 * @param datasets - 数据集
 * @param options - 额外配置选项
 */
export function createTimeSeriesChartConfig(
    labels: string[],
    datasets: Array<{
        label: string;
        data: number[];
        borderColor?: string;
        backgroundColor?: string;
        fill?: boolean;
    }>,
    options: Record<string, any> = {}
) {
    return {
        type: "line" as const,
        data: {
            labels,
            datasets: datasets.map((dataset, index) => ({
                label: dataset.label,
                data: dataset.data,
                borderColor: dataset.borderColor || getChartColor(index),
                backgroundColor: dataset.backgroundColor || getChartColor(index, 0.1),
                fill: dataset.fill !== undefined ? dataset.fill : false,
                tension: 0.4
            }))
        },
        options: {
            ...DEFAULT_CHART_OPTIONS,
            ...options,
            scales: {
                x: {
                    type: "category" as const,
                    display: true
                },
                y: {
                    display: true,
                    beginAtZero: true
                }
            }
        }
    };
}

/**
 * 创建饼图配置
 * @param labels - 标签
 * @param data - 数据
 * @param options - 额外配置选项
 */
export function createPieChartConfig(
    labels: string[],
    data: number[],
    options: Record<string, any> = {}
) {
    return {
        type: "pie" as const,
        data: {
            labels,
            datasets: [
                {
                    data,
                    backgroundColor: generateChartColors(data.length, 0.8),
                    borderColor: generateChartColors(data.length, 1),
                    borderWidth: 1
                }
            ]
        },
        options: {
            ...DEFAULT_CHART_OPTIONS,
            ...options
        }
    };
}

/**
 * 创建柱状图配置
 * @param labels - 标签
 * @param datasets - 数据集
 * @param options - 额外配置选项
 */
export function createBarChartConfig(
    labels: string[],
    datasets: Array<{
        label: string;
        data: number[];
        backgroundColor?: string;
        borderColor?: string;
    }>,
    options: Record<string, any> = {}
) {
    return {
        type: "bar" as const,
        data: {
            labels,
            datasets: datasets.map((dataset, index) => ({
                label: dataset.label,
                data: dataset.data,
                backgroundColor: dataset.backgroundColor || getChartColor(index, 0.8),
                borderColor: dataset.borderColor || getChartColor(index),
                borderWidth: 1
            }))
        },
        options: {
            ...DEFAULT_CHART_OPTIONS,
            ...options,
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    };
}

/**
 * 销毁图表实例
 * @param chart - Chart.js实例
 */
export function destroyChart(chart: any): void {
    if (chart && typeof chart.destroy === "function") {
        chart.destroy();
    }
}

/**
 * 批量销毁图表实例
 * @param charts - Chart.js实例数组
 */
export function destroyCharts(charts: any[]): void {
    charts.forEach((chart) => destroyChart(chart));
    charts.length = 0; // 清空数组
}

/**
 * 更新图表数据
 * @param chart - Chart.js实例
 * @param newData - 新数据
 * @param newLabels - 新标签（可选）
 */
export function updateChartData(
    chart: any,
    newData: number[] | number[][],
    newLabels?: string[]
): void {
    if (!chart || !chart.data) {
        return;
    }

    // 更新标签
    if (newLabels) {
        chart.data.labels = newLabels;
    }

    // 更新数据
    if (Array.isArray(newData[0])) {
        // 多数据集
        (newData as number[][]).forEach((data, index) => {
            if (chart.data.datasets[index]) {
                chart.data.datasets[index].data = data;
            }
        });
    } else {
        // 单数据集
        if (chart.data.datasets[0]) {
            chart.data.datasets[0].data = newData as number[];
        }
    }

    chart.update();
}
