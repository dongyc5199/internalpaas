package com.cmict.internalpaas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

/**
 * SSH配置解析结果DTO
 * SSH Config Parse Result Data Transfer Object
 *
 * 用于返回SSH配置文件解析的结果，包含解析出的服务器列表、警告和错误信息
 *
 * @author GitHub Copilot
 * @since 2025-10-18
 */
@Schema(description = "SSH配置解析结果，包含解析出的服务器列表、警告和错误信息")
public class SSHConfigParseResult {

    /**
     * 解析到的Host总数
     */
    @Schema(description = "解析到的Host总数", example = "10")
    private int totalHosts;

    /**
     * 可导入的服务器列表
     */
    @Schema(description = "可导入的服务器列表")
    private List<ServerImportDto> servers;

    /**
     * 警告信息列表
     * 例如：通配符Host被跳过、ProxyJump暂不支持等
     */
    @Schema(description = "警告信息列表", example = "[\"跳过通配符Host: *.example.com\"]")
    private List<String> warnings;

    /**
     * 错误信息列表
     * 例如：解析失败、格式错误等
     */
    @Schema(description = "错误信息列表", example = "[\"配置文件不存在\"]")
    private List<String> errors;

    /**
     * 默认构造函数
     */
    public SSHConfigParseResult() {
        this.servers = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.totalHosts = 0;
    }

    /**
     * 构造函数
     * 
     * @param servers 服务器列表
     */
    public SSHConfigParseResult(List<ServerImportDto> servers) {
        this.servers = servers != null ? servers : new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.totalHosts = this.servers.size();
    }

    // ==================== Getters and Setters ====================

    public int getTotalHosts() {
        return totalHosts;
    }

    public void setTotalHosts(int totalHosts) {
        this.totalHosts = totalHosts;
    }

    public List<ServerImportDto> getServers() {
        return servers;
    }

    public void setServers(List<ServerImportDto> servers) {
        this.servers = servers;
        if (servers != null) {
            this.totalHosts = servers.size();
        }
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    // ==================== 工具方法 ====================

    /**
     * 添加服务器
     * 
     * @param server 服务器导入DTO
     */
    public void addServer(ServerImportDto server) {
        this.servers.add(server);
        this.totalHosts = this.servers.size();
    }

    /**
     * 添加警告信息
     * 
     * @param warning 警告信息
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * 添加错误信息
     * 
     * @param error 错误信息
     */
    public void addError(String error) {
        this.errors.add(error);
    }

    /**
     * 检查是否有错误
     * 
     * @return true表示存在错误
     */
    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }

    /**
     * 检查是否有警告
     * 
     * @return true表示存在警告
     */
    public boolean hasWarnings() {
        return !this.warnings.isEmpty();
    }

    /**
     * 检查解析是否成功
     * 
     * @return true表示解析成功（至少有一个服务器且无错误）
     */
    public boolean isSuccess() {
        return !hasErrors() && !servers.isEmpty();
    }

    /**
     * 获取有效服务器数量
     * 统计通过验证且未重复的服务器数量
     * 
     * @return 有效服务器数量
     */
    public int getValidServerCount() {
        return (int) servers.stream()
                .filter(server -> server.isValid() && !server.isDuplicate())
                .count();
    }

    /**
     * 获取重复服务器数量
     * 
     * @return 重复服务器数量
     */
    public int getDuplicateServerCount() {
        return (int) servers.stream()
                .filter(ServerImportDto::isDuplicate)
                .count();
    }

    /**
     * 获取无效服务器数量
     * 
     * @return 无效服务器数量（缺失必填字段）
     */
    public int getInvalidServerCount() {
        return (int) servers.stream()
                .filter(server -> !server.isValid() && !server.isDuplicate())
                .count();
    }

    /**
     * 创建错误结果
     * 
     * @param errorMessage 错误信息
     * @return 包含错误信息的解析结果
     */
    public static SSHConfigParseResult error(String errorMessage) {
        SSHConfigParseResult result = new SSHConfigParseResult();
        result.addError(errorMessage);
        return result;
    }

    /**
     * 创建空结果（无Host）
     * 
     * @param warningMessage 警告信息
     * @return 空的解析结果
     */
    public static SSHConfigParseResult empty(String warningMessage) {
        SSHConfigParseResult result = new SSHConfigParseResult();
        result.addWarning(warningMessage);
        return result;
    }

    /**
     * 获取解析摘要
     * 
     * @return 摘要字符串
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("解析完成: ");
        summary.append("总计 ").append(totalHosts).append(" 个Host, ");
        summary.append("有效 ").append(getValidServerCount()).append(" 个, ");
        summary.append("重复 ").append(getDuplicateServerCount()).append(" 个, ");
        summary.append("无效 ").append(getInvalidServerCount()).append(" 个");
        
        if (hasWarnings()) {
            summary.append(", ").append(warnings.size()).append(" 个警告");
        }
        
        if (hasErrors()) {
            summary.append(", ").append(errors.size()).append(" 个错误");
        }
        
        return summary.toString();
    }

    @Override
    public String toString() {
        return "SSHConfigParseResult{" +
                "totalHosts=" + totalHosts +
                ", serversCount=" + servers.size() +
                ", validCount=" + getValidServerCount() +
                ", duplicateCount=" + getDuplicateServerCount() +
                ", warningsCount=" + warnings.size() +
                ", errorsCount=" + errors.size() +
                '}';
    }
}
