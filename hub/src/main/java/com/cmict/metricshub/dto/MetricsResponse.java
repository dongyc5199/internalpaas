package com.cmict.metricshub.dto;

import com.cmict.metricshub.model.ServerMetricSample;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Metrics Query Response DTO
 *
 * 用于封装查询API的响应数据,包含数据源信息和查询结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsResponse {

    /**
     * 数据源类型
     * - redis: Redis热数据 (最近5分钟)
     * - tsdb-raw: TSDB原始数据 (15秒粒度)
     * - tsdb-5m: TSDB 5分钟聚合数据
     * - tsdb-1h: TSDB 1小时聚合数据
     */
    private String source;

    /**
     * 查询时间范围 (毫秒)
     */
    private Long fromTimestamp;
    private Long toTimestamp;

    /**
     * 查询的服务器ID
     */
    private String serverId;

    /**
     * 查询的指标名称列表
     */
    private List<String> metricNames;

    /**
     * 查询结果 - 时间序列数据
     * Key: 指标名称 (如 "system.cpu.usage")
     * Value: 时间序列数据点列表
     */
    private Map<String, List<MetricDataPoint>> metrics;

    /**
     * 返回的数据点总数
     */
    private Integer dataPointCount;

    /**
     * 查询耗时 (毫秒)
     */
    private Long queryDurationMs;

    /**
     * 单个数据点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricDataPoint {
        /**
         * 时间戳 (epoch毫秒)
         */
        private Long timestamp;

        /**
         * 指标值
         */
        private Double value;

        /**
         * 单位
         */
        private String unit;

        /**
         * 标签 (可选)
         */
        private Map<String, String> labels;

        /**
         * 聚合信息 (仅聚合数据有值)
         */
        private AggregationInfo aggregation;
    }

    /**
     * 聚合信息 (用于聚合数据点)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregationInfo {
        /**
         * 样本数量
         */
        private Long count;

        /**
         * 最小值
         */
        private Double min;

        /**
         * 最大值
         */
        private Double max;

        /**
         * 平均值
         */
        private Double avg;

        /**
         * 总和
         */
        private Double sum;
    }

    /**
     * 批量查询响应 (多服务器)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchResponse {
        /**
         * 数据源类型
         */
        private String source;

        /**
         * 查询时间范围
         */
        private Long fromTimestamp;
        private Long toTimestamp;

        /**
         * 服务器指标快照
         * Key: serverId
         * Value: 该服务器的最新指标快照
         */
        private Map<String, MetricsSnapshot> serverMetrics;

        /**
         * 查询耗时
         */
        private Long queryDurationMs;
    }

    /**
     * 指标快照 (用于批量查询最新值)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsSnapshot {
        /**
         * 服务器ID
         */
        private String serverId;

        /**
         * 快照时间
         */
        private Instant timestamp;

        /**
         * 指标值
         * Key: 指标名称
         * Value: 指标值
         */
        private Map<String, Double> metrics;

        /**
         * 指标单位
         * Key: 指标名称
         * Value: 单位
         */
        private Map<String, String> units;

        /**
         * 数据新鲜度 (秒)
         * 表示数据距离当前时间的秒数
         */
        private Long freshnessSeconds;
    }

    /**
     * 创建Redis数据源响应
     */
    public static MetricsResponse fromRedis(String serverId, Map<String, Object> redisData,
                                           Long from, Long to, Long queryTime) {
        // 将Redis Hash数据转换为时间序列格式
        Map<String, List<MetricDataPoint>> metricsMap = Map.of();
        // TODO: 实现Redis数据转换逻辑

        return MetricsResponse.builder()
                .source("redis")
                .serverId(serverId)
                .fromTimestamp(from)
                .toTimestamp(to)
                .metrics(metricsMap)
                .dataPointCount(redisData.size())
                .queryDurationMs(queryTime)
                .build();
    }

    /**
     * 创建TSDB数据源响应
     */
    public static MetricsResponse fromTsdb(String serverId, List<ServerMetricSample> samples,
                                          Long from, Long to, String source, Long queryTime) {
        Map<String, List<MetricDataPoint>> metricsMap = new java.util.HashMap<>();

        // 按指标名称分组
        samples.stream()
                .collect(java.util.stream.Collectors.groupingBy(ServerMetricSample::getMetricName))
                .forEach((metricName, metricSamples) -> {
                    List<MetricDataPoint> dataPoints = metricSamples.stream()
                            .map(sample -> MetricDataPoint.builder()
                                    .timestamp(sample.getTimestamp().toEpochMilli())
                                    .value(sample.getValue())
                                    .unit(sample.getUnit())
                                    .build())
                            .sorted((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()))
                            .collect(java.util.stream.Collectors.toList());

                    metricsMap.put(metricName, dataPoints);
                });

        return MetricsResponse.builder()
                .source(source)
                .serverId(serverId)
                .fromTimestamp(from)
                .toTimestamp(to)
                .metrics(metricsMap)
                .metricNames(List.copyOf(metricsMap.keySet()))
                .dataPointCount(samples.size())
                .queryDurationMs(queryTime)
                .build();
    }
}
