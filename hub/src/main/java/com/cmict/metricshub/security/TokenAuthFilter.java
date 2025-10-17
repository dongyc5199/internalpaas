package com.cmict.metricshub.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucketBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT Bearer Token Authentication Filter with JWKS support
 * 
 * Features:
 * - Validates Bearer tokens using JWKS endpoint
 * - Per-agent rate limiting
 * - Token caching to reduce JWKS queries
 * - Detailed logging and metrics
 * 
 * Configuration:
 * <pre>
 * metrics-hub:
 *   security:
 *     jwt:
 *       enabled: true
 *       jwks-url: https://auth.example.com/.well-known/jwks.json
 *       issuer: https://auth.example.com
 *       audience: metrics-hub
 *       rate-limit:
 *         enabled: true
 *         requests-per-minute: 120  # Per agent
 *         burst-capacity: 10
 * </pre>
 * 
 * @author Metrics Hub Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "metrics-hub.security.jwt.enabled", havingValue = "true")
public class TokenAuthFilter extends OncePerRequestFilter {

    @Value("${metrics-hub.security.jwt.jwks-url}")
    private String jwksUrl;

    @Value("${metrics-hub.security.jwt.issuer}")
    private String expectedIssuer;

    @Value("${metrics-hub.security.jwt.audience:metrics-hub}")
    private String expectedAudience;

    @Value("${metrics-hub.security.jwt.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${metrics-hub.security.jwt.rate-limit.requests-per-minute:120}")
    private int requestsPerMinute;

    @Value("${metrics-hub.security.jwt.rate-limit.burst-capacity:10}")
    private int burstCapacity;

    // JWT Processor for token validation
    private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    // Per-agent rate limiters
    private final Map<String, Bucket> rateLimiters = new ConcurrentHashMap<>();

    // Token cache to reduce JWKS queries
    private final Map<String, TokenCacheEntry> tokenCache = new ConcurrentHashMap<>();

    private static final int TOKEN_CACHE_MAX_SIZE = 1000;
    private static final Duration TOKEN_CACHE_TTL = Duration.ofMinutes(5);

