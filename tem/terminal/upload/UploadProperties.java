package com.waveterm.demo.terminal.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "terminal.upload")
public class UploadProperties {

    private Path baseDir = Path.of(System.getProperty("user.home"), ".waveterm", "uploads");
    private long maxSizeBytes = 5 * 1024 * 1024;
    private List<String> allowedContentTypes = new ArrayList<>();
    private Duration ttl = Duration.ofHours(1);

    public Path getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(Path baseDir) {
        if (baseDir != null) {
            this.baseDir = baseDir;
        }
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        if (maxSizeBytes > 0) {
            this.maxSizeBytes = maxSizeBytes;
        }
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
            this.ttl = ttl;
        }
    }
}
