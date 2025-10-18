package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.SSHHostConfig;
import com.cmict.internalpaas.dto.ServerImportDto;
import com.cmict.internalpaas.model.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSH配置映射器测试类
 * SSHConfigMapper Test Class
 *
 * 测试覆盖:
 * - SSH配置到服务器DTO的映射
 * - 字段验证和缺失字段检测
 * - 批量映射和验证
 * - 过滤和摘要功能
 *
 * @author GitHub Copilot
 * @since 2025-10-18
 */
@DisplayName("SSH配置映射器测试")
class SSHConfigMapperTest {

    private SSHConfigMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SSHConfigMapper();
    }

    // ==================== 单个映射测试 ====================

    @Test
    @DisplayName("测试映射完整的SSH配置")
    void testMapToServer_completeConfig() {
        // Arrange
        SSHHostConfig sshConfig = new SSHHostConfig("dev-server");
        sshConfig.setHostname("192.168.1.100");
        sshConfig.setPort(22);
        sshConfig.setUser("admin");
        sshConfig.setIdentityFile("/home/user/.ssh/id_rsa");

        // Act
        ServerImportDto dto = mapper.mapToServer(sshConfig);

        // Assert
        assertNotNull(dto, "映射结果不应为null");
        assertEquals("dev-server", dto.getName(), "名称应映射自hostPattern");
        assertEquals("192.168.1.100", dto.getHostname(), "主机名应匹配");
        assertEquals(22, dto.getSshPort(), "SSH端口应匹配");
        assertEquals("admin", dto.getSshUsername(), "SSH用户名应匹配");
        assertEquals("/home/user/.ssh/id_rsa", dto.getSshKeyPath(), "私钥路径应匹配");

        // 验证默认值
        assertEquals(8080, dto.getPort(), "应用端口应为默认值8080");
        assertEquals(Server.ServerType.DEVELOPMENT, dto.getServerType(),
                    "服务器类型应为默认值DEVELOPMENT");

        // 验证自动生成的字段
        assertEquals("/home/admin", dto.getBaseWorkDirectory(),
                    "应自动生成工作目录");
        assertTrue(dto.getDescription().contains("dev-server"),
                  "应自动生成描述");

        // 验证状态
        assertTrue(dto.isValid(), "完整配置应通过验证");
        assertTrue(dto.getMissingFields().isEmpty(), "不应有缺失字段");
    }

    @Test
    @DisplayName("测试映射缺少端口的SSH配置")
    void testMapToServer_missingPort() {
        // Arrange
        SSHHostConfig sshConfig = new SSHHostConfig("prod-server");
        sshConfig.setHostname("10.0.0.50");
        sshConfig.setUser("root");
        sshConfig.setIdentityFile("~/.ssh/id_rsa");
        // 未设置port

        // Act
        ServerImportDto dto = mapper.mapToServer(sshConfig);

        // Assert
        assertNotNull(dto);
        assertEquals(22, dto.getSshPort(), "未指定端口应使用默认值22");
        assertEquals("/root", dto.getBaseWorkDirectory(),
                    "root用户工作目录应为/root");
    }

    @Test
    @DisplayName("测试映射null配置")
    void testMapToServer_nullConfig() {
        // Act
        ServerImportDto dto = mapper.mapToServer(null);

        // Assert
        assertNull(dto, "null配置应返回null");
    }

    @Test
    @DisplayName("测试映射缺少必填字段的配置")
    void testMapToServer_missingRequiredFields() {
        // Arrange - 只有hostPattern和hostname
        SSHHostConfig sshConfig = new SSHHostConfig("incomplete-server");
        sshConfig.setHostname("192.168.1.200");
        // 缺少user和identityFile

        // Act
        ServerImportDto dto = mapper.mapToServer(sshConfig);

        // Assert
        assertNotNull(dto);
        assertFalse(dto.isValid(), "缺少必填字段应验证失败");
        assertFalse(dto.getMissingFields().isEmpty(), "应检测到缺失字段");
        assertTrue(dto.getMissingFields().contains("sshUsername"),
                  "应检测到缺少sshUsername");
        assertTrue(dto.getMissingFields().contains("sshPassword/sshKeyPath"),
                  "应检测到缺少认证凭证");
    }

    // ==================== 批量映射测试 ====================

    @Test
    @DisplayName("测试批量映射SSH配置列表")
    void testMapToServers_multipleConfigs() {
        // Arrange
        SSHHostConfig config1 = new SSHHostConfig("server-1");
        config1.setHostname("192.168.1.1");
        config1.setUser("user1");
        config1.setIdentityFile("~/.ssh/id_rsa");

        SSHHostConfig config2 = new SSHHostConfig("server-2");
        config2.setHostname("192.168.1.2");
        config2.setUser("user2");
        config2.setIdentityFile("~/.ssh/id_rsa");

        SSHHostConfig config3 = new SSHHostConfig("server-3");
        config3.setHostname("192.168.1.3");
        config3.setUser("user3");
        config3.setIdentityFile("~/.ssh/id_rsa");

        List<SSHHostConfig> configs = Arrays.asList(config1, config2, config3);

        // Act
        List<ServerImportDto> dtos = mapper.mapToServers(configs);

        // Assert
        assertEquals(3, dtos.size(), "应映射3个配置");
        assertEquals("server-1", dtos.get(0).getName());
        assertEquals("server-2", dtos.get(1).getName());
        assertEquals("server-3", dtos.get(2).getName());
    }

    @Test
    @DisplayName("测试批量映射空列表")
    void testMapToServers_emptyList() {
        // Act
        List<ServerImportDto> dtos = mapper.mapToServers(new ArrayList<>());

        // Assert
        assertTrue(dtos.isEmpty(), "空列表应返回空列表");
    }

    @Test
    @DisplayName("测试批量映射null列表")
    void testMapToServers_nullList() {
        // Act
        List<ServerImportDto> dtos = mapper.mapToServers(null);

        // Assert
        assertTrue(dtos.isEmpty(), "null列表应返回空列表");
    }

    // ==================== 字段验证测试 ====================

    @Test
    @DisplayName("测试有效DTO验证")
    void testIsValid_validDto() {
        // Arrange
        ServerImportDto dto = new ServerImportDto();
        dto.setName("test-server");
        dto.setHostname("192.168.1.100");
        dto.setSshUsername("admin");
        dto.setSshKeyPath("~/.ssh/id_rsa");

        // Act
        boolean valid = mapper.isValid(dto);

        // Assert
        assertTrue(valid, "完整DTO应通过验证");
    }

    @Test
    @DisplayName("测试缺少名称的验证")
    void testIsValid_missingName() {
        // Arrange
        ServerImportDto dto = new ServerImportDto();
        dto.setHostname("192.168.1.100");
        dto.setSshUsername("admin");
        dto.setSshKeyPath("~/.ssh/id_rsa");
        // 缺少name

        // Act
        boolean valid = mapper.isValid(dto);

        // Assert
        assertFalse(valid, "缺少name应验证失败");
    }

    @Test
    @DisplayName("测试缺少主机名的验证")
    void testIsValid_missingHostname() {
        // Arrange
        ServerImportDto dto = new ServerImportDto();
        dto.setName("test-server");
        dto.setSshUsername("admin");
        dto.setSshKeyPath("~/.ssh/id_rsa");
        // 缺少hostname

        // Act
        boolean valid = mapper.isValid(dto);

        // Assert
        assertFalse(valid, "缺少hostname应验证失败");
    }

    @Test
    @DisplayName("测试缺少SSH用户名的验证")
    void testIsValid_missingSshUsername() {
        // Arrange
        ServerImportDto dto = new ServerImportDto();
        dto.setName("test-server");
        dto.setHostname("192.168.1.100");
        dto.setSshKeyPath("~/.ssh/id_rsa");
        // 缺少sshUsername

        // Act
        boolean valid = mapper.isValid(dto);

        // Assert
        assertFalse(valid, "缺少sshUsername应验证失败");
    }

    @Test
    @DisplayName("测试缺少认证凭证的验证")
    void testIsValid_missingAuthCredentials() {
        // Arrange
        ServerImportDto dto = new ServerImportDto();
        dto.setName("test-server");
        dto.setHostname("192.168.1.100");
        dto.setSshUsername("admin");
        // 既没有password也没有keyPath

        // Act
        boolean valid = mapper.isValid(dto);

        // Assert
        assertFalse(valid, "缺少认证凭证应验证失败");
    }

    @Test
    @DisplayName("测试使用密码的验证")
    void testIsValid_withPassword() {
        // Arrange
        ServerImportDto dto = new ServerImportDto();
        dto.setName("test-server");
        dto.setHostname("192.168.1.100");
        dto.setSshUsername("admin");
        dto.setSshPassword("password123");
        // 使用密码而非私钥

        // Act
        boolean valid = mapper.isValid(dto);

        // Assert
        assertTrue(valid, "使用密码应通过验证");
    }

    @Test
    @DisplayName("测试null DTO的验证")
    void testIsValid_nullDto() {
        // Act
        boolean valid = mapper.isValid(null);

        // Assert
        assertFalse(valid, "null DTO应验证失败");
    }

    // ==================== 缺失字段检测测试 ====================

    @Test
    @DisplayName("测试获取完整DTO的缺失字段")
    void testGetMissingFields_completeDto() {
        // Arrange
        ServerImportDto dto = new ServerImportDto();
        dto.setName("test-server");
        dto.setHostname("192.168.1.100");
        dto.setSshUsername("admin");
        dto.setSshKeyPath("~/.ssh/id_rsa");

        // Act
        List<String> missingFields = mapper.getMissingFields(dto);

        // Assert
        assertTrue(missingFields.isEmpty(), "完整DTO不应有缺失字段");
    }

    @Test
    @DisplayName("测试获取所有缺失字段")
    void testGetMissingFields_allMissing() {
        // Arrange
        ServerImportDto dto = new ServerImportDto();
        // 所有必填字段都为null

        // Act
        List<String> missingFields = mapper.getMissingFields(dto);

        // Assert
        assertEquals(4, missingFields.size(), "应检测到4个缺失字段");
        assertTrue(missingFields.contains("name"));
        assertTrue(missingFields.contains("hostname"));
        assertTrue(missingFields.contains("sshUsername"));
        assertTrue(missingFields.contains("sshPassword/sshKeyPath"));
    }

    @Test
    @DisplayName("测试获取部分缺失字段")
    void testGetMissingFields_partiallyMissing() {
        // Arrange
        ServerImportDto dto = new ServerImportDto();
        dto.setName("test-server");
        dto.setHostname("192.168.1.100");
        // 缺少sshUsername和认证凭证

        // Act
        List<String> missingFields = mapper.getMissingFields(dto);

        // Assert
        assertEquals(2, missingFields.size());
        assertTrue(missingFields.contains("sshUsername"));
        assertTrue(missingFields.contains("sshPassword/sshKeyPath"));
    }

    @Test
    @DisplayName("测试获取null DTO的缺失字段")
    void testGetMissingFields_nullDto() {
        // Act
        List<String> missingFields = mapper.getMissingFields(null);

        // Assert
        assertEquals(1, missingFields.size());
        assertTrue(missingFields.contains("整个对象为null"));
    }

    @Test
    @DisplayName("测试空白字段被识别为缺失")
    void testGetMissingFields_blankFields() {
        // Arrange
        ServerImportDto dto = new ServerImportDto();
        dto.setName("   ");  // 空白
        dto.setHostname("");  // 空字符串
        dto.setSshUsername(" ");  // 空白
        dto.setSshKeyPath("");  // 空字符串

        // Act
        List<String> missingFields = mapper.getMissingFields(dto);

        // Assert
        assertEquals(4, missingFields.size(), "空白字段应被识别为缺失");
    }

    // ==================== 批量验证测试 ====================

    @Test
    @DisplayName("测试批量验证所有DTO")
    void testValidateAll() {
        // Arrange
        ServerImportDto dto1 = createValidDto("server-1", "192.168.1.1");
        ServerImportDto dto2 = createInvalidDto("server-2");  // 缺失字段
        ServerImportDto dto3 = createValidDto("server-3", "192.168.1.3");

        List<ServerImportDto> dtos = Arrays.asList(dto1, dto2, dto3);

        // Act
        int validCount = mapper.validateAll(dtos);

        // Assert
        assertEquals(2, validCount, "应有2个有效DTO");
        assertTrue(dto1.isValid());
        assertFalse(dto2.isValid());
        assertTrue(dto3.isValid());
    }

    @Test
    @DisplayName("测试批量验证空列表")
    void testValidateAll_emptyList() {
        // Act
        int validCount = mapper.validateAll(new ArrayList<>());

        // Assert
        assertEquals(0, validCount);
    }

    @Test
    @DisplayName("测试批量验证null列表")
    void testValidateAll_nullList() {
        // Act
        int validCount = mapper.validateAll(null);

        // Assert
        assertEquals(0, validCount);
    }

    // ==================== 过滤测试 ====================

    @Test
    @DisplayName("测试过滤有效DTO")
    void testFilterValid() {
        // Arrange
        ServerImportDto dto1 = createValidDto("server-1", "192.168.1.1");
        ServerImportDto dto2 = createInvalidDto("server-2");
        ServerImportDto dto3 = createValidDto("server-3", "192.168.1.3");

        List<ServerImportDto> dtos = Arrays.asList(dto1, dto2, dto3);

        // Act
        List<ServerImportDto> validDtos = mapper.filterValid(dtos);

        // Assert
        assertEquals(2, validDtos.size(), "应过滤出2个有效DTO");
        assertTrue(validDtos.contains(dto1));
        assertTrue(validDtos.contains(dto3));
        assertFalse(validDtos.contains(dto2));
    }

    @Test
    @DisplayName("测试过滤无效DTO")
    void testFilterInvalid() {
        // Arrange
        ServerImportDto dto1 = createValidDto("server-1", "192.168.1.1");
        ServerImportDto dto2 = createInvalidDto("server-2");
        ServerImportDto dto3 = createValidDto("server-3", "192.168.1.3");

        List<ServerImportDto> dtos = Arrays.asList(dto1, dto2, dto3);

        // Act
        List<ServerImportDto> invalidDtos = mapper.filterInvalid(dtos);

        // Assert
        assertEquals(1, invalidDtos.size(), "应过滤出1个无效DTO");
        assertTrue(invalidDtos.contains(dto2));
        assertFalse(invalidDtos.contains(dto1));
        assertFalse(invalidDtos.contains(dto3));
    }

    @Test
    @DisplayName("测试过滤空列表")
    void testFilter_emptyList() {
        // Act
        List<ServerImportDto> validDtos = mapper.filterValid(new ArrayList<>());
        List<ServerImportDto> invalidDtos = mapper.filterInvalid(new ArrayList<>());

        // Assert
        assertTrue(validDtos.isEmpty());
        assertTrue(invalidDtos.isEmpty());
    }

    // ==================== 验证摘要测试 ====================

    @Test
    @DisplayName("测试获取验证摘要")
    void testGetValidationSummary() {
        // Arrange
        ServerImportDto dto1 = createValidDto("server-1", "192.168.1.1");
        ServerImportDto dto2 = createInvalidDto("server-2");
        ServerImportDto dto3 = createValidDto("server-3", "192.168.1.3");
        ServerImportDto dto4 = createValidDto("server-4", "192.168.1.4");
        dto4.markAsDuplicate("existing-server");

        List<ServerImportDto> dtos = Arrays.asList(dto1, dto2, dto3, dto4);

        // Act
        String summary = mapper.getValidationSummary(dtos);

        // Assert
        assertNotNull(summary);
        assertTrue(summary.contains("总计: 4"), "应包含总计");
        assertTrue(summary.contains("有效: 2"), "应包含有效数");
        assertTrue(summary.contains("无效: 1"), "应包含无效数");
        assertTrue(summary.contains("重复: 1"), "应包含重复数");
    }

    @Test
    @DisplayName("测试空列表的验证摘要")
    void testGetValidationSummary_emptyList() {
        // Act
        String summary = mapper.getValidationSummary(new ArrayList<>());

        // Assert
        assertEquals("无服务器DTO", summary);
    }

    @Test
    @DisplayName("测试null列表的验证摘要")
    void testGetValidationSummary_nullList() {
        // Act
        String summary = mapper.getValidationSummary(null);

        // Assert
        assertEquals("无服务器DTO", summary);
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建有效的ServerImportDto
     */
    private ServerImportDto createValidDto(String name, String hostname) {
        ServerImportDto dto = new ServerImportDto();
        dto.setName(name);
        dto.setHostname(hostname);
        dto.setSshUsername("admin");
        dto.setSshKeyPath("~/.ssh/id_rsa");

        // 执行验证以设置 valid 字段
        List<String> missingFields = mapper.getMissingFields(dto);
        dto.setMissingFields(missingFields);
        dto.setValid(missingFields.isEmpty());

        return dto;
    }

    /**
     * 创建无效的ServerImportDto（缺失必填字段）
     */
    private ServerImportDto createInvalidDto(String name) {
        ServerImportDto dto = new ServerImportDto();
        dto.setName(name);
        // 缺少其他必填字段

        // 执行验证以设置 valid 字段
        List<String> missingFields = mapper.getMissingFields(dto);
        dto.setMissingFields(missingFields);
        dto.setValid(missingFields.isEmpty());

        return dto;
    }
}
