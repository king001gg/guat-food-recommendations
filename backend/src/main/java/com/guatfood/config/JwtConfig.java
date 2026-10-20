package com.guatfood.config;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;

/**
 * JWT 工具类: 签发 / 校验 / 解析
 */
@Component
public class JwtConfig {

    private static final Log log = LogFactory.get();

    /** 优先从环境变量 JWT_SECRET 读取；未配置时生成临时随机密钥（仅限本地开发，重启后登录失效） */
    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:604800000}")
    private long expiration;

    @Autowired
    private Environment environment;

    @PostConstruct
    void init() {
        boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
        if (secret == null || secret.isBlank()) {
            if (prod) {
                throw new IllegalStateException("生产环境必须设置 JWT_SECRET（至少 32 字节）");
            }
            byte[] key = new byte[48];
            new SecureRandom().nextBytes(key);
            secret = Base64.getEncoder().encodeToString(key);
            log.warn("未配置 JWT_SECRET，已生成临时随机密钥（重启后所有登录失效）。生产环境请设置环境变量 JWT_SECRET。");
            return;
        }
        if (isPlaceholder(secret)) {
            throw new IllegalStateException("JWT_SECRET 仍是模板占位值，请改为随机密钥（推荐 openssl rand -base64 48）");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET 至少需要 32 字节（推荐 openssl rand -base64 48）");
        }
    }

    /** 拒绝环境变量模板里的占位值，避免用公开仓库中的示例字符串签发 Token */
    private static boolean isPlaceholder(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return value.contains("改成")
                || lower.contains("changeme")
                || lower.contains("change-me")
                || lower.contains("change_me")
                || lower.contains("please-change")
                || lower.contains("your-secret")
                || lower.contains("your_secret")
                || lower.equals("secret");
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 签发 Token；tv 与 user.token_version 对应，改密/注销/禁用后旧 Token 失效 */
    public String generateToken(Long userId, String username, int tokenVersion) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("tv", tokenVersion)
                .issuedAt(now)
                .expiration(exp)
                .signWith(getKey())
                .compact();
    }

    /** 解析 Token 中的 userId */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    /** 解析 Token 中的 token_version；旧 Token 无此声明时返回 null */
    public Integer getTokenVersionFromToken(String token) {
        Object tv = parseClaims(token).get("tv");
        return tv instanceof Number n ? n.intValue() : null;
    }

    /** 校验 Token 有效性（签名 + 过期时间） */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** 解析并验签 Token；无效时抛出 JwtException/IllegalArgumentException */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
