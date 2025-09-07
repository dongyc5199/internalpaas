package com.cmict.internalpaas.dto;

import java.time.LocalDateTime;

/**
 * 日志搜索请求DTO
 * 包含搜索条件和过滤参数
 */
public class LogSearchRequest {
    
    private String keyword;
    private boolean regex = false;
    private boolean caseSensitive = false;
    private String logLevel;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int maxResults = 1000;
    private boolean includeContext = false;
    private int contextLines = 2;
    private String sortOrder = "desc"; // asc or desc
    
    // 构造函数
    public LogSearchRequest() {}
    
    public LogSearchRequest(String keyword) {
        this.keyword = keyword;
    }
    
    // Getters and Setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    
    public boolean isRegex() { return regex; }
    public void setRegex(boolean regex) { this.regex = regex; }
    
    public boolean isCaseSensitive() { return caseSensitive; }
    public void setCaseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; }
    
    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
    
    public boolean isIncludeContext() { return includeContext; }
    public void setIncludeContext(boolean includeContext) { this.includeContext = includeContext; }
    
    public int getContextLines() { return contextLines; }
    public void setContextLines(int contextLines) { this.contextLines = contextLines; }
    
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    
    /**
     * 验证搜索请求参数
     */
    public boolean isValid() {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        if (maxResults <= 0 || maxResults > 10000) {
            return false;
        }
        
        if (contextLines < 0 || contextLines > 50) {
            return false;
        }
        
        if (!"asc".equalsIgnoreCase(sortOrder) && !"desc".equalsIgnoreCase(sortOrder)) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return "LogSearchRequest{" +
                "keyword='" + keyword + '\'' +
                ", regex=" + regex +
                ", caseSensitive=" + caseSensitive +
                ", logLevel='" + logLevel + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", maxResults=" + maxResults +
                ", includeContext=" + includeContext +
                ", contextLines=" + contextLines +
                ", sortOrder='" + sortOrder + '\'' +
                '}';
    }
}