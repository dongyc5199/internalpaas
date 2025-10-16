package com.cmict.internalpaas.service;

import com.cmict.internalpaas.client.MetricsHubClient;
import com.cmict.internalpaas.model.AgentDeployment;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.repository.AgentDeploymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Hub数据验证功能单元测试
 * 测试Agent部署后的Hub数据验证逻辑
 *
 * @author Dev Debug Platform Team
 * @version 1.0 (Task 2: Hub数据验证)
 */
@ExtendWith(MockitoExtension.class)
class HubDataVerificationTest {

    @Mock
    private AgentDeploymentRepository deploymentRepository;

    @Mock
    private RemoteCommandService remoteCommandService;

    @Mock
    private SshFileTransferService sshFileTransferService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MetricsHubClient metricsHubClient;

    @InjectMocks
    private AgentDeployService agentDeployService;

    private Server testServer;
    private AgentDeployment testDeployment;

    @BeforeEach
    void setUp() {
        // 创建测试服务器
        testServer = new Server();
        testServer.setId(1L);
        testServer.setName("Test Server");
        testServer.setHostname("192.168.1.100");

        // 创建测试部署记录
        testDeployment = new AgentDeployment(testServer, "0.91.0");
        testDeployment.setId(1L);

        // 设置配置参数
        ReflectionTestUtils.setField(agentDeployService, "agentVersion", "0.91.0");
    }

