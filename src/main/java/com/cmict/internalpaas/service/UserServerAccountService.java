package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.UserServerAccountDto;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerUserGroup;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.model.UserServerAccount;
import com.cmict.internalpaas.repository.UserServerAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 用户服务器账户服务（简化版）
 * 负责用户在服务器上的账户创建、同步和管理
 */
@Service
public class UserServerAccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserServerAccountService.class);
    
    @Autowired
    private UserServerAccountRepository accountRepository;
    
    @Autowired
    private RemoteCommandService remoteCommandService;
    
    /**
     * 检查用户在服务器上的账户状态
     * @param user 用户
     * @param server 服务器
     * @return 账户状态
     */
    public UserServerAccount checkUserAccountStatus(User user, Server server) {
        logger.info("检查用户 {} 在服务器 {} 上的账户状态", user.getUsername(), server.getName());
        
        try {
            // 查找现有账户记录
            Optional<UserServerAccount> existingAccount = accountRepository
                .findByUserIdAndServerId(user.getId(), server.getId());
            
            UserServerAccount account;
            if (existingAccount.isPresent()) {
                account = existingAccount.get();
            } else {
                // 创建新的账户记录
                account = new UserServerAccount();
                account.setUser(user);
                account.setServer(server);
                account.setAccountStatus(UserServerAccount.AccountStatus.NOT_CHECKED);
                account.setCreatedAt(LocalDateTime.now());
            }
            
            // 执行远程检查
            boolean accountExists = checkRemoteAccountExists(server, user.getUsername());
            
            if (accountExists) {
                account.markAsExists();
                logger.info("用户 {} 在服务器 {} 上的账户已存在", user.getUsername(), server.getName());
            } else {
                account.setAccountStatus(UserServerAccount.AccountStatus.NOT_EXISTS);
                logger.info("用户 {} 在服务器 {} 上的账户不存在", user.getUsername(), server.getName());
            }
            
            account.setLastSyncTime(LocalDateTime.now());
            account.setUpdatedAt(LocalDateTime.now());
            
            return accountRepository.save(account);
            
        } catch (Exception e) {
            logger.error("检查用户账户状态时发生异常", e);
            
            // 创建或更新为错误状态
            UserServerAccount account = accountRepository
                .findByUserIdAndServerId(user.getId(), server.getId())
                .orElse(new UserServerAccount());
            
            account.setUser(user);
            account.setServer(server);
            account.markAsSyncError("检查账户状态失败: " + e.getMessage());
            account.setLastSyncTime(LocalDateTime.now());
            account.setUpdatedAt(LocalDateTime.now());
            
            if (account.getId() == null) {
                account.setCreatedAt(LocalDateTime.now());
            }
            
            return accountRepository.save(account);
        }
    }
    
    /**
     * 检查远程服务器上是否存在指定用户账户
     */
    private boolean checkRemoteAccountExists(Server server, String username) {
        try {
            RemoteCommandService.CommandResult result = remoteCommandService.executeCommand(
                server, "id " + username + " > /dev/null 2>&1 && echo 'EXISTS' || echo 'NOT_EXISTS'");
            
            if (result.isSuccess()) {
                return result.getOutput().trim().equals("EXISTS");
            } else {
                logger.warn("检查用户 {} 是否存在时命令执行失败: {}", username, result.getError());
                return false;
            }
        } catch (Exception e) {
            logger.warn("检查远程用户账户存在性时发生异常: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建用户在服务器上的账户
     * @param user 用户
     * @param server 服务器
     * @param userGroup 用户组（可选）
     * @return 创建结果
     */
    @Transactional
    public UserServerAccount createUserAccount(User user, Server server, ServerUserGroup userGroup) {
        logger.info("为用户 {} 在服务器 {} 上创建账户", user.getUsername(), server.getName());
        
        try {
            // 获取或创建账户记录
            UserServerAccount account = accountRepository
                .findByUserIdAndServerId(user.getId(), server.getId())
                .orElse(new UserServerAccount());
            
            if (account.getId() == null) {
                account.setUser(user);
                account.setServer(server);
                account.setCreatedAt(LocalDateTime.now());
            }
            
            // 标记为创建中状态
            account.markAsCreating();
            account.setUpdatedAt(LocalDateTime.now());
            account = accountRepository.save(account);
            
            // 模拟账户创建过程
            logger.info("模拟创建用户账户过程...");
            Thread.sleep(1000); // 模拟创建过程
            
            // 标记为创建成功
            account.markAsCreated();
            if (userGroup != null) {
                account.setUserGroup(userGroup);
            }
            account.setLastSyncTime(LocalDateTime.now());
            account.setUpdatedAt(LocalDateTime.now());
            
            logger.info("成功为用户 {} 在服务器 {} 上创建账户", user.getUsername(), server.getName());
            return accountRepository.save(account);
            
        } catch (Exception e) {
            logger.error("创建用户账户时发生异常", e);
            
            UserServerAccount account = accountRepository
                .findByUserIdAndServerId(user.getId(), server.getId())
                .orElse(new UserServerAccount());
            
            account.setUser(user);
            account.setServer(server);
            account.markAsCreateFailed("创建过程中发生异常: " + e.getMessage());
            account.setUpdatedAt(LocalDateTime.now());
            
            if (account.getId() == null) {
                account.setCreatedAt(LocalDateTime.now());
            }
            
            return accountRepository.save(account);
        }
    }
    
    /**
     * 同步用户账户状态
     * @param user 用户
     * @param server 服务器
     * @return 同步后的账户状态
     */
    public UserServerAccount syncUserAccount(User user, Server server) {
        logger.info("同步用户 {} 在服务器 {} 上的账户状态", user.getUsername(), server.getName());
        return checkUserAccountStatus(user, server);
    }
    
    /**
     * 转换为DTO对象
     */
    public UserServerAccountDto convertToDto(UserServerAccount account) {
        UserServerAccountDto dto = new UserServerAccountDto();
        dto.setId(account.getId());
        dto.setUserId(account.getUser().getId());
        dto.setUsername(account.getUser().getUsername());
        dto.setServerId(account.getServer().getId());
        dto.setServerName(account.getServer().getName());
        dto.setServerHostname(account.getServer().getHostname());
        dto.setAccountStatus(account.getAccountStatus());
        dto.setHomeDirectory(account.getHomeDirectory());
        dto.setWorkDirectory(account.getWorkDirectory());
        dto.setLastSyncTime(account.getLastSyncTime());
        dto.setSyncErrorMessage(account.getSyncErrorMessage());
        dto.setAdditionalSystemGroups(account.getAdditionalSystemGroups());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        
        if (account.getUserGroup() != null) {
            dto.setUserGroupId(account.getUserGroup().getId());
            dto.setUserGroupName(account.getUserGroup().getGroupName());
        }
        
        return dto;
    }
}