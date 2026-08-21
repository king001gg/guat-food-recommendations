package com.guatfood.config;

import com.guatfood.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器: 校验 Token, 将 userId 存入 ThreadLocal
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtConfig jwtConfig;

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

        // GET 请求公开访问；但尝试解析 Token，如果有效则存入 ThreadLocal 供后续使用
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String token = extractToken(request);
            if (StringUtils.hasText(token) && jwtConfig.validateToken(token)) {
                setCurrentUserId(jwtConfig.getUserIdFromToken(token));
            }
            return true;
        }

        // POST / PUT / DELETE 必须认证
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "请先登录");
        }

        if (!jwtConfig.validateToken(token)) {
            throw new BusinessException(401, "Token 无效或已过期，请重新登录");
        }

        Long userId = jwtConfig.getUserIdFromToken(token);
        setCurrentUserId(userId);
        return true;
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