    /**
     * 测试: Hub数据验证 - 成功获取最新数据
     */
    @Test
    void testVerifyHubDataReporting_Success() throws Exception {
        // Arrange
        ServerMetrics metrics = new ServerMetrics();
        metrics.setServerId(testServer.getId());
        metrics.setTimestamp(LocalDateTime.now().minusSeconds(10)); // 10秒前的数据
        metrics.setCpuUsage(45.5);
        metrics.setMemoryUsage(60.2);
        metrics.setDiskUsage(35.8);

        when(metricsHubClient.getLatestMetrics(testServer.getId())).thenReturn(metrics);

        // Act
        java.lang.reflect.Method method = AgentDeployService.class.getDeclaredMethod(
                "verifyHubDataReporting", Server.class, AgentDeployment.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(agentDeployService, testServer, testDeployment);

        // Assert
        assertTrue(result, "Hub数据验证应该成功");
        verify(metricsHubClient).getLatestMetrics(testServer.getId());
        assertTrue(testDeployment.getDeploymentLog().contains("发现最新数据"), "日志应包含成功信息");
    }

    /**
     * 测试: Hub数据验证 - 数据过旧（超过60秒）
     */
    @Test
    void testVerifyHubDataReporting_OldData() throws Exception {
        // Arrange
        ServerMetrics metrics = new ServerMetrics();
        metrics.setServerId(testServer.getId());
        metrics.setTimestamp(LocalDateTime.now().minusSeconds(90)); // 90秒前的旧数据
        metrics.setCpuUsage(45.5);

        when(metricsHubClient.getLatestMetrics(testServer.getId())).thenReturn(metrics);

        // Act
        java.lang.reflect.Method method = AgentDeployService.class.getDeclaredMethod(
                "verifyHubDataReporting", Server.class, AgentDeployment.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(agentDeployService, testServer, testDeployment);

        // Assert
        assertFalse(result, "Hub数据验证应该失败（数据过旧）");
        verify(metricsHubClient, atLeast(1)).getLatestMetrics(testServer.getId());
        assertTrue(testDeployment.getDeploymentLog().contains("数据过旧"), "日志应包含数据过旧信息");
    }

    /**
     * 测试: Hub数据验证 - 未查询到数据，但重试后成功
     */
    @Test
    void testVerifyHubDataReporting_RetrySuccess() throws Exception {
        // Arrange
        ServerMetrics latestMetrics = new ServerMetrics();
        latestMetrics.setServerId(testServer.getId());
        latestMetrics.setTimestamp(LocalDateTime.now().minusSeconds(5));
        latestMetrics.setCpuUsage(45.5);
        latestMetrics.setMemoryUsage(60.2);

        // 第一次返回null（未查询到数据），第二次返回最新数据
        when(metricsHubClient.getLatestMetrics(testServer.getId()))
                .thenReturn(null)
                .thenReturn(latestMetrics);

        // Act
        java.lang.reflect.Method method = AgentDeployService.class.getDeclaredMethod(
                "verifyHubDataReporting", Server.class, AgentDeployment.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(agentDeployService, testServer, testDeployment);

        // Assert
        assertTrue(result, "Hub数据验证应该成功（重试后成功）");
        verify(metricsHubClient, times(2)).getLatestMetrics(testServer.getId());
        assertTrue(testDeployment.getDeploymentLog().contains("发现最新数据"), "日志应包含成功信息");
    }

    /**
     * 测试: Hub数据验证 - 所有重试均失败
     */
    @Test
    void testVerifyHubDataReporting_AllRetriesFailed() throws Exception {
        // Arrange
        when(metricsHubClient.getLatestMetrics(testServer.getId())).thenReturn(null);

        // Act
        java.lang.reflect.Method method = AgentDeployService.class.getDeclaredMethod(
                "verifyHubDataReporting", Server.class, AgentDeployment.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(agentDeployService, testServer, testDeployment);

        // Assert
        assertFalse(result, "Hub数据验证应该失败（所有重试均失败）");
        verify(metricsHubClient, times(6)).getLatestMetrics(testServer.getId());
        assertTrue(testDeployment.getDeploymentLog().contains("所有尝试均未成功"), "日志应包含失败信息");
    }

    /**
     * 测试: Hub数据验证 - Hub API 异常
     */
    @Test
    void testVerifyHubDataReporting_HubException() throws Exception {
        // Arrange
        when(metricsHubClient.getLatestMetrics(testServer.getId()))
                .thenThrow(new RuntimeException("Hub service unavailable"));

        // Act
        java.lang.reflect.Method method = AgentDeployService.class.getDeclaredMethod(
                "verifyHubDataReporting", Server.class, AgentDeployment.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(agentDeployService, testServer, testDeployment);

        // Assert
        assertFalse(result, "Hub数据验证应该失败（Hub异常）");
        verify(metricsHubClient, times(6)).getLatestMetrics(testServer.getId());
        assertTrue(testDeployment.getDeploymentLog().contains("异常"), "日志应包含异常信息");
    }

    /**
     * 测试: Hub数据验证 - 最大重试次数验证
     */
    @Test
    void testVerifyHubDataReporting_MaxRetriesCheck() throws Exception {
        // Arrange
        when(metricsHubClient.getLatestMetrics(testServer.getId())).thenReturn(null);

        // Act
        java.lang.reflect.Method method = AgentDeployService.class.getDeclaredMethod(
                "verifyHubDataReporting", Server.class, AgentDeployment.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(agentDeployService, testServer, testDeployment);

        // Assert
        assertFalse(result, "Hub数据验证应该失败");
        // 验证正好调用6次（最大重试次数）
        verify(metricsHubClient, times(6)).getLatestMetrics(testServer.getId());
        // 验证日志包含6次尝试信息
        String log = testDeployment.getDeploymentLog();
        assertTrue(log.contains("第 1 次尝试"), "日志应包含第1次尝试");
        assertTrue(log.contains("第 6 次尝试"), "日志应包含第6次尝试");
        assertFalse(log.contains("第 7 次尝试"), "日志不应包含第7次尝试");
    }

    /**
     * 测试: Hub数据验证 - 数据时间戳边界测试（正好60秒）
     */
    @Test
    void testVerifyHubDataReporting_TimestampBoundary() throws Exception {
        // Arrange
        ServerMetrics metrics = new ServerMetrics();
        metrics.setServerId(testServer.getId());
        metrics.setTimestamp(LocalDateTime.now().minusSeconds(60)); // 正好60秒前
        metrics.setCpuUsage(45.5);

        when(metricsHubClient.getLatestMetrics(testServer.getId())).thenReturn(metrics);

        // Act
        java.lang.reflect.Method method = AgentDeployService.class.getDeclaredMethod(
                "verifyHubDataReporting", Server.class, AgentDeployment.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(agentDeployService, testServer, testDeployment);

        // Assert
        assertTrue(result, "Hub数据验证应该成功（60秒边界内）");
        verify(metricsHubClient).getLatestMetrics(testServer.getId());
        assertTrue(testDeployment.getDeploymentLog().contains("发现最新数据"), "日志应包含成功信息");
    }
}
