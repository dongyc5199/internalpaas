package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.SSHHostConfig;
import com.cmict.internalpaas.service.aggregator.ResultAggregator;
import com.cmict.internalpaas.service.registry.ScannerRegistry;
import com.cmict.internalpaas.service.scanner.ClientScanner;
import com.cmict.internalpaas.service.scanner.MultiClientScanService;
import com.cmict.internalpaas.service.scanner.ScanResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SSH客户端配置扫描控制器
 * SSH Client Scanner Controller
 * 
 * <p>提供SSH客户端配置自动扫描功能的REST API接口，支持：</p>
 * <ul>
 *   <li>扫描所有已安装的SSH客户端（SecureCRT、Xshell、Tabby等）</li>
 *   <li>扫描单个指定SSH客户端</li>
 *   <li>获取系统中已注册的扫描器列表</li>
 *   <li>获取已安装的SSH客户端列表</li>
 *   <li>自动去重和合并扫描结果</li>
 * </ul>
 * 
 * <h3>API端点</h3>
 * <ul>
 *   <li>POST /api/ssh-scan/scan-all - 扫描所有SSH客户端</li>
 *   <li>POST /api/ssh-scan/scan/{clientName} - 扫描单个SSH客户端</li>
 *   <li>GET /api/ssh-scan/scanners - 获取所有注册的扫描器</li>
 *   <li>GET /api/ssh-scan/installed-scanners - 获取已安装的扫描器</li>
 * </ul>
 * 
 * <h3>扫描流程</h3>
 * <ol>
 *   <li>检测系统中已安装的SSH客户端</li>
 *   <li>并行扫描各客户端的配置文件（最多3个并发）</li>
 *   <li>聚合所有扫描结果</li>
 *   <li>按 hostname+port+user 去重</li>
 *   <li>选择信息最完整的配置</li>
 *   <li>记录来源客户端信息</li>
 *   <li>返回统计信息和去重后的主机列表</li>
 * </ol>
 * 
 * <h3>响应示例</h3>
 * <pre>{@code
 * {
 *   "success": true,
 *   "message": "扫描完成",
 *   "data": {
 *     "scanSummary": {
 *       "scannedClients": ["SecureCRT", "Xshell"],
 *       "successfulScanCount": 2,
 *       "totalHostCount": 150,
 *       "scanDuration": "PT2.5S"
 *     },
 *     "aggregatedResult": {
 *       "uniqueHosts": [...],
 *       "totalInputCount": 150,
 *       "uniqueCount": 120,
 *       "duplicateCount": 30,
 *       "deduplicationRate": 0.20,
 *       "sourceStatistics": {
 *         "SecureCRT": 80,
 *         "Xshell": 40
 *       }
 *     }
 *   }
 * }
 * }</pre>
 * 
 * @author InternalPaaS Team
 * @since 2025-10-19 (Phase 6 - Frontend Integration)
 * @see MultiClientScanService
 * @see ResultAggregator
 * @see ScannerRegistry
 */
