package com.cmict.internalpaas.service.scanner;

/**
 * SSH客户端扫描状态枚举
 * 
 * @author InternalPaaS Team
 * @since 2025-10-19 (Phase 1 - Multi-Client Scanner)
 */
public enum ScanStatus {
    /**
     * 扫描成功 - 客户端已安装且配置文件已成功解析
     */
    SUCCESS,
    
    /**
     * 客户端未安装 - 未在系统中检测到该SSH客户端
     */
    NOT_INSTALLED,
    
    /**
     * 配置文件未找到 - 客户端已安装但未找到配置文件
     */
    CONFIG_NOT_FOUND,
    
    /**
     * 解析错误 - 配置文件存在但解析失败（格式错误、编码问题等）
     */
    PARSE_ERROR,
    
    /**
     * 权限不足 - 无权限访问配置文件或注册表
     */
    PERMISSION_DENIED
}
