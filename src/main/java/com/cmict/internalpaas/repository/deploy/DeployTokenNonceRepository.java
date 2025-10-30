package com.cmict.internalpaas.repository.deploy;

import com.cmict.internalpaas.model.deploy.DeployTokenNonce;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface DeployTokenNonceRepository extends JpaRepository<DeployTokenNonce, Long> {

    Optional<DeployTokenNonce> findByNonce(String nonce);

    long deleteByExpiresAtBefore(Instant cutoff);
}
