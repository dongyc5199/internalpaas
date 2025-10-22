package com.waveterm.demo.terminal;

import com.waveterm.demo.terminal.pty.PtySignal;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * Minimal contract implemented by terminal backends so WebSocket handlers and
 * controllers can treat local PTY and SSH sessions uniformly.
 */
public interface ManagedTerminalSession {

    String getSessionId();

    void addOutputListener(Consumer<String> listener);

    void removeOutputListener(Consumer<String> listener);

    void addExitListener(Consumer<Integer> listener);

    void removeExitListener(Consumer<Integer> listener);

    void sendCommand(String command) throws IOException;

    void sendInput(String data) throws IOException;

    void resize(int columns, int rows) throws IOException;

    void sendSignal(PtySignal signal) throws IOException;

    void touch();

    boolean isIdle(Duration timeout);

    boolean isAlive();

    void close();
}
