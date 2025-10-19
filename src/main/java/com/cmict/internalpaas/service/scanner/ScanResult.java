package com.cmict.internalpaas.service.scanner;

import com.cmict.internalpaas.dto.SSHHostConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * SSH客户端扫描结果
 * 
 * 包含单个SSH客户端的扫描状态、配置路径、解析的会话列表等信息
 * 
 * @author InternalPaaS Team
 * @since 2025-10-19 (Phase 1 - Multi-Client Scanner)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanResult {
    
    /**
     * 客户端名称 (e.g., "SecureCRT", "Xshell", "Tabby")
     */
    private String clientName;
    
    /**
     * 客户端版本号 (e.g., "9.0.1", "7.0.0088")
     */
    private String clientVersion;
    
    /**
     * 扫描状态
     */
    private ScanStatus status;
    
    /**
     * 配置文件路径 (扫描成功时填充)
     */
    private String configPath;
    
    /**
     * 检测到的会话数量
     */
    @Builder.Default
    private int sessionCount = 0;
    
    /**
     * 解析的SSH会话配置列表
     */
    @Builder.Default
    private List<SSHHostConfig> sessions = new ArrayList<>();
    
    /**
     * 警告信息列表 (e.g., "5 sessions have encrypted passwords", "3 sessions missing port")
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    /**
     * 错误消息 (扫描失败时填充)
     */
    private String errorMessage;
    
    /**
     * 添加警告信息
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }
    
    /**
     * 添加会话配置
     */
    public void addSession(SSHHostConfig session) {
        if (this.sessions == null) {
            this.sessions = new ArrayList<>();
        }
        this.sessions.add(session);
        this.sessionCount = this.sessions.size();
    }
}
