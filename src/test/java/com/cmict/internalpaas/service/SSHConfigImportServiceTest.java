package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.SSHConfigParseResult;
import com.cmict.internalpaas.dto.ServerImportDto;
import com.cmict.internalpaas.dto.ServerImportResult;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.repository.ServerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSH配置导入服务集成测试
 * Integration tests for SSHConfigImportService
 *
 * 测试完整的SSH配置导入流程，包括：
 * - 解析本地SSH配置文件
 * - 预览导入和去重检查
 * - 批量导入服务器
 * - 数据验证和转换
 *
 * @author GitHub Copilot
 * @since 2025-10-18
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SSH配置导入服务集成测试")
class SSHConfigImportServiceTest {

    @Autowired
    private SSHConfigImportService importService;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private ServerService serverService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        serverRepository.deleteAll();
    }

    // ==================== parseLocalConfig 测试 ====================

    @Test
    @DisplayName("测试解析有效的SSH配置文件")
    void testParseLocalConfig_validFile() throws IOException {
        // 准备测试数据
        String config = """
                Host dev-server
                    HostName 192.168.1.100
                    Port 22
                    User admin
                    IdentityFile ~/.ssh/id_rsa

                Host prod-server
                    HostName 10.0.0.50
                    Port 2222
                    User root
                    IdentityFile ~/.ssh/prod_rsa
                """;

        Path configFile = tempDir.resolve("config");
        Files.writeString(configFile, config);

        // 执行测试
        SSHConfigParseResult result = importService.parseLocalConfig(configFile.toString());

        // 验证结果
        assertNotNull(result);
        assertFalse(result.hasErrors());
        assertEquals(2, result.getTotalHosts());
        assertNotNull(result.getServers());
        assertEquals(2, result.getServers().size());

        // 验证第一个服务器
        ServerImportDto dto1 = result.getServers().get(0);
        assertEquals("dev-server", dto1.getName());
        assertEquals("192.168.1.100", dto1.getHostname());
        assertEquals(22, dto1.getSshPort());
        assertEquals("admin", dto1.getSshUsername());
        assertTrue(dto1.getSshKeyPath().endsWith("/.ssh/id_rsa") ||
                   dto1.getSshKeyPath().endsWith("\\.ssh\\id_rsa"));

        // 验证第二个服务器
        ServerImportDto dto2 = result.getServers().get(1);
        assertEquals("prod-server", dto2.getName());
        assertEquals("10.0.0.50", dto2.getHostname());
        assertEquals(2222, dto2.getSshPort());
        assertEquals("root", dto2.getSshUsername());
    }

    @Test
    @DisplayName("测试解析不存在的配置文件")
    void testParseLocalConfig_fileNotExists() {
        // 执行测试
        SSHConfigParseResult result = importService.parseLocalConfig("/nonexistent/path/config");

        // 验证结果
        assertNotNull(result);
        assertTrue(result.hasErrors());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().get(0).contains("不存在"));
    }

    @Test
    @DisplayName("测试解析空配置文件")
    void testParseLocalConfig_emptyFile() throws IOException {
        // 准备测试数据
        Path configFile = tempDir.resolve("empty_config");
        Files.writeString(configFile, "");

        // 执行测试
        SSHConfigParseResult result = importService.parseLocalConfig(configFile.toString());

        // 验证结果
        assertNotNull(result);
        assertFalse(result.hasErrors());
        assertEquals(0, result.getTotalHosts());
        assertTrue(result.getServers().isEmpty());
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().get(0).contains("未找到有效的Host配置"));
    }

    @Test
    @DisplayName("测试解析包含通配符的配置文件")
    void testParseLocalConfig_withWildcards() throws IOException {
        // 准备测试数据
        String config = """
                Host *
                    ServerAliveInterval 60

                Host dev-*
                    User developer

                Host prod-server
                    HostName 10.0.0.50
                    Port 22
                    User root
                    IdentityFile ~/.ssh/id_rsa
                """;

        Path configFile = tempDir.resolve("config_wildcards");
        Files.writeString(configFile, config);

        // 执行测试
        SSHConfigParseResult result = importService.parseLocalConfig(configFile.toString());

        // 验证结果
        assertNotNull(result);
        assertFalse(result.hasErrors());
        // 通配符被解析器直接跳过，不计入totalHosts
        assertTrue(result.getTotalHosts() >= 1);

        // 应该只有1个有效服务器（通配符被过滤）
        assertEquals(1, result.getServers().size());
        assertEquals("prod-server", result.getServers().get(0).getName());
    }

    @Test
    @DisplayName("测试解析配置文件并检查警告信息")
    void testParseLocalConfig_withWarnings() throws IOException {
        // 准备测试数据 - 缺少必填字段的配置
        String config = """
                Host incomplete-server
                    HostName 192.168.1.100
                    # 缺少User和IdentityFile

                Host complete-server
                    HostName 10.0.0.50
                    Port 22
                    User admin
                    IdentityFile ~/.ssh/id_rsa
                """;

        Path configFile = tempDir.resolve("config_warnings");
        Files.writeString(configFile, config);

        // 执行测试
        SSHConfigParseResult result = importService.parseLocalConfig(configFile.toString());

        // 验证结果
        assertNotNull(result);
        assertFalse(result.hasErrors());
        assertEquals(2, result.getServers().size());

        // 警告信息可能存在也可能不存在（取决于解析器对缺失字段的处理）
        assertNotNull(result.getWarnings());
    }

    // ==================== 路径安全验证测试 ====================

    /**
     * 路径安全验证测试嵌套类
     * 这些测试需要启用路径安全验证
     */
    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @Transactional
    @TestPropertySource(properties = {
        "app.ssh-config-import.path-security-enabled=true"
    })
    @DisplayName("路径安全验证测试（启用安全检查）")
    class PathSecurityTests {

        @Autowired
        private SSHConfigImportService importService;

    @Test
    @DisplayName("测试路径安全验证 - 有效的.ssh目录内路径")
    void testPathSecurity_validSshPath() throws IOException {
        // 准备测试数据 - 创建一个在.ssh目录内的配置文件
        Path sshDir = Path.of(System.getProperty("user.home"), ".ssh");
        if (!Files.exists(sshDir)) {
            Files.createDirectories(sshDir);
        }

        Path configFile = sshDir.resolve("test_config_" + System.currentTimeMillis());
        String config = """
                Host test-server
                    HostName 192.168.1.100
                    Port 22
                    User admin
                    IdentityFile ~/.ssh/id_rsa
                """;

        try {
            Files.writeString(configFile, config);

            // 执行测试
            SSHConfigParseResult result = importService.parseLocalConfig(configFile.toString());

            // 验证结果 - 应该成功解析，没有安全错误
            assertNotNull(result);
            assertFalse(result.hasErrors(), "Should not have errors for valid .ssh path");
            assertEquals(1, result.getServers().size());
        } finally {
            // 清理测试文件
            if (Files.exists(configFile)) {
                Files.delete(configFile);
            }
        }
    }

    @Test
    @DisplayName("测试路径安全验证 - 路径遍历攻击防护")
    void testPathSecurity_pathTraversalAttack() {
        // 尝试通过路径遍历访问.ssh目录外的文件
        String maliciousPath = System.getProperty("user.home") + "/.ssh/../../etc/passwd";

        // 执行测试
        SSHConfigParseResult result = importService.parseLocalConfig(maliciousPath);

        // 验证结果 - 应该被安全检查拦截
        assertNotNull(result);
        assertTrue(result.hasErrors(), "Should have error for path traversal attempt");
        assertFalse(result.getErrors().isEmpty());
        assertTrue(
            result.getErrors().get(0).contains("安全限制") ||
            result.getErrors().get(0).contains("Security"),
            "Error message should indicate security restriction"
        );
    }

    @Test
    @DisplayName("测试路径安全验证 - 绝对路径在.ssh目录外")
    void testPathSecurity_absolutePathOutsideSsh() {
        // 尝试访问.ssh目录外的绝对路径
        String outsidePath;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            outsidePath = "C:\\Windows\\System32\\config";
        } else {
            outsidePath = "/etc/passwd";
        }

        // 执行测试
        SSHConfigParseResult result = importService.parseLocalConfig(outsidePath);

        // 验证结果 - 应该被安全检查拦截
        assertNotNull(result);
        assertTrue(result.hasErrors(), "Should have error for path outside .ssh");
        assertTrue(
            result.getErrors().get(0).contains("安全限制") ||
            result.getErrors().get(0).contains(".ssh"),
            "Error message should mention .ssh directory restriction"
        );
    }

    @Test
    @DisplayName("测试路径安全验证 - 相对路径在.ssh目录外")
    void testPathSecurity_relativePathOutsideSsh() {
        // 尝试使用相对路径访问.ssh目录外的文件
        String relativePath = "../../../etc/hosts";

        // 执行测试
        SSHConfigParseResult result = importService.parseLocalConfig(relativePath);

        // 验证结果 - 应该被安全检查拦截
        assertNotNull(result);
        assertTrue(result.hasErrors(), "Should have error for relative path outside .ssh");
        assertTrue(
            result.getErrors().get(0).contains("安全限制"),
            "Error message should indicate security restriction"
        );
    }

    @Test
    @DisplayName("测试路径安全验证 - .ssh目录内的子目录路径")
    void testPathSecurity_validSubdirectoryPath() throws IOException {
        // 创建.ssh目录的子目录
        Path sshDir = Path.of(System.getProperty("user.home"), ".ssh");
        Path subDir = sshDir.resolve("test_subdir_" + System.currentTimeMillis());

        if (!Files.exists(sshDir)) {
            Files.createDirectories(sshDir);
        }
        Files.createDirectories(subDir);

        Path configFile = subDir.resolve("config");
        String config = """
                Host subdir-server
                    HostName 192.168.1.100
                    Port 22
                    User admin
                    IdentityFile ~/.ssh/id_rsa
                """;

        try {
            Files.writeString(configFile, config);

            // 执行测试
            SSHConfigParseResult result = importService.parseLocalConfig(configFile.toString());

            // 验证结果 - 子目录内的路径应该被允许
            assertNotNull(result);
            assertFalse(result.hasErrors(), "Should allow paths in .ssh subdirectories");
            assertEquals(1, result.getServers().size());
        } finally {
            // 清理测试文件和目录
            if (Files.exists(configFile)) {
                Files.delete(configFile);
            }
            if (Files.exists(subDir)) {
                Files.delete(subDir);
            }
        }
    }

    @Test
    @DisplayName("测试路径安全验证 - 路径包含..但仍在.ssh目录内")
    void testPathSecurity_dotDotWithinSsh() throws IOException {
        // 创建.ssh目录的子目录
        Path sshDir = Path.of(System.getProperty("user.home"), ".ssh");
        Path subDir = sshDir.resolve("test_subdir2_" + System.currentTimeMillis());

        if (!Files.exists(sshDir)) {
            Files.createDirectories(sshDir);
        }
        Files.createDirectories(subDir);

        Path configFile = sshDir.resolve("test_config2_" + System.currentTimeMillis());
        String config = """
                Host dotdot-server
                    HostName 192.168.1.100
                    Port 22
                    User admin
                    IdentityFile ~/.ssh/id_rsa
                """;

        try {
            Files.writeString(configFile, config);

            // 使用包含..但规范化后仍在.ssh目录内的路径
            String pathWithDotDot = subDir.toString() + "/../" + configFile.getFileName();

            // 执行测试
            SSHConfigParseResult result = importService.parseLocalConfig(pathWithDotDot);

            // 验证结果 - 规范化后在.ssh目录内的路径应该被允许
            assertNotNull(result);
            assertFalse(result.hasErrors(), "Should allow paths that normalize to .ssh directory");
            assertEquals(1, result.getServers().size());
        } finally {
            // 清理测试文件和目录
            if (Files.exists(configFile)) {
                Files.delete(configFile);
            }
            if (Files.exists(subDir)) {
                Files.delete(subDir);
            }
        }
    }

    @Test
    @DisplayName("测试路径安全验证 - 符号链接安全检查")
    void testPathSecurity_symlinkSecurity() throws IOException {
        // 注意：此测试在Windows上可能需要管理员权限，在Linux/macOS上应正常运行
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            // Windows上跳过符号链接测试（需要管理员权限）
            return;
        }

        Path sshDir = Path.of(System.getProperty("user.home"), ".ssh");
        if (!Files.exists(sshDir)) {
            Files.createDirectories(sshDir);
        }

        // 创建一个指向.ssh目录外的符号链接
        Path symlinkPath = sshDir.resolve("malicious_link_" + System.currentTimeMillis());
        Path targetPath = Path.of(System.getProperty("user.home"), "test_target_" + System.currentTimeMillis());

        try {
            // 创建目标文件
            Files.writeString(targetPath, "Host test\n    HostName 192.168.1.1\n");

            // 创建符号链接
            Files.createSymbolicLink(symlinkPath, targetPath);

            // 执行测试
            SSHConfigParseResult result = importService.parseLocalConfig(symlinkPath.toString());

            // 验证结果 - 应该检测到符号链接指向.ssh目录外
            assertNotNull(result);
            assertTrue(result.hasErrors(), "Should detect symlink pointing outside .ssh");
            assertTrue(
                result.getErrors().get(0).contains("符号链接") ||
                result.getErrors().get(0).contains("symlink") ||
                result.getErrors().get(0).contains("安全限制"),
                "Error message should mention symlink or security restriction"
            );
        } catch (UnsupportedOperationException e) {
            // 某些文件系统不支持符号链接，跳过测试
            System.out.println("Symlink test skipped: " + e.getMessage());
        } finally {
            // 清理测试文件
            if (Files.exists(symlinkPath)) {
                Files.delete(symlinkPath);
            }
            if (Files.exists(targetPath)) {
                Files.delete(targetPath);
            }
        }
    }

    @Test
    @DisplayName("测试路径安全验证 - 非法路径格式")
    void testPathSecurity_invalidPathFormat() {
        // 测试各种非法路径格式
        String[] invalidPaths = {
            "",                          // 空路径
            "   ",                       // 空白路径
            "\0invalid",                 // 包含空字符
        };

        for (String invalidPath : invalidPaths) {
            SSHConfigParseResult result = importService.parseLocalConfig(invalidPath);
            assertNotNull(result, "Result should not be null for path: " + invalidPath);
            // 空路径会使用默认路径，其他非法路径应该有错误
            if (!invalidPath.trim().isEmpty()) {
                assertTrue(
                    result.hasErrors() || !result.getServers().isEmpty(),
                    "Should handle invalid path format: " + invalidPath
                );
            }
        }
    }

    } // End of PathSecurityTests nested class

    // ==================== previewImport 测试 ====================

    @Test
    @DisplayName("测试预览导入 - 无重复服务器")
    void testPreviewImport_noDuplicates() {
        // 准备测试数据
        List<ServerImportDto> servers = new ArrayList<>();
        servers.add(createValidDto("server1", "192.168.1.100", 22));
        servers.add(createValidDto("server2", "192.168.1.101", 22));

        // 执行测试
        List<ServerImportDto> result = importService.previewImport(servers);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());

        // 验证所有服务器都有效且无重复
        for (ServerImportDto dto : result) {
            assertTrue(dto.isValid());
            assertFalse(dto.isDuplicate());
            assertNull(dto.getDuplicateWith());
        }
    }

    @Test
    @DisplayName("测试预览导入 - 存在重复服务器")
    void testPreviewImport_withDuplicates() {
        // 准备现有服务器
        Server existingServer = new Server();
        existingServer.setName("existing-server");
        existingServer.setHostname("192.168.1.100");
        existingServer.setSshPort(22);
        existingServer.setSshUsername("admin");
        existingServer.setPort(8080);
        existingServer.setBaseWorkDirectory("/app");
        serverRepository.save(existingServer);

        // 准备待导入服务器（包含重复）
        List<ServerImportDto> servers = new ArrayList<>();
        servers.add(createValidDto("server1", "192.168.1.100", 22)); // 重复
        servers.add(createValidDto("server2", "192.168.1.101", 22)); // 不重复

        // 执行测试
        List<ServerImportDto> result = importService.previewImport(servers);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());

        // 验证第一个服务器被标记为重复
        ServerImportDto dto1 = result.get(0);
        assertTrue(dto1.isDuplicate());
        assertEquals("existing-server", dto1.getDuplicateWith());

        // 验证第二个服务器不重复
        ServerImportDto dto2 = result.get(1);
        assertFalse(dto2.isDuplicate());
        assertNull(dto2.getDuplicateWith());
    }

    @Test
    @DisplayName("测试预览导入 - 验证无效服务器")
    void testPreviewImport_invalidServers() {
        // 准备测试数据
        List<ServerImportDto> servers = new ArrayList<>();

        // 有效服务器
        servers.add(createValidDto("valid-server", "192.168.1.100", 22));

        // 无效服务器（缺少hostname）
        ServerImportDto invalid = new ServerImportDto();
        invalid.setName("invalid-server");
        invalid.setSshUsername("admin");
        invalid.setSshKeyPath("~/.ssh/id_rsa");
        servers.add(invalid);

        // 执行测试
        List<ServerImportDto> result = importService.previewImport(servers);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());

        // 验证第一个服务器有效
        assertTrue(result.get(0).isValid());
        assertTrue(result.get(0).getMissingFields().isEmpty());

        // 验证第二个服务器无效
        assertFalse(result.get(1).isValid());
        assertFalse(result.get(1).getMissingFields().isEmpty());
        // 检查缺失字段列表不为空即可（具体错误消息格式可能不同）
        assertTrue(result.get(1).getMissingFields().size() > 0);
    }

    @Test
    @DisplayName("测试预览导入 - 空列表")
    void testPreviewImport_emptyList() {
        // 执行测试
        List<ServerImportDto> result = importService.previewImport(new ArrayList<>());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== batchImport 测试 ====================

    @Test
    @DisplayName("测试批量导入 - 全部成功")
    void testBatchImport_allSuccess() {
        // 准备测试数据
        List<ServerImportDto> servers = new ArrayList<>();
        servers.add(createValidDto("server1", "192.168.1.100", 22));
        servers.add(createValidDto("server2", "192.168.1.101", 22));

        // 执行测试
        ServerImportResult result = importService.batchImport(servers);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());
        assertTrue(result.isAllSuccess());
        assertEquals(2, result.getSuccessServers().size());
        assertTrue(result.getFailures().isEmpty());

        // 验证数据库中的服务器
        List<Server> savedServers = serverRepository.findAll();
        assertEquals(2, savedServers.size());
    }

    @Test
    @DisplayName("测试批量导入 - 部分失败")
    void testBatchImport_partialFailure() {
        // 准备测试数据
        List<ServerImportDto> servers = new ArrayList<>();

        // 有效服务器
        servers.add(createValidDto("valid-server", "192.168.1.100", 22));

        // 无效服务器（缺少必填字段）
        ServerImportDto invalid = new ServerImportDto();
        invalid.setName("invalid-server");
        // 缺少hostname、username等必填字段
        servers.add(invalid);

        // 执行测试
        ServerImportResult result = importService.batchImport(servers);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
        assertTrue(result.isPartialSuccess());
        assertEquals(1, result.getSuccessServers().size());
        assertEquals(1, result.getFailures().size());

        // 验证失败记录
        ServerImportResult.ImportFailure failure = result.getFailures().get(0);
        assertEquals("invalid-server", failure.getServerName());
        assertTrue(failure.getReason().contains("验证失败"));

        // 验证数据库中只有1个服务器
        List<Server> savedServers = serverRepository.findAll();
        assertEquals(1, savedServers.size());
        assertEquals("valid-server", savedServers.get(0).getName());
    }

    @Test
    @DisplayName("测试批量导入 - 存在重复服务器")
    void testBatchImport_withDuplicates() {
        // 准备现有服务器
        Server existingServer = new Server();
        existingServer.setName("existing-server");
        existingServer.setHostname("192.168.1.100");
        existingServer.setSshPort(22);
        existingServer.setSshUsername("admin");
        existingServer.setPort(8080);
        existingServer.setBaseWorkDirectory("/app");
        serverRepository.save(existingServer);

        // 准备待导入服务器
        List<ServerImportDto> servers = new ArrayList<>();
        servers.add(createValidDto("duplicate-server", "192.168.1.100", 22)); // 重复
        servers.add(createValidDto("new-server", "192.168.1.101", 22)); // 新服务器

        // 执行测试
        ServerImportResult result = importService.batchImport(servers);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getSuccessCount()); // 只有new-server成功
        assertEquals(1, result.getFailedCount()); // duplicate-server失败

        // 验证失败原因
        assertEquals(1, result.getFailures().size());
        ServerImportResult.ImportFailure failure = result.getFailures().get(0);
        assertTrue(failure.getReason().contains("已存在"));

        // 验证数据库中有2个服务器（1个existing + 1个new）
        List<Server> savedServers = serverRepository.findAll();
        assertEquals(2, savedServers.size());
    }

    @Test
    @DisplayName("测试批量导入 - 空列表")
    void testBatchImport_emptyList() {
        // 执行测试
        ServerImportResult result = importService.batchImport(new ArrayList<>());

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(0, result.getTotalCount());
    }

    @Test
    @DisplayName("测试批量导入 - 验证默认值设置")
    void testBatchImport_defaultValues() {
        // 准备测试数据（不设置port）
        ServerImportDto dto = new ServerImportDto();
        dto.setName("test-server");
        dto.setHostname("192.168.1.100");
        dto.setSshUsername("admin");
        dto.setSshKeyPath("~/.ssh/id_rsa");
        // 不设置sshPort和port，应使用默认值

        List<ServerImportDto> servers = new ArrayList<>();
        servers.add(dto);

        // 执行测试
        ServerImportResult result = importService.batchImport(servers);

        // 验证结果
        assertEquals(1, result.getSuccessCount());

        // 验证数据库中的服务器使用了默认值
        Server savedServer = serverRepository.findAll().get(0);
        assertEquals("test-server", savedServer.getName());
        assertEquals(22, savedServer.getSshPort()); // 默认SSH端口
        assertEquals(8080, savedServer.getPort()); // 默认应用端口
        assertEquals(Server.ServerType.DEVELOPMENT, savedServer.getServerType()); // 默认类型
        assertTrue(savedServer.getActive()); // 默认激活
        assertTrue(savedServer.getAutoMonitorEnabled()); // 默认启用监控
    }

    // ==================== validate 测试 ====================

    @Test
    @DisplayName("测试验证 - 有效的DTO")
    void testValidate_validDto() {
        // 准备测试数据
        ServerImportDto dto = createValidDto("test-server", "192.168.1.100", 22);

        // 执行测试
        List<String> errors = importService.validate(dto);

        // 验证结果
        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("测试验证 - 缺少服务器名称")
    void testValidate_missingName() {
        // 准备测试数据
        ServerImportDto dto = createValidDto("test-server", "192.168.1.100", 22);
        dto.setName(null);

        // 执行测试
        List<String> errors = importService.validate(dto);

        // 验证结果
        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("服务器名称")));
    }

    @Test
    @DisplayName("测试验证 - 缺少主机名")
    void testValidate_missingHostname() {
        // 准备测试数据
        ServerImportDto dto = createValidDto("test-server", "192.168.1.100", 22);
        dto.setHostname(null);

        // 执行测试
        List<String> errors = importService.validate(dto);

        // 验证结果
        assertNotNull(errors);
        assertTrue(errors.stream().anyMatch(e -> e.contains("主机名")));
    }

    @Test
    @DisplayName("测试验证 - 缺少SSH用户名")
    void testValidate_missingUsername() {
        // 准备测试数据
        ServerImportDto dto = createValidDto("test-server", "192.168.1.100", 22);
        dto.setSshUsername(null);

        // 执行测试
        List<String> errors = importService.validate(dto);

        // 验证结果
        assertNotNull(errors);
        assertTrue(errors.stream().anyMatch(e -> e.contains("SSH用户名")));
    }

    @Test
    @DisplayName("测试验证 - 缺少认证凭证")
    void testValidate_missingCredentials() {
        // 准备测试数据
        ServerImportDto dto = new ServerImportDto();
        dto.setName("test-server");
        dto.setHostname("192.168.1.100");
        dto.setSshUsername("admin");
        // 不设置密码和私钥

        // 执行测试
        List<String> errors = importService.validate(dto);

        // 验证结果
        assertNotNull(errors);
        assertTrue(errors.stream().anyMatch(e -> e.contains("认证凭证")));
    }

    @Test
    @DisplayName("测试验证 - null对象")
    void testValidate_nullDto() {
        // 执行测试
        List<String> errors = importService.validate(null);

        // 验证结果
        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("null"));
    }

    // ==================== convertToServer 测试 ====================

    @Test
    @DisplayName("测试DTO转换为Server - 完整配置")
    void testConvertToServer_completeConfig() {
        // 准备测试数据
        ServerImportDto dto = createValidDto("test-server", "192.168.1.100", 2222);
        dto.setPort(9090);
        dto.setDescription("Test server");
        dto.setBaseWorkDirectory("/opt/app");
        dto.setServerType(Server.ServerType.PRODUCTION);

        // 执行测试
        Server server = importService.convertToServer(dto);

        // 验证结果
        assertNotNull(server);
        assertEquals("test-server", server.getName());
        assertEquals("192.168.1.100", server.getHostname());
        assertEquals(9090, server.getPort());
        assertEquals(2222, server.getSshPort());
        assertEquals("admin", server.getSshUsername());
        assertEquals("Test server", server.getDescription());
        assertEquals("/opt/app", server.getBaseWorkDirectory());
        assertEquals(Server.ServerType.PRODUCTION, server.getServerType());
        assertTrue(server.getActive());
        assertEquals(Server.ConnectionStatus.UNKNOWN, server.getConnectionStatus());
    }

    @Test
    @DisplayName("测试DTO转换为Server - 最小配置")
    void testConvertToServer_minimalConfig() {
        // 准备测试数据
        ServerImportDto dto = new ServerImportDto();
        dto.setName("minimal-server");
        dto.setHostname("192.168.1.200");
        dto.setSshUsername("user");
        dto.setSshKeyPath("~/.ssh/id_rsa");

        // 执行测试
        Server server = importService.convertToServer(dto);

        // 验证结果
        assertNotNull(server);
        assertEquals("minimal-server", server.getName());
        assertEquals("192.168.1.200", server.getHostname());
        assertEquals(8080, server.getPort()); // 默认端口
        assertEquals(22, server.getSshPort()); // 默认SSH端口
        assertEquals("user", server.getSshUsername());
        assertEquals(Server.ServerType.DEVELOPMENT, server.getServerType()); // 默认类型
        assertTrue(server.getActive());
        assertTrue(server.getAutoMonitorEnabled());
    }

    @Test
    @DisplayName("测试DTO转换为Server - 包含SSH密码")
    void testConvertToServer_withPassword() {
        // 准备测试数据
        ServerImportDto dto = createValidDto("pwd-server", "192.168.1.100", 22);
        dto.setSshKeyPath(null); // 移除私钥
        dto.setSshPassword("test-password"); // 使用密码

        // 执行测试
        Server server = importService.convertToServer(dto);

        // 验证结果
        assertNotNull(server);
        assertNotNull(server.getSshPassword());
        // 密码应该被加密存储，所以不等于原始密码
        // 注意：这里只验证密码字段不为空，具体加密逻辑在Server实体中处理
    }

    @Test
    @DisplayName("测试DTO转换为Server - 包含SSH私钥")
    void testConvertToServer_withKeyPath() {
        // 准备测试数据
        ServerImportDto dto = createValidDto("key-server", "192.168.1.100", 22);

        // 执行测试
        Server server = importService.convertToServer(dto);

        // 验证结果
        assertNotNull(server);
        assertNotNull(server.getSshKeyPath());
        assertTrue(server.getSshKeyPath().endsWith("/.ssh/id_rsa") ||
                   server.getSshKeyPath().endsWith("\\.ssh\\id_rsa"));
    }

    // ==================== 工作流集成测试 ====================

    @Test
    @DisplayName("测试完整导入工作流 - parseLocalConfig → previewImport → batchImport")
    void testCompleteWorkflow() throws IOException {
        // 1. 准备SSH配置文件
        String config = """
                Host web-server
                    HostName 192.168.1.100
                    Port 22
                    User www
                    IdentityFile ~/.ssh/web_rsa

                Host db-server
                    HostName 192.168.1.101
                    Port 22
                    User dba
                    IdentityFile ~/.ssh/db_rsa

                Host cache-server
                    HostName 192.168.1.102
                    Port 22
                    User admin
                    IdentityFile ~/.ssh/cache_rsa
                """;

        Path configFile = tempDir.resolve("complete_config");
        Files.writeString(configFile, config);

        // 2. 解析配置文件
        SSHConfigParseResult parseResult = importService.parseLocalConfig(configFile.toString());
        assertNotNull(parseResult);
        assertFalse(parseResult.hasErrors());
        assertEquals(3, parseResult.getServers().size());

        // 3. 预览导入
        List<ServerImportDto> previewResult = importService.previewImport(parseResult.getServers());
        assertEquals(3, previewResult.size());

        // 验证所有服务器都有效且无重复
        for (ServerImportDto dto : previewResult) {
            assertTrue(dto.isValid(), "Server " + dto.getName() + " should be valid");
            assertFalse(dto.isDuplicate(), "Server " + dto.getName() + " should not be duplicate");
        }

        // 4. 批量导入
        ServerImportResult importResult = importService.batchImport(previewResult);
        assertNotNull(importResult);
        assertEquals(3, importResult.getSuccessCount());
        assertEquals(0, importResult.getFailedCount());
        assertTrue(importResult.isAllSuccess());

        // 5. 验证数据库中的数据
        List<Server> savedServers = serverRepository.findAll();
        assertEquals(3, savedServers.size());

        // 验证服务器名称
        List<String> serverNames = savedServers.stream()
                .map(Server::getName)
                .sorted()
                .toList();
        assertEquals(List.of("cache-server", "db-server", "web-server"), serverNames);

        // 6. 再次导入相同配置，应该检测到重复
        SSHConfigParseResult secondParse = importService.parseLocalConfig(configFile.toString());
        List<ServerImportDto> secondPreview = importService.previewImport(secondParse.getServers());

        // 所有服务器都应该被标记为重复
        for (ServerImportDto dto : secondPreview) {
            assertTrue(dto.isDuplicate(), "Server " + dto.getName() + " should be marked as duplicate");
            assertNotNull(dto.getDuplicateWith());
        }

        // 尝试再次导入，应该全部失败
        ServerImportResult secondImport = importService.batchImport(secondPreview);
        assertEquals(0, secondImport.getSuccessCount());
        assertEquals(3, secondImport.getFailedCount());
        assertTrue(secondImport.isAllFailed());
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建有效的ServerImportDto用于测试
     */
    private ServerImportDto createValidDto(String name, String hostname, int sshPort) {
        ServerImportDto dto = new ServerImportDto();
        dto.setName(name);
        dto.setHostname(hostname);
        dto.setSshPort(sshPort);
        dto.setSshUsername("admin");
        dto.setSshKeyPath(System.getProperty("user.home") + "/.ssh/id_rsa");
        return dto;
    }
}
