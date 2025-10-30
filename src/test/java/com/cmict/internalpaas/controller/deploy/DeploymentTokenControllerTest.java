package com.cmict.internalpaas.controller.deploy;

import com.cmict.internalpaas.repository.deploy.DeployTokenAuditRepository;
import com.cmict.internalpaas.repository.deploy.DeployTokenNonceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "deploy.token.jwt-secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DeploymentTokenControllerTest {

    static {
        System.setProperty("jdk.attach.allowAttachSelf", "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeployTokenNonceRepository nonceRepository;

    @Autowired
    private DeployTokenAuditRepository auditRepository;

    @AfterEach
    void cleanup() {
        auditRepository.deleteAll();
        nonceRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void issueToken_success() throws Exception {
        mockMvc.perform(post("/api/deploy-platform/token").with(csrf())
                .contentType("application/json")
                .content("{\"nonce\":\"abc123456789\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.issuer").value("internalpaas"))
                .andExpect(jsonPath("$.audience").value("deploy-platform"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void issueToken_duplicateNonceReturns400() throws Exception {
        String body = "{\"nonce\":\"dupnonce123\"}";
        mockMvc.perform(post("/api/deploy-platform/token").with(csrf())
                .contentType("application/json")
                .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/deploy-platform/token").with(csrf())
                .contentType("application/json")
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void issueToken_forbiddenForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/deploy-platform/token").with(csrf())
                .contentType("application/json")
                .content("{\"nonce\":\"abc123456\"}"))
                .andExpect(status().isForbidden());
    }
}
