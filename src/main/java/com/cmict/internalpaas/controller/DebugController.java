package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/debug/check-root")
    public String checkRootUser() {
        return userRepository.findByUsername("root")
            .map(user -> {
                boolean passwordMatches = passwordEncoder.matches("admin123", user.getPassword());
                return String.format("Root用户存在: %s, 密码正确: %s, 角色: %s", 
                    user.getUsername(), 
                    passwordMatches, 
                    user.getRoles());
            })
            .orElse("Root用户不存在");
    }
}