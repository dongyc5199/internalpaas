package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.LoginRequest;
import com.cmict.internalpaas.dto.LoginResponse;
import com.cmict.internalpaas.dto.UserDto;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST API 认证控制器
 *
 * 为React前端提供JSON格式的认证API
 * - POST /api/auth/login - 登录
 * - POST /api/auth/logout - 登出
 * - GET /api/auth/current-user - 获取当前用户信息
 * - GET /api/auth/refresh - 刷新会话
 * - GET /api/auth/check-username - 检查用户名是否可用
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private static final Logger logger = LoggerFactory.getLogger(AuthApiController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求(username, password, rememberMe)
     * @param request HTTP请求对象
     * @return 登录响应,包含用户信息
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            logger.info("收到登录请求: username={}", loginRequest.getUsername());

            // 验证用户名和密码
            Optional<User> userOptional = userService.findByUsername(loginRequest.getUsername());
            if (userOptional.isEmpty()) {
                logger.warn("登录失败: 用户不存在 username={}", loginRequest.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid_credentials", "message", "用户名或密码错误"));
            }

            User user = userOptional.get();

            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                logger.warn("登录失败: 密码错误 username={}", loginRequest.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid_credentials", "message", "用户名或密码错误"));
            }

            // 创建认证对象 - 手动构建authorities
            java.util.List<org.springframework.security.core.GrantedAuthority> authorities =
                user.getRoles().stream()
                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.name()))
                    .collect(java.util.stream.Collectors.toList());

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user, null, authorities
            );

            // 设置到SecurityContext
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            // 创建新会话并保存SecurityContext
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            // 更新用户登录信息
            user.setLastLoginTime(LocalDateTime.now());
            user.setLastLoginIp(getClientIp(request));
            user.setLoginCount(user.getLoginCount() + 1);
            userService.save(user);

            // 构造响应
            UserDto userDto = UserDto.fromUser(user);
            LoginResponse response = new LoginResponse(
                    userDto,
                    session.getId(),
                    System.currentTimeMillis() + (session.getMaxInactiveInterval() * 1000L)
            );

            logger.info("登录成功: username={}, sessionId={}", user.getUsername(), session.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("登录处理异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "server_error", "message", "服务器内部错误"));
        }
    }

    /**
     * 用户登出
     *
     * @param request HTTP请求对象
     * @return 成功响应
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                logger.info("用户登出: sessionId={}", session.getId());
                session.invalidate();
            }

            SecurityContextHolder.clearContext();

            return ResponseEntity.ok(Map.of("message", "登出成功"));
        } catch (Exception e) {
            logger.error("登出处理异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "server_error", "message", "服务器内部错误"));
        }
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 当前用户信息
     */
    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    authentication.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "not_authenticated", "message", "未登录"));
            }

            User user = (User) authentication.getPrincipal();
            UserDto userDto = UserDto.fromUser(user);

            return ResponseEntity.ok(userDto);
        } catch (Exception e) {
            logger.error("获取当前用户信息异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "server_error", "message", "服务器内部错误"));
        }
    }

    /**
     * 刷新会话
     * 用于检查会话是否有效,并更新会话时间
     *
     * @param request HTTP请求对象
     * @return 当前用户信息
     */
    @GetMapping("/refresh")
    public ResponseEntity<?> refreshSession(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "session_expired", "message", "会话已过期"));
            }

            // 触发会话刷新
            session.setMaxInactiveInterval(session.getMaxInactiveInterval());

            // 返回当前用户信息
            return getCurrentUser();
        } catch (Exception e) {
            logger.error("刷新会话异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "server_error", "message", "服务器内部错误"));
        }
    }

    /**
     * 检查用户名是否可用
     *
     * @param username 要检查的用户名
     * @return 是否可用
     */
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        try {
            Optional<User> userOptional = userService.findByUsername(username);
            Map<String, Boolean> response = new HashMap<>();
            response.put("available", userOptional.isEmpty());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("检查用户名异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "server_error", "message", "服务器内部错误"));
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
