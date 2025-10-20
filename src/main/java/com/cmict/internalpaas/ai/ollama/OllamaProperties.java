package com.cmict.internalpaas.ai.ollama;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 本地 Ollama 模型配置。
 */
@Component
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {

    private String host = "http://127.0.0.1";
    private int port = 11434;
    private String model = "gpt-oss:20b";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofMinutes(5);
    private boolean enabled = true;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        if (host != null && !host.isBlank()) {
            this.host = host;
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        if (port > 0) {
            this.port = port;
        }
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        if (model != null && !model.isBlank()) {
            this.model = model;
        }
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        if (connectTimeout != null && !connectTimeout.isNegative() && !connectTimeout.isZero()) {
            this.connectTimeout = connectTimeout;
        }
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        if (readTimeout != null && !readTimeout.isNegative() && !readTimeout.isZero()) {
            this.readTimeout = readTimeout;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
