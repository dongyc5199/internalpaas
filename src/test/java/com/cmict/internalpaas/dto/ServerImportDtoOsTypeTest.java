package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ServerImportDto操作系统类型测试
 * Test cases for ServerImportDto osType field
 *
 * @author GitHub Copilot
 * @since 2025-10-20
 */
@DisplayName("ServerImportDto OsType 字段测试")
class ServerImportDtoOsTypeTest {

    @Test
    @DisplayName("测试ServerImportDto默认osType值")
    void testDefaultOsType() {
        ServerImportDto dto = new ServerImportDto();
        
        assertNotNull(dto.getOsType(), "osType不应为null");
        assertEquals(Server.OsType.LINUX, dto.getOsType(), "默认值应为LINUX");
    }

    @Test
    @DisplayName("测试ServerImportDto osType setter/getter")
    void testOsTypeSetterGetter() {
        ServerImportDto dto = new ServerImportDto();
        
        // 测试所有操作系统类型
        dto.setOsType(Server.OsType.WINDOWS);
        assertEquals(Server.OsType.WINDOWS, dto.getOsType());
        
        dto.setOsType(Server.OsType.MACOS);
        assertEquals(Server.OsType.MACOS, dto.getOsType());
        
        dto.setOsType(Server.OsType.UNIX);
        assertEquals(Server.OsType.UNIX, dto.getOsType());
        
        dto.setOsType(Server.OsType.BSD);
        assertEquals(Server.OsType.BSD, dto.getOsType());
        
        dto.setOsType(Server.OsType.OTHER);
        assertEquals(Server.OsType.OTHER, dto.getOsType());
    }

    @Test
    @DisplayName("测试完整的ServerImportDto创建（包含osType）")
    void testCompleteServerImportDto() {
        ServerImportDto dto = new ServerImportDto();
        
        // 设置基本信息
        dto.setName("test-server");
        dto.setHostname("192.168.1.100");
        dto.setSshPort(22);
        dto.setSshUsername("root");
        dto.setSshPassword("password");
        dto.setPort(8080);
        dto.setBaseWorkDirectory("/root");
        dto.setDescription("测试服务器");
        dto.setServerType(Server.ServerType.DEVELOPMENT);
        dto.setOsType(Server.OsType.LINUX);
        
        // 验证所有字段
        assertEquals("test-server", dto.getName());
        assertEquals("192.168.1.100", dto.getHostname());
        assertEquals(22, dto.getSshPort());
        assertEquals("root", dto.getSshUsername());
        assertEquals(Server.ServerType.DEVELOPMENT, dto.getServerType());
        assertEquals(Server.OsType.LINUX, dto.getOsType());
    }

    @Test
    @DisplayName("测试不同操作系统类型的组合")
    void testDifferentOsTypeCombinations() {
        // Windows + Production
        ServerImportDto dto1 = new ServerImportDto();
        dto1.setServerType(Server.ServerType.PRODUCTION);
        dto1.setOsType(Server.OsType.WINDOWS);
        assertEquals(Server.ServerType.PRODUCTION, dto1.getServerType());
        assertEquals(Server.OsType.WINDOWS, dto1.getOsType());
        
        // macOS + Development
        ServerImportDto dto2 = new ServerImportDto();
        dto2.setServerType(Server.ServerType.DEVELOPMENT);
        dto2.setOsType(Server.OsType.MACOS);
        assertEquals(Server.ServerType.DEVELOPMENT, dto2.getServerType());
        assertEquals(Server.OsType.MACOS, dto2.getOsType());
        
        // Unix + Testing
        ServerImportDto dto3 = new ServerImportDto();
        dto3.setServerType(Server.ServerType.TESTING);
        dto3.setOsType(Server.OsType.UNIX);
        assertEquals(Server.ServerType.TESTING, dto3.getServerType());
        assertEquals(Server.OsType.UNIX, dto3.getOsType());
    }

    @Test
    @DisplayName("测试osType为null时的处理")
    void testNullOsType() {
        ServerImportDto dto = new ServerImportDto();
        
        // 设置为null
        dto.setOsType(null);
        assertNull(dto.getOsType(), "设置为null后应该为null");
        
        // 重新设置默认值
        dto.setOsType(Server.OsType.LINUX);
        assertNotNull(dto.getOsType());
        assertEquals(Server.OsType.LINUX, dto.getOsType());
    }
}
