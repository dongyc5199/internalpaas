package com.waveterm.demo.terminal;

import com.waveterm.demo.terminal.pty.ShellLaunchConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "terminal.shell")
public class TerminalShellProperties {

    private final List<String> command = new ArrayList<>();
    private Charset charset = StandardCharsets.UTF_8;
    private String workingDirectory;
    private String lineSeparator = "\n";

    public TerminalShellProperties() {
        if (command.isEmpty()) {
            command.add("bash");
            command.add("-l");
        }
    }

    public List<String> getCommand() {
        return command;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        if (charset != null) {
            this.charset = charset;
        }
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public void setLineSeparator(String lineSeparator) {
        if (lineSeparator == null || lineSeparator.isBlank()) {
            return;
        }
        switch (lineSeparator.trim().toUpperCase()) {
            case "CR" -> this.lineSeparator = "\r";
            case "CRLF" -> this.lineSeparator = "\r\n";
            case "LF" -> this.lineSeparator = "\n";
            default -> this.lineSeparator = lineSeparator;
        }
    }

    public ShellLaunchConfiguration toLaunchConfiguration() {
        return new ShellLaunchConfiguration(List.copyOf(command), charset, workingDirectory, lineSeparator);
    }
}
