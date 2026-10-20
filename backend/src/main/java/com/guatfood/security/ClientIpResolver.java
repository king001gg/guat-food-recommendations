package com.guatfood.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 解析客户端 IP。
 *
 * <p>只有直连方是可信代理（同机 Nginx / 回环）时才读取 X-Forwarded-For，
 * 并从右往左取第一个非可信地址，防止客户端伪造最左侧 XFF。</p>
 */
@Component
public class ClientIpResolver {

    private final Set<String> trustedProxies;

    public ClientIpResolver(
            @Value("${app.security.rate-limit.trusted-proxies:127.0.0.1,::1}") String trusted) {
        Set<String> proxies = new LinkedHashSet<>();
        Arrays.stream(trusted.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(proxies::add);
        // JVM 在双栈环境下可能返回 IPv6 回环写法
        proxies.add("127.0.0.1");
        proxies.add("::1");
        proxies.add("0:0:0:0:0:0:0:1");
        this.trustedProxies = Set.copyOf(proxies);
    }

    public String resolve(HttpServletRequest request) {
        String remote = normalize(request.getRemoteAddr());
        if (remote == null) {
            remote = "unknown";
        }
        if (!trustedProxies.contains(remote)) {
            return remote;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String[] hops = forwarded.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String candidate = normalize(hops[i]);
                if (candidate != null && !trustedProxies.contains(candidate)) {
                    return candidate;
                }
            }
        }

        String realIp = normalize(request.getHeader("X-Real-IP"));
        if (realIp != null && !trustedProxies.contains(realIp)) {
            return realIp;
        }
        return remote;
    }

    /** 只做字面量校验，绝不做 DNS 解析，避免 XFF 触发反向解析 */
    private String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.isEmpty() || value.length() > 45 || !value.matches("[0-9a-fA-F:.]+")) {
            return null;
        }
        return value;
    }
}
