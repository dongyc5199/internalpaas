package com.cmict.internalpaas.service.parser;

import com.cmict.internalpaas.dto.SSHHostConfig;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Xshell XML 文件解析器
 * 
 * <p>Xshell 使用 XML 格式（.xsh 文件）存储会话配置。本解析器使用 Jackson XML 库解析这些文件。</p>
 * 
 * <h3>支持的文件格式</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <session version="8.0">
 *     <title>MyServer</title>
 *     <host>192.168.1.100</host>
 *     <port>22</port>
 *     <protocol>SSH</protocol>
 *     <username>admin</username>
 *     <authentication>PASSWORD</authentication>
 *     <userkey>/path/to/key</userkey>
 *     <description>Production Server</description>
 *     <folder>Production/Databases</folder>
 * </session>
 * }</pre>
 * 
 * <h3>字段映射</h3>
 * <table border="1">
 *   <tr><th>Xshell 字段</th><th>SSHHostConfig 字段</th><th>说明</th></tr>
 *   <tr><td>title</td><td>hostPattern</td><td>会话名称</td></tr>
 *   <tr><td>host</td><td>hostname</td><td>主机地址（必填）</td></tr>
 *   <tr><td>port</td><td>port</td><td>端口号（默认22）</td></tr>
 *   <tr><td>username</td><td>user</td><td>用户名</td></tr>
 *   <tr><td>userkey</td><td>identityFile</td><td>私钥文件路径</td></tr>
 *   <tr><td>description</td><td>description</td><td>描述信息</td></tr>
 *   <tr><td>folder</td><td>group</td><td>分组/文件夹路径</td></tr>
 * </table>
 * 
 * <h3>特殊处理</h3>
 * <ul>
 *   <li><b>协议过滤</b>: 仅解析 protocol=SSH 的会话</li>
 *   <li><b>认证方式</b>: 支持 PASSWORD（密码）和 PublicKey（公钥）</li>
 *   <li><b>密码加密</b>: Xshell 密码已加密，无法直接读取，标记为需重新输入</li>
 * </ul>
 * 
 * @author System
 * @since 2025-10-19
 */
@Component
public class XshellXmlParser implements ConfigParser<File> {
    
    private static final Logger log = LoggerFactory.getLogger(XshellXmlParser.class);
    
    /**
     * XML 映射器
     */
    private final XmlMapper xmlMapper;
    
    /**
     * 构造函数
     */
    public XshellXmlParser() {
        this.xmlMapper = new XmlMapper();
    }
    
