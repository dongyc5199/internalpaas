package com.cmict.internalpaas.test;

import com.cmict.internalpaas.dto.agent.DeployResult;
import com.cmict.internalpaas.dto.agent.PreCheckResult;
import com.cmict.internalpaas.model.AgentDeployment;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.repository.AgentDeploymentRepository;
import com.cmict.internalpaas.repository.ServerRepository;
import com.cmict.internalpaas.service.AgentDeployService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * Agent部署集成测试工具
 * 用于手动测试Agent自动部署功能
 *
 * 使用方法:
 * 1. 设置环境变量 AGENT_DEPLOY_TEST=true
 * 2. 运行应用: mvn spring-boot:run -Dspring-boot.run.arguments="--agent.deploy.test=true"
 * 3. 按照提示选择测试功能
 *
 * @author Dev Debug Platform Team
 * @version 2.0 (阶段2 - Agent自动部署)
 */
@Component
public class AgentDeploymentTester implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AgentDeploymentTester.class);

    @Autowired
    private AgentDeployService agentDeployService;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private AgentDeploymentRepository deploymentRepository;

    @Autowired
    private ApplicationContext applicationContext;

    private boolean testMode = false;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否启用测试模式
        for (String arg : args) {
            if ("--agent.deploy.test=true".equals(arg)) {
                testMode = true;
                break;
            }
        }

        if (!testMode) {
            return;
        }

        logger.info("=================================================");
        logger.info("   Agent部署集成测试工具已启动");
        logger.info("=================================================");

        try {
            runTestMenu();
        } catch (Exception e) {
            logger.error("测试工具运行异常", e);
        } finally {
            // 退出应用
            System.exit(0);
        }
    }

    /**
     * 显示测试菜单
     */
    private void runTestMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=================================================");
            System.out.println("Agent部署测试菜单");
            System.out.println("=================================================");
            System.out.println("1. 列出所有服务器");
            System.out.println("2. 对指定服务器执行预检查");
            System.out.println("3. 对指定服务器执行完整部署");
            System.out.println("4. 查看部署历史");
            System.out.println("5. 查看部署日志");
            System.out.println("6. 对指定服务器执行回滚");
            System.out.println("7. 测试配置文件渲染");
            System.out.println("0. 退出");
            System.out.println("=================================================");
            System.out.print("请选择操作 (0-7): ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // 消费换行符

                switch (choice) {
                    case 1:
                        listServers();
                        break;
                    case 2:
                        testPreCheck(scanner);
                        break;
                    case 3:
                        testFullDeployment(scanner);
                        break;
                    case 4:
                        viewDeploymentHistory(scanner);
                        break;
                    case 5:
                        viewDeploymentLog(scanner);
                        break;
                    case 6:
                        testRollback(scanner);
                        break;
                    case 7:
                        testConfigRendering(scanner);
                        break;
                    case 0:
                        running = false;
                        System.out.println("退出测试工具...");
                        break;
                    default:
                        System.out.println("无效选择，请重试");
                }
            } catch (Exception e) {
                logger.error("操作异常", e);
                scanner.nextLine(); // 清空输入缓冲
                System.out.println("操作失败: " + e.getMessage());
            }
        }

        scanner.close();
    }

    /**
     * 列出所有服务器
     */
    private void listServers() {
        System.out.println("\n=== 服务器列表 ===");
        List<Server> servers = serverRepository.findAll();

        if (servers.isEmpty()) {
            System.out.println("没有找到服务器");
            return;
        }

        for (Server server : servers) {
            System.out.printf("ID: %d | 名称: %s | 主机: %s | 端口: %d | 用户: %s\n",
                    server.getId(),
                    server.getName(),
                    server.getHostname(),
                    server.getSshPort() != null ? server.getSshPort() : 22,
                    server.getSshUsername());
        }
    }

    /**
     * 测试预检查
     */
    private void testPreCheck(Scanner scanner) {
        System.out.print("请输入服务器ID: ");
        Long serverId = scanner.nextLong();
        scanner.nextLine();

        Server server = serverRepository.findById(serverId).orElse(null);
        if (server == null) {
            System.out.println("❌ 服务器不存在");
            return;
        }

        System.out.println("\n开始预检查 - 服务器: " + server.getName());
        System.out.println("=================================================");

        PreCheckResult result = agentDeployService.preCheck(server);

        System.out.println("\n预检查结果:");
        System.out.println("  SSH连接: " + (result.isSshConnectable() ? "✅ 通过" : "❌ 失败"));
        System.out.println("  sudo权限: " + (result.isHasSudoPermission() ? "✅ 通过" : "⚠️ 警告"));
        System.out.println("  磁盘空间: " + (result.isHasEnoughDiskSpace() ? "✅ 通过" : "❌ 失败"));
        System.out.println("  端口可用: " + (result.isPortsAvailable() ? "✅ 通过" : "❌ 失败"));
        System.out.println("  总体结果: " + (result.isPassed() ? "✅ 通过" : "❌ 失败"));

        if (!result.getErrors().isEmpty()) {
            System.out.println("\n错误信息:");
            result.getErrors().forEach(error -> System.out.println("  ❌ " + error));
        }

        if (!result.getWarnings().isEmpty()) {
            System.out.println("\n警告信息:");
            result.getWarnings().forEach(warning -> System.out.println("  ⚠️ " + warning));
        }
    }

    /**
     * 测试完整部署
     */
    private void testFullDeployment(Scanner scanner) {
        System.out.print("请输入服务器ID: ");
        Long serverId = scanner.nextLong();
        scanner.nextLine();

        Server server = serverRepository.findById(serverId).orElse(null);
        if (server == null) {
            System.out.println("❌ 服务器不存在");
            return;
        }

        System.out.println("\n开始部署 - 服务器: " + server.getName());
        System.out.println("=================================================");
        System.out.println("注意: 这是一个异步操作，部署将在后台执行");
        System.out.println("请通过选项4查看部署历史，选项5查看部署日志");
        System.out.println("=================================================");

        System.out.print("确认开始部署? (y/n): ");
        String confirm = scanner.nextLine();

        if (!"y".equalsIgnoreCase(confirm)) {
            System.out.println("已取消部署");
            return;
        }

        try {
            CompletableFuture<DeployResult> future = agentDeployService.deployAgent(server);

            System.out.println("✅ 部署任务已提交");
            System.out.print("是否等待部署完成? (y/n): ");
            String wait = scanner.nextLine();

            if ("y".equalsIgnoreCase(wait)) {
                System.out.println("等待部署完成...");
                DeployResult result = future.get();

                System.out.println("\n部署结果:");
                System.out.println("  状态: " + (result.isSuccess() ? "✅ 成功" : "❌ 失败"));
                System.out.println("  消息: " + result.getMessage());
                if (result.getDeploymentId() != null) {
                    System.out.println("  部署ID: " + result.getDeploymentId());
                }
            }
        } catch (Exception e) {
            logger.error("部署异常", e);
            System.out.println("❌ 部署异常: " + e.getMessage());
        }
    }

    /**
     * 查看部署历史
     */
    private void viewDeploymentHistory(Scanner scanner) {
        System.out.print("请输入服务器ID (输入0查看所有): ");
        Long serverId = scanner.nextLong();
        scanner.nextLine();

        System.out.println("\n=== 部署历史 ===");

        List<AgentDeployment> deployments;
        if (serverId == 0) {
            deployments = deploymentRepository.findAll();
        } else {
            Server server = serverRepository.findById(serverId).orElse(null);
            if (server == null) {
                System.out.println("❌ 服务器不存在");
                return;
            }
            deployments = deploymentRepository.findByServerOrderByCreatedAtDesc(server);
        }

        if (deployments.isEmpty()) {
            System.out.println("没有找到部署记录");
            return;
        }

        for (AgentDeployment deployment : deployments) {
            System.out.printf("\nID: %d | 服务器: %s | 版本: %s\n",
                    deployment.getId(),
                    deployment.getServer().getName(),
                    deployment.getAgentVersion());
            System.out.printf("  状态: %s | 重试次数: %d\n",
                    deployment.getStatus(),
                    deployment.getRetryCount());
            System.out.printf("  开始时间: %s\n", deployment.getStartTime());
            System.out.printf("  结束时间: %s\n", deployment.getEndTime());
            if (deployment.getErrorMessage() != null) {
                System.out.printf("  错误: %s\n", deployment.getErrorMessage());
            }
        }
    }

    /**
     * 查看部署日志
     */
    private void viewDeploymentLog(Scanner scanner) {
        System.out.print("请输入部署ID: ");
        Long deploymentId = scanner.nextLong();
        scanner.nextLine();

        AgentDeployment deployment = deploymentRepository.findById(deploymentId).orElse(null);
        if (deployment == null) {
            System.out.println("❌ 部署记录不存在");
            return;
        }

        System.out.println("\n=== 部署日志 ===");
        System.out.printf("部署ID: %d | 服务器: %s | 状态: %s\n",
                deployment.getId(),
                deployment.getServer().getName(),
                deployment.getStatus());
        System.out.println("=================================================");

        if (deployment.getDeploymentLog() != null) {
            System.out.println(deployment.getDeploymentLog());
        } else {
            System.out.println("暂无日志");
        }
    }

    /**
     * 测试回滚
     */
    private void testRollback(Scanner scanner) {
        System.out.print("请输入服务器ID: ");
        Long serverId = scanner.nextLong();
        scanner.nextLine();

        Server server = serverRepository.findById(serverId).orElse(null);
        if (server == null) {
            System.out.println("❌ 服务器不存在");
            return;
        }

        System.out.println("\n警告: 这将卸载Agent并删除所有相关文件");
        System.out.println("服务器: " + server.getName());
        System.out.print("确认执行回滚? (y/n): ");
        String confirm = scanner.nextLine();

        if (!"y".equalsIgnoreCase(confirm)) {
            System.out.println("已取消回滚");
            return;
        }

        System.out.println("开始回滚...");
        agentDeployService.rollback(server);
        System.out.println("✅ 回滚完成");
    }

    /**
     * 测试配置文件渲染
     */
    private void testConfigRendering(Scanner scanner) {
        System.out.print("请输入服务器ID: ");
        Long serverId = scanner.nextLong();
        scanner.nextLine();

        Server server = serverRepository.findById(serverId).orElse(null);
        if (server == null) {
            System.out.println("❌ 服务器不存在");
            return;
        }

        try {
            // 使用反射调用私有方法
            java.lang.reflect.Method method = AgentDeployService.class.getDeclaredMethod(
                    "renderOtelConfig", Server.class);
            method.setAccessible(true);
            String config = (String) method.invoke(agentDeployService, server);

            System.out.println("\n=== 渲染后的配置文件 ===");
            System.out.println(config);
            System.out.println("=================================================");

            System.out.print("是否保存到文件? (y/n): ");
            String save = scanner.nextLine();
            if ("y".equalsIgnoreCase(save)) {
                String filename = "otelcol_server_" + serverId + ".yaml";
                java.nio.file.Files.writeString(
                        java.nio.file.Paths.get(filename),
                        config
                );
                System.out.println("✅ 已保存到: " + filename);
            }
        } catch (Exception e) {
            logger.error("配置渲染异常", e);
            System.out.println("❌ 配置渲染失败: " + e.getMessage());
        }
    }
}
