package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.SSHHostConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSH配置解析器测试类
 * SSHConfigParser Test Class
 *
 * 测试覆盖:
 * - 标准SSH配置文件解析
 * - 边界条件和异常情况
 * - 路径展开功能
 * - 通配符Host识别
 * - 警告消息生成
 *
 * @author GitHub Copilot
 * @since 2025-10-18
 */
@DisplayName("SSH配置解析器测试")
class SSHConfigParserTest {

    private SSHConfigParser parser;

    @BeforeEach
    void setUp() {
        parser = new SSHConfigParser();
    }

    // ==================== 标准配置解析测试 ====================

    @Test
    @DisplayName("测试解析单个标准Host配置")
    void testParseConfig_singleHost() {
        // Arrange
        String config = """
            Host dev-server
                HostName 192.168.1.100
                Port 22
                User admin
                IdentityFile ~/.ssh/id_rsa
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertEquals(1, hosts.size(), "应解析出1个Host");

        SSHHostConfig host = hosts.get(0);
        assertEquals("dev-server", host.getHostPattern(), "Host名称应匹配");
        assertEquals("192.168.1.100", host.getHostname(), "HostName应匹配");
        assertEquals(22, host.getPort(), "Port应匹配");
        assertEquals("admin", host.getUser(), "User应匹配");
        assertTrue(host.getIdentityFile().endsWith(".ssh/id_rsa"),
                  "IdentityFile路径应被展开");
    }

    @Test
    @DisplayName("测试解析多个Host配置")
    void testParseConfig_multipleHosts() {
        // Arrange
        String config = """
            Host dev-server
                HostName 192.168.1.100
                User admin

            Host prod-server
                HostName 10.0.0.50
                Port 2222
                User root

            Host staging
                HostName staging.example.com
                User deploy
                IdentityFile ~/.ssh/staging_key
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertEquals(3, hosts.size(), "应解析出3个Host");

        // 验证第一个Host
        assertEquals("dev-server", hosts.get(0).getHostPattern());
        assertEquals("192.168.1.100", hosts.get(0).getHostname());
        assertEquals("admin", hosts.get(0).getUser());

        // 验证第二个Host
        assertEquals("prod-server", hosts.get(1).getHostPattern());
        assertEquals("10.0.0.50", hosts.get(1).getHostname());
        assertEquals(2222, hosts.get(1).getPort());
        assertEquals("root", hosts.get(1).getUser());

        // 验证第三个Host
        assertEquals("staging", hosts.get(2).getHostPattern());
        assertEquals("staging.example.com", hosts.get(2).getHostname());
        assertEquals("deploy", hosts.get(2).getUser());
        assertNotNull(hosts.get(2).getIdentityFile());
    }

    @Test
    @DisplayName("测试解析带注释的配置")
    void testParseConfig_withComments() {
        // Arrange
        String config = """
            # Development server configuration
            Host dev-server
                # Internal IP address
                HostName 192.168.1.100
                Port 22
                User admin
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertEquals(1, hosts.size(), "应解析出1个Host，注释应被忽略");
        assertEquals("dev-server", hosts.get(0).getHostPattern());
        assertEquals("192.168.1.100", hosts.get(0).getHostname());
    }

    @Test
    @DisplayName("测试解析带引号的值")
    void testParseConfig_quotedValues() {
        // Arrange
        String config = """
            Host dev-server
                HostName "192.168.1.100"
                User "admin"
                IdentityFile "~/.ssh/id_rsa"
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertEquals(1, hosts.size());
        SSHHostConfig host = hosts.get(0);
        assertEquals("192.168.1.100", host.getHostname(), "引号应被正确去除");
        assertEquals("admin", host.getUser(), "引号应被正确去除");
        assertNotNull(host.getIdentityFile());
    }

    @Test
    @DisplayName("测试解析大小写不敏感的关键字")
    void testParseConfig_caseInsensitiveKeywords() {
        // Arrange
        String config = """
            host dev-server
                hostname 192.168.1.100
                port 22
                user admin
                identityfile ~/.ssh/id_rsa
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertEquals(1, hosts.size(), "小写关键字应被正确识别");
        SSHHostConfig host = hosts.get(0);
        assertEquals("192.168.1.100", host.getHostname());
        assertEquals(22, host.getPort());
        assertEquals("admin", host.getUser());
    }

    @Test
    @DisplayName("测试解析ProxyJump配置")
    void testParseConfig_withProxyJump() {
        // Arrange
        String config = """
            Host private-server
                HostName 10.0.1.50
                User admin
                ProxyJump bastion.example.com
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertEquals(1, hosts.size());
        SSHHostConfig host = hosts.get(0);
        assertEquals("bastion.example.com", host.getProxyJump());
        assertTrue(host.hasProxyJump(), "应检测到ProxyJump配置");
    }

