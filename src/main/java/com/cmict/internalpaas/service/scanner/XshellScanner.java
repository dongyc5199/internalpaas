package com.cmict.internalpaas.service.scanner;

import com.cmict.internalpaas.dto.SSHHostConfig;
import com.cmict.internalpaas.service.parser.ConfigParser;
import com.cmict.internalpaas.util.PathUtil;
import com.cmict.internalpaas.util.WindowsRegistryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Xshell SSH客户端扫描器
 * 
 * <p>Xshell 是由 NetSarang 开发的强大的终端模拟器，支持SSH、TELNET、RLOGIN等协议。
 * 它使用 XML 格式（.xsh 文件）存储会话配置。</p>
 * 
 * <h3>检测方式</h3>
 * <ul>
 *   <li><b>Windows</b>: 检查注册表 {@code HKEY_CURRENT_USER\Software\NetSarang\Xshell\{版本}}
 *       <br>支持的版本: Xshell 7, 6, 5, 4</li>
 *   <li><b>其他平台</b>: Xshell 仅支持 Windows 平台</li>
 * </ul>
 * 
 * <h3>配置文件位置</h3>
 * <ul>
 *   <li>Windows: {@code %USERPROFILE%\Documents\NetSarang Computer\{版本}\Xshell\Sessions\}</li>
 *   <li>备用路径: {@code %APPDATA%\NetSarang\Xshell\Sessions\}</li>
 * </ul>
 * 
 * <h3>配置文件格式</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <session version="8.0">
 *     <title>MyServer</title>
 *     <host>192.168.1.100</host>
 *     <port>22</port>
 *     <protocol>SSH</protocol>
 *     <username>admin</username>
 *     <authentication>PASSWORD</authentication>
 *     <description>Production Server</description>
 *     <folder>Production</folder>
 * </session>
 * }</pre>
 * 
 * <h3>扫描流程（4步）</h3>
 * <ol>
 *   <li><b>Step 1</b>: 从注册表读取配置路径（最精准）</li>
 *   <li><b>Step 2</b>: 扫描默认路径（最常见）</li>
 *   <li><b>Step 3</b>: 从安装目录推断配置路径（容错机制）</li>
 *   <li><b>Step 4</b>: 解析所有 .xsh 文件</li>
 * </ol>
 * 
 * @author System
 * @since 2025-10-19
 */
@Service
public class XshellScanner implements ClientScanner {
    
    private static final Logger log = LoggerFactory.getLogger(XshellScanner.class);
    
    /**
     * 注册表根键
     */
    private static final String REGISTRY_ROOT = "HKEY_CURRENT_USER";
    
    /**
     * 注册表基础路径
     */
    private static final String REGISTRY_BASE_KEY = "Software\\NetSarang";
    
    /**
     * 支持的 Xshell 版本列表（从新到旧）
     */
    private static final List<String> SUPPORTED_VERSIONS = Arrays.asList(
        "Xshell 7", "Xshell 6", "Xshell 5", "Xshell 4", "Xshell"
    );
    
    /**
     * 默认扫描路径列表
     * Windows: %USERPROFILE%\Documents\NetSarang Computer\{版本}\Xshell\Sessions\
     */
    private static final List<String> DEFAULT_PATHS = Arrays.asList(
        "%USERPROFILE%\\Documents\\NetSarang Computer\\7\\Xshell\\Sessions",
        "%USERPROFILE%\\Documents\\NetSarang Computer\\6\\Xshell\\Sessions",
        "%USERPROFILE%\\Documents\\NetSarang Computer\\5\\Xshell\\Sessions",
        "%USERPROFILE%\\Documents\\NetSarang Computer\\4\\Xshell\\Sessions",
        "%APPDATA%\\NetSarang\\Xshell\\Sessions"
    );
    
    /**
     * XML 解析器
     */
    private final ConfigParser<File> xmlParser;
    
    /**
     * 构造函数（依赖注入）
     *
     * @param xmlParser XML 格式配置解析器
     */
    @Autowired
    public XshellScanner(@Qualifier("xshellXmlParser") ConfigParser<File> xmlParser) {
        this.xmlParser = xmlParser;
    }
    
    /**
     * 获取客户端名称
     *
     * @return "Xshell"
     */
    @Override
    public String getClientName() {
        return "Xshell";
    }
    
    /**
     * 检测 Xshell 是否已安装
     * 
     * <p>检测逻辑：</p>
     * <ol>
     *   <li>检查是否为 Windows 平台（Xshell 仅支持 Windows）</li>
     *   <li>遍历支持的版本列表，检查注册表键是否存在</li>
     *   <li>任一版本的注册表键存在即返回 true</li>
     * </ol>
     *
     * @return 如果检测到 Xshell 已安装返回 true，否则返回 false
     */
    @Override
    public boolean isInstalled() {
        // Xshell 仅支持 Windows 平台
        if (!WindowsRegistryUtil.isWindows()) {
            log.debug("Xshell is only available on Windows platform");
            return false;
        }
        
        log.debug("Checking if Xshell is installed");
        
        // 检查所有支持的版本
        for (String version : SUPPORTED_VERSIONS) {
            String registryKey = REGISTRY_BASE_KEY + "\\" + version;
            boolean exists = WindowsRegistryUtil.registryKeyExists(REGISTRY_ROOT, registryKey);
            
            if (exists) {
                log.info("Found Xshell installation: {}", version);
                return true;
            }
        }
        
        log.debug("Xshell is not installed (no registry keys found)");
        return false;
    }
    
