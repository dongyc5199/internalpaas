package com.cmict.internalpaas.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户组同步结果数据传输对象
 * 记录用户组同步到Linux系统的详细结果
 */
public class UserGroupSyncResultDto {
    
    private Long userGroupId;
    private String groupName;
    private String serverName;
    private String serverHost;
    private boolean success;
    private String errorMessage;
    private LocalDateTime syncTime;
    
    // 详细的同步步骤结果
    private List<SyncStepResult> stepResults = new ArrayList<>();
    
    // 同步操作统计
    private int totalSteps;
    private int successfulSteps;
    private int failedSteps;
    
    public UserGroupSyncResultDto() {
        this.syncTime = LocalDateTime.now();
    }
    
    public UserGroupSyncResultDto(Long userGroupId, String groupName, String serverName, String serverHost) {
        this();
        this.userGroupId = userGroupId;
        this.groupName = groupName;
        this.serverName = serverName;
        this.serverHost = serverHost;
    }
    
    /**
     * 同步步骤结果
     */
    public static class SyncStepResult {
        private String stepName;
        private String command;
        private boolean success;
        private String output;
        private String error;
        private int exitCode;
        private long executionTimeMs;
        private LocalDateTime timestamp;
        
        public SyncStepResult(String stepName, String command) {
            this.stepName = stepName;
            this.command = command;
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters and Setters
        public String getStepName() { return stepName; }
        public void setStepName(String stepName) { this.stepName = stepName; }
        
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public int getExitCode() { return exitCode; }
        public void setExitCode(int exitCode) { this.exitCode = exitCode; }
        
        public long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
    
    // Helper methods
    public void addStepResult(SyncStepResult stepResult) {
        stepResults.add(stepResult);
        totalSteps++;
        if (stepResult.isSuccess()) {
            successfulSteps++;
        } else {
            failedSteps++;
        }
    }
    
    public void markAsSuccess() {
        this.success = true;
        this.errorMessage = null;
    }
    
    public void markAsFailure(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
    }
    
    public boolean hasErrors() {
        return failedSteps > 0 || !success;
    }
    
    public double getSuccessRate() {
        return totalSteps > 0 ? (double) successfulSteps / totalSteps : 0.0;
    }
    
    // Getters and Setters
    public Long getUserGroupId() { return userGroupId; }
    public void setUserGroupId(Long userGroupId) { this.userGroupId = userGroupId; }
    
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    
    public String getServerHost() { return serverHost; }
    public void setServerHost(String serverHost) { this.serverHost = serverHost; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getSyncTime() { return syncTime; }
    public void setSyncTime(LocalDateTime syncTime) { this.syncTime = syncTime; }
    
    public List<SyncStepResult> getStepResults() { return stepResults; }
    public void setStepResults(List<SyncStepResult> stepResults) { this.stepResults = stepResults; }
    
    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
    
    public int getSuccessfulSteps() { return successfulSteps; }
    public void setSuccessfulSteps(int successfulSteps) { this.successfulSteps = successfulSteps; }
    
    public int getFailedSteps() { return failedSteps; }
    public void setFailedSteps(int failedSteps) { this.failedSteps = failedSteps; }
    
    @Override
    public String toString() {
        return String.format("UserGroupSyncResult{groupName='%s', server='%s', success=%s, steps=%d/%d, successRate=%.1f%%}", 
                           groupName, serverName, success, successfulSteps, totalSteps, getSuccessRate() * 100);
    }
}