    @Test
    @DisplayName("测试解析额外配置项")
    void testParseConfig_extraOptions() {
        // Arrange
        String config = """
            Host dev-server
                HostName 192.168.1.100
                User admin
                ServerAliveInterval 60
                StrictHostKeyChecking no
                ForwardAgent yes
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertEquals(1, hosts.size());
        SSHHostConfig host = hosts.get(0);
        assertEquals("60", host.getExtraOption("ServerAliveInterval"),
                    "额外配置项应被存储");
        assertEquals("no", host.getExtraOption("StrictHostKeyChecking"));
        assertEquals("yes", host.getExtraOption("ForwardAgent"));
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("测试空配置内容")
    void testParseConfig_emptyContent() {
        // Act
        List<SSHHostConfig> hosts1 = parser.parseConfig("");
        List<SSHHostConfig> hosts2 = parser.parseConfig("   ");
        List<SSHHostConfig> hosts3 = parser.parseConfig(null);

        // Assert
        assertTrue(hosts1.isEmpty(), "空字符串应返回空列表");
        assertTrue(hosts2.isEmpty(), "空白字符串应返回空列表");
        assertTrue(hosts3.isEmpty(), "null应返回空列表");
    }

    @Test
    @DisplayName("测试仅包含注释的配置")
    void testParseConfig_onlyComments() {
        // Arrange
        String config = """
            # Comment line 1
            # Comment line 2
            # Comment line 3
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertTrue(hosts.isEmpty(), "仅包含注释应返回空列表");
    }

    @Test
    @DisplayName("测试通配符Host")
    void testParseConfig_wildcardHosts() {
        // Arrange
        String config = """
            Host prod-*
                HostName 10.0.0.1
                User admin

            Host *.example.com
                Port 2222

            Host server-?
                User root

            Host dev-server
                HostName 192.168.1.100
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertEquals(4, hosts.size(), "应解析所有Host（包括通配符）");

        // 验证通配符Host被正确解析
        assertEquals("prod-*", hosts.get(0).getHostPattern());
        assertEquals("*.example.com", hosts.get(1).getHostPattern());
        assertEquals("server-?", hosts.get(2).getHostPattern());
        assertEquals("dev-server", hosts.get(3).getHostPattern());
    }

    @Test
    @DisplayName("测试缺少HostName的配置")
    void testParseConfig_missingHostname() {
        // Arrange
        String config = """
            Host dev-server
                Port 22
                User admin
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertEquals(1, hosts.size(), "应解析出Host");
        SSHHostConfig host = hosts.get(0);
        assertNull(host.getHostname(), "HostName应为null");
        assertEquals("admin", host.getUser(), "其他字段应正常解析");
    }

    @Test
    @DisplayName("测试空Host名称")
    void testParseConfig_emptyHostPattern() {
        // Arrange
        String config = """
            Host
                HostName 192.168.1.100
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertTrue(hosts.isEmpty(), "空Host名称应被忽略");
    }

    @Test
    @DisplayName("测试格式错误的配置行")
    void testParseConfig_malformedLines() {
        // Arrange
        String config = """
            Host dev-server
                HostName 192.168.1.100
                InvalidLine
                Port 22
                AnotherInvalidLine
                User admin
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertEquals(1, hosts.size());
        SSHHostConfig host = hosts.get(0);
        assertEquals("192.168.1.100", host.getHostname());
        assertEquals(22, host.getPort());
        assertEquals("admin", host.getUser());
    }

    @Test
    @DisplayName("测试无效的Port值")
    void testParseConfig_invalidPort() {
        // Arrange
        String config = """
            Host dev-server
                HostName 192.168.1.100
                Port invalid-port
                User admin
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertEquals(1, hosts.size());
        SSHHostConfig host = hosts.get(0);
        assertNull(host.getPort(), "无效Port值应被忽略");
        assertEquals("admin", host.getUser(), "其他字段应正常解析");
    }

    @Test
    @DisplayName("测试全局配置（Host之前的配置）")
    void testParseConfig_globalConfig() {
        // Arrange
        String config = """
            ServerAliveInterval 60
            StrictHostKeyChecking no

            Host dev-server
                HostName 192.168.1.100
                User admin
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertEquals(1, hosts.size(), "全局配置应被忽略");
        assertEquals("dev-server", hosts.get(0).getHostPattern());
    }

