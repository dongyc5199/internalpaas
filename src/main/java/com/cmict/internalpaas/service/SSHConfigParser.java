package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.SSHHostConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * SSH配置解析器
 * SSH Config Parser Service
 * 
 * 负责解析OpenSSH配置文件格式，提取Host块及其配置项
 * 
 * 支持的配置项:
 * - Host: Host名称（可能包含通配符）
 * - HostName: 实际主机名或IP
 * - Port: SSH端口（默认22）
 * - User: SSH用户名
 * - IdentityFile: 私钥文件路径（支持~和$HOME展开）
 * - ProxyJump: 跳板机配置
 * - 其他标准SSH配置项（存储在extraOptions中）
 * 
 * 特殊处理:
 * - 跳过通配符Host（包含*或?的）
 * - 记录ProxyJump警告（暂不支持跳板机导入）
 * - 处理Include指令（记录警告）
 * - 自动展开路径中的~/和$HOME/
 * 
 * @author GitHub Copilot
 * @since 2025-10-18
 */
@Service
public class SSHConfigParser {
    
    private static final Logger logger = LoggerFactory.getLogger(SSHConfigParser.class);
    
    /**
     * 解析SSH配置文件内容
     * Parse SSH config file content
     * 
     * 按行读取配置内容，识别Host块，提取配置项
     * 
     * @param configContent SSH配置文件内容
     * @return 解析出的SSHHostConfig列表
     */
    public List<SSHHostConfig> parseConfig(String configContent) {
        List<SSHHostConfig> hosts = new ArrayList<>();
        
        if (configContent == null || configContent.trim().isEmpty()) {
            logger.warn("SSH配置内容为空");
            return hosts;
        }
        
        SSHHostConfig currentHost = null;
        int lineNumber = 0;
        
        try (BufferedReader reader = new BufferedReader(new StringReader(configContent))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // 跳过空行和注释
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // 检测Host指令
                if (line.startsWith("Host ") || line.startsWith("host ")) {
                    // 保存前一个Host
                    if (currentHost != null) {
                        hosts.add(currentHost);
                        logger.debug("解析完成Host: {}", currentHost.getHostPattern());
                    }
                    
                    // 开始新的Host块
                    String hostPattern = extractValue(line, "Host");
                    if (hostPattern != null && !hostPattern.isEmpty()) {
                        currentHost = new SSHHostConfig(hostPattern);
                        logger.debug("开始解析Host块: {}", hostPattern);
                    } else {
                        logger.warn("第{}行: Host指令缺少Host名称", lineNumber);
                        currentHost = null;
                    }
                }
                // 解析Host块内的配置项
                else if (currentHost != null) {
                    parseHostBlockLine(currentHost, line, lineNumber);
                }
                // Host指令之前的全局配置项（忽略）
                else {
                    logger.trace("第{}行: 跳过全局配置项: {}", lineNumber, line);
                }
            }
            
            // 添加最后一个Host
            if (currentHost != null) {
                hosts.add(currentHost);
                logger.debug("解析完成Host: {}", currentHost.getHostPattern());
            }
            
            logger.info("SSH配置解析完成，共解析{}个Host块", hosts.size());
            
        } catch (Exception e) {
            logger.error("解析SSH配置时发生错误: {}", e.getMessage(), e);
        }
        
