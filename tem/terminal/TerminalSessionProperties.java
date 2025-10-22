package com.waveterm.demo.terminal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "terminal.session")
public class TerminalSessionProperties {

    /**
     * 会话闲置超时时间，默认 15 分钟。
     */
    private Duration idleTimeout = Duration.ofMinutes(15);

    /**
     * 守护线程扫描间隔，默认 1 分钟。
     */
    private Duration sweepInterval = Duration.ofMinutes(1);

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Duration idleTimeout) {
        if (idleTimeout != null) {
            this.idleTimeout = idleTimeout;
        }
    }

    public Duration getSweepInterval() {
        return sweepInterval;
    }

    public void setSweepInterval(Duration sweepInterval) {
        if (sweepInterval != null) {
            this.sweepInterval = sweepInterval;
        }
    }
}
