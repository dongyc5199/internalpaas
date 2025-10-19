package com.cmict.internalpaas.service.integration;

import com.cmict.internalpaas.dto.SSHHostConfig;
import com.cmict.internalpaas.service.aggregator.ResultAggregator;
import com.cmict.internalpaas.service.registry.ScannerRegistry;
import com.cmict.internalpaas.service.scanner.ClientScanner;
import com.cmict.internalpaas.service.scanner.MultiClientScanService;
import com.cmict.internalpaas.service.scanner.ScanResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多扫描器集成测试
 * 
 * <p>测试目标:
 * <ul>
 *   <li>测试 ScannerRegistry 注册所有扫描器</li>
 *   <li>测试 MultiClientScanService 并行扫描</li>
 *   <li>测试 ResultAggregator 去重逻辑</li>
 *   <li>验证 SecureCRT/Xshell/Tabby 三个扫描器协同工作</li>
 * </ul>
 * 
 * @author Internal PaaS Team
 * @since 1.0.0
 */
@SpringBootTest
@Slf4j
public class MultiScannerIntegrationTest {
    
    @Autowired
    private ScannerRegistry scannerRegistry;
    
    @Autowired
    private MultiClientScanService multiClientScanService;
    
    @Autowired
    private ResultAggregator resultAggregator;
    
    /**
     * 测试 ScannerRegistry 是否成功注册所有扫描器
     */
    @Test
    void testScannerRegistry_AllScannersRegistered() {
        log.info("Testing ScannerRegistry...");
        
        // 验证扫描器数量 (应该至少有 3 个: SecureCRT, Xshell, Tabby)
        List<ClientScanner> allScanners = scannerRegistry.getAllScanners();
        assertThat(allScanners).isNotEmpty();
        assertThat(allScanners.size()).isGreaterThanOrEqualTo(3);
        
        // 验证扫描器名称
        List<String> scannerNames = scannerRegistry.getScannerNames();
        log.info("Registered scanners: {}", scannerNames);
        
        assertThat(scannerNames).contains("SecureCRT", "Xshell", "Tabby");
        
        log.info("✅ ScannerRegistry test passed: {} scanners registered", allScanners.size());
    }
    
    /**
     * 测试按名称查找扫描器
     */
    @Test
    void testScannerRegistry_FindByName() {
        log.info("Testing ScannerRegistry.getScannerByName()...");
        
        // 查找 SecureCRT 扫描器
        ClientScanner secureCRT = scannerRegistry.getScannerByName("SecureCRT");
        assertThat(secureCRT).isNotNull();
        assertThat(secureCRT.getClientName()).isEqualTo("SecureCRT");
        
        // 查找 Xshell 扫描器
        ClientScanner xshell = scannerRegistry.getScannerByName("Xshell");
        assertThat(xshell).isNotNull();
        assertThat(xshell.getClientName()).isEqualTo("Xshell");
        
        // 查找 Tabby 扫描器
        ClientScanner tabby = scannerRegistry.getScannerByName("Tabby");
        assertThat(tabby).isNotNull();
        assertThat(tabby.getClientName()).isEqualTo("Tabby");
        
        // 查找不存在的扫描器
        ClientScanner notFound = scannerRegistry.getScannerByName("NonExistent");
        assertThat(notFound).isNull();
        
        log.info("✅ ScannerRegistry.getScannerByName() test passed");
    }
    
    /**
     * 测试获取已安装的扫描器
     */
    @Test
    void testScannerRegistry_GetInstalledScanners() {
        log.info("Testing ScannerRegistry.getInstalledScanners()...");
        
        List<ClientScanner> installedScanners = scannerRegistry.getInstalledScanners();
        
        // 可能没有安装任何客户端,所以只验证返回非 null
        assertThat(installedScanners).isNotNull();
        
        if (!installedScanners.isEmpty()) {
            log.info("Found {} installed SSH clients: {}", 
                    installedScanners.size(),
                    installedScanners.stream()
                            .map(ClientScanner::getClientName)
                            .toList());
        } else {
            log.warn("No SSH clients installed on this system (test environment)");
        }
        
        log.info("✅ ScannerRegistry.getInstalledScanners() test passed");
    }
    
    /**
     * 测试 MultiClientScanService 扫描所有客户端
     * 
     * <p>注意: 此测试依赖于系统上是否安装了 SSH 客户端
     */
    @Test
    void testMultiClientScanService_ScanAllClients() {
        log.info("Testing MultiClientScanService.scanAllClients()...");
        
        // 执行扫描
        MultiClientScanService.MultiScanResult result = multiClientScanService.scanAllClients();
        
        // 验证结果不为 null
        assertThat(result).isNotNull();
        assertThat(result.getScannedClients()).isNotNull();
        assertThat(result.getClientResults()).isNotNull();
        
        log.info("Scan completed in {}ms", result.getScanDuration().toMillis());
        log.info("Scanned clients: {}", result.getScannedClients());
        log.info("Total hosts found: {}", result.getTotalHostCount());
        
        if (result.getTotalHostCount() > 0) {
            // 如果找到主机,验证结果结构
            assertThat(result.getScannedClients()).isNotEmpty();
            assertThat(result.getAllHosts()).hasSize(result.getTotalHostCount());
            
            // 打印每个客户端的结果
            for (String clientName : result.getScannedClients()) {
                ScanResult scanResult = result.getClientResults().get(clientName);
                log.info("{}: {} hosts found", clientName, scanResult.getSessions().size());
            }
        } else {
            log.warn("No SSH hosts found (test environment)");
        }
        
        log.info("✅ MultiClientScanService.scanAllClients() test passed");
    }
    
