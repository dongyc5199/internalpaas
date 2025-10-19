package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.SSHHostConfig;
import com.cmict.internalpaas.dto.ServerImportDto;
import com.cmict.internalpaas.model.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSHConfigMapper操作系统类型映射测试
 * Test cases for SSHConfigMapper osType mapping
 *
 * @author GitHub Copilot
 * @since 2025-10-20
 */
@DisplayName("SSHConfigMapper OsType 映射测试")
class SSHConfigMapperOsTypeTest {

    private SSHConfigMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SSHConfigMapper();
    }

    @Test
    @DisplayName("测试从SSHHostConfig映射osType为LINUX")
    void testMapOsTypeLinux() {
        SSHHostConfig sshConfig = new SSHHostConfig("test-server");
        sshConfig.setHostname("192.168.1.100");
        sshConfig.setUser("root");
        sshConfig.setPassword("password");
        sshConfig.setOsType("LINUX");

        ServerImportDto dto = mapper.mapToServer(sshConfig);

        assertNotNull(dto);
        assertEquals(Server.OsType.LINUX, dto.getOsType());
    }

    @Test
    @DisplayName("测试从SSHHostConfig映射osType为WINDOWS")
    void testMapOsTypeWindows() {
        SSHHostConfig sshConfig = new SSHHostConfig("windows-server");
        sshConfig.setHostname("192.168.1.101");
        sshConfig.setUser("administrator");
        sshConfig.setPassword("password");
        sshConfig.setOsType("WINDOWS");

        ServerImportDto dto = mapper.mapToServer(sshConfig);

        assertNotNull(dto);
        assertEquals(Server.OsType.WINDOWS, dto.getOsType());
    }

    @Test
    @DisplayName("测试从SSHHostConfig映射osType为MACOS")
    void testMapOsTypeMacOS() {
        SSHHostConfig sshConfig = new SSHHostConfig("mac-server");
        sshConfig.setHostname("192.168.1.102");
        sshConfig.setUser("admin");
        sshConfig.setPassword("password");
        sshConfig.setOsType("MACOS");

        ServerImportDto dto = mapper.mapToServer(sshConfig);

        assertNotNull(dto);
        assertEquals(Server.OsType.MACOS, dto.getOsType());
    }

    @Test
    @DisplayName("测试映射时osType大小写不敏感")
    void testMapOsTypeCaseInsensitive() {
        // 小写
        SSHHostConfig sshConfig1 = new SSHHostConfig("test1");
        sshConfig1.setHostname("192.168.1.100");
        sshConfig1.setUser("root");
        sshConfig1.setPassword("password");
        sshConfig1.setOsType("linux");
        
        ServerImportDto dto1 = mapper.mapToServer(sshConfig1);
        assertEquals(Server.OsType.LINUX, dto1.getOsType());

        // 混合大小写
        SSHHostConfig sshConfig2 = new SSHHostConfig("test2");
        sshConfig2.setHostname("192.168.1.101");
        sshConfig2.setUser("root");
        sshConfig2.setPassword("password");
        sshConfig2.setOsType("Windows");
        
        ServerImportDto dto2 = mapper.mapToServer(sshConfig2);
        assertEquals(Server.OsType.WINDOWS, dto2.getOsType());
    }

    @Test
    @DisplayName("测试映射无效的osType时使用默认值LINUX")
    void testMapInvalidOsTypeUsesDefault() {
        SSHHostConfig sshConfig = new SSHHostConfig("test-server");
        sshConfig.setHostname("192.168.1.100");
        sshConfig.setUser("root");
        sshConfig.setPassword("password");
        sshConfig.setOsType("INVALID_OS");

        ServerImportDto dto = mapper.mapToServer(sshConfig);

        assertNotNull(dto);
        assertEquals(Server.OsType.LINUX, dto.getOsType(), 
            "无效的osType应该使用默认值LINUX");
    }

    @Test
    @DisplayName("测试映射时osType为null或空时使用默认值")
    void testMapNullOrEmptyOsTypeUsesDefault() {
        // null osType
        SSHHostConfig sshConfig1 = new SSHHostConfig("test1");
        sshConfig1.setHostname("192.168.1.100");
        sshConfig1.setUser("root");
        sshConfig1.setPassword("password");
        sshConfig1.setOsType(null);
        
        ServerImportDto dto1 = mapper.mapToServer(sshConfig1);
        assertEquals(Server.OsType.LINUX, dto1.getOsType());

        // 空字符串 osType
        SSHHostConfig sshConfig2 = new SSHHostConfig("test2");
        sshConfig2.setHostname("192.168.1.101");
        sshConfig2.setUser("root");
        sshConfig2.setPassword("password");
        sshConfig2.setOsType("");
        
        ServerImportDto dto2 = mapper.mapToServer(sshConfig2);
        assertEquals(Server.OsType.LINUX, dto2.getOsType());
    }

    @Test
    @DisplayName("测试映射所有有效的osType值")
    void testMapAllValidOsTypes() {
        String[] osTypes = {"LINUX", "WINDOWS", "MACOS", "UNIX", "BSD", "OTHER"};
        Server.OsType[] expectedTypes = {
            Server.OsType.LINUX,
            Server.OsType.WINDOWS,
            Server.OsType.MACOS,
            Server.OsType.UNIX,
            Server.OsType.BSD,
            Server.OsType.OTHER
        };

        for (int i = 0; i < osTypes.length; i++) {
            SSHHostConfig sshConfig = new SSHHostConfig("test-" + i);
            sshConfig.setHostname("192.168.1." + (100 + i));
            sshConfig.setUser("root");
            sshConfig.setPassword("password");
            sshConfig.setOsType(osTypes[i]);

            ServerImportDto dto = mapper.mapToServer(sshConfig);

            assertEquals(expectedTypes[i], dto.getOsType(),
                "映射 " + osTypes[i] + " 应该得到 " + expectedTypes[i]);
        }
    }
}
