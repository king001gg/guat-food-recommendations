package com.guatfood.controller;

import com.guatfood.audit.Audit;
import com.guatfood.common.AuthUtil;
import com.guatfood.common.Result;
import com.guatfood.dto.ChangePasswordDTO;
import com.guatfood.dto.LoginDTO;
import com.guatfood.dto.RegisterDTO;
import com.guatfood.entity.User;
import com.guatfood.security.ClientIpResolver;
import com.guatfood.security.RateLimitExceededException;
import com.guatfood.security.RateLimitService;
import com.guatfood.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private ClientIpResolver ipResolver;

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.security.rate-limit.login-user-failures:5}")
    private int loginUserFailures;

    @Value("${app.security.rate-limit.login-user-window-seconds:900}")
    private int loginUserWindowSeconds;

    @Value("${app.security.rate-limit.password-change-failures:5}")
    private int passwordChangeFailures;

    @Value("${app.security.rate-limit.password-change-window-seconds:900}")
    private int passwordChangeWindowSeconds;

    @Audit("REGISTER")
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success("注册成功", userService.register(dto));
    }

    @Audit("LOGIN")
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String failureKey = null;
        if (rateLimitEnabled) {
            String ip = ipResolver.resolve(request);
            failureKey = "login:user:" + ip + "|" + dto.getUsername().trim().toLowerCase(Locale.ROOT);
            RateLimitService.Decision decision = rateLimitService.tryAcquire(
                    failureKey, loginUserFailures, Duration.ofSeconds(loginUserWindowSeconds));
            if (!decision.allowed()) {
                throw new RateLimitExceededException(
                        "登录失败次数过多，请稍后再试（" + decision.retryAfterSeconds() + " 秒后可重试）",
                        decision.retryAfterSeconds());
            }
        }
        Map<String, Object> result = userService.login(dto);
        if (failureKey != null) {
            rateLimitService.reset(failureKey);
        }
        return Result.success("登录成功", result);
    }

    /** 当前登录用户信息（用于前端刷新角色/状态，避免长期信任 localStorage） */
    @GetMapping("/me")
    public Result<User> me() {
        Long userId = AuthUtil.requireLogin();
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error(401, "登录状态已失效，请重新登录");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    /** 服务端注销：token_version + 1，旧 Token 立即失效 */
    @Audit("LOGOUT")
    @PostMapping("/logout")
    public Result<?> logout() {
        userService.logout(AuthUtil.requireLogin());
        return Result.success("已退出登录", null);
    }

    /** 修改自己的密码；成功后需要重新登录 */
    @Audit("PASSWORD_CHANGE")
    @PutMapping("/password")
    public Result<?> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = AuthUtil.requireLogin();
        // 持有有效 Token 不等于知道原密码：限制每用户改密尝试次数，防止暴力猜解
        String failureKey = "password:user:" + userId;
        if (rateLimitEnabled) {
            RateLimitService.Decision decision = rateLimitService.tryAcquire(
                    failureKey, passwordChangeFailures, Duration.ofSeconds(passwordChangeWindowSeconds));
            if (!decision.allowed()) {
                throw new RateLimitExceededException(
                        "改密尝试次数过多，请稍后再试（" + decision.retryAfterSeconds() + " 秒后可重试）",
                        decision.retryAfterSeconds());
            }
        }
        userService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        if (rateLimitEnabled) {
            rateLimitService.reset(failureKey);
        }
        return Result.success("密码修改成功，请重新登录", null);
    }
}
