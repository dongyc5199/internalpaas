package com.cmict.internalpaas.util;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Windows注册表工具类
 * 
 * 使用 JNA (Java Native Access) 读取 Windows 注册表信息，
 * 用于检测已安装的 SSH 客户端（SecureCRT、Xshell 等）。
 * 
 * 支持的注册表根键：
 * - HKEY_CURRENT_USER (HKCU)
 * - HKEY_LOCAL_MACHINE (HKLM)
 * - HKEY_CLASSES_ROOT (HKCR)
 * 
 * @author InternalPaaS Team
 * @since 2025-10-19 (Phase 1 - Multi-Client Scanner)
 */
@Slf4j
public class WindowsRegistryUtil {
    
    /**
     * 读取注册表字符串值
     * 
     * @param root 注册表根键 (e.g., "HKEY_CURRENT_USER", "HKEY_LOCAL_MACHINE")
     * @param key 注册表键路径 (e.g., "Software\\VanDyke\\SecureCRT")
     * @param valueName 值名称 (e.g., "Version", "InstallPath")
     * @return 注册表值，如果不存在或读取失败则返回 null
     */
    public static String readRegistryValue(String root, String key, String valueName) {
        if (!isWindows()) {
            log.debug("Not running on Windows, skipping registry read");
            return null;
        }
        
        try {
            WinReg.HKEY rootKey = parseRootKey(root);
            if (rootKey == null) {
                log.warn("Unknown registry root key: {}", root);
                return null;
            }
            
            if (!Advapi32Util.registryKeyExists(rootKey, key)) {
                log.debug("Registry key does not exist: {}\\{}", root, key);
                return null;
            }
            
            if (!Advapi32Util.registryValueExists(rootKey, key, valueName)) {
                log.debug("Registry value does not exist: {}\\{}\\{}", root, key, valueName);
                return null;
            }
            
            String value = Advapi32Util.registryGetStringValue(rootKey, key, valueName);
            log.debug("Read registry value: {}\\{}\\{} = {}", root, key, valueName, value);
            return value;
            
        } catch (Exception e) {
            log.error("Failed to read registry: {}\\{}\\{}", root, key, valueName, e);
            return null;
        }
    }
    
    /**
     * 检查注册表键是否存在
     * 
     * @param root 注册表根键
     * @param key 注册表键路径
     * @return true 表示键存在，false 表示不存在
     */
    public static boolean registryKeyExists(String root, String key) {
        if (!isWindows()) {
            return false;
        }
        
        try {
            WinReg.HKEY rootKey = parseRootKey(root);
            if (rootKey == null) {
                return false;
            }
            
            boolean exists = Advapi32Util.registryKeyExists(rootKey, key);
            log.debug("Registry key {} exists: {}", root + "\\" + key, exists);
            return exists;
            
        } catch (Exception e) {
            log.error("Failed to check registry key existence: {}\\{}", root, key, e);
            return false;
        }
    }
    
    /**
     * 列出注册表键的所有子键名称
     * 
     * @param root 注册表根键
     * @param key 注册表键路径
     * @return 子键名称列表，如果失败则返回空列表
     */
    public static List<String> listRegistrySubKeys(String root, String key) {
        if (!isWindows()) {
            return new ArrayList<>();
        }
        
        try {
            WinReg.HKEY rootKey = parseRootKey(root);
            if (rootKey == null || !Advapi32Util.registryKeyExists(rootKey, key)) {
                return new ArrayList<>();
            }
            
            String[] subKeys = Advapi32Util.registryGetKeys(rootKey, key);
            List<String> result = new ArrayList<>();
            if (subKeys != null) {
                for (String subKey : subKeys) {
                    result.add(subKey);
                }
            }
            
            log.debug("Found {} sub-keys under {}\\{}", result.size(), root, key);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to list registry sub-keys: {}\\{}", root, key, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 解析注册表根键字符串
     */
    private static WinReg.HKEY parseRootKey(String root) {
        if (root == null) {
            return null;
        }
        
        return switch (root.toUpperCase()) {
            case "HKEY_CURRENT_USER", "HKCU" -> WinReg.HKEY_CURRENT_USER;
            case "HKEY_LOCAL_MACHINE", "HKLM" -> WinReg.HKEY_LOCAL_MACHINE;
            case "HKEY_CLASSES_ROOT", "HKCR" -> WinReg.HKEY_CLASSES_ROOT;
            case "HKEY_USERS", "HKU" -> WinReg.HKEY_USERS;
            case "HKEY_CURRENT_CONFIG", "HKCC" -> WinReg.HKEY_CURRENT_CONFIG;
            default -> null;
        };
    }
    
    /**
     * 检查当前操作系统是否为 Windows
     */
    public static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("windows");
    }
}
