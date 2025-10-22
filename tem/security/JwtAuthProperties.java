package com.waveterm.demo.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtAuthProperties {

    private String issuer = "waveterm";
    private String audience = "waveterm-clients";
    private String secret;
    private Duration clockSkew = Duration.ofMinutes(1);
    private final Set<String> excludePaths = new HashSet<>();

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        if (issuer != null) {
            this.issuer = issuer;
        }
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        if (audience != null) {
            this.audience = audience;
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        if (clockSkew != null) {
            this.clockSkew = clockSkew;
        }
    }

    public Set<String> getExcludePaths() {
        return excludePaths;
    }
}
