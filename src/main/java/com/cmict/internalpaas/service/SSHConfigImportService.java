package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.SSHConfigParseResult;
import com.cmict.internalpaas.dto.SSHHostConfig;
import com.cmict.internalpaas.dto.ServerImportDto;
import com.cmict.internalpaas.dto.ServerImportResult;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.repository.ServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * SSH配置导入服务
 * SSH Config Import Service
 *
 * 提供SSH配置文件导入功能，支持:
 * - 解析本地SSH配置文件
 * - 批量导入服务器
 * - 去重检查
 * - 异步连接测试
 *
 * 工作流程:
 * 1. parseLocalConfig() - 读取并解析SSH配置文件
 * 2. previewImport() - 预览导入，执行去重检查
 * 3. batchImport() - 批量导入有效的服务器
 * 4. triggerConnectionTest() - 异步触发连接测试
 *
 * @author GitHub Copilot
 * @since 2025-10-18
 */
@Service
public class SSHConfigImportService {

    private static final Logger logger = LoggerFactory.getLogger(SSHConfigImportService.class);

    /**
     * 默认SSH配置文件路径
     */
    private static final String DEFAULT_SSH_CONFIG_PATH = System.getProperty("user.home") + "/.ssh/config";

    @Autowired
    private SSHConfigParser sshConfigParser;

    @Autowired
    private SSHConfigMapper sshConfigMapper;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private ServerService serverService;

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

    /**
     * 解析本地SSH配置文件
     * Parse local SSH config file
     *
     * 读取指定路径的SSH配置文件，解析为服务器导入DTO列表
     *
     * @param path SSH配置文件路径，如果为null则使用默认路径 ~/.ssh/config
     * @return SSH配置解析结果
     */
    public SSHConfigParseResult parseLocalConfig(String path) {
        // 确定配置文件路径
        String configPath = (path != null && !path.trim().isEmpty()) ? path : DEFAULT_SSH_CONFIG_PATH;

        logger.info("开始解析SSH配置文件: {}", configPath);

        try {
            // 检查文件是否存在
            Path filePath = Paths.get(configPath);
            if (!Files.exists(filePath)) {
                String errorMsg = "SSH配置文件不存在: " + configPath;
                logger.warn(errorMsg);
                return SSHConfigParseResult.error(errorMsg);
            }

            // 读取文件内容
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            logger.debug("成功读取SSH配置文件，大小: {} 字节", content.length());

            // 解析SSH配置
            List<SSHHostConfig> sshHosts = sshConfigParser.parseConfig(content);

            if (sshHosts.isEmpty()) {
                String warningMsg = "配置文件中未找到有效的Host配置";
                logger.warn(warningMsg);
                SSHConfigParseResult result = new SSHConfigParseResult();
                result.setTotalHosts(0);
                result.setServers(new ArrayList<>());
                result.addWarning(warningMsg);
                return result;
            }

            // 过滤掉应跳过的Host（通配符等）
            List<SSHHostConfig> validHosts = sshHosts.stream()
                    .filter(host -> !sshConfigParser.shouldSkipHost(host))
                    .collect(Collectors.toList());

            logger.info("解析到 {} 个Host配置，其中 {} 个有效",
                       sshHosts.size(), validHosts.size());

            // 映射为服务器导入DTO
            List<ServerImportDto> servers = sshConfigMapper.mapToServers(validHosts);

            // 收集警告信息
            List<String> warnings = new ArrayList<>();
            for (SSHHostConfig host : validHosts) {
                String warningMsg = sshConfigParser.getWarningMessage(host);
                if (warningMsg != null) {
                    warnings.add(host.getHostPattern() + ": " + warningMsg);
                }
            }

            // 构建解析结果
            SSHConfigParseResult result = new SSHConfigParseResult();
            result.setTotalHosts(sshHosts.size());
            result.setServers(servers);
            result.setWarnings(warnings);

            logger.info("SSH配置解析完成，生成 {} 个服务器导入DTO", servers.size());

            return result;

        } catch (IOException e) {
            String errorMsg = "读取SSH配置文件失败: " + e.getMessage();
            logger.error(errorMsg, e);
            return SSHConfigParseResult.error(errorMsg);
        } catch (Exception e) {
            String errorMsg = "解析SSH配置文件时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            return SSHConfigParseResult.error(errorMsg);
        }
    }

