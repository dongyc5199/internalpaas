package com.waveterm.demo.terminal.pty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Minimal abstraction over a PTY backed process.
 */
public interface PtyProcess extends AutoCloseable {

    InputStream getInputStream();

    OutputStream getOutputStream();

    default void resize(int columns, int rows) throws IOException {
        // optional
    }

    default void sendSignal(PtySignal signal) throws IOException {
        // optional
    }

    default boolean isAlive() {
        return true;
    }

    default int exitCode() {
        return -1;
    }

    @Override
    void close();
}
