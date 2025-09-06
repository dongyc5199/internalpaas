package com.cmict.internalpaas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService userDetailsService)
            throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .antMatchers("/css/**", "/js/**", "/register", "/debug/**", "/h2-console/**", "/ws/**", "/test/**").permitAll() // 允许访问静态资源、注册页面、H2控制台、WebSocket端点和测试页面
                .antMatchers("/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN") // 管理员和超级管理员才能访问管理页面
                .antMatchers("/developer/**").hasAnyRole("USER", "DEVELOPER", "ADMIN", "SUPER_ADMIN") // 开发者、管理员和超级管理员都能访问研发工作台
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
                .ignoringAntMatchers("/h2-console/**", "/ws/**", "/test/**", 
                    "/monitoring/server/*/refresh", "/monitoring/trigger-health-check",
                    "/monitoring/history/api/**", "/monitoring/thresholds/api/**",
                    "/api/server-user-groups/**", "/api/permission-test/**") // 禁用H2控制台、WebSocket、测试接口和监控接口的CSRF保护
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) // 使用Cookie存储CSRF token
            )
            .headers(headers -> headers
                .frameOptions().disable() // 禁用frame限制，允许H2控制台在iframe中运行
            )
            .sessionManagement(session -> session
                .maximumSessions(10) // 允许每个用户最多10个并发会话
                .sessionRegistry(sessionRegistry()) // 设置会话注册表
                .maxSessionsPreventsLogin(false) // 允许新登录挑出旧会话
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
}