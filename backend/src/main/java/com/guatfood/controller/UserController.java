package com.guatfood.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guatfood.common.AuthUtil;
import com.guatfood.common.Result;
import com.guatfood.entity.User;
import com.guatfood.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /** 管理员分页查询用户列表 */
    @GetMapping
    public Result<Page<User>> list(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   @RequestParam(required = false) String keyword) {
        AuthUtil.requireAdmin(userService);
        return Result.success(userService.getUserPage(page, size, keyword));
    }

    /** 启用/禁用用户 */
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        AuthUtil.requireAdmin(userService);
        userService.updateStatus(id, status);
        return Result.success("操作成功");
    }

    /** 变更角色 */
    @PutMapping("/{id}/role")
    public Result<?> updateRole(@PathVariable Long id, @RequestParam String role) {
        AuthUtil.requireAdmin(userService);
        userService.updateRole(id, role);
        return Result.success("操作成功");
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        AuthUtil.requireAdmin(userService);
        User user = userService.getById(id);
        if (user != null && "ADMIN".equals(user.getRole())) {
            return Result.error("不能删除管理员账号");
        }
        userService.removeById(id);
        return Result.success("删除成功");
    }
}
