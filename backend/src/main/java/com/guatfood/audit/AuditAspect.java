package com.guatfood.audit;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.guatfood.common.BusinessException;
import com.guatfood.common.Result;
import com.guatfood.config.JwtInterceptor;
import com.guatfood.dto.LoginDTO;
import com.guatfood.dto.RegisterDTO;
import com.guatfood.entity.AuditLog;
import com.guatfood.entity.User;
import com.guatfood.security.ClientIpResolver;
import com.guatfood.security.RateLimitExceededException;
import com.guatfood.service.AuditLogService;
import com.guatfood.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 审计日志切面：记录安全/管理写操作的成功与失败。
 *
 * <p>只记录方法、路径、查询串（脱敏）和结果码，不记录请求体，避免密码/Token 落库。
 * 审计写入使用独立事务并吞掉异常，不能影响主业务。</p>
 */
@Aspect
@Component
public class AuditAspect {

    private static final Log log = LogFactory.get();

    /** 参数名包含这些片段时（解码并归一化后）一律脱敏 */
    private static final List<String> SENSITIVE_NAME_PARTS = List.of(
            "pass", "pwd", "secret", "token", "credential", "session", "signature",
            "authorization", "cookie");

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserService userService;

    @Autowired
    private ClientIpResolver ipResolver;

    @Value("${app.audit.enabled:true}")
    private boolean enabled;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        if (!enabled) {
            return joinPoint.proceed();
        }

        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                writeAudit(joinPoint.getArgs(), audit.value(), result, error,
                        System.currentTimeMillis() - start);
            } catch (Exception e) {
                // 审计失败不能影响业务，也不能把异常抛给客户端
                log.warn("写入审计日志失败: {}", e.getMessage());
            }
        }
    }

    private void writeAudit(Object[] args, String action, Object result, Throwable error,
                            long durationMs) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();

        Long actorId = JwtInterceptor.getCurrentUserId();
        String actorUsername = null;
        String actorRole = null;

        if (actorId != null) {
            User actor = userService.getById(actorId);
            if (actor != null) {
                actorUsername = actor.getUsername();
                actorRole = actor.getRole();
            }
        }

        // 登录/注册时 JWT 尚未建立，从参数或返回结果中取账号
        if (actorUsername == null) {
            for (Object arg : args) {
                if (arg instanceof LoginDTO dto) {
                    actorUsername = dto.getUsername();
                } else if (arg instanceof RegisterDTO dto) {
                    actorUsername = dto.getUsername();
                }
            }
        }
        if (result instanceof Result<?> wrapper) {
            Object data = wrapper.getData();
            if (data instanceof User user) {
                actorId = user.getId();
                actorUsername = user.getUsername();
                actorRole = user.getRole();
            } else if (data instanceof Map<?, ?> map && map.get("user") instanceof User user) {
                actorId = user.getId();
                actorUsername = user.getUsername();
                actorRole = user.getRole();
            }
        }

        int code = 200;
        boolean success = true;
        String errorMessage = null;
        if (error instanceof BusinessException businessException) {
            code = businessException.getCode();
            success = false;
            errorMessage = businessException.getMessage();
        } else if (error instanceof RateLimitExceededException rateLimitException) {
            code = 429;
            success = false;
            errorMessage = rateLimitException.getMessage();
        } else if (error != null) {
            code = 500;
            success = false;
            errorMessage = "系统异常";
        } else if (result instanceof Result<?> wrapper) {
            code = wrapper.getCode();
            success = code >= 200 && code < 400;
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setActorId(actorId);
        auditLog.setActorUsername(truncate(actorUsername, 50));
        auditLog.setActorRole(truncate(actorRole, 20));
        auditLog.setAction(truncate(action, 50));
        auditLog.setMethod(truncate(request.getMethod(), 10));
        // 去掉 ;matrix 参数（可能含 jsessionid 等），只保留真实路径
        auditLog.setPath(truncate(request.getRequestURI().replaceAll(";[^/]*", ""), 255));
        auditLog.setQueryString(truncate(sanitizeQuery(request.getQueryString()), 1000));
        auditLog.setSuccess(success);
        auditLog.setResultCode(code);
        auditLog.setErrorMessage(truncate(errorMessage, 255));
        auditLog.setIp(truncate(ipResolver.resolve(request), 45));
        auditLog.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
        auditLog.setDurationMs((int) Math.min(Integer.MAX_VALUE, Math.max(0, durationMs)));
        auditLogService.record(auditLog);
    }

    /**
     * 查询串脱敏：按参数名（URL 解码、忽略大小写与常见分隔符）匹配，
     * 覆盖 password[]、user[password]、secret_key、apiKey、百分号编码等变体。
     */
    String sanitizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        StringBuilder sanitized = new StringBuilder(query.length());
        for (String pair : query.split("&", -1)) {
            if (sanitized.length() > 0) {
                sanitized.append('&');
            }
            int eq = pair.indexOf('=');
            if (eq >= 0 && isSensitiveName(pair.substring(0, eq))) {
                sanitized.append(pair, 0, eq + 1).append("***");
            } else {
                sanitized.append(pair);
            }
        }
        return sanitized.toString();
    }

    private boolean isSensitiveName(String rawName) {
        String name;
        try {
            name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            name = rawName;
        }
        String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.equals("key")
                || normalized.contains("apikey")
                || normalized.contains("accesskey")
                || normalized.contains("privatekey")
                || SENSITIVE_NAME_PARTS.stream().anyMatch(normalized::contains);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
