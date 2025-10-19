package com.cmict.internalpaas.service.parser;

import com.cmict.internalpaas.dto.SSHHostConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XshellXmlParser 单元测试
 * 
 * @author System
 * @since 2025-10-19
 */
class XshellXmlParserTest {
    
    private XshellXmlParser parser;
    private File testDataDir;
    
    @BeforeEach
    void setUp() {
        parser = new XshellXmlParser();
        
        // 获取测试数据目录
        String testResourcePath = "src/test/resources/test-data/xshell";
        testDataDir = new File(testResourcePath);
        
        // 确保测试数据目录存在
        assertTrue(testDataDir.exists(), "Test data directory should exist: " + testDataDir.getAbsolutePath());
    }
    
    /**
     * 测试：支持的文件扩展名
     */
    @Test
    void testSupports() {
        assertTrue(parser.supports(".xsh"), "Should support .xsh extension");
        assertTrue(parser.supports(".XSH"), "Should support .XSH extension (case-insensitive)");
        assertFalse(parser.supports(".ini"), "Should not support .ini extension");
        assertFalse(parser.supports(".yaml"), "Should not support .yaml extension");
        assertFalse(parser.supports(null), "Should return false for null extension");
    }
    
    /**
     * 测试：获取解析器名称
     */
    @Test
    void testGetParserName() {
        assertEquals("Xshell XML Parser", parser.getParserName());
    }
    
    /**
     * 测试：解析生产服务器配置（带私钥）
     */
    @Test
    void testParse_ProductionServer() throws ConfigParser.ParseException {
        File xshFile = new File(testDataDir, "prod-db-01.xsh");
        assertTrue(xshFile.exists(), "Test file should exist: " + xshFile.getAbsolutePath());
        
        List<SSHHostConfig> configs = parser.parse(xshFile);
        
        assertNotNull(configs, "Result should not be null");
        assertEquals(1, configs.size(), "Should parse exactly 1 session");
        
        SSHHostConfig config = configs.get(0);
        
        // 验证基础字段
        assertEquals("prod-db-01", config.getHostPattern(), "Host pattern should match title");
        assertEquals("192.168.1.100", config.getHostname(), "Hostname should match");
        assertEquals(22, config.getEffectivePort(), "Port should be 22");
        assertEquals("dbadmin", config.getUser(), "User should match");
        
        // 验证私钥文件
        assertEquals("C:\\Users\\admin\\.ssh\\id_rsa_prod", config.getIdentityFile(), 
            "Identity file path should match");
        assertTrue(config.hasIdentityFile(), "Should have identity file");
        
        // 验证描述和分组
        assertEquals("Production Database Server 01", config.getDescription(), 
            "Description should match");
        assertEquals("Production/Databases", config.getGroup(), "Group should match");
    }
    
    /**
     * 测试：解析开发服务器配置（无私钥，非标准端口）
     */
    @Test
    void testParse_DevelopmentServer() throws ConfigParser.ParseException {
        File xshFile = new File(testDataDir, "dev-api-server.xsh");
        assertTrue(xshFile.exists(), "Test file should exist: " + xshFile.getAbsolutePath());
        
        List<SSHHostConfig> configs = parser.parse(xshFile);
        
        assertNotNull(configs, "Result should not be null");
        assertEquals(1, configs.size(), "Should parse exactly 1 session");
        
        SSHHostConfig config = configs.get(0);
        
        // 验证基础字段
        assertEquals("dev-api-server", config.getHostPattern(), "Host pattern should match title");
        assertEquals("dev-server-01.example.com", config.getHostname(), "Hostname should match");
        assertEquals(2222, config.getEffectivePort(), "Port should be 2222");
        assertEquals("developer", config.getUser(), "User should match");
        
        // 验证无私钥
        assertFalse(config.hasIdentityFile(), "Should not have identity file");
        
        // 验证描述和分组
        assertEquals("Development API Server", config.getDescription(), "Description should match");
        assertEquals("Development/API", config.getGroup(), "Group should match");
    }
    
    /**
     * 测试：解析不存在的文件应抛出异常
     */
    @Test
    void testParse_NonExistentFile() {
        File nonExistent = new File(testDataDir, "non-existent.xsh");
        
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
