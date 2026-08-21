package com.guatfood.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guatfood.common.AuthUtil;
import com.guatfood.common.PageResult;
import com.guatfood.common.Result;
import com.guatfood.dto.DishDTO;
import com.guatfood.entity.Dish;
import com.guatfood.service.DishService;
import com.guatfood.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dishes")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private UserService userService;

    /** 排行榜 (type: overall/taste/hot/recent)，可按 windowId/canteenId 过滤 */
    @GetMapping
    public Result<PageResult<Dish>> rank(@RequestParam(defaultValue = "overall") String type,
                                         @RequestParam(required = false) Long windowId,
                                         @RequestParam(required = false) Long canteenId,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(dishService.rank(type, windowId, canteenId, keyword, page, size));
    }

    /** 管理端列表 (含待审核) */
    @GetMapping("/all")
    public Result<Page<Dish>> adminList(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size) {
        AuthUtil.requireAdmin(userService);
        return Result.success(dishService.adminList(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<Dish> detail(@PathVariable Long id) {
        return Result.success(dishService.getDetail(id));
    }

    /** 投稿菜品 */
    @PostMapping
    public Result<Dish> submit(@Valid @RequestBody DishDTO dto) {
        Long userId = AuthUtil.requireLogin();
        return Result.success("投稿成功，等待审核", dishService.submit(dto, userId));
    }

    /** 审核菜品 */
    @PutMapping("/{id}/status")
    public Result<?> approve(@PathVariable Long id, @RequestParam String status) {
        AuthUtil.requireAdmin(userService);
        dishService.approve(id, status);
        return Result.success("审核完成");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        AuthUtil.requireAdmin(userService);
        dishService.deleteDish(id);
        return Result.success("删除成功");
    }
}
