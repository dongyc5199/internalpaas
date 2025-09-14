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
        
        // 应用统计数据 - 使用优化查询避免N+1问题
        List<Application> applications = applicationRepository.findByUserWithUserOrderByCreatedAtDesc(user);
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
        
        // 生成最近调试记录（模拟数据）
        java.util.List<DeveloperDashboardDto.DebugRecord> records = new java.util.ArrayList<>();
        
        // 基于实际应用数据生成一些活动记录
        if (!applications.isEmpty()) {
            Application app = applications.get(0);
            DeveloperDashboardDto.DebugRecord record1 = new DeveloperDashboardDto.DebugRecord();
            record1.setApplicationName(app.getName());
            record1.setAction("应用启动");
            record1.setStatus("成功");
            record1.setTimestamp("5分钟前");
            records.add(record1);
            
            if (applications.size() > 1) {
                Application app2 = applications.get(1);
                DeveloperDashboardDto.DebugRecord record2 = new DeveloperDashboardDto.DebugRecord();
                record2.setApplicationName(app2.getName());
                record2.setAction("调试会话");
                record2.setStatus("开始");
                record2.setTimestamp("25分钟前");
                records.add(record2);
            }
        }
        
        // 添加一些通用活动记录
        DeveloperDashboardDto.DebugRecord record3 = new DeveloperDashboardDto.DebugRecord();
        record3.setApplicationName("test-app-2.0.jar");
        record3.setAction("历史记录查看");
        record3.setStatus("查看");
        record3.setTimestamp("2小时前");
        records.add(record3);
        
        DeveloperDashboardDto.DebugRecord record4 = new DeveloperDashboardDto.DebugRecord();
        record4.setApplicationName("broken-app-1.0.jar");
        record4.setAction("应用启动");
        record4.setStatus("失败");
        record4.setTimestamp("1天前");
        records.add(record4);
        
        dto.setRecentDebugRecords(records);
        
        return dto;
    }
}