package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.PasswordChangeDto;
import com.cmict.internalpaas.dto.UserPreferencesDto;
import com.cmict.internalpaas.dto.UserProfileDto;
import com.cmict.internalpaas.dto.UserRegistrationDto;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.model.UserConfig;
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
     * 查找有权限访问指定服务器的所有用户
     * @param serverId 服务器ID
     * @return 用户列表
     */
    List<User> findUsersByServerId(Long serverId);

    /**
     * 切换用户的管理员角色
     * @param userId 用户ID
     */
    void toggleAdminRole(Long userId);
    
    /**
     * 切换用户的超级管理员角色
     * @param userId 用户ID
     */
    void toggleSuperAdminRole(Long userId);
    
    /**
     * 删除用户（仅超级管理员可执行）
     * @param userId 用户ID
     */
    void deleteUser(Long userId);

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
    
    // === 用户档案管理方法 ===
    
    /**
     * 获取用户资料信息
     * @param username 用户名
     * @return 用户资料DTO
     */
    UserProfileDto getUserProfile(String username);
    
    /**
     * 更新用户资料
     * @param username 用户名
     * @param profileDto 用户资料DTO
     * @return 更新后的用户实体
     */
    User updateUserProfile(String username, UserProfileDto profileDto);
    
    /**
     * 修改用户密码
     * @param username 用户名
     * @param passwordChangeDto 密码修改DTO
     * @throws IllegalArgumentException 如果当前密码不正确或新密码格式不正确
     */
    void changePassword(String username, PasswordChangeDto passwordChangeDto);
    
    /**
     * 获取用户偏好设置
     * @param username 用户名
     * @return 用户偏好设置DTO
     */
    UserPreferencesDto getUserPreferences(String username);
    
    /**
     * 更新用户偏好设置
     * @param username 用户名
     * @param preferencesDto 偏好设置DTO
     * @return 更新后的用户配置
     */
    UserConfig updateUserPreferences(String username, UserPreferencesDto preferencesDto);
    
    /**
     * 获取或创建用户配置
     * @param user 用户实体
     * @return 用户配置
     */
    UserConfig getOrCreateUserConfig(User user);
    
    /**
     * 更新用户最后登录信息
     * @param username 用户名
     * @param loginIp 登录IP
     */
    void updateLastLogin(String username, String loginIp);
}
