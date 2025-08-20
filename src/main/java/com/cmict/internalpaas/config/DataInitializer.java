package com.cmict.internalpaas.config;

import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 数据初始化器，用于在应用启动时创建内置的root超级管理员账号
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否已存在root超级管理员
        if (!userService.existsByUsername("root")) {
            // 创建root超级管理员
            User rootAdmin = new User();
            rootAdmin.setUsername("root");
            rootAdmin.setPassword(passwordEncoder.encode("admin123"));
            rootAdmin.setEmail("root@internalpaas.com");
            rootAdmin.setRoles(Set.of(User.Role.SUPER_ADMIN));
            rootAdmin.setWorkDirectory("./workspaces/root");
            rootAdmin.setIsFirstLogin(false);
            
            userService.save(rootAdmin);
            
            System.out.println("==============================================");
            System.out.println("内置超级管理员账号已创建：");
            System.out.println("用户名：root");
            System.out.println("密码：admin123");
            System.out.println("请登录后及时修改密码！");
            System.out.println("==============================================");
        } else {
            System.out.println("内置超级管理员账号已存在，跳过创建。");
        }
    }
}