    @Test
    @DisplayName("测试Include指令")
    void testParseConfig_includeDirective() {
        // Arrange
        String config = """
            Host dev-server
                HostName 192.168.1.100
                Include ~/.ssh/config.d/*
                User admin
            """;

        // Act
        List<SSHHostConfig> hosts = parser.parseConfig(config);

        // Assert
        assertEquals(1, hosts.size());
        SSHHostConfig host = hosts.get(0);
        assertEquals("~/.ssh/config.d/*", host.getExtraOption("Include"),
                    "Include指令应被存储为额外配置");
    }

    // ==================== 路径展开测试 ====================

    @Test
    @DisplayName("测试~/路径展开")
    void testExpandPath_tildeSlash() {
        // Arrange
        String path = "~/.ssh/id_rsa";
        String userHome = System.getProperty("user.home");

        // Act
        String expanded = parser.expandPath(path);

        // Assert
        assertEquals(userHome + "/.ssh/id_rsa", expanded,
                    "~/应被展开为用户主目录");
    }

    @Test
    @DisplayName("测试$HOME/路径展开")
    void testExpandPath_homeEnv() {
        // Arrange
        String path = "$HOME/.ssh/id_rsa";
        String userHome = System.getProperty("user.home");

        // Act
        String expanded = parser.expandPath(path);

        // Assert
        assertEquals(userHome + "/.ssh/id_rsa", expanded,
                    "$HOME/应被展开为用户主目录");
    }

    @Test
    @DisplayName("测试绝对路径保持不变")
    void testExpandPath_absolutePath() {
        // Arrange
        String path = "/home/user/.ssh/id_rsa";

        // Act
        String expanded = parser.expandPath(path);

        // Assert
        assertEquals(path, expanded, "绝对路径应保持不变");
    }

    @Test
    @DisplayName("测试相对路径保持不变")
    void testExpandPath_relativePath() {
        // Arrange
        String path = ".ssh/id_rsa";

        // Act
        String expanded = parser.expandPath(path);

        // Assert
        assertEquals(path, expanded, "相对路径应保持不变");
    }

    @Test
    @DisplayName("测试空路径和null")
    void testExpandPath_emptyAndNull() {
        // Act & Assert
        assertEquals("", parser.expandPath(""), "空字符串应返回空字符串");
        assertNull(parser.expandPath(null), "null应返回null");
    }

    @Test
    @DisplayName("测试Windows风格路径")
    void testExpandPath_windowsPath() {
        // Arrange
        String path = "C:\\Users\\admin\\.ssh\\id_rsa";

        // Act
        String expanded = parser.expandPath(path);

        // Assert
        assertEquals(path, expanded, "Windows路径应保持不变");
    }

    // ==================== 通配符检测测试 ====================

    @Test
    @DisplayName("测试检测星号通配符")
    void testIsWildcardHost_asterisk() {
        // Act & Assert
        assertTrue(parser.isWildcardHost("prod-*"),
                  "应检测到*通配符");
        assertTrue(parser.isWildcardHost("*.example.com"),
                  "应检测到*通配符");
        assertTrue(parser.isWildcardHost("*"),
                  "应检测到*通配符");
    }

    @Test
    @DisplayName("测试检测问号通配符")
    void testIsWildcardHost_questionMark() {
        // Act & Assert
        assertTrue(parser.isWildcardHost("server-?"),
                  "应检测到?通配符");
        assertTrue(parser.isWildcardHost("srv-???-prod"),
                  "应检测到?通配符");
    }

    @Test
    @DisplayName("测试检测混合通配符")
    void testIsWildcardHost_mixed() {
        // Act & Assert
        assertTrue(parser.isWildcardHost("server-*-?"),
                  "应检测到混合通配符");
    }

    @Test
    @DisplayName("测试普通Host名称")
    void testIsWildcardHost_normal() {
        // Act & Assert
        assertFalse(parser.isWildcardHost("dev-server"),
                   "普通名称不应被识别为通配符");
        assertFalse(parser.isWildcardHost("192.168.1.100"),
                   "IP地址不应被识别为通配符");
        assertFalse(parser.isWildcardHost("example.com"),
                   "域名不应被识别为通配符");
    }

    @Test
    @DisplayName("测试null和空字符串")
    void testIsWildcardHost_nullAndEmpty() {
        // Act & Assert
        assertFalse(parser.isWildcardHost(null),
                   "null不应被识别为通配符");
        assertFalse(parser.isWildcardHost(""),
                   "空字符串不应被识别为通配符");
    }

    // ==================== 跳过检查测试 ====================

