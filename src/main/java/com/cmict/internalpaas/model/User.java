package com.cmict.internalpaas.model;

// 若使用 Java Persistence API 2.2，可添加以下依赖
import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users") // "user" 是很多数据库的关键字，用 "users" 更安全
@Data // Lombok 注解
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // 存储加密后的密码
    
    // 首次登录后会创建关联的配置
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserConfig userConfig;
}

