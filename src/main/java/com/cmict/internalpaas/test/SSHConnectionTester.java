package com.cmict.internalpaas.test;

import com.jcraft.jsch.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

/**
 * SSH连接测试工具类
 * 用于测试SSH连接功能
 */
@Component
public class SSHConnectionTester {

    /**
     * 测试SSH连接
     */
    public boolean testConnection(String host, int port, String username, String password) {
        JSch jsch = new JSch();
        Session session = null;
        
        try {
            // 创建SSH会话
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            
            // 配置SSH连接属性
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "password");
            session.setConfig(config);
            
            // 设置连接超时
            session.setTimeout(5000);
            session.connect();
            
            // 测试执行命令
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand("echo 'SSH连接测试成功'");
            
            channel.setInputStream(null);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel.setOutputStream(outputStream);
            
            channel.connect();
            
            // 等待命令执行完成
            while (channel.isConnected()) {
                Thread.sleep(100);
            }
            
            String result = outputStream.toString();
            System.out.println("SSH命令执行结果: " + result);
            
            channel.disconnect();
            return true;
            
        } catch (Exception e) {
            System.err.println("SSH连接测试失败: " + e.getMessage());
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
    
    /**
     * 执行SSH命令
     */
    public String executeCommand(String host, int port, String username, String password, String command) {
        JSch jsch = new JSch();
        Session session = null;
        
        try {
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "password");
            session.setConfig(config);
            
            session.setTimeout(10000);
            session.connect();
            
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            
            channel.setInputStream(null);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel.setOutputStream(outputStream);
            
            channel.connect();
            
            while (channel.isConnected()) {
                Thread.sleep(100);
            }
            
            String result = outputStream.toString();
            channel.disconnect();
            
            return result;
            
        } catch (Exception e) {
            return "命令执行失败: " + e.getMessage();
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}