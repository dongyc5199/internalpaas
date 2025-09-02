package com.cmict.internalpaas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

/**
 * 用户会话检查服务
 * 用于判断当前系统是否有活跃的登录用户
 */
@Service
public class UserSessionService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserSessionService.class);
    
    @Autowired(required = false)  // 设置为可选，避免Spring Security未配置时出错
    private SessionRegistry sessionRegistry;
    
    @Autowired
    private UserActivityService userActivityService;
    
    /**
     * 检查当前是否有活跃的登录用户
     * @return true 如果有活跃用户，false 如果没有
     */
    public boolean hasActiveUsers() {
        try {
            logger.debug("开始检查活跃用户状态...");
            
            // 只通过Spring Security SessionRegistry检查当前登录用户
            if (sessionRegistry != null) {
                int activeSessionCount = sessionRegistry.getAllPrincipals().size();
                logger.debug("SessionRegistry可用，检测到{}个活跃用户会话", activeSessionCount);
                if (activeSessionCount > 0) {
                    // 打印活跃用户列表
                    sessionRegistry.getAllPrincipals().forEach(principal -> {
                        logger.debug("活跃用户: {}", principal.toString());
                    });
                    return true;
                }
            } else {
                logger.debug("SessionRegistry不可用，无法检查用户会话状态");
            }
            
            logger.debug("当前系统没有检测到活跃登录用户");
            return false;
            
        } catch (Exception e) {
            logger.warn("检查活跃用户状态时发生错误，默认返回false", e);
            return false;
        }
    }
    
    /**
     * 获取当前活跃用户数量
     * @return 活跃用户数量
     */
    public int getActiveUserCount() {
        try {
            if (sessionRegistry != null) {
                return sessionRegistry.getAllPrincipals().size();
            }
            return 0;
        } catch (Exception e) {
            logger.warn("获取活跃用户数量时发生错误", e);
            return 0;
        }
    }
    
    /**
     * 检查指定服务器是否有活跃用户
     * @param serverId 服务器ID
     * @return true 如果有活跃用户在该服务器上，false 如果没有
     */
    public boolean hasActiveUsersOnServer(Long serverId) {
        try {
            long activeUsers = userActivityService.countActiveUsers(serverId);
            return activeUsers > 0;
        } catch (Exception e) {
            logger.warn("检查服务器{}活跃用户时发生错误", serverId, e);
            return false;
        }
    }
}