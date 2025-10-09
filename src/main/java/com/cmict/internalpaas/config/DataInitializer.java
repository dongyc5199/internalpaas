package com.cmict.internalpaas.config;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerResourceThreshold;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.repository.ServerResourceThresholdRepository;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 数据初始化器，用于在应用启动时创建内置的root超级管理员账号和默认资源阈值
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ServerService serverService;

    @Autowired
    private ServerResourceThresholdRepository thresholdRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // 0. 应用数据库补丁
        applySchemaPatches();

        // 1. 创建root超级管理员
        initializeRootAdmin();
        
        // 2. 初始化服务器资源阈值配置
        initializeServerResourceThresholds();
    }

    /**
     * 初始化root超级管理员账号
     */
    private void initializeRootAdmin() {
        if (!userService.existsByUsername("root")) {
            User rootAdmin = new User();
            rootAdmin.setUsername("root");
            rootAdmin.setPassword(passwordEncoder.encode("admin123"));
            rootAdmin.setEmail("root@internalpaas.com");
            rootAdmin.setRoles(Set.of(User.Role.SUPER_ADMIN));
            rootAdmin.setWorkDirectory("./workspaces/root");
            rootAdmin.setIsFirstLogin(false);
            
            userService.save(rootAdmin);
            
            logger.info("==============================================");
            logger.info("内置超级管理员账号已创建：");
            logger.info("用户名：root");
            logger.info("密码：admin123");
            logger.info("请登录后及时修改密码！");
            logger.info("==============================================");
        } else {
            logger.info("内置超级管理员账号已存在，跳过创建。");
        }
    }

    /**
     * 为所有服务器初始化默认的资源阈值配置
     */
    private void initializeServerResourceThresholds() {
        try {
            List<Server> servers = serverService.findAll();
            
            for (Server server : servers) {
                initializeServerThresholds(server.getId());
            }
            
            logger.info("服务器资源阈值初始化完成，共处理 {} 个服务器", servers.size());
            
        } catch (Exception e) {
            logger.error("初始化服务器资源阈值时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 为单个服务器初始化默认阈值
     */
    private void initializeServerThresholds(Long serverId) {
        // 内存阈值 - 80%关键阈值
        if (!thresholdRepository.existsByServerIdAndResourceType(serverId, 
                ServerResourceThreshold.ResourceType.MEMORY)) {
            ServerResourceThreshold memoryThreshold = ServerResourceThreshold
                .createDefaultMemoryThreshold(serverId);
            thresholdRepository.save(memoryThreshold);
            logger.debug("已为服务器 {} 创建内存阈值配置（80%关键阈值）", serverId);
        }

        // CPU阈值
        if (!thresholdRepository.existsByServerIdAndResourceType(serverId, 
                ServerResourceThreshold.ResourceType.CPU)) {
            ServerResourceThreshold cpuThreshold = new ServerResourceThreshold(serverId, 
                ServerResourceThreshold.ResourceType.CPU);
            thresholdRepository.save(cpuThreshold);
            logger.debug("已为服务器 {} 创建CPU阈值配置", serverId);
        }

        // 磁盘阈值
        if (!thresholdRepository.existsByServerIdAndResourceType(serverId, 
                ServerResourceThreshold.ResourceType.DISK)) {
            ServerResourceThreshold diskThreshold = new ServerResourceThreshold(serverId, 
                ServerResourceThreshold.ResourceType.DISK);
            thresholdRepository.save(diskThreshold);
            logger.debug("已为服务器 {} 创建磁盘阈值配置", serverId);
        }

        // 系统负载阈值
        if (!thresholdRepository.existsByServerIdAndResourceType(serverId, 
                ServerResourceThreshold.ResourceType.LOAD_AVERAGE)) {
            ServerResourceThreshold loadThreshold = new ServerResourceThreshold(serverId, 
                ServerResourceThreshold.ResourceType.LOAD_AVERAGE);
            thresholdRepository.save(loadThreshold);
            logger.debug("已为服务器 {} 创建系统负载阈值配置", serverId);
        }

        // 交换空间阈值
        if (!thresholdRepository.existsByServerIdAndResourceType(serverId, 
                ServerResourceThreshold.ResourceType.SWAP)) {
            ServerResourceThreshold swapThreshold = new ServerResourceThreshold(serverId, 
                ServerResourceThreshold.ResourceType.SWAP);
            thresholdRepository.save(swapThreshold);
            logger.debug("已为服务器 {} 创建交换空间阈值配置", serverId);
        }
    }

    /**
     * 确保新增的监控字段存在
     */
    private void applySchemaPatches() {
        try {
            jdbcTemplate.execute("ALTER TABLE server_metrics ADD COLUMN IF NOT EXISTS kernel_version VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE server_metrics ADD COLUMN IF NOT EXISTS network_interface VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE server_metrics ADD COLUMN IF NOT EXISTS network_received_bytes BIGINT");
            jdbcTemplate.execute("ALTER TABLE server_metrics ADD COLUMN IF NOT EXISTS network_transmitted_bytes BIGINT");
            jdbcTemplate.execute("ALTER TABLE server_metrics ADD COLUMN IF NOT EXISTS network_received_rate DOUBLE");
            jdbcTemplate.execute("ALTER TABLE server_metrics ADD COLUMN IF NOT EXISTS network_transmitted_rate DOUBLE");
        } catch (Exception e) {
            logger.error("初始化数据库结构失败: {}", e.getMessage(), e);
        }
    }
}
