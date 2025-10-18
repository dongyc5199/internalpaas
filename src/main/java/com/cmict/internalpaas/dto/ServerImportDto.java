package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.Server;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务器导入DTO
 * Server Import Data Transfer Object
 *
 * 用于SSH配置导入功能，存储待导入的服务器信息
 * 包含从SSH配置映射的字段和需要用户补充的字段
 *
 * @author GitHub Copilot
 * @since 2025-10-18
 */
@Schema(description = "服务器导入DTO，用于SSH配置导入功能")
public class ServerImportDto {
    
    // ==================== 从SSH配置映射的字段 ====================

    /**
     * 服务器名称（来自SSH配置的Host）
     */
    @Schema(description = "服务器名称", example = "dev-server-1", required = true)
    private String name;

    /**
     * 主机名或IP地址（来自SSH配置的HostName）
     */
    @Schema(description = "主机名或IP地址", example = "192.168.1.100", required = true)
    private String hostname;

    /**
     * SSH端口（来自SSH配置的Port）
     */
    @Schema(description = "SSH端口", example = "22", defaultValue = "22")
    private Integer sshPort;

    /**
     * SSH用户名（来自SSH配置的User）
     */
    @Schema(description = "SSH用户名", example = "root", required = true)
    private String sshUsername;

    /**
     * SSH私钥路径（来自SSH配置的IdentityFile）
     */
    @Schema(description = "SSH私钥路径", example = "~/.ssh/id_rsa")
    private String sshKeyPath;

    // ==================== 需要用户补充的字段 ====================

    /**
     * SSH密码（需手动输入）
     * 注意：仅用于传输，不会明文存储
     */
    @Schema(description = "SSH密码（与私钥二选一）", example = "password123")
    private String sshPassword;

    /**
     * 应用端口（默认8080）
     */
    @Schema(description = "应用端口", example = "8080", defaultValue = "8080")
    private Integer port;

    /**
     * 工作目录（默认/home/{user}）
     */
    @Schema(description = "工作目录", example = "/home/user")
    private String baseWorkDirectory;

    /**
     * 描述信息
     */
    @Schema(description = "描述信息", example = "开发服务器")
    private String description;

    /**
     * 服务器类型（默认DEVELOPMENT）
     */
    @Schema(description = "服务器类型", example = "DEVELOPMENT", defaultValue = "DEVELOPMENT")
    private Server.ServerType serverType;

    // ==================== 验证相关字段 ====================

    /**
     * 是否通过验证
     */
    @Schema(description = "是否通过验证", example = "true")
    private boolean valid;

    /**
     * 缺失的字段列表
     */
    @Schema(description = "缺失的字段列表", example = "[\"sshPassword\"]")
    private List<String> missingFields;

    /**
     * 是否与现有服务器重复
     */
    @Schema(description = "是否与现有服务器重复", example = "false")
    private boolean duplicate;

    /**
     * 与哪个服务器重复（服务器名称）
     */
    @Schema(description = "与哪个服务器重复", example = "existing-server-1")
    private String duplicateWith;

    /**
     * 默认构造函数
     */
    public ServerImportDto() {
        this.missingFields = new ArrayList<>();
        this.valid = false;
        this.duplicate = false;
        // 设置默认值
        this.port = 8080;
        this.sshPort = 22;
        this.serverType = Server.ServerType.DEVELOPMENT;
    }

    // ==================== Getters and Setters ====================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public String getSshKeyPath() {
        return sshKeyPath;
    }

    public void setSshKeyPath(String sshKeyPath) {
        this.sshKeyPath = sshKeyPath;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getBaseWorkDirectory() {
        return baseWorkDirectory;
    }

    public void setBaseWorkDirectory(String baseWorkDirectory) {
        this.baseWorkDirectory = baseWorkDirectory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Server.ServerType getServerType() {
        return serverType;
    }

    public void setServerType(Server.ServerType serverType) {
        this.serverType = serverType;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public String getDuplicateWith() {
        return duplicateWith;
    }

    public void setDuplicateWith(String duplicateWith) {
        this.duplicateWith = duplicateWith;
    }

    // ==================== 工具方法 ====================

    /**
     * 添加缺失字段
     * 
     * @param fieldName 字段名称
     */
    public void addMissingField(String fieldName) {
        if (!this.missingFields.contains(fieldName)) {
            this.missingFields.add(fieldName);
        }
    }

    /**
     * 检查是否有SSH认证凭证（密码或私钥）
     * 
     * @return true表示已配置密码或私钥
     */
    public boolean hasAuthCredentials() {
        return (sshPassword != null && !sshPassword.isEmpty()) ||
               (sshKeyPath != null && !sshKeyPath.isEmpty());
    }

    /**
     * 检查必填字段是否完整
     * 
     * @return true表示必填字段完整
     */
    public boolean hasRequiredFields() {
        return name != null && !name.isEmpty() &&
               hostname != null && !hostname.isEmpty() &&
               sshUsername != null && !sshUsername.isEmpty() &&
               hasAuthCredentials();
    }

    /**
     * 生成默认工作目录
     * 基于SSH用户名生成默认路径
     */
    public void generateDefaultWorkDirectory() {
        if (baseWorkDirectory == null || baseWorkDirectory.isEmpty()) {
            if (sshUsername != null && !sshUsername.isEmpty()) {
                if ("root".equals(sshUsername)) {
                    this.baseWorkDirectory = "/root";
                } else {
                    this.baseWorkDirectory = "/home/" + sshUsername;
                }
            }
        }
    }

    /**
     * 生成默认描述
     * 基于SSH配置来源生成描述
     * 
     * @param sourceHostPattern SSH配置中的Host名称
     */
    public void generateDefaultDescription(String sourceHostPattern) {
        if (description == null || description.isEmpty()) {
            this.description = "从SSH配置导入: " + sourceHostPattern;
        }
    }

    /**
     * 标记为重复
     * 
     * @param existingServerName 已存在的服务器名称
     */
    public void markAsDuplicate(String existingServerName) {
        this.duplicate = true;
        this.duplicateWith = existingServerName;
        this.valid = false;
    }

    /**
     * 获取验证状态描述
     * 
     * @return 状态描述字符串
     */
    public String getValidationStatus() {
        if (duplicate) {
            return "重复";
        } else if (!valid) {
            return "缺失字段";
        } else {
            return "就绪";
        }
    }

    @Override
    public String toString() {
        return "ServerImportDto{" +
                "name='" + name + '\'' +
                ", hostname='" + hostname + '\'' +
                ", sshPort=" + sshPort +
                ", sshUsername='" + sshUsername + '\'' +
                ", sshKeyPath='" + sshKeyPath + '\'' +
                ", port=" + port +
                ", baseWorkDirectory='" + baseWorkDirectory + '\'' +
                ", serverType=" + serverType +
                ", valid=" + valid +
                ", duplicate=" + duplicate +
                ", missingFieldsCount=" + missingFields.size() +
                '}';
    }
}
