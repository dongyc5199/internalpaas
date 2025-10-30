package com.cmict.internalpaas.model.deploy;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(name = "deploy_token_audit")
@Getter
@Setter
@ToString
public class DeployTokenAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String username;

    @Column(nullable = false, length = 128)
    private String nonce;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "remote_addr", length = 64)
    private String remoteAddr;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(length = 512)
    private String message;

    public DeployTokenAudit() {
        this.issuedAt = Instant.now();
    }
}