        return hosts;
    }
    
    /**
     * 解析Host块中的单行配置
     * Parse a single configuration line within a Host block
     * 
     * @param host 当前Host配置对象
     * @param line 配置行内容
     * @param lineNumber 行号（用于错误提示）
     */
    private void parseHostBlockLine(SSHHostConfig host, String line, int lineNumber) {
        // 分割配置项和值（最多分割为2部分）
        String[] parts = line.split("\\s+", 2);
        if (parts.length < 2) {
            logger.warn("第{}行: 配置项格式错误，缺少值: {}", lineNumber, line);
            return;
        }
        
        String key = parts[0];
        String value = parts[1].trim();
        
        // 去除值两端的引号（如果有）
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        
        // 解析已知配置项
        switch (key) {
            case "HostName":
            case "hostname":
                host.setHostname(value);
                logger.trace("设置HostName: {}", value);
                break;
                
            case "Port":
            case "port":
                try {
                    host.setPort(Integer.parseInt(value));
                    logger.trace("设置Port: {}", value);
                } catch (NumberFormatException e) {
                    logger.warn("第{}行: Port值无效: {}", lineNumber, value);
                }
                break;
                
            case "User":
            case "user":
                host.setUser(value);
                logger.trace("设置User: {}", value);
                break;
                
            case "IdentityFile":
            case "identityfile":
                String expandedPath = expandPath(value);
                host.setIdentityFile(expandedPath);
                logger.trace("设置IdentityFile: {} -> {}", value, expandedPath);
                break;
                
            case "ProxyJump":
            case "ProxyCommand":
            case "proxyjump":
            case "proxycommand":
                host.setProxyJump(value);
                logger.trace("设置ProxyJump: {}", value);
                break;
                
            case "Include":
            case "include":
                // Include指令暂不支持，记录为额外配置
                host.addExtraOption(key, value);
                logger.debug("检测到Include指令: {}（暂不支持）", value);
                break;
                
            default:
                // 其他配置项存储到extraOptions
                host.addExtraOption(key, value);
                logger.trace("存储额外配置项: {} = {}", key, value);
                break;
        }
    }
    
    /**
     * 展开路径中的特殊符号
     * Expand special symbols in file paths
     * 
     * 支持的展开:
     * - ~/path -> /home/user/path
     * - $HOME/path -> /home/user/path
     * 
     * @param path 原始路径
     * @return 展开后的路径
     */
    public String expandPath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        
        String userHome = System.getProperty("user.home");
        
        // 展开 ~/
        if (path.startsWith("~/")) {
            String expanded = userHome + path.substring(1);
            logger.trace("路径展开: {} -> {}", path, expanded);
            return expanded;
        }
        
        // 展开 $HOME/
        if (path.startsWith("$HOME/")) {
            String expanded = userHome + path.substring(5);
            logger.trace("路径展开: {} -> {}", path, expanded);
            return expanded;
        }
        
        // 展开 %h（Host替换，这里保持原样，稍后处理）
        // 展开 %r（RemoteUser替换，这里保持原样，稍后处理）
        
        return path;
    }
    
    /**
     * 从配置行中提取值
     * Extract value from configuration line
     * 
     * 例如: "Host dev-server" -> "dev-server"
     * 
     * @param line 配置行
     * @param keyword 关键字（如"Host"）
     * @return 提取的值
     */
    private String extractValue(String line, String keyword) {
        if (line.startsWith(keyword + " ")) {
            return line.substring(keyword.length() + 1).trim();
        } else if (line.toLowerCase().startsWith(keyword.toLowerCase() + " ")) {
            return line.substring(keyword.length() + 1).trim();
        }
        return null;
    }
    
    /**
     * 检查Host是否为通配符Host
     * Check if a host pattern contains wildcards
     * 
     * 通配符Host包含 * 或 ? 字符
     * 例如: "prod-*", "*.example.com", "server-?"
     * 
     * @param hostPattern Host名称模式
     * @return true表示包含通配符
     */
    public boolean isWildcardHost(String hostPattern) {
        return hostPattern != null && 
               (hostPattern.contains("*") || hostPattern.contains("?"));
    }
    
    /**
     * 检查Host是否应该被跳过
     * Check if a host should be skipped during import
     * 
     * 跳过条件:
     * - 通配符Host（包含*或?）
     * - 空Host名称
     * - 保留关键字（如"*"）
     * 
     * @param host SSH Host配置
     * @return true表示应跳过
     */
    public boolean shouldSkipHost(SSHHostConfig host) {
        if (host == null || host.getHostPattern() == null) {
            return true;
        }
        
        String hostPattern = host.getHostPattern();
        
        // 跳过通配符Host
        if (isWildcardHost(hostPattern)) {
            logger.debug("跳过通配符Host: {}", hostPattern);
            return true;
        }
        
        // 跳过空Host
        if (hostPattern.trim().isEmpty() || "*".equals(hostPattern)) {
            logger.debug("跳过空Host或全局配置: {}", hostPattern);
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查Host是否需要警告（配置了不支持的特性）
     * Check if a host configuration needs warning
     * 
     * 需要警告的情况:
     * - 配置了ProxyJump/ProxyCommand（暂不支持跳板机）
     * - 配置了Include指令
     * - 缺少必要的配置项（HostName）
     * 
     * @param host SSH Host配置
     * @return 警告消息，如果无需警告返回null
     */
    public String getWarningMessage(SSHHostConfig host) {
        if (host == null) {
            return null;
        }
        
        List<String> warnings = new ArrayList<>();
        
        // 检查ProxyJump
        if (host.hasProxyJump()) {
            warnings.add("配置了ProxyJump（跳板机），导入后需手动验证连接");
        }
        
        // 检查Include
        if (host.getExtraOption("Include") != null) {
            warnings.add("配置了Include指令，未处理包含的文件");
        }
        
        // 检查必要字段
        if (host.getHostname() == null || host.getHostname().isEmpty()) {
            warnings.add("缺少HostName配置，无法确定服务器地址");
        }
        
        if (warnings.isEmpty()) {
            return null;
        }
        
        return String.join("; ", warnings);
    }
}
