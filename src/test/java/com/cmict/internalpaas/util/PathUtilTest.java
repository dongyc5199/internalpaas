package com.cmict.internalpaas.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PathUtil 单元测试
 * 
 * @author InternalPaaS Team
 * @since 2025-10-19 (Phase 1 - Multi-Client Scanner)
 */
class PathUtilTest {
    
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testExpandEnvironmentVariables_Windows() {
        // Windows 环境变量展开
        String path1 = "%APPDATA%\\test";
        String expanded1 = PathUtil.expandEnvironmentVariables(path1);
        assertNotNull(expanded1);
        assertFalse(expanded1.contains("%APPDATA%"));
        
        String path2 = "%USERPROFILE%\\Documents\\test";
        String expanded2 = PathUtil.expandEnvironmentVariables(path2);
        assertNotNull(expanded2);
        assertFalse(expanded2.contains("%USERPROFILE%"));
    }
    
    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testExpandEnvironmentVariables_Unix() {
        // Unix/Mac 环境变量展开
        String path1 = "~/Documents/test";
        String expanded1 = PathUtil.expandEnvironmentVariables(path1);
        assertNotNull(expanded1);
        assertFalse(expanded1.startsWith("~/"));
        assertTrue(expanded1.contains("Documents/test"));
        
        String path2 = "$HOME/.config/test";
        String expanded2 = PathUtil.expandEnvironmentVariables(path2);
        assertNotNull(expanded2);
        assertFalse(expanded2.contains("$HOME"));
    }
    
    @Test
    void testIsValidPath() {
        // 有效路径
        assertTrue(PathUtil.isValidPath("/home/user/test"));
        assertTrue(PathUtil.isValidPath("C:\\Users\\test"));
        assertTrue(PathUtil.isValidPath("~/Documents"));
        
        // 无效路径
        assertFalse(PathUtil.isValidPath(null));
        assertFalse(PathUtil.isValidPath(""));
        assertFalse(PathUtil.isValidPath("   "));
        
        // 路径遍历攻击
        assertFalse(PathUtil.isValidPath("../../etc/passwd"));
        assertFalse(PathUtil.isValidPath("~/../../../etc/passwd"));
    }
    
    @Test
    void testPathExists() {
        // 测试当前工作目录（应该存在）
        String currentDir = System.getProperty("user.dir");
        assertTrue(PathUtil.pathExists(currentDir));
        
        // 测试用户主目录（应该存在）
        String homeDir = System.getProperty("user.home");
        assertTrue(PathUtil.pathExists(homeDir));
        
        // 测试不存在的路径
        assertFalse(PathUtil.pathExists("/this/path/should/not/exist/12345"));
    }
    
    @Test
    void testNormalizePath() {
        String path1 = PathUtil.normalizePath("/home/user/./test");
        assertFalse(path1.contains("/./")); // 应该移除 /./ 
        
        String path2 = PathUtil.normalizePath("C:\\Users\\test\\..\\public");
        // 规范化后应该简化路径
        assertNotNull(path2);
    }
    
    @Test
    void testGetParentPath() {
        String parent1 = PathUtil.getParentPath("/home/user/documents/test.txt");
        assertNotNull(parent1);
        assertTrue(parent1.endsWith("documents") || parent1.endsWith("documents\\"));
        
        String parent2 = PathUtil.getParentPath("C:\\Users\\test\\file.txt");
        assertNotNull(parent2);
    }
    
    @Test
    void testFindFilesWithExtension() {
        // 测试在当前项目目录查找 .md 文件
        String projectDir = System.getProperty("user.dir");
        var mdFiles = PathUtil.findFilesWithExtension(projectDir, ".md");
        
        // 项目中应该有 README.md 等文件
        assertNotNull(mdFiles);
        assertFalse(mdFiles.isEmpty(), "Should find at least one .md file in project");
    }
}
