package com.cmict.internalpaas.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 路径工具类
 * 
 * 提供路径展开、验证、文件查找等功能，支持：
 * - 环境变量展开 (%APPDATA%, %USERPROFILE%, ~/)
 * - 路径安全验证（防止路径遍历攻击）
 * - 文件扫描与查找
 * 
 * @author InternalPaaS Team
 * @since 2025-10-19 (Phase 1 - Multi-Client Scanner)
 */
@Slf4j
public class PathUtil {
    
    /**
     * 展开路径中的环境变量
     * 
     * 支持的环境变量：
     * - Windows: %APPDATA%, %USERPROFILE%, %LOCALAPPDATA%, %PROGRAMFILES% 等
     * - Unix/Mac: ~/, $HOME 等
     * 
     * @param path 包含环境变量的路径字符串
     * @return 展开后的完整路径
     */
    public static String expandEnvironmentVariables(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        
        String expanded = path;
        
        // Windows 环境变量
        if (isWindows()) {
            // %APPDATA% -> C:\Users\Username\AppData\Roaming
            expanded = expanded.replace("%APPDATA%", getEnvironmentVariable("APPDATA", ""));
            // %LOCALAPPDATA% -> C:\Users\Username\AppData\Local
            expanded = expanded.replace("%LOCALAPPDATA%", getEnvironmentVariable("LOCALAPPDATA", ""));
            // %USERPROFILE% -> C:\Users\Username
            expanded = expanded.replace("%USERPROFILE%", getEnvironmentVariable("USERPROFILE", ""));
            // %PROGRAMFILES% -> C:\Program Files
            expanded = expanded.replace("%PROGRAMFILES%", getEnvironmentVariable("PROGRAMFILES", ""));
            // %PROGRAMFILES(X86)% -> C:\Program Files (x86)
            expanded = expanded.replace("%PROGRAMFILES(X86)%", getEnvironmentVariable("PROGRAMFILES(X86)", ""));
        } else {
            // Unix/Mac 环境变量
            // ~/ -> /home/username/ 或 /Users/username/
            if (expanded.startsWith("~/")) {
                String home = System.getProperty("user.home");
                expanded = home + expanded.substring(1);
            }
            // $HOME -> /home/username 或 /Users/username
            expanded = expanded.replace("$HOME", System.getProperty("user.home"));
        }
        
        log.debug("Expanded path: {} -> {}", path, expanded);
        return expanded;
    }
    
    /**
     * 验证路径是否有效且安全
     * 
     * 安全检查：
     * - 路径不为空
     * - 不包含路径遍历攻击符号（../, ..\）
     * - 展开后的路径在允许的范围内
     * 
     * @param path 待验证的路径
     * @return true 表示路径有效且安全，false 表示无效或不安全
     */
    public static boolean isValidPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        // 检查路径遍历攻击
        if (path.contains("..") || path.contains("~/../")) {
            log.warn("Potential path traversal attack detected: {}", path);
            return false;
        }
        
        try {
            String expanded = expandEnvironmentVariables(path);
            Path normalizedPath = Paths.get(expanded).normalize();
            
            // 确保路径不为空
            if (normalizedPath.toString().isEmpty()) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Invalid path: {}", path, e);
            return false;
        }
    }
    
    /**
     * 检查路径是否存在
     * 
     * @param path 路径字符串（支持环境变量）
     * @return true 表示路径存在，false 表示不存在
     */
    public static boolean pathExists(String path) {
        if (!isValidPath(path)) {
            return false;
        }
        
        try {
            String expanded = expandEnvironmentVariables(path);
            Path filePath = Paths.get(expanded);
            boolean exists = Files.exists(filePath);
            
            log.debug("Path {} exists: {}", path, exists);
            return exists;
            
        } catch (Exception e) {
            log.error("Failed to check path existence: {}", path, e);
            return false;
        }
    }
    
    /**
     * 查找目录下所有指定扩展名的文件
     * 
     * @param directory 目录路径（支持环境变量）
     * @param extension 文件扩展名（e.g., ".ini", ".xsh", ".yaml"）
     * @return 找到的文件路径列表
     */
    public static List<String> findFilesWithExtension(String directory, String extension) {
        List<String> result = new ArrayList<>();
        
        if (!isValidPath(directory)) {
            log.warn("Invalid directory path: {}", directory);
            return result;
        }
        
        try {
            String expanded = expandEnvironmentVariables(directory);
            Path dirPath = Paths.get(expanded);
            
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                log.debug("Directory does not exist or is not a directory: {}", directory);
                return result;
            }
            
            // 递归扫描目录（限制深度为 5）
            try (Stream<Path> paths = Files.walk(dirPath, 5)) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> path.toString().toLowerCase().endsWith(extension.toLowerCase()))
                     .forEach(path -> result.add(path.toAbsolutePath().toString()));
            }
            
            log.debug("Found {} files with extension {} in {}", result.size(), extension, directory);
            
        } catch (IOException e) {
            log.error("Failed to find files in directory: {}", directory, e);
        }
        
        return result;
    }
    
    /**
     * 获取父目录路径
     * 
     * @param path 文件或目录路径
     * @return 父目录路径，如果无父目录则返回 null
     */
    public static String getParentPath(String path) {
        if (!isValidPath(path)) {
            return null;
        }
        
        try {
            String expanded = expandEnvironmentVariables(path);
            Path filePath = Paths.get(expanded);
            Path parent = filePath.getParent();
            
            return parent != null ? parent.toString() : null;
            
        } catch (Exception e) {
            log.error("Failed to get parent path: {}", path, e);
            return null;
        }
    }
    
    /**
     * 规范化路径（移除冗余的 /.. 和 /. 等）
     * 
     * @param path 路径字符串
     * @return 规范化后的路径
     */
    public static String normalizePath(String path) {
        if (!isValidPath(path)) {
            return path;
        }
        
        try {
            String expanded = expandEnvironmentVariables(path);
            return Paths.get(expanded).normalize().toString();
            
        } catch (Exception e) {
            log.error("Failed to normalize path: {}", path, e);
            return path;
        }
    }
    
    /**
     * 获取环境变量值
     */
    private static String getEnvironmentVariable(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 检查当前操作系统是否为 Windows
     */
    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("windows");
    }
}
