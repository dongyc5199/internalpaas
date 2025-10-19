package com.cmict.internalpaas.service.parser;

import com.cmict.internalpaas.dto.SSHHostConfig;

import java.util.List;

/**
 * SSH配置文件解析器接口
 * 
 * 定义了解析不同格式SSH配置文件的通用接口。
 * 支持的格式：
 * - .ini (SecureCRT)
 * - .xsh (Xshell XML)
 * - .yaml/.yml (Tabby)
 * - .config (OpenSSH standard)
 * 
 * @param <T> 解析源的类型 (e.g., File, String, InputStream)
 * 
 * @author InternalPaaS Team
 * @since 2025-10-19 (Phase 1 - Multi-Client Scanner)
 */
public interface ConfigParser<T> {
    
    /**
     * 解析配置源并提取SSH主机配置
     * 
     * @param source 配置源 (文件、字符串或输入流)
     * @return SSH主机配置列表
     * @throws ParseException 解析失败时抛出
     */
    List<SSHHostConfig> parse(T source) throws ParseException;
    
    /**
     * 检查是否支持指定的文件扩展名
     * 
     * @param fileExtension 文件扩展名 (e.g., ".ini", ".xsh", ".yaml")
     * @return true 表示支持该格式，false 表示不支持
     */
    boolean supports(String fileExtension);
    
    /**
     * 获取解析器名称
     * 
     * @return 解析器名称 (e.g., "SecureCRT INI Parser", "Xshell XML Parser")
     */
    String getParserName();
    
    /**
     * 配置文件解析异常
     */
    class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
        
        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