    /**
     * 获取 Xshell 客户端版本号
     * 
     * <p>版本检测逻辑：</p>
     * <ol>
     *   <li>遍历支持的版本列表（从新到旧）</li>
     *   <li>检查注册表键是否存在</li>
     *   <li>尝试读取 Version 键值</li>
     *   <li>如果读取成功返回版本号，否则返回版本名称（如 "7"）</li>
     * </ol>
     *
     * @return 版本号字符串（如 "7.0.0.1", "7", "Unknown"）
     */
    @Override
    public String getClientVersion() {
        if (!WindowsRegistryUtil.isWindows()) {
            return "Unknown";
        }
        
        log.debug("Getting Xshell version");
        
        // 从最新版本开始查找
        for (String version : SUPPORTED_VERSIONS) {
            String registryKey = REGISTRY_BASE_KEY + "\\" + version;
            boolean exists = WindowsRegistryUtil.registryKeyExists(REGISTRY_ROOT, registryKey);
            
            if (exists) {
                // 尝试读取版本号
                String versionNumber = WindowsRegistryUtil.readRegistryValue(
                    REGISTRY_ROOT, registryKey, "Version"
                );
                
                if (versionNumber != null && !versionNumber.isEmpty()) {
                    log.debug("Found Xshell version: {}", versionNumber);
                    return versionNumber;
                }
                
                // 如果无法读取具体版本号，返回版本名称中的数字
                String versionDigit = version.replace("Xshell ", "").trim();
                if (!versionDigit.isEmpty()) {
                    log.debug("Found Xshell version: {}", versionDigit);
                    return versionDigit;
                }
            }
        }
        
        log.debug("Unable to determine Xshell version");
        return "Unknown";
    }
    
    /**
     * 获取默认扫描路径列表
     *
     * @return 默认路径列表
     */
    @Override
    public List<String> getDefaultScanPaths() {
        return new ArrayList<>(DEFAULT_PATHS);
    }
    
    /**
     * 扫描 Xshell 配置文件并解析 SSH 会话
     * 
     * <p>扫描流程（4步）：</p>
     * <ol>
     *   <li><b>Step 1</b>: 从注册表读取配置路径
     *       <br>键: {@code HKEY_CURRENT_USER\Software\NetSarang\Xshell\{版本}\ConfigPath}</li>
     *   <li><b>Step 2</b>: 扫描默认路径
     *       <br>路径: {@code %USERPROFILE%\Documents\NetSarang Computer\{版本}\Xshell\Sessions\}</li>
     *   <li><b>Step 3</b>: 从安装目录推断配置路径
     *       <br>从注册表读取安装路径，构造可能的配置路径</li>
     *   <li><b>Step 4</b>: 解析配置文件
     *       <br>遍历找到的配置目录，解析所有 .xsh 文件</li>
     * </ol>
     *
     * @return 扫描结果对象，包含状态、会话列表、警告信息等
     */
    @Override
    public ScanResult scanConfigurations() {
        log.info("Starting Xshell configuration scan");
        
        ScanResult.ScanResultBuilder resultBuilder = ScanResult.builder()
            .clientName("Xshell")
            .clientVersion(getClientVersion());
        
        try {
            // 检查是否已安装
            if (!isInstalled()) {
                log.warn("Xshell is not installed");
                return resultBuilder
                    .status(ScanStatus.NOT_INSTALLED)
                    .build();
            }
            
            // Step 1: 从注册表读取配置路径
            String sessionsPath = readConfigPathFromRegistry();
            if (sessionsPath != null) {
                log.debug("Registry config path: {}", sessionsPath);
            }
            
            // Step 2: 如果注册表读取失败，尝试默认路径
            if (sessionsPath == null) {
                log.debug("Registry config path not found, trying default paths");
                sessionsPath = findConfigPathFromDefaults();
            }
            
            // Step 3: 如果默认路径也失败，从安装目录推断
            if (sessionsPath == null) {
                log.debug("Default paths not found, inferring from installation path");
                sessionsPath = inferConfigPathFromInstallation();
            }
            
            // 如果所有方法都失败
            if (sessionsPath == null) {
                log.warn("Xshell configuration path not found");
                return resultBuilder
                    .status(ScanStatus.CONFIG_NOT_FOUND)
                    .build();
            }
            
            resultBuilder.configPath(sessionsPath);
            
            // Step 4: 解析配置文件
            List<SSHHostConfig> sessions = parseConfigFiles(sessionsPath);
            
            if (sessions.isEmpty()) {
                log.info("No SSH sessions found in Xshell configuration");
                return resultBuilder
                    .status(ScanStatus.SUCCESS)
                    .sessionCount(0)
                    .build();
            }
            
            log.info("Successfully scanned {} Xshell sessions", sessions.size());
            return resultBuilder
                .status(ScanStatus.SUCCESS)
                .sessionCount(sessions.size())
                .sessions(sessions)
                .build();
            
        } catch (Exception e) {
            log.error("Failed to parse Xshell configurations", e);
            return resultBuilder
                .status(ScanStatus.PARSE_ERROR)
                .errorMessage(e.getMessage())
                .build();
        }
    }
    
