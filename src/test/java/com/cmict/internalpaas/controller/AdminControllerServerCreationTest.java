package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.repository.ServerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminController 服务器创建功能测试
 * Tests for server creation functionality in AdminController
 *
 * 测试覆盖：
 * - POST /admin/api/servers - 创建服务器（包含osType字段）
 * - 必填字段验证
 * - osType字段默认值
 * - osType字段各种有效值
 *
 * @author GitHub Copilot
 * @since 2025-10-20
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminController 服务器创建测试")
class AdminControllerServerCreationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServerRepository serverRepository;

    private Map<String, Object> validServerData;

    @BeforeEach
    void setUp() {
        // 准备有效的服务器数据
        validServerData = new HashMap<>();
        validServerData.put("name", "test-server");
        validServerData.put("hostname", "192.168.1.100");
        validServerData.put("port", 8080);
        validServerData.put("sshPort", 22);
        validServerData.put("sshUsername", "root");
        validServerData.put("sshPassword", "password123");
        validServerData.put("baseWorkDirectory", "/root");
        validServerData.put("description", "测试服务器");
        validServerData.put("serverType", "DEVELOPMENT");
        validServerData.put("osType", "LINUX");
        validServerData.put("active", true);
    }

    @Test
    @DisplayName("成功创建服务器（包含osType字段）")
    @WithMockUser(roles = "ADMIN")
    void testCreateServerWithOsType() throws Exception {
        mockMvc.perform(post("/admin/api/servers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "test-server-1")
                .param("hostname", "192.168.1.101")
                .param("port", "8080")
                .param("sshPort", "22")
                .param("sshUsername", "root")
                .param("sshPassword", "password123")
                .param("baseWorkDirectory", "/root")
                .param("serverType", "DEVELOPMENT")
                .param("osType", "LINUX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.server").exists())
                .andExpect(jsonPath("$.server.name").value("test-server-1"))
                .andExpect(jsonPath("$.server.osType").value("LINUX"));

        // 验证数据库中的记录
        Server savedServer = serverRepository.findByName("test-server-1").orElse(null);
        assertNotNull(savedServer);
        assertEquals("test-server-1", savedServer.getName());
        assertEquals(Server.OsType.LINUX, savedServer.getOsType());
    }

    @Test
    @DisplayName("创建Windows服务器")
    @WithMockUser(roles = "ADMIN")
    void testCreateWindowsServer() throws Exception {
        mockMvc.perform(post("/admin/api/servers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "windows-server")
                .param("hostname", "192.168.1.102")
                .param("port", "8080")
                .param("sshPort", "22")
                .param("sshUsername", "administrator")
                .param("sshPassword", "password123")
                .param("baseWorkDirectory", "C:\\workspaces")
                .param("serverType", "PRODUCTION")
                .param("osType", "WINDOWS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server.osType").value("WINDOWS"));

        Server savedServer = serverRepository.findByName("windows-server").orElse(null);
        assertNotNull(savedServer);
        assertEquals(Server.OsType.WINDOWS, savedServer.getOsType());
    }

    @Test
    @DisplayName("创建macOS服务器")
    @WithMockUser(roles = "ADMIN")
    void testCreateMacOSServer() throws Exception {
        mockMvc.perform(post("/admin/api/servers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "macos-server")
                .param("hostname", "192.168.1.103")
                .param("port", "8080")
                .param("sshPort", "22")
                .param("sshUsername", "admin")
                .param("sshPassword", "password123")
                .param("baseWorkDirectory", "/Users/admin")
                .param("serverType", "DEVELOPMENT")
                .param("osType", "MACOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server.osType").value("MACOS"));

        Server savedServer = serverRepository.findByName("macos-server").orElse(null);
        assertNotNull(savedServer);
        assertEquals(Server.OsType.MACOS, savedServer.getOsType());
    }

    @Test
    @DisplayName("创建服务器时不指定osType，应使用默认值LINUX")
    @WithMockUser(roles = "ADMIN")
    void testCreateServerWithoutOsType() throws Exception {
        mockMvc.perform(post("/admin/api/servers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "default-os-server")
                .param("hostname", "192.168.1.104")
                .param("port", "8080")
                .param("sshPort", "22")
                .param("sshUsername", "root")
                .param("sshPassword", "password123")
                .param("baseWorkDirectory", "/root")
                .param("serverType", "DEVELOPMENT"))
                // 注意：不传osType参数
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server.osType").value("LINUX"));

        Server savedServer = serverRepository.findByName("default-os-server").orElse(null);
        assertNotNull(savedServer);
        assertEquals(Server.OsType.LINUX, savedServer.getOsType(), "未指定osType时应使用默认值LINUX");
    }

    @Test
    @DisplayName("测试所有有效的osType值")
    @WithMockUser(roles = "ADMIN")
    void testAllValidOsTypes() throws Exception {
        String[] osTypes = {"LINUX", "WINDOWS", "MACOS", "UNIX", "BSD", "OTHER"};
        
        for (int i = 0; i < osTypes.length; i++) {
            String serverName = "server-" + osTypes[i].toLowerCase();
            
            mockMvc.perform(post("/admin/api/servers")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("name", serverName)
                    .param("hostname", "192.168.1." + (110 + i))
                    .param("port", "8080")
                    .param("sshPort", "22")
                    .param("sshUsername", "root")
                    .param("sshPassword", "password123")
                    .param("baseWorkDirectory", "/root")
                    .param("serverType", "DEVELOPMENT")
                    .param("osType", osTypes[i]))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.server.osType").value(osTypes[i]));

            Server savedServer = serverRepository.findByName(serverName).orElse(null);
            assertNotNull(savedServer);
            assertEquals(Server.OsType.valueOf(osTypes[i]), savedServer.getOsType());
        }
    }

    @Test
    @DisplayName("创建服务器时缺少必填字段应返回错误")
    @WithMockUser(roles = "ADMIN")
    void testCreateServerMissingRequiredFields() throws Exception {
        // 缺少hostname
        mockMvc.perform(post("/admin/api/servers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "incomplete-server")
                .param("port", "8080")
                .param("sshPort", "22")
                .param("sshUsername", "root")
                .param("sshPassword", "password123"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("创建服务器时serverType和osType组合测试")
    @WithMockUser(roles = "ADMIN")
    void testServerTypeAndOsTypeCombinations() throws Exception {
        // Production + Windows
        mockMvc.perform(post("/admin/api/servers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "prod-windows")
                .param("hostname", "192.168.1.120")
                .param("port", "8080")
                .param("sshPort", "22")
                .param("sshUsername", "administrator")
                .param("sshPassword", "password123")
                .param("baseWorkDirectory", "C:\\production")
                .param("serverType", "PRODUCTION")
                .param("osType", "WINDOWS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server.serverType").value("PRODUCTION"))
                .andExpect(jsonPath("$.server.osType").value("WINDOWS"));

        // Development + macOS
        mockMvc.perform(post("/admin/api/servers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "dev-macos")
                .param("hostname", "192.168.1.121")
                .param("port", "8080")
                .param("sshPort", "22")
                .param("sshUsername", "developer")
                .param("sshPassword", "password123")
                .param("baseWorkDirectory", "/Users/developer")
                .param("serverType", "DEVELOPMENT")
                .param("osType", "MACOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server.serverType").value("DEVELOPMENT"))
                .andExpect(jsonPath("$.server.osType").value("MACOS"));
    }

    @Test
    @DisplayName("验证创建的服务器包含完整的字段")
    @WithMockUser(roles = "ADMIN")
    void testCreatedServerHasAllFields() throws Exception {
        mockMvc.perform(post("/admin/api/servers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "complete-server")
                .param("hostname", "192.168.1.130")
                .param("port", "8080")
                .param("sshPort", "22")
                .param("sshUsername", "root")
                .param("sshPassword", "password123")
                .param("baseWorkDirectory", "/root")
                .param("description", "完整的测试服务器")
                .param("serverType", "TESTING")
                .param("osType", "UNIX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server.name").value("complete-server"))
                .andExpect(jsonPath("$.server.hostname").value("192.168.1.130"))
                .andExpect(jsonPath("$.server.port").value(8080))
                .andExpect(jsonPath("$.server.sshPort").value(22))
                .andExpect(jsonPath("$.server.sshUsername").value("root"))
                .andExpect(jsonPath("$.server.baseWorkDirectory").value("/root"))
                .andExpect(jsonPath("$.server.description").value("完整的测试服务器"))
                .andExpect(jsonPath("$.server.serverType").value("TESTING"))
                .andExpect(jsonPath("$.server.osType").value("UNIX"))
                .andExpect(jsonPath("$.server.active").value(true));

        Server savedServer = serverRepository.findByName("complete-server").orElse(null);
        assertNotNull(savedServer);
        assertEquals("complete-server", savedServer.getName());
        assertEquals("192.168.1.130", savedServer.getHostname());
        assertEquals(8080, savedServer.getPort());
        assertEquals(22, savedServer.getSshPort());
        assertEquals("root", savedServer.getSshUsername());
        assertEquals("/root", savedServer.getBaseWorkDirectory());
        assertEquals("完整的测试服务器", savedServer.getDescription());
        assertEquals(Server.ServerType.TESTING, savedServer.getServerType());
        assertEquals(Server.OsType.UNIX, savedServer.getOsType());
        assertTrue(savedServer.getActive());
    }

    @Test
    @DisplayName("非管理员用户不能创建服务器")
    @WithMockUser(roles = "USER")
    void testNonAdminCannotCreateServer() throws Exception {
        mockMvc.perform(post("/admin/api/servers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "unauthorized-server")
                .param("hostname", "192.168.1.140")
                .param("port", "8080")
                .param("sshPort", "22")
                .param("sshUsername", "root")
                .param("sshPassword", "password123")
                .param("baseWorkDirectory", "/root")
                .param("serverType", "DEVELOPMENT")
                .param("osType", "LINUX"))
                .andExpect(status().isForbidden());
    }
}
