package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.SSHConfigParseResult;
import com.cmict.internalpaas.dto.ServerImportDto;
import com.cmict.internalpaas.dto.ServerImportResult;
import com.cmict.internalpaas.service.SSHConfigImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SSH配置导入控制器
 * SSH Config Import Controller
 *
 * 提供SSH配置文件导入功能的REST API接口，支持：
 * - 上传SSH配置文件解析
 * - 解析本地SSH配置文件
 * - 预览导入（去重检查）
 * - 批量导入服务器
 *
 * API端点:
 * - POST /api/ssh-config-import/upload - 上传并解析SSH配置文件
 * - POST /api/ssh-config-import/parse-local - 解析本地SSH配置文件
 * - POST /api/ssh-config-import/preview - 预览导入（去重检查）
 * - POST /api/ssh-config-import/batch - 批量导入服务器
 * - GET  /api/ssh-config-import/default-path - 获取默认配置路径
 *
 * @author GitHub Copilot
 * @since 2025-10-18
 */
@Tag(name = "SSH配置导入", description = "SSH配置文件批量导入服务器管理API。支持上传配置文件、解析本地配置、预览去重、批量导入等功能。")
@RestController
@RequestMapping("/api/ssh-config-import")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class SSHConfigImportController {

    private static final Logger logger = LoggerFactory.getLogger(SSHConfigImportController.class);

    /**
     * 最大文件大小限制（1MB）
     */
    private static final long MAX_FILE_SIZE = 1024 * 1024; // 1MB

    @Autowired
    private SSHConfigImportService sshConfigImportService;

    /**
     * 上传SSH配置文件并解析
     * Upload and parse SSH config file
     *
     * POST /api/ssh-config-import/upload
     *
     * @param file 上传的SSH配置文件（最大1MB）
     * @return SSH配置解析结果
     *         - 200: 解析成功
     *         - 400: 文件格式错误或超过大小限制
     *         - 500: 服务器内部错误
     */
    @Operation(
        summary = "上传SSH配置文件",
        description = "上传SSH配置文件并解析为服务器列表。支持标准OpenSSH配置格式。文件大小限制1MB。"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "解析成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SSHConfigParseResult.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "文件格式错误、文件为空或超过大小限制",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "服务器内部错误",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadConfig(
            @Parameter(description = "SSH配置文件（最大1MB，支持text/plain和application/octet-stream）", required = true)
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        logger.info("收到SSH配置文件上传请求，文件名: {}, 大小: {} 字节",
                file.getOriginalFilename(), file.getSize());

        try {
            // 1. 验证文件
            if (file.isEmpty()) {
                logger.warn("上传文件为空");
                logAudit("文件上传", "文件为空", false, request);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("文件为空，请选择有效的SSH配置文件"));
            }

            // 2. 检查文件大小
            if (file.getSize() > MAX_FILE_SIZE) {
                logger.warn("文件大小超过限制: {} 字节 > {} 字节", file.getSize(), MAX_FILE_SIZE);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(
                                String.format("文件大小超过限制，最大允许 %d MB", MAX_FILE_SIZE / 1024 / 1024)));
            }

            // 3. 检查文件类型（仅允许文本文件）
            String contentType = file.getContentType();
            if (contentType != null && !contentType.startsWith("text/") &&
                !contentType.equals("application/octet-stream")) {
                logger.warn("不支持的文件类型: {}", contentType);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("不支持的文件类型，仅允许文本文件"));
            }

            // 4. 读取文件内容
            String configContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 5. 临时保存到文件并解析
            // 注意：这里直接使用SSHConfigParser.parseConfig(configContent)
            // 因为我们已经读取了内容，不需要通过文件路径
            logger.info("开始解析上传的SSH配置文件");
            SSHConfigParseResult result = parseConfigContent(configContent);

            logger.info("SSH配置文件解析完成，解析到 {} 个Host", result.getTotalHosts());

            // 6. 验证内容格式：至少包含一个有效的Host配置
            if (result.getServers() == null || result.getServers().isEmpty()) {
                logger.warn("配置文件中未找到有效的SSH Host配置");
                logAudit("文件上传", "格式错误：未找到有效Host配置", false, request);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("配置文件格式错误：未找到有效的SSH Host配置，请确保文件包含至少一个Host块"));
            }

            // 7. 记录审计日志 - 上传成功
            logAudit("文件上传",
                    String.format("文件: %s, 大小: %d bytes, 解析到 %d 个Host",
                                 file.getOriginalFilename(), file.getSize(), result.getTotalHosts()),
                    true, request);

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            logger.error("读取上传文件失败", e);
            logAudit("文件上传", "IO错误: " + e.getMessage(), false, request);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("读取文件失败: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("解析SSH配置文件失败", e);
            logAudit("文件上传", "解析失败: " + e.getMessage(), false, request);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("解析文件失败: " + e.getMessage()));
        }
    }

    /**
     * 解析本地SSH配置文件
     * Parse local SSH config file
     *
     * POST /api/ssh-config-import/parse-local?path={path}
     *
     * @param path 配置文件路径（可选，默认~/.ssh/config）
     * @return SSH配置解析结果
     *         - 200: 解析成功
     *         - 404: 配置文件不存在
     *         - 500: 服务器内部错误
     */
    @Operation(
        summary = "解析本地SSH配置文件",
        description = "解析服务器本地的SSH配置文件。如果不指定path参数，默认解析~/.ssh/config文件。"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "解析成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SSHConfigParseResult.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "配置文件不存在",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "服务器内部错误",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/parse-local")
    public ResponseEntity<?> parseLocalConfig(
            @Parameter(description = "配置文件路径（可选）。默认：~/.ssh/config", required = false)
            @RequestParam(value = "path", required = false) String path) {

        logger.info("收到解析本地SSH配置请求，路径: {}", path != null ? path : "默认路径");

        try {
            SSHConfigParseResult result = sshConfigImportService.parseLocalConfig(path);

            if (result.hasErrors()) {
                logger.warn("解析SSH配置失败: {}", result.getErrors());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(result);
            }

            logger.info("本地SSH配置解析完成，解析到 {} 个Host", result.getTotalHosts());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("解析本地SSH配置失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("解析失败: " + e.getMessage()));
        }
    }

    /**
     * 预览导入 - 执行去重检查
     * Preview import with duplicate check
     *
     * POST /api/ssh-config-import/preview
     *
     * @param servers 待导入的服务器列表
     * @return 更新后的服务器列表（包含重复标记）
     *         - 200: 预览成功
     *         - 400: 请求参数错误
     */
    @Operation(
        summary = "预览导入（去重检查）",
        description = "在正式导入前预览服务器列表，执行去重检查和字段验证。返回更新后的服务器列表，包含duplicate和valid标记。"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "预览成功",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "400",
            description = "服务器列表为空或参数错误",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/preview")
    public ResponseEntity<?> previewImport(
            @Parameter(description = "待导入的服务器列表", required = true)
            @RequestBody List<ServerImportDto> servers) {

        logger.info("收到预览导入请求，服务器数量: {}", servers != null ? servers.size() : 0);

        try {
            if (servers == null || servers.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("服务器列表不能为空"));
            }

            List<ServerImportDto> result = sshConfigImportService.previewImport(servers);

            logger.info("预览导入完成，有效服务器: {}, 重复服务器: {}, 无效服务器: {}",
                    result.stream().filter(dto -> dto.isValid() && !dto.isDuplicate()).count(),
                    result.stream().filter(ServerImportDto::isDuplicate).count(),
                    result.stream().filter(dto -> !dto.isValid()).count());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("预览导入失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("预览失败: " + e.getMessage()));
        }
    }

    /**
     * 批量导入服务器
     * Batch import servers
     *
     * POST /api/ssh-config-import/batch
     *
     * @param servers 待导入的服务器列表
     * @return 导入结果（成功数、失败数、详情）
     *         - 200: 全部成功
     *         - 207: 部分成功（Multi-Status）
     *         - 400: 全部失败或请求参数错误
     *         - 500: 服务器内部错误
     */
    @Operation(
        summary = "批量导入服务器",
        description = "将经过预览和验证的服务器列表批量导入到系统。自动跳过重复服务器，异步触发SSH连接测试。"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "全部导入成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ServerImportResult.class))
        ),
        @ApiResponse(
            responseCode = "207",
            description = "部分导入成功（Multi-Status）",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ServerImportResult.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "全部导入失败或服务器列表为空",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ServerImportResult.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "服务器内部错误",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/batch")
    public ResponseEntity<?> batchImport(
            @Parameter(description = "待导入的服务器列表", required = true)
            @RequestBody List<ServerImportDto> servers,
            HttpServletRequest request) {

        logger.info("收到批量导入请求，服务器数量: {}", servers != null ? servers.size() : 0);

        try {
            if (servers == null || servers.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("服务器列表不能为空"));
            }

            ServerImportResult result = sshConfigImportService.batchImport(servers);

            logger.info("批量导入完成，成功: {}, 失败: {}, 成功率: {:.1f}%",
                    result.getSuccessCount(),
                    result.getFailedCount(),
                    result.getSuccessRate());

            // 记录审计日志 - 批量导入结果
            String auditDetails = String.format("导入 %d 台服务器，成功: %d, 失败: %d, 成功率: %.1f%%",
                    servers.size(), result.getSuccessCount(), result.getFailedCount(), result.getSuccessRate());
            logAudit("批量导入服务器", auditDetails, result.getSuccessCount() > 0, request);

            // 根据导入结果返回不同的HTTP状态码
            if (result.isAllSuccess()) {
                return ResponseEntity.ok(result);
            } else if (result.isPartialSuccess()) {
                return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

        } catch (Exception e) {
            logger.error("批量导入失败", e);
            logAudit("批量导入服务器", "异常: " + e.getMessage(), false, request);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("导入失败: " + e.getMessage()));
        }
    }

    /**
     * 获取默认SSH配置文件路径
     * Get default SSH config file path
     *
     * GET /api/ssh-config-import/default-path
     *
     * @return 默认路径和文件是否存在的信息
     *         - 200: 查询成功
     */
    @Operation(
        summary = "获取默认SSH配置路径",
        description = "查询系统默认的SSH配置文件路径（~/.ssh/config）并检查文件是否存在。"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "查询成功",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "服务器内部错误",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/default-path")
    public ResponseEntity<Map<String, Object>> getDefaultConfigPath() {

        logger.info("收到获取默认SSH配置路径请求");

        try {
            String defaultPath = sshConfigImportService.getDefaultConfigPath();
            boolean exists = sshConfigImportService.isDefaultConfigExists();

            Map<String, Object> response = new HashMap<>();
            response.put("path", defaultPath);
            response.put("exists", exists);
            response.put("message", exists ? "默认配置文件存在" : "默认配置文件不存在");

            logger.info("默认SSH配置路径: {}, 存在: {}", defaultPath, exists);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取默认配置路径失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", true,
                            "message", "获取默认路径失败: " + e.getMessage()
                    ));
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 记录审计日志
     *
     * @param operation 操作类型
     * @param details 操作详情
     * @param success 是否成功
     * @param request HTTP请求对象
     */
    private void logAudit(String operation, String details, boolean success, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
        String ipAddress = getClientIp(request);

        String status = success ? "SUCCESS" : "FAILED";

        logger.info("SSH配置导入审计日志 | 操作: {} | 用户: {} | IP: {} | 状态: {} | 详情: {}",
                   operation, username, ipAddress, status, details);
    }

    /**
     * 获取客户端IP地址
     *
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多级代理，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 解析配置文件内容
     * 由于SSHConfigImportService.parseLocalConfig()需要文件路径，
     * 这里需要一个辅助方法来解析配置内容
     *
     * @param configContent 配置文件内容
     * @return 解析结果
     */
    private SSHConfigParseResult parseConfigContent(String configContent) {
        // 直接使用SSHConfigParser解析内容
        // 注意：需要从Service中获取Parser实例
        // 或者在Service中添加一个parseConfigContent(String content)方法

        // 临时方案：写入临时文件再解析
        try {
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("ssh-config-", ".tmp");
            java.nio.file.Files.writeString(tempFile, configContent, StandardCharsets.UTF_8);

            SSHConfigParseResult result = sshConfigImportService.parseLocalConfig(tempFile.toString());

            // 删除临时文件
            java.nio.file.Files.deleteIfExists(tempFile);

            return result;

        } catch (IOException e) {
            logger.error("创建临时文件失败", e);
            return SSHConfigParseResult.error("处理上传文件失败: " + e.getMessage());
        }
    }

    /**
     * 创建错误响应
     *
     * @param message 错误消息
     * @return 错误响应Map
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
