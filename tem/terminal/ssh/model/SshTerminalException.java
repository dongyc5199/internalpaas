package com.waveterm.demo.terminal.ssh.model;

import java.io.IOException;

public class SshTerminalException extends IOException {

    private final SshErrorCode code;

    public SshTerminalException(SshErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public SshTerminalException(SshErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public SshErrorCode getCode() {
        return code;
    }
}
