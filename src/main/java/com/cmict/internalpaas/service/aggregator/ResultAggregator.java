package com.cmict.internalpaas.service.aggregator;

import com.cmict.internalpaas.dto.SSHHostConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 结果聚合器 - 合并和去重多个扫描器的结果
 * 
 * <p>功能:
 * <ul>
 *   <li>按 hostname + port + user 去重</li>
 *   <li>保留最完整的配置信息</li>
 *   <li>记录配置来源 (哪个客户端)</li>
 *   <li>提供聚合统计信息</li>
 * </ul>
 * 
 * <p>去重策略:
 * <ul>
 *   <li>唯一性标识: hostname + port + user 三元组</li>
 *   <li>冲突解决: 保留字段更完整的配置 (优先保留有 identityFile 的)</li>
 *   <li>来源记录: 记录配置来自哪个客户端</li>
 * </ul>
 * 
 * <p>使用示例:
 * <pre>
 * ResultAggregator aggregator = new ResultAggregator();
 * AggregatedResult result = aggregator.aggregate(multiScanResult);
 * System.out.println("Unique hosts: " + result.getUniqueHosts().size());
 * System.out.println("Duplicate count: " + result.getDuplicateCount());
 * </pre>
 * 
 * @author Internal PaaS Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class ResultAggregator {
    
    /**
     * 聚合多客户端扫描结果
     * 
     * <p>聚合流程:
     * <ol>
     *   <li>收集所有配置</li>
     *   <li>按唯一键 (hostname+port+user) 分组</li>
     *   <li>每组保留最优配置</li>
     *   <li>生成统计信息</li>
     * </ol>
     * 
     * @param allHosts 所有主机配置列表
     * @param clientResults 客户端结果映射 (用于记录来源)
     * @return 聚合结果
     */
    public AggregatedResult aggregate(
            List<SSHHostConfig> allHosts,
            Map<String, List<SSHHostConfig>> clientResults) {
        
        log.info("Aggregating {} hosts from {} clients", 
                allHosts.size(), clientResults.size());
        
        // 1. 按唯一键分组
        Map<String, List<HostWithSource>> groupedHosts = allHosts.stream()
                .map(host -> {
                    String source = findHostSource(host, clientResults);
                    return new HostWithSource(host, source);
                })
                .collect(Collectors.groupingBy(
                        hws -> buildUniqueKey(hws.getHost()),
                        LinkedHashMap::new, // 保持插入顺序
                        Collectors.toList()
                ));
        
        // 2. 每组选择最优配置
        List<HostWithSource> uniqueHosts = new ArrayList<>();
        int duplicateCount = 0;
        
        for (Map.Entry<String, List<HostWithSource>> entry : groupedHosts.entrySet()) {
            List<HostWithSource> group = entry.getValue();
            
            if (group.size() > 1) {
                duplicateCount += (group.size() - 1);
                log.debug("Found {} duplicates for key: {}", group.size() - 1, entry.getKey());
            }
            
            // 选择最优配置
            HostWithSource best = selectBestConfig(group);
            uniqueHosts.add(best);
        }
        
        // 3. 生成统计信息
        Map<String, Integer> sourceStatistics = uniqueHosts.stream()
                .collect(Collectors.groupingBy(
                        HostWithSource::getSource,
                        Collectors.summingInt(hws -> 1)
                ));
        
        log.info("Aggregation completed: {} unique hosts (removed {} duplicates)", 
                uniqueHosts.size(), duplicateCount);
        log.info("Source statistics: {}", sourceStatistics);
        
        return AggregatedResult.builder()
                .uniqueHosts(uniqueHosts.stream()
                        .map(HostWithSource::getHost)
                        .collect(Collectors.toList()))
                .hostsWithSource(uniqueHosts)
                .totalInputCount(allHosts.size())
                .uniqueCount(uniqueHosts.size())
                .duplicateCount(duplicateCount)
                .sourceStatistics(sourceStatistics)
                .build();
    }
    
    /**
     * 构建唯一性键: hostname + port + user
     * 
     * <p>键格式: "hostname:port@user"
     * <p>示例: "192.168.1.100:22@root"
     * 
     * @param host 主机配置
     * @return 唯一性键
     */
    private String buildUniqueKey(SSHHostConfig host) {
        String hostname = host.getHostname() != null ? host.getHostname() : "unknown";
        int port = host.getPort() != null ? host.getPort() : 22;
        String user = host.getUser() != null ? host.getUser() : "unknown";
        return String.format("%s:%d@%s", hostname, port, user);
    }
    
    /**
     * 从多个配置中选择最优配置
     * 
     * <p>选择策略 (优先级从高到低):
     * <ol>
     *   <li>有 identityFile (私钥) 的配置</li>
     *   <li>有 description 的配置</li>
     *   <li>有 group 的配置</li>
     *   <li>第一个配置</li>
     * </ol>
     * 
     * @param configs 同一主机的多个配置
     * @return 最优配置
     */
    private HostWithSource selectBestConfig(List<HostWithSource> configs) {
        return configs.stream()
                .max(Comparator
                        .comparingInt((HostWithSource hws) -> 
                                hws.getHost().getIdentityFile() != null ? 10 : 0)
                        .thenComparingInt(hws -> 
                                hws.getHost().getDescription() != null && 
                                !hws.getHost().getDescription().isEmpty() ? 5 : 0)
                        .thenComparingInt(hws -> 
                                hws.getHost().getGroup() != null && 
                                !hws.getHost().getGroup().isEmpty() ? 3 : 0)
                )
                .orElse(configs.get(0));
    }
    
    /**
     * 查找主机配置的来源客户端
     * 
     * @param host 主机配置
     * @param clientResults 客户端结果映射
     * @return 客户端名称,如果找不到则返回 "Unknown"
     */
    private String findHostSource(SSHHostConfig host, Map<String, List<SSHHostConfig>> clientResults) {
        for (Map.Entry<String, List<SSHHostConfig>> entry : clientResults.entrySet()) {
            if (entry.getValue().contains(host)) {
                return entry.getKey();
            }
        }
        return "Unknown";
    }
    
    /**
     * 主机配置及其来源
     */
    public static class HostWithSource {
        private final SSHHostConfig host;
        private final String source;
        
        public HostWithSource(SSHHostConfig host, String source) {
            this.host = host;
            this.source = source;
        }
        
        public SSHHostConfig getHost() {
            return host;
        }
        
        public String getSource() {
            return source;
        }
    }
    
    /**
     * 聚合结果 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AggregatedResult {
        /**
         * 去重后的唯一主机列表
         */
        private List<SSHHostConfig> uniqueHosts;
        
        /**
         * 带来源信息的主机列表
         */
        private List<HostWithSource> hostsWithSource;
        
        /**
         * 输入的主机总数 (去重前)
         */
        private int totalInputCount;
        
        /**
         * 去重后的唯一主机数量
         */
        private int uniqueCount;
        
        /**
         * 重复的主机数量
         */
        private int duplicateCount;
        
        /**
         * 来源统计 (客户端名称 -> 主机数量)
         */
        private Map<String, Integer> sourceStatistics;
        
        /**
         * 获取去重率 (百分比)
         * 
         * @return 去重率 (0-100)
         */
        public double getDeduplicationRate() {
            if (totalInputCount == 0) {
                return 0.0;
            }
            return (duplicateCount * 100.0) / totalInputCount;
        }
        
        /**
         * 按来源获取主机列表
         * 
         * @param source 客户端名称
         * @return 该客户端贡献的主机列表
         */
        public List<SSHHostConfig> getHostsBySource(String source) {
            return hostsWithSource.stream()
                    .filter(hws -> hws.getSource().equals(source))
                    .map(HostWithSource::getHost)
                    .collect(Collectors.toList());
        }
    }
}