    /**
     * Step 1: 从注册表读取配置路径
     *
     * @return 配置路径，如果读取失败返回 null
     */
    private String readConfigPathFromRegistry() {
        if (!WindowsRegistryUtil.isWindows()) {
            return null;
        }
        
        // 尝试从所有支持的版本中读取
        for (String version : SUPPORTED_VERSIONS) {
            String registryKey = REGISTRY_BASE_KEY + "\\" + version;
            
            // 尝试读取 ConfigPath
            String configPath = WindowsRegistryUtil.readRegistryValue(
                REGISTRY_ROOT, registryKey, "ConfigPath"
            );
            
            if (configPath != null && !configPath.isEmpty()) {
                String sessionsPath = configPath + "\\Sessions";
                if (PathUtil.pathExists(sessionsPath)) {
                    log.debug("Found config path from registry ({}): {}", version, sessionsPath);
                    return sessionsPath;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Step 2: 从默认路径列表中查找配置目录
     *
     * @return 找到的配置路径，如果所有默认路径都不存在返回 null
     */
    private String findConfigPathFromDefaults() {
        for (String path : DEFAULT_PATHS) {
            String expandedPath = PathUtil.expandEnvironmentVariables(path);
            
            if (PathUtil.pathExists(expandedPath)) {
                log.debug("Found config path from defaults: {}", expandedPath);
                return expandedPath;
            }
        }
        
        return null;
    }
    
    /**
     * Step 3: 从安装目录推断配置路径
     * 
     * <p>推断逻辑：</p>
     * <ol>
     *   <li>从注册表读取安装路径（InstallPath）</li>
     *   <li>构造可能的配置路径：{InstallPath}\Sessions</li>
     *   <li>验证推断的路径是否存在</li>
     * </ol>
     *
     * @return 推断的配置路径，如果推断失败返回 null
     */
    private String inferConfigPathFromInstallation() {
        if (!WindowsRegistryUtil.isWindows()) {
            return null;
        }
        
        // 尝试从所有支持的版本中推断
        for (String version : SUPPORTED_VERSIONS) {
            String registryKey = REGISTRY_BASE_KEY + "\\" + version;
            
            // 读取安装路径
            String installPath = WindowsRegistryUtil.readRegistryValue(
                REGISTRY_ROOT, registryKey, "InstallPath"
            );
            
            if (installPath != null && !installPath.isEmpty()) {
                // 尝试多个可能的路径组合
                List<String> possiblePaths = Arrays.asList(
                    installPath + "\\Sessions",
                    installPath + "\\Config\\Sessions"
                );
                
                for (String inferredPath : possiblePaths) {
                    if (PathUtil.pathExists(inferredPath)) {
                        log.debug("Inferred config path from installation ({}): {}", version, inferredPath);
                        return inferredPath;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Step 4: 解析配置文件
     * 
     * <p>解析逻辑：</p>
     * <ol>
     *   <li>使用 {@link PathUtil#findFilesWithExtension(String, String)} 递归查找所有 .xsh 文件</li>
     *   <li>对每个文件调用 XML 解析器进行解析</li>
     *   <li>合并所有解析结果</li>
     *   <li>单个文件解析失败不影响其他文件</li>
     * </ol>
     *
     * @param configPath 配置文件目录路径
     * @return SSH 会话配置列表
     */
    private List<SSHHostConfig> parseConfigFiles(String configPath) {
        List<SSHHostConfig> allSessions = new ArrayList<>();
        
        try {
            // 查找所有 .xsh 文件
            List<String> xshFiles = PathUtil.findFilesWithExtension(configPath, ".xsh");
            
            log.debug("Found {} .xsh files in {}", xshFiles.size(), configPath);
            
            // 解析每个文件
            for (String xshFilePath : xshFiles) {
                try {
                    File xshFile = new File(xshFilePath);
                    List<SSHHostConfig> sessions = xmlParser.parse(xshFile);
                    
                    if (sessions != null && !sessions.isEmpty()) {
                        allSessions.addAll(sessions);
                        log.debug("Parsed {} sessions from {}", sessions.size(), xshFile.getName());
                    }
                    
                } catch (Exception e) {
                    log.warn("Failed to parse {}: {}", xshFilePath, e.getMessage());
                    // 继续解析其他文件
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to scan config directory: {}", configPath, e);
        }
        
        return allSessions;
    }
}
