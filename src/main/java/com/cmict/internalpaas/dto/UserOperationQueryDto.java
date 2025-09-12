package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.UserActivity;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户操作查询条件DTO
 */
public class UserOperationQueryDto {
    
    private Long serverId;
    private String username;
    private String sessionId;
    private UserActivity.ActivityType activityType;
    private Boolean isActive;
    private String remoteIp;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String terminalType;
    private Integer minCommandCount;
    private Integer maxCommandCount;
    private List<String> riskLevels;
    private String keyword; // 在活动详情中搜索关键词
    
    // 分页参数
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
    
    // 导出参数
    private String exportFormat; // csv, excel, json
    private List<String> exportFields; // 指定导出字段
    
    public UserOperationQueryDto() {}
    
    // Getters and Setters
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public UserActivity.ActivityType getActivityType() { return activityType; }
    public void setActivityType(UserActivity.ActivityType activityType) { this.activityType = activityType; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getRemoteIp() { return remoteIp; }
    public void setRemoteIp(String remoteIp) { this.remoteIp = remoteIp; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public String getTerminalType() { return terminalType; }
    public void setTerminalType(String terminalType) { this.terminalType = terminalType; }
    
    public Integer getMinCommandCount() { return minCommandCount; }
    public void setMinCommandCount(Integer minCommandCount) { this.minCommandCount = minCommandCount; }
    
    public Integer getMaxCommandCount() { return maxCommandCount; }
    public void setMaxCommandCount(Integer maxCommandCount) { this.maxCommandCount = maxCommandCount; }
    
    public List<String> getRiskLevels() { return riskLevels; }
    public void setRiskLevels(List<String> riskLevels) { this.riskLevels = riskLevels; }
    
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    
    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
    
    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }
    
    public List<String> getExportFields() { return exportFields; }
    public void setExportFields(List<String> exportFields) { this.exportFields = exportFields; }
}