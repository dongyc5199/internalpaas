package com.waveterm.demo.terminal;

import java.io.IOException;
import java.util.Optional;

/**
 * Abstraction over terminal backends so the rest of the application can talk to
 * either local PTY sessions or SSH-based sessions.
 */
public interface TerminalGateway {

    /** Returns an existing session or creates a new one if needed. */
    ManagedTerminalSession getOrCreate(String sessionId) throws IOException;

    /** Looks up a session by ID. */
    Optional<ManagedTerminalSession> get(String sessionId);

    /** Removes the session and releases all related resources. */
    void remove(String sessionId);
}
