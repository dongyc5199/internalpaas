package com.waveterm.demo.terminal.ssh;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.waveterm.demo.terminal.ManagedTerminalSession;
import com.waveterm.demo.terminal.TerminalGateway;
import com.waveterm.demo.terminal.gateway.SshTerminalGatewayAware;
import com.waveterm.demo.terminal.history.TerminalHistoryService;
import com.waveterm.demo.terminal.ssh.model.SshErrorCode;
import com.waveterm.demo.terminal.ssh.model.SshTerminalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Primary
@ConditionalOnProperty(prefix = "terminal.ssh", name = "enabled", havingValue = "true")
public class SshTerminalGateway implements TerminalGateway, com.waveterm.demo.terminal.gateway.SshTerminalGatewayAware {

    private static final Logger log = LoggerFactory.getLogger(SshTerminalGateway.class);

    private final TerminalHistoryService historyService;
    private final SshTargetProperties targetProperties;
    private final Map<String, SshTerminalSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService sweeper;

    public SshTerminalGateway(TerminalHistoryService historyService,
                              SshTargetProperties targetProperties) {
        this.historyService = historyService;
        this.targetProperties = targetProperties;
        this.sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ssh-terminal-session-sweeper");
            t.setDaemon(true);
            return t;
        });
        long intervalMillis = Math.max(targetProperties.findDefaultTarget()
                .map(target -> target.getIdleTimeout().toMillis() / 2)
                .orElse(5_000L), 1_000);
        this.sweeper.scheduleAtFixedRate(this::sweepSessions, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        targetProperties.findDefaultTarget().ifPresentOrElse(target ->
                        log.info("SSH terminal gateway initialized for target {}@{}:{} (id={})",
                                target.getUsername(), target.getHost(), target.getPort(), target.getId()),
                () -> log.warn("SSH terminal gateway enabled but no targets configured"));
    }

    @Override
    public ManagedTerminalSession getOrCreate(String sessionId) throws IOException {
        ensureConfigured();
        if (sessionId != null) {
            SshTerminalSession existing = sessions.get(sessionId);
            if (existing != null) {
                existing.touch();
                return existing;
            }
        }
        SshTargetProperties.Target target = targetProperties.findDefaultTarget()
                .orElseThrow(() -> new SshTerminalException(SshErrorCode.UNKNOWN, "未配置任何 SSH 目标。"));
        SshTerminalSession session = openSession(sessionId, target);
        sessions.put(session.getSessionId(), session);
        return session;
    }

    @Override
    public ManagedTerminalSession getOrCreate(String sessionId, String targetId) throws IOException {
        ensureConfigured();
        if (sessionId != null) {
            SshTerminalSession existing = sessions.get(sessionId);
            if (existing != null) {
                existing.touch();
                return existing;
            }
        }
        SshTargetProperties.Target target = targetProperties.findTarget(targetId)
                .orElseThrow(() -> new SshTerminalException(SshErrorCode.UNKNOWN,
                        "SSH 目标不存在：" + targetId));
        SshTerminalSession session = openSession(sessionId, target);
        sessions.put(session.getSessionId(), session);
        return session;
    }

    @Override
    public Optional<ManagedTerminalSession> get(String sessionId) {
        return Optional.ofNullable(sessionId).map(sessions::get);
    }

    @Override
    public void remove(String sessionId) {
        if (sessionId == null) {
            return;
        }
        SshTerminalSession session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
        }
    }

    @PreDestroy
    public void shutdown() {
        sweeper.shutdownNow();
        sessions.values().forEach(SshTerminalSession::close);
        sessions.clear();
    }

    private void sweepSessions() {
        sessions.entrySet().removeIf(entry -> {
            SshTerminalSession session = entry.getValue();
            if (session == null) {
                return true;
            }
            if (!session.isAlive()) {
                log.debug("Removing terminated SSH session {}", entry.getKey());
                session.close();
                return true;
            }
            if (session.isIdle(session.getIdleTimeout())) {
                log.debug("Removing idle SSH session {}", entry.getKey());
                session.close();
                return true;
            }
            return false;
        });
    }

    private SshTerminalSession openSession(String requestedId,
                                           SshTargetProperties.Target target) throws IOException {
        int attempts = Math.max(1, target.getMaxRetries() + 1);
        IOException lastException = null;
        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                JSch jsch = new JSch();
                target.applyToSession(jsch);
                Session session = jsch.getSession(
                        target.getUsername(),
                        target.getHost(),
                        target.getPort());

                if (target.getPassword() != null && !target.getPassword().isBlank()) {
                    session.setPassword(target.getPassword());
                }

                Properties config = new Properties();
                config.put("StrictHostKeyChecking", target.isStrictHostKeyChecking() ? "yes" : "no");
                session.setConfig(config);
                session.connect((int) target.getConnectTimeout().toMillis());

                ChannelShell channel = (ChannelShell) session.openChannel("shell");
                channel.setPty(true);
                channel.setPtyType("xterm-256color");
                channel.connect((int) target.getConnectTimeout().toMillis());

                SshTerminalSession sshSession = new SshTerminalSession(requestedId, historyService,
                        session, channel, target);
                if (target.getWorkingDirectory() != null && !target.getWorkingDirectory().isBlank()) {
                    sshSession.sendCommand("cd " + target.getWorkingDirectory());
                }
                sshSession.touch();
                if (attempt > 0) {
                    log.info("SSH session recovered for target {} after {} retry", target.getId(), attempt);
                }
                return sshSession;
            } catch (JSchException e) {
                lastException = classifyException(target, e);
                log.warn("Attempt {}/{} to open SSH session for target {} failed: {}",
                        attempt + 1, attempts, target.getId(), e.getMessage());
            } catch (IOException e) {
                lastException = new SshTerminalException(SshErrorCode.IO_ERROR, "SSH I/O 错误：" + e.getMessage(), e);
                log.warn("Attempt {}/{} to open SSH session for target {} failed: {}",
                        attempt + 1, attempts, target.getId(), e.getMessage());
            }

            if (attempt < attempts - 1) {
                sleepBackoff(target, attempt + 1);
            }
        }
        throw lastException != null ? lastException :
                new SshTerminalException(SshErrorCode.UNKNOWN, "SSH 连接失败，原因未知。");
    }

    private void ensureConfigured() throws IOException {
        if (!targetProperties.isEnabled()) {
            throw new IOException("SSH terminal not enabled.");
        }
        if (targetProperties.getTargets().isEmpty()) {
            throw new IOException("No SSH targets configured.");
        }
    }

    private SshTerminalException classifyException(SshTargetProperties.Target target, JSchException e) {
        String message = e.getMessage() != null ? e.getMessage() : "";
        if (message.contains("Auth fail")) {
            return new SshTerminalException(SshErrorCode.AUTHENTICATION_FAILED,
                    "SSH 认证失败，目标 " + target.getHost(), e);
        }
        if (message.contains("UnknownHostKey") || message.contains("reject HostKey")) {
            return new SshTerminalException(SshErrorCode.HOST_KEY_VERIFICATION_FAILED,
                    "SSH 主机指纹验证失败：" + target.getHost(), e);
        }
        if (message.contains("timeout") || message.contains("Connection timed out")) {
            return new SshTerminalException(SshErrorCode.CONNECTION_TIMEOUT,
                    "连接 SSH 服务器超时：" + target.getHost(), e);
        }
        if (message.contains("ChannelException") || message.contains("open failed")) {
            return new SshTerminalException(SshErrorCode.CHANNEL_OPEN_FAILED,
                    "SSH 通道打开失败：" + target.getHost(), e);
        }
        return new SshTerminalException(SshErrorCode.UNKNOWN,
                "SSH 连接错误：" + message, e);
    }

    private void sleepBackoff(SshTargetProperties.Target target, int attempt) {
        try {
            long millis = Math.max(200L, target.getRetryBackoff().toMillis() * attempt);
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}


