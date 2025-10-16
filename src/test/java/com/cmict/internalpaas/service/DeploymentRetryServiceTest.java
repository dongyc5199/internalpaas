package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.AgentDeployment;
import com.cmict.internalpaas.model.DeploymentStatus;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.repository.AgentDeploymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DeploymentRetryService 单元测试
 * 测试Agent部署重试调度服务的核心功能
 *
 * @author Dev Debug Platform Team
 * @version 2.0 (阶段3 - 重试机制)
 */
@ExtendWith(MockitoExtension.class)
class DeploymentRetryServiceTest {

    @Mock
    private AgentDeploymentRepository deploymentRepository;

    @Mock
    private AgentDeployService agentDeployService;

    @InjectMocks
    private DeploymentRetryService retryService;

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
        testDeployment.setStatus(DeploymentStatus.FAILED);
        testDeployment.setRetryCount(0);
        testDeployment.setUpdatedAt(LocalDateTime.now().minusMinutes(2)); // 2分钟前失败

        // 设置配置参数
        ReflectionTestUtils.setField(retryService, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(retryService, "retryDelayMinutesConfig", "1,5,15");
    }

    /**
     * 测试: 检查并重试失败的部署 - 无失败部署
     */
    @Test
    void testCheckAndRetry_NoFailedDeployments() {
        // Arrange
        when(deploymentRepository.findByStatusAndRetryCountLessThan(DeploymentStatus.FAILED, 3))
                .thenReturn(Collections.emptyList());

        // Act
        retryService.checkAndRetryFailedDeployments();

        // Assert
        verify(deploymentRepository).findByStatusAndRetryCountLessThan(DeploymentStatus.FAILED, 3);
        verify(agentDeployService, never()).retryDeployment(any());
    }

    /**
     * 测试: 检查并重试失败的部署 - 有失败部署且到达重试时间
     */
    @Test
    void testCheckAndRetry_WithFailedDeployment_ShouldRetry() {
        // Arrange
        testDeployment.setUpdatedAt(LocalDateTime.now().minusMinutes(2)); // 2分钟前，超过1分钟延迟
        when(deploymentRepository.findByStatusAndRetryCountLessThan(DeploymentStatus.FAILED, 3))
                .thenReturn(Arrays.asList(testDeployment));
        when(deploymentRepository.save(any(AgentDeployment.class))).thenReturn(testDeployment);

        // Act
        retryService.checkAndRetryFailedDeployments();

        // Assert
        verify(deploymentRepository).findByStatusAndRetryCountLessThan(DeploymentStatus.FAILED, 3);
        verify(agentDeployService).retryDeployment(testDeployment);
        verify(deploymentRepository, atLeastOnce()).save(testDeployment);
        assertEquals(DeploymentStatus.RETRYING, testDeployment.getStatus());
        assertEquals(1, testDeployment.getRetryCount());
    }

    /**
     * 测试: 检查并重试失败的部署 - 未到重试时间
     */
    @Test
    void testCheckAndRetry_WithFailedDeployment_NotYetTime() {
        // Arrange
        testDeployment.setUpdatedAt(LocalDateTime.now().minusSeconds(30)); // 30秒前，未到1分钟
        when(deploymentRepository.findByStatusAndRetryCountLessThan(DeploymentStatus.FAILED, 3))
                .thenReturn(Arrays.asList(testDeployment));

        // Act
        retryService.checkAndRetryFailedDeployments();

        // Assert
        verify(deploymentRepository).findByStatusAndRetryCountLessThan(DeploymentStatus.FAILED, 3);
        verify(agentDeployService, never()).retryDeployment(any());
    }

    /**
     * 测试: 检查并重试失败的部署 - 已达到最大重试次数
     */
    @Test
    void testCheckAndRetry_WithFailedDeployment_MaxRetriesReached() {
        // Arrange
        testDeployment.setRetryCount(3); // 已重试3次
        when(deploymentRepository.findByStatusAndRetryCountLessThan(DeploymentStatus.FAILED, 3))
                .thenReturn(Collections.emptyList()); // 不会返回已达最大重试次数的记录

        // Act
        retryService.checkAndRetryFailedDeployments();

        // Assert
        verify(agentDeployService, never()).retryDeployment(any());
    }

    /**
     * 测试: 第2次重试 - 需要等待5分钟
     */
    @Test
    void testCheckAndRetry_SecondRetry_Needs5Minutes() {
        // Arrange
        testDeployment.setRetryCount(1); // 已重试1次
        testDeployment.setUpdatedAt(LocalDateTime.now().minusMinutes(3)); // 3分钟前，未到5分钟
        when(deploymentRepository.findByStatusAndRetryCountLessThan(DeploymentStatus.FAILED, 3))
                .thenReturn(Arrays.asList(testDeployment));

        // Act
        retryService.checkAndRetryFailedDeployments();

        // Assert
        verify(agentDeployService, never()).retryDeployment(any()); // 未到时间，不重试
    }

