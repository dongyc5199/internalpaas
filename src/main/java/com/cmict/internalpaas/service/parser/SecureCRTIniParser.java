package com.cmict.internalpaas.service.parser;

import com.cmict.internalpaas.dto.SSHHostConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SecureCRT INI 文件解析器
 * 
 * SecureCRT 使用标准 INI 格式存储会话配置，每个字段都有特定的前缀：
 * - S:"字段名" = 字符串值
 * - D:"字段名" = 十六进制整数（如端口号）
 * - B:"字段名" = 布尔值
 * 
 * 示例 INI 文件：
 * ```ini
 * S:"Protocol Name"=SSH2
 * S:"Hostname"=192.168.1.100
 * D:"[SSH2] Port"=00000016  # 0x16 = 22
 * S:"Username"=admin
 * S:"Identity Filename"=/path/to/key
 * S:"Description"=Production Server
 * S:"Folder"=Production/Servers
 * ```
 * 
 * @author InternalPaaS Team
 * @since 2025-10-19 (Phase 2 - SecureCRT Scanner)
 */
@Slf4j
@Service
public class SecureCRTIniParser implements ConfigParser<File> {
    
    private static final String PARSER_NAME = "SecureCRT INI Parser";
    
    @Override
    public List<SSHHostConfig> parse(File source) throws ParseException {
        if (source == null || !source.exists() || !source.isFile()) {
            throw new ParseException("Invalid source file: " + source);
        }
        
        log.debug("Parsing SecureCRT INI file: {}", source.getAbsolutePath());
        
        try {
            Map<String, String> config = parseIniFile(source);
            SSHHostConfig hostConfig = mapToSSHHostConfig(config, source.getName());
            
            // 只返回有效的 SSH 配置（必须有主机名）
            if (hostConfig != null && hostConfig.getHostname() != null) {
                List<SSHHostConfig> result = new ArrayList<>();
                result.add(hostConfig);
                return result;
            }
            
            log.debug("File {} does not contain valid SSH configuration", source.getName());
            return new ArrayList<>();
            
        } catch (IOException e) {
            throw new ParseException("Failed to read file: " + source.getName(), e);
        } catch (Exception e) {
            throw new ParseException("Failed to parse INI file: " + source.getName(), e);
        }
    }
    
    @Override
    public boolean supports(String fileExtension) {
        return fileExtension != null && 
               fileExtension.toLowerCase().equals(".ini");
    }
    
    @Override
    public String getParserName() {
        return PARSER_NAME;
    }
    
    /**
     * 解析 INI 文件为键值对映射
     */
    private Map<String, String> parseIniFile(File file) throws IOException {
        Map<String, String> config = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 跳过空行和注释
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }
                
