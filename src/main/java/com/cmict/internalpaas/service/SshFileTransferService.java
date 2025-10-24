package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

/**
 * SSH文件传输服务
 * 使用SCP协议通过SSH上传文件到远程服务器
 *
 * @author Dev Debug Platform Team
 * @version 2.0 (阶段2 - Agent自动部署)
 */
@Service
public class SshFileTransferService {

    private static final Logger logger = LoggerFactory.getLogger(SshFileTransferService.class);

    @Autowired
    private SshConnectionService sshConnectionService;

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

    private static final int CONNECTION_TIMEOUT = 15000;
    private static final int TRANSFER_TIMEOUT = 300000; // 5分钟传输超时

    /**
     * 上传文件到远程服务器
     *
     * @param server     目标服务器
     * @param localPath  本地文件路径
     * @param remotePath 远程文件路径
     * @return 是否成功
     */
    public boolean uploadFile(Server server, String localPath, String remotePath) {
        logger.info("开始上传文件 - serverId: {}, local: {}, remote: {}",
                server.getId(), localPath, remotePath);

        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            // 创建SSH会话
            session = createSession(jsch, server);
            session.connect(CONNECTION_TIMEOUT);

            // 打开SFTP通道
            Channel channel = session.openChannel("sftp");
            channel.connect(CONNECTION_TIMEOUT);
            sftpChannel = (ChannelSftp) channel;

            // 上传文件
            File localFile = new File(localPath);
            if (!localFile.exists()) {
                logger.error("本地文件不存在 - path: {}", localPath);
                return false;
            }

            sftpChannel.put(localPath, remotePath, ChannelSftp.OVERWRITE);

            logger.info("✅ 文件上传成功 - serverId: {}, size: {} bytes",
                    server.getId(), localFile.length());
            return true;

        } catch (Exception e) {
            logger.error("文件上传失败 - serverId: {}, error: {}", server.getId(), e.getMessage(), e);
            return false;

        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 上传文件内容（从字符串）
     *
     * @param server      目标服务器
     * @param content     文件内容
     * @param remotePath  远程文件路径
     * @return 是否成功
     */
    public boolean uploadFileContent(Server server, String content, String remotePath) {
        Objects.requireNonNull(content, "content");
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        return uploadFileBytes(server, data, remotePath);
    }

    /**
     * 上传二进制文件内容
     *
     * @param server     目标服务器
     * @param data       文件字节内容
     * @param remotePath 远程文件路径
     * @return 是否成功
     */
    public boolean uploadFileBytes(Server server, byte[] data, String remotePath) {
        Objects.requireNonNull(data, "data");

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            return uploadStream(server, inputStream, remotePath, data.length);
        } catch (IOException e) {
            logger.error("二进制流上传失败 - serverId: {}, error: {}", server.getId(), e.getMessage(), e);
            return false;
        }
    }

    private boolean uploadStream(Server server,
                                  InputStream inputStream,
                                  String remotePath,
                                  long size) {
        logger.info("开始上传文件内容 - serverId: {}, remote: {}, size: {} bytes",
                server.getId(), remotePath, size);

        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = createSession(jsch, server);
            session.connect(CONNECTION_TIMEOUT);

            Channel channel = session.openChannel("sftp");
            channel.connect(CONNECTION_TIMEOUT);
            sftpChannel = (ChannelSftp) channel;

            sftpChannel.put(inputStream, remotePath, ChannelSftp.OVERWRITE);

            logger.info("✅ 文件内容上传成功 - serverId: {}", server.getId());
            return true;

        } catch (Exception e) {
            logger.error("文件内容上传失败 - serverId: {}, error: {}", server.getId(), e.getMessage(), e);
            return false;

        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 创建SSH会话
     */
    private Session createSession(JSch jsch, Server server) throws JSchException {
        Session session = jsch.getSession(
                server.getSshUsername(),
                server.getHostname(),
                server.getSshPort() != null ? server.getSshPort() : 22
        );

        // 解密密码
        String password = null;
        try {
            password = passwordEncryptionService.decryptPassword(server.getSshPasswordEncrypted());
        } catch (Exception e) {
            logger.error("解密密码失败", e);
            throw new JSchException("密码解密失败");
        }

        session.setPassword(password);

        // 配置
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setTimeout(TRANSFER_TIMEOUT);

        return session;
    }

    /**
     * 检查远程文件是否存在
     *
     * @param server     目标服务器
     * @param remotePath 远程文件路径
     * @return 是否存在
     */
    public boolean fileExists(Server server, String remotePath) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = createSession(jsch, server);
            session.connect(CONNECTION_TIMEOUT);

            Channel channel = session.openChannel("sftp");
            channel.connect(CONNECTION_TIMEOUT);
            sftpChannel = (ChannelSftp) channel;

            sftpChannel.stat(remotePath);
            return true;

        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            }
            logger.error("检查文件存在性失败 - serverId: {}, error: {}", server.getId(), e.getMessage());
            return false;

        } catch (Exception e) {
            logger.error("检查文件存在性失败 - serverId: {}, error: {}", server.getId(), e.getMessage());
            return false;

        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
