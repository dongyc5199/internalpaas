package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.SSHHostConfig;
import com.cmict.internalpaas.dto.ServerImportDto;
import com.cmict.internalpaas.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * SSH配置映射器
 * SSH Config Mapper Service
 *
 * 负责将SSH配置(SSHHostConfig)映射为服务器导入DTO(ServerImportDto)
 * 并提供字段验证功能
 *
 * 映射规则:
 * - hostPattern → name (服务器名称)
 * - hostname → hostname (主机名/IP)
 * - port → sshPort (SSH端口，默认22)
 * - user → sshUsername (SSH用户名)
 * - identityFile → sshKeyPath (SSH私钥路径)
 *
 * 默认值:
 * - port: 8080 (应用端口)
 * - sshPort: 22 (SSH端口)
 * - serverType: DEVELOPMENT
 * - baseWorkDirectory: /home/{username} 或 /root
 *
 * @author GitHub Copilot
 * @since 2025-10-18
 */
@Service
public class SSHConfigMapper {

    private static final Logger logger = LoggerFactory.getLogger(SSHConfigMapper.class);

    /**
     * 将SSH配置映射为服务器导入DTO
     * Map SSH config to server import DTO
     *
     * 映射步骤:
     * 1. 复制SSH配置字段到DTO
     * 2. 设置默认值
     * 3. 生成默认工作目录和描述
     * 4. 验证字段完整性
     *
     * @param sshConfig SSH配置对象
     * @return 服务器导入DTO
     */
    public ServerImportDto mapToServer(SSHHostConfig sshConfig) {
        if (sshConfig == null) {
            logger.warn("SSH配置为null，无法映射");
            return null;
        }

        ServerImportDto dto = new ServerImportDto();

        // 映射基本字段
        dto.setName(sshConfig.getHostPattern());
        dto.setHostname(sshConfig.getHostname());
        dto.setSshPort(sshConfig.getPort() != null ? sshConfig.getPort() : 22);
        dto.setSshUsername(sshConfig.getUser());
        dto.setSshKeyPath(sshConfig.getIdentityFile());
        
        // 映射密码字段（如果有）
        // ⚠️ 注意：密码为明文，仅用于传输，最终会加密存储
        if (sshConfig.getPassword() != null && !sshConfig.getPassword().isEmpty()) {
            dto.setSshPassword(sshConfig.getPassword());
            logger.debug("映射SSH密码字段（明文传输）");
        }

        // 默认值已在ServerImportDto构造函数中设置
        // port = 8080
        // sshPort = 22 (如果SSH配置没有指定，这里再次确认)
        // serverType = DEVELOPMENT

        // 生成默认工作目录
        dto.generateDefaultWorkDirectory();

        // 生成默认描述
        dto.generateDefaultDescription(sshConfig.getHostPattern());

        // 验证字段并更新验证状态
        List<String> missingFields = getMissingFields(dto);
        dto.setMissingFields(missingFields);
        dto.setValid(missingFields.isEmpty());

        logger.debug("映射SSH配置: {} -> 服务器: {}, 有效: {}",
                    sshConfig.getHostPattern(),
                    dto.getName(),
                    dto.isValid());

        return dto;
    }

    /**
     * 批量映射SSH配置列表
     * Batch map SSH configs to server import DTOs
     *
     * @param sshConfigs SSH配置列表
     * @return 服务器导入DTO列表
     */
    public List<ServerImportDto> mapToServers(List<SSHHostConfig> sshConfigs) {
        List<ServerImportDto> dtos = new ArrayList<>();

        if (sshConfigs == null || sshConfigs.isEmpty()) {
            logger.warn("SSH配置列表为空");
            return dtos;
        }

        for (SSHHostConfig sshConfig : sshConfigs) {
            ServerImportDto dto = mapToServer(sshConfig);
            if (dto != null) {
                dtos.add(dto);
            }
        }

        logger.info("批量映射完成，共{}个SSH配置 -> {}个服务器DTO",
                   sshConfigs.size(), dtos.size());

        return dtos;
    }

    /**
     * 验证服务器导入DTO的必填字段
     * Validate required fields of server import DTO
     *
     * 必填字段:
     * - name: 服务器名称
     * - hostname: 主机名或IP地址
     * - sshUsername: SSH用户名
     * - sshPassword 或 sshKeyPath: SSH认证凭证（至少一个）
     *
     * @param dto 服务器导入DTO
     * @return true表示所有必填字段完整
     */
    public boolean isValid(ServerImportDto dto) {
        if (dto == null) {
            return false;
        }

        List<String> missingFields = getMissingFields(dto);
        return missingFields.isEmpty();
    }

