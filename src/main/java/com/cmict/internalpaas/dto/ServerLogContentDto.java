package com.cmict.internalpaas.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务器日志内容DTO
 * 包含日志内容和相关的元信息
 */
public class ServerLogContentDto {
    
    private String filePath;
    private String fileName;
    private String content;  // Raw content for frontend compatibility
    private List<LogLineDto> lines;
    private int totalLines;
    private int displayedLines;
    private long startLine;
    private long endLine;
    private LocalDateTime retrieveTime;
    private boolean hasMore;
    private String nextPageToken;
    
    // 构造函数
    public ServerLogContentDto() {
        this.retrieveTime = LocalDateTime.now();
    }
    
    public ServerLogContentDto(String filePath, String fileName) {
        this();
        this.filePath = filePath;
        this.fileName = fileName;
    }
    
    // Getters and Setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public List<LogLineDto> getLines() { return lines; }
    public void setLines(List<LogLineDto> lines) { this.lines = lines; }
    
    public int getTotalLines() { return totalLines; }
    public void setTotalLines(int totalLines) { this.totalLines = totalLines; }
    
    public int getDisplayedLines() { return displayedLines; }
    public void setDisplayedLines(int displayedLines) { this.displayedLines = displayedLines; }
    
    public long getStartLine() { return startLine; }
    public void setStartLine(long startLine) { this.startLine = startLine; }
    
    public long getEndLine() { return endLine; }
    public void setEndLine(long endLine) { this.endLine = endLine; }
    
    public LocalDateTime getRetrieveTime() { return retrieveTime; }
    public void setRetrieveTime(LocalDateTime retrieveTime) { this.retrieveTime = retrieveTime; }
    
    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
    
    public String getNextPageToken() { return nextPageToken; }
    public void setNextPageToken(String nextPageToken) { this.nextPageToken = nextPageToken; }
    
    /**
     * 单个日志行DTO
     */
    public static class LogLineDto {
        private long lineNumber;
        private String content;
        private String level;
        private LocalDateTime timestamp;
        private String source;
        private boolean highlighted;
        
        public LogLineDto() {}
        
        public LogLineDto(long lineNumber, String content) {
            this.lineNumber = lineNumber;
            this.content = content;
        }
        
        public LogLineDto(long lineNumber, String content, String level, LocalDateTime timestamp) {
            this(lineNumber, content);
            this.level = level;
            this.timestamp = timestamp;
        }
        
        // Getters and Setters
        public long getLineNumber() { return lineNumber; }
        public void setLineNumber(long lineNumber) { this.lineNumber = lineNumber; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        
        public boolean isHighlighted() { return highlighted; }
        public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }
    }
}