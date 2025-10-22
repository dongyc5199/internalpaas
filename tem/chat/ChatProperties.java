package com.waveterm.demo.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    private String defaultClient = "echo";
    private Duration requestTimeout = Duration.ofSeconds(30);
    private int maxRetries = 1;
    private Duration cacheTtl = Duration.ofSeconds(30);
    private final Map<String, ClientConfig> clients = new HashMap<>();

    public String getDefaultClient() {
        return defaultClient;
    }

    public void setDefaultClient(String defaultClient) {
        if (defaultClient != null && !defaultClient.isBlank()) {
            this.defaultClient = defaultClient;
        }
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        if (requestTimeout != null && !requestTimeout.isNegative() && !requestTimeout.isZero()) {
            this.requestTimeout = requestTimeout;
        }
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        if (maxRetries >= 0) {
            this.maxRetries = maxRetries;
        }
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        if (cacheTtl != null && !cacheTtl.isNegative()) {
            this.cacheTtl = cacheTtl;
        }
    }

    public Map<String, ClientConfig> getClients() {
        return clients;
    }

    public record ClientConfig(int weight, Duration timeout) {
        public ClientConfig {
            if (weight <= 0) {
                weight = 1;
            }
            if (timeout == null || timeout.isNegative() || timeout.isZero()) {
                timeout = Duration.ofSeconds(30);
            }
        }
    }
}
