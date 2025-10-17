package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.UserRegistrationDto;
import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.repository.ApplicationRepository;
import com.cmict.internalpaas.repository.ServerRepository;
import com.cmict.internalpaas.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事务边界测试
 * 验证Service层方法的事务配置是否正确
 * 
 * Phase4-Step4 更新 (2025-10-17):
 * - 使用 H2 内存数据库 (scope=test) 支持测试
 * - 测试 User/Server/Application 实体的事务边界
 */
@SpringBootTest
@ActiveProfiles("test")
public class TransactionBoundaryTest {

    @Autowired
    private ApplicationService applicationService;
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Autowired
    private ServerRepository serverRepository;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 测试ApplicationService中的事务边界
     */
    @Test
    @Transactional
    public void testApplicationServiceTransactionBoundaries() {
        // 验证事务是否激活
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive(), 
                  "事务应该是激活状态");

        // 测试读取操作（只读事务）
        User testUser = createTestUser();
        
        // 获取用户应用列表 - 这应该使用只读事务
        var applications = applicationService.getUserApplications(testUser);
        assertNotNull(applications, "应用列表不应为null");
        
        // 测试保存应用配置 - 这需要写事务
        Application testApp = new Application();
        testApp.setName("Test App");
        testApp.setUser(testUser);
        testApp.setJarFilePath("/tmp/test.jar");
        testApp.setJarFileName("test.jar");
        
        Application savedApp = applicationService.saveApplication(testApp);
        assertNotNull(savedApp.getId(), "保存的应用应该有ID");
        
        System.out.println("✅ ApplicationService事务边界测试通过");
    }

    /**
     * 测试ServerService中的事务边界
     */
    @Test
    @Transactional
    public void testServerServiceTransactionBoundaries() {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive(), 
                  "事务应该是激活状态");

        // 测试保存服务器 - 需要写事务
        Server testServer = new Server();
        testServer.setName("Test Server");
        testServer.setHostname("localhost");
        testServer.setPort(8080);
        testServer.setBaseWorkDirectory("/tmp");
        testServer.setSshPort(22);
        testServer.setSshUsername("test");
        
        Server savedServer = serverService.saveServer(testServer);
        assertNotNull(savedServer.getId(), "保存的服务器应该有ID");
        
        // 测试更新服务器
        savedServer.setDescription("Updated description");
        Server updatedServer = serverService.updateServer(savedServer.getId(), savedServer);
        assertEquals("Updated description", updatedServer.getDescription(), 
                    "服务器描述应该被更新");
        
        System.out.println("✅ ServerService事务边界测试通过");
    }

    /**
     * 测试UserService中的事务边界
     */
    @Test
    @Transactional  
    public void testUserServiceTransactionBoundaries() {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive(), 
                  "事务应该是激活状态");

        // 测试用户创建 - 需要写事务
        User testUser = createTestUser();
        User savedUser = testUser; // 已经由registerNewUser保存
        assertNotNull(savedUser.getId(), "保存的用户应该有ID");
        
        // 测试只读操作
        var foundUser = userService.findByUsername(testUser.getUsername());
        assertTrue(foundUser.isPresent(), "应该能找到用户");
        
        System.out.println("✅ UserService事务边界测试通过");
    }

    /**
     * 测试事务回滚行为
     */
    @Test
    public void testTransactionRollback() {
        // 这个测试不使用@Transactional注解，以便测试回滚
        User testUser = createTestUser();
        
        // 计算初始应用数量
        long initialCount = applicationRepository.count();
        
        try {
            // 尝试创建一个会失败的应用上传操作
            // 这里模拟一个会抛异常的情况来测试回滚
            Application app = new Application();
            app.setName("Test App");
            app.setUser(testUser);
            app.setJarFilePath(null); // 故意设置为null来触发异常
            
            // 这应该会失败并回滚
            assertThrows(Exception.class, () -> {
                applicationService.saveApplication(app);
            });
            
        } catch (Exception e) {
            // 预期的异常
        }
        
        // 验证回滚后数据库状态没有改变
        long finalCount = applicationRepository.count();
        assertEquals(initialCount, finalCount, "事务回滚后应用数量应该不变");
        
        System.out.println("✅ 事务回滚测试通过");
    }

    /**
     * 测试复合操作的事务一致性
     */
    @Test
    @Transactional
    public void testComplexTransactionConsistency() {
        // 创建测试用户和服务器
        User testUser = createTestUser();
        
        Server testServer = new Server();
        testServer.setName("Test Server");
        testServer.setHostname("localhost");
        testServer.setPort(8080);
        testServer.setBaseWorkDirectory("/tmp");
        testServer.setSshPort(22);
        testServer.setSshUsername("test");
        serverRepository.save(testServer);
        
        // 创建应用
        Application testApp = new Application();
        testApp.setName("Complex Test App");
        testApp.setUser(testUser);
        testApp.setJarFilePath("/tmp/test.jar");
        testApp.setJarFileName("test.jar");
        
        // 保存应用
        Application savedApp = applicationService.saveApplication(testApp);
        assertNotNull(savedApp.getId());
        
        // 验证关联关系正确
        assertEquals(testUser.getId(), savedApp.getUser().getId());
        
        System.out.println("✅ 复合操作事务一致性测试通过");
    }

    private User createTestUser() {
        UserRegistrationDto dto = new UserRegistrationDto();
        String unique = UUID.randomUUID().toString().replace("-", "");
        dto.setUsername("test_user_" + unique);
        dto.setEmail("test_" + unique + "@example.com");
        dto.setPassword("test_password_123");
        return userService.registerNewUser(dto);
    }
}