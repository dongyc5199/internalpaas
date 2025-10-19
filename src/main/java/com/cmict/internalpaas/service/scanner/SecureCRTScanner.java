package com.cmict.internalpaas.service.scanner;

import com.cmict.internalpaas.dto.SSHHostConfig;
import com.cmict.internalpaas.service.parser.ConfigParser;
import com.cmict.internalpaas.util.PathUtil;
import com.cmict.internalpaas.util.WindowsRegistryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SecureCRT SSH客户端扫描器
 * 
 * SecureCRT 是由 VanDyke Software 开发的终端仿真程序，支持 SSH、Telnet 等协议。
 * 配置文件格式：.ini（标准 Windows INI 格式）
 * 
 * 检测方式：
 * - Windows: 检查注册表 HKEY_CURRENT_USER\Software\VanDyke\SecureCRT
 * - macOS: 检查 /Applications/SecureCRT.app
 * - Linux: 检查 ~/.vandyke/SecureCRT
 * 
 * 配置路径：
 * - Windows: %APPDATA%\VanDyke\Config\Sessions\
 * - macOS: ~/Library/Application Support/VanDyke/SecureCRT/Config/Sessions/
 * - Linux: ~/.vandyke/SecureCRT/Config/Sessions/
 * 
 * @author InternalPaaS Team
 * @since 2025-10-19 (Phase 2 - SecureCRT Scanner)
 */
@Slf4j
@Service
public class SecureCRTScanner implements ClientScanner {
    
    private static final String CLIENT_NAME = "SecureCRT";
    
    // Windows 注册表路径
    private static final String REGISTRY_KEY = "Software\\VanDyke\\SecureCRT";
    private static final String REGISTRY_ROOT = "HKEY_CURRENT_USER";
    
    // 默认配置路径（按优先级排序）
    private static final List<String> DEFAULT_PATHS = Arrays.asList(
        // Windows
        "%APPDATA%\\VanDyke\\Config\\Sessions",
        "%USERPROFILE%\\AppData\\Roaming\\VanDyke\\Config\\Sessions",
        
        // macOS
        "~/Library/Application Support/VanDyke/SecureCRT/Config/Sessions",
        
        // Linux
        "~/.vandyke/SecureCRT/Config/Sessions"
    );
    
    private final ConfigParser<File> iniParser;
    
    public SecureCRTScanner(@Qualifier("secureCRTIniParser") ConfigParser<File> iniParser) {
        this.iniParser = iniParser;
    }
    
    @Override
    public String getClientName() {
        return CLIENT_NAME;
    }
    
    @Override
    public String getClientVersion() {
        if (!isInstalled()) {
            log.debug("SecureCRT is not installed");
            return null;
        }
        
        try {
            // 尝试从注册表读取版本号（Windows）
            if (WindowsRegistryUtil.isWindows()) {
                String version = WindowsRegistryUtil.readRegistryValue(
                    REGISTRY_ROOT,
                    REGISTRY_KEY,
                    "Version"
                );
                
                if (version != null && !version.isEmpty()) {
                    log.info("Detected SecureCRT version: {}", version);
                    return version;
                }
            }
            
            // macOS/Linux: 从安装目录或配置文件推断版本
            // TODO: 实现 macOS/Linux 版本检测
            log.debug("Unable to detect SecureCRT version on non-Windows platform");
            return "Unknown";
            
        } catch (Exception e) {
            log.error("Failed to get SecureCRT version", e);
            return "Unknown";
        }
    }
    
    @Override
    public boolean isInstalled() {
        // Windows: 检查注册表
        if (WindowsRegistryUtil.isWindows()) {
            boolean registryExists = WindowsRegistryUtil.registryKeyExists(
                REGISTRY_ROOT,
                REGISTRY_KEY
            );
            
            if (registryExists) {
                log.debug("SecureCRT detected via Windows registry");
                return true;
            }
        }
        
        // macOS: 检查应用程序目录
        if (isMacOS()) {
            String appPath = "/Applications/SecureCRT.app";
            if (PathUtil.pathExists(appPath)) {
                log.debug("SecureCRT detected at {}", appPath);
                return true;
            }
        }
        
        // Linux: 检查用户目录
        if (isLinux()) {
            String configPath = PathUtil.expandEnvironmentVariables(
                "~/.vandyke/SecureCRT"
            );
            if (PathUtil.pathExists(configPath)) {
                log.debug("SecureCRT detected at {}", configPath);
                return true;
            }
        }
        
        // 最后尝试：检查是否有任何默认配置路径存在
        for (String path : DEFAULT_PATHS) {
            String expandedPath = PathUtil.expandEnvironmentVariables(path);
            if (PathUtil.pathExists(expandedPath)) {
                log.debug("SecureCRT configuration found at {}", expandedPath);
                return true;
            }
        }
        
        log.debug("SecureCRT is not installed");
        return false;
    }
    