                // 解析键值对
                KeyValue kv = parseIniLine(line);
                if (kv != null) {
                    config.put(kv.key, kv.value);
                }
            }
        }
        
        log.debug("Parsed {} key-value pairs from {}", config.size(), file.getName());
        return config;
    }
    
    /**
     * 解析单行 INI 配置
     * 
     * SecureCRT INI 格式：
     * - S:"Hostname"=192.168.1.100
     * - D:"[SSH2] Port"=00000016
     */
    private KeyValue parseIniLine(String line) {
        try {
            // 查找等号位置
            int equalsIndex = line.indexOf('=');
            if (equalsIndex == -1) {
                return null;
            }
            
            // 提取键和值
            String keyPart = line.substring(0, equalsIndex).trim();
            String valuePart = line.substring(equalsIndex + 1).trim();
            
            // 提取类型前缀 (S:, D:, B:)
            char typePrefix = keyPart.charAt(0);
            
            // 提取键名（去除引号）
            String key = extractQuotedString(keyPart.substring(2));
            if (key == null) {
                return null;
            }
            
            // 根据类型解析值
            String value = parseValue(typePrefix, valuePart);
            
            return new KeyValue(key, value);
            
        } catch (Exception e) {
            log.debug("Failed to parse INI line: {}", line, e);
            return null;
        }
    }
    
    /**
     * 提取双引号中的字符串
     */
    private String extractQuotedString(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        
        int firstQuote = str.indexOf('"');
        int lastQuote = str.lastIndexOf('"');
        
        if (firstQuote != -1 && lastQuote != -1 && firstQuote < lastQuote) {
            return str.substring(firstQuote + 1, lastQuote);
        }
        
        return str.trim();
    }
    
    /**
     * 根据类型前缀解析值
     * 
     * @param typePrefix S (字符串), D (十六进制整数), B (布尔)
     * @param valuePart 原始值字符串
     */
    private String parseValue(char typePrefix, String valuePart) {
        switch (typePrefix) {
            case 'S': // 字符串
                return extractQuotedString(valuePart);
                
            case 'D': // 十六进制整数
                return parseHexInteger(valuePart);
                
            case 'B': // 布尔值
                return valuePart;
                
            default:
                return valuePart;
        }
    }
    
    /**
     * 解析 SecureCRT 的十六进制端口号
     * 
     * 示例: "00000016" -> "22"
     */
    private String parseHexInteger(String hexString) {
        try {
            hexString = hexString.trim();
            int value = Integer.parseInt(hexString, 16);
            return String.valueOf(value);
        } catch (NumberFormatException e) {
            log.debug("Failed to parse hex integer: {}", hexString);
            return hexString; // 返回原始值
        }
    }
    
    /**
     * 将 INI 配置映射到 SSHHostConfig
     */
    private SSHHostConfig mapToSSHHostConfig(Map<String, String> config, String fileName) {
        // 检查是否为 SSH 协议
        String protocol = config.get("Protocol Name");
        if (protocol == null || (!protocol.equals("SSH2") && !protocol.equals("SSH1"))) {
            log.debug("File {} is not SSH protocol (protocol={})", fileName, protocol);
            return null;
        }
        
        SSHHostConfig hostConfig = new SSHHostConfig();
        
        // 主机名（必填）
        String hostname = config.get("Hostname");
        if (hostname == null || hostname.isEmpty()) {
            log.debug("File {} missing hostname", fileName);
            return null;
        }
        hostConfig.setHostname(hostname);
        
        // 端口号（默认 22）
        String portStr = config.get("[SSH2] Port");
        if (portStr == null) {
            portStr = config.get("[SSH1] Port");
        }
        int port = 22;
        if (portStr != null) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                log.debug("Invalid port number: {}, using default 22", portStr);
            }
        }
        hostConfig.setPort(port);
        
        // 用户名
        String username = config.get("Username");
        if (username != null && !username.isEmpty()) {
            hostConfig.setUser(username);
        }
        
        // 私钥路径
        String identityFile = config.get("Identity Filename");
        if (identityFile != null && !identityFile.isEmpty()) {
            hostConfig.setIdentityFile(identityFile);
        }
        
        // 会话名称（从文件名提取，去除 .ini 扩展名）
        String sessionName = fileName;
        if (sessionName.endsWith(".ini")) {
            sessionName = sessionName.substring(0, sessionName.length() - 4);
        }
        hostConfig.setHostPattern(sessionName);
        
        // 描述信息
        String description = config.get("Description");
        if (description != null && !description.isEmpty()) {
            hostConfig.setDescription(description);
        }
        
        // 分组信息（Folder）
        String folder = config.get("Folder");
        if (folder != null && !folder.isEmpty()) {
            hostConfig.setGroup(folder);
        }
        
        // 防火墙/跳板机设置
        String firewallName = config.get("Firewall Name");
        if (firewallName != null && !firewallName.isEmpty()) {
            // SecureCRT 的 Firewall 可能对应 ProxyJump
            // 暂不处理，后续可扩展
            log.debug("Session {} uses firewall: {}", sessionName, firewallName);
        }
        
        log.debug("Mapped SSH config: {}@{}:{}", username, hostname, port);
        return hostConfig;
    }
    
    /**
     * 键值对辅助类
     */
    private static class KeyValue {
        final String key;
        final String value;
        
        KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
