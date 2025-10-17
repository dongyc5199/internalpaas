package com.cmict.metricshub.security;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TLS/mTLS Configuration for Metrics Hub
 * 
 * Supports:
 * - gRPC Netty SSL Context (for OTLP gRPC endpoint)
 * - Spring Boot HTTP SSL (for REST API and OTLP HTTP endpoint)
 * - Keystore/Truststore management
 * - Certificate rotation monitoring
 * 
 * Configuration:
 * <pre>
 * metrics-hub:
 *   security:
 *     tls:
 *       enabled: true
 *       mtls: true  # Require client certificate
 *       keystore:
 *         path: /path/to/keystore.p12
 *         password: ${KEYSTORE_PASSWORD}
 *         type: PKCS12
 *       truststore:
 *         path: /path/to/truststore.p12
 *         password: ${TRUSTSTORE_PASSWORD}
 *         type: PKCS12
 *       cert-rotation:
 *         check-interval-hours: 24
 *         warn-days-before-expiry: 30
 * </pre>
 * 
 * @author Metrics Hub Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "metrics-hub.security.tls.enabled", havingValue = "true")
public class TlsConfig {

    @Value("${metrics-hub.security.tls.mtls:false}")
    private boolean mtlsEnabled;

    @Value("${metrics-hub.security.tls.keystore.path}")
    private String keystorePath;

    @Value("${metrics-hub.security.tls.keystore.password}")
    private String keystorePassword;

    @Value("${metrics-hub.security.tls.keystore.type:PKCS12}")
    private String keystoreType;

    @Value("${metrics-hub.security.tls.truststore.path:}")
    private String truststorePath;

    @Value("${metrics-hub.security.tls.truststore.password:}")
    private String truststorePassword;

    @Value("${metrics-hub.security.tls.truststore.type:PKCS12}")
    private String truststoreType;

    @Value("${metrics-hub.security.tls.cert-rotation.check-interval-hours:24}")
    private int certCheckIntervalHours;

    @Value("${metrics-hub.security.tls.cert-rotation.warn-days-before-expiry:30}")
    private int warnDaysBeforeExpiry;

    @Getter
    private volatile SslContext grpcSslContext;

    private ScheduledExecutorService certMonitorExecutor;

    /**
     * Configure SSL Context for gRPC Netty Server
     * 
     * @return SslContext for gRPC server
     * @throws Exception if SSL configuration fails
     */
    @Bean
    public SslContext grpcSslContext() throws Exception {
        log.info("🔐 Configuring gRPC SSL Context...");
        log.info("   Keystore: {}", keystorePath);
        log.info("   mTLS Enabled: {}", mtlsEnabled);

        File keystoreFile = new File(keystorePath);
        if (!keystoreFile.exists()) {
            throw new IllegalArgumentException("Keystore file not found: " + keystorePath);
        }

        SslContextBuilder sslContextBuilder = GrpcSslContexts.forServer(
            new File(keystorePath + ".crt"),  // Certificate chain
            new File(keystorePath + ".key")   // Private key
        );

        // Enable mTLS if configured
        if (mtlsEnabled && !truststorePath.isEmpty()) {
            log.info("   mTLS: Enabling client certificate verification");
            File truststoreFile = new File(truststorePath);
            if (!truststoreFile.exists()) {
                throw new IllegalArgumentException("Truststore file not found: " + truststorePath);
            }

            sslContextBuilder
                .trustManager(truststoreFile)
                .clientAuth(ClientAuth.REQUIRE);
        } else {
            log.info("   mTLS: Client certificate verification disabled");
            sslContextBuilder.clientAuth(ClientAuth.NONE);
        }

        this.grpcSslContext = sslContextBuilder.build();
        log.info("✅ gRPC SSL Context configured successfully");

        // Start certificate expiry monitoring
        startCertificateMonitoring();

        return this.grpcSslContext;
    }

