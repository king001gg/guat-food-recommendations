package com.guatfood;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guatfood.security.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.rate-limit.login-user-failures=2",
        "app.security.rate-limit.login-ip-per-minute=100",
        "app.security.rate-limit.register-ip-per-hour=2",
        "app.security.rate-limit.register-ip-per-day=100",
        "app.security.rate-limit.password-change-failures=2"
})
class RateLimitIntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RateLimitService rateLimitService;

    @BeforeEach
    void reset() {
        rateLimitService.clear();
    }

    @Test
    void repeatedLoginFailuresReturn429WithRetryAfter() throws Exception {
        String username = "nonexistent-" + SEQ.incrementAndGet() + "-" + (System.nanoTime() % 100000);
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("username", username, "password", "bad-password"))))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "bad-password"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value(429));
    }

    @Test
    void registrationIsLimitedPerIp() throws Exception {
        for (int i = 0; i < 2; i++) {
            String username = "limited-" + SEQ.incrementAndGet() + "-" + (System.nanoTime() % 100000);
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("username", username,
                                    "password", "UserPass-2026!", "nickname", "限流测试"))))
                    .andExpect(status().isOk());
        }
        String third = "limited-" + SEQ.incrementAndGet() + "-" + (System.nanoTime() % 100000);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", third,
                                "password", "UserPass-2026!", "nickname", "限流测试"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value(429));
    }

    @Test
    void matrixAndPercentEncodedPathsCannotBypassRegistrationLimit() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("username", unique("bypass"),
                                    "password", "UserPass-2026!", "nickname", "绕过测试"))))
                    .andExpect(status().isOk());
        }

        // Spring 路由会忽略矩阵参数并解码百分号路径；限流也必须使用同样的解析结果
        mockMvc.perform(post("/api/auth/register;x=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", unique("bypass-matrix"),
                                "password", "UserPass-2026!", "nickname", "矩阵参数"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));

        // 百分号编码路径不会被映射到公开的注册路由（先被 JWT 拦截），
        // 因此同样无法绕过限流；若未来 Spring 开始解码，则上面的 429 断言仍应成立。
        mockMvc.perform(post("/api/auth/%72egister")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", unique("bypass-encoded"),
                                "password", "UserPass-2026!", "nickname", "编码路径"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangeAttemptsAreLimitedPerUser() throws Exception {
        String username = unique("pwd-limit");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username,
                                "password", "UserPass-2026!", "nickname", "改密限流"))))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "UserPass-2026!"))))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("token").asText();

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(put("/api/auth/password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("oldPassword", "WrongPass-2026!",
                                    "newPassword", "NewPass-2026!"))))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", "WrongPass-2026!",
                                "newPassword", "NewPass-2026!"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value(429));
    }

    private String unique(String prefix) {
        return prefix + "-" + SEQ.incrementAndGet() + "-" + (System.nanoTime() % 100000);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
