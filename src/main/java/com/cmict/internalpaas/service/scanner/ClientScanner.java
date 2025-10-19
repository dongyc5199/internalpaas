package com.cmict.internalpaas.service.scanner;

import java.util.List;

/**
 * SSH客户端扫描器接口
 * 
 * 定义了检测SSH客户端、扫描配置文件的通用接口。
 * 每个具体的SSH客户端（SecureCRT、Xshell、Tabby等）都需要实现此接口。
 * 
 * 扫描流程：
 * 1. 检测客户端是否已安装 (isInstalled)
 * 2. 获取客户端版本信息 (getClientVersion)
 * 3. 扫描配置文件 (scanConfigurations)
 *    - Step 1: 从注册表读取配置路径
 *    - Step 2: 扫描默认配置路径
 *    - Step 3: 从安装目录推断配置位置
 *    - Step 4: 解析配置文件并验证
 * 
 * @author InternalPaaS Team
 * @since 2025-10-19 (Phase 1 - Multi-Client Scanner)
 */
public interface ClientScanner {
    
    /**
     * 获取客户端名称
     * 
     * @return 客户端名称 (e.g., "SecureCRT", "Xshell", "Tabby")
     */
    String getClientName();
    
    /**
     * 获取客户端版本号
     * 
     * @return 版本号字符串 (e.g., "9.0.1", "7.0.0088")，未安装时返回 null
     */
    String getClientVersion();
    
    /**
     * 检测客户端是否已安装
     * 
     * 检测方式：
     * - Windows: 检查注册表或特定安装目录
     * - macOS: 检查 /Applications/*.app
     * - Linux: 检查 ~/.local 或 /opt 目录
     * 
     * @return true 表示客户端已安装，false 表示未安装
     */
    boolean isInstalled();
    
    /**
     * 扫描并解析SSH配置文件
     * 
     * 扫描步骤：
     * 1. 尝试从注册表读取配置路径（Windows）
     * 2. 扫描默认配置路径
     * 3. 从安装目录推断配置位置
     * 4. 解析找到的配置文件
     * 5. 验证配置文件有效性
     * 
     * @return ScanResult 扫描结果，包含状态、路径、会话列表等
     */
    ScanResult scanConfigurations();
    
    /**
     * 获取默认扫描路径列表
     * 
     * 返回该客户端在不同操作系统下的默认配置文件路径，
     * 路径中可包含环境变量（如 %APPDATA%, %USERPROFILE%, ~/ 等）
     * 
     * @return 默认扫描路径列表
     */
    List<String> getDefaultScanPaths();
}
