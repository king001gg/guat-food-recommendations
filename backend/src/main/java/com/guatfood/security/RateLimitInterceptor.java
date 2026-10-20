package com.guatfood.security;

import com.guatfood.config.JwtInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;

import java.time.Duration;

/**
 * 登录/注册/上传/评分的粗粒度限流。
 *
 * <p>登录的“IP+用户名失败计数”在 AuthController 中处理，因为需要读取请求体；
 * 这里负责 IP 维度的总请求量，避免脚本无限撞库。</p>
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final ClientIpResolver ipResolver;

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.rate-limit.login-ip-per-minute:60}")
    private int loginIpPerMinute;

    @Value("${app.security.rate-limit.register-ip-per-hour:5}")
    private int registerIpPerHour;

    @Value("${app.security.rate-limit.register-ip-per-day:20}")
    private int registerIpPerDay;

    @Value("${app.security.rate-limit.upload-user-per-minute:10}")
    private int uploadUserPerMinute;

    @Value("${app.security.rate-limit.rating-user-per-minute:20}")
    private int ratingUserPerMinute;

    public RateLimitInterceptor(RateLimitService rateLimitService, ClientIpResolver ipResolver) {
        this.rateLimitService = rateLimitService;
        this.ipResolver = ipResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        if (!enabled || !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 不能用 getRequestURI()：它保留 ;matrix 参数和百分号编码，
        // 而拦截器匹配与 @RequestMapping 都使用解析后的路径，否则可被
        // /api/auth/register;x=1 或 /api/auth/%72egister 绕过限流。
        String path = parsedPath(request);
        String ip = ipResolver.resolve(request);

        if ("/api/auth/login".equals(path)) {
            enforce("login:ip:" + ip, loginIpPerMinute, Duration.ofMinutes(1),
                    "登录请求过于频繁，请稍后再试");
        } else if ("/api/auth/register".equals(path)) {
            enforce("register:ip:hour:" + ip, registerIpPerHour, Duration.ofHours(1),
                    "注册请求过于频繁，请稍后再试");
            enforce("register:ip:day:" + ip, registerIpPerDay, Duration.ofDays(1),
                    "今日注册次数已达上限，请明天再试");
        } else if ("/api/upload/image".equals(path)) {
            enforceUser("upload:user:", uploadUserPerMinute, Duration.ofMinutes(1),
                    "上传过于频繁，请稍后再试");
        } else if ("/api/ratings".equals(path)) {
            enforceUser("rating:user:", ratingUserPerMinute, Duration.ofMinutes(1),
                    "提交过于频繁，请稍后再试");
        }
        return true;
    }

    /** 与 Spring MVC 路由匹配一致的路径：已解码、且移除矩阵参数 */
    private String parsedPath(HttpServletRequest request) {
        var requestPath = ServletRequestPathUtils.hasParsedRequestPath(request)
                ? ServletRequestPathUtils.getParsedRequestPath(request)
                : ServletRequestPathUtils.parseAndCache(request);
        // PathContainer.value() 是否剥离 ;matrix 参数取决于 RequestPath 的解析来源，
        // 这里再统一剥离一次，确保与路由匹配结果一致。
        return requestPath.pathWithinApplication().value().replaceAll(";[^/]*", "");
    }

    private void enforceUser(String prefix, int limit, Duration window, String message) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId != null) {
            enforce(prefix + userId, limit, window, message);
        }
    }

    private void enforce(String key, int limit, Duration window, String message) {
        RateLimitService.Decision decision = rateLimitService.tryAcquire(key, limit, window);
        if (!decision.allowed()) {
            throw new RateLimitExceededException(
                    message + "（" + decision.retryAfterSeconds() + " 秒后可重试）",
                    decision.retryAfterSeconds());
        }
    }
}
