package com.cmict.internalpaas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * 配置Agent部署等异步任务的线程池
 *
 * @author Dev Debug Platform Team
 * @version 2.0 (阶段2 - Agent自动部署)
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Agent部署专用线程池
     * 用于异步执行Agent部署任务
     *
     * 配置说明:
     * - 核心线程数: 2 (同时处理2个部署任务)
     * - 最大线程数: 5 (高峰期最多5个并发部署)
     * - 队列容量: 10 (等待队列最多10个任务)
     * - 拒绝策略: CallerRunsPolicy (队列满时由调用线程执行)
     */
    @Bean(name = "agentDeployExecutor")
    public Executor agentDeployExecutor() {
        logger.info("初始化Agent部署线程池");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(2);

        // 最大线程数
        executor.setMaxPoolSize(5);

        // 队列容量
        executor.setQueueCapacity(10);

        // 线程名称前缀
        executor.setThreadNamePrefix("agent-deploy-");

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 等待所有任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 最多等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        // 拒绝策略：由调用线程处理该任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 初始化
        executor.initialize();

        logger.info("Agent部署线程池已初始化 - 核心线程: 2, 最大线程: 5, 队列容量: 10");

        return executor;
    }

    /**
     * 资源告警专用线程池
     * 用于异步执行资源告警检测和通知任务
     *
     * 配置说明:
     * - 核心线程数: 3 (同时处理3个告警任务)
     * - 最大线程数: 8 (高峰期最多8个并发检测)
     * - 队列容量: 20 (等待队列最多20个任务)
     * - 拒绝策略: CallerRunsPolicy (队列满时由调用线程执行)
     */
    @Bean(name = "resourceAlertExecutor")
    public Executor resourceAlertExecutor() {
        logger.info("初始化资源告警线程池");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(3);

        // 最大线程数
        executor.setMaxPoolSize(8);

        // 队列容量
        executor.setQueueCapacity(20);

        // 线程名称前缀
        executor.setThreadNamePrefix("resource-alert-");

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 等待所有任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 最多等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        // 拒绝策略：由调用线程处理该任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 初始化
        executor.initialize();

        logger.info("资源告警线程池已初始化 - 核心线程: 3, 最大线程: 8, 队列容量: 20");

        return executor;
    }
}
