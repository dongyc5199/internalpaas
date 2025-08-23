package com.cmict.internalpaas.test;

import com.jcraft.jsch.*;

import java.util.Properties;

/**
 * 直接SSH连接测试
 */
public class DirectSSHTest {
    
    public static void main(String[] args) {
        testSSHConnection();
    }
    
    public static void testSSHConnection() {
        JSch jsch = new JSch();
        Session session = null;
        
        try {
            System.out.println("开始SSH连接测试...");
            
            // 创建SSH会话
            session = jsch.getSession("devuser", "localhost", 2223);
            session.setPassword("admin123");
            
            // 配置SSH连接属性
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "password");
            config.put("UserKnownHostsFile", "/dev/null");
            session.setConfig(config);
            
            // 设置连接超时
            session.setTimeout(10000);
            
            System.out.println("尝试连接到 localhost:2223 用户名: devuser");
            session.connect();
            System.out.println("SSH连接成功！");
            
            // 测试执行命令
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("echo 'Hello from SSH'");
            
            channel.setInputStream(null);
            channel.setErrStream(System.err);
            channel.connect();
            
            java.io.InputStream in = channel.getInputStream();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    System.out.print(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    System.out.println("退出状态: " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
            channel.disconnect();
            
        } catch (JSchException e) {
            System.err.println("SSH连接失败: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
                System.out.println("SSH连接已断开");
            }
        }
    }
}