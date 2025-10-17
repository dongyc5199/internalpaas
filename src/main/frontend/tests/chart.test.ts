/**
 * chart工具模块测试
 */

import { describe, it, expect, beforeEach, vi } from "vitest";
import {
    CHART_COLORS,
    DEFAULT_COLORS,
    getChartColor,
    getChartColorByName,
    generateChartColors,
    generateGradientColors,
    createTimeSeriesChartConfig,
    createPieChartConfig,
    createBarChartConfig,
    destroyChart,
    destroyCharts,
    updateChartData
} from "../utils/chart";

describe("chart工具模块", () => {
    describe("颜色常量", () => {
        it("应包含预定义颜色", () => {
            expect(CHART_COLORS.indigo).toEqual([99, 102, 241]);
            expect(CHART_COLORS.green).toEqual([16, 185, 129]);
            expect(CHART_COLORS.amber).toEqual([245, 158, 11]);
            expect(CHART_COLORS.red).toEqual([239, 68, 68]);
            expect(CHART_COLORS.blue).toEqual([59, 130, 246]);
            expect(CHART_COLORS.purple).toEqual([139, 92, 246]);
        });

        it("DEFAULT_COLORS应包含6种颜色", () => {
            expect(DEFAULT_COLORS).toHaveLength(6);
        });
    });

    describe("getChartColor", () => {
        it("应返回RGBA格式的颜色字符串", () => {
            const color = getChartColor(0);
            expect(color).toBe("rgba(99, 102, 241, 1)");
        });

        it("应支持自定义透明度", () => {
            const color = getChartColor(0, 0.5);
            expect(color).toBe("rgba(99, 102, 241, 0.5)");
        });

        it("应循环使用颜色数组", () => {
            const color6 = getChartColor(6);
            const color0 = getChartColor(0);
            expect(color6).toBe(color0);
        });

        it("应处理大索引", () => {
            const color = getChartColor(100);
            expect(color).toMatch(/^rgba\(\d+, \d+, \d+, 1\)$/);
        });
    });

    describe("getChartColorByName", () => {
        it("应根据名称返回颜色", () => {
            const color = getChartColorByName("indigo");
            expect(color).toBe("rgba(99, 102, 241, 1)");
        });

        it("应支持自定义透明度", () => {
            const color = getChartColorByName("green", 0.8);
            expect(color).toBe("rgba(16, 185, 129, 0.8)");
        });
    });

    describe("generateChartColors", () => {
        it("应生成指定数量的颜色", () => {
            const colors = generateChartColors(3);
            expect(colors).toHaveLength(3);
            expect(colors[0]).toBe("rgba(99, 102, 241, 1)");
            expect(colors[1]).toBe("rgba(16, 185, 129, 1)");
            expect(colors[2]).toBe("rgba(245, 158, 11, 1)");
        });

        it("应支持自定义透明度", () => {
            const colors = generateChartColors(2, 0.5);
            expect(colors[0]).toBe("rgba(99, 102, 241, 0.5)");
            expect(colors[1]).toBe("rgba(16, 185, 129, 0.5)");
        });

        it("应处理0数量", () => {
            const colors = generateChartColors(0);
            expect(colors).toHaveLength(0);
        });
    });

    describe("generateGradientColors", () => {
        it("应生成渐变色", () => {
            const startColor = [0, 0, 0];
            const endColor = [255, 255, 255];
            const colors = generateGradientColors(startColor, endColor, 3);

            expect(colors).toHaveLength(3);
            expect(colors[0]).toBe("rgba(0, 0, 0, 1)");
            expect(colors[1]).toBe("rgba(128, 128, 128, 1)");
            expect(colors[2]).toBe("rgba(255, 255, 255, 1)");
        });

        it("应支持自定义透明度", () => {
            const startColor = [0, 0, 0];
            const endColor = [100, 100, 100];
            const colors = generateGradientColors(startColor, endColor, 2, 0.5);

            expect(colors[0]).toBe("rgba(0, 0, 0, 0.5)");
            expect(colors[1]).toBe("rgba(100, 100, 100, 0.5)");
        });

        it("应处理count为1的情况", () => {
            const colors = generateGradientColors([0, 0, 0], [255, 255, 255], 1);
            expect(colors).toHaveLength(1);
            expect(colors[0]).toBe("rgba(0, 0, 0, 1)");
        });
    });

    describe("createTimeSeriesChartConfig", () => {
        it("应创建时间序列图表配置", () => {
            const config = createTimeSeriesChartConfig(
                ["1月", "2月", "3月"],
                [
                    { label: "销售额", data: [100, 200, 300] },
                    { label: "成本", data: [50, 100, 150] }
                ]
            );

            expect(config.type).toBe("line");
            expect(config.data.labels).toEqual(["1月", "2月", "3月"]);
            expect(config.data.datasets).toHaveLength(2);
            expect(config.data.datasets[0].label).toBe("销售额");
            expect(config.data.datasets[0].data).toEqual([100, 200, 300]);
        });

        it("应使用自动生成的颜色", () => {
            const config = createTimeSeriesChartConfig(
                ["A", "B"],
                [{ label: "数据1", data: [1, 2] }]
            );

            expect(config.data.datasets[0].borderColor).toBe("rgba(99, 102, 241, 1)");
        });

        it("应支持自定义选项", () => {
            const config = createTimeSeriesChartConfig(["A"], [{ label: "数据", data: [1] }], {
                plugins: { legend: { display: false } }
            });

            expect(config.options.plugins.legend.display).toBe(false);
        });
    });

    describe("createPieChartConfig", () => {
        it("应创建饼图配置", () => {
            const config = createPieChartConfig(["A", "B", "C"], [10, 20, 30]);

            expect(config.type).toBe("pie");
            expect(config.data.labels).toEqual(["A", "B", "C"]);
            expect(config.data.datasets[0].data).toEqual([10, 20, 30]);
            expect(config.data.datasets[0].backgroundColor).toHaveLength(3);
        });
    });

    describe("createBarChartConfig", () => {
        it("应创建柱状图配置", () => {
            const config = createBarChartConfig(
                ["Q1", "Q2", "Q3"],
                [
                    { label: "2023", data: [100, 200, 300] },
                    { label: "2024", data: [150, 250, 350] }
                ]
            );

            expect(config.type).toBe("bar");
            expect(config.data.labels).toEqual(["Q1", "Q2", "Q3"]);
            expect(config.data.datasets).toHaveLength(2);
        });
    });

    describe("destroyChart", () => {
        it("应调用chart的destroy方法", () => {
            const mockChart = { destroy: vi.fn() };
            destroyChart(mockChart);
            expect(mockChart.destroy).toHaveBeenCalled();
        });

        it("应处理null chart", () => {
            expect(() => destroyChart(null)).not.toThrow();
        });

        it("应处理没有destroy方法的对象", () => {
            expect(() => destroyChart({})).not.toThrow();
        });
    });

    describe("destroyCharts", () => {
        it("应销毁多个chart并清空数组", () => {
            const mockChart1 = { destroy: vi.fn() };
            const mockChart2 = { destroy: vi.fn() };
            const charts = [mockChart1, mockChart2];

            destroyCharts(charts);

            expect(mockChart1.destroy).toHaveBeenCalled();
            expect(mockChart2.destroy).toHaveBeenCalled();
            expect(charts).toHaveLength(0);
        });
    });

    describe("updateChartData", () => {
        it("应更新单数据集", () => {
            const mockChart = {
                data: {
                    labels: ["A", "B"],
                    datasets: [{ data: [1, 2] }]
                },
                update: vi.fn()
            };

            updateChartData(mockChart, [3, 4]);

            expect(mockChart.data.datasets[0].data).toEqual([3, 4]);
            expect(mockChart.update).toHaveBeenCalled();
        });

        it("应更新多数据集", () => {
            const mockChart = {
                data: {
                    labels: ["A", "B"],
                    datasets: [{ data: [1, 2] }, { data: [3, 4] }]
                },
                update: vi.fn()
            };

            updateChartData(mockChart, [
                [5, 6],
                [7, 8]
            ]);

            expect(mockChart.data.datasets[0].data).toEqual([5, 6]);
            expect(mockChart.data.datasets[1].data).toEqual([7, 8]);
            expect(mockChart.update).toHaveBeenCalled();
        });

        it("应更新标签", () => {
            const mockChart = {
                data: {
                    labels: ["A", "B"],
                    datasets: [{ data: [1, 2] }]
                },
                update: vi.fn()
            };

            updateChartData(mockChart, [3, 4], ["C", "D"]);

            expect(mockChart.data.labels).toEqual(["C", "D"]);
        });

        it("应处理null chart", () => {
            expect(() => updateChartData(null, [1, 2])).not.toThrow();
        });
    });
});
