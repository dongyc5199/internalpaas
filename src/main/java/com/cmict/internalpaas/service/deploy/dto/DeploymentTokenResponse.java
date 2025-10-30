package com.cmict.internalpaas.service.deploy.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class DeploymentTokenResponse {
    String accessToken;
    Instant expiresAt;
    String issuer;
    String audience;
    String signature;
}