    /**
     * 预览导入 - 执行去重检查
     * Preview import with duplicate check
     *
     * 检查服务器列表中的每一台服务器是否与现有服务器重复
     * 不执行实际导入，仅标记重复项
     *
     * @param servers 待导入的服务器列表
     * @return 更新后的服务器列表（包含重复标记）
     */
    public List<ServerImportDto> previewImport(List<ServerImportDto> servers) {
        if (servers == null || servers.isEmpty()) {
            logger.warn("预览导入: 服务器列表为空");
            return new ArrayList<>();
        }

        logger.info("开始预览导入，检查 {} 台服务器", servers.size());

        int duplicateCount = 0;

        for (ServerImportDto dto : servers) {
            // 验证必填字段
            List<String> missingFields = sshConfigMapper.getMissingFields(dto);
            dto.setMissingFields(missingFields);
            dto.setValid(missingFields.isEmpty());

            // 去重检查（基于hostname和sshPort）
            if (dto.getHostname() != null && dto.getSshPort() != null) {
                Optional<Server> existingServer = serverRepository.findByHostnameAndSshPort(
                        dto.getHostname(), dto.getSshPort());

                if (existingServer.isPresent()) {
                    dto.markAsDuplicate(existingServer.get().getName());
                    duplicateCount++;
                    logger.debug("发现重复服务器: {} ({}:{})",
                               dto.getName(), dto.getHostname(), dto.getSshPort());
                }
            }
        }

        logger.info("预览导入完成，发现 {} 台重复服务器", duplicateCount);

        return servers;
    }

    /**
     * 批量导入服务器
     * Batch import servers
     *
     * 遍历服务器列表，验证、去重、创建服务器，并记录成功和失败结果
     *
     * @param servers 待导入的服务器列表
     * @return 导入结果（成功数、失败数、失败详情）
     */
    @Transactional
    public ServerImportResult batchImport(List<ServerImportDto> servers) {
        if (servers == null || servers.isEmpty()) {
            logger.warn("批量导入: 服务器列表为空");
            return new ServerImportResult();
        }

        logger.info("开始批量导入 {} 台服务器", servers.size());

        ServerImportResult result = new ServerImportResult();

        for (ServerImportDto dto : servers) {
            try {
                // 1. 验证必填字段
                List<String> validationErrors = validate(dto);
                if (!validationErrors.isEmpty()) {
                    String errorMsg = "验证失败: " + String.join(", ", validationErrors);
                    result.addFailure(dto.getName(), errorMsg);
                    logger.warn("服务器 {} 验证失败: {}", dto.getName(), errorMsg);
                    continue;
                }

                // 2. 去重检查
                if (serverRepository.existsByHostnameAndSshPort(dto.getHostname(), dto.getSshPort())) {
                    result.addFailure(dto.getName(), "服务器已存在（主机名和端口重复）");
                    logger.warn("服务器 {} 已存在: {}:{}", dto.getName(), dto.getHostname(), dto.getSshPort());
                    continue;
                }

                // 3. 转换为Server实体
                Server server = convertToServer(dto);

                // 4. 保存服务器
                Server savedServer = serverService.saveServer(server);
                result.addSuccess(savedServer);

                logger.info("成功导入服务器: {} (ID: {})", savedServer.getName(), savedServer.getId());

                // 5. 异步触发连接测试
                triggerConnectionTest(savedServer.getId());

            } catch (Exception e) {
                String errorMsg = "导入失败: " + e.getMessage();
                result.addFailure(dto.getName(), errorMsg);
                logger.error("导入服务器 {} 失败", dto.getName(), e);
            }
        }

        logger.info("批量导入完成，成功: {}, 失败: {}",
                   result.getSuccessCount(), result.getFailedCount());

        return result;
    }

    /**
     * 验证服务器导入DTO
     * Validate server import DTO
     *
     * 检查必填字段完整性，返回错误列表
     *
     * @param dto 服务器导入DTO
     * @return 验证错误列表（空列表表示验证通过）
     */
    public List<String> validate(ServerImportDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto == null) {
            errors.add("服务器对象为null");
            return errors;
        }

