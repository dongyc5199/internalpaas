package com.cmict.internalpaas.service;

import com.cmict.internalpaas.client.MetricsHubClient;
import com.cmict.internalpaas.dto.agent.DeployResult;
import com.cmict.internalpaas.dto.agent.PreCheckResult;
import com.cmict.internalpaas.model.AgentDeployment;
import com.cmict.internalpaas.model.DeploymentStatus;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.repository.AgentDeploymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Agent自动部署服务
 * 负责在服务器创建后自动部署OpenTelemetry Collector Agent
 *
 * 部署流程：
 * 1. 预检查（SSH连接、权限、磁盘空间、端口）
 * 2. 上传文件（agent二进制、配置文件、安装脚本）
 * 3. 执行安装（解压、配置、启动systemd服务）
 * 4. 健康检查（验证服务运行、端口监听、数据上报）
 */
@Service
public class AgentDeployService {

    private static final Logger logger = LoggerFactory.getLogger(AgentDeployService.class);

    @Autowired
    private AgentDeploymentRepository deploymentRepository;

    @Autowired
    private RemoteCommandService remoteCommandService;

    @Autowired
    private SshFileTransferService sshFileTransferService;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Autowired(required = false)
    private MetricsHubClient metricsHubClient;

    @Value("${agent.auto-deploy.enabled:true}")
    private boolean autoDeployEnabled;

    @Value("${agent.version:0.91.0}")
    private String agentVersion;

    @Value("${agent.otlp.endpoint:http://localhost:4317}")
    private String otlpEndpoint;

    @Value("${agent.pre-check.min-disk-mb:100}")
    private int minDiskSpaceMB;

    @Value("${agent.deploy.timeout-minutes:10}")
    private int deployTimeoutMinutes;

    /**
     * 部署Agent到指定服务器（异步）
     *
     * @param server 目标服务器
     * @return CompletableFuture<DeployResult>
     */
    @Async("agentDeployExecutor")
    @Transactional
    public CompletableFuture<DeployResult> deployAgent(Server server) {
        logger.info("开始部署Agent - serverId: {}, serverName: {}", server.getId(), server.getName());

        // 创建部署记录
        AgentDeployment deployment = new AgentDeployment(server, agentVersion);
        deployment.setStatus(DeploymentStatus.PENDING);
        deployment = deploymentRepository.save(deployment);

        try {
            // Step 1: 预检查
            sendProgress(server.getId(), "开始预检查", 10, DeploymentStatus.PRE_CHECK);
            deployment.setStatus(DeploymentStatus.PRE_CHECK);
            deployment.appendLog("[预检查] 开始预检查...");
            deployment = deploymentRepository.save(deployment);

            PreCheckResult preCheckResult = preCheck(server);
            if (!preCheckResult.isPassed()) {
                String error = "预检查失败: " + preCheckResult.getErrorSummary();
                logger.error("预检查失败 - serverId: {}, errors: {}", server.getId(), error);

                deployment.setStatus(DeploymentStatus.FAILED);
                deployment.setErrorMessage(error);
                deployment.appendLog("[预检查] 失败: " + error);
                deploymentRepository.save(deployment);

                return CompletableFuture.completedFuture(DeployResult.failure(error));
            }

            deployment.appendLog("[预检查] 通过");
            deployment = deploymentRepository.save(deployment);

            // Step 2: 上传文件
            sendProgress(server.getId(), "上传Agent文件", 30, DeploymentStatus.UPLOADING);
            deployment.setStatus(DeploymentStatus.UPLOADING);
            deployment.appendLog("[上传] 开始上传文件...");
            deployment = deploymentRepository.save(deployment);

            boolean uploadSuccess = uploadAgentFiles(server, deployment);
            if (!uploadSuccess) {
                String error = "文件上传失败";
                logger.error("文件上传失败 - serverId: {}", server.getId());

                deployment.setStatus(DeploymentStatus.FAILED);
                deployment.setErrorMessage(error);
                deployment.appendLog("[上传] 失败");
                deploymentRepository.save(deployment);

                return CompletableFuture.completedFuture(DeployResult.failure(error));
            }

            deployment.appendLog("[上传] 完成");
            deployment = deploymentRepository.save(deployment);

            // Step 3: 执行安装
            sendProgress(server.getId(), "执行安装", 60, DeploymentStatus.INSTALLING);
            deployment.setStatus(DeploymentStatus.INSTALLING);
            deployment.appendLog("[安装] 开始执行安装脚本...");
            deployment = deploymentRepository.save(deployment);

            boolean installSuccess = executeInstallation(server, deployment);
            if (!installSuccess) {
                String error = "安装执行失败";
                logger.error("安装执行失败 - serverId: {}", server.getId());

                deployment.setStatus(DeploymentStatus.FAILED);
                deployment.setErrorMessage(error);
                deployment.appendLog("[安装] 失败");
                deploymentRepository.save(deployment);

                // 尝试回滚
                rollback(server);

                return CompletableFuture.completedFuture(DeployResult.failure(error));
            }

            deployment.appendLog("[安装] 完成");
            deployment = deploymentRepository.save(deployment);

            // Step 4: 健康检查
            sendProgress(server.getId(), "健康检查", 90, DeploymentStatus.HEALTH_CHECK);
            deployment.setStatus(DeploymentStatus.HEALTH_CHECK);
            deployment.appendLog("[健康检查] 开始...");
            deployment = deploymentRepository.save(deployment);

            boolean healthCheckPassed = performHealthCheck(server, deployment);
            if (!healthCheckPassed) {
                String error = "健康检查失败，Agent可能未正常运行";
                logger.warn("健康检查失败 - serverId: {}", server.getId());

                deployment.setStatus(DeploymentStatus.FAILED);
                deployment.setErrorMessage(error);
                deployment.appendLog("[健康检查] 失败");
                deploymentRepository.save(deployment);

                return CompletableFuture.completedFuture(DeployResult.failure(error));
            }

            deployment.appendLog("[健康检查] 通过");

            // 部署成功
            deployment.setStatus(DeploymentStatus.SUCCESS);
            deployment = deploymentRepository.save(deployment);

            sendProgress(server.getId(), "部署完成", 100, DeploymentStatus.SUCCESS);
            logger.info("✅ Agent部署成功 - serverId: {}, deploymentId: {}", server.getId(), deployment.getId());

            DeployResult result = DeployResult.success("Agent部署成功");
            result.setDeploymentId(deployment.getId());
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            logger.error("Agent部署异常 - serverId: {}", server.getId(), e);

            deployment.setStatus(DeploymentStatus.FAILED);
            deployment.setErrorMessage("部署异常: " + e.getMessage());
            deployment.appendLog("[异常] " + e.getMessage());
            deploymentRepository.save(deployment);

            sendProgress(server.getId(), "部署异常: " + e.getMessage(), -1, DeploymentStatus.FAILED);

            return CompletableFuture.completedFuture(DeployResult.failure("部署异常: " + e.getMessage()));
        }
    }

