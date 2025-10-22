package com.waveterm.demo.terminal;

import com.waveterm.demo.terminal.history.TerminalHistoryService;
import com.waveterm.demo.terminal.pty.PtyProcess;
import com.waveterm.demo.terminal.pty.PtyProcessFactory;
import com.waveterm.demo.terminal.pty.PtySignal;
import com.waveterm.demo.terminal.pty.ShellLaunchConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

class TerminalSession implements ManagedTerminalSession {

    private static final Logger log = LoggerFactory.getLogger(TerminalSession.class);

    private final String sessionId;
    private final PtyProcess ptyProcess;
    private final BufferedWriter writer;
    private final ExecutorService outputExecutor;
    private final TerminalHistoryService historyService;
    private final List<Consumer<String>> outputListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Integer>> exitListeners = new CopyOnWriteArrayList<>();
    private final AtomicLong lastActivityMillis = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean exitNotified = new AtomicBoolean(false);
    private final Charset charset;
    private final String lineSeparator;
    private final Deque<String> recentOutput = new ArrayDeque<>();
    private static final int MAX_RECENT_OUTPUT = 20;

    TerminalSession(String sessionId,
                    TerminalHistoryService historyService,
                    TerminalShellProperties shellProperties,
                    PtyProcessFactory processFactory) throws IOException {
        this.sessionId = sessionId != null ? sessionId : UUID.randomUUID().toString();
        this.historyService = historyService;
        ShellLaunchConfiguration launchConfiguration = shellProperties.toLaunchConfiguration();
        this.charset = launchConfiguration.charset();
        String configuredSeparator = launchConfiguration.lineSeparator();
        this.lineSeparator = configuredSeparator != null && !configuredSeparator.isEmpty()
                ? configuredSeparator
                : "\n";
        this.ptyProcess = processFactory.spawn(launchConfiguration);
        this.writer = new BufferedWriter(new OutputStreamWriter(ptyProcess.getOutputStream(), this.charset));
        this.outputExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "terminal-session-" + this.sessionId);
            t.setDaemon(true);
            return t;
        });
        this.outputExecutor.submit(this::pumpOutput);
        log.info("Started terminal session {} using command {}", this.sessionId, String.join(" ", launchConfiguration.command()));
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void addOutputListener(Consumer<String> listener) {
        Objects.requireNonNull(listener, "listener");
        outputListeners.add(listener);
        emitRecentOutput(listener);
    }

    @Override
    public void removeOutputListener(Consumer<String> listener) {
        outputListeners.remove(listener);
    }

    @Override
    public void addExitListener(Consumer<Integer> listener) {
        Objects.requireNonNull(listener, "listener");
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
        String trimmed = trimTrailingNewlines(command);
        writer.write(trimmed);
        writer.write(lineSeparator);
        writer.flush();
        touch();
    }

    @Override
    public void sendInput(String data) throws IOException {
        if (data == null || data.isEmpty()) {
            return;
        }
        String normalized = data.replace("\r\n", "\n")
                .replace('\r', '\n');

        StringBuilder builder = new StringBuilder(normalized.length() * Math.max(1, lineSeparator.length()));
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch == '\u007f' || ch == '\b') {
                builder.append('\b');
            } else if (ch == '\n') {
                builder.append(lineSeparator);
            } else {
                builder.append(ch);
            }
        }

        writer.write(builder.toString());
        writer.flush();
        touch();
    }

    @Override
    public void resize(int columns, int rows) throws IOException {
        ptyProcess.resize(columns, rows);
        touch();
    }

    @Override
    public void sendSignal(PtySignal signal) throws IOException {
        ptyProcess.sendSignal(signal);
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
        return ptyProcess.isAlive();
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException ignored) {
        }
        int exitCode = ptyProcess.exitCode();
        ptyProcess.close();
        outputExecutor.shutdownNow();
        notifyExit(exitCode);
    }

    private void pumpOutput() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ptyProcess.getInputStream(), charset))) {
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, read);
                recordOutputChunk(chunk);
                touch();
                for (Consumer<String> listener : outputListeners) {
                    listener.accept(chunk);
                }
            }
            notifyExit(ptyProcess.exitCode());
        } catch (IOException ex) {
            recordOutputChunk("[terminal-error] " + ex.getMessage());
            for (Consumer<String> listener : outputListeners) {
                listener.accept("\n[terminal-error] " + ex.getMessage() + "\n");
            }
            notifyExit(-1);
        }
    }

    private void notifyExit(int exitCode) {
        if (!exitNotified.compareAndSet(false, true)) {
            return;
        }
        if (exitCode != 0) {
            log.warn("Terminal session {} exited with code {}. Recent output:\n{}", sessionId, exitCode, collectRecentOutput());
        } else if (log.isDebugEnabled()) {
            log.debug("Terminal session {} exited normally. Recent output:\n{}", sessionId, collectRecentOutput());
        }
        for (Consumer<Integer> listener : exitListeners) {
            try {
                listener.accept(exitCode);
            } catch (RuntimeException ex) {
                log.debug("Terminal exit listener error", ex);
            }
        }
    }

    private void recordOutputChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        String sanitized = sanitizeChunk(chunk);
        synchronized (recentOutput) {
            recentOutput.addLast(sanitized);
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
    }

    private String sanitizeChunk(String chunk) {
        StringBuilder builder = new StringBuilder(chunk.length());
        for (int i = 0; i < chunk.length(); i++) {
            char ch = chunk.charAt(i);
            if (ch >= 0x20 || ch == '\n' || ch == '\r' || ch == '\t') {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private String trimTrailingNewlines(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        int end = input.length();
        while (end > 0) {
            char ch = input.charAt(end - 1);
            if (ch == '\n' || ch == '\r') {
                end--;
            } else {
                break;
            }
        }
        if (end == input.length()) {
            return input;
        }
        return input.substring(0, end);
    }
}
