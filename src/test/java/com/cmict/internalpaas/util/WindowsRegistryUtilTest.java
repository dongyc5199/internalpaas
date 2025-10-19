package com.cmict.internalpaas.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WindowsRegistryUtil 单元测试
 * 
 * 注意：这些测试只在 Windows 系统上运行
 * 
 * @author InternalPaaS Team
 * @since 2025-10-19 (Phase 1 - Multi-Client Scanner)
 */
class WindowsRegistryUtilTest {
    
    @Test
    void testIsWindows() {
        // 这个测试在所有系统上都应该返回正确的结果
        String os = System.getProperty("os.name").toLowerCase();
        assertEquals(os.contains("windows"), WindowsRegistryUtil.isWindows());
    }
    
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testReadRegistryValue_SystemInfo() {
        // 读取 Windows 版本信息（应该存在）
        String version = WindowsRegistryUtil.readRegistryValue(
            "HKEY_LOCAL_MACHINE",
            "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
            "ProductName"
        );
        
        assertNotNull(version, "Should be able to read Windows version");
        assertTrue(version.contains("Windows"), "Product name should contain 'Windows'");
    }
    
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testReadRegistryValue_NotExist() {
        // 读取不存在的注册表项
        String value = WindowsRegistryUtil.readRegistryValue(
            "HKEY_CURRENT_USER",
            "Software\\NonExistentKey12345",
            "NonExistentValue"
        );
        
        assertNull(value, "Non-existent registry value should return null");
    }
    
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testRegistryKeyExists() {
        // 测试已知存在的注册表键
        boolean exists = WindowsRegistryUtil.registryKeyExists(
            "HKEY_LOCAL_MACHINE",
            "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion"
        );
        
        assertTrue(exists, "Windows version registry key should exist");
        
        // 测试不存在的注册表键
        boolean notExists = WindowsRegistryUtil.registryKeyExists(
            "HKEY_CURRENT_USER",
            "Software\\NonExistentKey12345"
        );
        
        assertFalse(notExists, "Non-existent registry key should return false");
    }
    
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testListRegistrySubKeys() {
        // 列出 Windows 版本键的子键
        List<String> subKeys = WindowsRegistryUtil.listRegistrySubKeys(
            "HKEY_LOCAL_MACHINE",
            "SOFTWARE\\Microsoft\\Windows NT"
        );
        
        assertNotNull(subKeys);
        assertFalse(subKeys.isEmpty(), "Should have sub-keys under Windows NT");
        assertTrue(subKeys.contains("CurrentVersion"), "Should contain CurrentVersion sub-key");
    }
    
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testListRegistrySubKeys_NotExist() {
        // 列出不存在的键的子键
        List<String> subKeys = WindowsRegistryUtil.listRegistrySubKeys(
            "HKEY_CURRENT_USER",
            "Software\\NonExistentKey12345"
        );
        
        assertNotNull(subKeys);
        assertTrue(subKeys.isEmpty(), "Non-existent key should return empty list");
    }
    
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testReadRegistryValue_HKCUAbbreviation() {
        // 测试缩写形式 (HKCU 而不是 HKEY_CURRENT_USER)
        String value1 = WindowsRegistryUtil.readRegistryValue(
            "HKCU",
            "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer",
            "ShellState"
        );
        
        String value2 = WindowsRegistryUtil.readRegistryValue(
            "HKEY_CURRENT_USER",
            "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer",
            "ShellState"
        );
        
        // 两种形式应该返回相同的结果
        assertEquals(value1, value2, "HKCU and HKEY_CURRENT_USER should return same result");
    }
}
