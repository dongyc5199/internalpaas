package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.agent.PreCheckResult;
import com.cmict.internalpaas.model.AgentDeployment;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.repository.AgentDeploymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AgentDeployService 单元测试
 * 测试Agent部署服务的核心功能
 *
 * @author Dev Debug Platform Team
 * @version 2.0 (阶段2 - Agent自动部署)
 */
@ExtendWith(MockitoExtension.class)
class AgentDeployServiceTest {

    @Mock
    private AgentDeploymentRepository deploymentRepository;

    @Mock
    private RemoteCommandService remoteCommandService;

    @Mock
    private SshFileTransferService sshFileTransferService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

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
        testServer.setSshPort(22);
        testServer.setSshUsername("testuser");
        testServer.setSshPasswordEncrypted("encrypted_password");

        // 创建测试部署记录
        testDeployment = new AgentDeployment(testServer, "0.91.0");
        testDeployment.setId(1L);

        // 设置配置参数
        ReflectionTestUtils.setField(agentDeployService, "autoDeployEnabled", true);
        ReflectionTestUtils.setField(agentDeployService, "agentVersion", "0.91.0");
        ReflectionTestUtils.setField(agentDeployService, "otlpEndpoint", "http://localhost:4317");
        ReflectionTestUtils.setField(agentDeployService, "minDiskSpaceMB", 100);
        ReflectionTestUtils.setField(agentDeployService, "deployTimeoutMinutes", 10);
    }

    /**
     * 测试: 预检查 - SSH连接成功
     */
    @Test
    void testPreCheck_SSHConnectionSuccess() {
        // Arrange
        RemoteCommandService.CommandResult sshResult = new RemoteCommandService.CommandResult(0, "SSH连接测试", "");
        RemoteCommandService.CommandResult sudoResult = new RemoteCommandService.CommandResult(0, "sudo测试", "");
        RemoteCommandService.CommandResult diskResult = new RemoteCommandService.CommandResult(0, "500", "");
        RemoteCommandService.CommandResult portResult = new RemoteCommandService.CommandResult(0, "AVAILABLE", "");

        // Mock所有的预检查命令
        when(remoteCommandService.executeCommand(eq(testServer), argThat(cmd -> cmd.contains("echo 'SSH连接测试'"))))
                .thenReturn(sshResult);
        when(remoteCommandService.executeCommand(eq(testServer), argThat(cmd -> cmd.contains("sudo -n echo"))))
                .thenReturn(sudoResult);
        when(remoteCommandService.executeCommand(eq(testServer), argThat(cmd -> cmd.contains("df -m"))))
                .thenReturn(diskResult);
        when(remoteCommandService.executeCommand(eq(testServer), argThat(cmd -> cmd.contains("netstat"))))
                .thenReturn(portResult);

        // Act
        PreCheckResult result = agentDeployService.preCheck(testServer);

        // Assert
        assertTrue(result.isPassed(), "预检查应该通过");
        assertTrue(result.isSshConnectable(), "SSH应该可连接");
        assertTrue(result.isHasSudoPermission(), "应该有sudo权限");
        assertTrue(result.isHasEnoughDiskSpace(), "磁盘空间应该充足");
        assertTrue(result.isPortsAvailable(), "端口应该可用");
    }

    /**
     * 测试: 预检查 - SSH连接失败
     */
    @Test
    void testPreCheck_SSHConnectionFailed() {
        // Arrange
        RemoteCommandService.CommandResult sshResult = new RemoteCommandService.CommandResult(255, "Connection refused", "");

        when(remoteCommandService.executeCommand(eq(testServer), contains("echo"))).thenReturn(sshResult);

        // Act
        PreCheckResult result = agentDeployService.preCheck(testServer);

        // Assert
        assertFalse(result.isPassed(), "预检查应该失败");
        assertFalse(result.isSshConnectable(), "SSH不应该可连接");
    }

    /**
     * 测试: 预检查 - 磁盘空间不足
     */
    @Test
    void testPreCheck_InsufficientDiskSpace() {
        // Arrange
        RemoteCommandService.CommandResult sshResult = new RemoteCommandService.CommandResult(0, "SSH连接测试", "");
        RemoteCommandService.CommandResult diskResult = new RemoteCommandService.CommandResult(0, "50", ""); // 只有50MB

        when(remoteCommandService.executeCommand(eq(testServer), contains("echo"))).thenReturn(sshResult);
        when(remoteCommandService.executeCommand(eq(testServer), contains("df -m"))).thenReturn(diskResult);

        // Act
        PreCheckResult result = agentDeployService.preCheck(testServer);

        // Assert
        assertFalse(result.isPassed(), "预检查应该失败");
        assertFalse(result.isHasEnoughDiskSpace(), "磁盘空间不应该充足");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("磁盘空间不足")));
    }

    /**
     * 测试: 预检查 - 端口已被占用
     */
    @Test
    void testPreCheck_PortsOccupied() {
        // Arrange
        RemoteCommandService.CommandResult sshResult = new RemoteCommandService.CommandResult(0, "SSH连接测试", "");
        RemoteCommandService.CommandResult diskResult = new RemoteCommandService.CommandResult(0, "500", "");
        RemoteCommandService.CommandResult portResult = new RemoteCommandService.CommandResult(0, "tcp        0      0 0.0.0.0:4317", "");

        when(remoteCommandService.executeCommand(eq(testServer), contains("echo"))).thenReturn(sshResult);
        when(remoteCommandService.executeCommand(eq(testServer), contains("df -m"))).thenReturn(diskResult);
        when(remoteCommandService.executeCommand(eq(testServer), contains("netstat"))).thenReturn(portResult);

        // Act
        PreCheckResult result = agentDeployService.preCheck(testServer);

        // Assert
        assertFalse(result.isPassed(), "预检查应该失败");
        assertFalse(result.isPortsAvailable(), "端口不应该可用");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("端口")));
    }

    /**
     * 测试: 检查是否启用自动部署
     */
    @Test
    void testIsAutoDeployEnabled_WhenEnabled() {
        // Act & Assert
        assertTrue(agentDeployService.isAutoDeployEnabled());
    }

    /**
     * 测试: 检查是否启用自动部署 - 禁用状态
     */
    @Test
    void testIsAutoDeployEnabled_WhenDisabled() {
        // Arrange
        ReflectionTestUtils.setField(agentDeployService, "autoDeployEnabled", false);

        // Act & Assert
        assertFalse(agentDeployService.isAutoDeployEnabled());
    }

    /**
     * 测试: 回滚功能
     */
    @Test
    void testRollback_Success() {
        // Arrange
        RemoteCommandService.CommandResult successResult = new RemoteCommandService.CommandResult(0, "Success", "");

        when(remoteCommandService.executeCommand(eq(testServer), anyString())).thenReturn(successResult);

        // Act
        agentDeployService.rollback(testServer);

        // Assert
        verify(remoteCommandService, times(5)).executeCommand(eq(testServer), anyString());
        verify(remoteCommandService).executeCommand(eq(testServer), contains("systemctl stop"));
        verify(remoteCommandService).executeCommand(eq(testServer), contains("systemctl disable"));
        verify(remoteCommandService).executeCommand(eq(testServer), contains("rm -rf"));
        verify(remoteCommandService).executeCommand(eq(testServer), contains("daemon-reload"));
        verify(remoteCommandService).executeCommand(eq(testServer), contains("rm -f /tmp"));
    }

    /**
     * 测试: 回滚功能 - 部分命令失败
     */
    @Test
    void testRollback_PartialFailure() {
        // Arrange
        RemoteCommandService.CommandResult successResult = new RemoteCommandService.CommandResult(0, "Success", "");
        RemoteCommandService.CommandResult failResult = new RemoteCommandService.CommandResult(1, "Failed", "");

        // 停止服务失败，但其他操作成功
        when(remoteCommandService.executeCommand(eq(testServer), contains("systemctl stop")))
                .thenReturn(failResult);
        when(remoteCommandService.executeCommand(eq(testServer), argThat(arg -> !arg.contains("systemctl stop"))))
                .thenReturn(successResult);

        // Act - 不应该抛出异常
        assertDoesNotThrow(() -> agentDeployService.rollback(testServer));

        // Assert
        verify(remoteCommandService, times(5)).executeCommand(eq(testServer), anyString());
    }

    /**
     * 测试: 配置模板渲染（通过反射测试私有方法）
     */
    @Test
    void testRenderOtelConfig() throws Exception {
        // Arrange
        java.lang.reflect.Method method = AgentDeployService.class.getDeclaredMethod(
                "renderOtelConfig", Server.class);
        method.setAccessible(true);

        // Act
        String config = (String) method.invoke(agentDeployService, testServer);

        // Assert
        assertNotNull(config);
        assertTrue(config.contains("Server ID: 1"), "配置应包含服务器ID");
        assertTrue(config.contains("Server Name: Test Server"), "配置应包含服务器名称");
        assertTrue(config.contains("value: \"1\""), "配置应包含服务器ID标签");
        assertTrue(config.contains("value: \"Test Server\""), "配置应包含服务器名称标签");
        assertTrue(config.contains("value: \"192.168.1.100\""), "配置应包含主机名标签");
        assertTrue(config.contains("endpoint: \"http://localhost:4317\""), "配置应包含OTLP endpoint");
    }
}
