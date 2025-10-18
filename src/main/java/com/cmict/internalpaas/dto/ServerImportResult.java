package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.Server;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务器导入结果DTO
 * Server Import Result Data Transfer Object
 * 
 * 用于返回批量导入服务器的执行结果，包含成功和失败的详情
 * 
 * @author GitHub Copilot
 * @since 2025-10-18
 */
public class ServerImportResult {
    
    /**
     * 成功导入的服务器数量
     */
    private int successCount;
    
    /**
     * 导入失败的服务器数量
     */
    private int failedCount;
    
    /**
     * 成功导入的服务器列表
     */
    private List<Server> successServers;
    
    /**
     * 失败详情列表
     */
    private List<ImportFailure> failures;

    /**
     * 默认构造函数
     */
    public ServerImportResult() {
        this.successCount = 0;
        this.failedCount = 0;
        this.successServers = new ArrayList<>();
        this.failures = new ArrayList<>();
    }

    // ==================== Getters and Setters ====================

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<Server> getSuccessServers() {
        return successServers;
    }

    public void setSuccessServers(List<Server> successServers) {
        this.successServers = successServers;
    }

    public List<ImportFailure> getFailures() {
        return failures;
    }

    public void setFailures(List<ImportFailure> failures) {
        this.failures = failures;
    }

    // ==================== 工具方法 ====================

    /**
     * 添加成功导入的服务器
     * 
     * @param server 成功导入的服务器实体
     */
    public void addSuccess(Server server) {
        this.successServers.add(server);
        this.successCount = this.successServers.size();
    }

    /**
     * 添加失败记录
     * 
     * @param serverName 服务器名称
     * @param reason 失败原因
     */
    public void addFailure(String serverName, String reason) {
        this.failures.add(new ImportFailure(serverName, reason));
        this.failedCount = this.failures.size();
    }

    /**
     * 添加失败记录（使用ImportFailure对象）
     * 
     * @param failure 失败记录
     */
    public void addFailure(ImportFailure failure) {
        this.failures.add(failure);
        this.failedCount = this.failures.size();
    }

    /**
     * 检查是否全部成功
     * 
     * @return true表示全部导入成功
     */
    public boolean isAllSuccess() {
        return failedCount == 0 && successCount > 0;
    }

    /**
     * 检查是否全部失败
     * 
     * @return true表示全部导入失败
     */
    public boolean isAllFailed() {
        return successCount == 0 && failedCount > 0;
    }

    /**
     * 检查是否部分成功
     * 
     * @return true表示部分成功、部分失败
     */
    public boolean isPartialSuccess() {
        return successCount > 0 && failedCount > 0;
    }

    /**
     * 获取总数
     * 
     * @return 成功数 + 失败数
     */
    public int getTotalCount() {
        return successCount + failedCount;
    }

    /**
     * 获取成功率
     * 
     * @return 成功率（百分比）
     */
    public double getSuccessRate() {
        int total = getTotalCount();
        if (total == 0) {
            return 0.0;
        }
        return (double) successCount / total * 100;
    }

    /**
     * 获取导入摘要
     * 
     * @return 摘要字符串
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("导入完成: ");
        summary.append("成功 ").append(successCount).append(" 台, ");
        summary.append("失败 ").append(failedCount).append(" 台");
        
        if (getTotalCount() > 0) {
            summary.append(" (成功率: ")
                   .append(String.format("%.1f", getSuccessRate()))
                   .append("%)");
        }
        
        return summary.toString();
    }

    @Override
    public String toString() {
        return "ServerImportResult{" +
                "successCount=" + successCount +
                ", failedCount=" + failedCount +
                ", successRate=" + String.format("%.1f%%", getSuccessRate()) +
                '}';
    }

    /**
     * 导入失败记录
     * 内部静态类，用于存储单个服务器的导入失败信息
     */
    public static class ImportFailure {
        
        /**
         * 服务器名称
         */
        private String serverName;
        
        /**
         * 失败原因
         */
        private String reason;

        /**
         * 构造函数
         * 
         * @param serverName 服务器名称
         * @param reason 失败原因
         */
        public ImportFailure(String serverName, String reason) {
            this.serverName = serverName;
            this.reason = reason;
        }

        /**
         * 默认构造函数
         */
        public ImportFailure() {
        }

        // ==================== Getters and Setters ====================

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        @Override
        public String toString() {
            return "ImportFailure{" +
                    "serverName='" + serverName + '\'' +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }
}
