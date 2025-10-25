package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.AgentDeployment;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.repository.AgentDeploymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Agent部署服务集成测试
 *
 * 注意：这些测试需要网络连接，可能较慢，默认禁用
 * 使用 @Disabled 注解，需要时手动启用
 */
@SpringBootTest
class AgentDeployServiceIntegrationTest {

    @Autowired
    private AgentDeployService agentDeployService;

    @MockBean
    private AgentDeploymentRepository deploymentRepository;

    @MockBean
    private SshFileTransferService sshFileTransferService;

    @MockBean
    private RemoteCommandService remoteCommandService;

    private AgentDeployment testDeployment;
    private Server testServer;

    @BeforeEach
    void setUp() {
        testServer = new Server();
        testServer.setId(1L);
        testServer.setName("test-server");
        testServer.setHostname("localhost");
        testServer.setSshPort(22);
        testServer.setSshUsername("testuser");

        testDeployment = new AgentDeployment(testServer, "0.91.0");
        testDeployment.setId(1L);
    }

    /**
     * T053: 集成测试 - 使用真实GitHub Release URL下载Agent二进制
     *
     * 这个测试会实际连接到GitHub并下载真实的OpenTelemetry Collector文件
     * 由于依赖外部网络，默认禁用，需要时手动启用
     *
     * 测试URL: https://github.com/open-telemetry/opentelemetry-collector-releases/releases/download/v0.91.0/otelcol-contrib_0.91.0_linux_amd64.tar.gz
     * 预期文件大小: 约 100-150 MB
     * 预期下载时间: 根据网络速度，可能需要 1-5 分钟
     */
    @Test
    @Disabled("需要网络连接，手动启用测试。运行命令：mvn test -Dtest=AgentDeployServiceIntegrationTest")
    void integration_downloadRealUrl_whenGitHubRelease_success() throws Exception {
        // Arrange
        String realGitHubUrl = "https://github.com/open-telemetry/opentelemetry-collector-releases/releases/"
                + "download/v0.91.0/otelcol-contrib_0.91.0_linux_amd64.tar.gz";

        ReflectionTestUtils.setField(agentDeployService, "cacheEnabled", true);
        ReflectionTestUtils.setField(agentDeployService, "agentBinaryPath", "agent/missing-file.bin");
        ReflectionTestUtils.setField(agentDeployService, "agentBinaryDownloadUrl", realGitHubUrl);
        ReflectionTestUtils.setField(agentDeployService, "downloadTimeoutMinutes", 10); // 10分钟超时

        // Act
        System.out.println("=== 开始集成测试：从GitHub下载真实Agent二进制 ===");
        System.out.println("URL: " + realGitHubUrl);
        System.out.println("这可能需要几分钟，请耐心等待...");

        long startTime = System.currentTimeMillis();

        Method method = AgentDeployService.class.getDeclaredMethod("resolveAgentBinary", AgentDeployment.class);
        method.setAccessible(true);
        byte[] result = (byte[]) method.invoke(agentDeployService, testDeployment);

        long endTime = System.currentTimeMillis();
        double durationSeconds = (endTime - startTime) / 1000.0;

        // Assert
        assertNotNull(result, "应该成功下载Agent二进制");
        assertTrue(result.length > 1_048_576, "下载的文件应该大于1MB");
        assertTrue(result.length < 200_000_000, "下载的文件应该小于200MB");

        // 验证gzip魔数
        assertEquals(0x1f, result[0] & 0xff, "第一个字节应该是gzip魔数 0x1f");
        assertEquals(0x8b, result[1] & 0xff, "第二个字节应该是gzip魔数 0x8b");

        // 验证日志
        String log = testDeployment.getDeploymentLog();
        assertTrue(log.contains("✅ 下载Agent二进制成功"), "日志应该包含下载成功标记");
        assertTrue(log.contains("MB"), "日志应该包含文件大小");
        assertTrue(log.contains("✅ 文件验证通过"), "日志应该包含验证通过标记");

        // 输出测试结果
        double sizeMB = result.length / 1024.0 / 1024.0;
        System.out.println("\n=== 集成测试完成 ===");
        System.out.println(String.format("下载成功！文件大小: %.2f MB", sizeMB));
        System.out.println(String.format("耗时: %.2f 秒", durationSeconds));
        System.out.println("\n部署日志：");
        System.out.println(log);
    }

    /**
     * 集成测试 - 验证下载缓存机制（第二次下载应该使用缓存）
     *
     * 这个测试依赖于 integration_downloadRealUrl_whenGitHubRelease_success
     * 需要先运行上面的测试填充缓存
     */
    @Test
    @Disabled("需要网络连接，手动启用测试")
    void integration_downloadRealUrl_whenCached_shouldUseCacheInstantly() throws Exception {
        // 先运行一次下载（填充缓存）
        integration_downloadRealUrl_whenGitHubRelease_success();

        // Arrange - 创建新的deployment对象
        AgentDeployment secondDeployment = new AgentDeployment(testServer, "0.91.0");
        secondDeployment.setId(2L);

        // Act - 第二次下载应该使用缓存
        long startTime = System.currentTimeMillis();

        Method method = AgentDeployService.class.getDeclaredMethod("resolveAgentBinary", AgentDeployment.class);
        method.setAccessible(true);
        byte[] result = (byte[]) method.invoke(agentDeployService, secondDeployment);

        long endTime = System.currentTimeMillis();
        double durationSeconds = (endTime - startTime) / 1000.0;

        // Assert
        assertNotNull(result, "缓存命中应该返回数据");
        assertTrue(durationSeconds < 1.0, "使用缓存应该在1秒内完成（实际: " + durationSeconds + "秒）");

        String log = secondDeployment.getDeploymentLog();
        assertTrue(log.contains("使用已缓存的Agent二进制"), "日志应该显示使用了缓存");

        System.out.println("\n=== 缓存测试完成 ===");
        System.out.println(String.format("缓存命中！耗时: %.3f 秒", durationSeconds));
        System.out.println("\n部署日志：");
        System.out.println(log);
    }
}
