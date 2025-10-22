package com.waveterm.demo.terminal.pty;

import java.io.InputStream;
import java.io.OutputStream;

class ProcessPtyProcess implements PtyProcess {

    private final Process delegate;

    ProcessPtyProcess(Process delegate) {
        this.delegate = delegate;
    }

    @Override
    public InputStream getInputStream() {
        return delegate.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() {
        return delegate.getOutputStream();
    }

    @Override
    public void resize(int columns, int rows) {
        // no-op for plain ProcessBuilder
    }

    @Override
    public void sendSignal(PtySignal signal) {
        if (signal == null) {
            return;
        }
        switch (signal) {
            case INTERRUPT, TERMINATE -> delegate.destroy();
            case KILL -> delegate.destroyForcibly();
        }
    }

    @Override
    public boolean isAlive() {
        return delegate.isAlive();
    }

    @Override
    public int exitCode() {
        try {
            return delegate.exitValue();
        } catch (IllegalThreadStateException ex) {
            return -1;
        }
    }

    @Override
    public void close() {
        delegate.destroy();
    }
}