    /**
     * 获取缺失的必填字段列表
     * Get list of missing required fields
     *
     * 检查以下必填字段:
     * - name: 服务器名称
     * - hostname: 主机名/IP
     * - sshUsername: SSH用户名
     * - sshPassword/sshKeyPath: SSH认证凭证（至少一个）
     *
     * @param dto 服务器导入DTO
     * @return 缺失字段名称列表
     */
    public List<String> getMissingFields(ServerImportDto dto) {
        List<String> missingFields = new ArrayList<>();

        if (dto == null) {
            missingFields.add("整个对象为null");
            return missingFields;
        }

        // 检查服务器名称
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            missingFields.add("name");
        }

        // 检查主机名
        if (dto.getHostname() == null || dto.getHostname().trim().isEmpty()) {
            missingFields.add("hostname");
        }

        // 检查SSH用户名
        if (dto.getSshUsername() == null || dto.getSshUsername().trim().isEmpty()) {
            missingFields.add("sshUsername");
        }

        // 检查SSH认证凭证（密码或私钥，至少一个）
        if (!dto.hasAuthCredentials()) {
            missingFields.add("sshPassword/sshKeyPath");
        }

        return missingFields;
    }

    /**
     * 批量验证服务器导入DTO列表
     * Batch validate server import DTOs
     *
     * 为每个DTO执行验证，并更新其验证状态
     *
     * @param dtos 服务器导入DTO列表
     * @return 有效的DTO数量
     */
    public int validateAll(List<ServerImportDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }

        int validCount = 0;

        for (ServerImportDto dto : dtos) {
            List<String> missingFields = getMissingFields(dto);
            dto.setMissingFields(missingFields);
            dto.setValid(missingFields.isEmpty());

            if (dto.isValid()) {
                validCount++;
            }
        }

        logger.info("批量验证完成，{}/{}个DTO有效", validCount, dtos.size());

        return validCount;
    }

    /**
     * 过滤有效的服务器导入DTO
     * Filter valid server import DTOs
     *
     * @param dtos 服务器导入DTO列表
     * @return 有效的DTO列表
     */
    public List<ServerImportDto> filterValid(List<ServerImportDto> dtos) {
        List<ServerImportDto> validDtos = new ArrayList<>();

        if (dtos == null || dtos.isEmpty()) {
            return validDtos;
        }

        for (ServerImportDto dto : dtos) {
            if (isValid(dto)) {
                validDtos.add(dto);
            }
        }

        logger.debug("过滤有效DTO，{}/{}个有效", validDtos.size(), dtos.size());

        return validDtos;
    }

    /**
     * 过滤无效的服务器导入DTO
     * Filter invalid server import DTOs
     *
     * @param dtos 服务器导入DTO列表
     * @return 无效的DTO列表
     */
    public List<ServerImportDto> filterInvalid(List<ServerImportDto> dtos) {
        List<ServerImportDto> invalidDtos = new ArrayList<>();

        if (dtos == null || dtos.isEmpty()) {
            return invalidDtos;
        }

        for (ServerImportDto dto : dtos) {
            if (!isValid(dto)) {
                invalidDtos.add(dto);
            }
        }

        logger.debug("过滤无效DTO，{}/{}个无效", invalidDtos.size(), dtos.size());

        return invalidDtos;
    }

    /**
     * 获取验证摘要信息
     * Get validation summary
     *
     * @param dtos 服务器导入DTO列表
     * @return 验证摘要字符串
     */
    public String getValidationSummary(List<ServerImportDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return "无服务器DTO";
        }

        int totalCount = dtos.size();
        int validCount = 0;
        int invalidCount = 0;
        int duplicateCount = 0;

        for (ServerImportDto dto : dtos) {
            if (dto.isDuplicate()) {
                duplicateCount++;
            } else if (dto.isValid()) {
                validCount++;
            } else {
                invalidCount++;
            }
        }

        return String.format("总计: %d, 有效: %d, 无效: %d, 重复: %d",
                           totalCount, validCount, invalidCount, duplicateCount);
    }
}
