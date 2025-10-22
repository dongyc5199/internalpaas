package com.waveterm.demo.terminal.pty;

import java.io.IOException;

public interface PtyProcessFactory {

    PtyProcess spawn(ShellLaunchConfiguration configuration) throws IOException;
}
