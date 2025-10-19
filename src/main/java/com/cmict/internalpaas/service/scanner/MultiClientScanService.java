package com.cmict.internalpaas.service.scanner;

import com.cmict.internalpaas.dto.SSHHostConfig;
import com.cmict.internalpaas.service.registry.ScannerRegistry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 多客户端扫描服务 - 协调多个 SSH 客户端扫描器
 * 
 * <p>功能:
 * <ul>
 *   <li>并行扫描所有已安装的 SSH 客户端</li>
 *   <li>聚合所有扫描结果</li>
 *   <li>提供统一的扫描接口</li>
 *   <li>记录扫描统计信息</li>
 * </ul>
 * 
 * <p>扫描流程:
 * <ol>
 *   <li>从 ScannerRegistry 获取所有已安装的扫描器</li>
 *   <li>并行执行所有扫描器的 scanConfigurations()</li>
 *   <li>聚合所有扫描结果</li>
 *   <li>返回 MultiScanResult</li>
 * </ol>
 * 
 * <p>使用示例:
 * <pre>
 * MultiScanResult result = multiClientScanService.scanAllClients();
 * System.out.println("Total hosts found: " + result.getTotalHostCount());
 * System.out.println("Scanned clients: " + result.getScannedClients());
 * </pre>
 * 
 * @author Internal PaaS Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiClientScanService {
    
    private final ScannerRegistry scannerRegistry;
    
    /**
     * 扫描所有已安装的 SSH 客户端配置
     * 
     * <p>扫描策略:
     * <ul>
     *   <li>并行扫描: 使用固定大小的线程池并行执行</li>
     *   <li>异常隔离: 单个扫描器失败不影响其他扫描器</li>
     *   <li>超时控制: 可选的超时机制 (当前无超时限制)</li>
     * </ul>
     * 
     * @return 多客户端扫描结果
     */
    public MultiScanResult scanAllClients() {
        log.info("Starting multi-client scan...");
        Instant startTime = Instant.now();
        
        // 1. 获取所有已安装的扫描器
        List<ClientScanner> installedScanners = scannerRegistry.getInstalledScanners();
        
        if (installedScanners.isEmpty()) {
            log.warn("No SSH clients installed on this system");
            return MultiScanResult.builder()
                    .scannedClients(Collections.emptyList())
                    .clientResults(Collections.emptyMap())
                    .totalHostCount(0)
                    .scanDuration(Duration.ZERO)
                    .build();
        }
        
        // 2. 并行扫描所有客户端
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(installedScanners.size(), 3) // 最多3个并发
        );
        
        try {
            Map<String, CompletableFuture<ScanResult>> futures = new HashMap<>();
            
            // 为每个扫描器创建异步任务
            for (ClientScanner scanner : installedScanners) {
                String clientName = scanner.getClientName();
                CompletableFuture<ScanResult> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        log.debug("Scanning {} configurations...", clientName);
                        Instant scanStart = Instant.now();
                        ScanResult result = scanner.scanConfigurations();
                        long scanTimeMs = Duration.between(scanStart, Instant.now()).toMillis();
                        log.info("{} scan completed in {}ms: {} hosts found", 
                                clientName, scanTimeMs, result.getSessions().size());
                        return result;
                    } catch (Exception e) {
                        log.error("Error scanning {} configurations: {}", clientName, e.getMessage(), e);
                        // 返回空结果而不是失败整个扫描
                        return ScanResult.builder()
                                .clientName(clientName)
                                .sessions(Collections.emptyList())
                                .configPath(null)
                                .build();
                    }
                }, executor);
                
                futures.put(clientName, future);
            }
            
            // 3. 等待所有扫描完成并收集结果
            Map<String, ScanResult> clientResults = new HashMap<>();
            for (Map.Entry<String, CompletableFuture<ScanResult>> entry : futures.entrySet()) {
                try {
                    ScanResult result = entry.getValue().join(); // 阻塞等待
                    clientResults.put(entry.getKey(), result);
                } catch (Exception e) {
                    log.error("Failed to get scan result for {}: {}", entry.getKey(), e.getMessage());
                }
            }
            
            // 4. 聚合结果
            List<String> scannedClients = new ArrayList<>(clientResults.keySet());
            int totalHostCount = clientResults.values().stream()
                    .mapToInt(result -> result.getSessions().size())
                    .sum();
            
            Duration scanDuration = Duration.between(startTime, Instant.now());
            
            log.info("Multi-client scan completed in {}ms: {} clients, {} total hosts", 
                    scanDuration.toMillis(), scannedClients.size(), totalHostCount);
            
            return MultiScanResult.builder()
                    .scannedClients(scannedClients)
                    .clientResults(clientResults)
                    .totalHostCount(totalHostCount)
                    .scanDuration(scanDuration)
                    .build();
            
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * 扫描指定的 SSH 客户端配置
     * 
     * @param clientName 客户端名称 (如 "SecureCRT", "Xshell", "Tabby")
     * @return 单个客户端的扫描结果,如果客户端未安装或不存在则返回 null
     */
    public ScanResult scanClient(String clientName) {
        log.info("Scanning {} configurations...", clientName);
        
        ClientScanner scanner = scannerRegistry.getScannerByName(clientName);
        if (scanner == null) {
            log.warn("Scanner for {} not found", clientName);
            return null;
        }
        
        if (!scanner.isInstalled()) {
            log.warn("{} is not installed on this system", clientName);
            return null;
        }
        
        try {
            return scanner.scanConfigurations();
        } catch (Exception e) {
            log.error("Error scanning {} configurations: {}", clientName, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取所有已安装客户端的名称列表
     * 
     * @return 已安装客户端名称列表
     */
    public List<String> getInstalledClientNames() {
        return scannerRegistry.getInstalledScanners().stream()
                .map(ClientScanner::getClientName)
                .collect(Collectors.toList());
    }
    
    /**
     * 多客户端扫描结果 DTO
     * 
     * <p>包含信息:
     * <ul>
     *   <li>已扫描的客户端列表</li>
     *   <li>每个客户端的扫描结果</li>
     *   <li>主机总数</li>
     *   <li>扫描耗时</li>
     * </ul>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MultiScanResult {
        /**
         * 已扫描的客户端名称列表
         */
        private List<String> scannedClients;
        
        /**
         * 每个客户端的扫描结果 (客户端名称 -> 扫描结果)
         */
        private Map<String, ScanResult> clientResults;
        
        /**
         * 所有客户端发现的主机总数 (未去重)
         */
        private int totalHostCount;
        
        /**
         * 扫描总耗时
         */
        private Duration scanDuration;
        
        /**
         * 获取所有主机配置 (未去重)
         * 
         * @return 所有主机配置列表
         */
        public List<SSHHostConfig> getAllHosts() {
            return clientResults.values().stream()
                    .flatMap(result -> result.getSessions().stream())
                    .collect(Collectors.toList());
        }
        
        /**
         * 按客户端名称获取主机配置
         * 
         * @param clientName 客户端名称
         * @return 该客户端的主机配置列表,如果不存在则返回空列表
         */
        public List<SSHHostConfig> getHostsByClient(String clientName) {
            ScanResult result = clientResults.get(clientName);
            return result != null ? result.getSessions() : Collections.emptyList();
        }
        
        /**
         * 获取扫描成功的客户端数量
         * 
         * @return 成功扫描的客户端数量
         */
        public int getSuccessfulScanCount() {
            return (int) clientResults.values().stream()
                    .filter(result -> !result.getSessions().isEmpty())
                    .count();
        }
    }
}