    /**
     * 测试 MultiClientScanService 扫描单个客户端
     */
    @Test
    void testMultiClientScanService_ScanSingleClient() {
        log.info("Testing MultiClientScanService.scanClient()...");
        
        // 扫描 SecureCRT
        ScanResult secureCRTResult = multiClientScanService.scanClient("SecureCRT");
        
        if (secureCRTResult != null) {
            assertThat(secureCRTResult.getClientName()).isEqualTo("SecureCRT");
            log.info("SecureCRT scan result: {} hosts", secureCRTResult.getSessions().size());
        } else {
            log.warn("SecureCRT not installed or scan failed (test environment)");
        }
        
        log.info("✅ MultiClientScanService.scanClient() test passed");
    }
    
    /**
     * 测试 ResultAggregator 去重逻辑
     * 
     * <p>使用模拟数据测试去重功能
     */
    @Test
    void testResultAggregator_Deduplication() {
        log.info("Testing ResultAggregator.aggregate()...");
        
        // 创建测试数据 (包含重复主机)
        SSHHostConfig host1 = new SSHHostConfig();
        host1.setHostname("192.168.1.100");
        host1.setPort(22);
        host1.setUser("root");
        host1.setHostPattern("prod-db-01");
        host1.setIdentityFile("~/.ssh/id_rsa");
        
        SSHHostConfig host2 = new SSHHostConfig();
        host2.setHostname("192.168.1.100");
        host2.setPort(22);
        host2.setUser("root");
        host2.setHostPattern("production-database");
        host2.setDescription("Production DB Server");
        
        SSHHostConfig host3 = new SSHHostConfig();
        host3.setHostname("dev-server-01.example.com");
        host3.setPort(2222);
        host3.setUser("developer");
        host3.setHostPattern("dev-api");
        
        List<SSHHostConfig> allHosts = List.of(host1, host2, host3);
        
        // 模拟客户端结果
        Map<String, List<SSHHostConfig>> clientResults = new HashMap<>();
        clientResults.put("SecureCRT", List.of(allHosts.get(0)));
        clientResults.put("Xshell", List.of(allHosts.get(1)));
        clientResults.put("Tabby", List.of(allHosts.get(2)));
        
        // 执行聚合
        ResultAggregator.AggregatedResult result = resultAggregator.aggregate(allHosts, clientResults);
        
        // 验证去重结果
        assertThat(result).isNotNull();
        assertThat(result.getTotalInputCount()).isEqualTo(3); // 输入 3 个主机
        assertThat(result.getUniqueCount()).isEqualTo(2);     // 去重后 2 个主机
        assertThat(result.getDuplicateCount()).isEqualTo(1);  // 1 个重复
        
        log.info("Total input: {}", result.getTotalInputCount());
        log.info("Unique hosts: {}", result.getUniqueCount());
        log.info("Duplicates removed: {}", result.getDuplicateCount());
        log.info("Deduplication rate: {:.2f}%", result.getDeduplicationRate());
        log.info("Source statistics: {}", result.getSourceStatistics());
        
        // 验证最优配置选择 (应该选择带 identityFile 的配置)
        SSHHostConfig duplicatedHost = result.getUniqueHosts().stream()
                .filter(host -> "192.168.1.100".equals(host.getHostname()))
                .findFirst()
                .orElse(null);
        
        assertThat(duplicatedHost).isNotNull();
        assertThat(duplicatedHost.getIdentityFile()).isEqualTo("~/.ssh/id_rsa"); // 应该选择有私钥的配置
        
        log.info("✅ ResultAggregator.aggregate() test passed");
    }
    
    /**
     * 集成测试: 完整的扫描 → 聚合流程
     */
    @Test
    void testFullIntegration_ScanAndAggregate() {
        log.info("Testing full integration: Scan → Aggregate...");
        
        // 1. 扫描所有客户端
        MultiClientScanService.MultiScanResult scanResult = multiClientScanService.scanAllClients();
        
        assertThat(scanResult).isNotNull();
        log.info("Step 1: Scanned {} clients, found {} total hosts", 
                scanResult.getScannedClients().size(), 
                scanResult.getTotalHostCount());
        
        if (scanResult.getTotalHostCount() > 0) {
            // 2. 准备聚合数据
            List<SSHHostConfig> allHosts = scanResult.getAllHosts();
            Map<String, List<SSHHostConfig>> clientHostMap = new HashMap<>();
            
            for (String clientName : scanResult.getScannedClients()) {
                clientHostMap.put(clientName, scanResult.getHostsByClient(clientName));
            }
            
            // 3. 执行聚合去重
            ResultAggregator.AggregatedResult aggregatedResult = 
                    resultAggregator.aggregate(allHosts, clientHostMap);
            
            assertThat(aggregatedResult).isNotNull();
            assertThat(aggregatedResult.getUniqueCount()).isLessThanOrEqualTo(scanResult.getTotalHostCount());
            
            log.info("Step 2: Aggregation completed");
            log.info("  - Input: {} hosts", aggregatedResult.getTotalInputCount());
            log.info("  - Output: {} unique hosts", aggregatedResult.getUniqueCount());
            log.info("  - Removed: {} duplicates", aggregatedResult.getDuplicateCount());
            log.info("  - Deduplication rate: {:.2f}%", aggregatedResult.getDeduplicationRate());
            log.info("  - Source statistics: {}", aggregatedResult.getSourceStatistics());
            
        } else {
            log.warn("No hosts found to aggregate (test environment)");
        }
        
        log.info("✅ Full integration test passed");
    }
}
