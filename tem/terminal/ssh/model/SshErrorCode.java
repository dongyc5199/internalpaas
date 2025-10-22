package com.waveterm.demo.terminal.ssh.model;

/**
 * Describes the type of error encountered when opening or managing an SSH terminal session.
 */
public enum SshErrorCode {
    CONNECTION_TIMEOUT("连接 SSH 服务器超时，请检查网络或目标配置。"),
    AUTHENTICATION_FAILED("SSH 认证失败，请确认用户名/密码或私钥是否正确。"),
    HOST_KEY_VERIFICATION_FAILED("无法验证 SSH 主机指纹，若信任目标可配置 known_hosts。"),
    CHANNEL_OPEN_FAILED("SSH 通道打开失败，目标可能不支持 shell 或资源不足。"),
    IO_ERROR("SSH 会话发生 I/O 异常，连接可能已断开。"),
    UNKNOWN("未知 SSH 错误，请查看后台日志。");

    private final String defaultMessage;

    SshErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
