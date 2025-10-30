-- Deploy platform token and audit tables
CREATE TABLE deploy_token_nonce (
    id BIGSERIAL PRIMARY KEY,
    nonce VARCHAR(128) NOT NULL,
    username VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX idx_deploy_token_nonce_nonce ON deploy_token_nonce (nonce);
CREATE INDEX idx_deploy_token_nonce_expires ON deploy_token_nonce (expires_at);

CREATE TABLE deploy_token_audit (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(128) NOT NULL,
    nonce VARCHAR(128) NOT NULL,
    success BOOLEAN NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remote_addr VARCHAR(64),
    user_agent VARCHAR(512),
    message VARCHAR(512)
);

CREATE INDEX idx_deploy_token_audit_username ON deploy_token_audit (username);
CREATE INDEX idx_deploy_token_audit_issued ON deploy_token_audit (issued_at);
