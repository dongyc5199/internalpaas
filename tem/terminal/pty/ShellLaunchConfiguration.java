package com.waveterm.demo.terminal.pty;

import java.nio.charset.Charset;
import java.util.List;

public record ShellLaunchConfiguration(List<String> command,
                                       Charset charset,
                                       String workingDirectory,
                                       String lineSeparator) {
}
