package com.cmict.internalpaas.service.deploy;

import com.cmict.internalpaas.model.deploy.DeployTokenAudit;
import com.cmict.internalpaas.model.deploy.DeployTokenNonce;
import com.cmict.internalpaas.repository.deploy.DeployTokenAuditRepository;
import com.cmict.internalpaas.repository.deploy.DeployTokenNonceRepository;
import com.cmict.internalpaas.service.deploy.dto.DeploymentTokenResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeploymentTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentTokenService.class);

    private final DeployTokenNonceRepository nonceRepository;
    private final DeployTokenAuditRepository auditRepository;

    @Value("${deploy.token.issuer:internalpaas}")
    private String issuer;

    @Value("${deploy.token.audience:deploy-platform}")
    private String audience;

    @Value("${deploy.token.expiry-seconds:1800}")
    private long expirySeconds;

    @Value("${deploy.token.jwt-secret:change-me-please-change-me-please-change-me-change-me-please-change-me}")
    private String jwtSecret;

    @Transactional
    public DeploymentTokenResponse issueToken(String username, String nonce, HttpServletRequest request) {
        Instant now = Instant.now();

        if (nonce == null || nonce.isBlank()) {
            throw new IllegalArgumentException("Nonce must not be blank");
        }
        if (nonce.length() < 8 || nonce.length() > 128) {
            throw new IllegalArgumentException("Nonce length invalid");
        }

        Optional<DeployTokenNonce> existing = nonceRepository.findByNonce(nonce);
        if (existing.isPresent()) {
            DeployTokenNonce entity = existing.get();
            if (entity.isUsed()) {
                log.warn("Nonce already used for user={}", username);
                saveAudit(username, nonce, false, request, "nonce used");
                throw new IllegalArgumentException("Nonce already used");
            }
            if (entity.getExpiresAt().isBefore(now)) {
                log.warn("Nonce expired for user={}", username);
                saveAudit(username, nonce, false, request, "nonce expired");
                throw new IllegalArgumentException("Nonce expired");
            }
            entity.markUsed();
            nonceRepository.save(entity);
        } else {
            DeployTokenNonce nonceEntity = new DeployTokenNonce();
            nonceEntity.setNonce(nonce);
            nonceEntity.setUsername(username);
            nonceEntity.setCreatedAt(now);
            nonceEntity.setExpiresAt(now.plus(Duration.ofSeconds(expirySeconds)));
            nonceEntity.markUsed();
            nonceRepository.save(nonceEntity);
        }

        String token = generateJwt(username, nonce, now);
        Instant expiresAt = now.plusSeconds(expirySeconds);

        saveAudit(username, nonce, true, request, "issued");

        return DeploymentTokenResponse.builder()
                .accessToken(token)
                .expiresAt(expiresAt)
                .issuer(issuer)
                .audience(audience)
                .signature(null)
                .build();
    }

    private String generateJwt(String username, String nonce, Instant issuedAt) {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        Key key = Keys.hmacShaKeyFor(secretBytes);
        Date now = Date.from(issuedAt);
        Date expiry = Date.from(issuedAt.plusSeconds(expirySeconds));

        return Jwts.builder()
                .setSubject(username)
                .setIssuer(issuer)
                .setAudience(audience)
                .claim("nonce", nonce)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private void saveAudit(String username, String nonce, boolean success, HttpServletRequest request, String message) {
        DeployTokenAudit audit = new DeployTokenAudit();
        audit.setUsername(username);
        audit.setNonce(nonce);
        audit.setSuccess(success);
        audit.setRemoteAddr(request.getRemoteAddr());
        audit.setUserAgent(request.getHeader("User-Agent"));
        audit.setMessage(message);
        auditRepository.save(audit);
    }
}