    /**
     * 测试: 第2次重试 - 已等待5分钟
     */
    @Test
    void testCheckAndRetry_SecondRetry_After5Minutes() {
        // Arrange
        testDeployment.setRetryCount(1); // 已重试1次
        testDeployment.setUpdatedAt(LocalDateTime.now().minusMinutes(6)); // 6分钟前，超过5分钟
        when(deploymentRepository.findByStatusAndRetryCountLessThan(DeploymentStatus.FAILED, 3))
                .thenReturn(Arrays.asList(testDeployment));
        when(deploymentRepository.save(any(AgentDeployment.class))).thenReturn(testDeployment);

        // Act
        retryService.checkAndRetryFailedDeployments();

        // Assert
        verify(agentDeployService).retryDeployment(testDeployment); // 应该重试
        assertEquals(2, testDeployment.getRetryCount()); // 重试次数应为2
    }

    /**
     * 测试: 手动触发重试 - 成功
     */
    @Test
    void testManualRetry_Success() {
        // Arrange
        when(deploymentRepository.findById(1L)).thenReturn(Optional.of(testDeployment));
        when(deploymentRepository.save(any(AgentDeployment.class))).thenReturn(testDeployment);

        // Act
        boolean result = retryService.manualRetry(1L);

        // Assert
        assertTrue(result);
        verify(agentDeployService).retryDeployment(testDeployment);
    }

    /**
     * 测试: 手动触发重试 - 部署不存在
     */
    @Test
    void testManualRetry_DeploymentNotFound() {
        // Arrange
        when(deploymentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        boolean result = retryService.manualRetry(999L);

        // Assert
        assertFalse(result);
        verify(agentDeployService, never()).retryDeployment(any());
    }

    /**
     * 测试: 手动触发重试 - 部署状态不是失败
     */
    @Test
    void testManualRetry_DeploymentNotFailed() {
        // Arrange
        testDeployment.setStatus(DeploymentStatus.SUCCESS);
        when(deploymentRepository.findById(1L)).thenReturn(Optional.of(testDeployment));

        // Act
        boolean result = retryService.manualRetry(1L);

        // Assert
        assertFalse(result);
        verify(agentDeployService, never()).retryDeployment(any());
    }

    /**
     * 测试: 手动触发重试 - 已达最大重试次数
     */
    @Test
    void testManualRetry_MaxRetriesReached() {
        // Arrange
        testDeployment.setRetryCount(3);
        when(deploymentRepository.findById(1L)).thenReturn(Optional.of(testDeployment));

        // Act
        boolean result = retryService.manualRetry(1L);

        // Assert
        assertFalse(result);
        verify(agentDeployService, never()).retryDeployment(any());
    }

    /**
     * 测试: 重试调度器异常处理
     */
    @Test
    void testCheckAndRetry_WithException() {
        // Arrange
        when(deploymentRepository.findByStatusAndRetryCountLessThan(any(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        // Act - 不应该抛出异常
        assertDoesNotThrow(() -> retryService.checkAndRetryFailedDeployments());

        // Assert
        verify(agentDeployService, never()).retryDeployment(any());
    }

    /**
     * 测试: 批量失败部署的重试
     */
    @Test
    void testCheckAndRetry_MultipleFailedDeployments() {
        // Arrange
        AgentDeployment deployment1 = new AgentDeployment(testServer, "0.91.0");
        deployment1.setId(1L);
        deployment1.setStatus(DeploymentStatus.FAILED);
        deployment1.setRetryCount(0);
        deployment1.setUpdatedAt(LocalDateTime.now().minusMinutes(2));

        AgentDeployment deployment2 = new AgentDeployment(testServer, "0.91.0");
        deployment2.setId(2L);
        deployment2.setStatus(DeploymentStatus.FAILED);
        deployment2.setRetryCount(0);
        deployment2.setUpdatedAt(LocalDateTime.now().minusMinutes(2));

        when(deploymentRepository.findByStatusAndRetryCountLessThan(DeploymentStatus.FAILED, 3))
                .thenReturn(Arrays.asList(deployment1, deployment2));
        when(deploymentRepository.save(any(AgentDeployment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        retryService.checkAndRetryFailedDeployments();

        // Assert
        verify(agentDeployService, times(2)).retryDeployment(any());
        verify(deploymentRepository, atLeast(2)).save(any(AgentDeployment.class));
    }
}
