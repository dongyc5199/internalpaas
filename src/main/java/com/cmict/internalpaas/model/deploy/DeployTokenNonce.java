package com.cmict.internalpaas.model.deploy;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(name = "deploy_token_nonce")
@Getter
@Setter
@ToString
public class DeployTokenNonce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128, unique = true)
    private String nonce;

    @Column(nullable = false, length = 128)
    private String username;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    public DeployTokenNonce() {
        this.createdAt = Instant.now();
    }

    public void markUsed() {
        this.used = true;
    }
}
