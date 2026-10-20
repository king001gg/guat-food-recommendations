package com.guatfood.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * 启动护栏：必须显式选择 dev / prod（测试可用 test），避免漏配 profile 时误用 H2 或空配置。
 */
@Component
public class ActiveProfileGuard {

    public ActiveProfileGuard(Environment environment) {
        boolean selected = environment.acceptsProfiles(Profiles.of("dev"))
                || environment.acceptsProfiles(Profiles.of("prod"))
                || environment.acceptsProfiles(Profiles.of("test"));
        if (!selected) {
            throw new IllegalStateException(
                    "未指定运行环境。请设置 SPRING_PROFILES_ACTIVE=dev（本地）或 prod（生产），不要直接启动。");
        }
    }
}
