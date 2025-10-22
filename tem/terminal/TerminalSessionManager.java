package com.waveterm.demo.terminal;

import com.waveterm.demo.terminal.history.TerminalHistoryService;
import com.waveterm.demo.terminal.pty.PtyProcessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "terminal.ssh", name = "enabled", havingValue = "false", matchIfMissing = true)
public class TerminalSessionManager implements TerminalGateway {

    private static final Logger log = LoggerFactory.getLogger(TerminalSessionManager.class);

    private final Map<String, ManagedTerminalSession> sessions = new ConcurrentHashMap<>();
    private final TerminalHistoryService historyService;
    private final TerminalShellProperties shellProperties;
    private final PtyProcessFactory processFactory;
    private final TerminalSessionProperties sessionProperties;
    private final ScheduledExecutorService sweeper;

    public TerminalSessionManager(TerminalHistoryService historyService,
                                  TerminalShellProperties shellProperties,
                                  PtyProcessFactory processFactory,
                                  TerminalSessionProperties sessionProperties) {
        this.historyService = historyService;
        this.shellProperties = shellProperties;
        this.processFactory = processFactory;
        this.sessionProperties = sessionProperties;
        this.sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "terminal-session-sweeper");
            t.setDaemon(true);
            return t;
        });
        Duration sweepInterval = sessionProperties.getSweepInterval();
        long intervalMillis = Math.max(sweepInterval.toMillis(), 1000);
        sweeper.scheduleAtFixedRate(this::sweepSessions, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ManagedTerminalSession getOrCreate(String sessionId) throws IOException {
        if (sessionId != null) {
            ManagedTerminalSession existing = sessions.get(sessionId);
            if (existing != null) {
                existing.touch();
                return existing;
            }
        }
        ManagedTerminalSession session = new TerminalSession(sessionId, historyService, shellProperties, processFactory);
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
        ManagedTerminalSession session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
        }
    }

    @PreDestroy
    public void shutdown() {
        sweeper.shutdownNow();
        sessions.values().forEach(ManagedTerminalSession::close);
        sessions.clear();
    }

    private void sweepSessions() {
        Duration idleTimeout = sessionProperties.getIdleTimeout();
        sessions.entrySet().removeIf(entry -> {
            ManagedTerminalSession session = entry.getValue();
            if (session == null) {
                return true;
            }
            if (!session.isAlive()) {
                log.debug("Removing terminated terminal session {}", entry.getKey());
                session.close();
                return true;
            }
            if (session.isIdle(idleTimeout)) {
                log.debug("Removing idle terminal session {}", entry.getKey());
                session.close();
                return true;
            }
            return false;
        });
    }
}
