package com.cmict.internalpaas.service.registry;

import com.cmict.internalpaas.service.scanner.ClientScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 扫描器注册表 - 统一管理所有 SSH 客户端扫描器
 * 
 * <p>功能:
 * <ul>
 *   <li>自动注入所有 ClientScanner 实现类</li>
 *   <li>提供获取所有扫描器的接口</li>
 *   <li>提供获取已安装客户端扫描器的接口</li>
 *   <li>提供按名称查找扫描器的接口</li>
 * </ul>
 * 
 * <p>设计模式: Registry Pattern (注册表模式)
 * 
 * <p>使用示例:
 * <pre>
 * // 获取所有扫描器
 * List&lt;ClientScanner&gt; allScanners = scannerRegistry.getAllScanners();
 * 
 * // 获取已安装的扫描器
 * List&lt;ClientScanner&gt; installedScanners = scannerRegistry.getInstalledScanners();
 * 
 * // 按名称查找
 * Optional&lt;ClientScanner&gt; scanner = scannerRegistry.getScannerByName("SecureCRT");
 * </pre>
 * 
 * @author Internal PaaS Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class ScannerRegistry {
    
    private final List<ClientScanner> scanners;
    
    /**
     * 构造函数 - Spring 自动注入所有 ClientScanner Bean
     * 
     * @param scanners Spring 容器中所有的 ClientScanner 实现类
     */
    public ScannerRegistry(List<ClientScanner> scanners) {
        this.scanners = new ArrayList<>(scanners);
        log.info("Scanner Registry initialized with {} scanners: {}", 
                 scanners.size(), 
                 scanners.stream()
                         .map(ClientScanner::getClientName)
                         .toList());
    }
    
    /**
     * 获取所有注册的扫描器
     * 
     * @return 所有扫描器的不可变列表
     */
    public List<ClientScanner> getAllScanners() {
        return Collections.unmodifiableList(scanners);
    }
    
    /**
     * 获取已安装的扫描器 (即客户端已安装在系统上)
     * 
     * <p>检测逻辑:
     * <ol>
     *   <li>遍历所有扫描器</li>
     *   <li>调用 isInstalled() 检查客户端是否已安装</li>
     *   <li>返回已安装的扫描器列表</li>
 * </ol>
     * 
     * @return 已安装客户端的扫描器列表
     */
    public List<ClientScanner> getInstalledScanners() {
        List<ClientScanner> installed = scanners.stream()
                .filter(scanner -> {
                    try {
                        boolean isInstalled = scanner.isInstalled();
                        if (isInstalled) {
                            log.debug("{} client is installed", scanner.getClientName());
                        }
                        return isInstalled;
                    } catch (Exception e) {
                        log.warn("Error checking if {} is installed: {}", 
                                scanner.getClientName(), 
                                e.getMessage());
                        return false;
                    }
                })
                .toList();
        
        log.info("Found {} installed SSH clients: {}", 
                 installed.size(), 
                 installed.stream()
                          .map(ClientScanner::getClientName)
                          .toList());
        
        return installed;
    }
    
    /**
     * 按客户端名称查找扫描器
     * 
     * <p>查找逻辑:
     * <ul>
     *   <li>不区分大小写</li>
     *   <li>返回第一个匹配的扫描器</li>
     * </ul>
     * 
     * @param clientName 客户端名称 (如 "SecureCRT", "Xshell", "Tabby")
     * @return 匹配的扫描器,如果未找到则返回 null
     */
    public ClientScanner getScannerByName(String clientName) {
        if (clientName == null || clientName.trim().isEmpty()) {
            log.warn("Client name is null or empty");
            return null;
        }
        
        return scanners.stream()
                .filter(scanner -> scanner.getClientName()
                        .equalsIgnoreCase(clientName.trim()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取所有已注册的扫描器数量
     * 
     * @return 扫描器总数
     */
    public int getScannerCount() {
        return scanners.size();
    }
    
    /**
     * 检查是否存在指定名称的扫描器
     * 
     * @param clientName 客户端名称
     * @return 如果存在返回 true,否则返回 false
     */
    public boolean hasScannerForClient(String clientName) {
        return getScannerByName(clientName) != null;
    }
    
    /**
     * 获取所有扫描器的名称列表
     * 
     * @return 扫描器名称列表
     */
    public List<String> getScannerNames() {
        return scanners.stream()
                .map(ClientScanner::getClientName)
                .toList();
    }
}