    /**
     * Initialize JWT Processor with JWKS
     */
    @Override
    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        try {
            log.info("🔐 Initializing JWT Token Authentication Filter");
            log.info("   JWKS URL: {}", jwksUrl);
            log.info("   Expected Issuer: {}", expectedIssuer);
            log.info("   Expected Audience: {}", expectedAudience);
            log.info("   Rate Limiting: {} ({} req/min, burst {})", 
                rateLimitEnabled, requestsPerMinute, burstCapacity);

            // Configure JWT Processor
            jwtProcessor = new DefaultJWTProcessor<>();

            // Set up JWKS source
            JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(new URL(jwksUrl));

            // Configure expected JWS algorithm (RS256)
            JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
            JWSKeySelector<SecurityContext> keySelector = 
                new JWSVerificationKeySelector<>(expectedJWSAlg, jwkSource);

            jwtProcessor.setJWSKeySelector(keySelector);

            log.info("✅ JWT Token Filter initialized successfully");

        } catch (Exception e) {
            log.error("❌ Failed to initialize JWT Token Filter", e);
            throw new ServletException("JWT Filter initialization failed", e);
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Skip authentication for health check endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator/health") || path.startsWith("/actuator/prometheus")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract Bearer token
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("❌ Missing or invalid Authorization header from {}", request.getRemoteAddr());
                sendError(response, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
                return;
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix

            // Check token cache first
            TokenCacheEntry cached = tokenCache.get(token);
            if (cached != null && !cached.isExpired()) {
                log.debug("✅ Token found in cache (agent: {})", cached.agentId);
                
                // Apply rate limiting
                if (rateLimitEnabled && !checkRateLimit(cached.agentId)) {
                    log.warn("⚠️ Rate limit exceeded for agent: {}", cached.agentId);
                    sendError(response, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
                    return;
                }

                // Set agent ID in request attribute for downstream use
                request.setAttribute("agentId", cached.agentId);
                filterChain.doFilter(request, response);
                return;
            }

            // Validate token via JWKS
            JWTClaimsSet claims = jwtProcessor.process(token, null);

            // Validate issuer
            if (!expectedIssuer.equals(claims.getIssuer())) {
                log.warn("❌ Invalid issuer: {} (expected: {})", claims.getIssuer(), expectedIssuer);
                sendError(response, HttpStatus.UNAUTHORIZED, "Invalid token issuer");
                return;
            }

            // Validate audience
            if (!claims.getAudience().contains(expectedAudience)) {
                log.warn("❌ Invalid audience: {} (expected: {})", claims.getAudience(), expectedAudience);
                sendError(response, HttpStatus.UNAUTHORIZED, "Invalid token audience");
                return;
            }

            // Extract agent ID from claims (subject or custom claim)
            String agentId = claims.getSubject();
            if (agentId == null || agentId.isEmpty()) {
                agentId = (String) claims.getClaim("agent_id");
            }

            if (agentId == null || agentId.isEmpty()) {
                log.warn("❌ Token missing agent ID");
                sendError(response, HttpStatus.UNAUTHORIZED, "Token missing agent identifier");
                return;
            }

            log.debug("✅ Token validated successfully (agent: {})", agentId);

            // Cache token
            cacheToken(token, agentId);

            // Apply rate limiting
            if (rateLimitEnabled && !checkRateLimit(agentId)) {
                log.warn("⚠️ Rate limit exceeded for agent: {}", agentId);
                sendError(response, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
                return;
            }

            // Set agent ID in request attribute
            request.setAttribute("agentId", agentId);

            // Continue filter chain
            filterChain.doFilter(request, response);

        } catch (com.nimbusds.jose.proc.BadJOSEException e) {
            log.warn("❌ Invalid JWT signature: {}", e.getMessage());
            sendError(response, HttpStatus.UNAUTHORIZED, "Invalid token signature");

        } catch (Exception e) {
            log.error("❌ Token validation error", e);
            sendError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Token validation failed");
        }
    }

    /**
     * Check rate limit for agent
     * 
     * @param agentId Agent identifier
     * @return true if request is allowed, false if rate limit exceeded
     */
    private boolean checkRateLimit(String agentId) {
        Bucket bucket = rateLimiters.computeIfAbsent(agentId, id -> {
            // Create rate limiter: X requests per minute with burst capacity
            LocalBucketBuilder builder = Bucket4j.builder();
            return builder
                .addLimit(limit -> limit
                    .capacity(requestsPerMinute + burstCapacity)
                    .refillGreedy(requestsPerMinute, Duration.ofMinutes(1)))
                .build();
        });

        return bucket.tryConsume(1);
    }

    /**
     * Cache validated token
     */
    private void cacheToken(String token, String agentId) {
        // Limit cache size
        if (tokenCache.size() >= TOKEN_CACHE_MAX_SIZE) {
            // Simple eviction: remove oldest entries
            tokenCache.entrySet().stream()
                .filter(e -> e.getValue().isExpired())
                .limit(100)
                .forEach(e -> tokenCache.remove(e.getKey()));
        }

        tokenCache.put(token, new TokenCacheEntry(agentId, System.currentTimeMillis()));
        log.debug("📦 Token cached (agent: {}, cache size: {})", agentId, tokenCache.size());
    }

    /**
     * Send error response
     */
    private void sendError(HttpServletResponse response, HttpStatus status, String message) 
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\":\"%s\",\"message\":\"%s\"}",
            status.getReasonPhrase(),
            message
        ));
    }

    /**
     * Token cache entry
     */
    private static class TokenCacheEntry {
        final String agentId;
        final long timestamp;

        TokenCacheEntry(String agentId, long timestamp) {
            this.agentId = agentId;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TOKEN_CACHE_TTL.toMillis();
        }
    }

    /**
     * Clear token cache (for testing or manual refresh)
     */
    public void clearTokenCache() {
        tokenCache.clear();
        log.info("🗑️ Token cache cleared");
    }

    /**
     * Get rate limiter statistics
     */
    public Map<String, Long> getRateLimiterStats() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        rateLimiters.forEach((agentId, bucket) -> {
            stats.put(agentId, bucket.getAvailableTokens());
        });
        return stats;
    }
}
