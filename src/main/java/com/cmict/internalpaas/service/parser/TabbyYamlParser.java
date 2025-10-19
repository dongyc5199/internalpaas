package com.cmict.internalpaas.service.parser;

import com.cmict.internalpaas.dto.SSHHostConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tabby YAML 配置文件解析器
 * 
 * <p>Tabby 使用 YAML 格式存储所有配置信息，包括 SSH 连接配置。
 * 本解析器使用 SnakeYAML 库解析配置文件。</p>
 * 
 * <h3>支持的文件格式</h3>
 * <pre>{@code
 * profiles:
 *   - type: ssh
 *     name: Production Server
 *     options:
 *       host: 192.168.1.100
 *       port: 22
 *       user: admin
 *       privateKey: /path/to/key
 *     group: Production/Databases
 * }</pre>
 * 
 * <h3>字段映射</h3>
 * <table border="1">
 *   <tr><th>Tabby 字段</th><th>SSHHostConfig 字段</th><th>说明</th></tr>
 *   <tr><td>name</td><td>hostPattern</td><td>会话名称</td></tr>
 *   <tr><td>options.host</td><td>hostname</td><td>主机地址（必填）</td></tr>
 *   <tr><td>options.port</td><td>port</td><td>端口号（默认22）</td></tr>
 *   <tr><td>options.user</td><td>user</td><td>用户名</td></tr>
 *   <tr><td>options.privateKey</td><td>identityFile</td><td>私钥文件路径</td></tr>
 *   <tr><td>group</td><td>group</td><td>分组路径</td></tr>
 * </table>
 * 
 * <h3>特殊处理</h3>
 * <ul>
 *   <li><b>类型过滤</b>: 仅解析 type=ssh 的 profile</li>
 *   <li><b>嵌套 options</b>: host/port/user 等字段在 options 对象下</li>
 *   <li><b>密码保存</b>: Tabby 不保存明文密码，通过系统密钥链管理</li>
 * </ul>
 * 
 * @author System
 * @since 2025-10-19
 */
@Component
public class TabbyYamlParser implements ConfigParser<File> {
    
    private static final Logger log = LoggerFactory.getLogger(TabbyYamlParser.class);
    
    /**
     * YAML 解析器
     */
    private final Yaml yaml;
    
    /**
     * 构造函数
     */
    public TabbyYamlParser() {
        this.yaml = new Yaml();
    }
    
    /**
     * 解析 Tabby config.yaml 配置文件
     *
     * @param configFile config.yaml 文件对象
     * @return SSH 会话配置列表
     * @throws ParseException 如果文件不存在、无法读取或格式错误
     */
    @Override
    public List<SSHHostConfig> parse(File configFile) throws ParseException {
        if (configFile == null) {
            throw new ParseException("File cannot be null");
        }
        
        if (!configFile.exists()) {
            throw new ParseException("File does not exist: " + configFile.getAbsolutePath());
        }
        
        log.debug("Parsing Tabby configuration file: {}", configFile.getName());
        
        try (InputStream inputStream = new FileInputStream(configFile)) {
            // 解析 YAML 文件
            Map<String, Object> config = yaml.load(inputStream);
            
            if (config == null || config.isEmpty()) {
                log.debug("Empty configuration file");
                return new ArrayList<>();
            }
            
            // 提取 profiles 列表
            Object profilesObj = config.get("profiles");
            if (!(profilesObj instanceof List)) {
                log.debug("No 'profiles' section found in config");
                return new ArrayList<>();
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> profiles = (List<Map<String, Object>>) profilesObj;
            
            List<SSHHostConfig> sessions = new ArrayList<>();
            
            // 遍历每个 profile
            for (Map<String, Object> profile : profiles) {
                try {
                    // 过滤：仅处理 SSH 类型
                    Object type = profile.get("type");
                    if (type == null || !"ssh".equalsIgnoreCase(type.toString())) {
                        continue;
                    }
                    
                    // 映射到 SSHHostConfig
                    SSHHostConfig session = mapToSSHHostConfig(profile);
                    if (session != null) {
                        sessions.add(session);
                    }
                    
                } catch (Exception e) {
                    log.warn("Failed to parse profile: {}", e.getMessage());
                    // 继续处理其他 profile
                }
            }
            
            log.debug("Successfully parsed {} SSH sessions", sessions.size());
            return sessions;
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse Tabby configuration file: " 
                + configFile.getName() + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查是否支持指定的文件扩展名
     *
     * @param fileExtension 文件扩展名（如 ".yaml"）
     * @return 如果支持返回 true
     */
    @Override
    public boolean supports(String fileExtension) {
        if (fileExtension == null) {
            return false;
        }
        return fileExtension.equalsIgnoreCase(".yaml") || 
               fileExtension.equalsIgnoreCase(".yml");
    }
    
    /**
     * 获取解析器名称
     *
     * @return "Tabby YAML Parser"
     */
    @Override
    public String getParserName() {
        return "Tabby YAML Parser";
    }
    
    /**
     * 将 Tabby profile 映射到 SSHHostConfig
     *
     * @param profile Tabby profile 对象
     * @return SSH 主机配置对象，如果缺少必填字段返回 null
     */
    private SSHHostConfig mapToSSHHostConfig(Map<String, Object> profile) {
        // 提取 options 对象
        Object optionsObj = profile.get("options");
        if (!(optionsObj instanceof Map)) {
            log.warn("Profile missing 'options' section");
            return null;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> options = (Map<String, Object>) optionsObj;
        
        // 验证必填字段：host
        Object hostObj = options.get("host");
        if (hostObj == null || hostObj.toString().trim().isEmpty()) {
            log.warn("Profile missing required field 'options.host'");
            return null;
        }
        
        SSHHostConfig config = new SSHHostConfig();
        
        // hostPattern: 使用 name 字段
        Object nameObj = profile.get("name");
        if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
            config.setHostPattern(nameObj.toString().trim());
        } else {
            // 如果没有名称，使用 hostname
            config.setHostPattern(hostObj.toString().trim());
        }
        
        // hostname（必填）
        config.setHostname(hostObj.toString().trim());
        
        // port（默认22）
        Object portObj = options.get("port");
        if (portObj != null) {
            try {
                int port = Integer.parseInt(portObj.toString());
                config.setPort(port);
            } catch (NumberFormatException e) {
                log.warn("Invalid port number: {}, using default 22", portObj);
                config.setPort(22);
            }
        } else {
            config.setPort(22);
        }
        
        // user
        Object userObj = options.get("user");
        if (userObj != null && !userObj.toString().trim().isEmpty()) {
            config.setUser(userObj.toString().trim());
        }
        
        // identityFile (Tabby 使用 privateKey 字段)
        Object privateKeyObj = options.get("privateKey");
        if (privateKeyObj != null && !privateKeyObj.toString().trim().isEmpty()) {
            config.setIdentityFile(privateKeyObj.toString().trim());
        }
        
        // description (可选字段)
        Object descriptionObj = profile.get("description");
        if (descriptionObj != null && !descriptionObj.toString().trim().isEmpty()) {
            config.setDescription(descriptionObj.toString().trim());
        }
        
        // group
        Object groupObj = profile.get("group");
        if (groupObj != null && !groupObj.toString().trim().isEmpty()) {
            config.setGroup(groupObj.toString().trim());
        }
        
        return config;
    }
}
