package com.waveterm.demo.terminal.ssh;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.waveterm.demo.terminal.ManagedTerminalSession;
import com.waveterm.demo.terminal.history.TerminalHistoryService;
import com.waveterm.demo.terminal.pty.PtySignal;
import com.waveterm.demo.terminal.ssh.model.SshErrorCode;
import com.waveterm.demo.terminal.ssh.model.SshTerminalError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.Optional;

final class SshTerminalSession implements ManagedTerminalSession {

    private static final Logger log = LoggerFactory.getLogger(SshTerminalSession.class);
    private static final int BUFFER_SIZE = 2048;
    private static final int MAX_RECENT_OUTPUT = 20;

    private final String sessionId;
    private final TerminalHistoryService historyService;
    private final Session jschSession;
    private final ChannelShell channelShell;
    private final Charset charset;
    private final ExecutorService outputExecutor;
    private final List<Consumer<String>> outputListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Integer>> exitListeners = new CopyOnWriteArrayList<>();
    private final AtomicLong lastActivityMillis = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean exitNotified = new AtomicBoolean(false);
    private final Deque<String> recentOutput = new ArrayDeque<>();
    private final Object writeLock = new Object();
    private final SshTargetProperties.Target target;
    private volatile SshTerminalError lastError;

    private final OutputStream commandStream;
    private final InputStream outputStream;

    SshTerminalSession(String sessionId,
                       TerminalHistoryService historyService,
                       Session jschSession,
                       ChannelShell channelShell,
                       SshTargetProperties.Target target) throws IOException, JSchException {
        this.sessionId = sessionId != null ? sessionId : "ssh-" + UUID.randomUUID().toString().replace("-", "");
        this.historyService = historyService;
        this.jschSession = jschSession;
        this.channelShell = channelShell;
        this.target = target;
        this.charset = target.getCharset();
        this.commandStream = channelShell.getOutputStream();
        this.outputStream = channelShell.getInputStream();
        this.outputExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ssh-terminal-session-" + this.sessionId);
            t.setDaemon(true);
            return t;
        });
        this.outputExecutor.submit(this::pumpOutput);
        log.info("Started SSH terminal session {} -> {}@{} (target={})", this.sessionId,
                target.getUsername(), target.getHost(), target.getId());
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void addOutputListener(Consumer<String> listener) {
        outputListeners.add(listener);
        emitRecentOutput(listener);
    }

    @Override
    public void removeOutputListener(Consumer<String> listener) {
        outputListeners.remove(listener);
    }

    @Override
    public void addExitListener(Consumer<Integer> listener) {
        exitListeners.add(listener);
    }

    @Override
    public void removeExitListener(Consumer<Integer> listener) {
        exitListeners.remove(listener);
    }

    @Override
    public void sendCommand(String command) throws IOException {
        if (command == null) {
            return;
        }
        String commandId = UUID.randomUUID().toString();
        historyService.append(sessionId, commandId, command, Instant.now().toEpochMilli());
        writeInternal(command.endsWith("\n") ? command : command + "\n");
    }

    @Override
    public void sendInput(String data) throws IOException {
        if (data == null || data.isEmpty()) {
            return;
        }
        writeInternal(data);
    }

    private void writeInternal(String data) throws IOException {
        synchronized (writeLock) {
            commandStream.write(data.getBytes(charset));
            commandStream.flush();
        }
        touch();
    }

    @Override
    public void resize(int columns, int rows) throws IOException {
        try {
            channelShell.setPtySize(columns, rows, columns * 8, rows * 16);
        } catch (Exception ex) {
            throw new IOException("Failed to resize SSH PTY", ex);
        }
        touch();
    }

    @Override
    public void sendSignal(PtySignal signal) throws IOException {
        if (signal == null) {
            return;
        }
        try {
            channelShell.sendSignal(signal.name());
        } catch (Exception ex) {
            throw new IOException("Failed to send SSH signal " + signal, ex);
        }
        touch();
    }

    @Override
    public void touch() {
        lastActivityMillis.set(System.currentTimeMillis());
    }

    @Override
    public boolean isIdle(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return false;
        }
        long idleMillis = System.currentTimeMillis() - lastActivityMillis.get();
        return idleMillis >= timeout.toMillis();
    }

    @Override
    public boolean isAlive() {
        return channelShell.isConnected() && jschSession.isConnected();
    }

    @Override
    public void close() {
        outputExecutor.shutdownNow();
        try {
            commandStream.close();
        } catch (IOException ignored) {
        }
        if (channelShell.isConnected()) {
            channelShell.disconnect();
        }
        if (jschSession.isConnected()) {
            jschSession.disconnect();
        }
        notifyExit(channelShell.getExitStatus());
    }

    private void pumpOutput() {
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            while (isAlive()) {
                int read = outputStream.read(buffer);
                if (read < 0) {
                    break;
                }
                if (read == 0) {
                    continue;
                }
                String chunk = new String(buffer, 0, read, charset);
                recordOutputChunk(chunk);
                touch();
                    for (Consumer<String> listener : outputListeners) {
                        listener.accept(chunk);
                    }
                }
            } catch (IOException ex) {
                recordOutputChunk("[ssh-terminal-error] " + ex.getMessage());
                lastError = SshTerminalError.ioError(ex.getMessage());
                for (Consumer<String> listener : outputListeners) {
                    listener.accept("\n[ssh-terminal-error] " + ex.getMessage() + "\n");
                }
            } finally {
                notifyExit(channelShell.getExitStatus());
        }
    }

    private void notifyExit(int exitCode) {
        if (!exitNotified.compareAndSet(false, true)) {
            return;
        }
        if (exitCode != 0) {
            log.warn("SSH terminal session {} exited with code {}. Recent output:\n{}", sessionId, exitCode,
                    collectRecentOutput());
            if (lastError == null) {
                String msg = exitCode == 255
                        ? "SSH 会话已关闭，远端程序异常退出。"
                        : "SSH shell 退出，exitCode=" + exitCode;
                lastError = SshTerminalError.ioError(msg);
            }
        } else if (log.isDebugEnabled()) {
            log.debug("SSH terminal session {} exited normally. Recent output:\n{}", sessionId, collectRecentOutput());
        }
        for (Consumer<Integer> listener : exitListeners) {
            try {
                listener.accept(exitCode);
            } catch (RuntimeException ex) {
                log.debug("SSH terminal exit listener error", ex);
            }
        }
    }

    private void recordOutputChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        synchronized (recentOutput) {
            recentOutput.addLast(chunk);
            while (recentOutput.size() > MAX_RECENT_OUTPUT) {
                recentOutput.removeFirst();
            }
        }
    }

    private String collectRecentOutput() {
        synchronized (recentOutput) {
            return String.join("", recentOutput);
        }
    }

    private void emitRecentOutput(Consumer<String> listener) {
        String snapshot = collectRecentOutput();
        if (!snapshot.isEmpty()) {
            listener.accept(snapshot);
        }
        SshTerminalError error = lastError;
        if (error != null) {
            listener.accept(error.toDisplayMessage());
        }
    }

    Optional<SshTerminalError> getLastError() {
        return Optional.ofNullable(lastError);
    }

    Duration getIdleTimeout() {
        return target.getIdleTimeout();
    }
}