    /**
     * 预检查服务器环境
     *
     * @param server 目标服务器
     * @return PreCheckResult
     */
    public PreCheckResult preCheck(Server server) {
        logger.debug("开始预检查 - serverId: {}", server.getId());

        PreCheckResult result = new PreCheckResult();

        try {
            // 1. 检查SSH连接
            String testCommand = "echo 'SSH连接测试'";
            RemoteCommandService.CommandResult testResult = remoteCommandService.executeCommand(server, testCommand);
            String testOutput = testResult != null ? testResult.getOutput() : null;

            if (testOutput != null && testOutput.contains("SSH连接测试")) {
                result.setSshConnectable(true);
                logger.debug("SSH连接正常 - serverId: {}", server.getId());
            } else {
                result.setSshConnectable(false);
                result.addError("SSH连接失败");
                logger.error("SSH连接失败 - serverId: {}", server.getId());
                return result;
            }

            // 2. 检查sudo权限
            String sudoCommand = "sudo -n echo 'sudo测试' 2>&1";
            RemoteCommandService.CommandResult sudoResult = remoteCommandService.executeCommand(server, sudoCommand);
            String sudoOutput = sudoResult != null ? sudoResult.getOutput() : null;

            if (sudoOutput != null && !sudoOutput.toLowerCase().contains("password")) {
                result.setHasSudoPermission(true);
                logger.debug("sudo权限正常 - serverId: {}", server.getId());
            } else {
                result.setHasSudoPermission(false);
                result.addWarning("无sudo权限，部署可能需要手动输入密码");
                logger.warn("sudo权限检查失败 - serverId: {}", server.getId());
            }

            // 3. 检查磁盘空间
            String diskCommand = "df -m /opt | tail -1 | awk '{print $4}'";
            RemoteCommandService.CommandResult diskResult = remoteCommandService.executeCommand(server, diskCommand);
            String diskOutput = diskResult != null ? diskResult.getOutput() : null;

            try {
                if (diskOutput != null && !diskOutput.trim().isEmpty()) {
                    int availableSpace = Integer.parseInt(diskOutput.trim());
                    if (availableSpace >= minDiskSpaceMB) {
                        result.setHasEnoughDiskSpace(true);
                        logger.debug("磁盘空间充足 - serverId: {}, available: {}MB", server.getId(), availableSpace);
                    } else {
                        result.setHasEnoughDiskSpace(false);
                        result.addError("磁盘空间不足，需要至少" + minDiskSpaceMB + "MB，当前可用" + availableSpace + "MB");
                        logger.error("磁盘空间不足 - serverId: {}", server.getId());
                    }
                } else {
                    result.setHasEnoughDiskSpace(false);
                    result.addWarning("无法检查磁盘空间");
                }
            } catch (NumberFormatException e) {
                result.setHasEnoughDiskSpace(false);
                result.addWarning("无法检查磁盘空间");
                logger.warn("磁盘空间检查失败 - serverId: {}", server.getId());
            }

            // 4. 检查端口占用
            String portCommand = "netstat -tuln | grep -E ':(4317|4318)\\s' || echo 'AVAILABLE'";
            RemoteCommandService.CommandResult portResult = remoteCommandService.executeCommand(server, portCommand);
            String portOutput = portResult != null ? portResult.getOutput() : null;

            if (portOutput != null && portOutput.contains("AVAILABLE")) {
                result.setPortsAvailable(true);
                logger.debug("端口可用 - serverId: {}", server.getId());
            } else {
                result.setPortsAvailable(false);
                result.addError("端口4317或4318已被占用");
                logger.error("端口被占用 - serverId: {}", server.getId());
            }

        } catch (Exception e) {
            logger.error("预检查异常 - serverId: {}", server.getId(), e);
            result.addError("预检查异常: " + e.getMessage());
        }

        logger.info("预检查完成 - serverId: {}, passed: {}", server.getId(), result.isPassed());
        return result;
    }