    @Override
    public ScanResult scanConfigurations() {
        log.info("Starting SecureCRT configuration scan");
        
        ScanResult.ScanResultBuilder resultBuilder = ScanResult.builder()
            .clientName(CLIENT_NAME)
            .clientVersion(getClientVersion());
        
        // 检查客户端是否已安装
        if (!isInstalled()) {
            log.info("SecureCRT is not installed, skipping scan");
            return resultBuilder
                .status(ScanStatus.NOT_INSTALLED)
                .build();
        }
        
        // Step 1: 尝试从注册表读取配置路径
        String configPath = readConfigPathFromRegistry();
        
        // Step 2: 如果注册表读取失败，扫描默认路径
        if (configPath == null || !PathUtil.pathExists(configPath)) {
            log.debug("Registry config path not found, trying default paths");
            configPath = findConfigPathFromDefaults();
        }
        
        // Step 3: 如果默认路径也失败，从安装目录推断
        if (configPath == null || !PathUtil.pathExists(configPath)) {
            log.debug("Default paths not found, trying to infer from installation directory");
            configPath = inferConfigPathFromInstallation();
        }
        
        // 验证配置路径
        if (configPath == null || !PathUtil.pathExists(configPath)) {
            log.warn("SecureCRT configuration path not found");
            return resultBuilder
                .status(ScanStatus.CONFIG_NOT_FOUND)
                .errorMessage("Configuration directory not found")
                .build();
        }
        
        log.info("SecureCRT configuration path: {}", configPath);
        resultBuilder.configPath(configPath);
        
        // Step 4: 扫描并解析配置文件
        try {
            List<SSHHostConfig> sessions = parseConfigFiles(configPath);
            
            if (sessions.isEmpty()) {
                log.warn("No sessions found in SecureCRT configuration");
                return resultBuilder
                    .status(ScanStatus.CONFIG_NOT_FOUND)
                    .errorMessage("No session files found")
                    .build();
            }
            
            log.info("Successfully parsed {} SecureCRT sessions", sessions.size());
            return resultBuilder
                .status(ScanStatus.SUCCESS)
                .sessions(sessions)
                .sessionCount(sessions.size())
                .build();
            
        } catch (Exception e) {
            log.error("Failed to parse SecureCRT configurations", e);
            return resultBuilder
                .status(ScanStatus.PARSE_ERROR)
                .errorMessage("Failed to parse configuration files: " + e.getMessage())
                .build();
        }
    }
    
    @Override
    public List<String> getDefaultScanPaths() {
        return new ArrayList<>(DEFAULT_PATHS);
    }
    
    /**
     * Step 1: 从 Windows 注册表读取配置路径
     */
    private String readConfigPathFromRegistry() {
        if (!WindowsRegistryUtil.isWindows()) {
            return null;
        }
        
        try {
            String configPath = WindowsRegistryUtil.readRegistryValue(
                REGISTRY_ROOT,
                REGISTRY_KEY,
                "Config Path"
            );
            
            if (configPath != null && !configPath.isEmpty()) {
                // 配置路径通常指向 Config 目录，需要拼接 Sessions 子目录
                String sessionsPath = configPath + "\\Sessions";
                log.debug("Registry config path: {}", sessionsPath);
                return sessionsPath;
            }
        } catch (Exception e) {
            log.debug("Failed to read config path from registry", e);
        }
        
        return null;
    }
    
    /**
     * Step 2: 从默认路径列表中查找存在的配置路径
     */
    private String findConfigPathFromDefaults() {
        for (String path : DEFAULT_PATHS) {
            String expandedPath = PathUtil.expandEnvironmentVariables(path);
            if (PathUtil.pathExists(expandedPath)) {
                log.debug("Found config path: {}", expandedPath);
                return expandedPath;
            }
        }
        return null;
    }
    
    /**
     * Step 3: 从安装目录推断配置路径
     */
    private String inferConfigPathFromInstallation() {
        if (WindowsRegistryUtil.isWindows()) {
            // 尝试从注册表读取安装路径
            String installPath = WindowsRegistryUtil.readRegistryValue(
                REGISTRY_ROOT,
                REGISTRY_KEY,
                "Install Path"
            );
            
            if (installPath != null && PathUtil.pathExists(installPath)) {
                // 通常配置在 %APPDATA% 而不是安装目录，但可以尝试
                String inferredPath = installPath + "\\Config\\Sessions";
                if (PathUtil.pathExists(inferredPath)) {
                    log.debug("Inferred config path from installation: {}", inferredPath);
                    return inferredPath;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Step 4: 解析配置目录下的所有 .ini 文件
     */
    private List<SSHHostConfig> parseConfigFiles(String configPath) throws Exception {
        List<SSHHostConfig> allSessions = new ArrayList<>();
        
        // 查找所有 .ini 文件
        List<String> iniFiles = PathUtil.findFilesWithExtension(configPath, ".ini");
        
        if (iniFiles.isEmpty()) {
            log.warn("No .ini files found in {}", configPath);
            return allSessions;
        }
        
        log.info("Found {} .ini files in SecureCRT configuration", iniFiles.size());
        
        // 解析每个 .ini 文件
        for (String iniFilePath : iniFiles) {
            try {
                File iniFile = new File(iniFilePath);
                List<SSHHostConfig> sessions = iniParser.parse(iniFile);
                
                if (sessions != null && !sessions.isEmpty()) {
                    allSessions.addAll(sessions);
                    log.debug("Parsed {} sessions from {}", sessions.size(), iniFile.getName());
                }
                
            } catch (Exception e) {
                log.error("Failed to parse file: {}", iniFilePath, e);
                // 继续处理其他文件
            }
        }
        
        return allSessions;
    }
    
    /**
     * 检查是否为 macOS 系统
     */
    private boolean isMacOS() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("mac");
    }
    
    /**
     * 检查是否为 Linux 系统
     */
    private boolean isLinux() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("linux");
    }
}
