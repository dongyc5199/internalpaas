package com.cmict.internalpaas.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_configs")
@Data
public class UserConfig {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // 将此实体的主键与User实体的主键关联
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String workDirectory;

    // 未来可以扩展更多配置，如首选JDK版本等
}