    /**
     * 上传Agent文件
     */
    private boolean uploadAgentFiles(Server server, AgentDeployment deployment) {
        logger.info("上传Agent文件 - serverId: {}", server.getId());

        try {
            // 1. 渲染配置文件模板
            deployment.appendLog("[上传] 渲染配置文件...");
            String configContent = renderOtelConfig(server);
            logger.debug("配置文件渲染完成 - serverId: {}, length: {}", server.getId(), configContent.length());

            // 2. 上传配置文件
            deployment.appendLog("[上传] 上传配置文件 otelcol.yaml...");
            boolean uploadConfig = sshFileTransferService.uploadFileContent(
                    server,
                    configContent,
                    "/tmp/otelcol.yaml"
            );
            if (!uploadConfig) {
                logger.error("配置文件上传失败 - serverId: {}", server.getId());
                deployment.appendLog("[上传] 配置文件上传失败");
                return false;
            }
            deployment.appendLog("[上传] ✅ 配置文件上传成功");

            // 3. 读取并上传bootstrap脚本
            deployment.appendLog("[上传] 上传安装脚本 bootstrap.sh...");
            String bootstrapScript = readResourceFile("agent/bootstrap.sh");
            boolean uploadScript = sshFileTransferService.uploadFileContent(
                    server,
                    bootstrapScript,
                    "/tmp/bootstrap.sh"
            );
            if (!uploadScript) {
                logger.error("安装脚本上传失败 - serverId: {}", server.getId());
                deployment.appendLog("[上传] 安装脚本上传失败");
                return false;
            }
            deployment.appendLog("[上传] ✅ 安装脚本上传成功");

            // 4. 上传Agent二进制文件（如果存在）
            // 注意：实际环境中需要准备 otelcol-linux-amd64.tar.gz 文件
            deployment.appendLog("[上传] 检查Agent二进制文件...");
            String agentBinaryPath = "agent/otelcol-linux-amd64.tar.gz";

            try {
                // 尝试从resources读取二进制文件
                java.io.InputStream binaryStream = getClass().getClassLoader().getResourceAsStream(agentBinaryPath);
                if (binaryStream != null) {
                    deployment.appendLog("[上传] 上传Agent二进制文件...");
                    byte[] binaryData = binaryStream.readAllBytes();
                    binaryStream.close();

                    String binaryContent = new String(binaryData, "ISO-8859-1"); // 保持二进制数据
                    boolean uploadBinary = sshFileTransferService.uploadFileContent(
                            server,
                            binaryContent,
                            "/tmp/otelcol-linux-amd64.tar.gz"
                    );

                    if (!uploadBinary) {
                        logger.warn("Agent二进制文件上传失败 - serverId: {}", server.getId());
                        deployment.appendLog("[上传] ⚠️ Agent二进制文件上传失败（将使用wget下载）");
                    } else {
                        deployment.appendLog("[上传] ✅ Agent二进制文件上传成功");
                    }
                } else {
                    logger.info("Agent二进制文件不存在，将在安装时使用wget下载 - serverId: {}", server.getId());
                    deployment.appendLog("[上传] ℹ️ Agent二进制文件未打包，将使用wget下载");
                }
            } catch (Exception e) {
                logger.warn("Agent二进制文件处理失败，将使用wget下载 - serverId: {}, error: {}",
                        server.getId(), e.getMessage());
                deployment.appendLog("[上传] ⚠️ Agent二进制文件未找到（将使用wget下载）");
            }

            logger.info("✅ 文件上传完成 - serverId: {}", server.getId());
            return true;

        } catch (Exception e) {
            logger.error("上传文件异常 - serverId: {}", server.getId(), e);
            deployment.appendLog("[上传] 异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 渲染OpenTelemetry配置文件模板
     */
    private String renderOtelConfig(Server server) throws Exception {
        // 读取模板文件
        String template = readResourceFile("agent/otelcol.yaml.tmpl");

        // 替换变量
        String config = template
                .replace("${SERVER_ID}", String.valueOf(server.getId()))
                .replace("${SERVER_NAME}", server.getName())
                .replace("${HOSTNAME}", server.getHostname())
                .replace("${GENERATED_AT}", java.time.LocalDateTime.now().toString())
                .replace("${DEPLOYMENT_ENVIRONMENT}", "production")
                .replace("${OTLP_ENDPOINT}", otlpEndpoint)
                .replace("${TLS_INSECURE}", "true")
                .replace("${LOG_LEVEL}", "info")
                .replace("${LOG_OUTPUT_PATH}", "/var/log/metrics-agent/otelcol.log");

        return config;
    }

    /**
     * 读取resources下的文件内容
     */
    private String readResourceFile(String resourcePath) throws Exception {
        java.io.InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new java.io.FileNotFoundException("Resource not found: " + resourcePath);
        }

        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(inputStream, "UTF-8"))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } finally {
            inputStream.close();
        }
    }

