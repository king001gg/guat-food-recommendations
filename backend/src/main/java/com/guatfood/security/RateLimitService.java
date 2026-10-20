package com.guatfood.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单机固定窗口限流器（Caffeine，有界 + 自动过期）。
 *
 * <p>适用于当前单实例部署；若扩展为多实例，应替换为 Redis 等共享存储。</p>
 */
@Service
public class RateLimitService {

    private final Cache<String, Counter> counters;

    public RateLimitService(
            @Value("${app.security.rate-limit.max-buckets:100000}") long maxBuckets) {
        this.counters = Caffeine.newBuilder()
                .maximumSize(Math.max(1000, maxBuckets))
                // 必须大于最长窗口（注册按天），否则空闲桶被淘汰会重置额度
                .expireAfterAccess(Duration.ofHours(25))
                .build();
    }

    /**
     * 尝试获取一次配额。窗口内超过 limit 返回 allowed=false。
     */
    public Decision tryAcquire(String key, int limit, Duration window) {
        if (limit <= 0) {
            return new Decision(true, 0, Integer.MAX_VALUE);
        }
        long now = System.currentTimeMillis();
        long windowMillis = Math.max(1000L, window.toMillis());
        Counter counter = counters.get(key, k -> new Counter(now));
        synchronized (counter) {
            if (now - counter.windowStart >= windowMillis) {
                counter.windowStart = now;
                counter.count.set(0);
            }
            int current = counter.count.incrementAndGet();
            if (current <= limit) {
                return new Decision(true, 0, Math.max(0, limit - current));
            }
            long waitMillis = counter.windowStart + windowMillis - now;
            long retryAfter = Math.max(1L, (waitMillis + 999L) / 1000L);
            return new Decision(false, retryAfter, 0);
        }
    }

    /** 登录成功后清除该 IP+用户名的失败计数 */
    public void reset(String key) {
        counters.invalidate(key);
    }

    /** 测试 / 运维手动清空 */
    public void clear() {
        counters.invalidateAll();
    }

    public record Decision(boolean allowed, long retryAfterSeconds, int remaining) {
    }

    private static final class Counter {
        private long windowStart;
        private final AtomicInteger count = new AtomicInteger();

        private Counter(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
