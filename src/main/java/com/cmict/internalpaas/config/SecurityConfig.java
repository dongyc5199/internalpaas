package com.cmict.internalpaas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.cmict.internalpaas.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.access.AccessDeniedException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService userDetailsService)
            throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/css/**", "/js/**", "/register", "/debug/**", "/h2-console/**", "/ws/**", "/test/**").permitAll() // 允许访问静态资源、注册页面、H2控制台、WebSocket端点和测试页面
                .requestMatchers("/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN") // 管理员和超级管理员才能访问管理页面
                .requestMatchers("/developer/**").hasAnyRole("USER", "DEVELOPER", "ADMIN", "SUPER_ADMIN") // 开发者、管理员和超级管理员都能访问研发工作台
                .requestMatchers("/terminal/**").hasAnyRole("USER", "DEVELOPER", "ADMIN", "SUPER_ADMIN") // 所有认证用户都能访问SSH终端
                .anyRequest().authenticated() // 其他所有请求都需要认证
            )
            .formLogin(form -> form
                .loginPage("/login").permitAll() // 自定义登录页面
                .defaultSuccessUrl("/", true) // 登录成功后跳转到主页
                .failureUrl("/login?error") // 登录失败时返回登录页面并显示错误
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/ws/**", "/test/**",
                    "/monitoring/server/*/refresh", "/monitoring/trigger-health-check",
                    "/monitoring/history/api/**", "/monitoring/thresholds/api/**",
                    "/api/server-user-groups/**", "/api/permission-test/**",
                    "/api/ssh-config-import/**", // SSH配置导入API
                    "/terminal/api/**", "/user-operations/api/**") // 禁用H2控制台、WebSocket、测试接口、监控接口、用户操作接口和终端API的CSRF保护
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) // 使用Cookie存储CSRF token
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable()) // 禁用frame限制，允许H2控制台在iframe中运行
            )
            .sessionManagement(session -> session
                .maximumSessions(10) // 允许每个用户最多10个并发会话
                .sessionRegistry(sessionRegistry()) // 设置会话注册表
                .maxSessionsPreventsLogin(false) // 允许新登录挑出旧会话
                .expiredUrl("/login?expired") // 会话过期时重定向到登录页面
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(ajaxAwareAuthenticationEntryPoint()) // 设置自定义认证入口点
                .accessDeniedHandler(ajaxAwareAccessDeniedHandler()) // 设置自定义访问拒绝处理器
            )
            .userDetailsService(userDetailsService); // 设置UserDetailsService

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 使用BCrypt进行密码加密
    }

    // 使用UserService作为UserDetailsService
    @Bean
    public UserDetailsService userDetailsService(UserService userService) {
        return userService;
    }
    
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
    
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
    
    /**
     * 自定义认证入口点，用于处理AJAX请求的会话超时
     */
    @Bean
    public AuthenticationEntryPoint ajaxAwareAuthenticationEntryPoint() {
        return new AuthenticationEntryPoint() {
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response,
                    AuthenticationException authException) throws IOException, ServletException {
                
                // 检查是否是AJAX请求
                String requestedWith = request.getHeader("X-Requested-With");
                String accept = request.getHeader("Accept");
                String contentType = request.getHeader("Content-Type");
                
                boolean isAjaxRequest = "XMLHttpRequest".equals(requestedWith) ||
                                      (accept != null && accept.contains("application/json")) ||
                                      (contentType != null && contentType.contains("application/json")) ||
                                      request.getRequestURI().contains("/api/");
                
                // 检查是否是会话过期（存在session但已失效）
                boolean isSessionExpired = false;
                String sessionId = null;
                
                // 检查Cookie中的JSESSIONID
                if (request.getCookies() != null) {
                    for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                        if ("JSESSIONID".equals(cookie.getName())) {
                            sessionId = cookie.getValue();
                            break;
                        }
                    }
                }
                
                // 如果有sessionId但session已失效,则认为是过期
                if (sessionId != null) {
                    jakarta.servlet.http.HttpSession session = request.getSession(false);
                    isSessionExpired = (session == null || !sessionId.equals(session.getId()));
                }
                
                if (isAjaxRequest) {
                    // AJAX请求返回401状态码和JSON响应
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    if (isSessionExpired) {
                        response.getWriter().write("{\"error\":\"session_expired\",\"message\":\"会话已过期，请重新登录\",\"redirect\":\"/login?expired\"}");
                    } else {
                        response.getWriter().write("{\"error\":\"authentication_required\",\"message\":\"需要登录\",\"redirect\":\"/login\"}");
                    }
                } else {
                    // 普通请求重定向到登录页面
                    if (isSessionExpired) {
                        response.sendRedirect("/login?expired");
                    } else {
                        response.sendRedirect("/login");
                    }
                }
            }
        };
    }

    /**
     * 自定义访问拒绝处理器，用于处理AJAX请求的访问被拒绝
     */
    @Bean
    public AccessDeniedHandler ajaxAwareAccessDeniedHandler() {
        return new AccessDeniedHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response,
                    AccessDeniedException accessDeniedException) throws IOException, ServletException {

                // 检查是否是API请求
                String requestedWith = request.getHeader("X-Requested-With");
                String accept = request.getHeader("Accept");
                String contentType = request.getHeader("Content-Type");

                boolean isApiRequest = "XMLHttpRequest".equals(requestedWith) ||
                                      (accept != null && accept.contains("application/json")) ||
                                      (contentType != null && contentType.contains("application/json")) ||
                                      request.getRequestURI().contains("/api/");

                if (isApiRequest) {
                    // API请求返回403状态码和JSON响应
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"access_denied\",\"message\":\"访问被拒绝，权限不足\"}");
                } else {
                    // 普通请求重定向到拒绝访问页面或首页
                    response.sendRedirect("/");
                }
            }
        };
    }
}