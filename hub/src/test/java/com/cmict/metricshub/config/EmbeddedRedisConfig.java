package com.cmict.metricshub.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * Test configuration that starts an embedded Redis server for integration tests.
 *
 * This ensures tests don't depend on an external Redis instance.
 * Uses com.github.codemonstur:embedded-redis library.
 */
@TestConfiguration
public class EmbeddedRedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedRedisConfig.class);
    private static final int REDIS_PORT = 6370;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        try {
            redisServer = new RedisServer(REDIS_PORT);
            redisServer.start();
            logger.info("✅ Embedded Redis started on port {}", REDIS_PORT);
        } catch (Exception e) {
            // Log but don't fail - Redis may already be running from another test
            logger.warn("⚠️ Embedded Redis may already be running or failed to start: {} - {}",
                    e.getClass().getSimpleName(), e.getMessage());
            // Don't throw - allow tests to proceed if Redis is already available
            redisServer = null;
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            try {
                redisServer.stop();
                logger.info("🛑 Embedded Redis stopped");
            } catch (Exception e) {
                logger.warn("⚠️ Error stopping embedded Redis: {}", e.getMessage());
            }
        }
    }
}