    /**
     * Configure SSL for Spring Boot HTTP Server (REST API + OTLP HTTP)
     * 
     * @return WebServerFactoryCustomizer to apply SSL configuration
     */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> sslCustomizer() {
        return factory -> {
            try {
                log.info("🔐 Configuring HTTP SSL...");

                Ssl ssl = new Ssl();
                ssl.setEnabled(true);
                ssl.setKeyStore(keystorePath);
                ssl.setKeyStorePassword(keystorePassword);
                ssl.setKeyStoreType(keystoreType);

                if (mtlsEnabled && !truststorePath.isEmpty()) {
                    ssl.setClientAuth(Ssl.ClientAuth.NEED);
                    ssl.setTrustStore(truststorePath);
                    ssl.setTrustStorePassword(truststorePassword);
                    ssl.setTrustStoreType(truststoreType);
                    log.info("   mTLS: Client certificate required");
                } else {
                    ssl.setClientAuth(Ssl.ClientAuth.NONE);
                    log.info("   mTLS: Client certificate not required");
                }

                factory.setSsl(ssl);
                log.info("✅ HTTP SSL configured successfully");

            } catch (Exception e) {
                log.error("❌ Failed to configure HTTP SSL", e);
                throw new RuntimeException("SSL configuration failed", e);
            }
        };
    }

    /**
     * Load Java KeyStore
     */
    private KeyStore loadKeyStore(String path, String password, String type) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type);
        try (FileInputStream fis = new FileInputStream(path)) {
            keyStore.load(fis, password.toCharArray());
        }
        return keyStore;
    }

    /**
     * Create SSL Context for programmatic use
     */
    @Bean
    public SSLContext javaSSLContext() throws Exception {
        KeyStore keyStore = loadKeyStore(keystorePath, keystorePassword, keystoreType);

        // Initialize KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());

        // Initialize TrustManagerFactory (if mTLS)
        TrustManagerFactory tmf = null;
        if (mtlsEnabled && !truststorePath.isEmpty()) {
            KeyStore trustStore = loadKeyStore(truststorePath, truststorePassword, truststoreType);
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
        }

        // Create SSL Context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
            kmf.getKeyManagers(),
            tmf != null ? tmf.getTrustManagers() : null,
            null
        );

        return sslContext;
    }

    /**
     * Monitor certificate expiry and log warnings
     */
    private void startCertificateMonitoring() {
        certMonitorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "cert-monitor");
            thread.setDaemon(true);
            return thread;
        });

        certMonitorExecutor.scheduleAtFixedRate(
            this::checkCertificateExpiry,
            0,
            certCheckIntervalHours,
            TimeUnit.HOURS
        );

        log.info("📅 Certificate expiry monitoring started (check every {} hours)", certCheckIntervalHours);
    }

    /**
     * Check certificate expiry and log warnings
     */
    private void checkCertificateExpiry() {
        try {
            KeyStore keyStore = loadKeyStore(keystorePath, keystorePassword, keystoreType);
            String alias = keyStore.aliases().nextElement();
            
            java.security.cert.Certificate cert = keyStore.getCertificate(alias);
            if (cert instanceof java.security.cert.X509Certificate) {
                java.security.cert.X509Certificate x509 = (java.security.cert.X509Certificate) cert;
                Instant expiry = x509.getNotAfter().toInstant();
                Instant now = Instant.now();
                
                long daysUntilExpiry = ChronoUnit.DAYS.between(now, expiry);

                if (daysUntilExpiry <= 0) {
                    log.error("❌ CERTIFICATE EXPIRED! Expiry: {}", expiry);
                } else if (daysUntilExpiry <= warnDaysBeforeExpiry) {
                    log.warn("⚠️ Certificate expires in {} days ({})", daysUntilExpiry, expiry);
                } else {
                    log.debug("✅ Certificate valid for {} days (expires {})", daysUntilExpiry, expiry);
                }
            }

        } catch (Exception e) {
            log.error("❌ Failed to check certificate expiry", e);
        }
    }

    /**
     * Reload SSL Context (for certificate rotation)
     * 
     * NOTE: This requires application restart for gRPC.
     * For HTTP, Spring Boot supports graceful SSL reload with Tomcat.
     */
    public void reloadSslContext() {
        log.info("🔄 Reloading SSL Context...");
        try {
            // Reload gRPC SSL Context
            this.grpcSslContext = grpcSslContext();
            log.info("✅ gRPC SSL Context reloaded successfully");

            log.warn("⚠️ HTTP SSL reload requires application restart");

        } catch (Exception e) {
            log.error("❌ Failed to reload SSL Context", e);
            throw new RuntimeException("SSL reload failed", e);
        }
    }

    /**
     * Shutdown hook to clean up resources
     */
    public void destroy() {
        if (certMonitorExecutor != null) {
            certMonitorExecutor.shutdown();
            log.info("🛑 Certificate monitoring stopped");
        }
    }
}
