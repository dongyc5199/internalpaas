package com.cmict.internalpaas.service.Impl;

import com.cmict.internalpaas.dto.UserRegistrationDto;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.repository.UserRepository;
import com.cmict.internalpaas.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
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
        
        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
        
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
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
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
    public boolean hasRole(User user, User.Role role) {
        return user.getRoles() != null && user.getRoles().contains(role);
    }
}