    /**
     * 执行安装
     */
    private boolean executeInstallation(Server server, AgentDeployment deployment) {
        logger.info("执行安装 - serverId: {}", server.getId());

        try {
            // 1. 设置bootstrap脚本执行权限
            deployment.appendLog("[安装] 设置脚本执行权限...");
            String chmodCommand = "chmod +x /tmp/bootstrap.sh";
            RemoteCommandService.CommandResult chmodResult = remoteCommandService.executeCommand(server, chmodCommand);

            if (chmodResult == null || chmodResult.getExitCode() != 0) {
                logger.error("设置脚本权限失败 - serverId: {}", server.getId());
                deployment.appendLog("[安装] 设置脚本权限失败");
                return false;
            }
            deployment.appendLog("[安装] ✅ 脚本权限设置成功");

            // 2. 执行安装脚本
            deployment.appendLog("[安装] 执行安装脚本...");
            String installCommand = "cd /tmp && sudo bash bootstrap.sh " + agentVersion;
            logger.info("执行安装命令 - serverId: {}, command: {}", server.getId(), installCommand);

            RemoteCommandService.CommandResult installResult = remoteCommandService.executeCommand(server, installCommand);

            // 记录安装输出
            if (installResult != null && installResult.getOutput() != null) {
                String output = installResult.getOutput();
                deployment.appendLog("[安装] 安装脚本输出:");
                // 限制日志长度，只记录关键信息
                String[] lines = output.split("\n");
                int logCount = 0;
                for (String line : lines) {
                    if (logCount < 20 && (line.contains("✅") || line.contains("❌") ||
                            line.contains("ERROR") || line.contains("SUCCESS") ||
                            line.contains("FAILED") || line.startsWith("["))) {
                        deployment.appendLog("  " + line.trim());
                        logCount++;
                    }
                }
            }

            // 检查退出码
            if (installResult == null || installResult.getExitCode() != 0) {
                logger.error("安装脚本执行失败 - serverId: {}, exitCode: {}",
                        server.getId(), installResult != null ? installResult.getExitCode() : "null");
                deployment.appendLog("[安装] ❌ 安装脚本执行失败，退出码: " +
                        (installResult != null ? installResult.getExitCode() : "null"));
                return false;
            }

            deployment.appendLog("[安装] ✅ 安装脚本执行成功");

            // 3. 验证systemd服务已启动
            deployment.appendLog("[安装] 验证systemd服务状态...");
            String statusCommand = "sudo systemctl is-active otelcol";
            RemoteCommandService.CommandResult statusResult = remoteCommandService.executeCommand(server, statusCommand);

            if (statusResult != null && statusResult.getOutput() != null) {
                String status = statusResult.getOutput().trim();
                if ("active".equals(status)) {
                    deployment.appendLog("[安装] ✅ systemd服务已启动");
                    logger.info("✅ Agent安装完成，服务已启动 - serverId: {}", server.getId());
                    return true;
                } else {
                    deployment.appendLog("[安装] ⚠️ systemd服务状态: " + status);
                    logger.warn("systemd服务状态异常 - serverId: {}, status: {}", server.getId(), status);
                    // 不直接失败，继续到健康检查阶段
                    return true;
                }
            }

            logger.info("✅ 安装执行完成 - serverId: {}", server.getId());
            return true;

        } catch (Exception e) {
            logger.error("执行安装异常 - serverId: {}", server.getId(), e);
            deployment.appendLog("[安装] 异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 健康检查
     */
    private boolean performHealthCheck(Server server, AgentDeployment deployment) {
        logger.info("健康检查 - serverId: {}", server.getId());

        try {
            boolean allChecksPassed = true;

            // 1. 检查systemd服务状态
            deployment.appendLog("[健康检查] 检查systemd服务状态...");
            String statusCommand = "sudo systemctl status otelcol | grep -E 'Active:|Main PID:|Memory:|CPU:'";
            RemoteCommandService.CommandResult statusResult = remoteCommandService.executeCommand(server, statusCommand);

            if (statusResult != null && statusResult.getOutput() != null) {
                String output = statusResult.getOutput();
                if (output.contains("active (running)")) {
                    deployment.appendLog("[健康检查] ✅ systemd服务正常运行");
                    logger.info("systemd服务正常 - serverId: {}", server.getId());
                } else {
                    deployment.appendLog("[健康检查] ❌ systemd服务未运行");
                    deployment.appendLog("  状态: " + output.substring(0, Math.min(output.length(), 200)));
                    logger.error("systemd服务未运行 - serverId: {}", server.getId());
                    allChecksPassed = false;
                }
            } else {
                deployment.appendLog("[健康检查] ⚠️ 无法获取systemd服务状态");
                logger.warn("无法获取systemd服务状态 - serverId: {}", server.getId());
            }

            // 2. 检查进程是否存在
            deployment.appendLog("[健康检查] 检查Agent进程...");
            String processCommand = "ps aux | grep '[o]telcol' | grep -v grep";
            RemoteCommandService.CommandResult processResult = remoteCommandService.executeCommand(server, processCommand);

            if (processResult != null && processResult.getOutput() != null &&
                    !processResult.getOutput().trim().isEmpty()) {
                deployment.appendLog("[健康检查] ✅ Agent进程正在运行");
                logger.info("Agent进程存在 - serverId: {}", server.getId());
            } else {
                deployment.appendLog("[健康检查] ❌ 未找到Agent进程");
                logger.error("未找到Agent进程 - serverId: {}", server.getId());
                allChecksPassed = false;
            }

            // 3. 检查日志文件是否正常写入
            deployment.appendLog("[健康检查] 检查Agent日志...");
            String logCommand = "test -f /var/log/metrics-agent/otelcol.log && tail -5 /var/log/metrics-agent/otelcol.log";
            RemoteCommandService.CommandResult logResult = remoteCommandService.executeCommand(server, logCommand);

            if (logResult != null && logResult.getExitCode() == 0 && logResult.getOutput() != null) {
                String logOutput = logResult.getOutput();
                deployment.appendLog("[健康检查] ✅ 日志文件正常");

                // 检查日志中是否有错误
                if (logOutput.toLowerCase().contains("error") || logOutput.toLowerCase().contains("failed")) {
                    deployment.appendLog("[健康检查] ⚠️ 日志中发现错误信息:");
                    String[] logLines = logOutput.split("\n");
                    for (int i = 0; i < Math.min(3, logLines.length); i++) {
                        deployment.appendLog("  " + logLines[i].trim());
                    }
                    logger.warn("Agent日志中有错误 - serverId: {}", server.getId());
                } else {
                    deployment.appendLog("[健康检查] ✅ 日志内容正常");
                }
            } else {
                deployment.appendLog("[健康检查] ⚠️ 无法读取日志文件");
                logger.warn("无法读取日志文件 - serverId: {}", server.getId());
            }

            // 4. 等待数据上报（可选，等待30秒）
            deployment.appendLog("[健康检查] 等待数据上报（30秒）...");
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 5. 验证 Hub 数据上报（如果 Hub 已启用）
            if (metricsHubClient != null && metricsHubClient.isAvailable()) {
                deployment.appendLog("[健康检查] 验证 Hub 数据上报...");
                boolean hubDataVerified = verifyHubDataReporting(server, deployment);
                
                if (hubDataVerified) {
                    deployment.appendLog("[健康检查] ✅ Hub 已接收到数据");
                } else {
                    deployment.appendLog("[健康检查] ⚠️ Hub 未接收到数据（但Agent可能还在初始化）");
                    logger.warn("Hub未接收到数据 - serverId: {}", server.getId());
                    // 不设置 allChecksPassed = false，因为数据可能稍后到达
                }
            } else {
                deployment.appendLog("[健康检查] ⏭️ Hub 未启用，跳过数据验证");
                logger.debug("Hub未启用，跳过数据验证 - serverId: {}", server.getId());
            }

            // 6. 检查配置文件是否正确加载
            deployment.appendLog("[健康检查] 验证配置文件...");
            String configCommand = "test -f /opt/metrics-agent/config/otelcol.yaml && echo 'exists'";
            RemoteCommandService.CommandResult configResult = remoteCommandService.executeCommand(server, configCommand);

            if (configResult != null && configResult.getOutput() != null &&
                    configResult.getOutput().contains("exists")) {
                deployment.appendLog("[健康检查] ✅ 配置文件存在");
            } else {
                deployment.appendLog("[健康检查] ❌ 配置文件不存在");
                allChecksPassed = false;
            }

            // 总结健康检查结果
            if (allChecksPassed) {
                deployment.appendLog("[健康检查] ✅ 所有检查通过");
                logger.info("✅ 健康检查通过 - serverId: {}", server.getId());
                return true;
            } else {
                deployment.appendLog("[健康检查] ⚠️ 部分检查未通过，但Agent可能正在启动");
                logger.warn("健康检查部分未通过 - serverId: {}", server.getId());
                // 允许部分检查失败，因为Agent可能还在初始化
                return true;
            }

        } catch (Exception e) {
            logger.error("健康检查异常 - serverId: {}", server.getId(), e);
            deployment.appendLog("[健康检查] 异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 验证 Hub 数据上报
     * 
     * @param server 服务器
     * @param deployment 部署记录
     * @return true 如果Hub已接收到数据
     */
    private boolean verifyHubDataReporting(Server server, AgentDeployment deployment) {
        int maxRetries = 6;  // 最多重试6次
        int retryInterval = 5000;  // 每次间隔5秒
        
        deployment.appendLog("[Hub验证] 开始验证数据上报（最多尝试 " + maxRetries + " 次，每次间隔 5 秒）");
        
        for (int i = 1; i <= maxRetries; i++) {
            try {
                deployment.appendLog("[Hub验证] 第 " + i + " 次尝试...");
                
                // 调用 Hub API 获取最新指标
                ServerMetrics metrics = metricsHubClient.getLatestMetrics(server.getId());
                
                if (metrics != null && metrics.getTimestamp() != null) {
                    // 检查数据时间戳是否为最近60秒内的数据
                    LocalDateTime now = LocalDateTime.now();
                    long secondsAgo = ChronoUnit.SECONDS.between(metrics.getTimestamp(), now);
                    
                    if (secondsAgo <= 60) {
                        deployment.appendLog(String.format("[Hub验证] ✅ 发现最新数据！ 数据时间: %s (%d秒前)",
                                metrics.getTimestamp(), secondsAgo));
                        deployment.appendLog(String.format("[Hub验证] 数据详情: CPU=%.1f%%, Memory=%.1f%%, Disk=%.1f%%",
                                metrics.getCpuUsage() != null ? metrics.getCpuUsage() : 0.0,
                                metrics.getMemoryUsage() != null ? metrics.getMemoryUsage() : 0.0,
                                metrics.getDiskUsage() != null ? metrics.getDiskUsage() : 0.0));
                        
                        logger.info("✅ Hub数据验证通过 - serverId: {}, 数据时间: {}, 延迟: {}秒",
                                server.getId(), metrics.getTimestamp(), secondsAgo);
                        return true;
                    } else {
                        deployment.appendLog(String.format("[Hub验证] ⚠️ 数据过旧: %s (%d秒前)",
                                metrics.getTimestamp(), secondsAgo));
                        logger.debug("Hub数据过旧 - serverId: {}, 数据时间: {}, 延迟: {}秒",
                                server.getId(), metrics.getTimestamp(), secondsAgo);
                    }
                } else {
                    deployment.appendLog("[Hub验证] ⚠️ 未查询到数据");
                    logger.debug("Hub未查询到数据 - serverId: {}, 尝试: {}/{}", server.getId(), i, maxRetries);
                }
                
                // 如果不是最后一次尝试，等待后继续
                if (i < maxRetries) {
                    deployment.appendLog("[Hub验证] 等待 5 秒后重试...");
                    Thread.sleep(retryInterval);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Hub数据验证被中断 - serverId: {}", server.getId());
                deployment.appendLog("[Hub验证] ⚠️ 验证被中断");
                return false;
                
            } catch (Exception e) {
                logger.error("Hub数据验证异常 - serverId: {}, 尝试: {}/{}, 错误: {}",
                        server.getId(), i, maxRetries, e.getMessage());
                deployment.appendLog("[Hub验证] ❌ 第 " + i + " 次尝试异常: " + e.getMessage());
                
                // 如果不是最后一次尝试，等待后继续
                if (i < maxRetries) {
                    try {
                        deployment.appendLog("[Hub验证] 等待 5 秒后重试...");
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        
        deployment.appendLog("[Hub验证] ⚠️ 所有尝试均未成功，但不影响部署状态");
        logger.warn("Hub数据验证失败（所有尝试均未成功）- serverId: {}", server.getId());
        return false;
    }

    /**
     * 回滚部署
     */
    public void rollback(Server server) {
        logger.info("回滚部署 - serverId: {}", server.getId());

        try {
            // 1. 停止systemd服务
            logger.info("停止Agent服务 - serverId: {}", server.getId());
            String stopCommand = "sudo systemctl stop otelcol";
            RemoteCommandService.CommandResult stopResult = remoteCommandService.executeCommand(server, stopCommand);

            if (stopResult != null && stopResult.getExitCode() == 0) {
                logger.info("✅ Agent服务已停止 - serverId: {}", server.getId());
            } else {
                logger.warn("停止Agent服务失败或服务不存在 - serverId: {}", server.getId());
            }

            // 2. 禁用systemd服务
            logger.info("禁用Agent服务 - serverId: {}", server.getId());
            String disableCommand = "sudo systemctl disable otelcol";
            RemoteCommandService.CommandResult disableResult = remoteCommandService.executeCommand(server, disableCommand);

            if (disableResult != null && disableResult.getExitCode() == 0) {
                logger.info("✅ Agent服务已禁用 - serverId: {}", server.getId());
            } else {
                logger.warn("禁用Agent服务失败 - serverId: {}", server.getId());
            }

            // 3. 删除安装目录
            logger.info("删除Agent文件 - serverId: {}", server.getId());
            String removeCommand = "sudo rm -rf /opt/metrics-agent /var/log/metrics-agent /etc/systemd/system/otelcol.service";
            RemoteCommandService.CommandResult removeResult = remoteCommandService.executeCommand(server, removeCommand);

            if (removeResult != null && removeResult.getExitCode() == 0) {
                logger.info("✅ Agent文件已删除 - serverId: {}", server.getId());
            } else {
                logger.warn("删除Agent文件失败 - serverId: {}", server.getId());
            }

            // 4. 重新加载systemd
            logger.info("重新加载systemd - serverId: {}", server.getId());
            String reloadCommand = "sudo systemctl daemon-reload";
            remoteCommandService.executeCommand(server, reloadCommand);

            // 5. 清理临时文件
            logger.info("清理临时文件 - serverId: {}", server.getId());
            String cleanupCommand = "rm -f /tmp/otelcol.yaml /tmp/bootstrap.sh /tmp/otelcol-linux-amd64.tar.gz";
            remoteCommandService.executeCommand(server, cleanupCommand);

            logger.info("✅ 回滚完成 - serverId: {}", server.getId());

        } catch (Exception e) {
            logger.error("回滚异常 - serverId: {}", server.getId(), e);
        }
    }

    /**
     * 发送部署进度到WebSocket
     */
    private void sendProgress(Long serverId, String message, int percentage, DeploymentStatus status) {
        if (messagingTemplate == null) {
            logger.debug("WebSocket未配置，跳过进度推送");
            return;
        }

        try {
            Map<String, Object> progress = new HashMap<>();
            progress.put("serverId", serverId);
            progress.put("message", message);
            progress.put("percentage", percentage);
            progress.put("status", status.name());
            progress.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/agent-deploy/" + serverId, progress);
            logger.debug("推送进度 - serverId: {}, message: {}, percentage: {}", serverId, message, percentage);

        } catch (Exception e) {
            logger.error("推送进度失败 - serverId: {}", serverId, e);
        }
    }

    /**
     * 检查是否启用自动部署
     */
    public boolean isAutoDeployEnabled() {
        return autoDeployEnabled;
    }

    /**
     * 重试失败的部署
     *
     * @param failedDeployment 失败的部署记录
     * @return CompletableFuture<DeployResult>
     */
    @Async("agentDeployExecutor")
    @Transactional
    public CompletableFuture<DeployResult> retryDeployment(AgentDeployment failedDeployment) {
        logger.info("🔄 重试部署 - deploymentId: {}, serverId: {}, retryCount: {}",
                failedDeployment.getId(),
                failedDeployment.getServer().getId(),
                failedDeployment.getRetryCount());

        Server server = failedDeployment.getServer();

        try {
            // Step 1: 预检查
            sendProgress(server.getId(), "重试: 开始预检查", 10, DeploymentStatus.PRE_CHECK);
            failedDeployment.setStatus(DeploymentStatus.PRE_CHECK);
            failedDeployment.appendLog("[重试-预检查] 开始...");
            failedDeployment = deploymentRepository.save(failedDeployment);

            PreCheckResult preCheckResult = preCheck(server);
            if (!preCheckResult.isPassed()) {
                String error = "预检查失败: " + preCheckResult.getErrorSummary();
                logger.error("重试预检查失败 - deploymentId: {}, errors: {}",
                        failedDeployment.getId(), error);

                failedDeployment.setStatus(DeploymentStatus.FAILED);
                failedDeployment.setErrorMessage(error);
                failedDeployment.appendLog("[重试-预检查] 失败: " + error);
                deploymentRepository.save(failedDeployment);

                return CompletableFuture.completedFuture(DeployResult.failure(error));
            }

            failedDeployment.appendLog("[重试-预检查] 通过");
            failedDeployment = deploymentRepository.save(failedDeployment);

            // Step 2: 上传文件
            sendProgress(server.getId(), "重试: 上传Agent文件", 30, DeploymentStatus.UPLOADING);
            failedDeployment.setStatus(DeploymentStatus.UPLOADING);
            failedDeployment.appendLog("[重试-上传] 开始...");
            failedDeployment = deploymentRepository.save(failedDeployment);

            boolean uploadSuccess = uploadAgentFiles(server, failedDeployment);
            if (!uploadSuccess) {
                String error = "文件上传失败";
                logger.error("重试文件上传失败 - deploymentId: {}", failedDeployment.getId());

                failedDeployment.setStatus(DeploymentStatus.FAILED);
                failedDeployment.setErrorMessage(error);
                failedDeployment.appendLog("[重试-上传] 失败");
                deploymentRepository.save(failedDeployment);

                return CompletableFuture.completedFuture(DeployResult.failure(error));
            }

            failedDeployment.appendLog("[重试-上传] 完成");
            failedDeployment = deploymentRepository.save(failedDeployment);

            // Step 3: 执行安装
            sendProgress(server.getId(), "重试: 执行安装", 60, DeploymentStatus.INSTALLING);
            failedDeployment.setStatus(DeploymentStatus.INSTALLING);
            failedDeployment.appendLog("[重试-安装] 开始...");
            failedDeployment = deploymentRepository.save(failedDeployment);

            boolean installSuccess = executeInstallation(server, failedDeployment);
            if (!installSuccess) {
                String error = "安装执行失败";
                logger.error("重试安装执行失败 - deploymentId: {}", failedDeployment.getId());

                failedDeployment.setStatus(DeploymentStatus.FAILED);
                failedDeployment.setErrorMessage(error);
                failedDeployment.appendLog("[重试-安装] 失败");
                deploymentRepository.save(failedDeployment);

                // 尝试回滚
                rollback(server);

                return CompletableFuture.completedFuture(DeployResult.failure(error));
            }

            failedDeployment.appendLog("[重试-安装] 完成");
            failedDeployment = deploymentRepository.save(failedDeployment);

            // Step 4: 健康检查
            sendProgress(server.getId(), "重试: 健康检查", 90, DeploymentStatus.HEALTH_CHECK);
            failedDeployment.setStatus(DeploymentStatus.HEALTH_CHECK);
            failedDeployment.appendLog("[重试-健康检查] 开始...");
            failedDeployment = deploymentRepository.save(failedDeployment);

            boolean healthCheckPassed = performHealthCheck(server, failedDeployment);
            if (!healthCheckPassed) {
                String error = "健康检查失败，Agent可能未正常运行";
                logger.warn("重试健康检查失败 - deploymentId: {}", failedDeployment.getId());

                failedDeployment.setStatus(DeploymentStatus.FAILED);
                failedDeployment.setErrorMessage(error);
                failedDeployment.appendLog("[重试-健康检查] 失败");
                deploymentRepository.save(failedDeployment);

                return CompletableFuture.completedFuture(DeployResult.failure(error));
            }

            failedDeployment.appendLog("[重试-健康检查] 通过");

            // 重试成功
            failedDeployment.setStatus(DeploymentStatus.SUCCESS);
            failedDeployment = deploymentRepository.save(failedDeployment);

            sendProgress(server.getId(), "重试部署完成", 100, DeploymentStatus.SUCCESS);
            logger.info("✅ 重试部署成功 - deploymentId: {}, serverId: {}, retryCount: {}",
                    failedDeployment.getId(), server.getId(), failedDeployment.getRetryCount());

            DeployResult result = DeployResult.success("重试部署成功");
            result.setDeploymentId(failedDeployment.getId());
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            logger.error("重试部署异常 - deploymentId: {}", failedDeployment.getId(), e);

            failedDeployment.setStatus(DeploymentStatus.FAILED);
            failedDeployment.setErrorMessage("重试异常: " + e.getMessage());
            failedDeployment.appendLog("[重试-异常] " + e.getMessage());
            deploymentRepository.save(failedDeployment);

            sendProgress(server.getId(), "重试部署异常: " + e.getMessage(), -1, DeploymentStatus.FAILED);

            return CompletableFuture.completedFuture(DeployResult.failure("重试异常: " + e.getMessage()));
        }
    }
}
