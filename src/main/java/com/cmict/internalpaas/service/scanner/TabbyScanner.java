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
 * Tabby 终端扫描器
 * 
 * <p>Tabby（前身为 Terminus）是一款现代化的开源终端仿真器，支持多种协议和跨平台使用。
 * 它使用 YAML 格式存储配置信息，包括 SSH 连接配置。</p>
 * 
 * <h3>检测方式</h3>
 * <ul>
 *   <li><b>Windows</b>: 检查应用目录 {@code %LOCALAPPDATA%\Programs\Tabby\}
 *       <br>或检查快捷方式 {@code %APPDATA%\Microsoft\Windows\Start Menu\Programs\Tabby.lnk}</li>
 *   <li><b>macOS</b>: 检查 {@code /Applications/Tabby.app}</li>
 *   <li><b>Linux</b>: 检查 {@code ~/.local/share/applications/} 或 {@code /usr/share/applications/}</li>
 * </ul>
 * 
 * <h3>配置文件位置</h3>
 * <ul>
 *   <li>Windows: {@code %APPDATA%\tabby\config.yaml}</li>
 *   <li>macOS: {@code ~/Library/Application Support/tabby/config.yaml}</li>
 *   <li>Linux: {@code ~/.config/tabby/config.yaml}</li>
 * </ul>
 * 
 * <h3>配置文件格式（YAML）</h3>
 * <pre>{@code
 * profiles:
 *   - type: ssh
 *     name: Production Server
 *     options:
 *       host: 192.168.1.100
 *       port: 22
 *       user: admin
 *       privateKey: /path/to/key
 *     group: Production
 * }</pre>
 * 
 * <h3>扫描流程（3步）</h3>
 * <ol>
 *   <li><b>Step 1</b>: 扫描默认配置路径（最常见）</li>
 *   <li><b>Step 2</b>: 从安装目录推断配置路径（容错机制）</li>
 *   <li><b>Step 3</b>: 解析 config.yaml 文件</li>
 * </ol>
 * 
 * @author System
 * @since 2025-10-19
 */
@Service
public class TabbyScanner implements ClientScanner {
    
    private static final Logger log = LoggerFactory.getLogger(TabbyScanner.class);
    
    /**
     * 默认扫描路径列表
     */
    private static final List<String> DEFAULT_PATHS = Arrays.asList(
        // Windows
        "%APPDATA%\\tabby\\config.yaml",
        // macOS
        "~/Library/Application Support/tabby/config.yaml",
        // Linux
        "~/.config/tabby/config.yaml"
    );
    
    /**
     * Tabby 安装位置检测路径
     */
    private static final List<String> INSTALLATION_PATHS = Arrays.asList(
        // Windows
        "%LOCALAPPDATA%\\Programs\\Tabby\\Tabby.exe",
        "%PROGRAMFILES%\\Tabby\\Tabby.exe",
        // macOS
        "/Applications/Tabby.app",
        // Linux
        "/usr/bin/tabby",
        "/usr/local/bin/tabby"
    );
    
    /**
     * YAML 解析器
     */
    private final ConfigParser<File> yamlParser;
    
    /**
     * 构造函数（依赖注入）
     *
     * @param yamlParser YAML 格式配置解析器
     */
    @Autowired
    public TabbyScanner(@Qualifier("tabbyYamlParser") ConfigParser<File> yamlParser) {
        this.yamlParser = yamlParser;
    }
    
    /**
     * 获取客户端名称
     *
     * @return "Tabby"
     */
    @Override
    public String getClientName() {
        return "Tabby";
    }
    
    /**
     * 检测 Tabby 是否已安装
     * 
     * <p>检测逻辑：</p>
     * <ol>
     *   <li>遍历安装路径列表</li>
     *   <li>检查可执行文件或应用包是否存在</li>
     *   <li>任一路径存在即返回 true</li>
     * </ol>
     *
     * @return 如果检测到 Tabby 已安装返回 true，否则返回 false
     */
    @Override
    public boolean isInstalled() {
        log.debug("Checking if Tabby is installed");
        
        for (String path : INSTALLATION_PATHS) {
            String expandedPath = PathUtil.expandEnvironmentVariables(path);
            
            if (PathUtil.pathExists(expandedPath)) {
                log.info("Found Tabby installation at: {}", expandedPath);
                return true;
            }
        }
        
        log.debug("Tabby is not installed (no installation paths found)");
        return false;
    }
    
