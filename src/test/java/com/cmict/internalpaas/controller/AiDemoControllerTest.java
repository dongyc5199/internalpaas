package com.cmict.internalpaas.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 针对 AI Demo 控制器的基础单元测试，验证 Stub 接口返回结构。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AiDemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "demo", roles = {"USER"})
    void suggest_shouldReturnDefaultSuggestions() throws Exception {
        String body = """
            {
              "prompt": "查看服务状态"
            }
            """;

        MvcResult result = mockMvc.perform(
                post("/ai/completion/suggest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(json).contains("suggestions");
        assertThat(json).contains("journalctl -fu internal-paas");
    }

    @Test
    @WithMockUser(username = "demo", roles = {"USER"})
    void chatStream_shouldProduceSsePayload() throws Exception {
        String body = """
            {
              "chatId": "test-chat",
              "model": "echo",
              "message": "帮我分析 CPU 使用情况",
              "terminalTail": "load average: 2.5"
            }
            """;

        MvcResult result = mockMvc.perform(
                post("/ai/chat/stream")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .with(csrf()))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk())
            .andReturn();

        String payload = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(payload).contains("event:token");
        assertThat(payload).contains("Echo: 帮我分析 CPU 使用情况");
    }
}
