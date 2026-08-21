package com.guatfood.config;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

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

    @PostConstruct
    void init() {
        if (secret == null || secret.isBlank()) {
            byte[] key = new byte[48];
            new SecureRandom().nextBytes(key);
            secret = Base64.getEncoder().encodeToString(key);
            log.warn("未配置 JWT_SECRET，已生成临时随机密钥（重启后所有登录失效）。生产环境请设置环境变量 JWT_SECRET。");
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 签发 Token */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(getKey())
                .compact();
    }

    /** 解析 Token 中的 userId */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.valueOf(claims.getSubject());
    }

    /** 校验 Token 有效性 */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** 解析 Token */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
