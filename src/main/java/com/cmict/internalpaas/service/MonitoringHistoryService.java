package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
// import com.cmict.internalpaas.repository.ServerMetricsRepository; // ❌ 已废弃 (Phase4-Step4)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cmict.internalpaas.client.MetricsHubClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 历史监控数据服务
 * 提供历史数据查询、聚合、分析和导出功能
 * 
 * ⚠️ 已废弃 (Phase4-Step4: 2025-10-17)
 * 原因: H2数据库已移除,监控数据由Hub存储
 * 
 * 替代方案:
 * - MetricsHubClient.queryMetrics(serverId, from, to, step)
 * - Hub提供历史数据查询、聚合、导出功能
 * 
 * @deprecated 使用 {@link com.cmict.internalpaas.client.MetricsHubClient}
 */
@Deprecated(since = "2025-10-17", forRemoval = true)
@Service
public class MonitoringHistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringHistoryService.class);
    
    // ❌ 已废弃 (Phase4-Step4: 2025-10-17)
    // @Autowired
    // private ServerMetricsRepository metricsRepository;
    @Autowired(required = false)
    private MetricsHubClient metricsHubClient;
    
    @Autowired
    private ServerService serverService;
    
    /**
     * 获取服务器历史数据
     * 
     * ❌ 已废弃 (Phase4-Step4)
     * @deprecated 使用 MetricsHubClient.queryMetrics()
     */
    @Deprecated
    public Map<String, Object> getServerHistoryData(Long serverId, LocalDateTime startTime, 
                                                   LocalDateTime endTime, boolean aggregated, 
                                                   String aggregationType) {
        logger.warn("getServerHistoryData已废弃,请使用MetricsHubClient.queryMetrics()");
        
        try {
            Map<String, Object> response = new HashMap<>();
            // 如果 Metrics Hub 可用，尝试通过 Hub 拉取数据（最小实现：将 Hub 返回的单点封装为时间序列）
            List<ServerMetrics> rawData = Collections.emptyList();
            if (metricsHubClient != null && metricsHubClient.isAvailable()) {
                try {
                    Long fromMillis = startTime != null ? startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
                    Long toMillis = endTime != null ? endTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
                    // step: 如果需要降采样，这里简单设为null（Hub 决定）或可传入秒数
                    ServerMetrics sample = metricsHubClient.queryMetrics(serverId, fromMillis, toMillis, null, null);
                    if (sample != null) {
                        rawData = List.of(sample);
                    }
                } catch (Exception ex) {
                    logger.warn("MetricsHub query failed for history data: {}", ex.getMessage());
                }
            } else {
                logger.debug("MetricsHub not available - returning empty history data");
            }

            if (aggregated) {
                // minimal: 当 Hub 返回单点时，将其视为聚合点；否则返回空聚合
                if (rawData.isEmpty()) {
                    response.put("data", new ArrayList<>());
                } else {
                    response.put("data", convertAggregatedToChartData(getAggregatedData(serverId, startTime, endTime, aggregationType), aggregationType));
                }
                response.put("dataType", "aggregated");
                response.put("aggregationType", aggregationType);
            } else {
                response.put("data", convertRawToChartData(rawData));
                response.put("dataType", "raw");
            }
            
            // 添加服务器信息
            Optional<Server> serverOpt = serverService.findById(serverId);
            if (serverOpt.isPresent()) {
                Server server = serverOpt.get();
                Map<String, Object> serverInfo = new HashMap<>();
                serverInfo.put("id", server.getId());
                serverInfo.put("name", server.getName());
                serverInfo.put("hostname", server.getHostname());
                response.put("serverInfo", serverInfo);
            }
            
            // 添加时间范围信息
            response.put("timeRange", Map.of("start", startTime, "end", endTime));
            response.put("success", true);
            
            return response;
        } catch (Exception e) {
            logger.error("获取服务器历史数据失败", e);
            throw new RuntimeException("获取历史数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 比较多个服务器的数据
     */
    public Map<String, Object> compareServers(List<Long> serverIds, LocalDateTime startTime, 
                                            LocalDateTime endTime, String aggregationType) {
        try {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> serversData = new HashMap<>();
            
            for (Long serverId : serverIds) {
                List<Object[]> aggregatedData = getAggregatedData(serverId, startTime, endTime, aggregationType);
                Map<String, Object> serverData = convertAggregatedToChartData(aggregatedData, aggregationType);
                
                // 获取服务器名称
                Optional<Server> serverOpt = serverService.findById(serverId);
                String serverName = serverOpt.map(Server::getName).orElse("Unknown Server");
                
                serversData.put(serverName, serverData);
            }
            
            response.put("serversData", serversData);
            response.put("timeRange", Map.of("start", startTime, "end", endTime));
            response.put("aggregationType", aggregationType);
            response.put("success", true);
            
            return response;
        } catch (Exception e) {
            logger.error("比较服务器数据失败", e);
            throw new RuntimeException("比较服务器数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取服务器统计信息
     */
    public Map<String, Object> getServerStatistics(Long serverId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<ServerMetrics> metrics = Collections.emptyList();
            if (metricsHubClient != null && metricsHubClient.isAvailable()) {
                logger.debug("MetricsHub available - statistics should be computed from Hub query results");
            }
            
            Map<String, Object> statistics = new HashMap<>();
            
            if (metrics.isEmpty()) {
                statistics.put("dataCount", 0);
                statistics.put("message", "指定时间范围内无数据");
                return statistics;
            }
            
            // 基本统计
            statistics.put("dataCount", metrics.size());
            statistics.put("timeSpan", Duration.between(startTime, endTime).toHours() + " hours");
            
            // CPU统计
            List<Double> cpuValues = metrics.stream()
                .map(ServerMetrics::getCpuUsage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            if (!cpuValues.isEmpty()) {
                statistics.put("cpu", Map.of(
                    "avg", cpuValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                    "max", Collections.max(cpuValues),
                    "min", Collections.min(cpuValues),
                    "latest", cpuValues.get(cpuValues.size() - 1)
                ));
            }
            
            // 内存统计
            List<Double> memoryValues = metrics.stream()
                .map(ServerMetrics::getMemoryUsage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            if (!memoryValues.isEmpty()) {
                statistics.put("memory", Map.of(
                    "avg", memoryValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                    "max", Collections.max(memoryValues),
                    "min", Collections.min(memoryValues),
                    "latest", memoryValues.get(memoryValues.size() - 1)
                ));
            }
            
            // 磁盘统计
            List<Double> diskValues = metrics.stream()
                .map(ServerMetrics::getDiskUsage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            if (!diskValues.isEmpty()) {
                statistics.put("disk", Map.of(
                    "avg", diskValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                    "max", Collections.max(diskValues),
                    "min", Collections.min(diskValues),
                    "latest", diskValues.get(diskValues.size() - 1)
                ));
            }
            
            statistics.put("success", true);
            return statistics;
        } catch (Exception e) {
            logger.error("获取服务器统计信息失败", e);
            throw new RuntimeException("获取统计信息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取异常事件
     */
    public Map<String, Object> getAnomalies(Long serverId, LocalDateTime startTime, LocalDateTime endTime,
                                          Double cpuThreshold, Double memoryThreshold) {
        try {
            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> anomalies = new ArrayList<>();
            
            // CPU异常 - 已由 Hub 提供，当前返回空集合
            List<ServerMetrics> highCpuEvents = Collections.emptyList();
            
            for (ServerMetrics event : highCpuEvents) {
                Map<String, Object> anomaly = new HashMap<>();
                anomaly.put("type", "HIGH_CPU");
                anomaly.put("timestamp", event.getTimestamp());
                anomaly.put("value", event.getCpuUsage());
                anomaly.put("threshold", cpuThreshold);
                anomaly.put("severity", event.getCpuUsage() > 90 ? "CRITICAL" : "WARNING");
                anomaly.put("message", String.format("CPU使用率达到 %.1f%%", event.getCpuUsage()));
                anomalies.add(anomaly);
            }
            
            // 内存异常 - 已由 Hub 提供，当前返回空集合
            List<ServerMetrics> highMemoryEvents = Collections.emptyList();
            
            for (ServerMetrics event : highMemoryEvents) {
                Map<String, Object> anomaly = new HashMap<>();
                anomaly.put("type", "HIGH_MEMORY");
                anomaly.put("timestamp", event.getTimestamp());
                anomaly.put("value", event.getMemoryUsage());
                anomaly.put("threshold", memoryThreshold);
                anomaly.put("severity", event.getMemoryUsage() > 90 ? "CRITICAL" : "WARNING");
                anomaly.put("message", String.format("内存使用率达到 %.1f%%", event.getMemoryUsage()));
                anomalies.add(anomaly);
            }
            
            // 按时间排序
            anomalies.sort((a, b) -> ((LocalDateTime) b.get("timestamp"))
                .compareTo((LocalDateTime) a.get("timestamp")));
            
            response.put("anomalies", anomalies);
            response.put("totalCount", anomalies.size());
            response.put("cpuAnomalies", highCpuEvents.size());
            response.put("memoryAnomalies", highMemoryEvents.size());
            response.put("success", true);
            
            return response;
        } catch (Exception e) {
            logger.error("获取异常事件失败", e);
            throw new RuntimeException("获取异常事件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 导出数据为CSV
     */
    public byte[] exportToCsv(Long serverId, LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        List<ServerMetrics> metrics = Collections.emptyList();
        if (metricsHubClient != null && metricsHubClient.isAvailable()) {
            logger.debug("MetricsHub available - export should be implemented via Hub API");
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            // 写入BOM以支持Excel中文显示
            writer.write('\ufeff');
            
            // CSV头部
            writer.write("时间,服务器名称,主机名,CPU使用率(%),内存使用率(%),磁盘使用率(%),CPU核心数,负载均值,运行时间\n");
            
            // 数据行
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (ServerMetrics metric : metrics) {
                writer.write(String.format("%s,%s,%s,%.2f,%.2f,%.2f,%d,%s,%s\n",
                    metric.getTimestamp().format(formatter),
                    metric.getServerName() != null ? metric.getServerName() : "",
                    metric.getHostname() != null ? metric.getHostname() : "",
                    metric.getCpuUsage() != null ? metric.getCpuUsage() : 0.0,
                    metric.getMemoryUsage() != null ? metric.getMemoryUsage() : 0.0,
                    metric.getDiskUsage() != null ? metric.getDiskUsage() : 0.0,
                    metric.getCpuCores() != null ? metric.getCpuCores() : 0,
                    metric.getLoadAverage() != null ? metric.getLoadAverage() : "",
                    metric.getUptime() != null ? metric.getUptime() : ""
                ));
            }
        }
        
        return outputStream.toByteArray();
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 获取聚合数据
     */
    private List<Object[]> getAggregatedData(Long serverId, LocalDateTime startTime, 
                                           LocalDateTime endTime, String aggregationType) {
        switch (aggregationType.toLowerCase()) {
            case "hour":
            case "day":
                try {
                    // 聚合查询应通过 Hub 获取，此处返回占位空数组
                    logger.debug("Aggregated data should be fetched from Hub - returning placeholder");
                    Object[] aggregatedData = new Object[0];
                    
                    List<Object[]> result = new ArrayList<>();
                    if (aggregatedData != null && aggregatedData.length > 0) {
                        // aggregatedData实际上是包含一个Object[]的数组，需要取出内部数组
                        Object[] actualData = (Object[]) aggregatedData[0];
                        logger.info("聚合查询结果 - serverId: {}, 外层数组长度: {}, 内层数组长度: {}", 
                            serverId, aggregatedData.length, actualData.length);
                        
                        if (actualData.length == 10) {
                            // 数据正常，创建13元素数组
                            Object[] fullRow = new Object[13];
                            fullRow[0] = serverId;
                            fullRow[1] = getServerName(serverId);
                            fullRow[2] = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            // actualData: [avgCpu, maxCpu, minCpu, avgMem, maxMem, minMem, avgDisk, maxDisk, minDisk, count]
                            System.arraycopy(actualData, 0, fullRow, 3, 10);
                            result.add(fullRow);
                        } else {
                            logger.warn("内层聚合数据长度异常 - serverId: {}, 期望10个元素，实际{}个元素", 
                                serverId, actualData.length);
                            // 创建空数据行避免数组越界
                            Object[] emptyRow = new Object[13];
                            emptyRow[0] = serverId;
                            emptyRow[1] = getServerName(serverId);
                            emptyRow[2] = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            for (int i = 3; i < 13; i++) {
                                emptyRow[i] = 0.0;
                            }
                            result.add(emptyRow);
                        }
                    } else {
                        logger.warn("聚合查询返回空结果 - serverId: {}", serverId);
                        // 创建空数据行
                        Object[] emptyRow = new Object[13];
                        emptyRow[0] = serverId;
                        emptyRow[1] = getServerName(serverId);
                        emptyRow[2] = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        for (int i = 3; i < 13; i++) {
                            emptyRow[i] = 0.0;
                        }
                        result.add(emptyRow);
                    }
                    return result;
                } catch (Exception e) {
                    logger.error("聚合数据处理失败 - serverId: {}", serverId, e);
                    return new ArrayList<>();
                }
            default:
                // 返回原始数据 - 当前由 Hub 提供，保守返回空结果
                List<ServerMetrics> rawData = Collections.emptyList();
                return rawData.stream()
                    .map(m -> new Object[]{
                        m.getServerId(), m.getServerName(), 
                        m.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        m.getCpuUsage(), m.getCpuUsage(), m.getCpuUsage(),
                        m.getMemoryUsage(), m.getMemoryUsage(), m.getMemoryUsage(),
                        m.getDiskUsage(), m.getDiskUsage(), m.getDiskUsage(),
                        1L
                    })
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * 转换聚合数据为图表格式
     */
    private Map<String, Object> convertAggregatedToChartData(List<Object[]> aggregatedData, String aggregationType) {
        Map<String, Object> chartData = new HashMap<>();
        
        List<String> timestamps = new ArrayList<>();
        List<Double> avgCpuData = new ArrayList<>();
        List<Double> maxCpuData = new ArrayList<>();
        List<Double> minCpuData = new ArrayList<>();
        List<Double> avgMemoryData = new ArrayList<>();
        List<Double> maxMemoryData = new ArrayList<>();
        List<Double> minMemoryData = new ArrayList<>();
        List<Double> avgDiskData = new ArrayList<>();
        List<Double> maxDiskData = new ArrayList<>();
        List<Double> minDiskData = new ArrayList<>();
        
        DateTimeFormatter formatter = aggregationType.equals("day") 
            ? DateTimeFormatter.ofPattern("MM-dd")
            : DateTimeFormatter.ofPattern("MM-dd HH:mm");
        
        for (Object[] row : aggregatedData) {
            // row结构: serverId, serverName, timeGroup, avgCpu, maxCpu, minCpu, avgMem, maxMem, minMem, avgDisk, maxDisk, minDisk, count
            // 实际从getAggregatedData返回的结构，timeGroup在索引2位置
            if (row.length < 13) {
                logger.warn("数组长度不足，期望13个元素，实际{}个元素", row.length);
                continue;
            }
            
            LocalDateTime timeGroup = LocalDateTime.parse((String) row[2], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            timestamps.add(timeGroup.format(formatter));
            
            avgCpuData.add(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0);
            maxCpuData.add(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0);
            minCpuData.add(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0);
            
            avgMemoryData.add(row[6] != null ? ((Number) row[6]).doubleValue() : 0.0);
            maxMemoryData.add(row[7] != null ? ((Number) row[7]).doubleValue() : 0.0);
            minMemoryData.add(row[8] != null ? ((Number) row[8]).doubleValue() : 0.0);
            
            avgDiskData.add(row[9] != null ? ((Number) row[9]).doubleValue() : 0.0);
            maxDiskData.add(row[10] != null ? ((Number) row[10]).doubleValue() : 0.0);
            minDiskData.add(row[11] != null ? ((Number) row[11]).doubleValue() : 0.0);
        }
        
        chartData.put("timestamps", timestamps);
        chartData.put("cpu", Map.of("avg", avgCpuData, "max", maxCpuData, "min", minCpuData));
        chartData.put("memory", Map.of("avg", avgMemoryData, "max", maxMemoryData, "min", minMemoryData));
        chartData.put("disk", Map.of("avg", avgDiskData, "max", maxDiskData, "min", minDiskData));
        
        return chartData;
    }
    
    /**
     * 转换原始数据为图表格式
     */
    private Map<String, Object> convertRawToChartData(List<ServerMetrics> rawData) {
        Map<String, Object> chartData = new HashMap<>();
        
        List<String> timestamps = new ArrayList<>();
        List<Double> cpuData = new ArrayList<>();
        List<Double> memoryData = new ArrayList<>();
        List<Double> diskData = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        
        for (ServerMetrics metric : rawData) {
            timestamps.add(metric.getTimestamp().format(formatter));
            cpuData.add(metric.getCpuUsage() != null ? metric.getCpuUsage() : 0.0);
            memoryData.add(metric.getMemoryUsage() != null ? metric.getMemoryUsage() : 0.0);
            diskData.add(metric.getDiskUsage() != null ? metric.getDiskUsage() : 0.0);
        }
        
        chartData.put("timestamps", timestamps);
        chartData.put("cpu", cpuData);
        chartData.put("memory", memoryData);
        chartData.put("disk", diskData);
        
        return chartData;
    }
    
    /**
     * 获取服务器名称
     */
    private String getServerName(Long serverId) {
        Optional<Server> serverOpt = serverService.findById(serverId);
        return serverOpt.map(Server::getName).orElse("Unknown Server");
    }
}