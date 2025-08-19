package com.cmict.internalpaas.service.Impl;

import com.cmict.internalpaas.dto.UserRegistrationDto;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.repository.UserRepository;
import com.cmict.internalpaas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

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

        // 3. 对密码进行加密处理！这是至关重要的安全措施。
        newUser.setPassword(passwordEncoder.encode(registrationDto.getPassword()));

        // 4. 保存到数据库
        return userRepository.save(newUser);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
