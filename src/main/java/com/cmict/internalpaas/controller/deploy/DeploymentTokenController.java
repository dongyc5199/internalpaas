package com.cmict.internalpaas.controller.deploy;

import com.cmict.internalpaas.service.deploy.DeploymentTokenService;
import com.cmict.internalpaas.service.deploy.dto.DeploymentTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deploy-platform")
@RequiredArgsConstructor
@Validated
public class DeploymentTokenController {

    private static final Logger log = LoggerFactory.getLogger(DeploymentTokenController.class);

    private final DeploymentTokenService deploymentTokenService;

    @PostMapping("/token")
    public ResponseEntity<DeploymentTokenResponse> exchangeToken(
            @Valid @RequestBody TokenRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String username = authentication != null ? authentication.getName() : "anonymous";
        log.debug("Issuing deployment token for user={} nonce={}", username, request.nonce());

        try {
            DeploymentTokenResponse response = deploymentTokenService.issueToken(
                    username,
                    request.nonce(),
                    httpRequest);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to issue deploy token: {}", ex.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    public record TokenRequest(
            @NotBlank(message = "nonce is required")
            String nonce
    ) {
    }
}
