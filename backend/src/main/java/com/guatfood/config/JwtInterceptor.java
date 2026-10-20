package com.guatfood.config;

import com.guatfood.common.BusinessException;
import com.guatfood.entity.User;
import com.guatfood.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器: 校验签名 + 用户状态 + token_version，将 userId 存入 ThreadLocal。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserService userService;

    /** ThreadLocal 存储当前登录用户 ID */
    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();

    public static Long getCurrentUserId() {
        return currentUserId.get();
    }

    public static void setCurrentUserId(Long userId) {
        currentUserId.set(userId);
    }

    public static void removeCurrentUserId() {
        currentUserId.remove();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // OPTIONS 预检直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        boolean readOnly = "GET".equalsIgnoreCase(request.getMethod());
        String token = extractToken(request);

        // GET 请求公开访问；带有效 Token 时注入当前用户，便于详情页返回“我的评分/点赞”状态
        if (!StringUtils.hasText(token)) {
            if (readOnly) {
                return true;
            }
            throw new BusinessException(401, "请先登录");
        }

        User user = authenticate(token);
        if (user == null) {
            // 公开 GET 遇到过期/被吊销 Token 时按匿名处理，避免首页被强制跳转登录
            if (readOnly) {
                return true;
            }
            throw new BusinessException(401, "Token 无效或已过期，请重新登录");
        }

        setCurrentUserId(user.getId());
        return true;
    }

    /**
     * 校验 Token 并返回当前用户；任何一项不满足都返回 null。
     * 校验链：签名/过期时间 -> 用户存在 -> 账号 ACTIVE -> token_version 一致。
     */
    private User authenticate(String token) {
        try {
            Claims claims = jwtConfig.parseClaims(token);
            Long userId = Long.valueOf(claims.getSubject());
            Object tv = claims.get("tv");
            // P1 之前签发的 Token 没有 tv 声明，一律视为失效
            if (!(tv instanceof Number)) {
                return null;
            }
            User user = userService.getById(userId);
            if (user == null || !"ACTIVE".equals(user.getStatus())) {
                return null;
            }
            int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
            return ((Number) tv).intValue() == currentVersion ? user : null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        removeCurrentUserId();
    }

    /** 从 Header 提取 Token */
    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