    @Test
    @DisplayName("测试应跳过通配符Host")
    void testShouldSkipHost_wildcardHost() {
        // Arrange
        SSHHostConfig host1 = new SSHHostConfig("prod-*");
        SSHHostConfig host2 = new SSHHostConfig("*.example.com");

        // Act & Assert
        assertTrue(parser.shouldSkipHost(host1), "应跳过通配符Host");
        assertTrue(parser.shouldSkipHost(host2), "应跳过通配符Host");
    }

    @Test
    @DisplayName("测试应跳过空Host")
    void testShouldSkipHost_emptyHost() {
        // Arrange
        SSHHostConfig host1 = new SSHHostConfig("");
        SSHHostConfig host2 = new SSHHostConfig("   ");
        SSHHostConfig host3 = new SSHHostConfig("*");

        // Act & Assert
        assertTrue(parser.shouldSkipHost(host1), "应跳过空Host");
        assertTrue(parser.shouldSkipHost(host2), "应跳过空白Host");
        assertTrue(parser.shouldSkipHost(host3), "应跳过全局配置*");
    }

    @Test
    @DisplayName("测试应跳过null")
    void testShouldSkipHost_null() {
        // Act & Assert
        assertTrue(parser.shouldSkipHost(null), "应跳过null");
        assertTrue(parser.shouldSkipHost(new SSHHostConfig(null)),
                  "应跳过hostPattern为null的对象");
    }

    @Test
    @DisplayName("测试不应跳过正常Host")
    void testShouldSkipHost_normalHost() {
        // Arrange
        SSHHostConfig host1 = new SSHHostConfig("dev-server");
        host1.setHostname("192.168.1.100");

        SSHHostConfig host2 = new SSHHostConfig("prod-server");
        host2.setHostname("10.0.0.50");

        // Act & Assert
        assertFalse(parser.shouldSkipHost(host1), "不应跳过正常Host");
        assertFalse(parser.shouldSkipHost(host2), "不应跳过正常Host");
    }

    // ==================== 警告消息测试 ====================

    @Test
    @DisplayName("测试ProxyJump警告")
    void testGetWarningMessage_proxyJump() {
        // Arrange
        SSHHostConfig host = new SSHHostConfig("private-server");
        host.setHostname("10.0.1.50");
        host.setProxyJump("bastion.example.com");

        // Act
        String warning = parser.getWarningMessage(host);

        // Assert
        assertNotNull(warning, "应返回警告消息");
        assertTrue(warning.contains("ProxyJump"), "警告应提及ProxyJump");
    }

    @Test
    @DisplayName("测试Include警告")
    void testGetWarningMessage_include() {
        // Arrange
        SSHHostConfig host = new SSHHostConfig("dev-server");
        host.setHostname("192.168.1.100");
        host.addExtraOption("Include", "~/.ssh/config.d/*");

        // Act
        String warning = parser.getWarningMessage(host);

        // Assert
        assertNotNull(warning, "应返回警告消息");
        assertTrue(warning.contains("Include"), "警告应提及Include");
    }

    @Test
    @DisplayName("测试缺少HostName警告")
    void testGetWarningMessage_missingHostname() {
        // Arrange
        SSHHostConfig host = new SSHHostConfig("dev-server");
        host.setUser("admin");
        // 故意不设置HostName

        // Act
        String warning = parser.getWarningMessage(host);

        // Assert
        assertNotNull(warning, "应返回警告消息");
        assertTrue(warning.contains("HostName"), "警告应提及缺少HostName");
    }

    @Test
    @DisplayName("测试多个警告")
    void testGetWarningMessage_multipleWarnings() {
        // Arrange
        SSHHostConfig host = new SSHHostConfig("problematic-server");
        // 缺少HostName
        host.setProxyJump("bastion");
        host.addExtraOption("Include", "other.conf");

        // Act
        String warning = parser.getWarningMessage(host);

        // Assert
        assertNotNull(warning, "应返回警告消息");
        assertTrue(warning.contains("HostName"), "应包含HostName警告");
        assertTrue(warning.contains("ProxyJump"), "应包含ProxyJump警告");
        assertTrue(warning.contains("Include"), "应包含Include警告");
    }

    @Test
    @DisplayName("测试无警告的正常Host")
    void testGetWarningMessage_normalHost() {
        // Arrange
        SSHHostConfig host = new SSHHostConfig("dev-server");
        host.setHostname("192.168.1.100");
        host.setUser("admin");
        host.setPort(22);

        // Act
        String warning = parser.getWarningMessage(host);

        // Assert
        assertNull(warning, "正常Host不应有警告");
    }

    @Test
    @DisplayName("测试null Host的警告")
    void testGetWarningMessage_nullHost() {
        // Act
        String warning = parser.getWarningMessage(null);

        // Assert
        assertNull(warning, "null应返回null警告");
    }
}
