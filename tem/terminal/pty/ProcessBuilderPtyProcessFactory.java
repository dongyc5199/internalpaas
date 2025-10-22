package com.waveterm.demo.terminal.pty;

import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ProcessBuilderPtyProcessFactory implements PtyProcessFactory {

    @Override
    public PtyProcess spawn(ShellLaunchConfiguration configuration) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(configuration.command());
        if (configuration.workingDirectory() != null && !configuration.workingDirectory().isBlank()) {
            builder.directory(new java.io.File(configuration.workingDirectory()));
        }
        builder.redirectErrorStream(true);
        Process process = builder.start();
        return new ProcessPtyProcess(process);
    }
}
