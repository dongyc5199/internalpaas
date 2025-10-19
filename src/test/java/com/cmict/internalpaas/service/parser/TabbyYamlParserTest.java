package com.cmict.internalpaas.service.parser;

import com.cmict.internalpaas.dto.SSHHostConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TabbyYamlParser 单元测试
 * 
 * @author System
 * @since 2025-10-19
 */
class TabbyYamlParserTest {
    
    private TabbyYamlParser parser;
    private File testDataDir;
    
    @BeforeEach
    void setUp() {
        parser = new TabbyYamlParser();
        
        // 获取测试数据目录
        String testResourcePath = "src/test/resources/test-data/tabby";
        testDataDir = new File(testResourcePath);
        
        // 确保测试数据目录存在
        assertTrue(testDataDir.exists(), "Test data directory should exist: " + testDataDir.getAbsolutePath());
    }
    
    /**
     * 测试：支持的文件扩展名
     */
    @Test
    void testSupports() {
        assertTrue(parser.supports(".yaml"), "Should support .yaml extension");
        assertTrue(parser.supports(".YAML"), "Should support .YAML extension (case-insensitive)");
        assertTrue(parser.supports(".yml"), "Should support .yml extension");
        assertTrue(parser.supports(".YML"), "Should support .YML extension (case-insensitive)");
        assertFalse(parser.supports(".ini"), "Should not support .ini extension");
        assertFalse(parser.supports(".xsh"), "Should not support .xsh extension");
        assertFalse(parser.supports(null), "Should return false for null extension");
    }
    
    /**
     * 测试：获取解析器名称
     */
    @Test
    void testGetParserName() {
        assertEquals("Tabby YAML Parser", parser.getParserName());
    }
    
    /**
     * 测试：解析 Tabby 配置文件（包含多个会话）
     */
    @Test
    void testParse_MultipleProfiles() throws ConfigParser.ParseException {
        File configFile = new File(testDataDir, "config.yaml");
        assertTrue(configFile.exists(), "Test file should exist: " + configFile.getAbsolutePath());
        
        List<SSHHostConfig> configs = parser.parse(configFile);
        
        assertNotNull(configs, "Result should not be null");
        assertEquals(2, configs.size(), "Should parse exactly 2 SSH sessions (local type filtered out)");
        
        // 验证第一个会话（生产服务器）
        SSHHostConfig config1 = configs.get(0);
        assertEquals("prod-db-server", config1.getHostPattern(), "Host pattern should match name");
        assertEquals("192.168.1.100", config1.getHostname(), "Hostname should match");
        assertEquals(22, config1.getEffectivePort(), "Port should be 22");
        assertEquals("dbadmin", config1.getUser(), "User should match");
        assertEquals("C:\\Users\\admin\\.ssh\\id_rsa_prod", config1.getIdentityFile(), 
            "Identity file path should match");
        assertTrue(config1.hasIdentityFile(), "Should have identity file");
        assertEquals("Production Database Server", config1.getDescription(), "Description should match");
        assertEquals("Production/Databases", config1.getGroup(), "Group should match");
        
        // 验证第二个会话（开发服务器）
        SSHHostConfig config2 = configs.get(1);
        assertEquals("dev-api-server", config2.getHostPattern(), "Host pattern should match name");
        assertEquals("dev-server-01.example.com", config2.getHostname(), "Hostname should match");
        assertEquals(2222, config2.getEffectivePort(), "Port should be 2222");
        assertEquals("developer", config2.getUser(), "User should match");
        assertFalse(config2.hasIdentityFile(), "Should not have identity file");
        assertEquals("Development API Server", config2.getDescription(), "Description should match");
        assertEquals("Development/API", config2.getGroup(), "Group should match");
    }
    
    /**
     * 测试：解析不存在的文件应抛出异常
     */
    @Test
    void testParse_NonExistentFile() {
        File nonExistent = new File(testDataDir, "non-existent.yaml");
        
        assertThrows(ConfigParser.ParseException.class, 
            () -> parser.parse(nonExistent),
            "Should throw ParseException for non-existent file");
    }
    
    /**
     * 测试：解析 null 文件应抛出异常
     */
    @Test
    void testParse_NullFile() {
        assertThrows(ConfigParser.ParseException.class, 
            () -> parser.parse(null),
            "Should throw ParseException for null file");
    }
}
