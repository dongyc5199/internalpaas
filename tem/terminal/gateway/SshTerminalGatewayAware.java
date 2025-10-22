package com.waveterm.demo.terminal.gateway;

import com.waveterm.demo.terminal.ManagedTerminalSession;
import java.io.IOException;

/**
 * Optional extension implemented by terminal gateways that support specifying an explicit target.
 */
public interface SshTerminalGatewayAware {

    ManagedTerminalSession getOrCreate(String sessionId, String targetId) throws IOException;
}
