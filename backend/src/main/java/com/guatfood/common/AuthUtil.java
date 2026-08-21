package com.guatfood.common;

import com.guatfood.config.JwtInterceptor;
import com.guatfood.entity.User;
import com.guatfood.service.UserService;

/**
 * 认证辅助工具
 */
public class AuthUtil {

    private AuthUtil() {}

    /** 获取当前登录用户 ID，未登录抛 401 */
    public static Long requireLogin() {
        Long id = JwtInterceptor.getCurrentUserId();
        if (id == null) {
            throw new BusinessException(401, "请先登录");
        }
        return id;
    }

    /** 校验当前用户为管理员 */
    public static void requireAdmin(UserService userService) {
        Long id = requireLogin();
        User user = userService.getById(id);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "需要管理员权限");
        }
    }
}