    /**
     * 获取 Tabby 客户端版本号
     * 
     * <p>由于 Tabby 配置文件中不包含版本信息，且跨平台版本检测复杂，
     * 本实现返回 "Unknown"。后续可通过执行 {@code tabby --version} 命令获取。</p>
     *
     * @return 版本号字符串（当前实现返回 "Unknown"）
     */
    @Override
    public String getClientVersion() {
        // Tabby 配置文件中不包含版本信息
        // 可以通过执行 `tabby --version` 命令获取，但这超出当前范围
        log.debug("Tabby version detection not implemented, returning 'Unknown'");
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
     * 扫描 Tabby 配置文件并解析 SSH 会话
     * 
     * <p>扫描流程（3步）：</p>
     * <ol>
     *   <li><b>Step 1</b>: 扫描默认配置路径
     *       <br>路径: {@code %APPDATA%\tabby\config.yaml} 等</li>
     *   <li><b>Step 2</b>: 从安装目录推断配置路径（如果 Step 1 失败）
     *       <br>构造可能的配置路径</li>
     *   <li><b>Step 3</b>: 解析配置文件
     *       <br>调用 YAML 解析器解析 config.yaml</li>
     * </ol>
     *
     * @return 扫描结果对象，包含状态、会话列表、警告信息等
     */
    @Override
    public ScanResult scanConfigurations() {
        log.info("Starting Tabby configuration scan");
        
        ScanResult.ScanResultBuilder resultBuilder = ScanResult.builder()
            .clientName("Tabby")
            .clientVersion(getClientVersion());
        
        try {
            // 检查是否已安装
            if (!isInstalled()) {
                log.warn("Tabby is not installed");
                return resultBuilder
                    .status(ScanStatus.NOT_INSTALLED)
                    .build();
            }
            
            // Step 1: 扫描默认配置路径
            String configPath = findConfigPathFromDefaults();
            
            // Step 2: 如果默认路径失败，从安装目录推断
            if (configPath == null) {
                log.debug("Default config path not found, inferring from installation path");
                configPath = inferConfigPathFromInstallation();
            }
            
            // 如果所有方法都失败
            if (configPath == null) {
                log.warn("Tabby configuration file not found");
                return resultBuilder
                    .status(ScanStatus.CONFIG_NOT_FOUND)
                    .build();
            }
            
            resultBuilder.configPath(configPath);
            
            // Step 3: 解析配置文件
            List<SSHHostConfig> sessions = parseConfigFile(configPath);
            
            if (sessions.isEmpty()) {
                log.info("No SSH sessions found in Tabby configuration");
                return resultBuilder
                    .status(ScanStatus.SUCCESS)
                    .sessionCount(0)
                    .build();
            }
            
            log.info("Successfully scanned {} Tabby sessions", sessions.size());
            return resultBuilder
                .status(ScanStatus.SUCCESS)
                .sessionCount(sessions.size())
                .sessions(sessions)
                .build();
            
        } catch (Exception e) {
            log.error("Failed to parse Tabby configurations", e);
            return resultBuilder
                .status(ScanStatus.PARSE_ERROR)
                .errorMessage(e.getMessage())
                .build();
        }
    }
    
    /**
     * Step 1: 从默认路径列表中查找配置文件
     *
     * @return 找到的配置文件路径，如果所有默认路径都不存在返回 null
     */
    private String findConfigPathFromDefaults() {
        for (String path : DEFAULT_PATHS) {
            String expandedPath = PathUtil.expandEnvironmentVariables(path);
            
            if (PathUtil.pathExists(expandedPath)) {
                log.debug("Found config file at default path: {}", expandedPath);
                return expandedPath;
            }
        }
        
        return null;
    }
    
    /**
     * Step 2: 从安装目录推断配置路径
     * 
     * <p>推断逻辑：</p>
     * <ol>
     *   <li>查找 Tabby 安装目录</li>
     *   <li>根据操作系统和安装路径构造可能的配置路径</li>
     *   <li>验证推断的路径是否存在</li>
     * </ol>
     *
     * @return 推断的配置文件路径，如果推断失败返回 null
     */
    private String inferConfigPathFromInstallation() {
        // 查找安装目录
        String installPath = null;
        for (String path : INSTALLATION_PATHS) {
            String expandedPath = PathUtil.expandEnvironmentVariables(path);
            if (PathUtil.pathExists(expandedPath)) {
                installPath = expandedPath;
                break;
            }
        }
        
        if (installPath == null) {
            return null;
        }
        
        // 根据操作系统推断配置路径
        List<String> possiblePaths = new ArrayList<>();
        
        if (WindowsRegistryUtil.isWindows()) {
            // Windows: %APPDATA%\tabby\config.yaml
            possiblePaths.add(PathUtil.expandEnvironmentVariables("%APPDATA%\\tabby\\config.yaml"));
        } else if (isMacOS()) {
            // macOS: ~/Library/Application Support/tabby/config.yaml
            possiblePaths.add(PathUtil.expandEnvironmentVariables("~/Library/Application Support/tabby/config.yaml"));
        } else if (isLinux()) {
            // Linux: ~/.config/tabby/config.yaml
            possiblePaths.add(PathUtil.expandEnvironmentVariables("~/.config/tabby/config.yaml"));
        }
        
        // 验证推断的路径
        for (String inferredPath : possiblePaths) {
            if (PathUtil.pathExists(inferredPath)) {
                log.debug("Inferred config path from installation: {}", inferredPath);
                return inferredPath;
            }
        }
        
        return null;
    }
    
    /**
     * Step 3: 解析配置文件
     * 
     * <p>解析逻辑：</p>
     * <ol>
     *   <li>调用 YAML 解析器解析 config.yaml 文件</li>
     *   <li>返回解析结果（SSH 会话列表）</li>
     * </ol>
     *
     * @param configPath 配置文件路径
     * @return SSH 会话配置列表
     */
    private List<SSHHostConfig> parseConfigFile(String configPath) {
        try {
            File configFile = new File(configPath);
            List<SSHHostConfig> sessions = yamlParser.parse(configFile);
            
            log.debug("Parsed {} sessions from {}", sessions.size(), configPath);
            return sessions;
            
        } catch (Exception e) {
            log.error("Failed to parse Tabby config file: {}", configPath, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 检测是否为 macOS 平台
     *
     * @return 如果是 macOS 返回 true
     */
    private boolean isMacOS() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac") || os.contains("darwin");
    }
    
    /**
     * 检测是否为 Linux 平台
     *
     * @return 如果是 Linux 返回 true
     */
    private boolean isLinux() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("linux") || os.contains("unix");
    }
}
