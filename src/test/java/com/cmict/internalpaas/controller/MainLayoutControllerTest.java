package com.cmict.internalpaas.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phase4-Step4 更新 (2025-10-17): 使用 H2 内存数据库 (scope=test)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MainLayoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminWorkspace_shouldRenderNavigationSection() throws Exception {
        mockMvc.perform(get("/admin/workspace"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-role=\"shell-sidebar\"")))
            .andExpect(content().string(containsString("data-route=\"overview\"")));
    }
}
