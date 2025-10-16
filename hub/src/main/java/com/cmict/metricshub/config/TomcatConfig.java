package com.cmict.metricshub.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat configuration for multiple HTTP ports.
 *
 * This configures Tomcat to listen on two ports:
 * - 8080: Main API and Actuator endpoints
 * - 4318: OTLP HTTP endpoint (as per OTLP specification)
 *
 * The OTLP HTTP endpoint at 4318 is added as an additional connector
 * so both ports can receive HTTP traffic.
 */
@Configuration
public class TomcatConfig {

    @Value("${metrics-hub.ingest.http.port:4318}")
    private int otlpHttpPort;

    /**
     * Add an additional Tomcat connector for OTLP HTTP port 4318.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer() {
        return factory -> factory.addAdditionalTomcatConnectors(createOtlpConnector());
    }

    private Connector createOtlpConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(otlpHttpPort);
        connector.setSecure(false);
        connector.setRedirectPort(8443);  // For potential future HTTPS redirect

        // Set max post size to match OTLP payload size
        connector.setMaxPostSize(4194304);  // 4MB

        return connector;
    }
}
