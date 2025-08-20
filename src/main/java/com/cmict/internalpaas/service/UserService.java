package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.UserRegistrationDto;
import com.cmict.internalpaas.model.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

public interface UserService extends UserDetailsService {
    /**
     * 注册一个新用户
     * @param registrationDto 包含用户名和密码的DTO
     * @return 创建成功的用户实体
     * @throws IllegalStateException 如果用户名已存在
     */
    User registerNewUser(UserRegistrationDto registrationDto);

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 包含用户的Optional，如果不存在则为空
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 保存用户
     * @param user 用户实体
     * @return 保存后的用户实体
     */
    User save(User user);

    /**
     * 查询所有用户
     * @return 用户列表
     */
    List<User> findAllUsers();

    /**
     * 切换用户的管理员角色
     * @param userId 用户ID
     */
    void toggleAdminRole(Long userId);

    /**
     * 判断用户是否拥有指定角色
     * @param user 用户实体
     * @param role 角色枚举
     * @return 是否拥有该角色
     */
    boolean hasRole(User user, User.Role role);

    /**
     * 判断用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);
}