        // 检查名称
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            errors.add("缺少服务器名称");
        }

        // 检查主机名
        if (dto.getHostname() == null || dto.getHostname().trim().isEmpty()) {
            errors.add("缺少主机名");
        }

        // 检查SSH用户名
        if (dto.getSshUsername() == null || dto.getSshUsername().trim().isEmpty()) {
            errors.add("缺少SSH用户名");
        }

        // 检查认证凭证（密码或私钥）
        if (!dto.hasAuthCredentials()) {
            errors.add("缺少SSH认证凭证（密码或私钥）");
        }

        return errors;
    }

    /**
     * 将ServerImportDto转换为Server实体
     * Convert ServerImportDto to Server entity
     *
     * 映射所有字段，并设置默认值
     *
     * @param dto 服务器导入DTO
     * @return Server实体
     */
    public Server convertToServer(ServerImportDto dto) {
        Server server = new Server();

        // 基本信息
        server.setName(dto.getName());
        server.setHostname(dto.getHostname());
        server.setPort(dto.getPort() != null ? dto.getPort() : 8080);
        server.setDescription(dto.getDescription());
        server.setBaseWorkDirectory(dto.getBaseWorkDirectory());

        // SSH配置
        server.setSshPort(dto.getSshPort() != null ? dto.getSshPort() : 22);
        server.setSshUsername(dto.getSshUsername());

        // 注入密码加密服务
        server.setPasswordEncryptionService(passwordEncryptionService);

        // 设置SSH密码（如果有）
        if (dto.getSshPassword() != null && !dto.getSshPassword().isEmpty()) {
            server.setSshPassword(dto.getSshPassword()); // 会自动加密
            logger.debug("服务器 {} 设置SSH密码（已加密）", server.getName());
        }

        // 设置SSH私钥路径（如果有）
        if (dto.getSshKeyPath() != null && !dto.getSshKeyPath().isEmpty()) {
            server.setSshKeyPath(dto.getSshKeyPath());
            logger.debug("服务器 {} 设置SSH私钥路径: {}", server.getName(), dto.getSshKeyPath());
        }

        // 服务器类型
        if (dto.getServerType() != null) {
            server.setServerType(dto.getServerType());
        } else {
            server.setServerType(Server.ServerType.DEVELOPMENT);
        }

        // 默认设置
        server.setActive(true);
        server.setConnectionStatus(Server.ConnectionStatus.UNKNOWN);
        server.setAutoMonitorEnabled(true);
        server.setMonitorIntervalSeconds(60);

        logger.debug("转换DTO为Server实体: {}", server.getName());

        return server;
    }

    /**
     * 异步触发服务器连接测试
     * Trigger async connection test
     *
     * 使用CompletableFuture异步执行连接测试，不阻塞主流程
     *
     * @param serverId 服务器ID
     * @return CompletableFuture
     */
    public CompletableFuture<Void> triggerConnectionTest(Long serverId) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("开始异步测试服务器连接，ID: {}", serverId);
                serverService.checkServerConnectionAndMetrics(serverId);
                logger.info("服务器连接测试完成，ID: {}", serverId);
            } catch (Exception e) {
                logger.error("服务器连接测试失败，ID: {}", serverId, e);
            }
        });
    }

    /**
     * 获取默认SSH配置文件路径
     * Get default SSH config path
     *
     * @return 默认路径和文件是否存在的信息
     */
    public String getDefaultConfigPath() {
        return DEFAULT_SSH_CONFIG_PATH;
    }

    /**
     * 检查默认SSH配置文件是否存在
     * Check if default SSH config file exists
     *
     * @return true表示文件存在
     */
    public boolean isDefaultConfigExists() {
        return Files.exists(Paths.get(DEFAULT_SSH_CONFIG_PATH));
    }

    /**
     * 批量验证服务器列表
     * Batch validate server list
     *
     * @param servers 服务器列表
     * @return 有效的服务器列表
     */
    public List<ServerImportDto> validateAndFilter(List<ServerImportDto> servers) {
        if (servers == null || servers.isEmpty()) {
            return new ArrayList<>();
        }

        List<ServerImportDto> validServers = new ArrayList<>();

        for (ServerImportDto dto : servers) {
            List<String> errors = validate(dto);
            if (errors.isEmpty()) {
                dto.setValid(true);
                validServers.add(dto);
            } else {
                dto.setValid(false);
                dto.setMissingFields(errors);
                logger.debug("服务器 {} 验证失败: {}", dto.getName(), errors);
            }
        }

        logger.info("批量验证完成，{}/{} 台服务器有效",
                   validServers.size(), servers.size());

        return validServers;
    }
}
