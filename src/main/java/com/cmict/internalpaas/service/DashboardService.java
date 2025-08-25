package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.AdminDashboardDto;
import com.cmict.internalpaas.dto.DeveloperDashboardDto;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.repository.UserRepository;
import com.cmict.internalpaas.repository.ServerRepository;
import com.cmict.internalpaas.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ServerRepository serverRepository;
    
    @Autowired
    private ApplicationRepository applicationRepository;

    /**
     * 获取管理员工作台数据
     * @return AdminDashboardDto 管理员工作台数据传输对象
     */
    public AdminDashboardDto getAdminDashboardData() {
        AdminDashboardDto dto = new AdminDashboardDto();
        
        // 服务器统计数据
        List<Server> servers = serverRepository.findAll();
        long totalServers = servers.size();
        long activeServers = servers.stream().filter(Server::getActive).count();
        long inactiveServers = totalServers - activeServers;
        long monitoringServers = servers.stream()
            .filter(server -> server.getConnectionStatus() != null && 
                             server.getConnectionStatus() == Server.ConnectionStatus.MONITORING)
            .count();
        
        dto.setTotalServers(totalServers);
        dto.setActiveServers(activeServers);
        dto.setInactiveServers(inactiveServers);
        dto.setMonitoringServers(monitoringServers);
        
        // 用户统计数据
        List<User> users = userRepository.findAll();
        long totalUsers = users.size();
        long adminUsers = users.stream().filter(user -> 
            user.getRoles().contains(User.Role.ADMIN) || 
            user.getRoles().contains(User.Role.SUPER_ADMIN)).count();
        long regularUsers = users.stream().filter(user -> 
            user.getRoles().contains(User.Role.USER) && 
            !user.getRoles().contains(User.Role.ADMIN) && 
            !user.getRoles().contains(User.Role.SUPER_ADMIN)).count();
        long firstLoginUsers = users.stream().filter(User::getIsFirstLogin).count();
        
        dto.setTotalUsers(totalUsers);
        dto.setAdminUsers(adminUsers);
        dto.setRegularUsers(regularUsers);
        dto.setFirstLoginUsers(firstLoginUsers);
        
        // 系统监控指标（模拟数据）
        dto.setCpuUsage(45.5);
        dto.setMemoryUsage(62.3);
        dto.setDiskUsage(58.5);
        
        return dto;
    }

    /**
     * 获取研发工作台数据
     * @param username 研发人员用户名
     * @return DeveloperDashboardDto 研发工作台数据传输对象
     */
    public DeveloperDashboardDto getDeveloperDashboardData(String username) {
        DeveloperDashboardDto dto = new DeveloperDashboardDto();
        
        // 获取用户
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (!userOptional.isPresent()) {
            // 如果用户不存在，返回空的统计数据
            dto.setTotalApplications(0);
            dto.setRunningApplications(0);
            dto.setStoppedApplications(0);
            dto.setActiveDebugSessions(0);
            dto.setTotalDebugSessions(0);
            dto.setCpuUsage(0.0);
            dto.setMemoryUsage(0.0);
            return dto;
        }
        
        User user = userOptional.get();
        
        // 应用统计数据
        List<Application> applications = applicationRepository.findByUser(user);
        long totalApplications = applications.size();
        long runningApplications = applications.stream()
            .filter(app -> "RUNNING".equals(app.getStatus())).count();
        long stoppedApplications = totalApplications - runningApplications;
        
        dto.setTotalApplications(totalApplications);
        dto.setRunningApplications(runningApplications);
        dto.setStoppedApplications(stoppedApplications);
        
        // 调试会话统计（模拟数据）
        dto.setActiveDebugSessions(2);
        dto.setTotalDebugSessions(15);
        
        // 资源使用情况（模拟数据）
        dto.setCpuUsage(32.5);
        dto.setMemoryUsage(45.8);
        
        return dto;
    }
}