package com.cmict.internalpaas.test;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 服务器管理功能测试器
 * 用于测试服务器的添加、查询、更新、删除功能
 */
@Component
public class ServerManagementTester {

    @Autowired
    private ServerService serverService;

    /**
     * 测试添加服务器功能
     */
    public boolean testAddServer() {
        try {
            System.out.println("=== 测试添加服务器功能 ===");
            
            // 创建测试服务器
            Server testServer = new Server();
            testServer.setName("测试服务器");
            testServer.setHostname("localhost");
            testServer.setPort(8080);
            testServer.setDescription("这是一个测试服务器");
            testServer.setBaseWorkDirectory("./test-workspaces");
            testServer.setActive(true);
            
            // SSH配置
            testServer.setSshPort(22);
            testServer.setSshUsername("testuser");
            testServer.setSshPassword("admin123");
            
            // 保存服务器
            Server savedServer = serverService.saveServer(testServer);
            
            if (savedServer != null && savedServer.getId() != null) {
                System.out.println("✅ 服务器添加成功");
                System.out.println("   ID: " + savedServer.getId());
                System.out.println("   名称: " + savedServer.getName());
                System.out.println("   主机: " + savedServer.getHostname());
                System.out.println("   端口: " + savedServer.getPort());
                return true;
            } else {
                System.out.println("❌ 服务器添加失败");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ 添加服务器时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 测试查询服务器功能
     */
    public boolean testQueryServers() {
        try {
            System.out.println("\n=== 测试查询服务器功能 ===");
            
            List<Server> allServers = serverService.getAllServers();
            System.out.println("全部服务器数量: " + allServers.size());
            
            for (Server server : allServers) {
                System.out.println("服务器: " + server.getName() + 
                                 " [" + server.getHostname() + ":" + server.getPort() + "]" +
                                 " 状态: " + (server.getActive() ? "激活" : "禁用"));
            }
            
            List<Server> activeServers = serverService.getActiveServers();
            System.out.println("激活的服务器数量: " + activeServers.size());
            
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ 查询服务器时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 测试更新服务器功能
     */
    public boolean testUpdateServer() {
        try {
            System.out.println("\n=== 测试更新服务器功能 ===");
            
            List<Server> servers = serverService.getAllServers();
            if (servers.isEmpty()) {
                System.out.println("❌ 没有服务器可以更新");
                return false;
            }
            
            Server server = servers.get(0);
            Long serverId = server.getId();
            String originalName = server.getName();
            
            // 更新服务器信息
            server.setName(originalName + " [已更新]");
            server.setDescription("更新后的描述信息");
            
            Server updatedServer = serverService.updateServer(serverId, server);
            
            if (updatedServer != null && updatedServer.getName().contains("[已更新]")) {
                System.out.println("✅ 服务器更新成功");
                System.out.println("   原名称: " + originalName);
                System.out.println("   新名称: " + updatedServer.getName());
                return true;
            } else {
                System.out.println("❌ 服务器更新失败");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ 更新服务器时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 运行所有测试
     */
    public void runAllTests() {
        System.out.println("🚀 开始测试服务器管理功能");
        System.out.println("========================================");
        
        boolean addResult = testAddServer();
        boolean queryResult = testQueryServers();
        boolean updateResult = testUpdateServer();
        
        System.out.println("\n========================================");
        System.out.println("📊 测试结果汇总:");
        System.out.println("   添加服务器: " + (addResult ? "✅ 通过" : "❌ 失败"));
        System.out.println("   查询服务器: " + (queryResult ? "✅ 通过" : "❌ 失败"));
        System.out.println("   更新服务器: " + (updateResult ? "✅ 通过" : "❌ 失败"));
        
        if (addResult && queryResult && updateResult) {
            System.out.println("\n🎉 所有测试通过！服务器管理功能正常工作。");
        } else {
            System.out.println("\n⚠️ 部分测试失败，请检查相关功能。");
        }
    }
}