package com.cmict.internalpaas.test;

import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.repository.ApplicationRepository;
import com.cmict.internalpaas.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * N+1查询优化验证测试
 * 测试JOIN FETCH查询是否有效减少数据库查询次数
 */
@Component
public class N1QueryOptimizationTest implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(N1QueryOptimizationTest.class);
    private static final boolean ENABLE_TEST = false; // 设置为true来启用测试

    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EntityManager entityManager;

    @Override
    public void run(String... args) throws Exception {
        if (!ENABLE_TEST) {
            return;
        }
        
        logger.info("========== N+1查询优化验证测试开始 ==========");
        
        try {
            runN1QueryTest();
        } catch (Exception e) {
            logger.error("N+1查询测试执行失败", e);
        }
        
        logger.info("========== N+1查询优化验证测试结束 ==========");
    }

    @Transactional(readOnly = true)
    public void runN1QueryTest() {
        // 查找一个测试用户
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            logger.warn("没有找到测试用户，跳过N+1查询测试");
            return;
        }
        
        User testUser = users.get(0);
        logger.info("使用测试用户: {}", testUser.getUsername());
        
        // 清除一级缓存，确保测试准确性
        entityManager.clear();
        
        // 测试1: 传统查询方式（可能存在N+1问题）
        logger.info("\n--- 测试1: 传统查询方式 ---");
        long startTime = System.currentTimeMillis();
        
        List<Application> apps1 = applicationRepository.findByUserOrderByCreatedAtDesc(testUser);
        logger.info("获取到 {} 个应用", apps1.size());
        
        // 访问User属性，可能触发懒加载
        for (Application app : apps1) {
            String username = app.getUser().getUsername(); // 潜在的N+1查询点
            logger.debug("应用: {}, 用户: {}", app.getName(), username);
        }
        
        long time1 = System.currentTimeMillis() - startTime;
        logger.info("传统查询耗时: {}ms", time1);
        
        // 清除缓存
        entityManager.clear();
        
        // 测试2: 优化后的查询方式（JOIN FETCH）
        logger.info("\n--- 测试2: 优化查询方式 (JOIN FETCH) ---");
        startTime = System.currentTimeMillis();
        
        List<Application> apps2 = applicationRepository.findByUserWithUserOrderByCreatedAtDesc(testUser);
        logger.info("获取到 {} 个应用", apps2.size());
        
        // 访问User属性，应该不会触发额外查询
        for (Application app : apps2) {
            String username = app.getUser().getUsername(); // 已预加载，无额外查询
            logger.debug("应用: {}, 用户: {}", app.getName(), username);
        }
        
        long time2 = System.currentTimeMillis() - startTime;
        logger.info("优化查询耗时: {}ms", time2);
        
        // 清除缓存
        entityManager.clear();
        
        // 测试3: 包含配置的优化查询
        logger.info("\n--- 测试3: 包含配置的优化查询 ---");
        startTime = System.currentTimeMillis();
        
        List<Application> apps3 = applicationRepository.findByUserWithConfigsOrderByCreatedAtDesc(testUser);
        logger.info("获取到 {} 个应用", apps3.size());
        
        // 访问User和ActiveConfiguration属性
        for (Application app : apps3) {
            String username = app.getUser().getUsername();
            String configInfo = app.getActiveConfiguration() != null ? 
                "有活动配置" : "无活动配置";
            logger.debug("应用: {}, 用户: {}, 配置: {}", app.getName(), username, configInfo);
        }
        
        long time3 = System.currentTimeMillis() - startTime;
        logger.info("配置查询耗时: {}ms", time3);
        
        // 性能对比总结
        logger.info("\n=== 性能对比总结 ===");
        logger.info("传统查询: {}ms", time1);
        logger.info("优化查询: {}ms", time2);
        logger.info("配置查询: {}ms", time3);
        
        if (time1 > time2) {
            double improvement = ((double)(time1 - time2) / time1) * 100;
            logger.info("性能提升: {:.1f}%", improvement);
        }
        
        // 验证数据一致性
        logger.info("\n=== 数据一致性验证 ===");
        boolean isConsistent = apps1.size() == apps2.size() && apps2.size() == apps3.size();
        logger.info("查询结果一致性: {}", isConsistent ? "PASS" : "FAIL");
        
        if (isConsistent && !apps1.isEmpty()) {
            // 验证第一个应用的数据是否一致
            Application app1 = apps1.get(0);
            Application app2 = apps2.get(0);
            Application app3 = apps3.get(0);
            
            boolean dataConsistent = app1.getId().equals(app2.getId()) && 
                                   app2.getId().equals(app3.getId()) &&
                                   app1.getUser().getUsername().equals(app2.getUser().getUsername());
            
            logger.info("应用数据一致性: {}", dataConsistent ? "PASS" : "FAIL");
        }
    }

    /**
     * 手动测试方法，可以在Controller中调用
     */
    public String performQuickTest(User user) {
        if (user == null) {
            return "测试用户为空";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("N+1查询优化测试结果:\n");
        
        try {
            // 传统查询
            long start1 = System.currentTimeMillis();
            List<Application> apps1 = applicationRepository.findByUserOrderByCreatedAtDesc(user);
            // 触发懒加载
            apps1.forEach(app -> app.getUser().getUsername());
            long time1 = System.currentTimeMillis() - start1;
            
            entityManager.clear();
            
            // 优化查询
            long start2 = System.currentTimeMillis();
            List<Application> apps2 = applicationRepository.findByUserWithUserOrderByCreatedAtDesc(user);
            // 访问预加载的数据
            apps2.forEach(app -> app.getUser().getUsername());
            long time2 = System.currentTimeMillis() - start2;
            
            result.append(String.format("传统查询: %dms (%d个应用)\n", time1, apps1.size()));
            result.append(String.format("优化查询: %dms (%d个应用)\n", time2, apps2.size()));
            
            if (time1 > time2) {
                double improvement = ((double)(time1 - time2) / time1) * 100;
                result.append(String.format("性能提升: %.1f%%\n", improvement));
            }
            
            result.append("数据一致性: ").append(apps1.size() == apps2.size() ? "PASS" : "FAIL");
            
        } catch (Exception e) {
            result.append("测试执行失败: ").append(e.getMessage());
            logger.error("快速测试执行失败", e);
        }
        
        return result.toString();
    }
}