@Tag(name = "SSH客户端扫描", description = "SSH客户端配置自动扫描API。支持扫描SecureCRT、Xshell、Tabby等主流SSH客户端的配置文件，自动去重合并。")
@Slf4j
@RestController
@RequestMapping("/api/ssh-scan")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class SSHScanController {
    
    private final MultiClientScanService scanService;
    private final ResultAggregator aggregator;
    private final ScannerRegistry scannerRegistry;
    
    /**
     * 扫描所有已安装的SSH客户端
     * Scan all installed SSH clients
     * 
     * <p>自动检测系统中已安装的SSH客户端（SecureCRT、Xshell、Tabby等），
     * 并行扫描各客户端的配置文件，聚合结果并自动去重。</p>
     * 
     * <p>POST /api/ssh-scan/scan-all</p>
     * 
     * <h3>扫描过程</h3>
     * <ol>
     *   <li>检测已安装的SSH客户端</li>
     *   <li>并行扫描配置文件（最多3个并发）</li>
     *   <li>按 hostname:port@user 去重</li>
     *   <li>选择信息最完整的配置</li>
     *   <li>统计来源分布</li>
     * </ol>
     * 
     * @return 扫描结果，包含去重后的主机列表、统计信息和来源分布
     *         - 200: 扫描成功
     *         - 404: 未找到已安装的SSH客户端
     *         - 500: 扫描失败
     */
    @Operation(
        summary = "扫描所有SSH客户端",
        description = "自动检测并扫描系统中已安装的所有SSH客户端（SecureCRT、Xshell、Tabby等），"
            + "并行扫描配置文件，自动去重合并，返回统一的主机列表。最多3个客户端并发扫描。"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "扫描成功",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "未找到已安装的SSH客户端",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "扫描失败",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/scan-all")
    public ResponseEntity<Map<String, Object>> scanAllClients() {
        log.info("开始扫描所有SSH客户端");
        
        try {
            // 1. 检查是否有已安装的扫描器
            List<ClientScanner> installedScanners = scannerRegistry.getInstalledScanners();
            if (installedScanners.isEmpty()) {
                log.warn("未找到已安装的SSH客户端");
                return ResponseEntity.status(404).body(createErrorResponse(
                    "未找到已安装的SSH客户端",
                    "请确保已安装 SecureCRT、Xshell 或 Tabby 中的至少一个客户端"
                ));
            }
            
            log.info("找到 {} 个已安装的SSH客户端: {}", 
                installedScanners.size(),
                installedScanners.stream()
                    .map(ClientScanner::getClientName)
                    .collect(Collectors.joining(", "))
            );
            
            // 2. 并行扫描所有客户端
            MultiClientScanService.MultiScanResult multiScanResult = scanService.scanAllClients();
            
            // 3. 聚合并去重结果
            List<SSHHostConfig> allHosts = multiScanResult.getAllHosts();
            Map<String, List<SSHHostConfig>> clientHostsMap = new HashMap<>();
            for (String clientName : multiScanResult.getScannedClients()) {
                clientHostsMap.put(clientName, multiScanResult.getHostsByClient(clientName));
            }
            ResultAggregator.AggregatedResult aggregatedResult = aggregator.aggregate(allHosts, clientHostsMap);
            
            log.info("扫描完成: 总共 {} 个主机，去重后 {} 个，去重率 {:.2f}%",
                aggregatedResult.getTotalInputCount(),
                aggregatedResult.getUniqueCount(),
                aggregatedResult.getDeduplicationRate() * 100
            );
            
            // 4. 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "扫描完成");
            
            Map<String, Object> data = new HashMap<>();
            
            // 扫描摘要
            Map<String, Object> scanSummary = new HashMap<>();
            scanSummary.put("scannedClients", multiScanResult.getScannedClients());
            scanSummary.put("successfulScanCount", multiScanResult.getSuccessfulScanCount());
            scanSummary.put("totalHostCount", multiScanResult.getTotalHostCount());
            scanSummary.put("scanDuration", multiScanResult.getScanDuration().toString());
            data.put("scanSummary", scanSummary);
            
            // 聚合结果
            Map<String, Object> aggregatedData = new HashMap<>();
            aggregatedData.put("uniqueHosts", aggregatedResult.getUniqueHosts());
            aggregatedData.put("hostsWithSource", aggregatedResult.getHostsWithSource());
            aggregatedData.put("totalInputCount", aggregatedResult.getTotalInputCount());
            aggregatedData.put("uniqueCount", aggregatedResult.getUniqueCount());
            aggregatedData.put("duplicateCount", aggregatedResult.getDuplicateCount());
            aggregatedData.put("deduplicationRate", aggregatedResult.getDeduplicationRate());
            aggregatedData.put("sourceStatistics", aggregatedResult.getSourceStatistics());
            data.put("aggregatedResult", aggregatedData);
            
            response.put("data", data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("扫描所有SSH客户端失败", e);
            return ResponseEntity.status(500).body(createErrorResponse(
                "扫描失败",
                e.getMessage()
            ));
        }
    }
    
    /**
     * 扫描单个指定的SSH客户端
     * Scan a specific SSH client
     * 
     * <p>扫描指定名称的SSH客户端配置文件。支持的客户端名称：
     * SecureCRT、Xshell、Tabby（不区分大小写）。</p>
     * 
     * <p>POST /api/ssh-scan/scan/{clientName}</p>
     * 
     * @param clientName SSH客户端名称（SecureCRT/Xshell/Tabby，不区分大小写）
     * @return 扫描结果，包含该客户端的主机列表
     *         - 200: 扫描成功
     *         - 404: 客户端未找到或未安装
     *         - 500: 扫描失败
     */
    @Operation(
        summary = "扫描单个SSH客户端",
        description = "扫描指定名称的SSH客户端配置文件。支持 SecureCRT、Xshell、Tabby（不区分大小写）。"
            + "返回该客户端的完整配置列表，不进行去重操作。"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "扫描成功",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "客户端未找到或未安装",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "扫描失败",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/scan/{clientName}")
    public ResponseEntity<Map<String, Object>> scanClient(
        @Parameter(description = "SSH客户端名称（SecureCRT/Xshell/Tabby）", example = "SecureCRT")
        @PathVariable String clientName
    ) {
        log.info("开始扫描SSH客户端: {}", clientName);
        
        try {
            // 1. 检查扫描器是否存在
            if (!scannerRegistry.hasScannerForClient(clientName)) {
                log.warn("未找到客户端扫描器: {}", clientName);
                return ResponseEntity.status(404).body(createErrorResponse(
                    "客户端未找到",
                    String.format("不支持的SSH客户端: %s。支持的客户端: %s", 
                        clientName, 
                        String.join(", ", scannerRegistry.getScannerNames()))
                ));
            }
            
            // 2. 扫描指定客户端
            long startTime = System.nanoTime();
            ScanResult scanResult = scanService.scanClient(clientName);
            long endTime = System.nanoTime();
            String scanDuration = String.format("%.2fs", (endTime - startTime) / 1_000_000_000.0);
            
            if (scanResult == null) {
                log.warn("客户端 {} 扫描结果为空", clientName);
                return ResponseEntity.status(404).body(createErrorResponse(
                    "扫描失败",
                    String.format("客户端 %s 可能未安装或配置文件不存在", clientName)
                ));
            }
            
            log.info("扫描完成: 客户端 {} 找到 {} 个主机",
                clientName,
                scanResult.getSessions().size()
            );
            
            // 3. 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "扫描完成");
            
            Map<String, Object> data = new HashMap<>();
            data.put("clientName", clientName);
            data.put("hostCount", scanResult.getSessions().size());
            data.put("hosts", scanResult.getSessions());
            data.put("scanDuration", scanDuration);
            
            response.put("data", data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("扫描SSH客户端 {} 失败", clientName, e);
            return ResponseEntity.status(500).body(createErrorResponse(
                "扫描失败",
                e.getMessage()
            ));
        }
    }
    
    /**
     * 获取所有注册的SSH客户端扫描器
     * Get all registered SSH client scanners
     * 
     * <p>返回系统中所有注册的SSH客户端扫描器列表，包括已安装和未安装的。
     * 每个扫描器包含客户端名称、版本号和安装状态。</p>
     * 
     * <p>GET /api/ssh-scan/scanners</p>
     * 
     * @return 扫描器列表，包含客户端名称、版本和安装状态
     *         - 200: 获取成功
     */
    @Operation(
        summary = "获取所有扫描器",
        description = "返回系统中所有注册的SSH客户端扫描器列表，包括已安装和未安装的。"
            + "每个扫描器包含客户端名称、版本号和安装状态。"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "获取成功",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/scanners")
    public ResponseEntity<Map<String, Object>> getAllScanners() {
        log.info("获取所有SSH客户端扫描器");
        
        try {
            List<ClientScanner> scanners = scannerRegistry.getAllScanners();
            
            List<Map<String, Object>> scannerInfoList = scanners.stream()
                .map(this::buildScannerInfo)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取成功");
            
            Map<String, Object> data = new HashMap<>();
            data.put("scanners", scannerInfoList);
            data.put("totalCount", scannerInfoList.size());
            
            response.put("data", data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取扫描器列表失败", e);
            return ResponseEntity.status(500).body(createErrorResponse(
                "获取失败",
                e.getMessage()
            ));
        }
    }
    
    /**
     * 获取已安装的SSH客户端扫描器
     * Get installed SSH client scanners
     * 
     * <p>返回系统中已安装的SSH客户端扫描器列表。
     * 只包含检测到已安装的客户端（isInstalled() = true）。</p>
     * 
     * <p>GET /api/ssh-scan/installed-scanners</p>
     * 
     * @return 已安装的扫描器列表
     *         - 200: 获取成功
     *         - 404: 未找到已安装的SSH客户端
     */
    @Operation(
        summary = "获取已安装的扫描器",
        description = "返回系统中已安装的SSH客户端扫描器列表。"
            + "只包含检测到已安装的客户端（通过注册表、应用目录等方式检测）。"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "获取成功",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "未找到已安装的SSH客户端",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/installed-scanners")
    public ResponseEntity<Map<String, Object>> getInstalledScanners() {
        log.info("获取已安装的SSH客户端扫描器");
        
        try {
            List<ClientScanner> installedScanners = scannerRegistry.getInstalledScanners();
            
            if (installedScanners.isEmpty()) {
                log.warn("未找到已安装的SSH客户端");
                return ResponseEntity.status(404).body(createErrorResponse(
                    "未找到已安装的SSH客户端",
                    "请确保已安装 SecureCRT、Xshell 或 Tabby 中的至少一个客户端"
                ));
            }
            
            List<Map<String, Object>> scannerInfoList = installedScanners.stream()
                .map(this::buildScannerInfo)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取成功");
            
            Map<String, Object> data = new HashMap<>();
            data.put("scanners", scannerInfoList);
            data.put("totalCount", scannerInfoList.size());
            
            response.put("data", data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取已安装扫描器列表失败", e);
            return ResponseEntity.status(500).body(createErrorResponse(
                "获取失败",
                e.getMessage()
            ));
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建扫描器信息对象
     * 
     * @param scanner 客户端扫描器
     * @return 扫描器信息Map
     */
    private Map<String, Object> buildScannerInfo(ClientScanner scanner) {
        Map<String, Object> info = new HashMap<>();
        info.put("clientName", scanner.getClientName());
        info.put("clientVersion", scanner.getClientVersion());
        info.put("installed", scanner.isInstalled());
        
        return info;
    }
    
    /**
     * 创建错误响应
     * 
     * @param message 错误消息
     * @param detail 详细信息
     * @return 错误响应Map
     */
    private Map<String, Object> createErrorResponse(String message, String detail) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("detail", detail);
        return response;
    }
}
