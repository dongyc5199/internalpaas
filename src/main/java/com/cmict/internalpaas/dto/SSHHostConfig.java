package com.cmict.internalpaas.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * SSH配置Host块
 * SSH Config Host Block DTO
 * 
 * 用于存储从SSH配置文件解析出的单个Host块信息
 * 
 * @author GitHub Copilot
 * @since 2025-10-18
 */
public class SSHHostConfig {
    
    /**
     * Host名称（可能包含通配符）
     * 例如: "dev-server-01", "prod-*", "*.example.com"
     */
    private String hostPattern;
    
    /**
     * HostName - 实际的主机名或IP地址
     * SSH配置中的HostName字段
     */
    private String hostname;
    
    /**
     * Port - SSH端口
     * SSH配置中的Port字段，默认22
     */
    private Integer port;
    
    /**
     * User - SSH用户名
     * SSH配置中的User字段
     */
    private String user;
    
    /**
     * IdentityFile - SSH私钥文件路径
     * SSH配置中的IdentityFile字段
     * 例如: "~/.ssh/id_rsa", "/home/user/.ssh/id_rsa_prod"
     */
    private String identityFile;
    
    /**
     * ProxyJump - 跳板机配置
     * SSH配置中的ProxyJump字段
     * 例如: "bastion.example.com"
     */
    private String proxyJump;
    
    /**
     * Description - 描述信息
     * 用于存储会话的描述信息（SecureCRT等客户端）
     */
    private String description;
    
    /**
     * Group - 分组信息
     * 用于存储会话的分组或文件夹路径（SecureCRT等客户端）
     * 例如: "Production/Servers", "Development"
     */
    private String group;
    
    /**
     * 其他配置项
     * 存储未明确定义的SSH配置项
     * 例如: ForwardAgent, ServerAliveInterval等
     */
    private Map<String, String> extraOptions;

    /**
     * 构造函数
     * 
     * @param hostPattern Host名称
     */
    public SSHHostConfig(String hostPattern) {
        this.hostPattern = hostPattern;
        this.extraOptions = new HashMap<>();
    }

    /**
     * 默认构造函数
     */
    public SSHHostConfig() {
        this.extraOptions = new HashMap<>();
    }

    // ==================== Getters and Setters ====================

    public String getHostPattern() {
        return hostPattern;
    }

    public void setHostPattern(String hostPattern) {
        this.hostPattern = hostPattern;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getIdentityFile() {
        return identityFile;
    }

    public void setIdentityFile(String identityFile) {
        this.identityFile = identityFile;
    }

    public String getProxyJump() {
        return proxyJump;
    }

    public void setProxyJump(String proxyJump) {
        this.proxyJump = proxyJump;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Map<String, String> getExtraOptions() {
        return extraOptions;
    }

    public void setExtraOptions(Map<String, String> extraOptions) {
        this.extraOptions = extraOptions;
    }

    /**
     * 添加额外配置项
     * 
     * @param key 配置项名称
     * @param value 配置项值
     */
    public void addExtraOption(String key, String value) {
        this.extraOptions.put(key, value);
    }

    /**
     * 获取额外配置项
     * 
     * @param key 配置项名称
     * @return 配置项值，如果不存在返回null
     */
    public String getExtraOption(String key) {
        return this.extraOptions.get(key);
    }

    /**
     * 检查是否为通配符Host
     * 通配符Host包含 * 或 ? 字符
     * 
     * @return true表示是通配符Host
     */
    public boolean isWildcardHost() {
        return hostPattern != null && 
               (hostPattern.contains("*") || hostPattern.contains("?"));
    }

    /**
     * 检查是否使用跳板机
     * 
     * @return true表示配置了ProxyJump
     */
    public boolean hasProxyJump() {
        return proxyJump != null && !proxyJump.isEmpty();
    }

    /**
     * 检查是否配置了私钥
     * 
     * @return true表示配置了IdentityFile
     */
    public boolean hasIdentityFile() {
        return identityFile != null && !identityFile.isEmpty();
    }

    /**
     * 获取有效的端口号
     * 如果未配置，返回默认值22
     * 
     * @return SSH端口号
     */
    public int getEffectivePort() {
        return port != null ? port : 22;
    }

    @Override
    public String toString() {
        return "SSHHostConfig{" +
                "hostPattern='" + hostPattern + '\'' +
                ", hostname='" + hostname + '\'' +
                ", port=" + port +
                ", user='" + user + '\'' +
                ", identityFile='" + identityFile + '\'' +
                ", proxyJump='" + proxyJump + '\'' +
                ", extraOptions=" + extraOptions +
                '}';
    }
}
