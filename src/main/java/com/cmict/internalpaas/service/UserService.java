package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.UserRegistrationDto;
import com.cmict.internalpaas.model.User;

import java.util.Optional;

public interface UserService {
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
}
