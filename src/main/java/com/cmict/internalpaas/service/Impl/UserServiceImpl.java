package com.cmict.internalpaas.service.Impl;

import com.cmict.internalpaas.dto.*;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.model.UserConfig;
import com.cmict.internalpaas.repository.ApplicationRepository;
import com.cmict.internalpaas.repository.UserActivityRepository;
import com.cmict.internalpaas.repository.UserConfigRepository;
import com.cmict.internalpaas.repository.UserRepository;
import com.cmict.internalpaas.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserConfigRepository userConfigRepository;
    private final ApplicationRepository applicationRepository;
    private final UserActivityRepository userActivityRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, 
                          UserConfigRepository userConfigRepository,
                          ApplicationRepository applicationRepository, 
                          UserActivityRepository userActivityRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userConfigRepository = userConfigRepository;
        this.applicationRepository = applicationRepository;
        this.userActivityRepository = userActivityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User registerNewUser(UserRegistrationDto registrationDto) {
        // 1. 检查用户名是否已经被注册
        if (userRepository.findByUsername(registrationDto.getUsername()).isPresent()) {
            throw new IllegalStateException("Username already exists: " + registrationDto.getUsername());
        }

        // 2. 创建一个新的User实体
        User newUser = new User();
        newUser.setUsername(registrationDto.getUsername());
        newUser.setEmail(registrationDto.getEmail());

        // 3. 对密码进行加密处理！这是至关重要的安全措施。
        newUser.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        
        // 4. 设置默认角色为USER
        newUser.setRoles(Set.of(User.Role.USER));
        
        // 5. 设置默认工作目录（后续首次登录时会创建）
        newUser.setWorkDirectory("./workspaces/" + registrationDto.getUsername());
        
        // 6. 保存到数据库
        return userRepository.save(newUser);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("正在加载用户: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("用户不存在: {}", username);
                    return new UsernameNotFoundException("用户不存在");
                });
        
        logger.info("找到用户: {}, 密码长度: {}, 角色: {}", 
                   username, user.getPassword().length(), user.getRoles());
        
        // 添加密码格式检查日志
        String password = user.getPassword();
        logger.info("密码格式检查 - 密码前缀: {}", password.length() > 6 ? password.substring(0, 6) : password);
        if (!password.startsWith("$2a$") && !password.startsWith("$2y$") && !password.startsWith("$2b$")) {
            logger.warn("密码未使用BCrypt加密格式: {}", password);
        }
        
        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
        
        logger.info("用户权限: {}", authorities);
        
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    
    @Override
    public List<User> findUsersByServerId(Long serverId) {
        logger.debug("查找有权限访问服务器 {} 的用户", serverId);
        return userRepository.findByAvailableServersContaining(serverId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional
    public void toggleAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Set<User.Role> roles = user.getRoles();
        if (roles.contains(User.Role.ADMIN)) {
            roles.remove(User.Role.ADMIN);
        } else {
            roles.add(User.Role.ADMIN);
        }
        
        user.setRoles(roles);
        userRepository.save(user);
    }
    
    @Override
    @Transactional
    public void toggleSuperAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Set<User.Role> roles = user.getRoles();
        if (roles.contains(User.Role.SUPER_ADMIN)) {
            roles.remove(User.Role.SUPER_ADMIN);
        } else {
            roles.add(User.Role.SUPER_ADMIN);
        }
        
        user.setRoles(roles);
        userRepository.save(user);
    }
    
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        logger.info("开始删除用户: {} (ID: {})", userToDelete.getUsername(), userId);
        
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("当前用户不存在"));
        
        // 检查是否删除自己
        if (currentUser.getId().equals(userId)) {
            logger.warn("用户 {} 尝试删除自己的账号", currentUsername);
            throw new RuntimeException("不允许删除自己的账号");
        }
        
        // 检查要删除的用户是否是超级管理员
        if (userToDelete.getRoles().contains(User.Role.SUPER_ADMIN)) {
            // 如果是超级管理员，检查是否是最后一个超级管理员
            long superAdminCount = userRepository.countByRolesContaining(User.Role.SUPER_ADMIN);
            if (superAdminCount <= 1) {
                logger.warn("尝试删除最后一个超级管理员: {}", userToDelete.getUsername());
                throw new RuntimeException("不能删除最后一个超级管理员");
            }
        }
        
        // 清理用户相关的应用数据
        long deletedApplications = applicationRepository.countByUserId(userId);
        if (deletedApplications > 0) {
            logger.info("删除用户 {} 的 {} 个应用", userToDelete.getUsername(), deletedApplications);
            applicationRepository.deleteByUserId(userId);
        }
        
        // 清理用户活动记录
        long deletedActivities = userActivityRepository.countByUsername(userToDelete.getUsername());
        if (deletedActivities > 0) {
            logger.info("删除用户 {} 的 {} 条活动记录", userToDelete.getUsername(), deletedActivities);
            userActivityRepository.deleteByUsername(userToDelete.getUsername());
        }
        
        // 清理用户配置
        userConfigRepository.findByUserId(userId).ifPresent(config -> {
            logger.info("删除用户 {} 的配置信息", userToDelete.getUsername());
            userConfigRepository.delete(config);
        });
        
        // 清理用户与服务器的关联关系（ManyToMany关系会自动清理，但记录日志）
        if (userToDelete.getAvailableServers() != null && !userToDelete.getAvailableServers().isEmpty()) {
            logger.info("清理用户 {} 与 {} 个服务器的关联关系", userToDelete.getUsername(), 
                       userToDelete.getAvailableServers().size());
        }
        
        // 执行删除操作
        userRepository.deleteById(userId);
        
        logger.info("成功删除用户: {} (ID: {})", userToDelete.getUsername(), userId);
    }

    @Override
    public boolean hasRole(User user, User.Role role) {
        return user.getRoles() != null && user.getRoles().contains(role);
    }

    // === 用户档案管理方法实现 ===

    @Override
    public UserProfileDto getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        UserProfileDto profileDto = new UserProfileDto();
        profileDto.setId(user.getId());
        profileDto.setUsername(user.getUsername());
        profileDto.setEmail(user.getEmail());
        profileDto.setWorkDirectory(user.getWorkDirectory());
        profileDto.setFullName(user.getFullName());
        profileDto.setPhone(user.getPhone());
        profileDto.setDepartment(user.getDepartment());
        
        // 设置角色信息
        Set<String> roleStrings = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        profileDto.setRoles(roleStrings);
        
        profileDto.setCreatedAt(user.getCreatedAt());
        profileDto.setLastLoginTime(user.getLastLoginTime());
        profileDto.setIsFirstLogin(user.getIsFirstLogin());
        
        // 获取统计信息
        Long userId = user.getId();
        profileDto.setTotalApplications(applicationRepository.countByUserId(userId));
        profileDto.setActiveApplications(applicationRepository.countByUserIdAndStatus(userId, "RUNNING"));
        profileDto.setTotalSessions(userActivityRepository.countByUsername(username));
        profileDto.setTotalCommands(userActivityRepository.sumCommandCountByUsername(username));
        profileDto.setLastActivityTime(userActivityRepository.findLastActivityByUsername(username));
        
        return profileDto;
    }

    @Override
    @Transactional
    public User updateUserProfile(String username, UserProfileDto profileDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 更新允许修改的字段
        user.setEmail(profileDto.getEmail());
        user.setWorkDirectory(profileDto.getWorkDirectory());
        user.setFullName(profileDto.getFullName());
        user.setPhone(profileDto.getPhone());
        user.setDepartment(profileDto.getDepartment());
        
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(String username, PasswordChangeDto passwordChangeDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 验证当前密码
        if (!passwordEncoder.matches(passwordChangeDto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("当前密码不正确");
        }
        
        // 验证新密码和确认密码是否匹配
        if (!passwordChangeDto.isPasswordMatching()) {
            throw new IllegalArgumentException("新密码和确认密码不匹配");
        }
        
        // 验证新密码不能与当前密码相同
        if (passwordEncoder.matches(passwordChangeDto.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(passwordChangeDto.getNewPassword()));
        user.resetFailedLoginAttempts(); // 重置失败登录尝试次数
        
        userRepository.save(user);
        
        logger.info("用户 {} 修改密码成功", username);
    }

    @Override
    @Transactional
    public UserPreferencesDto getUserPreferences(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        UserConfig userConfig = getOrCreateUserConfig(user);
        
        UserPreferencesDto preferencesDto = new UserPreferencesDto();
        preferencesDto.setTheme(userConfig.getTheme());
        preferencesDto.setEmailNotifications(userConfig.getEmailNotifications());
        preferencesDto.setSystemNotifications(userConfig.getSystemNotifications());
        preferencesDto.setApplicationStatusNotifications(userConfig.getApplicationStatusNotifications());
        preferencesDto.setSecurityNotifications(userConfig.getSecurityNotifications());
        preferencesDto.setDashboardLayout(userConfig.getDashboardLayout());
        preferencesDto.setShowWelcomeMessage(userConfig.getShowWelcomeMessage());
        preferencesDto.setShowQuickActions(userConfig.getShowQuickActions());
        preferencesDto.setShowRecentActivity(userConfig.getShowRecentActivity());
        preferencesDto.setTerminalTheme(userConfig.getTerminalTheme());
        preferencesDto.setTerminalFontSize(userConfig.getTerminalFontSize());
        preferencesDto.setTerminalFontFamily(userConfig.getTerminalFontFamily());
        preferencesDto.setLanguage(userConfig.getLanguage());
        preferencesDto.setTimeZone(userConfig.getTimeZone());
        
        return preferencesDto;
    }

    @Override
    @Transactional
    public UserConfig updateUserPreferences(String username, UserPreferencesDto preferencesDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        UserConfig userConfig = getOrCreateUserConfig(user);
        
        // 更新偏好设置
        userConfig.setTheme(preferencesDto.getTheme());
        userConfig.setEmailNotifications(preferencesDto.getEmailNotifications());
        userConfig.setSystemNotifications(preferencesDto.getSystemNotifications());
        userConfig.setApplicationStatusNotifications(preferencesDto.getApplicationStatusNotifications());
        userConfig.setSecurityNotifications(preferencesDto.getSecurityNotifications());
        userConfig.setDashboardLayout(preferencesDto.getDashboardLayout());
        userConfig.setShowWelcomeMessage(preferencesDto.getShowWelcomeMessage());
        userConfig.setShowQuickActions(preferencesDto.getShowQuickActions());
        userConfig.setShowRecentActivity(preferencesDto.getShowRecentActivity());
        userConfig.setTerminalTheme(preferencesDto.getTerminalTheme());
        userConfig.setTerminalFontSize(preferencesDto.getTerminalFontSize());
        userConfig.setTerminalFontFamily(preferencesDto.getTerminalFontFamily());
        userConfig.setLanguage(preferencesDto.getLanguage());
        userConfig.setTimeZone(preferencesDto.getTimeZone());
        
        return userConfigRepository.save(userConfig);
    }

    @Override
    @Transactional
    public UserConfig getOrCreateUserConfig(User user) {
        Optional<UserConfig> existingConfig = userConfigRepository.findByUserId(user.getId());
        if (existingConfig.isPresent()) {
            return existingConfig.get();
        }
        
        // 由于使用了@MapsId，必须设置user字段以便Hibernate生成ID
        UserConfig newConfig = new UserConfig();
        newConfig.setUser(user); // @MapsId要求必须设置关联实体
        newConfig.setWorkDirectory(user.getWorkDirectory());
        
        return userConfigRepository.save(newConfig);
    }

    @Override
    @Transactional
    public void updateLastLogin(String username, String loginIp) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        user.setLastLoginTime(java.time.LocalDateTime.now());
        user.setLastLoginIp(loginIp);
        user.incrementLoginCount();
        user.setIsFirstLogin(false);
        user.resetFailedLoginAttempts();
        
        userRepository.save(user);
        
        logger.info("更新用户 {} 登录信息，IP: {}", username, loginIp);
    }
}
