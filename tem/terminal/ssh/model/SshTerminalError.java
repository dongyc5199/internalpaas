package com.waveterm.demo.terminal.ssh.model;

import java.util.Objects;

public final class SshTerminalError {

    private final SshErrorCode code;
    private final String message;

    private SshTerminalError(SshErrorCode code, String message) {
        this.code = Objects.requireNonNull(code, "code");
        this.message = message != null && !message.isBlank()
                ? message
                : code.getDefaultMessage();
    }

    public static SshTerminalError of(SshErrorCode code, String message) {
        return new SshTerminalError(code, message);
    }

    public static SshTerminalError connectionTimeout(String message) {
        return of(SshErrorCode.CONNECTION_TIMEOUT, message);
    }

    public static SshTerminalError authFailed(String message) {
        return of(SshErrorCode.AUTHENTICATION_FAILED, message);
    }

    public static SshTerminalError hostKeyMismatch(String message) {
        return of(SshErrorCode.HOST_KEY_VERIFICATION_FAILED, message);
    }

    public static SshTerminalError channelOpenFailed(String message) {
        return of(SshErrorCode.CHANNEL_OPEN_FAILED, message);
    }

    public static SshTerminalError ioError(String message) {
        return of(SshErrorCode.IO_ERROR, message);
    }

    public static SshTerminalError unknown(String message) {
        return of(SshErrorCode.UNKNOWN, message);
    }

    public SshErrorCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String toDisplayMessage() {
        return "[ssh-error] " + message;
    }
}