    /**
     * 解析 Xshell .xsh 配置文件
     *
     * @param xshFile .xsh 文件对象
     * @return SSH 会话配置列表（单文件只包含一个会话）
     * @throws ParseException 如果文件不存在、无法读取或格式错误
     */
    @Override
    public List<SSHHostConfig> parse(File xshFile) throws ParseException {
        if (xshFile == null) {
            throw new ParseException("File cannot be null");
        }
        
        if (!xshFile.exists()) {
            throw new ParseException("File does not exist: " + xshFile.getAbsolutePath());
        }
        
        log.debug("Parsing Xshell configuration file: {}", xshFile.getName());
        
        try {
            // 解析 XML 文件
            XshellSession session = xmlMapper.readValue(xshFile, XshellSession.class);
            
            // 验证协议类型
            if (session.protocol == null || !session.protocol.toUpperCase().startsWith("SSH")) {
                log.debug("Skipping non-SSH session: {} (protocol: {})", 
                    session.title, session.protocol);
                return new ArrayList<>();
            }
            
            // 映射到 SSHHostConfig
            SSHHostConfig config = mapToSSHHostConfig(session, xshFile.getName());
            
            if (config == null) {
                log.debug("Skipping session due to missing required fields: {}", session.title);
                return new ArrayList<>();
            }
            
            List<SSHHostConfig> result = new ArrayList<>();
            result.add(config);
            
            log.debug("Successfully parsed session: {}", config.getHostPattern());
            return result;
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse Xshell configuration file: " 
                + xshFile.getName() + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查是否支持指定的文件扩展名
     *
     * @param fileExtension 文件扩展名（如 ".xsh"）
     * @return 如果支持返回 true
     */
    @Override
    public boolean supports(String fileExtension) {
        if (fileExtension == null) {
            return false;
        }
        return fileExtension.equalsIgnoreCase(".xsh");
    }
    
    /**
     * 获取解析器名称
     *
     * @return "Xshell XML Parser"
     */
    @Override
    public String getParserName() {
        return "Xshell XML Parser";
    }
    
    /**
     * 将 XshellSession 映射到 SSHHostConfig
     *
     * @param session Xshell 会话对象
     * @param fileName 文件名（用于生成默认会话名称）
     * @return SSH 主机配置对象，如果缺少必填字段返回 null
     */
    private SSHHostConfig mapToSSHHostConfig(XshellSession session, String fileName) {
        // 验证必填字段：hostname
        if (session.host == null || session.host.trim().isEmpty()) {
            log.warn("Missing required field 'host' in session: {}", session.title);
            return null;
        }
        
        SSHHostConfig config = new SSHHostConfig();
        
        // hostPattern: 使用 title 字段，如果为空则使用文件名（去除 .xsh）
        String hostPattern = session.title;
        if (hostPattern == null || hostPattern.trim().isEmpty()) {
            hostPattern = fileName.replace(".xsh", "").replace(".XSH", "");
        }
        config.setHostPattern(hostPattern);
        
        // hostname（必填）
        config.setHostname(session.host.trim());
        
        // port（默认22）
        if (session.port != null && session.port > 0) {
            config.setPort(session.port);
        } else {
            config.setPort(22);
        }
        
        // user
        if (session.username != null && !session.username.trim().isEmpty()) {
            config.setUser(session.username.trim());
        }
        
        // identityFile (Xshell 使用 userkey 字段)
        if (session.userkey != null && !session.userkey.trim().isEmpty()) {
            config.setIdentityFile(session.userkey.trim());
        }
        
        // description
        if (session.description != null && !session.description.trim().isEmpty()) {
            config.setDescription(session.description.trim());
        }
        
        // group（Xshell 使用 folder 字段）
        if (session.folder != null && !session.folder.trim().isEmpty()) {
            config.setGroup(session.folder.trim());
        }
        
        return config;
    }
    
    /**
     * Xshell 会话 XML 映射类
     * 
     * <p>用于 Jackson XML 反序列化的 POJO 类</p>
     */
    @JacksonXmlRootElement(localName = "session")
    static class XshellSession {
        
        /**
         * 会话版本号
         */
        @JacksonXmlProperty(isAttribute = true)
        public String version;
        
        /**
         * 会话名称/标题
         */
        @JacksonXmlProperty(localName = "title")
        public String title;
        
        /**
         * 主机地址
         */
        @JacksonXmlProperty(localName = "host")
        public String host;
        
        /**
         * 端口号
         */
        @JacksonXmlProperty(localName = "port")
        public Integer port;
        
        /**
         * 协议类型（SSH, TELNET, RLOGIN 等）
         */
        @JacksonXmlProperty(localName = "protocol")
        public String protocol;
        
        /**
         * 用户名
         */
        @JacksonXmlProperty(localName = "username")
        public String username;
        
        /**
         * 认证方式（PASSWORD, PublicKey 等）
         */
        @JacksonXmlProperty(localName = "authentication")
        public String authentication;
        
        /**
         * 私钥文件路径
         */
        @JacksonXmlProperty(localName = "userkey")
        public String userkey;
        
        /**
         * 描述信息
         */
        @JacksonXmlProperty(localName = "description")
        public String description;
        
        /**
         * 分组/文件夹路径
         */
        @JacksonXmlProperty(localName = "folder")
        public String folder;
    }
}
