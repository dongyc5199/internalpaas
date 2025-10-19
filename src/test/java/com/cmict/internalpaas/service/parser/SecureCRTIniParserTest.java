package com.cmict.internalpaas.service.parser;

import com.cmict.internalpaas.dto.SSHHostConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecureCRTIniParser 单元测试
 * 
 * @author InternalPaaS Team
 * @since 2025-10-19 (Phase 2 - SecureCRT Scanner)
 */
class SecureCRTIniParserTest {
    
    private SecureCRTIniParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new SecureCRTIniParser();
    }
    
    @Test
    void testSupports() {
        assertTrue(parser.supports(".ini"));
        assertTrue(parser.supports(".INI"));
        assertFalse(parser.supports(".xsh"));
        assertFalse(parser.supports(".yaml"));
        assertFalse(parser.supports(null));
    }
    
    @Test
    void testGetParserName() {
        assertEquals("SecureCRT INI Parser", parser.getParserName());
    }
    
    @Test
    void testParse_ProductionServer() throws Exception {
        // 加载测试文件
        File testFile = getTestFile("prod-db-server.ini");
        
        // 解析
        List<SSHHostConfig> configs = parser.parse(testFile);
        
        // 验证
        assertNotNull(configs);
        assertEquals(1, configs.size());
        
        SSHHostConfig config = configs.get(0);
        assertEquals("prod-db-server", config.getHostPattern());
        assertEquals("192.168.1.100", config.getHostname());
        assertEquals(22, config.getEffectivePort()); // 0x16 = 22
        assertEquals("admin", config.getUser());
        assertEquals("C:\\Users\\admin\\.ssh\\id_rsa_prod", config.getIdentityFile());
        assertEquals("Production Database Server", config.getDescription());
        assertEquals("Production/Databases", config.getGroup());
        assertTrue(config.hasIdentityFile());
    }
    
    @Test
    void testParse_DevelopmentServer() throws Exception {
        // 加载测试文件
        File testFile = getTestFile("dev-server-01.ini");
        
        // 解析
        List<SSHHostConfig> configs = parser.parse(testFile);
        
        // 验证
        assertNotNull(configs);
        assertEquals(1, configs.size());
        
        SSHHostConfig config = configs.get(0);
        assertEquals("dev-server-01", config.getHostPattern());
        assertEquals("dev-server-01.example.com", config.getHostname());
        assertEquals(23, config.getEffectivePort()); // 0x17 = 23
        assertEquals("developer", config.getUser());
        assertEquals("Development Server 01", config.getDescription());
        assertEquals("Development", config.getGroup());
        assertFalse(config.hasIdentityFile());
    }
    
    @Test
    void testParse_NonExistentFile() {
        File nonExistent = new File("non-existent-file.ini");
        
        ConfigParser.ParseException exception = assertThrows(
            ConfigParser.ParseException.class,
            () -> parser.parse(nonExistent)
        );
        
        assertTrue(exception.getMessage().contains("Invalid source file"));
    }
    
    @Test
    void testParse_NullFile() {
        ConfigParser.ParseException exception = assertThrows(
            ConfigParser.ParseException.class,
            () -> parser.parse(null)
        );
        
        assertTrue(exception.getMessage().contains("Invalid source file"));
    }
    
    /**
     * 获取测试资源文件
     */
    private File getTestFile(String fileName) {
        URL resource = getClass().getClassLoader().getResource(
            "test-data/securecrt/" + fileName
        );
        assertNotNull(resource, "Test file not found: " + fileName);
        return new File(resource.getFile());
    }
}
