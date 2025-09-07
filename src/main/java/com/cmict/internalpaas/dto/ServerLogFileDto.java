package com.cmict.internalpaas.dto;

import java.time.LocalDateTime;

/**
 * 服务器日志文件DTO
 * 包含日志文件的基本信息和元数据
 */
public class ServerLogFileDto {
    
    private String path;
    private String name;
    private String type;
    private String description;
    private Long size;
    private LocalDateTime lastModified;
    private boolean readable;
    private String permission;
    private int estimatedLines;
    
    // 构造函数
    public ServerLogFileDto() {}
    
    public ServerLogFileDto(String path, String name, String type, String description) {
        this.path = path;
        this.name = name;
        this.type = type;
        this.description = description;
        this.readable = true;
    }
    
    // Getters and Setters
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    
    public boolean isReadable() { return readable; }
    public void setReadable(boolean readable) { this.readable = readable; }
    
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    
    public int getEstimatedLines() { return estimatedLines; }
    public void setEstimatedLines(int estimatedLines) { this.estimatedLines = estimatedLines; }
    
    // 辅助方法
    public String getFormattedSize() {
        if (size == null) return "未知";
        
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
}