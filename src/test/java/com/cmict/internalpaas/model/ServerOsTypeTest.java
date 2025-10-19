package com.cmict.internalpaas.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Server操作系统类型枚举测试
 * Test cases for Server OsType enum
 *
 * @author GitHub Copilot
 * @since 2025-10-20
 */
@DisplayName("Server OsType 枚举测试")
class ServerOsTypeTest {

    @Test
    @DisplayName("测试OsType枚举所有值")
    void testOsTypeEnumValues() {
        Server.OsType[] osTypes = Server.OsType.values();
        
        assertEquals(6, osTypes.length, "应该有6个操作系统类型");
        
        // 验证所有枚举值
        assertNotNull(Server.OsType.valueOf("LINUX"));
        assertNotNull(Server.OsType.valueOf("WINDOWS"));
        assertNotNull(Server.OsType.valueOf("MACOS"));
        assertNotNull(Server.OsType.valueOf("UNIX"));
        assertNotNull(Server.OsType.valueOf("BSD"));
        assertNotNull(Server.OsType.valueOf("OTHER"));
    }

    @Test
    @DisplayName("测试OsType描述信息")
    void testOsTypeDescriptions() {
        assertEquals("Linux", Server.OsType.LINUX.getDescription());
        assertEquals("Windows", Server.OsType.WINDOWS.getDescription());
        assertEquals("macOS", Server.OsType.MACOS.getDescription());
        assertEquals("Unix", Server.OsType.UNIX.getDescription());
        assertEquals("BSD", Server.OsType.BSD.getDescription());
        assertEquals("其他", Server.OsType.OTHER.getDescription());
    }

    @Test
    @DisplayName("测试Server实体osType字段默认值")
    void testServerOsTypeDefaultValue() {
        Server server = new Server();
        
        assertNotNull(server.getOsType(), "osType不应为null");
        assertEquals(Server.OsType.LINUX, server.getOsType(), "默认值应为LINUX");
    }

    @Test
    @DisplayName("测试Server实体osType字段setter/getter")
    void testServerOsTypeSetterGetter() {
        Server server = new Server();
        
        // 测试所有操作系统类型
        for (Server.OsType osType : Server.OsType.values()) {
            server.setOsType(osType);
            assertEquals(osType, server.getOsType(), 
                "设置 " + osType + " 后应该能正确获取");
        }
    }

    @Test
    @DisplayName("测试完整的Server实体创建流程（包含osType）")
    void testServerCreationWithOsType() {
        Server server = new Server();
        server.setName("测试服务器");
        server.setHostname("192.168.1.100");
        server.setPort(8080);
        server.setSshPort(22);
        server.setSshUsername("root");
        server.setBaseWorkDirectory("/root");
        server.setServerType(Server.ServerType.DEVELOPMENT);
        server.setOsType(Server.OsType.LINUX);
        
        assertEquals("测试服务器", server.getName());
        assertEquals("192.168.1.100", server.getHostname());
        assertEquals(Server.ServerType.DEVELOPMENT, server.getServerType());
        assertEquals(Server.OsType.LINUX, server.getOsType());
    }
}
