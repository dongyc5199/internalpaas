package com.waveterm.demo.terminal.history;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "terminal.history")
public class TerminalHistoryProperties {

    /**
     * 单个会话保留的最大命令条数。
     */
    private int retentionPerSession = 500;

    /**
     * 命令保留的最长时间。
     */
    private Duration retentionTtl = Duration.ofDays(30);

    /**
     * 清理任务执行间隔。
     */
    private Duration cleanupInterval = Duration.ofMinutes(30);

    public int getRetentionPerSession() {
        return retentionPerSession;
    }

    public void setRetentionPerSession(int retentionPerSession) {
        if (retentionPerSession > 0) {
            this.retentionPerSession = retentionPerSession;
        }
    }

    public Duration getRetentionTtl() {
        return retentionTtl;
    }

    public void setRetentionTtl(Duration retentionTtl) {
        if (retentionTtl != null && !retentionTtl.isNegative() && !retentionTtl.isZero()) {
            this.retentionTtl = retentionTtl;
        }
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        if (cleanupInterval != null && !cleanupInterval.isNegative() && !cleanupInterval.isZero()) {
            this.cleanupInterval = cleanupInterval;
        }
    }
}
