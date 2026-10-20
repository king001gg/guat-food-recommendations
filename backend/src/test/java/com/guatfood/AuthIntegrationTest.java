package com.guatfood;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guatfood.entity.AuditLog;
import com.guatfood.entity.User;
import com.guatfood.mapper.AuditLogMapper;
import com.guatfood.mapper.UserMapper;
import com.guatfood.security.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String ADMIN_PASSWORD = "TestAdmin-2026!";
    private static final String USER_PASSWORD = "UserPass-2026!";
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private AuditLogMapper auditLogMapper;
    @Autowired private RateLimitService rateLimitService;

    @BeforeEach
    void resetRateLimit() {
        rateLimitService.clear();
    }

    @Test
    void loginFailureReturns401AndProtectedEndpointRequiresToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminCanListUsersAndDisabledStatusIsHttpForbiddenForNormalUser() throws Exception {
        String token = login("admin", ADMIN_PASSWORD);

        mockMvc.perform(get("/api/users?page=1&size=1000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.size").value(100));

        String username = unique("normal");
        register(username, USER_PASSWORD);
        String userToken = login(username, USER_PASSWORD);
        mockMvc.perform(post("/api/canteens")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "越权食堂"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void registerValidationAndDuplicateReturnRealHttpStatuses() throws Exception {
        String username = unique("reg");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "short", "nickname", "短密码"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        register(username, USER_PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", USER_PASSWORD, "nickname", "重复"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void logoutRevokesTokenImmediately() throws Exception {
        String username = unique("logout");
        register(username, USER_PASSWORD);
        String token = login(username, USER_PASSWORD);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ratings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        // 密码未变，重新登录即可拿到新版本 Token
        login(username, USER_PASSWORD);
    }

    @Test
    void changePasswordInvalidatesOldTokenAndRequiresNewPassword() throws Exception {
        String username = unique("chpwd");
        register(username, USER_PASSWORD);
        String token = login(username, USER_PASSWORD);
        String newPassword = "NewUserPass-2026!";

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", USER_PASSWORD, "newPassword", newPassword))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/likes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", USER_PASSWORD))))
                .andExpect(status().isUnauthorized());
        login(username, newPassword);
    }

    @Test
    void adminCanResetPasswordAndOldTokenIsRevoked() throws Exception {
        String username = unique("reset");
        JsonNode registered = register(username, USER_PASSWORD);
        long userId = registered.path("data").path("id").asLong();
        String userToken = login(username, USER_PASSWORD);
        String adminToken = login("admin", ADMIN_PASSWORD);
        String newPassword = "ResetPass-2026!";

        mockMvc.perform(put("/api/users/" + userId + "/password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("newPassword", newPassword))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        login(username, newPassword);

        long resets = auditLogMapper.selectCount(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getAction, "PASSWORD_RESET")
                .eq(AuditLog::getSuccess, true));
        assertTrue(resets > 0, "password reset should be audited");
    }

    @Test
    void auditLogCapturesFailedAndSuccessfulLoginsAndSupportsAdminQuery() throws Exception {
        String username = unique("audit");
        register(username, USER_PASSWORD);
        String adminToken = login("admin", ADMIN_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "bad-password"))))
                .andExpect(status().isUnauthorized());

        long failed = auditLogMapper.selectCount(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getAction, "LOGIN")
                .eq(AuditLog::getSuccess, false)
                .eq(AuditLog::getActorUsername, username));
        assertTrue(failed > 0, "failed login should be audited");

        mockMvc.perform(get("/api/audit-logs?page=1&size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void frameworkErrorsAndBusinessGuardsUseRealStatuses() throws Exception {
        String adminToken = login("admin", ADMIN_PASSWORD);
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        assertNotNull(admin);

        mockMvc.perform(delete("/api/users/" + admin.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(get("/api/no-such-endpoint"))
                .andExpect(status().isNotFound());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.path("data").path("token").asText();
        assertFalse(token.isBlank(), "token should not be blank");
        return token;
    }

    private JsonNode register(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password, "nickname", "测试用户"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String unique(String prefix) {
        return prefix + SEQ.incrementAndGet() + "-" + (System.nanoTime() % 100000);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
