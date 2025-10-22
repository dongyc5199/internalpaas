package com.waveterm.demo.terminal.ssh;

import com.jcraft.jsch.JSch;
import java.util.Map;
import com.jcraft.jsch.JSchException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ConfigurationProperties(prefix = "terminal.ssh")
public class SshTargetProperties {

    /**
     * Whether SSH terminal support is enabled.
     */
    private boolean enabled = false;

    /**
     * Default target ID to use when no explicit target is requested.
     */
    private String defaultTargetId;

    /**
     * List of SSH targets (single-node demo can provide just one item).
     */
    private final List<Target> targets = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultTargetId() {
        return defaultTargetId;
    }

    public void setDefaultTargetId(String defaultTargetId) {
        this.defaultTargetId = defaultTargetId;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public Optional<Target> findTarget(String targetId) {
        if (targetId == null || targetId.isBlank()) {
            return findDefaultTarget();
        }
        return targets.stream()
                .filter(target -> targetId.equals(target.getId()))
                .findFirst();
    }

    public Optional<Target> findDefaultTarget() {
        if (targets.isEmpty()) {
            return Optional.empty();
        }
        if (defaultTargetId != null) {
            return targets.stream()
                    .filter(target -> defaultTargetId.equals(target.getId()))
                    .findFirst()
                    .or(() -> Optional.of(targets.get(0)));
        }
        return Optional.of(targets.get(0));
    }

    public static class Target {
        private String id;
        private String name;
        private String host;
        private int port = 22;
        private String username = "root";
        private String password;
        private Charset charset = StandardCharsets.UTF_8;
        private String workingDirectory;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration idleTimeout = Duration.ofMinutes(10);
        private boolean strictHostKeyChecking = false;
        private String knownHosts;
        private String identityPath;
        private String identityPassword;
        private int maxRetries = 3;
        private Duration retryBackoff = Duration.ofSeconds(2);

        public void applyToSession(JSch jsch) throws JSchException {
            applyIdentities(jsch);
            applyKnownHosts(jsch);
        }

        private void applyIdentities(JSch jsch) throws JSchException {
            if (identityPath != null && !identityPath.isBlank()) {
                jsch.addIdentity(identityPath,
                        identityPassword != null && !identityPassword.isBlank()
                                ? identityPassword
                                : null);
            }
        }

        private void applyKnownHosts(JSch jsch) throws JSchException {
            if (knownHosts != null && !knownHosts.isBlank()) {
                jsch.setKnownHosts(knownHosts);
            }
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name != null ? name : id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
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

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            if (connectTimeout != null) {
                this.connectTimeout = connectTimeout;
            }
        }

        public Duration getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(Duration idleTimeout) {
            if (idleTimeout != null) {
                this.idleTimeout = idleTimeout;
            }
        }

        public boolean isStrictHostKeyChecking() {
            return strictHostKeyChecking;
        }

        public void setStrictHostKeyChecking(boolean strictHostKeyChecking) {
            this.strictHostKeyChecking = strictHostKeyChecking;
        }

        public String getKnownHosts() {
            return knownHosts;
        }

        public void setKnownHosts(String knownHosts) {
            this.knownHosts = knownHosts;
        }

        public String getIdentityPath() {
            return identityPath;
        }

        public void setIdentityPath(String identityPath) {
            this.identityPath = identityPath;
        }

        public String getIdentityPassword() {
            return identityPassword;
        }

        public void setIdentityPassword(String identityPassword) {
            this.identityPassword = identityPassword;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = Math.max(0, maxRetries);
        }

        public Duration getRetryBackoff() {
            return retryBackoff;
        }

        public void setRetryBackoff(Duration retryBackoff) {
            if (retryBackoff != null && !retryBackoff.isNegative()) {
                this.retryBackoff = retryBackoff;
            }
        }
    }
}

