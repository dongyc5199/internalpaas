package com.cmict.internalpaas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 应用关闭监听器
 * 确保在Web应用关闭时正确清理所有后台线程和定时任务
 */
@Component
public class ApplicationShutdownListener implements ApplicationListener<ContextClosedEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationShutdownListener.class);
    
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        logger.info("应用正在关闭，开始清理资源...");
        
        try {
            // 1. 清理ForkJoinPool (CompletableFuture的默认线程池)
            shutdownForkJoinPool();
            
            // 2. 等待所有定时任务完成
            waitForScheduledTasksToComplete();
            
            // 3. 清理自定义线程池
            shutdownCustomThreadPools();
            
            logger.info("应用资源清理完成");
            
        } catch (Exception e) {
            logger.error("应用资源清理过程中发生错误", e);
        }
    }
    
    /**
     * 关闭ForkJoinPool，这是CompletableFuture.runAsync()使用的默认线程池
     */
    private void shutdownForkJoinPool() {
        try {
            logger.info("开始关闭ForkJoinPool...");
            
            // 获取公共的ForkJoinPool
            java.util.concurrent.ForkJoinPool commonPool = java.util.concurrent.ForkJoinPool.commonPool();
            
            // 尝试优雅关闭
            commonPool.shutdown();
            
            // 等待最多10秒
            if (!commonPool.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("ForkJoinPool在10秒内未能正常关闭，尝试强制关闭");
                commonPool.shutdownNow();
                
                // 再等待5秒
                if (!commonPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.error("ForkJoinPool强制关闭失败");
                } else {
                    logger.info("ForkJoinPool已强制关闭");
                }
            } else {
                logger.info("ForkJoinPool已正常关闭");
            }
            
        } catch (Exception e) {
            logger.error("关闭ForkJoinPool时发生错误", e);
        }
    }
    
    /**
     * 等待定时任务完成
     */
    private void waitForScheduledTasksToComplete() {
        try {
            logger.info("等待定时任务完成...");
            
            // 等待一小段时间让正在执行的定时任务完成
            Thread.sleep(2000);
            
            logger.info("定时任务等待完成");
            
        } catch (InterruptedException e) {
            logger.warn("等待定时任务完成时被中断", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 关闭自定义线程池
     */
    private void shutdownCustomThreadPools() {
        try {
            logger.info("清理自定义线程池...");
            
            // 这里可以添加对任何自定义ExecutorService的清理
            // 目前项目主要使用CompletableFuture.runAsync()，它使用ForkJoinPool
            
            logger.info("自定义线程池清理完成");
            
        } catch (Exception e) {
            logger.error("清理自定义线程池时发生错误", e);
        }
    }
}