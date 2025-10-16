/**
 * format工具模块测试
 */

import { describe, it, expect } from "vitest";
import {
    formatBytes,
    formatUptime,
    formatPercentage,
    formatTimestamp,
    getUsageClass,
    formatNumber
} from "../utils/format";

describe("format工具模块", () => {
    describe("formatBytes", () => {
        it("应格式化0字节", () => {
            expect(formatBytes(0)).toBe("0 Bytes");
        });

        it("应格式化字节单位", () => {
            expect(formatBytes(500)).toBe("500 Bytes");
            expect(formatBytes(1023)).toBe("1023 Bytes");
        });

        it("应格式化KB单位", () => {
            expect(formatBytes(1024)).toBe("1 KB");
            expect(formatBytes(1536)).toBe("1.5 KB");
        });

        it("应格式化MB单位", () => {
            expect(formatBytes(1048576)).toBe("1 MB");
            expect(formatBytes(1572864)).toBe("1.5 MB");
        });

        it("应格式化GB单位", () => {
            expect(formatBytes(1073741824)).toBe("1 GB");
            expect(formatBytes(2147483648)).toBe("2 GB");
        });

        it("应格式化TB单位", () => {
            expect(formatBytes(1099511627776)).toBe("1 TB");
        });

        it("应支持自定义小数位数", () => {
            expect(formatBytes(1536, 0)).toBe("2 KB");
            expect(formatBytes(1536, 1)).toBe("1.5 KB");
            expect(formatBytes(1638.4, 2)).toBe("1.6 KB");
            expect(formatBytes(1638.4, 3)).toBe("1.6 KB");
        });

        it("应处理负数小数位数", () => {
            expect(formatBytes(1536, -1)).toBe("2 KB");
        });

        it("应处理大数值", () => {
            expect(formatBytes(1125899906842624)).toBe("1 PB");
        });
    });

    describe("formatUptime", () => {
        it("应格式化秒为分钟", () => {
            expect(formatUptime(30)).toBe("0分钟");
            expect(formatUptime(60)).toBe("1分钟");
            expect(formatUptime(300)).toBe("5分钟");
        });

        it("应格式化秒为小时和分钟", () => {
            expect(formatUptime(3600)).toBe("1小时 0分钟");
            expect(formatUptime(3660)).toBe("1小时 1分钟");
            expect(formatUptime(7200)).toBe("2小时 0分钟");
        });

        it("应格式化秒为天和小时", () => {
            expect(formatUptime(86400)).toBe("1天 0小时");
            expect(formatUptime(90000)).toBe("1天 1小时");
            expect(formatUptime(172800)).toBe("2天 0小时");
        });

        it("应处理大于1天的时间", () => {
            expect(formatUptime(259200)).toBe("3天 0小时");
            expect(formatUptime(266400)).toBe("3天 2小时");
        });

        it("应处理0秒", () => {
            expect(formatUptime(0)).toBe("0分钟");
        });

        it("应忽略秒数部分", () => {
            expect(formatUptime(3665)).toBe("1小时 1分钟");
            expect(formatUptime(86459)).toBe("1天 0小时");
        });
    });

    describe("formatPercentage", () => {
        it("应格式化百分比", () => {
            expect(formatPercentage(50)).toBe("50.0%");
            expect(formatPercentage(75.5)).toBe("75.5%");
            expect(formatPercentage(100)).toBe("100.0%");
        });

        it("应支持自定义小数位数", () => {
            expect(formatPercentage(85.678, 0)).toBe("86%");
            expect(formatPercentage(85.678, 1)).toBe("85.7%");
            expect(formatPercentage(85.678, 2)).toBe("85.68%");
        });

        it("应处理0%", () => {
            expect(formatPercentage(0)).toBe("0.0%");
        });

        it("应处理大于100%的值", () => {
            expect(formatPercentage(150.5)).toBe("150.5%");
        });

        it("应处理负数百分比", () => {
            expect(formatPercentage(-10.5)).toBe("-10.5%");
        });

        it("应处理小数", () => {
            expect(formatPercentage(0.123, 2)).toBe("0.12%");
        });
    });

    describe("formatTimestamp", () => {
        it("应格式化时间戳", () => {
            const timestamp = new Date("2025-10-11 12:34:56").getTime();
            expect(formatTimestamp(timestamp)).toBe("2025-10-11 12:34:56");
        });

        it("应格式化ISO字符串", () => {
            const isoString = "2025-10-11T12:34:56.000Z";
            const result = formatTimestamp(isoString);
            expect(result).toMatch(/\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/);
        });

        it("应支持自定义格式", () => {
            const timestamp = new Date("2025-10-11 12:34:56").getTime();
            expect(formatTimestamp(timestamp, "YYYY-MM-DD")).toBe("2025-10-11");
            expect(formatTimestamp(timestamp, "HH:mm:ss")).toBe("12:34:56");
            expect(formatTimestamp(timestamp, "YYYY/MM/DD HH:mm")).toBe("2025/10/11 12:34");
        });

        it("应处理月份小于10的情况", () => {
            const timestamp = new Date("2025-01-05 08:09:07").getTime();
            const result = formatTimestamp(timestamp);
            expect(result).toBe("2025-01-05 08:09:07");
        });

        it("应处理日期小于10的情况", () => {
            const timestamp = new Date("2025-12-09 01:02:03").getTime();
            const result = formatTimestamp(timestamp);
            expect(result).toBe("2025-12-09 01:02:03");
        });
    });

    describe("getUsageClass", () => {
        it("应返回danger类名（>=90%）", () => {
            expect(getUsageClass(90)).toBe("danger");
            expect(getUsageClass(95)).toBe("danger");
            expect(getUsageClass(100)).toBe("danger");
        });

        it("应返回warning类名（75-89%）", () => {
            expect(getUsageClass(75)).toBe("warning");
            expect(getUsageClass(80)).toBe("warning");
            expect(getUsageClass(89)).toBe("warning");
        });

        it("应返回normal类名（<75%）", () => {
            expect(getUsageClass(0)).toBe("normal");
            expect(getUsageClass(50)).toBe("normal");
            expect(getUsageClass(74)).toBe("normal");
        });

        it("应处理边界值", () => {
            expect(getUsageClass(74.9)).toBe("normal");
            expect(getUsageClass(75.0)).toBe("warning");
            expect(getUsageClass(89.9)).toBe("warning");
            expect(getUsageClass(90.0)).toBe("danger");
        });

        it("应处理超过100的值", () => {
            expect(getUsageClass(150)).toBe("danger");
        });

        it("应处理负数", () => {
            expect(getUsageClass(-10)).toBe("normal");
        });
    });

    describe("formatNumber", () => {
        it("应格式化整数", () => {
            expect(formatNumber(1000)).toBe("1,000");
            expect(formatNumber(1000000)).toBe("1,000,000");
        });

        it("应支持自定义小数位数", () => {
            expect(formatNumber(1234.5, 0)).toBe("1,235");
            expect(formatNumber(1234.5, 1)).toBe("1,234.5");
            expect(formatNumber(1234.5678, 2)).toBe("1,234.57");
        });

        it("应处理小于1000的数字", () => {
            expect(formatNumber(999)).toBe("999");
            expect(formatNumber(500.5, 1)).toBe("500.5");
        });

        it("应处理0", () => {
            expect(formatNumber(0)).toBe("0");
        });

        it("应处理负数", () => {
            expect(formatNumber(-1234567, 0)).toBe("-1,234,567");
            expect(formatNumber(-1234.56, 2)).toBe("-1,234.56");
        });

        it("应处理大数值", () => {
            expect(formatNumber(1234567890)).toBe("1,234,567,890");
            expect(formatNumber(1234567890.123, 3)).toBe("1,234,567,890.123");
        });

        it("应正确四舍五入", () => {
            expect(formatNumber(1234.567, 2)).toBe("1,234.57");
            expect(formatNumber(1234.564, 2)).toBe("1,234.56");
        });
    });
});
