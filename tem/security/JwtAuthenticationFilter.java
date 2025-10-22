package com.waveterm.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import javax.crypto.SecretKey;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtAuthProperties properties;

    public JwtAuthenticationFilter(JwtAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (properties.getSecret() == null || properties.getSecret().isBlank()) {
            return true;
        }
        String path = request.getRequestURI();
        return properties.getExcludePaths().stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing bearer token");
            return;
        }
        String token = header.substring(7).trim();
        try {
            Authentication authentication = authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtAuthenticationException ex) {
            log.debug("JWT authentication failed: {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Authentication authenticate(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
            Duration skew = properties.getClockSkew();
            long skewSeconds = skew != null ? Math.max(0L, skew.getSeconds()) : 0L;
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clockSkewSeconds(skewSeconds)
                    .requireIssuer(properties.getIssuer())
                    .requireAudience(properties.getAudience())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            validateExpiration(claims);

            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new JwtAuthenticationException("Token missing subject");
            }
            return new UsernamePasswordAuthenticationToken(subject, token, Collections.emptyList());
        } catch (JwtAuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtAuthenticationException("Invalid JWT token", ex);
        }
    }

    private void validateExpiration(Claims claims) {
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            throw new JwtAuthenticationException("Token missing expiration");
        }
        Instant now = Instant.now();
        Duration skew = properties.getClockSkew() != null ? properties.getClockSkew() : Duration.ZERO;
        Instant exp = expiration.toInstant().plus(skew);
        if (now.isAfter(exp)) {
            throw new JwtAuthenticationException("Token expired");
        }
    }
}
